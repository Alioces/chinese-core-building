package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.util.handler.TextureOverrideHandler;

/**
 * 状态驱动贴图提供者接口。
 * <p>
 * 实现此接口的方块可以提供自定义贴图切换逻辑，
 * 在模型烘焙阶段根据方块状态动态替换模型中的纹理变量。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * public class LightBulbBlock extends CustomBlock implements StatefulTextureProvider {
 *     private final TextureOverrideHandler handler = (state) -> state.get(LIT)
 *         ? Map.of("#all", "block/stone_bricks")
 *         : Map.of("#all", "block/stone");
 *
 *     &#64;Override
 *     public TextureOverrideHandler getTextureOverrideHandler() {
 *         return handler;
 *     }
 * }
 * </pre>
 *
 * @see TextureOverrideHandler
 * @see ModelBakeDecorator
 */
public interface StatefulTextureProvider extends ModelBakeDecorator {

    /**
     * 获取贴图覆盖处理器。
     * <p>
     * 返回的贴图覆盖处理器会在模型烘焙阶段调用，
     * 用于根据方块状态返回纹理变量到贴图路径的映射。
     * </p>
     *
     * @return 贴图覆盖处理器实例
     */
    TextureOverrideHandler getTextureOverrideHandler();
}