package net.minecraft.client.renderer.texture;

import net.minecraft.util.IIcon;

/**
 * F3-render compile-only shim. 1.7.10 IIconRegister — регистратор атласных иконок, удалён в neo.
 * Поверхность 1:1 с оригиналом (без @SideOnly). Реальная регистрация текстур отложена на F3.
 */
public interface IIconRegister {
	IIcon registerIcon(String name);
}
