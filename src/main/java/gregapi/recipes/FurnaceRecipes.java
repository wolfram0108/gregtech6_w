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
 * <p>ИНТЕГРАЦИЯ с ванильной печью neo ЗАКРЫТА (BUG-023, подтверждён живым тестом игрока): её делает
 * {@link GT6SmeltingDispatcher} — единственная точка входа GT6-плавок в печь, рецепт типа
 * {@code RecipeType.SMELTING} перебирает ЭТОТ реестр в {@code matches}/{@code assemble}, и печь находит
 * плавки штатно ({@code AbstractFurnaceBlockEntity.serverTick} → {@code quickCheck.getRecipeFor}).
 * Ни recipe-provider/датаген, ни mixin в {@code RecipeManager} не понадобились. Здесь — сам GT6-реестр плавок
 * (add/remove/query/iterate), 1:1 с 1.7.10.
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

	/** 1.7.10 func_151398_b = getSmeltingExperience(output). Правило 1:1 (recompSrc FurnaceRecipes:115-135):
	 *  СНАЧАЛА хук предмета-результата, его ответ != -1 ПЕРЕКРЫВАЕТ карту (у GT6-предметов: самоцвет → 1.0,
	 *  иначе 0 — анти-фарм Грега); не-носитель контракта = дефолт 1.7.10 Item.getSmeltingExperience = -1
	 *  «спроси карту»; нет и в карте → 0. */
	public float func_151398_b(ItemStack aOutput) {
		if (ST.invalid(aOutput)) return 0.0F;
		if (aOutput.getItem() instanceof gregapi.item.IItemSmeltingExperience tItem) {
			float tXP = tItem.getSmeltingExperience(aOutput);
			if (tXP != -1) return tXP;
		}
		for (Map.Entry<ItemStack, Float> tEntry : mExperienceList.entrySet()) if (ST.equal(aOutput, tEntry.getKey(), T)) return tEntry.getValue();
		return 0.0F;
	}

	/**
	 * ВОЗВРАТ ВАНИЛЬНОЙ ЧАСТИ СПИСКА — то, чем этот список БЫЛ в 1.7.10.
	 *
	 * <p>Там {@code FurnaceRecipes.smelting()} был ВАНИЛЬНЫМ singleton'ом: ванильные плавки лежали в нём
	 * изначально, GT6 доливал свои через {@code RM.add_smelting}, и печь GT6 (Oven), спрашивая
	 * {@code RM.get_smelting} → {@code getSmeltingResult}, видела и то и другое. Порт воссоздал класс как
	 * GT6-собственное хранилище (шов F11-smelting), а ванильные рецепты в neo — data-driven и живут в
	 * {@code RecipeManager}. Список остался наполовину пустым, и Oven переставал плавить руду, еду, глину:
	 * замер gt6ovenprobe — 1 из 8 ванильных сырьевых предметов (проходил только песок, эту плавку GT6
	 * добавляет себе сам), при 74 ванильных рецептах в мире.
	 *
	 * <p>Здесь список приводится к прежнему содержимому. ПРИОРИТЕТ GT6: если вход уже знаком (GT6 задал свою
	 * плавку для этого предмета), ванильная пропускается — в 1.7.10 тот же порядок обеспечивался тем, что GT6
	 * доливал СВОЁ поверх ванильного и перекрывал его при поиске.
	 *
	 * <p><b>РЕШЕНИЕ ПОЛЬЗОВАТЕЛЯ 2026-07-29 — переносим ВСЕ ванильные плавки, включая рецепты предметов,
	 * которых в 1.7.10 не существовало</b> ({@code raw_iron}/{@code raw_copper}/{@code raw_gold} появились
	 * в 1.17: там руда плавилась блоком, промежуточного «сырья» не было). Строго 1:1 это не воспроизведение,
	 * а решение: увязка ресурсов НОВЫХ версий отложена до полного завершения порта, отдельной задачей.
	 * Риск при этом теоретический: ванильные руды мод выключает сам — {@code GT6_Main.java:132},
	 * {@code mDisableVanillaOres} по умолчанию {@code T}, — поэтому до нового сырья игрок в норме не доходит.
	 *
	 * @return сколько ванильных плавок добавлено.
	 */
	public int importVanilla(net.minecraft.server.MinecraftServer aServer) {
		if (aServer == null) return 0;
		int rAdded = 0;
		try {
			net.minecraft.server.level.ServerLevel tLevel = aServer.overworld();
			if (tLevel == null) return 0;
			// ⛔ Обходим как RecipeHolder<?> и фильтруем instanceof: в реестре типа SMELTING лежит НЕ ТОЛЬКО
			// ванильный SmeltingRecipe, но и собственный мост GT6 — GT6SmeltingDispatcher (BUG-023), который
			// отдаёт GT6-плавки ванильной печи. Типизированный обход ронял ClassCastException прямо на нём,
			// цикл обрывался на середине (перенеслось 149 из всех, булыжник и прочее за ним — нет).
			// Диспетчер здесь пропускаем осознанно: он не носитель данных, а переходник в ЭТОТ же реестр.
			for (net.minecraft.world.item.crafting.RecipeHolder<?> tHolder
				: tLevel.recipeAccess().recipeMap().byType(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
				if (!(tHolder.value() instanceof net.minecraft.world.item.crafting.SmeltingRecipe tRecipe)) continue;
				for (net.minecraft.core.Holder<net.minecraft.world.item.Item> tItem : tRecipe.input().items().toList()) {
					ItemStack tIn = new ItemStack(tItem);
					if (ST.invalid(tIn)) continue;
					// GT6 приоритетнее: свою плавку не перекрываем
					if (ST.valid(getSmeltingResult(tIn))) continue;
					ItemStack tOut = tRecipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(tIn));
					if (ST.invalid(tOut)) continue;
					func_151394_a(tIn, ST.copy(tOut), tRecipe.experience());
					rAdded++;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace(gregapi.data.CS.ERR);
		}
		return rAdded;
	}
}
