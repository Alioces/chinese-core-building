package com.chinesecorebuilding.client;

import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.network.TextBlockPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端（客户端）文字方块数据服务代理。
 * <p>
 * 这是 Client 源集里唯一与网络层 / BlockEntity 打交道的类。
 * GUI（TextBlockScreen / SingleLineScreen）只依赖本类的公开方法，
 * 不直接关心网络或持久化细节。
 * </p>
 * <p>
 * 职责：
 * <ul>
 *   <li>{@link #readLines(BlockPos)} — 从客户端世界的 BlockEntity 读取现有文字行</li>
 *   <li>{@link #saveLines(BlockPos, List)} — 把编辑好的文字行发往服务端</li>
 * </ul>
 * </p>
 * <p>
 * 本模块是公共文字功能基础设施，任何实现 {@link SignTextProvider} 的方块
 * 都可以复用此服务进行文字数据的读取和保存。
 * </p>
 *
 * @see TextBlockPayload
 * @see com.chinesecorebuilding.block.properties.model.SignTextProvider
 */
public final class SignBlockClientService {

    /** 工具类不允许实例化 */
    private SignBlockClientService() {}

    /**
     * 从客户端世界读取目标方块的文字行。
     * <p>
     * 仅在客户端有世界（已进入存档）时有效；否则返回空列表。
     * </p>
     *
     * @param pos 文字方块位置
     * @return 文字行列表的副本（可安全修改，不影响 BlockEntity 原始数据）
     */
    public static List<TextLine> readLines(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return new ArrayList<>();
        if (!(mc.world.getBlockEntity(pos) instanceof TextBlockEntity entity)) return new ArrayList<>();
        return new ArrayList<>(entity.getLines());
    }

    /**
     * 将编辑好的文字行发往服务端持久化。
     * <p>
     * 使用 {@link TextBlockPayload} 编码后通过 {@link ClientPlayNetworking} 发送。
     * 服务端 {@link com.chinesecorebuilding.network.TextBlockService} 会解码并写入 BlockEntity。
     * </p>
     * <p>
     * <b>乐观更新</b>：发送 Payload 后立即更新本地 BlockEntity，
     * 确保 GUI 关闭后渲染器能立刻看到新文字，无需等待服务端回包或区块重载。
     * </p>
     *
     * @param pos   目标文字方块位置
     * @param lines 要保存的完整文字行列表
     */
    public static void saveLines(BlockPos pos, List<TextLine> lines) {
        PacketByteBuf buf = PacketByteBufs.create();
        TextBlockPayload.encode(buf, pos, lines);
        ClientPlayNetworking.send(TextBlockPayload.ID, buf);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && mc.world.getBlockEntity(pos) instanceof TextBlockEntity entity) {
            entity.setLines(lines);
        }
    }
}