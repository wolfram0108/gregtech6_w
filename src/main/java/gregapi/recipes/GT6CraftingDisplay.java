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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * A DISPLAY-ONLY crafting recipe: GT6's own buffer (F11) laid out so JEI's native crafting category and foreign
 * crafting integrations (AE2's pattern encoder) can read it as an ordinary crafting recipe. It never enters the
 * RecipeManager and never crafts: {@link #matches} is always false, the mechanic stays with GT6CraftingDispatcher
 * and CR.matching. Cells are kept as GT6's own lists, so the 70+ thousand recipes cost a wrapper each until a card
 * is actually displayed or encoded.
 */
public final class GT6CraftingDisplay implements CraftingRecipe, IShapedRecipe<CraftingContainer> {
	/** One cell = the stacks a slot may show (1:1 with GT6's ItemStack / List<ItemStack> cell). */
	private final List<List<ItemStack>> mCells;
	private final ItemStack mResult;
	private final ResourceLocation mId;
	private final boolean mShapeless;
	private final int mWidth, mHeight;

	public GT6CraftingDisplay(ResourceLocation aId, boolean aShapeless, int aWidth, int aHeight, List<List<ItemStack>> aCells, ItemStack aResult) {
		mId = aId;
		mShapeless = aShapeless;
		mWidth = aWidth;
		mHeight = aHeight;
		mCells = aCells;
		mResult = aResult;
	}

	@Override public ResourceLocation getId() {return mId;}
	@Override public RecipeSerializer<?> getSerializer() {return SERIALIZER;}
	@Override public CraftingBookCategory category() {return CraftingBookCategory.MISC;}

	/** Display only: the workbench keeps answering through CR.matching, never through a recipe manager entry. */
	@Override public boolean matches(CraftingContainer aInput, Level aLevel) {return false;}
	@Override public ItemStack assemble(CraftingContainer aInput, RegistryAccess aRegistries) {return ItemStack.EMPTY;}
	@Override public ItemStack getResultItem(RegistryAccess aRegistries) {return mResult;}
	@Override public boolean canCraftInDimensions(int aWidth, int aHeight) {return mShapeless ? aWidth * aHeight >= mCells.size() : aWidth >= mWidth && aHeight >= mHeight;}

	@Override public int getRecipeWidth() {return mWidth;}
	@Override public int getRecipeHeight() {return mHeight;}

	@Override public NonNullList<Ingredient> getIngredients() {
		NonNullList<Ingredient> rIngredients = NonNullList.create();
		if (mShapeless) {
			for (List<ItemStack> tCell : mCells) if (!tCell.isEmpty()) rIngredients.add(new FurnaceRecipes.RuntimeIngredient(tCell));
			return rIngredients;
		}
		// A shaped recipe's list is the full width x height grid: foreign encoders index it by cell, and an empty
		// cell must still occupy its position.
		for (int i = 0; i < mWidth * mHeight; i++) rIngredients.add(i < mCells.size() && !mCells.get(i).isEmpty() ? new FurnaceRecipes.RuntimeIngredient(mCells.get(i)) : Ingredient.EMPTY);
		return rIngredients;
	}

	public static final RecipeSerializer<GT6CraftingDisplay> SERIALIZER = new RecipeSerializer<>() {
		@Override public GT6CraftingDisplay fromJson(ResourceLocation aId, JsonObject aJson) {
			boolean tShapeless = aJson.get("shapeless").getAsBoolean();
			int tWidth = aJson.get("width").getAsInt(), tHeight = aJson.get("height").getAsInt();
			List<List<ItemStack>> tCells = new ArrayList<>();
			for (JsonElement tCell : aJson.getAsJsonArray("cells")) {
				List<ItemStack> tStacks = new ArrayList<>();
				for (JsonElement tStack : tCell.getAsJsonArray()) tStacks.add(ItemStack.CODEC.parse(JsonOps.INSTANCE, tStack).getOrThrow(false, s -> {}));
				tCells.add(tStacks);
			}
			ItemStack tResult = ItemStack.CODEC.parse(JsonOps.INSTANCE, aJson.get("result")).getOrThrow(false, s -> {});
			return new GT6CraftingDisplay(aId, tShapeless, tWidth, tHeight, tCells, tResult);
		}

		@Override public GT6CraftingDisplay fromNetwork(ResourceLocation aId, FriendlyByteBuf aBuf) {
			boolean tShapeless = aBuf.readBoolean();
			int tWidth = aBuf.readVarInt(), tHeight = aBuf.readVarInt(), tCellCount = aBuf.readVarInt();
			List<List<ItemStack>> tCells = new ArrayList<>(tCellCount);
			for (int i = 0; i < tCellCount; i++) {
				int tSize = aBuf.readVarInt();
				List<ItemStack> tStacks = new ArrayList<>(tSize);
				for (int j = 0; j < tSize; j++) tStacks.add(aBuf.readItem());
				tCells.add(tStacks);
			}
			return new GT6CraftingDisplay(aId, tShapeless, tWidth, tHeight, tCells, aBuf.readItem());
		}

		@Override public void toNetwork(FriendlyByteBuf aBuf, GT6CraftingDisplay aRecipe) {
			aBuf.writeBoolean(aRecipe.mShapeless);
			aBuf.writeVarInt(aRecipe.mWidth);
			aBuf.writeVarInt(aRecipe.mHeight);
			aBuf.writeVarInt(aRecipe.mCells.size());
			for (List<ItemStack> tCell : aRecipe.mCells) {
				aBuf.writeVarInt(tCell.size());
				for (ItemStack tStack : tCell) aBuf.writeItem(tStack);
			}
			aBuf.writeItem(aRecipe.mResult);
		}
	};
}
