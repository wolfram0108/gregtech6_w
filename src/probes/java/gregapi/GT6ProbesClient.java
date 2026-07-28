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

	// [GT6-UVPROBE] BUG-061, клиентская половина: САМ ЗАМЕР. Рендер живёт только на клиенте, поэтому сервер лишь
	// строит структуру и передаёт координаты, а квады читаются здесь — РЕАЛЬНЫМ путём рендерера секции: та же модель
	// из ModelManager, тот же collectParts с живыми level/pos/state. Судится координата, а не картинка: лежит ли UV
	// каждой вершины внутри своего тайла атласа. Снять при уборке фазы.
	@net.neoforged.bus.api.SubscribeEvent
	public static void onUVProbeClient(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (!gregapi.data.CS.probeFlag("gt6uvprobe.flag")) return;
		if (gregapi.GT6Probes.sUVPClientVerdict != null) return;
		net.minecraft.core.BlockPos tTarget = gregapi.GT6Probes.sUVPTargetPos, tControl = gregapi.GT6Probes.sUVPControlPos;
		if (tTarget == null || tControl == null) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null) return;
		StringBuilder rVerdict = new StringBuilder();
		try {
			uvProbeDiag(tMC.level, tTarget, "ЦЕЛЬ");
			uvProbeDiag(tMC.level, tControl, "КОНТРОЛЬ");
			// 1. Случай воспроизведён? Среди квадов тигля обязаны быть вершины ВНЕ куба 0..1 — иначе стенд
			//    проверяет не тот случай и любой PASS ничего не стоит.
			java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> tQuads = uvProbeQuads(tMC.level, tTarget);
			float tMaxOut = 0;
			for (net.minecraft.client.resources.model.geometry.BakedQuad tQuad : tQuads) for (int i = 0; i < 4; i++) {
				org.joml.Vector3fc tPos = tQuad.position(i);
				tMaxOut = Math.max(tMaxOut, Math.max(Math.max(-tPos.x(), tPos.x()-1), Math.max(Math.max(-tPos.y(), tPos.y()-1), Math.max(-tPos.z(), tPos.z()-1))));
			}
			uvProbeLine(rVerdict, "REPRO-CHECK: тигель реально рисуется ВНЕ куба", "> 0.5 блока за грань", String.format(java.util.Locale.ROOT, "%.3f", tMaxOut), tQuads.size() > 0 && tMaxOut > 0.5f);

			// 2. Главный судья: UV каждой вершины внутри своего спрайта.
			uvProbeUVJudge(rVerdict, "ТИГЕЛЬ: UV внутри своего спрайта (BUG-061)", tQuads);

			// 3. Позитивный контроль: обычный полный блок проходит ту же проверку (судья не всегда FAIL).
			uvProbeUVJudge(rVerdict, "POSITIVE-CONTROL: ванильный камень — UV внутри спрайта", uvProbeQuads(tMC.level, tControl));

			// 4. Чувствительность: та же грань по СТАРОЙ формуле (bounds*16 без страховки 1.7.10) обязана дать
			//    выход за спрайт — иначе проверка не способна поймать дефект вовсе.
			boolean tWouldFail = false; float tWorst = 0;
			for (net.minecraft.client.resources.model.geometry.BakedQuad tQuad : tQuads) {
				net.minecraft.client.renderer.texture.TextureAtlasSprite tSprite = tQuad.materialInfo().sprite();
				for (int i = 0; i < 4; i++) {
					org.joml.Vector3fc tPos = tQuad.position(i);
					// старая формула: offset = координата вершины (0..1 у куба, но у тигля −0.999..3.0) → getU(offset)
					float tOldU = tSprite.getU(tPos.x()), tOldV = tSprite.getV(1 - tPos.y());
					float tOut = Math.max(Math.max(tSprite.getU0()-tOldU, tOldU-tSprite.getU1()), Math.max(tSprite.getV0()-tOldV, tOldV-tSprite.getV1()));
					if (tOut > 1e-5f) {tWouldFail = true; tWorst = Math.max(tWorst, tOut);}
				}
			}
			uvProbeLine(rVerdict, "SENSITIVITY: старая формула дала бы UV ВНЕ спрайта", "выход > 0", String.format(java.util.Locale.ROOT, "%.5f", tWorst), tWouldFail);
		} catch (Throwable e) {
			uvProbeLine(rVerdict, "клиентский замер без исключений", "без EXC", String.valueOf(e), false);
			e.printStackTrace(gregapi.data.CS.ERR);
		}
		gregapi.GT6Probes.sUVPClientVerdict = rVerdict.toString();
		gregapi.data.CS.OUT.println("[GT6-UVPROBE] клиент отдал вердикт:\n" + rVerdict);
	}
	/** Что клиент реально видит в этой точке: без этого «квадов 0» нечем объяснить (нет блока? нет BE? нет модели?). */
	private static void uvProbeDiag(net.minecraft.client.multiplayer.ClientLevel aLevel, net.minecraft.core.BlockPos aPos, String aLabel) {
		net.minecraft.world.level.block.state.BlockState tState = aLevel.getBlockState(aPos);
		net.minecraft.world.level.block.entity.BlockEntity tBE = aLevel.getBlockEntity(aPos);
		Object tModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(tState);
		gregapi.data.CS.OUT.println("[GT6-UVPROBE] DIAG " + aLabel + " " + aPos
			+ " блок=" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tState.getBlock())
			+ " BE=" + (tBE == null ? "null" : tBE.getClass().getSimpleName())
			+ " модель=" + (tModel == null ? "null" : tModel.getClass().getSimpleName()));
	}

	/** Квады блока РЕАЛЬНЫМ путём рендера. Путей ДВА, и выбирает их сам мод:
	 *  · MTE-блоки рисует НЕ baked-модель, а {@link gregapi.render.MultiTileEntityBER} — секционный мешер
	 *    не отдаёт MTE-BE, поэтому geometry собирается на кадре из живого BE через
	 *    {@code GT6BlockModel.buildRendererQuads} (MultiTileEntityBER:126-128). Стенд зовёт ТОТ ЖЕ вызов
	 *    с тем же BE — иначе судил бы пустоту (первый прогон: «квадов 0» именно поэтому).
	 *  · остальные блоки — обычная модель из ModelManager + collectParts с живыми level/pos/state. */
	private static java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> uvProbeQuads(net.minecraft.client.multiplayer.ClientLevel aLevel, net.minecraft.core.BlockPos aPos) {
		net.minecraft.world.level.block.state.BlockState tState = aLevel.getBlockState(aPos);
		net.minecraft.world.level.block.entity.BlockEntity tBE = aLevel.getBlockEntity(aPos);
		if (tBE instanceof gregapi.render.IRenderedBlockObject tRenderer && tState.getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock tMTEBlock) {
			gregapi.render.GT6QuadBuilder tQB = new gregapi.render.GT6QuadBuilder();
			gregapi.render.GT6BlockModel.buildRendererQuads(tQB, tRenderer, tMTEBlock, aLevel, aPos.getX(), aPos.getY(), aPos.getZ());
			return tQB.quads();
		}
		net.minecraft.client.renderer.block.dispatch.BlockStateModel tModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(tState);
		java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> tParts = new java.util.ArrayList<>();
		tModel.collectParts(aLevel, aPos, tState, net.minecraft.util.RandomSource.create(42L), tParts);
		java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> rQuads = new java.util.ArrayList<>();
		for (net.minecraft.client.renderer.block.dispatch.BlockStateModelPart tPart : tParts) {
			java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> tNull = tPart.getQuads(null);
			if (tNull != null) rQuads.addAll(tNull);
			for (net.minecraft.core.Direction tDir : net.minecraft.core.Direction.values()) {
				java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> tSide = tPart.getQuads(tDir);
				if (tSide != null) rQuads.addAll(tSide);
			}
		}
		return rQuads;
	}
	/** UV каждой вершины обязаны лежать внутри тайла своего спрайта; иначе движок сэмплит соседей по атласу. */
	private static void uvProbeUVJudge(StringBuilder aOut, String aName, java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> aQuads) {
		float tWorst = 0; int tBad = 0;
		for (net.minecraft.client.resources.model.geometry.BakedQuad tQuad : aQuads) {
			net.minecraft.client.renderer.texture.TextureAtlasSprite tSprite = tQuad.materialInfo().sprite();
			for (int i = 0; i < 4; i++) {
				float tU = net.minecraft.client.model.geom.builders.UVPair.unpackU(tQuad.packedUV(i));
				float tV = net.minecraft.client.model.geom.builders.UVPair.unpackV(tQuad.packedUV(i));
				float tOut = Math.max(Math.max(tSprite.getU0()-tU, tU-tSprite.getU1()), Math.max(tSprite.getV0()-tV, tV-tSprite.getV1()));
				if (tOut > 1e-5f) {tBad++; tWorst = Math.max(tWorst, tOut);}
			}
		}
		uvProbeLine(aOut, aName, "вершин вне спрайта 0", tBad + " (макс.выход " + String.format(java.util.Locale.ROOT, "%.5f", tWorst) + ", квадов " + aQuads.size() + ")", tBad == 0 && !aQuads.isEmpty());
	}
	private static void uvProbeLine(StringBuilder aOut, String aName, String aExpected, String aActual, boolean aPass) {
		aOut.append(aName).append('|').append(aExpected).append('|').append(aActual).append('|').append(aPass ? "PASS" : "FAIL").append('\n');
	}

	// [GT6-NAMEPROBE] BUG-066: ИНВЕНТАРИЗАЦИЯ ПОДПИСЕЙ (не судья — список фактов). Игрок видит сырые ключи вместо
	// имён у воды GT6 и части дерева; вместо угадывания перечисляем ВСЕ предметы мода, чьё отображаемое имя
	// выглядит нераспознанным ключом. Меряем НА КЛИЕНТЕ и тем же вызовом, которым имя берут тултип и Jade —
	// ItemStack.getHoverName() (урок BUG-064: серверная сторона показывает не то, что видит игрок).
	// Снять при уборке фазы.
	private static boolean mNameProbeDone = false;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onNameProbeClient(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mNameProbeDone || !gregapi.data.CS.probeFlag("gt6nameprobe.flag")) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		mNameProbeDone = true;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-NAMEPROBE] подписи предметов мода: что реально видит игрок ==========");
		int tTotal = 0, tRaw = 0;
		java.util.List<String> tBroken = new java.util.ArrayList<>();
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			net.minecraft.resources.Identifier tID = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tID == null || !(tID.getNamespace().startsWith("gregtech") || tID.getNamespace().startsWith("gregapi"))) continue;
			// у мета-предметов подпись зависит от подтипа — проверяем несколько
			for (int tMeta : new int[]{0, 1, 2}) {
				net.minecraft.world.item.ItemStack tStack = gregapi.util.ST.make(tItem, 1, tMeta);
				if (tStack == null || tStack.isEmpty()) continue;
				String tName;
				try {tName = tStack.getHoverName().getString();} catch (Throwable e) {tName = "EXC " + e;}
				tTotal++;
				// «сырой ключ» = движок не нашёл перевода и вернул сам ключ
				if (tName.startsWith("item.") || tName.startsWith("block.") || tName.startsWith("gt.")) {
					tRaw++;
					if (tBroken.size() < 60) tBroken.add(tID + "#" + tMeta + " -> " + tName + "   [класс предмета: " + tItem.getClass().getSimpleName() + "]");
				}
				if (tMeta == 0 && !(tItem instanceof net.minecraft.world.item.BlockItem)) break; // не-блочные подтипы не перебираем
			}
		}
		O.println("[GT6-NAMEPROBE] проверено подписей: " + tTotal + ", СЫРЫХ (игрок видит ключ): " + tRaw);
		for (String tLine : tBroken) O.println("[GT6-NAMEPROBE] СЫРАЯ: " + tLine);
		// отдельно — ровно те два случая из репорта игрока
		for (net.minecraft.world.level.block.Block tBlock : new net.minecraft.world.level.block.Block[]{
				gregapi.data.CS.BlocksGT.River, gregapi.data.CS.BlocksGT.Ocean, gregapi.data.CS.BlocksGT.Swamp
			, gregapi.data.CS.BlocksGT.OilHeavy, gregapi.data.CS.BlocksGT.Beam1}) {
			if (tBlock == null) continue;
			net.minecraft.world.item.ItemStack tStack = gregapi.util.ST.make(tBlock, 1, 0);
			O.println("[GT6-NAMEPROBE] РЕПОРТ-СЛУЧАЙ " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock)
				+ " -> предмет=" + (tStack == null || tStack.isEmpty() ? "НЕТ ПРЕДМЕТА" : tStack.getItem().getClass().getSimpleName() + " подпись=«" + tStack.getHoverName().getString() + "»"));
		}
		O.println("========== [GT6-NAMEPROBE] DONE ==========");
	}

	// [GT6-JADECLIENT] BUG-064: почему в ИГРЕ нет строки инструмента, хотя прежний стенд давал 6/6.
	// Прежний судья считал на СЕРВЕРНОЙ стороне, а тултип рисуется на КЛИЕНТСКОЙ — здесь замер идёт ровно там,
	// где смотрит игрок, и по тем же вызовам, что делает Jade (исходники, ветка 26.1-neoforge):
	//   · инструмент  — HarvestToolProvider.getTool(state, level, pos), HarvestToolProvider.java:118-131;
	//   · содержимое  — level.getCapability(Capabilities.Item.BLOCK, …), CommonProxy.java:290-297.
	// Это инвентаризация фактов, а не судья. Снять при уборке фазы.
	private static boolean mJadeClientDone = false;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onJadeClientProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mJadeClientDone || !gregapi.data.CS.probeFlag("gt6jadeclient.flag")) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		// ищем ЛЮБУЮ машину GT6 среди клиентских BE рядом с игроком
		net.minecraft.core.BlockPos tCenter = tMC.player.blockPosition();
		net.minecraft.core.BlockPos tFound = null, tAnyMTE = null;
		for (int dx = -24; dx <= 24 && tFound == null; dx++) for (int dy = -8; dy <= 8 && tFound == null; dy++) for (int dz = -24; dz <= 24 && tFound == null; dz++) {
			net.minecraft.core.BlockPos tPos = tCenter.offset(dx, dy, dz);
			net.minecraft.world.level.block.Block tB = tMC.level.getBlockState(tPos).getBlock();
			if (!(tB instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) continue;
			if (tAnyMTE == null) tAnyMTE = tPos;
			// целимся именно в МАШИНУ (её инструмент — гаечный ключ GT6), а не в первый попавшийся MTE-блок:
			// прошлый замер попал на декоративный камень, у которого инструмент ванильный, и вопрос остался открыт
			net.minecraft.resources.Identifier tID = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tB);
			if (tID != null && tID.getPath().contains(".machine.")) tFound = tPos;
		}
		if (tFound == null) tFound = tAnyMTE;
		if (tFound == null) return; // машины рядом ещё нет — ждём (её ставит демо-проба)
		mJadeClientDone = true;

		net.minecraft.world.level.block.state.BlockState tState = tMC.level.getBlockState(tFound);
		net.minecraft.world.level.block.Block tBlock = tState.getBlock();
		net.minecraft.world.level.block.entity.BlockEntity tBE = tMC.level.getBlockEntity(tFound);
		O.println("========== [GT6-JADECLIENT] BUG-064: замер НА КЛИЕНТЕ @" + tFound + " ==========");
		O.println("[GT6-JADECLIENT] блок=" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock)
			+ " BE=" + (tBE == null ? "НЕТ (клиентский BE не реконструирован!)" : tBE.getClass().getSimpleName()));
		O.println("[GT6-JADECLIENT] requiresCorrectToolForDrops=" + tState.requiresCorrectToolForDrops()
			+ " (при false строка рисуется только если включена опция «Effective Tool»)");
		int tMeta = gregapi.util.WD.meta(tMC.level, tFound.getX(), tFound.getY(), tFound.getZ());
		String tTool = tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock tM ? tM.getHarvestTool(tMeta) : "<не MTE>";
		O.println("[GT6-JADECLIENT] мета на клиенте=" + tMeta + ", инструмент по GT6=" + tTool);
		try {
			O.println("[GT6-JADECLIENT] обработчиков инструментов в Jade: " + snownee.jade.addon.harvest.HarvestToolProvider.TOOL_HANDLERS.size()
				+ " (наших GT6: " + snownee.jade.addon.harvest.HarvestToolProvider.TOOL_HANDLERS.keySet().stream().filter(k -> k.getNamespace().startsWith("gregtech")).count() + ")");
			java.util.List<net.minecraft.world.item.ItemStack> tTools = snownee.jade.addon.harvest.HarvestToolProvider.getTool(tState, tMC.level, tFound);
			StringBuilder tNames = new StringBuilder();
			for (net.minecraft.world.item.ItemStack tS : tTools) tNames.append(tNames.length() == 0 ? "" : ", ").append(tS.getHoverName().getString());
			O.println("[GT6-JADECLIENT] Jade.getTool НА КЛИЕНТЕ отдал: " + tTools.size() + (tTools.isEmpty() ? " <ПУСТО — вот дефект>" : " (" + tNames + ")"));
		} catch (Throwable e) {O.println("[GT6-JADECLIENT] Jade недоступен/EXC: " + e);}
		// содержимое инвентаря — тем же каналом, которым его ищет Jade
		try {
			Object tHandler = tMC.level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, tFound, tState, tBE, null);
			O.println("[GT6-JADECLIENT] капа инвентаря (Capabilities.Item.BLOCK): " + (tHandler == null ? "НЕТ" : tHandler.getClass().getSimpleName()
				+ ", слотов " + ((net.neoforged.neoforge.transfer.ResourceHandler<?>)tHandler).size()));
		} catch (Throwable e) {O.println("[GT6-JADECLIENT] капа инвентаря EXC: " + e);}
		O.println("[GT6-JADECLIENT] Container у BE: " + (tBE instanceof net.minecraft.world.Container tC ? "да, слотов " + tC.getContainerSize() : "нет"));
		O.println("========== [GT6-JADECLIENT] DONE ==========");
	}

	// [GT6-JADELEVEL] BUG-070: СУДЬЯ ВИТРИНЫ ДОБЫЧИ В JADE. Игрок просил три вещи: тип инструмента, требуемый уровень
	// (любой, а не только ванильные три ступени) и соответствие того, что в руке. Судится НЕ картинка, а данные ровно
	// тех вызовов, из которых Jade строит тултип, и ровно на той стороне, где он его строит, — на КЛИЕНТЕ:
	//   П1 · тип инструмента — HarvestToolProvider.getTool(state, level, pos) (HarvestToolProvider.java:55-64);
	//        сверх того спрашиваем ПОШТУЧНО каждый обработчик: чей ответ сделал список непустым. Это встроенный
	//        контроль дефекта — у блока с уровнем выше 3 ванильный обработчик обязан молчать (тега нет,
	//        GT6HarvestTags:80), и если бы не наш, список остался бы пустым, как и было в репорте игрока.
	//   П2 · требуемый уровень — WD.harvestLevel(level,x,y,z) на КЛИЕНТЕ против того же значения на СЕРВЕРЕ
	//        (интегрированный сервер того же мира). Это судит настоящий клиентский риск: у GT6 подтип живёт в BE,
	//        и если он не доехал до клиента, витрина покажет 0 при честной механике.
	//   П3 · соответствие в руке — EventHooks.doPlayerHarvestCheck: то же событие, которым игра решает судьбу дропа
	//        (наш слушатель GT_API_Proxy.onPlayerHarvestCheckEvent). Витрина не имеет права разойтись с механикой:
	//        сверяем ответ события с арифметикой «ярус в руке >= требуемый уровень».
	// ПОЗИТИВНЫЙ КОНТРОЛЬ: ванильный камень — обязан обслуживаться ванильным обработчиком и НЕ нашим.
	// Кейсы берутся из мира вокруг игрока (полигон gt6toolyard даёт и низкие тиры, и высокие). Снять при уборке фазы.
	private static boolean mJadeLevelDone = false;
	private static int mJadeLevelWaited = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onJadeLevelProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mJadeLevelDone || !gregapi.data.CS.probeFlag("gt6jadelevelprobe.flag")) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		java.io.PrintStream O = gregapi.data.CS.OUT;

		// собираем кейсы: по одному представителю на пару (инструмент, требуемый уровень) — так в выборку попадают
		// и блоки внутри ванильной шкалы, и те, что выше неё (ради которых всё и делается)
		net.minecraft.core.BlockPos tCenter = tMC.player.blockPosition();
		java.util.LinkedHashMap<String, net.minecraft.core.BlockPos> tCases = new java.util.LinkedHashMap<>();
		for (int dx = -40; dx <= 40; dx++) for (int dy = -6; dy <= 6; dy++) for (int dz = -40; dz <= 40; dz++) {
			net.minecraft.core.BlockPos tPos = tCenter.offset(dx, dy, dz);
			net.minecraft.world.level.block.Block tB = tMC.level.getBlockState(tPos).getBlock();
			if (!(tB instanceof gregapi.block.IBlock)) continue;
			try {
				int tMeta = gregapi.util.WD.meta(tMC.level, tPos.getX(), tPos.getY(), tPos.getZ());
				String tTool = gregapi.util.WD.harvestTool(tB, tMeta);
				if (tTool == null || tTool.isEmpty()) continue;
				tCases.putIfAbsent(tTool + "/" + gregapi.util.WD.harvestLevel(tMC.level, tPos.getX(), tPos.getY(), tPos.getZ()), tPos);
			} catch (Throwable e) {/* блок ещё не догрузился */}
		}
		// ЖДЁМ ГОТОВНОСТИ МИРА, а не «первых четырёх блоков». Прошлый прогон судил на 1-м тике после входа и дал два
		// FAIL, которые оказались артефактом момента: чанки только что сгенерированы, BE-подтип ещё ехал на клиент,
		// инвентарь пуст. Условие готовности: полигон построен (есть блок с уровнем ВЫШЕ ванильной шкалы — ради них
		// всё и делается) и в руке есть инструмент (иначе П3 нечем судить). Критерии вердикта при этом НЕ меняются.
		boolean tHasHighTier = false;
		for (String tKey : tCases.keySet()) try {if (Integer.parseInt(tKey.substring(tKey.indexOf('/') + 1)) > 3) {tHasHighTier = true; break;}} catch (Throwable e) {/* ключ без числа */}
		boolean tHasTool = !tMC.player.getMainHandItem().isEmpty();
		if ((!tHasHighTier || !tHasTool) && mJadeLevelWaited++ < 3600) return;
		mJadeLevelDone = true;
		if (!tHasHighTier) O.println("[GT6-JADELEVEL] ⚠ блока с уровнем выше 3 в мире не нашлось — главный случай НЕ проверен (нужен полигон gt6toolyard)");
		if (!tHasTool) O.println("[GT6-JADELEVEL] ⚠ рука пуста — П3 судится вырожденно");

		O.println("========== [GT6-JADELEVEL] BUG-070: витрина добычи в Jade (замер НА КЛИЕНТЕ) ==========");
		// эталон берём из СЕРВЕРНОЙ половины (снят на серверном тике). Читать серверный мир отсюда нельзя: в первом
		// прогоне это дало 0 там, где сервер знает 4, — BlockEntity чужому потоку не отдаётся, и «расхождение» было
		// дефектом замера, а не витрины.
		java.util.Map<Long, Integer> tExpect = gregapi.GT6Probes.sJadeLevelExpect;
		O.println("[GT6-JADELEVEL] эталон уровня: серверная половина, снято позиций " + tExpect.size());
		// РЕЖИМ ИГРЫ — обязателен в протоколе: в КРЕАТИВЕ Jade намеренно гасит свою витрину добычи
		// (HarvestToolProvider:82, гейт MC_HARVEST_TOOL_CREATIVE), и замер в креативе ничего не доказывает.
		O.println("[GT6-JADELEVEL] режим игрока: " + (tMC.player.isCreative() ? "КРЕАТИВ — витрина Jade намеренно погашена, замер значка недействителен" : tMC.player.isSpectator() ? "НАБЛЮДАТЕЛЬ" : "ВЫЖИВАНИЕ (верно)"));
		try {
			O.println("[GT6-JADELEVEL] обработчиков инструментов у Jade: " + snownee.jade.addon.harvest.HarvestToolProvider.TOOL_HANDLERS.size()
				+ ", из них наших: " + snownee.jade.addon.harvest.HarvestToolProvider.TOOL_HANDLERS.keySet().stream().filter(k -> k.getNamespace().startsWith("gregtech")).count());
		} catch (Throwable e) {O.println("[GT6-JADELEVEL] Jade недоступен: " + e); O.println("========== [GT6-JADELEVEL] DONE =========="); return;}

		int tPass = 0, tFail = 0;
		// ПОЛНОТА КЛАССА, а не выборки: обходим ВЕСЬ реестр блоков и собираем типы инструментов, которые GT6-блоки
		// реально требуют. Каждый такой тип обязан быть покрыт каким-то обработчиком Jade (нашим или его собственным),
		// иначе для целого семейства блоков витрина останется пустой — и обнаружится это только жалобой игрока.
		// Инструмент от меты не зависит (BlockBase:191, PrefixBlock:715, MultiTileEntityBlock:582 — константа),
		// поэтому спрашиваем с метой 0.
		java.util.TreeMap<String, Integer> tDemanded = new java.util.TreeMap<>();
		for (net.minecraft.world.level.block.Block tB : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (!(tB instanceof gregapi.block.IBlock)) continue;
			try {String t = gregapi.util.WD.harvestTool(tB, 0); if (t != null && !t.isEmpty()) tDemanded.merge(t, 1, Integer::sum);} catch (Throwable e) {/* блок без канала */}
		}
		java.util.Set<String> tCovered = new java.util.TreeSet<>();
		try {
			for (net.minecraft.resources.Identifier tUID : snownee.jade.addon.harvest.HarvestToolProvider.TOOL_HANDLERS.keySet()) {
				String tPath = tUID.getPath();
				tCovered.add(tPath.startsWith("tool/") ? tPath.substring(5) : tPath); // наши — «tool/<тип>», его — просто «<тип>»
			}
		} catch (Throwable e) {/* Jade недоступен */}
		java.util.List<String> tUncovered = new java.util.ArrayList<>();
		for (java.util.Map.Entry<String, Integer> tD : tDemanded.entrySet()) if (!tCovered.contains(tD.getKey())) tUncovered.add(tD.getKey() + "×" + tD.getValue());
		O.println("[GT6-JADELEVEL] ПОЛНОТА: типов инструмента требуют блоки — " + tDemanded.size() + " " + tDemanded.keySet()
			+ ", обработчиков на типы — " + tCovered.size());
		if (tUncovered.isEmpty()) {tPass++; O.println("[GT6-JADELEVEL] ПОЛНОТА -> PASS (непокрытых типов нет)");}
		else {tFail++; O.println("[GT6-JADELEVEL] ПОЛНОТА -> FAIL · нет обработчика для типов: " + tUncovered + " — у этих блоков витрина будет пустой");}

		java.util.List<Object[]> tAll = new java.util.ArrayList<>();
		for (java.util.Map.Entry<String, net.minecraft.core.BlockPos> tE : tCases.entrySet()) tAll.add(new Object[]{tE.getKey(), tE.getValue(), Boolean.FALSE});
		// позитивный контроль — ванильный блок: его витрину рисует Jade сам, нашего обработчика там быть не должно
		tAll.add(new Object[]{"POSITIVE-CONTROL ванильный камень", tCenter.offset(0, -1, 0), Boolean.TRUE});

		for (Object[] tCase : tAll) {
			String tLabel = (String) tCase[0];
			net.minecraft.core.BlockPos tPos = (net.minecraft.core.BlockPos) tCase[1];
			boolean tControl = (Boolean) tCase[2];
			net.minecraft.world.level.block.state.BlockState tState = tMC.level.getBlockState(tPos);
			net.minecraft.world.level.block.Block tBlock = tState.getBlock();
			boolean tIsGT = tBlock instanceof gregapi.block.IBlock;
			if (tControl && tIsGT) {O.println("[GT6-JADELEVEL] контроль пропущен: под игроком GT6-блок, а не ванильный — переставьте площадку"); continue;}

			int tMeta = 0, tClientLevel = -1, tServerLvl = -1, tHeldLevel = -1;
			String tTool = "";
			try {
				tMeta = gregapi.util.WD.meta(tMC.level, tPos.getX(), tPos.getY(), tPos.getZ());
				tTool = gregapi.util.WD.harvestTool(tBlock, tMeta);
				tClientLevel = gregapi.util.WD.harvestLevel(tMC.level, tPos.getX(), tPos.getY(), tPos.getZ());
				Integer tFromServer = tExpect.get(tPos.asLong());
				if (tFromServer != null) tServerLvl = tFromServer;
				tHeldLevel = gregapi.util.WD.toolLevel(tMC.player.getMainHandItem(), tTool);
			} catch (Throwable e) {/* останутся -1 */}

			// П1: чей обработчик отдал инструмент
			java.util.List<String> tAnswered = new java.util.ArrayList<>();
			try {
				for (java.util.Map.Entry<net.minecraft.resources.Identifier, snownee.jade.addon.harvest.ToolHandler> tH : snownee.jade.addon.harvest.HarvestToolProvider.TOOL_HANDLERS.entrySet())
					if (!tH.getValue().test(tState, tMC.level, tPos).isEmpty()) tAnswered.add(tH.getKey().toString());
			} catch (Throwable e) {tAnswered.add("EXC " + e);}
			boolean tOurs = tAnswered.stream().anyMatch(s -> s.startsWith("gregtech"));
			boolean tVanillaAnswered = tAnswered.stream().anyMatch(s -> !s.startsWith("gregtech"));
			// тег именно СВОЕГО инструмента (через тот же центр, которым размечает мод) — «чужой» тег ни о чём не говорит
			net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tTagOfTool = gregapi.data.GT6HarvestTags.mineableTag(tTool);
			boolean tHasVanillaTag = tTagOfTool != null && tState.is(tTagOfTool);

			// П3: право по мнению движка против арифметики уровней
			boolean tEngineSaysCan = false;
			try {tEngineSaysCan = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(tMC.player, tState, tMC.level, tPos);} catch (Throwable e) {/* останется false */}
			boolean tArithmeticSaysCan = tHeldLevel >= 0 && tHeldLevel >= tClientLevel;

			// улики для разбора FAIL: разметил ли блок наш механизм тегов и есть ли вообще предметы такого типа
			int tRegistrySize = -1;
			try {tRegistrySize = gregapi.data.CS.ToolsGT.list(tTool).size();} catch (Throwable e) {/* тип неизвестен реестру */}

			// П2/П3 ПО ФАКТУ СТРОКИ, а не по величинам: собираем тултип РЕАЛЬНЫМ путём Jade — теми же провайдерами,
			// что он вызывает при наведении, — и читаем получившийся текст. Иначе «строка есть» не доказано ничем:
			// величины могут быть верны, а провайдер не зарегистрирован или отключён конфигом.
			String tLine = "";
			try {
				snownee.jade.impl.Tooltip tTip = new snownee.jade.impl.Tooltip();
				snownee.jade.api.BlockAccessor tAcc = new snownee.jade.impl.BlockAccessorImpl.Builder()
					.level(tMC.level).player(tMC.player).blockState(tState)
					.blockEntity(() -> tMC.level.getBlockEntity(tPos))
					.hit(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tPos), net.minecraft.core.Direction.UP, tPos, false))
					.showDetails(false).serverConnected(false).serverData(null).build();
				snownee.jade.api.config.IPluginConfig tCfg = snownee.jade.api.config.IWailaConfig.get().plugin();
				for (snownee.jade.api.IComponentProvider<snownee.jade.api.BlockAccessor> tProv
					: snownee.jade.impl.WailaClientRegistration.instance().getBlockProviders(tBlock, p -> true))
					try {tProv.appendTooltip(tTip, tAcc, tCfg);} catch (Throwable e) {/* чужой провайдер упал — не наша беда */}
				tLine = String.valueOf(tTip.getNarration()).replace("\n", " | ");
			} catch (Throwable e) {tLine = "<СБОРКА ТУЛТИПА УПАЛА: " + e + ">";}

			java.util.List<String> tWhy = new java.util.ArrayList<>();
			if (tControl) {
				if (!tVanillaAnswered) tWhy.add("ванильный блок остался без витрины (обработчик Jade не ответил)");
				if (tOurs) tWhy.add("на ванильный блок ответил НАШ обработчик — вторжение в чужую шкалу");
			} else {
				// ТРЕБУЕТ ли блок инструмента вообще. Для блока, который берётся рукой, Jade НАМЕРЕННО не рисует значок
				// (SimpleToolHandler:44 — skipInstaBreakingBlock), и сам GT6 в тултипе пишет «Hand-Harvestable»
				// (LH.getToolTipHarvest:275). Требовать витрину там, где инструмент не нужен, — ожидание строже
				// оригинала: ровно на этом судья BUG-071 уже ошибался один раз.
				boolean tNeedsTool = true;
				try {tNeedsTool = !gregapi.util.WD.getMaterial(tBlock).isToolNotRequired();} catch (Throwable e) {/* считаем, что требуется */}
				if (tNeedsTool && tAnswered.isEmpty()) tWhy.add("П1: инструмент не показан НИКЕМ — тултип останется пустым");
				// П2 ПО ФАКТУ: в собранном тултипе обязан быть требуемый тир. Это то, что игрок видит глазами,
				// и в отличие от значка Jade наша строка не гаснет в креативе (гейт HarvestToolProvider:82
				// закрывает от креатива только ЕГО собственную витрину).
				if (tNeedsTool && !tLine.contains(String.valueOf(tClientLevel))) tWhy.add("П2: в тултипе нет требуемого тира " + tClientLevel + " · собрано: «" + tLine + "»");
				// П3 — НЕ наша строка: значок соответствия ставит сам Jade по вердикту движка. Наша задача обратная —
				// НЕ дублировать его. Поэтому судим отсутствие: своего ✔/✘ и своего «In Hand» в тултипе быть не должно.
				if (tLine.contains("In Hand")) tWhy.add("П3: вернулся убранный «In Hand» — дубль витрины Jade · собрано: «" + tLine + "»");
				// ДУБЛЬ — это ОДИН И ТОТ ЖЕ инструмент, показанный дважды, а не «Jade и мы ответили вместе».
				// Разные типы — это не дубль, а заказ игрока: «иконка инструмента всегда, всех подходящих».
				// У мрамора законно отвечают кирка (Jade), молот и зубило (наши) — все трое его реально берут.
				// Сравниваем ТИПЫ: наш UID — «tool/<тип>», у Jade — просто «<тип>».
				java.util.List<String> tDup = new java.util.ArrayList<>();
				for (String tA : tAnswered) {
					if (!tA.startsWith("gregtech")) continue;
					String tType = tA.substring(tA.lastIndexOf('/') + 1);
					for (String tB : tAnswered) if (!tB.startsWith("gregtech") && tB.endsWith(":" + tType)) tDup.add(tType);
				}
				if (!tDup.isEmpty()) tWhy.add("П1: один и тот же инструмент показан дважды: " + tDup + " · ответили " + tAnswered);
				if (tServerLvl >= 0 && tClientLevel != tServerLvl) tWhy.add("П2: уровень на клиенте " + tClientLevel + " != серверного " + tServerLvl + " — витрина врёт");
				if (tClientLevel < 0) tWhy.add("П2: уровень не вычислен");
				// П3 судится ТОЛЬКО когда в руке предмет нужного класса (tHeldLevel >= 0). При чужом классе правило
				// 1.7.10 (ForgeHooks.canHarvestBlock:109-113) отдаёт ВАНИЛЬНЫЙ вердикт, а не false — сравнивать
				// движок с арифметикой «ярус >= уровня» там нельзя: это ожидание строже оригинала.
				if (tNeedsTool && tHeldLevel >= 0 && tEngineSaysCan != tArithmeticSaysCan) tWhy.add("П3: движок говорит " + tEngineSaysCan + ", а по уровням " + tArithmeticSaysCan + " — витрина разойдётся с механикой");
			}
			boolean tOK = tWhy.isEmpty();
			if (tOK) tPass++; else tFail++;
			O.println(String.format("[GT6-JADELEVEL] %-28s %-46s мета=%-5d уровень: клиент=%-3d сервер=%-3d инстр=%-12s в руке=%-3d право(движок)=%-5s ванил.тег=%-5s предметов=%-3d ответили=%s%n"
				+ "                               строка Jade: «%s» -> %s%s",
				tLabel, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock), tMeta, tClientLevel, tServerLvl, tTool, tHeldLevel, tEngineSaysCan,
				tHasVanillaTag, tRegistrySize, tAnswered.isEmpty() ? "<НИКТО>" : tAnswered, tLine, tOK ? "PASS" : "FAIL", tOK ? "" : " · " + String.join("; ", tWhy)));
		}
		O.println("[GT6-JADELEVEL] ИТОГ: PASS " + tPass + ", FAIL " + tFail + " (кейсов из мира " + tCases.size() + ")");
		O.println("========== [GT6-JADELEVEL] DONE ==========");
	}

	// [GT6-ITEMMODELPROBE] BUG-068: судья ITEM-МОДЕЛИ. Игрок видит у предмета воды GT6 пурпурную заглушку — это признак
	// «модели нет вовсе» (JSON-моделей в моде НЕТ, все модели инжектируются рантаймом; ModelManager.getItemModel:90-97 на
	// промахе пишет «Missing item model» и отдаёт missing-модель). Судится НЕ картинка, а ДАННЫЕ рендера, и берутся они
	// ровно тем вызовом, которым их берёт GUI/Jade: ItemModelResolver.updateForTopItem (ItemModelResolver.java:41-49).
	// Признак предмета без модели — particle-материал из missing-модели (minecraft:missingno) либо пустой render-state.
	// Замер идёт В МИРЕ: на TitleScreen ItemStack ещё нельзя построить («Components not bound yet» —
	// Holder$Reference.components:273, реестровые компоненты предметов связываются при загрузке мира). Снять при уборке фазы.
	private static boolean mItemModelDone = false;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onItemModelProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mItemModelDone || !gregapi.data.CS.probeFlag("gt6itemmodelprobe.flag")) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null || tMC.getModelManager() == null) return;
		mItemModelDone = true;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-ITEMMODELPROBE] BUG-068: есть ли у предмета модель (реальный путь GUI/Jade) ==========");
		StringBuilder tOut = new StringBuilder();
		int tPass = 0, tFail = 0;
		// ЦЕЛЬ репорта + позитивные контроли. Ожидание для водоподобных — ванильная вода (1.7.10 BlockWaterlike:200
		// getIcon → Blocks.water.getIcon), для нефти — своя текстура жидкости, для камня — ванильная модель движка.
		Object[][] tCases = {
			{"ЦЕЛЬ река  (BUG-068)"      , gregapi.data.CS.BlocksGT.River   , "block/water_still"},
			{"ЦЕЛЬ океан (BUG-068)"      , gregapi.data.CS.BlocksGT.Ocean   , "block/water_still"},
			{"ЦЕЛЬ болото (BUG-068)"     , gregapi.data.CS.BlocksGT.Swamp   , "block/water_still"},
			{"POSITIVE-CONTROL нефть"    , gregapi.data.CS.BlocksGT.OilHeavy, null},
			{"POSITIVE-CONTROL ванильный камень", net.minecraft.world.level.block.Blocks.STONE, null},
		};
		for (Object[] tCase : tCases) {
			String tLabel = (String)tCase[0];
			net.minecraft.world.level.block.Block tBlock = (net.minecraft.world.level.block.Block)tCase[1];
			String tWantSprite = (String)tCase[2];
			if (tBlock == null) {itemModelLine(tOut, tLabel, "блок существует", "БЛОКА НЕТ (null)", false); tFail++; continue;}
			net.minecraft.world.item.ItemStack tStack = gregapi.util.ST.make(tBlock, 1, 0); // центр мода (тот же, что у nameprobe)
			String tSprite = itemModelSprite(tStack);
			boolean tOk = tSprite != null && !tSprite.contains("missingno") && (tWantSprite == null || tSprite.contains(tWantSprite));
			itemModelLine(tOut, tLabel + " " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock),
				tWantSprite == null ? "модель есть, спрайт не missingno" : "спрайт " + tWantSprite, String.valueOf(tSprite), tOk);
			if (tOk) tPass++; else tFail++;
		}
		// SENSITIVITY: судья обязан УМЕТЬ дать FAIL — подсовываем стек с заведомо несуществующей моделью.
		// Без этой строки «все PASS» ничего не доказывают (урок: судья без контроля — не судья).
		net.minecraft.world.item.ItemStack tBogus = gregapi.util.ST.make(net.minecraft.world.level.block.Blocks.STONE, 1, 0);
		tBogus.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, net.minecraft.resources.Identifier.fromNamespaceAndPath("gregtech", "probe_nonexistent_model"));
		String tBogusSprite = itemModelSprite(tBogus);
		boolean tSensOk = tBogusSprite == null || tBogusSprite.contains("missingno");
		itemModelLine(tOut, "SENSITIVITY: предмет с несуществующей моделью", "missingno / пусто", String.valueOf(tBogusSprite), tSensOk);
		if (tSensOk) tPass++; else tFail++;
		// РЕГРЕСС-КОНТРОЛЬ мирового рендера: водоподобные обязаны остаться INVISIBLE (их рисует vanilla FluidRenderer по
		// getFluidState — SectionCompiler.java:99-106), у нефти — обычная модель. Иначе фикс item-формы задел бы мир.
		for (net.minecraft.world.level.block.Block tBlock : new net.minecraft.world.level.block.Block[]{
				gregapi.data.CS.BlocksGT.River, gregapi.data.CS.BlocksGT.Ocean, gregapi.data.CS.BlocksGT.Swamp}) {
			if (tBlock == null) continue;
			net.minecraft.world.level.block.state.BlockState tState = tBlock.defaultBlockState();
			boolean tOk = tState.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE && !tState.getFluidState().isEmpty();
			itemModelLine(tOut, "REGRESS: мировой рендер " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock) + " не задет",
				"INVISIBLE + FluidState воды", tState.getRenderShape() + " + fluid=" + (tState.getFluidState().isEmpty() ? "НЕТ" : String.valueOf(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tState.getFluidState().getType()))), tOk);
			if (tOk) tPass++; else tFail++;
		}
		if (gregapi.data.CS.BlocksGT.OilHeavy != null) {
			net.minecraft.world.level.block.state.BlockState tOil = gregapi.data.CS.BlocksGT.OilHeavy.defaultBlockState();
			boolean tOk = tOil.getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL;
			itemModelLine(tOut, "REGRESS: мировой рендер нефти не задет", "MODEL", String.valueOf(tOil.getRenderShape()), tOk);
			if (tOk) tPass++; else tFail++;
		}
		O.println(tOut.toString());
		O.println("[GT6-ITEMMODELPROBE] ИТОГ: PASS=" + tPass + " FAIL=" + tFail);
		O.println("========== [GT6-ITEMMODELPROBE] DONE ==========");
	}
	/** Спрайт, которым движок реально нарисует предмет: тот же путь, что у GUI (updateForTopItem → particle-материал слоя). */
	private static String itemModelSprite(net.minecraft.world.item.ItemStack aStack) {
		try {
			net.minecraft.client.renderer.item.ItemStackRenderState tState = new net.minecraft.client.renderer.item.ItemStackRenderState();
			Minecraft.getInstance().getItemModelResolver().updateForTopItem(tState, aStack, net.minecraft.world.item.ItemDisplayContext.GUI, null, null, 0);
			if (tState.isEmpty()) return null;
			net.minecraft.client.resources.model.sprite.Material.Baked tMat = tState.pickParticleMaterial(net.minecraft.util.RandomSource.create(42L));
			return tMat == null ? "<слой есть, particle нет>" : tMat.sprite().contents().name().toString();
		} catch (Throwable e) {return "EXC " + e;}
	}
	private static void itemModelLine(StringBuilder aOut, String aName, String aExpected, String aActual, boolean aPass) {
		aOut.append("[GT6-ITEMMODELPROBE] ").append(aName).append(" | ожидание: ").append(aExpected).append(" | факт: ").append(aActual).append(" | ").append(aPass ? "PASS" : "FAIL").append('\n');
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

	// ================= [GT6-JEICRAFT] BUG-079/BUG-073: ЖИВОЙ СУДЬЯ ВИТРИНЫ КРАФТА =================
	// Игрок 2026-07-28: «я не видел, чтобы ты запускал игру и реально проверял крафты… когда полностью
	// закончишь и подтвердишь ВСЕ крафты, тогда и сообщай». Судится не наш буфер и не наши данные, а САМА
	// витрина: спрашиваем живой JEI тем же лукапом, которым он отвечает игроку на «покажи крафты этого
	// предмета» — IRecipeManager.createRecipeLookup(type).limitFocus(focus(OUTPUT, стек)). Предметы берутся
	// из витрины креатива тем же центром, что наполняет её игре (CreativeTabsGT.enumerate).
	// ПОЗИТИВНЫЙ КОНТРОЛЬ встроен: ванильный верстак (RecipeTypes.CRAFTING) обязан отвечать на ванильные
	// предметы — если и он молчит, судья меряет собственную поломку, а не GT6.
	// ================= [GT6-RECIPEGUI] BUG-056: иконка «показать все рецепты машины» =================
	// Два слоя дефекта, мёртвые НЕЗАВИСИМО друг от друга (см. карточку BUG-056):
	//   (1) сама иконка не рисовалась: BI.nei() отдаёт текстуру только при CS.NEI, а взвести флаг мог лишь
	//       NEI-плагин 1.7.10 (NEI_GT_API_Config), который в 26.1.2 никем не инстанцируется;
	//   (2) клик не работал: RecipeMap.openNEI() звал codechicken.nei.recipe.GuiCraftingRecipe — класс есть
	//       только в src/compat-mirror, в проде это NoClassDefFoundError, молча съеденный catch(Throwable).
	// Судятся ФАКТЫ, не картинка (визуальных судей не строим): взведён ли флаг, СТРОИТСЯ ли текстура,
	// и открывается ли экран рецептов по тому же ключу mNameNEI, которым 1.7.10 звал NEI.
	// Список карт взят ИЗ КОДА — все 8 вызывателей openNEI(), а не «семь машин по памяти».
	// ПОЗИТИВНЫЙ КОНТРОЛЬ: несуществующее имя категории обязано дать false, иначе судья говорит «да» на всё.
	private static boolean mRecipeGuiDone = false;
	private static int mRecipeGuiWaited = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onRecipeGuiProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mRecipeGuiDone || !gregapi.data.CS.probeFlag("gt6recipegui.flag")) return;
		mezz.jei.api.runtime.IJeiRuntime tRT = gregapi.jei.GT6_JEI_Plugin.sRuntime;
		net.minecraft.client.Minecraft tMC = net.minecraft.client.Minecraft.getInstance();
		if (tRT == null || tMC == null || tMC.player == null) {
			if (++mRecipeGuiWaited == 100 && tMC != null && tMC.player != null) {
				try {tMC.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(tMC.player));} catch (Throwable e) {/**/}
			}
			if (mRecipeGuiWaited > 24000) {mRecipeGuiDone = true; gregapi.data.CS.OUT.println("[GT6-RECIPEGUI] runtime JEI не появился — судья НЕ отработал");}
			return;
		}
		mRecipeGuiDone = true;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-RECIPEGUI] BUG-056: иконка и клик «показать рецепты» ==========");
		int tPass = 0, tFail = 0;
		// СЛОЙ 1: флаг и сама текстура
		boolean tFlag = gregapi.data.CS.NEI;
		O.println("[GT6-RECIPEGUI] CS.NEI (гейт иконки) = " + tFlag + " => " + (tFlag ? "PASS" : "FAIL"));
		if (tFlag) tPass++; else tFail++;
		gregapi.render.ITexture tIcon = null;
		try {tIcon = gregapi.data.BI.nei();} catch (Throwable e) {O.println("[GT6-RECIPEGUI] BI.nei() бросил " + e);}
		O.println("[GT6-RECIPEGUI] BI.nei() строит текстуру = " + (tIcon != null) + " => " + (tIcon != null ? "PASS" : "FAIL"));
		if (tIcon != null) tPass++; else tFail++;
		// СЛОЙ 2: открытие экрана по ключу mNameNEI — все 8 вызывателей openNEI() из кода
		gregapi.recipes.Recipe.RecipeMap[] tMaps = {
			gregapi.data.RM.Anvil, gregapi.data.RM.Bath, gregapi.data.RM.Sharpening, gregapi.data.RM.Juicer,
			gregapi.data.RM.Mixer, gregapi.data.RM.Mortar, gregapi.data.RM.Sifting, gregapi.data.RM.BedrockOreList};
		String[] tNames = {"Наковальня", "Ванна", "Точило", "Соковыжималка", "Миска", "Ступка", "Стол просеивания", "Список руд коренной породы"};
		for (int i = 0; i < tMaps.length; i++) {
			boolean tOk = false;
			try {tOk = gregapi.jei.GT6_JEI_Plugin.showRecipeCategory(tMaps[i].mNameNEI);} catch (Throwable e) {O.println("[GT6-RECIPEGUI] " + tNames[i] + " бросил " + e);}
			O.println("[GT6-RECIPEGUI] открыть рецепты: " + tNames[i] + " (" + tMaps[i].mNameNEI + ") = " + tOk + " => " + (tOk ? "PASS" : "FAIL"));
			if (tOk) tPass++; else tFail++;
		}
		// путь целиком, как его зовёт сама машина (MultiTileEntityJuicer:164 mRecipes.openNEI())
		boolean tViaRecipeMap = false;
		try {tViaRecipeMap = gregapi.data.RM.Juicer.openNEI();} catch (Throwable e) {O.println("[GT6-RECIPEGUI] openNEI() бросил " + e);}
		O.println("[GT6-RECIPEGUI] RecipeMap.openNEI() (путь самой машины) = " + tViaRecipeMap + " => " + (tViaRecipeMap ? "PASS" : "FAIL"));
		if (tViaRecipeMap) tPass++; else tFail++;
		// негативный контроль
		boolean tBogus = true;
		try {tBogus = gregapi.jei.GT6_JEI_Plugin.showRecipeCategory("gt.recipe.заведомо.нет.такой");} catch (Throwable e) {/**/}
		O.println("[GT6-RECIPEGUI] НЕГАТИВНЫЙ КОНТРОЛЬ (несуществующая категория) = " + tBogus + " => " + (!tBogus ? "PASS" : "FAIL"));
		if (!tBogus) tPass++; else tFail++;
		O.println("========== [GT6-RECIPEGUI] DONE (pass=" + tPass + " fail=" + tFail + ") ==========");
	}

	// ================= [GT6-RECIPEGUI-B] BUG-056 ЧАСТЬ Б: кнопка в GUI интерфейсной машины =================
	// В 1.7.10 игрок открывал машину и одним кликом получал ВЕСЬ список её рецептов — кнопку рисовал мод NEI
	// поверх любого GuiContainer, GT6 лишь отдавал ему имя категории (ContainerClient.mNEI). В 26.1.2 JEI
	// кнопку в рамке GUI НЕ рисует, то есть функция была утрачена; теперь её выполняет сам мод
	// (ContainerClient.addRecipeButton). Судится РЕЗУЛЬТАТ для игрока, а не картинка: открыт ли экран машины,
	// ЕСТЬ ли на нём кнопка, и ПЕРЕКЛЮЧАЕТ ли нажатие на экран рецептов JEI.
	// Серверная половина (GT6Probes.gt6RecipeGuiServerTick) ставит машину и жмёт по ней ПКМ.
	private static boolean mRecipeGuiBDone = false;
	private static int mRecipeGuiBWaited = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onRecipeGuiBProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mRecipeGuiBDone || !gregapi.data.CS.probeFlag("gt6recipegui.flag")) return;
		net.minecraft.client.Minecraft tMC = net.minecraft.client.Minecraft.getInstance();
		if (tMC == null) return;
		// ждём, пока СЕРВЕР откроет игроку GUI машины (ПКМ на 40-м серверном тике)
		if (!(tMC.screen instanceof gregapi.gui.ContainerClient tScreen)) {
			if (++mRecipeGuiBWaited > 24000) {mRecipeGuiBDone = true; gregapi.data.CS.OUT.println("[GT6-RECIPEGUI-B] GUI машины так и не открылся — судья НЕ отработал");}
			return;
		}
		mRecipeGuiBDone = true;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		int tPass = 0, tFail = 0;
		O.println("========== [GT6-RECIPEGUI-B] кнопка «показать рецепты» в GUI машины ==========");
		O.println("[GT6-RECIPEGUI-B] открыт экран: " + tScreen.getClass().getSimpleName() + " · mNEI='" + tScreen.mNEI + "'");
		boolean tHasName = tScreen.mNEI != null && !tScreen.mNEI.isEmpty();
		O.println("[GT6-RECIPEGUI-B] у экрана есть имя категории => " + (tHasName ? "PASS" : "FAIL"));
		if (tHasName) tPass++; else tFail++;
		// РЕАЛЬНЫЙ ЖЕСТ ИГРОКА 1.7.10: клик мышью по СТРЕЛКЕ ПРОГРЕССА (её область — leftPos+78, topPos+24,
		// поле 20×18, ровно куда её рисует ContainerClientBasicMachine.drawGuiContainerBackgroundLayer2).
		// 26.1.2: mouseClicked(MouseButtonEvent, boolean) — событие несёт координаты и кнопку (0 = ЛКМ).
		// зона по ОРИГИНАЛУ (NEI_RecipeMap:70 — Rectangle(70,24,36,18) в координатах GUI); бьём по ЛЕВОМУ
		// краю зоны (+2 px), а не по центру: центр попадал бы и в мою прежнюю, УЖЕ, зону — такой судья
		// не отличил бы верную ширину от заниженной
		double tClickX = tScreen.getLeft() + 70 + 2, tClickY = tScreen.getTop() + 24 + 9;
		O.println("[GT6-RECIPEGUI-B] кликаю по ЛЕВОМУ КРАЮ зоны рецептов @(" + (int)tClickX + "," + (int)tClickY + ") — оригинал NEI_RecipeMap:70 (70,24,36×18)");
		try {
			tScreen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(tClickX, tClickY,
				new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
		} catch (Throwable e) {O.println("[GT6-RECIPEGUI-B] клик по прогрессу упал: " + e);}
		net.minecraft.client.gui.screens.Screen tAfter = tMC.screen;
		boolean tSwitched = tAfter != null && !(tAfter instanceof gregapi.gui.ContainerClient);
		O.println("[GT6-RECIPEGUI-B] после клика по прогрессу экран = " + (tAfter == null ? "null" : tAfter.getClass().getSimpleName())
			+ " (ушли из GUI машины = " + tSwitched + ") => " + (tSwitched ? "PASS" : "FAIL"));
		if (tSwitched) tPass++; else tFail++;
		O.println("========== [GT6-RECIPEGUI-B] DONE (pass=" + tPass + " fail=" + tFail + ") ==========");
	}

	// ================= [GT6-JUICEJEI] BUG-055 хвост: видит ли ЖИВОЙ JEI категории Соковыжималки и Пресса =================
	// Спрашиваем не код регистрации, а саму витрину: пересоздаём тот же RecipeType, что строит
	// GT6_JEI_Plugin:124 (RecipeType.create(MD.GT.mID, map.mNameNEI, Recipe.class)), и просим у JEI список рецептов.
	// ПОЗИТИВНЫЙ КОНТРОЛЬ: ванильная категория CRAFTING обязана быть непустой — иначе лукап сломан и замер недействителен.
	private static boolean mJuiceJeiDone = false;
	private static int mJuiceJeiWaited = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onJuiceJeiProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mJuiceJeiDone || !gregapi.data.CS.probeFlag("gt6juiceprobe.flag")) return;
		mezz.jei.api.runtime.IJeiRuntime tRT = gregapi.jei.GT6_JEI_Plugin.sRuntime;
		net.minecraft.client.Minecraft tMC = net.minecraft.client.Minecraft.getInstance();
		if (tRT == null || tMC == null || tMC.player == null) {
			// JEI поднимается лениво — его стартовое событие даёт открытие GUI (урок GT6-JEICRAFT)
			if (++mJuiceJeiWaited == 100 && tMC != null && tMC.player != null) {
				try {tMC.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(tMC.player));} catch (Throwable e) {/**/}
			}
			if (mJuiceJeiWaited > 24000) {mJuiceJeiDone = true; gregapi.data.CS.OUT.println("[GT6-JUICEJEI] runtime JEI не появился — судья НЕ отработал");}
			return;
		}
		mJuiceJeiDone = true;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-JUICEJEI] живой опрос витрины JEI: Соковыжималка и Пресс ==========");
		try {
			var tRM = tRT.getRecipeManager();
			int tCtrl = 0;
			try {tCtrl = (int) tRM.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).get().count();} catch (Throwable e) {O.println("[GT6-JUICEJEI] контроль упал: " + e);}
			O.println("[GT6-JUICEJEI] ПОЗИТИВНЫЙ КОНТРОЛЬ: ванильных CRAFTING-рецептов в витрине = " + tCtrl
				+ (tCtrl > 0 ? " — лукап рабочий" : " — ⛔ ЛУКАП СЛОМАН, замер ниже недействителен"));
			gt6JuiceJeiAsk(O, tRM, gregapi.data.RM.Juicer  , "Соковыжималка");
			gt6JuiceJeiAsk(O, tRM, gregapi.data.RM.Squeezer, "Пресс");
		} catch (Throwable e) {
			O.println("[GT6-JUICEJEI] исключение: " + e);
			e.printStackTrace(gregapi.data.CS.OUT);
		}
		O.println("========== [GT6-JUICEJEI] DONE ==========");
	}

	private static void gt6JuiceJeiAsk(java.io.PrintStream aOut, mezz.jei.api.recipe.IRecipeManager aRM, gregapi.recipes.Recipe.RecipeMap aMap, String aLabel) {
		try {
			mezz.jei.api.recipe.RecipeType<gregapi.recipes.Recipe> tType =
				mezz.jei.api.recipe.RecipeType.create(gregapi.data.MD.GT.mID, aMap.mNameNEI, gregapi.recipes.Recipe.class);
			java.util.List<gregapi.recipes.Recipe> tList = aRM.createRecipeLookup(tType).get().toList();
			int tFlower = 0;
			for (gregapi.recipes.Recipe r : tList) {
				if (r == null || r.mFluidOutputs == null) continue;
				for (net.neoforged.neoforge.fluids.FluidStack f : r.mFluidOutputs)
					if (f != null && f.getFluid() != null && gregapi.fluid.FluidGT.nameOf(f.getFluid()).startsWith("dye.flower.")) {tFlower++; break;}
			}
			aOut.println("[GT6-JUICEJEI] " + aLabel + " (" + aMap.mNameNEI + "): витрина отдаёт " + tList.size()
				+ " рецептов, из них цветочных " + tFlower + " => " + (tList.size() > 0 && tFlower > 0 ? "PASS" : "FAIL"));
		} catch (Throwable e) {
			aOut.println("[GT6-JUICEJEI] " + aLabel + ": категории НЕТ в витрине (" + e + ") => FAIL");
		}
	}

	private static boolean mJeiCraftDone = false;
	private static int mJeiCraftWaited = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onJeiCraftProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mJeiCraftDone || !gregapi.data.CS.probeFlag("gt6jeicraft.flag")) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		mezz.jei.api.runtime.IJeiRuntime tRT = gregapi.jei.GT6_JEI_Plugin.sRuntime;
		if (tRT == null) {
			// JEI грузит плагины ЛЕНИВО, по событию открытия GUI (mezz.jei.neoforge.startup.StartEventObserver):
			// одного входа в мир мало — в логе висел только «Sending ConfigManager». Открываем инвентарь один раз,
			// это ровно то действие игрока, после которого витрина оживает.
			if (++mJeiCraftWaited == 100) {
				gregapi.data.CS.OUT.println("[GT6-JEICRAFT] JEI ещё не поднялся — открываю инвентарь (его стартовое событие)");
				try {tMC.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(tMC.player));} catch (Throwable e) {gregapi.data.CS.OUT.println("[GT6-JEICRAFT] экран не открылся: " + e);}
			}
			if (mJeiCraftWaited % 400 == 0) gregapi.data.CS.OUT.println("[GT6-JEICRAFT] жду runtime JEI... тиков " + mJeiCraftWaited);
			if (mJeiCraftWaited > 24000) {mJeiCraftDone = true; gregapi.data.CS.OUT.println("[GT6-JEICRAFT] runtime JEI не появился — судья НЕ отработал");}
			return;
		}
		mJeiCraftDone = true;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-JEICRAFT] живой опрос витрины JEI ==========");
		try {
			var tRM = tRT.getRecipeManager();
			var tFF = tRT.getJeiHelpers().getFocusFactory();

			// позитивный контроль ДО основного замера: умеет ли лукап вообще находить рецепты
			int tCtrl = countRecipes(tRM, tFF, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
			int tCtrl2 = countRecipes(tRM, tFF, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST));
			O.println("[GT6-JEICRAFT] ПОЗИТИВНЫЙ КОНТРОЛЬ: палка=" + tCtrl + " рецептов, сундук=" + tCtrl2
				+ (tCtrl > 0 && tCtrl2 > 0 ? " — лукап рабочий" : " — ⛔ ЛУКАП СЛОМАН, замер ниже недействителен"));

			// ОЖИДАНИЕ по буферу: какие подтипы GT6 вообще обещаны как выход крафта. Расхождение «буфер обещает —
			// витрина не показывает» и есть остаточный дефект видимости; без этой сверки «без крафта» неотличимо
			// от «не крафтится by design» (руда, цветок, жидкостный дисплей).
			java.util.Set<String> tPromised = new java.util.HashSet<>();
			for (gregapi.recipes.ICraftingRecipeGT r : gregapi.util.CR.list()) {
				if (!(r instanceof gregapi.recipes.ShapedOreRecipe || r instanceof gregapi.recipes.ShapelessOreRecipe)) continue;
				try {
					net.minecraft.world.item.ItemStack o = r.getRecipeOutput();
					if (o != null && o.getItem() != null && !o.isEmpty()) tPromised.add(gregapi.util.ST.identityKey(o));
				} catch (Throwable t) {}
			}
			int tPromisedMissing = 0;
			java.util.List<String> tPromisedExamples = new java.util.ArrayList<>();
			java.util.Map<String, int[]> tPromisedFamily = new java.util.TreeMap<>();

			int tTotal = 0, tWithout = 0, tTools = 0, tToolsWithout = 0;
			java.util.Map<String, int[]> tByFamily = new java.util.TreeMap<>();
			java.util.List<String> tMissing = new java.util.ArrayList<>();
			for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
				var tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
				if (tKey == null) continue;
				String tNs = tKey.getNamespace();
				if (!tNs.equals("gregtech") && !tNs.equals("gregapi")) continue;
				boolean tIsTool = tItem instanceof gregapi.item.multiitem.MultiItemTool;
				for (net.minecraft.world.item.ItemStack tStack : gregapi.item.CreativeTabsGT.enumerate(tItem, tItem, null)) {
					if (tStack == null || tStack.isEmpty() || gregapi.util.ST.hidden(tStack)) continue;
					tTotal++;
					if (tIsTool) tTools++;
					int tN = countRecipes(tRM, tFF, tStack);
					if (tN == 0) {
						tWithout++;
						if (tIsTool) tToolsWithout++;
						int[] tRow = tByFamily.computeIfAbsent(tKey.toString(), k -> new int[1]);
						tRow[0]++;
						if (tIsTool && tMissing.size() < 12) tMissing.add(tStack.getHoverName().getString() + " (" + tKey + ":" + gregapi.util.ST.meta_(tStack) + ")");
						if (tPromised.contains(gregapi.util.ST.identityKey(tStack))) {   // буфер обещал рецепт, а витрина молчит
							tPromisedMissing++;
							tPromisedFamily.computeIfAbsent(tKey.toString(), k -> new int[1])[0]++;
							if (tPromisedExamples.size() < 10) tPromisedExamples.add(tKey + ":" + gregapi.util.ST.meta_(tStack) + " «" + tStack.getHoverName().getString() + "»");
						}
					}
				}
			}
			O.println("[GT6-JEICRAFT] предметов витрины GT опрошено: " + tTotal + " · БЕЗ единого крафта в JEI: " + tWithout);
			O.println("[GT6-JEICRAFT] ИНСТРУМЕНТЫ: " + tTools + " · без крафта: " + tToolsWithout
				+ (tToolsWithout == 0 ? "  ← ВСЕ ИНСТРУМЕНТЫ ПОКАЗЫВАЮТ КРАФТ" : "  ← ОСТАЛИСЬ БЕЗ КРАФТА"));
			for (String s : tMissing) O.println("[GT6-JEICRAFT]   инструмент без крафта: " + s);
			O.println("[GT6-JEICRAFT] ⭐ БУФЕР ОБЕЩАЛ, ВИТРИНА НЕ ПОКАЗАЛА: " + tPromisedMissing
				+ (tPromisedMissing == 0 ? "  ← видимость полная" : "  ← остаточный дефект видимости"));
			for (var e : tPromisedFamily.entrySet()) O.println("[GT6-JEICRAFT]   [обещано-нет] семья " + e.getKey() + ": " + e.getValue()[0]);
			for (String s : tPromisedExamples) O.println("[GT6-JEICRAFT]   [обещано-нет] пример: " + s);
			for (var e : tByFamily.entrySet()) O.println("[GT6-JEICRAFT]   семья " + e.getKey() + ": без крафта " + e.getValue()[0]);
		} catch (Throwable e) {O.println("[GT6-JEICRAFT] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR);}
		O.println("========== [GT6-JEICRAFT] DONE ==========");
	}
	// [GT6-ITEMFACINGPROBE] BUG-078 «остаток централизации»: судья ИДЕНТИЧНОСТИ item-facing — снять при уборке фазы.
	// Гейт §2.1 (-Pgt6probes + run/gt6itemfacingprobe.flag). Судится НЕ картинка (визуальных судей не строим), а ФАКТ:
	// какая грань реально стоит в detached-TE, рождённом ТЕМ ЖЕ вызовом, которым его рождает движок. Путей рождения два,
    // и проба идёт обоими: обычный item-рендер (MultiTileEntityBlockInternal.passRenderingToObject) и BER-ветка для
	// предметов со своим рендерером (MultiTileEntityBER.SPECIAL_ITEM_FORM.extractArgument — сундук, масстораж).
	// Ожидание для каждого MTE: mFacing == его же getItemFacing(). Плюс ПОЛНОТА класса: носитель поля mFacing,
	// не реализующий контракт IMTE_ItemFacing, — дыра (центр его не увидит), таких должно быть 0.
	private static boolean mItemFacingDone = false;
	@net.neoforged.bus.api.SubscribeEvent
	public static void onItemFacingProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mItemFacingDone || !gregapi.data.CS.probeFlag("gt6itemfacingprobe.flag")) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		mItemFacingDone = true;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-ITEMFACINGPROBE] BUG-078: грань item-формы подставляет ЕДИНЫЙ центр ==========");
		int tPass = 0, tFail = 0, tSeen = 0, tNoField = 0;
		java.util.Map<String, int[]> tByClass = new java.util.TreeMap<>();
		java.util.List<String> tFails = new java.util.ArrayList<>(), tGaps = new java.util.ArrayList<>();
		try {
			// Реестры MTE лежат в приватной статике — стенду нужен ПОЛНЫЙ обход, а не выборка «по памяти».
			java.lang.reflect.Field tField = gregapi.block.multitileentity.MultiTileEntityRegistry.class.getDeclaredField("NAMED_REGISTRIES");
			tField.setAccessible(true);
			@SuppressWarnings("unchecked")
			java.util.Map<String, gregapi.block.multitileentity.MultiTileEntityRegistry> tRegistries =
				(java.util.Map<String, gregapi.block.multitileentity.MultiTileEntityRegistry>)tField.get(null);
			O.println("[GT6-ITEMFACINGPROBE] реестров MTE: " + tRegistries.size());
			for (gregapi.block.multitileentity.MultiTileEntityRegistry tRegistry : tRegistries.values()) {
				for (Short tID : new java.util.TreeSet<>(tRegistry.mRegistry.keySet())) {
					net.minecraft.world.item.ItemStack tStack = tRegistry.getItem(tID);
					if (tStack == null || tStack.isEmpty()) continue;
					// ПУТЬ A — обычный item-рендер; ПУТЬ B — BER-ветка. Какой из них живой, решает сам MTE.
					Object tRendered = tRegistry.mBlock.passRenderingToObject(tStack);
					net.minecraft.world.level.block.entity.BlockEntity tTE =
						tRendered instanceof net.minecraft.world.level.block.entity.BlockEntity tBE ? tBE
						: gregapi.render.MultiTileEntityBER.SPECIAL_ITEM_FORM.extractArgument(tStack);
					if (tTE == null) continue;
					Byte tActual = itemFacingOf(tTE);
					if (tActual == null) {tNoField++; continue;}   // грани нет вовсе — подставлять нечего
					tSeen++;
					String tClass = tTE.getClass().getSimpleName();
					int[] tRow = tByClass.computeIfAbsent(tClass, k -> new int[2]);
					if (tTE instanceof gregapi.block.multitileentity.IMultiTileEntity.IMTE_ItemFacing tFacing) {
						boolean tOk = tActual.byteValue() == tFacing.getItemFacing();
						if (tOk) {tPass++; tRow[0]++;} else {
							tFail++; tRow[1]++;
							if (tFails.size() < 12) tFails.add(tClass + " id=" + tID + " ожидание=" + tFacing.getItemFacing() + " факт=" + tActual);
						}
					} else {
						// Дыра полноты: у TE есть грань, но центр его не видит — ровно этим и болел сундук до BUG-078.
						tFail++; tRow[1]++;
						if (tGaps.size() < 12) tGaps.add(tClass + " id=" + tID + " (поле mFacing есть, контракта IMTE_ItemFacing НЕТ)");
					}
				}
			}
			// SENSITIVITY: судья обязан УМЕТЬ дать FAIL. Берём тот же сундук, но рождаем его БЕЗ центра — грань обязана
			// остаться дефолтной (3) и разойтись с getItemFacing(). Без этой строки «всё PASS» ничего не доказывает.
			gregapi.block.multitileentity.MultiTileEntityRegistry tChestReg = null; short tChestID = 0;
			for (gregapi.block.multitileentity.MultiTileEntityRegistry tRegistry : tRegistries.values()) {
				for (Short tID : new java.util.TreeSet<>(tRegistry.mRegistry.keySet())) {
					net.minecraft.world.level.block.entity.BlockEntity tTE = tRegistry.getNewTileEntity(tRegistry.getItem(tID));
					if (tTE instanceof gregapi.block.multitileentity.example.MultiTileEntityChest) {tChestReg = tRegistry; tChestID = tID; break;}
				}
				if (tChestReg != null) break;
			}
			if (tChestReg == null) {
				O.println("[GT6-ITEMFACINGPROBE] SENSITIVITY: сундук в реестрах не найден — контроль НЕ выполнен | FAIL");
				tFail++;
			} else {
				net.minecraft.world.level.block.entity.BlockEntity tRaw = tChestReg.getNewTileEntity(tChestReg.getItem(tChestID));
				Byte tRawFacing = itemFacingOf(tRaw);
				byte tWant = ((gregapi.block.multitileentity.IMultiTileEntity.IMTE_ItemFacing)tRaw).getItemFacing();
				boolean tSensOk = tRawFacing != null && tRawFacing.byteValue() != tWant;
				O.println("[GT6-ITEMFACINGPROBE] SENSITIVITY: сундук БЕЗ центра | ожидание: грань дефолтная (≠" + tWant + ") | факт: " + tRawFacing + " | " + (tSensOk ? "PASS" : "FAIL"));
				if (tSensOk) tPass++; else tFail++;
				// ПОЗИТИВНЫЙ КОНТРОЛЬ: тот же сундук, но реальным путём движка — обязан прийти к своей величине.
				net.minecraft.world.level.block.entity.BlockEntity tLive = gregapi.render.MultiTileEntityBER.SPECIAL_ITEM_FORM.extractArgument(tChestReg.getItem(tChestID));
				Byte tLiveFacing = tLive == null ? null : itemFacingOf(tLive);
				boolean tLiveOk = tLiveFacing != null && tLiveFacing.byteValue() == gregapi.data.CS.ITEM_CHEST_FACING;
				O.println("[GT6-ITEMFACINGPROBE] ЦЕЛЬ BUG-078: сундук путём движка (BER) | ожидание: " + gregapi.data.CS.ITEM_CHEST_FACING + " | факт: " + tLiveFacing + " | " + (tLiveOk ? "PASS" : "FAIL"));
				if (tLiveOk) tPass++; else tFail++;
			}
		} catch (Throwable e) {O.println("[GT6-ITEMFACINGPROBE] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); tFail++;}
		for (String s : tGaps ) O.println("[GT6-ITEMFACINGPROBE]   ⛔ ДЫРА ПОЛНОТЫ: " + s);
		for (String s : tFails) O.println("[GT6-ITEMFACINGPROBE]   расхождение: " + s);
		for (java.util.Map.Entry<String, int[]> e : tByClass.entrySet())
			O.println("[GT6-ITEMFACINGPROBE]   " + e.getKey() + ": PASS=" + e.getValue()[0] + " FAIL=" + e.getValue()[1]);
		O.println("[GT6-ITEMFACINGPROBE] detached-TE с гранью опрошено: " + tSeen + " · без поля грани пропущено: " + tNoField);
		O.println("[GT6-ITEMFACINGPROBE] ИТОГ: PASS=" + tPass + " FAIL=" + tFail);
		O.println("========== [GT6-ITEMFACINGPROBE] DONE ==========");
	}
	/** Фактическая грань TE: поле у семей своё (база — public, сундук — protected), потому берётся по цепочке классов. */
	private static Byte itemFacingOf(Object aTileEntity) {
		for (Class<?> tClass = aTileEntity.getClass(); tClass != null; tClass = tClass.getSuperclass()) try {
			java.lang.reflect.Field tField = tClass.getDeclaredField("mFacing");
			tField.setAccessible(true);
			return Byte.valueOf(tField.getByte(aTileEntity));
		} catch (NoSuchFieldException e) {/* ищем выше по цепочке */} catch (Throwable e) {return null;}
		return null;
	}

	/** Сколько рецептов витрина отдаст на «покажи крафты» этого предмета: наша GT6-категория + ванильный верстак. */
	private static int countRecipes(mezz.jei.api.recipe.IRecipeManager aRM, mezz.jei.api.recipe.IFocusFactory aFF, net.minecraft.world.item.ItemStack aStack) {
		int r = 0;
		try {
			var tFocus = java.util.List.of(aFF.createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, aStack));
			r += (int)aRM.createRecipeLookup(gregapi.jei.GT6_JEI_CraftingCategory.TYPE).limitFocus(tFocus).get().count();
			r += (int)aRM.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).limitFocus(tFocus).get().count();
		} catch (Throwable e) {/* один предмет не должен ронять весь замер */}
		return r;
	}
}
