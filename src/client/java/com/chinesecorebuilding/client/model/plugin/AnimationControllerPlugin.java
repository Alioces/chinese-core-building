package com.chinesecorebuilding.client.model.plugin;

import com.chinesecorebuilding.block.properties.model.AnimatableModel;
import com.chinesecorebuilding.client.model.ModelBakePlugin;
import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.handler.AnimationHandler;
import net.minecraft.block.Block;

/**
 * 动画控制插件。
 * <p>
 * 优先级：-500。
 * </p>
 * <p>
 * 职责：
 * <ol>
 *   <li>扫描实现了 {@link AnimatableModel} 接口的方块</li>
 *   <li>获取 AnimationHandler 并注册到 BakedModelSpec</li>
 *   <li>动画处理器将在渲染时根据时间动态生成变换矩阵</li>
 * </ol>
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 方块实现 AnimatableModel
 * public class WindmillBlock extends CustomBlock implements AnimatableModel {
 *     &#64;Override
 *     public AnimationHandler getAnimationHandler() {
 *         return (time, delta, state) -&gt; {
 *             // 根据时间计算旋转角度
 *             float angle = (time % 20000) / 20000.0f * 360.0f;
 *             return List.of(
 *                 new Transform(
 *                     Vec3d.ZERO,
 *                     new Vec3d(0, angle, 0),
 *                     1.0f
 *                 )
 *             );
 *         };
 *     }
 * }
 * </pre>
 *
 * @see AnimatableModel
 * @see AnimationHandler
 */
public class AnimationControllerPlugin implements ModelBakePlugin {

    /**
     * 获取插件优先级。
     *
     * @return -500
     */
    @Override
    public int getPriority() {
        return -500;
    }

    /**
     * 判断是否处理当前方块。
     *
     * @param block 方块实例
     * @return true 当方块实现了 AnimatableModel 接口
     */
    @Override
    public boolean accepts(Block block) {
        return block instanceof AnimatableModel;
    }

    /**
     * 烘焙阶段注册动画处理器。
     * <p>
     * 获取方块的 AnimationHandler 并注册到 BakedModelSpec 中，
     * 动画处理器将在渲染时根据时间动态生成变换矩阵。
     * </p>
     *
     * @param context 烘焙上下文
     * @return 注册了动画处理器的 BakedModelSpec
     */
    @Override
    public BakedModelSpec bake(BakeContext context) {
        Block block = context.block();
        AnimatableModel provider = (AnimatableModel) block;
        AnimationHandler handler = provider.getAnimationHandler();

        // 注册动画处理器到 BakedModelSpec
        context.spec().registerAnimationHandler(handler);

        return context.spec();
    }
}