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

package net.minecraft.launchwrapper;

/**
 * F2 compile-only shim. 1.7.10 FML launchwrapper (пакет {@code net.minecraft.launchwrapper} — старый FML,
 * НЕ Mojang, отсутствует на neo-classpath целиком, 0 хитов во всех 3 корнях референса) — см. javadoc
 * {@link IClassTransformer} (тот же F2-шов, ADR {@code decisions/F2-coremod-mixin.md}). {@code extends
 * ClassLoader} — обязателен для узкого каста {@code (LaunchClassLoader)Thread.currentThread().
 * getContextClassLoader()} в {@code GT_ASM.injectData} (JLS 5.5 — narrowing reference conversion между
 * классами требует subtype-отношения). {@link #registerTransformer(String)} — единственный вызываемый
 * GT6-код метод; в рантайме этот путь недостижим (GT_ASM не регистрируется как coremod, ADR §6), поэтому
 * тело честно no-op, а не имитирует реальную ASM-регистрацию.
 */
public class LaunchClassLoader extends ClassLoader {
	public void registerTransformer(String transformerClassName) {
	}
}
