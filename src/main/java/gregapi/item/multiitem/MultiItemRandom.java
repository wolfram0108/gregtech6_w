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

import net.neoforged.api.distmarker.Dist;
import gt6mirror.enviromine.handlers.EM_StatusManager;
import gt6mirror.enviromine.trackers.EnviroDataTracker;
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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import gt6mirror.net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.neoforged.neoforge.fluids.FluidStack;

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
public abstract class MultiItemRandom extends MultiItem implements Runnable, gregapi.item.IMetaItem {
	private static final List<MultiItemRandom> FAMILIES = new ArrayListNoNulls<>();
	private final MultiItemRandom mRandomFamily;
	private final short mFixedMeta;
	private final HashMap<Short, MultiItemRandom> mFlattenedItems;
	public final BitSet mEnabledItems;
	public final BitSet mDeclaredItems;
	public final BitSet mVisibleItems;
	public final Identifier[][] mIconList;
	
	public final HashMap<Short, IFoodStat> mFoodStats;
	public final HashMap<Short, IItemEnergy> mElectricStats;
	public final HashMap<Short, Short> mBurnValues;
	
	/**
	 * Creates the Item using these Parameters.
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public MultiItemRandom(String aModID, String aUnlocalized) {
		this(aModID, aUnlocalized, null, (short)-1);
		// Execute after all the other things. This is to ensure that MultiItems are created after PrefixItems.
		GAPI.mBeforeInit.add(this);
		FAMILIES.add(this);
	}

	protected MultiItemRandom(MultiItemRandom aFamily, short aMeta) {
		this(aFamily.mModID, aFamily.getUnlocalizedName() + "-" + aMeta, aFamily, aMeta);
	}
	private MultiItemRandom(String aModID, String aName, MultiItemRandom aFamily, short aMeta) {
		super(aModID, aName, aFamily);
		mRandomFamily = aFamily == null ? this : aFamily.mRandomFamily;
		mFixedMeta = aMeta;
		mFlattenedItems = aFamily == null ? new HashMap<>() : aFamily.mFlattenedItems;
		mEnabledItems = aFamily == null ? new BitSet(32767) : aFamily.mEnabledItems;
		mDeclaredItems = aFamily == null ? new BitSet(32767) : aFamily.mDeclaredItems;
		mVisibleItems = aFamily == null ? new BitSet(32767) : aFamily.mVisibleItems;
		mIconList = aFamily == null ? new Identifier[32767][1] : aFamily.mIconList;
		mFoodStats = aFamily == null ? new HashMap<>() : aFamily.mFoodStats;
		mElectricStats = aFamily == null ? new HashMap<>() : aFamily.mElectricStats;
		mBurnValues = aFamily == null ? new HashMap<>() : aFamily.mBurnValues;
		if (aFamily != null) mFlattenedItems.put(aMeta, this);
	}
	@Override public short fixedMeta() {return mFixedMeta;}
	@Override public MultiItemRandom metaFamily() {return mRandomFamily;}
	@Override public MultiItemRandom itemForMeta(short aMeta) {return mFlattenedItems.getOrDefault(aMeta, mRandomFamily);}
	@Override public java.util.Collection<? extends Item> metaVariants() {return mFlattenedItems.values();}
	@Override public ItemStack make(long aMeta) {return ST.make(mRandomFamily, 1, aMeta);}
	@Override public ItemStack make(long aAmount, long aMeta) {return ST.make(mRandomFamily, aAmount, aMeta);}
	protected MultiItemRandom createVariant(short aMeta) {throw new UnsupportedOperationException(getClass().getName());}

	/** Только номера типов объявляются заранее; addItems сохраняет исходный порядок и побочные эффекты. */
	public static void registerVariants(net.neoforged.neoforge.registries.RegisterEvent aEvent) {
		if (!aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) return;
		try (var tStream = MultiItemRandom.class.getResourceAsStream("/gregapi/multiitem-types.json")) {
			var tCatalog = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(tStream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			for (MultiItemRandom tFamily : FAMILIES) {
				var tID = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tFamily);
				if (!tCatalog.has(tID.toString())) continue;
				for (String tRange : tCatalog.get(tID.toString()).getAsString().split(",")) {
					String[] tEnds = tRange.split("-");
					int tFirst = Integer.parseInt(tEnds[0]), tLast = Integer.parseInt(tEnds[tEnds.length - 1]);
					for (int i = tFirst; i <= tLast; i++) {
						short tMeta = (short)i;
						Identifier tName = Identifier.fromNamespaceAndPath(tID.getNamespace(), tID.getPath() + "-" + tMeta);
						aEvent.register(net.minecraft.core.registries.Registries.ITEM, tName, () -> tFamily.createVariant(tMeta));
					}
				}
			}
		} catch (java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
	}
	
	/**
	 * Add your Items here, and not within the Constructor.
	 * This gets called after all the PrefixItems and PrefixBlocks have been registered to the OreDict, what is during the @Init-Phase of the regular API.
	 */
	public abstract void addItems();
	
	private boolean mAllowedToAddItems = F;
	
	// addItems() calls ST.make/OM.reg, which need components bound only at server-start; run() itself fires on
	// @Init, so the work is deferred into runDeferredItemInit the same way PrefixItem.run defers its own init.
	@Override
	public final void run() {gregapi.GT_API.deferItemInit(() -> {mAllowedToAddItems = T; addItems();});}
	
	@Override
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {
		useEnergy(TD.Energy.EU, aStack, 0, aPlayer, null, null, 0, 0, 0, T);
		isItemStackUsable(aStack);
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		// Replaces the removed setItemInUse(ItemStack,int) with startUsingItem(InteractionHand); duration now comes
		// from Item.getUseDuration instead of being passed explicitly, and MAIN_HAND stands in for the single 1.7.10 hand.
		if (tStat != null && (UT.Entities.isCreative(aPlayer) || aPlayer.getFoodData().needsFood() || tStat.alwaysEdible(this, aStack, aPlayer))) aPlayer.startUsingItem(InteractionHand.MAIN_HAND);
		return super.onItemRightClick(aStack, aWorld, aPlayer);
	}
	
	protected short mLastID = W;
	public ItemStack last() {return last(1);}
	public ItemStack last(int aAmount) {return ST.make(metaFamily(), aAmount, mRandomFamily.mLastID);}
	public ItemStack prev() {return prev(1);}
	public ItemStack prev(int aAmount) {return ST.make(metaFamily(), aAmount, mRandomFamily.mLastID-1);}
	public ItemStack next() {return next(1);}
	public ItemStack next(int aAmount) {return ST.make(metaFamily(), aAmount, mRandomFamily.mLastID+1);}
	
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
			mDeclaredItems.set(aID);
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
					// Asked here, before registering: the recycling listeners make their Recipes from these data at once.
					OreDictItemData tData = gregapi.api.GT6Definitions.itemData(aStack, (OreDictItemData)tRandomData);
					if (tData.validData()) {
						OM.reg(aStack, tData);
						ItemStack tStack = tData.getStack(1);
						// Priority over autogenerated PrefixItems, but not over the hardcoded Compatibility Targets.
						if (ST.invalid(tStack) || tStack.getItem() instanceof PrefixItem) {
							OreDictManager.INSTANCE.setTarget_(tData.mPrefix, tData.mMaterial.mMaterial, aStack);
							continue;
						}
					}
					OreDictManager.INSTANCE.addItemData_(aStack, tData);
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
	
	// The three hooks below went dead without a CONSUMABLE component, so use duration defaulted to 0 and
	// nothing in this hierarchy was edible; they are now bridged onto the modern channels, bodies kept 1:1.
	@Override public int getUseDuration(ItemStack aStack, net.minecraft.world.entity.LivingEntity aEntity) {return getMaxItemUseDuration(aStack);}
	@Override public ItemUseAnimation getUseAnimation(ItemStack aStack) {return getItemUseAction(aStack);}
	@Override public ItemStack finishUsingItem(ItemStack aStack, Level aWorld, net.minecraft.world.entity.LivingEntity aEntity) {
		return aEntity instanceof Player tPlayer ? onEaten(aStack, aWorld, tPlayer) : super.finishUsingItem(aStack, aWorld, aEntity);
	}
	// @Override
	public int getMaxItemUseDuration(ItemStack aStack) {
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		return tStat == null ? 0 : Math.max(tStat.getFoodLevel(this, aStack, null) * 8, 16);
	}
	
	// @Override
	public ItemUseAnimation getItemUseAction(ItemStack aStack) {
		IFoodStat tStat = mFoodStats.get((short)getDamage(aStack));
		return tStat == null ? ItemUseAnimation.NONE : tStat.getFoodAction(this, aStack); // Replaces the 1.7.10 enum convention ItemUseAnimation.none with the modern UPPER_CASE constant.
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
					// AppleCore used to override the final food value via a polymorphic hook that neo's FoodData has no
					// equivalent of; unreachable anyway since AppleCore isn't ported, so this compat-mirror attempt is kept.
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
		if (aItem == this) for (int i = mFixedMeta >= 0 ? mFixedMeta : 0, j = mFixedMeta >= 0 ? mFixedMeta + 1 : mEnabledItems.length(); i < j; i++) if (mVisibleItems.get(i) || (SHOW_HIDDEN_ITEMS && mEnabledItems.get(i))) {
			IItemEnergy tStats = mElectricStats.get((short)i);
			if (tStats == null || tStats instanceof EnergyStatDebug) {
				ItemStack tStack = ST.make(metaFamily(), 1, i);
				isItemStackUsable(tStack);
				aList.add(tStack);
			} else {
				ItemStack
				tStack = ST.make(metaFamily(), 1, i);
				isItemStackUsable(tStack);
				aList.add(tStack);
				tStack = ST.make(metaFamily(), 1, i);
				for (TagData tEnergyType : tStats.getEnergyTypes(tStack)) tStats.setEnergyStored(tEnergyType, tStack, tStats.getEnergyCapacity(tEnergyType, tStack));
				isItemStackUsable(tStack);
				aList.add(tStack);
			}
		}
		if (aList.isEmpty()) ST.hide(this);
	}
	
	@Override
	// IIconRegister is gone in neo, so the Identifier is built directly from the same path instead.
	public void registerIcons(Object aIconRegister) {
		if (mFixedMeta >= 0) return;
		for (short aMeta = 0, tMaxMeta = (short)mEnabledItems.length(); aMeta < tMaxMeta; aMeta++) if (mEnabledItems.get(aMeta)) {
			for (byte k = 1; k < mIconList[aMeta].length; k++) {
				mIconList[aMeta][k] = Identifier.parse(mModID + ":" + getUnlocalizedName() + "/" + aMeta + "/" + k);
			}
			mIconList[aMeta][0] = Identifier.parse(mModID + ":" + getUnlocalizedName() + "/" + aMeta);
		}
	}

	// registerIcons(IIconRegister) is never called in neo, so the icon list stayed null and items failed to
	// render; it now fills lazily on first request, and subclasses reading it must trigger that fill themselves.
	protected boolean mIconsRegistered = F;
	protected void ensureIconsRegistered() {if (mRandomFamily != this) {mRandomFamily.ensureIconsRegistered(); return;} if (!mIconsRegistered) {mIconsRegistered = T; registerIcons(null);}}

	// @Override
	public Identifier getIconIndex(ItemStack aStack) {
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
	public Identifier getIcon(ItemStack aStack, int aRenderPass) {
		return getIconIndex(aStack);
	}

	// @Override
	public Identifier getIcon(ItemStack aStack, int aRenderPass, Player aPlayer, ItemStack aUsedStack, int aUseRemaining) {
		return getIcon(aStack, aRenderPass);
	}

	@Override
	public Identifier getIconFromDamage(int aMetaData) {
		ensureIconsRegistered();
		return UT.Code.exists(aMetaData, mIconList) ? mIconList[aMetaData][0] : Textures.ItemIcons.RENDERING_ERROR.getIcon(0);
	}

	// This channel is redundant with getIconFromDamage (render passes never changed the icon here, in the
	// original either); GT6ItemModel.iconForPass already reaches this method by reflection, so it is still live.
	// @Override
	public Identifier getIconFromDamageForRenderPass(int aMetaData, int aRenderPass) {
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
			// The tag is captured once, both mutations go into the same object, and the commit below is explicit, or
			// the setTag/setByte edits would be lost.
			CompoundTag tNBT = ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make();
			tNBT.put(NBT_USB_DATA, UT.NBT.makeString(UT.NBT.makeString(NBT_REACTOR_SETUP_NAME, ""+aSetup.hashCode()), NBT_REACTOR_SETUP, aSetup));
			tNBT.putByte(NBT_USB_TIER, (byte)2);
			ItemNBT.set(aStack, tNBT);
			return T;
		}
		return F;
	}

	public void setPlanName(ItemStack aStack, String aName) {
		// The tag is captured once; the nested mutation is committed back explicitly with ItemNBT.set.
		CompoundTag tNBT = ItemNBT.get(aStack);
		tNBT.getCompoundOrEmpty(NBT_USB_DATA).putString(NBT_REACTOR_SETUP_NAME, aName);
		ItemNBT.set(aStack, tNBT);
	}

	public boolean hasSetup(ItemStack aStack) {
		return OM.is(OD_USB_STICKS[2], aStack) && ItemNBT.has(aStack) && ItemNBT.get(aStack).getCompoundOrEmpty(NBT_USB_DATA).contains(NBT_REACTOR_SETUP);
	}

	public String getSetup(ItemStack aStack) {
		return ItemNBT.get(aStack).getCompoundOrEmpty(NBT_USB_DATA).getString(NBT_REACTOR_SETUP).orElse("");
	}
}
