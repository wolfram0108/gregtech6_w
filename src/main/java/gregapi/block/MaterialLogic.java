package gregapi.block;

/**
 * Переходник (PIVOT-6, BLOCK-MATERIAL). Дословная копия ванильного
 * net.minecraft.block.material.MaterialLogic (Minecraft 1.7.10).
 */
public class MaterialLogic extends Material {
	public MaterialLogic(MapColor aColor) {
		super(aColor);
		this.setAdventureModeExempt();
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
