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
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Transferiert Offiziere von und zu Schiffen/Basen.
 *
 * @author Christopher Jung
 */
@Module(name = "tc")
public class TCController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public TCController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Offizierstransfer");
	}

	private void validiereSchiff(Ship ship)
	{
		if ((ship == null) || (ship.getOwner() != getUser()) || (ship.getId() < 0))
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht oder gehöt ihnen nicht", Common.buildUrl("default", "module", "schiffe"));
		}

		if (ship.getBattle() != null)
		{
			throw new ValidierungException("Das angegebene Schiff befindet sich in einer Schlacht", Common.buildUrl("default", "module", "schiffe"));
		}
	}

	/**
	 * Offiziersliste eines Objekts ausgeben.
	 *
	 * @param mode Transfermodus (shipToShip, baseToShip usw)
	 * @param ziel Der Aufenthaltsort der Offiziere
	 */
	private void echoOffiList(TemplateEngine t, String mode, Object ziel)
	{
		t.setVar("tc.selectoffizier", 1,
				"tc.mode", mode);

		t.setBlock("_TC", "tc.offiziere.listitem", "tc.offiziere.list");

		List<Offizier> offiList = Offizier.getOffiziereByDest(ziel);
		for (Offizier offi : offiList)
		{
			if (offi.isTraining())
			{
				continue;
			}
			t.setVar("tc.offizier.picture", offi.getPicture(),
					"tc.offizier.id", offi.getID(),
					"tc.offizier.name", Common._plaintitle(offi.getName()),
					"tc.offizier.ability.ing", offi.getAbility(Offizier.Ability.ING),
					"tc.offizier.ability.nav", offi.getAbility(Offizier.Ability.NAV),
					"tc.offizier.ability.waf", offi.getAbility(Offizier.Ability.WAF),
					"tc.offizier.ability.sec", offi.getAbility(Offizier.Ability.SEC),
					"tc.offizier.ability.com", offi.getAbility(Offizier.Ability.COM),
					"tc.offizier.special", offi.getSpecial().getName());

			t.parse("tc.offiziere.list", "tc.offiziere.listitem", true);
		}
	}

	/**
	 * Transferiert einen Offizier von einem Schiff zu einem Schiff.
	 *
	 * @param ship Die ID des Schiffes, das Ausgangspunkt des Transfers ist.
	 * @param conf "ok", falls eine Sicherheitsabfrage bestaetigt werden soll
	 * @param off Die ID des zu transferierenden Offiziers, falls mehr als ein Offizier zur Auswahl steht
	 * @param tarShip Die ID des Ziels des Transfers
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine shipToShipAction(Ship ship, String conf, int off, @UrlParam(name = "target") Ship tarShip)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		validiereSchiff(ship);
		t.setVar("global.shipid", ship.getId());

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		if ((tarShip == null) || (tarShip.getId() < 0))
		{
			throw new ValidierungException("Das angegebene Zielschiff existiert nicht", errorurl);
		}

		t.setVar("tc.ship", ship.getId(),
				"tc.target", tarShip.getId(),
				"tc.target.name", tarShip.getName(),
				"tc.target.isown", (tarShip.getOwner() == user),
				"tc.stos", 1,
				"tc.mode", "shipToShip");

		if (!ship.getLocation().sameSector(0, tarShip.getLocation(), 0))
		{
			throw new ValidierungException("Die beiden Schiffe befinden sich nicht im selben Sektor", errorurl);
		}

		if (tarShip.getBattle() != null)
		{
			throw new ValidierungException("Das Zielschiff befindet sich in einer Schlacht", Common.buildUrl("default", "module", "schiffe"));
		}

		long officount = ((Number) db.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest AND owner=:owner")
				.setEntity("dest", ship)
				.setEntity("owner", user)
				.iterate().next()
		).longValue();

		if (officount == 0)
		{
			throw new ValidierungException("Das Schiff hat keinen Offizier an Bord", errorurl);
		}

		//IFF-Check
		boolean disableIFF = (tarShip.getStatus().contains("disable_iff"));
		if (disableIFF)
		{
			throw new ValidierungException("Sie k&ouml;nnen keinen Offizier zu diesem Schiff transferieren", errorurl);
		}

		ShipTypeData tarShipType = tarShip.getTypeData();

		// Schiff gross genug?
		if (tarShipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE)
		{
			throw new ValidierungException("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
		}

		// Check ob noch fuer einen weiteren Offi platz ist
		int maxoffis = 1;
		if (tarShipType.hasFlag(ShipTypeFlag.OFFITRANSPORT))
		{
			maxoffis = tarShipType.getCrew();
		}

		long tarOffiCount = ((Number) db.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest AND owner=:owner")
				.setEntity("dest", tarShip)
				.setEntity("owner", tarShip.getOwner())
				.iterate().next()
		).longValue();
		if (tarOffiCount >= maxoffis)
		{
			throw new ValidierungException("Das Schiff hat bereits die maximale Anzahl Offiziere an Bord", errorurl);
		}

		// Offiziersliste bei bedarf ausgeben
		if ((officount > 1) && (off == 0))
		{
			echoOffiList(t, "shipToShip", ship);
			return t;
		}

		// Offizier laden
		Offizier offizier;
		if (off != 0)
		{
			offizier = Offizier.getOffizierByID(off);
		}
		else
		{
			offizier = ship.getOffizier();
		}

		if ((offizier == null) || (offizier.getOwner() != user))
		{
			throw new ValidierungException("Der angegebene Offizier existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
		}

		if (offizier.isTraining() || offizier.getStationiertAufSchiff() == null || offizier.getStationiertAufSchiff().getId() != ship.getId())
		{
			throw new ValidierungException("Der angegebene Offizier befindet sich nicht auf dem Schiff", errorurl);
		}

		t.setVar("tc.offizier.name", Common._plaintitle(offizier.getName()));

		// Confirm?
		if ((tarShip.getOwner() != user) && !conf.equals("ok"))
		{
			t.setVar("tc.confirm", 1);

			return t;
		}

		User tarUser = tarShip.getOwner();
		
		// Transfer!
		offizier.stationierenAuf(tarShip);
		offizier.setOwner(tarUser);

		ship.recalculateShipStatus();
		tarShip.recalculateShipStatus();
		
		if(tarUser != user){
			String msg = "Die " + ship.getName() + " ("+ship.getId()+") hat den Offizier " + offizier.getName() + " (" + offizier.getID() + ") an die [ship="+tarShip.getId()+"]"+tarShip.getName() + " ("+tarShip.getId()+")[/ship] &uuml;bergeben.";
			PM.send(user, tarUser.getId(), "Offizier &uuml;bergeben", msg);
		}

		return t;
	}

	/**
	 * Transferiert einen Offizier von einem Schiff zu einer Basis.
	 *
	 * @param ship Die ID des Schiffes, das Ausgangspunkt des Transfers ist.
	 * @param conf "ok", falls eine Sicherheitsabfrage bestaetigt werden soll
	 * @param off Die ID des zu transferierenden Offiziers, falls mehr als ein Offizier zur Auswahl steht
	 * @param tarBase Die ID des Ziels des Transfers
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine shipToBaseAction(Ship ship, int off, String conf, @UrlParam(name = "target") Base tarBase)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		validiereSchiff(ship);
		t.setVar("global.shipid", ship.getId());


		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		t.setVar("tc.ship", ship.getId(),
				"tc.target", tarBase.getId(),
				"tc.target.name", tarBase.getName(),
				"tc.target.isown", (tarBase.getOwner() == user),
				"tc.stob", 1,
				"tc.mode", "shipToBase");

		if (!ship.getLocation().sameSector(0, tarBase.getLocation(), tarBase.getSize()))
		{
			throw new ValidierungException("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
		}

		long officount = ((Number) db.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest AND owner=:owner")
				.setEntity("dest", ship)
				.setEntity("owner", user)
				.iterate().next()
		).longValue();
		if (officount == 0)
		{
			throw new ValidierungException("Das Schiff hat keinen Offizier an Bord", errorurl);
		}

		// bei bedarf offiliste ausgeben
		if ((officount > 1) && (off == 0))
		{
			echoOffiList(t, "shipToBase", ship);
			return t;
		}

		// Offi laden
		Offizier offizier;
		if (off != 0)
		{
			offizier = Offizier.getOffizierByID(off);
		}
		else
		{
			offizier = ship.getOffizier();
		}

		if ((offizier == null) || (offizier.getOwner() != user))
		{
			throw new ValidierungException("Der angegebene Offizier existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
		}

		if (offizier.isTraining() || offizier.getStationiertAufSchiff() == null || offizier.getStationiertAufSchiff().getId() != ship.getId())
		{
			throw new ValidierungException("Der angegebene Offizier befindet sich nicht auf dem Schiff", errorurl);
		}

		t.setVar("tc.offizier.name", Common._plaintitle(offizier.getName()));

		// Confirm ?
		if ((tarBase.getOwner() != user) && (!"ok".equals(conf)))
		{
			t.setVar("tc.confirm", 1);

			return t;
		}

		User tarUser = tarBase.getOwner();

		// Transfer !
		offizier.stationierenAuf(tarBase);
		offizier.setOwner(tarUser);

		ship.recalculateShipStatus();

		return t;
	}

	/**
	 * Transfieriert Offiziere (sofern genug vorhanden) Offiziere von einer Basis
	 * zu einer Flotte.
	 *
	 * @param ship Die ID des Schiffes, das Ausgangspunkt des Transfers ist.
	 * @param upBase Die ID des Ziels des Transfers
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine baseToFleetAction(Ship ship, @UrlParam(name = "target") Base upBase)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		validiereSchiff(ship);
		t.setVar("global.shipid", ship.getId());


		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		t.setVar("tc.ship", ship.getId());

		if ((upBase == null) || (upBase.getOwner() != user))
		{
			throw new ValidierungException("Die angegebene Basis existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
		}

		if (!ship.getLocation().sameSector(0, upBase.getLocation(), upBase.getSize()))
		{
			throw new ValidierungException("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
		}

		if (ship.getFleet() == null)
		{
			throw new ValidierungException("Das Schiff befinden sich in keiner Flotte", errorurl);
		}

		t.setVar("tc.captainzuweisen", 1,
				"tc.ship", ship.getId(),
				"tc.target", upBase.getId());

		List<Offizier> offilist = Offizier.getOffiziereByDest(upBase);
		offilist.removeIf(Offizier::isTraining);

		int shipcount = 0;

		List<?> shiplist = db.createQuery("from Ship where fleet=:fleet and owner=:owner and system=:sys and x=:x and y=:y and " +
				"locate('offizier',status)=0")
				.setEntity("fleet", ship.getFleet())
				.setEntity("owner", user)
				.setInteger("sys", ship.getSystem())
				.setInteger("x", ship.getX())
				.setInteger("y", ship.getY())
				.setMaxResults(offilist.size())
				.list();
		for (Object aShiplist : shiplist)
		{
			Ship aship = (Ship) aShiplist;
			ShipTypeData shipType = aship.getTypeData();
			if (shipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE)
			{
				continue;
			}

			Offizier offi = offilist.remove(0);
			offi.stationierenAuf(aship);

			aship.recalculateShipStatus();

			shipcount++;
		}

		t.setVar("tc.message", shipcount + " Offiziere wurden transferiert");

		return t;
	}

	/**
	 * Transfieriert Offiziere von einer Basis zu einem Schiff.
	 *
	 * @param ship Die ID des Schiffes, das Ausgangspunkt des Transfers ist.
	 * @param off Die ID des zu transferierenden Offiziers, falls mehr als ein Offizier zur Auswahl steht
	 * @param upBase Die ID des Ziels des Transfers
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine baseToShipAction(Ship ship, int off, @UrlParam(name = "target") Base upBase)
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) getUser();

		validiereSchiff(ship);
		t.setVar("global.shipid", ship.getId());


		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		t.setVar("tc.ship", ship.getId());

		if ((upBase == null) || (upBase.getOwner() != user))
		{
			throw new ValidierungException("Die angegebene Basis existiert nicht oder geh&ouml;rt nicht ihnen");
		}

		if (!ship.getLocation().sameSector(0, upBase.getLocation(), upBase.getSize()))
		{
			throw new ValidierungException("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
		}

		ShipTypeData shipType = ship.getTypeData();
		if (shipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE)
		{
			throw new ValidierungException("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
		}

		t.setVar("tc.captainzuweisen", 1,
				"tc.offizier", off,
				"tc.ship", ship.getId(),
				"tc.target", upBase.getId());

		// Wenn noch kein Offizier ausgewaehlt wurde -> Liste der Offiziere in der Basis anzeigen
		if (off == 0)
		{
			echoOffiList(t, "baseToShip", upBase);

			if (ship.getFleet() != null)
			{
				long count = ((Number) db.createQuery("select count(*) from Ship where fleet=:fleet and owner=:owner and system=:sys and x=:x and " +
						"y=:y and locate('offizier',status)=0")
						.setEntity("fleet", ship.getFleet())
						.setEntity("owner", user)
						.setInteger("sys", ship.getSystem())
						.setInteger("x", ship.getX())
						.setInteger("y", ship.getY())
						.iterate().next()
				).longValue();
				if (count > 1)
				{
					t.setVar("show.fleetupload", 1,
							"tc.fleetmode", "baseToFleet");
				}
			}
		}
		//ein Offizier wurde ausgewaehlt -> transferieren
		else
		{
			Offizier offizier = (Offizier) getDB().get(Offizier.class, off);
			if (offizier == null)
			{
				throw new ValidierungException("Der angegebene Offizier existiert nicht", errorurl);
			}

			if (offizier.isTraining() || offizier.getStationiertAufBasis() == null || offizier.getStationiertAufBasis().getId() != upBase.getId())
			{
				throw new ValidierungException("Der angegebene Offizier ist nicht in der Basis stationiert", errorurl);
			}

			// Check ob noch fuer einen weiteren Offi platz ist
			ShipTypeData tarShipType = ship.getTypeData();

			long offi = ((Number) getDB().createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest")
					.setEntity("dest", ship)
					.iterate().next()
			).longValue();

			int maxoffis = 1;
			if (tarShipType.hasFlag(ShipTypeFlag.OFFITRANSPORT))
			{
				maxoffis = tarShipType.getCrew();
			}

			if (offi >= maxoffis)
			{
				throw new ValidierungException("Das Schiff hat bereits die maximale Anzahl Offiziere an Bord", errorurl);
			}

			t.setVar("tc.offizier.name", Common._plaintitle(offizier.getName()));

			offizier.stationierenAuf(ship);

			ship.recalculateShipStatus();
		}

		return t;
	}

	@Action(ActionType.DEFAULT)
	public void defaultAction()
	{
		// EMPTY
	}
}
