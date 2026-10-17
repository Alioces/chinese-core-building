package com.chinesecorebuilding.client.renderer.pipeline.context;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.function.Supplier;

/**
 * BakedModel 渲染上下文。
 * <p>
 * 封装 Fabric 渲染管线中 emitBlockQuads 所需的全部参数。
 * </p>
 *
 * @param blockView      世界视图，用于获取邻居方块等信息
 * @param state          当前方块状态（包含 ROTATION 或 FACING 属性）
 * @param pos            方块在世界中的位置
 * @param randomSupplier 随机数生成器（用于模型变体等）
 * @param context        Fabric RenderContext，用于注入变换
 */
public record BakedModelContext(
        BlockRenderView blockView,
        BlockState state,
        BlockPos pos,
        Supplier<Random> randomSupplier,
        RenderContext context
) {
}