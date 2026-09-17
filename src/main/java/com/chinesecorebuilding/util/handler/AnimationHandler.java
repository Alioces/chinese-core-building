package com.chinesecorebuilding.util.handler;

import com.chinesecorebuilding.util.Transform;
import net.minecraft.block.BlockState;

import java.util.List;

/**
 * 动画更新处理器。
 * <p>
 * 函数式接口，用于向烘焙插件链提交自定义动画逻辑。
 * 实现此接口的 Lambda 或方法引用会在渲染阶段每帧调用，
 * 用于计算动态变换矩阵（如旋转、平移、缩放）。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * AnimationHandler handler = (time, delta, state) -> {
 *     float angle = (time % 10000) / 10000.0f * (float) (2 * Math.PI);
 *     return List.of(new Transform(center, new Vec3d(0, angle, 0), 1.0f));
 * };
 * </pre>
 *
 * @see Transform
 * @see AnimatableModel
 */
@FunctionalInterface
public interface AnimationHandler {

    /**
     * 计算当前帧的变换矩阵列表。
     * <p>
     * 此方法在渲染阶段每帧调用，用于更新动态模型的变换状态。
     * 返回的变换矩阵会应用到对应的子模型或基础模型上。
     * </p>
     *
     * @param time  当前时间戳（毫秒），可用于计算周期性动画
     * @param delta 上一帧到当前帧的时间间隔（毫秒），用于帧率无关动画
     * @param state 当前方块状态，可用于根据 BlockState 调整动画行为
     * @return 变换矩阵列表，每个变换包含位置、旋转和缩放信息
     */
    List<Transform> handle(long time, float delta, BlockState state);
}