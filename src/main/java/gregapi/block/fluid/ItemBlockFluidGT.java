/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.block.fluid;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * BUG-066 — ПОДПИСЬ ЖИДКОСТНЫХ БЛОКОВ GT6 (репорт игрока: наведение на реку показывает
 * {@code item.gregtech.gt.block.river} вместо названия).
 *
 * <p><b>Что было потеряно.</b> В 1.7.10 имена GT6 попадали в ЖИВУЮ таблицу переводов движка:
 * {@code LanguageHandler.add} звал {@code LanguageRegistry.instance().injectLanguage("en_US", TEMPMAP)}
 * (оригинал {@code gregapi/lang/LanguageHandler.java:82}), поэтому их видел ЛЮБОЙ путь — и ванильный
 * тултип, и Waila. В neo такой таблицы-инъекции нет, и порт закрыл вопрос иначе: каждый предмет GT6
 * переопределяет {@code getName(ItemStack)} и берёт имя из своей таблицы ({@code LanguageHandler}).
 * Приём применён семь раз ({@code ItemBlockBase}, {@code PrefixBlockItem}, {@code MultiTileEntityItemInternal},
 * {@code ItemBase}, {@code PrefixItem}, {@code ItemFluidDisplay}, {@code ItemArmorBase}) — но жидкостные
 * блоки регистрировали ВАНИЛЬНЫЙ {@link BlockItem} ({@code BlockWaterlike:88}, {@code BlockBaseFluid:109}),
 * мимо моста: движок искал перевод по своему ключу, не находил и показывал сам ключ.
 *
 * <p><b>Почему отдельный класс, а не {@code ItemBlockBase}.</b> Тот требует блок типа {@code IBlockBase}
 * (17 методов подтипов/горючести/ПКМ), а жидкостные блоки — {@code IBlock}+{@code IItemGT} и всей этой
 * поверхности не имеют. Здесь достраивается ровно недостающее звено — имя, — и берётся оно из того же
 * места, что у самого блока: {@code getLocalizedName()} (общий у обеих иерархий:
 * {@code BlockBaseFluid:423}, {@code BlockWaterlike:280} — оба через центр {@code FL.name(mFluid, T)}).
 *
 * <p>Замер до фикса (проба {@code gt6nameprobe}, клиентская сторона, вызов {@code ItemStack.getHoverName}
 * — тот же, которым имя берут тултип и Jade): из 2261 подписи мода сырыми были 108, и ВСЕ видимые игроком
 * приходились на жидкостные блоки (река/океан/болото/геотермальная вода/нефти/газ).
 */
public class ItemBlockFluidGT extends BlockItem {
	public ItemBlockFluidGT(Block aBlock) {
		// F12-followup (item-split): id в Properties обязателен и производится из ключа уже зарегистрированного
		// блока — тот же приём, что в ItemBlockBase:51 (предмет конструируется на RegisterEvent<Item>, после блока).
		super(aBlock, new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aBlock))));
	}

	/** Имя — из таблицы GT6 (как у всех прочих предметов мода); если его там нет, поведение движка не меняем. */
	@Override
	public net.minecraft.network.chat.Component getName(ItemStack aStack) {
		String tName = nameOf(getBlock());
		return tName != null && !tName.isEmpty() ? net.minecraft.network.chat.Component.literal(tName) : super.getName(aStack);
	}

	/** Тот же источник, что у самого блока: {@code getLocalizedName()} обеих жидкостных иерархий. */
	public static String nameOf(Block aBlock) {
		try {
			if (aBlock instanceof BlockBaseFluid tFluid) return tFluid.getLocalizedName();
			if (aBlock instanceof gregtech.blocks.fluids.BlockWaterlike tWater) return tWater.getLocalizedName();
		} catch (Throwable e) {/* имя не должно ронять тултип */}
		return null;
	}
}
