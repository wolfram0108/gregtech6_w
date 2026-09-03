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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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

	// F-useOn: in 1.7.10 block placement entered through ItemBlock.onItemUse, which GregTech overrode; here
	// the entry point is Item.useOn(UseOnContext) and onItemUse is never called, so the unpacking of the
	// context into the 1.7.10 parameters lives in one place below and every item root delegates to it.
	default boolean onItemUse     (ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return false;}
	default boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return false;}
	/** Whether this stack answers a block click at all - asked on the client, where the action itself cannot run. */
	default boolean handlesUseOnFirst(ItemStack aStack) {return false;}

	/** Распаковка neo UseOnContext → 1.7.10-параметры. side = Direction.get3DDataValue() (DOWN0/UP1/NORTH2/SOUTH3/WEST4/
	 *  EAST5 — 1:1 с ForgeDirection ordinal, тем же индексом уже пользуются OFFX/FORGE_DIR в GT6); hit — относительно
	 *  кликнутого блока (0..1). Единственное место разбора контекста, переиспользуется всеми корнями. */
	static InteractionResult bridgeUseOn(IItemGT aSelf, UseOnContext aCtx) {
		BlockPos p = aCtx.getClickedPos(); Vec3 h = aCtx.getClickLocation();
		return aSelf.onItemUse(aCtx.getItemInHand(), aCtx.getPlayer(), aCtx.getLevel(), p.getX(), p.getY(), p.getZ(), aCtx.getClickedFace().get3DDataValue(), (float)(h.x-p.getX()), (float)(h.y-p.getY()), (float)(h.z-p.getZ())) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}
	static InteractionResult bridgeUseOnFirst(IItemGT aSelf, UseOnContext aCtx) {
		BlockPos p = aCtx.getClickedPos(); Vec3 h = aCtx.getClickLocation();
		if (aSelf.onItemUseFirst(aCtx.getItemInHand(), aCtx.getPlayer(), aCtx.getLevel(), p.getX(), p.getY(), p.getZ(), aCtx.getClickedFace().get3DDataValue(), (float)(h.x-p.getX()), (float)(h.y-p.getY()), (float)(h.z-p.getZ()))) return InteractionResult.SUCCESS;
		// The behaviours run server side, so the client would report PASS and then repeat the click with the
		// other hand - an empty hand undoes the server result (a candle lit and blown out in one click).
		if (aCtx.getLevel().isClientSide() && aSelf.handlesUseOnFirst(aCtx.getItemInHand())) return InteractionResult.CONSUME;
		return InteractionResult.PASS;
	}
}
