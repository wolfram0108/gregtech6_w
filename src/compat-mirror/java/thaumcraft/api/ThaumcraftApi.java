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
	/** GT_API.java / CompatTC: objectTags.isEmpty()/containsKey/put — заглушка-карта до интеграции. */
	public static Map<String, Object> objectTags = new HashMap<>();

	/** CompatTC.java: portableHoleBlackList.add(...). */
	public static List<Object> portableHoleBlackList = new ArrayList<>();

	public static void registerEntityTag(String aEntityName, AspectList aAspects) {}

	public static void registerComplexObjectTag(ItemStack aStack, AspectList aAspects) {}

	public static void addCrucibleRecipe(String aResearch, Object aInput, ItemStack aOutput, AspectList aAspects) {}
}
