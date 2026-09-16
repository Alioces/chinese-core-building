package com.chinesecorebuilding.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组 BlockEntity 统一基类。
 * <p>
 * 继承 Minecraft {@link BlockEntity}，封装通用的修改标记（markDirty）
 * 和网络同步（syncToClient）逻辑，子类只需实现具体的数据存储和读写。
 * </p>
 *
 * <h3>为什么需要基类？</h3>
 * <ul>
 *     <li>统一同步行为 — 所有子类自动拥有改后自动推送到客户端的能力</li>
 *     <li>统一 NBT 模式 — 避免每个子类重复实现 writeNbt/readNbt/toUpdatePacket 模板</li>
 *     <li>未来扩展点 — 如需加 tick 逻辑、玩家交互记录等，基类改一次所有子类受益</li>
 * </ul>
 *
 * <h3>继承层次</h3>
 * <pre>
 * BlockEntity (Minecraft)
 *   └── ModBlockEntity
 *         ├── SignBlockEntity        — 可编辑文字层
 *         └── 未来的 XXXBlockEntity  — 其他功能方块实体
 * </pre>
 *
 * <h3>修改-标记-同步机制</h3>
 * <p>
 * 子类任何修改数据的操作，只需调用 {@link #markModified()}，
 * 基类会自动处理 markDirty + 客户端同步。
 * </p>
 */
public abstract class CustomBlockEntity extends BlockEntity {

    /**
     * 构造函数，直接透传到 {@link BlockEntity} 基类。
     * <p>
     * 子类调用此构造函数即可获得统一的 markModified + 自动同步能力。
     * </p>
     *
     * @param type  BlockEntityType（必须由注册表创建）
     * @param pos   方块位置
     * @param state 方块状态
     */
    public CustomBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 标记数据已修改并同步到客户端。
     * <p>
     * 子类修改任何持久化数据后应调用此方法。
     * </p>
     */
    protected void markModified() {
        markDirty();
        syncToClient();
    }

    /**
     * 向客户端推送当前状态更新。
     * <p>
     * 仅在服务端有效：通过 {@code world.updateListeners} 触发
     * Fabric/原版的 BlockEntityUpdateS2CPacket 发送。
     * </p>
     */
    private void syncToClient() {
        if (world != null && !world.isClient) {
            BlockState state = getCachedState();
            if (state != null) {
                world.updateListeners(pos, state, state, 3);
            }
        }
    }

    /**
     * 构造初始区块数据 NBT。
     * <p>
     * Minecraft 原版在区块加载阶段调用此方法获取 BlockEntity 的初始数据，
     * 由 {@link #toUpdatePacket} 作为主数据源，本方法在此基础上追加写入 {@link #writeNbt} 的自定义字段，
     * 确保区块首次加载时就携带完整的业务数据。
     * </p>
     *
     * @return 包含基础 NBT + writeNbt 自定义字段的 NbtCompound
     */
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = super.toInitialChunkDataNbt();
        writeNbt(nbt);
        return nbt;
    }

    /**
     * 生成客户端同步用的更新数据包。
     * <p>
     * Fabric/原版的标准做法：创建 {@link BlockEntityUpdateS2CPacket}，
     * 它会调用本类的 {@link #writeNbt} 写入自定义字段，
     * 客户端收到后自动调用 {@link #readNbt} 恢复状态。
     * </p>
     *
     * @return 更新数据包，或 null（不可发送时）
     */
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}