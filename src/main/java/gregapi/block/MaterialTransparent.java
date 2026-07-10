package gregapi.block;

/**
 * Переходник (PIVOT-6, BLOCK-MATERIAL). Дословная копия ванильного
 * net.minecraft.block.material.MaterialTransparent (Minecraft 1.7.10).
 */
public class MaterialTransparent extends Material {
	public MaterialTransparent(MapColor aColor) {
		super(aColor);
		this.setReplaceable();
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
