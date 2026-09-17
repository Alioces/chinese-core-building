package com.chinesecorebuilding.client.model;

import com.chinesecorebuilding.block.properties.model.ModelBakeDecorator;
import com.chinesecorebuilding.client.model.postProcessing.SubModelBakedModel;
import com.chinesecorebuilding.util.BakedModelSpec;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

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
     * 必须在 registerAll() 之后注册，确保 SubModelBakedModel 在装饰链最内侧。
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
                if (!spec.getSubModels().isEmpty()) {
                    result = new SubModelBakedModel(result, spec.getSubModels());
                }

                // TODO: 后续扩展 — EmissiveBakedModel / AnimatedBakedModel / DynamicTextureBakedModel
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
                    return entry.wrapper().apply(original);
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