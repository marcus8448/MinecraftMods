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

package io.github.marcus8448.gamemodeoverhaul.platform;

import io.github.marcus8448.gamemodeoverhaul.Constant;
import io.github.marcus8448.gamemodeoverhaul.platform.services.Platform;

import java.util.ServiceLoader;

public class Services {
    public static final Platform PLATFORM = load(Platform.class);

    private static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constant.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
