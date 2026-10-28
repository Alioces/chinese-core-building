package com.chinesecorebuilding.client.renderer;

import com.chinesecorebuilding.client.renderer.pipeline.BlockEntityRendererRegistry;

/**
 * 渲染器自注册汇总入口。
 * <p>
 * 通过枚举常量持有的 {@code Class<?>} 引用强制触发各渲染器类的加载，
 * 从而执行其静态块中的 {@link BlockEntityRendererRegistry#register} 调用。
 * </p>
 * <p>
 * 使用 JVM 类加载机制替代脆弱的 {@code Class.forName()} 字符串方式，
 * 编译期即可检测渲染器类的存在，且不依赖任何反射。
 * </p>
 * <p>
 * <b>扩展方式（遵循开闭原则）：</b>
 * 新增渲染器时，只需在枚举中增加一个常量并传入渲染器的 Class 对象，
 * 无需修改客户端初始化代码或 {@code ChineseCoreBuildingClient}。
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
     * 枚举汇总所有渲染器类，通过 {@code Class<?>} 引用强制触发类加载。
     * <p>
     * 每个枚举常量持有的 {@code Class<?>} 引用会在枚举类被加载时触发
     * 目标渲染器类的初始化，包括其静态块的执行。
     * </p>
     */
    private enum Registry {
        /** 文字渲染器：负责在方块表面绘制可编辑文字。 */
        TEXT(TextBlockRenderer.class);

        /**
         * 渲染器类引用，触发类加载时执行静态块。
         */
        private final Class<?> rendererClass;

        /**
         * 构造函数。
         *
         * @param rendererClass 渲染器类的 Class 对象
         */
        Registry(Class<?> rendererClass) {
            this.rendererClass = rendererClass;
        }
    }

    /**
     * 私有构造函数，防止实例化。
     */
    private Renderers() {}

    /**
     * 初始化所有渲染器。
     * <p>
     * 通过访问枚举常量触发各渲染器类的加载和静态块执行，
     * 各渲染器类的静态块会向 {@link BlockEntityRendererRegistry} 注册工厂。
     * </p>
     * <p>
     * 调用时机：在 {@code ChineseCoreBuildingClient.onInitializeClient()} 中，
     * 在 {@link com.chinesecorebuilding.client.renderer.pipeline.UnifiedRenderPipelineRegistry#initialize()}
     * 之后、{@code registerBlockEntityRenderers()} 之前调用。
     * </p>
     */
    public static void initRenderers() {
        @SuppressWarnings("unused")
        Class<?> trigger = Registry.TEXT.rendererClass;
    }
}