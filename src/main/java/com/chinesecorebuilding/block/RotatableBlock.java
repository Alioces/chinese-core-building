package com.chinesecorebuilding.block;

import com.chinesecorebuilding.block.properties.model.Rotatable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;

/**
 * 可旋转方块基类。
 * <p>
 * 通过实现 {@link Rotatable} 接口获得 16 角度精细旋转能力（每档 22.5°），
 * 放置时由 {@link Rotatable#onPlace} 自动根据玩家朝向设置旋转角度。
 * </p>
 * <p>
 * 与 {@link com.chinesecorebuilding.block.roadSigns.RotatableSignBlock} 的区别：
 * 本类位于通用包下，可作为更通用的旋转方块基类扩展使用。
 * </p>
 *
 * @see Rotatable
 */
public class RotatableBlock extends CustomBlock implements Rotatable {

    /**
     * 构造函数。
     *
     * @param settings 方块属性配置
     */
    public RotatableBlock(Settings settings) {
        super(settings);
        setDefaultState(initRotation(getDefaultState()));
    }

    /**
     * 向状态管理器注册旋转属性。
     *
     * @param builder 状态管理器构建器
     */
    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        Rotatable.super.appendProperties(builder);
    }
}