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
 * F11-smelting (BUG-023) — this class closes the earlier debt of {@link FurnaceRecipes}: the ONLY entry point of
 * GT6 smelting recipes into neo's vanilla furnace. The same technique as {@link GT6CraftingDispatcher} (crafting
 * table): neo fills {@code RecipeManager} only from datapack JSON, runtime add was removed — while GT6
 * adds/removes smeltings procedurally ({@code RM.add_smelting} -> {@link FurnaceRecipes}, a mutable
 * 1:1 registry). ONE dispatcher of type {@code RecipeType.SMELTING} scans the GT6 registry in
 * {@code matches}/{@code assemble} — the furnace ({@code AbstractFurnaceBlockEntity.serverTick:170} via
 * {@code quickCheck.getRecipeFor}) finds and executes GT6 smeltings normally.
 *
 * <p>The ingredient showcase is built in the constructor from the GT6 registry's keys (by the time recipes load,
 * data-init has already run; the client-side copy of the recipe builds it from the client registry — GT6 fills
 * both sides): its only consumers are {@code RecipePropertySet.FURNACE_INPUT}
 * (the furnace menu's shift-click gate, {@code AbstractFurnaceMenu.canSmelt:141} — a static SET of items,
 * assembled by {@code Ingredient::items} on reload) and the recipe book's display. The actual smelting is judged
 * only by {@code matches} (an exact {@code ST.equal} with wildcard, as in 1.7.10).</p>
 *
 * <p>Furnace XP in neo is a recipe field ({@code experience()}), not an input function. Decision (b'), 2026-07-30:
 * there are several dispatchers — ONE INSTANCE PER XP CLASS (the {@code xp} field in json), an instance's
 * {@code matches} only lets through entries whose experience, by the 1.7.10 rule ({@code FurnaceRecipes.func_151398_b}:
 * the result-item hook overrides the map), equals its own class — the furnace finds the right dispatcher by its
 * {@code experience()} on its own. The xp=0 instance is the DEFAULT: it also takes classes not covered by any json
 * ({@code KNOWN_XP}), with a log warning (an exotic XP from a third-party add_smelting call -> smelting still
 * works, XP is 0 — same as before (b')). The technique, the registry, the laziness and the mutability are unchanged.</p>
 *
 * <p>A visible engine-forced deviation: the smoker/blast furnace ({@code SmokerRecipes}/{@code BlastFurnaceRecipes})
 * — the {@code MD.EtFu} branch (not loaded) = the 1:1 1.7.10 behavior without EtFu; wiring them up is deferred to F10-compat.</p>
 *
 * <p><b>Crash class, type contract.</b> The parent class is specifically {@link net.minecraft.world.item.crafting.SmeltingRecipe},
 * not {@code AbstractCookingRecipe}: {@code RecipeType.SMELTING} is TYPED to a concrete class
 * ({@code RecipeType.java:9} — {@code RecipeType<SmeltingRecipe> SMELTING}), so any consumer that fetches a
 * recipe by this type through a generic gets a compiler-inserted {@code checkcast} to {@code SmeltingRecipe}.
 * The consumer that used to crash the server was the "smelt the drop" loot function ({@code SmeltItemFunction.java:41},
 * an animal that burned in lava): {@code Optional<RecipeHolder<SmeltingRecipe>> = getRecipeFor(RecipeType.SMELTING, ...)} ->
 * a {@code ClassCastException} against the previous parent {@code AbstractCookingRecipe}. A second bridge arm (an
 * output without a container) is not needed here: in 26.1.2 the same loot function reads the output via
 * {@code assemble(input)} with a container ({@code SmeltItemFunction.java:41-43}), not a separate contextless getter as in 1.20.1.</p>
 */
public final class GT6SmeltingDispatcher extends net.minecraft.world.item.crafting.SmeltingRecipe {
	public static final MapCodec<GT6SmeltingDispatcher> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
		com.mojang.serialization.Codec.FLOAT.optionalFieldOf("xp", 0.0F).forGetter(AbstractCookingRecipe::experience)
	).apply(i, GT6SmeltingDispatcher::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GT6SmeltingDispatcher> STREAM_CODEC = new StreamCodec<>() {
		@Override public GT6SmeltingDispatcher decode(RegistryFriendlyByteBuf aBuf) {return new GT6SmeltingDispatcher(aBuf.readFloat());}
		@Override public void encode(RegistryFriendlyByteBuf aBuf, GT6SmeltingDispatcher aRecipe) {aBuf.writeFloat(aRecipe.experience());}
	};

	public static final RecipeSerializer<GT6SmeltingDispatcher> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	/** XP classes covered by their OWN instance (a json with xp > 0); filled by constructors while parsing the
	 *  datapack (both sides parse their own copies), the default instance (xp=0) takes everything uncovered. */
	private static final java.util.Set<Float> KNOWN_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** One warning per unfamiliar value, not a flood. */
	private static final java.util.Set<Float> WARNED_XP = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public GT6SmeltingDispatcher() {this(0.0F);}

	public GT6SmeltingDispatcher(float aExperience) {
		// cookingTime 200 = the 1.7.10 vanilla furnace (a fixed 200 ticks per smelt); XP is the instance's XP class.
		// The result template is display-only (the engine forbids AIR: "Item must be non-empty"); the real output always comes from assemble.
		// The ingredient placeholder: the real showcase is the LAZY input() override below (the constructor is called
		// while parsing the datapack, BEFORE GT6 data-init (runDeferredItemInit on server-start) — the smelting registry is still empty at this point).
		super(new Recipe.CommonInfo(F), new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""), Ingredient.of(Items.BARRIER), new ItemStackTemplate(Items.FURNACE), aExperience, 200);
		if (aExperience > 0) KNOWN_XP.add(aExperience);
	}

	/** The ingredient showcase from the GT6 registry's keys, assembled ON EVERY request (checkpoint #4 item 4
	 *  review: no cache heuristic — calls are rare: the propertySet extractor {@code RecipeManager.forSingleInput:256}
	 *  on reload, the recipe book display; placementInfo is cached by the engine). An engine-forced deviation:
	 *  {@code RecipePropertySet} (the shift-click gate) is assembled on the recipe reload that, on the FIRST start,
	 *  runs before data-init -> the gate stays blind until the next recipe rebuild; manually placing into the furnace
	 *  and the actual smelting (via {@code matches}'s live lookup) always work. */
	@Override public Ingredient input() {
		java.util.LinkedHashSet<Item> tItems = new java.util.LinkedHashSet<>();
		for (ItemStack tKey : FurnaceRecipes.smelting().getSmeltingList().keySet()) if (!tKey.isEmpty()) tItems.add(tKey.getItem());
		if (tItems.isEmpty()) {
			ERR.println("[GT6] GT6SmeltingDispatcher: the FurnaceRecipes registry is empty when the ingredient showcase was requested (before data-init — normal on the first reload)");
			return Ingredient.of(Items.BARRIER);
		}
		return Ingredient.of(tItems.stream());
	}

	@Override public boolean matches(SingleRecipeInput aInput, Level aLevel) {
		ItemStack tResult = FurnaceRecipes.smelting().getSmeltingResult(aInput.item());
		if (!ST.valid(tResult)) return F;
		// the entry's XP class — by the 1.7.10 rule (the result hook overrides the map); each instance only takes
		// ITS OWN class, the default (xp=0) takes zero plus everything not covered by a json (exotic -> 0 + one warning)
		float tXP = FurnaceRecipes.smelting().func_151398_b(tResult);
		float tMine = experience();
		if (tMine > 0) return tXP == tMine;
		if (tXP != 0.0F && !KNOWN_XP.contains(tXP) && WARNED_XP.add(tXP)) ERR.println("[GT6] GT6SmeltingDispatcher: XP class " + tXP + " is not covered by an instance (json) — smelting still works, XP is given as 0");
		return tXP == 0.0F || !KNOWN_XP.contains(tXP);
	}

	@Override public ItemStack assemble(SingleRecipeInput aInput) {
		// the registry holds the LIVE output stack — only a copy ever leaves it (the furnace mutates the result on burn/stacking)
		ItemStack tResult = FurnaceRecipes.smelting().getSmeltingResult(aInput.item());
		return ST.valid(tResult) ? ST.copy(tResult) : ItemStack.EMPTY;
	}

	@Override protected Item furnaceIcon() {
		return Items.FURNACE;
	}

	// Crash class, continued: SmeltingRecipe.getSerializer() (unlike the AbstractCookingRecipe parent) narrows the
	// return type WITHOUT a wildcard — RecipeSerializer<SmeltingRecipe> — generics are invariant, so the override
	// here must return EXACTLY that type (the engine rejects RecipeSerializer<GT6SmeltingDispatcher>: not a subtype).
	// The SERIALIZER field stays RecipeSerializer<GT6SmeltingDispatcher> — the same one the DeferredRegister
	// registration uses (GT6CraftingDispatcher.SERIALIZERS.register); the cast is safe — type erasure, the same
	// object, the recipe's real T remains GT6SmeltingDispatcher (extends SmeltingRecipe), and the codec/stream codec read and write exactly that.
	@SuppressWarnings("unchecked")
	@Override public RecipeSerializer<net.minecraft.world.item.crafting.SmeltingRecipe> getSerializer() {
		return (RecipeSerializer<net.minecraft.world.item.crafting.SmeltingRecipe>)(RecipeSerializer<?>) SERIALIZER;
	}

	@Override public RecipeType<net.minecraft.world.item.crafting.SmeltingRecipe> getType() {
		return RecipeType.SMELTING;
	}

	@Override public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
		return net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC; // matches SmeltingRecipe when category()=MISC (SmeltingRecipe.java:46)
	}
}
