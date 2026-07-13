package cpw.mods.fml.client.registry;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (GT_API_Proxy_Client). См. compat-mirror/README.md. */
public class RenderingRegistry {
	public static int addNewArmourRendererPrefix(String aPrefix) {return 0;}
	public static void registerEntityRenderingHandler(Class<?> aEntityClass, Object aRenderer) {}
	public static void registerBlockHandler(Object aRenderer) {}
	public static int getNextAvailableRenderId() {return 0;}
}
