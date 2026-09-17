package com.chinesecorebuilding.block;

import com.chinesecorebuilding.block.properties.RenderLayerType;
import com.chinesecorebuilding.block.properties.model.*;
import com.chinesecorebuilding.util.AnchorPoint;
import com.chinesecorebuilding.util.Transform;
import com.chinesecorebuilding.util.handler.AnimationHandler;
import com.chinesecorebuilding.util.handler.EmissiveHandler;
import com.chinesecorebuilding.util.handler.SubModelHandler;
import com.chinesecorebuilding.util.handler.TextureOverrideHandler;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 酒柜测试方块。
 * <p>
 * 实现了多个能力接口以验证插件链：
 * <ul>
 *   <li>{@link SubModelProvider}：提供酒瓶槽位锚点</li>
 *   <li>{@link StatefulTextureProvider}：根据状态切换贴图</li>
 *   <li>{@link LightEmissive}：根据状态设置发光等级</li>
 *   <li>{@link AnimatableModel}：提供简单旋转动画</li>
 * </ul>
 * </p>
 *
 * <h3>测试目标：</h3>
 * <ol>
 *   <li>验证子模型组合插件（优先级 -1000）</li>
 *   <li>验证动态贴图插件（优先级 -900）</li>
 *   <li>验证动画控制插件（优先级 -500）</li>
 *   <li>验证发光效果插件（优先级 -400）</li>
 * </ol>
 * </p>
 */
public class WineCabinetBlock extends CustomBlock
    implements Directional, SubModelProvider, StatefulTextureProvider, LightEmissive, AnimatableModel, Layered {

    /**
     * 酒柜的 6 个酒瓶槽位锚点。
     * <p>
     * 布局：上排 3 个（Y=0.65）+ 下排 3 个（Y=0.35），
     * 每个子模型缩放 0.3 以适配 1x1 方块内部空间。
     * </p>
     */
    private static final List<AnchorPoint> ANCHORS = List.of(
        new AnchorPoint("bottle_slot_1", new Vec3d(0.25, 0.35, 0), Vec3d.ZERO, 0.2f),
        new AnchorPoint("bottle_slot_2", new Vec3d(0.5,  0.35, 0), Vec3d.ZERO, 0.2f),
        new AnchorPoint("bottle_slot_3", new Vec3d(0.75, 0.35, 0), Vec3d.ZERO, 0.2f),
        new AnchorPoint("bottle_slot_4", new Vec3d(0.25, 0.35, 0.5), Vec3d.ZERO, 0.2f),
        new AnchorPoint("bottle_slot_5", new Vec3d(0.5,  0.35, 0.5), Vec3d.ZERO, 0.2f),
        new AnchorPoint("bottle_slot_6", new Vec3d(0.75, 0.35, 0.5), Vec3d.ZERO, 0.2f)
    );

    /**
     * 创建酒柜方块。
     *
     * @param settings 方块设置
     */
    public WineCabinetBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    /**
     * 创建酒柜方块（使用默认设置）。
     */
    public WineCabinetBlock() {
        this(FabricBlockSettings.create().strength(1.0f));
    }

    // ==================== SubModelProvider 实现 ====================

    /**
     * 获取所有锚点定义。
     *
     * @return 酒瓶槽位锚点列表（6个，分两排排列）
     */
    @Override
    public List<AnchorPoint> getAnchors() {
        return ANCHORS;
    }

    /**
     * 获取子模型处理器。
     *
     * @return 子模型处理器实例
     */
    @Override
    public SubModelHandler getSubModelHandler() {
        return (anchor, state) -> {
            // 根据锚点名称返回对应的子模型 ID（使用橡木木板作为测试子模型）
            // 注意：BakedModelManager.getModel() 使用不带 "block/" 前缀的 Identifier
            return switch (anchor.name()) {
                case "bottle_slot_1", "bottle_slot_2", "bottle_slot_3",
                     "bottle_slot_4", "bottle_slot_5", "bottle_slot_6"
                    -> "minecraft:oak_planks";
                default -> null;
            };
        };
    }

    // ==================== StatefulTextureProvider 实现 ====================

    /**
     * 获取贴图覆盖处理器。
     *
     * @return 贴图覆盖处理器实例
     */
    @Override
    public TextureOverrideHandler getTextureOverrideHandler() {
        return (state) -> {
            Map<String, String> overrides = new HashMap<>();
            // 根据方块朝向切换贴图
            String facing = state.get(Properties.HORIZONTAL_FACING).getName();
            overrides.put("#front", "minecraft:block/" + facing + "_glass");
            return overrides;
        };
    }

    // ==================== LightEmissive 实现 ====================

    /**
     * 获取发光处理器。
     *
     * @return 发光处理器实例
     */
    @Override
    public EmissiveHandler getEmissiveHandler() {
        return (state) -> {
            // 发光等级 10（模拟酒柜内部照明）
            return 10;
        };
    }

    // ==================== AnimatableModel 实现 ====================

    /**
     * 获取动画处理器。
     *
     * @return 动画处理器实例
     */
    @Override
    public AnimationHandler getAnimationHandler() {
        return (time, delta, state) -> {
            // 简单测试：不添加额外动画，仅返回空列表
            // 实际使用时可根据时间计算旋转角度
            return List.of();
        };
    }

    // ==================== 方块属性注册 ====================

    /**
     * 注册方块属性。
     *
     * @param builder 属性构建器
     */
    @Override
    public void appendProperties(net.minecraft.state.StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.HORIZONTAL_FACING);
    }

    /**
     * 声明路标使用 CUTOUT 渲染层，支持板面透明区域。
     *
     * @return {@link RenderLayerType#CUTOUT}
     */
    @Override
    public RenderLayerType getRenderLayerType() {
        return RenderLayerType.CUTOUT;
    }
}