package com.chinesecorebuilding.util;

/**
 * 方块工具类。
 * <p>
 * 提供方块相关的常用工具方法，如像素与方块单位转换等。
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
        return num / 16.0f;
    }

    /**
     * 将像素值转换为方块单位（格）。
     * <p>
     * 功能与 {@link #blockConstraint(float)} 相同，仅返回类型不同。
     * 适用于需要更高精度的场景。
     * </p>
     *
     * @param num 像素值
     * @return 方块单位值（double 精度）
     */
    public static double blockConstraint(double num) {
        return num / 16.0;
    }
}