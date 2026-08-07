/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregapi.api;

/**
 * Переходник F1 (жизненный цикл). Событие фазы Init GregTech6 поверх жизненного цикла NeoForge.
 *
 * <p>Сохраняет трёхфазный контракт GregTech6 (Pre/Init/Post). neo-точка входа
 * ({@code gregtech6.GregTech6}) конструирует его и передаёт в {@code Abstract_Mod.onModInit(...)}.
 * По всему исходнику GregTech6 с этого события ничего не читается — оно служит маркером фазы.</p>
 */
public class FMLInitializationEvent {
	public FMLInitializationEvent() {}
}
