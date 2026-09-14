package com.chinesecorebuilding.block;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.properties.*;
import com.chinesecorebuilding.util.BlockUtil;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 独立测试方块，用于验证文字渲染架构。
 * <p>
 * 自包含 Block、BlockEntityType、BlockItem 的全部注册逻辑，
 * 与 RoadSignsBlock 现有代码零耦合。
 * 有 Directional（4 方向旋转）和 Offset（3D 偏移）能力，
 * 可验证文字层在两种变换下是否与方块本体对齐。
 * </p>
 * <p>
 * 能力接口组合：Directional + Layered + Offset + BlockEntityProvider + SignTextProvider。
 * </p>
 * <p>
 * 用法：
 * <pre>
 * // ChineseCoreBuildingMod.onInitialize() 里加一行
 * TestSignBlock.register();
 * </pre>
 * </p>
 *
 * @see SignBlockEntity
 * SignBlockRenderer
 */
public class TestSignBlock extends CustomBlock
        implements Directional, Layered, Offset,
                   BlockEntityProvider, SignTextProvider {

    /** 方块注册 ID */
    public static final String ID = "test_sign";

    /** 测试方块单例实例 */
    public static final TestSignBlock INSTANCE = new TestSignBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque());

    /** 专属 BlockEntityType，由 {@link #register()} 赋值 */
    public static BlockEntityType<SignBlockEntity> ENTITY_TYPE;

    /** 轮廓箱：北侧显示面在南边 */
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(-2.0), 1, 1, BlockUtil.blockConstraint(10.0));
    /** 轮廓箱：南侧显示面在北边 */
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0, 0, BlockUtil.blockConstraint(2.0), 1, 1, BlockUtil.blockConstraint(18.0));
    /** 轮廓箱：东侧显示面在西边 */
    private static final VoxelShape EAST_SHAPE  = VoxelShapes.cuboid(BlockUtil.blockConstraint(2.0), 0, 0, BlockUtil.blockConstraint(18.0), 1, 1);
    /** 轮廓箱：西侧显示面在东边 */
    private static final VoxelShape WEST_SHAPE  = VoxelShapes.cuboid(BlockUtil.blockConstraint(-2.0), 0, 0, BlockUtil.blockConstraint(10.0), 1, 1);

    /**
     * 构造函数，初始化默认朝向为 NORTH。
     *
     * @param settings 方块设置（硬度 4.0，不透明）
     */
    public TestSignBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    /**
     * 一次性注册 Block + BlockItem + BlockEntityType。
     * <p>
     * BlockEntityType 的 factory 使用数组 holder 破循环依赖：
     * 先注册 BlockEntityType（暂存到 holder[0]），再从 holder[0] 取值赋给 ENTITY_TYPE。
     * </p>
     */
    public static void register() {
        Registry.register(Registries.BLOCK, ChineseCoreBuildingMod.id(ID), INSTANCE);
        Registry.register(Registries.ITEM, ChineseCoreBuildingMod.id(ID), new BlockItem(INSTANCE, new Item.Settings()));

        BlockEntityType<SignBlockEntity>[] holder = new BlockEntityType[1];
        holder[0] = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                ChineseCoreBuildingMod.id(ID + "_entity"),
                FabricBlockEntityTypeBuilder.create(
                        (pos, state) -> new SignBlockEntity(holder[0], pos, state),
                        INSTANCE
                ).build()
        );
        ENTITY_TYPE = holder[0];

        ChineseCoreBuildingMod.LOGGER.info("[TEST] TestSignBlock registered (Block + BlockItem + BlockEntityType)");
    }

    /**
     * 返回放置状态，附加 FACING 属性。
     * <p>
     * 覆盖默认实现以确保放置时使用玩家朝向。
     * </p>
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, calculateDirection(ctx));
    }

    /**
     * 向状态管理器注册 FACING 属性。
     * <p>
     * 委托 Directional.super.appendProperties 实现，
     * 因为 Directional 用 FACING（4 方向）替代 Rotatable 的 ROTATION（16 档）。
     * </p>
     */
    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        Directional.super.appendProperties(builder);
    }

    /**
     * 返回当前朝向对应的轮廓箱（选中高亮框）。
     *
     * @return 朝向匹配的 VoxelShape，默认 fullCube
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST  -> EAST_SHAPE;
            case WEST  -> WEST_SHAPE;
            default -> VoxelShapes.fullCube();
        };
    }

    /**
     * 返回空碰撞箱，使方块可被穿透（用于测试时不挡路）。
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0, 0, 0, 0, 0, 0);
    }

    /**
     * 根据朝向返回 3D 偏移，与 RoadSignsBlock 保持一致。
     * <p>
     * 每个方向偏移 BlockUtil.blockConstraint(1.5f) ≈ 0.09375 格，
     * 使显示面刚好贴在方块边界上。
     * </p>
     */
    @Override
    public float[] getOffset(BlockState state) {
        float ox = BlockUtil.blockConstraint(1.5f);
        return switch (state.get(FACING)) {
            case SOUTH -> new float[]{0, 0, ox};
            case NORTH -> new float[]{0, 0, -ox};
            case EAST  -> new float[]{ox, 0, 0};
            case WEST  -> new float[]{-ox, 0, 0};
            default -> new float[]{0, 0, 0};
        };
    }

    /**
     * 声明渲染层为 CUTOUT（路牌通常有透明区域）。
     */
    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }

    /**
     * 创建 BlockEntity 实例。
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 新创建的 SignBlockEntity
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SignBlockEntity(ENTITY_TYPE, pos, state);
    }

    /**
     * 返回此类方块专属的 BlockEntityType。
     *
     * @return {@link #ENTITY_TYPE}
     */
    @Override
    public BlockEntityType<? extends SignBlockEntity> getBlockEntityType() {
        return ENTITY_TYPE;
    }
}