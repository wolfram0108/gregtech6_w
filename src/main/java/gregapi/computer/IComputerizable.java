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

package gregapi.computer;

import gregapi.tileentity.delegate.DelegatorTileEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * @author Gregorius Techneticies
 * 
 * Note: ALL the Methods can and will be called at any time by another Thread. Nothing here is synched to World Ticks.
 */
public interface IComputerizable {
	/** @return a unique name for this device. All lowercase without spaces. null if you don't want functionality. */
	public String       getComputerizableName       (DelegatorTileEntity<BlockEntity> aDelegator);
	/** @return an Array of all the things getComputerizableArgs can output. */
	public String[]     allComputerizableArgs       (DelegatorTileEntity<BlockEntity> aDelegator);
	/** @return an Array of all the things getComputerizableHelp can output. */
	public String[]     allComputerizableHelps      (DelegatorTileEntity<BlockEntity> aDelegator);
	/** @return an Array of all the things getComputerizableMethod can output. */
	public String[]     allComputerizableMethods    (DelegatorTileEntity<BlockEntity> aDelegator);
	/** @return an Array of all the things getComputerizableReturn can output. */
	public Class<?>[]   allComputerizableReturns    (DelegatorTileEntity<BlockEntity> aDelegator);
	/** @return a String Description of the Arguments you are expecting. */
	public String       getComputerizableArgs       (DelegatorTileEntity<BlockEntity> aDelegator, int aFunctionIndex);
	/** @return a String Description of the Function itself. */
	public String       getComputerizableHelp       (DelegatorTileEntity<BlockEntity> aDelegator, int aFunctionIndex);
	/** @return a String Name of the Function at this Index. */
	public String       getComputerizableMethod     (DelegatorTileEntity<BlockEntity> aDelegator, int aFunctionIndex);
	/** @return a Class of the Return Type to expect. */
	public Class<?>     getComputerizableReturn     (DelegatorTileEntity<BlockEntity> aDelegator, int aFunctionIndex);
	/** The Function Call itself. The TileEntity implementing IComputerizable is always the one inside aDelegator. aDelegator is only there for Singletons like Covers, that require more Data access. */
	public Object[]     callComputerizableMethod    (DelegatorTileEntity<BlockEntity> aDelegator, int aFunctionIndex, Object[] aArguments);
}
