package com.chinesecorebuilding.client.model.plugin;

import com.chinesecorebuilding.block.properties.model.MultiBlockCoordinator;
import com.chinesecorebuilding.client.model.ModelBakePlugin;
import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.handler.MultiBlockHandler;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * 多方块协调插件。
 * <p>
 * 优先级：100。
 * </p>
 * <p>
 * 职责：
 * <ol>
 *   <li>扫描实现了 {@link MultiBlockCoordinator} 接口的方块</li>
 *   <li>通过 MultiBlockHandler 获取连接的多方块部分位置列表</li>
 *   <li>根据多方块结构调整模型规格（如添加连接部件）</li>
 * </ol>
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 方块实现 MultiBlockCoordinator
 * public class LargeDoorBlock extends CustomBlock implements MultiBlockCoordinator {
 *     &#64;Override
 *     public MultiBlockHandler getMultiBlockHandler() {
 *         return (state, world, pos) -&gt; {
 *             // 返回连接的多方块部分位置
 *             // 例如：双开门的另一半
 *             Direction facing = state.get(Properties.HORIZONTAL_FACING);
 *             BlockPos otherHalf = pos.offset(facing.rotateYClockwise());
 *             return List.of(otherHalf);
 *         };
 *     }
 * }
 * </pre>
 *
 * @see MultiBlockCoordinator
 * @see MultiBlockHandler
 */
public class MultiBlockCoordinatorPlugin implements ModelBakePlugin {

    /**
     * 获取插件优先级。
     *
     * @return 100
     */
    @Override
    public int getPriority() {
        return 100;
    }

    /**
     * 判断是否处理当前方块。
     *
     * @param block 方块实例
     * @return true 当方块实现了 MultiBlockCoordinator 接口
     */
    @Override
    public boolean accepts(Block block) {
        return block instanceof MultiBlockCoordinator;
    }

    /**
     * 烘焙阶段处理多方块协调。
     * <p>
     * 通过 MultiBlockHandler 获取连接的多方块部分位置列表，
     * 然后根据多方块结构调整模型规格。
     * </p>
     * <p>
     * 注意：此插件需要访问世界信息来获取相邻方块状态，
     * 但在烘焙阶段可能无法获取完整的世界信息。
     * 因此此插件主要用于注册多方块协调逻辑，
     * 实际的协调工作可能在渲染时动态执行。
     * </p>
     *
     * @param context 烘焙上下文
     * @return 处理了多方块协调的 BakedModelSpec
     */
    @Override
    public BakedModelSpec bake(BakeContext context) {
        Block block = context.block();
        MultiBlockCoordinator coordinator = (MultiBlockCoordinator) block;
        MultiBlockHandler handler = coordinator.getMultiBlockHandler();

        // 获取连接的多方块部分位置列表
        // 注意：烘焙阶段可能无法获取完整的世界信息
        // 这里主要用于注册协调逻辑
        List<BlockPos> connectedParts = handler.handle(
            context.state(),
            null, // 世界信息在烘焙阶段可能不可用
            null  // 位置信息在烘焙阶段可能不可用
        );

        // 将多方块信息存储到 BakedModelSpec 中
        context.spec().setConnectedParts(connectedParts);

        return context.spec();
    }
}