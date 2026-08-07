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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.BlockGetter;

public class BlockBarsTungstenSteel extends BlockBaseBars {
	public BlockBarsTungstenSteel(String aNameInternal) {
		super(aNameInternal, MT.TungstenSteel, Material.iron, SoundType.METAL);
		LH.add(getUnlocalizedName()+ ".0", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".1", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".2", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".3", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".4", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".5", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".6", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".7", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".8", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+ ".9", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+".10", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+".11", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+".12", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+".13", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+".14", "Tungstensteel Bars");
		LH.add(getUnlocalizedName()+".15", "Tungstensteel Bars");
	}
	
	@Override public float getExplosionResistance(byte aMeta) {return 16;}
	
	
	public boolean canEntityDestroy(BlockGetter aWorld, int aX, int aY, int aZ, Entity aEntity) {
		return !(aEntity instanceof WitherBoss || aEntity instanceof EnderDragon);
	}
}
