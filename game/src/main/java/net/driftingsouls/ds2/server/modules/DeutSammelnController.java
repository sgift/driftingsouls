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

import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.FleetMgmtService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sammelt mit einem Tanker in einem Nebel Deuterium.
 *
 * @author Christopher Jung
 */
@Module(name = "deutsammeln")
public class DeutSammelnController extends Controller
{
	private final TemplateViewResultFactory templateViewResultFactory;
	private final ShipService shipService;
	private final FleetMgmtService fleetMgmtService;
	private final ShipActionService shipActionService;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public DeutSammelnController(TemplateViewResultFactory templateViewResultFactory, ShipService shipService, FleetMgmtService fleetMgmtService, ShipActionService shipActionService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.shipService = shipService;
		this.fleetMgmtService = fleetMgmtService;
		this.shipActionService = shipActionService;

		setPageTitle("Deut. sammeln");
	}

	private List<Ship> erzeugeSchiffsliste(Ship ship, ShipFleet fleet)
	{
		List<Ship> ships = new ArrayList<>();
		User user = (User) getUser();

		if (fleet == null)
		{
			if ((ship == null) || (ship.getOwner() != user))
			{
				throw new ValidierungException("Das angegebene Schiff existiert nicht", Common.buildUrl("default", "module", "schiffe"));
			}

			ships.add(ship);
		}
		else
		{
			if (fleetMgmtService.getOwner(fleet) != user)
			{
				throw new ValidierungException("Die angegebene Flotte existiert nicht", Common.buildUrl("default", "module", "schiffe"));
			}
			ships.addAll(fleetMgmtService.getShips(fleet));
		}

		return ships;
	}

	private Nebel ermittleNebelFuerSchiffsliste(List<Ship> schiffe)
	{
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", schiffe.get(0).getId());
		Nebel nebel = em.find(Nebel.class, new MutableLocation(schiffe.get(0)));
		if (nebel == null)
		{
			throw new ValidierungException("Der Nebel befindet sich nicht im selben Sektor wie das Schiff", errorurl);
		}
		if (!nebel.getType().isDeuteriumNebel())
		{
			throw new ValidierungException("In diesem Nebel k&ouml;nnen sie kein Deuterium sammeln", errorurl);
		}

		return nebel;
	}

	private void filtereSchiffsliste(List<Ship> schiffe, Nebel nebel)
	{
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", schiffe.get(0).getId());

		String lastError = null;
		for (Iterator<Ship> iter = schiffe.iterator(); iter.hasNext(); )
		{
			Ship ship = iter.next();
			if (!nebel.getLocation().sameSector(0, ship, 0))
			{
				lastError = "Der Nebel befindet sich nicht im selben Sektor wie das Schiff";
				iter.remove();
				continue;
			}

			ShipTypeData shiptype = ship.getTypeData();
			if (shiptype.getDeutFactor() <= 0)
			{
				lastError = "Dieser Schiffstyp kann kein Deuterium sammeln";
				iter.remove();
				continue;
			}

			if (ship.getCrew() < (shiptype.getCrew() / 2))
			{
				lastError = "Sie haben nicht genug Crew um Deuterium zu sammeln";
				iter.remove();
			}
		}

		if (schiffe.isEmpty())
		{
			throw new ValidierungException(lastError, errorurl);
		}
	}

	/**
	 * Sammelnt fuer eine angegebene Menge Energie Deuterium aus einem Nebel.
	 *
	 * @param ship Der Tanker
	 * @param fleet Die Tankerflotte
	 * @param e Die Menge Energie, fuer die Deuterium gesammelt werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult sammelnAction(Ship ship, ShipFleet fleet, long e)
	{
		List<Ship> shipList = erzeugeSchiffsliste(ship, fleet);
		Nebel nebel = ermittleNebelFuerSchiffsliste(shipList);
		filtereSchiffsliste(shipList, nebel);

		StringBuilder message = new StringBuilder();

		for (Ship aship : shipList)
		{
			message.append(Common._plaintitle(aship.getName())).append(" (").append(aship.getId()).append("): ");

			long saugdeut = shipService.sammelDeuterium(aship, nebel, e);
			if (saugdeut <= 0)
			{
				message.append("Es konnte kein weiteres Deuterium gesammelt werden<br />");
			}
			else
			{
				shipActionService.recalculateShipStatus(aship);
				message.append("<img src=\"").append(Cargo.getResourceImage(Resources.DEUTERIUM)).append("\" alt=\"\" />").append(saugdeut).append(" für <img src=\"./data/interface/energie.gif\" alt=\"Energie\" />").append(e).append(" gesammelt<br />");
			}
		}

		return new RedirectViewResult("default").withMessage(message.toString());
	}

	/**
	 * Zeigt eine Eingabemaske an, in der angegeben werden kann,
	 * fuer wieviel Energie Deuterium gesammelt werden soll.
	 *
	 * @param ship Der Tanker
	 * @param fleet Die Tankerflotte
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(Ship ship, ShipFleet fleet, RedirectViewResult redirect)
	{
		List<Ship> shipList = erzeugeSchiffsliste(ship, fleet);
		Nebel nebel = ermittleNebelFuerSchiffsliste(shipList);
		filtereSchiffsliste(shipList, nebel);

		TemplateEngine t = templateViewResultFactory.createFor(this);
		t.setVar("deutsammeln.message", redirect != null ? redirect.getMessage() : null);

		long deutfactorSum = 0;
		int maxE = 0;
		for (Ship aship : shipList)
		{
			long deutfactor = aship.getTypeData().getDeutFactor();
			deutfactor = nebel.getType().modifiziereDeutFaktor(deutfactor);
			deutfactorSum += deutfactor;
			if (maxE < aship.getEnergy())
			{
				maxE = aship.getEnergy();
			}
		}

		t.setVar("deuterium.image", Cargo.getResourceImage(Resources.DEUTERIUM),
				"ship.type.deutfactor", Common.ln(deutfactorSum / (double) shipList.size()),
				"ship.id", shipList.get(0).getId(),
				"fleet.id", shipList.size() > 1 ? shipList.get(0).getFleet().getId() : 0,
				"ship.e", maxE);

		return t;
	}
}
