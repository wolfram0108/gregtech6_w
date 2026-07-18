/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregapi.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Empty Interface flagging an Item as GregTech controlled Item. This essentially means that the Item is more sane and less crash-y.
 *
 * Setting an Item of this Type to Wildcard Metadata will not cause a Crash.
 * The Internal Name of the Item wont be displayed in the Tooltip.
 * It shows the Mod that a Material based GT Item originally came from.
 * The Item Iteration Loading Step skips over Items with this Interface.
 * Items with this Interface do not trigger visibility of Materials when registered to the OreDict.
 * Blocks can have this marker Interface too, since it is just an empty marker.
 */
public interface IItemGT {
	/** F12/F1: 1.7.10 Item.setMaxDamage/setHasSubtypes — runtime-мутаторы, удалены движком (neo: прочность через
	 *  Properties.durability при регистрации; подтипы через DataComponents = F1). Дефолт-no-op для GT-итемов, где
	 *  value 0 (=neo-дефолт прочности) и подтипы обсолетны; {@code ItemBase} ПЕРЕОПРЕДЕЛЯЕТ setMaxDamage со хранением
	 *  поля + override getMaxDamage(ItemStack) (реальная прочность инструментов сохранена). Возврат Object → ItemBase
	 *  covariant-возврат ItemBase. Маркер реализуется и блоками — для них методы безвредны (не вызываются). */
	default Object setMaxDamage(int aMaxDamage) {return this;}
	default Object setHasSubtypes(boolean aHasSubtypes) {return this;}

	// F-useOn (класс дефектов «канал движка сместился», как F-tick/F13-appendHoverText): в 1.7.10 точкой входа установки
	// блока был vanilla ItemBlock.onItemUse(stack,player,world,x,y,z,side,hitX,hitY,hitZ) — GT6 его переопределял. В neo
	// 26.1.2 точка входа — Item.useOn(UseOnContext) (для BlockItem → place → placeBlock → Level.setBlock), а onItemUse
	// движком НЕ вызывается вовсе → вся GT6-логика установки (BE-init MTE, NBT материала prefix, звук) была мертва.
	// Приём (централизация): распаковка UseOnContext → 1.7.10-параметры в ОДНОМ месте (ниже), корни-предметы
	// (ItemBlockBase / MultiTileEntityItemInternal / PrefixBlockItem — все реализуют IItemGT) переопределяют neo-useOn/
	// onItemUseFirst тонкой делегацией сюда. 1.7.10-контракт установки объявлен здесь (дефолт-no-op для прочих IItemGT-
	// маркеров, включая блоки — у них метод не вызывается); корни его реализуют своими существующими телами onItemUse.
	default boolean onItemUse     (ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return false;}
	default boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return false;}

	/** Распаковка neo UseOnContext → 1.7.10-параметры. side = Direction.get3DDataValue() (DOWN0/UP1/NORTH2/SOUTH3/WEST4/
	 *  EAST5 — 1:1 с ForgeDirection ordinal, тем же индексом уже пользуются OFFX/FORGE_DIR в GT6); hit — относительно
	 *  кликнутого блока (0..1). Единственное место разбора контекста, переиспользуется всеми корнями. */
	static InteractionResult bridgeUseOn(IItemGT aSelf, UseOnContext aCtx) {
		BlockPos p = aCtx.getClickedPos(); Vec3 h = aCtx.getClickLocation();
		return aSelf.onItemUse(aCtx.getItemInHand(), aCtx.getPlayer(), aCtx.getLevel(), p.getX(), p.getY(), p.getZ(), aCtx.getClickedFace().get3DDataValue(), (float)(h.x-p.getX()), (float)(h.y-p.getY()), (float)(h.z-p.getZ())) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}
	static InteractionResult bridgeUseOnFirst(IItemGT aSelf, UseOnContext aCtx) {
		BlockPos p = aCtx.getClickedPos(); Vec3 h = aCtx.getClickLocation();
		return aSelf.onItemUseFirst(aCtx.getItemInHand(), aCtx.getPlayer(), aCtx.getLevel(), p.getX(), p.getY(), p.getZ(), aCtx.getClickedFace().get3DDataValue(), (float)(h.x-p.getX()), (float)(h.y-p.getY()), (float)(h.z-p.getZ())) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}
}
