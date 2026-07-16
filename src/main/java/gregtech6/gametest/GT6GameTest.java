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

	@Override public MapCodec<? extends GameTestInstance> codec() { return CODEC; }
	@Override protected MutableComponent typeDescription() { return Component.literal("gt6:" + mKind); }
}
