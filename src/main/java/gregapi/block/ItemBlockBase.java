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

package gregapi.block;
import gregapi.util.WD;

import net.neoforged.api.distmarker.Dist;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.item.IItemGT;
import gregapi.render.ITexture;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

public class ItemBlockBase extends BlockItem implements IBlock, IItemGT {
	public final IBlockBase mPlaceable;

	public ItemBlockBase(Block aBlock) {
		// F12-followup (item-split): neo Item requires an id in Properties (otherwise "Item id not set"); BlockItem shares its id with the block —
		// derived from the already-registered block's key (item construction happens on RegisterEvent<Item>, after the block).
		super(aBlock, new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aBlock))));
		mPlaceable = (IBlockBase)aBlock;
		setMaxDamage(0);
		setHasSubtypes(T);
	}

	// F-bounds: ItemBlockBase is a wrapper over Block (mPlaceable/getBlock()), it holds no geometry of its own ->
	// routes to the WD.setBlockBounds CENTER (already exists, gregapi/util/WD.java:122), the same approach used
	// throughout the rest of the F-bounds seam.
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		WD.setBlockBounds(getBlock(), aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ);
	}
	// Symmetric to setBlockBounds: reads come from the wrapped block (it holds nothing itself).
	@Override public float[] getRenderBounds() {return getBlock() instanceof IBlock tI ? tI.getRenderBounds() : null;}
	
	// F13: neo BlockItem calls appendHoverText (not the 1.7.10 addInformation) — a bridge: assemble the GT6 tooltip (List<String>) via
	// addInformation below, hand it to the neo builder as a Component. Player comes from the client proxy (getThePlayer, null on the server → skip).
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, net.minecraft.world.item.Item.TooltipContext aCtx, net.minecraft.world.item.component.TooltipDisplay aDisplay, java.util.function.Consumer<net.minecraft.network.chat.Component> aBuilder, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		if (tPlayer == null) return;
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aBuilder.accept(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}

	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		// F13: GT6 tooltip (1.7.10 addInformation style); the engine calls it through the appendHoverText bridge above.
		byte aMeta = UT.Code.bind4(ST.meta_(aStack));
		mPlaceable.addInformation(aStack, aMeta, aPlayer, aList, aF3_H);
		if (WD.hasCollide(aPlayer.level(), 0, 0, 0, getBlock())) {
			if (mPlaceable.doesWalkSpeed(aMeta)) aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_WALKSPEED));
			if (mPlaceable.canCreatureSpawn(aMeta)) {
				if (ITexture.Util.OPTIFINE_LOADED && aMeta != 0 && !mPlaceable.canCreatureSpawn((byte)0)) {
					aList.add(LH.Chat.BLINKING_RED + LH.get(gregapi.GT_API.api_proxy.isSingleplayer() ? LH.TOOLTIP_SPAWNPROOF_SP_BUG    : LH.TOOLTIP_SPAWNPROOF_MP_BUG   ));
					aList.add(LH.Chat.BLINKING_RED + LH.get(LH.TOOLTIP_SPAWNPROOF_OPTIFINE));
				}
			} else {
				if (ITexture.Util.OPTIFINE_LOADED && aMeta != 0 &&  mPlaceable.canCreatureSpawn((byte)0)) {
					aList.add(LH.Chat.BLINKING_RED + LH.get(gregapi.GT_API.api_proxy.isSingleplayer() ? LH.TOOLTIP_SPAWNPROOF_SP_BROKEN : LH.TOOLTIP_SPAWNPROOF_MP_BROKEN));
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
		
		// F-tool: Block.getHarvestTool/getHarvestLevel were removed from vanilla neo (getBlock() is statically typed as vanilla Block).
		// GT6 data lives on BlockBase (getHarvestTool/getHarvestLevel:87-88) — routed through a cast (the path DOES exist, not a stub).
		String tHarvestTool = TOOL_pickaxe; int tHarvestLevel = 0;
		if (getBlock() instanceof gregapi.block.BlockBase tBB) {tHarvestTool = tBB.getHarvestTool(aMeta); tHarvestLevel = tBB.getHarvestLevel(aMeta);}
		aList.add(LH.getToolTipHarvest(WD.getMaterial(getBlock()), tHarvestTool, tHarvestLevel));
		while (aList.remove(null));
	}
	
	// F16 dead-interface: getCreativeTab is NOT called by the neo engine (the per-block getter was removed; tabs are event-based).
	// Block membership in a tab is wired centrally: BlockBase ctor → CreativeTabsGT.assign(...). This method is dead (0 callers).
	public CreativeModeTab getCreativeTab() {return null;}
	public boolean func_150936_a(Level aWorld, int aX, int aY, int aZ, int aSide, Player aPlayer, ItemStack aStack) {return T;}
	// F-useOn bridge: neo calls useOn(UseOnContext)/onItemUseFirst(ItemStack,UseOnContext), not the 1.7.10
	// onItemUse/onItemUseFirst(x,y,z,side,hit) — unpack+delegate into the existing bodies below (the IItemGT center).
	@Override public InteractionResult useOn(UseOnContext aCtx) {return IItemGT.bridgeUseOn(this, aCtx);}
	@Override public InteractionResult onItemUseFirst(ItemStack aStack, UseOnContext aCtx) {return IItemGT.bridgeUseOnFirst(this, aCtx);}
	@Override public boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return mPlaceable.onItemUseFirst(this, aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ);}
	@Override public boolean onItemUse(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return mPlaceable.onItemUse(this, aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ);}
	// F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode path is dead, 0 neo callers): was getBlock().getIcon(SIDE_TOP,aMeta) (vanilla Block.getIcon was removed in 26.1.2 —
	// getBlock() is statically typed as vanilla Block, not our BlockBase, so the IIconContainer center is unreachable here).
	public Identifier getIconFromDamage(int aMeta) {throw new UnsupportedOperationException("F3 dead-interface: 1.7.10 Item.getIconFromDamage(meta) is gone in neo (NOT @Override). ItemBlockBase is a BlockItem, rendered by its own block model (GT6BlockModel); GT6ItemModel skips BlockItems. Defensive throw.");}
	@Override public Block getBlock() {return super.getBlock();}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	public String getUnlocalizedName(ItemStack aStack) {return mPlaceable.name(UT.Code.bind4(getDamage(aStack)));}
	public String getItemStackDisplayName(ItemStack aStack) {return gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack));}
	// LOCALIZATION-display: neo getName(ItemStack) → the GT6 name (LH.get); otherwise the raw key from vanilla lang.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	// F1 contract (1.7.10 itemDamage==meta): verbatim GT6 code calls getDamage to get the block's subtype; neo's default reads
	// the DAMAGE component (0 for meta items). Restored at the root (like ItemBase/MTE): non-damageable → ST.meta_.
	@Override public int getDamage(ItemStack aStack) {return getMaxDamage(aStack) > 0 ? super.getDamage(aStack) : ST.meta_(aStack);}
	public boolean placeBlockAt(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, int aMetaData) {return WD.set(aWorld, aX, aY, aZ, getBlock(), aMetaData, 3);}
	// BUG-021 v2: a bridge from neo's per-stack channel to the 1.7.10 hook below — without it the ENTIRE block hierarchy (BlockBase/slabs/planks/
	// logs/leaves/saplings/BlockStones — their getItemStackLimit via OP.*.mDefaultStackSize) stacked at the vanilla default of 64.
	@Override public int getMaxStackSize(ItemStack aStack) {return UT.Code.bindStack(getItemStackLimit(aStack));}
	public int getItemStackLimit(ItemStack aStack) {return mPlaceable.getItemStackLimit(aStack);}
	public int getMetadata(int aMeta) {return aMeta;}
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return mPlaceable.onItemRightClick(aStack, aWorld, aPlayer);}
}
