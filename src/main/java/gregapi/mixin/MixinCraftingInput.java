/**
 * Copyright (c) 2026 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * BUG-099 — ЕДИНСТВЕННАЯ вставка GT6 в код движка (F2: замена coremod-трансформеров Грегориуса, приём тот же —
 * из движка вызывается статический метод мода, вся логика остаётся в моде).
 *
 * <p>Зачем. Часть крафта GT6 несёт смысл В САМОМ МЕСТЕ предмета на сетке: {@code AdvancedCrafting1ToY}
 * различает варианты по числу ПУСТЫХ клеток перед предметом, и один и тот же предмет в разных клетках даёт
 * разный продукт (16-жильный провод → 8-жильные/4-жильные/2-жильные; дроблёная руда → надтреснутый самоцвет;
 * пыль → четвертинки). В 1.7.10 рецепт видел {@code InventoryCrafting} целиком, поэтому признак был осмыслен.
 * В 26.1.2 сетка приходит в рецепт УЖЕ подрезанной до габарита занятых клеток ({@code ofPositioned} ниже:
 * {@code newWidth = right-left+1}), и одиночный предмет всегда становится сеткой 1×1 — пустых клеток нет,
 * позиция потеряна ДО вызова рецепта. Замер {@code gt6gridprobe}: недостижимо 27 вариантов из 119.</p>
 *
 * <p>Как. {@code ofPositioned} — единственное место, через которое проходит ЛЮБАЯ сборка {@link CraftingInput}
 * (верстак игрока, автокрафт, чужие моды: {@code CraftingInput.of} делегирует сюда же, а
 * {@code CraftingContainer.asCraftInput} — в {@code of}). Здесь полная сетка ещё цела: запоминаем её
 * в центре GT6 ({@code CR.rememberFullGrid}) ровно на время текущего вызова, а рецепт при сопоставлении
 * спрашивает у того же центра. Движок при этом работает как работал — ничего не подменяется и не отменяется.</p>
 */
@Mixin(CraftingInput.class)
public class MixinCraftingInput {
	@Inject(method = "ofPositioned", at = @At("RETURN"))
	private static void gt6$rememberFullGrid(int aWidth, int aHeight, List<ItemStack> aItems, CallbackInfoReturnable<CraftingInput.Positioned> aCallback) {
		CraftingInput.Positioned tPositioned = aCallback.getReturnValue();
		if (tPositioned != null) gregapi.util.CR.rememberFullGrid(tPositioned.input(), aWidth, aHeight, aItems);
	}
}
