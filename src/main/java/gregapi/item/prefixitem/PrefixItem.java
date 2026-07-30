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

package gregapi.item.prefixitem;

import net.neoforged.api.distmarker.Dist;
import gregapi.GT_API;
import gregapi.code.ModData;
import gregapi.data.*;
import gregapi.item.*;
import gregapi.lang.LanguageHandler;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class PrefixItem extends Item implements Runnable, IItemUpdatable, IPrefixItem, IItemGT, IItemNoGTOverride {
	public final String mNameInternal;
	public final OreDictPrefix mPrefix;
	public final OreDictMaterial[] mMaterialList;
	
	public ItemStack mContainerItem = null;
	
	/** The Sound played when crafting with this Item */
	public String mCraftingSound = null;
	
	public PrefixItem(ModData aMod, String aNameInternal, OreDictPrefix aPrefix) {
		this(aMod.mID, aMod.mID, aNameInternal, aPrefix, OreDictMaterial.MATERIAL_ARRAY);
	}
	
	/**
	 * @param aModIDOwner the ID of the owning Mod. DO NOT INSERT ANY GREGTECH MODID!!!
	 * @param aModIDTextures the ID of the Texture providing Mod (for the "ModID:TextureName" thing)
	 * @param aNameInternal the internal Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 * @param aPrefix the OreDictPrefix corresponding to this Item.
	 */
	public PrefixItem(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, OreDictMaterial... aMaterialList) {
		// F12-followup (item-split): setId в Properties (иначе «Item id not set»); ключ = (владелец, имя), санитизирован,
		// совпадает с registerItemLazy на call-site. (было super() → super(new Item.Properties()) без id.)
		super(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(aModIDOwner, gregapi.GT_API.sanitizeRegName(aNameInternal)))));
		mPrefix = aPrefix;
		mPrefix.mRegisteredPrefixItems.add(this);
		mNameInternal = aNameInternal;
		mMaterialList = (aMaterialList.length > 0 ? aMaterialList : OreDictMaterial.MATERIAL_ARRAY);
		if (mMaterialList[0] != MT.Empty) throw new IllegalArgumentException("The first element of the custom Material List has to be MT.Empty for technical reasons!");
		
		setMaxDamage(0);
		setHasSubtypes(T);
		// F12-followup (item-split): само-рег УБРАНА — конструкция PrefixItem идёт на RegisterEvent через call-site
		// GT_API.registerItemLazy(modId, name, ()->new PrefixItem(...)) (Item.<init> createIntrusiveHolder требует разморож.
		// реестр). mPrefix.mRegisteredItems.add(this) ниже остаётся в конструкторе — выполнится на RegisterEvent (паритет).

		mPrefix.addTextureSet(aModIDTextures, T);
		LH.add("oredict." + mPrefix.dat(MT.Empty).toString(), getLocalName(mPrefix, MT.Empty));
		LH.add(mNameInternal+"."+W, "Any Sub-Item of this one"); // Local Name for the WildcardItem Variant.
		// F12-followup (item-split, hashCode-стабильность): ItemStackContainer.hashCode = id_(Item) = Item.getId(), а до
		// РЕГИСТРАЦИИ предмета (ctor идёт внутри DeferredRegister-supplier на RegisterEvent) getId=-1 → запись легла бы в
		// «мёртвый» бакет, и дедуп в onOreRegistration её не находит (даёт лишний предмет). Откладываем add на server-start,
		// где id_ стабилен (совпадает с id_ в дедуп-контейнере) → wildcard-дедуп {this,W} схлопывает все суб-предметы в 1.
		gregapi.GT_API.deferItemInit(() -> mPrefix.mRegisteredItems.add(this)); // this optimizes some processes by decreasing the size of the Set.
		
		if (SHOW_HIDDEN_PREFIXES || !mPrefix.contains(TD.Creative.HIDDEN)) {
			// F16 (1:1 golden): видимый prefix → СВОЯ prefix-вкладка (setCreativeTab(mPrefix.mCreativeTab)), НЕ ванильный misc.
			if (mPrefix.mCreativeTab == null) mPrefix.mCreativeTab = new CreativeTab(mPrefix.mNameInternal, mPrefix.mNameCategory, this, W);
			gregapi.item.CreativeTabsGT.joinOwnTab(this, mPrefix.mCreativeTab);
		} else {
			gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.MISC); // hidden prefix → tabMisc (golden else)
		}
		
		// Execute before all the other things. This is to ensure that PrefixItems are created before MultiItems.
		GAPI.mBeforeInit.add(0, this);
	}
	
	/** This ensures, that all Materials are registered at the time this Item registers to the OreDictionary. */
	// F12-followup (item-split): тело делает ST.make/OreDict-регистрацию (Holder.components привязаны только на server-start) →
	// отложено в runDeferredItemInit. run() вызывается на @Init (mBeforeInit) → defer добавлен ДО postInit-дефферов, сохраняя
	// GT6-порядок «PrefixItems до MultiItems». Guard registerOre_ «Only @Init/@PreInit» подавлён в этом окне (см. GT_API).
	@Override
	public void run() {gregapi.GT_API.deferItemInit(this::runDeferred);}
	private void runDeferred() {
		boolean tUnificationAllowed = (mPrefix.contains(TD.Prefix.UNIFICATABLE) && !mPrefix.contains(TD.Prefix.UNIFICATABLE_RECIPES));
		for (short i = 0; i < mMaterialList.length; i++) if (mPrefix.isGeneratingItem(mMaterialList[i])) {
			ItemStack tStack = ST.update_(ST.make(this, 1, i));
			LH.add("oredict." + mPrefix.dat(mMaterialList[i]).toString(), getLocalName(mPrefix, mMaterialList[i]));
			if (tUnificationAllowed) OreDictManager.INSTANCE.addTarget_(mPrefix, mMaterialList[i], tStack); else OreDictManager.INSTANCE.registerOre_(mPrefix, mMaterialList[i], tStack);
		}
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void getSubItems(Item var1, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {
		if ((SHOW_HIDDEN_PREFIXES || !mPrefix.contains(TD.Creative.HIDDEN))) for (int i = 0; i < mMaterialList.length; i++) if (mPrefix.isGeneratingItem(mMaterialList[i])) if (SHOW_HIDDEN_MATERIALS || !mMaterialList[i].mHidden) {
			ItemStack tStack = OM.get_(ST.make(this, 1, i));
			if (tStack.getItem() == this) {
				updateItemStack(tStack);
				if (ST.meta_(tStack) == i) aList.add(tStack);
			}
		}
		if (aList.isEmpty()) ST.hide(this);
	}
	
	public int getSpriteNumber() {return 1;}
	public int getRenderPasses(int metadata) {return 2;}
	// F3-render: тип параметра был IIconRegister (net.minecraft.client.renderer.texture — УДАЛЁН в neo, только в
	// compat-mirror при компиляции, вырезан из рантайма). getMethod-рефлексия GT6ItemModel.resolveIcon перечисляет
	// методы класса → сигнатура с отсутствующим типом → NoClassDefFoundError → resolveIcon возвращал null → предмет
	// не рисовался (пусто/пурпур). Параметр → Object (метод всё равно no-op в neo), рефлексия больше не падает.
	public void registerIcons(Object aIconRegister) {/**/}
	public boolean requiresMultipleRenderPasses() {return mPrefix.mIconIndexItem >= 0;}
	public Identifier getIconIndex(ItemStack aStack) {return getIconFromDamageForRenderPass(ST.meta_(aStack), 0);}
	public Identifier getIconFromDamage(int aMetaData) {return getIconFromDamageForRenderPass(aMetaData, 0);}
	public Identifier getIcon(ItemStack aStack, int aRenderPass) {return getIconFromDamageForRenderPass(ST.meta_(aStack), aRenderPass);}
	public Identifier getIcon(ItemStack aStack, int aRenderPass, Player aPlayer, ItemStack aUsedStack, int aUseRemaining) {return getIconFromDamageForRenderPass(ST.meta_(aStack), aRenderPass);}

	// @Override
	public Identifier getIconFromDamageForRenderPass(int aMetaData, int aRenderPass) {
		if (mPrefix.mIconIndexItem >= 0) {
			if (UT.Code.exists(aMetaData, mMaterialList) && mMaterialList[aMetaData].mTextureSetsItems != null)
			return mMaterialList[aMetaData] .mTextureSetsItems.get(mPrefix.mIconIndexItem).getIcon(aRenderPass);
			return MT.NULL                  .mTextureSetsItems.get(mPrefix.mIconIndexItem).getIcon(aRenderPass);
		}
		return null;
	}
	
	// @Override
	public int getColorFromItemStack(ItemStack aStack, int aRenderPass) {
		if (aRenderPass == 0) {
			short aMetaData = ST.meta_(aStack);
			if (UT.Code.exists(aMetaData, mMaterialList)) return UT.Code.getRGBInt(mMaterialList[aMetaData].mRGBa[mPrefix.mState]);
		}
		return 16777215;
	}
	
	// @Override
	public final String getUnlocalizedName(ItemStack aStack) {
		short aMetaData = ST.meta_(aStack);
		if (aMetaData == W) return mNameInternal+"."+W;
		if (UT.Code.exists(aMetaData, mMaterialList)) return "oredict." + mPrefix.dat(mMaterialList[aMetaData]).toString();
		return mNameInternal;
	}
	
	// @Override
	public ItemStack getContainerItem(ItemStack aStack) {
		if (ST.equal(aStack, mContainerItem, T)) return null;
		if (mCraftingSound != null) UT.Sounds.play(mCraftingSound, 20, 1.0F);
		return mContainerItem != null ? ST.amount(1, mContainerItem) : mPrefix.containerItem() != null ? ST.amount(1, mContainerItem = mPrefix.containerItem()) : null;
	}
	
	// @Override
	public boolean isBeaconPayment(ItemStack aStack) {
		if (mPrefix.mAmount >= U && (mPrefix.contains(TD.Prefix.GEM_BASED) || mPrefix.contains(TD.Prefix.INGOT_BASED))) {
			short aMetaData = ST.meta_(aStack);
			return UT.Code.exists(aMetaData, mMaterialList) && (mMaterialList[aMetaData].contains(TD.Properties.VALUABLE) || ANY.Iron.mToThis.contains(mMaterialList[aMetaData]));
		}
		return F;
	}
	
	@Override public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {updateItemStack(aStack);}
	@Override public void updateItemStack(ItemStack aStack) {
		if (mMaterialList != OreDictMaterial.MATERIAL_ARRAY) return;
		int aMeta = ST.meta_(aStack);
		if (UT.Code.exists(aMeta, mMaterialList)) {
			OreDictMaterial aMaterial = mMaterialList[aMeta];
			if (aMeta != aMaterial.mTargetRegistration.mID) ST.meta_(aStack, aMaterial.mTargetRegistration.mID);
			if (!mPrefix.isGeneratingItem(aMaterial.mTargetRegistration)) ST.set(aStack, mPrefix.mat(aMaterial.mTargetRegistration, 1), F, F);
		}
	}
	
	// ⚠️ КАНАЛ РАЗОБРАН, мост невозможен на текущей архитектуре — часть УЖЕ зафиксированного отклонения
	// движка (см. GT6SmeltingDispatcher javadoc, строки 60-62). В 1.7.10 опыт печи был ФУНКЦИЕЙ предмета:
	// движок спрашивал getSmeltingExperience у результата плавки. В neo опыт — поле РЕЦЕПТА
	// (AbstractCookingRecipe.experience(), выдаётся в AbstractFurnaceBlockEntity:400 без контекста входа
	// и результата). Все плавки GT6 идут через ОДИН объект-диспетчер, поэтому per-результатный опыт
	// на нём невыразим: переопределение experience() вернуло бы одно значение на все плавки мода.
	// Следствие для игрока: плавка с самоцветом на выходе не даёт 1.0 опыта. Выразить это можно только
	// сменой архитектуры диспетчера (рецепт на каждую плавку вместо одного) — отдельное решение, не заплатка.
	// @Override
	public float getSmeltingExperience(ItemStack aStack) {
		return mPrefix == OP.gem ? 1.0F : 0.0F;
	}
	
	@Override public String toString() {return mNameInternal;}
	public final String getUnlocalizedName() {return mNameInternal;}
	public final Item setUnlocalizedName(String aName) {return this;}
	public String getItemStackDisplayName(ItemStack aStack) {return gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack));}
	// F1-контракт (1.7.10 itemDamage==meta): meta = mID материала; neo-дефолт getDamage читает DAMAGE-компонент (0). Как на прочих корнях.
	@Override public int getDamage(ItemStack aStack) {return getMaxDamage(aStack) > 0 ? super.getDamage(aStack) : ST.meta_(aStack);}
	// F13-мост appendHoverText → addInformation (как ItemBlockBase:65).
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, net.minecraft.world.item.Item.TooltipContext aCtx, net.minecraft.world.item.component.TooltipDisplay aDisplay, java.util.function.Consumer<net.minecraft.network.chat.Component> aBuilder, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aBuilder.accept(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}
	// LOCALIZATION-display: neo getName(ItemStack) → GT6-имя (LH.get); иначе raw-ключ из vanilla-lang.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	public final boolean hasContainerItem(ItemStack aStack) {return getContainerItem(aStack) != null;}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	public void onCreated(ItemStack aStack, Level aWorld, Player aPlayer) {updateItemStack(aStack);}
	public boolean isBookEnchantable(ItemStack aStack, ItemStack aBook) {return F;}
	public boolean getIsRepairable(ItemStack aStack, ItemStack aMaterial) {return F;}
	public int getItemEnchantability() {return 0;}
	// BUG-021 v2: мост neo per-stack канала (IItemExtension.getMaxStackSize) на 1.7.10-хук ниже — без него все
	// префиксные предметы игнорировали mDefaultStackSize (жемчуг/платы/яйца и т.п. стакались по ItemBase-дефолту 64).
	@Override public int getMaxStackSize(ItemStack aStack) {return UT.Code.bindStack(getItemStackLimit(aStack));}
	public int getItemStackLimit(ItemStack aStack) {return mPrefix.mDefaultStackSize;}
	@Override public OreDictItemData getOreDictItemData(ItemStack aStack) {return UT.Code.exists(ST.meta_(aStack), mMaterialList) ? new OreDictItemData(mPrefix, mMaterialList[ST.meta_(aStack)]) : null;}
	@Override public OreDictMaterial getMaterial(int aMetaData) {return UT.Code.exists(aMetaData, mMaterialList) ? mMaterialList[aMetaData] : null;}
	@Override public OreDictPrefix getPrefix(int aMetaData) {return mPrefix;}
	@SuppressWarnings("deprecation") public boolean hasEffect(ItemStack aStack) {return F;}
	public boolean hasEffect(ItemStack aStack, int aRenderPass) {return F;}
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {while (aList.remove(null));}
	
	/*
	@Override @Optional.Method(modid = ModIDs.TC) public void setAspects(ItemStack aStack, AspectList aAspectList) {}
	
	@Override @Optional.Method(modid = ModIDs.TC)
	public AspectList getAspects(ItemStack aStack) {
		List<TC_AspectStack> rAspects = new ArrayListNoNulls<TC_AspectStack>();
		for (TC_AspectStack tAspect : mPrefix.mAspects) tAspect.addToAspectList(rAspects);
		OreDictMaterial aMaterial = (UT.Code.exists(aStack), mMaterialList) ? mMaterialList[ST.meta(aStack)] : null);
		if (aMaterial != null && (mPrefix.mAmount >= U || mPrefix.mAmount < 0)) for (TC_AspectStack tAspect : aMaterial.mAspects) tAspect.addToAspectList(rAspects);
		
		for (TC_AspectStack tAspect : rAspects) tAspect.mAmount = Math.min(10, tAspect.mAmount);
		return (AspectList)GT_API.sCompatTC.getAspectList(rAspects);
	}
	*/
	
	/** @return the Local Name for this Item depending on Prefix and Material. */
	public String getLocalName(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		return LanguageHandler.getLocalName(aPrefix, aMaterial);
	}
}
