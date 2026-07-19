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

package gregapi;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import gregapi.api.Abstract_Mod;
import gregapi.block.IBlockBase;
import gregapi.block.ToolCompat;
import gregapi.block.metatype.BlockMetaType;
import gregapi.block.multitileentity.MultiTileEntityBlockInternal;
import gregapi.block.prefixblock.PrefixBlockFallingEntity;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.code.ObjectStack;
import gregapi.cover.CoverRegistry;
import gregapi.cover.ICover;
import gregapi.data.*;
import gregapi.item.ItemFluidDisplay;
import gregapi.old.Textures;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.listeners.IOreDictListenerItem;
import gregapi.recipes.AdvancedCrafting1ToY;
import gregapi.recipes.AdvancedCraftingXToY;
import gregapi.render.*;
import gregapi.tileentity.render.ITileEntityOnDrawBlockHighlight;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.minecraft.network.chat.Component;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class GT_API_Proxy_Client extends GT_API_Proxy {
	
	public GT_API_Proxy_Client() {
		super();
		CODE_SERVER = T;
		CODE_CLIENT = T;
		CODE_UNCHECKED = F;
		
		for (int i = 0; i < 4; i++) {
			sPosR.addAll(Arrays.asList(MT.ChargedCertusQuartz.mRGBa[i], MT.Enderium.mRGBa[i], MT.Vinteum.mRGBa[i], MT.U_235.mRGBa[i], MT.Am_241.mRGBa[i], MT.Am_242.mRGBa[i], MT.Pu_241.mRGBa[i], MT.Pu_243.mRGBa[i], MT.Nq_528.mRGBa[i], MT.Nq_522.mRGBa[i], MT.InfusedOrder.mRGBa[i], MT.Force.mRGBa[i], MT.Pyrotheum.mRGBa[i], MT.Sunnarium.mRGBa[i], MT.Mcg.mRGBa[i], MT.Thaumium.mRGBa[i], MT.InfusedVis.mRGBa[i], MT.InfusedAir.mRGBa[i], MT.InfusedFire.mRGBa[i], MT.FierySteel.mRGBa[i], MT.Fireleaf.mRGBa[i], MT.Firestone.mRGBa[i], MT.ArcaneAsh.mRGBa[i]));
			sPosG.addAll(Arrays.asList(MT.ChargedCertusQuartz.mRGBa[i], MT.Enderium.mRGBa[i], MT.Vinteum.mRGBa[i], MT.U_235.mRGBa[i], MT.Am_241.mRGBa[i], MT.Am_242.mRGBa[i], MT.Pu_241.mRGBa[i], MT.Pu_243.mRGBa[i], MT.Nq_528.mRGBa[i], MT.Nq_522.mRGBa[i], MT.InfusedOrder.mRGBa[i], MT.Force.mRGBa[i], MT.Pyrotheum.mRGBa[i], MT.Sunnarium.mRGBa[i], MT.InfusedAir.mRGBa[i], MT.InfusedEarth.mRGBa[i]));
			sPosB.addAll(Arrays.asList(MT.ChargedCertusQuartz.mRGBa[i], MT.Enderium.mRGBa[i], MT.Vinteum.mRGBa[i], MT.U_235.mRGBa[i], MT.Am_241.mRGBa[i], MT.Am_242.mRGBa[i], MT.Pu_241.mRGBa[i], MT.Pu_243.mRGBa[i], MT.Nq_528.mRGBa[i], MT.Nq_522.mRGBa[i], MT.InfusedOrder.mRGBa[i], MT.Mcg.mRGBa[i], MT.InfusedVis.mRGBa[i], MT.InfusedWater.mRGBa[i], MT.Thaumium.mRGBa[i], MT.Co_60.mRGBa[i], MT.Lumium.mRGBa[i], MT.VinteumPurified.mRGBa[i], MT.ArcaneAsh.mRGBa[i]));
			sNegR.addAll(Arrays.asList(MT.InfusedEntropy.mRGBa[i], MT.NetherStar.mRGBa[i]));
			sNegG.addAll(Arrays.asList(MT.InfusedEntropy.mRGBa[i], MT.NetherStar.mRGBa[i]));
			sNegB.addAll(Arrays.asList(MT.InfusedEntropy.mRGBa[i], MT.NetherStar.mRGBa[i]));
			sRainbow.addAll(Arrays.asList(MT.GaiaSpirit.mRGBa[i], MT.GaiaSpirit.mRGBa[i], MT.Shimmerwood.mRGBa[i], MT.Shimmerwood.mRGBa[i], MT.Chimerite.mRGBa[i]));
			sRainbowFast.addAll(Arrays.asList(MT.Infinity.mRGBa[i], MT.InfusedBalance.mRGBa[i]));
		}
	}
	
	@Override
	public int addArmor(String aPrefix) {
		try {return RenderingRegistry.addNewArmourRendererPrefix(aPrefix);} catch(Throwable e) {/**/}
		return 0;
	}

	// F3-render (client): регистрация единого динамического типа модели всех GT6-блоков на mod-bus.
	// Замена удалённого `RenderingRegistry.registerBlockHandler`/render-id диспетчера (decisions/F3-render.md §2.1):
	// один `GT6BlockModel` тип. Две точки: (1) RegisterBlockStateModels — тип для blockstate-JSON (fallback);
	// (2) ModifyBakingResult — рантайм-инъекция модели ВСЕМ GT6-блокам (IRenderedBlock) БЕЗ JSON — процедурный
	// мод (сотни блоков динамически) не может держать тысячи статичных JSON; централизация 1:1 (одна модель на весь мод).
	@Override
	public void registerClientModels(net.neoforged.bus.api.IEventBus aModBus) {
		aModBus.addListener(this::onRegisterBlockStateModels);
		aModBus.addListener(this::onModifyBakingResult);
		aModBus.addListener(this::onRegisterFluidModels);
		aModBus.addListener(this::onRegisterBlockEntityRenderers);
		aModBus.addListener(this::onRegisterMenuScreens);
	}

	// F14-gui: КЛИЕНТ-регистрация экрана для ContainerCommon.MENU_TYPE (без неё neo падает при открытии любого GUI мода —
	// «no screen for menu type»). Фабрика маршрутизирует в ЕДИНЫЙ GT6-центр getGUIClient (тот же, что строил экран в
	// 1.7.10 — per-machine ContainerClient-подкласс+текстура); fallback (getGUIClient=null/исключение) — обёртка
	// neo-реконструированного menu базовым ContainerClient (без краша).
	private void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent aEvent) {
		if (gregapi.gui.ContainerCommon.MENU_TYPE == null) return;
		aEvent.<gregapi.gui.ContainerCommon, gregapi.gui.ContainerClient>register(gregapi.gui.ContainerCommon.MENU_TYPE.get(), (aMenu, aInv, aTitle) -> {
			// исключение отсюда = дисконнект (neoforge ClientPayloadHandler.createMenuScreen catch→disconnect) → тотальный null-гейт
			if (aMenu == null) aMenu = new gregapi.gui.ContainerCommon(0, aInv);
			// containerId-мост (корень «слот есть на сервере, но не отображается»): getGUIClient строит СВЕЖИЙ
			// клиент-контейнер легаси-конструктором (id из sPendingWindowID); вне withWindowID тот равен -1 →
			// id клиента ≠ id сервера → ВСЕ пакеты контента/слотов меню молча дропаются клиентом (проверка id в
			// handleContainerSetSlot/Content). Оборачиваем фабрику мостом с id сетевого меню (= серверный id).
			final gregapi.gui.ContainerCommon fMenu = aMenu;
			try { if (aMenu.mTileEntity instanceof gregapi.tileentity.ITileEntityGUI tGUI) { Object tScreen = gregapi.gui.ContainerCommon.withWindowID(fMenu.containerId, () -> tGUI.getGUIClient(fMenu.mGUIID, aInv.player)); if (tScreen instanceof gregapi.gui.ContainerClient tCC) return tCC; } }
			catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-GUI] getGUIClient упал, fallback-экран: "+e); }
			return new gregapi.gui.ContainerClient(aMenu, gregapi.data.CS.RES_PATH_GUI + "chests/" + (aMenu.mTileEntity == null ? 1 : aMenu.mTileEntity.getSizeInventoryGUI()) + ".png");
		});
		gregapi.data.CS.OUT.println("[GT6-GUI] MenuScreens: экран для ContainerCommon.MENU_TYPE зарегистрирован (F14).");
	}

	// F3-render: MTE-блоки рисует BER (не baked — регион не отдаёт MTE-BE, см. MultiTileEntityBER). Один generic BER на весь
	// MTE_TYPE (централизация 1:1). Руды/стабы отсеиваются внутри BER (гейт MultiTileEntityBlock+IRenderedBlockObject).
	private void onRegisterBlockEntityRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers aEvent) {
		if (gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE != null)
			aEvent.registerBlockEntityRenderer(gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE, gregapi.render.MultiTileEntityBER::new);
	}

	// Приёмочный скан рендера (гейт ②): на первом client-tick, когда атлас стежен И DataComponents ПРИВЯЗАНЫ (на
	// ModelEvent.BakingCompleted они ещё не bound → Item.getDefaultInstance NPE «Components not bound yet»). Проверяем,
	// что item-иконки GT6 резолвятся (не пурпур). Once. Пишет found/missing в gregtech.log (game-bus, авто-регистр).
	private boolean mIconsProbed = false;
	@net.neoforged.bus.api.SubscribeEvent
	public void onClientTickProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (!new java.io.File("gt6probe.flag").exists()) return; // debug визуал-паритета — по умолчанию ВЫКЛ (гейт флагом)
		if (mIconsProbed) return;
		// Ждём ЗАГРУЖЕННЫЙ МИР: DataComponents предметов привязываются на world-load, в меню ItemStack/getDefaultInstance
		// NPE-ит («Components not bound yet»). При входе игрока в мир (level!=null) components готовы, атлас стежен → probe истинный.
		if (Minecraft.getInstance().level == null) return;
		mIconsProbed = true;
		try { gregapi.render.GT6ItemModel.probeItemIcons(); } catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-RENDER-PROBE] скан упал: " + e); }
		// ВИЗУАЛ-ПАРИТЕТ: ПОРТ-СТОРОНА компаратора. Полный порт-дескриптор ВСЕХ предметов (спрайт+тинт per pass) на РЕАЛЬНОМ
		// рендер-пути (тем же кодом getIcon/itemColor, что рисует update). СИММЕТРИЧЕН golden-дескриптору оригинала 1.7.10
		// (оракул client, DumpRenderItems: getIcon().getIconName()+getColorFromItemStack per pass). Компаратор порт↔golden
		// → visual-parity % + MISMATCH. Под флагом gt6inject.flag.
		if (new java.io.File("gt6inject.flag").exists()) {
			try { gregapi.render.GT6ItemModel.dumpItemDescriptors(); } catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-VP] полный дамп упал: " + e); }
		}
	}

	// КАНДИДАТ-ИНЖЕКТОР (визуал-паритет Ф2): под флагом run/gt6inject.flag СИНТЕЗИРУЕТ процедурные предметы-кандидаты
	// (кирка Vibranium + Adamantium) прямо в инвентарь игрока при входе в мир (интегро-сервер) + дампит их render-дескриптор.
	// Не крафт, не креатив-список — прямой sMetaTool.getToolWithStats (тот же стек, что дал бы крафт: слои+тинт+зачар). Once, gated.
	private boolean mCandidateInjected = false;
	@net.neoforged.bus.api.SubscribeEvent
	public void onCandidateInject(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mCandidateInjected) return;
		if (!new java.io.File("gt6inject.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		net.minecraft.server.MinecraftServer tSrv = tMC.getSingleplayerServer();
		if (tSrv == null || tSrv.getPlayerList().getPlayers().isEmpty()) return;
		mCandidateInjected = true;
		try {
			net.minecraft.server.level.ServerPlayer tPlayer = tSrv.getPlayerList().getPlayers().get(0);
			gregapi.item.multiitem.MultiItemTool tMeta = gregapi.data.CS.ToolsGT.sMetaTool;
			java.util.List<net.minecraft.world.item.ItemStack> tCands = new java.util.ArrayList<>();
			if (tMeta != null) {
				net.minecraft.world.item.ItemStack tVib = tMeta.getToolWithStats(gregapi.data.CS.ToolsGT.CONSTRUCTION_PICK, gregapi.data.MT.Vb, gregapi.data.MT.WOODS.Spruce);
				net.minecraft.world.item.ItemStack tAda = tMeta.getToolWithStats(gregapi.data.CS.ToolsGT.CONSTRUCTION_PICK, gregapi.data.MT.Ad, gregapi.data.MT.WOODS.Spruce);
				if (gregapi.util.ST.valid(tVib)) tCands.add(tVib);
				if (gregapi.util.ST.valid(tAda)) tCands.add(tAda);
			}
			for (net.minecraft.world.item.ItemStack tS : tCands) tPlayer.getInventory().add(tS.copy());
			// A/B-эталон: ванильные предметы рендерятся ВАНИЛЬНЫМ путём (не GT6ItemModel) → сравнить яркость/цвет с GT6-кирками (разводит «GT6-рендер сломан» vs «глобально»)
			tPlayer.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
			tPlayer.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
			// BLOCK-item кандидаты (путь renderBlockInventory): первые GT6-блок-предметы (IRenderedBlock) + ванильные блоки STONE/FURNACE
			// для A/B изометрии — проверка block-GUI трансформации (3D-куб vs плоская тёмная грань).
			int tBlkAdded = 0;
			for (net.minecraft.world.item.Item tItm : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
				if (tBlkAdded >= 4) break;
				net.minecraft.resources.Identifier tK = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItm);
				if (tK == null || !isGregNamespace(tK.getNamespace())) continue;
				if (tItm instanceof net.minecraft.world.item.BlockItem tBI2 && tBI2.getBlock() instanceof gregapi.render.IRenderedBlock) {
					tPlayer.getInventory().add(new net.minecraft.world.item.ItemStack(tItm));
					gregapi.data.CS.OUT.println("[GT6-INJECT] block-item кандидат: " + tK);
					tBlkAdded++;
				}
			}
			tPlayer.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.STONE));
			tPlayer.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.FURNACE));
			gregapi.data.CS.OUT.println("[GT6-INJECT] синтезировано в инвентарь: " + tCands.size() + " (Vibranium+Adamantium кирки) + " + tBlkAdded + " GT6-блок-предметов + ванильные iron_pickaxe/stick/stone/furnace для A/B");
			gregapi.render.GT6ItemModel.dumpStacks(tCands, "descriptor.port.candidate.jsonl");
			try { gregapi.item.CreativeTabsGT.probeOwnTabs(); } catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-F16-PROBE] упал: " + e); }
			// MACHINE-проба (имя/текстура/тултип item-формы машин): по 1 стеку из вкладок Chests(32745)/SteamBoilers(1204)/
			// BasicMachines(20001) — hover-имя, число тултип-строк, render-дескриптор (спрайты block-пути) в дамп.
			try {
				gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
				if (tReg != null) {
					java.util.List<net.minecraft.world.item.ItemStack> tMachines = new java.util.ArrayList<>();
					short[] tWantTabs = {32745, 1204, 20001};
					for (short tTab : tWantTabs) for (gregapi.block.multitileentity.MultiTileEntityClassContainer tC : tReg.mRegistrations) {
						if (tC.mCreativeTabID == tTab && !tC.mHidden) {
							net.minecraft.world.item.ItemStack tS = tReg.getItem(tC.mID);
							if (gregapi.util.ST.valid(tS)) {
								tMachines.add(tS);
								String tName = "?"; try { tName = tS.getHoverName().getString(); } catch (Throwable e) { tName = "EXC:" + e.getClass().getSimpleName(); }
								int tTips = -1; try { tTips = tS.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, null, net.minecraft.world.item.TooltipFlag.NORMAL).size(); } catch (Throwable e) {}
								gregapi.data.CS.OUT.println("[GT6-MACHINE-PROBE] tab=" + tTab + " id=" + tC.mID + " class=" + tC.mClass.getSimpleName() + " name=\"" + tName + "\" tooltipLines=" + tTips);
							}
							break;
						}
					}
					for (net.minecraft.world.item.ItemStack tS : tMachines) tPlayer.getInventory().add(tS.copy());
					gregapi.render.GT6ItemModel.dumpStacks(tMachines, "descriptor.port.machines.jsonl");
					// П9-замер «машины без текстур»: item-форма ВСЕХ MTE — счёт пустых (ни блок-квадов, ни спец-рендера) по классам
					try {
						java.util.Map<String,int[]> tByCls = new java.util.TreeMap<>();
						int tEmptyAll = 0, tTotalAll = 0;
						for (gregapi.block.multitileentity.MultiTileEntityClassContainer tC : tReg.mRegistrations) {
							if (tC.mHidden) continue;
							net.minecraft.world.item.ItemStack tS = tReg.getItem(tC.mID);
							if (!gregapi.util.ST.valid(tS)) continue;
							tTotalAll++;
							boolean tEmpty;
							try {
								gregapi.render.GT6QuadBuilder tQB = new gregapi.render.GT6QuadBuilder();
								gregapi.render.GT6BlockModel.buildInventoryQuads(tQB, tReg.mBlock, tS);
								tEmpty = tQB.quads().isEmpty() && gregapi.render.MultiTileEntityBER.SPECIAL_ITEM_FORM.extractArgument(tS) == null;
							} catch (Throwable e) { tEmpty = true; }
							int[] tA = tByCls.computeIfAbsent(tC.mClass.getSimpleName(), k -> new int[2]);
							tA[0]++; if (tEmpty) { tA[1]++; tEmptyAll++; }
						}
						gregapi.data.CS.OUT.println("[GT6-MACHINE-FORM] item-формы MTE: всего=" + tTotalAll + " ПУСТЫХ=" + tEmptyAll);
						tByCls.entrySet().stream().filter(e2 -> e2.getValue()[1] > 0).limit(40).forEach(e2 ->
							gregapi.data.CS.OUT.println("[GT6-MACHINE-FORM]   " + e2.getKey() + ": " + e2.getValue()[1] + "/" + e2.getValue()[0] + " пустых"));
						gregapi.data.CS.OUT.println("[GT6-MACHINE-FORM] MISSING-спрайтов (грань молча пропущена): " + gregapi.render.GT6QuadBuilder.sMissingSprites.size());
						int tMs = 0; for (String tMiss : gregapi.render.GT6QuadBuilder.sMissingSprites) { if (++tMs > 30) break; gregapi.data.CS.OUT.println("[GT6-MACHINE-FORM]   MISSING " + tMiss); }
						// цвет вершин (тинт) первых квадов 3 машин: чёрный (ff000000) = корень «чёрные машины»
						for (int tID2 : new int[]{32745, 1204, 20001, 100, 1000}) try {
							net.minecraft.world.item.ItemStack tS2 = tReg.getItem(tID2);
							if (!gregapi.util.ST.valid(tS2)) continue;
							gregapi.render.GT6QuadBuilder tQB2 = new gregapi.render.GT6QuadBuilder();
							gregapi.render.GT6BlockModel.buildInventoryQuads(tQB2, tReg.mBlock, tS2);
							StringBuilder tCol = new StringBuilder();
							int tQn = 0;
							for (net.minecraft.client.resources.model.geometry.BakedQuad tQ : tQB2.quads()) {
								if (++tQn > 4) break;
								tCol.append(tQ.materialInfo().sprite().contents().name().getPath()).append("=").append(Integer.toHexString(tQ.bakedColors().color(0))).append(" ");
							}
							gregapi.data.CS.OUT.println("[GT6-MACHINE-FORM] id=" + tID2 + " quads=" + tQB2.quads().size() + " вершина-цвета: " + tCol);
						} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-MACHINE-FORM] цвет id=" + tID2 + " EXC " + e); }
					} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-MACHINE-FORM] упал: " + e); }
				}
			} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-MACHINE-PROBE] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-INJECT] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
	}

	// N1-СУДЬЯ (F-useOn установка, гейт: файл run/gt6placeprobe.flag): проверка, что блок мода встаёт РЕАЛЬНЫМ клик-путём.
	// НЕ прямой onItemUse (то мерило в обход движка), а ServerPlayerGameMode.useItemOn — ТОТ ЖЕ метод, что сервер зовёт
	// при ServerboundUseItemOnPacket (ServerGamePacketListenerImpl:1382). Для каждого корня-предмета (MTE машина/сундук,
	// PrefixBlockItem-руда, ItemBlockBase-камень): ставим гарантированный пол STONE, кликаем по его верху сеткой useItemOn,
	// читаем движком результат (блок мода встал? BE создан?). Успех = мост useOn→onItemUse жив на реальном пути.
	private int mPlaceProbePhase = 0; private int mPlaceProbeTick = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public void onPlaceProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mPlaceProbePhase >= 1) return;
		if (!new java.io.File("gt6placeprobe.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		net.minecraft.server.MinecraftServer tSrv = tMC.getSingleplayerServer();
		if (tSrv == null) { mPlaceProbePhase = 1; return; }
		if (++mPlaceProbeTick < 300) return;
		mPlaceProbePhase = 1;
		final java.io.PrintStream o = gregapi.data.CS.OUT;
		tSrv.execute(() -> { try {
			net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
			net.minecraft.server.level.ServerLevel tW = tP.level();
			gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
			// набор тестовых стеков: по одному каждого корня установки
			java.util.List<net.minecraft.world.item.ItemStack> tStacks = new java.util.ArrayList<>();
			java.util.List<String> tNames = new java.util.ArrayList<>();
			if (tReg != null) {
				net.minecraft.world.item.ItemStack tM = tReg.getItem(20001); if (gregapi.util.ST.valid(tM)) {tStacks.add(tM); tNames.add("MTE-машина#20001");}
				net.minecraft.world.item.ItemStack tC = tReg.getItem(32745); if (gregapi.util.ST.valid(tC)) {tStacks.add(tC); tNames.add("MTE-сундук#32745");}
			}
			net.minecraft.world.item.ItemStack tPrefix = null, tPlain = null;
			for (net.minecraft.world.item.Item it : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
				if (tPrefix == null && it instanceof gregapi.block.prefixblock.PrefixBlockItem) {
					java.util.List<net.minecraft.world.item.ItemStack> tV = new java.util.ArrayList<>();
					try { ((gregapi.block.prefixblock.PrefixBlockItem)it).getSubItems(it, null, tV); } catch (Throwable e) {/**/}
					if (!tV.isEmpty()) tPrefix = tV.get(0);
				}
				if (tPlain == null && it instanceof gregapi.block.ItemBlockBase && !(it instanceof gregapi.block.prefixblock.PrefixBlockItem)) {
					net.minecraft.world.item.ItemStack tS0 = gregapi.util.ST.make(it, 1, 0);
					if (gregapi.util.ST.valid(tS0)) tPlain = tS0;
				}
			}
			if (tPrefix != null) {tStacks.add(tPrefix); tNames.add("PrefixBlockItem-руда");}
			if (tPlain  != null) {tStacks.add(tPlain);  tNames.add("ItemBlockBase-блок");}
			tP.setShiftKeyDown(true); // обход onlyPlaceableWhenSneaking + активации кликнутого блока
			for (int i = 0; i < tStacks.size(); i++) {
				net.minecraft.core.BlockPos tBase = tP.blockPosition().offset(1+i, -1, 0); // гарантированный пол
				net.minecraft.core.BlockPos tTarget = tBase.above();
				tW.setBlock(tBase,   net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
				tW.setBlock(tTarget, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				net.minecraft.world.item.ItemStack tHand = tStacks.get(i).copy();
				tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tHand);
				net.minecraft.world.phys.Vec3 tLoc = new net.minecraft.world.phys.Vec3(tBase.getX()+0.5, tBase.getY()+1.0, tBase.getZ()+0.5);
				net.minecraft.world.phys.BlockHitResult tHit = new net.minecraft.world.phys.BlockHitResult(tLoc, net.minecraft.core.Direction.UP, tBase, false);
				net.minecraft.world.InteractionResult tRes;
				try { tRes = tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND, tHit); }
				catch (Throwable e) { o.println("[GT6-PLACE-PROBE] " + tNames.get(i) + " useItemOn УПАЛ: " + e); e.printStackTrace(gregapi.data.CS.ERR); continue; }
				net.minecraft.world.level.block.state.BlockState tSt = tW.getBlockState(tTarget);
				net.minecraft.world.level.block.entity.BlockEntity tBE = tW.getBlockEntity(tTarget);
				boolean tPlaced = !tSt.isAir();
				boolean tMod = tPlaced && (tSt.getBlock().getClass().getName().startsWith("gregapi.") || tSt.getBlock().getClass().getName().startsWith("gregtech."));
				o.println("[GT6-PLACE-PROBE] " + tNames.get(i) + " result=" + tRes + " placed=" + (tPlaced ? tSt.getBlock().getClass().getSimpleName() : "AIR")
					+ " be=" + (tBE == null ? "null" : tBE.getClass().getSimpleName()) + " modBlock=" + tMod + " @" + tTarget.getX() + "," + tTarget.getY() + "," + tTarget.getZ());
			}
			tP.setShiftKeyDown(false);
			o.println("[GT6-PLACE-PROBE] итог: протестировано корней=" + tStacks.size() + " (modBlock=true у всех = мост useOn→onItemUse жив на реальном клик-пути)");
		} catch (Throwable e) { o.println("[GT6-PLACE-PROBE] фаза упала: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
	}

	// Ф3.0-СУДЬЯ (F-useOn инструменты, гейт: файл gt6toolprobe.flag): проверка канала инструментов РЕАЛЬНЫМ клик-путём.
	// ServerPlayerGameMode.useItemOn (:388 itemStack.onItemUseFirst → :395 state.useItemOn → :415 itemStack.useOn) — тот же
	// метод, что при ServerboundUseItemOnPacket. Ставим MTE-машину, читаем mFacing, кликаем ключом в бок → мост
	// ItemBase.onItemUseFirst → MultiItem → Behavior_Tool → IBlockToolable.Util.onToolClick → TileEntityBase09FacingSingle:68
	// должен сменить mFacing на кликнутую сторону; заодно фиксируем износ (GT.ToolStats.k до/после).
	private int mToolProbePhase = 0; private int mToolProbeTick = 0;
	private net.minecraft.core.BlockPos mProbeChestPos, mProbeTablePos, mProbePipePos, mProbePipePos2;
	private static long probeLong(Object aObj, Class<?> aDecl, String aField) {
		try { java.lang.reflect.Field f = aDecl.getDeclaredField(aField); f.setAccessible(true); return f.getLong(aObj); } catch (Throwable e) { return Long.MIN_VALUE; }
	}
	private static double probeNum(Object aObj, Class<?> aDecl, String aField) {
		try { java.lang.reflect.Field f = aDecl.getDeclaredField(aField); f.setAccessible(true); Object v = f.get(aObj); return v instanceof Number tN ? tN.doubleValue() : Double.NaN; } catch (Throwable e) { return Double.NaN; }
	}
	@net.neoforged.bus.api.SubscribeEvent
	public void onToolProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mToolProbePhase >= 4) return;
		if (!new java.io.File("gt6toolprobe.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		net.minecraft.server.MinecraftServer tSrv = tMC.getSingleplayerServer();
		if (tSrv == null) { mToolProbePhase = 4; return; }
		// фоновый запуск без фокуса — авто-пауза убивает тики и закрывает меню; пробе нужен живой сервер
		tMC.options.pauseOnLostFocus = false;
		if (tMC.screen instanceof net.minecraft.client.gui.screens.PauseScreen) tMC.setScreen(null);
		++mToolProbeTick;
		if (mToolProbePhase == 2) {
			// ФАЗА 3 (~4с после переоткрытия): клиент ДОЛЖЕН видеть доски в слоте 31 (initial-data открытия); затем ломаем рецепт
			if (mToolProbeTick < 540) return;
			mToolProbePhase = 3;
			final java.io.PrintStream o3 = gregapi.data.CS.OUT;
			try {
				net.minecraft.world.item.ItemStack tCli31 = null;
				for (net.minecraft.world.inventory.Slot tS : tMC.player.containerMenu.slots) if (tS.getSlotIndex() == 31 && !(tS.container instanceof net.minecraft.world.entity.player.Inventory)) { tCli31 = tS.getItem(); break; }
				o3.println("[GT6-SYNC-PROBE] ФАЗА3 КЛИЕНТ: меню=" + tMC.player.containerMenu.getClass().getSimpleName()
					+ " containerId=" + tMC.player.containerMenu.containerId
					+ " слот31=" + tCli31 + " screen=" + (tMC.screen == null ? "null" : tMC.screen.getClass().getSimpleName())
					+ " (ждали: 2 доски — донеслось открытием/диффом)");
			} catch (Throwable e) { o3.println("[GT6-SYNC-PROBE] фаза3 клиент упала: " + e); }
			tSrv.execute(() -> { try {
				net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
				net.minecraft.world.level.block.entity.BlockEntity tBE = mProbeTablePos == null ? null : tP.level().getBlockEntity(mProbeTablePos);
				if (tBE instanceof net.minecraft.world.Container tCont) tCont.setItem(24, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG));
				net.minecraft.world.item.ItemStack tSrv31 = null;
				for (net.minecraft.world.inventory.Slot tS : tP.containerMenu.slots) if (tS.getSlotIndex() == 31 && !(tS.container instanceof net.minecraft.world.entity.player.Inventory)) { tSrv31 = tS.getItem(); break; }
				o3.println("[GT6-SYNC-PROBE] ФАЗА3 СЕРВЕР: containerId=" + tP.containerMenu.containerId + " слот31(до ломки)=" + tSrv31 + "; рецепт сломан (бревно в 24) — фаза 4 ждёт очистку у клиента");
			} catch (Throwable e) { o3.println("[GT6-SYNC-PROBE] фаза3 сервер упала: " + e); } });
			return;
		}
		if (mToolProbePhase == 3) {
			// ФАЗА 4 (~3с после ломки): per-tick дифф должен ДОНЕСТИ очистку слота 31
			if (mToolProbeTick < 620) return;
			mToolProbePhase = 4;
			final java.io.PrintStream o4 = gregapi.data.CS.OUT;
			try {
				net.minecraft.world.item.ItemStack tCli31 = null;
				for (net.minecraft.world.inventory.Slot tS : tMC.player.containerMenu.slots) if (tS.getSlotIndex() == 31 && !(tS.container instanceof net.minecraft.world.entity.player.Inventory)) { tCli31 = tS.getItem(); break; }
				o4.println("[GT6-SYNC-PROBE] ФАЗА4 КЛИЕНТ: меню=" + tMC.player.containerMenu.getClass().getSimpleName()
					+ " слот31=" + tCli31 + " (ждали: пусто = per-tick дифф доносит)");
			} catch (Throwable e) { o4.println("[GT6-SYNC-PROBE] фаза4 клиент упала: " + e); }
			tSrv.execute(() -> { try {
				net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
				net.minecraft.world.item.ItemStack tSrv31 = null;
				for (net.minecraft.world.inventory.Slot tS : tP.containerMenu.slots) if (tS.getSlotIndex() == 31 && !(tS.container instanceof net.minecraft.world.entity.player.Inventory)) { tSrv31 = tS.getItem(); break; }
				net.minecraft.world.level.block.entity.BlockEntity tBE = mProbeTablePos == null ? null : tP.level().getBlockEntity(mProbeTablePos);
				o4.println("[GT6-SYNC-PROBE] ФАЗА4 СЕРВЕР: меню=" + tP.containerMenu.getClass().getSimpleName() + " слот31=" + tSrv31
					+ " TE-inv31=" + (tBE instanceof net.minecraft.world.Container tC ? String.valueOf(tC.getItem(31)) : "-"));
				if (mProbePipePos != null) {
					net.minecraft.world.level.block.state.BlockState tPS = tP.level().getBlockState(mProbePipePos);
					net.minecraft.world.level.block.entity.BlockEntity tPBE = tP.level().getBlockEntity(mProbePipePos);
					o4.println("[GT6-PIPE-PROBE] труба-1 (ваниль сверху) через ~15с: блок=" + tPS.getBlock().getClass().getSimpleName()
						+ " be=" + (tPBE == null ? "null" : tPBE.getClass().getSimpleName())
						+ " цела=" + (tPS.getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock));
				}
				if (mProbePipePos2 != null) {
					net.minecraft.world.level.block.state.BlockState tPS = tP.level().getBlockState(mProbePipePos2);
					net.minecraft.world.level.block.entity.BlockEntity tPBE = tP.level().getBlockEntity(mProbePipePos2);
					o4.println("[GT6-PIPE-PROBE] труба-2 (GT6-River сбоку) через ~15с: блок=" + tPS.getBlock().getClass().getSimpleName()
						+ " be=" + (tPBE == null ? "null" : tPBE.getClass().getSimpleName())
						+ " цела=" + (tPS.getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock) + " (air=orphan-cleanup, вода=вытеснение)");
				}
			} catch (Throwable e) { o4.println("[GT6-SYNC-PROBE] фаза4 сервер упала: " + e); } });
			return;
		}
		if (mToolProbePhase == 1) {
			// ФАЗА 2 (~6с после установки): замер тиков и синка — сервер-BE стола/сундука + КЛИЕНТ-BE сундука (крышка)
			if (mToolProbeTick < 420) return;
			mToolProbePhase = 2;
			final java.io.PrintStream o2 = gregapi.data.CS.OUT;
			try {
				if (mProbeChestPos != null) {
					net.minecraft.world.level.block.entity.BlockEntity tCBE = tMC.level.getBlockEntity(mProbeChestPos);
					o2.println("[GT6-TICK-PROBE] КЛИЕНТ сундук: be=" + (tCBE == null ? "null" : tCBE.getClass().getSimpleName())
						+ " mTimer=" + (tCBE == null ? "-" : probeLong(tCBE, gregapi.tileentity.base.TileEntityBase02AdjacentTEBuffer.class, "mTimer"))
						+ " mUsingPlayers=" + (tCBE == null ? "-" : probeNum(tCBE, gregapi.block.multitileentity.example.MultiTileEntityChest.class, "mUsingPlayers"))
						+ " mLidAngle=" + (tCBE == null ? "-" : probeNum(tCBE, gregapi.block.multitileentity.example.MultiTileEntityChest.class, "mLidAngle")));
				}
				net.minecraft.world.item.ItemStack tCliMenu31 = null;
				for (net.minecraft.world.inventory.Slot tS : tMC.player.containerMenu.slots) if (tS.getSlotIndex() == 31 && !(tS.container instanceof net.minecraft.world.entity.player.Inventory)) { tCliMenu31 = tS.getItem(); break; }
				net.minecraft.world.level.block.entity.BlockEntity tCliTBE = mProbeTablePos == null ? null : tMC.level.getBlockEntity(mProbeTablePos);
				o2.println("[GT6-CRAFT-PROBE] КЛИЕНТ-меню слот31=" + tCliMenu31 + " (меню=" + tMC.player.containerMenu.getClass().getSimpleName()
					+ ") КЛИЕНТ-TE=" + (tCliTBE == null ? "null" : tCliTBE.getClass().getSimpleName())
					+ " КЛИЕНТ-TE-inv31=" + (tCliTBE instanceof net.minecraft.world.Container tCC ? String.valueOf(tCC.getItem(31)) : "-")
					+ " screen=" + (tMC.screen == null ? "null" : tMC.screen.getClass().getSimpleName()));
			} catch (Throwable e) { o2.println("[GT6-TICK-PROBE] клиент-фаза упала: " + e); }
			tSrv.execute(() -> { try {
				net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tW = tP.level();
				if (mProbeChestPos != null) {
					net.minecraft.world.level.block.entity.BlockEntity tBE = tW.getBlockEntity(mProbeChestPos);
					o2.println("[GT6-TICK-PROBE] СЕРВЕР сундук: be=" + (tBE == null ? "null" : tBE.getClass().getSimpleName())
						+ " mTimer=" + (tBE == null ? "-" : probeLong(tBE, gregapi.tileentity.base.TileEntityBase02AdjacentTEBuffer.class, "mTimer"))
						+ " mUsingPlayers=" + (tBE == null ? "-" : probeNum(tBE, gregapi.block.multitileentity.example.MultiTileEntityChest.class, "mUsingPlayers"))
						+ " canUpdate=" + (tBE instanceof gregapi.tileentity.base.TileEntityBase01Root tR ? tR.canUpdate() : null));
				}
				if (mProbeTablePos != null) {
					net.minecraft.world.level.block.entity.BlockEntity tBE = tW.getBlockEntity(mProbeTablePos);
					o2.println("[GT6-TICK-PROBE] СЕРВЕР стол: be=" + (tBE == null ? "null" : tBE.getClass().getSimpleName())
						+ " mTimer=" + (tBE == null ? "-" : probeLong(tBE, gregapi.tileentity.base.TileEntityBase02AdjacentTEBuffer.class, "mTimer"))
						+ " canUpdate=" + (tBE instanceof gregapi.tileentity.base.TileEntityBase01Root tR ? tR.canUpdate() : null)
						+ " mUpdatedGrid=" + (tBE instanceof gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable tT ? tT.mUpdatedGrid : null)
						+ " inv31=" + (tBE instanceof net.minecraft.world.Container tC ? tC.getItem(31) : "-"));
					net.minecraft.world.item.ItemStack tSrvMenu31 = null;
					for (net.minecraft.world.inventory.Slot tS : tP.containerMenu.slots) if (tS.getSlotIndex() == 31 && !(tS.container instanceof net.minecraft.world.entity.player.Inventory)) { tSrvMenu31 = tS.getItem(); break; }
					o2.println("[GT6-CRAFT-PROBE] сервер-меню слот31=" + tSrvMenu31 + " (меню=" + tP.containerMenu.getClass().getSimpleName() + " stillValid=" + tP.containerMenu.stillValid(tP) + ")");
					try { tP.containerMenu.broadcastFullState(); o2.println("[GT6-CRAFT-PROBE] broadcastFullState отправлен (контроль diff-пути)"); } catch (Throwable e) { o2.println("[GT6-CRAFT-PROBE] broadcastFullState УПАЛ: " + e); }
				}
				o2.println("[GT6-TICK-PROBE] итог: mTimer>0 обе стороны = тики живы; клиент mUsingPlayers>0 = синк крышки жив");
				// СИНК-ТЕСТ: переоткрыть меню стола (client-BE уже реконструирован → экран не самозакроется),
				// затем СЛОМАТЬ рецепт (второе бревно в слот 24) → сервер-тик очистит слот 31 → per-tick дифф
				// обязан донести очистку клиенту; фаза 3 сверит.
				if (mProbeTablePos != null && tW.getBlockEntity(mProbeTablePos) instanceof net.minecraft.world.Container tCont) {
					tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
					net.minecraft.world.InteractionResult tReopen = tP.gameMode.useItemOn(tP, tW, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND,
						new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(mProbeTablePos.getX()+0.5, mProbeTablePos.getY()+1.0, mProbeTablePos.getZ()+0.5), net.minecraft.core.Direction.UP, mProbeTablePos, false));
					o2.println("[GT6-SYNC-PROBE] переоткрытие=" + tReopen + " меню=" + tP.containerMenu.getClass().getSimpleName() + "; фаза 3 сверит клиент-слот31 (ждём доски)");
				}
			} catch (Throwable e) { o2.println("[GT6-TICK-PROBE] сервер-фаза упала: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
			return;
		}
		if (mToolProbeTick < 300) return;
		mToolProbePhase = 1;
		final java.io.PrintStream o = gregapi.data.CS.OUT;
		tSrv.execute(() -> { try {
			net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
			net.minecraft.server.level.ServerLevel tW = tP.level();
			gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
			if (tReg == null) { o.println("[GT6-TOOL-PROBE] реестр null"); return; }
			// 1) установка машины реальным клик-путём (как в PLACE-PROBE)
			net.minecraft.core.BlockPos tBase = tP.blockPosition().offset(2, -1, 2);
			net.minecraft.core.BlockPos tTarget = tBase.above();
			// воздушный карман 3×3×3 вокруг цели (иначе checkObstruction душит клик рельефом — артефакт замера, не баг)
			for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++)
				tW.setBlock(tBase.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			tW.setBlock(tBase,   net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
			net.minecraft.world.item.ItemStack tMachine = tReg.getItem(20001);
			if (!gregapi.util.ST.valid(tMachine)) { o.println("[GT6-TOOL-PROBE] MTE#20001 невалиден"); return; }
			tP.setShiftKeyDown(true);
			tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tMachine.copy());
			net.minecraft.world.phys.BlockHitResult tPlaceHit = new net.minecraft.world.phys.BlockHitResult(
				new net.minecraft.world.phys.Vec3(tBase.getX()+0.5, tBase.getY()+1.0, tBase.getZ()+0.5), net.minecraft.core.Direction.UP, tBase, false);
			tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND, tPlaceHit);
			tP.setShiftKeyDown(false);
			net.minecraft.world.level.block.entity.BlockEntity tBE = tW.getBlockEntity(tTarget);
			if (!(tBE instanceof gregapi.tileentity.base.TileEntityBase09FacingSingle tFS)) {
				o.println("[GT6-TOOL-PROBE] машина не встала или BE не FacingSingle: " + (tBE == null ? "null" : tBE.getClass().getSimpleName())); return; }
			byte tFacingBefore = tFS.mFacing;
			// 2) ключ в руку, клик в ВОСТОЧНЫЙ бок машины по центру грани
			net.minecraft.world.item.ItemStack tWrench = gregapi.data.CS.ToolsGT.sMetaTool.getToolWithStats(gregapi.data.CS.ToolsGT.WRENCH, gregapi.data.MT.Steel, gregapi.data.MT.Steel);
			if (!gregapi.util.ST.valid(tWrench)) { o.println("[GT6-TOOL-PROBE] ключ не синтезирован"); return; }
			long tDmgBefore = ((gregapi.item.multiitem.MultiItemTool)gregapi.util.ST.item_(tWrench)).getToolDamage(tWrench);
			tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tWrench);
			// износ гейтится hasInfiniteItems (креатив → без износа, 1:1 doDamage:434) — на время клика SURVIVAL
			tP.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			net.minecraft.world.phys.BlockHitResult tToolHit = new net.minecraft.world.phys.BlockHitResult(
				new net.minecraft.world.phys.Vec3(tTarget.getX()+1.0, tTarget.getY()+0.5, tTarget.getZ()+0.5), net.minecraft.core.Direction.EAST, tTarget, false);
			net.minecraft.world.InteractionResult tRes = tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND, tToolHit);
			byte tFacingAfter = tFS.mFacing;
			net.minecraft.world.item.ItemStack tWrenchAfter = tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
			long tDmgAfter = gregapi.util.ST.valid(tWrenchAfter) && gregapi.util.ST.item_(tWrenchAfter) instanceof gregapi.item.multiitem.MultiItemTool tMT ? tMT.getToolDamage(tWrenchAfter) : -1;
			o.println("[GT6-TOOL-PROBE] SURVIVAL: be=" + tBE.getClass().getSimpleName() + " result=" + tRes
				+ " facing " + tFacingBefore + "→" + tFacingAfter + " (клик EAST=5; смена=" + (tFacingBefore != tFacingAfter) + ")"
				+ " wear " + tDmgBefore + "→" + tDmgAfter + " (износ=" + (tDmgAfter > tDmgBefore) + ")");
			// послойная диагностика (мерить, не гадать): item-слой и блок-слой напрямую, всё ещё SURVIVAL
			if (tFacingAfter == tFacingBefore) {
				net.minecraft.world.item.ItemStack tHand = tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
				gregapi.item.multiitem.MultiItemTool tTool = (gregapi.item.multiitem.MultiItemTool)gregapi.util.ST.item_(tHand);
				o.println("[GT6-TOOL-PROBE] DIAG usable=" + tTool.isItemStackUsable(tHand)
					+ " itemLayer=" + tTool.onItemUseFirst(tHand, tP, tW, tTarget.getX(), tTarget.getY(), tTarget.getZ(), 5, 1.0F, 0.5F, 0.5F)
					+ " facingПосле_itemLayer=" + tFS.mFacing);
				java.util.List<String> tChat = new gregapi.code.ArrayListNoNulls<>();
				long tDirect = gregapi.block.IBlockToolable.Util.onToolClick(gregapi.data.CS.TOOL_wrench, Long.MAX_VALUE, 3, tP, tChat, tP.getInventory(), false, tHand, tW, (byte)5, tTarget.getX(), tTarget.getY(), tTarget.getZ(), 1.0F, 0.5F, 0.5F);
				o.println("[GT6-TOOL-PROBE] DIAG blockLayer tDamage=" + tDirect + " chat=" + tChat + " facingПосле_blockLayer=" + tFS.mFacing);
			}
			// контрольный клик в CREATIVE тем же движковым путём
			tP.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
			byte tFacingBefore2 = tFS.mFacing;
			net.minecraft.world.InteractionResult tRes2 = tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND, tToolHit);
			o.println("[GT6-TOOL-PROBE] CREATIVE: result=" + tRes2 + " facing " + tFacingBefore2 + "→" + tFS.mFacing);
			o.println("[GT6-TOOL-PROBE] итог: SURVIVAL-поворот=" + (tFacingAfter != tFacingBefore) + " износ=" + (tDmgAfter > tDmgBefore) + " (оба true = мост жив полностью)");
			// сундук (жалоба игрока «сундуки нельзя открыть»): ставим MTE-сундук и ПКМ пустой рукой → должен открыться GUI
			tW.setBlock(tTarget, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			net.minecraft.world.item.ItemStack tChest = tReg.getItem(32745);
			if (false && gregapi.util.ST.valid(tChest)) { // сундук-часть ВЫКЛЮЧЕНА: механика доказана, изолируем стол
				tP.setShiftKeyDown(true);
				tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tChest.copy());
				tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND, tPlaceHit);
				tP.setShiftKeyDown(false);
				tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
				net.minecraft.world.level.block.entity.BlockEntity tCBE = tW.getBlockEntity(tTarget);
				net.minecraft.world.InteractionResult tCRes = tP.gameMode.useItemOn(tP, tW, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND, tToolHit);
				boolean tOpened = tP.containerMenu != tP.inventoryMenu;
				o.println("[GT6-CHEST-PROBE] be=" + (tCBE == null ? "null" : tCBE.getClass().getSimpleName()) + " result=" + tCRes
					+ " menu=" + tP.containerMenu.getClass().getSimpleName() + " открыт=" + tOpened);
				mProbeChestPos = tTarget; // GUI остаётся открытым до фазы 2 → mUsingPlayers>0, крышка должна анимироваться
			} else o.println("[GT6-CHEST-PROBE] MTE#32745 невалиден");
			// крафт-стол: ищем ID по каноническому классу, ставим рядом (для фазы-2 замера тиков)
			short tTableID = -1;
			for (gregapi.block.multitileentity.MultiTileEntityClassContainer tC : tReg.mRegistrations)
				if (tC.mCanonicalTileEntity instanceof gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable) { tTableID = tC.mID; break; }
			if (tTableID >= 0) {
				net.minecraft.core.BlockPos tTBase = tBase.offset(0, 0, 2);
				for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++)
					tW.setBlock(tTBase.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				tW.setBlock(tTBase, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
				tP.setShiftKeyDown(true);
				tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tReg.getItem(tTableID).copy());
				tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(tTBase.getX()+0.5, tTBase.getY()+1.0, tTBase.getZ()+0.5), net.minecraft.core.Direction.UP, tTBase, false));
				tP.setShiftKeyDown(false);
				mProbeTablePos = tTBase.above();
				o.println("[GT6-CRAFT-PROBE] стол MTE#" + tTableID + " установлен: be=" + (tW.getBlockEntity(mProbeTablePos) == null ? "null" : tW.getBlockEntity(mProbeTablePos).getClass().getSimpleName()));
				// сценарий игрока: открыть GUI стола, ПОТОМ положить бревно в сетку; фаза 2 сверит все звенья синка
				if (tW.getBlockEntity(mProbeTablePos) instanceof net.minecraft.world.Container tCont) {
					tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
					net.minecraft.world.InteractionResult tOpenRes = tP.gameMode.useItemOn(tP, tW, net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND,
						new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(mProbeTablePos.getX()+0.5, mProbeTablePos.getY()+1.0, mProbeTablePos.getZ()+0.5), net.minecraft.core.Direction.UP, mProbeTablePos, false));
					Object tGUIS = null; try { tGUIS = ((gregapi.tileentity.ITileEntityGUI)tW.getBlockEntity(mProbeTablePos)).getGUIServer(0, tP); } catch (Throwable e) { tGUIS = "EX:" + e; }
					tCont.setItem(21, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG));
					o.println("[GT6-CRAFT-PROBE] открытие=" + tOpenRes + " меню=" + tP.containerMenu.getClass().getSimpleName()
						+ " getGUIServer=" + (tGUIS == null ? "null" : tGUIS.getClass().getSimpleName()) + "; бревно в слот 21");
				}
			} else o.println("[GT6-CRAFT-PROBE] AdvancedCraftingTable не найден в реестре");
			// труба + вода (репорт: «блок сразу пропадает при контакте с водой»): ставим трубу, рядом источник воды
			short tPipeID = -1;
			for (gregapi.block.multitileentity.MultiTileEntityClassContainer tC : tReg.mRegistrations)
				if (tC.mCanonicalTileEntity instanceof gregapi.tileentity.connectors.MultiTileEntityPipeFluid) { tPipeID = tC.mID; break; }
			if (tPipeID >= 0) {
				net.minecraft.core.BlockPos tPBase = tBase.offset(-3, 0, 0);
				for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++)
					tW.setBlock(tPBase.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				tW.setBlock(tPBase, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
				tW.setBlock(tPBase.offset(1, 0, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3); // пол под воду
				tP.setShiftKeyDown(true);
				tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tReg.getItem(tPipeID).copy());
				tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(tPBase.getX()+0.5, tPBase.getY()+1.0, tPBase.getZ()+0.5), net.minecraft.core.Direction.UP, tPBase, false));
				tP.setShiftKeyDown(false);
				mProbePipePos = tPBase.above();
				o.println("[GT6-PIPE-PROBE] труба MTE#" + tPipeID + " @" + mProbePipePos.toShortString() + ": блок=" + tW.getBlockState(mProbePipePos).getBlock().getClass().getSimpleName()
					+ " be=" + (tW.getBlockEntity(mProbePipePos) == null ? "null" : tW.getBlockEntity(mProbePipePos).getClass().getSimpleName()) + "; вода: ванильная сверху-сбоку");
				tW.setBlock(mProbePipePos.offset(1, 1, 0), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3); // льётся сверху-сбоку НА трубу
				// вторая труба + GT6-вода (River) рядом — игрок тестирует у мировой воды
				net.minecraft.core.BlockPos tP2Base = tPBase.offset(0, 0, -3);
				for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++)
					tW.setBlock(tP2Base.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				tW.setBlock(tP2Base, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
				tW.setBlock(tP2Base.offset(1, 0, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
				tP.setShiftKeyDown(true);
				tP.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tReg.getItem(tPipeID).copy());
				tP.gameMode.useItemOn(tP, tW, tP.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(tP2Base.getX()+0.5, tP2Base.getY()+1.0, tP2Base.getZ()+0.5), net.minecraft.core.Direction.UP, tP2Base, false));
				tP.setShiftKeyDown(false);
				mProbePipePos2 = tP2Base.above();
				if (gregapi.data.CS.BlocksGT.River != null) {
					tW.setBlock(mProbePipePos2.offset(1, 0, 0), gregapi.data.CS.BlocksGT.River.defaultBlockState(), 3);
					o.println("[GT6-PIPE-PROBE] труба-2 @" + mProbePipePos2.toShortString() + " be=" + (tW.getBlockEntity(mProbePipePos2) == null ? "null" : tW.getBlockEntity(mProbePipePos2).getClass().getSimpleName()) + "; GT6-River рядом");
				} else o.println("[GT6-PIPE-PROBE] BlocksGT.River == null — GT6-вода недоступна пробе");
			} else o.println("[GT6-PIPE-PROBE] PipeFluid не найден в реестре");
		} catch (Throwable e) { o.println("[GT6-TOOL-PROBE] фаза упала: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
	}

	// A/GAP-1 СУДЬЯ-ДАМПЕР (гейт gt6blockdump.flag): порт-дескриптор 3D-объектов-В-МИРЕ. Ставит по 1 представителю КАЖДОГО
	// уникального MTE-TE-класса (шкафы/трубы/провода/монетки/сундуки) в сетку, затем на КЛИЕНТЕ (живой BE, реальный BER-путь)
	// собирает world-квады → descriptor.port.block.jsonl. Это порт-сторона паритета блоков-в-мире (item-форма 98.28% не покрывает
	// FACING/mActive/mConnections). Пара к оракул-DumpRenderBlocks (GAP-2, след.) + компаратор (GAP-3).
	private int mBlockDumpPhase = 0; private int mBlockDumpTick = 0;
	private final java.util.List<net.minecraft.core.BlockPos> mBlockDumpPositions = new java.util.ArrayList<>();
	@net.neoforged.bus.api.SubscribeEvent
	public void onWorldBlockDump(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mBlockDumpPhase >= 2) return;
		if (!new java.io.File("gt6blockdump.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		net.minecraft.server.MinecraftServer tSrv = tMC.getSingleplayerServer();
		if (tSrv == null) { mBlockDumpPhase = 2; return; }
		++mBlockDumpTick;
		final java.io.PrintStream o = gregapi.data.CS.OUT;
		if (mBlockDumpPhase == 0 && mBlockDumpTick >= 300) {
			mBlockDumpPhase = 2; // A/GAP-3 симметрия: canonical-TE (без мира, level=null) — точная пара golden DumpRenderBlocks; фаза-1 (живой BE) не нужна.
			try {
				gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
				if (tReg == null) { o.println("[GT6-BLOCKDUMP] реестр gt.multitileentity null"); return; }
				net.minecraft.world.level.block.Block tBlk = tReg.mBlock;
				java.util.List<String> tLines = new java.util.ArrayList<>();
				java.util.Set<Class<?>> tSeen = new java.util.HashSet<>();
				for (gregapi.block.multitileentity.MultiTileEntityClassContainer tC : tReg.mRegistrations) {
					if (tC.mCanonicalTileEntity == null || !tSeen.add(tC.mCanonicalTileEntity.getClass())) continue;
					try { tLines.add(gregapi.render.GT6ItemModel.describeWorldBlockCanonical(tC.mCanonicalTileEntity, tBlk)); } catch (Throwable e) {/* один класс не рушит дамп */}
					// GAP-4: connector-TE (трубы/провода) — доп. дамп с mConnections=63 (все соединения), рендер по соединениям.
					if (tC.mCanonicalTileEntity instanceof gregapi.tileentity.connectors.TileEntityBase10ConnectorRendered)
						try { tLines.add(gregapi.render.GT6ItemModel.describeWorldBlockConn(tC.mCanonicalTileEntity, tBlk, (byte) 63)); } catch (Throwable e) {/**/}
				}
				java.nio.file.Path tDir = java.nio.file.Paths.get("gt6dump");
				java.nio.file.Files.createDirectories(tDir);
				java.nio.file.Files.write(tDir.resolve("descriptor.port.block.jsonl"), tLines, java.nio.charset.StandardCharsets.UTF_8);
				long tWithSpr = tLines.stream().filter(s -> s.contains("\"spr\":[\"")).count();
				o.println("[GT6-BLOCKDUMP] canonical descriptor.port.block.jsonl: TE-классов=" + tLines.size() + " с непустыми спрайтами=" + tWithSpr + " (симметрия golden)");
			} catch (Throwable e) { o.println("[GT6-BLOCKDUMP] canonical-дамп упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
			return;
		}
		if (mBlockDumpPhase == 1 && mBlockDumpTick >= 380) {
			mBlockDumpPhase = 2;
			try {
				java.util.List<String> tLines = new java.util.ArrayList<>();
				for (net.minecraft.core.BlockPos tPos : mBlockDumpPositions) try { tLines.add(gregapi.render.GT6ItemModel.describeWorldBlock(tMC.level, tPos)); } catch (Throwable e) {/**/}
				java.nio.file.Path tDir = java.nio.file.Paths.get("gt6dump");
				java.nio.file.Files.createDirectories(tDir);
				java.nio.file.Files.write(tDir.resolve("descriptor.port.block.jsonl"), tLines, java.nio.charset.StandardCharsets.UTF_8);
				long tWithSpr = tLines.stream().filter(s -> s.contains("\"spr\":[\"")).count();
				o.println("[GT6-BLOCKDUMP] descriptor.port.block.jsonl записан: " + tLines.size() + " world-блоков, с непустыми спрайтами=" + tWithSpr + " (клиент BER-путь)");
			} catch (Throwable e) { o.println("[GT6-BLOCKDUMP] dump-фаза упала: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
			return;
		}
	}

	// НАДЁЖНЫЙ МОСТ синка (пара к буферу NetworkHandler.PENDING): каждый клиент-тик доигрываем координатные
	// GT6-пакеты, обогнавшие свой чанк при логине (иначе worldgen-MTE стартовой области оставались без клиент-BE).
	@net.neoforged.bus.api.SubscribeEvent
	public void onPendingPackets(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		gregapi.network.NetworkHandler.processPending(Minecraft.getInstance().level);
	}

	// U2-СУДЬЯ под-боксов (гейт gt6geomprobe.flag): канал render-bounds MTE конец-в-конец на ЖИВОМ клиент-BE
	// (client-placement, метод F3-render.md §9): для каждого MTE-класса — пассы × setBlockBounds → чтение
	// IBlock.getRenderBounds (то, что ест GT6BlockModel.applyBounds). Критерий U2: у многопассовых объектов
	// bounds пассов РАЗЛИЧНЫ (не один куб). До фикса applyBounds (MTE≠BlockBase→null): у ВСЕХ был куб.
	private boolean mGeomProbeDone = false; private int mGeomProbeTick = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public void onGeomProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mGeomProbeDone) return;
		if (!new java.io.File("gt6geomprobe.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		if (++mGeomProbeTick < 100) return;
		mGeomProbeDone = true;
		final java.io.PrintStream o = gregapi.data.CS.OUT;
		try {
			gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
			if (tReg == null) {o.println("[GT6-GEOMPROBE] реестр null"); return;}
			net.minecraft.core.BlockPos tBase = tMC.player.blockPosition().offset(0, 8, 0);
			boolean[] tSides = {true, true, true, true, true, true};
			int tTotal = 0, tMulti = 0, tDistinctOK = 0, tSubBox = 0, tQuadsTotal = 0;
			java.util.List<String> tExamples = new java.util.ArrayList<>();
			java.util.Set<Class<?>> tSeen = new java.util.HashSet<>();
			int tSlot = 0;
			for (gregapi.block.multitileentity.MultiTileEntityClassContainer tC : tReg.mRegistrations) {
				if (tC.mCanonicalTileEntity == null || !tSeen.add(tC.mCanonicalTileEntity.getClass())) continue;
				net.minecraft.core.BlockPos tPos = tBase.offset((tSlot % 32) * 2, ((tSlot / 32) % 8) * 2, (tSlot / 256) * 2); ++tSlot;
				try {
					gregapi.block.multitileentity.MultiTileEntityContainer tCont = tReg.getNewTileEntityContainer(tMC.level, tPos.getX(), tPos.getY(), tPos.getZ(), tC.mID, null);
					if (tCont == null || !(tCont.mTileEntity instanceof gregapi.render.IRenderedBlockObject tR)) continue;
					tMC.level.setBlock(tPos, tCont.mBlock.defaultBlockState(), 3);
					tMC.level.setBlockEntity(tCont.mTileEntity);
					++tTotal;
					net.minecraft.world.level.block.Block tBlk = tCont.mBlock;
					int tPasses = tR.getRenderPasses(tBlk, tSides);
					if (tPasses > 1) ++tMulti;
					java.util.Set<String> tDistinct = new java.util.HashSet<>();
					boolean tHasSub = false;
					for (int p = 0; p < tPasses; p++) {
						if (!tR.usesRenderPass(p, tSides)) continue;
						tR.setBlockBounds(tBlk, p, tSides);
						float[] b = tBlk instanceof gregapi.block.IBlock tI ? tI.getRenderBounds() : null;
						if (b == null) continue;
						tDistinct.add(java.util.Arrays.toString(b));
						if (b[0] > 0.001F || b[1] > 0.001F || b[2] > 0.001F || b[3] < 0.999F || b[4] < 0.999F || b[5] < 0.999F) tHasSub = true;
					}
					if (tDistinct.size() > 1) ++tDistinctOK;
					if (tHasSub) ++tSubBox;
					gregapi.render.GT6QuadBuilder tQB = new gregapi.render.GT6QuadBuilder();
					gregapi.render.GT6BlockModel.buildRendererQuads(tQB, tR, tBlk, tMC.level, tPos.getX(), tPos.getY(), tPos.getZ());
					tQuadsTotal += tQB.quads().size();
					if (tExamples.size() < 12 && (tDistinct.size() > 1 || tHasSub))
						tExamples.add(tC.mCanonicalTileEntity.getClass().getSimpleName() + "{passes=" + tPasses + " distinct=" + tDistinct.size() + " sub=" + tHasSub + " quads=" + tQB.quads().size() + "}");
				} catch (Throwable e) {/* один класс не рушит пробу */}
			}
			o.println("[GT6-GEOMPROBE] MTE-классов=" + tTotal + " многопассовых=" + tMulti + " с-различными-bounds=" + tDistinctOK + " с-под-боксами=" + tSubBox + " quads=" + tQuadsTotal);
			o.println("[GT6-GEOMPROBE] примеры: " + String.join(", ", tExamples));
			// Улика R3/крусибл («нет грани/верхней части»): спрайты, НЕ найденные в атласе — их грани putFace молча пропускает.
			java.util.List<String> tMiss = new java.util.ArrayList<>(gregapi.render.GT6QuadBuilder.sMissingSprites);
			java.util.Collections.sort(tMiss);
			o.println("[GT6-GEOMPROBE] missing-спрайтов=" + tMiss.size() + (tMiss.isEmpty() ? "" : ": " + String.join(", ", tMiss.subList(0, Math.min(40, tMiss.size())))));
		} catch (Throwable e) {o.println("[GT6-GEOMPROBE] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR);}
	}

	// N5-прозрачность СУДЬЯ (гейт gt6inject.flag): скан клиент-чанков на worldgen-камешки MultiTileEntityRock — клиент-BE
	// существует (не прозрачен) + level!=null (mTexture реальная). До onChunkWatch-фикса non-ticking worldgen-MTE клиенту не
	// синкались (getUpdateTag=0) → found=0. После (sendUpdateToPlayer через ITileEntitySynchronising) → found>0.
	private int mRockScanPhase = 0; private int mRockScanTick = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public void onRockScan(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mRockScanPhase >= 1) return;
		if (!new java.io.File("gt6inject.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		if (++mRockScanTick < 420) return;
		mRockScanPhase = 1;
		java.io.PrintStream o = gregapi.data.CS.OUT;
		try {
			net.minecraft.world.level.Level tCL = tMC.level;
			net.minecraft.core.BlockPos tPP = tMC.player.blockPosition();
			int tFound = 0, tLevelOK = 0, tTexOK = 0;
			for (int dx = -24; dx <= 24; dx++) for (int dz = -24; dz <= 24; dz++) for (int dy = -6; dy <= 6; dy++) {
				net.minecraft.core.BlockPos tSP = tPP.offset(dx, dy, dz);
				if (!(tCL.getBlockState(tSP).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) continue;
				net.minecraft.world.level.block.entity.BlockEntity tBE = tCL.getBlockEntity(tSP);
				if (tBE instanceof gregtech.tileentity.placeables.MultiTileEntityRock tRk) {
					tFound++;
					if (tRk.getLevel() != null) tLevelOK++;
					try { if (tRk.mTexture != null) tTexOK++; } catch (Throwable e) {/**/}
				}
			}
			o.println("[GT6-ROCKSCAN] клиент worldgen-камешки: found=" + tFound + " levelOK=" + tLevelOK + " texOK=" + tTexOK
				+ " (found>0 = клиент-BE камешков ЕСТЬ → не прозрачны; onChunkWatch non-ticking синк жив)");
		} catch (Throwable e) { o.println("[GT6-ROCKSCAN] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
	}

	// N5-СУДЬЯ (worldgen-камни прозрачны+снег, гейт gt6inject.flag): ставит реальный BlockStones над игроком и замеряет
	// ДВИЖКОВО: (рендер) getTexture 6 граней valid/null + резолв спрайта mIcons[0] (PURPLE=missing); (снег) collision
	// shape isFaceFull(UP) — true у полного каменного блока = снег ляжет (норма для полного куба; MTE-камешек box=null → false).
	private int mStoneProbePhase = 0; private int mStoneProbeTick = 0;
	@net.neoforged.bus.api.SubscribeEvent
	public void onStoneProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mStoneProbePhase >= 1) return;
		if (!new java.io.File("gt6inject.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		net.minecraft.server.MinecraftServer tSrv = tMC.getSingleplayerServer();
		if (tSrv == null) { mStoneProbePhase = 1; return; }
		if (++mStoneProbeTick < 320) return;
		mStoneProbePhase = 1;
		final java.io.PrintStream o = gregapi.data.CS.OUT;
		tSrv.execute(() -> { try {
			net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
			net.minecraft.server.level.ServerLevel tW = tP.level();
			net.minecraft.world.level.block.Block tStone = null;
			for (net.minecraft.world.level.block.Block b : net.minecraft.core.registries.BuiltInRegistries.BLOCK) if (b instanceof gregapi.block.metatype.BlockStones) { tStone = b; break; }
			if (tStone == null) { o.println("[GT6-STONE-PROBE] BlockStones не найден в реестре"); return; }
			net.minecraft.core.BlockPos tPos = tP.blockPosition().above(3);
			boolean tSet = tW.setBlock(tPos, tStone.defaultBlockState(), 3);
			net.minecraft.world.phys.shapes.VoxelShape tColl = tW.getBlockState(tPos).getCollisionShape(tW, tPos);
			boolean tFaceFullUp = net.minecraft.world.level.block.Block.isFaceFull(tColl, net.minecraft.core.Direction.UP);
			int tValid = 0, tNull = 0;
			boolean[] tAll = {true,true,true,true,true,true};
			if (tStone instanceof gregapi.render.IRenderedBlock rb) {
				for (byte s = 0; s < 6; s++) {
					try { gregapi.render.ITexture tx = rb.getTexture(0, s, tAll, tW, tPos.getX(), tPos.getY(), tPos.getZ());
						if (tx == null || !tx.isValidTexture()) tNull++; else tValid++;
					} catch (Throwable e) { tNull++; }
				}
			}
			String tIconInfo = "?";
			try {
				gregapi.block.metatype.BlockStones tSB = (gregapi.block.metatype.BlockStones)tStone;
				net.minecraft.resources.Identifier tIcon = tSB.mIcons != null && tSB.mIcons.length > 0 ? tSB.mIcons[0].getIcon(0) : null;
				net.minecraft.client.renderer.texture.TextureAtlasSprite tSpr = tIcon == null ? null : gregapi.render.GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
				tIconInfo = "icon=" + tIcon + " sprite=" + (tSpr == null ? "PURPLE/null" : "VALID");
			} catch (Throwable e) { tIconInfo = "EXC:" + e; }
			o.println("[GT6-STONE-PROBE] block=" + tStone.getClass().getSimpleName() + " set=" + tSet + " грани valid=" + tValid + " null=" + tNull
				+ " " + tIconInfo + " faceFullUp(снег)=" + tFaceFullUp + " @" + tPos.getX()+","+tPos.getY()+","+tPos.getZ()
				+ " (valid=6 = рисуется НЕ прозрачен; PURPLE = missing-спрайт; faceFullUp=true у полного блока = снег норма)");
			// MTE-Rock (камешек): после F-shape-моста getCollisionShape должен стать маленьким/пустым → снег НЕ ляжет
			try {
				gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
				net.minecraft.world.item.ItemStack tRockStack = tReg == null ? null : tReg.getItem(32757);
				if (gregapi.util.ST.valid(tRockStack) && tRockStack.getItem() instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tRockItem) {
					net.minecraft.core.BlockPos tRPos = tP.blockPosition().offset(2, 0, 0);
					tW.setBlock(tRPos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
					tW.setBlock(tRPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
					boolean tRockPlaced = tRockItem.onItemUse(tRockStack, tP, tW, tRPos.getX(), tRPos.getY()-1, tRPos.getZ(), gregapi.data.CS.SIDE_TOP, 0.5f, 1.0f, 0.5f);
					net.minecraft.world.level.block.entity.BlockEntity tSrvBE = tW.getBlockEntity(tRPos);
					// N5-прозрачность: getUpdateTag (то, что уходит клиенту в chunk-пакете). Пустой (size=0) = клиент получит
					// пустой BE → mRock/mTexture теряются → прозрачен. Подтверждает корень (TileEntityBase01Root не переопределяет getUpdateTag).
					int tUpdTagSize = -1; try { if (tSrvBE != null) tUpdTagSize = tSrvBE.getUpdateTag(tW.registryAccess()).size(); } catch (Throwable e) { tUpdTagSize = -2; }
					net.minecraft.world.level.block.state.BlockState tRockState = tW.getBlockState(tRPos);
					net.minecraft.world.phys.shapes.VoxelShape tRockColl = tRockState.getCollisionShape(tW, tRPos);
					net.minecraft.world.phys.shapes.VoxelShape tRockShape = tRockState.getShape(tW, tRPos);
					boolean tRockFaceFull = net.minecraft.world.level.block.Block.isFaceFull(tRockColl, net.minecraft.core.Direction.UP);
					o.println("[GT6-STONE-PROBE] MTE-Rock placed=" + tRockPlaced + " blockCls=" + tRockState.getBlock().getClass().getSimpleName()
						+ " srvBE=" + (tSrvBE == null ? "null" : tSrvBE.getClass().getSimpleName()) + " updateTagSize=" + tUpdTagSize
						+ " collEmpty=" + tRockColl.isEmpty() + " shapeEmpty=" + tRockShape.isEmpty() + " faceFullUp(снег)=" + tRockFaceFull + " @" + tRPos.getX()+","+tRPos.getY()+","+tRPos.getZ()
						+ " (faceFullUp=false = снег НЕ ляжет; updateTagSize=0 = клиент получит ПУСТОЙ BE → прозрачность)");
				} else o.println("[GT6-STONE-PROBE] MTE-Rock item невалиден (id 32757)");
			} catch (Throwable e) { o.println("[GT6-STONE-PROBE] MTE-Rock замер упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
		} catch (Throwable e) { o.println("[GT6-STONE-PROBE] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
	}

	// B1-СУДЬЯ (F5-жидкости, гейт: файл run/gt6fluidprobe.flag): проверка, что движок mc26 распознаёт мировую воду GT6
	// (Ocean/River/Swamp) как ВОДУ — заливает куб Ocean вокруг игрока и через ~2с читает ДВИЖКОВЫЕ флаги погружения
	// (isInWater/isUnderWater/isEyeInFluid(WATER)/getFluidHeight/air/deltaY). Реальный путь: те же поля, что vanilla-вода
	// ставит в Entity.baseTick через EntityFluidInteraction. isInWater=true = получен весь vanilla-водоканал (push/утопление/плавание).
	private int mFluidProbePhase = 0; private int mFluidProbeTick = 0; private boolean mB5Done = false;
	@net.neoforged.bus.api.SubscribeEvent
	public void onFluidProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mFluidProbePhase >= 2) return;
		if (!new java.io.File("gt6fluidprobe.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		// B5-СУДЬЯ (цвет/рендер материал-жидкостей движком, клиент): render-type (translucent?), FluidGT-цвет+текстура,
		// getTexture(→BlockTextureFluid) — что neo РЕАЛЬНО рисует для BlockBaseFluid (масла/кислоты) через GT6BlockModel.
		// Вода (BlockWaterlike/Ocean) — B1-B4 (getFluidState→WATER, vanilla mc26-рендер = goal «реальная вода 26 версии»).
		if (!mB5Done) {
			mB5Done = true;
			final java.io.PrintStream ob5 = gregapi.data.CS.OUT;
			try {
				net.minecraft.core.Direction[] tDirs = {null, net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN,
					net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST};
				net.minecraft.client.renderer.block.BlockStateModelSet tSet = tMC.getModelManager().getBlockStateModelSet();
				// Инвариант B-цвета: FL-enum-жидкости (масла) несут цвет В ТЕКСТУРЕ → tint UNCOLOURED; расплавы (molten.X)
				// на generic-текстуре → tint = mRGBaLiquid (цвет материала). Статистика по ВСЕМ + примеры расплавов (цветные).
				int tTotal = 0, tColoured = 0, tRendered = 0, tTransl = 0;
				java.util.List<String> tMolten = new java.util.ArrayList<>();
				for (net.minecraft.world.level.block.Block bl : gregapi.data.FL.BLOCKS.values()) {
					if (!(bl instanceof gregapi.block.fluid.BlockBaseFluid tMF)) continue;
					tTotal++;
					String tNm = gregapi.data.FL.name(tMF.mFluid, false);
					gregapi.fluid.FluidGT tGT = gregapi.fluid.FluidGT.of(tMF.mFluid);
					short[] tRGBa = (tGT == null) ? null : tGT.getRGBa();
					boolean tColour = tRGBa != null && !((tRGBa[0] & 0xFF) == 255 && (tRGBa[1] & 0xFF) == 255 && (tRGBa[2] & 0xFF) == 255);
					if (tColour) tColoured++;
					// рендер + слой (первые 60 для скорости)
					if (tTotal <= 60) {
						net.minecraft.world.level.block.state.BlockState tS = tMF.defaultBlockState();
						java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> tParts = new java.util.ArrayList<>();
						tSet.get(tS).collectParts(tMC.level, tMC.player.blockPosition(), tS, net.minecraft.util.RandomSource.create(42L), tParts);
						int tQn = 0; boolean tT = false;
						for (net.minecraft.client.renderer.block.dispatch.BlockStateModelPart tp : tParts)
							for (net.minecraft.core.Direction d : tDirs) {
								java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> qs = tp.getQuads(d);
								if (qs != null) for (net.minecraft.client.resources.model.geometry.BakedQuad q : qs) { tQn++; if ("TRANSLUCENT".equals(String.valueOf(q.materialInfo().layer()))) tT = true; }
							}
						if (tQn > 0) tRendered++;
						if (tT) tTransl++;
					}
					if (tNm.contains("molten") && tMolten.size() < 5)
						tMolten.add(tNm + " rgba=" + (tRGBa == null ? "?" : (tRGBa[0] & 0xFF) + "," + (tRGBa[1] & 0xFF) + "," + (tRGBa[2] & 0xFF)));
				}
				ob5.println("[GT6-FLUID-PROBE] B5 ИТОГ: BlockBaseFluid=" + tTotal + " цветных-tint(≠белый)=" + tColoured
					+ " рендерятся(quads>0, из 60)=" + tRendered + " translucent(из 60)=" + tTransl);
				ob5.println("[GT6-FLUID-PROBE] B5 расплавы-примеры: " + tMolten);
			} catch (Throwable e) { ob5.println("[GT6-FLUID-PROBE] B5 упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
		}
		net.minecraft.server.MinecraftServer tSrv = tMC.getSingleplayerServer();
		if (tSrv == null) { mFluidProbePhase = 2; return; }
		++mFluidProbeTick;
		final java.io.PrintStream o = gregapi.data.CS.OUT;
		if (mFluidProbePhase == 0 && mFluidProbeTick >= 300) {
			mFluidProbePhase = 1;
			tSrv.execute(() -> { try {
				net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tW = tP.level();
				net.minecraft.core.BlockPos tC = tP.blockPosition();
				net.minecraft.world.level.block.Block tOcean = gregapi.data.CS.BlocksGT.Ocean;
				int tSet = 0;
				for (int dx=-1; dx<=1; dx++) for (int dz=-1; dz<=1; dz++) for (int dy=0; dy<=2; dy++) {
					net.minecraft.core.BlockPos tPp = tC.offset(dx, dy, dz);
					if (gregapi.util.WD.set(tW, tPp.getX(), tPp.getY(), tPp.getZ(), tOcean, 0, 3)) tSet++;
				}
				net.minecraft.world.level.block.state.BlockState tSt = tW.getBlockState(tC);
				o.println("[GT6-FLUID-PROBE] залито Ocean-блоков=" + tSet + " @центр " + tC.getX()+","+tC.getY()+","+tC.getZ()
					+ " block=" + tSt.getBlock().getClass().getSimpleName() + " fluidState.empty=" + tSt.getFluidState().isEmpty()
					+ " fluidType=" + (tSt.getFluidState().isEmpty() ? "-" : tSt.getFluidState().getType().getClass().getSimpleName())
					+ " isWaterTag=" + tSt.getFluidState().is(net.minecraft.tags.FluidTags.WATER));
					// B2-судья (content-жидкости BlockBaseFluid water/lava): getFluidState.is(WATER) → тег → эффекты (механизм доказан B1 Ocean).
					try {
						net.minecraft.world.level.block.Block tCW = null;
						for (net.minecraft.world.level.block.Block bl : gregapi.data.FL.BLOCKS.values()) if (bl instanceof gregapi.block.fluid.BlockBaseFluid bf && bf.getMaterial() == gregapi.block.Material.water) { tCW = bl; break; }
						if (tCW != null) {
							net.minecraft.core.BlockPos tCP = tC.offset(5, 0, 0);
							gregapi.util.WD.set(tW, tCP.getX(), tCP.getY(), tCP.getZ(), tCW, 7, 3);
							net.minecraft.world.level.block.state.BlockState tCS = tW.getBlockState(tCP);
							o.println("[GT6-FLUID-PROBE] B2 content-water=" + gregapi.data.FL.name(((gregapi.block.fluid.BlockBaseFluid)tCW).mFluid, false)
								+ " fluidState.empty=" + tCS.getFluidState().isEmpty() + " isWaterTag=" + tCS.getFluidState().is(net.minecraft.tags.FluidTags.WATER)
								+ " (isWaterTag=true = content-жидкость даёт погружение/утопление как B1)");
						} else o.println("[GT6-FLUID-PROBE] B2 content-water BlockBaseFluid не найден в FL.BLOCKS");
					} catch (Throwable e) { o.println("[GT6-FLUID-PROBE] B2 упал: " + e); }
					// B4-судья (слабы/waterlogging): Ocean.flowTo на STONE_SLAB → slab становится waterlogged (вода внутри, как mc26).
					try {
						net.minecraft.core.BlockPos tSlP = tC.offset(7, 0, 0);
						tW.setBlock(tSlP, net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState(), 3);
						boolean tWlBefore = tW.getBlockState(tSlP).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
						boolean tFlowed = tOcean instanceof gregtech.blocks.fluids.BlockWaterlike tOw && tOw.flowTo(tW, tSlP.getX(), tSlP.getY(), tSlP.getZ(), 0);
						net.minecraft.world.level.block.state.BlockState tSlSt = tW.getBlockState(tSlP);
						boolean tWlAfter = tSlSt.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
						o.println("[GT6-FLUID-PROBE] B4 slab=" + tSlSt.getBlock().getClass().getSimpleName() + " waterlogged " + tWlBefore + "→" + tWlAfter
							+ " flowTo=" + tFlowed + " fluidState.isWater=" + tSlSt.getFluidState().is(net.minecraft.tags.FluidTags.WATER)
							+ " (waterlogged=true = вода переживает слаб как mc26)");
					} catch (Throwable e) { o.println("[GT6-FLUID-PROBE] B4 упал: " + e); }
					// B3-судья (поглощение исходной воды): Ocean над vanilla WATER → updateTick → vanilla water поглощён в Ocean (как грег).
					try {
						net.minecraft.core.BlockPos tOP = tC.offset(9, 3, 0);
						gregapi.util.WD.set(tW, tOP.getX(), tOP.getY(), tOP.getZ(), tOcean, 0, 3);            // Ocean-source сверху
						tW.setBlock(tOP.below(), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3); // vanilla вода снизу
						boolean tWasWater = tW.getBlockState(tOP.below()).getBlock() == net.minecraft.world.level.block.Blocks.WATER;
						if (tOcean instanceof gregtech.blocks.fluids.BlockWaterlike) ((gregtech.blocks.fluids.BlockOcean) tOcean).updateTick(tW, tOP.getX(), tOP.getY(), tOP.getZ(), new java.util.Random());
						net.minecraft.world.level.block.Block tBelowAfter = tW.getBlockState(tOP.below()).getBlock();
						o.println("[GT6-FLUID-PROBE] B3 поглощение: под-vanilla-water=" + tWasWater + " → после updateTick below=" + tBelowAfter.getClass().getSimpleName()
							+ " поглощён=" + (tBelowAfter == tOcean) + " (поглощён=true = грег впитывает исходную воду mc26)");
					} catch (Throwable e) { o.println("[GT6-FLUID-PROBE] B3 упал: " + e); }
				} catch (Throwable e) { o.println("[GT6-FLUID-PROBE] заливка упала: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
			return;
		}
		if (mFluidProbePhase == 1 && mFluidProbeTick >= 340) {
			mFluidProbePhase = 2;
			tSrv.execute(() -> { try {
				net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
				o.println("[GT6-FLUID-PROBE] игрок: isInWater=" + tP.isInWater() + " isUnderWater=" + tP.isUnderWater()
					+ " eyeInWater=" + tP.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
					+ " fluidHeightWater=" + String.format("%.3f", tP.getFluidHeight(net.minecraft.tags.FluidTags.WATER))
					+ " air=" + tP.getAirSupply() + "/" + tP.getMaxAirSupply()
					+ " deltaY=" + String.format("%.4f", tP.getDeltaMovement().y)
					+ " (isInWater=true = движок видит GT6-воду как воду mc26 → погружение/push/утопление/плавание живы)");
			} catch (Throwable e) { o.println("[GT6-FLUID-PROBE] замер упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
			return;
		}
	}

	// П1-СУДЬЯ (F14-gui, гейт: файл run/gt6guiprobe.flag): авто-открытие GUI машины РЕАЛЬНЫМ путём — сервер-тред
	// размещает машину предметом (onItemUse, тот же код, что клик игрока) → ITileEntityGUI.openGUI (openMenu → пакет →
	// клиент-экран) → замер экрана (класс/фон/размеры/слоты) + счётчики отрисовки ContainerClient (blit/text per frame).
	private int mGuiProbePhase = 0; private int mGuiProbeTick = 0; private long mGuiProbeBlit0 = 0, mGuiProbeText0 = 0;
	private net.minecraft.core.BlockPos mGuiProbePos = null;
	@net.neoforged.bus.api.SubscribeEvent
	public void onGuiProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mGuiProbePhase >= 4) return;
		if (!new java.io.File("gt6guiprobe.flag").exists()) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) return;
		++mGuiProbeTick;
		java.io.PrintStream o = gregapi.data.CS.OUT;
		net.minecraft.server.MinecraftServer tSrv = tMC.getSingleplayerServer();
		if (tSrv == null) { mGuiProbePhase = 4; return; }
		if (mGuiProbePhase == 0 && mGuiProbeTick >= 300) {
			mGuiProbePhase = 1;
			tSrv.execute(() -> { try {
				net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tW = tP.level();
				gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
				net.minecraft.core.BlockPos tPP = tP.blockPosition();
				// машина (GUI-судья) + сундук 32745 и mass-storage (П2-судья спец-рендеров) в ряд
				int[] tIDs = {20001, 32745};
				for (int i = 0; i < tIDs.length; i++) {
					net.minecraft.world.item.ItemStack tS = tReg.getItem(tIDs[i]);
					int tX = tPP.getX()+2, tZ = tPP.getZ()+i*2, tY = tPP.getY();
					while (tY > tW.getMinY()+1 && tW.getBlockState(new net.minecraft.core.BlockPos(tX, tY-1, tZ)).isAir()) tY--;
					boolean tPlaced = gregapi.util.ST.valid(tS) && tS.getItem() instanceof gregapi.block.multitileentity.MultiTileEntityItemInternal tItem
						&& tItem.onItemUse(tS, tP, tW, tX, tY-1, tZ, gregapi.data.CS.SIDE_TOP, 0.5f, 1.0f, 0.5f);
					if (i == 0) mGuiProbePos = new net.minecraft.core.BlockPos(tX, tY, tZ);
					o.println("[GT6-GUI-PROBE] place id=" + tIDs[i] + " → " + tPlaced + " @" + tX + "," + tY + "," + tZ);
				}
				// П4-замер (жидкости, до правок): place полного fluid-блока + мета/FluidState/текстура-резолв
				try {
					java.util.Iterator<java.util.Map.Entry<String, net.minecraft.world.level.block.Block>> tIt = gregapi.data.FL.BLOCKS.entrySet().iterator();
					if (tIt.hasNext()) {
						java.util.Map.Entry<String, net.minecraft.world.level.block.Block> tE = tIt.next();
						int fX = tPP.getX()+4, fZ = tPP.getZ(), fY = tPP.getY();
						while (fY > tW.getMinY()+1 && tW.getBlockState(new net.minecraft.core.BlockPos(fX, fY-1, fZ)).isAir()) fY--;
						boolean tFSet = gregapi.util.WD.set(tW, fX, fY, fZ, tE.getValue(), 7, 3);
						net.minecraft.world.level.block.state.BlockState tFS = tW.getBlockState(new net.minecraft.core.BlockPos(fX, fY, fZ));
						Object tTex = null; try { tTex = ((gregapi.render.IRenderedBlock)tE.getValue()).getTexture(0, (byte)1, new boolean[]{true,true,true,true,true,true}, tW, fX, fY, fZ); } catch (Throwable e) { tTex = "EXC:"+e; }
						String tIconInfo = "?";
						try {
							gregapi.fluid.FluidGT tFG = gregapi.fluid.FluidGT.of(((gregapi.block.fluid.BlockBaseFluid)tE.getValue()).mFluid);
							net.minecraft.resources.Identifier tIcon = tFG == null || tFG.mTexture == null ? null : tFG.mTexture.getIcon(0);
							net.minecraft.client.renderer.texture.TextureAtlasSprite tSpr = tIcon == null ? null : gregapi.render.GT6QuadBuilder.resolveSprite(tIcon, net.minecraft.data.AtlasIds.BLOCKS);
							tIconInfo = "icon=" + tIcon + " sprite=" + (tSpr == null ? "PURPLE!" : "VALID");
						} catch (Throwable e) { tIconInfo = "EXC:" + e; }
						o.println("[GT6-FLUID-PROBE] fluid=" + tE.getKey() + " set=" + tFSet + " state=" + tFS.getBlock().getClass().getSimpleName()
							+ " meta=" + gregapi.util.WD.meta(tW, fX, fY, fZ) + " fluidState.empty=" + tFS.getFluidState().isEmpty()
							+ " tex=" + (tTex == null ? "NULL" : tTex.getClass().getSimpleName()) + " " + tIconInfo + " @" + fX + "," + fY + "," + fZ);
					} else o.println("[GT6-FLUID-PROBE] FL.BLOCKS пуст");
				} catch (Throwable e) { o.println("[GT6-FLUID-PROBE] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
			} catch (Throwable e) { o.println("[GT6-GUI-PROBE] place-фаза упала: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
			return;
		}
		// открытие ОТДЕЛЬНОЙ фазой (клиент-BE успевает синкнуться после place; race закрыт и центром — null-заглушка)
		if (mGuiProbePhase == 1 && mGuiProbeTick >= 340) {
			mGuiProbePhase = 2;
			final net.minecraft.core.BlockPos fPos = mGuiProbePos;
			if (fPos == null) { mGuiProbePhase = 4; return; }
			tSrv.execute(() -> { try {
				net.minecraft.server.level.ServerPlayer tP = tSrv.getPlayerList().getPlayers().get(0);
				net.minecraft.world.level.block.entity.BlockEntity tBE = tP.level().getBlockEntity(fPos);
				boolean tOpened = tBE instanceof gregapi.tileentity.ITileEntityGUI tGUI && tGUI.openGUI(tP);
				o.println("[GT6-GUI-PROBE] BE=" + (tBE == null ? "null" : tBE.getClass().getSimpleName()) + " openGUI=" + tOpened);
				// П8-судья (каверы): синтез кавера на машине реальным центром setCoverItem → рендер-пассы должны вырасти на 12
				try {
					if (tBE instanceof gregapi.tileentity.base.TileEntityBase06Covers tCov) {
						int tPassesBefore = tCov instanceof gregapi.render.IRenderedBlockObject tRO ? tRO.getRenderPasses(tBE.getBlockState().getBlock(), new boolean[]{true,true,true,true,true,true}) : -1;
						net.minecraft.world.item.ItemStack tCoverStack = null;
						for (java.util.Map.Entry<gregapi.code.ItemStackContainer, gregapi.cover.ICover> tE : gregapi.cover.CoverRegistry.COVERS.entrySet()) {tCoverStack = tE.getKey().toStack(); if (gregapi.util.ST.valid(tCoverStack)) break;}
						boolean tSet = tCoverStack != null && tCov.setCoverItem((byte)2, tCoverStack, tP, true, true);
						int tPassesAfter = tCov instanceof gregapi.render.IRenderedBlockObject tRO2 ? tRO2.getRenderPasses(tBE.getBlockState().getBlock(), new boolean[]{true,true,true,true,true,true}) : -1;
						o.println("[GT6-COVER-PROBE] cover=" + (tCoverStack == null ? "нет-в-реестре" : tCoverStack.getHoverName().getString()) + " set=" + tSet + " passes " + tPassesBefore + "→" + tPassesAfter + " (+12 = кавер-пассы живы)");
					}
				} catch (Throwable e) { o.println("[GT6-COVER-PROBE] упал: " + e); }
				// П8-судья (mActive-анимация): форс mActive → текстура пасса меняется (getTexture2 active-набор)
				try {
					if (tBE instanceof gregapi.tileentity.machines.MultiTileEntityBasicMachine tM) {
						boolean[] tAll = {true,true,true,true,true,true};
						Object tTexIdle = tM.getTexture2(tBE.getBlockState().getBlock(), 1, (byte)3, tAll);
						tM.mActive = true;
						Object tTexActive = tM.getTexture2(tBE.getBlockState().getBlock(), 1, (byte)3, tAll);
						tM.mActive = false;
						o.println("[GT6-ACTIVE-PROBE] текстура idle==" + (tTexIdle == null ? "null" : "ok") + " active==" + (tTexActive == null ? "null" : "ok") + " различаются=" + (tTexIdle != null && !String.valueOf(tTexIdle).equals(String.valueOf(tTexActive))) + " (true = mActive-канал жив)");
					}
				} catch (Throwable e) { o.println("[GT6-ACTIVE-PROBE] упал: " + e); }
			} catch (Throwable e) { o.println("[GT6-GUI-PROBE] open-фаза упала: " + e); e.printStackTrace(gregapi.data.CS.ERR); } });
			return;
		}
		if (mGuiProbePhase == 2 && mGuiProbeTick >= 400) {
			mGuiProbePhase = 3;
			net.minecraft.client.gui.screens.Screen tScr = tMC.screen;
			if (tScr instanceof gregapi.gui.ContainerClient tCC) {
				o.println("[GT6-GUI-PROBE] экран=" + tScr.getClass().getSimpleName() + " фон=" + tCC.mBackground + " size=" + tCC.getLeft() + "," + tCC.getTop() + " слотов=" + tCC.getMenu().slots.size());
			} else {
				o.println("[GT6-GUI-PROBE] экран НЕ ContainerClient: " + (tScr == null ? "null" : tScr.getClass().getName()));
			}
			mGuiProbeBlit0 = gregapi.gui.ContainerClient.sBlitCalls.get();
			mGuiProbeText0 = gregapi.gui.ContainerClient.sTextCalls.get();
			o.println("[GT6-GUI-PROBE] счётчики@400: blit=" + mGuiProbeBlit0 + " text=" + mGuiProbeText0);
			return;
		}
		if (mGuiProbePhase == 3 && mGuiProbeTick >= 460) {
			mGuiProbePhase = 4;
			long tB = gregapi.gui.ContainerClient.sBlitCalls.get(), tT = gregapi.gui.ContainerClient.sTextCalls.get();
			o.println("[GT6-GUI-PROBE] счётчики@460: blit=" + tB + " (Δ" + (tB-mGuiProbeBlit0) + ") text=" + tT + " (Δ" + (tT-mGuiProbeText0) + ") — Δ>0 = движок рисует фон/текст каждый кадр");
			o.println("[GT6-SPECIAL-PROBE] спец-рендеры (Chest/MassStorage): extract=" + gregapi.render.MultiTileEntityBER.sSpecialExtract.get() + " submit=" + gregapi.render.MultiTileEntityBER.sSpecialSubmit.get() + " itemForm=" + gregapi.render.MultiTileEntityBER.sSpecialItemForm.get() + " — >0 = BER-диспетч/спец-item-форма живы");
			// П4-судья (растекание): через ~8с после заливки полного кванта — счёт fluid-блоков и мет вокруг точки
			try {
				net.minecraft.server.MinecraftServer tS2 = tMC.getSingleplayerServer();
				if (tS2 != null && mGuiProbePos != null) tS2.execute(() -> {
					net.minecraft.server.level.ServerLevel tW = tS2.getPlayerList().getPlayers().get(0).level();
					net.minecraft.core.BlockPos tC = mGuiProbePos.offset(2, 0, 0);
					int tCount = 0; StringBuilder tMetas = new StringBuilder();
					for (int dx=-4; dx<=4; dx++) for (int dz=-4; dz<=4; dz++) for (int dy=-2; dy<=1; dy++) {
						net.minecraft.core.BlockPos tPp = tC.offset(dx, dy, dz);
						if (tW.getBlockState(tPp).getBlock() instanceof gregapi.block.fluid.BlockBaseFluid) {
							tCount++;
							if (tCount <= 12) tMetas.append(dx).append(",").append(dy).append(",").append(dz).append(":m").append(gregapi.util.WD.meta(tW, tPp.getX(), tPp.getY(), tPp.getZ())).append(" ");
						}
					}
					gregapi.data.CS.OUT.println("[GT6-FLUID-PROBE] растекание: fluid-блоков=" + tCount + " меты: " + tMetas + " (1 блок с метой 7 = мета жива; >1 блока = поток жив)");
					gregapi.data.CS.OUT.println("[GT6-WG-PROBE] placeBlock-откаты: гейт1(до BE)=" + gregapi.block.multitileentity.MultiTileEntityBlockInternal.sPlaceAbort1.get() + " гейт2(после BE)=" + gregapi.block.multitileentity.MultiTileEntityBlockInternal.sPlaceAbort2.get());
					gregapi.data.CS.OUT.println("[GT6-WG-PROBE] статусы чанков-приёмников worldgen-BE: " + gregapi.util.WD.sWgBEStatus);
					int tShown = 0;
					for (Object[] tS : gregapi.util.WD.sWgBESamples) {
						if (++tShown > 25) break;
						net.minecraft.core.BlockPos tBP = (net.minecraft.core.BlockPos)tS[0];
						String tNow = tW.hasChunkAt(tBP) ? String.valueOf(tW.getBlockState(tBP).getBlock()) : "чанк-не-загружен";
						gregapi.data.CS.OUT.println("[GT6-WG-PROBE] BE " + tS[1] + " @" + tBP.toShortString() + " (" + tS[3] + "): при-записи=" + tS[2] + " сейчас=" + tNow);
					}
				});
			} catch (Throwable e) { o.println("[GT6-FLUID-PROBE] финал упал: " + e); }
		}
	}

	// F-tileentity-construction (КЛИЕНТ-реконструкция MTE-BE): neo подменяет не-PrefixBlock GT6-MTE общим MTE_TYPE →
	// TileEntityLoaderStub при десериализации BE чанка НА КЛИЕНТЕ. Стаб — не IRenderedBlockObject → passRenderingToObject=null
	// → getRenderPasses=0 → MTE-блок НЕ рисуется (прозрачный: камни/палки/флюид-источники/машины). Серверная реконструкция
	// (server-tick) клиент не покрывает — у него ОТДЕЛЬНЫЕ BE. Здесь дренируем клиентскую очередь стабов на client-tick,
	// заменяя их настоящими MTE (единый механизм GT6WorldgenFeature.reconstructChunkMTEs, теперь Level-обобщённый).
	@net.neoforged.bus.api.SubscribeEvent
	public void onClientMTEReconstruct(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (Minecraft.getInstance().level == null) return;
		try { gregapi.worldgen.GT6WorldgenFeature.drainClientStubs(); } catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
	}

	// F6-worldgen приёмка гейт② (АВТОНОМНАЯ, не нужен пользователь): после входа в мир сканируем объём вокруг игрока —
	// (1) сгенерировал ли worldgen GT6-блоки (руды/камни/флюиды/MTE) на КЛИЕНТСКОМ интегрированном сервере;
	// (2) РЕЗОЛВИТСЯ ли материал руды на КЛИЕНТЕ (getMetaMaterial(BE)!=null = цветное вкрапление; null = серое «в прогрузке»).
	// Ждём ~200 тиков (10с): чанки загружены, BE-синк mMetaData дошёл. Пишет в gregtech.log (game-bus). Once.
	private int mOreProbeTick = -1;
	private int mOreProbeRuns = 0;
	private boolean mFluidDiagDone = false;
	@net.neoforged.bus.api.SubscribeEvent
	public void onOreMaterialProbe(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (!new java.io.File("gt6probe.flag").exists()) return; // debug (ore/icons/engine-дамп + форс-открытие инвентаря) — ВЫКЛ по умолчанию
		if (mOreProbeRuns >= 2) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (tMC.level == null || tMC.player == null) { mOreProbeTick = -1; return; }
		if (mOreProbeTick < 0) mOreProbeTick = 0;
		++mOreProbeTick;
		// Двухфазный: фаза 1 на 200 тиках, фаза 2 на 1000 тиках — сравнить srvBE null-BE MTE (тайминг серверного BE vs постоянная потеря).
		if (mOreProbeRuns == 0 && mOreProbeTick < 200) return;
		if (mOreProbeRuns == 1 && mOreProbeTick < 1000) return;
		mOreProbeRuns++;
		gregapi.data.CS.OUT.println("[GT6-ORE-PROBE] === ФАЗА "+mOreProbeRuns+" (тик "+mOreProbeTick+") ===");
		try { probeOreMaterials(tMC); } catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-ORE-PROBE] скан упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
	}

	private void probeOreMaterials(net.minecraft.client.Minecraft tMC) {
		net.minecraft.world.level.Level tLevel = tMC.level;
		net.minecraft.core.BlockPos tP = tMC.player.blockPosition();
		java.io.PrintStream tOut = gregapi.data.CS.OUT;
		int tOreTotal=0, tOreResolved=0, tOreGrey=0, tOreNoBE=0, tStoneGT=0, tFluidGT=0, tMTE=0;
		java.util.HashMap<String,Integer> tMatCounts = new java.util.HashMap<>();
		java.util.HashMap<String,Integer> tMTEs = new java.util.HashMap<>();
		java.util.List<String> tNullBE = new java.util.ArrayList<>();
		int tMinY = tLevel.getMinY(), tMaxScanY = tP.getY()+4;
		net.minecraft.core.BlockPos.MutableBlockPos tM = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (int dx=-40; dx<=40; dx++) for (int dz=-40; dz<=40; dz++) for (int y=tMinY; y<=tMaxScanY; y++) {
			tM.set(tP.getX()+dx, y, tP.getZ()+dz);
			net.minecraft.world.level.block.Block tB = tLevel.getBlockState(tM).getBlock();
			if (tB instanceof gregapi.block.prefixblock.PrefixBlock tPB) {
				tOreTotal++;
				net.minecraft.world.level.block.entity.BlockEntity tBE = tLevel.getBlockEntity(tM);
				if (!(tBE instanceof gregapi.block.prefixblock.PrefixBlockTileEntity)) { tOreNoBE++; continue; }
				gregapi.oredict.OreDictMaterial tMat = tPB.getMetaMaterial(tBE);
				if (tMat == null) tOreGrey++; else { tOreResolved++; tMatCounts.merge(tMat.mNameInternal, 1, Integer::sum); }
			} else if (tB instanceof gregapi.block.metatype.BlockStones) tStoneGT++;
			else if (tB instanceof gregapi.block.fluid.BlockBaseFluid) tFluidGT++;
			else if (tB instanceof gregapi.block.multitileentity.MultiTileEntityBlock) {
				tMTE++;
				net.minecraft.world.level.block.entity.BlockEntity tBE = tLevel.getBlockEntity(tM);
				tMTEs.merge(tBE==null?"(null-BE)":tBE.getClass().getSimpleName(), 1, Integer::sum);
				if (tBE==null && tNullBE.size()<8) {
					// srvBE: есть ли BE на интегрированном СЕРВЕРЕ в той же позиции? != null → потеря на sync сервер→клиент; == null → сервер не имеет BE (gen/промоушен ProtoChunk→LevelChunk).
					String tSrv = "?"; try { net.minecraft.server.MinecraftServer tSrvMc = tMC.getSingleplayerServer(); if (tSrvMc != null) { net.minecraft.world.level.block.entity.BlockEntity tSB = tSrvMc.overworld().getBlockEntity(tM.immutable()); tSrv = tSB==null?"null":tSB.getClass().getSimpleName(); } } catch (Throwable e) { tSrv = "ERR:"+e; }
					tNullBE.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tB)+"@Y"+y+" srvBE="+tSrv);
				}
			}
		}
		tOut.println("[GT6-ORE-PROBE] pos=" + tP + " scan=±40xz Y[" + tMinY + ".." + tMaxScanY + "]");
		tOut.println("[GT6-ORE-PROBE] РУДЫ: total=" + tOreTotal + " resolved(цвет)=" + tOreResolved + " grey(material=null)=" + tOreGrey + " noBE=" + tOreNoBE);
		tOut.println("[GT6-ORE-PROBE] WORLDGEN-блоки: GT6-камень=" + tStoneGT + " GT6-флюид=" + tFluidGT + " MTE=" + tMTE);
		tMTEs.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue()).forEach(e ->
			tOut.println("[GT6-ORE-PROBE]   MTE " + e.getKey() + " = " + e.getValue()));
		if (!tNullBE.isEmpty()) tOut.println("[GT6-ORE-PROBE]   null-BE блоки: " + tNullBE);
		// ЦЕЛЕВОЙ скан ИСТОЧНИКОВ: широкий бедрок-слой (±100, Y дно..дно+3) — источники редки, у спавна нет. Считаем FluidSpring + состояние BE/srvBE.
		try { int tSpring=0, tSpringBE=0, tSpringNull=0; java.util.List<String> tSp = new java.util.ArrayList<>();
		  net.minecraft.server.MinecraftServer tSrvMc = tMC.getSingleplayerServer();
		  // блок ИСТОЧНИКА = блок его class-контейнера (aStone), НЕ registry-default mBlock (разные!).
		  net.minecraft.world.level.block.Block tSpringBlock = null;
		  try { var tReg = gregtech.tileentity.misc.MultiTileEntityFluidSpring.MTE_REGISTRY; var tInst = gregtech.tileentity.misc.MultiTileEntityFluidSpring.INSTANCE;
		        if (tReg != null && tInst != null) { var tCC = tReg.getClassContainer(tInst.getMultiTileEntityID()); if (tCC != null) tSpringBlock = tCC.mBlock; } } catch (Throwable e) {}
		  for (int dx=-100; dx<=100 && tSpringBlock!=null; dx++) for (int dz=-100; dz<=100; dz++) for (int y=tMinY; y<=tMinY+3; y++) {
			tM.set(tP.getX()+dx, y, tP.getZ()+dz);
			if (tLevel.getBlockState(tM).getBlock() == tSpringBlock) { tSpring++;
				net.minecraft.world.level.block.entity.BlockEntity tBE = tLevel.getBlockEntity(tM);
				if (tBE instanceof gregtech.tileentity.misc.MultiTileEntityFluidSpring) tSpringBE++; else { tSpringNull++;
					if (tSp.size()<6) { String tSrv="?"; try{ if(tSrvMc!=null){var s=tSrvMc.overworld().getBlockEntity(tM.immutable()); tSrv=s==null?"null":s.getClass().getSimpleName();}}catch(Throwable e){} tSp.add("@Y"+y+" clientBE="+(tBE==null?"null":tBE.getClass().getSimpleName())+" srvBE="+tSrv); } }
			}
		  }
		  tOut.println("[GT6-ORE-PROBE] ИСТОЧНИКИ(spring) ±100 бедрок: всего="+tSpring+" настоящий-BE="+tSpringBE+" плохой-BE="+tSpringNull);
		  if (!tSp.isEmpty()) tOut.println("[GT6-ORE-PROBE]   плохие источники: "+tSp);
		} catch (Throwable e) { tOut.println("[GT6-ORE-PROBE] spring-scan упал: "+e); }
		tMatCounts.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue()).limit(12).forEach(e ->
			tOut.println("[GT6-ORE-PROBE]   материал " + e.getKey() + " = " + e.getValue()));
		if (mOreProbeRuns == 1) try { probeIconsHonest(tMC); } catch (Throwable e) { tOut.println("[GT6-ICONS] скан упал: "+e); e.printStackTrace(gregapi.data.CS.ERR); }
		if (mOreProbeRuns == 1) try { dumpEngineState(tMC); } catch (Throwable e) { tOut.println("[GT6-ENGINE] дамп упал: "+e); e.printStackTrace(gregapi.data.CS.ERR); }
	}

	// ЧЕСТНЫЙ icon-замер = что neo РЕАЛЬНО рисует (ItemModelResolver.updateForTopItem, та же модель, что в креативе/JEI/руке) по ВСЕМ
	// вариантам ВКЛЮЧАЯ block-предметы: isEmpty()=невидим, particle==missing=ПУРПУР. Единственный датчик, совпавший с глазами пользователя.
	private void probeIconsHonest(net.minecraft.client.Minecraft tMC) {
		java.io.PrintStream o = gregapi.data.CS.OUT;
		net.minecraft.client.renderer.item.ItemModelResolver tResolver = tMC.getItemModelResolver();
		net.minecraft.util.RandomSource tRnd = net.minecraft.util.RandomSource.create(0L);
		net.minecraft.client.renderer.texture.TextureAtlasSprite tMissI=null, tMissB=null;
		try { tMissI = tMC.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.ITEMS).missingSprite(); } catch (Throwable e) {}
		try { tMissB = tMC.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).missingSprite(); } catch (Throwable e) {}
		int[] tNB=new int[4], tBI=new int[4]; // [total, invisible, purple, valid]
		java.util.HashSet<String> tDNB=new java.util.HashSet<>(), tDBI=new java.util.HashSet<>();
		java.util.HashSet<String> tIdent=new java.util.HashSet<>(); // РАЗЛИЧНЫЕ model-identity (ключ GUI-кэша): мало = одна иконка всем
		java.util.List<String> tBadNB=new java.util.ArrayList<>(), tBadBI=new java.util.ArrayList<>(), tOkBI=new java.util.ArrayList<>();
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			net.minecraft.resources.Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey==null || !isGregNamespace(tKey.getNamespace())) continue;
			boolean tIsBlock = tItem instanceof net.minecraft.world.item.BlockItem;
			int[] tC = tIsBlock?tBI:tNB; java.util.HashSet<String> tDS = tIsBlock?tDBI:tDNB; java.util.List<String> tBad = tIsBlock?tBadBI:tBadNB;
			java.util.List<net.minecraft.world.item.ItemStack> tVars = new java.util.ArrayList<>();
			try { java.lang.reflect.Method gsi = tItem.getClass().getMethod("getSubItems", net.minecraft.world.item.Item.class, net.minecraft.world.item.CreativeModeTab.class, java.util.List.class); gsi.invoke(tItem, tItem, null, tVars); } catch (Throwable e) {}
			if (tVars.isEmpty()) tVars.add(new net.minecraft.world.item.ItemStack(tItem));
			for (net.minecraft.world.item.ItemStack tStack : tVars) {
				tC[0]++;
				try {
					net.minecraft.client.renderer.item.TrackingItemStackRenderState tRS = new net.minecraft.client.renderer.item.TrackingItemStackRenderState();
					tResolver.updateForTopItem(tRS, tStack, net.minecraft.world.item.ItemDisplayContext.GUI, tMC.level, null, 0);
					try { tIdent.add(String.valueOf(tRS.getModelIdentity())); } catch (Throwable e) {}
					if (tRS.isEmpty()) { tC[1]++; if (tBad.size()<10) tBad.add(tKey.getPath()+"[пусто]"); continue; }
					net.minecraft.client.resources.model.sprite.Material.Baked tPM = tRS.pickParticleMaterial(tRnd);
					net.minecraft.client.renderer.texture.TextureAtlasSprite tSp = tPM==null?null:tPM.sprite();
					if (tSp==null || tSp==tMissI || tSp==tMissB) { tC[2]++; if (tBad.size()<10) tBad.add(tKey.getPath()+"[missing]"); }
					else { tC[3]++; tDS.add(tSp.contents().name().toString()); if (tIsBlock && tOkBI.size()<10) tOkBI.add(tKey.getPath()); }
				} catch (Throwable e) { tC[2]++; if (tBad.size()<10) tBad.add(tKey.getPath()+"[EXC:"+e.getClass().getSimpleName()+"]"); }
			}
		}
		o.println("[GT6-ICONS] РАЗЛИЧНЫХ model-identity (ключ GUI-кэша иконок)="+tIdent.size()+" из "+(tNB[0]+tBI[0])+" вариантов  (мало=одна иконка всем; ≈вариантам=у каждого своя)");
		o.println("[GT6-ICONS] НЕ-БЛОК предметы: вариантов="+tNB[0]+" невидимых="+tNB[1]+" ПУРПУР="+tNB[2]+" валидных="+tNB[3]+" разл-спрайтов="+tDNB.size());
		o.println("[GT6-ICONS] BLOCK-предметы:  вариантов="+tBI[0]+" невидимых="+tBI[1]+" ПУРПУР="+tBI[2]+" валидных="+tBI[3]+" разл-спрайтов="+tDBI.size());
		if (!tOkBI.isEmpty()) o.println("[GT6-ICONS] block ОК-примеры: "+tOkBI);
		if (!tBadBI.isEmpty()) o.println("[GT6-ICONS] block битые: "+tBadBI);
		if (!tBadNB.isEmpty()) o.println("[GT6-ICONS] не-блок битые: "+tBadNB);
	}

	// ЗАМЕР СОСТОЯНИЯ В ИГРЕ (движковые данные, не визуал): сколько GT6-контента реально ЗАРЕГИСТРИРОВАНО, в КРЕАТИВЕ, в
	// RecipeManager. Ответ на «почему пусто в креативе» ЧИСЛАМИ, не предположением. (иконки — существующий item-probe отдельно.)
	private void dumpEngineState(net.minecraft.client.Minecraft tMC) {
		java.io.PrintStream o = gregapi.data.CS.OUT;
		o.println("[GT6-ENGINE] ================= ЗАМЕР СОСТОЯНИЯ В ИГРЕ =================");
		// Ф1.3: форс-триггер полного старта JEI — в headless авто-мире JEI стартует только при открытии экрана
		// (StartEventObserver), потому registerCategories/registerRecipes GT6_JEI_Plugin иначе не отрабатывают и не логируются.
		// Открываем инвентарь-экран → JEI регистрирует категории (успех/ошибку залогирует сама JEI в modloading-worker).
		try { tMC.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(tMC.player)); o.println("[GT6-ENGINE] JEI-триггер: инвентарь-экран открыт (форс-старт JEI)"); }
		catch (Throwable e) { o.println("[GT6-ENGINE] JEI-триггер упал: " + e); }
		// 1. РЕГИСТРАЦИЯ: сколько gregtech-предметов/блоков реально в реестрах движка.
		int tItems=0, tBlocks=0;
		for (net.minecraft.world.item.Item it : net.minecraft.core.registries.BuiltInRegistries.ITEM) if (isGregNamespace(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).getNamespace())) tItems++;
		for (net.minecraft.world.level.block.Block bl : net.minecraft.core.registries.BuiltInRegistries.BLOCK) if (isGregNamespace(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bl).getNamespace())) tBlocks++;
		o.println("[GT6-ENGINE] 1. РЕГИСТРАЦИЯ: gregtech ITEM=" + tItems + "  BLOCK=" + tBlocks);
		// 1b. BLOCKITEM: сколько gregtech-блоков имеют item-форму (asItem()!=AIR). Нет item → блок не в креативе, нельзя взять/поставить.
		int tBlkWithItem=0, tBlkNoItem=0;
		for (net.minecraft.world.level.block.Block bl : net.minecraft.core.registries.BuiltInRegistries.BLOCK) { if (!isGregNamespace(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bl).getNamespace())) continue; if (bl.asItem() == net.minecraft.world.item.Items.AIR) tBlkNoItem++; else tBlkWithItem++; }
		o.println("[GT6-ENGINE] 1b. BLOCKITEM: gregtech-блоков С item-формой=" + tBlkWithItem + "  БЕЗ item (asItem=AIR)=" + tBlkNoItem);
		// 1c. РЕАЛЬНЫЙ КОНТЕНТ = ВАРИАНТЫ (getSubItems/getSubBlocks), не база-реестр. GT6 мета-модель: 1 MultiItem = тысячи вариантов
		// (материал×префикс), 1 MTE-блок = тысячи по ID. Это десятки/сотни тысяч. Если тут ~847 (только база) — генерация вариантов СЛОМАНА.
		long tVarItems=0, tVarBlocks=0; int tItemMulti=0, tBlockMulti=0;
		for (net.minecraft.world.item.Item it : net.minecraft.core.registries.BuiltInRegistries.ITEM) { if (!isGregNamespace(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).getNamespace())) continue; int n = countSub(it, "getSubItems", it); tVarItems += Math.max(n,1); if (n>1) tItemMulti++; }
		for (net.minecraft.world.level.block.Block bl : net.minecraft.core.registries.BuiltInRegistries.BLOCK) { if (!isGregNamespace(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bl).getNamespace())) continue; int n = countSub(bl, "getSubBlocks", bl.asItem()); if (n==0) n = countSub(bl.asItem(), "getSubItems", bl.asItem()); tVarBlocks += Math.max(n,1); if (n>1) tBlockMulti++; }
		o.println("[GT6-ENGINE] 1c. ВАРИАНТЫ (реальный контент): ITEM-вариантов=" + tVarItems + " (мульти-предметов="+tItemMulti+")  BLOCK-вариантов=" + tVarBlocks + " (мульти-блоков="+tBlockMulti+")  ИТОГО="+(tVarItems+tVarBlocks));
		// Ф1.1 DIAG: в auto-world креатив-инвентарь не открывался → tryRebuildTabContents движком не вызывался → все табы пусты.
		// Форсируем построение содержимого, чтобы судить МЕХАНИЗМ наполнения (onBuildContents/populate), а не тайминг открытия меню.
		try { net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(tMC.level.enabledFeatures(), true, tMC.level.registryAccess()); }
		catch (Throwable e) { o.println("[GT6-ENGINE] 2-FORCE tryRebuildTabContents упал: " + e); }
		// 2. КРЕАТИВ: сколько gregtech-предметов реально попадает в creative-вкладки (getDisplayItems после BuildContents).
		// Ф1.2: тут же — имена РЕАЛЬНО ОТОБРАЖАЕМЫХ вариантов (не базовых стеков): getHoverName читаемое или сырой ключ.
		int tTabsTotal=0, tTabsGreg=0, tCreativeGreg=0, tCreativeAll=0, tVarNameOk=0, tVarNameRaw=0, tVarShown=0; StringBuilder tVarRawSamples = new StringBuilder();
		for (net.minecraft.world.item.CreativeModeTab tab : net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB) { tTabsTotal++;
			var tk = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab); if (tk != null && isGregNamespace(tk.getNamespace())) tTabsGreg++;
			try { for (net.minecraft.world.item.ItemStack st : tab.getDisplayItems()) { tCreativeAll++;
				if (isGregNamespace(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getNamespace())) { tCreativeGreg++;
					try { String nm = st.getHoverName().getString(); boolean tRaw = nm==null||nm.isEmpty()||nm.contains("gt.")||nm.startsWith("item.")||nm.startsWith("block.");
						if (tRaw) { tVarNameRaw++; if (tVarShown<8) { tVarRawSamples.append(" [").append(nm).append("]"); tVarShown++; } } else tVarNameOk++;
					} catch (Throwable e) { tVarNameRaw++; }
				}
			} } catch (Throwable e) {}
		}
		o.println("[GT6-ENGINE] 2. КРЕАТИВ: всего-вкладок=" + tTabsTotal + " gregtech-вкладок=" + tTabsGreg + " gregtech-предметов-в-табах=" + tCreativeGreg + " ВСЕГО-предметов-во-всех-табах=" + tCreativeAll);
		o.println("[GT6-ENGINE] 2b. ИМЕНА ВАРИАНТОВ (отображаемых в креативе): читаемых=" + tVarNameOk + " сырой-ключ=" + tVarNameRaw + " сырые-примеры:" + tVarRawSamples);
		// 3. РЕЦЕПТЫ в игре (server RecipeManager).
		try { net.minecraft.server.MinecraftServer srv = tMC.getSingleplayerServer();
			if (srv == null) o.println("[GT6-ENGINE] 3. РЕЦЕПТЫ: singleplayer-сервера нет");
			else { int tRec=0, tRecGreg=0;
				for (net.minecraft.world.item.crafting.RecipeHolder<?> rh : srv.getRecipeManager().getRecipes()) { tRec++; if (isGregNamespace(rh.id().identifier().getNamespace())) tRecGreg++; }
				o.println("[GT6-ENGINE] 3. РЕЦЕПТЫ: всего в RecipeManager=" + tRec + " gregtech=" + tRecGreg);
			}
		} catch (Throwable e) { o.println("[GT6-ENGINE] 3. РЕЦЕПТЫ (neo RecipeManager): скан упал=" + e); }
		// 4. GT6-RECIPEMAP-система (машинные рецепты — мацератор/ассемблер/EBF/химреактор — это ~300k, СВОЯ система GT6, не neo RecipeManager).
		try { int tMaps=0; long tGT6Rec=0; int tNonEmpty=0;
			for (gregapi.recipes.Recipe.RecipeMap rm : gregapi.recipes.Recipe.RecipeMap.RECIPE_MAP_LIST) { tMaps++; int s = rm.mRecipeList.size(); tGT6Rec += s; if (s>0) tNonEmpty++; }
			o.println("[GT6-ENGINE] 4. GT6-RECIPEMAPS (машины, СВОЯ система): карт=" + tMaps + " непустых=" + tNonEmpty + " ВСЕГО-РЕЦЕПТОВ=" + tGT6Rec);
		} catch (Throwable e) { o.println("[GT6-ENGINE] 4. GT6-RECIPEMAPS: скан упал=" + e); }
		// 4b. JEI (Ф1.3): сколько категорий/рецептов зарегистрирует GT6_JEI_Plugin — та же логика (mNEIAllowed && непустая getNEIAllRecipes).
		try { int tJeiCats=0; long tJeiRecipes=0;
			for (gregapi.recipes.Recipe.RecipeMap rm : gregapi.recipes.Recipe.RecipeMap.RECIPE_MAP_LIST) { if (!rm.mNEIAllowed) continue; try { java.util.List<gregapi.recipes.Recipe> l = rm.getNEIAllRecipes(); if (l != null && !l.isEmpty()) { tJeiCats++; tJeiRecipes += l.size(); } } catch (Throwable e) {} }
			o.println("[GT6-ENGINE] 4b. JEI (ожидаемо от плагина): категорий=" + tJeiCats + " рецептов-в-них=" + tJeiRecipes);
		} catch (Throwable e) { o.println("[GT6-ENGINE] 4b. JEI: скан упал=" + e); }
		// 5. ИМЕНА (Ф1.2): getName GT6-предмета — читаемое имя или сырой ключ? (мост ItemBase.getName → LanguageHandler; если не наполнено — вернёт ключ).
		int tNameOk=0, tNameRaw=0, tNameShown=0; StringBuilder tNameSamples = new StringBuilder();
		for (net.minecraft.world.item.Item it : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (!isGregNamespace(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).getNamespace())) continue;
			try {
				String nm = it.getName(new net.minecraft.world.item.ItemStack(it)).getString();
				boolean tRaw = nm == null || nm.isEmpty() || nm.contains("gt.") || nm.startsWith("item.") || nm.startsWith("block.");
				if (tRaw) tNameRaw++; else tNameOk++;
				if (tNameShown < 8) { tNameSamples.append(" [").append(nm).append("]"); tNameShown++; }
			} catch (Throwable e) { tNameRaw++; }
		}
		o.println("[GT6-ENGINE] 5. ИМЕНА (getName): читаемых=" + tNameOk + " сырой-ключ=" + tNameRaw + " примеры:" + tNameSamples);
		// 6. КРАФТ (Ф1.4/Ф1.3): RecipeManager gregtech=1 = диспетчер F11; рецепты в CR.BUFFER. Ф1.3: в JEI-крафт идут Shaped/Shapeless-наследники.
		try { int tCrTotal=gregapi.util.CR.list().size(), tCrJei=0;
			for (gregapi.recipes.ICraftingRecipeGT r : gregapi.util.CR.list()) if (r instanceof gregapi.recipes.ShapedOreRecipe || r instanceof gregapi.recipes.ShapelessOreRecipe) tCrJei++;
			o.println("[GT6-ENGINE] 6. КРАФТ CR.BUFFER (F11-диспетчер): буфер-крафт-рецептов=" + tCrTotal + " из них в JEI-крафт (shaped/shapeless)=" + tCrJei); }
		catch (Throwable e) { o.println("[GT6-ENGINE] 6. КРАФТ CR.BUFFER: скан упал=" + e); }
		o.println("[GT6-ENGINE] ================= КОНЕЦ ЗАМЕРА =================");
	}

	/** Перечислить варианты предмета/блока через его getSubItems/getSubBlocks(Item,CreativeModeTab,List) — та же рефлексия, что CreativeTabsGT. Вернуть кол-во. */
	private static int countSub(Object aTarget, String aMethod, net.minecraft.world.item.Item aItem) {
		if (aTarget == null || aItem == null) return 0;
		try {
			java.util.List<net.minecraft.world.item.ItemStack> tList = new java.util.ArrayList<>();
			java.lang.reflect.Method m = aTarget.getClass().getMethod(aMethod, net.minecraft.world.item.Item.class, net.minecraft.world.item.CreativeModeTab.class, java.util.List.class);
			m.invoke(aTarget, aItem, null, tList);
			return tList.size();
		} catch (Throwable e) { return 0; }
	}

	// F6-worldgen АВТОНОМНЫЙ вход в мир (гейт: файл run/wgautoworld.flag): quickPlay упирается в диалог-подтверждение
	// (некому кликнуть) → до генерации не доходит. Здесь на TitleScreen САМИ создаём свежий CREATIVE-мир через штатный
	// клиентский API createFreshLevel (тот же путь, что кнопка «Создать мир» → «Создать», минует ВСЕ диалоги). Старый
	// тест-мир удаляем сами. Ноль ручных действий. После входа — ore-probe (выше) дампует руды/материал в gregtech.log.
	private boolean mAutoWorldTriggered = false;
	@net.neoforged.bus.api.SubscribeEvent
	public void onAutoWorldCreate(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (mAutoWorldTriggered) return;
		net.minecraft.client.Minecraft tMC = Minecraft.getInstance();
		if (!(tMC.screen instanceof net.minecraft.client.gui.screens.TitleScreen)) return;
		if (!new java.io.File("wgautoworld.flag").exists()) return;
		mAutoWorldTriggered = true;
		try {
			java.io.File tOld = new java.io.File("saves/GT6WGTest");
			if (tOld.exists()) deleteRecursive(tOld);
			gregapi.data.CS.OUT.println("[GT6-AUTOWORLD] создаю свежий CREATIVE-мир GT6WGTest (программно, минуя диалоги)...");
			net.minecraft.world.level.LevelSettings tSettings = new net.minecraft.world.level.LevelSettings(
				"GT6 WG Test", net.minecraft.world.level.GameType.CREATIVE,
				net.minecraft.world.level.LevelSettings.DifficultySettings.DEFAULT, true,
				net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
			tMC.createWorldOpenFlows().createFreshLevel("GT6WGTest", tSettings,
				new net.minecraft.world.level.levelgen.WorldOptions(4242L, true, false), // ФИКС-сид: детерминированная генерация для чистого измерения reattach (было defaultWithRandomSeed)
				net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions,
				tMC.screen);
		} catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-AUTOWORLD] упал: " + e); e.printStackTrace(gregapi.data.CS.ERR); }
	}
	private static void deleteRecursive(java.io.File aFile) {
		java.io.File[] tKids = aFile.listFiles();
		if (tKids != null) for (java.io.File tK : tKids) deleteRecursive(tK);
		aFile.delete();
	}

	// F5/F3-render (client): единый динамический FluidModel ВСЕМ GT6-жидкостям (замена «Missing FluidModel» на реальный
	// рендер). GT6-жидкость = still/flow-текстура (mTexture, IIconContainer) + цвет (mRGBa, тинтит серый молтен). neo 26
	// рендерит жидкости через FluidModel.Unbaked(still, flow, overlay, tintSource) на RegisterFluidModelsEvent (mod-bus).
	// Централизация 1:1 (одна модель-фабрика на весь мод, как GT6BlockModel/GT6ItemModel). Fallback на воду при null-иконе.
	private void onRegisterFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent aEvent) {
		net.minecraft.client.resources.model.sprite.Material tWaterStill = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still"));
		net.minecraft.client.resources.model.sprite.Material tWaterFlow  = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_flow"));
		int tCount = 0;
		for (gregapi.fluid.FluidGT tF : gregapi.fluid.FluidGT.BY_NAME.values()) {
			try {
				net.minecraft.resources.Identifier tTex = null;
				try { if (tF.mTexture != null) tTex = tF.mTexture.getIcon(0); } catch (Throwable e) {/* невалидная икона → fallback вода */}
				net.minecraft.client.resources.model.sprite.Material tStill = tTex != null ? new net.minecraft.client.resources.model.sprite.Material(tTex) : tWaterStill;
				net.minecraft.client.resources.model.sprite.Material tFlow  = tTex != null ? tStill : tWaterFlow;
				short[] tRGBa = tF.getRGBa();
				int tTint = (tRGBa != null && tRGBa.length >= 3) ? (0xFF000000 | ((tRGBa[0]&0xFF)<<16) | ((tRGBa[1]&0xFF)<<8) | (tRGBa[2]&0xFF)) : 0xFFFFFFFF;
				net.minecraft.client.renderer.block.FluidModel.Unbaked tModel = new net.minecraft.client.renderer.block.FluidModel.Unbaked(tStill, tFlow, null, net.neoforged.neoforge.client.fluid.FluidTintSources.constant(tTint));
				net.minecraft.world.level.material.Fluid tSource  = tF.mSourceHolder.value();
				net.minecraft.world.level.material.Fluid tFlowing = tF.mFlowingHolder.isBound() ? tF.mFlowingHolder.value() : tSource;
				aEvent.register(tModel, tSource, tFlowing);
				tCount++;
			} catch (Throwable e) {/* сбой одной жидкости не рушит остальные */}
		}
		gregapi.data.CS.OUT.println("[GT6] F3-render: FluidModel зарегистрированы для " + tCount + " GT6-жидкостей.");
	}

	private void onRegisterBlockStateModels(net.neoforged.neoforge.client.event.RegisterBlockStateModels aEvent) {
		aEvent.registerModel(gregapi.render.GT6BlockModel.Unbaked.ID, gregapi.render.GT6BlockModel.Unbaked.MAP_CODEC);
	}

	// Рантайм-инъекция: каждому BlockState каждого GT6-блока-рендера назначаем единственный GT6BlockModel
	// (модель динамическая — читает блок/позицию/состояние в collectParts, один инстанс на весь мод).
	private void onModifyBakingResult(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult aEvent) {
		net.minecraft.client.resources.model.sprite.Material.Baked tParticle = new net.minecraft.client.resources.model.sprite.Material.Baked(
			// sprite-id БЕЗ "blocks/" префикса: atlas-source (assets/minecraft/atlases/blocks.json) кладёт textures/blocks/** с prefix:"" → gregtech:system/error (как GT6BlockModel:56). Прежний "blocks/system/error" не находился → "Failed to retrieve texture".
			aEvent.getTextureGetter().apply(net.minecraft.resources.Identifier.fromNamespaceAndPath("gregtech", "system/error")), false);
		gregapi.render.GT6BlockModel tModel = new gregapi.render.GT6BlockModel(tParticle);
		java.util.Map<net.minecraft.world.level.block.state.BlockState, net.minecraft.client.renderer.block.dispatch.BlockStateModel> tMap = aEvent.getBakingResult().blockStateModels();
		int tCount = 0;
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (!(tBlock instanceof gregapi.render.IRenderedBlock)) continue;
			for (net.minecraft.world.level.block.state.BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
				tMap.put(tState, tModel); tCount++;
			}
		}
		// F3-render: ЕДИНАЯ item-модель ВСЕМ GT6-предметам (включая block-предметы: их item-форму рисует GT6ItemModel через
		// buildInventoryQuads = renderInventoryBlock). Прежде block-предметы пропускались → у них не было item-модели → пурпур.
		gregapi.render.GT6ItemModel tItemModel = new gregapi.render.GT6ItemModel();
		java.util.Map<net.minecraft.resources.Identifier, net.minecraft.client.renderer.item.ItemModel> tItemMap = aEvent.getBakingResult().itemStackModels();
		int tItemCount = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			net.minecraft.resources.Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null || !isGregNamespace(tKey.getNamespace())) continue;
			// block-предмет инжектим только если его блок — IRenderedBlock (иначе оставляем дефолтную модель блока).
			if (tItem instanceof net.minecraft.world.item.BlockItem tBI && !(tBI.getBlock() instanceof gregapi.render.IRenderedBlock)) continue;
			tItemMap.put(tKey, tItemModel); tItemCount++;
		}
		gregapi.data.CS.OUT.println("[GT6] F3-render: GT6BlockModel injected into " + tCount + " block-states, GT6ItemModel into " + tItemCount + " items.");
	}

	private static boolean isGregNamespace(String aNs) {
		return aNs.equals(gregapi.data.CS.ModIDs.GT) || aNs.equals("gregtech") || aNs.equals("gregapi");
	}
	
	@Override
	public Player getThePlayer() {
		return Minecraft.getInstance().player;
	}

	@Override
	public boolean isSingleplayer() {
		return Minecraft.getInstance().isSingleplayer();
	}

	@Override
	public java.io.InputStream getResourceStream(net.minecraft.resources.Identifier aRL) {
		try {
			java.util.Optional<net.minecraft.server.packs.resources.Resource> tRes = Minecraft.getInstance().getResourceManager().getResource(aRL);
			if (tRes.isPresent()) return tRes.get().open();
		} catch (java.io.IOException e) {/**/}
		return null;
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code PlayerControllerMP.sendUseItem(player,world,stack)}
	 *  с явным {@code ItemStack} (тип метода удалён). Neo {@code MultiPlayerGameMode.useItem(Player,InteractionHand)}
	 *  берёт предмет из руки игрока, а не явный {@code aStack} — семантика "использовать ИМЕННО этот стек"
	 *  недостижима без него (движко-шов), поэтому используется основная рука как ближайший эквивалент. */
	@Override
	public boolean sendUseItemPacket(Player aPlayer, Level aWorld, ItemStack aStack) {
		Minecraft.getInstance().gameMode.useItem(aPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
		return T;
	}

	// @Override
	@SuppressWarnings("deprecation")
	public void onProxyAfterPreInit(Abstract_Mod aMod, FMLCommonSetupEvent aEvent) {
		/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code RenderingRegistry.registerEntityRenderingHandler}
		 *  (`cpw.mods.fml.client.registry`, F10-зеркало compile-only) с {@code new RenderFallingBlock()} —
		 *  в 26.1.2 {@code FallingBlockRenderer} требует {@code EntityRendererProvider.Context} и
		 *  регистрируется через {@code EntityRenderersEvent.RegisterRenderers} (decisions/F3-render.md §2.5/§6),
		 *  НЕ через этот пре-инит хук FML common setup. Заглушка сохраняет вызов центра (F10-зеркало)
		 *  с нейтральным held-объектом. */
		RenderingRegistry.registerEntityRenderingHandler(PrefixBlockFallingEntity.class, null);
		/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): {@code RenderingRegistry.registerBlockHandler}/{@code getNextAvailableRenderId}
		 *  (F10-зеркало) — старый render-id диспетчер blockstate-рендера удалён целиком (decisions/F3-render.md §1,3);
		 *  замена — {@code DynamicBlockStateModel}/{@code RegisterBlockStateModels} (там же §2.1). {@link RendererBlockFluid}/
		 *  {@link RendererBlockTextured} держат серверную поверхность (см. их class javadoc) — id тут заведомо no-op (0). */
		RenderingRegistry.registerBlockHandler(new RendererBlockFluid(0));
		RenderingRegistry.registerBlockHandler(new RendererBlockTextured(0));
		/** PORT-TODO(F3/F5 граница, baked-рендер клиента): {@code net.minecraftforge.fluids.FluidRegistry}
		 *  (старый Forge-кастом-жидкостный API) удалён целиком — F5 ({@code gregapi.fluid}/{@code FL}) уже
		 *  закрыт другим заходом и не использует эту точку (см. {@link RendererBlockFluid} class javadoc,
		 *  "F5 закрыт, сюда не лезем"); эта строка — осиротевший межшовный мост, не переносится. */
		// Check if OptiFine is loaded in order to disable some GT Render Hooks to fix Glitches.
		ITexture.Util.OPTIFINE_LOADED = FMLClientHandler.instance().hasOptifine();
		
		if (XMAS_IN_JULY) {
			// Christmas in July! Go look it up, it is an actual thing!
			Textures.BlockIcons.LEAVES_CD[0] = Textures.BlockIcons.LEAVES_BLUESPRUCE_XMAS;
			Textures.BlockIcons.LEAVES_CD[8] = Textures.BlockIcons.LEAVES_OPAQUE_BLUESPRUCE_XMAS;
		}
		if (XMAS_IN_DECEMBER) {
			// Normal Holiday Season!
			Textures.BlockIcons.LEAVES_CD[0] = Textures.BlockIcons.LEAVES_BLUESPRUCE_XMAS;
			Textures.BlockIcons.LEAVES_CD[8] = Textures.BlockIcons.LEAVES_OPAQUE_BLUESPRUCE_XMAS;
		}
		
		Date tDate = new Date();
		
		switch (tDate.getMonth()+1) {// Not going to use Calendar, because it fucking crashes with Missing Resource Exception...
		case  1:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_BROWN;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_BROWN;
			break;
		case  9:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_YELLOW;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_YELLOW;
			break;
		case 10:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_ORANGE;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_ORANGE;
			break;
		case 11:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_RED;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_RED;
			break;
		case 12:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_BROWN;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_BROWN;
			break;
		}
	}
	
	// @Override
	public void onProxyBeforeInit(Abstract_Mod aMod, FMLCommonSetupEvent aEvent) {
		for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_MAP.values()) LH.add("gt.material." + tMaterial.mNameInternal, tMaterial.mNameLocal);
	}
	
	// @Override
	public void onProxyAfterInit(Abstract_Mod aMod, FMLCommonSetupEvent aEvent) {
		for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
			LH.add("oredict.prefix." + tPrefix.mNameInternal, tPrefix.mNameLocal);
			tPrefix.mNameLocal = LH.get("oredict.prefix." + tPrefix.mNameInternal, tPrefix.mNameLocal);
		}
	}
	
	// @Override
	public void onProxyAfterPostInit(Abstract_Mod aMod, FMLLoadCompleteEvent aEvent) {
		// Initialising the List of Decorative Plank Icons
		for (int i = 0; i < PlankData.PLANKS.length; i++) {
			Block tBlock = ST.block(PlankData.PLANKS[i]);
			if (tBlock != null && tBlock != NB) PlankData.PLANK_ICONS[i] = new IconContainerCopied(tBlock, ST.meta_(PlankData.PLANKS[i]), SIDE_ANY);
		}
	}
	
	public static final List<short[]> sRainbow = new ArrayListNoNulls<>(), sRainbowFast = new ArrayListNoNulls<>(), sPosR = new ArrayListNoNulls<>(), sPosG = new ArrayListNoNulls<>(), sPosB = new ArrayListNoNulls<>(), sPosA = new ArrayListNoNulls<>(), sNegR = new ArrayListNoNulls<>(), sNegG = new ArrayListNoNulls<>(), sNegB = new ArrayListNoNulls<>(), sNegA = new ArrayListNoNulls<>();
	
	/**
	 * PORT-TODO(F3/F5 граница, baked-рендер клиента): 1.7.10 {@code TextureStitchEvent.Pre} (до стежки,
	 * позволял чинить иконки жидкостей ДО постройки атласа) заменён на {@code TextureAtlasStitchedEvent}
	 * (только ПОСЛЕ стежки, `neoforge-decompiled/.../TextureAtlasStitchedEvent.java:24-38`, нет Pre-варианта)
	 * — сама точка вмешательства форсированно иная (движко-шов). Тело зовёт удалённый Forge-кастом-жидкостный
	 * {@code net.minecraftforge.fluids.FluidRegistry}/{@code IIcon Fluid.getIcon()} — F5 ({@code gregapi.fluid}/
	 * {@code FL}) уже закрыт другим заходом и эту точку не использует (см. {@link RendererBlockFluid} class
	 * javadoc, "F5 закрыт, сюда не лезем") — фикс "жидкость без иконки" переносится в baked-фазу F3 (материал
	 * атласа резолвится через {@code ModelBaker.materials()}, decisions/F3-render.md §2.3), не сюда.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTextureStitchedPre(TextureAtlasStitchedEvent aEvent) {
		//
	}
	
	/**
	 * PORT-TODO(F3, baked-рендер клиента, частично): 1.7.10 {@code net.minecraftforge.event.entity.player.ItemTooltipEvent}
	 * держал tooltip как {@code List<String>} напрямую в поле {@code toolTip}; neo-эквивалент
	 * {@code net.neoforged.neoforge.event.entity.player.ItemTooltipEvent} (`neoforge-decompiled/.../ItemTooltipEvent.java:16-70`)
	 * — геттеры, а список типизирован {@code List<Component>} (движко-шов, т.к. рендер текста теперь
	 * дерево {@code Component}, не сырая строка). Вся GT6-логика ниже (300+ строк) оперирует СТРОКАМИ
	 * (конкатенация {@code LH.Chat.*} §-кодов, {@code replaceAll}, и т.д.) и передаётся в
	 * {@code ICover.addToolTips(List<String>,...)} (сотни реализаций по всему моду, {@code List<String>}
	 * НЕ тронут — вне зоны этого захода) — чтобы не терять ни строки бизнес-логики (R8), тело работает
	 * на ЛОКАЛЬНОЙ {@code List<String>}-копии (снятой из {@code Component.getString()} до, собранной
	 * обратно через {@code Component.literal(...)} после — §-коды внутри literal-строки по-прежнему
	 * рендерятся движком, см. {@code FormattedCharSequence}); синхронизация — в {@code finally}, чтобы
	 * сработать на ЛЮБОМ выходе (return/exception), как оригинал мутировал список напрямую. Единственная
	 * генуинно потерянная строка — harvest-tooltip ({@code Block.getHarvestTool/getHarvestLevel}, API
	 * удалено целиком, замены нет, помечено ниже отдельно).
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemTooltip(ItemTooltipEvent aEvent) {
		if (Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI || ST.invalid(aEvent.getItemStack())) return;
		if (!DISPLAY_TEMP_TOOLTIP) {DISPLAY_TEMP_TOOLTIP = T; return;}

		List<Component> tTT = aEvent.getToolTip();
		List<String> aToolTip = new ArrayList<>(tTT.size());
		for (Component tC : tTT) aToolTip.add(tC == null ? null : tC.getString());
		try {
			if (UT.NBT.getNBT(aEvent.getItemStack()).getBooleanOr("gt.err.oredict.output", F)) {
				aToolTip.clear();
				aToolTip.add(0, LH.Chat.BLINKING_RED+"A Recipe used an OreDict Item as Output directly, without copying it before!");
				aToolTip.add(1, LH.Chat.BLINKING_RED+"This is a typical CallByReference/CallByValue Error of the Modder doing it.");
				aToolTip.add(2, LH.Chat.BLINKING_RED+"Please check all Recipes outputting this Item, and report the Recipes to their Owner.");
				aToolTip.add(3, LH.Chat.BLINKING_RED+"The Owner of the RECIPE, NOT the Owner of the Item!");
				return;
			}

			String aRegName = ST.regName(aEvent.getItemStack());
			if (aRegName == null) {
				aToolTip.set(0, LH.Chat.BLINKING_RED+"ERROR: THIS ITEM HAS NOT BEEN REGISTERED!!!");
				aRegName = "ERROR: THIS ITEM HAS NOT BEEN REGISTERED!!!";
			}
			short aMeta = ST.meta_(aEvent.getItemStack());
			byte aBlockMeta = UT.Code.bind4(aMeta);
			Block aBlock = ST.block(aEvent.getItemStack());
			Item aItem = ST.item(aEvent.getItemStack());
			OreDictItemData tData = OM.anydata_(aEvent.getItemStack());

			if (ItemNBT.get(aEvent.getItemStack()) == null) {
				if (aBlock == Blocks.DIRT && aBlockMeta == 1) {
					aToolTip.set(0, aToolTip.get(0).replaceAll("Dirt", "Coarse Dirt"));
				}
				if (MD.RC.mLoaded && "Railcraft:part.plate".equalsIgnoreCase(aRegName)) {
					switch(aMeta) {
					case 0: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateIron")); break;
					case 1: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateSteel")); break;
					case 2: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateTinAlloy")); break;
					case 3: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateCopper")); break;
					case 4: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateLead")); break;
					}
				}
			} else {
				// Anything from TiC with an NBT on it has a potential to Crash if its Tooltip is touched, due to them establishing a frikkin Iterator before sending the Tooltip Event, so lets avoid that...
				if (MD.TiC.owns(aRegName)) return;
			}

			if (MD.Mek.owns(aRegName)) aToolTip.set(0, aToolTip.get(0).replaceAll("Osmium", MT.Ge.mNameLocal));
			if (MD.BP .owns(aRegName)) aToolTip.set(0, aToolTip.get(0).replaceAll("Infused Teslatite", MT.PurpleAlloy.mNameLocal).replaceAll("Teslatite", MT.Nikolite.mNameLocal));
			if (MD.BP.mLoaded) aToolTip.set(0, aToolTip.get(0).replaceAll("Teslatite", MT.Nikolite.mNameLocal));

			if (!(aItem instanceof ItemFluidDisplay) && SHOW_INTERNAL_NAMES) {
				if (tData != null && tData.validData()) {
					if (tData.mBlackListed) {
						if (ST.isGT(aItem))
						aToolTip.add(1, LH.Chat.ORANGE + tData.toString());
						else
						aToolTip.add(1, LH.Chat.DCYAN + aRegName + LH.Chat.WHITE + " - " + LH.Chat.CYAN + aMeta + LH.Chat.WHITE + " - " + LH.Chat.ORANGE + tData.toString());
					} else {
						if (ST.isGT(aItem))
						aToolTip.add(1, LH.Chat.GREEN + tData.toString());
						else
						aToolTip.add(1, LH.Chat.DCYAN + aRegName + LH.Chat.WHITE + " - " + LH.Chat.CYAN + aMeta + LH.Chat.WHITE + " - " + LH.Chat.GREEN + tData.toString());
					}
				} else {
					if (!ST.isGT(aItem))
					aToolTip.add(1, LH.Chat.DCYAN + aRegName + LH.Chat.WHITE + " - " + LH.Chat.CYAN + aMeta);
				}
			}

			if (ItemsGT.RECIPE_REMOVED_USE_TRASH_BIN_INSTEAD.contains(aEvent.getItemStack(), T)) {
				aToolTip.add(LH.Chat.BLINKING_RED + "Recipe has been removed in favour of the GregTech Ender Garbage Bin");
			}

			ICover tCover = CoverRegistry.get(aEvent.getItemStack());
			if (tCover != null) tCover.addToolTips(aToolTip, aEvent.getItemStack(), aEvent.getFlags().isAdvanced());

			if (aBlock != NB) {
				if (IL.TC_Warded_Glass.equal(aEvent.getItemStack(), F, T)) {
					aToolTip.add(LH.getToolTipBlastResistance(aBlock, 999));
				} else if (ItemsGT.SHOW_RESISTANCE.contains(aEvent.getItemStack(), T)) {
					if (IL.ICBM_Concrete.block() == aBlock) {
						switch(aMeta) {
						default: aToolTip.add(LH.getToolTipBlastResistance(aBlock, 30)); break;
						case  1: aToolTip.add(LH.getToolTipBlastResistance(aBlock, 38)); break;
						case  2: aToolTip.add(LH.getToolTipBlastResistance(aBlock, 48)); break;
						}
					} else {
						aToolTip.add(LH.getToolTipBlastResistance(aBlock, aBlock.getExplosionResistance()));
					}
					// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code Block.getHarvestTool(meta)/getHarvestLevel(meta)}
					// (API удалено целиком в 26.1.2 — заменено тег-системой {@code BlockTags.MINEABLE_WITH_*} без
					// прямого "имя инструмента + уровень" аксессора; равноценной замены нет) — строка не добавляется.
				}
				if (BlocksGT.openableCrowbar.contains(aBlock)) {
					aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOL_TO_OPEN_CROWBAR));
				}
			}

			if (BooksGT.BOOK_REGISTER.containsKey(aEvent.getItemStack(), T)) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_SHELFABLE));
			}

			if (Sandwiches.INGREDIENTS.containsKey(aEvent.getItemStack(), T)) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_SANDWICHABLE));
			}

			/* F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code Item.isBeaconPayment(ItemStack)} (Forge 1.7.10,
			 * метод удалён) — neo эквивалент тег {@code ItemTags.BEACON_PAYMENT_ITEMS}. */
			if (aEvent.getItemStack().is(net.minecraft.tags.ItemTags.BEACON_PAYMENT_ITEMS)) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_BEACON_PAYMENT));
			}

			/* F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code cpw.mods.fml.common.registry.GameRegistry.getFuelValue(ItemStack)}
			 * (Forge 1.7.10 static API, удалён) — neo {@code Level.fuelValues().burnDuration(ItemStack)}
			 * (`neo-decompiled/net/minecraft/world/level/block/entity/FuelValues.java:38`), инстанс с клиентского
			 * {@code Minecraft.getInstance().level} (ближайший клиентский эквивалент world-контекста). */
			Level tClientLevel = Minecraft.getInstance().level;
			long tBurnValue = tClientLevel == null ? 0 : tClientLevel.fuelValues().burnDuration(ST.amount(1, aEvent.getItemStack()));
			if (tBurnValue > 0) aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FURNACE_FUEL) + LH.Chat.WHITE + tBurnValue + " ("+(tBurnValue*EU_PER_FURNACE_TICK)+LH.Chat._RED+"HU"+LH.Chat.WHITE+")");

			if (tData != null) {
				if (tData.validPrefix()) {
					for (IOreDictListenerItem tListener : tData.mPrefix.mListenersItem) {
						String tToolTip = tListener.getListenerToolTip(tData.mPrefix, tData.mMaterial.mMaterial, aEvent.getItemStack());
						if (tToolTip != null) aToolTip.add(tToolTip);
					}
				} else {
					if (IL.RC_Firestone_Refined.equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "GT6 Burning Boxes: "+LH.Chat.WHITE+(800*EU_PER_LAVA)+LH.Chat._RED+"HU"+LH.Chat._CYAN+"per Lava Block"); else
					if (IL.RC_Firestone_Cracked.equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "GT6 Burning Boxes: "+LH.Chat.WHITE+(600*EU_PER_LAVA)+LH.Chat._RED+"HU"+LH.Chat._CYAN+"per Lava Block"); else
					if (IL.TF_Pick_Giant       .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "Repairable with Knightmetal Ingots on the Vanilla Anvil"); else
					if (IL.TF_Sword_Giant      .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "Repairable with Ironwood Ingots on the Vanilla Anvil"); else
					if (IL.TF_Lamp_of_Cinders  .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "Can be used as a Lighter for GT6 things and TNT");
				}
				if (tData.validMaterial()) {
					boolean tUnburnable = F;
					for (OreDictMaterialStack tMaterial : tData.getAllMaterialWeights()) {
						if (tMaterial.mMaterial.contains(TD.Properties.UNBURNABLE)) tUnburnable = T;
						for (IOreDictListenerItem tListener : tMaterial.mMaterial.mListenersItem) {
							String tToolTip = tListener.getListenerToolTip(tData.mPrefix, tData.mMaterial.mMaterial, aEvent.getItemStack());
							if (tToolTip != null) aToolTip.add(tToolTip);
						}
					}
					if (tData.mMaterial.mMaterial.mToolTypes > 0 && (tData.mPrefix != null || (aEvent.getItemStack().getMaxStackSize() > 1 && tData.mByProducts.length == 0 && tData.mMaterial.mAmount <= U))) {
						aToolTip.add(LH.Chat.BLUE + "Q: " + tData.mMaterial.mMaterial.mToolQuality + " - S: " + tData.mMaterial.mMaterial.mToolSpeed + " - D: " + tData.mMaterial.mMaterial.mToolDurability);
					}
					if (SHOW_CHEM_FORMULAS && UT.Code.stringValid(tData.mMaterial.mMaterial.mTooltipChemical) && (tData.mPrefix == null ? tData.mByProducts.length == 0 : tData.mPrefix.contains(TD.Prefix.TOOLTIP_MATERIAL))) {
						aToolTip.add(LH.Chat.YELLOW + tData.mMaterial.mMaterial.mTooltipChemical);
					}
					if (tData.mMaterial.mMaterial == MT.Nikolite) {
						aToolTip.set(0, aToolTip.get(0).replaceAll("(Teslatite|Electrotine)", MT.Nikolite.mNameLocal));
					}
					if (tData.mMaterial.mMaterial == MT.Ge) {
						aToolTip.set(0, aToolTip.get(0).replaceAll("Osmium", MT.Ge.mNameLocal));
					}
					if (tData.validPrefix()) {
						if (!ST.isGT(aItem) && tData.mPrefix == OP.dustTiny && ANY.Blaze.mToThis.contains(tData.mMaterial.mMaterial)) {
							aToolTip.set(0, aToolTip.get(0).replaceAll(tData.mMaterial.mMaterial.mNameLocal, OP.dustTiny.mMaterialPre + tData.mMaterial.mMaterial.mNameLocal));
						}
						if (tData.mPrefix.contains(TD.Prefix.NEEDS_SHARPENING)) aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_NEEDS_SHARPENING));
						if (tData.mPrefix.contains(TD.Prefix.NEEDS_HANDLE    )) aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_NEEDS_HANDLE) + LH.Chat.WHITE + tData.mMaterial.mMaterial.mHandleMaterial.getLocal());

						if (!tData.mMaterial.mMaterial.mSourceOf.isEmpty() && tData.mPrefix.containsAny(TD.Prefix.ORE,TD.Prefix.ORE_PROCESSING_DIRTY)) {
							StringBuilder
							tToolTip = null;
							for (OreDictMaterial tMaterial : tData.mMaterial.mMaterial.mSourceOf) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.CYAN).append("Source of: ").append(LH.Chat.WHITE); else tToolTip.append(", ");
								tToolTip.append(tMaterial.getLocal());
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
						}


						ArrayListNoNulls<Integer> tShapelessAmounts = new ArrayListNoNulls<>();
						for (AdvancedCrafting1ToY tHandler : tData.mPrefix.mShapelessManagersSingle) if (tHandler.hasOutputFor(tData.mMaterial.mMaterial)) tShapelessAmounts.add(1);
						for (AdvancedCraftingXToY tHandler : tData.mPrefix.mShapelessManagers      ) if (tHandler.hasOutputFor(tData.mMaterial.mMaterial)) tShapelessAmounts.add(tHandler.mInputCount);
						if (!tShapelessAmounts.isEmpty()) {
							Collections.sort(tShapelessAmounts);
							aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_SHAPELESS_CRAFT) + LH.Chat.WHITE + tShapelessAmounts);
						}
						if (tData.mPrefix.contains(TD.Prefix.TOOLTIP_ENCHANTS)) {
							StringBuilder
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentTools) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_TOOL_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
								if (tEnchantment.mObject == Enchantments.FIRE_ASPECT && tEnchantment.mAmount >= 3) tToolTip.append(" (Autosmelt)");
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentWeapons) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_WEAPON_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentAmmo) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_AMMO_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentFishing) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_FISHING_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());

							if (!tData.mPrefix.containsAny(TD.Prefix.TOOL_HEAD, TD.Prefix.WEAPON_ALIKE, TD.Prefix.AMMO_ALIKE, TD.Prefix.TOOL_ALIKE)) {
								tToolTip = null;
								for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentArmors) {
									if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_ARMOR_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
									tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
								}
								if (tToolTip != null) aToolTip.add(tToolTip.toString());

								if (MD.TF.mLoaded && tData.mMaterial.mMaterial.contains(TD.Properties.MAZEBREAKER)) {
									aToolTip.add(LH.Chat.PINK + LH.get(LH.TOOLTIP_TWILIGHT_MAZE_BREAKING));
								}
							}

							if (MD.BTL.mLoaded && tData.mMaterial.mMaterial.contains(TD.Properties.BETWEENLANDS)) {
								aToolTip.add(LH.Chat.GREEN + LH.get(LH.TOOLTIP_BETWEENLANDS_RESISTANCE));
							}

							if (MD.TC.mLoaded && tData.mMaterial.mMaterial.contains(TD.Properties.WARPING)) {
								aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_THAUMCRAFT_WARP));
							}
						}
						if (aBlock == NB || !(aBlock instanceof MultiTileEntityBlockInternal || aBlock instanceof IBlockBase)) {
							if (tData.mMaterial.mMaterial.contains(TD.Properties.FLAMMABLE)) {
								if (tData.mMaterial.mMaterial.contains(TD.Properties.EXPLOSIVE)) {
									aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FLAMMABLE_AND_EXPLOSIVE));
								} else {
									aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FLAMMABLE));
								}
							} else if (tData.mMaterial.mMaterial.contains(TD.Properties.EXPLOSIVE)) {
								aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_EXPLOSIVE));
							}
						}
					}
					if (tUnburnable && !MD.MC.owns(aRegName)) aToolTip.add(LH.Chat.GREEN + LH.get(LH.TOOLTIP_UNBURNABLE));
				}

				if (aEvent.getFlags().isAdvanced()) {
					boolean temp = T;
					for (OreDictMaterialStack tMaterial : tData.getAllMaterialWeights()) if (tMaterial.mAmount != 0 && !tMaterial.mMaterial.contains(TD.Properties.DONT_SHOW_THIS_COMPONENT)) {
						if (temp) {
							aToolTip.add(LH.Chat.DCYAN + LH.get(LH.TOOLTIP_CONTAINED_MATERIALS));
							temp = F;
						}
						StringBuilder tString = new StringBuilder(128);
						double aWeight = tMaterial.weight();
						long tWeight = ((long)(aWeight*1000))%1000;
						tString.append(LH.Chat.WHITE ).append(UT.Code.displayUnits(tMaterial.mAmount)).append(" ");
						tString.append(LH.Chat.YELLOW).append(tMaterial.mMaterial.getLocal());
						tString.append(LH.Chat.WHITE ).append(" (");
						tString.append(LH.Chat.CYAN  ).append("M: ");
						tString.append(LH.Chat.WHITE ).append(tMaterial.mMaterial.mMeltingPoint);
						tString.append(LH.Chat.RED   ).append("K ");
						tString.append(LH.Chat.CYAN  ).append(" B: ");
						tString.append(LH.Chat.WHITE ).append(tMaterial.mMaterial.mBoilingPoint);
						tString.append(LH.Chat.RED   ).append("K ");
						tString.append(LH.Chat.CYAN  ).append(" W: ");
						tString.append(LH.Chat.WHITE ).append((long)aWeight).append(".").append(tWeight<1?"000":tWeight<10?"00"+tWeight:tWeight<100?"0"+tWeight:tWeight);
						tString.append(LH.Chat.YELLOW).append("kg");
						tString.append(LH.Chat.WHITE ).append(")");
						aToolTip.add(tString.toString());
					}
				} else {
					aToolTip.add(LH.Chat.DGRAY + "Enable F3+H Mode for Info about contained Materials.");
				}

				if (tData.validData()) {
					if (ST.isGT(aItem)) {
						if (tData.mMaterial.mMaterial.mOriginalMod == null) {
							aToolTip.add(LH.Chat.BLUE + "Material from an Unknown Mod");
						} else if (tData.mMaterial.mMaterial.mOriginalMod == MD.MC) {
							aToolTip.add(LH.Chat.BLUE + "Vanilla Material");
						} else if (tData.mMaterial.mMaterial.mOriginalMod == MD.GAPI) {
							if (tData.mMaterial.mMaterial.mID > 0 && tData.mMaterial.mMaterial.mID < 8000) {
								aToolTip.add(LH.Chat.BLUE + "Material from the Periodic Table of Elements");
							} else {
								aToolTip.add(LH.Chat.BLUE + "Random Material handled by Greg API");
							}
						} else {
							aToolTip.add(LH.Chat.BLUE + "Material from " + tData.mMaterial.mMaterial.mOriginalMod.mName);
						}
					} else {
						if ((tData.mMaterial.mMaterial == MT.Fe || tData.mMaterial.mMaterial == MT.Fe2O3) && tData.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED) && !aToolTip.get(0).contains("Native")) {
							aToolTip.set(0, aToolTip.get(0).replaceAll("Banded Iron", MT.Fe2O3.mNameLocal).replaceAll("Iron", MT.Fe2O3.mNameLocal));
						}
						if (tData.mMaterial.mMaterial == MT.Au && tData.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED) && !aToolTip.get(0).contains("Native")) {
							aToolTip.set(0, aToolTip.get(0).replaceAll("Gold", "Native Gold"));
						}
						if (tData.mMaterial.mMaterial == MT.Cu && tData.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED) && !aToolTip.get(0).contains("Native")) {
							aToolTip.set(0, aToolTip.get(0).replaceAll("Copper", "Native Copper"));
						}
					}
				}
			}

			// Remove all Nulls and fix eventual Formatting mistakes.
			for (int i = 1, j = aToolTip.size(); i < j; i++) {
				String tTooltip = aToolTip.get(i);
				if (tTooltip == null || LH.Chat.BASICALLY_EMPTY_STRINGS.contains(tTooltip)) {aToolTip.remove(i--); j--;} else aToolTip.set(i, tTooltip + LH.Chat.RESET_TOOLTIP);
			}
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		} finally {
			// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): синхронизация локальной List<String> обратно в
			// List<Component> события (см. class javadoc метода) — движко-форсированный шов.
			tTT.clear();
			for (String s : aToolTip) tTT.add(s == null ? null : Component.literal(s));
		}
	}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent}
	 *  с полем {@code phase}/сравнением {@code == ServerTickEvent.END} (тип+поле удалены, F10-зеркало
	 *  `compat-mirror/java/cpw/mods/fml/common/gameevent/TickEvent.java` явным PORT-TODO уступает это
	 *  движко-шов сюда) — neo раздельно шлёт {@code ClientTickEvent.Pre}/{@code .Post}
	 *  (`neoforge-decompiled/net/neoforged/neoforge/client/event/ClientTickEvent.java:24-38`);
	 *  {@code .Post} = "после тика" 1:1 равно старому {@code END}-фазе — сигнатура ретипирована,
	 *  условие-обёртка снята (уже подразумевается типом события), тело БЕЗ ИЗМЕНЕНИЙ. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onClientTickEvent(ClientTickEvent.Post aEvent) {
		{
			if (CLIENT_TIME == 10) {
				// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было "Initializing the Fake Furnace Recipe Map" через
				// {@code RecipeManager.smelting().getSmeltingList()} — 1.7.10-статика {@code RecipeManager.smelting()}
				// удалена целиком (neo {@code RecipeManager} инстанс-ориентирован, читается из {@code Level});
				// фейковая furnace-recipe-карта не заполняется до отдельного захода (не render).
				// PORT-TODO(NEI/JEI-интеграция, вне рендер-объёма): было "hiding stuff from NEI" через
				// {@code Item.getSubItems(Item,CreativeModeTab,List)} (метод удалён) + {@code CreativeModeTab.tabAllSearch}
				// (константа удалена, замена {@code CreativeModeTabs.SEARCH} — {@code ResourceKey}, другой контракт) —
				// see память миссии "JEI (аналог NEI) — рано, для приёмки визуала", отдельный заход, не F3.
			}

			// Countdown the Timeout of Sounds that play in rapid succession.
			for (int i = 0; i < UT.Sounds.sPlayedSounds.size(); i++) if (UT.Sounds.sPlayedSounds.get(i).mTimer-- < 0) UT.Sounds.sPlayedSounds.remove(i--);
			// Mute Sounds for the first second so people wont get blasted with nonsense.
			if (CLIENT_TIME > 20) for (UT.Sounds.SoundWithLocation tSound : UT.Sounds.sSoundsToPlay) tSound.play();
			// Regardless of whether all the Sounds actually played, clear the List, we don't want any randomly delayed junk showing up.
			UT.Sounds.sSoundsToPlay.clear();
			
			
			switch((int)(CLIENT_TIME % 10)) {
			case   0: LH.Chat.RAINBOW_FAST = LH.Chat.RED; LH.Chat.BLINKING_CYAN = LH.Chat.CYAN; LH.Chat.BLINKING_RED = LH.Chat.RED; LH.Chat.BLINKING_ORANGE = LH.Chat.ORANGE; break;
			case   1: LH.Chat.RAINBOW_FAST = LH.Chat.ORANGE; break;
			case   2: LH.Chat.RAINBOW_FAST = LH.Chat.YELLOW; break;
			case   3: LH.Chat.RAINBOW_FAST = LH.Chat.GREEN; break;
			case   4: LH.Chat.RAINBOW_FAST = LH.Chat.CYAN; break;
			case   5: LH.Chat.RAINBOW_FAST = LH.Chat.DCYAN; LH.Chat.BLINKING_CYAN = LH.Chat.WHITE; LH.Chat.BLINKING_RED = LH.Chat.WHITE; LH.Chat.BLINKING_ORANGE = LH.Chat.YELLOW; break;
			case   6: LH.Chat.RAINBOW_FAST = LH.Chat.DBLUE; break;
			case   7: LH.Chat.RAINBOW_FAST = LH.Chat.BLUE; break;
			case   8: LH.Chat.RAINBOW_FAST = LH.Chat.PURPLE; break;
			case   9: LH.Chat.RAINBOW_FAST = LH.Chat.PINK; break;
			}
			
			switch((int)(CLIENT_TIME % 50)) {
			case   0: LH.Chat.RAINBOW = LH.Chat.RED; LH.Chat.BLINKING_GRAY = LH.Chat.GRAY; break;
			case   5: LH.Chat.RAINBOW = LH.Chat.ORANGE; break;
			case  10: LH.Chat.RAINBOW = LH.Chat.YELLOW; break;
			case  15: LH.Chat.RAINBOW = LH.Chat.GREEN; break;
			case  20: LH.Chat.RAINBOW = LH.Chat.CYAN; break;
			case  25: LH.Chat.RAINBOW = LH.Chat.DCYAN; LH.Chat.BLINKING_GRAY = LH.Chat.DGRAY; break;
			case  30: LH.Chat.RAINBOW = LH.Chat.DBLUE; break;
			case  35: LH.Chat.RAINBOW = LH.Chat.BLUE; break;
			case  40: LH.Chat.RAINBOW = LH.Chat.PURPLE; break;
			case  45: LH.Chat.RAINBOW = LH.Chat.PINK; break;
			}
			
			switch((int)(CLIENT_TIME % 250)) {
			case   0: LH.Chat.RAINBOW_SLOW = LH.Chat.RED; break;
			case  25: LH.Chat.RAINBOW_SLOW = LH.Chat.ORANGE; break;
			case  50: LH.Chat.RAINBOW_SLOW = LH.Chat.YELLOW; break;
			case  75: LH.Chat.RAINBOW_SLOW = LH.Chat.GREEN; break;
			case 100: LH.Chat.RAINBOW_SLOW = LH.Chat.CYAN; break;
			case 125: LH.Chat.RAINBOW_SLOW = LH.Chat.DCYAN; break;
			case 150: LH.Chat.RAINBOW_SLOW = LH.Chat.DBLUE; break;
			case 175: LH.Chat.RAINBOW_SLOW = LH.Chat.BLUE; break;
			case 200: LH.Chat.RAINBOW_SLOW = LH.Chat.PURPLE; break;
			case 225: LH.Chat.RAINBOW_SLOW = LH.Chat.PINK; break;
			}
			
			int tDirection = (CLIENT_TIME % 100 < 50 ? +1 : -1);
			for (short[] tArray : sPosR) tArray[0] = UT.Code.bind8(tArray[0]+tDirection);
			for (short[] tArray : sPosG) tArray[1] = UT.Code.bind8(tArray[1]+tDirection);
			for (short[] tArray : sPosB) tArray[2] = UT.Code.bind8(tArray[2]+tDirection);
			for (short[] tArray : sPosA) tArray[3] = UT.Code.bind8(tArray[3]+tDirection);
			for (short[] tArray : sNegR) tArray[0] = UT.Code.bind8(tArray[0]-tDirection);
			for (short[] tArray : sNegG) tArray[1] = UT.Code.bind8(tArray[1]-tDirection);
			for (short[] tArray : sNegB) tArray[2] = UT.Code.bind8(tArray[2]-tDirection);
			for (short[] tArray : sNegA) tArray[3] = UT.Code.bind8(tArray[3]-tDirection);
			
			boolean
			tNR = UT.Code.inside(  0,  99, (CLIENT_TIME/2) % 300), tNG = UT.Code.inside( 50, 149, (CLIENT_TIME/2) % 300), tNB = UT.Code.inside(100, 199, (CLIENT_TIME/2) % 300),
			tPR = UT.Code.inside(100, 199, (CLIENT_TIME/2) % 300), tPG = UT.Code.inside(150, 249, (CLIENT_TIME/2) % 300), tPB = UT.Code.inside(200, 299, (CLIENT_TIME/2) % 300);
			
			for (short[] tArray : sRainbow) {
			if (tPR) tArray[0] = UT.Code.bind8(tArray[0] + 1);
			if (tPG) tArray[1] = UT.Code.bind8(tArray[1] + 1);
			if (tPB) tArray[2] = UT.Code.bind8(tArray[2] + 1);
			if (tNR) tArray[0] = UT.Code.bind8(tArray[0] - 1);
			if (tNG) tArray[1] = UT.Code.bind8(tArray[1] - 1);
			if (tNB) tArray[2] = UT.Code.bind8(tArray[2] - 1);
			}
			
			tNR = UT.Code.inside( 0,  9, (CLIENT_TIME/2) % 30); tNG = UT.Code.inside( 5, 14, (CLIENT_TIME/2) % 30); tNB = UT.Code.inside(10, 19, (CLIENT_TIME/2) % 30);
			tPR = UT.Code.inside(10, 19, (CLIENT_TIME/2) % 30); tPG = UT.Code.inside(15, 24, (CLIENT_TIME/2) % 30); tPB = UT.Code.inside(20, 29, (CLIENT_TIME/2) % 30);
			
			for (short[] tArray : sRainbowFast) {
			if (tPR) tArray[0] = UT.Code.bind8(tArray[0] + 10);
			if (tPG) tArray[1] = UT.Code.bind8(tArray[1] + 10);
			if (tPB) tArray[2] = UT.Code.bind8(tArray[2] + 10);
			if (tNR) tArray[0] = UT.Code.bind8(tArray[0] - 10);
			if (tNG) tArray[1] = UT.Code.bind8(tArray[1] - 10);
			if (tNB) tArray[2] = UT.Code.bind8(tArray[2] - 10);
			}
			
			CLIENT_TIME++;
		}
	}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): см. javadoc {@link gregapi.tileentity.render.ITileEntityOnDrawBlockHighlight}
	 *  — {@link ExtractBlockOutlineRenderStateEvent} не несёт {@code player}/{@code currentItem}/{@code partialTicks}
	 *  старого события. Игрок восстановлен через {@code Minecraft.getInstance().player} (тот же центральный
	 *  паттерн, что {@link #getThePlayer()}); {@code sideHit} — из {@code getHitResult().getDirection()};
	 *  {@code partialTicks} недостижим (0 — нейтрально, единственный потребитель {@link RenderHelper#drawWrenchOverlay}
	 *  уже no-op, см. его javadoc). Ветвление/делегирование в {@link ITileEntityOnDrawBlockHighlight} — БЕЗ ИЗМЕНЕНИЙ. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawBlockHighlight(ExtractBlockOutlineRenderStateEvent aEvent) {
		Player tPlayer = Minecraft.getInstance().player;
		if (tPlayer == null) return;
		byte tSide = (byte)aEvent.getHitResult().getDirection().ordinal();
		Block
		aBlock = ST.block(tPlayer.getMainHandItem());
		if (aBlock instanceof BlockMetaType && ((BlockMetaType)aBlock).mIsSlab) {
			RenderHelper.drawWrenchOverlay(aEvent, (byte)0, tSide);
			return;
		}
		aBlock = WD.block(tPlayer.level(), aEvent.getBlockPos().getX(), aEvent.getBlockPos().getY(), aEvent.getBlockPos().getZ());
		BlockEntity aTileEntity = WD.te(tPlayer.level(), aEvent.getBlockPos().getX(), aEvent.getBlockPos().getY(), aEvent.getBlockPos().getZ(), T);
		if (!(aTileEntity instanceof ITileEntityOnDrawBlockHighlight) || !((ITileEntityOnDrawBlockHighlight)aTileEntity).onDrawBlockHighlight(aEvent)) {
			if ((ROTATABLE_VANILLA_BLOCKS.contains(aBlock) || (ToolCompat.IC_WRENCHABLE && aTileEntity instanceof ic2.api.tile.IWrenchable)) && ST.valid(tPlayer.getMainHandItem()) && ToolsGT.contains(TOOL_wrench, tPlayer.getMainHandItem())) {
				RenderHelper.drawWrenchOverlay(aEvent, (byte)0, tSide);
				return;
			}
		}
	}
	
	private static List<Block> ROTATABLE_VANILLA_BLOCKS = Arrays.asList(Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.FURNACE, Blocks.FURNACE, Blocks.DROPPER, Blocks.DISPENSER, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.HOPPER, Blocks.PUMPKIN, Blocks.JACK_O_LANTERN);
}
