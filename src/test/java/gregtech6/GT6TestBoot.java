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

import net.minecraft.server.MinecraftServer;

/**
 * Догон фазы отложенной data-init для ТЕСТОВОГО контекста — единственное место, где это делается.
 *
 * <p><b>Зачем.</b> {@code EphemeralTestServerProvider} поднимает headless-сервер, но НЕ создаёт ни одного
 * уровня: его {@code initServer()} не зовёт {@code loadLevel()}, только {@code handleServerAboutToStart} и
 * {@code handleServerStarting} (сверено байткодом testframework 26.1.2.77). В игре вся отложенная item-init
 * GT6 осушается на {@code LevelEvent.Load} оверворлда ({@code GT_API.onLevelLoadEarlyItemInit}) — это
 * единственный drain очереди. Раз уровня нет, событие не летит, очередь не осушается, и мод в тесте стоит
 * недогруженным: карт рецептов 0, материалов валидных 0 из 60 (замер 2026-08-07). Тесты при этом краснели
 * так, будто сломан мод, хотя сломан был контекст.</p>
 *
 * <p><b>Приём.</b> Ничего своего не заводим: зовём ТЕ ЖЕ центры, что зовёт мод в игре, в том же порядке,
 * что в {@code GT_API.onLevelLoadEarlyItemInit:604-621}. Шаги, требующие живого уровня (подавление
 * датапак-рецептов, {@code finalizeRecipeLoading}, инъекция лута), здесь недостижимы по построению — их
 * область проверяется парити-дампом и живыми стендами, а не этим мостом.</p>
 *
 * <p><b>Поток исполнения.</b> В игре drain идёт в СЕРВЕРНОМ потоке. Если делать его из потока JUnit, пока
 * сервер тикает, data-init меняет те же коллекции, по которым идёт итерация в
 * {@code GT_API_Proxy.onServerTick} → {@code ConcurrentModificationException} → сервер ОСТАНАВЛИВАЕТСЯ, и
 * все последующие тесты получают {@code server == null}. Замер 2026-08-07: так падали
 * {@code ServerContextTest} и {@code BarsCollisionTest}, причём зелёным прогон оказывался или нет в
 * зависимости от ПОРЯДКА тестов. Поэтому работа выполняется тем же потоком, что и в игре.</p>
 *
 * <p><b>Идемпотентность и НЕСКОЛЬКО серверов.</b> Тест-классы в одной JVM поднимают каждый свой эфемерный
 * сервер, поэтому одного флага «уже загружено» мало: data-init мода глобальна и делается один раз, а шаги,
 * привязанные к {@code RecipeManager} конкретного сервера (роль-C, сканы), снимаются заново для каждого
 * нового сервера — иначе второй тест-класс работал бы с заменами, снятыми на чужом сервере.</p>
 */
public final class GT6TestBoot {

	/** Глобальная data-init мода — одна на JVM (очередь отложек осушается один раз). */
	private static boolean sDataInitDone = false;
	/** Сервер, для которого уже сняты серверные шаги; сравнение по идентичности. */
	private static MinecraftServer sServedServer = null;

	private GT6TestBoot() {}

	/**
	 * Привести мод в то состояние, в котором он находится в игре сразу после загрузки оверворлда.
	 * Вызывать в начале любого теста, которому нужен загруженный мод.
	 *
	 * @param aServer сервер эфемерного стенда (параметр JUnit-теста); {@code null} допустим —
	 *                тогда не снимаются шаги, которым нужен {@code RecipeManager}.
	 */
	public static void ensureLoaded(MinecraftServer aServer) {
		// Не synchronized: монитор берётся только внутри run(), уже в серверном потоке. Иначе ожидание
		// submit().get() под захваченным монитором заблокировало бы серверный поток на этом же мониторе.
		if (aServer != null && !aServer.isSameThread()) {
			try {
				aServer.submit(() -> {run(aServer); return null;}).get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("догон data-init прерван", e);
			} catch (java.util.concurrent.ExecutionException e) {
				throw new IllegalStateException("догон data-init упал в серверном потоке", e.getCause());
			}
			return;
		}
		run(aServer);
	}

	private static synchronized void run(MinecraftServer aServer) {
		if (!sDataInitDone) {
			sDataInitDone = true;
			gregapi.data.MT.init();                                 // материалы (идемпотентно)
			try {Class.forName("gregapi.data.OP");} catch (ClassNotFoundException e) {throw new IllegalStateException("нет gregapi.data.OP — сборка неполна", e);}
			gregapi.GT_API.runDeferredItemInit();                   // ЕДИНСТВЕННЫЙ drain очереди, как в игре
		}
		if (aServer == null || aServer == sServedServer) return;
		sServedServer = aServer;
		gregapi.oredict.OreDictionary.initVanillaRecipeReplacements(aServer);   // F4 роль-C
		gregapi.GT_API.sCurrentServerForRecipeScan = aServer;                   // F11 recipe-scan
		try {
			for (Runnable tScan : gregapi.GT_API.DEFERRED_RECIPE_SCAN) try {tScan.run();} catch (Throwable e) {e.printStackTrace();}
			gregapi.GT_API.DEFERRED_RECIPE_SCAN.clear();
		} finally {gregapi.GT_API.sCurrentServerForRecipeScan = null;}
	}
}
