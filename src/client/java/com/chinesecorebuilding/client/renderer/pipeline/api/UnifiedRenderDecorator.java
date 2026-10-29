package com.chinesecorebuilding.client.renderer.pipeline.api;

import net.minecraft.block.BlockState;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 统一渲染管线装饰器接口。
 * <p>
 * 定义一个通用的装饰器契约，使 BakedModel 管线和 BlockEntityRenderer 管线
 * 能够使用相同的装饰器链进行渲染处理。
 * </p>
 * <p>
 * <b>设计目标：</b>
 * <ul>
 *   <li>统一装饰器接口，消除两条管线的重复逻辑</li>
 *   <li>保持现有装饰顺序：SubModel → Text → Rotation → Offset</li>
 *   <li>支持 BakedModel（静态）和 BlockEntityRenderer（动态）两种渲染上下文</li>
 * </ul>
 * </p>
 * <p>
 * <b>装饰器链结构：</b>
 * <pre>
 * OffsetDecorator
 *   └─ RotationDecorator
 *      └─ TextDecorator
 *         └─ SubModelDecorator
 *            └─ 原始模型/渲染器
 * </pre>
 * </p>
 *
 * @param <T> 渲染上下文类型（BakedModelContext 或 BlockEntityRendererContext）
 * @param <R> 渲染结果类型
 */
public interface UnifiedRenderDecorator<T, R> {

    /**
     * 装饰器优先级枚举。
     * <p>
     * 数值越小越先执行（越靠近内层）。
     * </p>
     */
    enum Priority {
        /** 子模型装饰器（最内层）。 */
        SUB_MODEL(-1000),
        /** 文字装饰器。 */
        TEXT(-500),
        /** 旋转变换装饰器。 */
        ROTATION(0),
        /** 偏移变换装饰器（最外层）。 */
        OFFSET(100);

        /** 优先级数值。 */
        public final int value;

        /**
         * 构造函数。
         *
         * @param value 优先级数值
         */
        Priority(int value) {
            this.value = value;
        }
    }

    /**
     * 获取装饰器优先级。
     *
     * @return 优先级枚举值
     */
    Priority getPriority();

    /**
     * 判断此装饰器是否适用于当前方块。
     *
     * @param state 方块状态
     * @return true 如果适用
     */
    boolean accepts(BlockState state);

    /**
     * 装饰渲染过程。
     * <p>
     * 实现类应在此方法中：
     * <ol>
     *   <li>执行前置变换（如 pushMatrix）</li>
     *   <li>调用 next.apply(context) 继续链式渲染</li>
     *   <li>执行后置变换（如 popMatrix）</li>
     *   <li>追加自己的渲染内容（如文字顶点）</li>
     * </ol>
     * </p>
     *
     * @param context 渲染上下文
     * @param next    下一个装饰器的渲染函数
     * @return 渲染结果
     */
    R decorate(T context, Function<T, R> next);

    /**
     * 创建装饰器链。
     * <p>
     * 按优先级排序后，从内到外依次包装。
     * </p>
     *
     * @param decorators 装饰器列表
     * @param baseRender 基础渲染函数
     * @param <T>        渲染上下文类型
     * @param <R>        渲染结果类型
     * @return 包装后的渲染函数
     */
    static <T, R> Function<T, R> createChain(
            List<UnifiedRenderDecorator<T, R>> decorators,
            Function<T, R> baseRender) {

        decorators.sort(Comparator.comparingInt(UnifiedRenderDecorator::getPriorityValue));

        Function<T, R> current = baseRender;
        for (UnifiedRenderDecorator<T, R> decorator : decorators) {
            final Function<T, R> next = current;
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