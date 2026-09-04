/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.dummies;

import static gregapi.data.CS.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import gregapi.util.ST;
import gregapi.util.WD;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

// F6, DummyWorld Level-redesign — CLOSED (ledger `DEFERRED-LEDGER.md:431-435`): the engine swapped its whole world
// model — the 1.7.10 `World` (ctor `(ISaveHandler,String,WorldProvider,WorldSettings,Profiler)`, `getBlock(x,y,z)`/
// `setBlock(x,y,z,Block,meta,flags)`/`getBiomeGenForCoords(x,z)`) is removed from all 3 reference roots entirely;
// neo's `Level` (`neo-decompiled/net/minecraft/world/level/Level.java:139-160`) has a concrete ctor
// `(WritableLevelData,ResourceKey<Level>,RegistryAccess,Holder<DimensionType>,boolean,boolean,long,int)`
// and a completely different set of abstract methods (see below). Rewritten as a minimal concrete Level subclass:
// ALL abstract methods of Level/LevelAccessor/LevelReader/EntityGetter/CollisionGetter are closed with dummy stubs,
// GT6's behavioural points (getBlock/setBlock/getBiomeGenForCoords/canBlockSeeTheSky) are carried over 1:1 to their
// neo equivalents (see the note at each method).
public class DummyWorld extends Level {
	public class GT_IteratorRandom extends Random {
		private static final long serialVersionUID = 1L;

		public int mIterationStep = Integer.MAX_VALUE;

		@Override public int nextInt(int aParameter) {
			if (mIterationStep == 0 || mIterationStep > aParameter) {
				mIterationStep = aParameter;
			}
			return --mIterationStep;
		}
	}

	// F6 dummy-world (fake world, random hookup not required): the original did `rand = mRandom;` (World.rand — a
	// public field of type java.util.Random). In neo `Level.random` is `private final RandomSource` (an interface
	// with a COMPLETELY different contract: fork()/forkPositional()/nextInt()/… — `neo-decompiled/net/minecraft/util/
	// RandomSource.java:35-57`), and no setter exists in any of the 3 reference roots. GT_IteratorRandom/mRandom are
	// kept structurally (the field+class are not removed), but are NOT wired to the engine's live RNG — a RandomSource
	// adapter would be a NEW entity that GT6 doesn't have (rule R2), and a grep over the whole GT6 tree (`gregtech6\src`)
	// confirms: neither `GT_IteratorRandom` nor `mRandom` is read anywhere outside this file — only
	// `CS.DW` (this instance) is passed into `recipe.matches(aCrafting, CS.DW)` (`gregapi/util/CR.java:524,562,573,681`),
	// which never touches `world.rand`. Deferred; there is no real behavioural loss.
	public GT_IteratorRandom mRandom = new GT_IteratorRandom();
	public ItemStack mLastSetBlock = null;

	// previously: no World.getSeaLevel()-like constant existed at all in 1.7.10 (WorldProvider.getAverageGroundLevel
	// was used inline); neo added abstract `LevelReader.getSeaLevel()` (LevelReader.java:66) — GT6's central
	// water level is already factored into `WD.waterLevel()` (`gregapi/util/WD.java:579-580`,
	// overworld default 62) — reuse the centre instead of inventing a new constant.
	private static int mSeaLevel() {return WD.waterLevel();}

	// Minimal self-contained DimensionType: fields are assembled from static constants WITHOUT RegistryAccess
	// (the infiniburn tag is lazy, timelines/defaultClock are empty) — modelled after the real overworld registration
	// (`neo-decompiled/net/minecraft/data/worldgen/DimensionTypes.java:45-63`), but without touching registries
	// (which the offline dummy doesn't have).
	private static final DimensionType mDimensionType = new DimensionType(
		F, T, F, F, 1.0D,
		-64, 384, 384,
		BlockTags.INFINIBURN_OVERWORLD,
		0.0F,
		new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
		DimensionType.Skybox.OVERWORLD,
		CardinalLighting.Type.DEFAULT,
		EnvironmentAttributeMap.EMPTY,
		HolderSet.empty(),
		Optional.empty()
	);

	private static final class DummyLevelData implements WritableLevelData {
		private LevelData.RespawnData mRespawn = LevelData.RespawnData.DEFAULT;
		@Override public void setSpawn(LevelData.RespawnData aRespawnData) {mRespawn = aRespawnData;}
		@Override public LevelData.RespawnData getRespawnData() {return mRespawn;}
		@Override public long getGameTime() {return 0;}
		@Override public boolean isHardcore() {return F;}
		@Override public Difficulty getDifficulty() {return Difficulty.NORMAL;}
		@Override public boolean isDifficultyLocked() {return F;}
	}

	// previously: DummyWorld(ISaveHandler,String,WorldProvider,WorldSettings,Profiler) + a no-arg DummyWorld()
	// delegating into it with anonymous ISaveHandler/WorldProvider stubs — all 4 parameter types are removed from
	// neo entirely (not found in any of the 3 reference roots), so a delegating ctor is physically impossible 1:1.
	// The one remaining no-arg ctor `DummyWorld()` (required, called by `GT_API.java:254 new DummyWorld()`) builds
	// the arguments of the new `Level` contract directly.
	public DummyWorld() {this(RegistryAccess.EMPTY);}

	/**
	 * The ONLY place {@code CS.DW} is created: builds the world once the registry is already loaded, and stays
	 * silent if it is already built. Called from server startup, where {@code MinecraftServer.registryAccess()}
	 * is populated. A failure is not silent: if the world still fails to build, the log gets the reason,
	 * not an empty reference.
	 */
	public static synchronized void ensure(RegistryAccess aRegistryAccess) {
		if (gregapi.data.CS.DW != null || aRegistryAccess == null) return;
		try {
			gregapi.data.CS.DW = new DummyWorld(aRegistryAccess);
		} catch (Throwable e) {
			gregapi.data.CS.ERR.println("GT6: dummy world not created — recipe matching will run without a world (" + e + ").");
			e.printStackTrace(gregapi.data.CS.ERR);
		}
	}

	/**
	 * THE REGISTRY IS MANDATORY AS OF 26.1.2. Previously a hardcoded {@code RegistryAccess.EMPTY} stood here, and the
	 * world did not build AT ALL: {@code Level.<init>} (Level.java:158) calls {@code PalettedContainerFactory.create}
	 * (PalettedContainerFactory.java:25), which in turn calls {@code lookupOrThrow(Registries.BIOME)}, and an empty
	 * registry has none. The mod caught this and printed «DUMMY WORLD COULD NOT BE CREATED» on EVERY startup, leaving
	 * {@code CS.DW} null; crafting only kept working because vanilla recipes don't read the world in
	 * {@code matches}. So the registry now comes from outside — from the server, once it is already loaded.
	 */
	public DummyWorld(RegistryAccess aRegistryAccess) {
		super(
			new DummyLevelData(),
			ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("dummy_dimension")),
			aRegistryAccess,
			Holder.direct(mDimensionType),
			F,
			F,
			0L,
			0
		);
	}

	// previously: protected IChunkProvider createChunkProvider() {return null;} — neo renamed this to abstract
	// `LevelAccessor.getChunkSource()` (`neo-decompiled/net/minecraft/world/level/LevelAccessor.java:51`);
	// the same "the dummy has no chunk provider" semantics is kept verbatim.
	@Override public ChunkSource getChunkSource() {
		return null;
	}

	// previously: public Entity getEntityByID(int aEntityID) {return null;} — neo's abstract `Level.getEntity(int)`
	// (Level.java:850).
	@Override public Entity getEntity(int aEntityID) {
		return null;
	}

	// previously: public boolean setBlock(int aX,int aY,int aZ,Block aBlock,int aMeta,int aFlags) — F13
	// (`decisions/F13-block-position-meta.md`): the numeric block meta is removed entirely in neo, the single
	// addressing unit being `BlockPos`+`BlockState` (`LevelWriter.java:10`). We carry the same record into
	// mLastSetBlock; the meta parameter of ST.make(Block,long,long) gets 0 — there is no general "meta of an
	// arbitrary BlockState" channel in the centre (F13 §3: only `IBlockExtendedMetaData` exists, for GT6's OWN
	// blocks; here blockState comes with an arbitrary block from any caller).
	@Override public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags, int aUpdateLimit) {
		mLastSetBlock = ST.make(aState.getBlock(), 1, 0);
		return T;
	}

	// previously: public float getSunBrightnessFactor(float p_72967_1_) {return 1.0F;}
	// F6 dummy-world (0 external callers, the fake world needs no real sun): the override point
	// `World.getSunBrightnessFactor(float)` is removed from neo entirely — a grep over all 3 reference roots
	// (neo-decompiled/neoforge-decompiled/fml-decompiled) is empty, no replacement/rename exists. We don't invent
	// a neo method that doesn't exist (rule 1); not called from anywhere outside (same `CS.DW` grep as for mRandom
	// above), no functional loss.

	// previously: public Biome getBiomeGenForCoords(int aX,int aZ) {return (in area) ? plains : ocean;}
	// F6 dummy-world (fake world, a default biome is enough): the 1.7.10 `BiomeGenBase.plains`/`.ocean` were static
	// VM singletons (a direct field), while neo's `Biome` is an object of the dynamic datapack registry
	// `Registries.BIOME` (`neo-decompiled/net/minecraft/core/registries/Registries.java:258`), NOT in
	// `BuiltInRegistries` — unreachable without a live `RegistryAccess`, which the offline dummy doesn't have
	// (`RegistryAccess.EMPTY`, see the ctor). The area distinction (previously `aX>=16 && aZ>=16 && aX<32 && aZ<32`)
	// is kept verbatim in mInArea(...) and FULLY executed below (both branches are visible, not collapsed) — the two
	// branches simply can't return a real `Holder<Biome>` yet without a registry. Not called from outside (same
	// `CS.DW` grep as mRandom/sun-brightness).
	private boolean mInArea(int aX, int aZ) {
		return aX >= 16 && aZ >= 16 && aX < 32 && aZ < 32;
	}

	@Override public Holder<Biome> getBiome(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return null; // plains area (was BiomeGenBase.plains)
		return null; // outside the area — ocean (was BiomeGenBase.ocean)
	}

	@Override public Holder<Biome> getUncachedNoiseBiome(int aQuartX, int aQuartY, int aQuartZ) {
		return getBiome(new BlockPos(QuartPos.toBlock(aQuartX), QuartPos.toBlock(aQuartY), QuartPos.toBlock(aQuartZ)));
	}

	@Override public int getSeaLevel() {
		return mSeaLevel();
	}

	// previously: public int getFullBlockLightValue(int aX,int aY,int aZ) {return 10;}
	// F6 dummy-world (fake world, default light is enough): the `World.getFullBlockLightValue(x,y,z)` point is
	// removed from neo entirely — same status as getSunBrightnessFactor above (3-root grep empty, not called
	// from outside).

	// previously: public boolean canBlockSeeTheSky(int aX,int aY,int aZ) {return (in area) ? aY>64 : T;} — neo's
	// default method `BlockAndLightGetter.canSeeSky(BlockPos)` (`BlockAndLightGetter.java:17-19`) normally
	// computes through getBrightness/getLightEngine (which the dummy doesn't have, getChunkSource()==null) —
	// overridden directly with the same coordinate check, bypassing the engine's lighting engine (the same
	// technique the original applied to WorldChunkManager).
	@Override public boolean canSeeSky(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() > 64;
		return T;
	}

	// previously: public Block getBlock(int aX,int aY,int aZ) {return (in area && aY==64) ? Blocks.grass : NB; else NB;}
	// — neo's abstract block-read point `BlockGetter.getBlockState(BlockPos)` (`BlockGetter.java:32`,
	// F13 §2 table). Overridden directly (bypassing getChunkSource()==null), area/height are kept verbatim.
	@Override public BlockState getBlockState(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() == 64 ? Blocks.GRASS_BLOCK.defaultBlockState() : NB.defaultBlockState();
		return NB.defaultBlockState();
	}

	// previously: public int getBlockMetadata(int aX,int aY,int aZ) {return 0;} — the numeric block meta is
	// removed from neo entirely (F13 §1); there is no separate override point for it in neo's Level contract
	// anymore (meta is now part of the BlockState itself from getBlockState(...) above), nothing to carry over.

	// safe "empty world" stub (not a GT6 behavioural point — the original didn't distinguish fluid from block
	// separately in 1.7.10; kept here so as not to hit getChunkSource()==null through the default Level.getFluidState).
	@Override public FluidState getFluidState(BlockPos aPos) {
		return Fluids.EMPTY.defaultFluidState();
	}

	// --- below are purely administrative abstract methods of Level/LevelAccessor/LevelReader/EntityGetter/
	// CollisionGetter that have no counterpart in the 1.7.10 original (that contract simply didn't exist there)
	// and are never called on CS.DW anywhere in the mod's tree (the only consumer — `gregapi/util/CR.java` —
	// passes CS.DW only into `recipe.matches(...)`, which never touches these methods).
	// Safe dummy values: a ready-made public no-arg constructor/static EMPTY factory where one exists without
	// a registry, otherwise null/empty/0/false.

	@Override public void gameEvent(Holder<GameEvent> aGameEvent, Vec3 aPosition, GameEvent.Context aContext) {/*Do nothing*/}

	@Override public void levelEvent(Entity aSource, int aType, BlockPos aPos, int aData) {/*Do nothing*/}

	// no 1.7.10 counterpart (a tick scheduler as a separate accessor only appeared in neo); reuse the engine's
	// ready-made "black hole" stub (`neo-decompiled/net/minecraft/world/ticks/
	// BlackholeTickAccess.java:46-48`), which vanilla itself uses to close the same "no real tick storage" cases —
	// we don't invent a new abstraction (rule R2).
	@Override public LevelTickAccess<Block> getBlockTicks() {
		return BlackholeTickAccess.<Block>emptyLevelList();
	}

	@Override public LevelTickAccess<Fluid> getFluidTicks() {
		return BlackholeTickAccess.<Fluid>emptyLevelList();
	}

	@Override public void sendBlockUpdated(BlockPos aPos, BlockState aOld, BlockState aCurrent, int aUpdateFlags) {/*Do nothing*/}

	@Override public void playSeededSound(Entity aExcept, double aX, double aY, double aZ, Holder<SoundEvent> aSound, SoundSource aSource, float aVolume, float aPitch, long aSeed) {/*Do nothing*/}

	@Override public void playSeededSound(Entity aExcept, Entity aSourceEntity, Holder<SoundEvent> aSound, SoundSource aSource, float aVolume, float aPitch, long aSeed) {/*Do nothing*/}

	@Override public void explode(Entity aSource, DamageSource aDamageSource, ExplosionDamageCalculator aDamageCalculator, double aX, double aY, double aZ, float aRadius, boolean aFire, Level.ExplosionInteraction aInteractionType, ParticleOptions aSmallExplosionParticles, ParticleOptions aLargeExplosionParticles, WeightedList<ExplosionParticleInfo> aBlockParticles, Holder<SoundEvent> aExplosionSound) {/*Do nothing*/}

	@Override public String gatherChunkSourceStats() {
		return "";
	}

	@Override public void setRespawnData(LevelData.RespawnData aRespawnData) {/*Do nothing*/}

	@Override public LevelData.RespawnData getRespawnData() {
		return LevelData.RespawnData.DEFAULT;
	}

	@Override public TickRateManager tickRateManager() {
		return new TickRateManager();
	}

	@Override public MapItemSavedData getMapData(MapId aId) {
		return null;
	}

	@Override public void destroyBlockProgress(int aId, BlockPos aPos, int aProgress) {/*Do nothing*/}

	@Override public Scoreboard getScoreboard() {
		return new Scoreboard();
	}

	@Override public Collection<? extends net.neoforged.neoforge.entity.PartEntity<?>> dragonParts() {
		return List.of();
	}

	@Override protected LevelEntityGetter<Entity> getEntities() {
		return null;
	}

	@Override public RecipeAccess recipeAccess() {
		return null;
	}

	@Override public ClockManager clockManager() {
		return null;
	}

	@Override public EnvironmentAttributeSystem environmentAttributes() {
		return EnvironmentAttributeSystem.builder().build();
	}

	@Override public PotionBrewing potionBrewing() {
		return PotionBrewing.EMPTY;
	}

	@Override public FuelValues fuelValues() {
		return null;
	}

	@Override public FeatureFlagSet enabledFeatures() {
		return FeatureFlagSet.of();
	}

	@Override public List<? extends Player> players() {
		return List.of();
	}

	@Override public WorldBorder getWorldBorder() {
		return new WorldBorder();
	}
}
