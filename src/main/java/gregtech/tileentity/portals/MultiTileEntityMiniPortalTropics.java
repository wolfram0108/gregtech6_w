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
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityMiniPortalTropics extends MultiTileEntityMiniPortal {
	public static List<MultiTileEntityMiniPortal>
	sListTropicSide = new ArrayListNoNulls<>(),
	sListWorldSide  = new ArrayListNoNulls<>();
	
	@Override public List<MultiTileEntityMiniPortal> getPortalListA() {return sListWorldSide;}
	@Override public List<MultiTileEntityMiniPortal> getPortalListB() {return sListTropicSide;}
	
	static {
		LH.add("gt.tileentity.portal.tropic.tooltip.1", "Only works between the Tropics and the Overworld!");
		LH.add("gt.tileentity.portal.tropic.tooltip.2", "Margin of Error to still work: 128 Meters.");
		LH.add("gt.tileentity.portal.tropic.tooltip.3", "Requires any Tropicraft Cocktail for activation");
	}
	
	@Override
	public void addToolTips2(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.tropic.tooltip.1"));
		aList.add(Chat.CYAN     + LH.get("gt.tileentity.portal.tropic.tooltip.2"));
		aList.add(Chat.ORANGE   + LH.get("gt.tileentity.portal.tropic.tooltip.3"));
	}
	
	@Override
	public void findTargetPortal() {
		mTarget = null;
		if (MD.TROPIC.mLoaded && level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				long tShortestDistance = 128*128;
				for (MultiTileEntityMiniPortal tTarget : sListTropicSide) if (tTarget != this && !tTarget.isDead()) {
					long tXDifference = getBlockPos().getX()-tTarget.getBlockPos().getX(), tZDifference = getBlockPos().getZ()-tTarget.getBlockPos().getZ();
					long tTempDist = tXDifference * tXDifference + tZDifference * tZDifference;
					if (tTempDist < tShortestDistance) {
						tShortestDistance = tTempDist;
						mTarget = tTarget;
					} else if (tTempDist == tShortestDistance && (mTarget == null || Math.abs(tTarget.getBlockPos().getY()-getBlockPos().getY()) < Math.abs(mTarget.getBlockPos().getY()-getBlockPos().getY()))) {
						mTarget = tTarget;
					}
				}
			} else if (WD.dimTROPIC(level)) {
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
		if (MD.TROPIC.mLoaded && level != null && isServerSide()) {
			if (WD.dimensionId(level) == DIM_OVERWORLD) {
				if (!sListWorldSide.contains(this)) sListWorldSide.add(this);
				for (MultiTileEntityMiniPortal tPortal : sListTropicSide) tPortal.findTargetPortal();
				findTargetPortal();
			} else if (WD.dimTROPIC(level)) {
				if (!sListTropicSide.contains(this)) sListTropicSide.add(this);
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
			if (ST.valid(aStack) && aStack.getCount() > 0 && ST.equal(aStack, MD.TROPIC, "cocktail")) {
				setPortalActive();
				if (mTarget != null) UT.Entities.sendchat(aPlayer, "X: " + mTarget.getBlockPos().getX() + "   Y: " + mTarget.getBlockPos().getY() + "   Z: " + mTarget.getBlockPos().getZ());
				if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
			}
		}
		return T;
	}
	
	@Override public float getBlockHardness() {return WD.hardness(Blocks.OAK_PLANKS, level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());}
	@Override public float getExplosionResistance2() {return Blocks.OAK_PLANKS.getExplosionResistance();}
	
	public ITexture sTropicsPortal = BlockTextureCopied.get(ST.block(MD.TROPIC, "tile.portal", Blocks.NETHER_PORTAL), SIDE_ANY, 0, UNCOLOURED, F, T, T), sTropicsPortalFrame = BlockTextureCopied.get(ST.block(MD.TROPIC, "tile.bambooBundle", Blocks.OAK_PLANKS), SIDE_FRONT, 0, UNCOLOURED, F, F, F), sTropicsPortalInactive = BlockTextureCopied.get(Blocks.WATER, SIDE_TOP, 0, 0x0088ffcc, F, T, T);
	@Override public ITexture getPortalTexture() {return sTropicsPortal;}
	@Override public ITexture getFrameTexture() {return sTropicsPortalFrame;}
	@Override public ITexture getInactiveTexture() {return sTropicsPortalInactive;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.portal.tropics";}
}
