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
		// BUG-039 v4 (JPMS-mirror audit): 1.7.10 RenderingRegistry.addNewArmourRendererPrefix (custom armor-texture
		// layer) was removed together with the whole armor-render model (neo: humanoid layers via equipment assets,
		// see ItemArmorBase/F13); the mirror class cpw.* is JPMS-cut from the runtime (the call used to throw NoClassDefFoundError into
		// an empty catch). Callers of the method: 0 (grep) — the returned index is not consumed by the neo renderer.
		return 0;
	}

	// F3-render (client): registration of the single dynamic model type for all GT6 blocks on the mod-bus.
	// Replacement for the removed `RenderingRegistry.registerBlockHandler`/render-id dispatcher (decisions/F3-render.md §2.1):
	// one `GT6BlockModel` type. Two points: (1) RegisterBlockStateModels — the type for blockstate JSON (fallback);
	// (2) ModifyBakingResult — runtime model injection for ALL GT6 blocks (IRenderedBlock) WITHOUT JSON — a procedural
	// mod (hundreds of blocks dynamically) cannot hold thousands of static JSONs; centralization 1:1 (one model for the whole mod).
	/** BUG-056: the client half of "open all recipes of this machine" — delegates to the single JEI-compatibility
 	 *  center ({@link gregapi.jei.GT6_JEI_Plugin#showRecipeCategory}), which holds the live runtime
 	 *  and the "category name → type" map. Same key {@code mNameNEI} that 1.7.10 used to call NEI. */
	@Override
	public boolean openRecipeGui(String aNameNEI) {return gregapi.jei.GT6_JEI_Plugin.showRecipeCategory(aNameNEI);}

	/** Client half of 1.7.10 displayGUIBook (EntityPlayerSP:379-391): opens the book screen with the PASSED
	 *  stack. Pages come from the component when the engine wrote one, else from the GT6 flat NBT "pages"
	 *  (UT.Books.createWrittenBook:491) — one converter, both worlds. */
	@Override
	public void displayBook(net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.item.ItemStack aStack, boolean aWritable) {
		net.minecraft.client.Minecraft tMC = net.minecraft.client.Minecraft.getInstance();
		if (aWritable) {
			net.minecraft.world.item.component.WritableBookContent tContent = aStack.get(net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT);
			if (tContent == null) tContent = new net.minecraft.world.item.component.WritableBookContent(bookPagesRaw(aStack).stream().map(net.minecraft.server.network.Filterable::passThrough).toList());
			tMC.setScreen(new net.minecraft.client.gui.screens.inventory.BookEditScreen(aPlayer, aStack, net.minecraft.world.InteractionHand.MAIN_HAND, tContent));
			return;
		}
		java.util.List<net.minecraft.network.chat.Component> tPages;
		net.minecraft.world.item.component.WrittenBookContent tWritten = aStack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
		if (tWritten != null) tPages = tWritten.getPages(tMC.isTextFilteringEnabled());
		else tPages = bookPagesRaw(aStack).stream().map(tPage -> (net.minecraft.network.chat.Component)net.minecraft.network.chat.Component.literal(tPage)).toList();
		tMC.setScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess(tPages)));
	}

	private static java.util.List<String> bookPagesRaw(net.minecraft.world.item.ItemStack aStack) {
		net.minecraft.nbt.ListTag tList = gregapi.util.UT.NBT.getNBT(aStack).getListOrEmpty("pages");
		java.util.List<String> rPages = new java.util.ArrayList<>(tList.size());
		for (int i = 0; i < tList.size(); i++) rPages.add(tList.getStringOr(i, ""));
		return rPages;
	}

	@Override
	public void registerClientModels(net.neoforged.bus.api.IEventBus aModBus) {
		aModBus.addListener(this::onRegisterBlockStateModels);
		aModBus.addListener(this::onModifyBakingResult);
		aModBus.addListener(this::onRegisterFluidModels);
		aModBus.addListener(this::onRegisterBlockEntityRenderers);
		aModBus.addListener(this::onRegisterMenuScreens);
		aModBus.addListener(this::onRegisterBlockTints);
	}

	// ===== F3 tint CENTER: color of GT6 blocks ====================================================================
	// In 1.7.10 the engine asked the BLOCK ITSELF for color via two methods: getRenderColor(meta) — color by subtype, and
	// colorMultiplier(world,x,y,z) — color at a point in the world (biome tint). Both methods survive in the port, but had no
	// callers: neo does NOT ask the block for color — it takes it from a REGISTRY of tint sources
	// (BlockColors.register(List<BlockTintSource>, Block...), RegisterColorHandlersEvent.BlockTintSources:68).
	// So there is ONE registration for the whole mod here, not a bridge per block: there is nothing to wire into seven roots —
	// the engine simply goes a different route. The source below asks those very 1.7.10 methods, values are not duplicated.
	// Consequence before this fix: crashed blocks (BlockColored — colored glass and kin) rendered without color,
	// and the biome tint of copied textures did not work (deferred-work marker in BlockTextureCopied:36 — the same
	// channel; the marker word is deliberately NOT written verbatim here, otherwise the marker counter would count this mention as a marker).
	private void onRegisterBlockTints(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources aEvent) {
		java.util.List<net.minecraft.client.color.block.BlockTintSource> tSource = java.util.List.of(new GT6BlockTint());
		java.util.List<net.minecraft.world.level.block.Block> tBlocks = new java.util.ArrayList<>();
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			net.minecraft.resources.Identifier tID = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tID == null || !(tID.getNamespace().equals(gregapi.data.CS.ModIDs.GT) || tID.getNamespace().equals("gregtech"))) continue;
			if (tBlock instanceof gregapi.block.IBlock) tBlocks.add(tBlock);
		}
		if (!tBlocks.isEmpty()) aEvent.register(tSource, tBlocks.toArray(new net.minecraft.world.level.block.Block[0]));
		gregapi.data.CS.OUT.println("[F3-tint] tint source registered for GT6 blocks: " + tBlocks.size());
	}

	/** Bridge "engine asks about color" → "GT6 block's 1.7.10 methods". One for the whole mod; values live in the blocks. */
	private static final class GT6BlockTint implements net.minecraft.client.color.block.BlockTintSource {
		/** Question without a world — 1.7.10 {@code getRenderColor(meta)}. The subtype is taken via the EXISTING meta↔BlockState
 		 *  center ({@code IBlockExtendedMetaData.getExtendedMetaData}), not via a private property parse. */
		@Override public int color(net.minecraft.world.level.block.state.BlockState aState) {
			if (!(aState.getBlock() instanceof gregapi.block.IBlock tBlock)) return 16777215;
			int tMeta = aState.getBlock() instanceof gregapi.block.IBlockExtendedMetaData tMetaBlock ? tMetaBlock.getExtendedMetaData(aState) : 0;
			return tBlock.getRenderColor(tMeta);
		}
		/** Question with a world — 1.7.10 {@code colorMultiplier(world,x,y,z)} (biome tint and other positional data). */
		@Override public int colorInWorld(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.client.renderer.block.BlockAndTintGetter aLevel, net.minecraft.core.BlockPos aPos) {
			return aState.getBlock() instanceof gregapi.block.IBlock tBlock ? tBlock.colorMultiplier(aLevel, aPos.getX(), aPos.getY(), aPos.getZ()) : color(aState);
		}
	}

	// F14-gui: CLIENT-side registration of the screen for ContainerCommon.MENU_TYPE (without it neo crashes when opening any mod GUI —
	// "no screen for menu type"). The factory routes into the SINGLE GT6 center getGUIClient (the same one that built the screen in
	// 1.7.10 — per-machine ContainerClient subclass+texture); fallback (getGUIClient=null/exception) — a wrapper
	// of the neo-reconstructed menu with a plain ContainerClient (no crash).
	private void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent aEvent) {
		if (gregapi.gui.ContainerCommon.MENU_TYPE == null) return;
		aEvent.<gregapi.gui.ContainerCommon, gregapi.gui.ContainerClient>register(gregapi.gui.ContainerCommon.MENU_TYPE.get(), (aMenu, aInv, aTitle) -> {
			// an exception here = disconnect (neoforge ClientPayloadHandler.createMenuScreen catch→disconnect) → a total null-gate
			if (aMenu == null) aMenu = new gregapi.gui.ContainerCommon(0, aInv);
			// containerId bridge (root of "the slot exists on the server but is not displayed"): getGUIClient builds a FRESH
			// client-side container via the legacy constructor (id from sPendingWindowID); outside withWindowID that equals -1 →
			// client id ≠ server id → ALL content/slot packets for the menu are silently dropped by the client (id check in
			// handleContainerSetSlot/Content). We wrap the factory in a bridge carrying the network menu's id (= the server id).
			final gregapi.gui.ContainerCommon fMenu = aMenu;
			try { if (aMenu.mTileEntity instanceof gregapi.tileentity.ITileEntityGUI tGUI) { Object tScreen = gregapi.gui.ContainerCommon.withWindowID(fMenu.containerId, () -> tGUI.getGUIClient(fMenu.mGUIID, aInv.player)); if (tScreen instanceof gregapi.gui.ContainerClient tCC) {
				// counter-balance (chest-"chorus"): the network container (createFromNetwork) ALREADY called openInventoryGUI on the client TE,
				// and the fresh container from getGUIClient called it ONE MORE time; removed() on close decrements ONCE → the client-side
				// mUsingPlayers stuck >0 forever (lid permanently open, everyone hears the "opened" sound when entering the area).
				// Compensation: close the network container's own count — the screen owns only its own.
				fMenu.mTileEntity.closeInventoryGUI();
				return tCC;
			} } }
			catch (Throwable e) { gregapi.data.CS.OUT.println("[GT6-GUI] getGUIClient threw, fallback screen: "+e); }
			return new gregapi.gui.ContainerClient(aMenu, gregapi.data.CS.RES_PATH_GUI + "chests/" + (aMenu.mTileEntity == null ? 1 : aMenu.mTileEntity.getSizeInventoryGUI()) + ".png");
		});
		gregapi.data.CS.OUT.println("[GT6-GUI] MenuScreens: screen for ContainerCommon.MENU_TYPE registered (F14).");
	}

	// F3-render: the MTE's appearance is rendered via the SECTION MESH (GT6BlockModel), as in 1.7.10 — BUG-138 carrier #2. The BER remains for
	// the same purpose bindTileEntitySpecialRenderer served in 1.7.10: chest, mass-storage and cracks on the live
	// geometry of the block being broken. Registration by BlockEntityType — the engine has no other way, dispatch by class happens inside BER.
	private void onRegisterBlockEntityRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers aEvent) {
		// BUG-138: the hierarchy now has two types (a ticking half and a non-ticking half — the trait is declared to the engine as a type,
		// see TileEntityBase01Root.MTE_TYPE_NOTICK). Rendering has nothing to do with ticking: appearance is drawn for BOTH
		// halves, and non-ticking ones are in fact the majority in the world (stones, bushes, sticks). It is the same renderer either way — otherwise
		// half the world would turn invisible.
		if (gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE != null)
			aEvent.registerBlockEntityRenderer(gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE, gregapi.render.MultiTileEntityBER::new);
		if (gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE_NOTICK != null)
			aEvent.registerBlockEntityRenderer(gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE_NOTICK, gregapi.render.MultiTileEntityBER::new);
		// F12-entity: renderer for the falling meta-block — 1:1 of the original (:126 registerEntityRenderingHandler(
		// PrefixBlockFallingEntity.class, new RenderFallingBlock())): the same vanilla falling-block renderer,
		// it draws whatever getBlockState() returns (for us — gravel, exactly as the author intended, see PrefixBlockFallingEntity).
		aEvent.registerEntityRenderer(GT_API.METABLOCK_FALLING.get(), net.minecraft.client.renderer.entity.FallingBlockRenderer::new);
	}

	// Render acceptance scan (gate ②): on the first client tick, once the atlas is stitched AND DataComponents are BOUND (on
	// ModelEvent.BakingCompleted they are not yet bound → Item.getDefaultInstance NPEs "Components not bound yet"). Verifies
	// that GT6 item icons resolve (not purple). Once. Writes found/missing into gregtech.log (game-bus, auto-registered).

	// Original contract: LAST_BROKEN_TILEENTITY lives NO LONGER than a tick — "Making sure it is being free'd up in order
	// to prevent exploits or Garbage Collection mishaps" (GT_API_Proxy.onServerTick, original :250). ThreadLocal:
	// server-side cleanup does not see the CLIENT copy, and in neo the break happens via client prediction (MultiPlayerGameMode.destroyBlock
	// → onDestroyedByPlayer) and sets it on the Render thread → WD.te kept handing back the ghost of the broken BE → its
	// ITileEntitySurface-opaque suppressed the neighboring block's face until the NEXT break (U3 "wandering hole" in walls).
	// A mirror of the same original line on the client tick — the lifecycle is restored 1:1.
	@net.neoforged.bus.api.SubscribeEvent
	public void onClientTickFreeLastBrokenTileEntity(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		gregapi.data.CS.LAST_BROKEN_TILEENTITY.set(null);
	}

	// RELIABLE SYNC BRIDGE (a counterpart to the NetworkHandler.PENDING buffer): every client tick, catch up on positional
	// GT6 packets that outran their chunk during login (otherwise worldgen MTEs in the starting area were left without a client BE).
	@net.neoforged.bus.api.SubscribeEvent
	public void onPendingPackets(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		gregapi.network.NetworkHandler.processPending(Minecraft.getInstance().level);
	}

	// LOCALIZATION CENTER (BUG-082), client-side arm. The engine's translation table is recreated on EVERY resource
	// reload (ClientLanguage.loadFrom) — taking with it the GT6 names appended earlier. Here the center refills it
	// fully: the event fires both on the first load and on every reload (F3+T, resource-pack switch, language switch).
	// The refill itself and its rationale — gregapi.lang.LanguageHandler.injectIntoEngine().
	@net.neoforged.bus.api.SubscribeEvent
	public void onClientResourcesLoaded(net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent aEvent) {
		int tInjected = gregapi.lang.LanguageHandler.injectIntoEngine();
		if (tInjected > 0) gregapi.data.CS.OUT.println("GT6 localization: GT6 names appended to the engine table: " + tInjected + (aEvent.isInitial() ? " (first resource load)" : " (resource reload)"));
	}

	/**
 	 * SECOND TRANSLATION CARRIER ON THE CLIENT (MODCOMPAT-014).
 	 *
 	 * <p>{@code I18n} holds its OWN pointer to the table ({@code I18n.java:11} — {@code private static
 	 * volatile Language language}), and the only place that sets it is {@code LanguageManager.apply:66-68}:
 	 * <pre>  I18n.setLanguage(locale);   Language.inject(locale);</pre>
 	 * The GT6 overlay arrives later and only through {@code Language.inject} ({@code Language.java:121-123}),
 	 * which does NOT touch this field. So {@code I18n.exists/get} never saw GT6 names at all: a measurement on a live
 	 * client showed {@code Language.getInstance()} knows 4 of 5 keys, {@code I18n} — 0, and the channel objects are
 	 * different. Third-party mods ask through {@code I18n} (Jade — translations of its own config options, hence
 	 * "Missing config translation"), so there are two carriers, and there should be one name.
 	 *
 	 * <p>We install into the second carrier the SAME overlay already installed into the first — at the same place the engine
 	 * installs its own: after every resource load. There is no official extension point on {@code I18n} ({@code setLanguage}
 	 * is package-private), so the field is taken via reflection — the same trick the mod already uses to reach closed engine
 	 * internals ({@code GT6ItemModel} → {@code ModBakery.resolvedModels}). Failure is not silent: if the trick stops
 	 * working, a line goes into the log rather than silence.
 	 *
 	 * <p>The method deliberately lives in the CLIENT proxy: {@code I18n} is {@code @OnlyIn(Dist.CLIENT)}, and naming
 	 * it in a common class would drag the client-only type onto the dedicated server (defect class BUG-092).
	 */
	// Lives in the client proxy for the same reason as syncClientI18n: LanguageManager is a client-only type and
	// naming it in a common class drags it onto the dedicated server (defect class BUG-092).
	@Override public String selectedLanguage() {
		try {return net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();} catch (Throwable e) {return null;}
	}

	@Override public void syncClientI18n() {
		try {
			java.lang.reflect.Field tField = net.minecraft.client.resources.language.I18n.class.getDeclaredField("language");
			tField.setAccessible(true);
			if (tField.get(null) != net.minecraft.locale.Language.getInstance()) tField.set(null, net.minecraft.locale.Language.getInstance());
		} catch (Throwable e) {
			gregapi.data.CS.ERR.println("GT6 localization: I18n was left without GT6 names — third-party mods will not see them (" + e + ").");
		}
	}

	// F-tileentity-construction (CLIENT MTE-BE reconstruction): neo substitutes non-PrefixBlock GT6 MTEs with a common MTE_TYPE →
	// TileEntityLoaderStub during BE deserialization ON THE CLIENT. The stub is not IRenderedBlockObject → passRenderingToObject=null
	// → getRenderPasses=0 → the MTE block is NOT rendered (transparent: stones/sticks/fluid sources/machines). Server-side reconstruction
	// (server-tick) does not cover the client — it has SEPARATE BEs. Here we drain the client-side stub queue on client-tick,
	// replacing them with real MTEs (the same unified mechanism GT6WorldgenFeature.reconstructChunkMTEs, now Level-generic).
	@net.neoforged.bus.api.SubscribeEvent
	public void onClientMTEReconstruct(net.neoforged.neoforge.client.event.ClientTickEvent.Post aEvent) {
		if (Minecraft.getInstance().level == null) return;
		try { gregapi.worldgen.GT6WorldgenFeature.drainClientStubs(); } catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
	}


	// F5/F3-render (client): a single dynamic FluidModel for ALL GT6 fluids (replacing "Missing FluidModel" with a real
	// render). A GT6 fluid = still/flow texture (mTexture, IIconContainer) + color (mRGBa, tints grey molten). neo 26
	// renders fluids via FluidModel.Unbaked(still, flow, overlay, tintSource) on RegisterFluidModelsEvent (mod-bus).
	// Centralization 1:1 (one model factory for the whole mod, like GT6BlockModel/GT6ItemModel). Falls back to water on a null icon.
	private void onRegisterFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent aEvent) {
		net.minecraft.client.resources.model.sprite.Material tWaterStill = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still"));
		net.minecraft.client.resources.model.sprite.Material tWaterFlow  = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_flow"));
		int tCount = 0;
		for (gregapi.fluid.FluidGT tF : gregapi.fluid.FluidGT.BY_NAME.values()) {
			try {
				net.minecraft.resources.Identifier tTex = null;
				try { if (tF.mTexture != null) tTex = tF.mTexture.getIcon(0); } catch (Throwable e) {/* invalid icon → fallback to water */}
				net.minecraft.client.resources.model.sprite.Material tStill = tTex != null ? new net.minecraft.client.resources.model.sprite.Material(tTex) : tWaterStill;
				net.minecraft.client.resources.model.sprite.Material tFlow  = tTex != null ? tStill : tWaterFlow;
				short[] tRGBa = tF.getRGBa();
				int tTint = (tRGBa != null && tRGBa.length >= 3) ? (0xFF000000 | ((tRGBa[0]&0xFF)<<16) | ((tRGBa[1]&0xFF)<<8) | (tRGBa[2]&0xFF)) : 0xFFFFFFFF;
				net.minecraft.client.renderer.block.FluidModel.Unbaked tModel = new net.minecraft.client.renderer.block.FluidModel.Unbaked(tStill, tFlow, null, net.neoforged.neoforge.client.fluid.FluidTintSources.constant(tTint));
				net.minecraft.world.level.material.Fluid tSource  = tF.mSourceHolder.value();
				net.minecraft.world.level.material.Fluid tFlowing = tF.mFlowingHolder.isBound() ? tF.mFlowingHolder.value() : tSource;
				aEvent.register(tModel, tSource, tFlowing);
				tCount++;
			} catch (Throwable e) {/* one fluid's failure must not break the rest */}
		}
		gregapi.data.CS.OUT.println("[GT6] F3-render: FluidModel registered for " + tCount + " GT6 fluids.");
	}

	private void onRegisterBlockStateModels(net.neoforged.neoforge.client.event.RegisterBlockStateModels aEvent) {
		aEvent.registerModel(gregapi.render.GT6BlockModel.Unbaked.ID, gregapi.render.GT6BlockModel.Unbaked.MAP_CODEC);
	}

	// Runtime injection: assign the single GT6BlockModel to every BlockState of every GT6 renderer block
	// (the model is dynamic — reads the block/position/state in collectParts, one instance for the whole mod).
	private void onModifyBakingResult(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult aEvent) {
		// Edit #3 (BUG-106): the atlas/models were recreated — old sprites in geometry caches are dead, invalidating them.
		gregapi.render.GT6ItemModel.invalidateCaches();
		net.minecraft.client.resources.model.sprite.Material.Baked tParticle = new net.minecraft.client.resources.model.sprite.Material.Baked(
			// sprite-id WITHOUT a "blocks/" prefix: the atlas-source (assets/minecraft/atlases/blocks.json) maps textures/blocks/** with prefix:"" → gregtech:system/error (like GT6BlockModel:56). The former "blocks/system/error" was not found → "Failed to retrieve texture".
			aEvent.getTextureGetter().apply(net.minecraft.resources.Identifier.fromNamespaceAndPath("gregtech", "system/error")), false);
		java.util.Map<net.minecraft.world.level.block.state.BlockState, net.minecraft.client.renderer.block.dispatch.BlockStateModel> tMap = aEvent.getBakingResult().blockStateModels();
		int tCount = 0;
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			// GT6BlockModel — both IRenderedBlock and BlockBaseRail (rails: their own rail branch in collectParts, a flat quad by meta).
			if (!(tBlock instanceof gregapi.render.IRenderedBlock) && !(tBlock instanceof gregapi.block.misc.BlockBaseRail)) continue;
			// per-BLOCK instance (not shared): the model must know its owner for the engine's breaking path
			// (it calls collectParts with an AIR state — the crack shape is otherwise unknowable; GT6BlockModel.mOwner).
			gregapi.render.GT6BlockModel tModel = new gregapi.render.GT6BlockModel(tParticle, tBlock);
			for (net.minecraft.world.level.block.state.BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
				tMap.put(tState, tModel); tCount++;
			}
		}
		// F3-render: a SINGLE item model for ALL GT6 items (including block items: their item shape is drawn by GT6ItemModel via
		// buildInventoryQuads = renderInventoryBlock). Previously block items were skipped → they had no item model → purple.
		gregapi.render.GT6ItemModel tItemModel = new gregapi.render.GT6ItemModel();
		java.util.Map<net.minecraft.resources.Identifier, net.minecraft.client.renderer.item.ItemModel> tItemMap = aEvent.getBakingResult().itemStackModels();
		int tItemCount = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			net.minecraft.resources.Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null || !gregapi.data.CS.ModIDs.isGregNamespace(tKey.getNamespace())) continue;
			// inject the block item if its block is IRenderedBlock OR a rail (BlockBaseRail: GT6ItemModel draws it a flat
			// straight icon); other block items are left with the default block model.
			if (tItem instanceof net.minecraft.world.item.BlockItem tBI && !(tBI.getBlock() instanceof gregapi.render.IRenderedBlock) && !(tBI.getBlock() instanceof gregapi.block.misc.BlockBaseRail)) continue;
			tItemMap.put(tKey, tItemModel); tItemCount++;
		}
		// Hygiene ("Missing model for variant"): GT6 blocks with RenderShape.INVISIBLE (fluid blocks river/ocean/swamp — the block
		// itself is invisible 1:1 to vanilla LiquidBlock, water is drawn by the FluidState/F5 subsystem) have no baked model → ModelManager
		// logged a warning for EVERY one of their BlockState variants (48 of them). We plug in an empty model (the same GT6BlockModel: for a
		// non-IRenderedBlock collectParts returns empty) — the engine finds a model, the warning stops; the visual does not change (the block is still INVISIBLE).
		gregapi.render.GT6BlockModel tEmptyModel = new gregapi.render.GT6BlockModel(tParticle);
		int tEmptyCount = 0;
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			net.minecraft.resources.Identifier tBKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tBKey == null || !gregapi.data.CS.ModIDs.isGregNamespace(tBKey.getNamespace())) continue;
			for (net.minecraft.world.level.block.state.BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
				if (tMap.containsKey(tState) || tState.getRenderShape() != net.minecraft.world.level.block.RenderShape.INVISIBLE) continue;
				tMap.put(tState, tEmptyModel); tEmptyCount++;
			}
		}
		gregapi.data.CS.OUT.println("[GT6] F3-render: GT6BlockModel injected into " + tCount + " block-states, GT6ItemModel into " + tItemCount + " items, " + tEmptyCount + " invisible-block placeholders.");
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

	/** F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo callers): was {@code PlayerControllerMP.sendUseItem(player,world,stack)}
 	 *  with an explicit {@code ItemStack} (the method type was removed). Neo {@code MultiPlayerGameMode.useItem(Player,InteractionHand)}
 	 *  takes the item from the player's hand, not the explicit {@code aStack} — the semantics of "use EXACTLY this stack"
 	 *  is unreachable without it (an engine seam), so the main hand is used as the closest equivalent. */
	@Override
	public boolean sendUseItemPacket(Player aPlayer, Level aWorld, ItemStack aStack) {
		Minecraft.getInstance().gameMode.useItem(aPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
		return T;
	}

	// BUG-039 v4 (JPMS-mirror audit): the method WAS a dead orphan — the signature (FMLCommonSetupEvent) did not match the
	// base Abstract_Proxy.onProxyAfterPreInit(Abstract_Mod, FMLPreInitializationEvent), @Override was
	// commented out → Abstract_Mod:167 never called it; OptiFine detection and seasonal foliage were lost
	// silently. Signature fixed, the channel is alive. RenderingRegistry stubs (registerEntityRenderingHandler/
	// registerBlockHandler — F3-superseded by the GT6BlockModel pipeline, no-op by design) were REMOVED: their mirror class
	// cpw.* is JPMS-cut from the runtime, invoking them would throw NoClassDefFoundError (see decisions/F3-render.md §1,2.1,2.5).
	@Override
	public void onProxyAfterPreInit(Abstract_Mod aMod, gregapi.api.FMLPreInitializationEvent aEvent) {
		// Check if OptiFine is loaded in order to disable some GT Render Hooks to fix Glitches.
		// 1:1 bridge: 1.7.10 FMLClientHandler.hasOptifine() = detects Class.forName("Config") (FMLClientHandler:272-286,
		// reference) — the FML wrapper is gone, the detection itself is reproduced; + net.optifine.Config (the modern OF path).
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
 	 * 1.7.10 {@code TextureStitchEvent.Pre} carried Gregorius's safety net here: "a fluid without an icon or with a broken
 	 * icon gets the icon of its own block or of water" (original {@code GT_API_Proxy_Client:194-212}). In neo
 	 * there is no Pre event ({@code TextureAtlasStitchedEvent} only fires AFTER stitching), and mutating someone else's
 	 * fluid is not allowed — the fluid's model is registered by the owning mod. The safety net's function is NOT lost: it is carried
 	 * by three centralized arms. The "no icon" branch ({@code getIcon()==null}) — {@link gregapi.data.FL#stillIcon}
 	 * (set up in BUG-049: no own texture → {@code water_still}; consumers are displays and ALL tanks via
 	 * {@link gregapi.render.BlockTextureFluid}). The "broken icon" branch ({@code FluidsGT.BROKEN}) —
 	 * {@code BlockTextureFluid:105}: the sprite was not found in the atlas → {@code water_still}. World render —
 	 * {@link #onRegisterFluidModels}: falls back to water on a null/invalid icon (the baked F3 phase — exactly where
 	 * the earlier deferred-work marker instructed the fix to move). The original's exception lists are carried over 1:1:
 	 * {@code BROKEN} is empty, {@code BORKEN} carries one thaumcraft fluid (the mod is absent from the build) — no
 	 * applicable foreign cases exist, only vanilla and GT6 register fluids. The fallback's judge is {@code gt6itemmodelprobe}
 	 * (BUG-068): river/ocean/swamp — fluids WITHOUT their own texture — get {@code water_still}, 10/10.
 	 * The handler is left empty, 1:1 with the original's subscription point.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTextureStitchedPre(TextureAtlasStitchedEvent aEvent) {
		//
	}

	/** Client-side arm of the beacon-payment bridge (center — {@link GT_API_Proxy#wrapBeaconPaymentSlot}): the client builds
 	 *  its OWN {@code BeaconMenu} instance from the network packet, the server-side slot swap does not reach it — without
 	 *  this arm the client's click prediction would reject a stack that the server accepts. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onScreenOpening(net.neoforged.neoforge.client.event.ScreenEvent.Opening aEvent) {
		if (aEvent.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.BeaconScreen tScreen) GT_API_Proxy.wrapBeaconPaymentSlot(tScreen.getMenu());
	}

	/**
	 * 1.7.10 {@code net.minecraftforge.event.entity.player.ItemTooltipEvent}
 	 * held the tooltip as a {@code List<String>} directly in the field {@code toolTip}; the neo equivalent
	 * {@code net.neoforged.neoforge.event.entity.player.ItemTooltipEvent} (`neoforge-decompiled/.../ItemTooltipEvent.java:16-70`)
 	 * — getters, and the list is typed {@code List<Component>} (an engine seam, since text rendering is now
 	 * a {@code Component} tree, not a raw string). All the GT6 logic below (300+ lines) operates on STRINGS
 	 * (concatenating {@code LH.Chat.*} § codes, {@code replaceAll}, etc.) and is passed into
 	 * {@code ICover.addToolTips(List<String>,...)} (hundreds of implementations across the mod, {@code List<String>}
 	 * left UNTOUCHED — out of scope for this pass) — so as not to lose a single line of business logic (R8), the body works
 	 * on a LOCAL {@code List<String>} copy (taken from {@code Component.getString()} before, reassembled
 	 * via {@code Component.literal(...)} after — § codes inside a literal string still render
 	 * correctly through the engine, see {@code FormattedCharSequence}); the sync-back happens in {@code finally}, so it
 	 * fires on ANY exit (return/exception), just as the original mutated the list directly. The harvest line
 	 * is restored below: {@code Block.getHarvestTool/getHarvestLevel} were removed from neo by name, but the same
 	 * values are supplied by the center {@link WD} from the 1.7.10 block passport (oracle data, see {@code WD.vanillaPassport}).
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
				aToolTip.add(0, LH.Chat.BLINKING_RED+LH.tt("A Recipe used an OreDict Item as Output directly, without copying it before!"));
				aToolTip.add(1, LH.Chat.BLINKING_RED+LH.tt("This is a typical CallByReference/CallByValue Error of the Modder doing it."));
				aToolTip.add(2, LH.Chat.BLINKING_RED+LH.tt("Please check all Recipes outputting this Item, and report the Recipes to their Owner."));
				aToolTip.add(3, LH.Chat.BLINKING_RED+LH.tt("The Owner of the RECIPE, NOT the Owner of the Item!"));
				return;
			}

			String aRegName = ST.regName(aEvent.getItemStack());
			if (aRegName == null) {
				aToolTip.set(0, LH.Chat.BLINKING_RED+LH.tt("ERROR: THIS ITEM HAS NOT BEEN REGISTERED!!!"));
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

			if (MD.Mek.owns(aRegName)) aToolTip.set(0, aToolTip.get(0).replaceAll("Osmium", MT.Ge.getLocal()));
			if (MD.BP .owns(aRegName)) aToolTip.set(0, aToolTip.get(0).replaceAll("Infused Teslatite", MT.PurpleAlloy.getLocal()).replaceAll("Teslatite", MT.Nikolite.getLocal()));
			if (MD.BP.mLoaded) aToolTip.set(0, aToolTip.get(0).replaceAll("Teslatite", MT.Nikolite.getLocal()));

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
				aToolTip.add(LH.Chat.BLINKING_RED + LH.tt("Recipe has been removed in favour of the GregTech Ender Garbage Bin"));
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
					// 1:1 with the original (`gregtech6/.../GT_API_Proxy_Client.java:301`): the same three values, only
					// asked from the center WD — in neo the block has neither a material nor harvestTool/Level by name.
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

			/* Was {@code Item.isBeaconPayment(ItemStack)} (Forge 1.7.10, method removed) — the central predicate of the bridge
	 		 * (tag {@code BEACON_PAYMENT_ITEMS} OR the IItemBeaconPayment contract): the tooltip answers the same as the beacon slot. */
			if (GT_API_Proxy.isBeaconPayment(aEvent.getItemStack())) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_BEACON_PAYMENT));
			}

			/* F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo callers): was {@code cpw.mods.fml.common.registry.GameRegistry.getFuelValue(ItemStack)}
	 		 * (Forge 1.7.10 static API, removed) — neo {@code Level.fuelValues().burnDuration(ItemStack)}
	 		 * (`neo-decompiled/net/minecraft/world/level/block/entity/FuelValues.java:38`), instance from the client's
	 		 * {@code Minecraft.getInstance().level} (the closest client-side equivalent of a world context). */
			Level tClientLevel = Minecraft.getInstance().level;
			long tBurnValue = tClientLevel == null ? 0 : tClientLevel.fuelValues().burnDuration(ST.amount(1, aEvent.getItemStack()));
			if (tBurnValue > 0) aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FURNACE_FUEL) + LH.Chat.WHITE + tBurnValue + " ("+(tBurnValue*EU_PER_FURNACE_TICK)+LH.Chat._RED+LH.tt("HU")+LH.Chat.WHITE+")");

			if (tData != null) {
				if (tData.validPrefix()) {
					for (IOreDictListenerItem tListener : tData.mPrefix.mListenersItem) {
						String tToolTip = tListener.getListenerToolTip(tData.mPrefix, tData.mMaterial.mMaterial, aEvent.getItemStack());
						if (tToolTip != null) aToolTip.add(tToolTip);
					}
				} else {
					if (IL.RC_Firestone_Refined.equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + LH.tt("GT6 Burning Boxes: ")+LH.Chat.WHITE+(800*EU_PER_LAVA)+LH.Chat._RED+LH.tt("HU")+LH.Chat._CYAN+LH.tt("per Lava Block")); else
					if (IL.RC_Firestone_Cracked.equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + LH.tt("GT6 Burning Boxes: ")+LH.Chat.WHITE+(600*EU_PER_LAVA)+LH.Chat._RED+LH.tt("HU")+LH.Chat._CYAN+LH.tt("per Lava Block")); else
					if (IL.TF_Pick_Giant       .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + LH.tt("Repairable with Knightmetal Ingots on the Vanilla Anvil")); else
					if (IL.TF_Sword_Giant      .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + LH.tt("Repairable with Ironwood Ingots on the Vanilla Anvil")); else
					if (IL.TF_Lamp_of_Cinders  .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + LH.tt("Can be used as a Lighter for GT6 things and TNT"));
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
						aToolTip.set(0, aToolTip.get(0).replaceAll("(Teslatite|Electrotine)", MT.Nikolite.getLocal()));
					}
					if (tData.mMaterial.mMaterial == MT.Ge) {
						aToolTip.set(0, aToolTip.get(0).replaceAll("Osmium", MT.Ge.getLocal()));
					}
					if (tData.validPrefix()) {
						if (!ST.isGT(aItem) && tData.mPrefix == OP.dustTiny && ANY.Blaze.mToThis.contains(tData.mMaterial.mMaterial)) {
							// The search side keeps the English name: that is what the foreign mod wrote. The replacement asks
							// for the localised item name, so on a Russian client the line does not switch back to English.
							aToolTip.set(0, aToolTip.get(0).replaceAll(tData.mMaterial.mMaterial.mNameLocal,
								LH.get("oredict." + OP.dustTiny.dat(tData.mMaterial.mMaterial).toString(), OP.dustTiny.mMaterialPre + tData.mMaterial.mMaterial.mNameLocal)));
						}
						if (tData.mPrefix.contains(TD.Prefix.NEEDS_SHARPENING)) aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_NEEDS_SHARPENING));
						if (tData.mPrefix.contains(TD.Prefix.NEEDS_HANDLE    )) aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_NEEDS_HANDLE) + LH.Chat.WHITE + tData.mMaterial.mMaterial.mHandleMaterial.getLocal());

						if (!tData.mMaterial.mMaterial.mSourceOf.isEmpty() && tData.mPrefix.containsAny(TD.Prefix.ORE,TD.Prefix.ORE_PROCESSING_DIRTY)) {
							StringBuilder
							tToolTip = null;
							for (OreDictMaterial tMaterial : tData.mMaterial.mMaterial.mSourceOf) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.CYAN).append(LH.tt("Source of: ")).append(LH.Chat.WHITE); else tToolTip.append(", ");
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
								if (tEnchantment.mObject == Enchantments.FIRE_ASPECT && tEnchantment.mAmount >= 3) tToolTip.append(LH.tt(" (Autosmelt)"));
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
					aToolTip.add(LH.Chat.DGRAY + LH.tt("Enable F3+H Mode for Info about contained Materials."));
				}

				if (tData.validData()) {
					if (ST.isGT(aItem)) {
						if (tData.mMaterial.mMaterial.mOriginalMod == null) {
							aToolTip.add(LH.Chat.BLUE + LH.tt("Material from an Unknown Mod"));
						} else if (tData.mMaterial.mMaterial.mOriginalMod == MD.MC) {
							aToolTip.add(LH.Chat.BLUE + LH.tt("Vanilla Material"));
						} else if (tData.mMaterial.mMaterial.mOriginalMod == MD.GAPI) {
							if (tData.mMaterial.mMaterial.mID > 0 && tData.mMaterial.mMaterial.mID < 8000) {
								aToolTip.add(LH.Chat.BLUE + LH.tt("Material from the Periodic Table of Elements"));
							} else {
								aToolTip.add(LH.Chat.BLUE + LH.tt("Random Material handled by Greg API"));
							}
						} else {
							aToolTip.add(LH.Chat.BLUE + LH.tt("Material from ") + tData.mMaterial.mMaterial.mOriginalMod.mName);
						}
					} else {
						if ((tData.mMaterial.mMaterial == MT.Fe || tData.mMaterial.mMaterial == MT.Fe2O3) && tData.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED) && !aToolTip.get(0).contains("Native")) {
							aToolTip.set(0, aToolTip.get(0).replaceAll("Banded Iron", MT.Fe2O3.getLocal()).replaceAll("Iron", MT.Fe2O3.getLocal()));
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
			// F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo callers): sync the local List<String> back into
			// the event's List<Component> (see method's class javadoc) — an engine-forced seam.
			tTT.clear();
			for (String s : aToolTip) tTT.add(s == null ? null : Component.literal(s));
		}
	}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo callers): was {@code cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent}
 	 *  with a {@code phase} field/compared to {@code == ServerTickEvent.END} (type+field removed, the F10 mirror
 	 *  `compat-mirror/java/cpw/mods/fml/common/gameevent/TickEvent.java` is a dummy type, the behavior lives
 	 *  here) — neo sends {@code ClientTickEvent.Pre}/{@code .Post} separately
 	 *  (`neoforge-decompiled/net/neoforged/neoforge/client/event/ClientTickEvent.java:24-38`);
 	 *  {@code .Post} = "after the tick" is 1:1 equal to the old {@code END} phase — the signature was retyped,
 	 *  the wrapping condition dropped (already implied by the event type), the body is UNCHANGED. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onClientTickEvent(ClientTickEvent.Post aEvent) {
		{
			if (CLIENT_TIME == 10) {
				// CLOSED, the body moved to the server side — {@code gregtech.GT6_Main.onModServerStarted2}:
				// "Fake Furnace Recipe Map" (populating the RM.Furnace showcase) and importing vanilla smelts into the
				// GT6 registry ({@code FurnaceRecipes.importVanilla}). In 1.7.10 this was done here because the vanilla
				// smelting list was static and already existed by the client tick; in neo recipes are data-driven and
				// arrive with the datapack, so the moment is the same (world loaded), but the side is server-side.
				//
				// The second half of the 1.7.10 body — "hiding stuff from NEI" ({@code Item.getSubItems} +
				// {@code CreativeTabs.tabAllSearch}) — CARRIES NO DEBT: the original (:530-536) hid microblocks
				// of FOREIGN mods (Forge Microblocks, Extra Utilities, Extra Simple, AE2 facades). None of them are
				// in the build, {@code ST.item(MD.FMB, "microblock")} = null, the loop would be empty.
			}

			// BUG-090: client-side arm of the slippery-slide effect. In 1.7.10 Potion.performEffect ticked on BOTH
			// sides (IEPotions.java:118-121 also moved the client player); in neo applyEffectTick is server-only
			// (ServerLevel signature), and player movement is client-authoritative — the server-side speed boost never
			// reaches the client. The effect is synced to the client normally (ClientboundUpdateMobEffectPacket) —
			// we read it here using the same vector and coefficient; the item drop remains server-side.
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
	
	/** F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo callers): see the javadoc of {@link gregapi.tileentity.render.ITileEntityOnDrawBlockHighlight}
 	 *  — {@link ExtractBlockOutlineRenderStateEvent} does not carry the old event's {@code player}/{@code currentItem}/{@code partialTicks}.
 	 *  The player is recovered via {@code Minecraft.getInstance().player} (the same central
 	 *  pattern as {@link #getThePlayer()}); {@code sideHit} — from {@code getHitResult().getDirection()};
 	 *  {@code partialTicks} is unreachable (0 — neutral, the sole consumer {@link RenderHelper#drawWrenchOverlay}
 	 *  is already a no-op, see its javadoc). Branching/delegation into {@link ITileEntityOnDrawBlockHighlight} — UNCHANGED. */
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
			// AE2 arm (ToolCompat:404): Greg's wrench also serves AE2 blocks — the placement targeting grid must
			// draw for them too, under the same condition as the arm itself. Live acceptance 2026-08-14: rotation
			// worked, but the grid was missing — the hint lagged behind the mechanic.
			if ((ROTATABLE_VANILLA_BLOCKS.contains(aBlock) || (ToolCompat.IC_WRENCHABLE && aTileEntity instanceof ic2.api.tile.IWrenchable) || (ToolCompat.AE_BASEBLOCKENTITY && aTileEntity instanceof appeng.blockentity.AEBaseBlockEntity)) && ST.valid(tPlayer.getMainHandItem()) && ToolsGT.contains(TOOL_wrench, tPlayer.getMainHandItem())) {
				RenderHelper.drawWrenchOverlay(aEvent, (byte)0, tSide);
				return;
			}
		}
	}
	
	private static List<Block> ROTATABLE_VANILLA_BLOCKS = Arrays.asList(Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.FURNACE, Blocks.FURNACE, Blocks.DROPPER, Blocks.DISPENSER, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.HOPPER, Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN);
	// ⚠ Same carrier as in ToolCompat: the directional pumpkin in 1.7.10 = CARVED_PUMPKIN, while Blocks.PUMPKIN is
	// the uncarved one and does NOT rotate at all (neo PumpkinBlock:24 extends Block, no FACING property).
	// Blocks.PUMPKIN used to be here, which is why the wrench hint on the pumpkin never showed up.
}
