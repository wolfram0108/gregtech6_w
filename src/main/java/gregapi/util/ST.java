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

import net.neoforged.neoforge.registries.DeferredRegister;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraftforge.fluids.IFluidContainerItem;
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
	// F4-flatten, family wildcard: 1.7.10 wrote "any subtype" as ONE stack with meta W (stained_glass:W = any
	// colored glass). In neo the family is separate Items, so one stack no longer covers it, and the "item in
	// item" comparison failed: the machine accepted only the variant literally named (white). The rule "wildcard +
	// member of the same family = match" is kept by the CS.Flattened map center; the path is cheap — it only kicks in
	// when the items are DIFFERENT and one of the stacks has meta W (rare in a hot comparison).
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
	public static boolean   valid(ItemStack aStack) {return aStack != null && !aStack.isEmpty() && item_(aStack) != null;} // F15 (BUG-011): mirrors invalid below — a zeroed split object is NOT a "valid item"
	// F15 (BUG-011, "hand slot permanently occupied"): 1.7.10 empty = null; neo empty = isEmpty() (count<=0 OR air),
	// and a zeroed split object (e.g. the hand after a stack transfer) is NOT the ItemStack.EMPTY singleton. The old
	// check "== EMPTY || count < 0" treated such a stack as VALID -> gates like anvil's onBlockActivated3 went into
	// the "item in hand" branch, so clicking with an empty hand was unreachable. Size-0 catalysts are unaffected:
	// they physically hold count=1 + the ZEROSIZE marker (see F-size0-catalyst above, ST.size:198).
	public static boolean invalid(ItemStack aStack) {return aStack == null || aStack.isEmpty() || item_(aStack) == null;}
	
	public static ItemStack validate(ItemStack aStack) {return valid(aStack)                         ? aStack : null;}
	public static ItemStack valisize(ItemStack aStack) {return valid(aStack) && aStack.getCount() > 0 ? aStack : null;}

	/**
	 * F15 (GT6<->engine bridge, "item model null<->EMPTY"): GT6 stores an empty slot as {@code null}
	 * (see {@code CS.NI}, the whole {@link ST} is built on null), while the engine never tolerates null — it
	 * requires the {@code ItemStack.EMPTY} singleton. Plus GT6 treats a stack with {@code count==0} as a MEANINGFUL
	 * state distinct from null ({@code TileEntityBase05Inventories.decrStackSizeGUI}: {@code allowZeroStacks} →
	 * {@code setCount(0)} without nulling it out) — the engine's {@code isEmpty()} (true when {@code count<=0})
	 * cannot tell the two apart, so the boundary bridges STRICTLY by reference equality with the
	 * {@code ItemStack.EMPTY} singleton, not by {@code isEmpty()}. This is the ONLY conversion point between the
	 * two models — the whole mod must cross the boundary only via {@link #nn} / {@link #ni}, never ad-hoc ternaries.
	 */
	public static ItemStack nn(ItemStack aStack) {return aStack == null ? ItemStack.EMPTY : aStack;}
	/** F15 reverse boundary: neo returns EMPTY where 1.7.10 returned null (getCarried cursor, Inventory.getItem slots).
	 *  GT6 bodies reason in null semantics 1:1 — on input from the engine we normalize EMPTY->null with this helper (paired with {@link #nn}). */
	public static ItemStack n(ItemStack aStack) {return aStack == null || aStack.isEmpty() ? null : aStack;}
	/** F15: engine->GT6. The engine's {@code ItemStack.EMPTY} singleton (and null) -> null (GT6-empty); a MEANINGFUL
	 *  {@code count==0} (a stack that is not the same object as {@code ItemStack.EMPTY}) survives the boundary as-is. */
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
	// F12-followup (subtype-meta): the GT6 subtype is stored in the GT_API.SUBTYPE component (not the damage value —
	// that clamps to [0,maxDamage], and meta items have maxDamage=0 -> collapses to 0). meta-0 = component absent (default 0, a vanilla-created stack).
	public static short     meta_(ItemStack aStack) {return gregapi.GT_API.SUBTYPE.isBound() ? (short)(int)aStack.getOrDefault(gregapi.GT_API.SUBTYPE.get(), 0) : 0;}
	public static ItemStack meta (ItemStack aStack, long aMeta) {return aStack == null ? null : meta_(aStack, aMeta);}
	public static ItemStack meta_(ItemStack aStack, long aMeta) {int tMeta = (short)aMeta; if (gregapi.GT_API.SUBTYPE.isBound()) {if (tMeta != 0) aStack.set(gregapi.GT_API.SUBTYPE.get(), tMeta); else aStack.remove(gregapi.GT_API.SUBTYPE.get());} return aStack;}

	/**
	 * BUG-079, the ONLY answer to "what makes an item a SEPARATE item for an external display".
	 *
	 * <p>In 1.7.10 identity = {@code item + damage} (meta); NBT was not part of the NEI comparison. In neo
	 * components are declared explicitly ({@code registerItemSubtypes}), and some GT6 families without NBT
	 * collapse into duplicates — coins/batteries/chests differ by their NBT material. So the decision belongs
	 * to the item itself: {@link gregapi.item.multiitem.MultiItem#identityIncludesNBT()} (default "yes",
	 * {@code MultiItemTool} — "no": for a tool NBT is state, not identity).</p>
	 *
	 * <p>NOBODY but this method should hold this rule: it is asked by both the JEI subtype declaration
	 * ({@code GT6_JEI_Plugin.registerItemSubtypes}) and the parity judges ({@code PortDump}, the
	 * {@code gt6jeicraft} probe). A copy of the policy drifts from the original on the exceptions — lesson of BUG-070.</p>
	 */
	public static boolean identityIncludesNBT(net.minecraft.world.item.Item aItem) {
		return !(aItem instanceof gregapi.item.multiitem.MultiItem tMulti) || tMulti.identityIncludesNBT();
	}

	/** Identity key of a stack for an external display: item + meta, plus NBT — but only when it is part of identity
	 *  ({@link #identityIncludesNBT}). The display matches an item against a recipe output with this same key. */
	public static String identityKey(ItemStack aStack) {
		if (aStack == null || aStack.getItem() == null) return "null";
		var tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem());
		String rKey = (tKey == null ? "" : tKey.toString()) + ":" + meta_(aStack);
		if (!identityIncludesNBT(aStack.getItem())) return rKey;
		net.minecraft.nbt.CompoundTag tNBT = gregapi.code.ItemNBT.get(aStack);
		return rKey + "|" + (tNBT == null ? "-" : tNBT.toString());
	}

	// F-size0-catalyst: logical size. A GT6 size-0 stack (catalyst) is stored in neo as count=1 + the GT_API.ZEROSIZE marker
	// (neo cannot hold count<=0 without turning into AIR/EMPTY). size() returns 0 for marked stacks -> recipe-matching/consume/dump
	// see the logical 0. Matches the old behavior of size(AIR catalyst)=0 -> existing callers are unaffected.
	public static byte      size (ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null || aStack.getCount() < 0 ? 0 : (gregapi.GT_API.ZEROSIZE.isBound() && aStack.has(gregapi.GT_API.ZEROSIZE.get()) ? 0 : UT.Code.bindByte(aStack.getCount()));}
	/** F15-size0 (BUG-015 v2): the LOGICAL count as int, without ST.size's byte clamp — the 1.7.10 stackSize field
	 *  reader for large stacks (mass storage holds thousands). A ZEROSIZE ghost ("type remembered, 0 units") reads as 0. */
	public static int count(ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || aStack.getCount() < 0 ? 0 : (gregapi.GT_API.ZEROSIZE.isBound() && aStack.has(gregapi.GT_API.ZEROSIZE.get()) ? 0 : aStack.getCount());}
	public static ItemStack size (long aSize, ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : size_(aSize, aStack);}
	// aSize<=0 = GT6 catalyst: keep count=1 (otherwise neo -> EMPTY/AIR, identity lost) + the ZEROSIZE marker; the logical
	// size is read via ST.size(). aSize>=1 -> a normal setCount + drop the marker (if the stack is being reused).
	public static ItemStack size_(long aSize, ItemStack aStack) {
		if (aSize <= 0) {aStack.setCount(1); if (gregapi.GT_API.ZEROSIZE.isBound()) aStack.set(gregapi.GT_API.ZEROSIZE.get(), net.minecraft.util.Unit.INSTANCE);}
		else {aStack.setCount((int)aSize); if (gregapi.GT_API.ZEROSIZE.isBound() && aStack.has(gregapi.GT_API.ZEROSIZE.get())) aStack.remove(gregapi.GT_API.ZEROSIZE.get());}
		return aStack;
	}
	
	public static byte maxsize(ItemStack aStack) {return (byte)(aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? 64 : item_(aStack).getMaxStackSize(aStack));}
	
	public static ItemStack copy (ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : copy_(aStack);}
	/** F15-size0 INVARIANT (BUG-015, see decisions/F-size0-catalyst): in GT6 code a "zero with type memory" is stored
	 *  ONLY as a ZEROSIZE ghost (count=1+marker, written via {@link #size_}) — such a stack copies NORMALLY.
	 *  A physical count<=0 is legal only as "consumed" (the stack is dead to the engine: the hand after spending it,
	 *  etc.) and MUST NOT BE USED AS A COPY TEMPLATE (neo copy()/getItem() are blind on it: EMPTY/AIR — 1.7.10 always
	 *  copied item/NBT, and this gap used to cause an endless mass-storage duplication loop). The branch below is NOT
	 *  the normal path but an emergency airbag with a siren: the invariant violation is fixed at the CALLER
	 *  (by routing the zero through size_), not here. */
	private static int sZeroCopyWarnings = 0;
	public static ItemStack copy_(ItemStack aStack) {
		if (aStack.getCount() <= 0 && aStack != ItemStack.EMPTY) {
			if (sZeroCopyWarnings < 10) {sZeroCopyWarnings++; ERR.println("[GT6] F15-size0 INVARIANT VIOLATION: copy of a physically-zero stack (a consumed stack used as a template?) — fix the caller: a zero with type memory is written via ST.size_ (ZEROSIZE ghost). Emergency airbag triggered (" + sZeroCopyWarnings + "/10 warnings):"); new Throwable().printStackTrace(ERR);}
			int tOldCount = aStack.getCount();
			aStack.setCount(1);
			ItemStack rStack = aStack.copy();
			aStack.setCount(tOldCount);
			if (rStack == ItemStack.EMPTY) return rStack; // the object was genuinely air — nothing to copy
			rStack.setCount(Math.max(0, tOldCount));
			return rStack;
		}
		return aStack.copy();
	}
	
	public static ItemStack name (ItemStack aStack, String aName) {return aStack == null || aName == null ? aStack : name_(aStack, aName);}
	public static ItemStack name_(ItemStack aStack, String aName) {aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(aName)); return aStack;}
	/** F1/F8 seam: used to be {@code ItemStack.setStackDisplayName(...)} — gone in neo; when the name is already a
	 *  {@code Component} (e.g. neo's {@code getDisplayName()} returns a Component, not the 1.7.10 String) — set
	 *  CUSTOM_NAME directly, preserving formatting (not via literal(String), which would lose the style). Same center — {@code DataComponents.CUSTOM_NAME}. */
	public static ItemStack name_(ItemStack aStack, net.minecraft.network.chat.Component aName) {aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, aName); return aStack;}

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
	
	/**
	 * F12/R3: the single point through which the whole mod registers an Item — used to be a direct
	 * invented {@code DeferredRegister.registerItem(...)} (NeoForge's DeferredRegister has no such
	 * static method). Now it goes through the centralized bridge {@code gregapi.GT_API}
	 * (decisions/F12-registration-lifecycle.md) — the whole mod talks to ONE center, no one
	 * creates its own DeferredRegister on the spot.
	 */
	public static void register(Item aItem, String aRegistryName) {
		GT_API.registerItem(aItem, aRegistryName);
	}

	/** F16/F10 (1:1): 1.7.10 Item.setMaxStackSize(n) mutated the item at runtime. neo: a GT6 item (ItemBase) holds a
	 *  field + overrides getMaxStackSize; a VANILLA/foreign item is immutable (stack size = the MAX_STACK_SIZE default
	 *  component) — the standard way to change it is a neo ModifyDefaultComponentsEvent. We accumulate the intent in a
	 *  map; WIRED UP: applyVanillaComponentOverrides (below, subscribed on the mod bus in GT_API) applies it on the
	 *  event. Callers are foreign items (TC/TF) behind .exists(): in this build the foreign mods are absent -> the map is empty -> nothing to apply (F10). */
	/*  Map type: ConcurrentHashMap, NOT IdentityHashMap. The reason is not style but two engine facts:
	 *  (1) component baking runs on a BACKGROUND executor (ReloadableServerResources.java:123-125,
	 *      supplyAsync(..., backgroundExecutor)), while the server thread writes here on LevelEvent.Load
	 *      (GT_API.onLevelLoadEarlyItemInit -> runDeferredItemInit -> prefix listeners) — the map lives under a race;
	 *  (2) IdentityHashMap.resize() DISCARDS the old table, and its Map.Entry is only an index into a table snapshot
	 *      (java.util.IdentityHashMap$EntryIterator$Entry), so any deferred read of such an entry goes stale.
	 *  Key identity across the type swap is PRESERVED: net.minecraft.world.item.Item has no own
	 *  equals/hashCode (inherited from Object) -> ConcurrentHashMap's equals semantics here are identical to ==.
	 *  The map accepts no null keys/values — and gets none: setMaxStackSize below rejects a null item, and the
	 *  value comes from an int (autoboxing, null is impossible). */
	public static final java.util.Map<Item, Integer> VANILLA_STACKSIZE_OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();
	public static Item setMaxStackSize(Item aItem, int aSize) {
		if (aItem instanceof gregapi.item.ItemBase) return ((gregapi.item.ItemBase)aItem).setMaxStackSize(aSize);
		if (aItem != null) VANILLA_STACKSIZE_OVERRIDES.put(aItem, aSize);
		return aItem;
	}

	/** F16/F10: applies vanilla/foreign stack-size overrides on ModifyDefaultComponentsEvent (mod bus, GT_API). Timing-safe:
	 *  the direct vanilla tweaks (golden ST.forceProperMaxStacksizes 1:1) are applied right in the handler (load-order independent).
	 *  craftRemainder cannot be applied this way — neo Item's craftingRemainingItem is an immutable field (not a component), see setContainerItem. */
	public static void applyVanillaComponentOverrides(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent aEvent) {
		// BUG-021 v3 (timing): the old carrier of applyAllStackSizes was onModPostInit2Deferred (server-start), but
		// THIS event is mod-load (RegistrationEvents.init, AFTER CommonSetup: CommonModLoader.java:74-78) -> the map
		// used to be applied EMPTY, so all GT6 vanilla stack sizes (pearls/plates/ice/clay/... from
		// OreDictPrefix.applyStackSizes) were dead. stacksizes.cfg is already read in onModPreInit2 (GT_API:817) ->
		// we fill the map HERE, before it is applied. The repeat call on server-start (GT_API:1140, the 1:1 spot)
		// stays — it is idempotent (same value).
		gregapi.oredict.OreDictPrefix.applyAllStackSizes();
		// foreign-mod overrides (the map, when a foreign item is present)
		// BUG-041R (stale capture): the value must be snapshotted HERE, into a local variable, not read from the
		// Map.Entry inside the lambda. The engine does not run this lambda now: modify() only PUTS it into the static
		// DataComponentModifiers.MODIFIERS_BY_ITEM (the event is posted once per process, RegistrationEvents.java:22),
		// and it runs on EVERY component bake — i.e. on every world load and on /reload
		// (ReloadableServerResources.java:124 build -> :175 apply). Between the subscription and the second bake the
		// map keeps growing (prefix listeners on LevelEvent.Load); the old IdentityHashMap went through a resize,
		// the resize discarded the table the captured Entry pointed at, and tE.getValue() returned null.
		// b.set(type, null) means the engine DELETES the component (DataComponentMap.java:143-149), and an item
		// without MAX_STACK_SIZE stacks to 1 (Item.java:176-177 getOrDefault(..., 1)) — hence "dirt/logs/wall stack
		// to 1" from the SECOND world entry onward. A local value closes the lambda over a constant: nothing goes stale.
		// The application method (modify per item) and its place in the chain are kept deliberately: MODIFIERS_BY_ITEM
		// is applied BEFORE MODIFIERS_BY_PREDICATE (DataComponentModifiers.java:27-37), and within it in merge order,
		// so the 1.7.10 literal tweaks further below override the map. Switching the map to modifyMatching would
		// flip this order and revert BUG-021.
		for (java.util.Map.Entry<Item, Integer> tE : VANILLA_STACKSIZE_OVERRIDES.entrySet()) {
			final int tStackSize = tE.getValue();
			aEvent.modify(tE.getKey(), b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, tStackSize));
		}
		// golden ST.forceProperMaxStacksizes() 1:1: GT6 changed the stack size of vanilla items (potion->1; glass_bottle/cake/stick/
		// books/snowball/egg->64; bed->64; doors->8). 1.7.10's "bed"/"wooden_door" got flattened in neo -> apply to ALL variants of the class.
		aEvent.modify(net.minecraft.world.item.Items.POTION, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1));
		for (Item tItem : new Item[]{net.minecraft.world.item.Items.GLASS_BOTTLE, net.minecraft.world.item.Items.CAKE, net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.WRITTEN_BOOK, net.minecraft.world.item.Items.WRITABLE_BOOK, net.minecraft.world.item.Items.ENCHANTED_BOOK, net.minecraft.world.item.Items.SNOWBALL, net.minecraft.world.item.Items.EGG})
			aEvent.modify(tItem, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64));
		aEvent.modifyMatching((tItem, tComps) -> tItem instanceof net.minecraft.world.item.BedItem, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64)); // 1.7.10 bed->64 (all colors)
		aEvent.modifyMatching((tItem, tComps) -> tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof net.minecraft.world.level.block.DoorBlock, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 8)); // 1.7.10 wooden_door+iron_door->8 (all doors)
	}

	/** F8 read-modify-write: 1.7.10 `stack.getTagCompound().putX(k,v)` mutated the stack's LIVE tag. neo's CustomData
	 *  is immutable — `ItemNBT.get` returns a COPY, so a mutation without writing it back is LOST (see the ItemNBT
	 *  javadoc, `gt6-contract-seams-blindspot`). These center helpers do an atomic get(copy)->put->set-back —
	 *  the only 1:1-correct path for a pinpoint write into item NBT. */
	public static void nbtPut(ItemStack aStack, String aKey, net.minecraft.nbt.Tag aValue) {
		CompoundTag tNBT = ItemNBT.get(aStack); if (tNBT == null) tNBT = new CompoundTag();
		tNBT.put(aKey, aValue); ItemNBT.set(aStack, tNBT);
	}
	public static void nbtPutByte(ItemStack aStack, String aKey, byte aValue) {
		CompoundTag tNBT = ItemNBT.get(aStack); if (tNBT == null) tNBT = new CompoundTag();
		tNBT.putByte(aKey, aValue); ItemNBT.set(aStack, tNBT);
	}
	/** F8: 1.7.10 ST.hasNBT(stack)/setTagCompound(nbt) -> the ItemNBT.has/set center (CUSTOM_DATA). 1:1 per the ItemNBT javadoc. */
	public static boolean hasNBT(ItemStack aStack) {return ItemNBT.has(aStack);}
	public static void setNBT(ItemStack aStack, CompoundTag aNBT) {ItemNBT.set(aStack, aNBT);}

	/** F16/F10 external-compat: 1.7.10 Item.setContainerItem(Item) set the crafting remainder (bucket->empty) at runtime.
	 *  neo's craftingRemainingItem is an IMMUTABLE Item field (Item.java:303, not a DataComponent) -> it cannot be
	 *  changed post-construction by a standard event (unlike MAX_STACK_SIZE). neo's remainder model is per-recipe
	 *  (recipe.getRemainingItems). Callers are ONLY foreign buckets (RC/TC/TF) behind .exists(): in this build the
	 *  foreign mods are absent -> the map is empty, no matching items/recipes exist -> the path is inert (F10). The
	 *  map stays visible for a future per-recipe application. Not a stub of the core. */
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

	/**
	 * F12/R7: the SINGLE item-lookup point by (modId, name) — used to be an invented
	 * {@code DeferredRegister.findItem(...)} (1.7.10's {@code GameRegistry.findItem} mechanically renamed to
	 * a nonexistent method). The real neo path: {@code BuiltInRegistries.ITEM} — a {@code DefaultedRegistry<Item>}
	 * (verified: neo-decompiled/net/minecraft/core/registries/BuiltInRegistries.java:185). Its
	 * {@code getValue(Identifier)} is overridden {@code @NonNull} and would return AIR for an unknown one
	 * (DefaultedRegistry.java:12) — so we check {@code containsKey(Identifier)} first (Registry.java:104),
	 * to keep the original "null for unknown" 1:1. {@code Identifier.fromNamespaceAndPath}
	 * — verified (used in DeferredRegister.java:230).
	 */
	/** Suffix of the companion key that stores a registry NAME next to a legacy numeric id. */
	private static final String REG_SUFFIX = ".reg";

	/** Writes a block into NBT by NAME (plus the legacy index, so older builds still read it):
	 *  the index is a registry position and shifts as soon as the set of installed items changes. */
	public static void putBlock(net.minecraft.nbt.CompoundTag aNBT, String aKey, Block aBlock) {
		if (aNBT == null || aBlock == null || aBlock == NB) return;
		aNBT.putInt(aKey, BuiltInRegistries.BLOCK.getId(aBlock));
		Identifier tID = BuiltInRegistries.BLOCK.getKey(aBlock);
		if (tID != null) aNBT.putString(aKey + REG_SUFFIX, tID.toString());
	}

	/** Block written by {@link #putBlock}; the name decides, the legacy index is the fallback. */
	public static Block getBlock(net.minecraft.nbt.CompoundTag aNBT, String aKey) {
		if (aNBT == null) return NB;
		String tName = aNBT.getStringOr(aKey + REG_SUFFIX, "");
		if (!tName.isEmpty()) {
			Identifier tID = Identifier.tryParse(tName);
			if (tID != null && BuiltInRegistries.BLOCK.containsKey(tID)) return BuiltInRegistries.BLOCK.getValue(tID);
		}
		return BuiltInRegistries.BLOCK.byId(aNBT.getIntOr(aKey, 0));
	}

	/** Full registry name of a block ("namespace:path"); empty string when it has none. */
	public static String blockRegName(Block aBlock) {
		if (aBlock == null || aBlock == NB) return "";
		Identifier tID = BuiltInRegistries.BLOCK.getKey(aBlock);
		return tID == null ? "" : tID.toString();
	}

	/** Block by its full registry name; AIR-block constant for an empty or unknown name. */
	public static Block blockByRegName(String aRegName) {
		if (aRegName == null || aRegName.isEmpty()) return NB;
		Identifier tID = Identifier.tryParse(aRegName);
		return tID != null && BuiltInRegistries.BLOCK.containsKey(tID) ? BuiltInRegistries.BLOCK.getValue(tID) : NB;
	}

	/** Writes an ITEM reference held as a runtime registry index (covers, visuals) with its NAME beside it. */
	public static void putItemId(net.minecraft.nbt.CompoundTag aNBT, String aKey, short aID) {
		if (aNBT == null) return;
		aNBT.putShort(aKey, aID);
		Item tItem = item_((long)aID);
		if (tItem != null) {
			Identifier tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID != null) aNBT.putString(aKey + REG_SUFFIX, tID.toString());
		}
	}

	/** Registry index for an item reference written by {@link #putItemId}: recomputed from the NAME when present. */
	public static short getItemId(net.minecraft.nbt.CompoundTag aNBT, String aKey) {
		if (aNBT == null) return 0;
		Item tItem = itemByRegName(aNBT.getStringOr(aKey + REG_SUFFIX, ""));
		return tItem == null ? aNBT.getShortOr(aKey, (short)0) : id_(tItem);
	}

	/** Copies a block reference (name and legacy index) between two NBT tags. */
	public static void copyBlock(net.minecraft.nbt.CompoundTag aFrom, net.minecraft.nbt.CompoundTag aTo, String aKey) {
		if (aFrom == null || aTo == null) return;
		aTo.putInt(aKey, aFrom.getIntOr(aKey, 0));
		String tName = aFrom.getStringOr(aKey + REG_SUFFIX, "");
		if (!tName.isEmpty()) aTo.putString(aKey + REG_SUFFIX, tName);
	}

	/** Item by its full registry name ("namespace:path"); null for an empty or unknown name. */
	public static Item itemByRegName(String aRegName) {
		if (aRegName == null || aRegName.isEmpty()) return null;
		int tColon = aRegName.indexOf(':');
		return tColon <= 0 ? null : findItem(aRegName.substring(0, tColon), aRegName.substring(tColon + 1));
	}

	public static Item findItem(String aModID, String aName) {
		if (aModID == null || aName == null) return null;
		// AE2 mission stage 0: used to be fromNamespaceAndPath — it THROWS IdentifierException on a path invalid
		// under 26.1 (Identifier.java:268 assertValidPath; uppercase letters are forbidden). Item names arrive here
		// as 1.7.10 wrote them ("item.ItemMultiMaterial", "tile.BlockQuartzGlass" — legal in 1.7.10), and the very
		// first such name used to bring down the WHOLE calling loader (LoaderItemList, LoaderBookList,
		// Loader_Recipes_Replace — their try/catch wraps the whole run()). While no MD.X.mLoaded was ever T,
		// ST.make filtered this out earlier (ST.java:621) and the seam stayed invisible; wiring up AE2 exposed it.
		// tryBuild is the same constructor but @Nullable instead of throwing (Identifier.java:57) => an unknown name
		// again yields null, exactly like GameRegistry.findItem in 1.7.10.
		Identifier tID = Identifier.tryBuild(aModID, aName);
		if (tID == null) return null;
		return BuiltInRegistries.ITEM.containsKey(tID) ? BuiltInRegistries.ITEM.getValue(tID) : null;
	}
	/** F12/R7: the SINGLE point for "item by (modId,name) -> ItemStack of size aSize" (used to be an invented
	 *  {@code DeferredRegister.findItemStack(...)}). null when the item is not registered — as in the original. */
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
		// F-item-final CENTER: 1.7.10 aSetStack.func_150996_a(Item) — an in-place Item swap on an existing stack. neo's
		// ItemStack.item = private final Holder<Item> (ItemStack.java:156) — there is no direct setter, and transmuteCopy
		// gives a COPY (not 1:1: the whole of GT6 relies on in-place mutation by reference). The only 1:1 path is
		// reflection on the final field through the UT.Reflection.setField center (a safe try/catch fallback: on
		// failure the item just stays unchanged = the old behavior). The new item's Holder = Item.builtInRegistryHolder()
		// (Item.java:153). NeoForge runtime = official mappings -> the field is named "item".
		UT.Reflection.setField(aSetStack, "item", item_(aToStack).builtInRegistryHolder());
		if (aCheckStacksize) aSetStack.setCount(aToStack.getCount());
		meta_(aSetStack, meta_(aToStack));
		if (aCheckNBT) ItemNBT.set(aSetStack, ItemNBT.get(aToStack));
		return aSetStack;
	}
	/** F-item-final CENTER: 1.7.10 {@code ItemStack.func_150996_a(Item)} — in-place swap of ONLY Item (count/meta/NBT preserved).
	 *  neo's {@code ItemStack.item} = private final Holder<Item> — reflection on the final field through the {@link UT.Reflection#setField}
	 *  center (a safe try/catch fallback). The item's Holder = {@code Item.builtInRegistryHolder()}. Differs from {@link #set}: that one copies everything. */
	public static ItemStack setItem(ItemStack aStack, Item aItem) {
		if (valid(aStack) && aItem != null) UT.Reflection.setField(aStack, "item", aItem.builtInRegistryHolder());
		return aStack;
	}
	/** F12-vanilla-durability CENTER: 1.7.10 {@code Item.setMaxDamage(int)} mutated durability (the GT config
	 *  SmallerVanillaToolDurability trims vanilla tools). neo: {@code MAX_DAMAGE} is a DataComponent on
	 *  {@code builtInRegistryHolder().components()} (Holder.java:133). The public {@code Holder.Reference.bindComponents}
	 *  (Holder.java:262) — NO reflection needed: rebuild the component map with the new MAX_DAMAGE. GT6 item durability = ItemBase.mMaxDamage (F12). */
	public static void setMaxDamage(Item aItem, int aMaxDamage) {
		if (aItem == null) return;
		net.minecraft.core.Holder.Reference<Item> tHolder = aItem.builtInRegistryHolder();
		tHolder.bindComponents(net.minecraft.core.component.DataComponentMap.builder().addAll(tHolder.components()).set(net.minecraft.core.component.DataComponents.MAX_DAMAGE, aMaxDamage).build());
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
		aStack.setCount((int)(aStack.getCount()-(aAmount))); // aAmount is long -> explicit cast (setCount(int))
		if (!(aPlayer instanceof Player)) return T;
		if (aStack.getCount() <= 0) {
			if (aTriggerEvent) EventHooks.onPlayerDestroyItem((Player)aPlayer, aStack, null); // neo: +@Nullable InteractionHand (EventHooks:235); a generic decr without hand context -> null
			if (aRemove) for (int i = 0; i < ((Player)aPlayer).getInventory().getNonEquipmentItems().size(); i++) {
				if (((Player)aPlayer).getInventory().getNonEquipmentItems().get(i) == aStack) {
					((Player)aPlayer).getInventory().getNonEquipmentItems().set(i, ItemStack.EMPTY); // the vanilla NonNullList boundary requires non-null (was null=1.7.10 empty)
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
	
	// F1-meta: neo's ItemStack(ItemLike,int,int) 3-arg ctor with a meta was removed (flattening) — meta is stored
	// in the DAMAGE component (the same F1 model as the meta_:188 setDamageValue center). 2-arg ctor + setDamageValue.
	// F4-flatten (DEFERRED-LEDGER "block-flatten"): for a VANILLA thing, 1.7.10's subtype lived in the meta (dye:1 = red
	// dye, stained_glass:14 = red glass), while the 1.13 engine split families into separate items. A vanilla thing
	// does not read the SUBTYPE component -> without a resolve, the output is always the FIRST family member with a
	// dead meta (evidence from a dump: a bath "glass+red dye" -> white_stained_glass:14). The map center is
	// CS.Flattened (rationale and boundaries live there); here is the single substitution point for things in a
	// stack, so all 477 caller sites are fixed at once and stay verbatim-1:1. Meta is NOT set on the variant: it is
	// already expressed by the item itself. Non-family things (GT meta items, tools with durability, wildcard W) go the old way.
	public static ItemStack make_(Item  aItem , long aSize, long aMeta) {Item  tFlat = CS.Flattened.item (aItem , aMeta); if (tFlat != null) return new ItemStack(tFlat, UT.Code.bindInt(aSize)); ItemStack tPotion = legacyPotion(aItem, aSize, aMeta); if (tPotion != null) return tPotion; ItemStack rStack = new ItemStack(aItem , UT.Code.bindInt(aSize)); meta_(rStack, UT.Code.bindShort(aMeta)); return rStack;}

	// BUG-118 §2, META MODEL: the bridge "1.7.10 legacy potion meta -> neo PotionContents" — the same bridge family
	// as CS.Flattened above, except in neo the potion's kind is expressed by a COMPONENT, not a separate item
	// (splash — by an item: SPLASH_POTION). In 1.7.10 the kind was encoded in the meta (low 4 bits — type per
	// PotionHelper, 0x20 strong, 0x40 extended, 0x2000 drinkable, 0x4000 splash); without the bridge a stack from
	// ST.make was a faceless "Uncraftable Potion" — no color, no name, no effect when drunk the vanilla way (all 96
	// potion containers of Loader_Fluids). The SUBTYPE meta is kept as before — stack identity in maps and recipes
	// is unchanged. A combination with no neo variant degrades to the base kind (strong fire resistance etc. was
	// never registered in 1.7.10 either); an unknown meta -> null = the old componentless path, we invent nothing.
	private static ItemStack legacyPotion(Item aItem, long aSize, long aMeta) {
		if (aItem != Items.POTION && aItem != Items.SPLASH_POTION && aItem != Items.LINGERING_POTION) return null;
		net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> tKind = legacyPotionKind(aMeta);
		if (tKind == null) return null;
		// lingering is already a separate ITEM on input (the EtFu-branch container is paved over onto vanilla, Loader_Fluids:344);
		// the splash meta bit does not change the item — lingering never had it in its metas
		ItemStack rStack = new ItemStack(aItem == Items.POTION && (aMeta & 0x4000) != 0 ? Items.SPLASH_POTION : aItem, UT.Code.bindInt(aSize));
		rStack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(tKind));
		meta_(rStack, UT.Code.bindShort(aMeta));
		return rStack;
	}
	private static net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> legacyPotionKind(long aMeta) {
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
		// low 4 bits empty: base brews (16 awkward, 32 thick, 64/8192 mundane) and water (meta 0 = 1.7.10 water bottle)
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
	// AE2 mission stage 2 (compat layer): the SINGLE point where a 1.7.10 item name turns into a stack —
	// ALL foreign-mod addressing paths converge here (the ST.block/ST.item ModData variants, OM.data,
	// OreDictManager.setTarget, ItemStackMap.put, ItemStackSet.add). AE2 26.1 renamed its items entirely
	// (the rv2 meta subtypes became separate ids), so the "name+meta" pair is resolved by the
	// gregapi.compat.AE2Names center table; it only knows AE2 names — for every other mod the path is unchanged, verbatim.
	// Same technique and same spot as the vanilla meta expansion (CS.Flattened in ST.make_ a few lines below).
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
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: a vanilla Container returns EMPTY, the 1.7.10 body reasons in null
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			for (int aSlotTo : aSlotsTo) {
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: see above
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
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: a vanilla Container returns EMPTY, the 1.7.10 body reasons in null
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			for (int aSlotTo : aSlotsTo) {
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: see above
				int tMovable = Math.min(aMaxMove, canPut((Container)aTo.mTileEntity, aIgnoreSideTo ? SIDE_ANY : aTo.mSideOfTileEntity, aTo.mSideOfTileEntity, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSize, aStackFrom.getMaxStackSize())));
				if (tMovable < aMinMove || tMovable + (aStackTo == null ? 0 : aStackTo.getCount()) < aMinSize) continue;
				// Actually Moving the Stack
				rMoved += move_((Container)aFrom.mTileEntity, (Container)aTo.mTileEntity, aStackFrom, aStackTo, aSlotFrom, aSlotTo, tMovable);
				aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: a vanilla Container returns EMPTY, the 1.7.10 body reasons in null
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
		
		ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: a vanilla Container returns EMPTY, the 1.7.10 body reasons in null
		if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) return 0;
		for (int aSlotTo : aSlotsTo) {
			ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: see above
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
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: a vanilla Container returns EMPTY, the 1.7.10 body reasons in null
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: see above
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
				ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: a vanilla Container returns EMPTY, the 1.7.10 body reasons in null
				if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) return 0;
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: see above
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
		// F15-size0 (BUG-011, an item used to vanish): neo's removeItem is a split of the SAME object — aStackFrom (a
		// getItem snapshot) gets zeroed out, a count-0 stack = EMPTY, amount/copy from it = air. In 1.7.10 decrStackSize
		// left the Item on a size-0 stack and copying worked. Copy source = tStack (what removeItem actually took, same item/NBT).
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
		// F15-size0 (BUG-011): see move_(Container,...) above — the copy comes from tStack, not from the zeroed-out aStackFrom.
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
		} else if (WD.getMaterial(aBlock) == gregapi.block.Material.lava /* F9 (1:1): do not eject items onto lava; WD.getMaterial is implemented (the stub tag was removed) */ || aBlock instanceof FireBlock || (invalid(aBlock) && aTo.mY < 1)) {
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
	
	/**
	 * The SINGLE POINT of the polymorphic "container-item" channel (BUG-022, nailed down 2026-07-26).
	 *
	 * <p>In 1.7.10 {@code hasContainerItem/getContainerItem} were methods of {@code Item} ITSELF, so
	 * {@code item_(aStack).hasContainerItem(aStack)} could ask any GT6 item. In neo {@code Item} has no such
	 * methods, and the GT6 implementations live in FIVE unrelated roots with no common ancestor:
	 * {@code ItemBase} (and its MultiItem*), {@code PrefixItem}, {@code ItemFluidDisplay},
	 * {@code MultiTileEntityItemInternal}, {@code PrefixBlockItem}. The earlier fix asked only
	 * {@code ItemBase} — the other four roots lost the channel.</p>
	 *
	 * <p><b>Evidence (F4 step 3).</b> The parity judge: the port has 1479 smelting recipes for chemical vials
	 * against ONE in the reference (+ the same number in the melter = 2958 entries, 39% of all recipe
	 * discrepancies). The vial is a {@code PrefixItem}, its container (an empty vial) was never asked, so a
	 * filled vial was treated as an ordinary ingredient and fell into the general smelting generator
	 * ({@code Loader_OreProcessing}). Live diagnostics confirmed: at event time the prefix container is
	 * ALREADY set — so the issue was not timing, but that nobody asked it.</p>
	 *
	 * <p><b>Public.</b> The crafting table asks the same question: the crafting remainder is returned by the
	 * bench dispatcher {@code GT6CraftingDispatcher.getRemainingItems} — it used to ask only {@code ItemBase}
	 * (a private {@code instanceof}) and lost the same four roots; we do not set up a second root scan —
	 * the dispatcher asks THIS point.</p>
	 */
	public static ItemStack gtContainerItem(ItemStack aStack) {
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
		// F5/BUG-045 (1:1): a fluid container (empty too) is not an ingredient; a restored IFluidContainerItem
		// (compat-mirror; original :841). Filled registry containers are caught by the channels below, as in the original.
		if (item_(aStack) instanceof IFluidContainerItem tICI && tICI.getCapacity(aStack) > 0) return F;
		// BUG-022 v2: symmetric to ST.container — 1.7.10 called the polymorphic hasContainerItem (GT6 bottles/cans/prefix),
		// the component channel below covers only vanilla. Ask ALL GT roots through the single point above.
		if (gtContainerItem(aStack) != null) return F;
		if (item_(aStack).getCraftingRemainder(aStack) != null) return F;
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
		// BUG-022 v2 (Greg's crafting table): in 1.7.10 there was a POLYMORPHIC Item channel hasContainerItem/getContainerItem —
		// GT6 tools (MultiItemTool:579) returned a copy with added wear; the port collapsed this to the component
		// getCraftingRemainder (GT6 items have none) -> the tool ingredient DISAPPEARED in every caller of ST.container
		// (including MultiTileEntityAdvancedCraftingTable.consumeSlot:436). The live ItemBase channel is restored FIRST,
		// the component one (vanilla bucket etc.) follows, matching the original's 1:1 order.
		ItemStack tGTContainer = gtContainerItem(aStack); // all GT roots at once (see gtContainerItem)
		if (tGTContainer != null) return copy(tGTContainer);
		if (item_(aStack).getCraftingRemainder(aStack) != null) return copy(item_(aStack).getCraftingRemainder(aStack).create());
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
		// F5/BUG-045 (1:1): the empty container after draining — a restored IFluidContainerItem (compat-mirror;
		// original :868-876 — drain to empty, guard count<=0, clear empty NBT).
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
		return aStack.has(DataComponents.FOOD); // used to be: item_(aStack) instanceof ItemFood — the class was removed in neo (REMAP-RULES §C-bis)
	}
	/** F-armor-detection: the 1.7.10 class ItemArmor was removed -> armor is determined by the DataComponents.EQUIPPABLE
	 *  component (Equippable.slot(), EquipmentSlot.isArmor()=HUMANOID_ARMOR|ANIMAL_ARMOR, EquipmentSlot.java:73). The item-domain center. */
	public static boolean armor(ItemStack aStack) {
		if (invalid(aStack)) return F;
		net.minecraft.world.item.equipment.Equippable tEquippable = aStack.get(DataComponents.EQUIPPABLE);
		return tEquippable != null && tEquippable.slot().isArmor();
	}

	public static int food(ItemStack aStack) {
		if (invalid(aStack)) return 0;
		if (aStack.has(DataComponents.FOOD)) {try {return aStack.get(DataComponents.FOOD).nutrition();} catch(Throwable e) {return 1;}}
		if (item_(aStack) instanceof MultiItemRandom) {
			IFoodStat tStat = ((MultiItemRandom)item_(aStack)).mFoodStats.get(meta_(aStack));
			return tStat == null ? 0 : tStat.getFoodLevel(item_(aStack), aStack, null);
		}
		return 0;
	}
	public static float saturation(ItemStack aStack) {
		if (invalid(aStack)) return 0;
		if (aStack.has(DataComponents.FOOD)) {try {return aStack.get(DataComponents.FOOD).saturation();} catch(Throwable e) {return 0.5F;}}
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
		// F#/fuel-registry: 1.7.10's GameRegistry.getFuelValue(ItemStack) was a static registry with no world
		// context. In neo the fuel value comes from FuelValues, which lives on MinecraftServer/Level, not a static
		// registry (neo-decompiled/net/minecraft/server/MinecraftServer.java:2302 fuelValues(),
		// net/minecraft/server/level/ServerLevel.java:1853). The fuel(ItemStack) signature without Level is unchanged —
		// we fetch the current server centrally via ServerLifecycleHooks.getCurrentServer() (neoforge-decompiled/
		// net/neoforged/neoforge/server/ServerLifecycleHooks.java:130); both callers (RecipeMapFurnaceFuel:53,
		// RecipeMapMicrowave:96) already gate the call behind GAPI_POST.mFinishedServerStarted>0, so by this point
		// the server is always up and fuelValues() is populated (MinecraftServer.java:357).
		// aStack.getBurnTime(RecipeType,FuelValues) is the real neo path (neoforge-decompiled/net/neoforged/
		// neoforge/common/extensions/IItemStackExtension.java:62, ItemStack implements it — ItemStack.java:103),
		// and goes through FurnaceFuelBurnTimeEvent — the same role of "mods register custom fuel" that
		// 1.7.10's GameRegistry.getFuelValue played.
		net.minecraft.server.MinecraftServer tFuelServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
		long rFuelValue = tFuelServer == null ? 0 : aStack.getBurnTime(null, tFuelServer.fuelValues());
		if (rFuelValue > 0) return rFuelValue;
		Item tItem = item_(aStack);
		// F9 redundant (already covered 1:1 above): 1.7.10 gave 200 fuel to wooden vanilla tools via instanceof
		// ItemTool/ItemSword/ItemHoe (the classes were removed — a tool is now Item+components). In neo this is
		// REDUNDANT: getBurnTime (above, ~line 1090) reads the data-driven fuel registry, where vanilla wooden tools
		// already carry burn-time 200 -> return rFuelValue would have fired already. A separate instanceof check is unneeded.
		if (tItem == Items.STICK) return 100;
		if (CS.Flattened.headItemOf(tItem) == Items.COAL) return 1600;
		if (tItem == Items.BLAZE_ROD) return 2400;
		if (tItem == Items.LAVA_BUCKET) return 20000;
		Block tBlock = block_(tItem);
		// 1.7.10's `Blocks.sapling`/`wooden_slab` were ONE block with a wood-type meta, i.e. burn time applied to ANY
		// wood type. In neo the families are split into per-type blocks; comparing against oak alone would leave
		// spruce/birch/jungle/acacia/dark oak without a burn time. We take the family trait from vanilla tags — the
		// same technique the port already uses to detect wood families in WD (`BlockTags.LEAVES`/`SAPLINGS`/`WOODEN_SLABS`, WD.java:620-640).
		if (tBlock != null && tBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.SAPLINGS)) return 100;
		if (tBlock != null && tBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.WOODEN_SLABS)) return 150;
		if (tBlock == Blocks.COAL_BLOCK) return 16000;
		// F9: 1.7.10's WD.getMaterial(tBlock)==Material.wood -> 300. neo removed Material — the "wood" equivalent is SoundType.WOOD
		// (the same technique as the WD block-sound center). tBlock is valid (no need to check block_(tItem)!=NB: NB.defaultBlockState is air, sound!=WOOD).
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
		try {if (UT.Code.stringValid(rName = aStack.getItem().getDescriptionId())) return rName.toString();} catch (Throwable e) {/*Do nothing*/} // getUnlocalizedName()->Item.getDescriptionId() (Item.java:350; ItemStack has none, the key lives on Item)
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
	
	// BUG-010: 1.7.10's ST.hide hid an item from NEI (the codechicken.nei API — a dead channel in neo, the catch
	// swallowed the call silently) -> 5 hidden slab variants of EVERY meta-type family (BlockMetaType: UP/N/S/W/E)
	// became visible in creative/JEI (measurement gt6slabprobe: 168 slab items, 140 extra). Centralized channel
	// replacement: a hidden-item registry here, respected by CreativeTabsGT (populate + onBuildContents — both tab-fill
	// paths; JEI builds its list from there). HIDDEN_BLOCKS is separate: hide(Block) is called while the block is
	// being CONSTRUCTED (RegisterEvent<Block>), when the BlockItem is not yet registered (make(block,...) would give
	// air) — we hide the Block itself, and hidden() checks it via BlockItem.getBlock.
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
	
	// F16 WIRED UP (1:1 golden forceProperMaxStacksizes): GT6's vanilla-item stack-size change (setMaxStackSize —
	// a runtime mutator, removed; the stack limit = DataComponents.MAX_STACK_SIZE at registration) is implemented in
	// applyVanillaComponentOverrides (above) via ModifyDefaultComponentsEvent: potion->1; glass_bottle/cake/stick/
	// written_book/writable_book/enchanted_book/snowball/egg->64; all beds->64; all doors->8. Not a stub.
	public static boolean forceProperMaxStacksizes() {
		return T;
	}
	
	public static final Collection<ItemStack> REVERT_TO_BOOK_TO_FIX_STUPID = arraylist();
	public static void fixBookStacks() {for (ItemStack tStack : REVERT_TO_BOOK_TO_FIX_STUPID) set(tStack, make(Items.BOOK, 1, 0), T, T);}
	
	public static final List<String>
	LOOT_TABLES         = new ArrayListNoNulls<>(F, "dungeonChest", "villageBlacksmith", "mineshaftCorridor", "strongholdLibrary", "strongholdCrossing", "strongholdCorridor", "pyramidDesertyChest", "pyramidJungleChest", "pyramidJungleDispenser", "bonusChest"),
	LOOT_TABLES_VANILLA = new ArrayListNoNulls<>(F, "dungeonChest", "villageBlacksmith", "mineshaftCorridor", "strongholdLibrary", "strongholdCrossing", "strongholdCorridor", "pyramidDesertyChest", "pyramidJungleChest", "pyramidJungleDispenser", "bonusChest");
	
	// stats-loot: 1.7.10 ChestGenHooks.getOneItem(random-vanilla-table, RNGSUS). BUG-039: the original's shape is
	// restored verbatim — the server-side sample lives ONLY in the shim-ChestGenHooks center (the merged
	// vanilla+GT pool table; null outside a server — as the original was outside a world). The local duplicate of the sample logic was removed.
	private static final java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> VANILLA_LOOT_KEYS = java.util.Arrays.asList(
		net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON, net.minecraft.world.level.storage.loot.BuiltInLootTables.VILLAGE_WEAPONSMITH,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.ABANDONED_MINESHAFT, net.minecraft.world.level.storage.loot.BuiltInLootTables.STRONGHOLD_LIBRARY,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.STRONGHOLD_CROSSING, net.minecraft.world.level.storage.loot.BuiltInLootTables.STRONGHOLD_CORRIDOR,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.DESERT_PYRAMID, net.minecraft.world.level.storage.loot.BuiltInLootTables.JUNGLE_TEMPLE,
		net.minecraft.world.level.storage.loot.BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER, net.minecraft.world.level.storage.loot.BuiltInLootTables.SPAWN_BONUS_CHEST);
	public static ItemStack generateOneVanillaLoot() {
		return net.minecraftforge.common.ChestGenHooks.getOneItem(UT.Code.select("dungeonChest", LOOT_TABLES_VANILLA), RNGSUS);
	}

	public static boolean generateLoot(Random aRandom, String aLoot, Container aInv) {
		try {
			if (aLoot.startsWith("twilightforest:")) {
				if (!TF_TREASURE) return F;
				// Twilight Forest is absent from this build: the TF_TREASURE gate (ST.java:81 = F, only set when
				// the mod is loaded, :95) makes this branch unreachable. The same AbstractContainerMenu/Container seam
				// lives in gregtech.worldgen.TwilightTreasureReplacer; here it is bridged via a checked cast.
				TwilightTreasureReplacer.generate((net.minecraft.world.Container)aInv, aLoot);
			} else if (!LOOT_TABLES_VANILLA.contains(aLoot)) {
				// BUG-039 (F-loot): GT6 categories (gt.gems/gt.misc/...) — their content lives entirely in the
				// shim-ChestGenHooks buffer (net.minecraftforge.common; filled by Loader_Loot). This line is 1:1 with the 1.7.10 original.
				net.minecraftforge.common.WeightedRandomChestContent.generateChestContents(aRandom, net.minecraftforge.common.ChestGenHooks.getItems(aLoot, aRandom), aInv, net.minecraftforge.common.ChestGenHooks.getCount(aLoot, aRandom));
			} else {
				// F-loot: 1.7.10 vanilla table names -> an index into LOOT_TABLES_VANILLA -> VANILLA_LOOT_KEYS (1:1 order,
				// see generateOneVanillaLoot) -> a weighted table roll (LootParams(CHEST)) — the vanilla part is
				// data-driven; GT additions are already injected into the table by the gregtech6:<category> pool (shim-ChestGenHooks).
				// Defaults to SIMPLE_DUNGEON for an unknown name.
				net.minecraft.server.MinecraftServer tServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
				if (tServer != null) {
					net.minecraft.server.level.ServerLevel tLevel = tServer.overworld();
					if (tLevel != null) {
						int tIndex = LOOT_TABLES_VANILLA.indexOf(aLoot);
						net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> tKey = (tIndex >= 0 && tIndex < VANILLA_LOOT_KEYS.size()) ? VANILLA_LOOT_KEYS.get(tIndex) : net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON;
						net.minecraft.world.level.storage.loot.LootTable tTable = tServer.reloadableRegistries().getLootTable(tKey);
						net.minecraft.world.level.storage.loot.LootParams tParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(tLevel)
							.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
							.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
						// BUG-105 §1 (log: "Tried to over-fill a container" x37 in one generation): the AMOUNT of loot in
						// 1.7.10 was set by the CATEGORY COUNTER, not the table — generateChestContents ran exactly
						// getCount(category) iterations and placed each item into a RANDOM slot, overwriting an occupied
						// one (WeightedRandomChestContent:39-51). Overflow cannot happen there by construction. The port
						// used to call LootTable.fill, leaving the amount up to the neo table: measurement gt6lootprobe —
						// simple_dungeon asks for 21 stacks on average against 1.7.10's count=8, and a bookshelf
						// (DummyInventory(14), MultiTileEntityBookShelf:107) overflowed almost every time, with the
						// engine SILENTLY discarding the excess. We restore the 1.7.10 layout: the table itself (vanilla
						// + the gregtech6:<category> pool) provides the single weighted list, while how many items to take
						// from it and where to put them is decided by the counter, as before.
						java.util.List<ItemStack> tPool = tTable.getRandomItems(tParams);
						for (int tRoll = 0, tCount = net.minecraftforge.common.ChestGenHooks.getCount(aLoot, aRandom); tRoll < tCount && !tPool.isEmpty(); tRoll++) {
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
				// F8 stale enchant holders in GT loot templates (BUG-002 class) — re-resolve against the current server
				// (a chest-encode crash "Can't find id for sharpness" when switching worlds mid-session; see UT.NBT.freshenEnchantments).
				UT.NBT.freshenEnchantments(tStack);
				if (IL.TC_Gold_Coin.exists()) {
					if (item_(tStack) == Items.GOLD_NUGGET) {
						set(tStack, IL.TC_Gold_Coin.get(tStack.getCount()));
					}
					if (item_(tStack) == Items.GOLD_INGOT && tStack.getCount() <= 7) {
						set(tStack, IL.TC_Gold_Coin.get(tStack.getCount() * 9L));
					}
				}
				// The EtFu mod is absent from this build: the whole branch, gated by IL.EtFu_Sus_Stew.exists()
				// (LoaderItemList.java:1444), is unreachable. 1.7.10 Potion effects as a raw NBT list ("EffectId"/
				// "EffectDuration" with a numeric .id) — in neo effects are registry objects (MobEffects.<NAME>,
				// Holder<MobEffect>, no .id), and the on-stack effect storage format is DataComponents (not an arbitrary "Effects" ListTag). Original:
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
			
			for (int i = 0; i < 36; i++) if (!(aPlayer instanceof Player) || i != ((Player)aPlayer).getInventory().getSelectedSlot()) {
				ItemStack tStack = aInv.getItem(i);
				if (equal(tStack, aStack) && aStack.getCount() + tStack.getCount() <= tStack.getMaxStackSize()) {
					tStack.setCount(tStack.getCount()+(aStack.getCount()));
					update(aPlayer);
					return T;
				}
			}
			if (aCurrentSlotFirst && aPlayer instanceof Player) {
				ItemStack tStack = aInv.getItem(((Player)aPlayer).getInventory().getSelectedSlot());
				if (tStack == null || tStack.getCount() == 0) {
					aInv.setItem(((Player)aPlayer).getInventory().getSelectedSlot(), aStack);
					update(aPlayer);
					return T;
				} else if (equal(tStack, aStack) && aStack.getCount() + tStack.getCount() <= tStack.getMaxStackSize()) {
					tStack.setCount(tStack.getCount()+(aStack.getCount()));
					update(aPlayer);
					return T;
				}
			}
			for (int i = 0; i < 36; i++) if (!(aPlayer instanceof Player) || i != ((Player)aPlayer).getInventory().getSelectedSlot()) {
				ItemStack tStack = aInv.getItem(i);
				if (tStack == null || tStack.getCount() <= 0) {
					aInv.setItem(i, aStack);
					update(aPlayer);
					return T;
				}
			}
			if (!aCurrentSlotFirst && aPlayer instanceof Player) {
				ItemStack tStack = aInv.getItem(((Player)aPlayer).getInventory().getSelectedSlot());
				if (tStack == null || tStack.getCount() == 0) {
					aInv.setItem(((Player)aPlayer).getInventory().getSelectedSlot(), aStack);
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
		// FORCED-ADAPTATION(F18-achievements): the original = achieve(parentAchievement)+triggerAchievement (a stat flag + a local toast).
		// Neo removed Achievement/AchievementList/triggerAchievement; the only API, PlayerAdvancements.award(), forces
		// recipe unlocks+xp+chat announcement+an event (neo PlayerAdvancements.java:177-182) — added behavior, which breaks
		// verbatim 1:1 (principle 6, PORTING-LAW). Decision: a centralized no-op. Full proof + an old->RL table
		// (2 achievements with no equivalent: mineWood/killCow) — decisions/F18-achievements.md. Zero parity weight.
		return T;
	}
	
	// FORCED-ADAPTATION(F18-achievements): net.minecraft.stats.AchievementList/Achievement/triggerAchievement were removed in neo
	// (data-driven Advancement/PlayerAdvancements). Granting via award() forces recipes/xp/chat/an event — not 1:1;
	// mineWood/killCow have no equivalent. Decision (proof + an RL table): decisions/F18-achievements.md.
	// The vanilla achieve(aPlayer, AchievementList.X) calls below are a centralized no-op; the original constant is kept for the trace.
	public static boolean check(Entity aPlayer, ItemStack aStack) {
		if (!(aPlayer instanceof Player) || aPlayer.level() == null || aPlayer.level().isClientSide()) return F;

		if (F /* F18-redundant: used to gate ONLY the vanilla "portal" achievement (achieve below), which neo now auto-grants as an advancement.
		     neo does have a dimension check (aPlayer.level().dimension()==Level.NETHER), but here it would gate a no-op -> leave it F */) {
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

		// FORCED-ADAPTATION(F18, like its neighbors below): the whole block only manually granted the vanilla
		// buildHoe/buildSword/buildPickaxe achievements by instanceof-checking the tool type. In neo achievements are
		// advancements, which the engine auto-grants on receiving the item -> the manual grant is REDUNDANT (and the
		// ItemHoe/ItemSword/ItemPickaxe classes are gone anyway). Original (a no-op in neo): achieve(buildHoe) for ItemHoe / buildSword / buildBetter?Pickaxe.

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
			if (tStack != null && (tStack.getCount() == 0 || tStack.getItem() == null)) aInv.setItem(i, ItemStack.EMPTY); // F15: neo's Inventory=NonNullList, setItem(i,null) throws an NPE (was null — a legal slot clear in 1.7.10)
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
		return aNBT == null ? null : load(aNBT.getCompoundOrEmpty(aTagName), NI);
	}
	/** Loads an ItemStack properly. */
	public static ItemStack load(CompoundTag aNBT, String aTagName, ItemStack aDefault) {
		return aNBT == null ? null : load(aNBT.getCompoundOrEmpty(aTagName), aDefault);
	}
	
	/** Loads an ItemStack properly. */
	public static ItemStack load(CompoundTag aNBT) {
		return load(aNBT, NI);
	}
	/** Loads an ItemStack properly. */
	public static ItemStack load(CompoundTag aNBT, ItemStack aDefault) {
		if (aNBT == null || aNBT.isEmpty()) return null;
		// BUG-077: Count<=0 is "a zero with type memory" (1.7.10 wrote stackSize=0). A stack with zero cannot be
		// built in neo (it would become EMPTY, identity lost), so we build it on 1 and mark it with a ZEROSIZE ghost
		// via the size_ center — exactly the representation GT6 code uses at runtime (ST.count will give 0).
		int tCount = aNBT.getIntOr("Count", 0);
		// The numeric item id is a registry INDEX on this engine: adding any mod shifts it and every saved
		// stack would come back as a different item, so the registry NAME decides whenever it is present.
		Item tItem = itemByRegName(aNBT.getStringOr("reg", ""));
		ItemStack rStack = tItem == null ? null : make(tItem, tCount <= 0 ? 1 : tCount, aNBT.getShortOr("Damage",(short)0));
		// Records written before the name existed carry only the shifting index, so the ore dictionary
		// name — saved next to it and describing the same unified stack — is trusted ahead of that index.
		if (rStack == null && aNBT.contains("od")) rStack = OreDictManager.INSTANCE.getStack(aNBT.getStringOr("od",""), tCount <= 0 ? 1 : tCount);
		if (rStack == null) rStack = make(Item.byId(aNBT.getShortOr("id",(short)0)), tCount <= 0 ? 1 : tCount, aNBT.getShortOr("Damage",(short)0));
		if (rStack == null) return aDefault == null ? null : update_(OM.get_(aDefault));
		if (tCount <= 0) size_(0, rStack);
		// Has to use setTagCompound instead of putting it into make()
		// because it would delete certain Tags on load, making stuff like unscanned Forestry Bees unstackable.
		// But update_() will still delete a completely empty NBT later on.
		ItemNBT.set(rStack, aNBT.getCompound("tag").orElse(null));
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
	/** used to be {@code aStack.writeToNBT(aNBT)} (1.7.10 ItemStack — writes the stack's fields INTO the passed tag, returns it) —
	 *  neo: a component model, reproduced via save(aStack)+merge (keys id/Count/Damage 1:1). */
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
		// BUG-077 (infinite item duplicator): write the LOGICAL size, not the physical count.
		// "A zero with type memory" (mass storage after emptying, catalysts) is stored in neo as a ZEROSIZE ghost —
		// count=1 + a component marker, because neo cannot hold count<=0. This record's format is 1:1 with 1.7.10
		// (id/Count/Damage/tag/od) and knows NOTHING about components: the marker never made it into the NBT, and
		// Count went out as 1. After re-entering the world the ghost came back to life as a real item — an item out
		// of nothing. The logical count (ST.count) returns 0 exactly where 1.7.10 wrote stackSize=0; ST.load reassembles the ghost on the way back.
		// The zero is written EXPLICITLY (putInt): UT.NBT.setNumber drops zero values, and "a zero with type memory"
		// would then rely on the convention "no key = 0". The convention is correct but implicit — when read by a
		// foreign parser, a missing Count reads as 1. We write 0 literally, as 1.7.10 did.
		int tLogicalCount = count(aStack);
		if (tLogicalCount <= 0) rNBT.putInt("Count", 0); else UT.NBT.setNumber(rNBT, "Count", tLogicalCount);
		UT.NBT.setNumber(rNBT, "Damage", meta_(aStack));
		if (ItemNBT.has(aStack)) rNBT.put("tag", ItemNBT.get(aStack));
		OreDictItemData tData = OM.anyassociation_(aStack);
		if (tData != null) rNBT.putString("od", tData.toString());
		return rNBT;
	}
}
