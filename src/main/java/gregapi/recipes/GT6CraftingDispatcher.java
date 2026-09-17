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

import gregapi.data.CS;
import gregapi.data.MD;
import gregapi.util.CR;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/** @author Gregorius Techneticies
 *  The engine fills RecipeManager only from datapack JSON now (runtime add is gone), so this CustomRecipe is the only
 *  entry point for GT6's procedural crafting: it stores nothing itself, just iterates and executes GT6's own recipe buffer. */
public final class GT6CraftingDispatcher extends CustomRecipe {
	public static final SimpleCraftingRecipeSerializer<GT6CraftingDispatcher> SERIALIZER = new SimpleCraftingRecipeSerializer<>(GT6CraftingDispatcher::new);

	/** The only place GT6 registers recipe serializers. */
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MD.GAPI.mID);

	static {
		SERIALIZERS.register("gt6_crafting_dispatcher", () -> SERIALIZER);
		SERIALIZERS.register("gt6_smelting_dispatcher", () -> GT6SmeltingDispatcher.SERIALIZER); // The furnace shares this same central registry point.
	}

	/** The subscription point on the mod bus, called from the central GT_API constructor. */
	public static void register(IEventBus aModBus) {
		SERIALIZERS.register(aModBus);
	}

	public GT6CraftingDispatcher(ResourceLocation aID, CraftingBookCategory aCategory) {
		super(aID, aCategory);
	}

	@Override
	public boolean matches(CraftingContainer aGrid, Level aLevel) {
		List<ICraftingRecipeGT> tList = CR.list();
		for (int i = 0, j = tList.size(); i < j; i++) {
			ICraftingRecipeGT tRecipe = tList.get(i);
			if (tRecipe != null && tRecipe.matches(aGrid, aLevel)) return T;
		}
		return F;
	}

	@Override
	public ItemStack assemble(CraftingContainer aGrid, RegistryAccess aRegistries) {
		// assemble is never given a Level, and no ICraftingRecipeGT.matches dereferences one (they only read the grid),
		// so the dummy world stands in here, the same as elsewhere in this file.
		List<ICraftingRecipeGT> tList = CR.list();
		for (int i = 0, j = tList.size(); i < j; i++) {
			ICraftingRecipeGT tRecipe = tList.get(i);
			// The enchant registry is static on this engine version and the bug class this used to guard against doesn't exist
			// anymore, so the method is now an identity -- kept only to preserve the call site.
			if (tRecipe != null && tRecipe.matches(aGrid, CS.DW)) return gregapi.util.UT.NBT.refreshEnchantments(tRecipe.getCraftingResult(aGrid));
		}
		return ItemStack.EMPTY;
	}

	/** The buffer carries recipes of any grid size; filtering by size is each recipe's own job inside matches. */
	@Override
	public boolean canCraftInDimensions(int aWidth, int aHeight) {
		return aWidth * aHeight >= 1;
	}

	// 1.7.10's crafting leftovers ran through a per-item Forge channel that GT6 tools implemented for their own worn-copy
	// behavior; the vanilla default doesn't know it, so this bridges GT6 items through the shared ST.containerItemGT center.
	@Override
	public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingContainer aGrid) {
		net.minecraft.core.NonNullList<ItemStack> rRemaining = net.minecraft.core.NonNullList.withSize(aGrid.getContainerSize(), ItemStack.EMPTY);
		for (int i = 0; i < aGrid.getContainerSize(); i++) {
			ItemStack tStack = aGrid.getItem(i);
			if (tStack.isEmpty()) continue;
			ItemStack tRemainder = gregapi.util.ST.containerItemGT(tStack);
			if (tRemainder != null) {
				if (gregapi.util.ST.valid(tRemainder)) rRemaining.set(i, tRemainder);
			} else if (tStack.hasCraftingRemainingItem()) {
				rRemaining.set(i, tStack.getCraftingRemainingItem());
			}
		}
		return rRemaining;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
