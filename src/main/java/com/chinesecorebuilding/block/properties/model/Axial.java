package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.util.PlacementBehavior;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * 轴向对齐方块接口。
 * <p>
 * 继承 {@link Rotatable}，将任意角度旋转特化为 2 轴向对齐（X 轴 / Z 轴），
 * 即水平纵向对齐（north-south）或水平横向对齐（east-west）。
 * 使用 {@code AXIS} 属性替代父接口的 {@code ROTATION} 属性、
 * 以及 {@link Directional} 接口的 {@code FACING} 属性。
 * </p>
 * <p>
 * <b>设计动机：</b>当方块正面和反面外观完全相同时（如双面路牌、对称装饰），
 * 无需区分 north/south 或 east/west 的具体朝向，只需知道方块沿哪个轴向对齐即可。
 * 使用 {@link Directional}（4 方向）会引入冗余的 180° 旋转状态，
 * 使用本接口则仅保留 2 个有效轴向，消除无意义的朝向区别。
 * </p>
 * <p>
 * 轴向与渲染旋转映射：
 * <ul>
 *   <li>{@code AXIS = Z}（纵向，north-south 对齐）→ 旋转 0°</li>
 *   <li>{@code AXIS = X}（横向，east-west 对齐）→ 旋转 90°</li>
 * </ul>
 * </p>
 * <p>
 * 渲染旋转由客户端 {@code RotationBakedModel} 在渲染阶段根据轴向实时完成，
 * blockstate JSON 中只需定义一个变体。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class MyBlock extends CustomBlock implements Axial {
 *     public MyBlock(Settings settings) {
 *         super(settings);
 *         setDefaultState(initAxis(getDefaultState()));
 *     }
 *
 *     &#64;Override
 *     public void appendProperties(StateManager.Builder&lt;Block, BlockState&gt; builder) {
 *         super.appendProperties(builder);
 *         Axial.super.appendProperties(builder);
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see Rotatable
 * @see Directional
 * @see Properties#HORIZONTAL_AXIS
 */
public interface Axial extends Rotatable {

    /**
     * 水平轴向属性，支持 X 轴和 Z 轴两个轴向。
     * <p>
     * 使用 Minecraft 内置的 {@link Properties#HORIZONTAL_AXIS}，
     * 取值范围为 {@link Direction.Axis#X}（east-west 横向对齐）
     * 和 {@link Direction.Axis#Z}（north-south 纵向对齐）。
     * </p>
     * <p>
     * 与 {@link Directional#FACING}（4 方向）的区别：
     * 本属性仅 2 个值，适合正面/反面外观完全相同的方块，
     * 不区分具体朝向（north vs south 或 east vs west）。
     * </p>
     */
    EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    /**
     * 初始化轴向相关状态。
     * <p>
     * 接收当前默认状态，返回附加轴向属性后的新默认状态。
     * 实现类应在构造函数中配合 {@code setDefaultState} 使用：
     * <pre>
     * setDefaultState(initAxis(getDefaultState()));
     * </pre>
     * </p>
     *
     * @param currentState 当前默认方块状态（来自 {@code getDefaultState()}）
     * @return 附加轴向属性后的新默认状态，默认为 Z 轴（north-south 对齐）
     */
    default BlockState initAxis(BlockState currentState) {
        return currentState.with(AXIS, Direction.Axis.Z);
    }

    /**
     * 向状态管理器注册轴向属性。
     * <p>
     * 覆盖父接口 {@link Rotatable#appendProperties} 的行为，
     * 注册 {@link #AXIS} 而非 {@link Rotatable#ROTATION}。
     * 实现类必须在 {@code appendProperties} 中显式委托此方法：
     * <pre>
     * Axial.super.appendProperties(builder);
     * </pre>
     * </p>
     *
     * @param builder 状态管理器构建器
     */
    @Override
    default void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /**
     * 根据轴向计算旋转值。
     * <p>
     * 将 {@link Direction.Axis} 转换为 {@link Rotatable#ROTATION} 属性值（0~15），
     * 每轴向对应 90° 增量。
     * </p>
     *
     * @param axis 水平轴向（X 或 Z）
     * @return 对应的旋转值：Z 轴 → 0，X 轴 → 1（90°）
     */
    default int axisToRotation(Direction.Axis axis) {
        return switch (axis) {
            case Z -> 0;
            case X -> 1;
            default -> 0;
        };
    }

    /**
     * 根据轴向属性计算渲染旋转角度（弧度）。
     * <p>
     * 覆盖父接口 {@link Rotatable#getRotationAngle} 的默认实现，
     * 从 {@link #AXIS} 属性读取轴向，通过 {@link #axisToRotation} 转换为旋转值，
     * 再转换为弧度角（每档 90°）。
     * </p>
     * <p>
     * Z 轴（north-south 对齐）→ 0° → 0.0 rad；
     * X 轴（east-west 对齐）→ 90° → π/2 rad。
     * 由于正面和反面外观相同，不需要 180° 和 270° 的区别。
     * </p>
     *
     * @param state 当前方块状态
     * @return 绕 Y 轴的旋转角度（弧度）
     */
    @Override
    default float getRotationAngle(BlockState state) {
        if (state == null || !state.contains(AXIS)) return 0;
        int rotation = axisToRotation(state.get(AXIS));
        return (float) (rotation * 90 * Math.PI / 180.0);
    }

    /**
     * 计算放置时的轴向。
     * <p>
     * 默认取玩家水平朝向的轴向，使得方块放置后沿玩家朝向的轴线对齐。
     * 例如玩家朝 north 或 south → Z 轴；玩家朝 east 或 west → X 轴。
     * 实现类可重写此方法以提供自定义的轴向计算逻辑。
     * </p>
     *
     * @param ctx 物品放置上下文，包含玩家朝向等信息
     * @return 放置时的轴向
     */
    default Direction.Axis calculateAxis(ItemPlacementContext ctx) {
        return ctx.getHorizontalPlayerFacing().getAxis();
    }

    /**
     * 放置时根据玩家朝向自动设置 AXIS 属性。
     * <p>
     * {@link PlacementBehavior#onPlace} 的实现，
     * 由 {@link com.chinesecorebuilding.block.CustomBlock#getPlacementState} 自动分派。
     * 子类只需 {@code implements Axial} 即可获得此行为，无需覆盖任何方法。
     * </p>
     * <p>
     * 玩家水平朝向 → 轴向映射：
     * <ul>
     *   <li>NORTH / SOUTH → AXIS = Z</li>
     *   <li>EAST / WEST → AXIS = X</li>
     * </ul>
     * </p>
     *
     * @param baseState 父类的初始放置状态
     * @param ctx       物品放置上下文，包含玩家朝向等信息
     * @return 附加了 AXIS 属性的方块状态
     */
    @Override
    default BlockState onPlace(BlockState baseState, ItemPlacementContext ctx) {
        return baseState.with(AXIS, calculateAxis(ctx));
    }
}