package com.chinesecorebuilding.block.properties;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * 客户端 GUI 交互接口。
 * <p>
 * 继承 {@link Interactive}，特化客户端右键打开 GUI 的逻辑。
 * 与父接口不同，此接口允许方块自定义：
 * <ol>
 *   <li>打开 GUI 的前置条件检查（如权限、距离、物品等）</li>
 *   <li>打开后的回调逻辑（如播放音效、触发粒子等）</li>
 *   <li>GUI 类型标识（用于客户端查找对应的 Screen 工厂）</li>
 *   <li>GUI 窗口标题（可选，返回 null 时使用默认标题）</li>
 * </ol>
 * </p>
 * <p>
 * 本接口是公共文字功能基础设施的一部分，任何需要 GUI 编辑功能的方块都应实现此接口。
 * 实现此接口并配合 {@link com.chinesecorebuilding.block.properties.model.SignTextProvider} 使用时，
 * 方块将自动获得文字编辑 GUI。
 * </p>
 * <p>
 * GUI 实例的创建由客户端 {@code GuiScreenRegistry} 根据 {@link #getGuiType()} 返回值自动处理。
 * 默认返回 {@code "multi_line"}（多行文字编辑界面）。
 * 单行文字方块应返回 {@code "single_line"}。
 * 自定义 GUI 类型需在客户端初始化时向 {@code GuiScreenRegistry} 注册工厂。
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>文字编辑方块 → 打开文字编辑 GUI</li>
 *   <li>配置面板方块 → 打开自定义设置界面</li>
 *   <li>数据展示方块 → 打开信息查看界面</li>
 * </ul>
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class MyTextBlock extends CustomBlock implements GuiInteractive, SignTextProvider {
 *     &#64;Override
 *     public String getGuiType() {
 *         return "multi_line"; // 或 "single_line" 或自定义类型
 *     }
 *
 *     &#64;Override
 *     public Text getGuiTitle(BlockPos pos) {
 *         return Text.literal("我的自定义标题"); // 或返回 null 使用默认标题
 *     }
 *
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
 * @see com.chinesecorebuilding.block.properties.model.SignTextProvider
 * @see com.chinesecorebuilding.client.gui.GuiScreenRegistry
 */
public interface GuiInteractive extends Interactive {

    /**
     * 多行文字编辑 GUI 类型标识符。
     */
    String GUI_TYPE_MULTI_LINE = "multi_line";

    /**
     * 单行文字编辑 GUI 类型标识符。
     */
    String GUI_TYPE_SINGLE_LINE = "single_line";

    /**
     * 简化版文字编辑 GUI 类型标识符。
     * <p>
     * 隐藏位置参数控件，仅保留文字内容、颜色、缩放、发光开关。
     * 适用于不需要自定义文字位置的场景。
     * </p>
     */
    String GUI_TYPE_SIMPLE = "simple";

    /**
     * 返回此方块对应的 GUI 类型标识符。
     * <p>
     * 客户端根据此返回值查找对应的 Screen 工厂并创建实例。
     * 内置类型：
     * <ul>
     *   <li>{@link #GUI_TYPE_MULTI_LINE} — 多行文字编辑器</li>
     *   <li>{@link #GUI_TYPE_SINGLE_LINE} — 单行文字编辑器</li>
     * </ul>
     * 自定义类型需在客户端初始化时向 {@code GuiScreenRegistry} 注册。
     * </p>
     * <p>
     * 默认返回 {@link #GUI_TYPE_MULTI_LINE}。
     * </p>
     *
     * @return GUI 类型标识符
     */
    default String getGuiType() {
        return GUI_TYPE_MULTI_LINE;
    }

    /**
     * 返回此方块 GUI 的窗口标题。
     * <p>
     * 返回 null 时使用 Screen 的默认标题。
     * 可用于为不同方块实例动态生成标题（如显示方块名称、位置等）。
     * </p>
     * <p>
     * 默认返回 null（使用默认标题）。
     * </p>
     *
     * @param pos 方块位置
     * @return 窗口标题，或 null 使用默认标题
     */
    default Text getGuiTitle(BlockPos pos) {
        return null;
    }

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