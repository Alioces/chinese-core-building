package com.chinesecorebuilding.client.renderer.pipeline;

import com.chinesecorebuilding.block.properties.model.Offset;
import net.minecraft.block.BlockState;

import java.util.function.Function;

/**
 * 偏移装饰器实现。
 * <p>
 * 统一处理 BakedModel 和 BlockEntityRenderer 的偏移变换。
 * 偏移量由方块实现的 {@link Offset} 接口定义，本类不硬编码任何偏移映射，遵循开闭原则。
 * </p>
 */
public class OffsetDecorator implements BakedModelDecorator<Void>, BlockEntityDecorator<Void> {

    @Override
    public UnifiedRenderDecorator.Priority getPriority() {
        return UnifiedRenderDecorator.Priority.OFFSET;
    }

    @Override
    public int getPriorityValue() {
        return getPriority().value;
    }

    @Override
    public boolean accepts(BlockState state) {
        return state != null && state.getBlock() instanceof Offset;
    }

    @Override
    public Void decorate(BakedModelContext context, Function<BakedModelContext, Void> next) {
        float[] offset = getOffset(context);
        if (offset[0] == 0 && offset[1] == 0 && offset[2] == 0) {
            return next.apply(context);
        }

        final float dx = offset[0], dy = offset[1], dz = offset[2];

        context.context().pushTransform(quad -> {
            for (int i = 0; i < 4; i++) {
                quad.pos(i, quad.x(i) + dx, quad.y(i) + dy, quad.z(i) + dz);
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
        if (!(state.getBlock() instanceof Offset offsetable)) {
            return next.apply(context);
        }

        float[] offset = offsetable.getOffset(state);
        if (offset[0] == 0 && offset[1] == 0 && offset[2] == 0) {
            return next.apply(context);
        }

        context.matrices().push();
        context.matrices().translate(offset[0], offset[1], offset[2]);

        try {
            return next.apply(context);
        } finally {
            context.matrices().pop();
        }
    }

    /**
     * 从 BakedModelContext 中获取偏移量。
     *
     * @param context BakedModel 渲染上下文
     * @return 偏移量数组 [dx, dy, dz]，非 Offset 方块返回 [0, 0, 0]
     */
    private float[] getOffset(BakedModelContext context) {
        var state = context.blockView().getBlockState(context.pos());
        if (state.getBlock() instanceof Offset offsetable) {
            return offsetable.getOffset(state);
        }
        return new float[]{0, 0, 0};
    }
}