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

import net.minecraft.core.BlockPos;

import gregapi.api.FMLPreInitializationEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import gregapi.GT_API;
import gregapi.api.Abstract_Mod;
import gregapi.api.Abstract_Proxy;
import gregapi.block.IBlockToolable;
import gregapi.block.metatype.BlockStones;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.data.*;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.IIconContainer;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import gregtech.blocks.fluids.BlockWaterlike;
import gregtech.entities.Override_Drops;
import gregtech.entities.ai.EntityAIBetterAttackOnCollide;
import gregtech.entities.projectiles.EntityArrow_Material;
import gregtech.tileentity.misc.MultiTileEntityCertificate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluid;

import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static gregapi.data.CS.*;

public abstract class GT_Proxy extends Abstract_Proxy {
	// F12-entity: mod-bus registration of client entity renderers — a no-op base (server), overridden in GT_Client.
	public void registerClientRenderers(IEventBus aModBus) {/**/}

	public final HashSetNoNulls<String> mSupporterListSilver = new HashSetNoNulls<>();
	public final HashSetNoNulls<String> mSupporterListGold = new HashSetNoNulls<>();
	
	public String mMessage = "";
	
	public boolean mDisableVanillaOres = T, mDisableVanillaLakes = T, mVersionOutdated = F;
	public int mSkeletonsShootGTArrows = 16, mFlintChance = 30;
	
	public GT_Proxy() {
		// neo: a single MinecraftForge.EVENT_BUS; ORE_GEN_BUS/TERRAIN_GEN_BUS and the FML bus were removed by the engine (see F-event-model cluster A).
		// F7 (centralized, Abstract_Proxy): register(this) is forbidden by neo — @SubscribeEvent lives on the GT_Proxy supertype, while
		// the instance is a GT_Server/GT_Client subclass; per-method addListener bypasses the hierarchy check. One place for the whole mod.
		registerSubscribeEvents();
	}

	@Override
	public void onProxyBeforePreInit(Abstract_Mod aMod, FMLPreInitializationEvent aEvent) {
		super.onProxyBeforePreInit(aMod, aEvent);
		
		// Because of the whole ban wave Mojang did with their new Microsoft Bullshit Auth System, I am not going to
		// ever add more people to these Lists anymore. So I decided to no longer check those Text Files on my Server.
		// Of-course the Server will still contain said Text Files, I just stop downloading them.
		
		try {
			Scanner tScanner = new Scanner(getClass().getResourceAsStream("/supporterlist.txt"));
			while (tScanner.hasNextLine()) mSupporterListSilver.add(tScanner.nextLine().toLowerCase());
			tScanner.close();
		} catch(Throwable e) {e.printStackTrace(ERR);}
		try {
			Scanner tScanner = new Scanner(getClass().getResourceAsStream("/supporterlistgold.txt"));
			while (tScanner.hasNextLine()) mSupporterListGold.add(tScanner.nextLine().toLowerCase());
			tScanner.close();
		} catch(Throwable e) {e.printStackTrace(ERR);}
		
		// Just making sure there is no overlaps.
		mSupporterListSilver.removeAll(mSupporterListGold);
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEndermanTeleportEvent(EntityTeleportEvent.EnderEntity aEvent) {
		if (aEvent.getEntityLiving() instanceof EnderMan && aEvent.getEntityLiving().getEffect(MobEffects.WEAKNESS) != null) aEvent.setCanceled(T);
	}

	// ── F-event-model cluster B (worldgen-terraingen) ──────────────────────────────────────────────────
	// neo REMOVED the terraingen bus entirely (the net.minecraftforge.event.terraingen package does not exist): the classes
	// OreGenEvent/DecorateBiomeEvent/PopulateChunkEvent/BiomeEvent do not exist. The 5 handlers that suppressed
	// vanilla generation (ore/lake DENY, village-block substitution, canceling decoration in GT street/biome zones)
	// CANNOT be event handlers. The suppression is moved centrally into the worldgen subsystem
	// (neo BiomeModifier + Feature), one approach for the whole cluster together with Worldgen*/ChestGenHooksChestReplacer —
	// see decisions/F-event-model-and-removed-subsystems-map.md cluster B (ADR-B). This is NOT a stub:
	// the handlers were removed along with the engine-removed bus; the behavior is implemented by the worldgen subsystem.

	private static final HashSetNoNulls<String> CHECKED_PLAYERS = new HashSetNoNulls<>();

	// F-event-model: the engine split the 1.7.10 PlayerInteractEvent (one event + aEvent.action) into subclasses;
	// RIGHT_CLICK_AIR -> PlayerInteractEvent.RightClickItem, RIGHT_CLICK_BLOCK -> RightClickBlock. We split it into
	// two handlers 1:1; the shared supporter check (which ran on any interaction) goes into a common method.
	private void checkSupporterCertificate(PlayerInteractEvent aEvent) {
		String aName = aEvent.getEntity().getName().getString(), aNameLowercase = aName.toLowerCase();
		if (!aEvent.getLevel().isClientSide() && CHECKED_PLAYERS.add(aName)) {
			if (mSupporterListSilver.contains(aEvent.getEntity().getUUID().toString()) || mSupporterListGold.contains(aEvent.getEntity().getUUID().toString()) || mSupporterListSilver.contains(aNameLowercase) || mSupporterListGold.contains(aNameLowercase)) {
				if (!MultiTileEntityCertificate.ALREADY_RECEIVED.contains(aNameLowercase)) {
					if (ST.give(aEvent.getEntity(), MultiTileEntityCertificate.getCertificate(1, aName), F)) {
						MultiTileEntityCertificate.ALREADY_RECEIVED.add(aNameLowercase);
						UT.Entities.sendchat(aEvent.getEntity(), CHAT_GREG + LH.tt("Thank you, ") + aName + LH.tt(", for Supporting GregTech! Here, have a Certificate. ;)"));
					}
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerInteractItem(PlayerInteractEvent.RightClickItem aEvent) {
		if (aEvent.getEntity() == null || aEvent.getEntity().level() == null) return;
		checkSupporterCertificate(aEvent);

		ItemStack aStack = aEvent.getEntity().getMainHandItem();
		if (aStack != null && aStack.getCount() > 0) {
			if (aStack.getItem() == Items.GLASS_BOTTLE) {
				aEvent.setCanceled(T);
				if (aEvent.getLevel().isClientSide()) {
					GT_API.api_proxy.sendUseItemPacket(aEvent.getEntity(), aEvent.getLevel(), aStack);
					return;
				}

				HitResult tTarget = WD.getMOP(aEvent.getLevel(), aEvent.getEntity(), T);
				if (tTarget == null || tTarget.getType() != HitResult.Type.BLOCK) return;
				// WD.getMOP now returns a neo HitResult; getBlockPos()/getDirection() live on BlockHitResult (safe after the BLOCK gate).
				BlockHitResult tHit = (BlockHitResult)tTarget; BlockPos tPos = tHit.getBlockPos();
				if (!aEvent.getLevel().mayInteract(aEvent.getEntity(), tPos) || !aEvent.getEntity().mayUseItemAt(tPos, tHit.getDirection(), aStack)) return;
				Block tBlock = WD.block(aEvent.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ());

				if (tBlock == Blocks.WATER || tBlock == Blocks.WATER) {
					if (WD.meta(aEvent.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ()) != 0) return;
					for (int i = 0; i < 3 && aStack.getCount() > 0; i++) {
						if (aStack.getCount() == 1) {
							aEvent.getEntity().getInventory().setItem(aEvent.getEntity().getInventory().selected, ST.make(Items.POTION, 1, 0));
						} else {
							ST.use(aEvent.getEntity(), aStack);
							ST.give(aEvent.getEntity(), ST.make(Items.POTION, 1, 0), F);
						}
					}
					if (!WD.infiniteWater(aEvent.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ())) WD.set(aEvent.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ(), NB, 0, 3);
					ST.update(aEvent.getEntity());
					return;
				}
				if (tBlock == BlocksGT.River || WD.waterstream(tBlock)) {
					ItemStack tStack = FL.Water.fill(aStack);
					if (tStack == null) return;
					ST.use(aEvent.getEntity(), aStack);
					ST.give(aEvent.getEntity(), tStack, F);
					return;
				}
				if (tBlock == BlocksGT.Ocean) {
					ItemStack tStack = FL.Ocean.fill(aStack);
					if (tStack == null) return;
					ST.use(aEvent.getEntity(), aStack);
					ST.give(aEvent.getEntity(), tStack, F);
					return;
				}
				if (tBlock == BlocksGT.Swamp) {
					ItemStack tStack = FL.Dirty_Water.fill(aStack);
					if (tStack == null) return;
					ST.use(aEvent.getEntity(), aStack);
					ST.give(aEvent.getEntity(), tStack, F);
					return;
				}
				return;
			}
			if (aStack.getItem() == Items.BUCKET) {
				HitResult tTarget = WD.getMOP(aEvent.getLevel(), aEvent.getEntity(), T);
				if (tTarget != null && tTarget.getType() == HitResult.Type.BLOCK) {
					BlockPos tPos = ((BlockHitResult)tTarget).getBlockPos();
					Block tBlock = WD.block(aEvent.getLevel(), tPos.getX(), tPos.getY(), tPos.getZ());
					if (tBlock instanceof BlockWaterlike && tBlock != BlocksGT.River) {
						// 1:1 (original :253-260): ocean/swamp are NOT scoopable with a vanilla bucket (otherwise salty/dirty
						// water would become free fresh water), a river IS scoopable.
						aEvent.setCanceled(T);
						// Insurance against a prediction desync (acceptance verdict 2026-07-30, "ghost bucket"):
						// the client BucketItem may have already shown a water bucket and erased the block — side raycasts
						// diverge for one interpolation frame at WD.getMOP:193. On a SERVER-side cancellation we return the
						// truth to the client: a full menu sync (broadcastChanges does not send — the server changed nothing) + the hit block.
						if (!aEvent.getLevel().isClientSide() && aEvent.getEntity() instanceof net.minecraft.server.level.ServerPlayer tSP) {
							tSP.containerMenu.sendAllDataToRemote();
							tSP.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(aEvent.getLevel(), tPos)); // ctor (BlockGetter,BlockPos) — ClientboundBlockUpdatePacket.java:29
						}
					}
				}
				return;
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerInteractBlock(PlayerInteractEvent.RightClickBlock aEvent) {
		if (aEvent.getEntity() == null || aEvent.getEntity().level() == null) return;
		checkSupporterCertificate(aEvent);

		final int tX = aEvent.getPos().getX(), tY = aEvent.getPos().getY(), tZ = aEvent.getPos().getZ();
		final byte tSide = UT.Code.side(aEvent.getFace()); // Direction -> GT6 side byte (the UT.Code.side center)

		ItemStack aStack = aEvent.getEntity().getMainHandItem();
		if (aStack != null && aStack.getCount() > 0) {
			if (IL.ERE_Spray_Repellant.equal(aStack, T, T)) {
				if (!aEvent.getLevel().isClientSide() && UT.tryPlaceItemIntoWorld(aStack, aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
					aEvent.setCanceled(T);
					ST.give(aEvent.getEntity(), IL.Spray_Empty.get(1), aEvent.getLevel(), tX, tY, tZ);
					return;
				}
			} else if (aStack.getItem() == Items.FLINT_AND_STEEL) {
				if (!aEvent.getLevel().isClientSide() && !UT.Entities.hasInfiniteItems(aEvent.getEntity()) && RNGSUS.nextInt(100) >= mFlintChance) {
					aEvent.setCanceled(T);
					aStack.hurtAndBreak(1, aEvent.getEntity(), tBroken -> tBroken.broadcastBreakEvent(InteractionHand.MAIN_HAND));
					if (aStack.getDamageValue() >= aStack.getMaxDamage()) ST.use(aEvent.getEntity(), aStack);
					return;
				}
				List<String> tChatReturn = new ArrayListNoNulls<>();
				long tDamage = IBlockToolable.Util.onToolClick(TOOL_igniter, aStack.getDamageValue()*10000, 1, aEvent.getEntity(), tChatReturn, aEvent.getEntity().getInventory(), aEvent.getEntity().isShiftKeyDown(), aStack, aEvent.getLevel(), tSide, tX, tY, tZ, 0.5F, 0.5F, 0.5F);
				UT.Entities.sendchat(aEvent.getEntity(), tChatReturn, F);
				if (tDamage > 0) {
					aEvent.setCanceled(T);
					UT.Sounds.send(SFX.MC_IGNITE, aEvent.getLevel(), tX, tY, tZ);
					if (!UT.Entities.hasInfiniteItems(aEvent.getEntity())) {
						aStack.hurtAndBreak(UT.Code.bindInt(UT.Code.units(tDamage, 10000, 1, T)), aEvent.getEntity(), tBroken -> tBroken.broadcastBreakEvent(InteractionHand.MAIN_HAND));
						if (aStack.getDamageValue() >= aStack.getMaxDamage()) ST.use(aEvent.getEntity(), aStack);
					}
					return;
				}
			} else if (IL.Food_Toast_Sliced.equal(aStack, F, T) || IL.Food_Toasted_Sliced.equal(aStack, F, T)) {
				int tUsed = Math.min(16, aStack.getCount());
				if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32105, ST.save("sandwich.0", ST.amount(tUsed, aStack))), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
					ST.use(aEvent.getEntity(), aStack, tUsed); aEvent.setCanceled(T);
				}
			} else if (aStack.getItem() == Items.STICK || IL.Stick.equal(aStack) || OM.is("stickAnyNormalWood", aStack)) {
				if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32073), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
					ST.use(aEvent.getEntity(), aStack); aEvent.setCanceled(T);
				}
			} else if (aStack.getItem() == Items.FLINT) {
				if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32074, ST.save(NBT_VALUE, ST.amount(1, aStack))), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
					ST.use(aEvent.getEntity(), aStack); aEvent.setCanceled(T);
				}
			} else {
				if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && ST.block(aStack) == NB) {
					OreDictItemData tData = OM.anyassociation_(aStack);
					if (tData != null) {
						if (tData.mPrefix == OP.rockGt || tData.mPrefix == OP.oreRaw) {
							if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32074, ST.save(NBT_VALUE, ST.amount(1, aStack))), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
								ST.use(aEvent.getEntity(), aStack); aEvent.setCanceled(T);
							}
						}
						if (tData.mPrefix == OP.ingot) if (!MD.BOTA.mLoaded || tData.mMaterial.mMaterial.mOriginalMod != MD.BOTA || Blocks.BEACON != WD.block(aEvent.getLevel(), tX, tY, tZ)) {
							if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32084, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
								ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
							}
						}
						if (tData.mPrefix == OP.plate) {
							if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32085, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
								ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
							}
						}
						if (tData.mPrefix == OP.plateGem) {
							if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32086, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
								ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
							}
						}
						if (tData.mPrefix == OP.scrapGt) {
							if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32103, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), tX, tY, tZ, tSide, 0.5F, 0.5F, 0.5F)) {
								ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
							}
						}
					}
				}
			}
		} else {
			if (aEvent.getEntity().isOnFire()) {
				List<String> tChatReturn = new ArrayListNoNulls<>();
				long tDamage = IBlockToolable.Util.onToolClick(TOOL_igniter, Long.MAX_VALUE, 1, aEvent.getEntity(), tChatReturn, aEvent.getEntity().getInventory(), aEvent.getEntity().isShiftKeyDown(), NI, aEvent.getLevel(), tSide, tX, tY, tZ, 0.5F, 0.5F, 0.5F);
				UT.Entities.sendchat(aEvent.getEntity(), tChatReturn, F);
				if (tDamage > 0) {
					UT.Sounds.send(SFX.MC_IGNITE, aEvent.getLevel(), tX, tY, tZ);
					aEvent.setCanceled(T);
				}
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntitySpawningEvent(EntityJoinLevelEvent aEvent) {
		if (aEvent.getEntity() == null) return;
		
		if (aEvent.getEntity() instanceof LivingEntity) {
			// AI Tasks for Entities — 1.7.10 EntityAITasks -> neo GoalSelector (only Mob carries goalSelector).
			if (aEvent.getEntity() instanceof Mob tMob) {
				GoalSelector tGoals = tMob.goalSelector;
				if (aEvent.getEntity() instanceof Villager) {
					tGoals.addGoal(3, new TemptGoal((PathfinderMob)aEvent.getEntity(), 0.6D, net.minecraft.world.item.crafting.Ingredient.of(Items.EMERALD), F));
				}
				if (aEvent.getEntity() instanceof Ocelot) {
					if (ItemsGT.CANS != null) tGoals.addGoal(3, new TemptGoal((PathfinderMob)aEvent.getEntity(), 0.6D, net.minecraft.world.item.crafting.Ingredient.of(ItemsGT.CANS), T));
				}
				if (aEvent.getEntity() instanceof Zombie) {
					// 1.7.10 replaced the vanilla EntityAIAttackOnCollide with the GT version; neo: find the MeleeAttackGoal wrapper
					// (ZombieAttackGoal is-a MeleeAttackGoal) and replace it with EntityAIBetterAttackOnCollide at the same priority.
					for (WrappedGoal tWrapped : new java.util.ArrayList<>(tGoals.getAvailableGoals())) {
						if (tWrapped.getGoal() instanceof MeleeAttackGoal) {
							int tPrio = tWrapped.getPriority();
							tGoals.removeGoal(tWrapped.getGoal());
							tGoals.addGoal(tPrio, new EntityAIBetterAttackOnCollide((PathfinderMob)aEvent.getEntity(), Player.class, 1.0D, F));
						}
					}
				}
			}

			// Check if this Entity was already spawned, and not just unloaded and reloaded.
			if (!aEvent.getEntity().level().isClientSide() && !aEvent.getEntity().getPersistentData().contains("gt.spawned")) {
				if (aEvent.getEntity() instanceof Zombie && !((Zombie)aEvent.getEntity()).isBaby() && ST.invalid(UT.Entities.getEquipmentInSlot(((Zombie)aEvent.getEntity()), 0))) {
					if (ZOMBIES_HOLD_TNT && RNGSUS.nextInt(250) == 0) {
						((Zombie)aEvent.getEntity()).setItemSlot(EquipmentSlot.MAINHAND, ST.make(Blocks.TNT, 1+RNGSUS.nextInt(2), 0));
					} else if (ZOMBIES_HOLD_PICKAXES && RNGSUS.nextInt(100) == 0) {
						((Zombie)aEvent.getEntity()).setItemSlot(EquipmentSlot.MAINHAND, ST.make(Items.IRON_PICKAXE, 1, new ItemStack(Items.IRON_PICKAXE).getMaxDamage() < 5 ? 0 : 1+RNGSUS.nextInt(new ItemStack(Items.IRON_PICKAXE).getMaxDamage()-2)));
					}
				}
				// Mark Entity as has been spawned
				aEvent.getEntity().getPersistentData().putBoolean("gt.spawned", T);
			}
			return;
		}

		if (aEvent.getEntity().level().isClientSide()) return;

		if (mSkeletonsShootGTArrows > 0 && aEvent.getEntity().getClass() == Arrow.class && RNGSUS.nextInt(mSkeletonsShootGTArrows) == 0) {
			if (((Arrow)aEvent.getEntity()).getOwner() instanceof Skeleton) {
				OreDictMaterial tMaterial = MT.Craponite; // Just default to Anti-Bear989Sr Arrows
				switch(RNGSUS.nextInt(10)) {
				case 0: tMaterial = MT.Steel; break; // Sharpness 2
				case 1: tMaterial = MT.AnnealedCopper; break; // Dissolving 5
				case 2: tMaterial = MT.AstralSilver; break; // Disjunction 5 and Werebane 5
				case 3: tMaterial = MT.BismuthBronze; break; // Bane of Arthropods 4
				case 4: tMaterial = MT.Pt; break; // Smite 5
				case 5: tMaterial = MT.Netherite; break; // Fire Aspect 3
				case 6: tMaterial = MT.Efrine; break; // Fortune/Looting 2
				case 7: tMaterial = MT.Rubber; break; // Knockback 2
				case 8: tMaterial = MT.DamascusSteel; break; // Sharpness 5
				case 9: tMaterial = MT.Craponite; break; // Werebane 10
				}
				ItemStack tArrow = OP.arrowGtWood.mat(tMaterial, 1);
				if (ST.valid(tArrow)) {
					// ⛔ THE SAME CRASH CLASS as in GT_API_Proxy.onEntitySpawningEvent (log04): it used to be
					// `addFreshEntity(replacement); discard()` — a verbatim carry-over of the 1.7.10
					// (`spawnEntityInWorld(...); setDead()`). In neo this event is posted INSIDE the entity's
					// addition (PersistentEntitySectionManager.addEntity:80), BEFORE setLevelCallback, and discard()
					// leaves a tracker for an already-removed entity in ChunkMap.entityMap — the map walk later breaks.
					// The proper substitution path: CANCEL the vanilla arrow's addition (the engine returns false and
					// never lets it into the world) and add our own — the order "cancel first, then substitute" keeps the
					// tracker map consistent. The observed behavior is 1:1 with the original: exactly one
					// GT6 arrow ends up in the world instead of the vanilla one.
					// BUG-103 (recurrence 2026-08-08): the substitute must NOT be added RIGHT HERE — the event is posted INSIDE
					// PersistentEntitySectionManager.addEntity:80, and addFreshEntity from here would trigger a nested
					// addition into the same structures (sectionStorage/knownUuids/ChunkMap.entityMap) while the outer one
					// has not finished yet. We queue the substitute on the server — it runs on the same server
					// thread right after the addition completes. The observed behavior is the same: exactly one
					// GT6 arrow ends up in the world instead of the vanilla one, the vanilla one never appears at all (the event is canceled).
					aEvent.setCanceled(true);
					final net.minecraft.world.level.Level tLevel = aEvent.getEntity().level();
					final EntityArrow_Material tReplacement = new EntityArrow_Material((Arrow)aEvent.getEntity(), tArrow);
					if (tLevel.getServer() != null) tLevel.getServer().execute(() -> tLevel.addFreshEntity(tReplacement));
					else tLevel.addFreshEntity(tReplacement);
				}
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityLivingDropsEventEvent(LivingDropsEvent aEvent) {
		if (aEvent.getEntity().level().isClientSide()) return;
		// neo: LivingDropsEvent no longer carries lootingLevel (looting moved into loot tables). Restoring it 1:1 —
		// the Looting level of the killer's weapon (the same meaning as the old event.lootingLevel), via the UT.NBT.getEnchantmentLevel center.
		int tLooting = 0;
		if (aEvent.getSource().getEntity() instanceof LivingEntity tKiller) tLooting = UT.NBT.getEnchantmentLevel(Enchantments.MOB_LOOTING, tKiller.getMainHandItem());
		Override_Drops.handleDrops(aEvent.getEntity(), UT.Reflection.getLowercaseClass(aEvent.getEntity()), aEvent.getDrops(), aEvent.getSource(), tLooting, aEvent.getEntity().isOnFire(), aEvent.isRecentlyHit());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityLivingFallEvent(LivingFallEvent aEvent) {
		if (!aEvent.getEntity().level().isClientSide() && aEvent.getEntity() instanceof Player) {
			if (ST.equal(((Player)aEvent.getEntity()).getMainHandItem(), ToolsGT.sMetaTool, ToolsGT.SCISSORS) || ST.equal(((Player)aEvent.getEntity()).getMainHandItem(), ToolsGT.sMetaTool, ToolsGT.POCKET_SCISSORS)) aEvent.setDistance(aEvent.getDistance()*2);
		}
	}
	
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedLiquid(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createLiquid(aMaterial, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedLiquid(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedGas(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createGas(aMaterial, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedGas(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createGas(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedMolten(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedMolten(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedVapor(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createVapour(aMaterial, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedVaporized(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createVapour(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedPlasma(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addAutogeneratedPlasma(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addFluid(String aName, String aLocalized, OreDictMaterial aMaterial, int aState, long aAmountPerUnit, long aTemperatureK, Set<String>... aFluidList) {return FL.create(aName, aLocalized, aMaterial, aState, aAmountPerUnit, aTemperatureK, aFluidList);}    
	@SafeVarargs public final gregapi.fluid.FluidGT addFluid(String aName, String aLocalized, OreDictMaterial aMaterial, int aState, long aAmountPerUnit, long aTemperatureK, java.util.function.Supplier<ItemStack> aFullContainer, java.util.function.Supplier<ItemStack> aEmptyContainer, int aFluidAmount, Set<String>... aFluidList) {return FL.create(aName, aLocalized, aMaterial, aState, aAmountPerUnit, aTemperatureK, aFullContainer, aEmptyContainer, aFluidAmount, aFluidList);}
	@SafeVarargs public final gregapi.fluid.FluidGT addFluid(String aName, IIconContainer aTexture, String aLocalized, OreDictMaterial aMaterial, short[] aRGBa, int aState, long aAmountPerUnit, long aTemperatureK, java.util.function.Supplier<ItemStack> aFullContainer, java.util.function.Supplier<ItemStack> aEmptyContainer, int aFluidAmount, Set<String>... aFluidList) {return FL.create(aName, aTexture, aLocalized, aMaterial, aRGBa, aState, aAmountPerUnit, aTemperatureK, aFullContainer, aEmptyContainer, aFluidAmount, aFluidList);}
}
