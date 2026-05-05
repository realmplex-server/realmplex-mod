package com.realmplex;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
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
        CurrencyConfig config = CurrencyConfig.load();

        for (CurrencyConfig.PairConfig pair : config.pairs) {
            CurrencyConfig.CurrencyItemConfig c = config.currencies.get(pair.currency);

            if (c == null) {
                RealmplexMod.LOGGER.warn("Unknown currency key '{}', skipping pair", pair.currency);
                continue;
            }

            Item currencyItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(c.item));
            Item rawItem      = BuiltInRegistries.ITEM.getValue(Identifier.parse(pair.rawItem));

            if (currencyItem == Items.AIR || rawItem == Items.AIR) {
                RealmplexMod.LOGGER.warn("Unknown item in pair '{}: {}', skipping", pair.currency, pair.rawItem);
                continue;
            }

            int color;
            try {
                color = Integer.parseInt(c.color.replace("#", ""), 16);
            } catch (NumberFormatException e) {
                RealmplexMod.LOGGER.warn("Invalid color '{}' for currency '{}', defaulting to white", c.color, pair.currency);
                color = 0xFFFFFF;
            }

            Map<String, Object> extraNbt = c.extraNbt != null ? c.extraNbt : Map.of();

            PAIRS.add(new ExchangePair(
                    new CurrencyItem(currencyItem, pair.currency, c.displayName, color, c.glint, c.itemModel, extraNbt),
                    rawItem,
                    pair.rate
            ));
        }

        RealmplexMod.LOGGER.info("Loaded {} currency exchange pairs", PAIRS.size());
    }

    public static void register() {
        loadPairs();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.<CommandSourceStack>literal("exchange")
                    .requires(source -> {
                        boolean opAllowed = source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
                        boolean isCommandBlock = source.getEntity() == null;
                        return opAllowed || isCommandBlock;
                    })
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

        context.getSource().sendFailure(Component.literal("no valid exchange for " + currencyKey));
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