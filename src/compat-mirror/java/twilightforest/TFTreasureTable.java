package twilightforest;

import net.minecraft.world.item.enchantment.Enchantment;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Twilight Forest. Минимум для TwilightTreasureReplacer
 *  + GT_API_Post.java:795-809 (addEnchantedBook(Enchantment,int) — 9 вызовов через
 *  {@code (TFTreasureTable)UT.Reflection.getFieldContent(TFTreasure.<поле>, "<строка>")}). */
public class TFTreasureTable {
    public void add(Object aStack) {}

    public void add(Object aItemOrBlock, int aAmount) {}

    public void clear() {}

    public void addEnchantedBook(Enchantment aEnchant, int aWeight) {}
}
