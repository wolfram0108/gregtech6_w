package net.minecraft.stats;

import net.minecraft.advancements.Advancement;

/** 1.7.10 {@code net.minecraft.stats.AchievementList} — реестр ванильных достижений. neo удалил Achievement/
 *  AchievementList/triggerAchievement (→ data-driven advancements). Ядро приняло F18-решение: выдача достижений —
 *  централизованный no-op ({@code ST.achieve(Entity, Advancement)} возвращает T без действия, decisions/F18-achievements.md),
 *  т.к. единственный neo-API PlayerAdvancements.award навязывает рецепты+xp+чат — не 1:1. Эти константы — типоносители,
 *  передаваемые в тот no-op (значение игнорируется); null консистентен с F18. Достижения не входят в golden-паритет. */
public class AchievementList {
	public static final Advancement acquireIron = null, buildPickaxe = null, buildBetterPickaxe = null,
		buildSword = null, buildHoe = null, buildFurnace = null;
}
