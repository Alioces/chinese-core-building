package com.chinesecorebuilding;

import com.chinesecorebuilding.block.ChineseCoreBuildingBlocks;
import com.chinesecorebuilding.group.ChineseCoreBuildingGroups;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主入口类，实现 Fabric 的 {@link ModInitializer} 接口。
 * <p>
 * 当 Minecraft 加载模组时，Fabric Loader 会自动调用 {@link #onInitialize()} 方法，
 * 在此处完成所有服务端/通用端的注册操作（方块、物品、物品组等）。
 * </p>
 *
 * @author chinesecorebuilding
 */
public class ChineseCoreBuildingMod implements ModInitializer {

	/** 模组命名空间 ID，用于构建 {@link Identifier}，所有注册路径都基于此 ID */
	public static final String MOD_ID = "chinese-core-building";

	/** 物品组翻译键前缀，完整键名为 itemGroup.chinese-core-building.group.{suffix} */
	public static final String MOD_GROUP = "itemGroup."+ MOD_ID + ".group.";

	/**
	 * 模组专用日志记录器。
	 * <p>
	 * 使用模组 ID 作为日志器名称，便于在日志中区分不同模组的输出。
	 * </p>
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * 模组初始化入口方法。
	 * <p>
	 * 在 Minecraft 进入模组加载阶段时由 Fabric Loader 自动调用。
	 * 负责按顺序初始化所有注册模块：
	 * <ol>
	 *     <li>{@link ChineseCoreBuildingBlocks#initialize()} — 注册所有方块及其对应物品</li>
	 *     <li>{@link ChineseCoreBuildingGroups#initialize()} — 注册创造模式物品组</li>
	 * </ol>
	 * </p>
	 * <b>注意：</b>此阶段部分资源（如客户端渲染资源）可能尚未初始化，
	 * 客户端相关逻辑应放在 {@link net.fabricmc.api.ClientModInitializer} 中。
	 */
	@Override
	public void onInitialize() {
		// 注册所有方块和方块物品
		ChineseCoreBuildingBlocks.initialize();
		// 注册创造模式物品组
		ChineseCoreBuildingGroups.initialize();

		LOGGER.info("Hello Fabric world!");
	}

	/**
	 * 快速构建本模组命名空间下的 {@link Identifier}。
	 *
	 * @param path 资源路径（如 "embankment_road01"）
	 * @return 格式为 {@code chinese-core-building:path} 的 Identifier
	 */
	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
