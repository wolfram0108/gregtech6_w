package net.minecraft.util;

import net.minecraft.world.item.ItemStack;

/** 1.7.10 chest-gen data-holder. neo убрал класс; GT6-loot (Loader_Loot) добавляет предметы через него.
 *  Применяется в рантайме диспетчером {@link net.minecraftforge.common.ChestGenHooks} на LootTableLoadEvent. */
public class WeightedRandomChestContent {
	public ItemStack theItemId;
	public int theMinimumChanceToGenerateItem;
	public int theMaximumChanceToGenerateItem;
	public int itemWeight;

	public WeightedRandomChestContent(ItemStack aStack, int aMinChance, int aMaxChance, int aWeight) {
		theItemId = aStack;
		theMinimumChanceToGenerateItem = aMinChance;
		theMaximumChanceToGenerateItem = aMaxChance;
		itemWeight = aWeight;
	}

	/** 1.7.10 генерация содержимого сундука. neo — data-driven LootPool; чтение/генерация требует рантайм-LootContext
	 *  (нет на init). REPLACE-подкласс ChestReplacer переопределяет; base — видимый PORT-TODO-сбой до IGlobalLootModifier. */
	protected ItemStack[] generateChestContent(java.util.Random aRandom, net.minecraft.world.Container aInventory) {
		throw new UnsupportedOperationException("PORT-TODO(F-loot REPLACE): generateChestContent требует neo LootContext; ре-экспрессировать в IGlobalLootModifier — decisions/F-loot-chestgen-map.md");
	}
}
