package gregtech6.parity;

import gregapi.data.MT;
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
import java.util.Collections;
import java.util.List;

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
        System.out.println("[port-dump] bootstrap ванильных реестров…");
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
        MT.init();                          // static-init MT (материалы) + init()
        Class.forName("gregapi.data.OP");   // static-init OP (префиксы + Items.GLASS_BOTTLE)

        Files.createDirectories(DUMP);
        int nMat = dumpMaterials();
        int nPre = dumpPrefixes();
        System.out.println("[port-dump] materials=" + nMat + " prefixes=" + nPre);

        report("materials.csv");
        report("prefixes.csv");
    }

    // ------------------------------------------------------------------ materials.csv (зеркало DumpMaterials)
    private static int dumpMaterials() throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreDictMaterial m : OreDictMaterial.MATERIAL_MAP.values()) {
            if (m == null) continue;
            m.materializeFluids(); // F5-lazy: mLiquid/mGas/mPlasma созданы отложенно (в MT.<clinit> Holder.components не привязаны) — материализуем перед чтением поля
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

    // ------------------------------------------------------------------ parity-отчёт по одному файлу
    private static void report(String file) {
        Path golden = ORACLE.resolve(file);
        Path port = DUMP.resolve(file);
        if (!Files.isRegularFile(golden)) {
            System.out.println("[parity] нет golden: " + golden.toAbsolutePath());
            return;
        }
        ParityDiff.ParitySet g = ParityDiff.fromCsv(golden, 1);
        ParityDiff.ParitySet p = ParityDiff.fromCsv(port, 1);
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
