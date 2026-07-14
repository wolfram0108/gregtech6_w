package gregtech6.parity;

import org.junit.jupiter.api.Test;

/**
 * Запуск порт-дампера material/prefix + parity-отчёт в FML-runtime (gradle {@code test -PcoreOnly}).
 *
 * <p>Единственный контекст, где заводится {@code FMLLoader} (материал-init GT6 монолитно тянет его через
 * ModData/GT_API — см. {@link PortDump} javadoc). Пока НЕ ассертит 100% (первый прогон, расхождения = сигнал
 * дрейфа/F5). Ассерт-порог добавить, когда срез позеленеет.</p>
 */
class PortDumpTest {

    @Test
    void dumpAndReportParity() throws Exception {
        PortDump.runFull();
    }
}
