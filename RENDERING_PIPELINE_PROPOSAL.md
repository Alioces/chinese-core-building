# 渲染管线技术方案与实施报告

> 版本：2026-09-18 · 状态：反映当前代码实际实现

---

## 1. 技术方案概述

### 1.1 目标

构建一套**声明式方块渲染管线**：方块通过实现接口声明能力，客户端在模型加载和渲染两个阶段自动识别并应用对应的渲染变换，实现"新增方块 → 零客户端代码改动"。

### 1.2 架构分层

```
┌──────────────────────────────────────────────────────────────┐
│  Layer 0: 接口声明层 (src/main/.../properties/model/)        │
│  Block implements Rotatable / SubModelProvider / ...         │
│  → 方块只管 declare，不关心 who consumes                    │
├──────────────────────────────────────────────────────────────┤
│  Layer 1: 函数式处理器 (src/main/.../util/handler/)          │
│  SubModelHandler, TextureOverrideHandler, EmissiveHandler,   │
│  AnimationHandler, MultiBlockHandler                         │
│  → 方块注入自定义 Lambda，供 Plugin 调用                    │
├──────────────────────────────────────────────────────────────┤
│  Layer 2: 烘焙数据容器 (src/main/.../util/)                  │
│  BakedModelSpec, BakeContext, AnchorPoint, Transform         │
│  → 跨 main↔client 共享的数据结构                             │
├──────────────────────────────────────────────────────────────┤
│  Layer 3: 插件链 (src/client/.../model/plugin/)              │
│  SubModelComposer / DynamicTexture / Animation / Emissive    │
│  / MultiBlock — 5 个 ModelBakePlugin，按优先级排序执行       │
│  → bake 阶段：遍历 handler 填充 BakedModelSpec               │
├──────────────────────────────────────────────────────────────┤
│  Layer 4: 后处理包装 (src/client/.../model/postProcessing/)  │
│  SubModelBakedModel / RotationBakedModel / OffsetBakedModel  │
│  / TextBakedModel                                            │
│  → modifyModelAfterBake 阶段：链式包装原模型                 │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. 渲染管线流程（当前实际实现）

### 2.1 两阶段时序

```
游戏启动 → 模型资源加载
  │
  ├─ [Bake 阶段] ModelBakePluginRegistry.bakeChain()
  │   │  触发条件: block instanceof ModelBakeDecorator
  │   │  调用方式: ModelPluginRegistry.registerBakeChainBridge()
  │   │           → ModelLoadingPlugin.modifyModelAfterBake 回调中
  │   │  输入:     block.getDefaultState()  ← ⚠️ 当前用默认状态
  │   │
  │   ├─ Plugin 1: SubModelComposerPlugin  (优先级 -1000)
  │   │   ├─ 静态路径: handler.handle(anchor, defaultState) → spec.addSubModel()
  │   │   └─ 动态路径: if (DynamicModelDecorator) → spec.registerDeferredSubModelHandler()
  │   │
  │   ├─ Plugin 2: DynamicTexturePlugin    (优先级 -900)
  │   │   ├─ 静态路径: handler.handle(defaultState) → spec.applyTextureOverrides()
  │   │   └─ 动态路径: if (DynamicModelDecorator) → spec.registerDeferredTextureHandler()
  │   │
  │   ├─ Plugin 3: AnimationControllerPlugin (优先级 -500)
  │   │   └─ 始终: spec.setAnimationHandler(handler)
  │   │
  │   ├─ Plugin 4: EmissivePlugin           (优先级 -400)
  │   │   ├─ 静态路径: spec.setEmissiveLevel(handler.handle(defaultState))
  │   │   └─ 动态路径: if (DynamicModelDecorator) → spec.registerDeferredEmissiveHandler()
  │   │
  │   └─ Plugin 5: MultiBlockCoordinatorPlugin (优先级 100)
  │       └─ 始终: spec.addConnectedPart(handler.handle(...))
  │
  └─ [After-Bake 阶段] ModelPluginRegistry.registerAll()
      │  Fabric 回调: modifyModelAfterBake
      │  包装顺序: Offset(Rotation(Text/SubModel(Original)))
      │
      ├─ DECORATOR 层 (先注册 → 内层)
      │   ├─ SubModelBakedModel  ← BakedModelSpec 含 subModels + deferred handlers
      │   └─ TextBakedModel      ← SignTextProvider
      │
      └─ TRANSFORMER 层 (后注册 → 外层)
          ├─ RotationBakedModel  ← Rotatable / Directional
          └─ OffsetBakedModel    ← Offset
```

### 2.2 静态 vs 动态分流

```
方块实现了 DynamicModelDecorator?
  │
  ├─ YES → 动态路径
  │   插件存储 handler 引用到 BakedModelSpec.deferred* 字段
  │   渲染时 SubModelBakedModel 调用 handler 按当前 BlockState 求值
  │   性能开销: 渲染时每次都调用 handler Lambda
  │
  └─ NO  → 静态路径
      插件立即调用 handler 并写入 BakedModelSpec 的最终字段
      渲染时直接使用预计算值，零额外开销
      陷阱: 使用的是 defaultState，所有状态的模型完全相同！
```

---

## 3. 当前实现状态 vs 技术方案差异

### 3.1 已完成项 ✅

| 组件 | 文件 | 状态 |
|------|------|:----:|
| 6 个函数式处理器 | `util/handler/*.java` | ✅ |
| 6 个能力接口 | `properties/model/*.java` | ✅ |
| AnchorPoint + Transform | `util/AnchorPoint.java`, `util/Transform.java` | ✅ |
| BakeContext + BakedModelSpec | `util/BakeContext.java`, `util/BakedModelSpec.java` | ✅ |
| ModelBakePlugin 接口 | `client/model/ModelBakePlugin.java` | ✅ |
| ModelBakePluginRegistry（5 插件链） | `client/model/ModelBakePluginRegistry.java` | ✅ |
| 5 个 Plugin 实现 | `client/model/plugin/*.java` | ✅ |
| SubModelBakedModel | `client/model/postProcessing/SubModelBakedModel.java` | ✅ |
| RotationBakedModel | `client/model/postProcessing/RotationBakedModel.java` | ✅ |
| OffsetBakedModel | `client/model/postProcessing/OffsetBakedModel.java` | ✅ |
| TextBakedModel | `client/model/postProcessing/TextBakedModel.java` | ✅ |
| ModelPluginRegistry 分流 | `client/model/ModelPluginRegistry.java` | ✅ |
| DynamicModelDecorator 标记 | `properties/model/DynamicModelDecorator.java` | ✅ |
| BakedModelSpec 延迟字段 | `deferredSubModelHandler/TextureOverride/Emissive` 字段 | ✅ |
| SubModelComposerPlugin 延迟注册 | `registerDeferredSubModelHandler()` | ✅ |
| DynamicTexturePlugin 延迟注册 | `registerDeferredTextureHandler()` | ✅ |
| EmissivePlugin 延迟注册 | `registerDeferredEmissiveHandler()` | ✅ |

### 3.2 待完成项 🚧

| 组件 | 缺失部分 | 影响 |
|------|----------|------|
| **DynamicTextureBakedModel** | 尚无 BakedModel 消费 `deferredTextureOverrideHandler` | 动态贴图无法按 BlockState 切换 |
| **EmissiveBakedModel** | 尚无 BakedModel 消费 `deferredEmissiveHandler` | 动态发光无法按 BlockState 计算 |
| **MultiBlock BakedModel** | 接口+Plugin 已有，缺渲染时合并模型的 BakedModel | 多方块组合不可见 |

**三个缺失项的共性问题**：handler 已存入 BakedModelSpec，但 `SubModelBakedModel.getQuads()` 只消费了 `deferredSubModelHandler`，Texture/Emissive handler 暂存但未被读取。

### 3.3 技术方案与实现的关键差异

| 差异点 | 方案文档描述 | 实际代码实现 | 影响评估 |
|--------|-------------|-------------|---------|
| AnchorPoint 预定义常量 | `AnchorPoint.BOTTLE_SLOT_1/2/3` 作为预定义静态常量 | `AnchorPoint` 无任何预定义常量，方块自行 `new AnchorPoint(...)` | 用户代码中需自行创建，缺少开箱即用的锚点库 |
| SubModelHandler 签名 | `(anchor, state) -> modelPath` | 完全一致 ✅ | — |
| bake 阶段使用 defaultState | 文档未明确说明 | `ModelBakePluginRegistry.bakeChain(block, block.getDefaultState(), ...)` | 静态路径正确；动态路径 handler 需在渲染时用实际 state 重新求值 |
| DynamicModelDecorator | 文档提及但未强调必要性 | 分流关键：不实现此接口 = 静态路径 = 所有状态同一模型 | 若测试方块不实现此接口，LIT 切换不会改变贴图 |
| EmissivePlugin 延迟注册 | 预期所有动态方块均可延迟 | 当前仅检查 `DynamicModelDecorator`，与 DynamicTexturePlugin 一致 | 逻辑一致，无问题 |

---

## 4. 测试方块配置（与当前架构对齐版）

### 4.1 对比：原始方案 vs 当前架构对齐版

原始测试方案中的 `AnchorPoint.BOTTLE_SLOT_1` 等预定义常量**当前不存在**，需改为：
- 方案 A：在 `AnchorPoint` 中添加预定义常量
- 方案 B：测试方块内联创建（当前 WineCabinetBlock 的做法）

推荐方案 A——一步到位，便于后续复用。但需确认不破坏现有 `WineCabinetBlock` 的独立锚点布局（6 个槽位 vs 3 个槽位）。

### 4.2 测试方块 1：TestWineCabinetBlock（子模型测试）

```java
/**
 * 测试酒柜方块——验证 SubModelComposerPlugin。
 * <p>
 * 基础模型：glass（透明玻璃外壳）
 * 子模型：3 个 oak_planks（模拟酒瓶，缩小至 30%）
 * 实现 DynamicModelDecorator：渲染时按 BlockState 动态求值子模型
 * </p>
 */
public class TestWineCabinetBlock extends CustomBlock
        implements Directional, SubModelProvider, DynamicModelDecorator, Layered {

    private static final List<AnchorPoint> ANCHORS = List.of(
        new AnchorPoint("bottle_slot_1", new Vec3d(0.25, 0.35, 0.5), Vec3d.ZERO, 0.3f),
        new AnchorPoint("bottle_slot_2", new Vec3d(0.50, 0.35, 0.5), Vec3d.ZERO, 0.3f),
        new AnchorPoint("bottle_slot_3", new Vec3d(0.75, 0.35, 0.5), Vec3d.ZERO, 0.3f)
    );

    public TestWineCabinetBlock(Settings settings) {
        super(settings);
        setDefaultState(initDirection(getDefaultState()));
    }

    @Override public List<AnchorPoint> getAnchors() { return ANCHORS; }

    @Override
    public SubModelHandler getSubModelHandler() {
        return (anchor, state) -> switch (anchor.name()) {
            case "bottle_slot_1" -> "minecraft:oak_planks";
            case "bottle_slot_2" -> "minecraft:spruce_planks";
            case "bottle_slot_3" -> "minecraft:birch_planks";
            default -> null;
        };
    }

    @Override public RenderLayerType getRenderLayerType() { return RenderLayerType.CUTOUT; }
    @Override public void appendProperties(...) { super.appendProperties(builder); Directional.super.appendProperties(builder); }
}
```

**blockstate JSON 要点**：
```json
{ "variants": { "facing=north": { "model": "chinese-core-building:block/glass" }, ... } }
```

### 4.3 测试方块 2：TestLightBulbBlock（贴图 + 发光测试）

```java
/**
 * 测试灯泡方块——验证 DynamicTexturePlugin + EmissivePlugin。
 * <p>
 * LIT=false: stone 贴图, 发光 0
 * LIT=true:  stone_bricks 贴图, 发光 15
 * 实现 DynamicModelDecorator：渲染时按实际 LIT 状态动态切换贴图和发光等级
 * </p>
 */
public class TestLightBulbBlock extends CustomBlock
        implements StatefulTextureProvider, LightEmissive, DynamicModelDecorator, Layered {

    public static final BooleanProperty LIT = Properties.LIT;

    public TestLightBulbBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LIT, false));
    }

    @Override public TextureOverrideHandler getTextureOverrideHandler() {
        return (state) -> state.get(LIT)
            ? Map.of("#all", "minecraft:block/stone_bricks")
            : Map.of("#all", "minecraft:block/stone");
    }

    @Override public EmissiveHandler getEmissiveHandler() {
        return (state) -> state.get(LIT) ? 15 : 0;
    }

    @Override public RenderLayerType getRenderLayerType() { return RenderLayerType.CUTOUT; }
    @Override public void appendProperties(...) { super.appendProperties(builder); builder.add(LIT); }
}
```

**⚠️ 架构阻塞点**：此方块正确运行需要 `DynamicTextureBakedModel` 和 `EmissiveBakedModel` 两个组件完成（当前 🚧）。

---

## 5. 实施路线图

### 阶段一：补齐渲染层消费方（阻塞项，优先）

| 步骤 | 任务 | 说明 |
|:----:|------|------|
| 5.1 | 实现 `DynamicTextureBakedModel` | 渲染时从 `deferredTextureOverrideHandler` 获取贴图覆盖，替换 Quad 材质 |
| 5.2 | 实现 `EmissiveBakedModel` | 渲染时从 `deferredEmissiveHandler` 获取发光等级，设置 Quad 自发光标志 |
| 5.3 | 集成到 `ModelPluginRegistry` | 在 DECORATOR 阶段注册两个新 BakedModel 包装器 |

### 阶段二：测试方块实现

| 步骤 | 任务 | 依赖 |
|:----:|------|------|
| 5.4 | 创建 `TestWineCabinetBlock` | 阶段一完成 |
| 5.5 | 创建 `TestLightBulbBlock` | 5.1 + 5.2 完成 |
| 5.6 | 创建 `TestGearBlock`（动画测试）| 验证 AnimationHandler 的 `List<Transform>` 逐帧更新 |

### 阶段三：缓存与优化

| 步骤 | 任务 |
|:----:|------|
| 5.7 | 模型实例缓存（Caffeine 或 HashBiMap）→ 避免重复加载同一 Identifier 的 UnbakedModel |
| 5.8 | Quad 缓存 → 静态路径方块缓存 `List<BakedQuad>`，跳过重复 bake 调用 |

### 阶段四：MultiBlock

| 步骤 | 任务 |
|:----:|------|
| 5.9 | 实现 MultiBlock BakedModel → 渲染时扫描相邻方块，合并顶点到当前模型 |

---

## 6. 架构风险与技术债务

### 6.1 已知风险

| 风险 | 严重度 | 说明 |
|------|:------:|------|
| bake 阶段只用 defaultState | ⚠️ 中 | 静态路径方块的所有 BlockState 实例共享同一个 BakedModelSpec（贴图/子模型完全一致）。若某方块需要按状态切换贴图，必须实现 `DynamicModelDecorator` |
| SubModelBakedModel 未消费 Texture/Emissive handler | 🔴 高 | handler 已正确存入 spec，但渲染时未读取——动态贴图和发光功能实际上不可用 |
| AnchorPoint 缺少预定义常量 | 🟡 低 | 每个测试方块手动 new 锚点，不符合"开箱即用"目标 |

### 6.2 技术债务

1. **subModelProvider → SubModelComposerPlugin 中的两重路径混合**：静态立即求值 + 延迟 handler 注册在同一个 plugin 中耦合，后续可拆分为 `StaticSubModelComposerPlugin` + `DeferredSubModelComposerPlugin`
2. **MultiBlockHandler 需要 World 参数**：`handle(BlockState, World, BlockPos)` 中的 World 在 bake 阶段为 null——MultiBlock 只能在渲染时按实际 World 求值，与当前架构的 bake 阶段语义冲突

---

## 7. 附录：原版模型纹理变量参考

| 模型 | JSON 路径 | 纹理变量 | 可用贴图 |
|------|----------|----------|----------|
| `cube_all` | `block/cube_all` | `#all` | 任意 16×16 贴图 |
| `cube` | `block/cube` | `#up`, `#down`, `#north`, `#south`, `#east`, `#west` | 各面独立 |
| `cube_column` | `block/cube_column` | `#end`, `#side` | 顶底 + 侧面 |
| `glass` | `block/glass`（继承 `cube_all`） | `#all` | `block/glass` |
| `stone` | `block/stone`（继承 `cube_all`） | `#all` | `block/stone` |
| `stone_bricks` | `block/stone_bricks`（继承 `cube_all`） | `#all` | `block/stone_bricks` |
| `oak_planks` | `block/oak_planks`（继承 `cube_all`） | `#all` | `block/oak_planks` |