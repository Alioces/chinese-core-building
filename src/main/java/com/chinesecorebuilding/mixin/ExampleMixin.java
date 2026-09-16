package com.chinesecorebuilding.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务端 Mixin 示例占位类。
 * <p>
 * Fabric 模板自动生成的模板文件，演示如何使用 SpongePowered Mixin
 * 在 {@link MinecraftServer#loadWorld} 方法头部注入自定义逻辑。
 * 本模组目前未使用此类，保留作为 Mixin 编写参考。
 * </p>
 *
 * @see MinecraftServer
 */
@Mixin(MinecraftServer.class)
public class ExampleMixin {

    /**
     * 在 {@link MinecraftServer#loadWorld} 方法头部注入回调。
     * <p>
     * 当服务端开始加载世界时，此注入逻辑会被最先执行。
     * 当前为空实现，可按需扩展。
     * </p>
     *
     * @param info Mixin 回调信息，可用于取消原方法执行或修改返回值
     */
    @Inject(at = @At("HEAD"), method = "loadWorld")
    private void init(CallbackInfo info) {
        // 在此处插入服务端世界加载时的自定义逻辑
    }
}