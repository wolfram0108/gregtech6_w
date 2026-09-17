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

import gregapi.api.Optional;
import net.minecraftforge.api.distmarker.Dist;
import forestry.api.apiculture.IArmorApiarist;
import gregapi.data.CS.*;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.oredict.OreDictItemData;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import ic2.api.item.IMetalArmor;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.Position;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/** @author Gregorius Techneticies
 *  1.20.1's ArmorMaterial is an interface, not a data record, so it matches 1.7.10's EnumHelper.addArmorMaterial again;
 *  ISpecialArmor is gone, so defense flows through attributes ArmorItem builds from material.getDefenseForType. */
@Optional.InterfaceList(value = {
  @Optional.Interface(iface = "ic2.api.item.IMetalArmor", modid = ModIDs.IC2),
  @Optional.Interface(iface = "forestry.api.apiculture.IArmorApiarist", modid = ModIDs.FR)
})
public class ItemArmorBase extends ArmorItem implements IItemUpdatable, IItemGT, IItemNoGTOverride, IMetalArmor, IArmorApiarist {
	protected final String mModID;
	protected final String mName, mTooltip;

	public int mEnchantability;
	public boolean mMetalArmor = F, mBeeArmor = F;
	public String mArmorTexture, mArmorName;
	/** 1.7.10's ItemArmor.armorType (int 0-3) is kept as-is; getType() is the typed view of the same slot. */
	protected final int mArmorSlot;
	// The old icon-registration hook died with the port (registerIcons is dead in neo), so armor stopped rendering at all.
	// Restored 1:1: the same ResourceLocation (armor/<name>/<slot>) is built lazily on first request instead.
	protected net.minecraft.resources.ResourceLocation mIcon;
	public net.minecraft.resources.ResourceLocation getIconFromDamage(int aMeta) {
		if (mIcon == null) mIcon = new net.minecraft.resources.ResourceLocation((mModID + ":armor/" + mArmorName + "/" + mArmorSlot).toLowerCase(java.util.Locale.ROOT)); // Sprite id has no "textures/" prefix: items.json supplies it, mapping to textures/items/armor/<name>/<slot>.png.
		return mIcon;
	}

	/**
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public ItemArmorBase(String aModID, String aUnlocalized, String aEnglish, String aEnglishTooltip, String aArmorName, int aSlot, int[] aShields, int aDurability, int aDamageReduction, int aEnchantability, boolean aMetalArmor, boolean aBeeArmor, Object... aRecipe) {
		super(new Material("armor."+aUnlocalized, aShields, aEnchantability), slotToArmorType(aSlot), new Item.Properties().durability(aDurability));
		if (GAPI.mStartedInit) throw new IllegalStateException("Items can only be initialised within preInit!");
		mName = aUnlocalized;
		mModID = aModID;
		mArmorSlot = aSlot;
		mArmorTexture = mModID+":"+TEX_DIR_ARMOR+aArmorName+"/"+mArmorSlot+".png";
		mArmorName = aArmorName;
		mEnchantability = aEnchantability;
		mMetalArmor = aMetalArmor;
		mBeeArmor = aBeeArmor;
		LH.add(mName, aEnglish);
		// Centralized through CreativeTabsGT.assign and BuildCreativeModeTabContentsEvent, not a direct setCreativeTab call.
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.COMBAT);
		if (UT.Code.stringValid(aEnglishTooltip)) LH.add(mTooltip = mName + ".tooltip_main", aEnglishTooltip); else mTooltip = null;
		// Self-registration is removed: construction now happens on RegisterEvent via GT_API.registerItemLazy, since
		// Item.<init> needs an unfrozen registry; recipe-stack creation is deferred to server-start for the same reason.
		if (aRecipe != null && aRecipe.length > 0) {
			final Object[] fRecipe = aRecipe;
			gregapi.GT_API.deferItemInit(() -> {
				CR.shaped(ST.make(this, 1, 0), CR.DEF_REV_NCC, fRecipe);
				OreDictItemData tData = OM.data(ST.make(this, 1, 0));
				if (tData != null) tData.setUseVanillaDamage();
			});
		}
	}

	/** Direct equivalent of EnumHelper.addArmorMaterial, declared as-is since ArmorMaterial is an interface on 1.20.1.
	 *  The material's own durability plays no role: durability is set absolutely by Properties.durability in the ctor. */
	private static final class Material implements ArmorMaterial {
		private final String mMaterialName;
		private final int[] mShields;
		private final int mEnchantability;
		private Material(String aName, int[] aShields, int aEnchantability) {mMaterialName = aName; mShields = aShields; mEnchantability = aEnchantability;}

		/** aShields[] indices are 1:1 with GT6's own slot convention (0=head, 1=chest, 2=legs, 3=boots), kept in that exact order. */
		@Override public int getDefenseForType(ArmorItem.Type aType) {
			int tIndex = armorTypeToSlot(aType);
			return mShields != null && mShields.length > tIndex ? mShields[tIndex] : 0;
		}
		/** Unused, since durability is set absolutely (see the class javadoc); returning 1 keeps defaultDurability harmless. */
		@Override public int getDurabilityForType(ArmorItem.Type aType) {return 1;}
		@Override public int getEnchantmentValue() {return mEnchantability;}
		@Override public SoundEvent getEquipSound() {return SoundEvents.ARMOR_EQUIP_GENERIC;}
		/** The original's getIsRepairable was always false; an empty ingredient matches nothing, giving the same result. */
		@Override public Ingredient getRepairIngredient() {return Ingredient.EMPTY;}
		@Override public String getName() {return mMaterialName;}
		@Override public float getToughness() {return 0.0F;}
		@Override public float getKnockbackResistance() {return 0.0F;}
	}

	private static ArmorItem.Type slotToArmorType(int aSlot) {
		switch (aSlot) {
		case 0: return ArmorItem.Type.HELMET;
		case 1: return ArmorItem.Type.CHESTPLATE;
		case 2: return ArmorItem.Type.LEGGINGS;
		case 3: return ArmorItem.Type.BOOTS;
		}
		throw new IllegalArgumentException("Unknown Armor Slot: "+aSlot);
	}

	private static int armorTypeToSlot(ArmorItem.Type aType) {
		switch (aType) {
		case HELMET    : return 0;
		case CHESTPLATE: return 1;
		case LEGGINGS  : return 2;
		case BOOTS     : return 3;
		}
		return 0;
	}

	// The engine calls appendHoverText, not 1.7.10's addInformation; this bridges to GT6's own addInformation-based tooltip.
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, Level aWorld, List<net.minecraft.network.chat.Component> aBuilder, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		if (tPlayer == null) return;
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aBuilder.add(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}

	/** 1:1 with the original: the same Forge channel, returning the same string. */
	@Override public String getArmorTexture(ItemStack aStack, Entity aEntity, EquipmentSlot aSlot, String aType) {return mArmorTexture;}

	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		if (aStack.getMaxDamage() > 0) aList.add((aStack.getMaxDamage() - aStack.getDamageValue()) + " / " + aStack.getMaxDamage());
		if (mTooltip != null) aList.add(LanguageHandler.translate(mTooltip, mTooltip));
		addAdditionalToolTips(aList, aStack, aF3_H);
		while (aList.remove(null));
	}

	protected void addAdditionalToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		//
	}

	public ItemStack onDispense(BlockSource aSource, ItemStack aStack) {
		Direction tFacing = aSource.getBlockState().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);
		Position tPosition = net.minecraft.world.level.block.DispenserBlock.getDispensePosition(aSource);
		ItemStack tSplit = aStack.split(1);
		DefaultDispenseItemBehavior.spawnItem(aSource.getLevel(), tSplit, 6, tFacing, tPosition);
		return aStack;
	}

	/** The 1.7.10 dispense behavior with a null projectile entity was already dead code (plain dispense); neo's
	 *  ProjectileDispenseBehavior requires a real ProjectileItem, so this correctly reduces to DefaultDispenseItemBehavior. */
	public static class GT_Item_Dispense extends DefaultDispenseItemBehavior {
		@Override
		protected ItemStack execute(BlockSource aSource, ItemStack aStack) {
			return ((ItemArmorBase)aStack.getItem()).onDispense(aSource, aStack);
		}
	}

	// IMetalArmor/IArmorApiarist are compat-mirror interfaces (IC2/Forestry not loaded); the methods stay
	// functional but without @Override while the mirror interfaces are empty, ready for real integration later.
	public boolean isMetalArmor(ItemStack aStack, Player aPlayer) {return mMetalArmor;}
	public boolean protectEntity(LivingEntity aPlayer, ItemStack aArmor, String aCause, boolean doProtect) {return mBeeArmor;}
	public boolean protectPlayer(Player aPlayer, ItemStack aArmor, String aCause, boolean doProtect) {return mBeeArmor;}
	public String toString() {return mName;}
	/** neo's Item.getDescriptionId() is final and can't be overridden, so this stays a domain method GT6 uses internally. */
	public final String getUnlocalizedName() {return mName;}
	// getName(ItemStack) resolves the GT6 armor name via the language handler; otherwise falls back to the raw lang key.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = gregapi.lang.LanguageHandler.get(mName); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	public String getUnlocalizedName(ItemStack aStack) {return mName;}
	public boolean isItemStackUsable(ItemStack aStack) {return T;}
	public ItemStack make(long aMetaData) {return ST.make(this, 1, aMetaData);}
	public ItemStack make(long aAmount, long aMetaData) {return ST.make(this, aAmount, aMetaData);}

	@Override public void updateItemStack(ItemStack aStack) {isItemStackUsable(aStack);}
	@Override public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {updateItemStack(aStack);}

	/** 1:1: the old onCreated(ItemStack,World,EntityPlayer) is onCraftedBy(ItemStack,Level,Player) again on 1.20.1, unchanged. */
	@Override public void onCraftedBy(ItemStack aStack, Level aWorld, Player aPlayer) {isItemStackUsable(aStack);}
}
