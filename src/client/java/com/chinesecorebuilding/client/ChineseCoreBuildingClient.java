package com.chinesecorebuilding.client;

import com.chinesecorebuilding.block.TestSignBlock;
import com.chinesecorebuilding.block.properties.Interactive;
import com.chinesecorebuilding.block.properties.model.Layered;
import com.chinesecorebuilding.block.properties.model.Offset;
import com.chinesecorebuilding.block.properties.model.Rotatable;
import com.chinesecorebuilding.block.properties.model.SignTextProvider;
import com.chinesecorebuilding.client.gui.SignBlockScreen;
import com.chinesecorebuilding.client.model.ModelPluginRegistry;
import com.chinesecorebuilding.client.model.ModelPluginRegistry.Phase;
import com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.TextBakedModel;
import com.chinesecorebuilding.client.renderer.SignBlockRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
     *   <li>BlockEntityRenderer — {@link TestSignBlock} 实体用 {@link SignBlockRenderer} 渲染文字</li>
     *   <li>交互回调 — 监听 {@link UseBlockCallback} 打开 {@link SignBlockScreen}</li>
     * </ol>
     * </p>
     */
    @Override
    public void onInitializeClient() {
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

        // 4. 统一注册到 Fabric 模型加载管线（先装饰后变换的顺序在此保证）
        ModelPluginRegistry.registerAll();

        // 5. BlockEntityRenderer：SignBlockEntity 使用 SignBlockRenderer 渲染文字
        //    文字渲染在独立的 BlockEntityRenderer 管线里，与 BakedModel 管线解耦，
        //    但 SignBlockRenderer 会手动读取 Rotatable/Offset 接口做同步变换
        BlockEntityRendererFactories.register(TestSignBlock.ENTITY_TYPE, SignBlockRenderer::new);

        // 6. 交互回调：客户端右键方块时检测 Interactive.shouldOpenGui()
        //    打开 SignBlockScreen 编辑器。不在 main 源集引用 client 类，
        //    保持 main → client 的单向依赖。
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) return ActionResult.PASS;   // 只在客户端处理
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;   // 只处理主手右键

            BlockPos pos = hit.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            // 命中 Interactive 且 shouldOpenGui() 返回 true → 打开编辑 GUI
            if (block instanceof Interactive interactive && interactive.shouldOpenGui()) {
                MinecraftClient.getInstance().setScreen(new SignBlockScreen(pos));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
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
}