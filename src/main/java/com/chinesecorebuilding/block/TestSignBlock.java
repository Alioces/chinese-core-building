package com.chinesecorebuilding.block;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.properties.*;
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
 * 独立测试方块，验证文字渲染、交互和 GUI 编辑。
 * <p>
 * 纯 invisible 方块：fullCube 选中框，无碰撞，无实体模型，文字渲染完全由 SignBlockRenderer 负责。
 * 没有方向偏移、没有边界框位移——文字位置完全由 TextLine.position 字段决定。
 * </p>
 */
public class TestSignBlock extends CustomBlock
        implements Directional, Layered, Interactive,
                   BlockEntityProvider, SignTextProvider {

    public static final String ID = "test_sign";
    public static final TestSignBlock INSTANCE = new TestSignBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque());
    public static BlockEntityType<SignBlockEntity> ENTITY_TYPE;

    public TestSignBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

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
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SignBlockEntity(ENTITY_TYPE, pos, state);
    }

    @Override
    public BlockEntityType<? extends SignBlockEntity> getBlockEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    public boolean shouldOpenGui() {
        return true;
    }
}