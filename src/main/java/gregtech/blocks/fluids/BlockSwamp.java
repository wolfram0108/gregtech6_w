/**
 * Copyright (c) 2024 GregTech-6 Team
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

package gregtech.blocks.fluids;

import gregapi.block.IBlockExtendedMetaData;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.IL;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;

import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F5 форс движка (decisions/F5-fluids.md §5): движковые хуки — см. javadoc {@link BlockOcean}.
 */
public class BlockSwamp extends BlockWaterlike {
	public static boolean PLACEMENT_ALLOWED = F, FLOWS_OUT = T;

	public BlockSwamp(String aName, Fluid aFluid) {
		super(aName, aFluid, FLOWS_OUT, T);
		tickRate = 10;
	}

	// @Override
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		if (PLACEMENT_ALLOWED) {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 10+RNGSUS.nextInt(90)); // было scheduleBlockUpdate(x,y,z,block,delay)
		} else {
			WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
		}
	}

	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		PLACEMENT_ALLOWED = T;

		if (aWorld.hasChunksAt(aX-33, aY-33, aZ-33, aX+33, aY+33, aZ+33)) { // было doChunksNearChunkExist(x,y,z,33) — см. BlockOcean
			// ADAPT-009: холостые re-light + клиент-апдейт каждого тика воды сняты — обоснование см. BlockOcean.updateTick
			if (aY > WD.minY(aWorld)) { // F6-Y-scale: было aY > 0, дно neo = getMinY()
				if (WD.block(aWorld, aX, aY-1, aZ) == this) {
					aWorld.scheduleTick(new BlockPos(aX, aY-1, aZ), this, tickRate);
				}
			}
		} else {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, Math.max(600, tickRate));
			PLACEMENT_ALLOWED = F;
			return;
		}
		
		if (aY <= WD.minY(aWorld)) { // F6-Y-scale: было aY <= 0, дно neo = getMinY()
			updateFlow(aWorld, aX, aY, aZ, aRandom);
			PLACEMENT_ALLOWED = F;
			return;
		}
		
		Block tBlock;

		Holder<Biome> tBiome = aWorld.getBiome(new BlockPos(aX, aY, aZ)); // было getBiomeGenForCoords(x,z) (2D) — см. BlockOcean

		boolean tDirt = F;
		
		byte tSwampCounter = 0;
		ArrayListNoNulls<BlockPos> tList = new ArrayListNoNulls<>();
		for (byte tSide : ALL_SIDES_BUT_TOP) {
			tBlock = WD.block(aWorld, aX, aY, aZ, tSide);
			if (tBlock != NB) {
				byte tMeta = WD.meta(aWorld, aX, aY, aZ, tSide);
				if (tBlock == this) {
					if (tMeta == 0) tSwampCounter++;
				} else if (tBlock instanceof BlockWaterlike) {
					if (tMeta == 0 || tBlock instanceof BlockOcean) tSwampCounter++;
				} else if (tBlock == Blocks.WATER || tBlock == Blocks.WATER) {
					tList.add(new BlockPos(aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]));
					if (tMeta == 0) tSwampCounter++;
				} else if (gregapi.data.CS.Flattened.headOf(tBlock) == Blocks.SAND || gregapi.data.CS.Flattened.headOf(tBlock) == Blocks.DIRT || tBlock == Blocks.GRASS_BLOCK || tBlock == Blocks.MYCELIUM || tBlock == BlocksGT.Grass || tBlock == BlocksGT.Diggables || tBlock == BlocksGT.Sands || tBlock == BlocksGT.oreSand || tBlock == BlocksGT.oreRedSand || tBlock == BlocksGT.oreMud || tBlock == BlocksGT.oreSmallSand || tBlock == BlocksGT.oreSmallRedSand || tBlock == BlocksGT.oreSmallMud || IL.EtFu_Dirt.equal(tBlock)) {
					tDirt = T;
				} else if (IL.TF_Mazestone.equal(tBlock)) {
					// prevent flooding the Twilight Mazes.
					// grow a Mushroom Stem
					if (WD.air     (aWorld, aX  , aY+1, aZ  )) WD.set(aWorld, aX  , aY+1, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3); else
					if (WD.anywater(aWorld, aX  , aY+1, aZ  )) WD.set(aWorld, aX  , aY+1, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3);
					if (WD.air     (aWorld, aX  , aY+2, aZ  )) WD.set(aWorld, aX  , aY+2, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3); else
					if (WD.anywater(aWorld, aX  , aY+2, aZ  )) WD.set(aWorld, aX  , aY+2, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3);
					if (WD.air     (aWorld, aX  , aY+3, aZ  )) WD.set(aWorld, aX  , aY+3, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3); else
					if (WD.anywater(aWorld, aX  , aY+3, aZ  )) WD.set(aWorld, aX  , aY+3, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3);
					if (WD.air     (aWorld, aX  , aY+4, aZ  )) WD.set(aWorld, aX  , aY+4, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3); else
					if (WD.anywater(aWorld, aX  , aY+4, aZ  )) WD.set(aWorld, aX  , aY+4, aZ  , Blocks.BROWN_MUSHROOM_BLOCK, 10, 3);
					for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
						// Plug the holes.
						if (Math.abs(i * j) < 4) {
						if (WD.air     (aWorld, aX+i, aY  , aZ+j)) WD.set(aWorld, aX+i, aY  , aZ+j, Blocks.BROWN_MUSHROOM_BLOCK, 5, 3); else
						if (WD.anywater(aWorld, aX+i, aY  , aZ+j)) WD.set(aWorld, aX+i, aY  , aZ+j, Blocks.BROWN_MUSHROOM_BLOCK, 5, 3);
						if (WD.air     (aWorld, aX+i, aY+5, aZ+j)) WD.set(aWorld, aX+i, aY+5, aZ+j, Blocks.BROWN_MUSHROOM_BLOCK, 5, 3); else
						if (WD.anywater(aWorld, aX+i, aY+5, aZ+j)) WD.set(aWorld, aX+i, aY+5, aZ+j, Blocks.BROWN_MUSHROOM_BLOCK, 5, 3);
						} else {
						if (WD.anywater(aWorld, aX+i, aY  , aZ+j)) WD.set(aWorld, aX+i, aY  , aZ+j, NB, 0, 3);
						if (WD.anywater(aWorld, aX+i, aY+5, aZ+j)) WD.set(aWorld, aX+i, aY+5, aZ+j, NB, 0, 3);
						}
						// Soak it all up.
						if (WD.anywater(aWorld, aX+i, aY+4, aZ+j)) WD.set(aWorld, aX+i, aY+4, aZ+j, NB, 0, 3);
						if (WD.anywater(aWorld, aX+i, aY+3, aZ+j)) WD.set(aWorld, aX+i, aY+3, aZ+j, NB, 0, 3);
						if (WD.anywater(aWorld, aX+i, aY+2, aZ+j)) WD.set(aWorld, aX+i, aY+2, aZ+j, NB, 0, 3);
						if (WD.anywater(aWorld, aX+i, aY+1, aZ+j)) WD.set(aWorld, aX+i, aY+1, aZ+j, NB, 0, 3);
						if (WD.anywater(aWorld, aX+i, aY-1, aZ+j)) WD.set(aWorld, aX+i, aY-1, aZ+j, NB, 0, 3);
						if (WD.anywater(aWorld, aX+i, aY-2, aZ+j)) WD.set(aWorld, aX+i, aY-1, aZ+j, NB, 0, 3);
					}
					PLACEMENT_ALLOWED = F;
					return;
				}
			}
		}
		
		if (tDirt) for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			tBlock = WD.block(aWorld, aX+i, aY+j, aZ+k);
			if (gregapi.data.CS.Flattened.headOf(tBlock) == Blocks.SAND || gregapi.data.CS.Flattened.headOf(tBlock) == Blocks.DIRT || tBlock == Blocks.GRASS_BLOCK || tBlock == Blocks.MYCELIUM || IL.EtFu_Dirt.equal(tBlock)) {WD.set(aWorld, aX+i, aY+j, aZ+k, BlocksGT.Diggables, 0, 2); continue;}
			if (tBlock == BlocksGT.oreSand || tBlock == BlocksGT.oreRedSand) {BlocksGT.oreMud.placeBlock(aWorld, aX+i, aY+j, aZ+k, SIDE_UNKNOWN, ((IBlockExtendedMetaData)tBlock).getExtendedMetaData(aWorld, aX+i, aY+j, aZ+k), null, T, T); continue;}
			if (tBlock == BlocksGT.oreSmallSand || tBlock == BlocksGT.oreSmallRedSand) {BlocksGT.oreSmallMud.placeBlock(aWorld, aX+i, aY+j, aZ+k, SIDE_UNKNOWN, ((IBlockExtendedMetaData)tBlock).getExtendedMetaData(aWorld, aX+i, aY+j, aZ+k), null, T, T); continue;}
		}
		
		if (WD.meta(aWorld, aX, aY, aZ) == 0) {
			if (tSwampCounter <= 0 && !(WD.block(aWorld, aX, aY+1, aZ) instanceof BlockSwamp)) {
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				PLACEMENT_ALLOWED = F;
				return;
			}
		} else {
			if (tSwampCounter >= 2) {
				WD.set(aWorld, aX, aY, aZ, this, 0, WATER_UPDATE_FLAGS);
			}
		}
		
		if (BIOMES_INFINITE_WATER.contains(tBiome)) {
			tSwampCounter = 0;
			for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) if (i != 0 && j != 0) {
				if (WD.block(aWorld, aX+i, aY, aZ+j) instanceof BlockSwamp && WD.meta(aWorld, aX+i, aY, aZ+j) == 0) {
					tSwampCounter++;
				}
			}
			if (tSwampCounter < 3) {
				updateFlow(aWorld, aX, aY, aZ, aRandom);
				PLACEMENT_ALLOWED = F;
				return;
			}
		}
		
		for (BlockPos tCoords : tList) {
			// ADAPT (класс «признак сменил носитель», см. BlockWaterlike.canClaim): в 1.7.10 болото
			// упиралось в воду, которая САМА была биомом river/ocean/beach, и ветка BIOMES_INFINITE_WATER
			// выше его останавливала. В mc26 разлив лежит в биоме суши (замер: minecraft:savanna), список
			// мимо — и болото ело чужую воду без предела. Своя территория болота = биом болота.
			if (!canClaim(aWorld, tCoords.getX(), tCoords.getY(), tCoords.getZ())) continue;
			if (WD.set(aWorld, tCoords.getX(), tCoords.getY(), tCoords.getZ(), this, 0, WATER_UPDATE_FLAGS)) for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
				if (WD.exists(aWorld, tCoords.getX()+i, tCoords.getY(), tCoords.getZ()+j)) {
					tBlock = WD.block(aWorld, tCoords.getX()+i, tCoords.getY(), tCoords.getZ()+j);
					if (tBlock instanceof BlockSwamp) aWorld.scheduleTick(new BlockPos(tCoords.getX()+i, tCoords.getY(), tCoords.getZ()+j), this, tickRate);
				}
			}
		}
		
		updateFlow(aWorld, aX, aY, aZ, aRandom);
		PLACEMENT_ALLOWED = F;
		return;
	}
	
	/** Своя территория болота — биом болота ({@code BIOMES_SWAMP}, тот же центр, которым пользуется
	 *  {@link gregtech.worldgen.WorldgenSwamp}, где болото и создаётся). Обоснование класса и замер —
	 *  {@link BlockWaterlike#canClaim}. Уже стоящее болото за границей биома (ворлдген кладёт его по
	 *  чанкам, а чанк пересекает границу) остаётся на месте — правило запрещает ЗАХВАТ, а не существование. */
	@Override
	public boolean canClaim(Level aWorld, int aX, int aY, int aZ) {
		return BIOMES_SWAMP.contains(aWorld.getBiome(new BlockPos(aX, aY, aZ)));
	}

	@Override
	public void onHeadInside(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if (aEntity instanceof Slime) return;
		super.onHeadInside(aEntity, aWorld, aX, aY, aZ);
	}
	
	/** Болото гасит свет НАСМЕРТЬ в толще и как вода — на поверхности (оригинал `:198`:
	 *  {@code сверху НЕ болото || мета > 0 -> LIGHT_OPACITY_WATER; иначе LIGHT_OPACITY_MAX}).
	 *  <p>Движок спрашивает затухание ТОЛЬКО по состоянию (LightEngine.getOpacity:85 → state.getLightDampening,
	 *  заполняется при сборке состояния — BlockBehaviour.java:518), соседей в этом канале нет. Из двух условий
	 *  оригинала по состоянию выразимо одно — уровень: поток (мета > 0) гасит как вода, источник (мета 0) —
	 *  насмерть. ⚠️ Отличие от 1.7.10 ровно в один блок глубины: там верхний слой (над ним воздух, а не болото)
	 *  тоже гасил как вода, здесь он гасит насмерть. Условие «сверху болото» без соседей не проверить —
	 *  занесено в реестр отложенного, а не спрятано. */
	@Override public int getLightOpacity(net.minecraft.world.level.block.state.BlockState aState) {
		return aState.getValue(FLUID_META) > 0 ? LIGHT_OPACITY_WATER : LIGHT_OPACITY_MAX;
	}
	// getIcon НЕ переопределяем: тело оригинала (:199) ДОСЛОВНО совпадает с базовым (BlockWaterlike:200).
	// Своё у болота только тинт: getRenderColor 1:1 :200, позиционный colorMultiplier 1:1 :202-210.
	@Override public int getRenderColor(int aMeta) {return 0x0000ff00;}
	
	@Override
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {
		if (WD.block(aWorld, aX, aY+1, aZ) ==this) return 0x0000ff00;
		if (water(WD.block(aWorld, aX+1, aY, aZ))) return 0x0060ff60;
		if (water(WD.block(aWorld, aX-1, aY, aZ))) return 0x0060ff60;
		if (water(WD.block(aWorld, aX, aY, aZ+1))) return 0x0060ff60;
		if (water(WD.block(aWorld, aX, aY, aZ-1))) return 0x0060ff60;
		return 0x0000ff00;
	}
	
	public static boolean water(Block aBlock) {return aBlock == Blocks.WATER || aBlock == Blocks.WATER || aBlock == BlocksGT.Ocean || aBlock == BlocksGT.River;}
}
