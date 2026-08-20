package com.chinesecorebuilding.block;

import com.chinesecorebuilding.block.properties.Rotatable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;

import java.util.Objects;

/**
 * 可旋转方块基类。
 * <p>
 * 通过实现 {@link Rotatable} 接口获得 16 角度精细旋转能力（每档 22.5°），
 * 放置时自动根据玩家朝向计算旋转角度。
 * </p>
 * <p>
 * 与 {@link com.chinesecorebuilding.block.roadSigns.RotatableSignBlock} 的区别：
 * 本类位于通用包下，可作为更通用的旋转方块基类扩展使用。
 * </p>
 *
 * @see Rotatable
 */
public class RotatableBlock extends CustomBlock implements Rotatable {

    /**
     * 构造函数。
     *
     * @param settings 方块属性配置
     */
    public RotatableBlock(Settings settings) {
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
     * 决定方块放置时的旋转角度。
     * <p>
     * 根据玩家水平朝向计算对应的旋转值（0~15），
     * 使方块放置后面向玩家。
     * </p>
     *
     * @param ctx 物品放置上下文
     * @return 带有正确旋转角度的方块状态
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(
                super.getPlacementState(ctx)).with(ROTATION, calculateRotation(
                        ctx.getHorizontalPlayerFacing().getHorizontal()
                    )
                );
    }
}