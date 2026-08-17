/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregapi.item.multiitem;

import net.minecraftforge.api.distmarker.Dist;
import enviromine.handlers.EM_StatusManager;
import enviromine.trackers.EnviroDataTracker;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.IItemContainer;
import gregapi.code.ItemNBT;
import gregapi.code.ItemStackSet;
import gregapi.code.TagData;
import gregapi.cover.CoverRegistry;
import gregapi.cover.ICover;
import gregapi.data.*;
import gregapi.data.TC.TC_AspectStack;
import gregapi.item.IItemEnergy;
import gregapi.item.multiitem.behaviors.IBehavior;
import gregapi.item.multiitem.energy.EnergyStatDebug;
import gregapi.item.multiitem.food.IFoodStat;
import gregapi.item.prefixitem.PrefixItem;
import gregapi.old.Textures;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import gt6mirror.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * For Custom Items.
 */
public abstract class MultiItemRandom extends MultiItem implements Runnable {
	public final BitSet mEnabledItems = new BitSet(32767);
	public final BitSet mVisibleItems = new BitSet(32767);
	public final ResourceLocation[][] mIconList = new ResourceLocation[32767][1];
	
	public final HashMap<Short, IFoodStat> mFoodStats = new HashMap<>();
	public final HashMap<Short, IItemEnergy> mElectricStats = new HashMap<>();
	public final HashMap<Short, Short> mBurnValues = new HashMap<>();
	
	/**
	 * Creates the Item using these Parameters.
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public MultiItemRandom(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
		// Execute after all the other things. This is to ensure that MultiItems are created after PrefixItems.
		GAPI.mBeforeInit.add(this);
	}
	
	/**
	 * Add your Items here, and not within the Constructor.
	 * This gets called after all the PrefixItems and PrefixBlocks have been registered to the OreDict, what is during the @Init-Phase of the regular API.
	 */
	public abstract void addItems();
	
	private boolean mAllowedToAddItems = F;
	
	// F12-followup (oredict-timing): addItems() делает ST.make/OM.reg (Holder.components привязаны только на server-start).
	// run() вызывается на @Init (mBeforeInit) → отложено в runDeferredItemInit (тот же приём, что PrefixItem.run).
	@Override
	public final void run() {gregapi.GT_API.deferItemInit(() -> {mAllowedToAddItems = T; addItems();});}
	
	@Override
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {
		useEnergy(TD.Energy.EU, aStack, 0, aPlayer, null, null, 0, 0, 0, T);
		isItemStackUsable(aStack);
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		// было setItemInUse(ItemStack,int) (1.7.10, явная длительность) -> neo LivingEntity.startUsingItem(InteractionHand)
		// (LivingEntity.java:3529) — длительность больше не параметр, берётся движком из Item.getUseDuration(ItemStack,LivingEntity)
		// (Item.java:328); 1.7.10 не знал рук (единственный слот) -> MAIN_HAND (тот же приём, что MultiItem.java:227).
		if (tStat != null && (UT.Entities.isCreative(aPlayer) || aPlayer.getFoodData().needsFood() || tStat.alwaysEdible(this, aStack, aPlayer))) aPlayer.startUsingItem(InteractionHand.MAIN_HAND);
		return super.onItemRightClick(aStack, aWorld, aPlayer);
	}
	
	protected short mLastID = W;
	public ItemStack last() {return last(1);}
	public ItemStack last(int aAmount) {return ST.make(this, aAmount, mLastID);}
	public ItemStack prev() {return prev(1);}
	public ItemStack prev(int aAmount) {return ST.make(this, aAmount, mLastID-1);}
	public ItemStack next() {return next(1);}
	public ItemStack next(int aAmount) {return ST.make(this, aAmount, mLastID+1);}
	
	/**
	 * This adds a Custom Item.
	 * @param aID The Id of the assigned Item [0 - 32766]
	 * @param aEnglish The Default Localised Name of the created Item
	 * @param aToolTip The Default ToolTip of the created Item, you can also insert null for having no ToolTip
	 * @param aRandomData The OreDict Names you want to give the Item. Also used for TC Aspects and some other things.
	 * @return An ItemStack containing the newly created Item.
	 */
	@SuppressWarnings("unchecked")
	public final ItemStack addItem(int aID, String aEnglish, String aToolTip, Object... aRandomData) {
		if (aToolTip == null) aToolTip = "";
		if (mAllowedToAddItems && aID >= 0 && aID < 32767 && aID != W) {
			mLastID = (short)aID;
			ItemStack aStack = ST.make(this, 1, aID);
			if (UT.Code.stringValid(aEnglish)) {
				mEnabledItems.set(aID);
				mVisibleItems.set(aID);
				LH.add(getUnlocalizedName(aStack), aEnglish);
				LH.add(getUnlocalizedName(aStack) + ".tooltip", aToolTip);
			}
			List<TC_AspectStack> tAspects = new ArrayListNoNulls<>();
			// Important Stuff to do first
			for (Object tRandomData : aRandomData) if (tRandomData instanceof TagData) {
				if (tRandomData == TD.Creative.HIDDEN           ) {mVisibleItems.set(aID, F); continue;}
				if (tRandomData == TD.Properties.AUTO_BLACKLIST ) {OM.blacklist_(aStack); continue;}
			}
			// now check for the rest
			for (Object tRandomData : aRandomData) if (tRandomData != null) {
				boolean tUseOreDict = T;
				if (tRandomData instanceof ItemStackSet) {
					((ItemStackSet<?>)tRandomData).add(aStack);
					continue;
				}
				if (tRandomData instanceof TagData) {
					continue;
				}
				if (tRandomData instanceof Number) {
					setBurnValue(aID, ((Number)tRandomData).intValue());
					continue;
				}
				if (tRandomData instanceof IFoodStat) {
					setFoodBehavior(aID, (IFoodStat)tRandomData);
					if (IL.IC2_Food_Can_Empty.exists() && IL.IC2_Food_Can_Filled.exists() && getContainerItem(aStack) == null) {
						int tFoodValue = ((IFoodStat)tRandomData).getFoodLevel(this, aStack, null);
						if (tFoodValue > 0) RM.Canner.addRecipe2(T, 16, tFoodValue * 16, aStack, IL.IC2_Food_Can_Empty.get(tFoodValue), ((IFoodStat)tRandomData).isRotten(this, aStack, null)?IL.IC2_Food_Can_Spoiled.get(tFoodValue, IL.IC2_Food_Can_Filled.get(tFoodValue)):IL.IC2_Food_Can_Filled.get(tFoodValue));
					}
					tUseOreDict = F;
				}
				if (tRandomData instanceof ICover) {
					CoverRegistry.put(aStack, (ICover)tRandomData);
					tUseOreDict = F;
				}
				if (tRandomData instanceof IBehavior) {
					addItemBehavior(aID, (IBehavior<MultiItem>)tRandomData);
					tUseOreDict = F;
				}
				if (tRandomData instanceof IItemEnergy) {
					setElectricStats(aID, (IItemEnergy)tRandomData);
					tUseOreDict = F;
				}
				if (tRandomData instanceof IItemContainer) {
					((IItemContainer)tRandomData).set(aStack);
					tUseOreDict = F;
				}
				if (tRandomData instanceof TC_AspectStack) {
					((TC_AspectStack)tRandomData).addToAspectList(tAspects);
					continue;
				}
				if (tRandomData instanceof OreDictItemData) {
					if (((OreDictItemData)tRandomData).validData()) {
						OM.reg(aStack, tRandomData);
						ItemStack tStack = ((OreDictItemData)tRandomData).getStack(1);
						// Priority over autogenerated PrefixItems, but not over the hardcoded Compatibility Targets.
						if (ST.invalid(tStack) || tStack.getItem() instanceof PrefixItem) {
							OreDictManager.INSTANCE.setTarget_(((OreDictItemData)tRandomData).mPrefix, ((OreDictItemData)tRandomData).mMaterial.mMaterial, aStack);
							continue;
						}
					}
					OreDictManager.INSTANCE.addItemData_(aStack, (OreDictItemData)tRandomData);
					continue;
				}
				if (tRandomData instanceof FluidStack) {
					tRandomData = new FluidContainerData((FluidStack)tRandomData, ST.copy_(aStack), getContainerItem(aStack), T);
				//  if (((FluidContainerData)tRandomData).emptyContainer != null)
				//  RM.Canner.addRecipe1(T, 16, Math.max(((FluidContainerData)tRandomData).fluid.amount / 64, 16), ((FluidContainerData)tRandomData).emptyContainer, ((FluidContainerData)tRandomData).fluid, NF, ((FluidContainerData)tRandomData).filledContainer);
				//  RM.Canner.addRecipe1(T, 16, Math.max(((FluidContainerData)tRandomData).fluid.amount / 64, 16), ((FluidContainerData)tRandomData).filledContainer, NF, ((FluidContainerData)tRandomData).fluid, ST.container(((FluidContainerData)tRandomData).filledContainer, F));
					FL.reg((FluidContainerData)tRandomData, T, F);
					continue;
				}
				if (tRandomData instanceof FluidContainerData) {
				//  if (((FluidContainerData)tRandomData).emptyContainer != null)
				//  RM.Canner.addRecipe1(T, 16, Math.max(((FluidContainerData)tRandomData).fluid.amount / 64, 16), ((FluidContainerData)tRandomData).emptyContainer, ((FluidContainerData)tRandomData).fluid, NF, ((FluidContainerData)tRandomData).filledContainer);
				//  RM.Canner.addRecipe1(T, 16, Math.max(((FluidContainerData)tRandomData).fluid.amount / 64, 16), ((FluidContainerData)tRandomData).filledContainer, NF, ((FluidContainerData)tRandomData).fluid, ST.container(((FluidContainerData)tRandomData).filledContainer, F));
					FL.reg((FluidContainerData)tRandomData, T, F);
					continue;
				}
				if (tRandomData instanceof Runnable) {
					GAPI.mAfterPostInit.add((Runnable)tRandomData);
					tUseOreDict = F;
				}
				if (tUseOreDict) {
					OM.reg(tRandomData, aStack);
					continue;
				}
			}
			if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(aStack, tAspects, F);
			
			return ST.update(ST.make(this, 1, aID));
		}
		return null;
	}
	
	/**
	 * Sets a Food Behavior for the Item.
	 * 
	 * @param aMetaValue the Meta Value of the Item you want to set it to. [0 - 32766]
	 * @param aFoodBehavior the Food Behavior you want to add.
	 * @return the Item itself for convenience in constructing.
	 */
	public MultiItemRandom setFoodBehavior(int aMetaValue, IFoodStat aFoodBehavior) {
		if (aMetaValue < 0 || aMetaValue >= W) return this;
		if (aFoodBehavior == null) mFoodStats.remove((short)aMetaValue); else mFoodStats.put((short)aMetaValue, aFoodBehavior);
		return this;
	}
	
	/**
	 * Sets the Furnace Burn Value for the Item.
	 * 
	 * @param aMetaValue the Meta Value of the Item you want to set it to. [0 - 32766]
	 * @param aValue 200 = 1 Burn Process = 500 EU, max = 32767 (that is 81917.5 EU)
	 * @return the Item itself for convenience in constructing.
	 */
	public MultiItemRandom setBurnValue(int aMetaValue, int aValue) {
		if (aMetaValue < 0 || aMetaValue >= W || aValue < 0) return this;
		if (aValue == 0) mBurnValues.remove((short)aMetaValue); else mBurnValues.put((short)aMetaValue, aValue>Short.MAX_VALUE?Short.MAX_VALUE:(short)aValue);
		return this;
	}
	
	/**
	 * @param aMetaValue the Meta Value of the Item you want to set it to. [0 - 32766]
	 * @return the Item itself for convenience in constructing.
	 */
	public MultiItemRandom setElectricStats(int aMetaValue, IItemEnergy aStats) {
		if (aMetaValue < 0 || aMetaValue >= W) return this;
		if (aStats == null) mElectricStats.remove((short)aMetaValue); else {
			mElectricStats.put((short)aMetaValue, aStats);
			if (!(aStats instanceof EnergyStatDebug)) mIconList[aMetaValue] = Arrays.copyOf(mIconList[aMetaValue], Math.max(9, mIconList[aMetaValue].length));
		}
		return this;
	}
	
	/**
	 * @param aMetaValue the Meta Value of the Item you want to set it to. [0 - 32766]
	 * @return the Item itself for convenience in constructing.
	 */
	public MultiItemRandom setFluidContainerStats(int aMetaValue, long aCapacity, long aStacksize) {
		if (aMetaValue < 0 || aMetaValue >= W) return this;
		if (aCapacity < 0) mFluidContainerStats.remove((short)aMetaValue); else mFluidContainerStats.put((short)aMetaValue, new Long[] {aCapacity, Math.max(1, aStacksize)});
		return this;
	}
	
	// BUG-019: три 1.7.10-хука ниже (getMaxItemUseDuration/getItemUseAction/onEaten) остались мёртвыми именами —
	// движок их не звал, у предметов нет CONSUMABLE-компонента → getUseDuration дефолт 0 → жевание не стартовало,
	// finishUsingItem не наступал, вся еда иерархии была несъедобной. Мосты на современные каналы (референс
	// Item.java:232/317/328); GT6-методы сохранены как тела 1:1 (потребление стека/тара — внутри FoodStat.onEaten:149-153).
	@Override public int getUseDuration(ItemStack aStack) {return getMaxItemUseDuration(aStack);}
	@Override public UseAnim getUseAnimation(ItemStack aStack) {return getItemUseAction(aStack);}
	@Override public ItemStack finishUsingItem(ItemStack aStack, Level aWorld, net.minecraft.world.entity.LivingEntity aEntity) {
		return aEntity instanceof Player tPlayer ? onEaten(aStack, aWorld, tPlayer) : super.finishUsingItem(aStack, aWorld, aEntity);
	}
	// @Override
	public int getMaxItemUseDuration(ItemStack aStack) {
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		return tStat == null ? 0 : Math.max(tStat.getFoodLevel(this, aStack, null) * 8, 16);
	}
	
	// @Override
	public UseAnim getItemUseAction(ItemStack aStack) {
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		return tStat == null ? UseAnim.NONE : tStat.getFoodAction(this, aStack); // было UseAnim.none (1.7.10 enum-конвенция) -> UPPER_CASE (UseAnim.java:15)
	}
	
	// @Override
	public ItemStack onEaten(ItemStack aStack, Level aWorld, Player aPlayer) {
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		if (tStat != null) {
			
			int tFoodLevel = tStat.getFoodLevel(this, aStack, aPlayer);
			float tSaturationLevel = tStat.getSaturation(this, aStack, aPlayer);
			
			if (tFoodLevel * tSaturationLevel > 0) {
				if (tStat.useAppleCoreFunctionality(this, aStack, aPlayer) && MD.APC.mLoaded) {
					// F10 foreign-gated impossible-1:1 (AppleCore ItemFoodProxy addStats): 1.7.10 FoodStats.addStats(ItemFood,ItemStack)
					// (SRG func_151686_a) давал AppleCore подменить итоговое питание через полиморфный ItemFood-хук
					// (ItemFoodProxy). В neo FoodData (FoodData.java) такого метода нет вовсе — компонентная
					// FoodProperties-модель без per-item override hook; 1:1 недостижимо архитектурно (не только
					// переименование). Ветка недостижима в рантайме (MD.APC.mLoaded==F, AppleCore не портирован,
					// decisions/F10-external-mod-compat.md) — сохраняем попытку сконструировать AppleCore-мост
					// (compat-mirror squeek.applecore.api.food.ItemFoodProxy) и питаем тем же результатом, что и
					// without-AppleCore ветка ниже.
					UT.Reflection.callConstructor("squeek.applecore.api.food.ItemFoodProxy", 0, null, T, this);
					aPlayer.getFoodData().eat(tFoodLevel, tSaturationLevel);
				} else {
					aPlayer.getFoodData().eat(tFoodLevel, tSaturationLevel);
				}
			}
			
			if (!aWorld.isClientSide() && MD.ENVM.mLoaded) {
				try {
					float tTemperature = tStat.getTemperature(this, aStack, aPlayer) - C, tHydration = tStat.getHydration(this, aStack, aPlayer);
					Object tTracker = EM_StatusManager.lookupTracker(aPlayer);
					if (tTracker != null && ((EnviroDataTracker)tTracker).bodyTemp >= 0) {
						((EnviroDataTracker)tTracker).bodyTemp += (tTemperature - ((EnviroDataTracker)tTracker).bodyTemp) * tStat.getTemperatureEffect(this, aStack, aPlayer);
						if (tHydration > 0) ((EnviroDataTracker)tTracker).hydrate(tHydration); else if (tHydration < 0) ((EnviroDataTracker)tTracker).dehydrate(-tHydration);
					}
				} catch(Throwable e) {
					e.printStackTrace(ERR);
				}
			}
			tStat.onEaten(this, aStack, aPlayer, T, T);
		}
		return aStack;
	}
	
	@Override
	public IItemEnergy getEnergyStats(ItemStack aStack) {
		return mElectricStats.get(ST.meta_(aStack));
	}
	
	@Override
	public Long[] getFluidContainerStats(ItemStack aStack) {
		return mFluidContainerStats.get(ST.meta_(aStack));
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void getSubItems(Item aItem, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {
		if (aItem == this) for (int i = 0, j = mEnabledItems.length(); i < j; i++) if (mVisibleItems.get(i) || (SHOW_HIDDEN_ITEMS && mEnabledItems.get(i))) {
			IItemEnergy tStats = mElectricStats.get((short)i);
			if (tStats == null || tStats instanceof EnergyStatDebug) {
				ItemStack tStack = ST.make(this, 1, i);
				isItemStackUsable(tStack);
				aList.add(tStack);
			} else {
				ItemStack
				tStack = ST.make(this, 1, i);
				isItemStackUsable(tStack);
				aList.add(tStack);
				tStack = ST.make(this, 1, i);
				for (TagData tEnergyType : tStats.getEnergyTypes(tStack)) tStats.setEnergyStored(tEnergyType, tStack, tStats.getEnergyCapacity(tEnergyType, tStack));
				isItemStackUsable(tStack);
				aList.add(tStack);
			}
		}
		if (aList.isEmpty()) ST.hide(this);
	}
	
	@Override
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было aIconRegister.registerIcon(...) (IIconRegister удалён) — ResourceLocation строим напрямую из того же пути.
	public void registerIcons(Object aIconRegister) {
		for (short aMeta = 0, tMaxMeta = (short)mEnabledItems.length(); aMeta < tMaxMeta; aMeta++) if (mEnabledItems.get(aMeta)) {
			for (byte k = 1; k < mIconList[aMeta].length; k++) {
				mIconList[aMeta][k] = new ResourceLocation(mModID + ":" + getUnlocalizedName() + "/" + aMeta + "/" + k);
			}
			mIconList[aMeta][0] = new ResourceLocation(mModID + ":" + getUnlocalizedName() + "/" + aMeta);
		}
	}

	// F3-render (ленивый триггер, тот же приём, что ItemBase.getIconFromDamage): registerIcons(IIconRegister) в neo НЕ
	// вызывается (Forge texture-stitch хук удалён) → mIconList оставался null → getIconIndex/getIconFromDamage возвращали
	// null → предмет не рисовался (пусто). Наполняем mIconList ЛЕНИВО при первом запросе иконки тем же registerIcons-перебором.
	// protected: подклассы (MultiItemBumbles), переопределяющие getIconFromDamage со своим чтением mIconList, обязаны
	// тоже лениво триггерить наполнение (иначе минуют родительский триггер) — тот же централизованный приём.
	protected boolean mIconsRegistered = F;
	protected void ensureIconsRegistered() {if (!mIconsRegistered) {mIconsRegistered = T; registerIcons(null);}}

	// @Override
	public ResourceLocation getIconIndex(ItemStack aStack) {
		ensureIconsRegistered();
		short aMetaData = ST.meta_(aStack);
		if (!UT.Code.exists(aMetaData, mIconList)) return Textures.ItemIcons.RENDERING_ERROR.getIcon(0);
		IItemEnergy tStats = mElectricStats.get(aMetaData);
		if (tStats != null && mIconList[aMetaData].length > 1) {
			TagData tEnergyType = tStats.getEnergyTypes(aStack).iterator().next();
			long tStored = tStats.getEnergyStored(tEnergyType, aStack), tCapacity = tStats.getEnergyCapacity(tEnergyType, aStack);
			if (tStored <= 0) return mIconList[aMetaData][1];
			if (tStored >= tCapacity) return mIconList[aMetaData][8];
			return mIconList[aMetaData][7-(int)Math.max(0, Math.min(5, ((tCapacity-tStored)*6L) / tCapacity))];
		}
		return mIconList[aMetaData][0];
	}
	
	// @Override
	public ResourceLocation getIcon(ItemStack aStack, int aRenderPass) {
		return getIconIndex(aStack);
	}

	// @Override
	public ResourceLocation getIcon(ItemStack aStack, int aRenderPass, Player aPlayer, ItemStack aUsedStack, int aUseRemaining) {
		return getIcon(aStack, aRenderPass);
	}

	@Override
	public ResourceLocation getIconFromDamage(int aMetaData) {
		ensureIconsRegistered();
		return UT.Code.exists(aMetaData, mIconList) ? mIconList[aMetaData][0] : Textures.ItemIcons.RENDERING_ERROR.getIcon(0);
	}

	// КАНАЛ ИЗБЫТОЧЕН — тело ИДЕНТИЧНО getIconFromDamage:424 (и в оригинале 1.7.10 :396-403 оба отдают
	// mIconList[meta][0] — пассы у MultiItemRandom иконку не меняли). Роль закрыта живым путём: центр
	// GT6ItemModel.iconForPass:227 зовёт getIcon(ItemStack,int):414 рефлексией. Прежняя метка «разобран»
	// была ложной (2026-07-30).
	// @Override
	public ResourceLocation getIconFromDamageForRenderPass(int aMetaData, int aRenderPass) {
		ensureIconsRegistered();
		return UT.Code.exists(aMetaData, mIconList) ? mIconList[aMetaData][0] : Textures.ItemIcons.RENDERING_ERROR.getIcon(0);
	}
	
	@Override
	public void addAdditionalToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		if (tStat != null) tStat.addAdditionalToolTips(this, aList, aStack, aF3_H);
	}
	
	public boolean canBeStoredInToolbox(ItemStack aStack) {
		return mElectricStats.get(ST.meta(aStack)) != null;
	}
	
	public boolean isPlanStorage(ItemStack aStack) {
		return OM.is(OD_USB_STICKS[2], aStack);
	}
	
	public boolean setSetup(ItemStack aStack, String aSetup) {
		if (OM.is(OD_USB_STICKS[2], aStack)) {
			// F8: тег захвачен ОДИН раз (ItemNBT.get копирует), обе мутации идут в один и тот же
			// объект, коммит явный ниже — иначе setTag/setByte-правки потерялись бы (см. ItemNBT.java).
			CompoundTag tNBT = ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make();
			tNBT.put(NBT_USB_DATA, UT.NBT.makeString(UT.NBT.makeString(NBT_REACTOR_SETUP_NAME, ""+aSetup.hashCode()), NBT_REACTOR_SETUP, aSetup));
			tNBT.putByte(NBT_USB_TIER, (byte)2);
			ItemNBT.set(aStack, tNBT);
			return T;
		}
		return F;
	}

	public void setPlanName(ItemStack aStack, String aName) {
		// F8: тег захвачен ОДИН раз, вложенная мутация коммитится явным ItemNBT.set (см. ItemNBT.java).
		CompoundTag tNBT = ItemNBT.get(aStack);
		tNBT.getCompound(NBT_USB_DATA).putString(NBT_REACTOR_SETUP_NAME, aName);
		ItemNBT.set(aStack, tNBT);
	}

	public boolean hasSetup(ItemStack aStack) {
		return OM.is(OD_USB_STICKS[2], aStack) && ItemNBT.has(aStack) && ItemNBT.get(aStack).getCompound(NBT_USB_DATA).contains(NBT_REACTOR_SETUP);
	}

	public String getSetup(ItemStack aStack) {
		return ItemNBT.get(aStack).getCompound(NBT_USB_DATA).getString(NBT_REACTOR_SETUP);
	}
}
