package ic2.api.energy.event;

import ic2.api.energy.tile.IEnergyTile;

/** F10 ЗЕРКАЛО (compile-only) чужого API IC2. GT6 постит его в NeoForge.EVENT_BUS из TileEntityBase01Root.
 *  loadIntoEnet() (под @Optional.Method(IC2)). Реальный IC2-класс — конкретный Forge-Event с ctor(IEnergyTile);
 *  здесь extends neo Event (bus.api.Event), чтобы годиться post(); без IC2 подписчиков нет → no-op.
 *  См. compat-mirror/README.md. */
public class EnergyTileLoadEvent extends net.neoforged.bus.api.Event {
	public final IEnergyTile tile;
	public EnergyTileLoadEvent(IEnergyTile aTile) {tile = aTile;}
}
