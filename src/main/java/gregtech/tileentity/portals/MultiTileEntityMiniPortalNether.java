/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregtech.tileentity.portals;

import static gregapi.data.CS.*;

import java.util.List;
import net.minecraft.util.RandomSource;

import gregapi.block.multitileentity.IMultiTileEntity.IMTE_RandomDisplayTick;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.render.BlockTextureCopied;
import gregapi.render.ITexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityMiniPortalNether extends MultiTileEntityMiniPortal implements IMTE_RandomDisplayTick {
	public static List<MultiTileEntityMiniPortal>
	sListNetherSide = new ArrayListNoNulls<>(),
	sListWorldSide  = new ArrayListNoNulls<>();
	
	@Override public List<MultiTileEntityMiniPortal> getPortalListA() {return sListWorldSide;}
	@Override public List<MultiTileEntityMiniPortal> getPortalListB() {return sListNetherSide;}
	
	static {
		LH.add("gt.tileentity.portal.nether.tooltip.1", "Only works between the Nether and the Overworld with a x8 Distance Factor!");
		LH.add("gt.tileentity.portal.nether.tooltip.2", "Margin of Error to still work: 128 Meters.");
	}
	
	@Override
	public void addToolTips2(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.nether.tooltip.1"));
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.nether.tooltip.2"));
		aList.add(Chat.ORANGE   + LH.get(LH.REQUIREMENT_IGNITE_FIRE));
	}
	
	@Override
	public void randomDisplayTick(RandomSource aRandom) {
		if (mActive) for (int i = 0; i < 4; ++i) level.spawnParticle("portal", getBlockPos().getX() + aRandom.nextFloat(), getBlockPos().getY() + aRandom.nextFloat(), getBlockPos().getZ() + aRandom.nextFloat(), (aRandom.nextFloat() - 0.5D) * 0.5D, (aRandom.nextFloat() - 0.5D) * 0.5D, (aRandom.nextFloat() - 0.5D) * 0.5D);
	}
	
	@Override
	public void findTargetPortal() {
		mTarget = null;
		if (level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				long tShortestDistance = 128*128;
				for (MultiTileEntityMiniPortal tTarget : sListNetherSide) if (tTarget != this && !tTarget.isDead()) {
					long tXDifference = getBlockPos().getX()-tTarget.getBlockPos().getX()*8, tZDifference = getBlockPos().getZ()-tTarget.getBlockPos().getZ()*8;
					long tTempDist = tXDifference * tXDifference + tZDifference * tZDifference;
					if (tTempDist < tShortestDistance) {
						tShortestDistance = tTempDist;
						mTarget = tTarget;
					} else if (tTempDist == tShortestDistance && (mTarget == null || Math.abs(tTarget.getBlockPos().getY()-getBlockPos().getY()) < Math.abs(mTarget.getBlockPos().getY()-getBlockPos().getY()))) {
						mTarget = tTarget;
					}
				}
			} else if (WD.dimensionId(level) == DIM_NETHER) {
				long tShortestDistance = 128*128;
				for (MultiTileEntityMiniPortal tTarget : sListWorldSide) if (tTarget != this && !tTarget.isDead()) {
					long tXDifference = tTarget.getBlockPos().getX()-getBlockPos().getX()*8, tZDifference = tTarget.getBlockPos().getZ()-getBlockPos().getZ()*8;
					long tTempDist = tXDifference * tXDifference + tZDifference * tZDifference;
					if (tTempDist < tShortestDistance) {
						tShortestDistance = tTempDist;
						mTarget = tTarget;
					} else if (tTempDist == tShortestDistance && (mTarget == null || Math.abs(tTarget.getBlockPos().getY()-getBlockPos().getY()) < Math.abs(mTarget.getBlockPos().getY()-getBlockPos().getY()))) {
						mTarget = tTarget;
					}
				}
			}
		}
	}
	
	@Override
	public void addThisPortalToLists() {
		if (level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				if (!sListWorldSide.contains(this)) sListWorldSide.add(this);
				for (MultiTileEntityMiniPortal tPortal : sListNetherSide) tPortal.findTargetPortal();
				findTargetPortal();
			} else if (WD.dimensionId(level) == DIM_NETHER) {
				if (!sListNetherSide.contains(this)) sListNetherSide.add(this);
				for (MultiTileEntityMiniPortal tPortal : sListWorldSide) tPortal.findTargetPortal();
				findTargetPortal();
			} else {
				setPortalInactive();
			}
		}
	}
	
	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isClientSide()) return super.onToolClick(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
		if (aTool.equals(TOOL_igniter)) {
			if (mActive) setPortalInactive(); else setPortalActive();
			if (mTarget != null && aChatReturn != null) aChatReturn.add("X: " + mTarget.getBlockPos().getX() + "   Y: " + mTarget.getBlockPos().getY() + "   Z: " + mTarget.getBlockPos().getZ());
			return 10000;
		}
		if (aTool.equals(TOOL_extinguisher)) {
			if (mActive) setPortalInactive();
			return 10000;
		}
		return super.onToolClick(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
	}
	
	@Override public float getBlockHardness() {return Blocks.OBSIDIAN.getBlockHardness(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());}
	@Override public float getExplosionResistance2() {return Blocks.OBSIDIAN.getExplosionResistance(null);}
	
	public ITexture sNetherPortal = BlockTextureCopied.get(Blocks.NETHER_PORTAL, SIDE_ANY, 0, UNCOLOURED, F, T, T), sNetherPortalFrame = BlockTextureCopied.get(Blocks.OBSIDIAN, SIDE_ANY, 0);
	@Override public ITexture getPortalTexture() {return sNetherPortal;}
	@Override public ITexture getFrameTexture() {return sNetherPortalFrame;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.portal.nether";}
}
