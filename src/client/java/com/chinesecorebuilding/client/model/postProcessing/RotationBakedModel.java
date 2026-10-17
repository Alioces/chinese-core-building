package com.chinesecorebuilding.client.model.postProcessing;

import com.chinesecorebuilding.block.properties.model.Rotatable;
import com.chinesecorebuilding.client.renderer.pipeline.decorator.RotationDecorator;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;

/**
 * 模型渲染旋转变换包装器。
 * <p>
 * 继承 {@link DecoratorBakedModel}，委托给 {@link RotationDecorator} 执行旋转变换，
 * 消除与 BlockEntityRenderer 管线的重复逻辑。
 * 旋转角度由方块实现的 {@link Rotatable} 接口通过 {@link Rotatable#getRotationAngle} 计算，
 * 本类不硬编码任何旋转映射，遵循开闭原则。
 * </p>
 *
 * @see DecoratorBakedModel
 * @see Rotatable
 * @see RotationDecorator
 */
public class RotationBakedModel extends DecoratorBakedModel<RotationDecorator> {

    /**
     * 旋转装饰器实例（单例，共享变换逻辑）。
     */
    private static final RotationDecorator DECORATOR = new RotationDecorator();

    /**
     * 构造函数。
     *
     * @param original 原始烘焙模型
     */
    public RotationBakedModel(BakedModel original) {
        super(original, DECORATOR, state -> state.getBlock() instanceof Rotatable);
    }
}