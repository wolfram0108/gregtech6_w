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

package gregapi.tileentity.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

import enviromine.handlers.EM_StatusManager;
import enviromine.trackers.EnviroDataTracker;
import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.code.ItemNBT;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.data.MD;
import gregapi.fluid.FluidTankGT;
import gregapi.item.multiitem.food.FoodStatFluid;
import gregapi.tileentity.ITileEntityConnectedTank;
import gregapi.tileentity.base.TileEntityBase07Paintable;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import ic2.api.crops.ICropTile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.capability.IFluidHandler;
import squeek.applecore.api.food.FoodValues;
import thaumcraft.common.tiles.TileCrucible;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class TileEntityBase08FluidContainer extends TileEntityBase07Paintable implements ITileEntityConnectedTank, IMTE_GetMaxStackSize, IMTE_OnlyPlaceableWhenSneaking, IMTE_OnItemRightClick, IMTE_OnItemUseFirst, IMTE_AddToolTips, IFluidContainerItem {
	public FluidTankGT mTank = new FluidTankGT(1000);
	public boolean mLiquidProof = T, mGasProof = F, mAcidProof = F, mPlasmaProof = F, mMagicProof = F;
	public long mTemperatureMax = 0;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_GASPROOF)) mGasProof = aNBT.getBoolean(NBT_GASPROOF).orElse(false);
		if (aNBT.contains(NBT_ACIDPROOF)) mAcidProof = aNBT.getBoolean(NBT_ACIDPROOF).orElse(false);
		if (aNBT.contains(NBT_MAGICPROOF)) mMagicProof = aNBT.getBoolean(NBT_MAGICPROOF).orElse(false);
		if (aNBT.contains(NBT_LIQUIDPROOF)) mLiquidProof = aNBT.getBoolean(NBT_LIQUIDPROOF).orElse(false);
		if (aNBT.contains(NBT_PLASMAPROOF)) mPlasmaProof = aNBT.getBoolean(NBT_PLASMAPROOF).orElse(false);
		if (aNBT.contains(NBT_TEMPERATURE)) mTemperatureMax = aNBT.getLong(NBT_TEMPERATURE).orElse(0L); else mTemperatureMax = mMaterial.mMeltingPoint - 50;
		if (aNBT.contains(NBT_TANK_CAPACITY)) mTank.setCapacity(aNBT.getLong(NBT_TANK_CAPACITY).orElse(0L));
		mTank.readFromNBT(aNBT, NBT_TANK);
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		mTank.writeToNBT(aNBT, NBT_TANK);
	}
	
	@Override
	public CompoundTag writeItemNBT2(CompoundTag aNBT) {
		mTank.writeToNBT(aNBT, NBT_TANK);
		if (isClientSide() && !mTank.isEmpty()) aNBT.put("display", UT.NBT.makeString(aNBT.getCompoundOrEmpty("display"), "Name", FL.name(mTank, T)));
		return super.writeItemNBT2(aNBT);
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.CYAN + mTank.contentcap());
		if (mTank.has(250) && isDrinkable()) {
			FoodStatFluid.INSTANCE.addAdditionalToolTips(aStack.getItem(), aList, aStack, aF3_H);
			if (aStack.getCount() != 1) aList.add(LH.Chat.RED + LH.get(LH.REQUIREMENT_UNSTACKED));
		}
		aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_HEATPROOF) + LH.Chat.WHITE + mTemperatureMax + LH.Chat.RED + " K");
		if (mLiquidProof    ) aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_LIQUIDPROOF));
		if (mGasProof       ) aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_GASPROOF));
		if (mAcidProof      ) aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_ACIDPROOF));
		if (mPlasmaProof    ) aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_PLASMAPROOF));
		if (mMagicProof     ) aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_MAGICPROOF));
	}
	
	@Override
	public void onTick2(long aTimer, boolean aIsServerSide) {
		super.onTick2(aTimer, aIsServerSide);
		if (aIsServerSide && canFillWithRain() && SERVER_TIME % 600 == 10 && level.isRaining() && getRainOffset(0, 1, 0)) {
			Biome tBiome = getBiome();
			if (WD.rainfall(tBiome) > 0 && tBiome.getBaseTemperature() >= 0.2) {
				Block tInFront = getBlockAtSide(SIDE_TOP);
				if (!WD.liquid(tInFront) && !WD.sideSolid(tInFront, level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), FORGE_DIR_OPPOSITES[SIDE_TOP]) && !WD.sideSolid(tInFront, level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), FORGE_DIR[SIDE_TOP])) {
					mTank.fill(FL.Water.make((long)Math.max(1, WD.rainfall(tBiome)*100) * (level.isThundering()?2:1)), T);
				}
			}
		}
	}
	
	@Override
	public long onToolClick2(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		long rReturn = super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
		if (rReturn > 0 || isClientSide()) return rReturn;
		if (aTool.equals(TOOL_plunger)) {
			return GarbageGT.trash(mTank, 1000);
		}
		if (aTool.equals(TOOL_magnifyingglass)) {
			if (aChatReturn != null) aChatReturn.add(mTank.contentcap());
			return 1;
		}
		return 0;
	}
	
	@Override
	public boolean onBlockActivated3(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isClientSide()) return T;
		
		ItemStack aStack = ST.n(aPlayer.getMainHandItem()), tStack = ST.container(ST.amount(1, aStack), T); // F15-граница: движок EMPTY -> GT6 null
		FluidStack tFluid = FL.getFluid(ST.amount(1, aStack), T);
		if (aStack != null && isFluidAllowed(tFluid) && mTank.fillAll(tFluid)) {
			aStack.setCount(aStack.getCount()-1);
			ST.give(aPlayer, tStack, T);
			return T;
		}
		if (aStack != null) if ((tStack = FL.fill(mTank, ST.amount(1, aStack), T, T, T, T)) != null) {
			aStack.setCount(aStack.getCount()-1);
			ST.give(aPlayer, tStack, T);
			return T;
		}
		
		if (isDrinkable() && canDrinkFromSide(aSide)) {
			aStack = toStack();
			if (aStack == null) return T;
			if (UT.Entities.isCreative(aPlayer) || aPlayer.getFoodData().needsFood() || FoodStatFluid.INSTANCE.alwaysEdible(aStack.getItem(), aStack, aPlayer)) {
				switch(FoodStatFluid.INSTANCE.getFoodAction(aStack.getItem(), aStack)) {
				case EAT : UT.Sounds.send(SFX.MC_EAT  , this, F); break; // было "case eat" (1.7.10 enum-конвенция) -> UPPER_CASE (UseAnim.java:16)
				default  : UT.Sounds.send(SFX.MC_DRINK, this, F); break;
				}
				mTank.remove(250);
				// было Item.onEaten(ItemStack,World,EntityPlayer) (1.7.10) -> neo Item.finishUsingItem(ItemStack,Level,LivingEntity)
				// (Item.java:232), тот же приём возврата-как-statement (результат отбрасывался и там, и там).
				aStack.getItem().finishUsingItem(aStack, level, aPlayer);
			}
		}
		return T;
	}
	
	@Override public boolean onlyPlaceableWhenSneaking() {return T;}
	@Override public boolean canDrop(int aInventorySlot) {return F;}
	
	@Override
	public FluidStack getFluid(ItemStack aStack) {
		return mTank.getFluid();
	}

	@Override
	public int getCapacity(ItemStack aStack) {
		return mTank.getCapacity();
	}

	@Override
	public int fill(ItemStack aStack, FluidStack aFluid, boolean aDoFill) {
		if (!isFluidAllowed(aFluid) || aStack.getCount() != 1) return 0;
		int tFilled = mTank.fill(aFluid, aDoFill);
		if (tFilled > 0 && aDoFill) UT.NBT.set(aStack, writeItemNBT(ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make()));
		return tFilled;
	}
	
	@Override
	public FluidStack drain(ItemStack aStack, int aMaxDrain, boolean aDoDrain) {
		if (aStack.getCount() != 1) return NF;
		FluidStack tDrained = mTank.drain(aMaxDrain, aDoDrain);
		if (tDrained != NF && aDoDrain) UT.NBT.set(aStack, writeItemNBT(ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make()));
		return tDrained;
	}
	
	@Override
	public boolean onItemUseFirst(MultiTileEntityItemInternal aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		if (aWorld.isClientSide() || aPlayer == null || !(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || aStack.getCount() != 1) return F;
		if (canWaterCrops()) {
			FluidStack mFluid = aItem.getFluid(aStack);
			if (FL.water(mFluid)) {
				Block aBlock = WD.block(aWorld, aX, aY, aZ);
				int aMeta = WD.meta(aWorld, aX, aY, aZ);
				
				if (aBlock instanceof CauldronBlock) {
					if (aMeta >= 3 || mFluid.getAmount() < 334) return F;
					if (mFluid.getAmount() >= 1000 && aMeta <= 0) {
						aItem.drain(aStack, 1000, T);
						WD.set(aWorld, aX, aY, aZ, aBlock, aMeta+3, 3);
					} else if (mFluid.getAmount() >= 667 && aMeta <= 1) {
						aItem.drain(aStack, 667, T);
						WD.set(aWorld, aX, aY, aZ, aBlock, aMeta+2, 3);
					} else if (aMeta <= 2) {
						aItem.drain(aStack, 334, T);
						WD.set(aWorld, aX, aY, aZ, aBlock, aMeta+1, 3);
					}
					UT.Sounds.send(SFX.MC_LIQUID_WATER, level, getCoords());
					return T;
				}
				
				if (IL.GrC_Paddy.exists() && mFluid.getAmount() >= 10) {
					if (IL.GrC_Paddy.block() == aBlock) {
						int tIncrement = Math.min(7-aMeta, mFluid.getAmount()/10);
						if (tIncrement > 0) {
							aItem.drain(aStack, tIncrement*10, T);
							WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMeta+tIncrement, 3, F);
							UT.Sounds.send(SFX.MC_LIQUID_WATER, aWorld, aX, aY, aZ);
						}
						return T;
					}
					if (IL.GrC_Paddy.block() == WD.block(aWorld, aX, aY-1, aZ)) {
						int tMeta = WD.meta(aWorld, aX, aY-1, aZ);
						int tIncrement = Math.min(7-tMeta, mFluid.getAmount()/10);
						if (tIncrement > 0) {
							aItem.drain(aStack, tIncrement*10, T);
							WD.set(aWorld, aX, aY-1, aZ, WD.block(aWorld, aX, aY-1, aZ), tMeta+tIncrement, 3, F);
							UT.Sounds.send(SFX.MC_LIQUID_WATER, aWorld, aX, aY-1, aZ);
						}
						return T;
					}
				}
				
				BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, F);
				
				try {if (tTileEntity instanceof ICropTile) {
					int tHydration = ((ICropTile)tTileEntity).getHydrationStorage();
					int tDrained = Math.min((200-tHydration)/10, mFluid.getAmount());
					if (tDrained > 0) {
						aItem.drain(aStack, tDrained, T);
						((ICropTile)tTileEntity).setHydrationStorage(tHydration + tDrained*10);
						UT.Sounds.send(SFX.MC_LIQUID_WATER, aWorld, aX, aY, aZ);
					}
					return T;
				}} catch(Throwable e) {/**/}
				
				try {if (tTileEntity instanceof TileCrucible) {
					if (FL.water(mFluid) && FL.nonzero(aItem.drain(aStack, (int)FL.fill((IFluidHandler)tTileEntity, SIDE_TOP, FL.Water.make(mFluid.getAmount()), T), T))) {
						UT.Sounds.send(SFX.MC_LIQUID_WATER, aWorld, aX, aY, aZ);
					}
					return T;
				}} catch(Throwable e) {/**/}
			}
		}
		return F;
	}
	
	/** ADAPT-003 (согласованное отклонение, требование игрока 2026-07-23; журнал ADAPTATIONS.md): зачерпывание работает и СО СТАКОМ
	 *  пустых ёмкостей — тратится 1 из стака, полный уходит в свободный слот (ведро-механика vanilla).
	 *  Оригинальный гейт 1.7.10 (:275) требовал ровно 1 в руке; centrally здесь — накрывает все ёмкости. */
	private ItemStack scoopResult(Player aPlayer, ItemStack aStack, ItemStack aTarget) {
		if (aTarget != aStack && FL.getFluid(aTarget, T) != null) {
			aStack.shrink(1);
			ST.give(aPlayer, aTarget, T);
		}
		return aStack;
	}

	@Override
	public ItemStack onItemRightClick(MultiTileEntityItemInternal aItem, ItemStack aStack, Level aWorld, Player aPlayer) {
		if (canPickUpFluids() && aStack.getCount() >= 1) {
			ItemStack aTarget = aStack.getCount() == 1 ? aStack : ST.amount(1, aStack);
			HitResult tTarget = WD.getMOP(aWorld, aPlayer, T);
			// было World.canMineBlock(EntityPlayer,x,y,z) (1.7.10) -> neo Level.mayInteract(Entity,BlockPos) (Level.java:887)
			if (tTarget != null && tTarget.getType() == HitResult.Type.BLOCK && aWorld.mayInteract(aPlayer, ((BlockHitResult)tTarget).getBlockPos())) {
				Block tBlock = WD.block(aWorld, ((BlockHitResult)tTarget).getBlockPos().getX(), ((BlockHitResult)tTarget).getBlockPos().getY(), ((BlockHitResult)tTarget).getBlockPos().getZ());
				if (tBlock == Blocks.WATER || tBlock == Blocks.WATER) {
					if (WD.meta(aWorld, ((BlockHitResult)tTarget).getBlockPos().getX(), ((BlockHitResult)tTarget).getBlockPos().getY(), ((BlockHitResult)tTarget).getBlockPos().getZ()) == 0) {
						if (WD.infiniteWater(aWorld, ((BlockHitResult)tTarget).getBlockPos().getX(), ((BlockHitResult)tTarget).getBlockPos().getY(), ((BlockHitResult)tTarget).getBlockPos().getZ())) {
							aItem.fill(aTarget, FL.Water.make(1000), T);
						} else {
							if (aItem.fill(aTarget, FL.Water.make(1000), F) == 1000) {
								WD.set(aWorld, ((BlockHitResult)tTarget).getBlockPos().getX(), ((BlockHitResult)tTarget).getBlockPos().getY(), ((BlockHitResult)tTarget).getBlockPos().getZ(), NB, 0, 3);
								aItem.fill(aTarget, FL.Water.make(1000), T);
							}
						}
					}
					return scoopResult(aPlayer, aStack, aTarget);
				}
				if (tBlock == Blocks.LAVA || tBlock == Blocks.LAVA) {
					if (FL.drainable(aWorld, ((BlockHitResult)tTarget).getBlockPos()) != null && aItem.fill(aTarget, FL.Lava.make(1000), F) == 1000) { // F5 §6.2 — центр вместо «мета 0»
						WD.set(aWorld, ((BlockHitResult)tTarget).getBlockPos().getX(), ((BlockHitResult)tTarget).getBlockPos().getY(), ((BlockHitResult)tTarget).getBlockPos().getZ(), NB, 0, 3);
						aItem.fill(aTarget, FL.Lava.make(1000), T);
					}
					return scoopResult(aPlayer, aStack, aTarget);
				}
				if (tBlock == BlocksGT.River || WD.waterstream(tBlock)) {
					aItem.fill(aTarget, FL.Water.make(1000), T);
					return scoopResult(aPlayer, aStack, aTarget);
				}
				if (tBlock == BlocksGT.Ocean) {
					aItem.fill(aTarget, FL.Ocean.make(1000), T);
					return scoopResult(aPlayer, aStack, aTarget);
				}
				if (tBlock == BlocksGT.Swamp) {
					aItem.fill(aTarget, FL.Dirty_Water.make(1000), T);
					return scoopResult(aPlayer, aStack, aTarget);
				}
				if (tBlock instanceof IFluidBlock) {
					FluidStack tDrained = FL.drainable(aWorld, ((BlockHitResult)tTarget).getBlockPos()); // F5 §6.2 — центр
					if (tDrained != null && tDrained.getAmount() > 0 && aItem.fill(aTarget, tDrained, F) == tDrained.getAmount()) {
						// Forge fucked up the Fluid Draining Function, meaning if you insert true for doDrain it will ALWAYS return a null Fluid for the finite Fluid Blocks. That's why I take the result from the simulation instead of the actual draining.
						aItem.fill(aTarget, tDrained, T);
						FL.drainCell(aWorld, ((BlockHitResult)tTarget).getBlockPos()); // F5 §6.2 — центр
					}
					return scoopResult(aPlayer, aStack, aTarget);
				}
				
				// было tTarget.blockX/Y/Z += OFFX/Y/Z[sideHit] (сдвиг на соседний блок по стороне удара); neo BlockPos immutable -> переприсвоить BlockHitResult на relative(getDirection())
				tTarget = new BlockHitResult(tTarget.getLocation(), ((BlockHitResult)tTarget).getDirection(), ((BlockHitResult)tTarget).getBlockPos().relative(((BlockHitResult)tTarget).getDirection()), ((BlockHitResult)tTarget).isInside());
				tBlock = WD.block(aWorld, ((BlockHitResult)tTarget).getBlockPos().getX(), ((BlockHitResult)tTarget).getBlockPos().getY(), ((BlockHitResult)tTarget).getBlockPos().getZ());
				
				if (tBlock instanceof IFluidBlock) {
					FluidStack tDrained = FL.drainable(aWorld, ((BlockHitResult)tTarget).getBlockPos()); // F5 §6.2 — центр
					if (tDrained != null && tDrained.getAmount() > 0 && aItem.fill(aTarget, tDrained, F) == tDrained.getAmount()) {
						// Forge fucked up the Fluid Draining Function, meaning if you insert true for doDrain it will ALWAYS return a null Fluid for the finite Fluid Blocks. That's why I take the result from the simulation instead of the actual draining.
						aItem.fill(aTarget, tDrained, T);
						FL.drainCell(aWorld, ((BlockHitResult)tTarget).getBlockPos()); // F5 §6.2 — центр
					}
					return scoopResult(aPlayer, aStack, aTarget);
				}
			}
		}
		if (isDrinkable() && aStack.getCount() == 1 && (UT.Entities.isCreative(aPlayer) || aPlayer.getFoodData().needsFood() || FoodStatFluid.INSTANCE.alwaysEdible(aStack.getItem(), aStack, aPlayer))) {
			// было setItemInUse(ItemStack,int) (1.7.10) -> neo LivingEntity.startUsingItem(InteractionHand) (LivingEntity.java:3529),
			// длительность больше не параметр — берётся движком из Item.getUseDuration(ItemStack,LivingEntity) (Item.java:328);
			// getMaxItemUseDuration(aItem,aStack) ниже остаётся источником этого числа для будущего Item-хука (F13, вне этого шва).
			aPlayer.startUsingItem(InteractionHand.MAIN_HAND);
			return aStack;
		}
		return aStack;
	}
	
	public int getMaxItemUseDuration(MultiTileEntityItemInternal aItem, ItemStack aStack) {
		return isDrinkable() && aStack.getCount() == 1 ? Math.max(FoodStatFluid.INSTANCE.getFoodLevel(aStack.getItem(), aStack, null) * 8, 32) : 0;
	}
	
	public UseAnim getItemUseAction(MultiTileEntityItemInternal aItem, ItemStack aStack) {
		return isDrinkable() && aStack.getCount() == 1 ? FoodStatFluid.INSTANCE.getFoodAction(aStack.getItem(), aStack) : UseAnim.NONE; // было UseAnim.none (1.7.10 enum-конвенция) -> UPPER_CASE (UseAnim.java:15)
	}
	
	public ItemStack onEaten(MultiTileEntityItemInternal aItem, ItemStack aStack, Level aWorld, Player aPlayer) {
		if (!isDrinkable() || aStack.getCount() != 1) return aStack;
		
		int tFoodLevel = FoodStatFluid.INSTANCE.getFoodLevel(aStack.getItem(), aStack, aPlayer);
		
		if (tFoodLevel > 0) {
			if (FoodStatFluid.INSTANCE.useAppleCoreFunctionality(aStack.getItem(), aStack, aPlayer)) {
				// F10 foreign-gated impossible-1:1 (AppleCore ItemFoodProxy addStats): тот же неразрешимый 1:1 разрыв, что
				// gregapi/item/multiitem/MultiItemRandom.java (FoodData.func_151686_a убран целиком в neo,
				// компонентная FoodProperties-модель без per-item override hook) — тот же честный фолбэк.
				UT.Reflection.callConstructor("squeek.applecore.api.food.ItemFoodProxy", 0, null, T, aStack.getItem());
				aPlayer.getFoodData().eat(tFoodLevel, FoodStatFluid.INSTANCE.getSaturation(aStack.getItem(), aStack, aPlayer));
			} else {
				aPlayer.getFoodData().eat(tFoodLevel, FoodStatFluid.INSTANCE.getSaturation(aStack.getItem(), aStack, aPlayer));
			}
		}
		
		if (!aWorld.isClientSide() && MD.ENVM.mLoaded) {
			try {
				float tTemperature = FoodStatFluid.INSTANCE.getTemperature(aStack.getItem(), aStack, aPlayer) - C, tHydration = FoodStatFluid.INSTANCE.getHydration(aStack.getItem(), aStack, aPlayer);
				Object tTracker = EM_StatusManager.lookupTracker(aPlayer);
				if (tTracker != null && ((EnviroDataTracker)tTracker).bodyTemp >= 0) {
					((EnviroDataTracker)tTracker).bodyTemp += (tTemperature - ((EnviroDataTracker)tTracker).bodyTemp) * FoodStatFluid.INSTANCE.getTemperatureEffect(aStack.getItem(), aStack, aPlayer);
					if (tHydration > 0) ((EnviroDataTracker)tTracker).hydrate(tHydration); else if (tHydration < 0) ((EnviroDataTracker)tTracker).dehydrate(-tHydration);
				}
			} catch(Throwable e) {
				e.printStackTrace(ERR);
			}
		}
		
		FoodStatFluid.INSTANCE.onEaten(aStack.getItem(), aStack, aPlayer, F, T);
		
		mTank.remove(250);
		UT.NBT.set(aStack, writeItemNBT(ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make()));
		return aStack;
	}
	
	public FoodValues getFoodValues(MultiTileEntityItemInternal aItem, ItemStack aStack) {
		int tFoodLevel = FoodStatFluid.INSTANCE.getFoodLevel(aStack.getItem(), aStack, null);
		return tFoodLevel > 0 && isDrinkable() ? new squeek.applecore.api.food.FoodValues(tFoodLevel, FoodStatFluid.INSTANCE.getSaturation(aStack.getItem(), aStack, null)) : null;
	}
	
	@Override
	public int addFluidToConnectedTank(byte aSide, FluidStack aFluid, boolean aOnlyAddIfItAlreadyHasFluidsOfThatTypeOrIsDedicated) {
		if (aFluid == NF || !isFluidAllowed(aFluid) || (mTank.isEmpty() && aOnlyAddIfItAlreadyHasFluidsOfThatTypeOrIsDedicated)) return 0;
		return mTank.fill(aFluid, T);
	}
	
	@Override
	public int removeFluidFromConnectedTank(byte aSide, FluidStack aFluid, boolean aOnlyRemoveIfItCanRemoveAllAtOnce) {
		if (mTank.contains(aFluid) && mTank.has(aOnlyRemoveIfItCanRemoveAllAtOnce ? aFluid.getAmount() : 1)) return (int)mTank.remove(aFluid.getAmount());
		return 0;
	}
	
	@Override
	public long getAmountOfFluidInConnectedTank(byte aSide, FluidStack aFluid) {
		return mTank.contains(aFluid) ? mTank.amount() : 0;
	}
	
	public int tapFill(byte aSide, FluidStack aFluid, boolean aDoFill) {
		return SIDES_TOP[aSide] && isFluidAllowed(aFluid) ? mTank.fill(aFluid, aDoFill) : 0;
	}
	
	public int funnelFill(byte aSide, FluidStack aFluid, boolean aDoFill) {
		return SIDES_TOP[aSide] && isFluidAllowed(aFluid) ? mTank.fill(aFluid, aDoFill) : 0;
	}
	
	public boolean isDrinkable() {
		return this instanceof IMTE_GetFoodValues && mTank.has(250) && DrinksGT.REGISTER.containsKey(mTank.name());
	}
	
	public boolean isFluidAllowed(FluidStack aFluid) {
		return aFluid != null && !FL.powerconducting(aFluid) && (FL.gas(aFluid) ? mGasProof : mLiquidProof) && (mAcidProof || !FL.acid(aFluid)) && (mPlasmaProof || !FL.plasma(aFluid)) && (mMagicProof || !FL.magic(aFluid)) && FL.temperature(aFluid) <= mTemperatureMax;
	}
	
	@Override public byte getMaxStackSize(ItemStack aStack, byte aDefault) {return mTank.has() ? 1 : aDefault;}
	
	public boolean canWaterCrops() {return F;}
	public boolean canPickUpFluids() {return F;}
	public boolean canFillWithRain() {return F;}
	public boolean canDrinkFromSide(byte aSide) {return F;}
}
