package com.chinesecorebuilding.block.properties;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * 客户端 GUI 交互接口。
 * <p>
 * 继承 {@link Interactive}，特化客户端右键打开 GUI 的逻辑。
 * 与父接口不同，此接口允许方块自定义：
 * <ol>
 *   <li>打开 GUI 的前置条件检查（如权限、距离、物品等）</li>
 *   <li>打开后的回调逻辑（如播放音效、触发粒子等）</li>
 * </ol>
 * </p>
 * <p>
 * GUI 实例的创建由客户端 {@code UseBlockCallback} 根据方块类型自动处理。
 * 默认情况下，实现此接口的方块会打开 {@code SignBlockScreen}（文字编辑界面）。
 * 如需自定义 GUI，请在客户端代码中注册自定义处理器。
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>文字编辑方块 → 打开 SignBlockScreen</li>
 *   <li>配置面板方块 → 打开自定义设置界面</li>
 *   <li>数据展示方块 → 打开信息查看界面</li>
 * </ul>
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class MySignBlock extends CustomBlock implements GuiInteractive {
 *     &#64;Override
 *     public boolean canOpenGui(PlayerEntity player, BlockPos pos) {
 *         // 可选：添加权限检查、距离检查等
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public void onGuiOpened(PlayerEntity player, BlockPos pos) {
 *         // 可选：播放音效、触发粒子等
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see Interactive
 */
public interface GuiInteractive extends Interactive {

    /**
     * 检查是否允许打开 GUI。
     * <p>
     * 在打开 GUI 之前执行的前置条件检查。
     * 可用于实现权限控制、距离限制、物品消耗等逻辑。
     * </p>
     * <p>
     * 默认返回 true（无条件允许），子类可覆盖此方法添加检查逻辑。
     * </p>
     *
     * @param player 尝试打开 GUI 的玩家
     * @param pos 方块位置
     * @return true 允许打开 GUI，false 拒绝打开
     */
    default boolean canOpenGui(PlayerEntity player, BlockPos pos) {
        return true;
    }

    /**
     * GUI 打开后的回调逻辑。
     * <p>
     * 在 GUI 被设置后执行。
     * 可用于播放音效、触发粒子、发送统计等副作用操作。
     * </p>
     * <p>
     * 默认无操作，子类可覆盖此方法添加自定义逻辑。
     * </p>
     *
     * @param player 打开 GUI 的玩家
     * @param pos 方块位置
     */
    default void onGuiOpened(PlayerEntity player, BlockPos pos) {
        // 默认无操作
    }

    /**
     * 标记此方块是否需要客户端打开编辑 GUI。
     * <p>
     * 对于 {@link GuiInteractive} 始终返回 true。
     * </p>
     *
     * @return true（始终允许打开 GUI）
     */
    default boolean shouldOpenGui() {
        return true;
    }
}