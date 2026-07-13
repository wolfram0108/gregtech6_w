package forestry.apiculture.tiles;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Forestry. Только setLit, используемый
 *  GregTech6 (ToolCompat: ((TileCandle)aTileEntity).setLit(T)). См. compat-mirror/README.md. */
public interface TileCandle {
	void setLit(boolean aLit);
}
