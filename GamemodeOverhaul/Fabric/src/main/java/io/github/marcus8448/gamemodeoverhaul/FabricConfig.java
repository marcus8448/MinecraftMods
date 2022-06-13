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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

@SuppressWarnings("FieldCanBeLocal")
public class FabricConfig implements GamemodeOverhaulConfig {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private final boolean enableGamemode = true;
    private final boolean enableGm = true;
    private final boolean enableNoArgsGm = false;
    private final boolean enableDefaultGamemode = true;
    private final boolean enableDgm = false;
    private final boolean enableDifficulty = true;
    private final boolean enableToggledownfall = true;

    private FabricConfig() {}

    public static FabricConfig create() {
        File file = FabricLoader.getInstance().getConfigDir().resolve("gamemodeoverhaul.json").toFile();
        if (file.exists()) {
            try {
                return GSON.fromJson(new FileReader(file), FabricConfig.class);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to read configuration file!", e);
            }
        } else {
            try {
                FabricConfig src = new FabricConfig();
                GSON.toJson(src, new FileWriter(file));
                return src;
            } catch (IOException e) {
                throw new RuntimeException("Failed to create configuration file!", e);
            }
        }
    }

    @Override
    public boolean enableGamemode() {
        return this.enableGamemode;
    }

    @Override
    public boolean enableGm() {
        return this.enableGm;
    }

    @Override
    public boolean enableNoArgsGm() {
        return this.enableNoArgsGm;
    }

    @Override
    public boolean enableDefaultGamemode() {
        return this.enableDefaultGamemode;
    }

    @Override
    public boolean enableDgm() {
        return this.enableDgm;
    }

    @Override
    public boolean enableDifficulty() {
        return this.enableDifficulty;
    }

    @Override
    public boolean enableToggledownfall() {
        return this.enableToggledownfall;
    }
}
