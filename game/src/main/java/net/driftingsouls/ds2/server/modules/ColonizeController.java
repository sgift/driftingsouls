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
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.BuildingService;
import net.driftingsouls.ds2.server.services.DismantlingService;
import net.driftingsouls.ds2.server.services.UserValueService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kolonisieren eines Asteroiden mittels eines Colonizers (Schiff).
 *
 * @author Christopher Jung
 */
@Module(name = "colonize")
public class ColonizeController extends Controller
{
	private final TemplateViewResultFactory templateViewResultFactory;
	private final BuildingService buildingService;
	private final UserValueService userValueService;
	private final DismantlingService dismantlingService;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public ColonizeController(TemplateViewResultFactory templateViewResultFactory, BuildingService buildingService, UserValueService userValueService, DismantlingService dismantlingService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.buildingService = buildingService;
		this.userValueService = userValueService;
		this.dismantlingService = dismantlingService;

		setPageTitle("Kolonisieren");
	}

	private void validiereSchiffUndBasis(Ship ship, Base base)
	{
		User user = (User) getUser();
		if ((ship == null) || (ship.getOwner() != user))
		{
			throw new ValidierungException("Fehler: Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "schiffe"));
		}

		ShipTypeData shiptype = ship.getTypeData();
		if (!shiptype.hasFlag(ShipTypeFlag.COLONIZER))
		{
			throw new ValidierungException("Fehler: Das angegebene Schiff kann keine Planeten kolonisieren", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}

		if ((base == null) || (base.getOwner().getId() != 0))
		{
			throw new ValidierungException("Fehler: Der angegebene Asteroid existiert nicht oder geh&ouml;rt bereits einem anderen Spieler", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}

		if (!ship.getLocation().sameSector(0, base.getLocation(), base.getSize()))
		{
			throw new ValidierungException("Fehler: Der Asteroid befindet sich nicht im selben Sektor wie das Schiff", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}
	}

	/**
	 * Der Kolonisiervorgang.
	 *  @param ship Die Schiffs-ID des Colonizers
	 * @param base Die Basis-ID des zu kolonisierenden Asteroiden
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(Ship ship, @UrlParam(name = "col") Base base)
	{
		validiereSchiffUndBasis(ship, base);

		User user = (User) getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		t.setVar("ship.id", ship.getId());

		Integer[] bebauung = base.getBebauung();
		Integer[] bebon = base.getActive();

		Map<Integer, Integer> bases = new HashMap<>();
		bases.put(base.getSystem(), 1);
		int basecount = 0;

		for (Base aBase : user.getBases())
		{
			final int system = aBase.getSystem();
			Common.safeIntInc(bases, system);
			basecount += aBase.getMaxTiles();
		}
		basecount += base.getMaxTiles();

		if (basecount > userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_BASES_MAXTILES))
		{
			t.setVar("colonize.message", "<span style=\"color:#ff0000; font-weight:bold\">Kolonisierung unzul&auml;ssig, da dies die Gesamtzahl an zul&auml;ssigen Oberfl&auml;chenfeldern " + userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_BASES_MAXTILES) + " &uuml;bersteigen w&uuml;rde.</span>");

			return t;
		}

		StarSystem system = em.find(StarSystem.class, base.getSystem());

		if ((system.getMaxColonies() >= 0) &&
				(bases.get(base.getSystem()) >= system.getMaxColonies()))
		{
			t.setVar("colonize.message", "<span style=\"color:#ff0000\">Sie d&uuml;rfen lediglich " + system.getMaxColonies() + " Asteroiden in " + system.getName() + " (" + base.getSystem() + ") kolonisieren");

			return t;
		}

		int crew = ship.getCrew();
		int e = ship.getEnergy();

		/*
		 *
		 * Evt muessen einige Gebaeude entfernt werden, wenn der betreffende Spieler sonst zu viele haette
		 *
		 */

		nichtErlaubteGebaeudeEntfernen(base, user, bebauung, bebon);

		/*
		 *
		 * Nun den Asteroiden kolonisieren
		 *
		 */

		Cargo cargo = ship.getCargo();

		t.setBlock("_COLONIZE", "res.listitem", "res.list");

		ResourceList reslist = cargo.getResourceList();
		for (ResourceEntry res : reslist)
		{
			t.setVar("res.image", res.getImage(),
					"res.name", res.getName(),
					"res.cargo", res.getCargo1(),
					"user.sounds.mute", userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_USER_SOUNDS_MUTE),
					"user.sounds.volume", userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_USER_SOUNDS_VOLUME));
			t.parse("res.list", "res.listitem", true);
		}

		Cargo cargo2 = base.getCargo();
		cargo.addCargo(cargo2);

		for (Offizier offi : Offizier.getOffiziereByDest(base))
		{
			offi.stationierenAuf(base);
		}

		dismantlingService.destroy(ship);

		// Die Kommandozentrale setzen
		bebauung[0] = 1;
		bebon[0] = 1;

		base.setBebauung(bebauung);
		base.setActive(bebon);
		base.setCargo(cargo);
		base.setOwner(user);
		base.setBewohner(crew);
		base.setEnergy(e);

		for (Offizier offi : Offizier.getOffiziereByDest(base))
		{
			offi.setOwner(user);
		}

		t.setVar("base.id", base.getId());
		return t;
	}

	private void nichtErlaubteGebaeudeEntfernen(Base base, User user, Integer[] bebauung, Integer[] bebon)
	{
		//Anzahl der Gebaeude pro Spieler berechnen
		Map<Integer, Integer> ownerBuildingCount = new HashMap<>();

		for (Base aBase : user.getBases())
		{
			Integer[] abeb = aBase.getBebauung();
			for (Integer anAbeb : abeb)
			{
				if (ownerBuildingCount.containsKey(anAbeb))
				{
					ownerBuildingCount.put(anAbeb, ownerBuildingCount.get(anAbeb) + 1);
				}
				else
				{
					ownerBuildingCount.put(anAbeb, 1);
				}
			}
		}

		// Problematische Gebaeude ermitteln
		Map<Integer, Integer> problematicBuildings = new HashMap<>();
		List<Building> buildings = em.createQuery("from Building where perOwner>0", Building.class).getResultList();
		for (Building building: buildings)
		{
			problematicBuildings.put(building.getId(), building.getPerUserCount());
		}

		// Nun die Gebaeude auf dem Asti durchlaufen und bei Bedarf einige entfernen
		for (int index = 0; index < bebauung.length; index++)
		{
			final Integer building = bebauung[index];
			if (problematicBuildings.containsKey(building) && ownerBuildingCount.containsKey(building) &&
					(ownerBuildingCount.get(building) + 1 > problematicBuildings.get(building)))
			{
				bebauung[index] = 0;
				bebon[index] = 0;
				Building gebaeude = Building.getBuilding(building);
				buildingService.cleanup(gebaeude, base, building);
			}

			ownerBuildingCount.merge(building, 0, (id, count) -> count + 1);
		}
	}

}
