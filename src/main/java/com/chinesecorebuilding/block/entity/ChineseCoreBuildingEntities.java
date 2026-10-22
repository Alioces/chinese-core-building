package com.chinesecorebuilding.block.entity;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.ChineseCoreBuildingBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ChineseCoreBuildingEntities {
    public static final BlockEntityType<TextBlockEntity> TEXT_BLOCK_ENTITY_TYPE = Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(ChineseCoreBuildingMod.MOD_ID, "text_block_entity"),
                    FabricBlockEntityTypeBuilder.create(
                            TextBlockEntity::new, // 方法引用，对应只接收 (BlockPos, BlockState) 的构造函数
                            ChineseCoreBuildingBlocks.test_sign,
                            ChineseCoreBuildingBlocks.simple_text_sign,
                            ChineseCoreBuildingBlocks.single_line_sign
                    ).build()
            );;
}
