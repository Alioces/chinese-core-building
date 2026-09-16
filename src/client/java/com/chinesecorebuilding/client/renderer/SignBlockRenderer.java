package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.block.properties.model.Offset;
import com.chinesecorebuilding.block.properties.model.Rotatable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.List;

/**
 * 文字层渲染器——与 BakedModel 管线变换完全同步。
 * <p>
 * BlockEntityRenderer 和 BakedModel 是两条独立的渲染管线，变换参数不会自动传递。
 * 为了让文字跟随方块的旋转/偏移，本渲染器<b>必须手动读取</b>
 * {@link Rotatable} / {@link Offset} 接口，应用与
 * {@link com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel}
 * 和 {@link com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel}
 * <b>完全一致的变换公式和旋转中心</b>。
 * </p>
 *
 * <h3>变换矩阵链（顺序从外到内）</h3>
 * <pre>
 * matrices.translate(dx, dy, dz)           ← OffsetBakedModel 的偏移（阶段2最终变换）
 * matrices.translate(0.5, 0.5, 0.5)         ← RotationBakedModel 旋转中心
 * matrices.multiply(new Quaternionf().rotateY(-angle))  ← 绕 Y 轴旋转
 * matrices.translate(-0.5, -0.5, -0.5)     ← 回到原点
 *    └── 对每行 TextLine:
 *        matrices.push()
 *        matrices.translate(line.position)
 *        matrices.scale(0.02*scale, -0.02*scale, 0.02*scale)
 *        水平居中 → 绘制文字 → pop
 * </pre>
 * <p>
 * <b>为什么 rotateY 用 {@code -angle}？</b>
 * RotationBakedModel 里用的公式是"顺时针为正"（符合直觉）：
 * <pre>
 * x' =  cos(θ)(x - 0.5) + sin(θ)(z - 0.5) + 0.5
 * z' = -sin(θ)(x - 0.5) + cos(θ)(z - 0.5) + 0.5
 * </pre>
 * 但 MatrixStack.multiply(Quaternionf.rotateY) 是右手系逆时针为正，
 * 所以传入 {@code -angle} 让它等效于顺时针旋转。
 * </p>
 *
 * @param <T> 渲染的 BlockEntity 类型（SignBlockEntity 或其子类）
 * @see Rotatable
 * @see Offset
 * @see com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel
 * @see com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel
 */
public class SignBlockRenderer<T extends SignBlockEntity> implements BlockEntityRenderer<T> {

    /**
     * 旋转中心常量。
     * <p>
     * 与 {@link com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel#CENTER} 保持一致，
     * 确保 BlockEntityRenderer 和 BakedModel 管线的旋转中心完全相同。
     * </p>
     */
    private static final float CENTER = 0.5f;

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
    public SignBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
    }

    /**
     * 渲染 SignBlockEntity 的文字层。
     * <p>
     * 对 Entity 存储的每一行 {@link TextLine} 依次渲染，
     * 变换链（Offset → Rotation → 每行独立 translate/scale）与
     * {@link com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel} /
     * {@link com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel}
     * 完全同步，保证文字跟随方块的旋转和偏移。
     * </p>
     * <p>
     * 无文字行时直接短路返回，避免无谓的矩阵操作。
     * </p>
     *
     * @param entity     目标 BlockEntity（SignBlockEntity 或其子类）
     * @param tickDelta  帧间插值（0~1），用于平滑动画
     * @param matrices   矩阵栈，承载世界变换和自定义变换
     * @param vertexConsumers 顶点消费者提供者，用于获取光照/雾化等渲染上下文
     * @param light      方块光照值
     * @param overlay    方块叠加色（红/伤等效果）
     */
    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        List<TextLine> lines = entity.getLines();
        if (lines.isEmpty()) return;   // 没有文字行就跳过，避免无谓的矩阵操作

        BlockState state = entity.getCachedState();
        Block block = state.getBlock();

        // 整体矩阵栈 push → 先应用最外层变换（Offset），再应用内层变换（Rotation）
        // 变换顺序决定了：矩阵从外到内依次生效，与 RenderContext.pushTransform 的作用方向一致
        matrices.push();

        // 偏移是最外层变换：在旋转之前先把整个文字层平移
        applyOffset(block, state, matrices);
        // 旋转是内层变换：绕方块中心旋转，文字跟着一起转
        applyRotation(block, state, matrices);

        // 每行独立 push/pop —— TextLine.position 在变换后的方块空间里生效
        for (TextLine line : lines) {
            renderLine(line, matrices, vertexConsumers, light);
        }

        matrices.pop();
    }

    /**
     * 应用偏移变换——与 OffsetBakedModel 公式完全一致。
     * <p>
     * 如果方块实现了 {@link Offset}，读取偏移值并 translate 矩阵栈。
     * 偏移是最外层变换，先于旋转应用。
     * </p>
     * <p>
     * 矩阵栈变换的应用顺序：外层先 translate → 内层再 rotate → 内层 translate 影响的坐标
     * 最终会被外层 translate 再平移一次。这正是"旋转中心 (0.5,0.5,0.5) 绕方块中心，
     * 然后整块平移到 Offset"的预期效果。
     * </p>
     */
    private void applyOffset(Block block, BlockState state, MatrixStack matrices) {
        if (block instanceof Offset offset) {
            float[] off = offset.getOffset(state);
            // 短路判断：{0,0,0} 偏移直接跳过 translate，减少一次无效矩阵操作
            if (off[0] != 0 || off[1] != 0 || off[2] != 0) {
                matrices.translate(off[0], off[1], off[2]);
            }
        }
    }

    /**
     * 应用旋转变换——与 RotationBakedModel 公式完全一致。
     * <p>
     * 旋转中心 (0.5, 0.5, 0.5)，角度来自 {@link Rotatable#getRotationAngle}
     * （Directional 作为子接口自动覆盖）。
     * 使用 {@code -angle} 取反是因为 MatrixStack 的 Y 轴旋转是右手系逆时针，
     * 而 RotationBakedModel 的公式定义顺时针为正。
     * </p>
     * <p>
     * 操作序列：translate 到旋转中心 → rotateY(angle) → translate 回来。
     * 这等价于 RotationBakedModel 里 pushTransform 的矩阵计算，
     * 只是 MatrixStack.multiply 是右手系，公式是左手系，所以 angle 取反。
     * </p>
     */
    private void applyRotation(Block block, BlockState state, MatrixStack matrices) {
        float angle = 0;
        // Rotatable 接口提供 getRotationAngle，Directional 子接口覆盖它从 FACING 算角度
        if (block instanceof Rotatable rotatable) {
            angle = rotatable.getRotationAngle(state);
        }
        if (angle == 0) return;   // 无角度则跳过，避免无效矩阵操作

        // 三步完成绕中心点旋转：原点 → 中心 → 旋转 → 原点
        matrices.translate(CENTER, CENTER, CENTER);
        matrices.multiply(new Quaternionf().rotateY(-angle));   // -angle：补偿右手系 vs 顺时针
        matrices.translate(-CENTER, -CENTER, -CENTER);
    }

    /**
     * 渲染单个 TextLine。
     * <p>
     * 每行独立 push/pop，保证 transform 不污染外层。
     * 位置用 line.position（方块相对坐标 0~1），
     * 缩放 0.02 ≈ 1px/16单位，Y 轴取反（屏幕坐标向下 vs 世界坐标向上）。
     * </p>
     * <p>
     * 文字发光（glowing=true）时强制满亮度 0xFFFF，绕过方块实际接收到的光照，
     * 实现自发光效果。非发光文字使用方块实际收到的亮度，与方块本体一致。
     * </p>
     */
    private void renderLine(TextLine line, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers, int light) {
        // null safety：position 为 null 时用默认位置（方块中心偏上）
        Vec3d pos = line.position() != null ? line.position() : TextLine.DEFAULT_POSITION;

        // 每行独立 push —— 保证这一行的 transform 不会影响下一行
        matrices.push();
        // 把矩阵原点移到 TextLine.position（方块相对坐标 0~1）
        matrices.translate(pos.x, pos.y, pos.z);

        // 缩放：0.02 ≈ 1px / 16单位（Minecraft 方块单位 = 16px）
        // Y 轴取反：屏幕坐标原点在左上角向下为正，世界坐标原点在底部向上为正
        float scale = 0.02f * line.scale();
        matrices.scale(scale, -scale, scale);

        // 水平居中：translate(-width/2) 让文字中心对齐到 position.x
        float width = textRenderer.getWidth(line.text());
        matrices.translate(-width / 2.0f, 0, 0);

        int color = line.color();
        boolean glowing = line.glowing();
        // 发光强制满亮度，非发光使用方块实际光照
        int effectiveLight = glowing ? 0xFFFF : light;

        if (glowing) {
            // 发光文字用 drawWithOutline：主体颜色 + 黑色描边（最后一个参数 0xFF000000|color 是描边色）
            // wrapLines 处理超长文字，findFirst 取第一行
            OrderedText ordered = textRenderer.wrapLines(line.text(), Integer.MAX_VALUE)
                    .stream().findFirst().orElse(OrderedText.EMPTY);
            textRenderer.drawWithOutline(
                    ordered, 0, 0, color,
                    0xFF000000 | (color & 0xFFFFFF),
                    matrices.peek().getPositionMatrix(), vertexConsumers,
                    effectiveLight
            );
        } else {
            // 普通文字：shadow=true 开启文字阴影；SEE_THROUGH 让文字在方块背后也能看到（类似原版告示牌）
            textRenderer.draw(
                    line.text(), 0, 0, color, true,
                    matrices.peek().getPositionMatrix(), vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0, effectiveLight
            );
        }

        // 恢复矩阵栈 —— 这一行的 transform 到此结束
        matrices.pop();
    }
}