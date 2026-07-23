package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.DBTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeJob;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeType;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * End-to-End-Regressionstest fuer die Tickfehler-Klasse "Illegal attempt to associate a collection
 * with two open sessions" (siehe {@link net.driftingsouls.ds2.server.tasks.HandleUpgradeJobStaleEntityManagerTest}
 * fuer den fokussierten Test auf {@code HandleUpgradeJob}-Ebene). Dieser Test geht stattdessen ueber
 * die echte, von Spring verwaltete {@link RestTick}-Bean und bildet damit nach, was in Produktion
 * tatsaechlich passiert: Quartz fuehrt viele, voneinander unabhaengige Tick-Ausfuehrungen auf einem
 * wiederverwendeten Worker-Thread aus. Da {@code RestTick} (wie alle {@code TickController} und
 * {@code TaskHandler}) {@code @Scope("thread")} ist, cached Springs {@code ThreadScope} dieselbe
 * Instanz ueber mehrere, hier explizit simulierte "Tick-Ausfuehrungen" auf diesem Testthread hinweg.
 *
 * <p>Vor der Behebung (Konstruktor-injizierter {@code EntityManager} in {@code TaskHandler} bzw. ein
 * in {@link net.driftingsouls.ds2.server.tick.TickController} bei der ersten Konstruktion auf einem
 * Thread eingefrorenes Feld) fuehrte genau dieses Szenario dazu, dass ein selten gebrauchter
 * TaskHandler wie {@code HandleUpgradeJob} irgendwann mit einer anderen, aber weiterhin offenen
 * Session arbeitete als der Rest des Ticks. Seit {@code TickController.getEM()}/{@code getDB()} und
 * die {@code TaskHandler} ihren EntityManager bei jedem Aufruf dynamisch ueber
 * {@code ContextMap.getContext().getEM()} beziehen, verarbeitet ein zweiter, unabhaengiger
 * "Tick-Durchlauf" auf demselben (wiederverwendeten) Thread einen Ausbau-Auftrag korrekt.
 */
public class RestTickThreadReuseTest extends DBTest
{
	@Test
	public void restTick_nachSimuliertemTickwechselAufDemselbenThread_verarbeitetUpgradeJobKorrekt()
	{
		// "Erster Tick" auf diesem Thread: RestTick wird hier zum ersten Mal ueber Spring aufgeloest
		// und von ThreadScope fuer die Lebensdauer dieses Threads gecacht.
		RestTick restTick = (RestTick) getContext().getBean(RestTick.class, null);
		restTick.execute();

		// Tick-Grenze, wie ScheduledTick sie zwischen zwei unabhaengigen Ausfuehrungen herstellt:
		// die aktuelle EntityManager-Session wird geschlossen; der naechste Zugriff erzeugt ueber
		// HibernateUtil eine komplett neue.
		HibernateUtil.removeCurrentEntityManager();

		final int[] orderId = new int[1];
		final String[] taskId = new String[1];

		mitTransaktion(() -> {
			User faction = persist(new User("faction", "***", 0, "", new Cargo(), "faction@localhost"));
			User owner = persist(new User("owner", "***", 0, "", new Cargo(), "owner@localhost"));
			BaseType baseType = persist(new BaseType("TestKlasse"));
			Base base = persist(new Base(new Location(1, 1, 1), owner, baseType));

			UpgradeInfo upgrade = persist(new UpgradeInfo(baseType, UpgradeType.CORE));

			UpgradeJob order = persist(new UpgradeJob(base, owner, false, null));
			order.addUpgrade(upgrade);
			order.setEnd(1);

			Task task = persist(new Task(Taskmanager.Types.UPGRADE_JOB));
			task.setData1(String.valueOf(order.getId()));
			task.setData2("0");
			task.setData3(String.valueOf(faction.getId()));
			// RestTick.doTasks() holt Tasks ueber Taskmanager.getTasksByTimeout(1) ab - im
			// Gegensatz zu HandleUpgradeJobStaleEntityManagerTest, das handleEvent() direkt aufruft.
			task.setTimeout(1);

			orderId[0] = order.getId();
			taskId[0] = task.getTaskID();
		});

		// "Zweiter", unabhaengiger Tick auf demselben (wiederverwendeten) Thread: RestTick ist ueber
		// ThreadScope dieselbe Instanz wie eben - genau das Szenario, das den urspruenglichen Fehler
		// ausgeloest hat.
		RestTick restTickZweiterTick = (RestTick) getContext().getBean(RestTick.class, null);
		assertSame(restTick, restTickZweiterTick);

		restTickZweiterTick.execute();

		// Der Ausbau-Auftrag wurde im "Ausbau abgeschlossen"-Zweig von HandleUpgradeJob entfernt -
		// waere die Session-Vermischung noch vorhanden, waere die Exception dort abgefangen und
		// geloggt worden (RestTick.doTasks() faengt RuntimeExceptions ab), aber Auftrag und Task
		// waeren dann NICHT entfernt worden.
		assertThat(getEM().find(UpgradeJob.class, orderId[0]), nullValue());
		assertThat(getEM().find(Task.class, taskId[0]), nullValue());
	}
}
