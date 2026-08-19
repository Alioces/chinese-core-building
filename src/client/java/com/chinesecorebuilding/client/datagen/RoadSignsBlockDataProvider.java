package com.chinesecorebuilding.client.datagen;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.roadSigns.RoadSignsBlock;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.block.Block;
import net.minecraft.data.DataOutput;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@link RoadSignsBlock} 特化数据生成器。
 * <p>
 * 自动扫描方块注册表中本模组命名空间下所有 {@link RoadSignsBlock} 实例，
 * 为其写入包含 {@code gui_light} 和 {@code display} 设置的特化物品模型，
 * 覆盖基础阶段（{@link CustomBlockModelProvider}）生成的简单物品模型。
 * </p>
 * <p>
 * 职责分离：
 * <ul>
 *     <li>{@link CustomBlockModelProvider} — 自动扫描所有 CustomBlock 的 blockstate 和基础物品模型</li>
 *     <li>本类 — 自动扫描 RoadSignsBlock 的 gui_light 和 display 特化配置</li>
 * </ul>
 * </p>
 *
 * @see RoadSignsBlock
 * @see CustomBlockModelProvider
 */
public class RoadSignsBlockDataProvider extends FabricModelProvider {

	private final FabricDataOutput dataOutput;

	public RoadSignsBlockDataProvider(FabricDataOutput output) {
		super(output);
		this.dataOutput = output;
	}

	/**
	 * 覆盖默认名称 "Model Definitions"，避免与 {@link CustomBlockModelProvider} 冲突。
	 */
	@Override
	public String getName() {
		return "RoadSignsBlock Specialized Models";
	}

	@Override
	public void generateBlockStateModels(BlockStateModelGenerator generator) {
		// blockstate 由 CustomBlockModelProvider 基础生成阶段处理，此处无需操作
	}

	/**
	 * 自动扫描注册表，为本模组所有 {@link RoadSignsBlock} 生成特化物品模型。
	 * <p>
	 * 遍历方块注册表，筛选本模组命名空间下所有 {@code RoadSignsBlock} 实例，
	 * 为每个方块写入包含 {@code gui_light} 和 {@code display} 设置的物品模型。
	 * </p>
	 *
	 * @param generator 物品模型生成器
	 */
	@Override
	public void generateItemModels(ItemModelGenerator generator) {
		for (Block block : Registries.BLOCK) {
			Identifier blockId = Registries.BLOCK.getId(block);
			if (!blockId.getNamespace().equals(ChineseCoreBuildingMod.MOD_ID)) {
				continue;
			}
			if (!(block instanceof RoadSignsBlock)) {
				continue;
			}
			writeSpecializedItemModel(block);
		}
	}

	// ==================== 特化配置 ====================

	/**
	 * 为方块写入特化物品模型（含 gui_light 和 display 设置）。
	 * <p>
	 * 覆盖基础阶段生成的简单 parent 模型，追加：
	 * <ul>
	 *     <li>{@code gui_light: "front"} — 关闭 GUI 底部光照着色</li>
	 *     <li>自定义 {@code display} — 第三人称、第一人称、GUI 的变换参数</li>
	 * </ul>
	 * </p>
	 *
	 * @param block 目标方块（应为 RoadSignsBlock 或其子类）
	 */
	private void writeSpecializedItemModel(Block block) {
		Identifier blockId = Registries.BLOCK.getId(block);
		Identifier blockModelId = new Identifier(blockId.getNamespace(), "block/" + blockId.getPath());

		JsonObject root = new JsonObject();
		root.addProperty("parent", blockModelId.toString());
		root.addProperty("gui_light", "front");

		JsonObject display = new JsonObject();
		display.add("thirdperson_righthand", createDisplayEntry(
			new float[]{0, 0, 0}, new float[]{0, 3, 1}, new float[]{0.55f, 0.55f, 0.55f}));
		display.add("firstperson_righthand", createDisplayEntry(
			new float[]{0, -90, 25}, new float[]{1.13f, 3.2f, 1.13f}, new float[]{0.68f, 0.68f, 0.68f}));
		display.add("gui", createDisplayEntry(
			new float[]{0, 180, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1}));

		root.add("display", display);

		Path outputPath = dataOutput.resolvePath(DataOutput.OutputType.RESOURCE_PACK)
			.resolve(blockId.getNamespace()).resolve("models").resolve("item").resolve(blockId.getPath() + ".json");

		try {
			Files.createDirectories(outputPath.getParent());
			Files.writeString(outputPath, root.toString());
		} catch (IOException e) {
			throw new RuntimeException("Failed to write specialized item model for " + blockId, e);
		}
	}

	/**
	 * 创建单个 display 变换条目。
	 *
	 * @param rotation    旋转 [x, y, z]（度）
	 * @param translation 平移 [x, y, z]
	 * @param scale       缩放 [x, y, z]
	 * @return JSON 对象
	 */
	private JsonObject createDisplayEntry(float[] rotation, float[] translation, float[] scale) {
		JsonObject entry = new JsonObject();
		entry.add("rotation", toJsonArray(rotation));
		entry.add("translation", toJsonArray(translation));
		entry.add("scale", toJsonArray(scale));
		return entry;
	}

	/**
	 * 将 float 数组转换为 JSON 数组。
	 *
	 * @param values 浮点数组
	 * @return JSON 数组
	 */
	private JsonArray toJsonArray(float[] values) {
		JsonArray array = new JsonArray();
		for (float v : values) {
			array.add(v);
		}
		return array;
	}
}
