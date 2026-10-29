package com.chinesecorebuilding.client.gui;

import com.chinesecorebuilding.block.entity.TextLine;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * 简化版单行文字编辑 GUI（无 XYZ 位置字段）。
 * <p>
 * 继承 {@link BaseSingleLineTextScreen}，隐藏位置参数控件（XYZ），
 * 仅保留文字内容、颜色、缩放、发光开关。
 * </p>
 * <p>
 * 适用于不需要自定义文字位置的场景，用户只需编辑文字内容和外观属性。
 * 文字位置使用 {@link #getDefaultPosition()} 提供的预设值，不可修改。
 * </p>
 */
public class SimpleTextScreen extends BaseSingleLineTextScreen {

    /**
     * 编辑器内部标题。
     */
    private static final String EDITOR_TITLE = "文字编辑器";

    /**
     * 默认窗口标题。
     */
    private static final Text DEFAULT_TITLE = Text.literal("文字编辑器");

    /**
     * 默认文字位置（方块预设位置）。
     * <p>
     * 简化版 GUI 不允许修改位置，使用此默认值。
     * 子类可通过覆盖 {@link #getDefaultPosition()} 自定义。
     * </p>
     */
    private static final Vec3d DEFAULT_POSITION = new Vec3d(DEFAULT_POS_X, DEFAULT_POS_Y, DEFAULT_POS_Z);

    /**
     * 构造函数。
     *
     * @param blockPos 目标方块位置
     */
    public SimpleTextScreen(BlockPos blockPos) {
        super(blockPos, DEFAULT_TITLE);
    }

    /**
     * 构造函数（可指定标题）。
     *
     * @param blockPos 目标方块位置
     * @param title    窗口标题（为 null 时使用默认标题）
     */
    public SimpleTextScreen(BlockPos blockPos, Text title) {
        super(blockPos, title != null ? title : DEFAULT_TITLE);
    }

    @Override
    protected boolean showPositionFields() {
        return false;
    }

    @Override
    protected String getEditorTitle() {
        return EDITOR_TITLE;
    }

    /**
     * 获取默认文字位置。
     * <p>
     * 子类可覆盖此方法以提供自定义默认位置。
     * </p>
     *
     * @return 默认位置 Vec3d
     */
    protected Vec3d getDefaultPosition() {
        return DEFAULT_POSITION;
    }

    /**
     * 将编辑区当前内容写回 editingLines 中指定索引的 TextLine。
     * <p>
     * 简化版没有位置输入框，位置取值遵循"保留优先"策略：
     * 优先沿用该行<b>已有的位置</b>，仅当原有位置为 null 时才回退到
     * {@link #getDefaultPosition()}。
     * </p>
     * <p>
     * 这样可避免覆盖方块在 {@code createBlockEntity} 阶段播种的默认位置
     * （如路标牌自定义的 (0.5, 0.6, 0.9)），保证不同方块的专属位置不被 GUI 抹平。
     * </p>
     *
     * @param index 文字行索引
     */
    @Override
    protected void saveEditToIndex(int index) {
        if (index < 0 || index >= editingLines.size()) return;
        TextLine current = editingLines.get(index);
        Vec3d position = current.position() != null ? current.position() : getDefaultPosition();
        TextLine.Builder builder = TextLine.builder();
        builder.text(Text.literal(textField.getText()));
        builder.position(position);
        int color;
        try {
            color = Integer.parseInt(colorField.getText().trim(), 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            color = 0xFFFFFFFF;
        }
        builder.color(color);
        builder.scale(parseFloatSafe(scaleField.getText(), DEFAULT_SCALE));
        builder.glowing(current.glowing());
        editingLines.set(index, builder.build());
    }

    /**
     * 将指定行的属性同步到编辑区的输入框。
     * <p>
     * 简化版不显示位置输入框，因此跳过位置字段的同步。
     * </p>
     *
     * @param index 文字行索引
     */
    @Override
    protected void refreshEditFields(int index) {
        if (index < 0 || index >= editingLines.size()) {
            textField.setText("");
            colorField.setText("");
            scaleField.setText("");
            glowBtn.setMessage(Text.literal("发光: 关"));
            return;
        }
        TextLine line = editingLines.get(index);
        textField.setText(line.text().getString());
        colorField.setText(String.format("%06X", line.color() & 0xFFFFFF));
        scaleField.setText(String.format("%.2f", line.scale()));
        glowBtn.setMessage(Text.literal("发光: " + (line.glowing() ? "开" : "关")));
    }
}