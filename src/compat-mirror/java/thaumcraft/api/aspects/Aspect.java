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

package thaumcraft.api.aspects;

import net.minecraft.resources.Identifier;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (константы аспектов + минимум методов) — держит ядро компилируемым, пока интеграция с TC отложена.
 *  Значения намеренно нейтральны (интеграция не исполняется без реального TC). См. compat-mirror/README.md. */
public class Aspect {
	// Обёртка GT6 (TC_Aspect) читает вложенный `.mAspect` — держим совместимое поле для зеркала.
	public final Aspect mAspect;

	public Aspect() {this.mAspect = null;}

	/** CompatTC.java:138-142: new Aspect(String, int, Aspect[], Identifier, int). */
	public Aspect(String aTag, int aColor, Aspect[] aComponents, Identifier aImage, int aRarity) {this.mAspect = null;}

	public int getMetadata() {return 0;}

	// Константы-аспекты, к которым обращается GregTech6 (grep Aspect.X по gregapi/*).
	public static final Aspect
		  AIR       = new Aspect(), ARMOR   = new Aspect(), AURA    = new Aspect(), BEAST   = new Aspect()
		, CLOTH     = new Aspect(), COLD    = new Aspect(), CRAFT   = new Aspect(), CROP    = new Aspect()
		, CRYSTAL   = new Aspect(), DARKNESS= new Aspect(), DEATH   = new Aspect(), EARTH   = new Aspect()
		, ELDRITCH  = new Aspect(), ENERGY  = new Aspect(), ENTROPY = new Aspect(), EXCHANGE= new Aspect()
		, FIRE      = new Aspect(), FLESH   = new Aspect(), FLIGHT  = new Aspect(), GREED   = new Aspect()
		, HARVEST   = new Aspect(), HEAL    = new Aspect(), HUNGER  = new Aspect(), LIFE    = new Aspect()
		, LIGHT     = new Aspect(), MAGIC   = new Aspect(), MAN     = new Aspect(), MECHANISM= new Aspect()
		, METAL     = new Aspect(), MIND    = new Aspect(), MINE    = new Aspect(), MOTION  = new Aspect()
		, ORDER     = new Aspect(), PLANT   = new Aspect(), POISON  = new Aspect(), SENSES  = new Aspect()
		, SLIME     = new Aspect(), SOUL    = new Aspect(), TAINT   = new Aspect(), TOOL    = new Aspect()
		, TRAP      = new Aspect(), TRAVEL  = new Aspect(), TREE    = new Aspect(), UNDEAD  = new Aspect()
		, VOID      = new Aspect(), WATER   = new Aspect(), WEAPON  = new Aspect(), WEATHER = new Aspect()
		;
}
