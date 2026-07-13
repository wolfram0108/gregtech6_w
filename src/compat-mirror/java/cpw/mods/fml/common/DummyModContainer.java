package cpw.mods.fml.common;

import com.google.common.eventbus.EventBus;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath; coremod ASM-система в neo — иная модель, отдельный шов вне зоны F10).
 *  Только используемое GregTech6 (GT_ASM_Dummy extends DummyModContainer). См. compat-mirror/README.md. */
public abstract class DummyModContainer {
	private final ModMetadata mMetadata;

	public DummyModContainer(ModMetadata aMetadata) {mMetadata = aMetadata;}

	public ModMetadata getMetadata() {return mMetadata;}

	public boolean registerBus(EventBus aBus, LoadController aController) {return false;}
}
