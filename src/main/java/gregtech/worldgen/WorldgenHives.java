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

package gregtech.worldgen;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.WorldGenLevel;

import gregapi.block.metatype.BlockStones;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.IL;
import gregapi.item.bumble.IItemBumbleBee;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.WorldgenObject;
import gregtech.blocks.fluids.BlockWaterlike;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
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
public class WorldgenHives extends WorldgenObject {
	@SafeVarargs
	public WorldgenHives(String aName, boolean aDefault, List<WorldgenObject>... aLists) {
		super(aName, aDefault, aLists);
	}
	
	@Override
	public boolean generate(WorldGenLevel aWorld, ChunkAccess aChunk, int aDimType, int aMinX, int aMinZ, int aMaxX, int aMaxZ, Random aRandom, Biome[][] aBiomes, Set<String> aBiomeNames) {
		if (checkForMajorWorldgen(aWorld, aMinX, aMinZ, aMaxX, aMaxZ)) return F;
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		if (tRegistry == null) return F;
		int tX = aMinX + aRandom.nextInt(16), tY = 0, tZ = aMinZ + aRandom.nextInt(16), tCount = 0;
		boolean rResult = F;
		
		
		switch (aDimType) {
		case DIM_EREBUS:
			tY = 16+aRandom.nextInt(96);
			if (!IL.ERE_Umberstone.equal(WD.block(aWorld, tX, tY, tZ))) return rResult;
			for (byte tSide : ALL_SIDES_VALID) {
				if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) return rResult;
				if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
			}
			return (tCount == 5 && placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, DYE_INT_Brown       , aRandom.nextBoolean()?500:0, aRandom)) || rResult;
		case DIM_BETWEENLANDS:
			tY = 16+aRandom.nextInt(96);
			if (!IL.BTL_Betweenstone.equal(WD.block(aWorld, tX, tY, tZ))) return rResult;
			for (byte tSide : ALL_SIDES_VALID) {
				if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) return rResult;
				if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
			}
			return (tCount == 5 && placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, DYE_INT_Green       , aRandom.nextBoolean()?500:0, aRandom)) || rResult;
		case DIM_ATUM:
			tY = 16+aRandom.nextInt(64);
			if (!IL.ATUM_Limestone.equal(WD.block(aWorld, tX, tY, tZ))) return rResult;
			for (byte tSide : ALL_SIDES_VALID) {
				if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) return rResult;
				if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
			}
			return (tCount == 5 && placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, DYE_INT_Yellow      ,   900, aRandom)) || rResult;
		case DIM_ENVM: case DIM_CW2_Caveland:
			tY = 16+aRandom.nextInt(96);
			if (WD.block(aWorld, tX, tY, tZ) != Blocks.NETHERRACK) return rResult;
			for (byte tSide : ALL_SIDES_VALID) {
				if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) return rResult;
				if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
			}
			return (tCount == 5 && placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, DYE_INT_LightGray   ,   500, aRandom)) || rResult;
		case DIM_AETHER:
			tY = 16+aRandom.nextInt(96);
			if (WD.getMaterial(WD.block(aWorld, tX, tY, tZ)) == Material.ground) return rResult;
			for (byte tSide : ALL_SIDES_VALID) {
				if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) return rResult;
				if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
			}
			return (tCount == 5 && placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, DYE_INT_Cyan        ,     0, aRandom)) || rResult;
		case DIM_NETHER:
			tY = 16+aRandom.nextInt(WD.bedrock(aWorld, tX, 255, tZ) ? 224 : 96);
			if (WD.block(aWorld, tX, tY, tZ) != Blocks.NETHERRACK) return rResult;
			for (byte tSide : ALL_SIDES_VALID) {
				if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) return rResult;
				if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
			}
			return (tCount == 5 && placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, 0xaa0000            ,   300, aRandom)) || rResult;
		case DIM_END:
			if (aRandom.nextInt(3) > 0) return F;
			for (tY = 16; tY < 128; tY++) if (WD.block(aWorld, tX, tY, tZ) == Blocks.END_STONE) {
				for (byte tSide : ALL_SIDES_VALID) {
					if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) return rResult;
					if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
				}
				if (tCount == 5) {
					rResult = placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, 0x00aaaa, 400, aRandom) || rResult;
					break;
				}
				tCount = 0;
			}
			return rResult;
		case DIM_OVERWORLD: case DIM_ALFHEIM: case DIM_TROPICS: case DIM_UNKNOWN: case DIM_TWILIGHT: case DIM_A97:
			for (tY = 8; tY < 28; tY++) {
				Block tBlock = WD.block(aWorld, tX, tY, tZ);
				if (WD.getMaterial(tBlock) == Material.rock && WD.opq(tBlock) && WD.stone(tBlock, WD.meta(aWorld, tX, tY, tZ))) {
					for (byte tSide : ALL_SIDES_VALID) {
						if (WD.liquid(aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide])) {tCount = 0; break;}
						if (WD.opq   (aWorld, tX+OFFX[tSide], tY+OFFY[tSide], tZ+OFFZ[tSide], F, T)) tCount++;
					}
					if (tCount == 5) {
						rResult = placeHive(tRegistry, aDimType, aWorld, tX, tY, tZ, DYE_INT_LightGray,   500, aRandom);
						break;
					}
					tCount = 0;
				}
			}
			
			for (tY = !aWorld.dimensionType().hasSkyLight() ? 80 : WD.topY(aWorld)-50; tY > 2; tY--) { // F6-Y-scale: no-arg getHeight()=COUNT в MC26 → topY (maxY+1).
				Block tContact = WD.block(aWorld, tX, tY, tZ);
				if (WD.getMaterial(tContact).isLiquid()) return rResult;
				if (tContact instanceof BlockStones && WD.meta(aWorld, tX, tY, tZ) != 0) return rResult;
				if (!WD.opaque(tContact) || WD.leaves(tContact, aWorld, tX, tY, tZ) || WD.wood(tContact, aWorld, tX, tY, tZ) || WD.getMaterial(tContact) == Material.ice || WD.getMaterial(tContact) == Material.wood || WD.getMaterial(tContact) == Material.leaves) continue;
				
				for (byte tSide : ALL_SIDES_HORIZONTAL_DOWN) {
					Block tBlock = WD.block(aWorld, tX+OFFX[tSide], tY-1+OFFY[tSide], tZ+OFFZ[tSide]);
					if (WD.hasCollide(aWorld, tX+OFFX[tSide], tY-1+OFFY[tSide], tZ+OFFZ[tSide], tBlock)) continue;
					
					
				//  for (String tName : aBiomeNames) if (BIOMES_SPACE.contains(tName))
				//  return placeHive(tRegistry, aWorld, tX, tY-1, tZ, 0x44bbbb          ,   400, aRandom) || rResult;
					if (tBlock == Blocks.WATER || tBlock == Blocks.WATER || tBlock instanceof BlockWaterlike)
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_LightBlue ,   100, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_MAGICAL.contains(tName) && (aDimType != DIM_ALFHEIM || aRandom.nextBoolean()))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Purple    ,   200, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_VOLCANIC.contains(tName))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Black     ,   300, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_END.contains(tName))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, 0x00aaaa          ,   400, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_NETHER.contains(tName))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, 0xaa0000          ,   300, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_SHROOM.contains(tName))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Pink      ,   800, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_OCEAN_BEACH.contains(tName) || BIOMES_LAKE.contains(tName))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_LightBlue ,   100, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_JUNGLE.contains(tName))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Green     ,   600, aRandom) || rResult;
					for (String tName : aBiomeNames) if (BIOMES_FROZEN.contains(tName))
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_White     ,   700, aRandom) || rResult;
					if (tContact == Blocks.MYCELIUM)
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Pink      ,   800, aRandom) || rResult;
					if (tContact == Blocks.SAND && WD.meta(aWorld, tX, tY, tZ) == 1)
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Red       ,   900, aRandom) || rResult;
					if (tContact == Blocks.SANDSTONE || WD.getMaterial(tContact) == Material.sand)
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Yellow    ,   900, aRandom) || rResult;
					if (tContact == Blocks.GRAVEL || WD.getMaterial(tContact) == Material.rock)
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_LightGray ,   500, aRandom) || rResult;
					if (tContact == Blocks.GRASS_BLOCK || WD.getMaterial(tContact) == Material.grass)
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, 0xffdd99          ,     0, aRandom) || rResult;
					if (tContact == Blocks.DIRT || WD.getMaterial(tContact) == Material.ground)
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Brown     ,     0, aRandom) || rResult;
					// Lets make the magical Bumbles the Default if all else fails, so technically they are obtainable, even though I literally just made sure they can't spawn under the big Mushrooms. XD
					return placeHive(tRegistry, aDimType, aWorld, tX, tY-1, tZ, DYE_INT_Purple    ,   200, aRandom) || rResult;
				}
				return rResult;
			}
			return rResult;
		}
		return rResult;
	}
	
	public boolean placeHive(MultiTileEntityRegistry aRegistry, int aDimType, WorldGenLevel aWorld, int aX, int aY, int aZ, int aColor, int aSpeciesID, Random aRandom) {
		CompoundTag aBumbleTag;
		if (aDimType == DIM_OVERWORLD) {
			aBumbleTag = IItemBumbleBee.Util.getBumbleGenes(WD.envTemp(aWorld, aX, aY, aZ), WD.biome(aWorld, aX, aZ), !!aWorld.dimensionType().hasSkyLight() && WD.precipitationHeight(aWorld, aX, aZ) <= aY + 5, aRandom);
		} else {
			// Just pick whichever is the current time of day, because most dimensions don't have passage of daytime.
			aBumbleTag = IItemBumbleBee.Util.getBumbleGenes(WD.envTemp(aWorld, aX, aY, aZ), WD.biome(aWorld, aX, aZ), !!aWorld.dimensionType().hasSkyLight() && WD.precipitationHeight(aWorld, aX, aZ) <= aY + 5, aWorld.getLevel().isBrightOutside(), !aWorld.getLevel().isBrightOutside(), aRandom); // isBrightOutside — Level-only; worldgen-приёмник WorldGenLevel -> итоговый ServerLevel через getLevel()
		}
		return aRegistry.mBlock.placeBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN, (short)32755, UT.NBT.make(NBT_COLOR, aColor, NBT_INV_LIST, UT.NBT.makeInv(((IItemBumbleBee)ItemsGT.BUMBLEBEES).bumbleProductStack(NI, (short)aSpeciesID, UT.Code.units(IItemBumbleBee.Util.getWorkForce(aBumbleTag), 10000, 10, T), 0), IItemBumbleBee.Util.setBumbleTag(ST.make(ItemsGT.BUMBLEBEES, 1, aSpeciesID+1), aBumbleTag), IItemBumbleBee.Util.setBumbleTag(ST.make(ItemsGT.BUMBLEBEES, IItemBumbleBee.Util.getOffspring(aBumbleTag), aSpeciesID), aBumbleTag)), NBT_PAINTED, T), F, T);
	}
}
