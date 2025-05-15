/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.tasks;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeJob;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeType;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * TASK_AUSBAU_AUFTRAG
 * 		Ein Ausbau eines Asteroiden..
 * 	- data1 -> die Auftrags-ID
 * 	- data2 -> Die Anzahl der bisherigen Versuche den Task durchzufuehren
 *  - data3 -> Die ID der ausfuehrenden Fraktion
 *
 *  @author Christoph Peltz
 */
@Service
public class HandleUpgradeJob implements TaskHandler
{
	private static final int ITEM_BBS = 182;
	private static final int ITEM_RE = 6;

	@Override
	public void handleEvent(Task task, String event)
	{
		Context context = ContextMap.getContext();
		var db = context.getEM();
		Taskmanager tm = Taskmanager.getInstance();

		if( !event.equals("tick_timeout") )
		{
			return;
		}

		final int faction = Integer.parseInt(task.getData3());

		int orderid = Integer.parseInt(task.getData1());
		int tick = context.get(ContextCommon.class).getTick();
		UpgradeJob order = db.find(UpgradeJob.class, orderid);
		int preis = order.getPrice(db);
		Base base = order.getBase();
		User user = order.getUser();
		Ship colonizer = order.getColonizer();
		User nullUser = db.find(User.class, 0);

		// Try-Count ueberschritten?
		if( Integer.parseInt(task.getData2()) > 35 && order.getEnd() == 0 )
		{
			// Es wurde nicht geschafft in 35 Versuchen die Ressourcen fuer den Ausbau bereit zu stellen
			cancelJob(db, tm, task, order, faction);
			return;
		}

		if( (order.getEnd() == 0) && (colonizer == null) )
		{
			cancelJob(db, tm, task, order, faction);
			return;
		}

		// Ausbau im Gange
		if( order.getEnd() != 0 )
		{
			// Ausbau abgeschlossen
			if( order.getEnd() <= tick )
			{
				// Setzen der Base-Informationen
				for(UpgradeInfo upgrade : order.getUpgrades())
                {
                    UpgradeType upgradetype = upgrade.getUpgradeType();
                    if(upgradetype != null)
                    {
                        upgrade.getUpgradeType().doWork(upgrade, base);
                    }
                }

				Map<Integer,Integer> bases = new HashMap<>();
				bases.put(base.getSystem(), 0);
				int basecount = 0;

				for( Base aBase : user.getBases() ) {
					final int system = aBase.getSystem();
					Common.safeIntInc(bases, system);
					basecount += aBase.getMaxTiles();
				}

				basecount += base.getMaxTiles();

				if( basecount > user.getUserValue(WellKnownUserValue.GAMEPLAY_BASES_MAXTILES) ) {

					sendFinishedWarningMessage(db, order, faction);
					base.setOwner(nullUser);
				}
				else
				{
					sendFinishedMessage(db, order, faction);
					base.setOwner(user);
				}
				// Loesche den Auftrag und den Task
				db.remove(order);
				tm.removeTask( task.getTaskID() );

				return;
			}
		}
		// Ausbau noch nicht begonnen
		// Geld muss noch bezahlt werden
		else if( order.getBar() && !order.getPayed() )
		{
			// Teste ob das Geld auf dem Colonizer ist
			if( colonizer.getCargo().hasResource(new ItemID(ITEM_RE), preis) )
			{
				// Genug Geld vorhanden
				colonizer.getCargo().substractResource(new ItemID(ITEM_RE), preis);
				order.setPayed(true);
			}
            else if( base.getCargo().hasResource(new ItemID(ITEM_RE), preis))
            {
                // Genug Geld vorhanden
                base.getCargo().substractResource(new ItemID(ITEM_RE), preis);
                order.setPayed(true);
            }
			else
			{
				// Setze den Try-Counter hoch
				tm.modifyTask( task.getTaskID(), task.getData1(), (Integer.parseInt(task.getData2()) + 1) + "", Integer.toString(faction) );
			}
		}
		// Ausbau noch nicht begonnen
		// Es wurde bereits gezahlt
		else
		{
			int bbsRequired = order.getMiningExplosive();
			int erzRequired = order.getOre();
			if( base.getCargo().hasResource(new ItemID(ITEM_BBS), bbsRequired) &&
					base.getCargo().hasResource(Resources.ERZ, erzRequired) )
			{
				// Genuegend Erz und BBS vorhanden
				// BBS und Erz "verbrauchen"
				base.getCargo().substractResource( new ItemID(ITEM_BBS), bbsRequired );
				base.getCargo().substractResource( Resources.ERZ, erzRequired );

				// Base "besetzen"
				base.setOwner(db.find(User.class, -19));
				List<ShipWerft> linkedShipyards = db.createQuery("from ShipWerft where linked=:linked", ShipWerft.class)
						.setParameter("linked", base)
						.getResultList();

				for(ShipWerft shipyard: linkedShipyards)
				{
					shipyard.resetLink();
				}

				// "Vernichte" den colonizer.
				order.setColonizer(null);
				colonizer.destroy();

				// Setzen wann wir fertig sind
                int dauer = new Random().nextInt(order.getMaxTicks(db)-order.getMinTicks(db)+1)+order.getMinTicks(db);
				order.setEnd( tick + dauer );
                task.setTimeout(dauer);
			}
			else
			{
				// Setze den Try-Counter hoch
				tm.modifyTask( task.getTaskID(), task.getData1(), (Integer.parseInt(task.getData2()) + 1) + "", Integer.toString(faction) );
			}
		}

		if( Integer.parseInt(task.getData2()) == 6 && order.getEnd() == 0 )
		{
			sendWarningMessage(db, order, faction);
		}

		tm.incTimeout(task.getTaskID());
	}

	private void cancelJob(EntityManager db, Taskmanager tm, Task task, UpgradeJob order, final int faction)
	{
		if( order.getBar() && order.getPayed() )
		{
			order.getBase().getCargo().addResource(new ItemID(ITEM_RE), order.getPrice(db));
		}
		else if( !order.getBar() )
		{
			order.getUser().transferMoneyFrom( faction, order.getPrice(db), "Ausbau von " + order.getBase().getName() + " aufgrund von Ressourcenknappheit fehlgeschlagen.");
		}

		// Loesche den Auftrag und den Task
		db.remove(order);
		tm.removeTask( task.getTaskID() );
	}

	private void sendWarningMessage(EntityManager db, UpgradeJob order, final int factionId)
	{
		User faction = db.find(User.class, factionId);

		String message = "Sehr geehrter/geehrte/geehrtes "+order.getUser().getName()+",\n\n"+
			"gerne führen wir den von Ihnen beauftragten Ausbau des Asteroids '"+order.getBase().getName()+"' ("+order.getBase().getId()+") durch. Bitte stellen Sie die folgenden Dinge sicher: [list]\n";
		if( !order.getPayed() ) {
			if( order.getBar() ) {
				message += "[*] Es müssen sich mindestens "+order.getPrice(db)+" RE auf dem angegebenen Bergbauschiff oder der Basis befinden.\n";
			}
			else {
				message += "[*] Sie müssen mindestens "+order.getPrice(db)+" RE auf Ihrem Konto haben.\n";
			}
		}
		message += "[*] Es müssen [resource=i"+ITEM_BBS+"|0|0]"+order.getMiningExplosive()+"[/resource] "+
			"und [resource="+Resources.ERZ.toString()+"]"+order.getOre()+"[/resource] auf dem Asteroiden vorhanden sein.\n";

		message += "[/list]\n";
		message += "Bitte erfüllen Sie die genannten Bedingungen zeitnah, da andernfalls Ihre Bestellung storniert werden muss.\n\n";
		message += "Mit freundlichen Grüßen\n";
		message += faction.getPlainname();

		PM.send(faction, order.getUser().getId(), "Ihr bestellter Asteroidenausbau", message, db);
	}

	private void sendFinishedMessage(EntityManager db, UpgradeJob order, final int factionId)
	{
		User faction = db.find(User.class, factionId);

		String message = "Sehr geehrter/geehrte/geehrtes "+order.getUser().getName()+",\n\n"+
			"der von Ihnen bestellte Ausbau des Asteroids '"+order.getBase().getName()+"' ("+order.getBase().getId()+") ist nun abgeschlossen. " +
					"Wir hoffen, dass Sie mit der herausragenden Qualität der '"+faction.getPlainname()+"'-Asteroidenerweiterungen zufrieden sind. \n";
		message += "Wir würden uns freuen, wenn wir Sie in Zukunft erneut als Kunden begrüßen dürften.\n\n";
		message += "Mit freundlichen Grüßen\n";
		message += faction.getPlainname();

		PM.send(faction, order.getUser().getId(), "Asteroidenausbau abgeschlossen", message, db);
	}

	private void sendFinishedWarningMessage(EntityManager db, UpgradeJob order, final int factionId)
	{
		User faction = db.find(User.class, factionId);

		String message = "Sehr geehrter/geehrte/geehrtes "+order.getUser().getName()+",\n\n"+
			"der von Ihnen bestellte Ausbau des Asteroids '"+order.getBase().getName()+"' ("+order.getBase().getId()+") ist nun abgeschlossen. " +
					"Aufgrund Ihres zwischenzeitlich erreichten Feldermaximums ist es nicht möglich, Ihnen den Asteroiden zuückzugeben.\n";
		message += "Der Asteroid wurde von "+ faction.getPlainname() +" aufgegeben und kann jederzeit von Ihnen erneut besiedelt werden.\n";
		message += "Wir würden uns freuen, wenn wir Sie in Zukunft erneut als Kunden begrüßen dürften.\n\n";
		message += "Mit freundlichen Grüßen\n";
		message += faction.getPlainname();

		PM.send(faction, order.getUser().getId(), "Asteroidenausbau abgeschlossen", message, db);
	}
}
