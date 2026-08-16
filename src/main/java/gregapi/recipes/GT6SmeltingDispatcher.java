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

import com.google.gson.JsonObject;
import gregapi.util.ST;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

import static gregapi.data.CS.ERR;
import static gregapi.data.CS.F;

/**
 * @author Gregorius Techneticies
 *
 * F11-smelting (BUG-023) — этим классом закрыт прежний долг {@link FurnaceRecipes}: ЕДИНСТВЕННАЯ точка входа
 * GT6-плавок в ванильную печь. Тот же приём, что {@link GT6CraftingDispatcher} (верстак): движок наполняет
 * {@code RecipeManager} только из датапак-JSON, рантайм-add удалён — GT6 же добавляет/удаляет плавки
 * процедурно ({@code RM.add_smelting} → {@link FurnaceRecipes}, мутабельный 1:1-реестр). ОДИН диспетчер типа
 * {@code RecipeType.SMELTING} перебирает GT6-реестр в {@code matches}/{@code assemble} — печь
 * ({@code AbstractFurnaceBlockEntity.serverTick:249} через {@code quickCheck.getRecipeFor}) находит и
 * исполняет GT6-плавки штатно.
 *
 * <p>Контракт 1.20.1: {@code Recipe<Container>} — печь отдаёт СЕБЯ как контейнер, вход берётся из слота 0
 * (forge-1201-decompiled {@code AbstractCookingRecipe.java:31-33}, {@code AbstractFurnaceBlockEntity.java:305});
 * {@code assemble} берёт {@code RegistryAccess}; id носит сам рецепт ({@code AbstractCookingRecipe.java:70}).
 * Shift-click-гейт меню печи в 1.20.1 — ЖИВОЙ запрос ({@code AbstractFurnaceMenu.canSmelt:145-146}
 * → {@code getRecipeFor} → {@code matches}), статического сета входов, как в 26.x, нет: слепоты гейта не
 * возникает. Ингредиент-витрина ({@link #getIngredients}) остаётся только для книги рецептов и JEI и
 * собирается ЖИВЬЁМ из GT6-реестра.</p>
 *
 * <p>XP печи — поле рецепта, не функция входа. Решение (б′), 2026-07-30: диспетчеров несколько —
 * ЭКЗЕМПЛЯР НА КЛАСС ОПЫТА (поле {@code xp} в json), {@code matches} экземпляра пускает только записи, чей
 * опыт по правилу 1.7.10 ({@code FurnaceRecipes.func_151398_b}: хук предмета-результата перекрывает карту)
 * равен его классу — печь сама находит диспетчер с верным {@code getExperience()}. Экземпляр xp=0 — ДЕФОЛТ:
 * берёт и классы, не покрытые ни одним json ({@code KNOWN_XP}), с предупреждением в лог.</p>
 *
 * <p>Отклонение-форс движка (видимо): коптильня/домна ({@code SmokerRecipes}/{@code BlastFurnaceRecipes})
 * — ветка {@code MD.EtFu} (не загружен) = 1:1 поведение 1.7.10 без EtFu; их проброс — при F10-compat.</p>
 *
 * <p><b>BP-BUG-010, контракт типа.</b> Предок — именно {@link SmeltingRecipe}, не {@code AbstractCookingRecipe}:
 * в 1.20.1 ванильный {@code RecipeType} ТИПИЗИРОВАН конкретным классом ({@code RecipeType.java:9}
 * — {@code RecipeType<SmeltingRecipe> SMELTING}), поэтому любой потребитель, берущий рецепт по этому типу
 * через generic, получает от компилятора checkcast к {@code SmeltingRecipe}. Потребитель, ронявший сервер, —
 * лут-функция «выплавить дроп» ({@code SmeltItemFunction.java:30-32}, животное сгорело в лаве):
 * {@code Optional<SmeltingRecipe> = getRecipeFor(RecipeType.SMELTING, …)} → {@code ClassCastException}.
 * Второе плечо ТОГО ЖЕ контракта — {@link #getResultItem}: та же лут-функция читает выход БЕЗ контейнера
 * ({@code SmeltItemFunction.java:32}), тогда как выход диспетчера вычисляется по входу; мост — ниже.</p>
 */
public final class GT6SmeltingDispatcher extends SmeltingRecipe {
	/** Сериализатор читает единственное поле json — {@code xp} (класс опыта экземпляра). Формат ванильного
	 *  smelting-json (ингредиент/результат/время) здесь неприменим: и вход, и выход динамические. */
	public static final RecipeSerializer<GT6SmeltingDispatcher> SERIALIZER = new RecipeSerializer<>() {
		@Override public GT6SmeltingDispatcher fromJson(ResourceLocation aID, JsonObject aJSON) {return new GT6SmeltingDispatcher(aID, GsonHelper.getAsFloat(aJSON, "xp", 0.0F));}
		@Override public GT6SmeltingDispatcher fromNetwork(ResourceLocation aID, FriendlyByteBuf aBuf) {return new GT6SmeltingDispatcher(aID, aBuf.readFloat());}
		@Override public void toNetwork(FriendlyByteBuf aBuf, GT6SmeltingDispatcher aRecipe) {aBuf.writeFloat(aRecipe.getExperience());}
	};

	/** Классы опыта, покрытые СВОИМ экземпляром (json с xp > 0); наполняется конструкторами при парсе
	 *  датапака (обе стороны парсят свои копии), дефолт-экземпляр (xp=0) берёт всё непокрытое. */
	private static final java.util.Set<Float> KNOWN_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** Один warning на незнакомое значение, не флуд. */
	private static final java.util.Set<Float> WARNED_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();

	/** BP-BUG-010, второе плечо контракта типа. Выход, признанный {@link #matches} на ЭТОМ потоке — мост к
	 *  контекстно-независимому {@link #getResultItem}. В 1.20.1 лут-функция «выплавить дроп» берёт выход
	 *  БЕЗ контейнера ({@code SmeltItemFunction.java:32}), но вход отдаёт нам сам движок мгновением раньше и
	 *  на том же потоке: {@code RecipeManager.getRecipeFor:91-95} фильтрует поток вызовом
	 *  {@code matches(container, level)} и лишь затем читает выход у найденного рецепта. Поэтому вход,
	 *  признанный matches, запоминается на поток и отдаётся следующему запросу выхода.
	 *  Печь этим мостом не пользуется — она берёт выход через {@code assemble(this, registryAccess)}
	 *  ({@code AbstractFurnaceBlockEntity.java:305,328}), с контейнером.
	 *  На main (26.1) шва нет: там та же лут-функция зовёт {@code assemble(input)}
	 *  (neo-decompiled {@code SmeltItemFunction.java:41}). */
	private static final ThreadLocal<ItemStack> MATCHED_RESULT = new ThreadLocal<>();

	public GT6SmeltingDispatcher(ResourceLocation aID, float aExperience) {
		// cookingTime 200 = ванильная печь 1.7.10 (фиксированные 200 тиков на плавку); XP — класс опыта экземпляра.
		// ingredient/result здесь — плейсхолдеры-витрина: реальный вход судится matches, реальный выход даёт
		// assemble (оба живут в мутабельном реестре GT6, который на момент парса датапака ещё пуст).
		// Тип рецепта задаёт сам предок (SmeltingRecipe.java:9 → RecipeType.SMELTING) — это и есть контракт типа.
		super(aID, "", CookingBookCategory.MISC, Ingredient.of(Items.BARRIER), new ItemStack(Items.FURNACE), aExperience, 200);
		if (aExperience > 0) KNOWN_XP.add(aExperience);
	}

	/** Ингредиент-витрина из ключей GT6-реестра, собирается НА КАЖДЫЙ запрос (вызовы редки: книга рецептов, JEI). */
	@Override public NonNullList<Ingredient> getIngredients() {
		java.util.LinkedHashSet<ItemStack> tItems = new java.util.LinkedHashSet<>();
		for (ItemStack tKey : FurnaceRecipes.smelting().getSmeltingList().keySet()) if (!tKey.isEmpty()) tItems.add(ST.amount(1, tKey));
		NonNullList<Ingredient> rList = NonNullList.create();
		if (tItems.isEmpty()) {
			ERR.println("[GT6] GT6SmeltingDispatcher: реестр FurnaceRecipes пуст при запросе ингредиент-витрины (до data-init — штатно на первом reload)");
			rList.add(Ingredient.of(Items.BARRIER));
		} else {
			rList.add(Ingredient.of(tItems.stream()));
		}
		return rList;
	}

	@Override public boolean matches(Container aContainer, Level aLevel) {
		ItemStack tResult = FurnaceRecipes.smelting().getSmeltingResult(aContainer.getItem(0));
		if (!ST.valid(tResult)) {MATCHED_RESULT.remove(); return F;}
		// класс опыта записи — по правилу 1.7.10 (хук результата перекрывает карту); каждый экземпляр берёт
		// только СВОЙ класс, дефолт (xp=0) — нулевой и все непокрытые json'ами (экзотика → 0 + один warning)
		float tXP = FurnaceRecipes.smelting().func_151398_b(tResult);
		float tMine = getExperience();
		boolean tMatch;
		if (tMine > 0) {
			tMatch = (tXP == tMine);
		} else {
			if (tXP != 0.0F && !KNOWN_XP.contains(tXP) && WARNED_XP.add(tXP)) ERR.println("[GT6] GT6SmeltingDispatcher: класс опыта " + tXP + " не покрыт экземпляром (json) — плавка работает, опыт выдаётся 0");
			tMatch = (tXP == 0.0F || !KNOWN_XP.contains(tXP));
		}
		// BP-BUG-010: вход отдан движком только здесь — запоминаем выход для getResultItem (см. MATCHED_RESULT)
		if (tMatch) MATCHED_RESULT.set(tResult); else MATCHED_RESULT.remove();
		return tMatch;
	}

	/** BP-BUG-010: живой выход для потребителей 1.20.1, читающих его БЕЗ контейнера — лут-функция «выплавить
	 *  дроп» ({@code SmeltItemFunction.java:32}). Нет живого совпадения на потоке (книга рецептов, витрина) —
	 *  отдаём плейсхолдер-витрину предка, как было. */
	@Override public ItemStack getResultItem(RegistryAccess aRegistries) {
		ItemStack tResult = MATCHED_RESULT.get();
		return ST.valid(tResult) ? ST.copy(tResult) : super.getResultItem(aRegistries);
	}

	@Override public ItemStack assemble(Container aContainer, RegistryAccess aRegistries) {
		// реестр хранит ЖИВОЙ выход-стек — наружу только копия (печь мутирует результат при burn/стаковке)
		ItemStack tResult = FurnaceRecipes.smelting().getSmeltingResult(aContainer.getItem(0));
		return ST.valid(tResult) ? ST.copy(tResult) : ItemStack.EMPTY;
	}

	// getToastSymbol не переопределяется: предок SmeltingRecipe отдаёт ровно тот же значок печи
	// (forge-1201-decompiled SmeltingRecipe.java:13-14) — дубля детали не заводим.

	@Override public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
