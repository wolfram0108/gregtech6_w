package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только используется в instanceof
 *  (TileEntityBase08FluidContainer) — конструктор никогда не зовётся без реального TC.
 *  См. compat-mirror/README.md. */
public class TileCrucible extends BlockEntity {
	public TileCrucible(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {super(aType, aPos, aState);}
}
