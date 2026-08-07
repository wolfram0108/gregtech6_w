/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregtech.tileentity.tools;

import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import gregapi.GT_API_Proxy;
import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.block.multitileentity.MultiTileEntityContainer;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.*;
import gregapi.data.LH.Chat;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;
import gregapi.render.BlockTextureDefault;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityServerTickPost;
import gregapi.tileentity.base.TileEntityBase07Paintable;
import gregapi.tileentity.data.ITileEntityGibbl;
import gregapi.tileentity.data.ITileEntityTemperature;
import gregapi.tileentity.data.ITileEntityWeight;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntitySmeltery extends TileEntityBase07Paintable implements ITileEntityCrucible, ITileEntityEnergy, ITileEntityGibbl, ITileEntityWeight, ITileEntityTemperature, ITileEntityMold, ITileEntityServerTickPost, IMTE_RemovedByPlayer, IMTE_OnEntityCollidedWithBlock, IMTE_GetCollisionBoundingBoxFromPool, IMTE_AddToolTips, IMTE_OnPlaced {
	public static int GAS_RANGE = 3, FLAME_RANGE = 3;
	public static long MAX_AMOUNT = 16*U, KG_PER_ENERGY = 100;
	public static double HEAT_RESISTANCE_BONUS = 1.25;
	
	protected boolean mAcidProof = F, mMeltDown = F;
	protected byte mDisplayedHeight = 0, oDisplayedHeight = 0, mCooldown = 100;
	protected short mDisplayedFluid = -1, oDisplayedFluid = -1;
	protected long mEnergy = 0, mTemperature = DEF_ENV_TEMP, oTemperature = 0;
	protected List<OreDictMaterialStack> mContent = new ArrayListNoNulls<>();
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		mEnergy = aNBT.getLongOr(NBT_ENERGY, 0L);
		if (aNBT.contains(NBT_ACIDPROOF)) mAcidProof = aNBT.getBooleanOr(NBT_ACIDPROOF, false);
		if (aNBT.contains(NBT_TEMPERATURE)) mTemperature = aNBT.getLongOr(NBT_TEMPERATURE, 0L);
		if (aNBT.contains(NBT_TEMPERATURE+".old")) oTemperature = aNBT.getLongOr(NBT_TEMPERATURE+".old", 0L);
		mContent = OreDictMaterialStack.loadList(NBT_MATERIALS, aNBT);
		mMeltDown = (mTemperature+100 > getTemperatureMax(SIDE_INSIDE));
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		UT.NBT.setNumber(aNBT, NBT_ENERGY, mEnergy);
		UT.NBT.setNumber(aNBT, NBT_TEMPERATURE, mTemperature);
		UT.NBT.setNumber(aNBT, NBT_TEMPERATURE+".old", oTemperature);
		OreDictMaterialStack.saveList(NBT_MATERIALS, aNBT, mContent);
	}
	
	static {
		LH.add("gt.tooltip.crucible.1", "KU Input will turn into Air for Steelmaking");
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get(LH.CONVERTS_FROM_X) + " 1 " + TD.Energy.HU.getLocalisedNameShort() + " " + LH.get(LH.CONVERTS_TO_Y) + " +1 K " + LH.get(LH.CONVERTS_PER_Z) + " "+ KG_PER_ENERGY + "kg (at least "+getEnergySizeInputMin(TD.Energy.HU, SIDE_ANY)+" Units per Tick required!)");
		aList.add(Chat.YELLOW   + LH.get(LH.TOOLTIP_THERMALMASS) + mMaterial.getWeight(U*7) + " kg");
		aList.add(Chat.DRED     + LH.get(LH.HAZARD_MELTDOWN) + " (" + getTemperatureMax(SIDE_INSIDE) + " K)");
		aList.add(Chat.WHITE    + LH.get("gt.tooltip.crucible.1"));
		if (mAcidProof) aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_ACIDPROOF));
		aList.add(Chat.DRED     + LH.get(LH.HAZARD_FIRE) + " ("+(FLAME_RANGE+1)+"m)");
		aList.add(Chat.DRED     + LH.get(LH.HAZARD_CONTACT));
		aList.add(Chat.DGRAY    + LH.get(LH.TOOL_TO_MEASURE_THERMOMETER));
		aList.add(Chat.DGRAY    + LH.get(LH.TOOL_TO_REMOVE_SHOVEL));
	}
	
	private boolean mHasToAddTimer = T;
	
	@Override public void onUnregisterPost() {mHasToAddTimer = T;}
	
	@Override
	public void onCoordinateChange() {
		super.onCoordinateChange();
		GT_API_Proxy.SERVER_TICK_POST.remove(this);
		onUnregisterPost();
	}
	
	@Override
	public void onTick2(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide && mHasToAddTimer) {
			GT_API_Proxy.SERVER_TICK_POST.add(this);
			mHasToAddTimer = F;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void onServerTickPost(boolean aFirst) {
		long tTemperature = WD.envTemp(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()), tHash = mContent.hashCode();
		
		if (SERVER_TIME % 600 == 10 && level.isRaining() && getRainOffset(0, 1, 0)) {
			Biome tBiome = getBiome();
			if (WD.rainfall(tBiome) > 0 && tBiome.getBaseTemperature() >= 0.2) {
				addMaterialStacks(Arrays.asList(OM.stack(MT.Water, U1000 * (long)Math.max(1, WD.rainfall(tBiome)*100) * (level.isThundering()?2:1))), tTemperature);
			}
		}
		
		if (!slotHas(0)) slot(0, WD.suck(level, getBlockPos().getX()+PX_P[2], getBlockPos().getY()+PX_P[2], getBlockPos().getZ()+PX_P[2], PX_N[4], 1, PX_N[4]));
		
		ItemStack tStack = slot(0);
		
		if (ST.valid(tStack)) {
			OreDictItemData tData = OM.anydata_(tStack);
			if (tData == null) {
				slotTrash(0);
				UT.Sounds.send(SFX.MC_FIZZ, this, F);
			} else if (tData.mPrefix == null) {
				List<OreDictMaterialStack> tList = new ArrayListNoNulls<>();
				for (OreDictMaterialStack tMaterial : tData.getAllMaterialStacks()) if (tMaterial.mAmount > 0) tList.add(tMaterial.clone());
				if (addMaterialStacks(tList, tTemperature)) removeItem(0, 1);
			} else if (tData.mPrefix == OP.oreRaw) {
				if (addMaterialStacks(Arrays.asList(OM.stack(tData.mMaterial.mMaterial.mTargetCrushing.mMaterial, tData.mMaterial.mMaterial.mTargetCrushing.mAmount * tData.mMaterial.mMaterial.mOreMultiplier     )), tTemperature)) removeItem(0, 1);
			} else if (tData.mPrefix == OP.blockRaw) {
				if (addMaterialStacks(Arrays.asList(OM.stack(tData.mMaterial.mMaterial.mTargetCrushing.mMaterial, tData.mMaterial.mMaterial.mTargetCrushing.mAmount * tData.mMaterial.mMaterial.mOreMultiplier *  9)), tTemperature)) removeItem(0, 1);
			} else if (tData.mPrefix == OP.crateGtRaw) {
				if (addMaterialStacks(Arrays.asList(OM.stack(tData.mMaterial.mMaterial.mTargetCrushing.mMaterial, tData.mMaterial.mMaterial.mTargetCrushing.mAmount * tData.mMaterial.mMaterial.mOreMultiplier * 16)), tTemperature)) removeItem(0, 1);
			} else if (tData.mPrefix == OP.crateGt64Raw) {
				if (addMaterialStacks(Arrays.asList(OM.stack(tData.mMaterial.mMaterial.mTargetCrushing.mMaterial, tData.mMaterial.mMaterial.mTargetCrushing.mAmount * tData.mMaterial.mMaterial.mOreMultiplier * 64)), tTemperature)) removeItem(0, 1);
			} else if (tData.mPrefix.contains(TD.Prefix.STANDARD_ORE)) {
				if (addMaterialStacks(Arrays.asList(OM.stack(tData.mMaterial.mMaterial.mTargetCrushing.mMaterial, tData.mMaterial.mMaterial.mTargetCrushing.mAmount * tData.mMaterial.mMaterial.mOreMultiplier     )), tTemperature)) removeItem(0, 1);
			} else if (tData.mPrefix.contains(TD.Prefix.DENSE_ORE)) {
				if (addMaterialStacks(Arrays.asList(OM.stack(tData.mMaterial.mMaterial.mTargetCrushing.mMaterial, tData.mMaterial.mMaterial.mTargetCrushing.mAmount * tData.mMaterial.mMaterial.mOreMultiplier *  2)), tTemperature)) removeItem(0, 1);
			} else {
				List<OreDictMaterialStack> tList = new ArrayListNoNulls<>();
				for (OreDictMaterialStack tMaterial : tData.getAllMaterialStacks()) if (tMaterial.mAmount > 0) tList.add(tMaterial.clone());
				if (addMaterialStacks(tList, tTemperature)) removeItem(0, 1);
			}
		}
		
		Set<OreDictMaterial> tAlreadyCheckedAlloys = new HashSetNoNulls<>();
		
		OreDictMaterial tPreferredAlloy = null;
		IOreDictConfigurationComponent tPreferredRecipe = null;
		long tMaxConversions = 0;
		boolean tNewContent = (tHash != mContent.hashCode());
		
		for (OreDictMaterialStack tMaterial : mContent) {
			if (mTemperature >= tMaterial.mMaterial.mMeltingPoint) {
				for (OreDictMaterial tAlloy : tMaterial.mMaterial.mAlloyComponentReferences) if (tAlreadyCheckedAlloys.add(tAlloy) && mTemperature >= tAlloy.mMeltingPoint) {
					for (IOreDictConfigurationComponent tAlloyRecipe : tAlloy.mAlloyCreationRecipes) {
						List<OreDictMaterialStack> tNeededStuff = new ArrayListNoNulls<>();
						for (OreDictMaterialStack tComponent : tAlloyRecipe.getUndividedComponents()) {
							tNeededStuff.add(OM.stack(tComponent.mMaterial, Math.max(1, tComponent.mAmount / U)));
						}
						
						if (!tNeededStuff.isEmpty()) {
							int tNonMolten = 0;
							
							boolean tBreak = F;
							long tConversions = Long.MAX_VALUE;
							for (OreDictMaterialStack tComponent : tNeededStuff) {
								if (mTemperature < tComponent.mMaterial.mMeltingPoint) tNonMolten++;
								
								tBreak = T;
								for (OreDictMaterialStack tContent : mContent) {
									if (tContent.mMaterial == tComponent.mMaterial) {
										tConversions = Math.min(tConversions, tContent.mAmount / tComponent.mAmount);
										tBreak = F;
										break;
									}
								}
								if (tBreak) break;
							}
							
							if (!tBreak && tNonMolten <= 1 && tConversions > 0) {
								if (tPreferredAlloy == null || tPreferredRecipe == null || tConversions * tAlloyRecipe.getCommonDivider() > tMaxConversions * tPreferredRecipe.getCommonDivider()) {
									tMaxConversions = tConversions;
									tPreferredRecipe = tAlloyRecipe;
									tPreferredAlloy = tAlloy;
								}
							}
						}
					}
				}
			}
		}
		
		if (tPreferredAlloy != null && tPreferredRecipe != null) {
			for (OreDictMaterialStack tComponent : tPreferredRecipe.getUndividedComponents()) {
				for (OreDictMaterialStack tContent : mContent) {
					if (tContent.mMaterial == tComponent.mMaterial) {
						tContent.mAmount -= UT.Code.units_(tMaxConversions, U, tComponent.mAmount, T);
						break;
					}
				}
			}
			OM.stack(tPreferredAlloy, tPreferredRecipe.getCommonDivider() * tMaxConversions).addToList(mContent);
		}
		
		List<OreDictMaterialStack> tToBeAdded = new ArrayListNoNulls<>();
		for (int i = 0; i < mContent.size(); i++) {
			OreDictMaterialStack tMaterial = mContent.get(i);
			if (tMaterial == null || tMaterial.mMaterial == MT.NULL || tMaterial.mMaterial == MT.Air || tMaterial.mAmount <= 0) {
				mContent.remove(i--);
			} else if (tMaterial.mMaterial.mGramPerCubicCentimeter <= WEIGHT_AIR_G_PER_CUBIC_CENTIMETER) {
				GarbageGT.trash(mContent.remove(i--));
				UT.Sounds.send(SFX.MC_FIZZ, this, F);
			} else if (mTemperature >= tMaterial.mMaterial.mBoilingPoint || (mTemperature > C + 40 && tMaterial.mMaterial.contains(TD.Properties.FLAMMABLE) && !tMaterial.mMaterial.containsAny(TD.Properties.UNBURNABLE, TD.Processing.MELTING))) {
				GarbageGT.trash(mContent.remove(i--));
				UT.Sounds.send(SFX.MC_FIZZ, this, F);
				if (tMaterial.mMaterial.mBoilingPoint >=  320) try {for (LivingEntity tLiving : (List<LivingEntity>)level.getEntitiesOfClass(LivingEntity.class, box(-GAS_RANGE, -1, -GAS_RANGE, GAS_RANGE+1, GAS_RANGE+1, GAS_RANGE+1))) UT.Entities.applyTemperatureDamage(tLiving, tMaterial.mMaterial.mBoilingPoint, 2);} catch(Throwable e) {e.printStackTrace(ERR);}
				if (tMaterial.mMaterial.mBoilingPoint >= 2000) for (int j = 0, k = Math.max(1, UT.Code.bindInt((9 * tMaterial.mAmount) / U)); j < k; j++) WD.fire(level, getBlockPos().getX()-FLAME_RANGE+rng(2*FLAME_RANGE+1), getBlockPos().getY()-1+rng(2+FLAME_RANGE), getBlockPos().getZ()-FLAME_RANGE+rng(2*FLAME_RANGE+1), rng(3) != 0);
				if (tMaterial.mMaterial.contains(TD.Properties.EXPLOSIVE)) {
					GarbageGT.trash(mContent);
					GarbageGT.trash(tToBeAdded);
					explode(UT.Code.scale(tMaterial.mAmount, MAX_AMOUNT, 6, F));
					return;
				}
			} else if (!mAcidProof && tMaterial.mMaterial.contains(TD.Properties.ACID)) {
				GarbageGT.trash(mContent);
				GarbageGT.trash(tToBeAdded);
				UT.Sounds.send(SFX.MC_FIZZ, this, F);
				setToAir();
				return;
			} else if (mTemperature >= tMaterial.mMaterial.mMeltingPoint && (oTemperature <  tMaterial.mMaterial.mMeltingPoint || tNewContent)) {
				mContent.remove(i--);
				OM.stack(tMaterial.mMaterial.mTargetSmelting.mMaterial, UT.Code.units_(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSmelting.mAmount, F)).addToList(tToBeAdded);
			} else if (mTemperature <  tMaterial.mMaterial.mMeltingPoint && (oTemperature >= tMaterial.mMaterial.mMeltingPoint || tNewContent)) {
				mContent.remove(i--);
				OM.stack(tMaterial.mMaterial.mTargetSolidifying.mMaterial, UT.Code.units_(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSolidifying.mAmount, F)).addToList(tToBeAdded);
			}
		}
		for (int i = 0; i < tToBeAdded.size(); i++) {
			OreDictMaterialStack tMaterial = tToBeAdded.get(i);
			if (tMaterial != null && tMaterial.mAmount > 0 && tMaterial.mMaterial != MT.NULL && tMaterial.mMaterial != MT.Air) {
				tMaterial.addToList(mContent);
			}
		}
		
		double tWeight = mMaterial.getWeight(U*7);
		long tTotal = 0;
		OreDictMaterialStack tLightest = null;
		
		for (OreDictMaterialStack tMaterial : mContent) {
			if (tLightest == null || tMaterial.mMaterial.mGramPerCubicCentimeter < tLightest.mMaterial.mGramPerCubicCentimeter) tLightest = tMaterial;
			tWeight += tMaterial.weight();
			tTotal += tMaterial.mAmount;
		}
		
		oTemperature = mTemperature;
		
		mDisplayedHeight = (byte)UT.Code.scale(tTotal, MAX_AMOUNT, 255, F);
		mDisplayedFluid = (tLightest == null || tLightest.mMaterial.mMeltingPoint > mTemperature ? -1 : tLightest.mMaterial.mID);
		
		long tRequiredEnergy = 1 + (long)(tWeight / KG_PER_ENERGY), tConversions = mEnergy / tRequiredEnergy;
		
		if (mCooldown > 0) mCooldown--;
		
		if (tConversions != 0) {
			mEnergy -= tConversions * tRequiredEnergy;
			mTemperature += tConversions;
			mCooldown = 100;
		}
		
		if (mCooldown <= 0) {mCooldown = 10; if (mTemperature > tTemperature) mTemperature--; if (mTemperature < tTemperature) mTemperature++;}
		
		mTemperature = Math.max(mTemperature, Math.min(200, tTemperature));
		
		if (mTemperature > getTemperatureMax(SIDE_INSIDE)) {
			UT.Sounds.send(SFX.MC_FIZZ, this, F);
			GarbageGT.trash(mContent);
			if (mTemperature >=  320) try {for (LivingEntity tLiving : (List<LivingEntity>)level.getEntitiesOfClass(LivingEntity.class, box(-GAS_RANGE, -1, -GAS_RANGE, GAS_RANGE+1, GAS_RANGE+1, GAS_RANGE+1))) UT.Entities.applyTemperatureDamage(tLiving, mTemperature, 2);} catch(Throwable e) {e.printStackTrace(ERR);}
			for (int j = 0, k = UT.Code.bindInt(mTemperature / 25); j < k; j++) WD.fire(level, getBlockPos().getX()-FLAME_RANGE+rng(2*FLAME_RANGE+1), getBlockPos().getY()-1+rng(2+FLAME_RANGE), getBlockPos().getZ()-FLAME_RANGE+rng(2*FLAME_RANGE+1), rng(3) != 0);
			WD.set(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), Blocks.LAVA, 1, 3);
			return;
		}
		
		if (mMeltDown != (mTemperature+100 > getTemperatureMax(SIDE_INSIDE))) {
			mMeltDown = !mMeltDown;
			updateClientData();
		}
	}
	
	public boolean addMaterialStacks(List<OreDictMaterialStack> aList, long aTemperature) {
		if (OM.total(mContent)+OM.total(aList) <= MAX_AMOUNT) {
			double tWeight1 = OM.weight(mContent)+mMaterial.getWeight(U*7), tWeight2 = OM.weight(aList);
			if (tWeight1+tWeight2 > 0) mTemperature = aTemperature + (mTemperature>aTemperature?+1:-1)*UT.Code.units(Math.abs(mTemperature - aTemperature), (long)(tWeight1+tWeight2), (long)tWeight1, F);
			for (OreDictMaterialStack tMaterial : aList) {
				if (mTemperature >= tMaterial.mMaterial.mMeltingPoint) {
					if (aTemperature <  tMaterial.mMaterial.mMeltingPoint) {
						OM.stack(tMaterial.mMaterial.mTargetSmelting.mMaterial, UT.Code.units_(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSmelting.mAmount, F)).addToList(mContent);
					} else {
						tMaterial.addToList(mContent);
					}
				} else {
					if (aTemperature >= tMaterial.mMaterial.mMeltingPoint) {
						OM.stack(tMaterial.mMaterial.mTargetSolidifying.mMaterial, UT.Code.units_(tMaterial.mAmount, U, tMaterial.mMaterial.mTargetSolidifying.mAmount, F)).addToList(mContent);
					} else {
						tMaterial.addToList(mContent);
					}
				}
			}
			return T;
		}
		return F;
	}
	
	@Override
	public long getTemperatureValue(byte aSide) {
		return mTemperature;
	}
	
	@Override
	public long getTemperatureMax(byte aSide) {
		return (long)(mMaterial.mMeltingPoint * HEAT_RESISTANCE_BONUS);
	}
	
	@Override
	public boolean isMoldInputSide(byte aSide) {
		return SIDES_TOP[aSide];
	}
	
	@Override
	public long getMoldMaxTemperature() {
		return getTemperatureMax(SIDE_INSIDE);
	}
	
	@Override
	public long getMoldRequiredMaterialUnits() {
		return 1;
	}
	
	@Override
	public long fillMold(OreDictMaterialStack aMaterial, long aTemperature, byte aSide) {
		if (isMoldInputSide(aSide)) {
			if (addMaterialStacks(Arrays.asList(aMaterial), aTemperature)) return aMaterial.mAmount;
			if (aMaterial.mAmount > U && addMaterialStacks(Arrays.asList(OM.stack(aMaterial.mMaterial, U)), aTemperature)) return U;
		}
		return 0;
	}
	
	@Override public double getWeightValue(byte aSide) {return OM.weight(mContent);}
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean removedByPlayer(Level aWorld, Player aPlayer, boolean aWillHarvest) {
		if (mTemperature >= 1300 && isServerSide() && !UT.Entities.isCreative(aPlayer)) {
			UT.Sounds.send(SFX.MC_FIZZ, this, F);
			GarbageGT.trash(mContent);
			try {for (LivingEntity tLiving : (List<LivingEntity>)level.getEntitiesOfClass(LivingEntity.class, box(-GAS_RANGE, -1, -GAS_RANGE, GAS_RANGE+1, GAS_RANGE+1, GAS_RANGE+1))) UT.Entities.applyTemperatureDamage(tLiving, mTemperature);} catch(Throwable e) {e.printStackTrace(ERR);}
			for (int j = 0, k = UT.Code.bindInt(mTemperature / 25); j < k; j++) WD.fire(level, getBlockPos().getX()-FLAME_RANGE+rng(2*FLAME_RANGE+1), getBlockPos().getY()-1+rng(2+FLAME_RANGE), getBlockPos().getZ()-FLAME_RANGE+rng(2*FLAME_RANGE+1), rng(3) != 0);
			return WD.set(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), Blocks.LAVA, 1, 3);
		}
		return WD.set(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), NB, 0, 3);
	}
	
	@Override
	public boolean breakBlock() {
		GarbageGT.trash(mContent);
		return super.breakBlock();
	}
	
	@Override public boolean attachCoversFirst(byte aSide) {return F;}
	
	@Override
	public boolean onBlockActivated3(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (SIDES_TOP[aSide]) {
			if (isServerSide() && aPlayer != null) {
				ItemStack aStack = ST.n(aPlayer.getMainHandItem()); // F15-граница: движок EMPTY -> GT6 null (тело 1:1 рассуждает null-семантикой)
				OreDictMaterialStack tLightest = null;
				for (OreDictMaterialStack tMaterial : mContent) if (tLightest == null || tMaterial.mMaterial.mGramPerCubicCentimeter < tLightest.mMaterial.mGramPerCubicCentimeter) tLightest = tMaterial;
				
				if (slotHas(0)) {
					if (aStack == null) {
						aPlayer.getInventory().setItem(aPlayer.getInventory().getSelectedSlot(), slotTake(0));
						UT.Entities.applyTemperatureDamage(aPlayer, mTemperature, 1, 5.0F);
						return T;
					}
				} else {
					if (tLightest != null && mTemperature < tLightest.mMaterial.mMeltingPoint) {
						ItemStack tOutputStack = OP.scrapGt.mat(tLightest.mMaterial, 1);
						if (tOutputStack == null || tLightest.mAmount < OP.scrapGt.mAmount) {
							tLightest.mAmount = 0;
							UT.Entities.exhaust(aPlayer);
							UT.Entities.applyTemperatureDamage(aPlayer, mTemperature, 1, 5.0F);
							return T;
						}
						if (aStack == null) {
							aPlayer.getInventory().setItem(aPlayer.getInventory().getSelectedSlot(), tOutputStack);
							tLightest.mAmount-=OP.scrapGt.mAmount;
							UT.Entities.exhaust(aPlayer);
							UT.Entities.applyTemperatureDamage(aPlayer, mTemperature, 1, 5.0F);
							return T;
						}
						if (ST.equal(aStack, tOutputStack) && aStack.getCount() < aStack.getMaxStackSize()) {
							aStack.setCount(aStack.getCount()+1);
							tLightest.mAmount-=OP.scrapGt.mAmount;
							UT.Entities.exhaust(aPlayer);
							UT.Entities.applyTemperatureDamage(aPlayer, mTemperature, 1, 5.0F);
							return T;
						}
					}
				}
				if (aStack != null) {
					FluidStack tFluid = FL.getFluid(ST.amount(1, aStack), T);
					if (tFluid == null) {
						if (tLightest != null && tLightest.mMaterial.mLiquid != null) {
							long tTemperature = FL.temperature(tLightest.mMaterial.mLiquid);
							if (mTemperature >= tLightest.mMaterial.mMeltingPoint && (tTemperature < 320 || mTemperature >= tTemperature)) {
								tFluid = tLightest.mMaterial.liquid(tLightest.mAmount, F);
								if (FL.nonzero(tFluid)) {
									int tAmount = tFluid.getAmount();
									ItemStack tStack = FL.fill(tFluid, ST.amount(1, aStack), T, T, T, T);
									if (ST.valid(tStack)) {
										tLightest.mAmount -= UT.Code.units(tAmount - tFluid.getAmount(), tLightest.mMaterial.mLiquid.getAmount(), tLightest.mMaterial.mLiquidUnit, T);
										aStack.setCount(aStack.getCount()-1);
										ST.give(aPlayer, tStack, T);
										return T;
									}
								}
							}
						}
					} else {
						if (!FL.gas(tFluid, T) && !FL.acid(tFluid)) {
							ItemStack tStack = ST.container(ST.amount(1, aStack), T);
							OreDictMaterialStack tFluidData = OreDictMaterial.FLUID_MAP.get(FL.regName(tFluid.getFluid()));
							if (tFluidData != null) {
								if (FL.equal(tFluidData.mMaterial.mLiquid, tFluid)) {
									if (addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(tFluidData.mMaterial, UT.Code.units(tFluid.getAmount(), tFluidData.mMaterial.mLiquid.getAmount(), tFluidData.mMaterial.mLiquidUnit, F))), UT.Code.bind(FL.temperature(tFluid), tFluidData.mMaterial.mMeltingPoint+25, tFluidData.mMaterial.mBoilingPoint-1))) {
										aStack.setCount(aStack.getCount()-1);
										ST.give(aPlayer, tStack, T);
										return T;
									}
								} else {
									if (addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(tFluidData.mMaterial, UT.Code.units(tFluid.getAmount(), tFluidData.mAmount, U, F))), UT.Code.bind(FL.temperature(tFluid), tFluidData.mMaterial.mMeltingPoint+25, tFluidData.mMaterial.mBoilingPoint-1))) {
										aStack.setCount(aStack.getCount()-1);
										ST.give(aPlayer, tStack, T);
										return T;
									}
								}
							}
						}
					}
				}
			}
			return T;
		}
		return F;
	}
	
	@Override
	public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
		for (OreDictMaterialStack tContent : mContent) if (tContent != null && mTemperature >= tContent.mMaterial.mMeltingPoint && tContent.mMaterial.mTargetSmelting.mMaterial == tContent.mMaterial) {
			long tAmount = aMold.fillMold(tContent, mTemperature, aSideOfMold);
			if (tAmount > 0) {
				tContent.mAmount -= tAmount;
				return T;
			}
		}
		return F;
	}
	
	@Override
	public long onToolClick2(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isClientSide()) return super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
		if (aTool.equals(TOOL_thermometer)) {if (aChatReturn != null) aChatReturn.add("Temperature: " + mTemperature + (mTemperature >= 1300 ? "K (too hot to pick it up right now!)" : "K")); return 10000;}
		if (aTool.equals(TOOL_shovel) && SIDES_TOP[aSide] && aPlayer instanceof Player) {
			OreDictMaterialStack tLightest = null;
			for (OreDictMaterialStack tMaterial : mContent) if (tLightest == null || tMaterial.mMaterial.mGramPerCubicCentimeter < tLightest.mMaterial.mGramPerCubicCentimeter) tLightest = tMaterial;
			if (tLightest != null && mTemperature < tLightest.mMaterial.mMeltingPoint) {
				if (tLightest.mAmount < OP.scrapGt.mAmount) {
					tLightest.mAmount = 0;
					UT.Entities.exhaust(aPlayer);
					return 500;
				}
				ItemStack tOutputStack = OP.scrapGt.mat(tLightest.mMaterial, UT.Code.bindStack(tLightest.mAmount / OP.scrapGt.mAmount));
				if (tOutputStack == null) {
					tLightest.mAmount = 0;
					UT.Entities.exhaust(aPlayer);
					return 500;
				}
				if (ST.add(aPlayer, tOutputStack)) {
					UT.Entities.exhaust(aPlayer, 0.1F * tOutputStack.getCount());
					tLightest.mAmount -= OP.scrapGt.mAmount * tOutputStack.getCount();
					return 1000 * tOutputStack.getCount();
				}
				return 0;
			}
		}
		return super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
	}
	
	@Override
	public boolean onPlaced(ItemStack aStack, Player aPlayer, MultiTileEntityContainer aMTEContainer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		mTemperature = WD.envTemp(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
		return T;
	}
	
	@Override
	public boolean onTickCheck(long aTimer) {
		return super.onTickCheck(aTimer) || mDisplayedHeight != oDisplayedHeight || mDisplayedFluid != oDisplayedFluid;
	}
	
	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		oDisplayedFluid = mDisplayedFluid;
		oDisplayedHeight = mDisplayedHeight;
	}
	
	@Override
	public IPacket getClientDataPacket(boolean aSendAll) {
		if (aSendAll) return getClientDataPacketByteArray(T, mDisplayedHeight, UT.Code.toByteS(mDisplayedFluid, 0), UT.Code.toByteS(mDisplayedFluid, 1), (byte)UT.Code.getR(mRGBa), (byte)UT.Code.getG(mRGBa), (byte)UT.Code.getB(mRGBa), (byte)(mMeltDown ? 1 : 0));
		if (mDisplayedFluid != oDisplayedFluid) return getClientDataPacketByteArray(F, mDisplayedHeight, UT.Code.toByteS(mDisplayedFluid, 0), UT.Code.toByteS(mDisplayedFluid, 1));
		return getClientDataPacketByteArray(F, mDisplayedHeight);
	}
	
	@Override
	public boolean receiveDataByteArray(byte[] aData, INetworkHandler aNetworkHandler) {
		mDisplayedHeight = aData[0];
		if (aData.length >= 3) mDisplayedFluid = UT.Code.combine(aData[1], aData[2]);
		if (aData.length >= 6) mRGBa = UT.Code.getRGBInt(new short[] {UT.Code.unsignB(aData[3]), UT.Code.unsignB(aData[4]), UT.Code.unsignB(aData[5])});
		if (aData.length >= 7) mMeltDown = (aData[6] != 0);
		return T;
	}
	
	@Override
	public int getRenderPasses2(Block aBlock, boolean[] aShouldSideBeRendered) {
		short[] tRGBaArray = UT.Code.getRGBaArray(mRGBa);
		boolean tGlow = F;
		if (mMeltDown) {
			tRGBaArray[0] = UT.Code.bind8(tRGBaArray[0]*2+50);
			tRGBaArray[1] = UT.Code.bind8(tRGBaArray[1]*2+50);
			tRGBaArray[2] = UT.Code.bind8(tRGBaArray[2]/2+50);
			tGlow = T;
		} else {
			tGlow = mMaterial.contains(TD.Properties.GLOWING);
		}
		mTexture = BlockTextureDefault.get(mMaterial, OP.blockSolid, tRGBaArray, tGlow);
		
		if (UT.Code.exists(mDisplayedFluid, OreDictMaterial.MATERIAL_ARRAY)) {
			mTextureMolten = OreDictMaterial.MATERIAL_ARRAY[mDisplayedFluid].getTextureMolten();
			} else {
			mTextureMolten = BlockTextureDefault.get(MT.NULL, OP.blockRaw, CA_GRAY_64, F);
		}
		return 6;
	}
	
	@Override
	public boolean setBlockBounds2(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		switch(aRenderPass) {
		case  0: box(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[14], PX_N[ 0], PX_N[ 0]); return T;
		case  1: box(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[14]); return T;
		case  2: box(aBlock, PX_P[14], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
		case  3: box(aBlock, PX_P[ 0], PX_P[ 0], PX_P[14], PX_N[ 0], PX_N[ 0], PX_N[ 0]); return T;
		case  4: box(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], PX_N[14], PX_N[ 0]); return T;
		case  5: box(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 0], PX_N[ 0], 0.125F+(UT.Code.unsignB(mDisplayedHeight) / 292.571428F), PX_N[ 0]); return T;
		}
		return F;
	}
	
	private ITexture mTexture, mTextureMolten;
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		switch(aRenderPass) {
		case  0: case  2: return SIDES_AXIS_Z[aSide]||aSide==SIDE_BOTTOM?null:mTexture;
		case  1: case  3: return SIDES_AXIS_X[aSide]||aSide==SIDE_BOTTOM?null:mTexture;
		case  4: return SIDES_VERTICAL[aSide]?mTexture:null;
		case  5: return mDisplayedHeight != 0 && SIDES_TOP[aSide]?mTextureMolten:null;
		}
		return mTexture;
	}
	
	@Override
	public void onEntityCollidedWithBlock(Entity aEntity) {
		if (UT.Entities.applyTemperatureDamage(aEntity, mTemperature, 1, 10.0F) && mTemperature > 320) {
			if (aEntity instanceof LivingEntity && !((LivingEntity)aEntity).isAlive()) {
				if (aEntity instanceof Villager || aEntity instanceof Witch) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(2*U, MT.SoylentGreen)), C+37);
				} else if (aEntity instanceof SnowGolem) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(4*U, MT.Snow)), C-10);
				} else if (aEntity instanceof IronGolem) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(4*U, MT.Fe)), WD.envTemp(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()));
				} else if (aEntity instanceof Skeleton) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(1*U, (aEntity instanceof WitherSkeleton) ? MT.BoneWither : MT.Bone), (aEntity instanceof WitherSkeleton) ? OM.stack(1*U, MT.Coal) : null), WD.envTemp(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()));
				} else if (aEntity instanceof Zombie) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(1*U, MT.MeatRotten)), WD.envTemp(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()));
				} else if (aEntity instanceof MushroomCow || aEntity instanceof Cow || aEntity instanceof Horse) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(3*U, MT.MeatRaw)), C+37);
				} else if (aEntity instanceof Pig || aEntity instanceof Sheep || aEntity instanceof Wolf || aEntity instanceof Squid) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(2*U, MT.MeatRaw)), C+37);
				} else if (aEntity instanceof Chicken || aEntity instanceof Ocelot || aEntity instanceof Spider || aEntity instanceof Silverfish) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(1*U, MT.MeatRaw)), C+37);
				} else if (aEntity instanceof Creeper) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(1*U, MT.Gunpowder)), C+20);
				} else if (aEntity instanceof EnderMan) {
					addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(1*U, MT.EnderPearl)), C+20);
				} else if (aEntity instanceof Player) {
					if ("GregoriusT".equalsIgnoreCase(aEntity.getName().getString())) for (int i = 0; i < 16; i++) addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(1*U, MT.Tc)), C+20);
				}
			}
		}
	}
	
	@Override public AABB getCollisionBoundingBoxFromPool() {return box(0.125, 0.125, 0.125, 0.875, 0.875, 0.875);}
	@Override public boolean addDefaultCollisionBoxToList() {return F;}
	
	@Override
	public void addCollisionBoxesToList2(AABB aAABB, List<AABB> aList, Entity aEntity) {
		box(aAABB, aList, PX_P[14], PX_P[ 1], PX_P[ 1], PX_N[ 1], PX_N[ 1], PX_N[ 1]);
		box(aAABB, aList, PX_P[ 1], PX_P[ 1], PX_P[14], PX_N[ 1], PX_N[ 1], PX_N[ 1]);
		box(aAABB, aList, PX_P[ 1], PX_P[ 1], PX_P[ 1], PX_N[14], PX_N[ 1], PX_N[ 1]);
		box(aAABB, aList, PX_P[ 1], PX_P[ 1], PX_P[ 1], PX_N[ 1], PX_N[ 1], PX_N[14]);
		box(aAABB, aList, PX_P[ 1], PX_P[ 1], PX_P[ 1], PX_N[ 1], PX_N[14], PX_N[ 1]);
	}
	
	@Override
	public boolean checkObstruction(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		return SIDES_BOTTOM_HORIZONTAL[aSide] && super.checkObstruction(aPlayer, aSide, aHitX, aHitY, aHitZ);
	}
	
	@Override public float getSurfaceSize           (byte aSide) {return 1.0F;}
	@Override public float getSurfaceSizeAttachable (byte aSide) {return 1.0F;}
	@Override public float getSurfaceDistance       (byte aSide) {return 0.0F;}
	@Override public boolean isSurfaceSolid         (byte aSide) {return !SIDES_TOP[aSide];}
	@Override public boolean isSurfaceOpaque2       (byte aSide) {return !SIDES_TOP[aSide];}
	@Override public boolean isSideSolid2           (byte aSide) {return !SIDES_TOP[aSide];}
	
	@Override public long getGibblValue(byte aSide) {return UT.Code.divup(OM.total(mContent)*1000, U9);}
	@Override public long getGibblMax  (byte aSide) {return UT.Code.divup(MAX_AMOUNT*1000, U9);}
	
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	@Override public boolean allowCovers(byte aSide) {return F;}
	
	@Override public ItemStack[] getDefaultInventory(CompoundTag aNBT) {return new ItemStack[1];}
	@Override public int[] getAccessibleSlotsFromSide2(byte aSide) {return UT.Code.getAscendingArray(1);}
	@Override public boolean canInsertItem2(int aSlot, ItemStack aStack, byte aSide) {return SIDES_TOP[aSide] && !slotHas(0);}
	@Override public boolean canExtractItem2(int aSlot, ItemStack aStack, byte aSide) {return F;}
	@Override public int getMaxStackSize() {return 64;}
	
	public static final List<TagData> ENERGYTYPES = new ArrayListNoNulls<>(F, TD.Energy.KU, TD.Energy.HU, TD.Energy.CU, TD.Energy.VIS_IGNIS);
	
	@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return !aEmitting && ENERGYTYPES.contains(aEnergyType);}
	@Override public boolean isEnergyCapacitorType(TagData aEnergyType, byte aSide) {return ENERGYTYPES.contains(aEnergyType);}
	@Override public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return ENERGYTYPES.contains(aEnergyType);}
	@Override public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {if (aDoInject) {if (aEnergyType == TD.Energy.KU) {if (aSize*aAmount > 0 && WD.oxygen(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ())) addMaterialStacks(new ArrayListNoNulls<>(F, OM.stack(Math.min(MAX_AMOUNT-OM.total(mContent), aSize*aAmount*U1000), MT.Air)), mTemperature);} else if (aEnergyType == TD.Energy.CU) mEnergy -= Math.abs(aAmount * aSize); else mEnergy += Math.abs(aAmount * aSize);} return aAmount;}
	@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return Long.MAX_VALUE - mEnergy;}
	@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 1;}
	@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 2048;}
	@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}
	@Override public Collection<TagData> getEnergyTypes(byte aSide) {return ENERGYTYPES;}
	
	@Override public float getBlockHardness() {return mDisplayedHeight != 0 ? super.getBlockHardness() * 100 : super.getBlockHardness();}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.smeltery";}
}
