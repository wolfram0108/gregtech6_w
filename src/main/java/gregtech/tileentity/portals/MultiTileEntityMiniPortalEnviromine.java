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
public class MultiTileEntityMiniPortalEnviromine extends MultiTileEntityMiniPortal {
	public static List<MultiTileEntityMiniPortal>
	sListEnviromineSide = new ArrayListNoNulls<>(),
	sListWorldSide      = new ArrayListNoNulls<>();
	
	@Override public List<MultiTileEntityMiniPortal> getPortalListA() {return sListWorldSide;}
	@Override public List<MultiTileEntityMiniPortal> getPortalListB() {return sListEnviromineSide;}
	
	static {
		LH.add("gt.tileentity.portal.enviromine.tooltip.1", "Only works between the Enviromine Caves and the Overworld!");
		LH.add("gt.tileentity.portal.enviromine.tooltip.2", "Margin of Error to still work: 128 Meters.");
		LH.add("gt.tileentity.portal.enviromine.tooltip.3", "Requires any Exquisite Diamond for activation");
	}
	
	@Override
	public void addToolTips2(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.enviromine.tooltip.1"));
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.enviromine.tooltip.2"));
		aList.add(Chat.ORANGE   + LH.get("gt.tileentity.portal.enviromine.tooltip.3"));
	}
	
	@Override
	public void findTargetPortal() {
		mTarget = null;
		if (MD.ENVM.mLoaded && level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				long tShortestDistance = 128*128;
				for (MultiTileEntityMiniPortal tTarget : sListEnviromineSide) if (tTarget != this && !tTarget.isDead()) {
					long tXDifference = getBlockPos().getX()-tTarget.getBlockPos().getX(), tZDifference = getBlockPos().getZ()-tTarget.getBlockPos().getZ();
					long tTempDist = tXDifference * tXDifference + tZDifference * tZDifference;
					if (tTempDist < tShortestDistance) {
						tShortestDistance = tTempDist;
						mTarget = tTarget;
					} else if (tTempDist == tShortestDistance && (mTarget == null || Math.abs(tTarget.getBlockPos().getY()-getBlockPos().getY()) < Math.abs(mTarget.getBlockPos().getY()-getBlockPos().getY()))) {
						mTarget = tTarget;
					}
				}
			} else if (WD.dimENVM(level)) {
				long tShortestDistance = 128*128;
				for (MultiTileEntityMiniPortal tTarget : sListWorldSide) if (tTarget != this && !tTarget.isDead()) {
					long tXDifference = tTarget.getBlockPos().getX()-getBlockPos().getX(), tZDifference = tTarget.getBlockPos().getZ()-getBlockPos().getZ();
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
		if (MD.ENVM.mLoaded && level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				if (!sListWorldSide.contains(this)) sListWorldSide.add(this);
				for (MultiTileEntityMiniPortal tPortal : sListEnviromineSide) tPortal.findTargetPortal();
				findTargetPortal();
			} else if (WD.dimENVM(level)) {
				if (!sListEnviromineSide.contains(this)) sListEnviromineSide.add(this);
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
			ItemStack aStack = aPlayer.getInventory().getSelectedItem();
			if (ST.valid(aStack) && aStack.getCount() > 0 && OM.is_("gemExquisiteAnyDiamond", aStack)) {
				setPortalActive();
				if (mTarget != null) UT.Entities.sendchat(aPlayer, "X: " + mTarget.getBlockPos().getX() + "   Y: " + mTarget.getBlockPos().getY() + "   Z: " + mTarget.getBlockPos().getZ());
				if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
			}
		}
		return T;
	}
	
	@Override public float getBlockHardness() {return WD.hardness(Blocks.STONE, level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());}
	@Override public float getExplosionResistance2() {return Blocks.STONE.getExplosionResistance(null);}
	
	public ITexture sEnvirominePortal = BlockTextureCopied.get(Blocks.NETHER_PORTAL, SIDE_ANY, 0, 0x00ff0000, F, T, T), sEnvirominePortalFrame = BlockTextureCopied.get(Blocks.BEDROCK, SIDE_ANY, 0, UNCOLOURED, F, F, F);
	@Override public ITexture getPortalTexture() {return sEnvirominePortal;}
	@Override public ITexture getFrameTexture() {return sEnvirominePortalFrame;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.portal.enviromine";}
}
