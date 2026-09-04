/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregtech;

import gregapi.api.FMLPreInitializationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import gregapi.GT_API;
import gregapi.api.Abstract_Mod;
import gregapi.config.ConfigCategories;
import gregapi.data.LH;
import gregapi.data.MD;
import gregtech.entities.EntitiesGT;
import gregtech.entities.projectiles.EntityArrow_Material;
import gregtech.entities.projectiles.EntityArrow_Potion;
import gregtech.render.GT_Renderer_Entity_Arrow;
import gregtech.render.PlayerModelRenderer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;

import static gregapi.data.CS.*;

public class GT_Client extends GT_Proxy {
	private final PlayerModelRenderer mPlayerRenderer = new PlayerModelRenderer(mSupporterListSilver, mSupporterListGold);
	
	// BUG-039 v4 (JPMS-mirror audit): RenderingRegistry.addNewArmourRendererPrefix — the 1.7.10 armor-layer API was removed
	// (neo: equipment assets, ItemArmorBase/F13); the cpw.* mirror is JPMS-cut from the runtime (a call without catch = a
	// NoClassDefFoundError crash). 0 callers (grep) — the index is not consumed by the neo renderer.
	public int addArmor(String aPrefix) {return 0;}
	
	public GT_Client() {super();}
	
	/* F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo calls): was {@code FMLPreInitializationEvent} from the old FML —
	 * the type matches the central F12 adapter {@code gregapi.api.FMLPreInitializationEvent}
	 * (see {@code Abstract_Proxy#onProxyAfterPreInit}), the signature was retyped for a real {@code @Override}. */
	@Override
	public void onProxyAfterPreInit(Abstract_Mod aMod, FMLPreInitializationEvent aEvent) {
		super.onProxyAfterPreInit(aMod, aEvent);
		// F12-entity: arrow renderers are registered via EntityRenderersEvent (see registerClientRenderers),
		// not by a direct new here — PreInit has no EntityRendererProvider.Context (this used to crash super(null)).
	}

	// F12-entity: mod-bus registration of client entity renderers (replaces the removed 1.7.10
	// RenderingRegistry.registerEntityRenderingHandler). Called from the GT6_Main constructor only on the client
	// (the GT_Proxy#registerClientRenderers base is a no-op, the server never loads client classes). EntityRenderersEvent is
	// an IModBusEvent, so we hook an explicit addListener onto the mod bus (Abstract_Proxy.registerSubscribeEvents skips them).
	@Override
	public void registerClientRenderers(IEventBus aModBus) {
		aModBus.addListener(this::onRegisterEntityRenderers);
	}

	private void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers aEvent) {
		aEvent.registerEntityRenderer(EntitiesGT.ARROW_MATERIAL.get(), aContext -> new GT_Renderer_Entity_Arrow(aContext, "arrow"));
		aEvent.registerEntityRenderer(EntitiesGT.ARROW_POTION.get()  , aContext -> new GT_Renderer_Entity_Arrow(aContext, "arrow_potions"));
	}
	
	private boolean FIRST_CLIENT_PLAYER_TICK = T;
	
	/**
	 * Branch 1.20.1: the event's form is back to the original ({@code TickEvent.java:24-29,110-118} — the fields
	 * player/phase/side are present, phase END as it was). Was {@code cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent}
	 * with public fields {@code player}/{@code phase}/{@code side} — neo {@code PlayerTickEvent.Post}
	 * (`neoforge-decompiled/net/neoforged/neoforge/event/tick/PlayerTickEvent.java:38-46`, "after the tick" = the old
	 * {@code END}) with a {@code getEntity()} getter; the side filter is {@code getEntity().level().isClientSide()}
	 * (the event is sent on both sides, see the class javadoc). {@code Component} is now a tree, not {@code new
	 * Component(String)} (an interface, abstract) — {@code Component.literal(...)}, similar to the F3 fix
	 * {@code GT_API_Proxy_Client#onItemTooltip}. {@code addChatComponentMessage}→{@code sendSystemMessage}
	 * (`neo-decompiled/net/minecraft/world/entity/player/Player.java:1399`). {@code ClickEvent} is now
	 * a sealed interface with per-action records ({@code ClickEvent.OpenFile(String)}/{@code .OpenUrl(URI)},
	 * `neo-decompiled/net/minecraft/network/chat/ClickEvent.java:103-135`), the style comes from {@code MutableComponent.withStyle}.
	 * (previously there was a degradation here due to the unresolved "F12, config-subsystem" — {@code ConfigsGT.CLIENT.mConfig.
	 * getConfigFile()} was unreachable, because {@code gregapi.config.Config} used the declarative neo
	 * {@code net.neoforged.neoforge.common.ModConfigSpec} with no {@code File} constructor/{@code .load()}/
	 * {@code .save()}; CLOSED by the same checkpoint as this ledger mark — {@code gregapi.config.Config}
	 * now uses its own {@code gregapi.config.ModConfigSpec} (dynamic, file-based, reproducing
	 * 1.7.10 Forge Configuration/Property), {@code getConfigFile()} is real — the clickable "open
	 * file" link (was {@code ClickEvent.Action.OPEN_FILE}) is restored as {@code new ClickEvent.OpenFile(String)}).
	 */
	@SubscribeEvent
	public void onPlayerTickEventClient(TickEvent.PlayerTickEvent aEvent) {
		Player tPlayer = aEvent.player;
		if (!tPlayer.isDeadOrDying() && aEvent.phase == TickEvent.Phase.END && aEvent.side.isClient() && CLIENT_TIME > 20) {
			if (tPlayer == GT_API.api_proxy.getThePlayer()) {
				if (FIRST_CLIENT_PLAYER_TICK) {
					FIRST_CLIENT_PLAYER_TICK = F;
					MutableComponent tLink;
					if (!mMessage.isEmpty() && ConfigsGT.CLIENT.get(ConfigCategories.news, mMessage, T)) {
						tPlayer.sendSystemMessage(Component.literal(mMessage));
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.DGRAY + ""));
						tLink = Component.literal(LH.Chat.DGRAY + LH.tt("disable message in the clientside gregtech.cfg"));
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, ConfigsGT.CLIENT.mConfig.getConfigFile().getAbsolutePath())));
						tPlayer.sendSystemMessage(tLink);
					}
					if (mVersionOutdated) {
						tPlayer.sendSystemMessage(Component.literal(LH.tt("Major GT6 Update released, for details visit")));
						tLink = Component.literal(LH.Chat.BLUE + LH.tt("https://gregtech.mechaenetia.com/1.7.10"));
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://gregtech.mechaenetia.com/1.7.10")));
						tPlayer.sendSystemMessage(tLink);
						tLink = Component.literal(LH.Chat.DGRAY + LH.tt("disable checker in the clientside gregtech.cfg"));
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, ConfigsGT.CLIENT.mConfig.getConfigFile().getAbsolutePath())));
						tPlayer.sendSystemMessage(tLink);
					}
					if (MD.IC2.mLoaded && !MD.IC2C.mLoaded) {
						try {
							int tVersion = Integer.parseInt(((String)Class.forName("ic2.core.IC2").getField("VERSION").get(null)).substring(4, 7));
							if (tVersion < 827) {
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.RED + LH.tt("Please update IndustrialCraft!")));
								// IC2 Site doesn't support https.
								tLink = Component.literal(LH.Chat.BLUE + LH.tt("http://ic2api.player.to:8080/job/IC2_experimental/827/"));
								tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://ic2api.player.to:8080/job/IC2_experimental/827/")));
								tPlayer.sendSystemMessage(tLink);
							}
						} catch(Throwable e) {/**/}
					}
					if (MD.TC.mLoaded) {
						try {
							if (Class.forName("com.chocohead.patcher.ThaumicFixer") != null) {
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.RED + LH.tt("Warning! Chocoheads ThaumicFixer needs to be uninstalled!")));
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + LH.tt("Not uninstalling it can lead to crashes when viewing Aspects.")));
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + LH.tt("Lag is already fixed with a better Version of the ASM Code,")));
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + LH.tt("that doesn't obliterate the Thaumcraft API for no reason.")));
							}
						} catch(Throwable e) {/**/}
					}
					if (MD.COG.mLoaded && !MD.PFAA.mLoaded && ConfigsGT.CLIENT.get(ConfigCategories.general, "warnings_customoregen", T)) {
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.RED + LH.tt("Warning! CustomOreGen will screw up all GregTech Worldgen with its Default Configs!")));
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + LH.tt("If you don't even use CustomOreGen, I would highly recommend you to remove it.")));
						tLink = Component.literal(LH.Chat.DGRAY + LH.tt("disable warning in the clientside gregtech.cfg"));
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, ConfigsGT.CLIENT.mConfig.getConfigFile().getAbsolutePath())));
						tPlayer.sendSystemMessage(tLink);
					}
					if (WOODMANS_BDAY) {
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.WHITE+"<"+LH.Chat.GREEN+">:]"+LH.Chat.WHITE+LH.tt("> Have a nice day!")));
					}
					if (APRIL_FOOLS) {
						tPlayer.sendSystemMessage(Component.literal(CHAT_GREG + LH.tt("Watch your Calendar!")));
					}
				}
			}
		}
	}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo calls): was {@code new ResourceLocation(String)} (a one-argument
	 *  constructor) — the {@code ResourceLocation} constructor is {@code private} in 26.1.2, the public factory for the
	 *  vanilla namespace is {@code new ResourceLocation("minecraft", path)}
	 *  (`neo-decompiled/net/minecraft/resources/ResourceLocation.java:49`). */
	private ResourceLocation WATER_OVERLAY = new ResourceLocation("minecraft", "textures/misc/underwater.png");

	/**
	 * F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo calls): was {@code net.minecraftforge.client.event.RenderBlockOverlayEvent}
	 * (immediate-mode: a {@code Tessellator}/GL11 swamp-haze quad) — replaced with
	 * {@code RenderBlockScreenEffectEvent} (`neoforge-decompiled/net/neoforged/neoforge/client/event/
	 * RenderBlockScreenEffectEvent.java:29-116`, {@code getBlockState()} instead of the old {@code blockForOverlay}).
	 * The actual redraw uses {@code MultiBufferSource}/{@code PoseStack} from the event (decisions/F3-render.md §1);
	 * the quad body is a no-op stub, {@code setCanceled(T)} (a structurally meaningful effect — it suppresses the vanilla
	 * water overlay in this case) is kept.
	 */
	@SubscribeEvent
	public void receiveRenderEvent(RenderBlockScreenEffectEvent aEvent) {
		if (aEvent.getBlockState().getBlock() == BlocksGT.Swamp) {
			// Branch 1.20.1: drawing is immediate-mode again, as in 1.7.10. The quad is verbatim from the original
			// (gt6-original GT_Client.java:131-149); the call's canon is ScreenEffectRenderer.renderFluid (ScreenEffectRenderer.java:104-128):
			// the same position_tex shader, the same format, the same vertex order. The color (0, brightness/2, 0, 0.75) goes
			// through RenderSystem.setShaderColor — the same technique the engine itself uses to set it (same place, :111), instead of GL11.
			try {
				net.minecraft.world.entity.player.Player tPlayer = GT_API.api_proxy.getThePlayer();
				if (tPlayer != null) {
					float tBright = tPlayer.getLightLevelDependentMagicValue(); // was getBrightness(partialTicks)
					float tUo = -tPlayer.getYRot() / 64F, tVo = tPlayer.getXRot() / 64F;
					org.joml.Matrix4f tPose = aEvent.getPoseStack().last().pose();
					com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
					com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, WATER_OVERLAY);
					com.mojang.blaze3d.systems.RenderSystem.enableBlend();
					com.mojang.blaze3d.systems.RenderSystem.setShaderColor(0F, tBright / 2F, 0F, 0.75F);
					com.mojang.blaze3d.vertex.BufferBuilder tBuf = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
					tBuf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
					tBuf.vertex(tPose, -1, -1, -0.5F).uv(4 + tUo, 4 + tVo).endVertex();
					tBuf.vertex(tPose,  1, -1, -0.5F).uv(    tUo, 4 + tVo).endVertex();
					tBuf.vertex(tPose,  1,  1, -0.5F).uv(    tUo,     tVo).endVertex();
					tBuf.vertex(tPose, -1,  1, -0.5F).uv(4 + tUo,     tVo).endVertex();
					com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(tBuf.end());
					com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
					com.mojang.blaze3d.systems.RenderSystem.disableBlend();
				}
			} catch (Throwable e) {e.printStackTrace(ERR);}
			aEvent.setCanceled(T);
		}
	}

	@SubscribeEvent
	public void receiveRenderEvent(RenderPlayerEvent.Pre aEvent) {
//      if (UT.Entities.getFullInvisibility(aEvent.entityPlayer)) {aEvent.setCanceled(true); return;}
	}

	@SubscribeEvent
	public void receiveRenderSpecialsEvent(RenderPlayerEvent.Pre aEvent) {
		mPlayerRenderer.receiveRenderSpecialsEvent(aEvent);
	}
	/*
	@Override
	public void doSonictronSound(ItemStack aStack, World aWorld, double aX, double aY, double aZ) {
		if (UT.Stacks.invalid(aStack)) return;
		String tString = "note.harp";
		for (int i = 0, j = mSoundItems.size(); i < j; i++) if (UT.Stacks.equal(mSoundItems.get(i), aStack)) {tString = mSoundNames.get(i); break;}
		if (tString.startsWith("random.explode")) if (aStack.stackSize==3) tString = "random.fuse"; else if (aStack.stackSize==2) tString = "random.old_explode";
		if (tString.startsWith("streaming.")) {
			switch (aStack.stackSize) {
			case  1: tString += "13"; break;
			case  2: tString += "cat"; break;
			case  3: tString += "blocks"; break;
			case  4: tString += "chirp"; break;
			case  5: tString += "far"; break;
			case  6: tString += "mall"; break;
			case  7: tString += "mellohi"; break;
			case  8: tString += "stal"; break;
			case  9: tString += "strad"; break;
			case 10: tString += "ward"; break;
			case 11: tString += "11"; break;
			case 12: tString += "wait"; break;
			default: tString += "wherearewenow"; break;
			}
		}
		if (tString.startsWith("streaming.")) aWorld.playRecord(tString.substring(10, tString.length()), (int)aX, (int)aY, (int)aZ); else aWorld.playSound(aX, aY, aZ, tString, 3.0F, tString.startsWith("note.")?(float)Math.pow(2.0D, (aStack.stackSize - 13) / 12.0D):1.0F, false);
	}*/
	
}
