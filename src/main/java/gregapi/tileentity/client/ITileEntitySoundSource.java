/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.tileentity.client;

import static gregapi.data.CS.*;

import net.minecraftforge.api.distmarker.Dist;
import gregapi.random.IHasWorldAndCoords;
import gregapi.tileentity.ITileEntityUnloadable;
// 1.7.10's ISound/ITickableSound getter interfaces are gone; neo models sounds as protected fields on
// AbstractTickableSoundInstance, so only tick() needs implementing.
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Gregorius Techneticies
 */
public interface ITileEntitySoundSource extends ITileEntityUnloadable {
	public void startSound();

	public void stopSound();

	public static class SoundSourceTileEntity extends AbstractTickableSoundInstance {
		public boolean mRunning = F;
		public float mSoundStrength, mSoundModulation;
		public final IHasWorldAndCoords mTileEntity;

		public SoundSourceTileEntity(IHasWorldAndCoords aTileEntity, boolean aRunning, String aSoundName, float aSoundStrength, float aSoundModulation) {
			// SoundEvent built from a resource string with source BLOCKS; legacy 1.7.10 SFX names map to neo sound ids through the
			// checked UT.Sounds.neoSound table.
			super(SoundEvent.createVariableRangeEvent(new ResourceLocation(gregapi.util.UT.Sounds.neoSound(aSoundName))), SoundSource.BLOCKS, RandomSource.create());
			mTileEntity = aTileEntity;
			mRunning = aRunning;
			mSoundStrength = aSoundStrength;
			mSoundModulation = aSoundModulation;
			// 1.7.10's volume/pitch/repeat/attenuation/position getters are now just protected fields set directly.
			this.volume = aSoundStrength;
			this.pitch = aSoundModulation;
			this.looping = aRunning;
			this.delay = 1;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			this.x = mTileEntity == null ? 0 : mTileEntity.getX()+0.5D;
			this.y = mTileEntity == null ? 0 : mTileEntity.getY()+0.5D;
			this.z = mTileEntity == null ? 0 : mTileEntity.getZ()+0.5D;
		}

		@Override
		public void tick() {
			// 1.7.10's getters read live mutable fields; neo reads protected fields every frame instead, so they're synced from GT6
			// state and muted once the machine turns off.
			this.volume = mSoundStrength;
			this.pitch = mSoundModulation;
			this.looping = mRunning;
			if (!mRunning) stop();
		}
	}
}
