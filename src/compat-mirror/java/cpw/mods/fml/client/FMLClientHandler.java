package cpw.mods.fml.client;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (GT_API_Proxy_Client: hasOptifine()).
 *  См. compat-mirror/README.md. */
public class FMLClientHandler {
	private static final FMLClientHandler INSTANCE = new FMLClientHandler();

	public static FMLClientHandler instance() {return INSTANCE;}

	public boolean hasOptifine() {return false;}
}
