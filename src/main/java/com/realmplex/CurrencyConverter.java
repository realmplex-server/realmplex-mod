package com.realmplex;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CurrencyConverter {
    private record CurrencyItem(
        Item item,
        String nbtKey,
        String displayName,
        int color,
        boolean glint,
        String itemModel,
        Map<String, Object> extraNbt
    ) {}

    private record ExchangePair(
            CurrencyItem currency,
            Item rawItem,
            int rate
    ) {}

    private static final List<ExchangePair> PAIRS = new ArrayList<>();

    private static void loadPairs() {
        PAIRS.clear();

        CurrencyItem usd = new CurrencyItem(
                Items.PAPER,
                "usd",
                "US Dollar",
                0x5555FF,
                true,
                "minecraft:filled_map",
                Map.of("usd", true)
        );

        PAIRS.add(new ExchangePair(usd, Items.NETHERITE_INGOT, 64));
        PAIRS.add(new ExchangePair(usd, Items.NETHERITE_SCRAP, 16));

        CurrencyItem spud = new CurrencyItem(
                Items.POTATO,
                "spud",
                "Spud",
                0xFFFF55,
                true,
                null,
                Map.of("spud", true)
        );

        PAIRS.add(new ExchangePair(spud, Items.NETHERITE_INGOT, 64));
        PAIRS.add(new ExchangePair(spud, Items.NETHERITE_SCRAP, 16));

        CurrencyItem mash = new CurrencyItem(
                Items.PACKED_MUD,
                "mash",
                "Mash",
                0xFF5555,
                true,
                null,
                Map.of("mash", true)
        );

        PAIRS.add(new ExchangePair(mash, Items.NETHERITE_INGOT, 32));

    }

    public static void register() {
        loadPairs();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.<CommandSourceStack>literal("exchange")
                    .executes(CurrencyConverter::executeExchange)
                    .then(Commands.argument("currency", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                PAIRS.stream()
                                        .map(p -> p.currency().nbtKey())
                                        .distinct()
                                        .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(CurrencyConverter::executeExchange)
                    )
                    .then(Commands.literal("reload")
                            .executes(CurrencyConverter::executeReload)
                    )
            );
        });
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        PAIRS.clear();
        loadPairs();
        context.getSource().sendSuccess(() -> Component.literal("reloaded"), false);
        return 1;
    }

    private static int executeExchange(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("not a player"));
            return 0;
        }

        String currencyKey = StringArgumentType.getString(context, "currency");
        ItemStack held = player.getMainHandItem();

        for (ExchangePair pair : PAIRS) {
            if (!pair.currency().nbtKey().equals(currencyKey)) continue;

            if (held.is(pair.rawItem()) && held.getCount() >= 1) {
                return convert(player, held, 1, buildCurrency(pair), pair.rate());
            }

            if (held.is(pair.currency().item()) && held.getCount() >= pair.rate()) {
                CustomData heldData = held.get(DataComponents.CUSTOM_DATA);
                if (heldData == null || !heldData.copyTag().contains(pair.currency().nbtKey())) continue;
                return convert(player, held, pair.rate(), new ItemStack(pair.rawItem(), 1), 1);
            }
        }

        context.getSource().sendFailure(Component.literal("no valid exchange" + currencyKey));
        return 0;
    }

    private static Component itemComponent(ItemStack stack) {
        ChatFormatting rarityColor = stack.getRarity().color();
        ChatFormatting displayColor = (rarityColor == ChatFormatting.WHITE) ? ChatFormatting.GREEN : rarityColor;

        Component name = stack.has(DataComponents.CUSTOM_NAME)
                ? stack.getHoverName()
                : stack.getHoverName().copy().withStyle(displayColor);

        return name.copy().withStyle(style -> style
                .withHoverEvent(new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack(stack)))
        );
    }

    private static int convert(ServerPlayer player, ItemStack held, int cost, ItemStack output, int yield) {
        Component inputComponent  = itemComponent(held);
        Component outputComponent = itemComponent(output);

        held.shrink(cost);
        player.addItem(output);

        player.sendSystemMessage(
                Component.literal(String.format("Converted %d ", cost))
                        .append(inputComponent)
                        .append(Component.literal(String.format(" into %d ", yield)))
                        .append(outputComponent)
        );
        return 1;
    }

    private static ItemStack buildCurrency(ExchangePair pair) {
        CurrencyItem currency = pair.currency();
        ItemStack output = new ItemStack(currency.item(), pair.rate());

        CompoundTag tag = new CompoundTag();
        tag.putBoolean(currency.nbtKey(), true);

        for (Map.Entry<String, Object> entry : currency.extraNbt().entrySet()) {
            if (entry.getValue() instanceof Boolean b)   tag.putBoolean(entry.getKey(), b);
            else if (entry.getValue() instanceof Integer i) tag.putInt(entry.getKey(), i);
            else if (entry.getValue() instanceof String s)  tag.putString(entry.getKey(), s);
            else if (entry.getValue() instanceof Float f)   tag.putFloat(entry.getKey(), f);
        }

        output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        output.set(DataComponents.CUSTOM_NAME,
                Component.literal(currency.displayName()).withStyle(s -> s
                        .withColor(currency.color())
                        .withItalic(false)
                )
        );


        if (currency.glint() ) {
            output.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        if (currency.itemModel() != null) {
            output.set(DataComponents.ITEM_MODEL, Identifier.parse(currency.itemModel()));
        }

        return output;
    }
}