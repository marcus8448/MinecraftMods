/*
 * GamemodeOverhaul Copyright (C) 2019-2022 marcus8448
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

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public class ForgeConfig implements GamemodeOverhaulConfig {
    public final Common common;
    public final IConfigSpec<?> commonSpec;

    @SubscribeEvent
    public void onLoad(ModConfigEvent.Loading configEvent) {
        Constant.LOGGER.debug("Successfully loaded GamemodeOverhaul's config file!");
    }

    public ForgeConfig() {
        Pair<Common, ForgeConfigSpec> configure = new ForgeConfigSpec.Builder().configure(Common::new);
        this.common = configure.getLeft();
        this.commonSpec = configure.getRight();
    }

    @Override
    public boolean enableGamemode() {
        return this.common.enableDefaultGamemode.get();
    }

    @Override
    public boolean enableGm() {
        return this.common.enableGm.get();
    }

    @Override
    public boolean enableNoArgsGm() {
        return this.common.enableNoArgsGm.get();
    }

    @Override
    public boolean enableDefaultGamemode() {
        return this.common.enableDefaultGamemode.get();
    }

    @Override
    public boolean enableDgm() {
        return this.common.enableDgm.get();
    }

    @Override
    public boolean enableDifficulty() {
        return this.common.enableDifficulty.get();
    }

    @Override
    public boolean enableToggledownfall() {
        return this.common.enableToggledownfall.get();
    }

    public static class Common {
        final ForgeConfigSpec.BooleanValue enableGamemode;
        final ForgeConfigSpec.BooleanValue enableGm;
        final ForgeConfigSpec.BooleanValue enableNoArgsGm;
        final ForgeConfigSpec.BooleanValue enableDefaultGamemode;
        final ForgeConfigSpec.BooleanValue enableDgm;
        final ForgeConfigSpec.BooleanValue enableDifficulty;
        final ForgeConfigSpec.BooleanValue enableToggledownfall;

        Common(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("GamemodeOverhaul's command config settings").push("commands");
            this.enableGamemode = builder.comment("Set this to false if you don't want the mod to add additional arguments to the '/gamemode' command").translation("gamemodeoverhaul.config.enable_gamemode").worldRestart().define("enableGamemode", true);
            this.enableGm = builder.comment("Set this to false if you don't want the mod to add the '/gm' command").translation("gamemodeoverhaul.config.enable_gm").worldRestart().define("enableGm", true);
            this.enableNoArgsGm = builder.comment("Set this to false if you don't the mod to add '/gmc', '/gms', '/gmsp' and '/gma' commands").translation("gamemodeoverhaul.config.enable_no_args_gm").worldRestart().define("enableNoArgsGm", false);
            this.enableDefaultGamemode = builder.comment("Set this to false if you don't want the mod to add additional arguments to the '/defaultgamemode' command").translation("gamemodeoverhaul.config.enable_default_gamemode").worldRestart().define("enableDefaultGamemode", true);
            this.enableDgm = builder.comment("Set this to false if you don't want the mod to add the '/dgm' command").translation("gamemodeoverhaul.config.enable_dgm").worldRestart().define("enableDgm", false);
            this.enableDifficulty = builder.comment("Set this to false if you don't want the mod to add the integer values for '/difficulty'").translation("gamemodeoverhaul.config.enable_difficulty").worldRestart().define("enableDifficulty", true);
            this.enableToggledownfall = builder.comment("Set this to false if you don't want to have the mod add '/toggledownfall' back").translation("gamemodeoverhaul.config.enable_toggledownfall").worldRestart().define("enableToggledownfall", true);

            builder.pop();
        }
    }
}
