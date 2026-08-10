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
	// F4-flatten, джокер семьи: 1.7.10 писал «любой подтип» ОДНИМ стеком с метой W (stained_glass:W = любое
	// цветное стекло). В neo семья — разные Item, поэтому один стек её не покрывает, и сравнение «предмет в
	// предмет» проваливалось: машина принимала только тот вариант, что назван (белый). Понимание «джокер +
	// член той же семьи = совпадение» держит центр карт CS.Flattened; путь дешёвый — включается лишь когда
	// предметы РАЗНЫЕ и у одного из стеков мета равна W (в горячем сравнении это редкость).
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
	public static boolean   valid(ItemStack aStack) {return aStack != null && !aStack.isEmpty() && item_(aStack) != null;} // F15 (BUG-011): зеркало invalid ниже — обнулённый split-объект НЕ «валидный предмет»
	// F15 (BUG-011, «слот руки постоянно занят»): 1.7.10 пустота = null; neo пустота = isEmpty() (count<=0 ИЛИ air),
	// причём обнулённый split-объект (например, рука после переноса стека) — НЕ синглтон ItemStack.EMPTY. Прежняя
	// проверка «== EMPTY || count < 0» считала такой стек ВАЛИДНЫМ -> гейты типа onBlockActivated3 наковальни уходили
	// в ветку «в руке предмет», и забор пустой рукой был недостижим. Size-0-катализаторы НЕ задеты: они физически
	// count=1 + маркер ZEROSIZE (см. F-size0-catalyst выше, ST.size:198).
	public static boolean invalid(ItemStack aStack) {return aStack == null || aStack.isEmpty() || item_(aStack) == null;}
	
	public static ItemStack validate(ItemStack aStack) {return valid(aStack)                         ? aStack : null;}
	public static ItemStack valisize(ItemStack aStack) {return valid(aStack) && aStack.getCount() > 0 ? aStack : null;}

	/**
	 * F15 (мост GT6↔движок, «модель предмета null↔EMPTY»): GT6 хранит пустой слот как {@code null}
	 * (см. {@code CS.NI}, весь {@link ST} построен на null), а движок никогда не терпит null — обязательный
	 * синглтон {@code ItemStack.EMPTY}. Плюс GT6 держит стек с {@code count==0} как ЗНАЧИМОЕ состояние,
	 * отличное от null ({@code TileEntityBase05Inventories.decrStackSizeGUI}: {@code allowZeroStacks} →
	 * {@code setCount(0)} без обнуления в null) — движковый {@code isEmpty()} (true при {@code count<=0})
	 * такое состояние не различает, поэтому граница мостится СТРОГО по ссылочному равенству с синглтоном
	 * {@code ItemStack.EMPTY}, не по {@code isEmpty()}. Это ЕДИНСТВЕННАЯ точка перевода между моделями —
	 * весь мод обязан пересекать границу только через {@link #nn} / {@link #ni}, не ad-hoc тернарниками.
	 */
	public static ItemStack nn(ItemStack aStack) {return aStack == null ? ItemStack.EMPTY : aStack;}
	/** F15-обратная граница: neo отдаёт EMPTY там, где 1.7.10 отдавал null (курсор getCarried, слоты Inventory.getItem).
	 *  GT6-тела 1:1 рассуждают null-семантикой — на входе из движка нормализуем EMPTY→null этим хелпером (пара к {@link #nn}). */
	public static ItemStack n(ItemStack aStack) {return aStack == null || aStack.isEmpty() ? null : aStack;}
	/** F15: движок→GT6. Движковый синглтон {@code ItemStack.EMPTY} (и null) → null (GT6-пусто); ЗНАЧИМЫЙ
	 *  {@code count==0} (стек — не тот же объект, что {@code ItemStack.EMPTY}) переживает границу как есть. */
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
	// F12-followup (subtype-meta): GT6-подтип хранится в компоненте GT_API.SUBTYPE (не в damage-value — тот клампится к
	// [0,maxDamage], у meta-предметов maxDamage=0 → схлопывание в 0). meta-0 = компонент отсутствует (дефолт 0, стек с ванилла).
	public static short     meta_(ItemStack aStack) {return gregapi.GT_API.SUBTYPE.isBound() ? (short)(int)aStack.getOrDefault(gregapi.GT_API.SUBTYPE.get(), 0) : 0;}
	public static ItemStack meta (ItemStack aStack, long aMeta) {return aStack == null ? null : meta_(aStack, aMeta);}
	public static ItemStack meta_(ItemStack aStack, long aMeta) {int tMeta = (short)aMeta; if (gregapi.GT_API.SUBTYPE.isBound()) {if (tMeta != 0) aStack.set(gregapi.GT_API.SUBTYPE.get(), tMeta); else aStack.remove(gregapi.GT_API.SUBTYPE.get());} return aStack;}

	/**
	 * BUG-079, ЕДИНСТВЕННЫЙ ответ на вопрос «что делает предмет ОТДЕЛЬНЫМ предметом для внешней витрины».
	 *
	 * <p>В 1.7.10 личность = {@code item + damage} (мета); NBT в сравнение NEI не входил. В neo компоненты
	 * заявляются явно ({@code registerItemSubtypes}), и часть семей GT6 без NBT схлопывается в дубли —
	 * монеты/батареи/сундуки различаются NBT-материалом. Поэтому решение принадлежит самому предмету:
	 * {@link gregapi.item.multiitem.MultiItem#identityIncludesNBT()} (дефолт «да»,
	 * {@code MultiItemTool} — «нет»: у инструмента NBT это состояние, а не личность).</p>
	 *
	 * <p>Держать это правило у себя не должен НИКТО, кроме этого метода: его спрашивают и заявка JEI
	 * ({@code GT6_JEI_Plugin.registerItemSubtypes}), и судьи паритета ({@code PortDump}, проба
	 * {@code gt6jeicraft}). Копия политики расходится с оригиналом на исключениях — урок BUG-070.</p>
	 */
	public static boolean identityIncludesNBT(net.minecraft.world.item.Item aItem) {
		return !(aItem instanceof gregapi.item.multiitem.MultiItem tMulti) || tMulti.identityIncludesNBT();
	}

	/** Ключ личности стека для внешней витрины: предмет + мета, плюс NBT — но только если он в личности
	 *  ({@link #identityIncludesNBT}). Тем же ключом витрина сопоставляет предмет с выходом рецепта. */
	public static String identityKey(ItemStack aStack) {
		if (aStack == null || aStack.getItem() == null) return "null";
		var tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aStack.getItem());
		String rKey = (tKey == null ? "" : tKey.toString()) + ":" + meta_(aStack);
		if (!identityIncludesNBT(aStack.getItem())) return rKey;
		net.minecraft.nbt.CompoundTag tNBT = gregapi.code.ItemNBT.get(aStack);
		return rKey + "|" + (tNBT == null ? "-" : tNBT.toString());
	}

	// F-size0-catalyst: логический размер. GT6 size-0-стек (катализатор) хранится в neo как count=1 + маркер GT_API.ZEROSIZE
	// (neo не держит count<=0 без превращения в AIR/EMPTY). size() отдаёт 0 для маркированных → recipe-matching/consume/дамп
	// видят логический 0. Совпадает со старым поведением size(AIR-катализатор)=0 → существующие вызыватели не затронуты.
	public static byte      size (ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null || aStack.getCount() < 0 ? 0 : (gregapi.GT_API.ZEROSIZE.isBound() && aStack.has(gregapi.GT_API.ZEROSIZE.get()) ? 0 : UT.Code.bindByte(aStack.getCount()));}
	/** F15-size0 (BUG-015 v2): ЛОГИЧЕСКИЙ count как int, без byte-клампа ST.size — читатель поля 1.7.10 stackSize
	 *  для больших стеков (масстораж держит тысячи). ZEROSIZE-призрак («тип запомнен, штук 0») читается как 0. */
	public static int count(ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || aStack.getCount() < 0 ? 0 : (gregapi.GT_API.ZEROSIZE.isBound() && aStack.has(gregapi.GT_API.ZEROSIZE.get()) ? 0 : aStack.getCount());}
	public static ItemStack size (long aSize, ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : size_(aSize, aStack);}
	// aSize<=0 = GT6-катализатор: держим count=1 (иначе neo → EMPTY/AIR, идентичность теряется) + маркер ZEROSIZE; логический
	// размер читается через ST.size(). aSize>=1 — обычный setCount + снять маркер (если стек переиспользуется).
	public static ItemStack size_(long aSize, ItemStack aStack) {
		if (aSize <= 0) {aStack.setCount(1); if (gregapi.GT_API.ZEROSIZE.isBound()) aStack.set(gregapi.GT_API.ZEROSIZE.get(), net.minecraft.util.Unit.INSTANCE);}
		else {aStack.setCount((int)aSize); if (gregapi.GT_API.ZEROSIZE.isBound() && aStack.has(gregapi.GT_API.ZEROSIZE.get())) aStack.remove(gregapi.GT_API.ZEROSIZE.get());}
		return aStack;
	}
	
	public static byte maxsize(ItemStack aStack) {return (byte)(aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? 64 : item_(aStack).getMaxStackSize(aStack));}
	
	public static ItemStack copy (ItemStack aStack) {return aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null ? null : copy_(aStack);}
	/** F15-size0 ИНВАРИАНТ (BUG-015, см. decisions/F-size0-catalyst): в GT6-коде «ноль с памятью типа» хранится
	 *  ТОЛЬКО как ZEROSIZE-призрак (count=1+маркер, писать через {@link #size_}) — такой стек копируется ШТАТНО.
	 *  Физический count<=0 легален лишь как «потребление» (стек умер для движка: рука после траты и т.п.) и
	 *  ШАБЛОНОМ КОПИИ БЫТЬ НЕ ДОЛЖЕН (neo copy()/getItem() на нём слепы: EMPTY/AIR — 1.7.10 копировал item/NBT
	 *  всегда, на этом разрыве висел вечный цикл выдачи масстоража). Ветка ниже — НЕ рабочий путь, а аварийная
	 *  подушка с сиреной: нарушение инварианта чинится в ВЫЗЫВАТЕЛЕ (маршрутизацией нуля в size_), не здесь. */
	private static int sZeroCopyWarnings = 0;
	public static ItemStack copy_(ItemStack aStack) {
		if (aStack.getCount() <= 0 && aStack != ItemStack.EMPTY) {
			if (sZeroCopyWarnings < 10) {sZeroCopyWarnings++; ERR.println("[GT6] НАРУШЕНИЕ ИНВАРИАНТА F15-size0: копия физически-нулевого стека (потреблённый стек как шаблон?) — почини вызывателя: ноль с памятью типа пишется через ST.size_ (ZEROSIZE-призрак). Аварийная подушка сработала (" + sZeroCopyWarnings + "/10 предупреждений):"); new Throwable().printStackTrace(ERR);}
			int tOldCount = aStack.getCount();
			aStack.setCount(1);
			ItemStack rStack = aStack.copy();
			aStack.setCount(tOldCount);
			if (rStack == ItemStack.EMPTY) return rStack; // объект был настоящим air — копировать нечего
			rStack.setCount(Math.max(0, tOldCount));
			return rStack;
		}
		return aStack.copy();
	}
	
	public static ItemStack name (ItemStack aStack, String aName) {return aStack == null || aName == null ? aStack : name_(aStack, aName);}
	public static ItemStack name_(ItemStack aStack, String aName) {aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(aName)); return aStack;}
	/** F1/F8-стык: было {@code ItemStack.setStackDisplayName(...)} — в neo нет; когда имя уже {@code Component}
	 *  (напр. {@code getDisplayName()} в neo возвращает Component, а не String 1.7.10) — ставим CUSTOM_NAME напрямую,
	 *  сохраняя форматирование (не через literal(String), что теряло бы стиль). Центр тот же — {@code DataComponents.CUSTOM_NAME}. */
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
	 * F12/R3: единственная точка, через которую весь мод регистрирует Item — раньше был прямой
	 * выдуманный {@code DeferredRegister.registerItem(...)} (такого статического метода у NeoForge
	 * DeferredRegister нет). Теперь идёт через централизованный мост {@code gregapi.GT_API}
	 * (decisions/F12-registration-lifecycle.md) — весь мод обращается к ОДНОМУ центру, не создаёт
	 * свой DeferredRegister по месту.
	 */
	public static void register(Item aItem, String aRegistryName) {
		GT_API.registerItem(aItem, aRegistryName);
	}

	/** F16/F10 (1:1): 1.7.10 Item.setMaxStackSize(n) мутировал итем в рантайме. neo: GT6-итем (ItemBase) хранит поле +
	 *  override getMaxStackSize; ВАНИЛЬНЫЙ/форейн итем неизменяем (стек-размер = дефолт-компонент MAX_STACK_SIZE) —
	 *  штатно меняется через neo ModifyDefaultComponentsEvent. Копим намерение в карту; ПОДКЛЮЧЕНО:
	 *  applyVanillaComponentOverrides (ниже, подписан на mod-bus в GT_API) применяет её на событии. Вызыватели —
	 *  форейн-итемы (TC/TF) под .exists(): в этой сборке форейн-моды отсутствуют → карта пуста → применять нечего (F10). */
	public static final java.util.Map<Item, Integer> VANILLA_STACKSIZE_OVERRIDES = new java.util.IdentityHashMap<>();
	public static Item setMaxStackSize(Item aItem, int aSize) {
		if (aItem instanceof gregapi.item.ItemBase) return ((gregapi.item.ItemBase)aItem).setMaxStackSize(aSize);
		if (aItem != null) VANILLA_STACKSIZE_OVERRIDES.put(aItem, aSize);
		return aItem;
	}

	/** F16/F10: применяет vanilla/форейн stack-size-override на ModifyDefaultComponentsEvent (mod-bus, GT_API). Тайминг-безопасно:
	 *  прямые vanilla-твики (golden ST.forceProperMaxStacksizes 1:1) применяются в самом хендлере (не зависят от порядка загрузки).
	 *  craftRemainder так НЕ применить — craftingRemainingItem у neo Item иммутабельное поле (не компонент), см. setContainerItem. */
	public static void applyVanillaComponentOverrides(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent aEvent) {
		// BUG-021 v3 (тайминг): прежний носитель applyAllStackSizes — onModPostInit2Deferred (server-start), а ЭТО
		// событие — mod-load (RegistrationEvents.init, ПОСЛЕ CommonSetup: CommonModLoader.java:74-78) → карта применялась
		// ПУСТОЙ, все vanilla-стаки GT6 (жемчуг/пластинки/лёд/глина/… из OreDictPrefix.applyStackSizes) были мертвы.
		// Конфиг stacksizes.cfg прочитан ещё в onModPreInit2 (GT_API:817) → наполняем карту ЗДЕСЬ, до применения.
		// Повторный вызов на server-start (GT_API:1140, 1:1-место) остаётся — идемпотентен (та же величина).
		gregapi.oredict.OreDictPrefix.applyAllStackSizes();
		// форейн-мод override'ы (карта, если форейн-предмет присутствует)
		for (java.util.Map.Entry<Item, Integer> tE : VANILLA_STACKSIZE_OVERRIDES.entrySet())
			aEvent.modify(tE.getKey(), b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, tE.getValue()));
		// golden ST.forceProperMaxStacksizes() 1:1: GT6 менял стек vanilla-предметов (potion→1; glass_bottle/cake/stick/книги/
		// snowball/egg→64; bed→64; двери→8). 1.7.10 «bed»/«wooden_door» флэттились в neo → применяем ко ВСЕМ вариантам класса.
		aEvent.modify(net.minecraft.world.item.Items.POTION, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1));
		for (Item tItem : new Item[]{net.minecraft.world.item.Items.GLASS_BOTTLE, net.minecraft.world.item.Items.CAKE, net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.WRITTEN_BOOK, net.minecraft.world.item.Items.WRITABLE_BOOK, net.minecraft.world.item.Items.ENCHANTED_BOOK, net.minecraft.world.item.Items.SNOWBALL, net.minecraft.world.item.Items.EGG})
			aEvent.modify(tItem, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64));
		aEvent.modifyMatching((tItem, tComps) -> tItem instanceof net.minecraft.world.item.BedItem, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64)); // 1.7.10 bed→64 (все цвета)
		aEvent.modifyMatching((tItem, tComps) -> tItem instanceof net.minecraft.world.item.BlockItem tBI && tBI.getBlock() instanceof net.minecraft.world.level.block.DoorBlock, b -> b.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 8)); // 1.7.10 wooden_door+iron_door→8 (все двери)
	}

	/** F8 read-modify-write: 1.7.10 `stack.getTagCompound().putX(k,v)` мутировал ЖИВОЙ тег стека. neo CustomData
	 *  иммутабельна — `ItemNBT.get` отдаёт КОПИЮ, мутация без обратной записи ТЕРЯЕТСЯ (см. ItemNBT javadoc,
	 *  `gt6-contract-seams-blindspot`). Эти центр-хелперы делают атомарный get(копия)→put→set-обратно —
	 *  единственный 1:1-корректный путь для точечной записи в предметный NBT. */
	public static void nbtPut(ItemStack aStack, String aKey, net.minecraft.nbt.Tag aValue) {
		CompoundTag tNBT = ItemNBT.get(aStack); if (tNBT == null) tNBT = new CompoundTag();
		tNBT.put(aKey, aValue); ItemNBT.set(aStack, tNBT);
	}
	public static void nbtPutByte(ItemStack aStack, String aKey, byte aValue) {
		CompoundTag tNBT = ItemNBT.get(aStack); if (tNBT == null) tNBT = new CompoundTag();
		tNBT.putByte(aKey, aValue); ItemNBT.set(aStack, tNBT);
	}
	/** F8: 1.7.10 ST.hasNBT(stack)/setTagCompound(nbt) -> центр ItemNBT.has/set (CUSTOM_DATA). 1:1 по javadoc ItemNBT. */
	public static boolean hasNBT(ItemStack aStack) {return ItemNBT.has(aStack);}
	public static void setNBT(ItemStack aStack, CompoundTag aNBT) {ItemNBT.set(aStack, aNBT);}

	/** F16/F10 external-compat: 1.7.10 Item.setContainerItem(Item) задавал крафт-остаток (ведро->пустое) в рантайме.
	 *  neo craftingRemainingItem — ИММУТАБЕЛЬНОЕ поле Item (Item.java:303, не DataComponent) → post-construction не
	 *  меняется штатным событием (в отличие от MAX_STACK_SIZE). neo-модель остатка — per-recipe (recipe.getRemainingItems).
	 *  Вызыватели — ТОЛЬКО форейн-вёдра (RC/TC/TF) под .exists(): в этой сборке форейн-моды отсутствуют → карта пуста,
	 *  соответствующих итемов/рецептов нет → путь инертен (F10). Карта видима для будущего per-recipe-применения. Не заглушка ядра. */
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
	 * F12/R7: ЕДИНАЯ точка item-lookup по (modId, name) — раньше был выдуманный
	 * {@code DeferredRegister.findItem(...)} (1.7.10 {@code GameRegistry.findItem} мех-переименован в
	 * несуществующий метод). Реальный neo-путь: {@code BuiltInRegistries.ITEM} — {@code DefaultedRegistry<Item>}
	 * (сверено: neo-decompiled/net/minecraft/core/registries/BuiltInRegistries.java:185). Его
	 * {@code getValue(Identifier)} переопределён {@code @NonNull} и вернул бы AIR для неизвестного
	 * (DefaultedRegistry.java:12) — поэтому сперва {@code containsKey(Identifier)} (Registry.java:104),
	 * чтобы сохранить оригинальное «null для неизвестного» 1:1. {@code Identifier.fromNamespaceAndPath}
	 * — сверено (используется в DeferredRegister.java:230).
	 */
	public static Item findItem(String aModID, String aName) {
		if (aModID == null || aName == null) return null;
		Identifier tID = Identifier.fromNamespaceAndPath(aModID, aName);
		return BuiltInRegistries.ITEM.containsKey(tID) ? BuiltInRegistries.ITEM.getValue(tID) : null;
	}
	/** F12/R7: ЕДИНАЯ точка «item по (modId,name) → ItemStack размера aSize» (был выдуманный
	 *  {@code DeferredRegister.findItemStack(...)}). null, если item не зарегистрирован — как в оригинале. */
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
		// F-item-final ЦЕНТР: 1.7.10 aSetStack.func_150996_a(Item) — in-place смена Item существующего стека. neo
		// ItemStack.item = private final Holder<Item> (ItemStack.java:156) — прямого сеттера нет, а transmuteCopy даёт
		// КОПИЮ (не 1:1: весь GT6 опирается на in-place мутацию по ссылке). Единственный 1:1-путь — reflection на final-поле
		// через центр UT.Reflection.setField (безопасный try/catch-фолбэк: при неудаче item не сменится = прежнее поведение).
		// Holder нового предмета = Item.builtInRegistryHolder() (Item.java:153). NeoForge runtime = official mappings → поле "item".
		UT.Reflection.setField(aSetStack, "item", item_(aToStack).builtInRegistryHolder());
		if (aCheckStacksize) aSetStack.setCount(aToStack.getCount());
		meta_(aSetStack, meta_(aToStack));
		if (aCheckNBT) ItemNBT.set(aSetStack, ItemNBT.get(aToStack));
		return aSetStack;
	}
	/** F-item-final ЦЕНТР: 1.7.10 {@code ItemStack.func_150996_a(Item)} — in-place смена ТОЛЬКО Item (count/meta/NBT сохранены).
	 *  neo {@code ItemStack.item} = private final Holder<Item> — reflection на final-поле через центр {@link UT.Reflection#setField}
	 *  (безопасный try/catch-фолбэк). Holder предмета = {@code Item.builtInRegistryHolder()}. Отличается от {@link #set}: тот копирует всё. */
	public static ItemStack setItem(ItemStack aStack, Item aItem) {
		if (valid(aStack) && aItem != null) UT.Reflection.setField(aStack, "item", aItem.builtInRegistryHolder());
		return aStack;
	}
	/** F12-vanilla-durability ЦЕНТР: 1.7.10 {@code Item.setMaxDamage(int)} мутировал durability (GT config
	 *  SmallerVanillaToolDurability урезает vanilla-инструменты). neo: {@code MAX_DAMAGE} = DataComponent на
	 *  {@code builtInRegistryHolder().components()} (Holder.java:133). Публичный {@code Holder.Reference.bindComponents}
	 *  (Holder.java:262) — БЕЗ reflection: пересобрать component-map с новым MAX_DAMAGE. GT6-item durability = ItemBase.mMaxDamage (F12). */
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
		aStack.setCount((int)(aStack.getCount()-(aAmount))); // aAmount long -> явный cast (setCount(int))
		if (!(aPlayer instanceof Player)) return T;
		if (aStack.getCount() <= 0) {
			if (aTriggerEvent) EventHooks.onPlayerDestroyItem((Player)aPlayer, aStack, null); // neo: +@Nullable InteractionHand (EventHooks:235); generic decr без hand-контекста -> null
			if (aRemove) for (int i = 0; i < ((Player)aPlayer).getInventory().getNonEquipmentItems().size(); i++) {
				if (((Player)aPlayer).getInventory().getNonEquipmentItems().get(i) == aStack) {
					((Player)aPlayer).getInventory().getNonEquipmentItems().set(i, ItemStack.EMPTY); // граница vanilla NonNullList требует non-null (было null=1.7.10 empty)
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
	
	// F1-meta: neo ItemStack(ItemLike,int,int) 3-арг ctor с метой удалён (flattening) — мета хранится
	// компонентом DAMAGE (та же F1-модель, что центр meta_:188 setDamageValue). 2-арг ctor + setDamageValue.
	// F4-flatten (DEFERRED-LEDGER «block-flatten»): у ВАНИЛЬНОЙ вещи 1.7.10 подтип жил в мете (dye:1 = красный
	// краситель, stained_glass:14 = красное стекло), а движок 1.13 расщепил семейства на отдельные предметы.
	// Компонент SUBTYPE ванильная вещь не читает → без резолва на выходе всегда ПЕРВЫЙ член семьи с мёртвой
	// метой (улика из дампа: bath «стекло+красный краситель» → white_stained_glass:14). Центр карт — CS.Flattened
	// (там же обоснование и границы); здесь — единственная точка подстановки для вещей в стеке, поэтому все
	// 477 мест-вызывателей чинятся разом и остаются verbatim-1:1. Мета варианту НЕ ставится: она уже выражена
	// самим предметом. Не-семейные вещи (GT-мета-предметы, инструменты с износом, wildcard W) идут прежним путём.
	public static ItemStack make_(Item  aItem , long aSize, long aMeta) {Item  tFlat = CS.Flattened.item (aItem , aMeta); if (tFlat != null) return new ItemStack(tFlat, UT.Code.bindInt(aSize)); ItemStack tPotion = legacyPotion(aItem, aSize, aMeta); if (tPotion != null) return tPotion; ItemStack rStack = new ItemStack(aItem , UT.Code.bindInt(aSize)); meta_(rStack, UT.Code.bindShort(aMeta)); return rStack;}

	// BUG-118 §2, МОДЕЛЬ МЕТЫ: мост «легаси-мета зелья 1.7.10 → neo PotionContents» — то же семейство мостов,
	// что CS.Flattened строкой выше, только вид у зелий в neo выражен КОМПОНЕНТОМ, а не отдельным предметом
	// (взрывное — предметом: SPLASH_POTION). В 1.7.10 вид кодировала мета (низ 4 бита — тип по PotionHelper,
	// 0x20 усиленное, 0x40 растянутое, 0x2000 питьевое, 0x4000 взрывное); без моста стек из ST.make был
	// безликим «Uncraftable Potion» — ни цвета, ни имени, ни эффекта при питье ванильным путём (все 96
	// контейнеров-зелий Loader_Fluids). SUBTYPE-мета сохраняется как раньше — личность стека в картах и
	// рецептах не меняется. Комбинация без neo-варианта деградирует до базового вида (усиленное огнестойкое
	// и т.п. в 1.7.10 не регистрировалось); неизвестная мета → null = прежний путь без компонента, не выдумываем.
	private static ItemStack legacyPotion(Item aItem, long aSize, long aMeta) {
		if (aItem != Items.POTION && aItem != Items.SPLASH_POTION && aItem != Items.LINGERING_POTION) return null;
		net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> tKind = legacyPotionKind(aMeta);
		if (tKind == null) return null;
		// lingering — отдельный ПРЕДМЕТ уже на входе (тара EtFu-ветки замощена на ваниль, Loader_Fluids:344);
		// взрывной бит меты предмет не меняет — у lingering его в метах не было
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
		// низ 4 бита пусты: базовые варева (16 неуклюжее, 32 густое, 64/8192 заурядное) и вода (мета 0 = бутылка воды 1.7.10)
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
	public static ItemStack make(ModData aModID, String aItem, long aSize, long aMeta                                   ) {return     meta(make(aModID, aItem, aSize), aMeta);}
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
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: vanilla-Container отдаёт EMPTY, 1.7.10-тело рассуждает null
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			for (int aSlotTo : aSlotsTo) {
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: см. выше
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
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: vanilla-Container отдаёт EMPTY, 1.7.10-тело рассуждает null
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			for (int aSlotTo : aSlotsTo) {
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: см. выше
				int tMovable = Math.min(aMaxMove, canPut((Container)aTo.mTileEntity, aIgnoreSideTo ? SIDE_ANY : aTo.mSideOfTileEntity, aTo.mSideOfTileEntity, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSize, aStackFrom.getMaxStackSize())));
				if (tMovable < aMinMove || tMovable + (aStackTo == null ? 0 : aStackTo.getCount()) < aMinSize) continue;
				// Actually Moving the Stack
				rMoved += move_((Container)aFrom.mTileEntity, (Container)aTo.mTileEntity, aStackFrom, aStackTo, aSlotFrom, aSlotTo, tMovable);
				aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: vanilla-Container отдаёт EMPTY, 1.7.10-тело рассуждает null
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
		
		ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: vanilla-Container отдаёт EMPTY, 1.7.10-тело рассуждает null
		if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) return 0;
		for (int aSlotTo : aSlotsTo) {
			ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: см. выше
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
			ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: vanilla-Container отдаёт EMPTY, 1.7.10-тело рассуждает null
			if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) continue;
			ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: см. выше
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
				ItemStack aStackFrom = n(((Container)aFrom.mTileEntity).getItem(aSlotFrom)); // F15: vanilla-Container отдаёт EMPTY, 1.7.10-тело рассуждает null
				if (aStackFrom == null || aStackFrom.getCount() < aMinMove || (aFilter != null && aFilter.contains(aStackFrom, T) == aInvertFilter) || !canTake((Container)aFrom.mTileEntity, aIgnoreSideFrom ? SIDE_ANY : aFrom.mSideOfTileEntity, aFrom.mSideOfTileEntity, aSlotFrom, aStackFrom)) return 0;
				ItemStack aStackTo = n(((Container)aTo.mTileEntity).getItem(aSlotTo)); // F15: см. выше
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
		// F15-size0 (BUG-011, предмет исчезал): neo removeItem = split ТОГО ЖЕ объекта — aStackFrom (снимок getItem)
		// обнуляется, count-0 стек = EMPTY, amount/copy от него = air. В 1.7.10 decrStackSize оставлял Item у size-0
		// стека и копия работала. Источник копии = tStack (реально изъятое removeItem, item/NBT те же).
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
		// F15-size0 (BUG-011): см. move_(Container,...) выше — копия от tStack, не от обнулённого aStackFrom.
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
		} else if (WD.getMaterial(aBlock) == gregapi.block.Material.lava /* F9 (1:1): не выбрасывать предметы на лаву; WD.getMaterial реализован (стух-тег снят) */ || aBlock instanceof FireBlock || (invalid(aBlock) && aTo.mY < 1)) {
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
	 * ЕДИНАЯ ТОЧКА полиморфного канала «контейнер-предмет» (BUG-022, добита 2026-07-26).
	 *
	 * <p>В 1.7.10 {@code hasContainerItem/getContainerItem} были методами САМОГО {@code Item}, поэтому
	 * {@code item_(aStack).hasContainerItem(aStack)} спрашивал любой GT6-предмет. В neo этих методов у
	 * {@code Item} нет, и GT6-реализации живут в ПЯТИ несвязанных корнях без общего предка:
	 * {@code ItemBase} (и его MultiItem*), {@code PrefixItem}, {@code ItemFluidDisplay},
	 * {@code MultiTileEntityItemInternal}, {@code PrefixBlockItem}. Прежняя правка спрашивала только
	 * {@code ItemBase} — остальные четыре корня канал теряли.</p>
	 *
	 * <p><b>Улика (Ф4 шаг 3).</b> Судья паритета: у порта 1479 рецептов плавки химических пробирок против
	 * ОДНОГО в эталоне (+ столько же в melter = 2958 записей, 39 % всех расхождений рецептов). Пробирка —
	 * {@code PrefixItem}, её контейнер (пустая пробирка) не спрашивался, поэтому наполненная пробирка
	 * считалась обычным ингредиентом и попадала в общий генератор плавки
	 * ({@code Loader_OreProcessing}). Живая диагностика подтвердила: контейнер префикса на момент события
	 * УЖЕ установлен — значит дело было не в тайминге, а в том, что его никто не спрашивал.</p>
	 */
	private static ItemStack gtContainerItem(ItemStack aStack) {
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
		// F5/BUG-045 (1:1): fluid-контейнер (и пустой тоже) — не ингредиент; восстановленный IFluidContainerItem
		// (compat-mirror; оригинал :841). Реестровые наполненные контейнеры ловятся каналами ниже, как в оригинале.
		if (item_(aStack) instanceof IFluidContainerItem tICI && tICI.getCapacity(aStack) > 0) return F;
		// BUG-022 v2: симметрично ST.container — 1.7.10 звал полиморфный hasContainerItem (GT6-бутылки/каны/prefix),
		// компонентный канал ниже покрывает только vanilla. Спрашиваем ВСЕ GT-корни через единую точку выше.
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
		// BUG-022 v2 (стол Грега): 1.7.10 здесь был ПОЛИМОРФНЫЙ Item-канал hasContainerItem/getContainerItem —
		// GT6-инструменты (MultiItemTool:579) давали копию с износом; порт свёл к компонентному getCraftingRemainder
		// (у GT6-предметов его нет) → инструмент-ингредиент ПРОПАДАЛ во всех вызывателях ST.container (в т.ч.
		// MultiTileEntityAdvancedCraftingTable.consumeSlot:436). Живой ItemBase-канал восстановлен ПЕРВЫМ, компонентный
		// (vanilla ведро и т.п.) — следом, 1:1 порядок оригинала.
		ItemStack tGTContainer = gtContainerItem(aStack); // все GT-корни разом (см. gtContainerItem)
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
		// F5/BUG-045 (1:1): пустой контейнер после слива — восстановленный IFluidContainerItem (compat-mirror;
		// оригинал :868-876 — drain до пустоты, guard count<=0, очистка пустого NBT).
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
		return aStack.has(DataComponents.FOOD); // было: item_(aStack) instanceof ItemFood — класс удалён в neo (REMAP-RULES §C-bis)
	}
	/** F-armor-detection: 1.7.10 класс ItemArmor удалён -> броня определяется компонентом DataComponents.EQUIPPABLE
	 *  (Equippable.slot(), EquipmentSlot.isArmor()=HUMANOID_ARMOR|ANIMAL_ARMOR, EquipmentSlot.java:73). Центр item-домена. */
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
		// F#/fuel-registry: 1.7.10 GameRegistry.getFuelValue(ItemStack) — статический реестр без контекста
		// мира. В neo топливная ценность приходит из FuelValues, который живёт на MinecraftServer/Level, не
		// в статическом реестре (neo-decompiled/net/minecraft/server/MinecraftServer.java:2302 fuelValues(),
		// net/minecraft/server/level/ServerLevel.java:1853). Сигнатура fuel(ItemStack) без Level не меняется —
		// текущий сервер берём централизованно через ServerLifecycleHooks.getCurrentServer() (neoforge-decompiled/
		// net/neoforged/neoforge/server/ServerLifecycleHooks.java:130); оба вызывателя (RecipeMapFurnaceFuel:53,
		// RecipeMapMicrowave:96) уже гейтят вызов через GAPI_POST.mFinishedServerStarted>0, так что к этому
		// моменту сервер всегда поднят и fuelValues() заполнен (MinecraftServer.java:357).
		// aStack.getBurnTime(RecipeType,FuelValues) — реальный neo-путь (neoforge-decompiled/net/neoforged/
		// neoforge/common/extensions/IItemStackExtension.java:62, ItemStack implements его — ItemStack.java:103),
		// проходит через FurnaceFuelBurnTimeEvent — та же роль "кастомный fuel регистрируется модами", что и
		// 1.7.10 GameRegistry.getFuelValue.
		net.minecraft.server.MinecraftServer tFuelServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
		long rFuelValue = tFuelServer == null ? 0 : aStack.getBurnTime(null, tFuelServer.fuelValues());
		if (rFuelValue > 0) return rFuelValue;
		Item tItem = item_(aStack);
		// F9 redundant (1:1 покрыт выше): 1.7.10 давал 200 fuel деревянным vanilla-инструментам через instanceof
		// ItemTool/ItemSword/ItemHoe (классы удалены — инструмент теперь Item+компоненты). В neo это ИЗБЫТОЧНО:
		// getBurnTime (выше, строка ~1090) читает data-driven fuel-registry, где vanilla деревянные инструменты уже
		// несут burn-time 200 → return rFuelValue сработал бы раньше. Отдельная instanceof-проверка не нужна.
		if (tItem == Items.STICK) return 100;
		if (CS.Flattened.headItemOf(tItem) == Items.COAL) return 1600;
		if (tItem == Items.BLAZE_ROD) return 2400;
		if (tItem == Items.LAVA_BUCKET) return 20000;
		Block tBlock = block_(tItem);
		// 1.7.10 `Blocks.sapling`/`wooden_slab` — ОДИН блок с метой-породой, т.е. время горения было у ЛЮБОЙ породы.
		// В neo семьи расщеплены на блоки по породам; сравнение с дубовым оставило бы ель/берёзу/джунгли/акацию/
		// тёмный дуб без времени горения. Признак семьи берём ванильными тегами — тем же приёмом, каким порт уже
		// определяет древесные семьи в WD (`BlockTags.LEAVES`/`SAPLINGS`/`WOODEN_SLABS`, WD.java:620-640).
		if (tBlock != null && tBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.SAPLINGS)) return 100;
		if (tBlock != null && tBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.WOODEN_SLABS)) return 150;
		if (tBlock == Blocks.COAL_BLOCK) return 16000;
		// F9: 1.7.10 WD.getMaterial(tBlock)==Material.wood → 300. neo убрал Material — эквивалент «дерево» = SoundType.WOOD
		// (тот же приём, что WD block-sound-центр). tBlock валиден (block_(tItem)!=NB проверять не нужно: NB.defaultBlockState — air, sound!=WOOD).
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
		try {if (UT.Code.stringValid(rName = aStack.getItem().getDescriptionId())) return rName.toString();} catch (Throwable e) {/*Do nothing*/} // getUnlocalizedName()->Item.getDescriptionId() (Item.java:350; ItemStack не имеет, ключ на Item)
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
	
	// BUG-010: 1.7.10 ST.hide прятал предмет из NEI (codechicken.nei API — в neo мёртвый канал, catch глотал вызов
	// молча) → 5 скрытых слэб-вариантов КАЖДОЙ метатип-семьи (BlockMetaType: UP/N/S/W/E) стали видимы в креативе/JEI
	// (замер gt6slabprobe: 168 слэб-предметов, 140 лишних). Централизованная замена канала: реестр скрытых здесь,
	// его уважает CreativeTabsGT (populate + onBuildContents — оба пути наполнения вкладок; JEI строит список оттуда).
	// HIDDEN_BLOCKS отдельно: hide(Block) зовётся при КОНСТРУИРОВАНИИ блока (RegisterEvent<Block>), когда BlockItem ещё
	// не зарегистрирован (make(block,...) дал бы air) — прячем сам Block, hidden() сверяет через BlockItem.getBlock.
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
	
	// F16 ПОДКЛЮЧЕНО (1:1 golden forceProperMaxStacksizes): GT6-смена стека vanilla-предметов (setMaxStackSize —
	// рантайм-мутатор, удалён; стек-лимит = DataComponents.MAX_STACK_SIZE на регистрации) реализована в
	// applyVanillaComponentOverrides (выше) через ModifyDefaultComponentsEvent: potion→1; glass_bottle/cake/stick/
	// written_book/writable_book/enchanted_book/snowball/egg→64; все кровати→64; все двери→8. Не заглушка.
	public static boolean forceProperMaxStacksizes() {
		return T;
	}
	
	public static final Collection<ItemStack> REVERT_TO_BOOK_TO_FIX_STUPID = arraylist();
	public static void fixBookStacks() {for (ItemStack tStack : REVERT_TO_BOOK_TO_FIX_STUPID) set(tStack, make(Items.BOOK, 1, 0), T, T);}
	
	public static final List<String>
	LOOT_TABLES         = new ArrayListNoNulls<>(F, "dungeonChest", "villageBlacksmith", "mineshaftCorridor", "strongholdLibrary", "strongholdCrossing", "strongholdCorridor", "pyramidDesertyChest", "pyramidJungleChest", "pyramidJungleDispenser", "bonusChest"),
	LOOT_TABLES_VANILLA = new ArrayListNoNulls<>(F, "dungeonChest", "villageBlacksmith", "mineshaftCorridor", "strongholdLibrary", "strongholdCrossing", "strongholdCorridor", "pyramidDesertyChest", "pyramidJungleChest", "pyramidJungleDispenser", "bonusChest");
	
	// stats-loot: 1.7.10 ChestGenHooks.getOneItem(random-vanilla-table, RNGSUS). BUG-039: форма оригинала
	// восстановлена дословно — серверный семпл живёт ТОЛЬКО в центре shim-ChestGenHooks (итоговая таблица
	// vanilla+GT-пул; вне сервера null — как оригинал вне мира). Локальный дубль семпл-логики убран.
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
				// Twilight Forest вне сборки: гейт TF_TREASURE (ST.java:81 = F, взводится только при загруженном
				// моде, :95) — ветка недостижима. Тот же AbstractContainerMenu/Container-шов живёт в
				// gregtech.worldgen.TwilightTreasureReplacer; здесь мост через checked cast.
				TwilightTreasureReplacer.generate((net.minecraft.world.Container)aInv, aLoot);
			} else if (!LOOT_TABLES_VANILLA.contains(aLoot)) {
				// BUG-039 (F-loot): GT6-категории (gt.gems/gt.misc/...) — содержимое целиком в буфере shim-ChestGenHooks
				// (net.minecraftforge.common; заполняет Loader_Loot). Строка 1:1 с оригиналом 1.7.10.
				net.minecraftforge.common.WeightedRandomChestContent.generateChestContents(aRandom, net.minecraftforge.common.ChestGenHooks.getItems(aLoot, aRandom), aInv, net.minecraftforge.common.ChestGenHooks.getCount(aLoot, aRandom));
			} else {
				// F-loot: ванильные 1.7.10-имена таблиц → индекс в LOOT_TABLES_VANILLA → VANILLA_LOOT_KEYS (1:1 порядок,
				// см. generateOneVanillaLoot) → взвешенная выдача таблицы (LootParams(CHEST)) — vanilla-часть
				// data-driven; GT-добавки уже инъектированы в таблицу пулом gregtech6:<категория> (shim-ChestGenHooks).
				// Дефолт SIMPLE_DUNGEON для неизвестного имени.
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
						// BUG-105 §1 (лог: «Tried to over-fill a container» ×37 за одну генерацию): КОЛИЧЕСТВО лута в
						// 1.7.10 задавал СЧЁТЧИК КАТЕГОРИИ, а не таблица — generateChestContents крутил ровно
						// getCount(категория) итераций и клал каждый предмет в СЛУЧАЙНЫЙ слот, перезаписывая занятый
						// (WeightedRandomChestContent:39-51). Переполнения там не бывает по построению. Порт звал
						// LootTable.fill, отдав количество на откуп neo-таблице: замер gt6lootprobe — simple_dungeon
						// просит в среднем 21 стек против count=8 у 1.7.10, а книжная полка (DummyInventory(14),
						// MultiTileEntityBookShelf:107) переполнялась почти всегда, и движок МОЛЧА выбрасывал лишнее.
						// Возвращаем 1.7.10-раскладку: единый взвешенный список даёт сама таблица (ваниль + пул
						// gregtech6:<категория>), а сколько из него взять и куда положить — решает счётчик, как раньше.
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
				// F8 протухшие enchant-holder'ы шаблонов GT-лута (класс BUG-002) — пере-резолв по текущему серверу
				// (краш энкода сундука «Can't find id for sharpness» при смене мира в сессии; см. UT.NBT.freshenEnchantments).
				UT.NBT.freshenEnchantments(tStack);
				if (IL.TC_Gold_Coin.exists()) {
					if (item_(tStack) == Items.GOLD_NUGGET) {
						set(tStack, IL.TC_Gold_Coin.get(tStack.getCount()));
					}
					if (item_(tStack) == Items.GOLD_INGOT && tStack.getCount() <= 7) {
						set(tStack, IL.TC_Gold_Coin.get(tStack.getCount() * 9L));
					}
				}
				// Мод EtFu вне сборки: ветка целиком под гейтом IL.EtFu_Sus_Stew.exists() (LoaderItemList.java:1444)
				// — недостижима. 1.7.10 Potion-эффекты как сырой NBT-список ("EffectId"/"EffectDuration"
				// с числовым .id) — в neo эффекты registry-объекты (MobEffects.<ИМЯ>, Holder<MobEffect>, без .id), а
				// формат хранения эффектов на стаке — DataComponents (не произвольный "Effects" ListTag). Оригинал:
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
		// FORCED-ADAPTATION(F18-achievements): оригинал = achieve(parentAchievement)+triggerAchievement (stat-флаг+локальный тост).
		// Neo удалил Achievement/AchievementList/triggerAchievement; единственный API PlayerAdvancements.award() навязывает
		// выдачу рецептов+опыта+чат-анонс+событие (neo PlayerAdvancements.java:177-182) — добавленное поведение, нарушает
		// verbatim-1:1 (принцип 6, PORTING-LAW). Решение: централизованный no-op. Полное доказательство + таблица старое→RL
		// (2 достижения без эквивалента: mineWood/killCow) — decisions/F18-achievements.md. Паритет-вес нулевой.
		return T;
	}
	
	// FORCED-ADAPTATION(F18-achievements): net.minecraft.stats.AchievementList/Achievement/triggerAchievement удалены в neo
	// (data-driven Advancement/PlayerAdvancements). Выдача через award() навязывает рецепты/опыт/чат/событие — не 1:1;
	// mineWood/killCow эквивалента не имеют. Решение (доказательство + таблица RL): decisions/F18-achievements.md.
	// Ванильные вызовы achieve(aPlayer, AchievementList.X) ниже — централизованный no-op; исходная константа сохранена для трассы.
	public static boolean check(Entity aPlayer, ItemStack aStack) {
		if (!(aPlayer instanceof Player) || aPlayer.level() == null || aPlayer.level().isClientSide()) return F;

		if (F /* F18-redundant: гейтил ТОЛЬКО vanilla-достижение portal (achieve ниже), которое neo авто-выдаёт advancement'ом.
		     neo dimension-check есть (aPlayer.level().dimension()==Level.NETHER), но здесь гейтил бы no-op → оставляем F */) {
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

		// FORCED-ADAPTATION(F18, как соседние ниже): весь блок — лишь вручную выдавал vanilla-достижения
		// buildHoe/buildSword/buildPickaxe по instanceof типа инструмента. В neo достижения = advancements, которые
		// движок авто-выдаёт при получении предмета → ручная выдача ИЗБЫТОЧНА (а классы ItemHoe/ItemSword/ItemPickaxe
		// всё равно удалены). Оригинал (no-op в neo): achieve(buildHoe) для ItemHoe / buildSword / buildBetter?Pickaxe.

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
			if (tStack != null && (tStack.getCount() == 0 || tStack.getItem() == null)) aInv.setItem(i, ItemStack.EMPTY); // F15: neo Inventory=NonNullList, setItem(i,null) кидает NPE (было null — легальная очистка слота в 1.7.10)
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
		// BUG-077: Count<=0 — это «ноль с памятью типа» (1.7.10 писал stackSize=0). Собрать стек с нулём в neo
		// нельзя (получится EMPTY, идентичность потеряется), поэтому строим на 1 и помечаем ZEROSIZE-призраком
		// через центр size_ — ровно то представление, которым GT6-код пользуется в рантайме (ST.count даст 0).
		int tCount = aNBT.getIntOr("Count", 0);
		ItemStack rStack = make(Item.byId(aNBT.getShortOr("id",(short)0)), tCount <= 0 ? 1 : tCount, aNBT.getShortOr("Damage",(short)0));
		if (rStack == null) if (aNBT.contains("od")) {
			rStack = OreDictManager.INSTANCE.getStack(aNBT.getStringOr("od",""), tCount <= 0 ? 1 : tCount);
			if (rStack == null) return aDefault == null ? null : update_(OM.get_(aDefault));
		} else return aDefault == null ? null : update_(OM.get_(aDefault));
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
	/** было {@code aStack.writeToNBT(aNBT)} (1.7.10 ItemStack — пишет поля стека В переданный tag, возвращает его) —
	 *  neo: компонентная модель, воспроизводим через save(aStack)+merge (ключи id/Count/Damage 1:1). */
	public static CompoundTag writeToNBT(ItemStack aStack, CompoundTag aNBT) {
		CompoundTag tNBT = save(aStack);
		return tNBT == null ? aNBT : aNBT.merge(tNBT);
	}

	public static CompoundTag save(ItemStack aStack) {
		if (aStack == null || aStack == ItemStack.EMPTY || item_(aStack) == null || aStack.getCount() < 0) return null;
		CompoundTag rNBT = UT.NBT.make();
		aStack = OM.get_(aStack);
		rNBT.putShort("id", id(aStack));
		// BUG-077 (бесконечный генератор предметов): пишем ЛОГИЧЕСКИЙ размер, а не физический count.
		// «Ноль с памятью типа» (масстораж после опустошения, катализаторы) в neo хранится ZEROSIZE-призраком —
		// count=1 + компонент-маркер, потому что neo не держит count<=0. Формат этой записи 1:1 с 1.7.10
		// (id/Count/Damage/tag/od) и про компоненты НЕ знает: маркер в NBT не попадал, а Count уходил как 1.
		// После перезахода в мир призрак оживал настоящим предметом — предмет из ничего. Логический count
		// (ST.count) отдаёт 0 ровно там, где 1.7.10 писал stackSize=0; обратно призрак собирает ST.load.
		// Ноль пишется ЯВНО (putInt): UT.NBT.setNumber опускает нулевые значения, и «ноль с памятью типа»
		// держался бы на соглашении «ключа нет = 0». Соглашение верное, но неявное — при чтении чужим
		// парсером отсутствие Count читается как 1. Пишем 0 буквально, как это делал 1.7.10.
		int tLogicalCount = count(aStack);
		if (tLogicalCount <= 0) rNBT.putInt("Count", 0); else UT.NBT.setNumber(rNBT, "Count", tLogicalCount);
		UT.NBT.setNumber(rNBT, "Damage", meta_(aStack));
		if (ItemNBT.has(aStack)) rNBT.put("tag", ItemNBT.get(aStack));
		OreDictItemData tData = OM.anyassociation_(aStack);
		if (tData != null) rNBT.putString("od", tData.toString());
		return rNBT;
	}
}
