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

package gregapi.worldgen.dungeon;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import gregapi.block.metatype.BlockStones;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.IL;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
// F-layer-decouple: gregtech.tileentity.placeables.MultiTileEntityCoin — CONTENT-класс (вне ядра-272/среза,
// не портирован). Прямой import из gregapi-worldgen = утечка core->content. Данные COIN_MAP (Map<OreDictMaterial,
// ItemStack>) читаются рефлексией по имени класса в рантайме (когда контент загружен) — приём GT6 для
// кросс-слойного доступа (UT.Reflection.getFieldContent, см. использование ниже). Значение — CORE-тип ItemStack.
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class WorldgenDungeonGT extends WorldgenObject {
	public static IDungeonChunk
	  PILLAR          = new DungeonChunkPillar()
	, ROOM_EMPTY      = new DungeonChunkRoomEmpty()
	, DOOR_PISTON     = new DungeonChunkDoorPiston()
	, CORRIDOR        = new DungeonChunkCorridor()
	, CORRIDOR3       = new DungeonChunkCorridor3()
	, CORRIDOR4       = new DungeonChunkCorridor4()
	, ENTRANCE        = new DungeonChunkEntrance()
	, BARRACKS        = new DungeonChunkBarracks()
	;
	
	public static final TagData
	  TAG_PORTAL_NETHER   = TagData.createTagData("gt.dungeon.portal.nether")
	, TAG_PORTAL_END      = TagData.createTagData("gt.dungeon.portal.end")
	, TAG_PORTAL_TWILIGHT = TagData.createTagData("gt.dungeon.portal.twilight")
	, TAG_PORTAL_AETHER   = TagData.createTagData("gt.dungeon.portal.aether")
	, TAG_PORTAL_MYST     = TagData.createTagData("gt.dungeon.portal.myst")
	, TAG_WORKSHOP        = TagData.createTagData("gt.dungeon.workshop")
	, TAG_MINING_BEDROCK  = TagData.createTagData("gt.dungeon.mining.bedrock")
	, TAG_LIBRARY         = TagData.createTagData("gt.dungeon.library")
	, TAG_LIBRARY_NORMAL  = TagData.createTagData("gt.dungeon.library.normal")
	, TAG_LIBRARY_THAUM   = TagData.createTagData("gt.dungeon.library.thaumcraft")
	, TAG_LIBRARY_MYST    = TagData.createTagData("gt.dungeon.library.mystcraft")
	, TAG_FARM_MOBS       = TagData.createTagData("gt.dungeon.farm.mobs")
	, TAG_FARM_CROP       = TagData.createTagData("gt.dungeon.farm.crop")
	, TAG_FARM_FISH       = TagData.createTagData("gt.dungeon.farm.fish")
	;
	
	public static final List<IDungeonChunk> ROOMS = new ArrayListNoNulls<IDungeonChunk>(F
	, new DungeonChunkRoomWorkshop()
	, new DungeonChunkRoomMiningBedrock()
	, new DungeonChunkRoomLibraryNormal()
	, new DungeonChunkRoomLibraryMystcraft()
	, new DungeonChunkRoomLibraryThaumcraft()
	, new DungeonChunkRoomFarmMobs()
	, new DungeonChunkRoomFarmCrop()
	, new DungeonChunkRoomFarmFish()
	);
	
	public static final List<IDungeonChunk> DEAD_END = new ArrayListNoNulls<IDungeonChunk>(F
	, new DungeonChunkRoomStorage()
	, new DungeonChunkRoomPortalNether()
	, new DungeonChunkRoomPortalEnd()
	, new DungeonChunkRoomPortalTwilight()
	, new DungeonChunkRoomPortalAether()
	, new DungeonChunkRoomPortalMyst()
	);
	
	public int mProbability, mMinSize, mMaxSize, mMinY, mMaxY, mRoomChance;
	public boolean mPortalNether, mPortalEnd, mPortalTwilight, mPortalAether, mPortalMyst, mZPM;
	public HashSetNoNulls<TagData> mTags = new HashSetNoNulls<>();
	
	@SafeVarargs
	public WorldgenDungeonGT(String aName, boolean aDefault, int aProbability, int aMinSize, int aMaxSize, int aMinY, int aMaxY, int aRoomChance, boolean aOverworld, boolean aNether, boolean aEnd, boolean aPortalNether, boolean aPortalEnd, boolean aPortalTwilight, boolean aPortalAether, boolean aPortalMyst, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
		mProbability        = Math.max(1,           getConfigFile().get(mCategory, "Probability"      , aProbability   ));
		mMinSize            = Math.max(2,           getConfigFile().get(mCategory, "MinSize"          , aMinSize       ));
		mMaxSize            = Math.max(mMinSize,    getConfigFile().get(mCategory, "MaxSize"          , aMaxSize       ));
		mMinY               = Math.max(5,           getConfigFile().get(mCategory, "MinY"             , aMinY          ));
		mMaxY               = Math.max(mMinY,       getConfigFile().get(mCategory, "MaxY"             , aMaxY          ));
		mRoomChance         = Math.max(1,           getConfigFile().get(mCategory, "RoomChance"       , aRoomChance    ));
		mPortalNether       =                       getConfigFile().get(mCategory, "PortalNether"     , aPortalNether  );
		mPortalEnd          =                       getConfigFile().get(mCategory, "PortalEnd"        , aPortalEnd     );
		mPortalTwilight     =                       getConfigFile().get(mCategory, "PortalTwilight"   , aPortalTwilight);
		mPortalAether       =                       getConfigFile().get(mCategory, "PortalAether"     , aPortalAether  );
		mPortalMyst         =                       getConfigFile().get(mCategory, "PortalMyst"       , aPortalMyst    );
		mZPM                =                       getConfigFile().get(mCategory, "ZPMs"             , T);
		
		if (!getConfigFile().get(mCategory, "Room.Workshop"          , T)) mTags.add(TAG_WORKSHOP);
		if (!getConfigFile().get(mCategory, "Room.Mining.Bedrock"    , T)) mTags.add(TAG_MINING_BEDROCK);
		if (!getConfigFile().get(mCategory, "Room.Library.Normal"    , T)) mTags.add(TAG_LIBRARY_NORMAL);
		if (!getConfigFile().get(mCategory, "Room.Library.Thaumcraft", T)) mTags.add(TAG_LIBRARY_THAUM);
		if (!getConfigFile().get(mCategory, "Room.Library.Mystcraft" , T)) mTags.add(TAG_LIBRARY_MYST);
		if (!getConfigFile().get(mCategory, "Room.Farming.Mobs"      , T)) mTags.add(TAG_FARM_MOBS);
		if (!getConfigFile().get(mCategory, "Room.Farming.Crop"      , T)) mTags.add(TAG_FARM_CROP);
		if (!getConfigFile().get(mCategory, "Room.Farming.Fish"      , T)) mTags.add(TAG_FARM_FISH);
	}
	
	public WorldgenDungeonGT() {this(null, F, 100, 3, 7, 20, 20, 6, F, F, F, F, F, F, F, F);}
	
	public static final int ROOM_ID_COUNT = 1, IMPORTANT_ROOM_COUNT = 2;
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		// F6-worldgen (данжи per-chunk, снятие отложки #39). Два форса среды против схемы 1.7.10 «якорный чанк рисует
		// весь данж разом»: (1) фиче разрешена запись только ±1 чанк от генерируемого (neo ChunkPyramid.java:29-35
		// blockStateWriteRadius(1), WorldGenRegion.ensureCanWrite:225) — данж же занимает до 9×9 чанков; (2) прежний
		// разворот региона в ServerLevel давал реентрантный дедлок getChunk().join. МЕХАНИЗМ: каждый чанк данж-области
		// детерминированно ПЕРЕИГРЫВАЕТ весь данж (общий tRandom от якоря — WD.random по seed мира, воспроизводим из
		// любого чанка) и физически пишет ТОЛЬКО клетку своего чанка (маска DungeonData.mWrite). Межклеточное состояние
		// (mGeneratedKeys/mTags/layout/цепочка Random клеток) воспроизводится идентично во всех чанках области, потому
		// что ни выбор комнат, ни Random-потребление от мира не зависят (аудит architecture/dungeons.md: возвраты
		// generate комнат безусловны, кроме generateVein — детерминизирован в DungeonChunkRoomMiningBedrock).
		//
		// ВНИМАНИЕ: aRandom (общий чанковый Random воркген-контейнера) для данж-решений НЕПРИГОДЕН — его состояние на
		// входе зависит от Random-потребления предыдущих воркгенов чанка и НЕвоспроизводимо из соседнего чанка. Данж
		// сидит на собственном tRandom от якоря (соль отделяет поток данжа от рудных WD.random тех же координат).
		if (checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return F;
		// Инвариант дна (замена прежнего чтения бедрока под ЯКОРЕМ, при переигрывании недоступного): генератор MC26
		// кладёт бедрок на minY всегда (SurfaceRules verticalGradient "bedrock_floor" bottom()..aboveBottom(5) —
		// SurfaceRuleData.java:280) → проверка дна ТЕКУЩЕГО чанка (всегда доступен) даёт тот же ответ, что дала бы
		// якорная: T на канонических генераторах, F на суперфлэт-подобных (интент Грега — отсечь миры без бедрока).
		if (!WD.bedrock(aWorld, aMinX+8, WD.minY(aWorld), aMinZ+8)) return F;

		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");

		if (tRegistry == null) return F;

		// Поиск якоря, чья область может накрывать текущий чанк: формула якоря 1:1 (период mMaxSize+4 > радиуса
		// области (2+mMaxSize)/2 → максимум один кандидат на ось в окне).
		int tCurChunkX = aMinX >> 4, tCurChunkZ = aMinZ >> 4, tReach = (2+mMaxSize)/2;
		int tAnchorChunkX = Integer.MIN_VALUE, tAnchorChunkZ = Integer.MIN_VALUE;
		for (int i = -tReach; i <= tReach && tAnchorChunkX == Integer.MIN_VALUE; i++) if (Math.abs(tCurChunkX+i)%(mMaxSize+4) == (mMaxSize+4)/2) tAnchorChunkX = tCurChunkX+i;
		for (int j = -tReach; j <= tReach && tAnchorChunkZ == Integer.MIN_VALUE; j++) if (Math.abs(tCurChunkZ+j)%(mMaxSize+4) == (mMaxSize+4)/2) tAnchorChunkZ = tCurChunkZ+j;
		if (tAnchorChunkX == Integer.MIN_VALUE || tAnchorChunkZ == Integer.MIN_VALUE) return F;
		int tAnchorMinX = tAnchorChunkX << 4, tAnchorMinZ = tAnchorChunkZ << 4;

		// Пороги данжа 1:1, но на ЯКОРНЫХ координатах (одинаковый вердикт из каждого чанка области).
		if (Math.abs(tAnchorMinZ) < 256+mMaxSize*16 && Math.abs(tAnchorMinX) < 256+mMaxSize*16) return F;
		if ((GENERATE_STREETS && WD.dimensionId(aWorld) == DIM_OVERWORLD) && (Math.abs(tAnchorMinX) < 256+mMaxSize*16 || Math.abs(tAnchorMinZ) < 256+mMaxSize*16)) return F;

		// Собственный детерминированный Random данжа: WD.random-центр (двойной Random Грега) по seed мира ^ dim ^ соль.
		Random tRandom = WD.random(WD.seed(aWorld) ^ WD.dimensionId(aWorld) ^ "gt.dungeon".hashCode(), tAnchorChunkX, tAnchorChunkZ);
		if (tRandom.nextInt(mProbability) != 0) return F;

		// F6 §4.1: окно глубины данжа [mMinY..mMaxY] (старый мир) растягивается sea-anchored под MC26.
		int tRMinY = WD.remapY(aWorld, mMinY), tRMaxY = WD.remapY(aWorld, mMaxY);
		int tOffsetY = tRMinY + tRandom.nextInt(Math.max(1, tRMaxY-tRMinY)), tColor = tRandom.nextInt(16);
		
		BlockStones
		tPrimaryBlock   = (BlockStones)BlocksGT.stones[tRandom.nextInt(BlocksGT.stones.length)],
		tSecondaryBlock = (BlockStones)BlocksGT.stones[tRandom.nextInt(BlocksGT.stones.length)];

		HashSetNoNulls<BlockPos> tLightUpdateCoords = new HashSetNoNulls<>();
		HashSetNoNulls<TagData> tTags = new HashSetNoNulls<>(mTags);

		byte[][] tRoomLayout = new byte[2+mMinSize+tRandom.nextInt(1+mMaxSize-mMinSize)][2+mMinSize+tRandom.nextInt(1+mMaxSize-mMinSize)];
		
		boolean[] tGeneratedKeys = new boolean[5];
		
		if (!(mPortalNether                                               && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimensionId(aWorld) == DIM_NETHER))) tTags.add(TAG_PORTAL_NETHER);
		if (!(mPortalEnd                                                  && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimensionId(aWorld) == DIM_END   ))) tTags.add(TAG_PORTAL_END);
		if (!(mPortalTwilight && MD.TF.mLoaded                            && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimTF(aWorld)                         ))) tTags.add(TAG_PORTAL_TWILIGHT);
		if (!(mPortalAether   && (MD.AETHER.mLoaded || MD.AETHEL.mLoaded) && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimAETHER(aWorld)                     ))) tTags.add(TAG_PORTAL_AETHER);
		if (!(mPortalMyst     && MD.MYST.mLoaded)) tTags.add(TAG_PORTAL_MYST);
		
		long[] tKeyIDs = new long[tGeneratedKeys.length];
		// F6-worldgen (детерминизация ключей): было 1+Math.max(RNGSUS.nextInt(1000000), System.nanoTime()) — nanoTime
		// давал МЕЖ-данжевую уникальность ID, но невоспроизводим между переигрываниями чанков области. Дет-эквивалент:
		// long от якорного tRandom (>>>1 — неотрицательный) — тот же масштаб уникальности (2^63), разные данжи → разные
		// якоря → разные потоки → разные ID; семантика «ключ подходит только своему данжу» сохранена.
		tKeyIDs[0] = 1+(tRandom.nextLong()>>>1);
		for (int i = 1; i < tKeyIDs.length; i++) tKeyIDs[i] = tKeyIDs[i-1]-1;
		ItemStack[] tKeyStacks = new ItemStack[tKeyIDs.length];
		for (int i = 0; i < tKeyIDs.length; i++) tKeyStacks[i] = IL.KEYS[tRandom.nextInt(IL.KEYS.length)].getWithNameAndNBT(1, "Key #"+(i+1), UT.NBT.makeLong(NBT_KEY, tKeyIDs[i]));

		// База области = якорь минус пол-layout (1:1 прежнему aMinX -= (len/2)*16, но от якоря).
		int tBaseX = tAnchorMinX - (tRoomLayout   .length / 2) * 16;
		int tBaseZ = tAnchorMinZ - (tRoomLayout[0].length / 2) * 16;
		// Клетка текущего чанка в layout; вне области — данжа в этом чанке нет.
		int tCellI = tCurChunkX - (tBaseX >> 4), tCellJ = tCurChunkZ - (tBaseZ >> 4);
		if (tCellI < 0 || tCellI >= tRoomLayout.length || tCellJ < 0 || tCellJ >= tRoomLayout[0].length) return F;

		// Маркер у потолка (F6-Y-scale: был 254) — запись, значит только клетка-владелец (маска per-chunk).
		WD.set(aWorld, tBaseX+8+tCellI*16, WD.maxY(aWorld)-1, tBaseZ+8+tCellJ*16, NB, 0, 3);

		for (int i = 0, j = 0, k = -1, l = 0; k >= -IMPORTANT_ROOM_COUNT && l < 10000; l++) {
			i = 1+tRandom.nextInt(tRoomLayout   .length-2);
			j = 1+tRandom.nextInt(tRoomLayout[i].length-2);
			if (tRoomLayout[i][j] == 0) {tRoomLayout[i][j] = (byte)k--;}
		}

		int tRoomCount = 0;
		while (tRoomCount < 2) for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] == 0) if (tRandom.nextInt(mRoomChance) == 0) {tRoomLayout[i][j] = (byte)(1+tRandom.nextInt(ROOM_ID_COUNT)); tRoomCount++;}
		
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] != 0) {
			int a = i, b = j;
			while (a != tRoomLayout   .length/2) {a+=(a>(tRoomLayout   .length/2)?-1:+1); if (tRoomLayout[a][b] == 0) tRoomLayout[a][b] = -128; else break;}
			while (b != tRoomLayout[a].length/2) {b+=(b>(tRoomLayout[a].length/2)?-1:+1); if (tRoomLayout[a][b] == 0) tRoomLayout[a][b] = -128; else break;}
		}
		
		@SuppressWarnings("unchecked")
		java.util.Map<gregapi.oredict.OreDictMaterial, net.minecraft.world.item.ItemStack> tCoinMap = (java.util.Map<gregapi.oredict.OreDictMaterial, net.minecraft.world.item.ItemStack>)UT.Reflection.getFieldContent("gregtech.tileentity.placeables.MultiTileEntityCoin", "COIN_MAP", T, T);
		// F6-worldgen (детерминизация): no-index-форма UT.Code.select сидит на глобальном RNGSUS (UT.java:1333) —
		// невоспроизводима между переигрываниями (материал монет разошёлся бы по клеткам одного данжа). Явная
		// индексация от якорного tRandom — то же распределение Cu×3/Ag×2/Au×2/Pt×1, один материал на весь данж
		// (1:1-интент); индексная перегрузка select здесь неприменима (ambiguous с varargs-формой из-за боксинга).
		// Бросок ВНЕ тернарника — потребление tRandom безусловно (инвариант переигрывания).
		gregapi.oredict.OreDictMaterial[] tCoinMats = {MT.Cu, MT.Cu, MT.Cu, MT.Ag, MT.Ag, MT.Au, MT.Au, MT.Pt};
		gregapi.oredict.OreDictMaterial tCoinMat = tCoinMats[tRandom.nextInt(tCoinMats.length)];
		net.minecraft.world.item.ItemStack tCoinStack = tCoinMap == null ? null : tCoinMap.get(tCoinMat);
		CompoundTag tCoin = tCoinStack == null ? null : gregapi.code.ItemNBT.get(tCoinStack); // getTagCompound()->ItemNBT.get (neo NBT через DataComponents)
		if (tCoin == null) tCoin = UT.NBT.make(); else tCoin = (CompoundTag)tCoin.copy();
		
		boolean
		temp = T;
		while (temp) {
			temp = F;
			for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] == -128) {
				if (tRoomLayout[i+1][j  ] != 0 && tRoomLayout[i-1][j  ] != 0 && tRoomLayout[i  ][j-1] == 0 && tRoomLayout[i  ][j+1] == 0) continue;
				if (tRoomLayout[i+1][j  ] == 0 && tRoomLayout[i-1][j  ] == 0 && tRoomLayout[i  ][j-1] != 0 && tRoomLayout[i  ][j+1] != 0) continue;
				
				int tConnectionCount = 0;
				for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;
				
				if (tConnectionCount <= 1) {tRoomLayout[i][j] = 0; temp = T; continue;}
				
				if (tRoomLayout[i+1][j  ] != 0 && tRoomLayout[i+1][j+1] != 0 && tRoomLayout[i  ][j+1] != 0 && tRoomLayout[i-1][j  ] == 0 && tRoomLayout[i  ][j-1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
				if (tRoomLayout[i+1][j  ] != 0 && tRoomLayout[i+1][j-1] != 0 && tRoomLayout[i  ][j-1] != 0 && tRoomLayout[i-1][j  ] == 0 && tRoomLayout[i  ][j+1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
				if (tRoomLayout[i-1][j  ] != 0 && tRoomLayout[i-1][j+1] != 0 && tRoomLayout[i  ][j+1] != 0 && tRoomLayout[i+1][j  ] == 0 && tRoomLayout[i  ][j-1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
				if (tRoomLayout[i-1][j  ] != 0 && tRoomLayout[i-1][j-1] != 0 && tRoomLayout[i  ][j-1] != 0 && tRoomLayout[i+1][j  ] == 0 && tRoomLayout[i  ][j+1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
			}
		}
		temp = T;
		while (temp) {
			temp = F;
			for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] == -128) {
				if (tRoomLayout[i+1][j  ] != 0 && tRoomLayout[i-1][j  ] != 0 && tRoomLayout[i  ][j-1] == 0 && tRoomLayout[i  ][j+1] == 0) continue;
				if (tRoomLayout[i+1][j  ] == 0 && tRoomLayout[i-1][j  ] == 0 && tRoomLayout[i  ][j-1] != 0 && tRoomLayout[i  ][j+1] != 0) continue;
				
				int tConnectionCount = 0;
				for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;
				
				if (tConnectionCount <= 1) {tRoomLayout[i][j] = 0; temp = T; continue;}
				
				if (tRoomLayout[i+1][j+1] != 0) tConnectionCount++;
				if (tRoomLayout[i+1][j-1] != 0) tConnectionCount++;
				if (tRoomLayout[i-1][j+1] != 0) tConnectionCount++;
				if (tRoomLayout[i-1][j-1] != 0) tConnectionCount++;
				
				if (tConnectionCount >= 7) {tRoomLayout[i][j] = 0; temp = T; continue;}
				
				if (tConnectionCount == 5) {
					if (tRoomLayout[i+1][j-1] == 0 && tRoomLayout[i+1][j  ] == 0 && tRoomLayout[i+1][j+1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
					if (tRoomLayout[i-1][j-1] == 0 && tRoomLayout[i-1][j  ] == 0 && tRoomLayout[i-1][j+1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
					if (tRoomLayout[i-1][j+1] == 0 && tRoomLayout[i  ][j+1] == 0 && tRoomLayout[i+1][j+1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
					if (tRoomLayout[i-1][j-1] == 0 && tRoomLayout[i  ][j-1] == 0 && tRoomLayout[i+1][j-1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
				}
				
				if (tRoomLayout[i+1][j  ] != 0 && tRoomLayout[i+1][j+1] != 0 && tRoomLayout[i  ][j+1] != 0 && tRoomLayout[i-1][j  ] == 0 && tRoomLayout[i  ][j-1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
				if (tRoomLayout[i+1][j  ] != 0 && tRoomLayout[i+1][j-1] != 0 && tRoomLayout[i  ][j-1] != 0 && tRoomLayout[i-1][j  ] == 0 && tRoomLayout[i  ][j+1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
				if (tRoomLayout[i-1][j  ] != 0 && tRoomLayout[i-1][j+1] != 0 && tRoomLayout[i  ][j+1] != 0 && tRoomLayout[i+1][j  ] == 0 && tRoomLayout[i  ][j-1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
				if (tRoomLayout[i-1][j  ] != 0 && tRoomLayout[i-1][j-1] != 0 && tRoomLayout[i  ][j-1] != 0 && tRoomLayout[i+1][j  ] == 0 && tRoomLayout[i  ][j+1] == 0) {tRoomLayout[i][j] = 0; temp = T; continue;}
			}
		}
		
		// Layout финализирован (обе волны чистки прошли). Пустая клетка текущего чанка → этому чанку данж ничего
		// не пишет, переигрывание не нужно (Keys/Tags вне данж-клеток не потребляются) — ранний выход.
		if (tRoomLayout[tCellI][tCellJ] == 0) return F;

		// Оба клеточных цикла — 1:1 (порядок значим: комнаты мутируют mGeneratedKeys/mTags, коридоры их читают).
		// Каждая клетка ПЕРЕИГРЫВАЕТСЯ во всех чанках области; физически пишет лишь клетка текущего чанка
		// (aWrite=(i,j)==(tCellI,tCellJ) — маска DungeonData.mWrite). markUnsaved — запись → только владелец.
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] > 0) {
			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).markUnsaved();

			int tConnectionCount = 0;
			for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;

			DungeonData aData = new DungeonData(aWorld, tBaseX+i*16, tOffsetY, tBaseZ+j*16, this, tPrimaryBlock, tSecondaryBlock, tRegistry, tLightUpdateCoords, tTags, tKeyIDs, tKeyStacks, tGeneratedKeys, tRoomLayout, i, j, tConnectionCount, tColor, new Random(tRandom.nextLong()), tCoin, i == tCellI && j == tCellJ);

			switch(tRoomLayout[i][j]) {
			case ROOM_ID_COUNT:
				if (aData.mConnectionCount == 1) {
					// Generate a random Dead End
					List<IDungeonChunk> tList = new ArrayListNoNulls<>(DEAD_END);
					while (T) {
						try {if (tList.remove(tRandom.nextInt(tList.size())).generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
						try {if (tList.isEmpty() && ROOM_EMPTY              .generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
					}
					break;
				}
				// Generate a random Normal Room
				List<IDungeonChunk> tList = new ArrayListNoNulls<>(ROOMS);
				while (T) {
					try {if (tList.remove(tRandom.nextInt(tList.size())).generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
					try {if (tList.isEmpty() && ROOM_EMPTY              .generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
				}
				break;
			}

			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).markUnsaved();
		}
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] < 0) {
			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).markUnsaved();

			int tConnectionCount = 0;
			for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;

			DungeonData aData = new DungeonData(aWorld, tBaseX+i*16, tOffsetY, tBaseZ+j*16, this, tPrimaryBlock, tSecondaryBlock, tRegistry, tLightUpdateCoords, tTags, tKeyIDs, tKeyStacks, tGeneratedKeys, tRoomLayout, i, j, tConnectionCount, tColor, new Random(tRandom.nextLong()), tCoin, i == tCellI && j == tCellJ);

			switch(tRoomLayout[i][j]) {
			case -128: try {if (tConnectionCount == 4) CORRIDOR4.generate(aData); else if (tConnectionCount == 3) CORRIDOR3.generate(aData); else CORRIDOR.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			case   -2: try {ENTRANCE.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			case   -1: try {BARRACKS.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			}

			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).markUnsaved();
		}
		// F6-worldgen (пост-световой цикл 1.7.10 СНЯТ): (1) в пирамиде MC26 шаги INITIALIZE_LIGHT/LIGHT идут ПОСЛЕ
		// FEATURES (ChunkPyramid.java:36-37) — свет чанка полностью пересчитывается движком после фич, ручной
		// checkBlock из воркгена холост; (2) WD.update внутри цикла кастует мир к Level (WD.java:816
		// sendBlockUpdated) — на WorldGenRegion это ClassCastException, а клиент-апдейты из генерации бессмысленны:
		// чанк ещё не отправлен ни одному клиенту (тот же класс решения, что ADAPT-009 П1 — холостые апдейты).
		// tLightUpdateCoords продолжает собираться клеткой-владельцем (структура DungeonData 1:1) — потребителей нет.
		return T;
	}
	
	public static boolean setRandomBricks   (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, 3+aRandom.nextInt(3), 2);}
	public static boolean setStandardBrick  (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.BRICK, 2);}
	public static boolean setRedstoneBrick  (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.RSTBR, 3);}
	public static boolean setCrackedBrick   (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.CRACK, 2);}
	public static boolean setMossyBrick     (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.MBRIK, 2);}
	public static boolean setChiseledStone  (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.CHISL, 2);}
	public static boolean setStoneTiles     (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.TILES, 2);}
	public static boolean setSmallTiles     (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.STILE, 2);}
	public static boolean setSmallBricks    (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.SBRIK, 2);}
	public static boolean setSmoothBlock    (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.SMOTH, 2);}
	public static boolean setAirBlock       (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, NB, 0, 2);}
	
	public static boolean setRandomBricks   (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, 3+aRandom.nextInt(3), 2);}
	public static boolean setStandardBrick  (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.BRICK, 2);}
	public static boolean setRedstoneBrick  (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.RSTBR, 3);}
	public static boolean setCrackedBrick   (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.CRACK, 2);}
	public static boolean setMossyBrick     (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.MBRIK, 2);}
	public static boolean setChiseledStone  (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.CHISL, 2);}
	public static boolean setStoneTiles     (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.TILES, 2);}
	public static boolean setSmallTiles     (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.STILE, 2);}
	public static boolean setSmallBricks    (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.SBRIK, 2);}
	public static boolean setSmoothBlock    (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.SMOTH, 2);}
	public static boolean setAirBlock       (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, NB, 0, 2);}
	
	public static boolean setGlass          (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, BlocksGT.Glass, aData.mColor, 2);}
	public static boolean setGlowGlass      (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, BlocksGT.GlowGlass, aData.mColor, 2);}
	public static boolean setColored        (WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, BlocksGT.Concrete, aData.mColor, 2);}
	
	public static boolean setLampBlock(WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom, int aGenerateRedstoneBrick) {
		aData.mLightUpdateCoords.add(new BlockPos(aX, aY, aZ));
		if (aGenerateRedstoneBrick != 0) setRedstoneBrick(aWorld, aX, aY+aGenerateRedstoneBrick, aZ, aData, aRandom);
		WD.set(aWorld, aX, aY, aZ, aGenerateRedstoneBrick == 0 ? Blocks.REDSTONE_LAMP : Blocks.REDSTONE_LAMP, 0, 2);
		return T;
	}
	
	public static boolean setLampBlock(WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom, int aGenerateRedstoneBrick) {
		aData.mLightUpdateCoords.add(new BlockPos(aX, aY, aZ));
		if (aGenerateRedstoneBrick != 0) setRedstoneBrick(aWorld, aX, aY+aGenerateRedstoneBrick, aZ, aData, aRandom);
		WD.set(aWorld, aX, aY, aZ, aGenerateRedstoneBrick == 0 ? Blocks.REDSTONE_LAMP : Blocks.REDSTONE_LAMP, 0, 2);
		return T;
	}
	
	public static boolean setCoins(WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {
		for (int i = 0; i < 16; i++) aData.mCoin.putByte("gt.coin.stacksize."+i, (byte)(aRandom.nextInt(3) == 0 ? aRandom.nextInt(8) : 0));
		aData.mCoin.putByte("gt.coin.stacksize."+aRandom.nextInt(16), (byte)(1+aRandom.nextInt(8)));
		aData.mMTERegistryGT.mBlock.placeBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN, (short)32700, aData.mCoin, T, T);
		return T;
	}
	
	public static boolean setFlower(WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {
		int tIndex = aRandom.nextInt(BlocksGT.FLOWER_TILES.length);
		WD.set(aWorld, aX, aY, aZ, BlocksGT.FLOWER_TILES[tIndex], BlocksGT.FLOWER_METAS[tIndex], 2);
		return T;
	}
	
	public static boolean setFlowerPot(WorldGenLevel aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {
		int tIndex = aRandom.nextInt(BlocksGT.POT_FLOWER_TILES.length);
		// F16 flower-pot ЗАКРЫТ (BUG-039 v4): 1.7.10 «FLOWER_POT + TileEntityFlowerPot.func_145964_a» — в neo горшок
		// без BE, наполненный горшок = POTTED_*-блок; выбор — центр BlocksGT.potted (контент POT_FLOWER_TILES/METAS 1:1).
		// Прежний путь через mirror-класс был JPMS-миной (NCDFE при генерации данжа).
		Block tPotted = BlocksGT.potted(BlocksGT.POT_FLOWER_TILES[tIndex], BlocksGT.POT_FLOWER_METAS[tIndex]);
		WD.set(aWorld, aX, aY, aZ, tPotted == null ? Blocks.FLOWER_POT : tPotted, 0, 2);
		return T;
	}
	
	public static boolean setBlock(WorldGenLevel aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags) {
		WD.set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags);
		return T;
	}
	
	public static boolean setBlock(WorldGenLevel aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags, int aRotationCount) {
		WD.set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags);
		while (aRotationCount-->0) WD.rotateBlock(aWorld, aX, aY, aZ, FORGE_DIR[SIDE_Y_POS]); // F-tool-rotation центр (блок уже поставлен WD.set выше)
		return T;
	}
}
