package com.chinesecorebuilding.util.handler;

import net.minecraft.block.BlockState;

/**
 * 发光处理器。
 * <p>
 * 函数式接口，用于向烘焙插件链提交自定义发光逻辑。
 * 实现此接口的 Lambda 或方法引用会在模型烘焙阶段调用，
 * 用于根据方块状态动态设置发光等级（0~15）。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * EmissiveHandler handler = (state) -> state.get(LIT) ? 15 : 0;
 * </pre>
 *
 * @see LightEmissive
 */
@FunctionalInterface
public interface EmissiveHandler {

    /**
     * 计算当前方块状态的发光等级。
     * <p>
     * 此方法在模型烘焙阶段调用一次，返回值用于设置模型的
     * 自发光属性。发光等级范围为 0（不发光）到 15（最大亮度）。
     * </p>
     *
     * @param state 当前方块状态，可用于根据属性（如 LIT）调整发光等级
     * @return 发光等级（0~15），0 表示不发光，15 表示最大亮度
     */
    int handle(BlockState state);
}