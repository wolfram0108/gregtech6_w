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

import net.minecraft.core.BlockPos;

import cpw.mods.fml.common.FMLCommonHandler;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
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
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate;
import net.minecraft.world.level.material.Fluid;

import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static gregapi.data.CS.*;

public abstract class GT_Proxy extends Abstract_Proxy {
	public final HashSetNoNulls<String> mSupporterListSilver = new HashSetNoNulls<>();
	public final HashSetNoNulls<String> mSupporterListGold = new HashSetNoNulls<>();
	
	public String mMessage = "";
	
	public boolean mDisableVanillaOres = T, mDisableVanillaLakes = T, mVersionOutdated = F;
	public int mSkeletonsShootGTArrows = 16, mFlintChance = 30;
	
	public GT_Proxy() {
		NeoForge.EVENT_BUS         .register(this);
		NeoForge.ORE_GEN_BUS       .register(this);
		NeoForge.TERRAIN_GEN_BUS   .register(this);
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@Override
	public void onProxyBeforePreInit(Abstract_Mod aMod, FMLCommonSetupEvent aEvent) {
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
	
	@SubscribeEvent
	public void onClientConnectedToServerEvent(ClientConnectedToServerEvent aEvent) {
		//
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEndermanTeleportEvent(EnderTeleportEvent aEvent) {
		if (aEvent.entityLiving instanceof EnderMan && aEvent.entityLiving.getActivePotionEffect(MobEffect.weakness) != null) aEvent.setCanceled(T);
	}
	
	private static final EnumSet<EventType> PREVENTED_ORES = EnumSet.of(EventType.COAL, EventType.IRON, EventType.GOLD, EventType.DIAMOND, EventType.REDSTONE, EventType.LAPIS, EventType.QUARTZ);
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onOreGenEvent(GenerateMinable aEvent) {
		if (mDisableVanillaOres && !WD.dimTF(aEvent.world) && PREVENTED_ORES.contains(aEvent.type)) aEvent.setResult(Result.DENY);
	}
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onTerrainGenEvent(DecorateBiomeEvent.Decorate aEvent) {
		if (WD.dimensionId(aEvent.world) == 0) {
			if (MD.RTG.mLoaded) {
				String tClassName = UT.Reflection.getLowercaseClass(aEvent.world.provider.terrainType);
				if ("WorldProviderSurfaceRTG".equalsIgnoreCase(tClassName) || "WorldTypeRTG".equalsIgnoreCase(tClassName)) return;
			}
			if (GENERATE_STREETS && (UT.Code.inside(-48, 47, aEvent.chunkX) || UT.Code.inside(-48, 47, aEvent.chunkZ))) {aEvent.setResult(Result.DENY); return;}
			if (GENERATE_BIOMES  && (UT.Code.inside(-96, 95, aEvent.chunkX) && UT.Code.inside(-96, 95, aEvent.chunkZ))) {aEvent.setResult(Result.DENY); return;}
		}
	}
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onTerrainGenEvent(PopulateChunkEvent.Populate aEvent) {
		if (WD.dimensionId(aEvent.world) == 0) {
			if (mDisableVanillaLakes && (aEvent.type == Populate.EventType.LAKE || aEvent.type == Populate.EventType.LAVA)) {aEvent.setResult(Result.DENY); return;}
			if (MD.RTG.mLoaded) {
				String tClassName = UT.Reflection.getLowercaseClass(aEvent.world.provider.terrainType);
				if ("WorldProviderSurfaceRTG".equalsIgnoreCase(tClassName) || "WorldTypeRTG".equalsIgnoreCase(tClassName)) return;
			}
			if (GENERATE_STREETS && (UT.Code.inside(-48, 47, aEvent.chunkX) || UT.Code.inside(-48, 47, aEvent.chunkZ))) {aEvent.setResult(Result.DENY); return;}
			if (GENERATE_BIOMES  && (UT.Code.inside(-96, 95, aEvent.chunkX) && UT.Code.inside(-96, 95, aEvent.chunkZ))) {aEvent.setResult(Result.DENY); return;}
		}
	}
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onGetVillageBlockIDEvent(BiomeEvent.GetVillageBlockID aEvent) {
		if (aEvent.original == Blocks.COBBLESTONE) {
			aEvent.replacement = (aEvent.biome == null ? BlocksGT.Andesite : BlocksGT.stones[(aEvent.biome.biomeID+6) % BlocksGT.stones.length]);
			aEvent.setResult(Result.DENY);
		}
	}
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onGetVillageBlockMetaEvent(BiomeEvent.GetVillageBlockMeta aEvent) {
		if (aEvent.original == Blocks.COBBLESTONE || aEvent.original instanceof BlockStones) {
			aEvent.replacement = BlockStones.SBRIK;
			aEvent.setResult(Result.DENY);
		}
		if (aEvent.original == Blocks.SANDSTONE) {
			aEvent.replacement = 2; // That's smooth Sandstone.
			aEvent.setResult(Result.DENY);
		}
	}
	
	private static final HashSetNoNulls<String> CHECKED_PLAYERS = new HashSetNoNulls<>();
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerInteraction(PlayerInteractEvent aEvent) {
		if (aEvent.getEntity() == null || aEvent.getEntity().level() == null || aEvent.action == null || aEvent.getLevel().provider == null) return;
		String aName = aEvent.getEntity().getName().getString(), aNameLowercase = aName.toLowerCase();
		if (!aEvent.getLevel().isClientSide() && CHECKED_PLAYERS.add(aName)) {
			if (mSupporterListSilver.contains(aEvent.getEntity().getUUID().toString()) || mSupporterListGold.contains(aEvent.getEntity().getUUID().toString()) || mSupporterListSilver.contains(aNameLowercase) || mSupporterListGold.contains(aNameLowercase)) {
				if (!MultiTileEntityCertificate.ALREADY_RECEIVED.contains(aNameLowercase)) {
					if (ST.give(aEvent.getEntity(), MultiTileEntityCertificate.getCertificate(1, aName), F)) {
						MultiTileEntityCertificate.ALREADY_RECEIVED.add(aNameLowercase);
						UT.Entities.sendchat(aEvent.getEntity(), CHAT_GREG + "Thank you, " + aName + ", for Supporting GregTech! Here, have a Certificate. ;)");
					}
				}
			}
		}
		
		ItemStack aStack = aEvent.getEntity().getMainHandItem();
		if (aStack != null && aStack.getCount() > 0) {
			if (aEvent.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
				if (aStack.getItem() == Items.GLASS_BOTTLE) {
					aEvent.setCanceled(T);
					if (aEvent.getLevel().isClientSide()) {
						GT_API.api_proxy.sendUseItemPacket(aEvent.getEntity(), aEvent.getLevel(), aStack);
						return;
					}
					
					HitResult tTarget = WD.getMOP(aEvent.getLevel(), aEvent.getEntity(), T);
					if (tTarget == null || tTarget.getType() != HitResult.Type.BLOCK || !aEvent.getLevel().canMineBlock(aEvent.getEntity(), tTarget.getBlockPos().getX(), tTarget.getBlockPos().getY(), tTarget.getBlockPos().getZ()) || !aEvent.getEntity().mayUseItemAt(new BlockPos(tTarget.getBlockPos().getX(), tTarget.getBlockPos().getY(), tTarget.getBlockPos().getZ()), ((BlockHitResult)tTarget).getDirection(), aStack)) return;
					Block tBlock = WD.block(aEvent.getLevel(), tTarget.getBlockPos().getX(), tTarget.getBlockPos().getY(), tTarget.getBlockPos().getZ());
					
					if (tBlock == Blocks.WATER || tBlock == Blocks.WATER) {
						if (WD.meta(aEvent.getLevel(), tTarget.getBlockPos().getX(), tTarget.getBlockPos().getY(), tTarget.getBlockPos().getZ()) != 0) return;
						for (int i = 0; i < 3 && aStack.getCount() > 0; i++) {
							if (aStack.getCount() == 1) {
								aEvent.getEntity().inventory.setItem(aEvent.getEntity().inventory.getSelectedSlot(), ST.make(Items.POTION, 1, 0));
							} else {
								ST.use(aEvent.getEntity(), aStack);
								ST.give(aEvent.getEntity(), ST.make(Items.POTION, 1, 0), F);
							}
						}
						if (!WD.infiniteWater(aEvent.getLevel(), tTarget.getBlockPos().getX(), tTarget.getBlockPos().getY(), tTarget.getBlockPos().getZ())) WD.set(aEvent.getLevel(), tTarget.getBlockPos().getX(), tTarget.getBlockPos().getY(), tTarget.getBlockPos().getZ(), NB, 0, 3);
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
						Block tBlock = WD.block(aEvent.getLevel(), tTarget.getBlockPos().getX(), tTarget.getBlockPos().getY(), tTarget.getBlockPos().getZ());
						if (tBlock instanceof BlockWaterlike && tBlock != BlocksGT.River) aEvent.setCanceled(T);
					}
					return;
				}
			}
			if (aEvent.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
				if (IL.ERE_Spray_Repellant.equal(aStack, T, T)) {
					if (!aEvent.getLevel().isClientSide() && aStack.getItem().onItemUse(aStack, aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
						aEvent.setCanceled(T);
						ST.give(aEvent.getEntity(), IL.Spray_Empty.get(1), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z);
						return;
					}
				} else if (aStack.getItem() == Items.FLINT_AND_STEEL) {
					if (!aEvent.getLevel().isClientSide() && !UT.Entities.hasInfiniteItems(aEvent.getEntity()) && RNGSUS.nextInt(100) >= mFlintChance) {
						aEvent.setCanceled(T);
						aStack.damageItem(1, aEvent.getEntity());
						if (aStack.getDamageValue() >= aStack.getMaxDamage()) ST.use(aEvent.getEntity(), aStack);
						return;
					}
					List<String> tChatReturn = new ArrayListNoNulls<>();
					long tDamage = IBlockToolable.Util.onToolClick(TOOL_igniter, aStack.getDamageValue()*10000, 1, aEvent.getEntity(), tChatReturn, aEvent.getEntity().inventory, aEvent.getEntity().isShiftKeyDown(), aStack, aEvent.getLevel(), (byte)aEvent.getFace(), aEvent.x, aEvent.y, aEvent.z, 0.5F, 0.5F, 0.5F);
					UT.Entities.sendchat(aEvent.getEntity(), tChatReturn, F);
					if (tDamage > 0) {
						aEvent.setCanceled(T);
						UT.Sounds.send(SFX.MC_IGNITE, aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z);
						if (!UT.Entities.hasInfiniteItems(aEvent.getEntity())) {
							aStack.damageItem(UT.Code.bindInt(UT.Code.units(tDamage, 10000, 1, T)), aEvent.getEntity());
							if (aStack.getDamageValue() >= aStack.getMaxDamage()) ST.use(aEvent.getEntity(), aStack);
						}
						return;
					}
				} else if (IL.Food_Toast_Sliced.equal(aStack, F, T) || IL.Food_Toasted_Sliced.equal(aStack, F, T)) {
					int tUsed = Math.min(16, aStack.getCount());
					if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32105, ST.save("sandwich.0", ST.amount(tUsed, aStack))), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
						ST.use(aEvent.getEntity(), aStack, tUsed); aEvent.setCanceled(T);
					}
				} else if (aStack.getItem() == Items.STICK || IL.Stick.equal(aStack) || OM.is("stickAnyNormalWood", aStack)) {
					if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32073), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
						ST.use(aEvent.getEntity(), aStack); aEvent.setCanceled(T);
					}
				} else if (aStack.getItem() == Items.FLINT) {
					if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32074, ST.save(NBT_VALUE, ST.amount(1, aStack))), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
						ST.use(aEvent.getEntity(), aStack); aEvent.setCanceled(T);
					}
				} else {
					if (!aEvent.getLevel().isClientSide() && aEvent.getEntity().isShiftKeyDown() && ST.block(aStack) == NB) {
						OreDictItemData tData = OM.anyassociation_(aStack);
						if (tData != null) {
							if (tData.mPrefix == OP.rockGt || tData.mPrefix == OP.oreRaw) {
								if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32074, ST.save(NBT_VALUE, ST.amount(1, aStack))), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
									ST.use(aEvent.getEntity(), aStack); aEvent.setCanceled(T);
								}
							}
							if (tData.mPrefix == OP.ingot) if (!MD.BOTA.mLoaded || tData.mMaterial.mMaterial.mOriginalMod != MD.BOTA || Blocks.BEACON != WD.block(aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z)) {
								if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32084, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
									ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
								}
							}
							if (tData.mPrefix == OP.plate) {
								if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32085, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
									ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
								}
							}
							if (tData.mPrefix == OP.plateGem) {
								if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32086, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
									ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
								}
							}
							if (tData.mPrefix == OP.scrapGt) {
								if (UT.tryPlaceItemIntoWorld(MultiTileEntityRegistry.getRegistry("gt.multitileentity").getItem(32103, ST.save(NBT_VALUE, aStack)), aEvent.getEntity(), aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z, (byte)aEvent.getFace(), 0.5F, 0.5F, 0.5F)) {
									ST.use(aEvent.getEntity(), aStack, aStack.getCount()); aEvent.setCanceled(T);
								}
							}
						}
					}
				}
			}
		} else {
			if (aEvent.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
				if (aEvent.getEntity().isBurning()) {
					List<String> tChatReturn = new ArrayListNoNulls<>();
					long tDamage = IBlockToolable.Util.onToolClick(TOOL_igniter, Long.MAX_VALUE, 1, aEvent.getEntity(), tChatReturn, aEvent.getEntity().inventory, aEvent.getEntity().isShiftKeyDown(), NI, aEvent.getLevel(), (byte)aEvent.getFace(), aEvent.x, aEvent.y, aEvent.z, 0.5F, 0.5F, 0.5F);
					UT.Entities.sendchat(aEvent.getEntity(), tChatReturn, F);
					if (tDamage > 0) {
						UT.Sounds.send(SFX.MC_IGNITE, aEvent.getLevel(), aEvent.x, aEvent.y, aEvent.z);
						aEvent.setCanceled(T);
					}
				}
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntitySpawningEvent(EntityJoinLevelEvent aEvent) {
		if (aEvent.getEntity() == null) return;
		
		if (aEvent.getEntity() instanceof LivingEntity) {
			// AI Tasks for Entities
			EntityAITasks tTasks = ((LivingEntity)aEvent.getEntity()).tasks;
			if (tTasks != null) {
				if (aEvent.getEntity() instanceof Villager) {
					tTasks.addTask(3, new EntityAITempt((PathfinderMob)aEvent.getEntity(), 0.6D, Items.EMERALD, F));
				}
				if (aEvent.getEntity() instanceof Ocelot) {
					if (ItemsGT.CANS != null) tTasks.addTask(3, new EntityAITempt((PathfinderMob)aEvent.getEntity(), 0.6D, ItemsGT.CANS, T));
				}
				if (aEvent.getEntity() instanceof Zombie) {
					for (int i = 0; i < tTasks.taskEntries.size(); i++) {
						EntityAITasks.EntityAITaskEntry tEntry = (EntityAITasks.EntityAITaskEntry)tTasks.taskEntries.get(i);
						if (tEntry.action.getClass() == EntityAIAttackOnCollide.class) {
							tEntry.action = new EntityAIBetterAttackOnCollide((EntityAIAttackOnCollide)tEntry.action);
						}
					}
				}
			}
			
			// Check if this Entity was already spawned, and not just unloaded and reloaded.
			if (!aEvent.getEntity().level().isClientSide() && !aEvent.getEntity().getPersistentData().contains("gt.spawned")) {
				if (aEvent.getEntity() instanceof Zombie && !((Zombie)aEvent.getEntity()).isBaby() && ST.invalid(UT.Entities.getEquipmentInSlot(((Zombie)aEvent.getEntity()), 0))) {
					if (ZOMBIES_HOLD_TNT && RNGSUS.nextInt(250) == 0) {
						((Zombie)aEvent.getEntity()).setCurrentItemOrArmor(0, ST.make(Blocks.TNT, 1+RNGSUS.nextInt(2), 0));
					} else if (ZOMBIES_HOLD_PICKAXES && RNGSUS.nextInt(100) == 0) {
						((Zombie)aEvent.getEntity()).setCurrentItemOrArmor(0, ST.make(Items.IRON_PICKAXE, 1, Items.IRON_PICKAXE.getMaxDamage() < 5 ? 0 : 1+RNGSUS.nextInt(Items.IRON_PICKAXE.getMaxDamage()-2)));
					}
				}
				// Mark Entity as has been spawned
				aEvent.getEntity().getPersistentData().putBoolean("gt.spawned", T);
			}
			return;
		}
		
		if (aEvent.getEntity().level().isClientSide()) return;
		
		if (mSkeletonsShootGTArrows > 0 && aEvent.getEntity().getClass() == Arrow.class && RNGSUS.nextInt(mSkeletonsShootGTArrows) == 0) {
			if (((Arrow)aEvent.getEntity()).shootingEntity instanceof Skeleton) {
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
					aEvent.getEntity().level().addFreshEntity(new EntityArrow_Material((Arrow)aEvent.getEntity(), tArrow));
					aEvent.getEntity().discard();
				}
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityLivingDropsEventEvent(LivingDropsEvent aEvent) {
		if (aEvent.entity.level().isClientSide() || aEvent.entityLiving == null) return;
		Override_Drops.handleDrops(aEvent.entityLiving, UT.Reflection.getLowercaseClass(aEvent.entityLiving), aEvent.drops, aEvent.source, aEvent.lootingLevel, aEvent.entityLiving.isBurning(), aEvent.recentlyHit);
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityLivingFallEvent(LivingFallEvent aEvent) {
		if (!aEvent.entity.level().isClientSide() && aEvent.entity instanceof Player) {
			if (ST.equal(((Player)aEvent.entity).getMainHandItem(), ToolsGT.sMetaTool, ToolsGT.SCISSORS) || ST.equal(((Player)aEvent.entity).getMainHandItem(), ToolsGT.sMetaTool, ToolsGT.POCKET_SCISSORS)) aEvent.distance *= 2;
		}
	}
	
	@SafeVarargs public final Fluid addAutogeneratedLiquid(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createLiquid(aMaterial, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedLiquid(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedGas(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createGas(aMaterial, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedGas(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createGas(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedMolten(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedMolten(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedVapor(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createVapour(aMaterial, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedVaporized(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createVapour(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedPlasma(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aFluidList);}
	@SafeVarargs public final Fluid addAutogeneratedPlasma(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aTexture, aFluidList);}
	@SafeVarargs public final Fluid addFluid(String aName, String aLocalized, OreDictMaterial aMaterial, int aState, long aAmountPerUnit, long aTemperatureK, Set<String>... aFluidList) {return FL.create(aName, aLocalized, aMaterial, aState, aAmountPerUnit, aTemperatureK, aFluidList);}    
	@SafeVarargs public final Fluid addFluid(String aName, String aLocalized, OreDictMaterial aMaterial, int aState, long aAmountPerUnit, long aTemperatureK, ItemStack aFullContainer, ItemStack aEmptyContainer, int aFluidAmount, Set<String>... aFluidList) {return FL.create(aName, aLocalized, aMaterial, aState, aAmountPerUnit, aTemperatureK, aFullContainer, aEmptyContainer, aFluidAmount, aFluidList);}
	@SafeVarargs public final Fluid addFluid(String aName, IIconContainer aTexture, String aLocalized, OreDictMaterial aMaterial, short[] aRGBa, int aState, long aAmountPerUnit, long aTemperatureK, ItemStack aFullContainer, ItemStack aEmptyContainer, int aFluidAmount, Set<String>... aFluidList) {return FL.create(aName, aTexture, aLocalized, aMaterial, aRGBa, aState, aAmountPerUnit, aTemperatureK, aFullContainer, aEmptyContainer, aFluidAmount, aFluidList);}
}
