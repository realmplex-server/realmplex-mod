package com.realmplex.mixin;

import com.realmplex.PlayerLogger;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class LogServerStartupMixin {
    @Inject(method = "initServer", at = @At("TAIL"))
    private void logServerStartup(CallbackInfoReturnable<Boolean> cir) {
        PlayerLogger.logStartup();
    }
}