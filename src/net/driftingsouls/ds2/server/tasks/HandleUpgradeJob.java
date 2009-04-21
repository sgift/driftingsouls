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

import java.util.Random;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.UpgradeJob;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * TASK_AUSBAU_AUFTRAG
 * 		Ein Ausbau eines Asteroiden..
 * 
 * 	- data1 -> die Auftrags-ID
 * 	- data2 -> Die Anzahl der bisherigen Versuche den Task durchzufuehren
 *  - data3 -> Die ID der ausfuehrenden Fraktion
 *   
 *  @author Christoph Peltz
 */
class HandleUpgradeJob implements TaskHandler
{
	private static final int ITEM_BBS = 182;
	private static final int ITEM_RE = 6;
	
	public void handleEvent(Task task, String event)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		Taskmanager tm = Taskmanager.getInstance();
		
		if( !event.equals("tick_timeout") )
		{
			return;
		}
		
		final int faction = Integer.parseInt(task.getData3());

		int orderid = Integer.parseInt(task.getData1());
		int tick = context.get(ContextCommon.class).getTick();
		UpgradeJob order = (UpgradeJob) db.get(UpgradeJob.class, orderid);
		int preis = (order.getTiles().getPrice() + order.getCargo().getPrice());
		Base base = order.getBase();
		User user = order.getUser();
		Ship colonizer = order.getColonizer();

		// Try-Count ueberschritten?
		if( Integer.parseInt(task.getData2()) > 35 && order.getEnd() == 0 )
		{
			// Es wurde nicht geschafft in 35 Versuchen die Ressourcen fuer den Ausbau bereit zu stellen
			cancelJob(tm, task, order, faction);
			return;
		}
		
		if( (order.getEnd() == 0) && (colonizer == null) )
		{
			cancelJob(tm, task, order, faction);
			return;
		}

		// Ausbau im Gange
		if( order.getEnd() != 0 )
		{
			// Ausbau abgeschlossen
			if( order.getEnd() <= tick )
			{
				// Setzen der Base-Informationen
				int tilemod = order.getTiles().getMod();
				if( tilemod > 0 ) {
					if( tilemod % base.getHeight() == 0 ) {
						base.setWidth(base.getWidth() + tilemod / base.getHeight());
					}
					else {
						base.setHeight(base.getHeight() + tilemod / base.getWidth());
					}
				}
				base.setMaxCargo(base.getMaxCargo() + order.getCargo().getMod());
				base.setOwner(user);

				sendFinishedMessage(db, order, faction);
				
				// Loesche den Auftrag und den Task
				db.delete(order);
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
			int bbsRequired = order.getTiles().getMiningExplosive() + order.getCargo().getMiningExplosive();
			int erzRequired = order.getTiles().getOre() + order.getCargo().getOre();
			if( base.getCargo().hasResource(new ItemID(ITEM_BBS), bbsRequired) && 
					base.getCargo().hasResource(Resources.ERZ, erzRequired) )
			{
				// Genuegend Erz und BBS vorhanden
				// BBS und Erz "verbrauchen"
				base.getCargo().substractResource( new ItemID(ITEM_BBS), bbsRequired );
				base.getCargo().substractResource( Resources.ERZ, erzRequired );

				// Base "besetzen"
				base.setOwner((User) db.get(User.class, -19));

				// "Vernichte" den colonizer.
				order.setColonizer(null);
				colonizer.destroy();

				// Setzen wann wir fertig sind
				order.setEnd( tick + new Random().nextInt(63)+7 );
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

	private void cancelJob(Taskmanager tm, Task task, UpgradeJob order, final int faction)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		if( order.getBar() && order.getPayed() )
		{
			order.getBase().getCargo().addResource(new ItemID(ITEM_RE), order.getTiles().getPrice() + order.getCargo().getPrice());
		}
		else if( !order.getBar() )
		{
			order.getUser().transferMoneyFrom( faction, order.getTiles().getPrice() + order.getCargo().getPrice(), "Ausbau von " + order.getBase().getName() + " aufgrund von Ressourcenknappheit fehlgeschlagen.");
		}

		// Loesche den Auftrag und den Task
		db.delete(order);
		tm.removeTask( task.getTaskID() );
	}

	private void sendWarningMessage(org.hibernate.Session db, UpgradeJob order, final int factionId)
	{
		User faction = (User)db.get(User.class, factionId);
		
		String message = "Sehr geehrter "+order.getUser().getPlainname()+",\n\n"+
			"gerne führen wir den von ihnen beauftragten Ausbau des Asteroids '"+order.getBase().getName()+"' ("+order.getBase().getId()+") durch. Bitte stellen sie die folgenden Dinge sicher: [list]\n";
		if( !order.getPayed() ) {
			if( order.getBar() ) {
				message += "[*] Es müssen sich mindestens "+(order.getTiles().getPrice() + order.getCargo().getPrice())+" RE auf dem angegebenen Colonizer befinden.\n";
			}
			else {
				message += "[*] Sie müssen mindestens "+(order.getTiles().getPrice() + order.getCargo().getPrice())+" RE auf ihrem Konto haben.\n";
			}
		}
		message += "[*] Es müssen [resource=i"+ITEM_BBS+"|0|0]"+(order.getTiles().getMiningExplosive() + order.getCargo().getMiningExplosive())+"[/resource] "+
			"und [resource="+Resources.ERZ.getID()+"]"+(order.getTiles().getOre()+order.getCargo().getOre())+"[/resource] auf dem Asteroiden vorhanden sein.\n";
		
		message += "[/list]\n";
		message += "Bitte erfüllen sie die genannten Bedingungen zeitnah, da andernfalls ihre Bestellung storniert werden muss.\n\n";
		message += "Mit freundlichen Grüßen\n";
		message += faction.getPlainname();
		
		PM.send(faction, order.getUser().getId(), "Ihr bestellter Asteroidenausbau", message);
	}
	
	private void sendFinishedMessage(org.hibernate.Session db, UpgradeJob order, final int factionId)
	{
		User faction = (User)db.get(User.class, factionId);
		
		String message = "Sehr geehrter "+order.getUser().getPlainname()+",\n\n"+
			"der von ihnen bestellte Ausbau des Asteroids '"+order.getBase().getName()+"' ("+order.getBase().getId()+") ist nun abgeschlossen. " +
					"Wir hoffen, dass sie mit der herausragenden Qualität von "+faction.getPlainname()+" Asteroidenerweiterungen zufrieden sind. \n";
		message += "Wir würden uns freuen, wenn wir sie in Zukunft erneut als Kunden begrüßen dürften.\n\n";
		message += "Mit freundlichen Grüßen\n";
		message += faction.getPlainname();
		
		PM.send(faction, order.getUser().getId(), "Asteroidenausbau abgeschlossen", message);
	}
}
