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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.marcus8448.gamemodeoverhaul.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class GamemodeOverhaulCommon {
    public static final String MOD_ID = "gamemodeoverhaul";
    public static final String MOD_NAME = "Gamemode Overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static final GamemodeOverhaulConfig CONFIG = Services.PLATFORM.createConfig();

    public static void registerCommands(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        if (CONFIG.enableGamemode()) registerGamemode(dispatcher);
        if (CONFIG.enableGm()) registerGm(dispatcher);
        if (CONFIG.enableNoArgsGm()) registerGmNoArgs(dispatcher);
        if (CONFIG.enableDefaultGamemode()) registerDefaultGamemode(dispatcher);
        if (CONFIG.enableDgm()) registerDgm(dispatcher);
        if (CONFIG.enableDifficulty()) registerDifficulty(dispatcher);
        if (CONFIG.enableToggledownfall()) registerToggleDownfall(dispatcher);

        LOGGER.info("Commands registered!");
    }

    private static void registerGamemode(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("gamemode").requires(stack -> stack.hasPermission(2));
        CommandNode<CommandSourceStack> gamemode = dispatcher.getRoot().getChild("gamemode");
        for (GameType type : GameType.values()) {
            CommandNode<CommandSourceStack> base = gamemode.getChild(type.getName());
            node.then(Commands.literal(String.valueOf(type.ordinal()))
                            .executes(base.getCommand())
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(base.getChild("target").getCommand())))
                    .then(Commands.literal(createShort(type))
                            .executes(base.getCommand())
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(base.getChild("target").getCommand())));
        }
        dispatcher.register(node);
    }

    private static void registerGm(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> gm = Commands.literal("gm").requires(stack -> stack.hasPermission(2)).build();
        for (CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChild("gamemode").getChildren()) {
            gm.addChild(node);
        }
        dispatcher.getRoot().addChild(gm);
    }

    private static void registerGmNoArgs(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> gamemode = dispatcher.getRoot().getChild("gamemode");
        for (GameType type : GameType.values()) {
            CommandNode<CommandSourceStack> base = gamemode.getChild(type.getName());
            dispatcher.register(Commands.literal("gm" + createShort(type))
                    .requires(stack -> stack.hasPermission(2))
                    .executes(base.getCommand())
                    .then(Commands.argument("target", EntityArgument.players())
                            .executes(base.getChild("target").getCommand())));
        }
    }

    private static void registerDefaultGamemode(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("defaultgamemode").requires(stack -> stack.hasPermission(2));
        CommandNode<CommandSourceStack> defaultgamemode = dispatcher.getRoot().getChild("defaultgamemode");
        for (GameType type : GameType.values()) {
            Command<CommandSourceStack> base = defaultgamemode.getChild(type.getName()).getCommand();
            node.then(Commands.literal(String.valueOf(type.ordinal())).executes(base))
                    .then(Commands.literal(createShort(type)).executes(base));
        }
        dispatcher.register(node);
    }

    private static void registerDgm(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> gm = Commands.literal("dgm").requires(stack -> stack.hasPermission(2)).build();
        for (CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChild("defaultgamemode").getChildren()) {
            gm.addChild(node);
        }
        dispatcher.getRoot().addChild(gm);
    }

    private static void registerDifficulty(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("difficulty").requires(stack -> stack.hasPermission(2));
        CommandNode<CommandSourceStack> difficulty = dispatcher.getRoot().getChild("difficulty");
        for (Difficulty value : Difficulty.values()) {
            node.then(Commands.literal(String.valueOf(value.ordinal()))
                    .executes(difficulty.getChild(value.getKey()).getCommand()));
        }
        dispatcher.register(node);
    }

    private static void registerToggleDownfall(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("toggledownfall").requires(stack -> stack.hasPermission(2))
                .executes((context) -> toggleDownfall(context.getSource())));
    }

    private static int toggleDownfall(@Nonnull CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (level.isRaining() || level.getLevelData().isRaining() || level.isThundering() || level.getLevelData().isThundering()) {
            level.setWeatherParameters(6000, 0, false, false);
        } else {
            level.setWeatherParameters(0, 6000, true, false);
        }
        source.sendSuccess(Component.translatable("commands.toggledownfall"), false);
        return 1;
    }

    @Contract(pure = true)
    public static @NotNull String createShort(@NotNull GameType type) {
        return switch (type) {
            case SURVIVAL -> "s";
            case CREATIVE -> "c";
            case ADVENTURE -> "a";
            case SPECTATOR -> "sp";
        };
    }
}