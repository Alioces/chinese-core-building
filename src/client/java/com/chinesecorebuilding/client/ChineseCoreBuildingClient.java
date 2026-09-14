package com.chinesecorebuilding.client;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.TestSignBlock;
import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.block.properties.Layered;
import com.chinesecorebuilding.block.properties.RenderLayerType;
import com.chinesecorebuilding.client.model.ModelPluginRegistry;
import com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel;
import com.chinesecorebuilding.client.renderer.SignBlockRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 客户端初始化入口，实现 Fabric 的 ClientModInitializer。
 * <p>
 * 在 {@code client} 源集自动加载，只在客户端执行。
 * 完成三类注册：
 * <ol>
 *   <li>渲染层 — 遍历所有 {@link Layered} 方块，按其 {@link RenderLayerType} 绑定到对应 RenderLayer</li>
 *   <li>模型后处理插件 — 向 {@link ModelPluginRegistry} 注册 Rotatable → RotationBakedModel、Offset → OffsetBakedModel</li>
 *   <li>BlockEntityRenderer — 为 TestSignBlock 注册 {@link SignBlockRenderer}（叠加文字层）</li>
 * </ol>
 * </p>
 * <p>
 * 临时测试逻辑：{@link #registerTestDataInjector()} 会在每 tick 扫描玩家附近的 TestSignBlock，
 * 对没有文字的方块自动注入两行测试文字（"禁止通行" / "STOP"）。
 * 这是阶段一验收用的硬编码，正式交付时需要移除。
 * </p>
 */
public class ChineseCoreBuildingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerRenderLayers();

        ModelPluginRegistry.register(block -> block instanceof com.chinesecorebuilding.block.properties.Rotatable, RotationBakedModel::new);
        ModelPluginRegistry.register(block -> block instanceof com.chinesecorebuilding.block.properties.Offset, OffsetBakedModel::new);
        ModelPluginRegistry.registerAll();

        BlockEntityRendererFactories.register(
                TestSignBlock.ENTITY_TYPE,
                SignBlockRenderer::new
        );

        registerTestDataInjector();
    }

    /**
     * 为所有实现了 {@link Layered} 接口的方块注册渲染层。
     * <p>
     * 遍历整个方块注册表，根据方块自身声明的 {@link RenderLayerType} 映射：
     * <pre>
     * CUTOUT      → RenderLayer.getCutout()
     * TRANSLUCENT → RenderLayer.getTranslucent()
     * 其他        → RenderLayer.getSolid()
     * </pre>
     * 好处是 Block 本身只管声明渲染层类型，不需要客户端单独配置。
     * </p>
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

    /**
     * 注册测试数据注入器。
     * <p>
     * 每 tick 扫描玩家周围 ±3 格 × 上下 ±1 格 内的 TestSignBlock，
     * 对没有文字行的 BlockEntity 自动追加两行示例文本。
     * 仅在玩家和世界都存在时执行。
     * </p>
     * <p>
     * <b>⚠️ 临时代码，阶段一验收完成后必须删除。</b>
     * 正式环境下文字由交互 UI 或命令设置，不应该在 tick 里硬塞。
     * </p>
     */
    private void registerTestDataInjector() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            injectTestData(client.world, client.player.getBlockPos());
        });
    }

    /**
     * 扫描范围内所有 TestSignBlock 并注入测试文字。
     * <p>
     * 扫描范围以玩家脚下为中心：X/Z ±3 格、Y -1~+1 格。
     * 只处理 {@link SignBlockEntity#getLineCount()} 为 0 的方块，
     * 已注入过的不会被重复覆盖。
     * </p>
     * <p>
     * 注入内容：
     * <ul>
     *   <li>行 0 — 文字 {@code "禁止通行"}，位置 {@code (0.5, 0.6, 1.01)}，颜色 红色 0xFF0000，发光 true</li>
     *   <li>行 1 — 文字 {@code "STOP"}，位置 {@code (0.5, 0.4, 1.01)}，颜色 白色 0xFFFFFF，缩放 0.8×</li>
     * </ul>
     * </p>
     */
    private void injectTestData(World world, BlockPos center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (world.getBlockState(pos).getBlock() == TestSignBlock.INSTANCE) {
                        SignBlockEntity entity = world.getBlockEntity(pos) instanceof SignBlockEntity se ? se : null;
                        if (entity != null && entity.getLineCount() == 0) {
                            entity.addLine(TextLine.builder()
                                    .text(Text.literal("禁止通行"))
                                    .position(0.5f, 0.6f, 1.01f)
                                    .color(0xFF0000)
                                    .glowing(true)
                                    .build());
                            entity.addLine(TextLine.builder()
                                    .text(Text.literal("STOP"))
                                    .position(0.5f, 0.4f, 1.01f)
                                    .color(0xFFFFFF)
                                    .scale(0.8f)
                                    .build());
                            ChineseCoreBuildingMod.LOGGER.info("[TEST] 测试数据已注入到 {}", pos);
                        }
                    }
                }
            }
        }
    }
}