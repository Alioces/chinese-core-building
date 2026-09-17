package com.chinesecorebuilding.client.model;

import com.chinesecorebuilding.client.model.plugin.AnimationControllerPlugin;
import com.chinesecorebuilding.client.model.plugin.DynamicTexturePlugin;
import com.chinesecorebuilding.client.model.plugin.EmissivePlugin;
import com.chinesecorebuilding.client.model.plugin.MultiBlockCoordinatorPlugin;
import com.chinesecorebuilding.client.model.plugin.SubModelComposerPlugin;
import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.block.properties.model.ModelBakeDecorator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 模型烘焙插件注册中心。
 * 按优先级排序后依次执行，每个插件可修改 BakedModelSpec。
 */
public final class ModelBakePluginRegistry {

    /** 插件列表（按优先级排序，数值越小越先执行）。 */
    private static final List<ModelBakePlugin> plugins = new ArrayList<>();

    /** 注册插件（自动按优先级排序）。 */
    public static void register(ModelBakePlugin plugin) {
        plugins.add(plugin);
        plugins.sort(Comparator.comparingInt(ModelBakePlugin::getPriority));
    }

    /**
     * 执行所有插件，生成最终模型规格。
     * 只有实现了 ModelBakeDecorator 的方块才会触发插件链。
     */
    public static BakedModelSpec bakeChain(Block block, BlockState state, BakedModelSpec baseSpec) {
        if (!(block instanceof ModelBakeDecorator)) {
            return baseSpec;
        }

        BakedModelSpec current = baseSpec;
        BakeContext context = new BakeContext(block, state, current);

        for (ModelBakePlugin plugin : plugins) {
            if (plugin.accepts(block)) {
                current = plugin.bake(context);
                context = new BakeContext(block, state, current);
            }
        }

        return current;
    }

    /**
     * 注册所有内置插件。
     * 优先级：SubModel(-1000) → DynamicTexture(-900) → Animation(-500) → Emissive(-400) → MultiBlock(100)
     */
    public static void registerAll() {
        register(new SubModelComposerPlugin());
        register(new DynamicTexturePlugin());
        register(new AnimationControllerPlugin());
        register(new EmissivePlugin());
        register(new MultiBlockCoordinatorPlugin());
    }

    /**
     * 从模型 ID 解析方块实例。
     * 模型 ID 格式：namespace:block/block_name 或 namespace:item/item_name。
     */
    private static Block resolveBlock(Identifier modelId) {
        if (!modelId.getPath().startsWith("block/")) return null;
        String blockPath = modelId.getPath().substring("block/".length());
        Identifier blockId = new Identifier(modelId.getNamespace(), blockPath);
        return Registries.BLOCK.get(blockId);
    }

    /** 工具类不允许实例化。 */
    private ModelBakePluginRegistry() {}
}