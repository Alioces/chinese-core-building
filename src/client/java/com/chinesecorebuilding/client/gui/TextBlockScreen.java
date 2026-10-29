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
     * 右侧面板最小宽度（像素）。
     */
    private static final int RIGHT_PANEL_MIN_W = 220;

    /**
     * 右侧面板高度（像素）。
     */
    private static final int RIGHT_PANEL_H = 210;

    /**
     * 面板 Y 坐标最小值（像素）。
     */
    private static final int PANEL_Y_MIN = 30;

    /**
     * 面板高度（像素）。
     */
    private static final int PANEL_H = 260;

    /**
     * 字段间距（像素）。
     */
    private static final int FIELD_SPACING = 4;

    /**
     * 位置输入框宽度（像素）。
     */
    private static final int POS_FIELD_W = 60;

    /**
     * 位置输入框间距（像素）。
     */
    private static final int POS_FIELD_GAP = 66;

    /**
     * 颜色输入框宽度（像素）。
     */
    private static final int COLOR_FIELD_W = 90;

    /**
     * 缩放输入框 X 偏移（像素）。
     */
    private static final int SCALE_FIELD_X = 114;

    /**
     * 缩放输入框宽度（像素）。
     */
    private static final int SCALE_FIELD_W = 56;

    /**
     * 颜色预览 X 偏移（像素）。
     */
    private static final int COLOR_PREVIEW_X_OFFSET = 95;

    /**
     * 发光按钮宽度（像素）。
     */
    private static final int GLOW_BTN_W = 110;

    /**
     * 添加/删除按钮宽度（像素）。
     */
    private static final int LIST_BTN_W = 80;

    /**
     * 删除按钮 X 偏移（像素）。
     */
    private static final int DEL_BTN_X_OFFSET = 90;

    /**
     * 保存/取消按钮宽度（像素）。
     */
    private static final int BOTTOM_BTN_W = 100;

    /**
     * 保存/取消按钮间距（像素）。
     */
    private static final int BOTTOM_BTN_GAP = 20;

    /**
     * 底部按钮 Y 偏移（像素）。
     */
    private static final int BOTTOM_BTN_Y_OFFSET = 34;

    /**
     * 文字输入框最大长度。
     */
    private static final int TEXT_MAX_LENGTH = 64;

    /**
     * 位置输入框最大长度。
     */
    private static final int POS_MAX_LENGTH = 8;

    /**
     * 缩放输入框最大长度。
     */
    private static final int SCALE_MAX_LENGTH = 6;

    /**
     * 列表预览最大显示长度。
     */
    private static final int LIST_PREVIEW_MAX_LEN = 22;

    /**
     * 列表预览截断长度。
     */
    private static final int LIST_PREVIEW_TRUNCATE_LEN = 20;

    /**
     * 滚动条滑块最小高度（像素）。
     */
    private static final int SCROLLBAR_THUMB_MIN_H = 12;

    /**
     * 滚动条内边距（像素）。
     */
    private static final int SCROLLBAR_PADDING = 1;

    /**
     * 面板内边距（像素）。
     */
    private static final int PANEL_PADDING = 4;

    /**
     * 标题 X 偏移（像素）。
     */
    private static final int TITLE_X_OFFSET = 60;

    /**
     * 标题 Y 坐标（像素）。
     */
    private static final int TITLE_Y = 10;

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

        rightPanelW = Math.max(RIGHT_PANEL_MIN_W, w - LEFT_PANEL_W - MARGIN * 4);
        rightPanelX = (w - LEFT_PANEL_W - rightPanelW - MARGIN) / 2 + LEFT_PANEL_W + MARGIN;
        rightPanelY = Math.max(PANEL_Y_MIN, (h - PANEL_H) / 2);

        listX = (w - LEFT_PANEL_W - rightPanelW - MARGIN) / 2;
        listLabelY = rightPanelY;
        btnRowY = listLabelY + LABEL_H + FIELD_SPACING;
        listY = btnRowY + ROW_H + FIELD_SPACING;
        listBottom = listY + LIST_AREA_H;

        int x = rightPanelX;
        int y = rightPanelY + LABEL_H + LABEL_H + FIELD_SPACING;

        textField = new TextFieldWidget(textRenderer, x, y, rightPanelW, ROW_H, Text.literal(""));
        textField.setPlaceholder(Text.literal("输入文字内容"));
        addDrawable(textField); addSelectableChild(textField);
        y += ROW_H + LABEL_H + FIELD_SPACING;

        posXField = new TextFieldWidget(textRenderer, x, y, POS_FIELD_W, ROW_H, Text.literal(""));
        posXField.setPlaceholder(Text.literal("X"));
        posYField = new TextFieldWidget(textRenderer, x + POS_FIELD_GAP, y, POS_FIELD_W, ROW_H, Text.literal(""));
        posYField.setPlaceholder(Text.literal("Y"));
        posZField = new TextFieldWidget(textRenderer, x + POS_FIELD_W + POS_FIELD_GAP, y, POS_FIELD_W, ROW_H, Text.literal(""));
        posZField.setPlaceholder(Text.literal("Z"));
        addDrawable(posXField); addSelectableChild(posXField);
        addDrawable(posYField); addSelectableChild(posYField);
        addDrawable(posZField); addSelectableChild(posZField);
        y += ROW_H + LABEL_H + FIELD_SPACING;

        colorField = new TextFieldWidget(textRenderer, x, y, COLOR_FIELD_W, ROW_H, Text.literal(""));
        colorField.setPlaceholder(Text.literal("#RRGGBB"));
        scaleField = new TextFieldWidget(textRenderer, x + SCALE_FIELD_X, y, SCALE_FIELD_W, ROW_H, Text.literal(""));
        scaleField.setPlaceholder(Text.literal("1.0"));
        addDrawable(colorField); addSelectableChild(colorField);
        addDrawable(scaleField); addSelectableChild(scaleField);

        // 颜色预览位置：颜色输入框右侧，与输入框垂直居中
        colorPreviewX = x + COLOR_PREVIEW_X_OFFSET;
        colorPreviewY = y + (ROW_H - COLOR_PREVIEW_SIZE) / 2;

        y += ROW_H + LABEL_H + FIELD_SPACING;

        glowBtn = ButtonWidget.builder(Text.literal("发光: 关"), b -> {
            if (selectedIndex >= 0) {
                TextLine cur = editingLines.get(selectedIndex);
                TextLine updated = cur.withGlowing(!cur.glowing());
                editingLines.set(selectedIndex, updated);
                glowBtn.setMessage(Text.literal("发光: " + (updated.glowing() ? "开" : "关")));
            }
        }).dimensions(x, y, GLOW_BTN_W, ROW_H).build();
        addDrawableChild(glowBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("+ 添加"), b -> {
            editingLines.add(TextLine.builder().text(Text.literal("新行")).build());
            selectedIndex = editingLines.size() - 1;
            ensureSelectedVisible();
            refreshEditFields(selectedIndex);
        }).dimensions(listX, btnRowY, LIST_BTN_W, ROW_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("- 删除"), b -> {
            if (selectedIndex >= 0 && selectedIndex < editingLines.size()) {
                editingLines.remove(selectedIndex);
                selectedIndex = Math.min(selectedIndex, editingLines.size() - 1);
                ensureSelectedVisible();
                refreshEditFields(selectedIndex);
            }
        }).dimensions(listX + DEL_BTN_X_OFFSET, btnRowY, LIST_BTN_W, ROW_H).build());

        saveBtn = ButtonWidget.builder(Text.literal("保存"), b -> {
            saveEditToSelected();
            save();
        }).dimensions(w / 2 - BOTTOM_BTN_W, h - BOTTOM_BTN_Y_OFFSET, BOTTOM_BTN_W, 20).build();
        cancelBtn = ButtonWidget.builder(Text.literal("取消"), b -> close()).dimensions(w / 2 + BOTTOM_BTN_GAP, h - BOTTOM_BTN_Y_OFFSET, BOTTOM_BTN_W, 20).build();

        textField.setFocused(true);
        textField.setMaxLength(TEXT_MAX_LENGTH);
        posXField.setMaxLength(POS_MAX_LENGTH);
        posYField.setMaxLength(POS_MAX_LENGTH);
        posZField.setMaxLength(POS_MAX_LENGTH);
        colorField.setMaxLength(POS_MAX_LENGTH);
        scaleField.setMaxLength(SCALE_MAX_LENGTH);

        ensureDefaultLine();

        selectedIndex = 0;
        refreshEditFields(selectedIndex);
    }

    /**
     * 确保列表中至少存在一行可编辑文字，否则自动补一行默认值。
     * <p>
     * 多行编辑器允许方块首次放置时 BlockEntity 中没有任何文字行（如 {@code test_sign}
     * 的 {@code createBlockEntity} 不播种初始行）。若不补默认行，玩家首次打开 GUI
     * 会看到空列表与空白输入框，必须手动点击"+ 添加"才能开始编辑，体验割裂。
     * </p>
     * <p>
     * 与 {@code BaseSingleLineTextScreen.loadLines} 的保底行为保持一致：
     * 位置取 {@link TextLine#DEFAULT_POSITION}，颜色、缩放、发光取 Builder 默认值。
     * </p>
     */
    private void ensureDefaultLine() {
        if (editingLines.isEmpty()) {
            editingLines.add(TextLine.builder().text(Text.literal("")).build());
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
        context.fill(listX - PANEL_PADDING, listLabelY - PANEL_PADDING, listAreaRight + SCROLLBAR_PADDING, listBottom + PANEL_PADDING, 0x80100010);
        context.fill(rightPanelX - PANEL_PADDING, rightPanelY - PANEL_PADDING, rightPanelX + rightPanelW + PANEL_PADDING, rightPanelY + RIGHT_PANEL_H, 0x80100010);

        context.drawText(textRenderer, Text.literal("文字编辑器"), width / 2 - TITLE_X_OFFSET, TITLE_Y, 0xFFFFFF, true);

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
            if (preview.length() > LIST_PREVIEW_MAX_LEN) preview = preview.substring(0, LIST_PREVIEW_TRUNCATE_LEN) + "..";
            context.drawText(textRenderer,
                    Text.literal(String.format("%d: %s", dataIndex + 1, preview)),
                    listX + PANEL_PADDING, rowY + PANEL_PADDING, 0xFFFFFF, false);
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
        int thumbH = Math.max(SCROLLBAR_THUMB_MIN_H, LIST_AREA_H * LIST_VISIBLE_ROWS / total);
        int travel = LIST_AREA_H - thumbH;
        int thumbY = listY + (int) (travel * (double) listScroll / maxScroll);
        context.fill(sbX + SCROLLBAR_PADDING, thumbY, sbX + SCROLLBAR_W - SCROLLBAR_PADDING, thumbY + thumbH, 0xFFAAAAAA);
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