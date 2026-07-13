package ic2.api.event;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Компилятор вскрыл сверх исходной спеки — CompatIC2.java:87-95
 *  (@SubscribeEvent onRetextureEvent) читает поля {@code world/x/y/z/side/referencedBlock/
 *  referencedMeta/applied}. Сверено javap ic2:IC2Classic:1.2.1.8-dev (ic2.api.event.RetextureEvent
 *  extends net.minecraftforge.event.world.WorldEvent, поле world — из WorldEvent 1.7.10).
 *  Здесь БЕЗ extends WorldEvent (чужая forge-1.7.10 иерархия событий не мигрирует в neo,
 *  и не нужна — компилятору достаточно плоских полей); world типизирован Level, как везде в порту
 *  (WD.te(Level,...) — единственный потребитель). Поле referencedSide реального API не
 *  используется (греп 0) — не добавлено. */
public class RetextureEvent {
	public Level world;
	public int x;
	public int y;
	public int z;
	public int side;
	public Block referencedBlock;
	public int referencedMeta;
	public boolean applied;
}
