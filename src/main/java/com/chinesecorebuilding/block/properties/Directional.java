package com.chinesecorebuilding.block.properties;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 可朝向方块接口。
 * <p>
 * 继承 {@link Rotatable}，将任意角度旋转特化为 4 方向水平朝向（north/east/south/west），
 * 每个方向对应 90° 增量。使用 {@code FACING} 属性替代父接口的 {@code ROTATION} 属性。
 * </p>
 * <p>
 * 渲染旋转由客户端 {@code RotationBakedModel} 在渲染阶段根据朝向实时完成，
 * 渲染偏移由 {@code OffsetBakedModel} 根据朝向实时完成，
 * blockstate JSON 中只需定义一个变体。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class MyBlock extends CustomBlock implements Directional {
 *     public MyBlock(Settings settings) {
 *         super(settings);
 *         setDefaultState(initDirection(getDefaultState()));
 *     }
 *
 *     &#64;Override
 *     public void appendProperties(StateManager.Builder&lt;Block, BlockState&gt; builder) {
 *         super.appendProperties(builder);
 *         Directional.super.appendProperties(builder);
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see Rotatable
 * @see Properties#HORIZONTAL_FACING
 */
public interface Directional extends Rotatable {

    /**
     * 水平朝向属性，支持 north/east/south/west 四个方向。
     * <p>
     * 使用 Minecraft 内置的 {@link Properties#HORIZONTAL_FACING}，
     * 替代父接口 {@link Rotatable} 的 {@link Rotatable#ROTATION} 属性。
     * </p>
     */
    DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    /**
     * 初始化朝向相关状态。
     * <p>
     * 接收当前默认状态，返回附加朝向属性后的新默认状态。
     * 实现类应在构造函数中配合 {@code setDefaultState} 使用：
     * <pre>
     * setDefaultState(initDirection(getDefaultState()));
     * </pre>
     * </p>
     *
     * @param currentState 当前默认方块状态（来自 {@code getDefaultState()}）
     * @return 附加朝向属性后的新默认状态
     */
    default BlockState initDirection(BlockState currentState) {
        return currentState.with(FACING, Direction.NORTH);
    }

    /**
     * 向状态管理器注册朝向属性。
     * <p>
     * 覆盖父接口 {@link Rotatable#appendProperties} 的行为，
     * 注册 {@link #FACING} 而非 {@link Rotatable#ROTATION}。
     * 实现类必须在 {@code appendProperties} 中显式委托此方法：
     * <pre>
     * Directional.super.appendProperties(builder);
     * </pre>
     * </p>
     *
     * @param builder 状态管理器构建器
     */
    @Override
    default void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * 根据朝向计算旋转值。
     * <p>
     * 将 {@link Direction} 转换为 {@link Rotatable#ROTATION} 属性值（0~15），
     * 每方向对应 90° 增量。
     * </p>
     *
     * @param direction 水平朝向
     * @return 对应的旋转值
     */
    default int directionToRotation(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0;
            case EAST  -> 1;
            case NORTH -> 2;
            case WEST  -> 3;
            default    -> 0;
        };
    }

    /**
     * 根据朝向属性计算渲染旋转角度（弧度）。
     * <p>
     * 覆盖父接口 {@link Rotatable#getRotationAngle} 的默认实现，
     * 从 {@link #FACING} 属性读取朝向，通过 {@link #directionToRotation} 转换为旋转值，
     * 再转换为弧度角。
     * </p>
     *
     * @param state 当前方块状态
     * @return 绕 Y 轴的旋转角度（弧度）
     */
    @Override
    default float getRotationAngle(BlockState state) {
        if (state == null || !state.contains(FACING)) return 0;
        int rotation = directionToRotation(state.get(FACING));
        return (float) (rotation * 90 * Math.PI / 180.0);
    }

    /**
     * 计算放置时的朝向。
     * <p>
     * 默认取玩家水平朝向的同方向，使方块放置后与玩家朝向一致。
     * 实现类可重写此方法以提供自定义的朝向计算逻辑。
     * </p>
     *
     * @param ctx 物品放置上下文，包含玩家朝向等信息
     * @return 放置时的朝向方向
     */
    default Direction calculateDirection(ItemPlacementContext ctx) {
        return ctx.getHorizontalPlayerFacing();
    }

    /**
     * 返回方块的轮廓箱（选中高亮框）。
     * <p>
     * 默认返回完整方块边界，实现类可按需重写以提供更精确的轮廓。
     * </p>
     *
     * @param state   当前方块状态
     * @param world   世界视图
     * @param pos     方块位置
     * @param context 形状上下文
     * @return 轮廓箱
     */
    @Override
    default VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    /**
     * 返回方块的碰撞箱。
     * <p>
     * 默认返回完整方块边界，实现类可按需重写以提供更精确的碰撞体。
     * </p>
     *
     * @param state   当前方块状态
     * @param world   世界视图
     * @param pos     方块位置
     * @param context 形状上下文
     * @return 碰撞箱
     */
    @Override
    default VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }
}
