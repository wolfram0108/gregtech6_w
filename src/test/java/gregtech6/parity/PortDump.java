package gregtech6.parity;

import gregapi.data.MT;
import gregapi.data.FL;
import gregapi.data.CS;
import gregapi.fluid.FluidGT;
import gregapi.code.TagData;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.block.multitileentity.MultiTileEntityClassContainer;
import gregapi.worldgen.WorldgenOresLarge;
import gregapi.worldgen.StoneLayer;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.tools.IToolStats;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;

import net.minecraft.SharedConstants;
import net.minecraft.DetectedVersion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * Порт-дампер material/prefix + parity-прогон против golden-оракула (первый реальный судья критерия «паритет»).
 *
 * <p>ЗЕРКАЛО golden-дампера ({@code gt6-oracle-dumper/DumpMaterials + DumpPrefixes + DumpUtil}) — те же поля,
 * тот же формат/порядок колонок → строки сравнимы движком {@link ParityDiff}. Заполнение материалов/префиксов
 * (MT/OP static-init) — чистые данные, БЕЗ мира; единственная внешняя точка {@code OP:576 Items.GLASS_BOTTLE}
 * снимается {@link Bootstrap#bootStrap()} (ванильные реестры headless, без сервера).</p>
 *
 * <p><b>⚠️ ЗАПУСК ТРЕБУЕТ FML-runtime.</b> Эмпирически (2026-07-15): standalone {@code java -cp} НЕ работает —
 * патченный MC-jar ({@code SharedConstants.clinit->FMLLoader}) + монолит GT6: material-init тянет
 * {@code OreDictMaterial-><init>->ModData(ModList.get() null)->GT_API.clinit->FMLEnvironment.getDist()->FMLLoader} —
 * всё требует FML-launch-контекста (принцип 1, всё связано). Логика дампа/формата ВЕРНА (компилируется, зеркалит
 * golden-дампер), но запускать — только в FML-runtime: gradle {@code test} (unitTest, даёт FMLLoader). А тот требует,
 * чтобы {@code compileJava} ВСЕГО src/main прошёл (сейчас контент-слой валит 2860 ошибок) ЛИБО main-sourceSet временно
 * сужен до ядра-692+mirror. Тогда обернуть в JUnit {@code @Test} с ассерт-порогом. См. STATE.md «ПАРИТЕТ-ОСНАСТКА».</p>
 */
public final class PortDump {

    private static final Path DUMP = Path.of("build", "dump");
    private static final Path ORACLE = Path.of("D:/Temp/MC_NEW/.claude/doc/missions/gt6-port/oracle");

    private PortDump() {}

    public static void main(String[] args) throws Exception {
        runFull();
    }

    /**
     * Полный дамп material+prefix + parity-отчёт. ТРЕБУЕТ FML-runtime (см. класс-javadoc) — вызывать из
     * gradle-теста ({@code ./gradlew test -PcoreOnly}), где FMLLoader инициализирован.
     */
    public static void runFull() throws Exception {
        // В FML-контексте (coreOnly test) SharedConstants/Bootstrap УЖЕ инициализированы загрузкой мода — guard от
        // "Cannot override the current game version!". Standalone (без FML) — инициализируем сами.
        try {SharedConstants.setVersion(DetectedVersion.BUILT_IN); Bootstrap.bootStrap();} catch (Throwable e) {System.out.println("[port-dump] bootstrap уже сделан FML: " + e);}
        MT.init();                          // static-init MT (материалы) + init() — идемпотентно (мод уже инициализировал)
        Class.forName("gregapi.data.OP");   // static-init OP (префиксы)
        // Выполнить отложенный data-init (loaders/targets/associations — заполняют registeredMaterialsCount/registeredItemsCount,
        // fluid-компоненты). В @Test (после EphemeralTestServer server-start) компоненты привязаны → ST.make работает. onModServerStarting2
        // в тесте не срабатывает, потому зовём явно здесь.
        gregapi.GT_API.runDeferredItemInit();

        Files.createDirectories(DUMP);
        int nMat = dumpMaterials();
        int nPre = dumpPrefixes();
        int nFl = dumpFluids();
        dumpMTE(); dumpTools(); dumpTags(); dumpWorldgen();
        dumpOreDict(); dumpUnification(); dumpLocalization();
        System.out.println("[port-dump] materials=" + nMat + " prefixes=" + nPre + " fluids=" + nFl);

        double tMat = report("materials.csv");
        double tPre = report("prefixes.csv");
        report("fluids.csv", 1, 1); // игнор col1=fluidId (нео-артефакт: source+flowing = 2× id 1.7.10)
        report("mte.csv", 4);           // ключ = все 4 колонки (нет стабильного одиночного ключа)
        report("tools.csv", 1);
        report("tool_types.csv", 1);
        report("tags.csv", 1);
        report("tag_links.csv", 2);     // ключ = object+objectType
        report("worldgen_veins.csv", 1);
        report("worldgen_layers.csv", 1);
        reportCI("oredict.csv", 1);      // CI: neo lowercase ResourceLocation-имена (camelCase gt.meta.* невозможен)
        reportCI("unification.csv", 1);
        report("localization.csv", 1);
        // РЕГРЕСС-ГЕЙТ: судья валидировал core-scalar-данные (~99.8%); текущий full-паритет coreOnly — materials 85.19% / prefixes
        // 46.58% (остаток = content-зависимые fluid/registeredCounts, см. STATE). Порог = текущий floor: тест ПАДАЕТ при регрессе
        // core-данных. Поднимать floor по мере закрытия контент-слоя (рост fluid/registered паритета).
        if (tMat < 85.0) throw new AssertionError("РЕГРЕСС material-паритета: " + tMat + "% < 85.0% (core-данные сломаны)");
        if (tPre < 46.0) throw new AssertionError("РЕГРЕСС prefix-паритета: " + tPre + "% < 46.0% (core-данные сломаны)");
        System.out.println("[parity] РЕГРЕСС-ГЕЙТ ОК: materials " + tMat + "% >= 85.0, prefixes " + tPre + "% >= 46.0");
    }

    // ------------------------------------------------------------------ materials.csv (зеркало DumpMaterials)
    private static int dumpMaterials() throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreDictMaterial m : OreDictMaterial.MATERIAL_MAP.values()) {
            if (m == null) continue;
            // F5-lazy: материализовать mLiquid/mGas/mPlasma перед чтением. Резилиентно: GT6-fluid-компоненты привязаны только
            // на server-start; без server (plain @Test) FluidStack не создать → пропускаем fluid-колонки (пусты), scalar-колонки
            // всех 2215 материалов дампятся. Fluid-паритет — при EphemeralTestServer (см. STATE).
            try {m.materializeFluids();} catch (Throwable e) {/* компоненты не привязаны (нет server) — fluid-колонки пусты */}
            StringBuilder sb = new StringBuilder(256);
            sb.append(m.mNameInternal).append(',');
            sb.append(m.mID).append(',');
            sb.append(m.mGramPerCubicCentimeter).append(',');
            sb.append(m.mMeltingPoint).append(',');
            sb.append(m.mBoilingPoint).append(',');
            sb.append(m.mPlasmaPoint).append(',');
            sb.append(m.mNeutrons).append(',');
            sb.append(m.mProtons).append(',');
            sb.append(m.mElectrons).append(',');
            sb.append(m.mMass).append(',');
            sb.append(m.mToolTypes).append(',');
            sb.append(m.mToolQuality).append(',');
            sb.append(m.mToolDurability).append(',');
            sb.append(m.mToolSpeed).append(',');
            sb.append(m.mHeatDamage).append(',');
            sb.append(m.mOreMultiplier).append(',');
            sb.append(m.mOreProcessingMultiplier).append(',');
            sb.append(m.mFurnaceBurnTime).append(',');
            sb.append(m.mHidden).append(',');
            sb.append(m.mHasMetallum).append(',');
            sb.append(rgba(m.mRGBaSolid)).append(',');
            sb.append(rgba(m.mRGBaLiquid)).append(',');
            sb.append(rgba(m.mRGBaGas)).append(',');
            sb.append(rgba(m.mRGBaPlasma)).append(',');
            sb.append(m.mPriorityPrefix == null ? "" : m.mPriorityPrefix.mNameInternal).append(',');
            sb.append(matName(m.mTargetReversing)).append(',');
            sb.append(matName(m.mTargetRegistration)).append(',');
            sb.append(matName(m.mHandleMaterial)).append(',');
            sb.append(m.mLiquidUnit).append(',');
            sb.append(m.mGasUnit).append(',');
            sb.append(m.mPlasmaUnit).append(',');
            sb.append(fluidName(m.mLiquid)).append(',');
            sb.append(fluidName(m.mGas)).append(',');
            sb.append(fluidName(m.mPlasma)).append(',');
            sb.append(csvField(m.mNameLocal)).append(',');
            sb.append(csvField(m.mTooltipChemical));
            lines.add(sb.toString());
        }
        writeCsv("materials.csv",
                "mNameInternal,mID,gramPerCC,meltK,boilK,plasmaK,neutrons,protons,electrons,mass,"
                + "toolTypes,toolQuality,toolDurability,toolSpeed,heatDamage,oreMul,oreProcMul,"
                + "furnaceBurnTime,hidden,hasMetallum,rgbSolid,rgbLiquid,rgbGas,rgbPlasma,"
                + "priorityPrefix,targetReversing,targetRegistration,handleMaterial,liquidUnit,gasUnit,"
                + "plasmaUnit,liquid,gas,plasma,nameLocal,tooltipChemical",
                lines);
        return lines.size();
    }

    // ------------------------------------------------------------------ prefixes.csv (зеркало DumpPrefixes)
    private static int dumpPrefixes() throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreDictPrefix p : OreDictPrefix.VALUES) {
            if (p == null) continue;
            lines.add(p.mNameInternal + "," + p.mAmount + "," + p.mWeight + "," + p.mState + ","
                    + p.mConfigStackSize + "," + p.mDefaultStackSize + "," + p.mMinimumStackSize + ","
                    + p.mFamiliarPrefixes.size() + "," + p.mByProducts.size() + ","
                    + p.mRegisteredMaterials.size() + "," + p.mRegisteredItems.size());
        }
        writeCsv("prefixes.csv",
                "mNameInternal,mAmount,mWeight,mState,configStackSize,defaultStackSize,minimumStackSize,"
                + "familiarCount,byproductCount,registeredMaterialsCount,registeredItemsCount", lines);
        return lines.size();
    }

    // ------------------------------------------------------------------ fluids.csv (зеркало DumpFluids)
    private static int dumpFluids() throws IOException {
        // F5-шов: в neo FluidGT НЕ extends Fluid (отдельный holder). golden читал mRGBa рефлексией с Fluid-объекта
        // (1.7.10 Fluid == FluidGT); порт-эквивалент — FluidGT.BY_NAME по имени жидкости + публичный getRGBa().
        List<String> lines = new ArrayList<>();
        for (FL fl : FL.values()) {
            try {
                Fluid f = fl.fluid();
                int id = -1; long temp = 0; boolean gas = false;
                if (f != null) { id = fl.id(); temp = FL.temperature(f); gas = FL.gas(f); }
                List<String> allNames = new ArrayList<>();
                if (fl.mAllNames != null) for (String n : fl.mAllNames) allNames.add(n);
                String rgbaStr = "";
                if (f != null) { FluidGT tGT = FluidGT.BY_NAME.get(fl.mName.toLowerCase()); if (tGT != null) rgbaStr = rgba(tGT.getRGBa()); }
                lines.add(fl.mName + "," + id + "," + temp + "," + gas + "," + String.join("|", allNames) + "," + rgbaStr);
            } catch (Throwable t) {}
        }
        writeCsv("fluids.csv", "mName,fluidId,temperatureK,gaseous,allNames,rgba", lines);
        return lines.size();
    }

    // ------------------------------------------------------------------ mte.csv (зеркало DumpRegistry)
    @SuppressWarnings("unchecked")
    private static int dumpMTE() throws IOException {
        List<String> lines = new ArrayList<>();
        try {
            Field f = MultiTileEntityRegistry.class.getDeclaredField("NAMED_REGISTRIES");
            f.setAccessible(true);
            java.util.Map<String, MultiTileEntityRegistry> regs = (java.util.Map<String, MultiTileEntityRegistry>) f.get(null);
            for (java.util.Map.Entry<String, MultiTileEntityRegistry> e : regs.entrySet()) {
                MultiTileEntityRegistry reg = e.getValue();
                if (reg == null || reg.mRegistry == null) continue;
                String rn = e.getKey() == null ? "" : e.getKey();
                for (MultiTileEntityClassContainer c : reg.mRegistry.values())
                    lines.add(rn + "," + c.mID + "," + c.mBlockMetaData + "," + (c.mClass == null ? "" : c.mClass.getName()));
            }
        } catch (Throwable t) {}
        writeCsv("mte.csv", "registry,id,blockMetaData,className", lines);
        return lines.size();
    }

    // ------------------------------------------------------------------ tools.csv + tool_types.csv
    private static int dumpTools() throws IOException {
        List<String> lines = new ArrayList<>();
        try {
            MultiItemTool meta = CS.ToolsGT.sMetaTool;
            if (meta != null && meta.mToolStats != null)
                for (java.util.Map.Entry<Short, IToolStats> e : meta.mToolStats.entrySet())
                    lines.add(e.getKey() + "," + (e.getValue() == null ? "" : e.getValue().getClass().getName()));
        } catch (Throwable t) {}
        writeCsv("tools.csv", "id,toolStatsClass", lines);
        List<String> tt = new ArrayList<>();
        try {
            for (Field field : CS.ToolsGT.class.getDeclaredFields()) {
                int m = field.getModifiers();
                if (!Modifier.isStatic(m) || !Modifier.isPublic(m) || field.getType() != short.class) continue;
                try { tt.add(field.getName() + "," + field.getShort(null)); } catch (Throwable t) {}
            }
        } catch (Throwable t) {}
        writeCsv("tool_types.csv", "name,id", tt);
        return lines.size();
    }

    // ------------------------------------------------------------------ tags.csv + tag_links.csv
    private static int dumpTags() throws IOException {
        List<String> lines = new ArrayList<>();
        for (TagData td : TagData.TAGS) lines.add(td.mTagID + "," + td.mName + "," + esc(td.mChatFormat).replace(",", " "));
        writeCsv("tags.csv", "mTagID,mName,chatFormat", lines);
        List<String> tl = new ArrayList<>();
        for (OreDictMaterial m : OreDictMaterial.MATERIAL_MAP.values()) try {
            List<String> tn = new ArrayList<>(); for (TagData td : TagData.TAGS) if (m.contains(td)) tn.add(td.mName); Collections.sort(tn);
            if (!tn.isEmpty()) tl.add(m.mNameInternal + ",material," + String.join("|", tn));
        } catch (Throwable t) {}
        for (OreDictPrefix p : OreDictPrefix.VALUES) { if (p == null) continue; try {
            List<String> tn = new ArrayList<>(); for (TagData td : TagData.TAGS) if (p.contains(td)) tn.add(td.mName); Collections.sort(tn);
            if (!tn.isEmpty()) tl.add(p.mNameInternal + ",prefix," + String.join("|", tn));
        } catch (Throwable t) {} }
        writeCsv("tag_links.csv", "object,objectType,tags", tl);
        return lines.size();
    }

    // ------------------------------------------------------------------ worldgen_veins.csv + worldgen_layers.csv
    private static int dumpWorldgen() throws IOException {
        java.util.Set<WorldgenOresLarge> veins = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Field fld : CS.class.getDeclaredFields()) {
            if (!Modifier.isStatic(fld.getModifiers()) || !List.class.isAssignableFrom(fld.getType())) continue;
            try { fld.setAccessible(true); Object v = fld.get(null);
                if (v instanceof List) for (Object o : (List<?>) v) if (o instanceof WorldgenOresLarge) veins.add((WorldgenOresLarge) o);
            } catch (Throwable t) {}
        }
        List<String> vl = new ArrayList<>();
        for (WorldgenOresLarge v : veins) vl.add((v.mName==null?"":v.mName) + "," + matName(v.mTop) + "," + matName(v.mBottom) + "," + matName(v.mBetween)
                + "," + matName(v.mSpread) + "," + v.mWeight + "," + v.mDistance + "," + v.mMinY + "," + v.mMaxY + "," + v.mDensity + "," + v.mSize + "," + v.mIndicatorRocks);
        writeCsv("worldgen_veins.csv", "name,top,bottom,between,spread,weight,distance,minY,maxY,density,size,indicatorRocks", vl);
        List<String> ll = new ArrayList<>();
        for (StoneLayer layer : StoneLayer.LAYERS) ll.add(matName(layer.mMaterial) + "," + matName(layer.mMaterialSurface) + "," + blockName(layer.mStone)
                + "," + blockName(layer.mCobble) + "," + blockName(layer.mMossy) + "," + layer.mMetaStone + "," + layer.mMetaCobble + "," + layer.mMetaMossy
                + "," + (layer.mOres==null?0:layer.mOres.size()));
        writeCsv("worldgen_layers.csv", "material,materialSurface,stone,cobble,mossy,metaStone,metaCobble,metaMossy,oreCount", ll);
        return vl.size();
    }

    // ------------------------------------------------------------------ oredict.csv (зеркало DumpOreDict)
    private static int dumpOreDict() throws IOException {
        List<String> lines = new ArrayList<>();
        List<String> names = new ArrayList<>(Arrays.asList(gregapi.oredict.OreDictionary.getOreNames()));
        Collections.sort(names);
        for (String name : names) {
            if (name == null) continue;
            List<String> stacks = new ArrayList<>();
            for (ItemStack s : gregapi.oredict.OreDictionary.getOres(name, false)) { if (s == null || s.getItem() == null) continue; stacks.add(stackId(s)); }
            Collections.sort(stacks);
            lines.add(name + "," + String.join("|", stacks));
        }
        writeCsv("oredict.csv", "oreName,stacks", lines);
        return lines.size();
    }

    // ------------------------------------------------------------------ unification.csv (зеркало DumpUnification)
    @SuppressWarnings("unchecked")
    private static int dumpUnification() throws IOException {
        List<String> lines = new ArrayList<>();
        try {
            Field f = gregapi.oredict.OreDictManager.class.getDeclaredField("sName2StackMap");
            f.setAccessible(true);
            Map<String, ItemStack> map = (Map<String, ItemStack>) f.get(gregapi.oredict.OreDictManager.INSTANCE);
            for (Map.Entry<String, ItemStack> e : map.entrySet()) { if (e.getKey() == null) continue; lines.add(e.getKey() + "," + stackId(e.getValue())); }
        } catch (Throwable t) {}
        writeCsv("unification.csv", "oreName,unifiedStack", lines);
        return lines.size();
    }

    // ------------------------------------------------------------------ localization.csv (зеркало DumpLocalization, TAB)
    @SuppressWarnings("unchecked")
    private static int dumpLocalization() throws IOException {
        List<String> lines = new ArrayList<>();
        try {
            Field f = gregapi.lang.LanguageHandler.class.getDeclaredField("BACKUPMAP");
            f.setAccessible(true);
            Map<String, String> m = (Map<String, String>) f.get(null);
            for (Map.Entry<String, String> e : m.entrySet()) {
                if (e.getKey() == null) continue;
                String v = e.getValue() == null ? "" : e.getValue().replace("\n", " ").replace("\r", " ");
                lines.add(e.getKey() + "\t" + v);
            }
        } catch (Throwable t) {}
        writeCsv("localization.csv", "key\tvalue", lines);
        return lines.size();
    }

    /** DumpUtil.stackId: реестр-имя предмета + ":" + subtype-meta. neo: BuiltInRegistries.ITEM.getKey. */
    private static String stackId(ItemStack s) {
        if (s == null || s.getItem() == null) return "";
        var k = BuiltInRegistries.ITEM.getKey(s.getItem());
        return (k == null ? "" : k.toString()) + ":" + gregapi.util.ST.meta_(s);
    }

    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String blockName(Block block) {
        if (block == null) return "";
        try { var k = BuiltInRegistries.BLOCK.getKey(block); return k == null ? "" : k.toString(); } catch (Throwable t) { return ""; }
    }

    // ------------------------------------------------------------------ parity-отчёт по одному файлу
    private static double report(String file) { return report(file, 1); }
    /** {@code ignoreCols} — 0-based индексы artifact-колонок (registry-id), игнорируемых для СЕМАНТИЧЕСКОГО паритета. */
    private static double report(String file, int keyCols, int... ignoreCols) {
        Path golden = ORACLE.resolve(file);
        Path port = DUMP.resolve(file);
        if (!Files.isRegularFile(golden)) {
            System.out.println("[parity] нет golden: " + golden.toAbsolutePath());
            return 0.0;
        }
        ParityDiff.ParitySet g = ignoreCols.length == 0 ? ParityDiff.fromCsv(golden, keyCols) : ParityDiff.fromCsvIgnoring(golden, keyCols, ignoreCols);
        ParityDiff.ParitySet p = ignoreCols.length == 0 ? ParityDiff.fromCsv(port, keyCols) : ParityDiff.fromCsvIgnoring(port, keyCols, ignoreCols);
        return reportSets(file, g, p);
    }
    /** Case-insensitive отчёт (neo lowercase ResourceLocation-имена). */
    private static double reportCI(String file, int keyCols) {
        Path golden = ORACLE.resolve(file), port = DUMP.resolve(file);
        if (!Files.isRegularFile(golden)) { System.out.println("[parity] нет golden: " + golden); return 0.0; }
        return reportSets(file, ParityDiff.fromCsvLower(golden, keyCols), ParityDiff.fromCsvLower(port, keyCols));
    }
    private static double reportSets(String file, ParityDiff.ParitySet g, ParityDiff.ParitySet p) {
        ParityDiff.Report r = ParityDiff.diff(file, g, p);
        System.out.printf("[parity] %-14s golden=%d совпало=%d нет=%d лишних=%d отлич=%d  %6.2f%%%n",
                file, r.goldenTotal, r.matched, r.missing.size(), r.extra.size(), r.differ.size(), r.percent());
        int shown = 0;
        for (String key : r.differ) {
            if (shown++ >= 3) break;
            System.out.println("   DIFFER  golden: " + g.records().get(key));
            System.out.println("           port  : " + p.records().get(key));
        }
        shown = 0;
        for (String key : r.missing) {
            if (shown++ >= 5) break;
            System.out.println("   MISSING в порте: " + key);
        }
        shown = 0;
        for (String key : r.extra) {
            if (shown++ >= 5) break;
            System.out.println("   EXTRA в порте: " + key);
        }
        return r.percent();
    }

    // ------------------------------------------------------------------ хелперы (зеркало DumpUtil)
    /** CSV: сортирует строки (детерминизм, как golden), пишет "# header", затем строки. */
    private static void writeCsv(String file, String header, List<String> lines) throws IOException {
        Collections.sort(lines);
        List<String> out = new ArrayList<>(lines.size() + 1);
        out.add("# " + header);
        out.addAll(lines);
        Files.write(DUMP.resolve(file), out, StandardCharsets.UTF_8);
    }

    /** Цвет RGBA как hex8 из short[]{r,g,b,a} (DumpUtil.rgba). */
    private static String rgba(short[] c) {
        if (c == null || c.length < 4) return "00000000";
        return String.format("%02x%02x%02x%02x", c[0] & 0xFF, c[1] & 0xFF, c[2] & 0xFF, c[3] & 0xFF);
    }

    /** Внутреннее имя материала или "" (DumpUtil.matName). */
    private static String matName(OreDictMaterial m) {
        return m == null ? "" : m.mNameInternal;
    }

    /** "materialName:amount" из OreDictMaterialStack или "null" (DumpUtil.matStack). */
    private static String matStack(OreDictMaterialStack s) {
        return (s == null || s.mMaterial == null) ? "null" : (s.mMaterial.mNameInternal + ":" + s.mAmount);
    }

    /**
     * "name:amount" жидкости или "" (F5-адаптация DumpUtil.fluidName под neo FluidStack:
     * getFluid()->Fluid, имя = registry-key path, getAmount() метод). Headless mLiquid обычно null → "".
     */
    private static String fluidName(FluidStack s) {
        if (s == null || s.getFluid() == null) return "";
        Fluid f = s.getFluid();
        Identifier id = BuiltInRegistries.FLUID.getKey(f);
        return (id == null ? "?" : id.getPath()) + ":" + s.getAmount();
    }

    /** Экранирование свободного текста: ',' -> ';', CR/LF -> ' ', null -> "" (DumpMaterials.csvField). */
    private static String csvField(String s) {
        if (s == null) return "";
        return s.replace(",", ";").replace("\r", " ").replace("\n", " ");
    }
}
