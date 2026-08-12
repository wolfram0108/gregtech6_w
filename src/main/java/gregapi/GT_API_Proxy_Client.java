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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;



import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.registries.DeferredRegister;
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
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

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
		// BUG-039 v4 (аудит JPMS-mirror): 1.7.10 RenderingRegistry.addNewArmourRendererPrefix (кастомный слой
		// armor-текстуры) удалён вместе со всей моделью armor-рендера (neo: humanoid-слои через equipment assets,
		// см. ItemArmorBase/F13); mirror-класс cpw.* JPMS-вырезан из рантайма (вызов кидал NoClassDefFoundError в
		// пустой catch). Вызывателей метода 0 (греп) — возвращаемый индекс neo-рендером не потребляется.
		return 0;
	}

	// F3-render (client): регистрация единого динамического типа модели всех GT6-блоков на mod-bus.
	// Замена удалённого `RenderingRegistry.registerBlockHandler`/render-id диспетчера (decisions/F3-render.md §2.1):
	// один `GT6BlockModel` тип. Две точки: (1) RegisterBlockStateModels — тип для blockstate-JSON (fallback);
	// (2) ModifyBakingResult — рантайм-инъекция модели ВСЕМ GT6-блокам (IRenderedBlock) БЕЗ JSON — процедурный
	// мод (сотни блоков динамически) не может держать тысячи статичных JSON; централизация 1:1 (одна модель на весь мод).
	/** BUG-056: клиентская половина «открыть все рецепты машины» — делегирует единственному центру
	 *  JEI-совместимости ({@link gregapi.jei.GT6_JEI_Plugin#showRecipeCategory}), который держит живой
	 *  рантайм и карту «имя категории → тип». Ключ тот же {@code mNameNEI}, которым 1.7.10 звал NEI. */
	@Override
	public boolean openRecipeGui(String aNameNEI) {return gregapi.jei.GT6_JEI_Plugin.showRecipeCategory(aNameNEI);}

	@Override
	public void registerClientModels(net.minecraftforge.eventbus.api.IEventBus aModBus) {
		// Ветка 1.20.1: точки регистрации рендера — те же три события мод-шины, что назвал F3-render.md §4.6
		// (модели, BER, тинты); отдельного «RegisterBlockStateModels» здесь нет — GT6 не держит blockstate-JSON
		// и всегда инжектил модель рантаймом, так что канал у него один — ModifyBakingResult.
		// Модели жидкостей регистрируются не событием, а расширением типа жидкости (см. bindFluidClientExtensions).
		aModBus.addListener(this::onModifyBakingResult);
		aModBus.addListener(this::onRegisterBlockEntityRenderers);
		aModBus.addListener(this::onClientSetup);
		aModBus.addListener(this::onRegisterBlockTints);
		aModBus.addListener(this::registerClientResourceListener);
	}

	// ===== F3 tint ЦЕНТР: цвет блоков GT6 ====================================================================
	// В 1.7.10 движок спрашивал цвет У БЛОКА двумя методами: getRenderColor(meta) — цвет по подтипу, и
	// colorMultiplier(world,x,y,z) — цвет в точке мира (биом-оттенок). Оба метода в порте живы, но вызывателей
	// не имели: neo цвет у блока НЕ спрашивает — он берёт его из РЕЕСТРА источников тинта
	// (BlockColors.register(List<BlockTintSource>, Block...), RegisterColorHandlersEvent.BlockTintSources:68).
	// Поэтому здесь ОДНА регистрация на весь мод, а не мост в каждом блоке: подключать нечего к семи корням —
	// движок вообще ходит другим путём. Источник ниже спрашивает те самые 1.7.10-методы, значения не дублируются.
	// Следствие до этой правки: крашеные блоки (BlockColored — цветное стекло и родня) рисовались без цвета,
	// а биом-оттенок скопированных текстур не работал (метка отложенности в BlockTextureCopied:36 — тот же
	// канал; слово-маркер здесь НЕ пишем дословно, иначе счётчик меток считает упоминание за метку).
	private void onRegisterBlockTints(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block aEvent) {
		java.util.List<net.minecraft.world.level.block.Block> tBlocks = new java.util.ArrayList<>();
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			net.minecraft.resources.ResourceLocation tID = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tID == null || !(tID.getNamespace().equals(gregapi.data.CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			if (tBlock instanceof gregapi.block.IBlock) tBlocks.add(tBlock);
		}
		if (!tBlocks.isEmpty()) aEvent.register(new GT6BlockTint(), tBlocks.toArray(new net.minecraft.world.level.block.Block[0]));
		gregapi.data.CS.OUT.println("[F3-tint] источник цвета зарегистрирован для блоков GT6: " + tBlocks.size());
	}

	/** Переходник «вопрос движка о цвете» → «1.7.10-методы блока GT6». Один на весь мод; величины живут в блоках.
	 *  Ветка 1.20.1: носитель — {@code BlockColor} с ОДНИМ методом {@code getColor(state, level, pos, tintIndex)},
	 *  где отсутствие мира выражено {@code null}-аргументами ({@code BlockColor.java:11}); две ветки 26.x
	 *  ({@code color(state)} / {@code colorInWorld(...)}) сходятся в него по тому же признаку. */
	private static final class GT6BlockTint implements net.minecraft.client.color.block.BlockColor {
		@Override
		public int getColor(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockAndTintGetter aLevel, net.minecraft.core.BlockPos aPos, int aTintIndex) {
			if (!(aState.getBlock() instanceof gregapi.block.IBlock tBlock)) return 16777215;
			// Вопрос с миром — 1.7.10 colorMultiplier(world,x,y,z) (биом-оттенок и прочее позиционное).
			if (aLevel != null && aPos != null) return tBlock.colorMultiplier(aLevel, aPos.getX(), aPos.getY(), aPos.getZ());
			// Вопрос без мира — 1.7.10 getRenderColor(meta). Подтип берём СУЩЕСТВУЮЩИМ центром мета↔BlockState
			// (IBlockExtendedMetaData.getExtendedMetaData), а не своим разбором свойств.
			int tMeta = aState.getBlock() instanceof gregapi.block.IBlockExtendedMetaData tMetaBlock ? tMetaBlock.getExtendedMetaData(aState) : 0;
			return tBlock.getRenderColor(tMeta);
		}
	}

	// F14-gui: КЛИЕНТ-регистрация экрана для ContainerCommon.MENU_TYPE (без неё neo падает при открытии любого GUI мода —
	// «no screen for menu type»). Фабрика маршрутизирует в ЕДИНЫЙ GT6-центр getGUIClient (тот же, что строил экран в
	// 1.7.10 — per-machine ContainerClient-подкласс+текстура); fallback (getGUIClient=null/исключение) — обёртка
	// neo-реконструированного menu базовым ContainerClient (без краша).
	private void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent aEvent) {
		// Ветка 1.20.1: отдельного события регистрации экранов нет — привязка делается статическим
		// MenuScreens.register в client-setup (канон Forge 1.20.1), поэтому кладём её в enqueueWork
		// (MenuScreens не потокобезопасен, а событие приходит на параллельном лоадере).
		aEvent.enqueueWork(this::registerMenuScreens);
	}

	private void registerMenuScreens() {
		if (gregapi.gui.ContainerCommon.MENU_TYPE == null) return;
		net.minecraft.client.gui.screens.MenuScreens.<gregapi.gui.ContainerCommon, gregapi.gui.ContainerClient>register(gregapi.gui.ContainerCommon.MENU_TYPE.get(), (aMenu, aInv, aTitle) -> {
			// исключение отсюда = дисконнект (neoforge ClientPayloadHandler.createMenuScreen catch→disconnect) → тотальный null-гейт
			if (aMenu == null) aMenu = new gregapi.gui.ContainerCommon(0, aInv);
			// containerId-мост (корень «слот есть на сервере, но не отображается»): getGUIClient строит СВЕЖИЙ
			// клиент-контейнер легаси-конструктором (id из sPendingWindowID); вне withWindowID тот равен -1 →
			// id клиента ≠ id сервера → ВСЕ пакеты контента/слотов меню молча дропаются клиентом (проверка id в
			// handleContainerSetSlot/Content). Оборачиваем фабрику мостом с id сетевого меню (= серверный id).
			final gregapi.gui.ContainerCommon fMenu = aMenu;
			try { if (aMenu.mTileEntity instanceof gregapi.tileentity.ITileEntityGUI tGUI) { Object tScreen = gregapi.gui.ContainerCommon.withWindowID(fMenu.containerId, () -> tGUI.getGUIClient(fMenu.mGUIID, aInv.player)); if (tScreen instanceof gregapi.gui.ContainerClient tCC) {
				// счётчик-баланс (сундук-«хор»): сетевой контейнер (createFromNetwork) УЖЕ вызвал openInventoryGUI на клиент-TE,
				// и свежий контейнер из getGUIClient вызвал ЕЩЁ раз; removed() при закрытии декрементит ОДИН раз → клиентский
				// mUsingPlayers залипал >0 навсегда (крышка вечно открыта, звук у всех «открытых» при входе в зону).
				// Компенсация: закрываем счёт сетевого контейнера — экран владеет только своим.
				fMenu.mTileEntity.closeInventoryGUI();
				return tCC;
			} } }
			catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-GUI] getGUIClient упал, fallback-экран: "+e); }
			return new gregapi.gui.ContainerClient(aMenu, gregapi.data.CS.RES_PATH_GUI + "chests/" + (aMenu.mTileEntity == null ? 1 : aMenu.mTileEntity.getSizeInventoryGUI()) + ".png");
		});
		gregapi.data.CS.OUT.println("[GT6-GUI] MenuScreens: экран для ContainerCommon.MENU_TYPE зарегистрирован (F14).");
	}

	// F3-render: MTE-блоки рисует BER (не baked — регион не отдаёт MTE-BE, см. MultiTileEntityBER). Один generic BER на весь
	// MTE_TYPE (централизация 1:1). Руды/стабы отсеиваются внутри BER (гейт MultiTileEntityBlock+IRenderedBlockObject).
	private void onRegisterBlockEntityRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers aEvent) {
		if (gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE != null)
			aEvent.registerBlockEntityRenderer(gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE, gregapi.render.MultiTileEntityBER::new);
		// F12-entity: рендерер падающего мета-блока — 1:1 оригинала (:126 registerEntityRenderingHandler(
		// PrefixBlockFallingEntity.class, new RenderFallingBlock())): тот же ванильный рендерер падающего блока,
		// он рисует то, что отдаёт getBlockState() (у нас — гравий, как и задумал автор, см. PrefixBlockFallingEntity).
		aEvent.registerEntityRenderer(GT_API.METABLOCK_FALLING.get(), net.minecraft.client.renderer.entity.FallingBlockRenderer::new);
	}

	// Приёмочный скан рендера (гейт ②): на первом client-tick, когда атлас стежен И DataComponents ПРИВЯЗАНЫ (на
	// ModelEvent.BakingCompleted они ещё не bound → Item.getDefaultInstance NPE «Components not bound yet»). Проверяем,
	// что item-иконки GT6 резолвятся (не пурпур). Once. Пишет found/missing в gregtech.log (game-bus, авто-регистр).

	// Контракт оригинала: LAST_BROKEN_TILEENTITY живёт НЕ ДОЛЬШЕ тика — «Making sure it is being free'd up in order
	// to prevent exploits or Garbage Collection mishaps» (GT_API_Proxy.onServerTick, оригинал :250). ThreadLocal:
	// серверная чистка не видит КЛИЕНТСКУЮ копию, а в neo слом идёт клиент-предикшеном (MultiPlayerGameMode.destroyBlock
	// → onDestroyedByPlayer) и ставит её на Render-потоке → WD.te вечно отдавал призрак сломанного BE → его
	// ITileEntitySurface-opaque гасил грань соседнего блока до СЛЕДУЮЩЕГО слома (U3 «блуждающая дыра» стен).
	// Зеркало той же строки оригинала на клиентском тике — жизненный цикл восстановлен 1:1.
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public void onClientTickFreeLastBrokenTileEntity(net.minecraftforge.event.TickEvent.ClientTickEvent aEvent) {
		if (aEvent.phase != net.minecraftforge.event.TickEvent.Phase.END) return; // 1.20.1: фаза — поле события (форма 1.7.10), отдельного .Post-типа нет
		gregapi.data.CS.LAST_BROKEN_TILEENTITY.set(null);
	}

	// НАДЁЖНЫЙ МОСТ синка (пара к буферу NetworkHandler.PENDING): каждый клиент-тик доигрываем координатные
	// GT6-пакеты, обогнавшие свой чанк при логине (иначе worldgen-MTE стартовой области оставались без клиент-BE).
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public void onPendingPackets(net.minecraftforge.event.TickEvent.ClientTickEvent aEvent) {
		if (aEvent.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
		gregapi.network.NetworkHandler.processPending(Minecraft.getInstance().level);
	}

	// ЦЕНТР ЛОКАЛИЗАЦИИ (BUG-082), клиентское плечо. Таблица переводов движка пересоздаётся ПРИ КАЖДОЙ загрузке
	// ресурсов (ClientLanguage.loadFrom) — вместе с ней исчезают имена GT6, дописанные ранее. Здесь центр доливается
	// целиком: событие приходит и на первой загрузке, и на каждой перезагрузке (F3+T, смена ресурспака, смена языка).
	// Сам долив и его обоснование — gregapi.lang.LanguageHandler.injectIntoEngine().
	// Ветка 1.20.1: отдельного «ресурсы загружены» события нет — сигнал берётся штатным слушателем перезагрузки
	// ресурсов (RegisterClientReloadListenersEvent, мод-шина): он вызывается и на первой загрузке, и на каждой
	// последующей — ровно тот набор моментов, что нёс ClientResourceLoadFinishedEvent.
	public void registerClientResourceListener(net.minecraftforge.client.event.RegisterClientReloadListenersEvent aEvent) {
		aEvent.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) aManager -> {
			int tInjected = gregapi.lang.LanguageHandler.injectIntoEngine();
			if (tInjected > 0) gregapi.data.CS.OUT.println("GT6 localization: имён GT6 дописано в таблицу движка: " + tInjected);
		});
	}

	/**
	 * ВТОРОЙ НОСИТЕЛЬ ПЕРЕВОДА НА КЛИЕНТЕ (MODCOMPAT-014).
	 *
	 * <p>{@code I18n} держит СОБСТВЕННЫЙ указатель на таблицу ({@code I18n.java:11} — {@code private static
	 * volatile Language language}), и ставит его единственное место — {@code LanguageManager.apply:66-68}:
	 * <pre>  I18n.setLanguage(locale);   Language.inject(locale);</pre>
	 * Надстройка GT6 приходит позже и только через {@code Language.inject} ({@code Language.java:121-123}),
	 * которое это поле НЕ трогает. Поэтому {@code I18n.exists/get} не видели имён GT6 вообще: замер на живом
	 * клиенте — {@code Language.getInstance()} знает 4 ключа из 5, {@code I18n} — 0, и объекты у каналов
	 * разные. Через {@code I18n} спрашивают сторонние моды (Jade — переводы своих конфиг-опций, отсюда
	 * «Missing config translation»), поэтому носителя два, а имя должно быть одно.
	 *
	 * <p>Ставим во второй носитель ТУ ЖЕ надстройку, что уже стоит в первом, — и там же, где движок ставит
	 * свою: после каждой загрузки ресурсов. Штатной точки расширения у {@code I18n} нет ({@code setLanguage}
	 * пакетный), поэтому поле берётся отражением — тем же приёмом, каким мод уже достаёт закрытые узлы
	 * движка ({@code GT6ItemModel} → {@code ModBakery.resolvedModels}). Отказ не тихий: если приём перестанет
	 * работать, в лог уйдёт строка, а не молчание.
	 *
	 * <p>Метод живёт в КЛИЕНТСКОМ прокси намеренно: {@code I18n} — {@code @OnlyIn(Dist.CLIENT)}, и упоминание
	 * его в общем классе тянет клиентский тип на выделенный сервер (класс дефекта BUG-092).
	 */
	@Override public void syncClientI18n() {
		try {
			java.lang.reflect.Field tField = net.minecraft.client.resources.language.I18n.class.getDeclaredField("language");
			tField.setAccessible(true);
			if (tField.get(null) != net.minecraft.locale.Language.getInstance()) tField.set(null, net.minecraft.locale.Language.getInstance());
		} catch (Throwable e) {
			gregapi.data.CS.ERR.println("GT6 localization: I18n остался без имён GT6 — сторонние моды их не увидят (" + e + ").");
		}
	}

	// F-tileentity-construction (КЛИЕНТ-реконструкция MTE-BE): neo подменяет не-PrefixBlock GT6-MTE общим MTE_TYPE →
	// TileEntityLoaderStub при десериализации BE чанка НА КЛИЕНТЕ. Стаб — не IRenderedBlockObject → passRenderingToObject=null
	// → getRenderPasses=0 → MTE-блок НЕ рисуется (прозрачный: камни/палки/флюид-источники/машины). Серверная реконструкция
	// (server-tick) клиент не покрывает — у него ОТДЕЛЬНЫЕ BE. Здесь дренируем клиентскую очередь стабов на client-tick,
	// заменяя их настоящими MTE (единый механизм GT6WorldgenFeature.reconstructChunkMTEs, теперь Level-обобщённый).
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public void onClientMTEReconstruct(net.minecraftforge.event.TickEvent.ClientTickEvent aEvent) {
		if (aEvent.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
		if (Minecraft.getInstance().level == null) return;
		try { gregapi.worldgen.GT6WorldgenFeature.drainClientStubs(); } catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
	}


	// F5/F3-render (client): рендер ВСЕХ GT6-жидкостей. GT6-жидкость = still/flow-текстура (mTexture, IIconContainer)
	// + цвет (mRGBa, тинтит серый молтен). Ветка 1.20.1: движок берёт эти три величины не из модели и не из события,
	// а из расширения ТИПА жидкости — {@code IClientFluidTypeExtensions} ({@code FluidType.initializeClient},
	// FluidType.java:899), поэтому здесь живёт ЕДИНЫЙ переходник, а зовёт его сам GTFluidType (тот же приём ленивого
	// invokestatic из common-класса, что у item-расширений MTE). Централизация 1:1 — одна фабрика на весь мод,
	// как GT6BlockModel/GT6ItemModel. Fallback на воду при null-иконе.
	public static void bindFluidClientExtensions(gregapi.fluid.FluidGT aFluid, java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> aConsumer) {
		aConsumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
			private net.minecraft.resources.ResourceLocation texture() {
				try { if (aFluid.mTexture != null) { net.minecraft.resources.ResourceLocation t = aFluid.mTexture.getIcon(0); if (t != null) return t; } } catch (Throwable e) {/* невалидная икона → fallback вода */}
				return null;
			}
			@Override public net.minecraft.resources.ResourceLocation getStillTexture() {
				net.minecraft.resources.ResourceLocation t = texture();
				return t != null ? t : net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still");
			}
			@Override public net.minecraft.resources.ResourceLocation getFlowingTexture() {
				net.minecraft.resources.ResourceLocation t = texture();
				return t != null ? t : net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_flow");
			}
			@Override public int getTintColor() {
				short[] tRGBa = aFluid.getRGBa();
				return (tRGBa != null && tRGBa.length >= 3) ? (0xFF000000 | ((tRGBa[0]&0xFF)<<16) | ((tRGBa[1]&0xFF)<<8) | (tRGBa[2]&0xFF)) : 0xFFFFFFFF;
			}
		});
	}

	// Рантайм-инъекция: каждому BlockState каждого GT6-блока-рендера назначаем единственный GT6BlockModel
	// (модель динамическая — читает блок/позицию/состояние в collectParts, один инстанс на весь мод).
	private void onModifyBakingResult(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult aEvent) {
		// Правка №3 (BUG-106): атлас/модели пересозданы — старые спрайты в кэшах геометрии мертвы, сбрасываем.
		gregapi.render.GT6ItemModel.invalidateCaches();
		gregapi.render.GT6BlockModel.invalidateParticle();
		// Ветка 1.20.1: карта результата ОДНА и общая — ключ блока это ModelResourceLocation состояния
		// (BlockModelShaper.stateToModelLocation, BlockModelShaper.java:52), ключ предмета — «<id>#inventory»
		// (ModelResourceLocation.java:26; ItemModelShaper ищет модель именно по нему).
		java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> tMap = aEvent.getModels();
		int tCount = 0;
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			// GT6BlockModel — и IRenderedBlock, и BlockBaseRail (рельсы: своя рельс-ветка, плоский quad по мете).
			if (!(tBlock instanceof gregapi.render.IRenderedBlock) && !(tBlock instanceof gregapi.block.misc.BlockBaseRail)) continue;
			// per-BlockState инстанс: модель обязана знать владельца там, где движок контекста мира НЕ даёт
			// (breaking-оверлей, статический запрос модели), и мету — для спрайта крошки (getParticleIcon без pos).
			for (net.minecraft.world.level.block.state.BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
				tMap.put(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(tState), new gregapi.render.GT6BlockModel(tBlock, tState));
				tCount++;
			}
		}
		// F3-render: ЕДИНАЯ item-модель ВСЕМ GT6-предметам (включая block-предметы: их item-форму рисует GT6ItemModel через
		// buildInventoryQuads = renderInventoryBlock). Прежде block-предметы пропускались → у них не было item-модели → пурпур.
		gregapi.render.GT6ItemModel tItemModel = new gregapi.render.GT6ItemModel();
		int tItemCount = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			net.minecraft.resources.ResourceLocation tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null || !isGregNamespace(tKey.getNamespace())) continue;
			// block-предмет инжектим, если его блок — IRenderedBlock ИЛИ рельс (BlockBaseRail: GT6ItemModel рисует ему плоскую
			// straight-иконку); прочие block-предметы оставляем дефолтной модели блока.
			if (tItem instanceof net.minecraft.world.item.BlockItem tBI && !(tBI.getBlock() instanceof gregapi.render.IRenderedBlock) && !(tBI.getBlock() instanceof gregapi.block.misc.BlockBaseRail)) continue;
			tMap.put(new net.minecraft.client.resources.model.ModelResourceLocation(tKey, "inventory"), tItemModel); tItemCount++;
		}
		// Гигиена («Missing model for variant»): GT6-блоки с RenderShape.INVISIBLE (fluid-блоки river/ocean/swamp — сам блок
		// невидим 1:1 к vanilla LiquidBlock, вода рисуется FluidState/F5-подсистемой) не имеют baked-модели → ModelManager сыпал
		// предупреждение на КАЖДЫЙ их BlockState-вариант (48 шт). Кладём пустую модель (тот же GT6BlockModel: для не-IRenderedBlock
		// геометрия пуста) — движок находит модель, предупреждение уходит; визуал не меняется (блок и так INVISIBLE).
		gregapi.render.GT6BlockModel tEmptyModel = new gregapi.render.GT6BlockModel();
		int tEmptyCount = 0;
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			net.minecraft.resources.ResourceLocation tBKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tBKey == null || !isGregNamespace(tBKey.getNamespace())) continue;
			for (net.minecraft.world.level.block.state.BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
				net.minecraft.client.resources.model.ModelResourceLocation tStateKey = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(tState);
				if (tMap.containsKey(tStateKey) || tState.getRenderShape() != net.minecraft.world.level.block.RenderShape.INVISIBLE) continue;
				tMap.put(tStateKey, tEmptyModel); tEmptyCount++;
			}
		}
		gregapi.data.CS.OUT.println("[GT6] F3-render: GT6BlockModel injected into " + tCount + " block-states, GT6ItemModel into " + tItemCount + " items, " + tEmptyCount + " invisible-block placeholders.");
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
	public java.io.InputStream getResourceStream(net.minecraft.resources.ResourceLocation aRL) {
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

	// BUG-039 v4 (аудит JPMS-mirror): метод БЫЛ мёртвым сиротой — сигнатура (FMLCommonSetupEvent) не совпадала с
	// базовой Abstract_Proxy.onProxyAfterPreInit(Abstract_Mod, FMLPreInitializationEvent), @Override был
	// закомментирован → Abstract_Mod:167 его никогда не звал; OptiFine-детект и сезонная листва были потеряны
	// молча. Сигнатура исправлена, канал жив. RenderingRegistry-заглушки (registerEntityRenderingHandler/
	// registerBlockHandler — F3-суперсид GT6BlockModel-пайплайном, no-op по замыслу) СНЯТЫ: их mirror-класс
	// cpw.* JPMS-вырезан из рантайма, исполнение кидало бы NoClassDefFoundError (см. decisions/F3-render.md §1,2.1,2.5).
	@Override
	public void onProxyAfterPreInit(Abstract_Mod aMod, gregapi.api.FMLPreInitializationEvent aEvent) {
		// Check if OptiFine is loaded in order to disable some GT Render Hooks to fix Glitches.
		// 1:1-мост: 1.7.10 FMLClientHandler.hasOptifine() = детект Class.forName("Config") (FMLClientHandler:272-286,
		// референс) — FML-обёртка удалена, сам детект воспроизведён; + net.optifine.Config (современный путь OF).
		boolean tOptifine = F;
		try {Class.forName("Config", false, GT_API_Proxy_Client.class.getClassLoader()); tOptifine = T;} catch(Throwable e) {/**/}
		if (!tOptifine) try {Class.forName("net.optifine.Config", false, GT_API_Proxy_Client.class.getClassLoader()); tOptifine = T;} catch(Throwable e) {/**/}
		ITexture.Util.OPTIFINE_LOADED = tOptifine;
		
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
	 * 1.7.10 {@code TextureStitchEvent.Pre} нёс здесь страховку Грегориуса «жидкость без иконки или с битой
	 * иконкой получает иконку своего блока либо воды» (оригинал {@code GT_API_Proxy_Client:194-212}). В neo
	 * Pre-события нет ({@code TextureAtlasStitchedEvent} приходит только ПОСЛЕ стежки), а мутировать чужую
	 * жидкость нельзя — модель жидкости регистрирует владеющий мод. Функция страховки НЕ утрачена: она несётся
	 * тремя централизованными плечами. Ветка «иконки нет» ({@code getIcon()==null}) — {@link gregapi.data.FL#stillIcon}
	 * (заведено в BUG-049: нет своей текстуры → {@code water_still}; потребители — дисплеи и ВСЕ баки через
	 * {@link gregapi.render.BlockTextureFluid}). Ветка «иконка битая» ({@code FluidsGT.BROKEN}) —
	 * {@code BlockTextureFluid:105}: спрайт не нашёлся в атласе → {@code water_still}. Мировой рендер —
	 * {@link #onRegisterFluidModels}: фолбэк на воду при null/невалидной иконе (baked-фаза F3 — ровно куда
	 * прежняя метка отложенности предписывала перенести фикс). Списки-исключения оригинала перенесены 1:1:
	 * {@code BROKEN} пуст, {@code BORKEN} несёт одну thaumcraft-жидкость (мода в сборке нет) — применимых
	 * чужих случаев нет, жидкости регистрируют только ваниль и GT6. Судья фолбэка — {@code gt6itemmodelprobe}
	 * (BUG-068): river/ocean/swamp — жидкости БЕЗ своей текстуры — получают {@code water_still}, 10/10.
	 * Обработчик оставлен пустым 1:1 к точке подписки оригинала.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTextureStitchedPre(TextureStitchEvent.Post aEvent) {
		//
	}

	/** Клиентское плечо моста оплаты маяка (центр — {@link GT_API_Proxy#wrapBeaconPaymentSlot}): клиент строит
	 *  СВОЙ экземпляр {@code BeaconMenu} по сетевому пакету, серверная подмена слота его не достигает — без
	 *  этого плеча клиентское предсказание клика отвергало бы стек, который сервер принимает. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onScreenOpening(net.minecraftforge.client.event.ScreenEvent.Opening aEvent) {
		if (aEvent.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.BeaconScreen tScreen) GT_API_Proxy.wrapBeaconPaymentSlot(tScreen.getMenu());
	}

	/**
	 * 1.7.10 {@code net.minecraftforge.event.entity.player.ItemTooltipEvent}
	 * держал tooltip как {@code List<String>} напрямую в поле {@code toolTip}; neo-эквивалент
	 * {@code net.minecraftforge.event.entity.player.ItemTooltipEvent} (`neoforge-decompiled/.../ItemTooltipEvent.java:16-70`)
	 * — геттеры, а список типизирован {@code List<Component>} (движко-шов, т.к. рендер текста теперь
	 * дерево {@code Component}, не сырая строка). Вся GT6-логика ниже (300+ строк) оперирует СТРОКАМИ
	 * (конкатенация {@code LH.Chat.*} §-кодов, {@code replaceAll}, и т.д.) и передаётся в
	 * {@code ICover.addToolTips(List<String>,...)} (сотни реализаций по всему моду, {@code List<String>}
	 * НЕ тронут — вне зоны этого захода) — чтобы не терять ни строки бизнес-логики (R8), тело работает
	 * на ЛОКАЛЬНОЙ {@code List<String>}-копии (снятой из {@code Component.getString()} до, собранной
	 * обратно через {@code Component.literal(...)} после — §-коды внутри literal-строки по-прежнему
	 * рендерятся движком, см. {@code FormattedCharSequence}); синхронизация — в {@code finally}, чтобы
	 * сработать на ЛЮБОМ выходе (return/exception), как оригинал мутировал список напрямую. Harvest-строка
	 * восстановлена ниже: {@code Block.getHarvestTool/getHarvestLevel} удалены из neo по имени, но те же
	 * величины отдаёт центр {@link WD} из паспорта блока 1.7.10 (данные оракула, см. {@code WD.vanillaPassport}).
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemTooltip(ItemTooltipEvent aEvent) {
		if (Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI || ST.invalid(aEvent.getItemStack())) return;
		if (!DISPLAY_TEMP_TOOLTIP) {DISPLAY_TEMP_TOOLTIP = T; return;}

		List<Component> tTT = aEvent.getToolTip();
		List<String> aToolTip = new ArrayList<>(tTT.size());
		for (Component tC : tTT) aToolTip.add(tC == null ? null : tC.getString());
		try {
			if (UT.NBT.getNBT(aEvent.getItemStack()).getBoolean("gt.err.oredict.output")) {
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
				if (aBlock == Blocks.COARSE_DIRT) {
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
					// 1:1 с оригиналом (`gregtech6/.../GT_API_Proxy_Client.java:301`): те же три величины, только
					// спрошенные у центра WD — в neo у блока нет ни материала, ни harvestTool/Level по имени.
					aToolTip.add(LH.getToolTipHarvest(WD.getMaterial(aBlock), WD.harvestTool(aBlock, aBlockMeta), WD.harvestLevel(aBlock, aBlockMeta)));
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

			/* Было {@code Item.isBeaconPayment(ItemStack)} (Forge 1.7.10, метод удалён) — центральный предикат моста
			 * (тег {@code BEACON_PAYMENT_ITEMS} ИЛИ контракт IItemBeaconPayment): тултип отвечает то же, что слот маяка. */
			if (GT_API_Proxy.isBeaconPayment(aEvent.getItemStack())) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_BEACON_PAYMENT));
			}

			/* F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code cpw.mods.fml.common.registry.GameRegistry.getFuelValue(ItemStack)}
			 * (Forge 1.7.10 static API, удалён) — neo {@code Level.fuelValues().burnDuration(ItemStack)}
			 * (`neo-decompiled/net/minecraft/world/level/block/entity/FuelValues.java:38`), инстанс с клиентского
			 * {@code Minecraft.getInstance().level} (ближайший клиентский эквивалент world-контекста). */
			Level tClientLevel = Minecraft.getInstance().level;
			long tBurnValue = tClientLevel == null ? 0 : net.minecraftforge.common.ForgeHooks.getBurnTime(ST.amount(1, aEvent.getItemStack()), null);
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
							for (ObjectStack<Enchantment> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentTools) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_TOOL_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
								if (tEnchantment.mObject == Enchantments.FIRE_ASPECT && tEnchantment.mAmount >= 3) tToolTip.append(" (Autosmelt)");
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<Enchantment> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentWeapons) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_WEAPON_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<Enchantment> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentAmmo) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_AMMO_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<Enchantment> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentFishing) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_FISHING_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());

							if (!tData.mPrefix.containsAny(TD.Prefix.TOOL_HEAD, TD.Prefix.WEAPON_ALIKE, TD.Prefix.AMMO_ALIKE, TD.Prefix.TOOL_ALIKE)) {
								tToolTip = null;
								for (ObjectStack<Enchantment> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentArmors) {
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
	 *  `compat-mirror/java/cpw/mods/fml/common/gameevent/TickEvent.java` — тип-пустышка, поведение живёт
	 *  здесь) — neo раздельно шлёт {@code ClientTickEvent.Pre}/{@code .Post}
	 *  (`forge-1201-decompiled/net/minecraftforge/event/TickEvent.java:24-29`) — в 1.20.1 фаза снова ПОЛЕ
	 *  события ({@code phase}), как в 1.7.10, поэтому вернулось и условие-обёртка; тело БЕЗ ИЗМЕНЕНИЙ. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onClientTickEvent(ClientTickEvent aEvent) {
		if (aEvent.phase != net.minecraftforge.event.TickEvent.Phase.END) return; // 1.20.1: фаза — поле события (форма 1.7.10)
		{
			if (CLIENT_TIME == 10) {
				// ЗАКРЫТО, тело переехало на серверную сторону — {@code gregtech.GT6_Main.onModServerStarted2}:
				// «Fake Furnace Recipe Map» (наполнение витрины RM.Furnace) и перенос ванильных плавок в реестр
				// GT6 ({@code FurnaceRecipes.importVanilla}). В 1.7.10 это делалось здесь, потому что ванильный
				// список плавок был статикой и существовал уже к клиентскому тику; в neo рецепты data-driven и
				// приходят с датапаком, поэтому момент тот же (мир загружен), а сторона — серверная.
				//
				// Вторая половина 1.7.10-тела — «hiding stuff from NEI» ({@code Item.getSubItems} +
				// {@code CreativeTabs.tabAllSearch}) — ДОЛГА НЕ НЕСЁТ: оригинал (:530-536) прятал микроблоки
				// ЧУЖИХ модов (Forge Microblocks, Extra Utilities, Extra Simple, фасады AE2). Ни один из них не
				// в сборке, {@code ST.item(MD.FMB, "microblock")} = null, цикл был бы пустым.
			}

			// BUG-090: клиентское плечо скольжения slippery. В 1.7.10 Potion.performEffect тикал на ОБЕИХ
			// сторонах (IEPotions.java:118-121 двигал и клиентского игрока); в neo applyEffectTick сервер-only
			// (сигнатура ServerLevel), а движение игрока клиент-авторитетно — серверная добавка скорости до
			// клиента не доезжает. Эффект синхронизирован клиенту штатно (ClientboundUpdateMobEffectPacket) —
			// читаем его здесь тем же вектором и коэффициентом; выброс предмета остаётся серверным.
			{
				net.minecraft.client.player.LocalPlayer tSlipperyPlayer = Minecraft.getInstance().player;
				if (tSlipperyPlayer != null && tSlipperyPlayer.onGround() && tSlipperyPlayer.hasEffect(gregapi.potion.MobEffectsGT.SLIPPERY))
					tSlipperyPlayer.moveRelative(0.005F, new net.minecraft.world.phys.Vec3(0, 0, 1));
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
	 *  — {@link RenderHighlightEvent.Block} не несёт {@code player}/{@code currentItem}/{@code partialTicks}
	 *  старого события. Игрок восстановлен через {@code Minecraft.getInstance().player} (тот же центральный
	 *  паттерн, что {@link #getThePlayer()}); {@code sideHit} — из {@code getHitResult().getDirection()};
	 *  {@code partialTicks} недостижим (0 — нейтрально, единственный потребитель {@link RenderHelper#drawWrenchOverlay}
	 *  уже no-op, см. его javadoc). Ветвление/делегирование в {@link ITileEntityOnDrawBlockHighlight} — БЕЗ ИЗМЕНЕНИЙ. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawBlockHighlight(RenderHighlightEvent.Block aEvent) {
		Player tPlayer = Minecraft.getInstance().player;
		if (tPlayer == null) return;
		byte tSide = (byte)aEvent.getTarget().getDirection().ordinal();
		Block
		aBlock = ST.block(tPlayer.getMainHandItem());
		if (aBlock instanceof BlockMetaType && ((BlockMetaType)aBlock).mIsSlab) {
			RenderHelper.drawWrenchOverlay(aEvent, (byte)0, tSide);
			return;
		}
		aBlock = WD.block(tPlayer.level(), aEvent.getTarget().getBlockPos().getX(), aEvent.getTarget().getBlockPos().getY(), aEvent.getTarget().getBlockPos().getZ());
		BlockEntity aTileEntity = WD.te(tPlayer.level(), aEvent.getTarget().getBlockPos().getX(), aEvent.getTarget().getBlockPos().getY(), aEvent.getTarget().getBlockPos().getZ(), T);
		if (!(aTileEntity instanceof ITileEntityOnDrawBlockHighlight) || !((ITileEntityOnDrawBlockHighlight)aTileEntity).onDrawBlockHighlight(aEvent)) {
			if ((ROTATABLE_VANILLA_BLOCKS.contains(aBlock) || (ToolCompat.IC_WRENCHABLE && aTileEntity instanceof ic2.api.tile.IWrenchable)) && ST.valid(tPlayer.getMainHandItem()) && ToolsGT.contains(TOOL_wrench, tPlayer.getMainHandItem())) {
				RenderHelper.drawWrenchOverlay(aEvent, (byte)0, tSide);
				return;
			}
		}
	}
	
	private static List<Block> ROTATABLE_VANILLA_BLOCKS = Arrays.asList(Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.FURNACE, Blocks.FURNACE, Blocks.DROPPER, Blocks.DISPENSER, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.HOPPER, Blocks.PUMPKIN, Blocks.JACK_O_LANTERN);
}
