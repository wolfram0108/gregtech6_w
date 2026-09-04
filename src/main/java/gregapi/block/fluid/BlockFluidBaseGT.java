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

import java.util.HashMap;
import java.util.Map;

import static gregapi.data.CS.*;

/**
 * F5 engine force (decisions/F5-fluids.md §5): in 1.7.10 {@link BlockWaterlike} and {@link BlockBaseFluid} shared
 * ONE common ancestor — Forge {@code net.minecraftforge.fluids.BlockFluidBase} (quanta-fluidity: quantaPerBlock/
 * density/densityDir/tickRate/displacements fields + canDisplace/displaceIfPossible/getDensity/
 * getQuantaValueBelow methods). The class is gone in neo (absent from all 3 reference roots) — GT6 itself
 * never wrote this class (it's a third-party Forge library), so the ancestor is reproduced here ONCE (centralization
 * §3, F5 report §5 "custom Block base class"), bodies 1:1 from Forge 1.7.10
 * {@code BlockFluidBase}/{@code BlockFluidClassic}/{@code BlockFluidFinite} (only the API surface is remapped —
 * BlockGetter/Level/BlockPos instead of IBlockAccess/World/int triples; {@code Material.func_149688_o()} ->
 * {@link WD#getMaterial(Block)}, {@code material.func_76230_c()} -> {@code Material.blocksMovement()},
 * {@code Material.field_151567_E} -> {@code Material.portal} — cross-checked against `methods.csv`/`fields.csv` MCP 1.7.10).
 * The GT6 quanta-flow logic actually in use (Ocean/River/Swamp updateTick, BlockBaseFluid.updateTick) is
 * GT6's OWN, not from here; only what is actually invoked through an unqualified/{@code super.} name
 * from {@link BlockWaterlike}/{@link BlockBaseFluid} made it in here (canDisplace/displaceIfPossible/getQuantaValueBelow/
 * getDensity) — the {@code BlockFluidClassic} tick tail that is dead in GT6 (getOptimalFlowDirections/
 * calculateFlowCost/flowIntoBlock/canFlowInto/isFlowingVertically — never called, GT6 overrides the
 * tick entirely in Ocean/River/Swamp and never calls {@code super.updateTick}) was not ported — we don't invent
 * dead code.
 *
 * <p><b>F5 surface-B (2026-07-30): the ancestor is {@link LiquidBlock}, not {@code Block}.</b> In 1.7.10 the common
 * Forge ancestor carried fluid IDENTITY (the {@code IFluidBlock} interface), and the whole engine+mods saw the GT6 block
 * as a fluid. The port reproduced the fluidity but lost the identity: every engine path that selects by
 * {@code instanceof LiquidBlock} could not see the GT6 fluid ({@code Biome.shouldFreeze:161} — freezing,
 * {@code SnowAndFreezeFeature:34} — worldgen ice, {@code SpongeBlock:66-69} — sponge,
 * {@code LavaFluid.spreadTo:218} — lava+water→stone, {@code SpawnEggItem:108}, {@code LevelChunk:587}), plus
 * the vanilla bucket ({@code BucketItem} → {@code BucketPickup}). Identity is restored via inheritance;
 * FLUIDITY stays GT6's quanta-based own: all {@code LiquidBlock} tick channels are overridden right here
 * ({@link #onPlace}/{@link #neighborChanged}/{@link #tick}/{@link #updateShape}/{@link #isRandomlyTicking}) —
 * the vanilla fluid tick is NEVER scheduled, so there is no double spill.
 *
 * <p><b>BUG-115 (2026-08-10): the second half of identity — {@code IFluidBlock}.</b> The re-parenting above restored
 * identity for the ENGINE ({@code instanceof LiquidBlock}), but not for the MOD: in 1.7.10 both hierarchies got
 * {@code net.minecraftforge.fluids.IFluidBlock} from the same Forge ancestor ({@code BlockWaterlike extends
 * BlockFluidClassic}, {@code BlockBaseFluid extends BlockFluidFinite} -> {@code BlockFluidBase implements
 * IFluidBlock}), and the whole mod selected fluids by exactly that. The port reproduced the ancestor but lost the
 * interface — eight live branches always returned {@code false}: the pump ({@code MultiTileEntityPump:193,225}), the
 * Drain cover ({@code CoverDrain:149,153}), both bucket behaviors ({@code Behavior_Bucket_Simple:103,160},
 * {@code Behavior_Bucket_Container:80,94}), tanks ({@code TileEntityBase08FluidContainer:334,348}) and three
 * gauges ({@code Bucketometer}/{@code Fluidometer}/{@code KiloBucketometer}:64). The bodies were intact and
 * marked {@code // @Override} the whole time — the code was alive, the channel was just severed. Measurement {@code [GT6-PUMPPROBE]}:
 * the pump drained the ocean and the swamp (36 of 36 blocks) and collected 0 mb — the fluid was destroyed.
 * The interface is restored HERE, in the common ancestor, exactly where Forge carried it: all eight branches come
 * back to life at once, not a single caller needs a fix.
 */
public abstract class BlockFluidBaseGT extends net.minecraft.world.level.block.LiquidBlock implements IBlock, gregapi.block.IBlockExtendedMetaData, gregapi.render.IRenderedBlock, net.minecraftforge.fluids.IFluidBlock {
	/** Was Forge {@code BlockFluidBase.displacements} + the static {@code defaultDisplacements}
	 *  (wooden_door/iron_door/standing_sign/wall_sign/reeds -> false). F5 data default (doors/signs/reeds are not displaced by fluid — a block set, not a stub):
	 *  1.7.10 knew ONE block per door/sign; neo split it into a block per wood type (no 1:1 mapping without
	 *  guessing the full list — REMAP-RULES "don't invent"), so the map is left empty (safe default:
	 *  doors/signs already return false through the material.blocksMovement() branch). */
	protected Map<Block, Boolean> displacements = new HashMap<>();

	protected int quantaPerBlock = 8;
	protected float quantaPerBlockFloat = 8F;
	protected int density = 1;
	protected int densityDir = -1;
	protected int tickRate = 20;

	/** F9: see {@link gregapi.block.BlockBase#getMaterial()} — the same approach (own field instead of
	 *  the removed neo {@code Material} constructor argument of Block). */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}

	// ================= ROLE PASSPORT: what the fluid block IS to the engine (BUG-120, cleanup) =====
	// The role is declared EXPLICITLY when a family is constructed and turned, in ONE place (here), into both
	// engine answers: which fluid the cell declares (getFluidState) and whether the block draws its own model (getRenderShape).
	// Before the passport, the role was COMPUTED by conditions in three places (getFluidState of both families + a render
	// rule that water-likes carried as an implicit INVISIBLE inheritance) — understanding swamp behavior required
	// reading all three.
	//
	// | Role             | Who (all 10 world fluids)          | Answer to the engine                | Render                |
	// |------------------|------------------------------------|------------------------------------|-----------------------|
	// | VANILLA_WATER    | ocean, river, swamp, (soda)        | vanilla water by quanta — otherwise| engine fluid pass     |
	// |                  |                                    | 47 waterlogging branches and       |                       |
	// |                  |                                    | freezing (is(WATER) identity) die  |                       |
	// | OWN_TAGGED_FLUID | geothermal (water/lava material)   | ITS OWN fluid by quanta; medium is | engine fluid pass     |
	// |                  |                                    | via a TAG (data/minecraft/tags/    | with its own texture  |
	// |                  |                                    | fluid/water.json); guard below     |                       |
	// | NO_ENGINE_FLUID  | 4 oils, gas                        | EMPTY — not a fluid to the engine; | GT6 model at          |
	// |                  |                                    | oil medium is a mod channel        | quanta height         |
	// |                  |                                    | setMedium (BlockBaseFluid)         |                       |
	//
	// THIRD CONSEQUENCE OF THE OWN_TAGGED_FLUID ROLE — THE FLUID HAS NO DIRECTIONAL FLOW (N-8; the owner's
	// snapshot from 1.7.10: the geo-water surface is FLAT, only an animation ripple — no wedges, no flow pattern).
	// The reference drew it with ONE icon and no flow strip (`getStillIcon()==getFlowingIcon()`, both
	// `mTexture.getIcon(0)`, `gt6-original/gregapi/fluid/FluidGT.java:85-87`); with its own tessellator class
	// (`RendererBlockFluid`, ISimpleBlockRenderingHandler) — directional flow never happened there, this is a
	// PROPERTY OF THE FLUID ITSELF, not an artifact of how it used to be drawn. The port moved this role onto the
	// engine fluid pass (see above) — and the engine's flow formula (`FlowingFluid.getFlow`, UV rotated by the
	// flow angle in `FluidRenderer.tesselate:151-175`) expects a strip texture like `water_flow.png`;
	// feeding it the same animated "still" icon (both the still/flow roles are the same sprite,
	// `GT_API_Proxy_Client.onRegisterFluidModels:306`) produces an incoherent pattern instead of a flat surface.
	// This is told to the engine DIRECTLY, on the fluid itself — `FluidGT.Source.getFlow`/`FluidGT.Flowing.getFlow`
	// (`gregapi/fluid/FluidGT.java:278,308`) return a zero vector on both engine arms (full cell / partial quantum)
	// — not by suppressing the draw, but by honestly saying "this fluid has nowhere to flow". There is only one
	// carrier right now — geo-water; BUT THIS IS A PROPERTY OF THE ROLE, not a named patch for one fluid: the next
	// `OWN_TAGGED_FLUID` carrier inherits it automatically, no second formula is needed.
	//
	// The engine executes both answers INDEPENDENTLY (SectionCompiler:99-104 — fluid, :106 — model): declaring both
	// means drawing the cell twice (BUG-119). The rule "model ⟺ no fluid" is derived from the role right here.
	// Entity medium is selected ONLY by water/lava tags (Entity:251, EntityFluidInteraction:121-129) —
	// there is no third medium in the 26.1 engine, NeoForge FluidType physics is never invoked (their patch was lost since 26.1-snapshot-8).
	public enum EngineRole {VANILLA_WATER, OWN_TAGGED_FLUID, NO_ENGINE_FLUID}
	public final EngineRole mEngineRole;

	/** All live fluid blocks — for the role guard at server start ({@link #validateEngineRoles}). */
	private static final java.util.List<BlockFluidBaseGT> ALL_FLUID_BLOCKS = new java.util.ArrayList<>();

	/** Quanta from state — each family has its own scale (finite: meta+1 grows with amount;
	 *  classic: quantaPerBlock−meta, meta 0 = a full source). Inherited from Forge Finite/Classic, 1:1. */
	protected abstract int quantaOfState(BlockState aState);

	/** The ONLY place where the role turns into the engine's answer "what fluid is here". The level is always
	 *  derived from ONE measure — the block's quanta: a full cell → a source, a partial one → a flow at that same height. */
	@Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState aState) {
		switch (mEngineRole) {
			case NO_ENGINE_FLUID: return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
			case VANILLA_WATER: {
				int tQuanta = quantaOfState(aState);
				// BUG-141-A: the SOURCE is declared via getSource(false), NOT defaultFluidState(). The fluid's
				// default state is stateDefinition.any() (Fluid.java:37-38), and the first FALLING value is true
				// (BooleanProperty.VALUES = [true,false]) → the engine got a "falling source", which vanilla
				// water never has: the engine itself builds a source as fluid.getSource(false) (LiquidBlock.java:71).
				// Consequences: FlowingFluid.getFlow:90 produced a nonzero downward vector, and WalkNodeEvaluator saw
				// a different path start. The sibling branch of this same method (OWN_TAGGED_FLUID) always did it
				// right — the discrepancy was within a single center.
				if (tQuanta >= quantaPerBlock) return net.minecraft.world.level.material.Fluids.WATER.getSource(false);
				return net.minecraft.world.level.material.Fluids.FLOWING_WATER.getFlowing(net.minecraft.util.Mth.clamp(tQuanta, 1, 8), false);
			}
			default: { // OWN_TAGGED_FLUID
				if (!(getFluid() instanceof net.minecraft.world.level.material.FlowingFluid tOwn))
					return (mMaterial == Material.lava ? net.minecraft.world.level.material.Fluids.LAVA : net.minecraft.world.level.material.Fluids.WATER).getSource(false); // BUG-141-A: a source, not the default state (see above)
				int tQuanta = net.minecraft.util.Mth.clamp(quantaOfState(aState), 1, quantaPerBlock);
				if (tQuanta >= quantaPerBlock) return tOwn.getSource(false);
				return tOwn.getFlowing(tQuanta, false);
			}
		}
	}

	/** The second engine answer derived from the SAME role: the block draws its own model if and only if the engine
	 *  does not draw it as a fluid — one geometry per cell (BUG-119). */
	@Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
		return mEngineRole == EngineRole.NO_ENGINE_FLUID ? net.minecraft.world.level.block.RenderShape.MODEL : net.minecraft.world.level.block.RenderShape.INVISIBLE;
	}

	/** ROLE GUARD (called at server start, GT_API_Proxy.onProxyBeforeServerStarted): the OWN_TAGGED_FLUID role
	 *  promises the engine a medium via a TAG, and that promise lives in a data file (tags/fluid/water.json) — no
	 *  compiler catches a code/file desync, swimming would just silently die. A loss must announce itself. */
	public static void validateEngineRoles() {
		for (BlockFluidBaseGT tBlock : ALL_FLUID_BLOCKS) {
			if (tBlock.mEngineRole != EngineRole.OWN_TAGGED_FLUID) continue;
			net.minecraft.world.level.material.FluidState tFs = tBlock.defaultBlockState().getFluidState();
			if (!tFs.is(net.minecraft.tags.FluidTags.WATER) && !tFs.is(net.minecraft.tags.FluidTags.LAVA))
				gregapi.data.CS.ERR.println("[GT6] FLUID ROLE MISMATCH: " + tBlock + " declares its own fluid as a medium, yet it is missing from the water/lava tag — swimming in it is DEAD. Check data/minecraft/tags/fluid/*.json (both entries: source and flowing).");
		}
	}

	/** F-bounds: see {@link gregapi.block.BlockBase#setBlockBounds} — the same center-approach, shared by BOTH
	 *  fluid blocks (was Forge {@code Block.setBlockBounds} inside the {@code BlockFluidBase} constructor). */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	/** The same contract as {@link gregapi.block.BlockBase#getRenderBounds()} — read by GT6BlockModel.applyBounds
	 *  (without this the fluid's quanta-height was lost and the block rendered as a full cube). */
	public float[] getRenderBounds() {return mRenderBounds;}

	/** F16/F9 engine force: was {@code BlockFluidBase(Fluid,Material)}, which read density/temperature/
	 *  maxScaledLight/tickRate/densityDir FROM the Forge {@code Fluid} object ITSELF (data-holder fields) — neo's
	 *  {@code net.minecraft.world.level.material.Fluid} carries none of these fields (the data is split into
	 *  {@code FluidType}, F5 report §1/§3). Transferring the characteristics is reproduced by the Fluid overload below
	 *  (data from {@link gregapi.fluid.FluidGT}); this 2-arg overload keeps the Forge defaults
	 *  (density=1, densityDir=-1, tickRate=20, quantaPerBlock=8). */
	// The 2-arg overload (Properties, Material) was REMOVED when the role passport was introduced: there were no
	// callers (both families go through the full form), and the role must be named explicitly — no silent default.

	/** F5 surface-B: the block's engine fluid identity — ONE rule, the same one {@code getFluidState}
	 *  uses in both hierarchies: material water → vanilla WATER, lava → LAVA (that's the FluidState the block returns),
	 *  otherwise — GT6's own {@link net.minecraft.world.level.material.FlowingFluid} (Source; the block does NOT
	 *  return its FluidState — identity without physics). Registry order guarantees GT6 fluids are bound by the time
	 *  blocks are constructed: FLUID is registered BEFORE BLOCK ({@code BuiltInRegistries.java:178,180} +
	 *  {@code GameData.getRegistrationOrder} — the vanilla order). */
	private static net.minecraft.world.level.material.FlowingFluid liquidCarrierFor(Material aMaterial, net.minecraft.world.level.material.Fluid aFluid) {
		if (aMaterial == Material.water) return net.minecraft.world.level.material.Fluids.WATER;
		if (aMaterial == Material.lava ) return net.minecraft.world.level.material.Fluids.LAVA;
		if (aFluid instanceof net.minecraft.world.level.material.FlowingFluid tFlowing) return tFlowing;
		return net.minecraft.world.level.material.Fluids.WATER; // unreachable under live registration (every caller carries a GT6 Source); safe identity carrier
	}

	/** Fluid→block characteristic transfer 1:1 with Forge {@code BlockFluidBase(Fluid,Material)} (:68-72):
	 *  {@code density = fluid.density; tickRate = fluid.viscosity / 200; densityDir = density > 0 ? -1 : 1}.
	 *  In neo, the Fluid's data-holder fields live in {@link gregapi.fluid.FluidGT} (F5) — the {@code FluidGT.of(Fluid)} center.
	 *  Hence: gas (density −500) flows UP (densityDir=+1), oils carry densities 600-900, waters 1000;
	 *  tickRate: LIQUID 1000/200=5 (like vanilla water), GAS 200/200=1. Subclasses that need a different tickRate
	 *  reset it AFTER super (Ocean/River/Swamp 20/20/10 — 1:1 with the source).
	 *  {@code maxScaledLight} (luminosity) is NOT transferred: all 10 world fluids have luminosity=0
	 *  (Loader_Fluids: waters/oils/gas without setLuminosity) — we don't invent a dead field.
	 *  {@code temperature} is NOT transferred: nothing in the port reads it (the Forge-static getTemperature was not ported). */
	public BlockFluidBaseGT(BlockBehaviour.Properties aProperties, Material aMaterial, net.minecraft.world.level.material.Fluid aFluid, EngineRole aRole) {
		// F5 surface-B: super = LiquidBlock(FlowingFluid, Properties) — the block IS a fluid to the engine.
		// Its stateCache/LEVEL channels are unused (getFluidState/quanta are GT6's own, role passport above).
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

	// META MODEL (1.7.10 quanta): Forge BlockFluidFinite stored quanta IN the block's meta (0..7 → 1..8 quanta);
	// neo's numeric-meta carrier = a blockstate property (like vanilla LiquidBlock.LEVEL 0..15). The WD.set/WD.meta
	// channel (IBlockExtendedMetaData) → all the verbatim quanta logic (updateTick/drain/updateFluidBlocks) comes back to life unchanged.
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLUID_META =
		net.minecraft.world.level.block.state.properties.IntegerProperty.create("gt6_meta", 0, 15);

	// F5 surface-B: LEVEL is declared ONLY because the ancestor constructor requires it (LiquidBlock:78
	// registerDefaultState(...LEVEL...)); the quanta carrier is FLUID_META, LEVEL is always 0 and read by no one
	// (every LEVEL channel of LiquidBlock — getFluidState/getCollisionShape/pickupBlock — is overridden).
	// Save compatibility: old states have no level property — on read it takes the default (0).
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
	/** Flag 2 (SEND_TO_CLIENT without neighbors) — a meta write must not cascade updates (the cascade is done by GT6's own logic). */
	protected static final int FLUID_UPDATE_FLAGS_META = 2;

	// F-tick of fluids: 1.7.10 World.scheduleBlockUpdate → Block.updateTick; neo — BlockBehaviour.tick.
	// onBlockAdded (Forge BlockFluidBase) scheduled the first tick — neo onPlace 1:1.
	public void updateTick(Level aWorld, int aX, int aY, int aZ, java.util.Random aRandom) {/* overridden by BlockBaseFluid/Ocean/River/Swamp */}
	@Override protected void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), gregapi.util.UT.Code.random(aRandom)); // converter — the UT.Code.random center
	}
	@Override protected void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {
		onBlockAdded(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
	}
	/** Was Forge {@code BlockFluidBase.onBlockAdded(World,x,y,z)} (:227-230) — body 1:1. The dispatch from onPlace
	 *  is MANDATORY: Ocean/River/Swamp override it (PLACEMENT_ALLOWED gate + initial tick 10+rand(90)) —
	 *  without the dispatch their channel was an orphan (swamp did not tick → mud was not converted). */
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
	}
	@Override protected void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, net.minecraft.world.level.redstone.Orientation aOrientation, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}

	// ================= F5 surface-B: neutralizing the ancestor's VANILLA fluidity =================
	// LiquidBlock schedules vanilla fluid ticks in onPlace:153 / neighborChanged:199 / updateShape:181-183 —
	// the first two are already overridden by the GT6 channels above; updateShape is overridden here (body = the
	// BlockBehaviour.updateShape default "return the state unchanged", as it was before re-parenting). Without this
	// vanilla's FlowingFluid.tick would run ON TOP OF GT6's quanta — a double spill.
	@Override protected BlockState updateShape(BlockState aState, net.minecraft.world.level.LevelReader aWorld, net.minecraft.world.level.ScheduledTickAccess aTicks, BlockPos aPos, net.minecraft.core.Direction aDirection, BlockPos aNeighborPos, BlockState aNeighborState, net.minecraft.util.RandomSource aRandom) {
		return aState;
	}

	// LiquidBlock delegates randomTick to FluidState (:105-111) — for lava that's vanilla ignition
	// (LavaFluid.randomTick), which GT6's 1.7.10 fluids never had (their own flammability lives in updateTick).
	// The default before re-parenting = F (randomTicks() is not set on Properties); a descendant with its OWN
	// random channel overrides it itself (in 1.7.10 none of GT6's fluids had one).
	@Override protected boolean isRandomlyTicking(BlockState aState) {return F;}

	/** F5 surface-B, bucket 1:1 with 1.7.10 vanilla ({@code recompSrc/.../ItemBucket.java:85-98}): material water
	 *  + meta 0 → {@code setBlockToAir} + a water bucket; material lava + meta 0 → a lava bucket; OTHERWISE —
	 *  no pickup, the block is NOT touched (oils/gas were only ever scooped by GT6's own drain() mechanic). The channel
	 *  is read by {@code BucketItem} (player's bucket) and {@code SpongeBlock:66} (sponge). The ancestor's LEVEL body
	 *  (:249-256) doesn't fit: it reads the dead LEVEL and hands out a bucket via {@code fluid.getBucket()} with no material gate. */
	@Override public net.minecraft.world.item.ItemStack pickupBlock(net.minecraft.world.entity.LivingEntity aUser, net.minecraft.world.level.LevelAccessor aLevel, BlockPos aPos, BlockState aState) {
		if (aState.getValue(FLUID_META) != 0) return net.minecraft.world.item.ItemStack.EMPTY;
		net.minecraft.world.item.Item tBucket = mMaterial == Material.water ? net.minecraft.world.item.Items.WATER_BUCKET : mMaterial == Material.lava ? net.minecraft.world.item.Items.LAVA_BUCKET : null;
		if (tBucket == null) return net.minecraft.world.item.ItemStack.EMPTY;
		aLevel.setBlock(aPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
		return new net.minecraft.world.item.ItemStack(tBucket);
	}

	public abstract int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ);

	/** Was Forge {@code BlockFluidBase.onNeighborBlockChange(World,x,y,z,Block)} (func_149695_a) — body 1:1.
	 *  Needed by {@link gregtech.blocks.fluids.BlockOcean}/{@link gregtech.blocks.fluids.BlockRiver}, which call
	 *  {@code super.onNeighborBlockChange(...)} after their own logic. */
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
	}

	/** Was Forge {@code BlockFluidBase.canDisplace(IBlockAccess,x,y,z)} — body 1:1. */
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockPos aPos = new BlockPos(aX, aY, aZ);
		if (aWorld.getBlockState(aPos).isAir()) return T; // was block.isAir(world,x,y,z) — BlockState.isAir() (BlockBehaviour.java:575)
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == this) return F;
		if (displacements.containsKey(aBlock)) return displacements.get(aBlock);
		Material aBlockMaterial = WD.getMaterial(aBlock);
		if (aBlockMaterial.blocksMovement() || aBlockMaterial == Material.portal) return F;
		int tDensity = getDensity(aWorld, aX, aY, aZ);
		if (tDensity == Integer.MAX_VALUE) return T;
		return this.density > tDensity;
	}

	/** Was Forge {@code BlockFluidBase.displaceIfPossible(World,x,y,z)} — body 1:1. F5 (1:1): when density==MAX_VALUE
	 *  the Forge original dropped the displaced block ({@code block.dropBlockAsItem}) BEFORE displacing it → neo Block.dropResources
	 *  (Block.java:380). The drop is restored (it had been deferred as a silent no-op). The only difference from canDisplace is this side-effect drop. */
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
			if (aWorld instanceof net.minecraft.server.level.ServerLevel) net.minecraft.world.level.block.Block.dropResources(aWorld.getBlockState(aPos), aWorld, aPos); // Forge dropBlockAsItem of the displaced block
			return T;
		}
		return this.density > tDensity;
	}

	/** Was Forge {@code BlockFluidBase.getDensity(IBlockAccess,x,y,z)} (static). */
	public static int getDensity(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (!(aBlock instanceof BlockFluidBaseGT)) return Integer.MAX_VALUE;
		return ((BlockFluidBaseGT)aBlock).density;
	}

	/** Was Forge {@code BlockFluidBase.getQuantaValueBelow(IBlockAccess,x,y,z,belowThis)} (final) — body 1:1. */
	public final int getQuantaValueBelow(BlockGetter aWorld, int aX, int aY, int aZ, int aBelowThis) {
		int tQuantaRemaining = getQuantaValue(aWorld, aX, aY, aZ);
		if (tQuantaRemaining >= aBelowThis) return -1;
		return tQuantaRemaining;
	}

	/** densityDir accessor for the renderer (was 1.7.10 {@code FL.dir(BlockFluidBase)} / a direct field). */
	public int dir() {return densityDir;}

	// ================= BUG-115: IFluidBlock surface (see class header) =================
	/** The block's fluid. In 1.7.10 it came from the Forge ancestor ({@code BlockFluidBase.getFluid()}); here the
	 *  carriers know it themselves — both subclasses already hold it in their own {@code mFluid}, we don't add a second store. */
	@Override public abstract net.minecraft.world.level.material.Fluid getFluid();

	/** {@code drain} is NOT declared here: bodies already exist on both carriers ({@link gregtech.blocks.fluids.BlockWaterlike},
	 *  {@link BlockBaseFluid}) — 1:1 with 1.7.10, where they were {@code @Override} of this same interface. */

	/** {@code canDrain} is NOT implemented here: in Forge it lived in DIFFERENT descendants with different bodies —
	 *  {@code BlockFluidClassic.canDrain:358} = {@code isSourceBlock(...)} (overridden in GT6 with its own, meta 0),
	 *  {@code BlockFluidFinite.canDrain:332} = {@code return true}. A single default in the common ancestor would
	 *  replace both branches with an invented rule, so the method stays with the carriers. */

	/** Was Forge {@code BlockFluidBase.getFilledPercentage(World,x,y,z)} (:524-531) — body 1:1:
	 *  quanta+1, clamped to 1.0, sign follows density (gases have negative density, the fraction comes out negative —
	 *  that's how Forge told "filled from below" apart from "filled from above"). */
	@Override public float getFilledPercentage(Level aWorld, int aX, int aY, int aZ) {
		int tQuantaRemaining = getQuantaValue(aWorld, aX, aY, aZ) + 1;
		float tRemaining = tQuantaRemaining / quantaPerBlockFloat;
		if (tRemaining > 1) tRemaining = 1.0F;
		return tRemaining * (density > 0 ? 1 : -1);
	}

	/** Was Forge {@code BlockFluidBase.getQuantaPercentage(IBlockAccess,x,y,z)} (:452) — body 1:1. */
	public final float getQuantaPercentage(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getQuantaValue(aWorld, aX, aY, aZ) / quantaPerBlockFloat;
	}

	/** Was 1.7.10 {@code Block.isBlockSolid(IBlockAccess,x,y,z,side)} — body {@code material.isSolid()}
	 *  (the same approach as {@link gregtech.blocks.fluids.BlockWaterlike}). */
	protected boolean isBlockSolid(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		return WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isSolid();
	}

	/** Was Forge {@code BlockFluidBase.getFlowVector(IBlockAccess,x,y,z)} (:458-515) — body 1:1
	 *  (Vec3.createVectorHelper→new Vec3, addVector→add; {@code (y-y)*power}=0 folded away). Read by the renderer
	 *  ({@link gregapi.render.RendererBlockFluid} — rotates the surface texture by the flow direction). */
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

	/** Was Forge {@code BlockFluidBase.getFlowDirection(IBlockAccess,x,y,z)} (static :421-430) — body 1:1
	 *  (+an instanceof gate before the cast: it's only ever called on the fluid's own position, semantics unchanged). */
	public static double getFlowDirection(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (!(tBlock instanceof BlockFluidBaseGT) || !WD.getMaterial(tBlock).isLiquid()) return -1000.0D;
		net.minecraft.world.phys.Vec3 vec = ((BlockFluidBaseGT)tBlock).getFlowVector(aWorld, aX, aY, aZ);
		return vec.x == 0.0D && vec.z == 0.0D ? -1000.0D : Math.atan2(vec.z, vec.x) - Math.PI / 2D;
	}

	// ================================ F3-render: RENDERING BOTH FLUID HIERARCHIES — HERE ================================
	// In 1.7.10, fluid block rendering was CENTRALIZED by Gregorius himself: both BlockWaterlike (:197) and BlockBaseFluid
	// returned the SAME getRenderType() = RendererBlockFluid.RENDER_ID — one ISimpleBlockRenderingHandler for both
	// hierarchies, precisely because they shared an ancestor (Forge BlockFluidBase). The port restored the neo equivalent
	// of this channel (IRenderedBlock → GT6BlockModel/GT6ItemModel) ONLY for BlockBaseFluid — the water-likes (river/ocean/swamp)
	// stayed outside the channel: onModifyBakingResult (GT_API_Proxy_Client:258) injects an item model only for
	// IRenderedBlock blocks, and there are no JSON models in the mod at all (the mod is procedural) → their BlockItem had
	// NO model at all → the purple missing-texture stub (BUG-068).
	// The restoration approach is the same one Greg used: the channel is declared ONCE, in the common ancestor, and serves
	// both hierarchies. The one and only difference between them lives in the descendant — which texture (renderTexture):
	// BlockBaseFluid uses its own fluid (mFluid.getStillIcon 1.7.10), BlockWaterlike uses VANILLA water (1.7.10 :200 getIcon
	// → Blocks.water.getIcon). WORLD rendering follows the SINGLE rule "model ⟺ no fluid" — derived from the ROLE PASSPORT
	// (this class's getRenderShape, BUG-119/120): water-likes and geothermal (medium present) are drawn by the engine's
	// fluid pass, oils and gas (no medium) by GT6's quanta-based model.

	// ================= F3 light-opacity CENTER: how much light the fluid absorbs ==================================
	// 1.7.10 asked the block's getLightOpacity(), and BOTH fluid hierarchies answered the same way —
	// LIGHT_OPACITY_WATER=3 (gregtech6/.../BlockWaterlike.java:199 and .../BlockBaseFluid.java:367). In the port this
	// value was a COPY in both descendants, and the engine never asked for it at all: neo computes attenuation from
	// BlockState — LightEngine.getOpacity:85-87 takes state.getLightDampening(), which is filled in ONCE when the
	// state is assembled (BlockBehaviour.java:518) by calling the block's getLightDampening(BlockState). The
	// 1.7.10-signature methods were left with no caller => GT6 water did not darken depth: the default gave 1 instead of 3
	// (BlockBehaviour.java:290-295: not solid + propagatesSkylightDown=false → 1).
	// The bridge is declared ONCE here, in the common ancestor of both hierarchies, both value copies are removed.
	// ⚠️ Engine limitation: getLightDampening sees ONLY the state. The original's context-aware versions
	// (BlockOcean:164 — "a source, two air blocks above it, light passes through below → 16"; BlockSwamp:198 — "swamp
	// above → 255") asked the NEIGHBORS, which this channel has none of. What's expressible from state alone is
	// ported (BlockSwamp); what isn't goes into the deferred-work registry, not a silent stub.
	@Override protected int getLightDampening(net.minecraft.world.level.block.state.BlockState aState) {return getLightOpacity(aState);}

	/** Light attenuation for a specific state. The common value for both 1.7.10 hierarchies — {@code LIGHT_OPACITY_WATER}. */
	public int getLightOpacity(net.minecraft.world.level.block.state.BlockState aState) {return gregapi.data.CS.LIGHT_OPACITY_WATER;}

	// ================= F3 shade CENTER: how much the fluid darkens neighbors =================================
	// Same approach and same reason as light-opacity above: the 1.7.10 rule is identical for BOTH hierarchies —
	// renderAsNormalBlock()==F (gregtech6/.../BlockBaseFluid.java:379 and .../BlockWaterlike.java:214), meaning
	// the fluid was not treated as a normal cube and did not dim neighbors (Block.java:1334-1337, 502-504). In neo
	// the flag switched to collision (BlockBehaviour:306-308), so the value is carried over by a bridge; declared
	// ONCE here, in the common ancestor, both descendant copies removed. Channel breakdown — BlockBase.
	@Override protected float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** 1.7.10 {@code Block.isBlockNormalCube()} ({@code Block.java:502-504}) — body 1:1, see {@code BlockBase}. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}

	/** 1.7.10 rule shared by both fluid hierarchies, consolidated into the common ancestor (descendant copies were a duplicate). */
	public boolean renderAsNormalBlock() {return gregapi.data.CS.F;}

	/** Fluid texture for both render branches (world + item form). Client-only: {@code BlockTextureFluid.get} under {@code CODE_CLIENT}. */
	public abstract gregapi.render.ITexture renderTexture();

	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, net.minecraft.world.item.ItemStack aStack) {return renderTexture();}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return renderTexture();}
	@Override public boolean usesRenderPass(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return aRenderPass == 0;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return F;}
	/** Default — a full cube; the quanta-based surface height is overridden by {@link BlockBaseFluid} (world render of its own fluid). */
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(net.minecraft.world.item.ItemStack aStack) {return 1;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(net.minecraft.world.item.ItemStack aStack) {return null;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
}
