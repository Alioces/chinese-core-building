package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.client.renderer.pipeline.BlockEntityRendererRegistry;
import com.chinesecorebuilding.client.renderer.pipeline.UnifiedRenderPipelineRegistry;
import com.chinesecorebuilding.client.renderer.pipeline.context.BlockEntityRendererContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;
import java.util.function.Function;

/**
 * 文字层渲染器（DECORATOR 层）。
 * <p>
 * <b>设计原则</b>：本渲染器作为文字层的 DECORATOR（装饰器），职责是"在局部空间产生文字顶点"，
 * <b>不应该</b>关心旋转/偏移等 TRANSFORMER（变换层）的逻辑。
 * </p>
 * <p>
 * <b>实现方式：</b>通过 {@link UnifiedRenderPipelineRegistry} 创建完整装饰器链，
 * 自动应用 RotationDecorator 和 OffsetDecorator，保证与 BakedModel 管线同步。
 * </p>
 * <p>
 * 本渲染器是公共文字功能基础设施的一部分，任何实现 {@link com.chinesecorebuilding.block.properties.model.SignTextProvider} 的方块
 * 都可以复用此渲染器进行文字层的渲染。
 * </p>
 *
 * @param <T> 渲染的 BlockEntity 类型（TextBlockEntity 或其子类）
 * @see com.chinesecorebuilding.block.properties.model.SignTextProvider
 */
public class TextBlockRenderer<T extends TextBlockEntity> implements BlockEntityRenderer<T> {

    /**
     * 向注册中心提交文字渲染器工厂。
     * <p>
     * 静态块在类加载时执行，将 "text" 标识与 TextBlockRenderer 工厂绑定。
     * 方块实现 SignTextProvider 接口即自动获得此渲染器。
     * </p>
     */
    static {
        BlockEntityRendererRegistry.register("text", TextBlockRenderer::new);
    }

    /**
     * 文字装饰器实例（通过构造函数初始化，避免 MinecraftClient.getInstance() 返回 null）。
     */
    private final com.chinesecorebuilding.client.renderer.pipeline.decorator.TextDecorator textDecorator;

    /**
     * 构造函数。
     *
     * @param ctx BlockEntityRenderer 工厂上下文（提供 textRenderer）
     */
    public TextBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        this.textDecorator = new com.chinesecorebuilding.client.renderer.pipeline.decorator.TextDecorator(
                ctx.getTextRenderer()
        );
    }

    /**
     * 渲染 TextBlockEntity 的文字层。
     * <p>
     * 通过 UnifiedRenderPipelineRegistry 创建完整装饰器链，
     * 自动应用 RotationDecorator 和 OffsetDecorator，保证与 BakedModel 管线同步。
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

        BlockState state = entity.getCachedState();
        Block block = state.getBlock();

        Function<BlockEntityRendererContext<?>, Void> textRender = ctx -> {
            if (ctx.entity() instanceof TextBlockEntity textEntity) {
                List<TextLine> textLines = textEntity.getLines();
                if (!textLines.isEmpty()) {
                    renderTextLines(textLines, ctx);
                }
            }
            return null;
        };

        var context = new BlockEntityRendererContext<>(
                entity, tickDelta, matrices, vertexConsumers, light, overlay
        );

        Function<BlockEntityRendererContext<?>, Void> chain =
                UnifiedRenderPipelineRegistry.createBlockEntityChain(block, state, textRender);
        chain.apply(context);
    }

    /**
     * 渲染文字行列表。
     * <p>
     * 委托给 TextDecorator 执行实际文字渲染。
     * </p>
     *
     * @param lines   文字行列表
     * @param context 渲染上下文
     */
    private void renderTextLines(List<TextLine> lines, BlockEntityRendererContext<?> context) {
        textDecorator.renderTextLines(lines, context);
    }
}