package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.block.entity.TextBlockEntity;
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
 * 文字层渲染器（DECORATOR 层）。
 * <p>
 * <b>设计原则</b>：本渲染器作为文字层的 DECORATOR（装饰器），职责是"在局部空间产生文字顶点"，
 * <b>不应该</b>关心旋转/偏移等 TRANSFORMER（变换层）的逻辑。
 * </p>
 * <p>
 * 根据 Block Properties API 的插件链架构：
 * <pre>
 * OffsetBakedModel         ← 最外层，最后变换
 *   → RotationBakedModel   ← 第二层，应用旋转
 *     → TextBakedModel     ← 最内层，只产生文字顶点（本渲染器的角色）
 *       → 原始模型
 * </pre>
 * 文字层（TextBakedModel / TextBlockRenderer）只负责：
 * <ul>
 *   <li>读取 TextLine 的 position（世界坐标，相对于方块原点）</li>
 *   <li>应用缩放（scale）</li>
 *   <li>水平居中文字</li>
 *   <li>调用 TextRenderer 绘制</li>
 * </ul>
 * 旋转和偏移由外层的 BakedModel 插件链统一处理。
 * </p>
 * <p>
 * 本渲染器是公共文字功能基础设施的一部分，任何实现 {@link SignTextProvider} 的方块
 * 都可以复用此渲染器进行文字层的渲染。
 * </p>
 *
 * @param <T> 渲染的 BlockEntity 类型（TextBlockEntity 或其子类）
 * @see com.chinesecorebuilding.block.properties.model.SignTextProvider
 */
public class TextBlockRenderer<T extends TextBlockEntity> implements BlockEntityRenderer<T> {

    /**
     * Minecraft 字体渲染器。
     * <p>
     * 由 {@link BlockEntityRendererFactory.Context} 提供，
     * 用于将 {@link net.minecraft.text.Text} 渲染为顶点数据。
     * </p>
     */
    private final TextRenderer textRenderer;

    /**
     * 构造函数。
     *
     * @param ctx BlockEntityRenderer 工厂上下文，从中获取 {@link TextRenderer}
     */
    public TextBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
    }

    /**
     * 渲染 TextBlockEntity 的文字层。
     * <p>
     * 对 Entity 存储的每一行 {@link TextLine} 依次渲染。
     * </p>
     * <p>
     * <b>设计原则</b>：文字层作为 DECORATOR（装饰层），职责是"在局部空间产生文字顶点"，
     * <b>不应该</b>关心旋转/偏移等 TRANSFORMER（变换层）的逻辑。
     * 旋转和偏移应由外层（BakedModel 插件链或 Minecraft 的模型矩阵）统一处理。
     * </p>
     * <p>
     * 本渲染器只负责：
     * <ul>
     *   <li>读取 TextLine 的 position（局部坐标 0~1）</li>
     *   <li>应用缩放（scale）</li>
     *   <li>水平居中文字</li>
     *   <li>调用 TextRenderer 绘制</li>
     * </ul>
     * </p>
     * <p>
     * 无文字行时直接短路返回，避免无谓的矩阵操作。
     * </p>
     *
     * @param entity     目标 BlockEntity（TextBlockEntity 或其子类）
     * @param tickDelta  帧间插值（0~1），用于平滑动画
     * @param matrices   矩阵栈，承载世界变换（已包含方块旋转/偏移）
     * @param vertexConsumers 顶点消费者提供者，用于获取光照/雾化等渲染上下文
     * @param light      方块光照值
     * @param overlay    方块叠加色（红/伤等效果）
     */
    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        List<TextLine> lines = entity.getLines();
        if (lines.isEmpty()) return;

        matrices.push();

        for (TextLine line : lines) {
            renderLine(line, matrices, vertexConsumers, light);
        }

        matrices.pop();
    }

    /**
     * 渲染单个 TextLine。
     * <p>
     * 每行独立 push/pop，保证 transform 不污染外层。
     * 位置用 line.position（世界坐标，相对于方块原点），
     * 缩放 0.02 ≈ 1px/16单位，Y 轴取反（屏幕坐标向下 vs 世界坐标向上）。
     * </p>
     * <p>
     * 文字发光（glowing=true）时强制满亮度 0xFFFF，绕过方块实际接收到的光照，
     * 实现自发光效果。非发光文字使用方块实际收到的亮度，与方块本体一致。
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