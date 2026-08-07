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
import net.minecraft.world.level.WorldGenLevel;
import gregapi.util.WD;

import gregapi.block.IBlockPlacable;
import gregapi.block.metatype.BlockStones;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.OP;
import gregapi.fluid.FluidTankGT;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraft.world.level.material.Fluid;

import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
// F6-worldgen (данжи per-chunk, снятие Level-пленника): в 1.7.10 DungeonData extends WorldAndCoords (носитель
// полного World — populate-фаза это позволяла). В neo данж-код исполняется в Feature-фазе, где мир = WorldGenRegion,
// а разворот в ServerLevel (прежний super(aWorld.getLevel())) давал реентрантный дедлок getChunk().join (см.
// WorldgenDungeonGT.generate). WorldAndCoords — gameplay-база (BlockEntity и пр.), её тип менять нельзя → DungeonData
// (единственный центр данж-подсистемы: ВСЕ чтения/записи комнат идут через него — аудит: прямых WD.set/placeBlock
// мимо aData в комнатах ноль) объявляет носитель сам: mWorld = WorldGenLevel-регион. WD-центры и IBlockPlacable
// принимают LevelAccessor — комнаты не тронуты.
//
// Маска записи mWrite (механизм per-chunk «переигрывание»): движок разрешает фиче писать только ±1 чанк от
// генерируемого (neo ChunkPyramid.java:33 blockStateWriteRadius(1), WorldGenRegion.ensureCanWrite:225), а данж —
// до 9×9 чанков. Каждый чанк области ПЕРЕИГРЫВАЕТ весь данж детерминированно (общий Random от якоря — см.
// WorldgenDungeonGT), но ФИЗИЧЕСКИ пишет только клетку своего чанка: mWrite=T только у неё. Гейт стоит в приватных
// низах place/rotate НИЖЕ вычисления аргументов — потребление Random (next() в аргументах) идентично в пишущем и
// переигрывающем режимах, иначе клетки разошлись бы. Подавленная запись возвращает T («успех») — возвраты set
// нигде не ветвят поток комнат (аудит architecture/dungeons.md).
public class DungeonData {
	public final WorldGenLevel mWorld;
	public final int mX, mY, mZ;
	/** Владение клеткой: клетка совпадает с генерируемым чанком (гейт generateVein/light-координат). */
	public final boolean mWrite;
	/** Координатный гейт записи (см. низы place/rotate): чанк, который сейчас генерируется. */
	public final int mChunkX, mChunkZ;
	public final MultiTileEntityRegistry mMTERegistryGT;
	public final BlockStones mPrimary, mSecondary;
	public final byte mColor, mColorInversed, mRoomLayout[][];
	public final int mRoomX, mRoomZ, mConnectionCount;
	public final long mKeyIDs[];
	public final ItemStack mKeyStacks[];
	public final boolean mGeneratedKeys[];
	public final HashSetNoNulls<BlockPos> mLightUpdateCoords;
	public final HashSetNoNulls<TagData> mTags;
	public final WorldgenDungeonGT mStructure;
	public final CompoundTag mCoin;
	public final Random mRandom;

	public DungeonData(WorldGenLevel aWorld, int aX, int aY, int aZ, WorldgenDungeonGT aStructure, BlockStones aPrimaryBlock, BlockStones aSecondaryBlock, MultiTileEntityRegistry aRegistry, HashSetNoNulls<BlockPos> aLightUpdateCoords, HashSetNoNulls<TagData> aTags, long[] aKeyIDs, ItemStack[] aKeyStacks, boolean[] aGeneratedKeys, byte[][] aRoomLayout, int aRoomX, int aRoomZ, int aConnectionCount, int aColor, Random aRandom, CompoundTag aCoin, int aChunkX, int aChunkZ) {
		mWorld = aWorld; mX = aX; mY = aY; mZ = aZ; mChunkX = aChunkX; mChunkZ = aChunkZ;
		mWrite = (aX >> 4) == aChunkX && (aZ >> 4) == aChunkZ;
		mStructure = aStructure;
		mPrimary = aPrimaryBlock;
		mSecondary = aSecondaryBlock;
		mMTERegistryGT = aRegistry;
		mRoomLayout = aRoomLayout;
		mRoomX = aRoomX;
		mRoomZ = aRoomZ;
		mConnectionCount = aConnectionCount;
		mKeyIDs = aKeyIDs;
		mKeyStacks = aKeyStacks;
		mGeneratedKeys = aGeneratedKeys;
		mLightUpdateCoords = aLightUpdateCoords;
		mTags = aTags;
		mColor = UT.Code.bind4(aColor);
		mColorInversed = UT.Code.bind4(15-aColor);
		mCoin = aCoin;
		mRandom = aRandom;
	}
	
	// Низы записи (ЕДИНСТВЕННЫЕ точки, где данж-подсистема физически трогает мир) — КООРДИНАТНЫЙ гейт маски:
	// позицию пишет ТОЛЬКО тот чанк, которому она принадлежит (не «автор»-клетка!). Причина: комнаты пишут и в
	// СОСЕДНИЕ клетки (FarmMobs строит башни-платформы ±16 в клетках-нулях и коридорах, коридор вырезает себя в
	// башне ПОЗЖЕ — порядок «комнаты → коридоры» у Грега значим). Гейт по автору дал бы гонку порядка генерации
	// чанков (ферма, сгенерированная позже коридора, затирала бы его проход); координатный гейт воспроизводит
	// ПОЛНЫЙ порядок записей 1.7.10 внутри каждого чанка. Вызывать ТОЛЬКО с уже вычисленными аргументами: next()
	// потребляется в выражениях аргументов вызывателя, поэтому Random-цепочка идентична во всех переигрываниях.
	private boolean owned(int aX, int aZ) {return (aX >> 4) == mChunkX && (aZ >> 4) == mChunkZ;}
	private boolean place(IBlockPlacable aBlock, int aX, int aY, int aZ, byte aSide, short aMeta, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return !owned(aX, aZ) || aBlock.placeBlock(mWorld, aX, aY, aZ, aSide, aMeta, aNBT, aCauseBlockUpdates, aForcePlacement);
	}
	private boolean place(Block aBlock, int aX, int aY, int aZ, int aMeta, int aFlags) {
		return !owned(aX, aZ) || WD.set(mWorld, aX, aY, aZ, aBlock, aMeta, aFlags);
	}
	private boolean place(net.minecraft.world.level.block.state.BlockState aState, int aX, int aY, int aZ, int aFlags) {
		return !owned(aX, aZ) || WD.set(mWorld, aX, aY, aZ, aState, aFlags);
	}
	private void rotate(int aX, int aY, int aZ) {
		if (owned(aX, aZ)) WD.rotateBlock(mWorld, aX, aY, aZ, FORGE_DIR[SIDE_Y_POS]); // F-tool-rotation центр (блок уже поставлен низом place выше)
	}

	public int next(int aNumber) {return mRandom.nextInt(aNumber);}
	/** Gives a random positive StackSize */
	public int nextStack() {return 1+mRandom.nextInt(64);}
	/** Gives a random MetaData but biased towards the Dungeons Color. */
	public int nextMetaA() {return next1in3() ? mColor         : next(16);}
	/** Gives a random MetaData but biased towards the Dungeons Inverse Color. */
	public int nextMetaB() {return next1in3() ? mColorInversed : next(16);}
	public boolean next1in2() {return mRandom.nextBoolean();}
	public boolean next1in3() {return next(3)<1;}
	public boolean next1in4() {return next(4)<1;}
	public boolean next1in5() {return next(5)<1;}
	public boolean next1in6() {return next(6)<1;}
	public boolean next1in7() {return next(7)<1;}
	public boolean next1in8() {return next(8)<1;}
	public boolean next1in9() {return next(9)<1;}
	public boolean next2in3() {return next(3)<2;}
	public boolean next2in5() {return next(5)<2;}
	public boolean next2in7() {return next(7)<2;}
	public boolean next2in9() {return next(9)<2;}
	public boolean next3in4() {return next(4)<3;}
	public boolean next3in5() {return next(5)<3;}
	public boolean next3in7() {return next(7)<3;}
	public boolean next3in8() {return next(8)<3;}
	public boolean next4in5() {return next(5)<4;}
	public boolean next4in7() {return next(7)<4;}
	public boolean next4in9() {return next(9)<4;}
	public boolean next5in6() {return next(6)<5;}
	public boolean next5in7() {return next(7)<5;}
	public boolean next5in8() {return next(8)<5;}
	public boolean next5in9() {return next(9)<5;}
	public boolean next6in7() {return next(7)<6;}
	public boolean next7in8() {return next(8)<7;}
	public boolean next7in9() {return next(9)<7;}
	public boolean next8in9() {return next(9)<8;}
	
	public boolean bricks     (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, 3+next(3), 2);}
	public boolean cobbles    (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, 1+next(2), 2);}
	public boolean cobble     (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.COBBL, 2);}
	public boolean mossycobble(int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.MCOBL, 2);}
	public boolean brick      (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.BRICK, 2);}
	public boolean redstoned  (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.RSTBR, 3);}
	public boolean cracked    (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.CRACK, 2);}
	public boolean mossy      (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.MBRIK, 2);}
	public boolean chiseled   (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.CHISL, 2);}
	public boolean tiles      (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.TILES, 2);}
	public boolean smalltiles (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.STILE, 2);}
	public boolean smallbricks(int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.SBRIK, 2);}
	public boolean smooth     (int aX, int aY, int aZ, Block aPrimary, Block aSecondary) {return set(aX, aY, aZ, aY == 2 ? aSecondary : aPrimary, BlockStones.SMOTH, 2);}
	
	public boolean bricks     (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, 3+next(3), 2);}
	public boolean cobbles    (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, 1+next(2), 2);}
	public boolean cobble     (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.COBBL, 2);}
	public boolean mossycobble(int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.MCOBL, 2);}
	public boolean brick      (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.BRICK, 2);}
	public boolean redstoned  (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.RSTBR, 3);}
	public boolean cracked    (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.CRACK, 2);}
	public boolean mossy      (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.MBRIK, 2);}
	public boolean chiseled   (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.CHISL, 2);}
	public boolean tiles      (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.TILES, 2);}
	public boolean smalltiles (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.STILE, 2);}
	public boolean smallbricks(int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.SBRIK, 2);}
	public boolean smooth     (int aX, int aY, int aZ) {return set(aX, aY, aZ, aY == 2 ? mSecondary : mPrimary, BlockStones.SMOTH, 2);}
	public boolean air        (int aX, int aY, int aZ) {return set(aX, aY, aZ, NB, 0, 2);}
	
	public boolean glass      (int aX, int aY, int aZ) {return set(aX, aY, aZ, BlocksGT.Glass, mColor, 2);}
	public boolean glassglow  (int aX, int aY, int aZ) {return set(aX, aY, aZ, BlocksGT.GlowGlass, mColor, 2);}
	public boolean colored    (int aX, int aY, int aZ) {return set(aX, aY, aZ, BlocksGT.Concrete, mColor, 2);}
	
	// F6-worldgen (лампы данжа ГОРЯТ, заход #39): 1.7.10 ставил при aGenerateRedstoneBrick!=0 ОТДЕЛЬНЫЙ блок
	// Blocks.lit_redstone_lamp (ориг. DungeonData.java:160/166), а лампы «без своего кирпича» (ветка 0 — коридоры,
	// узором смежные с RSTBR) зажигал пост-цикл нотификаций WorldgenDungeonGT (в neo снят: Level-каст + LIGHT после
	// FEATURES). Движок 1.13+ разложил lit-блок в свойство REDSTONE_LAMP[LIT] — числовой метой не выразить → стейт-канал
	// WD.set(state). Итоговое состояние мира 1.7.10 = обе ветки горят; стабильность держит RSTBR-мост сигнала
	// (BlockStones.getSignal:754 = 15, 1:1 ориг. :733) — лампа без сигнала штатно потухнет при первом апдейте (тот же
	// self-healing, что в 1.7.10).
	public boolean lamp(int aX, int aY, int aZ, Block aPrimary, Block aSecondary, int aGenerateRedstoneBrick) {
		if (mWrite) mLightUpdateCoords.add(new BlockPos(mX+aX, mY+aY, mZ+aZ)); // маска: свет чинит только клетка-владелец
		if (aGenerateRedstoneBrick != 0) redstoned(aX, aY+aGenerateRedstoneBrick, aZ);
		return place(Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, Boolean.TRUE), mX+aX, mY+aY, mZ+aZ, 2);
	}

	public boolean lamp(int aX, int aY, int aZ, int aGenerateRedstoneBrick) {
		if (mWrite) mLightUpdateCoords.add(new BlockPos(mX+aX, mY+aY, mZ+aZ)); // маска: свет чинит только клетка-владелец
		if (aGenerateRedstoneBrick != 0) redstoned(aX, aY+aGenerateRedstoneBrick, aZ);
		return place(Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, Boolean.TRUE), mX+aX, mY+aY, mZ+aZ, 2);
	}
	
	public boolean coins(int aX, int aY, int aZ) {
		for (int i = 0; i < 16; i++) mCoin.putByte("gt.coin.stacksize."+i, (byte)(next1in3() ? next(8) : 0));
		mCoin.putByte("gt.coin.stacksize."+next(16), (byte)(1+next(8)));
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)32700, mCoin, T, T);
	}
	
	public boolean set(int aX, int aY, int aZ, long aMeta) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, null, T, T);
	}
	public boolean set(int aX, int aY, int aZ, byte aSide, long aMeta) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, null, T, T);
	}
	public boolean set(int aX, int aY, int aZ, long aMeta, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, null, aCauseBlockUpdates, aForcePlacement);
	}
	public boolean set(int aX, int aY, int aZ, byte aSide, long aMeta, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, null, aCauseBlockUpdates, aForcePlacement);
	}
	public boolean set(int aX, int aY, int aZ, long aMeta, CompoundTag aNBT) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, aNBT, T, T);
	}
	public boolean set(int aX, int aY, int aZ, byte aSide, long aMeta, CompoundTag aNBT) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, aNBT, T, T);
	}
	public boolean set(int aX, int aY, int aZ, long aMeta, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, aNBT, aCauseBlockUpdates, aForcePlacement);
	}
	public boolean set(int aX, int aY, int aZ, byte aSide, long aMeta, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(mMTERegistryGT.mBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, aNBT, aCauseBlockUpdates, aForcePlacement);
	}
	
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, long[] aMetas) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMetas[next(aMetas.length)], null, T, T);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, OreDictMaterial... aMaterials) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, aMaterials[next(aMaterials.length)].mID, null, T, T);
	}
	public boolean set(IBlockPlacable[] aBlocks, int aX, int aY, int aZ, long[] aMetas) {
		return place(aBlocks[next(aBlocks.length)], mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMetas[next(aMetas.length)], null, T, T);
	}
	public boolean set(IBlockPlacable[] aBlocks, int aX, int aY, int aZ, OreDictMaterial... aMaterials) {
		return place(aBlocks[next(aBlocks.length)], mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, aMaterials[next(aMaterials.length)].mID, null, T, T);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, long aMeta) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, null, T, T);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, byte aSide, long aMeta) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, null, T, T);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, long aMeta, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, null, aCauseBlockUpdates, aForcePlacement);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, byte aSide, long aMeta, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, null, aCauseBlockUpdates, aForcePlacement);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, long aMeta, CompoundTag aNBT) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, aNBT, T, T);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, byte aSide, long aMeta, CompoundTag aNBT) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, aNBT, T, T);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, long aMeta, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,SIDE_UNKNOWN, (short)aMeta, aNBT, aCauseBlockUpdates, aForcePlacement);
	}
	public boolean set(IBlockPlacable aBlock, int aX, int aY, int aZ, byte aSide, long aMeta, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ,aSide, (short)aMeta, aNBT, aCauseBlockUpdates, aForcePlacement);
	}
	
	public boolean obsidian(int aX, int aY, int aZ, boolean aGravity) {
		return set(aX, aY, aZ, Blocks.OBSIDIAN, 0, IL.NeLi_Obsidian.exists() ? IL.NeLi_Obsidian.block() : IL.NePl_Obsidian.block(), 0, IL.EtFu_Obsidian.block(), 0, !aGravity ? Blocks.OBSIDIAN : IL.RC_Crushed_Obsidian.exists() ? IL.RC_Crushed_Obsidian.block() : IL.HBM_Crushed_Obsidian.exists() ? IL.HBM_Crushed_Obsidian.block() : Blocks.OBSIDIAN, aGravity && IL.RC_Crushed_Obsidian.exists() ? 4 : 0);
	}
	
	public boolean flower(int aX, int aY, int aZ) {
		return flower(aX, aY, aZ, F, F);
	}
	public boolean flower(int aX, int aY, int aZ, boolean aGrassOnly, boolean aVanillaOnly) {
		if (BlocksGT.FlowersA != null) {
			if (aGrassOnly || BlocksGT.FlowersB == null) {
				if (next1in2()) return set(aX, aY, aZ, (Block)BlocksGT.FlowersA, next(BlocksGT.FlowersA.maxMeta()), 2);
			} else {
				switch (next(3)) {
				case 0: return set(aX, aY, aZ, (Block)BlocksGT.FlowersA, next(BlocksGT.FlowersA.maxMeta()), 2);
				case 1: return set(aX, aY, aZ, (Block)BlocksGT.FlowersB, next(BlocksGT.FlowersB.maxMeta()), 2);
				}
			}
		}
		int tIndex = next(BlocksGT.FLOWER_TILES.length);
		return set(aX, aY, aZ, BlocksGT.FLOWER_TILES[tIndex], BlocksGT.FLOWER_METAS[tIndex], 2);
	}
	
	public boolean ingots_or_plates(int aX, int aY, int aZ, long aStackSize, OreDictMaterial... aMaterials) {
		ItemStack aIngot = OP.ingot.mat(aMaterials[next(aMaterials.length)], aStackSize <= 0 ? nextStack() : UT.Code.bindStack(aStackSize));
		ItemStack aPlate = OP.plate.mat(aMaterials[next(aMaterials.length)], aStackSize <= 0 ? nextStack() : UT.Code.bindStack(aStackSize));
		if (ST.valid(aIngot)) return ST.valid(aPlate) && next1in2() ? set(aX, aY, aZ, 32085, ST.save(NBT_VALUE, aPlate)) : set(aX, aY, aZ, 32084, ST.save(NBT_VALUE, aIngot));
		return ST.valid(aPlate) && set(aX, aY, aZ, 32085, ST.save(NBT_VALUE, aPlate));
	}
	public boolean ingots(int aX, int aY, int aZ, long aStackSize, OreDictMaterial... aMaterials) {
		ItemStack aStack = OP.ingot.mat(aMaterials[next(aMaterials.length)], aStackSize <= 0 ? nextStack() : UT.Code.bindStack(aStackSize));
		return ST.valid(aStack) && set(aX, aY, aZ, 32084, ST.save(NBT_VALUE, aStack));
	}
	public boolean plates(int aX, int aY, int aZ, long aStackSize, OreDictMaterial... aMaterials) {
		ItemStack aStack = OP.plate.mat(aMaterials[next(aMaterials.length)], aStackSize <= 0 ? nextStack() : UT.Code.bindStack(aStackSize));
		return ST.valid(aStack) && set(aX, aY, aZ, 32085, ST.save(NBT_VALUE, aStack));
	}
	public boolean gemplates(int aX, int aY, int aZ, long aStackSize, OreDictMaterial... aMaterials) {
		ItemStack aStack = OP.plateGem.mat(aMaterials[next(aMaterials.length)], aStackSize <= 0 ? nextStack() : UT.Code.bindStack(aStackSize));
		return ST.valid(aStack) && set(aX, aY, aZ, 32086, ST.save(NBT_VALUE, aStack));
	}
	
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String aLootFront) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", aLootFront));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String aLootFront, ListTag aInventory) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", aLootFront, NBT_INV_LIST, aInventory));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String aLootFront, String aLootBack) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", aLootFront, "gt.dungeonloot.back", aLootBack));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String aLootFront, String aLootBack, ListTag aInventory) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", aLootFront, "gt.dungeonloot.back", aLootBack, NBT_INV_LIST, aInventory));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String[] aLootFront) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", UT.Code.select(ChestGenHooks.STRONGHOLD_LIBRARY, aLootFront)));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String[] aLootFront, ListTag aInventory) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", UT.Code.select(ChestGenHooks.STRONGHOLD_LIBRARY, aLootFront), NBT_INV_LIST, aInventory));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String[] aLootFront, String[] aLootBack) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", UT.Code.select(ChestGenHooks.STRONGHOLD_LIBRARY, aLootFront), "gt.dungeonloot.back", UT.Code.select(ChestGenHooks.STRONGHOLD_LIBRARY, aLootBack)));
	}
	public boolean shelf(int aX, int aY, int aZ, long aMeta, byte aFacing, String[] aLootFront, String[] aLootBack, ListTag aInventory) {
		return set(aX, aY, aZ, aMeta, UT.NBT.make(NBT_FACING, aFacing, "gt.dungeonloot.front", UT.Code.select(ChestGenHooks.STRONGHOLD_LIBRARY, aLootFront), "gt.dungeonloot.back", UT.Code.select(ChestGenHooks.STRONGHOLD_LIBRARY, aLootBack), NBT_INV_LIST, aInventory));
	}
	
	public boolean zpm(int aX, int aY, int aZ) {
		return zpm(aX, aY, aZ, next2in3());
	}
	public boolean zpm(int aX, int aY, int aZ, boolean aActive) {
		return mStructure.mZPM && set(aX, aY, aZ, 14999, UT.NBT.make(NBT_ACTIVE_ENERGY, aActive));
	}
	
	public boolean cup(int aX, int aY, int aZ, FL aFluid) {
		return set(aX, aY, aZ, 32739, FluidTankGT.writeToNBT(UT.NBT.make(NBT_COLOR, DYES_INT[mColor], NBT_PAINTED, T), NBT_TANK, aFluid == null ? null : aFluid.make(250)));
	}
	public boolean cup(int aX, int aY, int aZ, Fluid aFluid) {
		return set(aX, aY, aZ, 32739, FluidTankGT.writeToNBT(UT.NBT.make(NBT_COLOR, DYES_INT[mColor], NBT_PAINTED, T), NBT_TANK, FL.make(aFluid, 250)));
	}
	public boolean cup(int aX, int aY, int aZ, FL aFluid, Block aBlock, int aMeta) {
		if (aBlock != NB && aBlock != null && next1in2()) return set(aX, aY, aZ, aBlock, aMeta, 2);
		return cup(aX, aY, aZ, aFluid);
	}
	public boolean cup(int aX, int aY, int aZ, Fluid aFluid, Block aBlock, int aMeta) {
		if (aBlock != NB && aBlock != null && next1in2()) return set(aX, aY, aZ, aBlock, aMeta, 2);
		return cup(aX, aY, aZ, aFluid);
	}
	
	public boolean pot(int aX, int aY, int aZ) {
		int tIndex = next(BlocksGT.POT_FLOWER_TILES.length);
		// F16 flower-pot ЗАКРЫТ (BUG-039 v4): 1.7.10 наполнял горшок через TileEntityFlowerPot-BE — в neo наполненный
		// горшок = POTTED_*-блок; выбор — центр BlocksGT.potted (контент POT_FLOWER_TILES/METAS 1:1 оригинала).
		// FORCED-ADAPTATION: ветки GT6-цветов оригинала (50%: FlowersA/FlowersB по next1in2) требуют СВОИХ
		// potted-блоков + моделей (несоразмерно декоративной фиче; регистрация N блоков ради горшков в данжах) —
		// деградация принята: ванильное растение того же ролла; наполненность горшков данжа 1:1, видовой состав сужен.
		Block tPotted = BlocksGT.potted(BlocksGT.POT_FLOWER_TILES[tIndex], BlocksGT.POT_FLOWER_METAS[tIndex]);
		set(aX, aY, aZ, tPotted == null ? Blocks.FLOWER_POT : tPotted, 0, 2);
		return T;
	}
	public boolean pot(int aX, int aY, int aZ, Block aBlock, int aMeta) {
		if (aBlock != NB && aBlock != null && next1in2()) return set(aX, aY, aZ, aBlock, aMeta, 2);
		return pot(aX, aY, aZ);
	}
	
	/** Стейт-канал (зеркало WD.set(state) — для состояний, не выразимых метой: равновесие поршней двери и т.п.). */
	public boolean set(int aX, int aY, int aZ, net.minecraft.world.level.block.state.BlockState aState, int aFlags) {
		return place(aState, mX+aX, mY+aY, mZ+aZ, aFlags);
	}

	public boolean set(int aX, int aY, int aZ, Block aBlock) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ, 0, 2);
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock, int aMeta) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ, aMeta, 2);
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags) {
		return place(aBlock, mX+aX, mY+aY, mZ+aZ, aMeta, aFlags);
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags, int aRotationCount) {
		if (!set(aX, aY, aZ, aBlock, aMeta, aFlags)) return F;
		while (aRotationCount-->0) rotate(mX+aX, mY+aY, mZ+aZ);
		return T;
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock1, int aMeta1, Block aBlock2, int aMeta2) {
		return set(aX, aY, aZ, aBlock1, aMeta1, aBlock2, aMeta2, 2);
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock1, int aMeta1, Block aBlock2, int aMeta2, int aFlags) {
		if (aBlock1 == NB || aBlock1 == null) return set(aX, aY, aZ                 , aBlock2, aMeta2, aFlags);
		if (aBlock2 == NB || aBlock2 == null) return set(aX, aY, aZ, aBlock1, aMeta1                 , aFlags);
		switch(next(2)) {
		case  0: return set(aX, aY, aZ, aBlock1, aMeta1, aFlags);
		default: return set(aX, aY, aZ, aBlock2, aMeta2, aFlags);
		}
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock1, int aMeta1, Block aBlock2, int aMeta2, Block aBlock3, int aMeta3) {
		return set(aX, aY, aZ, aBlock1, aMeta1, aBlock2, aMeta2, aBlock3, aMeta3, 2);
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock1, int aMeta1, Block aBlock2, int aMeta2, Block aBlock3, int aMeta3, int aFlags) {
		if (aBlock1 == NB || aBlock1 == null) return set(aX, aY, aZ                 , aBlock2, aMeta2, aBlock3, aMeta3, aFlags);
		if (aBlock2 == NB || aBlock2 == null) return set(aX, aY, aZ, aBlock1, aMeta1                 , aBlock3, aMeta3, aFlags);
		if (aBlock3 == NB || aBlock3 == null) return set(aX, aY, aZ, aBlock1, aMeta1, aBlock2, aMeta2                 , aFlags);
		switch(next(3)) {
		case  0: return set(aX, aY, aZ, aBlock1, aMeta1, aFlags);
		case  1: return set(aX, aY, aZ, aBlock2, aMeta2, aFlags);
		default: return set(aX, aY, aZ, aBlock3, aMeta3, aFlags);
		}
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock1, int aMeta1, Block aBlock2, int aMeta2, Block aBlock3, int aMeta3, Block aBlock4, int aMeta4) {
		return set(aX, aY, aZ, aBlock1, aMeta1, aBlock2, aMeta2, aBlock3, aMeta3, aBlock4, aMeta4, 2);
	}
	public boolean set(int aX, int aY, int aZ, Block aBlock1, int aMeta1, Block aBlock2, int aMeta2, Block aBlock3, int aMeta3, Block aBlock4, int aMeta4, int aFlags) {
		if (aBlock1 == NB || aBlock1 == null) return set(aX, aY, aZ                 , aBlock2, aMeta2, aBlock3, aMeta3, aBlock4, aMeta4, aFlags);
		if (aBlock2 == NB || aBlock2 == null) return set(aX, aY, aZ, aBlock1, aMeta1                 , aBlock3, aMeta3, aBlock4, aMeta4, aFlags);
		if (aBlock3 == NB || aBlock3 == null) return set(aX, aY, aZ, aBlock1, aMeta1, aBlock2, aMeta2                 , aBlock4, aMeta4, aFlags);
		if (aBlock4 == NB || aBlock4 == null) return set(aX, aY, aZ, aBlock1, aMeta1, aBlock2, aMeta2, aBlock3, aMeta3                 , aFlags);
		switch(next(4)) {
		case  0: return set(aX, aY, aZ, aBlock1, aMeta1, aFlags);
		case  1: return set(aX, aY, aZ, aBlock2, aMeta2, aFlags);
		case  2: return set(aX, aY, aZ, aBlock3, aMeta3, aFlags);
		default: return set(aX, aY, aZ, aBlock4, aMeta4, aFlags);
		}
	}
}
