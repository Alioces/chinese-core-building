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
 * 文字方块编辑 GUI 抽象基类。
 * <p>
 * 提取 {@link TextBlockScreen} 和 {@link SingleLineScreen} 的共同逻辑，
 * 包括编辑字段管理、保存流程、颜色预览、事件处理等。
 * 子类只需实现布局构建和渲染差异部分。
 * </p>
 * <p>
 * 本类是公共文字功能基础设施的一部分，为所有需要文字编辑功能的 GUI 提供统一的
 * 字段管理、保存流程和交互逻辑。
 * </p>
 * <p>
 * 职责：
 * <ul>
 *   <li>管理编辑字段（文字/位置/颜色/缩放/发光）</li>
 *   <li>提供保存流程（编辑内容写回 → 网络同步）</li>
 *   <li>提供颜色预览绘制</li>
 *   <li>提供 ESC 关闭、鼠标点击处理</li>
 * </ul>
 * </p>
 */
public abstract class AbstractTextScreen extends Screen {

    /**
     * 单行控件高度（像素）。
     */
    protected static final int ROW_H = 20;

    /**
     * 标签文字高度（像素）。
     */
    protected static final int LABEL_H = 12;

    /**
     * 整个 UI 的外边距（像素）。
     */
    protected static final int MARGIN = 20;

    /**
     * 颜色预览方块尺寸（像素）。
     */
    protected static final int COLOR_PREVIEW_SIZE = 16;

    /**
     * 目标方块位置。
     */
    protected final BlockPos blockPos;

    /**
     * 当前编辑的文字行副本列表。
     */
    protected final List<TextLine> editingLines;

    /**
     * 文字内容输入框。
     */
    protected TextFieldWidget textField;

    /**
     * XYZ 位置三元输入框。
     */
    protected TextFieldWidget posXField, posYField, posZField;

    /**
     * 颜色和缩放输入框。
     */
    protected TextFieldWidget colorField, scaleField;

    /**
     * 发光开关按钮。
     */
    protected ButtonWidget glowBtn;

    /**
     * 保存按钮。
     */
    protected ButtonWidget saveBtn;

    /**
     * 取消按钮。
     */
    protected ButtonWidget cancelBtn;

    /**
     * 颜色预览位置。
     */
    protected int colorPreviewX, colorPreviewY;

    /**
     * 构造函数。
     *
     * @param title        窗口标题
     * @param blockPos     目标方块位置
     * @param editingLines 编辑中的文字行副本
     */
    protected AbstractTextScreen(Text title, BlockPos blockPos, List<TextLine> editingLines) {
        super(title);
        this.blockPos = blockPos;
        this.editingLines = editingLines;
    }

    /**
     * 将指定行的属性同步到编辑区的输入框。
     *
     * @param index 文字行索引
     */
    protected void refreshEditFields(int index) {
        if (index < 0 || index >= editingLines.size()) {
            textField.setText("");
            posXField.setText("");
            posYField.setText("");
            posZField.setText("");
            colorField.setText("");
            scaleField.setText("");
            glowBtn.setMessage(Text.literal("发光: 关"));
            return;
        }
        TextLine line = editingLines.get(index);
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
     * 将编辑区当前内容写回 editingLines 中指定索引的 TextLine。
     *
     * @param index 文字行索引
     */
    protected void saveEditToIndex(int index) {
        if (index < 0 || index >= editingLines.size()) return;
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
            color = 0xFF808080;
        }
        builder.color(color);
        builder.scale(parseFloatSafe(scaleField.getText(), 1.0f));
        builder.glowing(editingLines.get(index).glowing());
        editingLines.set(index, builder.build());
    }

    /**
     * 安全解析浮点数，解析失败时返回 fallback。
     *
     * @param s        待解析字符串
     * @param fallback 解析失败时的默认值
     * @return 解析成功的 float，或 fallback
     */
    protected static float parseFloatSafe(String s, float fallback) {
        try { return Float.parseFloat(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    /**
     * 保存并关闭 GUI。
     * <p>
     * 子类应调用 {@link #saveEditToIndex(int)} 保存当前编辑内容，
     * 然后调用此方法提交到服务端。
     * </p>
     */
    protected void save() {
        SignBlockClientService.saveLines(blockPos, new ArrayList<>(editingLines));
        close();
    }

    /**
     * 在颜色输入框右侧绘制当前颜色预览方块（16x16，带 1px 边框）。
     *
     * @param context DrawContext
     * @param index   文字行索引（用于获取当前颜色）
     */
    protected void drawColorPreview(DrawContext context, int index) {
        int color = 0xFF808080;
        try {
            String hex = colorField.getText().trim();
            if (!hex.isEmpty()) {
                color = Integer.parseInt(hex, 16) | 0xFF000000;
            } else if (index >= 0 && index < editingLines.size()) {
                color = editingLines.get(index).color();
            }
        } catch (NumberFormatException ignored) {
            if (index >= 0 && index < editingLines.size()) {
                color = editingLines.get(index).color();
            }
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