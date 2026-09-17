package com.chinesecorebuilding.client.model.plugin;

import com.chinesecorebuilding.block.properties.model.StatefulTextureProvider;
import com.chinesecorebuilding.client.model.ModelBakePlugin;
import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.handler.TextureOverrideHandler;
import net.minecraft.block.Block;

import java.util.Map;

/**
 * 动态贴图插件。
 * <p>
 * 优先级：-900。
 * </p>
 * <p>
 * 职责：
 * <ol>
 *   <li>扫描实现了 {@link StatefulTextureProvider} 接口的方块</li>
 *   <li>通过 TextureOverrideHandler 获取贴图覆盖映射</li>
 *   <li>将贴图覆盖应用到 BakedModelSpec 中</li>
 * </ol>
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 方块实现 StatefulTextureProvider
 * public class FurnaceBlock extends CustomBlock implements StatefulTextureProvider {
 *     &#64;Override
 *     public TextureOverrideHandler getTextureOverrideHandler() {
 *         return (state) -&gt; {
 *             Map&lt;String, String&gt; overrides = new HashMap&lt;&gt;();
 *             if (state.get(Properties.LIT)) {
 *                 overrides.put("#front", "minecraft:block/furnace_front_on");
 *             } else {
 *                 overrides.put("#front", "minecraft:block/furnace_front_off");
 *             }
 *             return overrides;
 *         };
 *     }
 * }
 * </pre>
 *
 * @see StatefulTextureProvider
 * @see TextureOverrideHandler
 */
public class DynamicTexturePlugin implements ModelBakePlugin {

    /**
     * 获取插件优先级。
     *
     * @return -900
     */
    @Override
    public int getPriority() {
        return -900;
    }

    /**
     * 判断是否处理当前方块。
     *
     * @param block 方块实例
     * @return true 当方块实现了 StatefulTextureProvider 接口
     */
    @Override
    public boolean accepts(Block block) {
        return block instanceof StatefulTextureProvider;
    }

    /**
     * 烘焙阶段处理贴图覆盖。
     * <p>
     * 通过 TextureOverrideHandler 获取当前方块状态对应的贴图覆盖映射，
     * 然后应用到 BakedModelSpec 中。
     * </p>
     *
     * @param context 烘焙上下文
     * @return 应用了贴图覆盖的 BakedModelSpec
     */
    @Override
    public BakedModelSpec bake(BakeContext context) {
        Block block = context.block();
        StatefulTextureProvider provider = (StatefulTextureProvider) block;
        TextureOverrideHandler handler = provider.getTextureOverrideHandler();

        // 获取贴图覆盖映射
        Map<String, String> overrides = handler.handle(context.state());

        // 应用到 BakedModelSpec
        context.spec().applyTextureOverrides(overrides);

        return context.spec();
    }
}