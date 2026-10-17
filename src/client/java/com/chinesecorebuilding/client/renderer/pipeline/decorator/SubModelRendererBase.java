package com.chinesecorebuilding.client.renderer.pipeline.decorator;

import com.chinesecorebuilding.util.AnchorPoint;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.handler.SubModelHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 子模型渲染公共基类。
 * <p>
 * 提取 SubModelDecorator 和 SubModelBakedModel 的公共逻辑，消除重复代码：
 * <ul>
 *   <li>子模型配置（立即求值/延迟求值）</li>
 *   <li>线程安全的模型缓存</li>
 *   <li>延迟求值缓存</li>
 *   <li>模型加载与回退逻辑</li>
 *   <li>子模型条目解析</li>
 * </ul>
 * </p>
 * <p>
 * 子类需实现 {@link #loadModelWithFallback(Identifier)} 提供具体的模型加载策略。
 * </p>
 */
public abstract class SubModelRendererBase {

    /**
     * 延迟求值：子模型处理器（渲染时根据实际 BlockState 计算子模型 ID）。
     */
    protected final SubModelHandler deferredSubModelHandler;

    /**
     * 延迟求值：锚点列表（配合 {@link #deferredSubModelHandler} 使用）。
     */
    protected final List<AnchorPoint> deferredAnchors;

    /**
     * 子模型规格列表（未解析的 Identifier）。立即求值模式时使用。
     */
    protected final List<BakedModelSpec.SubModelEntry> subModelEntries;

    /**
     * 线程安全的模型缓存（Identifier → BakedModel）。
     */
    protected final Map<Identifier, BakedModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 延迟求值缓存（BlockState → 子模型列表），避免每帧重复调用 handler。
     */
    protected final Map<BlockState, List<BakedModelSpec.SubModelEntry>> deferredCache = new ConcurrentHashMap<>();

    /**
     * 标记是否已完成初始化。
     */
    protected volatile boolean initialized = false;

    /**
     * 构造函数（立即求值模式，向后兼容）。
     *
     * @param subModelEntries 子模型规格列表（未解析）
     */
    protected SubModelRendererBase(List<BakedModelSpec.SubModelEntry> subModelEntries) {
        this.subModelEntries = subModelEntries;
        this.deferredSubModelHandler = null;
        this.deferredAnchors = null;
    }

    /**
     * 构造函数（延迟求值模式）。
     *
     * @param subModelHandler 子模型处理器
     * @param anchors         锚点列表
     */
    protected SubModelRendererBase(SubModelHandler subModelHandler, List<AnchorPoint> anchors) {
        this.deferredSubModelHandler = subModelHandler;
        this.deferredAnchors = anchors;
        this.subModelEntries = null;
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
    public List<BakedModelSpec.SubModelEntry> resolveSubModelEntries(BlockState state) {
        if (deferredSubModelHandler != null && deferredAnchors != null && state != null) {
            return deferredCache.computeIfAbsent(state, s -> {
                List<BakedModelSpec.SubModelEntry> entries = new java.util.ArrayList<>();
                for (AnchorPoint anchor : deferredAnchors) {
                    String modelId = deferredSubModelHandler.handle(anchor, s);
                    if (modelId != null && !modelId.isEmpty()) {
                        entries.add(new BakedModelSpec.SubModelEntry(
                                new Identifier(modelId), anchor.transform()));
                    }
                }
                return entries;
            });
        }
        return subModelEntries != null ? subModelEntries : java.util.List.of();
    }

    /**
     * 延迟初始化子模型（线程安全，仅执行一次）。
     * <p>
     * 子类可重写此方法添加额外的初始化逻辑（如提交到主线程）。
     * </p>
     */
    protected void initializeSubModels() {
        synchronized (this) {
            if (initialized) {
                return;
            }

            boolean isMainThread = Thread.currentThread().getName().equals("Render thread");
            if (isMainThread) {
                loadAllSubModels();
            }

            initialized = true;
        }
    }

    /**
     * 加载所有子模型并缓存。
     * <p>
     * 仅适用于立即求值模式（subModelEntries 非空）。
     * </p>
     */
    protected void loadAllSubModels() {
        if (subModelEntries == null) {
            return;
        }

        for (BakedModelSpec.SubModelEntry entry : subModelEntries) {
            try {
                BakedModel model = loadModelWithFallback(entry.modelId());
                if (model != null) {
                    modelCache.put(entry.modelId(), model);
                }
            } catch (Exception e) {
                // 静默处理，渲染时回退
            }
        }
    }

    /**
     * 加载模型，失败时回退。
     * <p>
     * 子类需实现具体的加载策略（如通过 BakedModelManager 或 BlockRenderManager）。
     * </p>
     *
     * @param modelId 模型标识符
     * @return BakedModel 或 null
     */
    protected abstract BakedModel loadModelWithFallback(Identifier modelId);

    /**
     * 判断模型是否为缺失模型。
     * <p>
     * Minecraft 1.20.1 中通过检查模型类名判断是否为 MissingModel。
     * </p>
     *
     * @param model 模型实例
     * @return true 如果是缺失模型
     */
    protected boolean isMissingModel(BakedModel model) {
        return model != null && model.getClass().getSimpleName().contains("Missing");
    }

    /**
     * 通过 BakedModelManager 加载模型（公共实现）。
     * <p>
     * 适用于 SubModelDecorator 的加载策略。
     * </p>
     *
     * @param modelId 模型标识符
     * @return BakedModel 或 null
     */
    protected BakedModel loadModelViaManager(Identifier modelId) {
        var modelManager = MinecraftClient.getInstance().getBakedModelManager();
        if (modelManager == null) {
            return null;
        }

        BakedModel model = modelManager.getModel(modelId);
        if (model == null || isMissingModel(model)) {
            String blockPath = modelId.getPath().replaceFirst("^(block|item)/", "");
            Identifier blockModelId = new Identifier(modelId.getNamespace(), "block/" + blockPath);
            model = modelManager.getModel(blockModelId);
        }

        return (model != null && !isMissingModel(model)) ? model : null;
    }
}