package gregapi.oredict;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import gregapi.util.ST;

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
 * <p><b>Событие + дедуп (R8, восстановлено 1:1; УЛИКА R8-2 доработка dedup-семантики).</b> Оригинал
 * Forge {@code registerOreImpl} (эталон
 * {@code gt6-oracle-dumper/build/tmp/recompSrc/net/minecraftforge/oredict/OreDictionary.java:517-562})
 * перед вставкой считал HASH из ID реестра предмета, к которому (только если {@code meta !=
 * WILDCARD_VALUE}) подмешивал {@code (meta+1)<<16} ({@code :543-546}) — то есть wildcard-запись
 * (meta == WILDCARD_VALUE) попадает в hash-бакет БЕЗ meta-битов и НИКОГДА не совпадает с
 * hash-бакетом конкретной meta того же предмета; проверял, не встречался ли уже {@code oreID} этого
 * имени в этом бакете ({@code ids.contains(oreID) return}, {@code :549-550}), и после реальной вставки
 * рассылал {@code MinecraftForge.EVENT_BUS.post(new OreRegisterEvent(name, ore))} ({@code :561}). NBT
 * в hash НЕ участвует (только Item+meta) — {@code ST.equal(a,b,true)} с {@code aIgnoreNBT=true}
 * корректно это отражал, НО его meta-компаратор {@code equal(long,long)}
 * ({@code aMeta1==aMeta2 || aMeta1==W || aMeta2==W}) СИММЕТРИЧНО-permissive: любой wildcard с ЛЮБОЙ
 * стороны считает мету равной — это НЕ Forge-семантика (у Forge wildcard-бакет и конкретный-meta-бакет
 * РАЗНЫЕ, схлопывания нет). Заменено точным hash-бакет-критерием Forge: тот же Item И (обе meta ==
 * {@link #WILDCARD_VALUE}, либо обе meta численно равны — без permissive-wildcard-стороны). Рассылка —
 * через {@code NeoForge.EVENT_BUS.post(...)}, на который {@code OreDictManager} подписан
 * ({@code @SubscribeEvent onOreRegistration1} + {@code NeoForge.EVENT_BUS.register(this)}, {@code :121}).
 * Свои ore GregTech6 ДОПОЛНИТЕЛЬНО прогоняет через РЕПЛЕЙ в конструкторе ({@code OreDictManager:118}) —
 * это покрывает записи, существовавшие ДО создания {@code OreDictManager}; сама рассылка здесь покрывает
 * все записи ПОСЛЕ (ровно как в оригинале, где реплей и живая {@code EVENT_BUS.post} — два разных
 * источника одного потока). Требование движка: {@code IEventBus.post(T)} принимает только
 * {@code T extends net.neoforged.bus.api.Event} (реально проверено в реализации шины —
 * {@code fml-decompiled/.../bus/EventBus.java:143}, кидает {@code IllegalArgumentException} на
 * {@code register(this)} для не-{@code Event} параметра {@code @SubscribeEvent}-метода) — поэтому
 * {@code OreRegisterEvent} здесь наследует {@code Event} (движко-форсированный тип-шов, не отсебятина).</p>
 */
public class OreDictionary {
	private OreDictionary() {/* только статика, как у Forge-класса */}

	/** Метка-джокер «любая metadata». Значение Forge (= {@link Short#MAX_VALUE}); матчинг — зона F1 (F4 §4.3). */
	public static final int WILDCARD_VALUE = Short.MAX_VALUE;

	/** Роль-A: плоское хранилище {@code имя -> список ItemStack}. LinkedHashMap — детерминированный порядок свипа ({@code :118}).
	 *  Тип значения — {@code ArrayList} (не просто {@code List}), чтобы 1-арг {@code getOres(name)} мог вернуть его
	 *  оригинальный тип {@code ArrayList<ItemStack>} без каста (см. ниже, R8-сигнатура). */
	private static final Map<String, ArrayList<ItemStack>> sOres = new LinkedHashMap<>();
	private static final List<ItemStack> EMPTY = Collections.emptyList();

	/** Forge {@code registerOre(name, stack)} == {@code registerOreImpl} 1:1: дедуп (пропускает уже
	 *  зарегистрированный под этим именем физический стек) + рассылка {@code OreRegisterEvent} после
	 *  реальной вставки копии (см. javadoc класса, R8). */
	public static void registerOre(String aName, ItemStack aStack) {
		ArrayList<ItemStack> tList = sOres.computeIfAbsent(aName, k -> new ArrayList<>());
		for (ItemStack tExisting : tList) if (sameHashBucket(tExisting, aStack)) return;
		ItemStack tOre = aStack.copy();
		tList.add(tOre);
		NeoForge.EVENT_BUS.post(new OreRegisterEvent(aName, tOre));
	}

	/** УЛИКА R8-2: Forge hash-бакет 1:1 ({@code registerOreImpl:543-546} — meta подмешивается в hash
	 *  ТОЛЬКО если {@code != WILDCARD_VALUE}), НЕ общий {@code ST.equal(...,true)} (тот трактует
	 *  wildcard permissive-симметрично и ошибочно схлопывает wildcard-запись с конкретной meta). */
	private static boolean sameHashBucket(ItemStack aExisting, ItemStack aStack) {
		return aExisting.getItem() == aStack.getItem() && ST.meta_(aExisting) == ST.meta_(aStack);
	}

	/** Forge 1-арг {@code getOres(name)}: авто-создаёт запись и возвращает ЖИВОЙ список (на него ссылается {@code OD.mItems}).
	 *  Тип возврата — {@code ArrayList<ItemStack>}, ровно как у оригинала
	 *  ({@code gt6-oracle-dumper/.../OreDictionary.java:376}); потребители ({@code Compat_Recipes_HarvestCraft} и др.)
	 *  объявляют переменные именно этим типом (R8-сигнатура). */
	public static ArrayList<ItemStack> getOres(String aName) {
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
	public static class OreRegisterEvent extends Event {
		public final String Name;
		public final ItemStack Ore;

		public OreRegisterEvent(String aName, ItemStack aOre) {
			Name = aName;
			Ore = aOre;
		}
	}
}
