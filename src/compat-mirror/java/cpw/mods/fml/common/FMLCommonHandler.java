package cpw.mods.fml.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import cpw.mods.fml.relauncher.Side;

/** F10 ЗЕРКАЛО (compile-only) — legacy Forge/FML 1.7.10 (пакет cpw.mods.fml не существует
 *  на neo-classpath). Только используемое GregTech6 (WD/UT/TileEntityBase01Root/
 *  DelegatorTileEntity/WorldAndCoords/MultiTileEntityAdvancedCraftingTable). См. compat-mirror/README.md. */
public class FMLCommonHandler {
	private static final FMLCommonHandler INSTANCE = new FMLCommonHandler();

	public static FMLCommonHandler instance() {return INSTANCE;}

	public Side getEffectiveSide() {return Side.SERVER;}

	public void firePlayerCraftingEvent(Player aPlayer, ItemStack aCrafted, CraftingInput aCraftMatrix) {}

	public void firePlayerChangedDimensionEvent(Object aPlayer, int aFrom, int aTo) {}
}
