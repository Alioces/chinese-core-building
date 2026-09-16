package com.chinesecorebuilding.block.properties;

/**
 * 方块交互能力接口。
 * <p>
 * 实现此接口的方块声明自己拥有交互能力。
 * 默认提供三层能力：
 * <ol>
 *   <li>服务端交互处理器 ({@link #getInteraction}) —— 自定义服务端行为</li>
 *   <li>客户端 GUI 标记 ({@link #shouldOpenGui}) —— 声明需要弹出编辑界面</li>
 *   <li>默认行为阻断 ({@link #blocksDefaultInteraction}) —— 声明是否阻止原版右键默认行为</li>
 * </ol>
 * </p>
 * <p>
 * CustomBlock.onUse 的处理优先级：
 * <pre>
 * 如果 getInteraction() != null → 委托执行，handler 自己决定 ActionResult
 * 如果 blocksDefaultInteraction() == true → 返回 CONSUME（阻断放置、容器打开等原版行为）
 * 否则 → super.onUse()（走原版逻辑，通常返回 PASS 让放置继续）
 * </pre>
 * </p>
 */
public interface Interactive {

    /**
     * 返回服务端右键交互处理器，默认返回 null 表示无服务端逻辑。
     * <p>
     * CustomBlock.onUse 会自动检查此方法，若返回非 null 则委托执行，
     * handler 的 ActionResult 直接成为 onUse 的返回值。
     * </p>
     *
     * @return 服务端交互处理器，或 null
     */
    default BlockInteraction getInteraction() {
        return null;
    }

    /**
     * 标记此方块是否需要客户端打开编辑 GUI。
     * <p>
     * ChineseCoreBuildingClient 会监听 Fabric 的 UseBlockCallback，
     * 若 {@code shouldOpenGui()} 返回 true 且方块实现了 Interactive，
     * 则自动打开 SignBlockScreen 编辑器。
     * </p>
     *
     * @return true 表示右键时应该打开 GUI
     */
    default boolean shouldOpenGui() {
        return false;
    }

    /**
     * 是否阻断原版右键默认行为。
     * <p>
     * 默认返回 true——只要方块声明了 Interactive，就认为它接管了右键，
     * 原版的放置（BlockItem）、容器打开等行为都被阻断。
     * </p>
     * <p>
     * 为什么需要这个？原版 Block.onUse 返回 PASS 时，
     * 持有 BlockItem 的玩家右键会在方块旁边放置新方块。
     * TestSignBlock 没有 getInteraction() 返回值，
     * 但它实现了 Interactive 意味着"我接管了右键"，
     * 所以 CustomBlock 需要返回 CONSUME 告诉原版"我处理了，别再放置"。
     * </p>
     * <p>
     * 某些场景需要关闭：比如一个既有交互又允许放置的方块（罕见），
     * 可以覆盖此方法返回 false。
     * </p>
     *
     * @return true 阻断原版默认行为（放置、容器等），false 放行
     */
    default boolean blocksDefaultInteraction() {
        return true;
    }
}