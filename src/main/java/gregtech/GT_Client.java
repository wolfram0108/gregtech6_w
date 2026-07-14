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
 */

package gregtech;

import cpw.mods.fml.client.registry.RenderingRegistry;
import gregapi.api.FMLPreInitializationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import gregapi.GT_API;
import gregapi.api.Abstract_Mod;
import gregapi.config.ConfigCategories;
import gregapi.data.LH;
import gregapi.data.MD;
import gregtech.entities.projectiles.EntityArrow_Material;
import gregtech.entities.projectiles.EntityArrow_Potion;
import gregtech.render.GT_Renderer_Entity_Arrow;
import gregtech.render.PlayerModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.net.URI;

import static gregapi.data.CS.*;

public class GT_Client extends GT_Proxy {
	private final PlayerModelRenderer mPlayerRenderer = new PlayerModelRenderer(mSupporterListSilver, mSupporterListGold);
	
	public int addArmor(String aPrefix) {return RenderingRegistry.addNewArmourRendererPrefix(aPrefix);}
	
	public GT_Client() {super();}
	
	/* PORT-TODO(F3, baked-рендер клиента): было {@code FMLPreInitializationEvent} из старого FML —
	 * тип совпадает с центральным F12-переходником {@code gregapi.api.FMLPreInitializationEvent}
	 * (см. {@code Abstract_Proxy#onProxyAfterPreInit}), сигнатура ретипирована для реального {@code @Override}. */
	@Override
	public void onProxyAfterPreInit(Abstract_Mod aMod, FMLPreInitializationEvent aEvent) {
		super.onProxyAfterPreInit(aMod, aEvent);
		new GT_Renderer_Entity_Arrow(EntityArrow_Material.class, "arrow");
		new GT_Renderer_Entity_Arrow(EntityArrow_Potion.class, "arrow_potions");
	}
	
	private boolean FIRST_CLIENT_PLAYER_TICK = T;
	
	/**
	 * PORT-TODO(F3, baked-рендер клиента, частично): было {@code cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent}
	 * с публичными полями {@code player}/{@code phase}/{@code side} — neo {@code PlayerTickEvent.Post}
	 * (`neoforge-decompiled/net/neoforged/neoforge/event/tick/PlayerTickEvent.java:38-46`, "после тика" = старый
	 * {@code END}) с геттером {@code getEntity()}; фильтр стороны — {@code getEntity().level().isClientSide()}
	 * (событие шлётся на обеих сторонах, см. javadoc класса). {@code Component} — теперь дерево, не {@code new
	 * Component(String)} (интерфейс, абстрактный) — {@code Component.literal(...)}, аналогично F3-правке
	 * {@code GT_API_Proxy_Client#onItemTooltip}. {@code addChatComponentMessage}→{@code sendSystemMessage}
	 * (`neo-decompiled/net/minecraft/world/entity/player/Player.java:1399`). {@code ClickEvent} — теперь
	 * sealed-интерфейс с записями по действию ({@code ClickEvent.OpenFile(String)}/{@code .OpenUrl(URI)},
	 * `neo-decompiled/net/minecraft/network/chat/ClickEvent.java:103-135`), стиль — {@code MutableComponent.withStyle}.
	 * (ранее здесь была деградация из-за незакрытого "F12, config-subsystem" — {@code ConfigsGT.CLIENT.mConfig.
	 * getConfigFile()} был недостижим, т.к. {@code gregapi.config.Config} использовал декларативный neo
	 * {@code net.neoforged.neoforge.common.ModConfigSpec} без {@code File}-конструктора/{@code .load()}/
	 * {@code .save()}; ЗАКРЫТО тем же чекпоинтом, что и эта ledger-метка — {@code gregapi.config.Config}
	 * теперь использует свой {@code gregapi.config.ModConfigSpec} (динамический, файловый, воспроизводящий
	 * 1.7.10 Forge Configuration/Property), {@code getConfigFile()} реален — кликабельная ссылка "открыть
	 * файл" (было {@code ClickEvent.Action.OPEN_FILE}) восстановлена как {@code new ClickEvent.OpenFile(String)}).
	 */
	@SubscribeEvent
	public void onPlayerTickEventClient(PlayerTickEvent.Post aEvent) {
		Player tPlayer = aEvent.getEntity();
		if (!tPlayer.isDeadOrDying() && tPlayer.level().isClientSide() && CLIENT_TIME > 20) {
			if (tPlayer == GT_API.api_proxy.getThePlayer()) {
				if (FIRST_CLIENT_PLAYER_TICK) {
					FIRST_CLIENT_PLAYER_TICK = F;
					MutableComponent tLink;
					if (!mMessage.isEmpty() && ConfigsGT.CLIENT.get(ConfigCategories.news, mMessage, T)) {
						tPlayer.sendSystemMessage(Component.literal(mMessage));
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.DGRAY + ""));
						tLink = Component.literal(LH.Chat.DGRAY + "disable message in the clientside gregtech.cfg");
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(ConfigsGT.CLIENT.mConfig.getConfigFile().getAbsolutePath())));
						tPlayer.sendSystemMessage(tLink);
					}
					if (mVersionOutdated) {
						tPlayer.sendSystemMessage(Component.literal("Major GT6 Update released, for details visit"));
						tLink = Component.literal(LH.Chat.BLUE + "https://gregtech.mechaenetia.com/1.7.10");
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://gregtech.mechaenetia.com/1.7.10"))));
						tPlayer.sendSystemMessage(tLink);
						tLink = Component.literal(LH.Chat.DGRAY + "disable checker in the clientside gregtech.cfg");
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(ConfigsGT.CLIENT.mConfig.getConfigFile().getAbsolutePath())));
						tPlayer.sendSystemMessage(tLink);
					}
					if (MD.IC2.mLoaded && !MD.IC2C.mLoaded) {
						try {
							int tVersion = Integer.parseInt(((String)Class.forName("ic2.core.IC2").getField("VERSION").get(null)).substring(4, 7));
							if (tVersion < 827) {
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.RED + "Please update IndustrialCraft!"));
								// IC2 Site doesn't support https.
								tLink = Component.literal(LH.Chat.BLUE + "http://ic2api.player.to:8080/job/IC2_experimental/827/");
								tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent.OpenUrl(URI.create("http://ic2api.player.to:8080/job/IC2_experimental/827/"))));
								tPlayer.sendSystemMessage(tLink);
							}
						} catch(Throwable e) {/**/}
					}
					if (MD.TC.mLoaded) {
						try {
							if (Class.forName("com.chocohead.patcher.ThaumicFixer") != null) {
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.RED + "Warning! Chocoheads ThaumicFixer needs to be uninstalled!"));
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + "Not uninstalling it can lead to crashes when viewing Aspects."));
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + "Lag is already fixed with a better Version of the ASM Code,"));
								tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + "that doesn't obliterate the Thaumcraft API for no reason."));
							}
						} catch(Throwable e) {/**/}
					}
					if (MD.COG.mLoaded && !MD.PFAA.mLoaded && ConfigsGT.CLIENT.get(ConfigCategories.general, "warnings_customoregen", T)) {
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.RED + "Warning! CustomOreGen will screw up all GregTech Worldgen with its Default Configs!"));
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.ORANGE + "If you don't even use CustomOreGen, I would highly recommend you to remove it."));
						tLink = Component.literal(LH.Chat.DGRAY + "disable warning in the clientside gregtech.cfg");
						tLink = tLink.withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(ConfigsGT.CLIENT.mConfig.getConfigFile().getAbsolutePath())));
						tPlayer.sendSystemMessage(tLink);
					}
					if (WOODMANS_BDAY) {
						tPlayer.sendSystemMessage(Component.literal(LH.Chat.WHITE+"<"+LH.Chat.GREEN+">:]"+LH.Chat.WHITE+"> Have a nice day!"));
					}
					if (APRIL_FOOLS) {
						tPlayer.sendSystemMessage(Component.literal(CHAT_GREG + "Watch your Calendar!"));
					}
				}
			}
		}
	}
	
	/** PORT-TODO(F3, baked-рендер клиента): было {@code new ResourceLocation(String)} (одноаргументный
	 *  конструктор) — {@code Identifier} конструктор {@code private} в 26.1.2, публичная фабрика для
	 *  ванильного namespace — {@code Identifier.withDefaultNamespace(path)}
	 *  (`neo-decompiled/net/minecraft/resources/Identifier.java:49`). */
	private Identifier WATER_OVERLAY = Identifier.withDefaultNamespace("textures/misc/underwater.png");

	/**
	 * PORT-TODO(F3, baked-рендер клиента): было {@code net.minecraftforge.client.event.RenderBlockOverlayEvent}
	 * (immediate-mode: {@code Tessellator}/GL11 квад болотной пелены) — заменён на
	 * {@code RenderBlockScreenEffectEvent} (`neoforge-decompiled/net/neoforged/neoforge/client/event/
	 * RenderBlockScreenEffectEvent.java:29-116`, {@code getBlockState()} вместо старого {@code blockForOverlay}).
	 * Реальная перерисовка — {@code MultiBufferSource}/{@code PoseStack} из события (decisions/F3-render.md §1);
	 * тело квада — no-op заглушка, {@code setCanceled(T)} (структурно значимый эффект — подавляет ванильный
	 * оверлей воды в этом случае) сохранён.
	 */
	@SubscribeEvent
	public void receiveRenderEvent(RenderBlockScreenEffectEvent aEvent) {
		if (aEvent.getBlockState().getBlock() == BlocksGT.Swamp) {
			aEvent.setCanceled(T);
		}
	}

	@SubscribeEvent
	public void receiveRenderEvent(RenderPlayerEvent.Pre<?> aEvent) {
//      if (UT.Entities.getFullInvisibility(aEvent.entityPlayer)) {aEvent.setCanceled(true); return;}
	}

	@SubscribeEvent
	public void receiveRenderSpecialsEvent(RenderPlayerEvent.Pre<?> aEvent) {
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
