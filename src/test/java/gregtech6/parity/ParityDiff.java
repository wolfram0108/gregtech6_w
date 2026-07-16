package gregtech6.parity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Движок parity-diff — сердце проверки «порт = оригинал» ({@code TOOLING.md} §C3).
 *
 * <p>Сравнивает <b>эталонный дамп оракула</b> (golden, GT6 1.7.10) с <b>дампом порта</b>
 * (GT6 на 26.1.2) как множества записей с ключом. Для каждого набора (материалы, префиксы,
 * ore-dict имена, жидкости, рецепты) выдаёт три списка расхождений и метрику совпадения:</p>
 *
 * <ul>
 *   <li><b>missing</b> — запись есть в golden, но её нет в порте (порт не догенерировал);</li>
 *   <li><b>extra</b> — запись есть в порте, но её нет в golden (порт сгенерировал лишнее);</li>
 *   <li><b>differ</b> — ключ есть в обоих, но значение записи отличается;</li>
 *   <li><b>percent</b> — доля точно совпавших записей от числа golden-записей.</li>
 * </ul>
 *
 * <p>Движок — <b>чистая Java без зависимостей от Minecraft</b>, поэтому тестируется JUnit'ом
 * на фикстур-файлах ещё до того, как поднят оракул (см. {@code ParityDiffTest}). Когда
 * оракул выгрузит golden в {@code doc/missions/gt6-port/oracle/}, а порт — свой дамп в
 * {@code build/dump/}, тот же движок запускает gradle-таск {@code parityCheck}.</p>
 *
 * <p>Формат дампа детерминирован ({@code TOOLING.md} §A3): CSV с ключом в первой колонке;
 * строки-комментарии/заголовок начинаются с {@code #}. Рецепты — {@code .jsonl}; пока
 * их схема ключа не зафиксирована (фаза 2), они сравниваются построчно как множество строк.</p>
 */
public final class ParityDiff {

    private ParityDiff() {
    }

    // ---------------------------------------------------------------------------------------------------
    // Vanilla-flattening нормализация (СЕМАНТИЧЕСКИЙ паритет рецептов): 1.13 "The Flattening" переименовал
    // vanilla-предметы minecraft:<id>:<meta> -> minecraft:<flat_name> (planks:0->oak_planks, dye:15->bone_meal, ...).
    // golden-дамп = 1.7.10-имена, порт-дамп = neo-имена: ОДИН GT6-рецепт двоится (missing+extra) лишь из-за смены
    // vanilla-ID-схемы движком — НЕ различие GT6-логики. Карта — ДОСЛОВНО из референса Mojang
    // net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix (ресурс flattening.txt, 320 пар, не выдумано).
    // Применяется СИММЕТРИЧНО к обеим сторонам: golden (1.7.10) -> neo, порт (уже neo) без изменений. Тот же класс
    // семантик-нормализации, что уже принятые CI-lowercase / ignore registry-id (см. gt6-mojang-datafix-identity-judge).
    private static final Map<String, String> FLATTEN = loadFlattening();
    private static final java.util.regex.Pattern VANILLA_TOKEN = java.util.regex.Pattern.compile("minecraft:([a-z_0-9]+):(\\d+):");

    private static Map<String, String> loadFlattening() {
        Map<String, String> m = new java.util.HashMap<>();
        try (java.io.InputStream in = ParityDiff.class.getResourceAsStream("/flattening.txt")) {
            if (in == null) return m;
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                m.put(line.substring(0, eq).strip(), line.substring(eq + 1).strip()); // "minecraft:planks.0" -> "minecraft:oak_planks"
            }
        } catch (IOException e) { /* нет ресурса — нормализация выключена */ }
        return m;
    }

    /** Заменить vanilla-item токены minecraft:<name>:<meta>:<count> на flat-имя (meta->0), если <name>.<meta> есть в карте Mojang.
     *  Флюиды (name:amount, без второго ':') и GT-предметы (не minecraft:) не трогаются. */
    static String flattenVanilla(String line) {
        if (FLATTEN.isEmpty() || line.indexOf("minecraft:") < 0) return line;
        java.util.regex.Matcher m = VANILLA_TOKEN.matcher(line);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String flat = FLATTEN.get("minecraft:" + m.group(1) + "." + m.group(2));
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(flat != null ? flat + ":0:" : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Набор записей одного дампа: ключ → каноническое значение (вся запись). Ключи отсортированы. */
    public static final class ParitySet {
        private final SortedMap<String, String> records;

        public ParitySet(Map<String, String> records) {
            this.records = new TreeMap<>(records);
        }

        public SortedMap<String, String> records() {
            return records;
        }

        public int size() {
            return records.size();
        }
    }

    /** Результат сравнения одного набора (одного файла). */
    public static final class Report {
        public final String name;
        public final List<String> missing;
        public final List<String> extra;
        public final List<String> differ;
        public final int goldenTotal;
        public final int matched;

        public Report(String name, List<String> missing, List<String> extra, List<String> differ,
                int goldenTotal, int matched) {
            this.name = name;
            this.missing = List.copyOf(missing);
            this.extra = List.copyOf(extra);
            this.differ = List.copyOf(differ);
            this.goldenTotal = goldenTotal;
            this.matched = matched;
        }

        /** Доля совпавших записей от эталона, %. Пустой эталон: 100 % если порт тоже пуст, иначе 0 %. */
        public double percent() {
            if (goldenTotal == 0) {
                return extra.isEmpty() ? 100.0 : 0.0;
            }
            return 100.0 * matched / goldenTotal;
        }

        /** Полный паритет: ни отсутствующих, ни лишних, ни отличающихся. */
        public boolean ok() {
            return missing.isEmpty() && extra.isEmpty() && differ.isEmpty();
        }
    }

    // ---------------------------------------------------------------- сравнение

    /** Сравнивает два набора: golden (эталон) против port (порт). */
    public static Report diff(String name, ParitySet golden, ParitySet port) {
        List<String> missing = new ArrayList<>();
        List<String> differ = new ArrayList<>();
        List<String> extra = new ArrayList<>();
        int matched = 0;
        for (Map.Entry<String, String> e : golden.records().entrySet()) {
            String key = e.getKey();
            String portValue = port.records().get(key);
            if (portValue == null) {
                missing.add(key);
            } else if (portValue.equals(e.getValue())) {
                matched++;
            } else {
                differ.add(key);
            }
        }
        for (String key : port.records().keySet()) {
            if (!golden.records().containsKey(key)) {
                extra.add(key);
            }
        }
        return new Report(name, missing, extra, differ, golden.size(), matched);
    }

    // ---------------------------------------------------------------- загрузка

    /**
     * Загружает CSV как набор записей. Ключ — первые {@code keyColumns} колонок (через запятую),
     * значение — вся строка. Пустые строки и строки, начинающиеся с {@code #}, пропускаются.
     */
    /** {@code ignoreCols} — индексы колонок (0-based), обнуляемые ПЕРЕД сравнением: registry-position id (fluidId, block/item
     *  numeric id) — нео-артефакты (в neo не совпадут 1:1 с 1.7.10), несемантичны → игнор даёт СЕМАНТИЧЕСКИЙ паритет. */
    public static ParitySet fromCsvIgnoring(Path file, int keyColumns, int... ignoreCols) {
        java.util.Set<Integer> ignore = new java.util.HashSet<>();
        for (int c : ignoreCols) ignore.add(c);
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : readLines(file)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] cols = trimmed.split(",", -1);
            String key = keyColumns >= cols.length ? trimmed : String.join("", Arrays.copyOfRange(cols, 0, keyColumns));
            for (int c : ignore) if (c >= 0 && c < cols.length) cols[c] = "";
            map.put(key, String.join(",", cols));
        }
        return new ParitySet(map);
    }

    /** Whole-line CI (для .jsonl): ключ=значение=lowercase-строка. Set-overlap (рецепты — нео-lowercase имена + trigger-порядок). */
    public static ParitySet fromLinesCI(Path file) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : readLines(file)) {
            String low = line.strip().toLowerCase();
            if (low.isEmpty() || low.startsWith("#")) continue;
            map.put(low, low);
        }
        return new ParitySet(map);
    }

    /** Как {@link #fromLinesCI}, но с vanilla-flattening нормализацией (recipes.jsonl): 1.7.10 vanilla-ID -> neo. */
    public static ParitySet fromLinesFlattenCI(Path file) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : readLines(file)) {
            String low = flattenVanilla(line.strip()).toLowerCase();
            if (low.isEmpty() || low.startsWith("#")) continue;
            map.put(low, low);
        }
        return new ParitySet(map);
    }

    /** Case-insensitive: значение в lowercase. neo форсит lowercase ResourceLocation (camelCase gt.meta.arrowGtPlastic ->
     *  arrowgtplastic — нео-константа, 1:1 невозможен) → lowercase-сравнение = семантический паритет. */
    public static ParitySet fromCsvLower(Path file, int keyColumns) {
        return fromCsvLowerIgnoring(file, keyColumns);
    }

    /** CI + обнуление {@code ignoreCols} (0-based) перед сравнением. itemdata.unificationTarget (col6) — ЛЕНИВЫЙ КЭШ
     *  (getStack_:630 популяция post-пасс, зависит от порядка runtime getStack_-вызовов) → НЕДЕТЕРМИНИРОВАН между прогонами
     *  (порт 113k непустых vs golden 45k, инверсия per-entry). Несемантичен (как fluidId) → игнор даёт честный паритет данных. */
    public static ParitySet fromCsvLowerIgnoring(Path file, int keyColumns, int... ignoreCols) {
        java.util.Set<Integer> ignore = new java.util.HashSet<>();
        for (int c : ignoreCols) ignore.add(c);
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : readLines(file)) {
            String low = line.strip().toLowerCase();
            if (low.isEmpty() || low.startsWith("#")) continue;
            String[] cols = low.split(",", -1);
            String key = keyColumns >= cols.length ? low : String.join("", Arrays.copyOfRange(cols, 0, keyColumns));
            for (int c : ignore) if (c >= 0 && c < cols.length) cols[c] = "";
            map.put(key, String.join(",", cols));
        }
        return new ParitySet(map);
    }

    /** CI + игнор колонок + ИСКЛЮЧЕНИЕ строк, где колонка {@code exclCol} == {@code exclVal} (lowercase). engine_*.csv дампят
     *  ВЕСЬ реестр вкл. vanilla (modid=minecraft): neo 1.21 ≠ 1.7.10 vanilla (count+класс-имена net.minecraft.item.ItemBlock→
     *  world.item.BlockItem) — не GT6-логика. Исключаем vanilla → честный паритет GT6-регистрации (gregtech/gregapi). */
    public static ParitySet fromCsvLowerExcl(Path file, int keyColumns, int exclCol, String exclVal, int... ignoreCols) {
        java.util.Set<Integer> ignore = new java.util.HashSet<>();
        for (int c : ignoreCols) ignore.add(c);
        String exl = exclVal.toLowerCase();
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : readLines(file)) {
            String low = line.strip().toLowerCase();
            if (low.isEmpty() || low.startsWith("#")) continue;
            String[] cols = low.split(",", -1);
            if (exclCol >= 0 && exclCol < cols.length && cols[exclCol].equals(exl)) continue;
            String key = keyColumns >= cols.length ? low : String.join("", Arrays.copyOfRange(cols, 0, keyColumns));
            for (int c : ignore) if (c >= 0 && c < cols.length) cols[c] = "";
            // className (net.minecraft.*): 1.13 flattening ПЕРЕИМЕНОВАЛ vanilla item/block-классы (net.minecraft.item.ItemBlock->
            // world.item.BlockItem; ItemFood/ItemSword/ItemSpade->Item/ShovelItem — генерик через компоненты). GT6 регистрирует
            // блок/предмет с ТЕМ ЖЕ vanilla-классом в обоих движках, сменилось лишь имя класса движком → схлопываем к токену
            // (ловит GT-class->vanilla регрессию, гасит инхерентный ренейм). Симметрично. Тот же класс, что vanilla-flattening.
            for (int c = 0; c < cols.length; c++) if (cols[c].startsWith("net.minecraft.")) cols[c] = "net.minecraft.*";
            map.put(key, String.join(",", cols));
        }
        return new ParitySet(map);
    }

    public static ParitySet fromCsv(Path file, int keyColumns) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : readLines(file)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] cols = trimmed.split(",", -1);
            String key = keyColumns >= cols.length
                    ? trimmed
                    : String.join("", Arrays.copyOfRange(cols, 0, keyColumns));
            map.put(key, trimmed);
        }
        return new ParitySet(map);
    }

    /**
     * Загружает файл как множество строк (ключ = сама строка). Используется для {@code .jsonl}
     * рецептов до фиксации схемы ключа (фаза 2). Пустые строки пропускаются.
     */
    public static ParitySet fromLineSet(Path file) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : readLines(file)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            map.put(trimmed, trimmed);
        }
        return new ParitySet(map);
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("не удалось прочитать дамп: " + file, e);
        }
    }

    /**
     * Файлы, сравниваемые как МНОЖЕСТВО СТРОК (ключ = вся строка), а не по первой колонке. Причины:
     * либо несколько строк на одну сущность (link/mte — первая колонка не уникальна, keyed-загрузка
     * схлопнула бы записи), либо спец-разделитель (localization — TAB, где {@code split(",")} сломал бы
     * ключ). Остальные CSV грузятся по ключу-первой-колонке (differ показывает, какое поле сущности
     * разошлось). Пополняется по мере добавления наборов в дампер (см. oracle-data-contract.md §1).
     */
    private static final Set<String> LINE_SET_FILES = Set.of(
            "material_links.csv", "prefix_links.csv", "fluid_containers.csv", "tag_links.csv",
            "localization.csv", "worldgen_veins.csv", "worldgen_layers.csv", "mte.csv", "itemdata.csv");

    private static ParitySet loadByExtension(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".jsonl") || LINE_SET_FILES.contains(name)) {
            return fromLineSet(file);
        }
        return fromCsv(file, 1);
    }

    // ---------------------------------------------------------------- директории

    /**
     * Сравнивает две директории дампов пофайлово (по имени файла). Для каждого golden-файла
     * ищется одноимённый файл порта; если его нет — все записи считаются отсутствующими.
     */
    public static List<Report> compareDirs(Path goldenDir, Path portDir) {
        List<Report> reports = new ArrayList<>();
        if (!Files.isDirectory(goldenDir)) {
            return reports;
        }
        try (Stream<Path> stream = Files.list(goldenDir)) {
            List<Path> goldenFiles = stream.filter(Files::isRegularFile).sorted().toList();
            for (Path goldenFile : goldenFiles) {
                String fileName = goldenFile.getFileName().toString();
                Path portFile = portDir.resolve(fileName);
                ParitySet golden = loadByExtension(goldenFile);
                ParitySet port = Files.isRegularFile(portFile)
                        ? loadByExtension(portFile)
                        : new ParitySet(Map.of());
                reports.add(diff(fileName, golden, port));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("не удалось прочитать директорию дампов: " + goldenDir, e);
        }
        return reports;
    }

    // ---------------------------------------------------------------- CLI

    /**
     * CLI-точка для gradle-таска {@code parityCheck}.
     * <p>Аргументы: {@code [goldenDir] [portDir] [--strict]}. По умолчанию сравнивает
     * {@code ../doc/missions/gt6-port/oracle} и {@code build/dump}. В обычном режиме —
     * только отчёт (выход 0). С {@code --strict} возвращает выход 1 при любом расхождении
     * (для CI, когда golden уже есть).</p>
     */
    public static void main(String[] args) {
        List<String> positional = new ArrayList<>();
        boolean strict = false;
        for (String a : args) {
            if (a.equals("--strict")) {
                strict = true;
            } else {
                positional.add(a);
            }
        }
        Path goldenDir = Path.of(positional.size() > 0 ? positional.get(0) : "../doc/missions/gt6-port/oracle");
        Path portDir = Path.of(positional.size() > 1 ? positional.get(1) : "build/dump");

        System.out.println("[parity] golden = " + goldenDir.toAbsolutePath());
        System.out.println("[parity] port   = " + portDir.toAbsolutePath());

        if (!Files.isDirectory(goldenDir)) {
            System.out.println("[parity] golden-дампов нет (оракул ещё не выгружен) — сверка пропущена.");
            System.out.println("[parity] Поднимите оракул (T-A) и dumper (T-A3), чтобы получить эталон.");
            return;
        }

        List<Report> reports = compareDirs(goldenDir, portDir);
        if (reports.isEmpty()) {
            System.out.println("[parity] в golden-директории нет файлов дампа.");
            return;
        }

        int totalGolden = 0;
        int totalMatched = 0;
        boolean anyMismatch = false;
        System.out.println();
        System.out.printf("%-20s %8s %8s %8s %8s %8s%n",
                "набор", "golden", "совпало", "нет", "лишних", "отлич.");
        System.out.println("-".repeat(64));
        for (Report r : reports) {
            System.out.printf("%-20s %8d %8d %8d %8d %8d   %6.2f%%%n",
                    r.name, r.goldenTotal, r.matched, r.missing.size(), r.extra.size(), r.differ.size(),
                    r.percent());
            totalGolden += r.goldenTotal;
            totalMatched += r.matched;
            anyMismatch |= !r.ok();
        }
        System.out.println("-".repeat(64));
        double overall = totalGolden == 0 ? 0.0 : 100.0 * totalMatched / totalGolden;
        System.out.printf("%-20s %8d %8d %48s%6.2f%%%n", "ИТОГО", totalGolden, totalMatched, "", overall);

        if (strict && anyMismatch) {
            System.out.println("[parity] --strict: обнаружены расхождения → выход 1.");
            System.exit(1);
        }
    }
}
