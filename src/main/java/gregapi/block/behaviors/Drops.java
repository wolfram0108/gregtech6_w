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
	// In 1.7.10 the BlockItem existed before the block's own drops field was built, but neo registers it later, so resolving
	// Block-to-Item eagerly here used to freeze in AIR forever; it is now kept as an Object and resolved lazily at drop time.
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

	/** Resolves the drop reference to an Item only at drop time, once the item registry is full;
	 *  AIR becomes null here since 1.7.10's equivalent returned null for a block with no item, not AIR. */
	private static Item item(Object aDrop) {Item rItem = aDrop instanceof Block ? ST.item((Block)aDrop) : (Item)aDrop; return rItem == net.minecraft.world.item.Items.AIR ? null : rItem;}
	
	public int getExp(PrefixBlock aBlock) {
		return mExpBase + RNGSUS.nextInt(1+mExpRandom);
	}
}
