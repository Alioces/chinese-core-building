package com.chinesecorebuilding.client.renderer.pipeline;

import net.minecraft.block.BlockState;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * BakedModel 管线装饰器接口。
 * <p>
 * 专门用于 BakedModel 渲染管线的装饰器契约。
 * </p>
 *
 * @param <R> 渲染结果类型
 */
public interface BakedModelDecorator<R> {

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
    R decorate(BakedModelContext context, Function<BakedModelContext, R> next);

    /**
     * 创建装饰器链。
     *
     * @param decorators 装饰器列表
     * @param baseRender 基础渲染函数
     * @param <R>        渲染结果类型
     * @return 包装后的渲染函数
     */
    static <R> Function<BakedModelContext, R> createChain(
            List<BakedModelDecorator<R>> decorators,
            Function<BakedModelContext, R> baseRender) {

        decorators.sort(Comparator.comparingInt(BakedModelDecorator::getPriorityValue));

        Function<BakedModelContext, R> current = baseRender;
        for (int i = decorators.size() - 1; i >= 0; i--) {
            var decorator = decorators.get(i);
            final Function<BakedModelContext, R> next = current;
            current = ctx -> decorator.decorate(ctx, next);
        }

        return current;
    }

    /**
     * 获取优先级数值（用于排序）。
     *
     * @return 优先级数值
     */
    default int getPriorityValue() {
        return getPriority().value;
    }
}