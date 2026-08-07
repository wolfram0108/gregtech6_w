package gregtech6;

import gregapi.data.CS;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless-проверка МЕХАНИК GT6 (не только дампы-паритет): реальная работа систем в загруженном моде.
 * EphemeralTestServer стартует headless-сервер (мод загружен, server-start, компоненты привязаны).
 */
@ExtendWith(EphemeralTestServerProvider.class)
class MechanicsTest {

	/**
	 * Механика recipe-matching (машины/крафты): берём РЕАЛЬНЫЕ GT6-рецепты из каждой RecipeMap и прогоняем их же
	 * входами через {@code findRecipe} — движок-логика машины должна найти тот же рецепт (тот же выход). Доказывает,
	 * что recipe-система (индексация по входам, unification, fluid+item матчинг) реально работает в рантайме.
	 */
	@Test
	void recipeMatchingMechanic(MinecraftServer server) {
		GT6TestBoot.ensureLoaded(server);   // догон фазы data-init: в эфемерном сервере нет уровня → LevelEvent.Load не летит
		int maps = 0, exact = 0, other = 0, miss = 0, err = 0;
		StringBuilder tProblems = new StringBuilder();
		for (java.util.Map.Entry<String, RecipeMap> e : RecipeMap.RECIPE_MAPS.entrySet()) {
			RecipeMap m = e.getValue();
			if (m == null || m.mRecipeList == null || m.mRecipeList.isEmpty()) continue;
			maps++;
			int n = 0;
			for (Recipe r : m.mRecipeList) {
				if (r == null || r.mInputs == null || r.mInputs.length == 0) continue;
				boolean tHasReal = false;
				for (net.minecraft.world.item.ItemStack s : r.mInputs) if (gregapi.util.ST.valid(s)) {tHasReal = true; break;}
				if (!tHasReal) continue;
				if (n++ >= 3) break;
				try {
					FluidStack[] tFluids = r.mFluidInputs != null ? r.mFluidInputs : CS.ZL_FLUIDSTACK;
					Recipe tFound = m.findRecipe(null, null, false, Long.MAX_VALUE, null, tFluids, r.mInputs);
					if (tFound == r) exact++;
					else if (tFound != null) other++;
					else { miss++; if (tProblems.length() < 1200) tProblems.append(m.mNameInternal).append(' '); }
				} catch (Throwable t) {
					err++; if (tProblems.length() < 1200) tProblems.append(m.mNameInternal).append("!ERR ");
				}
			}
		}
		System.out.println("[MECH-RECIPE] карт=" + maps + " точно=" + exact + " иначе=" + other + " не_нашёл=" + miss + " ошибок=" + err + " | проблемные: " + tProblems);
		assertTrue(exact + other > 0, "findRecipe не нашёл НИ ОДНОГО рецепта своими же входами — recipe-matching сломан");
		assertTrue(miss + err <= (exact + other), "recipe-matching: провалов больше половины (не_нашёл=" + miss + " ошибок=" + err + " vs найдено=" + (exact + other) + ")");
	}

	/**
	 * Механика ПРОЦЕДУРНОЙ ГЕНЕРАЦИИ предметов (суть GT6): {@code prefix.mat(material)} порождает предмет материал×префикс;
	 * {@code OM.data} читает материал/префикс обратно. Round-trip доказывает, что генератор предметов и опознавание работают.
	 */
	@Test
	void materialItemGenerationMechanic(MinecraftServer server) {
		GT6TestBoot.ensureLoaded(server);   // догон фазы data-init: в эфемерном сервере нет уровня → LevelEvent.Load не летит
		gregapi.oredict.OreDictMaterial[] tMats = {gregapi.data.MT.Fe, gregapi.data.MT.Au, gregapi.data.MT.Cu, gregapi.data.MT.Sn, gregapi.data.MT.Pb, gregapi.data.MT.Ag, gregapi.data.MT.Al, gregapi.data.MT.Ni, gregapi.data.MT.Zn, gregapi.data.MT.Ti};
		gregapi.oredict.OreDictPrefix[] tPrefixes = {gregapi.data.OP.dust, gregapi.data.OP.ingot, gregapi.data.OP.plate, gregapi.data.OP.stick, gregapi.data.OP.nugget, gregapi.data.OP.gear};
		int tValid = 0, tRoundtrip = 0, tEmpty = 0;
		for (gregapi.oredict.OreDictMaterial tMat : tMats) for (gregapi.oredict.OreDictPrefix tP : tPrefixes) {
			net.minecraft.world.item.ItemStack tStack = tP.mat(tMat, 1);
			if (gregapi.util.ST.valid(tStack)) {
				tValid++;
				gregapi.oredict.OreDictItemData tData = gregapi.util.OM.data(tStack, false);
				if (tData != null && tData.mPrefix == tP && tData.mMaterial != null && tData.mMaterial.mMaterial == tMat) tRoundtrip++;
			} else tEmpty++;
		}
		System.out.println("[MECH-MATGEN] валидных=" + tValid + " round-trip(item->material)=" + tRoundtrip + " пусто=" + tEmpty + " из " + (tMats.length * tPrefixes.length));
		assertTrue(tValid >= tMats.length, "процедурная генерация предметов (материал×префикс) не работает: valid=" + tValid);
		assertTrue(tRoundtrip >= tValid / 2, "round-trip item->material сломан: " + tRoundtrip + "/" + tValid);
	}

	/**
	 * Механика жидкостей материалов (F5): {@code material.fluid(amount)} материализует FluidStack расплава/жидкости.
	 * Доказывает, что fluid-подсистема (ленивая материализация FL→FluidStack) реально отдаёт валидные жидкости.
	 */
	@Test
	void materialFluidMechanic(MinecraftServer server) {
		GT6TestBoot.ensureLoaded(server);   // догон фазы data-init: в эфемерном сервере нет уровня → LevelEvent.Load не летит
		gregapi.oredict.OreDictMaterial[] tMats = {gregapi.data.MT.Fe, gregapi.data.MT.Au, gregapi.data.MT.Cu, gregapi.data.MT.H2O, gregapi.data.MT.Sn, gregapi.data.MT.Pb};
		int tOk = 0;
		StringBuilder tGot = new StringBuilder();
		for (gregapi.oredict.OreDictMaterial tMat : tMats) {
			try {
				FluidStack tF = tMat.fluid(CS.U, false);
				if (tF != null && !tF.isEmpty() && tF.getAmount() > 0) {tOk++; tGot.append(tMat.mNameInternal).append('=').append(tF.getAmount()).append("mB ");}
			} catch (Throwable t) {/**/}
		}
		System.out.println("[MECH-FLUID] материалов с валидной жидкостью=" + tOk + "/" + tMats.length + " | " + tGot);
		assertTrue(tOk > 0, "механика жидкостей материалов сломана (mat.fluid → пусто для всех)");
	}

	/**
	 * Механика ЭНЕРГИИ (EU): батарея-MTE принимает энергию (doEnergyInjection) до capacity и хранит (mEnergy/getEnergyStored),
	 * отдаёт (doEnergyExtraction). Доказывает, что энерго-подсистема GT6 (приём/хранение/выдача по типу+размеру) работает.
	 */
	@Test
	void energyMechanic(MinecraftServer server) {
		GT6TestBoot.ensureLoaded(server);   // догон фазы data-init: в эфемерном сервере нет уровня → LevelEvent.Load не летит
		gregtech.tileentity.batteries.eu.MultiTileEntityBatteryEU8 tBat = new gregtech.tileentity.batteries.eu.MultiTileEntityBatteryEU8();
		tBat.mType = gregapi.data.TD.Energy.EU;
		tBat.mCapacity = 100000; tBat.mEnergy = 0;
		tBat.mSizeMin = 1; tBat.mSizeMax = 10000; tBat.mSizeRec = 32;
		net.minecraft.world.item.ItemStack tStack = gregapi.util.ST.make(net.minecraft.world.level.block.Blocks.STONE, 1, 0);
		long tInjected = tBat.doEnergyInjection(gregapi.data.TD.Energy.EU, tStack, 32, 100, null, null, 0, 0, 0, true);
		long tStored = tBat.getEnergyStored(gregapi.data.TD.Energy.EU, tStack);
		System.out.println("[MECH-ENERGY] инъекция вернула=" + tInjected + " mEnergy=" + tBat.mEnergy + " getEnergyStored=" + tStored + " capacity=" + tBat.mCapacity);
		assertTrue(tInjected > 0, "energy: doEnergyInjection не принял энергию");
		assertTrue(tBat.mEnergy > 0 && tStored == tBat.mEnergy, "energy: энергия не сохранилась (mEnergy=" + tBat.mEnergy + " getStored=" + tStored + ")");
		assertTrue(tBat.mEnergy == tInjected * 32L, "energy: сохранённое (" + tBat.mEnergy + ") != инъекция*size (" + (tInjected * 32) + ")");
		// неверный тип энергии не принимается
		long tWrong = tBat.doEnergyInjection(gregapi.data.TD.Energy.RU, tStack, 32, 100, null, null, 0, 0, 0, true);
		assertTrue(tWrong == 0, "energy: батарея приняла ЧУЖОЙ тип энергии (RU в EU-батарею)");
	}

	// Механика РАЗМЕЩЕНИЯ блоков судится живыми стендами (реальный путь игрока: useItemOn/destroyBlock,
	// 26 точек в src/probes), а не здесь. Прежний headless-тест ставил блок через setBlock и всё равно
	// НИКОГДА не выполнялся: EphemeralTestServer не грузит уровни вовсе — его initServer() не зовёт
	// loadLevel(), только AboutToStart/Starting (сверено байткодом testframework 26.1.2.77). Тест вечно
	// висел в состоянии "пропущен", то есть занимал место сторожа, ничего не сторожа.

	/**
	 * Механика ОБРАБОТКИ РЕЦЕПТА МАШИНОЙ ЗА ТИКИ (ядро GT6): берём РЕАЛЬНУЮ MTE-машину (MultiTileEntityBasicMachineElectric),
	 * ставим ей РЕАЛЬНУЮ RecipeMap мода и РЕАЛЬНЫЙ рецепт из неё, кладём входы в слоты, даём энергию — и гоняем НАСТОЯЩИЙ
	 * тик-цикл машины ({@code checkRecipe} → {@code doActive}) пока выход не появится в выходном слоте. Тик-логика GT6
	 * не требует загруженного мира (level==null защищён, updateInventory no-op, checkStructure→true), поэтому проверяется
	 * headless. Доказывает: машина сама находит рецепт по входам, СПИСЫВАЕТ входы, двигает прогресс на энергии и КЛАДЁТ
	 * реальный выход рецепта в выходной слот — весь производственный цикл, не отдельные куски.
	 */
	@Test
	void machineProcessingMechanic(MinecraftServer server) {
		GT6TestBoot.ensureLoaded(server);   // догон фазы data-init: в эфемерном сервере нет уровня → LevelEvent.Load не летит
		int tTested = 0, tProcessed = 0, tSetupFail = 0;
		StringBuilder tOk = new StringBuilder();
		for (java.util.Map.Entry<String, RecipeMap> e : RecipeMap.RECIPE_MAPS.entrySet()) {
			if (tTested >= 12) break;
			RecipeMap m = e.getValue();
			if (m == null || m.mRecipeList == null || m.mRecipeList.isEmpty()) continue;
			if (m.mInputItemsCount <= 0 || m.mOutputItemsCount <= 0) continue;
			// Ищем чистый item->item рецепт (без жидкостей), потребляющий энергию, входы/выходы влезают в слоты.
			Recipe tPick = null;
			for (Recipe r : m.mRecipeList) {
				if (r == null || r.mInputs == null || r.mOutputs == null) continue;
				if (r.mFluidInputs != null && r.mFluidInputs.length > 0) continue;
				if (r.mFluidOutputs != null && r.mFluidOutputs.length > 0) continue;
				if (r.mEUt <= 0) continue;
				int tNin = 0; for (net.minecraft.world.item.ItemStack s : r.mInputs)  if (gregapi.util.ST.valid(s)) tNin++;
				int tNout = 0; for (net.minecraft.world.item.ItemStack s : r.mOutputs) if (gregapi.util.ST.valid(s)) tNout++;
				if (tNin == 0 || tNin > m.mInputItemsCount) continue;
				if (tNout == 0 || tNout > m.mOutputItemsCount) continue;
				tPick = r; break;
			}
			if (tPick == null) continue;
			tTested++;
			try {
				gregapi.tileentity.machines.MultiTileEntityBasicMachineElectric tMach = new gregapi.tileentity.machines.MultiTileEntityBasicMachineElectric();
				tMach.mRecipes = m;                             // РЕАЛЬНАЯ карта мода
				tMach.readFromNBT2(gregapi.util.UT.NBT.make()); // канонический init: аллокация слотов+танков по mRecipes
				tMach.mEnergyTypeAccepted = gregapi.data.TD.Energy.EU;
				tMach.mEfficiency = 10000;                      // 100% — реалистичная длительность
				tMach.mDisabledItemInput = true; tMach.mDisabledItemOutput = true;
				tMach.mDisabledFluidInput = true; tMach.mDisabledFluidOutput = true;
				long tEUt = Math.max(1, tPick.mEUt);
				tMach.mInputMin = 1; tMach.mInput = tMach.mInputMax = tEUt * 64L;
				int tSi = 0; for (net.minecraft.world.item.ItemStack s : tPick.mInputs) if (gregapi.util.ST.valid(s)) tMach.slot(tSi++, s.copy());
				tMach.mEnergy = tMach.mInputMax;
				int tRc = tMach.checkRecipe(true, false);       // РЕАЛЬНЫЙ поиск+запуск рецепта машиной
				if (tRc != gregapi.tileentity.machines.MultiTileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE) { tSetupFail++; continue; }
				boolean tGotOut = false;
				for (int t = 0; t < 100000 && !tGotOut; t++) {
					tMach.mEnergy = tMach.mInputMax;            // держим питание
					tMach.doActive(t, tMach.mInputMax);         // РЕАЛЬНЫЙ тик обработки
					for (int oi = m.mInputItemsCount; oi < m.mInputItemsCount + m.mOutputItemsCount; oi++)
						if (gregapi.util.ST.valid(tMach.slot(oi))) { tGotOut = true; break; }
				}
				if (tGotOut) { tProcessed++; tOk.append(m.mNameInternal).append(' '); } else tSetupFail++;
			} catch (Throwable t) { tSetupFail++; }
		}
		System.out.println("[MECH-MACHINE] карт_проверено=" + tTested + " обработали_рецепт_до_выхода=" + tProcessed + " setup_fail=" + tSetupFail + " | " + tOk);
		assertTrue(tProcessed > 0, "НИ ОДНА машина не прогнала рецепт через полный тик-цикл до выхода в слот (обработка сломана)");
		assertTrue(tProcessed >= tTested / 2, "машина-обработка: провалов больше половины (обработали=" + tProcessed + " из " + tTested + ")");
	}

	// (Убран headless iconResourceProbe: getIconIndex требует client-инициализированные TextureSet → в EphemeralTestServer
	//  NPE-ит для 139/140 предметов. Скан рендера item-иконок достижим ТОЛЬКО в client-контексте — probe-оснастка встроена в
	//  GT6ItemModel.probeItemIcons на ModelEvent.BakingCompleted (сработает в интерактивном runClient пользователя).)
}
