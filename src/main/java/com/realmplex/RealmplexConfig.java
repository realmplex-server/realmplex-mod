package com.realmplex;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

public class RealmplexConfig {
    private final GsonBuilder builder = new GsonBuilder();
    private final Gson gson = builder
        .setPrettyPrinting()
        .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private File configFile;

    public static class Config {
        public String apiKey;
        public String apiUrl;

        private Config(String apiKey, String apiUrl) {
            this.apiKey = apiKey;
            this.apiUrl = apiUrl;
        }
    }

    public Config loadConfig() {
        Path path = FabricLoader.getInstance().getConfigDir();
        StringBuilder json = new StringBuilder();

        this.configFile = path.resolve("realmplex-mod.json").toFile();

        if (!configFile.exists()) {
            try {
                createConfig();
            } catch (IOException e) {
                RealmplexMod.LOGGER.error("Failed to create config!", e);
            }
        }

        try {
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNextLine()) {
                json.append(scanner.nextLine());
            }

            scanner.close();
        } catch (java.io.FileNotFoundException e) {
            RealmplexMod.LOGGER.error("Failed to load config!", e);
        }

        RealmplexMod.LOGGER.debug("Loaded config");
        return gson.fromJson(json.toString(), Config.class);
    }

    private void createConfig() throws IOException {
        if (configFile.getParentFile().mkdirs()) RealmplexMod.LOGGER.debug("Created config directory");
        Files.createFile(configFile.toPath());

        // TODO write default config
        Config defaultConfig = new Config("", "");
        PrintWriter writer = new PrintWriter(configFile, StandardCharsets.UTF_8);
        writer.write(gson.toJson(defaultConfig));
        writer.close();
    }
}
