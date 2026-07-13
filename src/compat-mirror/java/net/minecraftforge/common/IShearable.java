package net.minecraftforge.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

import java.util.ArrayList;

/**
 * F10 compile-only shim. 1.7.10 Forge {@code net.minecraftforge.common.IShearable} удалён в neo (стрижка через
 * {@code IShearable} neo-варианта/теги). Поверхность 1:1 с 1.7.10 (референс), World->BlockGetter. GT6-листва
 * (BlockBaseLeaves) реализует + {@code instanceof} в Behavior_Shears. Реальная стрижка -> F-shear (PORT-TODO).
 */
public interface IShearable {
	boolean isShearable(ItemStack aItem, BlockGetter aWorld, int aX, int aY, int aZ);
	ArrayList<ItemStack> onSheared(ItemStack aItem, BlockGetter aWorld, int aX, int aY, int aZ, int aFortune);
}
