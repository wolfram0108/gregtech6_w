/**
 * Copyright (c) 2021 GregTech-6 Team
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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.block;

import static gregapi.data.CS.*;

import gregapi.block.MapColor;

public class MaterialGas extends Material {
	public static MaterialGas instance = new MaterialGas();
	
	private MaterialGas() {
		super(MapColor.snowColor);
		setLiquid();   // прежде приходило от MaterialLiquid (жидкость, не плотная, застраивается, поршнем не толкается)
		setNoPushMobility();
		setReplaceable();
	}
	
	@Override public boolean isOpaque() {return F;}
	@Override public boolean isLiquid() {return T;}
	@Override public boolean isSolid() {return F;}
	@Override public boolean blocksMovement() {return F;}
}
