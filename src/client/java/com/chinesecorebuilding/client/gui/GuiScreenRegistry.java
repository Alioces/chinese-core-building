package com.chinesecorebuilding.client.gui;

import com.chinesecorebuilding.block.properties.GuiInteractive;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * GUI 屏幕工厂注册表。
 * <p>
 * 管理 GUI 类型标识符到 Screen 工厂函数的映射关系。
 * 客户端根据方块 {@link GuiInteractive#getGuiType()} 的返回值查找对应工厂并创建 Screen 实例。
 * </p>
 * <p>
 * 本类是公共文字功能基础设施的一部分，为所有需要 GUI 交互的方块提供统一的
 * Screen 创建机制，遵循开闭原则——新增 GUI 类型只需注册工厂，无需修改调用方代码。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * // 客户端初始化时注册内置 GUI 类型
 * GuiScreenRegistry.register(GuiInteractive.GUI_TYPE_MULTI_LINE, TextBlockScreen::new);
 * GuiScreenRegistry.register(GuiInteractive.GUI_TYPE_SINGLE_LINE, SingleLineScreen::new);
 *
 * // 注册自定义 GUI 类型
 * GuiScreenRegistry.register("config", ConfigScreen::new);
 *
 * // 创建 Screen（调用方无需知道具体类型）
 * Screen screen = GuiScreenRegistry.createScreen(guiType, pos, title);
 * </pre>
 * </p>
 *
 * @see GuiInteractive
 * @see TextBlockScreen
 * @see SingleLineScreen
 */
public final class GuiScreenRegistry {

    /**
     * GUI 类型标识符 → Screen 工厂函数映射表。
     * <p>
     * 工厂函数签名：(BlockPos, Text) → Screen
     * </p>
     */
    private static final Map<String, BiFunction<BlockPos, Text, Screen>> REGISTRY = new HashMap<>();

    /**
     * 私有构造函数，防止实例化。
     */
    private GuiScreenRegistry() {
    }

    /**
     * 注册 GUI 类型对应的 Screen 工厂函数。
     * <p>
     * 应在客户端初始化阶段调用，所有注册完成后调用 {@link #lock()} 锁定注册表。
     * </p>
     *
     * @param guiType GUI 类型标识符（如 "multi_line"、"single_line"）
     * @param factory Screen 工厂函数，接收 (BlockPos, Text) 返回 Screen 实例
     */
    public static void register(String guiType, BiFunction<BlockPos, Text, Screen> factory) {
        REGISTRY.put(guiType, factory);
    }

    /**
     * 根据 GUI 类型标识符创建 Screen 实例。
     * <p>
     * 调用方无需知道具体 Screen 类，只需传入类型标识符、方块位置和标题。
     * 如果类型未注册，返回 null。
     * </p>
     *
     * @param guiType GUI 类型标识符
     * @param pos     目标方块位置
     * @param title   窗口标题（可为 null，Screen 应处理 null 情况）
     * @return Screen 实例，或 null（类型未注册时）
     */
    public static Screen createScreen(String guiType, BlockPos pos, Text title) {
        BiFunction<BlockPos, Text, Screen> factory = REGISTRY.get(guiType);
        if (factory == null) {
            return null;
        }
        return factory.apply(pos, title);
    }

    /**
     * 检查指定 GUI 类型是否已注册。
     *
     * @param guiType GUI 类型标识符
     * @return true 已注册，false 未注册
     */
    public static boolean isRegistered(String guiType) {
        return REGISTRY.containsKey(guiType);
    }
}