package mods.railcraft.common.items.enchantment;

import net.minecraft.world.item.enchantment.Enchantment;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Минимум для компиляции ядра; члены добираются
 *  компилятором. Реальная зависимость — при возврате к интеграции. См. compat-mirror/README.md.
 *  Поля добраны компилятором при F8 (gregapi.util.UT.NBT.getEnchantmentLevelDestruction/Wrecking/
 *  Implosion): все обращения к ним в ядре гейтятся `MD.RC.mLoaded` (Railcraft не загружен без
 *  реальной зависимости) — `null` безопасен рантайм-инвариантом README (внешние пути мертвы). */
public interface RailcraftEnchantments {
	Enchantment destruction = null;
	Enchantment wrecking    = null;
	Enchantment implosion   = null;
}
