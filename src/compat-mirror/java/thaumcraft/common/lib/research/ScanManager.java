package thaumcraft.common.lib.research;

import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.ScanResult;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (CompatTC.scan: hasBeenScanned/completeScan). См. compat-mirror/README.md. */
public class ScanManager {
	public static boolean hasBeenScanned(Player aPlayer, ScanResult aResult) {return false;}
	public static boolean completeScan(Player aPlayer, ScanResult aResult, String aExtra) {return false;}
}
