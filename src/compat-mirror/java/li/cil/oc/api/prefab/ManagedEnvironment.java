package li.cil.oc.api.prefab;

/** F10 ЗЕРКАЛО (compile-only) чужого API — OpenComputers. Базовый абстрактный класс,
 *  реализующий сетевой интерфейс {@code li.cil.oc.api.network.ManagedEnvironment}
 *  (EnvironmentOC extends этот класс). См. compat-mirror/README.md. */
public abstract class ManagedEnvironment implements li.cil.oc.api.network.ManagedEnvironment {
	public void setNode(Object aNode) {}
}
