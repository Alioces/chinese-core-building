package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.block.properties.model.SignTextProvider;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.Block;
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
 * 文字层装饰包装器——装饰阶段（ModelBakeDecorator）。
 * <p>
 * 标识实现了 {@link SignTextProvider} 的方块需要文字层渲染能力。
 * 作为 ModelPluginRegistry {@code DECORATOR} 阶段的插件链入口，
 * 包装在变换阶段（RotationBakedModel / OffsetBakedModel）的<b>内侧</b>。
 * </p>
 * <p>
 * <b>执行顺序（插件链从外向内）：</b>
 * <pre>
 * OffsetBakedModel         ← 最外层，最后 emitBlockQuads
 *   → RotationBakedModel
 *     → TextBakedModel     ← 最内层，最先 emitBlockQuads
 *       → 原始模型
 * </pre>
 * RenderContext.pushTransform 的作用方向是内向外，
 * 因此原始模型的面片先被 pushTransform，然后是 TextBakedModel 追加的顶点，
 * 最后由 Rotation/Offset 统一变换。
 * </p>
 * <p>
 * <b>关于文字顶点的合成策略：</b>
 * TextLine 数据是 BlockEntity 级别（每个方块实体独有），不是 BlockState 级别，
 * 因此无法在 BakedModel 静态烘焙阶段注入（BakedModel 是全局共享缓存的）。
 * 当前版本文字渲染仍由 {@code SignBlockRenderer}（BlockEntityRenderer 管线）负责，
 * 但 SignBlockRenderer 必须与 RotationBakedModel / OffsetBakedModel
 * 使用<b>完全一致的旋转中心和变换公式</b>，保证文字与模型同步旋转偏移。
 * </p>
 * <p>
 * <b>架构演进方向：</b>
 * 当 Fabric 渲染器 API 支持动态贴图注入 RenderContext 时，
 * TextBakedModel 可升级为在 emitBlockQuads 运行时动态渲染文字到 FrameBuffer，
 * 然后生成带 UV 的 BakedQuad 注入 RenderContext。届时 SignBlockRenderer 可删除，
 * 文字真正进入 BakedModel 管线，与模型本体共享同一个 Draw call。
 * </p>
 *
 * @see ForwardingBakedModel
 * @see SignTextProvider
 * @see RotationBakedModel
 * @see OffsetBakedModel
 */
public class TextBakedModel extends ForwardingBakedModel {

    /**
     * 构造函数。
     *
     * @param original 原始烘焙模型（通常来自 JSON 静态数据）
     */
    public TextBakedModel(BakedModel original) {
        this.wrapped = original;
    }

    /**
     * Fabric 渲染管线的面片发射方法。
     * <p>
     * 当前版本仅代理原始模型——文字层顶点暂由 BlockEntityRenderer 管线负责。
     * 插件链的装饰阶段顺序保证了 TextBakedModel 在 Rotation/Offset 内侧，
     * 使 BlockEntityRenderer 的文字变换能与 BakedModel 管线的变换同步。
     * </p>
     * <p>
     * 未来升级：在此处调用 FrameBuffer 渲染 TextLine 文字到贴图，
     * 然后通过 RenderContext 注入带 UV 的 BakedQuad 实现真正的合成。
     * </p>
     */
    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    /**
     * 原版渲染管线的面片获取方法。
     * <p>
     * 直接代理到原始模型——物品栏、粒子等场景不需要文字层。
     * </p>
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        return wrapped.getQuads(state, face, random);
    }
}