package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.util.handler.EmissiveHandler;

/**
 * 发光体接口。
 * <p>
 * 实现此接口的方块可以提供自定义发光逻辑，
 * 在模型烘焙阶段根据方块状态动态设置发光等级。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * public class LightBulbBlock extends CustomBlock implements LightEmissive {
 *     private final EmissiveHandler handler = (state) -> state.get(LIT) ? 15 : 0;
 *
 *     &#64;Override
 *     public EmissiveHandler getEmissiveHandler() {
 *         return handler;
 *     }
 * }
 * </pre>
 *
 * @see EmissiveHandler
 * @see ModelBakeDecorator
 */
public interface LightEmissive extends ModelBakeDecorator {

    /**
     * 获取发光处理器。
     * <p>
     * 返回的发光处理器会在模型烘焙阶段调用，
     * 用于根据方块状态计算发光等级（0~15）。
     * </p>
     *
     * @return 发光处理器实例
     */
    EmissiveHandler getEmissiveHandler();
}