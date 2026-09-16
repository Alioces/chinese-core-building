package com.chinesecorebuilding.block.properties;

import com.chinesecorebuilding.block.properties.model.Layered;

/**
 * 渲染层类型枚举。
 * <p>
 * 定义方块在 Minecraft 中使用的渲染层类型，
 * 客户端根据此枚举自动映射到对应的 {@code RenderLayer}。
 * </p>
 * <ul>
 *     <li>{@link #SOLID} — 固体渲染层，适用于不透明方块（默认）</li>
 *     <li>{@link #CUTOUT} — 镂空渲染层，适用于带透明贴图的方块（如路标、栅栏）</li>
 *     <li>{@link #TRANSLUCENT} — 半透明渲染层，适用于玻璃等半透明方块</li>
 * </ul>
 *
 * @see Layered
 */
public enum RenderLayerType {
    /**
     * 镂空渲染层。
     * <p>
     * 支持完全透明/不透明像素，适用于路标、栅栏等带透明贴图的方块。
     * 对应 Minecraft 的 {@code RenderLayer.getCutout()}。
     * </p>
     */
    CUTOUT,
    /**
     * 半透明渲染层。
     * <p>
     * 支持 Alpha 混合，适用于玻璃等半透明方块。
     * 对应 Minecraft 的 {@code RenderLayer.getTranslucent()}。
     * </p>
     */
    TRANSLUCENT,
    /**
     * 固体渲染层。
     * <p>
     * 完全不透明，适用于石头、泥土等普通方块。
     * 对应 Minecraft 的 {@code RenderLayer.getSolid()}。
     * </p>
     */
    SOLID
}