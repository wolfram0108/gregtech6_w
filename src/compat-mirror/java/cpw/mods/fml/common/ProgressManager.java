package cpw.mods.fml.common;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (UT.LoadingBar: push/pop, поля message/step
 *  читаются рефлексией по имени — не требуют совпадения на этапе компиляции). См. compat-mirror/README.md. */
public class ProgressManager {
	public static ProgressBar push(String aTitle, int aSize, boolean aIsFake) {return new ProgressBar();}
	public static void pop(ProgressBar aBar) {}

	public static class ProgressBar {
		public String message = "";
		public int step = 0;
	}
}
