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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A DISPLAY-ONLY crafting recipe: GT6's own buffer (F11) laid out so JEI's native crafting category and foreign
 * crafting integrations (AE2's pattern encoder) can read it as an ordinary crafting recipe. It never enters the
 * RecipeManager and never crafts: {@link #matches} is always false, the mechanic stays with GT6CraftingDispatcher
 * and CR.matching. Cells are kept as GT6's own lists, so the 70+ thousand recipes cost a wrapper each until a card
 * is actually displayed or encoded.
 */
public final class GT6CraftingDisplay extends NormalCraftingRecipe {
	/** One cell = the stacks a slot may show (1:1 with GT6's ItemStack / List<ItemStack> cell). */
	private final List<List<ItemStack>> mCells;
	private final ItemStack mResult;
	private final boolean mShapeless;
	private final int mWidth, mHeight;

	public GT6CraftingDisplay(boolean aShapeless, int aWidth, int aHeight, List<List<ItemStack>> aCells, ItemStack aResult) {
		super(new Recipe.CommonInfo(false), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""));
		mShapeless = aShapeless;
		mWidth = aWidth;
		mHeight = aHeight;
		mCells = aCells;
		mResult = aResult;
	}

	@Override public RecipeSerializer<? extends NormalCraftingRecipe> getSerializer() {return SERIALIZER;}

	/** Display only: the workbench keeps answering through CR.matching, never through a recipe manager entry. */
	@Override public boolean matches(CraftingInput aInput, Level aLevel) {return false;}

	@Override public ItemStack assemble(CraftingInput aInput) {return ItemStack.EMPTY;}

	@Override protected PlacementInfo createPlacementInfo() {
		// Nine slots always, so AE2's own 3x3 expansion copies cell for cell instead of shifting a narrow grid.
		List<Optional<Ingredient>> tSlots = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) tSlots.add(Optional.empty());
		if (mShapeless) {
			for (int i = 0; i < Math.min(9, mCells.size()); i++) tSlots.set(i, ingredient(mCells.get(i)));
		} else {
			for (int y = 0; y < Math.min(mHeight, 3); y++) for (int x = 0; x < Math.min(mWidth, 3); x++) {
				int tSource = x + y * mWidth;
				if (tSource < mCells.size()) tSlots.set(x + y * 3, ingredient(mCells.get(tSource)));
			}
		}
		return PlacementInfo.createFromOptionals(tSlots);
	}

	@Override public List<RecipeDisplay> display() {
		List<SlotDisplay> tIngredients = new ArrayList<>(mCells.size());
		for (List<ItemStack> tCell : mCells) tIngredients.add(display(tCell));
		SlotDisplay tResult = stackDisplay(mResult);
		SlotDisplay tStation = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
		if (mShapeless) return List.of(new ShapelessCraftingRecipeDisplay(tIngredients, tResult, tStation));
		return List.of(new ShapedCraftingRecipeDisplay(mWidth, mHeight, tIngredients, tResult, tStation));
	}

	private static Optional<Ingredient> ingredient(List<ItemStack> aCell) {
		return aCell.isEmpty() ? Optional.empty() : Optional.of(new FurnaceRecipes.RuntimeIngredient(aCell).toVanilla());
	}

	private static SlotDisplay display(List<ItemStack> aCell) {
		if (aCell.isEmpty()) return SlotDisplay.Empty.INSTANCE;
		if (aCell.size() == 1) return stackDisplay(aCell.get(0));
		List<SlotDisplay> tAlternatives = new ArrayList<>(aCell.size());
		for (ItemStack tStack : aCell) tAlternatives.add(stackDisplay(tStack));
		return new SlotDisplay.Composite(tAlternatives);
	}

	private static SlotDisplay stackDisplay(ItemStack aStack) {
		return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(aStack));
	}

	public static final MapCodec<GT6CraftingDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.BOOL.fieldOf("shapeless").forGetter(o -> o.mShapeless),
		Codec.INT.fieldOf("width").forGetter(o -> o.mWidth),
		Codec.INT.fieldOf("height").forGetter(o -> o.mHeight),
		ItemStack.CODEC.listOf().listOf().fieldOf("cells").forGetter(o -> o.mCells),
		ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> ItemStackTemplate.fromNonEmptyStack(o.mResult))
	).apply(i, (aShapeless, aWidth, aHeight, aCells, aResult) -> new GT6CraftingDisplay(aShapeless, aWidth, aHeight, aCells, aResult.create())));

	public static final StreamCodec<RegistryFriendlyByteBuf, GT6CraftingDisplay> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, o -> o.mShapeless,
		ByteBufCodecs.VAR_INT, o -> o.mWidth,
		ByteBufCodecs.VAR_INT, o -> o.mHeight,
		ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), o -> o.mCells,
		ItemStackTemplate.STREAM_CODEC, o -> ItemStackTemplate.fromNonEmptyStack(o.mResult),
		(aShapeless, aWidth, aHeight, aCells, aResult) -> new GT6CraftingDisplay(aShapeless, aWidth, aHeight, aCells, aResult.create()));

	public static final RecipeSerializer<GT6CraftingDisplay> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
}
