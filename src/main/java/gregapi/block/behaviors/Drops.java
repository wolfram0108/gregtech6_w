/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.block.behaviors;

import gregapi.block.prefixblock.PrefixBlock;
import gregapi.block.prefixblock.PrefixBlockTileEntity;
import gregapi.code.ArrayListNoNulls;
import gregapi.util.ST;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class Drops {
	// F12-followup (block-split): в 1.7.10 ST.register шёл в конструкторе блока ДО mDrops (PrefixBlock.java:208/227
	// оригинала) → эагерный ST.item(Block) в конструкторе Drops был валиден. В neo BlockItem рождается ПОЗЖЕ блока
	// (RegisterEvent<Item> после RegisterEvent<Block>), а дефолтный Drops(this,...) строится в конструкторе блока
	// (PrefixBlock:269) → Item.byBlock в этот момент отдаёт Items.AIR (карта NeoForge наполняется при регистрации
	// BlockItem), и пустышка замораживалась навсегда: любой PrefixBlock с дефолтным mDrops (все broken-руды) дропал
	// ПУСТО. Храним ссылку как Object, Block→Item разрешается в момент дропа (реестр полон) — семантика выбора
	// дропа 1:1, сдвинут только момент разрешения. Тот же класс дефекта уже закрывался тем же приёмом в
	// MultiTileEntityRegistry:118 («ключ Item.byBlock(mBlock) = AIR»).
	public final Object mDropNormal, mDropSilkTouch, mDropFortune, mDropSilkFortune;
	public final boolean mFortunable, mPreferSilk;
	public final int mExpBase, mExpRandom;
	
	@Deprecated public Drops(Item  aDropNormal) {this(aDropNormal, aDropNormal, aDropNormal, aDropNormal, F, F, 0, 0);}
	@Deprecated public Drops(Block aDropNormal) {this(aDropNormal, aDropNormal, aDropNormal, aDropNormal, F, F, 0, 0);}
	@Deprecated public Drops(Block aDropNormal, Block aDropSilkTouch) {this(aDropNormal, aDropSilkTouch, aDropNormal, aDropSilkTouch, F, F, 0, 0);}
	@Deprecated public Drops(Item  aDropNormal, Block aDropSilkTouch) {this(aDropNormal, aDropSilkTouch, aDropNormal, aDropSilkTouch, F, F, 0, 0);}
	@Deprecated public Drops(Block aDropNormal, Item  aDropSilkTouch) {this(aDropNormal, aDropSilkTouch, aDropNormal, aDropSilkTouch, F, F, 0, 0);}
	@Deprecated public Drops(Item  aDropNormal, Item  aDropSilkTouch) {this(aDropNormal, aDropSilkTouch, aDropNormal, aDropSilkTouch, F, F, 0, 0);}
	
	public Drops(int aExpBase, int aExpRandom) {this(null, null, null, null, F, F, aExpBase, aExpRandom);}
	public Drops(Object aDropNormal, Object aDropSilkTouch, Object aDropFortune) {this(aDropNormal, aDropSilkTouch, aDropFortune, aDropFortune, T, F, 0, 0);}
	public Drops(Object aDropNormal, Object aDropSilkTouch, Object aDropFortune, int aExpBase, int aExpRandom) {this(aDropNormal, aDropSilkTouch, aDropFortune, aDropFortune, T, F, aExpBase, aExpRandom);}
	public Drops(Object aDropNormal, Object aDropSilkTouch, Object aDropFortune, Object aDropSilkFortune, boolean aFortunable, boolean aPreferSilk, int aExpBase, int aExpRandom) {
		mDropNormal      = aDropNormal     ;
		mDropSilkTouch   = aDropSilkTouch  ;
		mDropFortune     = aDropFortune    ;
		mDropSilkFortune = aDropSilkFortune;
		mFortunable      = aFortunable;
		mPreferSilk      = aPreferSilk;
		mExpBase         = Math.max(0, aExpBase);
		mExpRandom       = Math.max(0, aExpRandom);
	}
	
	public ArrayList<ItemStack> getDrops(PrefixBlock aBlock, Level aWorld, int aX, int aY, int aZ, int aFortune, boolean aSilkTouch) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (aTileEntity instanceof PrefixBlockTileEntity) return getDrops(aBlock, aWorld, aX, aY, aZ, aBlock.getMetaDataValue(aTileEntity), aTileEntity, aFortune, aSilkTouch);
		return ST.arraylist();
	}
	
	public ArrayList<ItemStack> getDrops(PrefixBlock aBlock, Level aWorld, int aX, int aY, int aZ, short aMetaData, BlockEntity aTileEntity, int aFortune, boolean aSilkTouch) {
		ArrayListNoNulls<ItemStack> rList = ST.arraylist();
		rList.add(ST.update(ST.make(item(aFortune>0?aSilkTouch?mDropFortune:mDropSilkFortune:aSilkTouch?mDropSilkTouch:mDropNormal), mPreferSilk&&aSilkTouch?1:mFortunable?1+RNGSUS.nextInt(aFortune+1):1, aMetaData, aTileEntity instanceof PrefixBlockTileEntity?((PrefixBlockTileEntity)aTileEntity).mItemNBT:null)));
		return rList;
	}

	/** Разрешение ссылки дропа (Block или Item) в Item — в момент дропа, когда реестр предметов уже полон.
	 *  AIR → null: Item.byBlock отдаёт Items.AIR для блока без предмета, а 1.7.10 getItemFromBlock отдавал null
	 *  (null не попадает в список — ST.make(null)=null, ArrayListNoNulls его пропускает). */
	private static Item item(Object aDrop) {Item rItem = aDrop instanceof Block ? ST.item((Block)aDrop) : (Item)aDrop; return rItem == net.minecraft.world.item.Items.AIR ? null : rItem;}
	
	public int getExp(PrefixBlock aBlock) {
		return mExpBase + RNGSUS.nextInt(1+mExpRandom);
	}
}
