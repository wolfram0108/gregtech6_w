/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregapi.gui;

import static gregapi.data.CS.*;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Gregorius Techneticies
 *
 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): 1.7.10 {@code GuiContainer} (immediate-mode: {@code drawGuiContainerBackgroundLayer}
 * рисовал фон через {@code mc.renderEngine.bindTexture}+GL11, {@code drawTexturedModalRect} слал квады в
 * {@code Tessellator}, {@code drawScreen} каждый кадр перерисовывал tooltip) заменён {@code AbstractContainerScreen<T>}
 * — в 1.20.1 это immediate-mode-конвейер {@code GuiGraphics}
 * (`forge-1201-decompiled/net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.java:89,160,181,186`:
 * {@code render}→{@code renderBg}/{@code renderLabels}/{@code renderTooltip}), то есть та же схема «слой фона,
 * слой текста, тултип», что и {@code drawGuiContainerXxxLayer}/{@code drawScreen} 1.7.10. Legacy-имена полей/методов
 * ({@code mc}, {@code fontRendererObj}, {@code xSize}/{@code ySize}, {@code drawTexturedModalRect}, {@code allowUserInput})
 * сохранены здесь КАК ОДИН нейтральный compile-only мост (централизация #3, единая точка для всей иерархии
 * {@code ContainerClientDefault/Chest/BasicMachine}) — их построчную адаптацию под подклассы делать не пришлось.
 * F14-gui МОСТ (единый на всю иерархию): движок зовёт {@code renderBg}/{@code renderLabels}/{@code renderTooltip},
 * мост маршрутизирует их в 1.7.10-хуки {@code drawGuiContainerBackgroundLayer}/{@code drawGuiContainerForegroundLayer}
 * (тела подклассов дословные); {@code drawTexturedModalRect}/{@code drawString} рисуют через держатель
 * {@link #mGraphics} (текущий {@code GuiGraphics} кадра — аналог «связанной текстуры + Tessellator» 1.7.10:
 * bindTexture(mBackground) заменён параметром текстуры в каждом blit).
 */
public class ContainerClient extends AbstractContainerScreen<ContainerCommon> {

	public boolean mCrashed = F;

	public ResourceLocation mBackground;

	public String mNEI = "";

	public ContainerCommon mContainer;

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было поле {@code GuiScreen.mc} (переименовано в {@code Screen.minecraft}, см. class javadoc). */
	protected final Minecraft mc;
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было поле {@code GuiScreen.fontRendererObj} (переименовано в {@code Screen.font}, см. class javadoc). */
	protected final Font fontRendererObj;
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): были мутируемые поля {@code GuiContainer.xSize/ySize}; в 26.1.2
	 *  {@code AbstractContainerScreen.imageWidth/imageHeight} — {@code final} (подклассы GT6 мутируют
	 *  {@code ySize} ПОСЛЕ {@code super(...)}, см. {@link ContainerClientChest}) — отдельный держатель, см. class javadoc. */
	protected int xSize, ySize;
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было поле {@code GuiContainer.allowUserInput} (см. class javadoc). */
	protected boolean allowUserInput;

	/** Держатель графики кадра: валиден только внутри renderBg/renderLabels (мост, см. class javadoc). */
	protected GuiGraphics mGraphics = null;

	/** Диаг-счётчики судьи П1 (движок реально нарисовал фон/текст; образец — MultiTileEntityBER.sSubmitCalls). */
	public static final java.util.concurrent.atomic.AtomicLong sBlitCalls = new java.util.concurrent.atomic.AtomicLong(), sTextCalls = new java.util.concurrent.atomic.AtomicLong();

	public int getLeft() {return leftPos;}
	public int getTop() {return topPos;}

	public ContainerClient(ContainerCommon aContainer, String aBackgroundPath) {
		super(aContainer, aContainer.mInventoryPlayer, Component.empty());
		mContainer = aContainer;
		// F-namespace lowercase: GT6-пути несут заглавные (machines/Oven.png), neo отвергает не-[a-z0-9/._-]
		// (IdentifierException) — тот же приём, что TextureSet:122 (ассеты на диске уже lowercase).
		mBackground = ResourceLocation.parse(aBackgroundPath.toLowerCase(java.util.Locale.ROOT));
		mc = minecraft;
		fontRendererObj = font;
		xSize = imageWidth;
		ySize = imageHeight;
	}

	// GT6-подклассы мутируют xSize/ySize ПОСЛЕ super(...) (ContainerClientChest), а neo imageWidth/imageHeight — final →
	// guiLeft/guiTop 1.7.10 (= leftPos/topPos) центрируем по GT6-полям, чтобы слоты и фон сходились.
	@Override protected void init() {
		super.init();
		leftPos = (width - xSize) / 2;
		topPos  = (height - ySize) / 2;
	}

	/**
	 * BUG-056 часть Б: открыть список рецептов ЭТОЙ машины — то, что в 1.7.10 давал клик по ПРОГРЕСС-БАРУ.
	 *
	 * <p><b>Почему это восстановление функции, а не новая фича.</b> Обработку клика в 1.7.10 делал НЕ GT6,
	 * а мод NEI — своим оверлеем поверх любого {@code GuiContainer}: игрок жал на стрелку прогресса и
	 * получал весь список рецептов машины (уточнение игрока 2026-07-28). GT6 лишь отдавал имя категории
	 * полем {@link #mNEI} ({@code ContainerClientBasicMachine:37}, 1:1 с оригиналом {@code :39}).
	 * В 26.1.2 роль NEI занял JEI, но клик по прогрессу он не обрабатывает — функция была УТРАЧЕНА.
	 * Раз эквивалента в новой версии нет, её выполняет сам мод (указание игрока: «функцию нужно выполнить
	 * или заменить на новую, если она не существует в новой версии»).</p>
	 *
	 * <p>Централизация: сама ОТКРЫВАЛКА живёт здесь, в базовом классе всей иерархии GUI, и ведёт в тот же
	 * центр, что иконка безынтерфейсных машин — {@code GT6_JEI_Plugin.showRecipeCategory(mNEI)}, ключ
	 * прежний {@code mNameNEI}. Где именно кликать, знает подкласс: у машинного GUI это область
	 * прогресс-бара ({@link ContainerClientBasicMachine#mouseClicked}).</p>
	 *
	 * @return {@code true}, если экран рецептов открыт (клик считается обработанным)
	 */
	public boolean openRecipesForThisGUI() {
		if (!NEI || !gregapi.util.UT.Code.stringValid(mNEI)) return F;
		return gregapi.jei.GT6_JEI_Plugin.showRecipeCategory(mNEI);
	}

	// Заглушка null-реконструкции (ContainerCommon.createFromNetwork, mTileEntity==null) = 1.7.10-семантика
	// «GUI не открылся»: закрываем на первом тике (vanilla containerTick пуст — закрытия по stillValid клиент не делает).
	@Override protected void containerTick() {
		super.containerTick();
		if (mContainer != null && mContainer.mTileEntity == null) onClose();
	}

	// Порядок кадра 1.20.1 — дословно ванильный ContainerScreen.render:25-29 (затемнение, слои, тултип);
	// в 1.7.10 то же делал GuiContainer.drawScreen.
	@Override public void render(GuiGraphics aGraphics, int aMouseX, int aMouseY, float aPartial) {
		renderBackground(aGraphics);
		super.render(aGraphics, aMouseX, aMouseY, aPartial);
		renderTooltip(aGraphics, aMouseX, aMouseY);
	}

	// F14-gui мост: renderBg → 1.7.10 background-хук (экранные координаты, как в 1.7.10 — без translate).
	@Override protected void renderBg(GuiGraphics aGraphics, float aPartial, int aMouseX, int aMouseY) {
		mGraphics = aGraphics;
		try {drawGuiContainerBackgroundLayer(aPartial, aMouseX, aMouseY);} finally {mGraphics = null;}
	}

	// F14-gui мост: renderLabels → 1.7.10 foreground-хук. БЕЗ super: 1.7.10 GuiContainer лейблов сам не рисовал,
	// заголовки — дело подкласса; pose уже translate(leftPos, topPos) — локальные координаты, как в 1.7.10.
	@Override protected void renderLabels(GuiGraphics aGraphics, int aMouseX, int aMouseY) {
		mGraphics = aGraphics;
		try {drawGuiContainerForegroundLayer(aMouseX, aMouseY);} finally {mGraphics = null;}
	}

	// 1.7.10 drawScreen поверх стандартных тултипов показывал тултип ПУСТОГО Slot_Base (getTooltip) — сам drawScreen
	// (цикл кадра) теперь у движка, GT6-довесок переносится в его tooltip-хук.
	//
	// BUG-082: УСЛОВИЕ СТОИТ НА СТЕКЕ, как в 1.7.10 (`ContainerClient.drawScreen:81` — `ST.invalid(tSlot.getStack())`),
	// а не на hasItem(). Роль этого довеска ровно одна и та же, что была: подсказка ПУСТОГО слота. Тултип СОДЕРЖИМОГО
	// (в т.ч. голо-слотов с дисплеями жидкостей) собирает сам движок в super — его политикой целиком
	// (AbstractContainerScreen:199-208), после того как Slot_Holo перестал лгать движку про hasItem() (см. Slot_Holo).
	@Override protected void renderTooltip(GuiGraphics aGraphics, int aMouseX, int aMouseY) {
		super.renderTooltip(aGraphics, aMouseX, aMouseY);
		if (!(hoveredSlot instanceof Slot_Base tSlot)) return;
		if (gregapi.util.ST.n(hoveredSlot.getItem()) != null) return;   // F15-граница: EMPTY -> null; непустой слот — дело движка
		java.util.List<String> tTip = tSlot.getTooltip(minecraft.player, minecraft.options.advancedItemTooltips);
		if (tTip != null && !tTip.isEmpty()) {
			java.util.List<Component> tComps = new java.util.ArrayList<>();
			for (String tLine : tTip) if (tLine != null) tComps.add(Component.literal(tLine));
			aGraphics.renderTooltip(font, tComps, java.util.Optional.empty(), aMouseX, aMouseY);
		}
	}

	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		//
	}

	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		drawGuiContainerBackgroundLayer2(par1, par2, par3);
	}

	protected void drawGuiContainerBackgroundLayer2(float par1, int par2, int par3) {
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}

	/** 1.7.10 drawTexturedModalRect: квад из связанной текстуры (у GT6 всегда mBackground, атлас 256×256) → один blit. */
	protected void drawTexturedModalRect(int aX, int aY, int aU, int aV, int aW, int aH) {
		if (mGraphics != null) {mGraphics.blit(mBackground, aX, aY, aU, aV, aW, aH); sBlitCalls.incrementAndGet();}
	}

	/** 1.7.10 GuiScreen.drawString(FontRenderer,...): подклассы звали fontRendererObj.drawString — у neo Font рисующих
	 *  методов нет, мост тот же (без тени; альфа 0 → 0xFF, как FontRenderer 1.7.10). */
	public void drawString(Font aFont, String aText, int aX, int aY, int aColor) {
		if (mGraphics != null && aText != null) {mGraphics.drawString(aFont, aText, aX, aY, (aColor & 0xFF000000) == 0 ? aColor | 0xFF000000 : aColor, F); sTextCalls.incrementAndGet();}
	}

	protected boolean isMouseOverSlot(Slot aSlot, int aX, int aY) {return isHovering(aSlot.x, aSlot.y, 16, 16, aX, aY);}
}
