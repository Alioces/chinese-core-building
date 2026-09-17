package com.chinesecorebuilding.client.model;

import com.chinesecorebuilding.block.properties.model.DynamicModelDecorator;
import com.chinesecorebuilding.block.properties.model.ModelBakeDecorator;
import com.chinesecorebuilding.client.model.postProcessing.SubModelBakedModel;
import com.chinesecorebuilding.util.BakedModelSpec;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 模型后处理插件注册中心——分两阶段架构。
 * <p>
 * 装饰阶段（DECORATOR）先执行，往模型追加几何数据；
 * 变换阶段（TRANSFORMER）后执行，对所有顶点做矩阵变换。
 * 包装顺序：Transform(Decorator(Original))，装饰层自动跟随变换。
 * </p>
 */
public final class ModelPluginRegistry {

    /**
     * 插件条目。
     */
    private record Entry(Predicate<Block> matcher, Function<BakedModel, BakedModel> wrapper) {}

    /**
     * 阶段标记枚举。
     */
    public enum Phase {
        /** 模型烘焙修饰阶段，往顶点流追加几何数据。 */
        DECORATOR,
        /** 世界坐标最终处理阶段，对所有顶点做矩阵变换。 */
        TRANSFORMER
    }

    /** 装饰阶段插件列表。 */
    private static final List<Entry> DECORATORS = new ArrayList<>();

    /** 变换阶段插件列表。 */
    private static final List<Entry> TRANSFORMERS = new ArrayList<>();

    /** 日志记录器，用于诊断模型包装链。 */
    private static final Logger LOGGER = LoggerFactory.getLogger("ModelPluginRegistry");

    /**
     * 注册一个模型插件。
     */
    public static void register(Phase phase, Predicate<Block> matcher, Function<BakedModel, BakedModel> wrapper) {
        switch (phase) {
            case DECORATOR   -> DECORATORS.add(new Entry(matcher, wrapper));
            case TRANSFORMER -> TRANSFORMERS.add(new Entry(matcher, wrapper));
        }
    }

    /**
     * 将所有已注册的插件注册到 Fabric 模型加载管线。
     * 装饰阶段先注册，变换阶段后注册。
     */
    public static void registerAll() {
        for (Entry entry : DECORATORS) {
            registerPlugin(entry);
        }
        for (Entry entry : TRANSFORMERS) {
            registerPlugin(entry);
        }
    }

    /**
     * 注册模型烘焙插件链桥接（ModelBakePluginRegistry → Fabric 管线）。
     * <p>
     * 检测实现 ModelBakeDecorator 的方块，执行烘焙链生成 BakedModelSpec，
     * 并用 SubModelBakedModel 包装含子模型的模型。
     * </p>
     * <p>
     * 必须在 {@link #registerAll()} 之前注册，确保 SubModelBakedModel（装饰器）
     * 在变换层（RotationBakedModel/OffsetBakedModel）内侧。
     * 回调注册顺序：先注册 = 内层，后注册 = 外层包装。
     * 最终包装顺序：Offset(Rotation(Text(SubModel(原模型))))。
     * </p>
     */
    public static void registerBakeChainBridge() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelAfterBake().register((original, context) -> {
                Block block = resolveBlock(context.id());
                if (block == null || !(block instanceof ModelBakeDecorator)) {
                    return original;
                }

                BakedModelSpec spec = ModelBakePluginRegistry.bakeChain(
                    block, block.getDefaultState(), new BakedModelSpec(context.id())
                );

                BakedModel result = original;

                // 静态/动态分支：实现 DynamicModelDecorator 的方块走延迟求值路径
                boolean isDynamic = block instanceof DynamicModelDecorator;
                boolean hasSubModels = !spec.getSubModels().isEmpty()
                        || (isDynamic && spec.getDeferredSubModelHandler() != null);

                if (hasSubModels) {
                    if (isDynamic) {
                        // 动态路径：传入完整 spec，渲染时根据实际 BlockState 动态计算
                        result = new SubModelBakedModel(result, spec);
                        LOGGER.info("[bakeChainBridge] model={}, dynamic=true, deferredHandler={}",
                                context.id(), spec.getDeferredSubModelHandler() != null);
                    } else {
                        // 静态路径：仅传入预计算列表，零运行时开销
                        result = new SubModelBakedModel(result, spec.getSubModels());
                        LOGGER.info("[bakeChainBridge] model={}, static=true, subModels={}",
                                context.id(), spec.getSubModels().size());
                    }
                }

                return result;
            });
        });
    }

    /**
     * 将单个插件包装为 Fabric ModelLoadingPlugin。
     */
    private static void registerPlugin(Entry entry) {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelAfterBake().register((original, context) -> {
                Block block = resolveBlock(context.id());
                if (block != null && entry.matcher().test(block)) {
                    BakedModel wrapped = entry.wrapper().apply(original);
                    LOGGER.info("[registerPlugin] model={}, wrapped={}({})",
                            context.id(),
                            wrapped.getClass().getSimpleName(),
                            original.getClass().getSimpleName());
                    return wrapped;
                }
                return original;
            });
        });
    }

    /**
     * 从模型 ID 解析方块实例。
     * 模型 ID 格式：namespace:block/block_name 或 namespace:item/item_name。
     */
    static Block resolveBlock(Identifier modelId) {
        if (!modelId.getPath().startsWith("block/")) return null;
        String blockPath = modelId.getPath().substring("block/".length());
        Identifier blockId = new Identifier(modelId.getNamespace(), blockPath);
        return Registries.BLOCK.get(blockId);
    }

    /** 工具类不允许实例化。 */
    private ModelPluginRegistry() {}
}