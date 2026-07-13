package thaumcraft.api.aspects;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6.
 *  Chainable add(Aspect,int) — GT6 строит наборы `new AspectList().add(Aspect.X, n).add(...)`.
 *  См. compat-mirror/README.md. */
public class AspectList {
	public AspectList() {}

	public AspectList add(Aspect aAspect, int aAmount) {return this;}

	public int getAmount(Aspect aAspect) {return 0;}

	public int size() {return 0;}

	public Aspect[] getAspects() {return new Aspect[0];}
}
