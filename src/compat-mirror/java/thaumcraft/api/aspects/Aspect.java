package thaumcraft.api.aspects;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (константы аспектов + минимум методов) — держит ядро компилируемым, пока интеграция с TC отложена.
 *  Значения намеренно нейтральны (интеграция не исполняется без реального TC). См. compat-mirror/README.md. */
public class Aspect {
	// Обёртка GT6 (TC_Aspect) читает вложенный `.mAspect` — держим совместимое поле для зеркала.
	public final Aspect mAspect;

	public Aspect() {this.mAspect = null;}

	public int getMetadata() {return 0;}

	// Константы-аспекты, к которым обращается GregTech6 (grep Aspect.X по gregapi/*).
	public static final Aspect
		  AIR       = new Aspect(), ARMOR   = new Aspect(), AURA    = new Aspect(), BEAST   = new Aspect()
		, CLOTH     = new Aspect(), COLD    = new Aspect(), CRAFT   = new Aspect(), CROP    = new Aspect()
		, CRYSTAL   = new Aspect(), DARKNESS= new Aspect(), DEATH   = new Aspect(), EARTH   = new Aspect()
		, ELDRITCH  = new Aspect(), ENERGY  = new Aspect(), ENTROPY = new Aspect(), EXCHANGE= new Aspect()
		, FIRE      = new Aspect(), FLESH   = new Aspect(), FLIGHT  = new Aspect(), GREED   = new Aspect()
		, HARVEST   = new Aspect(), HEAL    = new Aspect(), HUNGER  = new Aspect(), LIFE    = new Aspect()
		, LIGHT     = new Aspect(), MAGIC   = new Aspect(), MAN     = new Aspect(), MECHANISM= new Aspect()
		, METAL     = new Aspect(), MIND    = new Aspect(), MINE    = new Aspect(), MOTION  = new Aspect()
		, ORDER     = new Aspect(), PLANT   = new Aspect(), POISON  = new Aspect(), SENSES  = new Aspect()
		, SLIME     = new Aspect(), SOUL    = new Aspect(), TAINT   = new Aspect(), TOOL    = new Aspect()
		, TRAP      = new Aspect(), TRAVEL  = new Aspect(), TREE    = new Aspect(), UNDEAD  = new Aspect()
		, VOID      = new Aspect(), WATER   = new Aspect(), WEAPON  = new Aspect(), WEATHER = new Aspect()
		;
}
