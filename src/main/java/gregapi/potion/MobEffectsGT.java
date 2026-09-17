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

package gregapi.potion;

import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.util.UT;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;

import net.minecraftforge.registries.DeferredRegister;

import static gregapi.data.CS.*;

/** Central carrier for GT6's MobEffect registrations, the only place the mod registers potion effects in neo;
 *  registers exactly the five effects GT6's own behavior actually applies, others deliberately left out. */
public class MobEffectsGT {

	private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MD.GAPI.mID);

	/** Same numeric ids as the 1.7.10 channel (the owning mods' config defaults), copied into PotionsGT.ID_* on postInit. */
	public static final int ID_FLAMMABLE = 24, ID_SLIPPERY = 25, ID_CONDUCTIVE = 26, ID_STICKY = 27, ID_INSANITY = 31;

	/** IE flammable does nothing on tick by itself; its behavior lives entirely in the damage-event handler. */
	public static final net.minecraftforge.registries.RegistryObject<MobEffect> FLAMMABLE = EFFECTS.register("flammable",
		() -> new MobEffectGT6(MobEffectCategory.HARMFUL, 0x8f3f1f));

	/** IE slippery: slides every tick while grounded, plus a 1/300 chance to drop the held item. */
	public static final net.minecraftforge.registries.RegistryObject<MobEffect> SLIPPERY = EFFECTS.register("slippery",
		() -> new MobEffectSlippery(MobEffectCategory.HARMFUL, 0x171003));

	/** IE conductive does nothing on tick by itself; without IE machines there is no source of its damage type
	 *  either, same as in 1.7.10 where GT6's own electric damage used a different channel. */
	public static final net.minecraftforge.registries.RegistryObject<MobEffect> CONDUCTIVE = EFFECTS.register("conductive",
		() -> new MobEffectGT6(MobEffectCategory.HARMFUL, 0x690000));

	/** IE sticky: -50% movement speed scaled by amplifier, and weakens jumping through the same event handler. */
	/** 1.20.1's addAttributeModifier takes a string UUID, not a ResourceLocation; deriving it from the effect's own name
	 *  avoids a magic constant that could drift between runs or saves. */
	private static final String STICKY_MODIFIER_UUID = java.util.UUID.nameUUIDFromBytes((MD.GAPI.mID + ":effect.sticky").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();

	public static final net.minecraftforge.registries.RegistryObject<MobEffect> STICKY = EFFECTS.register("sticky",
		() -> new MobEffectGT6(MobEffectCategory.HARMFUL, 0x9c6800)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, STICKY_MODIFIER_UUID, -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));

	/** EnviroMine insanity on a 30-tick cadence: nausea plus a chance of a phantom scary sound nearby; the
	 *  amplifier-2 fake-death GUI branch is not ported since it depends on a foreign mod's own screen. */
	public static final net.minecraftforge.registries.RegistryObject<MobEffect> INSANITY = EFFECTS.register("insanity",
		() -> new MobEffectInsanity(MobEffectCategory.HARMFUL, 5578058));

	/** Sound order matches the original switch 0-15 one-for-one, mapped to the closest neo SoundEvents equivalents. */
	private static Holder<SoundEvent>[] SOUNDS = null;
	@SuppressWarnings("unchecked")
	private static Holder<SoundEvent>[] sounds() {
		if (SOUNDS == null) SOUNDS = new Holder[] {
			  SoundEvents.AMBIENT_CAVE                                                    // ambient.cave.cave
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.GENERIC_EXPLODE)      // random.explode
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.CREEPER_PRIMED)      // creeper.primed
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ZOMBIE_AMBIENT)      // mob.zombie.say
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDERMAN_AMBIENT)    // mob.endermen.idle
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.SKELETON_AMBIENT)    // mob.skeleton.say
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WITHER_AMBIENT)      // mob.wither.idle
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.SPIDER_AMBIENT)      // mob.spider.say
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LIGHTNING_BOLT_THUNDER) // ambient.weather.thunder
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LAVA_AMBIENT)        // liquid.lava
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WATER_AMBIENT)       // liquid.water
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.GHAST_AMBIENT)       // mob.ghast.moan
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARROW_HIT)           // random.bowhit
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_HURT)         // game.player.hurt
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDER_DRAGON_GROWL)  // mob.enderdragon.growl
			, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDERMAN_TELEPORT)   // mob.endermen.portal
		};
		return SOUNDS;
	}

	/** Base with no tick behavior; behavior lives either in the shared event handler or in attribute modifiers only. */
	private static class MobEffectGT6 extends MobEffect {
		private MobEffectGT6(MobEffectCategory aCategory, int aColor) {super(aCategory, aColor);}
	}

	private static class MobEffectSlippery extends MobEffect {
		private MobEffectSlippery(MobEffectCategory aCategory, int aColor) {super(aCategory, aColor);}
		// Zero tick rate means this fires every tick, matching the original's tick-rate-0 behavior.
		@Override public boolean isDurationEffectTick(int aTickCount, int aAmplifier) {return T;}
		@Override public void applyEffectTick(LivingEntity aEntity, int aAmplifier) {
			// Matches the original's forward-motion nudge and coefficient exactly.
			if (aEntity.onGround()) aEntity.moveRelative(0.005F, new Vec3(0, 0, 1));
			if (aEntity.getRandom().nextInt(300) == 0) {
				ItemStack tHeld = aEntity.getMainHandItem();
				if (!tHeld.isEmpty()) {
					ItemEntity tDropped = aEntity.spawnAtLocation(tHeld.copy(), 1.0F);
					if (tDropped != null) tDropped.setPickUpDelay(20);
					aEntity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				}
			}
		}
	}

	private static class MobEffectInsanity extends MobEffect {
		private MobEffectInsanity(MobEffectCategory aCategory, int aColor) {super(aCategory, aColor);}
		@Override public boolean isDurationEffectTick(int aTickCount, int aAmplifier) {return aTickCount % 30 == 0;}
		@Override public void applyEffectTick(LivingEntity aEntity, int aAmplifier) {
			RandomSource tRNG = aEntity.getRandom();
			int tChance = Math.max(1, 50 / (aAmplifier + 1));
			if (aAmplifier >= 1 && tRNG.nextInt(tChance) == 0) UT.Entities.applyPotion(aEntity, MobEffects.CONFUSION, 200, 0, F);
			if (aEntity instanceof ServerPlayer tPlayer && tRNG.nextInt(tChance) == 0) {
				float tX = (tRNG.nextInt(6) - 3) * tRNG.nextFloat(), tY = (tRNG.nextInt(6) - 3) * tRNG.nextFloat(), tZ = (tRNG.nextInt(6) - 3) * tRNG.nextFloat();
				float tPitch = tRNG.nextBoolean() ? 0.2F : (tRNG.nextFloat() - tRNG.nextFloat()) * 0.2F + 1.0F;
				tPlayer.connection.send(new ClientboundSoundPacket(sounds()[tRNG.nextInt(16)], SoundSource.AMBIENT, aEntity.getX() + tX, aEntity.getY() + tY, aEntity.getZ() + tZ, 1.0F, tPitch, tRNG.nextLong()));
			}
		}
	}

	/** Central subscription point, called once from the GT_API constructor; display names use the modern
	 *  description-id keys the engine actually reads, with text copied from the owning mods' own lang files. */
	public static void register(IEventBus aModBus) {
		EFFECTS.register(aModBus);
		LH.add("effect."+MD.GAPI.mID+".flammable" , "Flammable" );
		LH.add("effect."+MD.GAPI.mID+".slippery"  , "Slippery"  );
		LH.add("effect."+MD.GAPI.mID+".conductive", "Conductive");
		LH.add("effect."+MD.GAPI.mID+".sticky"    , "Sticky"    );
		LH.add("effect."+MD.GAPI.mID+".insanity"  , "Insanity"  );
	}
}
