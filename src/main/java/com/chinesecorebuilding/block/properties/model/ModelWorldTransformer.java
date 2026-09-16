package com.chinesecorebuilding.block.properties.model;

/**
 * 模型世界坐标最终处理阶段标识接口。
 * <p>
 * 标记实现类需要在模型合成完成后，对<b>所有顶点</b>（模型本体 + 装饰层追加的顶点）
 * 应用世界坐标变换，如旋转、偏移、镜像等。
 * </p>
 * <p>
 * 属于此阶段的能力接口：
 * <ul>
 *     <li>{@link Rotatable} / {@link Directional} — 绕 Y 轴旋转</li>
 *     <li>{@link Offset} — 3D 空间平移</li>
 * </ul>
 * </p>
 * <p>
 * <b>执行顺序保证：</b>ModelPluginRegistry 会先处理
 * {@link ModelBakeDecorator} 阶段（往模型里加东西），
 * 再处理 ModelWorldTransformer 阶段（对所有顶点做变换）。
 * 因此，装饰层（如文字）产生的顶点会自动被旋转/偏移，
 * 无需在 BlockEntityRenderer 里手动重复一遍变换逻辑。
 * </p>
 *
 * @see ModelBakeDecorator
 * @see Rotatable
 * @see Directional
 * @see Offset
 */
public interface ModelWorldTransformer {
}