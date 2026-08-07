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

package gregtech.blocks.tool;

import net.minecraft.world.level.block.SoundType;
import gregapi.block.misc.BlockBaseBars;
import gregapi.data.LH;
import gregapi.data.MT;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;

public class BlockBarsBrass extends BlockBaseBars {
	public BlockBarsBrass(String aNameInternal) {
		super(aNameInternal, MT.Brass, Material.iron, SoundType.METAL);
		LH.add(getUnlocalizedName()+ ".0", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".1", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".2", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".3", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".4", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".5", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".6", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".7", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".8", "Brass Bars");
		LH.add(getUnlocalizedName()+ ".9", "Brass Bars");
		LH.add(getUnlocalizedName()+".10", "Brass Bars");
		LH.add(getUnlocalizedName()+".11", "Brass Bars");
		LH.add(getUnlocalizedName()+".12", "Brass Bars");
		LH.add(getUnlocalizedName()+".13", "Brass Bars");
		LH.add(getUnlocalizedName()+".14", "Brass Bars");
		LH.add(getUnlocalizedName()+".15", "Brass Bars");
	}
	
	@Override public float getExplosionResistance(byte aMeta) {return 5;}
}
