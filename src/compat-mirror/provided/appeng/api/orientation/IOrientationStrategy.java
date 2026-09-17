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

public interface IOrientationStrategy {
	java.util.Collection<net.minecraft.world.level.block.state.properties.Property<?>> getProperties();
	static appeng.api.orientation.IOrientationStrategy get(net.minecraft.world.level.block.state.BlockState a0) { throw new UnsupportedOperationException(); }
	default net.minecraft.world.level.block.state.BlockState setOrientation(net.minecraft.world.level.block.state.BlockState a0, net.minecraft.core.Direction a1, int a2) { throw new UnsupportedOperationException(); }
	default boolean allowsPlayerRotation() { throw new UnsupportedOperationException(); }
}
