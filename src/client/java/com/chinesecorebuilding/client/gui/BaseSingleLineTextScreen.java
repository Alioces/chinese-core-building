package com.chinesecorebuilding.client.gui;

import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.client.SignBlockClientService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 单行文字编辑 GUI 抽象基类。
 * <p>
 * 提取 {@link SingleLineScreen} 和 {@link SimpleTextScreen} 的共同布局和交互逻辑，
 * 子类通过覆盖 {@link #showPositionFields()} 决定是否显示 XYZ 位置输入框。
 * </p>
 * <p>
 * 布局：居中的编辑面板，包含文字输入框、颜色、缩放、发光开关、保存和取消按钮。
 * 子类可通过覆盖模板方法调整面板尺寸、标题等。
 * </p>
 */
public abstract class BaseSingleLineTextScreen extends AbstractTextScreen {

    /** 编辑面板宽度（像素）。 */
    protected static final int PANEL_W = 300;

    /** 编辑面板高度（像素）。 */
    protected static final int PANEL_H = 220;

    /** 控件之间的垂直间距（像素）。 */
    protected static final int SPACING = 8;

    /** 面板 Y 坐标最小值（像素）。 */
    protected static final int PANEL_Y_MIN = 30;

    /** 面板内边距（像素）。 */
    protected static final int PANEL_PADDING = 4;

    /** 标题 X 偏移（像素）。 */
    protected static final int TITLE_X_OFFSET = 70;

    /** 标题 Y 偏移（像素）。 */
    protected static final int TITLE_Y_OFFSET = 16;

    /** 位置输入框宽度（像素）。 */
    protected static final int POS_FIELD_W = 80;

    /** 位置输入框间距（像素）。 */
    protected static final int POS_FIELD_GAP = 86;

    /** 颜色输入框宽度（像素）。 */
    protected static final int COLOR_FIELD_W = 100;

    /** 缩放输入框 X 偏移（像素）。 */
    protected static final int SCALE_FIELD_X = 140;

    /** 缩放输入框宽度（像素）。 */
    protected static final int SCALE_FIELD_W = 80;

    /** 颜色预览 X 偏移（像素）。 */
    protected static final int COLOR_PREVIEW_X = 250;

    /** 颜色预览 Y 偏移（像素）。 */
    protected static final int COLOR_PREVIEW_Y_OFFSET = 2;

    /** 发光按钮宽度（像素）。 */
    protected static final int GLOW_BTN_W = 120;

    /** 底部按钮 Y 偏移（像素）。 */
    protected static final int BOTTOM_BTN_Y_OFFSET = 10;

    /** 保存按钮宽度（像素）。 */
    protected static final int SAVE_BTN_W = 130;

    /** 取消按钮 X 偏移（像素）。 */
    protected static final int CANCEL_BTN_X_OFFSET = 140;

    /** 面板左侧内边距（像素）。 */
    protected static final int PANEL_LEFT_PADDING = 10;

    /** 文字输入框最大长度。 */
    protected static final int TEXT_MAX_LENGTH = 128;

    /** 位置输入框最大长度。 */
    protected static final int POS_MAX_LENGTH = 8;

    /** 颜色输入框最大长度。 */
    protected static final int COLOR_MAX_LENGTH = 6;

    /** 缩放输入框最大长度。 */
    protected static final int SCALE_MAX_LENGTH = 6;

    /**
     * 默认文字位置 X 分量。
     * <p>
     * 直接引用 {@link TextLine#DEFAULT_POSITION}，避免与数据层重复定义默认值。
     * 默认位置是"单一数据源"：GUI 兜底、Builder 默认值、NBT 兜底都应以它为准。
     * </p>
     */
    protected static final double DEFAULT_POS_X = TextLine.DEFAULT_POSITION.x;

    /** 默认文字位置 Y 分量，引用 {@link TextLine#DEFAULT_POSITION}。 */
    protected static final double DEFAULT_POS_Y = TextLine.DEFAULT_POSITION.y;

    /** 默认文字位置 Z 分量，引用 {@link TextLine#DEFAULT_POSITION}。 */
    protected static final double DEFAULT_POS_Z = TextLine.DEFAULT_POSITION.z;

    /** 默认文字颜色。 */
    protected static final int DEFAULT_COLOR = 0x000000;

    /** 默认文字缩放。 */
    protected static final float DEFAULT_SCALE = 1.0f;

    /** 默认发光状态。 */
    protected static final boolean DEFAULT_GLOWING = false;

    /**
     * 构造函数。
     * <p>
     * 子类 <b>必须</b>提供非 null 标题，通过 {@code super(blockPos, title)} 传入。
     * </p>
     *
     * @param blockPos 目标方块位置
     * @param title    窗口标题（不可为 null）
     */
    protected BaseSingleLineTextScreen(BlockPos blockPos, Text title) {
        super(title, blockPos, loadLines(blockPos));
    }

    // ==================== 模板方法（子类覆盖） ====================

    /**
     * 是否显示 XYZ 位置输入框。
     *
     * @return true 显示位置字段，false 隐藏
     */
    protected abstract boolean showPositionFields();

    /**
     * 获取编辑器内部标题文字（面板上方显示）。
     *
     * @return 编辑器标题字符串
     */
    protected abstract String getEditorTitle();

    // ==================== 数据加载 ====================

    /**
     * 从 BlockEntity 加载文字行，为空时创建默认行。
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
                    .position(DEFAULT_POS_X, DEFAULT_POS_Y, DEFAULT_POS_Z)
                    .color(DEFAULT_COLOR)
                    .scale(DEFAULT_SCALE)
                    .glowing(DEFAULT_GLOWING)
                    .build());
            return result;
        }
        List<TextLine> result = new ArrayList<>();
        result.add(lines.get(0));
        return result;
    }

    // ==================== 初始化 ====================

    /**
     * Screen 初始化：根据窗口宽高动态计算所有控件坐标并实例化。
     * <p>
     * 通过 {@link #showPositionFields()} 决定是否创建 XYZ 位置输入框。
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

        textField = new TextFieldWidget(textRenderer, x, y, PANEL_W - MARGIN * 2, ROW_H, Text.literal(""));
        textField.setPlaceholder(Text.literal("输入文字内容"));
        textField.setMaxLength(TEXT_MAX_LENGTH);
        addDrawable(textField);
        addSelectableChild(textField);
        y += ROW_H + LABEL_H + SPACING;

        if (showPositionFields()) {
            createPositionFields(x, y);
            y += ROW_H + LABEL_H + SPACING;
        }

        colorField = new TextFieldWidget(textRenderer, x, y, COLOR_FIELD_W, ROW_H, Text.literal(""));
        colorField.setPlaceholder(Text.literal("颜色 #RRGGBB"));
        colorField.setMaxLength(COLOR_MAX_LENGTH);
        scaleField = new TextFieldWidget(textRenderer, x + SCALE_FIELD_X, y, SCALE_FIELD_W, ROW_H, Text.literal(""));
        scaleField.setPlaceholder(Text.literal("缩放 1.0"));
        scaleField.setMaxLength(SCALE_MAX_LENGTH);
        addDrawable(colorField);
        addSelectableChild(colorField);
        addDrawable(scaleField);
        addSelectableChild(scaleField);

        colorPreviewX = x + COLOR_PREVIEW_X;
        colorPreviewY = y + COLOR_PREVIEW_Y_OFFSET;
        y += ROW_H + LABEL_H + SPACING;

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
     * 创建 XYZ 位置输入框。
     * <p>
     * 仅在 {@link #showPositionFields()} 返回 true 时调用。
     * 子类可覆盖以自定义位置字段布局。
     * </p>
     *
     * @param x 左边缘 X 坐标
     * @param y 上边缘 Y 坐标
     */
    protected void createPositionFields(int x, int y) {
        posXField = new TextFieldWidget(textRenderer, x, y, POS_FIELD_W, ROW_H, Text.literal(""));
        posXField.setPlaceholder(Text.literal("X"));
        posXField.setMaxLength(POS_MAX_LENGTH);
        posYField = new TextFieldWidget(textRenderer, x + POS_FIELD_GAP, y, POS_FIELD_W, ROW_H, Text.literal(""));
        posYField.setPlaceholder(Text.literal("Y"));
        posYField.setMaxLength(POS_MAX_LENGTH);
        posZField = new TextFieldWidget(textRenderer, x + POS_FIELD_W + POS_FIELD_GAP, y, POS_FIELD_W, ROW_H, Text.literal(""));
        posZField.setPlaceholder(Text.literal("Z"));
        posZField.setMaxLength(POS_MAX_LENGTH);
        addDrawable(posXField);
        addSelectableChild(posXField);
        addDrawable(posYField);
        addSelectableChild(posYField);
        addDrawable(posZField);
        addSelectableChild(posZField);
    }

    // ==================== 渲染 ====================

    /**
     * 主渲染方法，每帧调用一次。
     * <p>
     * 负责绘制半透明背景、面板、标题和标签。
     * 子类可覆盖以添加额外的标签或元素。
     * </p>
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = width;
        int h = height;
        int panelX = (w - PANEL_W) / 2;
        int panelY = Math.max(PANEL_Y_MIN, (h - PANEL_H) / 2);

        context.fill(0, 0, w, h, 0x80000000);
        context.fill(panelX - PANEL_PADDING, panelY - PANEL_PADDING,
                panelX + PANEL_W + PANEL_PADDING, panelY + PANEL_H + PANEL_PADDING, 0x80100010);

        context.drawText(textRenderer, Text.literal(getEditorTitle()),
                w / 2 - TITLE_X_OFFSET, panelY - TITLE_Y_OFFSET, 0xFFFFFF, true);

        int x = panelX + MARGIN;
        context.drawText(textRenderer, Text.literal("文字内容"),
                x, textField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);

        if (showPositionFields()) {
            context.drawText(textRenderer, Text.literal("位置 (XYZ)"),
                    x, posXField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        }

        context.drawText(textRenderer, Text.literal("颜色 / 缩放"),
                x, colorField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);

        super.render(context, mouseX, mouseY, delta);

        drawColorPreview(context, 0);

        saveBtn.render(context, mouseX, mouseY, delta);
        cancelBtn.render(context, mouseX, mouseY, delta);
    }
}