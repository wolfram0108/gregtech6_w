package micdoodle8.mods.galacticraft.api.transmission.tile;

import net.minecraft.core.Direction;
import micdoodle8.mods.galacticraft.api.transmission.NetworkType;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Galacticraft. Реально используется —
 *  EnergyCompat.java:114,181: canConnect. */
public interface IConnector {
	boolean canConnect(Direction aSide, NetworkType aType);
}
