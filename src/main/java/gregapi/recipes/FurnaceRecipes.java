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

import static gregapi.data.CS.*;

import java.util.HashMap;
import java.util.Map;

import gregapi.data.CS;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Vanilla's mutable FurnaceRecipes singleton is gone (smelting is now immutable datapack data), but GT6
 *  still mutates smelting at runtime, so this reproduces the old API 1:1 over GT6's own mutable storage. */
public class FurnaceRecipes {
	private static final FurnaceRecipes INSTANCE = new FurnaceRecipes();
	/** EFR child furnaces: check blacklist, explicit recipe, then fallback to inherited smelting. */
	public enum Channel {SMELTING, SMOKING, BLASTING}
	private static final FurnaceRecipes SMOKING = new FurnaceRecipes(Channel.SMOKING);
	private static final FurnaceRecipes BLASTING = new FurnaceRecipes(Channel.BLASTING);
	private final Channel mChannel;
	private final java.util.List<ItemStack> mBlacklist = new java.util.ArrayList<>();
	private final java.util.List<Map.Entry<ItemStack, ItemStack>> mConditionalRemovals = new java.util.ArrayList<>();
	private final java.util.Set<Float> mKnownXP = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private final java.util.Set<Float> mWarnedXP = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private FurnaceRecipes() {this(Channel.SMELTING);}
	private FurnaceRecipes(Channel aChannel) {mChannel = aChannel;}
	public static FurnaceRecipes smoking() {return SMOKING;}
	public static FurnaceRecipes blasting() {return BLASTING;}
	public Channel channel() {return mChannel;}
	private static final java.util.concurrent.locks.ReentrantLock STATE_LOCK = new java.util.concurrent.locks.ReentrantLock();
	private static int sChangeDepth;
	private static boolean sDirty, sRefreshing;
	public static void beginChange() {AutoNames.ensureInitialized(); STATE_LOCK.lock(); sChangeDepth++;}
	public static void endChange() {
		boolean tPublish = F;
		try {if (--sChangeDepth < 0) {sChangeDepth = 0; throw new IllegalStateException("Furnace mutation transaction violated");} tPublish = sChangeDepth == 0;}
		finally {STATE_LOCK.unlock();}
		if (tPublish) publishChange();
	}
	private static void changed() {sDirty = T;}
	private static void publishChange() {
		if (!gregapi.GT_API.sDeferredItemInitDone || gregapi.GT_API.sDeferredItemInitRunning) return;
		net.minecraft.server.MinecraftServer tServer = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
		if (tServer == null || tServer.overworld() == null) return;
		if (!tServer.isSameThread()) {tServer.execute(FurnaceRecipes::publishChange); return;}
		boolean tAgain;
		do {
			STATE_LOCK.lock();
			try {if (!sDirty || sRefreshing || sChangeDepth != 0) return; sDirty = F; sRefreshing = T;}
			finally {STATE_LOCK.unlock();}
			// Network events and external handlers invoked outside the state lock.
			try {
				gregapi.GT_API.removeDatapackRecipes(tServer, java.util.Set.of());

				tServer.getPlayerList().reloadResources();
			} finally {
				STATE_LOCK.lock();
				try {sRefreshing = F; tAgain = sDirty;}
				finally {STATE_LOCK.unlock();}
			}
		} while (tAgain);
	}

	public boolean blacklisted(ItemStack aInput) {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			for (ItemStack tInput : mBlacklist) if (ST.equal(aInput, tInput, T)) return T;
			return F;

		} finally {STATE_LOCK.unlock();}
	}
	private static boolean sameKey(ItemStack aFirst, ItemStack aSecond) {return ST.item(aFirst) == ST.item(aSecond) && ST.meta(aFirst) == ST.meta(aSecond);}
	public void blacklist(ItemStack aInput) {
		beginChange();
		try {

			if (ST.invalid(aInput)) return;
			for (ItemStack tKey : mBlacklist) if (sameKey(aInput, tKey)) return;
			mBlacklist.add(ST.copy(aInput)); changed();

		} finally {endChange();}
	}
	private void unblacklist(ItemStack aInput) {mBlacklist.removeIf(tInput -> mChannel == Channel.SMELTING ? ST.equal(aInput, tInput, T) : sameKey(aInput, tInput));}
	public boolean remove(ItemStack aInput) {
		beginChange();
		try {

			ItemStack tOwnOutput = NI;
			if (mChannel != Channel.SMELTING) for (Map.Entry<ItemStack, ItemStack> tEntry : mSmeltingList.entrySet()) if (sameKey(aInput, tEntry.getKey())) {tOwnOutput = tEntry.getValue(); break;}
			if (ST.valid(tOwnOutput)) {
				ItemStack tRemovedOutput = tOwnOutput;
				mExperienceList.entrySet().removeIf(tEntry -> sameKey(tRemovedOutput, tEntry.getKey()));
			}
			boolean rRemoved = mSmeltingList.entrySet().removeIf(tEntry -> mChannel == Channel.SMELTING ? ST.equal(aInput, tEntry.getKey(), T) : sameKey(aInput, tEntry.getKey()));
			blacklist(aInput);
			if (rRemoved) changed();
			return rRemoved;

		} finally {endChange();}
	}
	public boolean remove(ItemStack aInput, ItemStack aOutput) {
		beginChange();
		try {

			if (mChannel == Channel.SMELTING) {
				boolean rRemoved = mSmeltingList.entrySet().removeIf(tEntry -> {
					if (!ST.equal(aInput, tEntry.getKey(), T) || !ST.equal(aOutput, tEntry.getValue(), T)) return F;
					mConditionalRemovals.add(new java.util.AbstractMap.SimpleImmutableEntry<>(ST.copy(tEntry.getKey()), ST.copy(tEntry.getValue())));
					return T;
				});
				if (rRemoved) changed();
				return rRemoved;
			}
			if (!ST.equal(aOutput, getSmeltingResult(aInput), T)) return F;
			return remove(aInput);

		} finally {endChange();}
	}
	private boolean conditionallyRemoved(ItemStack aInput, ItemStack aOutput) {return conditionallyRemoved(aInput, aOutput, mConditionalRemovals);}
	private static boolean conditionallyRemoved(ItemStack aInput, ItemStack aOutput, java.util.List<Map.Entry<ItemStack, ItemStack>> aRemoved) {
		for (Map.Entry<ItemStack, ItemStack> tPair : aRemoved) if (ST.equal(aInput, tPair.getKey(), T) && ST.equal(aOutput, tPair.getValue(), T)) return T;
		return F;
	}
	private void conditionalKeys(ItemStack aInput, ItemStack aOutput, java.util.List<ItemStack> aKeys) {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			for (Map.Entry<ItemStack, ItemStack> tPair : mConditionalRemovals) if (ST.equal(aInput, tPair.getKey(), T) && ST.equal(aOutput, tPair.getValue(), T)) aKeys.add(ST.copy(tPair.getKey()));

		} finally {STATE_LOCK.unlock();}
	}

	/** Automatic acceptance mirrors EFR, including extra oredict aliases without primary material. */
	private boolean canInherit(ItemStack aInput, ItemStack aOutput) {return canInherit(aInput, aOutput, AutoNames.INPUTS, AutoNames.OUTPUTS);}
	private boolean canInherit(ItemStack aInput, ItemStack aOutput, gregapi.code.ItemStackMap<gregapi.code.ItemStackContainer, Boolean> aNamedInputs, gregapi.code.ItemStackMap<gregapi.code.ItemStackContainer, Boolean> aNamedOutputs) {
		if (ST.invalid(aInput) || ST.invalid(aOutput)) return F;
		if (mChannel == Channel.SMOKING) return ST.edible(aOutput);
		if (mChannel != Channel.BLASTING) return F;
		return aNamedInputs.containsKey(aInput, T) || aNamedOutputs.containsKey(aOutput, T);
	}
	private static final class AutoNames {
		private static final gregapi.code.ItemStackMap<gregapi.code.ItemStackContainer, Boolean> INPUTS = new gregapi.code.ItemStackMap<>(), OUTPUTS = new gregapi.code.ItemStackMap<>();
		private static void ensureInitialized() {/**/}
		private static gregapi.code.ItemStackMap<gregapi.code.ItemStackContainer, Boolean> copy(gregapi.code.ItemStackMap<gregapi.code.ItemStackContainer, Boolean> aMap) {
			var rCopy = new gregapi.code.ItemStackMap<gregapi.code.ItemStackContainer, Boolean>(F);
			for (var tEntry : aMap.entrySet()) rCopy.put(tEntry.getKey(), tEntry.getValue());
			return rCopy;
		}
		static {
			// Ore dictionary replays past registrations and dispatches new ones with the same event.
			gregapi.oredict.OreDictManager.INSTANCE.addListener((gregapi.oredict.event.IOreDictListenerEvent)tEvent -> {
				STATE_LOCK.lock();
				try {
				String tName = tEvent.mOreDictName;
				if (tName.startsWith("ore") || tName.startsWith("cluster") || tName.startsWith("crushed")) INPUTS.put(new gregapi.code.ItemStackContainer(tEvent.mStack), T);
				if (tName.startsWith("ingot") || tName.startsWith("chunkGt") || tName.startsWith("nugget") || tName.equalsIgnoreCase("quicksilver")) OUTPUTS.put(new gregapi.code.ItemStackContainer(tEvent.mStack), T);
				} finally {STATE_LOCK.unlock();}
			});
		}
	}

	/** Traversal keys cover explicit entries and inherited smelting; outputs pass central filter. */
	public java.util.List<ItemStack> candidates() {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			java.util.List<ItemStack> rInputs = new java.util.ArrayList<>(mSmeltingList.keySet());
			if (mChannel != Channel.SMELTING) rInputs.addAll(smelting().mSmeltingList.keySet());
			return rInputs;

		} finally {STATE_LOCK.unlock();}
	}
	private boolean owns(ItemStack aInput) {
		boolean tExplicit = F;
		for (Map.Entry<ItemStack, ItemStack> tEntry : mSmeltingList.entrySet()) if (!mImportedKeys.contains(tEntry.getKey()) && ST.equal(aInput, tEntry.getKey(), T)) {tExplicit = T; break;}
		return blacklisted(aInput) || tExplicit
			|| (mChannel != Channel.SMELTING && canInherit(aInput, smelting().getSmeltingResult(aInput)));
	}

	/** Common helper for typed wrappers: output, experience, and display reuse single function. */
	private float engineXP(float aXP) {
		if (aXP == 0 || mKnownXP.contains(aXP)) return aXP;
		if (mWarnedXP.add(aXP)) ERR.println("[GT6] Furnace " + mChannel + ": no dispatcher for experience " + aXP + "; using zero experience");
		return 0;
	}
	public float engineExperience(ItemStack aOutput) {return engineXP(func_151398_b(aOutput));}
	public final class Dispatch {
		private final float mXP;
		public Dispatch(float aXP) {mXP = aXP; if (aXP > 0) mKnownXP.add(aXP);}
		public ItemStack result(ItemStack aInput, Answers aAnswers) {
			ItemStack tResult = aAnswers.result(aInput);
			if (ST.invalid(tResult)) return NI;
			return engineXP(aAnswers.xp(tResult)) == mXP ? tResult : NI;
		}
		public boolean matches(ItemStack aInput) {return ST.valid(result(aInput, live()));}
		public ItemStack assemble(ItemStack aInput) {ItemStack tResult = result(aInput, live()); return ST.valid(tResult) ? ST.copy(tResult) : ItemStack.EMPTY;}
		public java.util.List<ItemStack> showcase() {
			Answers tWalk = snapshot();
			java.util.List<ItemStack> rInputs = new java.util.ArrayList<>();
			java.util.Set<ItemStack> tSeen = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndTagSet();
			for (ItemStack tKey : tWalk.candidates()) {
				ItemStack tInput = ST.amount(1, tKey);
				if (ST.invalid(tInput) || ST.invalid(result(tInput, tWalk))) continue;
				if (tSeen.add(tInput)) rInputs.add(tInput);
			}
			return rInputs;
		}
	}


	/** 1.7.10's FurnaceRecipes.smelting() singleton accessor. */
	public static FurnaceRecipes smelting() {return INSTANCE;}

	private final Map<ItemStack, ItemStack> mSmeltingList   = new HashMap<>();
	private final Map<ItemStack, Float>     mExperienceList = new HashMap<>();

	/** 1.7.10's getSmeltingResult: first output whose input matches, wildcard-aware, same as GT6's own iteration. */
	public ItemStack getSmeltingResult(ItemStack aInput) {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			if (blacklisted(aInput)) return NI;
			ItemStack tResult = resultOf(aInput, mSmeltingList.entrySet());
			if (ST.valid(tResult) || mChannel == Channel.SMELTING) return tResult;
			tResult = smelting().getSmeltingResult(aInput);
			return canInherit(aInput, tResult) ? tResult : NI;

		} finally {STATE_LOCK.unlock();}
	}

	/** The live map's answers: what the furnace asks one input at a time. */
	public Answers live() {return mLive;}
	private final Answers mLive = new Answers() {
		@Override public ItemStack result(ItemStack aInput) {return getSmeltingResult(aInput);}
		@Override public float xp(ItemStack aOutput) {return func_151398_b(aOutput);}
		@Override public java.util.List<ItemStack> candidates() {return FurnaceRecipes.this.candidates();}
	};

	/** The registry's answers, from the live map (this object) or from one walk's snapshot of it ({@link #snapshot}). */
	public interface Answers {
		ItemStack result(ItemStack aInput);
		float xp(ItemStack aOutput);
		java.util.List<ItemStack> candidates();
	}

	/** The one result rule: the first entry, in the map's own order, whose input the stack matches. */
	private ItemStack resultOf(ItemStack aInput, Iterable<Map.Entry<ItemStack, ItemStack>> aEntries) {return resultOf(aInput, aEntries, mImportedRules, mConditionalRemovals);}
	private ItemStack resultOf(ItemStack aInput, Iterable<Map.Entry<ItemStack, ItemStack>> aEntries, java.util.Map<ItemStack, java.util.function.Function<ItemStack, ItemStack>> aImported, java.util.List<Map.Entry<ItemStack, ItemStack>> aRemoved) {
		if (ST.invalid(aInput)) return NI;
		if (mChannel != Channel.SMELTING) for (Map.Entry<ItemStack, ItemStack> tEntry : aEntries) if (ST.meta(tEntry.getKey()) == W && ST.equal(aInput, tEntry.getKey(), T)) return tEntry.getValue();
		for (Map.Entry<ItemStack, ItemStack> tEntry : aEntries) if (ST.equal(aInput, tEntry.getKey(), T)) {
			java.util.function.Function<ItemStack, ItemStack> tImported = aImported.get(tEntry.getKey());
			ItemStack tResult = tImported == null ? tEntry.getValue() : tImported.apply(aInput);
			if (ST.valid(tResult) && (tImported == null || !conditionallyRemoved(aInput, tResult, aRemoved))) return tResult;
		}
		return NI;
	}

	/** 1.7.10 func_151394_a = addSmeltingRecipe(input, output, experience). */
	public void func_151394_a(ItemStack aInput, ItemStack aOutput, float aExperience) {
		beginChange();
		try {

			if (ST.invalid(aInput) || ST.invalid(aOutput)) return;
			unblacklist(aInput);
			mSmeltingList.entrySet().removeIf(tEntry -> sameKey(aInput, tEntry.getKey()));
			mSmeltingList.put(ST.copy(aInput), aOutput);
			mExperienceList.entrySet().removeIf(tEntry -> ST.item(tEntry.getKey()) == ST.item(aOutput) && ST.meta(tEntry.getKey()) == ST.meta(aOutput));
			mExperienceList.put(aOutput, aExperience);
			changed();

		} finally {endChange();}
	}

	/** 1.7.10's getSmeltingList: a mutable map GT6 iterates and removes from directly. */
	public Map<ItemStack, ItemStack> getSmeltingList() {return mSmeltingList;}

	/** Matches the original rule: the result item's own hook is asked first and its non-(-1) answer overrides
	 *  the map; a type that doesn't implement the hook gets the vanilla default of -1 (ask the map), else 0. */
	public float func_151398_b(ItemStack aOutput) {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			if (mChannel != Channel.SMELTING) {
				boolean tOwn = F;
				for (ItemStack tKey : mExperienceList.keySet()) if (ST.equal(aOutput, tKey, T)) {tOwn = T; break;}
				if (!tOwn) return smelting().func_151398_b(aOutput);
			}
			return xpOf(aOutput, mExperienceList.entrySet());

		} finally {STATE_LOCK.unlock();}
	}

	/** The one experience rule over the given entries of the experience map, in its own order. */
	private static float xpOf(ItemStack aOutput, Iterable<Map.Entry<ItemStack, Float>> aEntries) {
		if (ST.invalid(aOutput)) return 0.0F;
		if (aOutput.getItem() instanceof gregapi.item.IItemSmeltingExperience tItem) {
			float tXP = tItem.getSmeltingExperience(aOutput);
			if (tXP != -1) return tXP;
		}
		for (Map.Entry<ItemStack, Float> tEntry : aEntries) if (ST.equal(aOutput, tEntry.getKey(), T)) return tEntry.getValue();
		return 0.0F;
	}

	/** The registry's answers for a walk over many stacks, the same as the live map gives: ST.equal only matches stacks of
	 *  one item or of one flattened family, so each lookup reads only its item's part, kept in the map's own order. Built
	 *  per walk and read only while the registry is not changed; a whole-registry pass stays linear. */
	public Answers snapshot() {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {
			java.util.Map<ItemStack, ItemStack> tCopied = new java.util.LinkedHashMap<>();
			java.util.Map<ItemStack, java.util.function.Function<ItemStack, ItemStack>> tRules = new java.util.IdentityHashMap<>();
			for (Map.Entry<ItemStack, ItemStack> tEntry : mSmeltingList.entrySet()) {
				ItemStack tKey = ST.copy(tEntry.getKey()); tCopied.put(tKey, ST.copy(tEntry.getValue()));
				var tRule = mImportedRules.get(tEntry.getKey()); if (tRule != null) tRules.put(tKey, tRule);
			}
			java.util.Map<ItemStack, Float> tCopiedXP = new java.util.LinkedHashMap<>();
			for (Map.Entry<ItemStack, Float> tEntry : mExperienceList.entrySet()) tCopiedXP.put(ST.copy(tEntry.getKey()), tEntry.getValue());
			var tResults = part(tCopied); var tXP = part(tCopiedXP);
			var tBlacklist = mBlacklist.stream().map(ST::copy).toList();
			java.util.List<Map.Entry<ItemStack, ItemStack>> tRemoved = mConditionalRemovals.stream().map(t -> (Map.Entry<ItemStack, ItemStack>)new java.util.AbstractMap.SimpleImmutableEntry<>(ST.copy(t.getKey()), ST.copy(t.getValue()))).toList();
			var tNamedInputs = AutoNames.copy(AutoNames.INPUTS); var tNamedOutputs = AutoNames.copy(AutoNames.OUTPUTS);
			Answers tParent = mChannel == Channel.SMELTING ? null : smelting().snapshot();
			java.util.List<ItemStack> tInputs = new java.util.ArrayList<>(tCopied.keySet());
			if (tParent != null) tInputs.addAll(tParent.candidates());
			var tCandidates = java.util.List.copyOf(tInputs);
			return new Answers() {
				@Override public java.util.List<ItemStack> candidates() {return tCandidates.stream().map(ST::copy).toList();}
				@Override public ItemStack result(ItemStack aInput) {
					if (ST.invalid(aInput) || matchesKeys(aInput, tBlacklist)) return NI;
					ItemStack tResult = resultOf(aInput, tResults.getOrDefault(partOf(aInput), java.util.List.of()), tRules, tRemoved);
					if (ST.valid(tResult) || mChannel == Channel.SMELTING) return ST.valid(tResult) ? ST.copy(tResult) : NI;
					tResult = tParent.result(aInput);
					if (ST.invalid(tResult)) return NI;
					return canInherit(aInput, tResult, tNamedInputs, tNamedOutputs) ? tResult : NI;
				}
				@Override public float xp(ItemStack aOutput) {
					if (ST.invalid(aOutput)) return 0;
					java.util.List<Map.Entry<ItemStack, Float>> tEntries = tXP.getOrDefault(partOf(aOutput), java.util.List.of());
					for (Map.Entry<ItemStack, Float> tEntry : tEntries) if (ST.equal(aOutput, tEntry.getKey(), T)) return xpOf(aOutput, tEntries);
					return mChannel == Channel.SMELTING ? xpOf(aOutput, tEntries) : tParent.xp(aOutput);
				}
			};
		} finally {STATE_LOCK.unlock();}
	}

	/** The part a stack can match in: its flattened family's head, or its own item. */
	private static net.minecraft.world.item.Item partOf(ItemStack aStack) {
		net.minecraft.world.item.Item tHead = CS.Flattened.headItemOf(aStack.getItem());
		return tHead != null ? tHead : aStack.getItem();
	}

	private static <V> java.util.Map<net.minecraft.world.item.Item, java.util.List<Map.Entry<ItemStack, V>>> part(Map<ItemStack, V> aMap) {
		java.util.Map<net.minecraft.world.item.Item, java.util.List<Map.Entry<ItemStack, V>>> rParts = new HashMap<>();
		for (Map.Entry<ItemStack, V> tEntry : aMap.entrySet()) if (ST.valid(tEntry.getKey())) rParts.computeIfAbsent(partOf(tEntry.getKey()), k -> new java.util.ArrayList<>()).add(new java.util.AbstractMap.SimpleImmutableEntry<>(tEntry.getKey(), tEntry.getValue()));
		return rParts;
	}

	/** Smelting and ore bans form shared contract across import and live recipe map. */
	public static boolean blockedVanilla(ItemStack aInput, ItemStack aOutput) {
		gregapi.oredict.OreDictItemData tIn = gregapi.util.OM.anydata(aInput), tOut = gregapi.util.OM.anydata(aOutput);
		if (tIn == null || !tIn.nonemptyData() || tOut == null || !tOut.nonemptyData()) return F;
		gregapi.oredict.OreDictPrefix tPrefix = tIn.mPrefix;
		if (tPrefix == gregapi.data.OP.ore || tPrefix == gregapi.data.OP.oreDeepslate || tPrefix == gregapi.data.OP.oreNetherrack
			|| tPrefix == gregapi.data.OP.oreEndstone || tPrefix == gregapi.data.OP.oreBlackgranite || tPrefix == gregapi.data.OP.oreRedgranite
			|| tPrefix == gregapi.data.OP.oreMarble || tPrefix == gregapi.data.OP.oreBasalt) return T;
		if (tPrefix == gregapi.data.OP.oreRaw && (tIn.mMaterial == null || !tIn.mMaterial.mMaterial.contains(gregapi.data.TD.Processing.FURNACE))) return T;
		if (tOut.mPrefix != gregapi.data.OP.ingot && tOut.mPrefix != gregapi.data.OP.gem) return F;
		return tPrefix != gregapi.data.OP.dust && tPrefix != gregapi.data.OP.dustSmall && tPrefix != gregapi.data.OP.dustTiny
			&& tPrefix != gregapi.data.OP.crushed && tPrefix != gregapi.data.OP.crushedPurified && tPrefix != gregapi.data.OP.crushedCentrifuged
			&& tPrefix != gregapi.data.OP.nugget && tPrefix != gregapi.data.OP.rockGt;
	}

	private java.util.List<net.minecraft.world.item.crafting.Recipe<?>> mPendingVanilla;
	public void resumeVanilla(net.minecraft.world.level.Level aLevel) {if (mPendingVanilla != null) {var tRecipes = mPendingVanilla; mPendingVanilla = null; restoreVanilla(tRecipes, aLevel);}}

	private final java.util.List<gregapi.recipes.Recipe> mShowcaseOwned = new java.util.ArrayList<>();
	/** Only recipes owned by this holder are removed; external additions remain untouched. */
	private void rebuildShowcase() {
		for (gregapi.recipes.Recipe tOld : mShowcaseOwned) gregapi.data.RM.Furnace.remove(tOld);
		mShowcaseOwned.clear();
		for (ItemStack tKey : new java.util.ArrayList<>(mSmeltingList.keySet())) {
			if (ST.invalid(tKey)) continue;
			gregapi.recipes.Recipe tRecipe = gregapi.data.RM.Furnace.findRecipe(null, null, F, Long.MAX_VALUE, NI, ZL_FS, ST.array(ST.copy(tKey)));
			if (tRecipe == null) continue;
			gregapi.data.RM.Furnace.addFakeRecipe(F, tRecipe);
			if (gregapi.data.RM.Furnace.mRecipeList.contains(tRecipe)) mShowcaseOwned.add(tRecipe);
		}
	}

	private boolean mShowcaseFilled = false;
	private final java.util.Set<ItemStack> mImportedKeys = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
	private final java.util.Map<ItemStack, java.util.function.Function<ItemStack, ItemStack>> mImportedRules = new java.util.IdentityHashMap<>();
	private final java.util.Set<ItemStack> mImportedXPKeys = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

	/** The vanilla half of this list and the furnace showcase, from the smelting recipes at hand: the server's at its start,
	 *  a dedicated-server client's on receiving them. 1.7.10 filled the showcase on the client, once per process. */
	public void restoreVanilla(Iterable<? extends net.minecraft.world.item.crafting.Recipe<?>> aSmelting, net.minecraft.world.level.Level aLevel) {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			if (aLevel == null) {mPendingVanilla = new java.util.ArrayList<>(); for (net.minecraft.world.item.crafting.Recipe<?> tRecipe : aSmelting) mPendingVanilla.add(tRecipe); return;}
			int tImported = importVanilla(aSmelting, aLevel);
			if (!mShowcaseFilled) {mShowcaseFilled = true; rebuildShowcase();}
			OUT.println("[GT6] Import vanilla smelting: " + tImported + "; active cards: " + mShowcaseOwned.size());

		} finally {STATE_LOCK.unlock();}
	}

	/** Restores the vanilla half of this list, lost when the class became GT6's own storage instead of vanilla's;
	 *  by user decision every vanilla recipe is imported, including items that did not exist in 1.7.10. */
	public int importVanilla(Iterable<? extends net.minecraft.world.item.crafting.Recipe<?>> aSmelting, net.minecraft.world.level.Level aLevel) {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			if (aLevel == null) throw new IllegalArgumentException("Furnace import requires non-null Level");
			int rAdded = 0;
			mSmeltingList.keySet().removeAll(mImportedKeys);
			mExperienceList.keySet().removeAll(mImportedXPKeys);
			mImportedKeys.clear(); mImportedXPKeys.clear(); mImportedRules.clear();
			try {
				// The smelting recipes also hold GT6's own dispatcher, a SmeltingRecipe that relays into this same list rather than data.
				for (net.minecraft.world.item.crafting.Recipe<?> tAny : aSmelting) {
					if (tAny instanceof GT6SmeltingDispatcher) continue;
					if (!(tAny instanceof net.minecraft.world.item.crafting.SmeltingRecipe tRecipe)) continue;
					for (Ingredient tIngredient : tRecipe.getIngredients()) for (ItemStack tItem : tIngredient.getItems()) {
						ItemStack tIn = ST.amount(1, tItem);
						if (!tRecipe.matches(new net.minecraft.world.SimpleContainer(tIn), aLevel)) continue;
						if (ST.invalid(tIn) || blacklisted(tIn)) continue;
						// A GT6 recipe already registered for this input takes priority and is not overwritten.
						if (ST.valid(getSmeltingResult(tIn))) continue;
						ItemStack tOut = tRecipe.assemble(new net.minecraft.world.SimpleContainer(tIn), aLevel.registryAccess());
						if (ST.invalid(tOut)) continue;
						if (blockedVanilla(tIn, tOut) || conditionallyRemoved(tIn, tOut)) continue;
						ItemStack tCopied = ST.copy(tOut);
						mSmeltingList.put(tIn, tCopied); mExperienceList.put(tCopied, tRecipe.getExperience());
						mImportedKeys.add(tIn); mImportedXPKeys.add(tCopied);
						mImportedRules.put(tIn, aInput -> tRecipe.matches(new net.minecraft.world.SimpleContainer(aInput), aLevel) ? tRecipe.assemble(new net.minecraft.world.SimpleContainer(aInput), aLevel.registryAccess()) : NI);
						rAdded++;
					}
				}
			} catch (Throwable e) {
				e.printStackTrace(gregapi.data.CS.ERR);
			}
			if (mShowcaseFilled) rebuildShowcase();
			return rAdded;

		} finally {STATE_LOCK.unlock();}
	}

	/** Network key equality predicate: meta is checked, custom names and other NBT ignored. */
	public static boolean matchesKeys(ItemStack aInput, java.util.List<ItemStack> aKeys) {
		for (ItemStack tKey : aKeys) if (ST.equal(aInput, tKey, T)) return T;
		return F;
	}
	public java.util.List<ItemStack> ownedKeys() {
		AutoNames.ensureInitialized(); STATE_LOCK.lock();
		try {

			java.util.List<ItemStack> rKeys = new java.util.ArrayList<>(mBlacklist);
			for (ItemStack tInput : candidates()) if (owns(tInput)) rKeys.add(ST.copy(tInput));
			return rKeys;

		} finally {STATE_LOCK.unlock();}
	}
	public static boolean dispatcher(Object aRecipe) {
		return aRecipe instanceof GT6SmeltingDispatcher || aRecipe instanceof GT6SmokingDispatcher || aRecipe instanceof GT6BlastingDispatcher;
	}

	/** Single rule snapshot per map rebuild; independent recipes retain object identity and ID. */
	public static final java.util.Set<String> NATIVE_LIMITS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	public static final class NativeFilter {
		private final net.minecraft.server.level.ServerLevel mLevel;
		private final java.util.Map<Channel, java.util.List<ItemStack>> mKeys = new java.util.EnumMap<>(Channel.class);
		public NativeFilter(net.minecraft.server.level.ServerLevel aLevel) {
			mLevel = aLevel;
			for (FurnaceRecipes tChannel : new FurnaceRecipes[]{smelting(), smoking(), blasting()}) mKeys.put(tChannel.channel(), tChannel.ownedKeys());
		}
		public net.minecraft.world.item.crafting.Recipe<?> filter(net.minecraft.world.item.crafting.Recipe<?> aRecipe) {
			try {return filterOne(aRecipe);} catch (RuntimeException tError) {
				String tLimit = aRecipe.getClass().getName() + " / " + aRecipe.getId();
				NATIVE_LIMITS.add(tLimit); ERR.println("[GT6] Furnace recipe adaptation limit: " + tLimit + " / " + tError);
				return aRecipe;
			}
		}
		private net.minecraft.world.item.crafting.Recipe<?> filterOne(net.minecraft.world.item.crafting.Recipe<?> aRecipe) {
			if (!(aRecipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe tRecipe) || dispatcher(tRecipe)) return aRecipe;
			Channel tChannel = tRecipe.getType() == net.minecraft.world.item.crafting.RecipeType.SMELTING ? Channel.SMELTING
				: tRecipe.getType() == net.minecraft.world.item.crafting.RecipeType.SMOKING ? Channel.SMOKING
				: tRecipe.getType() == net.minecraft.world.item.crafting.RecipeType.BLASTING ? Channel.BLASTING : null;
			if (tChannel == null) return aRecipe;
			net.minecraft.world.item.crafting.Ingredient tBase = tRecipe.getIngredients().get(0);
			if (tBase instanceof RuntimeIngredient tOld && tOld.mBase != null) {
				tBase = tOld.mBase;
				tRecipe = (net.minecraft.world.item.crafting.AbstractCookingRecipe)rebuild(aRecipe, tBase);
			}
			java.util.Set<net.minecraft.world.item.Item> tItems = java.util.Arrays.stream(tBase.getItems()).map(ItemStack::getItem).collect(java.util.stream.Collectors.toSet());
			java.util.List<ItemStack> tKeys = new java.util.ArrayList<>();
			for (ItemStack tKey : mKeys.get(tChannel)) for (net.minecraft.world.item.Item tItem : tItems) {
				if (tKey.getItem() == tItem || (ST.meta(tKey) == W && CS.Flattened.sameFamily(tKey.getItem(), tItem))) {tKeys.add(tKey); break;}
			}
			for (ItemStack tInput : tBase.getItems()) if (tRecipe.matches(new net.minecraft.world.SimpleContainer(tInput), mLevel) ) {
				ItemStack tOutput = tRecipe.assemble(new net.minecraft.world.SimpleContainer(tInput), mLevel.registryAccess());
				if (blockedVanilla(tInput, tOutput)) tKeys.add(tInput);
				smelting().conditionalKeys(tInput, tOutput, tKeys);
			}
			if (tKeys.isEmpty()) return aRecipe == tRecipe ? aRecipe : rebuild(aRecipe, tBase);
			net.minecraft.world.item.crafting.Ingredient tFiltered = new RuntimeIngredient(tKeys, tBase);
			return rebuild(aRecipe, tFiltered);
		}
		private net.minecraft.world.item.crafting.Recipe<?> rebuild(net.minecraft.world.item.crafting.Recipe<?> aRecipe, net.minecraft.world.item.crafting.Ingredient tFiltered) {
			var tRecipe = (net.minecraft.world.item.crafting.AbstractCookingRecipe)aRecipe;
			ItemStack tOutput = tRecipe.getResultItem(mLevel.registryAccess());
			// 1.20.1 cannot serialize arbitrary Recipe to JSON; factory preserves standard typed contract.
			if (tRecipe.getClass() == net.minecraft.world.item.crafting.SmeltingRecipe.class) return new net.minecraft.world.item.crafting.SmeltingRecipe(tRecipe.getId(), tRecipe.getGroup(), tRecipe.category(), tFiltered, ST.copy(tOutput), tRecipe.getExperience(), tRecipe.getCookingTime());
			if (tRecipe.getClass() == net.minecraft.world.item.crafting.SmokingRecipe.class) return new net.minecraft.world.item.crafting.SmokingRecipe(tRecipe.getId(), tRecipe.getGroup(), tRecipe.category(), tFiltered, ST.copy(tOutput), tRecipe.getExperience(), tRecipe.getCookingTime());
			if (tRecipe.getClass() == net.minecraft.world.item.crafting.BlastingRecipe.class) return new net.minecraft.world.item.crafting.BlastingRecipe(tRecipe.getId(), tRecipe.getGroup(), tRecipe.category(), tFiltered, ST.copy(tOutput), tRecipe.getExperience(), tRecipe.getCookingTime());
			String tLimit = tRecipe.getClass().getName() + " / " + tRecipe.getId(); NATIVE_LIMITS.add(tLimit); ERR.println("[GT6] Furnace subclass adaptation limit: " + tLimit);
			throw new IllegalArgumentException("Cannot safely rebuild furnace subclass " + tRecipe.getClass().getName() + " / " + tRecipe.getId());
		}
	}

	public static final class RuntimeIngredient extends net.minecraftforge.common.crafting.AbstractIngredient {
		private final java.util.List<ItemStack> mKeys;
		private final net.minecraft.world.item.crafting.Ingredient mBase, mDifference;
		public RuntimeIngredient(java.util.List<ItemStack> aKeys) {this(aKeys, null);}
		public RuntimeIngredient(java.util.List<ItemStack> aKeys, net.minecraft.world.item.crafting.Ingredient aBase) {
			mKeys = aKeys.stream().map(ST::copy).toList(); mBase = aBase;
			mDifference = mBase == null ? null : net.minecraftforge.common.crafting.DifferenceIngredient.of(mBase, new RuntimeIngredient(mKeys));
		}
		@Override public boolean test(ItemStack aInput) {return mDifference == null ? matchesKeys(aInput, mKeys) : mDifference.test(aInput);}
		@Override public boolean isSimple() {return F;}
		@Override public ItemStack[] getItems() {
			if (mBase != null) return java.util.Arrays.stream(mBase.getItems()).filter(this::test).map(ST::copy).toArray(ItemStack[]::new);
			java.util.List<ItemStack> rInputs = new java.util.ArrayList<>();
			for (ItemStack tKey : mKeys) {
				if (ST.meta(tKey) != W) rInputs.add(ST.copy(tKey));
				else for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) if (tKey.getItem() == tItem || CS.Flattened.sameFamily(tKey.getItem(), tItem)) rInputs.add(new ItemStack(tItem));
			}
			return rInputs.toArray(new ItemStack[0]);
		}
		@Override public net.minecraftforge.common.crafting.IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer() {return RUNTIME_INGREDIENT_SERIALIZER;}
		@Override public com.google.gson.JsonElement toJson() {
			com.google.gson.JsonObject rJSON = new com.google.gson.JsonObject();
			rJSON.addProperty("type", "gregapi:cooking_runtime_keys");
			com.google.gson.JsonArray tKeys = new com.google.gson.JsonArray();
			for (ItemStack tKey : mKeys) {
				com.google.gson.JsonObject tJSON = new com.google.gson.JsonObject();
				tJSON.addProperty("item", ST.regName(tKey)); tJSON.addProperty("meta", ST.meta(tKey)); tKeys.add(tJSON);
			}
			rJSON.add("keys", tKeys); if (mBase != null) rJSON.add("base", mBase.toJson()); return rJSON;
		}
	}
	public static final net.minecraftforge.common.crafting.IIngredientSerializer<RuntimeIngredient> RUNTIME_INGREDIENT_SERIALIZER = new net.minecraftforge.common.crafting.IIngredientSerializer<>() {
		@Override public RuntimeIngredient parse(net.minecraft.network.FriendlyByteBuf aBuf) {
			int tSize = aBuf.readVarInt();
			java.util.List<ItemStack> tKeys = new java.util.ArrayList<>(tSize);
			for (int i = 0; i < tSize; i++) tKeys.add(aBuf.readItem());
			return new RuntimeIngredient(tKeys, aBuf.readBoolean() ? net.minecraft.world.item.crafting.Ingredient.fromNetwork(aBuf) : null);
		}
		@Override public RuntimeIngredient parse(com.google.gson.JsonObject aJSON) {
			java.util.List<ItemStack> tKeys = new java.util.ArrayList<>();
			for (com.google.gson.JsonElement tElement : aJSON.getAsJsonArray("keys")) {
				com.google.gson.JsonObject tKey = tElement.getAsJsonObject();
				tKeys.add(ST.make(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(tKey.get("item").getAsString())), 1, tKey.get("meta").getAsInt()));
			}
			return new RuntimeIngredient(tKeys, aJSON.has("base") ? net.minecraft.world.item.crafting.Ingredient.fromJson(aJSON.get("base")) : null);
		}
		@Override public void write(net.minecraft.network.FriendlyByteBuf aBuf, RuntimeIngredient aIngredient) {
			aBuf.writeVarInt(aIngredient.mKeys.size()); for (ItemStack tKey : aIngredient.mKeys) aBuf.writeItem(tKey);
			aBuf.writeBoolean(aIngredient.mBase != null); if (aIngredient.mBase != null) aIngredient.mBase.toNetwork(aBuf);
		}
	};

}
