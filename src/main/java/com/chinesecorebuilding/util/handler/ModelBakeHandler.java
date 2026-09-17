package com.chinesecorebuilding.util.handler;

import com.chinesecorebuilding.util.BakeContext;
import com.chinesecorebuilding.util.BakedModelSpec;

/**
 * 模型烘焙处理器。
 * <p>
 * 函数式接口，用于向烘焙插件链提交自定义模型处理逻辑。
 * 实现此接口的 Lambda 或方法引用会在模型烘焙阶段调用，
 * 用于修改或替换当前模型规格（BakedModelSpec）。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * ModelBakeHandler handler = (context, spec) -> {
 *     spec.addSubModel("block/oak_planks", anchor.transform());
 *     return spec;
 * };
 * </pre>
 *
 * @see BakeContext
 * @see BakedModelSpec
 */
@FunctionalInterface
public interface ModelBakeHandler {

    /**
     * 处理模型烘焙。
     * <p>
     * 此方法在模型烘焙阶段调用，接收当前烘焙上下文和模型规格，
     * 可以修改模型规格（如添加子模型、应用贴图覆盖等）并返回。
     * </p>
     *
     * @param context 烘焙上下文，包含方块实例、BlockState 等信息
     * @param spec    当前模型规格，可修改后返回
     * @return 处理后的模型规格
     */
    BakedModelSpec handle(BakeContext context, BakedModelSpec spec);
}