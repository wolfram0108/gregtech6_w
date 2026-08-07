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

package gregapi.block.misc;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import gregapi.block.BlockBaseMeta;
import gregapi.render.IIconContainer;
import gregapi.tileentity.ITileEntityMachineBlockUpdateable;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBaseMachineUpdate extends BlockBaseMeta {
	public BlockBaseMachineUpdate(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons, int aBitMask) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType, aMaxMeta, aIcons);
		ITileEntityMachineBlockUpdateable.Util.registerMachineBlock(this, aBitMask);
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.REDSTONE);
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W)));
	}
	
	@Override public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ)                           {if (ITileEntityMachineBlockUpdateable.Util.isMachineBlock(this, WD.meta(aWorld, aX, aY, aZ))) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(aWorld, aX, aY, aZ, this, UT.Code.bind4(WD.meta(aWorld, aX, aY, aZ)), F);}
	public void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMetaData) {if (ITileEntityMachineBlockUpdateable.Util.isMachineBlock(this, aMetaData                          )) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(aWorld, aX, aY, aZ, this, UT.Code.bind4(aMetaData), T);}
	public int getMobilityFlag() {return 2;}
}
