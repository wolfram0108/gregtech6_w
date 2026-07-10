package gregapi.block;

/**
 * Переходник (PIVOT-6, BLOCK-MATERIAL). Дословная копия ванильного
 * net.minecraft.block.material.MaterialLiquid (Minecraft 1.7.10). База для GT6-подклассов
 * MaterialGas/MaterialOil.
 */
public class MaterialLiquid extends Material {
	public MaterialLiquid(MapColor aColor) {
		super(aColor);
		this.setReplaceable();
		this.setNoPushMobility();
	}

	public boolean isLiquid() {
		return true;
	}

	public boolean blocksMovement() {
		return false;
	}

	public boolean isSolid() {
		return false;
	}
}
