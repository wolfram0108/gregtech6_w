package gregtech6;
// [GT6-DUNGEONPROBE] ВРЕМЕННАЯ ПРОБА разведки захода «данжи #39» — убрать после приёмки захода.
// Судья проверки 1 (architecture/dungeons.md §6): все MTE-ID, которые ставят данж-комнаты, обязаны быть
// зарегистрированы в рантайм-реестре gt.multitileentity (серийные регистрации статическим грепом не видны),
// и класс сейфа (3010) обязан проходить NBT-цикл с ключом (класс дефектов BUG-057: стаб терял NBT).

import static gregapi.data.CS.NBT_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregapi.block.multitileentity.MultiTileEntityClassContainer;
import gregapi.block.multitileentity.MultiTileEntityContainer;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.util.UT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class DungeonMteProbeTest {

	// Все MTE-ID, которые данж-комнаты ставят через aData.set(...)/хелперы (инвентарь architecture/dungeons.md §5).
	// 508+next(3) => 508..510.
	private static final int[] DUNGEON_MTE_IDS = {
		11, 502, 508, 509, 510,             // сундуки (серии aID / 500+aID)
		3010,                               // сейф с замком (NBT_KEY)
		4009, 5011, 6009, 6011,             // фермы/мастерская (серии)
		7110, 7111,                         // книжные полки (7100+aID)
		8010, 8762, 25377,                  // серии
		14999,                              // ZPM
		32055, 32056, 32065, 32074,         // бочки-газ, горшок растений, камень
		32084, 32085, 32086,                // слитки/пластины/гем-пластины
		32104, 32110, 32111,                // Boomstick, Loot Crate, Hand Crank
		32700, 32703, 32705, 32707,         // Coin, Grindstone, Mixing Bowl, Bathing Pot
		32712, 32713, 32716, 32725,         // динамиты, Drum, Funnel
		32730, 32735, 32738, 32739          // Tap, Mortar, Measuring Pot, Cup
	};

	@Test
	void dungeonMteIdsRegisteredAndSafeKeepsKeyNbt(MinecraftServer server) {
		assertNotNull(server, "эфемерный MC-сервер должен подняться");
		// F12-отложка: Loader_MultiTileEntities живёт в deferItemInit (server-start); в тест-контексте
		// onModServerStarting2 не срабатывает — дёргаем явно (тот же приём, что PortDump.runFull).
		gregapi.GT_API.runDeferredItemInit();
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		assertNotNull(tRegistry, "реестр gt.multitileentity должен существовать");

		int tMissing = 0;
		StringBuilder tReport = new StringBuilder("\n[GT6-DUNGEONPROBE] дамп данж-MTE-ID:\n");
		for (int tID : DUNGEON_MTE_IDS) {
			MultiTileEntityClassContainer tClass = tRegistry.getClassContainer(tID);
			if (tClass == null) {
				tMissing++;
				tReport.append(String.format("  %5d: === НЕ ЗАРЕГИСТРИРОВАН ===%n", tID));
			} else {
				tReport.append(String.format("  %5d: %s | %s%n", tID, tRegistry.getLocal(tID), tClass.mClass.getSimpleName()));
			}
		}
		System.out.println(tReport);
		assertTrue(tMissing == 0, "[GT6-DUNGEONPROBE] незарегистрированных данж-MTE-ID: " + tMissing + " (см. дамп выше)");

		// BUG-057-класс: сейф 3010 создаётся с ключом и НЕ теряет его при сериализации BE.
		MultiTileEntityContainer tSafe = tRegistry.getNewTileEntityContainer(3010, UT.NBT.make(NBT_KEY, 12345L));
		assertNotNull(tSafe, "[GT6-DUNGEONPROBE] контейнер сейфа 3010 должен создаваться");
		CompoundTag tOut = new CompoundTag();
		((gregapi.tileentity.base.TileEntityBase01Root)tSafe.mTileEntity).writeToNBT(tOut);
		assertTrue(tOut.getLongOr(NBT_KEY, 0L) == 12345L,
			"[GT6-DUNGEONPROBE] сейф 3010 потерял NBT_KEY при writeToNBT (BUG-057-класс): " + tOut.get(NBT_KEY));

		// Живой тест «в шкафах одна книга»: лут-канал полок = ST.generateLoot по 1.7.10-именам пулов (ваниль-ветка
		// через ServerLifecycleHooks.getCurrentServer — в ЭТОМ тест-контексте сервер туда не регистрируется, ветка
		// молча пропускается → судить пулы здесь нельзя; честный судья — живая проба [GT6-DUNGEONPROBE], фаза 2).
		if (net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
			for (String tLoot : new String[] {"strongholdLibrary", "dungeonChest", "mineshaftCorridor", "strongholdCrossing", "villageBlacksmith"}) {
				gregapi.dummies.DummyInventory tInv = new gregapi.dummies.DummyInventory(27);
				boolean tOk = gregapi.util.ST.generateLoot(gregapi.data.CS.RNGSUS, tLoot, tInv);
				int tCount = 0;
				for (net.minecraft.world.item.ItemStack tStack : tInv.mInventory) if (tStack != null && !tStack.isEmpty()) tCount++;
				System.out.println("[GT6-DUNGEONPROBE] лут-пул '" + tLoot + "': generateLoot=" + tOk + " предметов=" + tCount);
				assertTrue(tOk && tCount > 0, "[GT6-DUNGEONPROBE] лут-пул '" + tLoot + "' пуст (ok=" + tOk + ", count=" + tCount + ") — полки данжа будут пустыми");
			}
		} else System.out.println("[GT6-DUNGEONPROBE] лут-судья пропущен: getCurrentServer=null в тест-контексте (судит живая проба)");
	}
}
