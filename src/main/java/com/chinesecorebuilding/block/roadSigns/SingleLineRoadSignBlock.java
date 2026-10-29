package com.chinesecorebuilding.block.roadSigns;

import com.chinesecorebuilding.block.SingleLineSignBlock;
import com.chinesecorebuilding.block.TestSignBlock;
import com.chinesecorebuilding.block.entity.ChineseCoreBuildingEntities;
import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import com.chinesecorebuilding.block.properties.GuiInteractive;
import com.chinesecorebuilding.block.properties.model.SignTextProvider;
import com.chinesecorebuilding.util.OffsetFunction;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * 单行文字路标牌方块。
 * <p>
 * 继承 {@link RoadSignsBlock} 的路标能力（方向、偏移、CUTOUT 渲染层），
 * 同时实现 {@link GuiInteractive}、{@link BlockEntityProvider}、{@link SignTextProvider} 接口，
 * 使路标牌支持单行文字编辑功能。
 * </p>
 * <p>
 * 本方块是公共文字功能的单行模式实现，适用于需要单行文字编辑的路标场景。
 * 复用 {@link TextBlockEntity} 和 {@link TestSignBlock#ENTITY_TYPE}，
 * 不创建新的 BlockEntityType，简化渲染器和网络层逻辑。
 * </p>
 * <p>
 * 能力接口：
 * <ul>
 *   <li>{@link RoadSignsBlock} — 方向、偏移、CUTOUT 渲染层</li>
 *   <li>{@link GuiInteractive} — 右键打开单行编辑 GUI</li>
 *   <li>{@link BlockEntityProvider} — 提供 BlockEntity</li>
 *   <li>{@link SignTextProvider} — 文字层渲染</li>
 * </ul>
 * </p>
 *
 * @see RoadSignsBlock
 * @see SingleLineSignBlock
 * @see GuiInteractive
 * @see SignTextProvider
 */
public class SingleLineRoadSignBlock extends RoadSignsBlock
        implements GuiInteractive, BlockEntityProvider, SignTextProvider {

    /**
     * 默认文字位置：方块局部坐标 (0.5, 0.6, 0.9)。
     * <p>
     * 该坐标由实际测试得出，配合路标板面模型可让文字正对观察者。
     * 在 {@link #createBlockEntity} 中作为初始行的位置播种，
     * 使 GUI 打开时直接沿用此值，无需用户手动填写。
     * </p>
     */
    private static final double DEFAULT_TEXT_X = 0.5;

    /** 默认文字位置 Y 分量（方块局部坐标）。 */
    private static final double DEFAULT_TEXT_Y = 0.6;

    /** 默认文字位置 Z 分量（方块局部坐标）。 */
    private static final double DEFAULT_TEXT_Z = 0.9;

    /** 默认文字颜色：黑色（0x000000）。 */
    private static final int DEFAULT_TEXT_COLOR = 0x000000;

    /** 默认文字缩放：1.0（不缩放）。 */
    private static final float DEFAULT_TEXT_SCALE = 1.0f;

    /** 默认文字发光状态：关闭。 */
    private static final boolean DEFAULT_TEXT_GLOWING = false;

    /**
     * 构造函数，使用默认偏移（1.5 单位水平 + 0 Y 偏移）。
     *
     * @param settings 方块属性配置
     */
    public SingleLineRoadSignBlock(Settings settings) {
        super(settings);
    }

    /**
     * 构造函数，自定义偏移函数。
     *
     * @param settings 方块属性配置
     * @param offset   偏移值提供器
     */
    public SingleLineRoadSignBlock(Settings settings, OffsetFunction offset) {
        super(settings, offset);
    }

    /**
     * 构造函数，仅自定义 Y 偏移。
     *
     * @param settings 方块属性配置
     * @param offsetY  垂直偏移（单位：方块）
     */
    public SingleLineRoadSignBlock(Settings settings, float offsetY) {
        super(settings, offsetY);
    }

    /**
     * 创建 TextBlockEntity 实例，并播种一行默认文字。
     * <p>
     * 复用 {@link ChineseCoreBuildingEntities#TEXT_BLOCK_ENTITY_TYPE}，不创建新实体类型。
     * 播种初始行是必要的：GUI 打开时 {@code BaseSingleLineTextScreen.loadLines}
     * 优先读取 BlockEntity 中已有的第 0 行，只有列表为空时才回退到 GUI 内置常量。
     * 因此在此写入默认位置 (0.5, 0.6, 0.9)，可让 GUI 与渲染器都直接使用本类定义的默认值。
     * </p>
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 已播种默认文字行的 TextBlockEntity
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        TextBlockEntity entity = new TextBlockEntity(pos, state);
        entity.setLines(List.of(createDefaultLine()));
        return entity;
    }

    /**
     * 构建默认文字行。
     * <p>
     * 内容为空字符串，位置使用 {@link #DEFAULT_TEXT_X} / {@link #DEFAULT_TEXT_Y} /
     * {@link #DEFAULT_TEXT_Z}，其余外观属性取类内默认常量。
     * </p>
     *
     * @return 默认 TextLine 实例
     */
    private static TextLine createDefaultLine() {
        return TextLine.builder()
                .text(Text.literal(""))
                .position(DEFAULT_TEXT_X, DEFAULT_TEXT_Y, DEFAULT_TEXT_Z)
                .color(DEFAULT_TEXT_COLOR)
                .scale(DEFAULT_TEXT_SCALE)
                .glowing(DEFAULT_TEXT_GLOWING)
                .build();
    }

    /**
     * 返回关联的 BlockEntityType，复用 TestSignBlock 的实体类型。
     *
     * @return TestSignBlock.ENTITY_TYPE
     */
    @Override
    public BlockEntityType<? extends TextBlockEntity> getBlockEntityType() {
        return ChineseCoreBuildingEntities.TEXT_BLOCK_ENTITY_TYPE;
    }

    /**
     * 标识此方块为单行编辑模式。
     * <p>
     * GUI 层通过此方法判断是否显示简化的单行编辑界面。
     * 返回 true 时，GUI 只显示单行文字编辑，隐藏列表和添加/删除按钮。
     * </p>
     *
     * @return true 表示单行模式
     */
    public boolean isSingleLineMode() {
        return true;
    }

    /**
     * 返回此方块对应的 GUI 类型标识符。
     * <p>
     * 路标牌的文字位置由方块自身固定（见 {@link #createBlockEntity}），
     * 玩家无需也不应手动调整，因此使用 {@link GuiInteractive#GUI_TYPE_SIMPLE}：
     * 客户端据此创建 {@link com.chinesecorebuilding.client.gui.SimpleTextScreen}，
     * 只保留文字内容、颜色、缩放、发光四项编辑能力，隐藏 XYZ 位置输入框。
     * </p>
     *
     * @return {@link GuiInteractive#GUI_TYPE_SIMPLE}
     */
    @Override
    public String getGuiType() {
        return GUI_TYPE_SIMPLE;
    }
}