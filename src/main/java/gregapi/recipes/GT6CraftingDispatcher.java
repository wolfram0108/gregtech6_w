/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregapi.recipes;

import gregapi.data.CS;
import gregapi.data.MD;
import gregapi.util.CR;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/**
 * @author Gregorius Techneticies
 *
 * F11 (decisions/F11-crafting-recipe.md §4,§5) — ЕДИНСТВЕННАЯ точка входа процедурно генерируемого
 * крафта GT6 в ванильный верстак. Движок наполняет {@code RecipeManager} ТОЛЬКО из датапак-JSON
 * (forge-1201-decompiled {@code RecipeManager.java:49,53}, рантайм-{@code addRecipe} удалён ещё в 1.12) —
 * штатный путь динамического код-рецепта без ингредиентов в JSON — {@code CustomRecipe} (эталон той же
 * версии: AE2-1.20.1 {@code appeng/recipes/game/FacadeRecipe.java:36-86} + заглушка
 * {@code data/ae2/recipes/special/facade.json}).
 *
 * <p>Контракт 1.20.1 отличается от 26.x: {@code Recipe<C extends Container>} (не {@code RecipeInput}),
 * обёртки «рецепт+id» нет — id носит сам рецепт ({@code CustomRecipe.getId()},
 * {@code net/minecraft/world/item/crafting/CustomRecipe.java:14}); сетка — {@code CraftingContainer}
 * (прямой наследник 1.7.10 {@code InventoryCrafting}), {@code assemble} берёт {@code RegistryAccess}.</p>
 *
 * <p>Диспетчер сам НЕ хранит рецептов и НЕ переписывает генерацию: {@link #matches}/{@link #assemble}
 * перебирают собственный ПОСТОЯННЫЙ буфер GT6 ({@link CR#BUFFER}, наполняемый процедурно как раньше) и
 * исполняют дословную логику найденного {@link ICraftingRecipeGT} (NBT/заряд/зачар/динамический материал —
 * уже реализовано в {@code AdvancedCrafting*}/{@code ShapedOreRecipe}/{@code ShapelessOreRecipe}). Порядок —
 * порядок самого буфера (F11 §4.3: {@code RecipeSorter} отброшен, замены не требует).</p>
 *
 * <p>{@link #register(IEventBus)} вызывается из {@code GT_API}-конструктора (тот же мод-бас, на который
 * подписаны {@code ITEMS}/{@code BLOCKS}); плюс ОДНА JSON-заглушка
 * {@code data/gregapi/recipes/special/gt6_crafting_dispatcher.json}.</p>
 */
public final class GT6CraftingDispatcher extends CustomRecipe {
	public static final SimpleCraftingRecipeSerializer<GT6CraftingDispatcher> SERIALIZER = new SimpleCraftingRecipeSerializer<>(GT6CraftingDispatcher::new);

	/** Центральный DeferredRegister — ЕДИНСТВЕННОЕ место, где GT6 регистрирует рецепт-сериализаторы. */
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MD.GAPI.mID);

	static {
		SERIALIZERS.register("gt6_crafting_dispatcher", () -> SERIALIZER);
		SERIALIZERS.register("gt6_smelting_dispatcher", () -> GT6SmeltingDispatcher.SERIALIZER); // F11-smelting (BUG-023): печь — тем же центральным реестром
	}

	/** F11: точка подписки на мод-шину. Вызывается из {@code GT_API}-конструктора (F12↔F11 стык). */
	public static void register(IEventBus aModBus) {
		SERIALIZERS.register(aModBus);
	}

	public GT6CraftingDispatcher(ResourceLocation aID, CraftingBookCategory aCategory) {
		super(aID, aCategory);
	}

	@Override
	public boolean matches(CraftingContainer aGrid, Level aLevel) {
		List<ICraftingRecipeGT> tList = CR.list();
		for (int i = 0, j = tList.size(); i < j; i++) {
			ICraftingRecipeGT tRecipe = tList.get(i);
			if (tRecipe != null && tRecipe.matches(aGrid, aLevel)) return T;
		}
		return F;
	}

	@Override
	public ItemStack assemble(CraftingContainer aGrid, RegistryAccess aRegistries) {
		// Level в assemble не передаётся (Recipe.java:15); ни один ICraftingRecipeGT.matches его не
		// разыменовывает (Shaped/Shapeless/1ToY/XToY/Tool читают только сетку) — CS.DW (dummy world),
		// как и в остальном CR.java (CR.get/CR.remove).
		List<ICraftingRecipeGT> tList = CR.list();
		for (int i = 0, j = tList.size(); i < j; i++) {
			ICraftingRecipeGT tRecipe = tList.get(i);
			// refreshEnchantments — центр-воронка всех GT6-крафтов; в 1.20.1 реестр чар статический, класса
			// дефекта BUG-002 нет, и метод сведён к тождеству (UT.java:2322) — вызыватель сохранён как точка.
			if (tRecipe != null && tRecipe.matches(aGrid, CS.DW)) return gregapi.util.UT.NBT.refreshEnchantments(tRecipe.getCraftingResult(aGrid));
		}
		return ItemStack.EMPTY;
	}

	/** Буфер несёт рецепты любых габаритов (от 1 клетки) — отбор по размеру делает сам рецепт в {@code matches}. */
	@Override
	public boolean canCraftInDimensions(int aWidth, int aHeight) {
		return aWidth * aHeight >= 1;
	}

	// BUG-022: 1.7.10 остаток крафта = per-item Forge-канал hasContainerItem/getContainerItem (SlotCrafting звал для
	// КАЖДОГО слота; GT6-инструменты давали копию с износом doDamage(getToolDamagePerContainerCraft) — MultiItemTool:579,
	// бутылки/каны/prefix — свои). Дефолт Recipe.getRemainingItems (Recipe.java:20-31) читает только
	// hasCraftingRemainingItem предмета — про GT6-канал не знает → инструмент-ингредиент потреблялся бы целиком.
	// Мост в ЕДИНОЙ воронке всех GT6-крафтов (F11-центр): GT6-предметы — через живой GT6-канал, прочие — ванильная
	// семантика 1:1.
	//
	// BP-BUG-014: спрашивать надо ВСЕ ПЯТЬ корней канала, а не один. В 1.7.10 hasContainerItem/getContainerItem были
	// методами САМОГО Item, поэтому SlotCrafting спрашивал любой GT6-предмет; в 1.20.1 у Item их нет, и реализации
	// GT6 живут в пяти несвязанных корнях (ItemBase и его MultiItem*, PrefixItem, ItemFluidDisplay, PrefixBlockItem,
	// MultiTileEntityItemInternal). Здесь спрашивался только ItemBase — тот же разрыв, что закрыт в ST.container
	// (BUG-022 v2). Своего перебора корней не заводим: спрашиваем ТУ ЖЕ единую точку — ST.containerItemGT.
	@Override
	public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingContainer aGrid) {
		net.minecraft.core.NonNullList<ItemStack> rRemaining = net.minecraft.core.NonNullList.withSize(aGrid.getContainerSize(), ItemStack.EMPTY);
		for (int i = 0; i < aGrid.getContainerSize(); i++) {
			ItemStack tStack = aGrid.getItem(i);
			if (tStack.isEmpty()) continue;
			ItemStack tRemainder = gregapi.util.ST.containerItemGT(tStack);
			if (tRemainder != null) {
				if (gregapi.util.ST.valid(tRemainder)) rRemaining.set(i, tRemainder);
			} else if (tStack.hasCraftingRemainingItem()) {
				rRemaining.set(i, tStack.getCraftingRemainingItem());
			}
		}
		return rRemaining;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
