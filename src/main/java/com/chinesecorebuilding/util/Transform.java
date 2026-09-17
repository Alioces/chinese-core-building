package com.chinesecorebuilding.util;

import net.minecraft.util.math.Vec3d;

/**
 * 变换矩阵数据类。
 * <p>
 * 封装模型变换的三个基本属性：位置、旋转和缩放。
 * 用于在模型烘焙阶段或渲染阶段应用变换到子模型或基础模型。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * Transform transform = new Transform(
 *     new Vec3d(0.5, 0.3, 0.5),  // 位置（方块坐标）
 *     new Vec3d(0, Math.PI / 2, 0),  // 旋转（弧度）
 *     0.5f  // 缩放因子
 * );
 * </pre>
 *
 * @param position 位置偏移（方块坐标，0.0~1.0）
 * @param rotation 旋转角度（弧度，XYZ 轴）
 * @param scale    缩放因子（1.0 为原始大小）
 */
public record Transform(
    Vec3d position,
    Vec3d rotation,
    float scale
) {

    /**
     * 创建变换（默认缩放 1.0）。
     *
     * @param position 位置偏移
     * @param rotation 旋转角度
     */
    public Transform(Vec3d position, Vec3d rotation) {
        this(position, rotation, 1.0f);
    }

    /**
     * 创建纯平移变换。
     *
     * @param position 位置偏移
     */
    public Transform(Vec3d position) {
        this(position, Vec3d.ZERO, 1.0f);
    }

    /**
     * 创建恒等变换（无变换）。
     */
    public Transform() {
        this(Vec3d.ZERO, Vec3d.ZERO, 1.0f);
    }
}