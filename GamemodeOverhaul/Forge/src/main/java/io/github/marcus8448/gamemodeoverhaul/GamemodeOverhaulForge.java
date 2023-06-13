/*
 * GamemodeOverhaul
 * Copyright (C) 2019-2023 marcus8448
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

import io.github.marcus8448.gamemodeoverhaul.client.GamemodeOverhaulClientForge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nonnull;

@Mod(GamemodeOverhaulCommon.MOD_ID)
public class GamemodeOverhaulForge {
    public static final ForgeConfig CONFIG = new ForgeConfig();

    public GamemodeOverhaulForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG.commonSpec);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CONFIG::onLoad);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        //ignore version check as we only need to be on the logical server.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> IExtensionPoint.DisplayTest.IGNORESERVERONLY,
                        (remoteVersion, isFromServer) -> true));

        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.addListener(GamemodeOverhaulClientForge::registerClientCommands);
        }
    }
    
    private void registerCommands(@Nonnull RegisterCommandsEvent event) {
        GamemodeOverhaulCommon.registerCommands(event.getDispatcher());
    }
}