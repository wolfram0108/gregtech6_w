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

package gregapi.recipes;

/** Stand-in for Forge's RecipeSorter, which neo has no equivalent of; GT6's own recipe order already comes
 *  from iterating its buffer, so registration calls are no-ops kept only to compile call sites unchanged. */
public final class RecipeSorter {
	private RecipeSorter() {}

	/** Forge sorter category; has no effect in neo, kept so GT6 call sites stay unchanged. */
	public enum Category {UNKNOWN, SHAPELESS, SHAPED}

	/** No-op: neo has no recipe sorter; order comes from iterating GT6's own buffer. */
	public static void register(String aName, Class<?> aRecipeClass, Category aCategory, String aOrdering) {/* no-op (F11) */}
}
