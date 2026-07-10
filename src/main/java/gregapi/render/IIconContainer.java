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
 */

package gregapi.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.resources.Identifier;

/** 
 * @author Gregorius Techneticies
 */
public interface IIconContainer {
	/**
	 * @return A regular Icon.
	 */
	@OnlyIn(Dist.CLIENT)
	public IIcon getIcon(int aRenderPass);
	
	/**
	 * @return if this Render Pass uses Color Modulation.
	 */
	@OnlyIn(Dist.CLIENT)
	public boolean isUsingColorModulation(int aRenderPass);
	
	/**
	 * @return the Color Modulation of the Icon.
	 */
	@OnlyIn(Dist.CLIENT)
	public short[] getIconColor(int aRenderPass);
	
	/**
	 * @return the Amount of Render Passes for this Icon.
	 */
	@OnlyIn(Dist.CLIENT)
	public int getIconPasses();
	
	/**
	 * @return the Default Texture File for this Icon.
	 */
	@OnlyIn(Dist.CLIENT)
	public Identifier getTextureFile();
	
	/**
	 * Registers the Icon of this IconContainer.
	 */
	@OnlyIn(Dist.CLIENT)
	public void registerIcons(IIconRegister aIconRegister);
}
