package com.chinesecorebuilding.client.gui;

import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.client.SignBlockClientService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * 多行文字方块编辑 GUI。
 * <p>
 * 继承 {@link AbstractTextScreen}，实现多行文字的列表管理和编辑。
 * 纯表示层——只负责渲染控件、收集用户输入，不直接访问网络层或 BlockEntity。
 * </p>
 * <p>
 * 本 GUI 是公共文字功能基础设施的一部分，适用于任何需要多行文字编辑的场景。
 * 左侧布局：「文字行列表:」标签 → 「+ 添加」「- 删除」按钮行 → 可滚动行区域（右侧带滚动条）。
 * 右侧布局：编辑选中行的文字、XYZ 位置、颜色、缩放、发光属性。
 * </p>
 */
public class TextBlockScreen extends AbstractTextScreen {

    /**
     * 左侧文字列表面板宽度（像素）。
     */
    private static final int LEFT_PANEL_W = 180;

    /**
     * 列表区最多同时显示的行数。
     */
    private static final int LIST_VISIBLE_ROWS = 8;

    /**
     * 列表可视区域高度（像素）。
     */
    private static final int LIST_AREA_H = LIST_VISIBLE_ROWS * ROW_H;

    /**
     * 滚动条宽度（像素）。
     */
    private static final int SCROLLBAR_W = 6;

    /**
     * 当前选中行索引。
     */
    private int selectedIndex = -1;

    /**
     * 列表区域滚动偏移。
     */
    private int listScroll = 0;

    /** 右侧面板的坐标变量 */
    private int rightPanelX, rightPanelY, rightPanelW;

    /** 左侧列表区域的坐标变量 */
    private int listX, listLabelY, btnRowY, listY, listBottom;

    /**
     * 默认窗口标题。
     */
    private static final Text DEFAULT_TITLE = Text.literal("文字编辑器");

    /**
     * 构造函数。
     * <p>
     * 从服务端同步过来的文字行通过 {@link SignBlockClientService#readLines} 拷贝一份副本。
     * 使用默认标题。
     * </p>
     *
     * @param blockPos 目标文字方块位置
     */
    public TextBlockScreen(BlockPos blockPos) {
        this(blockPos, DEFAULT_TITLE);
    }

    /**
     * 构造函数（可指定标题）。
     * <p>
     * 从服务端同步过来的文字行通过 {@link SignBlockClientService#readLines} 拷贝一份副本。
     * 由 {@link GuiScreenRegistry} 调用，传入方块指定的标题。
     * </p>
     *
     * @param blockPos 目标文字方块位置
     * @param title    窗口标题（为 null 时使用默认标题）
     */
    public TextBlockScreen(BlockPos blockPos, Text title) {
        super(title != null ? title : DEFAULT_TITLE, blockPos, SignBlockClientService.readLines(blockPos));
    }

    /**
     * Screen 初始化：根据窗口宽高动态计算所有控件坐标并实例化。
     */
    @Override
    protected void init() {
        int w = width;
        int h = height;

        rightPanelW = Math.max(220, w - LEFT_PANEL_W - MARGIN * 4);
        rightPanelX = (w - LEFT_PANEL_W - rightPanelW - MARGIN) / 2 + LEFT_PANEL_W + MARGIN;
        rightPanelY = Math.max(30, (h - 260) / 2);

        listX = (w - LEFT_PANEL_W - rightPanelW - MARGIN) / 2;
        listLabelY = rightPanelY;
        btnRowY = listLabelY + LABEL_H + 2;
        listY = btnRowY + ROW_H + 2;
        listBottom = listY + LIST_AREA_H;

        int x = rightPanelX;
        int y = rightPanelY + LABEL_H + LABEL_H + 4;

        textField = new TextFieldWidget(textRenderer, x, y, rightPanelW, ROW_H, Text.literal(""));
        textField.setPlaceholder(Text.literal("输入文字内容"));
        addDrawable(textField); addSelectableChild(textField);
        y += ROW_H + LABEL_H + 4;

        posXField = new TextFieldWidget(textRenderer, x, y, 60, ROW_H, Text.literal(""));
        posXField.setPlaceholder(Text.literal("X"));
        posYField = new TextFieldWidget(textRenderer, x + 66, y, 60, ROW_H, Text.literal(""));
        posYField.setPlaceholder(Text.literal("Y"));
        posZField = new TextFieldWidget(textRenderer, x + 132, y, 60, ROW_H, Text.literal(""));
        posZField.setPlaceholder(Text.literal("Z"));
        addDrawable(posXField); addSelectableChild(posXField);
        addDrawable(posYField); addSelectableChild(posYField);
        addDrawable(posZField); addSelectableChild(posZField);
        y += ROW_H + LABEL_H + 4;

        colorField = new TextFieldWidget(textRenderer, x, y, 90, ROW_H, Text.literal(""));
        colorField.setPlaceholder(Text.literal("#RRGGBB"));
        scaleField = new TextFieldWidget(textRenderer, x + 114, y, 56, ROW_H, Text.literal(""));
        scaleField.setPlaceholder(Text.literal("1.0"));
        addDrawable(colorField); addSelectableChild(colorField);
        addDrawable(scaleField); addSelectableChild(scaleField);
        y += ROW_H + LABEL_H + 4;

        glowBtn = ButtonWidget.builder(Text.literal("发光: 关"), b -> {
            if (selectedIndex >= 0) {
                TextLine cur = editingLines.get(selectedIndex);
                TextLine updated = cur.withGlowing(!cur.glowing());
                editingLines.set(selectedIndex, updated);
                glowBtn.setMessage(Text.literal("发光: " + (updated.glowing() ? "开" : "关")));
            }
        }).dimensions(x, y, 110, ROW_H).build();
        addDrawableChild(glowBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("+ 添加"), b -> {
            editingLines.add(TextLine.builder().text(Text.literal("新行")).build());
            selectedIndex = editingLines.size() - 1;
            ensureSelectedVisible();
            refreshEditFields(selectedIndex);
        }).dimensions(listX, btnRowY, 80, ROW_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("- 删除"), b -> {
            if (selectedIndex >= 0 && selectedIndex < editingLines.size()) {
                editingLines.remove(selectedIndex);
                selectedIndex = Math.min(selectedIndex, editingLines.size() - 1);
                ensureSelectedVisible();
                refreshEditFields(selectedIndex);
            }
        }).dimensions(listX + 90, btnRowY, 80, ROW_H).build());

        saveBtn = ButtonWidget.builder(Text.literal("保存"), b -> {
            saveEditToSelected();
            save();
        }).dimensions(w / 2 - 110, h - 34, 100, 20).build();
        cancelBtn = ButtonWidget.builder(Text.literal("取消"), b -> close()).dimensions(w / 2 + 10, h - 34, 100, 20).build();

        textField.setFocused(true);
        textField.setMaxLength(64);
        posXField.setMaxLength(8);
        posYField.setMaxLength(8);
        posZField.setMaxLength(8);
        colorField.setMaxLength(8);
        scaleField.setMaxLength(6);

        if (!editingLines.isEmpty()) {
            selectedIndex = 0;
            refreshEditFields(selectedIndex);
        }
    }

    /**
     * 确保 selectedIndex 处于可视区域内。
     */
    private void ensureSelectedVisible() {
        if (selectedIndex < 0) { listScroll = 0; return; }
        if (selectedIndex < listScroll) {
            listScroll = selectedIndex;
        } else if (selectedIndex >= listScroll + LIST_VISIBLE_ROWS) {
            listScroll = selectedIndex - LIST_VISIBLE_ROWS + 1;
        }
        int maxScroll = Math.max(0, editingLines.size() - LIST_VISIBLE_ROWS);
        listScroll = Math.max(0, Math.min(maxScroll, listScroll));
    }

    /**
     * 将当前选中行的编辑内容写回 editingLines。
     */
    private void saveEditToSelected() {
        saveEditToIndex(selectedIndex);
    }

    /**
     * 主渲染方法，每帧调用一次。
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x80000000);

        int listAreaRight = listX + LEFT_PANEL_W - SCROLLBAR_W;
        context.fill(listX - 4, listLabelY - 4, listAreaRight + 2, listBottom + 4, 0x80100010);
        context.fill(rightPanelX - 4, rightPanelY - 4, rightPanelX + rightPanelW + 4, rightPanelY + 210, 0x80100010);

        context.drawText(textRenderer, Text.literal("文字编辑器"), width / 2 - 60, 10, 0xFFFFFF, true);

        int x = rightPanelX;
        context.drawText(textRenderer, Text.literal("编辑选中行:"), x, rightPanelY, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("文字"), x, textField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("位置 (XYZ)"), x, posXField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("颜色 / 缩放"), x, colorField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);

        super.render(context, mouseX, mouseY, delta);

        context.drawText(textRenderer, Text.literal("文字行列表:"), listX, listLabelY, 0xFFFFFF, false);

        renderListRows(context);
        drawScrollbar(context);

        drawColorPreview(context, selectedIndex);

        saveBtn.render(context, mouseX, mouseY, delta);
        cancelBtn.render(context, mouseX, mouseY, delta);
    }

    /**
     * 手动计算可视行范围并渲染列表行。
     */
    private void renderListRows(DrawContext context) {
        int total = editingLines.size();
        int endExclusive = Math.min(listScroll + LIST_VISIBLE_ROWS, total);
        int rowRight = listX + LEFT_PANEL_W - SCROLLBAR_W;

        for (int dataIndex = listScroll; dataIndex < endExclusive; dataIndex++) {
            int rowY = listY + (dataIndex - listScroll) * ROW_H;
            if (dataIndex == selectedIndex) {
                context.fill(listX, rowY, rowRight, rowY + ROW_H - 2, 0xFF404080);
            }
            String preview = editingLines.get(dataIndex).text().getString();
            if (preview.length() > 22) preview = preview.substring(0, 20) + "..";
            context.drawText(textRenderer,
                    Text.literal(String.format("%d: %s", dataIndex + 1, preview)),
                    listX + 4, rowY + 4, 0xFFFFFF, false);
        }
    }

    /**
     * 绘制列表区右侧滚动条。
     */
    private void drawScrollbar(DrawContext context) {
        int sbX = listX + LEFT_PANEL_W - SCROLLBAR_W;
        context.fill(sbX, listY, sbX + SCROLLBAR_W, listBottom, 0x55222222);

        int total = editingLines.size();
        if (total <= LIST_VISIBLE_ROWS) return;

        int maxScroll = total - LIST_VISIBLE_ROWS;
        int thumbH = Math.max(12, LIST_AREA_H * LIST_VISIBLE_ROWS / total);
        int travel = LIST_AREA_H - thumbH;
        int thumbY = listY + (int) (travel * (double) listScroll / maxScroll);
        context.fill(sbX + 1, thumbY, sbX + SCROLLBAR_W - 1, thumbY + thumbH, 0xFFAAAAAA);
    }

    /**
     * 鼠标滚轮事件处理——仅在左侧列表区域内生效。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= listX && mouseX <= listX + LEFT_PANEL_W
                && mouseY >= listY && mouseY <= listBottom) {
            int maxScroll = Math.max(0, editingLines.size() - LIST_VISIBLE_ROWS);
            listScroll = (int) Math.max(0, Math.min(maxScroll, listScroll - amount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    /**
     * 鼠标点击事件处理——列表行选择和保存/取消按钮。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rowRight = listX + LEFT_PANEL_W - SCROLLBAR_W;
        if (button == 0 && mouseX >= listX && mouseX <= rowRight
                && mouseY >= listY && mouseY <= listBottom) {
            int visibleRow = (int) ((mouseY - listY) / ROW_H);
            int clickedIndex = listScroll + visibleRow;
            if (clickedIndex >= 0 && clickedIndex < editingLines.size()) {
                saveEditToSelected();
                selectedIndex = clickedIndex;
                refreshEditFields(selectedIndex);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}