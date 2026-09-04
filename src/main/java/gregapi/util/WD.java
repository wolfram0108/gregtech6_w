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

package gregapi.util;
import gregapi.code.ItemNBT;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
// F5: net.minecraftforge.fluids.BlockFluidClassic/BlockFluidFinite are gone from the engine; see liquid_classic/liquid_finite below.
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import gregapi.GT_API;
import gregapi.block.IBlockDebugable;
import gregapi.block.IBlockExtendedMetaData;
import gregapi.block.IBlockPlacable;
import gregapi.block.IBlockTileEntity;
import gregapi.block.metatype.BlockMetaType;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.code.ItemStackContainer;
import gregapi.code.TagData;
import gregapi.data.*;
import gregapi.event.BlockScanningEvent;
import gregapi.item.IItemGT;
import gregapi.oredict.OreDictMaterial;
import gregapi.random.IHasWorldAndCoords;
import gregapi.tileentity.ITileEntity;
import gregapi.tileentity.ITileEntityQuickObstructionCheck;
import gregapi.tileentity.ITileEntityUnloadable;
import gregapi.tileentity.data.ITileEntityGibbl;
import gregapi.tileentity.data.ITileEntityProgress;
import gregapi.tileentity.data.ITileEntityTemperature;
import gregapi.tileentity.data.ITileEntityWeight;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityDelegating;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.energy.ITileEntityEnergyDataCapacitor;
import gregapi.tileentity.machines.*;
import gregapi.util.UT.Code;
import gregtech.blocks.fluids.BlockWaterlike;
import micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock;
import micdoodle8.mods.galacticraft.core.util.OxygenUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import gregapi.block.Material;
import gregapi.block.BlockBase;
import gregapi.block.multitileentity.MultiTileEntityBlock;
import gregapi.block.metatype.BlockStones;
import net.minecraft.core.Direction;
// F#(WD-block): world access goes through BlockPos/BlockState now (world.getBlockState(pos).getBlock() —
// BlockGetter.java:32 + BlockBehaviour.java:521 getBlock()); coordinate types, shapes and raytrace below.
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.util.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.*;
import thaumcraft.api.nodes.INode;

import java.util.*;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class WD {
	/** F-bounds center (instanceof-safe): 1.7.10 {@code WD.setBlockBounds(aBlock, ...)} mutated the block's bounds
	 *  (per-pass render / collision); neo bounds are immutable → delegate to the GT6 block (stores it itself, {@link gregapi.block.IBlock}),
	 *  a non-GT6 block is ignored (render usage deferred to the F3 client pass). */
	public static void setBlockBounds(net.minecraft.world.level.block.Block aBlock, float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		if (aBlock instanceof gregapi.block.IBlock) ((gregapi.block.IBlock)aBlock).setBlockBounds(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ);
	}
	public static ItemStack suck(IHasWorldAndCoords aCoordinates) {return suck(aCoordinates.getWorld(), aCoordinates.getX(), aCoordinates.getY(), aCoordinates.getZ());}
	public static ItemStack suck(LevelAccessor aWorld, double aX, double aY, double aZ) {return suck(aWorld, aX, aY, aZ, 1, 1, 1);}
	@SuppressWarnings("unchecked")
	public static ItemStack suck(LevelAccessor aWorld, double aX, double aY, double aZ, double aL, double aH, double aW) {
		for (ItemEntity tItem : (Iterable<ItemEntity>)aWorld.getEntitiesOfClass(ItemEntity.class, new AABB(aX, aY, aZ, aX+aL, aY+aH, aZ+aW))) {
			if (!tItem.isRemoved()) {
				tItem.discard();
				ItemStack rStack = tItem.getItem();
				tItem.setItem(ST.amount(0, rStack));
				tItem.discard();
				return rStack;
			}
		}
		return null;
	}
	// ==========================================================================================================
	// BUG-103, root cause "during world generation": WORLDGEN REMOVES ENTITIES FROM A FOREIGN THREAD.
	//
	// In 1.7.10 generation ran on the server main thread (IWorldGenerator.generate from the chunk provider),
	// so "kill items dropped by generation" was an ordinary line. In 26.1.2 chunk generation runs on worker
	// threads, while entity bookkeeping is the server thread's job: discard() drags the chain
	// Callback.onRemove -> stopTracking -> onTrackingEnd -> ChunkMap.removeEntity, i.e. it mutates entityMap
	// and EntityLookup.byId. While the server thread walks that same map (ChunkMap.tick:1206), a worker-thread
	// mutation tears the fastutil iterator — NPE "this.wrapped is null", not in the mod's own stack.
	//
	// SINGLE technique for the whole mod: both the SEARCH and the removal are deferred to the server thread
	// (which executes them right away, on its very next tick). The observable result is the same — entities
	// disappear; only the thread changes. Off-server (client/test level) it runs in place, as before.
	// ==========================================================================================================
	public static <T extends net.minecraft.world.entity.Entity> void discardEntitiesSafely(LevelAccessor aWorld, Class<T> aClass, AABB aBox, java.util.function.Predicate<T> aFilter) {
		if (aWorld == null || aClass == null || aBox == null) return;
		net.minecraft.world.level.Level tLevel = aWorld instanceof net.minecraft.world.level.Level tL ? tL : (aWorld instanceof net.minecraft.world.level.ServerLevelAccessor tS ? tS.getLevel() : null);
		if (tLevel == null) return;
		Runnable tJob = () -> {for (T tEntity : tLevel.getEntitiesOfClass(aClass, aBox)) if (!tEntity.isRemoved() && (aFilter == null || aFilter.test(tEntity))) tEntity.discard();};
		net.minecraft.server.MinecraftServer tServer = tLevel.getServer();
		if (tServer != null) tServer.execute(tJob); else tJob.run();
	}

	public static List<ItemStack> suckAll(IHasWorldAndCoords aCoordinates) {return suckAll(aCoordinates.getWorld(), aCoordinates.getX(), aCoordinates.getY(), aCoordinates.getZ());}
	public static List<ItemStack> suckAll(LevelAccessor aWorld, double aX, double aY, double aZ) {return suckAll(aWorld, aX, aY, aZ, 1, 1, 1);}
	@SuppressWarnings("unchecked")
	public static List<ItemStack> suckAll(LevelAccessor aWorld, double aX, double aY, double aZ, double aL, double aH, double aW) {
		List<ItemEntity> tList = aWorld.getEntitiesOfClass(ItemEntity.class, new AABB(aX, aY, aZ, aX+aL, aY+aH, aZ+aW));
		if (tList.isEmpty()) return Collections.emptyList();
		List<ItemStack> rOutput = ST.arraylist();
		for (ItemEntity tItem : tList) {
			if (!tItem.isRemoved()) {
				tItem.discard();
				ItemStack rStack = tItem.getItem();
				tItem.setItem(ST.amount(0, rStack));
				tItem.discard();
				rOutput.add(rStack);
			}
		}
		return rOutput;
	}
	
	public static boolean obstructed(LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide) {
		if (!OBSTRUCTION_CHECKS) return F;
		aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		BlockEntity tTileEntity = te(aWorld, aX, aY, aZ, T);
		if (tTileEntity != null) {
			if (tTileEntity instanceof ITileEntityQuickObstructionCheck) return ((ITileEntityQuickObstructionCheck)tTileEntity).isObstructingBlockAt(OPOS[aSide]);
			if (MD.TC.mLoaded && tTileEntity instanceof INode) return F;
		}
		BlockPos tObstrPos = new BlockPos(aX, aY, aZ);
		BlockState tObstrState = state(aWorld, tObstrPos); // was aWorld.getBlock(x,y,z) — BlockGetter.java:32
		Block tBlock = tObstrState.getBlock();
		if (tBlock instanceof TrapDoorBlock || tBlock instanceof DoorBlock || tBlock instanceof LadderBlock) return F;
		// was tBlock.getCollisionBoundingBoxFromPool(world,x,y,z) — BlockBehaviour.getCollisionShape(level,pos)
		// (BlockBehaviour.java:674) gives a local VoxelShape; .move(pos).bounds() carries it into world coordinates
		// (VoxelShape.java:39,81); an empty shape is the old null return (no collision).
		VoxelShape tObstrShape = tObstrState.getCollisionShape(aWorld, tObstrPos);
		if (tObstrShape.isEmpty()) return F;
		AABB tBoundingBox = tObstrShape.move(tObstrPos.getX(), tObstrPos.getY(), tObstrPos.getZ()).bounds();
		switch(aSide) {
		case 0: return tBoundingBox.maxY-aY > PX_N[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2];
		case 1: return tBoundingBox.minY-aY < PX_P[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2];
		case 2: return tBoundingBox.maxZ-aZ > PX_N[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		case 3: return tBoundingBox.minZ-aZ < PX_P[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		case 4: return tBoundingBox.maxX-aX > PX_N[4] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		case 5: return tBoundingBox.minX-aX < PX_P[4] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		}
		return F;
	}
	
	public static HitResult getMOP(LevelAccessor aWorld, Player aPlayer, boolean aFlag) {
		Vec3 vec3 = new Vec3( // 1.7.10 Vec3.createVectorHelper(x,y,z) removed -> neo ctor new Vec3(double,double,double).
		  aPlayer.xo + (aPlayer.getX() - aPlayer.xo)
		, aPlayer.yo + (aPlayer.getY() - aPlayer.yo) + (aWorld.isClientSide() ? aPlayer.getEyeHeight() - aPlayer.getEyeHeight(net.minecraft.world.entity.Pose.STANDING) : aPlayer.getEyeHeight()) // F6-eye: 1.7.10 getDefaultEyeHeight() -> neo getEyeHeight(Pose.STANDING) (standing eye height, Entity.java:3381). isRemote check to revert changes to ray trace position due to adding the eye height clientside and player yOffset differences
		, aPlayer.zo + (aPlayer.getZ() - aPlayer.zo)
		);
		float  tPitch = aPlayer.xRotO + (aPlayer.getXRot() - aPlayer.xRotO);
		float  tYaw   = aPlayer.yRotO   + (aPlayer.getYRot()   - aPlayer.yRotO  );
		float  tZ     =  Mth.cos(-tYaw   * 0.017453292F - (float)Math.PI);
		float  tX     =  Mth.sin(-tYaw   * 0.017453292F - (float)Math.PI);
		float  tW     = -Mth.cos(-tPitch * 0.017453292F);
		float  tY     =  Mth.sin(-tPitch * 0.017453292F);
		double tReach = 5; // 1.20.1: no reach attribute/method yet, engine constant is 5 (same fallback as the 26.x branch)
		// was aWorld.func_147447_a(from,to,stopOnLiquid,ignoreBlockWithoutBoundingBox,returnLastUncollidableBlock=F) —
		// neo: BlockGetter.clip(ClipContext) (BlockGetter.java:65). stopOnLiquid=aFlag -> ClipContext.Fluid.ANY/NONE
		// (ClipContext.java:96-110, ANY accepts any non-empty FluidState, NONE — never); Block.OUTLINE is the same
		// shape mode the vanilla player-look raytrace actually uses (Item.getPlayerPOVHitResult,
		// Item.java:362-365); returnLastUncollidableBlock is always F here (no equivalent, unused).
		return aWorld.clip(new ClipContext(vec3, vec3.add(tX * tW * tReach, tY * tReach, tZ * tW * tReach), ClipContext.Block.OUTLINE, aFlag ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, aPlayer));
	}
	
	// F6: used to have `WorldProvider aProvider` overloads (numeric `dimensionId`, `UT.Reflection.getLowercaseClass`
	// by the foreign mod's provider java class name) IN PARALLEL with `Level aWorld` overloads calling them
	// through `aWorld.provider` — `net.minecraft.world.WorldProvider` is gone from neo entirely (not in any of the
	// 3 reference roots), so the compiler could not choose between the two same-named `dimXXX(...)` overloads
	// (ambiguous). The `WorldProvider` overloads are removed, leaving a single entry point — `dimXXX(Level)`.
	// Vanilla dimension identity (mod-independent) is ported verbatim to the neo equivalents
	// (`Level.dimension()`==`Level.OVERWORLD/NETHER/END`, see `decisions/README.md` "Dimension-identity").
	// FOREIGN mod dimension identity went through reflection on the mod's `WorldProvider` subclass java class name
	// (`"WorldProviderCaves".equalsIgnoreCase(...)` etc.) or (dimTF) through the numeric
	// `TwilightForestMod.dimensionID` — none of the 3 reference roots contains a neo equivalent for these
	// ancient 1.7.10 mods (not ported), this is foreign-gated (ancient 1.7.10 mods not ported): F is correct while the mod is absent.
	/** F6 dimension-identity CENTER: a neo dimension has no numeric id, the dimension key is {@code ResourceKey<Level>}.
	 *  {@code Level} gives {@code dimension()} directly; the worldgen receiver {@code WorldGenLevel}/{@code ServerLevelAccessor}
	 *  knows its {@code ServerLevel} through {@code getLevel()} → {@code getLevel().dimension()}. The single entry point for the
	 *  whole mod takes {@code LevelAccessor} (the common supertype of Level and WorldGenLevel) — replaces the direct {@code aWorld.dimension()},
	 *  which the bare {@code LevelAccessor} does not have. */
	public static net.minecraft.resources.ResourceKey<Level> dimKey(LevelAccessor aWorld) {
		if (aWorld instanceof Level) return ((Level)aWorld).dimension();
		if (aWorld instanceof net.minecraft.world.level.ServerLevelAccessor) return ((net.minecraft.world.level.ServerLevelAccessor)aWorld).getLevel().dimension();
		return null;
	}

	public static boolean dimOverworldLike(LevelAccessor aWorld) {return aWorld != null && (dimKey(aWorld) == Level.OVERWORLD || dimENVM(aWorld) || dimA97(aWorld) || dimWTCH(aWorld) || dimMYST(aWorld) || dimCW2(aWorld));}

	public static boolean dimPlanet(LevelAccessor aWorld) {return aWorld != null && dimKey(aWorld) != Level.OVERWORLD && dimKey(aWorld) != Level.NETHER && dimKey(aWorld) != Level.END && !(dimMYST(aWorld) || dimATUM(aWorld) || dimWTCH(aWorld) || dimA97(aWorld) || dimCW2(aWorld) || dimTF(aWorld) || dimERE(aWorld) || dimBTL(aWorld) || dimENVM(aWorld) || dimDD(aWorld) || dimLM(aWorld) || dimAETHER(aWorld) || dimALF(aWorld) || dimTROPIC(aWorld) || dimCANDY(aWorld));}

	public static boolean dimMYST(LevelAccessor aWorld) {return aWorld != null && MD.MYST.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimMYST — the Mystcraft provider was identified by java class name ("com.xcompwiz.mystcraft"), WorldProvider is gone, no equivalent exists in any of the 3 reference roots */}

	public static boolean dimCANDY(LevelAccessor aWorld) {return aWorld != null && MD.CANDY.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimCANDY — the CandyCraft provider by class name "WorldProviderCandy" */}

	public static boolean dimTROPIC(LevelAccessor aWorld) {return aWorld != null && MD.TROPIC.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimTROPIC — the Tropicraft provider by class name "WorldProviderTropicraft" */}

	public static boolean dimATUM(LevelAccessor aWorld) {return aWorld != null && MD.ATUM.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimATUM — the Atum provider by class name "AtumWorldProvider" */}

	public static boolean dimTF(LevelAccessor aWorld) {return aWorld != null && MD.TF.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimTF — compared against the numeric TwilightForestMod.dimensionID, WorldProvider.dimensionId was removed along with numeric dimension identity */}

	public static boolean dimBTL(LevelAccessor aWorld) {return aWorld != null && MD.BTL.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimBTL — the Betweenlands provider by class name "WorldProviderBetweenlands" */}

	public static boolean dimERE(LevelAccessor aWorld) {return aWorld != null && MD.ERE.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimERE — the Erebus provider by class name "WorldProviderErebus" */}

	public static boolean dimALF(LevelAccessor aWorld) {return aWorld != null && MD.ALF.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimALF — the Alfheim provider by class name "WorldProviderAlfheim" */}

	public static boolean dimDD(LevelAccessor aWorld) {return aWorld != null && (MD.ExU.mLoaded || MD.ExS.mLoaded) && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimDD — the Underdark provider by class name "WorldProviderUnderdark" */}

	public static boolean dimLM(LevelAccessor aWorld) {return aWorld != null && (MD.ExU.mLoaded || MD.ExS.mLoaded) && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimLM — the EndOfTime provider by class name "WorldProviderEndOfTime" */}

	public static boolean dimENVM(LevelAccessor aWorld) {return aWorld != null && MD.ENVM.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimENVM — the Enviromine Caves provider by class name "WorldProviderCaves" */}

	public static boolean dimGC(LevelAccessor aWorld) {return aWorld != null && MD.GC.mLoaded && F; /* F6/F10 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimGC — the Galacticraft dimension was determined by `aWorld.provider instanceof IGalacticraftWorldProvider`, WorldProvider was removed from the engine (the same issue as the dimXXX family above) */}

	public static boolean dimA97(LevelAccessor aWorld) {return aWorld != null && MD.A97_MINING.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimA97 — the Aroma1997 Mining provider by class name "WorldProviderMiner" */}

	public static boolean dimCW2(LevelAccessor aWorld) {return aWorld != null && (dimCW2AquaCavern(aWorld) || dimCW2Caveland(aWorld) || dimCW2Cavenia(aWorld) || dimCW2Cavern(aWorld) || dimCW2Caveworld(aWorld));}

	public static boolean dimCW2AquaCavern(LevelAccessor aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimCW2AquaCavern — by class name "WorldProviderAquaCavern" */}

	public static boolean dimCW2Caveland(LevelAccessor aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimCW2Caveland — by class name "WorldProviderCaveland" */}

	public static boolean dimCW2Cavenia(LevelAccessor aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimCW2Cavenia — by class name "WorldProviderCavenia" */}

	public static boolean dimCW2Cavern(LevelAccessor aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimCW2Cavern — by class name "WorldProviderCavern" */}

	public static boolean dimCW2Caveworld(LevelAccessor aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimCW2Caveworld — by class name "WorldProviderCaveworld" */}

	public static boolean dimWTCH(LevelAccessor aWorld) {return aWorld != null && MD.WTCH.mLoaded && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimWTCH — the Witchery Dream World provider by class name "WorldProviderDreamWorld" */}

	public static boolean dimAETHER(LevelAccessor aWorld) {return aWorld != null && (MD.AETHER.mLoaded || MD.AETHEL.mLoaded) && F; /* F6 impossible-1:1 (foreign-gated; neo dimension-identity = aWorld.dimension() ResourceKey, the foreign-dimension key exists only once the mod is ported; MD.*.mLoaded absent -> false is correct): dimAETHER — the Aether provider by class name "AetherWorldProvider"/"WorldProviderAether" */}

	/** used to be manual 1.7.10 dimension-travel (DimensionManager/ridingEntity/removePlayerEntityDangerously/ClientboundRespawnPacket/
	 *  theItemInWorldManager/getConfigurationManager/FMLCommonHandler.firePlayerChangedDimensionEvent/createEntityByName — all removed) —
	 *  neo Entity.teleportTo(ServerLevel,x,y,z,Set<Relative>,yRot,xRot,resetCamera) (Entity.java:3257) performs the entire cross-dimension
	 *  move cycle (dismount/respawn packet/inventory-sync/entity re-creation) internally. Target world by int-dim via WD.dimensionId
	 *  (getAllLevels:1239). resetCamera=F, coordinates are absolute (empty Set<Relative>). F-dimension caveat: move() itself works 1:1 (Entity.teleportTo); only mod int-ids depend on the WD.dimensionId map (foreign-gated). */
	public static boolean move(Entity aEntity, int aDimension, double aX, double aY, double aZ) {
		MinecraftServer tServer = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
		if (tServer == null || !(aEntity.level() instanceof ServerLevel)) return F;
		ServerLevel tTargetWorld = null;
		for (ServerLevel tLevel : tServer.getAllLevels()) if (WD.dimensionId(tLevel) == aDimension) {tTargetWorld = tLevel; break;}
		if (tTargetWorld == null || tTargetWorld == aEntity.level()) return F;
		// Branch 1.20.1: Set<Relative> (26.x) does not exist; the cross-world teleportTo(ServerLevel,x,y,z,Set,yaw,pitch)
		// overload is also absent. 1.7.10 form: switch dimension and set coordinates (ServerLevel.java / Entity.java).
		if (aEntity instanceof net.minecraft.server.level.ServerPlayer tSP) {tSP.teleportTo(tTargetWorld, aX+0.5, aY+0.5, aZ+0.5, aEntity.getYRot(), aEntity.getXRot()); return T;}
		net.minecraft.world.entity.Entity tMoved = aEntity.changeDimension(tTargetWorld);
		if (tMoved == null) return F;
		tMoved.moveTo(aX+0.5, aY+0.5, aZ+0.5, aEntity.getYRot(), aEntity.getXRot());
		return T;
	}
	
	
	/** Marks a Chunk dirty so it is saved */
	public static boolean mark(LevelAccessor aWorld, int aX, int aZ) {
		if (!(aWorld instanceof Level) || aWorld.isClientSide()) return F;
		// NO-LOAD (edit #2, 2026-08-09): 1.7.10 getChunkFromBlockCoords on an unloaded chunk gave an EmptyChunk,
		// markDirty on it was a no-op — meaning the original did NOT load the chunk just to mark it. The port called
		// the loading Level.getChunk(int,int) (ticket unknown on every call, ServerChunkCache.java:242). An unloaded
		// chunk has nothing to mark — its state is already on disk.
		net.minecraft.world.level.chunk.LevelChunk aChunk = chunkNow((Level)aWorld, aX >> 4, aZ >> 4);
		if (aChunk == null) return F;
		aChunk.setUnsaved(true); // was aChunk.setUnsaved(true) — neo: LevelChunk.setUnsaved(true) (see Level.java:868 aWorld.getChunkAt(pos).setUnsaved(true))
		return T;
	}
	/** Marks a Chunk dirty so it is saved */
	public static boolean mark(Object aTileEntity) {
		// used to be .getWorldObj()/.x/.z — neo: BlockEntity.getLevel() (BlockEntity.java:89) + .getBlockPos() (BlockEntity.java:232)
		return aTileEntity instanceof BlockEntity && mark(((BlockEntity)aTileEntity).getLevel(), ((BlockEntity)aTileEntity).getBlockPos().getX(), ((BlockEntity)aTileEntity).getBlockPos().getZ());
	}
	
	
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static DelegatorTileEntity<BlockEntity> te(Level aWorld, BlockPos aCoords, byte aSide, boolean aLoadUnloadedChunks) {
		return te(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aSide, aLoadUnloadedChunks);
	}
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static DelegatorTileEntity<BlockEntity> te(Level aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {
		BlockEntity aTileEntity = te(aWorld, aX, aY, aZ, aLoadUnloadedChunks);
		return aTileEntity instanceof ITileEntityDelegating ? ((ITileEntityDelegating)aTileEntity).getDelegateTileEntity(aSide) : new DelegatorTileEntity<>(aTileEntity, aWorld, aX, aY, aZ, aSide);
	}
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static BlockEntity te(LevelAccessor aWorld, BlockPos aCoords, boolean aLoadUnloadedChunks) {
		return te(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aLoadUnloadedChunks);
	}
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static BlockEntity te(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		// NO-LOAD (edit #2, 2026-08-09): reading NEVER loads the chunk and never sets a ticket — the 1.7.10
		// contract (blockExists+EmptyChunk). The previous path, even with a POSITIVE gate, went Level.getBlockEntity →
		// getChunk(FULL,true) → ticket unknown on EVERY call even for an already-loaded chunk (ServerChunkCache.java:242)
		// — this extended chunk lifetime (measurement BUG-106: 4769 server chunks against a norm of ~1400). The
		// aLoadUnloadedChunks flag is kept in the signature but can no longer load (user decision 2026-08-09:
		// optimization outweighs 1:1, centralization is mandatory).
		if (aWorld instanceof Level tL) {
			net.minecraft.world.level.chunk.LevelChunk tChunk = chunkNow(tL, aX >> 4, aZ >> 4);
			if (tChunk == null || tL.isOutsideBuildHeight(tPos)) return null;
			BlockEntity rTileEntity = tChunk.getBlockEntity(tPos); // lazy creation from NBT — the same as the engine would do
			if (rTileEntity instanceof ITileEntityUnloadable && ((ITileEntityUnloadable)rTileEntity).isDead()) return null;
			if (rTileEntity != null) return rTileEntity;
			rTileEntity = LAST_BROKEN_TILEENTITY.get();
			// used to be .x/.y/.z — neo: BlockEntity.getBlockPos() (BlockEntity.java:232)
			if (rTileEntity != null && rTileEntity.getBlockPos().getX() == aX && rTileEntity.getBlockPos().getY() == aY && rTileEntity.getBlockPos().getZ() == aZ) return rTileEntity;
			Block tBlock = tChunk.getBlockState(tPos).getBlock(); // used to be aWorld.getBlock(x,y,z) — BlockGetter.java:32
			return tBlock instanceof IBlockTileEntity ? ((IBlockTileEntity)tBlock).getTileEntity(aWorld, aX, aY, aZ) : null;
		}
		return null;
	}
	/** F-world: read-only access (BlockGetter = former IBlockAccess). The GT6 original called aWorld.getTileEntity(x,y,z)
	 *  directly on IBlockAccess (MultiTileEntityBlock.receiveDataX, BlockBaseFluid) — centralize through WD.te,
	 *  same as the Level version. If it's a Level — delegate to the full version (chunk-load, 1:1 behavior); otherwise a flat
	 *  read (BlockGetter doesn't load chunks — it's already a read-view, aLoadUnloadedChunks has nothing to load). */
	public static BlockEntity te(BlockGetter aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {
		if (aWorld instanceof Level) return te((Level)aWorld, aX, aY, aZ, aLoadUnloadedChunks);
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockEntity rTileEntity = aWorld.getBlockEntity(tPos);
		if (rTileEntity instanceof ITileEntityUnloadable && ((ITileEntityUnloadable)rTileEntity).isDead()) return null;
		if (rTileEntity != null) return rTileEntity;
		rTileEntity = LAST_BROKEN_TILEENTITY.get();
		if (rTileEntity != null && rTileEntity.getBlockPos().getX() == aX && rTileEntity.getBlockPos().getY() == aY && rTileEntity.getBlockPos().getZ() == aZ) return rTileEntity;
		Block tBlock = state(aWorld, tPos).getBlock();
		return tBlock instanceof IBlockTileEntity ? ((IBlockTileEntity)tBlock).getTileEntity(aWorld, aX, aY, aZ) : null;
	}
	/** F6-worldgen/lighting CRITICAL (deadlock during generation): BE access WITHOUT force-loading the chunk. Block methods that the engine
	 *  calls DURING chunk generation / light computation ({@link gregapi.block.multitileentity.MultiTileEntityBlock#getLightEmission}
	 *  etc.) must NOT go through the regular {@link #te} — it forces {@code Level.getBlockState}→{@code getChunk}→
	 *  {@code CompletableFuture.join} on the CURRENTLY generating chunk → the light-engine thread waits on the chunk it is generating itself →
	 *  permanent DEADLOCK (jstack: BlockLightEngine.getEmission→MTE.getLightEmission→WD.te→ServerChunkCache.getChunk.join).
	 *  Take the BE ONLY from an already-FULL chunk: {@code getChunk(cx,cz,FULL,false)} is non-blocking (null if the chunk isn't ready yet) →
	 *  then return null (the engine recomputes BE light emission after chunk finalization). For a non-Level BlockGetter (chunk/region)
	 *  {@code getBlockEntity} is non-blocking anyway (reading from the BE map). */
	public static BlockEntity teNonForcing(BlockGetter aWorld, int aX, int aY, int aZ) {
		// CRITICAL: getChunk(cx,cz,FULL,false) is NOT non-blocking — requireChunk=false only means "return null instead of throw", but
		// the main-thread future + join REMAINS → on an off-thread (light thread during generation) it still deadlocks.
		// CRITICAL-2 (ADAPT-005, live probe): getChunkNow doesn't work — it has a hard "main thread only" gate
		// (ServerChunkCache.getChunkNow:183 `Thread.currentThread() != mainThread -> null`), and BlockLightEngine.getEmission
		// calls us precisely FROM THE LIGHT THREAD → BE is always null → dynamic BE light (IMTE_GetLightValue) is DEAD on the server.
		// The correct path is the lock-free chain of the chunk engine itself: chunkMap.getVisibleChunkIfPresent (ChunkMap.java:255, public)
		// → ChunkHolder.getLatestChunk (GenerationChunkHolder.java:263 — AtomicReference + future.getNow, WITHOUT a thread gate
		// and WITHOUT join; getChunkForLighting doesn't work — for a long-FULL chunk the intermediate-stage futures are cleared → null).
		// For a still-generating chunk it returns a ProtoChunk (not LevelChunk) → null → the baked default (anti-deadlock preserved).
		// BE is read from the chunk's map (getBlockEntities().get) — a clean read without side-effect BE creation from a foreign thread.
		if (aWorld instanceof net.minecraft.server.level.ServerLevel tSL) {
			net.minecraft.server.level.ChunkHolder tHolder = tSL.getChunkSource().chunkMap.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.asLong(aX >> 4, aZ >> 4));
			net.minecraft.world.level.chunk.ChunkAccess tChunk = tHolder == null ? null : tHolder.getLastAvailable(); // 1.20.1: ChunkHolder.getLastAvailable()
			// A FULL chunk may arrive wrapped in ImposterProtoChunk — unwrap it (getWrapped:255).
			if (tChunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk tIPC) tChunk = tIPC.getWrapped();
			return tChunk instanceof net.minecraft.world.level.chunk.LevelChunk tLC ? tLC.getBlockEntities().get(new BlockPos(aX, aY, aZ)) : null;
		}
		return aWorld == null ? null : aWorld.getBlockEntity(new BlockPos(aX, aY, aZ));
	}
	/** F-world: 1.7.10 World.blockExists(x,y,z) = "chunk holding this block is loaded". The port centralized calls as
	 *  WD.exists, but the method was never defined. NO-LOAD (edit #2): the previous hasChunkAt went through
	 *  getChunk(FULL,false)+managedBlock (Level.java:695) — didn't load, but pumped the main-thread queue; now
	 *  a lock-free look at the visible holder — the exact meaning of 1.7.10's "chunk in loadedChunkHashMap". */
	public static boolean exists(LevelAccessor aWorld, int aX, int aY, int aZ) {
		return aWorld instanceof Level tL && chunkNow(tL, aX >> 4, aZ >> 4) != null;
	}

	/** CENTER for chunk reading (edit #2, 2026-08-09): the only way for the mod to get a chunk for READING.
	 *  NEVER loads the chunk, NEVER sets a ticket (the loading Level.getChunk sets an unknown ticket on
	 *  EVERY call even for an already-loaded chunk — ServerChunkCache.java:242; measurement BUG-106: these tickets
	 *  held 4769 server chunks against a norm of ~1400). Server: mirrors getChunkNow (ServerChunkCache.java:182-211),
	 *  but without the "main thread only" gate — a lock-free chain visible-holder → getChunkIfPresent(FULL) (AtomicReference,
	 *  no join; the same approach as teNonForcing below). Client: ClientChunkCache.getChunk(...,false) — reads the map.
	 *  null = chunk not loaded to FULL → for readers this means "air/nothing", the 1.7.10 blockExists+EmptyChunk contract. */
	public static net.minecraft.world.level.chunk.LevelChunk chunkNow(Level aWorld, int aChunkX, int aChunkZ) {
		if (aWorld instanceof net.minecraft.server.level.ServerLevel tSL) {
			// BACKPORT BUG FIX (root BP-BUG-008, uncovered 2026-08-16). The previous branch revision read
			// ChunkHolder.getFullChunk() (ChunkHolder.java:111) as "a FULL chunk already exists". That is a DIFFERENT
			// predicate: it looks at the fullChunkFuture field, which ChunkMap.prepareAccessibleChunk builds
			// (ChunkMap.java:768-776) — getChunkRangeFuture of RADIUS 1 (all 9 chunks in a 3x3) plus a task on
			// mainThreadMailbox. The engine hands the chunk out much earlier than that:
			// Level.getChunkAt -> ServerChunkCache.getChunk(FULL,true) only waits on the chunk's own STATUS future
			// (ChunkHolder.getOrScheduleFuture(ChunkStatus.FULL), ServerChunkCache.java:200-208). Between those two
			// moments there is a window where the engine already lives in the chunk's blocks (BEs tick, setBlock/getBlockState
			// work), while the mod answered "no such chunk" — i.e. the WHOLE chunk was read as air-with-no-entities
			// (te/state/exists/mark/precipitationHeight). Hence the lost builds: orphan cleanup in
			// MultiTileEntityBlock.onNeighborBlockChange saw WD.te == null for live MTEs and tore them down in bulk,
			// exactly at one chunk's borders.
			// The predicate now matches the engine's own — a mirror of ServerChunkCache.getChunkNow (ServerChunkCache.java:157-190),
			// same as on main (WD.java:441-450 of the 26.1.2 branch, which uses getChunkIfPresent(ChunkStatus.FULL)).
			// On the main thread we REUSE the engine method itself: it carries a cache of the last 4 chunks and the Forge bypass
			// currentlyLoading (ChunkMap.java:719-724 — chunk in the process of loading, a package-private field, invisible from outside).
			// Off the main thread (light thread, client render) getChunkNow has a hard "main only" gate -> there we use a lock-free
			// look at that same STATUS future, without join and without the thread gate.
			if (tSL.getServer() != null && tSL.getServer().isSameThread()) return tSL.getChunkSource().getChunkNow(aChunkX, aChunkZ);
			net.minecraft.server.level.ChunkHolder tHolder = tSL.getChunkSource().chunkMap.getVisibleChunkIfPresent(net.minecraft.world.level.ChunkPos.asLong(aChunkX, aChunkZ));
			if (tHolder == null) return null;
			com.mojang.datafixers.util.Either<net.minecraft.world.level.chunk.ChunkAccess, net.minecraft.server.level.ChunkHolder.ChunkLoadingFailure> tEither
				= tHolder.getFutureIfPresent(net.minecraft.world.level.chunk.ChunkStatus.FULL).getNow(null);
			return tEither != null && tEither.left().orElse(null) instanceof net.minecraft.world.level.chunk.LevelChunk tLC ? tLC : null;
		}
		return aWorld.getChunkSource().getChunk(aChunkX, aChunkZ, net.minecraft.world.level.chunk.ChunkStatus.FULL, F) instanceof net.minecraft.world.level.chunk.LevelChunk tLC ? tLC : null;
	}

	/** CENTER for the "the cell PROVABLY has no BlockEntity" predicate (root BP-BUG-008, second half).
	 *
	 *  <p>In 1.7.10 "TE not found" meant exactly one thing — an orphan, because the question and the action went through ONE path:
	 *  {@code World.getTileEntity} loaded the chunk ({@code World.java:2141} → {@code getChunkFromChunkCoords}), and
	 *  {@code Chunk.func_150806_e} read the single {@code chunkTileEntityMap} map, filled synchronously with the
	 *  chunk load. A "entity still packed" stage did not exist at all.
	 *
	 *  <p>In 1.20.1 the "no BE" answer became multi-valued, and EACH value is NOT orphanhood:
	 *  <ul>
	 *  <li>the chunk is not visible to the mod (promotion/unload window) — {@link #chunkNow} returns null, while {@code Level.setBlock}
	 *      WILL load that same chunk and write to it: we ask non-blockingly, we act blockingly;</li>
	 *  <li>the entity is still "packed" in {@code LevelChunk.pendingBlockEntities} — unpacking happens only at
	 *      {@code postProcessGeneration} ({@code LevelChunk.java:514-518} ← {@code ChunkMap.prepareTickingChunk}), i.e. the
	 *      chunk must reach TICKING; before that the {@code blockEntities} map is empty despite full content on disk;</li>
	 *  <li>the MTE entity is not yet reconstructed from its stub ({@code GT6WorldgenFeature.drainStubs}, 16 chunks per tick);</li>
	 *  <li>{@link #te} additionally filters out a "dead" entity from the answer ({@code ITileEntityUnloadable.isDead}) —
	 *      this is a lifecycle state, not absence.</li>
	 *  </ul>
	 *
	 *  <p><b>What the predicate does.</b> Asks the chunk DIRECTLY and only it: {@code LevelChunk.getBlockEntity(pos)} itself
	 *  unpacks the pending entry ({@code pendingBlockEntities.remove} + promotion, {@code LevelChunk.java:303-310}) — the same
	 *  technique the ore-registering hopper uses to lift the pending entry ({@code PrefixBlock:969-971}). A stub is also an entity, also
	 *  an "exists" answer. The {@code isDead} filter is NOT applied: a dead entity exists. Chunk not visible — the answer is "not
	 *  proven": one cannot judge a cell that is not visible.
	 *
	 *  <p><b>Cost.</b> The predicate is called ONLY in the branch where {@link #te} has already returned null — on the hot path of
	 *  neighbor updates it never runs while everything is fine. It is itself a single chunk-map lookup, with no allocation beyond the position. */
	public static boolean teProvenAbsent(Level aWorld, int aX, int aY, int aZ) {
		if (aWorld == null) return F;
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		if (aWorld.isOutsideBuildHeight(tPos)) return F;
		net.minecraft.world.level.chunk.LevelChunk tChunk = chunkNow(aWorld, aX >> 4, aZ >> 4);
		return tChunk != null && tChunk.getBlockEntity(tPos) == null;
	}

	/** CENTER for the "blocks tick here" gate (edit #2c, user decision 2026-08-09: "wherever water freezes,
	 *  the pipe freezes too" — one law for GT6 and vanilla). The engine predicate ServerLevel.shouldTickBlocksAt
	 *  (ServerLevel.java:480 → DistanceManager.inBlockTickingRange) is exactly the same gate the engine uses to
	 *  disable BlockEntityTicker, random ticks and fluids in border chunks. In 1.7.10 there was no gradation
	 *  (any loaded chunk ticked in full) — GT6's global tick lists without this gate ticked machinery where
	 *  the engine had already frozen everything. Non-BlockEntity (no position) is not gated. */
	public static boolean blockTicking(Object aTileEntity) {
		if (!(aTileEntity instanceof BlockEntity tTE)) return T;
		Level tLevel = tTE.getLevel();
		return !(tLevel instanceof net.minecraft.server.level.ServerLevel tSL) || tSL.shouldTickBlocksAt(tTE.getBlockPos());
	}
	public static boolean blockTicking(LevelAccessor aWorld, BlockPos aPos) {
		return !(aWorld instanceof net.minecraft.server.level.ServerLevel tSL) || tSL.shouldTickBlocksAt(aPos);
	}

	/** CENTER for state reading (edit #2): all block reads inside WD go through here. Level → via chunkNow
	 *  (unloaded = air, no tickets); other BlockGetters (chunk view, render region) — direct read,
	 *  which is non-blocking by nature. */
	public static BlockState state(BlockGetter aView, BlockPos aPos) {
		if (aView instanceof Level tL) {
			if (tL.isOutsideBuildHeight(aPos)) return NB.defaultBlockState();
			net.minecraft.world.level.chunk.LevelChunk tChunk = chunkNow(tL, aPos.getX() >> 4, aPos.getZ() >> 4);
			return tChunk == null ? NB.defaultBlockState() : tChunk.getBlockState(aPos);
		}
		return aView.getBlockState(aPos);
	}
	/** F-world: 1.7.10 World sky-visibility(x,y,z) -> neo canSeeSky(BlockPos) (BlockAndLightGetter.java:17). */
	public static boolean canSeeSky(LevelAccessor aWorld, int aX, int aY, int aZ) {
		return aWorld != null && aWorld.canSeeSky(new BlockPos(aX, aY, aZ));
	}
	/** F-world/F-hardness: 1.7.10 WD.hardness(Block, world,x,y,z) = Block.getBlockHardness (a Forge per-position hook).
	 *  GT6 hierarchies (IBlock contract: BlockBase per-meta / PrefixBlock per-material / MTE per-TE mHardness) — dispatch
	 *  to their getBlockHardness; the previous path via Properties.destroyTime was blind to them (MTE destroyTime=0 → tool
	 *  wear when harvesting machines = 0). A vanilla block — its defaultBlockState (the argument block matters more than the block at
	 *  the position: callers like BlockMetaType.getBlockHardness ask WD.hardness(Blocks.STONE, ...) with a FOREIGN position). */
	public static float hardness(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof gregapi.block.IBlock tBlock && aWorld instanceof Level tLevel) return tBlock.getBlockHardness(tLevel, aX, aY, aZ);
		return aBlock.defaultBlockState().getDestroySpeed(aWorld, new BlockPos(aX, aY, aZ));
	}
	/** F-hardness CENTER (pass #4 item 1 revision): the vanilla harvest-progress formula — 1.7.10
	 *  ForgeHooks.blockStrength(hardness, player, world, x, y, z) in ONE place. It was duplicated across 4 Block roots
	 *  (BlockBase/PrefixBlock/MTE-Block/MTE-Internal — no common ancestor, but it's the same formula); the roots call the center,
	 *  adding their own gates (MTE — per-TE IMTE_GetPlayerRelativeBlockHardness) on top. hardness<0 = indestructible. */
	public static float destroyProgress(float aHardness, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos) {
		if (aHardness < 0) return 0.0F;
		int tDivider = canHarvestBlock(aState, aWorld, aPos, aPlayer) ? 30 : 100; // same drop-right center used by GT6 blocks
		return aPlayer.getDestroySpeed(aState) / aHardness / (float)tDivider;
	}
	/** F9-harvest-level CENTER: 1.7.10 Block.getHarvestLevel(int aMeta) (numeric required harvest level 0=wood/
	 *  1=stone/2=iron/3=diamond) was removed by NAME, but the capability exists under neo's tag model. A GT6 block stores its own level
	 *  (BlockBase.getHarvestLevel(meta)); a vanilla block — via neo tier tags BlockTags.NEEDS_*_TOOL (the same meaning: which
	 *  tool tier is required). Direct mapping STONE=1/IRON=2/DIAMOND=3, otherwise 0. Used in the "tool
	 *  strong enough" check (MultiItemTool.getDigSpeed:517, used to be degraded to 0 = any tool dug any block). */
	/** BUG-071 CENTER (positional): the required block level AT THIS POSITION. Needed because in the port, meta for some
	 *  hierarchies is taken by something else (prefix — material ID, MTE — subtype in the BE) and degenerates to 0 on harvest paths,
	 *  while 1.7.10 held the REQUIRED QUALITY right in meta and called getHarvestLevel(meta). The dispatcher is thin: the
	 *  value is known by the block itself (contract {@link gregapi.block.IBlock#getHarvestLevel(BlockGetter,int,int,int)}), for vanilla —
	 *  the previous path via tier tags. Consumers — the harvest bridges (permission and speed) in GT_API_Proxy. */
	public static int harvestLevel(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block tBlock = block(aWorld, aX, aY, aZ);
		if (tBlock instanceof gregapi.block.IBlock tGT) return tGT.getHarvestLevel(aWorld, aX, aY, aZ);
		return harvestLevel(tBlock, meta(aWorld, aX, aY, aZ));
	}
	public static int harvestLevel(Block aBlock, int aMeta) {
		// dispatch by the IBlock CONTRACT (all GT6 hierarchies: BlockBase/MTE-Block/Internal/Prefix/Rail — no common ancestor;
		// the previous instanceof BlockBase was blind to MTE → machines "without a level" — class "two Block hierarchies")
		if (aBlock instanceof gregapi.block.IBlock tBlock) return tBlock.getHarvestLevel(aMeta);
		// exact original data before falling back to tier tags; the 1.7.10 default "level not set" = -1
		// (Block.java:2490), and it is carried over as-is: consumers clamp it themselves the same way
		// the original does (`MultiItemTool.java:482` bind4 → 0), and the "tool is stronger" comparison against -1 is correct anyway
		// (`ForgeHooks.java:115`).
		VanillaPassport tPassport = vanillaPassport(aBlock);
		if (tPassport != null) return tPassport.mLevel();
		BlockState tState = aBlock.defaultBlockState();
		if (tState.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
		if (tState.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL  )) return 2;
		if (tState.is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL )) return 1;
		return 0;
	}
	/** F-block-resistance CENTER: 1.7.10 {@code Block.setResistance(float)} — a RUNTIME patch of explosion-resistance (GT strengthens
	 *  vanilla blocks against explosions, globally on the shared Block object). neo {@code BlockBehaviour.explosionResistance} =
	 *  {@code protected final float} (BlockBehaviour.java:81, assigned in the constructor — not an inline constant) → no setter.
	 *  The field is opened by the Access Transformer and written DIRECTLY — GT6 canon (ADR F2 §2.3), the same technique used by
	 *  {@code Item.maxDamage} in {@code ST.setMaxDamage}. No reflection here: on 1.20.1 {@code Field.set}
	 *  on a final field does not go through at all (the same defect class that gave 29 silent failures in {@code ST.setItem}).
	 *  The field is read live — {@code Block.getExplosionResistance()} → {@code return this.explosionResistance}
	 *  (Block.java:336-337). GT6-1.7.10 performed exactly this global mutation of the vanilla block. */
	public static void setResistance(Block aBlock, float aResistance) {
		if (aBlock != null) aBlock.explosionResistance = aResistance;
	}
	/** F-tool CENTER: 1.7.10 {@code Block.getHarvestTool(int aMeta)} (a Forge hook on vanilla Block, string-typed
	 *  tool "pickaxe"/"shovel"/"axe"...) was removed by NAME; a GT6 block stores it ({@code BlockBase.getHarvestTool(meta)}),
	 *  a vanilla block → "" (as the core inlines UT.java:1202, ItemBlockBase:109). Centralizes the inline {@code instanceof BlockBase}. */
	/** BUG-071 CENTER: the TOOL level for a given class — 1:1 {@code Item.getHarvestLevel(stack, toolClass)}
	 *  from 1.7.10 (recompSrc {@code Item.java:1457-1461}: a {@code toolClasses} class→level map, {@code -1} if the class
	 *  is foreign to the item). GT6 tools answer through their own method (same as the original). Vanilla items in neo
	 *  no longer have a class map at all: class membership is expressed by an item TAG, and there is no numeric tier at all (replaced
	 *  by {@code INCORRECT_FOR_*_TOOL} tags) — so we ask the ENGINE with reference blocks (needs_stone/iron/
	 *  diamond) for the tier instead of a constants table. The answer is cached per item: the set of vanilla tiers never changes during a game. */
	public static int toolLevel(ItemStack aStack, String aToolClass) {
		if (ST.invalid(aStack) || !UT.Code.stringValid(aToolClass)) return -1;
		if (aStack.getItem() instanceof gregapi.item.multiitem.MultiItemTool tTool) return tTool.getHarvestLevel(aStack, aToolClass);
		if (!vanillaToolClassMatches(aStack, aToolClass)) return -1;
		return vanillaToolTier(aStack);
	}
	/** Whether a vanilla item belongs to a tool class (1.7.10: presence of the key in {@code toolClasses}). */
	private static boolean vanillaToolClassMatches(ItemStack aStack, String aToolClass) {
		switch (aToolClass) {
		case TOOL_pickaxe: return aStack.is(net.minecraft.tags.ItemTags.PICKAXES);
		case TOOL_axe    : return aStack.is(net.minecraft.tags.ItemTags.AXES);
		case TOOL_shovel : return aStack.is(net.minecraft.tags.ItemTags.SHOVELS);
		case TOOL_hoe    : return aStack.is(net.minecraft.tags.ItemTags.HOES);
		case TOOL_sword  : return aStack.is(net.minecraft.tags.ItemTags.SWORDS);
		case TOOL_shears : return aStack.getItem() == net.minecraft.world.item.Items.SHEARS;
		default          : return F; // wrench/crowbar/cutter/… — no vanilla carriers of these classes exist
		}
	}
	/** Numeric tier of a vanilla tool (0..3) — asked from the engine via reference blocks, cached per item.
	 *  Public because rigs display the same value (`gt6toolmatrixprobe`/`gt6toolyard`): a second copy of
	 *  this ladder must not exist in the tree — it was set up twice and was consolidated here. */
	private static final Map<net.minecraft.world.item.Item, Integer> VANILLA_TOOL_TIERS = new HashMap<>();
	public static int vanillaToolTier(ItemStack aStack) {
		Integer tCached = VANILLA_TOOL_TIERS.get(aStack.getItem());
		if (tCached != null) return tCached;
		int rTier = 0;
		try {
			if (aStack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()))    rTier = 1; // needs_stone_tool
			if (aStack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())) rTier = 2; // needs_iron_tool
			if (aStack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()))    rTier = 3; // needs_diamond_tool
		} catch (Throwable e) {/* reference block unavailable — stays 0 */}
		VANILLA_TOOL_TIERS.put(aStack.getItem(), rTier);
		return rTier;
	}

	/** F-tool CENTER (continued): for vanilla blocks the 1.7.10 tool class is DATA-DEFINED, not derived —
	 *  no neo tag is equivalent to it ({@code MINEABLE_WITH_*} is broader: it covers doors, fences, barrels, foliage, and
	 *  a {@code hoe} class did not exist for 1.7.10 vanilla at all). Taken from the passport; outside the table — {@code ""}, which for
	 *  the harvest rule is equivalent to the 1.7.10 default {@code null} ("ask the vanilla rule", {@code ForgeHooks:104}),
	 *  since all consumers compare the class via {@code equalsIgnoreCase} and an empty string matches nothing. */
	public static String harvestTool(Block aBlock, int aMeta) {
		// dispatch by the IBlock CONTRACT (see harvestLevel above: instanceof BlockBase was blind to MTE/Prefix/Rail —
		// the key did not "see" the machine as its own block → canHarvestBlock=false → the machine was NOT harvestable at all after
		// requiresCorrectToolForDrops; caught by the gt6seamprobe judge)
		if (aBlock instanceof gregapi.block.IBlock tBlock) return tBlock.getHarvestTool(aMeta);
		VanillaPassport tPassport = vanillaPassport(aBlock);
		return tPassport != null && tPassport.mTool() != null ? tPassport.mTool() : "";
	}
	/**
	 * BUG-071 SINGLE center for drop-eligibility — one for the whole mod. Verbatim port of Forge 1.7.10
	 * {@code ForgeHooks.canHarvestBlock} ({@code gt6-original build/tmp/recompSrc/net/minecraftforge/common/
	 * ForgeHooks.java:95-116}): material with no requirement -> allowed; no stack / block has no tool class assigned ->
	 * vanilla verdict; tool level &lt; 0 (class foreign to the item) -> vanilla verdict; otherwise compare
	 * LEVELS at the position.
	 *
	 * <p><b>Branch 1.20.1 — the HOME changed, not the rule.</b> On 26.x the single entry point was the
	 * {@code PlayerEvent.HarvestCheck} event handler, because that event carried the world and the position. On 1.20.1 it
	 * does not carry them at all ({@code PlayerEvent.java:69-81} — only player + state + success), while the world and
	 * position come from a different hook of the same engine: {@code IForgeBlock.canHarvestBlock(BlockState, BlockGetter, BlockPos, Player)}
	 * ({@code IForgeBlock.java:167-170}), and this is exactly the hook the engine asks through
	 * ({@code Player.hasCorrectToolForDrops(state, level, pos)} → {@code IForgeBlockState.canHarvestBlock}).
	 * So the rule moved here, and the GT6 block roots merely call it.</p>
	 *
	 * <p>Why it matters at all: without a position the rule is computed as {@code Item.isCorrectToolForDrops(stack, state)},
	 * and GT6's block subtype lives in the BlockEntity/chunk map — on that path meta degenerates to 0, and the required level
	 * became zero for ALL ores and machines (BUG-071, measured by gt6harvestprobe).</p>
	 */
	public static boolean canHarvestBlock(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer) {
		try {
			Block tBlock = aState.getBlock();
			if (getMaterial(tBlock).isToolNotRequired()) return T;                                                        // :97-100
			ItemStack tStack = aPlayer.getMainHandItem();
			String tTool = harvestTool(tBlock, meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));
			if (gregapi.util.ST.invalid(tStack) || !UT.Code.stringValid(tTool)) return net.minecraftforge.common.ForgeHooks.isCorrectToolForDrops(aState, aPlayer); // :102-107
			int tToolLevel = toolLevel(tStack, tTool);
			if (tToolLevel < 0) return net.minecraftforge.common.ForgeHooks.isCorrectToolForDrops(aState, aPlayer);        // :109-113 — class foreign to the item
			return tToolLevel >= harvestLevel(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());                              // :115
		} catch (Throwable e) {
			// drop eligibility must not crash block destruction — fall back to the vanilla verdict
			return net.minecraftforge.common.ForgeHooks.isCorrectToolForDrops(aState, aPlayer);
		}
	}

	/** F-motion: 1.7.10 WD.motionX(Entity)/Y/Z (public fields) -> neo Vec3 getDeltaMovement()/setDeltaMovement (Entity.java).
	 *  Component-wise writes must preserve the other two axes -> centralize here ONCE (philosophy §2). */
	public static double motionX(Entity aEntity) {return aEntity.getDeltaMovement().x;}
	public static double motionY(Entity aEntity) {return aEntity.getDeltaMovement().y;}
	public static double motionZ(Entity aEntity) {return aEntity.getDeltaMovement().z;}
	public static void setMotionX(Entity aEntity, double aX) {net.minecraft.world.phys.Vec3 v = aEntity.getDeltaMovement(); aEntity.setDeltaMovement(aX, v.y, v.z);}
	public static void setMotionY(Entity aEntity, double aY) {net.minecraft.world.phys.Vec3 v = aEntity.getDeltaMovement(); aEntity.setDeltaMovement(v.x, aY, v.z);}
	public static void setMotionZ(Entity aEntity, double aZ) {net.minecraft.world.phys.Vec3 v = aEntity.getDeltaMovement(); aEntity.setDeltaMovement(v.x, v.y, aZ);}
	/** 1.7.10 WD.opaque(Block) = overridable Block.isOpaqueCube() ("full opaque cube"). The 1.20.1 carrier is
	 *  BlockState.isSolidRender(BlockGetter,BlockPos) (full occlusion cube), which GT6 blocks feed per-class via
	 *  the getOcclusionShape bridge — same reasoning as visOpq below. canOcclude() here was wrong twice: it is
	 *  true for partial occluders (slabs, stairs) AND for every GT6 cross block (Properties lack noOcclusion),
	 *  so WD.set stripped grass under saplings on placement (original WD.java:482 passed isOpaqueCube). */
	public static boolean opaque(Block aBlock) {return aBlock.defaultBlockState().isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);}
	/** F-harvest-event (decisions/): 1.7.10 {@code ForgeEventFactory.fireBlockHarvesting} fired HarvestDropsEvent —
	 *  external mods edited the drop list and chance, the method returned the chance. neo: the drop model = engine-fired
	 *  {@code BlockDropsEvent} on spawning through the loot system, there is NO DIRECT EventHooks equivalent (checked against
	 *  neoforge/event/EventHooks.java). GT6 spawns the drop MANUALLY ({@code WD.dropBlockAsItem}), bypassing loot; no-op:
	 *  return {@code aDropChance} as-is, GT6 drop preserved 1:1. F-harvest-event impossible-1:1: a hook for
	 *  external-mod drop modification does not exist in neo for the manual-drop path; GT6 drop 1:1. */
	public static float fireBlockHarvesting(java.util.List<ItemStack> aDrops, Level aWorld, Block aBlock, int aX, int aY, int aZ, int aMeta, int aFortune, float aDropChance, boolean aSilkTouch, Player aPlayer) {return aDropChance;}
	/** F-render: 1.7.10 Block-normal-cube = isOpaque && renderAsNormalBlock && !canProvidePower — EXACTLY a "redstone
	 *  conductor" (a full opaque block, not a signal source). neo BlockState.isRedstoneConductor(BlockGetter,
	 *  BlockPos) (BlockBehaviour.java:616) — the canonical successor (§8). */
	public static boolean normalCube(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		return aBlock.defaultBlockState().isRedstoneConductor(aWorld, new BlockPos(aX, aY, aZ));
	}
	/** F-plant: 1.7.10 Block.canSustainPlant(IBlockAccess,x,y,z,side,IPlantable):boolean -> neo
	 *  IBlockExtension.canSustainPlant(BlockState,BlockGetter,BlockPos,Direction,BlockState):TriState (plant expressed as
	 *  BlockState, not IPlantable). CENTER for the whole mod's translation. A non-DEFAULT soil answer is its own word (mod hooks and
	 *  GT6 blocks: BlockStones:644, MultiTileEntityBlock:474). DEFAULT: it DID NOT EXIST in 1.7.10 — the soil
	 *  table Block.canSustainPlant:2237-2252 (recompSrc) decided by plant type; reproduced verbatim, the type
	 *  is expressed by the same vanilla representative block that callers used to express IPlantable. Split families
	 *  (dirt:0..2 -> DIRT/COARSE_DIRT/PODZOL, sand:0..1 -> SAND/RED_SAND) — via the Flattened center (headOf).
	 *  ⛔ The previous body collapsed toBoolean(F): DEFAULT became "does not grow" — GT6 tree saplings were knocked off
	 *  grass on the very first tick, B-flowers did not hold on sand; copies with toBoolean(T) in flowers/saplings, conversely,
	 *  held the flower on stone and in mid-air (measurement gt6flowerprobe, 2026-07-30). */
	public static boolean canSustainPlant(LevelAccessor aWorld, int aX, int aY, int aZ, Direction aSide, Block aPlant) {
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockState tSoil = state(aWorld, tPos);
		// Branch 1.20.1: the hook returns boolean and takes IPlantable (Block.java:514) — the 1.7.10 form verbatim;
		// "the plant decides itself" (TriState.DEFAULT on 26.x) is expressed by falling through to our own rules on false.
		if (aPlant instanceof net.minecraftforge.common.IPlantable tPlantable && tSoil.getBlock().canSustainPlant(tSoil, aWorld, tPos, aSide, tPlantable)) return T;
		Block tSelf = tSoil.getBlock(), tHead = gregapi.data.CS.Flattened.headOf(tSelf);
		if (tHead == null) tHead = tSelf;
		// ADAPT-015: vanilla mud is the single mud and carries the soil rule of BlockDiggable meta 0 — reeds,
		// bushes and Plains/Water/Desert/Beach grow, Crop and Nether do not (BlockDiggable canSustainPlant).
		if (tSelf == Blocks.MUD) return aPlant != Blocks.WHEAT && aPlant != Blocks.NETHER_WART;
		if (aPlant == Blocks.CACTUS)      return tSelf == Blocks.CACTUS || tHead == Blocks.SAND;  // cactus-on-cactus (:2222) + Desert (:2239)
		if (aPlant == Blocks.SUGAR_CANE)  return tSelf == Blocks.SUGAR_CANE                       // reed-on-reed (:2227) + Beach (:2245-2251)
			|| ((tSelf == Blocks.GRASS_BLOCK || tHead == Blocks.DIRT || tHead == Blocks.SAND)
				&& (state(aWorld, tPos.west()).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
				 || state(aWorld, tPos.east()).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
				 || state(aWorld, tPos.north()).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
				 || state(aWorld, tPos.south()).getFluidState().is(net.minecraft.tags.FluidTags.WATER)));
		if (aPlant == Blocks.NETHER_WART) return tSelf == Blocks.SOUL_SAND;                       // Nether (:2240)
		if (aPlant == Blocks.WHEAT)       return tSelf == Blocks.FARMLAND;                        // Crop (:2241)
		if (aPlant == Blocks.LILY_PAD)    return tSoil.getFluidState().isSource() && tSoil.getFluidState().is(net.minecraft.tags.FluidTags.WATER); // Water (:2244: material water + meta 0)
		// Plains (:2243) — the default BlockBush type in 1.7.10 (flowers, saplings, grasses)
		return tSelf == Blocks.GRASS_BLOCK || tHead == Blocks.DIRT || tSelf == Blocks.FARMLAND;
	}
	/** F-spawn: 1.7.10 {@code World.setSpawnLocation(x,y,z)}. Branch 1.20.1: the form survived almost verbatim —
	 *  {@code ServerLevel.setDefaultSpawnPos(BlockPos, float)} (no RespawnData record from 26.x here).
	 *  Centralized adapter (worldgen sets the world spawn). */
	public static void setSpawnLocation(LevelAccessor aWorld, int aX, int aY, int aZ) {
		if (aWorld instanceof net.minecraft.server.level.ServerLevel sl) sl.setDefaultSpawnPos(new BlockPos(aX, aY, aZ), 0.0F);
	}
	/** F-worldgen: 1.7.10 {@code Arrays.fill(chunk.getBiomeArray(), (byte)Biome.X.biomeID)} — the byte biome array is gone;
	 *  neo stores biomes in a per-section {@code PalettedContainer<Holder<Biome>>} (RO), the only setter is
	 *  {@code ChunkAccess.fillBiomesFromNoise(BiomeResolver, Climate.Sampler)} (ChunkAccess:447). Center: fills the whole
	 *  chunk with one biome via a constant-resolver. neo has no numeric biomeID -> addressed by {@code ResourceKey<Biome>}. */
	public static void setBiomes(LevelAccessor aWorld, ChunkAccess aChunk, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> aBiome) {
		if (!(aWorld instanceof net.minecraft.server.level.ServerLevel tSL)) return;
		net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> tHolder = aWorld.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(aBiome);
		aChunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> tHolder, tSL.getChunkSource().randomState().sampler());
	}
	/** F-worldgen: 1.7.10 {@code new WorldGenTrees(...).generate(world,rng,x,y,z)} (WorldGenTrees is gone) -> neo
	 *  feature system: place the vanilla {@code TreeFeatures.OAK} {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature#place}
	 *  (ConfiguredFeature:24). FORCED-ADAPTATION: 1.7.10 tree height/meta parameters -> a fixed OAK config (cosmetic). */
	public static void placeTree(LevelAccessor aWorld, int aX, int aY, int aZ) {
		if (!(aWorld instanceof net.minecraft.server.level.ServerLevel tSL)) return;
		aWorld.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).getOrThrow(net.minecraft.data.worldgen.features.TreeFeatures.OAK).value().place(tSL, tSL.getChunkSource().getGenerator(), tSL.getRandom(), new BlockPos(aX, aY, aZ));
	}
	/** F-dimension: 1.7.10 World-provider numeric id -> neo has NO numeric id (Level.dimension() =
	 *  ResourceKey<Level>). Vanilla 1:1: overworld=0, nether=-1, end=1 (Level.java:95-97). F-dimension impossible-1:1 (neo has no int-dim-id; vanilla 0/-1/1 work 1:1, modded->hash),
	 *  modded-dim-id): modded dimensions have no stable int in neo -> a hash of the key (unique within a session, but
	 *  GT6's switch-cases only match vanilla 0/-1/1 anyway, modded -> default; NBT-persisted modded id degrades). */
	public static int dimensionId(LevelAccessor aWorld) {
		if (aWorld == null) return 0;
		net.minecraft.resources.ResourceKey<Level> tKey = dimKey(aWorld);
		if (tKey == Level.OVERWORLD) return 0;
		if (tKey == Level.NETHER) return -1;
		if (tKey == Level.END) return 1;
		return tKey == null ? 0 : tKey.location().hashCode(); // neo ResourceKey: location()->identifier() (ResourceKey.java:52); a null key (exotic LevelAccessor without Level/ServerLevelAccessor) -> 0 as the overworld default
	}
	/** F9: 1.7.10 WD.getMaterial(Block) was removed in neo (the Material class is gone). A GT6 block (BlockBase) stores its ported
	 *  gregapi.block.Material; 1.7.10 vanilla blocks answer with the EXACT data of the original — the passport
	 *  {@link #vanillaPassport} (171 entries, taken by the oracle from a live 1.7.10). Below — generalizations by identity
	 *  and neo tags: they serve what is fundamentally absent from the passport — split neo families (flatten) and
	 *  blocks that DID NOT EXIST in 1.7.10 (copper, deepslate, corals…). For the latter no original answer
	 *  exists in nature, so the {@code Material.rock} tail is not deferred port debt,
	 *  but a classification of new content (adaptation log), and it does not participate in parity against 1.7.10. */
	/** F-sound: 1.7.10 WD.playStepSound(aWorld, x, y, z, block) —
	 *  a string sound-path + a separate call. neo: SoundType via state.getSoundType() -> getStepSound() (SoundEvent),
	 *  Level.playSound(null,x,y,z,SoundEvent,SoundSource.BLOCKS,vol,pitch) (Level.java:444). Step formula 1:1 (the same
	 *  for all callers: (vol+1)/2, pitch*0.8; `*0.5`≡`/2`). The center takes the block — SoundType from its defaultBlockState. */
	public static void playStepSound(Level aWorld, double aX, double aY, double aZ, Block aBlock) {
		net.minecraft.world.level.block.SoundType tSound = aBlock.defaultBlockState().getSoundType();
		// 1.7.10 callers played stepSound.func_150496_b() = PLACEMENT SOUND (all callers of this center are place paths) → getPlaceSound
		aWorld.playSound(null, aX, aY, aZ, tSound.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, (tSound.getVolume() + 1.0F) / 2.0F, tSound.getPitch() * 0.8F);
	}
	/** F-sound CENTER: 1.7.10 {@code Block.stepSound} (public field {@code Block.SoundType}) removed; neo — SoundType via
	 *  {@code defaultBlockState().getSoundType()} ({@code SoundType.java}). Querying a block's sound type — in one place. */
	public static net.minecraft.world.level.block.SoundType soundType(Block aBlock) {
		return aBlock.defaultBlockState().getSoundType();
	}
	/**
	 * PASSPORT OF A 1.7.10 VANILLA BLOCK — DATA taken from the LIVE original, not derived from neo tags.
	 *
	 * <p>1.7.10 held three fields on every {@code Block}, removed in neo BY NAME: {@code blockMaterial},
	 * {@code harvestTool[16]} (default <b>null</b>, {@code Block.java:2489}) and {@code harvestLevel[16]}
	 * (default <b>-1</b>, {@code :2490}). Vanilla block values were set by TWO sources, and neither
	 * of them is expressible by neo tags:</p>
	 * <ol>
	 *   <li>Forge — {@code ForgeHooks.initTools} ({@code ForgeHooks.java:154-189}): three lists
	 *       ({@code ItemPickaxe:11}, {@code ItemSpade:11}, {@code ItemAxe:11}) + 9 explicit strings.
	 *       No vanilla block has a {@code hoe} class at all — the {@code MINEABLE_WITH_HOE} tag is not equivalent to it;</li>
	 *   <li>GT6 itself — {@code gregtech6/.../GT_API.java:204-209} (bed/sponge/hay to axe, TNT and
	 *       the "monster egg" to pickaxe, obsidian to level 3). neo has no runtime mutator for a foreign block
	 *       ({@link gregapi.GT_API} flags this), but the mod asks NOT the engine but this center — so the
	 *       function is recoverable without mutating vanilla.</li>
	 * </ol>
	 *
	 * <p>The table below is <b>generated</b> from the golden set {@code engine_block_passport.csv}
	 * (oracle {@code DumpEngine.dumpBlockPassport} on a live 1.7.10) — 171 entries, one per original vanilla
	 * block; values are not invented or generalized. Verified against the same set in
	 * {@code PortDump.dumpBlockPassport}.</p>
	 *
	 * <p>Blocks that did not exist in 1.7.10, and split families (neo-flatten: {@code leaves}/{@code leaves2}
	 * → 12 foliage variants, etc.) are not found in the table by name — they are served by the tag-based
	 * generalization branches below, same as before.</p>
	 */
	private record VanillaPassport(Material mMaterial, String mTool, int mLevel) {}
	private static final Map<String, VanillaPassport> VANILLA_PASSPORT_BY_NAME = new HashMap<>(256);
	private static final Map<Block, VanillaPassport> VANILLA_PASSPORT = new java.util.IdentityHashMap<>(256);
	private static boolean VANILLA_PASSPORT_RESOLVED = F;

	private static void p(String aName, Material aMaterial, String aTool, int aLevel) {
		VANILLA_PASSPORT_BY_NAME.put(aName, new VanillaPassport(aMaterial, aTool, aLevel));
	}

	/**
	 * RENAMED AND SPLIT FAMILIES: where to look for the 1.7.10 passport if the name no longer exists in neo.
	 *
	 * <p>There are NO values here — only an address. Tool, tier and material are still taken from the passport
	 * captured from the live original; this map only tells which neo blocks it belongs to. The list is finite
	 * and machine-counted: of the 171 passport entries, names absent from neo number exactly 57, and only 12 of them carry
	 * a tool — those are the ones listed (measurement 2026-08-06).</p>
	 *
	 * <p>Without this, planks and logs stayed "toolless": in 1.7.10 the whole wood type lived under one name
	 * {@code minecraft:planks}/{@code log}, while neo split them into {@code oak_planks}, {@code birch_log} and so
	 * on. Player 2026-08-06: "stone and ores have it, but gravel, sand, wood, chest — don't".</p>
	 */
	private static final Map<String, net.minecraft.tags.TagKey<Block>> PASSPORT_FAMILY = new LinkedHashMap<>();
	private static final Map<String, String> PASSPORT_RENAMED = new LinkedHashMap<>();
	static {
		// one 1.7.10 name -> an entire neo family
		PASSPORT_FAMILY.put("minecraft:planks", net.minecraft.tags.BlockTags.PLANKS);
		PASSPORT_FAMILY.put("minecraft:log"   , net.minecraft.tags.BlockTags.LOGS  ); // and log2: shared tag, same passport (axe 0)
		PASSPORT_FAMILY.put("minecraft:bed"   , net.minecraft.tags.BlockTags.BEDS  );
		PASSPORT_FAMILY.put("minecraft:snow_layer", net.minecraft.tags.BlockTags.SNOW);
		// simply renamed
		PASSPORT_RENAMED.put("minecraft:grass"      , "minecraft:grass_block");
		PASSPORT_RENAMED.put("minecraft:golden_rail", "minecraft:powered_rail");
		PASSPORT_RENAMED.put("minecraft:lit_pumpkin", "minecraft:jack_o_lantern");
		PASSPORT_RENAMED.put("minecraft:quartz_ore" , "minecraft:nether_quartz_ore");
		PASSPORT_RENAMED.put("minecraft:double_stone_slab", "minecraft:smooth_stone"); // 1.7.10 double slab = a solid neo block
		PASSPORT_RENAMED.put("minecraft:monster_egg", "minecraft:infested_stone");
		// minecraft:lit_redstone_ore has no separate block in neo (glow became a property of redstone_ore,
		// which is already covered by name), minecraft:log2 is covered by the LOGS tag above — neither needs an address.
	}

	/** Resolving "1.7.10 name -> live neo block" is deferred to the first request: at {@code <clinit>} the block
	 *  registry is not yet populated. The name is looked up as-is, then among the renamed, then by family. */
	private static VanillaPassport vanillaPassport(Block aBlock) {
		if (!VANILLA_PASSPORT_RESOLVED) {
			VANILLA_PASSPORT_RESOLVED = T;
			for (Map.Entry<String, VanillaPassport> tEntry : VANILLA_PASSPORT_BY_NAME.entrySet()) try {
				String tName = tEntry.getKey();
				VanillaPassport tPassport = tEntry.getValue();
				net.minecraft.resources.ResourceLocation tID = new net.minecraft.resources.ResourceLocation(PASSPORT_RENAMED.getOrDefault(tName, tName));
				net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(tID).ifPresent(tBlock -> VANILLA_PASSPORT.put(tBlock, tPassport));
			} catch (Throwable e) {/* name absent from neo and no address assigned to it — served by the generalization branches */}
		}
		VanillaPassport rPassport = VANILLA_PASSPORT.get(aBlock);
		if (rPassport != null) return rPassport;
		// ⛔ THE FAMILY IS ASKED AT THE MOMENT OF THE QUERY, not ahead of time. Tag contents arrive with the datapack LATER than
		// the first passport request happens, and a pre-emptive registry walk yielded empty — and the miss was cached
		// forever: planks and logs stayed "toolless" (measurement 2026-08-06). The check is cheap
		// (4 tags), and only a FOUND result is cached — a negative answer must not freeze in before tags are ready.
		try {
			net.minecraft.world.level.block.state.BlockState tState = aBlock.defaultBlockState();
			for (Map.Entry<String, net.minecraft.tags.TagKey<Block>> tEntry : PASSPORT_FAMILY.entrySet()) {
				if (!tState.is(tEntry.getValue())) continue;
				VanillaPassport tPassport = VANILLA_PASSPORT_BY_NAME.get(tEntry.getKey());
				if (tPassport == null) continue;
				VANILLA_PASSPORT.put(aBlock, tPassport);
				return tPassport;
			}
		} catch (Throwable e) {/* state/tags not ready yet — will answer on the next request */}
		return null;
	}

	static {
		p("minecraft:acacia_stairs",                 Material.wood,          null,            -1);
		p("minecraft:activator_rail",                Material.circuits,      TOOL_pickaxe,       0);
		p("minecraft:air",                           Material.air,           null,            -1);
		p("minecraft:anvil",                         Material.anvil,         null,            -1);
		p("minecraft:beacon",                        Material.glass,         null,            -1);
		p("minecraft:bed",                           Material.cloth,         TOOL_axe,           0);
		p("minecraft:bedrock",                       Material.rock,          null,            -1);
		p("minecraft:birch_stairs",                  Material.wood,          null,            -1);
		p("minecraft:bookshelf",                     Material.wood,          TOOL_axe,           0);
		p("minecraft:brewing_stand",                 Material.iron,          null,            -1);
		p("minecraft:brick_block",                   Material.rock,          null,            -1);
		p("minecraft:brick_stairs",                  Material.rock,          null,            -1);
		p("minecraft:brown_mushroom",                Material.plants,        null,            -1);
		p("minecraft:brown_mushroom_block",          Material.wood,          null,            -1);
		p("minecraft:cactus",                        Material.cactus,        null,            -1);
		p("minecraft:cake",                          Material.cake,          null,            -1);
		p("minecraft:carpet",                        Material.carpet,        null,            -1);
		p("minecraft:carrots",                       Material.plants,        null,            -1);
		p("minecraft:cauldron",                      Material.iron,          null,            -1);
		p("minecraft:chest",                         Material.wood,          TOOL_axe,           0);
		p("minecraft:clay",                          Material.clay,          TOOL_shovel,        0);
		p("minecraft:coal_block",                    Material.rock,          null,            -1);
		p("minecraft:coal_ore",                      Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:cobblestone",                   Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:cobblestone_wall",              Material.rock,          null,            -1);
		p("minecraft:cocoa",                         Material.plants,        null,            -1);
		p("minecraft:command_block",                 Material.iron,          null,            -1);
		p("minecraft:crafting_table",                Material.wood,          null,            -1);
		p("minecraft:dark_oak_stairs",               Material.wood,          null,            -1);
		p("minecraft:daylight_detector",             Material.wood,          null,            -1);
		p("minecraft:deadbush",                      Material.vine,          null,            -1);
		p("minecraft:detector_rail",                 Material.circuits,      TOOL_pickaxe,       0);
		p("minecraft:diamond_block",                 Material.iron,          TOOL_pickaxe,       2);
		p("minecraft:diamond_ore",                   Material.rock,          TOOL_pickaxe,       2);
		p("minecraft:dirt",                          Material.ground,        TOOL_shovel,        0);
		p("minecraft:dispenser",                     Material.rock,          null,            -1);
		p("minecraft:double_plant",                  Material.plants,        null,            -1);
		p("minecraft:double_stone_slab",             Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:double_wooden_slab",            Material.wood,          null,            -1);
		p("minecraft:dragon_egg",                    Material.dragonEgg,     null,            -1);
		p("minecraft:dropper",                       Material.rock,          null,            -1);
		p("minecraft:emerald_block",                 Material.iron,          TOOL_pickaxe,       2);
		p("minecraft:emerald_ore",                   Material.rock,          TOOL_pickaxe,       2);
		p("minecraft:enchanting_table",              Material.rock,          null,            -1);
		p("minecraft:end_portal",                    Material.portal,        null,            -1);
		p("minecraft:end_portal_frame",              Material.rock,          null,            -1);
		p("minecraft:end_stone",                     Material.rock,          null,            -1);
		p("minecraft:ender_chest",                   Material.rock,          null,            -1);
		p("minecraft:farmland",                      Material.ground,        TOOL_shovel,        0);
		p("minecraft:fence",                         Material.wood,          null,            -1);
		p("minecraft:fence_gate",                    Material.wood,          null,            -1);
		p("minecraft:fire",                          Material.fire,          null,            -1);
		p("minecraft:flower_pot",                    Material.circuits,      null,            -1);
		p("minecraft:flowing_lava",                  Material.lava,          null,            -1);
		p("minecraft:flowing_water",                 Material.water,         null,            -1);
		p("minecraft:furnace",                       Material.rock,          null,            -1);
		p("minecraft:glass",                         Material.glass,         null,            -1);
		p("minecraft:glass_pane",                    Material.glass,         null,            -1);
		p("minecraft:glowstone",                     Material.glass,         null,            -1);
		p("minecraft:gold_block",                    Material.iron,          TOOL_pickaxe,       2);
		p("minecraft:gold_ore",                      Material.rock,          TOOL_pickaxe,       2);
		p("minecraft:golden_rail",                   Material.circuits,      TOOL_pickaxe,       0);
		p("minecraft:grass",                         Material.grass,         TOOL_shovel,        0);
		p("minecraft:gravel",                        Material.sand,          TOOL_shovel,        0);
		p("minecraft:hardened_clay",                 Material.rock,          null,            -1);
		p("minecraft:hay_block",                     Material.grass,         TOOL_axe,           0);
		p("minecraft:heavy_weighted_pressure_plate", Material.iron,          null,            -1);
		p("minecraft:hopper",                        Material.iron,          null,            -1);
		p("minecraft:ice",                           Material.ice,           TOOL_pickaxe,       0);
		p("minecraft:iron_bars",                     Material.iron,          null,            -1);
		p("minecraft:iron_block",                    Material.iron,          TOOL_pickaxe,       1);
		p("minecraft:iron_door",                     Material.iron,          null,            -1);
		p("minecraft:iron_ore",                      Material.rock,          TOOL_pickaxe,       1);
		p("minecraft:jukebox",                       Material.wood,          null,            -1);
		p("minecraft:jungle_stairs",                 Material.wood,          null,            -1);
		p("minecraft:ladder",                        Material.circuits,      null,            -1);
		p("minecraft:lapis_block",                   Material.iron,          TOOL_pickaxe,       1);
		p("minecraft:lapis_ore",                     Material.rock,          TOOL_pickaxe,       1);
		p("minecraft:lava",                          Material.lava,          null,            -1);
		p("minecraft:leaves",                        Material.leaves,        null,            -1);
		p("minecraft:leaves2",                       Material.leaves,        null,            -1);
		p("minecraft:lever",                         Material.circuits,      null,            -1);
		p("minecraft:light_weighted_pressure_plate", Material.iron,          null,            -1);
		p("minecraft:lit_furnace",                   Material.rock,          null,            -1);
		p("minecraft:lit_pumpkin",                   Material.gourd,         TOOL_axe,           0);
		p("minecraft:lit_redstone_lamp",             Material.redstoneLight, null,            -1);
		p("minecraft:lit_redstone_ore",              Material.rock,          TOOL_pickaxe,       2);
		p("minecraft:log",                           Material.wood,          TOOL_axe,           0);
		p("minecraft:log2",                          Material.wood,          TOOL_axe,           0);
		p("minecraft:melon_block",                   Material.gourd,         null,            -1);
		p("minecraft:melon_stem",                    Material.plants,        null,            -1);
		p("minecraft:mob_spawner",                   Material.rock,          null,            -1);
		p("minecraft:monster_egg",                   Material.clay,          TOOL_pickaxe,       0);
		p("minecraft:mossy_cobblestone",             Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:mycelium",                      Material.grass,         TOOL_shovel,        0);
		p("minecraft:nether_brick",                  Material.rock,          null,            -1);
		p("minecraft:nether_brick_fence",            Material.rock,          null,            -1);
		p("minecraft:nether_brick_stairs",           Material.rock,          null,            -1);
		p("minecraft:nether_wart",                   Material.plants,        null,            -1);
		p("minecraft:netherrack",                    Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:noteblock",                     Material.wood,          null,            -1);
		p("minecraft:oak_stairs",                    Material.wood,          null,            -1);
		p("minecraft:obsidian",                      Material.rock,          TOOL_pickaxe,       3);
		p("minecraft:packed_ice",                    Material.packedIce,     null,            -1);
		p("minecraft:piston",                        Material.piston,        null,            -1);
		p("minecraft:piston_extension",              Material.piston,        null,            -1);
		p("minecraft:piston_head",                   Material.piston,        null,            -1);
		p("minecraft:planks",                        Material.wood,          TOOL_axe,           0);
		p("minecraft:portal",                        Material.portal,        null,            -1);
		p("minecraft:potatoes",                      Material.plants,        null,            -1);
		p("minecraft:powered_comparator",            Material.circuits,      null,            -1);
		p("minecraft:powered_repeater",              Material.circuits,      null,            -1);
		p("minecraft:pumpkin",                       Material.gourd,         TOOL_axe,           0);
		p("minecraft:pumpkin_stem",                  Material.plants,        null,            -1);
		p("minecraft:quartz_block",                  Material.rock,          null,            -1);
		p("minecraft:quartz_ore",                    Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:quartz_stairs",                 Material.rock,          null,            -1);
		p("minecraft:rail",                          Material.circuits,      TOOL_pickaxe,       0);
		p("minecraft:red_flower",                    Material.plants,        null,            -1);
		p("minecraft:red_mushroom",                  Material.plants,        null,            -1);
		p("minecraft:red_mushroom_block",            Material.wood,          null,            -1);
		p("minecraft:redstone_block",                Material.iron,          null,            -1);
		p("minecraft:redstone_lamp",                 Material.redstoneLight, null,            -1);
		p("minecraft:redstone_ore",                  Material.rock,          TOOL_pickaxe,       2);
		p("minecraft:redstone_torch",                Material.circuits,      null,            -1);
		p("minecraft:redstone_wire",                 Material.circuits,      null,            -1);
		p("minecraft:reeds",                         Material.plants,        null,            -1);
		p("minecraft:sand",                          Material.sand,          TOOL_shovel,        0);
		p("minecraft:sandstone",                     Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:sandstone_stairs",              Material.rock,          null,            -1);
		p("minecraft:sapling",                       Material.plants,        null,            -1);
		p("minecraft:skull",                         Material.circuits,      null,            -1);
		p("minecraft:snow",                          Material.craftedSnow,   TOOL_shovel,        0);
		p("minecraft:snow_layer",                    Material.snow,          TOOL_shovel,        0);
		p("minecraft:soul_sand",                     Material.sand,          TOOL_shovel,        0);
		p("minecraft:sponge",                        Material.sponge,        TOOL_axe,           0);
		p("minecraft:spruce_stairs",                 Material.wood,          null,            -1);
		p("minecraft:stained_glass",                 Material.glass,         null,            -1);
		p("minecraft:stained_glass_pane",            Material.glass,         null,            -1);
		p("minecraft:stained_hardened_clay",         Material.rock,          null,            -1);
		p("minecraft:standing_sign",                 Material.wood,          null,            -1);
		p("minecraft:sticky_piston",                 Material.piston,        null,            -1);
		p("minecraft:stone",                         Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:stone_brick_stairs",            Material.rock,          null,            -1);
		p("minecraft:stone_button",                  Material.circuits,      null,            -1);
		p("minecraft:stone_pressure_plate",          Material.rock,          null,            -1);
		p("minecraft:stone_slab",                    Material.rock,          TOOL_pickaxe,       0);
		p("minecraft:stone_stairs",                  Material.rock,          null,            -1);
		p("minecraft:stonebrick",                    Material.rock,          null,            -1);
		p("minecraft:tallgrass",                     Material.vine,          null,            -1);
		p("minecraft:tnt",                           Material.tnt,           TOOL_pickaxe,       0);
		p("minecraft:torch",                         Material.circuits,      null,            -1);
		p("minecraft:trapdoor",                      Material.wood,          null,            -1);
		p("minecraft:trapped_chest",                 Material.wood,          null,            -1);
		p("minecraft:tripwire",                      Material.circuits,      null,            -1);
		p("minecraft:tripwire_hook",                 Material.circuits,      null,            -1);
		p("minecraft:unlit_redstone_torch",          Material.circuits,      null,            -1);
		p("minecraft:unpowered_comparator",          Material.circuits,      null,            -1);
		p("minecraft:unpowered_repeater",            Material.circuits,      null,            -1);
		p("minecraft:vine",                          Material.vine,          null,            -1);
		p("minecraft:wall_sign",                     Material.wood,          null,            -1);
		p("minecraft:water",                         Material.water,         null,            -1);
		p("minecraft:waterlily",                     Material.plants,        null,            -1);
		p("minecraft:web",                           Material.web,           null,            -1);
		p("minecraft:wheat",                         Material.plants,        null,            -1);
		p("minecraft:wooden_button",                 Material.circuits,      null,            -1);
		p("minecraft:wooden_door",                   Material.wood,          null,            -1);
		p("minecraft:wooden_pressure_plate",         Material.wood,          null,            -1);
		p("minecraft:wooden_slab",                   Material.wood,          null,            -1);
		p("minecraft:wool",                          Material.cloth,         null,            -1);
		p("minecraft:yellow_flower",                 Material.plants,        null,            -1);
	}

	public static gregapi.block.Material getMaterial(Block aBlock) {
		// SELECTION BY CONTRACT, not by enumerating hierarchies. This used to have five instanceof checks
		// (BlockBase, BlockFluidBaseGT, MultiTileEntityBlock, BlockBaseFlower, BlockBaseRail), and a carrier
		// missing from the list silently got the `rock` tail: that's how PrefixBlock fell through — crates answered "stone"
		// instead of wood, ores in sand/gravel/dirt instead of sand/ground, machine housings instead of iron (27 blocks,
		// caught by the engine_block_passport.csv set against a live 1.7.10). The value is still known by the
		// block itself — the center only asks it, the same way as harvestTool/harvestLevel.
		if (aBlock instanceof gregapi.block.IBlock tGT) {
			gregapi.block.Material tMaterial = tGT.getMaterial();
			if (tMaterial != null) return tMaterial;
		}
		// (PLAYER REPORT "I break a battery box/barrel/crucible with a pickaxe and it drops" is closed by this same contract:
		// machines/flowers/rails do not extend BlockBase — machines are Block, flowers are FlowerBlock, rails are BaseRailBlock —
		// and used to fall through into the vanilla-block parsing, getting the `rock` tail, meaning the PICKAXE TREATED THE MACHINE
		// AS STONE, even though the block declares a wrench as its tool. In 1.7.10 the material was a method of the block itself.)
		// EXACT ORIGINAL DATA comes BEFORE the generalizations: the branches below (identity + neo tags) derive the material
		// by family and are therefore inevitably approximate — measurement against 1.7.10 gave 51 discrepancies out of 114 shared
		// vanilla blocks, almost all collapsed into rock (iron 14, circuits 12, glass/plants/wood 4 each, piston 3).
		// The generalizations remain: they serve split families and blocks that did not exist in 1.7.10.
		VanillaPassport tPassport = vanillaPassport(aBlock);
		if (tPassport != null) return tPassport.mMaterial();
		net.minecraft.world.level.block.state.BlockState tState = aBlock.defaultBlockState();
		if (tState.isAir())                                                                                      return gregapi.block.Material.air;
		if (aBlock == Blocks.WATER || aBlock == Blocks.BUBBLE_COLUMN)                                            return gregapi.block.Material.water;
		if (aBlock == Blocks.LAVA)                                                                               return gregapi.block.Material.lava;
		if (aBlock == Blocks.FIRE || aBlock == Blocks.SOUL_FIRE)                                                 return gregapi.block.Material.fire;
		if (aBlock == Blocks.CACTUS)                                                                             return gregapi.block.Material.cactus;
		if (aBlock == Blocks.VINE)                                                                               return gregapi.block.Material.vine;
		if (aBlock == Blocks.CLAY)                                                                               return gregapi.block.Material.clay;
		if (aBlock == Blocks.MELON || aBlock == Blocks.PUMPKIN || aBlock == Blocks.CARVED_PUMPKIN || aBlock == Blocks.JACK_O_LANTERN) return gregapi.block.Material.gourd;
		if (aBlock == Blocks.GRASS_BLOCK || aBlock == Blocks.MYCELIUM || aBlock == Blocks.PODZOL)                return gregapi.block.Material.grass;
		if (aBlock == Blocks.PACKED_ICE)                                                                         return gregapi.block.Material.packedIce;
		if (aBlock == Blocks.ICE || aBlock == Blocks.BLUE_ICE || aBlock == Blocks.FROSTED_ICE)                   return gregapi.block.Material.ice;
		if (aBlock == Blocks.SNOW || aBlock == Blocks.SNOW_BLOCK || aBlock == Blocks.POWDER_SNOW)                return gregapi.block.Material.snow;
		if (aBlock == Blocks.COBWEB)                                                                             return gregapi.block.Material.web;
		if (aBlock == Blocks.TNT)                                                                                return gregapi.block.Material.tnt;
		if (tState.is(net.minecraft.tags.BlockTags.SAND))                                                        return gregapi.block.Material.sand;
		if (aBlock == Blocks.DIRT || aBlock == Blocks.COARSE_DIRT || aBlock == Blocks.GRAVEL || aBlock == Blocks.FARMLAND || aBlock == Blocks.DIRT_PATH || aBlock == Blocks.ROOTED_DIRT || aBlock == Blocks.SOUL_SAND || aBlock == Blocks.SOUL_SOIL
		 // ADAPT-015: mud carries the material of BlockDiggable meta 0 it replaced; without it the hardness
		 // and worldgen branches read the fallback `rock` and dug mud like stone.
		 || aBlock == Blocks.MUD) return gregapi.block.Material.ground;
		if (tState.is(net.minecraft.tags.BlockTags.LEAVES))                                                      return gregapi.block.Material.leaves;
		// BUG-013: derived wooden blocks. 1.7.10: BlockWoodSlab/BlockDoor(wood)/trapdoor/fence/fence_gate/
		// wooden_pressure_plate/BlockSign = Material.wood, wooden stairs inherit the material of their donor planks
		// (Block.java:316,329,341 + BlockWoodSlab:22/BlockSign:25/BlockFenceGate:24 of the 1.7.10 reference). Buttons and
		// ladder in 1.7.10 = Material.circuits — NOT included (1:1). FENCE_GATES has no wooden variant: in vanilla all gates are wooden.
		if (tState.is(net.minecraft.tags.BlockTags.LOGS) || tState.is(net.minecraft.tags.BlockTags.PLANKS)
		 || tState.is(net.minecraft.tags.BlockTags.WOODEN_SLABS) || tState.is(net.minecraft.tags.BlockTags.WOODEN_STAIRS)
		 || tState.is(net.minecraft.tags.BlockTags.WOODEN_DOORS) || tState.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS)
		 || tState.is(net.minecraft.tags.BlockTags.WOODEN_FENCES) || tState.is(net.minecraft.tags.BlockTags.FENCE_GATES)
		 || tState.is(net.minecraft.tags.BlockTags.WOODEN_PRESSURE_PLATES) || tState.is(net.minecraft.tags.BlockTags.ALL_SIGNS)
		 || aBlock == Blocks.CRAFTING_TABLE || aBlock == Blocks.BOOKSHELF || aBlock == Blocks.CHEST || aBlock == Blocks.JUKEBOX || aBlock == Blocks.NOTE_BLOCK) return gregapi.block.Material.wood;
		if (tState.is(net.minecraft.tags.BlockTags.WOOL_CARPETS))                                                return gregapi.block.Material.carpet;
		if (tState.is(net.minecraft.tags.BlockTags.WOOL))                                                        return gregapi.block.Material.cloth;
		// FAMILIES SPLIT BY neo: in 1.7.10 these were blocks with a meta-subtype, their names no longer exist in neo, and the passport
		// is not found by name. The material used to be taken from the `rock` tail — measurement against the original gave 62 such blocks
		// (`stained_glass`/`stained_glass_pane` → glass 32 pcs., `skull` → circuits 15, `wooden_button` → circuits 12,
		// `repeater`/`comparator` → circuits 2, `portal` → portal 1). Selection by engine CLASS, not by a name list:
		// the list of wood types and colors grows from version to version, but a family has one class.
		if (aBlock instanceof net.minecraft.world.level.block.StainedGlassBlock
		 || aBlock instanceof net.minecraft.world.level.block.StainedGlassPaneBlock)                            return gregapi.block.Material.glass;
		if (aBlock instanceof net.minecraft.world.level.block.AbstractSkullBlock)                                return gregapi.block.Material.circuits;
		if (aBlock instanceof net.minecraft.world.level.block.DiodeBlock)                                        return gregapi.block.Material.circuits; // repeater + comparator
		if (aBlock instanceof net.minecraft.world.level.block.ButtonBlock
		 && tState.is(net.minecraft.tags.BlockTags.WOODEN_BUTTONS))                                             return gregapi.block.Material.circuits; // a stone button in 1.7.10 = rock (tail)
		if (aBlock == Blocks.NETHER_PORTAL)                                                                      return gregapi.block.Material.portal;
		// flower pot: in 1.7.10 it was ONE block, contents lived in the TileEntity — neo split it into 38 occupied
		// (`potted_*`) plus an empty one. The empty one was found by name and answered circuits, the occupied ones fell into the `rock` tail.
		if (aBlock instanceof net.minecraft.world.level.block.FlowerPotBlock)                                    return gregapi.block.Material.circuits;
		// BUG-012: 1.7.10 BlockTallGrass (short grass/fern) and BlockDeadBush = Material.vine
		// (BlockTallGrass:33/BlockDeadBush:23 of the 1.7.10 reference) — the knife/scythe/sword gate accepts vine.
		// ADAPT-006/007 (BUSH, SHORT_DRY_GRASS) — content added by the engine in 26.1.2; absent on 1.20.1,
		// and absent in the 1.7.10 original too — the branches are dropped, the group content is back to the original.
		if (aBlock == Blocks.GRASS || aBlock == Blocks.FERN || aBlock == Blocks.DEAD_BUSH) return gregapi.block.Material.vine;
		// BUG-012: 1.7.10 BlockDoublePlant (all 6: sunflower/lilac/tall grass/large fern/rose bush/peony)
		// and BlockLilyPad (BlockBush:30) = Material.plants (BlockDoublePlant:37 of the 1.7.10 reference).
		if (tState.is(net.minecraft.tags.BlockTags.SAPLINGS) || tState.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) || tState.is(net.minecraft.tags.BlockTags.FLOWERS) || tState.is(net.minecraft.tags.BlockTags.CROPS)
		 || aBlock == Blocks.SUGAR_CANE || aBlock == Blocks.SUNFLOWER || aBlock == Blocks.LILAC || aBlock == Blocks.ROSE_BUSH || aBlock == Blocks.PEONY
		 || aBlock == Blocks.TALL_GRASS || aBlock == Blocks.LARGE_FERN || aBlock == Blocks.LILY_PAD) return gregapi.block.Material.plants;
		return gregapi.block.Material.rock;
	}

	/** F-block-behavior: 1.7.10 {@code Block.isReplaceable/isSideSolid/isReplaceableOreGen} were removed in neo
	 *  (absent from {@code Block.java}, {@code BlockBehaviour.java}, and every one of the 3 reference roots). GT6 blocks
	 *  (BlockBase, MultiTileEntityBlock, BlockStones) define their own versions themselves (compiled as their own
	 *  methods) — centralize here the CALLS on receivers of static type vanilla {@code Block}: an instanceof
	 *  dispatcher (virtual dispatch resolves down to the real subclass override), otherwise — the 1.7.10 Forge default. */
	/** used to be {@code tBlock.isReplaceable(aWorld, aX, aY, aZ)} — the 1.7.10 {@code Block.isReplaceable} default =
	 *  {@code blockMaterial.isReplaceable()} (BlockBase does NOT override it, see `gregtech6/.../BlockBase.java`,
	 *  uses the material), MultiTileEntityBlock overrides it (TileEntity delegation). */
	public static boolean replaceable(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isReplaceable(aWorld, aX, aY, aZ);
		return getMaterial(aBlock).isReplaceable();
	}
	/** used to be {@code aBlock.isSideSolid(aWorld, aX, aY, aZ, aSide)} — BlockBase.java:95 overrides it (and all its
	 *  subclasses via virtual dispatch), MultiTileEntityBlock.java:279 overrides it (TileEntity delegation).
	 *  The vanilla neo equivalent of the default is {@code BlockState.isFaceSturdy(BlockGetter,BlockPos,Direction)}
	 *  (BlockBehaviour.java:876). */
	public static boolean sideSolid(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {
		if (aBlock instanceof BlockBase) return ((BlockBase)aBlock).isSideSolid(aWorld, aX, aY, aZ, aSide);
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isSideSolid(aWorld, aX, aY, aZ, aSide);
		return aBlock.defaultBlockState().isFaceSturdy(aWorld, new BlockPos(aX, aY, aZ), aSide);
	}
	/** used to be {@code aBlock.isReplaceableOreGen(aWorld, aX, aY, aZ, aTarget)} — BlockBase does NOT override it
	 *  (checked against `gregtech6/.../BlockBase.java`, default), only MultiTileEntityBlock.java:245
	 *  (TileEntity delegation) and BlockStones.java:746 (stone ores/generation) override it. The vanilla Forge 1.7.10
	 *  {@code Block.isReplaceableOreGen} default = identity ({@code this==target}). */
	public static boolean oreGen(Block aBlock, LevelAccessor aWorld, int aX, int aY, int aZ, Block aTarget) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isReplaceableOreGen(aWorld, aX, aY, aZ, aTarget);
		if (aBlock instanceof BlockStones) return ((BlockStones)aBlock).isReplaceableOreGen(aWorld, aX, aY, aZ, aTarget);
		return aBlock == aTarget;
	}
	/** used to be {@code aBlock.isWood(aWorld,x,y,z)} (a Forge block-behavior, removed) — GT6 blocks (MTE/BlockBaseLog) override it;
	 *  the vanilla Forge default = false, except for logs -> neo BlockTags.LOGS (1.7.10 vanilla BlockLog.isWood=true). */
	public static boolean wood(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isWood(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.tree.BlockBaseLog) return ((gregapi.block.tree.BlockBaseLog)aBlock).isWood(aWorld, aX, aY, aZ);
		return aBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS);
	}
	/** used to be {@code aBlock.isLeaves(aWorld,x,y,z)} (a Forge block-behavior, removed) — GT6 (MTE/BlockBaseLeaves) override it;
	 *  the vanilla default is false, except for leaves -> neo BlockTags.LEAVES. */
	public static boolean leaves(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.tree.BlockBaseLeaves) return ((gregapi.block.tree.BlockBaseLeaves)aBlock).isLeaves(aWorld, aX, aY, aZ);
		return aBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES);
	}

	public static byte WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD = 0;
	
	public static BlockEntity invalidateTileEntityWithNegativeYCoord(int aX, int aY, int aZ, BlockEntity aTileEntity) {
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD == 0) UT.Entities.chat(null, LH.tt("Please provide the gregtech.log File to Greg, there was a weird Error"));
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD < 10) {
			ERR.println("===============================");
			ERR.println("X:" + aX);
			ERR.println("Y:" + aY);
			ERR.println("Z:" + aZ);
			ERR.println("Class:" + aTileEntity.getClass());
			new Throwable().printStackTrace(ERR);
			ERR.println("===============================");
		}
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD == 9) UT.Entities.chat(null, LH.tt("Please provide the gregtech.log File to Greg, there was a LOT of weird Errors"));
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD < 99) WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD++;
		aTileEntity.setRemoved(); // used to be .invalidate() — neo: BlockEntity.setRemoved() (BlockEntity.java:252)
		// F IMPOSSIBLE-1:1 + OBSOLETE (blockentity-position-immutable): used to be aTileEntity.y = 0 — neo BlockEntity.worldPosition
		// (BlockEntity.java:48) protected final, set by the constructor, no setter exists in any of the 3 reference roots. Unreachable AND
		// unneeded: zeroing Y was a 1.7.10 hack against a bug with a TE at "negative" Y (back then Y∈0..255); in neo Y∈-64..320
		// is legitimate, no anomaly. setRemoved() above is the correct removal; reproducing the Y-reset is pointless.
		return aTileEntity;
	}
	
	/** Sets the TileEntity at the passed position, with the option of turning adjacent TileEntity updates off. */
	public static BlockEntity te(LevelAccessor aWorld, int aX, int aY, int aZ, BlockEntity aTileEntity, boolean aCauseTileEntityUpdates) {
		if (tileYInvalid(aWorld, aY)) return invalidateTileEntityWithNegativeYCoord(aX, aY, aZ, aTileEntity); // used to be aY<0 — MC26 bedrock Y=−64 is legitimate, the threshold is the world floor getMinY()
		// N-5 CENTER (mirrors main): remove a possible "packed" entry of the PREVIOUS BE at this position BEFORE
		// the force-swap below — on an MTE-to-MTE change with the SAME physical Block class, the "block changed"
		// criterion of LevelChunk.setBlockState on main (neo) — Block-identity (oldState.is(newBlock),
		// LevelChunk.java:304) — is physically unreachable regardless of BlockState properties; on this branch (forge
		// 1.20.1) the criterion is BlockState-instance identity (blockstate==p_62866_, LevelChunk.java:224), which is
		// why in live measurements the path more often clears itself (proven by the stand: COLD without this fix already
		// gives GREEN on gt6pending, case D). The fix remains a safety net for the same defect class on paths where the
		// BlockState object DOES MATCH (a property-less singleton defaultBlockState) — there too the unpacking would be
		// physically unreachable, by the same technique sweepBlockEntityRemains (MultiTileEntityBlock.java) closes for
		// the "block fully removed" case: a chunk.getBlockEntity(pos) query itself unpacks pendingBlockEntities and
		// removes the entry from there (ChunkAccess.getBlockEntity → pendingBlockEntities.remove); the put() below
		// (Level.setBlockEntity/ChunkAccess.setBlockEntity) correctly removes the temporarily unpacked object through
		// the normal map-entry replacement.
		try {
			ChunkAccess tPendingChunk = aWorld.getChunk(aX >> 4, aZ >> 4);
			BlockPos tPendingPos = new BlockPos(aX, aY, aZ);
			if (tPendingChunk != null && tPendingChunk.getBlockEntityNbt(tPendingPos) != null) tPendingChunk.getBlockEntity(tPendingPos);
		} catch (Throwable e) {e.printStackTrace(ERR);}
		// F-tick (the channel shifted): in 1.7.10 BOTH branches (World.setTileEntity AND Chunk.setTileEntity) added the TE to the world
		// tick cycle loadedTileEntityList; in neo the ticker is registered ONLY through Level.setBlockEntity→addAndRegisterBlockEntity
		// (LevelChunk.setBlockEntity/ChunkAccess — a map without a ticker). So on a real Level we ALWAYS go the full path —
		// otherwise a client-sync BE (receiveData*, aCauseTileEntityUpdates=F) exists but does not tick (chest lid/client animations).
		if (aWorld instanceof Level tLevel) tLevel.setBlockEntity(aTileEntity); // Level.java:681 → addAndRegisterBlockEntity (position from te.getBlockPos())
		else {
			// F6-worldgen CENTER for BE placement: the receiver is widened to LevelAccessor (worldgen operates through WorldGenLevel/WorldGenRegion,
			// not Level). getChunk on a LevelReader gives back ChunkAccess (Level=full chunk, worldgen=ProtoChunk/ImposterProtoChunk).
			// ChunkAccess.setBlockEntity(BlockEntity) (ChunkAccess.java:129, abstract — exists on both LevelChunk and ProtoChunk)
			// puts the BE into the chunk without updating neighbors (the analog of the 1.7.10 "without TileEntity-update" branch). The previous
			// LevelChunk.addAndRegisterBlockEntity was available ONLY on a full chunk (Level) → broke the worldgen path; setBlockEntity
			// on ChunkAccess works for a still-generating chunk too (BE is promoted by the engine when ProtoChunk→LevelChunk finalizes).
			ChunkAccess tChunk = aWorld.getChunk(aX >> 4, aZ >> 4);
			if (tChunk != null) {
				tChunk.setBlockEntity(aTileEntity); // used to be tChunk.func_150812_a(x&15,y,z&15,te)/addAndRegisterBlockEntity (LevelChunk-only) — neo: ChunkAccess.setBlockEntity(BlockEntity), position from te.getBlockPos()
				tChunk.setUnsaved(true); // was tChunk.setUnsaved(true)
				// F6-worldgen CROSS-CHUNK BE PERSISTENCE (CENTER): worldgen places MTE into NEIGHBORING chunks of the region too; in the neo
				// Feature.place model a BE write into an already-finalized neighbor LevelChunk does NOT persist (only the center ProtoChunk persists) →
				// srvBE=null permanently (client: sources/stones/redstonelight/foliage are transparent). WD.te is the ONLY point
				// that binds an MTE BE in worldgen (placeBlock AND direct paths) → register HERE for re-attachment once the
				// MTE's own chunk finalizes into a LevelChunk (server-tick sweep / ChunkEvent.Load). Worldgen only (not Level), MTE only (not ores).
				if (!(aWorld instanceof Level) && aTileEntity instanceof gregapi.block.multitileentity.IMultiTileEntity) gregapi.worldgen.GT6WorldgenFeature.recordWorldgenMTE(aTileEntity);
			}
		}
		return aTileEntity;
	}
	
	
	public static boolean oxygen(Level aWorld, int aX, int aY, int aZ) {
		return  !MD.GC.mLoaded || !dimGC(aWorld) || OxygenUtil.checkTorchHasOxygen(aWorld, NB, aX, aY, aZ); // F10: aWorld.provider instanceof IGalacticraftWorldProvider -> the dimGC center (WorldProvider removed).
	}
	public static boolean collectable_air(LevelAccessor aWorld, int aX, int aY, int aZ) {
		return (!MD.GC.mLoaded || !dimGC(aWorld)) && !hasCollide(aWorld, aX, aY, aZ) && !liquid(aWorld, aX, aY, aZ); // F10: aWorld.provider instanceof IGalacticraftWorldProvider -> the dimGC center.
	}
	
	/** @return the regular Environment Temperature of the World at this Location according to my calculations. In Kelvin, ofcourse. */
	public static long envTemp(LevelAccessor aWorld, int aX, int aY, int aZ) {
		// used to be aWorld.getBiomeGenForCoords(x,z) (2D) — neo: LevelReader.getBiome(BlockPos) (LevelReader.java:42),
		// returns Holder<Biome>; .value() (Holder.java:17) unwraps it to Biome (the envTemp(Biome,...) signature does not change).
		return envTemp(aWorld.getBiome(new BlockPos(aX, aY, aZ)).value(), aX, aY, aZ);
	}
	/** @return the regular Environment Temperature of the World at this Location according to my calculations. In Kelvin, ofcourse. */
	public static long envTemp(Biome aBiome, int aX, int aY, int aZ) {
		// used to be aBiome.getFloatTemperature(x,y,z) (position-adjusted, removed) -> getBaseTemperature() (Biome.java:247).
		// F6 functional-adapted: elevation cooling (climateSettings.temperatureModifier is private) is not reproduced — the base getBaseTemperature dominates, a parity detail.
		return Math.max(1, aBiome == null ? DEF_ENV_TEMP : (long)(C - 3 + aBiome.getBaseTemperature() * 20));
	}
	/** @return the regular Environment Temperature of the World at this Location according to my calculations. In Kelvin, ofcourse. */
	public static long envTemp(Biome aBiome) {
		return Math.max(1, aBiome == null ? DEF_ENV_TEMP : (long)(C - 3 + aBiome.getBaseTemperature() * 20));
	}
	// F6 center for biome/climate/light/precipitation (used to be World.getBiomeGenForCoords/getLightBrightness/getPrecipitationHeight + Biome.rainfall/temperature fields — removed):
	/** used to be World.getBiomeGenForCoords(x,z) (2D, BiomeGenBase) -> Level.getBiome(BlockPos).value() (LevelReader:42, Holder.value()); the 2D form takes Y=getSeaLevel() (LevelReader:66) as the surface column. */
	public static Biome biome(LevelAccessor aWorld, int aX, int aZ) {return aWorld == null ? null : aWorld.getBiome(new BlockPos(aX, aWorld.getSeaLevel(), aZ)).value();}
	public static Biome biome(LevelAccessor aWorld, int aX, int aY, int aZ) {return aWorld == null ? null : aWorld.getBiome(new BlockPos(aX, aY, aZ)).value();}
	/** used to be Biome.rainfall (a field, removed) -> Biome.getModifiedClimateSettings().downfall() (Biome.java:367 record ClimateSettings.downfall, :458 getModifiedClimateSettings). */
	public static float rainfall(Biome aBiome) {return aBiome == null ? 0 : aBiome.getModifiedClimateSettings().downfall();}
	/** used to be World.getLightBrightness(x,y,z) (float 0..1) -> LevelLightEngine.getRawBrightness(pos,0)/15 (LevelLightEngine.java:146, Level.getLightEngine() :375). */
	public static float lightBrightness(LevelAccessor aWorld, int aX, int aY, int aZ) {return aWorld == null ? 0 : aWorld.getLightEngine().getRawBrightness(new BlockPos(aX, aY, aZ), 0) / 15.0F;}
	/** used to be World.getPrecipitationHeight(x,z) -> the chunk's height map. NO-LOAD (edit #2): Level.getHeight
	 *  used to go through the loading getChunk(FULL,true) (a ticket on every call); 1.7.10 gave 0 on an unloaded chunk (EmptyChunk). */
	public static int precipitationHeight(LevelAccessor aWorld, int aX, int aZ) {
		if (!(aWorld instanceof Level tL)) return 0;
		net.minecraft.world.level.chunk.LevelChunk tChunk = chunkNow(tL, aX >> 4, aZ >> 4);
		return tChunk == null ? 0 : tChunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, aX & 15, aZ & 15) + 1; // +1: the chunk's getHeight gives back the Y of the top block, Level.getHeight — the first free one
	}
	/** used to be Block.dropBlockAsItem(world,x,y,z,meta,fortune) (block loot, removed) -> Block.dropResources(state,level,pos) (Block.java:380).
	 *  F6/F13 functional-adapted: the fortune parameter is unneeded (all callers pass fortune=0; dropResources is the default loot). */
	public static void dropBlockAsItem(LevelAccessor aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) {if (aWorld == null) return; BlockPos tPos = new BlockPos(aX, aY, aZ); Block.dropResources(state(aWorld, tPos), aWorld, tPos, te(aWorld, tPos, F));} // the 4-arg dropResources(BlockState,LevelAccessor,BlockPos,BlockEntity) (Block.java:389) — takes LevelAccessor (the 3-arg one took Level)
	/** used to be Block.dropBlockAsItem(world,x,y,z,ItemStack) (a specific stack) -> Block.popResource(level,pos,stack) (Block.java:407, receiver Level — spawns an ItemEntity, gameplay). */
	public static void dropBlockAsItem(Level aWorld, int aX, int aY, int aZ, ItemStack aStack) {if (aWorld != null && ST.valid(aStack)) Block.popResource(aWorld, new BlockPos(aX, aY, aZ), aStack);}
	/** used to be Block.getCollisionBoundingBoxFromPool(w,x,y,z) (a world-space AABB or null) -> getCollisionShape(w,pos).bounds().move(x,y,z)
	 *  (VoxelShape.bounds:39/isEmpty:73, AABB.move:220); an empty shape -> null (1:1 with 1.7.10's "no collision"). */
	public static AABB collisionBox(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {if (aWorld == null) return null; BlockPos tPos = new BlockPos(aX, aY, aZ); net.minecraft.world.phys.shapes.VoxelShape tShape = state(aWorld, tPos).getCollisionShape(aWorld, tPos); return tShape.isEmpty() ? null : tShape.bounds().move(aX, aY, aZ);}
	/** N-9: used to be World.checkNoEntityCollision(AABB,Entity) — 1.7.10 did NOT filter "is there any entity at all", but "is there
	 *  an entity with !isDead && preventEntitySpawning && entity!=except" (recompSrc/net/minecraft/world/World.java:2379-2394).
	 *  The previous edition lost this (`getEntities(...).isEmpty()` — blocks ANY entity, including lying loot).
	 *  Restored 1:1 through the forge analog of the same filter — EntityGetter.isUnobstructed (EntityGetter.java:35-40)
	 *  checks entity.blocksBuilding && !entity.isRemoved(); getEntities(except,bb) already returns only ACTUALLY
	 *  intersecting entities (EntitySection.getEntities:32 — entity.getBoundingBox().intersects(bb)), so a separate shape check
	 *  is not needed. ⚠ EntityItem (a dropped item) sets preventEntitySpawning/blocksBuilding NOWHERE in the tree —
	 *  neither in 1.7.10 nor in forge-1.20.1 — building a block over loot was, and should be, possible; only real
	 *  obstacles block (living entities, boats/minecarts, a falling block, TNT, an end crystal, a marker-less armor stand, a strider).
	 *  a null box -> true (no collision). */
	public static boolean noEntityCollision(LevelAccessor aWorld, AABB aBox) {if (aWorld == null || aBox == null) return T; for (Entity tEntity : aWorld.getEntities((Entity)null, aBox)) if (!tEntity.isRemoved() && tEntity.blocksBuilding) return F; return T;}
	public static boolean noEntityCollision(LevelAccessor aWorld, AABB aBox, Entity aExcept) {if (aWorld == null || aBox == null) return T; for (Entity tEntity : aWorld.getEntities(aExcept, aBox)) if (!tEntity.isRemoved() && tEntity.blocksBuilding) return F; return T;}
	
	// F6: used to have `WorldProvider aProvider` overloads IN PARALLEL with `Level aWorld` overloads (called through
	// `aWorld.provider`) — the same issue as the `dimXXX` family above: `WorldProvider` is removed in neo, the compiler
	// could not choose between `waterLevel(Level)`/`waterLevel(WorldProvider)` (ambiguous). Merged into one entry
	// `waterLevel(Level, int)`; `dimensionId == DIM_OVERWORLD` -> `Level.dimension() == Level.OVERWORLD`,
	// `hasNoSky` -> `!dimensionType().hasSkyLight()` (see `decisions/README.md` "Dimension-identity").
	/** @return the Height of the Water Level that should probably be in this World. */
	public static int waterLevel(LevelAccessor aWorld) {
		return waterLevel(aWorld, 62);
	}
	/** @return the Height of the Water Level that should probably be in this World. */
	public static int waterLevel(LevelAccessor aWorld, int aDefaultOverworld) {
		return dimKey(aWorld) == Level.OVERWORLD ? waterLevel(aDefaultOverworld) : !aWorld.dimensionType().hasSkyLight() || dimTF(aWorld) ? 31 : 62;
	}
	/** @return the Height of the Water Level that should probably be in the Overworld. */
	public static int waterLevel(int aDefaultOverworld) {
		return MD.TFC.mLoaded || MD.TFCP.mLoaded? 143 : aDefaultOverworld;
	}
	/** @return the Height of the Water Level that should probably be in the Overworld. */
	public static int waterLevel() {
		return waterLevel(62);
	}

	// ===== F6-Y-scale CENTER (§4.1 decisions/F6-worldgen.md): a single height adapter for worldgen (principle 2/4). =====
	// The 1.7.10 world = [0..255], bedrock Y=0, sea 62 (SEA_old, the waterLevel default). The MC26 world = [getMinY()..getMaxY()]
	// (usually −64..319), bedrock at getMinY(), sea at getSeaLevel() (63). GT6 generators are hard-wired to the old
	// absolute Y (0/255/getHeight()) → without adaptation they place things in the wrong spot (ores/water/bedrock/surface off). All
	// worldgen Y values pass through THIS center, not copy-pasted across files — the section-index reference is already in WorldgenStoneLayers.
	private static final int OLD_BOTTOM = 0, OLD_TOP = 255, SEA_OLD = 62;
	/** Lower world bound (worldgen: replaces the hard 0 / bedrock anchor). */
	public static int minY(net.minecraft.world.level.LevelHeightAccessor aWorld) {return aWorld.getMinBuildHeight();}
	/** Upper world bound INCLUSIVE (worldgen: replaces the hard 255). */
	public static int maxY(net.minecraft.world.level.LevelHeightAccessor aWorld) {return aWorld.getMaxBuildHeight()-1;}
	/** maxY+1 = the old {@code World.getHeight()} semantics (was 256). 1.20.1 getMaxBuildHeight() is EXCLUSIVE — that is exactly maxY+1. */
	public static int topY(net.minecraft.world.level.LevelHeightAccessor aWorld) {return aWorld.getMaxBuildHeight();}
	/** A dimension whose bounds match the old 1.7.10 world [0..255] → NOTHING to stretch (identity remap).
	 *  Nether and End in MC26 are exactly like that: {@code min_y=0, height=256} (`neo-decompiled/…/data/worldgen/DimensionTypes.java:72-73,105-106`),
	 *  and only the Overworld grew (−64/384). Without this gate the remap would pull windows to a FOREIGN dimension's sea level
	 *  (nether: {@code sea_level=32}, `NoiseGeneratorSettings.java:110`) and SHRINK the underground part instead of stretching. */
	private static boolean sameAsOldWorld(net.minecraft.world.level.LevelHeightAccessor aWorld) {
		return minY(aWorld) == OLD_BOTTOM && maxY(aWorld) == OLD_TOP;
	}
	/** §4.1 sea-anchored: old absolute Y (world [0..255], sea 62) → new Y (world [minY..maxY], sea getSeaLevel),
	 *  separately for the underground [0..62]→[minY..sea] and above-ground [62..255]→[sea..maxY] parts (sea is the anchor, not the floor). */
	public static int remapY(LevelAccessor aWorld, int aOldY) {
		if (sameAsOldWorld(aWorld)) return aOldY;
		int tSea = aWorld.getSeaLevel(), tMinY = minY(aWorld), tMaxY = maxY(aWorld);
		if (aOldY <= SEA_OLD) return tMinY + Math.round((aOldY - OLD_BOTTOM) * (tSea - tMinY) / (float)(SEA_OLD - OLD_BOTTOM));
		return tSea + Math.round((aOldY - SEA_OLD) * (tMaxY - tSea) / (float)(OLD_TOP - SEA_OLD));
	}
	/** §4.1 item 3, user instruction 2026-08-07: the window stretches — ORE DENSITY is preserved, which means the
	 *  COUNT grows proportionally to the enlarged volume. This is the growth coefficient for the window [aOldMinY..aOldMaxY]:
	 *  new thickness / old (1.0 where the world did not grow). One multiplier for the whole mod: generators do not compute
	 *  the stretch themselves, they ask the center — just like {@link #remapY} itself. */
	public static float yStretch(LevelAccessor aWorld, int aOldMinY, int aOldMaxY) {
		if (sameAsOldWorld(aWorld)) return 1.0F;
		int tOldSpan = Math.max(1, aOldMaxY - aOldMinY), tNewSpan = Math.max(1, remapY(aWorld, aOldMaxY) - remapY(aWorld, aOldMinY));
		return Math.max(1.0F, tNewSpan / (float)tOldSpan);
	}
	/** Integer object count for the stretched window. The fractional remainder is decided RANDOMLY, not dropped:
	 *  at a coefficient of 2.05 and a piece-count ore (2 pcs/chunk), rounding down would lose 5% of generation, rounding up would add 45%. */
	public static int yScaleAmount(LevelAccessor aWorld, int aOldMinY, int aOldMaxY, int aAmount, Random aRandom) {
		float tScaled = aAmount * yStretch(aWorld, aOldMinY, aOldMaxY);
		int rAmount = (int)tScaled;
		if (aRandom != null && aRandom.nextFloat() < tScaled - rAmount) rAmount++;
		return rAmount;
	}
	/** F-tileentity-construction Y-threshold (ADR): 1.7.10 invalidated a TE at Y<0 (world [0..255], Y<0 = anomaly-bug).
	 *  The MC26 world [minY..maxY] (usually −64..319) has Y<0 LEGITIMATE (bedrock at minY=−64, bedrock ores/fluid sources are there too)
	 *  → invalidate ONLY below the world floor getMinY(). The single center for the TE-invalidation Y-threshold for the whole mod.
	 *  CRITICAL (spam + ore removal): WITHOUT a level (getLevel()==null during chunk-load loadStatic→readFromNBT, before attach to the world)
	 *  do NOT invalidate — the previous fallback threshold of 0 falsely caught ALL underground ores (Y<0, legitimate) → setRemoved() removed their
	 *  BE (ore material was lost → a grey speck) + printed a Throwable stack trace for each one (spam ×tens of thousands). The position at
	 *  load time is legitimate (saved by the engine), and minY cannot be known without a level → safe to skip (checked once a level is present). */
	public static boolean tileYInvalid(LevelAccessor aLevel, int aY) {
		return aLevel != null && aY < minY(aLevel);
	}

	/** @return the regular Temperature of the World at this Location according to Gregs calculations. In Kelvin, ofcourse. */
	public static long temperature(LevelAccessor aWorld, int aX, int aY, int aZ) {
		long rTemperature = envTemp(aWorld, aX, aY, aZ);
		if (burning(aWorld, aX, aY, aZ)) rTemperature = Math.max(rTemperature, C + 200);
		for (BlockPos tCoords : new BlockPos[] {new BlockPos(aX, aY, aZ), new BlockPos(aX+1, aY, aZ), new BlockPos(aX-1, aY, aZ), new BlockPos(aX, aY+1, aZ), new BlockPos(aX, aY-1, aZ), new BlockPos(aX, aY, aZ+1), new BlockPos(aX, aY, aZ-1)}) {
			Block tBlock = block(aWorld, tCoords.getX(), tCoords.getY(), tCoords.getZ(), F);
			if (tBlock == Blocks.LAVA || tBlock == Blocks.LAVA) rTemperature = Math.max(rTemperature, C + 500);
			else if (tBlock instanceof FireBlock) rTemperature = Math.max(rTemperature, C + 200);
		}
		return rTemperature;
	}
	
	public static ItemStack stack(LevelAccessor aWorld, int aX, int aY, int aZ) {
		Block tBlock = state(aWorld, new BlockPos(aX, aY, aZ)).getBlock(); // used to be aWorld.getBlock(x,y,z)
		// used to be aWorld.getBlockMetadata(x,y,z) in the else branch — neo no longer has numeric meta (META MODEL item 4):
		// for vanilla blocks (not IBlockExtendedMetaData) return 0, without inventing a numeric table.
		return ST.make(tBlock, 1, tBlock instanceof IBlockExtendedMetaData ? ((IBlockExtendedMetaData)tBlock).getExtendedMetaData(aWorld, aX, aY, aZ) : 0);
	}

	public static void update(BlockGetter aWorld, int aX, int aY, int aZ) {
		// used to be ((Level)aWorld).markBlockForUpdate(x,y,z) — neo: Level.sendBlockUpdated(pos,old,new,flags)
		// (Level.java:333); the old/new state was not tracked separately, the same approach is already applied in
		// GT_API_Proxy.java:1316 (getBlockState twice, flags=3=UPDATE_ALL).
		BlockPos tUpdPos = new BlockPos(aX, aY, aZ);
		BlockState tUpdState = state(aWorld, tUpdPos);
		((Level)aWorld).sendBlockUpdated(tUpdPos, tUpdState, tUpdState, 3);
		// THE MTE GEOMETRY CACHE IS RESET HERE, AT THE SOURCE OF THE CHANGE. Previously the only reset was
		// the vanilla LevelRenderer.setSectionDirty signal (intercepted by MixinLevelRenderer): while the renderer
		// is vanilla, the signal arrives — but a mod that replaces it (render optimizers and the like)
		// takes the signal away with it, and the quad cache is NEVER reset: the block stays drawn as
		// it was assembled the first time (place an empty anvil — and an ingot on it will never appear).
		// EVERY client message about an MTE change (receiveData* dispatchers, MultiTileEntityBlock) passes
		// through WD.update, so the invalidation here is correct by construction and depends on nothing else.
		// Point invalidation of a single instance — alongside the O(1) section-stamp scheme (MultiTileEntityBER),
		// not instead of it: mQuadCacheEpoch=MIN_VALUE never matches the current epoch, so the cache hit
		// won't go through regardless of its own section's stamp.
		if (((Level)aWorld).isClientSide()) {
			BlockEntity tUpdTE = ((Level)aWorld).getBlockEntity(tUpdPos);
			if (tUpdTE instanceof gregapi.tileentity.base.TileEntityBase01Root tUpdRoot) tUpdRoot.mQuadCacheEpoch = Long.MIN_VALUE;
		}
		if (CLIENT_BLOCKUPDATE_SOUNDS && CODE_CLIENT && CLIENT_TIME > 100) {
			Player tPlayer = GT_API.api_proxy.getThePlayer();
			if (tPlayer != null && Math.abs(tPlayer.getX() - aX) < 16 && Math.abs(tPlayer.getY() - aY) < 16 && Math.abs(tPlayer.getZ() - aZ) < 16) {
				UT.Sounds.play(SFX.MC_FIREWORK_LAUNCH, 1, 1.0F, 1.0F, aX, aY, aZ);
			}
		}
	}
	
	// used to be aWorld.getBlock(x,y,z) — neo: BlockGetter.getBlockState(BlockPos).getBlock() (BlockGetter.java:32); used to be
	// WD.exists(aWorld, x, y, z) — Level.isLoaded(BlockPos) (Level.java:695).
	public static Block block(BlockGetter aWorld, int aX, int aY, int aZ) {return state(aWorld, new BlockPos(aX, aY, aZ)).getBlock();}
	public static Block block(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {BlockPos tP = new BlockPos(aX, aY, aZ); return aLoadUnloadedChunks || exists(aWorld, aX, aY, aZ) ? state(aWorld, tP).getBlock() : NB;}
	public static Block block(LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {return block(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide], aLoadUnloadedChunks);}
	public static Block block(LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide) {return block(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]);}
	// META MODEL item 4: neo no longer has numeric meta — for IBlockExtendedMetaData (own blocks, item 1) the real
	// value, otherwise 0 (we do not invent a numeric table for vanilla blocks).
	public static byte  meta (BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockState tState = state(aWorld, new BlockPos(aX, aY, aZ)); Block tB = tState.getBlock();
		// BUG-025: the 1.7.10 cauldron (one block, meta 0-3 = water level) was split by the engine (1.13+) into CAULDRON(empty, no
		// level property) / WATER_CAULDRON(LayeredCauldronBlock, LEVEL 1-3) — read the level from the split block so
		// GT6 code (pipe) sees the cauldron meta as in 1.7.10 (getBlockMetadata gave 0-3). Empty = 0 (below, default).
		if (tB == Blocks.WATER_CAULDRON) return UT.Code.bind4(tState.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL));
		// F4-flatten, the read side of the write bridge (legacyVanillaState): for split families (wool/carpet/
		// glass/panes/terracotta/tallgrass) the subtype is now expressed by the BLOCK ITSELF, while 1.7.10 code asks for it via meta.
		// Without this read the bridge is one-way: write red glass — read back 0 ("white"), and checks like
		// "is the block already the wanted color" (Behavior_Spray_Color.colorize:167) are always false. The maps are the same center.
		int tFlat = gregapi.data.CS.Flattened.metaOf(tB);
		if (tFlat >= 0) return UT.Code.bind4(tFlat);
		if (tB instanceof IBlockExtendedMetaData) return UT.Code.bind4(((IBlockExtendedMetaData)tB).getExtendedMetaData(aWorld, aX, aY, aZ));
		return UT.Code.bind4(vanillaFluidLevel(tState));
	}
	/** VANILLA FLUID LEVEL (water/lava). In 1.7.10 {@code getBlockMetadata} on water returned the level
	 *  (0 — source, 1-7 — decreasing flow, 8 — falling), and all of GT6's 1.7.10 logic stands on this: counting
	 *  source neighbors ({@code BlockSwamp:106-108}, {@code BlockOcean:113,119,129}, {@code BlockRiver:94}),
	 *  spread quanta ({@code BlockWaterlike.getQuantaValue:227}), "only a source can be scooped"
	 *  ({@code Behavior_Bucket_Container:55,62}, {@code TileEntityBase08FluidContainer:303,316}), lily-pad support
	 *  ({@code BlockBaseLilyPad:86,123}). The carrier of the same number in neo is {@code LiquidBlock.LEVEL}
	 *  (LiquidBlock.java:125-128: {@code stateCache.get(min(level,8))}), semantics match 1:1.
	 *  <p>Without this branch any water read as "meta 0 = source": a swamp FLOW next to a stream became
	 *  a SOURCE ({@code BlockSwamp:161}), the source never self-removed ({@code BlockSwamp:155}), and water
	 *  quanta were always full (8) — the swamp front never faded and spread beyond the biome.
	 *  <p>GT6's own fluids do not hit this: they are {@link IBlockExtendedMetaData} (branch above), their
	 *  quanta live in {@code FLUID_META}, and the {@code LEVEL} inherited from {@code LiquidBlock} is always 0
	 *  (BlockFluidBaseGT.java:152-154). The same approach already applied to the split cauldron above. */
	private static int vanillaFluidLevel(BlockState aState) {
		return aState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock ? aState.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) : 0;
	}
	/** F13 contract: meta from a SNAPSHOT BlockState (BlockDropsEvent.getState() / mineBlock aState). In neo removeBlock
	 *  happens BEFORE drops and Item.mineBlock (in 1.7.10 — AFTER), so meta(aWorld,x,y,z) on harvest paths already
	 *  reads AIR → meta 0 → stale hammer recipes (BUG-016) and dig speeds. Through the snapshot channel the 1.7.10
	 *  contract "meta of the block being destroyed" is restored. BUG-047: laying out meta from properties — knowledge of the BLOCK
	 *  (META for meta-families, SHAPE+POWERED for rail) → delegated to IBlockExtendedMetaData.getExtendedMetaData(BlockState)
	 *  (interface default = the previous META hardcode, family behavior 1:1). */
	public static byte  meta (net.minecraft.world.level.block.state.BlockState aState) {
		if (!(aState.getBlock() instanceof IBlockExtendedMetaData)) return UT.Code.bind4(vanillaFluidLevel(aState)); // vanilla fluid level — the same channel as in the positional overload above
		return UT.Code.bind4(((IBlockExtendedMetaData)aState.getBlock()).getExtendedMetaData(aState));
	}
	public static byte  meta (LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {return aLoadUnloadedChunks || exists(aWorld, aX, aY, aZ) ? meta((BlockGetter)aWorld, aX, aY, aZ) : 0;}
	public static byte  meta (LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {return meta(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide], aLoadUnloadedChunks);}
	public static byte  meta (LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide) {return meta(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]);}
	public static byte  meta (long aBitAnd, BlockGetter aWorld, int aX, int aY, int aZ) {return UT.Code.bind4(meta(aWorld, aX, aY, aZ) & aBitAnd);}
	public static byte  meta (long aBitAnd, LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {return aLoadUnloadedChunks || exists(aWorld, aX, aY, aZ) ? UT.Code.bind4(meta((BlockGetter)aWorld, aX, aY, aZ) & aBitAnd) : 0;}
	public static byte  meta (long aBitAnd, LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {return meta(aBitAnd, aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide], aLoadUnloadedChunks);}
	public static byte  meta (long aBitAnd, LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide) {return meta(aBitAnd, aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]);}
	
	public static boolean set(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta, long aFlags) {
		return set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags, WD.opaque(aBlock));
	}

	// F-tool-rotation CENTER: 1.7.10 Block.rotateBlock(World,x,y,z,ForgeDirection axis) was removed from neo (rotation is
	// BlockState.rotate(Rotation), Y-axis only). Read the block state at the position (callers place the block via WD.set
	// one line above, then rotate) and rotate(CLOCKWISE_90): state.rotate respects each block's own rotate behavior
	// (directional blocks rotate, non-directional ones return themselves — more accurate than the previous Block.rotateBlock default=no-op).
	// aAxis: neo Rotation is Y-only (an enum without horizontal axes); the only callers (worldgen dungeon) rotate
	// around SIDE_Y_POS(UP) -> a Y rotation; a non-Y axis does not occur in the current slice (should one appear — a separate ADR F-tool-rotation).
	public static boolean rotateBlock(LevelAccessor aWorld, int aX, int aY, int aZ, Direction aAxis) {
		if (aWorld == null) return F;
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockState tState = state(aWorld, tPos);
		BlockState tRotated = tState.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
		return tRotated != tState && aWorld.setBlock(tPos, tRotated, 3);
	}

	// F-hook-removed → CENTER (principle 4: the capability exists under another name). 1.7.10 Forge World.canPlaceEntityOnSide
	// (block,x,y,z,skipColl,side,entity,stack) — the NAME was removed, but the capability exists: neo CollisionGetter.isUnobstructed /
	// shape collision. We reproduce the original semantics 1:1 (net/minecraft/world/World.java:3647-3649): (1) shape collision
	// of the block BEING PLACED against entities, except the placing one (skipColl -> no check) — through the already existing
	// center noEntityCollision(box, entity) = 1:1 with 1.7.10 checkNoEntityCollision(aabb, entity); (2) replaceability of the target —
	// neo BlockState.canBeReplaced() (1.7.10 block1.isReplaceable); (3) "can the block BEING PLACED actually stand here" —
	// neo BlockState.canSurvive (1.7.10 aBlock.canReplace -> canPlaceBlockOnSide -> canPlaceBlockAt). The "anvil on
	// circuits" branch is omitted: unreachable for GT6 blocks (aBlock is always a GT6 block, never Blocks.ANVIL).
	//
	// ⛔ BP-BUG-016 (player report "rail places infinitely, the extras drop straight into loot"). The previous edition
	// OMITTED item (3) with the justification "block.canReplace for a GT6 block = T (BlockBase.canReplace)". That
	// justification is wrong: in 1.7.10 GT6 canReplace did NOT override it at all (grep of gt6-original: only CALLERS,
	// BlockBase.java:162 and MultiTileEntityItemInternal.java:169) — it resolved to vanilla
	// Block.canReplace (recompSrc net/minecraft/block/Block.java:1021-1023) -> canPlaceBlockOnSide (:1038-1041) ->
	// canPlaceBlockAt (:1046-1049), and it was EXACTLY this branch that rails (BlockRailBase: solid block below),
	// lily pads (BlockBaseLilyPad:73), saplings (BlockBaseSapling:133) and flowers overrode. I.e. the port's
	// `canReplace=T` is a new, empty detail, not a copy; the check itself was lost, and the block would place
	// anywhere, only to crumble into loot on the very same tick. neo's canSurvive default is true, 1.7.10's
	// canPlaceBlockAt default = isReplaceable(target) (already covered by item (2)) — so item (3) returns EXACTLY
	// the overrides, nothing extra.
	public static boolean canPlaceEntityOnSide(LevelAccessor aWorld, Block aBlock, int aX, int aY, int aZ, boolean aSkipCollisionCheck, int aSide, Entity aEntity, ItemStack aStack) {
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		if (!aSkipCollisionCheck) {
			net.minecraft.world.phys.shapes.VoxelShape tShape = aBlock.defaultBlockState().getCollisionShape(aWorld, tPos);
			if (!tShape.isEmpty() && !noEntityCollision(aWorld, tShape.bounds().move(aX, aY, aZ), aEntity)) return F;
		}
		if (!state(aWorld, tPos).canBeReplaced()) return F;
		return aBlock.defaultBlockState().canSurvive(aWorld, tPos);
	}

	/** CENTER for the explosion-drop gate (BUG-024; consolidation of the BUG-047 revision — there used to be copies in BlockBase/PrefixBlock/
	 *  BlockBaseRail): a vanilla explosion drops through the loot channel with EXPLOSION_RADIUS — the 1.7.10 explosion drop chance
	 *  = 1/size (Explosion.doExplosionA), without this gate GT6 blocks would drop from TNT at 100%. true = suppress the drop. */
	public static boolean explosionDropDenied(net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		Float tExplosionRadius = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.EXPLOSION_RADIUS);
		return tExplosionRadius != null && RNGSUS.nextFloat() >= 1.0F / tExplosionRadius;
	}

	/** CENTER for the waterlog handling (BUG-010 slabs / BUG-047 rails; semantics of vanilla getStateForPlacement for
	 *  waterloggable blocks): placing a block INTO water — the source is preserved as WATERLOGGED=true. Call AFTER
	 *  placing the block; the caller reads the "was there water" decision BEFORE that (the set itself overwrites the water). */
	public static boolean waterlog(LevelAccessor aWorld, int aX, int aY, int aZ) {
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockState tState = state(aWorld, tPos);
		if (!tState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) return F;
		return aWorld.setBlock(tPos, tState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, Boolean.TRUE), 3);
	}


	/** The SINGLE point that writes a block into the world from the {@link #set} center. Purpose — remove the engine's
	 *  block-entity STUB that the 1.20.1 engine drops into the chunk on EVERY worldgen write of a block with a {@code BlockEntity}.
	 *
	 *  <p><b>What the engine does.</b> {@code WorldGenRegion.setBlock} [WorldGenRegion.java:267-282]: if the new
	 *  block carries an entity and the chunk is not yet full, the engine puts its own entry {@code {id:"DUMMY"}}
	 *  into {@code pendingBlockEntities} — a promise "I'll create the real entity later, on promotion". That promise
	 *  is removed by exactly two means: {@code ChunkAccess.removeBlockEntity} (the engine itself calls it on the sibling
	 *  branch [WorldGenRegion.java:283-284], when the new block carries no entity) and the general sweep
	 *  {@code LevelChunk.postProcessGeneration} [LevelChunk.java:514-518], which runs ONLY once the chunk becomes
	 *  TICKING [ChunkMap.java:747] — noticeably later than promotion.
	 *
	 *  <p><b>Why the promise becomes an orphan.</b> The normal block-removal path does NOT touch the promise:
	 *  {@code BlockBehaviour.onRemove} [BlockBehaviour.java:163-166] goes into
	 *  {@code LevelChunk.removeBlockEntity} [LevelChunk.java:394-403], which cleans ONLY the map of LIVE
	 *  entities. If a worldgen block disappears inside the "promotion ... post-processing" window, the promise
	 *  stays hanging. And it disappears there NORMALLY AND BY CANON: a pebble removes itself when there's fluid
	 *  next to it or its support is gone — {@code MultiTileEntityRock.onNeighborBlockChange} (body 1:1 with the
	 *  1.7.10 original {@code gt6-original/.../placeables/MultiTileEntityRock.java:151-163}), and neighbor updates
	 *  arrive exactly when the chunk starts ticking. The result — engine WARN
	 *  "Tried to load a DUMMY block entity ... but found not block entity block" and a ghost entry on disk
	 *  (BP-BUG-009: measured — 12/16/12 warnings per ~31.9k fresh chunks).
	 *
	 *  <p><b>Why fixed here, not at the carrier.</b> There are as many carriers of this class as there are worldgen
	 *  blocks with an entity (~150 per chunk: ores, pebbles, sticks, springs), and the pebble's behavior is CANON — it
	 *  must not be changed. There is exactly one shared detail — this: the block-write center.
	 *
	 *  <p><b>REMOVAL BOUNDARY — MOD BLOCKS, AND ONLY THOSE (BUG-139).</b> "The mod does not need the promise" is true
	 *  exactly for OWN blocks: GT6 sets their entity itself ({@link #te}), just as in 1.7.10 — "Where I come from, we set the
	 *  TileEntities ourselves" ({@code PrefixBlock.createTileEntity}); stubs did not exist at all in 1.7.10,
	 *  and {@code Chunk.func_150807_a} removed the entity together with the block. For a FOREIGN block this is mirror-wrong:
	 *  the mod NEVER sets a vanilla entity, and the engine's promise is its ONLY source. The previous
	 *  edition removed the promise regardless of owner, and worldgen-placed vanilla block-entities in dungeons stayed
	 *  WITHOUT an entity: the ender chest and bed (their whole look is drawn by their BER), the enchanting table's book,
	 *  the End portal, signs, vanilla chests and the beacon — the block stands, but there's nothing to draw, until the
	 *  entity is created by a lazy {@code Level.getBlockEntity} request and the section is rebuilt. The "mod block"
	 *  predicate is taken from the mod's CENTER {@code ST.isGT(Block)} (the same one the mod uses to tell its own block
	 *  apart in {@code WorldgenOresVanilla:58}).
	 *
	 *  <p><b>The technique is engine-native, we start no mechanism of our own:</b> we remove the promise with the same
	 *  {@code ChunkAccess.removeBlockEntity} that {@code WorldGenRegion} itself uses. It cleans both maps [ProtoChunk.java:248-251],
	 *  so an already-attached live entity (the "block -> entity" order at {@code placeBlock} is doubled: set/te/set/te)
	 *  is put back in place through the same channel {@code ChunkAccess.setBlockEntity} that {@link #te} uses to set it.
	 *  {@code ProtoChunk.removeBlockEntity} is a clean map operation, it does not call {@code setRemoved()}.
	 *
	 *  <p><b>Symmetry with main (26.1.2): this fix is needed ONLY here.</b> On the 26.1.2 engine the promise is removed by
	 *  {@code ProtoChunk.setBlockEntity} itself [neo-decompiled/.../ProtoChunk.java:173-175:
	 *  {@code pendingBlockEntities.remove(pos); blockEntities.put(pos, be);}], i.e. attaching the real
	 *  entity extinguishes the stub automatically. On 1.20.1 there is no such method
	 *  [forge-1201-decompiled/.../ProtoChunk.java:148-150 — only {@code blockEntities.put}]. This fix
	 *  reproduces the later engine's behavior on this branch. */
	private static boolean setWG(LevelAccessor aWorld, BlockPos aPos, BlockState aState, int aFlags) {
		boolean rSet = aWorld.setBlock(aPos, aState, aFlags);
		// BUG-139: gate ST.isGT — the promise is removed ONLY for MOD blocks (their entity is set by the mod itself through WD.te).
		// For a foreign block the engine's promise is the sole source of the entity; removing it would leave the block without one.
		if (rSet && aState.hasBlockEntity() && ST.isGT(aState.getBlock())) dropWorldgenBEStub(aWorld, aPos);
		// Starting fluid tick: WorldGenRegion does not call onPlace, while in 1.7.10 the chunk itself called it, and the flowing water
		// of the dungeon mob farm (original DungeonChunkRoomFarmMobs:198-201) spread on its own. The technique is engine-native — the
		// engine's own structure generator schedules a tick the same way (StructurePiece.placeBlock).
		if (rSet && !(aWorld instanceof Level)) {
			net.minecraft.world.level.material.FluidState tFluid = aWorld.getFluidState(aPos);
			if (!tFluid.isEmpty()) aWorld.scheduleTick(aPos, tFluid.getType(), 0);
		}
		return rSet;
	}

	/** Removal of the engine's promise. The condition is exactly the one under which the engine writes it
	 *  ({@code WorldGenRegion.setBlock:268}: chunk type is NOT {@code LEVELCHUNK}), so on a live world and on an
	 *  already-full neighbor the method does nothing. */
	private static void dropWorldgenBEStub(LevelAccessor aWorld, BlockPos aPos) {
		if (aWorld instanceof Level) return; // not worldgen — no promises exist
		try {
			ChunkAccess tChunk = aWorld.getChunk(aPos.getX() >> 4, aPos.getZ() >> 4);
			if (tChunk == null || tChunk.getStatus().getChunkType() == net.minecraft.world.level.chunk.ChunkStatus.ChunkType.LEVELCHUNK) return;
			net.minecraft.nbt.CompoundTag tStub = tChunk.getBlockEntityNbt(aPos);
			if (tStub == null || !"DUMMY".equals(tStub.getString("id"))) return; // do not touch a foreign packed entry
			BlockEntity tLive = tChunk.getBlockEntity(aPos); // ProtoChunk: a clean map read (ProtoChunk.java:153-155), no promotion
			tChunk.removeBlockEntity(aPos);
			if (tLive != null) tChunk.setBlockEntity(tLive);
		} catch (Throwable e) {e.printStackTrace(ERR);}
	}

	public static boolean set(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta, long aFlags, boolean aRemoveGrassBelow) {
		// BUG-115 tail (report: "half-blocks of lava remain") — FORCE THE ENGINE on fluid REMOVAL.
		// In 1.7.10 the flow lived on its own tick: BlockDynamicLiquid.updateTick rescheduled itself while the block
		// existed, and dissolved on its own once the source disappeared — notifying neighbors was unnecessary, which is why
		// the whole mod removes fluid with flag 2 (e.g. MultiTileEntityPump.drainFluid, BlockWaterlike.drain).
		// In neo the recalculation is scheduled by LiquidBlock.updateShape, and ONLY when one of the sides is a SOURCE
		// (neo-decompiled/.../LiquidBlock.java:181). Remove a source without UPDATE_NEIGHBORS — a neighboring flow stands
		// next to air, there is no source on any side, no tick is scheduled, and the "half-block" hangs forever.
		// Differential measurement [GT6-PUMPPROBE]: two identical plots, 44 lava flows each; clearing the
		// source with flag 2 — 44 remained, with flag 3 — 0. Hence: removing fluid means we must wake the neighbors.
		// The condition is deliberately narrow: only REMOVAL (setting air) and only if the cell HAD fluid — worldgen
		// filling (WD.set(..., 0) in an ocean/river) is not affected, no extra updates during generation (ADAPT-009).
		// BP-BUG-003: the "the cell HAD fluid" predicate is taken the SAME way the mod recognizes fluid everywhere —
		// by block CLASS (liquid(Block) below), not through FluidState. The previous FluidState-based gate tied this
		// fix to the block's engine answer: the moment the role passport moved oils and gas to EMPTY, removing oil
		// would stop waking neighbors and the "half-blocks" would return — now for oils. One predicate — one
		// carrier (the same fix, done with the same technique, on main, WD.java:1443-1447).
		if ((aFlags & 1) == 0 && aBlock == NB && liquid(state(aWorld, new BlockPos(aX, aY, aZ)).getBlock())) aFlags |= 1;
		if (aRemoveGrassBelow) {
			Block tBlock = state(aWorld, new BlockPos(aX, aY-1, aZ)).getBlock(); // used to be aWorld.getBlock(x,y-1,z)
			if (tBlock == Blocks.GRASS_BLOCK || tBlock == Blocks.MYCELIUM) setWG(aWorld, new BlockPos(aX, aY-1, aZ), Blocks.DIRT.defaultBlockState(), (int)aFlags); // used to be aWorld.setBlock(x,y-1,z,Blocks.DIRT,0,flags)
		}
		// BUG-025: the engine (1.13+) split the 1.7.10 cauldron (one block, meta 0-3 = water level) into DIFFERENT registry blocks:
		// CAULDRON(empty, WITHOUT a level property) / WATER_CAULDRON(LayeredCauldronBlock, LEVEL 1-3). The universal bridge below
		// (setBlock defaultBlockState) did not express the level → setMetaData(3) placed an empty CAULDRON, losing the water (a cauldron fed by
		// a pipe+Drain did not fill). The centralized translation "numeric cauldron meta ↔ split block+LEVEL" is here, in the SINGLE
		// WD write center (1:1 semantics of the 1.7.10 setBlockMetadataWithNotify on a cauldron): 0→empty, 1-3→WATER_CAULDRON.LEVEL.
		if (aBlock == Blocks.CAULDRON || aBlock == Blocks.WATER_CAULDRON) {
			byte tLevel = Code.bind4(aMeta);
			BlockState tCauldron = tLevel <= 0
				? Blocks.CAULDRON.defaultBlockState()
				: Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, (int) Math.min(net.minecraft.world.level.block.LayeredCauldronBlock.MAX_FILL_LEVEL, tLevel));
			return setWG(aWorld, new BlockPos(aX, aY, aZ), tCauldron, (int) aFlags);
		}
		// F13-legacy-meta BRIDGE (dungeon pass #39, live test: "rotations are a pervasive problem"): GT6 worldgen places
		// vanilla directional blocks with LITERAL 1.7.10 metas, and the neo model has no meta — the previous path gave a
		// default state (pistons facing down, buttons in mid-air, doors/beds/frames without orientation). Breakdown below.
		{
			BlockState tLegacy = legacyVanillaState(aWorld, aX, aY, aZ, aBlock, aMeta);
			if (tLegacy != null) return setWG(aWorld, new BlockPos(aX, aY, aZ), tLegacy, (int)aFlags);
		}
		// used to be aWorld.setBlock(x,y,z,block,meta,flags) — neo: LevelWriter.setBlock(BlockPos,BlockState,flags) (LevelWriter.java:10).
		// BlockState has no numeric meta (META MODEL item 1/4). BUG-047: 1.7.10 Chunk.func_150807_a wrote block+meta in ONE
		// set (onBlockAdded saw the meta) — the atomic path restores the contract: a state-with-meta in one setBlock.
		// Base = the current state if it's the same block (a meta change does not touch other properties — WATERLOGGED etc., 1:1 with 1.7.10),
		// otherwise defaultBlockState. An equal state → setBlock itself returns false without mutation (gate Chunk.java:623-625, 1:1).
		// TE meta (PrefixBlock: getStateForExtendedMetaData=null) — the previous two-phase path. For vanilla, aMeta is lost (engine force).
		BlockPos tSetPos = new BlockPos(aX, aY, aZ);
		if (aBlock instanceof IBlockExtendedMetaData) {
			BlockState tCur = state(aWorld, tSetPos);
			BlockState tNew = ((IBlockExtendedMetaData)aBlock).getStateForExtendedMetaData(tCur.getBlock() == aBlock ? tCur : aBlock.defaultBlockState(), Code.bind4(aMeta));
			if (tNew != null) return setWG(aWorld, tSetPos, tNew, (int)aFlags);
		}
		boolean rSet = setWG(aWorld, tSetPos, aBlock.defaultBlockState(), (int)aFlags);
		if (aBlock instanceof IBlockExtendedMetaData) {
			byte tNewMeta = Code.bind4(aMeta);
			// meta is a separate channel; but the setter has side-effects (WD.te/WD.update), hence — only on a REAL difference
			// (the original Chunk.java:623-625 returned false without mutation when block AND meta matched):
			if (((IBlockExtendedMetaData)aBlock).getExtendedMetaData(aWorld, aX, aY, aZ) != tNewMeta) {
				((IBlockExtendedMetaData)aBlock).setExtendedMetaData(aWorld, aX, aY, aZ, tNewMeta);
				rSet = true;
			}
		}
		return rSet;
	}

	/** State channel of the write CENTER (dungeon pass #39): for cases where 1.7.10 expressed a state as a SEPARATE registry
	 *  block, and the 1.13+ engine broke it down into a blockstate property of the same block (lit_redstone_lamp → REDSTONE_LAMP[LIT];
	 *  the same class of split as the cauldron BUG-025 above). Such states have no numeric meta — cannot be expressed through the meta channel. */
	public static boolean set(LevelAccessor aWorld, int aX, int aY, int aZ, BlockState aState, long aFlags) {
		return setWG(aWorld, new BlockPos(aX, aY, aZ), aState, (int)aFlags);
	}

	// F13-legacy-meta BRIDGE (dungeon pass #39): 1.7.10 direction maps. Verified by the GEOMETRY of Greg's dungeon
	// constructs (cross-check, architecture/dungeons.md §8): torch meta 4 @(13,4,6) attaches exactly to smooth(13,4,7) —
	// support from the south, pointing north; barracks buttons with metas 3/4 sit on walls z=5/z=10; a bed "head" with meta 8+f
	// is strictly in the facing direction from the "foot"; all 8 End frames with meta 4-7 face into the 2×2 portal (and carry
	// an eye, bit4); door pistons with metas 2/3/4/5 face the door columns (1.7.10's SIDE order = Direction ordinals);
	// cocoa: COMPASS_FROM_SIDE (CS.java:685 — compass 0=N,1=E,2=S,3=W), meta = the pod's side FROM the trunk, while neo's
	// CocoaBlock.FACING points AT the support (canSurvive: pos.relative(FACING) — CocoaBlock.java:63-67) → opposite.
	private static final Direction[] DIR_1710_TORCH   = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}; // meta 1..4 (torches/buttons: support at the back)
	private static final Direction[] DIR_1710_HORIZ   = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST}; // meta&3 (beds/End frames/anvil)
	// Door: a closed neo door's panel sits at the edge OPPOSITE FACING (hinges at the back). Verified (live test
	// "door hangs with a gap"): barracks door meta 1 in the slab-wall opening z=5 (panel mSlabs[SIDE_Z_NEG] occupies
	// z∈[0,0.5]) — the panel must hug the north edge → FACING=SOUTH; the paired door meta 3 at wall
	// z=10 (panel z∈[0.5,1]) → panel south → FACING=NORTH. Full cycle: 0=E,1=S,2=W,3=N.
	private static final Direction[] DIR_1710_DOOR    = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH}; // meta&3 of the door's lower half
	private static final Direction[] DIR_1710_COMPASS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}; // compass (cocoa)

	/** F13-legacy-meta BRIDGE: 1.7.10 vanilla-block meta → BlockState (or a block substitution: wall torch, log variant,
	 *  double plants — in 1.7.10 variants lived in the meta of ONE block, the engine broke them into separate blocks).
	 *  null = the family is not legacy-bridged (the regular WD.set path). Mappings — see the comment on the maps above. */
	/** Base for legacy meta: the CURRENT state if the cell holds the same block, otherwise the default. A direction change
	 *  must not reset other properties — a lit furnace stays lit, a flooded chest stays flooded;
	 *  this is exactly the 1.7.10 semantics, where setBlockMetadataWithNotify changed the meta without recreating the block. */
	private static BlockState legacyBase(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {
		BlockState tCur = state(aWorld, new BlockPos(aX, aY, aZ));
		return tCur.getBlock() == aBlock ? tCur : aBlock.defaultBlockState();
	}

	private static BlockState legacyVanillaState(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta) {
		int tMeta = (int)aMeta;
		// Pistons: meta&7 = side (1.7.10 SIDE order = Direction ordinals: 0D,1U,2N,3S,4W,5E).
		if (aBlock == Blocks.PISTON || aBlock == Blocks.STICKY_PISTON)
			return aBlock.defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.from3DDataValue(tMeta & 7));
		// Directional VANILLA that Greg's wrench addresses DIRECTLY ("face the poked side",
		// ToolCompat sets the block's meta = aTargetSide). Without these lines their meta was lost, WD.set returned
		// false, and the wrench fell back to the WD.rotateBlock path — meaning it ROTATED BY 90° instead of
		// addressed rotation, losing Greg's semantics. The 1.7.10 meta order = Direction ordinals (0D,1U,2N,3S,4W,5E) —
		// same as the piston above.
		if (aBlock instanceof net.minecraft.world.level.block.FurnaceBlock || aBlock instanceof net.minecraft.world.level.block.ChestBlock
		 || aBlock instanceof net.minecraft.world.level.block.EnderChestBlock || aBlock instanceof net.minecraft.world.level.block.CarvedPumpkinBlock) {
			Direction tDirH = Direction.from3DDataValue(tMeta & 7);
			if (tDirH.getAxis().isHorizontal()) return legacyBase(aWorld, aX, aY, aZ, aBlock).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, tDirH);
		}
		// The hopper holds direction in ITS OWN property (HopperBlock FACING_HOPPER): UP is forbidden for it.
		if (aBlock instanceof net.minecraft.world.level.block.HopperBlock) {
			Direction tDirHop = Direction.from3DDataValue(tMeta & 7);
			if (tDirHop != Direction.UP) return legacyBase(aWorld, aX, aY, aZ, aBlock).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING_HOPPER, tDirHop);
		}
		// Dispenser and dropper (DropperBlock extends DispenserBlock) — all six sides, like the piston.
		if (aBlock instanceof net.minecraft.world.level.block.DispenserBlock)
			return legacyBase(aWorld, aX, aY, aZ, aBlock).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.from3DDataValue(tMeta & 7));
		// Redstone torch: metas 1-4 = WALL-mounted (in neo — a separate REDSTONE_WALL_TORCH block); 0/5 = standing (default).
		if (aBlock == Blocks.REDSTONE_TORCH) {
			int tSide = tMeta & 7;
			if (tSide >= 1 && tSide <= 4) return Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneWallTorchBlock.FACING, DIR_1710_TORCH[tSide-1]);
			return Blocks.REDSTONE_TORCH.defaultBlockState();
		}
		// Fluid: 1.7.10 meta = level (0 source, 1-7 flow, bit 8 "falling"), and neo holds the same scale in
		// LiquidBlock.LEVEL (LiquidBlock.java:70-78). Without this branch the meta was lost: a crucible spill (orig. Crucible:373-375
		// flowing_lava meta 1) landed as a lava SOURCE and never dissolved. Vanilla only: mod fluids have their own meta channel.
		if (aBlock == Blocks.WATER || aBlock == Blocks.LAVA)
			return aBlock.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, Math.min(15, tMeta));
		// Buttons: metas 1-4 = wall-mounted (the same map as torches); others — the default path.
		if (aBlock instanceof net.minecraft.world.level.block.ButtonBlock && (tMeta & 7) >= 1 && (tMeta & 7) <= 4)
			return aBlock.defaultBlockState()
				.setValue(net.minecraft.world.level.block.ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
				.setValue(net.minecraft.world.level.block.ButtonBlock.FACING, DIR_1710_TORCH[(tMeta & 7)-1]);
		// Doors: meta<8 = lower half (facing in meta); meta>=8 = upper (only the hinge is in meta; facing/open
		// is read from the ALREADY placed lower half — 1.7.10 stored them only below, placement order bottom→top).
		if (aBlock instanceof net.minecraft.world.level.block.DoorBlock) {
			if (tMeta < 8) return aBlock.defaultBlockState()
				.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
				.setValue(net.minecraft.world.level.block.DoorBlock.FACING, DIR_1710_DOOR[tMeta & 3]);
			BlockState tLower = state(aWorld, new BlockPos(aX, aY-1, aZ));
			BlockState rDoor = aBlock.defaultBlockState()
				.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
				.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, (tMeta & 1) == 0 ? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT : net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT);
			if (tLower.getBlock() == aBlock) rDoor = rDoor.setValue(net.minecraft.world.level.block.DoorBlock.FACING, tLower.getValue(net.minecraft.world.level.block.DoorBlock.FACING)).setValue(net.minecraft.world.level.block.DoorBlock.OPEN, tLower.getValue(net.minecraft.world.level.block.DoorBlock.OPEN));
			return rDoor;
		}
		// Beds: bit8 = headboard; facing (meta&3) is the same for both halves (neo: HEAD in the facing direction from FOOT).
		if (aBlock instanceof net.minecraft.world.level.block.BedBlock)
			return aBlock.defaultBlockState()
				.setValue(net.minecraft.world.level.block.BedBlock.PART, (tMeta & 8) != 0 ? net.minecraft.world.level.block.state.properties.BedPart.HEAD : net.minecraft.world.level.block.state.properties.BedPart.FOOT)
				.setValue(net.minecraft.world.level.block.BedBlock.FACING, DIR_1710_HORIZ[tMeta & 3]);
		// End portal frame: meta&3 facing + bit4 = eye inserted.
		if (aBlock == Blocks.END_PORTAL_FRAME)
			return Blocks.END_PORTAL_FRAME.defaultBlockState()
				.setValue(net.minecraft.world.level.block.EndPortalFrameBlock.FACING, DIR_1710_HORIZ[tMeta & 3])
				.setValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE, (tMeta & 4) != 0);
		// 1.7.10 log (one log block): meta&3 = variant (0=oak,1=spruce,2=birch,3=jungle), meta&12 = axis (0=Y,4=X,8=Z).
		// Port-flattening into OAK_LOG lost the variant — and cocoa survives ONLY on jungle logs (SUPPORTS_COCOA).
		if (aBlock == Blocks.OAK_LOG && tMeta != 0) {
			Block tLog = switch (tMeta & 3) {case 1 -> Blocks.SPRUCE_LOG; case 2 -> Blocks.BIRCH_LOG; case 3 -> Blocks.JUNGLE_LOG; default -> Blocks.OAK_LOG;};
			Direction.Axis tAxis = switch (tMeta & 12) {case 4 -> Direction.Axis.X; case 8 -> Direction.Axis.Z; default -> Direction.Axis.Y;};
			return tLog.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, tAxis);
		}
		// 1.7.10 double plants (one double_plant block): meta&7 = variant, bit8 = upper half.
		if (aBlock == Blocks.SUNFLOWER && tMeta != 0) {
			Block tPlant = switch (tMeta & 7) {case 1 -> Blocks.LILAC; case 2 -> Blocks.TALL_GRASS; case 3 -> Blocks.LARGE_FERN; case 4 -> Blocks.ROSE_BUSH; case 5 -> Blocks.PEONY; default -> Blocks.SUNFLOWER;};
			return tPlant.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, (tMeta & 8) != 0 ? net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER : net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
		}
		// Cocoa: meta&3 = compass side of the pod FROM the trunk → neo FACING (AT the support) = opposite; age = meta>>2 (0..2).
		if (aBlock == Blocks.COCOA)
			return Blocks.COCOA.defaultBlockState()
				.setValue(net.minecraft.world.level.block.CocoaBlock.FACING, DIR_1710_COMPASS[tMeta & 3].getOpposite())
				.setValue(net.minecraft.world.level.block.CocoaBlock.AGE, Math.min(2, tMeta >> 2));
		// Farmland: meta = moisture (1.7.10 clamp 7).
		if (aBlock == Blocks.FARMLAND && tMeta != 0)
			return Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, Math.min(net.minecraft.world.level.block.FarmBlock.MAX_MOISTURE, tMeta));
		// Anvil: meta&3 = horizontal facing (damage from meta>>2 does not occur in dungeons).
		if (aBlock instanceof net.minecraft.world.level.block.AnvilBlock)
			return aBlock.defaultBlockState().setValue(net.minecraft.world.level.block.AnvilBlock.FACING, DIR_1710_HORIZ[tMeta & 3]);
		// F4-flatten: colored families (wool/carpet/glass/panes/terracotta) and tallgrass — in 1.7.10 the subtype lived
		// in the meta of ONE block, the engine split them into separate blocks. The maps are the same CS.Flattened center used
		// for stack items (ST.make_), declared there ONCE. Take worldBlock (not block): families where the block's meta
		// meant something other than a subtype (skull — position, anvil — rotation) are handled by the branches ABOVE and do not
		// reach here. Without this, Behavior_Spray_Color (WD.set(..., WHITE_STAINED_GLASS, ~mColor & 15, 3)) painted
		// everything white: the block name is white, and the meta in the world means nothing.
		Block tFlat = gregapi.data.CS.Flattened.worldBlock(aBlock, tMeta);
		if (tFlat != null) return tFlat.defaultBlockState();
		return null;
	}

	public static boolean set(ChunkAccess aChunk, int aX, int aY, int aZ, Block aBlock, long aMeta) {
		// used to be aChunk.func_150807_a(localX,y,localZ,block,meta) — neo: ChunkAccess.setBlockState(BlockPos,BlockState,flags)
		// (LevelChunk.java:270 / ChunkAccess) wants a WORLD BlockPos (it masks &15 internally, using absolute
		// coordinates for heightmap/light engine) — ChunkPos.getBlockAt(localX,y,localZ) (ChunkPos.java:151) converts
		// local chunk coordinates to world ones, preserving the same calling contract (local x/z 0-15).
		// F6-worldgen: the receiver is widened LevelChunk->ChunkAccess (worldgen writes directly into the generating ProtoChunk/
		// ImposterProtoChunk, not the full chunk). setBlockState/getPos/getBlockState — all on ChunkAccess.
		BlockPos tChunkSetPos = aChunk.getPos().getBlockAt(aX, aY, aZ);
		// BUG-047: the atomic "state-with-meta in one set" path — mirrors WD.set(LevelAccessor) above (1.7.10 Chunk contract);
		// TE meta (getStateForExtendedMetaData=null) — the previous two-phase path below.
		if (aBlock instanceof IBlockExtendedMetaData) {
			BlockState tCurChunk = aChunk.getBlockState(tChunkSetPos);
			BlockState tNewChunk = ((IBlockExtendedMetaData)aBlock).getStateForExtendedMetaData(tCurChunk.getBlock() == aBlock ? tCurChunk : aBlock.defaultBlockState(), Code.bind4(aMeta));
			if (tNewChunk != null) return aChunk.setBlockState(tChunkSetPos, tNewChunk, F) != null;
		}
		boolean rSet = aChunk.setBlockState(tChunkSetPos, aBlock.defaultBlockState(), F) != null;
		if (aBlock instanceof IBlockExtendedMetaData) {
			byte tNewMeta = Code.bind4(aMeta);
			// the IBlockExtendedMetaData meta channel takes a BlockGetter (IBlockExtendedMetaData.java:28-29); ChunkAccess itself
			// IS a BlockGetter (ChunkAccess extends BlockGetter) → pass the chunk directly instead of the LevelChunk-only getLevel()
			// (ProtoChunk has no getLevel()). The TileEntity is looked up via ChunkAccess.getBlockEntity(pos) — available during generation.
			if (((IBlockExtendedMetaData)aBlock).getExtendedMetaData(aChunk, tChunkSetPos.getX(), tChunkSetPos.getY(), tChunkSetPos.getZ()) != tNewMeta) {
				((IBlockExtendedMetaData)aBlock).setExtendedMetaData(aChunk, tChunkSetPos.getX(), tChunkSetPos.getY(), tChunkSetPos.getZ(), tNewMeta);
				rSet = true;
			}
		}
		return rSet;
	}
	public static boolean set(ChunkAccess aChunk, int aX, int aY, int aZ, Block aBlock, long aMeta, boolean aRemoveGrassBelow) {
		if (aRemoveGrassBelow) {
			Block tBlock = aChunk.getBlockState(aChunk.getPos().getBlockAt(aX, aY-1, aZ)).getBlock(); // used to be aChunk.getBlock(x,y-1,z)
			if (tBlock == Blocks.GRASS_BLOCK || tBlock == Blocks.MYCELIUM) aChunk.setBlockState(aChunk.getPos().getBlockAt(aX, aY-1, aZ), Blocks.DIRT.defaultBlockState(), F); // was aChunk.func_150807_a(x,y-1,z,Blocks.DIRT,0)
		}
		return set(aChunk, aX, aY, aZ, aBlock, aMeta);
	}

	public static boolean replace(LevelAccessor aWorld, int aX, int aY, int aZ, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		if (aTargetBlock == null || aReplaceBlock == null) return F;
		if (aReplaceBlock != block(aWorld, aX, aY, aZ)) return F;
		if (aReplaceMeta != W && aReplaceMeta != meta(aWorld, aX, aY, aZ)) return F;
		return set(aWorld, aX, aY, aZ, aTargetBlock, aTargetMeta, Block.UPDATE_CLIENTS, F); // used to be aWorld.setBlock(x,y,z,block,meta,2) — flag 2=UPDATE_CLIENTS (Block.java:91-104); routed through the center set(...) — meta of own blocks (IBlockExtendedMetaData) is not lost
	}
	public static boolean replace(LevelAccessor aWorld, BlockPos aCoords, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		return replace(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta);
	}
	public static boolean replaceAll(LevelAccessor aWorld, int aX, int aY, int aZ, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		return replaceAll(aWorld, new BlockPos(aX, aY, aZ), aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta);
	}
	public static boolean replaceAll(LevelAccessor aWorld, BlockPos aCoords, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		if (!replace(aWorld, aCoords, aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta)) return F;
		HashSetNoNulls<BlockPos> tSwap,
		tDone  = new HashSetNoNulls<>(F, aCoords),
		tCheck = new HashSetNoNulls<>(F, aCoords),
		tNext  = new HashSetNoNulls<>();
		
		while (!tCheck.isEmpty() && tDone.size() < 32768) {
			tNext.clear();
			for (BlockPos tChecking : tCheck) {
				if (Math.abs(tChecking.getX() - aCoords.getX()) < 128 && Math.abs(tChecking.getZ() - aCoords.getZ()) < 128) for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
					BlockPos tCoords = new BlockPos(tChecking.getX()+i, tChecking.getY()+j, tChecking.getZ()+k);
					if (tDone.add(tCoords) && replace(aWorld, tCoords, aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta)) tNext.add(tCoords);
				}
			}
			tSwap = tNext; tNext = tCheck; tCheck = tSwap;
		}
		return T;
	}
	
	public static boolean sign(LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, long aFlags, String aLine1, String aLine2, String aLine3, String aLine4) {
		// used to be aWorld.setBlock(x,y,z,Blocks.OAK_WALL_SIGN,aSide,flags) — aSide was the direct meta-orientation of wall_sign
		// (2-5); neo: WallSignBlock.FACING (EnumProperty<Direction>, WallSignBlock.java:30) via the already
		// centralized FORGE_DIR[side]->Direction (the same array used throughout the file).
		setWG(aWorld, new BlockPos(aX, aY, aZ), Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, FORGE_DIR[aSide]), (int)aFlags);
		BlockEntity tSign = te(aWorld, aX, aY, aZ, T);
		if (!(tSign instanceof SignBlockEntity)) return F;
		// used to be signText[0..3]=String (a 1.7.10 mutable string array) -> neo SignText immutable (front/back):
		// getFrontText():77 -> a chain of setMessage(index,Component):84 (returns a new SignText) -> setText(SignText,isFront):161.
		((SignBlockEntity)tSign).setText(((SignBlockEntity)tSign).getFrontText()
			.setMessage(0, net.minecraft.network.chat.Component.literal(aLine1))
			.setMessage(1, net.minecraft.network.chat.Component.literal(aLine2))
			.setMessage(2, net.minecraft.network.chat.Component.literal(aLine3))
			.setMessage(3, net.minecraft.network.chat.Component.literal(aLine4)), T);
		return T;
	}
	
	/** F-worldgen: 1.7.10 {@code World.getSeed()} -> neo has ONLY {@code ServerLevel.getSeed()}:1697 (the base Level
	 *  has no seed). Deterministic per-chunk random — worldgen (server), where aWorld is always ServerLevel; client (no worldgen) -> 0.
	 *  DEFECT FIX (dungeon pass #39): the {@code WorldGenRegion} arriving from the Feature phase is NOT a ServerLevel → the previous gate
	 *  gave seed=0 (determinism within the world was preserved — 0 is the same for all chunks — but the world seed was lost). The real
	 *  channel: {@code WorldGenLevel.getSeed()} ({@code WorldGenLevel.java:8}; {@code WorldGenRegion.getSeed():367}
	 *  returns {@code level.getSeed()}); ServerLevel itself implements WorldGenLevel — one gate covers both. */
	public static long seed(LevelAccessor aWorld) {return aWorld instanceof net.minecraft.world.level.WorldGenLevel tWGL ? tWGL.getSeed() : 0L;}
	public static Random random(LevelAccessor aWorld, long aChunkX, long aChunkZ) {return random(seed(aWorld) ^ WD.dimensionId(aWorld), aChunkX >> 4, aChunkZ >> 4);}
	public static Random random(long aSeed, long aChunkX, long aChunkZ) {
		// Seed is XOR-ed with the Dimension ID to prevent multiple Dimensions from being identical in Ore Generation.
		// Yes that actually happened with Aromas Mining World, and resulted in a prospecting exploit.
		Random rRandom = new Random(aSeed);
		// Javas Random sucks so bad, the first few results are to be discarded
		for (int i = 0; i < 50; i++) rRandom.nextInt(0x00ffffff);
		// And then I use the first Result as a Seed for a second Random because it is THAT bad!
		rRandom = new Random(aSeed ^ ((rRandom.nextLong() >> 2 + 1L) * aChunkX + (rRandom.nextLong() >> 2 + 1L) * aChunkZ));
		// Javas Random still sucks badly, discarding some results again.
		for (int i = 0; i < 50; i++) rRandom.nextInt(0x00ffffff);
		// There we have it, a somewhat working Random function that is actually random
		// and does not cause my Code to generate almost perfect Diagonal Lines of Ores.
		return rRandom;
	}
	
	public static int random(LevelAccessor aWorld, int aX, int aY, int aZ, int aBound) {return random(seed(aWorld) ^ WD.dimensionId(aWorld), aX, aY, aZ, aBound);}
	public static int random(long aSeed, int aX, int aY, int aZ, int aBound) {
		Random rRandom = new Random(aSeed ^ aY);
		for (int i = 0; i < 10; i++) rRandom.nextInt(0x00ffffff);
		rRandom = new Random(aSeed ^ ((rRandom.nextLong() >> 2 + 1L) * aX + (rRandom.nextLong() >> 2 + 1L) * aZ));
		for (int i = 0; i < 10; i++) rRandom.nextInt(0x00ffffff);
		return rRandom.nextInt(aBound);
	}
	
	public static Random random(BlockEntity aTileEntity) {return new Random(aTileEntity.getBlockPos().getX() ^ aTileEntity.getBlockPos().getY() ^ aTileEntity.getBlockPos().getZ());} // used to be .x/.y/.z — BlockEntity.getBlockPos() (BlockEntity.java:232)
	public static int random(BlockEntity aTileEntity, int aBound) {return random(aTileEntity).nextInt(aBound);}
	public static boolean random(BlockEntity aTileEntity, int aBound, long aTime) {return random(aTileEntity, aBound) == aTime % aBound;}
	
	public static boolean border(int aFromX, int aFromZ, int aToX, int aToZ) {return aFromX >> 4 != aToX >> 4 || aFromZ >> 4 != aToZ >> 4;}
	
	public static boolean even(BlockEntity aTileEntity) {return even(aTileEntity.getBlockPos().getX(), aTileEntity.getBlockPos().getY(), aTileEntity.getBlockPos().getZ());} // used to be .x/.y/.z
	public static boolean even(BlockPos aCoords) {return even(aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	public static boolean even(int... aCoords) {int i = 0; for (int tCoord : aCoords) if (tCoord % 2 == 0) i++; return i % 2 == 0;}
	
	public static int evenness(BlockEntity aTileEntity) {return evenness(aTileEntity.getBlockPos().getX(), aTileEntity.getBlockPos().getY(), aTileEntity.getBlockPos().getZ());} // used to be .x/.y/.z
	public static int evenness(BlockPos aCoords) {return evenness(aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	public static int evenness(int... aCoords) {int i = 0; for (int tCoord : aCoords) {i <<= 1; if (tCoord % 2 != 0) i++;} return i;}
	
	// used to be aWorld.getBlock(x,y,z)/getBlockMetadata(x,y,z)/setBlock(x,y,z,block,meta,flags) — meta through the centralized meta(...)
	public static boolean setIfDiff(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags) {return (block(aWorld, aX, aY, aZ) != aBlock || meta(aWorld, aX, aY, aZ) != aMeta) && set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags, F);} // used to be aWorld.setBlock(x,y,z,block,meta,flags) — routed through the center set(...)

	public static boolean set(LevelAccessor aWorld, int aX, int aY, int aZ, ItemStack aStack) {
		Block tBlock = ST.block(aStack);
		if (tBlock == NB) return F;
		if (tBlock instanceof IBlockPlacable) return ((IBlockPlacable)tBlock).placeBlock(aWorld, aX, aY, aZ, (byte)6, ST.meta_(aStack), ItemNBT.get(aStack), T, F);
		if (ST.meta_(aStack) < 16) return set(aWorld, aX, aY, aZ, tBlock, ST.meta_(aStack), Block.UPDATE_ALL, F); // used to be aWorld.setBlock(x,y,z,block,meta,3) — flag 3=UPDATE_ALL; routed through the center set(...)
		return F;
	}

	public static boolean leafdecay(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {return leafdecay(aWorld, aX, aY, aZ, aBlock, F, F);}
	public static boolean leafdecay(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, boolean aOnlyTopArea) {return leafdecay(aWorld, aX, aY, aZ, aBlock, aOnlyTopArea, F);}
	public static boolean leafdecay(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, boolean aOnlyTopArea, boolean aTreeCapitator) {
		// F-tree: Forge Block.canSustainLeaves (a block keeps leaves from decaying — logs) was removed -> neo tag
		// BlockTags.LOGS (BlockTags.java:38; neo's leaf-decay looks exactly at logs), checked on the state.
		if (aBlock == null || state(aWorld, new BlockPos(aX, aY, aZ)).is(net.minecraft.tags.BlockTags.LOGS)) {
			for (int j = (aOnlyTopArea ? 0 : -7); j <= 7; ++j) for (int i = -7; i <= 7; ++i) for (int k = -7; k <= 7; ++k) {
				Block tBlock = state(aWorld, new BlockPos(aX+i, aY+j, aZ+k)).getBlock(); // used to be aWorld.getBlock(x+i,y+j,z+k)
				if (tBlock != NB) {
					if (tBlock == Blocks.BROWN_MUSHROOM_BLOCK || tBlock == Blocks.RED_MUSHROOM_BLOCK) {
						if (aTreeCapitator && Math.abs(i) <= 4 && Math.abs(k) <= 4 && j <= 0 && j >= -2) aWorld.destroyBlock(new BlockPos(aX+i, aY+j, aZ+k), T); // used to be aWorld.func_147480_a(x,y,z,drop) — LevelWriter.destroyBlock(BlockPos,boolean) (LevelWriter.java:18)
					} else if (IL.NeLi_Wart_Block_Crimson.equal(tBlock) || IL.NeLi_ShroomLight.equal(tBlock)) {
						if (aTreeCapitator && Math.abs(i) <= 4 && Math.abs(k) <= 4) aWorld.destroyBlock(new BlockPos(aX+i, aY+j, aZ+k), T); // used to be aWorld.func_147480_a(x,y,z,drop)
					} else {
						if (WD.leaves(tBlock, aWorld, aX+i, aY+j, aZ+k)) {
							// F-tree (BUG-005): in 1.7.10 scheduleBlockUpdate hit the leaf's updateTick, which ITSELF checked support and
							// decayed (a single channel). In neo 26.1.2 the VANILLA leaf channel is split: the scheduled tick only recomputes
							// DISTANCE (LeavesBlock.tick:79-81), while decay lives in randomTick:67-72 (decaying: DISTANCE==7 && !PERSISTENT).
							// Moreover, scheduleTick for a vanilla leaf is HARMFUL here: a pending tick (1..100) is DEDUPED by (block,pos)
							// and blocks cascading delay-1 DISTANCE recomputations from felled logs (LevelChunkTicks.schedule:52 —
							// ticksPerPosition.add), making decay SLOWER than vanilla (measured by the gt6leafprobe rig: settle >100 ticks).
							// -> for vanilla leaves, do NOT scheduleTick, instead force-randomTick with the SAME delay formula through the central
							// server-tick queue (GT_API_Proxy.DELAYED_LEAF_DECAYS, executed by the engine's state.tick+state.randomTick,
							// an under-ripened DISTANCE catches up through repeats there too). GT6 and other leaves — as in the original: scheduleTick,
							// their updateTick decays them itself (tick bridge BlockBase -> updateTick2).
							if (tBlock instanceof net.minecraft.world.level.block.LeavesBlock) {
								if (aWorld instanceof net.minecraft.server.level.ServerLevel)
									gregapi.GT_API_Proxy.DELAYED_LEAF_DECAYS.add(new Object[] {aWorld, new BlockPos(aX+i, aY+j, aZ+k), SERVER_TIME + 1 + RNGSUS.nextInt(100), 0});
							} else {
								aWorld.scheduleTick(new BlockPos(aX+i, aY+j, aZ+k), tBlock, 1+RNGSUS.nextInt(100)); // used to be aWorld.scheduleTick(new BlockPos(x, y, z), block, delay) — ScheduledTickAccess.scheduleTick(BlockPos,Block,int) (ScheduledTickAccess.java:21)
							}
						}
					}
				}
			}
			return T;
		}
		return F;
	}
	
	// F5 (a single fluids center): in Forge the "identity of a fluid" was the IFluidBlock interface on the common ancestor
	// BlockFluidBase (→ BlockFluidClassic/BlockFluidFinite). The port reproduced the hierarchy with its own classes
	// (decisions/F5-fluids.md §5): the SINGLE marker of a GT6 fluid is BlockFluidBaseGT, its descendants BlockWaterlike
	// (classic) / BlockBaseFluid (finite). GT6 fluids do NOT carry IFluidBlock/LiquidBlock → the classifiers below
	// ask this live center instead (the same approach as WD.getMaterial:530). LiquidBlock=vanilla, IFluidBlock=foreign mods (interop).
	public static boolean liquid(LevelAccessor aWorld, int aX, int aY, int aZ) {return liquid(state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean liquid(Block aBlock) {return aBlock instanceof LiquidBlock || aBlock instanceof gregapi.block.fluid.BlockFluidBaseGT || aBlock instanceof IFluidBlock;} // used to be BlockLiquid || IFluidBlock; BlockFluidBaseGT = GT6 fluids (do not carry IFluidBlock)

	public static boolean liquid_classic(LevelAccessor aWorld, int aX, int aY, int aZ) {return liquid_classic(state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean liquid_classic(Block aBlock) {return aBlock instanceof LiquidBlock || aBlock instanceof BlockWaterlike;} // used to be BlockLiquid || BlockFluidClassic; BlockWaterlike = GT6 classic-style

	public static boolean liquid_finite(LevelAccessor aWorld, int aX, int aY, int aZ) {return liquid_finite(state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean liquid_finite(Block aBlock) {return aBlock instanceof gregapi.block.fluid.BlockBaseFluid;} // used to be BlockFluidFinite; BlockBaseFluid = GT6 finite-style (the only caller — MultiTileEntityFluidSpring)

	public static boolean liquid_borken(LevelAccessor aWorld, int aX, int aY, int aZ) {return liquid_borken(state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean liquid_borken(Block aBlock) {return !(aBlock instanceof IItemGT) && liquid_classic(aBlock);}
	
	public static boolean stone(Block aBlock, short aMeta) {
		if (aBlock == NB) return F;
		if (aBlock == Blocks.OBSIDIAN) return T;
		ItemStackContainer tStack = new ItemStackContainer(aBlock, 1, aMeta);
		return BlocksGT.stoneToNormalOres.containsKey(tStack) || BlocksGT.stoneToBrokenOres.containsKey(tStack) || BlocksGT.stoneToSmallOres.containsKey(tStack);
	}
	
	public static boolean floor(LevelAccessor aWorld, int aX, int aY, int aZ) {return floor(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean floor(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {return WD.sideSolid(aBlock, aWorld, aX, aY, aZ, FORGE_DIR[SIDE_UP]) || floor(aBlock);}
	public static boolean floor(Block aBlock) {return WD.opaque(aBlock) || aBlock instanceof SlabBlock || aBlock instanceof StairBlock || aBlock instanceof BlockMetaType;}
	
	@SuppressWarnings("unlikely-arg-type")
	public static boolean ore(Block aBlock, short aMeta) {return (aBlock instanceof IBlockPlacable && (BlocksGT.stoneToBrokenOres.containsValue(aBlock) || BlocksGT.stoneToNormalOres.containsValue(aBlock) || BlocksGT.stoneToSmallOres.containsValue(aBlock)) || OM.prefixcontains(ST.make(aBlock, 1, aMeta), TD.Prefix.ORE));}
	public static boolean ore_stone(Block aBlock, short aMeta) {return ore(aBlock, aMeta) || stone(aBlock, aMeta);}
	
	public static boolean visOcc(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {return visOpq(aWorld, aX+1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX+1, aZ), aDefault) && visOpq(aWorld, aX-1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX-1, aZ), aDefault) && visOpq(aWorld, aX, aY+1, aZ, T, aDefault) && visOpq(aWorld, aX, aY-1, aZ, T, aDefault) && visOpq(aWorld, aX, aY, aZ+1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ+1), aDefault) && visOpq(aWorld, aX, aY, aZ-1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ-1), aDefault);}
	public static boolean visOpq(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {BlockPos tP = new BlockPos(aX, aY, aZ); return aLoadUnloadedChunks || exists(aWorld, aX, aY, aZ) ? visOpq(state(aWorld, tP).getBlock()) : aDefault;} // used to be blockExists/getBlock(x,y,z)
	// F3-render ROOT of "a GT6 block's face disappears at the seam with a fence/slab/any NOT-full neighbor": 1.7.10 hid the face
	// if the neighbor was isOpaqueCube() (a FULL opaque cube). The port mistakenly took canOcclude() (WD.opaque) — which is TRUE also for slabs/
	// stairs/fences (they occlude PARTIALLY) → the GT6 block's face was hidden against them too. The correct neo equivalent of isOpaqueCube =
	// BlockState.isSolidRender() (=Block.isShapeFullBlock(occlusionShape), BlockBehaviour.java:499) — TRUE only for a FULL cube.
	public static boolean visOpq(Block aBlock) {return aBlock.defaultBlockState().isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO) || VISUALLY_OPAQUE_BLOCKS.contains(aBlock);} // 1.20.1: isSolidRender takes (BlockGetter,BlockPos); the engine makes a position-less query through EmptyBlockGetter (BlockBehaviour.java:916)
	
	public static boolean occ(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {return opq(aWorld, aX+1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX+1, aZ), aDefault) && opq(aWorld, aX-1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX-1, aZ), aDefault) && opq(aWorld, aX, aY+1, aZ, T, aDefault) && opq(aWorld, aX, aY-1, aZ, T, aDefault) && opq(aWorld, aX, aY, aZ+1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ+1), aDefault) && opq(aWorld, aX, aY, aZ-1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ-1), aDefault);}
	public static boolean opq(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {BlockPos tP = new BlockPos(aX, aY, aZ); return aLoadUnloadedChunks || exists(aWorld, aX, aY, aZ) ? opq(state(aWorld, tP).getBlock()) : aDefault;} // used to be blockExists/getBlock(x,y,z)
	public static boolean opq(Block aBlock) {return WD.opaque(aBlock) && !(aBlock instanceof LeavesBlock);}
	
	public static boolean air(LevelAccessor aWorld, int aX, int aY, int aZ) {return air(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean air(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock == NB || (state(aWorld, new BlockPos(aX, aY, aZ)).isAir() && !(MD.TC.mLoaded && !WD.opaque(aBlock) && te(aWorld, aX, aY, aZ, T) instanceof INode));} // used to be aBlock.isAir(world,x,y,z) — BlockBehaviour.java:575 state.isAir()
	public static boolean air(Block aBlock) {return aBlock == NB;}
	/** BlockGetter variant (without the Level-only TC/INode check): a pure air test for block physics (canDisplace etc.). */
	public static boolean air(BlockGetter aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock == NB || state(aWorld, new BlockPos(aX, aY, aZ)).isAir();}
	
	public static boolean lava(BlockGetter aWorld, int aX, int aY, int aZ) {return lava(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean lava(BlockGetter aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock == Blocks.LAVA || aBlock == Blocks.LAVA;}
	public static boolean lava(Block aBlock) {return aBlock == Blocks.LAVA || aBlock == Blocks.LAVA;}
	
	public static boolean water(BlockGetter aWorld, int aX, int aY, int aZ) {return water(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean water(BlockGetter aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock == Blocks.WATER || aBlock == Blocks.WATER;}
	public static boolean water(Block aBlock) {return aBlock == Blocks.WATER || aBlock == Blocks.WATER;}
	
	public static boolean waterstream(Block aBlock) {return MD.Streams.mLoaded && UT.Code.stringValidate(ST.regName(aBlock)).startsWith("streams:river/tile.water");}
	
	public static boolean anywater(BlockGetter aWorld, int aX, int aY, int aZ) {return anywater(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean anywater(BlockGetter aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock instanceof BlockWaterlike || water(aWorld, aX, aY, aZ, aBlock) || waterstream(aBlock);}
	public static boolean anywater(Block aBlock) {return aBlock instanceof BlockWaterlike || water(aBlock) || waterstream(aBlock);}
	
	public static boolean bedrock(LevelAccessor aWorld, int aX, int aY, int aZ) {return bedrock(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean bedrock(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {return bedrock(aBlock);}
	public static boolean bedrock(Block aBlock) {return aBlock == Blocks.BEDROCK || IL.BTL_Bedrock.equal(aBlock);}
	
	public static boolean grass(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {return grass(block(aWorld, aX, aY, aZ, aLoadUnloadedChunks), meta(aWorld, aX, aY, aZ, aLoadUnloadedChunks));}
	public static boolean grass(LevelAccessor aWorld, int aX, int aY, int aZ) {return grass(block(aWorld, aX, aY, aZ), meta(aWorld, aX, aY, aZ));}
	public static boolean grass(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta) {return grass(aBlock, aMeta);}
	public static boolean grass(Block aBlock, long aMeta) {
		if (aBlock == Blocks.DEAD_BUSH) return T;
		if (aBlock == Blocks.SUNFLOWER)  return aMeta ==  2 || aMeta ==  3;
		if (IL.TF_Tall_Grass.equal(aBlock)) return aMeta ==  8 || aMeta == 10;
		return IL.AETHER_Tall_Grass.equal(aBlock);
	}
	
	public static boolean irrelevant(LevelAccessor aWorld, int aX, int aY, int aZ) {return irrelevant(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean irrelevant(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {return air(aWorld, aX, aY, aZ, aBlock) || aBlock == Blocks.VINE || aBlock == Blocks.SNOW || aBlock == Blocks.FIRE || grass(aWorld, aX, aY, aZ) || anywater(aBlock);}
	
	public static boolean easyRep(LevelAccessor aWorld, int aX, int aY, int aZ) {return easyRep(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	// ⛔ The plant predicate is VegetationBlock, NOT BushBlock. In 1.7.10 `BlockBush` was the base of ALL plants
	// (recompSrc: BlockCrops, BlockDeadBush, BlockDoublePlant, BlockFlower, BlockLilyPad, BlockMushroom,
	// BlockNetherWart, BlockSapling — all `extends BlockBush`), while in neo the hierarchy split and `BushBlock`
	// narrowed to ONE registry block out of 1875 against 72 for `VegetationBlock` (measurement `M-84`). With the previous predicate
	// 58 plants — saplings of every wood type, dandelion, torchflower — stopped being "easily displaceable", even though
	// in the original they were. The same transition is already made in `dropUnsupportedPlants` below: one predicate per file.
	public static boolean easyRep(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {return air(aWorld, aX, aY, aZ, aBlock) || aBlock instanceof net.minecraft.world.level.block.BushBlock || aBlock instanceof SnowLayerBlock || aBlock instanceof FireBlock || WD.leaves(aBlock, aWorld, aX, aY, aZ) || state(aWorld, new BlockPos(aX, aY, aZ)).canBeReplaced();}

	/** A cell is suitable for a SURFACE worldgen object (indicator pebble, stick, bush, flower, sapling).
	 *
	 *  <p>Differs from {@link #easyRep} by exactly one thing: fluid does NOT free the cell. {@code easyRep} answers
	 *  the question "can the cell be occupied at all" and therefore lets water through — it is {@code replaceable} and in 1.7.10
	 *  ({@code canBeReplacedByLeaves} for a non-solid block) also gave "yes". For worldgen this is not enough: every
	 *  surface generator's fluid gate sits on the SUPPORT (the block being placed on), while the target cell itself
	 *  was checked by nothing but {@code easyRep}.</p>
	 *
	 *  <p>While the ray comes from the sky, water is caught before the support and the hole is invisible. But {@code WorldgenOresLarge:118}
	 *  casts its ray not from the sky, but from "vein top + 25" — under an ocean and above an underground aquifer this height is already
	 *  BELOW the water level: the ray stops at the floor sand (a legitimate support), and the indicator lands right in the water. Measurement on
	 *  a fresh world: pebbles in water 332 out of 7983 (99% of them have an ore vein below), sticks 1 out of 186 — sticks have
	 *  no sand in their support list, and the floor is 78% sand, hence the difference in frequency for the same root cause.</p>
	 *
	 *  <p>In 1.7.10 there were no indicators underwater, so the behavior reverts to the original. {@code FluidState} is also
	 *  checked: in neo water occurs not only as a separate block but also as a {@code waterlogged} state. */
	public static boolean easyRepDry(LevelAccessor aWorld, int aX, int aY, int aZ) {return easyRepDry(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());}
	public static boolean easyRepDry(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {
		if (!easyRep(aWorld, aX, aY, aZ, aBlock)) return F;
		BlockState tState = state(aWorld, new BlockPos(aX, aY, aZ));
		return !getMaterial(tState.getBlock()).isLiquid() && tState.getFluidState().isEmpty();
	}

	/** F6-worldgen, class "lost support cascade". In 1.7.10 worldgen finished the world through LIVE {@code World.setBlock}
	 *  with neighbor notification, and the engine itself dropped whatever was left without support ({@code BlockDoublePlant
	 *  .onNeighborBlockChange -> checkAndDropBlock}). In neo worldgen goes through {@code WorldGenRegion}, which writes
	 *  the state straight into the chunk and does NOT notify neighbors at all ({@code neo-decompiled/server/level/WorldGenRegion.java:257-262})
	 *  — the channel vanished silently. GT6, meanwhile, occupies the surface AFTER vanilla vegetation (the
	 *  {@code TOP_LAYER_MODIFICATION} step, {@code GT6WorldgenFeature:146}) and deliberately displaces it: {@code easyRep}
	 *  permits replacing {@code BushBlock}, and that includes the LOWER half of a two-block plant
	 *  (tall_grass, large_fern). The upper half stayed hanging in mid-air — the player's "tall grass floating over stone" symptom.
	 *
	 *  <p>Fixed by ONE pass per chunk, not in every generator: there are many, heterogeneous write paths —
	 *  {@code placeBlock} on an MTE (stones/sticks/bushes: {@code WorldgenOnSurface}, {@code WorldgenOresLarge:119},
	 *  {@code WorldgenOresBedrock:181}) and {@code WD.set} for stone layers ({@code WorldgenStoneLayers:126-176},
	 *  including the REPLACEABLE_BLOCKS branch). A final sweep after worldgen is the original's own approach: nearby,
	 *  items dropped during generation are cleaned up the same way ({@code GT6WorldGenerator}).</p>
	 *
	 *  <p>Traversal is over non-empty chunk sections (empty ones are skipped entirely) with a cheap block-type pre-filter:
	 *  it checks exactly what {@code easyRep} allows displacing — {@code BushBlock} (grass, ferns,
	 *  flowers, saplings, both halves of two-block plants). Walking by the height map is NOT ALLOWED: during generation it is not yet
	 *  finished, and a surface-level strip misses part of the cases (measured: 14 hanging vs. 7).</p>
	 *  @return how many blocks were removed. */
	public static int dropUnsupportedPlants(LevelAccessor aWorld, net.minecraft.world.level.chunk.ChunkAccess aChunk) {
		int rDropped = 0;
		int tMinX = aChunk.getPos().getMinBlockX(), tMinZ = aChunk.getPos().getMinBlockZ();
		// Traversal TOP TO BOTTOM through the entire habitable column via the chunk's own getBlockState. Neither the height map, nor
		// LevelChunkSection.hasOnlyAir() deserves trust during generation — both already produced a miss
		// (height-map strip: 7 hanging left; skipping "empty" sections: 20, meaning the sweep barely ran).
		// The expensive part is removed by a type pre-filter: canSurvive is asked only of VegetationBlock.
		// ⛔ Specifically VegetationBlock, NOT BushBlock: in 1.7.10 BlockDoublePlant extended BlockBush, but in neo
		// the hierarchy split — DoublePlantBlock extends VegetationBlock, BushBlock extends VegetationBlock,
		// and two-block plants (exactly our case) do NOT fall under instanceof BushBlock. Measured: with a
		// BushBlock filter the sweep saw 1 bush block out of 2395 double plants in the same chunks.
		int tTop = Math.min(maxY(aChunk), 200), tBottom = Math.max(minY(aChunk)+1, 40);
		for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) for (int tY = tTop; tY >= tBottom; tY--) {
			BlockPos tPos = new BlockPos(tMinX+i, tY, tMinZ+j);
			BlockState tState = aChunk.getBlockState(tPos);
			if (!(tState.getBlock() instanceof net.minecraft.world.level.block.BushBlock) || tState.canSurvive(aWorld, tPos)) continue;
			aWorld.setBlock(tPos, Blocks.AIR.defaultBlockState(), 2);
			rDropped++;
		}
		return rDropped;
	}

	// used to be aWorld.getBiomeGenForCoords(x,z) — LevelReader.getBiome(BlockPos) (LevelReader.java:42); the F6 center
	// BiomeNameSet.contains(Holder<Biome>) resolves identity itself (unwrapKey().location()), the raw
	// .value().biomeName (a dead 1.7.10 field) is no longer needed — gregapi/code/BiomeNameSet.java.
	public static boolean infiniteWater(LevelAccessor aWorld, int aX, int aY, int aZ              ) {int tLevel = waterLevel(aWorld); return                                                                                       UT.Code.inside(tLevel-15, tLevel, aY) && BIOMES_RIVER_LAKE.contains(aWorld.getBiome(new BlockPos(aX, aY, aZ)));}
	public static boolean infiniteWater(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {int tLevel = waterLevel(aWorld); return waterstream(aBlock) || ((aBlock == Blocks.WATER || aBlock == Blocks.WATER) && UT.Code.inside(tLevel-15, tLevel, aY) && BIOMES_RIVER_LAKE.contains(aWorld.getBiome(new BlockPos(aX, aY, aZ))));}
	
	public static boolean hasCollide(LevelAccessor aWorld, int aX, int aY, int aZ) {return hasCollide(aWorld, aX, aY, aZ, state(aWorld, new BlockPos(aX, aY, aZ)).getBlock());} // used to be aWorld.getBlock(x,y,z)
	// used to be aBlock.getCollisionBoundingBoxFromPool(world,x,y,z)!=null — BlockState.getCollisionShape(level,pos).isEmpty()
	// inverted (BlockBehaviour.java:674; VoxelShape.isEmpty(), VoxelShape.java:73); isOpaqueCube() untouched.
	public static boolean hasCollide(LevelAccessor aWorld, int aX, int aY, int aZ, Block aBlock) {return WD.opaque(aBlock) || !state(aWorld, new BlockPos(aX, aY, aZ)).getCollisionShape(aWorld, new BlockPos(aX, aY, aZ)).isEmpty();}

	public static boolean hasCollide(LevelAccessor aWorld, BlockPos aCoords) {return hasCollide(aWorld, aCoords, state(aWorld, aCoords).getBlock());} // used to be aWorld.getBlock(x,y,z)
	public static boolean hasCollide(LevelAccessor aWorld, BlockPos aCoords, Block aBlock) {return WD.opaque(aBlock) || !state(aWorld, aCoords).getCollisionShape(aWorld, aCoords).isEmpty();} // used to be aBlock.getCollisionBoundingBoxFromPool(world,x,y,z)!=null
	
	public static boolean flaming(LevelAccessor aWorld, int aX, int aY, int aZ) {return block(aWorld, aX, aY, aZ, F) instanceof FireBlock;}
	public static boolean burning(LevelAccessor aWorld, int aX, int aY, int aZ) {return flaming(aWorld, aX, aY, aZ) || flaming(aWorld, aX+1, aY, aZ) || flaming(aWorld, aX-1, aY, aZ) || flaming(aWorld, aX, aY+1, aZ) || flaming(aWorld, aX, aY-1, aZ) || flaming(aWorld, aX, aY, aZ+1) || flaming(aWorld, aX, aY, aZ-1);}
	
	public static void burn(LevelAccessor aWorld, BlockPos aCoords, boolean aReplaceCenter, boolean aCheckFlammability) {for (byte tSide : aReplaceCenter?ALL_SIDES_MIDDLE_UP:ALL_SIDES_VALID) fire(aWorld, aCoords.getX()+OFFX[tSide], aCoords.getY()+OFFY[tSide], aCoords.getZ()+OFFZ[tSide], aCheckFlammability);}
	public static void burn(LevelAccessor aWorld, int aX, int aY, int aZ  , boolean aReplaceCenter, boolean aCheckFlammability) {for (byte tSide : aReplaceCenter?ALL_SIDES_MIDDLE_UP:ALL_SIDES_VALID) fire(aWorld, aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide], aCheckFlammability);}
	
	public static boolean fire(LevelAccessor aWorld, BlockPos aCoords, boolean aCheckFlammability) {return fire(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aCheckFlammability);}
	public static boolean fire(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aCheckFlammability) {
		BlockPos tFirePos = new BlockPos(aX, aY, aZ);
		Block tBlock = state(aWorld, tFirePos).getBlock(); // used to be aWorld.getBlock(x,y,z)
		if (WD.getMaterial(tBlock) == Material.lava || WD.getMaterial(tBlock) == Material.fire) return F;
		// used to be tBlock.getCollisionBoundingBoxFromPool(world,x,y,z)==null — BlockState.getCollisionShape(level,pos).isEmpty() (BlockBehaviour.java:674)
		if (WD.getMaterial(tBlock) == Material.carpet || state(aWorld, tFirePos).getCollisionShape(aWorld, tFirePos).isEmpty()) {
			if (MD.TC.mLoaded && te(aWorld, aX, aY, aZ, T) instanceof INode) return F;
			// F-block: IBlockExtension.getFlammability(int meta,world,x,y,z,dir) -> BlockState.getFlammability(
			// BlockGetter,BlockPos,Direction) (IBlockExtension.java:677) — on the state, not on Block.
			if (state(aWorld, tFirePos).getFlammability(aWorld, tFirePos, FORGE_DIR[SIDE_ANY]) > 0) return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // used to be aWorld.setBlock(x,y,z,Blocks.FIRE,0,3)
			if (tBlock instanceof IItemGT) return F;
			if (aCheckFlammability) {
				for (byte tSide : ALL_SIDES_VALID) {
					BlockPos tAdjPos = new BlockPos(aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]);
					Block tAdjacent = block(aWorld, aX, aY, aZ, tSide);
					if (tAdjacent == Blocks.CHEST || tAdjacent == Blocks.TRAPPED_CHEST) return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // used to be aWorld.setBlock(x,y,z,Blocks.FIRE) (3-arg default meta=0,flags=3)
					// F-block: getFlammability on the neighbor's BlockState (IBlockExtension.java:677), the neighbor's pos is computed.
					if (state(aWorld, tAdjPos).getFlammability(aWorld, tAdjPos, FORGE_DIR_OPPOSITES[tSide]) > 0) return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // used to be aWorld.setBlock(x,y,z,Blocks.FIRE)
				}
			} else {
				return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // used to be aWorld.setBlock(x,y,z,Blocks.FIRE,0,3)
			}
		}
		return F;
	}
	
	public static boolean oreGenReplaceable(LevelAccessor aWorld, int aX, int aY, int aZ, boolean aAllowAir) {
		Block aBlock = state(aWorld, new BlockPos(aX, aY, aZ)).getBlock(); // used to be aWorld.getBlock(x,y,z)
		if (aBlock == NB) return aAllowAir;
		byte aMeta = meta(aWorld, aX, aY, aZ); // used to be (byte)WD.meta(aWorld, x,y,z) — the centralized meta(...), META MODEL item 4
		if (BlocksGT.sDontGenerateOresIn.contains(new ItemStackContainer(aBlock, 1, aMeta))) return F;
		if (BlocksGT.stoneToNormalOres.containsKey(new ItemStackContainer(aBlock, 1, aMeta))) return T;
		if (Blocks.STONE      != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.STONE     )) return T;
		if (Blocks.GRAVEL     != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.GRAVEL    )) return T;
		if (Blocks.SAND       != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.SAND      )) return T;
		if (Blocks.NETHERRACK != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.NETHERRACK)) return T;
		if (Blocks.END_STONE  != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.END_STONE )) return T;
		return F;
	}
	
	public static boolean setOre(LevelAccessor aWorld, int aX, int aY, int aZ, OreDictMaterial aMaterial) {
		return aMaterial != null && setOre(aWorld, aX, aY, aZ, aMaterial.mID);
	}
	
	public static boolean setOre(LevelAccessor aWorld, int aX, int aY, int aZ, short aID) {
		if (aID <= 0 && aID == W) return F;
		Block aBlock = state(aWorld, new BlockPos(aX, aY, aZ)).getBlock(); // used to be aWorld.getBlock(x,y,z)
		if (aBlock == NB) return F;
		byte aMeta = meta(aWorld, aX, aY, aZ); // used to be (byte)WD.meta(aWorld, x,y,z)
		if (BlocksGT.sDontGenerateOresIn.contains(new ItemStackContainer(aBlock, 1, aMeta))) return F;
		IBlockPlacable tBlock = BlocksGT.stoneToNormalOres.get(new ItemStackContainer(aBlock, 1, aMeta));
		if (tBlock == null) {
		if (Blocks.STONE      != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.STONE     )) tBlock = BlocksGT.ore; else
		if (Blocks.GRAVEL     != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.GRAVEL    )) tBlock = BlocksGT.oreGravel; else
		if (Blocks.SAND       != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.SAND      )) tBlock = BlocksGT.oreSand; else
		if (Blocks.NETHERRACK != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.NETHERRACK)) tBlock = BlocksGT.oreNetherrack; else
		if (Blocks.END_STONE  != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.END_STONE )) tBlock = BlocksGT.oreEndstone;
		}
		return tBlock != null && tBlock.placeBlock(aWorld, aX, aY, aZ, (byte)6, aID, null, F, T);
	}
	
	public static boolean setSmallOre(LevelAccessor aWorld, int aX, int aY, int aZ, OreDictMaterial aMaterial) {
		return aMaterial != null && setSmallOre(aWorld, aX, aY, aZ, aMaterial.mID);
	}
	
	public static boolean setSmallOre(LevelAccessor aWorld, int aX, int aY, int aZ, short aID) {
		if (aID <= 0 && aID == W) return F;
		Block aBlock = state(aWorld, new BlockPos(aX, aY, aZ)).getBlock(); // used to be aWorld.getBlock(x,y,z)
		if (aBlock == NB || WD.bedrock(aBlock)) return F;
		byte aMeta = meta(aWorld, aX, aY, aZ); // used to be (byte)WD.meta(aWorld, x,y,z)
		if (BlocksGT.sDontGenerateOresIn.contains(new ItemStackContainer(aBlock, 1, aMeta))) return F;
		IBlockPlacable tBlock = BlocksGT.stoneToSmallOres.get(new ItemStackContainer(aBlock, 1, aMeta));
		if (tBlock == null) {
		if (Blocks.STONE      != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.STONE     )) tBlock = BlocksGT.oreSmall; else
		if (Blocks.GRAVEL     != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.GRAVEL    )) tBlock = BlocksGT.oreSmallGravel; else
		if (Blocks.SAND       != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.SAND      )) tBlock = BlocksGT.oreSmallSand; else
		if (Blocks.NETHERRACK != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.NETHERRACK)) tBlock = BlocksGT.oreSmallNetherrack; else
		if (Blocks.END_STONE  != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.END_STONE )) tBlock = BlocksGT.oreSmallEndstone;
		}
		return tBlock != null && tBlock.placeBlock(aWorld, aX, aY, aZ, (byte)6, aID, null, F, T);
	}
	
	/** Removes Bedrock from that Position and replaces it with regular Stone of the region. */
	public static boolean removeBedrock(LevelAccessor aWorld, int aX, int aY, int aZ) {
		// used to be aWorld.getBlock(x,y,z) + WD.dimensionId(aWorld)==DIM_NETHER — Level.dimension()==Level.NETHER,
		// the same F6 approach already applied to dimOverworldLike/dimPlanet earlier in this file.
		Block tBlock = state(aWorld, new BlockPos(aX, aY, aZ)).getBlock(), tStone = (dimKey(aWorld) == Level.NETHER ? Blocks.NETHERRACK : Blocks.STONE);

		if (tBlock == NB || bedrock(tBlock)) {
			for (byte tSide : ALL_SIDES_BUT_BOTTOM) for (int i = 1; i < 7; i++) {
				BlockPos tRBPos = new BlockPos(aX+OFFX[tSide]*i, aY+OFFY[tSide]*i, aZ+OFFZ[tSide]*i);
				tBlock = state(aWorld, tRBPos).getBlock(); // used to be aWorld.getBlock(x,y,z)
				if (tBlock != NB && tBlock != tStone && !bedrock(tBlock)) {
					int tMetaData = meta(aWorld, tRBPos.getX(), tRBPos.getY(), tRBPos.getZ()); // used to be WD.meta(aWorld, x,y,z)
					if (BlocksGT.stoneToNormalOres.containsKey(new ItemStackContainer(tBlock, 1, tMetaData))) {
						return set(aWorld, aX, aY, aZ, tBlock, tMetaData, 0, F); // used to be aWorld.setBlock(x,y,z,block,meta,0) — routed through the center set(...)
					}
				}
			}
			return set(aWorld, aX, aY, aZ, tStone, 0, 0, F); // used to be aWorld.setBlock(x,y,z,tStone,0,0) — routed through the center set(...)
		}
		return F;
	}
	
	public static List<BlockPos> line(final Vec3 aStart, final Vec3 aEnd) {
		List<BlockPos> rList = new ArrayListNoNulls<>();
		if (Double.isNaN(aStart.x) || Double.isNaN(aStart.y) || Double.isNaN(aStart.z) || Double.isNaN(aEnd.x) || Double.isNaN(aEnd.y) || Double.isNaN(aEnd.z)) return rList;
		// F-vec: neo Vec3 is immutable (fields x/y/z final) — 1.7.10 mutated tPoint.xCoord component-wise;
		// reproduce by reassigning tPoint = new Vec3(...) (see the three branches below), behavior 1:1.
		Vec3 tPoint = new Vec3(aStart.x, aStart.y, aStart.z);
		
		int sx = UT.Code.roundDown(tPoint.x);
		int sy = UT.Code.roundDown(tPoint.y);
		int sz = UT.Code.roundDown(tPoint.z);
		int ex = UT.Code.roundDown(aEnd.x);
		int ey = UT.Code.roundDown(aEnd.y);
		int ez = UT.Code.roundDown(aEnd.z);
		
		rList.add(new BlockPos(sx, sy, sz));
		
		int maxAttempts = 2000; // Just to prevent accidental infinite loops
		
		while (maxAttempts-- >= 0) {
			if (Double.isNaN(tPoint.x) || Double.isNaN(tPoint.y) || Double.isNaN(tPoint.z)) return rList;
			if (sx == ex && sy == ey && sz == ez) return rList;
			
			boolean performx = true;
			boolean performy = true;
			boolean performz = true;
			
			double nx = 999.0D;
			double ny = 999.0D;
			double nz = 999.0D;
			
			double ndx = 999.0D;
			double ndy = 999.0D;
			double ndz = 999.0D;
			
			double distx = aEnd.x - tPoint.x;
			double disty = aEnd.y - tPoint.y;
			double distz = aEnd.z - tPoint.z;
			
			if (ex > sx) {
				nx = (double) sx + 1.0D;
			} else if (ex < sx) {
				nx = (double) sx + 0.0D;
			} else {
				performx = false;
			}
			
			if (ey > sy) {
				ny = (double) sy + 1.0D;
			} else if (ey < sy) {
				ny = (double) sy + 0.0D;
			} else {
				performy = false;
			}
			
			if (ez > sz) {
				nz = (double) sz + 1.0D;
			} else if (ez < sz) {
				nz = (double) sz + 0.0D;
			} else {
				performz = false;
			}
			
			if (performx) {
				ndx = (nx - tPoint.x) / distx;
			}
			
			if (performy) {
				ndy = (ny - tPoint.y) / disty;
			}
			
			if (performz) {
				ndz = (nz - tPoint.z) / distz;
			}
			
			byte whereTo;
			
			if (ndx < ndy && ndx < ndz) {
				if (ex > sx) whereTo = 4;
				else whereTo = 5;
				
				tPoint = new Vec3(nx, tPoint.y + disty * ndx, tPoint.z + distz * ndx);
			} else if (ndy < ndz) {
				if (ey > sy) whereTo = 0;
				else whereTo = 1;
				
				tPoint = new Vec3(tPoint.x + distx * ndy, ny, tPoint.z + distz * ndy);
			} else {
				if (ez > sz) whereTo = 2;
				else whereTo = 3;
				
				tPoint = new Vec3(tPoint.x + distx * ndz, tPoint.y + disty * ndz, nz);
			}
			
			sx = UT.Code.roundDown(tPoint.x);
			sy = UT.Code.roundDown(tPoint.y);
			sz = UT.Code.roundDown(tPoint.z);
			
			if (whereTo == 5) --sx;
			if (whereTo == 1) --sy;
			if (whereTo == 3) --sz;
			
			rList.add(new BlockPos(sx, sy, sz));
		}
		return rList;
	}
	
	public static long scan(ArrayList<String> aList, Player aPlayer, Level aWorld, int aScanLevel, int aX, int aY, int aZ, byte aSide, float aClickX, float aClickY, float aClickZ) {
		if (aList == null) return 0;
		
		ArrayList<String> rList = new ArrayListNoNulls<>();
		long rEUAmount = 0;
		
		Block aBlock = state(aWorld, new BlockPos(aX, aY, aZ)).getBlock(); // used to be aWorld.getBlock(x,y,z)
		byte aMeta = meta(aWorld, aX, aY, aZ); // used to be (byte)WD.meta(aWorld, x,y,z)
		BlockEntity aTileEntity = te(aWorld, aX, aY, aZ, T);
		
		rList.add("--- X: " + aX + " Y: " + aY + " Z: " + aZ + " ---");
		try {
			// F-container: 1.7.10 TileEntity could be IWorldNameable.getInventoryName() (String). neo BlockEntity
		// is not a Menu (instanceof AbstractContainerMenu is impossible) — a custom name comes from Nameable.getName():Component
		// (Nameable.java:7), .getString() -> String for stringValid. Debug scan, behavior 1:1.
		rList.add("Name: " + (aTileEntity instanceof net.minecraft.world.Nameable tNameable && Code.stringValid(tNameable.getName().getString()) ? tNameable.getName().getString() : aBlock.getDescriptionId()) + "  MetaData: " + aMeta);
			rList.add("Registry: " + ST.regName(aBlock));
			if (aScanLevel >= 10) {
				rList.add("Block Class: " + aBlock.getClass());
				if (aTileEntity != null) rList.add("TileEntity Class: " + aTileEntity.getClass());
			}
			// used to be getExplosionResistance(Entity,World,x,y,z,eX,eY,eZ) -> no direct equivalent without a real Explosion object
			// (IBlockExtension.getExplosionResistance(BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]
			// requires an Explosion, which debug-scan does not have); route to Block.getExplosionResistance() [Block.java:453] -
			// the same fallback that the location-sensitive default itself uses when there is no override.
			float tResistance = aBlock.getExplosionResistance();
			rList.add("Hardness: " + WD.hardness(aBlock, aWorld, aX, aY, aZ) + " - " + LH.getToolTipBlastResistance(aBlock, tResistance));
			// F-tool: getHarvestLevel/getHarvestTool(int) — GT6 methods on BlockBase (Forge hooks on vanilla Block
			// are removed). Debug scan of an arbitrary block: guard instanceof, vanilla -> 0/"" (no GT6 tier).
			int tHarvestLevel = aBlock instanceof BlockBase ? ((BlockBase)aBlock).getHarvestLevel(aMeta) : 0;
			String tHarvestTool = aBlock instanceof BlockBase ? ((BlockBase)aBlock).getHarvestTool(aMeta) : "";
			rList.add(tHarvestLevel == 0 && WD.getMaterial(aBlock).isAdventureModeExempt() ? LH.tt("Hand-Harvestable, but ") + (Code.stringValid(tHarvestTool)?Code.capitalise(tHarvestTool):LH.tt("None")) + LH.tt(" is faster") : LH.tt("Tool to Harvest: ") + (Code.stringValid(tHarvestTool)?Code.capitalise(tHarvestTool):LH.tt("None")) + " (" + tHarvestLevel + ")");
			// F-block: Forge Block.isBeaconBase(world,x,y,z,bx,by,bz) was removed -> neo tag BlockTags.BEACON_BASE_BLOCKS
			// (BlockTags.java:115), checked on the state.
			if (state(aWorld, new BlockPos(aX, aY, aZ)).is(net.minecraft.tags.BlockTags.BEACON_BASE_BLOCKS)) rList.add("Is usable for Beacon Pyramids");
			if (MD.GC.mLoaded && aBlock instanceof IPartialSealableBlock) rList.add(((IPartialSealableBlock)aBlock).isSealed(aWorld, aX, aY, aZ, FORGE_DIR[aSide ^ 1]) ? "Is Sealable on this Side" : "Is not Sealable on this Side");
		} catch(Throwable e) {e.printStackTrace(ERR);}
		if (aTileEntity != null) {
			try {if (aTileEntity instanceof ITileEntityWeight && ((ITileEntityWeight)aTileEntity).getWeightValue(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Weight: " + ((ITileEntityWeight)aTileEntity).getWeightValue(aSide) + " kg");
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityTemperature && ((ITileEntityTemperature)aTileEntity).getTemperatureMax(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Temperature: " + ((ITileEntityTemperature)aTileEntity).getTemperatureValue(aSide) + " / " + ((ITileEntityTemperature)aTileEntity).getTemperatureMax(aSide) + " K");
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityGibbl && ((ITileEntityGibbl)aTileEntity).getGibblMax(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Pressure: " + ((ITileEntityGibbl)aTileEntity).getGibblValue(aSide) + " / " + ((ITileEntityGibbl)aTileEntity).getGibblMax(aSide) + " Gibbl");
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityProgress && ((ITileEntityProgress)aTileEntity).getProgressMax(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Progress: " + ((ITileEntityProgress)aTileEntity).getProgressValue(aSide) + " / " + ((ITileEntityProgress)aTileEntity).getProgressMax(aSide));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			
			
			String rState = "";
			try {if (aTileEntity instanceof ITileEntitySwitchableOnOff) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("State: " + (((ITileEntitySwitchableOnOff)aTileEntity).getStateOnOff()?"ON":"OFF"));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntitySwitchableMode) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Mode: " + (((ITileEntitySwitchableMode)aTileEntity).getStateMode()));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityRunningSuccessfully) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningSuccessfully()?"Successfully":((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningActively()?"Actively":((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningPassively()?"Passively":((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			} else if (aTileEntity instanceof ITileEntityRunningActively) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningActively)aTileEntity).getStateRunningActively()?"Actively":((ITileEntityRunningActively)aTileEntity).getStateRunningPassively()?"Passively":((ITileEntityRunningActively)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			} else if (aTileEntity instanceof ITileEntityRunningPassively) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningPassively)aTileEntity).getStateRunningPassively()?"Passively":((ITileEntityRunningPassively)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			} else if (aTileEntity instanceof ITileEntityRunningPossible) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningPossible)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			if (Code.stringValid(rState)) rList.add(rState);
			
			
			try {if (aTileEntity instanceof ITileEntityEnergy) {
				rEUAmount+=V[3];
				for (TagData tEnergyType : ((ITileEntityEnergy)aTileEntity).getEnergyTypes(aSide)) {
					rList.add("Input: " + ((ITileEntityEnergy)aTileEntity).getEnergySizeInputMin(tEnergyType, aSide) + " to " + ((ITileEntityEnergy)aTileEntity).getEnergySizeInputMax(tEnergyType, aSide) + tEnergyType.getLocalisedNameShort());
					rList.add("Output: " + ((ITileEntityEnergy)aTileEntity).getEnergySizeOutputMin(tEnergyType, aSide) + " to " + ((ITileEntityEnergy)aTileEntity).getEnergySizeOutputMax(tEnergyType, aSide) + tEnergyType.getLocalisedNameShort());
				}
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityEnergyDataCapacitor) {
				rEUAmount+=V[3];
				for (TagData tEnergyType : ((ITileEntityEnergyDataCapacitor)aTileEntity).getEnergyCapacitorTypes(aSide)) {
					rList.add("Stored: " + ((ITileEntityEnergyDataCapacitor)aTileEntity).getEnergyStored(tEnergyType, aSide) + " of " + ((ITileEntityEnergyDataCapacitor)aTileEntity).getEnergyCapacity(tEnergyType, aSide) + tEnergyType.getLocalisedNameShort());
				}
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			
			
			try {if (aTileEntity instanceof IFluidHandler) {
				rEUAmount+=V[3];
				// F5: 1.7.10 IFluidHandler.getTankInfo(ForgeDirection) was removed from neo — the side to a GT6 TE is carried by
				// the seam CENTER FL.getTankInfo(handler, side) (FL.java:944), the same one all other
				// callers use (sensors, BasicMachine:705). A manual sideless walk of getTanks/getFluidInTank here
				// was BOTH a duplicate of the center AND a loss of the side (the scan showed tanks of "any side" instead of the visible one).
				// F15: an empty tank -> null for FL.name (1:1 with the original's fluid==null->"").
				gregapi.fluid.FluidTankInfo[] tTanks = FL.getTankInfo((IFluidHandler)aTileEntity, aSide);
				if (tTanks != null) for (byte i = 0; i < tTanks.length; i++) {
					rList.add("Tank " + i + ": " + (tTanks[i].fluid==null||tTanks[i].fluid.isEmpty()?0:tTanks[i].fluid.getAmount()) + " / " + tTanks[i].capacity + " " + FL.name(tTanks[i].fluid==null||tTanks[i].fluid.isEmpty()?null:tTanks[i].fluid, T));
				}
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			
			if (!(aTileEntity instanceof ITileEntity)) {
				try {if (aTileEntity instanceof ic2.api.reactor.IReactorChamber) {
					rEUAmount+=V[4];
					aTileEntity = (BlockEntity)(((ic2.api.reactor.IReactorChamber)aTileEntity).getReactor());
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.reactor.IReactor) {
					rEUAmount+=V[4];
					rList.add( "Heat: " + ((ic2.api.reactor.IReactor)aTileEntity).getHeat() + "/" + ((ic2.api.reactor.IReactor)aTileEntity).getMaxHeat()
							+ "  HEM: " + ((ic2.api.reactor.IReactor)aTileEntity).getHeatEffectModifier() + "  Base IC2-EU Output: " + ((ic2.api.reactor.IReactor)aTileEntity).getReactorEUEnergyOutput());
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.tile.IWrenchable) {
					rEUAmount+=V[3];
					rList.add("Facing: " + ((ic2.api.tile.IWrenchable)aTileEntity).getFacing() + " / IC2 Wrench Drop Chance: " + (((ic2.api.tile.IWrenchable)aTileEntity).wrenchCanRemove(aPlayer)?(((ic2.api.tile.IWrenchable)aTileEntity).getWrenchDropRate()*100):0) + "%");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.energy.tile.IEnergySink) {
					rEUAmount+=V[3];
					rList.add("Demanded Energy: " + ((ic2.api.energy.tile.IEnergySink)aTileEntity).getDemandedEnergy() + " IC2-EU");
					rList.add("Max Safe Input: " + V[((ic2.api.energy.tile.IEnergySink)aTileEntity).getSinkTier()] + " IC2-EU/t");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.energy.tile.IEnergySource) {
					rEUAmount+=V[3];
					rList.add("Max Energy Output: " + V[((ic2.api.energy.tile.IEnergySource)aTileEntity).getSourceTier()] + " IC2-EU/t");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.energy.tile.IEnergyConductor) {
					rEUAmount+=V[3];
					rList.add("Conduction Loss: " + ((ic2.api.energy.tile.IEnergyConductor)aTileEntity).getConductionLoss() + " IC2-EU/m");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.tile.IEnergyStorage) {
					rEUAmount+=V[3];
					rList.add("Contained Energy: " + ((ic2.api.tile.IEnergyStorage)aTileEntity).getStored() + " of " + ((ic2.api.tile.IEnergyStorage)aTileEntity).getCapacity() + " IC2-EU");
					rList.add(((ic2.api.tile.IEnergyStorage)aTileEntity).isTeleporterCompatible(FORGE_DIR[aSide])?"Teleporter Compatible":"Not Teleporter Compatible");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
			}
		}
		try {if (aBlock instanceof IBlockDebugable) {
			rEUAmount+=V[3];
			ArrayList<String> temp = ((IBlockDebugable)aBlock).getDebugInfo(aPlayer, aX, aY, aZ, aScanLevel);
			if (temp != null) rList.addAll(temp);
		}} catch(Throwable e) {e.printStackTrace(ERR);}
		
		BlockScanningEvent tEvent = new BlockScanningEvent(aWorld, aPlayer, aX, aY, aZ, aSide, aScanLevel, aBlock, aTileEntity, rList, aClickX, aClickY, aClickZ);
		tEvent.mEUCost = rEUAmount;
		MinecraftForge.EVENT_BUS.post(tEvent);
		if (!tEvent.isCanceled()) aList.addAll(rList);
		return tEvent.mEUCost;
	}
}
