package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;

/** F10 ЗЕРКАЛО (compile-only) чужого API — ComputerCraft. Полный набор методов, которые
 *  CompatCC.ComputerizablePeripheral реально реализует (не все помечены @Override в оригинале,
 *  но контракт нужен для {@code implements IPeripheral}). См. compat-mirror/README.md. */
public interface IPeripheral {
	String getType();
	String[] getMethodNames();
	Object[] callMethod(IComputerAccess aComputer, ILuaContext aContext, int aFunctionIndex, Object[] aArguments) throws LuaException, InterruptedException;
	void attach(IComputerAccess aComputer);
	void detach(IComputerAccess aComputer);
	boolean equals(IPeripheral aOther);
}
