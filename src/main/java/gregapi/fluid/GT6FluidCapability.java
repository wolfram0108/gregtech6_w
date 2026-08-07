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

package gregapi.fluid;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * MODCOMPAT-001 П2 — ВОЗВРАТ СТАНДАРТНОГО КАНАЛА ЖИДКОСТЕЙ БЛОКАМ (F5-capability).
 *
 * <p><b>Что было потеряно при портировании.</b> В 1.7.10 танки GT6 торчали наружу через СТАНДАРТНЫЙ
 * Forge-интерфейс: пять TE-иерархий объявляли {@code implements IFluidHandler}
 * ({@code tank/TileEntityBase08Barrel}, {@code connectors/MultiTileEntityPipeFluid},
 * {@code machines/MultiTileEntityBasicMachine}, {@code multiblocks/MultiTileEntityMultiBlockPart},
 * {@code tools/MultiTileEntityAdvancedCraftingTable}), и любой чужой мод — насос, труба, тултип-мод —
 * читал содержимое БЕЗ единой строчки кода про GT6. В neo сам интерфейс на BlockEntity больше ничего не
 * значит: наружу видно только то, что ЗАРЕГИСТРИРОВАНО как capability. Порт перенёс интерфейс дословно
 * (методы живут в {@code TileEntityBase01Root:809-817}), но регистрации не сделал — {@code grep} по
 * {@code RegisterCapabilitiesEvent} давал 0. Итог: снаружи танки GT6 не существуют.
 *
 * <p><b>Почему это одна точка, а не правка пяти классов.</b> Вся GT6-TE-иерархия живёт под ОДНИМ
 * {@code BlockEntityType} — {@code TileEntityBase01Root.MTE_TYPE} (общий placeholder-тип, F-tileentity-
 * construction). Значит и регистрация ровно одна, а какие танки показать — решает сам TE своим
 * side-aware {@code getFluidTanks(side)}, то есть тем же кодом, что и внутренний тракт. Ковер-оверрайды
 * ({@code TileEntityBase06Covers:375}) при этом соблюдаются сами собой.
 *
 * <p><b>Транзакционность взята готовая, не самодельная.</b> Каждый {@link FluidTankGT} уже умеет отдавать
 * корректный {@code ResourceHandler<FluidResource>} ({@link FluidTankGT#asResourceHandler}) со
 * snapshot/rollback-семантикой neo; несколько танков стороны склеиваются штатным
 * {@link CombinedResourceHandler}. Своей транзакционной логики здесь нет — только выбор танков.
 */
public class GT6FluidCapability {
	private GT6FluidCapability() {}

	/** Подписка на мод-шину — рядом с остальными центральными переходниками в {@code GT_API.init}. */
	public static void register(IEventBus aModBus) {
		aModBus.addListener(GT6FluidCapability::onRegisterCapabilities);
	}

	/**
	 * Регистрация идёт ПО БЛОКАМ, а не по {@code BlockEntityType}, и это не вкусовщина, а требование движка:
	 * {@code registerBlockEntity} раздаёт провайдер блокам из {@code BlockEntityType.validBlocks}, а
	 * {@code MTE_TYPE} создан с ПУСТЫМ набором ({@code TileEntityBase01Root.createType()}:
	 * {@code java.util.Set.<Block>of()}) — он общий placeholder-тип динамической GT6-иерархии. Замер это и
	 * показал: регистрация проходила, тип BE совпадал с {@code MTE_TYPE}, танк на месте — а капа всё равно
	 * отдавалась {@code null}. Поэтому перечисляем сами блоки: все, чей BlockEntity — GT6-корень.
	 */
	private static void onRegisterCapabilities(RegisterCapabilitiesEvent aEvent) {
		List<net.minecraft.world.level.block.Block> tBlocks = new ArrayList<>();
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock || tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) tBlocks.add(tBlock);
		}
		if (tBlocks.isEmpty()) {
			// Тихо пропустить нельзя: молчаливый пропуск = «танков снаружи нет» без единого следа в логе.
			gregapi.data.CS.ERR.println("GT6 F5-capability: MTE-блоков в реестре 0 — канал жидкостей НЕ зарегистрирован!");
			return;
		}
		aEvent.registerBlock(Capabilities.Fluid.BLOCK, GT6FluidCapability::handlerAt, tBlocks.toArray(new net.minecraft.world.level.block.Block[0]));
		gregapi.data.CS.OUT.println("GT6 F5-capability: канал жидкостей зарегистрирован для " + tBlocks.size() + " MTE-блоков (Capabilities.Fluid.BLOCK).");
	}

	/** Блок-вариант провайдера: BlockEntity движок передаёт сам (может быть null, если его ещё нет). */
	private static ResourceHandler<FluidResource> handlerAt(net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.block.entity.BlockEntity aBlockEntity, Direction aSide) {
		return aBlockEntity instanceof gregapi.tileentity.base.TileEntityBase01Root tRoot ? handlerOf(tRoot, aSide) : null;
	}

	/**
	 * Танки, видимые снаружи с данной стороны. {@code aSide == null} — sideless-запрос, родная
	 * GT6-конвенция {@code SIDE_ANY} (та же, что у унаследованных мостов в {@code TileEntityBase01Root}).
	 * {@code null} на выходе означает «капы здесь нет» — так neo и отличает блок без танков от пустого танка.
	 */
	private static ResourceHandler<FluidResource> handlerOf(gregapi.tileentity.base.TileEntityBase01Root aTileEntity, Direction aSide) {
		if (aTileEntity == null) return null;
		IFluidTank[] tTanks;
		try {
			tTanks = aTileEntity.getFluidTanksForCapability(aSide);
		} catch (Throwable e) {return null;} // логика конкретного TE не должна ронять чужой мод, который просто спросил капу
		if (tTanks == null || tTanks.length <= 0) return null;
		List<ResourceHandler<FluidResource>> rHandlers = new ArrayList<>(tTanks.length);
		for (IFluidTank tTank : tTanks) if (tTank instanceof FluidTankGT tGT) rHandlers.add(tGT.asResourceHandler());
		if (rHandlers.isEmpty()) return null;
		return rHandlers.size() == 1 ? rHandlers.get(0) : new CombinedResourceHandler<>(rHandlers);
	}
}
