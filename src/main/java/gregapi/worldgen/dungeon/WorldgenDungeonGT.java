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
// MultiTileEntityCoin is a content-layer class; a direct import here would leak core into content, so COIN_MAP is read
// by reflection on class name instead, GT6's usual cross-layer trick.
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
		INSTANCE = this; // redstone-wake (isDungeonAreaChunk); the one real registration is Loader_Worldgen
	}
	
	public WorldgenDungeonGT() {this(null, F, 100, 3, 7, 20, 20, 6, F, F, F, F, F, F, F, F);}

	/** The one real registration, done by Loader_Worldgen; kept for redstone-wake (see isDungeonAreaChunk). */
	public static WorldgenDungeonGT INSTANCE = null;

	/** Does this chunk belong to a potential dungeon area, mirroring generate's own thresholds? Needed since
	 *  WorldGenRegion sends no neighbor updates during generation, leaving wires born at POWER=0 until this wakes them. */
	public static boolean isDungeonAreaChunk(net.minecraft.server.level.ServerLevel aLevel, int aChunkX, int aChunkZ) {
		WorldgenDungeonGT tGen = INSTANCE;
		if (tGen == null) return F;
		int tReach = (2+tGen.mMaxSize)/2, tAX = Integer.MIN_VALUE, tAZ = Integer.MIN_VALUE;
		for (int i = -tReach; i <= tReach && tAX == Integer.MIN_VALUE; i++) if (Math.abs(aChunkX+i)%(tGen.mMaxSize+4) == (tGen.mMaxSize+4)/2) tAX = aChunkX+i;
		for (int j = -tReach; j <= tReach && tAZ == Integer.MIN_VALUE; j++) if (Math.abs(aChunkZ+j)%(tGen.mMaxSize+4) == (tGen.mMaxSize+4)/2) tAZ = aChunkZ+j;
		if (tAX == Integer.MIN_VALUE || tAZ == Integer.MIN_VALUE) return F;
		int tAnchorMinX = tAX << 4, tAnchorMinZ = tAZ << 4;
		if (Math.abs(tAnchorMinZ) < 256+tGen.mMaxSize*16 && Math.abs(tAnchorMinX) < 256+tGen.mMaxSize*16) return F;
		Random tRandom = WD.random(WD.seed(aLevel) ^ WD.dimensionId(aLevel) ^ "gt.dungeon".hashCode(), tAX, tAZ);
		return tRandom.nextInt(tGen.mProbability) == 0;
	}
	
	/** Settles a dungeon's redstone chain in a chunk, replacing 1.7.10's flags=3 torch notifications at populate time;
	 *  idempotent, since it just lets the chain reach equilibrium. */
	public static void wakeRedstone(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.world.level.chunk.LevelChunk aChunk) {
		WorldgenDungeonGT tGen = INSTANCE;
		if (tGen == null) return;
		int tMinY = WD.remapY(aLevel, tGen.mMinY) - 12, tMaxY = WD.remapY(aLevel, tGen.mMaxY) + 14;
		int tX0 = aChunk.getPos().getMinBlockX(), tZ0 = aChunk.getPos().getMinBlockZ();
		net.minecraft.core.BlockPos.MutableBlockPos tPos = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = tMinY; y <= tMaxY; y++) {
			tPos.set(tX0 + x, y, tZ0 + z);
			net.minecraft.world.level.block.Block tBlock = aChunk.getBlockState(tPos).getBlock();
			if (tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_WIRE || tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH
			 || tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_TORCH || tBlock == net.minecraft.world.level.block.Blocks.STICKY_PISTON
			 || tBlock == net.minecraft.world.level.block.Blocks.PISTON || tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_LAMP) {
				aLevel.updateNeighborsAt(tPos.immutable(), tBlock);
			}
		}
	}

	public static final int ROOM_ID_COUNT = 1, IMPORTANT_ROOM_COUNT = 2;
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		// A feature may only write +-1 chunk from the one generating, and unwrapping the region to ServerLevel
		// deadlocked, so every chunk deterministically replays it from a shared anchor-seeded Random, writing only its own cell.
		if (checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return F;
		// MC26 always places bedrock at minY, so checking the current chunk's own floor (always reachable) gives the same answer
		// the unreachable anchor-chunk check would have.
		if (!WD.bedrock(aWorld, aMinX+8, WD.minY(aWorld), aMinZ+8)) return F;

		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");

		if (tRegistry == null) return F;

		// Searches for an anchor whose area may cover the current chunk; the anchor formula is 1:1, since its period always
		// exceeds the area radius, giving at most one candidate per axis.
		int tCurChunkX = aMinX >> 4, tCurChunkZ = aMinZ >> 4, tReach = (2+mMaxSize)/2;
		int tAnchorChunkX = Integer.MIN_VALUE, tAnchorChunkZ = Integer.MIN_VALUE;
		for (int i = -tReach; i <= tReach && tAnchorChunkX == Integer.MIN_VALUE; i++) if (Math.abs(tCurChunkX+i)%(mMaxSize+4) == (mMaxSize+4)/2) tAnchorChunkX = tCurChunkX+i;
		for (int j = -tReach; j <= tReach && tAnchorChunkZ == Integer.MIN_VALUE; j++) if (Math.abs(tCurChunkZ+j)%(mMaxSize+4) == (mMaxSize+4)/2) tAnchorChunkZ = tCurChunkZ+j;
		if (tAnchorChunkX == Integer.MIN_VALUE || tAnchorChunkZ == Integer.MIN_VALUE) return F;
		int tAnchorMinX = tAnchorChunkX << 4, tAnchorMinZ = tAnchorChunkZ << 4;

		// Dungeon thresholds are 1:1, evaluated on the anchor's coordinates so every chunk in the area reaches the same verdict.
		if (Math.abs(tAnchorMinZ) < 256+mMaxSize*16 && Math.abs(tAnchorMinX) < 256+mMaxSize*16) return F;
		if ((GENERATE_STREETS && WD.dimensionId(aWorld) == DIM_OVERWORLD) && (Math.abs(tAnchorMinX) < 256+mMaxSize*16 || Math.abs(tAnchorMinZ) < 256+mMaxSize*16)) return F;

		// The dungeon's own deterministic Random, seeded through the central WD.random helper by world seed, dimension and a
		// dedicated salt.
		Random tRandom = WD.random(WD.seed(aWorld) ^ WD.dimensionId(aWorld) ^ "gt.dungeon".hashCode(), tAnchorChunkX, tAnchorChunkZ);
		if (tRandom.nextInt(mProbability) != 0) return F;

		// The dungeon's depth window stretches sea-anchored for MC26's taller world, same as the ore windows elsewhere.
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
		// nanoTime gave unique key IDs but isn't reproducible across chunk replays; a long from the anchor's own
		// deterministic Random gives the same scale of uniqueness while staying identical on every replay of the same dungeon.
		tKeyIDs[0] = 1+(tRandom.nextLong()>>>1);
		for (int i = 1; i < tKeyIDs.length; i++) tKeyIDs[i] = tKeyIDs[i-1]-1;
		ItemStack[] tKeyStacks = new ItemStack[tKeyIDs.length];
		for (int i = 0; i < tKeyIDs.length; i++) tKeyStacks[i] = IL.KEYS[tRandom.nextInt(IL.KEYS.length)].getWithNameAndNBT(1, "Key #"+(i+1), UT.NBT.makeLong(NBT_KEY, tKeyIDs[i]));

		// Area base is the anchor minus half the room layout, the same formula as before but measured from the anchor instead of
		// a fixed origin.
		int tBaseX = tAnchorMinX - (tRoomLayout   .length / 2) * 16;
		int tBaseZ = tAnchorMinZ - (tRoomLayout[0].length / 2) * 16;
		// Current chunk's cell in the layout; falling outside it means this chunk has no dungeon in it at all.
		int tCellI = tCurChunkX - (tBaseX >> 4), tCellJ = tCurChunkZ - (tBaseZ >> 4);
		if (tCellI < 0 || tCellI >= tRoomLayout.length || tCellJ < 0 || tCellJ >= tRoomLayout[0].length) return F;

		// Marker near the ceiling (was a hardcoded 254); writing it happens only for the owning cell, per the per-chunk mask.
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
		// The no-index select() form sits on a global Random, unreproducible across chunk replays; explicit indexing
		// from the anchor's Random gives the same distribution, keeping one material per dungeon, rolled unconditionally.
		gregapi.oredict.OreDictMaterial[] tCoinMats = {MT.Cu, MT.Cu, MT.Cu, MT.Ag, MT.Ag, MT.Au, MT.Au, MT.Pt};
		gregapi.oredict.OreDictMaterial tCoinMat = tCoinMats[tRandom.nextInt(tCoinMats.length)];
		net.minecraft.world.item.ItemStack tCoinStack = tCoinMap == null ? null : tCoinMap.get(tCoinMat);
		CompoundTag tCoin = tCoinStack == null ? null : gregapi.code.ItemNBT.get(tCoinStack); // getTagCompound()->ItemNBT.get (neo NBT via DataComponents)
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
		
		// Early-exiting on an empty cell is forbidden, since rooms write into neighboring cells (FarmMobs towers), so any layout
		// cell's chunk must still replay the dungeon for its neighbors' writes.

		// Both per-cell loops are 1:1, since their order matters (rooms mutate shared state corridors read, and a corridor
		// carves into a tower cell later); DungeonData's coordinate gate reproduces that order.
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] > 0) {
			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).setUnsaved(true);

			int tConnectionCount = 0;
			for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;

			DungeonData aData = new DungeonData(aWorld, tBaseX+i*16, tOffsetY, tBaseZ+j*16, this, tPrimaryBlock, tSecondaryBlock, tRegistry, tLightUpdateCoords, tTags, tKeyIDs, tKeyStacks, tGeneratedKeys, tRoomLayout, i, j, tConnectionCount, tColor, new Random(tRandom.nextLong()), tCoin, tCurChunkX, tCurChunkZ);

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

			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).setUnsaved(true);
		}
		for (int i = 1; i < tRoomLayout.length-1; i++) for (int j = 1; j < tRoomLayout[i].length-1; j++) if (tRoomLayout[i][j] < 0) {
			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).setUnsaved(true);

			int tConnectionCount = 0;
			for (byte tSide : ALL_SIDES_HORIZONTAL) if (tRoomLayout[i+OFFX[tSide]][j+OFFZ[tSide]] != 0) tConnectionCount++;

			DungeonData aData = new DungeonData(aWorld, tBaseX+i*16, tOffsetY, tBaseZ+j*16, this, tPrimaryBlock, tSecondaryBlock, tRegistry, tLightUpdateCoords, tTags, tKeyIDs, tKeyStacks, tGeneratedKeys, tRoomLayout, i, j, tConnectionCount, tColor, new Random(tRandom.nextLong()), tCoin, tCurChunkX, tCurChunkZ);

			switch(tRoomLayout[i][j]) {
			case -128: try {if (tConnectionCount == 4) CORRIDOR4.generate(aData); else if (tConnectionCount == 3) CORRIDOR3.generate(aData); else CORRIDOR.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			case   -2: try {ENTRANCE.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			case   -1: try {BARRACKS.generate(aData);} catch(Throwable e) {e.printStackTrace(ERR);} break;
			}

			if (i == tCellI && j == tCellJ) aWorld.getChunk((tBaseX >> 4) + i, (tBaseZ >> 4) + j).setUnsaved(true);
		}
		// 1.7.10's post-generation light-notification cycle is removed: MC26 already runs light after features,
		// and casting a WorldGenRegion to Level for it would throw a ClassCastException besides; nothing consumes that data now.
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
		// A filled flower pot is a POTTED_* block via the central BlocksGT.potted, not a BE method; the old mirror-class path
		// used to crash class loading during dungeon generation.
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
		while (aRotationCount-->0) WD.rotateBlock(aWorld, aX, aY, aZ, FORGE_DIR[SIDE_Y_POS]); // The shared tool-rotation placement center (the block is already placed by WD.set above).
		return T;
	}
}
