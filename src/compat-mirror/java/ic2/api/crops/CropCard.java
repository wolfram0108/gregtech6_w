package ic2.api.crops;

import net.minecraft.util.IIcon;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Было {@code interface CropCard {}} — сломано:
 *  GT_BaseCrop.java:40 делает {@code class GT_BaseCrop extends CropCard} (класс не может
 *  extends интерфейс) — нужен класс. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.crops.CropCard, {@code public abstract class}, упрощено до конкретного класса —
 *  ничего кроме GT_BaseCrop его не наследует, F10 dead-path).
 *  Реально используются: поле {@code textures} — GT_BaseCrop.java:161 (прямая запись, унаследованное
 *  protected-поле); displayName()/attributes()/discoveredBy() — вызываются на {@code ICropTile.getCrop()}
 *  в Behavior_Cropnalyzer.java:84,99,101. Остальные методы реального CropCard (name/tier/stat/maxSize/
 *  canGrow/canBeHarvested/canCross/getGain/rightclick/getOptimalHavestSize/registerSprites/…) в
 *  GT6-исходнике нигде не вызываются НА ссылке типа CropCard (только переопределены в GT_BaseCrop как
 *  собственные методы — {@code // @Override} закомментирован, реального override не требуется) —
 *  не добавлены. */
public class CropCard {
	protected IIcon[] textures;

	public String displayName() {return NB();}
	public String[] attributes() {return null;}
	public String discoveredBy() {return NB();}

	private static String NB() {return null;}
}
