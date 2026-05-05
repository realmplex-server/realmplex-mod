package com.realmplex;

import com.realmplex.RealmplexConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurrencyConfig {
    public Map<String, CurrencyItemConfig> currencies = new HashMap<>();
    public List<PairConfig> pairs = new ArrayList<>();

    public static class PairConfig {
        public String currency;
        public String rawItem;
        public int rate;
    }

    public static class CurrencyItemConfig {
        public String item;
        public String displayName;
        public String color;
        public boolean glint;
        public String itemModel;
        public Map<String, Object> extraNbt;
    }

    private static final RealmplexConfig<CurrencyConfig> HANDLER =
            new RealmplexConfig<>("realmplex/currency.json", defaultConfig(), CurrencyConfig.class);

    public static CurrencyConfig load() {
        return HANDLER.load();
    }

    private static CurrencyConfig defaultConfig() {
        CurrencyConfig config = new CurrencyConfig();

        CurrencyItemConfig usd = new CurrencyItemConfig();
        usd.item = "minecraft:paper";
        usd.displayName = "US Dollar";
        usd.color = "#5555FF";
        usd.glint = true;
        usd.itemModel = "minecraft:filled_map";
        usd.extraNbt = Map.of("usd", true);
        config.currencies.put("usd", usd);

        config.pairs.add(pair("usd", "minecraft:netherite_ingot", 64));
        config.pairs.add(pair("usd", "minecraft:netherite_scrap", 16));
        return config;
    }

    private static PairConfig pair(String currency, String rawItem, int rate) {
        PairConfig p = new PairConfig();
        p.currency = currency;
        p.rawItem = rawItem;
        p.rate = rate;
        return p;
    }
}