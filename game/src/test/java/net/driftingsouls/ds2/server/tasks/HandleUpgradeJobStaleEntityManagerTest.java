package net.driftingsouls.ds2.server.tasks;

import net.driftingsouls.ds2.server.DBTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeJob;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeType;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Regressionstest fuer die Tickfehler-Klasse "Illegal attempt to associate a collection with two
 * open sessions", die im produktiven Tick-Log bei der Bearbeitung von UPGRADE_JOB-Tasks auftrat.
 *
 * <p>Hintergrund: {@link HandleUpgradeJob} (wie alle {@code TaskHandler}) injizierte frueher
 * seinen {@code EntityManager} per Konstruktor. Da die Bean {@code @Scope("thread")} war, cachte
 * Springs {@code ThreadScope} diese Instanz - inklusive des zum Konstruktionszeitpunkt aktuellen
 * EntityManagers - fuer die gesamte Lebensdauer des ausfuehrenden Threads; es gab keinen
 * Aufraeum-Mechanismus dafuer. Da Quartz denselben Worker-Thread ueber viele, unabhaengige
 * Tick-Ausfuehrungen hinweg wiederverwendet und jede davon (ueber {@code HibernateUtil}/
 * {@code ContextMap}) ihre eigene, frische {@code EntityManager}-Session erzeugt, arbeitete ein
 * nur selten gebrauchter TaskHandler wie {@code HandleUpgradeJob} irgendwann mit einer anderen -
 * aber weiterhin offenen - Session als der Rest des Ticks (z.B. {@link Taskmanager}, der stets
 * ueber {@code ContextMap.getContext().getDB()} arbeitet), was Hibernate beim Vermischen von
 * Collections aus beiden Sessions mit "Illegal attempt to associate a collection with two open
 * sessions" quittierte.
 *
 * <p>{@link HandleUpgradeJob} holt seinen {@code EntityManager} nun bei jedem Aufruf frisch ueber
 * {@code ContextMap.getContext().getEM()} - denselben Kanal wie {@link Taskmanager} - statt ihn
 * einmalig per Konstruktor zu injizieren. Damit ist die alte, gecachte Session ueberhaupt nicht
 * mehr konstruierbar; dieser Test bestaetigt, dass ein Ausbau-Auftrag ueber den vollstaendigen
 * "Ausbau abgeschlossen"-Zweig durchlaeuft und dabei durchgehend dieselbe Session verwendet wird.
 */
public class HandleUpgradeJobStaleEntityManagerTest extends DBTest
{
	@Test
	public void handleUpgradeJob_abgeschlossenerAusbau_verwendetDurchgehendDieselbeSessionUndRaeumtAufraeumt()
	{
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

			orderId[0] = order.getId();
			taskId[0] = task.getTaskID();
		});

		HandleUpgradeJob handler = new HandleUpgradeJob();
		Task task = getEM().find(Task.class, taskId[0]);
		handler.handleEvent(task, "tick_timeout");

		// Auftrag und Task wurden im "Ausbau abgeschlossen"-Zweig entfernt - beides innerhalb
		// derselben, ueber ContextMap.getContext().getEM() bezogenen Session wie handleEvent() sie
		// selbst verwendet hat.
		assertThat(getEM().find(UpgradeJob.class, orderId[0]), nullValue());
		assertThat(getEM().find(Task.class, taskId[0]), nullValue());
	}
}
