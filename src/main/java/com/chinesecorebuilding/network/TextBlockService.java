package com.chinesecorebuilding.network;

import com.chinesecorebuilding.block.entity.TextBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;

/**
 * 文字方块数据的后端业务服务。
 * <p>
 * 职责：
 * <ol>
 *   <li>注册 Fabric networking 全局接收器，等待客户端发过来的 {@link TextBlockPayload}</li>
 *   <li>收到 Payload 后解码，把文字行写入目标 {@link TextBlockEntity}</li>
 * </ol>
 * 本类是后端的"单一数据入口"——无论未来从 GUI、命令方块还是数据包
 * 修改文字方块的文字，都应该走本类的 {@link #saveLines} 方法。
 * </p>
 * <p>
 * 本模块是公共文字功能基础设施，任何实现 {@link SignTextProvider} 的方块
 * 都可以复用此服务进行文字数据的持久化。
 * </p>
 * <p>
 * 初始化入口：{@link com.chinesecorebuilding.ChineseCoreBuildingMod#onInitialize()} 中调用
 * {@link #registerReceiver()} 完成注册。
 * </p>
 *
 * @see TextBlockPayload
 * @see com.chinesecorebuilding.block.properties.model.SignTextProvider
 */
public final class TextBlockService {

    /** 工具类不允许实例化 */
    private TextBlockService() {}

    /**
     * 注册服务端 networking 全局接收器。
     * <p>
     * 收到客户端发来的 Payload 后，使用 {@link TextBlockPayload#decode} 解码，
     * 然后调用自身 {@link #saveLines} 写入 BlockEntity。
     * </p>
     * <p>
     * 应当在 Mod 初始化阶段调用一次。
     * </p>
     */
    public static void registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(
                TextBlockPayload.ID,
                (server, player, handler, buf, responseSender) -> {
                    TextBlockPayload.Decoded data = TextBlockPayload.decode(buf);
                    server.execute(() -> {
                        saveLines(player.getWorld(), data.pos(), data.lines());
                        // 立即同步 BlockEntity NBT 到编辑者客户端，
                        // 确保编辑者能收到服务端确认的完整数据
                        World world = player.getWorld();
                        if (world.getBlockEntity(data.pos()) instanceof TextBlockEntity entity) {
                            Packet<?> packet = entity.toUpdatePacket();
                            if (packet != null) {
                                player.networkHandler.sendPacket(packet);
                            }
                        }
                    });
                }
        );
    }

    /**
     * 核心业务方法：把文字行列表写入指定位置的文字方块 BlockEntity。
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
        if (world.getBlockEntity(pos) instanceof TextBlockEntity entity) {
            entity.setLines(lines);
        }
    }
}