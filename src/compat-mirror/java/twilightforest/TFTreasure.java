package twilightforest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Random;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Twilight Forest. Минимум для наследника GT6. */
public class TFTreasure {
    protected TFTreasureTable useless;
    protected TFTreasureTable common;
    protected TFTreasureTable uncommon;
    protected TFTreasureTable rare;
    protected TFTreasureTable ultrarare;

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
