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

import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.batch.UnitOfWork;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import javax.persistence.FlushModeType;
import java.util.List;

/**
 * Berechnung des Ticks fuer Werften.
 *
 * @author Christopher Jung
 */
@Service
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class WerftTick extends TickController
{
	@Override
	protected void prepare() {}

	@Override
	protected void tick()
	{
		var em = getEM();
		em.setFlushMode(FlushModeType.COMMIT);
		final User sourceUser = em.find(User.class, -1);

		List<WerftObject> werften = em.createQuery("from WerftObject w where size(w.queue)>0", WerftObject.class)
								.getResultList();
		new UnitOfWork<WerftObject>("Werft Tick", em)
		{
			@Override
			public void doWork(WerftObject werft) {
				//Kampagnentick und die Werft steht nicht in einem der ausgewaehlten Systeme
				if(isCampaignTick() && !affectedSystems.contains(werft.getSystem())) {
					return;
				}
				processWerft(sourceUser, werft);
			}

		}.setFlushSize(10)
		.executeFor(werften);
	}

	private void processWerft(final User sourceUser, WerftObject werft)
	{
		try
		{
			javax.persistence.EntityManager em = getEM();

			if ((werft instanceof ShipWerft) && (((ShipWerft) werft).getShipID() < 0))
			{
				return;
			}

			User owner = werft.getOwner();
			if ((werft instanceof WerftKomplex) && !((WerftKomplex) werft).isExistant())
			{
				return;
			}
			if ((owner.getVacationCount() > 0) && (owner.getWait4VacationCount() == 0))
			{
				this.log("xxx Ignoriere Werft " + werft.getWerftID() + " [VAC]");
				return;
			}
			this.log("+++ Werft " + werft.getWerftID() + ":");

			if (!werft.isBuilding())
			{
				return;
			}

			int maxCompleted = 60;

			werft.rescheduleQueue();

			WerftQueueEntry[] entries = werft.getScheduledQueueEntries();
			for (WerftQueueEntry entry : entries)
			{
				ShipTypeData shipd = entry.getBuildShipType();

				this.log("\tAktueller Auftrag: " + shipd.getTypeId() + "; dauer: " + entry.getRemainingTime());

				if (entry.getRequiredItem() > -1)
				{
					Item item = em.find(Item.class, entry.getRequiredItem());
					this.log("\tItem benoetigt: " + item.getName() + " (" + entry.getRequiredItem() + ")");
				}

				// Wenn keine volle Crew vorhanden ist, besteht hier die Moeglichkeit, dass nicht weitergebaut wird.
				if (Math.random() <= werft.getWorkerPercentageAvailable())
				{
					if (entry.isBuildContPossible())
					{
						entry.continueBuild();
						this.log("\tVoraussetzungen erfuellt - bau geht weiter");
					}
				}
				else
				{
					this.log("Bau wegen Arbeitermangel pausiert: " + werft.getWorkerPercentageAvailable());
				}

				if (entry.getRemainingTime() <= 0)
				{
					this.log("\tSchiff " + shipd.getTypeId() + " gebaut");

					int shipid = entry.finishBuildProcess();
					this.slog(entry.MESSAGE.getMessage());

					if (shipid > 0)
					{
						// MSG
						String msg = "Auf " + bbcode(werft) + " wurde eine [ship=" + shipid + "]" + shipd.getNickname() + "[/ship] gebaut. Sie steht bei [map]" + werft.getLocation().displayCoordinates(false) + "[/map].";

                        if(werft.getOwner().getUserValue(WellKnownUserValue.GAMEPLAY_USER_SHIP_BUILD_PM)) {
                            PM.send(sourceUser, werft.getOwner().getId(), "Schiff gebaut", msg, em);
                        }
					}

					if (--maxCompleted <= 0)
					{
						this.log("Maximum an fertigen Schiffen erreicht - abbruch");
						PM.send(sourceUser, werft.getOwner().getId(), "Geplante Auslieferungen",
							   "Auf " + bbcode(werft) + " wurde die maximale Anzahl an gleichzeig zu produzierenden Schiffen erreicht. " +
							   "Weitere Fertigstellungen wurden von der Raumsicherheit als auch vom Arbeitsschutzbeauftragten der auf den " +
							   "Werften vertretenen Gewerkschaften abgeleht. Der Weiterbau wird beim nächsten Tick automatisch wieder aufgenommen.\n\ngez.\nKoordinationsbüro Werftkomplexe", em);
						break;
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			this.log("Werft " + werft.getWerftID() + " failed: " + e);
			e.printStackTrace();
			Common.mailThrowable(e, "WerftTick Exception", "werft: " + werft.getWerftID());

			throw e;
		}
	}

	private String bbcode(WerftObject werft)
	{
		if (werft instanceof BaseWerft)
		{
			return "[base=" + ((BaseWerft) werft).getBaseID() + "]" + werft.getName() + "[/base]";
		}
		if (werft instanceof ShipWerft)
		{
			return "[ship=" + ((ShipWerft) werft).getShipID() + "]" + werft.getName() + "[/ship]";
		}
		if (!(werft instanceof WerftKomplex))
		{
			return werft.getName();
		}

		WerftKomplex komplex = (WerftKomplex) werft;
		return bbcode(komplex.getMembers()[0]);
	}

}
