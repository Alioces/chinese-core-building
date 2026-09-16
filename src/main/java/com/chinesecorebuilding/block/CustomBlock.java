package com.chinesecorebuilding.block;

import com.chinesecorebuilding.block.properties.Interactive;
import com.chinesecorebuilding.block.roadSigns.RoadSignsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 自定义方块抽象基类。
 * <p>
 * 继承自 Minecraft 原版 {@link Block}，作为本模组所有自定义方块的父类。
 * 提供统一的构造入口和属性扩展机制，子类可在此基础上添加特定属性（如朝向）。
 * </p>
 * <p>
 * 自动调度服务端交互：覆盖 {@link #onUse}，若当前方块实现了 {@link Interactive}
 * 且返回了非 null 的 {@link com.chinesecorebuilding.block.properties.BlockInteraction}，
 * 则委托执行。否则走原版默认逻辑。
 * </p>
 */
public class CustomBlock extends Block {

    public CustomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (this instanceof Interactive interactive) {
            var handler = interactive.getInteraction();
            if (handler != null) {
                return handler.interact(state, world, pos, player, hand, hit);
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }
}