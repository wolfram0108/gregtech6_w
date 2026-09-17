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

import java.util.List;
import java.util.OptionalLong;
import java.util.Random;
import java.util.function.Supplier;

import gregapi.util.ST;
import gregapi.util.WD;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

// F6, DummyWorld Level-redesign: 1.7.10 `World` (ctor `(ISaveHandler,String,WorldProvider,WorldSettings,Profiler)`,
// Level needs a concrete constructor and its own full set of abstract methods, same on 1.20.1 as on newer versions.
// This stays a minimal Level subclass: abstract methods get dummies, GT6's own points map 1:1 onto engine equivalents.
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

	// Level.random is a private final RandomSource with no setter, unlike 1.7.10's public World.rand field.
	// mRandom/GT_IteratorRandom stay as unused fields: nothing outside this file reads them, so no behavior is lost.
	public GT_IteratorRandom mRandom = new GT_IteratorRandom();
	public ItemStack mLastSetBlock = null;

	// 1.7.10 had no such constant at all; this reuses GT6's own central WD.waterLevel() instead of inventing a new one.
	private static int mSeaLevel() {return WD.waterLevel();}

	/** Level requires a Holder with a real registry key; a raw Holder.direct(...) has none, so construction crashed on startup.
	 *  Taking the vanilla OVERWORLD holder from the live server registry avoids that, matching 1.7.10's own dummy provider. */
	private static Holder<DimensionType> overworldType(RegistryAccess aRegistryAccess) {
		return aRegistryAccess.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
	}

	// WritableLevelData on 1.20.1 takes per-coordinate spawn setters, not a single RespawnData record like newer versions.
	private static final class DummyLevelData implements WritableLevelData {
		private int mX = 0, mY = 64, mZ = 0;
		private float mAngle = 0;
		private final GameRules mGameRules = new GameRules();
		@Override public void setXSpawn(int aX) {mX = aX;}
		@Override public void setYSpawn(int aY) {mY = aY;}
		@Override public void setZSpawn(int aZ) {mZ = aZ;}
		@Override public void setSpawnAngle(float aAngle) {mAngle = aAngle;}
		@Override public int getXSpawn() {return mX;}
		@Override public int getYSpawn() {return mY;}
		@Override public int getZSpawn() {return mZ;}
		@Override public float getSpawnAngle() {return mAngle;}
		@Override public long getGameTime() {return 0;}
		@Override public long getDayTime() {return 0;}
		@Override public boolean isThundering() {return F;}
		@Override public boolean isRaining() {return F;}
		@Override public void setRaining(boolean aRaining) {/*Do nothing*/}
		@Override public boolean isHardcore() {return F;}
		@Override public GameRules getGameRules() {return mGameRules;}
		@Override public Difficulty getDifficulty() {return Difficulty.NORMAL;}
		@Override public boolean isDifficultyLocked() {return F;}
	}

	// The old delegating constructor took four 1.7.10-only types that don't exist on 1.20.1, so it can't be reproduced 1:1;
	// the remaining no-arg constructor builds the Level contract's arguments directly instead.
	public DummyWorld() {this(RegistryAccess.EMPTY);}

	/** The only place CS.DW gets built: makes the dummy world once the registry loads, a no-op if it already exists.
	 *  Called from server start, where the registry is guaranteed full; a failure here logs its cause, not a silent null. */
	public static synchronized void ensure(RegistryAccess aRegistryAccess) {
		if (gregapi.data.CS.DW != null || aRegistryAccess == null) return;
		try {
			gregapi.data.CS.DW = new DummyWorld(aRegistryAccess);
		} catch (Throwable e) {
			gregapi.data.CS.ERR.println("GT6: dummy-мир не создан — проверка совпадения рецептов пойдёт без мира (" + e + ").");
			e.printStackTrace(gregapi.data.CS.ERR);
		}
	}

	/** The registry comes from the caller (the live server), since a bare RegistryAccess.EMPTY crashes on the biome registry.
	 *  The profiler is the engine's own InactiveProfiler.INSTANCE, the same disabled impl vanilla uses for the same cases. */
	public DummyWorld(RegistryAccess aRegistryAccess) {
		super(
			new DummyLevelData(),
			ResourceKey.create(Registries.DIMENSION, new ResourceLocation("minecraft", "dummy_dimension")),
			aRegistryAccess,
			overworldType(aRegistryAccess),
			(Supplier<ProfilerFiller>)() -> InactiveProfiler.INSTANCE,
			F,
			F,
			0L,
			0
		);
	}

	// LevelAccessor.getChunkSource() replaces createChunkProvider(); the 'dummy has no provider' behavior is unchanged.
	@Override public ChunkSource getChunkSource() {
		return null;
	}

	// Level.getEntity(int) is the abstract replacement for the old getEntityByID(int); same null-returning body.
	// (`Level.java:712`).
	@Override public Entity getEntity(int aEntityID) {
		return null;
	}

	// Block meta is gone; BlockPos+BlockState is the single addressing unit now, reshaping the old setBlock call around it.
	// mLastSetBlock uses meta 0, since the central ST.make(Block,long,long) has no channel for an arbitrary BlockState meta.
	@Override public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags, int aUpdateLimit) {
		mLastSetBlock = ST.make(aState.getBlock(), 1, 0);
		return T;
	}

	// 1.7.10's plains/.ocean were static singletons; Biome is now a dynamic registry object needing a live RegistryAccess.
	// The area check itself is kept exactly as mInArea(...), with both branches still spelled out rather than collapsed.
	private boolean mInArea(int aX, int aZ) {
		return aX >= 16 && aZ >= 16 && aX < 32 && aZ < 32;
	}

	@Override public Holder<Biome> getUncachedNoiseBiome(int aQuartX, int aQuartY, int aQuartZ) {
		return getBiome(new BlockPos(QuartPos.toBlock(aQuartX), QuartPos.toBlock(aQuartY), QuartPos.toBlock(aQuartZ)));
	}

	@Override public Holder<Biome> getBiome(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return null; // The plains area (replacing BiomeGenBase.plains).
		return null; // Outside the area, ocean (replacing BiomeGenBase.ocean).
	}

	@Override public int getSeaLevel() {
		return mSeaLevel();
	}

	// The engine's own sky-check runs through the lighting engine, which this dummy has none of (getChunkSource()==null),
	// so it's overridden directly here with the same coordinate check.
	@Override public boolean canSeeSky(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() > 64;
		return T;
	}

	// BlockGetter.getBlockState(BlockPos) is the block-read entry point, overridden here to bypass getChunkSource()==null,
	// keeping the same area/height check as before.
	@Override public BlockState getBlockState(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() == 64 ? Blocks.GRASS_BLOCK.defaultBlockState() : NB.defaultBlockState();
		return NB.defaultBlockState();
	}

	// Block meta is gone, so there's no separate override point for it anymore; it's now part of BlockState above.

	// A safe 'empty world' stub, not a GT6 behavior point: 1.7.10 never separated fluid from block, and this avoids
	// the default Level.getFluidState touching getChunkSource()==null.
	@Override public FluidState getFluidState(BlockPos aPos) {
		return Fluids.EMPTY.defaultFluidState();
	}

	// Everything below is administrative Level/LevelAccessor overrides with no 1.7.10 counterpart, never called on CS.DW.
	// The only consumer passes CS.DW into recipe.matches(...); each override gets a safe no-arg/EMPTY/null/0/false dummy.

	// LevelAccessor.gameEvent takes a raw GameEvent on 1.20.1, not a Holder<GameEvent>.
	@Override public void gameEvent(GameEvent aGameEvent, Vec3 aPosition, GameEvent.Context aContext) {/*Do nothing*/}

	// LevelAccessor.levelEvent takes a Player on 1.20.1, not an Entity.
	@Override public void levelEvent(Player aPlayer, int aType, BlockPos aPos, int aData) {/*Do nothing*/}

	// There's no 1.7.10 analog; a separate tick-scheduler accessor came later.
	// This reuses the engine's own BlackholeTickAccess stub, the one vanilla uses for 'no real tick storage' too.
	@Override public LevelTickAccess<Block> getBlockTicks() {
		return BlackholeTickAccess.<Block>emptyLevelList();
	}

	@Override public LevelTickAccess<Fluid> getFluidTicks() {
		return BlackholeTickAccess.<Fluid>emptyLevelList();
	}

	@Override public void sendBlockUpdated(BlockPos aPos, BlockState aOld, BlockState aCurrent, int aUpdateFlags) {/*Do nothing*/}

	// Both overloads take a Player on 1.20.1; on newer engine versions the first parameter was Entity.
	@Override public void playSeededSound(Player aExcept, double aX, double aY, double aZ, Holder<SoundEvent> aSound, SoundSource aSource, float aVolume, float aPitch, long aSeed) {/*Do nothing*/}

	@Override public void playSeededSound(Player aExcept, Entity aSourceEntity, Holder<SoundEvent> aSound, SoundSource aSource, float aVolume, float aPitch, long aSeed) {/*Do nothing*/}

	@Override public String gatherChunkSourceStats() {
		return "";
	}

	// Maps are addressed by a String name on 1.20.1, not a MapId.
	@Override public MapItemSavedData getMapData(String aId) {
		return null;
	}

	@Override public void setMapData(String aId, MapItemSavedData aData) {/*Do nothing*/}

	@Override public int getFreeMapId() {
		return 0;
	}

	@Override public void destroyBlockProgress(int aId, BlockPos aPos, int aProgress) {/*Do nothing*/}

	@Override public Scoreboard getScoreboard() {
		return new Scoreboard();
	}

	// On 1.20.1 the world exposes RecipeManager directly; newer engine versions ask through RecipeAccess instead.
	@Override public RecipeManager getRecipeManager() {
		return new RecipeManager();
	}

	// EntityGetter.players() has a default on newer engine versions but stays abstract on 1.20.1; this dummy has none.
	@Override public List<? extends Player> players() {
		return List.of();
	}

	// BlockAndTintGetter.getShade is abstract on Level in 1.20.1. This dummy is never rendered, so it returns 'no shading',
	// the same as vanilla's own shade==false path.
	@Override public float getShade(net.minecraft.core.Direction aDirection, boolean aShade) {
		return 1.0F;
	}

	// LevelReader.enabledFeatures is abstract on 1.20.1, unlike newer engine versions where it isn't part of the contract.
	// This dummy carries no datapack state, so it returns the vanilla default feature set.
	@Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() {
		return net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS;
	}

	@Override protected LevelEntityGetter<Entity> getEntities() {
		return null;
	}
}
