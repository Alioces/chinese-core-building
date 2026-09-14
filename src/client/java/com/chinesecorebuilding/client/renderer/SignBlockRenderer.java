package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.block.properties.Directional;
import com.chinesecorebuilding.block.properties.Offset;
import com.chinesecorebuilding.block.properties.Rotatable;
import com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel;
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
 * 路牌文字渲染器。
 * <p>
 * 独立于 BakedModel 管线，负责在方块本体上叠加绘制文字层。
 * 支持任意数量的 {@link TextLine}，每行有独立位置、颜色、缩放。
 * </p>
 * <p>
 * 坐标变换链路：方块空间原点 → 应用 Y 轴旋转（与 RotationBakedModel 一致）
 * → 应用偏移（与 OffsetBakedModel 一致）→ 对每个 TextLine：translate 到 line.position → 缩放 → 绘制。
 * </p>
 * <p>
 * 与 BakedModel 管线的一致性保证：文字层必须与方块本体严格对齐，
 * 因此渲染器必须用相同的变换逻辑。变换参数从 Block 的能力接口
 * （Directional / Rotatable / Offset）读取，与 BakedModel 插件链完全解耦。
 * </p>
 * <p>
 * TextRenderer.draw 签名（yarn 1.20.1）：
 * <pre>
 * draw(Text/String/OrderedText, x, y, color, shadow, Matrix4f, VertexConsumerProvider, TextLayerType, color2, light)
 *   shadow=true  → 绘制阴影（替代不存在的 drawWithShadow）
 *   glowing      → 使用 SEE_THROUGH 层 + 强制满亮度
 * </pre>
 * </p>
 *
 * @param <T> 渲染的 BlockEntity 类型（必须是 SignBlockEntity 或其子类）
 * @see RotationBakedModel
 * @see OffsetBakedModel
 */
public class SignBlockRenderer<T extends SignBlockEntity> implements BlockEntityRenderer<T> {

    /** Minecraft 内置文字渲染器，由 BlockEntityRendererFactory.Context 提供 */
    private final TextRenderer textRenderer;

    /**
     * @param ctx 方块实体渲染器工厂上下文，用于获取 TextRenderer
     */
    public SignBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
    }

    /**
     * 方块实体渲染入口，由 Minecraft 每帧对可见方块调用。
     * <p>
     * 变换链路：
     * <ol>
     *   <li>从 BlockState 读取旋转角度 → 绕 Y 轴旋转（旋转中心 0.5,0,0.5，即方块底部中心）</li>
     *   <li>从 BlockState 读取 3D 偏移 → 平移矩阵</li>
     *   <li>对每条 TextLine：push → translate 到 line.position → 缩放 → 绘制 → pop</li>
     * </ol>
     * 任何变换前先 push，全部绘制完再 pop，保证不污染外层矩阵栈。
     * </p>
     */
    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        BlockState state = entity.getCachedState();
        List<TextLine> lines = entity.getLines();
        if (lines.isEmpty()) return;

        matrices.push();

        applyRotation(state, matrices);
        applyOffset(state, matrices);

        for (TextLine line : lines) {
            renderLine(line, matrices, vertexConsumers, light);
        }

        matrices.pop();
    }

    /**
     * 应用 Y 轴旋转，与 RotationBakedModel 保持完全一致。
     * <p>
     * 旋转中心是方块底部锚点 (0.5, 0, 0.5)，操作顺序：
     * <pre>translate(anchor) → rotateY(-angle) → translate(-anchor)</pre>
     * </p>
     * <p>
     * 顺时针为正角度时用 {@code -angle} 取反，因为 Minecraft 坐标是右手系，
     * 而 {@link Quaternionf#rotateY(float)} 以逆时针为正。
     * </p>
     * <p>
     * 优先级：Directional（4 方向）＞ Rotatable（任意角度）。
     * </p>
     */
    private void applyRotation(BlockState state, MatrixStack matrices) {
        Block block = state.getBlock();
        float angle = 0;
        if (block instanceof Directional d) {
            angle = d.getRotationAngle(state);
        } else if (block instanceof Rotatable r) {
            angle = r.getRotationAngle(state);
        }
        if (angle != 0) {
            matrices.translate(0.5, 0, 0.5);
            matrices.multiply(new Quaternionf().rotateY(-angle));
            matrices.translate(-0.5, 0, -0.5);
        }
    }

    /**
     * 应用 3D 空间偏移，与 OffsetBakedModel 保持完全一致。
     * <p>
     * 偏移值由 {@link Offset#getOffset(BlockState)} 返回，顺序 [x, y, z]，
     * 单位为方块格（1.0 = 1 格）。
     * </p>
     */
    private void applyOffset(BlockState state, MatrixStack matrices) {
        Block block = state.getBlock();
        if (block instanceof Offset offset) {
            float[] off = offset.getOffset(state);
            matrices.translate(off[0], off[1], off[2]);
        }
    }

    /**
     * 渲染单个 TextLine。
     * <p>
     * 缩放系数 0.02 ≈ 1px/16单位（与 vanilla SignBlockEntityRenderer 一致）。
     * Y 轴取反：屏幕坐标向下，世界坐标向上。
     * </p>
     * <p>
     * 绘制逻辑：先 translate 到 line.position，缩放到像素空间，
     * 再向左偏移半个文字宽度实现水平居中，最后调用 TextRenderer.draw 或 drawWithOutline。
     * 发光文字强制满亮度（0xFFFF），正常文字使用方块收到的实际亮度。
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