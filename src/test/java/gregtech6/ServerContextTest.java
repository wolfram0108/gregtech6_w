package gregtech6;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Headless-тест в контексте РЕАЛЬНОГО MC-сервера (без графики, без структур .nbt) —
 * через NeoForge test framework. Поднимает эфемерный сервер с загруженным модом и
 * проверяет, что регистрация прошла: наш блок {@code gregtech6:test_block} есть в реестре.
 *
 * <p>Это ключевой headless-инструмент: так можно тестировать логику, которой нужны
 * настоящие реестры/сервер (материалы, рецепты, унификация GT6), не запуская клиент.</p>
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ServerContextTest {

    @Test
    void modLoadsAndBlockIsRegistered(MinecraftServer server) {
        assertNotNull(server, "эфемерный MC-сервер должен подняться");
        assertTrue(
                BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath("gregtech6", "test_block")),
                "блок gregtech6:test_block должен быть зарегистрирован на сервере");
    }
}
