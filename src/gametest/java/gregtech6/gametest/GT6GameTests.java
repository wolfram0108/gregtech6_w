/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregtech6.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Регистрация GameTest'ов мода (проверка механик в РЕАЛЬНОМ мире через {@code runGameTestServer}).
 *
 * <p><b>ОСНАСТКА ПРОВЕРКИ, не часть поставки.</b> Каталог {@code src/gametest/java} подключается к сборке
 * только флагом {@code -Pgt6probes} — тем же, что и стенды. Без флага эти классы не компилируются вовсе,
 * значит и в jar игрока их нет.</p>
 *
 * <p>Класс АВТОНОМЕН: подписывается на мод-шину сам ({@code @EventBusSubscriber}), поэтому production-код
 * о нём не знает ни строчкой. Прежде регистрация звалась из конструктора {@code GT_API} — из-за этого
 * оснастка лежала в продуктовом дереве и уезжала в поставку.</p>
 *
 * <p>Тип-codec регистрируется через {@code RegisterEvent} (а не через собственный {@code DeferredRegister}):
 * так классу не нужна ссылка на шину, которую больше некому передать. {@code RegisterGameTestsEvent}
 * стреляет только под gametest-запуск.</p>
 *
 * <p>Структура тест-региона — {@code gregtech6:gt6_platform} (9x5x9 с полом), окружение — своё пустое.</p>
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = "gregtech6")
public final class GT6GameTests {
	private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath("gregtech6", "gt6_platform");

	private GT6GameTests() {}

	@net.neoforged.bus.api.SubscribeEvent
	static void onRegisterTypes(net.neoforged.neoforge.registries.RegisterEvent aEvent) {
		aEvent.register(Registries.TEST_INSTANCE_TYPE, Identifier.fromNamespaceAndPath("gregtech6", "gt6"), () -> GT6GameTest.CODEC);
	}

	@net.neoforged.bus.api.SubscribeEvent
	static void onRegisterTests(RegisterGameTestsEvent aEvent) {
		Holder<TestEnvironmentDefinition<?>> tEnv = aEvent.registerEnvironment(Identifier.fromNamespaceAndPath("gregtech6", "gt6_env"));
		registerOne(aEvent, tEnv, "block");
		registerOne(aEvent, tEnv, "interact");
		registerOne(aEvent, tEnv, "mte");
	}

	private static void registerOne(RegisterGameTestsEvent aEvent, Holder<TestEnvironmentDefinition<?>> aEnv, String aKind) {
		TestData<Holder<TestEnvironmentDefinition<?>>> tData = new TestData<>(aEnv, STRUCTURE, 400, 0, true);
		aEvent.registerTest(Identifier.fromNamespaceAndPath("gregtech6", "gt6_" + aKind), new GT6GameTest(aKind, tData));
	}
}
