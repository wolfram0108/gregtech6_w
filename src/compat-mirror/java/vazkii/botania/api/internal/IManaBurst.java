package vazkii.botania.api.internal;

/** F10 ЗЕРКАЛО (compile-only) чужого API Botania. GT6 ВЫЗЫВАЕТ isFake/getColor/getSourceLens в
 *  MultiTileEntityBlockWithCompat.onBurstCollision (@Optional.Method BOTA, ветка мёртва без Botania).
 *  Сигнатуры сверены javap Botania-r1.8-250 (getSourceLens: старый ItemStack ремаплен на neo). Реальный мод
 *  не грузится. См. compat-mirror/README.md. */
public interface IManaBurst {
	boolean isFake();
	int getColor();
	net.minecraft.world.item.ItemStack getSourceLens();
}
