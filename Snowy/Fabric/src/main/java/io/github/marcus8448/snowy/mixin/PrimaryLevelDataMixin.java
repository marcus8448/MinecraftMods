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

package io.github.marcus8448.snowy.mixin;

import io.github.marcus8448.snowy.SnowyFabric;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataMixin {
    @Inject(method = "getClearWeatherTime", at = @At("HEAD"), cancellable = true)
    private void neverClearWeather(CallbackInfoReturnable<Integer> cir) {
        if (SnowyFabric.CONFIG.enableConstantSnow()) cir.setReturnValue(0);
    }

    @Inject(method = "getRainTime", at = @At("HEAD"), cancellable = true)
    private void alwaysRain(CallbackInfoReturnable<Integer> cir) {
        if (SnowyFabric.CONFIG.enableConstantSnow()) cir.setReturnValue(6000);
    }

    @Inject(method = "getThunderTime", at = @At("HEAD"), cancellable = true)
    private void alwaysThunder(CallbackInfoReturnable<Integer> cir) {
        if (SnowyFabric.CONFIG.enableConstantSnow()) cir.setReturnValue(6000);
    }

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void enableRain(CallbackInfoReturnable<Boolean> cir) {
        if (SnowyFabric.CONFIG.enableConstantSnow()) cir.setReturnValue(true);
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void disableThunder(CallbackInfoReturnable<Boolean> cir) {
        if (SnowyFabric.CONFIG.enableConstantSnow()) cir.setReturnValue(false);
    }
}
