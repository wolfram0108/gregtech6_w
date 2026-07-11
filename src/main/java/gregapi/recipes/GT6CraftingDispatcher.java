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
 */

package gregapi.recipes;

import com.mojang.serialization.MapCodec;
import gregapi.data.CS;
import gregapi.data.MD;
import gregapi.util.CR;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/**
 * @author Gregorius Techneticies
 *
 * F11 (decisions/F11-crafting-recipe.md §4,§5) — ЕДИНСТВЕННАЯ точка входа процедурно генерируемого
 * крафта GT6 в ванильный верстак neo. Neo наполняет {@code RecipeManager} ТОЛЬКО из датапак-JSON
 * (`RecipeManager.java:74`, рантайм-{@code addRecipe} удалён) — штатный путь динамического код-рецепта
 * без ингредиентов в JSON — {@code CustomRecipe} (эталон AE2 {@code appeng.recipes.game.FacadeRecipe},
 * `Applied-Energistics-2/.../FacadeRecipe.java:36-90`). Сигнатуры {@code Recipe}/{@code CraftingRecipe}/
 * {@code CustomRecipe} — {@code neo-decompiled/.../item/crafting/{Recipe,CraftingRecipe,CustomRecipe}.java}.
 *
 * <p>Диспетчер сам НЕ хранит рецептов и НЕ переписывает генерацию: {@link #matches}/{@link #assemble}
 * перебирают собственный ПОСТОЯННЫЙ буфер GT6 ({@link CR#BUFFER}, наполняемый процедурно как раньше) и
 * исполняют дословную логику найденного {@link ICraftingRecipeGT} (NBT/заряд/зачар/динамический материал —
 * уже реализовано в {@code AdvancedCrafting*}/{@code ShapedOreRecipe}/{@code ShapelessOreRecipe}). Порядок —
 * порядок самого буфера (F11 §4.3: {@code RecipeSorter} отброшен, замены не требует).</p>
 *
 * <p>{@link #register(IEventBus)} вызывается из {@code GT_API}-конструктора (тот же мод-бас, на который
 * подписаны {@code ITEMS}/{@code BLOCKS}/{@code GT6WorldgenFeature}/{@code FluidGT} — F12↔F11 стык закрыт,
 * паттерн зеркалит {@code gregapi.worldgen.GT6WorldgenFeature.register(IEventBus)}); плюс ОДНА JSON-заглушка
 * {@code data/gregapi/recipe/special/gt6_crafting_dispatcher.json} (аналог AE2
 * {@code data/ae2/recipe/special/facade.json}) — уже добавлена рядом с этим классом.</p>
 */
public final class GT6CraftingDispatcher extends CustomRecipe {
	public static final MapCodec<GT6CraftingDispatcher> CODEC = MapCodec.unit(GT6CraftingDispatcher::new);

	public static final StreamCodec<RegistryFriendlyByteBuf, GT6CraftingDispatcher> STREAM_CODEC = new StreamCodec<>() {
		@Override public GT6CraftingDispatcher decode(RegistryFriendlyByteBuf aBuf) {return new GT6CraftingDispatcher();}
		@Override public void encode(RegistryFriendlyByteBuf aBuf, GT6CraftingDispatcher aRecipe) {/* без данных, как AE2 FacadeRecipe */}
	};

	public static final RecipeSerializer<GT6CraftingDispatcher> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	/** Центральный DeferredRegister — ЕДИНСТВЕННОЕ место, где GT6 регистрирует крафт-верстак-сериализатор в neo. */
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(Registries.RECIPE_SERIALIZER, MD.GAPI.mID);

	static {
		SERIALIZERS.register("gt6_crafting_dispatcher", () -> SERIALIZER);
	}

	/** F11: точка подписки на мод-шину, зеркало {@code GT6WorldgenFeature.register(IEventBus)} (F6-переходник,
	 *  `GT6WorldgenFeature.java:158-163`). Вызывается из {@code GT_API}-конструктора (`GT_API.java`, F12↔F11 стык). */
	public static void register(IEventBus aModBus) {
		SERIALIZERS.register(aModBus);
	}

	@Override
	public boolean matches(CraftingInput aGrid, Level aLevel) {
		List<ICraftingRecipeGT> tList = CR.list();
		for (int i = 0, j = tList.size(); i < j; i++) {
			ICraftingRecipeGT tRecipe = tList.get(i);
			if (tRecipe != null && tRecipe.matches(aGrid, aLevel)) return T;
		}
		return F;
	}

	@Override
	public ItemStack assemble(CraftingInput aGrid) {
		// F11: Recipe.assemble(T input) не получает Level (neo-decompiled/.../item/crafting/Recipe.java:27);
		// ни один порт-ированный ICraftingRecipeGT.matches(...) не разыменовывает aWorld (AdvancedCrafting*/
		// Shaped/Shapeless/1ToY/XToY/Tool читают только сетку) — CS.DW (dummy world) как в остальном CR.java
		// (см. CR.get/CR.remove) вместо null, дословный повторный перебор буфера в поиске совпавшего рецепта.
		List<ICraftingRecipeGT> tList = CR.list();
		for (int i = 0, j = tList.size(); i < j; i++) {
			ICraftingRecipeGT tRecipe = tList.get(i);
			if (tRecipe != null && tRecipe.matches(aGrid, CS.DW)) return tRecipe.getCraftingResult(aGrid);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return SERIALIZER;
	}
}
