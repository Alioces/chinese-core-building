package com.chinesecorebuilding.client.renderer.pipeline;

import com.chinesecorebuilding.client.renderer.pipeline.api.BakedModelDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.api.BlockEntityDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.context.BakedModelContext;
import com.chinesecorebuilding.client.renderer.pipeline.context.BlockEntityRendererContext;
import com.chinesecorebuilding.client.renderer.pipeline.decorator.OffsetDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.decorator.RotationDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.decorator.TextDecorator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 统一渲染管线装饰器注册中心。
 * <p>
 * 管理所有装饰器的注册和链式调用。
 * 根据方块实现的接口自动组装装饰器链。
 * </p>
 * <p>
 * <b>装饰顺序：</b>
 * <pre>
 * OffsetDecorator (ModelWorldTransformer)
 *   └─ RotationDecorator (ModelWorldTransformer)
 *      └─ TextDecorator (ModelBakeDecorator)
 *         └─ SubModelDecorator (ModelBakeDecorator)
 *            └─ 原始模型/渲染器
 * </pre>
 * </p>
 */
public final class UnifiedRenderPipelineRegistry {

    /** BakedModel 装饰器列表。 */
    private static final List<BakedModelDecorator<Void>> bakedModelDecorators = new ArrayList<>();

    /** BlockEntityRenderer 装饰器列表。 */
    private static final List<BlockEntityDecorator<Void>> blockEntityDecorators = new ArrayList<>();

    /** 延迟初始化的文字装饰器。 */
    private static TextDecorator textDecorator;

    /**
     * 获取或创建文字装饰器（延迟初始化，避免 MinecraftClient 未就绪时 textRenderer 为 null）。
     *
     * @return 文字装饰器实例
     */
    private static TextDecorator getTextDecorator() {
        if (textDecorator == null) {
            textDecorator = new TextDecorator(MinecraftClient.getInstance().textRenderer);
        }
        return textDecorator;
    }

    /**
     * 注册 BakedModel 装饰器。
     *
     * @param decorator 装饰器实例
     */
    public static void registerBakedModelDecorator(BakedModelDecorator<Void> decorator) {
        bakedModelDecorators.add(decorator);
    }

    /**
     * 注册 BlockEntityRenderer 装饰器。
     *
     * @param decorator 装饰器实例
     */
    public static void registerBlockEntityDecorator(BlockEntityDecorator<Void> decorator) {
        blockEntityDecorators.add(decorator);
    }

    /**
     * 为指定方块创建 BakedModel 装饰器链。
     *
     * @param block      方块实例
     * @param state      方块状态
     * @param baseRender 基础渲染函数
     * @return 装饰后的渲染函数
     */
    public static Function<BakedModelContext, Void> createBakedModelChain(
            Block block,
            BlockState state,
            Function<BakedModelContext, Void> baseRender) {

        var applicableDecorators = new ArrayList<BakedModelDecorator<Void>>();

        for (var decorator : bakedModelDecorators) {
            if (decorator.accepts(state)) {
                applicableDecorators.add(decorator);
            }
        }

        return BakedModelDecorator.createChain(applicableDecorators, baseRender);
    }

    /**
     * 为指定方块创建 BlockEntityRenderer 装饰器链。
     *
     * @param block      方块实例
     * @param state      方块状态
     * @param baseRender 基础渲染函数
     * @return 装饰后的渲染函数
     */
    public static Function<BlockEntityRendererContext<?>, Void> createBlockEntityChain(
            Block block,
            BlockState state,
            Function<BlockEntityRendererContext<?>, Void> baseRender) {

        var applicableDecorators = new ArrayList<BlockEntityDecorator<Void>>();

        for (var decorator : blockEntityDecorators) {
            if (decorator.accepts(state)) {
                applicableDecorators.add(decorator);
            }
        }

        return BlockEntityDecorator.createChain(applicableDecorators, baseRender);
    }

    /**
     * 初始化所有内置装饰器。
     * <p>
     * 在客户端初始化时调用一次，注册所有装饰器到对应管线。
     * 装饰器实例在两条管线间共享，保证变换逻辑完全一致。
     * </p>
     */
    public static void initialize() {
        var rotationDecorator = new RotationDecorator();
        var offsetDecorator = new OffsetDecorator();

        registerBakedModelDecorator(rotationDecorator);
        registerBakedModelDecorator(offsetDecorator);

        registerBlockEntityDecorator(rotationDecorator);
        registerBlockEntityDecorator(offsetDecorator);
    }

    /** 工具类不允许实例化。 */
    private UnifiedRenderPipelineRegistry() {}
}