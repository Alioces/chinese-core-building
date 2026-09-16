package com.chinesecorebuilding.block.properties.model;

import com.chinesecorebuilding.block.entity.SignBlockEntity;
import com.chinesecorebuilding.block.entity.TextLine;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * 文字层能力接口。
 * <p>
 * 标识方块需要在渲染时叠加可自定义文字层。
 * 实现类通常同时实现 {@link BlockEntityProvider}。
 * </p>
 *
 * <h3>能力接口体系</h3>
 * <pre>
 * Rotatable              → 声明旋转能力   → RotationBakedModel 读取
 *   └─ Directional       → 特化为 4 方向  → RotationBakedModel 读取
 * Offset                 → 声明偏移能力   → OffsetBakedModel 读取
 * Layered                → 声明渲染层类型 → 渲染管线读取
 * <b>SignTextProvider</b> → 声明文字层     → SignBlockRenderer 读取
 * </pre>
 *
 * <h3>扩展方式（开闭原则）</h3>
 * <p>
 * 新增一种"渲染层"能力 = 新增一个接口 + 新增一个渲染处理器，
 * 不修改既有接口和处理器的逻辑。
 * </p>
 */
public interface SignTextProvider extends ModelBakeDecorator {

    /**
     * 获取此类方块使用的 BlockEntityType。
     * <p>
     * 实现类需返回与自己注册时一致的 BlockEntityType，
     * 供 {@link #getSignEntity} 进行类型校验，避免跨方块类型取错实体。
     * </p>
     *
     * @return 此类方块专属的 BlockEntityType（泛型上界为 SignBlockEntity）
     */
    BlockEntityType<? extends SignBlockEntity> getBlockEntityType();

    /**
     * 在指定世界和位置查找此类型的 BlockEntity。
     *
     * @param world 世界对象
     * @param pos   方块位置
     * @return 匹配的 SignBlockEntity，或 null（位置为空、类型不符、世界为 null）
     */
    default SignBlockEntity getSignEntity(World world, BlockPos pos) {
        if (world == null || pos == null) return null;
        var entity = world.getBlockEntity(pos);
        if (entity != null && entity.getType() == getBlockEntityType()) {
            return (SignBlockEntity) entity;
        }
        return null;
    }

    /**
     * 获取指定位置 SignBlockEntity 的所有文字行。
     *
     * @param world 世界对象
     * @param pos   方块位置
     * @return 文字行不可变列表；方块不存在时返回 {@link List#of()}（空列表）
     */
    default List<TextLine> getTextLines(World world, BlockPos pos) {
        SignBlockEntity e = getSignEntity(world, pos);
        return e != null ? e.getLines() : List.of();
    }

    /**
     * 获取指定位置 SignBlockEntity 的文字行数量。
     *
     * @param world 世界对象
     * @param pos   方块位置
     * @return 文字行数量；方块不存在时返回 0
     */
    default int getTextLineCount(World world, BlockPos pos) {
        SignBlockEntity e = getSignEntity(world, pos);
        return e != null ? e.getLineCount() : 0;
    }
}