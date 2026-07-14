package micdoodle8.mods.galacticraft.api.event.wgen;

/** F10 ЗЕРКАЛО (compile-only) чужого API Galacticraft. GT6 подписывается @SubscribeEvent на .Post в
 *  CompatGC.populate (worldObj/chunkX/chunkZ). Post extends neo Event (bus.api.Event) чтобы годиться
 *  обработчику; без GC событие не публикуется. См. compat-mirror/README.md. */
public class GCCoreEventPopulate {
	public static class Post extends net.neoforged.bus.api.Event {
		public net.minecraft.world.level.Level worldObj;
		public int chunkX, chunkZ;
	}
}
