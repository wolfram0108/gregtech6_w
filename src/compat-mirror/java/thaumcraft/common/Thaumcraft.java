package thaumcraft.common;

import java.util.LinkedHashMap;
import java.util.Map;

import thaumcraft.api.aspects.AspectList;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (CompatTC.validate: Thaumcraft.proxy.getPlayerKnowledge().aspectsDiscovered.values()).
 *  См. compat-mirror/README.md. */
public class Thaumcraft {
	public static CommonProxy proxy = new CommonProxy();

	public static class CommonProxy {
		public PlayerKnowledge getPlayerKnowledge() {return new PlayerKnowledge();}
	}

	public static class PlayerKnowledge {
		public Map<Object, AspectList> aspectsDiscovered = new LinkedHashMap<>();
	}
}
