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
SignTextProvider ───────► TextBakedModel          模型烘焙时追加文字层顶点
Interactive ────────────► UseBlockCallback       客户端右键打开 GUI
```

---

## 2. 接口层次图

```
Block (Minecraft)
  │
  ├── ModelBakeDecorator (阶段标识父接口)
  │     └── SignTextProvider   (独立，需配合 BlockEntityProvider)
  │           ├── getBlockEntityType()
  │           ├── getSignEntity(World, BlockPos) [default]
  │           ├── getTextLines(World, BlockPos)  [default]
  │           └── getTextLineCount(World, BlockPos) [default]
  │
  ├── ModelWorldTransformer (阶段标识父接口)
  │     ├── Rotatable ◄── Directional
  │     │        │              │
  │     │        │              └── 特化为 4 方向 (FACING)
  │     │        │                   覆盖 getRotationAngle()
  │     │        │
  │     │        └── 通用旋转 (ROTATION 0~15)
  │     │            getRotationAngle() 每档 22.5°
  │     │
  │     └── Offset             (独立，可与 Rotatable/Directional 共存)
  │           └── getOffset(BlockState) → {dx, dy, dz}
  │
  ├── Layered            (独立)
  │     └── getRenderLayerType() → SOLID | CUTOUT | TRANSLUCENT
  │
  ├── Interactive        (独立，声明方块交互能力)
  │     ├── getInteraction() → BlockInteraction (服务端处理器)
  │     └── blocksDefaultInteraction() → boolean (阻断原版默认行为)
  │           │
  │           └── GuiInteractive (继承，特化客户端 GUI 交互)
  │                 ├── shouldOpenGui() → boolean (始终返回 true)
  │                 ├── canOpenGui(PlayerEntity, BlockPos) → boolean (前置条件检查)
  │                 └── onGuiOpened(PlayerEntity, BlockPos) → void (打开后回调)
  │
  └── OffsetFunction     (函数式接口，供构造函数传递偏移配置)
        └── getOffset() → {offsetXZ, offsetY}
```

### 典型组合

| 方块类型 | 应实现的接口 |
|----------|-------------|
| 需要旋转 + 偏移的路标 | `Directional` + `Offset` + `Layered` |
| 只需旋转的装饰方块 | `Directional` + `Layered` |
| 可自由角度旋转的方块 | `Rotatable` + `Layered` |
| 半透明方块 | `Layered`（TRANSLUCENT） |
| 纯文字显示牌 | `Directional` + `Offset` + `Layered` + `BlockEntityProvider` + `SignTextProvider` + `GuiInteractive` |
| 可交互编辑的文字方块 | `GuiInteractive` + `SignTextProvider` + `BlockEntityProvider` |
| 需要权限控制的编辑方块 | `GuiInteractive`（覆盖 `canOpenGui` 添加权限检查） |

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
| **阶段标识** | 继承 `ModelBakeDecorator`（模型烘焙修饰阶段） |
| **客户端消费方** | `TextBakedModel`（模型烘焙阶段） + `SignBlockRenderer`（BlockEntityRenderer，每帧渲染） |

**核心方法**：

| 方法 | 类型 | 说明 |
|------|------|------|
| `getBlockEntityType()` | abstract | 返回此类方块专属的 `BlockEntityType<? extends SignBlockEntity>` |
| `getSignEntity(World, BlockPos)` | default | 类型安全地获取 SignBlockEntity（自动校验类型） |
| `getTextLines(World, BlockPos)` | default | 获取不可变文字行列表；不存在返回 `List.of()` |
| `getTextLineCount(World, BlockPos)` | default | 获取文字行数量；不存在返回 0 |

**与其他接口的关系**：
```
SignTextProvider 属于 ModelBakeDecorator 阶段（往模型里追加顶点）。
变换阶段（Rotatable/Offset）作用在装饰阶段的结果之上。
SignBlockRenderer 会自动检查：
  - Directional / Rotatable → 应用 Y 轴旋转（绕 0.5,0.5,0.5）
  - Offset                  → 应用 3D 偏移
  - 然后再渲染每行 TextLine
```

---

### 3.6 Interactive（方块交互能力）

| 项目 | 说明 |
|------|------|
| **用途** | 声明方块拥有右键交互能力，包括服务端处理器和默认行为阻断 |
| **依赖** | 无（可独立使用） |
| **消费方** | `CustomBlock.onUse()`（服务端分发） |

**核心方法**：

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getInteraction()` | `BlockInteraction` | 服务端交互处理器，默认 null（无服务端逻辑） |
| `blocksDefaultInteraction()` | `boolean` | 是否阻断原版右键默认行为（放置、容器等），默认 true |

**CustomBlock.onUse 两级决策链**：
```
1. 若方块实现 Interactive 且 getInteraction() != null
   → 完全交给 handler，handler 的 ActionResult 决定一切

2. 若 getInteraction() == null 但 blocksDefaultInteraction() == true
   → 返回 CONSUME 阻断原版默认行为（防止 BlockItem 在旁边放置新方块）

3. 否则 → super.onUse() 走原版逻辑（通常返回 PASS 放行放置）
```

**典型实现**：
```java
public class MyBlock extends CustomBlock implements Interactive {
    @Override
    public BlockInteraction getInteraction() {
        return (state, world, pos, player, hand, hit) -> {
            // 自定义服务端逻辑
            return ActionResult.SUCCESS;
        };
    }
}
```

---

### 3.6.1 GuiInteractive（客户端 GUI 交互）

| 项目 | 说明 |
|------|------|
| **用途** | 继承 `Interactive`，特化客户端右键打开 GUI 的逻辑，支持前置条件检查和打开后回调 |
| **继承** | `extends Interactive` |
| **依赖** | 无（可独立使用） |
| **消费方** | `UseBlockCallback`（客户端 GUI 触发） |

**核心方法**：

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `canOpenGui(PlayerEntity, BlockPos)` | `boolean` | 打开 GUI 前的前置条件检查（权限、距离等），默认 true |
| `onGuiOpened(PlayerEntity, BlockPos)` | `void` | GUI 打开后的回调逻辑（音效、粒子等），默认无操作 |
| `shouldOpenGui()` | `boolean` | 始终返回 true（标记需要打开 GUI） |

**与 Interactive 的区别**：
- `Interactive` 只负责服务端交互和默认行为阻断
- `GuiInteractive` 专门处理客户端 GUI 交互，提供前置检查和回调机制

**典型实现**：
```java
public class MySignBlock extends CustomBlock implements GuiInteractive {
    @Override
    public boolean canOpenGui(PlayerEntity player, BlockPos pos) {
        // 示例：只有 OP 玩家才能打开编辑界面
        return player.hasPermissionLevel(2);
    }

    @Override
    public void onGuiOpened(PlayerEntity player, BlockPos pos) {
        // 示例：打开 GUI 时播放音效
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }
}
```

**客户端处理逻辑**：
```
UseBlockCallback 检测到 GuiInteractive
  → 调用 canOpenGui(player, pos) 检查前置条件
    → 若返回 false → 阻断，返回 SUCCESS
    → 若返回 true → 打开 SignBlockScreen
      → 调用 onGuiOpened(player, pos) 执行回调
      → 返回 SUCCESS
```

---

### 3.7 BlockInteraction（服务端交互处理器）

| 项目 | 说明 |
|------|------|
| **用途** | 封装完整的右键处理逻辑，由 `Interactive.getInteraction()` 返回 |
| **类型** | `@FunctionalInterface` |

**核心方法**：

| 方法 | 参数 | 返回值 |
|------|------|--------|
| `interact(...)` | `BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult` | `ActionResult` |

**使用方式**：
```java
public class MySignBlock extends CustomBlock implements Interactive {
    @Override
    public BlockInteraction getInteraction() {
        return (state, world, pos, player, hand, hit) -> {
            // 打开 GUI / 修改文字 / 其他自定义行为
            return ActionResult.SUCCESS;
        };
    }
}
```

---

### 3.8 OffsetFunction（偏移计算函数式接口）

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

## 4. 模型烘焙管线架构

### 4.1 阶段标识接口

| 阶段标识 | 职责 | 属于此阶段的能力接口 |
|----------|------|---------------------|
| `ModelBakeDecorator` | 往模型顶点流里**追加新的几何数据**（如文字层） | `SignTextProvider` |
| `ModelWorldTransformer` | 对**所有已有顶点**做世界坐标变换（旋转、偏移） | `Rotatable` / `Directional` / `Offset` |

**执行顺序保证**：
```
ModelPluginRegistry.registerAll()
  → 先注册所有 DECORATOR 阶段插件（往模型里加东西）
  → 再注册所有 TRANSFORMER 阶段插件（对所有顶点做变换）
```

因此装饰层（如文字）产生的顶点会自动被旋转/偏移，无需在 BlockEntityRenderer 里手动重复变换逻辑。

### 4.2 插件链结构（从外向内）

```
OffsetBakedModel         ← 最外层，最后 emitBlockQuads
  → RotationBakedModel
    → TextBakedModel     ← 最内层，最先 emitBlockQuads
      → 原始模型
```

`RenderContext.pushTransform` 的作用方向是内向外，因此原始模型的面片先被 pushTransform，然后是 TextBakedModel 追加的顶点，最后由 Rotation/Offset 统一变换。

### 4.3 性能保证

- **BakedModel 是全局共享缓存的**，bake 时执行一次
- **emitBlockQuads 运行时的变换**只是在 RenderContext 上做 pushTransform → 矩阵乘法，**零额外 Draw call**
- **装饰层（文字）的顶点**与模型本体走同一个 RenderContext，同样**零额外 Draw call**

---

## 5. 客户端消费架构

```
ChineseCoreBuildingClient.onInitializeClient()
│
├── registerRenderLayers()
│     └── 遍历 Registries.BLOCK → 扫描 Layered → BlockRenderLayerMap.putBlock()
│
├── ModelPluginRegistry
│     ├── register(Phase.DECORATOR, block -> block instanceof SignTextProvider, TextBakedModel::new)
│     ├── register(Phase.TRANSFORMER, block -> block instanceof Rotatable, RotationBakedModel::new)
│     ├── register(Phase.TRANSFORMER, block -> block instanceof Offset,    OffsetBakedModel::new)
│     └── registerAll()  // 先装饰后变换的顺序保证
│
├── BlockEntityRendererFactories.register(
│         TestSignBlock.ENTITY_TYPE,
│         SignBlockRenderer::new
│     )
│
└── UseBlockCallback.EVENT.register(...)
      └── 客户端右键 → Interactive.shouldOpenGui() → 打开 SignBlockScreen
```

### 渲染管线对比

| 阶段 | 消费方 | 时机 | 处理内容 |
|------|--------|------|----------|
| **模型烘焙（装饰）** | TextBakedModel | 资源加载 | 读取 BlockEntity 数据 → 追加文字层顶点 |
| **模型烘焙（变换）** | RotationBakedModel / OffsetBakedModel | 资源加载 | 读取 BlockState 属性 → 变换所有顶点 |
| **BlockEntity 渲染** | SignBlockRenderer | 每帧渲染 | 读取 BlockState 属性 → 变换矩阵 → 绘制文字 |
| **渲染层绑定** | BlockRenderLayerMap | 初始化一次 | Layered.getRenderLayerType() → RenderLayer 映射 |

> **关键一致性保证**：RotationBakedModel / OffsetBakedModel 和 SignBlockRenderer 使用完全相同的接口读取旋转/偏移参数，保证方块模型和文字层严格对齐。

---

## 6. 数据流与网络通信

### 6.1 文字数据流

```
客户端 SignBlockScreen (GUI)
  │
  ├─ 编辑 editingLines (副本列表)
  │
  └─ 点击"保存"
       │
       ▼
  SignBlockClientService.saveLines(pos, lines)
       │
       ├─ SignBlockTextPayload.encode(buf, pos, lines)
       │     └─ NBT 序列化: BlockPos + NbtList(TextLine NBT)
       │
       └─ ClientPlayNetworking.send(ID, buf)
             │
             ▼
  服务端 SignBlockTextService (接收器)
       │
       ├─ SignBlockTextPayload.decode(buf) → Decoded(pos, lines)
       │
       └─ saveLines(world, pos, lines)
             │
             └─ entity.setLines(lines)
                   │
                   └─ markModified()
                         │
                         ├─ markDirty() → 持久化到磁盘
                         └─ syncToClient() → 广播更新包
```

### 6.2 核心数据结构

**TextLine（文字行记录）**：
```java
public record TextLine(
    Text text,           // 文字内容（支持 JSON 富文本）
    Vec3d position,      // 方块空间相对偏移 (0.5,0.5,1.0 = +Z 面中心)
    int color,           // 文字颜色 0xRRGGBB
    float scale,         // 缩放因子（默认 1.0）
    boolean glowing,     // 是否发光
    NbtCompound extra    // 预留扩展容器
)
```

**SignBlockEntity（文字层 BlockEntity）**：
- 继承 `CustomBlockEntity`（统一基类）
- 存储 `List<TextLine>`，支持增删改查
- 自动 `markModified()` → 持久化 + 客户端同步

---

## 7. BlockEntity 统一基类

### 7.1 CustomBlockEntity

| 项目 | 说明 |
|------|------|
| **用途** | 本模组 BlockEntity 统一基类，封装修改标记和网络同步逻辑 |
| **继承** | `net.minecraft.block.entity.BlockEntity` |

**核心方法**：

| 方法 | 说明 |
|------|------|
| `markModified()` | 标记数据已修改并同步到客户端（markDirty + syncToClient） |
| `syncToClient()` | 向客户端推送当前状态更新（仅服务端有效） |
| `toInitialChunkDataNbt()` | 构造初始区块数据 NBT（追加 writeNbt 自定义字段） |
| `toUpdatePacket()` | 生成客户端同步用的更新数据包 |

**继承层次**：
```
BlockEntity (Minecraft)
  └── CustomBlockEntity
        ├── SignBlockEntity        — 可编辑文字层
        └── 未来的 XXXBlockEntity  — 其他功能方块实体
```

---

## 8. 实现规范 Checklist

实现一个带文字层的路牌方块需满足：

- [ ] Block 类 `implements Directional, Offset, Layered, BlockEntityProvider, SignTextProvider, GuiInteractive`
- [ ] 构造函数：`setDefaultState(initDirection(getDefaultState()))`
- [ ] `appendProperties`：显式调用 `Directional.super.appendProperties(builder)`
- [ ] `getRenderLayerType()` 返回 CUTOUT（路牌通常有透明区域）
- [ ] `getOffset(BlockState)` 根据 FACING 返回正确的偏移
- [ ] `createBlockEntity(pos, state)` 返回正确类型的 SignBlockEntity 子类
- [ ] `getBlockEntityType()` 返回注册的 BlockEntityType
- [ ] 注册时 BlockEntityType 的 factory 必须与构造函数签名匹配（三元 `(type, pos, state)`）
- [ ] `blocksDefaultInteraction()` 返回 true（阻断原版放置逻辑）
- [ ] （可选）覆盖 `canOpenGui(player, pos)` 添加权限/距离检查
- [ ] （可选）覆盖 `onGuiOpened(player, pos)` 添加音效/粒子回调

---

## 9. 扩展新能力（开闭原则实践）

想给方块加一种新的渲染能力（比如发光效果、动态纹理、粒子叠加）？

**三步**，不改一行既有代码：

```java
// 1. 新建接口
interface Glowable extends ModelWorldTransformer {
    default boolean shouldGlow(BlockState state) { return false; }
}

// 2. 新建消费方
class GlowBakedModel extends ForwardingBakedModel {
    // 在 RotationBakedModel / OffsetBakedModel 同层级实现
}

// 3. 在 ChineseCoreBuildingClient.onInitializeClient() 中注册
ModelPluginRegistry.register(
    Phase.TRANSFORMER,
    block -> block instanceof Glowable,
    GlowBakedModel::new
);
```

方块类想获得发光能力？`implements Glowable` 就行。客户端自动扫描。

---

## 10. 常见问题

### Q: 为什么文字渲染在两个地方都有？

- **TextBakedModel**（模型烘焙阶段）：当前版本仅代理原始模型，为未来升级预留（动态贴图注入 RenderContext）
- **SignBlockRenderer**（BlockEntityRenderer）：当前实际渲染文字的地方，每帧调用

两者使用完全一致的变换公式和旋转中心，保证文字与模型同步。

### Q: 为什么 rotateY 用 `-angle`？

`RotationBakedModel` 里用的公式是"顺时针为正"（符合直觉），但 `MatrixStack.multiply(Quaternionf.rotateY)` 是右手系逆时针为正，所以传入 `-angle` 让它等效于顺时针旋转。

### Q: 如何添加新的交互逻辑？

实现 `Interactive` 接口的 `getInteraction()` 方法，返回一个 `BlockInteraction` 函数式接口实例即可。服务端会自动分发执行。