/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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
import gregapi.block.Material;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.HashMap;
import java.util.Map;

import static gregapi.data.CS.*;

/** Forge's own BlockFluidBase ancestor is gone from neo entirely, so it's reproduced here once, centrally,
 *  for both fluid hierarchies; the real GT6 quanta-flow logic lives elsewhere (Ocean/River/Swamp/BlockBaseFluid). */
public abstract class BlockFluidBaseGT extends net.minecraft.world.level.block.LiquidBlock implements IBlock, gregapi.block.IBlockExtendedMetaData, gregapi.render.IRenderedBlock, net.minecraftforge.fluids.IFluidBlock {
	/** Was Forge's BlockFluidBase.displacements + static defaultDisplacements; left empty (safe default) since
	 *  1.7.10 knew one block per door/sign, and neo split those into per-wood-type blocks with no 1:1 mapping to guess at. */
	protected Map<Block, Boolean> displacements = new HashMap<>();

	protected int quantaPerBlock = 8;
	protected float quantaPerBlockFloat = 8F;
	protected int density = 1;
	protected int densityDir = -1;
	protected int tickRate = 20;

	/** Same trick as BlockBase#getMaterial -- a field of our own, replacing the removed neo Material constructor argument. */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}

	/** Same center as BlockBase#setBlockBounds, shared by both fluid blocks (was Forge's Block.setBlockBounds
	 *  inside the BlockFluidBase constructor). */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	/** Same contract as BlockBase#getRenderBounds -- read by GT6BlockModel.applyBounds; without it the fluid's
	 *  quanta height was lost and it drew as a full cube. */
	public float[] getRenderBounds() {return mRenderBounds;}

	/** Was BlockFluidBase(Fluid,Material), which read density/tickRate/etc. straight off Forge's own Fluid object;
	 *  neo's Fluid carries none of that (split into FluidType); this 2-arg overload keeps Forge's own defaults instead. */
	// The 2-arg (Properties,Material) overload is removed along with role inference from material: it had
	// no callers, and a role must be named explicitly, never defaulted silently.

	/** One rule decides the block's engine fluid identity, same as getFluidState below: water material -> vanilla
	 *  WATER, lava -> LAVA, otherwise GT6's own FlowingFluid Source. Registration order guarantees fluids exist first. */
	// ==========================================================================================================
	// The engine asks two independent questions per fluid cell -- which fluid is here, and does the block draw
	// its own model -- and per-subclass answers used to disagree, drawing double geometry; both now come from one shared role.
	public enum EngineRole {VANILLA_WATER, OWN_TAGGED_FLUID, NO_ENGINE_FLUID}

	/** This block's role, declared by its family at construction -- there's no second source of truth. */
	public final EngineRole mEngineRole;

	/** Every live fluid block, for the startup role guard (validateEngineRoles). */
	private static final java.util.List<BlockFluidBaseGT> ALL_FLUID_BLOCKS = new java.util.ArrayList<>();

	/** Same registry, second consumer (client surface-layer registration); no second list, since
	 *  the role already knows who's who. */
	public static java.util.List<BlockFluidBaseGT> allFluidBlocks() {return java.util.Collections.unmodifiableList(ALL_FLUID_BLOCKS);}

	/** "Which fluid does this cell hold" is separate from getFluid() (which must be the ancestor's carrier,
	 *  answering what the block IS for buckets/maps); both carriers store it in their own mFluid, so no second store is needed. */
	public abstract net.minecraft.world.level.material.Fluid ownFluid();

	/** The family's own quanta scale, the only thing families declare themselves; the DECISION lives in the
	 *  center below. >=quantaPerBlock means source, less a flowing cell of that height; the scale is Forge's own, kept 1:1. */
	protected abstract int engineLevelOfState(BlockState aState);

	/** The FIRST engine answer -- the only place a role becomes "which fluid is here". Final on purpose:
	 *  a subclass's own answer is exactly the defect this passport exists to prevent, since two truths would silently diverge. */
	@Override public final net.minecraft.world.level.material.FluidState getFluidState(BlockState aState) {
		switch (mEngineRole) {
			case NO_ENGINE_FLUID: return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
			case VANILLA_WATER: {
				boolean tLava = (mMaterial == Material.lava);
				if (engineLevelOfState(aState) >= quantaPerBlock)
					// A source is getSource(false), not defaultFluidState(): the default state's first boolean value is
					// FALLING=true, which would hand the engine a "falling source", something vanilla water never has.
					return (tLava ? net.minecraft.world.level.material.Fluids.LAVA : net.minecraft.world.level.material.Fluids.WATER).getSource(false);
				return (tLava ? net.minecraft.world.level.material.Fluids.FLOWING_LAVA : net.minecraft.world.level.material.Fluids.FLOWING_WATER)
					.getFlowing(net.minecraft.util.Mth.clamp(engineLevelOfState(aState), 1, 8), false);
			}
			default: { // OWN_TAGGED_FLUID has exactly one carrier on this branch: geothermal water.
				// Uses the cell's OWN fluid (ownFluid), not the ancestor carrier getFluid(): geothermal water's material
				// is water, so its ancestor carrier is vanilla WATER, which is exactly what used to draw instead of the hot source.
				// FluidGT.Source extends FlowingFluid, FluidGT.java:264).
				if (!(ownFluid() instanceof net.minecraft.world.level.material.FlowingFluid tOwn))
					return (mMaterial == Material.lava ? net.minecraft.world.level.material.Fluids.LAVA : net.minecraft.world.level.material.Fluids.WATER).getSource(false); // A source, not the default state (see above).
				int tLevel = net.minecraft.util.Mth.clamp(engineLevelOfState(aState), 1, quantaPerBlock);
				return tLevel >= quantaPerBlock ? tOwn.getSource(false) : tOwn.getFlowing(tLevel, false);
			}
		}
	}

	/** SECOND engine answer from the same role: the block's own model draws if and only if the engine doesn't
	 *  draw the cell as a fluid -- one geometry per cell. Final, for the same reason as getFluidState. */
	@Override public final net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
		return mEngineRole == EngineRole.NO_ENGINE_FLUID
			? net.minecraft.world.level.block.RenderShape.MODEL
			: net.minecraft.world.level.block.RenderShape.INVISIBLE;
	}

	/** THIRD answer from the same role: does a fluid-seeking ray see this cell? 1.7.10 let a normal aim miss
	 *  the fluid but a seeking ray (tooltips) saw only a full cell; 1.20.1 took the channel away, restored here. */
	// No consumer right now: the tooltip wrapper over a foreign ray was removed by a later decision; the
	// role's answer stays until block-property visibility is settled properly.
	public final boolean isFullFluidCell(BlockState aState) {return engineLevelOfState(aState) >= quantaPerBlock;}

	/** Invisible to the engine's fluid ray under role NO_ENGINE_FLUID (FluidState empty); asked of the state,
	 *  not a block list, so a new carrier picks this up automatically. */
	// No consumer right now: the tooltip wrapper over a foreign ray was removed by a later decision; the
	// role's answer stays until block-property visibility is settled properly.
	public final boolean isInvisibleToFluidClip(BlockState aState) {return aState.getFluidState().isEmpty();}

	/** Startup guard for exactly what the compiler can't catch: a role that claims to be an engine fluid must
	 *  actually sit in the water/lava tag, or swimming/drowning/flow silently die; the geometry invariant is just a safety net. */
	public static void validateEngineRoles() {
		for (BlockFluidBaseGT tBlock : ALL_FLUID_BLOCKS) {
			net.minecraft.world.level.material.FluidState tFs = tBlock.defaultBlockState().getFluidState();
			boolean tPromises = tBlock.mEngineRole != EngineRole.NO_ENGINE_FLUID;
			if (tPromises && !tFs.is(net.minecraft.tags.FluidTags.WATER) && !tFs.is(net.minecraft.tags.FluidTags.LAVA))
				gregapi.data.CS.ERR.println("[GT6] РАССИНХРОН РОЛИ ЖИДКОСТИ: " + tBlock + " роль=" + tBlock.mEngineRole
					+ " объявляет движку среду, но её жидкости нет в теге воды/лавы — плавание и утопление в ней МЕРТВЫ."
					+ " Проверь data/minecraft/tags/fluids/water.json (обе записи: source и flowing; каталог на 1.20.1"
					+ " именно tags/fluids — TagManager.java:20, в tags/fluid движок не заглянет).");
			boolean tDrawnAsFluid = !tFs.isEmpty();
			boolean tDrawnAsModel = tBlock.defaultBlockState().getRenderShape() != net.minecraft.world.level.block.RenderShape.INVISIBLE;
			if (tDrawnAsFluid == tDrawnAsModel) gregapi.data.CS.ERR.println("[GT6] РАССИНХРОН РОЛИ ЖИДКОСТИ: " + tBlock
				+ " роль=" + tBlock.mEngineRole + " — клетка будет нарисована "
				+ (tDrawnAsFluid ? "ДВАЖДЫ (и жидкостью, и моделью)" : "НИ РАЗУ") + " (BP-BUG-003).");
		}
	}

	protected static net.minecraft.world.level.material.FlowingFluid liquidCarrierFor(Material aMaterial, net.minecraft.world.level.material.Fluid aFluid) {
		if (aMaterial == Material.water) return net.minecraft.world.level.material.Fluids.WATER;
		if (aMaterial == Material.lava ) return net.minecraft.world.level.material.Fluids.LAVA;
		if (aFluid instanceof net.minecraft.world.level.material.FlowingFluid tFlowing) return tFlowing;
		return net.minecraft.world.level.material.Fluids.WATER; // Unreachable in live registration (every caller carries a GT6 Source); a safe identity fallback.
	}

	/** Carries Fluid characteristics onto the block 1:1 from Forge's BlockFluidBase(Fluid,Material); the
	 *  data-holder fields now live in the central FluidGT registry; luminosity/temperature aren't carried: nothing reads them. */
	public BlockFluidBaseGT(BlockBehaviour.Properties aProperties, Material aMaterial, net.minecraft.world.level.material.Fluid aFluid, EngineRole aRole) {
		// Super is LiquidBlock(FlowingFluid,Properties), so the block IS a fluid to the engine;
		// its own stateCache/LEVEL channels go unused, since getFluidState and quanta are GT6's own, from the role passport above.
		super(liquidCarrierFor(aMaterial, aFluid), aProperties);
		mMaterial = aMaterial;
		mEngineRole = aRole;
		ALL_FLUID_BLOCKS.add(this);
		registerDefaultState(getStateDefinition().any().setValue(FLUID_META, 0).setValue(LEVEL, 0));
		gregapi.fluid.FluidGT tFluid = gregapi.fluid.FluidGT.of(aFluid);
		if (tFluid != null) {
			density    = tFluid.getDensity();
			tickRate   = tFluid.getViscosity() / 200;
			densityDir = tFluid.getDensity() > 0 ? -1 : 1;
		}
	}

	// Forge's BlockFluidFinite stored quanta in block meta (0..7); neo's meta carrier is a blockstate
	// property instead (like vanilla LiquidBlock.LEVEL), routed through WD.set/WD.meta so the quanta logic works unmodified.
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLUID_META =
		net.minecraft.world.level.block.state.properties.IntegerProperty.create("gt6_meta", 0, 15);

	// LEVEL is declared only because the ancestor's constructor requires it; the real quanta carrier is
	// FLUID_META, and LEVEL stays 0, unread by anyone (all its LiquidBlock channels are overridden).
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(FLUID_META, LEVEL);
	}

	public void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		if (!(aWorld instanceof net.minecraft.world.level.LevelAccessor tLevel)) return;
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockState tState = tLevel.getBlockState(tPos);
		if (tState.getBlock() == this) tLevel.setBlock(tPos, tState.setValue(FLUID_META, aMetaData & 15), FLUID_UPDATE_FLAGS_META);
	}
	public short getExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockState tState = aWorld.getBlockState(new BlockPos(aX, aY, aZ));
		return (short)(tState.getBlock() == this ? tState.getValue(FLUID_META) : 0);
	}
	/** Flag 2 (send to client, no neighbor notify): a meta write shouldn't cascade updates.
	 *  GT6's own logic already does the cascading. */
	protected static final int FLUID_UPDATE_FLAGS_META = 2;

	// 1.7.10's World.scheduleBlockUpdate/Block.updateTick becomes neo's BlockBehaviour.tick;
	// Forge's onBlockAdded (which scheduled the first tick) becomes neo's onPlace, 1:1.
	public void updateTick(Level aWorld, int aX, int aY, int aZ, java.util.Random aRandom) {/* overridden by BlockBaseFluid/Ocean/River/Swamp */}
	@Override public void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), gregapi.util.UT.Code.random(aRandom)); // the RandomSource-to-Random converter lives in the single center UT.Code.random
	}
	@Override public void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {
		onBlockAdded(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
	}
	/** Was Forge's BlockFluidBase.onBlockAdded, body 1:1. The dispatch from onPlace is mandatory: Ocean/River/Swamp
	 *  override it for their placement gate and starting tick, or their channel was orphaned (mud never converted). */
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
	}
	@Override public void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, BlockPos aFromPos, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}

	// Neutralizes the inherited vanilla flow: LiquidBlock schedules vanilla fluid ticks in onPlace/neighborChanged/
	// updateShape; the first two are overridden above, and this closes the third, or vanilla's tick would double-spill GT6's.
	@Override public BlockState updateShape(BlockState aState, net.minecraft.core.Direction aDirection, BlockState aNeighborState, net.minecraft.world.level.LevelAccessor aWorld, BlockPos aPos, BlockPos aNeighborPos) {
		return aState;
	}

	// LiquidBlock delegates randomTick to FluidState (lava's vanilla fires this way, which none of GT6's
	// 1.7.10 fluids ever had); false is the pre-reparenting default, and a subclass with its own channel overrides it.
	@Override public boolean isRandomlyTicking(BlockState aState) {return F;}

	/** Bucket pickup matches vanilla 1.7.10 1:1: water material at meta 0 empties to a water bucket, lava
	 *  material at meta 0 to a lava bucket, otherwise nothing (oils/gas only ever drained through GT6's own drain()). */
	@Override public net.minecraft.world.item.ItemStack pickupBlock(net.minecraft.world.level.LevelAccessor aLevel, BlockPos aPos, BlockState aState) {
		if (aState.getValue(FLUID_META) != 0) return net.minecraft.world.item.ItemStack.EMPTY;
		net.minecraft.world.item.Item tBucket = mMaterial == Material.water ? net.minecraft.world.item.Items.WATER_BUCKET : mMaterial == Material.lava ? net.minecraft.world.item.Items.LAVA_BUCKET : null;
		if (tBucket == null) return net.minecraft.world.item.ItemStack.EMPTY;
		aLevel.setBlock(aPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
		return new net.minecraft.world.item.ItemStack(tBucket);
	}

	public abstract int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ);

	/** Was Forge's BlockFluidBase.onNeighborBlockChange, body 1:1.
	 *  Needed by BlockOcean/BlockRiver, which call super after their own logic. */
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
	}

	/** Was Forge's BlockFluidBase.canDisplace, body 1:1. */
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockPos aPos = new BlockPos(aX, aY, aZ);
		if (aWorld.getBlockState(aPos).isAir()) return T; // Was block.isAir(world,x,y,z) -- BlockState.isAir().
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == this) return F;
		if (displacements.containsKey(aBlock)) return displacements.get(aBlock);
		Material aBlockMaterial = WD.getMaterial(aBlock);
		if (aBlockMaterial.blocksMovement() || aBlockMaterial == Material.portal) return F;
		int tDensity = getDensity(aWorld, aX, aY, aZ);
		if (tDensity == Integer.MAX_VALUE) return T;
		return this.density > tDensity;
	}

	/** Was Forge's BlockFluidBase.displaceIfPossible, body 1:1. At density==MAX_VALUE the Forge original
	 *  dropped the displaced block before displacing it; that drop is restored here (it had silently become a no-op). */
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {
		BlockPos aPos = new BlockPos(aX, aY, aZ);
		if (aWorld.getBlockState(aPos).isAir()) return T;
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == this) return F;
		if (displacements.containsKey(aBlock)) return displacements.get(aBlock);
		Material aBlockMaterial = WD.getMaterial(aBlock);
		if (aBlockMaterial.blocksMovement() || aBlockMaterial == Material.portal) return F;
		int tDensity = getDensity(aWorld, aX, aY, aZ);
		if (tDensity == Integer.MAX_VALUE) {
			if (aWorld instanceof net.minecraft.server.level.ServerLevel) net.minecraft.world.level.block.Block.dropResources(aWorld.getBlockState(aPos), aWorld, aPos); // Forge's own drop of the displaced block.
			return T;
		}
		return this.density > tDensity;
	}

	/** Was Forge's static BlockFluidBase.getDensity(IBlockAccess,x,y,z). */
	public static int getDensity(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (!(aBlock instanceof BlockFluidBaseGT)) return Integer.MAX_VALUE;
		return ((BlockFluidBaseGT)aBlock).density;
	}

	/** Was Forge's final BlockFluidBase.getQuantaValueBelow(...), body 1:1. */
	public final int getQuantaValueBelow(BlockGetter aWorld, int aX, int aY, int aZ, int aBelowThis) {
		int tQuantaRemaining = getQuantaValue(aWorld, aX, aY, aZ);
		if (tQuantaRemaining >= aBelowThis) return -1;
		return tQuantaRemaining;
	}

	/** Render-side density-direction accessor (was 1.7.10's FL.dir(BlockFluidBase) / a direct field). */
	public int dir() {return densityDir;}

	// IFluidBlock surface (see class header).
	/** The block's fluid. In 1.7.10 it came from the Forge ancestor; here both carriers already know it
	 *  themselves in their own mFluid, so no second store is needed. */
	@Override public abstract net.minecraft.world.level.material.FlowingFluid getFluid();

	/** drain isn't declared here: both carriers (BlockWaterlike, BlockBaseFluid) already have their own bodies,
	 *  1:1 with 1.7.10, where they were @Override of this same interface. */

	/** canDrain isn't implemented here either: Forge gave it two DIFFERENT bodies on different subclasses
	 *  (source-only vs. always-true); one shared default here would fake a rule for one of them, so it stays with the carriers. */

	/** Was Forge's BlockFluidBase.getFilledPercentage, body 1:1: quanta+1, capped at 1.0, sign follows
	 *  density (gas's negative density flips the sign, distinguishing "filled from below" from "above", as Forge did). */
	@Override public float getFilledPercentage(Level aWorld, BlockPos aPos) {
		int tQuantaRemaining = getQuantaValue(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) + 1;
		float tRemaining = tQuantaRemaining / quantaPerBlockFloat;
		if (tRemaining > 1) tRemaining = 1.0F;
		return tRemaining * (density > 0 ? 1 : -1);
	}

	/** A new IFluidBlock surface the 1.7.10 shim never had and GT6 never called (filling goes through
	 *  BucketItem.emptyContents instead); implemented honestly (a full source for 1000mB), not an invented partial fill. */
	@Override public int place(Level aWorld, BlockPos aPos, FluidStack aFluidStack, IFluidHandler.FluidAction aAction) {
		if (aFluidStack == null || aFluidStack.getAmount() < 1000) return 0;
		if (aAction.execute()) WD.set(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), this, 0, 3);
		return 1000;
	}

	/** Was Forge's final BlockFluidBase.getQuantaPercentage, body 1:1. */
	public final float getQuantaPercentage(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getQuantaValue(aWorld, aX, aY, aZ) / quantaPerBlockFloat;
	}

	/** Was 1.7.10's Block.isBlockSolid, body = material.isSolid() (same trick as BlockWaterlike). */
	protected boolean isBlockSolid(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		return WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isSolid();
	}

	/** Was Forge's BlockFluidBase.getFlowVector, body 1:1; read by the render layer (RendererBlockFluid) to
	 *  orient the surface texture by flow direction. */
	public net.minecraft.world.phys.Vec3 getFlowVector(BlockGetter aWorld, int aX, int aY, int aZ) {
		net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(0, 0, 0);
		int decay = quantaPerBlock - getQuantaValue(aWorld, aX, aY, aZ);
		for (int side = 0; side < 4; ++side) {
			int x2 = aX, z2 = aZ;
			switch (side) {
			case 0: --x2; break;
			case 1: --z2; break;
			case 2: ++x2; break;
			default: ++z2; break;
			}
			int otherDecay = quantaPerBlock - getQuantaValue(aWorld, x2, aY, z2);
			if (otherDecay >= quantaPerBlock) {
				if (!WD.getMaterial(WD.block(aWorld, x2, aY, z2)).blocksMovement()) {
					otherDecay = quantaPerBlock - getQuantaValue(aWorld, x2, aY - 1, z2);
					if (otherDecay >= 0) {
						int power = otherDecay - (decay - quantaPerBlock);
						vec = vec.add((x2 - aX) * power, 0, (z2 - aZ) * power);
					}
				}
			} else if (otherDecay >= 0) {
				int power = otherDecay - decay;
				vec = vec.add((x2 - aX) * power, 0, (z2 - aZ) * power);
			}
		}
		if (WD.block(aWorld, aX, aY + 1, aZ) == this) {
			boolean flag =
				isBlockSolid(aWorld, aX    , aY    , aZ - 1, (byte)2) ||
				isBlockSolid(aWorld, aX    , aY    , aZ + 1, (byte)3) ||
				isBlockSolid(aWorld, aX - 1, aY    , aZ    , (byte)4) ||
				isBlockSolid(aWorld, aX + 1, aY    , aZ    , (byte)5) ||
				isBlockSolid(aWorld, aX    , aY + 1, aZ - 1, (byte)2) ||
				isBlockSolid(aWorld, aX    , aY + 1, aZ + 1, (byte)3) ||
				isBlockSolid(aWorld, aX - 1, aY + 1, aZ    , (byte)4) ||
				isBlockSolid(aWorld, aX + 1, aY + 1, aZ    , (byte)5);
			if (flag) vec = vec.normalize().add(0.0D, -6.0D, 0.0D);
		}
		return vec.normalize();
	}

	/** Was Forge's static BlockFluidBase.getFlowDirection, body 1:1 (with an instanceof guard before the cast,
	 *  since it's only called on the fluid's own position). */
	public static double getFlowDirection(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (!(tBlock instanceof BlockFluidBaseGT) || !WD.getMaterial(tBlock).isLiquid()) return -1000.0D;
		net.minecraft.world.phys.Vec3 vec = ((BlockFluidBaseGT)tBlock).getFlowVector(aWorld, aX, aY, aZ);
		return vec.x == 0.0D && vec.z == 0.0D ? -1000.0D : Math.atan2(vec.z, vec.x) - Math.PI / 2D;
	}

	// 1.7.10 centralized fluid rendering in one handler shared by both hierarchies (same Forge ancestor); the
	// port restored the neo equivalent only for BlockBaseFluid, leaving water-like blocks with no item model -- fixed here.

	// Light-opacity center: both fluid hierarchies answered identically in 1.7.10 (LIGHT_OPACITY_WATER=3),
	// but the value was duplicated in both subclasses and the engine never asked at all; the bridge now lives here once.
	@Override public int getLightBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return getLightOpacity(aState);}

	/** Light dampening for a given state. Both 1.7.10 hierarchies shared one value, LIGHT_OPACITY_WATER. */
	public int getLightOpacity(net.minecraft.world.level.block.state.BlockState aState) {return gregapi.data.CS.LIGHT_OPACITY_WATER;}

	// Shade center: both hierarchies answered the same in 1.7.10 (renderAsNormalBlock()==false, so a fluid
	// never darkened its neighbors); neo reads this from collision instead, so the bridge lives here once now.
	@Override public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** Body 1:1 with 1.7.10's Block.isBlockNormalCube; see BlockBase for the same method. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}

	/** 1.7.10 rule for both fluid hierarchies, folded into the shared ancestor (the subclass copies were duplicates). */
	public boolean renderAsNormalBlock() {return gregapi.data.CS.F;}

	/** Fluid texture for both render paths (world + item form). Client-only: BlockTextureFluid.get is gated under CODE_CLIENT. */
	public abstract gregapi.render.ITexture renderTexture();

	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, net.minecraft.world.item.ItemStack aStack) {return renderTexture();}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return renderTexture();}
	@Override public boolean usesRenderPass(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return aRenderPass == 0;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return F;}
	/** Defaults to a full cube; BlockBaseFluid overrides the quanta surface height for its own world render. */
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(net.minecraft.world.item.ItemStack aStack) {return 1;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(net.minecraft.world.item.ItemStack aStack) {return null;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}

	/** Harvest permission is judged by the center WD.canHarvestBlock; this is only the call site.
	 *  The rule moved here from PlayerEvent.HarvestCheck, which in 1.20.1 carries neither world nor position. */
	@Override public boolean canHarvestBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer) {
		return gregapi.util.WD.canHarvestBlock(aState, aWorld, aPos, aPlayer);
	}

}
