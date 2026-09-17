package com.chinesecorebuilding.client.model.plugin;

import com.chinesecorebuilding.block.properties.model.SubModelProvider;
import com.chinesecorebuilding.client.model.ModelBakePlugin;
import com.chinesecorebuilding.util.AnchorPoint;
import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.handler.SubModelHandler;
import net.minecraft.block.Block;

import java.util.List;

/**
 * 子模型组合插件。
 * <p>
 * 优先级：-1000（最高优先级，最先执行）。
 * </p>
 * <p>
 * 职责：
 * <ol>
 *   <li>扫描实现了 {@link SubModelProvider} 接口的方块</li>
 *   <li>获取所有锚点定义（AnchorPoint）</li>
 *   <li>通过 SubModelHandler 获取每个锚点对应的子模型 ID</li>
 *   <li>将子模型添加到 BakedModelSpec 中</li>
 * </ol>
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 方块实现 SubModelProvider
 * public class WineCabinetBlock extends CustomBlock implements SubModelProvider {
 *     &#64;Override
 *     public List&lt;AnchorPoint&gt; getAnchors() {
 *         return List.of(AnchorPoint.BOTTLE_SLOT_1, AnchorPoint.BOTTLE_SLOT_2);
 *     }
 *
 *     &#64;Override
 *     public SubModelHandler getSubModelHandler() {
 *         return (anchor, state) -&gt; {
 *             // 根据方块状态返回子模型 ID
 *             return "minecraft:block/glass";
 *         };
 *     }
 * }
 * </pre>
 *
 * @see SubModelProvider
 * @see AnchorPoint
 * @see SubModelHandler
 */
public class SubModelComposerPlugin implements ModelBakePlugin {

    /**
     * 获取插件优先级。
     *
     * @return -1000（最高优先级）
     */
    @Override
    public int getPriority() {
        return -1000;
    }

    /**
     * 判断是否处理当前方块。
     *
     * @param block 方块实例
     * @return true 当方块实现了 SubModelProvider 接口
     */
    @Override
    public boolean accepts(Block block) {
        return block instanceof SubModelProvider;
    }

    /**
     * 烘焙阶段处理子模型组合。
     * <p>
     * 遍历所有锚点，通过 SubModelHandler 获取对应的子模型 ID，
     * 然后将子模型添加到 BakedModelSpec 中。
     * </p>
     *
     * @param context 烘焙上下文
     * @return 添加了子模型的 BakedModelSpec
     */
    @Override
    public BakedModelSpec bake(BakeContext context) {
        Block block = context.block();
        SubModelProvider provider = (SubModelProvider) block;
        SubModelHandler handler = provider.getSubModelHandler();
        List<AnchorPoint> anchors = provider.getAnchors();

        // 遍历所有锚点，添加子模型
        for (AnchorPoint anchor : anchors) {
            String modelId = handler.handle(anchor, context.state());
            if (modelId != null && !modelId.isEmpty()) {
                context.spec().addSubModel(modelId, anchor.transform());
            }
        }

        return context.spec();
    }
}