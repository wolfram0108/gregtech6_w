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

package gregapi.worldgen.dungeon;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import gregapi.data.CS.BlocksGT;
import gregapi.data.MT;
import gregapi.data.MT.OREMATS;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.UT;
import gregapi.worldgen.WorldgenOresBedrock;

/**
 * @author Gregorius Techneticies
 */
public class DungeonChunkRoomMiningBedrock extends DungeonChunkRoomEmpty {
	@Override
	public boolean generate(DungeonData aData) {
		if (aData.mTags.contains(WorldgenDungeonGT.TAG_MINING_BEDROCK) || !super.generate(aData)) return F;
		aData.mTags.add(WorldgenDungeonGT.TAG_MINING_BEDROCK);
		OreDictMaterial tMaterial = UT.Code.select(MT.Redstone, MT.Redstone, MT.S, MT.Fe2O3, MT.MnO2, MT.Apatite, OREMATS.Molybdenite, MT.OREMATS.Bauxite, MT.OREMATS.Sphalerite, MT.OREMATS.Tetrahedrite, MT.OREMATS.Cassiterite, MT.OREMATS.Garnierite, MT.OREMATS.Galena);
		// F6-worldgen (данжи per-chunk): generateVein читает дно СВОЕЙ клетки и пишет бедрок-руду НАПРЯМУЮ (мимо
		// aData) — при переигрывании чужим чанком клетка недоступна (регион ±1 чанк) → зовёт только клетка-владелец
		// (mWrite). Для остальных результат детерминирован инвариантом дна: bedrock_floor на minY всегда бедрок
		// (SurfaceRuleData.java:280), не-бедрок-генераторы данж отсёк проверкой дна текущего чанка в
		// WorldgenDungeonGT.generate → согласованный «успех» во всех переигрываниях. Расхождение Random внутри
		// клетки (generateVein потребляет aData.mRandom только у владельца) изолировано клеточным Random.
		if (aData.mWrite && !WorldgenOresBedrock.generateVein(tMaterial, aData.mWorld, WD.dimensionId(aData.mWorld), aData.mX, aData.mZ, aData.mRandom)) return F;
		
		boolean tBrass = aData.next1in2();

		// F6-Y-scale (живой тест: «шахта заполнена породой»): абсолютные уровни низа шахты в 1.7.10 писались от
		// бедрока Y=0 («5-aData.mY» = прокоп от абс Y=5 до комнаты; лифт/динамиты/руда на абс Y=0..7). В MC26 дно
		// на getMinY() (-64) → прежняя математика давала мёртвый цикл прокопа (5-mY>0 при mY<0) и низ шахты в толще
		// породы на Y≈3. Якорим все абсолютные уровни к дну мира (тот же приём, что WorldgenOresBedrock.generateVein:197).
		final int tFloor = WD.minY(aData.mWorld);
		for (int tY = tFloor+5-aData.mY; tY < 0; tY++) {
			for (int tX =  0; tX <= 15; tX++) for (int tZ =  0; tZ <= 15; tZ++) {
				if (tX ==  0) {
					aData.bricks(tX, tY, tZ);
				} else if (tX == 15) {
					aData.bricks(tX, tY, tZ);
				} else if (tZ ==  0) {
					aData.bricks(tX, tY, tZ);
				} else if (tZ == 15) {
					aData.bricks(tX, tY, tZ);
				} else if (tZ ==  7 && tX ==  2 && aData.mRoomLayout[aData.mRoomX-1][aData.mRoomZ  ] != 0) {
					aData.set   (tX, tY, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_X_POS));
				} else if (tZ ==  7 && tX == 13 && aData.mRoomLayout[aData.mRoomX+1][aData.mRoomZ  ] != 0) {
					aData.set   (tX, tY, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_X_NEG));
				} else if (tX ==  7 && tZ ==  2 && aData.mRoomLayout[aData.mRoomX  ][aData.mRoomZ-1] != 0) {
					aData.set   (tX, tY, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_Z_POS));
				} else if (tX ==  7 && tZ == 13 && aData.mRoomLayout[aData.mRoomX  ][aData.mRoomZ+1] != 0) {
					aData.set   (tX, tY, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_Z_NEG));
				} else if (tY == -1 && (tX == 2 || tX == 13 || tZ == 2 || tZ == 13)) {
					aData.bricks(tX, tY, tZ);
				} else {
					aData.air   (tX, tY, tZ);
				}
			}
		}
		
		for (int tX =  2; tX <= 13; tX++) for (int tZ =  2; tZ <= 13; tZ++) {
			if (tX ==  2) {
				if (tZ != 2 && tZ != 13) {
				aData.set   (tX,  1, tZ, tBrass ? BlocksGT.Bars_Brass : BlocksGT.Bars_Steel,  8);
				aData.set   (tX,  0, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_X_POS));
				} else {
				aData.lamp(tX,  0, tZ, -1);
				}
			} else if (tX == 13) {
				if (tZ != 2 && tZ != 13) {
				aData.set   (tX,  1, tZ, tBrass ? BlocksGT.Bars_Brass : BlocksGT.Bars_Steel,  4);
				aData.set   (tX,  0, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_X_NEG));
				} else {
				aData.lamp(tX,  0, tZ, -1);
				}
			} else if (tZ ==  2) {
				aData.set   (tX,  1, tZ, tBrass ? BlocksGT.Bars_Brass : BlocksGT.Bars_Steel,  2);
				aData.set   (tX,  0, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_Z_POS));
			} else if (tZ == 13) {
				aData.set   (tX,  1, tZ, tBrass ? BlocksGT.Bars_Brass : BlocksGT.Bars_Steel,  1);
				aData.set   (tX,  0, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_Z_NEG));
			} else {
				aData.air   (tX,  0, tZ);
			}
		}
		
		if (aData.mRoomLayout[aData.mRoomX+1][aData.mRoomZ] != 0 && aData.mRoomLayout[aData.mRoomX-1][aData.mRoomZ] != 0) {
			aData.air   ( 2,  1,  8);
			aData.air   (13,  1,  8);
			for (int tX = 3; tX <= 12; tX++) {
				aData.set   (tX,  0,  8, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_X_NEG));
				aData.set   (tX,  1,  8, tBrass ? BlocksGT.Bars_Brass : BlocksGT.Bars_Steel,  3);
			}
		} else if (aData.mRoomLayout[aData.mRoomX][aData.mRoomZ+1] != 0 && aData.mRoomLayout[aData.mRoomX][aData.mRoomZ-1] != 0) {
			aData.air   ( 8,  1,  2);
			aData.air   ( 8,  1, 13);
			for (int tZ = 3; tZ <= 12; tZ++) {
				aData.set   ( 8,  0, tZ, tBrass ? 8408 : 8410, UT.NBT.make(NBT_FACING, SIDE_Z_NEG));
				aData.set   ( 8,  1, tZ, tBrass ? BlocksGT.Bars_Brass : BlocksGT.Bars_Steel, 12);
			}
		}
		
		for (int tX =  5; tX <= 10; tX++) for (int tZ =  5; tZ <= 10; tZ++) {
			if ((tX !=  5 && tX != 10) || (tZ !=  5 && tZ != 10)) {
				aData.air   (tX, tFloor+4-aData.mY, tZ);
			}
		}
		
		for (int tX =  6; tX <=  9; tX++) for (int tZ =  6; tZ <=  9; tZ++) {
			aData.air   (tX, tFloor+3-aData.mY, tZ);
		}
		
		aData.set   ( 6, tFloor+3-aData.mY,  6, 32104, UT.NBT.make(NBT_FACING, SIDE_Y_POS, NBT_MODE, T));
		aData.set   ( 6, tFloor+3-aData.mY,  9, 32713, UT.NBT.make(NBT_FACING, SIDE_Y_POS, NBT_MODE, T));
		aData.set   ( 9, tFloor+3-aData.mY,  6, 32713, UT.NBT.make(NBT_FACING, SIDE_Y_POS, NBT_MODE, T));
		aData.set   ( 9, tFloor+3-aData.mY,  9, 32712, UT.NBT.make(NBT_FACING, SIDE_Y_POS, NBT_MODE, T));
		
		int[] tStart = {1, 11}, tEnd = {4, 14};
		for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
			for (int i = tStart[a]; i <= tEnd[a]; i++) for (int j = tStart[b]; j <= tEnd[b]; j++) {
				if (aData.next3in4()) {aData.set(BlocksGT.blockRaw, i, tFloor+5-aData.mY, j, tMaterial.mID);
				if (aData.next2in3()) {aData.set(BlocksGT.blockRaw, i, tFloor+6-aData.mY, j, tMaterial.mID);
				if (aData.next1in2()) {aData.set(BlocksGT.blockRaw, i, tFloor+7-aData.mY, j, tMaterial.mID);
				}}}
			}
		}
		
		return T;
	}
}
