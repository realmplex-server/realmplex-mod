package com.realmplex;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public class ItemFlexer {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.<CommandSourceStack>literal("flex")
                    .executes(ItemFlexer::itemFlex)
            );
        });
    }

    private static int itemFlex(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        ServerPlayer player = commandSourceStackCommandContext.getSource().getPlayer();
        if (player == null) {
            commandSourceStackCommandContext.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            commandSourceStackCommandContext.getSource().sendFailure(Component.literal("You are not holding an item"));
            return 0;
        }

        Component heldItem  = held.getStyledHoverName().copy().withStyle(style -> style
                        .withHoverEvent(new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack(held))));

        commandSourceStackCommandContext.getSource().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(player.getScoreboardName())
                        .append(Component.literal(" is flexing their: "))
                        .append(heldItem),
                false
        );
        return 1;
    }
}
