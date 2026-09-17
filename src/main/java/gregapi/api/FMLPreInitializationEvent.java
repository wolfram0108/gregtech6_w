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

package gregapi.api;

import java.io.File;

/** NeoForge has no equivalent lifecycle event, so GregTech6's own entry point constructs this to
 *  keep every mod's code 1:1; only getModConfigurationDirectory() is ever actually read from it. */
public class FMLPreInitializationEvent {
	private final File mModConfigurationDirectory;

	public FMLPreInitializationEvent(File aModConfigurationDirectory) {
		mModConfigurationDirectory = aModConfigurationDirectory;
	}

	/** The mod's config directory, backed by FMLPaths.CONFIGDIR in neo. */
	public File getModConfigurationDirectory() {
		return mModConfigurationDirectory;
	}
}
