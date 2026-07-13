package ic2.api.crops;

import net.minecraft.world.item.ItemStack;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Было {@code interface Crops {}} — сломано:
 *  {@code Crops.instance} вызывается как объект-менеджер (.registerBaseSeed/.getCropCard/
 *  .getCropList/.registerCrop) — CompatIC2.java:77, GT_BaseCrop.java:76-77,
 *  Compat_Recipes_IndustrialCraft.java:268. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.crops.Crops, {@code public abstract class}, упрощено до конкретного класса — instance
 *  никогда не присваивается в GT6-коде, dead-path F10). Поле weed реального API нигде не
 *  используется (греп 0) — не добавлено. */
public class Crops {
	public static Crops instance;

	public boolean registerBaseSeed(ItemStack aSeed, CropCard aCard, int aA, int aB, int aC, int aD) {return false;}
	public CropCard getCropCard(String aOwner, String aName) {return null;}
	public CropCard[] getCropList() {return null;}
	public short registerCrop(CropCard aCard) {return 0;}
}
