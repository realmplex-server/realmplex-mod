package com.realmplex.mixin;

import carpet.patches.EntityPlayerMPFake;
import me.senseiwells.puppet.PuppetPlayer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class BotTeamMixin {

	private static final String MOD_ID = "realmplex-mod";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final String BOT_TEAM_NAME = "bot";

	@Shadow @Final
	private MinecraftServer server;

	@Inject(method = "placeNewPlayer", at = @At("HEAD"))
	private void addFakePlayerToTeam(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		if ((player instanceof EntityPlayerMPFake) || (player instanceof PuppetPlayer)) {
			Scoreboard scoreboard = this.server.getScoreboard();
			PlayerTeam team = scoreboard.getPlayerTeam(BOT_TEAM_NAME);
			if (team == null) {
				LOGGER.warn("Cannot find team for fake player, expecting team: " + BOT_TEAM_NAME);
				return;
			}

			scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
		}
	}

	@Inject(method = "remove", at = @At("HEAD"))
	private void removeFakePlayerFromTeam(ServerPlayer player, CallbackInfo ci) {
		if ((player instanceof EntityPlayerMPFake) || (player instanceof PuppetPlayer)) {
			Scoreboard scoreboard = this.server.getScoreboard();
			PlayerTeam team = scoreboard.getPlayerTeam(BOT_TEAM_NAME);
			if (team == null) {
				LOGGER.warn("Cannot find team for fake player, expecting team: " + BOT_TEAM_NAME);
				return;
			}
			scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
		}
	}
}