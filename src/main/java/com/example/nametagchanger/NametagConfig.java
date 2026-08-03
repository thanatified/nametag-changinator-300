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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Purely client-side storage of "username -> custom display name" overrides.
 * Keyed by username (lowercased for lookups) since that's what's actually
 * available on the per-frame PlayerEntityRenderState we hook into - it does
 * not carry the player's UUID. This never touches any server data; it only
 * changes what is rendered locally above other players' heads.
 */
public final class NametagConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("nametagchanger.json");

    // lowercased username -> custom name
    private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();

    private NametagConfig() {
    }

    private static String key(String username) {
        return username.toLowerCase(Locale.ROOT);
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
                    OVERRIDES.put(key(entry.getKey()), entry.getValue());
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

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(OVERRIDES, MAP_TYPE, writer);
        } catch (IOException e) {
            NametagChangerClient.LOGGER.error("Failed to save nametagchanger.json", e);
        }
    }

    /** Returns the custom name for this username, or null if none is set. */
    public static String getOverride(String username) {
        if (username == null) {
            return null;
        }
        return OVERRIDES.get(key(username));
    }

    public static void setOverride(String username, String customName) {
        OVERRIDES.put(key(username), customName);
        save();
    }

    public static boolean removeOverride(String username) {
        boolean removed = OVERRIDES.remove(key(username)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public static void clearAll() {
        OVERRIDES.clear();
        save();
    }

    public static Map<String, String> getAllOverrides() {
        return Collections.unmodifiableMap(OVERRIDES);
    }
}
