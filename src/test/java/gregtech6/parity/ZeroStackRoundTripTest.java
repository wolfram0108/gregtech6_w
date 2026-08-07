/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregtech6.parity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import gregapi.util.ST;

/**
 * СУДЬЯ «ПРЕДМЕТ ИЗ НИЧЕГО» (BUG-077).
 *
 * <p>Вопрос: переживает ли цикл сохранение→загрузка «ноль с памятью типа»? В 1.7.10 это был стек с
 * {@code stackSize=0}: тип помнится, штук ноль. В neo нуля в {@code ItemStack} нет, поэтому GT6 хранит
 * его ZEROSIZE-призраком ({@code count=1} + компонент-маркер, центр {@code ST.size_}). Формат записи
 * {@code ST.save} — 1:1 с 1.7.10 ({@code id/Count/Damage/tag/od}) и про компоненты не знает, из-за чего
 * призрак сохранялся как {@code Count=1} и после перезахода оживал НАСТОЯЩИМ предметом.</p>
 *
 * <p><b>Контроли:</b> обычные стеки (1 и 64) обязаны пройти круг без изменений — иначе судья ловит не
 * дефект, а собственную поломку сериализации.</p>
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ZeroStackRoundTripTest {

    private static String describe(ItemStack aStack) {
        if (aStack == null) return "null";
        return ST.id(aStack) + " физич.count=" + aStack.getCount() + " логич.ST.count=" + ST.count(aStack);
    }

    @Test
    void zeroStackSurvivesSaveLoad(MinecraftServer aServer) {
        int tFail = 0;

        // 1. ЗЕРО-ПРИЗРАК: тип помнится, штук ноль
        ItemStack tGhost = ST.size_(0, new ItemStack(Items.IRON_INGOT));
        CompoundTag tNBT = ST.save(tGhost);
        ItemStack tBack = ST.load(tNBT);
        System.out.println("[zero] призрак ДО   : " + describe(tGhost));
        System.out.println("[zero] в NBT        : Count=" + tNBT.getIntOr("Count", -999));
        System.out.println("[zero] призрак ПОСЛЕ: " + describe(tBack));
        if (ST.count(tBack) != 0) {tFail++; System.out.println("[zero] ⛔ FAIL: из нуля родилось " + ST.count(tBack) + " шт.");}
        else System.out.println("[zero] ✅ ноль остался нулём");

        // 2. КОНТРОЛЬ: обычный одиночный стек
        ItemStack tOne = new ItemStack(Items.IRON_INGOT);
        ItemStack tOneBack = ST.load(ST.save(tOne));
        System.out.println("[zero] контроль 1шт : " + describe(tOneBack));
        if (ST.count(tOneBack) != 1) {tFail++; System.out.println("[zero] ⛔ FAIL: контроль 1шт сломан");}

        // 3. КОНТРОЛЬ: полный стек
        ItemStack tFull = new ItemStack(Items.IRON_INGOT, 64);
        ItemStack tFullBack = ST.load(ST.save(tFull));
        System.out.println("[zero] контроль 64шт: " + describe(tFullBack));
        if (ST.count(tFullBack) != 64) {tFail++; System.out.println("[zero] ⛔ FAIL: контроль 64шт сломан");}

        // 4. Двойной круг — призрак не должен «накапливать» единицы при каждом перезаходе
        ItemStack tTwice = ST.load(ST.save(ST.load(ST.save(ST.size_(0, new ItemStack(Items.DIAMOND))))));
        System.out.println("[zero] два круга    : " + describe(tTwice));
        if (ST.count(tTwice) != 0) {tFail++; System.out.println("[zero] ⛔ FAIL: за два круга накопилось " + ST.count(tTwice));}

        System.out.println("[zero] ИТОГ: провалов " + tFail);
        org.junit.jupiter.api.Assertions.assertEquals(0, tFail, "цикл save→load рождает предметы из нуля");
    }
}
