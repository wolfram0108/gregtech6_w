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

package gregtech6.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import gregtech6.parity.ParityDiff.ParitySet;
import gregtech6.parity.ParityDiff.Report;

/**
 * Доказывает логику движка parity-diff на фикстур-файлах (без оракула, без Minecraft).
 * Когда оракул выгрузит настоящий golden — тот же движок сравнивает реальные наборы.
 */
class ParityDiffTest {

    /** Резолвит фикстуру из test-ресурсов в {@link Path}. */
    private Path fixture(String classpathPath) throws Exception {
        URL url = getClass().getClassLoader().getResource(classpathPath);
        assertNotNull(url, "фикстура не найдена на classpath: " + classpathPath);
        return Path.of(url.toURI());
    }

    @Test
    void detectsMissingExtraAndDiffer() throws Exception {
        ParitySet golden = ParityDiff.fromCsv(fixture("parity/golden/materials.csv"), 1);
        ParitySet port = ParityDiff.fromCsv(fixture("parity/dump/materials.csv"), 1);

        Report r = ParityDiff.diff("materials.csv", golden, port);

        assertEquals(3, r.goldenTotal, "в эталоне 3 записи");
        assertEquals(1, r.matched, "совпала только запись id=1");
        assertEquals(List.of("3"), r.missing, "id=3 (Gold) есть в эталоне, нет в порте");
        assertEquals(List.of("4"), r.extra, "id=4 (Tin) есть в порте, нет в эталоне");
        assertEquals(List.of("2"), r.differ, "id=2 (Copper) отличается значением (масса 64 vs 63)");
        assertEquals(33.33, r.percent(), 0.01, "1 из 3 → 33.33 %");
        assertFalse(r.ok(), "паритета нет");
    }

    @Test
    void identicalSetsGiveFullParity() throws Exception {
        Path same = fixture("parity/golden/materials.csv");
        Report r = ParityDiff.diff("materials.csv", ParityDiff.fromCsv(same, 1), ParityDiff.fromCsv(same, 1));

        assertEquals(3, r.matched);
        assertEquals(100.0, r.percent(), 0.0001);
        assertTrue(r.ok(), "идентичные наборы → полный паритет");
        assertTrue(r.missing.isEmpty() && r.extra.isEmpty() && r.differ.isEmpty());
    }

    @Test
    void emptyPortGivesZeroParity() throws Exception {
        ParitySet golden = ParityDiff.fromCsv(fixture("parity/golden/materials.csv"), 1);
        ParitySet emptyPort = new ParitySet(Map.of());

        Report r = ParityDiff.diff("materials.csv", golden, emptyPort);

        assertEquals(0, r.matched);
        assertEquals(3, r.missing.size(), "весь эталон отсутствует в пустом порте");
        assertEquals(0.0, r.percent(), 0.0001);
        assertFalse(r.ok());
    }
}
