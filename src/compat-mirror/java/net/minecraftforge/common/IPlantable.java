package net.minecraftforge.common;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/**
 * F10 compile-only shim. 1.7.10 Forge {@code net.minecraftforge.common.IPlantable} удалён в neo. Поверхность 1:1
 * с 1.7.10 (референс), World->BlockGetter. GT6-растения (BlockBaseFlower/LilyPad/Sapling) его реализуют + проверки
 * {@code instanceof IPlantable}. Реальная семантика посадки растений — контракт F-plant (PORT-TODO); тип для сборки.
 */
public interface IPlantable {
	EnumPlantType getPlantType(BlockGetter aWorld, int aX, int aY, int aZ);
	Block getPlant(BlockGetter aWorld, int aX, int aY, int aZ);
	int getPlantMetadata(BlockGetter aWorld, int aX, int aY, int aZ);
}
