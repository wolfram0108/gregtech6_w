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


package gregapi.enchants;

import gregapi.data.MD;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The only place in the mod that registers GT6's 4 enchants into ForgeRegistries.ENCHANTMENTS; on 1.20.1 they're ordinary
 *  imperative registry objects again, so cost/levels/slots/effect assembly moved back into each enchant class. */
public class EnchantsGT6 {

	/** The only place in the mod that writes to the enchantment registry. */
	private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MD.GAPI.mID);

	public static final RegistryObject<Enchantment> WEREBANE      = ENCHANTMENTS.register("werebane"     , () -> Enchantment_WerewolfDamage.INSTANCE);
	public static final RegistryObject<Enchantment> DISSOLVING    = ENCHANTMENTS.register("dissolving"   , () -> Enchantment_SlimeDamage   .INSTANCE);
	public static final RegistryObject<Enchantment> DISJUNCTION   = ENCHANTMENTS.register("disjunction"  , () -> Enchantment_EnderDamage   .INSTANCE);
	public static final RegistryObject<Enchantment> RADIOACTIVITY = ENCHANTMENTS.register("radioactivity", () -> Enchantment_Radioactivity .INSTANCE);

	/** The central enchantment subscription point, called once from GT_API on the same mod bus. */
	public static void register(IEventBus aModBus) {
		ENCHANTMENTS.register(aModBus);
	}
}
