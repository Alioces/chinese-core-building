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
 * 路标牌方块。
 * 通过 Directional 获得水平 4 方向朝向，通过 Offset 获得渲染偏移。
 */
public class RoadSignsBlock extends CustomBlock
        implements Directional, Layered, Offset {

    private float OFFSET_XOZ = BlockUtil.blockConstraint(1.5f);
    private float OFFSET_Y = BlockUtil.blockConstraint(0.0f);

    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(-2.0), 1, 1, BlockUtil.blockConstraint(10.0));
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(2.0), 1, 1, BlockUtil.blockConstraint(18.0));
    private static final VoxelShape EAST_SHAPE  = VoxelShapes.cuboid(BlockUtil.blockConstraint(2.0), 0, 0, BlockUtil.blockConstraint(18.0), 1, 1);
    private static final VoxelShape WEST_SHAPE  = VoxelShapes.cuboid(BlockUtil.blockConstraint(-2.0), 0, 0, BlockUtil.blockConstraint(10.0), 1, 1);

    public RoadSignsBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    public RoadSignsBlock(Settings settings, OffsetFunction offset) {
        this(settings);
        this.OFFSET_XOZ = BlockUtil.blockConstraint(offset.getOffset()[0]);
        this.OFFSET_Y = BlockUtil.blockConstraint(offset.getOffset()[1]);
    }

    public RoadSignsBlock(Settings settings, float OFFSET_Y) {
        this(settings, () -> new float[]{1.5f, OFFSET_Y});
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, calculateDirection(ctx));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        Directional.super.appendProperties(builder);
    }

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

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0, 0, 0, 0, 0, 0);
    }

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

    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }
}