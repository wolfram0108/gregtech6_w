package net.minecraft.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * F-flowerpot compile-only shim. 1.7.10 {@code net.minecraft.tileentity.TileEntityFlowerPot} удалён в neo
 * целиком — пакет {@code net.minecraft.tileentity} отсутствует на classpath (0 записей в minecraft-patched/
 * neoforge jar, переименован в {@code net.minecraft.world.level.block.entity}) — не split-package, поэтому
 * зеркалирование в compat-mirror возможно (тот же приём, что {@code net.minecraft.launchwrapper}, F2-shim).
 * <p>
 * neo {@code Blocks.FLOWER_POT} — {@code FlowerPotBlock(Blocks.AIR, properties)} БЕЗ BlockEntity вообще:
 * содержимое горшка в neo — ОТДЕЛЬНЫЙ per-вариант блок ({@code Blocks.POTTED_DANDELION}, ...,
 * neo-decompiled/.../level/block/Blocks.java:2774+), а не TE-состояние. Значит `WD.te(...)` НИКОГДА не
 * вернёт экземпляр этого типа для позиции с {@code FLOWER_POT} — {@code instanceof TileEntityFlowerPot}
 * во всех 4 живых местах (gregapi.block.misc.BlockBaseFlower, gregtech.items.MultiItemBumbles,
 * gregapi.worldgen.dungeon.{WorldgenDungeonGT,DungeonData}) остаётся синтаксически валиден (класс — реальный
 * подтип {@link BlockEntity}, cast легален), но структурно НЕДОСТИЖИМ в рантайме — класс здесь только ради
 * компиляции (instanceof/cast), конструктор нигде не вызывается (grep {@code new TileEntityFlowerPot} по
 * всему дереву = 0).
 * <p>
 * PORT-TODO(F-flowerpot, potted-item-model): реальное восстановление "положить цветок в горшок"/"прочитать,
 * что в горшке" требует редизайна на neo per-variant {@code POTTED_X}-блоки, ключуемые по item; GT6-собственные
 * цветы ({@code BlocksGT.FlowersA/FlowersB}, {@code POT_FLOWER_TILES}) не имеют {@code POTTED_}-аналога в
 * vanilla — не 1:1 маппинг, нужен отдельный F#-заход (не эта задача — компиляция split-package removed-типа).
 */
public class TileEntityFlowerPot extends BlockEntity {
	public TileEntityFlowerPot(BlockPos aPos, BlockState aState) {
		super(null, aPos, aState);
	}

	/** 1.7.10: возвращает {@link Item}, посаженный в горшок, или {@code null}, если горшок пуст. Здесь всегда {@code null} — см. javadoc класса. */
	public Item getFlowerPotItem() {
		return null;
	}

	/** 1.7.10 SRG-имя ({@code func_145964_a}) — ставит цветок {@code aItem} с метой {@code aMeta} в горшок. No-op — см. javadoc класса. */
	public void func_145964_a(Item aItem, int aMeta) {
	}
}
