package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.block.properties.model.Offset;
import com.chinesecorebuilding.client.renderer.pipeline.decorator.OffsetDecorator;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;

/**
 * 模型渲染偏移包装器。
 * <p>
 * 继承 {@link DecoratorBakedModel}，委托给 {@link OffsetDecorator} 执行偏移变换，
 * 消除与 BlockEntityRenderer 管线的重复逻辑。
 * 偏移量由方块实现的 {@link Offset} 接口定义，本类不硬编码任何偏移映射，遵循开闭原则。
 * </p>
 *
 * @see DecoratorBakedModel
 * @see Offset
 * @see OffsetDecorator
 */
public class OffsetBakedModel extends DecoratorBakedModel<OffsetDecorator> {

    /**
     * 偏移装饰器实例（单例，共享变换逻辑）。
     */
    private static final OffsetDecorator DECORATOR = new OffsetDecorator();

    /**
     * 构造函数。
     *
     * @param original 原始烘焙模型（由 Minecraft 从 JSON 模型文件烘焙而来）
     */
    public OffsetBakedModel(BakedModel original) {
        super(original, DECORATOR, state -> state.getBlock() instanceof Offset);
    }
}