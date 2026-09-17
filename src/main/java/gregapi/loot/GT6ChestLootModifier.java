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

package gregapi.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** The old channel mutated a live LootTable, which Forge 1.20.1 freezes before GT6's own chest-loot buffer is even filled,
 *  so it crashed on every start; this uses the same after-the-fact loot-modifier trick as GT6BlockDropsModifier instead. */
public class GT6ChestLootModifier extends LootModifier {
	public static final Codec<GT6ChestLootModifier> CODEC = RecordCodecBuilder.create(aInstance -> codecStart(aInstance).apply(aInstance, GT6ChestLootModifier::new));

	/** The same GLM-serializer registry as GT6BlockDropsModifier; adds one more entry rather than starting a second registry. */
	public static final net.minecraftforge.registries.RegistryObject<Codec<GT6ChestLootModifier>> TYPE = GT6BlockDropsModifier.SERIALIZERS.register("chest_loot", () -> CODEC);

	/** Empty on purpose: it only touches the class and initializes TYPE before RegisterEvent; no second subscription needed. */
	public static void register() {}

	public GT6ChestLootModifier(LootItemCondition[] aConditions) {super(aConditions);}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> aLoot, LootContext aContext) {
		ResourceLocation tTableId = aContext.getQueriedLootTableId();
		if (tTableId == null) return aLoot;
		String tCategory = gt6mirror.minecraftforge.common.ChestGenHooks.categoryForTable(tTableId);
		if (tCategory == null) return aLoot;
		LootPool tPool = gt6mirror.minecraftforge.common.ChestGenHooks.buildPool(tCategory);
		if (tPool == null) return aLoot;
		tPool.addRandomItems(LootTable.createStackSplitter(aContext.getLevel(), aLoot::add), aContext);
		return aLoot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {return CODEC;}
}
