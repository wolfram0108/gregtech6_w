package gregapi.oredict;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F4 ЦЕНТРАЛЬНЫЙ ПЕРЕХОДНИК — замена удалённого Forge-класса {@code net.minecraftforge.oredict.OreDictionary}.
 * См. {@code doc/missions/gt6-port/decisions/F4-oredictionary.md} §4.1 и {@code architecture/oredict.md}.
 *
 * <p>В Forge 1.7.10 этот класс держал плоское хранилище {@code имя -> список ItemStack} и рассылал
 * {@code OreRegisterEvent} по шине. GregTech6 уже владеет всей богатой семантикой поверх (унификация,
 * ассоциация, слушатели с реплеем — {@code OreDictManager}); единственное, чем реально владел Forge-класс,
 * это плоское хранилище (роль-A F4-ADR). По решению F4 оно интернализуется сюда БЕЗ изменения логики
 * вызывающих: точки {@code OreDictionary.registerOre/getOres/getOreNames/WILDCARD_VALUE} остаются
 * дословными (1:1), меняется только пакет в импорте (словарь движка F4).</p>
 *
 * <p><b>Событие.</b> Свои ore GregTech6 прогоняет в {@code OreDictManager} через РЕПЛЕЙ в конструкторе
 * ({@code OreDictManager:118}), а не через шину; шина ({@code NeoForge.EVENT_BUS.register}, {@code :121})
 * ловит только ЧУЖИЕ моды — это роль-B, отложенная в фазу 12 (F4 §4.4). Поэтому здесь событие —
 * простой класс-носитель ({@code Name}, {@code Ore}), без обвязки шины. Реконсиляция диспетчеризации с
 * neo-шиной (нужно ли {@code OreRegisterEvent} наследовать neo {@code Event}) — шов F4↔F7, проверяется
 * под parity, здесь компиляцию не трогает.</p>
 */
public class OreDictionary {
	private OreDictionary() {/* только статика, как у Forge-класса */}

	/** Метка-джокер «любая metadata». Значение Forge (= {@link Short#MAX_VALUE}); матчинг — зона F1 (F4 §4.3). */
	public static final int WILDCARD_VALUE = Short.MAX_VALUE;

	/** Роль-A: плоское хранилище {@code имя -> список ItemStack}. LinkedHashMap — детерминированный порядок свипа ({@code :118}). */
	private static final Map<String, List<ItemStack>> sOres = new LinkedHashMap<>();
	private static final List<ItemStack> EMPTY = Collections.emptyList();

	/** Forge {@code registerOre(name, stack)}: добавляет копию стека в список имени (Forge тоже копировал). */
	public static void registerOre(String aName, ItemStack aStack) {
		sOres.computeIfAbsent(aName, k -> new ArrayList<>()).add(aStack.copy());
	}

	/** Forge 1-арг {@code getOres(name)}: авто-создаёт запись и возвращает ЖИВОЙ список (на него ссылается {@code OD.mItems}). */
	public static List<ItemStack> getOres(String aName) {
		return sOres.computeIfAbsent(aName, k -> new ArrayList<>());
	}

	/** Forge 2-арг {@code getOres(name, alwaysCreateEntry)}: живой список либо (при {@code false} и отсутствии) общий пустой. */
	public static List<ItemStack> getOres(String aName, boolean aAlwaysCreateEntry) {
		if (aAlwaysCreateEntry) return sOres.computeIfAbsent(aName, k -> new ArrayList<>());
		List<ItemStack> tList = sOres.get(aName);
		return tList == null ? EMPTY : tList;
	}

	/** Forge {@code getOreNames()}: все зарегистрированные имена. */
	public static String[] getOreNames() {
		return sOres.keySet().toArray(new String[0]);
	}

	/**
	 * F4 §4.2 — простой GT6-класс-носитель взамен Forge {@code OreDictionary.OreRegisterEvent}.
	 * Поля {@code Name}/{@code Ore} — ровно то, что читает {@code OreDictManager.onOreRegistration1}.
	 * Вложен в {@code OreDictionary}, чтобы импорт {@code OreDictionary.OreRegisterEvent} остался 1:1.
	 */
	public static class OreRegisterEvent {
		public final String Name;
		public final ItemStack Ore;

		public OreRegisterEvent(String aName, ItemStack aOre) {
			Name = aName;
			Ore = aOre;
		}
	}
}
