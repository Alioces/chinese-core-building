package com.chinesecorebuilding.client.model.plugin;

import com.chinesecorebuilding.block.properties.model.LightEmissive;
import com.chinesecorebuilding.client.model.ModelBakePlugin;
import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.handler.EmissiveHandler;
import net.minecraft.block.Block;

/**
 * 发光效果插件。
 * <p>
 * 优先级：-400。
 * </p>
 * <p>
 * 职责：
 * <ol>
 *   <li>扫描实现了 {@link LightEmissive} 接口的方块</li>
 *   <li>通过 EmissiveHandler 获取当前方块状态的发光等级</li>
 *   <li>将发光等级设置到 BakedModelSpec 中</li>
 * </ol>
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 方块实现 LightEmissive
 * public class GlowstoneLampBlock extends CustomBlock implements LightEmissive {
 *     &#64;Override
 *     public EmissiveHandler getEmissiveHandler() {
 *         return (state) -&gt; {
 *             // 根据方块状态返回发光等级（0-15）
 *             return state.get(Properties.LIT) ? 15 : 0;
 *         };
 *     }
 * }
 * </pre>
 *
 * @see LightEmissive
 * @see EmissiveHandler
 */
public class EmissivePlugin implements ModelBakePlugin {

    /**
     * 获取插件优先级。
     *
     * @return -400
     */
    @Override
    public int getPriority() {
        return -400;
    }

    /**
     * 判断是否处理当前方块。
     *
     * @param block 方块实例
     * @return true 当方块实现了 LightEmissive 接口
     */
    @Override
    public boolean accepts(Block block) {
        return block instanceof LightEmissive;
    }

    /**
     * 烘焙阶段设置发光等级。
     * <p>
     * 通过 EmissiveHandler 获取当前方块状态的发光等级，
     * 然后设置到 BakedModelSpec 中。
     * </p>
     *
     * @param context 烘焙上下文
     * @return 设置了发光等级的 BakedModelSpec
     */
    @Override
    public BakedModelSpec bake(BakeContext context) {
        Block block = context.block();
        LightEmissive provider = (LightEmissive) block;
        EmissiveHandler handler = provider.getEmissiveHandler();

        // 获取发光等级并设置
        int emissiveLevel = handler.handle(context.state());
        context.spec().setEmissiveLevel(emissiveLevel);

        return context.spec();
    }
}