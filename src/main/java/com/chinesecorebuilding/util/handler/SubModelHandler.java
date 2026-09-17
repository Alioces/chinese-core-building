package com.chinesecorebuilding.util.handler;

import com.chinesecorebuilding.util.AnchorPoint;
import net.minecraft.block.BlockState;

/**
 * 子模型提供者处理器。
 * <p>
 * 函数式接口，用于向烘焙插件链提交自定义子模型加载逻辑。
 * 实现此接口的 Lambda 或方法引用会在模型烘焙阶段调用，
 * 用于根据锚点信息和方块状态返回对应的子模型 ID。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * SubModelHandler handler = (anchor, state) -> switch (anchor.name()) {
 *     case "bottle_slot_1" -> "block/oak_planks";
 *     case "bottle_slot_2" -> "block/spruce_planks";
 *     default -> null;
 * };
 * </pre>
 *
 * @see AnchorPoint
 * @see SubModelProvider
 */
@FunctionalInterface
public interface SubModelHandler {

    /**
     * 根据锚点信息和方块状态返回子模型 ID。
     * <p>
     * 此方法在模型烘焙阶段调用，用于确定每个锚点位置
     * 应该加载哪个子模型。返回 null 表示该锚点不加载子模型。
     * </p>
     *
     * @param anchor 锚点信息，包含名称、位置、旋转和缩放
     * @param state  当前方块状态，可用于根据属性调整子模型选择
     * @return 子模型 ID（资源路径，如 "block/oak_planks"），null 表示不加载
     */
    String handle(AnchorPoint anchor, BlockState state);
}