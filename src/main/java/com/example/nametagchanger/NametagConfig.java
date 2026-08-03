package com.example.nametagchanger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Purely client-side storage of "UUID -> custom display name" overrides.
 * This never touches any server data; it only changes what is rendered
 * locally above other players' heads in the world.
 */
public final class NametagConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("nametagchanger.json");

    // uuid (as string) -> custom name
    private static final Map<UUID, String> OVERRIDES = new ConcurrentHashMap<>();

    private NametagConfig() {
    }

    public static void load() {
        OVERRIDES.clear();
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            Map<String, String> raw = GSON.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, String> entry : raw.entrySet()) {
                    try {
                        OVERRIDES.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                        // skip malformed UUID entries
                    }
                }
            }
        } catch (IOException e) {
            NametagChangerClient.LOGGER.error("Failed to load nametagchanger.json", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException e) {
            NametagChangerClient.LOGGER.error("Failed to create config directory", e);
            return;
        }

        Map<String, String> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, String> entry : OVERRIDES.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(raw, MAP_TYPE, writer);
        } catch (IOException e) {
            NametagChangerClient.LOGGER.error("Failed to save nametagchanger.json", e);
        }
    }

    /** Returns the custom name for this player's UUID, or null if none is set. */
    public static String getOverride(UUID uuid) {
        return OVERRIDES.get(uuid);
    }

    public static void setOverride(UUID uuid, String customName) {
        OVERRIDES.put(uuid, customName);
        save();
    }

    public static boolean removeOverride(UUID uuid) {
        boolean removed = OVERRIDES.remove(uuid) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public static void clearAll() {
        OVERRIDES.clear();
        save();
    }

    public static Map<UUID, String> getAllOverrides() {
        return Collections.unmodifiableMap(OVERRIDES);
    }
}
