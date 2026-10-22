package com.chinesecorebuilding.client.renderer;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * BlockEntityRenderer 工厂注册中心。
 * <p>
 * 渲染器类通过静态块向此注册中心提交自己的工厂函数，
 * 客户端初始化时统一注册到 Fabric 管线。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * // 渲染器类中声明（client 源集）
 * static {
 *     BlockEntityRendererRegistry.register("text", TextBlockRenderer::new);
 * }
 * 
 * // 客户端初始化时统一注册
 * BlockEntityRendererRegistry.registerAll();
 * </pre>
 * </p>
 */
public final class BlockEntityRendererRegistry {

    /**
     * 渲染器类型标识 → 工厂函数的映射。
     * <p>
     * 使用原始类型避免泛型推断问题。
     * </p>
     */
    @SuppressWarnings("rawtypes")
    private static final Map<String, BlockEntityRendererFactory> FACTORIES = new HashMap<>();

    private BlockEntityRendererRegistry() {}

    /**
     * 注册 BlockEntityRenderer 工厂。
     *
     * @param typeId  渲染器类型标识
     * @param factory 渲染器工厂函数
     */
    @SuppressWarnings({"rawtypes"})
    public static void register(String typeId, BlockEntityRendererFactory factory) {
        FACTORIES.put(typeId, factory);
    }

    /**
     * 获取所有已注册的渲染器工厂。
     *
     * @return 不可变映射
     */
    @SuppressWarnings("rawtypes")
    public static Map<String, BlockEntityRendererFactory> getFactories() {
        return Map.copyOf(FACTORIES);
    }
}