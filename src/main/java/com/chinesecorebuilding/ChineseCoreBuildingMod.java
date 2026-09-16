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

    /**
     * 模组唯一标识符。
     * <p>
     * 用于注册表 ID、日志、配置文件路径等。
     * 全模组统一使用此常量作为 namespace，避免硬编码字符串。
     * </p>
     */
    public static final String MOD_ID = "chinese-core-building";

    /**
     * 创造模式物品组翻译键前缀。
     * <p>
     * 拼接具体物品组名后传入 {@link net.minecraft.text.Text#translatable}。
     * 例如：{@code itemGroup.chinese-core-building.group.road}。
     * </p>
     */
    public static final String MOD_GROUP = "itemGroup."+ MOD_ID + ".group.";

    /**
     * 全模组共享 SLF4J 日志器。
     * <p>
     * 按 {@link #MOD_ID} 命名，便于在日志中过滤本模组的输出。
     * </p>
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Fabric 模组初始化入口。
     * <p>
     * 服务端侧：注册方块、物品组、测试方块、网络 Payload 接收端。
     * 客户端侧逻辑由 {@code ChineseCoreBuildingClient.onInitializeClient()} 独立处理，
     * 保持 main → client 的单向依赖（main 源集不引用任何 client 类）。
     * </p>
     */
    @Override
    public void onInitialize() {
        ChineseCoreBuildingBlocks.initialize();
        ChineseCoreBuildingGroups.initialize();

        TestSignBlock.register();

        SignBlockTextService.registerReceiver();

        LOGGER.info("Hello Fabric world!");
    }

    /**
     * 快捷方法：用 {@link #MOD_ID} 作为 namespace 拼接路径构造 {@link Identifier}。
     * <p>
     * 全模组统一通过此方法创建注册 ID，避免到处硬编码 namespace 字符串。
     * </p>
     *
     * @param path 注册名（如 {@code "beware_of_crosswind"}）
     * @return 完整标识符（如 {@code chinese-core-building:beware_of_crosswind}）
     */
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}