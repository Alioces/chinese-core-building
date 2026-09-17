package com.chinesecorebuilding.util.handler;

import net.minecraft.block.BlockState;

import java.util.Map;

/**
 * 贴图覆盖处理器。
 * <p>
 * 函数式接口，用于向烘焙插件链提交自定义贴图切换逻辑。
 * 实现此接口的 Lambda 或方法引用会在模型烘焙阶段调用，
 * 用于根据方块状态返回纹理变量到贴图路径的映射。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * TextureOverrideHandler handler = (state) -> state.get(LIT)
 *     ? Map.of("#all", "block/stone_bricks")
 *     : Map.of("#all", "block/stone");
 * </pre>
 *
 * @see StatefulTextureProvider
 */
@FunctionalInterface
public interface TextureOverrideHandler {

    /**
     * 根据方块状态返回贴图覆盖映射。
     * <p>
     * 此方法在模型烘焙阶段调用，返回的 Map 键是模型中定义的
     * 纹理变量名（如 "#all"、"#front"），值是对应的贴图路径
     * （如 "block/stone_bricks"）。空 Map 表示不覆盖任何贴图。
     * </p>
     *
     * @param state 当前方块状态，可用于根据属性切换贴图
     * @return 纹理变量到贴图路径的映射，空 Map 表示不覆盖
     */
    Map<String, String> handle(BlockState state);
}