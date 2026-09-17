# 项目交接提示词

## 1. 项目概述

| 项目 | 说明 |
|------|------|
| **名称** | chinese-core-building（中核建设） |
| **平台** | Minecraft Java Edition 1.20.1 + Fabric Loader 0.16+ |
| **JDK** | 17（编译）/ 21（Loom 1.17-SNAPSHOT 需要 JDK 21 运行 Gradle） |
| **简介** | 面向中式建筑主题的装饰性方块模组，通过"方块声明接口"的方式驱动模型渲染、放置行为、交互逻辑，消除客户端硬编码 `instanceof` 判断 |

### 1.1 模组 ID

```java
// 项目唯一标识，贯穿所有注册表、日志、翻译键
ChineseCoreBuildingMod.MOD_ID = "chinese-core-building"
```

### 1.2 入口点

```jsonc
// fabric.mod.json
{
  "main":   "com.chinesecorebuilding.ChineseCoreBuildingMod",    // 服务端 + 数据端
  "client": "com.chinesecorebuilding.client.ChineseCoreBuildingClient", // 客户端
  "fabric-datagen": "..." // 数据生成
}
```

---

## 2. 项目设计方向

### 2.1 核心设计原则

> **方块只声明能力（通过接口），客户端统一扫描接口并自动注册**

新增一个方块类型时**不改任何客户端代码**，只需：

1. 创建 `XxxBlock extends CustomBlock`
2. 声明 `implements Rotatable, Layered, SubModelProvider, ...`
3. 编写 blockstate JSON、模型 JSON

### 2.2 开闭原则落地

| 原则 | 实现 |
|------|------|
| 对扩展开放 | 新增方块 = 新建类 + `implements` 接口 |
| 对修改封闭 | 客户端管线不改，`ChineseCoreBuildingClient` 不改 |
| 单一职责 | `CustomBlock` 只做分派；接口自带 default 实现；客户端 Plugin 只做渲染注册 |

### 2.3 反对的实践

- ❌ `if (block instanceof XxxBlock)` 硬编码 —— 改用接口扫描
- ❌ 方块类写样板 `getPlacementState` / `appendProperties` —— 交给接口 default 方法
- ❌ 客户端类跨包依赖服务端具体类型 —— 客户端只依赖接口

---

## 3. Fabric splitEnvironmentSourceSets 规则（务必理解！）

### 3.1 源码集分割

```gradle
// build.gradle
loom {
    splitEnvironmentSourceSets()  // ← 这条语句分离 main / client
}
```

效果：

```
src/
├── main/java/    ← 仅含服务端逻辑，编译时无法访问 client/ 下的任何类
└── client/java/  ← 可自由引用 main/java/ 中的所有类（单向依赖）
```

**main → client 是单向的**：`main/` 的代码不能 `import` 任何 `client/` 的代码，反之可以。

### 3.2 什么放在 main

| 可以 | 不可以 |
|------|--------|
| Block 类、BlockEntity 类 | `MinecraftClient` |
| 接口定义（Rotatable、Directional...） | `BakedModel`、`BakedModelSpec` |
| 枚举（RenderLayerType） | `RenderLayer`、`ModelLoadingPlugin` |
| 工具类（PlacementBehavior、AnchorPoint...） | `BlockEntityRendererFactories` |
| 网络 Payload | GUI、Screen |

### 3.3 什么放在 client

| 可以 | 不可以 |
|------|--------|
| 模型插件（*Plugin.java） | — |
| BakedModel 实现（*BakedModel.java） | — |
| Renderer（SignBlockRenderer） | — |
| Screen / GUI | — |
| datagen | — |

### 3.4 跨源集通信方案

如果服务端需要在特定时机通知客户端做某事（如打开 GUI），使用 **Fabric 事件钩子**：

```java
// client/ 中注册回调，监听服务端事件
UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
    // 客户端代码可以访问 BlockState、BlockPos 等 main 中的类型
    // 但 main 中不能写这段代码——它依赖 UseBlockCallback（client API）
});
```

这是 Fabric 的标准"服务端触发 → 客户端响应"模式。

---

## 4. 类的组合逻辑

### 4.1 接口层次图

```
Block (Minecraft)
  │
  ├── PlacementBehavior (util 包, 放置后处理)
  │     ├── Rotatable → onPlace: with(ROTATION, ...)
  │     └── Directional → onPlace: with(FACING, ...)    ← overrides
  │
  ├── ModelBakeDecorator (空标记, 装饰阶段)
  │     ├── SubModelProvider       → 子模型锚点 + 延迟求值
  │     ├── StatefulTextureProvider → 状态驱动贴图
  │     ├── LightEmissive          → 状态驱动发光
  │     ├── AnimatableModel        → 运行时动画变换
  │     ├── MultiBlockCoordinator  → 多方块组合模型 (待消费方)
  │     ├── SignTextProvider       → 文字层渲染 (需 +BlockEntityProvider)
  │     └── DynamicModelDecorator  → 延迟求值标记 (静态/动态分流)
  │
  ├── ModelWorldTransformer (空标记, 变换阶段)
  │     ├── Rotatable  → getRotationAngle()  16 档 22.5°
  │     │     └── Directional → getRotationAngle() 重写为 90° 步进
  │     └── Offset     → getOffset(BlockState) → {dx, dy, dz}
  │
  ├── Layered         → getRenderLayerType()  CUTOUT / TRANSLUCENT
  ├── Interactive     → getInteraction()      BlockInteraction 函数式处理器
  │     └── GuiInteractive → canOpenGui() / onGuiOpened()
  ├── OffsetFunction  → 静态偏移 @FunctionalInterface
  └── BlockInteraction → 交互处理 @FunctionalInterface
```

### 4.2 CustomBlock —— 唯一的调度中枢

```java
public class CustomBlock extends Block {
    // ① 放置分派
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (this instanceof PlacementBehavior behavior) {
            state = behavior.onPlace(state, ctx);  // Rotatable / Directional 提供实现
        }
        return state;
    }

    // ② 交互分派
    @Override
    public ActionResult onUse(...) {
        if (this instanceof Interactive interactive) {
            BlockInteraction handler = interactive.getInteraction();
            if (handler != null) return handler.interact(...);
            if (interactive.blocksDefaultInteraction()) return ActionResult.CONSUME;
        }
        return super.onUse(...);
    }
}
```

**要点**：
- `CustomBlock` 只知道 `PlacementBehavior` 和 `Interactive` 两个通用接口，不感知 `Directional`、`Rotatable` 等具体类型
- 具体逻辑完全归接口所有，每个接口的 `default` 方法实现自己的职责
- 新增能力 = 新增接口 + CustomBlock 补一行 `instanceof` 检查，零样板代码

### 4.3 客户端模型管线 —— 两阶段 + 插件链

```
ModelPluginRegistry.registerAll()
  │
  ├─ Phase.DECORATOR (先注册 → 内层)
  │   └─ TextBakedModel ← SignTextProvider 方块
  │
  ├─ registerBakeChainBridge() (在 DECORATOR 和 TRANSFORMER 之间)
  │   └─ SubModelBakedModel ← ModelBakeDecorator 方块
  │       └─ 内部执行 5 个 Plugin 链: SubModelComposer → DynamicTexture
  │                                     → AnimationController → Emissive
  │                                     → MultiBlockCoordinator
  │
  └─ Phase.TRANSFORMER (后注册 → 外层)
      ├─ RotationBakedModel ← Rotatable / Directional 方块
      └─ OffsetBakedModel   ← Offset 方块

最终包装顺序（从外到内）:
Offset(Rotation(Text(SubModel(原模型))))
```

**静态 vs 动态分流**：

```java
// 方块实现 DynamicModelDecorator → 动态路径（渲染时按 BlockState 计算子模型/贴图/发光）
// 未实现 DynamicModelDecorator → 静态路径（烘焙时预计算，零渲染时开销）
```

### 4.4 现有方块及其组合

| 方块 | 实现的接口 | 展示的能力 |
|------|-----------|-----------|
| `RotatableBlock` | `Rotatable` | 16 角度旋转 + 自动 placement |
| `RotatableSignBlock` | `Rotatable` | 旋转 + 自定义形状 |
| `RoadSignsBlock` | `Directional, Layered, Offset` | 4 方向 + CUTOUT 渲染 + 空间偏移 |
| `WineCabinetBlock` | `Directional, SubModelProvider, StatefulTextureProvider, LightEmissive, AnimatableModel, Layered` | 全能力展示：子模型 + 动态贴图 + 发光 + 动画 |
| `TestSignBlock` | `Directional, Layered, GuiInteractive, BlockEntityProvider, SignTextProvider` | 文字渲染 + GUI 交互 |

---

## 5. 渲染管线技术方案与当前状态

> 完整技术报告见：[RENDERING_PIPELINE_PROPOSAL.md](./RENDERING_PIPELINE_PROPOSAL.md)

### 5.1 管线分层

```
Layer 0 (main)  接口声明    Rotatable, Directional, SubModelProvider...
Layer 1 (main)  函数式处理器  SubModelHandler, TextureOverrideHandler...
Layer 2 (main)  数据容器    BakedModelSpec, BakeContext, AnchorPoint
Layer 3 (client) 插件链    5 个 ModelBakePlugin，按优先级执行
Layer 4 (client) 后处理    SubModelBakedModel, RotationBakedModel...
```

### 5.2 两阶段时序

```
[Bake 阶段] ModelBakePluginRegistry.bakeChain(block, defaultState, spec)
  SubModelComposer(-1000) → DynamicTexture(-900) → Animation(-500)
  → Emissive(-400) → MultiBlock(100)

[After-Bake 阶段] ModelPluginRegistry.registerAll()
  DECORATOR 层: SubModelBakedModel + TextBakedModel   (内层)
  TRANSFORMER 层: RotationBakedModel + OffsetBakedModel (外层)
  最终: Offset(Rotation(Text/SubModel(Original)))
```

### 5.3 静态/动态分流

```
方块实现了 DynamicModelDecorator?
  ├─ YES → 动态路径: 插件存储 handler 引用，渲染时按实际 BlockState 求值
  └─ NO  → 静态路径: 插件立即调用 handler 写入 BakedModelSpec
             ⚠️ 使用 defaultState，所有状态的模型完全相同！
```

### 5.4 已完成 ✅ vs 待完成 🚧

| 组件 | 状态 | 说明 |
|------|:----:|------|
| 6 个函数式处理器 | ✅ | SubModelHandler, TextureOverrideHandler, EmissiveHandler, AnimationHandler, MultiBlockHandler, ModelBakeHandler |
| 6 个能力接口 | ✅ | 见 §4.1 接口层次图 |
| AnchorPoint + Transform | ✅ | Vec3d position/rotation + float scale |
| BakeContext + BakedModelSpec | ✅ | spec 含 5 个 deferred* 字段 |
| ModelBakePluginRegistry + 5 Plugin | ✅ | 优先级链完整，静态/动态分流逻辑已实现 |
| SubModelBakedModel | ✅ | 消费 deferredSubModelHandler |
| RotationBakedModel + OffsetBakedModel | ✅ | getRotationAngle / getOffset |
| TextBakedModel | ✅ | SignTextProvider |
| DynamicModelDecorator 标记 | ✅ | ModelPluginRegistry 根据此标记分流 |
| **DynamicTextureBakedModel** | 🚧 | handler 已存入 spec，但缺少渲染时消费方 |
| **EmissiveBakedModel** | 🚧 | handler 已存入 spec，但缺少渲染时消费方 |
| **MultiBlock BakedModel** | 🚧 | 接口 + Plugin 已有，缺 BakedModel |

### 5.5 当前架构阻塞点 🔴

> 以下三个 handler 已通过 Plugin 正确存入 `BakedModelSpec.deferred*` 字段，但 `SubModelBakedModel.getQuads()` 仅消费了 `deferredSubModelHandler`，纹理和发光 handler 暂存未读取。

| 阻塞点 | 影响 | 优先级 |
|--------|------|:------:|
| 缺少 DynamicTextureBakedModel | 状态驱动的贴图切换不可用（如 LIT=true → stone→stone_bricks） | 🔴 P0 |
| 缺少 EmissiveBakedModel | 状态驱动的发光等级不可用（如 LIT=true → emissive 15） | 🔴 P0 |
| 缺少 MultiBlock BakedModel | 多方块模型合并不可见 | 🟡 P2 |

### 5.6 技术方案与实现的已知差异

| 差异 | 方案 | 实际 | 影响 |
|------|------|------|------|
| AnchorPoint 预定义常量 | `AnchorPoint.BOTTLE_SLOT_1` | 无预定义常量，需手动 `new` | 缺少开箱即用的锚点库 |
| bake 使用 defaultState | 文档未强调 | `bakeChain(block, block.getDefaultState(), spec)` | 静态路径所有状态共享同一模型 |
| DynamicModelDecorator 必要性 | 未明确要求测试方块实现 | 是实现状态驱动渲染的唯一途径 | 若忘记实现，LIT 等属性变更不会产生视觉效果 |
| MultiBlockHandler 的 World 参数 | 传入 World 实例 | bake 阶段 World 为 null | 只能在渲染时求值，与 bake 阶段语义冲突 |

### 5.7 实施路线图

```
Phase 1 (P0) 补齐消费方
  ├─ DynamicTextureBakedModel → 渲染时调用 deferredTextureOverrideHandler
  ├─ EmissiveBakedModel       → 渲染时调用 deferredEmissiveHandler
  └─ 集成到 ModelPluginRegistry DECORATOR 阶段

Phase 2 (P1) 测试方块
  ├─ TestWineCabinetBlock      → glass + oak_planks 子模型
  ├─ TestLightBulbBlock        → LIT 切换 stone/stone_bricks + 发光
  └─ TestGearBlock             → AnimationHandler 逐帧旋转

Phase 3 (P2) 缓存与优化
  ├─ 模型实例缓存 → 避免重复加载 UnbakedModel
  └─ Quad 缓存    → 静态块跳过重复 bake

Phase 4 (P3) MultiBlock
  └─ MultiBlock BakedModel → 扫描相邻方块合并顶点
```

---

## 6. 新增方块的标准流程

假设要新增一个"展示架"方块：

```
1. 创建 ShowcaseBlock extends CustomBlock
      implements Directional, SubModelProvider, Layered

2. 声明 AnchorPoint 和 SubModelHandler:
      @Override
      public List<AnchorPoint> getAnchors() { return List.of(new AnchorPoint(0.5, 0.2, 0.5, "item")); }
      @Override
      public SubModelHandler getSubModelHandler() { return (state, spec) -> spec.withSubModel(identifier("showcase_item")); }

3. 注册方块:
      ChineseCoreBuildingBlocks.SHOWCASE = register("showcase", new ShowcaseBlock(...), group);

4. 创建 blockstate JSON 和模型 JSON:
      assets/chinese-core-building/blockstates/showcase.json
      assets/chinese-core-building/models/block/showcase_base.json

5. 客户端 零代码改动——Layered 自动注册 RenderLayer，Directional 自动应用旋转，
   SubModelProvider 自动加载子模型
```

---

## 7. 常见陷阱

### 7.1 main 不能引用 client 类

```java
// ❌ main 中这样写会编译失败
import net.minecraft.client.MinecraftClient;

// ✅ 需要在 main 中触发客户端行为的正确方式：
// 使用 Fabric API 的 ObjectShare 或网络包间接通知 client
```

### 7.2 接口 default 方法被类方法屏蔽

```java
// ❌ 不能这样做——Block.getPlacementState() 始终优先于接口 default 方法
interface Directional {
    default BlockState getPlacementState(ItemPlacementContext ctx) { ... }
}

// ✅ 正确做法：使用独立命名的挂钩 + CustomBlock 显式分派
interface PlacementBehavior {
    BlockState onPlace(BlockState baseState, ItemPlacementContext ctx);
}
// CustomBlock.getPlacementState 调用 behavior.onPlace(...)
```

### 7.3 模型包装顺序 = Fabric 注册顺序

```java
// 先注册的 = 内层包装，后注册的 = 外层包装
ModelPluginRegistry.register(Phase.DECORATOR, ...);   // 内层
ModelPluginRegistry.register(Phase.TRANSFORMER, ...); // 外层
// 最终: Transformer(Decorator(Original))
```

### 7.4 渲染上下文在插件注册时不可用

```java
// ❌ 在 register() 回调中获取 RenderContext/MinecraftClient
// ✅ 在 BakedModel.getQuads() / emitBlockQuads() 中获取
```

### 7.5 Fabric 面片发射规范

```java
// ✅ 正确：通过 QuadEmitter 提交面片
emitter.fromVanilla(quad, material, side).emit();

// ❌ 错误（已废弃）：
context.fallbackConsumer().accept(quad);  // 参数类型不匹配
```

### 7.6 BlockState / World 在 main 包中可用

```java
// main 包中的接口可以安全使用这些 Minecraft 通用 API：
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemPlacementContext;
// 这些都在 main sourceSet 范围内，Fabric 不区分
```

### 7.7 动态/静态路径选择

```java
// 若方块需要根据 BlockState 切换贴图/发光/子模型：
public class MyBlock extends CustomBlock
        implements StatefulTextureProvider, DynamicModelDecorator { ... }
//                                         ↑ 必须实现此标记接口！
// 否则所有状态的模型完全相同（使用 defaultState 烘焙）
```

### 7.8 bake 阶段 World = null

```java
// MultiBlockHandler.handle(BlockState, World, BlockPos)
// bake 阶段传入的 World 为 null → 无法在 bake 时查询相邻方块
// 解决：MultiBlock 逻辑必须在渲染时（BakedModel.getQuads()）执行
```

---

## 8. 编译命令

```powershell
# 设置 JDK 21 并构建
$env:JAVA_HOME="E:\Java-jdk\21"
./gradlew build -x test

# 完整构建含测试
./gradlew build
```

---

## 9. 关键文件速查

| 文件 | 作用 |
|------|------|
| `build.gradle` | Loom 17-SNAPSHOT + splitEnvironmentSourceSets |
| `src/main/.../CustomBlock.java` | 统一分派中枢：placement + interaction |
| `src/main/.../properties/model/` | 所有模型能力接口（Rotatable、Directional、SubModelProvider...） |
| `src/main/.../util/PlacementBehavior.java` | 放置挂钩接口（util 包，非渲染能力） |
| `src/main/.../util/BakedModelSpec.java` | 烘焙数据容器 + 5 个延迟求值 handler 字段 |
| `src/main/.../util/BakeContext.java` | 烘焙上下文（block + state + spec） |
| `src/main/.../util/AnchorPoint.java` | 锚点 record（name + position + rotation + scale） |
| `src/main/.../util/handler/` | 6 个函数式处理器接口 |
| `src/client/.../model/ModelPluginRegistry.java` | 两阶段注册入口（DECORATOR → TRANSFORMER） |
| `src/client/.../model/ModelBakePluginRegistry.java` | 5-PLUGIN 烘焙链（按优先级排序） |
| `src/client/.../model/plugin/` | 5 个 BakePlugin 实现 |
| `src/client/.../model/postProcessing/` | 4 个 BakedModel 包装器 |
| `src/client/.../ChineseCoreBuildingClient.java` | 客户端 6 步注册流程 |
| `apiWiki.md` | 完整 API 文档 + 接口层次图 + 使用示例 |
| `HANDOVER_PROMPT.md` | 本文档——项目交接提示词 |
| `RENDERING_PIPELINE_PROPOSAL.md` | 渲染管线技术方案与实施报告（含差异对照、阻塞点、路线图） |