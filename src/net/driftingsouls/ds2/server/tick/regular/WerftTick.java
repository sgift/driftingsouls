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
package net.driftingsouls.ds2.server.tick.regular;

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftKomplex;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

import org.hibernate.FlushMode;
import org.hibernate.Transaction;

/**
 * Berechnung des Ticks fuer Werften.
 * @author Christopher Jung
 *
 */
public class WerftTick extends TickController {

	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void tick() {
		org.hibernate.Session db = getDB();
		FlushMode oldMode = db.getFlushMode();
		db.setFlushMode(FlushMode.MANUAL);
		Transaction transaction = db.beginTransaction();
		final User sourceUser = (User)db.get(User.class, -1);

		List<WerftObject> werften = db.createQuery("from ShipWerft s inner join fetch s.ship")
			.list();
		werften.addAll(db.createQuery("from BaseWerft b inner join fetch b.base")
			.list());
		werften.addAll(db.createQuery("from WerftKomplex")
			.list());
		int count = 0;
		for( Iterator<?> iter=werften.iterator(); iter.hasNext(); )
		{
			try
			{
				processWerft(sourceUser, (WerftObject)iter.next());
			}
			catch(Exception e)
			{
				transaction.rollback();
				transaction = db.beginTransaction();
			}

			count++;
			int MAX_UNFLUSHED_OBJECTS = 50;
			if(count%MAX_UNFLUSHED_OBJECTS == 0)
			{
				db.flush();
				transaction.commit();
				transaction = db.beginTransaction();
			}
		}
		db.flush();
		transaction.commit();
		db.setFlushMode(oldMode);
	}

	private void processWerft(final User sourceUser, WerftObject werft) {
		try
		{
			org.hibernate.Session db = getDB();

			if( (werft instanceof ShipWerft) && (((ShipWerft)werft).getShipID() < 0) )
			{
				return;
			}

			User owner = werft.getOwner();
			if( (werft instanceof WerftKomplex) && !((WerftKomplex)werft).isExistant())
			{
				return;
			}
			if( (owner.getVacationCount() > 0) && (owner.getWait4VacationCount() == 0) )
			{
				this.log("xxx Ignoriere Werft "+werft.getWerftID()+" [VAC]");
				return;
			}
			this.log("+++ Werft "+werft.getWerftID()+":");

			if( !werft.isBuilding() )
			{
				return;
			}

			int maxCompleted = 60;

			WerftQueueEntry[] entries = werft.getScheduledQueueEntries();
			for( int i=0; i < entries.length; i++ )
			{
				WerftQueueEntry entry = entries[i];

				ShipTypeData shipd = entry.getBuildShipType();

				this.log("\tAktueller Auftrag: "+shipd.getTypeId()+"; dauer: "+entry.getRemainingTime());

				if( entry.getRequiredItem() > -1 )
				{
					Item item = (Item)db.get(Item.class, entry.getRequiredItem());
					this.log("\tItem benoetigt: "+item.getName()+" ("+entry.getRequiredItem()+")");
				}

				// Wenn keine volle Crew vorhanden ist, besteht hier die Moeglichkeit, dass nicht weitergebaut wird.
				if( Math.random() <= werft.getWorkerPercentageAvailable() )
				{
					if( entry.isBuildContPossible() )
					{
						entry.continueBuild();
						this.log("\tVoraussetzungen erfuellt - bau geht weiter");
					}
				}
				else
				{
					this.log("Bau wegen Arbeitermangel pausiert: "+werft.getWorkerPercentageAvailable());
				}

				if( entry.getRemainingTime() <= 0 ) {
					this.log("\tSchiff "+shipd.getTypeId()+" gebaut");

					int shipid = entry.finishBuildProcess();
					this.slog(entry.MESSAGE.getMessage());

					if( shipid > 0 ) {
						// MSG
						String msg = "Auf "+werft.getName()+" wurde eine "+shipd.getNickname()+" gebaut. Sie steht bei "+werft.getLocation().displayCoordinates(false)+".";

						PM.send(sourceUser, werft.getOwner().getId(), "Schiff gebaut", msg);
					}

					if( --maxCompleted <= 0 )
					{
						this.log("Maximum an fertige Schiffen erreicht - abbruch");
						PM.send(sourceUser, werft.getOwner().getId(), "Geplante Auslieferungen",
								"Auf "+werft.getName()+" wurde die maximale Anzahl an gleichzeig zu produzierenden Schiffen erreicht. " +
								"Weitere Fertigstellungen wurden von der Raumsicherheit als auch vom Arbeitsschutzbeauftragten der auf den " +
								"Werften vertretenen Gewerkschaften abgeleht. Der Weiterbau wird beim nächsten Tick automatisch wieder aufgenommen.\n\ngez.\nKoordinationsbüro Werftkomplexe");
						break;
					}
				}
			}
		}
		catch( RuntimeException e )
		{
			this.log("Werft "+werft.getWerftID()+" failed: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "WerftTick Exception", "werft: "+werft.getWerftID());

			throw e;
		}
	}

}
