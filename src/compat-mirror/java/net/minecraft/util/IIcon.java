package net.minecraft.util;

/**
 * F3-render compile-only shim. 1.7.10 net.minecraft.util.IIcon — интерфейс атласной текстуры, удалён в neo
 * (заменён TextureAtlasSprite + модельная система). Поверхность 1:1 с оригиналом
 * (gregtech6/build/tmp/recompSrc/net/minecraft/util/IIcon.java), БЕЗ @SideOnly (cpw.mods.fml нет в neo).
 * РЕАЛЬНЫЙ рендер отложен на F3-клиент-проход; здесь только тип для сборки ядра.
 */
public interface IIcon {
	int getIconWidth();
	int getIconHeight();
	float getMinU();
	float getMaxU();
	float getInterpolatedU(double u);
	float getMinV();
	float getMaxV();
	float getInterpolatedV(double v);
	String getIconName();
}
