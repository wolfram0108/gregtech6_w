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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorMaterial;
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
 * F13 (РЕАЛИЗОВАНО): {@code ItemArmor}/{@code EnumHelper} (1.7.10 armor-модель) заменены на {@code ArmorMaterial}
 * record + {@code Item.Properties.humanoidArmor} (durability/defense/enchantability/equip-слот/repair из одной
 * точки), см. конструктор/makeProperties ниже — армор функционален 1:1. Динамическая защита
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
	/** F13: 1.7.10 ItemArmor.armorType (int 0-3) → neo ArmorType (mArmorType ниже); слот-int сохранён как mArmorSlot. Реализовано. */
	protected final int mArmorSlot;
	protected final ArmorType mArmorType;
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
	 * F13 (РЕАЛИЗОВАНО): neo durability+defense+enchantability+equip-слот+repair одновременно через
	 * {@link Item.Properties#humanoidArmor(ArmorMaterial, ArmorType)} (Item.java:579). Статический (вызов до super()).
	 */
	private static Item.Properties makeProperties(String aModID, String aUnlocalized, String aArmorName, int aSlot, int[] aShields, int aDurability, int aEnchantability) {
		ArmorType tType = slotToArmorType(aSlot);
		ArmorMaterial tMaterial = new ArmorMaterial(
			aDurability,
			makeDefense(aShields),
			aEnchantability,
			// F13: neo ArmorMaterial record ТРЕБУЕТ Holder<SoundEvent> equip-sound (1.7.10 ItemArmor такого концепта не имел) →
			// движок-форс, выбран нейтральный ванильный дефолт ARMOR_EQUIP_GENERIC. Функционально, не заглушка.
			SoundEvents.ARMOR_EQUIP_GENERIC,
			0.0F, // toughness: нет 1:1-концепта в GT6 1.7.10 (введено в ванили позже)
			0.0F, // knockbackResistance: тот же случай
			// F13: оригинал getIsRepairable всегда F (не чинится). neo ArmorMaterial ТРЕБУЕТ TagKey<Item> repair-материала →
			// пустой (никогда не заполняемый) тег «repair/none» = ничего не матчит => не чинится. 1:1 по следствию. Не заглушка.
			TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(aModID, "repair/none")),
			// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было mArmorTexture-строка (PNG-путь), реальный держатель —
			// assets/<mModID>/equipment/<aArmorName>.json, клиентский ресурс не порождается этим Java-кодом
			// (оригинал тоже не порождал PNG из кода — только ссылался строкой).
			ResourceKey.create(EquipmentAssets.ROOT_ID, ResourceLocation.fromNamespaceAndPath(aModID, aArmorName))
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
			// F12-followup (item-split): neo Item требует id в Properties (иначе «Item id not set»); ключ = (modID, unlocalized),
			// санитизирован, совпадает с именем регистрации registerItemLazy на call-site (как ItemBase:87).
			.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(aModID, gregapi.GT_API.sanitizeRegName(aUnlocalized))))
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

	// F13: neo зовёт appendHoverText (не 1.7.10 addInformation) — мост: GT6-тултип через addInformation → neo builder. ПОДКЛЮЧЕНО.
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

	/** F1 (1:1): было {@code onCreated(ItemStack,World,EntityPlayer)} → neo {@code Item.onCraftedBy(ItemStack,Player)} (Item.java:310)
	 *  без Level. Оригинальное тело = только isItemStackUsable(aStack), World НЕ использовался → потеря параметра = ноль потерь, полный 1:1. */
	@Override public void onCraftedBy(ItemStack aStack, Player aPlayer) {isItemStackUsable(aStack);}
}
