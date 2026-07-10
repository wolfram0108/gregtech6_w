package gregapi.block;

/**
 * Переходник (PIVOT-6, BLOCK-MATERIAL). Дословная копия ванильного
 * net.minecraft.block.material.MaterialPortal (Minecraft 1.7.10).
 */
public class MaterialPortal extends Material {
	public MaterialPortal(MapColor aColor) {
		super(aColor);
	}

	public boolean isSolid() {
		return false;
	}

	public boolean getCanBlockGrass() {
		return false;
	}

	public boolean blocksMovement() {
		return false;
	}
}
