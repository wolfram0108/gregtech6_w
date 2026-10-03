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
import gregapi.data.CS;
import gregapi.data.MD;
import gregapi.util.CR;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/** Sole entry point for GT6's procedural crafting recipes into the vanilla workbench, since neo only fills
 *  RecipeManager from datapack JSON; this dispatcher scans GT6's own buffer and runs the matched recipe's logic. */
public final class GT6CraftingDispatcher extends CustomRecipe {
	public static final MapCodec<GT6CraftingDispatcher> CODEC = MapCodec.unit(GT6CraftingDispatcher::new);

	public static final StreamCodec<RegistryFriendlyByteBuf, GT6CraftingDispatcher> STREAM_CODEC = new StreamCodec<>() {
		@Override public GT6CraftingDispatcher decode(RegistryFriendlyByteBuf aBuf) {return new GT6CraftingDispatcher();}
		@Override public void encode(RegistryFriendlyByteBuf aBuf, GT6CraftingDispatcher aRecipe) {/* No data, the same approach AE2's FacadeRecipe uses. */}
	};

	public static final RecipeSerializer<GT6CraftingDispatcher> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	/** Central DeferredRegister, the only place GT6 registers a workbench recipe serializer in neo. */
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(Registries.RECIPE_SERIALIZER, MD.GAPI.mID);

	static {
		SERIALIZERS.register("gt6_crafting_dispatcher", () -> SERIALIZER);
		SERIALIZERS.register("gt6_smelting_dispatcher", () -> GT6SmeltingDispatcher.SERIALIZER); // The furnace shares this same central registry point.
	}

	/** Mod-bus subscription point, mirroring GT6WorldgenFeature's own registration call from the GT_API constructor. */
	public static void register(IEventBus aModBus) {
		SERIALIZERS.register(aModBus);
	}

	// The workbench answers as every other path (CR.matching); where that is a live vanilla recipe the dispatcher stands
	// aside, so the engine's own recipe answers wherever its id sorts against this one.
	@Override
	public boolean matches(CraftingInput aGrid, Level aLevel) {
		ICraftingRecipeGT tRecipe = CR.matching(aGrid, aLevel);
		return tRecipe != null && !(tRecipe instanceof CR.VanillaRecipe);
	}

	@Override
	public ItemStack assemble(CraftingInput aGrid) {
		// Recipe.assemble doesn't receive a Level, and no ported recipe actually reads it, so the shared dummy
		// world stands in for null while finding the grid's answer.
		ICraftingRecipeGT tRecipe = CR.matching(aGrid, CS.DW);
		// A statically-enchanted recipe result's enchantment holders go stale after a world restart, breaking the
		// network codec by identity; refreshing them against the current server's registry fixes every GT6 crafting result.
		return tRecipe == null || tRecipe instanceof CR.VanillaRecipe ? ItemStack.EMPTY : gregapi.util.UT.NBT.refreshEnchantments(tRecipe.getCraftingResult(aGrid));
	}

	// 1.7.10 checked container-item leftovers on the item itself; neo checks per-recipe and knows nothing about
	// GT6's channel, so this bridges through the single shared lookup instead of duplicating it per item root.
	@Override
	public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput aGrid) {
		net.minecraft.core.NonNullList<ItemStack> rRemaining = net.minecraft.core.NonNullList.withSize(aGrid.size(), ItemStack.EMPTY);
		for (int i = 0; i < aGrid.size(); i++) {
			ItemStack tStack = aGrid.getItem(i);
			if (tStack.isEmpty()) continue;
			ItemStack tRemainder = gregapi.util.ST.gtContainerItem(tStack);
			if (tRemainder != null) {
				if (gregapi.util.ST.valid(tRemainder)) rRemaining.set(i, tRemainder);
			} else {
				net.minecraft.world.item.ItemStackTemplate tVanillaRemainder = tStack.getCraftingRemainder();
				if (tVanillaRemainder != null) rRemaining.set(i, tVanillaRemainder.create());
			}
		}
		return rRemaining;
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return SERIALIZER;
	}
}
