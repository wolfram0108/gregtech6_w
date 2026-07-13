package thaumcraft.api.research;

import java.util.LinkedHashMap;
import java.util.Map;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (CompatTC.addResearch: ResearchCategories.getResearchList(aCategory)). См. compat-mirror/README.md. */
public class ResearchCategories {
	private static final Map<String, ResearchCategoryList> sLists = new LinkedHashMap<>();

	public static ResearchCategoryList getResearchList(String aCategory) {
		return sLists.computeIfAbsent(aCategory, k -> new ResearchCategoryList());
	}
}
