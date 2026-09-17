package com.chinesecorebuilding.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

/**
 * 烘焙上下文。
 * <p>
 * 封装模型烘焙过程中的所有上下文信息，包括方块实例、
 * BlockState 和当前模型规格。在插件链中传递，
 * 每个插件可以读取上下文信息并修改模型规格。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * BakeContext context = new BakeContext(block, state, spec);
 * Block block = context.block();
 * BlockState state = context.state();
 * BakedModelSpec spec = context.spec();
 * </pre>
 *
 * @param block 方块实例
 * @param state 当前方块状态
 * @param spec  当前模型规格（可修改）
 */
public record BakeContext(
    Block block,
    BlockState state,
    BakedModelSpec spec
) {
}