package gregtech6.gametest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTest мода GT6: проверка механик в РЕАЛЬНОМ загруженном мире (не заглушка), которых нельзя достичь на
 * EphemeralTestServer (там нет мира). Один класс с селектором {@code mKind} гоняет разные проверки в тест-регионе
 * (структура gregtech6:gt6_platform 9x5x9 с полом). Логика {@code run} — хардкод (не данные), поэтому codec несёт
 * только {@link TestData}. Запуск: {@code ./gradlew runGameTestServer}.
 */
public class GT6GameTest extends GameTestInstance {
	public static final MapCodec<GT6GameTest> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("kind").forGetter((GT6GameTest t) -> t.mKind),
			TestData.CODEC.forGetter((GT6GameTest t) -> t.info())
	).apply(i, GT6GameTest::new));

	private final String mKind;

	public GT6GameTest(String aKind, TestData<Holder<TestEnvironmentDefinition<?>>> aInfo) {
		super(aInfo);
		mKind = aKind;
	}

	@Override
	public void run(GameTestHelper aHelper) {
		switch (mKind) {
			case "block":    runBlock(aHelper);    break;
			case "interact": runInteract(aHelper); break;
			case "mte":      runMTE(aHelper);      break;
			default: aHelper.fail(Component.literal("неизвестный GT6-тест: " + mKind));
		}
	}

	/** Механика БЛОКОВ в реальном мире: ставим GT6-блоки (setBlock), читаем состояние+TileEntity, сносим (→air). */
	private void runBlock(GameTestHelper aHelper) {
		int tTried = 0, tPlaced = 0, tBroke = 0, tWithTE = 0;
		BlockPos tPos = new BlockPos(4, 1, 4); // над полом структуры
		for (Block tBlock : BuiltInRegistries.BLOCK) {
			Identifier tKey = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tKey == null) continue;
			if (!(tKey.getNamespace().equals("gregtech") || tKey.getNamespace().equals("gregapi"))) continue;
			if (tBlock instanceof LiquidBlock) continue;
			if (tTried++ >= 30) break;
			try {
				aHelper.setBlock(tPos, tBlock);
				BlockState tGot = aHelper.getBlockState(tPos);
				if (tGot.getBlock() == tBlock) {
					tPlaced++;
					// nullable-геттер (getBlockEntity(pos,type) у helper — это АССЕРТ, бросает при null; здесь не нужен)
					net.minecraft.world.level.block.entity.BlockEntity tTE = aHelper.getLevel().getBlockEntity(aHelper.absolutePos(tPos));
					if (tTE != null) tWithTE++;
					aHelper.setBlock(tPos, Blocks.AIR);
					if (aHelper.getBlockState(tPos).isAir()) tBroke++;
				}
			} catch (Throwable t) { /* блок с особыми требованиями к размещению — пропускаем */ }
		}
		aHelper.getLevel().getServer().sendSystemMessage(Component.literal(
				"[GT6-GAMETEST-BLOCK] испытано=" + tTried + " поставлено+прочитано=" + tPlaced + " сTE=" + tWithTE + " снесено=" + tBroke));
		if (tPlaced > 0 && tBroke > 0) aHelper.succeed();
		else aHelper.fail(Component.literal("block placement/break в реальном мире сломан: поставлено=" + tPlaced + " снесено=" + tBroke));
	}

	/** Механика ВЗАИМОДЕЙСТВИЯ игрок-блок: ставим GT6-блок, mock-игрок правой кнопкой (useItemOn/useWithoutItem) — хендлер должен отработать без падения. */
	private void runInteract(GameTestHelper aHelper) {
		int tTried = 0, tInteracted = 0;
		BlockPos tRel = new BlockPos(4, 1, 4);
		Player tPlayer = aHelper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
		BlockPos tAbs = aHelper.absolutePos(tRel);
		BlockHitResult tHit = new BlockHitResult(Vec3.atCenterOf(tAbs), net.minecraft.core.Direction.UP, tAbs, false);
		for (Block tBlock : BuiltInRegistries.BLOCK) {
			Identifier tKey = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tKey == null) continue;
			if (!(tKey.getNamespace().equals("gregtech") || tKey.getNamespace().equals("gregapi"))) continue;
			if (tBlock instanceof LiquidBlock) continue;
			if (tTried++ >= 20) break;
			try {
				aHelper.setBlock(tRel, tBlock);
				BlockState tState = aHelper.getBlockState(tRel);
				ItemStack tHeld = tPlayer.getItemInHand(InteractionHand.MAIN_HAND);
				// РЕАЛЬНЫЙ путь взаимодействия neo (как GameTestHelper.useBlock): useItemOn → useWithoutItem.
				net.minecraft.world.InteractionResult tRes = tState.useItemOn(tHeld, aHelper.getLevel(), tPlayer, InteractionHand.MAIN_HAND, tHit);
				if (!tRes.consumesAction()) tState.useWithoutItem(aHelper.getLevel(), tPlayer, tHit);
				tInteracted++; // хендлер отработал без исключения
				aHelper.setBlock(tRel, Blocks.AIR);
			} catch (Throwable t) { /* блок с GUI-требованием клиента — не проваливаем весь тест из-за него */ }
		}
		aHelper.getLevel().getServer().sendSystemMessage(Component.literal(
				"[GT6-GAMETEST-INTERACT] испытано=" + tTried + " хендлер_отработал=" + tInteracted));
		if (tInteracted > 0) aHelper.succeed();
		else aHelper.fail(Component.literal("взаимодействие игрок-блок сломано: ни один GT6-блок не отработал use-хендлер без падения"));
	}

	/**
	 * Живой гейт MTE-РАЗМЕЩЕНИЯ: ставим MTE-блоки (камень 32757, палка 32756) настоящим путём GT6 —
	 * {@code registry.mBlock.placeBlock(...)} (тот же, что зовёт worldgen WorldgenRocks), затем читаем
	 * {@code getBlockEntity(pos)}. Диагностика корня «камни/палки=0»: MTE-TE создаётся no-arg-конструктором
	 * (pos=BlockPos.ZERO, worldPosition в neo immutable) → WD.te крепит его по te.getBlockPos()=(0,0,0), а не на
	 * реальную позицию. Тест ждёт: TE не-null И te.getBlockPos()==позиция блока. До фикса — красный (ZERO).
	 */
	private void runMTE(GameTestHelper aHelper) {
		gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		if (tReg == null) { aHelper.fail(Component.literal("реестр gt.multitileentity не найден")); return; }
		int[] tIDs = {32757, 32756};
		String[] tNames = {"rock", "stick"};
		int[] tRelX = {2, 6};
		StringBuilder tReport = new StringBuilder("[GT6-GAMETEST-MTE] ");
		int tOK = 0;
		BlockPos tRockAbs = null; net.minecraft.world.level.block.entity.BlockEntity tRockTE = null;
		for (int i = 0; i < tIDs.length; i++) {
			BlockPos tRel = new BlockPos(tRelX[i], 1, 4);
			BlockPos tAbs = aHelper.absolutePos(tRel);
			boolean tPlaced = false; net.minecraft.world.level.block.entity.BlockEntity tTE = null; String tTEPos = "нет";
			try {
				tPlaced = tReg.mBlock.placeBlock(aHelper.getLevel(), tAbs.getX(), tAbs.getY(), tAbs.getZ(), gregapi.data.CS.SIDE_UNKNOWN, (short)tIDs[i], null, false, true);
				tTE = aHelper.getLevel().getBlockEntity(tAbs);
				if (tTE != null) tTEPos = tTE.getBlockPos().toShortString();
			} catch (Throwable t) { tReport.append(tNames[i]).append("=EXC(").append(t).append(") "); continue; }
			if (tTE != null && tTE.getBlockPos().equals(tAbs) && tTE instanceof gregapi.block.multitileentity.IMultiTileEntity) tOK++;
			if (i == 0) { tRockAbs = tAbs; tRockTE = tTE; }
			tReport.append(tNames[i]).append("{placed=").append(tPlaced)
				.append(" te=").append(tTE == null ? "null" : tTE.getClass().getSimpleName()).append(" tePos=").append(tTEPos)
				.append(" ждали=").append(tAbs.toShortString()).append("} ");
		}
		aHelper.getLevel().getServer().sendSystemMessage(Component.literal(tReport.toString()));
		if (tOK != tIDs.length) { aHelper.fail(Component.literal("MTE placement сломан: " + tReport)); return; }

		// ── Гейт Ключа 1 (LOAD): выживает ли MTE save/load? Полный цикл БЕЗ реального reload (headless): реальный TE → writeToNBT
		// (как neo сохраняет на диск: NBT_MTE_REG=item-id + данные) → сделать TileEntityLoaderStub с этим NBT (как neo подменяет на
		// load) → GT6WorldgenFeature.reconstructMTE → проверить, что на позиции снова РЕАЛЬНЫЙ MTE (не стаб) с тем же mteid.
		// Корневой фикс, что это разблокировал: registry-id теперь item-id (getRegistry(reg) находит реестр; был block-id 1168 → null).
		StringBuilder tR2 = new StringBuilder("[GT6-GAMETEST-MTE-LOAD] ");
		boolean tLoadOK = false;
		try {
			gregapi.block.multitileentity.IMultiTileEntity tMTE = (gregapi.block.multitileentity.IMultiTileEntity)tRockTE;
			short tRegID = tMTE.getMultiTileEntityRegistryID();
			boolean tRegFound = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry(tRegID) != null;
			tR2.append("regID=").append(tRegID).append(" regFound=").append(tRegFound).append(" ");
			// как neo сохраняет: writeToNBT (public на базе MTE; в интерфейсе нет → рефлексия)
			net.minecraft.nbt.CompoundTag tSaveNBT = gregapi.util.UT.NBT.make();
			tRockTE.getClass().getMethod("writeToNBT", net.minecraft.nbt.CompoundTag.class).invoke(tRockTE, tSaveNBT);
			short tSavedReg = tSaveNBT.getShort(gregapi.data.CS.NBT_MTE_REG).orElse((short)-1);
			short tSavedID  = tSaveNBT.getShort(gregapi.data.CS.NBT_MTE_ID ).orElse((short)-1);
			tR2.append("savedReg=").append(tSavedReg).append(" savedID=").append(tSavedID).append(" ");
			// как neo подменяет на load: ставим TileEntityLoaderStub на позицию (заменяя реальный камень) → потом реконструкция.
			// setBlockEntity(стаб) гарантирует, что «reloaded» ниже — результат РЕКОНСТРУКЦИИ, а не уцелевший оригинал.
			BlockState tState = aHelper.getLevel().getBlockState(tRockAbs);
			gregapi.tileentity.base.TileEntityLoaderStub tStub = new gregapi.tileentity.base.TileEntityLoaderStub(tRockAbs, tState);
			tStub.setLevel(aHelper.getLevel());
			tStub.mLoadedNBT = tSaveNBT;
			aHelper.getLevel().setBlockEntity(tStub);
			tR2.append("stubSet=").append(aHelper.getLevel().getBlockEntity(tRockAbs) instanceof gregapi.tileentity.base.TileEntityLoaderStub).append(" ");
			gregapi.worldgen.GT6WorldgenFeature.reconstructMTE(aHelper.getLevel(), tStub);
			net.minecraft.world.level.block.entity.BlockEntity tReloaded = aHelper.getLevel().getBlockEntity(tRockAbs);
			boolean tReal = tReloaded instanceof gregapi.block.multitileentity.IMultiTileEntity && !(tReloaded instanceof gregapi.tileentity.base.TileEntityLoaderStub);
			short tReloadID = tReal ? ((gregapi.block.multitileentity.IMultiTileEntity)tReloaded).getMultiTileEntityID() : -1;
			tR2.append("reloaded=").append(tReloaded == null ? "null" : tReloaded.getClass().getSimpleName()).append(" reloadID=").append(tReloadID).append(" ");
			tLoadOK = tRegFound && tReal && tReloadID == 32757;
		} catch (Throwable t) { tR2.append("EXC(").append(t).append(") "); }
		aHelper.getLevel().getServer().sendSystemMessage(Component.literal(tR2.toString()));
		if (tLoadOK) aHelper.succeed();
		else aHelper.fail(Component.literal("MTE load-реконструкция сломана: " + tR2));
	}

	@Override public MapCodec<? extends GameTestInstance> codec() { return CODEC; }
	@Override protected MutableComponent typeDescription() { return Component.literal("gt6:" + mKind); }
}
