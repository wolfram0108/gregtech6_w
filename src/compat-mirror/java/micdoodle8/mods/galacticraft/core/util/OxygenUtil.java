package micdoodle8.mods.galacticraft.core.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** F10 ЗЕРКАЛО (compile-only) чужого API Galacticraft. GT6 зовёт статический checkTorchHasOxygen в WD.oxygen();
 *  путь мёртв без GC (dimGC всегда false, guard MD.GC.mLoaded) -> возвращаем true = штатное «обычный мир, кислород
 *  есть». Честная отложенность F10, не тихий стаб (GC не портирован). Реальная зависимость — при интеграции. */
public interface OxygenUtil {
	static boolean checkTorchHasOxygen(Level world, Block block, int x, int y, int z) {return true;}
}
