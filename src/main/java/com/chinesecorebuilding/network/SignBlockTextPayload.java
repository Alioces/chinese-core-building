package com.chinesecorebuilding.network;

import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.ChineseCoreBuildingMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 → 服务端 文字数据 Payload 的编解码工具。
 * <p>
 * 本类只负责传输层职责：把 BlockPos + List(TextLine) 序列化为 ByteBuf，
 * 或从 ByteBuf 反序列化回来。不涉及任何业务逻辑（如写入 BlockEntity），
 * 业务逻辑由 {@link SignBlockTextService} 负责。
 * </p>
 * <p>
 * 数据格式：BlockPos → NbtCompound { Lines: NbtList(TextLine NBT) }
 * </p>
 *
 * @see SignBlockTextService
 */
public final class SignBlockTextPayload {

    /**
     * 网络 Payload 标识符。
     * <p>
     * 格式为 {@code chinese-core-building:sign_text}，
     * 用于客户端与服务端之间的文字数据同步通信。
     * </p>
     */
    public static final Identifier ID = ChineseCoreBuildingMod.id("sign_text");

    /** 工具类不允许实例化 */
    private SignBlockTextPayload() {}

    /**
     * 客户端侧：将目标坐标 + 文字行列表写入 ByteBuf。
     *
     * @param buf   待写入的字节缓冲区
     * @param pos   目标路牌方块位置
     * @param lines 待保存的文字行列表
     */
    public static void encode(PacketByteBuf buf, BlockPos pos, List<TextLine> lines) {
        buf.writeBlockPos(pos);
        NbtList list = new NbtList();
        for (TextLine line : lines) {
            list.add(line.writeNbt());
        }
        NbtCompound wrapper = new NbtCompound();
        wrapper.put("Lines", list);
        buf.writeNbt(wrapper);
    }

    /**
     * 服务端侧：从 ByteBuf 解码出目标坐标 + 文字行列表。
     * <p>
     * 返回的 DTO 交给 {@link SignBlockTextService} 执行业务写入。
     * </p>
     *
     * @param buf 待解码的字节缓冲区
     * @return 解码结果 record（包含 pos 和 lines）
     */
    public static Decoded decode(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        NbtCompound wrapper = buf.readNbt();
        List<TextLine> lines = new ArrayList<>();
        if (wrapper != null && wrapper.contains("Lines", NbtElement.LIST_TYPE)) {
            NbtList list = wrapper.getList("Lines", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                lines.add(TextLine.readNbt(list.getCompound(i)));
            }
        }
        return new Decoded(pos, lines);
    }

    /**
     * 解码结果 DTO，承载坐标和文字行列表。
     *
     * @param pos   目标路牌方块位置
     * @param lines 解码得到的文字行列表
     */
    public record Decoded(BlockPos pos, List<TextLine> lines) {}
}