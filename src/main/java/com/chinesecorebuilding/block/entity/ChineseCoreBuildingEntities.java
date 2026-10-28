package com.chinesecorebuilding.block.entity;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.ChineseCoreBuildingBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 方块实体类型注册中心。
 * <p>
 * 使用延迟初始化避免类加载顺序问题。
 * 必须在方块注册完成后调用 {@link #initialize()}。
 * </p>
 */
public final class ChineseCoreBuildingEntities {

    /**
     * 文字方块实体类型。
     */
    public static BlockEntityType<TextBlockEntity> TEXT_BLOCK_ENTITY_TYPE;

    private ChineseCoreBuildingEntities() {}

    /**
     * 注册所有方块实体类型。
     * <p>
     * 必须在方块注册完成后调用（由 ChineseCoreBuildingMod.onInitialize() 调用）。
     * </p>
     */
    public static void initialize() {
        TEXT_BLOCK_ENTITY_TYPE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(ChineseCoreBuildingMod.MOD_ID, "text_block_entity"),
                FabricBlockEntityTypeBuilder.create(
                        TextBlockEntity::new,
                        ChineseCoreBuildingBlocks.test_sign,
                        ChineseCoreBuildingBlocks.simple_text_sign,
                        ChineseCoreBuildingBlocks.single_line_sign,
                        ChineseCoreBuildingBlocks.width_limit
                ).build()
        );
    }
}