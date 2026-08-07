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

package ic2.api.crops;

import net.minecraft.util.IIcon;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Было {@code interface CropCard {}} — сломано:
 *  GT_BaseCrop.java:40 делает {@code class GT_BaseCrop extends CropCard} (класс не может
 *  extends интерфейс) — нужен класс. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.crops.CropCard, {@code public abstract class}, упрощено до конкретного класса —
 *  ничего кроме GT_BaseCrop его не наследует, F10 dead-path).
 *  Реально используются: поле {@code textures} — GT_BaseCrop.java:161 (прямая запись, унаследованное
 *  protected-поле); displayName()/attributes()/discoveredBy() — вызываются на {@code ICropTile.getCrop()}
 *  в Behavior_Cropnalyzer.java:84,99,101. Остальные методы реального CropCard (name/tier/stat/maxSize/
 *  canGrow/canBeHarvested/canCross/getGain/rightclick/getOptimalHavestSize/registerSprites/…) в
 *  GT6-исходнике нигде не вызываются НА ссылке типа CropCard (только переопределены в GT_BaseCrop как
 *  собственные методы — {@code // @Override} закомментирован, реального override не требуется) —
 *  не добавлены. */
public class CropCard {
	protected IIcon[] textures;

	public String displayName() {return NB();}
	public String[] attributes() {return null;}
	public String discoveredBy() {return NB();}

	private static String NB() {return null;}
}
