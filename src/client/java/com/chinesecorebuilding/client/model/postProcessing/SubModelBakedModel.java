package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.util.AnchorPoint;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.Transform;
import com.chinesecorebuilding.util.handler.SubModelHandler;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.Identifier;
import net.minecraft.world.BlockRenderView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 子模型渲染包装器。
 * <p>
 * 采用"延迟初始化 + 线程安全缓存"策略：
 * 构造函数仅存储 Identifier，首次渲染时加载并缓存 BakedModel。
 * </p>
 */
public class SubModelBakedModel extends ForwardingBakedModel {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("SubModel");

    /**
     * 延迟求值：子模型处理器（渲染时根据实际 BlockState 计算子模型 ID）。
     */
    private final SubModelHandler deferredSubModelHandler;

    /**
     * 延迟求值：锚点列表（配合 {@link #deferredSubModelHandler} 使用）。
     */
    private final List<AnchorPoint> deferredAnchors;

    /**
     * 子模型规格列表（未解析的 Identifier）。立即求值模式时使用。
     */
    private final List<BakedModelSpec.SubModelEntry> subModelEntries;

    /**
     * 线程安全的模型缓存（Identifier → BakedModel）。
     */
    private final Map<Identifier, BakedModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 延迟求值缓存（BlockState → 子模型列表），避免每帧重复调用 handler。
     */
    private final Map<BlockState, List<BakedModelSpec.SubModelEntry>> deferredCache = new ConcurrentHashMap<>();

    /**
     * 标记是否已完成初始化。
     */
    private volatile boolean initialized = false;

    /**
     * 构造函数（立即求值模式，向后兼容）。
     *
     * @param original        原始烘焙模型
     * @param subModelEntries 子模型规格列表（未解析）
     */
    public SubModelBakedModel(BakedModel original, List<BakedModelSpec.SubModelEntry> subModelEntries) {
        this.wrapped = original;
        this.subModelEntries = subModelEntries;
        this.deferredSubModelHandler = null;
        this.deferredAnchors = null;
    }

    /**
     * 构造函数（完整模式，同时支持立即求值和延迟求值）。
     * <p>
     * 内层包装：传入的 original 应是 SubModelBakedModel 的父模型原始渲染结果。
     * 外层包装：本模型通过 RotationBakedModel 等待变换阶段应用旋转。
     * </p>
     *
     * @param original 原始烘焙模型
     * @param spec     完整的烘焙模型规格（包含子模型列表和延迟 handler）
     */
    public SubModelBakedModel(BakedModel original, BakedModelSpec spec) {
        this.wrapped = original;
        this.subModelEntries = spec.getSubModels();
        this.deferredSubModelHandler = spec.getDeferredSubModelHandler();
        this.deferredAnchors = spec.getDeferredAnchors();
    }

    /**
     * 始终返回 false，强制使用 Fabric 渲染管线。
     * <p>
     * 返回 true 会导致 Fabric 使用混合管线（Fabric 发射 + 原版剔除），
     * 在相邻方块场景下剔除逻辑会检查错误的方块位置。
     * </p>
     */
    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    /**
     * 发射原始模型和所有子模型的面片。
     * 首次调用时触发子模型的延迟加载。
     * <p>
     * 父模型面片通过 {@link QuadEmitter} 从 {@link BakedModel#getQuads} 逐面片发射，
     * 确保穿过 {@link RenderContext#pushTransform} 注入的变换栈（如 RotationBakedModel 的旋转）。
     * 不能使用已废弃的 {@code fallbackConsumer()}（参数类型为 BakedModel 而非 BakedQuad）。
     * </p>
     */
    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        if (!initialized) {
            initializeSubModels();
        }

        // 诊断日志：记录渲染入口信息
        LOGGER.info("[emitBlockQuads] pos={}, state={}, wrapped.class={}, deferredHandler={}, loadedModels={}",
                pos, state,
                wrapped != null ? wrapped.getClass().getSimpleName() : "null",
                deferredSubModelHandler != null,
                modelCache.size());

        // 通过 QuadEmitter 逐面片发射父模型，确保穿过变换栈（pushTransform）
        QuadEmitter emitter = context.getEmitter();
        Random random = randomSupplier.get();
        RenderMaterial material = RendererAccess.INSTANCE.getRenderer()
                .materialById(RenderMaterial.MATERIAL_STANDARD);
        int quadCount = 0;
        for (int nullSide = 0; nullSide <= 6; nullSide++) {
            Direction side = nullSide == 0 ? null : Direction.byId(nullSide - 1);
            for (BakedQuad quad : wrapped.getQuads(state, side, random)) {
                emitter.fromVanilla(quad, material, side).emit();
                quadCount++;
            }
        }
        LOGGER.info("[emitBlockQuads] emitted {} parent quads via QuadEmitter", quadCount);

        // 解析当前状态对应的子模型列表：优先使用延迟 handler，回退到立即求值列表
        List<BakedModelSpec.SubModelEntry> entries = resolveSubModelEntries(state);

        for (BakedModelSpec.SubModelEntry entry : entries) {
            BakedModel cachedModel = modelCache.get(entry.modelId());
            if (cachedModel != null) {
                emitSubModelQuads(cachedModel, entry.transform(), context);
            } else {
                // 延迟加载：尝试加载子模型并缓存
                BakedModel loaded = loadModelWithFallback(entry.modelId());
                if (loaded != null) {
                    modelCache.put(entry.modelId(), loaded);
                    emitSubModelQuads(loaded, entry.transform(), context);
                } else {
                    LOGGER.warn("[emitBlockQuads] subModel not found: {}", entry.modelId());
                }
            }
        }
    }

    /**
     * 解析当前方块状态对应的子模型条目列表。
     * <p>
     * 优先使用延迟 handler（根据实际 BlockState 动态计算），回退到立即求值的 subModelEntries。
     * 延迟 handler 的结果会缓存以 {@link BlockState} 为 key 避免重复计算。
     * </p>
     *
     * @param state 当前方块状态
     * @return 子模型条目列表
     */
    private List<BakedModelSpec.SubModelEntry> resolveSubModelEntries(BlockState state) {
        // 如果注册了延迟 handler，根据实际 BlockState 动态计算
        if (deferredSubModelHandler != null && deferredAnchors != null && state != null) {
            return deferredCache.computeIfAbsent(state, s -> {
                List<BakedModelSpec.SubModelEntry> entries = new ArrayList<>();
                for (AnchorPoint anchor : deferredAnchors) {
                    String modelId = deferredSubModelHandler.handle(anchor, s);
                    if (modelId != null && !modelId.isEmpty()) {
                        entries.add(new BakedModelSpec.SubModelEntry(
                                new Identifier(modelId), anchor.transform()));
                    }
                }
                LOGGER.info("[resolveSubModelEntries] deferred: state={}, entries={}", s, entries.size());
                return entries;
            });
        }
        // 回退到立即求值的列表（向后兼容）
        return subModelEntries;
    }

    /**
     * 延迟初始化子模型（线程安全，仅执行一次）。
     * 模型加载必须在 Render thread 执行，Worker 线程中 BakedModelManager 不可用。
     */
    private void initializeSubModels() {
        synchronized (this) {
            if (initialized) {
                return;
            }

            boolean isMainThread = Thread.currentThread().getName().equals("Render thread");
            if (isMainThread) {
                loadAllSubModels();
            } else {
                submitToMainThread();
            }

            initialized = true;
        }
    }

    /**
     * 加载所有子模型并缓存。
     */
    private void loadAllSubModels() {
        for (BakedModelSpec.SubModelEntry entry : subModelEntries) {
            try {
                BakedModel model = loadModelWithFallback(entry.modelId());
                if (model != null) {
                    modelCache.put(entry.modelId(), model);
                } else {
                    LOGGER.error("子模型加载失败: {}", entry.modelId());
                }
            } catch (Exception e) {
                LOGGER.error("加载子模型异常: {}", entry.modelId(), e);
            }
        }
    }

    /**
     * 将模型加载提交到主线程并等待完成（5 秒超时）。
     */
    private void submitToMainThread() {
        final CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] errorHolder = new Throwable[1];

        try {
            MinecraftClient.getInstance().execute(() -> {
                try {
                    loadAllSubModels();
                } catch (Throwable t) {
                    errorHolder[0] = t;
                } finally {
                    latch.countDown();
                }
            });

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            if (!completed) {
                LOGGER.error("等待主线程加载超时（5秒）");
            } else if (errorHolder[0] != null) {
                LOGGER.error("主线程加载失败", errorHolder[0]);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("等待主线程时被中断", e);
        }
    }

    /**
     * 通过方块注册表加载 BakedModel，支持多种 ID 格式回退。
     */
    private BakedModel loadModelWithFallback(Identifier modelId) {
        String namespace = modelId.getNamespace();
        String path = modelId.getPath();

        // 尝试 1：直接用 path 查找方块
        Identifier blockId = new Identifier(namespace, path);
        BakedModel model = loadModelFromBlock(blockId);
        if (model != null) return model;

        // 尝试 2：如果 path 以 block/ 开头，去掉前缀后重试
        if (path.startsWith("block/")) {
            return loadModelFromBlock(new Identifier(namespace, path.substring(6)));
        }

        // 尝试 3：添加 block/ 前缀
        return loadModelFromBlock(new Identifier(namespace, "block/" + path));
    }

    /**
     * 通过方块注册表和 BlockRenderManager 加载 BakedModel。
     */
    private BakedModel loadModelFromBlock(Identifier blockId) {
        try {
            net.minecraft.block.Block block = Registries.BLOCK.get(blockId);
            if (block == Blocks.AIR) {
                return null;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                return null;
            }

            return mc.getBlockRenderManager().getModel(block.getDefaultState());
        } catch (Exception e) {
            LOGGER.error("加载方块模型异常: {}", blockId, e);
            return null;
        }
    }

    /**
     * 应用变换后发射子模型面片。
     */
    private void emitSubModelQuads(BakedModel subModel, Transform transform, RenderContext context) {
        Vec3d position = transform.position();
        Vec3d rotation = transform.rotation();
        float scale = transform.scale();

        // 预计算旋转矩阵分量
        float cosX = (float) Math.cos(rotation.x);
        float sinX = (float) Math.sin(rotation.x);
        float cosY = (float) Math.cos(rotation.y);
        float sinY = (float) Math.sin(rotation.y);
        float cosZ = (float) Math.cos(rotation.z);
        float sinZ = (float) Math.sin(rotation.z);

        context.pushTransform(quad -> {
            for (int i = 0; i < 4; i++) {
                float x = quad.x(i);
                float y = quad.y(i);
                float z = quad.z(i);

                // 1. 缩放
                x *= scale;
                y *= scale;
                z *= scale;

                // 2. Y 轴旋转
                float tempX = x * cosY - z * sinY;
                float tempZ = x * sinY + z * cosY;
                x = tempX;
                z = tempZ;

                // 3. X 轴旋转
                float tempY = y * cosX - z * sinX;
                z = y * sinX + z * cosX;
                y = tempY;

                // 4. Z 轴旋转
                tempX = x * cosZ - y * sinZ;
                y = x * sinZ + y * cosZ;
                x = tempX;

                // 5. 位置偏移
                x += position.x;
                y += position.y;
                z += position.z;

                quad.pos(i, x, y, z);
            }

            // 同步变换法线方向（与顶点使用相同的旋转）
            for (int i = 0; i < 4; i++) {
                float nx = quad.normalX(i);
                float ny = quad.normalY(i);
                float nz = quad.normalZ(i);

                // Y 轴旋转
                float tempNx = nx * cosY - nz * sinY;
                float tempNz = nx * sinY + nz * cosY;
                nx = tempNx;
                nz = tempNz;

                // X 轴旋转
                float tempNy = ny * cosX - nz * sinX;
                nz = ny * sinX + nz * cosX;
                ny = tempNy;

                // Z 轴旋转
                tempNx = nx * cosZ - ny * sinZ;
                ny = nx * sinZ + ny * cosZ;
                nx = tempNx;

                quad.normal(i, nx, ny, nz);
            }

            // 清除剔除面信息，防止子模型面被相邻方块错误剔除
            quad.cullFace(null);

            return true;
        });

        subModel.emitBlockQuads(
            null,
            null,
            null,
            () -> Random.create(42L),
            context
        );

        context.popTransform();
    }
}