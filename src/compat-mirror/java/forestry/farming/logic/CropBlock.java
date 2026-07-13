package forestry.farming.logic;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import forestry.api.farming.ICrop;
import forestry.core.utils.vect.Vect;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Forestry. Только конструктор, используемый
 *  GregTech6 (CompatFR.getCropAt: new CropBlock(aWorld, aBlock, aMeta, aVect)). См. compat-mirror/README.md. */
public class CropBlock implements ICrop {
	public CropBlock(Level aWorld, Block aBlock, int aMeta, Vect aPos) {}
}
