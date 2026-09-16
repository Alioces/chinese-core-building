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
 * 路牌文字编辑 GUI。
 * <p>
 * 纯表示层——只负责渲染控件、收集用户输入，不直接访问网络层或 BlockEntity。
 * 所有数据读写都委托给 {@link SignBlockClientService}。
 * </p>
 * <p>
 * 左侧布局：「文字行列表:」标签 → 「+ 添加」「- 删除」按钮行 → 可滚动行区域（右侧带滚动条）。
 * 右侧布局：编辑选中行的文字、XYZ 位置、颜色、缩放、发光属性。
 * </p>
 */
public class SignBlockScreen extends Screen {

    /**
     * 左侧文字列表面板宽度（像素）。
     * <p>
     * 控制左侧列表区域的固定宽度，包括列表行、添加/删除按钮和滚动条。
     * 右侧编辑区的起始 X 坐标由此值和外边距共同决定。
     * </p>
     */
    private static final int LEFT_PANEL_W = 180;

    /**
     * 整个 UI 的外边距（像素）。
     * <p>
     * 用于左右面板的水平留白，以及窗口边缘到 UI 边界的距离。
     * 所有控件的坐标计算都以此值为基准进行偏移。
     * </p>
     */
    private static final int MARGIN = 20;

    /**
     * 单行高度（像素）。
     * <p>
     * 列表行和按钮行统一使用此高度，保证左侧面板的视觉一致性。
     * 输入框、按钮控件的高度也与此值对齐。
     * </p>
     */
    private static final int ROW_H = 20;

    /**
     * 标签文字高度（像素）。
     * <p>
     * 比 {@link #ROW_H} 小，作为输入框上方的描述性文字高度。
     * 用于计算输入框 Y 坐标时的垂直间距。
     * </p>
     */
    private static final int LABEL_H = 12;

    /**
     * 列表区最多同时显示的行数。
     * <p>
     * 超出此数量时启用滚动条，用户可通过鼠标滚轮或拖动滚动条浏览更多行。
     * 与 {@link #ROW_H} 共同决定列表可视区域高度。
     * </p>
     */
    private static final int LIST_VISIBLE_ROWS = 8;

    /**
     * 列表可视区域高度（像素）。
     * <p>
     * 等于 {@link #LIST_VISIBLE_ROWS} × {@link #ROW_H}，
     * 用于计算滚动条轨道和滑块的高度。
     * </p>
     */
    private static final int LIST_AREA_H = LIST_VISIBLE_ROWS * ROW_H;

    /**
     * 滚动条宽度（像素）。
     * <p>
     * 列表区域右侧滚动条的固定宽度，
     * 列表内容的右边界会减去此宽度以避免被滚动条遮挡。
     * </p>
     */
    private static final int SCROLLBAR_W = 6;

    /**
     * 目标方块位置。
     * <p>
     * 用于从 BlockEntity 读取当前文字数据，以及向服务端保存编辑结果。
     * 在构造函数中传入，整个 GUI 生命周期内保持不变。
     * </p>
     */
    private final BlockPos blockPos;

    /**
     * 当前正在编辑的文字行副本列表。
     * <p>
     * 编辑过程中的中间态数据，未同步回服务端。
     * 用户点击"保存"时才通过 {@link SignBlockClientService#saveLines} 提交。
     * 所有添加、删除、修改操作都作用于此列表。
     * </p>
     */
    private final List<TextLine> editingLines;

    /**
     * 当前选中行索引。
     * <p>
     * -1 表示无选中行，此时右侧编辑区显示为空。
     * 左侧列表点击和右侧编辑区的联动索引，
     * 任何 selectedIndex 变化都会触发 {@link #refreshEditFields()}。
     * </p>
     */
    private int selectedIndex = -1;

    /**
     * 列表区域滚动偏移。
     * <p>
     * 0 表示列表顶到最上方，N 表示向下滚了 N 行。
     * 仅当总行数超过 {@link #LIST_VISIBLE_ROWS} 时才生效。
     * 通过鼠标滚轮或 {@link #ensureSelectedVisible()} 自动调整。
     * </p>
     */
    private int listScroll = 0;

    /** 右侧面板的水平坐标起始值（X）和垂直坐标起始值（Y），以及面板宽度 */
    private int rightPanelX, rightPanelY, rightPanelW;

    /**
     * 左侧列表区域的坐标变量。
     * <ul>
     *   <li>{@code listX} — 列表区域左侧起点 X 坐标</li>
     *   <li>{@code listLabelY} — "文字行列表:" 标签的 Y 坐标</li>
     *   <li>{@code btnRowY} — 添加/删除按钮行的 Y 坐标</li>
     *   <li>{@code listY} — 列表内容区域的起点 Y 坐标</li>
     *   <li>{@code listBottom} — 列表内容区域的底部 Y 坐标</li>
     * </ul>
     */
    private int listX, listLabelY, btnRowY, listY, listBottom;

    /**
     * 文字内容输入框。
     * <p>
     * 位于右侧编辑区顶部，用于编辑选中行的文字内容。
     * 最大长度限制为 64 字符。
     * </p>
     */
    private TextFieldWidget textField;

    /**
     * XYZ 位置三元输入框。
     * <p>
     * 分别编辑 TextLine 的 position.x、position.y、position.z。
     * 值为方块空间相对偏移（0.5, 0.5, 1.0 = +Z 面中心）。
     * 每个输入框最大长度 8 字符。
     * </p>
     */
    private TextFieldWidget posXField, posYField, posZField;

    /**
     * 颜色（#RRGGBB）和缩放输入框。
     * <p>
     * {@code colorField} 编辑文字颜色，支持 6 位十六进制格式。
     * {@code scaleField} 编辑文字缩放因子，默认 1.0。
     * </p>
     */
    private TextFieldWidget colorField, scaleField;

    /**
     * 发光开关按钮。
     * <p>
     * 点击切换发光状态（开/关），按钮文字实时显示当前状态。
     * 发光文字使用满亮度渲染，绕过方块实际光照。
     * </p>
     */
    private ButtonWidget glowBtn;

    /**
     * 保存按钮。
     * <p>
     * 不注册为 children，手动 render 在最顶层避免被背景覆盖。
     * 点击后调用 {@link #save()} 提交编辑结果并关闭 GUI。
     * </p>
     */
    private ButtonWidget saveBtn;

    /**
     * 取消按钮。
     * <p>
     * 不注册为 children，手动 render 在最顶层避免被背景覆盖。
     * 点击后直接关闭 GUI，不保存任何修改。
     * </p>
     */
    private ButtonWidget cancelBtn;

    /**
     * 构造函数。
     * <p>
     * 从服务端同步过来的文字行通过 {@link SignBlockClientService#readLines} 拷贝一份副本，
     * 编辑过程中始终操作副本，直到用户点保存才统一提交。
     * </p>
     *
     * @param blockPos 目标方块位置（用于后续保存）
     */
    public SignBlockScreen(BlockPos blockPos) {
        super(Text.literal("编辑路牌文字"));
        this.blockPos = blockPos;
        this.editingLines = SignBlockClientService.readLines(blockPos);
    }

    /**
     * Screen 初始化：根据窗口宽高动态计算所有控件坐标并实例化。
     * <p>
     * Minecraft Screen 的标准生命周期：构造函数 → init() → render() 循环。
     * init() 保证每次窗口尺寸变化重新计算位置。
     * </p>
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
            refreshEditFields();
        }).dimensions(listX, btnRowY, 80, ROW_H).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("- 删除"), b -> {
            if (selectedIndex >= 0 && selectedIndex < editingLines.size()) {
                editingLines.remove(selectedIndex);
                selectedIndex = Math.min(selectedIndex, editingLines.size() - 1);
                ensureSelectedVisible();
                refreshEditFields();
            }
        }).dimensions(listX + 90, btnRowY, 80, ROW_H).build());

        saveBtn = ButtonWidget.builder(Text.literal("保存"), b -> save()).dimensions(w / 2 - 110, h - 34, 100, 20).build();
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
            refreshEditFields();
        }
    }

    /**
     * 确保 selectedIndex 处于可视区域内；超出时调整 listScroll。
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
     * 将当前选中行的属性同步到右侧编辑区的输入框。
     * <p>
     * 每次 selectedIndex 变化（列表点击、删除后、添加后）都调用此方法刷新。
     * 无选中行时清空所有输入框，避免残留旧数据。
     * </p>
     */
    private void refreshEditFields() {
        if (selectedIndex < 0 || selectedIndex >= editingLines.size()) {
            textField.setText("");
            posXField.setText("");
            posYField.setText("");
            posZField.setText("");
            colorField.setText("");
            scaleField.setText("");
            glowBtn.setMessage(Text.literal("发光: 关"));
            return;
        }
        TextLine line = editingLines.get(selectedIndex);
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
     * 将编辑区当前内容写回 editingLines 中 selectedIndex 对应的 TextLine。
     * <p>
     * 每次"切换选中行前"或"点击保存"时调用，确保输入框的最新值不丢失。
     * 使用 {@link TextLine.Builder} 重建实例（TextLine 是 record 不可变）。
     * </p>
     * <p>
     * 容错：颜色解析失败时默认黑色 0xFFFFFFFF，位置/缩放解析失败时用 0.5/1.0 兜底。
     * </p>
     */
    private void saveEditToSelected() {
        if (selectedIndex < 0) return;
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
        builder.glowing(editingLines.get(selectedIndex).glowing());
        editingLines.set(selectedIndex, builder.build());
    }

    /**
     * 安全解析浮点数，解析失败时返回 fallback。
     *
     * @param s        待解析字符串（来自 TextFieldWidget.getText()）
     * @param fallback 解析失败时的默认值
     * @return 解析成功的 float，或 fallback
     */
    private static float parseFloatSafe(String s, float fallback) {
        try { return Float.parseFloat(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    /**
     * 保存并关闭 GUI。
     * <p>
     * 先把当前选中行的编辑内容写回 editingLines（防止最后一行未切换就保存），
     * 然后调用 {@link SignBlockClientService#saveLines} 将完整列表发往服务端。
     * </p>
     */
    private void save() {
        saveEditToSelected();
        SignBlockClientService.saveLines(blockPos, new ArrayList<>(editingLines));
        close();
    }

    /**
     * 主渲染方法，每帧调用一次。
     * <p>
     * 渲染顺序：背景填充 → 两个面板半透明底板 → 标题 / 标签文字 → super.render()（子控件）
     * → 左侧列表行 → 滚动条 → 颜色预览 → 保存/取消按钮（手动 render 在最顶层）。
     * 保存/取消按钮因为要盖在所有面板之上，不注册为 children，在 render 末尾手动绘制。
     * </p>
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x80000000);

        int listAreaRight = listX + LEFT_PANEL_W - SCROLLBAR_W;
        context.fill(listX - 4, listLabelY - 4, listAreaRight + 2, listBottom + 4, 0x80100010);
        context.fill(rightPanelX - 4, rightPanelY - 4, rightPanelX + rightPanelW + 4, rightPanelY + 210, 0x80100010);

        context.drawText(textRenderer, Text.literal("路牌文字编辑器"), width / 2 - 60, 10, 0xFFFFFF, true);

        int x = rightPanelX;
        context.drawText(textRenderer, Text.literal("编辑选中行:"), x, rightPanelY, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("文字"), x, textField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("位置 (XYZ)"), x, posXField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);
        context.drawText(textRenderer, Text.literal("颜色 / 缩放"), x, colorField.getY() - LABEL_H - 2, 0xFFCCCCCC, false);

        super.render(context, mouseX, mouseY, delta);

        context.drawText(textRenderer, Text.literal("文字行列表:"), listX, listLabelY, 0xFFFFFF, false);

        renderListRows(context);
        drawScrollbar(context);

        drawColorPreview(context);

        saveBtn.render(context, mouseX, mouseY, delta);
        cancelBtn.render(context, mouseX, mouseY, delta);
    }

    /**
     * 在颜色输入框右侧绘制当前颜色预览方块（16x16，带 1px 边框）。
     * <p>
     * 无选中行或颜色无效时显示灰色占位。
     * </p>
     *
     * @param context DrawContext（Minecraft 统一绘图上下文）
     */
    private void drawColorPreview(DrawContext context) {
        int x = colorField.getX() + colorField.getWidth() + 4;
        int y = colorField.getY() + 2;
        int size = 16;
        int color = 0xFF808080;
        if (selectedIndex >= 0 && selectedIndex < editingLines.size()) {
            try {
                String hex = colorField.getText().trim();
                if (!hex.isEmpty()) {
                    color = Integer.parseInt(hex, 16) | 0xFF000000;
                } else {
                    color = editingLines.get(selectedIndex).color();
                }
            } catch (NumberFormatException ignored) {
                color = editingLines.get(selectedIndex).color();
            }
        }
        context.fill(x, y, x + size, y + size, color);
        context.fill(x - 1, y - 1, x + size + 1, y, 0xFF000000);
        context.fill(x - 1, y + size, x + size + 1, y + size + 1, 0xFF000000);
        context.fill(x - 1, y, x, y + size, 0xFF000000);
        context.fill(x + size, y, x + size + 1, y + size, 0xFF000000);
    }

    /**
     * 手动计算可视行范围；只渲染 [listScroll, listScroll + LIST_VISIBLE_ROWS) 内的数据行。
     *
     * @param context DrawContext（Minecraft 统一绘图上下文）
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
     * <p>
     * track 和 thumb 都严格限制在 [listY, listBottom] 范围内。
     * 仅当总行数超过可视行数时才显示 thumb。
     * </p>
     *
     * @param context DrawContext（Minecraft 统一绘图上下文）
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
     * <p>
     * 向上滚（amount > 0）listScroll 减小，列表向上滚；向下滚 listScroll 增大。
     * 用 Math.max/Math.min 限制在 [0, editingLines.size() - LIST_VISIBLE_ROWS] 内。
     * </p>
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     * @param amount 滚轮滚动量（正 = 向上滚，负 = 向下滚）
     * @return true 表示消费了事件，false 让父类继续处理
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
     * 键盘按下事件——ESC 关闭 GUI。
     *
     * @param keyCode  LWJGL 键码（参考 GLFW 常量）
     * @param scanCode 平台相关扫描码
     * @param modifiers 修饰位（Shift / Ctrl / Alt）
     * @return true 表示消费了事件
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
     * 鼠标点击事件处理。
     * <p>
     * 处理三个区域：
     * <ol>
     *   <li>左侧列表 — 点击行切换 selectedIndex（先 saveEditToSelected 保存当前编辑内容）</li>
     *   <li>保存按钮 — saveBtn.mouseClicked 内部调用 save() 关闭</li>
     *   <li>取消按钮 — cancelBtn.mouseClicked 内部调用 close() 关闭不保存</li>
     * </ol>
     * </p>
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     * @param button 鼠标按键（0 = 左键，1 = 右键）
     * @return true 表示消费了事件
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
                refreshEditFields();
                return true;
            }
        }
        if (button == 0 && saveBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && cancelBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 关闭 GUI，返回游戏。
     * <p>
     * 直接将 client.currentScreen 设为 null，不触发任何保存逻辑。
     * </p>
     */
    @Override
    public void close() {
        client.setScreen(null);
    }

    /**
     * GUI 不暂停游戏。
     * <p>
     * 返回 false 让玩家在编辑文字时游戏继续运行（方块仍在更新、动画仍在播放）。
     * 原版告示牌编辑器也是同样的行为。
     * </p>
     *
     * @return false
     */
    @Override
    public boolean shouldPause() {
        return false;
    }
}