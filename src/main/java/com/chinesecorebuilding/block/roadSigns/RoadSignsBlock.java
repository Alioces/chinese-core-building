package com.chinesecorebuilding.block.roadSigns;

import com.chinesecorebuilding.block.CustomBlock;
import com.chinesecorebuilding.block.properties.*;
import com.chinesecorebuilding.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 路标牌方块类。
 * <p>
 * 通过实现 {@link Directional} 接口获得水平朝向能力，
 * 支持 4 个水平朝向（东南西北），放置时自动与玩家朝向一致。
 * 模型紧贴方块背面边缘，碰撞体为空（玩家可穿过），
 * 轮廓箱根据朝向动态偏移以匹配模型实际位置。
 * </p>
 * <p>
 * 方块状态属性：
 * <ul>
 *     <li>{@code facing} — 水平朝向，取值 north/east/south/west</li>
 * </ul>
 * </p>
 *
 * @see CustomBlock
 * @see Directional
 */
public class RoadSignsBlock extends CustomBlock implements Directional, Layered, Offset {

    /**
     * 偏移量（单位：格）。
     * <p>
     * 将 3.5 像素通过 {@link BlockUtil#blockConstraint(float)} 转换为方块单位。
     * 3.5 像素 = 3.5 / 16 = 0.21875 格。
     * </p>
     */
    private float OFFSET_XOZ = BlockUtil.blockConstraint(1.5f);
    private float OFFSET_Y = BlockUtil.blockConstraint(0.0f);

    // ====== 各朝向的轮廓箱（VoxelShape） ======
    // 模型默认位于 +Z 侧（南侧）边缘，旋转后位于对应朝向侧边缘
    // 偏移将模型从边缘推向中心，轮廓箱需覆盖渲染后的模型位置

    /**
     * 朝北时，模型旋转 180° 后在北侧边缘，偏移 +Z 推向中心
     */
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(-2.0), 1, 1, BlockUtil.blockConstraint(10.0));
    /**
     * 朝南时，模型无旋转在南侧边缘，偏移 -Z 推向中心
     */
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(2.0), 1, 1, BlockUtil.blockConstraint(18.0));
    /**
     * 朝东时，模型旋转 270° 后在东侧边缘，偏移 -X 推向中心
     */
    private static final VoxelShape EAST_SHAPE = VoxelShapes.cuboid(BlockUtil.blockConstraint(2.0), 0, 0, BlockUtil.blockConstraint(18.0), 1, 1);
    /**
     * 朝西时，模型旋转 90° 后在西侧边缘，偏移 +X 推向中心
     */
    private static final VoxelShape WEST_SHAPE = VoxelShapes.cuboid(BlockUtil.blockConstraint(-2.0), 0, 0, BlockUtil.blockConstraint(10.0), 1, 1);

    /**
     * 构造函数。
     * <p>
     * 通过 {@link Directional#initDirection} 设置默认方块状态为 {@code facing=north}。
     * </p>
     *
     * @param settings 方块属性配置（硬度、非透明等）
     */
    public RoadSignsBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    /**
     * 构造函数（支持自定义偏移量）。
     *
     * @param settings 方块属性配置
     * @param offset   偏移量计算函数，返回 {@code {offsetXZ, offsetY}}（像素值）
     */
    public RoadSignsBlock(Settings settings, OffsetFunction offset) {
        this(settings);

        this.OFFSET_XOZ = BlockUtil.blockConstraint(offset.getOffset()[0]);
        this.OFFSET_Y = BlockUtil.blockConstraint(offset.getOffset()[1]);
    }

    /**
     * 构造函数（仅自定义 Y 轴偏移量）。
     * <p>
     * 水平偏移量固定为 1.5 像素，仅允许调整 Y 轴偏移。
     * </p>
     *
     * @param settings 方块属性配置
     * @param OFFSET_Y Y 轴偏移量（像素值）
     */
    public RoadSignsBlock(Settings settings, float OFFSET_Y) {
        this(settings, () -> new float[]{1.5f, OFFSET_Y});
    }

    /**
     * 决定方块放置时的朝向。
     * <p>
     * 委托 {@link Directional#calculateDirection} 计算朝向，
     * 默认取玩家水平朝向的同方向，使路标放置后与玩家朝向一致。
     * </p>
     *
     * @param ctx 物品放置上下文，包含玩家朝向等信息
     * @return 带有正确朝向的方块状态
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, calculateDirection(ctx));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        Directional.super.appendProperties(builder);
    }

    /**
     * 返回方块的轮廓箱（选中高亮框）。
     * <p>
     * 根据朝向返回偏移后的轮廓箱，使模型紧贴方块背面边缘，
     * 高亮框与模型实际位置一致。
     * </p>
     *
     * @param state   当前方块状态（包含 facing 属性）
     * @param world   世界视图
     * @param pos     方块位置
     * @param context 形状上下文
     * @return 对应朝向的轮廓箱
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
     * 返回方块的碰撞箱。
     * <p>
     * 返回空碰撞体，玩家和实体可以自由穿过此方块。
     * </p>
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
     * 根据朝向计算渲染偏移量。
     * <p>
     * 模型默认位于 +Z 侧（南侧）边缘，旋转后位于对应朝向侧边缘。
     * 偏移将模型从边缘推向方块中心 3.5 像素。
     * </p>
     *
     * @return 偏移量数组 {dx, dz}
     */
    @Override
    public float[] getOffset(BlockState state) {
        return switch (state.get(FACING)) {
            case SOUTH -> new float[]{0, OFFSET_Y, OFFSET_XOZ};  // 南侧边缘 → 向中心推（-Z）
            case NORTH -> new float[]{0, OFFSET_Y, -OFFSET_XOZ};  // 北侧边缘 → 向中心推（+Z）
            case EAST -> new float[]{OFFSET_XOZ, OFFSET_Y, 0};       // 东侧边缘 → 向中心推（-X）
            case WEST -> new float[]{-OFFSET_XOZ, OFFSET_Y, 0};       // 西侧边缘 → 向中心推（+X）
            default -> new float[]{0, 0, 0};
        };
    }

    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }
}