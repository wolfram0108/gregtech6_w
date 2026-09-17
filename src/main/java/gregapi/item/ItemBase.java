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

package gregapi.item;

import net.minecraftforge.api.distmarker.Dist;
import gregapi.code.TagData;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class ItemBase extends Item implements IItemProjectile, IItemUpdatable, IItemGT, IItemNoGTOverride {
	protected ResourceLocation mIcon;
	protected final String mModID;
	protected final String mName, mTooltip;
	/** neo's Item is immutable and stack size is a data component, but this GT6 item class keeps its own field
	 *  and overrides getMaxStackSize at the engine's normal override point; the default of 64 matches vanilla. */
	protected int mMaxStackSize = 64;
	public ItemBase setMaxStackSize(int aMaxStackSize) {mMaxStackSize = aMaxStackSize; return this;}
	@Override public int getMaxStackSize(ItemStack aStack) {return mMaxStackSize;}
	/** neo's Item is immutable and max damage is set via Properties.durability at registration, but this GT6
	 *  item class keeps its own field and overrides getMaxDamage; the default of 0 matches vanilla (no durability). */
	protected int mMaxDamage = 0;
	public ItemBase setMaxDamage(int aMaxDamage) {mMaxDamage = aMaxDamage; return this;}
	@Override public int getMaxDamage(ItemStack aStack) {return mMaxDamage;}
	/** neo dropped the simple no-repair override flag (repair now runs through isValidRepairItem+durability),
	 *  but GT6 tools have their own repair logic that still needs this flag. */
	protected boolean mNoRepair = false;
	public ItemBase setNoRepair() {mNoRepair = true; return this;}
	/** 1.7.10's setFull3D()/isFull3D() made the client hold the item like a handle instead of flat; the port's
	 *  setter was a no-op, so tools rendered flat, fixed via the item's model, GT6ItemModel.flatItemTransforms. */
	protected boolean mFull3D = false;
	public ItemBase setFull3D() {mFull3D = true; return this;}
	public boolean isFull3D() {return mFull3D;}
	/** The subtype flag has real consumers here and in MultiItem's unoverridden getUnlocalizedName(ItemStack),
	 *  so it cannot be a no-op without silently losing the subtype suffix from item names. */
	protected boolean mHasSubtypes = F;
	public ItemBase setHasSubtypes(boolean aHasSubtypes) {mHasSubtypes = aHasSubtypes; return this;}
	public boolean getHasSubtypes() {return mHasSubtypes;}

	// The engine no longer routes right-click through the old onItemUseFirst/onBlockActivated/onItemUse chain,
	// orphaning MultiItem's tool behaviors; this bridges them back through IItemGT.bridgeUseOn*.
	@Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext aCtx) {return IItemGT.bridgeUseOn(this, aCtx);}
	@Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack aStack, net.minecraft.world.item.context.UseOnContext aCtx) {return IItemGT.bridgeUseOnFirst(this, aCtx);}
	/** 1.7.10 contract for right-click-in-air (the engine put the returned stack back in hand); default is the
	 *  vanilla no-op of returning the same stack, MultiItem overrides it with its behavior-list dispatch. */
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	@Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level aWorld, Player aPlayer, net.minecraft.world.InteractionHand aHand) {
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		ItemStack tResult = onItemRightClick(tStack, aWorld, aPlayer);
		if (tResult != tStack) {aPlayer.setItemInHand(aHand, ST.nn(tResult)); return net.minecraft.world.InteractionResultHolder.success(tResult);} // The 1.7.10 contract can return null for a consumed item; only the central helper puts that back in the engine's hand.
		return super.use(aWorld, aPlayer, aHand);
	}

	/**
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public ItemBase(String aModID, String aUnlocalized, String aEnglish, String aEnglishTooltip) {
		// neo's Item.<init> requires an id in Properties or it throws; the id is built from (mModID, mName), matching
		// the name used to register this item on the DeferredRegister at the call site.
		super(new Item.Properties());
		if (GAPI.mStartedInit) throw new IllegalStateException("Items can only be initialised within preInit!");
		mName = aUnlocalized;
		mModID = aModID;
		LH.add(mName, aEnglish);
		if (UT.Code.stringValid(aEnglishTooltip)) LH.add(mTooltip = mName + ".tooltip_main", aEnglishTooltip); else mTooltip = null;
		// Self-registration is removed from the constructor: construction now happens on RegisterEvent via a
		// DeferredRegister supplier at the call site, since self-registering after construction is too late for it.
		DispenserBlock.registerBehavior(this, new GT_Item_Dispense()); // Replaces the removed Forge dispenseBehaviorRegistry.putObject call.
	}
	
	// neo calls appendHoverText, not the 1.7.10 addInformation; this bridge previously existed only on MultiItem,
	// so ItemBase and its other subclasses never had their tooltips (durability, mTooltip) reach the engine at all.
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, net.minecraft.world.level.Level aWorld, java.util.List<net.minecraft.network.chat.Component> aTooltips, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		if (tPlayer == null) return;
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aTooltips.add(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}

	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		try {
			if (getMaxDamage(aStack) > 0 && !getHasSubtypes()) aList.add((aStack.getMaxDamage() - getDamage(aStack)) + " / " + aStack.getMaxDamage());
			if (mTooltip != null) aList.add(LanguageHandler.translate(mTooltip, mTooltip));
			addAdditionalToolTips(aList, aStack, aF3_H);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		while (aList.remove(null));
	}
	
	protected void addAdditionalToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		//
	}
	
	public ItemStack onDispense(BlockSource aSource, ItemStack aStack) {
		Direction enumfacing = aSource.getBlockState().getValue(DispenserBlock.FACING); // Replaces the removed func_149937_b(getBlockMetadata()) facing lookup with BlockSource's own state/pos.
		Position iposition = DispenserBlock.getDispensePosition(aSource);
		ItemStack itemstack1 = aStack.split(1);
		DefaultDispenseItemBehavior.spawnItem(aSource.getLevel(), itemstack1, 6, enumfacing, iposition); // Replaces the removed doDispense hook with the engine's own spawnItem.
		return aStack;
	}

	/** The 1.7.10 dispense behavior with a null projectile entity was already dead code; neo's ProjectileDispenseBehavior
	 *  requires a real ProjectileItem, so this correctly reduces to DefaultDispenseItemBehavior. */
	public static class GT_Item_Dispense extends DefaultDispenseItemBehavior {
		@Override
		protected ItemStack execute(BlockSource aSource, ItemStack aStack) {
			return ((ItemBase)aStack.getItem()).onDispense(aSource, aStack);
		}
	}
	
	@Override public boolean hasProjectile(TagData aProjectileType, ItemStack aStack) {return F;}
	@Override public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, double aX, double aY, double aZ) {return null;}
	@Override public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, LivingEntity aEntity, float aSpeed) {return null;}
	public final Item setUnlocalizedName(String aName) {return this;}
	@Override public String toString() {return mName;}
	public final String getUnlocalizedName() {return mName;}
	public String getUnlocalizedName(ItemStack aStack) {return getHasSubtypes()?mName+"."+ST.meta_(aStack):mName;}
	public String getItemStackDisplayName(ItemStack aStack) {return gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack));}
	// neo resolves the display name through getName(ItemStack), not the 1.7.10 getItemStackDisplayName; without
	// this bridge to the GT6 name it would fall back to vanilla lang and show raw keys.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	// No getDamage(ItemStack) override here, deliberately: Forge's default already returns the subtype from
	// raw Damage, and calling back through ST.meta_ would close the loop on itself.
	public final boolean getShareTag() {return T;} // just to be sure.
	// The old icon-registration hook (IIconRegister) is gone; the same path is now used to build a ResourceLocation directly.
	public void registerIcons(Object aIconRegister) {mIcon = new ResourceLocation((mModID + ":" + mName).toLowerCase(java.util.Locale.ROOT));}
	// registerIcons is never called in neo, so mIcon stayed null and the item failed to render; it is now built
	// lazily on first request using the same "modid:name" path.
	public ResourceLocation getIconFromDamage(int aMeta) {if (mIcon == null) registerIcons(null); return mIcon;}
	public void onCreated(ItemStack aStack, Level aWorld, Player aPlayer) {isItemStackUsable(aStack);}
	public ItemStack getContainerItem(ItemStack aStack) {return null;}
	public boolean hasContainerItem(ItemStack aStack) {return getContainerItem(aStack) != null;}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	@Override public void updateItemStack(ItemStack aStack) {isItemStackUsable(aStack);}
	@Override public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {updateItemStack(aStack);}
	public boolean doesSneakBypassUse(Level aWorld, int aX, int aY, int aZ, Player aPlayer) {return T;}
	public boolean isItemStackUsable(ItemStack aStack) {return T;}
	public ItemStack make(long aMetaData) {return ST.make(this, 1, aMetaData);}
	public ItemStack make(long aAmount, long aMetaData) {return ST.make(this, aAmount, aMetaData);}
}
