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
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F5 форс движка (decisions/F5-fluids.md §5): было {@code extends BlockFluidFinite} (Forge, удалён в neo) —
 * общий предок с {@link gregapi.block.fluid.BlockWaterlike} воспроизведён ОДИН раз в {@link BlockFluidBaseGT}
 * (см. его javadoc). Тела GT6-собственных методов (updateTick/tryToFlowVerticallyInto/updateFluidBlocks/...) —
 * 1:1, только API-свод.
 */
public class BlockBaseFluid extends BlockFluidBaseGT implements IBlock, IItemGT, IBlockOnHeadInside, gregapi.render.IRenderedBlock {
	public static int FLUID_UPDATE_FLAGS = 2;
	
	public final String mNameInternal;
	public final int mFlammability, mAmountPerQuanta, mDensityDir;
	public final Fluid mFluid;
	// F12-followup (block-split): было final; FL.make(=new FluidStack) требует компоненты (server-start), присваивание отложено.
	public FluidStack mQuanta;
	
	public BlockBaseFluid(String aNameInternal, FL aFluid, int aFlammability) {
		this(aNameInternal, aFluid.fluid(), aFlammability);
	}
	public BlockBaseFluid(String aNameInternal, FL aFluid, int aFlammability, Material aMaterial) {
		this(aNameInternal, aFluid.fluid(), aFlammability, aMaterial);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aFlammability) {
		// было aFluid.isGaseous()/aFluid.getTemperature() — Forge-only data-holder-методы на самом Fluid;
		// neo net.minecraft.world.level.material.Fluid их не несёт (данные расщеплены в FluidType, F5-доклад
		// §1/§3) — центр FL.gas(Fluid)/FL.temperature(Fluid) уже воспроизводит то же самое (gregapi/data/FL.java).
		this(aNameInternal, aFluid, aFlammability, FL.gas(aFluid) ? MaterialGas.instance : FL.temperature(aFluid) > 500 ? Material.lava : Material.water);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aFlammability, Material aMaterial) {
		this(aNameInternal, aFluid, 125, aFlammability, aMaterial);
	}
	public BlockBaseFluid(String aNameInternal, Fluid aFluid, int aAmountPerQuanta, int aFlammability, Material aMaterial) {
		// было super(aFluid, aMaterial) + setResistance(FL.gas(mFluid)?1:30) — neo Block immutable (Properties
		// ДО super, F16/F9 форс движка, см. BlockFluidBaseGT); resistance считаем от параметра aFluid (mFluid
		// ещё не присвоен на этой стадии — тот же самый Fluid).
		// F12-followup (block-split): setId в Properties (иначе «Block id not set»); namespace=GAPI (совпадает с реестром/call-site).
		// .replaceable().liquid().pushReaction(DESTROY).noLootTable() — 1:1 c 1.7.10 MaterialOil/MaterialGas
		// (MaterialLiquid: setReplaceable + setNoPushMobility; дропов у Forge BlockFluidBase не было) — без
		// replaceable в neo НЕЛЬЗЯ поставить блок в жидкость (у vanilla-воды тот же набор флагов, Blocks.java:297-304).
		super(BlockBehaviour.Properties.of().replaceable().liquid().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).noLootTable().explosionResistance(FL.gas(aFluid) ? 1F : 30F).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aNameInternal)))), aMaterial, aFluid);
		mFluid = aFluid;
		mAmountPerQuanta = aAmountPerQuanta;
		gregapi.GT_API.deferItemInit(() -> mQuanta = FL.make(mFluid, mAmountPerQuanta));
		mDensityDir = densityDir;
		mFlammability = aFlammability;
		mNameInternal = aNameInternal;
		// F12-followup (block-split): блок регистрирует registerBlockLazy на call-site (Loader_Blocks); ЗДЕСЬ — только BlockItem.
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> gregapi.GT_API.blockItemFor(this, BlockItem.class));
		FL.BLOCKS.put(FL.regName(mFluid), this);
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
		// F6-Y-scale: было inside(0, aWorld.getHeight(), aY+j) — границы мира neo [minY..topY), не [0..256).
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
		// F6-Y-scale: было aY <= 0 || aY+1 >= aWorld.getHeight() (мир 1.7.10 [0..255]); neo дно = getMinY() (-64) —
		// бедрок-источники живут НИЖЕ нуля, старая проверка мгновенно трэшила их жидкость (эталон WorldgenOcean d6dc0f2d).
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
			// F6-Y-scale: было ++tY < aWorld.getHeight() — верх мира neo = topY (320), no-arg getHeight()=COUNT(384).
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
			if (tBlock instanceof BlockBaseFluid) { // было instanceof BlockFluidFinite (Forge) — GT6-Finite-стиль воплощён ТОЛЬКО в BlockBaseFluid (BlockWaterlike=Classic-стиль, F5-доклад §5)
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
		if (tBlock instanceof BlockFluidBaseGT) { // было instanceof BlockFluidBase (Forge, общий предок Classic+Finite) — BlockFluidBaseGT воспроизводит тот же общий предок (F5-доклад §5)
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
	
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], семантика ИНВЕРТИРОВАНА (shouldRender -> skipRendering). Позиция(aX,aY,aZ) в исходнике
	// была позицией СОСЕДА -> aNeighbor.getBlock()/aNeighbor.isAir() эквивалентны без потерь для block-identity веток.
	// F3 functional-adapted (neo skipRendering сигнатура потеряла World/BlockPos → per-TE culling недостижим; используется vanilla-дефолт super.skipRendering, 1:1 по следствию): ITileEntitySurface-проверка соседа недостижима -
	// новая сигнатура не передаёт позицию соседа для WD.te-поиска; используется дефолт "не ITileEntitySurface" ветки.
	@Override
	protected boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {
		Block aBlock = aNeighbor.getBlock();
		if (aBlock == NB) return F;
		if (aBlock == this || WD.getMaterial(aBlock) == Material.water || WD.visOpq(aBlock)) return T;
		if (aNeighbor.isAir()) return F; // было aBlock.isAir(world,x,y,z) — BlockState.isAir()
		return F;
	}
	
	// было Forge BlockFluidFinite.getQuantaValue(IBlockAccess,x,y,z) (@Override там же) — тело 1:1, GT6 сама
	// свой getQuantaValue не переопределяла (жила на унаследованном Finite-теле), нужен getQuantaValueBelow.
	public int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).isAir()) return 0;
		if (WD.block(aWorld, aX, aY, aZ) != this) return -1;
		return WD.meta(aWorld, aX, aY, aZ)+1;
	}

	// F5-B2 (реверс воды mc26, content-жидкости): погружение/утопление/push через getFluidState (см. BlockWaterlike). Движок
	// применяет эффекты только для fluid в теге FluidTags.WATER/LAVA (EntityFluidInteraction). Content-жидкость с материалом
	// water/lava → vanilla WATER/LAVA FluidState по квантам (meta+1=1..8, полный=8) → игрок тонет/горит в ней. Прочие
	// (газ/материал-специфика) → super=EMPTY: у них GT6-эффекты (bathing/breathing/drown) уже в entityInside/onHeadInside.
	@Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState aState) {
		// SOURCE-ВСЕГДА (НЕ FLOWING): критично. Neo вызывает FluidState.tick() для любого non-EMPTY FLOWING-состояния
		// (LevelChunk.postProcessGeneration / ServerLevel.tickFluid) → FlowingFluid.getNewLiquid не находит vanilla-источников
		// (GT6-источник ≠ Blocks.WATER) → EMPTY → level.setBlock(AIR) УДАЛЯЕТ GT6-flowing мгновенно («гейзер появился и пропал»).
		// isSource-состояния движок НЕ тикает. GT6-flow (updateTick по FLUID_META/quanta) полностью независим от FluidState —
		// FluidState нужен лишь для физики (тег WATER/LAVA → погружение/тонешь/горишь); визуал BlockBaseFluid — GT6BlockModel.
		// Исходник Forge 1.7.10 getFluidState НЕ имел (конфликта двух tick-систем не было). Материал water/lava → source-тег.
		gregapi.block.Material tMat = getMaterial();
		if (tMat == gregapi.block.Material.water) return net.minecraft.world.level.material.Fluids.WATER.defaultFluidState();
		if (tMat == gregapi.block.Material.lava ) return net.minecraft.world.level.material.Fluids.LAVA.defaultFluidState();
		return super.getFluidState(aState);
	}

	// F5-B block-контракт (проходимость): материал-жидкость/газ (масла/кислоты/газы) — ПРОХОДИМЫ, нельзя стоять на них
	// как на твёрдом блоке (оригинал: Forge BlockFluidBase — коллайдера нет, canDisplace). getCollisionShape/getShape=empty.
	// В ОТЛИЧИЕ от BlockWaterlike здесь НЕТ getRenderShape=INVISIBLE: материал-жидкость РИСУЕТСЯ своей текстурой через
	// GT6BlockModel (IRenderedBlock getTexture→renderTexture→FluidGT), а не через vanilla FluidRenderer.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {return net.minecraft.world.phys.shapes.Shapes.empty();}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {return net.minecraft.world.phys.shapes.Shapes.empty();}

	@Override public Block getBlock() {return this;}
	public final String getUnlocalizedName() {return FL.name(mFluid, F);} // было mFluid.getUnlocalizedName() (Forge Fluid) — FL.name(Fluid,boolean) центр (F5, см. BlockWaterlike)
	public String getLocalizedName() {return FL.name(mFluid, T);} // было LH.get(mFluid.getUnlocalizedName()) — FL.name(...,T) уже включает LH-локализацию (FL.java:952)
	public void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public IIcon getIcon(int aSide, int aMeta) {throw new UnsupportedOperationException("PORT-TODO(F3, fluid icon): было mFluid.getStillIcon()/getFlowingIcon() — 1.7.10 IIcon-атлас мёртв, реальный рендер RegisterFluidModelsEvent; crash-only per /goal");}
	public int getRenderColor(int aMeta) {throw new UnsupportedOperationException("PORT-TODO(F3, fluid tint): было mFluid.getColor() — neo-тинт FluidTintSources.constant на FluidType; crash-only per /goal");}
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {throw new UnsupportedOperationException("PORT-TODO(F3, fluid tint): см. getRenderColor; crash-only per /goal");}
	public int getRenderType() {return RendererBlockFluid.RENDER_ID;}
	public int getRenderBlockPass() {return 1;}

	// F3-render (флюид-блок ВИДИМ): было мёртвый getRenderType()=RendererBlockFluid.RENDER_ID (1.7.10) + getIcon/getRenderColor
	// кидали PORT-TODO → флюид-блок (нефть/газ/гео-вода worldgen'а) НЕ рисовался (прозрачный). Реализуем IRenderedBlock — та же
	// централизованная модель GT6BlockModel, что у всех грег-блоков (onModifyBakingResult инжектит её каждому IRenderedBlock).
	// Текстура+цвет флюида — из центра F5 (BlockTextureFluid → FluidGT.of(mFluid): still-иконка + mRGBa + свечение). Одна текстура
	// на все грани/один проход, полный куб. Кэшируем (mFluid final). alpha=T (флюид полупрозрачный, если слой это позволит).
	private gregapi.render.ITexture mRenderTexture = null;
	public gregapi.render.ITexture renderTexture() {if (mRenderTexture == null && CODE_CLIENT) mRenderTexture = gregapi.render.BlockTextureFluid.get(mFluid, T); return mRenderTexture;}

	/** было shouldSideBeRendered(IBlockAccess,x,y,z,side) (:348-356 оригинала) — тело 1:1, world-aware
	 *  (координаты СОСЕДА). Читает {@link gregapi.render.RendererBlockFluid} — neo skipRendering потерял
	 *  World/BlockPos, а fluid-мешу видимость нужна per-позиции (грань к соседней жидкости/opaque скрыта,
	 *  склоны угловых высот смыкают уровни без дыр). */
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return T;
		if (aBlock == this || WD.getMaterial(aBlock) == Material.water || WD.visOpq(aBlock)) return F;
		if (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).isAir()) return T;
		BlockEntity tTileEntity = aWorld.getBlockEntity(new BlockPos(aX, aY, aZ));
		if (tTileEntity instanceof ITileEntitySurface) return !((ITileEntitySurface)tTileEntity).isSurfaceOpaque(OPOS[aSide]);
		return T;
	}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return renderTexture();}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return renderTexture();}
	@Override public boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return aRenderPass == 0;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override public boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return F;}
	// высота поверхности 1:1 Forge BlockFluidBase.getFluidHeightForRender: сверху жидкость → полный куб,
	// иначе quanta/quantaPerBlock(8) * 0.875 (кванты живут в мете, getQuantaValue).
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {
		Block tAbove = WD.block(aWorld, aX, aY - mDensityDir, aZ);
		float tHeight = tAbove == this || tAbove instanceof BlockFluidBaseGT || WD.getMaterial(tAbove).isLiquid()
			? 1F : Math.max(1, getQuantaValue(aWorld, aX, aY, aZ)) / 8F * 0.875F;
		setBlockBounds(0, 0, 0, 1, tHeight, 1);
		return T;
	}
	@Override public int getRenderPasses(ItemStack aStack) {return 1;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return null;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	public int getLightOpacity() {return LIGHT_OPACITY_WATER;}
	
	// F-fire мост: было Forge Block.getFlammability/getFireSpreadSpeed(IBlockAccess,x,y,z,side) — в neo канал огня
	// живёт в IBlockExtension.getFlammability/getFireSpreadSpeed(BlockState,BlockGetter,BlockPos,Direction)
	// (IBlockExtension.java:677/721, читает FireBlock). Старая сигнатура была мёртвой — нефть не поджигалась.
	@Override public int getFlammability(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {return mFlammability;}
	@Override public int getFireSpreadSpeed(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {return mFlammability;}
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.canDisplace(aWorld, aX, aY, aZ);}
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.displaceIfPossible(aWorld, aX, aY, aZ);}
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
	// было onEntityCollidedWithBlock(World,x,y,z,Entity) -> BlockBehaviour.entityInside(BlockState,Level,BlockPos,Entity,InsideBlockEffectApplier,boolean) [BlockBehaviour.java:360];
	// новые параметры effectApplier/isPrecise (батч damage-эффектов, F16-концепция без 1.7.10-аналога) не используются - GT6 их и раньше не применял.
	@Override protected void entityInside(BlockState aState, Level aWorld, BlockPos aPos, Entity aEntity, net.minecraft.world.entity.InsideBlockEffectApplier aEffectApplier, boolean aIsPrecise) {
		if (mActLikeWeb) aEntity.makeStuckInBlock(defaultBlockState(), new Vec3(0.25, 0.05F, 0.25)); // было setInWeb() — 1.7.10 Entity.setInWeb() удалён; Entity.makeStuckInBlock(BlockState,Vec3) — тот же приём/константа, что ванильный WebBlock.entityInside (WebBlock.java:28,33)
		if (!aWorld.isClientSide() && !mEffectsBathing.isEmpty() && aEntity instanceof LivingEntity && !UT.Entities.isWearingFullChemHazmat((LivingEntity)aEntity)) {
			for (int[] tEffects : mEffectsBathing) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
		}
	}
	@Override
	public void onHeadInside(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !mEffectsBreathing.isEmpty() && !UT.Entities.isImmuneToBreathingGases(aEntity)) {
			for (int[] tEffects : mEffectsBreathing) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
			if (getMaterial() != Material.water && SERVER_TIME % 20 == 0) aEntity.hurt(aWorld.damageSources().drown(), 2.0F); // было attackEntityFrom(DamageSource.drown,...) — см. BlockWaterlike (GT_API_Proxy.java:744 precedent)
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
