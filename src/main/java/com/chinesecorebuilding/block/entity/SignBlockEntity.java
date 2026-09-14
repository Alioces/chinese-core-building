package com.chinesecorebuilding.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可编辑路牌 BlockEntity。
 * <p>
 * 存储任意数量的 {@link TextLine}，每行拥有独立的位置、颜色、缩放等属性。
 * 继承 {@link CustomBlockEntity} 统一基类，自动拥有 markModified + 客户端同步能力。
 * </p>
 * <p>
 * 继承层次：BlockEntity → ModBlockEntity（统一基类） → SignBlockEntity。
 * </p>
 * <p>
 * 修改-标记-同步机制：本类所有修改方法都会自动调用 {@link CustomBlockEntity#markModified()}，
 * 无需手动处理 dirty 标记和客户端同步。
 * </p>
 *
 * @see TextLine
 */
public class SignBlockEntity extends CustomBlockEntity {

    /** 所有文字行，按列表顺序依次渲染 */
    private List<TextLine> lines = new ArrayList<>();

    /**
     * @param type  BlockEntityType（必须由注册表创建）
     * @param pos   方块位置
     * @param state 方块状态
     */
    public SignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 获取所有文字行的不可变视图。
     * <p>
     * 返回只读列表，修改必须通过本类的 addLine / removeLine / updateLine 等方法，
     * 以触发 markModified 同步。
     * </p>
     *
     * @return 只读文字行列表
     */
    public List<TextLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * 获取文字行总数。
     *
     * @return 文字行数量
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * 获取指定索引的文字行。
     *
     * @param index 行索引（0-based）
     * @return TextLine 或 null（索引超出范围）
     */
    public TextLine getLine(int index) {
        if (index < 0 || index >= lines.size()) return null;
        return lines.get(index);
    }

    /**
     * 追加一行文字到末尾，自动触发 markModified。
     *
     * @param line 待追加的 TextLine
     */
    public void addLine(TextLine line) {
        lines.add(line);
        markModified();
    }

    /**
     * 在指定索引处插入一行文字，自动触发 markModified。
     *
     * @param index 插入位置（0-based）
     * @param line  待插入的 TextLine
     */
    public void addLine(int index, TextLine line) {
        lines.add(index, line);
        markModified();
    }

    /**
     * 移除指定索引的文字行，自动触发 markModified。
     *
     * @param index 待移除的行索引（0-based）
     * @return true 如果索引有效并成功移除
     */
    public boolean removeLine(int index) {
        if (index < 0 || index >= lines.size()) return false;
        lines.remove(index);
        markModified();
        return true;
    }

    /**
     * 替换指定索引的文字行，自动触发 markModified。
     *
     * @param index   待替换的行索引（0-based）
     * @param newLine 替换后的 TextLine
     * @return true 如果索引有效并成功替换
     */
    public boolean updateLine(int index, TextLine newLine) {
        if (index < 0 || index >= lines.size()) return false;
        lines.set(index, newLine);
        markModified();
        return true;
    }

    /**
     * 清空所有文字行，仅当非空时才触发 markModified。
     */
    public void clearLines() {
        if (!lines.isEmpty()) {
            lines.clear();
            markModified();
        }
    }

    /**
     * 整体替换所有文字行，只触发一次 markModified，适合批量设置。
     *
     * @param newLines 新的文字行列表（会被拷贝，后续外部修改不影响内部状态）
     */
    public void setLines(List<TextLine> newLines) {
        lines = new ArrayList<>(newLines);
        markModified();
    }

    /**
     * 快捷方法：只替换指定行的文字内容，保持位置/颜色/缩放/发光状态不变。
     *
     * @param index 行索引（0-based）
     * @param text  新的文字内容
     * @return true 如果索引有效并成功更新
     */
    public boolean setLineText(int index, Text text) {
        TextLine line = getLine(index);
        if (line == null) return false;
        return updateLine(index, line.withText(text));
    }

    /**
     * 将文字行列表序列化为 NBT。
     * <p>
     * 先调用父类 writeNbt 写入方块位置等基础字段，
     * 然后追加 {@code Lines}（NbtList，每个元素为 TextLine.writeNbt() 的输出）。
     * </p>
     */
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        for (TextLine line : lines) {
            list.add(line.writeNbt());
        }
        nbt.put("Lines", list);
    }

    /**
     * 从 NBT 反序列化文字行列表。
     * <p>
     * 先调用父类 readNbt 读取方块位置等基础字段，
     * 然后从 {@code Lines}（NbtList，COMPOUND_TYPE）逐项调用 TextLine.readNbt() 恢复。
     * </p>
     */
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        lines.clear();
        if (nbt.contains("Lines", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("Lines", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                lines.add(TextLine.readNbt(list.getCompound(i)));
            }
        }
    }
}