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
        // Путь печатаем абсолютным: рабочий каталог тест-JVM — build/minecraft-junit/, поэтому относительный
        // build/dump материализуется как build/minecraft-junit/build/dump и «теряется» при поиске глазами.
        // Дамп переживает прогон — по нему разбор расхождений идёт оффлайн (analysis/tools/parity_report.py).
        System.out.println("[port-dump] дамп порта: " + DUMP.toAbsolutePath());
        System.out.println("[port-dump] golden:     " + ORACLE.toAbsolutePath());
        int nMat = dumpMaterials();
        int nPre = dumpPrefixes();
        int nFl = dumpFluids();
        dumpMTE(); dumpTools(); dumpTags(); dumpWorldgen();
        // dumpRecipes ПЕРЕД dumpRecipeMaps: triggerAllRecipesDeterministically популирует mRecipeList (растит mMaxFluidInput/OutputSize
        // по рецептам, Recipe.java:375/383) — иначе recipemaps.csv меряет maxFluid ДО триггера ленивых хендлеров (cutter/melter/squeezer
        // занижены vs golden, где регистрация была эагерной). Так recipemaps отражает то же популированное состояние, что recipes.jsonl.
        dumpOreDict(); dumpUnification(); dumpLocalization(); dumpItemData(); dumpEngine(); dumpRecipes(); dumpRecipeMaps();
        System.out.println("[port-dump] materials=" + nMat + " prefixes=" + nPre + " fluids=" + nFl);

        Map<String, Double> tFact = new java.util.LinkedHashMap<>();
        tFact.put("materials.csv", report("materials.csv"));
        tFact.put("prefixes.csv", report("prefixes.csv"));
        tFact.put("fluids.csv", report("fluids.csv", 1, 1, 5)); // игнор col1=fluidId (нео-артефакт 2× id) + col5=rgba (материал-флюид-цвет избыточен с
        // materials.csv mRGBaLiquid 99.95%; potion.* цвета — neo-vanilla зелья ≠ 1.7.10, F-potion-data инхерентно, порт корректен)
        tFact.put("mte.csv", report("mte.csv", 4));           // ключ = все 4 колонки (нет стабильного одиночного ключа)
        tFact.put("tools.csv", report("tools.csv", 1));
        tFact.put("tool_types.csv", report("tool_types.csv", 1));
        tFact.put("tags.csv", report("tags.csv", 1));
        tFact.put("tag_links.csv", report("tag_links.csv", 2));     // ключ = object+objectType
        tFact.put("worldgen_veins.csv", report("worldgen_veins.csv", 1));
        tFact.put("worldgen_layers.csv", report("worldgen_layers.csv", 1));
        tFact.put("oredict.csv", reportCI("oredict.csv", 1));      // CI: neo lowercase ResourceLocation-имена (camelCase gt.meta.* невозможен)
        tFact.put("unification.csv", reportCI("unification.csv", 1));
        tFact.put("localization.csv", report("localization.csv", 1));
        tFact.put("itemdata.csv", reportCI("itemdata.csv", 1, new int[]{6})); // игнор col6 unificationTarget: ленивый недетерминир. кэш (getStack_:630), несемантичен как fluidId
        tFact.put("engine_items.csv", reportEngine("engine_items.csv")); // GT6-регистрация (искл. vanilla minecraft: neo 1.21 ≠ 1.7.10 count+класс-имена, инхерентно)
        tFact.put("engine_blocks.csv", reportEngine("engine_blocks.csv"));
        tFact.put("recipemaps.csv", report("recipemaps.csv", 1, 20));
        tFact.put("recipes.jsonl", reportJsonl("recipes.jsonl")); // config-паритет; recipeCount(col20) trigger-недетерминирован в golden (60s-лимит)
        gate(tFact);
    }

    /**
     * РЕГРЕСС-ГЕЙТ по ВСЕМ наборам (обновлён 2026-07-26 живым замером, Ф4 шаг 2).
     *
     * <p>Прежний гейт стерёг только materials≥85 / prefixes≥46 — пороги от замера на УРЕЗАННОЙ сборке
     * ({@code -PcoreOnly}, контент-слой не компилировался). На полной сборке факт — 99.955 / 96.581, то есть
     * гейт проспал бы обвал в 14 и 50 процентных пунктов и остальные 16 наборов целиком. Пороги ниже —
     * фактический floor живого прогона за вычетом запаса на инхерентный шум.</p>
     *
     * <p><b>Запас (почему не «ровно факт»):</b> у рецептов golden сам по себе плавает на ±0.03 % (гонка
     * порядка OreDict-событий внутри 1.7.10, TOOLING §A3) — требовать точное число нельзя, иначе гейт
     * начнёт падать на шуме. Остальные наборы детерминированы, им дан минимальный запас 0.05 п.п.
     * Порог поднимается по мере закрытия расхождений — он обязан догонять факт, а не отставать от него.</p>
     */
    private static void gate(Map<String, Double> aFact) {
        Map<String, Double> tFloor = new java.util.LinkedHashMap<>();
        tFloor.put("materials.csv", 99.90);        // факт 99.955 (1 расхождение: имя жидкости «molten hsla» → «molten_hsla»)
        tFloor.put("prefixes.csv", 96.95);         // факт 97.009 (было 96.581 до закрытия block-flatten + ванильных записей Forge)
        tFloor.put("fluids.csv", 100.0);
        tFloor.put("mte.csv", 100.0);
        tFloor.put("tools.csv", 100.0);
        tFloor.put("tool_types.csv", 100.0);
        tFloor.put("tags.csv", 100.0);
        tFloor.put("tag_links.csv", 100.0);
        tFloor.put("worldgen_veins.csv", 100.0);
        tFloor.put("worldgen_layers.csv", 100.0);
        tFloor.put("engine_items.csv", 100.0);
        tFloor.put("engine_blocks.csv", 100.0);
        tFloor.put("oredict.csv", 99.95);          // факт 99.957 (0 пропавших записей — ванильный набор Forge восстановлен)
        tFloor.put("unification.csv", 99.95);      // факт 99.974
        tFloor.put("localization.csv", 99.95);     // факт 99.996
        tFloor.put("itemdata.csv", 99.90);         // факт 99.939
        tFloor.put("recipemaps.csv", 98.90);       // факт 98.947
        tFloor.put("recipes.jsonl", 99.00);        // факт 99.060; floor шума golden ±0.03 п.п.
        List<String> tFailed = new ArrayList<>();
        for (Map.Entry<String, Double> e : tFloor.entrySet()) {
            Double tGot = aFact.get(e.getKey());
            if (tGot == null) {tFailed.add(e.getKey() + ": НЕ ИЗМЕРЕН (набор пропал из прогона)"); continue;}
            if (tGot < e.getValue()) tFailed.add(String.format("%s: %.3f%% < %.2f%%", e.getKey(), tGot, e.getValue()));
        }
        if (!tFailed.isEmpty()) throw new AssertionError("РЕГРЕСС ПАРИТЕТА (" + tFailed.size() + " наборов): " + String.join(" | ", tFailed));
        System.out.println("[parity] РЕГРЕСС-ГЕЙТ ОК: все " + tFloor.size() + " наборов не ниже своего floor.");
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

    // ------------------------------------------------------------------ itemdata.csv (зеркало DumpUnification.itemData)
    @SuppressWarnings("unchecked")
    private static int dumpItemData() throws IOException {
        List<String> lines = new ArrayList<>();
        try {
            Field f = gregapi.oredict.OreDictManager.class.getDeclaredField("sItemStack2DataMap");
            f.setAccessible(true);
            Map<?, gregapi.oredict.OreDictItemData> map = (Map<?, gregapi.oredict.OreDictItemData>) f.get(gregapi.oredict.OreDictManager.INSTANCE);
            for (gregapi.oredict.OreDictItemData data : map.values()) {
                if (data == null || data.mOreDictName == null) continue;
                String prefix = data.mPrefix == null ? "" : data.mPrefix.mNameInternal;
                List<String> bp = new ArrayList<>();
                if (data.mByProducts != null) for (gregapi.oredict.OreDictMaterialStack s : data.mByProducts) bp.add(matStack(s));
                Collections.sort(bp);
                lines.add(data.mOreDictName + "," + prefix + "," + matStack(data.mMaterial) + "," + String.join("|", bp)
                        + "," + data.mBlackListed + "," + data.mBlocked + "," + stackId(data.mUnificationTarget));
            }
        } catch (Throwable t) {}
        writeCsv("itemdata.csv", "key,prefix,material,byproducts,blacklisted,blocked,unificationTarget", lines);
        return lines.size();
    }
    /** DumpUtil.stackId: реестр-имя предмета + ":" + subtype-meta. neo: BuiltInRegistries.ITEM.getKey. */
    private static String stackId(ItemStack s) {
        if (s == null || s.getItem() == null) return "null"; // golden DumpUtil.stackId(null)="null" (itemdata.unificationTarget)
        var k = BuiltInRegistries.ITEM.getKey(s.getItem());
        return (k == null ? "" : k.toString()) + ":" + gregapi.util.ST.meta_(s);
    }

    // ------------------------------------------------------------------ recipes.jsonl (зеркало DumpRecipes.recipeJson)
    private static int dumpRecipes() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, gregapi.recipes.Recipe.RecipeMap> e : gregapi.recipes.Recipe.RecipeMap.RECIPE_MAPS.entrySet()) {
            gregapi.recipes.Recipe.RecipeMap m = e.getValue();
            if (m == null || m.mRecipeList == null) continue;
            triggerAllRecipesDeterministically(m);
            // Имя карты = mNameInternal (как golden DumpRecipes:35), НЕ ключ RECIPE_MAPS: алиас-карты (Debarker=PressureWasher
            // под 2 ключами, RM.java:182) иначе дампятся под ключом-псевдонимом → фантомная карта. mNameInternal канонический.
            for (gregapi.recipes.Recipe r : m.mRecipeList) { if (r == null) continue; try { lines.add(recipeJson(m.mNameInternal, r)); } catch (Throwable t) {} }
        }
        Collections.sort(lines);
        Files.write(DUMP.resolve("recipes.jsonl"), lines, StandardCharsets.UTF_8);
        return lines.size();
    }
    private static void triggerAllRecipesDeterministically(gregapi.recipes.Recipe.RecipeMap map) {
        // Робастный триггер (harness, не мод-код): прямой обход СНАПШОТА mRecipeMapHandlers с addAllRecipes до
        // нулевого прироста. Обходит два дефекта getNEIAllRecipes как ПОЛНО-триггера: (1) remove-во-время-индексной-
        // итерации (Recipe.java:557-560 пропускает сосед сдвинутого) + (2) 60s wall-clock таймаут (:561), из-за которых
        // stability-цикл ложно стабилизировался рано. Цель harness — полностью проявить генерацию мода для замера паритета.
        int prev = -1, guard = 0;
        while (map.mRecipeList.size() != prev && guard < 10000) {
            prev = map.mRecipeList.size();
            guard++;
            for (gregapi.recipes.IRecipeMapHandler h : new java.util.ArrayList<>(map.mRecipeMapHandlers)) {
                try { h.addAllRecipes(map); } catch (Throwable ignore) {}
            }
        }
    }
    private static String recipeJson(String map, gregapi.recipes.Recipe r) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"map\":\"").append(esc(map)).append('"');
        sb.append(",\"in\":[").append(recItems(r.mInputs)).append(']');
        sb.append(",\"fin\":[").append(recFluids(r.mFluidInputs)).append(']');
        sb.append(",\"out\":[").append(recItems(r.mOutputs)).append(']');
        sb.append(",\"fout\":[").append(recFluids(r.mFluidOutputs)).append(']');
        sb.append(",\"chn\":[").append(longs(r.mChances)).append(']');
        sb.append(",\"chnmax\":[").append(longs(r.mMaxChances)).append(']');
        sb.append(",\"eut\":").append(r.mEUt);
        sb.append(",\"dur\":").append(r.mDuration);
        sb.append(",\"spv\":").append(r.mSpecialValue);
        sb.append(",\"en\":").append(r.mEnabled);
        sb.append(",\"hid\":").append(r.mHidden);
        sb.append(",\"fake\":").append(r.mFakeRecipe);
        sb.append(",\"nempty\":").append(r.mNeedsEmptyOutput);
        sb.append(",\"buf\":").append(r.mCanBeBuffered);
        sb.append(",\"nonbt\":").append(r.mNoNBTChecks);
        sb.append(",\"spec\":\"").append(r.mSpecialItems == null ? "" : esc(r.mSpecialItems.getClass().getSimpleName())).append('"');
        sb.append('}');
        return sb.toString();
    }
    private static String recItems(ItemStack[] arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : arr) { if (sb.length() > 0) sb.append(','); sb.append('"').append(s == null ? "null" : esc(stackId(s) + ":" + gregapi.util.ST.size(s))).append('"'); } // F-size0-catalyst: ST.size даёт 0 для size-0-катализатора (golden 1.7.10 stackSize=0)
        return sb.toString();
    }
    private static String recFluids(FluidStack[] arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (FluidStack s : arr) { if (sb.length() > 0) sb.append(','); String v = (s == null || s.getFluid() == null) ? "null" : (FluidGT.nameOf(s.getFluid()) + ":" + s.getAmount()); sb.append('"').append(esc(v)).append('"'); }
        return sb.toString();
    }
    private static String longs(long[] arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (long v : arr) { if (sb.length() > 0) sb.append(','); sb.append(v); }
        return sb.toString();
    }

    // ------------------------------------------------------------------ recipemaps.csv (зеркало DumpRecipes)
    private static int dumpRecipeMaps() throws IOException {
        List<String> lines = new ArrayList<>();
        for (gregapi.recipes.Recipe.RecipeMap m : gregapi.recipes.Recipe.RecipeMap.RECIPE_MAPS.values()) {
            if (m == null) continue;
            lines.add(m.mNameInternal + "," + m.mInputItemsCount + "," + m.mOutputItemsCount + "," + m.mInputFluidCount + ","
                    + m.mOutputFluidCount + "," + m.mMinimalInputItems + "," + m.mMinimalInputFluids + "," + m.mMinimalInputs + ","
                    + m.mPower + "," + m.mNEISpecialValueMultiplier + "," + m.mProgressBarDirection + "," + m.mProgressBarAmount + ","
                    + m.mNEIAllowed + "," + m.mShowVoltageAmperageInNEI + "," + m.mNeedsOutputs + "," + m.mCombinePower + ","
                    + m.mUseBucketSizeIn + "," + m.mUseBucketSizeOut + "," + m.mMaxFluidInputSize + "," + m.mMaxFluidOutputSize + "," + m.mRecipeList.size());
        }
        writeCsv("recipemaps.csv", "mNameInternal,inItems,outItems,inFluid,outFluid,minInItems,minInFluid,minIn,power,neiSpecialMul,progDir,progAmount,neiAllowed,showVA,needsOutputs,combinePower,bucketIn,bucketOut,maxFluidIn,maxFluidOut,recipeCount", lines);
        return lines.size();
    }

    // ------------------------------------------------------------------ engine_items.csv + engine_blocks.csv (зеркало DumpEngine)
    private static int dumpEngine() throws IOException {
        List<String> il = new ArrayList<>();
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            var k = BuiltInRegistries.ITEM.getKey(item);
            String rn = k == null ? "" : k.toString();
            il.add(rn + "," + BuiltInRegistries.ITEM.getId(item) + "," + (k == null ? "" : k.getNamespace()) + "," + item.getClass().getName());
        }
        writeCsv("engine_items.csv", "registryName,id,modid,className", il);
        List<String> bl = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            var k = BuiltInRegistries.BLOCK.getKey(block);
            String rn = k == null ? "" : k.toString();
            bl.add(rn + "," + BuiltInRegistries.BLOCK.getId(block) + "," + (k == null ? "" : k.getNamespace()) + "," + block.getClass().getName());
        }
        writeCsv("engine_blocks.csv", "registryName,id,modid,className", bl);
        return il.size();
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
    // engine_*.csv: registryName,id,modid,className. Ключ=col0, игнор id(col1) — registry-position артефакт; ИСКЛ vanilla
    // (col2 modid==minecraft): neo 1.21 vanilla ≠ 1.7.10 (count+класс-имена) — не GT6-логика. Даёт паритет GT6-регистрации.
    private static double reportEngine(String file) {
        Path golden = ORACLE.resolve(file), port = DUMP.resolve(file);
        if (!Files.isRegularFile(golden)) { System.out.println("[parity] нет golden: " + golden); return 0.0; }
        return reportSets(file, ParityDiff.fromCsvLowerExcl(golden, 1, 2, "minecraft", 1), ParityDiff.fromCsvLowerExcl(port, 1, 2, "minecraft", 1));
    }
    private static double reportCI(String file, int keyCols) { return reportCI(file, keyCols, new int[0]); }
    private static double reportCI(String file, int keyCols, int[] ignoreCols) {
        Path golden = ORACLE.resolve(file), port = DUMP.resolve(file);
        if (!Files.isRegularFile(golden)) { System.out.println("[parity] нет golden: " + golden); return 0.0; }
        return reportSets(file, ParityDiff.fromCsvLowerIgnoring(golden, keyCols, ignoreCols), ParityDiff.fromCsvLowerIgnoring(port, keyCols, ignoreCols));
    }
    /** Whole-line CI set-overlap отчёт (для .jsonl рецептов). */
    private static double reportJsonl(String file) {
        Path golden = ORACLE.resolve(file), port = DUMP.resolve(file);
        if (!Files.isRegularFile(golden)) { System.out.println("[parity] нет golden: " + golden); return 0.0; }
        return reportSets(file, ParityDiff.fromLinesFlattenCI(golden), ParityDiff.fromLinesFlattenCI(port));
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
