package com.chinesecorebuilding.block.roadSigns;

import com.chinesecorebuilding.block.CustomBlock;
import com.chinesecorebuilding.block.properties.model.Rotatable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 可旋转路标牌方块。
 * <p>
 * 通过实现 {@link Rotatable} 接口获得旋转能力，
 * 支持 16 角度精细旋转（每格 22.5°），放置时自动面向玩家。
 * </p>
 *
 * @see Rotatable
 */
public class RotatableSignBlock extends CustomBlock implements Rotatable {

    /**
     * 构造函数。
     *
     * @param settings 方块属性配置
     */
    public RotatableSignBlock(Settings settings) {
        super(settings);
        setDefaultState(initRotation(getDefaultState()));
    }

    /**
     * 向状态管理器注册旋转属性。
     *
     * @param builder 状态管理器构建器
     */
    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        Rotatable.super.appendProperties(builder);
    }

    /**
     * 计算旋转值。
     * <p>
     * 将玩家朝向索引（0~3 对应南/西/北/东）映射到 16 档旋转值，
     * 公式 {@code (rotation * 4 + 8) % 16} 确保路标始终面向玩家。
     * </p>
     *
     * @param rotation 玩家朝向索引
     * @return 对应的 16 档旋转值（0~15）
     */
    @Override
    public int calculateRotation(int rotation) {
        return ((rotation * 4 + 8) % 16);
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
}