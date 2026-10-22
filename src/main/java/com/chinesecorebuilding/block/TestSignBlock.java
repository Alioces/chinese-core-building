package com.chinesecorebuilding.block;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.entity.ChineseCoreBuildingEntities;
import com.chinesecorebuilding.block.entity.TextBlockEntity;
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
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 文字方块测试实现。
 * <p>
 * 验证公共文字功能基础设施的完整工作流程：文字渲染、交互和 GUI 编辑。
 * 纯 invisible 方块：fullCube 选中框，无碰撞，无实体模型，文字渲染完全由 SignBlockRenderer 负责。
 * 没有方向偏移、没有边界框位移——文字位置完全由 TextLine.position 字段决定。
 * </p>
 * <p>
 * 本方块是公共文字功能的参考实现，展示了如何接入文字功能所需的全部接口。
 * 其他需要文字功能的方块可以参考此类的实现方式。
 * </p>
 */
public class TestSignBlock extends CustomBlock
        implements Directional, Layered, GuiInteractive,
                   BlockEntityProvider, SignTextProvider {

    /**
     * 方块注册名。
     * <p>
     * 拼接 {@link ChineseCoreBuildingMod#MOD_ID} 得到完整注册 ID {@code chinese-core-building:test_sign}。
     * 作为公共文字功能的测试方块，用于验证文字渲染、交互和 GUI 编辑的完整流程。
     * </p>
     */
    public static final String ID = "test_sign";

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
     * 创建 TextBlockEntity 实例，由 Minecraft 原版在方块被放置到世界时调用。
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 新的 TextBlockEntity
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TextBlockEntity(pos, state);
    }

    /**
     * 返回关联的 BlockEntityType，供 SignTextProvider 接口消费时做类型匹配。
     *
     * @return {@link ChineseCoreBuildingEntities#TEXT_BLOCK_ENTITY_TYPE}
     */
    @Override
    public BlockEntityType<? extends TextBlockEntity> getBlockEntityType() {
        return ChineseCoreBuildingEntities.TEXT_BLOCK_ENTITY_TYPE;
    }
}