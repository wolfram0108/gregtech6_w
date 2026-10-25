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

package gregapi.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Carries foreign (vanilla/modded) block-drop handling; the rule itself lives in one place (GT_API_Proxy),
 *  this is only delivery, since this version has no drop-list event -- the global loot modifier is the only hook. */
public class GT6BlockDropsModifier extends LootModifier {
	public static final Codec<GT6BlockDropsModifier> CODEC = RecordCodecBuilder.create(aInstance -> codecStart(aInstance).apply(aInstance, GT6BlockDropsModifier::new));

	/** The mod's only global-loot-modifier serializer registry, on the same mod bus as the rest of the centers. */
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, gregapi.data.MD.GT.mID);
	public static final net.minecraftforge.registries.RegistryObject<Codec<GT6BlockDropsModifier>> TYPE = SERIALIZERS.register("block_drops", () -> CODEC);

	public static void register(IEventBus aModBus) {SERIALIZERS.register(aModBus);}

	public GT6BlockDropsModifier(LootItemCondition[] aConditions) {super(aConditions);}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> aLoot, LootContext aContext) {
		BlockState tState = aContext.getParamOrNull(LootContextParams.BLOCK_STATE);
		if (tState == null) return aLoot;
		net.minecraft.world.phys.Vec3 tOrigin = aContext.getParamOrNull(LootContextParams.ORIGIN);
		if (tOrigin == null) return aLoot;
		gregapi.GT_API_Proxy.processBlockDrops(aLoot, aContext.getLevel(), BlockPos.containing(tOrigin), tState, aContext.getParamOrNull(LootContextParams.THIS_ENTITY));
		return aLoot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {return CODEC;}
}
