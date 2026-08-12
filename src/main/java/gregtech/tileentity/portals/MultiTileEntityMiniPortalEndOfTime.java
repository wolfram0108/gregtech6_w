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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.tileentity.portals;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.code.ArrayListNoNulls;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.data.MD;
import gregapi.render.BlockTextureCopied;
import gregapi.render.ITexture;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityMiniPortalEndOfTime extends MultiTileEntityMiniPortal {
	public static List<MultiTileEntityMiniPortal>
	sListEndOfTimeSide = new ArrayListNoNulls<>(),
	sListWorldSide  = new ArrayListNoNulls<>();
	
	@Override public List<MultiTileEntityMiniPortal> getPortalListA() {return sListWorldSide;}
	@Override public List<MultiTileEntityMiniPortal> getPortalListB() {return sListEndOfTimeSide;}
	
	static {
		LH.add("gt.tileentity.portal.endoftime.tooltip.1", "Only works between the Last Millenium and the Overworld with a x128 Distance Factor!");
		LH.add("gt.tileentity.portal.endoftime.tooltip.2", "Margin of Error to still work: 512 Meters.");
		LH.add("gt.tileentity.portal.endoftime.tooltip.3", "Requires a Bedrockium Ingot for activation");
	}
	
	@Override
	public void addToolTips2(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.endoftime.tooltip.1"));
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.endoftime.tooltip.2"));
		aList.add(Chat.ORANGE   + LH.get("gt.tileentity.portal.endoftime.tooltip.3"));
	}
	
	@Override
	public void findTargetPortal() {
		mTarget = null;
		if ((MD.ExU.mLoaded || MD.ExS.mLoaded) && level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				long tShortestDistance = 512*512;
				for (MultiTileEntityMiniPortal tTarget : sListEndOfTimeSide) if (tTarget != this && !tTarget.isDead()) {
					long tXDifference = getBlockPos().getX()-tTarget.getBlockPos().getX()*128, tZDifference = getBlockPos().getZ()-tTarget.getBlockPos().getZ()*128;
					long tTempDist = tXDifference * tXDifference + tZDifference * tZDifference;
					if (tTempDist < tShortestDistance) {
						tShortestDistance = tTempDist;
						mTarget = tTarget;
					} else if (tTempDist == tShortestDistance && (mTarget == null || Math.abs(tTarget.getBlockPos().getY()-getBlockPos().getY()) < Math.abs(mTarget.getBlockPos().getY()-getBlockPos().getY()))) {
						mTarget = tTarget;
					}
				}
			} else if (WD.dimLM(level)) {
				long tShortestDistance = 512*512;
				for (MultiTileEntityMiniPortal tTarget : sListWorldSide) if (tTarget != this && !tTarget.isDead()) {
					long tXDifference = tTarget.getBlockPos().getX()-getBlockPos().getX()*128, tZDifference = tTarget.getBlockPos().getZ()-getBlockPos().getZ()*128;
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
		if ((MD.ExU.mLoaded || MD.ExS.mLoaded) && level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				if (!sListWorldSide.contains(this)) sListWorldSide.add(this);
				for (MultiTileEntityMiniPortal tPortal : sListEndOfTimeSide) tPortal.findTargetPortal();
				findTargetPortal();
			} else if (WD.dimLM(level)) {
				if (!sListEndOfTimeSide.contains(this)) sListEndOfTimeSide.add(this);
				for (MultiTileEntityMiniPortal tPortal : sListWorldSide) tPortal.findTargetPortal();
				findTargetPortal();
			} else {
				setPortalInactive();
			}
		}
	}
	
	@Override
	public boolean onBlockActivated2(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isServerSide()) {
			ItemStack aStack = aPlayer.getInventory().getSelected();
			if (ST.valid(aStack) && aStack.getCount() > 0 && OM.is_("ingotBedrockium", aStack)) {
				setPortalActive();
				if (mTarget != null) UT.Entities.sendchat(aPlayer, "X: " + mTarget.getBlockPos().getX() + "   Y: " + mTarget.getBlockPos().getY() + "   Z: " + mTarget.getBlockPos().getZ());
				if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
				
			}
		}
		return T;
	}
	
	@Override public float getBlockHardness() {return WD.hardness(Blocks.STONE, level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());}
	@Override public float getExplosionResistance2() {return Blocks.STONE.getExplosionResistance();}
	
	public ITexture sEndOfTimePortal = BlockTextureCopied.get(ST.block(MD.ExU, "dark_portal", ST.block(MD.ExS, "deepPortal", Blocks.NETHER_PORTAL)), SIDE_ANY, 0, UNCOLOURED, F, T, T), sEndOfTimePortalFrame = BlockTextureCopied.get(ST.block(MD.ExU, "dark_portal", ST.block(MD.ExS, "deepPortal", Blocks.STONE_BRICKS)), SIDE_ANY, 2, UNCOLOURED, F, F, F);
	@Override public ITexture getPortalTexture() {return sEndOfTimePortal;}
	@Override public ITexture getFrameTexture() {return sEndOfTimePortalFrame;}
	
	
	@Override public String getTileEntityName() {return "gt.multitileentity.portal.endoftime";}
}
