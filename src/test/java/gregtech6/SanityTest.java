package gregtech6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Дымовой тест: доказывает, что headless JUnit-харнесс запускается ({@code ./gradlew test})
 * и видит классы мода. Реальные тесты логики порта (материал-математика, унификация,
 * матчинг рецептов) появятся здесь по мере портирования слоёв.
 */
class SanityTest {

    @Test
    void junitHarnessRuns() {
        assertEquals(4, 2 + 2);
    }

    @Test
    void modIdMatchesConvention() {
        assertEquals("gregtech6", GregTech6.MODID);
        // modId должен подходить под регэксп NeoForge: [a-z][a-z0-9_]{1,63}
        assertTrue(GregTech6.MODID.matches("[a-z][a-z0-9_]{1,63}"));
    }
}
