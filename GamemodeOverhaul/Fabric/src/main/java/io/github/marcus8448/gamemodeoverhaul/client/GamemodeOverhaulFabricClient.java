package io.github.marcus8448.gamemodeoverhaul.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import io.github.marcus8448.gamemodeoverhaul.GamemodeOverhaulCommon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;

public class GamemodeOverhaulFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            if (GamemodeOverhaulCommon.CONFIG.enableGamemode()) registerGamemode(dispatcher);
            if (GamemodeOverhaulCommon.CONFIG.enableGm()) registerGm(dispatcher);
            if (GamemodeOverhaulCommon.CONFIG.enableNoArgsGm()) registerGmNoArgs(dispatcher);
            if (GamemodeOverhaulCommon.CONFIG.enableDefaultGamemode()) registerDefaultGamemode(dispatcher);
            if (GamemodeOverhaulCommon.CONFIG.enableDgm()) registerDgm(dispatcher);
            if (GamemodeOverhaulCommon.CONFIG.enableDifficulty()) registerDifficulty(dispatcher);
            if (GamemodeOverhaulCommon.CONFIG.enableToggledownfall()) registerToggleDownfall(dispatcher);

            GamemodeOverhaulCommon.LOGGER.info("Client commands registered!");
        });
    }

    private static void registerGamemode(@Nonnull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal("gamemode")
                .requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(ClientCommandManager.literal(type.getName())
                            .executes(context -> redirectToServer(context, "gamemode " + type.getName()))
                            .then(ClientCommandManager.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer(context, "gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(ClientCommandManager.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer(context, "gamemode " + type.getName()))
                            .then(ClientCommandManager.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer(context, "gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(ClientCommandManager.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer(context, "gamemode " + type.getName()))
                            .then(ClientCommandManager.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer(context, "gamemode " + type.getName() + ' ' + captureLastArgument(context)))));
        }
        dispatcher.register(node);
    }

    private static void registerGm(@Nonnull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal("gm")
                .requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(ClientCommandManager.literal(type.getName())
                            .executes(context -> redirectToServer(context, "gamemode " + type.getName()))
                            .then(ClientCommandManager.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer(context, "gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(ClientCommandManager.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer(context, "gamemode " + type.getName()))
                            .then(ClientCommandManager.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer(context, "gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(ClientCommandManager.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer(context, "gamemode " + type.getName()))
                            .then(ClientCommandManager.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer(context, "gamemode " + type.getName() + ' ' + captureLastArgument(context)))));
        }
        dispatcher.register(node);
    }

    private static void registerGmNoArgs(@Nonnull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        for (GameType type : GameType.values()) {
            dispatcher.register(ClientCommandManager.literal("gm" + GamemodeOverhaulCommon.createShort(type))
                    .requires(stack -> stack.hasPermission(2))
                    .executes(context -> redirectToServer(context, "gamemode " + type.getName()))
                    .then(ClientCommandManager.argument("target", EntityArgument.players())
                            .executes(context -> redirectToServer(context, "gamemode " + type.getName() + ' ' + captureLastArgument(context)))));
        }
    }

    private static void registerDefaultGamemode(@Nonnull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal("defaultgamemode").requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(ClientCommandManager.literal(type.getName())
                            .executes(context -> redirectToServer(context, "defaultgamemode " + type.getName())))
                    .then(ClientCommandManager.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer(context, "defaultgamemode " + type.getName())))
                    .then(ClientCommandManager.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer(context, "defaultgamemode " + type.getName())));
        }
        dispatcher.register(node);
    }

    private static void registerDgm(@Nonnull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal("dgm")
                .requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(ClientCommandManager.literal(type.getName())
                            .executes(context -> redirectToServer(context, "defaultgamemode " + type.getName())))
                    .then(ClientCommandManager.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer(context, "defaultgamemode " + type.getName())))
                    .then(ClientCommandManager.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer(context, "defaultgamemode " + type.getName())));
        }
        dispatcher.register(node);
    }

    private static void registerDifficulty(@Nonnull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal("difficulty").requires(stack -> stack.hasPermission(2));
        for (Difficulty value : Difficulty.values()) {
             node.then(ClientCommandManager.literal(value.getKey())
                             .executes(context -> redirectToServer(context, "difficulty " + value.getKey())))
                     .then(ClientCommandManager.literal(String.valueOf(value.ordinal()))
                             .executes(context -> redirectToServer(context, "difficulty " + value.getKey())));
        }
        dispatcher.register(node);
    }

    private static void registerToggleDownfall(@Nonnull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("toggledownfall").requires(stack -> stack.hasPermission(2))
                .executes((context) -> toggleDownfall(context.getSource())));
    }

    private static int toggleDownfall(@Nonnull FabricClientCommandSource source) {
        ClientLevel level = source.getWorld();
        if (level.isRaining() || level.getLevelData().isRaining() || level.isThundering() || level.getLevelData().isThundering()) {
            source.getPlayer().commandUnsigned("weather clear");
        } else {
            source.getPlayer().commandUnsigned("weather rain");
        }
        return 1;
    }
    
    private static int redirectToServer(@NotNull CommandContext<FabricClientCommandSource> source, @NotNull String command) {
        LocalPlayer player = source.getSource().getPlayer();
        if (player.commandHasSignableArguments(command)) {
            return -1;
        }
        LastSeenMessages.Update update = player.connection.generateMessageAcknowledgements();
        player.connection.send(new ServerboundChatCommandPacket(command, Instant.now(), 0L, ArgumentSignatures.EMPTY, false, update));
        return 1;
    }

    @Contract(pure = true)
    private static @NotNull String captureLastArgument(@NotNull CommandContext<FabricClientCommandSource> source) {
        List<ParsedCommandNode<FabricClientCommandSource>> nodes = source.getNodes();
        StringRange node = nodes.get(nodes.size() - 1).getRange();
        return source.getInput().substring(node.getStart(), node.getEnd());
    }
}
