package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.block.entity.TextBlockEntity;
import net.minecraft.block.entity.BlockEntityType;

/**
 * 模型烘焙修饰阶段标识接口。
 * <p>
 * 标记实现类需要在模型烘焙阶段（BakedModel.emitBlockQuads 运行时）
 * 对模型进行内容合成——即往顶点流里<b>追加新的几何数据</b>，
 * 如文字层、额外面片、装饰性顶点等。
 * </p>
 * <p>
 * 属于此阶段的能力接口：
 * <ul>
 *     <li>{@link SignTextProvider} — 合成文字层顶点</li>
 * </ul>
 * </p>
 * <p>
 * <b>与 ModelWorldTransformer 的区别：</b>
 * {@code ModelBakeDecorator} 是"做加法"——往模型里加东西；
 * {@link ModelWorldTransformer} 是"做变换"——对已有顶点改位置/角度。
 * 执行顺序：先装饰，后变换。变换作用在装饰的结果之上，
 * 因此装饰层产生的顶点（如文字）会自动跟随旋转偏移。
 * </p>
 *
 * @see ModelWorldTransformer
 * @see SignTextProvider
 */
public interface ModelBakeDecorator {
    /**
     * 获取此类方块使用的 BlockEntityType。
     * <p>
     * 默认返回 null，表示此方块不需要 BlockEntity。
     * 需要方块实体的子类接口（如 SignTextProvider）应覆盖此方法。
     * </p>
     *
     * @return 此类方块专属的 BlockEntityType，或 null
     */
    default BlockEntityType<?> getBlockEntityType() {
        return null;
    }

    /**
     * 获取此方块类型需要的 BlockEntityRenderer 类型标识。
     * <p>
     * 默认返回 null，表示不需要 BlockEntityRenderer。
     * 子类接口（如 SignTextProvider）可覆盖此方法提供具体标识。
     * </p>
     * <p>
     * 返回类型为 String 以避免 main 源集引用客户端的
     * BlockEntityRendererFactory 接口，保持编译隔离。
     * 客户端消费时通过标识查找对应的渲染器工厂。
     * </p>
     *
     * @return 渲染器类型标识，或 null
     */
    default String getRendererTypeId() {
        return null;
    }
}