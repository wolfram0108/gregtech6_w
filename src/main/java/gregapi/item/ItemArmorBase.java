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

package gregapi.item;

import gregapi.api.Optional;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

/**
 * @author Gregorius Techneticies
 *
 * PORT-TODO(F13, item-base компонентный редизайн): {@code ItemArmor}/{@code EnumHelper} (1.7.10 armor-модель)
 * не существуют в 26.1.2 целиком — замены: {@code ArmorMaterial} record + {@code Item.Properties.humanoidArmor}
 * (durability/defense/enchantability/equip-слот/repair из одной точки), см. конструктор ниже. Динамическая защита
 * через {@code ISpecialArmor.getProperties} (нет ни в одном из 3 корней референса — движко-модель Forge-core, НЕ
 * F10 compat-mod) заменена статическими {@code ItemAttributeModifiers} внутри {@link ArmorMaterial#createAttributes}.
 * Данные GT6 (durability/aShields/enchantability) сохранены 1:1 — меняется только канал доставки.
 */
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
	/** PORT-TODO(F13, item-base компонентный редизайн): было унаследованное поле {@code ItemArmor.armorType} (int 0-3). */
	protected final int mArmorSlot;
	protected final ArmorType mArmorType;

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
		// PORT-TODO(F13, creative-tab): Item.setCreativeTab не существует в 26.1.2 (creative-tab членство
		// теперь через BuildCreativeModeTabContentsEvent/CreativeModeTab.Builder.displayItems, не per-Item
		// сеттер) — тот же класс проблемы во всём gregtech.items, центра ещё нет нигде в дереве (grep=0).
		// setCreativeTab(CreativeModeTab.tabCombat);
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
	 * PORT-TODO(F13, item-base компонентный редизайн): единственный neo-путь к durability+defense+enchantability+
	 * equip-слот+repair одновременно — {@link Item.Properties#humanoidArmor(ArmorMaterial, ArmorType)}
	 * (neo-decompiled/net/minecraft/world/item/Item.java:579). Статический (не может ссылаться на поля
	 * экземпляра — вызывается до {@code super()}).
	 */
	private static Item.Properties makeProperties(String aModID, String aUnlocalized, String aArmorName, int aSlot, int[] aShields, int aDurability, int aEnchantability) {
		ArmorType tType = slotToArmorType(aSlot);
		ArmorMaterial tMaterial = new ArmorMaterial(
			aDurability,
			makeDefense(aShields),
			aEnchantability,
			// PORT-TODO(F13, item-base компонентный редизайн): 1.7.10 ItemArmor не имел Equip-Sound Konzept —
			// ArmorMaterial record требует Holder<SoundEvent> непременно, ближайший ванильный дефолт без 1:1-источника.
			SoundEvents.ARMOR_EQUIP_GENERIC,
			0.0F, // toughness: нет 1:1-концепта в GT6 1.7.10 (введено в ванили позже)
			0.0F, // knockbackResistance: тот же случай
			// PORT-TODO(F13, item-base компонентный редизайн): оригинал — getIsRepairable(...) всегда F (не
			// чинится вообще). ArmorMaterial требует TagKey<Item> непременно — пустой (никогда не заполняемый)
			// тег даёт тот же эффект (ничего не матчит => не чинится), 1:1 по СЛЕДСТВИЮ, не по механизму.
			TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(aModID, "repair/none")),
			// PORT-TODO(F3, baked-рендер клиента): было mArmorTexture-строка (PNG-путь), реальный держатель —
			// assets/<mModID>/equipment/<aArmorName>.json, клиентский ресурс не порождается этим Java-кодом
			// (оригинал тоже не порождал PNG из кода — только ссылался строкой).
			ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(aModID, aArmorName))
		);
		// R8-фикс (GPT-возврат): humanoidArmor() сам делает durability(type.getDurability(material.durability()))
		// (Item.java:580) — ArmorType.getDurability(int) УМНОЖАЕТ переданное значение на unitDurability слота
		// (ArmorType.java:25-27: HELMET=11/CHESTPLATE=16/LEGGINGS=15/BOOTS=13/BODY=16), т.е. итоговый MAX_DAMAGE
		// стал бы aDurability*unitDurability, а не aDurability. Оригинал (gregtech6/.../ItemArmorBase.java:92)
		// делал setMaxDamage(aDurability) АБСОЛЮТНО. .durability(aDurability) ниже перезаписывает
		// DataComponents.MAX_DAMAGE поверх (component()→componentInitializer.andThen(...set(type,value)),
		// последующий set с тем же DataComponentType в builder'е перезаписывает предыдущий, Item.java:692-695,
		// DataComponentInitializers.java:123-126) — итоговая прочность == aDurability, 1:1 с оригиналом.
		// R8-фикс + F-armor-enchant0: 1.7.10 hazmat-броня НЕПОКОРЯЕМА (aEnchantability=0), но neo humanoidArmor() (Item.java:579)
		// БЕЗУСЛОВНО зовёт enchantable(material.enchantmentValue()) → Enchantable.<init>:18 требует >0 → краш на RegisterEvent<Item>.
		// Воспроизводим humanoidArmor вручную 1:1, вызывая enchantable ТОЛЬКО при >0 (0 → компонент Enchantable опущен =
		// непокоряемо, 1:1 со СЛЕДСТВИЕМ оригинала). durability(aDurability) в конце перезаписывает поверх (R8, см. выше).
		Item.Properties rProperties = new Item.Properties()
			.durability(tType.getDurability(tMaterial.durability()))
			.attributes(tMaterial.createAttributes(tType))
			.component(net.minecraft.core.component.DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(tType.getSlot()).setEquipSound(tMaterial.equipSound()).setAsset(tMaterial.assetId()).build())
			.repairable(tMaterial.repairIngredient());
		if (aEnchantability > 0) rProperties.enchantable(aEnchantability);
		rProperties.durability(aDurability);
		return rProperties;
	}

	/** aShields[] — индексы 1:1 с этим же классом собственным aSlot-соглашением (0=head,1=chest,2=legs,3=boots, см. gregtech/loaders/a/Loader_Tools.java); во всех текущих вызовах массив однороден, порядок не влияет на значения. */
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

	/** PORT-TODO(item-tooltip, addInformation→appendHoverText): {@code Item.appendHoverText} сигнатура полностью
	 * иная в 26.1.2 (Consumer&lt;Component&gt;+TooltipContext/TooltipDisplay/TooltipFlag вместо List+Player+boolean),
	 * тот же класс проблемы во всех Item-наследниках gregtech.items (ItemBase/MultiItem, вне зоны этого захода).
	 * Текст-строящая логика сохранена 1:1 на будущее подключение, метод сейчас не вызывается движком. */
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

	/**
	 * PORT-TODO(F13, item-base компонентный редизайн): 1.7.10 {@code BehaviorProjectileDispense} было абстрактным,
	 * переопределяемым через {@code getProjectileEntity} (тут всегда null => фактически обычный дефолтный
	 * dispense). Neo {@code ProjectileDispenseBehavior} — конкретный класс, требующий реального
	 * {@code ProjectileItem} в конструкторе (neo-decompiled/.../ProjectileDispenseBehavior.java:16) — этот Item
	 * им не является, 1:1-подкласс недостижим. Класс никогда не регистрируется в конструкторе (тот же факт и
	 * в оригинале gregtech6/, dead code) — сведён к {@code DefaultDispenseItemBehavior}, что совпадает с
	 * фактическим прежним поведением (null-projectile == обычный dispense).
	 */
	public static class GT_Item_Dispense extends DefaultDispenseItemBehavior {
		@Override
		protected ItemStack execute(BlockSource aSource, ItemStack aStack) {
			return ((ItemArmorBase)aStack.getItem()).onDispense(aSource, aStack);
		}
	}

	// PORT-TODO(F13, item-base компонентный редизайн): IMetalArmor/IArmorApiarist — F10-зеркала (compat-mirror/README.md)
	// сейчас ПУСТЫЕ маркер-интерфейсы ("члены добираются компилятором") — эти методы временно НЕ @Override
	// (нечего переопределять), тела 1:1 сохранены для реальной IC2/Forestry-интеграции позже.
	public boolean isMetalArmor(ItemStack aStack, Player aPlayer) {return mMetalArmor;}
	public boolean protectEntity(LivingEntity aPlayer, ItemStack aArmor, String aCause, boolean doProtect) {return mBeeArmor;}
	public boolean protectPlayer(Player aPlayer, ItemStack aArmor, String aCause, boolean doProtect) {return mBeeArmor;}
	public String toString() {return mName;}
	/** PORT-TODO(item-tooltip, addInformation→appendHoverText): было {@code getUnlocalizedName()} (реальный override 1.7.10 Item); {@code Item.getDescriptionId()} в 26.1.2 — final, не переопределяем. Оставлен как обычный доменный метод, используется этим же классом. */
	public final String getUnlocalizedName() {return mName;}
	public String getUnlocalizedName(ItemStack aStack) {return mName;}
	public boolean isItemStackUsable(ItemStack aStack) {return T;}
	public ItemStack make(long aMetaData) {return ST.make(this, 1, aMetaData);}
	public ItemStack make(long aAmount, long aMetaData) {return ST.make(this, aAmount, aMetaData);}

	@Override public void updateItemStack(ItemStack aStack) {isItemStackUsable(aStack);}
	@Override public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {updateItemStack(aStack);}

	/** PORT-TODO(item-tooltip, addInformation→appendHoverText): было {@code onCreated(ItemStack,World,EntityPlayer)}; neo {@code Item.onCraftedBy(ItemStack,Player)} (Item.java:310) не имеет параметра Level — сигнатура сузилась 1:1 по доступным данным. */
	@Override public void onCraftedBy(ItemStack aStack, Player aPlayer) {isItemStackUsable(aStack);}
}
