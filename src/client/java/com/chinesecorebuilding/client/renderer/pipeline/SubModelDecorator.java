package com.chinesecorebuilding.client.renderer.pipeline;

import com.chinesecorebuilding.util.AnchorPoint;
import com.chinesecorebuilding.util.BakedModelSpec;
import com.chinesecorebuilding.util.handler.SubModelHandler;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.function.Function;

/**
 * 子模型装饰器实现。
 * <p>
 * 统一处理 BakedModel 管线的子模型渲染。
 * 继承 {@link SubModelRendererBase} 复用公共逻辑，专注装饰器职责。
 * </p>
 */
public class SubModelDecorator extends SubModelRendererBase implements BakedModelDecorator<Void> {

    /**
     * 构造函数（立即求值模式，向后兼容）。
     *
     * @param subModelEntries 子模型规格列表（未解析）
     */
    public SubModelDecorator(List<BakedModelSpec.SubModelEntry> subModelEntries) {
        super(subModelEntries);
    }

    /**
     * 构造函数（延迟求值模式）。
     *
     * @param subModelHandler 子模型处理器
     * @param anchors         锚点列表
     */
    public SubModelDecorator(SubModelHandler subModelHandler, List<AnchorPoint> anchors) {
        super(subModelHandler, anchors);
    }

    @Override
    public UnifiedRenderDecorator.Priority getPriority() {
        return UnifiedRenderDecorator.Priority.SUB_MODEL;
    }

    @Override
    public boolean accepts(BlockState state) {
        return true;
    }

    @Override
    public Void decorate(BakedModelContext context, Function<BakedModelContext, Void> next) {
        var result = next.apply(context);

        if (!initialized) {
            initializeSubModels();
        }

        renderSubModels(context);

        return result;
    }

    /**
     * 渲染子模型。
     *
     * @param context BakedModel 上下文
     */
    private void renderSubModels(BakedModelContext context) {
        List<BakedModelSpec.SubModelEntry> entries = resolveSubModelEntries(context.state());

        for (BakedModelSpec.SubModelEntry entry : entries) {
            BakedModel cachedModel = modelCache.get(entry.modelId());
            if (cachedModel != null) {
                emitSubModelQuads(cachedModel, entry.transform(), context.context());
            } else {
                BakedModel loaded = loadModelWithFallback(entry.modelId());
                if (loaded != null) {
                    modelCache.put(entry.modelId(), loaded);
                    emitSubModelQuads(loaded, entry.transform(), context.context());
                }
            }
        }
    }

    @Override
    protected BakedModel loadModelWithFallback(Identifier modelId) {
        return loadModelViaManager(modelId);
    }

    /**
     * 发射子模型面片到 RenderContext。
     *
     * @param model     子模型
     * @param transform 变换（当前未使用，由外层装饰器处理）
     * @param context   渲染上下文
     */
    private void emitSubModelQuads(BakedModel model, com.chinesecorebuilding.util.Transform transform, RenderContext context) {
        QuadEmitter emitter = context.getEmitter();
        Random random = net.minecraft.util.math.random.Random.create();
        RenderMaterial material = RendererAccess.INSTANCE.getRenderer()
                .materialById(RenderMaterial.MATERIAL_STANDARD);

        for (int nullSide = 0; nullSide <= 6; nullSide++) {
            Direction side = nullSide == 0 ? null : Direction.byId(nullSide - 1);
            for (BakedQuad quad : model.getQuads(null, side, random)) {
                emitter.fromVanilla(quad, material, side).emit();
            }
        }
    }
}