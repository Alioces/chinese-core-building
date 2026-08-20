package com.chinesecorebuilding.block.properties;

import net.minecraft.block.BlockState;

/**
 * 可朝向偏移方块标识接口。
 * <p>
 * 标识方块需要根据当前朝向对渲染模型应用位置偏移。
 * 偏移的具体方向和距离由实现类通过 {@link #getOffset} 方法自行定义，
 * 客户端渲染插件 {@code OffsetBakedModel} 仅调用该接口获取偏移值，
 * 不硬编码任何偏移映射，遵循开闭原则。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class MyBlock extends CustomBlock implements Directional, Offset {
 *     private float OFFSET = BlockUtil.blockConstraint(3.5f);
 *     &#64;Override
 *     public float[] getOffset(BlockState state) {
 *         return switch (state.get(FACING)) {
 *             case SOUTH -> new float[]{0, 0, -OFFSET};
 *             case NORTH -> new float[]{0, 0,  OFFSET};
 *             case EAST  -> new float[]{-OFFSET, 0, 0};
 *             case WEST  -> new float[]{ OFFSET, 0, 0};
 *             default    -> new float[]{0, 0, 0};
 *         };
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see Directional
 * @see com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel
 */
public interface Offset {

    /**
     * 根据方块状态计算渲染偏移量。
     * <p>
     * 返回一个长度为 3 的 float 数组：{@code {dx, dy, dz}}，
     * 分别表示 X 轴、Y 轴和 Z 轴的偏移量（单位：格）。
     * </p>
     * <p>
     * 实现类应根据方块状态中的朝向属性（如 {@code FACING}）计算偏移方向和距离。
     * 不同方块可定义不同的偏移量和偏移逻辑。
     * </p>
     *
     * @param state 当前方块状态
     * @return 偏移量数组 {@code {dx, dy, dz}}，默认返回 {@code {0, 0, 0}}（不偏移）
     */
    default float[] getOffset(BlockState state) {
        return new float[]{0, 0, 0};
    }
}