package thaumcraft.api.aspects;

import net.minecraft.core.Direction;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Thaumcraft. Полный интерфейс эссентия-транспорта
 *  (сверено TC4 API), параметр стороны адаптирован к neo {@link Direction}. Реально GregTech
 *  использует takeEssentia/getEssentiaType/getEssentiaAmount (Behavior_Plunger_Essentia).
 *  См. compat-mirror/README.md. */
public interface IEssentiaTransport {
	boolean isConnectable(Direction face);
	boolean canInputFrom(Direction face);
	boolean canOutputTo(Direction face);
	void setSuction(Aspect aspect, int amount);
	Aspect getSuctionType(Direction face);
	int getSuctionAmount(Direction face);
	int takeEssentia(Aspect aspect, int amount, Direction face);
	int addEssentia(Aspect aspect, int amount, Direction face);
	Aspect getEssentiaType(Direction face);
	int getEssentiaAmount(Direction face);
	int getMinimumSuction();
	boolean renderExtendedTube();
}
