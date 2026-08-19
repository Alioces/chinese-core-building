package com.chinesecorebuilding.client.datagen;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.CustomBlock;
import com.chinesecorebuilding.block.properties.Directional;
import com.chinesecorebuilding.block.properties.Rotatable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.block.Block;
import net.minecraft.data.DataOutput;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.BlockStateVariant;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.MultipartBlockStateSupplier;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.data.client.When;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * {@link CustomBlock} 基础模型数据生成器。
 * <p>
 * 自动扫描方块注册表中本模组命名空间下所有 {@link CustomBlock} 实例，
 * 根据方块实现的接口自动选择 blockstate 生成策略：
 * <ul>
 *     <li>{@link Directional} — 4 个水平朝向变体（multipart）</li>
 *     <li>{@link Rotatable} — 16 个旋转值变体（multipart）</li>
 *     <li>普通 {@link CustomBlock} — 单变体（variants）</li>
 * </ul>
 * 同时自动注册基础物品模型（parent 引用方块模型）。
 * </p>
 * <p>
 * 特化配置（如 gui_light、display）由各自的特化生成器负责，
 * 参见 {@link RoadSignsBlockDataProvider}。
 * </p>
 * <p>
 * 所有 blockstate 变体均指向同一个方块模型，不使用 vanilla 旋转，
 * 因为旋转由客户端后处理插件在渲染阶段完成。
 * </p>
 *
 * @see CustomBlock
 * @see RoadSignsBlockDataProvider
 */
public class CustomBlockModelProvider extends FabricModelProvider {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final FabricDataOutput dataOutput;

	public CustomBlockModelProvider(FabricDataOutput output) {
		super(output);
		this.dataOutput = output;
	}

	// ==================== 基础生成：blockstate + 物品模型 ====================

	/**
	 * 自动扫描注册表，为本模组所有 {@link CustomBlock} 生成 blockstate 和基础物品模型。
	 * <p>
	 * 遍历方块注册表，筛选本模组命名空间下所有 {@code CustomBlock} 实例，
	 * 通过 {@link #generateCustomBlock} 统一处理，
	 * 自动检测方块类型并选择对应的 blockstate 生成策略。
	 * </p>
	 *
	 * @param generator 方块状态模型生成器
	 */
	@Override
	public void generateBlockStateModels(BlockStateModelGenerator generator) {
		for (Block block : Registries.BLOCK) {
			Identifier blockId = Registries.BLOCK.getId(block);
			if (!blockId.getNamespace().equals(ChineseCoreBuildingMod.MOD_ID)) {
				continue;
			}
			if (!(block instanceof CustomBlock)) {
				continue;
			}
			generateCustomBlock(generator, block);
		}
	}

	/**
	 * 基础物品模型生成。
	 * <p>
	 * 所有物品模型已在 {@link #generateBlockStateModels} 中通过
	 * {@link #generateCustomBlock} 注册完毕，此处无需额外操作。
	 * </p>
	 *
	 * @param generator 物品模型生成器
	 */
	@Override
	public void generateItemModels(ItemModelGenerator generator) {
		// 基础物品模型已在 generateBlockStateModels 中注册，无需额外操作
	}

	// ==================== 基础生成逻辑 ====================

	/**
	 * 为单个 {@link CustomBlock} 生成 blockstate 和基础物品模型。
	 * <p>
	 * 自动检测方块实现的接口，选择对应的 blockstate 生成策略。
	 * 物品模型统一使用 parent 引用方块模型。
	 * </p>
	 *
	 * @param generator 方块状态模型生成器
	 * @param block     CustomBlock 实例
	 */
	private void generateCustomBlock(BlockStateModelGenerator generator, Block block) {
		// 根据方块类型生成 blockstate
		if (block instanceof Directional) {
			generateDirectionalBlockstate(generator, block);
		} else if (block instanceof Rotatable) {
			generateRotatableBlockstate(generator, block);
		} else {
			generateSimpleBlockstate(generator, block);
		}

		// 注册基础物品模型：parent 到方块模型
		Identifier blockModelId = createBlockModelId(block);
		generator.registerParentedItemModel(block, blockModelId);

		// 处理方块模型纹理：补全命名空间 + 注入粒子纹理
		processBlockModelTextures(block);
	}

	// ==================== Blockstate 生成策略 ====================

	/**
	 * 为 {@link Directional} 方块生成 blockstate。
	 * <p>
	 * 使用 multipart 格式，为 4 个水平朝向各创建一个条件映射，
	 * 所有朝向均指向同一个方块模型。
	 * </p>
	 */
	private void generateDirectionalBlockstate(BlockStateModelGenerator generator, Block block) {
		Identifier modelId = createBlockModelId(block);

		generator.blockStateCollector.accept(
			MultipartBlockStateSupplier.create(block)
				.with(When.create().set(Directional.FACING, Direction.NORTH),
					BlockStateVariant.create().put(VariantSettings.MODEL, modelId))
				.with(When.create().set(Directional.FACING, Direction.EAST),
					BlockStateVariant.create().put(VariantSettings.MODEL, modelId))
				.with(When.create().set(Directional.FACING, Direction.SOUTH),
					BlockStateVariant.create().put(VariantSettings.MODEL, modelId))
				.with(When.create().set(Directional.FACING, Direction.WEST),
					BlockStateVariant.create().put(VariantSettings.MODEL, modelId))
		);
	}

	/**
	 * 为 {@link Rotatable} 方块生成 blockstate。
	 * <p>
	 * 使用 multipart 格式，为 16 个旋转值各创建一个条件映射，
	 * 所有旋转值均指向同一个方块模型。
	 * </p>
	 */
	private void generateRotatableBlockstate(BlockStateModelGenerator generator, Block block) {
		Identifier modelId = createBlockModelId(block);
		IntProperty rotation = Rotatable.ROTATION;

		MultipartBlockStateSupplier supplier = MultipartBlockStateSupplier.create(block);
		for (int i = 0; i < 16; i++) {
			supplier = supplier.with(
				When.create().set(rotation, i),
				BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
			);
		}

		generator.blockStateCollector.accept(supplier);
	}

	/**
	 * 为普通 {@link CustomBlock} 生成简单 blockstate。
	 * <p>
	 * 使用 variants 格式，单变体指向方块模型。
	 * </p>
	 */
	private void generateSimpleBlockstate(BlockStateModelGenerator generator, Block block) {
		generator.registerSimpleCubeAll(block);
	}

	// ==================== 工具方法 ====================

	/**
	 * 根据方块创建对应的方块模型 Identifier。
	 * <p>
	 * 遵循 Minecraft 模型命名约定：{@code namespace:block/block_name}。
	 * </p>
	 */
	private Identifier createBlockModelId(Block block) {
		Identifier blockId = Registries.BLOCK.getId(block);
		return new Identifier(blockId.getNamespace(), "block/" + blockId.getPath());
	}

	// ==================== 方块模型纹理处理 ====================

	/**
	 * 处理方块模型纹理：补全命名空间前缀 + 自动注入 {@code particle} 纹理。
	 * <p>
	 * 从 {@code src/main/resources} 读取 Blockbench 导出的原始方块模型，
	 * 依次执行：
	 * <ol>
	 *     <li>为所有缺少命名空间的纹理路径补全 {@code namespace:} 前缀</li>
	 *     <li>若 {@code textures} 中缺少 {@code particle} 条目，
	 *         自动从已有纹理中取第一个作为粒子纹理</li>
	 * </ol>
	 * 修改后的 JSON 写回源资源文件（覆盖原文件）。
	 * </p>
	 * <p>
	 * 写回源资源目录（而非 {@code src/main/generated}），
	 * 因为 datagen 缓存清理会删除 generated 目录中未被跟踪的文件，
	 * 而源资源目录不受缓存管理。
	 * </p>
	 *
	 * @param block 目标方块
	 */
	private void processBlockModelTextures(Block block) {
		Identifier blockId = Registries.BLOCK.getId(block);
		String namespace = blockId.getNamespace();

		// 从 generated 输出路径反推项目根目录
		// resolvePath(RESOURCE_PACK) 返回 <root>/src/main/generated/assets
		Path generatedRoot = dataOutput.resolvePath(DataOutput.OutputType.RESOURCE_PACK);
		Path projectRoot = generatedRoot.getParent().getParent().getParent().getParent();

		// 源资源中的方块模型 JSON 路径
		Path sourceModelPath = projectRoot
			.resolve("src").resolve("main").resolve("resources").resolve("assets")
			.resolve(namespace)
			.resolve("models")
			.resolve("block")
			.resolve(blockId.getPath() + ".json");

		if (!Files.exists(sourceModelPath)) {
			return;
		}

		try {
			String content = Files.readString(sourceModelPath);
			JsonObject modelJson = GSON.fromJson(content, JsonObject.class);

			JsonObject textures = modelJson.has("textures")
				? modelJson.getAsJsonObject("textures")
				: new JsonObject();

			boolean modified = false;

			// 1. 补全纹理路径为完整的 "namespace:block/xxx" 格式
			//    Blockbench 导出的纹理路径可能缺少：
			//    - 命名空间前缀（如 "block/xxx" 或 "xxx"）
			//    - block/ 目录前缀（如 "xxx"）
			for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
				String value = entry.getValue().getAsString();
				// 跳过纹理引用（以 # 开头的是对其他纹理条目的引用）
				if (value.startsWith("#")) continue;

				String fixed = value;
				// 补全 block/ 目录前缀（无 / 说明缺少子目录）
				if (!fixed.contains("/")) {
					fixed = "block/" + fixed;
				}
				// 补全命名空间前缀（无 : 说明缺少命名空间）
				if (!fixed.contains(":")) {
					fixed = namespace + ":" + fixed;
				}

				if (!fixed.equals(value)) {
					textures.addProperty(entry.getKey(), fixed);
					modified = true;
				}
			}

			// 2. 自动注入粒子纹理
			if (!textures.has("particle")) {
				for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
					textures.addProperty("particle", entry.getValue().getAsString());
					modified = true;
					break;
				}
			}

			if (modified) {
				modelJson.add("textures", textures);
				// 写回源资源目录（不受 datagen 缓存清理影响）
				Files.writeString(sourceModelPath, GSON.toJson(modelJson));
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to process block model textures for " + blockId, e);
		}
	}
}
