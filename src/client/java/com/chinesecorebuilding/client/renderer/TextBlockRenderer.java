package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.client.renderer.pipeline.BlockEntityDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.BlockEntityRendererContext;
import com.chinesecorebuilding.client.renderer.pipeline.TextDecorator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

/**
 * 文字层渲染器（DECORATOR 层）。
 * <p>
 * <b>设计原则</b>：本渲染器作为文字层的 DECORATOR（装饰器），职责是"在局部空间产生文字顶点"，
 * <b>不应该</b>关心旋转/偏移等 TRANSFORMER（变换层）的逻辑。
 * </p>
 * <p>
 * <b>实现方式：</b>委托给 {@link TextDecorator} 执行文字渲染，
 * 通过统一渲染管线装饰器链处理旋转/偏移变换，保证与 BakedModel 管线同步。
 * </p>
 * <p>
 * 本渲染器是公共文字功能基础设施的一部分，任何实现 {@link com.chinesecorebuilding.block.properties.model.SignTextProvider} 的方块
 * 都可以复用此渲染器进行文字层的渲染。
 * </p>
 *
 * @param <T> 渲染的 BlockEntity 类型（TextBlockEntity 或其子类）
 * @see com.chinesecorebuilding.block.properties.model.SignTextProvider
 * @see TextDecorator
 */
public class TextBlockRenderer<T extends TextBlockEntity> implements BlockEntityRenderer<T> {

    /**
     * 文字装饰器实例（单例，共享变换逻辑）。
     */
    private static final BlockEntityDecorator<Void> DECORATOR = new TextDecorator(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer
    );

    /**
     * 构造函数。
     *
     * @param ctx BlockEntityRenderer 工厂上下文
     */
    public TextBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        // 装饰器使用静态单例，不需要实例状态
    }

    /**
     * 渲染 TextBlockEntity 的文字层。
     * <p>
     * 委托给 {@link TextDecorator} 执行文字渲染，
     * 装饰器链自动处理旋转/偏移变换。
     * </p>
     *
     * @param entity          目标 BlockEntity（TextBlockEntity 或其子类）
     * @param tickDelta       帧间插值（0~1），用于平滑动画
     * @param matrices        矩阵栈，承载世界变换（已包含方块旋转/偏移）
     * @param vertexConsumers 顶点消费者提供者，用于获取光照/雾化等渲染上下文
     * @param light           方块光照值
     * @param overlay         方块叠加色（红/伤等效果）
     */
    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        List<TextLine> lines = entity.getLines();
        if (lines.isEmpty()) return;

        var context = new BlockEntityRendererContext<>(
                entity, tickDelta, matrices, vertexConsumers, light, overlay
        );

        DECORATOR.decorate(context, ctx -> null);
    }
}