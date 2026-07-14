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

package gregapi.old;
import gregapi.util.WD;


public class GT_Spray_Ice_Item extends GT_Tool_Item {
	public GT_Spray_Ice_Item(String aUnlocalized, String aEnglish, int aMaxDamage, int aEntityDamage) {
		super(aUnlocalized, aEnglish, "Very effective against Slimes", aMaxDamage, aEntityDamage, true);/*
		addToEffectiveList(Slime.class.getName());
		addToEffectiveList("BlueSlime");
		addToEffectiveList("SlimeClone");
		addToEffectiveList("MetalSlime");
		addToEffectiveList("EntityTFFireBeetle");
		addToEffectiveList("EntityTFMazeSlime");
		addToEffectiveList("EntityTFSlimeBeetle");
		setCraftingSound(SFX.IC_SPRAY);
		setBreakingSound(SFX.IC_SPRAY);
		setEntityHitSound(SFX.IC_SPRAY);
		setUsageAmounts(4, 16, 1);*/
		/*
		for (Object tName : Arrays.asList(UT.Stacks.make(Items.WATER_BUCKET, 1, W), OP.cell.dat(MT.Water), OP.capsule.dat(MT.Water))) {
			GT_ModHandler.addShapelessCraftingRecipe(UT.Stacks.make(Blocks.ICE, 1, 0), new Object[] {UT.Stacks.make(this, 1, W), tName});
		}*/
	}
	/*
	@Override
	public void onHitEntity(Entity aEntity) {
		if (aEntity instanceof EntityLiving) {
			((EntityLiving)aEntity).addPotionEffect(new PotionEffect(Potion.weakness.getId(), 400, 2, false));
			((EntityLiving)aEntity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 400, 2, false));
		}
	}
	*/
	/*
	@Override
	public boolean onItemUseFirst(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ, int aSide, float hitX, float hitY, float hitZ) {
		super.onItemUseFirst(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, hitX, hitY, hitZ);
		if (aWorld.isClientSide()) {
			return false;
		}
		
		aX += OFFSETS_X[aSide]; aY += OFFSETS_Y[aSide]; aZ += OFFSETS_Z[aSide];
		Block aBlock = aWorld.getBlock(aX, aY, aZ);
		if (aBlock == null) return false;
		byte aMeta = (byte)WD.meta(aWorld, aX, aY, aZ);
//      TileEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		
		if (aBlock == Blocks.WATER || aBlock == Blocks.WATER) {
			if (aMeta == 0 && GT_ModHandler.damageOrDechargeItem(aStack, 1, 1000, aPlayer)) {
				UT.Sounds.send(aWorld, SFX.IC_SPRAY, 1.0F, -1, aX, aY, aZ);
				aWorld.setBlock(aX, aY, aZ, Blocks.ICE, 0, 3);
				return true;
			}
			return false;
		}
		
		if (aBlock == Blocks.LAVA || aBlock == Blocks.LAVA) {
			if (aMeta == 0 && GT_ModHandler.damageOrDechargeItem(aStack, 1, 1000, aPlayer)) {
				UT.Sounds.send(aWorld, SFX.IC_SPRAY, 1.0F, -1, aX, aY, aZ);
				aWorld.setBlock(aX, aY, aZ, Blocks.OBSIDIAN, 0, 3);
				return true;
			}
			return false;
		}
		return false;
	}*/
}
