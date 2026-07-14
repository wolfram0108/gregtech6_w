package micdoodle8.mods.galacticraft.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** F10 ЗЕРКАЛО (compile-only) чужого API Galacticraft. GT6 кастует блок к этому интерфейсу (WD:1163), НЕ реализует —
 *  сигнатура под neo-типы сайта (isSealed(Level,x,y,z,Direction)). Без GC блок никогда не IPartialSealableBlock
 *  (ветка мёртвая, guard MD.GC.mLoaded). Реальная зависимость — при интеграции. См. compat-mirror/README.md. */
public interface IPartialSealableBlock {
	boolean isSealed(Level world, int x, int y, int z, Direction direction);
}
