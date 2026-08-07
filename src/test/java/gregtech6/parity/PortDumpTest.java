package gregtech6.parity;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Запуск порт-дампера material/prefix + parity-отчёт в FML-runtime с ЭФЕМЕРНЫМ СЕРВЕРОМ.
 *
 * <p>{@link EphemeralTestServerProvider} стартует настоящий (headless) MC-сервер: мод грузится → server-start →
 * GT6 {@code deferItemInit}-pipeline выполняется (регистрация OreDict/targets, привязка Holder.components жидкостей —
 * всё это возможно только на server-start, см. STATE.md «ФУНДАМЕНТАЛЬНАЯ КОРРЕКЦИЯ»). ТОЛЬКО в этом контексте дамп
 * материалов/префиксов полон (registeredMaterialsCount/registeredItemsCount, fluid-колонки заполнены).</p>
 *
 * <p>Пока НЕ ассертит 100% (расхождения = сигнал реального дрейфа/тихих data-drop). Ассерт-порог — когда срез позеленеет.</p>
 */
/*
 * ОСНАСТКА ФАЗЫ ПОРТА, а не проверка поставки. Отвечает на вопрос «совпадает ли с оригиналом 1.7.10»,
 * для чего нужны дампы живого оригинала (~200 МБ, в репозиторий не входят). Этап порта закрыт, вопрос
 * больше не задаётся на каждый прогон — тег выводит тест из обычного `gradlew test`. Запуск по требованию:
 *     gradlew test -Pgt6.oracle=<путь к дампам оригинала>
 */
@Tag("parity")
@ExtendWith(EphemeralTestServerProvider.class)
class PortDumpTest {

    @Test
    void dumpAndReportParity(MinecraftServer server) throws Exception {
        // server-параметр → EphemeralTestServer стартует сервер (deferItemInit-pipeline выполнен, компоненты привязаны) ДО дампа.
        PortDump.runFull(server);
    }
}
