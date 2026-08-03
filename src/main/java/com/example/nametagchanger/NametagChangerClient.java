package com.example.nametagchanger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class NametagChangerClient implements ClientModInitializer {

    public static final String MOD_ID = "nametagchanger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        NametagConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("nametag")
                        .then(literal("set")
                                .then(argument("player", word())
                                        .then(argument("newName", StringArgumentType.greedyString())
                                                .executes(NametagChangerClient::executeSet))))
                        .then(literal("reset")
                                .then(argument("player", word())
                                        .executes(NametagChangerClient::executeReset)))
                        .then(literal("resetall")
                                .executes(NametagChangerClient::executeResetAll))
                        .then(literal("list")
                                .executes(NametagChangerClient::executeList))
                ));

        LOGGER.info("Nametag Changer initialized (client-side only).");
    }

    private static int executeSet(CommandContext<FabricClientCommandSource> ctx) {
        String targetName = getString(ctx, "player");
        String newName = getString(ctx, "newName");
        FabricClientCommandSource source = ctx.getSource();

        UUID uuid = resolveUuid(source, targetName);
        if (uuid == null) {
            source.sendError(Text.literal("Couldn't find a player named '" + targetName
                    + "' nearby. They need to be visible/loaded (e.g. in the tab list) first."));
            return 0;
        }

        NametagConfig.setOverride(uuid, newName);
        source.sendFeedback(Text.literal("Nametag for " + targetName + " will now show as: " + newName
                + " (client-side only, only you will see this)"));
        return 1;
    }

    private static int executeReset(CommandContext<FabricClientCommandSource> ctx) {
        String targetName = getString(ctx, "player");
        FabricClientCommandSource source = ctx.getSource();

        UUID uuid = resolveUuid(source, targetName);
        if (uuid == null) {
            source.sendError(Text.literal("Couldn't find a player named '" + targetName + "' nearby."));
            return 0;
        }

        boolean removed = NametagConfig.removeOverride(uuid);
        if (removed) {
            source.sendFeedback(Text.literal("Reset nametag override for " + targetName + "."));
        } else {
            source.sendFeedback(Text.literal(targetName + " doesn't have a custom nametag set."));
        }
        return 1;
    }

    private static int executeResetAll(CommandContext<FabricClientCommandSource> ctx) {
        NametagConfig.clearAll();
        ctx.getSource().sendFeedback(Text.literal("Cleared all custom nametags."));
        return 1;
    }

    private static int executeList(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        Map<UUID, String> overrides = NametagConfig.getAllOverrides();

        if (overrides.isEmpty()) {
            source.sendFeedback(Text.literal("No custom nametags set."));
            return 1;
        }

        source.sendFeedback(Text.literal("Custom nametags:"));
        MinecraftClient client = MinecraftClient.getInstance();
        for (Map.Entry<UUID, String> entry : overrides.entrySet()) {
            String realName = entry.getKey().toString();
            if (client.getNetworkHandler() != null) {
                PlayerListEntry plEntry = client.getNetworkHandler().getPlayerListEntry(entry.getKey());
                if (plEntry != null) {
                    realName = plEntry.getProfile().getName();
                }
            }
            source.sendFeedback(Text.literal(" - " + realName + " -> " + entry.getValue()));
        }
        return 1;
    }

    /**
     * Resolves a UUID from a typed name by checking the current tab list
     * (network handler's player list), which is populated for every player
     * the client currently knows about.
     */
    private static UUID resolveUuid(FabricClientCommandSource source, String typedName) {
        if (source.getClient().getNetworkHandler() == null) {
            return null;
        }
        for (PlayerListEntry entry : source.getClient().getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().getName().equalsIgnoreCase(typedName)) {
                return entry.getProfile().getId();
            }
        }
        // Fall back: maybe the user directly typed a raw UUID.
        try {
            return UUID.fromString(typedName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
