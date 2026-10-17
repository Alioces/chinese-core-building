package com.chinesecorebuilding.client.gui;

import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.client.SignBlockClientService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化版单行文字编辑 GUI。
 * <p>
 * 继承 {@link AbstractTextScreen}，隐藏位置参数控件（XYZ），
 * 仅保留文字内容、颜色、缩放、发光开关。
 * </p>
 * <p>
 * 适用于不需要自定义文字位置的场景，用户只需编辑文字内容和外观属性。
 * 文字位置使用方块预设的默认位置，不可修改。
 * </p>
 * <p>
 * 布局：居中的紧凑编辑面板，包含文字输入框、颜色、缩放、发光开关、
 * 保存和取消按钮。比完整版更简洁，适合只需编辑文字内容的场景。
 * </p>
 */
public class SimpleTextScreen extends AbstractTextScreen {

    /**
     * 编辑面板宽度（像素）。
     */
    private static final int PANEL_W = 280;

    /**
     * 编辑面板高度（像素）。
     */
    private static final int PANEL_H = 180;

    /**
     * 控件之间的垂直间距（像素）。
     */
    private static final int SPACING = 8;

    /**
     * 面板 Y 坐标最小值（像素）。
     */
    private static final int PANEL_Y_MIN = 30;

    /**
     * 面板内边距（像素）。
     */
    private static final int PANEL_PADDING = 4;

    /**
     * 标题 X 偏移（像素）。
     */
    private static final int TITLE_X_OFFSET = 60;

    /**
     * 标题 Y 偏移（像素）。
     */
    private static final int TITLE_Y_OFFSET = 16;

    /**
     * 颜色输入框宽度（像素）。
     */
    private static final int COLOR_FIELD_W = 100;

    /**
     * 缩放输入框 X 偏移（像素）。
     */
    private static final int SCALE_FIELD_X = 130;

    /**
     * 缩放输入框宽度（像素）。
     */
    private static final int SCALE_FIELD_W = 70;

    /**
     * 颜色预览 X 偏移（像素）。
     */
    private static final int COLOR_PREVIEW_X = 210;

    /**
     * 颜色预览 Y 偏移（像素）。
     */
    private static final int COLOR_PREVIEW_Y_OFFSET = 2;

    /**
     * 发光按钮宽度（像素）。
     */
    private static final int GLOW_BTN_W = 120;

    /**
     * 底部按钮 Y 偏移（像素）。
     */
    private static final int BOTTOM_BTN_Y_OFFSET = 10;

    /**
     * 保存按钮宽度（像素）。
     */
    private static final int SAVE_BTN_W = 120;

    /**
     * 取消按钮 X 偏移（像素）。
     */
    private static final int CANCEL_BTN_X_OFFSET = 130;

    /**
     * 文字输入框最大长度。
     */
    private static final int TEXT_MAX_LENGTH = 128;

    /**
     * 颜色输入框最大长度。
     */
    private static final int COLOR_MAX_LENGTH = 6;

    /**
     * 缩放输入框最大长度。
     */
    private static final int SCALE_MAX_LENGTH = 6;

    /**
     * 面板左侧内边距（像素）。
     */
    private static final int PANEL_LEFT_PADDING = 10;

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
    private static final Vec3d DEFAULT_POSITION = new Vec3d(0.5, 0.6, 1.01);

    /**
     * 构造函数。
     * <p>
     * 从 BlockEntity 读取当前文字数据，如果为空则创建一个默认行。
     * 使用默认标题和默认位置。
     * </p>
     *
     * @param blockPos 目标方块位置
     */
    public SimpleTextScreen(BlockPos blockPos) {
        this(blockPos, DEFAULT_TITLE);
    }

    /**
     * 构造函数（可指定标题）。
     * <p>
     * 从 BlockEntity 读取当前文字数据，如果为空则创建一个默认行。
     * 由 {@link GuiScreenRegistry} 调用，传入方块指定的标题。
     * </p>
     *
     * @param blockPos 目标方块位置
     * @param title    窗口标题（为 null 时使用默认标题）
     */
    public SimpleTextScreen(BlockPos blockPos, Text title) {
        super(title != null ? title : DEFAULT_TITLE, blockPos, loadLines(blockPos));
    }

    /**
     * 从 BlockEntity 加载文字行，为空时创建默认行。
     * <p>
     * 使用 {@link #getDefaultPosition()} 作为默认位置。
     * </p>
     *
     * @param blockPos 方块位置
     * @return 文字行列表（始终包含一个元素）
     */
    private static List<TextLine> loadLines(BlockPos blockPos) {
        List<TextLine> lines = SignBlockClientService.readLines(blockPos);
        if (lines.isEmpty()) {
            List<TextLine> result = new ArrayList<>();
            result.add(TextLine.builder()
                    .text(Text.literal(""))
                    .position(DEFAULT_POSITION)
                    .color(0x000000)
                    .scale(1.0f)
                    .glowing(false)
                    .build());
            return result;
        }
        List<TextLine> result = new ArrayList<>();
        result.add(lines.get(0));
        return result;
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
     * Screen 初始化：根据窗口宽高动态计算所有控件坐标并实例化。
     * <p>
     * 与 {@link SingleLineScreen} 相比，省略了位置输入框（XYZ），
     * 布局更紧凑，面板高度更小。
     * </p>
     */
    @Override
    protected void init() {
        int w = width;
        int h = height;

        int panelX = (w - PANEL_W) / 2;
        int panelY = Math.max(PANEL_Y_MIN, (h - PANEL_H) / 2);

        int x = panelX + MARGIN;
        int y = panelY + LABEL_H + LABEL_H + PANEL_LEFT_PADDING;

        // 文字输入框
        textField = new TextFieldWidget(textRenderer, x, y, PANEL_W - MARGIN * 2, ROW_H, Text.literal(""));
        textField.setPlaceholder(Text.literal("输入文字内容"));
        textField.setMaxLength(TEXT_MAX_LENGTH);
        addDrawable(textField);
        addSelectableChild(textField);
        y += ROW_H + LABEL_H + SPACING;

        // 颜色和缩放输入框（无位置输入框）
        colorField = new TextFieldWidget(textRenderer, x, y, COLOR_FIELD_W, ROW_H, Text.literal(""));
        colorField.setPlaceholder(Text.literal("颜色 #RRGGBB"));
        colorField.setMaxLength(COLOR_MAX_LENGTH);
        scaleField = new TextFieldWidget(textRenderer, x + SCALE_FIELD_X, y, SCALE_FIELD_W, ROW_H, Text.literal(""));
        scaleField.setPlaceholder(Text.literal("缩放 1.0"));
        scaleField.setMaxLength(SCALE_MAX_LENGTH);
        addDrawable(colorField); addSelectableChild(colorField);
        addDrawable(scaleField); addSelectableChild(scaleField);

        colorPreviewX = x + COLOR_PREVIEW_X;
        colorPreviewY = y + COLOR_PREVIEW_Y_OFFSET;
        y += ROW_H + LABEL_H + SPACING;

        // 发光开关按钮
        TextLine line = editingLines.get(0);
        glowBtn = ButtonWidget.builder(
                Text.literal("发光: " + (line.glowing() ? "开" : "关")),
                b -> {
                    TextLine cur = editingLines.get(0);
                    TextLine updated = cur.withGlowing(!cur.glowing());
                    editingLines.set(0, updated);
                    glowBtn.setMessage(Text.literal("发光: " + (updated.glowing() ? "开" : "关")));
                }
        ).dimensions(x, y, GLOW_BTN_W, ROW_H).build();
        addDrawableChild(glowBtn);

        // 保存和取消按钮
        int btnY = panelY + PANEL_H - ROW_H - BOTTOM_BTN_Y_OFFSET;
        saveBtn = ButtonWidget.builder(Text.literal("保存"), b -> {
            saveEditToIndex(0);
            save();
        }).dimensions(panelX + PANEL_LEFT_PADDING, btnY, SAVE_BTN_W, 20).build();
        cancelBtn = ButtonWidget.builder(Text.literal("取消"), b -> close())
                .dimensions(panelX + PANEL_W - CANCEL_BTN_X_OFFSET, btnY, SAVE_BTN_W, 20).build();

        refreshEditFields(0);
        textField.setFocused(true);
    }

    /**
     * 将编辑区当前内容写回 editingLines 中指定索引的 TextLine。
     * <p>
     * 与父类不同，简化版使用 {@link #getDefaultPosition()} 作为位置，
     * 不从输入框读取位置数据。
     * </p>
     *
     * @param index 文字行索引
     */
    @Override
    protected void saveEditToIndex(int index) {
        if (index < 0 || index >= editingLines.size()) return;
        TextLine.Builder builder = TextLine.builder();
        builder.text(Text.literal(textField.getText()));
        // 使用默认位置，不从输入框读取
        builder.position(getDefaultPosition());
        int color;
        try {
            color = Integer.parseInt(colorField.getText().trim(), 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            color = 0xFFFFFFFF;
        }
        builder.color(color);
        builder.scale(parseFloatSafe(scaleField.getText(), 1.0f));
        builder.glowing(editingLines.get(index).glowing());
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

    /**
     * 主渲染方法，每帧调用一次。
     * <p>
     * 与 {@link SingleLineScreen} 相比，省略了位置标签的绘制。
     * </p>
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = width;
        int h = height;
        int panelX = (w - PANEL_W) / 2;
        int panelY = Math.max(PANEL_Y_MIN, (h - PANEL_H) / 2);

        context.fill(0, 0, w, h, 0x80000000);
        context.fill(panelX - PANEL_PADDING, panelY - PANEL_PADDING, panelX + PANEL_W + PANEL_PADDING, panelY + PANEL_H + PANEL_PADDING, 0x80100010);

        context.drawText(textRenderer, Text.literal("文字编辑器"), w / 2 - TITLE_X_OFFSET, panelY - TITLE_Y_OFFSET, 0xFFFFFF, true);

        int x = panelX + MARGIN;
        context.drawText(textRenderer, Text.literal("文字内容"), x, textField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("颜色 / 缩放"), x, colorField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);

        super.render(context, mouseX, mouseY, delta);

        drawColorPreview(context, 0);

        saveBtn.render(context, mouseX, mouseY, delta);
        cancelBtn.render(context, mouseX, mouseY, delta);
    }
}