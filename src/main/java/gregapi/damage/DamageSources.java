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

package gregapi.damage;

import com.mojang.datafixers.util.Either;
import gregapi.util.UT;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;

import static gregapi.data.CS.APRIL_FOOLS;
import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/**
 * @author Gregorius Techneticies
 */
public class DamageSources {
	private static final String MODID = "gregtech";
	private static final float DEFAULT_EXHAUSTION = 0.3F;
	private static final float ZERO_EXHAUSTION = 0.0F;

	static enum Kind {
		ALCOHOL    ("alcohol"    , T, T, F, target -> literal(target, " died from alcohol poisoning")),
		BUMBLE     ("bumble"     , T, F, F, target -> literal(target, " was allergic to Bumblebees")),
		CAFFEINE   ("caffeine"   , T, T, F, target -> literal(target, " overdosed on caffeine")),
		CHEM       ("chemical"   , F, F, F, target -> literal(target, " had a chemical accident")),
		CRUSHER    ("crusher"    , F, F, F, target -> literal(target, " was crushed to a pulp")),
		DEHYDRATION("dehydration", T, T, F, target -> literal(target, " took more than the deadly dose of Salt")),
		EXPLODING  ("exploded"   , T, T, T, target -> literal(target, " exploded")),
		FAT        ("fat"        , T, T, F, target -> literal(target, " got a Heart Attack")),
		FROST      ("frost"      , F, F, F, target -> literal(target, " got frozen")),
		HEAT       ("heat"       , F, F, F, target -> literal(target, " was boiled alive")),
		SHREDDER   ("shredder"   , F, F, F, target -> literal(target, " was shred into flakes")),
		SPIKE      ("spike"      , F, F, F, target -> literal(target, APRIL_FOOLS ? " stepped on a LEGO!" : " was impaled by a Spike!")),
		SUGAR      ("sugar"      , T, T, F, target -> literal(target, " died of Diabetes"));

		final String mMsgId;
		final ResourceKey<DamageType> mKey;
		final DamageType mType;
		final Set<TagKey<DamageType>> mTags;
		final Function<LivingEntity, Component> mDeathMessage;

		Kind(String aMsgId, boolean aBypassesArmor, boolean aAbsolute, boolean aCreative, Function<LivingEntity, Component> aDeathMessage) {
			mMsgId = aMsgId;
			mKey = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MODID, aMsgId));
			mTags = tags(aBypassesArmor, aAbsolute, aCreative);
			mType = new DamageType(aMsgId, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, mTags.isEmpty() ? DEFAULT_EXHAUSTION : ZERO_EXHAUSTION, DamageEffects.HURT);
			mDeathMessage = aDeathMessage;
		}
	}

	public static void bootstrap(BootstrapContext<DamageType> aContext) {
		for (Kind tKind : Kind.values()) aContext.register(tKind.mKey, tKind.mType);
	}

	public static DamageSource getElectricDamage() {
		return compatDamage("DMG_ELECTRIC", getHeatDamage());
	}

	public static DamageSource getRadioactiveDamage() {
		return compatDamage("DMG_RADIATION", getHeatDamage());
	}

	public static DamageSource getNukeExplosionDamage() {
		return compatDamage("DMG_NUKE_EXPLOSION", getHeatDamage());
	}

	private static DamageSource compatDamage(String aFieldName, DamageSource aFallback) {
		try {
			Object tDamage = Class.forName("ic2.api.info.Info").getField(aFieldName).get(null);
			if (tDamage instanceof DamageSource) return (DamageSource)tDamage;
		} catch(Throwable e) {/**/}
		return aFallback;
	}

	public static GregTechDamageSource getExplodingDamage() {return source(Kind.EXPLODING);}

	public static DamageSourceCombat getCombatDamage(String aType, LivingEntity aPlayer, Component aDeathMessage) {return getCombatDamage(aType, aPlayer, aDeathMessage, F);}
	public static DamageSourceCombat getCombatDamage(String aType, LivingEntity aPlayer, Component aDeathMessage, boolean aBeheading) {
		return new DamageSourceCombat(aType, aPlayer, aDeathMessage, aBeheading);
	}

	public static GregTechDamageSource getSpikeDamage() {return source(Kind.SPIKE);}
	public static GregTechDamageSource getShredderDamage() {return source(Kind.SHREDDER);}
	public static GregTechDamageSource getCrusherDamage() {return source(Kind.CRUSHER);}
	public static GregTechDamageSource getHeatDamage() {return source(Kind.HEAT);}
	public static GregTechDamageSource getFrostDamage() {return source(Kind.FROST);}
	public static GregTechDamageSource getChemDamage() {return source(Kind.CHEM);}
	public static GregTechDamageSource getBumbleDamage() {return source(Kind.BUMBLE);}
	public static GregTechDamageSource getAlcoholDamage() {return source(Kind.ALCOHOL);}
	public static GregTechDamageSource getCaffeineDamage() {return source(Kind.CAFFEINE);}
	public static GregTechDamageSource getDehydrationDamage() {return source(Kind.DEHYDRATION);}
	public static GregTechDamageSource getSugarDamage() {return source(Kind.SUGAR);}
	public static GregTechDamageSource getFatDamage() {return source(Kind.FAT);}

	static GregTechDamageSource source(Kind aKind) {
		return new GregTechDamageSource(aKind);
	}

	static DamageDefinition combatDefinition(String aType) {
		return new DamageDefinition(aType, ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MODID, "combat_" + sanitizePath(aType))), new DamageType(aType, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, DEFAULT_EXHAUSTION, DamageEffects.HURT), Set.of(), null);
	}

	public static Component getDeathMessage(LivingEntity aPlayer, Entity aEntity, String aMessage) {
		return getDeathMessage(aPlayer, aEntity, UT.Code.stringValidate(entityName(aPlayer), "Someone"), UT.Code.stringValidate(entityName(aEntity), "Someone"), aMessage);
	}

	public static Component getDeathMessage(LivingEntity aPlayer, Entity aEntity, String aNamePlayer, String aNameEntity, String aMessage) {
		if (UT.Code.stringInvalid(aNamePlayer) || UT.Code.stringInvalid(aEntity)) return Component.literal("Death Message lacks names of involved People");
		aNamePlayer = aNamePlayer.trim(); aNameEntity = aNameEntity.trim();
		if (aNamePlayer.equalsIgnoreCase("CrazyJ84") || aNamePlayer.equalsIgnoreCase("CrazyJ1984")) {
			if (aNameEntity.equalsIgnoreCase("Bear989jr")) return Component.literal("<"+ ChatFormatting.LIGHT_PURPLE+"Mrs. Crazy"+ChatFormatting.WHITE + "> Sorry "+ChatFormatting.RED+"Junior"+ChatFormatting.WHITE);
			if (aNameEntity.equalsIgnoreCase("Bear989Sr")) return Component.literal("<"+ChatFormatting.LIGHT_PURPLE+"Mrs. Crazy"+ChatFormatting.WHITE + "> Hush it!, "+ChatFormatting.RED+"Bear"+ChatFormatting.WHITE+"!");
		}
		if (aNamePlayer.equalsIgnoreCase("Bear989Sr") || aNamePlayer.equalsIgnoreCase("Bear989jr")) {
			//
		}

		if (UT.Code.stringValid(aMessage)) {
			return Component.literal(aMessage.replace("[KILLER]", ChatFormatting.GREEN+aNamePlayer+ChatFormatting.WHITE).replace("[VICTIM]", ChatFormatting.RED+aNameEntity+ChatFormatting.WHITE));
		} else if (aEntity instanceof LivingEntity) {
			return getCombatDamage(aPlayer instanceof Player ? "player" : "mob", aPlayer, null).getLocalizedDeathMessage((LivingEntity)aEntity);
		} else if (aEntity instanceof EnderDragonPart) {
			return getCombatDamage(aPlayer instanceof Player ? "player" : "mob", aPlayer, null).getLocalizedDeathMessage(((EnderDragonPart)aEntity).parentMob);
		}
		return Component.literal(ChatFormatting.GREEN+aNamePlayer+ChatFormatting.WHITE+" has killed "+ChatFormatting.RED+aNameEntity+ChatFormatting.WHITE);
	}

	private static Component literal(LivingEntity aTarget, String aSuffix) {
		return Component.literal(ChatFormatting.RED + entityName(aTarget) + ChatFormatting.WHITE + aSuffix);
	}

	private static String entityName(Entity aEntity) {
		return aEntity.getName().getString();
	}

	private static Set<TagKey<DamageType>> tags(boolean aBypassesArmor, boolean aAbsolute, boolean aCreative) {
		Set<TagKey<DamageType>> rTags = new LinkedHashSet<>();
		if (aBypassesArmor) {
			rTags.add(DamageTypeTags.BYPASSES_ARMOR);
			rTags.add(DamageTypeTags.BYPASSES_SHIELD);
		}
		if (aAbsolute) rTags.add(DamageTypeTags.BYPASSES_EFFECTS);
		if (aCreative) rTags.add(DamageTypeTags.BYPASSES_INVULNERABILITY);
		return Set.copyOf(rTags);
	}

	private static String sanitizePath(String aPath) {
		if (aPath == null || aPath.isEmpty()) return "generic";
		StringBuilder rPath = new StringBuilder();
		for (int i = 0; i < aPath.length(); i++) {
			char tChar = Character.toLowerCase(aPath.charAt(i));
			rPath.append((tChar >= 'a' && tChar <= 'z') || (tChar >= '0' && tChar <= '9') || tChar == '_' || tChar == '-' || tChar == '.' ? tChar : '_');
		}
		return rPath.length() <= 0 ? "generic" : rPath.toString();
	}

	static final class DamageDefinition {
		final String mMsgId;
		final ResourceKey<DamageType> mKey;
		final DamageType mType;
		final Set<TagKey<DamageType>> mTags;
		final Function<LivingEntity, Component> mDeathMessage;

		DamageDefinition(String aMsgId, ResourceKey<DamageType> aKey, DamageType aType, Set<TagKey<DamageType>> aTags, Function<LivingEntity, Component> aDeathMessage) {
			mMsgId = aMsgId;
			mKey = aKey;
			mType = aType;
			mTags = aTags;
			mDeathMessage = aDeathMessage;
		}
	}

	private static final class TaggedDamageTypeHolder implements Holder<DamageType> {
		private final ResourceKey<DamageType> mKey;
		private final DamageType mType;
		private final Set<TagKey<DamageType>> mTags = new LinkedHashSet<>();

		TaggedDamageTypeHolder(DamageSources.Kind aKind) {
			this(aKind.mKey, aKind.mType, aKind.mTags);
		}

		TaggedDamageTypeHolder(DamageDefinition aDefinition) {
			this(aDefinition.mKey, aDefinition.mType, aDefinition.mTags);
		}

		private TaggedDamageTypeHolder(ResourceKey<DamageType> aKey, DamageType aType, Set<TagKey<DamageType>> aTags) {
			mKey = aKey;
			mType = aType;
			mTags.addAll(aTags);
		}

		void add(TagKey<DamageType> aTag) {
			mTags.add(aTag);
			if (aTag == DamageTypeTags.BYPASSES_ARMOR) mTags.add(DamageTypeTags.BYPASSES_SHIELD);
		}

		@Override public DamageType value() {return mType;}
		@Override public boolean isBound() {return T;}
		@Override public boolean areComponentsBound() {return T;}
		@Override public boolean is(Identifier aKey) {return mKey.identifier().equals(aKey);}
		@Override public boolean is(ResourceKey<DamageType> aKey) {return mKey == aKey || (mKey.registry().equals(aKey.registry()) && mKey.identifier().equals(aKey.identifier()));}
		@Override public boolean is(Predicate<ResourceKey<DamageType>> aPredicate) {return aPredicate.test(mKey);}
		@Override public boolean is(TagKey<DamageType> aTag) {return mTags.contains(aTag);}
		@Override public boolean is(Holder<DamageType> aHolder) {return aHolder.is(mKey);}
		@Override public Stream<TagKey<DamageType>> tags() {return mTags.stream();}
		@Override public DataComponentMap components() {return DataComponentMap.EMPTY;}
		@Override public Either<ResourceKey<DamageType>, DamageType> unwrap() {return Either.left(mKey);}
		@Override public Optional<ResourceKey<DamageType>> unwrapKey() {return Optional.of(mKey);}
		@Override public Holder.Kind kind() {return Holder.Kind.REFERENCE;}
		@Override public boolean canSerializeIn(HolderOwner<DamageType> aRegistry) {return T;}
	}

	public static class GregTechDamageSource extends DamageSource {
		private final TaggedDamageTypeHolder mHolder;
		private final Function<LivingEntity, Component> mDeathMessage;
		private float mFoodExhaustion;

		protected GregTechDamageSource(DamageSources.Kind aKind) {
			this(new TaggedDamageTypeHolder(aKind), aKind.mTags.isEmpty() ? DEFAULT_EXHAUSTION : ZERO_EXHAUSTION, aKind.mDeathMessage, null, null);
		}

		protected GregTechDamageSource(DamageDefinition aDefinition, Entity aCausingEntity) {
			this(new TaggedDamageTypeHolder(aDefinition), DEFAULT_EXHAUSTION, aDefinition.mDeathMessage, aCausingEntity, aCausingEntity);
		}

		private GregTechDamageSource(TaggedDamageTypeHolder aHolder, float aFoodExhaustion, Function<LivingEntity, Component> aDeathMessage, Entity aDirectEntity, Entity aCausingEntity) {
			super(aHolder, aDirectEntity, aCausingEntity);
			mHolder = aHolder;
			mFoodExhaustion = aFoodExhaustion;
			mDeathMessage = aDeathMessage;
		}

		@Override public float getFoodExhaustion() {return mFoodExhaustion;}

		@Override
		public Component getLocalizedDeathMessage(LivingEntity aTarget) {
			return mDeathMessage == null ? super.getLocalizedDeathMessage(aTarget) : mDeathMessage.apply(aTarget);
		}

		public Component func_151519_b(LivingEntity aTarget) {return getLocalizedDeathMessage(aTarget);}

		public GregTechDamageSource setDamageBypassesArmor() {
			mHolder.add(DamageTypeTags.BYPASSES_ARMOR);
			mFoodExhaustion = ZERO_EXHAUSTION;
			return this;
		}

		public GregTechDamageSource setDamageIsAbsolute() {
			mHolder.add(DamageTypeTags.BYPASSES_EFFECTS);
			mFoodExhaustion = ZERO_EXHAUSTION;
			return this;
		}

		public GregTechDamageSource setDamageAllowedInCreativeMode() {
			mHolder.add(DamageTypeTags.BYPASSES_INVULNERABILITY);
			return this;
		}

		public GregTechDamageSource setFireDamage() {
			mHolder.add(DamageTypeTags.IS_FIRE);
			return this;
		}

		public GregTechDamageSource setProjectile() {
			mHolder.add(DamageTypeTags.IS_PROJECTILE);
			return this;
		}

		public GregTechDamageSource setExplosion() {
			mHolder.add(DamageTypeTags.IS_EXPLOSION);
			return this;
		}

		public boolean isUnblockable() {return is(DamageTypeTags.BYPASSES_ARMOR);}
		public boolean isDamageAbsolute() {return is(DamageTypeTags.BYPASSES_EFFECTS);}
		public boolean canHarmInCreative() {return is(DamageTypeTags.BYPASSES_INVULNERABILITY);}
		public boolean isFireDamage() {return is(DamageTypeTags.IS_FIRE);}
		public boolean isProjectile() {return is(DamageTypeTags.IS_PROJECTILE);}
		public boolean isExplosion() {return is(DamageTypeTags.IS_EXPLOSION);}
		public float getHungerDamage() {return getFoodExhaustion();}
		public String getDamageType() {return getMsgId();}
	}
}
