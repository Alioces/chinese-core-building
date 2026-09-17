package com.chinesecorebuilding.client.model;

import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.BakeContext;
import net.minecraft.block.Block;

/**
 * 模型烘焙插件接口。
 * <p>
 * 所有模型烘焙阶段的扩展都实现此接口，
 * 通过责任链模式依次处理，每个插件只关注自己的职责。
 * 插件按优先级排序执行，前一个插件的输出作为后一个插件的输入。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * public class SubModelComposerPlugin implements ModelBakePlugin {
 *     &#64;Override
 *     public int getPriority() { return -1000; }
 *
 *     &#64;Override
 *     public boolean accepts(Block block) {
 *         return block instanceof SubModelProvider;
 *     }
 *
 *     &#64;Override
 *     public BakedModelSpec bake(BakeContext context) {
 *         // 处理子模型组合逻辑
 *         return context.spec();
 *     }
 * }
 * </pre>
 *
 * @see BakeContext
 * @see BakedModelSpec
 */
public interface ModelBakePlugin {

    /**
     * 获取插件优先级。
     * <p>
     * 数值越小优先级越高，越先执行。
     * 建议范围：
     * <ul>
     *   <li>-1000 ~ -500：基础模型处理（子模型、贴图）</li>
     *   <li>-499 ~ 0：动画和动态效果</li>
     *   <li>1 ~ 500：多方块协调</li>
     *   <li>501 ~ 1000：最终修饰</li>
     * </ul>
     * </p>
     *
     * @return 优先级数值
     */
    int getPriority();

    /**
     * 判断是否处理当前方块。
     * <p>
     * 此方法在烘焙链执行时调用，用于判断当前插件
     * 是否需要处理指定的方块。返回 true 表示执行
     * {@link #bake(BakeContext)} 方法。
     * </p>
     *
     * @param block 方块实例
     * @return true 表示此插件需要处理该方块
     */
    boolean accepts(Block block);

    /**
     * 烘焙阶段处理。
     * <p>
     * 在模型烘焙时调用一次，用于生成静态顶点或注册动态更新器。
     * 接收烘焙上下文和当前模型规格，可以修改规格后返回。
     * </p>
     *
     * @param context 烘焙上下文（包含方块实例、BlockState、当前模型规格）
     * @return 处理后的模型规格
     */
    BakedModelSpec bake(BakeContext context);
}