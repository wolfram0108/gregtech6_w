/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * Written for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w). It holds no
 * code of the project that owns this package name, only the public signatures GregTech 6 calls or
 * implements, so the port compiles without fetching that mod. It never ships: at runtime the package
 * belongs to the real mod. All names and rights remain with its authors; see src/compat-mirror/README.md.
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

package appeng.api.orientation;

public enum BlockOrientation {
	DOWN_NORTH, DOWN_WEST, DOWN_SOUTH, DOWN_EAST, UP_NORTH, UP_EAST, UP_SOUTH, UP_WEST, NORTH_UP, NORTH_WEST, NORTH_DOWN, NORTH_EAST, SOUTH_UP, SOUTH_EAST, SOUTH_DOWN, SOUTH_WEST, WEST_UP, WEST_SOUTH, WEST_DOWN, WEST_NORTH, EAST_UP, EAST_NORTH, EAST_DOWN, EAST_SOUTH;
	public int getSpin() { throw new UnsupportedOperationException(); }
	public static appeng.api.orientation.BlockOrientation get(appeng.api.orientation.IOrientationStrategy a0, net.minecraft.world.level.block.state.BlockState a1) { throw new UnsupportedOperationException(); }
}
