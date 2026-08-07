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

package mods.railcraft.common.carts;

import net.minecraft.world.level.block.Block;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Railcraft. Реально используются два оверлоада
 *  (найдено компилятором сверх исходной спеки — греп {@code addMineableBlock} по всему
 *  gregtech6_w/src/main): addMineableBlock(Block):void — 12 вызовов (BlockBaseTree.java:43 и др.);
 *  addMineableBlock(Block,int):void (мета-специфичный) — BlockStones.java:206-208,
 *  Compat_Recipes_Railcraft.java:172,183. Остальная поверхность реального EntityTunnelBore
 *  (класс сущности-вагонетки) не используется (греп 0) — не добавлена. */
public final class EntityTunnelBore {
	public static void addMineableBlock(Block aBlock) {/**/}
	public static void addMineableBlock(Block aBlock, int aMeta) {/**/}
}
