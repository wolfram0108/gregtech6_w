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
import net.minecraft.tileentity.TileEntityFlowerPot;
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
	public boolean generate(Level aWorld, LevelChunk aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (aRandom.nextInt(mProbability) != 0 || checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return F;
		if (Math.abs(aMinZ) < 256+mMaxSize*16 && Math.abs(aMinX) < 256+mMaxSize*16) return F;
		if ((GENERATE_STREETS && WD.dimensionId(aWorld) == DIM_OVERWORLD) && (Math.abs(aMinX) < 256+mMaxSize*16 || Math.abs(aMinZ) < 256+mMaxSize*16)) return F;
		// F6-Y-scale (КОРЕНЬ «данжи не генерятся»): бедрок MC26 на getMinY() (был Y=0) — проверка бедрока в центре якорится к дну мира.
		if (Math.abs(aMinX/16)%(mMaxSize+4) != (mMaxSize+4)/2 || Math.abs(aMinZ/16)%(mMaxSize+4) != (mMaxSize+4)/2 || !WD.bedrock(aWorld, aMinX+8, WD.minY(aWorld), aMinZ+8)) return F;
		
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		
		if (tRegistry == null) return F;
		
		// F6 §4.1: окно глубины данжа [mMinY..mMaxY] (старый мир) растягивается sea-anchored под MC26.
		int tRMinY = WD.remapY(aWorld, mMinY), tRMaxY = WD.remapY(aWorld, mMaxY);
		int tOffsetY = tRMinY + aRandom.nextInt(Math.max(1, tRMaxY-tRMinY)), tColor = aRandom.nextInt(16);
		
		BlockStones
		tPrimaryBlock   = (BlockStones)BlocksGT.stones[aRandom.nextInt(BlocksGT.stones.length)],
		tSecondaryBlock = (BlockStones)BlocksGT.stones[aRandom.nextInt(BlocksGT.stones.length)];
		
		HashSetNoNulls<BlockPos> tLightUpdateCoords = new HashSetNoNulls<>();
		HashSetNoNulls<TagData> tTags = new HashSetNoNulls<>(mTags);
		
		byte[][] tRoomLayout = new byte[2+mMinSize+aRandom.nextInt(1+mMaxSize-mMinSize)][2+mMinSize+aRandom.nextInt(1+mMaxSize-mMinSize)];
		
		boolean[] tGeneratedKeys = new boolean[5];
		
		if (!(mPortalNether                                               && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimensionId(aWorld) == DIM_NETHER))) tTags.add(TAG_PORTAL_NETHER);
		if (!(mPortalEnd                                                  && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimensionId(aWorld) == DIM_END   ))) tTags.add(TAG_PORTAL_END);
		if (!(mPortalTwilight && MD.TF.mLoaded                            && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimTF(aWorld)                         ))) tTags.add(TAG_PORTAL_TWILIGHT);
		if (!(mPortalAether   && (MD.AETHER.mLoaded || MD.AETHEL.mLoaded) && (WD.dimensionId(aWorld) == DIM_OVERWORLD || WD.dimAETHER(aWorld)                     ))) tTags.add(TAG_PORTAL_AETHER);
		if (!(mPortalMyst     && MD.MYST.mLoaded)) tTags.add(TAG_PORTAL_MYST);
		
		long[] tKeyIDs = new long[tGeneratedKeys.length];
		tKeyIDs[0] = 1+Math.max(RNGSUS.nextInt(1000000), System.nanoTime());
		for (int i = 1; i < tKeyIDs.length; i++) tKeyIDs[i] = tKeyIDs[i-1]-1;
		ItemStack[] tKeyStacks = new ItemStack[tKeyIDs.length];
		for (int i = 0; i < tKeyIDs.length; i++) tKeyStacks[i] = IL.KEYS[aRandom.nextInt(IL.KEYS.length)].getWithNameAndNBT(1, "Key #"+(i+1), UT.NBT.makeLong(NBT_KEY, tKeyIDs[i]));
		
		aMinX -= (tRoomLayout   .length / 2) * 16;
		aMinZ -= (tRoomLayout[0].length / 2) * 16;
		
		for (int i = 0; i < tRoomLayout.length; i++) for (int j = 0; j < tRoomLayout[i].length; j++) WD.set(aWorld, aMinX+8+i*16, WD.maxY(aWorld)-1, aMinZ+8+j*16, NB, 0, 3); // F6-Y-scale: маркер у потолка (был 254 = старый 255-1) → maxY-1.
		
		for (int i = 0, j = 0, k = -1, l = 0; k >= -IMPORTANT_ROOM_COUNT && l < 10000; l++) {
			i = 1+aRandom.nextInt(tRoomLayout   .length-2);
			j = 1+aRandom.nextInt(tRoomLayout[i].length-2);
			if (tRoomLayout[i][j] == 0) {tRoomLayout[i][j] = (byte)k--;}
		}
		
		int tRoomCount = 0;
		while (tRoomCount < 2) for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] == 0) if (aRandom.nextInt(mRoomChance) == 0) {tRoomLayout[i][j] = (byte)(1+aRandom.nextInt(ROOM_ID_COUNT)); tRoomCount++;}
		
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] != 0) {
			int a = i, b = j;
			while (a != tRoomLayout   .length/2) {a+=(a>(tRoomLayout   .length/2)?-1:+1); if (tRoomLayout[a][b] == 0) tRoomLayout[a][b] = -128; else break;}
			while (b != tRoomLayout[a].length/2) {b+=(b>(tRoomLayout[a].length/2)?-1:+1); if (tRoomLayout[a][b] == 0) tRoomLayout[a][b] = -128; else break;}
		}
		
		@SuppressWarnings("unchecked")
		java.util.Map<gregapi.oredict.OreDictMaterial, net.minecraft.world.item.ItemStack> tCoinMap = (java.util.Map<gregapi.oredict.OreDictMaterial, net.minecraft.world.item.ItemStack>)UT.Reflection.getFieldContent("gregtech.tileentity.placeables.MultiTileEntityCoin", "COIN_MAP", T, T);
		net.minecraft.world.item.ItemStack tCoinStack = tCoinMap == null ? null : tCoinMap.get(UT.Code.select(MT.NULL, MT.Cu, MT.Cu, MT.Cu, MT.Ag, MT.Ag, MT.Au, MT.Au, MT.Pt));
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
		
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] > 0) {
			aWorld.getChunk((aMinX >> 4) + i, (aMinZ >> 4) + j).markUnsaved();
			
			int tConnectionCount = 0;
			for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;
			
			DungeonData aData = new DungeonData(aWorld, aMinX+i*16, tOffsetY, aMinZ+j*16, this, tPrimaryBlock, tSecondaryBlock, tRegistry, tLightUpdateCoords, tTags, tKeyIDs, tKeyStacks, tGeneratedKeys, tRoomLayout, i, j, tConnectionCount, tColor, new Random(aRandom.nextLong()), tCoin);
			
			switch(tRoomLayout[i][j]) {
			case ROOM_ID_COUNT:
				if (aData.mConnectionCount == 1) {
					// Generate a random Dead End
					List<IDungeonChunk> tList = new ArrayListNoNulls<>(DEAD_END);
					while (T) {
						try {if (tList.remove(aRandom.nextInt(tList.size())).generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
						try {if (tList.isEmpty() && ROOM_EMPTY              .generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
					}
					break;
				}
				// Generate a random Normal Room
				List<IDungeonChunk> tList = new ArrayListNoNulls<>(ROOMS);
				while (T) {
					try {if (tList.remove(aRandom.nextInt(tList.size())).generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
					try {if (tList.isEmpty() && ROOM_EMPTY              .generate(aData)) break;} catch(Throwable e) {e.printStackTrace(ERR);}
				}
				break;
			}
			
			aWorld.getChunk((aMinX >> 4) + i, (aMinZ >> 4) + j).markUnsaved();
		}
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] < 0) {
			aWorld.getChunk((aMinX >> 4) + i, (aMinZ >> 4) + j).markUnsaved();
			
			int tConnectionCount = 0;
			for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;
			
			DungeonData aData = new DungeonData(aWorld, aMinX+i*16, tOffsetY, aMinZ+j*16, this, tPrimaryBlock, tSecondaryBlock, tRegistry, tLightUpdateCoords, tTags, tKeyIDs, tKeyStacks, tGeneratedKeys, tRoomLayout, i, j, tConnectionCount, tColor, new Random(aRandom.nextLong()), tCoin);
			
			switch(tRoomLayout[i][j]) {
			case -128: try {if (tConnectionCount == 4) CORRIDOR4.generate(aData); else if (tConnectionCount == 3) CORRIDOR3.generate(aData); else CORRIDOR.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			case   -2: try {ENTRANCE.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			case   -1: try {BARRACKS.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			}
			
			aWorld.getChunk((aMinX >> 4) + i, (aMinZ >> 4) + j).markUnsaved();
		}
		for (BlockPos tCoords : tLightUpdateCoords) {
			// F-lighting: 1.7.10 World.setLightValue(EnumSkyBlock,x,y,z,15) — ручная установка блок-света удалена из neo
			// (свет полностью управляется LevelLightEngine, вычисляется из эмиссии блоков; прямого сеттера нет). Прежний
			// форс «15» был предподсветкой источников — в neo обеспечивается самими светящимися блоками + движком света;
			// здесь оставляем лишь пересчёт в точке (checkBlock), значение движок выведет сам.
			aWorld.getLightEngine().checkBlock(tCoords);
			for (byte tSide : ALL_SIDES_MIDDLE) {
				// F-lighting: 1.7.10 World.func_147451_t (принудительный пересчёт света в точке) -> neo LevelLightEngine.checkBlock(BlockPos) (LevelLightEngine.java).
				aWorld.getLightEngine().checkBlock(new BlockPos(tCoords.getX()+OFFX[tSide], tCoords.getY()+OFFY[tSide], tCoords.getZ()+OFFZ[tSide]));
				WD.update(   aWorld, tCoords.getX()+OFFX[tSide], tCoords.getY()+OFFY[tSide], tCoords.getZ()+OFFZ[tSide]);
			}
		}
		return T;
	}
	
	public static boolean setRandomBricks   (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, 3+aRandom.nextInt(3), 2);}
	public static boolean setStandardBrick  (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.BRICK, 2);}
	public static boolean setRedstoneBrick  (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.RSTBR, 3);}
	public static boolean setCrackedBrick   (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.CRACK, 2);}
	public static boolean setMossyBrick     (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.MBRIK, 2);}
	public static boolean setChiseledStone  (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.CHISL, 2);}
	public static boolean setStoneTiles     (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.TILES, 2);}
	public static boolean setSmallTiles     (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.STILE, 2);}
	public static boolean setSmallBricks    (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.SBRIK, 2);}
	public static boolean setSmoothBlock    (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aSecondary : aPrimary, BlockStones.SMOTH, 2);}
	public static boolean setAirBlock       (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, NB, 0, 2);}
	
	public static boolean setRandomBricks   (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, 3+aRandom.nextInt(3), 2);}
	public static boolean setStandardBrick  (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.BRICK, 2);}
	public static boolean setRedstoneBrick  (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.RSTBR, 3);}
	public static boolean setCrackedBrick   (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.CRACK, 2);}
	public static boolean setMossyBrick     (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.MBRIK, 2);}
	public static boolean setChiseledStone  (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.CHISL, 2);}
	public static boolean setStoneTiles     (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.TILES, 2);}
	public static boolean setSmallTiles     (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.STILE, 2);}
	public static boolean setSmallBricks    (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.SBRIK, 2);}
	public static boolean setSmoothBlock    (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, aY == aData.mY+2 ? aData.mSecondary : aData.mPrimary, BlockStones.SMOTH, 2);}
	public static boolean setAirBlock       (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, NB, 0, 2);}
	
	public static boolean setGlass          (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, BlocksGT.Glass, aData.mColor, 2);}
	public static boolean setGlowGlass      (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, BlocksGT.GlowGlass, aData.mColor, 2);}
	public static boolean setColored        (Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {return WD.set(aWorld, aX, aY, aZ, BlocksGT.Concrete, aData.mColor, 2);}
	
	public static boolean setLampBlock(Level aWorld, int aX, int aY, int aZ, DungeonData aData, Block aPrimary, Block aSecondary, Random aRandom, int aGenerateRedstoneBrick) {
		aData.mLightUpdateCoords.add(new BlockPos(aX, aY, aZ));
		if (aGenerateRedstoneBrick != 0) setRedstoneBrick(aWorld, aX, aY+aGenerateRedstoneBrick, aZ, aData, aRandom);
		WD.set(aWorld, aX, aY, aZ, aGenerateRedstoneBrick == 0 ? Blocks.REDSTONE_LAMP : Blocks.REDSTONE_LAMP, 0, 2);
		return T;
	}
	
	public static boolean setLampBlock(Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom, int aGenerateRedstoneBrick) {
		aData.mLightUpdateCoords.add(new BlockPos(aX, aY, aZ));
		if (aGenerateRedstoneBrick != 0) setRedstoneBrick(aWorld, aX, aY+aGenerateRedstoneBrick, aZ, aData, aRandom);
		WD.set(aWorld, aX, aY, aZ, aGenerateRedstoneBrick == 0 ? Blocks.REDSTONE_LAMP : Blocks.REDSTONE_LAMP, 0, 2);
		return T;
	}
	
	public static boolean setCoins(Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {
		for (int i = 0; i < 16; i++) aData.mCoin.putByte("gt.coin.stacksize."+i, (byte)(aRandom.nextInt(3) == 0 ? aRandom.nextInt(8) : 0));
		aData.mCoin.putByte("gt.coin.stacksize."+aRandom.nextInt(16), (byte)(1+aRandom.nextInt(8)));
		aData.mMTERegistryGT.mBlock.placeBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN, (short)32700, aData.mCoin, T, T);
		return T;
	}
	
	public static boolean setFlower(Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {
		int tIndex = aRandom.nextInt(BlocksGT.FLOWER_TILES.length);
		WD.set(aWorld, aX, aY, aZ, BlocksGT.FLOWER_TILES[tIndex], BlocksGT.FLOWER_METAS[tIndex], 2);
		return T;
	}
	
	public static boolean setFlowerPot(Level aWorld, int aX, int aY, int aZ, DungeonData aData, Random aRandom) {
		int tIndex = aRandom.nextInt(BlocksGT.POT_FLOWER_TILES.length);
		WD.set(aWorld, aX, aY, aZ, Blocks.FLOWER_POT, 0, 2);
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity instanceof TileEntityFlowerPot) ((TileEntityFlowerPot)tTileEntity).func_145964_a(Item.byBlock(BlocksGT.POT_FLOWER_TILES[tIndex]), BlocksGT.POT_FLOWER_METAS[tIndex]);
		return T;
	}
	
	public static boolean setBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags) {
		WD.set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags);
		return T;
	}
	
	public static boolean setBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags, int aRotationCount) {
		WD.set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags);
		while (aRotationCount-->0) WD.rotateBlock(aWorld, aX, aY, aZ, FORGE_DIR[SIDE_Y_POS]); // F-tool-rotation центр (блок уже поставлен WD.set выше)
		return T;
	}
}
