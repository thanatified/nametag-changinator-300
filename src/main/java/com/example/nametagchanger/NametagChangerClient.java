package com.example.nametagchanger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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

        NametagConfig.setOverride(targetName, newName);
        source.sendFeedback(Text.literal("Nametag for " + targetName + " will now show as: " + newName
                + " (client-side only, only you will see this)"));
        return 1;
    }

    private static int executeReset(CommandContext<FabricClientCommandSource> ctx) {
        String targetName = getString(ctx, "player");
        FabricClientCommandSource source = ctx.getSource();

        boolean removed = NametagConfig.removeOverride(targetName);
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
        Map<String, String> overrides = NametagConfig.getAllOverrides();

        if (overrides.isEmpty()) {
            source.sendFeedback(Text.literal("No custom nametags set."));
            return 1;
        }

        source.sendFeedback(Text.literal("Custom nametags:"));
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            source.sendFeedback(Text.literal(" - " + entry.getKey() + " -> " + entry.getValue()));
        }
        return 1;
    }
}
