package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.Transform;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.BakedModel;
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
     * 子模型规格列表（未解析的 Identifier）。
     */
    private final List<BakedModelSpec.SubModelEntry> subModelEntries;

    /**
     * 线程安全的模型缓存（Identifier → BakedModel）。
     */
    private final Map<Identifier, BakedModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 标记是否已完成初始化。
     */
    private volatile boolean initialized = false;

    /**
     * 构造函数。
     *
     * @param original        原始烘焙模型
     * @param subModelEntries 子模型规格列表（未解析）
     */
    public SubModelBakedModel(BakedModel original, List<BakedModelSpec.SubModelEntry> subModelEntries) {
        this.wrapped = original;
        this.subModelEntries = subModelEntries;
    }

    /**
     * 发射原始模型和所有子模型的面片。
     * 首次调用时触发子模型的延迟加载。
     */
    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        if (!initialized) {
            initializeSubModels();
        }

        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);

        for (BakedModelSpec.SubModelEntry entry : subModelEntries) {
            BakedModel cachedModel = modelCache.get(entry.modelId());
            if (cachedModel != null) {
                emitSubModelQuads(cachedModel, entry.transform(), context);
            }
        }
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
                float cosY = (float) Math.cos(rotation.y);
                float sinY = (float) Math.sin(rotation.y);
                float tempX = x * cosY - z * sinY;
                float tempZ = x * sinY + z * cosY;
                x = tempX;
                z = tempZ;

                // 3. X 轴旋转
                float cosX = (float) Math.cos(rotation.x);
                float sinX = (float) Math.sin(rotation.x);
                float tempY = y * cosX - z * sinX;
                z = y * sinX + z * cosX;
                y = tempY;

                // 4. Z 轴旋转
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