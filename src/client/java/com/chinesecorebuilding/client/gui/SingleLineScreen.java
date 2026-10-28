package com.chinesecorebuilding.client.gui;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * 单行文字方块编辑 GUI（带 XYZ 位置字段）。
 * <p>
 * 继承 {@link BaseSingleLineTextScreen}，包含完整的 XYZ 位置输入框，
 * 允许用户自定义文字在方块上的渲染位置。
 * </p>
 * <p>
 * 布局：居中的单行编辑面板，包含文字输入框、XYZ 位置、颜色、缩放、
 * 发光开关、保存和取消按钮。
 * </p>
 */
public class SingleLineScreen extends BaseSingleLineTextScreen {

    /**
     * 默认窗口标题。
     */
    private static final Text DEFAULT_TITLE = Text.literal("单行文字编辑器");

    /**
     * 编辑器内部标题。
     */
    private static final String EDITOR_TITLE = "单行文字编辑器";

    /**
     * 构造函数。
     *
     * @param blockPos 目标方块位置
     */
    public SingleLineScreen(BlockPos blockPos) {
        super(blockPos, DEFAULT_TITLE);
    }

    /**
     * 构造函数（可指定标题）。
     *
     * @param blockPos 目标方块位置
     * @param title    窗口标题（为 null 时使用默认标题）
     */
    public SingleLineScreen(BlockPos blockPos, Text title) {
        super(blockPos, title != null ? title : DEFAULT_TITLE);
    }

    @Override
    protected boolean showPositionFields() {
        return true;
    }

    @Override
    protected String getEditorTitle() {
        return EDITOR_TITLE;
    }
}