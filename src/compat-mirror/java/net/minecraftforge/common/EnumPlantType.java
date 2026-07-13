package net.minecraftforge.common;

/**
 * F10 compile-only shim. 1.7.10 Forge {@code net.minecraftforge.common.EnumPlantType} удалён в neo
 * (растения через теги/{@code net.neoforged.neoforge.common.util}). Значения 1:1 с 1.7.10 (референс).
 * GT6 использует как тип возврата {@code IPlantable.getPlantType} + значения (напр. {@code EnumPlantType.Cave}).
 */
public enum EnumPlantType {
	Plains, Desert, Beach, Cave, Water, Nether, Crop;
}
