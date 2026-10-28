package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.client.renderer.pipeline.BlockEntityRendererRegistry;

/**
 * 渲染器自注册汇总入口。
 * <p>
 * 直接向 {@link BlockEntityRendererRegistry} 注册各渲染器工厂，
 * 避免间接类加载机制（类字面量/反射）导致的 JVM 类初始化时序问题。
 * </p>
 * <p>
 * <b>扩展方式（遵循开闭原则）：</b>
 * 新增渲染器时，在 {@link #initRenderers()} 中增加一行
 * {@code BlockEntityRendererRegistry.register("id", XxxRenderer::new)}。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * // 客户端初始化时调用一次
 * Renderers.initRenderers();
 * </pre>
 * </p>
 *
 * @see BlockEntityRendererRegistry
 * @see TextBlockRenderer
 */
public final class Renderers {

    /**
     * 私有构造函数，防止实例化。
     */
    private Renderers() {}

    /**
     * 初始化所有渲染器。
     * <p>
     * 直接向 {@link BlockEntityRendererRegistry} 注册工厂函数，
     * 后续 {@code registerBlockEntityRenderers()} 会遍历所有
     * {@code SignTextProvider} 方块并通过注册的工厂创建 BER 实例。
     * </p>
     * <p>
     * 调用时机：在 {@code ChineseCoreBuildingClient.onInitializeClient()} 中，
     * 在 {@link com.chinesecorebuilding.client.renderer.pipeline.UnifiedRenderPipelineRegistry#initialize()}
     * 之后、{@code registerBlockEntityRenderers()} 之前调用。
     * </p>
     */
    public static void initRenderers() {
        // 直接注册工厂，不依赖 static 块自注册机制
        // （类字面量不触发 JVM 类初始化，static 块中的注册逻辑从未执行）
        BlockEntityRendererRegistry.register("text", TextBlockRenderer::new);
    }
}