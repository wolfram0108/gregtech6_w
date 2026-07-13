package thaumcraft.api.aspects;

import java.util.LinkedHashMap;
import java.util.Map;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6.
 *  Chainable add(Aspect,int) — GT6 строит наборы `new AspectList().add(Aspect.X, n).add(...)`.
 *  См. compat-mirror/README.md. */
public class AspectList {
	/** CompatTC.validate(): for (Map.Entry<Aspect,Integer> : tList.aspects.entrySet()). */
	public Map<Aspect, Integer> aspects = new LinkedHashMap<>();

	public AspectList() {}

	public AspectList add(Aspect aAspect, int aAmount) {return this;}

	public int getAmount(Aspect aAspect) {return 0;}

	public int size() {return 0;}

	public Aspect[] getAspects() {return new Aspect[0];}

	/** gregtech.asm.transformers.Thaumcraft_AspectLagFix: {@code aspects.copy()} — реальная неглубокая копия аспект-карты (не {@code this}, чтобы кэш-мутация не текла в оригинал). */
	public AspectList copy() {
		AspectList rCopy = new AspectList();
		rCopy.aspects.putAll(aspects);
		return rCopy;
	}
}
