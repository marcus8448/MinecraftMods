/*
 * Snowy
 * Copyright (C) 2020-2023 marcus8448
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

package io.github.marcus8448.snowy;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Constant.MOD_ID)
public class SnowyForge {
    public static final ForgeSnowyConfig CONFIG = new ForgeSnowyConfig();
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Constant.MOD_ID);
    private static final DeferredRegister<BiomeModifier> BIOME_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIERS, Constant.MOD_ID);
    public static final RegistryObject<Codec<? extends BiomeModifier>> BIOME_MODIFIER_CODEC = BIOME_MODIFIER_SERIALIZERS.register("transform", () -> SnowyBiomeModifier.CODEC);

    public SnowyForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG.commonSpec);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CONFIG::onLoad);

        MinecraftForge.EVENT_BUS.addListener(this::serverTick);

        BIOME_MODIFIER_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BIOME_MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public void serverTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER) {
            if (CONFIG.enableConstantSnow()) {
                if (CONFIG.enableNonOverworldBiomes() || event.level.dimension().equals(Level.OVERWORLD)) {
                    ((ServerLevel) event.level).setWeatherParameters(0, 6000, true, false);
                }
            }
        }
    }

    private static boolean canModify(Holder<Biome> biome) {
        for (String forceEnabledBiome : CONFIG.forceEnabledBiomes()) {
            if (biome.is(new ResourceLocation(forceEnabledBiome))) return true;
        }
        for (String forceDisabledBiome : CONFIG.forceDisabledBiomes()) {
            if (biome.is(new ResourceLocation(forceDisabledBiome))) return false;
        }

        return CONFIG.enableNonOverworldBiomes() || biome.is(BiomeTags.IS_OVERWORLD);
    }

    private record SnowyBiomeModifier(HolderGetter<PlacedFeature> getter) implements BiomeModifier {
        private static final Codec<SnowyBiomeModifier> CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<SnowyBiomeModifier, T>> decode(DynamicOps<T> ops, T input) {
                if (ops instanceof RegistryOps<T> registryOps) {
                    return DataResult.success(new Pair<>(new SnowyBiomeModifier(registryOps.getter(Registries.PLACED_FEATURE).get()), input));
                }
                throw new UnsupportedOperationException("Non-registry ops");
            }

            @Override
            public <T> DataResult<T> encode(SnowyBiomeModifier input, DynamicOps<T> ops, T prefix) {
                return DataResult.success(prefix);
            }
        };

        @Override
            public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
                if (phase == Phase.ADD) {
                    if (canModify(biome)) {
                        for (Holder<PlacedFeature> feature : builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.TOP_LAYER_MODIFICATION)) {
                            if (feature.is(MiscOverworldPlacements.FREEZE_TOP_LAYER)) {
                                return;
                            }
                        }
                        builder.getGenerationSettings().addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, this.getter.getOrThrow(MiscOverworldPlacements.FREEZE_TOP_LAYER));
                    }
                } else if (phase == Phase.MODIFY) {
                    if (canModify(biome)) {
                        builder.getClimateSettings().setHasPrecipitation(true);
                        builder.getClimateSettings().setTemperature(0.0f);
                        builder.getClimateSettings().setDownfall(0.5f);
                        builder.getClimateSettings().setTemperatureModifier(CONFIG.enableTemperatureNoise() ? Biome.TemperatureModifier.FROZEN : Biome.TemperatureModifier.NONE);
                    }
                }
            }

            @Override
            public Codec<? extends BiomeModifier> codec() {
                return CODEC;
            }
        }
}