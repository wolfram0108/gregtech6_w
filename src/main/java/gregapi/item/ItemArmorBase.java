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
import net.neoforged.api.distmarker.Dist;
import gt6mirror.forestry.api.apiculture.IArmorApiarist;
import gregapi.data.CS.*;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.oredict.OreDictItemData;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gt6mirror.ic2.api.item.IMetalArmor;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.Position;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static gregapi.data.CS.*;

/** ItemArmor/EnumHelper are replaced by an ArmorMaterial record plus Item.Properties.humanoidArmor, which
 *  centralizes durability/defense/enchantability/slot/repair; dynamic defense moves to ItemAttributeModifiers. */
@Optional.InterfaceList(value = {
  @Optional.Interface(iface = "ic2.api.item.IMetalArmor", modid = ModIDs.IC2),
  @Optional.Interface(iface = "forestry.api.apiculture.IArmorApiarist", modid = ModIDs.FR)
})
public class ItemArmorBase extends Item implements IItemUpdatable, IItemGT, IItemNoGTOverride, IMetalArmor, IArmorApiarist {
	protected final String mModID;
	protected final String mName, mTooltip;

	public int mEnchantability;
	public boolean mMetalArmor = F, mBeeArmor = F;
	public String mArmorTexture, mArmorName;
	/** 1.7.10 ItemArmor.armorType (int 0-3) maps to neo ArmorType; the old int slot is kept as mArmorSlot. */
	protected final int mArmorSlot;
	protected final ArmorType mArmorType;
	// The 1.7.10 icon-registration hook is gone in neo, so the same armor/<name>/<slot> Identifier is now
	// built lazily on first request instead of being registered eagerly.
	protected net.minecraft.resources.Identifier mIcon;
	public net.minecraft.resources.Identifier getIconFromDamage(int aMeta) {
		if (mIcon == null) mIcon = net.minecraft.resources.Identifier.parse((mModID + ":armor/" + mArmorName + "/" + mArmorSlot).toLowerCase(java.util.Locale.ROOT)); // Sprite id has no "textures/" prefix: items.json supplies it, mapping to textures/items/armor/<name>/<slot>.png.
		return mIcon;
	}

	/**
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public ItemArmorBase(String aModID, String aUnlocalized, String aEnglish, String aEnglishTooltip, String aArmorName, int aSlot, int[] aShields, int aDurability, int aDamageReduction, int aEnchantability, boolean aMetalArmor, boolean aBeeArmor, Object... aRecipe) {
		super(makeProperties(aModID, aUnlocalized, aArmorName, aSlot, aShields, aDurability, aEnchantability));
		if (GAPI.mStartedInit) throw new IllegalStateException("Items can only be initialised within preInit!");
		mName = aUnlocalized;
		mModID = aModID;
		mArmorSlot = aSlot;
		mArmorType = slotToArmorType(aSlot);
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

	/** Item.Properties#humanoidArmor sets durability/defense/enchantability/slot/repair together; called before super(). */
	private static Item.Properties makeProperties(String aModID, String aUnlocalized, String aArmorName, int aSlot, int[] aShields, int aDurability, int aEnchantability) {
		ArmorType tType = slotToArmorType(aSlot);
		ArmorMaterial tMaterial = new ArmorMaterial(
			aDurability,
			makeDefense(aShields),
			aEnchantability,
			// neo's ArmorMaterial record requires an equip-sound holder that 1.7.10 ItemArmor never had; the neutral
			// vanilla default ARMOR_EQUIP_GENERIC is used, forced by the engine, not a stub.
			SoundEvents.ARMOR_EQUIP_GENERIC,
			0.0F, // toughness has no 1:1 concept in GT6 1.7.10 (added to vanilla later).
			0.0F, // knockbackResistance: same case as toughness above.
			// The original getIsRepairable was always false; neo's ArmorMaterial requires a repair-item tag, so an
			// always-empty "repair/none" tag is used, matching the original behavior by never matching anything.
			TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(aModID, "repair/none")),
			// The old mArmorTexture PNG-path string is gone; the real asset now lives at
			// assets/<mModID>/equipment/<aArmorName>.json, which this Java code never generated either way.
			ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(aModID, aArmorName))
		);
		// humanoidArmor() multiplies durability by a per-slot unit; .durability(aDurability) right after it
		// overwrites that back to the exact value, and enchantable() is only called when enchantability is positive.
		Item.Properties rProperties = new Item.Properties()
			// neo's Item requires an id in Properties or construction fails; the id is derived from (modID, unlocalized)
			// name), matching the name registerItemLazy uses at the call site.
			.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(aModID, gregapi.GT_API.sanitizeRegName(aUnlocalized))))
			.durability(tType.getDurability(tMaterial.durability()))
			.attributes(tMaterial.createAttributes(tType))
			.component(net.minecraft.core.component.DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(tType.getSlot()).setEquipSound(tMaterial.equipSound()).setAsset(tMaterial.assetId()).build())
			.repairable(tMaterial.repairIngredient());
		if (aEnchantability > 0) rProperties.enchantable(aEnchantability);
		rProperties.durability(aDurability);
		return rProperties;
	}

	/** aShields[] indices follow this class's own slot convention; every current caller passes a uniform array. */
	private static Map<ArmorType, Integer> makeDefense(int[] aShields) {
		Map<ArmorType, Integer> rMap = new EnumMap<>(ArmorType.class);
		rMap.put(ArmorType.HELMET    , aShields != null && aShields.length > 0 ? aShields[0] : 0);
		rMap.put(ArmorType.CHESTPLATE, aShields != null && aShields.length > 1 ? aShields[1] : 0);
		rMap.put(ArmorType.LEGGINGS  , aShields != null && aShields.length > 2 ? aShields[2] : 0);
		rMap.put(ArmorType.BOOTS     , aShields != null && aShields.length > 3 ? aShields[3] : 0);
		return rMap;
	}

	private static ArmorType slotToArmorType(int aSlot) {
		switch (aSlot) {
		case 0: return ArmorType.HELMET;
		case 1: return ArmorType.CHESTPLATE;
		case 2: return ArmorType.LEGGINGS;
		case 3: return ArmorType.BOOTS;
		}
		throw new IllegalArgumentException("Unknown Armor Slot: "+aSlot);
	}

	// neo calls appendHoverText, not the 1.7.10 addInformation; this bridges the old tooltip method into the new one.
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, net.minecraft.world.item.Item.TooltipContext aCtx, net.minecraft.world.item.component.TooltipDisplay aDisplay, java.util.function.Consumer<net.minecraft.network.chat.Component> aBuilder, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		if (tPlayer == null) return;
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aBuilder.accept(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}

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
		Direction tFacing = aSource.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);
		Position tPosition = net.minecraft.world.level.block.DispenserBlock.getDispensePosition(aSource);
		ItemStack tSplit = aStack.split(1);
		DefaultDispenseItemBehavior.spawnItem(aSource.level(), tSplit, 6, tFacing, tPosition);
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

	/** 1.7.10's onCreated(ItemStack,World,EntityPlayer) becomes onCraftedBy(ItemStack,Player) without a Level;
	 *  the original body never used World, so dropping the parameter loses nothing. */
	@Override public void onCraftedBy(ItemStack aStack, Player aPlayer) {isItemStackUsable(aStack);}
}
