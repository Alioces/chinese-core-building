package com.chinesecorebuilding.util.handler;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * 多方块处理器。
 * <p>
 * 函数式接口，用于向烘焙插件链提交自定义多方块协调逻辑。
 * 实现此接口的 Lambda 或方法引用会在模型烘焙阶段调用，
 * 用于查找并合并相邻的多方块结构部分。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * MultiBlockHandler handler = (state, world, pos) -> {
 *     List&lt;BlockPos&gt; parts = new ArrayList&lt;&gt;();
 *     for (Direction dir : Direction.HORIZONTAL) {
 *         BlockPos neighbor = pos.offset(dir);
 *         if (world.getBlockState(neighbor).getBlock() instanceof MultiBlockPart) {
 *             parts.add(neighbor);
 *         }
 *     }
 *     return parts;
 * };
 * </pre>
 *
 * @see MultiBlockCoordinator
 */
@FunctionalInterface
public interface MultiBlockHandler {

    /**
     * 查找并返回连接的多方块部分位置列表。
     * <p>
     * 此方法在模型烘焙阶段调用，用于扫描相邻方块并收集
     * 属于同一多方块结构的部分位置。返回的位置列表会用于
     * 合并模型，实现无缝衔接效果。
     * </p>
     *
     * @param state 当前方块状态
     * @param world 世界实例，用于查询相邻方块状态
     * @param pos   当前方块位置
     * @return 连接的多方块部分位置列表
     */
    List<BlockPos> handle(BlockState state, World world, BlockPos pos);
}