package com.chinesecorebuilding.network;

import com.chinesecorebuilding.block.entity.SignBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.World;

/**
 * 路牌文字数据的后端业务服务。
 * <p>
 * 职责：
 * <ol>
 *   <li>注册 Fabric networking 全局接收器，等待客户端发过来的 {@link SignBlockTextPayload}</li>
 *   <li>收到 Payload 后解码，把文字行写入目标 {@link SignBlockEntity}</li>
 * </ol>
 * 本类是后端的"单一数据入口"——无论未来从 GUI、命令方块还是数据包
 * 修改路牌文字，都应该走本类的 {@link #saveLines} 方法。
 * </p>
 * <p>
 * 初始化入口：{@link com.chinesecorebuilding.ChineseCoreBuildingMod#onInitialize()} 中调用
 * {@link #registerReceiver()} 完成注册。
 * </p>
 *
 * @see SignBlockTextPayload
 */
public final class SignBlockTextService {

    /** 工具类不允许实例化 */
    private SignBlockTextService() {}

    /**
     * 注册服务端 networking 全局接收器。
     * <p>
     * 收到客户端发来的 Payload 后，使用 {@link SignBlockTextPayload#decode} 解码，
     * 然后调用自身 {@link #saveLines} 写入 BlockEntity。
     * </p>
     * <p>
     * 应当在 Mod 初始化阶段调用一次。
     * </p>
     */
    public static void registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(
                SignBlockTextPayload.ID,
                (server, player, handler, buf, responseSender) -> {
                    SignBlockTextPayload.Decoded data = SignBlockTextPayload.decode(buf);
                    server.execute(() -> saveLines(player.getWorld(), data.pos(), data.lines()));
                }
        );
    }

    /**
     * 核心业务方法：把文字行列表写入指定位置的 SignBlockEntity。
     * <p>
     * 仅在服务端生效。调用后 BlockEntity 自动 {@code markModified()}，
     * Fabric 同步逻辑会广播给所有客户端，触发渲染更新。
     * </p>
     *
     * @param world 服务端世界（忽略 isClient）
     * @param pos   目标方块位置
     * @param lines 要保存的文字行列表
     */
    public static void saveLines(World world, net.minecraft.util.math.BlockPos pos,
                                 java.util.List<com.chinesecorebuilding.block.entity.TextLine> lines) {
        if (world.isClient) return;
        if (world.getBlockEntity(pos) instanceof SignBlockEntity entity) {
            entity.setLines(lines);
        }
    }
}