package com.chinesecorebuilding.block.entity;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * 单行文字数据记录。
 * <p>
 * 每个实例携带：文字内容、独立位置、独立颜色、独立缩放、发光状态，
 * 以及一个预留扩展容器 {@link #extra} 用于未来功能扩展。
 * </p>
 * <p>
 * 使用 record 保证不可变性，任何修改通过 {@code withXxx()} 返回新实例，
 * 配合 Builder 模式方便链式构建。
 * </p>
 * <p>
 * 字段说明：
 * <ul>
 *   <li>{@code text} — 文字内容（支持 JSON 富文本）</li>
 *   <li>{@code position} — 方块空间相对偏移（0.5,0.5,1.0 = +Z 面中心）</li>
 *   <li>{@code color} — 文字颜色 0xRRGGBB</li>
 *   <li>{@code scale} — 缩放因子</li>
 *   <li>{@code glowing} — 是否发光</li>
 *   <li>{@code extra} — 预留扩展容器</li>
 * </ul>
 * </p>
 *
 * @see SignBlockEntity
 */
public record TextLine(
        Text text,
        Vec3d position,
        int color,
        float scale,
        boolean glowing,
        NbtCompound extra
) {

    /** 默认颜色：黑色（0x000000） */
    public static final int DEFAULT_COLOR = 0;

    /** 默认缩放：1.0（不缩放） */
    public static final float DEFAULT_SCALE = 1.0f;

    /** 默认位置：+Z 面中心 (0.5, 0.5, 1.0) */
    public static final Vec3d DEFAULT_POSITION = new Vec3d(0.5, 0.5, 1.0);

    /**
     * 紧凑型构造函数，保证所有字段非 null。
     * <p>
     * 对 null 参数做默认值兜底：
     * {@code text → Text.empty()}、{@code position → DEFAULT_POSITION}、
     * {@code scale → DEFAULT_SCALE}（当 ≤ 0 时）、{@code extra → new NbtCompound()}。
     * </p>
     */
    public TextLine {
        if (text == null) text = Text.empty();
        if (position == null) position = DEFAULT_POSITION;
        if (scale <= 0) scale = DEFAULT_SCALE;
        if (extra == null) extra = new NbtCompound();
    }

    /** 返回一个副本，文字内容替换为指定值。 */
    public TextLine withText(Text text) {
        return new TextLine(text, position, color, scale, glowing, extra);
    }

    /** 返回一个副本，位置替换为指定 Vec3d。 */
    public TextLine withPosition(Vec3d position) {
        return new TextLine(text, position, color, scale, glowing, extra);
    }

    /** 返回一个副本，位置替换为指定三维坐标。 */
    public TextLine withPosition(double x, double y, double z) {
        return new TextLine(text, new Vec3d(x, y, z), color, scale, glowing, extra);
    }

    /** 返回一个副本，颜色替换为指定值（0xRRGGBB）。 */
    public TextLine withColor(int color) {
        return new TextLine(text, position, color, scale, glowing, extra);
    }

    /** 返回一个副本，缩放替换为指定值。 */
    public TextLine withScale(float scale) {
        return new TextLine(text, position, color, scale, glowing, extra);
    }

    /** 返回一个副本，发光状态替换为指定值。 */
    public TextLine withGlowing(boolean glowing) {
        return new TextLine(text, position, color, scale, glowing, extra);
    }

    /** 返回一个副本，扩展容器替换为指定值。 */
    public TextLine withExtra(NbtCompound extra) {
        return new TextLine(text, position, color, scale, glowing, extra);
    }

    /** 检查扩展容器是否包含指定键。 */
    public boolean hasExtra(String key) { return extra.contains(key); }

    /** 从扩展容器读取指定键的值。 */
    public NbtElement getExtra(String key) { return extra.get(key); }

    /** 返回一个副本，扩展容器追加指定字符串键值对。 */
    public TextLine putExtra(String key, String value) {
        NbtCompound copy = extra.copy(); copy.putString(key, value);
        return withExtra(copy);
    }

    /** 返回一个副本，扩展容器追加指定布尔键值对。 */
    public TextLine putExtra(String key, boolean value) {
        NbtCompound copy = extra.copy(); copy.putBoolean(key, value);
        return withExtra(copy);
    }

    /** 返回一个副本，扩展容器追加指定整数键值对。 */
    public TextLine putExtra(String key, int value) {
        NbtCompound copy = extra.copy(); copy.putInt(key, value);
        return withExtra(copy);
    }

    /** 返回一个副本，扩展容器追加指定浮点键值对。 */
    public TextLine putExtra(String key, float value) {
        NbtCompound copy = extra.copy(); copy.putFloat(key, value);
        return withExtra(copy);
    }

    /** 返回一个副本，扩展容器移除指定键。 */
    public TextLine removeExtra(String key) {
        NbtCompound copy = extra.copy(); copy.remove(key);
        return withExtra(copy);
    }

    /**
     * 序列化为 NBT 复合标签。
     * <p>
     * 字段映射：Text → {@code Text}、PosX/Y/Z → {@code PosX}/{@code PosY}/{@code PosZ}、
     * Color → {@code Color}、Scale → {@code Scale}、Glowing → {@code Glowing}、
     * Extra → {@code Extra}（仅当非空时写入）。
     * </p>
     *
     * @return 序列化后的 NbtCompound
     */
    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Text", Text.Serializer.toJson(text));
        nbt.putDouble("PosX", position.x);
        nbt.putDouble("PosY", position.y);
        nbt.putDouble("PosZ", position.z);
        nbt.putInt("Color", color);
        nbt.putFloat("Scale", scale);
        nbt.putBoolean("Glowing", glowing);
        if (!extra.isEmpty()) nbt.put("Extra", extra);
        return nbt;
    }

    /**
     * 从 NBT 复合标签反序列化。
     * <p>
     * 对缺失字段使用默认值兜底：Scale → {@link #DEFAULT_SCALE}、
     * Extra → 空 NbtCompound。
     * </p>
     *
     * @param nbt 待解析的 NbtCompound
     * @return 解析后的 TextLine 实例
     */
    public static TextLine readNbt(NbtCompound nbt) {
        Text text = Text.empty();
        if (nbt.contains("Text")) {
            Text parsed = Text.Serializer.fromJson(nbt.getString("Text"));
            if (parsed != null) text = parsed;
        }
        Vec3d pos = new Vec3d(
                nbt.getDouble("PosX"),
                nbt.getDouble("PosY"),
                nbt.getDouble("PosZ")
        );
        int color = nbt.getInt("Color");
        float scale = nbt.contains("Scale") ? nbt.getFloat("Scale") : DEFAULT_SCALE;
        boolean glowing = nbt.getBoolean("Glowing");
        NbtCompound extra = nbt.contains("Extra") ? nbt.getCompound("Extra") : new NbtCompound();
        return new TextLine(text, pos, color, scale, glowing, extra);
    }

    /** 创建 Builder 实例，用于链式构建 TextLine。 */
    public static Builder builder() { return new Builder(); }

    /**
     * TextLine 构建器。
     * <p>
     * 默认值与 record 紧凑构造函数的兜底值一致：
     * text → {@link Text#empty()}、position → {@link #DEFAULT_POSITION}、
     * color → {@link #DEFAULT_COLOR}、scale → {@link #DEFAULT_SCALE}、
     * glowing → false、extra → 空 NbtCompound。
     * </p>
     */
    public static class Builder {
        private Text text = Text.empty();
        private Vec3d position = DEFAULT_POSITION;
        private int color = DEFAULT_COLOR;
        private float scale = DEFAULT_SCALE;
        private boolean glowing = false;
        private NbtCompound extra = new NbtCompound();

        /** 设置文字内容（null 自动兜底为 {@link Text#empty()}）。 */
        public Builder text(Text text) { this.text = text != null ? text : Text.empty(); return this; }

        /** 设置文字内容为普通字符串。 */
        public Builder textLiteral(String literal) { this.text = Text.literal(literal); return this; }

        /** 设置位置为 Vec3d。 */
        public Builder position(Vec3d position) { this.position = position; return this; }

        /** 设置位置为 double 坐标。 */
        public Builder position(double x, double y, double z) { this.position = new Vec3d(x, y, z); return this; }

        /** 设置位置为 float 坐标。 */
        public Builder position(float x, float y, float z) { this.position = new Vec3d(x, y, z); return this; }

        /** 设置颜色（0xRRGGBB）。 */
        public Builder color(int color) { this.color = color; return this; }

        /** 设置缩放因子。 */
        public Builder scale(float scale) { this.scale = scale; return this; }

        /** 设置发光状态。 */
        public Builder glowing(boolean glowing) { this.glowing = glowing; return this; }

        /** 设置完整的扩展容器（null 自动兜底为空）。 */
        public Builder extra(NbtCompound extra) { this.extra = extra != null ? extra : new NbtCompound(); return this; }

        /** 向扩展容器追加字符串键值对。 */
        public Builder putExtra(String key, String value) { extra.putString(key, value); return this; }

        /** 向扩展容器追加布尔键值对。 */
        public Builder putExtra(String key, boolean value) { extra.putBoolean(key, value); return this; }

        /** 向扩展容器追加整数键值对。 */
        public Builder putExtra(String key, int value) { extra.putInt(key, value); return this; }

        /** 向扩展容器追加浮点键值对。 */
        public Builder putExtra(String key, float value) { extra.putFloat(key, value); return this; }

        /** 构建最终的 TextLine 实例。 */
        public TextLine build() {
            return new TextLine(text, position, color, scale, glowing, extra);
        }
    }
}