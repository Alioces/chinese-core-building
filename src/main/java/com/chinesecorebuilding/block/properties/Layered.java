package com.chinesecorebuilding.block.properties;

/**
 * 可声明渲染层的方块接口。
 * <p>
 * 实现此接口的方块会向客户端声明自己需要的渲染层类型，
 * 客户端自动扫描并注册到 {@code BlockRenderLayerMap}，
 * 无需在客户端代码中手动逐个设置。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * public class GlassBlock extends CustomBlock implements LayeredBlock {
 *     &#64;Override
 *     public RenderLayerType getRenderLayerType() {
 *         return RenderLayerType.CUTOUT;
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see RenderLayerType
 */
public interface Layered {

    /**
     * 声明此方块所需的渲染层类型。
     * <p>
     * 客户端会自动读取此值并映射到对应的渲染层。
     * </p>
     *
     * @return 渲染层类型
     */
    default RenderLayerType getRenderLayerType(){
        return RenderLayerType.SOLID;
    };
}