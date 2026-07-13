package cpw.mods.fml.common.gameevent;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (GT_API_Proxy_Client.onClientTickEvent).
 *  Поле {@code phase} — Object: сравнение с {@code ServerTickEvent.END} (несуществующая
 *  neo-константа) — отдельный движко-шов вне зоны F10, не воспроизводим. См. compat-mirror/README.md. */
public class TickEvent {
	public static class ClientTickEvent {
		public Object phase;
	}
}
