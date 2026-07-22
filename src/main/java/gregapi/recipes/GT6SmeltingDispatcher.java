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
import gregapi.util.ST;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import static gregapi.data.CS.ERR;
import static gregapi.data.CS.F;

/**
 * @author Gregorius Techneticies
 *
 * F11-smelting (BUG-023) — закрытие PORT-TODO из {@link FurnaceRecipes}: ЕДИНСТВЕННАЯ точка входа
 * GT6-плавок в ванильную печь neo. Тот же приём, что {@link GT6CraftingDispatcher} (верстак):
 * neo наполняет {@code RecipeManager} только из датапак-JSON, рантайм-add удалён — GT6 же
 * добавляет/удаляет плавки процедурно ({@code RM.add_smelting} → {@link FurnaceRecipes}, мутабельный
 * 1:1-реестр). ОДИН диспетчер типа {@code RecipeType.SMELTING} перебирает GT6-реестр в
 * {@code matches}/{@code assemble} — печь ({@code AbstractFurnaceBlockEntity.serverTick:170} через
 * {@code quickCheck.getRecipeFor}) находит и исполняет GT6-плавки штатно.
 *
 * <p>Ингредиент-витрина строится в конструкторе из ключей GT6-реестра (к моменту загрузки рецептов
 * data-init уже прошёл; клиентская копия рецепта строит её от клиентского реестра — GT6 наполняет
 * обе стороны): её единственные потребители — {@code RecipePropertySet.FURNACE_INPUT}
 * (shift-click-гейт меню печи, {@code AbstractFurnaceMenu.canSmelt:141} — статический СЕТ item'ов,
 * собирается {@code Ingredient::items} на reload) и витрина книги рецептов. Сама плавка судится
 * только {@code matches} (точный {@code ST.equal} с wildcard, как 1.7.10).</p>
 *
 * <p>Отклонения-форс движка (видимо): (1) XP печи в neo — поле рецепта ({@code experience()}), не
 * функция входа → на одном диспетчере per-рецептный XP GT6 ({@code mExperienceList}) невыразим;
 * у GT6-плавок XP=0 кроме 4 вызовов (0.05-0.1F) — диспетчер отдаёт 0. (2) Коптильня/домна
 * ({@code SmokerRecipes}/{@code BlastFurnaceRecipes}) — ветка {@code MD.EtFu} (не загружен) =
 * 1:1 поведение 1.7.10 без EtFu; их проброс — при F10-compat.</p>
 */
public final class GT6SmeltingDispatcher extends AbstractCookingRecipe {
	public static final MapCodec<GT6SmeltingDispatcher> CODEC = MapCodec.unit(GT6SmeltingDispatcher::new);

	public static final StreamCodec<RegistryFriendlyByteBuf, GT6SmeltingDispatcher> STREAM_CODEC = new StreamCodec<>() {
		@Override public GT6SmeltingDispatcher decode(RegistryFriendlyByteBuf aBuf) {return new GT6SmeltingDispatcher();}
		@Override public void encode(RegistryFriendlyByteBuf aBuf, GT6SmeltingDispatcher aRecipe) {/* без данных, как GT6CraftingDispatcher */}
	};

	public static final RecipeSerializer<GT6SmeltingDispatcher> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	public GT6SmeltingDispatcher() {
		// cookingTime 200 = ванильная печь 1.7.10 (фиксированные 200 тиков на плавку); XP 0 — см. javadoc.
		// result-шаблон — только витрина (AIR запрещён движком: «Item must be non-empty»); реальный выход всегда из assemble.
		// ingredient-плейсхолдер: реальная витрина — ЛЕНИВЫЙ override input() ниже (конструктор зовётся при парсе
		// датапака ДО GT6 data-init (runDeferredItemInit на server-start) — реестр плавок в этот момент ещё пуст).
		super(new Recipe.CommonInfo(F), new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""), Ingredient.of(Items.BARRIER), new ItemStackTemplate(Items.FURNACE), 0.0F, 200);
	}

	private Ingredient mLazyInput = null;
	private int mLazyInputSize = -1;

	/** Ленивая ингредиент-витрина из ключей GT6-реестра; все потребители (propertySet-extractor
	 *  {@code RecipeManager.forSingleInput:256}, placementInfo, display) идут через {@code input()}.
	 *  Отклонение-форс движка: {@code RecipePropertySet} (shift-click-гейт) собирается на reload рецептов, который
	 *  на ПЕРВОМ старте идёт до data-init → гейт слеп до следующей пересборки рецептов; ручная укладка в печь и
	 *  сама плавка ({@code matches} live-lookup'ом) работают всегда. */
	@Override public Ingredient input() {
		int tSize = FurnaceRecipes.smelting().getSmeltingList().size();
		if (mLazyInput == null || tSize != mLazyInputSize) {
			mLazyInputSize = tSize;
			java.util.LinkedHashSet<Item> tItems = new java.util.LinkedHashSet<>();
			for (ItemStack tKey : FurnaceRecipes.smelting().getSmeltingList().keySet()) if (!tKey.isEmpty()) tItems.add(tKey.getItem());
			mLazyInput = tItems.isEmpty() ? Ingredient.of(Items.BARRIER) : Ingredient.of(tItems.stream());
			if (tItems.isEmpty()) ERR.println("[GT6] GT6SmeltingDispatcher: реестр FurnaceRecipes пуст при запросе ингредиент-витрины (до data-init — штатно на первом reload)");
		}
		return mLazyInput;
	}

	@Override public boolean matches(SingleRecipeInput aInput, Level aLevel) {
		return ST.valid(FurnaceRecipes.smelting().getSmeltingResult(aInput.item()));
	}

	@Override public ItemStack assemble(SingleRecipeInput aInput) {
		// реестр хранит ЖИВОЙ выход-стек — наружу только копия (печь мутирует результат при burn/стаковке)
		ItemStack tResult = FurnaceRecipes.smelting().getSmeltingResult(aInput.item());
		return ST.valid(tResult) ? ST.copy(tResult) : ItemStack.EMPTY;
	}

	@Override protected Item furnaceIcon() {
		return Items.FURNACE;
	}

	@Override public RecipeSerializer<GT6SmeltingDispatcher> getSerializer() {
		return SERIALIZER;
	}

	@Override public RecipeType<net.minecraft.world.item.crafting.SmeltingRecipe> getType() {
		return RecipeType.SMELTING;
	}

	@Override public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
		return net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC; // как SmeltingRecipe при category()=MISC (SmeltingRecipe.java:46)
	}
}
