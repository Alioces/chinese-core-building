package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.client.renderer.pipeline.api.BakedModelDecorator;
import com.chinesecorebuilding.client.renderer.pipeline.context.BakedModelContext;
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
 * 装饰器 BakedModel 公共基类。
 * <p>
 * 消除 OffsetBakedModel 和 RotationBakedModel 的重复代码。
 * 提供统一的装饰器委托逻辑，子类仅需提供装饰器实例和条件判断。
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * public class OffsetBakedModel extends DecoratorBakedModel {
 *     public OffsetBakedModel(BakedModel original) {
 *         super(original, new OffsetDecorator(), state -> state.getBlock() instanceof Offset);
 *     }
 * }
 * </pre>
 * </p>
 *
 * @param <D> 装饰器类型
 */
public abstract class DecoratorBakedModel<D extends BakedModelDecorator<Void>> extends ForwardingBakedModel {

    /**
     * 装饰器实例（单例，共享变换逻辑）。
     */
    private final D decorator;

    /**
     * 条件判断函数（判断是否应用装饰器）。
     */
    private final java.util.function.Predicate<BlockState> condition;

    /**
     * 构造函数。
     *
     * @param original  原始烘焙模型
     * @param decorator 装饰器实例
     * @param condition 条件判断函数
     */
    protected DecoratorBakedModel(BakedModel original, D decorator,
                                  java.util.function.Predicate<BlockState> condition) {
        this.wrapped = original;
        this.decorator = decorator;
        this.condition = condition;
    }

    /**
     * Fabric 渲染管线的面片发射方法。
     * <p>
     * 委托给装饰器执行变换。
     * </p>
     *
     * @param blockView      世界视图
     * @param state          当前方块状态
     * @param pos            方块位置
     * @param randomSupplier 随机数生成器
     * @param context        渲染上下文
     */
    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        if (state != null && condition.test(blockView.getBlockState(pos))) {
            var ctx = new BakedModelContext(blockView, state, pos, randomSupplier, context);
            decorator.decorate(ctx, c -> {
                super.emitBlockQuads(c.blockView(), c.state(), c.pos(), c.randomSupplier(), c.context());
                return null;
            });
        } else {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }

    /**
     * 标记此模型为非原版适配器。
     * <p>
     * 告知渲染管线（如 Sodium）此模型的渲染必须通过
     * {@link #emitBlockQuads} 走 FRAPI 路径，
     * 因为 {@link #getQuads} 返回的顶点是不完整的——
     * 缺少旋转、偏移、文字层、子模型等变换结果。
     * </p>
     * <p>
     * <b>Sodium 兼容性</b>：Sodium 通过 {@code isVanillaAdapter()}
     * 判断是调用 {@code getQuads()} 还是 {@code emitBlockQuads()}。
     * 返回 {@code false} 强制 Sodium 走 FRAPI 路径，
     * 确保 {@link #emitBlockQuads} 中的 pushTransform 变换被正确应用。
     * </p>
     *
     * @return false，强制走 FRAPI 渲染路径
     */
    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    /**
     * 原版渲染管线的面片获取方法。
     * <p>
     * 直接代理到原始模型，不做变换处理。
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