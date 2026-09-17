package com.chinesecorebuilding.block;

import com.chinesecorebuilding.block.properties.Interactive;
import com.chinesecorebuilding.block.roadSigns.RoadSignsBlock;
import com.chinesecorebuilding.util.PlacementBehavior;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 自定义方块抽象基类。
 * <p>
 * 继承自 Minecraft 原版 {@link Block}，作为本模组所有自定义方块的父类。
 * 提供统一的构造入口和属性扩展机制，子类可在此基础上添加特定属性（如朝向）。
 * </p>
 * <p>
 * 自动调度服务端交互：覆盖 {@link #onUse}，若当前方块实现了 {@link Interactive}
 * 且返回了非 null 的 {@link com.chinesecorebuilding.block.properties.BlockInteraction}，
 * 则委托执行。否则走原版默认逻辑。
 * </p>
 */
public class CustomBlock extends Block {

    /**
     * 构造函数，直接透传设置到 {@link Block} 基类。
     * <p>
     * 子类（如 {@link RoadSignsBlock}、{@link com.chinesecorebuilding.block.TestSignBlock}、
     * {@link RotatableBlock}）都通过此构造函数初始化，由子类负责设置默认 blockstate 属性。
     * </p>
     *
     * @param settings 方块属性配置（材质、碰撞、硬度、透明度等）
     */
    public CustomBlock(Settings settings) {
        super(settings);
    }

    /**
     * 右键交互分发器。
     * <p>
     * 三级决策链：
     * <ol>
     *   <li>若方块实现 {@link Interactive} 且 {@code getInteraction() != null}
     *       → 完全交给 handler，handler 的 ActionResult 决定一切</li>
     *   <li>若 {@code getInteraction() == null} 但 {@code blocksDefaultInteraction() == true}
     *       → 返回 CONSUME 阻断原版默认行为（防止 BlockItem 在旁边放置新方块）</li>
     *   <li>否则 → {@code super.onUse()} 走原版逻辑（通常返回 PASS 放行放置）</li>
     * </ol>
     * </p>
     *
     * @param state  当前方块状态
     * @param world  方块所在世界
     * @param pos    方块位置
     * @param player 右键玩家
     * @param hand   使用的手
     * @param hit    命中细节
     * @return 交互结果，CONSUME / SUCCESS 阻断后续行为，PASS 放行
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (this instanceof Interactive interactive) {
            var handler = interactive.getInteraction();
            if (handler != null) {
                // 有自定义处理器 → 完全交给 handler，handler 的 ActionResult 决定一切
                return handler.interact(state, world, pos, player, hand, hit);
            }
            // 无自定义处理器但声明要阻断默认行为 → 返回 CONSUME 阻止放置/容器等
            // TestSignBlock 就是这种情况：它只需要客户端开 GUI，服务端不需要额外逻辑
            // 但必须返回 CONSUME 防止 BlockItem 在旁边放置新方块
            if (interactive.blocksDefaultInteraction()) {
                return ActionResult.CONSUME;
            }
        }
        // 非 Interactive 或显式关闭了阻断 → 走原版逻辑（通常返回 PASS 让放置继续）
        return super.onUse(state, world, pos, player, hand, hit);
    }

    /**
     * 统一分发放置行为。
     * <p>
     * 调用 {@code super.getPlacementState(ctx)} 获取原版初始状态后，
     * 遍历实现的 {@link PlacementBehavior} 接口进行后处理。
     * 每个 PlacementBehavior（如 {@code Directional}）仅需在接口中提供 default 实现，
     * 无需子类重复覆盖样板代码。
     * </p>
     * <p>
     * 与 {@link #onUse} 对 {@link Interactive} 的分派模式一致，
     * 遵循单一职责：CustomBlock 仅负责分派，具体逻辑归接口所有。
     * </p>
     *
     * @param ctx 物品放置上下文，包含玩家朝向等信息
     * @return 经过所有 PlacementBehavior 后处理后的方块状态
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (this instanceof PlacementBehavior behavior) {
            state = behavior.onPlace(state, ctx);
        }
        return state;
    }
}