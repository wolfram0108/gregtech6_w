package gregtech6;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Headless-тест в контексте РЕАЛЬНОГО MC-сервера (без графики) — через NeoForge test framework.
 * Поднимает эфемерный сервер с загруженным модом и проверяет, что регистрация контента прошла.
 *
 * <p><b>Что проверялось раньше и почему это было неверно.</b> Тест требовал блок
 * {@code gregtech6:test_block} — артефакт первичной проверки сборки. Этот блок был осознанно удалён при
 * закрытии F12 (регистрация в обход центра — нарушение централизации, см. {@link GregTech6} javadoc), и с
 * тех пор тест краснел на СОБСТВЕННОМ устаревании, а не на дефекте: он стерёг вещь, которой по замыслу нет.</p>
 *
 * <p>Теперь стережётся то, ради чего тест заводился: регистрация идёт через центры мода
 * ({@code GT_API} для блоков/предметов, {@code FluidGT} для жидкостей), и её результат обязан быть в
 * движковых реестрах. Порог — не «хотя бы один»: пустой центр даёт ноль, один живой префикс — сотни, поэтому
 * величина ниже отличает «центр отработал» от «что-то случайно зарегистрировалось».</p>
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ServerContextTest {

	/** Ниже этого — центр регистрации не отрабатывал; замер живого прогона даёт кратно больше. */
	private static final int MIN_REGISTERED = 100;

	@Test
	void modLoadsAndContentIsRegistered(MinecraftServer server) {
		assertNotNull(server, "эфемерный MC-сервер должен подняться");
		GT6TestBoot.ensureLoaded(server);

		int tBlocks = 0, tItems = 0;
		for (net.minecraft.world.level.block.Block tBlock : BuiltInRegistries.BLOCK) {
			net.minecraft.resources.Identifier tKey = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tKey != null && isGT6(tKey.getNamespace())) tBlocks++;
		}
		for (net.minecraft.world.item.Item tItem : BuiltInRegistries.ITEM) {
			net.minecraft.resources.Identifier tKey = BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey != null && isGT6(tKey.getNamespace())) tItems++;
		}
		System.out.println("[SERVER-CTX] зарегистрировано GT6: блоков=" + tBlocks + " предметов=" + tItems);
		assertTrue(tBlocks >= MIN_REGISTERED, "центр регистрации блоков GT6 не отработал: блоков=" + tBlocks);
		assertTrue(tItems >= MIN_REGISTERED, "центр регистрации предметов GT6 не отработал: предметов=" + tItems);
	}

	private static boolean isGT6(String aNamespace) {
		return aNamespace.equals("gregtech") || aNamespace.equals("gregapi") || aNamespace.equals("gregtech6");
	}
}
