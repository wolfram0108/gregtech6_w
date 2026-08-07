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

package appeng.api;

import appeng.api.registries.IRegistries;

/** F10 ЗЕРКАЛО (compile-only) чужого API — AppliedEnergistics2. Было {@code interface AEApi {}} —
 *  сломано: GT_API_Post.java:733 делает {@code AEApi.instance().registries().grinder().getRecipes().clear()}
 *  (цепочка вызовов, статика + 2 инстанс-метода). Пакеты {@code appeng.api.registries}/
 *  {@code appeng.api.recipes} у реального AE2 rv3-beta-6 (build.gradle:218, jar не разрешён в
 *  gradle-кэше — недоступен для javap) НЕ подтверждены грепом по трём каноничным корням
 *  (не neo/neoforge/fml API) — размещение по аналогии с реальной структурой AE2
 *  (api/registries/*, api/recipes/*), помечено как вывод из цепочки вызова, не факт. */
public final class AEApi {
	public static AEApi instance() {return null;}
	public IRegistries registries() {return null;}
}
