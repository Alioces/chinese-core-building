package com.chinesecorebuilding.client;

import com.chinesecorebuilding.client.datagen.CustomBlockModelProvider;
import com.chinesecorebuilding.client.datagen.RoadSignsBlockDataProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * 数据生成器入口。
 * <p>
 * 按职责分层注册数据 Provider：
 * <ol>
 *     <li>{@link CustomBlockModelProvider} — 基础生成：所有 CustomBlock 的 blockstate 和物品模型</li>
 *     <li>{@link RoadSignsBlockDataProvider} — 特化配置：RoadSignsBlock 的 gui_light 和 display</li>
 * </ol>
 * 运行方式：{@code ./gradlew runDatagen}
 * </p>
 */
public class ChineseCoreBuildingDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		// 阶段一：基础生成（所有 CustomBlock 的 blockstate + 物品模型）
		pack.addProvider(CustomBlockModelProvider::new);
		// 阶段二：特化配置（RoadSignsBlock 的 gui_light + display）
		pack.addProvider(RoadSignsBlockDataProvider::new);
	}
}
