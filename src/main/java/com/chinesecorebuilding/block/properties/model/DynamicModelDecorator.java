package com.chinesecorebuilding.block.properties.model;

/**
 * 动态模型装饰器标记接口。
 * <p>
 * 实现此接口的方块声明其模型装饰数据（子模型、贴图覆盖、发光等级）
 * 依赖方块状态属性（如 FACING、LIT），需要在<b>渲染阶段</b>根据实际
 * BlockState 动态求值，而非烘焙阶段以 defaultState 一次性固化。
 * </p>
 *
 * <h3>两层架构对比</h3>
 * <table>
 *   <caption>静态 vs 动态求值</caption>
 *   <tr><th>维度</th><th>静态 (未实现本接口)</th><th>动态 (实现本接口)</th></tr>
 *   <tr>
 *     <td>求值时机</td>
 *     <td>烘焙阶段，block.getDefaultState()</td>
 *     <td>渲染阶段，当前 BlockState</td>
 *   </tr>
 *   <tr>
 *     <td>运行时开销</td>
 *     <td>零（数据预计算、不可变）</td>
 *     <td>ConcurrentHashMap 缓存查找 O(1)，每状态仅计算一次</td>
 *   </tr>
 *   <tr>
 *     <td>适用场景</td>
 *     <td>所有状态模型数据相同的方块</td>
 *     <td>子模型/贴图/发光随朝向、点亮等属性变化的方块</td>
 *   </tr>
 *   <tr>
 *     <td>子模型</td>
 *     <td>预计算列表，直接遍历</td>
 *     <td>resolveSubModelEntries(state) 按状态查缓存</td>
 *   </tr>
 *   <tr>
 *     <td>贴图覆盖</td>
 *     <td>预计算 Map，不进渲染路径</td>
 *     <td>存储 handler 引用，等 DynamicTextureBakedModel 消费</td>
 *   </tr>
 *   <tr>
 *     <td>发光等级</td>
 *     <td>预计算 int，不进渲染路径</td>
 *     <td>存储 handler 引用，等 EmissiveBakedModel 消费</td>
 *   </tr>
 * </table>
 *
 * <h3>使用方式</h3>
 * <pre>
 * // 静态方块：不实现 DynamicModelDecorator（默认，零运行时开销）
 * public class StonePillarBlock extends CustomBlock implements SubModelProvider {
 *     // handler 总返回相同子模型，烘焙时一次性求值即可
 * }
 *
 * // 动态方块：额外实现 DynamicModelDecorator（标记需要渲染时动态求值）
 * public class WineCabinetBlock extends CustomBlock
 *     implements SubModelProvider, DynamicModelDecorator {
 *     // handler 返回的子模型随 FACING 变化，需要渲染时动态计算
 * }
 * </pre>
 *
 * @see ModelBakeDecorator
 * @see SubModelProvider
 * @see StatefulTextureProvider
 * @see LightEmissive
 */
public interface DynamicModelDecorator extends ModelBakeDecorator {
}