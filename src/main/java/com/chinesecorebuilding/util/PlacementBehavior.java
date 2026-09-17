package com.chinesecorebuilding.util;

import com.chinesecorebuilding.block.CustomBlock;
import com.chinesecorebuilding.block.properties.model.Directional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;

/**
 * 放置行为挂钩接口。
 * <p>
 * 定义方块放置时的状态后处理契约。位于 {@code util} 包——
 * 纯工具接口，不涉及方块模型渲染能力，确保 {@code model} 包职责内聚。
 * </p>
 * <p>
 * 方块实现本接口（或其含有 default 实现的子接口）即可在放置时
 * 对 {@link Block#getPlacementState} 返回的初始状态进行后处理（如追加朝向属性）。
 * </p>
 * <p>
 * <b>为什么不用 default getPlacementState？</b>
 * Java 类方法优先于接口 default 方法，{@code Block.getPlacementState()}
 * 始终优先执行。因此本接口提供一个独立命名的挂钩方法，由
 * {@link CustomBlock} 在覆盖 {@code getPlacementState} 时分派。
 * </p>
 *
 * <h3>使用方式</h3>
 * 子接口（如 {@link Directional} 和
 * {@link com.chinesecorebuilding.block.properties.model.Rotatable}）
 * 提供 default 实现，方块只需 {@code implements Directional} 即可自动获得放置行为，
 * 无需重复样板代码。
 *
 * @see CustomBlock
 * @see com.chinesecorebuilding.block.properties.model.Rotatable
 * @see Directional
 */
public interface PlacementBehavior {

    /**
     * 对父类 {@code getPlacementState} 返回的初始状态进行后处理。
     * <p>
     * 调用时机：{@link com.chinesecorebuilding.block.CustomBlock#getPlacementState}
     * 调用 {@code super.getPlacementState(ctx)} 之后、返回结果之前。
     * </p>
     *
     * @param baseState 父类的初始放置状态
     * @param ctx       物品放置上下文，包含玩家朝向等信息
     * @return 后处理后的方块状态
     */
    BlockState onPlace(BlockState baseState, ItemPlacementContext ctx);
}