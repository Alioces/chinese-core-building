package com.chinesecorebuilding.block.properties;

/**
 * 偏移量计算函数式接口。
 * <p>
 * 用于向方块构造函数传递自定义偏移量配置，
 * 避免为每种偏移组合创建子类。
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * new RoadSignsBlock(settings, () -> new float[]{1.5f, 2.0f});
 * </pre>
 * </p>
 *
 * @see Offset
 */
@FunctionalInterface
public interface OffsetFunction {

    /**
     * 获取偏移量配置。
     *
     * @return 长度为 2 的 float 数组 {@code {offsetXZ, offsetY}}，
     *         分别表示水平方向偏移像素和垂直方向偏移像素
     */
    float[] getOffset();
}