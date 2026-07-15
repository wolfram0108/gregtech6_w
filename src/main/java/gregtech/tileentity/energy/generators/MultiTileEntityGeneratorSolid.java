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
 */

package gregtech.tileentity.energy.generators;

import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetCollisionBoundingBoxFromPool;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_OnEntityCollidedWithBlock;
import gregapi.code.TagData;
import gregapi.data.FM;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.data.TD;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.machines.ITileEntityRunningActively;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class MultiTileEntityGeneratorSolid extends TileEntityBase09FacingSingle implements ITileEntityEnergy, ITileEntityRunningActively, IMTE_GetCollisionBoundingBoxFromPool, IMTE_OnEntityCollidedWithBlock {
	private static int FLAME_RANGE = 3;
	
	protected short mEfficiency = 10000;
	protected long mEnergy = 0, mRate = 1;
	protected boolean mBurning = F, oBurning = F;
	protected TagData mEnergyTypeEmitted = TD.Energy.HU;
	protected RecipeMap mRecipes = FM.Furnace;
	protected Recipe mLastRecipe = null;
	protected ItemStack mOutput1 = null;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		mEnergy = aNBT.getLongOr(NBT_ENERGY, 0L);
		mBurning = aNBT.getBooleanOr(NBT_ACTIVE, false);
		mOutput1 = ST.load(aNBT, NBT_INV_OUT + ".1");
		if (aNBT.contains(NBT_OUTPUT)) mRate = aNBT.getLongOr(NBT_OUTPUT, 0L);
		if (aNBT.contains(NBT_FUELMAP)) mRecipes = RecipeMap.RECIPE_MAPS.get(aNBT.getString(NBT_FUELMAP));
		if (aNBT.contains(NBT_EFFICIENCY)) mEfficiency = (short)UT.Code.bind_(0, 10000, aNBT.getShortOr(NBT_EFFICIENCY, (short)0));
		if (aNBT.contains(NBT_ENERGY_EMITTED)) mEnergyTypeEmitted = TagData.createTagData(aNBT.getStringOr(NBT_ENERGY_EMITTED, ""));
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		UT.NBT.setNumber(aNBT, NBT_ENERGY, mEnergy);
		UT.NBT.setBoolean(aNBT, NBT_ACTIVE, mBurning);
		ST.save(aNBT, NBT_INV_OUT + ".1", mOutput1);
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN     + LH.get(LH.RECIPES)        + ": " + Chat.WHITE + LH.get(mRecipes.mNameInternal));
		aList.add(LH.getToolTipEfficiency(mEfficiency));
		LH.addEnergyToolTips(this, aList, null, mEnergyTypeEmitted, null, LH.get(LH.FACE_TOP));
		aList.add(Chat.ORANGE   + LH.get(LH.REQUIREMENT_AIR_IN_FRONT));
		aList.add(Chat.ORANGE   + LH.get(LH.REQUIREMENT_EMPTY_ASHES) + " (" + LH.get(LH.FACE_FRONT) + ")");
		aList.add(Chat.ORANGE   + LH.get(LH.REQUIREMENT_IGNITE_FIRE) + " (" + LH.get(LH.FACE_FRONT) + ")");
		aList.add(Chat.ORANGE   + LH.get(LH.NO_GUI_CLICK_TO_INVENTORY) + " (" + LH.get(LH.FACE_FRONT) + ")");
		aList.add(Chat.DRED     + LH.get(LH.HAZARD_FIRE) + " ("+(FLAME_RANGE+1)+"m)");
		aList.add(Chat.DRED     + LH.get(LH.HAZARD_CONTACT) + " (" + LH.get(LH.FACE_TOP) + ")");
		aList.add(Chat.DGRAY    + LH.get(LH.TOOL_TO_REMOVE_SHOVEL));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public void onTick2(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide) {
			if (mBurning) {
				if (mEnergy >= mRate) {
					ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyTypeEmitted, 1, Math.min(mRate, mEnergy), this);
					mEnergy -= mRate;
					if (mEfficiency < 1 || rng(mEfficiency) == 0) {
						WD.fire(level, getBlockPos().getX()-FLAME_RANGE+rng(2*FLAME_RANGE+1), getBlockPos().getY()-1+rng(2+FLAME_RANGE), getBlockPos().getZ()-FLAME_RANGE+rng(2*FLAME_RANGE+1), T);
					}
				}
				if (mEnergy < mRate * 2) {
					WD.burn(level, getOffset(mFacing, 1), T, T);
					if (addStackToSlot(1, mOutput1)) mOutput1 = null;
					if (mOutput1 == null && slotHas(0) && !WD.hasCollide(level, getOffsetX(mFacing), getOffsetY(mFacing), getOffsetZ(mFacing)) && !WD.getMaterial(getBlockAtSide(mFacing)).isLiquid() && WD.oxygen(level, getOffsetX(mFacing), getOffsetY(mFacing), getOffsetZ(mFacing))) {
						if (IL.RC_Firestone_Refined.equal(slot(0), T, T)) {
							ItemStack tStack = ST.container(slot(0), F);
							if (ST.invalid(tStack)) {
								// Just dump the empty Firestone to the Output. This should not happen, unless you insert an empty Firestone.
								if (addStackToSlot(1, slot(0))) slotKill(0);
							} else if (ST.invalid(ST.container(tStack, F))) {
								// 80% of whatever Heat Energy you get from Lava. This is over 10 times the normal Firestone Furnace burn Value.
								mEnergy += 800L * EU_PER_LAVA;
								// Prevent using up the Firestone entirely.
								slotKill(0);
								mOutput1 = tStack;
							} else {
								// 80% of whatever Heat Energy you get from Lava. This is over 10 times the normal Firestone Furnace burn Value.
								mEnergy += 800L * EU_PER_LAVA;
								// Continue using the Firestone.
								slot(0, tStack);
							}
						} else if (IL.RC_Firestone_Cracked.equal(slot(0), T, T)) {
							ItemStack tStack = ST.container(slot(0), F);
							if (ST.invalid(tStack)) {
								// Just dump the empty Firestone to the Output. This should not happen, unless you insert an empty Firestone.
								if (addStackToSlot(1, slot(0))) slotKill(0);
							} else if (ST.invalid(ST.container(tStack, F))) {
								// Less Power for the broken Firestone, so 60%.
								mEnergy += 600L * EU_PER_LAVA;
								// Prevent using up the Firestone entirely.
								slotKill(0);
								mOutput1 = tStack;
							} else {
								// Less Power for the broken Firestone, so 60%.
								mEnergy += 600L * EU_PER_LAVA;
								// Continue using the Firestone.
								slot(0, tStack);
								// Cracked Firestones cause Fire to be released way more often.
								WD.fire(level, getBlockPos().getX()-FLAME_RANGE+rng(2*FLAME_RANGE+1), getBlockPos().getY()-1+rng(2+FLAME_RANGE), getBlockPos().getZ()-FLAME_RANGE+rng(2*FLAME_RANGE+1), T);
							}
						} else {
							Recipe tRecipe = mRecipes.findRecipe(this, mLastRecipe, T, Long.MAX_VALUE, null, ZL_FS, slot(0));
							if (tRecipe != null && tRecipe.isRecipeInputEqual(T, F, ZL_FS, slot(0))) {
								mLastRecipe = tRecipe;
								ItemStack[] tOutputs = tRecipe.getOutputs();
								if (tOutputs.length > 0) mOutput1 = ST.copy(tOutputs[0]);
								mEnergy += UT.Code.units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, F);
								removeAllDroppableNullStacks();
							}
						}
					}
				}
				// Out of Fuel I guess.
				if (mEnergy < mRate) mBurning = F;
			} else {
				// Something burning in front of it? Lets ignite!
				if (rng(200) == 0 && WD.flaming(level, getOffsetX(mFacing), getOffsetY(mFacing), getOffsetZ(mFacing))) {
					mBurning = T;
				}
			}
			// Out of Fuel I guess.
			if (mEnergy < 0) mEnergy = 0;
		} else {
			if (mBurning && rng(4) == 0) spawnBurningParticles(getBlockPos().getX()+0.5+OFFX[mFacing]*0.55+(SIDES_AXIS_X[mFacing]?0:RNGSUS.nextFloat()*0.6F-0.3F), getBlockPos().getY()+RNGSUS.nextFloat()*0.375F, getBlockPos().getZ()+0.5+OFFZ[mFacing]*0.55+(SIDES_AXIS_Z[mFacing]?0:RNGSUS.nextFloat()*0.6F-0.3F));
		}
	}
	
	@Override public boolean attachCoversFirst(byte aSide) {return F;}
	
	@Override
	public boolean onBlockActivated3(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (aSide != mFacing) return F;
		if (isServerSide()) {
			ItemStack aStack = aPlayer.getMainHandItem();
			if (aStack == null) {
				if (slotHas(1)) {
					aPlayer.getInventory().setItem(aPlayer.getInventory().getSelectedSlot(), slot(1));
					slotKill(1);
					if (mBurning) UT.Entities.applyHeatDamage(aPlayer, Math.max(1.0F, Math.min(5.0F, mRate / 20.0F)));
					return T;
				}
				if (!mBurning && slotHas(0)) {
					aPlayer.getInventory().setItem(aPlayer.getInventory().getSelectedSlot(), slot(0));
					slotKill(0);
					return T;
				}
			} else if (!slotHas(0)) {
				if (canInsertItem2(0, aStack, SIDE_INSIDE)) {
					slot(0, aStack);
					aPlayer.getInventory().setItem(aPlayer.getInventory().getSelectedSlot(), null);
					return T;
				}
			} else if (ST.equal(aStack, slot(0))) {
				int tDifference = Math.min(aStack.getCount(), slot(0).getMaxStackSize() - slot(0).getCount());
				aStack.setCount(aStack.getCount()-(tDifference));
				slot(0).setCount(slot(0).getCount()+(tDifference));
				return T;
			} else if (ST.equal(aStack, slot(1))) {
				int tDifference = Math.min(slot(1).getCount(), aStack.getMaxStackSize() - aStack.getCount());
				aStack.setCount(aStack.getCount()+(tDifference));
				slot(1).setCount(slot(1).getCount()-(tDifference));
				removeAllDroppableNullStacks();
				if (mBurning) UT.Entities.applyHeatDamage(aPlayer, Math.max(1.0F, Math.min(5.0F, mRate / 20.0F)));
				return T;
			}
		}
		return T;
	}
	
	@Override
	public long onToolClick2(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		long rReturn = super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
		if (rReturn > 0) return rReturn;
		
		if (isClientSide()) return 0;
		
		if (aTool.equals(TOOL_igniter       ) && (aSide == mFacing || aPlayer == null)) {mBurning = T; return 10000;}
		if (aTool.equals(TOOL_extinguisher  ) && (aSide == mFacing || aPlayer == null)) {mBurning = F; return 10000;}
		if (aTool.equals(TOOL_shovel        ) &&  aSide == mFacing && slotHas(1)) {
			long rDamage = 1000 * slot(1).getCount();
			ST.give(aPlayer, slot(1), level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
			slotKill(1);
			return rDamage;
		}
		
		return 0;
	}
	
	@Override
	public boolean onTickCheck(long aTimer) {
		return mBurning != oBurning || super.onTickCheck(aTimer);
	}
	
	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		oBurning = mBurning;
	}
	
	@Override
	public void setVisualData(byte aData) {
		mBurning = ((aData & 1) != 0);
	}
	
	@Override public byte getVisualData() {return (byte)(mBurning?1:0);}
	@Override public byte getDefaultSide() {return SIDE_FRONT;}
	@Override public boolean[] getValidSides() {return mBurning ? SIDES_THIS[mFacing] : SIDES_HORIZONTAL;}
	
	@Override public void onEntityCollidedWithBlock(Entity aEntity) {if (mBurning) UT.Entities.applyHeatDamage(aEntity, Math.min(10.0F, mRate / 10.0F));}
	@Override public AABB getCollisionBoundingBoxFromPool() {return box(0, 0, 0, 1, 0.875, 1);}
	
	// Inventory Stuff
	private static final int[] ACCESSABLE_SLOTS = new int[] {0, 1};
	@Override public int[] getAccessibleSlotsFromSide2(byte aSide) {return aSide == mFacing ? ZL_INTEGER : ACCESSABLE_SLOTS;}
	@Override public boolean canInsertItem2 (int aSlot, ItemStack aStack, byte aSide) {return aStack != null && aSlot == 0 && aSide != mFacing && (mRecipes.containsInput(aStack, this, NI) || IL.RC_Firestone_Refined.equal(aStack, T, T) || IL.RC_Firestone_Cracked.equal(aStack, T, T));}
	@Override public boolean canExtractItem2(int aSlot, ItemStack aStack, byte aSide) {return aStack != null && aSlot == 1 && aSide != mFacing;}
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	@Override public ItemStack[] getDefaultInventory(CompoundTag aNBT) {return new ItemStack[2];}
	
	@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEmitting && aEnergyType == mEnergyTypeEmitted;}
	@Override public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return SIDES_TOP[aSide] && super.isEnergyEmittingTo(aEnergyType, aSide, aTheoretical);}
	@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return Math.min(mRate, mEnergy);}
	@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return mRate;}
	@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return mRate;}
	@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return mRate;}
	@Override public Collection<TagData> getEnergyTypes(byte aSide) {return mEnergyTypeEmitted.AS_LIST;}
	
	@Override public boolean getStateRunningPassively() {return mBurning;}
	@Override public boolean getStateRunningPossible() {return mBurning || (mOutput1 == null && slotHas(0) && !WD.hasCollide(level, getOffsetX(mFacing), getOffsetY(mFacing), getOffsetZ(mFacing)) && !WD.getMaterial(getBlockAtSide(mFacing)).isLiquid() && WD.oxygen(level, getOffsetX(mFacing), getOffsetY(mFacing), getOffsetZ(mFacing)));}
	@Override public boolean getStateRunningActively() {return mBurning;}
	
	@Override public float getBlockHardness() {return mBurning ? super.getBlockHardness() * 16 : super.getBlockHardness();}
	
	protected void spawnBurningParticles(double aX, double aY, double aZ) {
		level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, aX, aY, aZ, 0, 0, 0);
		level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, aX, aY, aZ, 0, 0, 0);
	}
}
