package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.block.properties.Rotatable;
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
 * 模型渲染旋转变换包装器。
 * <p>
 * 继承 Fabric 的 {@link ForwardingBakedModel}，包装原始 {@link BakedModel} 并在渲染时
 * 对所有面片绕方块中心 Y 轴应用旋转变换。
 * 旋转角度由方块实现的 {@link Rotatable} 接口通过 {@link Rotatable#getRotationAngle} 计算，
 * 本类不硬编码任何旋转映射，遵循开闭原则。
 * 仿照原版头颅方块的旋转机制，在客户端渲染阶段实时完成，
 * blockstate JSON 中只需定义一个变体。
 * </p>
 * <p>
 * 工作原理：
 * <ol>
 *     <li>在模型烘焙后通过 {@link com.chinesecorebuilding.client.ChineseCoreBuildingClient} 包装原始模型</li>
 *     <li>渲染时拦截 {@link #emitBlockQuads} 调用</li>
 *     <li>通过 {@link RenderContext#pushTransform} 注入顶点旋转变换</li>
 *     <li>根据方块的 {@code ROTATION} 属性（0~15）动态计算旋转角度</li>
 * </ol>
 * </p>
 * <p>
 * 本类仅负责旋转变换，不涉及任何位置偏移。
 * 偏移由 {@link OffsetBakedModel} 独立处理。
 * </p>
 *
 * @see ForwardingBakedModel
 * @see Rotatable
 */
public class RotationBakedModel extends ForwardingBakedModel {

    /**
     * 方块中心坐标（旋转中心）。
     * <p>
     * 旋转围绕方块几何中心 (0.5, y, 0.5) 的 Y 轴进行。
     * </p>
     */
    private static final float CENTER = 0.5f;

    /**
     * 构造函数。
     *
     * @param original 原始烘焙模型
     */
    public RotationBakedModel(BakedModel original) {
        this.wrapped = original;
    }

    /**
     * Fabric 渲染管线的面片发射方法。
     * <p>
     * 对每个面片的 4 个顶点绕方块中心 (0.5, y, 0.5) 执行 Y 轴旋转。
     * 通过 {@link Rotatable#getRotationAngle} 获取旋转角度，
     * 具体旋转逻辑由方块实现的接口决定。
     * </p>
     * <p>
     * 旋转公式（绕 Y 轴，顺时针为正方向）：
     * <pre>
     *   x' =  cos(θ) × (x - 0.5) + sin(θ) × (z - 0.5) + 0.5
     *   z' = -sin(θ) × (x - 0.5) + cos(θ) × (z - 0.5) + 0.5
     * </pre>
     * </p>
     *
     * @param blockView      世界视图
     * @param state          当前方块状态（包含 ROTATION 或 FACING 属性）
     * @param pos            方块在世界中的位置
     * @param randomSupplier 随机数生成器
     * @param context        渲染上下文，用于注入变换
     */
    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        float angle = 0;

        if (state != null && blockView.getBlockState(pos).getBlock() instanceof Rotatable rotatable) {
            angle = rotatable.getRotationAngle(state);
        }

        final float cos = (float) Math.cos(angle);
        final float sin = (float) Math.sin(angle);

        context.pushTransform(quad -> {
            for (int i = 0; i < 4; i++) {
                float x = quad.x(i) - CENTER;
                float z = quad.z(i) - CENTER;

                quad.pos(i,
                    cos * x + sin * z + CENTER,
                    quad.y(i),
                    -sin * x + cos * z + CENTER
                );
            }
            return true;
        });

        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        context.popTransform();
    }

    /**
     * 原版渲染管线的面片获取方法。
     * <p>
     * 直接代理到原始模型，不做旋转处理。
     * 用于粒子效果、物品栏渲染等场景。
     * </p>
     *
     * @param state  方块状态
     * @param face   请求的面方向
     * @param random 随机数生成器
     * @return 原始模型的面片列表
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        return wrapped.getQuads(state, face, random);
    }
}