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

package gregapi.block;

/**
 * A map colour LABEL under the names GT6's code uses (the 1.7.10 palette).
 *
 * <p><b>No colour values are stored here.</b> The only holder of the value is the engine:
 * it has its own map palette ({@code net.minecraft.world.level.material.MapColor}), addressed by the same
 * index 0..63 in the same order. This class only holds the mapping "GT6 name → palette index",
 * because the mod addresses colours by names the engine doesn't have.</p>
 *
 * <p>The previous revision carried its own RGB table — and it was dead weight: nothing read the value
 * field (a tree-wide grep = 0), the actual colour was already fetched from the engine through {@link #toNeo()}.
 * Removing the table doesn't change behaviour by a single bit and removes the question of where those numbers came from.</p>
 *
 * <p>The bridge into the engine is {@link #toNeo()}, one place for the whole mod (F9-bridge).</p>
 */
public final class MapColor {
	/** Index in the 64-colour map palette. Matches between 1.7.10 and the target engine. */
	public final int colorIndex;

	private MapColor(int aIndex) {
		if (aIndex < 0 || aIndex > 63) throw new IndexOutOfBoundsException("Map colour index must be within 0..63, given: " + aIndex);
		colorIndex = aIndex;
	}

	private static MapColor idx(int aIndex) {return new MapColor(aIndex);}

	/** Palette colour by index — for code that addresses a colour by number, not by name. */
	public static MapColor byId(int aIndex) {return idx(aIndex);}

	/** F9-bridge: GT6 label → engine colour. The single crossover point for the whole mod. */
	public net.minecraft.world.level.material.MapColor toNeo() {
		return net.minecraft.world.level.material.MapColor.byId(colorIndex);
	}

	// Names as used by GT6's code; the number on the right is the palette index.
	public static final MapColor airColor         = idx( 0);
	public static final MapColor grassColor       = idx( 1);
	public static final MapColor sandColor        = idx( 2);
	public static final MapColor clothColor       = idx( 3);
	public static final MapColor tntColor         = idx( 4);
	public static final MapColor iceColor         = idx( 5);
	public static final MapColor ironColor        = idx( 6);
	public static final MapColor foliageColor     = idx( 7);
	public static final MapColor snowColor        = idx( 8);
	public static final MapColor clayColor        = idx( 9);
	public static final MapColor dirtColor        = idx(10);
	public static final MapColor stoneColor       = idx(11);
	public static final MapColor waterColor       = idx(12);
	public static final MapColor woodColor        = idx(13);
	public static final MapColor quartzColor      = idx(14);
	public static final MapColor adobeColor       = idx(15);
	public static final MapColor magentaColor     = idx(16);
	public static final MapColor lightBlueColor   = idx(17);
	public static final MapColor yellowColor      = idx(18);
	public static final MapColor limeColor        = idx(19);
	public static final MapColor pinkColor        = idx(20);
	public static final MapColor grayColor        = idx(21);
	public static final MapColor silverColor      = idx(22);
	public static final MapColor cyanColor        = idx(23);
	public static final MapColor purpleColor      = idx(24);
	public static final MapColor blueColor        = idx(25);
	public static final MapColor brownColor       = idx(26);
	public static final MapColor greenColor       = idx(27);
	public static final MapColor redColor         = idx(28);
	public static final MapColor blackColor       = idx(29);
	public static final MapColor goldColor        = idx(30);
	public static final MapColor diamondColor     = idx(31);
	public static final MapColor lapisColor       = idx(32);
	public static final MapColor emeraldColor     = idx(33);
	public static final MapColor obsidianColor    = idx(34);
	public static final MapColor netherrackColor  = idx(35);
}
