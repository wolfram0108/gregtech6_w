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

package gregtech6;

import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import gregapi.network.NetworkHandler;
import org.slf4j.Logger;

/**
 * Точка входа мода с modId {@code "gregtech6"} — совпадает с объявленным в
 * {@code src/main/templates/META-INF/neoforge.mods.toml} ({@code ${mod_id}}) и используется тестом
 * {@code gregtech6.SanityTest} (обращается к {@link #MODID}). Поэтому класс СОХРАНЁН.
 *
 * <p><b>F12/R3.</b> ADR {@code decisions/F12-registration-lifecycle.md:79} предписывает удалить этот
 * временный toolchain-bring-up-скелет, раз реальным neo-{@code @Mod} стал {@code gregapi.GT_API}.
 * Однако полное удаление класса сломало бы mod-точку-входа (declared modId {@code gregtech6} в
 * mods.toml) и компиляцию {@code SanityTest}. Поэтому применён вариант ADR «оставить класс, но убрать
 * из него регистрацию в обход центра»: удалены собственные {@code DeferredRegister.Blocks/Items},
 * тестовые {@code TEST_BLOCK}/{@code TEST_BLOCK_ITEM} и тестовый {@code CreativeModeTab} — это был
 * артефакт проверки сборки, не из архитектуры Грегориуса.</p>
 *
 * <p><b>Централизация (F12).</b> Регистрация контента теперь идёт ТОЛЬКО через центры:
 * {@code gregapi.GT_API} (Item/Block, F12) и {@code gregapi.fluid.FluidGT} (Fluid, F5). Здесь ничего
 * не регистрируется. Единственная оставшаяся привязка к мод-шине — подписка neo-payload'ов сети
 * (F7, {@code NetworkHandler::registerPayloadHandlers}), это не R3-регистрация контента.</p>
 *
 * <p>Осиротевший ассет {@code block.gregtech6.test_block} в
 * {@code assets/gregtech6/lang/en_us.json} остаётся безвредным (неиспользуемый ключ локализации);
 * ресурсы — вне scope F12-кода.</p>
 */
// F12 mod-структура (boot работает): временный @Mod-носитель modId gregtech6; удалить, когда
// GT6_Main станет реальным @Mod(GT) — decisions/F12-registration-lifecycle.md §4. Причина отложенности:
// neoforge.mods.toml требует живой entrypoint УЖЕ на этапе сборки, а законный владелец modId (мод GT,
// GT6_Main) переводится на neo-@Mod только в порту контента (контент-824, отложен по §4.1).
@Mod(GregTech6.MODID)
public class GregTech6 {
    public static final String MODID = "gregtech6";

    public static final Logger LOGGER = LogUtils.getLogger();

    public GregTech6(IEventBus modEventBus) {
        // F7 (сеть): регистрация neo-payload'ов — не R3-регистрация контента, оставлена как есть.
        modEventBus.addListener(NetworkHandler::registerPayloadHandlers);
        LOGGER.info("[GregTech6] entrypoint loaded — content registration centralised in GT_API (F12) / FluidGT (F5)");
    }
}
