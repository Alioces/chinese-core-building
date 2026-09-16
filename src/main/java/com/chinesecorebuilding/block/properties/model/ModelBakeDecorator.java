package com.chinesecorebuilding.block.properties.model;

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
}