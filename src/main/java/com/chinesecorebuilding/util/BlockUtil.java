package com.chinesecorebuilding.util;

/**
 * 方块工具类。
 * <p>
 * 提供方块相关的常用工具方法，如像素单位转换等。
 * </p>
 */
public class BlockUtil {

    /**
     * 将像素值转换为方块单位（格）。
     * <p>
     * Minecraft 中方块的基本单位为 1 格 = 16 像素。
     * 此方法将像素值除以 16，转换为方块单位，用于构建 {@link net.minecraft.util.shape.VoxelShape}。
     * </p>
     * <p>
     * 示例：
     * <ul>
     *     <li>{@code blockConstraint(16)} → 1.0（1格）</li>
     *     <li>{@code blockConstraint(8)}  → 0.5（半格）</li>
     *     <li>{@code blockConstraint(3.5f)} → 0.21875（3.5像素）</li>
     * </ul>
     * </p>
     *
     * @param num 像素值（0~16 对应 0~1 格）
     * @return 方块单位值
     */
    public static float blockConstraint(float num) {
        // 1 格 = 16 像素，除以 16 转换为方块单位
        return num / 16.0f;
    }

    /**
     * 将像素值转换为方块单位（格）。
     * <p>
     * Minecraft 中方块的基本单位为 1 格 = 16 像素。
     * 此方法将像素值除以 16，转换为方块单位，用于构建 {@link net.minecraft.util.shape.VoxelShape}。
     * </p>
     * <p>
     * 示例：
     * <ul>
     *     <li>{@code blockConstraint(16)} → 1.0（1格）</li>
     *     <li>{@code blockConstraint(8)}  → 0.5（半格）</li>
     *     <li>{@code blockConstraint(3.5f)} → 0.21875（3.5像素）</li>
     * </ul>
     * </p>
     *
     * @param num 像素值（0~16 对应 0~1 格）
     * @return 方块单位值
     */
    public static double blockConstraint(double num) {
        // 1 格 = 16 像素，除以 16 转换为方块单位
        return num / 16.0;
    }
}
