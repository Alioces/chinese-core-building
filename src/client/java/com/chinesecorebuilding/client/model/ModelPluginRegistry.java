package com.chinesecorebuilding.client.model;

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
 * 模型后处理插件注册中心。
 * <p>
 * 采用链式架构，每个插件只负责单一变换（旋转、偏移等）。
 * 多个插件自动串联，变换效果依次叠加：一个方块可同时命中多个插件。
 * </p>
 * <p>
 * 使用方式：
 * <ol>
 *     <li>通过 {@link #register} 注册插件（匹配条件 + 包装函数）</li>
 *     <li>调用 {@link #registerAll} 将所有插件注册到 Fabric 模型加载管线</li>
 * </ol>
 * </p>
 */
public final class ModelPluginRegistry {

    /**
     * 插件条目：匹配条件 + 模型包装函数。
     */
    private record Entry(Predicate<Block> matcher, Function<BakedModel, BakedModel> wrapper) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    /**
     * 注册一个模型后处理插件。
     * <p>
     * 每个插件由两部分组成：
     * <ul>
     *     <li>{@code matcher} — 方块匹配条件（如 {@code block instanceof Rotatable}）</li>
     *     <li>{@code wrapper} — 模型包装函数（如 {@code RotationBakedModel::new}）</li>
     * </ul>
     * 匹配成功的方块模型会被包装，未匹配的保持原样传递给下一个插件。
     * </p>
     *
     * @param matcher 方块匹配谓词
     * @param wrapper 模型包装函数
     */
    public static void register(Predicate<Block> matcher, Function<BakedModel, BakedModel> wrapper) {
        ENTRIES.add(new Entry(matcher, wrapper));
    }

    /**
     * 将所有已注册的插件注册到 Fabric 模型加载管线。
     * <p>
     * 每个插件注册为独立的 {@link ModelLoadingPlugin}，
     * Fabric 会自动链式调用，变换效果依次叠加。
     * 应在客户端初始化阶段调用一次。
     * </p>
     */
    public static void registerAll() {
        for (Entry entry : ENTRIES) {
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
    }

    /**
     * 从模型 ID 解析对应的方块实例。
     * <p>
     * 模型 ID 格式为 {@code namespace:block/block_name}，
     * 去掉 {@code block/} 前缀后从方块注册表中查找。
     * </p>
     *
     * @param modelId 模型 ID
     * @return 对应的方块实例，若非方块模型或不存在则返回 null
     */
    private static Block resolveBlock(Identifier modelId) {
        if (!modelId.getPath().startsWith("block/")) return null;
        String blockPath = modelId.getPath().substring("block/".length());
        Identifier blockId = new Identifier(modelId.getNamespace(), blockPath);
        return Registries.BLOCK.get(blockId);
    }

    private ModelPluginRegistry() {}
}
