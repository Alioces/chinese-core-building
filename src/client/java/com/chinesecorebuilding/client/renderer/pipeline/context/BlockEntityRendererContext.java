package com.chinesecorebuilding.client.renderer.pipeline.context;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * BlockEntityRenderer 渲染上下文。
 * <p>
 * 封装 BlockEntity 渲染所需的全部参数。
 * </p>
 *
 * @param <T>              BlockEntity 类型
 * @param entity           BlockEntity 实例
 * @param tickDelta        帧间插值（0~1），用于平滑动画
 * @param matrices         矩阵栈，承载世界变换
 * @param vertexConsumers  顶点消费者提供者，用于获取光照/雾化等渲染上下文
 * @param light            方块光照值
 * @param overlay          方块叠加色（红/伤等效果）
 */
public record BlockEntityRendererContext<T extends BlockEntity>(
        T entity,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int overlay
) {
}