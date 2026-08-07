/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.worldgen;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import static gregapi.data.CS.*;

import java.util.List;
import java.util.Random;

import gregapi.code.ArrayListNoNulls;
import gregapi.code.BiomeNameSet;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * @author Gregorius Techneticies
 */
public class GT6WorldGenerator {
	public static class WorldGenContainer implements Runnable {
		public final int mMinX, mMinZ, mMaxX, mMaxZ, mDimType;
		public final WorldGenLevel mWorld;
		public final Random mRandom;
		public final List<WorldgenObject> mGenNormal;
		public final List<WorldgenObject> mGenLargeOres;
		
		public WorldGenContainer(List<WorldgenObject> aGenNormal, List<WorldgenObject> aGenLargeOres, int aDimType, WorldGenLevel aWorld, int aX, int aZ) {
			mMinX = aX; mMinZ = aZ; mMaxX = aX + 15; mMaxZ = aZ + 15;
			mDimType = aDimType;
			mWorld = aWorld;
			mGenNormal = aGenNormal;
			mGenLargeOres = aGenLargeOres;
			mRandom = WD.random(aWorld, aX, aZ);
		}
		
		@Override
		public void run() {
			if (!mGenNormal.isEmpty()) {
				// F6: было `mWorld.getChunkFromBlockCoords(blockX,blockZ)` (взятие чанка по блок-координатам) —
				// удалено; реальный neo-эквивалент `Level.getChunk(chunkX,chunkZ)` берёт ЧАНКОВЫЕ координаты
				// (Level.java:202-203), поэтому блок-координаты сдвинуты `>>4`, как делал сам старый метод внутри.
				ChunkAccess tChunk = mWorld.getChunk((mMinX+7) >> 4, (mMinZ+7) >> 4);
				if (tChunk == null) return;
				Biome[][] tBiomes = new Biome[16][16];
				BiomeNameSet tBiomeNames = new BiomeNameSet();
				// F6: было `tChunk.getBiomeGenForWorldCoords(x,z,worldChunkMgr)` через удалённый
				// `WorldProvider.worldChunkMgr` (2D-биом-менеджер 1.7.10) с ручным null-фолбэком на статические
				// `Biome.hell/sky/plains` (тоже удалены — биомы data-driven, нет compile-time констант-объектов).
				// Реальный neo-путь: `LevelReader.getBiome(BlockPos): Holder<Biome>` (LevelReader.java:42-43,
				// `.value()` — используемый паттерн тот же, что и в самом Level.java:952), который в отличие от
				// 1.7.10 не возвращает null для загруженного чанка — фолбэк на hell/sky/plains поэтому исчез
				// (недостижим на практике; см. F6-примечание ниже, если когда-либо словим NPE/ISE здесь). Имя биома
				// (для BiomeNameSet) берётся из самого Holder через `unwrapKey()` (Holder.java:40, паттерн
				// `unwrapKey().map(k->k.identifier().toString())` — как в самом Holder.java:47) — биом
				// НЕ built-in реестр (data-driven/datapack), `BuiltInRegistries.BIOME` не существует.
				// Y для биом-грида (R8-БРАК-ФИКС 2026-07-11): оригинал звал биом 2D (`getBiomeGenForWorldCoords`
				// внутри принимал только x,z, Y игнорировался — 1.7.10 биомы плоские). Раньше здесь ошибочно
				// стояло `WD.waterLevel(Level)` (GT6-специфичная "высота воды по измерению", НЕ то же самое,
				// что высота поверхности суши) — на суше выше/ниже уровня моря это давало НЕВЕРНЫЙ биом
				// (не 1:1 с оригинальным 2D-запросом). Реальный neo `LevelReader.getBiome(BlockPos)` 3D и
				// ТРЕБУЕТ Y — берём высоту ПОВЕРХНОСТИ данной колонки: `Level.getHeight(Heightmap.Types,x,z)`
				// (объявление `LevelReader.java:32`, реализация `Level.java:359`; тот же паттерн "Y поверхности
				// перед биом/событием" использует сам движок: `ServerLevel.java:588` берёт `getHeightmapPos(...)`
				// перед `getBiome(topPos)`, аналогично `BeaconBlockEntity.java:139`, `VillageSiege.java:121`,
				// `Raid.java:675`). ПОКОЛОННО (i,j) — не одна высота на весь чанк, как было бы при одиночном
				// `tBiomeY` (высота поверхности разная в каждой из 16x16 колонок).
				for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
					// F6-примечание (не заглушка; код ниже работает): .value() может бросить, если Holder не bound — на загруженном
					// чанке такого практически не бывает, отдельный try/catch не заводим (упадёт в общий catch
					// вызывающего WorldgenObject, если вообще случится).
					int tX = mMinX+i, tZ = mMinZ+j;
					// ТОЧНЫЙ биом-канал (R-стык-чанков 2026-07-19): было mWorld.getBiome(pos) = BiomeManager с
					// ШУМОВЫМ ДЖИТТЕРОМ ±2-3 блока (обфускация границ для визуала/спавна) — 1.7.10 генлейер давал
					// ТОЧНЫЙ биом per-колонка. Джиттер у границы биомов подмешивал «болото» в речные колонны →
					// WorldgenSwamp захватывал воду/берега на речной стороне (резко по шву чанка). Реальный точный
					// канал = сохранённый quart-биом чанка: ChunkAccess.getNoiseBiome (ChunkAccess.java:432, без
					// джиттера; quart-координаты мира = блок >>2, Y — поверхность колонки).
					Holder<Biome> tBiomeHolder = tChunk.getNoiseBiome(tX >> 2, mWorld.getHeight(Heightmap.Types.WORLD_SURFACE, tX, tZ) >> 2, tZ >> 2);
					tBiomes[i][j] = tBiomeHolder.value();
					tBiomeHolder.unwrapKey().ifPresent(k -> tBiomeNames.add(k.identifier().toString()));
				}
				
				GENERATING_SPECIAL = F;
				
				// Yes, it has to be looped twice in a row, this cannot be optimized into one Loop!
				for (WorldgenObject tWorldGen : mGenNormal) tWorldGen.reset(mWorld, tChunk, mDimType, mMinX, mMinZ, mMaxX, mMaxZ, mRandom, tBiomes, tBiomeNames);
				for (WorldgenObject tWorldGen : mGenNormal) if (tWorldGen.enabled(mWorld, mDimType)) {
					try {
						tWorldGen.generate(mWorld, tChunk, mDimType, mMinX, mMinZ, mMaxX, mMaxZ, mRandom, tBiomes, tBiomeNames);
					} catch (Throwable e) {
						e.printStackTrace(ERR);
					}
					// F6 impossible-1:1 (переносить нечего): было `if (tChunk.lastSaveTime==Long.MAX_VALUE) {
					// tChunk.hasEntities=tChunk.isModified=F; throw new RuntimeException(...);}` — защита от
					// конкретного 1.7.10-бага повреждения чанка через сигнальное значение lastSaveTime.
					// `lastSaveTime`/`hasEntities`/`isModified` как публичные поля LevelChunk удалены (нет
					// аналога в neo-decompiled LevelChunk.java), а сам класс повреждения чанков, под который
					// был заточен этот детектор, в современном движке не существует — переносить нечего.
				}
				
				if (mGenLargeOres != null && !mGenLargeOres.isEmpty()) {
					int tMaxWeight = 0;
					List<WorldgenOresLarge> tList = new ArrayListNoNulls<>();
					
					for (WorldgenObject tWorldGen : mGenLargeOres) if (tWorldGen.enabled(mWorld, mDimType)) {tMaxWeight += ((WorldgenOresLarge)tWorldGen).mWeight; tList.add((WorldgenOresLarge)tWorldGen);}
					
					if (tMaxWeight > 0 && !tList.isEmpty()) for (int tX=-32; tX<=32; tX+=16) for (int tZ=-32; tZ<=32; tZ+=16) {
						int tChunkX = mMinX+tX, tChunkZ = mMinZ+tZ;
						if (((tChunkX >> 4)+402653184) % 3 == 1 && ((tChunkZ >> 4)+402653184) % 3 == 1) {
							Random aRandom = WD.random(mWorld, tChunkX, tChunkZ);
							int tRandomWeight = aRandom.nextInt(tMaxWeight);
							for (WorldgenOresLarge tWorldGen : tList) {
								tRandomWeight -= tWorldGen.mWeight;
								if (tRandomWeight <= 0) {try {tWorldGen.generate(mWorld, tChunk, mMinX, mMinZ, mMaxX, mMaxZ, tChunkX, tChunkZ, mRandom);} catch (Throwable e) {e.printStackTrace(ERR);} break;}
							}
						}
					}
				}
				
				// Kill off every single Item Entity that may have dropped during Worldgen.
				// F6: было `(List<ItemEntity>)mWorld.getEntitiesWithinAABB(Class,AABB)` (удалённый Forge-метод)
				// + `new AABB(x1,y1,z1,x2,y2,z2)` (удалённая static-фабрика) + `Entity.setDead()`
				// (удалён). Реальные neo-эквиваленты: `EntityGetter.getEntitiesOfClass(Class,AABB)`
				// (EntityGetter.java:50, уже без unchecked-каста), конструктор `AABB(double x6)` (AABB.java:23),
				// `Entity.discard()` (Entity.java:409, `remove(RemovalReason.DISCARDED)`).
				// BUG-103: удаление идёт через центр WD (серверным потоком) — генерация чанка бежит в воркере,
				// а состав сущностей правит только серверный поток; иначе рвётся обход ChunkMap.tick.
				WD.discardEntitiesSafely(mWorld, ItemEntity.class, new AABB(mMinX-32, 0, mMinZ-32, mMinX+48, 256, mMinZ+48), null);
				// F6 impossible-1:1 (поле удалено; neo Heightmap пересчитывает сам, ручной сброс не нужен): было `Arrays.fill(tChunk.precipitationHeightMap,-999)` —
				// обходной 1.7.10-хак против убийства снегом пеньков деревьев. Поле удалено, современный
				// Heightmap-механизм (`net.minecraft.world.level.levelgen.Heightmap`) считается движком заново
				// на лету — прямого ручного "сброса" в neo нет и, судя по всему, не нужен (движок сам
				// пересчитывает высоты после генерации), но поведенчески не подтверждено.
				// F6: было `tChunk.isModified = T` (публичное поле-флаг) — реальный neo-эквивалент
				// `LevelChunk.markUnsaved()` (LevelChunk.java:178, вызывает `ChunkAccess.markUnsaved()`).
				tChunk.markUnsaved();
			}
		}
	}
	
	private static final List<Runnable> LIST = new ArrayListNoNulls<>();
	private static boolean LOCK = F;
	public static boolean PFAA = F, TFC = F;
	
	public static void generate(WorldGenLevel aWorld, int aX, int aZ, boolean aGalactiCraft) {
		// F6: было `switch(WD.dimensionId(aWorld)) {case -2147483648: return; case DIM_OVERWORLD: ...}` —
		// `WorldProvider.dimensionId` удалён, у измерения в neo нет числового id вообще (см. javadoc
		// NoiseGenerator.java). Ветка `case -2147483648` (Integer.MIN_VALUE) была сигнальным значением "мир
		// недогружен/провайдер не готов" — в neo `aWorld.getLevel().dimension()` для валидного `Level`-объекта всегда
		// возвращает настоящий `ResourceKey<Level>`, такого сигнального состояния не бывает — ветка не имеет
		// аналога (см. F6-примечание ниже). Три ванильных ветки сверены на реальные `Level.OVERWORLD/NETHER/END`
		// (Level.java:95-97).
		ResourceKey<Level> tDim = aWorld.getLevel().dimension();
		if (tDim == Level.OVERWORLD) {generate(new WorldGenContainer(TFC ? GEN_TFC : PFAA ? GEN_PFAA : GENERATE_STONE ? GEN_GT : GEN_OVERWORLD, TFC ? ORE_TFC : PFAA ? ORE_PFAA : GENERATE_STONE ? null : ORE_OVERWORLD, DIM_OVERWORLD, aWorld, aX, aZ)); return;}
		if (tDim == Level.NETHER   ) {generate(new WorldGenContainer(GEN_NETHER, ORE_NETHER, DIM_NETHER, aWorld, aX, aZ)); return;}
		if (tDim == Level.END      ) {generate(new WorldGenContainer(GEN_END   , ORE_END   , DIM_END   , aWorld, aX, aZ)); return;}
		// F6 impossible-1:1 (сигнал «мир не готов» из 1.7.10 без neo-аналога): сигнальная ветка "мир не готов" из 1.7.10 не имеет
		// аналога в neo (см. комментарий выше) — намеренно опущена, а не угадана.

		if (WD.dimENVM         (aWorld)) {generate(new WorldGenContainer(GENERATE_STONE ? GEN_ENVM_GT           : GEN_ENVM          , GENERATE_STONE ? null : ORE_ENVM          , DIM_ENVM          , aWorld, aX, aZ)); return;}
		if (WD.dimA97          (aWorld)) {generate(new WorldGenContainer(GENERATE_STONE ? GEN_A97_GT            : GEN_A97           , GENERATE_STONE ? null : ORE_A97           , DIM_A97           , aWorld, aX, aZ)); return;}
		if (WD.dimCW2AquaCavern(aWorld)) {generate(new WorldGenContainer(GENERATE_STONE ? GEN_CW2_AquaCavern_GT : GEN_CW2_AquaCavern, GENERATE_STONE ? null : ORE_CW2_AquaCavern, DIM_CW2_AquaCavern, aWorld, aX, aZ)); return;}
		if (WD.dimCW2Caveland  (aWorld)) {generate(new WorldGenContainer(GENERATE_STONE ? GEN_CW2_Caveland_GT   : GEN_CW2_Caveland  , GENERATE_STONE ? null : ORE_CW2_Caveland  , DIM_CW2_Caveland  , aWorld, aX, aZ)); return;}
		if (WD.dimCW2Cavenia   (aWorld)) {generate(new WorldGenContainer(GENERATE_STONE ? GEN_CW2_Cavenia_GT    : GEN_CW2_Cavenia   , GENERATE_STONE ? null : ORE_CW2_Cavenia   , DIM_CW2_Cavenia   , aWorld, aX, aZ)); return;}
		if (WD.dimCW2Cavern    (aWorld)) {generate(new WorldGenContainer(GENERATE_STONE ? GEN_CW2_Cavern_GT     : GEN_CW2_Cavern    , GENERATE_STONE ? null : ORE_CW2_Cavern    , DIM_CW2_Cavern    , aWorld, aX, aZ)); return;}
		if (WD.dimCW2Caveworld (aWorld)) {generate(new WorldGenContainer(GENERATE_STONE ? GEN_CW2_Caveworld_GT  : GEN_CW2_Caveworld , GENERATE_STONE ? null : ORE_CW2_Caveworld , DIM_CW2_Caveworld , aWorld, aX, aZ)); return;}
		
		if (WD.dimMYST         (aWorld)) {generate(new WorldGenContainer(TFC ? GEN_TFC : PFAA ? GEN_PFAA : GENERATE_STONE ? GEN_GT : GEN_OVERWORLD, TFC ? ORE_TFC : PFAA ? ORE_PFAA : GENERATE_STONE ? null : ORE_OVERWORLD, DIM_OVERWORLD, aWorld, aX, aZ)); return;}
		if (WD.dimWTCH         (aWorld)) {generate(new WorldGenContainer(TFC ? GEN_TFC : PFAA ? GEN_PFAA : GENERATE_STONE ? GEN_GT : GEN_OVERWORLD, TFC ? ORE_TFC : PFAA ? ORE_PFAA : GENERATE_STONE ? null : ORE_OVERWORLD, DIM_OVERWORLD, aWorld, aX, aZ)); return;}
		
		if (WD.dimTF           (aWorld)) {generate(new WorldGenContainer(GEN_TWILIGHT    , ORE_TWILIGHT    , DIM_TWILIGHT    , aWorld, aX, aZ)); return;}
		if (WD.dimAETHER       (aWorld)) {generate(new WorldGenContainer(GEN_AETHER      , ORE_AETHER      , DIM_AETHER      , aWorld, aX, aZ)); return;}
		if (WD.dimERE          (aWorld)) {generate(new WorldGenContainer(GEN_EREBUS      , ORE_EREBUS      , DIM_EREBUS      , aWorld, aX, aZ)); return;}
		if (WD.dimBTL          (aWorld)) {generate(new WorldGenContainer(GEN_BETWEENLANDS, ORE_BETWEENLANDS, DIM_BETWEENLANDS, aWorld, aX, aZ)); return;}
		if (WD.dimATUM         (aWorld)) {generate(new WorldGenContainer(GEN_ATUM        , ORE_ATUM        , DIM_ATUM        , aWorld, aX, aZ)); return;}
		if (WD.dimALF          (aWorld)) {generate(new WorldGenContainer(GEN_ALFHEIM     , ORE_ALFHEIM     , DIM_ALFHEIM     , aWorld, aX, aZ)); return;}
		if (WD.dimDD           (aWorld)) {generate(new WorldGenContainer(GEN_DEEPDARK    , ORE_DEEPDARK    , DIM_DEEPDARK    , aWorld, aX, aZ)); return;}
		if (WD.dimTROPIC       (aWorld)) {generate(new WorldGenContainer(GEN_TROPICS     , ORE_TROPICS     , DIM_TROPICS     , aWorld, aX, aZ)); return;}
		if (WD.dimCANDY        (aWorld)) {generate(new WorldGenContainer(GEN_CANDY       , ORE_CANDY       , DIM_CANDY       , aWorld, aX, aZ)); return;}
		
		
		// F6: было `aWorld.getBiomeGenForCoords(x,z)` (2D, удалён) + `Biome.biomeName` (поле, удалено, биомы
		// data-driven). Реальный neo-путь: `LevelReader.getBiome(BlockPos): Holder<Biome>` (LevelReader.java:
		// 42-43) + `Holder.unwrapKey()` для строкового идентификатора (Holder.java:40,47 — биом НЕ built-in
		// реестр, `BuiltInRegistries.BIOME` не существует; тот же паттерн, что и в WorldGenContainer.run() выше).
		// Y (тот же R8-фикс, что и в WorldGenContainer.run() выше): не `WD.waterLevel` (GT6-высота воды по
		// измерению) — высота ПОВЕРХНОСТИ колонки (aX+7,aZ+7) через `Level.getHeight(Heightmap.Types,x,z)`.
		Holder<Biome> aBiomeHolder = aWorld.getBiome(new BlockPos(aX+7, aWorld.getHeight(Heightmap.Types.WORLD_SURFACE, aX+7, aZ+7), aZ+7));
		String aBiomeName = aBiomeHolder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
		if (BIOMES_VOID.contains(aBiomeName)) return;

		if (BIOMES_EREBUS.contains(aBiomeName)) {
			generate(new WorldGenContainer(GEN_EREBUS, ORE_EREBUS, DIM_EREBUS, aWorld, aX, aZ));
			return;
		}
		if (BIOMES_MOON.contains(aBiomeName)) {
			generate(new WorldGenContainer(GEN_MOON, ORE_MOON, DIM_MOON, aWorld, aX, aZ));
			return;
		}
		if (BIOMES_MARS.contains(aBiomeName)) {
			generate(new WorldGenContainer(GEN_MARS, ORE_MARS, DIM_MARS, aWorld, aX, aZ));
			return;
		}
		if (BIOMES_ASTEROIDS.contains(aBiomeName)) {
			generate(new WorldGenContainer(GEN_ASTEROIDS, ORE_ASTEROIDS, DIM_ASTEROIDS, aWorld, aX, aZ));
			return;
		}
		if (aGalactiCraft || BIOMES_SPACE.contains(aBiomeName)) {
			generate(new WorldGenContainer(GEN_PLANETS, ORE_PLANETS, DIM_PLANETS, aWorld, aX, aZ));
			return;
		}
	}
	
	public static void generate(WorldGenContainer aWorldgen) {
		LIST.add(aWorldgen);
		if (!LOCK) {
			LOCK = T;
			while (!LIST.isEmpty()) LIST.remove(LIST.size()-1).run();
			LOCK = F;
		}
	}
}
