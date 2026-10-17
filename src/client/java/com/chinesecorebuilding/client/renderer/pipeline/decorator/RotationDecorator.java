package com.chinesecorebuilding.client.renderer.pipeline.decorator;

import com.chinesecorebuilding.block.properties.model.Rotatable;
import com.chinesecorebuilding.client.renderer.pipeline.api.BakedModelDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.api.BlockEntityDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.api.UnifiedRenderDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.context.BakedModelContext;
import com.chinesecorebuilding.client.renderer.pipeline.context.BlockEntityRendererContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

/**
 * 旋转装饰器实现。
 * <p>
 * 统一处理 BakedModel 和 BlockEntityRenderer 的旋转变换。
 * 旋转角度由方块实现的 {@link Rotatable} 接口通过 {@link Rotatable#getRotationAngle} 计算，
 * 本类不硬编码任何旋转映射，遵循开闭原则。
 * </p>
 * <p>
 * 旋转公式（绕 Y 轴，顺时针为正方向）：
 * <pre>
 *   x' =  cos(θ) × (x - 0.5) + sin(θ) × (z - 0.5) + 0.5
 *   z' = -sin(θ) × (x - 0.5) + cos(θ) × (z - 0.5) + 0.5
 * </pre>
 * </p>
 */
public class RotationDecorator implements BakedModelDecorator<Void>, BlockEntityDecorator<Void> {

    /**
     * 方块中心坐标（旋转中心）。
     * <p>
     * 旋转围绕方块几何中心 (0.5, y, 0.5) 的 Y 轴进行。
     * </p>
     */
    private static final float CENTER = 0.5f;

    @Override
    public UnifiedRenderDecorator.Priority getPriority() {
        return UnifiedRenderDecorator.Priority.ROTATION;
    }

    @Override
    public int getPriorityValue() {
        return getPriority().value;
    }

    @Override
    public boolean accepts(BlockState state) {
        return state != null && state.getBlock() instanceof Rotatable;
    }

    @Override
    public Void decorate(BakedModelContext context, Function<BakedModelContext, Void> next) {
        float angle = getRotationAngle(context);
        if (angle == 0) {
            return next.apply(context);
        }

        final float cos = MathHelper.cos(angle);
        final float sin = MathHelper.sin(angle);

        context.context().pushTransform(quad -> {
            for (int i = 0; i < 4; i++) {
                float x = quad.x(i) - CENTER;
                float z = quad.z(i) - CENTER;

                quad.pos(i,
                        cos * x + sin * z + CENTER,
                        quad.y(i),
                        -sin * x + cos * z + CENTER
                );
            }
            return true;
        });

        try {
            return next.apply(context);
        } finally {
            context.context().popTransform();
        }
    }

    @Override
    public Void decorate(BlockEntityRendererContext<?> context, Function<BlockEntityRendererContext<?>, Void> next) {
        BlockState state = context.entity().getCachedState();
        if (!(state.getBlock() instanceof Rotatable rotatable)) {
            return next.apply(context);
        }

        float angle = rotatable.getRotationAngle(state);
        if (angle == 0) {
            return next.apply(context);
        }

        context.matrices().push();
        context.matrices().translate(CENTER, 0, CENTER);
        context.matrices().multiply(new org.joml.Quaternionf().rotateY(angle));
        context.matrices().translate(-CENTER, 0, -CENTER);

        try {
            return next.apply(context);
        } finally {
            context.matrices().pop();
        }
    }

    /**
     * 从 BakedModelContext 中获取旋转角度。
     *
     * @param context BakedModel 渲染上下文
     * @return 旋转角度（弧度），非 Rotatable 方块返回 0
     */
    private float getRotationAngle(BakedModelContext context) {
        var state = context.blockView().getBlockState(context.pos());
        if (state.getBlock() instanceof Rotatable rotatable) {
            return rotatable.getRotationAngle(state);
        }
        return 0;
    }
}