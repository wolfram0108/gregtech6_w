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

import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Typed engine dispatcher; recipe data, experience and showcase belong to FurnaceRecipes. */
public final class GT6BlastingDispatcher extends BlastingRecipe {
	public static final RecipeSerializer<GT6BlastingDispatcher> SERIALIZER = new RecipeSerializer<>() {
		@Override public GT6BlastingDispatcher fromJson(net.minecraft.resources.ResourceLocation aID, com.google.gson.JsonObject aJSON) {return new GT6BlastingDispatcher(aID, net.minecraft.util.GsonHelper.getAsFloat(aJSON, "xp", 0));}
		@Override public GT6BlastingDispatcher fromNetwork(net.minecraft.resources.ResourceLocation aID, net.minecraft.network.FriendlyByteBuf aBuf) {return new GT6BlastingDispatcher(aID, aBuf.readFloat());}
		@Override public void toNetwork(net.minecraft.network.FriendlyByteBuf aBuf, GT6BlastingDispatcher aRecipe) {aBuf.writeFloat(aRecipe.getExperience());}
	};
	private final FurnaceRecipes.Dispatch mDispatch;
	private final ThreadLocal<ItemStack> mMatchedResult = new ThreadLocal<>();
	public GT6BlastingDispatcher(net.minecraft.resources.ResourceLocation aID, float aXP) {
		super(aID, "", CookingBookCategory.MISC, Ingredient.of(Items.BARRIER), new ItemStack(Items.BLAST_FURNACE), aXP, 100);
		mDispatch = FurnaceRecipes.blasting().new Dispatch(aXP);
	}
	@Override public boolean matches(net.minecraft.world.Container aInput, Level aLevel) {
		ItemStack tResult = mDispatch.assemble(aInput.getItem(0));
		if (ST.valid(tResult)) mMatchedResult.set(tResult); else mMatchedResult.remove();
		return ST.valid(tResult);
	}
	@Override public ItemStack assemble(net.minecraft.world.Container aInput, net.minecraft.core.RegistryAccess aRegistries) {return mDispatch.assemble(aInput.getItem(0));}
	@Override public ItemStack getResultItem(net.minecraft.core.RegistryAccess aRegistries) {
		ItemStack tResult = mMatchedResult.get();
		return ST.valid(tResult) ? ST.copy(tResult) : super.getResultItem(aRegistries);
	}
	@Override public boolean isSpecial() {return true;}
	@Override public net.minecraft.core.NonNullList<Ingredient> getIngredients() {
		net.minecraft.core.NonNullList<Ingredient> rInputs = net.minecraft.core.NonNullList.create();
		java.util.List<ItemStack> tInputs = mDispatch.showcase();
		if (!tInputs.isEmpty()) rInputs.add(Ingredient.of(tInputs.stream()));
		return rInputs;
	}
	@Override public RecipeSerializer<?> getSerializer() {return SERIALIZER;}
}
