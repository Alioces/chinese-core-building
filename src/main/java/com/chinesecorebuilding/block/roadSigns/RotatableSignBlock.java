package com.chinesecorebuilding.block.roadSigns;

import com.chinesecorebuilding.block.CustomBlock;
import com.chinesecorebuilding.block.properties.Rotatable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

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
public class RotatableSignBlock extends CustomBlock implements Rotatable {

    public RotatableSignBlock(Settings settings) {
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
     * <p>
     * 将玩家水平朝向映射到最接近的旋转值（0~15），使路标面向玩家。
     * 每个旋转值对应 22.5° 增量：
     * <ul>
     *     <li>0  → 南（0°）</li>
     *     <li>4  → 西（90°）</li>
     *     <li>8  → 北（180°）</li>
     *     <li>12 → 东（270°）</li>
     * </ul>
     * </p>
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(
                super.getPlacementState(ctx)).with(ROTATION, calculateRotation(
                        ctx.getHorizontalPlayerFacing().getHorizontal()
                    )
                );
    }

    @Override
    public int calculateRotation(int rotation) {
        return ((rotation * 4 + 8) % 16);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0, 0, 0, 0, 0, 0);
    }
}
