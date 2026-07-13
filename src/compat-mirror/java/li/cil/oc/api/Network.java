package li.cil.oc.api;

import li.cil.oc.api.network.Visibility;

/** F10 ЗЕРКАЛО (compile-only) чужого API — OpenComputers. Только цепочка, используемая
 *  EnvironmentOC (Network.newNode(this, Visibility.Network).create()). См. compat-mirror/README.md. */
public class Network {
	public static Builder newNode(Object aHost, Visibility aVisibility) {return new Builder();}

	public static class Builder {
		public Object create() {return null;}
	}
}
