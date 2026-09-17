package com.chinesecorebuilding.block.roadSigns;

import com.chinesecorebuilding.block.CustomBlock;
import com.chinesecorebuilding.block.properties.*;
import com.chinesecorebuilding.block.properties.model.Directional;
import com.chinesecorebuilding.block.properties.model.Layered;
import com.chinesecorebuilding.block.properties.model.Offset;
import com.chinesecorebuilding.util.BlockUtil;
import com.chinesecorebuilding.util.OffsetFunction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 路标牌方块。
 * <p>
 * 通过 {@link Directional} 获得水平 4 方向朝向，
 * 通过 {@link Offset} 获得与朝向绑定的渲染偏移（路标板面从方块中心向外伸），
 * 通过 {@link Layered} 声明为 CUTOUT 渲染层以支持板面透明。
 * </p>
 * <p>
 * 提供三种构造：默认 1.5 单位水平偏移 + 0 Y 偏移；自定义 OffsetFunction；自定义 Y 偏移（XZ 用默认 1.5）。
 * 所有路牌方块实例都由此类创建，注册在 {@link com.chinesecorebuilding.block.ChineseCoreBuildingBlocks} 中。
 * </p>
 *
 * @see Directional
 * @see Offset
 * @see OffsetFunction
 */
public class RoadSignsBlock extends CustomBlock
        implements Directional, Layered, Offset {

    /**
     * 路牌板面从方块中心向外的水平偏移（X/Z 方向，单位：方块）。
     * <p>
     * 默认 1.5 单位，表示板面从方块中心向外的水平距离。
     * 可通过构造函数或 {@link com.chinesecorebuilding.util.OffsetFunction} 自定义。
     * </p>
     */
    private float OFFSET_XOZ = BlockUtil.blockConstraint(1.5f);

    /**
     * 路牌板面的垂直偏移（Y 方向，单位：方块）。
     * <p>
     * 默认 0.0 单位（贴地面），可通过构造函数自定义。
     * </p>
     */
    private float OFFSET_Y = BlockUtil.blockConstraint(0.0f);

    /**
     * 北方向边界框。
     * <p>
     * 方块北面向外延伸 0.125 单位（-2.0 像素）到 0.625 单位（10.0 像素），
     * 用于 {@link #getOutlineShape} 返回与朝向匹配的可视区域。
     * </p>
     */
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(-2.0), 1, 1, BlockUtil.blockConstraint(10.0));

    /**
     * 南方向边界框。
     * <p>
     * 方块南面向外延伸，与 {@link #NORTH_SHAPE} 对称。
     * </p>
     */
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(2.0), 1, 1, BlockUtil.blockConstraint(18.0));

    /**
     * 东方向边界框。
     * <p>
     * 方块东面向外延伸，X 轴方向扩展。
     * </p>
     */
    private static final VoxelShape EAST_SHAPE  = VoxelShapes.cuboid(BlockUtil.blockConstraint(2.0), 0, 0, BlockUtil.blockConstraint(18.0), 1, 1);

    /**
     * 西方向边界框。
     * <p>
     * 方块西面向外延伸，与 {@link #EAST_SHAPE} 对称。
     * </p>
     */
    private static final VoxelShape WEST_SHAPE  = VoxelShapes.cuboid(BlockUtil.blockConstraint(-2.0), 0, 0, BlockUtil.blockConstraint(10.0), 1, 1);

    /**
     * 默认构造函数，使用 1.5 单位水平偏移 + 0 Y 偏移。
     *
     * @param settings 方块属性配置（材质、碰撞、硬度等）
     */
    public RoadSignsBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    /**
     * 自定义偏移构造函数，通过 {@link OffsetFunction} 完全控制 XZ 和 Y 偏移。
     * <p>
     * OffsetFunction 返回 float[2]：[0] = XZ 偏移（水平），[1] = Y 偏移（垂直）。
     * </p>
     *
     * @param settings 方块属性配置
     * @param offset   偏移值提供器
     */
    public RoadSignsBlock(Settings settings, OffsetFunction offset) {
        this(settings);
        this.OFFSET_XOZ = BlockUtil.blockConstraint(offset.getOffset()[0]);
        this.OFFSET_Y = BlockUtil.blockConstraint(offset.getOffset()[1]);
    }

    /**
     * 仅自定义 Y 偏移的便捷构造函数，XZ 偏移使用默认 1.5 单位。
     *
     * @param settings 方块属性配置
     * @param OFFSET_Y 垂直偏移（单位：方块）
     */
    public RoadSignsBlock(Settings settings, float OFFSET_Y) {
        this(settings, () -> new float[]{1.5f, OFFSET_Y});
    }

    /**
     * 向状态管理器注册 FACING 属性。
     *
     * @param builder 状态管理器构建器
     */
    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        Directional.super.appendProperties(builder);
    }

    /**
     * 返回当前朝向对应的可视边界框（outline shape）。
     * <p>
     * 边界框比完整方块略大，让玩家在板面任意位置点击都能命中方块。
     * </p>
     *
     * @param state   当前方块状态
     * @param world   世界视图
     * @param pos     方块位置
     * @param context 形状上下文
     * @return 与 FACING 对应的 VoxelShape
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> VoxelShapes.fullCube();
        };
    }

    /**
     * 返回空碰撞箱，路标是装饰性方块不阻挡移动。
     *
     * @param state   当前方块状态
     * @param world   世界视图
     * @param pos     方块位置
     * @param context 形状上下文
     * @return 空碰撞体
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0, 0, 0, 0, 0, 0);
    }

    /**
     * 返回与朝向绑定的渲染偏移向量。
     * <p>
     * 根据 FACING 决定偏移方向：
     * SOUTH → +Z 偏移，NORTH → -Z 偏移，EAST → +X 偏移，WEST → -X 偏移。
     * Y 分量始终使用 OFFSET_Y。
     * 偏移值经过 {@link BlockUtil#blockConstraint} 约束到合法范围。
     * </p>
     *
     * @param state 当前方块状态
     * @return 三维偏移向量 float[]{X, Y, Z}，单位：方块
     */
    @Override
    public float[] getOffset(BlockState state) {
        return switch (state.get(FACING)) {
            case SOUTH -> new float[]{0, OFFSET_Y, OFFSET_XOZ};
            case NORTH -> new float[]{0, OFFSET_Y, -OFFSET_XOZ};
            case EAST  -> new float[]{OFFSET_XOZ, OFFSET_Y, 0};
            case WEST  -> new float[]{-OFFSET_XOZ, OFFSET_Y, 0};
            default -> new float[]{0, 0, 0};
        };
    }

    /**
     * 声明路标使用 CUTOUT 渲染层，支持板面透明区域。
     *
     * @return {@link RenderLayerType#CUTOUT}
     */
    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }
}