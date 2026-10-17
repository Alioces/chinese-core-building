package com.chinesecorebuilding.block.roadSigns;

import com.chinesecorebuilding.block.SingleLineSignBlock;
import com.chinesecorebuilding.block.TestSignBlock;
import com.chinesecorebuilding.block.entity.TextBlockEntity;
import com.chinesecorebuilding.block.properties.GuiInteractive;
import com.chinesecorebuilding.block.properties.model.SignTextProvider;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

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
    public SingleLineRoadSignBlock(Settings settings, com.chinesecorebuilding.util.OffsetFunction offset) {
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
     * 创建 TextBlockEntity 实例，复用 TestSignBlock 的实体类型。
     *
     * @param pos   方块位置
     * @param state 方块状态
     * @return 新的 TextBlockEntity
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TextBlockEntity(TestSignBlock.ENTITY_TYPE, pos, state);
    }

    /**
     * 返回关联的 BlockEntityType，复用 TestSignBlock 的实体类型。
     *
     * @return TestSignBlock.ENTITY_TYPE
     */
    @Override
    public BlockEntityType<? extends TextBlockEntity> getBlockEntityType() {
        return TestSignBlock.ENTITY_TYPE;
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
     * 单行文字路标牌返回 {@link GuiInteractive#GUI_TYPE_SINGLE_LINE}，
     * 客户端据此创建 {@link com.chinesecorebuilding.client.gui.SingleLineScreen}。
     * </p>
     *
     * @return {@link GuiInteractive#GUI_TYPE_SINGLE_LINE}
     */
    @Override
    public String getGuiType() {
        return GUI_TYPE_SINGLE_LINE;
    }
}