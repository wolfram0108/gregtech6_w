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

package gregapi.recipes;

import static gregapi.data.CS.*;

import java.util.HashMap;
import java.util.Map;

import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;

/**
 * F11-smelting ЦЕНТР. 1.7.10 vanilla {@code net.minecraft.item.crafting.FurnaceRecipes} — мутабельный singleton
 * ({@code getSmeltingResult}/{@code addSmelting}/{@code getSmeltingList}/{@code getSmeltingExperience}) — удалён из
 * neo: ванильные плавки стали data-driven ({@code RecipeManager} + датапаки, НЕИЗМЕНЯЕМЫ в рантайме). Порт наивно
 * переименовал {@code FurnaceRecipes} -> neo {@code RecipeManager} (у которого нет {@code smelting()}). GT6 же
 * добавляет/удаляет/итерирует плавки в рантайме (RM.add_smelting/rem_smelting/get_smelting/RecipeMapFurnace) —
 * воспроизводим прежний API 1:1 поверх GT6-собственного мутабельного хранилища.
 *
 * <p>PORT-TODO(F11-smelting, decisions/F11-crafting-recipe.md): ИНТЕГРАЦИЯ с ванильной печью neo (чтобы
 * neo-{@code FurnaceBlockEntity} реально использовал добавленные сюда рецепты) требует recipe-provider/датаген
 * или mixin в {@code RecipeManager} — отдельный ADR, ещё не разработан (не выдумываю). Здесь — GT6-реестр плавок,
 * self-consistent для внутренней логики RM (add/remove/query/iterate), но пока не проброшенный в neo-печь.
 * Тот же приём, что F12-config (gregapi.config.ModConfigSpec) — воссоздание удалённого движкового API как GT6-класса.
 */
public class FurnaceRecipes {
	private static final FurnaceRecipes INSTANCE = new FurnaceRecipes();

	/** 1.7.10 FurnaceRecipes.smelting() — singleton-доступ. */
	public static FurnaceRecipes smelting() {return INSTANCE;}

	private final Map<ItemStack, ItemStack> mSmeltingList   = new HashMap<>();
	private final Map<ItemStack, Float>     mExperienceList = new HashMap<>();

	/** 1.7.10 getSmeltingResult(input): первый выход, чей вход совпадает (ST.equal с wildcard, как итерация GT6). */
	public ItemStack getSmeltingResult(ItemStack aInput) {
		if (ST.invalid(aInput)) return NI;
		for (Map.Entry<ItemStack, ItemStack> tEntry : mSmeltingList.entrySet()) if (ST.equal(aInput, tEntry.getKey(), T)) return tEntry.getValue();
		return NI;
	}

	/** 1.7.10 func_151394_a = addSmeltingRecipe(input, output, experience). */
	public void func_151394_a(ItemStack aInput, ItemStack aOutput, float aExperience) {
		if (ST.invalid(aInput) || ST.invalid(aOutput)) return;
		mSmeltingList.put(aInput, aOutput);
		mExperienceList.put(aOutput, aExperience);
	}

	/** 1.7.10 getSmeltingList(): мутабельная карта — GT6 итерирует и удаляет через entrySet().iterator().remove(). */
	public Map<ItemStack, ItemStack> getSmeltingList() {return mSmeltingList;}

	/** 1.7.10 func_151398_b = getSmeltingExperience(output). */
	public float func_151398_b(ItemStack aOutput) {
		if (ST.invalid(aOutput)) return 0.0F;
		for (Map.Entry<ItemStack, Float> tEntry : mExperienceList.entrySet()) if (ST.equal(aOutput, tEntry.getKey(), T)) return tEntry.getValue();
		return 0.0F;
	}
}
