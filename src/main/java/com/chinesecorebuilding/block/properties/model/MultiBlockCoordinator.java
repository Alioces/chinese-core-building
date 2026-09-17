package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.util.handler.MultiBlockHandler;

/**
 * 多方块协调器接口。
 * <p>
 * 实现此接口的方块可以提供自定义多方块协调逻辑，
 * 在模型烘焙阶段扫描相邻方块并合并模型，
 * 实现多方块结构无缝衔接效果。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * public class MultiBlockController extends CustomBlock implements MultiBlockCoordinator {
 *     private final MultiBlockHandler handler = (state, world, pos) -> {
 *         List&lt;BlockPos&gt; parts = new ArrayList&lt;&gt;();
 *         for (Direction dir : Direction.HORIZONTAL) {
 *             BlockPos neighbor = pos.offset(dir);
 *             if (world.getBlockState(neighbor).getBlock() instanceof MultiBlockPart) {
 *                 parts.add(neighbor);
 *             }
 *         }
 *         return parts;
 *     };
 *
 *     &#64;Override
 *     public MultiBlockHandler getMultiBlockHandler() {
 *         return handler;
 *     }
 * }
 * </pre>
 *
 * @see MultiBlockHandler
 * @see ModelBakeDecorator
 */
public interface MultiBlockCoordinator extends ModelBakeDecorator {

    /**
     * 获取多方块处理器。
     * <p>
     * 返回的多方块处理器会在模型烘焙阶段调用，
     * 用于查找并返回连接的多方块部分位置列表。
     * </p>
     *
     * @return 多方块处理器实例
     */
    MultiBlockHandler getMultiBlockHandler();
}