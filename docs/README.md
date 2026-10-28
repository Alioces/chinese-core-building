# Chinese Core Building

> 面向中式建筑主题的装饰性方块模组（Minecraft 1.20.1 + Fabric Loader 0.16+）

## 设计理念

**方块只声明能力（通过接口），客户端统一扫描接口并自动注册。**
新增方块类型不改任何客户端代码。

## 核心接口（能力声明）

| 接口 | 能力 | 消费方 |
|------|------|--------|
| `Rotatable` | 16 档任意角度旋转（22.5°/档） | `RotationBakedModel` |
| `Directional` | 4 方向水平朝向（90°/档） | `RotationBakedModel` |
| `Offset` | 3D 空间偏移 | `OffsetBakedModel` + `OffsetDecorator` |
| `Layered` | 渲染层声明（CUTOUT/TRANSLUCENT） | `BlockRenderLayerMap` |
| `SignTextProvider` | 文字层渲染能力 | `TextBakedModel` + `TextBlockRenderer` |
| `GuiInteractive` | 客户端 GUI 交互 | `GuiScreenRegistry` + `UseBlockCallback` |

## 项目结构

```
src/
├── main/java/          ← 服务端：Block、BlockEntity、接口、工具类
└── client/java/        ← 客户端：模型管线、装饰器、渲染器、GUI、数据生成
```

## 文档

| 文档 | 内容 |
|------|------|
| [apiWiki.md](docs/apiWiki.md) | 完整 API 文档 + 接口层次图 + 使用示例 |
| [HANDOVER_PROMPT.md](docs/HANDOVER_PROMPT.md) | 项目交接提示词（设计方向、常见陷阱） |
| [RENDERING_PIPELINE_PROPOSAL.md](docs/RENDERING_PIPELINE_PROPOSAL.md) | 渲染管线技术方案与实施报告 |
| [CODE_PHILOSOPHY.md](docs/CODE_PHILOSOPHY.md) | 代码编写规范（20 条实施指南） |

## 编译

```powershell
$env:JAVA_HOME="E:\Java-jdk\21"
./gradlew build -x test
```

> Loom 1.17-SNAPSHOT 需要 JDK 21 运行 Gradle，但编译目标为 Java 17。