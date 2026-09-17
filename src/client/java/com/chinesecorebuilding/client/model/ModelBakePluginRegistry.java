package com.chinesecorebuilding.client.model;

import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.block.properties.model.ModelBakeDecorator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 模型烘焙插件注册中心。
 * <p>
 * 管理所有模型烘焙阶段的插件，按优先级排序后依次执行。
 * 每个插件接收 BakeContext 和 BakedModelSpec，可以修改模型规格
 * 以添加子模型、应用贴图覆盖、注册动画等。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 注册插件
 * ModelBakePluginRegistry.register(new SubModelComposerPlugin());
 * ModelBakePluginRegistry.register(new DynamicTexturePlugin());
 *
 * // 执行烘焙链
 * BakedModelSpec spec = ModelBakePluginRegistry.bakeChain(block, state, baseSpec);
 * </pre>
 *
 * @see ModelBakePlugin
 * @see BakeContext
 * @see BakedModelSpec
 */
public final class ModelBakePluginRegistry {

    /**
     * 插件列表（按优先级排序）。
     * <p>
     * 数值越小的优先级越高，越先执行。
     * 建议范围：
     * <ul>
     *   <li>-1000 ~ -500：基础模型处理（子模型、贴图）</li>
     *   <li>-499 ~ 0：动画和动态效果</li>
     *   <li>1 ~ 500：多方块协调</li>
     *   <li>501 ~ 1000：最终修饰</li>
     * </ul>
     * </p>
     */
    private static final List<ModelBakePlugin> plugins = new ArrayList<>();

    /**
     * 注册插件（自动按优先级排序）。
     * <p>
     * 插件注册后会自动插入到列表中的正确位置，
     * 保证执行顺序始终按优先级从低到高。
     * </p>
     *
     * @param plugin 插件实例
     */
    public static void register(ModelBakePlugin plugin) {
        plugins.add(plugin);
        plugins.sort(Comparator.comparingInt(ModelBakePlugin::getPriority));
    }

    /**
     * 执行所有插件，生成最终模型规格。
     * <p>
     * 按优先级顺序依次执行每个插件，前一个插件的输出
     * 作为后一个插件的输入。只有实现了 ModelBakeDecorator
     * 接口的方块才会触发插件链。
     * </p>
     *
     * @param block    方块实例
     * @param state    当前方块状态
     * @param baseSpec 基础模型规格
     * @return 处理后的最终模型规格
     */
    public static BakedModelSpec bakeChain(Block block, BlockState state, BakedModelSpec baseSpec) {
        // 如果方块没有实现 ModelBakeDecorator，直接返回基础规格
        if (!(block instanceof ModelBakeDecorator)) {
            return baseSpec;
        }

        BakedModelSpec current = baseSpec;
        BakeContext context = new BakeContext(block, state, current);

        for (ModelBakePlugin plugin : plugins) {
            if (plugin.accepts(block)) {
                current = plugin.bake(context);
                // 更新上下文中的规格
                context = new BakeContext(block, state, current);
            }
        }

        return current;
    }

    /**
     * 注册所有内置插件。
     * <p>
     * 在客户端初始化时调用，注册所有内置的模型烘焙插件。
     * 插件按优先级排序执行：
     * <ol>
     *   <li>SubModelComposerPlugin (-1000)：子模型组合</li>
     *   <li>DynamicTexturePlugin (-900)：动态贴图</li>
     *   <li>AnimationControllerPlugin (-500)：动画控制</li>
     *   <li>EmissivePlugin (-400)：发光效果</li>
     *   <li>MultiBlockCoordinatorPlugin (100)：多方块协调</li>
     * </ol>
     * </p>
     *
     * @implNote 此方法在阶段二实现具体插件后启用
     */
    public static void registerAll() {
        // TODO: 阶段二实现插件后取消注释
        // 基础模型处理（优先级最高）
        // register(new SubModelComposerPlugin());
        // register(new DynamicTexturePlugin());

        // 动画和动态效果
        // register(new AnimationControllerPlugin());
        // register(new EmissivePlugin());

        // 多方块协调
        // register(new MultiBlockCoordinatorPlugin());
    }

    /**
     * 从模型 ID 解析对应的方块实例。
     * <p>
     * Fabric 的 modifyModelAfterBake 回调里 context.id() 返回的是模型 ID，
     * 格式是 {@code namespace:block/block_name}（方块模型）或 {@code namespace:item/item_name}（物品模型）。
     * 只有方块模型才走 BakedModel 管线，物品模型直接跳过。
     * </p>
     *
     * @param modelId 模型 ID（格式 {@code namespace:block/block_name}）
     * @return 对应的方块实例，若非方块模型或不存在则返回 null
     */
    private static Block resolveBlock(Identifier modelId) {
        // 非方块模型（如 item/xxx）直接跳过，方块插件不处理
        if (!modelId.getPath().startsWith("block/")) return null;
        // 去掉 "block/" 前缀，得到方块名
        String blockPath = modelId.getPath().substring("block/".length());
        // 用原 namespace + blockPath 拼出方块注册 ID
        Identifier blockId = new Identifier(modelId.getNamespace(), blockPath);
        return Registries.BLOCK.get(blockId);
    }

    /**
     * 工具类不允许实例化。
     */
    private ModelBakePluginRegistry() {
    }
}