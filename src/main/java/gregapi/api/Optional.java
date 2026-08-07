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

package gregapi.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Переходник F1/F10. Пустышка-замена {@code cpw.mods.fml.common.Optional} из Forge 1.7.10.
 *
 * <p><b>Зачем.</b> В Forge движок при загрузке ВЫРЕЗАЛ у класса реализацию чужого интерфейса
 * ({@code @Optional.Interface}) или отдельный метод ({@code @Optional.Method}), если нужного мода нет —
 * так GregTech6 мог «дружить» с IC2/AE2/… не требуя их. В NeoForge такого вырезания НЕТ.</p>
 *
 * <p><b>Как сохраняем 1:1.</b> Аннотация оставлена как ПУСТОЙ маркер (движок её не обрабатывает), чтобы
 * все 77 мест в 22 файлах остались дословными — меняется лишь адрес импорта на {@code gregapi.api.Optional}.
 * Компиляцию обеспечивает центральное зеркало чужих API (см. {@code decisions/F10-external-mod-compat.md} §3.2);
 * в рантайме без мода эти пути мертвы — все внешние касания GregTech6 под {@code MD.X.mLoaded} (=false).</p>
 *
 * <p><b>Точка возврата.</b> Реальную интеграцию модов дозаполняем позже и централизованно (F10). Тогда
 * эта аннотация и зеркало API заменяются на настоящие зависимости — в одном месте.</p>
 */
public final class Optional {
	private Optional() {}

	/** Класс условно реализует интерфейс {@code iface} мода {@code modid} (в neo — пустой маркер). */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface Interface {
		String iface();
		String modid();
		boolean striprefs() default false;
	}

	/** Список нескольких {@link Interface} на одном классе. */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface InterfaceList {
		Interface[] value();
	}

	/** Метод существует только при наличии мода {@code modid} (в neo — пустой маркер). */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.METHOD)
	public @interface Method {
		String modid();
	}
}
