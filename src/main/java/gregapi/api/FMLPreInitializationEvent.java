package gregapi.api;

import java.io.File;

/**
 * Переходник F1 (жизненный цикл). Событие фазы PreInit GregTech6 поверх жизненного цикла NeoForge.
 *
 * <p>В Forge 1.7.10 движок сам передавал {@code cpw.mods.fml.common.event.FMLPreInitializationEvent}.
 * В NeoForge такого события нет — фазы устроены иначе. Поэтому GregTech6 сохраняет СВОЙ трёхфазный
 * контракт (Pre/Init/Post), а neo-точка входа ({@code gregtech6.GregTech6}) конструирует это событие
 * и передаёт его в {@code Abstract_Mod.onModPreInit(...)}. Так код всех модов остаётся 1:1.</p>
 *
 * <p>По всему исходнику GregTech6 с этого события читается ровно один метод —
 * {@link #getModConfigurationDirectory()} (см. {@code gregapi.GT_API}). Больше событие ничего не несёт.</p>
 */
public class FMLPreInitializationEvent {
	private final File mModConfigurationDirectory;

	public FMLPreInitializationEvent(File aModConfigurationDirectory) {
		mModConfigurationDirectory = aModConfigurationDirectory;
	}

	/** Каталог конфигураций мода (в neo — {@code FMLPaths.CONFIGDIR}). */
	public File getModConfigurationDirectory() {
		return mModConfigurationDirectory;
	}
}
