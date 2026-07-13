package twilightforest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Random;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Twilight Forest. Минимум для наследника GT6.
 *  Добавлены 8 статических полей — GT_API_Post.java:795-809 читает их НАПРЯМУЮ (не рефлексией)
 *  как аргумент {@code UT.Reflection.getFieldContent(TFTreasure.tower_library, "ultrarare")}:
 *  сама рефлексия идёт по строковому имени вложенного поля (useless/common/uncommon/rare/ultrarare,
 *  уже существовали ниже), а верхний статический field-access должен резолвиться компилятором. */
public class TFTreasure {
    protected TFTreasureTable useless;
    protected TFTreasureTable common;
    protected TFTreasureTable uncommon;
    protected TFTreasureTable rare;
    protected TFTreasureTable ultrarare;

    public static TFTreasure tower_library;
    public static TFTreasure labyrinth_vault;
    public static TFTreasure stronghold_room;
    public static TFTreasure darktower_cache;
    public static TFTreasure aurora_cache;
    public static TFTreasure aurora_room;
    public static TFTreasure darktower_key;
    public static TFTreasure troll_vault;

    public TFTreasure(int aIndex) {}

    public boolean generate(Level aWorld, Random aRandom, int aX, int aY, int aZ) {
        return false;
    }

    public boolean generate(Level aWorld, Random aRandom, int aX, int aY, int aZ, Block aChest) {
        return false;
    }

    public ItemStack getRareItem(Random aRandom) {
        return null;
    }

    public ItemStack getUncommonItem(Random aRandom) {
        return null;
    }

    public ItemStack getCommonItem(Random aRandom) {
        return null;
    }
}
