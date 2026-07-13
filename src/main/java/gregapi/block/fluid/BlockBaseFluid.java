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
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidFinite;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class BlockBaseFluid extends BlockFluidFinite implements IBlock, IItemGT, IBlockOnHeadInside {
	public static int FLUID_UPDATE_FLAGS = 2;
	
	public final String mNameInternal;
	public final int mFlammability, mAmountPerQuanta, mDensityDir;
	public final Fluid mFluid;
	public final FluidStack mQuanta;
	
	public BlockBaseFluid(String aNameInternal, FL aFluid, int aFlammability) {
		this(aNameInternal, aFluid.fluid(), aFlammability);
	}
	public BlockBaseFluid(String aNameInternal, FL aFluid, int aFlammability, Material aMaterial) {
		this(aNameInternal, aFluid.fluid(), aFlammability, aMaterial);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aFlammability) {
		this(aNameInternal, aFluid, aFlammability, aFluid.isGaseous()?MaterialGas.instance:aFluid.getTemperature()>500?Material.lava:Material.water);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aFlammability, Material aMaterial) {
		this(aNameInternal, aFluid, 125, aFlammability, aMaterial);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aAmountPerQuanta, int aFlammability, Material aMaterial) {
		super(aFluid, aMaterial);
		mFluid = aFluid;
		mAmountPerQuanta = aAmountPerQuanta;
		mQuanta = FL.make(mFluid, mAmountPerQuanta);
		mDensityDir = densityDir;
		mFlammability = aFlammability;
		mNameInternal = aNameInternal;
		setResistance(FL.gas(mFluid) ? 1 : 30);
		ST.register(this, mNameInternal, BlockItem.class);
		FL.BLOCKS.put(mFluid.getName(), this);
		displacements.put(this, F);
		LanguageHandler.set(getLocalizedName(), getLocalizedName()); // WAILA is retarded...
		// Speaking of retarded, only allowing one type of Block per Fluid is retarded too! So I guess I gotta override all pre-existing Fluids with my Version to make sure shit works.
		UT.Reflection.setField(Fluid.class, aFluid, "block", this);
	}
	
	// @Override
	public FluidStack drain(Level aWorld, int aX, int aY, int aZ, boolean aDoDrain) {
		// Forge royally fucked up again. You check for MetaData FIRST and do the set Block to Air SECOND, like I demonstrate here!!!
		FluidStack rFluid = FL.mul(mQuanta, WD.meta(aWorld, aX, aY, aZ)+1);
		if (aDoDrain) {
			WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
			updateFluidBlocks(aWorld, aX, aY, aZ, T);
		}
		return rFluid;
	}
	
	// @Override
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aUselessBlock) {
		// Do the update in a few ticks.
		aWorld.scheduleBlockUpdate(aX, aY, aZ, this, tickRate);
		// Remove Flowing Water/Lava from adjacent Blocks!
		for (byte tSide : ALL_SIDES_VALID) {
			Block tBlock = WD.block(aWorld, aX, aY, aZ, tSide, F);
			// Check for broken Fluids of the same Material as this Fluid.
			if (tBlock.getMaterial() == getMaterial() && WD.liquid_borken(tBlock)) {
				// Get rid of Flowing Water/Lava adjacent to my Fluids, because Forge is fucked up.
				if (WD.meta(aWorld, aX, aY, aZ, tSide, F) != 0 && WD.set(aWorld, aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide], NB, 0, 2)) {
					// The Water might have blocked a previous path.
					updateFluidBlocks(aWorld, aX, aY, aZ, T);
				}
			}
		}
	}
	
	public void updateFluidBlocks(Level aWorld, int aX, int aY, int aZ, boolean aAll) {
		for (int j = mDensityDir > 0 ? -1 : 0; j < (mDensityDir > 0 ? 1 : 2); j++) if (UT.Code.inside(0, aWorld.getHeight(), aY+j)) for (int i = -4; i <= 4; i++) for (int k = -4; k <= 4; k++) if (i != 0 || j != 0 || k != 0) {
			if (WD.block(aWorld, aX+i, aY+j, aZ+k) == this && (aAll || WD.meta(aWorld, aX+i, aY+j, aZ+k) > (j == 0 ? Math.abs(i) : 0))) {
				aWorld.scheduleBlockUpdate(aX+i, aY+j, aZ+k, this, tickRate);
			}
		}
	}
	
	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		// Flammability checks.
		if (mFlammability > 0) for (byte tSide : ALL_SIDES_VALID) {
			Block tBlock = WD.block(aWorld, aX, aY, aZ, tSide, F);
			if (tBlock != this && (tBlock.getMaterial() == Material.lava || tBlock.getMaterial() == Material.fire)) {
				WD.burn(aWorld, aX, aY, aZ, T, F);
				WD.burn(aWorld, aX-4+aRandom.nextInt(9), aY-4+aRandom.nextInt(9), aZ-4+aRandom.nextInt(9), F, F);
				WD.burn(aWorld, aX-4+aRandom.nextInt(9), aY-4+aRandom.nextInt(9), aZ-4+aRandom.nextInt(9), F, F);
				WD.burn(aWorld, aX-4+aRandom.nextInt(9), aY-4+aRandom.nextInt(9), aZ-4+aRandom.nextInt(9), F, F);
				return;
			}
		}
		
		int tRemainingQuanta = WD.meta(aWorld, aX, aY, aZ)+1;
		
		// Trash Fluid Blocks that get in contact with the vertical World Limits.
		if (aY <= 0 || aY+1 >= aWorld.getHeight()) {
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
					if (aWorld.blockExists        (aX+OFFX[tSide], aY            , aZ+OFFZ[tSide])
					&& !WD.hasCollide     (aWorld, aX+OFFX[tSide], aY+mDensityDir, aZ+OFFZ[tSide])
					&& displaceIfPossible (aWorld, aX+OFFX[tSide], aY            , aZ+OFFZ[tSide])
					&& set                (aWorld, aX+OFFX[tSide], aY            , aZ+OFFZ[tSide], tRemainingQuanta-1, F)) {
						aWorld.scheduleBlockUpdate(aX+OFFX[tSide], aY            , aZ+OFFZ[tSide], this, tickRate);
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
			if (tNew != north) if (tNew > 0) {if (set(aWorld, aX  , aY, aZ-1, tNew-1, F)) aWorld.scheduleBlockUpdate(aX  , aY, aZ-1, this, tickRate);} else WD.setIfDiff(aWorld, aX  , aY, aZ-1, NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		if (south >= 0) {
			int tNew = tSpread;
			if (tRemainder == tCount || tRemainder > 1 && aRandom.nextInt(tCount - tRemainder) != 0) {++tNew; --tRemainder;} tCount--;
			if (tNew != south) if (tNew > 0) {if (set(aWorld, aX  , aY, aZ+1, tNew-1, F)) aWorld.scheduleBlockUpdate(aX  , aY, aZ+1, this, tickRate);} else WD.setIfDiff(aWorld, aX  , aY, aZ+1, NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		if (west >= 0) {
			int tNew = tSpread;
			if (tRemainder == tCount || tRemainder > 1 && aRandom.nextInt(tCount - tRemainder) != 0) {++tNew; --tRemainder;} tCount--;
			if (tNew != west ) if (tNew > 0) {if (set(aWorld, aX-1, aY, aZ  , tNew-1, F)) aWorld.scheduleBlockUpdate(aX-1, aY, aZ  , this, tickRate);} else WD.setIfDiff(aWorld, aX-1, aY, aZ  , NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		if (east >= 0) {
			int tNew = tSpread;
			if (tRemainder == tCount || tRemainder > 1 && aRandom.nextInt(tCount - tRemainder) != 0) {++tNew; --tRemainder;} tCount--;
			if (tNew != east ) if (tNew > 0) {if (set(aWorld, aX+1, aY, aZ  , tNew-1, F)) aWorld.scheduleBlockUpdate(aX+1, aY, aZ  , this, tickRate);} else WD.setIfDiff(aWorld, aX+1, aY, aZ  , NB, 0, FLUID_UPDATE_FLAGS | 1);
		}
		set(aWorld, aX, aY, aZ, tRemainder > 0 ? tSpread : tSpread - 1, F);
	}
	
	// @Override
	public int tryToFlowVerticallyInto(Level aWorld, int aX, int aY, int aZ, int aAmount) {
		// First do the Water specific check.
		if (mLighterThanWater) {
			int tY = aY;
			while (++tY < aWorld.getHeight() && WD.anywater(aWorld, aX, tY, aZ));
			if (tY-1 > aY) {
				Block tBlock = WD.block(aWorld, aX, tY, aZ);
				if (tBlock == this) {
					int tAmount = 1 + WD.meta(aWorld, aX, tY, aZ) + aAmount;
					if (tAmount > 16) {
						set(aWorld, aX, tY, aZ, 16 - 1, T);
						aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
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
					aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
					return 0;
				}
			}
		}
		
		// Compressed Fluid Blocks behave a little bit "jumpier" than normal ones. ;)
		if (aAmount > 8) {
			int tY = aY - mDensityDir;
			Block tBlock = WD.block(aWorld, aX, tY, aZ);
			
			// Swap with any finite Fluid Blocks "above" this one unless they are also compressed.
			if (tBlock instanceof BlockFluidFinite) {
				int tMeta = WD.meta(aWorld, aX, tY, aZ);
				if (tMeta > 7) return aAmount;
				WD.set(aWorld, aX, aY, aZ, tBlock, tMeta, FLUID_UPDATE_FLAGS | 1);
				set(aWorld, aX, tY, aZ, aAmount - 1, T);
				aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
				return 0;
			}
			// Swap with GT6 Water Blocks.
			if (!mLighterThanWater && WD.anywater(tBlock)) {
				WD.set(aWorld, aX, aY, aZ, tBlock, WD.meta(aWorld, aX, tY, aZ), FLUID_UPDATE_FLAGS | 1);
				set(aWorld, aX, tY, aZ, aAmount - 1, T);
				aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
				return 0;
			}
			// Lets just jump up! Make a Fountain!
			if (WD.air(aWorld, aX, tY, aZ, tBlock) || displaceIfPossible(aWorld, aX, tY, aZ)) {
				// The Block left behind should stay for a bit.
				aWorld.scheduleBlockUpdate(aX, aY, aZ, this, 128 - aAmount * 4);
				// All but one Quanta will move up!
				set(aWorld, aX, tY, aZ, aAmount - 2, T);
				// Since it is a Jump, we will give it a fast reaction time!
				aWorld.scheduleBlockUpdate(aX, tY, aZ, this, 1);
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
				aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
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
		if (tBlock instanceof BlockFluidBase) {
			if (mDensityDir > 0 ? getDensity(aWorld, aX, tY, aZ) > density : getDensity(aWorld, aX, tY, aZ) < density) {
				WD.set(aWorld, aX, aY, aZ, tBlock, WD.meta(aWorld, aX, tY, aZ), FLUID_UPDATE_FLAGS | 1);
				set(aWorld, aX, tY, aZ, aAmount - 1, T);
				// And don't just cast the result of world.getBlock directly like Forge does.
				// Why the fuck do they call world.getBlock more than once for the Block below/above a Fluid...
				// Even worse I noticed that the Block Update caused by the second setBlock will schedule the update for the Block ANYWAYS!!!
				// aWorld.scheduleBlockUpdate(aX, aY, aZ, tBlock, ((BlockFluidBase)tBlock).tickRate(aWorld));
				aWorld.scheduleBlockUpdate(aX, tY, aZ, this, tickRate);
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
	
	// @Override
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return T;
		if (aBlock == this || aBlock.getMaterial() == Material.water || WD.visOpq(aBlock)) return F;
		if (aBlock.isAir(aWorld, aX, aY, aZ)) return T;
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity instanceof ITileEntitySurface) return !((ITileEntitySurface)tTileEntity).isSurfaceOpaque(OPOS[aSide]);
		return T;
	}
	
	@Override public Block getBlock() {return this;}
	public final String getUnlocalizedName() {return mFluid.getUnlocalizedName();}
	public String getLocalizedName() {return LH.get(mFluid.getUnlocalizedName());}
	public void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public IIcon getIcon(int aSide, int aMeta) {return SIDES_VERTICAL[aSide]?mFluid.getStillIcon():mFluid.getFlowingIcon();}
	public int getRenderColor(int aMeta) {return mFluid.getColor();}
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return mFluid.getColor();}
	public int getRenderType() {return RendererBlockFluid.RENDER_ID;}
	public int getRenderBlockPass() {return 1;}
	public int getLightOpacity() {return LIGHT_OPACITY_WATER;}
	
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return mFlammability;}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return mFlammability;}
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {return !WD.block(aWorld, aX, aY, aZ).getMaterial().isLiquid() && super.canDisplace(aWorld, aX, aY, aZ);}
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {return !WD.block(aWorld, aX, aY, aZ).getMaterial().isLiquid() && super.displaceIfPossible(aWorld, aX, aY, aZ);}
	public boolean canCollideCheck(int aMeta, boolean aFullHit) {return aFullHit && aMeta >= 7;}
	public boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {return mActLikeWeb || !mEffectsBathing.isEmpty() || !mEffectsBreathing.isEmpty();}
	public boolean isNormalCube() {return F;}
	public boolean isOpaqueCube() {return F;}
	public boolean func_149730_j() {return F;}
	public boolean getTickRandomly() {return F;}
	public boolean renderAsNormalBlock() {return F;}
	public boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return F;}
	
	
	public boolean mLighterThanWater = F;
	public BlockBaseFluid setLighterThanWater() {
		mLighterThanWater = T;
		return this;
	}
	
	public boolean mActLikeWeb = F;
	public BlockBaseFluid setWeb() {
		mActLikeWeb = T;
		return this;
	}
	
	public boolean set(Level aWorld, int aX, int aY, int aZ, int aMeta, boolean aBlockUpdate) {
		if (WD.block(aWorld, aX, aY, aZ) != this) return WD.set(aWorld, aX, aY, aZ, this, aMeta, aBlockUpdate ? 3 : 2);
		byte tMeta = WD.meta(aWorld, aX, aY, aZ);
		return aMeta == tMeta || WD.set(aWorld, aX, aY, aZ, this, aMeta, aMeta >= 7 && tMeta >= 7 ? aBlockUpdate ? 5 : 4 : aBlockUpdate ? 3 : 2);
	}
	
	/** This Function has been named wrong. It should be onEntityOverlapWithBlock */
	// @Override
	public void onEntityCollidedWithBlock(Level aWorld, int aX, int aY, int aZ, Entity aEntity) {
		if (mActLikeWeb) aEntity.setInWeb();
		if (!aWorld.isClientSide() && !mEffectsBathing.isEmpty() && aEntity instanceof LivingEntity && !UT.Entities.isWearingFullChemHazmat((LivingEntity)aEntity)) {
			for (int[] tEffects : mEffectsBathing) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
		}
	}
	@Override
	public void onHeadInside(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !mEffectsBreathing.isEmpty() && !UT.Entities.isImmuneToBreathingGases(aEntity)) {
			for (int[] tEffects : mEffectsBreathing) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
			if (getMaterial() != Material.water && SERVER_TIME % 20 == 0) aEntity.attackEntityFrom(DamageSource.drown, 2.0F);
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
