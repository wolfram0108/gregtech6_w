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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraftforge.registries.DeferredRegister;

import static gregapi.data.CS.*;

/**
 * Центральный носитель GT6-регистраций {@code MobEffect} — ЕДИНСТВЕННОЕ место мода, регистрирующее
 * зелья-эффекты в neo (тот же приём, что {@code gregapi.enchants.EnchantsGT6} для чаров и
 * {@code gregapi.player.EntityFoodTracker#ATTACHMENTS} для attachment-типов).
 *
 * <p>Кому это нужно: в 1.7.10 класс {@code CS.PotionsGT} нёс id зелий ЧУЖИХ модов, заполнявшиеся на
 * postInit ({@code gregtech6/src/.../GT_API.java:773-790}): IC2 (radiation), EnviroMine (5 средовых),
 * Immersive Engineering (flammable/slippery/conductive/sticky). Ни одного из этих модов для 26.1.2 не
 * существует, а функцию эффектов потребляет сам GT6 (еда/напитки {@code MultiItemFood}/{@code
 * Loader_Fluids}, купание в нефтях {@code Loader_Blocks:163-164} → {@code BlockBaseFluid:519,525}).
 * По правилу «функция, не авторство» эффекты регистрируются здесь, поведение — 1:1 с исходниками
 * модов-владельцев (декомпил-референсы в дереве проекта: {@code ImmersiveEngineering-1.7.10/},
 * {@code EnviroMine-1.7.10/}).
 *
 * <p>Регистрируются РОВНО 5 — те, чью функцию GT6 реально накладывает:
 * <ul>
 * <li>{@code flammable}/{@code slippery}/{@code conductive}/{@code sticky} — IE
 *     ({@code IEPotions.java:29-38}, тик-поведение {@code :108-129}, обработчики урона/прыжка —
 *     {@code EventHandler.java:387-408}, продублированы в {@code GT_API_Proxy});</li>
 * <li>{@code insanity} — EnviroMine ({@code EnviroPotion.java:153-287}, каденция 30 тиков —
 *     {@code EM_StatusManager.java:84-88}).</li>
 * </ul>
 * НЕ регистрируются (осознанно, не долг): {@code RADIATION} — у Грега свой фолбэк-дизайн без IC2
 * (wither/poison, {@code UT.java:3118-3122}, {@code EntityFoodTracker:176-197}), он и есть каноническое
 * поведение, а IC2-референса поведения в проекте нет («не найдено — не выдумываем»); {@code DEHYDRATION}
 * — фолбэк hunger ({@code EntityFoodTracker:238-244}) плюс Грег сам кладёт hunger-пару рядом с
 * dehydration-парой в те же напитки ({@code Loader_Fluids.java:387}), а EnviroMine-поведение осушало
 * ЕГО стат гидратации, которого не существует (завязка на GT6-стат {@code mDehydration} создала бы
 * петлю «стат → эффект → стат», отсутствующую в оригинале); {@code HYPOTHERMIA}/{@code HEATSTROKE}/
 * {@code FROSTBITE} — GT6 их никогда не НАКЛАДЫВАЕТ (единственный потребитель — снятие Pill_Cure_All,
 * снятие незарегистрированного эффекта = no-op, 1:1 с «мод не установлен»).
 *
 * <p>Привязка к int-каналу {@code applyPotion(Entity,int,...)}: см. {@code GT_API.onModPostInit2Deferred}
 * — «real IDs are to be set on API postInit» ({@code CS.java:1690}), механизм Грега сохранён, источник
 * id теперь этот реестр. Численные id — дефолты конфигов модов-владельцев (IE {@code Config.getPotionID(24,
 * ...)} → 24-27; EnviroMine {@code EM_Settings.java:71} insanity=31).
 */
public class MobEffectsGT {

	private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MD.GAPI.mID);

	/** 1:1 int-id канала 1.7.10 (дефолты конфигов модов-владельцев), уходят в PotionsGT.ID_* на postInit. */
	public static final int ID_FLAMMABLE = 24, ID_SLIPPERY = 25, ID_CONDUCTIVE = 26, ID_STICKY = 27, ID_INSANITY = 31;

	/** IE flammable: сам по тику ничего не делает ({@code IEPotions.java:116-129} — performEffect пуст для
	 *  него), поведение целиком в обработчике урона ({@code EventHandler.java:390-395} → {@code GT_API_Proxy}). */
	public static final DeferredHolder<MobEffect, MobEffect> FLAMMABLE = EFFECTS.register("flammable",
		() -> new MobEffectGT6(MobEffectCategory.HARMFUL, 0x8f3f1f));

	/** IE slippery: каждый тик на земле скользит + 1/300 шанс выронить предмет из руки
	 *  ({@code IEPotions.java:118-128}: tick=0 → isReady всегда T). */
	public static final DeferredHolder<MobEffect, MobEffect> SLIPPERY = EFFECTS.register("slippery",
		() -> new MobEffectSlippery(MobEffectCategory.HARMFUL, 0x171003));

	/** IE conductive: сам по тику ничего не делает; в 1.7.10 усиливал урон типа "flux" (IE-электричество,
	 *  {@code EventHandler.java:396-401}) — обработчик продублирован 1:1 в {@code GT_API_Proxy}; в сборке
	 *  без IE-машин источника "flux"-урона нет, как не было и в 1.7.10 (GT6-электроурон шёл каналом
	 *  IC2-или-heat, {@code DamageSources.getElectricDamage}). */
	public static final DeferredHolder<MobEffect, MobEffect> CONDUCTIVE = EFFECTS.register("conductive",
		() -> new MobEffectGT6(MobEffectCategory.HARMFUL, 0x690000));

	/** IE sticky: атрибут скорости −50 % × (amp+1), op 2 = MULTIPLY_TOTAL ({@code IEPotions.java:38}
	 *  {@code func_111184_a(movementSpeed, uuid, -0.5D, 2)}; neo масштабирует amount×(amplifier+1) —
	 *  {@code MobEffect.AttributeTemplate.create}, тот же закон, что 1.7.10). Ослабление прыжка —
	 *  {@code EventHandler.java:403-408} → {@code GT_API_Proxy}. */
	public static final DeferredHolder<MobEffect, MobEffect> STICKY = EFFECTS.register("sticky",
		() -> new MobEffectGT6(MobEffectCategory.HARMFUL, 0x9c6800)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(MD.GAPI.mID, "effect.sticky"), -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	/** EnviroMine insanity, каденция 30 тиков ({@code EM_StatusManager.java:84-88}): amp≥1 → тошнота 200
	 *  тиков с шансом 1/(50/(amp+1)); игроку — фантомный жуткий звук в случайной точке ±3 блока с тем же
	 *  шансом ({@code EnviroPotion.java:153-287}). Ветка amp≥2 (fake-death GUI EnviroMine) не переносится:
	 *  GUI чужого мода без эквивалента, GT6-потребители кладут максимум amp 1 ({@code Loader_Fluids.java:517,636}). */
	public static final DeferredHolder<MobEffect, MobEffect> INSANITY = EFFECTS.register("insanity",
		() -> new MobEffectInsanity(MobEffectCategory.HARMFUL, 5578058));

	/** Порядок 1:1 со switch 0-15 {@code EnviroPotion.java:187-269} (имена звуков 1.7.10 → neo-эквиваленты
	 *  по каталогу {@code SoundEvents}); часть констант neo — голые {@code SoundEvent}, пакет требует
	 *  {@code Holder} → {@code wrapAsHolder} из живого реестра (не direct — сетевой кодек шлёт id). */
	private static Holder<SoundEvent>[] SOUNDS = null;
	@SuppressWarnings("unchecked")
	private static Holder<SoundEvent>[] sounds() {
		if (SOUNDS == null) SOUNDS = new Holder[] {
			  SoundEvents.AMBIENT_CAVE                                                    // ambient.cave.cave
			, SoundEvents.GENERIC_EXPLODE                                                 // random.explode
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

	/** База: без тик-поведения (поведение — в обработчиках {@code GT_API_Proxy} или только атрибуты). */
	private static class MobEffectGT6 extends MobEffect {
		private MobEffectGT6(MobEffectCategory aCategory, int aColor) {super(aCategory, aColor);}
	}

	private static class MobEffectSlippery extends MobEffect {
		private MobEffectSlippery(MobEffectCategory aCategory, int aColor) {super(aCategory, aColor);}
		// IEPotion(id,bad,colour,tick=0,halveTick=F,icon): isReady при tickrate 0 отдаёт T каждый тик (IEPotions.java:108-114).
		@Override public boolean shouldApplyEffectTickThisTick(int aTickCount, int aAmplifier) {return T;}
		@Override public boolean applyEffectTick(ServerLevel aWorld, LivingEntity aEntity, int aAmplifier) {
			// 1:1 IEPotions.java:118-128: moveFlying(0,1,0.005F) → moveRelative (тот же вектор «вперёд» и коэффициент).
			if (aEntity.onGround()) aEntity.moveRelative(0.005F, new Vec3(0, 0, 1));
			if (aEntity.getRandom().nextInt(300) == 0) {
				ItemStack tHeld = aEntity.getMainHandItem();
				if (!tHeld.isEmpty()) {
					ItemEntity tDropped = aEntity.spawnAtLocation(aWorld, tHeld.copy(), 1.0F);
					if (tDropped != null) tDropped.setPickUpDelay(20);
					aEntity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				}
			}
			return T;
		}
	}

	private static class MobEffectInsanity extends MobEffect {
		private MobEffectInsanity(MobEffectCategory aCategory, int aColor) {super(aCategory, aColor);}
		@Override public boolean shouldApplyEffectTickThisTick(int aTickCount, int aAmplifier) {return aTickCount % 30 == 0;}
		@Override public boolean applyEffectTick(ServerLevel aWorld, LivingEntity aEntity, int aAmplifier) {
			RandomSource tRNG = aEntity.getRandom();
			int tChance = Math.max(1, 50 / (aAmplifier + 1));
			if (aAmplifier >= 1 && tRNG.nextInt(tChance) == 0) UT.Entities.applyPotion(aEntity, MobEffects.NAUSEA, 200, 0, F);
			if (aEntity instanceof ServerPlayer tPlayer && tRNG.nextInt(tChance) == 0) {
				float tX = (tRNG.nextInt(6) - 3) * tRNG.nextFloat(), tY = (tRNG.nextInt(6) - 3) * tRNG.nextFloat(), tZ = (tRNG.nextInt(6) - 3) * tRNG.nextFloat();
				float tPitch = tRNG.nextBoolean() ? 0.2F : (tRNG.nextFloat() - tRNG.nextFloat()) * 0.2F + 1.0F;
				tPlayer.connection.send(new ClientboundSoundPacket(sounds()[tRNG.nextInt(16)], SoundSource.AMBIENT, aEntity.getX() + tX, aEntity.getY() + tY, aEntity.getZ() + tZ, 1.0F, tPitch, tRNG.nextLong()));
			}
			return T;
		}
	}

	/** Центральная точка подписки — вызывается ОДИН раз из {@code GT_API}-конструктора рядом с
	 *  {@code EnchantsGT6.register(aModBus)} (тот же мод-бас). Английские имена — под modern-ключами
	 *  {@code Util.makeDescriptionId("effect", ...)} (единственные, что читает движок через
	 *  {@code MobEffect.getDisplayName}); тексты 1:1 из lang-файлов модов-владельцев
	 *  ({@code IE .../en_US.lang}, {@code EnviroMine .../en_US.lang}). */
	public static void register(IEventBus aModBus) {
		EFFECTS.register(aModBus);
		LH.add("effect."+MD.GAPI.mID+".flammable" , "Flammable" );
		LH.add("effect."+MD.GAPI.mID+".slippery"  , "Slippery"  );
		LH.add("effect."+MD.GAPI.mID+".conductive", "Conductive");
		LH.add("effect."+MD.GAPI.mID+".sticky"    , "Sticky"    );
		LH.add("effect."+MD.GAPI.mID+".insanity"  , "Insanity"  );
	}
}
