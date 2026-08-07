/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregtech.blocks.wood;

import net.minecraft.world.level.block.SoundType;
import gregapi.block.tree.BlockBaseBeamFlammable;
import gregapi.data.LH;
import gregapi.old.Textures;
import gregapi.block.Material;

public class BlockTreeBeamA extends BlockBaseBeamFlammable {
	public BlockTreeBeamA(String aUnlocalised) {
		super(null, aUnlocalised, Material.wood, SoundType.WOOD, 4, Textures.BlockIcons.BEAMS_A);
		
		LH.add(getUnlocalizedName()+ ".0", "Rubber Beam");
		LH.add(getUnlocalizedName()+ ".4", "Rubber Beam");
		LH.add(getUnlocalizedName()+ ".8", "Rubber Beam");
		LH.add(getUnlocalizedName()+".12", "Rubber Beam");
		
		LH.add(getUnlocalizedName()+ ".1", "Maple Beam");
		LH.add(getUnlocalizedName()+ ".5", "Maple Beam");
		LH.add(getUnlocalizedName()+ ".9", "Maple Beam");
		LH.add(getUnlocalizedName()+".13", "Maple Beam");
		
		LH.add(getUnlocalizedName()+ ".2", "Willow Beam");
		LH.add(getUnlocalizedName()+ ".6", "Willow Beam");
		LH.add(getUnlocalizedName()+".10", "Willow Beam");
		LH.add(getUnlocalizedName()+".14", "Willow Beam");
		
		LH.add(getUnlocalizedName()+ ".3", "Blue Mahoe Beam");
		LH.add(getUnlocalizedName()+ ".7", "Blue Mahoe Beam");
		LH.add(getUnlocalizedName()+".11", "Blue Mahoe Beam");
		LH.add(getUnlocalizedName()+".15", "Blue Mahoe Beam");
	}
}
