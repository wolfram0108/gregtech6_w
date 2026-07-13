package net.minecraft.client.renderer;

/**
 * F3-render compile-only shim. 1.7.10 RenderBlocks — immediate-mode (GL11) рендер блоков, удалён в neo
 * (заменён baked-модели + BlockRenderDispatcher). В GT6 фигурирует ТОЛЬКО как тип-параметр в сигнатурах
 * renderItem/renderBlock (методы на нём не вызываются) — пустого типа достаточно для сборки ядра.
 * Реальный рендер отложен на F3-клиент-проход (контролируемая отложенность §10).
 */
public class RenderBlocks {
}
