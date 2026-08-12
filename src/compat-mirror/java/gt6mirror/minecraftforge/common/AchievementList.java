/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * This declaration was written from scratch for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). It contains no code from the project
 * that owns this package name, and no part of it was copied or decompiled from that
 * project: it declares only the members GregTech 6 itself implements or calls, so that
 * the port compiles while integration with that mod stays deferred.
 *
 * The original package name is kept deliberately, because GregTech 6 implements these
 * types verbatim and the port does not alter the code Gregorius Techneticies wrote.
 * Removing these classes from the build is not possible: 66 classes of the mod extend
 * or implement them, and the JVM requires the type to load the implementing class.
 *
 * All names, trademarks and rights in the project this package belongs to remain with
 * its authors. See src/compat-mirror/README.md and NOTICE.
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

// Пакет gt6mirror.minecraftforge.common (не net.minecraftforge.common): boot-краш ResolutionException —
// настоящий модуль forge 1.20.1 и модуль gregtech6 экспортировали бы один и тот же пакет net.minecraftforge.*
// (split-package), JPMS такое не резолвит; тип живой (используется рантаймом), поэтому переупакован, а не удалён.
package gt6mirror.minecraftforge.common;

import net.minecraft.advancements.Advancement;

/** BUG-039 v4: перемещён из net.minecraft.stats (JPMS вырезал пакет из рантайма -> getstatic из GT_Tool_* кидал NoClassDefFoundError при крафте инструментов; канон WRCC). 1.7.10 {@code net.minecraft.stats.AchievementList} — реестр ванильных достижений. neo удалил Achievement/
 *  AchievementList/triggerAchievement (→ data-driven advancements). Ядро приняло F18-решение: выдача достижений —
 *  централизованный no-op ({@code ST.achieve(Entity, Advancement)} возвращает T без действия, decisions/F18-achievements.md),
 *  т.к. единственный neo-API PlayerAdvancements.award навязывает рецепты+xp+чат — не 1:1. Эти константы — типоносители,
 *  передаваемые в тот no-op (значение игнорируется); null консистентен с F18. Достижения не входят в golden-паритет. */
public class AchievementList {
	public static final Advancement acquireIron = null, buildPickaxe = null, buildBetterPickaxe = null,
		buildSword = null, buildHoe = null, buildFurnace = null;
}
