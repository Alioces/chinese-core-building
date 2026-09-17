package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.Transform;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.Identifier;
import net.minecraft.world.BlockRenderView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 子模型渲染包装器。
 * <p>
 * 负责在渲染时延迟加载和渲染子模型，并应用正确的缩放变换。
 * 继承 {@link ForwardingBakedModel}，包装原始模型并在渲染时
 * 追加子模型的面片。
 * </p>
 * <p>
 * <b>架构设计：</b>
 * 采用"延迟初始化 + 线程安全缓存"策略解决模型加载时机问题：
 * <ul>
 *   <li>构造函数：仅存储 Identifier，不尝试解析（避免烘焙时 NPE）</li>
 *   <li>首次渲染：在 emitBlockQuads 中延迟加载并缓存 BakedModel</li>
 *   <li>后续渲染：直接使用缓存，无额外开销</li>
 * </ul>
 * </p>
 *
 * @see ForwardingBakedModel
 * @see BakedModelSpec
 */
public class SubModelBakedModel extends ForwardingBakedModel {

    /**
     * SLF4J 日志记录器，用于输出子模型调试信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("SubModel");

    /**
     * 子模型规格列表（未解析的 Identifier）。
     */
    private final List<BakedModelSpec.SubModelEntry> subModelEntries;

    /**
     * 线程安全的模型缓存（Identifier → BakedModel）。
     * <p>
     * 使用 ConcurrentHashMap 保证多线程安全，
     * 避免在 Worker 线程和主线程间竞争。
     * </p>
     */
    private final Map<Identifier, BakedModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 标记是否已完成初始化（所有子模型都已尝试加载）。
     */
    private volatile boolean initialized = false;

    /**
     * 构造函数。
     * <p>
     * 仅存储子模型规格，不立即解析 BakedModel。
     * 实际的模型加载会延迟到第一次渲染时进行。
     * </p>
     *
     * @param original        原始烘焙模型
     * @param subModelEntries 子模型规格列表（未解析）
     */
    public SubModelBakedModel(BakedModel original, List<BakedModelSpec.SubModelEntry> subModelEntries) {
        this.wrapped = original;
        this.subModelEntries = subModelEntries;
        LOGGER.info("SubModelBakedModel 创建完成, 待解析子模型数: {}", subModelEntries.size());
    }

    /**
     * Fabric 渲染管线的面片发射方法。
     * <p>
     * 先发射原始模型的面片，然后依次发射每个子模型的面片，
     * 并应用对应的变换（位置、旋转、缩放）。
     * 首次调用时会触发子模型的延迟加载和缓存。
     * </p>
     *
     * @param blockView      世界视图
     * @param state          当前方块状态
     * @param pos            方块在世界中的位置
     * @param randomSupplier 随机数生成器
     * @param context        渲染上下文
     */
    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        // 延迟初始化：仅在首次渲染时加载子模型
        if (!initialized) {
            initializeSubModels();
        }

        LOGGER.info("emitBlockQuads 被调用! 子模型数量: {}, 已缓存: {}", 
            subModelEntries.size(), modelCache.size());

        // 先发射原始模型的面片
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);

        // 依次发射每个子模型
        for (int i = 0; i < subModelEntries.size(); i++) {
            BakedModelSpec.SubModelEntry entry = subModelEntries.get(i);
            BakedModel cachedModel = modelCache.get(entry.modelId());
            
            if (cachedModel != null) {
                LOGGER.info("发射子模型 {}/{}: {} pos={} scale={}",
                    (i + 1), subModelEntries.size(),
                    entry.modelId(), entry.transform().position(), entry.transform().scale());
                emitSubModelQuads(cachedModel, entry.transform(), context);
            } else {
                LOGGER.warn("跳过未缓存的子模型 {}/{}: {}", 
                    (i + 1), subModelEntries.size(), entry.modelId());
            }
        }
    }

    /**
     * 延迟初始化所有子模型。
     * <p>
     * 此方法仅在首次渲染时调用一次，通过 volatile 标志位保证线程安全。
     * 关键：模型加载必须在 Render thread（主线程）执行，
     * 因为 BakedModelManager.getModel() 在 Worker 线程返回 null。
     * </p>
     */
    private void initializeSubModels() {
        synchronized (this) {
            // 双重检查：防止多线程重复初始化
            if (initialized) {
                return;
            }

            LOGGER.info("开始延迟初始化 {} 个子模型...", subModelEntries.size());

            // 检查当前线程
            boolean isMainThread = Thread.currentThread().getName().equals("Render thread");
            
            if (isMainThread) {
                // 已经在主线程，直接加载
                LOGGER.info("✅ 当前是主线程，直接加载子模型");
                loadAllSubModels();
            } else {
                // 在 Worker 线程，需要提交到主线程执行
                LOGGER.info("⏳ 当前是 Worker 线程 ({})，提交到主线程加载...", 
                    Thread.currentThread().getName());
                submitToMainThread();
            }

            initialized = true;
            LOGGER.info("子模型初始化完成, 成功缓存: {}/{}", modelCache.size(), subModelEntries.size());
        }
    }

    /**
     * 在当前线程直接加载所有子模型。
     */
    private void loadAllSubModels() {
        for (BakedModelSpec.SubModelEntry entry : subModelEntries) {
            Identifier modelId = entry.modelId();
            try {
                BakedModel model = loadModelWithFallback(modelId);
                if (model != null) {
                    modelCache.put(modelId, model);
                    LOGGER.info("✅ 子模型加载成功: {}", modelId);
                } else {
                    LOGGER.error("❌ 子模型加载失败: {} (所有格式都失败)", modelId);
                }
            } catch (Exception e) {
                LOGGER.error("❌ 加载子模型异常: {}", modelId, e);
            }
        }
    }

    /**
     * 将模型加载任务提交到主线程并等待完成。
     * <p>
     * 使用 CountDownLatch 同步，确保在主线程完成加载后才继续。
     * 设置 5 秒超时避免死锁。
     * </p>
     */
    private void submitToMainThread() {
        final CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] errorHolder = new Throwable[1];
        
        try {
            MinecraftClient.getInstance().execute(() -> {
                try {
                    LOGGER.info("🎯 主线程开始执行模型加载任务...");
                    loadAllSubModels();
                    LOGGER.info("🎯 主线程模型加载完成");
                } catch (Throwable t) {
                    errorHolder[0] = t;
                    LOGGER.error("🎯 主线程加载时发生异常:", t);
                } finally {
                    latch.countDown(); // 通知等待的线程
                }
            });

            // 等待主线程完成（最多 5 秒）
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            
            if (!completed) {
                LOGGER.error("⏰ 等待主线程加载超时（5秒）！");
            } else if (errorHolder[0] != null) {
                LOGGER.error("❌ 主线程加载失败:", errorHolder[0]);
            } else {
                LOGGER.info("✅ 主线程加载成功，已返回 Worker 线程");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("⛔ 等待主线程时被中断:", e);
        }
    }

    /**
     * 通过方块注册表加载 BakedModel。
     * <p>
     * 核心原理：绕过 BakedModelManager.getModel(Identifier) 的
     * ModelIdentifier hashCode 不匹配问题，改为通过
     * Registries.BLOCK → BlockRenderManager.getModel(BlockState)
     * 的正确 API 获取模型。
     * </p>
     * <p>
     * 按顺序尝试：
     * <ol>
     *   <li>直接用 modelId 的 path 查找方块（如 oak_planks）</li>
     *   <li>去掉 block/ 前缀后查找（如 block/oak_planks → oak_planks）</li>
     * </ol>
     * </p>
     *
     * @param modelId 模型标识符（如 minecraft:oak_planks 或 minecraft:block/oak_planks）
     * @return 成功加载的 BakedModel，或 null
     */
    private BakedModel loadModelWithFallback(Identifier modelId) {
        String namespace = modelId.getNamespace();
        String path = modelId.getPath();

        LOGGER.info("🔍 开始查找模型: {}://{}", namespace, path);

        // 尝试 1：直接用 path 查找方块（标准格式如 oak_planks）
        Identifier blockId = new Identifier(namespace, path);
        BakedModel model = loadModelFromBlock(blockId);
        if (model != null) return model;

        // 尝试 2：如果 path 以 block/ 开头，去掉前缀后重试
        if (path.startsWith("block/")) {
            Identifier strippedId = new Identifier(namespace, path.substring(6));
            LOGGER.warn("⚠️ 格式1失败: {}, 尝试去掉block/前缀: {}", blockId, strippedId);
            model = loadModelFromBlock(strippedId);
            if (model != null) {
                LOGGER.info("✅ 格式2成功: {}", strippedId);
                return model;
            }
        } else {
            // 尝试 3：添加 block/ 前缀
            Identifier prefixedId = new Identifier(namespace, "block/" + path);
            LOGGER.warn("⚠️ 格式1失败: {}, 尝试添加block/前缀: {}", blockId, prefixedId);
            model = loadModelFromBlock(prefixedId);
            if (model != null) {
                LOGGER.info("✅ 格式3成功: {}", prefixedId);
                return model;
            }
        }

        LOGGER.error("❌ 所有格式都失败: 原始ID={}", modelId);
        return null;
    }

    /**
     * 通过方块注册表和 BlockRenderManager 加载 BakedModel。
     * <p>
     * 流程：Identifier → Registries.BLOCK.get() → BlockState
     * → BlockRenderManager.getModel(state)
     * </p>
     * <p>
     * 这是 Minecraft 内部使用的正规模型获取方式，
     * 避免了 BakedModelManager.getModel(Identifier)
     * 因 ModelIdentifier hashCode 不匹配导致的 null 问题。
     * </p>
     *
     * @param blockId 方块注册表 ID
     * @return BakedModel 实例，或 null
     */
    private BakedModel loadModelFromBlock(Identifier blockId) {
        try {
            Thread currentThread = Thread.currentThread();
            LOGGER.info("🔍 线程: {} | 尝试方块: {}", currentThread.getName(), blockId);

            // 步骤 1：从注册表查找方块
            net.minecraft.block.Block block = Registries.BLOCK.get(blockId);
            if (block == Blocks.AIR) {
                LOGGER.warn("⚠️ 方块未注册或为空气: {}", blockId);
                return null;
            }
            LOGGER.info("   找到方块: {}", block.getClass().getSimpleName());

            // 步骤 2：获取默认 BlockState
            BlockState state = block.getDefaultState();

            // 步骤 3：通过 BlockRenderManager 获取 BakedModel
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                LOGGER.error("❌ MinecraftClient 为 null!");
                return null;
            }

            BakedModel model = mc.getBlockRenderManager().getModel(state);
            if (model != null) {
                LOGGER.info("✅ 模型加载成功: {} (class={})",
                    blockId, model.getClass().getSimpleName());
                return model;
            }

            LOGGER.warn("⚠️ BlockRenderManager.getModel() 返回 null: {}", blockId);
            return null;

        } catch (Exception e) {
            LOGGER.error("❌ 加载方块模型异常: {}", blockId, e);
            return null;
        }
    }

    /**
     * 发射单个子模型的面片。
     * <p>
     * 应用缩放、旋转、位置变换后发射面片到渲染上下文。
     * </p>
     *
     * @param subModel  已加载的子模型
     * @param transform 变换矩阵
     * @param context   渲染上下文
     */
    private void emitSubModelQuads(BakedModel subModel, Transform transform, RenderContext context) {
        Vec3d position = transform.position();
        Vec3d rotation = transform.rotation();
        float scale = transform.scale();

        // 应用子模型变换
        context.pushTransform(quad -> {
            for (int i = 0; i < 4; i++) {
                float x = quad.x(i);
                float y = quad.y(i);
                float z = quad.z(i);

                // 1. 缩放（围绕原点）
                x *= scale;
                y *= scale;
                z *= scale;

                // 2. 旋转（Y 轴）
                float cosY = (float) Math.cos(rotation.y);
                float sinY = (float) Math.sin(rotation.y);
                float tempX = x * cosY - z * sinY;
                float tempZ = x * sinY + z * cosY;
                x = tempX;
                z = tempZ;

                // 3. 旋转（X 轴）
                float cosX = (float) Math.cos(rotation.x);
                float sinX = (float) Math.sin(rotation.x);
                float tempY = y * cosX - z * sinX;
                z = y * sinX + z * cosX;
                y = tempY;

                // 4. 旋转（Z 轴）
                float cosZ = (float) Math.cos(rotation.z);
                float sinZ = (float) Math.sin(rotation.z);
                tempX = x * cosZ - y * sinZ;
                y = x * sinZ + y * cosZ;
                x = tempX;

                // 5. 位置偏移
                x += position.x;
                y += position.y;
                z += position.z;

                quad.pos(i, x, y, z);
            }
            return true;
        });

        // 发射子模型的所有面片
        subModel.emitBlockQuads(
            null,  // blockView（子模型不需要世界上下文）
            null,  // state
            null,  // pos
            () -> Random.create(42L),  // 固定随机种子保证一致性
            context
        );

        context.popTransform();
    }
}