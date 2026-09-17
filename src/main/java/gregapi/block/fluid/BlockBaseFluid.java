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

package gregapi.block.fluid;

import gregapi.block.IBlock;
import gregapi.block.IBlockOnHeadInside;
import gregapi.block.MaterialGas;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.item.IItemGT;
import gregapi.lang.LanguageHandler;
import gregapi.render.RendererBlockFluid;
import net.minecraft.world.item.ItemStack;
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/** @author Gregorius Techneticies
 *  Forge's BlockFluidFinite was removed, so the shared ancestor with BlockWaterlike is now reproduced
 *  once in BlockFluidBaseGT; only the API surface changed here, method bodies stay 1:1. */
public class BlockBaseFluid extends BlockFluidBaseGT implements IBlock, IItemGT, IBlockOnHeadInside {
	public static int FLUID_UPDATE_FLAGS = 2;
	
	public final String mNameInternal;
	public final int mFlammability, mAmountPerQuanta, mDensityDir;
	public final Fluid mFluid;
	// No longer final: FL.make needs registry components that only exist at server start, so
	// assignment is deferred to then.
	public FluidStack mQuanta;
	
	public BlockBaseFluid(String aNameInternal, FL aFluid, int aFlammability) {
		this(aNameInternal, aFluid.fluid(), aFlammability);
	}
	public BlockBaseFluid(String aNameInternal, FL aFluid, int aFlammability, Material aMaterial) {
		this(aNameInternal, aFluid.fluid(), aFlammability, aMaterial);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aFlammability) {
		// neo's Fluid no longer carries these Forge-only data methods (that data moved to FluidType),
		// so the FL.gas/FL.temperature centers reproduce the same information instead.
		this(aNameInternal, aFluid, aFlammability, FL.gas(aFluid) ? MaterialGas.instance : FL.temperature(aFluid) > 500 ? Material.lava : Material.water);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aFlammability, Material aMaterial) {
		this(aNameInternal, aFluid, 125, aFlammability, aMaterial);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aAmountPerQuanta, int aFlammability, Material aMaterial) {
		// neo Block Properties are immutable and must be built before super(), so resistance is computed from
		// the aFluid parameter directly, not mFluid (not yet assigned); map color and role are set explicitly (BlockFluidBaseGT).
		super(gregapi.block.BlockBase.mapColorOf(BlockBehaviour.Properties.of().replaceable().liquid().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).noLootTable().explosionResistance(FL.gas(aFluid) ? 1F : 30F), aMaterial), aMaterial, aFluid,
			aMaterial == Material.water || aMaterial == Material.lava ? EngineRole.OWN_TAGGED_FLUID : EngineRole.NO_ENGINE_FLUID);
		mFluid = aFluid;
		mAmountPerQuanta = aAmountPerQuanta;
		gregapi.GT_API.deferItemInit(() -> mQuanta = FL.make(mFluid, mAmountPerQuanta));
		mDensityDir = densityDir;
		mFlammability = aFlammability;
		mNameInternal = aNameInternal;
		// Only the BlockItem is registered here; the block itself registers lazily at its call site.
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> gregapi.GT_API.blockItemFor(this, gregapi.block.fluid.ItemBlockFluidGT.class));
		FL.BLOCKS.put(FL.regName(mFluid), this);
		displacements.put(this, F);
		LanguageHandler.set(getLocalizedName(), getLocalizedName()); // WAILA is retarded...
		// Speaking of retarded, only allowing one type of Block per Fluid is retarded too! So I guess I gotta override all pre-existing Fluids with my Version to make sure shit works.
		// The Forge field this reflects into doesn't exist on the neo mirror, which used to spam a NoSuchFieldException on every
		// registration; the link is centralized through FL.BLOCKS instead, and this call is kept silent so it stays harmless.
		UT.Reflection.setField(Fluid.class, aFluid, "block", this, F);
	}
	
	/** Restored after the shared ancestor regained IFluidBlock; matches 1.7.10 exactly. */
	// LiquidBlock.getFluid() returns FlowingFluid while Forge's IFluidBlock.getFluid() returns Fluid; a
	// covariant return covers both, taken from the shared center liquidCarrierFor.
	@Override public net.minecraft.world.level.material.FlowingFluid getFluid() {return liquidCarrierFor(mMaterial, mFluid);}

	/** "Which fluid does this cell hold" is a separate question from the ancestor's role (see there):
	 *  for content fluids it's their own mFluid, not the carrier. */
	@Override public Fluid ownFluid() {return mFluid;}

	/** A finite fluid can be drained from any level, unlike the classic branch where GregTech6
	 *  overrides this with its own "meta 0 is the source" rule. */
	// The real net.minecraftforge.fluids.IFluidBlock signature is canDrain(Level,BlockPos).
	// Not the old shim's (Level,int,int,int).
	@Override public boolean canDrain(Level aWorld, net.minecraft.core.BlockPos aPos) {return T;}

	// The real signature is drain(Level,BlockPos,IFluidHandler.FluidAction), not the old (Level,int,int,int,boolean).
	@Override
	public FluidStack drain(Level aWorld, net.minecraft.core.BlockPos aPos, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction aAction) {
		int aX = aPos.getX(), aY = aPos.getY(), aZ = aPos.getZ();
		// Forge royally fucked up again. You check for MetaData FIRST and do the set Block to Air SECOND, like I demonstrate here!!!
		FluidStack rFluid = FL.mul(mQuanta, WD.meta(aWorld, aX, aY, aZ)+1);
		if (aAction.execute()) {
			WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
			updateFluidBlocks(aWorld, aX, aY, aZ, T);
		}
		return rFluid;
	}
	
	// @Override
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aUselessBlock) {
		// Do the update in a few ticks.
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
		// Remove Flowing Water/Lava from adjacent Blocks!
		for (byte tSide : ALL_SIDES_VALID) {
			Block tBlock = WD.block(aWorld, aX, aY, aZ, tSide, F);
			// Check for broken Fluids of the same Material as this Fluid.
			if (WD.getMaterial(tBlock) == getMaterial() && WD.liquid_borken(tBlock)) {
				// Get rid of Flowing Water/Lava adjacent to my Fluids, because Forge is fucked up.
				if (WD.meta(aWorld, aX, aY, aZ, tSide, F) != 0 && WD.set(aWorld, aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide], NB, 0, 2)) {
					// The Water might have blocked a previous path.
					updateFluidBlocks(aWorld, aX, aY, aZ, T);
				}
			}
		}
	}
	
	public void updateFluidBlocks(Level aWorld, int aX, int aY, int aZ, boolean aAll) {
		// neo's world bounds are [minY, topY), not the old fixed [0, 256), so this checks against them instead.
		for (int j = mDensityDir > 0 ? -1 : 0; j < (mDensityDir > 0 ? 1 : 2); j++) if (UT.Code.inside(WD.minY(aWorld), WD.topY(aWorld), aY+j)) for (int i = -4; i <= 4; i++) for (int k = -4; k <= 4; k++) if (i != 0 || j != 0 || k != 0) {
			if (WD.block(aWorld, aX+i, aY+j, aZ+k) == this && (aAll || WD.meta(aWorld, aX+i, aY+j, aZ+k) > (j == 0 ? Math.abs(i) : 0))) {
				aWorld.scheduleTick(new BlockPos(aX+i, aY+j, aZ+k), this, tickRate);
			}
		}
	}
	
	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		// Flammability checks.
		if (mFlammability > 0) for (byte tSide : ALL_SIDES_VALID) {
			Block tBlock = WD.block(aWorld, aX, aY, aZ, tSide, F);
			if (tBlock != this && (WD.getMaterial(tBlock) == Material.lava || WD.getMaterial(tBlock) == Material.fire)) {
				WD.burn(aWorld, aX, aY, aZ, T, F);
				WD.burn(aWorld, aX-4+aRandom.nextInt(9), aY-4+aRandom.nextInt(9), aZ-4+aRandom.nextInt(9), F, F);
				WD.burn(aWorld, aX-4+aRandom.nextInt(9), aY-4+aRandom.nextInt(9), aZ-4+aRandom.nextInt(9), F, F);
				WD.burn(aWorld, aX-4+aRandom.nextInt(9), aY-4+aRandom.nextInt(9), aZ-4+aRandom.nextInt(9), F, F);
				return;
			}
		}
		
		int tRemainingQuanta = WD.meta(aWorld, aX, aY, aZ)+1;

		// Trash Fluid Blocks that get in contact with the vertical World Limits.
		// neo's world floor is getMinY() (-64), below zero, so bedrock-level fluid sources used to get
		// trashed instantly by the old fixed-zero check; this reads the real floor instead.
		if (aY <= WD.minY(aWorld) || aY+1 >= WD.topY(aWorld)) {
			if (WD.set(aWorld, aX, aY, aZ, NB, 0, FLUID_UPDATE_FLAGS | 1)) GarbageGT.trash(FL.mul(mQuanta, tRemainingQuanta));
			return;
		}
		
		int oRemainingQuanta = tRemainingQuanta;
		
		tRemainingQuanta = tryToFlowVerticallyInto(aWorld, aX, aY, aZ, tRemainingQuanta);
		
		if (tRemainingQuanta < 1) {
			updateFluidBlocks(aWorld, aX, aY, aZ, F);
			return;
		}
		
		boolean tChanged = (tRemainingQuanta != oRemainingQuanta);
		if (tRemainingQuanta == 1) {
			if (tChanged) {
				set(aWorld, aX, aY, aZ, tRemainingQuanta-1, F);
				updateFluidBlocks(aWorld, aX, aY, aZ, F);
				return;
			}
			if (!WD.liquid(aWorld, aX, aY+mDensityDir, aZ)) {
				for (byte tSide : ALL_SIDES_HORIZONTAL_ORDER[RNGSUS.nextInt(ALL_SIDES_HORIZONTAL_ORDER.length)]) {
					if (WD.exists                 (aWorld, aX+OFFX[tSide], aY            , aZ+OFFZ[tSide])
					&& !WD.hasCollide     (aWorld, aX+OFFX[tSide], aY+mDensityDir, aZ+OFFZ[tSide])
					&& displaceIfPossible (aWorld, aX+OFFX[tSide], aY            , aZ+OFFZ[tSide])
					&& set                (aWorld, aX+OFFX[tSide], aY            , aZ+OFFZ[tSide], tRemainingQuanta-1, F)) {
						aWorld.scheduleTick(new BlockPos(aX+OFFX[tSide], aY            , aZ+OFFZ[tSide]), this, tickRate);
						WD.set            (aWorld, aX            , aY            , aZ            , NB, 0, FLUID_UPDATE_FLAGS | 1);
						updateFluidBlocks (aWorld, aX            , aY            , aZ            , T);
						return;
					}
				}
			}
			return;
		}
		
		if (WD.exists(aWorld, aX, aY, aZ-1) && displaceIfPossible(aWorld, aX  , aY, aZ-1)) WD.setIfDiff(aWorld, aX  , aY, aZ-1, NB, 0, FLUID_UPDATE_FLAGS | 1);
		if (WD.exists(aWorld, aX, aY, aZ+1) && displaceIfPossible(aWorld, aX  , aY, aZ+1)) WD.setIfDiff(aWorld, aX  , aY, aZ+1, NB, 0, FLUID_UPDATE_FLAGS | 1);
		if (WD.exists(aWorld, aX-1, aY, aZ) && displaceIfPossible(aWorld, aX-1, aY, aZ  )) WD.setIfDiff(aWorld, aX-1, aY, aZ  , NB, 0, FLUID_UPDATE_FLAGS | 1);
		if (WD.exists(aWorld, aX+1, aY, aZ) && displaceIfPossible(aWorld, aX+1, aY, aZ  )) WD.setIfDiff(aWorld, aX+1, aY, aZ  , NB, 0, FLUID_UPDATE_FLAGS | 1);
		
		int tTotal = tRemainingQuanta, tCount = 1;
		
		int north = getQuantaValueBelow(aWorld, aX  , aY, aZ-1, tRemainingQuanta-1);
		int south = getQuantaValueBelow(aWorld, aX  , aY, aZ+1, tRemainingQuanta-1);
		int west  = getQuantaValueBelow(aWorld, aX-1, aY, aZ  , tRemainingQuanta-1);
		int east  = getQuantaValueBelow(aWorld, aX+1, aY, aZ  , tRemainingQuanta-1);
		
		if (north >= 0) {tCount++; tTotal += north;}
		if (south >= 0) {tCount++; tTotal += south;}
		if (west  >= 0) {tCount++; tTotal += west ;}
		if (east  >= 0) {tCount++; tTotal += east ;}
		
		if (tCount == 1) {
			if (tChanged) {
				set(aWorld, aX, aY, aZ, tRemainingQuanta-1, F);
				updateFluidBlocks(aWorld, aX, aY, aZ, F);
			}
			return;
		}
		
		int tSpread = tTotal / tCount, tRemainder = tTotal % tCount;
		if (north >= 0) {
			int tNew = tSpread;
			if (tRemainder == tCount || tRemainder > 1 && aRandom.nextInt(tCount - tRemainder) != 0) {++tNew; --tRemainder;} tCount--;
			if (tNew != north) if (tNew > 0) {if (set(aWorld, aX  , aY, aZ-1, tNew-1, F)) aWorld.scheduleTick(new BlockPos(aX  , aY, aZ-1), this, tickRate);} else WD.setIfDiff(aWorld, aX  , aY, aZ-1, NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		if (south >= 0) {
			int tNew = tSpread;
			if (tRemainder == tCount || tRemainder > 1 && aRandom.nextInt(tCount - tRemainder) != 0) {++tNew; --tRemainder;} tCount--;
			if (tNew != south) if (tNew > 0) {if (set(aWorld, aX  , aY, aZ+1, tNew-1, F)) aWorld.scheduleTick(new BlockPos(aX  , aY, aZ+1), this, tickRate);} else WD.setIfDiff(aWorld, aX  , aY, aZ+1, NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		if (west >= 0) {
			int tNew = tSpread;
			if (tRemainder == tCount || tRemainder > 1 && aRandom.nextInt(tCount - tRemainder) != 0) {++tNew; --tRemainder;} tCount--;
			if (tNew != west ) if (tNew > 0) {if (set(aWorld, aX-1, aY, aZ  , tNew-1, F)) aWorld.scheduleTick(new BlockPos(aX-1, aY, aZ  ), this, tickRate);} else WD.setIfDiff(aWorld, aX-1, aY, aZ  , NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		if (east >= 0) {
			int tNew = tSpread;
			if (tRemainder == tCount || tRemainder > 1 && aRandom.nextInt(tCount - tRemainder) != 0) {++tNew; --tRemainder;} tCount--;
			if (tNew != east ) if (tNew > 0) {if (set(aWorld, aX+1, aY, aZ  , tNew-1, F)) aWorld.scheduleTick(new BlockPos(aX+1, aY, aZ  ), this, tickRate);} else WD.setIfDiff(aWorld, aX+1, aY, aZ  , NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		set(aWorld, aX, aY, aZ, tRemainder > 0 ? tSpread : tSpread - 1, F);
	}
	
	// @Override
	public int tryToFlowVerticallyInto(Level aWorld, int aX, int aY, int aZ, int aAmount) {
		// First do the Water specific check.
		if (mLighterThanWater) {
			int tY = aY;
			// neo's world ceiling is topY (320), not the total height count (384) that the no-arg getHeight() returns.
			while (++tY < WD.topY(aWorld) && WD.anywater(aWorld, aX, tY, aZ));
			if (tY-1 > aY) {
				Block tBlock = WD.block(aWorld, aX, tY, aZ);
				if (tBlock == this) {
					int tAmount = 1 + WD.meta(aWorld, aX, tY, aZ) + aAmount;
					if (tAmount > 16) {
						set(aWorld, aX, tY, aZ, 16 - 1, T);
						aWorld.scheduleTick(new BlockPos(aX, tY, aZ), this, tickRate);
						return tAmount - 16;
					}
					if (tAmount > 0) {
						set(aWorld, aX, tY, aZ, tAmount - 1, T);
						// Called by the Block Update caused by setBlockToAir
						// aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
						WD.set(aWorld, aX, aY, aZ, NB, 0, FLUID_UPDATE_FLAGS | 1);
						return 0;
					}
					return aAmount;
				}
				if (WD.air(aWorld, aX, tY, aZ, tBlock) || displaceIfPossible(aWorld, aX, tY, aZ)) {
					set(aWorld, aX, tY, aZ, aAmount - 1, T);
					aWorld.scheduleTick(new BlockPos(aX, tY, aZ), this, tickRate);
					return 0;
				}
			}
		}
		
		// Compressed Fluid Blocks behave a little bit "jumpier" than normal ones. ;)
		if (aAmount > 8) {
			int tY = aY - mDensityDir;
			Block tBlock = WD.block(aWorld, aX, tY, aZ);
			
			// Swap with any finite Fluid Blocks "above" this one unless they are also compressed.
			if (tBlock instanceof BlockBaseFluid) { // the Forge BlockFluidFinite style now lives only in this class
				int tMeta = WD.meta(aWorld, aX, tY, aZ);
				if (tMeta > 7) return aAmount;
				WD.set(aWorld, aX, aY, aZ, tBlock, tMeta, FLUID_UPDATE_FLAGS | 1);
				set(aWorld, aX, tY, aZ, aAmount - 1, T);
				aWorld.scheduleTick(new BlockPos(aX, tY, aZ), this, tickRate);
				return 0;
			}
			// Swap with GT6 Water Blocks.
			if (!mLighterThanWater && WD.anywater(tBlock)) {
				WD.set(aWorld, aX, aY, aZ, tBlock, WD.meta(aWorld, aX, tY, aZ), FLUID_UPDATE_FLAGS | 1);
				set(aWorld, aX, tY, aZ, aAmount - 1, T);
				aWorld.scheduleTick(new BlockPos(aX, tY, aZ), this, tickRate);
				return 0;
			}
			// Lets just jump up! Make a Fountain!
			if (WD.air(aWorld, aX, tY, aZ, tBlock) || displaceIfPossible(aWorld, aX, tY, aZ)) {
				// The Block left behind should stay for a bit.
				aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 128 - aAmount * 4);
				// All but one Quanta will move up!
				set(aWorld, aX, tY, aZ, aAmount - 2, T);
				// Since it is a Jump, we will give it a fast reaction time!
				aWorld.scheduleTick(new BlockPos(aX, tY, aZ), this, 1);
				// Update all Fluid Blocks around this, since they might have been very compressed before too.
				updateFluidBlocks(aWorld, aX, aY, aZ, T);
				// Leaving a minimal Block at the original location to make it more Fountain like.
				return 1;
			}
		}
		
		int tY = aY + mDensityDir;
		Block tBlock = WD.block(aWorld, aX, tY, aZ);
		
		if (tBlock == this) {
			int tAmount = 1 + WD.meta(aWorld, aX, tY, aZ) + aAmount;
			if (tAmount > 8) {
				set(aWorld, aX, tY, aZ, 8 - 1, T);
				aWorld.scheduleTick(new BlockPos(aX, tY, aZ), this, tickRate);
				return tAmount - 8;
			}
			if (tAmount > 0) {
				set(aWorld, aX, tY, aZ, tAmount - 1, T);
				// Called by the Block Update caused by setBlockToAir
				// aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
				WD.set(aWorld, aX, aY, aZ, NB, 0, FLUID_UPDATE_FLAGS | 1);
				return 0;
			}
			return aAmount;
		}
		if (tBlock instanceof BlockFluidBaseGT) { // BlockFluidBaseGT now reproduces that same shared Forge ancestor
			if (mDensityDir > 0 ? getDensity(aWorld, aX, tY, aZ) > density : getDensity(aWorld, aX, tY, aZ) < density) {
				WD.set(aWorld, aX, aY, aZ, tBlock, WD.meta(aWorld, aX, tY, aZ), FLUID_UPDATE_FLAGS | 1);
				set(aWorld, aX, tY, aZ, aAmount - 1, T);
				// And don't just cast the result of world.getBlock directly like Forge does.
				// Why the fuck do they call world.getBlock more than once for the Block below/above a Fluid...
				// Even worse I noticed that the Block Update caused by the second setBlock will schedule the update for the Block ANYWAYS!!!
				// aWorld.scheduleBlockUpdate(aX, aY, aZ, tBlock, ((BlockFluidBase)tBlock).tickRate(aWorld));
				aWorld.scheduleTick(new BlockPos(aX, tY, aZ), this, tickRate);
				return 0;
			}
			return aAmount;
		}
		if (WD.air(aWorld, aX, tY, aZ, tBlock) || displaceIfPossible(aWorld, aX, tY, aZ)) {
			set(aWorld, aX, tY, aZ, aAmount - 1, T);
			// Called by the Block Update caused by setBlockToAir
			// aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
			WD.set(aWorld, aX, aY, aZ, NB, 0, FLUID_UPDATE_FLAGS | 1);
			return 0;
		}
		return aAmount;
	}
	
	// neo's skipRendering has inverted semantics and drops the neighbor's position, which this
	// branch never needed; the per-tile-surface check that did need it now falls back to vanilla's default.
	@Override
	public boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {
		Block aBlock = aNeighbor.getBlock();
		if (aBlock == NB) return F;
		if (aBlock == this || WD.getMaterial(aBlock) == Material.water || WD.visOpq(aBlock)) return T;
		if (aNeighbor.isAir()) return F; // now just BlockState.isAir()
		return F;
	}
	
	// Ported body-for-body from Forge's BlockFluidFinite; GregTech6 never overrode this itself,
	// it just lived on the inherited body, and getQuantaValueBelow needs it.
	public int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).isAir()) return 0;
		if (WD.block(aWorld, aX, aY, aZ) != this) return -1;
		return WD.meta(aWorld, aX, aY, aZ)+1;
	}

	// There's no answer of this block's own anymore: "which fluid is here" is decided entirely by the
	// ancestor's role (BlockFluidBaseGT.getFluidState, final); the old material branch disagreed, drawing two geometries.
	/** This family's quanta scale for the role passport: FLUID_META+1 = 1..8 (meta 7 = full cell), matching main;
	 *  cell height comes from getOwnHeight(). A flowing state is safe: no vanilla tick is ever scheduled for this block. */
	@Override protected int engineLevelOfState(BlockState aState) {return aState.getValue(FLUID_META) + 1;}

	// Content fluids/gas are walkable, not solid (no collision, matching Forge's original BlockFluidBase);
	// unlike BlockWaterlike this one is NOT invisible -- it draws its own texture via GT6BlockModel, not vanilla's renderer.
	@Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {return net.minecraft.world.phys.shapes.Shapes.empty();}
	@Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {return net.minecraft.world.phys.shapes.Shapes.empty();}

	@Override public Block getBlock() {return this;}
	public final String getUnlocalizedName() {return FL.name(mFluid, F);} // replaced by the FL.name center
	public String getLocalizedName() {return FL.name(mFluid, T);} // FL.name(..., true) already includes the LH localization step
	public void registerBlockIcons(Object aIconRegister) {/**/}
	/** GT6 fluids use the same texture for still and flowing, so the side branch degenerates; this
	 *  still asks the shared center so "which texture" stays defined in one place. */
	@Override public net.minecraft.resources.ResourceLocation getIcon(int aSide, int aMeta) {return renderTexture() instanceof gregapi.render.BlockTextureFluid tTex ? tTex.icon() : null;}
	/** Both tints come from the same center that colors the block in the renderer, avoiding a second source of truth. */
	@Override public int getRenderColor(int aMeta) {return renderTexture() instanceof gregapi.render.BlockTextureFluid tTex && tTex.mRGBa != null ? gregapi.util.UT.Code.getRGBInt(tTex.mRGBa) : 0x00ffffff;}
	@Override public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return getRenderColor(0);}
	public int getRenderType() {return RendererBlockFluid.RENDER_ID;}
	public int getRenderBlockPass() {return 1;}

	// The old render-type channel had no real implementation, so fluid blocks (oil, gas, geothermal water) were invisible;
	// IRenderedBlock is now declared once in the shared ancestor, the same centralized model every GT6 block uses.
	private gregapi.render.ITexture mRenderTexture = null;
	@Override public gregapi.render.ITexture renderTexture() {if (mRenderTexture == null && CODE_CLIENT) mRenderTexture = gregapi.render.BlockTextureFluid.get(mFluid, T); return mRenderTexture;}

	/** Kept the world-aware body because fluid-mesh visibility genuinely needs per-position info
	 *  (hiding faces against neighboring fluid, joining corner-height slopes), which neo's skipRendering no longer passes. */
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return T;
		if (aBlock == this || WD.getMaterial(aBlock) == Material.water || WD.visOpq(aBlock)) return F;
		if (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).isAir()) return T;
		BlockEntity tTileEntity = aWorld.getBlockEntity(new BlockPos(aX, aY, aZ));
		if (tTileEntity instanceof ITileEntitySurface) return !((ITileEntitySurface)tTileEntity).isSurfaceOpaque(OPOS[aSide]);
		return T;
	}
	// Ported 1:1 from Forge's getFluidHeightForRender; the only render branch this hierarchy owns
	// itself, since water-like fluids skip world meshing entirely.
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {
		Block tAbove = WD.block(aWorld, aX, aY - mDensityDir, aZ);
		float tHeight = tAbove == this || tAbove instanceof BlockFluidBaseGT || WD.getMaterial(tAbove).isLiquid()
			? 1F : Math.max(1, getQuantaValue(aWorld, aX, aY, aZ)) / 8F * 0.875F;
		setBlockBounds(0, 0, 0, 1, tHeight, 1);
		return T;
	}
	// getLightOpacity lives on the shared ancestor BlockFluidBaseGT now, the central light-opacity bridge; the
	// 1.7.10 value was identical for both hierarchies, so the copy here was a duplicate.
	
	// neo moved fire spread to IBlockExtension's getFlammability/getFireSpreadSpeed; the old Forge
	// signature was dead, so oil never actually caught fire before this bridge.
	@Override public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {return mFlammability;}
	@Override public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {return mFlammability;}
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.canDisplace(aWorld, aX, aY, aZ);}
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.displaceIfPossible(aWorld, aX, aY, aZ);}
	public boolean canCollideCheck(int aMeta, boolean aFullHit) {return aFullHit && aMeta >= 7;}
	public boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {return mMediumDragH > 0 || !mEffectsBathing.isEmpty() || !mEffectsBreathing.isEmpty();}
	public boolean isNormalCube() {return F;}
	public boolean isOpaqueCube() {return F;}
	public boolean func_149730_j() {return F;}
	public boolean getTickRandomly() {return F;}
	// Moved to the shared ancestor for the same reason as getLightOpacity above: both hierarchies
	// had the same 1.7.10 value, and the engine reads it via getShadeBrightness.
	public boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return F;}
	
	
	public boolean mLighterThanWater = F;
	public BlockBaseFluid setLighterThanWater() {
		mLighterThanWater = T;
		return this;
	}
	
	// Ported from main 1:1 (buoyancy in every oil, not just the two 1.7.10 marked as webs): makeStuckInBlock
	// slows movement and a held jump adds lift, the same trick vanilla uses for swimmers. FluidType stays data/texture-only.
	public float mMediumDragH = 0, mMediumDragV = 0, mMediumRise = 0;
	/** aDragH/aDragV are movement multipliers (1 = no hindrance, cobweb-like = 0.25/0.05);
	 *  aRise is the per-tick rise impulse while holding jump, 0 meaning no buoyancy. */
	public BlockBaseFluid setMedium(double aDragH, double aDragV, double aRise) {
		mMediumDragH = (float)aDragH; mMediumDragV = (float)aDragV; mMediumRise = (float)aRise;
		return this;
	}
	/** Matches 1.7.10's cobweb behavior exactly, using vanilla WebBlock's own constants, with no buoyancy. */
	public BlockBaseFluid setWeb() {return setMedium(0.25, 0.05, 0);}
	
	public boolean set(Level aWorld, int aX, int aY, int aZ, int aMeta, boolean aBlockUpdate) {
		if (WD.block(aWorld, aX, aY, aZ) != this) return WD.set(aWorld, aX, aY, aZ, this, aMeta, aBlockUpdate ? 3 : 2);
		byte tMeta = WD.meta(aWorld, aX, aY, aZ);
		return aMeta == tMeta || WD.set(aWorld, aX, aY, aZ, this, aMeta, aMeta >= 7 && tMeta >= 7 ? aBlockUpdate ? 5 : 4 : aBlockUpdate ? 3 : 2);
	}
	
	/** This Function has been named wrong. It should be onEntityOverlapWithBlock */
	// Was onEntityCollidedWithBlock(World,x,y,z,Entity); neo's entityInside gained effectApplier/isPrecise
	// params (no 1.7.10 analog) that GT6 doesn't use, since it never used them before either.
	@Override public void entityInside(BlockState aState, Level aWorld, BlockPos aPos, Entity aEntity) {
		if (mMediumDragH > 0) {
			aEntity.makeStuckInBlock(defaultBlockState(), new Vec3(mMediumDragH, mMediumDragV, mMediumDragH)); // setInWeb() no longer exists, so this follows the same technique as vanilla WebBlock.entityInside
			// Exactly one buoyancy impulse per tick, gated to the cell the feet stand in, or a body spanning two
			// cells would get pushed twice; move() damps inertia itself, so rise speed settles at (impulse - gravity) x drag.
			if (mMediumRise > 0 && aEntity instanceof LivingEntity tLiving && tLiving.jumping && aPos.equals(aEntity.blockPosition())) {
				// Smoothing at the surface: a full impulse there launched the body out, and the next tick dropped it
				// back in -- a jittery bounce. Scaling the impulse by depth lets it settle at equilibrium near the surface instead.
				double tDepth;
				if (aWorld.getBlockState(aPos.above()).getBlock() == this) tDepth = 1;
				else tDepth = Math.max(0, Math.min(1, aPos.getY() + (aState.getValue(FLUID_META) + 1) / 8.0 - aEntity.getY()));
				if (tDepth > 0) {
					if (tLiving.horizontalCollision) {
						// Climbing onto the shore uses the engine's own water trick 1:1 (a wall in liquid gives 0.3 vertical speed);
						// the push is set before the drag multiplier divides the whole motion, so it still comes out to 0.3 in any oil.
						Vec3 tV = tLiving.getDeltaMovement();
						tLiving.setDeltaMovement(tV.x, 0.3F / mMediumDragV, tV.z);
					} else tLiving.addDeltaMovement(new Vec3(0, mMediumRise * tDepth, 0));
				}
			}
		}
		if (!aWorld.isClientSide() && !mEffectsBathing.isEmpty() && aEntity instanceof LivingEntity && !UT.Entities.isWearingFullChemHazmat((LivingEntity)aEntity)) {
			for (int[] tEffects : mEffectsBathing) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
		}
	}
	@Override
	public void onHeadInside(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !mEffectsBreathing.isEmpty() && !UT.Entities.isImmuneToBreathingGases(aEntity)) {
			for (int[] tEffects : mEffectsBreathing) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
			if (getMaterial() != Material.water && SERVER_TIME % 20 == 0) aEntity.hurt(aWorld.damageSources().drown(), 2.0F); // see BlockWaterlike for the drowning-damage equivalent
		}
	}
	
	public List<int[]> mEffectsBathing = new ArrayListNoNulls<>();
	public BlockBaseFluid addEffectBathing(int aEffectID, int aEffectDuration, int aEffectLevel) {
		mEffectsBathing.add(new int[] {aEffectID, aEffectDuration, aEffectLevel});
		return this;
	}
	
	public List<int[]> mEffectsBreathing = new ArrayListNoNulls<>();
	public BlockBaseFluid addEffectBreathing(int aEffectID, int aEffectDuration, int aEffectLevel) {
		mEffectsBreathing.add(new int[] {aEffectID, aEffectDuration, aEffectLevel});
		return this;
	}
}
