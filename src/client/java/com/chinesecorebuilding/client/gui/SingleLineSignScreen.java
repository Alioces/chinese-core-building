package com.chinesecorebuilding.client.gui;

import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.client.SignBlockClientService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 单行路牌文字编辑 GUI。
 * <p>
 * 基于 {@link SignBlockScreen} 的编辑逻辑简化而来，
 * 只显示单行文字的编辑控件，隐藏列表区域和添加/删除按钮。
 * 所有数据读写复用 {@link SignBlockClientService}。
 * </p>
 * <p>
 * 布局：居中的单行编辑面板，包含文字输入框、XYZ 位置、颜色、缩放、发光开关、
 * 保存和取消按钮。比多行编辑器更简洁，适合只需编辑单行文字的场景。
 * </p>
 */
public class SingleLineSignScreen extends Screen {

    /**
     * 编辑面板宽度（像素）。
     */
    private static final int PANEL_W = 300;

    /**
     * 整个 UI 的外边距（像素）。
     */
    private static final int MARGIN = 20;

    /**
     * 单行控件高度（像素）。
     */
    private static final int ROW_H = 20;

    /**
     * 标签文字高度（像素）。
     */
    private static final int LABEL_H = 12;

    /**
     * 控件之间的垂直间距（像素）。
     */
    private static final int SPACING = 8;

    /**
     * 目标方块位置。
     */
    private final BlockPos blockPos;

    /**
     * 当前编辑的文字行副本。
     * <p>
     * 单行模式下，此列表始终只包含一个 TextLine 元素。
     * 编辑过程中的中间态数据，未同步回服务端。
     * 用户点击"保存"时才通过 {@link SignBlockClientService#saveLines} 提交。
     * </p>
     */
    private final List<TextLine> editingLines;

    /**
     * 文字内容输入框。
     */
    private TextFieldWidget textField;

    /**
     * XYZ 位置三元输入框。
     */
    private TextFieldWidget posXField, posYField, posZField;

    /**
     * 颜色和缩放输入框。
     */
    private TextFieldWidget colorField, scaleField;

    /**
     * 发光开关按钮。
     */
    private ButtonWidget glowBtn;

    /**
     * 保存按钮。
     */
    private ButtonWidget saveBtn;

    /**
     * 取消按钮。
     */
    private ButtonWidget cancelBtn;

    /**
     * 颜色预览位置。
     */
    private int colorPreviewX, colorPreviewY;
    private static final int COLOR_PREVIEW_SIZE = 16;

    /**
     * 构造函数。
     * <p>
     * 从 BlockEntity 读取当前文字数据，如果为空则创建一个默认行。
     * </p>
     *
     * @param blockPos 目标方块位置
     */
    public SingleLineSignScreen(BlockPos blockPos) {
        super(Text.literal("编辑单行路牌文字"));
        this.blockPos = blockPos;
        List<TextLine> lines = SignBlockClientService.readLines(blockPos);
        if (lines.isEmpty()) {
            // 创建默认单行
            this.editingLines = new ArrayList<>();
            this.editingLines.add(TextLine.builder()
                    .text(Text.literal(""))
                    .position(0.5, 0.6, 0.501)
                    .color(0xFFFFFF)
                    .scale(1.0f)
                    .glowing(false)
                    .build());
        } else {
            // 只取第一行
            this.editingLines = new ArrayList<>();
            this.editingLines.add(lines.get(0));
        }
    }

    /**
     * Screen 初始化：根据窗口宽高动态计算所有控件坐标并实例化。
     */
    @Override
    protected void init() {
        int w = width;
        int h = height;

        // 面板居中计算
        int panelX = (w - PANEL_W) / 2;
        int panelY = Math.max(30, (h - 200) / 2);

        int x = panelX + MARGIN;
        int y = panelY + LABEL_H + LABEL_H + 4;

        // 文字输入框
        textField = new TextFieldWidget(textRenderer, x, y, PANEL_W - MARGIN * 2, ROW_H, Text.literal(""));
        textField.setPlaceholder(Text.literal("输入文字内容"));
        textField.setMaxLength(128);
        addDrawable(textField);
        addSelectableChild(textField);
        y += ROW_H + LABEL_H + SPACING;

        // XYZ 位置
        posXField = new TextFieldWidget(textRenderer, x, y, 80, ROW_H, Text.literal(""));
        posXField.setPlaceholder(Text.literal("X"));
        posXField.setMaxLength(8);
        posYField = new TextFieldWidget(textRenderer, x + 86, y, 80, ROW_H, Text.literal(""));
        posYField.setPlaceholder(Text.literal("Y"));
        posYField.setMaxLength(8);
        posZField = new TextFieldWidget(textRenderer, x + 172, y, 80, ROW_H, Text.literal(""));
        posZField.setPlaceholder(Text.literal("Z"));
        posZField.setMaxLength(8);
        addDrawable(posXField); addSelectableChild(posXField);
        addDrawable(posYField); addSelectableChild(posYField);
        addDrawable(posZField); addSelectableChild(posZField);
        y += ROW_H + LABEL_H + SPACING;

        // 颜色和缩放
        colorField = new TextFieldWidget(textRenderer, x, y, 120, ROW_H, Text.literal(""));
        colorField.setPlaceholder(Text.literal("颜色 #RRGGBB"));
        colorField.setMaxLength(6);
        scaleField = new TextFieldWidget(textRenderer, x + 130, y, 100, ROW_H, Text.literal(""));
        scaleField.setPlaceholder(Text.literal("缩放 1.0"));
        scaleField.setMaxLength(6);
        addDrawable(colorField); addSelectableChild(colorField);
        addDrawable(scaleField); addSelectableChild(scaleField);

        // 颜色预览位置
        colorPreviewX = x + 120 + 8;
        colorPreviewY = y + 2;
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
        ).dimensions(x, y, 120, ROW_H).build();
        addDrawableChild(glowBtn);

        // 保存和取消按钮
        int btnY = panelY + 160;
        saveBtn = ButtonWidget.builder(Text.literal("保存"), b -> save())
                .dimensions(w / 2 - 110, btnY, 100, 20).build();
        cancelBtn = ButtonWidget.builder(Text.literal("取消"), b -> close())
                .dimensions(w / 2 + 10, btnY, 100, 20).build();

        // 填充编辑字段
        refreshEditFields();

        // 默认聚焦文字输入框
        textField.setFocused(true);
    }

    /**
     * 将当前 TextLine 的属性同步到编辑区的输入框。
     */
    private void refreshEditFields() {
        TextLine line = editingLines.get(0);
        textField.setText(line.text().getString());
        if (line.position() != null) {
            posXField.setText(String.format("%.3f", line.position().x));
            posYField.setText(String.format("%.3f", line.position().y));
            posZField.setText(String.format("%.3f", line.position().z));
        }
        colorField.setText(String.format("%06X", line.color() & 0xFFFFFF));
        scaleField.setText(String.format("%.2f", line.scale()));
        glowBtn.setMessage(Text.literal("发光: " + (line.glowing() ? "开" : "关")));
    }

    /**
     * 将编辑区当前内容写回 editingLines[0]。
     */
    private void saveEditToLine() {
        TextLine.Builder builder = TextLine.builder();
        builder.text(Text.literal(textField.getText()));
        builder.position(
                parseFloatSafe(posXField.getText(), 0.5f),
                parseFloatSafe(posYField.getText(), 0.5f),
                parseFloatSafe(posZField.getText(), 0.5f)
        );
        int color;
        try {
            color = Integer.parseInt(colorField.getText().trim(), 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            color = 0xFFFFFFFF;
        }
        builder.color(color);
        builder.scale(parseFloatSafe(scaleField.getText(), 1.0f));
        builder.glowing(editingLines.get(0).glowing());
        editingLines.set(0, builder.build());
    }

    /**
     * 安全解析浮点数。
     */
    private static float parseFloatSafe(String s, float fallback) {
        try { return Float.parseFloat(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    /**
     * 保存并关闭 GUI。
     */
    private void save() {
        saveEditToLine();
        SignBlockClientService.saveLines(blockPos, new ArrayList<>(editingLines));
        close();
    }

    /**
     * 主渲染方法，每帧调用一次。
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = width;
        int h = height;
        int panelX = (w - PANEL_W) / 2;
        int panelY = Math.max(30, (h - 200) / 2);

        // 背景填充
        context.fill(0, 0, w, h, 0x80000000);

        // 面板底板
        context.fill(panelX - 4, panelY - 4, panelX + PANEL_W + 4, panelY + 180, 0x80100010);

        // 标题
        context.drawText(textRenderer, Text.literal("单行路牌文字编辑器"), w / 2 - 70, 10, 0xFFFFFF, true);

        // 标签文字
        int x = panelX + MARGIN;
        context.drawText(textRenderer, Text.literal("文字内容"), x, textField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("位置 (XYZ)"), x, posXField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("颜色 / 缩放"), x, colorField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);

        // 渲染子控件
        super.render(context, mouseX, mouseY, delta);

        // 颜色预览
        drawColorPreview(context);

        // 保存/取消按钮
        saveBtn.render(context, mouseX, mouseY, delta);
        cancelBtn.render(context, mouseX, mouseY, delta);
    }

    /**
     * 绘制颜色预览方块。
     */
    private void drawColorPreview(DrawContext context) {
        int color = 0xFF808080;
        try {
            String hex = colorField.getText().trim();
            if (!hex.isEmpty()) {
                color = Integer.parseInt(hex, 16) | 0xFF000000;
            }
        } catch (NumberFormatException ignored) {
            color = editingLines.get(0).color();
        }

        context.fill(colorPreviewX, colorPreviewY,
                colorPreviewX + COLOR_PREVIEW_SIZE, colorPreviewY + COLOR_PREVIEW_SIZE, color);
        context.fill(colorPreviewX - 1, colorPreviewY - 1,
                colorPreviewX + COLOR_PREVIEW_SIZE + 1, colorPreviewY, 0xFF000000);
        context.fill(colorPreviewX - 1, colorPreviewY + COLOR_PREVIEW_SIZE,
                colorPreviewX + COLOR_PREVIEW_SIZE + 1, colorPreviewY + COLOR_PREVIEW_SIZE + 1, 0xFF000000);
        context.fill(colorPreviewX - 1, colorPreviewY,
                colorPreviewX, colorPreviewY + COLOR_PREVIEW_SIZE, 0xFF000000);
        context.fill(colorPreviewX + COLOR_PREVIEW_SIZE, colorPreviewY,
                colorPreviewX + COLOR_PREVIEW_SIZE + 1, colorPreviewY + COLOR_PREVIEW_SIZE, 0xFF000000);
    }

    /**
     * 键盘按下事件——ESC 关闭 GUI。
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 鼠标点击事件处理——保存/取消按钮。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && saveBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && cancelBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 关闭 GUI，返回游戏。
     */
    @Override
    public void close() {
        client.setScreen(null);
    }

    /**
     * GUI 不暂停游戏。
     */
    @Override
    public boolean shouldPause() {
        return false;
    }
}