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
 */

package gregapi.block;
import gregapi.util.WD;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.item.IItemGT;
import gregapi.render.ITexture;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

public class ItemBlockBase extends BlockItem implements IBlock, IItemGT {
	public final IBlockBase mPlaceable;
	
	public ItemBlockBase(Block aBlock) {
		super(aBlock);
		mPlaceable = (IBlockBase)aBlock;
		setMaxDamage(0);
		setHasSubtypes(T);
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		super.addInformation(aStack, aPlayer, aList, aF3_H);
		byte aMeta = UT.Code.bind4(ST.meta_(aStack));
		mPlaceable.addInformation(aStack, aMeta, aPlayer, aList, aF3_H);
		if (getBlock().getCollisionBoundingBoxFromPool(aPlayer.level(), 0, 0, 0) != null) {
			if (mPlaceable.doesWalkSpeed(aMeta)) aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_WALKSPEED));
			if (mPlaceable.canCreatureSpawn(aMeta)) {
				if (ITexture.Util.OPTIFINE_LOADED && aMeta != 0 && !mPlaceable.canCreatureSpawn((byte)0)) {
					aList.add(LH.Chat.BLINKING_RED + LH.get(Minecraft.getMinecraft().isSingleplayer() ? LH.TOOLTIP_SPAWNPROOF_SP_BUG    : LH.TOOLTIP_SPAWNPROOF_MP_BUG   ));
					aList.add(LH.Chat.BLINKING_RED + LH.get(LH.TOOLTIP_SPAWNPROOF_OPTIFINE));
				}
			} else {
				if (ITexture.Util.OPTIFINE_LOADED && aMeta != 0 &&  mPlaceable.canCreatureSpawn((byte)0)) {
					aList.add(LH.Chat.BLINKING_RED + LH.get(Minecraft.getMinecraft().isSingleplayer() ? LH.TOOLTIP_SPAWNPROOF_SP_BROKEN : LH.TOOLTIP_SPAWNPROOF_MP_BROKEN));
					aList.add(LH.Chat.BLINKING_RED + LH.get(LH.TOOLTIP_SPAWNPROOF_OPTIFINE));
				} else {
					aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_SPAWNPROOF));
				}
			}
			if (MD.GC.mLoaded) {
				byte tCount = 0;
				for (byte tSide : ALL_SIDES_VALID) if (mPlaceable.isSealable(aMeta, tSide)) tCount++;
				if (tCount >= 6) {
					aList.add(LH.Chat.GREEN  + LH.get(LH.TOOLTIP_SEALABLE_ANY));
				} else if (WD.opaque(getBlock())) {
					aList.add(LH.Chat.ORANGE + LH.get(LH.TOOLTIP_SEALABLE_BUGGED));
				} else if (tCount > 0) {
					aList.add(LH.Chat.YELLOW + LH.get(LH.TOOLTIP_SEALABLE_SOME));
				}
			}
		}
		if (mPlaceable.useGravity(aMeta))
			aList.add(LH.Chat.ORANGE + LH.get(LH.TOOLTIP_GRAVITY));
		if (mPlaceable.doesPistonPush(aMeta))
			aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_PISTONPUSHABLE));
		if (mPlaceable.isFlammable(aMeta) || mPlaceable.isFireSource(aMeta) || mPlaceable.getFlammability(aMeta) > 0)
			aList.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FLAMMABLE));
		float tResistance = mPlaceable.getExplosionResistance(aMeta);
		if (tResistance >= 4) aList.add(LH.getToolTipBlastResistance(getBlock(), tResistance));
		
		aList.add(LH.getToolTipHarvest(WD.getMaterial(getBlock()), getBlock().getHarvestTool(aMeta), getBlock().getHarvestLevel(aMeta)));
		while (aList.remove(null));
	}
	
	@OnlyIn(Dist.CLIENT) public CreativeModeTab getCreativeTab() {return getBlock().getCreativeTabToDisplayOn();}
	public boolean func_150936_a(Level aWorld, int aX, int aY, int aZ, int aSide, Player aPlayer, ItemStack aStack) {return T;}
	public boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return mPlaceable.onItemUseFirst(this, aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ);}
	public boolean onItemUse(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return mPlaceable.onItemUse(this, aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ);}
	public IIcon getIconFromDamage(int aMeta) {return getBlock().getIcon(SIDE_TOP, aMeta);}
	@Override public Block getBlock() {return super.getBlock();}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	public String getUnlocalizedName(ItemStack aStack) {return mPlaceable.name(UT.Code.bind4(getDamage(aStack)));}
	public String getItemStackDisplayName(ItemStack aStack) {return I18n.translateToLocal(getUnlocalizedName(aStack));}
	public boolean placeBlockAt(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, int aMetaData) {return WD.set(aWorld, aX, aY, aZ, getBlock(), aMetaData, 3);}
	public int getItemStackLimit(ItemStack aStack) {return mPlaceable.getItemStackLimit(aStack);}
	public int getMetadata(int aMeta) {return aMeta;}
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return mPlaceable.onItemRightClick(aStack, aWorld, aPlayer);}
}
