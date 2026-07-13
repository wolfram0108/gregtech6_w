package net.minecraftforge.common;

import net.minecraft.world.item.ItemStack;

import java.util.Random;

/**
 * F-loot compile-only shim. 1.7.10 Forge {@code net.minecraftforge.common.ChestGenHooks} удалён в neo —
 * chest-loot стал data-driven (neo {@code LootTable} + {@code GlobalLootModifier}). Категорийные String-константы
 * портированы 1:1 (значения сверены с референсом gregtech6/.../ChestGenHooks.java) — GT6-loot-система использует
 * их как КЛЮЧИ (LOOT_TABLES_VANILLA, dungeon loot, Behavior_Drop_Loot), потому строки faithful.
 * <p>
 * {@link #getOneItem} — единственный активный метод (getInfo/getItems/getCount у GT6 закомментированы). Реальная
 * генерация лута через neo LootTable — отдельный контракт (PORT-TODO stats-loot/chest-loot); сейчас deferred
 * (null = "нет предмета", 1.7.10-семантика пустой категории; GT6-потребители null обрабатывают).
 */
public class ChestGenHooks {
	public static final String
		  STRONGHOLD_LIBRARY   = "strongholdLibrary"
		, STRONGHOLD_CORRIDOR  = "strongholdCorridor"
		, STRONGHOLD_CROSSING  = "strongholdCrossing"
		, BONUS_CHEST          = "bonusChest"
		, DUNGEON_CHEST        = "dungeonChest"
		, VILLAGE_BLACKSMITH   = "villageBlacksmith"
		, PYRAMID_JUNGLE_CHEST = "pyramidJungleChest"
		, PYRAMID_DESERT_CHEST = "pyramidDesertyChest"
		, MINESHAFT_CORRIDOR   = "mineshaftCorridor";

	/** PORT-TODO(F-loot, chest-loot): реальная генерация через neo LootTable по категории; сейчас deferred. */
	public static ItemStack getOneItem(String aCategory, Random aRand) {return null;}
}
