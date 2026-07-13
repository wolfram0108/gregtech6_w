package forestry.api.farming;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Forestry. Только статика, используемая
 *  GregTech6 (CompatFR.onPostLoad: farmables.get("farmArboreal").add(this)). Путь не исполняется
 *  без реального Forestry — карта пуста, значение недостижимо. См. compat-mirror/README.md. */
public class Farmables {
	public static Map<String, List<IFarmable>> farmables = new LinkedHashMap<>();
}
