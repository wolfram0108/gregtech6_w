package thaumcraft.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (grep ThaumcraftApi.* по gregapi/*). Реальная зависимость — при возврате к интеграции TC.
 *  Тела намеренно пусты (интеграция не исполняется без реального Thaumcraft). См. compat-mirror/README.md. */
public class ThaumcraftApi {
	/** GT_API.java / CompatTC: objectTags.isEmpty()/containsKey/put — заглушка-карта до интеграции.
	 *  Ключ — CompatTC.java:379/391 кладёт List(Item,Integer), потому Object, не String. */
	public static Map<Object, Object> objectTags = new HashMap<>();

	/** CompatTC.java: portableHoleBlackList.add(...). */
	public static List<Object> portableHoleBlackList = new ArrayList<>();

	public static void registerEntityTag(String aEntityName, AspectList aAspects) {}

	public static void registerComplexObjectTag(ItemStack aStack, AspectList aAspects) {}

	/** CompatTC.java:354/359/375/387: addCrucibleRecipe(String, ItemStack, Object, AspectList). */
	public static Object addCrucibleRecipe(String aResearch, ItemStack aOutput, Object aInput, AspectList aAspects) {return null;}
}
