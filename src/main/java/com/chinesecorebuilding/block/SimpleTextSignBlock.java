package com.chinesecorebuilding.block;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.block.properties.GuiInteractive;
import com.chinesecorebuilding.block.properties.RenderLayerType;
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
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.List;

/**
 * 简化版文字方块（测试用）。
 * <p>
 * 使用 {@link GuiInteractive#GUI_TYPE_SIMPLE} 简化版 GUI，
 * 隐藏位置参数控件，用户只能编辑文字内容、颜色、缩放和发光属性。
 * </p>
 * <p>
 * 文字位置根据方块朝向自动设置到距离玩家最远的那个面上：
 * <ul>
 *   <li>朝北（NORTH）→ 文字在北侧外表面（Z=0.01）</li>
 *   <li>朝南（SOUTH）→ 文字在南侧外表面（Z=1.01）</li>
 *   <li>朝东（EAST）→ 文字在东侧外表面（X=1.01）</li>
 *   <li>朝西（WEST）→ 文字在西侧外表面（X=0.01）</li>
 * </ul>
 * 文字渲染面始终与方块朝向一致。
 * </p>
 * <p>
 * 本方块使用空气方块模型，方块本身完全不可见，仅用于测试文字功能。
 * 能力接口：
 * <ul>
 *   <li>{@link Directional} — 4 方向水平朝向</li>
 *   <li>{@link Layered} — CUTOUT 渲染层</li>
 *   <li>{@link GuiInteractive} — 右键打开简化版编辑 GUI</li>
 *   <li>{@link BlockEntityProvider} — 提供 BlockEntity</li>
 *   <li>{@link SignTextProvider} — 文字层渲染</li>
 * </ul>
 * </p>
 */
public class SimpleTextSignBlock extends CustomBlock
        implements Directional, Layered, GuiInteractive,
                   BlockEntityProvider, SignTextProvider {

    /**
     * 方块注册名。
     */
    public static final String ID = "simple_text_sign";

    /**
     * 全局单例实例。
     */
    public static final SimpleTextSignBlock INSTANCE = new SimpleTextSignBlock(
            FabricBlockSettings.create().strength(4.0f).nonOpaque()
    );

    /**
     * BlockEntityType 注册结果。
     * <p>
     * 独立的实体类型，确保渲染器正确关联。
     * 由 {@link #register()} 赋值后供 {@link #createBlockEntity} 使用。
     * </p>
     */
    public static BlockEntityType<TextBlockEntity> ENTITY_TYPE;

    /**
     * 构造函数，初始化默认朝向。
     *
     * @param settings 方块属性配置
     */
    public SimpleTextSignBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    /**
     * 一次性注册简化版文字方块、对应物品和独立的 BlockEntityType。
     */
    public static void register() {
        Registry.register(Registries.BLOCK, ChineseCoreBuildingMod.id(ID), INSTANCE);
        Registry.register(Registries.ITEM, ChineseCoreBuildingMod.id(ID), new BlockItem(INSTANCE, new Item.Settings()));

        BlockEntityType<TextBlockEntity>[] holder = new BlockEntityType[1];
        holder[0] = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                ChineseCoreBuildingMod.id(ID + "_entity"),
                FabricBlockEntityTypeBuilder.create(
                        (pos, state) -> new TextBlockEntity(holder[0], pos, state),
                        INSTANCE
                ).build()
        );
        ENTITY_TYPE = holder[0];
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
     * 根据方块朝向计算文字的默认位置（世界坐标）。
     * <p>
     * <b>重要</b>：由于 {@link com.chinesecorebuilding.client.renderer.TextBlockRenderer}
     * 作为 DECORATOR 层不再应用旋转变换，此方法返回的是<b>世界坐标</b>
     * （相对于方块原点，不受方块朝向影响）。
     * </p>
     * <p>
     * 坐标系说明：
     * <ul>
     *   <li>原点 (0,0,0) = 方块西北下角</li>
     *   <li>+X = 东方向</li>
     *   <li>+Y = 上方向</li>
     *   <li>+Z = 南方向</li>
     * </ul>
     * </p>
     * <p>
     * 文字显示在方块的<b>背面</b>（距离放置者最远的面）：
     * <ul>
     *   <li>朝北（NORTH）→ 背面是南侧 → Z=1.01</li>
     *   <li>朝南（SOUTH）→ 背面是北侧 → Z=0.01</li>
     *   <li>朝东（EAST）→ 背面是西侧 → X=0.01</li>
     *   <li>朝西（WEST）→ 背面是东侧 → X=1.01</li>
     * </ul>
     * </p>
     *
     * @param facing 方块朝向
     * @return 文字位置 Vec3d（世界坐标，不会被旋转）
     */
    public static Vec3d getTextPositionForFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3d(0.5, 0.6, 1.01);   // 背面=南侧
            case SOUTH -> new Vec3d(0.5, 0.6, 0.01);   // 背面=北侧
            case EAST  -> new Vec3d(0.01, 0.6, 0.5);  // 背面=西侧
            case WEST  -> new Vec3d(1.01, 0.6, 0.5);  // 背面=东侧
            default    -> new Vec3d(0.5, 0.6, 0.01);
        };
    }

    /**
     * 创建 TextBlockEntity 实例，根据方块朝向设置初始文字位置。
     * <p>
     * 与父类不同，此方法会根据当前方块状态的 FACING 属性，
     * 为初始 TextLine 设置正确的显示位置。
     * </p>
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 新的 TextBlockEntity
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        TextBlockEntity entity = new TextBlockEntity(ENTITY_TYPE, pos, state);
        Direction facing = state.get(FACING);
        Vec3d textPos = getTextPositionForFacing(facing);
        TextLine initialLine = TextLine.builder()
                .text(Text.literal(""))
                .position(textPos)
                .color(0x000000)
                .scale(1.0f)
                .glowing(false)
                .build();
        entity.setLines(List.of(initialLine));
        return entity;
    }

    /**
     * 返回关联的 BlockEntityType，使用本方块专用的实体类型。
     *
     * @return ENTITY_TYPE
     */
    @Override
    public BlockEntityType<? extends TextBlockEntity> getBlockEntityType() {
        return ENTITY_TYPE;
    }

    /**
     * 返回此方块对应的 GUI 类型标识符。
     * <p>
     * 简化版文字方块返回 {@link GuiInteractive#GUI_TYPE_SIMPLE}，
     * 客户端据此创建 {@link com.chinesecorebuilding.client.gui.SimpleTextScreen}。
     * </p>
     *
     * @return {@link GuiInteractive#GUI_TYPE_SIMPLE}
     */
    @Override
    public String getGuiType() {
        return GUI_TYPE_SIMPLE;
    }
}