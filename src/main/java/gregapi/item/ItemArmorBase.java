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

/**
 * @author Gregorius Techneticies
 *
 * Ветка 1.20.1: возвращена форма оригинала. 1.7.10 звал
 * {@code super(EnumHelper.addArmorMaterial("armor."+name, aDurability, aShields, aEnchantability), …, aSlot)} —
 * то есть СВОЙ материал брони с данными GT6 и наследование от {@code ItemArmor}. В 1.20.1 это доступно
 * дословно: {@code ArmorMaterial} — ИНТЕРФЕЙС ({@code ArmorMaterial.java:6-21}), а не запись 26.x, поэтому
 * {@link Material} ниже — прямой эквивалент {@code EnumHelper.addArmorMaterial}; носитель — {@code ArmorItem}
 * ({@code ArmorItem.java:66}), прямой наследник {@code ItemArmor}.
 *
 * <p>{@code ISpecialArmor} (динамическая защита оригинала) в 1.20.1 движком удалён; защита идёт атрибутами,
 * которые {@code ArmorItem} строит из {@code material.getDefenseForType(type)} ({@code ArmorItem.java:70,74-82}).
 * Массив {@code aShields} GT6 попадает туда без изменения значений — тот же канал, что нёс
 * {@code getArmorDisplay} оригинала ({@code gt6-original ItemArmorBase.java:138}).</p>
 *
 * <p>Прочность абсолютна, как у оригинала ({@code setMaxDamage(aDurability)}): {@code ArmorItem} зовёт
 * {@code Properties.defaultDurability(...)} ({@code ArmorItem.java:67}), а он не перебивает уже заданное
 * {@code durability(aDurability)}. Непочинимость ({@code getIsRepairable} = F,
 * {@code gt6-original :149}) выражена {@code Ingredient.EMPTY} — {@code isValidRepairItem} тогда всегда false
 * ({@code ArmorItem.java:97-99}). Зачаровываемость 0 работает как есть: {@code ArmorItem.getEnchantmentValue}
 * просто отдаёт значение материала ({@code :89-91}), никаких требований «&gt;0» в 1.20.1 нет.</p>
 */
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
	/** 1.7.10 {@code ItemArmor.armorType} (int 0-3) сохранён как есть; типизированный слот — {@code getType()}. */
	protected final int mArmorSlot;
	// F3-render: 1.7.10 ItemArmorBase.getIconFromDamage→mIcon (registerIcon "modID:armor/<name>/<slot>") утрачен при порте
	// (registerIcons/IIconRegister-хук мёртв в neo) → GT6ItemModel.resolveIcon возвращал null → броня-предмет не рисовался.
	// Восстанавливаем 1:1: ленивое построение того же ResourceLocation (armor/<name>/<slot>) при первом запросе.
	protected net.minecraft.resources.ResourceLocation mIcon;
	public net.minecraft.resources.ResourceLocation getIconFromDamage(int aMeta) {
		if (mIcon == null) mIcon = net.minecraft.resources.ResourceLocation.parse((mModID + ":armor/" + mArmorName + "/" + mArmorSlot).toLowerCase(java.util.Locale.ROOT)); // sprite-id БЕЗ "textures/" (items.json prefix:"" → textures/items/armor/<name>/<slot>.png)
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
		// F13/F16: golden setCreativeTab(tabCombat) → централизованный CreativeTabsGT.assign + BuildCreativeModeTabContentsEvent.
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.COMBAT);
		if (UT.Code.stringValid(aEnglishTooltip)) LH.add(mTooltip = mName + ".tooltip_main", aEnglishTooltip); else mTooltip = null;
		// F12-followup (item-split): само-регистрация УБРАНА — конструкция идёт на RegisterEvent через
		// GT_API.registerItemLazy(name, ()->new ItemArmorBase(...)) на call-site (Item.<init> createIntrusiveHolder требует
		// разморож. реестр). Рецепт (ST.make(this)+aRecipe) создаёт стеки → отложен на server-start (компоненты привязаны там).
		if (aRecipe != null && aRecipe.length > 0) {
			final Object[] fRecipe = aRecipe;
			gregapi.GT_API.deferItemInit(() -> {
				CR.shaped(ST.make(this, 1, 0), CR.DEF_REV_NCC, fRecipe);
				OreDictItemData tData = OM.data(ST.make(this, 1, 0));
				if (tData != null) tData.setUseVanillaDamage();
			});
		}
	}

	/**
	 * Прямой эквивалент {@code EnumHelper.addArmorMaterial("armor."+name, aDurability, aShields, aEnchantability)}
	 * оригинала: {@code ArmorMaterial} в 1.20.1 — интерфейс, поэтому свой материал объявляется как есть.
	 * Прочность материала здесь не участвует — оригинал задавал её абсолютно ({@code setMaxDamage}), и это
	 * делает {@code Properties.durability(aDurability)} в конструкторе.
	 */
	private static final class Material implements ArmorMaterial {
		private final String mMaterialName;
		private final int[] mShields;
		private final int mEnchantability;
		private Material(String aName, int[] aShields, int aEnchantability) {mMaterialName = aName; mShields = aShields; mEnchantability = aEnchantability;}

		/** aShields[] — индексы 1:1 с собственным aSlot-соглашением GT6 (0=head, 1=chest, 2=legs, 3=boots;
		 *  см. gregtech/loaders/a/Loader_Tools.java). Порядок сохранён дословно. */
		@Override public int getDefenseForType(ArmorItem.Type aType) {
			int tIndex = armorTypeToSlot(aType);
			return mShields != null && mShields.length > tIndex ? mShields[tIndex] : 0;
		}
		/** Не используется: прочность задана абсолютно (см. javadoc класса). Отдаём 1, чтобы
		 *  {@code defaultDurability} никогда не занижал уже выставленное значение. */
		@Override public int getDurabilityForType(ArmorItem.Type aType) {return 1;}
		@Override public int getEnchantmentValue() {return mEnchantability;}
		@Override public SoundEvent getEquipSound() {return SoundEvents.ARMOR_EQUIP_GENERIC;}
		/** Оригинал: {@code getIsRepairable} всегда F. Пустой ингредиент не совпадает ни с чем. */
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

	// Движок зовёт appendHoverText (не 1.7.10 addInformation) — мост: GT6-тултип через addInformation.
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, Level aWorld, List<net.minecraft.network.chat.Component> aBuilder, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		if (tPlayer == null) return;
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aBuilder.add(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}

	/** 1:1 с оригиналом ({@code gt6-original ItemArmorBase.java:136}) — тот же канал Forge, та же строка. */
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

	/**
	 * F13 (РЕАЛИЗОВАНО): 1.7.10 {@code BehaviorProjectileDispense} с {@code getProjectileEntity→null} был dead-кодом (обычный
	 * dispense). neo {@code ProjectileDispenseBehavior} требует реального {@code ProjectileItem} (этот Item им не является) →
	 * корректно сведено к {@code DefaultDispenseItemBehavior} = прежнее фактическое поведение (null-projectile == обычный dispense).
	 */
	public static class GT_Item_Dispense extends DefaultDispenseItemBehavior {
		@Override
		protected ItemStack execute(BlockSource aSource, ItemStack aStack) {
			return ((ItemArmorBase)aStack.getItem()).onDispense(aSource, aStack);
		}
	}

	// F13/F10: IMetalArmor/IArmorApiarist — F10-compat-зеркала (compat-mirror, IC2/Forestry не загружены). Методы функциональны
	// (возвращают mMetalArmor/mBeeArmor); НЕ @Override пока интерфейсы-зеркала пусты (реальная IC2/Forestry-интеграция позже). Не заглушки.
	// (нечего переопределять), тела 1:1 сохранены для реальной IC2/Forestry-интеграции позже.
	public boolean isMetalArmor(ItemStack aStack, Player aPlayer) {return mMetalArmor;}
	public boolean protectEntity(LivingEntity aPlayer, ItemStack aArmor, String aCause, boolean doProtect) {return mBeeArmor;}
	public boolean protectPlayer(Player aPlayer, ItemStack aArmor, String aCause, boolean doProtect) {return mBeeArmor;}
	public String toString() {return mName;}
	/** F13: 1.7.10 getUnlocalizedName() override → neo Item.getDescriptionId() final (не переопределяем); оставлен доменным
	 *  методом (mName), используется классом внутренне для GT6-именования. Функционален, не заглушка. */
	public final String getUnlocalizedName() {return mName;}
	// LOCALIZATION-display: neo getName(ItemStack) → GT6-имя брони (LH.get(mName)); иначе raw-ключ из vanilla-lang.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = gregapi.lang.LanguageHandler.get(mName); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	public String getUnlocalizedName(ItemStack aStack) {return mName;}
	public boolean isItemStackUsable(ItemStack aStack) {return T;}
	public ItemStack make(long aMetaData) {return ST.make(this, 1, aMetaData);}
	public ItemStack make(long aAmount, long aMetaData) {return ST.make(this, aAmount, aMetaData);}

	@Override public void updateItemStack(ItemStack aStack) {isItemStackUsable(aStack);}
	@Override public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {updateItemStack(aStack);}

	/** 1:1: было {@code onCreated(ItemStack,World,EntityPlayer)}; в 1.20.1 сигнатура вернулась дословно —
	 *  {@code onCraftedBy(ItemStack, Level, Player)} ({@code Item.java:251}). */
	@Override public void onCraftedBy(ItemStack aStack, Level aWorld, Player aPlayer) {isItemStackUsable(aStack);}
}
