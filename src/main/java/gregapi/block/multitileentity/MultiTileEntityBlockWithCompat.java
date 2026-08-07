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

package gregapi.block.multitileentity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import gregapi.util.WD;

import gregapi.api.Optional;
import gregapi.code.ItemNBT;
import gregapi.compat.galacticraft.IBlockSealable;
import gregapi.data.IL;
import gregapi.util.UT;
import micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock;
import gregapi.block.Material;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import openblocks.api.IPaintableBlock;
import vazkii.botania.api.mana.IManaTrigger;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
  @Optional.Interface(iface = "openblocks.api.IPaintableBlock", modid = ModIDs.OB)
, @Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock", modid = ModIDs.GC)
, @Optional.Interface(iface = "vazkii.botania.api.mana.IManaTrigger", modid = ModIDs.BOTA)
})
@SuppressWarnings("deprecation")
public class MultiTileEntityBlockWithCompat extends MultiTileEntityBlock implements IBlockSealable, IOxygenReliantBlock, IPaintableBlock, IManaTrigger {
	protected static MultiTileEntityBlock create(String aModID, String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		return new MultiTileEntityBlockWithCompat(aModID, aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube);
	}
	protected MultiTileEntityBlockWithCompat(String aModID, String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		super(aModID, aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube);
	}
	
	public final boolean recolourBlockRGB(Level aWorld, int aX, int aY, int aZ, Direction aDirection, int aRGB) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMultiTileEntity.IMTE_OnPainting && ((IMultiTileEntity.IMTE_OnPainting)aTileEntity).onPainting(UT.Code.side(aDirection), aRGB);}
	public final boolean isSealed(Level aWorld, int aX, int aY, int aZ, Direction aDirection) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMultiTileEntity.IMTE_IsSealable && ((IMultiTileEntity.IMTE_IsSealable)aTileEntity).isSealable((byte)(UT.Code.side(aDirection) ^ 1));}
	public final void onOxygenAdded  (Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMultiTileEntity.IMTE_OnOxygenAdded) ((IMultiTileEntity.IMTE_OnOxygenAdded)aTileEntity).onOxygenAdded  ();}
	public final void onOxygenRemoved(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMultiTileEntity.IMTE_OnOxygenRemoved) ((IMultiTileEntity.IMTE_OnOxygenRemoved)aTileEntity).onOxygenRemoved();}
	@Optional.Method(modid = ModIDs.BOTA) public final void onBurstCollision(vazkii.botania.api.internal.IManaBurst aMana, Level aWorld, int aX, int aY, int aZ) {if (aWorld.isClientSide()) return; if (aMana.isFake() || !IL.BOTA_Paintslinger.equal(aMana.getSourceLens(), F, T) || !ItemNBT.has(aMana.getSourceLens()) || !ItemNBT.get(aMana.getSourceLens()).contains("color") || ItemNBT.get(aMana.getSourceLens()).getIntOr("color", 0) == -1) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMultiTileEntity.IMTE_OnPainting) ((IMultiTileEntity.IMTE_OnPainting)aTileEntity).onPainting(SIDE_UNKNOWN, (aMana.getColor() & 0x00ffffff));}
}
