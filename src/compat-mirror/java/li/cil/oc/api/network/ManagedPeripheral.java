package li.cil.oc.api.network;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;

/** F10 ЗЕРКАЛО (compile-only) чужого API — OpenComputers. Методы, реально реализуемые
 *  EnvironmentOC (не помечены @Override в оригинале, но контракт нужен для implements).
 *  См. compat-mirror/README.md. */
public interface ManagedPeripheral {
	Object[] invoke(String aMethod, Context aContext, Arguments aArgs);
	String[] methods();
}
