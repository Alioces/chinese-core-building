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
 * 单行文字方块编辑 GUI。
 * <p>
 * 继承 {@link AbstractTextScreen}，只显示单行文字的编辑控件，
 * 隐藏列表区域和添加/删除按钮。
 * </p>
 * <p>
 * 本 GUI 是公共文字功能基础设施的一部分，适用于任何只需要编辑单行文字的场景。
 * 布局：居中的单行编辑面板，包含文字输入框、XYZ 位置、颜色、缩放、发光开关、
 * 保存和取消按钮。比多行编辑器更简洁，适合只需编辑单行文字的场景。
 * </p>
 */
public class SingleLineScreen extends AbstractTextScreen {

    /**
     * 编辑面板宽度（像素）。
     */
    private static final int PANEL_W = 300;

    /**
     * 编辑面板高度（像素）。
     */
    private static final int PANEL_H = 220;

    /**
     * 控件之间的垂直间距（像素）。
     */
    private static final int SPACING = 8;

    /**
     * 构造函数。
     * <p>
     * 从 BlockEntity 读取当前文字数据，如果为空则创建一个默认行。
     * </p>
     *
     * @param blockPos 目标方块位置
     */
    public SingleLineScreen(BlockPos blockPos) {
        super(Text.literal("编辑单行文字"), blockPos, loadLines(blockPos));
    }

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
                    .position(0.5, 0.6, 0.501)
                    .color(0xFFFFFF)
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
     * Screen 初始化：根据窗口宽高动态计算所有控件坐标并实例化。
     */
    @Override
    protected void init() {
        int w = width;
        int h = height;

        int panelX = (w - PANEL_W) / 2;
        int panelY = Math.max(30, (h - PANEL_H) / 2);

        int x = panelX + MARGIN;
        int y = panelY + LABEL_H + LABEL_H + 4;

        textField = new TextFieldWidget(textRenderer, x, y, PANEL_W - MARGIN * 2, ROW_H, Text.literal(""));
        textField.setPlaceholder(Text.literal("输入文字内容"));
        textField.setMaxLength(128);
        addDrawable(textField);
        addSelectableChild(textField);
        y += ROW_H + LABEL_H + SPACING;

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

        colorField = new TextFieldWidget(textRenderer, x, y, 100, ROW_H, Text.literal(""));
        colorField.setPlaceholder(Text.literal("颜色 #RRGGBB"));
        colorField.setMaxLength(6);
        scaleField = new TextFieldWidget(textRenderer, x + 140, y, 80, ROW_H, Text.literal(""));
        scaleField.setPlaceholder(Text.literal("缩放 1.0"));
        scaleField.setMaxLength(6);
        addDrawable(colorField); addSelectableChild(colorField);
        addDrawable(scaleField); addSelectableChild(scaleField);

        colorPreviewX = x + 250;
        colorPreviewY = y + 2;
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
        ).dimensions(x, y, 120, ROW_H).build();
        addDrawableChild(glowBtn);

        int btnY = panelY + PANEL_H - ROW_H - 10;
        saveBtn = ButtonWidget.builder(Text.literal("保存"), b -> {
            saveEditToIndex(0);
            save();
        }).dimensions(panelX + 10, btnY, 130, 20).build();
        cancelBtn = ButtonWidget.builder(Text.literal("取消"), b -> close())
                .dimensions(panelX + PANEL_W - 140, btnY, 130, 20).build();

        refreshEditFields(0);
        textField.setFocused(true);
    }

    /**
     * 主渲染方法，每帧调用一次。
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = width;
        int h = height;
        int panelX = (w - PANEL_W) / 2;
        int panelY = Math.max(30, (h - PANEL_H) / 2);

        context.fill(0, 0, w, h, 0x80000000);
        context.fill(panelX - 4, panelY - 4, panelX + PANEL_W + 4, panelY + PANEL_H + 4, 0x80100010);

        context.drawText(textRenderer, Text.literal("单行文字编辑器"), w / 2 - 70, panelY - 16, 0xFFFFFF, true);

        int x = panelX + MARGIN;
        context.drawText(textRenderer, Text.literal("文字内容"), x, textField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("位置 (XYZ)"), x, posXField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("颜色 / 缩放"), x, colorField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);

        super.render(context, mouseX, mouseY, delta);

        drawColorPreview(context, 0);

        saveBtn.render(context, mouseX, mouseY, delta);
        cancelBtn.render(context, mouseX, mouseY, delta);
    }
}