package net.minecraftforge.common;

import java.util.Random;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** 1.7.10 {@code net.minecraft.util.WeightedRandomChestContent} — data-holder chest-лута.
 *  BUG-039: класс живёт в {@code net.minecraftforge.common}, а НЕ в оригинальном {@code net.minecraft.util} —
 *  пакетом {@code net.minecraft.*} в рантайме владеет модуль minecraft (JPMS split-package), поэтому build.gradle
 *  исключает {@code net/minecraft/**} из jar и стрипает его из dev-запусков → класс в оригинальном пакете
 *  физически не существует в рантайме (NoClassDefFoundError). Пакет net.minecraftforge никем не занят и уходит
 *  в jar (там же живёт {@link ChestGenHooks}). Тело 1:1; см. decisions/F-loot-chestgen-map.md. */
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

	/** 1.7.10 Forge-хук: генерация стеков одного entry. Тело 1:1 ({@code ChestGenHooks.generateStacks}). */
	protected ItemStack[] generateChestContent(Random aRandom, Container aInventory) {
		return ChestGenHooks.generateStacks(aRandom, theItemId, theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem);
	}

	/** 1.7.10 {@code WeightedRandomChestContent.generateChestContents} 1:1: aCount раз — взвешенный выбор entry,
	 *  генерация его стеков, раскладка в случайные слоты инвентаря. */
	public static void generateChestContents(Random aRandom, WeightedRandomChestContent[] aList, Container aInventory, int aCount) {
		for (int j = 0; j < aCount; ++j) {
			WeightedRandomChestContent tContent = getRandomItem(aRandom, aList);
			if (tContent == null) continue;
			ItemStack[] tStacks = tContent.generateChestContent(aRandom, aInventory);
			for (ItemStack tStack : tStacks) {
				aInventory.setItem(aRandom.nextInt(aInventory.getContainerSize()), tStack);
			}
		}
	}

	/** 1.7.10 {@code WeightedRandom.getRandomItem} 1:1 (рулетка по itemWeight; сам класс WeightedRandom в neo
	 *  переработан несовместимо, потому формула воспроизведена здесь — единственном месте её использования). */
	public static WeightedRandomChestContent getRandomItem(Random aRandom, WeightedRandomChestContent[] aList) {
		int tTotal = 0;
		for (WeightedRandomChestContent tContent : aList) tTotal += tContent.itemWeight;
		if (tTotal <= 0) return null;
		int tRoll = aRandom.nextInt(tTotal);
		for (WeightedRandomChestContent tContent : aList) {
			tRoll -= tContent.itemWeight;
			if (tRoll < 0) return tContent;
		}
		return null;
	}
}
