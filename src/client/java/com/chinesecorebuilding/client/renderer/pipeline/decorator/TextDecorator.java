package com.chinesecorebuilding.client.renderer.pipeline.decorator;

import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.block.properties.model.SignTextProvider;
import com.chinesecorebuilding.client.renderer.pipeline.api.BakedModelDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.api.BlockEntityDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.api.UnifiedRenderDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.context.BakedModelContext;
import com.chinesecorebuilding.client.renderer.pipeline.context.BlockEntityRendererContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Function;

/**
 * 文字装饰器实现。
 * <p>
 * 统一处理 BakedModel 和 BlockEntityRenderer 的文字渲染。
 * </p>
 * <p>
 * <b>当前限制：</b>文字数据存储在 TextBlockEntity 中（每个方块实例独有），
 * 无法在 BakedModel 静态烘焙阶段注入。因此 BakedModel 管线中此装饰器仅代理原始模型，
 * 实际文字渲染仍在 BlockEntityRenderer 管线中执行。
 * </p>
 * <p>
 * <b>未来演进：</b>当 Fabric Renderer API 支持动态贴图注入 RenderContext 时，
 * 可升级为在 emitBlockQuads 运行时动态渲染文字到 FrameBuffer，
 * 然后生成带 UV 的 BakedQuad 注入 RenderContext。
 * </p>
 */
public class TextDecorator implements BakedModelDecorator<Void>, BlockEntityDecorator<Void> {

    /**
     * 缩放因子：0.02 ≈ 1px/16单位。
     */
    private static final float SCALE_FACTOR = 0.02f;

    /**
     * Minecraft 字体渲染器。
     */
    private final TextRenderer textRenderer;

    /**
     * 构造函数。
     *
     * @param textRenderer Minecraft 字体渲染器
     */
    public TextDecorator(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    @Override
    public UnifiedRenderDecorator.Priority getPriority() {
        return UnifiedRenderDecorator.Priority.TEXT;
    }

    @Override
    public int getPriorityValue() {
        return getPriority().value;
    }

    @Override
    public boolean accepts(BlockState state) {
        return state != null && state.getBlock() instanceof SignTextProvider;
    }

    /**
     * 延迟初始化 textRenderer，避免 MinecraftClient 未就绪时返回 null。
     *
     * @return TextRenderer 实例
     */
    private TextRenderer getTextRenderer() {
        if (textRenderer == null) {
            return net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        }
        return textRenderer;
    }

    @Override
    public Void decorate(BakedModelContext context, Function<BakedModelContext, Void> next) {
        // 当前版本：BakedModel 阶段不渲染文字（数据在 BlockEntity 中）
        // 未来升级：在此处使用 FrameBuffer 渲染文字到贴图，注入 RenderContext
        return next.apply(context);
    }

    @Override
    public Void decorate(BlockEntityRendererContext<?> context, Function<BlockEntityRendererContext<?>, Void> next) {
        var result = next.apply(context);

        if (context.entity() instanceof TextBlockEntity textEntity) {
            List<TextLine> lines = textEntity.getLines();
            if (!lines.isEmpty()) {
                renderTextLines(lines, context);
            }
        }

        return result;
    }

    /**
     * 渲染文字行列表。
     *
     * @param lines   文字行列表
     * @param context 渲染上下文
     */
    public void renderTextLines(List<TextLine> lines, BlockEntityRendererContext<?> context) {
        context.matrices().push();

        for (TextLine line : lines) {
            renderSingleLine(line, context);
        }

        context.matrices().pop();
    }

    /**
     * 渲染单行文字。
     * <p>
     * 每行独立 push/pop，保证 transform 不污染外层。
     * 位置用 line.position（局部坐标 0~1），
     * 缩放 0.02 ≈ 1px/16单位，Y 轴取反（屏幕坐标向下 vs 世界坐标向上）。
     * </p>
     * <p>
     * 文字默认朝向 -Z 方向（面向方块内部），需要绕 Y 轴旋转 180° 使其面向外部玩家。
     * 文字发光（glowing=true）时强制满亮度 0xFFFF，绕过方块实际接收到的光照，
     * 实现自发光效果。非发光文字使用方块实际收到的亮度，与方块本体一致。
     * </p>
     *
     * @param line    文字行
     * @param context 渲染上下文
     */
    private void renderSingleLine(TextLine line, BlockEntityRendererContext<?> context) {
        TextRenderer renderer = getTextRenderer();
        Vec3d pos = line.position() != null ? line.position() : TextLine.DEFAULT_POSITION;

        context.matrices().push();
        context.matrices().translate(pos.x, pos.y, pos.z);

        // 绕 Y 轴旋转 180°，使文字面向外部玩家（默认朝向 -Z，旋转后朝向 +Z）
        context.matrices().multiply(new org.joml.Quaternionf().rotateY((float) Math.PI));

        float scale = SCALE_FACTOR * line.scale();
        context.matrices().scale(scale, -scale, scale);

        float width = renderer.getWidth(line.text());
        context.matrices().translate(-width / 2.0f, 0, 0);

        int color = line.color();
        boolean glowing = line.glowing();
        int effectiveLight = glowing ? 0xFFFF : context.light();

        if (glowing) {
            OrderedText ordered = renderer.wrapLines(line.text(), Integer.MAX_VALUE)
                    .stream().findFirst().orElse(OrderedText.EMPTY);
            renderer.drawWithOutline(
                    ordered, 0, 0, color,
                    0xFF000000 | (color & 0xFFFFFF),
                    context.matrices().peek().getPositionMatrix(), context.vertexConsumers(),
                    effectiveLight
            );
        } else {
            renderer.draw(
                    line.text(), 0, 0, color, true,
                    context.matrices().peek().getPositionMatrix(), context.vertexConsumers(),
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0, effectiveLight
            );
        }

        context.matrices().pop();
    }
}