package gregapi;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;

/**
 * КЛИЕНТСКАЯ половина верификационной оснастки Ф3.1: клиент-часть аудит-пробы BUG-057 и автономный
 * вход в мир (harness живых стендов, гейт {@code run/wgautoworld.flag}).
 *
 * <p>Как и {@link GT6Probes}, живёт в {@code src/probes/java} и компилируется ТОЛЬКО при
 * {@code -Pgt6probes} — в jar игрока не попадает. Обработчики статические и подписаны через
 * {@code @EventBusSubscriber}, поэтому {@code GT_API_Proxy_Client} о них больше не знает.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = "gregapi", value = Dist.CLIENT)
public final class GT6ProbesClient {
	private GT6ProbesClient() {}

	// [GT6-MTEAUDIT] BUG-057, клиентская половина (§2.4): 1=скан клиентских BE той же зоны, 2=relog (двухмировой приём
	// BUG-002: disconnectFromWorld + перевзвод автовхода wgautoworld). Снять при уборке фазы.
	@net.neoforged.bus.api.SubscribeEvent
	public static void onMTEAuditClient(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (!gregapi.data.CS.probeFlag("gt6mteauditprobe.flag")) return;
		int tCmd = gregapi.GT6Probes.sMTEAuditClientCmd;
		if (tCmd == 0) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		try {
			if (tCmd == 1) {
				if (tMC.level == null || tMC.player == null) return; // повтор на следующем тике
				gregapi.GT6Probes.sMTEAuditClientCmd = 0;
				gregapi.GT6Probes.gt6MTEAuditScan(gregapi.GT6Probes.sMTEAuditScanLabel + "-CLIENT", tMC.level, tMC.player.blockPosition());
			} else if (tCmd == 2) {
				gregapi.GT6Probes.sMTEAuditClientCmd = 0;
				mAutoWorldTriggered = false; // перевзвод автовхода: на TitleScreen wgautoworld снова войдёт в мир из wgautoworld.world
				gregapi.data.CS.OUT.println("[GT6-MTEAUDIT] клиент: relog (disconnectFromWorld -> автоперевход)");
				tMC.disconnectFromWorld(net.minecraft.network.chat.Component.literal("[GT6-MTEAUDIT] relog"));
			}
		} catch (Throwable e) {gregapi.data.CS.OUT.println("[GT6-MTEAUDIT] клиент EXC " + e); e.printStackTrace(gregapi.data.CS.ERR);}
	}

	// АВТОНОМНЫЙ вход в мир (переиспользуемый harness живых проб, гейт: файл run/wgautoworld.flag; вне флага НЕ активен):
	// quickPlay упирается в диалог-подтверждение (некому кликнуть) → до генерации не доходит. Здесь на TitleScreen САМИ
	// создаём свежий CREATIVE-мир через штатный клиентский API createFreshLevel (тот же путь, что кнопка «Создать мир» →
	// «Создать», минует ВСЕ диалоги), либо входим в существующий мир по имени из wgautoworld.world. Ноль ручных действий.
	private static boolean mAutoWorldTriggered = false; // [GT6-MTEAUDIT] static: перевзвод relog-пробой BUG-057 — вернуть instance при уборке фазы
	@net.neoforged.bus.api.SubscribeEvent
	public static void onAutoWorldCreate(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mAutoWorldTriggered) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (!(tMC.screen instanceof net.minecraft.client.gui.screens.TitleScreen)) return;
		if (!gregapi.data.CS.probeFlag("wgautoworld.flag")) return;
		mAutoWorldTriggered = true;
		try {
			// Вход в СУЩЕСТВУЮЩИЙ мир (аудит мира игрока): имя папки сейва — в файле wgautoworld.world.
			java.io.File tTargetFile = new java.io.File("wgautoworld.world");
			if (tTargetFile.exists()) {
				String tName = new String(java.nio.file.Files.readAllBytes(tTargetFile.toPath()), java.nio.charset.StandardCharsets.UTF_8).trim();
				gregapi.data.CS.OUT.println("[GT6-AUTOWORLD] вход в существующий мир '" + tName + "' (WorldOpenFlows.openWorld:303)...");
				tMC.createWorldOpenFlows().openWorld(tName, () -> gregapi.data.CS.OUT.println("[GT6-AUTOWORLD] вход в '" + tName + "' ОТМЕНЁН/провален"));
				return;
			}
			java.io.File tOld = new java.io.File("saves/GT6WGTest");
			if (tOld.exists()) deleteRecursive(tOld);
			gregapi.data.CS.OUT.println("[GT6-AUTOWORLD] создаю свежий CREATIVE-мир GT6WGTest (программно, минуя диалоги)...");
			net.minecraft.world.level.LevelSettings tSettings = new net.minecraft.world.level.LevelSettings(
				"GT6 WG Test", net.minecraft.world.level.GameType.CREATIVE,
				net.minecraft.world.level.LevelSettings.DifficultySettings.DEFAULT, true,
				net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
			tMC.createWorldOpenFlows().createFreshLevel("GT6WGTest", tSettings,
				new net.minecraft.world.level.levelgen.WorldOptions(4242L, true, false), // ФИКС-сид: детерминированная генерация для чистого измерения reattach (было defaultWithRandomSeed)
				net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions,
				tMC.screen);
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-AUTOWORLD] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
	}
	private static void deleteRecursive(java.io.File aFile) {
		java.io.File[] tKids = aFile.listFiles();
		if (tKids != null) for (java.io.File tK : tKids) deleteRecursive(tK);
		aFile.delete();
	}
}
