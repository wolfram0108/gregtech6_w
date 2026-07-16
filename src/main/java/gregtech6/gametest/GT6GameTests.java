package gregtech6.gametest;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Регистрация GameTest'ов мода GT6 (проверка механик в РЕАЛЬНОМ мире через {@code runGameTestServer}).
 * Подключается из конструктора GT_API на mod-шину (как остальные register). {@link RegisterGameTestsEvent}
 * стреляет только при включённом gametest (dev), в проде безвреден; тип-codec — обычная запись реестра.
 * Структура тест-региона — {@code gregtech6:gt6_platform} (9x5x9 с полом), окружение — своё пустое.
 */
public final class GT6GameTests {
	public static final DeferredRegister<MapCodec<? extends GameTestInstance>> TYPES =
			DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, "gregtech6");
	public static final Supplier<MapCodec<GT6GameTest>> GT6_TYPE = TYPES.register("gt6", () -> GT6GameTest.CODEC);

	private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath("gregtech6", "gt6_platform");

	public static void register(IEventBus aModBus) {
		TYPES.register(aModBus);
		aModBus.addListener(GT6GameTests::onRegisterTests);
	}

	private static void onRegisterTests(RegisterGameTestsEvent aEvent) {
		Holder<TestEnvironmentDefinition<?>> tEnv = aEvent.registerEnvironment(Identifier.fromNamespaceAndPath("gregtech6", "gt6_env"));
		registerOne(aEvent, tEnv, "block");
		registerOne(aEvent, tEnv, "interact");
	}

	private static void registerOne(RegisterGameTestsEvent aEvent, Holder<TestEnvironmentDefinition<?>> aEnv, String aKind) {
		TestData<Holder<TestEnvironmentDefinition<?>>> tData = new TestData<>(aEnv, STRUCTURE, 400, 0, true);
		aEvent.registerTest(Identifier.fromNamespaceAndPath("gregtech6", "gt6_" + aKind), new GT6GameTest(aKind, tData));
	}
}
