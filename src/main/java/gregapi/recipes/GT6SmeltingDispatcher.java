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

/** @author Gregorius Techneticies
 *  The only entry point for GT6's procedural smelts into the vanilla furnace: several dispatcher instances exist, one
 *  per XP class, since XP is a recipe field here, not a function of input, and each only matches entries of its own class. */
public final class GT6SmeltingDispatcher extends SmeltingRecipe {
	/** The serializer reads only the json field xp (this instance's experience class); vanilla's ingredient/result json
	 *  format doesn't apply, since both input and output here are dynamic. */
	public static final RecipeSerializer<GT6SmeltingDispatcher> SERIALIZER = new RecipeSerializer<>() {
		@Override public GT6SmeltingDispatcher fromJson(ResourceLocation aID, JsonObject aJSON) {return new GT6SmeltingDispatcher(aID, GsonHelper.getAsFloat(aJSON, "xp", 0.0F));}
		@Override public GT6SmeltingDispatcher fromNetwork(ResourceLocation aID, FriendlyByteBuf aBuf) {return new GT6SmeltingDispatcher(aID, aBuf.readFloat());}
		@Override public void toNetwork(FriendlyByteBuf aBuf, GT6SmeltingDispatcher aRecipe) {aBuf.writeFloat(aRecipe.getExperience());}
	};

	/** Experience classes already covered by their own dispatcher instance; anything uncovered falls to the zero-xp default. */
	private static final java.util.Set<Float> KNOWN_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** One warning per unfamiliar value, not a flood of them. */
	private static final java.util.Set<Float> WARNED_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();

	/** A lut function reads the smelting result with no container at all, so the input matches() saw a moment earlier
	 *  is remembered here for it; the furnace itself skips this path, since it calls assemble with a real container. */
	private static final ThreadLocal<ItemStack> MATCHED_RESULT = new ThreadLocal<>();

	public GT6SmeltingDispatcher(ResourceLocation aID, float aExperience) {
		// cookingTime 200 matches 1.7.10's own fixed furnace time; the ingredient/result fields here are display-only
		// placeholders, since real input/output are judged by matches/assemble against GT6's still-empty registry.
		super(aID, "", CookingBookCategory.MISC, Ingredient.of(Items.BARRIER), new ItemStack(Items.FURNACE), aExperience, 200);
		if (aExperience > 0) KNOWN_XP.add(aExperience);
	}

	/** Built fresh from GT6's registry keys on every request, since callers (recipe book, JEI) are rare. */
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
		// The experience class follows 1.7.10's own rule (the result item's hook overrides the map); each instance takes
		// only its own class, and the zero-xp default absorbs anything no json covers.
		float tXP = FurnaceRecipes.smelting().func_151398_b(tResult);
		float tMine = getExperience();
		boolean tMatch;
		if (tMine > 0) {
			tMatch = (tXP == tMine);
		} else {
			if (tXP != 0.0F && !KNOWN_XP.contains(tXP) && WARNED_XP.add(tXP)) ERR.println("[GT6] GT6SmeltingDispatcher: класс опыта " + tXP + " не покрыт экземпляром (json) — плавка работает, опыт выдаётся 0");
			tMatch = (tXP == 0.0F || !KNOWN_XP.contains(tXP));
		}
		// The engine only hands over the input here, so the matched output is remembered for getResultItem to read next.
		if (tMatch) MATCHED_RESULT.set(tResult); else MATCHED_RESULT.remove();
		return tMatch;
	}

	/** The live output, for the one consumer on this engine version that reads a result with no container at all;
	 *  with no live match on this thread it falls back to the parent's placeholder, as before. */
	@Override public ItemStack getResultItem(RegistryAccess aRegistries) {
		ItemStack tResult = MATCHED_RESULT.get();
		return ST.valid(tResult) ? ST.copy(tResult) : super.getResultItem(aRegistries);
	}

	@Override public ItemStack assemble(Container aContainer, RegistryAccess aRegistries) {
		// The registry holds a live result stack; only a copy ever leaves it, since the furnace mutates its result in place.
		ItemStack tResult = FurnaceRecipes.smelting().getSmeltingResult(aContainer.getItem(0));
		return ST.valid(tResult) ? ST.copy(tResult) : ItemStack.EMPTY;
	}

	// Not overridden: the SmeltingRecipe parent already returns the same furnace icon, duplicating it would be pointless.

	@Override public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
