package gregapi;

/**
 * ПРОБНЫЙ JEI-плагин: существует только ради живого судьи витрины ({@code gt6jeicraft}).
 *
 * <p>Чтобы спросить готовую витрину «а что ты показываешь игроку», нужен {@code IJeiRuntime} — его JEI отдаёт
 * только своим плагинам. Раньше ссылку держал боевой {@code GT6_JEI_Plugin}, то есть в моде жила точка,
 * которой в оригинале не было и которая нужна была исключительно диагностике. Теперь её держит ЭТОТ класс:
 * он лежит в {@code src/probes} и попадает в сборку лишь при {@code -Pgt6probes} — в jar игрока его нет.</p>
 *
 * <p>Плагин ничего не регистрирует и ни на что не влияет: только запоминает runtime. Категории, рецепты,
 * подтипы — по-прежнему целиком за боевым {@code GT6_JEI_Plugin}, второй реализации не заводится.</p>
 */
@mezz.jei.api.JeiPlugin
public final class GT6ProbeJeiPlugin implements mezz.jei.api.IModPlugin {
	/** Живой runtime JEI для пробы {@code gt6jeicraft}; {@code null}, пока витрина не поднялась. */
	public static volatile mezz.jei.api.runtime.IJeiRuntime RUNTIME = null;

	private static final net.minecraft.resources.Identifier UID =
		net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, "probe_jei");

	@Override public net.minecraft.resources.Identifier getPluginUid() {return UID;}

	@Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime aRuntime) {RUNTIME = aRuntime;}
}
