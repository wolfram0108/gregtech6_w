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
// `getBlock(x,y,z)`/`setBlock(x,y,z,Block,meta,flags)`/`getBiomeGenForCoords(x,z)`) в 1.20.1 отсутствует так же,
// как и в 26.x — `Level` (`forge-1201-decompiled/net/minecraft/world/level/Level.java:119`) имеет конкретный ctor
// `(WritableLevelData,ResourceKey<Level>,RegistryAccess,Holder<DimensionType>,Supplier<ProfilerFiller>,boolean,
// boolean,long,int)` и свой набор abstract-методов. Класс остаётся минимальным конкретным Level-подклассом:
// ВСЕ abstract-методы Level/LevelAccessor закрыты дамми-заглушками, GT6-поведенческие точки (getBlock/setBlock/
// getBiomeGenForCoords/canBlockSeeTheSky) — 1:1 на своих движковых эквивалентах (пометки при каждом методе).
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

	// F6 dummy-world (фейк-мир, random-hookup не требуется): оригинал делал `rand = mRandom;` (World.rand — публичное
	// поле типа java.util.Random). В 1.20.1 `Level.random` — `private final RandomSource` (интерфейс с другим
	// контрактом: fork()/forkPositional()/nextInt()/… — `forge-1201-decompiled/net/minecraft/util/RandomSource.java`),
	// сеттера нет. GT_IteratorRandom/mRandom сохранены структурно (поле+класс не удалены), но НЕ подключены к
	// живому RNG движка — адаптер RandomSource был бы НОВОЙ сущностью, которой нет в GT6 (правило R2), а grep по
	// всему дереву GT6 подтверждает: ни `GT_IteratorRandom`, ни `mRandom` нигде не читаются извне этого файла —
	// только `CS.DW` (сюда) передаётся в `recipe.matches(aCrafting, CS.DW)` (`gregapi/util/CR.java:524,562,573,681`),
	// который не трогает `world.rand`. Реальной потери поведения нет.
	public GT_IteratorRandom mRandom = new GT_IteratorRandom();
	public ItemStack mLastSetBlock = null;

	// было World.getSeaLevel()-подобной константы не было вовсе в 1.7.10 (WorldProvider.getAverageGroundLevel
	// использовался по месту); центральный уровень воды GT6 уже вынесен в `WD.waterLevel()`
	// (`gregapi/util/WD.java`, дефолт оверворлда 62) — переиспользуем центр вместо новой константы.
	private static int mSeaLevel() {return WD.waterLevel();}

	/**
	 * Тип измерения дамми-мира — ЗАРЕГИСТРИРОВАННЫЙ ванильный OVERWORLD, взятый из реестра живого сервера.
	 *
	 * <p>Своей копии {@code DimensionType} здесь быть не может: {@code Level} требует Holder С КЛЮЧОМ —
	 * {@code this.dimensionTypeId = p_270240_.unwrapKey().orElseThrow(() -> new IllegalArgumentException(
	 * "Dimension must be registered, got " + p_270240_))} ({@code Level.java:123-126}). У {@code Holder.direct(…)}
	 * ключа нет по определению, поэтому конструирование падало КАЖДЫЙ старт (3 раза за запуск — по разу на
	 * GT-мод), и {@code CS.DW} оставался null: сверка совпадения рецептов шла без мира.
	 *
	 * <p>Значения прежней ручной копии были и так «дословно с ванильного OVERWORLD» — то есть копия дублировала
	 * факт движка. Берём сам факт: реестр приходит от сервера тем же параметром {@code aRegistryAccess}, ради
	 * которого он и заводился. 1.7.10 давал дамми ровно такой же безымянный overworld-подобный провайдер
	 * ({@code gt6-original/…/DummyWorld.java:82} — {@code new WorldProvider() {…}}).
	 */
	private static Holder<DimensionType> overworldType(RegistryAccess aRegistryAccess) {
		return aRegistryAccess.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
	}

	// WritableLevelData в 1.20.1 — покоординатный спавн (setXSpawn/…/setSpawnAngle), а не запись RespawnData,
	// как было в 26.x (`WritableLevelData.java`); читающая половина — `LevelData.java:10-34`.
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

	// было DummyWorld(ISaveHandler,String,WorldProvider,WorldSettings,Profiler) + DummyWorld() делегирующий
	// в него анонимными ISaveHandler/WorldProvider-заглушками — все 4 типа параметров в 1.20.1 отсутствуют,
	// делегирующий ctor физически невозможен 1:1. Оставшийся no-arg ctor строит аргументы Level-контракта напрямую.
	public DummyWorld() {this(RegistryAccess.EMPTY);}

	/**
	 * ЕДИНСТВЕННАЯ точка появления {@code CS.DW}: строит мир, когда реестр уже загружен, и молчит, если он
	 * уже построен. Зовётся со старта сервера — там {@code MinecraftServer.registryAccess()} полон.
	 * Потеря не молчит: если мир не построится и здесь, в лог уйдёт причина, а не пустая ссылка.
	 */
	public static synchronized void ensure(RegistryAccess aRegistryAccess) {
		if (gregapi.data.CS.DW != null || aRegistryAccess == null) return;
		try {
			gregapi.data.CS.DW = new DummyWorld(aRegistryAccess);
		} catch (Throwable e) {
			gregapi.data.CS.ERR.println("GT6: dummy-мир не создан — проверка совпадения рецептов пойдёт без мира (" + e + ").");
			e.printStackTrace(gregapi.data.CS.ERR);
		}
	}

	/**
	 * Реестр приходит снаружи — от сервера, когда он уже загружен (в 26.x жёсткий {@code RegistryAccess.EMPTY}
	 * ронял конструирование мира на биом-реестре). Профайлер — {@code InactiveProfiler.INSTANCE}: движковая
	 * «выключенная» реализация {@code ProfilerFiller}, ей же ванильный сервер закрывает те же случаи —
	 * не изобретаем новую заглушку (правило R2).
	 */
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

	// было protected IChunkProvider createChunkProvider() {return null;} — `LevelAccessor.getChunkSource()`
	// (`LevelAccessor.java:73`); та же семантика "нет чанк-провайдера у дамми" сохранена дословно.
	@Override public ChunkSource getChunkSource() {
		return null;
	}

	// было public Entity getEntityByID(int aEntityID) {return null;} — abstract `Level.getEntity(int)`
	// (`Level.java:712`).
	@Override public Entity getEntity(int aEntityID) {
		return null;
	}

	// было public boolean setBlock(int aX,int aY,int aZ,Block aBlock,int aMeta,int aFlags) — F13: числовая
	// мета блока удалена, единый узел адресации — `BlockPos`+`BlockState`. Переносим ту же запись в
	// mLastSetBlock; meta-параметр у ST.make(Block,long,long) получает 0 — общего канала "meta произвольного
	// BlockState" в центре нет (F13 §3).
	@Override public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags, int aUpdateLimit) {
		mLastSetBlock = ST.make(aState.getBlock(), 1, 0);
		return T;
	}

	// было public Biome getBiomeGenForCoords(int aX,int aZ) {return (in area) ? plains : ocean;}
	// F6 dummy-world: 1.7.10 `BiomeGenBase.plains`/`.ocean` были статическими VM-синглтонами, а `Biome` —
	// объект динамического датапак-реестра `Registries.BIOME`, недостижимый без живого `RegistryAccess`.
	// Область-различение сохранена дословно в mInArea(...) и исполняется ниже (обе ветки видимы, не схлопнуты).
	private boolean mInArea(int aX, int aZ) {
		return aX >= 16 && aZ >= 16 && aX < 32 && aZ < 32;
	}

	@Override public Holder<Biome> getUncachedNoiseBiome(int aQuartX, int aQuartY, int aQuartZ) {
		return getBiome(new BlockPos(QuartPos.toBlock(aQuartX), QuartPos.toBlock(aQuartY), QuartPos.toBlock(aQuartZ)));
	}

	@Override public Holder<Biome> getBiome(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return null; // область плейнс (было BiomeGenBase.plains)
		return null; // вне области — океан (было BiomeGenBase.ocean)
	}

	@Override public int getSeaLevel() {
		return mSeaLevel();
	}

	// было public boolean canBlockSeeTheSky(int aX,int aY,int aZ) {return (in area) ? aY>64 : T;} — движковый
	// путь считает через lighting-engine (которого у дамми нет, getChunkSource()==null), поэтому перекрыт
	// напрямую той же координатной проверкой.
	@Override public boolean canSeeSky(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() > 64;
		return T;
	}

	// было public Block getBlock(int aX,int aY,int aZ) {return (in area && aY==64) ? Blocks.grass : NB; else NB;}
	// — точка чтения блока `BlockGetter.getBlockState(BlockPos)`. Перекрыт напрямую (минуя
	// getChunkSource()==null), область/высота — дословно.
	@Override public BlockState getBlockState(BlockPos aPos) {
		if (mInArea(aPos.getX(), aPos.getZ())) return aPos.getY() == 64 ? Blocks.GRASS_BLOCK.defaultBlockState() : NB.defaultBlockState();
		return NB.defaultBlockState();
	}

	// было public int getBlockMetadata(int aX,int aY,int aZ) {return 0;} — числовая мета блока удалена
	// (F13 §1); отдельной точки перекрытия для неё в контракте больше нет (мета — часть BlockState выше).

	// безопасная заглушка "пустого мира" (не GT6-поведенческая точка — оригинал не различал жидкость
	// отдельно от блока в 1.7.10; чтобы не трогать getChunkSource()==null через дефолтный Level.getFluidState).
	@Override public FluidState getFluidState(BlockPos aPos) {
		return Fluids.EMPTY.defaultFluidState();
	}

	// --- ниже — чисто административные abstract-методы Level/LevelAccessor, у которых нет соответствия в
	// 1.7.10-оригинале (в нём просто не существовало этого контракта) и которые не вызываются на CS.DW нигде
	// в дереве мода (единственный потребитель — `gregapi/util/CR.java` передаёт CS.DW только в
	// `recipe.matches(...)`, не трогающий эти методы). Безопасные дамми-значения: готовый public no-arg
	// конструктор/статический EMPTY там, где он есть без реестра, иначе null/empty/0/false.

	// LevelAccessor.gameEvent в 1.20.1 берёт сырой GameEvent, а не Holder<GameEvent> (`LevelAccessor.java:102`).
	@Override public void gameEvent(GameEvent aGameEvent, Vec3 aPosition, GameEvent.Context aContext) {/*Do nothing*/}

	// LevelAccessor.levelEvent в 1.20.1 берёт Player, а не Entity (`LevelAccessor.java:96`).
	@Override public void levelEvent(Player aPlayer, int aType, BlockPos aPos, int aData) {/*Do nothing*/}

	// нет 1.7.10-аналога (тик-планировщик как отдельный аксессор появился позже); переиспользуем готовый
	// движковый "чёрная дыра"-стаб (`net/minecraft/world/ticks/BlackholeTickAccess.java`), которым сам
	// ванильный движок закрывает те же случаи "нет реального тик-хранилища" — не изобретаем абстракцию (R2).
	@Override public LevelTickAccess<Block> getBlockTicks() {
		return BlackholeTickAccess.<Block>emptyLevelList();
	}

	@Override public LevelTickAccess<Fluid> getFluidTicks() {
		return BlackholeTickAccess.<Fluid>emptyLevelList();
	}

	@Override public void sendBlockUpdated(BlockPos aPos, BlockState aOld, BlockState aCurrent, int aUpdateFlags) {/*Do nothing*/}

	// обе перегрузки в 1.20.1 принимают Player (`Level.java:392,398`), в 26.x первым шёл Entity.
	@Override public void playSeededSound(Player aExcept, double aX, double aY, double aZ, Holder<SoundEvent> aSound, SoundSource aSource, float aVolume, float aPitch, long aSeed) {/*Do nothing*/}

	@Override public void playSeededSound(Player aExcept, Entity aSourceEntity, Holder<SoundEvent> aSound, SoundSource aSource, float aVolume, float aPitch, long aSeed) {/*Do nothing*/}

	@Override public String gatherChunkSourceStats() {
		return "";
	}

	// карты в 1.20.1 адресуются String-именем, а не MapId (`Level.java:804-808`).
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

	// в 1.20.1 у мира спрашивают RecipeManager напрямую (`Level.java:911`), а не RecipeAccess, как в 26.x.
	@Override public RecipeManager getRecipeManager() {
		return new RecipeManager();
	}

	// EntityGetter.players() — в 26.x закрывался дефолтом Level, в 1.20.1 остаётся abstract
	// (EntityGetter.java:24). У дамми игроков нет.
	@Override public List<? extends Player> players() {
		return List.of();
	}

	// BlockAndTintGetter.getShade(Direction, boolean) — в 1.20.1 abstract на уровне Level
	// (BlockAndTintGetter.java:8). Дамми не рисуется: отдаём «без затенения», как ванильный
	// путь при shade==false (ClientLevel.java:717-718).
	@Override public float getShade(net.minecraft.core.Direction aDirection, boolean aShade) {
		return 1.0F;
	}

	// LevelReader.enabledFeatures() — в 1.20.1 abstract (в 26.x его в контракте нет).
	// Дамми не несёт датапак-состояния: отдаём ванильный набор по умолчанию, как FeatureFlags.DEFAULT_FLAGS.
	@Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() {
		return net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS;
	}

	@Override protected LevelEntityGetter<Entity> getEntities() {
		return null;
	}
}
