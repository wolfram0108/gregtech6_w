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
 * F11-smelting (BUG-023) — этим классом закрыт прежний долг {@link FurnaceRecipes}: ЕДИНСТВЕННАЯ точка входа
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
 * <p>XP печи в neo — поле рецепта ({@code experience()}), не функция входа. Решение (б′), 2026-07-30:
 * диспетчеров несколько — ЭКЗЕМПЛЯР НА КЛАСС ОПЫТА (поле {@code xp} в json), {@code matches} экземпляра
 * пускает только записи, чей опыт по правилу 1.7.10 ({@code FurnaceRecipes.func_151398_b}: хук
 * предмета-результата перекрывает карту) равен его классу — печь сама находит диспетчер с верным
 * {@code experience()}. Экземпляр xp=0 — ДЕФОЛТ: берёт и классы, не покрытые ни одним json
 * ({@code KNOWN_XP}), с предупреждением в лог (экзотический XP стороннего вызова add_smelting → плавка
 * работает, опыт 0 — как до (б′)). Приём, реестр, ленивость и мутабельность — без изменений.</p>
 *
 * <p>Отклонение-форс движка (видимо): коптильня/домна ({@code SmokerRecipes}/{@code BlastFurnaceRecipes})
 * — ветка {@code MD.EtFu} (не загружен) = 1:1 поведение 1.7.10 без EtFu; их проброс — при F10-compat.</p>
 */
public final class GT6SmeltingDispatcher extends AbstractCookingRecipe {
	public static final MapCodec<GT6SmeltingDispatcher> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
		com.mojang.serialization.Codec.FLOAT.optionalFieldOf("xp", 0.0F).forGetter(AbstractCookingRecipe::experience)
	).apply(i, GT6SmeltingDispatcher::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GT6SmeltingDispatcher> STREAM_CODEC = new StreamCodec<>() {
		@Override public GT6SmeltingDispatcher decode(RegistryFriendlyByteBuf aBuf) {return new GT6SmeltingDispatcher(aBuf.readFloat());}
		@Override public void encode(RegistryFriendlyByteBuf aBuf, GT6SmeltingDispatcher aRecipe) {aBuf.writeFloat(aRecipe.experience());}
	};

	public static final RecipeSerializer<GT6SmeltingDispatcher> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	/** Классы опыта, покрытые СВОИМ экземпляром (json с xp > 0); наполняется конструкторами при парсе
	 *  датапака (обе стороны парсят свои копии), дефолт-экземпляр (xp=0) берёт всё непокрытое. */
	private static final java.util.Set<Float> KNOWN_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** Один warning на незнакомое значение, не флуд. */
	private static final java.util.Set<Float> WARNED_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public GT6SmeltingDispatcher() {this(0.0F);}

	public GT6SmeltingDispatcher(float aExperience) {
		// cookingTime 200 = ванильная печь 1.7.10 (фиксированные 200 тиков на плавку); XP — класс опыта экземпляра.
		// result-шаблон — только витрина (AIR запрещён движком: «Item must be non-empty»); реальный выход всегда из assemble.
		// ingredient-плейсхолдер: реальная витрина — ЛЕНИВЫЙ override input() ниже (конструктор зовётся при парсе
		// датапака ДО GT6 data-init (runDeferredItemInit на server-start) — реестр плавок в этот момент ещё пуст).
		super(new Recipe.CommonInfo(F), new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""), Ingredient.of(Items.BARRIER), new ItemStackTemplate(Items.FURNACE), aExperience, 200);
		if (aExperience > 0) KNOWN_XP.add(aExperience);
	}

	/** Ингредиент-витрина из ключей GT6-реестра, собирается НА КАЖДЫЙ запрос (ревизия захода №4 п.4: без
	 *  кэш-эвристики — вызовы редки: propertySet-extractor {@code RecipeManager.forSingleInput:256} на reload,
	 *  display книги; placementInfo кэшируется движком). Отклонение-форс движка: {@code RecipePropertySet}
	 *  (shift-click-гейт) собирается на reload рецептов, который на ПЕРВОМ старте идёт до data-init → гейт слеп до
	 *  следующей пересборки рецептов; ручная укладка в печь и сама плавка ({@code matches} live-lookup'ом) работают всегда. */
	@Override public Ingredient input() {
		java.util.LinkedHashSet<Item> tItems = new java.util.LinkedHashSet<>();
		for (ItemStack tKey : FurnaceRecipes.smelting().getSmeltingList().keySet()) if (!tKey.isEmpty()) tItems.add(tKey.getItem());
		if (tItems.isEmpty()) {
			ERR.println("[GT6] GT6SmeltingDispatcher: реестр FurnaceRecipes пуст при запросе ингредиент-витрины (до data-init — штатно на первом reload)");
			return Ingredient.of(Items.BARRIER);
		}
		return Ingredient.of(tItems.stream());
	}

	@Override public boolean matches(SingleRecipeInput aInput, Level aLevel) {
		ItemStack tResult = FurnaceRecipes.smelting().getSmeltingResult(aInput.item());
		if (!ST.valid(tResult)) return F;
		// класс опыта записи — по правилу 1.7.10 (хук результата перекрывает карту); каждый экземпляр берёт
		// только СВОЙ класс, дефолт (xp=0) — нулевой и все непокрытые json'ами (экзотика → 0 + один warning)
		float tXP = FurnaceRecipes.smelting().func_151398_b(tResult);
		float tMine = experience();
		if (tMine > 0) return tXP == tMine;
		if (tXP != 0.0F && !KNOWN_XP.contains(tXP) && WARNED_XP.add(tXP)) ERR.println("[GT6] GT6SmeltingDispatcher: класс опыта " + tXP + " не покрыт экземпляром (json) — плавка работает, опыт выдаётся 0");
		return tXP == 0.0F || !KNOWN_XP.contains(tXP);
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
		return net.minecraft.client.RecipeBookCategories.FURNACE_MISC; // как SmeltingRecipe при category()=MISC (SmeltingRecipe.java:46)
	}
}
