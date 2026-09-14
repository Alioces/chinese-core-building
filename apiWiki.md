# Block Properties API Wiki

> 方块能力接口体系 —— 以接口声明能力，客户端自动消费，遵循开闭原则（OCP）。

---

## 1. 设计理念

传统 Fabric Mod 做法：每种方块在客户端写一堆 `if (block instanceof XxxBlock)` 的硬编码判断。本模块**反其道而行之**：

- **方块只声明能力**（通过接口），不关心"谁来消费"
- **客户端统一扫描接口**，自动注册渲染器 / 模型后处理 / 渲染层
- **新增能力 = 新增接口 + 新增消费方**，不改既有代码

```
Block (实现接口)          Client (扫描接口)       实际效果
─────────────────────────────────────────────────────────
Rotatable ──────────────► RotationBakedModel      模型烘焙时应用 Y 旋转
Directional ────────────► RotationBakedModel      同上（从 FACING 算角度）
Offset ─────────────────► OffsetBakedModel       模型烘焙时应用 3D 偏移
Layered ─────────────────► BlockRenderLayerMap    自动绑定 CUTOUT/TRANSLUCENT
SignTextProvider ───────► SignBlockRenderer      方块实体渲染时叠加文字层
```

---

## 2. 接口层次图

```
Block (Minecraft)
  │
  ├── Rotatable ◄── Directional
  │        │              │
  │        │              └── 特化为 4 方向 (FACING)
  │        │                   覆盖 getRotationAngle()
  │        │
  │        └── 通用旋转 (ROTATION 0~15)
  │            getRotationAngle() 每档 22.5°
  │
  ├── Offset             (独立，可与 Rotatable/Directional 共存)
  │     └── getOffset(BlockState) → {dx, dy, dz}
  │
  ├── Layered            (独立)
  │     └── getRenderLayerType() → SOLID | CUTOUT | TRANSLUCENT
  │
  ├── SignTextProvider   (独立，需配合 BlockEntityProvider)
  │     ├── getBlockEntityType()
  │     ├── getSignEntity(World, BlockPos) [default]
  │     ├── getTextLines(World, BlockPos)  [default]
  │     └── getTextLineCount(World, BlockPos) [default]
  │
  └── OffsetFunction     (函数式接口，供构造函数传递偏移配置)
        └── getOffset() → {offsetXZ, offsetY}
```

### 典型组合

| 方块类型 | 应实现的接口 |
|----------|-------------|
| 需要旋转 + 偏移的路标 | `Directional` + `Offset` + `Layered` + `BlockEntityProvider` + `SignTextProvider` |
| 只需旋转的装饰方块 | `Directional` + `Layered` |
| 可自由角度旋转的方块 | `Rotatable` + `Layered` |
| 半透明方块 | `Layered`（TRANSLUCENT） |
| 纯文字显示牌 | `Directional` + `Offset` + `Layered` + `BlockEntityProvider` + `SignTextProvider` |

---

## 3. 各接口详细说明

### 3.1 Rotatable（可旋转）

| 项目 | 说明 |
|------|------|
| **用途** | 声明方块支持任意角度旋转（0~15 档，每档 22.5°） |
| **依赖** | 无（可独立使用） |
| **BlockState 属性** | `ROTATION: IntProperty`（Minecraft 内置 `Properties.ROTATION`） |
| **客户端消费方** | `RotationBakedModel` |
| **构造要求** | `setDefaultState(initRotation(getDefaultState()))` |
| **appendProperties** | 必须显式调用 `Rotatable.super.appendProperties(builder)` |

**核心方法**：

| 方法 | 说明 |
|------|------|
| `initRotation(BlockState)` | 默认 ROTATION=0 |
| `calculateRotation(int)` | 默认原值返回，可自定义旋转值映射 |
| `getRotationAngle(BlockState)` | 从 ROTATION 属性计算弧度角（每档 22.5°） |
| `getOutlineShape(...)` / `getCollisionShape(...)` | 默认 fullCube，可自定义 |

---

### 3.2 Directional（可朝向）

| 项目 | 说明 |
|------|------|
| **用途** | 声明方块支持 4 方向水平朝向（north/east/south/west） |
| **继承** | `extends Rotatable` |
| **依赖** | 无（可独立使用） |
| **BlockState 属性** | `FACING: DirectionProperty`（Minecraft 内置 `Properties.HORIZONTAL_FACING`） |
| **覆盖** | 用 FACING 替代 ROTATION；每方向 90° 增量 |
| **客户端消费方** | `RotationBakedModel`（通过父接口的 `getRotationAngle`） |
| **构造要求** | `setDefaultState(initDirection(getDefaultState()))` |
| **appendProperties** | 必须显式调用 `Directional.super.appendProperties(builder)` |

**核心方法**：

| 方法 | 说明 |
|------|------|
| `initDirection(BlockState)` | 默认 FACING=NORTH |
| `directionToRotation(Direction)` | SOUTH→0, EAST→1, NORTH→2, WEST→3 |
| `getRotationAngle(BlockState)` | 重写 Rotatable：从 FACING 算弧度（每方向 90°） |
| `calculateDirection(ItemPlacementContext)` | 默认取玩家水平朝向 |
| `getOutlineShape(...)` / `getCollisionShape(...)` | 默认 fullCube，可自定义 per-direction |

**getRotationAngle 计算示例**：
```
FACING = SOUTH → rotation=0  → 0°   → 0.0 rad
FACING = EAST  → rotation=1  → 90°  → π/2 rad
FACING = NORTH → rotation=2  → 180° → π   rad
FACING = WEST  → rotation=3  → 270° → 3π/2 rad
```

---

### 3.3 Offset（可偏移）

| 项目 | 说明 |
|------|------|
| **用途** | 声明方块渲染时需要应用 3D 空间偏移 |
| **依赖** | 无（可与 Rotatable/Directional 任意组合） |
| **BlockState 属性** | 无新增（偏移量由实现类根据已有属性计算） |
| **客户端消费方** | `OffsetBakedModel` + `SignBlockRenderer` |

**核心方法**：

| 方法 | 说明 |
|------|------|
| `getOffset(BlockState)` | 返回 `{dx, dy, dz}`，单位为方块格（1.0 = 1 格） |

**典型实现**（与 Directional 组合）：
```java
public float[] getOffset(BlockState state) {
    float ox = BlockUtil.blockConstraint(1.5f); // ≈ 0.09375 格
    return switch (state.get(FACING)) {
        case SOUTH -> new float[]{0, 0,  ox};
        case NORTH -> new float[]{0, 0, -ox};
        case EAST  -> new float[]{ ox, 0, 0};
        case WEST  -> new float[]{-ox, 0, 0};
        default    -> new float[]{0, 0, 0};
    };
}
```

---

### 3.4 Layered（可声明渲染层）

| 项目 | 说明 |
|------|------|
| **用途** | 声明方块所需的 Minecraft 渲染层类型 |
| **依赖** | 无 |
| **客户端消费方** | `ChineseCoreBuildingClient.registerRenderLayers()` 自动扫描注册 |

**核心方法**：

| 方法 | 返回值 |
|------|--------|
| `getRenderLayerType()` | `RenderLayerType`（默认 SOLID） |

**RenderLayerType 枚举**：

| 枚举值 | 对应 Minecraft RenderLayer | 适用场景 |
|--------|----------------------------|----------|
| `SOLID` | `RenderLayer.getSolid()` | 不透明方块（默认） |
| `CUTOUT` | `RenderLayer.getCutout()` | 带透明贴图的方块（路标、栅栏、树叶） |
| `TRANSLUCENT` | `RenderLayer.getTranslucent()` | 玻璃、冰、粘液块 |

---

### 3.5 SignTextProvider（文字层能力）

| 项目 | 说明 |
|------|------|
| **用途** | 声明方块需要在渲染时叠加可自定义文字层（独立于方块模型） |
| **依赖** | 必须同时实现 `BlockEntityProvider`（因为文字数据存于 BlockEntity） |
| **客户端消费方** | `SignBlockRenderer`（BlockEntityRenderer，每帧渲染） |

**核心方法**：

| 方法 | 类型 | 说明 |
|------|------|------|
| `getBlockEntityType()` | abstract | 返回此类方块专属的 `BlockEntityType<? extends SignBlockEntity>` |
| `getSignEntity(World, BlockPos)` | default | 类型安全地获取 SignBlockEntity（自动校验类型） |
| `getTextLines(World, BlockPos)` | default | 获取不可变文字行列表；不存在返回 `List.of()` |
| `getTextLineCount(World, BlockPos)` | default | 获取文字行数量；不存在返回 0 |

**与其他接口的关系**：
```
SignTextProvider 本身不处理变换，变换由消费方 SignBlockRenderer 负责。
SignBlockRenderer 会自动检查：
  - Directional / Rotatable → 应用 Y 轴旋转（绕 0.5,0,0.5）
  - Offset                  → 应用 3D 偏移
  - 然后再渲染每行 TextLine
```

---

### 3.6 OffsetFunction（偏移计算函数式接口）

| 项目 | 说明 |
|------|------|
| **用途** | 向方块构造函数传递偏移配置（函数式替代子类） |
| **类型** | `@FunctionalInterface` |
| **与 Offset 的区别** | `Offset` 是方块的**实例方法**（运行时根据 BlockState 动态计算）；`OffsetFunction` 是**构造时的静态配置** |

**核心方法**：

| 方法 | 返回值 |
|------|--------|
| `getOffset()` | `{offsetXZ, offsetY}`（长度为 2） |

---

## 4. 客户端消费架构

```
ChineseCoreBuildingClient.onInitializeClient()
│
├── registerRenderLayers()
│     └── 遍历 Registries.BLOCK → 扫描 Layered → BlockRenderLayerMap.putBlock()
│
├── ModelPluginRegistry
│     ├── register(block -> block instanceof Rotatable, RotationBakedModel::new)
│     ├── register(block -> block instanceof Offset,    OffsetBakedModel::new)
│     └── registerAll()  // 把插件链应用到所有匹配方块的 BakedModel
│
└── BlockEntityRendererFactories.register(
        TestSignBlock.ENTITY_TYPE,
        SignBlockRenderer::new
    )
```

### 渲染管线对比

| 阶段 | 消费方 | 时机 | 处理内容 |
|------|--------|------|----------|
| **模型烘焙** | RotationBakedModel / OffsetBakedModel | 资源加载 | 读取 BlockState 属性 → 变换顶点 |
| **BlockEntity 渲染** | SignBlockRenderer | 每帧渲染 | 读取 BlockState 属性 → 变换矩阵 → 绘制文字 |
| **渲染层绑定** | BlockRenderLayerMap | 初始化一次 | Layered.getRenderLayerType() → RenderLayer 映射 |

> **关键一致性保证**：RotationBakedModel 和 SignBlockRenderer 使用完全相同的接口读取旋转/偏移参数，保证方块模型和文字层严格对齐。

---

## 5. 实现规范 Checklist

实现一个带文字层的路牌方块需满足：

- [ ] Block 类 `implements Directional, Offset, Layered, BlockEntityProvider, SignTextProvider`
- [ ] 构造函数：`setDefaultState(initDirection(getDefaultState()))`
- [ ] `appendProperties`：显式调用 `Directional.super.appendProperties(builder)`
- [ ] `getRenderLayerType()` 返回 CUTOUT（路牌通常有透明区域）
- [ ] `getOffset(BlockState)` 根据 FACING 返回正确的偏移
- [ ] `createBlockEntity(pos, state)` 返回正确类型的 SignBlockEntity 子类
- [ ] `getBlockEntityType()` 返回注册的 BlockEntityType
- [ ] 注册时 BlockEntityType 的 factory 必须与构造函数签名匹配（三元 `(type, pos, state)`）

---

## 6. 扩展新能力（开闭原则实践）

想给方块加一种新的渲染能力（比如发光效果、动态纹理、粒子叠加）？

**三步**，不改一行既有代码：

```
1. 新建接口 Glowable {
     default boolean shouldGlow(BlockState state) { return false; }
   }

2. 新建消费方 GlowBakedModel implements BakedModel, Wrapper  {
     // 在 RotationBakedModel / OffsetBakedModel 同层级注册
   }

3. 在 ChineseCoreBuildingClient.onInitializeClient() 中注册：
     ModelPluginRegistry.register(
         block -> block instanceof Glowable,
         GlowBakedModel::new
     );
```

方块类想获得发光能力？`implements Glowable` 就行。客户端自动扫描。