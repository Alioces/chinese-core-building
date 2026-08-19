package com.chinesecorebuilding.client;

import com.chinesecorebuilding.block.properties.Offset;
import com.chinesecorebuilding.block.properties.Layered;
import com.chinesecorebuilding.block.properties.Rotatable;
import com.chinesecorebuilding.client.model.ModelPluginRegistry;
import com.chinesecorebuilding.client.model.postProcessing.OffsetBakedModel;
import com.chinesecorebuilding.client.model.postProcessing.RotationBakedModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.registry.Registries;

/**
 * 客户端初始化入口。
 * <p>
 * 实现 Fabric 的 {@link ClientModInitializer} 接口，在客户端启动时执行。
 * 负责设置方块渲染层和注册模型后处理逻辑。
 * </p>
 * <p>
 * 主要职责：
 * <ul>
 *     <li>设置方块的渲染层（如 cutout 透明渲染）</li>
 *     <li>通过 {@link ModelLoadingPlugin} 注册模型后处理，
 *         自动为 {@link RoadSignsBlock} 及其子类应用渲染偏移</li>
 * </ul>
 * </p>
 */
public class ChineseCoreBuildingClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		registerRenderLayers();

		ModelPluginRegistry.register(block -> block instanceof Rotatable, RotationBakedModel::new);
		ModelPluginRegistry.register(block -> block instanceof Offset, OffsetBakedModel::new);

		ModelPluginRegistry.registerAll();
	}

	/**
	 * 自动扫描所有已注册方块，为实现了 {@link Layered} 的方块设置渲染层。
	 * <p>
	 * 方块只需在 main 源集中实现 {@link Layered} 接口并声明渲染层类型，
	 * 此处自动完成映射，无需手动逐个设置。
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
}
