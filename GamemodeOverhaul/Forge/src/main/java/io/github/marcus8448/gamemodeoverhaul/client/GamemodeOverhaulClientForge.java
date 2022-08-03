package io.github.marcus8448.gamemodeoverhaul.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import io.github.marcus8448.gamemodeoverhaul.GamemodeOverhaulCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;

public class GamemodeOverhaulClientForge {
    public static void registerClientCommands(@NotNull RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        if (GamemodeOverhaulCommon.CONFIG.enableGamemode()) registerGamemode(dispatcher);
        if (GamemodeOverhaulCommon.CONFIG.enableGm()) registerGm(dispatcher);
        if (GamemodeOverhaulCommon.CONFIG.enableNoArgsGm()) registerGmNoArgs(dispatcher);
        if (GamemodeOverhaulCommon.CONFIG.enableDefaultGamemode()) registerDefaultGamemode(dispatcher);
        if (GamemodeOverhaulCommon.CONFIG.enableDgm()) registerDgm(dispatcher);
        if (GamemodeOverhaulCommon.CONFIG.enableDifficulty()) registerDifficulty(dispatcher);
        if (GamemodeOverhaulCommon.CONFIG.enableToggledownfall()) registerToggleDownfall(dispatcher);

        GamemodeOverhaulCommon.LOGGER.info("Client commands registered!");
    }


    private static void registerGamemode(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("gamemode")
                .requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(Commands.literal(type.getName())
                            .executes(context -> redirectToServer("gamemode " + type.getName()))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer("gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(Commands.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer("gamemode " + type.getName()))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer("gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(Commands.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer("gamemode " + type.getName()))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer("gamemode " + type.getName() + ' ' + captureLastArgument(context)))));
        }
        dispatcher.register(node);
    }

    private static void registerGm(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("gm")
                .requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(Commands.literal(type.getName())
                            .executes(context -> redirectToServer("gamemode " + type.getName()))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer("gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(Commands.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer("gamemode " + type.getName()))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer("gamemode " + type.getName() + ' ' + captureLastArgument(context)))))
                    .then(Commands.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer("gamemode " + type.getName()))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(context -> redirectToServer("gamemode " + type.getName() + ' ' + captureLastArgument(context)))));
        }
        dispatcher.register(node);
    }

    private static void registerGmNoArgs(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        for (GameType type : GameType.values()) {
            dispatcher.register(Commands.literal("gm" + GamemodeOverhaulCommon.createShort(type))
                    .requires(stack -> stack.hasPermission(2))
                    .executes(context -> redirectToServer("gamemode " + type.getName()))
                    .then(Commands.argument("target", EntityArgument.players())
                            .executes(context -> redirectToServer("gamemode " + type.getName() + ' ' + captureLastArgument(context)))));
        }
    }

    private static void registerDefaultGamemode(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("defaultgamemode").requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(Commands.literal(type.getName())
                            .executes(context -> redirectToServer("defaultgamemode " + type.getName())))
                    .then(Commands.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer("defaultgamemode " + type.getName())))
                    .then(Commands.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer("defaultgamemode " + type.getName())));
        }
        dispatcher.register(node);
    }

    private static void registerDgm(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("dgm")
                .requires(stack -> stack.hasPermission(2));
        for (GameType type : GameType.values()) {
            node.then(Commands.literal(type.getName())
                            .executes(context -> redirectToServer("defaultgamemode " + type.getName())))
                    .then(Commands.literal(String.valueOf(type.ordinal()))
                            .executes(context -> redirectToServer("defaultgamemode " + type.getName())))
                    .then(Commands.literal(GamemodeOverhaulCommon.createShort(type))
                            .executes(context -> redirectToServer("defaultgamemode " + type.getName())));
        }
        dispatcher.register(node);
    }

    private static void registerDifficulty(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("difficulty").requires(stack -> stack.hasPermission(2));
        for (Difficulty value : Difficulty.values()) {
            node.then(Commands.literal(value.getKey())
                            .executes(context -> redirectToServer("difficulty " + value.getKey())))
                    .then(Commands.literal(String.valueOf(value.ordinal()))
                            .executes(context -> redirectToServer("difficulty " + value.getKey())));
        }
        dispatcher.register(node);
    }

    private static void registerToggleDownfall(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("toggledownfall").requires(stack -> stack.hasPermission(2))
                .executes((context) -> toggleDownfall(context.getSource())));
    }

    private static int toggleDownfall(@Nonnull CommandSourceStack source) {
        Level level = source.getUnsidedLevel();
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        if (level.isRaining() || level.getLevelData().isRaining() || level.isThundering() || level.getLevelData().isThundering()) {
            player.commandUnsigned("weather clear");
        } else {
            player.commandUnsigned("weather rain");
        }
        return 1;
    }

    private static int redirectToServer(@NotNull String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        if (player.commandHasSignableArguments(command)) {
            return -1;
        }
        LastSeenMessages.Update update = player.connection.generateMessageAcknowledgements();
        player.connection.send(new ServerboundChatCommandPacket(command, Instant.now(), 0L, ArgumentSignatures.EMPTY, false, update));
        return 1;
    }

    @Contract(pure = true)
    private static @NotNull String captureLastArgument(@NotNull CommandContext<CommandSourceStack> source) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = source.getNodes();
        StringRange node = nodes.get(nodes.size() - 1).getRange();
        return source.getInput().substring(node.getStart(), node.getEnd());
    }
}
