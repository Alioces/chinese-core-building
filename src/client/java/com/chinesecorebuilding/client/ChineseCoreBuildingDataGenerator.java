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

    /**
     * Fabric 数据生成器初始化入口。
     * <p>
     * 创建数据生成 Pack 并按两阶段顺序注册 Provider。
     * 阶段一的基础生成先于阶段二的特化配置，避免后者被覆盖。
     * </p>
     *
     * @param fabricDataGenerator Fabric 数据生成器实例
     */
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        // 阶段一：基础生成（所有 CustomBlock 的 blockstate + 物品模型）
        pack.addProvider(CustomBlockModelProvider::new);
        // 阶段二：特化配置（RoadSignsBlock 的 gui_light + display）
        pack.addProvider(RoadSignsBlockDataProvider::new);
    }
}