package ic2.api.tile;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap против реального артефакта
 *  IC2Classic-1.2.1.8-dev.jar (ic2.api.tile.IWrenchable), взятого из Gradle-кэша
 *  (~/.gradle/caches/modules-2/files-2.1/ic2/IC2Classic/1.2.1.8/.../IC2Classic-1.2.1.8-dev.jar):
 *    public abstract boolean wrenchCanSetFacing(EntityPlayer, int);
 *    public abstract short getFacing();
 *    public abstract void setFacing(short);
 *    public abstract boolean wrenchCanRemove(EntityPlayer);
 *    public abstract float getWrenchDropRate();
 *    public abstract ItemStack getWrenchDrop(EntityPlayer);
 *  EntityPlayer (1.7.10) -> neo Player, как во всём порту. Используются — WD.java:1237 (getFacing,
 *  wrenchCanRemove, getWrenchDropRate) и ToolCompat.java (wrenchCanSetFacing, setFacing, getWrenchDrop). */
public interface IWrenchable {
	short getFacing();
	boolean wrenchCanSetFacing(Player aPlayer, int aSide);
	void setFacing(short aFacing);
	boolean wrenchCanRemove(Player aPlayer);
	float getWrenchDropRate();
	ItemStack getWrenchDrop(Player aPlayer);
}
