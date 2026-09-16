package com.chinesecorebuilding.block;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.properties.*;
import com.chinesecorebuilding.block.properties.model.Directional;
import com.chinesecorebuilding.block.properties.model.Layered;
import com.chinesecorebuilding.block.properties.model.SignTextProvider;
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
        implements Directional, Layered, GuiInteractive,
                   BlockEntityProvider, SignTextProvider {

    /**
     * 方块注册名。
     * <p>
     * 拼接 {@link ChineseCoreBuildingMod#MOD_ID} 得到完整注册 ID {@code chinese-core-building:test_sign}。
     * </p>
     */
    public static final String ID = "test_sign";

    /**
     * 全局单例实例。
     * <p>
     * 测试方块无需多实例，所有注册和引用都使用此单例。
     * </p>
     */
    public static final TestSignBlock INSTANCE = new TestSignBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque());

    /**
     * BlockEntityType 注册结果。
     * <p>
     * 由 {@link #register()} 赋值后供 {@link #createBlockEntity} 使用。
     * 使用 holder 数组延迟赋值是因为 BlockEntityType 构造时需要先拿到类型自身。
     * </p>
     */
    public static BlockEntityType<SignBlockEntity> ENTITY_TYPE;

    /**
     * 默认构造函数，初始化默认朝向并声明方块属性（nonOpaque）。
     *
     * @param settings 方块属性配置
     */
    public TestSignBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    /**
     * 一次性注册测试方块、对应物品和 BlockEntityType。
     * <p>
     * 由 {@link ChineseCoreBuildingMod#onInitialize()} 调用。
     * 使用 FabricBlockEntityTypeBuilder 避免原生 BlockEntityType 注册的冗长样板。
     * </p>
     */
    public static void register() {
        Registry.register(Registries.BLOCK, ChineseCoreBuildingMod.id(ID), INSTANCE);
        Registry.register(Registries.ITEM, ChineseCoreBuildingMod.id(ID), new BlockItem(INSTANCE, new Item.Settings()));

        // holder 数组是因为 BlockEntityType 构造时需要先拿到类型自身，用数组做延迟赋值
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

    /**
     * 放置时根据玩家朝向自动设置 FACING 属性。
     *
     * @param ctx 物品放置上下文
     * @return 带有正确 FACING 的方块状态
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, calculateDirection(ctx));
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
     * 返回完整方块的 outline 形状——虽然实际渲染不可见，但让玩家能点到方块。
     *
     * @param state   当前方块状态
     * @param world   世界视图
     * @param pos     方块位置
     * @param context 形状上下文
     * @return fullCube
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    /**
     * 返回空碰撞箱——纯装饰性方块，不阻挡移动。
     *
     * @param state   当前方块状态
     * @param world   世界视图
     * @param pos     方块位置
     * @param context 形状上下文
     * @return 空碰撞体
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    /**
     * 声明使用 CUTOUT 渲染层。
     *
     * @return {@link RenderLayerType#CUTOUT}
     */
    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }

    /**
     * 创建 SignBlockEntity 实例，由 Minecraft 原版在方块被放置到世界时调用。
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 新的 SignBlockEntity
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SignBlockEntity(ENTITY_TYPE, pos, state);
    }

    /**
     * 返回关联的 BlockEntityType，供 SignTextProvider 接口消费时做类型匹配。
     *
     * @return ENTITY_TYPE
     */
    @Override
    public BlockEntityType<? extends SignBlockEntity> getBlockEntityType() {
        return ENTITY_TYPE;
    }
}