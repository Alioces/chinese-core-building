package com.chinesecorebuilding;

import com.chinesecorebuilding.block.ChineseCoreBuildingBlocks;
import com.chinesecorebuilding.block.TestSignBlock;
import com.chinesecorebuilding.group.ChineseCoreBuildingGroups;
import com.chinesecorebuilding.network.SignBlockTextService;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主入口。
 * <p>
 * 负责注册方块、物品组、测试方块，以及委托各子系统的 Service 完成 networking 注册。
 * 业务逻辑不写在此处，全部下沉到各自的 Service 类。
 * </p>
 */
public class ChineseCoreBuildingMod implements ModInitializer {

    public static final String MOD_ID = "chinese-core-building";
    public static final String MOD_GROUP = "itemGroup."+ MOD_ID + ".group.";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChineseCoreBuildingBlocks.initialize();
        ChineseCoreBuildingGroups.initialize();

        TestSignBlock.register();

        SignBlockTextService.registerReceiver();

        LOGGER.info("Hello Fabric world!");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}