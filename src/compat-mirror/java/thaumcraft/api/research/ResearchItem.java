package thaumcraft.api.research;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (CompatTC.addResearch). См. compat-mirror/README.md. */
public class ResearchItem {
	public int displayColumn, displayRow;

	public ResearchItem(String aKey, String aCategory, AspectList aAspects, int aX, int aY, int aComplexity, ItemStack aIcon) {}

	public ResearchItem setAutoUnlock() {return this;}
	public ResearchItem setSecondary() {return this;}
	public ResearchItem setSpecial() {return this;}
	public ResearchItem setVirtual() {return this;}
	public ResearchItem setHidden() {return this;}
	public ResearchItem setRound() {return this;}
	public ResearchItem setStub() {return this;}
	public ResearchItem setLost() {return this;}
	public ResearchItem setParents(String[] aParents) {return this;}
	public ResearchItem setConcealed() {return this;}
	public ResearchItem setItemTriggers(ItemStack[] aTriggers) {return this;}
	public ResearchItem setPages(ResearchPage[] aPages) {return this;}
	public Object registerResearchItem() {return this;}
}
