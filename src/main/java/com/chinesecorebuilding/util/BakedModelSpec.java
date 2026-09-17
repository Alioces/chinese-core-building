package com.chinesecorebuilding.util;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烘焙模型规格。
 * <p>
 * 封装模型烘焙过程中的所有配置信息，包括基础模型、子模型列表、
 * 贴图覆盖映射和发光等级。在插件链中传递，每个插件可以修改此规格
 * 以添加或修改模型属性。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * BakedModelSpec spec = new BakedModelSpec("block/glass");
 * spec.addSubModel("block/oak_planks", anchor.transform());
 * spec.applyTextureOverrides(Map.of("#all", "block/stone_bricks"));
 * spec.setEmissiveLevel(15);
 * </pre>
 */
public class BakedModelSpec {

    /**
     * 基础模型 ID。
     */
    private final Identifier baseModelId;

    /**
     * 子模型列表（模型 ID + 变换）。
     */
    private final List<SubModelEntry> subModels = new ArrayList<>();

    /**
     * 贴图覆盖映射（纹理变量 → 贴图路径）。
     */
    private final Map<String, String> textureOverrides = new HashMap<>();

    /**
     * 发光等级（0~15）。
     */
    private int emissiveLevel = 0;

    /**
     * 创建烘焙模型规格。
     *
     * @param baseModelId 基础模型 ID（如 "block/glass"）
     */
    public BakedModelSpec(Identifier baseModelId) {
        this.baseModelId = baseModelId;
    }

    /**
     * 创建烘焙模型规格（字符串 ID）。
     *
     * @param baseModelId 基础模型 ID（如 "block/glass"）
     */
    public BakedModelSpec(String baseModelId) {
        this(Identifier.tryParse(baseModelId));
    }

    /**
     * 添加子模型。
     *
     * @param modelId   子模型 ID
     * @param transform 变换矩阵（位置、旋转、缩放）
     */
    public void addSubModel(String modelId, Transform transform) {
        subModels.add(new SubModelEntry(Identifier.tryParse(modelId), transform));
    }

    /**
     * 添加子模型。
     *
     * @param modelId   子模型 ID
     * @param transform 变换矩阵
     */
    public void addSubModel(Identifier modelId, Transform transform) {
        subModels.add(new SubModelEntry(modelId, transform));
    }

    /**
     * 应用贴图覆盖映射。
     *
     * @param overrides 纹理变量到贴图路径的映射
     */
    public void applyTextureOverrides(Map<String, String> overrides) {
        textureOverrides.putAll(overrides);
    }

    /**
     * 设置发光等级。
     *
     * @param level 发光等级（0~15）
     */
    public void setEmissiveLevel(int level) {
        this.emissiveLevel = Math.max(0, Math.min(15, level));
    }

    /**
     * 获取基础模型 ID。
     *
     * @return 基础模型 ID
     */
    public Identifier getBaseModelId() {
        return baseModelId;
    }

    /**
     * 获取子模型列表。
     *
     * @return 子模型条目列表
     */
    public List<SubModelEntry> getSubModels() {
        return subModels;
    }

    /**
     * 获取贴图覆盖映射。
     *
     * @return 纹理变量到贴图路径的映射
     */
    public Map<String, String> getTextureOverrides() {
        return textureOverrides;
    }

    /**
     * 获取发光等级。
     *
     * @return 发光等级（0~15）
     */
    public int getEmissiveLevel() {
        return emissiveLevel;
    }

    /**
     * 子模型条目。
     *
     * @param modelId   子模型 ID
     * @param transform 变换矩阵
     */
    public record SubModelEntry(Identifier modelId, Transform transform) {
    }
}