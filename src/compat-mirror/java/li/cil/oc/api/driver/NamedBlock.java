package li.cil.oc.api.driver;

/** F10 ЗЕРКАЛО (compile-only) чужого API — OpenComputers. Методы, реально реализуемые
 *  EnvironmentOC (не помечены @Override в оригинале, но контракт нужен для implements).
 *  См. compat-mirror/README.md. */
public interface NamedBlock {
	String preferredName();
	int priority();
}
