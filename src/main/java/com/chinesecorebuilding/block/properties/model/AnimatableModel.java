package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.util.handler.AnimationHandler;

/**
 * 可动画模型接口。
 * <p>
 * 实现此接口的方块可以提供自定义动画逻辑，
 * 在模型烘焙阶段注册动画处理器，在渲染阶段每帧调用。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * public class GearBlock extends CustomBlock implements AnimatableModel {
 *     private final AnimationHandler handler = (time, delta, state) -> {
 *         float angle = (time % 10000) / 10000.0f * (float) (2 * Math.PI);
 *         return List.of(new Transform(center, new Vec3d(0, angle, 0), 1.0f));
 *     };
 *
 *     &#64;Override
 *     public AnimationHandler getAnimationHandler() {
 *         return handler;
 *     }
 * }
 * </pre>
 *
 * @see AnimationHandler
 * @see ModelBakeDecorator
 */
public interface AnimatableModel extends ModelBakeDecorator {

    /**
     * 获取动画处理器。
     * <p>
     * 返回的动画处理器会在模型烘焙阶段注册到动画注册中心，
     * 并在渲染阶段每帧调用以计算变换矩阵。
     * </p>
     *
     * @return 动画处理器实例
     */
    AnimationHandler getAnimationHandler();
}