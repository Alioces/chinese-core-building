package com.chinesecorebuilding.block;

import com.chinesecorebuilding.block.properties.Rotatable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;

import java.util.Objects;

/**
 * 可旋转路标牌方块。
 * <p>
 * 通过实现 {@link Rotatable} 接口获得旋转能力，
 * 支持 16 角度精细旋转（每格 22.5°），放置时自动面向玩家。
 * </p>
 *
 * @see Rotatable
 */
public class RotatableBlock extends CustomBlock implements Rotatable {

    public RotatableBlock(Settings settings) {
        super(settings);
        setDefaultState(initRotation(getDefaultState()));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        Rotatable.super.appendProperties(builder);
    }

    /**
     * 决定方块放置时的旋转角度。
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
