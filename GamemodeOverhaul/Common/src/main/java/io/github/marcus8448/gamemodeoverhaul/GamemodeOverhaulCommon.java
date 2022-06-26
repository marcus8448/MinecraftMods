/*
 * GamemodeOverhaul
 * Copyright (C) 2019-2022 marcus8448
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package io.github.marcus8448.gamemodeoverhaul;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.marcus8448.gamemodeoverhaul.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

public class GamemodeOverhaulCommon {
    public static final GamemodeOverhaulConfig CONFIG = Services.PLATFORM.createConfig();

    public static void registerCommands(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        if (CONFIG.enableGamemode()) registerGamemode(dispatcher);
        if (CONFIG.enableGm()) registerGm(dispatcher);
        if (CONFIG.enableNoArgsGm()) registerGmNoArgs(dispatcher);
        if (CONFIG.enableDefaultGamemode()) registerDefaultGamemode(dispatcher);
        if (CONFIG.enableDgm()) registerDgm(dispatcher);
        if (CONFIG.enableDifficulty()) registerDifficulty(dispatcher);
        if (CONFIG.enableToggledownfall()) registerToggleDownfall(dispatcher);

        Constant.LOGGER.info("Commands registered!");
    }

    private static void registerGamemode(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gamemode").requires((commandSource) -> commandSource.hasPermission(2))
                .then((Commands.literal("0")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.SURVIVAL)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.SURVIVAL))))
                .then((Commands.literal("1")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.CREATIVE)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.CREATIVE))))
                .then((Commands.literal("2")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.ADVENTURE)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.ADVENTURE))))
                .then((Commands.literal("3")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.SPECTATOR)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.SPECTATOR))))

                .then((Commands.literal("s")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.SURVIVAL)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.SURVIVAL))))
                .then((Commands.literal("c")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.CREATIVE)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.CREATIVE))))
                .then((Commands.literal("a")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.ADVENTURE)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.ADVENTURE))))
                .then((Commands.literal("sp")
                        .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.SPECTATOR)))
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.SPECTATOR)))));
    }

    private static void registerGm(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> gm = Commands.literal("gm").requires((commandSource) -> commandSource.hasPermission(2)).build();
        for (CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChild("gamemode").getChildren()) {
            gm.addChild(node);
        }
        dispatcher.getRoot().addChild(gm);
    }

    private static void registerGmNoArgs(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gms")
                .requires((commandSource) -> commandSource.hasPermission(2))
                .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.SURVIVAL))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.SURVIVAL))));
        dispatcher.register(Commands.literal("gmc")
                .requires((commandSource) -> commandSource.hasPermission(2))
                .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.CREATIVE))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.CREATIVE))));
        dispatcher.register(Commands.literal("gma")
                .requires((commandSource) -> commandSource.hasPermission(2))
                .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.ADVENTURE))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.ADVENTURE))));
        dispatcher.register(Commands.literal("gmsp")
                .requires((commandSource) -> commandSource.hasPermission(2))
                .executes((context) -> setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), GameType.SPECTATOR))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes((cmdContext) -> setMode(cmdContext, EntityArgument.getPlayers(cmdContext, "target"), GameType.SPECTATOR))));
    }

    private static void registerDefaultGamemode(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("defaultgamemode").requires((commandSource) -> commandSource.hasPermission(2))
                .then((Commands.literal("0")
                        .executes((context) -> setMode(context.getSource(), GameType.SURVIVAL))))
                .then((Commands.literal("1")
                        .executes((context) -> setMode(context.getSource(), GameType.CREATIVE))))
                .then((Commands.literal("2")
                        .executes((context) -> setMode(context.getSource(), GameType.ADVENTURE))))
                .then((Commands.literal("3")
                        .executes((context) -> setMode(context.getSource(), GameType.SPECTATOR))))

                .then((Commands.literal("s")
                        .executes((context) -> setMode(context.getSource(), GameType.SURVIVAL))))
                .then((Commands.literal("c")
                        .executes((context) -> setMode(context.getSource(), GameType.CREATIVE))))
                .then((Commands.literal("a")
                        .executes((context) -> setMode(context.getSource(), GameType.ADVENTURE))))
                .then((Commands.literal("sp")
                        .executes((context) -> setMode(context.getSource(), GameType.SPECTATOR)))));
    }

    private static void registerDgm(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> gm = Commands.literal("dgm").requires((commandSource) -> commandSource.hasPermission(2)).build();
        for (CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChild("defaultgamemode").getChildren()) {
            gm.addChild(node);
        }
        dispatcher.getRoot().addChild(gm);
    }

    private static void registerDifficulty(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("difficulty").requires((commandSource) -> commandSource.hasPermission(2))
                .then((Commands.literal("0")
                        .executes((context) -> DifficultyCommand.setDifficulty(context.getSource(), Difficulty.PEACEFUL))))
                .then((Commands.literal("1")
                        .executes((context) -> DifficultyCommand.setDifficulty(context.getSource(), Difficulty.EASY))))
                .then((Commands.literal("2")
                        .executes((context) -> DifficultyCommand.setDifficulty(context.getSource(), Difficulty.NORMAL))))
                .then((Commands.literal("3")
                        .executes((context) -> DifficultyCommand.setDifficulty(context.getSource(), Difficulty.HARD)))));
    }

    private static void registerToggleDownfall(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("toggledownfall").requires((commandSource) -> commandSource.hasPermission(2))
                .executes((context) -> toggleDownfall(context.getSource())));
    }


    private static void logGamemodeChange(@Nonnull CommandSourceStack context, ServerPlayer player, @Nonnull GameType type) {
        Component text = Component.translatable("gameMode." + type.getName());
        if (context.getEntity() == player) {
            context.sendSuccess(Component.translatable("commands.gamemode.success.self", text), true);
        } else {
            if (context.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
                player.sendSystemMessage(Component.translatable("gameMode.changed", text));
            }

            context.sendSuccess(Component.translatable("commands.gamemode.success.other", player.getDisplayName(), text), true);
        }
    }

    private static int setMode(@Nonnull CommandContext<CommandSourceStack> context, @Nonnull Collection<ServerPlayer> players, GameType type) {
        int i = 0;

        for (ServerPlayer player : players) {
            if (player.setGameMode(type)) {
                logGamemodeChange(context.getSource(), player, type);
                ++i;
            }
        }

        return i;
    }

    private static int setMode(@Nonnull CommandSourceStack source, @Nonnull GameType type) {
        int i = 0;
        MinecraftServer server = source.getServer();
        server.setDefaultGameType(type);
        GameType forcedType = server.getForcedGameType();
        if (forcedType != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.setGameMode(forcedType)) {
                    ++i;
                }
            }
        }

        source.sendSuccess(Component.translatable("commands.defaultgamemode.success", type.getLongDisplayName()), true);
        return i;
    }

    private static int toggleDownfall(@Nonnull CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (!(level.isRaining() || level.getLevelData().isRaining() || level.isThundering() || level.getLevelData().isThundering())) {
            level.setWeatherParameters(0, 6000, true, false);
        } else {
            level.setWeatherParameters(6000, 0, false, false);
        }
        source.sendSuccess(Component.translatable("commands.toggledownfall"), false);
        return 1;
    }
}