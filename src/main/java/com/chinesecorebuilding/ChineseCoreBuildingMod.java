package com.chinesecorebuilding;

import com.chinesecorebuilding.block.ChineseCoreBuildingBlocks;
import com.chinesecorebuilding.block.TestSignBlock;
import com.chinesecorebuilding.group.ChineseCoreBuildingGroups;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChineseCoreBuildingMod implements ModInitializer {

    public static final String MOD_ID = "chinese-core-building";
    public static final String MOD_GROUP = "itemGroup."+ MOD_ID + ".group.";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChineseCoreBuildingBlocks.initialize();
        ChineseCoreBuildingGroups.initialize();

        TestSignBlock.register();

        LOGGER.info("Hello Fabric world!");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}