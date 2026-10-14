package com.chinesecorebuilding.block;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.properties.GuiInteractive;
import com.chinesecorebuilding.block.properties.RenderLayerType;
import com.chinesecorebuilding.block.properties.model.Directional;
import com.chinesecorebuilding.block.properties.model.Layered;
import com.chinesecorebuilding.block.properties.model.SignTextProvider;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 单行文字方块。
 * <p>
 * 复用 {@link TextBlockEntity} 和 {@link SignTextProvider} 基础设施，
 * 但通过 {@link #isSingleLineMode()} 标识为单行模式，
 * GUI 层根据此标识显示简化的单行编辑界面。
 * </p>
 * <p>
 * 本方块是公共文字功能的单行模式实现，适用于只需要编辑单行文字的场景。
 * 能力接口：
 * <ul>
 *   <li>{@link Directional} — 4 方向水平朝向</li>
 *   <li>{@link Layered} — CUTOUT 渲染层</li>
 *   <li>{@link GuiInteractive} — 右键打开单行编辑 GUI</li>
 *   <li>{@link BlockEntityProvider} — 提供 BlockEntity</li>
 *   <li>{@link SignTextProvider} — 文字层渲染</li>
 * </ul>
 * </p>
 */
public class SingleLineSignBlock extends CustomBlock
        implements Directional, Layered, GuiInteractive,
                   BlockEntityProvider, SignTextProvider {

    /**
     * 方块注册名。
     */
    public static final String ID = "single_line_sign";

    /**
     * 全局单例实例。
     */
    public static final SingleLineSignBlock INSTANCE = new SingleLineSignBlock(
            FabricBlockSettings.create().strength(4.0f).nonOpaque()
    );

    /**
     * 构造函数，初始化默认朝向。
     *
     * @param settings 方块属性配置
     */
    public SingleLineSignBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    /**
     * 一次性注册单行文字方块、对应物品。
     * <p>
     * 复用 {@link TestSignBlock#ENTITY_TYPE}，不创建新的 BlockEntityType。
     * 这样所有文字方块共享同一个实体类型，简化渲染器和网络层逻辑。
     * </p>
     */
    public static void register() {
        Registry.register(Registries.BLOCK, ChineseCoreBuildingMod.id(ID), INSTANCE);
        Registry.register(Registries.ITEM, ChineseCoreBuildingMod.id(ID), new BlockItem(INSTANCE, new Item.Settings()));
    }

    /**
     * 向状态管理器注册 FACING 属性。
     */
    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        Directional.super.appendProperties(builder);
    }

    /**
     * 返回完整方块的 outline 形状。
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    /**
     * 返回空碰撞箱。
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    /**
     * 声明使用 CUTOUT 渲染层。
     */
    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }

    /**
     * 创建 TextBlockEntity 实例，复用测试方块的实体类型。
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 新的 TextBlockEntity
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TextBlockEntity(TestSignBlock.ENTITY_TYPE, pos, state);
    }

    /**
     * 返回关联的 BlockEntityType，复用 TestSignBlock 的实体类型。
     *
     * @return TestSignBlock.ENTITY_TYPE
     */
    @Override
    public BlockEntityType<? extends TextBlockEntity> getBlockEntityType() {
        return TestSignBlock.ENTITY_TYPE;
    }

    /**
     * 标识此方块为单行编辑模式。
     * <p>
     * GUI 层通过此方法判断是否显示简化的单行编辑界面。
     * 返回 true 时，GUI 只显示单行文字编辑，隐藏列表和添加/删除按钮。
     * </p>
     *
     * @return true 表示单行模式
     */
    public boolean isSingleLineMode() {
        return true;
    }
}