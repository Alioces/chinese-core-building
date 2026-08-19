package com.chinesecorebuilding.client.model.postProcessing;


import com.chinesecorebuilding.block.properties.Offset;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.function.Supplier;

/**
 * 模型渲染偏移包装器。
 * <p>
 * 继承 Fabric 的 {@link ForwardingBakedModel}，包装原始 {@link BakedModel} 并在渲染时
 * 对所有面片应用位置偏移。偏移量由方块实现的 {@link Offset} 接口定义，
 * 本类不硬编码任何偏移映射，遵循开闭原则。
 * </p>
 * <p>
 * 工作原理：
 * <ol>
 *     <li>在模型烘焙后通过 {@link com.chinesecorebuilding.client.ChineseCoreBuildingClient} 包装原始模型</li>
 *     <li>渲染时拦截 {@link #emitBlockQuads} 调用</li>
 *     <li>通过 {@link RenderContext#pushTransform} 注入顶点位移变换</li>
 *     <li>通过 {@link Offset#getOffset} 获取方块定义的偏移量</li>
 * </ol>
 * </p>
 *
 * @see ForwardingBakedModel
 * @see com.chinesecorebuilding.client.ChineseCoreBuildingClient
 */
public class OffsetBakedModel extends ForwardingBakedModel {

    /**
     * 构造函数。
     * <p>
     * 将原始模型保存到 {@link ForwardingBakedModel#wrapped} 字段，
     * 所有未重写的方法会自动代理到原始模型。
     * </p>
     *
     * @param original 原始烘焙模型（由 Minecraft 从 JSON 模型文件烘焙而来）
     */
    public OffsetBakedModel(BakedModel original) {
        this.wrapped = original;
    }

    /**
     * Fabric 渲染管线的面片发射方法。
     * <p>
     * 这是 Fabric Indigo 渲染器的核心回调，在渲染方块时被调用。
     * 通过 {@link RenderContext#pushTransform} 注入顶点位移变换，
     * 使所有面片在渲染时自动偏移。
     * </p>
     * <p>
     * 偏移量由方块实现的 {@link Offset} 接口动态计算，
     * 本类仅负责应用偏移变换。
     * </p>
     *
     * @param blockView      世界视图，用于获取邻居方块等信息
     * @param state          当前方块状态（包含 FACING 属性）
     * @param pos            方块在世界中的位置
     * @param randomSupplier 随机数生成器（用于模型变体等）
     * @param context        渲染上下文，用于注入变换
     */
    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        float dx = 0, dz = 0;

        if (state != null && blockView.getBlockState(pos).getBlock() instanceof Offset offsetable) {
            float[] offset = offsetable.getOffset(state);
            dx = offset[0];
            dz = offset[1];
        }

        final float fdx = dx, fdz = dz;

        // 推入顶点变换：对每个面片的 4 个顶点应用偏移
        context.pushTransform(quad -> {
            // 遍历面片的 4 个顶点，逐个偏移位置
            for (int i = 0; i < 4; i++) {
                // 读取当前顶点坐标，加上偏移量后写回
                // quad.pos(index, x, y, z) 设置指定顶点的位置
                // quad.x(i) / quad.z(i) 读取指定顶点的 X / Z 坐标
                quad.pos(i, quad.x(i) + fdx, quad.y(i), quad.z(i) + fdz);
            }
            // 返回 true 表示保留该面片（false 会丢弃）
            return true;
        });

        // 调用父类方法，触发原始模型的面片发射
        // 发射的面片会经过上面注册的变换，自动应用偏移
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);

        // 弹出变换，恢复渲染上下文状态
        context.popTransform();
    }

    /**
     * 原版渲染管线的面片获取方法。
     * <p>
     * 直接代理到原始模型，不做偏移处理。
     * 此方法主要用于粒子效果、物品栏渲染等场景，
     * 方块在世界中的渲染由 {@link #emitBlockQuads} 处理。
     * </p>
     *
     * @param state 方块状态（可能为 null）
     * @param face  请求的面方向（可能为 null）
     * @param random 随机数生成器
     * @return 原始模型的面片列表（未偏移）
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        // 直接返回原始模型的面片，不做处理
        return wrapped.getQuads(state, face, random);
    }
}