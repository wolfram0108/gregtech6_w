/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.tileentity.energy;
import gregapi.util.WD;

import gregapi.code.TagData;
import gregapi.data.MD;
import gregapi.data.TD;
import gregapi.random.ExplosionGT;
import gregapi.util.UT;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * For mostly Internal Use.
 */
public class EnergyCompat {
	// AE2 26.1's network only accepts FE, and the class this flag guarded (a direct EU sink AE2 rv2 implemented
	// itself) no longer exists, so the branch is simply removed; GT6's FE bridge lives elsewhere.
	public static boolean RF_ENERGY = F, RF_ENERGY_NEW = F, FL_ENERGY = F, WD_ENERGY = F, IC_ENERGY = F, BB_ENERGY = F, GC_ENERGY = F, BC_LASER = F, XM_ROTATION = F;
	
	/** Gets Called once during postInit to see which Interfaces are there and Classloaded. */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void checkAvailabilities() {
		// A gt6mirror type ships inside GT6 and always loads, so a class probe alone would report every mod present;
		// each probe therefore runs only when the mod list holds the owner of that API.
		if (MD.FUNK.mLoaded) try {
			gt6mirror.com.rwtema.funkylocomotion.blocks.TilePusher                 .class.getCanonicalName();
			gt6mirror.com.rwtema.funkylocomotion.blocks.TileBooster                .class.getCanonicalName();
			FL_ENERGY = T;
		} catch(Throwable e) {/**/}
		if (MD.WARPDRIVE.mLoaded) try {
			gt6mirror.cr0s.warpdrive.block.TileEntityAbstractEnergy                .class.getCanonicalName();
			WD_ENERGY = T;
		} catch(Throwable e) {/**/}
		if (MD.COFH_API_ENERGY.mLoaded) try {
			gt6mirror.cofh.api.energy.IEnergyHandler                               .class.getCanonicalName();
			gt6mirror.cofh.api.energy.IEnergyConnection                            .class.getCanonicalName();
			RF_ENERGY = T;
			// Some Mods do not include this File, due to badly referencing old RF-API stuff, so this gets a separate Boolean now.
			gt6mirror.cofh.api.energy.IEnergyReceiver                              .class.getCanonicalName();
			RF_ENERGY_NEW = T;
		} catch(Throwable e) {/**/}
		if (MD.IC2.mLoaded) try {
			gt6mirror.ic2.api.energy.tile.IEnergyTile                              .class.getCanonicalName();
			gt6mirror.ic2.api.energy.tile.IEnergySink                              .class.getCanonicalName();
			gt6mirror.ic2.api.energy.tile.IEnergySource                            .class.getCanonicalName();
			gt6mirror.ic2.api.energy.tile.IEnergyConductor                         .class.getCanonicalName();
			IC_ENERGY = T;
		} catch(Throwable e) {/**/}
		if (MD.VOLTZ.mLoaded) try {
			gt6mirror.com.builtbroken.mc.api.energy.IEnergyBufferProvider          .class.getCanonicalName();
			gt6mirror.com.builtbroken.mc.api.energy.IEnergyBuffer                  .class.getCanonicalName();
			BB_ENERGY = T;
		} catch(Throwable e) {/**/}
		if (MD.GC.mLoaded) try {
			gt6mirror.micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC      .class.getCanonicalName();
			gt6mirror.micdoodle8.mods.galacticraft.api.power.EnergySource          .class.getCanonicalName();
			gt6mirror.micdoodle8.mods.galacticraft.api.transmission.tile.IConnector.class.getCanonicalName();
			gt6mirror.micdoodle8.mods.galacticraft.api.transmission.NetworkType    .class.getCanonicalName();
			gt6mirror.micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler .class.getCanonicalName();
			GC_ENERGY = T;
		} catch(Throwable e) {/**/}
		if (MD.BC.mLoaded) try {
			gt6mirror.buildcraft.api.power.ILaserTarget                            .class.getCanonicalName();
			BC_LASER = T;
		} catch(Throwable e) {/**/}
		try {
			net.commoble.exmachina.api.ExMachinaRegistries               .class.getCanonicalName();
			net.commoble.exmachina.api.MechanicalNodeStates              .class.getCanonicalName();
			net.commoble.exmachina.api.MechanicalState                   .class.getCanonicalName();
			net.commoble.exmachina.api.NodeShape                         .class.getCanonicalName();
			XM_ROTATION = T;
		} catch(Throwable e) {/**/}
	}
	
	public static boolean isElectricRFReceiver(BlockEntity aReceiver) {
		if (aReceiver == null) return F;
		String tClass = null;
		if (MD.OMT .mLoaded) {                    tClass = aReceiver.getClass().getName(); if (tClass.startsWith("openmodularturrets"             )) return T;}
		if (MD.IE  .mLoaded) {if (tClass == null) tClass = aReceiver.getClass().getName(); if (tClass.startsWith("blusunrize.immersiveengineering")) return T;}
		if (MD.OC  .mLoaded) {if (tClass == null) tClass = aReceiver.getClass().getName(); if (tClass.startsWith("li.cil.oc"                      )) return T;}
		if (MD.TG  .mLoaded) {if (tClass == null) tClass = aReceiver.getClass().getName(); if (tClass.startsWith("techguns"                       )) return T;}
		return F;
	}
	
	/** Rotation, asked by the Axles the same way the electric Wires ask canConnectElectricity. */
	public static boolean canConnectRotation(BlockEntity aThis, BlockEntity aTarget, byte aSide) {
		if (aTarget == null) return F;
		if (aTarget instanceof ITileEntityEnergy) return ((ITileEntityEnergy)aTarget).isEnergyAcceptingFrom(TD.Energy.RU, aSide, T) || ((ITileEntityEnergy)aTarget).isEnergyEmittingTo(TD.Energy.RU, aSide, T);
		return isMechanical(aTarget);
	}

	/** A foreign Block only turns if some Mod put it into the mechanical Graph, so that Registry is the Answer. */
	public static boolean isMechanical(BlockEntity aTarget) {
		if (!XM_ROTATION || aTarget == null || aTarget.getLevel() == null) return F;
		try {
			net.minecraft.resources.Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aTarget.getBlockState().getBlock());
			return tKey != null && aTarget.getLevel().registryAccess().lookupOrThrow(net.commoble.exmachina.api.ExMachinaRegistries.MECHANICAL_COMPONENT).containsKey(tKey);
		} catch(Throwable e) {return F;}
	}

	@SuppressWarnings("deprecation")
	public static boolean canConnectElectricity(BlockEntity aThis, BlockEntity aTarget, byte aSide) {
		if (aTarget == null) return F;
		if (aTarget instanceof ITileEntityEnergy                                  ) return ((ITileEntityEnergy                   )aTarget).isEnergyAcceptingFrom(TD.Energy.EU, aSide, T) || ((ITileEntityEnergy                   )aTarget).isEnergyEmittingTo(TD.Energy.EU, aSide, T);
		if (aTarget instanceof gregapi.tileentity.ITileEntityEnergy               ) return ((gregapi.tileentity.ITileEntityEnergy)aTarget).isEnergyAcceptingFrom(TD.Energy.EU, aSide, T) || ((gregapi.tileentity.ITileEntityEnergy)aTarget).isEnergyEmittingTo(TD.Energy.EU, aSide, T);
		// IMPORTANT: Ignore the Fact that this SEEMS to be unused. It does exist, SOMETIMES.
		if (aTarget instanceof gregtech.api.interfaces.tileentity.IEnergyConnected) return ((gregtech.api.interfaces.tileentity.IEnergyConnected)aTarget).inputEnergyFrom(aSide) || ((gregtech.api.interfaces.tileentity.IEnergyConnected)aTarget).outputsEnergyTo(aSide);
		
		// Branch removed for the same reason as the field above: its carrier class no longer exists.

		if (FL_ENERGY && (aTarget instanceof gt6mirror.com.rwtema.funkylocomotion.blocks.TilePusher || aTarget instanceof gt6mirror.com.rwtema.funkylocomotion.blocks.TileBooster)) return T;
		
		if (WD_ENERGY &&  aTarget instanceof gt6mirror.cr0s.warpdrive.block.TileEntityAbstractEnergy) return ((gt6mirror.cr0s.warpdrive.block.TileEntityAbstractEnergy)aTarget).energy_canInput(FORGE_DIR[aSide]);
		
		if (GC_ENERGY &&  aTarget instanceof gt6mirror.micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC && (!(aTarget instanceof gt6mirror.micdoodle8.mods.galacticraft.api.transmission.tile.IConnector) || ((gt6mirror.micdoodle8.mods.galacticraft.api.transmission.tile.IConnector)aTarget).canConnect(FORGE_DIR[aSide], gt6mirror.micdoodle8.mods.galacticraft.api.transmission.NetworkType.POWER))) return T;
		
		if (BB_ENERGY &&  aTarget instanceof gt6mirror.com.builtbroken.mc.api.energy.IEnergyBufferProvider && ((gt6mirror.com.builtbroken.mc.api.energy.IEnergyBufferProvider)aTarget).getEnergyBuffer(FORGE_DIR[aSide]) != null) return T;
		
		if (IC_ENERGY) {
			BlockEntity tConnected = (aTarget instanceof gt6mirror.ic2.api.energy.tile.IEnergyTile || gt6mirror.ic2.api.energy.EnergyNet.instance == null ? aTarget : gt6mirror.ic2.api.energy.EnergyNet.instance.getTileEntity(aTarget.getLevel(), aTarget.getBlockPos().getX(), aTarget.getBlockPos().getY(), aTarget.getBlockPos().getZ()));
			if (tConnected instanceof gt6mirror.ic2.api.energy.tile.IEnergySink   && (aThis == null || ((gt6mirror.ic2.api.energy.tile.IEnergySink  )tConnected).acceptsEnergyFrom(aThis, FORGE_DIR[aSide]))) return T;
			if (tConnected instanceof gt6mirror.ic2.api.energy.tile.IEnergySource && (aThis == null || ((gt6mirror.ic2.api.energy.tile.IEnergySource)tConnected).emitsEnergyTo    (aThis, FORGE_DIR[aSide]))) return T;
		}
		
		// IMPORTANT: Ignore the Fact that IEnergyConnection is SUPPOSEDLY part of IEnergyHandler. There is versions of the RF API in circulation, where this is NOT the case!!!
		if (RF_ENERGY && (EMIT_EU_AS_RF || isElectricRFReceiver(aTarget)) && (aTarget instanceof gt6mirror.cofh.api.energy.IEnergyHandler || (RF_ENERGY_NEW && aTarget instanceof gt6mirror.cofh.api.energy.IEnergyReceiver))) return !(aTarget instanceof gt6mirror.cofh.api.energy.IEnergyConnection) || ((gt6mirror.cofh.api.energy.IEnergyConnection)aTarget).canConnectEnergy(FORGE_DIR[aSide]);

		// The RF-arm's carrier API doesn't exist in 26.1, so RF_ENERGY is always false; its meaning moves to the
		// engine's own Capabilities.Energy.BLOCK, which AE2's Energy Acceptor and every other FE receiver already implement.
		if ((EMIT_EU_AS_RF || isElectricRFReceiver(aTarget)) && feHandler(aTarget, aSide) != null) return T;

		return F;
	}

	/** Neighbor's FE receiver capability on a given side, the one place in the mod that asks it, for both the connection
	 *  predicate and energy insertion. */
	public static net.neoforged.neoforge.transfer.energy.EnergyHandler feHandler(BlockEntity aReceiver, byte aSide) {
		if (aReceiver == null || aReceiver.getLevel() == null) return null;
		try {
			return net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK.getCapability(aReceiver.getLevel(), aReceiver.getBlockPos(), null, aReceiver, FORGE_DIR[aSide]);
		} catch(Throwable e) {return null;}
	}
	
	public static boolean checkOverCharge(long aSize, BlockEntity aReceiver) {
		if (aSize > VMAX[3]) {
			Level tWorld = aReceiver.getLevel();
			WD.set(tWorld, aReceiver.getBlockPos().getX(), aReceiver.getBlockPos().getY(), aReceiver.getBlockPos().getZ(), NB, 0, 3);
			ExplosionGT.explode(tWorld, null, aReceiver.getBlockPos().getX()+0.5, aReceiver.getBlockPos().getY()+0.5, aReceiver.getBlockPos().getZ()+0.5, 5, F, T);
			return T;
		}
		return F;
	}
	
	@SuppressWarnings("deprecation")
	public static long insertEnergyInto(TagData aEnergyType, byte aSide, long aSize, long aAmount, Object aEmitter, BlockEntity aReceiver) {
		if (aAmount <= 0 || aSize == 0 || aReceiver == null) return 0;
		// Obvious GT6 Blocks should not be eligible for Compat. Should reduce some IC2 Compat Lag.
		if (aReceiver instanceof gregapi.tileentity.base.TileEntityBase01Root) return 0;
		
		// Foreign Machines read their Node State and nothing else, so that is where the Rotation has to go.
		if (aEnergyType == TD.Energy.RU) {
			if (!isMechanical(aReceiver)) return 0;
			try {
				java.util.Map<net.commoble.exmachina.api.NodeShape, net.commoble.exmachina.api.MechanicalState> tStates = aReceiver.getData(net.commoble.exmachina.api.MechanicalNodeStates.HOLDER.get());
				// The Windmills own the whole Block while the Plates own one Face, so write into the Node that is there.
				net.commoble.exmachina.api.NodeShape tFace = net.commoble.exmachina.api.NodeShape.ofSide(FORGE_DIR[aSide]), tCube = net.commoble.exmachina.api.NodeShape.ofCube();
				net.commoble.exmachina.api.NodeShape tShape = !tStates.containsKey(tFace) && tStates.containsKey(tCube) ? tCube : tFace;
				net.commoble.exmachina.api.MechanicalState tState = new net.commoble.exmachina.api.MechanicalState(Math.abs(aSize * aAmount), aSize * RAD_PER_RU);
				if (!tState.equals(tStates.put(tShape, tState))) {
					aReceiver.setChanged();
					aReceiver.syncData(net.commoble.exmachina.api.MechanicalNodeStates.HOLDER.get());
				}
				return aAmount;
			} catch(Throwable e) {return 0;}
		}
		
		if (aEnergyType == TD.Energy.EU) {
			// Nothing here needs the Negative Part of this, so it's gonna be skipped.
			aSize = Math.abs(aSize);
			
			// Applied Energistics gets a special case.
			// AE2's own EU receiver from 1.7.10 is gone, so the real FE exit lives here, at the engine cap level, reaching any FE
			// receiver, not just AE2's Energy Acceptor; the rate is the mod's own RF_PER_EU=4 against AE2's 0.5, giving 1 EU = 2 AE.
			if (EMIT_EU_AS_RF || isElectricRFReceiver(aReceiver)) {
				net.neoforged.neoforge.transfer.energy.EnergyHandler tFE = feHandler(aReceiver, aSide);
				if (tFE != null) {
					if (checkOverCharge(aSize, aReceiver)) return aAmount;
					long tWanted = aAmount * aSize * RF_PER_EU;
					try (net.neoforged.neoforge.transfer.transaction.Transaction tTx = net.neoforged.neoforge.transfer.transaction.Transaction.open(net.neoforged.neoforge.transfer.transaction.Transaction.getCurrentOpenedTransaction())) {
						int tAccepted = tFE.insert(UT.Code.bind31(tWanted), tTx);
						// aEmitter here can be anyone; the transaction commits only once something was actually accepted for real.
						if (tAccepted > 0) tTx.commit();
						return UT.Code.divup(tAccepted, aSize * RF_PER_EU);
					} catch(Throwable e) {return 0;}
				}
			}

			// Funky Locomotion includes the OLD RF-API that it does not even use, while also using NEWER parts of the RF API that it does not include... This sort of utter Bullshit makes RF-Mods incompatible with each other...
			if (FL_ENERGY) {
				if (aReceiver instanceof gt6mirror.com.rwtema.funkylocomotion.blocks.TilePusher ) return checkOverCharge(aSize, aReceiver) ? aAmount : UT.Code.divup(((gt6mirror.com.rwtema.funkylocomotion.blocks.TilePusher )aReceiver).receiveEnergy(FORGE_DIR[aSide], UT.Code.bind31(aAmount * aSize * RF_PER_EU * 10), F), aSize * RF_PER_EU * 10);
				if (aReceiver instanceof gt6mirror.com.rwtema.funkylocomotion.blocks.TileBooster) return checkOverCharge(aSize, aReceiver) ? aAmount : UT.Code.divup(((gt6mirror.com.rwtema.funkylocomotion.blocks.TileBooster)aReceiver).receiveEnergy(FORGE_DIR[aSide], UT.Code.bind31(aAmount * aSize * RF_PER_EU * 10), F), aSize * RF_PER_EU * 10);
			}
			
			// WarpDrive does not include ANY of the APIs it uses inside its Jar, which is a good thing, but it does force me to do this special case...
			if (WD_ENERGY) {
				if (aReceiver instanceof gt6mirror.cr0s.warpdrive.block.TileEntityAbstractEnergy) {
					if (((gt6mirror.cr0s.warpdrive.block.TileEntityAbstractEnergy)aReceiver).energy_getEnergyStored() >= ((gt6mirror.cr0s.warpdrive.block.TileEntityAbstractEnergy)aReceiver).energy_getMaxStorage()) return 0;
					if (checkOverCharge(aSize, aReceiver)) return aAmount;
					// I love how this does not have any sanity checks, and not even a boolean to check if it worked XD
					((gt6mirror.cr0s.warpdrive.block.TileEntityAbstractEnergy)aReceiver).energy_consume(-aSize);
					return 1;
				}
			}
			
			// GalactiCraft and its Addons
			if (GC_ENERGY && COMPAT_GC != null) {
				if (aReceiver instanceof gt6mirror.micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC && !(RF_ENERGY && isElectricRFReceiver(aReceiver))) {
					if (!(aReceiver instanceof gt6mirror.micdoodle8.mods.galacticraft.api.transmission.tile.IConnector) || ((gt6mirror.micdoodle8.mods.galacticraft.api.transmission.tile.IConnector)aReceiver).canConnect(FORGE_DIR[aSide], gt6mirror.micdoodle8.mods.galacticraft.api.transmission.NetworkType.POWER)) {
						if (checkOverCharge(aSize, aReceiver)) return aAmount;
						float tSizeToReceive = aSize * gt6mirror.micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler.IC2_RATIO, tStored = ((gt6mirror.micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC)aReceiver).getEnergyStoredGC((gt6mirror.micdoodle8.mods.galacticraft.api.power.EnergySource)COMPAT_GC.dir(aSide));
						if (tSizeToReceive >= tStored || tSizeToReceive <= ((gt6mirror.micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC)aReceiver).getMaxEnergyStoredGC((gt6mirror.micdoodle8.mods.galacticraft.api.power.EnergySource)COMPAT_GC.dir(aSide)) - tStored) {
							float tReceived = ((gt6mirror.micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC)aReceiver).receiveEnergyGC((gt6mirror.micdoodle8.mods.galacticraft.api.power.EnergySource)COMPAT_GC.dir(aSide), tSizeToReceive, F);
							if (tReceived > 0) {
								tSizeToReceive -= tReceived;
								while (tSizeToReceive > 0) {
									tReceived = ((gt6mirror.micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC)aReceiver).receiveEnergyGC((gt6mirror.micdoodle8.mods.galacticraft.api.power.EnergySource)COMPAT_GC.dir(aSide), tSizeToReceive, F);
									if (tReceived < 1) break;
									tSizeToReceive -= tReceived;
								}
								return 1;
							}
						}
					}
					return 0;
				}
			}
			
			// Voltz Stuff
			if (BB_ENERGY && aReceiver instanceof gt6mirror.com.builtbroken.mc.api.energy.IEnergyBufferProvider) {
				Object tEnergyBuffer = ((gt6mirror.com.builtbroken.mc.api.energy.IEnergyBufferProvider)aReceiver).getEnergyBuffer(FORGE_DIR[aSide]);
				if (tEnergyBuffer != null) {
					//noinspection CastCanBeRemovedNarrowingVariableType
					return checkOverCharge(aSize, aReceiver) ? aAmount : UT.Code.divup(((gt6mirror.com.builtbroken.mc.api.energy.IEnergyBuffer)tEnergyBuffer).addEnergyToStorage(UT.Code.bind31(aSize * aAmount) * J_PER_EU, T), aSize * J_PER_EU);
				}
			}
			
			// Electricity alike RF Receivers that are whitelisted for my Power System.
			if (RF_ENERGY && (EMIT_EU_AS_RF || isElectricRFReceiver(aReceiver))) {
				if (!(aReceiver instanceof gt6mirror.cofh.api.energy.IEnergyConnection) || ((gt6mirror.cofh.api.energy.IEnergyConnection)aReceiver).canConnectEnergy(FORGE_DIR[aSide])) {
					if (RF_ENERGY_NEW && aReceiver instanceof gt6mirror.cofh.api.energy.IEnergyReceiver) return checkOverCharge(aSize, aReceiver) ? aAmount : UT.Code.divup(((gt6mirror.cofh.api.energy.IEnergyReceiver)aReceiver).receiveEnergy(FORGE_DIR[aSide], UT.Code.bind31(aAmount * aSize * RF_PER_EU), F), aSize * RF_PER_EU);
					if (                 aReceiver instanceof gt6mirror.cofh.api.energy.IEnergyHandler ) return checkOverCharge(aSize, aReceiver) ? aAmount : UT.Code.divup(((gt6mirror.cofh.api.energy.IEnergyHandler )aReceiver).receiveEnergy(FORGE_DIR[aSide], UT.Code.bind31(aAmount * aSize * RF_PER_EU), F), aSize * RF_PER_EU);
				}
				return 0;
			}
			
			// Since GT5U is still basically an IC2-Addon, I don't want the IC2 Power System to come before this by accident.
			if (aReceiver instanceof gregtech.api.interfaces.tileentity.IEnergyConnected) {
				return ((gregtech.api.interfaces.tileentity.IEnergyConnected)aReceiver).injectEnergyUnits(aSide, aSize, aAmount);
			}
			
			// IC2 Power at last, because special cases should always override the very "compatible" IC2 Stuff.
			if (IC_ENERGY) {
				BlockEntity tReceiver = (aReceiver instanceof gt6mirror.ic2.api.energy.tile.IEnergyTile || gt6mirror.ic2.api.energy.EnergyNet.instance == null ? aReceiver : gt6mirror.ic2.api.energy.EnergyNet.instance.getTileEntity(aReceiver.getLevel(), aReceiver.getBlockPos().getX(), aReceiver.getBlockPos().getY(), aReceiver.getBlockPos().getZ()));
				if (tReceiver instanceof gt6mirror.ic2.api.energy.tile.IEnergySink && ((gt6mirror.ic2.api.energy.tile.IEnergySink)tReceiver).acceptsEnergyFrom(aEmitter instanceof BlockEntity ? (BlockEntity)aEmitter : null, FORGE_DIR[aSide])) {
					long rUsedAmount = 0;
					while (aAmount > rUsedAmount && ((gt6mirror.ic2.api.energy.tile.IEnergySink)tReceiver).getDemandedEnergy() >= (rUsedAmount <= 0 && aSize <= VMAX[0] ? 4 : aSize) && ((gt6mirror.ic2.api.energy.tile.IEnergySink)tReceiver).injectEnergy(FORGE_DIR[aSide], aSize, aSize) < aSize) rUsedAmount++;
					if (rUsedAmount > 0) {
						int tTier = ((gt6mirror.ic2.api.energy.tile.IEnergySink)tReceiver).getSinkTier();
						if (tTier >= 0 && tTier < VMAX.length-1 && aSize > VMAX[tTier]) {
							Level tWorld = tReceiver.getLevel();
							WD.set(tWorld, tReceiver.getBlockPos().getX(), tReceiver.getBlockPos().getY(), tReceiver.getBlockPos().getZ(), NB, 0, 3);
							ExplosionGT.explode(tWorld, null, tReceiver.getBlockPos().getX()+0.5, tReceiver.getBlockPos().getY()+0.5, tReceiver.getBlockPos().getZ()+0.5, tTier+1, F, T);
							return aAmount;
						}
					}
					return rUsedAmount;
				}
			}
			
			// No need to check the rest since this isn't RF.
			return 0;
		}
		
		if (RF_ENERGY && aSize > 0) {
			long tSizeToReceive = 0;
			// GT KineticUnits auto-convert to RF, but only in the Push Phase, so when they are postive!
			if (aEnergyType == TD.Energy.KU) tSizeToReceive = aSize * RF_PER_EU; else
			// MJ auto-convert to RF too. And yes I do know that BuildCraft and other Mods moved away from MJ to use RF.
			if (aEnergyType == TD.Energy.MJ) tSizeToReceive = aSize * RF_PER_MJ; else
			// RF is RF and therefore doesn't really need to be converted, meaning a 1:1 Ratio
			if (aEnergyType == TD.Energy.RF) tSizeToReceive = aSize;
			
			if (tSizeToReceive > 0) {
				if (!(aReceiver instanceof gt6mirror.cofh.api.energy.IEnergyConnection) || ((gt6mirror.cofh.api.energy.IEnergyConnection)aReceiver).canConnectEnergy(FORGE_DIR[aSide])) {
					if (RF_ENERGY_NEW && aReceiver instanceof gt6mirror.cofh.api.energy.IEnergyReceiver) return UT.Code.divup(((gt6mirror.cofh.api.energy.IEnergyReceiver)aReceiver).receiveEnergy(FORGE_DIR[aSide], UT.Code.bind31(aAmount * tSizeToReceive), F), tSizeToReceive);
					if (                 aReceiver instanceof gt6mirror.cofh.api.energy.IEnergyHandler ) return UT.Code.divup(((gt6mirror.cofh.api.energy.IEnergyHandler )aReceiver).receiveEnergy(FORGE_DIR[aSide], UT.Code.bind31(aAmount * tSizeToReceive), F), tSizeToReceive);
				}
			}
		}
		return 0;
	}
}
