package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 文字层渲染器。
 * <p>
 * 独立于 BakedModel 管线，负责在方块本体上叠加绘制文字层。
 * 支持任意数量的 {@link TextLine}，每行有独立位置、颜色、缩放。
 * </p>
 * <p>
 * <b>没有自动旋转、没有自动偏移。</b>
 * 文字位置和朝向完全由 TextLine.position 决定。
 * BlockEntityRenderer 与 BakedModel 是两条独立渲染管线，
 * RotationBakedModel / OffsetBakedModel 只影响方块静态模型，
 * 不会传递到 BlockEntity 渲染层，因此这里不做任何与方块朝向相关的矩阵变换。
 * </p>
 *
 * @param <T> 渲染的 BlockEntity 类型（必须是 SignBlockEntity 或其子类）
 */
public class SignBlockRenderer<T extends SignBlockEntity> implements BlockEntityRenderer<T> {

    private final TextRenderer textRenderer;

    public SignBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        List<TextLine> lines = entity.getLines();
        if (lines.isEmpty()) return;

        for (TextLine line : lines) {
            renderLine(line, matrices, vertexConsumers, light);
        }
    }

    /**
     * 渲染单个 TextLine。
     * <p>
     * 位置：直接 translate 到 line.position（方块相对坐标）。
     * 缩放系数 0.02 ≈ 1px/16单位。
     * Y 轴取反：屏幕坐标向下，世界坐标向上。
     * </p>
     */
    private void renderLine(TextLine line, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers, int light) {
        Vec3d pos = line.position() != null ? line.position() : TextLine.DEFAULT_POSITION;

        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);

        float scale = 0.02f * line.scale();
        matrices.scale(scale, -scale, scale);

        float width = textRenderer.getWidth(line.text());
        matrices.translate(-width / 2.0f, 0, 0);

        int color = line.color();
        boolean glowing = line.glowing();
        int effectiveLight = glowing ? 0xFFFF : light;

        if (glowing) {
            OrderedText ordered = textRenderer.wrapLines(line.text(), Integer.MAX_VALUE)
                    .stream().findFirst().orElse(OrderedText.EMPTY);
            textRenderer.drawWithOutline(
                    ordered, 0, 0, color,
                    0xFF000000 | (color & 0xFFFFFF),
                    matrices.peek().getPositionMatrix(), vertexConsumers,
                    effectiveLight
            );
        } else {
            textRenderer.draw(
                    line.text(), 0, 0, color, true,
                    matrices.peek().getPositionMatrix(), vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0, effectiveLight
            );
        }

        matrices.pop();
    }
}