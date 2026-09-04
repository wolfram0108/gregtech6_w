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

package gregapi.old;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import java.util.List;

import net.neoforged.api.distmarker.Dist;
import gregapi.code.ItemNBT;
import gregapi.data.LH;
import gregapi.util.UT;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * F13 (docs for the neo item/armor component model): was {@code extends ItemArmor} (1.7.10, armorType/renderIndex
 * ctor params + mutators setMaxStackSize/setMaxDamage/setNoRepair/setUnlocalizedName) — ItemArmor does not exist
 * in 26.1.2 (0 in the 3 reference roots), the same approach already adopted in {@code gregapi.item.ItemArmorBase} (see this
 * file): {@code Item.Properties.humanoidArmor(ArmorMaterial,ArmorType)} + {@code .durability}/{@code .repairable}
 * assembled BEFORE {@code super()} in {@link #makeProperties}, mName field instead of the mutable unlocalizedName.
 */
public class GT_EnergyArmor_Item extends Item /*implements ISpecialArmor*/ {
	public int mCharge, mTransfer, mTier, mDamageEnergyCost, mSpecials;
	public boolean mChargeProvider;
	public double mArmorAbsorbtionPercentage;
	protected final String mName;
	/** Was the inherited field {@code ItemArmor.armorType} (0=helmet,1=chest,2=legs,3=boots) — now its own field, same semantics. */
	protected final int mArmorType;
	/** Was {@code armorInventory[0..3]} (1.7.10 boots/leggings/chest/helmet) — {@code EquipmentSlot[]}, the same
	 *  FEET/LEGS/CHEST/HEAD order already adopted by the armor center (gregapi/GT_API_Proxy.java:1002). */
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

//  public static Map jumpChargeMap = new HashMap<>();

	public GT_EnergyArmor_Item(int aID, String aUnlocalized, String aEnglish, int aCharge, int aTransfer, int aTier, int aDamageEnergyCost, int aSpecials, double aArmorAbsorbtionPercentage, boolean aChargeProvider, int aType, int aArmorIndex) {
		// aArmorIndex (was renderIndex, 1.7.10 ItemArmor 2nd ctor param) — has no neo equivalent (the render model
		// is now EquipmentAssets/ResourceKey, not a numeric index), unused, same as before it never affected logic.
		super(makeProperties(aType));
		mName = aUnlocalized;
		mArmorType = aType;
		LH.add(getUnlocalizedName(), aEnglish);
		mCharge = Math.max(1, aCharge);
		mTransfer = Math.max(1, aTransfer);
		mTier = Math.max(1, aTier);
		mSpecials = aSpecials;
		mChargeProvider = aChargeProvider;
		mDamageEnergyCost = Math.max(0, aDamageEnergyCost);
		mArmorAbsorbtionPercentage = aArmorAbsorbtionPercentage;
		
		
		NeoForge.EVENT_BUS.register(this);
	}

	/** F13: assembles Properties BEFORE super() — durability/repair/humanoid-armor in one center, the same approach as
	 *  {@link gregapi.item.ItemArmorBase#makeProperties} (Item.Properties#humanoidArmor, Item.java:579). */
	private static Item.Properties makeProperties(int aArmorType) {
		return new Item.Properties()
			.humanoidArmor(ArmorMaterials.DIAMOND, armorTypeFor(aArmorType)) // was ArmorMaterial.DIAMOND (1.7.10 enum) -> neo ArmorMaterials.DIAMOND (ArmorMaterials.java:24)
			.durability(100) // was setMaxDamage(100); .durability() also gives stacksTo(1), covers setMaxStackSize(1) (Item.java:440-444)
			// was setNoRepair() — an empty (never populated) repair tag gives the same effect as a consequence, approach from ItemArmorBase.java:130-133
			.repairable(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ModIDs.GT, "repair/none")));
	}

	private static ArmorType armorTypeFor(int aArmorType) {
		switch (aArmorType) {
		case 0: return ArmorType.HELMET;
		case 1: return ArmorType.CHESTPLATE;
		case 2: return ArmorType.LEGGINGS;
		case 3: return ArmorType.BOOTS;
		}
		throw new IllegalArgumentException("Unknown Armor Slot: "+aArmorType);
	}

	public final String getUnlocalizedName() {return mName;}
	public final Item setUnlocalizedName(String aName) {return this;} // was a mutator on the 1.7.10 Item; the name is now immutable via mName, approach from ItemBase.java:125

	// @Override
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {
		ItemStack tStack = aPlayer.getItemBySlot(ARMOR_SLOTS[3-mArmorType]); // was armorInventory[3-armorType] (F15: getItemBySlot never null, EMPTY instead)
		if (!tStack.isEmpty()) {
			for (int i = 0; i < 9; i++) {
				if (aPlayer.getInventory().getItem(i) == aStack) {
					aPlayer.setItemSlot(ARMOR_SLOTS[3-mArmorType], aPlayer.getInventory().getItem(i)); // was armorInventory[3-armorType] = mainInventory[i] -> LivingEntity.setItemSlot (LivingEntity.java:2329)
					aPlayer.getInventory().setItem(i, tStack);
					return tStack;
				}
			}
		}
		// F-item-use dead-interface: neo Item does not declare onItemRightClick (use() is the new contract), the old super call is dead.
		// Honest fallback: return the stack unchanged (as if no matching slot was found).
		return aStack;
	}
	
	// @Override
	// F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo calls): was this.itemIcon=aIconRegister.registerIcon(...) — both the Item.itemIcon field
	// and IIconRegister are removed entirely in 26.1.2 (same bug class as ItemBase.java:131/getSubItems below in this
	// file, already reduced to a no-op); no replacement until Phase C.
	// F3-render: the param was IIconRegister (a removed class in the signature breaks method enumeration in GT6ItemModel.resolveIcon
	// -> NoClassDefFoundError -> the item does not render) -> Object. No-op body (the 1.7.10 icon-load phase is not ported to neo).
	public void registerIcons(Object aIconRegister) {/**/}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		aList.add(LH.tt("Tier: ") + mTier);
		if ((mSpecials &    1) != 0) aList.add(LH.tt("Rebreather"));
		if ((mSpecials &    2) != 0) aList.add(LH.tt("Inertia Damper"));
		if ((mSpecials &    4) != 0) aList.add(LH.tt("Food Replicator"));
		if ((mSpecials &    8) != 0) aList.add(LH.tt("Medicine Module"));
		if ((mSpecials &   16) != 0) aList.add(LH.tt("Lamp"));
		if ((mSpecials &   32) != 0) aList.add(LH.tt("Solarpanel"));
		if ((mSpecials &   64) != 0) aList.add(LH.tt("Extinguisher Module"));
		if ((mSpecials &  128) != 0) aList.add(LH.tt("Jump Booster"));
		if ((mSpecials &  256) != 0) aList.add(LH.tt("Speed Booster"));
		if ((mSpecials &  512) != 0) aList.add(LH.tt("Invisibility Field"));
		if ((mSpecials & 1024) != 0) aList.add(LH.tt("Infinite Charge"));
	}
	
	private static void setCharge(ItemStack aStack) {
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT == null) tNBT = UT.NBT.make();
		tNBT.putInt("charge", 1000000000);
		ItemNBT.set(aStack, tNBT);
	}
	
	// @Override
	public void onArmorTick(Level aWorld, Player aPlayer, ItemStack aStack) {/*
		if (mSpecials == 0) return;
		
		if (!aPlayer.worldObj.isClientSide() && (mSpecials & 1) != 0) {
			int var4 = aPlayer.getAir();
			if (GT_ModHandler.canUseElectricItem(aStack, 1000) && var4 < 50) {
				aPlayer.setAir(var4 + 250);
				GT_ModHandler.useElectricItem(aStack, 1000, aPlayer);
			}
		}
		
		if (!aPlayer.worldObj.isClientSide() && (mSpecials & 4) != 0) {
			if (GT_ModHandler.canUseElectricItem(aStack, 50000) && aPlayer.getFoodData().needsFood()) {
				aPlayer.getFoodData().eat(1, 0.0F);
				GT_ModHandler.useElectricItem(aStack, 50000, aPlayer);
			}
		}
		
		if ((mSpecials & 8) != 0) {
			if (GT_ModHandler.canUseElectricItem(aStack, 10000) && aPlayer.isPotionActive(Potion.poison)) {
				UT.Entities.removePotion(aPlayer, Potion.poison.id);
				GT_ModHandler.useElectricItem(aStack, 10000, aPlayer);
			}
			if (GT_ModHandler.canUseElectricItem(aStack, 100000) && aPlayer.isPotionActive(Potion.wither)) {
				UT.Entities.removePotion(aPlayer, Potion.wither.id);
				GT_ModHandler.useElectricItem(aStack, 100000, aPlayer);
			}
		}

		if ((mSpecials & 64) != 0) {
			aPlayer.setFire(0);
		}
		
		if (!aPlayer.worldObj.isClientSide() && (mSpecials & 128) != 0) {
			float var6 = jumpChargeMap.containsKey(aPlayer) ? ((Float)jumpChargeMap.get(aPlayer)).floatValue() : 1.0F;

			if (GT_ModHandler.canUseElectricItem(aStack, 1000) && aPlayer.onGround && var6 < 1.0F) {
				var6 = 1.0F;
				GT_ModHandler.useElectricItem(aStack, 1000, aPlayer);
			}

			if (aPlayer.getDeltaMovement().y >= 0.0D && var6 > 0.0F && !aPlayer.isInWater()) {
				if (GT_ModHandler.getJumpKeyDown(aPlayer) && GT_ModHandler.getBoostKeyDown(aPlayer)) {
					if (var6 == 1.0F) {
						aPlayer.getDeltaMovement().x *= 3.5D;
						aPlayer.getDeltaMovement().z *= 3.5D;
					}

					aPlayer.getDeltaMovement().y += (var6 * 0.3F);
					var6 = (float)(var6 * 0.75D);
				} else if (var6 < 1.0F) {
					var6 = 0.0F;
				}
			}

			jumpChargeMap.put(aPlayer, Float.valueOf(var6));
		}

		if ((mSpecials & 256) != 0) {
			if (GT_ModHandler.canUseElectricItem(aStack, 100) && aPlayer.isSprinting() && (aPlayer.onGround && Math.abs(aPlayer.getDeltaMovement().x) + Math.abs(aPlayer.getDeltaMovement().z) > 0.10000000149011612D || aPlayer.isInWater())) {
				GT_ModHandler.useElectricItem(aStack, 100, aPlayer);
				float var7 = 0.22F;
				
				if (aPlayer.isInWater()) {
					GT_ModHandler.useElectricItem(aStack, 100, aPlayer);
					var7 = 0.1F;
					
					
					if (aPlayer.getDeltaMovement().y > 0) {
						aPlayer.getDeltaMovement().y += 0.10000000149011612D;
					}
				}
				
				if (var7 > 0.0F) {
					aPlayer.moveFlying(0.0F, 1.0F, var7);
				}
			}
		}
		
		if ((mSpecials & 512) != 0) {
			if (GT_ModHandler.canUseElectricItem(aStack, 10000)) {
				GT_ModHandler.useElectricItem(aStack, 10000, aPlayer);
				aPlayer.addPotionEffect(new PotionEffect(Potion.invisibility.getId(), 25, 1, true));
			}
		}
		
		if (!aPlayer.worldObj.isClientSide() && (mSpecials & (16|32)) != 0) {
			//if (GregTech_API.sWorldTickCounter%20==0) {
				ItemStack tTargetChargeItem = aStack, tTargetDechargeItem = aStack;
				
				if (GT_ModHandler.chargeElectricItem(tTargetChargeItem, 1, Integer.MAX_VALUE, true, true) < 1) {
					tTargetChargeItem = aPlayer.inventory.armorInventory[2];
				}
				if (GT_ModHandler.dischargeElectricItem(tTargetDechargeItem, 10, Integer.MAX_VALUE, true, true, true) < 10) {
					tTargetDechargeItem = aPlayer.inventory.armorInventory[2];
				}
				
				if (tTargetChargeItem == null || !GT_ModHandler.isElectricItem(tTargetChargeItem)) {
					tTargetChargeItem = null;
				}
				if (tTargetDechargeItem == null || !GT_ModHandler.isElectricItem(tTargetChargeItem) || !(aStack == tTargetDechargeItem || GT_ModHandler.isChargerItem(tTargetDechargeItem))) {
					tTargetDechargeItem = null;
				}
				
				if (aPlayer.worldObj.isDaytime() && WD.canSeeSky(aPlayer.worldObj, MathHelper.floor_double(aPlayer.posX), MathHelper.floor_double(aPlayer.posY+1), MathHelper.floor_double(aPlayer.posZ))) {
					if ((mSpecials & 32) != 0 && tTargetChargeItem != null) {
						GT_ModHandler.chargeElectricItem(tTargetChargeItem, 20, Integer.MAX_VALUE, true, false);
					}
				} else {
					if ((mSpecials & 16) != 0 && tTargetDechargeItem != null && GT_ModHandler.canUseElectricItem(tTargetDechargeItem, 10)) {
						if (aPlayer.worldObj.getBlock   ((int)aPlayer.posX, (int)aPlayer.posY+1, (int)aPlayer.posZ) == Blocks.AIR)
							aPlayer.worldObj.setBlock   ((int)aPlayer.posX, (int)aPlayer.posY+1, (int)aPlayer.posZ, GregTech_API.sBlockList[3]);
						GT_ModHandler.useElectricItem(tTargetDechargeItem, 10, aPlayer);
					}
				//}
			}
		}
	*/}
	
	// @Override
	public boolean getShareTag() {
		return true;
	}
	
	// @Override
//  @SuppressWarnings("unchecked")
	public void getSubItems(Item aItem, CreativeModeTab var2, @SuppressWarnings("rawtypes") List var3) {
		//ItemStack tCharged = ST.make(this, 1, 0), tUncharged = ST.make(this, 1, getMaxDamage());
		//GT_ModHandler.chargeElectricItem(tCharged, Integer.MAX_VALUE, Integer.MAX_VALUE, true, false);
		//var3.add(tCharged);
		//var3.add(tUncharged);
	}
	
	public boolean canProvideEnergy(ItemStack aStack) {
		if ((mSpecials & 1024) != 0) setCharge(aStack);
		return mChargeProvider;
	}
	
	public Item getChargedItem(ItemStack aStack) {
		if ((mSpecials & 1024) != 0) setCharge(aStack);
		return this;
	}
	
	public Item getEmptyItem(ItemStack aStack) {
		if ((mSpecials & 1024) != 0) setCharge(aStack);
		return this;
	}
	
	public int getMaxCharge(ItemStack aStack) {
		if ((mSpecials & 1024) != 0) setCharge(aStack);
		return mCharge;
	}
	
	public int getTier(ItemStack aStack) {
		if ((mSpecials & 1024) != 0) setCharge(aStack);
		return mTier;
	}
	
	public int getTransferLimit(ItemStack aStack) {
		if ((mSpecials & 1024) != 0) setCharge(aStack);
		return mTransfer;
	}
	
	// @Override
	public int getItemEnchantability() {
		return 0;
	}
	
	// @Override
	public boolean isBookEnchantable(ItemStack itemstack1, ItemStack itemstack2) {
		return false;
	}
	
	// @Override
	public boolean getIsRepairable(ItemStack par1ItemStack, ItemStack par2ItemStack) {
		return false;
	}
	
	// @ForgeSubscribe
	public void onEntityLivingFallEvent(LivingFallEvent var1) {/*
		if (!var1.entity.worldObj.isClientSide() && var1.entity instanceof EntityPlayer) {
			EntityPlayer var2 = (EntityPlayer)var1.entity;
			for (int i = 0; i < 4; i++) {
				ItemStack var3 = var2.inventory.armorInventory[i];
				if (var3 != null && var3.getItem() == this && (mSpecials & 2) != 0) {
					int var4 = (int)var1.distance - 3;
					int var5 = (this.mDamageEnergyCost * var4) / 4;
					if (var5 <= GT_ModHandler.dischargeElectricItem(var3, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, true)) {
						GT_ModHandler.dischargeElectricItem(var3, var5, Integer.MAX_VALUE, true, false, true);
						var1.setCanceled(true);
						break;
					}
				}
			}
		}
	*/}
	/*
	@Override
	public ISpecialArmor.ArmorProperties getProperties(EntityLivingBase var1, ItemStack var2, DamageSource var3, double var4, int var6) {
		return new ISpecialArmor.ArmorProperties((var3 == DamageSource.fall && (mSpecials & 2) != 0)?10:0, getBaseAbsorptionRatio() * mArmorAbsorbtionPercentage, mDamageEnergyCost > 0 ? 25 * GT_ModHandler.dischargeElectricItem(var2, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, true) / mDamageEnergyCost : 0);
	}
	
	@Override
	public int getArmorDisplay(EntityPlayer var1, ItemStack var2, int var3) {
		return (int)Math.round(20.0D * getBaseAbsorptionRatio() * mArmorAbsorbtionPercentage);
	}
	
	@Override
	public void damageArmor(EntityLivingBase var1, ItemStack var2, DamageSource var3, int var4, int var5) {
		//GT_ModHandler.dischargeElectricItem(var2, var4 * mDamageEnergyCost, Integer.MAX_VALUE, true, false, true);
	}
	
	private double getBaseAbsorptionRatio() {
		if (mArmorAbsorbtionPercentage <= 0) return 0.00;
		switch (armorType) {
			case  0: return 0.15;
			case  1: return 0.40;
			case  2: return 0.30;
			case  3: return 0.15;
			default: return 0.00;
		}
	}*/
}
