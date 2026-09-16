package com.chinesecorebuilding.client;

import com.chinesecorebuilding.block.TestSignBlock;
import com.chinesecorebuilding.block.properties.Interactive;
import com.chinesecorebuilding.block.properties.Layered;
import com.chinesecorebuilding.block.properties.RenderLayerType;
import com.chinesecorebuilding.client.gui.SignBlockScreen;
import com.chinesecorebuilding.client.model.ModelPluginRegistry;
import com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel;
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
 * 职责仅为注册：
 * <ol>
 *   <li>渲染层 — 遍历 Layered 方块</li>
 *   <li>模型后处理插件 — Rotatable/Offset → BakedModel</li>
 *   <li>BlockEntityRenderer — TestSignBlock → SignBlockRenderer</li>
 *   <li>UseBlockCallback — 监听 Interactive.shouldOpenGui() 打开 SignBlockScreen</li>
 * </ol>
 * 数据读写 / 网络发送全部委托给 {@link SignBlockClientService}，本类不持有任何业务逻辑。
 * </p>
 */
public class ChineseCoreBuildingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerRenderLayers();

        ModelPluginRegistry.register(block -> block instanceof com.chinesecorebuilding.block.properties.Rotatable, RotationBakedModel::new);
        ModelPluginRegistry.register(block -> block instanceof com.chinesecorebuilding.block.properties.Offset, OffsetBakedModel::new);
        ModelPluginRegistry.registerAll();

        BlockEntityRendererFactories.register(TestSignBlock.ENTITY_TYPE, SignBlockRenderer::new);

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof Interactive interactive && interactive.shouldOpenGui()) {
                MinecraftClient.getInstance().setScreen(new SignBlockScreen(pos));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }

    /**
     * 为所有 Layered 方块注册渲染层。
     */
    private void registerRenderLayers() {
        for (Block block : Registries.BLOCK) {
            if (block instanceof Layered layered) {
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