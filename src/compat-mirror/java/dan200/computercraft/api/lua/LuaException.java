package dan200.computercraft.api.lua;

/** F10 ЗЕРКАЛО (compile-only) чужого API — ComputerCraft. Checked-исключение, объявляется
 *  в throws IPeripheral.callMethod (CompatCC.ComputerizablePeripheral). См. compat-mirror/README.md. */
public class LuaException extends Exception {
	public LuaException() {super();}
	public LuaException(String aMessage) {super(aMessage);}
}
