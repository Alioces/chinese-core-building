package com.chinesecorebuilding.client.renderer.pipeline.api;

import com.chinesecorebuilding.client.renderer.pipeline.context.BlockEntityRendererContext;
import net.minecraft.block.BlockState;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * BlockEntityRenderer 管线装饰器接口。
 * <p>
 * 专门用于 BlockEntity 渲染管线的装饰器契约。
 * </p>
 *
 * @param <R> 渲染结果类型
 */
public interface BlockEntityDecorator<R> {

    /**
     * 获取装饰器优先级。
     *
     * @return 优先级枚举值
     */
    UnifiedRenderDecorator.Priority getPriority();

    /**
     * 判断此装饰器是否适用于当前方块。
     *
     * @param state 方块状态
     * @return true 如果适用
     */
    boolean accepts(BlockState state);

    /**
     * 装饰渲染过程。
     *
     * @param context 渲染上下文
     * @param next    下一个装饰器的渲染函数
     * @return 渲染结果
     */
    R decorate(BlockEntityRendererContext<?> context, Function<BlockEntityRendererContext<?>, R> next);

    /**
     * 创建装饰器链。
     *
     * @param decorators 装饰器列表
     * @param baseRender 基础渲染函数
     * @param <R>        渲染结果类型
     * @return 包装后的渲染函数
     */
    static <R> Function<BlockEntityRendererContext<?>, R> createChain(
            List<BlockEntityDecorator<R>> decorators,
            Function<BlockEntityRendererContext<?>, R> baseRender) {

        decorators.sort(Comparator.comparingInt(BlockEntityDecorator::getPriorityValue));

        Function<BlockEntityRendererContext<?>, R> current = baseRender;
        for (int i = decorators.size() - 1; i >= 0; i--) {
            var decorator = decorators.get(i);
            final Function<BlockEntityRendererContext<?>, R> next = current;
            current = ctx -> decorator.decorate(ctx, next);
        }

        return current;
    }

    /**
     * 获取优先级数值（用于排序）。
     * <p>
     * 由实现类通过 {@link UnifiedRenderDecorator.Priority#value} 返回数值。
     * </p>
     *
     * @return 优先级数值（数值越小越靠近内层）
     */
    int getPriorityValue();
}