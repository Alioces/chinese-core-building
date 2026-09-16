package com.chinesecorebuilding.client;

import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.network.SignBlockTextPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端（客户端）路牌文字数据服务代理。
 * <p>
 * 这是 Client 源集里唯一与网络层 / BlockEntity 打交道的类。
 * GUI（SignBlockScreen）只依赖本类的公开方法，不直接关心网络或持久化细节。
 * </p>
 * <p>
 * 职责：
 * <ul>
 *   <li>{@link #readLines(BlockPos)} — 从客户端世界的 BlockEntity 读取现有文字行</li>
 *   <li>{@link #saveLines(BlockPos, List)} — 把编辑好的文字行发往服务端</li>
 * </ul>
 * </p>
 *
 * @see SignBlockTextPayload
 */
public final class SignBlockClientService {

    /** 工具类不允许实例化 */
    private SignBlockClientService() {}

    /**
     * 从客户端世界读取目标路牌的当前文字行。
     * <p>
     * 仅在客户端有世界（已进入存档）时有效；否则返回空列表。
     * </p>
     *
     * @param pos 路牌方块位置
     * @return 文字行列表的副本（可安全修改，不影响 BlockEntity 原始数据）
     */
    public static List<TextLine> readLines(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return new ArrayList<>();
        if (!(mc.world.getBlockEntity(pos) instanceof SignBlockEntity entity)) return new ArrayList<>();
        return new ArrayList<>(entity.getLines());
    }

    /**
     * 将编辑好的文字行发往服务端持久化。
     * <p>
     * 使用 {@link SignBlockTextPayload} 编码后通过 {@link ClientPlayNetworking} 发送。
     * 服务端 {@link com.chinesecorebuilding.network.SignBlockTextService} 会解码并写入 BlockEntity。
     * </p>
     *
     * @param pos   目标路牌方块位置
     * @param lines 要保存的完整文字行列表
     */
    public static void saveLines(BlockPos pos, List<TextLine> lines) {
        PacketByteBuf buf = PacketByteBufs.create();
        SignBlockTextPayload.encode(buf, pos, lines);
        ClientPlayNetworking.send(SignBlockTextPayload.ID, buf);
    }
}