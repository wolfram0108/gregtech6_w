/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregapi.block;

/**
 * ЯРЛЫК цвета карты в именах, которыми оперирует GT6 (палитра 1.7.10).
 *
 * <p><b>Значения цветов здесь НЕ хранятся.</b> Единственный носитель значения — движок:
 * палитра карты у него своя ({@code net.minecraft.world.level.material.MapColor}), адресуется тем же
 * индексом 0..63 и тем же порядком. Этот класс держит только соответствие «имя GT6 → индекс палитры»,
 * потому что мод адресует цвета именами, которых в движке нет.</p>
 *
 * <p>Прежняя редакция несла собственную таблицу RGB — и она была мёртвым грузом: поле значения не
 * читал никто (греп по дереву = 0), фактический цвет и так брался у движка через {@link #toNeo()}.
 * Снятие таблицы поведение не меняет ни на бит и снимает вопрос о происхождении этих чисел.</p>
 *
 * <p>Мост в движок — {@link #toNeo()}, одно место на весь мод (F9-bridge).</p>
 */
public final class MapColor {
	/** Индекс в 64-цветной палитре карты. Совпадает у 1.7.10 и у целевого движка. */
	public final int colorIndex;

	private MapColor(int aIndex) {
		if (aIndex < 0 || aIndex > 63) throw new IndexOutOfBoundsException("Индекс цвета карты обязан лежать в 0..63, дано: " + aIndex);
		colorIndex = aIndex;
	}

	private static MapColor idx(int aIndex) {return new MapColor(aIndex);}

	/** Цвет палитры по индексу — для кода, который адресует цвет числом, а не именем. */
	public static MapColor byId(int aIndex) {return idx(aIndex);}

	/** F9-bridge: ярлык GT6 → цвет движка. Единственная точка перехода на весь мод. */
	public net.minecraft.world.level.material.MapColor toNeo() {
		return net.minecraft.world.level.material.MapColor.byId(colorIndex);
	}

	// Имена — те, которыми пользуется код GT6; число справа — индекс палитры.
	public static final MapColor airColor         = idx( 0);
	public static final MapColor grassColor       = idx( 1);
	public static final MapColor sandColor        = idx( 2);
	public static final MapColor clothColor       = idx( 3);
	public static final MapColor tntColor         = idx( 4);
	public static final MapColor iceColor         = idx( 5);
	public static final MapColor ironColor        = idx( 6);
	public static final MapColor foliageColor     = idx( 7);
	public static final MapColor snowColor        = idx( 8);
	public static final MapColor clayColor        = idx( 9);
	public static final MapColor dirtColor        = idx(10);
	public static final MapColor stoneColor       = idx(11);
	public static final MapColor waterColor       = idx(12);
	public static final MapColor woodColor        = idx(13);
	public static final MapColor quartzColor      = idx(14);
	public static final MapColor adobeColor       = idx(15);
	public static final MapColor magentaColor     = idx(16);
	public static final MapColor lightBlueColor   = idx(17);
	public static final MapColor yellowColor      = idx(18);
	public static final MapColor limeColor        = idx(19);
	public static final MapColor pinkColor        = idx(20);
	public static final MapColor grayColor        = idx(21);
	public static final MapColor silverColor      = idx(22);
	public static final MapColor cyanColor        = idx(23);
	public static final MapColor purpleColor      = idx(24);
	public static final MapColor blueColor        = idx(25);
	public static final MapColor brownColor       = idx(26);
	public static final MapColor greenColor       = idx(27);
	public static final MapColor redColor         = idx(28);
	public static final MapColor blackColor       = idx(29);
	public static final MapColor goldColor        = idx(30);
	public static final MapColor diamondColor     = idx(31);
	public static final MapColor lapisColor       = idx(32);
	public static final MapColor emeraldColor     = idx(33);
	public static final MapColor obsidianColor    = idx(34);
	public static final MapColor netherrackColor  = idx(35);
}
