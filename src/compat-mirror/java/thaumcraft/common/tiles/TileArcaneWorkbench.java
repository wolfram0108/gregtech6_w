package thaumcraft.common.tiles;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (ContainerArcaneWorkbenchFixed — @Deprecated путь, не исполняется без реального TC). Реализует
 *  {@link Container} — ContainerArcaneWorkbenchFixed.onCraftMatrixChanged(mTileEntity) требует эту
 *  совместимость (реальный TileArcaneWorkbench у Thaumcraft — тоже инвентарь). См. compat-mirror/README.md. */
public class TileArcaneWorkbench implements Container {
	public ItemStack[] stackList = new ItemStack[0];

	public Level getLevel() {return null;}
	public ItemStack getStackInSlot(int aSlot) {return ItemStack.EMPTY;}
	public void setInventorySlotContentsSoftly(int aSlot, ItemStack aStack) {}

	@Override public int getContainerSize() {return 0;}
	@Override public boolean isEmpty() {return true;}
	@Override public ItemStack getItem(int aSlot) {return ItemStack.EMPTY;}
	@Override public ItemStack removeItem(int aSlot, int aAmount) {return ItemStack.EMPTY;}
	@Override public ItemStack removeItemNoUpdate(int aSlot) {return ItemStack.EMPTY;}
	@Override public void setItem(int aSlot, ItemStack aStack) {}
	@Override public void setChanged() {}
	@Override public boolean stillValid(Player aPlayer) {return false;}
	@Override public void clearContent() {}
}
