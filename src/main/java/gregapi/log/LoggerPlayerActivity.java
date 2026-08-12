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

package gregapi.log;
import gregapi.util.WD;

import java.io.PrintStream;
import java.util.ArrayList;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import gregapi.util.UT;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

/**
 * @author Gregorius Techneticies
 */
public class LoggerPlayerActivity implements Runnable {
	private ArrayList<String> mBufferedPlayerActivity = new ArrayList<>();
	
	public static PrintStream mLog = null;
	
	public LoggerPlayerActivity(PrintStream aLog) {
		NeoForge.EVENT_BUS.register(this);
		mLog = aLog;
	}
	
	// F7-event: 1.7.10 единое PlayerInteractEvent + поле action -> neo абстрактная база + суб-события (LeftClickBlock/
	// RightClickBlock/RightClickItem/…). action восстановлен через instanceof; air-взаимодействия (RightClickItem/
	// LeftClickEmpty) пропускаются = 1.7.10 RIGHT_CLICK_AIR-skip. Поля: entityPlayer->getEntity, world->getLevel,
	// x/y/z->getPos(); provider!=null-проверка снята (WorldProvider удалён, дублировала level!=null).
	// F7-event-bus-hierarchy (документация иерархии событий): если neo-шина не доставляет суб-события подписчику БАЗОВОГО
	// PlayerInteractEvent — разбить на два @SubscribeEvent (RightClickBlock+LeftClickBlock); лог косметичен.
	@SubscribeEvent
	public void onPlayerInteraction(PlayerInteractEvent aEvent) {
		String tAction = (aEvent instanceof PlayerInteractEvent.LeftClickBlock) ? "LEFT_CLICK_BLOCK" : (aEvent instanceof PlayerInteractEvent.RightClickBlock) ? "RIGHT_CLICK_BLOCK" : null;
		if (tAction != null && aEvent.getEntity() != null && aEvent.getLevel() != null && !aEvent.getLevel().isClientSide() && mLog != null) mBufferedPlayerActivity.add(UT.Code.dateAndTime()+";"+tAction+";"+aEvent.getEntity().getName().getString()+";DIM:"+WD.dimensionId(aEvent.getLevel())+";"+aEvent.getPos().getX()+";"+aEvent.getPos().getY()+";"+aEvent.getPos().getZ()+";|;"+aEvent.getPos().getX()/10+";"+aEvent.getPos().getY()/10+";"+aEvent.getPos().getZ()/10);
	}
	
	// F7-event: 1.7.10 BlockEvent.HarvestDropsEvent -> neo BlockDropsEvent (event/level). harvester->getBreaker():Entity,
	// world->getLevel():ServerLevel, x/y/z->getPos().
	@SubscribeEvent
	public void onBlockHarvestingEvent(net.neoforged.neoforge.event.level.BlockDropsEvent aEvent) {
		if (aEvent.getBreaker() != null && !aEvent.getLevel().isClientSide() && mLog != null) mBufferedPlayerActivity.add(UT.Code.dateAndTime()+";HARVEST_BLOCK;"+aEvent.getBreaker().getName().getString()+";DIM:"+WD.dimensionId(aEvent.getLevel())+";"+aEvent.getPos().getX()+";"+aEvent.getPos().getY()+";"+aEvent.getPos().getZ()+";|;"+aEvent.getPos().getX()/10+";"+aEvent.getPos().getY()/10+";"+aEvent.getPos().getZ()/10);
	}
	
	@Override
	public void run() {
		while (true) {try {
			if (mLog == null) return;
			ArrayList<String> tList = mBufferedPlayerActivity;
			mBufferedPlayerActivity = new ArrayList<>();
			String tLastOutput = "";
			for (int i = 0, j = tList.size(); i < j; i++) {
				if (!tLastOutput.equals(tList.get(i))) mLog.println(tList.get(i));
				tLastOutput = tList.get(i);
			}
			Thread.sleep(10000);
		} catch(Throwable e) {/**/}}
	}
}
