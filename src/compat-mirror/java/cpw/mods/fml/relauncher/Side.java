package cpw.mods.fml.relauncher;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (FMLCommonHandler.getEffectiveSide().
 *  isServer()/isClient()). См. compat-mirror/README.md. */
public enum Side {
	CLIENT, SERVER;

	public boolean isServer() {return this == SERVER;}
	public boolean isClient() {return this == CLIENT;}
}
