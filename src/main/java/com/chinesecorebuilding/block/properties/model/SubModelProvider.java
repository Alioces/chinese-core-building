package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.util.AnchorPoint;
import com.chinesecorebuilding.util.handler.SubModelHandler;

import java.util.List;

/**
 * 子模型提供者接口。
 * <p>
 * 实现此接口的方块可以提供自定义子模型加载逻辑，
 * 在模型烘焙阶段根据锚点信息和方块状态动态加载子模型，
 * 并应用锚点定义的变换（位置、旋转、缩放）。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * public class WineCabinetBlock extends CustomBlock implements SubModelProvider {
 *     private static final List&lt;AnchorPoint&gt; ANCHORS = List.of(
 *         AnchorPoint.BOTTLE_SLOT_1,
 *         AnchorPoint.BOTTLE_SLOT_2,
 *         AnchorPoint.BOTTLE_SLOT_3
 *     );
 *
 *     private final SubModelHandler handler = (anchor, state) -> switch (anchor.name()) {
 *         case "bottle_slot_1" -> "block/oak_planks";
 *         case "bottle_slot_2" -> "block/spruce_planks";
 *         default -> null;
 *     };
 *
 *     &#64;Override
 *     public List&lt;AnchorPoint&gt; getAnchors() {
 *         return ANCHORS;
 *     }
 *
 *     &#64;Override
 *     public SubModelHandler getSubModelHandler() {
 *         return handler;
 *     }
 * }
 * </pre>
 *
 * @see AnchorPoint
 * @see SubModelHandler
 * @see ModelBakeDecorator
 */
public interface SubModelProvider extends ModelBakeDecorator {

    /**
     * 获取锚点列表。
     * <p>
     * 返回的锚点列表定义了子模型的放置位置和变换属性。
     * 每个锚点包含名称、位置、旋转和缩放信息。
     * </p>
     *
     * @return 锚点列表
     */
    List<AnchorPoint> getAnchors();

    /**
     * 获取子模型处理器。
     * <p>
     * 返回的子模型处理器会在模型烘焙阶段对每个锚点调用，
     * 用于确定该锚点位置应该加载哪个子模型。
     * </p>
     *
     * @return 子模型处理器实例
     */
    SubModelHandler getSubModelHandler();
}