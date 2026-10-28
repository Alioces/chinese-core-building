package com.chinesecorebuilding.client;

import com.chinesecorebuilding.block.properties.GuiInteractive;
import com.chinesecorebuilding.block.properties.model.*;
import com.chinesecorebuilding.client.gui.GuiScreenRegistry;
import com.chinesecorebuilding.client.gui.SimpleTextScreen;
import com.chinesecorebuilding.client.gui.TextBlockScreen;
import com.chinesecorebuilding.client.gui.SingleLineScreen;
import com.chinesecorebuilding.client.model.ModelBakePluginRegistry;
import com.chinesecorebuilding.client.model.ModelPluginRegistry;
import com.chinesecorebuilding.client.model.ModelPluginRegistry.Phase;
import com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.TextBakedModel;
import com.chinesecorebuilding.client.renderer.pipeline.BlockEntityRendererRegistry;
import com.chinesecorebuilding.client.renderer.Renderers;
import com.chinesecorebuilding.client.renderer.TextBlockRenderer;
import com.chinesecorebuilding.client.renderer.pipeline.UnifiedRenderPipelineRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

/**
 * 客户端初始化入口。
 * <p>
 * 模型插件注册严格按照阶段顺序：
 * <ol>
 *   <li>装饰阶段 ({@link Phase#DECORATOR})：SignTextProvider → TextBakedModel</li>
 *   <li>变换阶段 ({@link Phase#TRANSFORMER})：Rotatable → RotationBakedModel / Offset → OffsetBakedModel</li>
 * </ol>
 * 插件链从外到内：OffsetBakedModel → RotationBakedModel → TextBakedModel → 原始模型。
 * </p>
 * <p>
 * 本类负责注册公共文字功能基础设施的客户端组件，包括渲染器、GUI 路由、模型插件等。
 * 任何实现 {@link SignTextProvider} 的方块都会自动获得文字渲染和 GUI 编辑能力。
 * </p>
 */
public class ChineseCoreBuildingClient implements ClientModInitializer {

    /**
     * Fabric 客户端初始化入口。
     * <p>
     * 执行 6 步注册流程：
     * <ol>
     *   <li>渲染层注册 — 遍历所有 {@link Layered} 方块自动映射 RenderLayer</li>
     *   <li>装饰阶段插件 — {@link SignTextProvider} → {@link TextBakedModel}</li>
     *   <li>变换阶段插件 — {@link Rotatable} → {@link RotationBakedModel} / {@link Offset} → {@link OffsetBakedModel}</li>
     *   <li>统一注册到 Fabric 模型加载管线（先装饰后变换的顺序在此保证）</li>
     *   <li>BlockEntityRenderer — 文字方块实体用 {@link TextBlockRenderer} 渲染文字</li>
     *   <li>交互回调 — 监听 {@link UseBlockCallback} 打开文字编辑 GUI</li>
     * </ol>
     * </p>
     */
    @Override
    public void onInitializeClient() {
        // 0. 初始化渲染器注册中心
        Renderers.initRenderers();
        // 0. 初始化统一渲染管线装饰器注册中心
        UnifiedRenderPipelineRegistry.initialize();

        // 1. 渲染层注册：遍历所有 Layered 方块，自动映射到对应 RenderLayer
        registerRenderLayers();

        // 2. 模型插件 — 装饰阶段（先执行，往模型里加东西）
        ModelPluginRegistry.register(Phase.DECORATOR,
                block -> block instanceof SignTextProvider, TextBakedModel::new);

        // 3. 模型插件 — 变换阶段（后执行，对所有顶点做矩阵变换）
        ModelPluginRegistry.register(Phase.TRANSFORMER,
                block -> block instanceof Rotatable, RotationBakedModel::new);
        ModelPluginRegistry.register(Phase.TRANSFORMER,
                block -> block instanceof Offset, OffsetBakedModel::new);

        // 3.5 注册模型烘焙插件链（必须在 bakeChain 调用之前！）
        //     将 5 个 ModelBakePlugin 注册到 plugins 列表中，
        //     后续 registerBakeChainBridge 调用 bakeChain() 时才能遍历到这些插件
        ModelBakePluginRegistry.registerAll();

        // 4. 模型烘焙插件链桥接 — 连接 ModelBakePluginRegistry → Fabric 管线
        //    必须在 registerAll() 之前注册！
        //    Fabric 的 modifyModelAfterBake 回调按注册顺序链式执行（先注册 = 内层），
        //    SubModelBakedModel 作为 DECORATOR（往模型里加东西），必须在变换层内侧。
        //    先注册 → 内层 → 变换层（Rotation/Offset）在最外层包裹，子模型随父模型一起旋转。
        //    包装顺序：Offset(Rotation(Text(SubModel(原模型))))
        ModelPluginRegistry.registerBakeChainBridge();

        // 5. 统一注册 DECORATOR 和 TRANSFORMER 到 Fabric 模型加载管线
        //    注册顺序：DECORATOR(Text) → TRANSFORMER(Rotation) → TRANSFORMER(Offset)
        //    后注册 = 外包装层，变换层作用在装饰层的输出之上
        ModelPluginRegistry.registerAll();

        // 6. BlockEntityRenderer：自动扫描所有 ModelBakeDecorator 方块注册渲染器
        //    方块通过 getRendererTypeId() 声明渲染器标识，渲染器通过静态块向
        //    BlockEntityRendererRegistry 提交工厂，客户端统一注册
        //    文字渲染在独立的 BlockEntityRenderer 管线里，与 BakedModel 管线解耦，
        //    TextBlockRenderer 通过完整装饰器链（Offset→Rotation→Text）保证变换一致
        registerBlockEntityRenderers();

        // 6.5 注册 GUI 屏幕工厂（遵循开闭原则：新增 GUI 类型只需在此注册，回调代码无需修改）
        GuiScreenRegistry.register(GuiInteractive.GUI_TYPE_MULTI_LINE, TextBlockScreen::new);
        GuiScreenRegistry.register(GuiInteractive.GUI_TYPE_SINGLE_LINE, SingleLineScreen::new);
        GuiScreenRegistry.register(GuiInteractive.GUI_TYPE_SIMPLE, SimpleTextScreen::new);

        // 7. 交互回调：客户端右键方块时检测 GuiInteractive
        //    检查前置条件后通过 GuiScreenRegistry 查找对应 Screen 工厂创建实例。
        //    不在 main 源集引用 client 类，保持 main → client 的单向依赖。
        UseBlockCallback.EVENT.register(ChineseCoreBuildingClient::handleGuiInteraction);
    }

    /**
     * 处理 GUI 交互逻辑。
     * <p>
     * 使用卫语句（early return）避免深层嵌套，
     * 检查前置条件后通过 GuiScreenRegistry 查找对应 Screen 工厂创建实例。
     * 类型未注册时 fallback 到多行编辑器。
     * </p>
     *
     * @param player 操作玩家
     * @param world  方块所在世界
     * @param hand   使用的手（主/副）
     * @param hit    命中结果
     * @return 交互结果
     */
    private static ActionResult handleGuiInteraction(
            PlayerEntity player,
            World world,
            Hand hand,
            BlockHitResult hit) {
        if (!world.isClient) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof GuiInteractive gui)) return ActionResult.PASS;
        if (!gui.canOpenGui(player, pos)) return ActionResult.SUCCESS;

        Screen screen = createScreenWithFallback(gui, pos);
        MinecraftClient.getInstance().setScreen(screen);
        gui.onGuiOpened(player, pos);
        return ActionResult.SUCCESS;
    }

    /**
     * 创建 Screen 实例，类型未注册时 fallback 到多行编辑器。
     *
     * @param gui  GUI 交互接口
     * @param pos  方块位置
     * @return Screen 实例
     */
    private static Screen createScreenWithFallback(GuiInteractive gui, BlockPos pos) {
        Text title = gui.getGuiTitle(pos);
        Screen screen = GuiScreenRegistry.createScreen(gui.getGuiType(), pos, title);
        return screen != null ? screen : new TextBlockScreen(pos, title);
    }

    /**
     * 为所有 Layered 方块注册渲染层。
     * <p>
     * 遍历方块注册表，找到实现了 Layered 接口的方块，
     * 根据 getRenderLayerType() 返回值映射到 Minecraft 内置的 RenderLayer。
     * 避免在每个方块类里手动调用 BlockRenderLayerMap。
     * </p>
     */
    private void registerRenderLayers() {
        for (Block block : Registries.BLOCK) {
            if (block instanceof Layered layered) {
                // RenderLayerType → Minecraft RenderLayer 的映射
                RenderLayer layer = switch (layered.getRenderLayerType()) {
                    case CUTOUT      -> RenderLayer.getCutout();
                    case TRANSLUCENT -> RenderLayer.getTranslucent();
                    default          -> RenderLayer.getSolid();
                };
                BlockRenderLayerMap.INSTANCE.putBlock(block, layer);
            }
        }
    }

    /**
     * 自动扫描所有实现 ModelBakeDecorator 的方块，注册其声明的 BlockEntityRenderer。
     * <p>
     * 渲染器类通过静态块向 BlockEntityRendererRegistry 提交工厂，
     * 方块通过 getRendererTypeId() 声明需要的渲染器标识，
     * 此方法统一读取注册表并完成 Fabric 管线注册。
     * </p>
     * <p>
     * 遵循开闭原则：新增方块只需实现 ModelBakeDecorator 并覆盖
     * getRendererTypeId()，渲染器类向 BlockEntityRendererRegistry 注册自己，
     * 客户端无需修改。
     * </p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerBlockEntityRenderers() {

        var factories = BlockEntityRendererRegistry.getFactories();

        for (Block block : Registries.BLOCK) {
            if (block instanceof ModelBakeDecorator decorator) {
                String typeId = decorator.getRendererTypeId();
                if (typeId != null && factories.containsKey(typeId)) {
                    BlockEntityRendererFactories.register(
                            (BlockEntityType) decorator.getBlockEntityType(),
                            factories.get(typeId)
                    );
                }
            }
        }
    }
}