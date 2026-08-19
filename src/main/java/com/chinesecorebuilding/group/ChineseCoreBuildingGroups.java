package com.chinesecorebuilding.group;

import com.chinesecorebuilding.block.ChineseCoreBuildingBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

import static com.chinesecorebuilding.ChineseCoreBuildingMod.MOD_GROUP;
import static com.chinesecorebuilding.ChineseCoreBuildingMod.id;

/**
 * 创造模式物品组注册中心。
 * <p>
 * 负责注册本模组的所有创造模式物品组（ItemGroup），
 * 物品组显示在创造模式物品栏中，包含本模组的所有物品。
 * </p>
 * <p>
 * 初始化时机：由 {@link com.chinesecorebuilding.ChineseCoreBuildingMod#onInitialize()} 调用 {@link #initialize()}。
 * </p>
 */
public class ChineseCoreBuildingGroups {

    /**
     * 路标物品组。
     * <p>
     * 在创造模式物品栏中显示为一个独立标签页，包含所有路标类方块。
     * </p>
     * <ul>
     *     <li>图标：堤坝路标方块</li>
     *     <li>显示名称：翻译键 {@code itemGroup.chinese-core-building.group.road}</li>
     *     <li>内容：所有注册的路标方块物品</li>
     * </ul>
     */
    public static final ItemGroup ROAD_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            id("test_group"),  // 注册 ID：chinese-core-building:test_group
            FabricItemGroup.builder()
                    // 设置物品组图标（显示在标签页上的图标）
                    .icon(() -> new ItemStack(ChineseCoreBuildingBlocks.embankment_road01))
                    // 设置显示名称（通过翻译键从语言文件获取）
                    .displayName(Text.translatable(MOD_GROUP + "road"))
                    // 添加物品组内容
                    .entries((context, entries) -> {
                        // ------ 警告标志 ------
                        entries.add(ChineseCoreBuildingBlocks.beware_of_crosswind.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossbuck_symbol.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads03.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads04.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads05.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads06.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads07.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads08.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads09.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads10.asItem());
                        entries.add(ChineseCoreBuildingBlocks.crossroads11.asItem());
                        entries.add(ChineseCoreBuildingBlocks.embankment_road01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.embankment_road02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.falling_rocks_on_left.asItem());
                        entries.add(ChineseCoreBuildingBlocks.falling_rocks_on_right.asItem());
                        entries.add(ChineseCoreBuildingBlocks.hump_back_bridge.asItem());
                        entries.add(ChineseCoreBuildingBlocks.keep_distance.asItem());
                        entries.add(ChineseCoreBuildingBlocks.lane_induce01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.lane_induce02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.lane_induce03.asItem());
                        entries.add(ChineseCoreBuildingBlocks.lane_induce04.asItem());
                        entries.add(ChineseCoreBuildingBlocks.lane_induce05.asItem());
//                        entries.add(ChineseCoreBuildingBlocks.narrow_bridge_both_sides.asItem());
                        entries.add(ChineseCoreBuildingBlocks.obstacle_bypass_left.asItem());
                        entries.add(ChineseCoreBuildingBlocks.obstacle_bypass_right.asItem());
//                        entries.add(ChineseCoreBuildingBlocks.obstacle_bypass.asItem());
                        entries.add(ChineseCoreBuildingBlocks.reversible_ahead.asItem());
                        entries.add(ChineseCoreBuildingBlocks.road_narrows_on_both_sides.asItem());
                        entries.add(ChineseCoreBuildingBlocks.road_narrows_on_left.asItem());
                        entries.add(ChineseCoreBuildingBlocks.road_narrows_on_right.asItem());
                        entries.add(ChineseCoreBuildingBlocks.road_work01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.road_work02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.rough_road_ahead.asItem());
                        entries.add(ChineseCoreBuildingBlocks.speed_hump_ahead.asItem());
                        entries.add(ChineseCoreBuildingBlocks.stripe_symbol01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.stripe_symbol02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.stripe_symbol03.asItem());
                        entries.add(ChineseCoreBuildingBlocks.traffic_accident_ahead.asItem());
                        entries.add(ChineseCoreBuildingBlocks.traffic_lights_ahead.asItem());
                        entries.add(ChineseCoreBuildingBlocks.tunnel.asItem());
                        entries.add(ChineseCoreBuildingBlocks.two_way_traffic.asItem());
//                        entries.add(ChineseCoreBuildingBlocks.unguarded_railway_crossing01.asItem());
//                        entries.add(ChineseCoreBuildingBlocks.unguarded_railway_crossing02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.videl_to_people_with_disabilities01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.videl_to_people_with_disabilities02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.village.asItem());
                        entries.add(ChineseCoreBuildingBlocks.watch_for_children01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.watch_for_children02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.watch_for_livestock.asItem());
                        entries.add(ChineseCoreBuildingBlocks.watch_for_non_motor_vehicles.asItem());
                        entries.add(ChineseCoreBuildingBlocks.watch_for_pedestrians01.asItem());
                        entries.add(ChineseCoreBuildingBlocks.watch_for_pedestrians02.asItem());
                        entries.add(ChineseCoreBuildingBlocks.watch_for_wild_animals.asItem());
                        // ------ 禁令标志 ------
                        entries.add(ChineseCoreBuildingBlocks.axle_weight_limit.asItem());
                        entries.add(ChineseCoreBuildingBlocks.do_not_enter.asItem());
                        entries.add(ChineseCoreBuildingBlocks.end_of_km_h.asItem());
                        entries.add(ChineseCoreBuildingBlocks.end_of_no_overtaking.asItem());
                        entries.add(ChineseCoreBuildingBlocks.give_way_to_oncoming_vehi.asItem());
                        entries.add(ChineseCoreBuildingBlocks.height_limit.asItem());
                        entries.add(ChineseCoreBuildingBlocks.km_h_and_no_entry.asItem());
                        entries.add(ChineseCoreBuildingBlocks.motor_vehicles_only.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_honking.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_large_buses.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_mini_buses.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_motor_vehicles.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_overtaking.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_parking.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_pedestrians.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_stoping.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_straight_thru_or_left_tur.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_straight_thru_or_right_tu.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_trailers_or_semi_teailers.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_trucks.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_turn_left.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_turn_right.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_turn_straight.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_turns.asItem());
                        entries.add(ChineseCoreBuildingBlocks.no_u_turn.asItem());
                        entries.add(ChineseCoreBuildingBlocks.stop.asItem());
                        entries.add(ChineseCoreBuildingBlocks.stop_for_inspection.asItem());
                        entries.add(ChineseCoreBuildingBlocks.this_area.asItem());
                        entries.add(ChineseCoreBuildingBlocks.weight_limit.asItem());
                        entries.add(ChineseCoreBuildingBlocks.width_limit.asItem());
                        entries.add(ChineseCoreBuildingBlocks.yield.asItem());
                    })
                    .build()
    );

    /**
     * 初始化入口。
     * <p>
     * 调用此方法会触发类加载，使所有静态常量（物品组实例）被初始化并注册。
     * 由 {@link com.chinesecorebuilding.ChineseCoreBuildingMod#onInitialize()} 调用。
     * </p>
     */
    public static void initialize() {
        // 无需额外逻辑，类加载时静态字段会自动初始化
    }
}
