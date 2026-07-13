package ic2.api.reactor;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.reactor.IReactorChamber). Реально используется — WD.java:1228: getReactor()
 *  (результат приводится к BlockEntity — легальный unchecked-cast интерфейса к
 *  неfinal-классу, IReactor extends BlockEntity не нужен). Метод setRedstoneSignal
 *  реального API не используется (греп 0) — не добавлен. */
public interface IReactorChamber {
	IReactor getReactor();
}
