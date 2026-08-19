package com.chinesecorebuilding.block;

import com.chinesecorebuilding.ChineseCoreBuildingMod;
import com.chinesecorebuilding.block.roadSigns.RoadSignsBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 方块注册中心。
 * <p>
 * 负责注册本模组的所有方块及其对应的方块物品。
 * 每个方块通过 {@link #register(String, Block)} 方法统一注册，
 * 同时向方块注册表和物品注册表写入数据。
 * </p>
 * <p>
 * 初始化时机：由 {@link ChineseCoreBuildingMod#onInitialize()} 调用 {@link #initialize()}。
 * </p>
 */
public class ChineseCoreBuildingBlocks {

    // ==================== 路标方块（RoadSignsBlock） ====================

    // ------ 警告标志 ------
    public static final Block beware_of_crosswind = register("beware_of_crosswind", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossbuck_symbol = register("crossbuck_symbol", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads01 = register("crossroads01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads02 = register("crossroads02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads03 = register("crossroads03", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads04 = register("crossroads04", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads05 = register("crossroads05", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads06 = register("crossroads06", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads07 = register("crossroads07", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads08 = register("crossroads08", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads09 = register("crossroads09", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads10 = register("crossroads10", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block crossroads11 = register("crossroads11", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    //public static final Block drive_with_caution = register("drive_with_caution", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block embankment_road01 = register("embankment_road01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block embankment_road02 = register("embankment_road02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block falling_rocks_on_left = register("falling_rocks_on_left", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block falling_rocks_on_right = register("falling_rocks_on_right", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block hump_back_bridge = register("hump_back_bridge", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block keep_distance = register("keep_distance", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block lane_induce01 = register("lane_induce01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block lane_induce02 = register("lane_induce02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block lane_induce03 = register("lane_induce03", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block lane_induce04 = register("lane_induce04", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block lane_induce05 = register("lane_induce05", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    //public static final Block narrow_bridge_both_sides = register("narrow_bridge_both_sides", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block obstacle_bypass_left = register("obstacle_bypass_left", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block obstacle_bypass_right = register("obstacle_bypass_right", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    //public static final Block obstacle_bypass = register("obstacle_bypass", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block reversible_ahead = register("reversible_ahead", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block road_narrows_on_both_sides = register("road_narrows_on_both_sides", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block road_narrows_on_left = register("road_narrows_on_left", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block road_narrows_on_right = register("road_narrows_on_right", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block road_work01 = register("road_work01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block road_work02 = register("road_work02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block rough_road_ahead = register("rough_road_ahead", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block speed_hump_ahead = register("speed_hump_ahead", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block stripe_symbol01 = register("stripe_symbol01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block stripe_symbol02 = register("stripe_symbol02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block stripe_symbol03 = register("stripe_symbol03", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block traffic_accident_ahead = register("traffic_accident_ahead", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block traffic_lights_ahead = register("traffic_lights_ahead", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block tunnel = register("tunnel", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block two_way_traffic = register("two_way_traffic", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    //public static final Block unguarded_railway_crossing01 = register("unguarded_railway_crossing01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    //public static final Block unguarded_railway_crossing02 = register("unguarded_railway_crossing02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block videl_to_people_with_disabilities01 = register("videl_to_people_with_disabilities01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block videl_to_people_with_disabilities02 = register("videl_to_people_with_disabilities02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block village = register("village", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block watch_for_children01 = register("watch_for_children01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block watch_for_children02 = register("watch_for_children02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block watch_for_livestock = register("watch_for_livestock", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block watch_for_non_motor_vehicles = register("watch_for_non_motor_vehicles", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block watch_for_pedestrians01 = register("watch_for_pedestrians01", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block watch_for_pedestrians02 = register("watch_for_pedestrians02", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block watch_for_wild_animals = register("watch_for_wild_animals", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));

    // ------ 禁令标志 ------
    public static final Block axle_weight_limit = register("axle_weight_limit", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block do_not_enter = register("do_not_enter", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block end_of_km_h = register("end_of_km_h", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block end_of_no_overtaking = register("end_of_no_overtaking", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block give_way_to_oncoming_vehi = register("give_way_to_oncoming_vehi", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block height_limit = register("height_limit", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block km_h_and_no_entry = register("km_h_and_no_entry", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block motor_vehicles_only = register("motor_vehicles_only", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_honking = register("no_honking", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_large_buses = register("no_large_buses", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_mini_buses = register("no_mini_buses", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_motor_vehicles = register("no_motor_vehicles", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_overtaking = register("no_overtaking", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_parking = register("no_parking", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_pedestrians = register("no_pedestrians", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_stoping = register("no_stoping", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_straight_thru_or_left_tur = register("no_straight_thru_or_left_tur", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_straight_thru_or_right_tu = register("no_straight_thru_or_right_tu", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_trailers_or_semi_teailers = register("no_trailers_or_semi_teailers", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_trucks = register("no_trucks", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_turn_left = register("no_turn_left", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_turn_right = register("no_turn_right", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_turn_straight = register("no_turn_straight", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_turns = register("no_turns", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block no_u_turn = register("no_u_turn", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block stop = register("stop", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block stop_for_inspection = register("stop_for_inspection", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block this_area = register("this_area", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block weight_limit = register("weight_limit", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block width_limit = register("width_limit", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));
    public static final Block yield = register("yield", new RoadSignsBlock(FabricBlockSettings.create().strength(4.0f).nonOpaque()));

    /**
     * 统一注册方块及其对应的方块物品。
     * <p>
     * 执行两步注册：
     * <ol>
     *     <li>将方块注册到 {@link Registries#BLOCK}，ID 为 {@code chinese-core-building:path}</li>
     *     <li>自动创建 {@link BlockItem} 并注册到 {@link Registries#ITEM}，使方块可放入物品栏</li>
     * </ol>
     * </p>
     *
     * @param path  方块注册名（如 "embankment_road01"）
     * @param block 方块实例
     * @param <T>   方块类型，用于保留具体类型信息
     * @return 传入的方块实例（支持链式调用）
     */
    private static <T extends Block> T register(String path, T block) {
        // 注册方块到方块注册表
        Registry.register(Registries.BLOCK, Identifier.of(ChineseCoreBuildingMod.MOD_ID, path), block);
        // 自动创建对应的方块物品并注册到物品注册表
        Registry.register(Registries.ITEM, Identifier.of(ChineseCoreBuildingMod.MOD_ID, path), new BlockItem(block, new Item.Settings()));
        return block;
    }

    /**
     * 初始化入口。
     * <p>
     * 调用此方法会触发类加载，使所有静态常量（方块实例）被初始化并注册。
     * 由 {@link ChineseCoreBuildingMod#onInitialize()} 调用。
     * </p>
     */
    public static void initialize() {
        // 无需额外逻辑，类加载时静态字段会自动初始化
    }
}
