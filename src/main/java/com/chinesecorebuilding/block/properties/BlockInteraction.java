package com.chinesecorebuilding.block.properties;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 方块右键交互行为函数式接口。
 * <p>
 * 封装一个完整的右键处理逻辑，由 {@link Interactive} 能力接口返回。
 * 每次玩家右键实现了 Interactive 的方块时，
 * {@code CustomBlock.onUse} 会自动调用此接口的实现。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class MySignBlock extends CustomBlock implements Interactive, BlockEntityProvider, SignTextProvider {
 *     &#64;Override
 *     public BlockInteraction getInteraction() {
 *         return (state, world, pos, player, hand, hit) -> {
 *             // 打开 GUI / 修改文字 / 其他自定义行为
 *             return ActionResult.SUCCESS;
 *         };
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see Interactive
 */
@FunctionalInterface
public interface BlockInteraction {

    /**
     * 处理玩家右键方块的交互事件。
     * <p>
     * 所有参数与 Minecraft {@code Block.onUse} 一致。
     * 若返回 {@link ActionResult#SUCCESS} 或 {@link ActionResult#CONSUME}，
     * 会取消原版默认行为（如容器打开）。
     * 返回 {@link ActionResult#PASS} 让游戏继续处理下一个处理器。
     * </p>
     *
     * @param state  当前方块状态
     * @param world  方块所在世界（服务端/客户端均可）
     * @param pos    方块坐标
     * @param player 操作玩家
     * @param hand   使用的手（主/副）
     * @param hit    命中细节（命中面、精确坐标等）
     * @return 交互结果
     */
    ActionResult interact(BlockState state, World world, BlockPos pos,
                          PlayerEntity player, Hand hand, BlockHitResult hit);
}