package com.chinesecorebuilding.util;

import net.minecraft.util.math.Vec3d;

/**
 * 锚点定义。
 * <p>
 * 锚点用于标记子模型在父模型中的放置位置和变换属性。
 * 每个锚点包含名称、位置、旋转和缩放信息，
 * 在模型烘焙阶段用于确定子模型的放置位置和姿态。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * AnchorPoint anchor = new AnchorPoint(
 *     "bottle_slot_1",
 *     new Vec3d(0.25, 0.3, 0.5),
 *     Vec3d.ZERO,
 *     0.5f
 * );
 * </pre>
 *
 * @param name     锚点名称（唯一标识，如 "bottle_slot_1"）
 * @param position 相对位置（方块坐标，0.0~1.0）
 * @param rotation 旋转角度（弧度，XYZ 轴）
 * @param scale    缩放因子（1.0 为原始大小）
 */
public record AnchorPoint(
    String name,
    Vec3d position,
    Vec3d rotation,
    float scale
) {

    /**
     * 创建锚点（默认缩放 1.0）。
     *
     * @param name     锚点名称
     * @param position 相对位置
     * @param rotation 旋转角度
     */
    public AnchorPoint(String name, Vec3d position, Vec3d rotation) {
        this(name, position, rotation, 1.0f);
    }

    /**
     * 创建纯平移锚点。
     *
     * @param name     锚点名称
     * @param position 相对位置
     */
    public AnchorPoint(String name, Vec3d position) {
        this(name, position, Vec3d.ZERO, 1.0f);
    }

    /**
     * 将此锚点转换为变换矩阵。
     *
     * @return 对应的变换对象
     */
    public Transform transform() {
        return new Transform(position, rotation, scale);
    }

    // ==================== 预定义锚点常量 ====================

    /**
     * 酒瓶槽位 1（左侧）。
     */
    public static final AnchorPoint BOTTLE_SLOT_1 = new AnchorPoint(
        "bottle_slot_1",
        new Vec3d(0.25, 0.3, 0.5),
        Vec3d.ZERO,
        0.5f
    );

    /**
     * 酒瓶槽位 2（中间）。
     */
    public static final AnchorPoint BOTTLE_SLOT_2 = new AnchorPoint(
        "bottle_slot_2",
        new Vec3d(0.5, 0.3, 0.5),
        Vec3d.ZERO,
        0.5f
    );

    /**
     * 酒瓶槽位 3（右侧）。
     */
    public static final AnchorPoint BOTTLE_SLOT_3 = new AnchorPoint(
        "bottle_slot_3",
        new Vec3d(0.75, 0.3, 0.5),
        Vec3d.ZERO,
        0.5f
    );
}