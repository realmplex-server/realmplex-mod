package com.realmplex.mixin;

import com.realmplex.PlayerLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerList.class)
public abstract class LogPlayerLeaveMixin {
    @Inject(method = "remove", at = @At("TAIL"))
    private void logPlayerLeave(ServerPlayer player, CallbackInfo ci) {
        PlayerLogger.logLeave(player.nameAndId().name());
    }
}
