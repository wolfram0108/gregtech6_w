package micdoodle8.mods.galacticraft.api.power;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Galacticraft. EnergySourceAdjacent(Direction) —
 *  реально используется CompatGC.java:37 (построение ENERGY_DIR по всем 7 FORGE_DIR). */
public class EnergySource {
	public static class EnergySourceAdjacent extends EnergySource {
		public EnergySourceAdjacent(Direction aDir) {}
	}
}
