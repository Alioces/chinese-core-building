package com.chinesecorebuilding.block;

import com.chinesecorebuilding.block.roadSigns.RoadSignsBlock;
import net.minecraft.block.Block;

/**
 * 自定义方块抽象基类。
 * <p>
 * 继承自 Minecraft 原版 {@link Block}，作为本模组所有自定义方块的父类。
 * 提供统一的构造入口和属性扩展机制，子类可在此基础上添加特定属性（如朝向）。
 * </p>
 * <p>
 * 继承层次：
 * <pre>
 * Block (Minecraft)
 *   └── CustomBlock (本类)
 *         └── RoadSignsBlock (路标方块)
 * </pre>
 * </p>
 *
 * @see RoadSignsBlock
 */
public class CustomBlock extends Block {

    /**
     * 构造函数。
     *
     * @param settings 方块属性配置（硬度、爆炸抗性、透明度等），
     *                 通过 {@link net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings} 构建
     */
    public CustomBlock(Settings settings) {
        super(settings);
    }
}
