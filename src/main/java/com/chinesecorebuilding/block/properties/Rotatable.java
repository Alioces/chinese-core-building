package com.chinesecorebuilding.block.properties;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 可旋转方块接口。
 * <p>
 * 为方块提供旋转能力支持，接口仅定义旋转属性的注册与状态管理，
 * 具体地旋转角度计算与放置逻辑由实现类自行决定。
 * </p>
 * <p>
 * 旋转渲染由客户端 {@code RotationBakedModel} 在渲染阶段实时完成，
 * blockstate JSON 中只需定义一个变体。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class MyBlock extends CustomBlock implements Rotatable {
 *     public MyBlock(Settings) {
 *         super(settings);
 *         setDefaultState(initRotation(getDefaultState()));
 *     }
 *
 *     &#64;Override
 *     protected void appendProperties(StateManager.Builder&lt;Block, BlockState&gt; builder) {
 *         super.appendProperties(builder);
 *         Rotatable.super.appendProperties(builder);
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see Properties#ROTATION
 */
public interface Rotatable {

    /**
     * 旋转属性。
     * <p>
     * 使用 Minecraft 内置的 {@link Properties#ROTATION}，
     * 支持 0~15 共 16 个值，具体角度映射由实现类定义。
     * </p>
     */
    IntProperty ROTATION = Properties.ROTATION;

    /**
     * 初始化旋转相关状态。
     * <p>
     * 接收当前默认状态，返回附加旋转属性后的新默认状态。
     * 实现类应在构造函数中配合 {@code setDefaultState} 使用：
     * <pre>
     * setDefaultState(initRotation(getDefaultState()));
     * </pre>
     * 必须在 {@link Block} 构造函数执行完毕后调用（即子类构造函数体内）。
     * </p>
     *
     * @param currentState 当前默认方块状态（来自 {@code getDefaultState()}）
     * @return 附加旋转属性后的新默认状态
     */
    default BlockState initRotation(BlockState currentState) {
        return currentState.with(ROTATION, 0);
    }

    /**
     * 向状态管理器注册旋转属性。
     * <p>
     * 由于 Java 类方法优先于接口默认方法，实现类必须在
     * {@link Block#appendProperties} 中显式委托此方法：
     * <pre>
     * Rotatable.super.appendProperties(builder);
     * </pre>
     * </p>
     *
     * @param builder 状态管理器构建器
     */
    default void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    /**
     * 计算旋转值。
     * <p>
     * 默认实现不对参数进行任何变化，原样返回当前旋转值。
     * 实现类可重写此方法以提供自定义的旋转角度计算逻辑。
     * </p>
     *
     * @param rotation 当前旋转值（0~15）
     * @return 计算后的旋转值
     */
    default int calculateRotation(int rotation) {
        return rotation;
    }

    /**
     * 根据方块状态计算渲染旋转角度（弧度）。
     * <p>
     * 默认实现从方块状态中读取 {@link #ROTATION} 属性，
     * 通过 {@link #calculateRotation} 计算最终旋转值，
     * 然后转换为弧度角（每档 22.5°）。
     * </p>
     * <p>
     * 子接口（如 {@link Directional}）可重写此方法，
     * 从不同的状态属性（如 {@code FACING}）计算旋转角度，
     * 而无需修改客户端渲染代码。
     * </p>
     *
     * @param state 当前方块状态
     * @return 绕 Y 轴的旋转角度（弧度），默认返回 0
     */
    default float getRotationAngle(BlockState state) {
        if (state == null || !state.contains(ROTATION)) return 0;
        int rotation = calculateRotation(state.get(ROTATION));
        return (float) (rotation * 22.5 * Math.PI / 180.0);
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
    default VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }
}