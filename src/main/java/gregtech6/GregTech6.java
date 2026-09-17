/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6;

import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

/** This class stays only because its mod id is the declared entry point mods.toml and SanityTest depend on;
 *  its own item/block registration is stripped so it no longer competes with the central GT_API/FluidGT registries. */
// Temporary @Mod carrier for modId gregtech6, removed once GT6_Main becomes the real @Mod(GT).
// neoforge.mods.toml needs a live entrypoint already at build time, before content registration moves there.
@Mod(GregTech6.MODID)
public class GregTech6 {
    public static final String MODID = "gregtech6";

    public static final Logger LOGGER = LogUtils.getLogger();

    // javafml on 1.20.1 constructs the @Mod class with a no-arg constructor.
    // The bus/context constructor arguments only exist starting on 26.x.
    public GregTech6() {
        LOGGER.info("[GregTech6] entrypoint loaded — content registration centralised in GT_API (F12) / FluidGT (F5)");
    }
}
