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
