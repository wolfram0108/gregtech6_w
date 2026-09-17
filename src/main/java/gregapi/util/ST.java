/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregapi.util;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.FireBlock;

import net.minecraftforge.registries.DeferredRegister;
import gregapi.GT_API;
import gregapi.block.ItemBlockBase;
import gregapi.code.*;
import gregapi.data.*;
import gregapi.item.IItemGT;
import gregapi.item.IItemGTContainerTool;
import gregapi.item.IItemProjectile;
import gregapi.item.IItemUpdatable;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.item.multiitem.food.IFoodStat;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityCanDelegate;
import gregapi.wooddict.WoodDictionary;
import gregtech.worldgen.TwilightTreasureReplacer;
import ic2.api.item.IC2Items;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.advancements.Advancement;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import gt6mirror.minecraftforge.fluids.IFluidContainerItem;
import twilightforest.TFAchievementPage;

import java.util.*;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class ST {
	public static boolean TE_PIPES = F, BC_PIPES = F, TF_TREASURE = F;
	
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void checkAvailabilities() {
		try {
			cofh.api.transport.IItemDuct.class.getCanonicalName();
			TE_PIPES = T;
		} catch(Throwable e) {/**/}
		try {
			buildcraft.api.transport.IInjectable.class.getCanonicalName();
			BC_PIPES = T;
		} catch(Throwable e) {/**/}
		try {
			twilightforest.TFTreasure.class.getCanonicalName();
			TF_TREASURE = T;
		} catch(Throwable e) {/**/}
	}
	
	public static boolean equal (ItemStack aStack1, ItemStack aStack2) {return equal(aStack1, aStack2, F);}
	public static boolean equal (ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return aStack1 != null && aStack2 != null && equal_(aStack1, aStack2, aIgnoreNBT);}
	// 1.7.10 wrote 'any subtype' with a single wildcard-meta stack; flattening split each subtype into its own Item,
	// so matching now also asks CS.Flattened whether the two different items share the same family.
	public static boolean equal_(ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return (item_(aStack1) == item_(aStack2) ? equal(meta_(aStack1), meta_(aStack2)) : equalWildcardFamily(aStack1, aStack2)) && (aIgnoreNBT || (((nbt_(aStack1) == null) == (nbt_(aStack2) == null)) && (nbt_(aStack1) == null || nbt_(aStack1).equals(nbt_(aStack2)))));}
	private static boolean equalWildcardFamily(ItemStack aStack1, ItemStack aStack2) {return (meta_(aStack1) == W || meta_(aStack2) == W) && CS.Flattened.sameFamily(item_(aStack1), item_(aStack2));}
	
	public static boolean equal (ItemStack aStack, Item  aItem                             ) {return aStack != null && aItem  != null && equal_(aStack, aItem );}
	public static boolean equal (ItemStack aStack, Block aBlock                            ) {return aStack != null && aBlock != null && equal_(aStack, aBlock);}
	public static boolean equal_(ItemStack aStack, Item  aItem                             ) {return item_ (aStack) == aItem ;}
	public static boolean equal_(ItemStack aStack, Block aBlock                            ) {return block_(aStack) == aBlock;}
	public static boolean equal (ItemStack aStack, Item  aItem                 , long aMeta) {return aStack != null && aItem  != null && equal_(aStack, aItem , aMeta);}
	public static boolean equal (ItemStack aStack, Block aBlock                , long aMeta) {return aStack != null && aBlock != null && equal_(aStack, aBlock, aMeta);}
	public static boolean equal_(ItemStack aStack, Item  aItem                 , long aMeta) {return equal(meta_(aStack), aMeta) && item_ (aStack) == aItem ;}
	public static boolean equal_(ItemStack aStack, Block aBlock                , long aMeta) {return equal(meta_(aStack), aMeta) && block_(aStack) == aBlock;}
	public static boolean equal (ItemStack aStack, ModData aModID, String aItem            ) {return equal(aStack, findItem(aModID.mID, aItem));}
	public static boolean equal (ItemStack aStack, ModData aModID, String aItem, long aMeta) {return equal(aStack, findItem(aModID.mID, aItem), aMeta);}
	
	public static boolean equal (ItemStack aStack, Item  aItem                             , boolean aAllowNBT) {return aStack != null && aItem  != null && equal_(aStack, aItem , aAllowNBT);}
	public static boolean equal (ItemStack aStack, Block aBlock                            , boolean aAllowNBT) {return aStack != null && aBlock != null && equal_(aStack, aBlock, aAllowNBT);}
	public static boolean equal_(ItemStack aStack, Item  aItem                             , boolean aAllowNBT) {return item_ (aStack) == aItem  && aAllowNBT == ItemNBT.has(aStack);}
	public static boolean equal_(ItemStack aStack, Block aBlock                            , boolean aAllowNBT) {return block_(aStack) == aBlock && aAllowNBT == ItemNBT.has(aStack);}
	public static boolean equal (ItemStack aStack, Item  aItem                 , long aMeta, boolean aAllowNBT) {return aStack != null && aItem  != null && equal_(aStack, aItem , aMeta, aAllowNBT);}
	public static boolean equal (ItemStack aStack, Block aBlock                , long aMeta, boolean aAllowNBT) {return aStack != null && aBlock != null && equal_(aStack, aBlock, aMeta, aAllowNBT);}
	public static boolean equal_(ItemStack aStack, Item  aItem                 , long aMeta, boolean aAllowNBT) {return equal(meta_(aStack), aMeta) && item_ (aStack) == aItem  && aAllowNBT == ItemNBT.has(aStack);}
	public static boolean equal_(ItemStack aStack, Block aBlock                , long aMeta, boolean aAllowNBT) {return equal(meta_(aStack), aMeta) && block_(aStack) == aBlock && aAllowNBT == ItemNBT.has(aStack);}
	public static boolean equal (ItemStack aStack, ModData aModID, String aItem            , boolean aAllowNBT) {return equal(aStack, findItem(aModID.mID, aItem), aAllowNBT);}
	public static boolean equal (ItemStack aStack, ModData aModID, String aItem, long aMeta, boolean aAllowNBT) {return equal(aStack, findItem(aModID.mID, aItem), aMeta, aAllowNBT);}
	
	public static boolean equal (long aMeta1, long aMeta2) {return aMeta1 == aMeta2 || aMeta1 == W || aMeta2 == W;}
	
	public static boolean equalTools (ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return aStack1 != null && aStack2 != null && equalTools_(aStack1, aStack2, aIgnoreNBT);}
	public static boolean equalTools_(ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return item_(aStack1) == item_(aStack2) && equal(meta_(aStack1), meta_(aStack2)) && (aIgnoreNBT || item_(aStack1) instanceof IItemGTContainerTool || (((nbt_(aStack1) == null) == (nbt_(aStack2) == null)) && (nbt_(aStack1) == null || nbt_(aStack1).equals(nbt_(aStack2)))));}
	
	public static boolean identical (ItemStack aStack1, ItemStack aStack2) {return aStack1 == aStack2 || (aStack1 != null && aStack2 != null && identical_(aStack1, aStack2));}
	public static boolean identical_(ItemStack aStack1, ItemStack aStack2) {return aStack1.getCount() == aStack2.getCount() && equal_(aStack1, aStack2, F);}
	
	public static boolean isGT (Item aItem) {return aItem instanceof IItemGT;}
	public static boolean isGT (Block aBlock) {return aBlock instanceof IItemGT;}
	public static boolean isGT (ItemStack aStack) {return aStack != null && isGT_(aStack);}
	public static boolean isGT_(ItemStack aStack) {return isGT(aStack.getItem());}
	
	public static boolean   valid(Block aBlock) {return aBlock != null && aBlock != NB;}
	public static boolean invalid(Block aBlock) {return aBlock == null || aBlock == NB;}
	public static boolean   valid(ItemStack aStack) {return aStack != null && !aStack.isEmpty() && item_(aStack) != null;} // Mirrors invalid below: a zeroed-out split remainder is not a valid stack.
	// 1.7.10 treated null as empty; neo's isEmpty() also covers a zeroed split remainder that isn't the EMPTY singleton.
	// Size-0 catalysts aren't affected: they keep count=1 with their own marker, never a plain zero count.
	public static boolean invalid(ItemStack aStack) {return aStack == null || aStack.isEmpty() || item_(aStack) == null;}
	
	public static ItemStack validate(ItemStack aStack) {return valid(aStack)                         ? aStack : null;}
	public static ItemStack valisize(ItemStack aStack) {return valid(aStack) && aStack.getCount() > 0 ? aStack : null;}

	/** GT6 stores an empty slot as null; the engine only tolerates the EMPTY singleton, and treats count==0 as ordinary.
	 *  The boundary here is by reference to EMPTY, not isEmpty(), since GT6 also gives count==0 its own separate meaning. */
	public static ItemStack nn(ItemStack aStack) {return aStack == null ? ItemStack.EMPTY : aStack;}
	/** Neo returns EMPTY where 1.7.10 returned null (cursor, Inventory.getItem).
	 *  This normalizes it back to null at the boundary, since GT6 bodies still reason in null semantics. */
	public static ItemStack n(ItemStack aStack) {return aStack == null || aStack.isEmpty() ? null : aStack;}
	/** Engine to GT6: the EMPTY singleton (and null) becomes null.
	 *  A meaningful count==0 stack that isn't that object crosses unchanged. */
	public static ItemStack ni(ItemStack aStack) {return (aStack == null || aStack == ItemStack.EMPTY) ? null : aStack;}

	public static short id (Item      aItem ) {return aItem  == null ? 0 : id_(aItem);}
	public static short id_(Item      aItem ) {return (short)Item.getId(aItem);}
	public static short id (Block     aBlock) {return aBlock == null ? 0 : id_(aBlock);}
	public static short id_(Block     aBlock) {return aBlock == NB   ? 0 : (short)BuiltInRegistries.BLOCK.getId(aBlock);}
	public static short id (ItemStack aStack) {return aStack == null ? 0 : id(item_(aStack));}
	
	public static Item item (ModData aModID, String aItem) {return item(make(aModID, aItem, 1, 0));}
	public static Item item (ModData aModID, String aItem, Item aReplacement) {Item rItem = item(aModID, aItem); return rItem == null ? aReplacement : rItem;}
	public static Item item (Block aBlock) {return aBlock == null ? null : item_(aBlock);}
	public static Item item_(Block aBlock) {return aBlock == NB   ? null : Item.byBlock(aBlock);}
	public static Item item (ItemStack aStack) {return aStack == null ? null : item_(aStack);}
	public static Item item_(ItemStack aStack) {return aStack.getItem();}
	public static Item item (long aID) {return aID > 0 && aID < 65536 ? item_(aID) : null;}
	public static Item item_(long aID) {return Item.byId((int)aID);}
	
	public static Block block (ModData aModID, String aBlock) {return block(make(aModID, aBlock, 1, 0));}
	public static Block block (ModData aModID, String aBlock, Block aReplacement) {Block rBlock = block(aModID, aBlock); return rBlock == NB ? aReplacement : rBlock;}
	public static Block block (Item aItem) {return aItem != null ? block_(aItem) : NB;}
	public static Block block_(Item aItem) {return Block.byItem(aItem);}
	public static Block block (ItemStack aStack) {return aStack != null ? block(item_(aStack)) : NB;}
	public static Block block_(ItemStack aStack) {return block_(item_(aStack));}
	public static Block block (long aID) {return aID > 0 && aID < 65536 ? block_(aID) : NB;}
	public static Block block_(long aID) {return BuiltInRegistries.BLOCK.byId((int)aID);}
	
	public static short     meta (ItemStack aStack) {return aStack == null ? 0 : meta_(aStack);}
	// Subtype lives in damage again, as in 1.7.10: 1.20.1's setDamage only clamps at zero, so it survives
	// isSameItemSameTags matching unlike 26.x. Item.getDamage(ItemStack) must never be overridden - it would recurse forever.
	public static short     meta_(ItemStack aStack) {return UT.Code.bindShort(aStack.getDamageValue());}
	public static ItemStack meta (ItemStack aStack, long aMeta) {return aStack == null ? null : meta_(aStack, aMeta);}
	// Writing meta 0 used to leave the stack untouched, like 1.7.10's field default; here it wrote a stray {Damage:0}
	// tag that blocked merging with an ordinary stack of the same item, so meta 0 now removes the key instead.
	public static ItemStack meta_(ItemStack aStack, long aMeta) {short tMeta = UT.Code.bindShort(aMeta); if (tMeta != 0) aStack.setDamageValue(tMeta); else gregapi.code.ItemNBT.field(aStack, ItemStack.TAG_DAMAGE, F); return aStack;}

	/** 1.7.10's identity for a display was item+damage; some GT6 families now need NBT too, or they collapse into duplicates.
	 *  Each item decides for itself via identityIncludesNBT(); no one else should hold a copy of this rule. */
	public static boolean identityIncludesNBT(net.minecraft.world.item.Item aItem) {
		return !(aItem instanceof gregapi.item.multiitem.MultiItem tMulti) || tMulti.identityIncludesNBT();
	}

	/** The same key an external display uses to match a recipe's output.
	 *  Item plus meta, and NBT only when identityIncludesNBT() says so. */
	public static String identityKey(ItemStack aStack) {
		if (aStack == null || aStack.getItem() == null) return "null";
		var tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem());
		String rKey = (tKey == null ? "" : tKey.toString()) + ":" + meta_(aStack);
		if (!identityIncludesNBT(aStack.getItem())) return rKey;
		net.minecraft.nbt.CompoundTag tNBT = gregapi.code.ItemNBT.get(aStack);
		return rKey + "|" + (tNBT == null ? "-" : tNBT.toString());
	}

	// Neo turns any count<=0 into AIR/EMPTY, so a size-0 catalyst is stored as count=1 plus a marker instead.
	// size() still reports 0 for a marked stack, matching the old behavior for an AIR catalyst.
	public static byte      size (ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null || aStack.getCount() < 0 ? 0 : (zerosize(aStack) ? 0 : UT.Code.bindByte(aStack.getCount()));}
	/** Logical count as an int, unclamped to a byte, for stacks holding thousands (mass storage).
	 *  A ZEROSIZE ghost still reads as 0. */
	public static int count(ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || aStack.getCount() < 0 ? 0 : (zerosize(aStack) ? 0 : aStack.getCount());}
	/** 1.7.10 stored this as a stack field, not NBT; the marker here goes through ItemNBT.field instead of the normal
	 *  item-NBT path, so a size-0 stack doesn't falsely report having NBT, which 1.7.10 never did either. */
	public static boolean zerosize(ItemStack aStack) {return ItemNBT.field(aStack, CS.NBT_ZEROSIZE);}
	private static void zerosize(ItemStack aStack, boolean aFlag) {ItemNBT.field(aStack, CS.NBT_ZEROSIZE, aFlag);}

	public static ItemStack size (long aSize, ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : size_(aSize, aStack);}
	// aSize<=0 keeps count=1 plus the ZEROSIZE marker; else neo collapses the stack to EMPTY/AIR and loses identity.
	// aSize>=1 is an ordinary setCount that clears the marker if the stack is reused.
	public static ItemStack size_(long aSize, ItemStack aStack) {
		if (aSize <= 0) {aStack.setCount(1); zerosize(aStack, T);}
		else {aStack.setCount((int)aSize); zerosize(aStack, F);}
		return aStack;
	}
	
	public static byte maxsize(ItemStack aStack) {return (byte)(aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? 64 : item_(aStack).getMaxStackSize(aStack));}
	
	public static ItemStack copy (ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : copy_(aStack);}
	/** 'Zero with remembered type' must only ever be the ZEROSIZE ghost (count=1+marker); a physical count<=0 is legal
	 *  only as a spent stack, never a copy template - neo's copy()/getItem() go blind on EMPTY/AIR there. */
	private static int sZeroCopyWarnings = 0;
	public static ItemStack copy_(ItemStack aStack) {
		if (aStack.getCount() <= 0 && aStack != ItemStack.EMPTY) {
			if (sZeroCopyWarnings < 10) {sZeroCopyWarnings++; ERR.println("[GT6] НАРУШЕНИЕ ИНВАРИАНТА F15-size0: копия физически-нулевого стека (потреблённый стек как шаблон?) — почини вызывателя: ноль с памятью типа пишется через ST.size_ (ZEROSIZE-призрак). Аварийная подушка сработала (" + sZeroCopyWarnings + "/10 предупреждений):"); new Throwable().printStackTrace(ERR);}
			int tOldCount = aStack.getCount();
			aStack.setCount(1);
			ItemStack rStack = aStack.copy();
			aStack.setCount(tOldCount);
			if (rStack == ItemStack.EMPTY) return rStack; // The object was real air - nothing to copy.
			rStack.setCount(Math.max(0, tOldCount));
			return rStack;
		}
		return aStack.copy();
	}
	
	public static ItemStack name (ItemStack aStack, String aName) {return aStack == null || aName == null ? aStack : name_(aStack, aName);}
	public static ItemStack name_(ItemStack aStack, String aName) {aStack.setHoverName(net.minecraft.network.chat.Component.literal(aName)); return aStack;}
	/** 1.7.10's setStackDisplayName(String) is now setHoverName(Component); this writes CUSTOM_NAME directly to keep
	 *  formatting that a plain literal(String) would lose, into the same tag.display.Name the original used. */
	public static ItemStack name_(ItemStack aStack, net.minecraft.network.chat.Component aName) {aStack.setHoverName(aName); return aStack;}

	public static CompoundTag nbt (ItemStack aStack) {return aStack == null ? null : nbt_(aStack);}
	public static CompoundTag nbt_(ItemStack aStack) {return ItemNBT.get(aStack);}
	public static ItemStack      nbt (ItemStack aStack, CompoundTag aNBT) {return aStack == null ? null : nbt_(aStack, aNBT);}
	public static ItemStack      nbt_(ItemStack aStack, CompoundTag aNBT) {return UT.NBT.set(aStack, aNBT);}
	
	public static ItemStack amount (long aSize, ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : amount_(aSize, aStack);}
	public static ItemStack amount_(long aSize, ItemStack aStack) {return size_(aSize, copy_(aStack));}
	
	public static ItemStack mul (long aMultiplier, ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : mul_(aMultiplier, aStack);}
	public static ItemStack mul_(long aMultiplier, ItemStack aStack) {return amount_(aStack.getCount() * aMultiplier, aStack);}
	
	public static ItemStack div (long aDivider, ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : div_(aDivider, aStack);}
	public static ItemStack div_(long aDivider, ItemStack aStack) {return amount_(aStack.getCount() / aDivider, aStack);}
	
	public static ItemStack validMeta (long aSize, ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : validMeta_(aSize, aStack);}
	public static ItemStack validMeta_(long aSize, ItemStack aStack) {return size_(aSize, validMeta_(aStack));}
	public static ItemStack validMeta (ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : validMeta_(aStack);}
	public static ItemStack validMeta_(ItemStack aStack) {return meta_(aStack) == W ? meta_(copy_(aStack), 0) : copy_(aStack);}
	
	public static Block nullair(Block aBlock) {return aBlock != NB ? aBlock : null;}
	
	public static int toInt(Item aItem, long aMeta) {return aItem == null ? 0 : id_(aItem) | (((short)aMeta)<<16);}
	public static int toInt(ItemStack aStack) {return aStack != null ? toInt(item_(aStack), meta_(aStack)) : 0;}
	public static int toInt(ItemStack aStack, long aMeta) {return aStack != null ? toInt(item_(aStack), aMeta) : 0;}
	
	public static ItemStack toStack(int aStack) {return make(toItem(aStack), 1, toMeta(aStack));}
	public static Block     toBlock(int aStack) {return block(aStack&(~0>>>16));}
	public static Item      toItem (int aStack) {return item(aStack&(~0>>>16));}
	public static short     toMeta (int aStack) {return (short)(aStack>>>16);}
	
	public static String regName (ItemStack aStack) {return regName(item(aStack));}
	public static String regName (Block     aBlock) {return regName(item(aBlock));}
	public static String regName (Item      aItem ) {return aItem == null ? null : regName_(aItem);}
	public static String regName_(Item      aItem ) {return BuiltInRegistries.ITEM.getKey(aItem).toString();}
	
	public static String regMeta (ItemStack aStack) {return invalid(aStack) ? "" : regName(item_(aStack))+":"+meta_(aStack);}
	public static String regMeta (Block     aBlock) {return aBlock  == null ? "" : regName(item_(aBlock))+":0";}
	public static String regMeta (Item      aItem ) {return aItem   == null ? "" : regName(aItem)+":0";}
	public static String regMeta_(Item      aItem ) {return regName(aItem)+":0";}
	
	public static boolean ownedBy (ModData aMod, BlockGetter aWorld, int aX, int aY, int aZ) {return aMod.mLoaded && ownedBy(aMod.mID, aWorld, aX, aY, aZ);}
	public static boolean ownedBy (ModData aMod, ItemStack    aStack                        ) {return aMod.mLoaded && ownedBy(aMod.mID, aStack);}
	public static boolean ownedBy (ModData aMod, Block        aBlock                        ) {return aMod.mLoaded && ownedBy(aMod.mID, aBlock);}
	public static boolean ownedBy (ModData aMod, Item         aItem                         ) {return aMod.mLoaded && ownedBy(aMod.mID, aItem);}
	public static boolean ownedBy (ModData aMod, String       aRegName                      ) {return aMod.mLoaded && ownedBy(aMod.mID, aRegName);}
	public static boolean ownedBy (String  aMod, BlockGetter aWorld, int aX, int aY, int aZ) {return ownedBy(aMod, WD.block(aWorld, aX, aY, aZ));}
	public static boolean ownedBy (String  aMod, ItemStack    aStack                        ) {return ownedBy(aMod, regName(aStack));}
	public static boolean ownedBy (String  aMod, Block        aBlock                        ) {return ownedBy(aMod, regName(aBlock));}
	public static boolean ownedBy (String  aMod, Item         aItem                         ) {return ownedBy(aMod, regName(aItem));}
	public static boolean ownedBy (String  aMod, String       aRegName                      ) {return aRegName != null && aMod != null && ownedBy_(aMod, aRegName);}
	public static boolean ownedBy_(String  aMod, String       aRegName                      ) {return aRegName.startsWith(aMod);}
	
	/** The single point through which the whole mod registers an Item.
	 *  Routed through the central GT_API bridge rather than a per-call DeferredRegister. */
	public static void register(Item aItem, String aRegistryName) {
		GT_API.registerItem(aItem, aRegistryName);
	}

	/** 1.7.10's setMaxStackSize mutated even a foreign item's field at runtime; 1.20.1's field is private final with no
	 *  setter, so this branch's Access Transformer opens it and writes straight in, exactly as the original did. */
	public static Item setMaxStackSize(Item aItem, int aSize) {
		if (aItem instanceof gregapi.item.ItemBase) return ((gregapi.item.ItemBase)aItem).setMaxStackSize(aSize);
		if (aItem != null) aItem.maxStackSize = aSize;
		return aItem;
	}

	/** 1.7.10 mutated a stack's live NBT tag directly; neo's copy-on-read tag means a plain mutation without writing
	 *  it back is silently lost, so these helpers do an atomic get-copy, put, and set-back instead. */
	public static void nbtPut(ItemStack aStack, String aKey, net.minecraft.nbt.Tag aValue) {
		CompoundTag tNBT = ItemNBT.get(aStack); if (tNBT == null) tNBT = new CompoundTag();
		tNBT.put(aKey, aValue); ItemNBT.set(aStack, tNBT);
	}
	public static void nbtPutByte(ItemStack aStack, String aKey, byte aValue) {
		CompoundTag tNBT = ItemNBT.get(aStack); if (tNBT == null) tNBT = new CompoundTag();
		tNBT.putByte(aKey, aValue); ItemNBT.set(aStack, tNBT);
	}
	/** 1.7.10's hasNBT/setTagCompound now route through the central ItemNBT.has/set. */
	public static boolean hasNBT(ItemStack aStack) {return ItemNBT.has(aStack);}
	public static void setNBT(ItemStack aStack, CompoundTag aNBT) {ItemNBT.set(aStack, aNBT);}

	/** 1.7.10 set a crafting remainder at runtime; neo's field is immutable after construction, and the modern model is
	 *  per-recipe instead, so this map only serves foreign-mod buckets, currently absent, and stays inert but visible. */
	public static final java.util.Map<Item, Item> VANILLA_CRAFTREMAINDER_OVERRIDES = new java.util.IdentityHashMap<>();
	public static Item setContainerItem(Item aItem, Item aContainer) {
		if (aItem != null && aContainer != null) VANILLA_CRAFTREMAINDER_OVERRIDES.put(aItem, aContainer);
		return aItem;
	}
	public static void register(Block aBlock, String aRegistryName) {register(aBlock, aRegistryName, null);}
	public static void register(Block aBlock, String aRegistryName, Class<? extends BlockItem> aItemClass) {
		GT_API.registerBlock(aBlock, aRegistryName, aItemClass == null ? ItemBlockBase.class : aItemClass);
		if (COMPAT_IC2 != null) COMPAT_IC2.addToExplosionWhitelist(aBlock);
	}

	/** The single point for item lookup by (modId,name); BuiltInRegistries.ITEM.getValue is @NonNull and returns AIR
	 *  for an unknown name, so containsKey is checked first to keep the original null-for-unknown contract. */
	/** Suffix of the companion key that stores a registry NAME next to a legacy numeric id. */
	private static final String REG_SUFFIX = ".reg";

	/** Writes a block into NBT by NAME (plus the legacy index, so older builds still read it):
	 *  the index is a registry position and shifts as soon as the set of installed items changes. */
	public static void putBlock(net.minecraft.nbt.CompoundTag aNBT, String aKey, Block aBlock) {
		if (aNBT == null || aBlock == null || aBlock == NB) return;
		aNBT.putInt(aKey, BuiltInRegistries.BLOCK.getId(aBlock));
		ResourceLocation tID = BuiltInRegistries.BLOCK.getKey(aBlock);
		if (tID != null) aNBT.putString(aKey + REG_SUFFIX, tID.toString());
	}

	/** Block written by {@link #putBlock}; the name decides, the legacy index is the fallback. */
	public static Block getBlock(net.minecraft.nbt.CompoundTag aNBT, String aKey) {
		if (aNBT == null) return NB;
		String tName = aNBT.getString(aKey + REG_SUFFIX);
		if (tName != null && !tName.isEmpty()) {
			ResourceLocation tID = ResourceLocation.tryParse(tName);
			if (tID != null && BuiltInRegistries.BLOCK.containsKey(tID)) return BuiltInRegistries.BLOCK.get(tID);
		}
		return BuiltInRegistries.BLOCK.byId(aNBT.getInt(aKey));
	}

	/** Full registry name of a block ("namespace:path"); empty string when it has none. */
	public static String blockRegName(Block aBlock) {
		if (aBlock == null || aBlock == NB) return "";
		ResourceLocation tID = BuiltInRegistries.BLOCK.getKey(aBlock);
		return tID == null ? "" : tID.toString();
	}

	/** Block by its full registry name; AIR-block constant for an empty or unknown name. */
	public static Block blockByRegName(String aRegName) {
		if (aRegName == null || aRegName.isEmpty()) return NB;
		ResourceLocation tID = ResourceLocation.tryParse(aRegName);
		return tID != null && BuiltInRegistries.BLOCK.containsKey(tID) ? BuiltInRegistries.BLOCK.get(tID) : NB;
	}

	/** Writes an ITEM reference held as a runtime registry index (covers, visuals) with its NAME beside it. */
	public static void putItemId(net.minecraft.nbt.CompoundTag aNBT, String aKey, short aID) {
		if (aNBT == null) return;
		aNBT.putShort(aKey, aID);
		Item tItem = item_((long)aID);
		if (tItem != null) {
			ResourceLocation tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID != null) aNBT.putString(aKey + REG_SUFFIX, tID.toString());
		}
	}

	/** Registry index for an item reference written by {@link #putItemId}: recomputed from the NAME when present. */
	public static short getItemId(net.minecraft.nbt.CompoundTag aNBT, String aKey) {
		if (aNBT == null) return 0;
		Item tItem = itemByRegName(aNBT.getString(aKey + REG_SUFFIX));
		return tItem == null ? aNBT.getShort(aKey) : id_(tItem);
	}

	/** Copies a block reference (name and legacy index) between two NBT tags. */
	public static void copyBlock(net.minecraft.nbt.CompoundTag aFrom, net.minecraft.nbt.CompoundTag aTo, String aKey) {
		if (aFrom == null || aTo == null) return;
		aTo.putInt(aKey, aFrom.getInt(aKey));
		String tName = aFrom.getString(aKey + REG_SUFFIX);
		if (tName != null && !tName.isEmpty()) aTo.putString(aKey + REG_SUFFIX, tName);
	}

	/** Item by its full registry name ("namespace:path"); null for an empty, malformed or unknown name. */
	public static Item itemByRegName(String aRegName) {
		if (aRegName == null || aRegName.isEmpty()) return null;
		int tColon = aRegName.indexOf(':');
		if (tColon <= 0) return null;
		try {return findItem(aRegName.substring(0, tColon), aRegName.substring(tColon + 1));}
		catch (Throwable e) {return null;}
	}

	public static Item findItem(String aModID, String aName) {
		if (aModID == null || aName == null) return null;
		ResourceLocation tID = new ResourceLocation(aModID, aName);
		return BuiltInRegistries.ITEM.containsKey(tID) ? BuiltInRegistries.ITEM.get(tID) : null;
	}
	/** Single point for building a sized stack from (modId,name); returns null for an unregistered item, as the original did. */
	public static ItemStack findItemStack(String aModID, String aName, int aSize) {
		Item tItem = findItem(aModID, aName);
		return tItem == null ? null : make_(tItem, aSize, 0);
	}

	public static ItemStack set(ItemStack aSetStack, ItemStack aToStack) {
		return set(aSetStack, aToStack, T, T);
	}
	public static ItemStack set(ItemStack aSetStack, ItemStack aToStack, boolean aCheckStacksize, boolean aCheckNBT) {
		if (aSetStack == aToStack) return aSetStack;
		if (invalid(aSetStack) || invalid(aToStack)) return null;
		// 1.7.10 called the same engine method from both points.
		// This calls setItem below for the same reason - one center, not two.
		setItem(aSetStack, item_(aToStack));
		if (aCheckStacksize) aSetStack.setCount(aToStack.getCount());
		meta_(aSetStack, meta_(aToStack));
		if (aCheckNBT) ItemNBT.set(aSetStack, ItemNBT.get(aToStack));
		return aSetStack;
	}
	/** Unlike set(), this mutates only the Item in place (count/meta/NBT untouched); identity's sole carrier is the
	 *  delegate field (getItem() reads it), opened by the Access Transformer and written directly, as with setMaxDamage. */
	public static ItemStack setItem(ItemStack aStack, Item aItem) {
		if (valid(aStack) && aItem != null) aStack.delegate = net.minecraftforge.registries.ForgeRegistries.ITEMS.getDelegateOrThrow(aItem);
		return aStack;
	}
	/** 1.7.10's setMaxDamage mutated even a foreign item's durability at runtime; 1.20.1's field is private final, so
	 *  the Access Transformer opens it and writes straight in, as the original did. */
	public static void setMaxDamage(Item aItem, int aMaxDamage) {
		if (aItem == null) return;
		aItem.maxDamage = aMaxDamage;
	}

	public static ItemStack update (ItemStack aStack) {
		return invalid(aStack)?aStack:update_(aStack);
	}
	public static ItemStack update_(ItemStack aStack) {
		if (ItemNBT.has(aStack) && ItemNBT.get(aStack).isEmpty()) ItemNBT.set(aStack, null);
		if (item_(aStack) instanceof IItemUpdatable) ((IItemUpdatable)item_(aStack)).updateItemStack(aStack);
		return aStack;
	}
	public static ItemStack update (ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {
		return invalid(aStack)?aStack:update_(aStack, aWorld, aX, aY, aZ);
	}
	public static ItemStack update_(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {
		if (ItemNBT.has(aStack) && ItemNBT.get(aStack).isEmpty()) ItemNBT.set(aStack, null);
		if (item_(aStack) instanceof IItemUpdatable) ((IItemUpdatable)item_(aStack)).updateItemStack(aStack, aWorld, aX, aY, aZ);
		return aStack;
	}
	public static ItemStack update (ItemStack aStack, Entity aEntity) {
		return update(aStack, aEntity.level(), UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ()));
	}
	public static ItemStack update_(ItemStack aStack, Entity aEntity) {
		return update_(aStack, aEntity.level(), UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ()));
	}
	
	public static boolean update(Entity aPlayer) {
		if (aPlayer instanceof Player && !aPlayer.level().isClientSide() && ((Player)aPlayer).containerMenu != null) ((Player)aPlayer).containerMenu.broadcastChanges();
		return T;
	}
	
	public static boolean use(Entity aPlayer, ItemStack aStack) {
		return use(aPlayer, F, T, aStack, 1);
	}
	public static boolean use(Entity aPlayer, ItemStack aStack, long aAmount) {
		return use(aPlayer, F, T, aStack, aAmount);
	}
	public static boolean use(Entity aPlayer, boolean aRemove, ItemStack aStack) {
		return use(aPlayer, aRemove, T, aStack, 1);
	}
	public static boolean use(Entity aPlayer, boolean aRemove, ItemStack aStack, long aAmount) {
		return use(aPlayer, aRemove, T, aStack, aAmount);
	}
	public static boolean use(Entity aPlayer, boolean aRemove, boolean aTriggerEvent, ItemStack aStack) {
		return use(aPlayer, aRemove, aTriggerEvent, aStack, 1);
	}
	public static boolean use(Entity aPlayer, boolean aRemove, boolean aTriggerEvent, ItemStack aStack, long aAmount) {
		if (UT.Entities.hasInfiniteItems(aPlayer)) return T;
		if (invalid(aStack)) return F;
		if (aStack.getCount() < aAmount) return F;
		aStack.setCount((int)(aStack.getCount()-(aAmount))); // aAmount is a long, so it needs an explicit cast for setCount(int).
		if (!(aPlayer instanceof Player)) return T;
		if (aStack.getCount() <= 0) {
			if (aTriggerEvent) net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem((Player)aPlayer, aStack, null); // A generic decrement with no hand context returns null here.
			if (aRemove) for (int i = 0; i < ((Player)aPlayer).getInventory().items.size(); i++) {
				if (((Player)aPlayer).getInventory().items.get(i) == aStack) {
					((Player)aPlayer).getInventory().items.set(i, ItemStack.EMPTY); // Vanilla's NonNullList requires non-null; 1.7.10 used null here for an empty slot.
					break;
				}
			}
		}
		update(aPlayer);
		return T;
	}
	
	public static ItemStack[] copyArray(ItemStack... aStacks) {
		ItemStack[] rStacks = new ItemStack[aStacks.length];
		for (int i = 0; i < aStacks.length; i++) rStacks[i] = copy(aStacks[i]);
		return rStacks;
	}
	
	public static ItemStack copyFirst(Object... aStacks) {
		return copy(get(aStacks));
	}
	
	public static ItemStack copyMeta(long aMeta, ItemStack aStack) {
		return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : meta_(copy_(aStack), aMeta);
	}
	public static ItemStack copyAmountAndMeta(long aSize, long aMeta, ItemStack aStack) {
		return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : meta_(amount_(aSize, aStack), aMeta);
	}
	public static ItemStack get(Object... aStacks) {
		for (Object aStack : aStacks) {
		if (aStack instanceof ItemStack         ) {if (valid((ItemStack)aStack)) return (ItemStack)aStack; continue;}
		if (aStack instanceof Item              ) return make_((Item)aStack, 1, 0);
		if (aStack instanceof Block             ) return make((Block)aStack, 1, 0);
		if (aStack instanceof IItemContainer    ) {ItemStack rStack = ((IItemContainer    )aStack).get(   1); if (valid(rStack)) return rStack; continue;}
		if (aStack instanceof ItemStackContainer) {ItemStack rStack = ((ItemStackContainer)aStack).toStack(); if (valid(rStack)) return rStack; continue;}
		}
		return null;
	}
	
	public static boolean hasValid(ItemStack... aStacks) {if (aStacks != null) for (ItemStack aStack : aStacks) if (valid(aStack)) return T; return F;}
	
	
	public static ItemStackSet<ItemStackContainer> hashset(ItemStack... aStacks) {return new ItemStackSet<>(aStacks);}
	public static ArrayListNoNulls<ItemStack> arraylist(ItemStack... aStacks) {return new ArrayListNoNulls<>(F, aStacks);}
	public static ItemStack[] array(ItemStack... aStacks) {return aStacks;}
	public static ItemStack[] array(int aLength) {return new ItemStack[aLength];}
	
	// Flattening removed the 3-arg meta constructor; the value now goes through setDamageValue on a 2-arg stack.
	// For a flattened vanilla family, CS.Flattened resolves the right member so all 477 call sites stay verbatim.
	public static ItemStack make_(Item  aItem , long aSize, long aMeta) {Item  tFlat = CS.Flattened.item (aItem , aMeta); if (tFlat != null) return new ItemStack(tFlat, UT.Code.bindInt(aSize)); ItemStack tPotion = legacyPotion(aItem, aSize, aMeta); if (tPotion != null) return tPotion; ItemStack rStack = new ItemStack(aItem , UT.Code.bindInt(aSize)); meta_(rStack, UT.Code.bindShort(aMeta)); return rStack;}

	// Bridges 1.7.10's potion-meta bits to neo's PotionContents component, the vanilla equivalent of CS.Flattened above.
	// An unmapped combination degrades to the base look, matching gaps that already existed in 1.7.10.
	private static ItemStack legacyPotion(Item aItem, long aSize, long aMeta) {
		if (aItem != Items.POTION && aItem != Items.SPLASH_POTION && aItem != Items.LINGERING_POTION) return null;
		net.minecraft.world.item.alchemy.Potion tKind = legacyPotionKind(aMeta);
		if (tKind == null) return null;
		// Lingering is already a separate item on input; the explosive meta bit is ignored since lingering never used it.
		ItemStack rStack = new ItemStack(aItem == Items.POTION && (aMeta & 0x4000) != 0 ? Items.SPLASH_POTION : aItem, UT.Code.bindInt(aSize));
		// 1.20.1 sets the brew through PotionUtils.setPotion, the equivalent of 1.7.10's ItemPotion meta.
		net.minecraft.world.item.alchemy.PotionUtils.setPotion(rStack, tKind);
		meta_(rStack, UT.Code.bindShort(aMeta));
		return rStack;
	}
	// Potions.X is the plain Potion object here; the Holder layer only exists on the 26.x branch.
	private static net.minecraft.world.item.alchemy.Potion legacyPotionKind(long aMeta) {
		boolean tStrong = (aMeta & 0x20) != 0, tLong = (aMeta & 0x40) != 0;
		switch ((int)(aMeta & 15)) {
		case  1: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_REGENERATION : tLong ? net.minecraft.world.item.alchemy.Potions.LONG_REGENERATION : net.minecraft.world.item.alchemy.Potions.REGENERATION;
		case  2: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_SWIFTNESS : tLong ? net.minecraft.world.item.alchemy.Potions.LONG_SWIFTNESS : net.minecraft.world.item.alchemy.Potions.SWIFTNESS;
		case  3: return tLong ? net.minecraft.world.item.alchemy.Potions.LONG_FIRE_RESISTANCE : net.minecraft.world.item.alchemy.Potions.FIRE_RESISTANCE;
		case  4: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_POISON : tLong ? net.minecraft.world.item.alchemy.Potions.LONG_POISON : net.minecraft.world.item.alchemy.Potions.POISON;
		case  5: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_HEALING : net.minecraft.world.item.alchemy.Potions.HEALING;
		case  6: return tLong ? net.minecraft.world.item.alchemy.Potions.LONG_NIGHT_VISION : net.minecraft.world.item.alchemy.Potions.NIGHT_VISION;
		case  8: return tLong ? net.minecraft.world.item.alchemy.Potions.LONG_WEAKNESS : net.minecraft.world.item.alchemy.Potions.WEAKNESS;
		case  9: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_STRENGTH : tLong ? net.minecraft.world.item.alchemy.Potions.LONG_STRENGTH : net.minecraft.world.item.alchemy.Potions.STRENGTH;
		case 10: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_SLOWNESS : tLong ? net.minecraft.world.item.alchemy.Potions.LONG_SLOWNESS : net.minecraft.world.item.alchemy.Potions.SLOWNESS;
		case 11: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_LEAPING : tLong ? net.minecraft.world.item.alchemy.Potions.LONG_LEAPING : net.minecraft.world.item.alchemy.Potions.LEAPING;
		case 12: return tStrong ? net.minecraft.world.item.alchemy.Potions.STRONG_HARMING : net.minecraft.world.item.alchemy.Potions.HARMING;
		case 13: return tLong ? net.minecraft.world.item.alchemy.Potions.LONG_WATER_BREATHING : net.minecraft.world.item.alchemy.Potions.WATER_BREATHING;
		case 14: return tLong ? net.minecraft.world.item.alchemy.Potions.LONG_INVISIBILITY : net.minecraft.world.item.alchemy.Potions.INVISIBILITY;
		// Empty low bits cover the base brews (awkward/thick/mundane) and plain water, matching 1.7.10's meta-0 water bottle.
		case  0: return aMeta == 16 ? net.minecraft.world.item.alchemy.Potions.AWKWARD : aMeta == 32 ? net.minecraft.world.item.alchemy.Potions.THICK
			: (aMeta == 64 || aMeta == 8192) ? net.minecraft.world.item.alchemy.Potions.MUNDANE : aMeta == 0 ? net.minecraft.world.item.alchemy.Potions.WATER : null;
		default: return null;
		}
	}
	public static ItemStack make_(Block aBlock, long aSize, long aMeta) {Block tFlat = CS.Flattened.block(aBlock, aMeta); if (tFlat != null) return new ItemStack(tFlat, UT.Code.bindInt(aSize)); ItemStack rStack = new ItemStack(aBlock, UT.Code.bindInt(aSize)); meta_(rStack, UT.Code.bindShort(aMeta)); return rStack;}
	public static ItemStack make(ModData aModID, String aItem, long aSize) {
		if (!aModID.mLoaded || UT.Code.stringInvalid(aItem) || !GAPI_POST.mStartedPreInit) return null;
		ItemStack
		rStack = findItemStack(aModID.mID, aItem, (int)aSize);
		if (valid(rStack)) return rStack;
		if (aItem.length() < 5 || aItem.charAt(4) != '.' || !aItem.startsWith("tile")) return null;
		return validate(findItemStack(aModID.mID, aItem.substring(5), (int)aSize));
	}
	public static ItemStack mkic(String aItem, long aSize) {
		if (UT.Code.stringInvalid(aItem) || !GAPI_POST.mStartedPreInit) return null;
		if (!sIC2ItemMap.containsKey(aItem)) try {
			ItemStack tStack = validate(IC2Items.getItem(aItem));
			sIC2ItemMap.put(aItem, tStack);
			if (tStack == null && MD.IC2.mLoaded && !aItem.startsWith("rubber")) ERR.println(aItem + " is not found in the IC2 Items!");
		} catch (Throwable e) {
			sIC2ItemMap.put(aItem, null);
		}
		return amount(aSize, sIC2ItemMap.get(aItem));
	}
	
	private static final Map<String, ItemStack> sIC2ItemMap = new HashMap<>();
	
	public static ItemStack mkic(String aItem                , long aSize, long aMeta                                   ) {return     meta(mkic(aItem, aSize), aMeta);}
	public static ItemStack mkic(String aItem                , long aSize            , ItemStack aReplacement           ) {return get(     mkic(aItem, aSize)        , aReplacement);}
	public static ItemStack mkic(String aItem                , long aSize, long aMeta, Object    aReplacement           ) {return get(meta(mkic(aItem, aSize), aMeta), aReplacement);}
	// The sole point where a 1.7.10 foreign-mod item name becomes a stack; every addressing path funnels here.
	// AE2 renamed its items entirely on 1.20.1, so its pairs resolve through a dedicated table; others stay verbatim.
	public static ItemStack make(ModData aModID, String aItem, long aSize, long aMeta                                   ) {if (gregapi.compat.AE2Names.owns(aModID, aItem)) return gregapi.compat.AE2Names.make(aItem, aSize, aMeta); return     meta(make(aModID, aItem, aSize), aMeta);}
	public static ItemStack make(ModData aModID, String aItem, long aSize, long aMeta, Object    aReplacement           ) {return get(meta(make(aModID, aItem, aSize), aMeta), aReplacement);}
	public static ItemStack make(long   aItemID              , long aSize, long aMeta                                   ) {return make(item(aItemID), aSize, aMeta);}
	public static ItemStack make(long   aItemID              , long aSize, long aMeta              , CompoundTag aNBT) {return make(item(aItemID), aSize, aMeta, aNBT);}
	public static ItemStack make(long   aItemID              , long aSize, long aMeta, String aName                     ) {return make(item(aItemID), aSize, aMeta, aName);}
	public static ItemStack make(long   aItemID              , long aSize, long aMeta, String aName, CompoundTag aNBT) {return make(item(aItemID), aSize, aMeta, aName, aNBT);}
	public static ItemStack make(Item   aItem                , long aSize, long aMeta                                   ) {return aItem   == null                 ? null :          make_(aItem            , aSize, aMeta);}
	public static ItemStack make(Block  aBlock               , long aSize, long aMeta                                   ) {return aBlock  == null || aBlock == NB ? null :          make_(aBlock           , aSize, aMeta);}
//  public static ItemStack make(IBlock aBlock               , long aSize, long aMeta                                   ) {return aBlock  == null                 ? null :          make_(aBlock.getBlock(), aSize, aMeta);}
	public static ItemStack make(Item   aItem                , long aSize, long aMeta              , CompoundTag aNBT) {return aItem   == null                 ? null :      nbt(make_(aItem            , aSize, aMeta), aNBT);}
	public static ItemStack make(Block  aBlock               , long aSize, long aMeta              , CompoundTag aNBT) {return aBlock  == null || aBlock == NB ? null :      nbt(make_(aBlock           , aSize, aMeta), aNBT);}
//  public static ItemStack make(IBlock aBlock               , long aSize, long aMeta              , NBTTagCompound aNBT) {return aBlock  == null                 ? null :      nbt(make_(aBlock.getBlock(), aSize, aMeta), aNBT);}
	public static ItemStack make(Item   aItem                , long aSize, long aMeta, String aName                     ) {return aItem   == null                 ? null : name(    make_(aItem            , aSize, aMeta)       , aName);}
	public static ItemStack make(Block  aBlock               , long aSize, long aMeta, String aName                     ) {return aBlock  == null || aBlock == NB ? null : name(    make_(aBlock           , aSize, aMeta)       , aName);}
//  public static ItemStack make(IBlock aBlock               , long aSize, long aMeta, String aName                     ) {return aBlock  == null                 ? null : name(    make_(aBlock.getBlock(), aSize, aMeta)       , aName);}
	public static ItemStack make(Item   aItem                , long aSize, long aMeta, String aName, CompoundTag aNBT) {return aItem   == null                 ? null : name(nbt(make_(aItem            , aSize, aMeta), aNBT), aName);}
	public static ItemStack make(Block  aBlock               , long aSize, long aMeta, String aName, CompoundTag aNBT) {return aBlock  == null || aBlock == NB ? null : name(nbt(make_(aBlock           , aSize, aMeta), aNBT), aName);}
//  public static ItemStack make(IBlock aBlock               , long aSize, long aMeta, String aName, NBTTagCompound aNBT) {return aBlock  == null                 ? null : name(nbt(make_(aBlock.getBlock(), aSize, aMeta), aNBT), aName);}
	public static ItemStack make(ItemStack          aStack                                         , CompoundTag aNBT) {return aStack == null ? null :      nbt_(aStack.copy(), aNBT);}
	public static ItemStack make(ItemStack          aStack                           , String aName                     ) {return aStack == null ? null : name(     aStack.copy()       , aName);}
	public static ItemStack make(ItemStack          aStack                           , String aName, CompoundTag aNBT) {return aStack == null ? null : name(nbt_(aStack.copy(), aNBT), aName);}
	public static ItemStack make(ItemStackContainer aStack                                         , CompoundTag aNBT) {return      nbt(aStack.toStack(), aNBT);}
	public static ItemStack make(ItemStackContainer aStack                           , String aName                     ) {return name(    aStack.toStack()       , aName);}
	public static ItemStack make(ItemStackContainer aStack                           , String aName, CompoundTag aNBT) {return name(nbt(aStack.toStack(), aNBT), aName);}
	
	public static ItemEntity place  (Level aWorld, double aX, double aY, double aZ, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, double aX, double aY, double aZ, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, double aX, double aY, double aZ, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, double aX, double aY, double aZ, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, double aX, double aY, double aZ, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, double aX, double aY, double aZ, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, double aX, double aY, double aZ, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, double aX, double aY, double aZ, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, double aX, double aY, double aZ, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, double aX, double aY, double aZ, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aX, aY, aZ, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity entity (Level aWorld, double aX, double aY, double aZ, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; return               entity_(aWorld, aX, aY, aZ, rStack);}
	public static ItemEntity entity (Level aWorld, double aX, double aY, double aZ, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; return               entity_(aWorld, aX, aY, aZ, rStack);}
	public static ItemEntity entity (Level aWorld, double aX, double aY, double aZ, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; return               entity_(aWorld, aX, aY, aZ, rStack);}
	public static ItemEntity entity (Level aWorld, double aX, double aY, double aZ, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; return               entity_(aWorld, aX, aY, aZ, rStack);}
	public static ItemEntity entity (Level aWorld, double aX, double aY, double aZ, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; return               entity_(aWorld, aX, aY, aZ, rStack);}
	public static ItemEntity entity_(Level aWorld, double aX, double aY, double aZ, ItemStack aStack                                    ) {return new ItemEntity(aWorld, aX, aY, aZ, update_(aStack, aWorld, UT.Code.roundDown(aX), UT.Code.roundDown(aY), UT.Code.roundDown(aZ)));}
	
	public static ItemEntity place  (Entity aEntity, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); rEntity.setDeltaMovement(0,0,0); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Entity aEntity, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); rEntity.setDeltaMovement(0,0,0); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Entity aEntity, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); rEntity.setDeltaMovement(0,0,0); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Entity aEntity, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); rEntity.setDeltaMovement(0,0,0); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Entity aEntity, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); rEntity.setDeltaMovement(0,0,0); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Entity aEntity, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Entity aEntity, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Entity aEntity, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Entity aEntity, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Entity aEntity, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aEntity, rStack); return aEntity.level().addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity entity (Entity aEntity, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; return               entity_(aEntity, rStack);}
	public static ItemEntity entity (Entity aEntity, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; return               entity_(aEntity, rStack);}
	public static ItemEntity entity (Entity aEntity, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; return               entity_(aEntity, rStack);}
	public static ItemEntity entity (Entity aEntity, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; return               entity_(aEntity, rStack);}
	public static ItemEntity entity (Entity aEntity, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; return               entity_(aEntity, rStack);}
	public static ItemEntity entity_(Entity aEntity, ItemStack aStack                                    ) {return new ItemEntity(aEntity.level(), aEntity.getX(), aEntity.getY(), aEntity.getZ(), update_(aStack, aEntity));}
	
	public static ItemEntity place  (Level aWorld, BlockPos aCoords, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, BlockPos aCoords, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, BlockPos aCoords, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, BlockPos aCoords, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity place  (Level aWorld, BlockPos aCoords, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); rEntity.setDeltaMovement(0,0,0); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, BlockPos aCoords, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, BlockPos aCoords, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, BlockPos aCoords, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, BlockPos aCoords, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity drop   (Level aWorld, BlockPos aCoords, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; ItemEntity rEntity = entity_(aWorld, aCoords, rStack); return aWorld.addFreshEntity(rEntity) ? rEntity : null;}
	public static ItemEntity entity (Level aWorld, BlockPos aCoords, ModData aModID, String aItem, long aSize, long aMeta) {ItemStack rStack = make(aModID, aItem, aSize, aMeta); if (invalid(rStack)) return null; return               entity_(aWorld, aCoords, rStack);}
	public static ItemEntity entity (Level aWorld, BlockPos aCoords, Item aItem, long aSize, long aMeta                  ) {ItemStack rStack = make(aItem, aSize, aMeta)        ; if (invalid(rStack)) return null; return               entity_(aWorld, aCoords, rStack);}
	public static ItemEntity entity (Level aWorld, BlockPos aCoords, Block aBlock, long aSize, long aMeta                ) {ItemStack rStack = make(aBlock, aSize, aMeta)       ; if (invalid(rStack)) return null; return               entity_(aWorld, aCoords, rStack);}
	public static ItemEntity entity (Level aWorld, BlockPos aCoords, ItemStackContainer aStack                           ) {ItemStack rStack = aStack.toStack()                 ; if (invalid(rStack)) return null; return               entity_(aWorld, aCoords, rStack);}
	public static ItemEntity entity (Level aWorld, BlockPos aCoords, ItemStack aStack                                    ) {ItemStack rStack = aStack                           ; if (invalid(rStack)) return null; return               entity_(aWorld, aCoords, rStack);}
	public static ItemEntity entity_(Level aWorld, BlockPos aCoords, ItemStack aStack                                    ) {return new ItemEntity(aWorld, aCoords.getX()+0.5, aCoords.getY()+0.5, aCoords.getZ()+0.5, update_(aStack, aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ()));}
	
	@SuppressWarnings("rawtypes")
	public static int move(DelegatorTileEntity aFrom, DelegatorTileEntity aTo) {return move(aFrom, aTo, null, F, F, F, T, 64, 1, 64, 1);}
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static int move(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, ItemStackSet<ItemStackContainer> aFilter, boolean aIgnoreSideFrom, boolean aIgnoreSideTo, boolean aInvertFilter, boolean aEjectItems, int aMaxSize, int aMinSize, int aMaxMove, int aMinMove) {
		if (!(aFrom.mTileEntity instanceof Container)) return 0;
		aFrom = getPotentialDoubleChest(aFrom);
		int[] aSlotsFrom = (!aIgnoreSideFrom && aFrom.mTileEntity instanceof WorldlyContainer ? ((WorldlyContainer)aFrom.mTileEntity).getSlotsForFace(FORGE_DIR[aFrom.mSideOfTileEntity]) : UT.Code.getAscendingArray(((Container)aFrom.mTileEntity).getContainerSize()));
		if (!(aTo.mTileEntity instanceof Container)) return put(aFrom, aSlotsFrom, aTo, aFilter, aIgnoreSideFrom, aInvertFilter, aEjectItems, aMaxMove, aMinMove);
		aTo = getPotentialDoubleChest(aTo);
		int[] aSlotsTo   = (!aIgnoreSideTo   && aTo  .mTileEntity instanceof WorldlyContainer ? ((WorldlyContainer)aTo  .mTileEntity).getSlotsForFace(FORGE_DIR[aTo  .mSideOfTileEntity]) : UT.Code.getAscendingArray(((Container)aTo  .mTileEntity).getContainerSize()));
		
		for (int aSlotFrom : aSlotsFrom) {
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // Vanilla's Container returns EMPTY here, but the 1.7.10 body still reasons in null.
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			for (int aSlotTo : aSlotsTo) {
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // Same reasoning as above: vanilla returns EMPTY, not null.
				int tMovable = Math.min(aMaxMove, canPut((Container)aTo.mTileEntity, aIgnoreSideTo ? SIDE_ANY : aTo.mSideOfTileEntity, aTo.mSideOfTileEntity, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSize, aStackFrom.getMaxStackSize())));
				if (tMovable < aMinMove || tMovable + (aStackTo == null ? 0 : aStackTo.getCount()) < aMinSize) continue;
				// Actually Moving the Stack
				return move_((Container)aFrom.mTileEntity, (Container)aTo.mTileEntity, aStackFrom, aStackTo, aSlotFrom, aSlotTo, tMovable);
			}
		}
		return 0;
	}
	
	
	@SuppressWarnings("rawtypes")
	public static int moveAll(DelegatorTileEntity aFrom, DelegatorTileEntity aTo) {return moveAll(aFrom, aTo, null, F, F, F, T, 64, 1, 64, 1);}
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static int moveAll(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, ItemStackSet<ItemStackContainer> aFilter, boolean aIgnoreSideFrom, boolean aIgnoreSideTo, boolean aInvertFilter, boolean aEjectItems, int aMaxSize, int aMinSize, int aMaxMove, int aMinMove) {
		if (!(aFrom.mTileEntity instanceof Container)) return 0;
		aFrom = getPotentialDoubleChest(aFrom);
		int[] aSlotsFrom = (!aIgnoreSideFrom && aFrom.mTileEntity instanceof WorldlyContainer ? ((WorldlyContainer)aFrom.mTileEntity).getSlotsForFace(FORGE_DIR[aFrom.mSideOfTileEntity]) : UT.Code.getAscendingArray(((Container)aFrom.mTileEntity).getContainerSize()));
		if (!(aTo.mTileEntity instanceof Container)) return put(aFrom, aSlotsFrom, aTo, aFilter, aIgnoreSideFrom, aInvertFilter, aEjectItems, aMaxMove, aMinMove);
		aTo = getPotentialDoubleChest(aTo);
		int[] aSlotsTo   = (!aIgnoreSideTo   && aTo  .mTileEntity instanceof WorldlyContainer ? ((WorldlyContainer)aTo  .mTileEntity).getSlotsForFace(FORGE_DIR[aTo  .mSideOfTileEntity]) : UT.Code.getAscendingArray(((Container)aTo  .mTileEntity).getContainerSize()));
		
		int rMoved = 0;
		
		for (int aSlotFrom : aSlotsFrom) {
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // Vanilla's Container returns EMPTY here, but the 1.7.10 body still reasons in null.
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			for (int aSlotTo : aSlotsTo) {
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // Same reasoning as above: vanilla returns EMPTY, not null.
				int tMovable = Math.min(aMaxMove, canPut((Container)aTo.mTileEntity, aIgnoreSideTo ? SIDE_ANY : aTo.mSideOfTileEntity, aTo.mSideOfTileEntity, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSize, aStackFrom.getMaxStackSize())));
				if (tMovable < aMinMove || tMovable + (aStackTo == null ? 0 : aStackTo.getCount()) < aMinSize) continue;
				// Actually Moving the Stack
				rMoved += move_((Container)aFrom.mTileEntity, (Container)aTo.mTileEntity, aStackFrom, aStackTo, aSlotFrom, aSlotTo, tMovable);
				aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // Vanilla's Container returns EMPTY here, but the 1.7.10 body still reasons in null.
				if (size(aStackFrom) < 1) break;
			}
		}
		return rMoved;
	}
	
	
	@SuppressWarnings("rawtypes")
	public static int moveFrom(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, int aSlotFrom) {return moveFrom(aFrom, aTo, aSlotFrom, null, F, F, F, T, 64, 1, 64, 1);}
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static int moveFrom(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, int aSlotFrom, ItemStackSet<ItemStackContainer> aFilter, boolean aIgnoreSideFrom, boolean aIgnoreSideTo, boolean aInvertFilter, boolean aEjectItems, int aMaxSize, int aMinSize, int aMaxMove, int aMinMove) {
		if (aSlotFrom < 0) return 0;
		if (!(aFrom.mTileEntity instanceof Container)) return 0;
		aFrom = getPotentialDoubleChest(aFrom);
		if (aSlotFrom >= ((Container)aFrom.mTileEntity).getContainerSize()) return 0;
		if (!(aTo.mTileEntity instanceof Container)) return put(aFrom, new int[] {aSlotFrom}, aTo, aFilter, aIgnoreSideFrom, aInvertFilter, aEjectItems, aMaxMove, aMinMove);
		aTo = getPotentialDoubleChest(aTo);
		int[] aSlotsTo   = (!aIgnoreSideTo   && aTo  .mTileEntity instanceof WorldlyContainer ? ((WorldlyContainer)aTo  .mTileEntity).getSlotsForFace(FORGE_DIR[aTo  .mSideOfTileEntity]) : UT.Code.getAscendingArray(((Container)aTo  .mTileEntity).getContainerSize()));
		
		ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // Vanilla's Container returns EMPTY here, but the 1.7.10 body still reasons in null.
		if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) return 0;
		for (int aSlotTo : aSlotsTo) {
			ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // Same reasoning as above: vanilla returns EMPTY, not null.
			int tMovable = Math.min(aMaxMove, canPut((Container)aTo.mTileEntity, aIgnoreSideTo ? SIDE_ANY : aTo.mSideOfTileEntity, aTo.mSideOfTileEntity, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSize, aStackFrom.getMaxStackSize())));
			if (tMovable < aMinMove || tMovable + (aStackTo == null ? 0 : aStackTo.getCount()) < aMinSize) continue;
			// Actually Moving the Stack
			return move_((Container)aFrom.mTileEntity, (Container)aTo.mTileEntity, aStackFrom, aStackTo, aSlotFrom, aSlotTo, tMovable);
		}
		return 0;
	}
	
	@SuppressWarnings("rawtypes")
	public static int moveTo(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, int aSlotTo) {return moveTo(aFrom, aTo, aSlotTo, null, F, F, F, T, 64, 1, 64, 1);}
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static int moveTo(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, int aSlotTo, ItemStackSet<ItemStackContainer> aFilter, boolean aIgnoreSideFrom, boolean aIgnoreSideTo, boolean aInvertFilter, boolean aEjectItems, int aMaxSize, int aMinSize, int aMaxMove, int aMinMove) {
		if (aSlotTo < 0) return 0;
		if (!(aFrom.mTileEntity instanceof Container)) return 0;
		aFrom = getPotentialDoubleChest(aFrom);
		int[] aSlotsFrom = (!aIgnoreSideFrom && aFrom.mTileEntity instanceof WorldlyContainer ? ((WorldlyContainer)aFrom.mTileEntity).getSlotsForFace(FORGE_DIR[aFrom.mSideOfTileEntity]) : UT.Code.getAscendingArray(((Container)aFrom.mTileEntity).getContainerSize()));
		if (!(aTo.mTileEntity instanceof Container)) return put(aFrom, aSlotsFrom, aTo, aFilter, aIgnoreSideFrom, aInvertFilter, aEjectItems, aMaxMove, aMinMove);
		aTo = getPotentialDoubleChest(aTo);
		if (aSlotTo >= ((Container)aTo.mTileEntity).getContainerSize()) return 0;
		
		for (int aSlotFrom : aSlotsFrom) {
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // Vanilla's Container returns EMPTY here, but the 1.7.10 body still reasons in null.
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // Same reasoning as above: vanilla returns EMPTY, not null.
			int tMovable = Math.min(aMaxMove, canPut((Container)aTo.mTileEntity, aIgnoreSideTo ? SIDE_ANY : aTo.mSideOfTileEntity, aTo.mSideOfTileEntity, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSize, aStackFrom.getMaxStackSize())));
			if (tMovable < aMinMove || tMovable + (aStackTo == null ? 0 : aStackTo.getCount()) < aMinSize) continue;
			// Actually Moving the Stack
			return move_((Container)aFrom.mTileEntity, (Container)aTo.mTileEntity, aStackFrom, aStackTo, aSlotFrom, aSlotTo, tMovable);
		}
		return 0;
	}
	
	@SuppressWarnings("rawtypes")
	public static int move(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, int aSlotFrom, int aSlotTo) {return move(aFrom, aTo, aSlotFrom, aSlotTo, null, F, F, F, T, 64, 1, 64, 1);}
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static int move(DelegatorTileEntity aFrom, DelegatorTileEntity aTo, int aSlotFrom, int aSlotTo, ItemStackSet<ItemStackContainer> aFilter, boolean aIgnoreSideFrom, boolean aIgnoreSideTo, boolean aInvertFilter, boolean aEjectItems, int aMaxSize, int aMinSize, int aMaxMove, int aMinMove) {
		if (aSlotFrom < 0 || aSlotTo < 0) return 0;
		if (aFrom.mTileEntity instanceof Container) {
			aFrom = getPotentialDoubleChest(aFrom);
			if (aSlotFrom >= ((Container)aFrom.mTileEntity).getContainerSize()) return 0;
			if (aTo.mTileEntity instanceof Container) {
				aTo = getPotentialDoubleChest(aTo);
				if (aSlotTo >= ((Container)aTo.mTileEntity).getContainerSize()) return 0;
				ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // Vanilla's Container returns EMPTY here, but the 1.7.10 body still reasons in null.
				if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) return 0;
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // Same reasoning as above: vanilla returns EMPTY, not null.
				int tMovable = Math.min(aMaxMove, canPut((Container)aTo.mTileEntity, aIgnoreSideTo ? SIDE_ANY : aTo.mSideOfTileEntity, aTo.mSideOfTileEntity, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSize, aStackFrom.getMaxStackSize())));
				if (tMovable < aMinMove || tMovable + (aStackTo == null ? 0 : aStackTo.getCount()) < aMinSize) return 0;
				// Actually Moving the Stack
				return move_((Container)aFrom.mTileEntity, (Container)aTo.mTileEntity, aStackFrom, aStackTo, aSlotFrom, aSlotTo, tMovable);
			}
			// Maybe the Recipient is a Pipe or something that causes Auto-Trash.
			return put(aFrom, new int[] {aSlotFrom}, aTo, aFilter, aIgnoreSideFrom, aInvertFilter, aEjectItems, Math.min(aMaxSize, aMaxMove), Math.max(aMinSize, aMinMove));
		}
		return 0;
	}
	
	public static int move(Container aInv, int aSlotFrom, int aSlotTo) {
		if (aSlotFrom == aSlotTo) return 0;
		ItemStack aStackFrom = n(aInv.getItem(aSlotFrom)), aStackTo = n(aInv.getItem(aSlotTo)); // F15
		return aStackFrom != null && (aStackTo == null || equal_(aStackFrom, aStackTo, F)) ? move_(aInv, aStackFrom, aStackTo, aSlotFrom, aSlotTo, Math.min(aStackFrom.getCount(), Math.min(aInv.getMaxStackSize(), aStackTo == null ? aStackFrom.getMaxStackSize() : aStackTo.getMaxStackSize() - aStackTo.getCount()))) : 0;
	}
	public static int move(Container aInv, int aSlotFrom, int aSlotTo, int aCount) {
		return move(aInv, n(aInv.getItem(aSlotFrom)), n(aInv.getItem(aSlotTo)), aSlotFrom, aSlotTo, aCount); // F15
	}
	public static int move(Container aInv, ItemStack aStackFrom, ItemStack aStackTo, int aSlotFrom, int aSlotTo, int aCount) {
		return aStackFrom != null && (aStackTo == null || equal_(aStackFrom, aStackTo, F)) ? move_(aInv, aStackFrom, aStackTo, aSlotFrom, aSlotTo, aCount) : 0;
	}
	public static int move_(Container aInv, ItemStack aStackFrom, ItemStack aStackTo, int aSlotFrom, int aSlotTo, int aCount) {
		aCount = Math.min(aCount, aStackFrom.getCount());
		if (aCount < 0) return 0;
		ItemStack tStack = aInv.removeItem(aSlotFrom, aCount);
		if (tStack == null || tStack.getCount() <= 0) return 0;
		aCount = Math.min(aCount, tStack.getCount());
		// Neo's removeItem zeroes the source object itself into EMPTY/air, unlike 1.7.10 which left the Item on a size-0
		// stack; the copy must come from the actually-removed result, not the zeroed original.
		if (aStackTo == null) aInv.setItem(aSlotTo, amount(aCount, tStack)); else aStackTo.setCount(aStackTo.getCount()+(aCount));
		aInv.setChanged();
		WD.mark(aInv);
		return aCount;
	}
	public static int move(Container aFrom, Container aTo, int aSlotFrom, int aSlotTo) {
		ItemStack aStackFrom = n(aFrom.getItem(aSlotFrom)), aStackTo = n(aTo.getItem(aSlotTo)); // F15
		return aStackFrom != null && (aStackTo == null || equal_(aStackFrom, aStackTo, F)) ? move_(aFrom, aTo, aStackFrom, aStackTo, aSlotFrom, aSlotTo, Math.min(aStackFrom.getCount(), Math.min(aTo.getMaxStackSize(), aStackTo == null ? aStackFrom.getMaxStackSize() : aStackTo.getMaxStackSize() - aStackTo.getCount()))) : 0;
	}
	public static int move(Container aFrom, Container aTo, int aSlotFrom, int aSlotTo, int aCount) {
		return move(aFrom, aTo, n(aFrom.getItem(aSlotFrom)), n(aTo.getItem(aSlotTo)), aSlotFrom, aSlotTo, aCount); // F15
	}
	public static int move(Container aFrom, Container aTo, ItemStack aStackFrom, ItemStack aStackTo, int aSlotFrom, int aSlotTo, int aCount) {
		return aStackFrom != null && (aStackTo == null || equal_(aStackFrom, aStackTo, F)) ? move_(aFrom, aTo, aStackFrom, aStackTo, aSlotFrom, aSlotTo, aCount) : 0;
	}
	public static int move_(Container aFrom, Container aTo, ItemStack aStackFrom, ItemStack aStackTo, int aSlotFrom, int aSlotTo, int aCount) {
		if (aStackFrom == aStackTo) return 0;
		if (aFrom == aTo && aSlotFrom == aSlotTo) return 0;
		aCount = Math.min(aCount, aStackFrom.getCount());
		if (aCount < 0) return 0;
		ItemStack tStack = aFrom.removeItem(aSlotFrom, aCount);
		if (tStack == null || tStack.getCount() <= 0) return 0;
		aCount = Math.min(aCount, tStack.getCount());
		// Same fix as move_(Container,...) above: copy from the removed result, not the zeroed source.
		if (aStackTo == null) aTo.setItem(aSlotTo, amount(aCount, tStack)); else aStackTo.setCount(aStackTo.getCount()+(aCount));
		aFrom.setChanged();
		aTo  .setChanged();
		WD.mark(aFrom);
		WD.mark(aTo);
		return aCount;
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static DelegatorTileEntity getPotentialDoubleChest(DelegatorTileEntity aPotentialChest) {
		if (aPotentialChest.mTileEntity instanceof ChestBlockEntity) {
			Block aChestBlock = aPotentialChest.getBlock();
			if (aPotentialChest.getBlockAtSide(SIDE_X_NEG) == aChestBlock) {
				BlockEntity tAdjacentChest = aPotentialChest.getTileEntityAtSideAndDistance(SIDE_X_NEG, 1);
				if (tAdjacentChest instanceof ChestBlockEntity) return new DelegatorTileEntity(new CompoundContainer((Container)tAdjacentChest, (Container)aPotentialChest.mTileEntity), aPotentialChest);
			}
			if (aPotentialChest.getBlockAtSide(SIDE_X_POS) == aChestBlock) {
				BlockEntity tAdjacentChest = aPotentialChest.getTileEntityAtSideAndDistance(SIDE_X_POS, 1);
				if (tAdjacentChest instanceof ChestBlockEntity) return new DelegatorTileEntity(new CompoundContainer((Container)aPotentialChest.mTileEntity, (Container)tAdjacentChest), aPotentialChest);
			}
			if (aPotentialChest.getBlockAtSide(SIDE_Z_NEG) == aChestBlock) {
				BlockEntity tAdjacentChest = aPotentialChest.getTileEntityAtSideAndDistance(SIDE_Z_NEG, 1);
				if (tAdjacentChest instanceof ChestBlockEntity) return new DelegatorTileEntity(new CompoundContainer((Container)tAdjacentChest, (Container)aPotentialChest.mTileEntity), aPotentialChest);
			}
			if (aPotentialChest.getBlockAtSide(SIDE_Z_POS) == aChestBlock) {
				BlockEntity tAdjacentChest = aPotentialChest.getTileEntityAtSideAndDistance(SIDE_Z_POS, 1);
				if (tAdjacentChest instanceof ChestBlockEntity) return new DelegatorTileEntity(new CompoundContainer((Container)aPotentialChest.mTileEntity, (Container)tAdjacentChest), aPotentialChest);
			}
		}
		return aPotentialChest;
	}
	
	public static boolean canConnect(@SuppressWarnings("rawtypes") DelegatorTileEntity aDelegator) {
		if (aDelegator.mTileEntity == null) return F;
		if (TE_PIPES && aDelegator.mTileEntity instanceof cofh.api.transport.IItemDuct) return T;
		if (BC_PIPES && aDelegator.mTileEntity instanceof buildcraft.api.transport.IInjectable) return ((buildcraft.api.transport.IInjectable)aDelegator.mTileEntity).canInjectItems(aDelegator.getForgeSideOfTileEntity());
		if (aDelegator.mTileEntity instanceof ITileEntityCanDelegate && ((ITileEntityCanDelegate)aDelegator.mTileEntity).isExtender(aDelegator.mSideOfTileEntity)) return T;
		if (aDelegator.mTileEntity instanceof Container && ((Container)aDelegator.mTileEntity).getContainerSize() > 0) return T;
		return F;
	}
	
	@Deprecated public static boolean canTake (Container      aFrom, byte aSideFrom, int aSlotFrom, ItemStack aStackFrom) {return canTake(aFrom, aSideFrom, aSideFrom, aSlotFrom, aStackFrom);}
	@Deprecated public static boolean canTake_(WorldlyContainer aFrom, byte aSideFrom, int aSlotFrom, ItemStack aStackFrom) {return canTake(aFrom, aSideFrom, aSideFrom, aSlotFrom, aStackFrom);}
	
	public static boolean canTake(Container aFrom, byte aSideFrom, byte aSideFallbackFrom, int aSlotFrom, ItemStack aStackFrom) {
		if (aFrom instanceof WorldlyContainer) {
			if (SIDES_VALID[aSideFrom]) return ((WorldlyContainer)aFrom).canTakeItemThroughFace(aSlotFrom, aStackFrom, FORGE_DIR[aSideFrom]);
			for (byte tSideFrom : ALL_SIDES_VALID) if (((WorldlyContainer)aFrom).canTakeItemThroughFace(aSlotFrom, aStackFrom, FORGE_DIR[tSideFrom])) {
				// Important fallback to prevent crashes with things coded like my Inventory Extenders!
				((WorldlyContainer)aFrom).canTakeItemThroughFace(aSlotFrom, aStackFrom, FORGE_DIR[aSideFallbackFrom]);
				return T;
			}
			return F;
		}
		return T;
	}
	
	@Deprecated public static int canPut(Container aTo, byte aSideTo, int aSlotTo, ItemStack aStackFrom) {return canPut(aTo, aSideTo, aSlotTo, aStackFrom, aStackFrom.getMaxStackSize());}
	@Deprecated public static int canPut(Container aTo, byte aSideTo, int aSlotTo, ItemStack aStackFrom, int aMaxSize) {return canPut(aTo, aSideTo, aSlotTo, aStackFrom, n(aTo.getItem(aSlotTo)));} // F15
	@Deprecated public static int canPut(Container aTo, byte aSideTo, int aSlotTo, ItemStack aStackFrom, ItemStack aStackTo) {return canPut(aTo, aSideTo, aSlotTo, aStackFrom, aStackTo, aStackFrom.getMaxStackSize());}
	@Deprecated public static int canPut(Container aTo, byte aSideTo, int aSlotTo, ItemStack aStackFrom, ItemStack aStackTo, int aMaxSize) {return canPut(aTo, aSideTo, aSideTo, aSlotTo, aStackFrom, aStackTo, aMaxSize);}
	
	public static int canPut(Container aTo, byte aSideTo, byte aSideFallbackTo, int aSlotTo, ItemStack aStackFrom) {return canPut(aTo, aSideTo, aSideFallbackTo, aSlotTo, aStackFrom, aStackFrom.getMaxStackSize());}
	public static int canPut(Container aTo, byte aSideTo, byte aSideFallbackTo, int aSlotTo, ItemStack aStackFrom, int aMaxSize) {return canPut(aTo, aSideTo, aSideFallbackTo, aSlotTo, aStackFrom, n(aTo.getItem(aSlotTo)));} // F15
	public static int canPut(Container aTo, byte aSideTo, byte aSideFallbackTo, int aSlotTo, ItemStack aStackFrom, ItemStack aStackTo) {return canPut(aTo, aSideTo, aSideFallbackTo, aSlotTo, aStackFrom, aStackTo, aStackFrom.getMaxStackSize());}
	public static int canPut(Container aTo, byte aSideTo, byte aSideFallbackTo, int aSlotTo, ItemStack aStackFrom, ItemStack aStackTo, int aMaxSize) {
		int rMaxMove = (aStackTo == null ? Math.min(aMaxSize, aTo.getMaxStackSize()) : equal_(aStackTo, aStackFrom, F) ? Math.min(aMaxSize, aTo.getMaxStackSize()) - aStackTo.getCount() : 0);
		if (rMaxMove <= 0 || !aTo.canPlaceItem(aSlotTo, aStackFrom)) return 0;
		if (!(aTo instanceof WorldlyContainer)) return rMaxMove;
		if (SIDES_VALID[aSideTo]) return ((WorldlyContainer)aTo).canPlaceItemThroughFace(aSlotTo, aStackFrom, FORGE_DIR[aSideTo]) ? rMaxMove : 0;
		for (byte tSideTo : ALL_SIDES_VALID) if (((WorldlyContainer)aTo).canPlaceItemThroughFace(aSlotTo, aStackFrom, FORGE_DIR[tSideTo])) {
			// Important fallback to prevent crashes with things coded like my Inventory Extenders!
			((WorldlyContainer)aTo).canPlaceItemThroughFace(aSlotTo, aStackFrom, FORGE_DIR[aSideFallbackTo]);
			return rMaxMove;
		}
		return 0;
	}
	
	
	public static int put(DelegatorTileEntity<Container> aFrom, int[] aSlotsFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, ItemStackSet<ItemStackContainer> aFilter, boolean aIgnoreSideFrom, boolean aInvertFilter, boolean aEjectItems, int aMaxMove, int aMinMove) {
		if (aTo.mTileEntity != null) {
			if (TE_PIPES && aTo.mTileEntity instanceof cofh.api.transport.IItemDuct) {
				for (int aSlotFrom : aSlotsFrom) {
					ItemStack aStackFrom = aFrom.mTileEntity.getItem(aSlotFrom);
					if (aStackFrom != null && aMinMove <= aStackFrom.getCount() && (aFilter == null || aFilter.contains(aStackFrom, T) != aInvertFilter) && canTake(aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) {
						// Actually Moving the Stack
						ItemStack tStackMoved = amount(Math.min(aStackFrom.getCount(), aMaxMove), aStackFrom);
						ItemStack rStackMoved = ((cofh.api.transport.IItemDuct)aTo.mTileEntity).insertItem(aTo.getForgeSideOfTileEntity(), copy(tStackMoved));
						int rMoved = (tStackMoved.getCount() - (rStackMoved == null ? 0 : rStackMoved.getCount()));
						if (rMoved > 0) {
							aFrom.mTileEntity.removeItem(aSlotFrom, rMoved);
							aFrom.mTileEntity.setChanged();
							WD.mark(aFrom);
							WD.mark(aTo);
							return rMoved;
						}
					}
				}
				return 0;
			}
			if (BC_PIPES && aTo.mTileEntity instanceof buildcraft.api.transport.IInjectable) {
				for (int aSlotFrom : aSlotsFrom) {
					ItemStack aStackFrom = aFrom.mTileEntity.getItem(aSlotFrom);
					if (aStackFrom != null && aMinMove <= aStackFrom.getCount() && (aFilter == null || aFilter.contains(aStackFrom, T) != aInvertFilter) && canTake(aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) {
						// Actually Moving the Stack
						ItemStack tStackMoved = amount(Math.min(aStackFrom.getCount(), aMaxMove), aStackFrom);
						int rMoved = ((buildcraft.api.transport.IInjectable)aTo.mTileEntity).injectItem(copy(tStackMoved), F, aTo.getForgeSideOfTileEntity(), null);
						if (rMoved >= aMinMove) {
							rMoved = (((buildcraft.api.transport.IInjectable)aTo.mTileEntity).injectItem(amount(rMoved, tStackMoved), T, aTo.getForgeSideOfTileEntity(), null));
							aFrom.mTileEntity.removeItem(aSlotFrom, rMoved);
							aFrom.mTileEntity.setChanged();
							WD.mark(aFrom);
							WD.mark(aTo);
							return rMoved;
						}
					}
				}
				return 0;
			}
		}
		
		Block aBlock = aTo.getBlock();
		if (aBlock instanceof BaseRailBlock) {
			// Do not eject shit onto Rails directly.
		} else if (WD.getMaterial(aBlock) == gregapi.block.Material.lava /* Items are still not dropped onto lava; WD.getMaterial works now, so the old gate is unnecessary. */ || aBlock instanceof FireBlock || (invalid(aBlock) && aTo.mY < 1)) {
			for (int aSlotFrom : aSlotsFrom) {
				ItemStack aStackFrom = aFrom.mTileEntity.getItem(aSlotFrom);
				if (aStackFrom != null && aMinMove <= aStackFrom.getCount() && (aFilter == null || aFilter.contains(aStackFrom, T) != aInvertFilter) && canTake(aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) {
					// Actually Moving the Stack
					int rMoved = GarbageGT.trash(amount(Math.min(aStackFrom.getCount(), aMaxMove), aStackFrom));
					aFrom.mTileEntity.removeItem(aSlotFrom, rMoved);
					aFrom.mTileEntity.setChanged();
					WD.mark(aFrom);
					return rMoved;
				}
			}
		} else if (!WD.hasCollide(aTo.mWorld, aTo.mX, aTo.mY, aTo.mZ, aBlock)) {
			if (aEjectItems) for (int aSlotFrom : aSlotsFrom) {
				ItemStack aStackFrom = aFrom.mTileEntity.getItem(aSlotFrom);
				if (aStackFrom != null && aMinMove <= aStackFrom.getCount() && (aFilter == null || aFilter.contains(aStackFrom, T) != aInvertFilter) && canTake(aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) {
					// Actually Moving the Stack
					ItemStack tStack = amount(Math.min(aStackFrom.getCount(), aMaxMove), aStackFrom);
					place(aTo.mWorld, aTo.mX+0.5, aTo.mY+0.5, aTo.mZ+0.5, tStack);
					aFrom.mTileEntity.removeItem(aSlotFrom, tStack.getCount());
					aFrom.mTileEntity.setChanged();
					WD.mark(aFrom);
					return tStack.getCount();
				}
			}
		}
		return 0;
	}
	
	public static ItemStack emptySlot() {
		return IL.Empty_Slot.get(0);
	}
	public static ItemStack tag(long aNumber) {
		return IL.Circuit_Selector.getWithDamage(0, aNumber);
	}
	
	public static ItemStack skull(Entity aPlayer) {return skull(aPlayer == null ? "" : aPlayer.getName().getString());}
	public static ItemStack skull(String aPlayer) {return ST.make(Items.PLAYER_HEAD, 1, 3, UT.Code.stringValid(aPlayer) ? UT.NBT.makeString("SkullOwner", aPlayer) : null);}
	
	public static ItemStack book(String aMapping) {
		return UT.Books.getBookWithTitle(aMapping);
	}
	public static ItemStack book(String aMapping, ItemStack aBook) {
		return UT.Books.getBookWithTitle(aMapping, aBook);
	}
	
	public static boolean debug(ItemStack aStack) {
		return ItemsGT.DEBUG_ITEMS.contains(aStack, T);
	}
	
	public static boolean instaharvest(Block aBlock) {
		return torch(aBlock) || BlocksGT.instaharvest.contains(aBlock);
	}
	public static boolean instaharvest(Block aBlock, long aMeta) {
		return torch(aBlock, aMeta) || BlocksGT.instaharvest.contains(aBlock);
	}
	
	public static boolean torch(Block aBlock) {
		return torch(aBlock, 1); // that "1" is totally not hacky at all. XD
	}
	public static boolean torch(Block aBlock, long aMeta) {
		if (IL.TFC_Torch.equal(aBlock) || IL.NePl_Torch.equal(aBlock) || IL.GC_Torch_Glowstone.equal(aBlock) || IL.AETHER_Torch_Ambrosium.equal(aBlock) || IL.AE_Torch_Quartz.equal(aBlock) || IL.TF_Firefly_Jar.equal(aBlock) || IL.TF_Firefly.equal(aBlock) || (aMeta == 1 && IL.TC_Block_Air.equal(aBlock))) return T;
		return aBlock instanceof TorchBlock && !(aBlock instanceof RedstoneTorchBlock);
	}
	public static boolean torch(ItemStack aStack) {
		return IL.TC_Nitor.equal(aStack, F, T) || torch(block(aStack), meta(aStack));
	}
	
	public static boolean ammo(ItemStack aStack) {
		if (ItemsGT.AMMO_ITEMS.contains(aStack, T)) return T;
		OreDictItemData tData = OM.anydata(aStack);
		return tData != null && tData.nonemptyData() && tData.mPrefix.contains(TD.Prefix.AMMO_ALIKE);
	}
	
	public static boolean nonautoinsert(ItemStack aStack) {
		if (ItemsGT.NON_AUTO_INSERT_ITEMS.contains(aStack, T) || torch(aStack)) return T;
		OreDictItemData tData = OM.anydata(aStack);
		return tData != null && tData.nonemptyData() && tData.mPrefix.contains(TD.Prefix.AMMO_ALIKE);
	}
	
	public static boolean listed(Collection<ItemStack> aList, ItemStack aStack, boolean aTrueIfListEmpty, boolean aInvertFilter) {
		if (aStack == null || aStack.getCount() < 1) return F;
		if (aList == null) return aTrueIfListEmpty;
		while (aList.contains(null)) aList.remove(null);
		if (aList.size() < 1) return aTrueIfListEmpty;
		Iterator<ItemStack> tIterator = aList.iterator();
		ItemStack tStack = null;
		while (tIterator.hasNext()) if ((tStack = tIterator.next())!= null && equal(aStack, tStack)) return !aInvertFilter;
		return aInvertFilter;
	}
	
	/** 1.7.10 exposed this as a method of Item itself, reachable from any GT6 item; neo has no such method, and GT6's
	 *  five implementing roots share no common ancestor, so this is the one place that asks all five instead of one. */
	public static ItemStack containerItemGT(ItemStack aStack) {
		Item tItem = item_(aStack);
		if (tItem instanceof gregapi.item.ItemBase                                  tI) return tI.getContainerItem(aStack);
		if (tItem instanceof gregapi.item.prefixitem.PrefixItem                     tI) return tI.getContainerItem(aStack);
		if (tItem instanceof gregapi.item.ItemFluidDisplay                          tI) return tI.getContainerItem(aStack);
		if (tItem instanceof gregapi.block.prefixblock.PrefixBlockItem              tI) return tI.getContainerItem(aStack);
		if (tItem instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tI) return tI.getContainerItem(aStack);
		return null;
	}

	public static boolean ingredable(ItemStack aStack) {
		if (invalid(aStack)) return F;
		if (item_(aStack) instanceof IItemGTContainerTool) return F;
		// A fluid container, even empty, is never a plain ingredient here, matching the restored IFluidContainerItem mirror.
		if (item_(aStack) instanceof IFluidContainerItem tICI && tICI.getCapacity(aStack) > 0) return F;
		// Symmetric to ST.container: the component channel below only covers vanilla.
		// All GT6 roots are asked through the single point above, covering what 1.7.10's polymorphic hook used to.
		if (containerItemGT(aStack) != null) return F;
		if (item_(aStack).hasCraftingRemainingItem(aStack)) return F; // IForgeItem.java:253
		if (ItemsGT.CONTAINER_DURABILITY.contains(aStack, T)) return F;
		if (IL.Cell_Empty.equal(aStack, F, T) || IL.SC2_Teapot_Empty.equal(aStack, F, T) || IL.SC2_Teacup_Empty.equal(aStack, F, T)) return T;
		if (IL.Cell_Empty.equal(aStack, T, T) || IL.SC2_Teapot_Empty.equal(aStack, T, T) || IL.SC2_Teacup_Empty.equal(aStack, T, T)) return F;
		return T;
	}
	
	public static ItemStack container(ItemStack aStack, boolean aCheckIFluidContainerItems) {
		if (invalid(aStack)) return NI;
		// Decrease Durability by 1 for these Items.
		if (ItemsGT.CONTAINER_DURABILITY.contains(aStack, T)) return copyMeta(meta_(aStack) + 1, aStack);
		// Use normal Container Item Mechanics.
		// The port narrowed this to the component-based getCraftingRemainder, which GT6 items don't have, so a tool
		// ingredient's remainder vanished everywhere; the live GT6 channel is restored first, then the component one.
		ItemStack tGTContainer = containerItemGT(aStack); // All GT6 roots at once, via containerItemGT above.
		if (tGTContainer != null) return copy(tGTContainer);
		if (item_(aStack).hasCraftingRemainingItem(aStack)) return copy(item_(aStack).getCraftingRemainingItem(aStack)); // IForgeItem.java:237,253
		// These are all special Cases, in which it is intended to have only GT Blocks outputting those Container Items.
		if (IL.Cell_Empty.exists()) {
			if (IL.Cell_Empty.equal(aStack, F, T)) return NI;
			if (IL.Cell_Empty.equal(aStack, T, T)) return IL.Cell_Empty.get(1);
		}
		if (IL.SC2_Teapot_Empty.exists()) {
			if (IL.SC2_Teapot_Empty.equal(aStack, F, T)) return NI;
			if (IL.SC2_Teapot_Empty.equal(aStack, T, T)) return IL.SC2_Teapot_Empty.get(1);
		}
		if (IL.SC2_Teacup_Empty.exists()) {
			if (IL.SC2_Teacup_Empty.equal(aStack, F, T)) return NI;
			if (IL.SC2_Teacup_Empty.equal(aStack, T, T)) return IL.SC2_Teacup_Empty.get(1);
		}
		// The emptied container after draining is the restored IFluidContainerItem mirror.
		// Matches the original's drain-to-empty path (guard on count<=0, clear empty NBT).
		if (aCheckIFluidContainerItems && item_(aStack) instanceof IFluidContainerItem tICI && tICI.getCapacity(aStack) > 0) {
			ItemStack tStack = amount(1, aStack);
			tICI.drain(tStack, Integer.MAX_VALUE, T);
			if (tStack.getCount() <= 0) return NI;
			CompoundTag tNBT = ItemNBT.get(tStack);
			if (tNBT != null && tNBT.isEmpty()) ItemNBT.set(tStack, null);
			return tStack;
		}
		return NI;
	}
	
	public static ItemStack container(ItemStack aStack, boolean aCheckIFluidContainerItems, int aSize) {
		return amount(aSize, container(aStack, aCheckIFluidContainerItems));
	}
	
	public static boolean rotten(ItemStack aStack) {
		if (invalid(aStack)) return F;
		if (item_(aStack) instanceof MultiItemRandom) {
			IFoodStat tStat = ((MultiItemRandom)item_(aStack)).mFoodStats.get(meta_(aStack));
			return tStat != null && tStat.isRotten(item_(aStack), aStack, null);
		}
		return item_(aStack) == Items.ROTTEN_FLESH || OM.materialcontained(aStack, MT.MeatRotten, MT.FishRotten);
	}
	
	public static boolean edible(ItemStack aStack) {
		if (invalid(aStack)) return F;
		if (item_(aStack) instanceof MultiItemRandom) return ((MultiItemRandom)item_(aStack)).mFoodStats.get(meta_(aStack)) != null;
		return item_(aStack).getFoodProperties(aStack, null) != null; // The 1.7.10 equivalent was instanceof ItemFood.
	}
	/** 1.20.1 still has a real ArmorItem class, so the original's instanceof-based check is restored.
	 *  The 26.x EQUIPPABLE component this used to rely on doesn't exist here. */
	public static boolean armor(ItemStack aStack) {
		if (invalid(aStack)) return F;
		return item_(aStack) instanceof net.minecraft.world.item.ArmorItem;
	}

	public static int food(ItemStack aStack) {
		if (invalid(aStack)) return 0;
		{net.minecraft.world.food.FoodProperties tFood = item_(aStack).getFoodProperties(aStack, null); if (tFood != null) {try {return tFood.getNutrition();} catch(Throwable e) {return 1;}}}
		if (item_(aStack) instanceof MultiItemRandom) {
			IFoodStat tStat = ((MultiItemRandom)item_(aStack)).mFoodStats.get(meta_(aStack));
			return tStat == null ? 0 : tStat.getFoodLevel(item_(aStack), aStack, null);
		}
		return 0;
	}
	public static float saturation(ItemStack aStack) {
		if (invalid(aStack)) return 0;
		{net.minecraft.world.food.FoodProperties tFood = item_(aStack).getFoodProperties(aStack, null); if (tFood != null) {try {return tFood.getSaturationModifier();} catch(Throwable e) {return 0.5F;}}}
		if (item_(aStack) instanceof MultiItemRandom) {
			IFoodStat tStat = ((MultiItemRandom)item_(aStack)).mFoodStats.get(meta_(aStack));
			return tStat == null ? 0 : tStat.getSaturation(item_(aStack), aStack, null);
		}
		return 0;
	}
	public static float hydration(ItemStack aStack) {
		if (invalid(aStack)) return 0;
		if (item_(aStack) instanceof MultiItemRandom) {
			IFoodStat tStat = ((MultiItemRandom)item_(aStack)).mFoodStats.get(meta_(aStack));
			return tStat == null ? 0 : tStat.getHydration(item_(aStack), aStack, null);
		}
		return 0;
	}
	
	/** @param aValue the Value of this Stack, when burning inside a Furnace (200 = 1 Burn Process = 5000 HU, max = 32767 (that is 819175 HU)), limited to Short because the vanilla Furnace otherwise can't handle it properly, stupid Mojang... */
	public static ItemStack fuel(ItemStack aStack, short aValue) {ItemNBT.set(aStack, UT.NBT.makeShort(ItemNBT.get(aStack), NBT_FUEL_VALUE, aValue)); return aStack;}
	/** @return the Value of this Stack, when burning inside a Furnace (200 = 1 Burn Process = 5000 HU, max = 32767 (that is 819175 HU)), limited to Short because the vanilla Furnace otherwise can't handle it properly, stupid Mojang... */
	public static long fuel(ItemStack aStack) {
		if (invalid(aStack)) return 0;
		// Neo's fuel value lives on the server/level (FuelValues), not a static registry like 1.7.10's GameRegistry.
		// getBurnTime fires the same FurnaceFuelBurnTimeEvent role that 1.7.10's GameRegistry.getFuelValue played for mods.
		long rFuelValue = net.minecraftforge.common.ForgeHooks.getBurnTime(aStack, null);
		if (rFuelValue > 0) return rFuelValue;
		Item tItem = item_(aStack);
		// 1.7.10 special-cased wooden tool classes that no longer exist.
		// The general fuel table already returns 200 for them, so no separate check is needed.
		if (tItem == Items.STICK) return 100;
		if (CS.Flattened.headItemOf(tItem) == Items.COAL) return 1600;
		if (tItem == Items.BLAZE_ROD) return 2400;
		if (tItem == Items.LAVA_BUCKET) return 20000;
		Block tBlock = block_(tItem);
		// 1.7.10's sapling/slab was one block with a wood-species meta, so any species burned; flattening split them into
		// per-species blocks, so family membership is read via vanilla tags instead of comparing against one species.
		if (tBlock != null && tBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.SAPLINGS)) return 100;
		if (tBlock != null && tBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.WOODEN_SLABS)) return 150;
		if (tBlock == Blocks.COAL_BLOCK) return 16000;
		// Neo has no Material class, so wood-ness reads from SoundType.WOOD instead, the same trick WD's own sound center uses.
		if (tBlock != NB && tBlock.defaultBlockState().getSoundType() == net.minecraft.world.level.block.SoundType.WOOD) return 300;
		return 0;
	}
	
	public static Integer[] toIntegerArray(ItemStack... aStacks) {
		Integer[] rArray = new Integer[aStacks.length];
		for (int i = 0; i < rArray.length; i++) rArray[i] = toInt(aStacks[i]);
		return rArray;
	}
	
	public static int[] toIntArray(ItemStack... aStacks) {
		int[] rArray = new int[aStacks.length];
		for (int i = 0; i < rArray.length; i++) rArray[i] = toInt(aStacks[i]);
		return rArray;
	}
	
	public static String configName(ItemStack aStack) {
		if (invalid(aStack)) return "";
		Object rName = OreDictManager.INSTANCE.getAssociation_(aStack, T);
		if (rName != null) return rName.toString();
		try {if (UT.Code.stringValid(rName = aStack.getItem().getDescriptionId())) return rName.toString();} catch (Throwable e) {/*Do nothing*/} // getUnlocalizedName() is now Item.getDescriptionId(); the key lives on Item, not ItemStack.
		return item_(aStack) + "." + meta_(aStack);
	}
	public static String configNames(ItemStack... aStacks) {
		String rString = "";
		for (ItemStack tStack : aStacks) rString += (tStack == null ? "null;" : configName(tStack) + ";");
		return rString;
	}
	public static String names(ItemStack... aStacks) {
		String rString = "";
		for (ItemStack tStack : aStacks) rString += (tStack == null ? "null; " : tStack.getDisplayName() + "; ");
		return rString;
	}
	public static String namesAndSizes(ItemStack... aStacks) {
		String rString = "";
		for (ItemStack tStack : aStacks) rString += (tStack == null ? "null; " : tStack.getDisplayName() + " " + tStack.getCount() + "; ");
		return rString;
	}
	
	// The old hide() used a NEI API that's a dead no-op on neo, so hidden slab variants were leaking into creative/JEI.
	// A central registry replaces it; blocks are tracked separately since hide(Block) runs before the BlockItem exists.
	public static final ItemStackSet<ItemStackContainer> HIDDEN_ITEMS  = hashset();
	public static final java.util.Set<Block>             HIDDEN_BLOCKS = new java.util.HashSet<>();
	public static boolean hidden(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return F;
		if (HIDDEN_ITEMS.contains(aStack, T)) return T;
		return aStack.getItem() instanceof net.minecraft.world.item.BlockItem tBI && HIDDEN_BLOCKS.contains(tBI.getBlock());
	}
	public static void hide(Item aItem) {
		for (int i = 0; i < 16; i++) hide(aItem, i);
		hide(aItem, W);
	}
	public static void hide(Item aItem, long aMeta) {
		hide(make(aItem, 1, aMeta));
	}
	public static void hide(Block aBlock) {
		HIDDEN_BLOCKS.add(aBlock);
		for (int i = 0; i < 16; i++) hide(aBlock, i);
		hide(aBlock, W);
	}
	public static void hide(Block aBlock, long aMeta) {
		hide(make(aBlock, 1, aMeta));
	}
	public static void hide(ItemStack aStack) {
		if (aStack != null && !aStack.isEmpty()) HIDDEN_ITEMS.add(aStack);
		if (aStack != null) try {codechicken.nei.api.API.hideItem(aStack);} catch(Throwable e) {/**/}
	}
	
	// The original's direct setMaxStackSize calls are restored verbatim.
	// The AT keeps the field mutable, so the deferred-map workaround from 26.x isn't needed.
	public static boolean forceProperMaxStacksizes() {
		setMaxStackSize(net.minecraft.world.item.Items.POTION, 1);
		for (Item tItem : new Item[]{net.minecraft.world.item.Items.GLASS_BOTTLE, net.minecraft.world.item.Items.CAKE, net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.WRITTEN_BOOK, net.minecraft.world.item.Items.WRITABLE_BOOK, net.minecraft.world.item.Items.ENCHANTED_BOOK, net.minecraft.world.item.Items.SNOWBALL, net.minecraft.world.item.Items.EGG})
			setMaxStackSize(tItem, 64);
		// 1.7.10 named bed/door items directly; flattening turned them into whole families, so the registry is walked instead.
		for (Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (tItem instanceof net.minecraft.world.item.BedItem) setMaxStackSize(tItem, 64);
			else if (tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) setMaxStackSize(tItem, 8);
		}
		return T;
	}
	
	public static final Collection<ItemStack> REVERT_TO_BOOK_TO_FIX_STUPID = arraylist();
	public static void fixBookStacks() {for (ItemStack tStack : REVERT_TO_BOOK_TO_FIX_STUPID) set(tStack, make(Items.BOOK, 1, 0), T, T);}
	
	public static final List<String>
	LOOT_TABLES         = new ArrayListNoNulls<>(F, "dungeonChest", "villageBlacksmith", "mineshaftCorridor", "strongholdLibrary", "strongholdCrossing", "strongholdCorridor", "pyramidDesertyChest", "pyramidJungleChest", "pyramidJungleDispenser", "bonusChest"),
	LOOT_TABLES_VANILLA = new ArrayListNoNulls<>(F, "dungeonChest", "villageBlacksmith", "mineshaftCorridor", "strongholdLibrary", "strongholdCrossing", "strongholdCorridor", "pyramidDesertyChest", "pyramidJungleChest", "pyramidJungleDispenser", "bonusChest");
	
	// The original's server-side random sample now lives only in the central ChestGenHooks shim (vanilla+GT6 pool).
	// A local duplicate of that sampling logic is removed.
	private static final java.util.List<net.minecraft.resources.ResourceLocation> VANILLA_LOOT_KEYS = java.util.Arrays.asList(
		net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON, net.minecraft.world.level.storage.loot.BuiltInLootTables.VILLAGE_WEAPONSMITH,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.ABANDONED_MINESHAFT, net.minecraft.world.level.storage.loot.BuiltInLootTables.STRONGHOLD_LIBRARY,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.STRONGHOLD_CROSSING, net.minecraft.world.level.storage.loot.BuiltInLootTables.STRONGHOLD_CORRIDOR,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.DESERT_PYRAMID, net.minecraft.world.level.storage.loot.BuiltInLootTables.JUNGLE_TEMPLE,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER, net.minecraft.world.level.storage.loot.BuiltInLootTables.SPAWN_BONUS_CHEST);
	public static ItemStack generateOneVanillaLoot() {
		return gt6mirror.minecraftforge.common.ChestGenHooks.getOneItem(UT.Code.select("dungeonChest", LOOT_TABLES_VANILLA), RNGSUS);
	}

	public static boolean generateLoot(Random aRandom, String aLoot, Container aInv) {
		try {
			if (aLoot.startsWith("twilightforest:")) {
				if (!TF_TREASURE) return F;
				// Twilight Forest is absent from this build, so this branch is unreachable.
				// The same bridge lives in TwilightTreasureReplacer for when it's present.
				TwilightTreasureReplacer.generate((net.minecraft.world.Container)aInv, aLoot);
			} else if (!LOOT_TABLES_VANILLA.contains(aLoot)) {
				// GT6's own loot categories live entirely in the ChestGenHooks shim buffer, filled by Loader_Loot, matching the original.
				gt6mirror.minecraftforge.common.WeightedRandomChestContent.generateChestContents(aRandom, gt6mirror.minecraftforge.common.ChestGenHooks.getItems(aLoot, aRandom), aInv, gt6mirror.minecraftforge.common.ChestGenHooks.getCount(aLoot, aRandom));
			} else {
				// Vanilla table names map to the vanilla key list for a weighted pull.
				// GT6's own additions are already injected into that table by the shim.
				net.minecraft.server.MinecraftServer tServer = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
				if (tServer != null) {
					net.minecraft.server.level.ServerLevel tLevel = tServer.overworld();
					if (tLevel != null) {
						int tIndex = LOOT_TABLES_VANILLA.indexOf(aLoot);
						net.minecraft.resources.ResourceLocation tKey = (tIndex >= 0 && tIndex < VANILLA_LOOT_KEYS.size()) ? VANILLA_LOOT_KEYS.get(tIndex) : net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON;
						net.minecraft.world.level.storage.loot.LootTable tTable = tServer.getLootData().getLootTable(tKey); // MinecraftServer.java:1465
						net.minecraft.world.level.storage.loot.LootParams tParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(tLevel)
							.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
							.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
						// 1.7.10's loot count came from a category counter that rolled exactly that many random slots, never overflowing.
						// The table's own fill logic requested far more and overflowed containers, so the counter is restored instead.
						java.util.List<ItemStack> tPool = tTable.getRandomItems(tParams);
						for (int tRoll = 0, tCount = gt6mirror.minecraftforge.common.ChestGenHooks.getCount(aLoot, aRandom); tRoll < tCount && !tPool.isEmpty(); tRoll++) {
							ItemStack tPicked = tPool.get(aRandom.nextInt(tPool.size()));
							if (invalid(tPicked)) continue;
							aInv.setItem(aRandom.nextInt(aInv.getContainerSize()), tPicked.copy());
						}
					}
				}
			}
			for (int i = 0, j = aInv.getContainerSize(); i < j; i++) {
				ItemStack tStack = aInv.getItem(i);
				if (invalid(tStack)) continue;
				// Re-resolves enchantment holders against the current server, avoiding a crash on switching worlds mid-session.
				UT.NBT.freshenEnchantments(tStack);
				if (IL.TC_Gold_Coin.exists()) {
					if (item_(tStack) == Items.GOLD_NUGGET) {
						set(tStack, IL.TC_Gold_Coin.get(tStack.getCount()));
					}
					if (item_(tStack) == Items.GOLD_INGOT && tStack.getCount() <= 7) {
						set(tStack, IL.TC_Gold_Coin.get(tStack.getCount() * 9L));
					}
				}
				// The Sus Stew mod is absent from this build, so this whole branch is unreachable.
				// 1.7.10 stored potion effects as a raw NBT list by numeric id; neo stores them as DataComponents instead.
				// if (IL.EtFu_Sus_Stew.exists() && item_(tStack) == Items.MUSHROOM_STEW) {
				//     ListTag tList = new ListTag();
				//     switch(RNGSUS.nextInt(9)) {
				//     case  1: tList.add(UT.NBT.make("EffectId", MobEffect.field_76443_y .id, "EffectDuration",   7)); break;
				//     case  2: tList.add(UT.NBT.make("EffectId", MobEffect.fireResistance.id, "EffectDuration",  80)); break;
				//     case  3: tList.add(UT.NBT.make("EffectId", MobEffect.nightVision   .id, "EffectDuration", 100)); break;
				//     case  4: tList.add(UT.NBT.make("EffectId", MobEffect.weakness      .id, "EffectDuration", 180)); break;
				//     case  5: tList.add(UT.NBT.make("EffectId", MobEffect.regeneration  .id, "EffectDuration", 160)); break;
				//     case  6: tList.add(UT.NBT.make("EffectId", MobEffect.jump          .id, "EffectDuration", 120)); break;
				//     case  7: tList.add(UT.NBT.make("EffectId", MobEffect.poison        .id, "EffectDuration", 240)); break;
				//     case  8: tList.add(UT.NBT.make("EffectId", MobEffect.wither        .id, "EffectDuration", 160)); break;
				//     default: tList.add(UT.NBT.make("EffectId", MobEffect.blindness     .id, "EffectDuration", 160)); break;
				//     }
				//     nbt(set(tStack, IL.EtFu_Sus_Stew.get(tStack.getCount())), UT.NBT.make("Effects", tList));
				// }
				aInv.setItem(i, update_(OM.get_(tStack)));
			}
			return T;
		} catch(Throwable e) {e.printStackTrace(ERR);}
		return F;
	}
	
	public static boolean add(Entity aPlayer, ItemStack aStack) {
		return add(aPlayer, aStack, F);
	}
	public static boolean add(Entity aPlayer, ItemStack aStack, boolean aCurrentSlotFirst) {
		return aPlayer instanceof Player && add(aPlayer, ((Player)aPlayer).getInventory(), aStack, aCurrentSlotFirst);
	}
	public static boolean add(Entity aPlayer, Container aInv, ItemStack aStack, boolean aCurrentSlotFirst) {
		if (aInv != null && valid(aStack)) {
			check(aPlayer, aStack);
			
			// wait no, i cant do this one because of the boolean return!
			//
			// To make sure no accidents cause NEI to make 111 infinite Stacks.
			//if (aStack.stackSize > 64) {
			//addStackToPlayerInventory(aPlayer, aInv, amount(64, aStack), F);
			//aStack.stackSize -= 64;
			//}
			
			for (int i = 0; i < 36; i++) if (!(aPlayer instanceof Player) || i != ((Player)aPlayer).getInventory().selected) {
				ItemStack tStack = aInv.getItem(i);
				if (equal(tStack, aStack) && aStack.getCount() + tStack.getCount() <= tStack.getMaxStackSize()) {
					tStack.setCount(tStack.getCount()+(aStack.getCount()));
					update(aPlayer);
					return T;
				}
			}
			if (aCurrentSlotFirst && aPlayer instanceof Player) {
				ItemStack tStack = aInv.getItem(((Player)aPlayer).getInventory().selected);
				if (tStack == null || tStack.getCount() == 0) {
					aInv.setItem(((Player)aPlayer).getInventory().selected, aStack);
					update(aPlayer);
					return T;
				} else if (equal(tStack, aStack) && aStack.getCount() + tStack.getCount() <= tStack.getMaxStackSize()) {
					tStack.setCount(tStack.getCount()+(aStack.getCount()));
					update(aPlayer);
					return T;
				}
			}
			for (int i = 0; i < 36; i++) if (!(aPlayer instanceof Player) || i != ((Player)aPlayer).getInventory().selected) {
				ItemStack tStack = aInv.getItem(i);
				if (tStack == null || tStack.getCount() <= 0) {
					aInv.setItem(i, aStack);
					update(aPlayer);
					return T;
				}
			}
			if (!aCurrentSlotFirst && aPlayer instanceof Player) {
				ItemStack tStack = aInv.getItem(((Player)aPlayer).getInventory().selected);
				if (tStack == null || tStack.getCount() == 0) {
					aInv.setItem(((Player)aPlayer).getInventory().selected, aStack);
					update(aPlayer);
					return T;
				} else if (equal(tStack, aStack) && aStack.getCount() + tStack.getCount() <= tStack.getMaxStackSize()) {
					tStack.setCount(tStack.getCount()+(aStack.getCount()));
					update(aPlayer);
					return T;
				}
			}
		}
		return F;
	}
	public static boolean give(Entity aPlayer, ItemStack aStack) {
		return give(aPlayer, aStack, F, aPlayer.level(), aPlayer.getX(), aPlayer.getY(), aPlayer.getZ());
	}
	public static boolean give(Entity aPlayer, ItemStack aStack, boolean aCurrentSlotFirst) {
		return give(aPlayer, aStack, aCurrentSlotFirst, aPlayer.level(), aPlayer.getX(), aPlayer.getY(), aPlayer.getZ());
	}
	public static boolean give(Entity aPlayer, ItemStack aStack, Level aWorld, double aX, double aY, double aZ) {
		return give(aPlayer, aStack, F, aWorld, aX, aY, aZ);
	}
	public static boolean give(Entity aPlayer, ItemStack aStack, boolean aCurrentSlotFirst, Level aWorld, double aX, double aY, double aZ) {
		if (valid(aStack) && !add(aPlayer, aStack, aCurrentSlotFirst)) drop(aWorld, aX, aY, aZ, aStack);
		return T;
	}
	public static boolean give(Entity aPlayer, Container aInv, ItemStack aStack, boolean aCurrentSlotFirst, Level aWorld, double aX, double aY, double aZ) {
		if (valid(aStack) && !add(aPlayer, aInv, aStack, aCurrentSlotFirst)) drop(aWorld, aX, aY, aZ, aStack);
		return T;
	}
	
	public static boolean achieve(Entity aPlayer, Advancement aAchievement) {
		if (aAchievement == null|| !(aPlayer instanceof Player) || aPlayer.level() == null || aPlayer.level().isClientSide()) return F;
		// Neo removed Achievement/AchievementList/triggerAchievement entirely; the only replacement, award(), also grants
		// recipes/XP/chat/an event, which isn't a faithful substitute, so this becomes a centralized no-op instead.
		return T;
	}
	
	// The achievement API this called is gone from neo entirely; award() would add unwanted rewards, so this stays a
	// centralized no-op instead, with the original constant kept only for tracing.
	public static boolean check(Entity aPlayer, ItemStack aStack) {
		if (!(aPlayer instanceof Player) || aPlayer.level() == null || aPlayer.level().isClientSide()) return F;

		if (F /* This only gated a vanilla achievement that neo now grants automatically via advancements, so the check stays false. */) {
			// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.portal);
		}

		if (invalid(aStack)) return F;

		OreDictItemData tData = OM.association_(aStack);
		Item aItem = item(aStack);
		Block aBlock = block(aItem);
		String aRegName = regName(aItem);

		if (WoodDictionary.WOODS.containsKey(aStack, T) || WoodDictionary.BEAMS.containsKey(aStack, T) || WoodDictionary.PLANKS_ANY.containsKey(aStack, T) || OD.logWood.is_(aStack) || OD.logRubber.is_(aStack)) {
			// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.mineWood);
		}

		// This block only granted vanilla tool achievements by instanceof.
		// Advancements auto-grant now, and those tool classes are gone anyway.

		if (MD.MC.owns(aRegName)) {
			if (CS.Flattened.headItemOf(aItem) == Items.COOKED_COD) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.cookFish);
			} else
			if (aItem == Items.BREAD) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.makeBread);
			} else
			if (aItem == Items.LEATHER || aItem == Items.BEEF || aItem == Items.COOKED_BEEF || aItem == Items.SADDLE) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.killCow);
			} else
			if (aBlock == Blocks.CAKE || aItem == Items.CAKE) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.bakeCake);
			} else
			if (aBlock == Blocks.FURNACE || aBlock == Blocks.FURNACE) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.buildFurnace);
			} else
			if (aItem == Items.GHAST_TEAR) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.portal);
			} else
			if (aItem == Items.BREWING_STAND || aBlock == Blocks.BREWING_STAND || aItem == Items.BLAZE_ROD || aItem == Items.BLAZE_POWDER || aItem == Items.ENDER_EYE) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.blazeRod);
			} else
			if (aBlock == Blocks.ENCHANTING_TABLE) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.enchantments);
			} else
			if (aBlock == Blocks.BOOKSHELF) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.bookcase);
			}
		}

		if (MD.TF.owns(aRegName)) {
			if (IL.TF_Trophy_Naga.equal(aStack, F, T)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressNaga);
			} else if (IL.TF_Trophy_Lich.equal(aStack, F, T)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressLich);
			} else if (IL.TF_Trophy_Hydra.equal(aStack, F, T)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressHydra);
			} else if (IL.TF_Trophy_Urghast.equal(aStack, F, T)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressUrghast);
			} else if (IL.TF_Trophy_Snowqueen.equal(aStack, F, T)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressGlacier);
			} else if (IL.TF_Lamp_of_Cinders.equal(aStack, T, T)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressThorns);
			} else if (IL.TF_Cube_of_Annihilation.equal(aStack, T, T)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressCastle);
			}
		}
		
		if (tData != null && !tData.mPrefix.containsAny(TD.Prefix.ORE_PROCESSING_BASED, TD.Prefix.ORE)) {
			if (ANY.Diamond.mToThis.contains(tData.mMaterial.mMaterial) && tData.mPrefix.contains(TD.Prefix.GEM_BASED)) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.diamonds);
			}
			if (ANY.Iron.mToThis.contains(tData.mMaterial.mMaterial)) {
				// FORCED-ADAPTATION(F18): achieve(aPlayer, AchievementList.acquireIron);
			}
			if (MD.TF.mLoaded && tData.mMaterial.mMaterial.mOriginalMod == MD.TF && tData.mMaterial.mMaterial.contains(TD.Properties.MAZEBREAKER)) {
				achieve(aPlayer, TFAchievementPage.twilightProgressHydra);
			}
		}
		return T;
	}
	
	public static void denull(Entity  aPlayer) {if (aPlayer instanceof Player) denull(((Player)aPlayer).getInventory());}
	public static void denull(Container aInv) {
		if (aInv != null) for (int i = 0, j = aInv.getContainerSize(); i < j; i++) {
			ItemStack tStack = aInv.getItem(i);
			if (tStack != null && (tStack.getCount() == 0 || tStack.getItem() == null)) aInv.setItem(i, ItemStack.EMPTY); // Neo's inventory list throws on setItem(i,null); 1.7.10 used null there to clear a slot legally.
		}
	}
	
	public static ItemStack projectile(Entity  aPlayer, TagData aType) {if (aPlayer instanceof Player) return projectile(((Player)aPlayer).getInventory(), aType); return null;}
	public static ItemStack projectile(Container aInv, TagData aType) {
		if (aInv != null) for (int i = 0, j = aInv.getContainerSize(); i < j; i++) {
			ItemStack rStack = aInv.getItem(i);
			if (ST.valid(rStack) && rStack.getItem() instanceof IItemProjectile && ((IItemProjectile)rStack.getItem()).hasProjectile(aType, rStack)) return ST.update(rStack);
		}
		return null;
	}
	
	/** Loads an ItemStack properly. */
	public static ItemStack load(CompoundTag aNBT, String aTagName) {
		return aNBT == null ? null : load(aNBT.getCompound(aTagName), NI);
	}
	/** Loads an ItemStack properly. */
	public static ItemStack load(CompoundTag aNBT, String aTagName, ItemStack aDefault) {
		return aNBT == null ? null : load(aNBT.getCompound(aTagName), aDefault);
	}
	
	/** Loads an ItemStack properly. */
	public static ItemStack load(CompoundTag aNBT) {
		return load(aNBT, NI);
	}
	/** Loads an ItemStack properly. */
	public static ItemStack load(CompoundTag aNBT, ItemStack aDefault) {
		if (aNBT == null || aNBT.isEmpty()) return null;
		// A zero count can't be built directly on neo (it collapses to EMPTY, losing identity), so this builds it at count 1
		// and marks it ZEROSIZE via size_, the same representation GT6 code already relies on at runtime.
		int tCount = aNBT.getInt("Count");
		// The numeric item id is a registry INDEX on this engine: adding any mod shifts it and every saved
		// stack would come back as a different item, so the registry NAME decides whenever it is present.
		Item tItem = itemByRegName(aNBT.getString("reg"));
		ItemStack rStack = tItem == null ? null : make(tItem, tCount <= 0 ? 1 : tCount, aNBT.getShort("Damage"));
		// Records written before the name existed carry only the shifting index, so the ore dictionary
		// name — saved next to it and describing the same unified stack — is trusted ahead of that index.
		if (rStack == null && aNBT.contains("od")) rStack = OreDictManager.INSTANCE.getStack(aNBT.getString("od"), tCount <= 0 ? 1 : tCount);
		if (rStack == null) rStack = make(Item.byId(aNBT.getShort("id")), tCount <= 0 ? 1 : tCount, aNBT.getShort("Damage"));
		if (rStack == null) return aDefault == null ? null : update_(OM.get_(aDefault));
		if (tCount <= 0) size_(0, rStack);
		// Has to use setTagCompound instead of putting it into make()
		// because it would delete certain Tags on load, making stuff like unscanned Forestry Bees unstackable.
		// But update_() will still delete a completely empty NBT later on.
		ItemNBT.set(rStack, aNBT.contains("tag") ? aNBT.getCompound("tag") : null); // getCompound returns an empty tag when absent; the original expected null there.
		// Does anyone even migrate IC2exp Items anymore? This is only used when updating from IC2-Non-Exp to IC2-Exp.
	//  if (item_(rStack).getClass().getName().startsWith("ic2.core.migration")) item_(rStack).onUpdate(rStack, DW, null, 0, F); // I do not think this could possibly happen anymore
		return update_(OM.get_(rStack));
	}

	/** Saves an ItemStack properly. */
	public static CompoundTag save(String aTagName, Block aBlock) {
		CompoundTag aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aBlock, 1, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(String aTagName, Block aBlock, long aStackSize) {
		CompoundTag aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aBlock, aStackSize, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(String aTagName, Block aBlock, long aStackSize, long aMeta) {
		CompoundTag aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aBlock, aStackSize, aMeta));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(String aTagName, Item aItem) {
		CompoundTag aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aItem, 1, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(String aTagName, Item aItem, long aStackSize) {
		CompoundTag aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aItem, aStackSize, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(String aTagName, Item aItem, long aStackSize, long aMeta) {
		CompoundTag aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aItem, aStackSize, aMeta));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(String aTagName, ItemStack aStack) {
		CompoundTag aNBT = UT.NBT.make();
		CompoundTag tNBT = save(aStack);
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(CompoundTag aNBT, String aTagName, Block aBlock) {
		if (aNBT == null) aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aBlock, 1, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(CompoundTag aNBT, String aTagName, Block aBlock, long aStackSize) {
		if (aNBT == null) aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aBlock, aStackSize, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(CompoundTag aNBT, String aTagName, Block aBlock, long aStackSize, long aMeta) {
		if (aNBT == null) aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aBlock, aStackSize, aMeta));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(CompoundTag aNBT, String aTagName, Item aItem) {
		if (aNBT == null) aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aItem, 1, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(CompoundTag aNBT, String aTagName, Item aItem, long aStackSize) {
		if (aNBT == null) aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aItem, aStackSize, 0));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(CompoundTag aNBT, String aTagName, Item aItem, long aStackSize, long aMeta) {
		if (aNBT == null) aNBT = UT.NBT.make();
		CompoundTag tNBT = save(make(aItem, aStackSize, aMeta));
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	public static CompoundTag save(CompoundTag aNBT, String aTagName, ItemStack aStack) {
		if (aNBT == null) aNBT = UT.NBT.make();
		CompoundTag tNBT = save(aStack);
		if (tNBT == null) aNBT.remove(aTagName); else aNBT.put(aTagName, tNBT);
		return aNBT;
	}
	/** Saves an ItemStack properly. */
	/** 1.7.10 wrote stack fields directly into the passed tag.
	 *  neo's component model is reproduced via save()+merge instead, keeping the same id/Count/Damage keys. */
	public static CompoundTag writeToNBT(ItemStack aStack, CompoundTag aNBT) {
		CompoundTag tNBT = save(aStack);
		return tNBT == null ? aNBT : aNBT.merge(tNBT);
	}

	public static CompoundTag save(ItemStack aStack) {
		if (aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null || aStack.getCount() < 0) return null;
		CompoundTag rNBT = UT.NBT.make();
		aStack = OM.get_(aStack);
		rNBT.putShort("id", id(aStack));
		// The name survives a changed mod set; the numeric id above stays only so older builds can still read this.
		String tRegName = regName(aStack);
		if (tRegName != null) rNBT.putString("reg", tRegName);
		// Writes the logical size, not the physical count: the ZEROSIZE ghost's NBT format doesn't know about
		// components, so omitting Count for zero (the usual shortcut) would read back as 1 and resurrect a lost item.
		int tLogicalCount = count(aStack);
		if (tLogicalCount <= 0) rNBT.putInt("Count", 0); else UT.NBT.setNumber(rNBT, "Count", tLogicalCount);
		UT.NBT.setNumber(rNBT, "Damage", meta_(aStack));
		if (ItemNBT.has(aStack)) rNBT.put("tag", ItemNBT.get(aStack));
		OreDictItemData tData = OM.anyassociation_(aStack);
		if (tData != null) rNBT.putString("od", tData.toString());
		return rNBT;
	}
}
