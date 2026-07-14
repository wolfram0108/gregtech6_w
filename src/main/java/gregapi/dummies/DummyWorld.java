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

// F6, DummyWorld Level-redesign — ЗАКРЫТО (ledger `DEFERRED-LEDGER.md:431-435`): движок сменил модель мира целиком — 1.7.10 `World`
// (ctor `(ISaveHandler,String,WorldProvider,WorldSettings,Profiler)`, `getBlock(x,y,z)`/`setBlock(x,y,z,
// Block,meta,flags)`/`getBiomeGenForCoords(x,z)`) удалён из всех 3 корней референса целиком; neo `Level`
// (`neo-decompiled/net/minecraft/world/level/Level.java:139-160`) — конкретный ctor
// `(WritableLevelData,ResourceKey<Level>,RegistryAccess,Holder<DimensionType>,boolean,boolean,long,int)`
// и совсем другой набор abstract-методов (см. ниже). Переписано как минимальный конкретный Level-подкласс:
// ВСЕ abstract-методы Level/LevelAccessor/LevelReader/EntityGetter/CollisionGetter закрыты дамми-заглушками,
// GT6-поведенческие точки (getBlock/setBlock/getBiomeGenForCoords/canBlockSeeTheSky) перенесены 1:1 на их
// neo-эквиваленты (см. пометки при каждом методе).
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

	// PORT-TODO(F6, dummy-world-random-hookup): оригинал делал `rand = mRandom;` (World.rand — публичное
	// поле типа java.util.Random). В neo `Level.random` — `private final RandomSource` (интерфейс с СОВСЕМ
	// другим контрактом: fork()/forkPositional()/nextInt()/… — `neo-decompiled/net/minecraft/util/
	// RandomSource.java:35-57`), сеттера нет ни в одном из 3 корней. GT_IteratorRandom/mRandom сохранены
	// структурно (поле+класс не удалены), но НЕ подключены к живому RNG движка — адаптер RandomSource
	// был бы НОВОЙ сущностью, которой нет в GT6 (правило R2), а grep по всему дереву GT6 (`gregtech6\src`)
	// подтверждает: ни `GT_IteratorRandom`, ни `mRandom` нигде не читаются извне этого файла — только
	// `CS.DW` (сюда) передаётся в `recipe.matches(aCrafting, CS.DW)` (`gregapi/util/CR.java:524,562,573,681`),
	// который не трогает `world.rand`. Отложено, реальной потери поведения нет.
	public GT_IteratorRandom mRandom = new GT_IteratorRandom();
	public ItemStack mLastSetBlock = null;

	// было World.getSeaLevel()-подобной константы не было вовсе в 1.7.10 (WorldProvider.getAverageGroundLevel
	// использовался по месту); neo добавил abstract `LevelReader.getSeaLevel()` (LevelReader.java:66) —
	// центральный уровень воды GT6 уже вынесен в `WD.waterLevel()` (`gregapi/util/WD.java:579-580`,
	// дефолт оверворлда 62) — переиспользуем центр вместо изобретения новой константы.
	private static int mSeaLevel() {return WD.waterLevel();}

	// Минимальный самодостаточный DimensionType: поля собраны из статических констант БЕЗ RegistryAccess
	// (инфиниберн-тег ленивый, timelines/defaultClock пустые) — по образцу настоящей регистрации оверворлда
	// (`neo-decompiled/net/minecraft/data/worldgen/DimensionTypes.java:45-63`), но без обращения к реестрам
	// (которых у офлайн-дамми нет).
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

	// было DummyWorld(ISaveHandler,String,WorldProvider,WorldSettings,Profiler) + DummyWorld() делегирующий
	// в него анонимными ISaveHandler/WorldProvider-заглушками — все 4 типа параметров удалены из neo целиком
	// (не найдены ни в одном из 3 корней референса), делегирующий ctor физически невозможен 1:1. Единственный
	// оставшийся no-arg ctor `DummyWorld()` (обязателен, зовёт `GT_API.java:254 new DummyWorld()`) строит
	// аргументы нового `Level`-контракта напрямую.
	public DummyWorld() {
		super(
			new DummyLevelData(),
			ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("dummy_dimension")),
			RegistryAccess.EMPTY,
			Holder.direct(mDimensionType),
			F,
			F,
			0L,
			0
		);
	}

	// было protected IChunkProvider createChunkProvider() {return null;} — neo переименовал в abstract
	// `LevelAccessor.getChunkSource()` (`neo-decompiled/net/minecraft/world/level/LevelAccessor.java:51`);
	// та же семантика "нет чанк-провайдера у дамми" сохранена дословно.
	@Override public ChunkSource getChunkSource() {
		return null;
	}

	// было public Entity getEntityByID(int aEntityID) {return null;} — neo abstract `Level.getEntity(int)`
	// (Level.java:850).
	@Override public Entity getEntity(int aEntityID) {
		return null;
	}

	// было public boolean setBlock(int aX,int aY,int aZ,Block aBlock,int aMeta,int aFlags) — F13
	// (`decisions/F13-block-position-meta.md`): числовая мета блока в neo удалена целиком, единый узел
	// адресации — `BlockPos`+`BlockState` (`LevelWriter.java:10`). Переносим ту же запись в mLastSetBlock;
	// meta-параметр у ST.make(Block,long,long) получает 0 — общего канала "meta произвольного BlockState"
	// в центре нет (F13 §3: есть только `IBlockExtendedMetaData` для СВОИХ блоков, здесь blockState — с
	// произвольным блоком любого вызывающего).
	@Override public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags, int aUpdateLimit) {
		mLastSetBlock = ST.make(aState.getBlock(), 1, 0);
		return T;
	}

	// было public float getSunBrightnessFactor(float p_72967_1_) {return 1.0F;}
	// PORT-TODO(F6, dummy-world-sun-brightness): точка перекрытия `World.getSunBrightnessFactor(float)`
	// удалена из neo целиком — grep по всем 3 корням референса (neo-decompiled/neoforge-decompiled/
	// fml-decompiled) пуст, замены/переименования нет. Не выдумываем несуществующий neo-метод (правило 1);
	// не вызывается нигде извне (тот же grep `CS.DW`, что и у mRandom выше), функциональной потери нет.

	// было public Biome getBiomeGenForCoords(int aX,int aZ) {return (in area) ? plains : ocean;}
	// PORT-TODO(F6, dummy-world-biome): 1.7.10 `BiomeGenBase.plains`/`.ocean` были статическими VM-синглтонами
	// (прямое поле), а neo `Biome` — объект динамического датапак-реестра `Registries.BIOME`
	// (`neo-decompiled/net/minecraft/core/registries/Registries.java:258`), НЕ в `BuiltInRegistries` —
	// недостижим без живого `RegistryAccess`, которого у офлайн-дамми нет (`RegistryAccess.EMPTY`, см. ctor).
	// Область-различение (было `aX>=16 && aZ>=16 && aX<32 && aZ<32`) сохранена дословно в mInArea(...) и
	// СПОЛНА исполняется ниже (обе ветки видимы, не схлопнуты) — просто пока обе не могут вернуть настоящий
	// `Holder<Biome>` без реестра. Не вызывается извне (grep `CS.DW` как у mRandom/sun-brightness).
	private boolean mInArea(int aX, int aZ) {
		return aX >= 16 && aZ >= 16 && aX < 32 && aZ < 32;
	}

	@Override public Holder<Biome> getBiome(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return null; // область плейнс (было BiomeGenBase.plains)
		return null; // вне области — океан (было BiomeGenBase.ocean)
	}

	@Override public Holder<Biome> getUncachedNoiseBiome(int aQuartX, int aQuartY, int aQuartZ) {
		return getBiome(new BlockPos(QuartPos.toBlock(aQuartX), QuartPos.toBlock(aQuartY), QuartPos.toBlock(aQuartZ)));
	}

	@Override public int getSeaLevel() {
		return mSeaLevel();
	}

	// было public int getFullBlockLightValue(int aX,int aY,int aZ) {return 10;}
	// PORT-TODO(F6, dummy-world-full-light): точка `World.getFullBlockLightValue(x,y,z)` удалена из neo
	// целиком — тот же статус, что и getSunBrightnessFactor выше (grep 3 корней пуст, не вызывается извне).

	// было public boolean canBlockSeeTheSky(int aX,int aY,int aZ) {return (in area) ? aY>64 : T;} — neo
	// default-метод `BlockAndLightGetter.canSeeSky(BlockPos)` (`BlockAndLightGetter.java:17-19`), обычно
	// считает через getBrightness/getLightEngine (которого у дамми нет, getChunkSource()==null) — перекрыт
	// напрямую той же координатной проверкой, минуя движковый lighting-engine (тот же приём, что оригинал
	// применял к WorldChunkManager).
	@Override public boolean canSeeSky(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() > 64;
		return T;
	}

	// было public Block getBlock(int aX,int aY,int aZ) {return (in area && aY==64) ? Blocks.grass : NB; else NB;}
	// — neo abstract-точка чтения блока `BlockGetter.getBlockState(BlockPos)` (`BlockGetter.java:32`,
	// F13 §2 таблица). Перекрыт напрямую (минуя getChunkSource()==null), область/высота — дословно.
	@Override public BlockState getBlockState(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() == 64 ? Blocks.GRASS_BLOCK.defaultBlockState() : NB.defaultBlockState();
		return NB.defaultBlockState();
	}

	// было public int getBlockMetadata(int aX,int aY,int aZ) {return 0;} — числовая мета блока удалена
	// целиком из neo (F13 §1); отдельной точки перекрытия для неё в neo-контракте Level больше нет
	// (мета — часть самого BlockState из getBlockState(...) выше), переносить нечего.

	// безопасная заглушка "пустого мира" (не GT6-поведенческая точка — оригинал не различал жидкость
	// отдельно от блока в 1.7.10; чтобы не трогать getChunkSource()==null через дефолтный Level.getFluidState).
	@Override public FluidState getFluidState(BlockPos aPos) {
		return Fluids.EMPTY.defaultFluidState();
	}

	// --- ниже — чисто административные abstract-методы Level/LevelAccessor/LevelReader/EntityGetter/
	// CollisionGetter, у которых нет соответствия в 1.7.10-оригинале (в нём просто не существовало этого
	// контракта) и которые не вызываются на CS.DW нигде в дереве мода (единственный потребитель —
	// `gregapi/util/CR.java` передаёт CS.DW только в `recipe.matches(...)`, не трогающий эти методы).
	// Безопасные дамми-значения: готовый public no-arg конструктор/статический EMPTY-фактори там, где он
	// есть без реестра, иначе null/empty/0/false.

	@Override public void gameEvent(Holder<GameEvent> aGameEvent, Vec3 aPosition, GameEvent.Context aContext) {/*Do nothing*/}

	@Override public void levelEvent(Entity aSource, int aType, BlockPos aPos, int aData) {/*Do nothing*/}

	// нет 1.7.10-аналога (тик-планировщик как отдельный аксессор появился только в neo); переиспользуем
	// готовый движковый "чёрная дыра"-стаб (`neo-decompiled/net/minecraft/world/ticks/
	// BlackholeTickAccess.java:46-48`), которым сам ванильный движок закрывает те же случаи "нет реального
	// тик-хранилища" — не изобретаем новую абстракцию (правило R2).
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
