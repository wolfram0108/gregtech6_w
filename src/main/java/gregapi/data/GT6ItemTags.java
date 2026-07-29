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

package gregapi.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;

import gregapi.item.IItemProjectile;

/**
 * F12-ammo — ПЕРЕНОС GT6-ДАННЫХ О БОЕПРИПАСЕ В ТЕГИ (тот же приём, что {@link GT6HarvestTags} для добычи).
 *
 * <p><b>Что изменил движок.</b> В 1.7.10 стрельба стрелой GT6 из ванильного лука держалась на порядке
 * вызовов: {@code ItemBow.onPlayerStoppedUsing} постил {@code ArrowLooseEvent} ПЕРВЫМ
 * ({@code gregtech6/build/tmp/recompSrc/net/minecraft/item/ItemBow.java:39-40}), ДО всякой проверки
 * боеприпаса, и GT6 перехватывал выстрел ({@code GT_API_Proxy.onArrowLooseEvent}), создавал свой снаряд и
 * отменял ванильную ветку. Наличие ванильной стрелы не требовалось — проверка {@code inventory.hasItem
 * (Items.arrow)} стояла ПОСЛЕ события ({@code :49}).
 *
 * <p>В NeoForge 26.1.2 порядок обратный: {@code BowItem.releaseUsing} сначала спрашивает
 * {@code player.getProjectile(лук)} и при пустом результате выходит СРАЗУ
 * ({@code neo-decompiled/net/minecraft/world/item/BowItem.java:30-33}) — событие выстрела не постится
 * вовсе. А отбор боеприпаса там — {@code ARROW_ONLY = itemStack.is(ItemTags.ARROWS)}
 * ({@code ProjectileWeaponItem.java:20}), то есть чисто теговый.
 *
 * <p><b>Следствие для порта.</b> Стрелы GT6 в ванильном теге не значились, поэтому лук их не видел
 * боеприпасом — и вся стрельба стрелами GT6 была мертва молча (замер: {@code getProjectile -> ПУСТО} при
 * зелёном контроле на ванильной стреле). Это не «чары не применяются», это отсутствующее предусловие.
 *
 * <p><b>Перенос, а не список.</b> Кто является стрелой, знает сам предмет — контракт
 * {@link IItemProjectile#hasProjectile}, тот же, которым его спрашивает мод при выстреле
 * ({@code GT_API_Proxy.onArrowLooseEvent} через {@code ST.projectile}). Провайдер обходит реестр и
 * спрашивает КОНТРАКТ, поэтому новая стрела попадёт в тег сама, без правки данных — сохраняется свойство
 * GT6 порождать содержимое процедурно.
 *
 * <p>Пули ({@code TD.Projectiles.BULLET_*}) сюда НЕ попадают: они и в 1.7.10 из лука не стрелялись —
 * {@code onArrowLooseEvent} искал строго {@code TD.Projectiles.ARROW}.
 */
public class GT6ItemTags extends TagsProvider<Item> {
	public GT6ItemTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookup) {
		super(aOutput, Registries.ITEM, aLookup, CS.ModIDs.GT);
	}

	@Override
	public String getName() {return "GT6 Item Tags (ammo)";}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		int tArrows = 0;
		for (Item tItem : BuiltInRegistries.ITEM) {
			if (!(tItem instanceof IItemProjectile tProjectile)) continue;
			net.minecraft.resources.Identifier tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID == null || !(tID.getNamespace().equals(CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			if (!isArrow(tProjectile, tID)) continue;
			getOrCreateRawBuilder(ItemTags.ARROWS).addElement(tID);
			tArrows++;
		}
		System.out.println("[GT6-DATAGEN] стрел GT6 внесено в minecraft:arrows: " + tArrows);
	}

	/** Спрашиваем у предмета ЕГО ТИП СНАРЯДА, а не «годен ли конкретный стек».
	 *  <p>⛔ На фазе датагена {@code ItemStack} создавать НЕЛЬЗЯ: {@code Holder.Reference.components}
	 *  ещё не привязан и любой {@code ST.make} падает «Components not bound yet» (то же ограничение записано
	 *  в {@code build.gradle:213}; первый вариант этого провайдера ронял {@code runData} именно так).
	 *  Поэтому отбор идёт по полю, заданному в конструкторе предмета ({@code PrefixItemProjectile.java:68}) —
	 *  оно от фазы не зависит. Носитель контракта вне этой иерархии не потеряется молча: он попадёт в
	 *  предупреждение ниже, и решение по нему принимает человек, а не тихий пропуск. */
	private static boolean isArrow(IItemProjectile aProjectile, net.minecraft.resources.Identifier aID) {
		if (aProjectile instanceof gregapi.item.prefixitem.PrefixItemProjectile tPrefix)
			return tPrefix.mProjectileType == TD.Projectiles.ARROW;
		// Второй носитель контракта — мультипредмет: у него «стрела ли это» решают ПОВЕДЕНИЯ
		// (MultiItem.java:103-106). Спрашиваем реестр поведений напрямую, без создания стека:
		// Behavior_Arrow.hasProjectile на стек не смотрит вовсе (Behavior_Arrow.java:114-115).
		if (aProjectile instanceof gregapi.item.multiitem.MultiItem tMulti) {
			for (java.util.ArrayList<gregapi.item.multiitem.behaviors.IBehavior<gregapi.item.multiitem.MultiItem>> tList : tMulti.mItemBehaviors.values())
				for (gregapi.item.multiitem.behaviors.IBehavior<gregapi.item.multiitem.MultiItem> tBehavior : tList) {
					try {
						if (tBehavior.hasProjectile(tMulti, TD.Projectiles.ARROW, null)) return true;
					} catch (Throwable e) {/* поведение смотрит на стек — на этой фазе не спросить, оно и не стрела */}
				}
			return false;
		}
		System.out.println("[GT6-DATAGEN] ⚠ носитель IItemProjectile вне известных иерархий: " + aID
			+ " — тип снаряда на фазе датагена не спросить (стек создавать нельзя), в minecraft:arrows НЕ внесён");
		return false;
	}
}
