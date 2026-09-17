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

package gregtech.blocks.fluids;

import gregapi.block.IBlock;
import gregapi.block.IBlockOnHeadInside;
import gregapi.block.fluid.BlockFluidBaseGT;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/** @author Gregorius Techneticies
 *  Was Forge's removed BlockFluidClassic; its shared ancestor with BlockBaseFluid is reproduced once in
 *  {@link BlockFluidBaseGT} instead. GT6's own method bodies here stay 1:1, only the API surface changed. */
public abstract class BlockWaterlike extends BlockFluidBaseGT implements IBlock, IItemGT, IBlockOnHeadInside {
	// A state change reaches the client only with the UPDATE_CLIENTS bit set (Level.markAndNotifyBlock);
	// 1.7.10 got that from the per-tick WD.update this port dropped, so the flag itself has to carry it.
	public static int WATER_UPDATE_FLAGS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS;

	public final Fluid mFluid;

	public BlockWaterlike(String aName, Fluid aFluid, boolean aFlowsOut, boolean aHide) {
		// The block is immutable, so every trait is decided here: water keeps density 1000 or the heavier oils
		// would push it aside, and the map colour is taken from the same Material.water the block declares.
		// Big waters must BE vanilla water by identity: waterlogging, freezing and swimming pick them with
		// is(Fluids.WATER) and offer no hook to join in otherwise.
		super(gregapi.block.BlockBase.mapColorOf(BlockBehaviour.Properties.of().replaceable().liquid().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).noLootTable().explosionResistance(30F), Material.water), Material.water, aFluid,
			gregapi.block.fluid.BlockFluidBaseGT.EngineRole.VANILLA_WATER);
		mFluid = aFluid;
		quantaPerBlock = (aFlowsOut ? 8 : 3);
		quantaPerBlockFloat = quantaPerBlock;
		// The block itself is registered at the call site; only its item belongs here.
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, aName, () -> gregapi.GT_API.blockItemFor(this, gregapi.block.fluid.ItemBlockFluidGT.class));
		LH.add(getUnlocalizedName(), getLocalizedName());
		LanguageHandler.set(getLocalizedName(), getLocalizedName()); // WAILA is retarded...
		if (aHide) gregapi.GT_API.deferItemInit(() -> ST.hide(this));
	}

	/** Restored after the shared ancestor regained IFluidBlock; matches 1.7.10 exactly. */
	@Override public net.minecraft.world.level.material.FlowingFluid getFluid() {return liquidCarrierFor(mMaterial, mFluid);}

	/** A second, separate question from the ancestor's own answer.
	 *  World water carries its own fluid field (sea/swamp/river), not a shared one. */
	@Override public Fluid ownFluid() {return mFluid;}

	// IFluidBlock's real drain/canDrain signatures take (Level,BlockPos[,FluidAction]).
	// The old shim's took raw coordinates instead.
	@Override
	public FluidStack drain(Level aWorld, net.minecraft.core.BlockPos aPos, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction aAction) {
		if (aAction.execute()) WD.set(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), NB, 0, 2);
		// getFluid() must stay a FlowingFluid here and so answers as the ancestor's plain-water carrier, not this cell's
		// actual fluid; drains ask the center (ownFluid) instead, keeping that question in one place for both hierarchies.
		return FL.make(ownFluid(), 1000);
	}

	@Override
	public boolean canDrain(Level aWorld, net.minecraft.core.BlockPos aPos) {
		return WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) == 0;
	}
	
	/** Was Forge's BlockFluidClassic.getLargerQuanta, body 1:1; only needed by updateFlow, since BlockBaseFluid has its own
	 *  separate quanta flow. */
	protected int getLargerQuanta(BlockGetter aWorld, int aX, int aY, int aZ, int aCompare) {
		int tQuantaRemaining = getQuantaValue(aWorld, aX, aY, aZ);
		if (tQuantaRemaining <= 0) return aCompare;
		return tQuantaRemaining >= aCompare ? tQuantaRemaining : aCompare;
	}

	/** Was Forge's isBlockSolid helper, whose side parameter the original body never used either; aSide is kept 1:1 in the
	 *  signature only because the caller passes it. */
	protected boolean isBlockSolid(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		return WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isSolid();
	}

	public void updateFlow(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		int quantaRemaining = quantaPerBlock - WD.meta(aWorld, aX, aY, aZ);
		int expQuanta = -101;
		// check adjacent block levels if non-source
		if (quantaRemaining < quantaPerBlock) {
			if (WD.block(aWorld, aX  , aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX-1, aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX+1, aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX  , aY-densityDir, aZ-1) instanceof BlockWaterlike ||
				WD.block(aWorld, aX  , aY-densityDir, aZ+1) instanceof BlockWaterlike) {
				expQuanta = quantaPerBlock - 1;
			} else {
				int maxQuanta = -100;
				maxQuanta = getLargerQuanta(aWorld, aX-1, aY, aZ  , maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX+1, aY, aZ  , maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX  , aY, aZ-1, maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX  , aY, aZ+1, maxQuanta);
				expQuanta = maxQuanta - 1;
			}
			if (expQuanta != quantaRemaining) {
				quantaRemaining = expQuanta;
				if (expQuanta <= 0) {
					WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				} else {
					WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), quantaPerBlock - expQuanta, 3, F);
					aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate); // was aWorld.scheduleBlockUpdate(x,y,z,block,delay); ScheduledTickAccess.scheduleTick(BlockPos,Block,int)
					aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this); // was aWorld.notifyBlocksOfNeighborChange(x,y,z,block); LevelAccessor.updateNeighborsAt(BlockPos,Block)
				}
			}
		}
		// Here was an else Block that only caused huge amounts of Network Lag with no purpose. Forge, just what the fuck, setting Metadata from 0 to 0 and updating that "change" to Clients? There was no change that needed to be updated!
		
		
		if (canDisplace(aWorld, aX, aY+densityDir, aZ)) {
			if (displaceIfPossible(aWorld, aX, aY+densityDir, aZ)) WD.set(aWorld, aX, aY+densityDir, aZ, this, 1, WATER_UPDATE_FLAGS | 1);
			return;
		}
		
		int tFlowMeta  = (WD.block(aWorld, aX, aY-densityDir, aZ) instanceof BlockWaterlike ? 1 : quantaPerBlock - quantaRemaining + 1);
		if (tFlowMeta >= quantaPerBlock) return;
		
		if (WD.exists(aWorld, aX, aY, aZ-1)) flowTo(aWorld, aX  , aY, aZ-1, tFlowMeta);
		if (WD.exists(aWorld, aX, aY, aZ+1)) flowTo(aWorld, aX  , aY, aZ+1, tFlowMeta);
		if (WD.exists(aWorld, aX-1, aY, aZ)) flowTo(aWorld, aX-1, aY, aZ  , tFlowMeta);
		if (WD.exists(aWorld, aX+1, aY, aZ)) flowTo(aWorld, aX+1, aY, aZ  , tFlowMeta);
	}

	// Spreading onto a waterloggable block (slab/stairs/fence) now waterlogs it instead of displacing it, like
	// vanilla mc26 water; waterlogging didn't exist in 1.7.10, so this is centralized into one helper for all 4 directions.
	public boolean flowTo(Level aWorld, int aX, int aY, int aZ, int aMeta) {
		net.minecraft.core.BlockPos tP = new net.minecraft.core.BlockPos(aX, aY, aZ);
		net.minecraft.world.level.block.state.BlockState tSt = aWorld.getBlockState(tP);
		if (tSt.getBlock() instanceof net.minecraft.world.level.block.SimpleWaterloggedBlock
		 && tSt.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
		 && !tSt.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
			return aWorld.setBlock(tP, tSt.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, Boolean.TRUE), 3);
		}
		if (displaceIfPossible(aWorld, aX, aY, aZ)) { WD.set(aWorld, aX, aY, aZ, this, aMeta, WATER_UPDATE_FLAGS | 1); return true; }
		return false;
	}
	
	// @Override
	public Vec3 getFlowVector(BlockGetter aWorld, int aX, int aY, int aZ) {
		Vec3 rVector = new Vec3(0, 0, 0); // was Vec3.createVectorHelper(0,0,0), a Forge/1.7.10-only factory; neo constructor Vec3(double,double,double)
		int tDecay = quantaPerBlock - getQuantaValue(aWorld, aX, aY, aZ);
		for (byte tSide : ALL_SIDES_HORIZONTAL) {
			int tX = aX+OFFX[tSide], tZ = aZ+OFFZ[tSide];
			int tOtherDecay = quantaPerBlock - getQuantaValue(aWorld, tX, aY, tZ);
			if (tOtherDecay >= quantaPerBlock) {
				if (!WD.getMaterial(WD.block(aWorld, tX, aY, tZ)).blocksMovement()) {
					tOtherDecay = quantaPerBlock - getQuantaValue(aWorld, tX, aY-1, tZ);
					if (tOtherDecay >= 0) {
						int tPower = tOtherDecay - (tDecay - quantaPerBlock);
						rVector = rVector.add((tX - aX) * tPower, 0, (tZ - aZ) * tPower); // was .addVector(...); Vec3.add(double,double,double)
					}
				}
			} else if (tOtherDecay >= 0) {
				int power = tOtherDecay - tDecay;
				rVector = rVector.add((tX - aX) * power, 0, (tZ - aZ) * power);
			}
		}
		if (WD.block(aWorld, aX, aY+1, aZ) instanceof BlockWaterlike && (
			isBlockSolid(aWorld, aX  , aY  , aZ-1, SIDE_Z_NEG) ||
			isBlockSolid(aWorld, aX  , aY  , aZ+1, SIDE_Z_POS) ||
			isBlockSolid(aWorld, aX-1, aY  , aZ  , SIDE_X_NEG) ||
			isBlockSolid(aWorld, aX+1, aY  , aZ  , SIDE_X_POS) ||
			isBlockSolid(aWorld, aX  , aY+1, aZ-1, SIDE_Z_NEG) ||
			isBlockSolid(aWorld, aX  , aY+1, aZ+1, SIDE_Z_POS) ||
			isBlockSolid(aWorld, aX-1, aY+1, aZ  , SIDE_X_NEG) ||
			isBlockSolid(aWorld, aX+1, aY+1, aZ  , SIDE_X_POS))) {
			rVector = rVector.normalize().add(0, -6, 0);
		}
		return rVector.normalize();
	}

	// @Override
	public int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return 0;
		if (aBlock == this) return quantaPerBlock - WD.meta(aWorld, aX, aY, aZ);
		if (aBlock instanceof BlockWaterlike) return 8-WD.meta(aWorld, aX, aY, aZ);
		if (aBlock == Blocks.WATER || aBlock == Blocks.WATER) return 8-WD.meta(aWorld, aX, aY, aZ);
		return -1;
	}

	// The engine's swim/drown/push/fog behavior keys strictly on the vanilla WATER fluid tag, so GT6 world water
	// reports vanilla water scaled by its own quanta; this family now only supplies that scale, not the fluid choice.
	/** The classic family's quanta scale: meta 0 is a full source, otherwise a flowing level.
	 *  That level is quantaPerBlock minus meta, the same two answers this family always gave. */
	@Override protected int engineLevelOfState(net.minecraft.world.level.block.state.BlockState aState) {
		int tMeta = aState.getValue(FLUID_META);
		return tMeta <= 0 ? quantaPerBlock : quantaPerBlock - tMeta;
	}

	// Four block-contract methods are now inherited from a real LiquidBlock instead of manually copied, after
	// re-parenting to it; the custom freeze-tick logic is removed too, since both vanilla freeze paths see it on their own.

	// This whole channel turned out redundant, closed by the engine plus a shared fluid-face-hiding seam and
	// confirmed by a live stand: all water-like blocks share one fluid state whose default hides same-fluid faces.
	// @Override
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return T;
		if (WD.getMaterial(aBlock) == Material.water || WD.visOpq(aBlock)) return F;
		if (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).isAir()) return T; // now just BlockState.isAir()
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity instanceof ITileEntitySurface) return !((ITileEntitySurface)tTileEntity).isSurfaceOpaque(OPOS[aSide]);
		return T;
	}
	
	/** Whether foreign water in a cell may be converted to our own; in 1.7.10 the limiter was a biome list
	 *  because every large water body was itself a biome, but mc26 water sits inside ordinary land biomes now. */
	public boolean canClaim(Level aWorld, int aX, int aY, int aZ) {return T;}

	// A bed that stands still never schedules a tick, so an old seam of plain water would sit there forever.
	// The engine's own random tick is the cheapest pulse there is, and it does nothing at all unless foreign
	// water is actually touching this cell — a scheduled tick is only asked for when there is work to do.
	@Override public boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState aState) {return T;}

	@Override
	public void randomTick(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		if (!plainWaterAround(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()).isEmpty()) aWorld.scheduleTick(aPos, this, tickRate);
	}

	/** Take over foreign water inside own territory and wake the neighbours, so the front keeps moving. */
	public void claimWater(Level aWorld, Iterable<BlockPos> aList) {
		for (BlockPos tCoords : aList) {
			if (!canClaim(aWorld, tCoords.getX(), tCoords.getY(), tCoords.getZ())) continue;
			if (!WD.set(aWorld, tCoords.getX(), tCoords.getY(), tCoords.getZ(), this, 0, WATER_UPDATE_FLAGS)) continue;
			for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
				int tX = tCoords.getX()+i, tZ = tCoords.getZ()+j;
				if (WD.exists(aWorld, tX, tCoords.getY(), tZ) && WD.block(aWorld, tX, tCoords.getY(), tZ) == this)
					aWorld.scheduleTick(new BlockPos(tX, tCoords.getY(), tZ), this, tickRate);
			}
		}
	}

	/** Plain water touching this cell — the cells a claim may take over; own waters are never taken.
	 *  Allocates nothing while there is none, because this runs in every tick of every water cell. */
	public java.util.List<BlockPos> plainWaterAround(Level aWorld, int aX, int aY, int aZ) {
		java.util.List<BlockPos> rList = null;
		for (byte tSide : ALL_SIDES_BUT_TOP) if (WD.water(WD.block(aWorld, aX, aY, aZ, tSide))) {
			if (rList == null) rList = new ArrayListNoNulls<>();
			rList.add(new BlockPos(aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]));
		}
		return rList == null ? java.util.Collections.emptyList() : rList;
	}

	public boolean isSourceBlock(BlockGetter aWorld, int aX, int aY, int aZ) {return WD.block(aWorld, aX, aY, aZ) instanceof BlockWaterlike && WD.meta(aWorld, aX, aY, aZ) == 0;}
	@Override public Block getBlock() {return this;}
	public final String getUnlocalizedName() {return FL.name(mFluid, F);}
	public String getLocalizedName() {return FL.name(mFluid, T);}
	public void registerBlockIcons(Object aIconRegister) {/**/}
	public int getRenderType() {return RendererBlockFluid.RENDER_ID;}
	public int getRenderBlockPass() {return 1;}
	// getLightOpacity lives on the shared ancestor BlockFluidBaseGT now, the central light-opacity bridge; the
	// 1.7.10 value was identical for both hierarchies, so the copy here was a duplicate.
	/** 1:1 with the original: water-like blocks render as vanilla water, not their own fluid's icon; the sprite
	 *  is already held by the central BlockTextureFluid, asked here so 'which texture' stays in one place. */
	@Override public net.minecraft.resources.ResourceLocation getIcon(int aSide, int aMeta) {return renderTexture() instanceof gregapi.render.BlockTextureFluid tTex ? tTex.icon() : null;}
	/** 1:1 with the original: 0x00ffffff means no tint of its own; subclasses with their own hue (Ocean, Swamp) override both
	 *  methods, as the original did. */
	@Override public int getRenderColor(int aMeta) {return 0x00ffffff;}
	@Override public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return 0x00ffffff;}
	
	// The item-form had no model at all since GT6 only injects item models into IRenderedBlock blocks; the
	// render channel now lives in the shared BlockFluidBaseGT ancestor, only the texture staying here as water-like's own.
	private gregapi.render.ITexture mRenderTexture = null;
	@Override public gregapi.render.ITexture renderTexture() {
		if (mRenderTexture == null && CODE_CLIENT) mRenderTexture = gregapi.render.BlockTextureFluid.get(net.minecraft.world.level.material.Fluids.WATER, T);
		return mRenderTexture;
	}

	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return 0;}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return 0;}
	// Blocks that merely contain water (kelp, seagrass, coral, waterlogged blocks) now count as water themselves
	// for displacement, extending 1.7.10's liquid-can't-displace-liquid rule to water containers that didn't exist back then.
	private boolean holdsWater(BlockGetter aWorld, int aX, int aY, int aZ) {return aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getFluidState().is(net.minecraft.tags.FluidTags.WATER);}
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {return !holdsWater(aWorld, aX, aY, aZ) && !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.canDisplace(aWorld, aX, aY, aZ);}
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {return !holdsWater(aWorld, aX, aY, aZ) && !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.displaceIfPossible(aWorld, aX, aY, aZ);}
	public boolean canCollideCheck(int aMeta, boolean aFullHit) {return aFullHit && aMeta == 0;}
	public boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {return !mEffects.isEmpty();}
	public boolean isNormalCube() {return F;}
	public boolean isOpaqueCube() {return F;}
	public boolean func_149730_j() {return F;}
	public boolean getTickRandomly() {return F;}
	// Moved to the shared ancestor for the same reason as getLightOpacity above: both hierarchies
	// had the same 1.7.10 value, and the engine reads it via getShadeBrightness.
	public boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return F;}
	
	public BlockWaterlike addEffect(int aEffectID, int aEffectDuration, int aEffectLevel) {
		mEffects.add(new int[] {aEffectID, aEffectDuration, aEffectLevel});
		return this;
	}
	
	public List<int[]> mEffects = new ArrayListNoNulls<>();
	
	@Override
	public void onHeadInside(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !mEffects.isEmpty() && (FL.gas(mFluid) ? !UT.Entities.isImmuneToBreathingGases(aEntity) : !UT.Entities.isWearingFullChemHazmat(aEntity))) {
			for (int[] tEffects : mEffects) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
			if (getMaterial() != Material.water && SERVER_TIME % 20 == 0) aEntity.hurt(aWorld.damageSources().drown(), 2.0F); // was attackEntityFrom(DamageSource.drown,...); the static field is gone, replaced by DamageSources.drown().
		}
	}
}
