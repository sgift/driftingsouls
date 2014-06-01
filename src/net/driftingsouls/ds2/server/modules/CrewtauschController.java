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
package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Transfer von Crew von Schiffen zu Schiffen/Basen (und umgekehrt).
 *
 * @author Christopher Jung
 */
@Module(name = "crewtausch")
public class CrewtauschController extends TemplateController
{
	/**
	 * Das Ziel fuer einen Crewtransfer.
	 */
	private static interface Target
	{
		/**
		 * Gibt die verfuegbare Crew zurueck.
		 *
		 * @return Die Crew
		 */
		public int getCrew();

		/**
		 * Setzt die Crew auf dem Objekt.
		 *
		 * @param crew Die Crew
		 */
		public void setCrew(int crew);

		/**
		 * Wird am Ende des Transfervorgangs aufgerufen.
		 */
		public void finishTransfer();

		/**
		 * Gibt den Namen des Objekts zurueck.
		 *
		 * @return Der Name
		 */
		public String getName();

		/**
		 * Gibt die ID des Objekts zurueck.
		 *
		 * @return Die ID
		 */
		public int getId();

		/**
		 * Gibt den Besitzer des Objekts zurueck.
		 *
		 * @return Der Besitzer
		 */
		public User getOwner();

		/**
		 * Gibt die maximale Anzahl an Crew auf dem Objekt zurueck.
		 *
		 * @return Die maximale Crewmenge (<code>-1</code> = unbegrenzt)
		 */
		public int getMaxCrew();
	}

	/**
	 * Crewtransfer-Ziel "Schiff".
	 */
	private static class ShipTarget implements Target
	{
		private Ship ship;

		ShipTarget(Ship ship)
		{
			this.ship = ship;
		}

		@Override
		public void finishTransfer()
		{
			ship.recalculateShipStatus();
		}

		@Override
		public int getCrew()
		{
			return ship.getCrew();
		}

		@Override
		public int getId()
		{
			return ship.getId();
		}

		@Override
		public String getName()
		{
			return ship.getName();
		}

		@Override
		public void setCrew(int crew)
		{
			ship.setCrew(crew);
		}

		@Override
		public int getMaxCrew()
		{
			return ship.getTypeData().getCrew();
		}

		@Override
		public User getOwner()
		{
			return ship.getOwner();
		}
	}

	/**
	 * Crewtransfer-Ziel "Basis".
	 */
	private static class BaseTarget implements Target
	{
		private Base base;

		BaseTarget(Base base)
		{
			this.base = base;
		}

		@Override
		public void finishTransfer()
		{
			// EMPTY
		}

		@Override
		public int getCrew()
		{
			return base.getBewohner() - base.getArbeiter();
		}

		@Override
		public int getId()
		{
			return base.getId();
		}

		@Override
		public String getName()
		{
			return base.getName();
		}

		@Override
		public void setCrew(int crew)
		{
			base.setBewohner(base.getArbeiter() + crew);
		}

		@Override
		public int getMaxCrew()
		{
			return -1;
		}

		@Override
		public User getOwner()
		{
			return base.getOwner();
		}
	}

	/**
	 * Konstruktor.
	 *
	 */
	public CrewtauschController()
	{
		super();

		setPageTitle("Crewtransfer");
	}

	private void validiereSchiff(Ship schiff)
	{
		User user = (User) getUser();

		if ((schiff == null) || (schiff.getOwner() != user) || (schiff.getId() < 0))
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht oder gehört dir nicht", Common.buildUrl("default", "module", "schiffe"));
		}

		if (schiff.getBattle() != null)
		{
			throw new ValidierungException("Dein Schiff befindet sich in einer Schlacht. Diese verlangt die volle Aufmerksamkeit der Crew weshalb ein Crewaustausch " +
					"momentan nicht durchgeführt werden kann.",
					Common.buildUrl("default", "module", "schiff", "ship", schiff.getId()));
		}
	}

	private Target ladeCrewtauschZiel(String mode, int tar, Ship ship)
	{
		org.hibernate.Session db = getDB();
		Target datat;

		switch (mode)
		{
			case "ss":
				Ship aship = (Ship) db.get(Ship.class, tar);

				if ((aship == null) || (aship.getId() < 0) || !ship.getLocation().sameSector(0, aship, 0))
				{
					throw new ValidierungException("Die beiden Schiffe befinden sich nicht im selben Sektor", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
				}

				if (aship.getBattle() != null)
				{
					throw new ValidierungException("Das Ziel befindet sich in einer Schlacht. Ihre Crew weigert sich " +
							"deshalb in die Shuttles einzusteigen und einen Crewaustausch durchzuführen.",
							Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
				}


				datat = new ShipTarget(aship);
				break;
			case "sb":
				Base abase = (Base) db.get(Base.class, tar);

				if ((abase == null) || !ship.getLocation().sameSector(0, abase, abase.getSize()))
				{
					throw new ValidierungException("Schiff und Basis befinden sich nicht im selben Sektor", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
				}

				datat = new BaseTarget(abase);
				break;
			default:
				throw new ValidierungException("Dieser Transportweg ist unbekannt (hoer mit dem scheiss URL-Hacking auf) - Versuch geloggt", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}

		if (ship.getOwner() != datat.getOwner())
		{
			throw new ValidierungException("Das gehört dir nicht!", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}

		return datat;
	}

	/**
	 * Transferiert Crew vom Ausgangsschiff zum Zielschiff/Basis.
	 *
	 * @param ship Die ID des Schiffes von dem/zu dem transferiert werden soll
	 * @param tar Die ID der Basis/des Schiffes, welches als Gegenstueck beim transfer fungiert. Die Bedeutung ist abhaengig vom Parameter <code>mode</code>
	 * @param mode Der Transfermodus. Entweder ss (Schiff zu Schiff) oder sb (Schiff zu Basis)
	 * @param send Die Anzahl der zu transferierenden Crew
	 */
	@Action(ActionType.DEFAULT)
	public void sendAction(Ship ship, int tar, String mode, int send)
	{
		validiereSchiff(ship);

		Target datat = ladeCrewtauschZiel(mode, tar, ship);

		TemplateEngine t = getTemplateEngine();

		if (send < 0)
		{
			send = 0;
		}
		if ((datat.getMaxCrew() > -1) && (send > datat.getMaxCrew() - datat.getCrew()))
		{
			send = datat.getMaxCrew() - datat.getCrew();
		}
		if (send > ship.getCrew())
		{
			send = ship.getCrew();
		}

		if (send > 0)
		{
			t.setVar("crewtausch.transfer", 1,
					"transfer.way.to", 1,
					"transfer.count", send);

			ship.setCrew(ship.getCrew() - send);
			datat.setCrew(datat.getCrew() + send);
			datat.finishTransfer();
			ship.recalculateShipStatus();
		}

		redirect();
	}

	/**
	 * Transfer in umgekehrter Richtung.
	 *
	 * @param ship Die ID des Schiffes von dem/zu dem transferiert werden soll
	 * @param tar Die ID der Basis/des Schiffes, welches als Gegenstueck beim transfer fungiert. Die Bedeutung ist abhaengig vom Parameter <code>mode</code>
	 * @param mode Der Transfermodus. Entweder ss (Schiff zu Schiff) oder sb (Schiff zu Basis)
	 * @param rec Die Anzahl der zu transferierenden Crew
	 */
	@Action(ActionType.DEFAULT)
	public void recAction(Ship ship, int tar, String mode, int rec)
	{
		validiereSchiff(ship);

		Target datat = ladeCrewtauschZiel(mode, tar, ship);

		TemplateEngine t = getTemplateEngine();

		if (rec < 0)
		{
			rec = 0;
		}
		int maxcrewf = ship.getTypeData().getCrew();
		if (rec > maxcrewf - ship.getCrew())
		{
			rec = maxcrewf - ship.getCrew();
		}
		if (rec > datat.getCrew())
		{
			rec = datat.getCrew();
		}

		if (rec > 0)
		{
			t.setVar("crewtausch.transfer", 1,
					"transfer.way.to", 0,
					"transfer.count", rec);

			ship.setCrew(ship.getCrew() + rec);
			datat.setCrew(datat.getCrew() - rec);
			datat.finishTransfer();
			ship.recalculateShipStatus();
		}

		redirect();
	}

	/**
	 * Anzeige von Infos sowie Eingabe der zu transferierenden Crew.
	 *
	 * @param ship Die ID des Schiffes von dem/zu dem transferiert werden soll
	 * @param tar Die ID der Basis/des Schiffes, welches als Gegenstueck beim transfer fungiert. Die Bedeutung ist abhaengig vom Parameter <code>mode</code>
	 * @param mode Der Transfermodus. Entweder ss (Schiff zu Schiff) oder sb (Schiff zu Basis)
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(Ship ship, int tar, String mode)
	{
		validiereSchiff(ship);

		Target datat = ladeCrewtauschZiel(mode, tar, ship);

		TemplateEngine t = getTemplateEngine();

		t.setVar("ship.id", ship.getId(),
				"ship.name", Common._plaintitle(ship.getName()),
				"ship.crew", ship.getCrew(),
				"ship.maxcrew", ship.getTypeData().getCrew(),
				"target.id", datat.getId(),
				"target.name", datat.getName(),
				"target.crew", datat.getCrew(),
				"target.maxcrew", (datat.getMaxCrew() > -1 ? datat.getMaxCrew() : "&#x221E;"),
				"global.mode", mode,
				"global.mode.ss", mode.equals("ss"),
				"global.mode.sb", mode.equals("sb"),
				"target.send", datat.getMaxCrew() > -1 ? datat.getMaxCrew() - datat.getCrew() : 0,
				"ship.receive", ship.getTypeData().getCrew() - ship.getCrew());
	}
}
