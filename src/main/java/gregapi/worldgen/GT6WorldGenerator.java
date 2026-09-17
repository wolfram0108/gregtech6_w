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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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
				// Was getChunkFromBlockCoords(blockX,blockZ); neo's getChunk takes chunk coordinates, so the >>4 shift happens here
				// explicitly instead of inside the removed method.
				ChunkAccess tChunk = mWorld.getChunk((mMinX+7) >> 4, (mMinZ+7) >> 4);
				if (tChunk == null) return;
				Biome[][] tBiomes = new Biome[16][16];
				BiomeNameSet tBiomeNames = new BiomeNameSet();
				// The old 2D biome lookup and its null fallback (removed constants) are replaced by getBiome(BlockPos), which
				// never returns null for a loaded chunk; querying it needs each column's own surface height, not a chunk-wide value.
				for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
					// Holder.value() can throw if unbound, which practically never happens on a loaded chunk; no dedicated catch, it would
					// fall into the caller's general one.
					int tX = mMinX+i, tZ = mMinZ+j;
					// mWorld.getBiome uses BiomeManager, which adds a +-2-3 block noise jitter at biome borders for visuals/spawn;
					// that let swamp claim river water at chunk seams. ChunkAccess.getNoiseBiome gives the exact biome 1.7.10's genlayer did.
					Holder<Biome> tBiomeHolder = tChunk.getNoiseBiome(tX >> 2, mWorld.getHeight(Heightmap.Types.WORLD_SURFACE, tX, tZ) >> 2, tZ >> 2);
					tBiomes[i][j] = tBiomeHolder.value();
					tBiomeHolder.unwrapKey().ifPresent(k -> tBiomeNames.add(k.location().toString()));
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
					// Was a guard against a specific 1.7.10 chunk-corruption bug via a sentinel field value; neither the fields nor the
					// corruption class they guarded against exist in the modern engine.
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
				// Was a removed Forge getEntitiesWithinAABB/AABB-factory/setDead combo, replaced by their real neo equivalents;
				// removal is routed through the server thread via WD, since a worker thread must not mutate the entity list directly.
				WD.discardEntitiesSafely(mWorld, ItemEntity.class, new AABB(mMinX-32, 0, mMinZ-32, mMinX+48, 256, mMinZ+48), null);
				// The old manual heightmap-reset hack against snow killing stumps has no field left to reset; the modern Heightmap
				// recomputes on its own, seemingly without needing one, though this isn't behaviorally confirmed.
				tChunk.setUnsaved(true);
			}
		}
	}
	
	private static final List<Runnable> LIST = new ArrayListNoNulls<>();
	private static boolean LOCK = F;
	public static boolean PFAA = F, TFC = F;
	
	public static void generate(WorldGenLevel aWorld, int aX, int aZ, boolean aGalactiCraft) {
		// Was a dimensionId switch with a MIN_VALUE case meaning "world not ready"; a valid neo Level always returns a real
		// ResourceKey<Level> with no such not-ready signal.
		// (Level.java:95-97).
		ResourceKey<Level> tDim = aWorld.getLevel().dimension();
		if (tDim == Level.OVERWORLD) {generate(new WorldGenContainer(TFC ? GEN_TFC : PFAA ? GEN_PFAA : GENERATE_STONE ? GEN_GT : GEN_OVERWORLD, TFC ? ORE_TFC : PFAA ? ORE_PFAA : GENERATE_STONE ? null : ORE_OVERWORLD, DIM_OVERWORLD, aWorld, aX, aZ)); return;}
		if (tDim == Level.NETHER   ) {generate(new WorldGenContainer(GEN_NETHER, ORE_NETHER, DIM_NETHER, aWorld, aX, aZ)); return;}
		if (tDim == Level.END      ) {generate(new WorldGenContainer(GEN_END   , ORE_END   , DIM_END   , aWorld, aX, aZ)); return;}
		// The 1.7.10 'world not ready' signal branch has no neo analog (see comment above); deliberately omitted rather than
		// guessed at.

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
		
		
		// Was the removed 2D getBiomeGenForCoords plus the removed Biome.biomeName field; the real neo path resolves
		// the biome via LevelReader.getBiome + Holder.unwrapKey, at per-column surface height rather than dimension water level.
		Holder<Biome> aBiomeHolder = aWorld.getBiome(new BlockPos(aX+7, aWorld.getHeight(Heightmap.Types.WORLD_SURFACE, aX+7, aZ+7), aZ+7));
		String aBiomeName = aBiomeHolder.unwrapKey().map(k -> k.location().toString()).orElse("");
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
