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

import net.neoforged.bus.api.SubscribeEvent;
import gregapi.util.UT;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * @author Gregorius Techneticies
 */
public class LoggerPlayerActivity implements Runnable {
	private ArrayList<String> mBufferedPlayerActivity = new ArrayList<>();

	/**
	 * Поток дневника, который он запоминает сам в run(). Так точка запуска остаётся дословно той же, что в
	 * оригинале (`new Thread(mPlayerLogger).start()` в GT_API), а прощание всё равно знает, кого будить из сна.
	 */
	private volatile Thread mThread = null;

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
	// ⛔ ПОДПИСКА РАЗБИТА ПО КОНКРЕТНЫМ СОБЫТИЯМ — иначе журнала нет вовсе. Развилка, предсказанная
	// пометкой выше, сбылась: база `PlayerInteractEvent` объявлена абстрактной, а шина такие подписки
	// запрещает — `addToListeners` бросает IllegalArgumentException «Cannot register listeners for
	// abstract …» на КАЖДУЮ подписку абстрактным типом. Падало это в конструкторе, на
	// EVENT_BUS.register(this), и гасилось пустым перехватом у вызывающего (GT_API), поэтому
	// mPlayerLogger оставался null и журнал активности не работал ни одной строкой.
	// Набор срабатываний при этом НЕ изменился: прежнее тело само отбирало ровно эти два подсобытия
	// (instanceof LeftClickBlock / RightClickBlock, остальные -> null -> пропуск), что 1:1 повторяло
	// правило 1.7.10 «любое действие, кроме RIGHT_CLICK_AIR» при перечне действий
	// {LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR}. Имена действий в строке — те же.
	@SubscribeEvent
	public void onPlayerInteractionLeftClickBlock(PlayerInteractEvent.LeftClickBlock aEvent) {logInteraction(aEvent, "LEFT_CLICK_BLOCK");}

	@SubscribeEvent
	public void onPlayerInteractionRightClickBlock(PlayerInteractEvent.RightClickBlock aEvent) {logInteraction(aEvent, "RIGHT_CLICK_BLOCK");}

	/** Тело прежнего единого обработчика: одно на оба подсобытия, чтобы строка журнала осталась в одном месте. */
	private void logInteraction(PlayerInteractEvent aEvent, String aAction) {
		if (aEvent.getEntity() != null && aEvent.getLevel() != null && !aEvent.getLevel().isClientSide() && mLog != null) mBufferedPlayerActivity.add(UT.Code.dateAndTime()+";"+aAction+";"+aEvent.getEntity().getName().getString()+";DIM:"+WD.dimensionId(aEvent.getLevel())+";"+aEvent.getPos().getX()+";"+aEvent.getPos().getY()+";"+aEvent.getPos().getZ()+";|;"+aEvent.getPos().getX()/10+";"+aEvent.getPos().getY()/10+";"+aEvent.getPos().getZ()/10);
	}
	
	// F7-event: 1.7.10 BlockEvent.HarvestDropsEvent -> neo BlockDropsEvent (event/level). harvester->getBreaker():Entity,
	// world->getLevel():ServerLevel, x/y/z->getPos().
	@SubscribeEvent
	public void onBlockHarvestingEvent(net.neoforged.neoforge.event.level.BlockDropsEvent aEvent) {
		if (aEvent.getBreaker() != null && !aEvent.getLevel().isClientSide() && mLog != null) mBufferedPlayerActivity.add(UT.Code.dateAndTime()+";HARVEST_BLOCK;"+aEvent.getBreaker().getName().getString()+";DIM:"+WD.dimensionId(aEvent.getLevel())+";"+aEvent.getPos().getX()+";"+aEvent.getPos().getY()+";"+aEvent.getPos().getZ()+";|;"+aEvent.getPos().getX()/10+";"+aEvent.getPos().getY()/10+";"+aEvent.getPos().getZ()/10);
	}
	
	@Override
	public void run() {
		mThread = Thread.currentThread();
		while (true) {try {
			if (mLog == null) return;
			flush(mLog);
			Thread.sleep(10000);
		} catch(Throwable e) {/**/}}
	}

	/**
	 * Слив накопленного в файл. Вынесен из тела цикла, потому что нужен ДВАЖДЫ — в такте работы и в прощании;
	 * второй копии этих строк быть не должно. Логика внутри дословно та же, что была в цикле у Грегориуса.
	 */
	private void flush(PrintStream aLog) {
		ArrayList<String> tList = mBufferedPlayerActivity;
		mBufferedPlayerActivity = new ArrayList<>();
		String tLastOutput = "";
		for (int i = 0, j = tList.size(); i < j; i++) {
			if (!tLastOutput.equals(tList.get(i))) aLog.println(tList.get(i));
			tLastOutput = tList.get(i);
		}
	}

	/**
	 * ПРОЩАНИЕ С ДНЕВНИКОМ. Зовётся из центра прощания мода (GT_API.onModServerStopped2) — там же, где мод
	 * прощается со всем остальным, а не отдельным механизмом.
	 * <p>Порядок важен и обеспечивает требование «дописать и закрыть», а не «оборвать»:
	 * гасим mLog (ловители событий замолкают ровно по тому же условию, по которому молчали до открытия файла)
	 * -> будим поток из Thread.sleep(10000) штатным interrupt, иначе выход сервера ждал бы до десяти секунд
	 * -> дожидаемся, пока он выйдет по СВОЕМУ ЖЕ условию `if (mLog == null) return`
	 * -> дописываем то, что успело накопиться после его последнего прохода, и закрываем файл.
	 * Пометить поток служебным (setDaemon) было нельзя: такой поток движок обрывает на полуслове, теряя хвост записи.
	 */
	public void stop() {
		PrintStream tLog = mLog;
		if (tLog == null) return;
		mLog = null;
		Thread tThread = mThread;
		if (tThread != null) {
			tThread.interrupt();
			try {tThread.join(5000);} catch(InterruptedException e) {Thread.currentThread().interrupt();}
		}
		flush(tLog);
		tLog.close();
	}
}
