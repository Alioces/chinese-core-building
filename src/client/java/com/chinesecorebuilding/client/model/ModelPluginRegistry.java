package com.chinesecorebuilding.client.model;

import com.chinesecorebuilding.block.properties.model.ModelBakeDecorator;
import com.chinesecorebuilding.client.model.postProcessing.SubModelBakedModel;
import com.chinesecorebuilding.util.BakedModelSpec;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 模型后处理插件注册中心——分两阶段架构。
 * <p>
 * 两个阶段的职责边界严格分离：
 * <ol>
 *   <li><b>装饰阶段 ({@link ModelBakeDecorator})</b>：往模型顶点流里追加新的几何数据
 *       （如文字层），执行顺序 {@link #DECORATOR} → 先</li>
 *   <li><b>变换阶段 ({@link ModelWorldTransformer})</b>：对所有已有顶点做世界坐标变换
 *       （旋转、偏移），执行顺序 {@link #TRANSFORMER} → 后</li>
 * </ol>
 * <p>
 * 变换阶段的插件链套在装饰阶段的外面——Transform(Decorator(Original))。
 * 因此 Decorator 追加的顶点会被 Transform 统一处理，文字自动跟随旋转偏移，
 * 不需要在 BlockEntityRenderer 里手动重复变换逻辑。
 * </p>
 * <p>
 * <b>性能保证：</b>
 * BakedModel 是全局共享缓存的，bake 时执行一次。emitBlockQuads 运行时的变换
 * 只是在 RenderContext 上做 pushTransform → 矩阵乘法，零额外 Draw call。
 * 装饰层（文字）的顶点与模型本体走同一个 RenderContext，同样零额外 Draw call。
 * </p>
 *
 * @see ModelBakeDecorator
 * @see ModelWorldTransformer
 */
public final class ModelPluginRegistry {

    /**
     * SLF4J 日志记录器，用于输出子模型调试信息。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("SubModel");

    /**
     * 插件条目。
     * <p>
     * 包含方块匹配谓词和模型包装函数。
     * 当模型烘焙完成时，{@link #matcher} 用于判断是否应用 {@link #wrapper}。
     * </p>
     *
     * @param matcher 方块匹配谓词
     * @param wrapper 模型包装函数
     */
    private record Entry(Predicate<Block> matcher, Function<BakedModel, BakedModel> wrapper) {}

    /**
     * 阶段标记枚举。
     * <p>
     * 定义模型后处理的两个阶段：装饰阶段（先执行）和变换阶段（后执行）。
     * </p>
     */
    public enum Phase {
        /**
         * 模型烘焙修饰阶段。
         * <p>
         * 往顶点流里追加新的几何数据（如文字层）。
         * 此阶段不改变已有顶点的位置。
         * </p>
         */
        DECORATOR,
        /**
         * 世界坐标最终处理阶段。
         * <p>
         * 对所有顶点做矩阵变换（旋转、偏移）。
         * 变换作用在装饰阶段的结果之上，使装饰层顶点自动跟随变换。
         * </p>
         */
        TRANSFORMER
    }

    /**
     * 装饰阶段插件列表。
     * <p>
     * 存储所有注册到 {@link Phase#DECORATOR} 阶段的插件条目。
     * 在 {@link #registerAll()} 中优先注册，确保装饰先于变换执行。
     * </p>
     */
    private static final List<Entry> DECORATORS = new ArrayList<>();

    /**
     * 变换阶段插件列表。
     * <p>
     * 存储所有注册到 {@link Phase#TRANSFORMER} 阶段的插件条目。
     * 在 {@link #registerAll()} 中后于装饰阶段注册，确保变换作用在装饰结果之上。
     * </p>
     */
    private static final List<Entry> TRANSFORMERS = new ArrayList<>();

    /**
     * 注册一个模型插件，指定所属阶段。
     *
     * @param phase   所属阶段
     * @param matcher 方块匹配谓词（如 {@code block instanceof SignTextProvider}）
     * @param wrapper 模型包装函数（如 {@code TextBakedModel::new}）
     */
    public static void register(Phase phase, Predicate<Block> matcher, Function<BakedModel, BakedModel> wrapper) {
        // 按阶段分类存储，registerAll 时再按顺序统一注册到 Fabric
        switch (phase) {
            case DECORATOR  -> DECORATORS.add(new Entry(matcher, wrapper));
            case TRANSFORMER -> TRANSFORMERS.add(new Entry(matcher, wrapper));
        }
    }

    /**
     * 将所有已注册的插件注册到 Fabric 模型加载管线。
     * <p>
     * 先注册所有装饰阶段，再注册所有变换阶段，
     * 保证变换作用在装饰的结果之上。
     * </p>
     */
    public static void registerAll() {
        // 先装装饰阶段：TextBakedModel 等，负责往模型里加东西
        for (Entry entry : DECORATORS) {
            registerPlugin(entry);
        }
        // 后装变换阶段：RotationBakedModel / OffsetBakedModel，统一对所有顶点做矩阵变换
        for (Entry entry : TRANSFORMERS) {
            registerPlugin(entry);
        }
    }

    /**
     * 注册模型烘焙插件链桥接（ModelBakePluginRegistry → Fabric 管线）。
     * <p>
     * 解决两套注册体系独立运行导致的断路问题：
     * {@link ModelBakePluginRegistry#bakeChain} 生成 {@link BakedModelSpec}
     * （含子模型、贴图覆盖、发光等级等），但从未连接到 Fabric 渲染管线。
     * </p>
     * <p>
     * 桥接逻辑：
     * <ol>
     *   <li>检测方块是否实现 {@link ModelBakeDecorator}</li>
     *   <li>调用 bakeChain 执行所有已注册插件，生成最终 BakedModelSpec</li>
     *   <li>若 BakedModelSpec 包含子模型，用 {@link SubModelBakedModel} 包装</li>
     * </ol>
     * </p>
     * <p>
     * <b>注册顺序：</b>此桥接必须早于 {@link #registerAll()} 注册，
     * 确保 SubModelBakedModel 在 DECORATOR 插件链最内侧，
     * 使子模型顶点先于旋转/偏移变换处理。
     * </p>
     *
     * @see ModelBakePluginRegistry
     * @see SubModelBakedModel
     * @see BakedModelSpec
     */
    public static void registerBakeChainBridge() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelAfterBake().register((original, context) -> {
                // 从模型 ID 反查方块实例
                Block block = resolveBlock(context.id());
                if (block == null) {
                    return original;
                }

                // 只有实现了 ModelBakeDecorator 的方块才走烘焙链
                if (!(block instanceof ModelBakeDecorator)) {
                    return original;
                }

                LOGGER.info("检测到 ModelBakeDecorator 方块: {} (modelId={})",
                    block.getClass().getSimpleName(), context.id());

                // 执行烘焙链：遍历所有 ModelBakePlugin，
                // 生成包含子模型、贴图覆盖、发光等级等的 BakedModelSpec
                BakedModelSpec spec = ModelBakePluginRegistry.bakeChain(
                    block,
                    block.getDefaultState(),
                    new BakedModelSpec(context.id())
                );

                LOGGER.info("bakeChain 完成, 子模型数量: {}", spec.getSubModels().size());

                BakedModel result = original;

                // 若有子模型，用 SubModelBakedModel 包装
                if (!spec.getSubModels().isEmpty()) {
                    result = new SubModelBakedModel(result, spec.getSubModels());
                    LOGGER.info("创建 SubModelBakedModel 包装器 ✅");
                } else {
                    LOGGER.info("无子模型，返回原始模型");
                }

                // TODO: 后续扩展 — 发光包装器 (EmissiveBakedModel)
                // TODO: 后续扩展 — 动画包装器 (AnimatedBakedModel)
                // TODO: 后续扩展 — 贴图覆盖包装器 (DynamicTextureBakedModel)

                return result;
            });
        });
    }

    /**
     * 将单个插件包装为 Fabric 能识别的 ModelLoadingPlugin。
     * <p>
     * 核心流程：监听模型烘焙后的回调 → 从模型 ID 反查方块 → matcher 匹配则 apply wrapper。
     * 这是 Fabric BakedModel 包装器模式的标准写法。
     * </p>
     */
    private static void registerPlugin(Entry entry) {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelAfterBake().register((original, context) -> {
                // modelId 格式 namespace:block/block_name → 反查对应 Block 实例
                Block block = resolveBlock(context.id());
                // 匹配成功则用 wrapper 包装原始 BakedModel，否则原样返回
                if (block != null && entry.matcher().test(block)) {
                    return entry.wrapper().apply(original);
                }
                return original;
            });
        });
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
    static Block resolveBlock(Identifier modelId) {
        // 非方块模型（如 item/xxx）直接跳过，方块插件不处理
        if (!modelId.getPath().startsWith("block/")) return null;
        // 去掉 "block/" 前缀，得到方块名
        String blockPath = modelId.getPath().substring("block/".length());
        // 用原 namespace + blockPath 拼出方块注册 ID
        Identifier blockId = new Identifier(modelId.getNamespace(), blockPath);
        return Registries.BLOCK.get(blockId);
    }

    /** 工具类不允许实例化 */
    private ModelPluginRegistry() {}
}