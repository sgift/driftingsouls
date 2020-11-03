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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Werft;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.*;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.services.HandelspostenService;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.UserService;
import net.driftingsouls.ds2.server.services.UserValueService;
import net.driftingsouls.ds2.server.ships.*;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schiffsmodul fuer die SRS-Sensoren.
 * @author Christopher Jung
 *
 */
@Controller
@Scope("request")
public class SensorsDefault implements SchiffPlugin {
	private static final Log log = LogFactory.getLog(SensorsDefault.class);

	@PersistenceContext
	private EntityManager em;

	private final HandelspostenService handelspostenService;
	private final UserService userService;
	private final BattleService battleService;
	private final Rassen races;
	private final BBCodeParser bbCodeParser;
	private final UserValueService userValueService;
	private final LocationService locationService;
	private final ShipService shipService;

	@Autowired
	public SensorsDefault(HandelspostenService handelspostenService, UserService userService, BattleService battleService, Rassen races, BBCodeParser bbCodeParser, UserValueService userValueService, LocationService locationService, ShipService shipService)
	{
		this.handelspostenService = handelspostenService;
		this.userService = userService;
		this.battleService = battleService;
		this.races = races;
		this.bbCodeParser = bbCodeParser;
		this.userValueService = userValueService;
		this.locationService = locationService;
		this.shipService = shipService;
	}

	@Action(ActionType.DEFAULT)
	public String action(Parameters caller, String order, int showonly, int showid) {
		SchiffController controller = caller.controller;

		if( !order.equals("") ) {
			if( !order.equals("id") && !order.equals("name") && !order.equals("owner") && !order.equals("shiptype") ) {
				order = "id";
			}
			User user = (User)controller.getUser();
			userValueService.setUserValue(user, WellKnownUserValue.TBLORDER_SCHIFF_SENSORORDER, order);
		}

		return "";
	}

	@Action(ActionType.DEFAULT)
	@Transactional(readOnly = true)
	public void output(Parameters caller, int showonly, int showid) {
		String pluginid = caller.pluginId;
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;

		User user = (User) ContextMap.getContext().getActiveUser();
		TemplateEngine t = caller.t;

		t.setFile("_PLUGIN_"+pluginid, "schiff.sensors.default.html");

		//Kein SRS oder nicht nutzbar? Ende Gelaende.
		if(!ship.canUseSrs()) {
			t.setVar("has.srs", false);
			t.parse(caller.target,"_PLUGIN_"+pluginid);
			return;
		}
		t.setVar("has.srs", true);

		t.setVar(
				"global.ship", ship.getId(),
				"global.pluginid", pluginid,
				"ship.sensors.location", locationService.displayCoordinates(ship.getLocation(), true),
				"global.awac", shiptype.hasFlag(ShipTypeFlag.SRS_AWAC) || shiptype.hasFlag(ShipTypeFlag.SRS_EXT_AWAC) );

		String order = userValueService.getUserValue(user, WellKnownUserValue.TBLORDER_SCHIFF_SENSORORDER);

		if( ( ship.getSensors() > 30 ) && ( ship.getCrew() >= shiptype.getMinCrew() / 4 ) )
		{
			t.setVar("global.goodscan",1);
		}
		else if( ship.getSensors() > 0 ) {
			t.setVar("global.badscan",1);
		}

		if( ship.getSensors() > 0 ) {
			t.setVar("global.scan",1);
		}

		t.setBlock("_SENSORS","bases.listitem","bases.list");
		t.setBlock("_SENSORS","battles.listitem","battles.list");
		t.setBlock("_SENSORS","sships.listitem","sships.list");
		t.setBlock("_SENSORS","sshipgroup.listitem","none");
		t.setVar("none","");

		/*
			Asteroiden
			-> Immer anzeigen, wenn die sensoren (noch so gerade) funktionieren
		*/
		if( ship.getSensors() > 0 ) {
			outputBases(caller, user, t, order);
		}

		//
		// Nebel,Jumpnodes und Schiffe nur anzeigen, wenn genuegend crew vorhanden und die Sensoren funktionsfaehig sind (>30)
		//
		if( ( ship.getSensors() > 30 ) && ( ship.getCrew() >= shiptype.getMinCrew() / 4 ) ) {
			/*
				Nebel
			*/

			outputNebel(caller, t);

			/*
				Jumpnodes
			*/

			outputJumpnodes(caller, user, t);

			/*
				Schlachten
			*/
			outputBattles(caller, user, t);

			/*
				Subraumspalten (durch Sprungantriebe)
			*/

			outputSubraumspalten(caller, t);

			/*
				Schiffe
			*/

			outputShips(caller, user, t, order, showonly, showid);
		}

		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

	private void outputShips(Parameters caller,
						User user, TemplateEngine t,
						String order, int showOnly, int showId)
	{
		List<Integer> fleetlist = null;
		ShipTypeData shiptype = caller.shiptype;
		Ship ship = caller.ship;

		// Cache fuer Jaegerflotten. Key ist die Flotten-ID. Value die Liste der Schiffs-IDs der Flotte
		Map<ShipFleet,List<Integer>> jaegerFleetCache = new HashMap<>();

		// Die ID des Schiffes, an dem das aktuell ausgewaehlte Schiff angedockt ist
		int currentDockID = 0;
		Ship baseShip = shipService.getBaseShip(ship);
		if(baseShip != null)
		{
			currentDockID = baseShip.getId();
		}

		int userWrapFactor = userValueService.getUserValue(user, WellKnownUserValue.TBLORDER_SCHIFF_WRAPFACTOR);

		// dockCount - die Anzahl der aktuell angedockten Schiffe
		final long dockCount = shipService.getDockedCount(ship);

		// superdock - Kann der aktuelle Benutzer alles andocken?
		boolean superdock = false;
		if( shiptype.getADocks() > dockCount ) {
			superdock = user.hasFlag( UserFlag.SUPER_DOCK );
		}

		// fullcount - Die Anzahl der freien Landeplaetze auf dem aktuell ausgewaehlten Traeger
		final long fullcount = shipService.getLandedCount(ship);

		// spaceToLand - Ist ueberhaupt noch Platz auf dem aktuell ausgewaehlten Traeger?
		final boolean spaceToLand = fullcount < shiptype.getJDocks();

		String thisorder = "s."+order;
		if( order.equals("id") ) {
			thisorder = "case when s.docked!='' then s.docked else s.id end";
		}
		if( order.equals("type") ) {
			thisorder = "s.shiptype";
		}

		List<Ship> ships;
		List<Ship> shipsInBattle;
		boolean firstentry = false;
		Map<String,Long> types = new HashMap<>();

		//Schiffe im Kampf auf diesem Feld fuer den Scanner mitladen
		shipsInBattle = em.createQuery("from Ship s inner join fetch s.owner " +
										"where s.id!= :id and s.id>0 and s.x= :x and s.y=:y and s.system= :sys and " +
											"s.battle is not null and locate('l ',s.docked)=0 " +
										"order by "+thisorder+",case when s.docked!='' then s.docked else s.id end, s.fleet", Ship.class)
									.setParameter("id", ship.getId())
									.setParameter("x", ship.getX())
									.setParameter("y", ship.getY())
									.setParameter("sys", ship.getSystem())
									.getResultList();

		// Soll nur ein bestimmter Schiffstyp angezeigt werden?
		if( showOnly != 0 ) {
			// IF(t1.docked!='',t1.docked+0.1,t1.id) as myorder
			ships = em.createQuery("from Ship s inner join fetch s.owner " +
					"where s.id!= :id and s.id>0 and s.x= :x and s.y= :y and s.system= :sys and " +
						"s.battle is null and " +
						"locate('l ',s.docked)=0 and s.shiptype= :showonly and s.owner= :showid and " +
						"locate('disable_iff',s.status)=0 "+
					"order by "+thisorder+",case when s.docked!='' then s.docked else s.id end,s.fleet", Ship.class)
				.setParameter("id", ship.getId())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("sys", ship.getSystem())
				.setParameter("showonly", showOnly)
				.setParameter("showid", showId)
				.getResultList();

			firstentry = true;
		}
		else {
			// wenn wir kein Wrap wollen, koennen wir uns das hier auch sparen

			if( userWrapFactor != 0 ) {
				// herausfinden wieviele Schiffe welches Typs im Sektor sind
				List<Object[]> typeList = em.createQuery("select count(*),s.shiptype,s.owner.id " +
						"from Ship s " +
						"where s.id!= :id and s.id>0 and s.x= :x and s.y= :y and s.system= :sys and s.battle is null and " +
							"locate('disable_iff',s.status)=0 and " +
							"locate('l ',s.docked)=0 " +
						"group by s.shiptype,s.owner", Object[].class)
					.setParameter("id", ship.getId())
					.setParameter("x", ship.getX())
					.setParameter("y", ship.getY())
					.setParameter("sys", ship.getSystem())
					.getResultList();

				for (Object[] data : typeList)
				{
					Long count = (Long) data[0];
					ShipType type = (ShipType) data[1];
					int owner = (Integer) data[2];
					types.put(type.getId() + "_" + owner, count);
				}
			}
			ships = em.createQuery("from Ship s inner join fetch s.owner " +
					"where s.id!= :id and s.id>0 and s.x= :x and s.y=:y and s.system= :sys and " +
						"s.battle is null and locate('l ',s.docked)=0 " +
					"order by "+thisorder+",case when s.docked!='' then s.docked else s.id end, s.fleet", Ship.class)
				.setParameter("id", ship.getId())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("sys", ship.getSystem())
				.getResultList();

		}

		final long fleetlesscount = em.createQuery("SELECT count(*) FROM Ship WHERE id > 0 AND system=:system AND x=:x AND y=:y AND owner=:owner AND shiptype=:shiptype AND LOCATE('l ',docked) = 0 AND LOCATE('disable_iff',status) = 0 AND fleet is null", Long.class)
				.setParameter("system", ship.getSystem())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("owner", ship.getOwner())
				.setParameter("shiptype", ship.getBaseType())
				.getSingleResult();

		long ownfleetcount = 0;
		if( ship.getFleet() != null )
		{
			ownfleetcount = em.createQuery("SELECT count(*) FROM Ship WHERE id > 0 AND system=:system AND x=:x AND y=:y AND owner=:owner AND shiptype=:shiptype AND LOCATE('l ',docked) = 0 AND LOCATE('disable_iff',status) = 0 AND fleet =:fleet", Long.class)
				.setParameter("system", ship.getSystem())
				.setParameter("x", ship.getX())
				.setParameter("y", ship.getY())
				.setParameter("owner", ship.getOwner())
				.setParameter("shiptype", ship.getBaseType())
				.setParameter("fleet", ship.getFleet())
				.getSingleResult();
		}
		List<Ship> ownShipList = new ArrayList<>();
		List<Ship> friendShipList = new ArrayList<>();
		List<Ship> enemyShipList = new ArrayList<>();

		//das Schiff selbst einfuegen. Das ist in der Liste ships nicht enthalten
		ownShipList.add(ship);
		friendShipList.add(ship);

		UserService.Relations relations = userService.getRelations(user);
		//Schiffe im Kampf fuer den Scanner auswerten:
		for (Object ship1 : shipsInBattle)
		{
			Ship aship = (Ship) ship1;
			if (aship.getOwner().getId() == user.getId()) //man selbst
			{
				ownShipList.add(aship);
				friendShipList.add(aship); //man ist auch mit sich selbst befreundet und das macht es unten einfacher in der Berechnung
			}
			else if (relations.beziehungZu(aship.getOwner())== User.Relation.FRIEND) //Freunde
			{
				friendShipList.add(aship);
			}
			else //alles andere ist feindlich
			{
				enemyShipList.add(aship);
			}
		}
		for (Object ship1 : ships)
		{
			Ship aship = (Ship) ship1;
			ShipTypeData ashiptype = aship.getTypeData();
			ShipTypeData mastertype = aship.getBaseType();
			if (aship.getOwner().getId() == user.getId()) //man selbst
			{
				ownShipList.add(aship);
				friendShipList.add(aship); //man ist auch mit sich selbst befreundet und das macht es unten einfacher in der Berechnung
			}
			else if (relations.beziehungZu(aship.getOwner())== User.Relation.FRIEND) //Freunde
			{
				friendShipList.add(aship);
			}
			else //alles andere ist feindlich
			{
				enemyShipList.add(aship);
			}

			final String typeGroupID = aship.getType() + "_" + aship.getOwner().getId();

			// Schiff nur als/in Gruppe anzeigen
			if ((showOnly == 0) && !aship.getStatus().contains("disable_iff") &&
					(userWrapFactor != 0) && (mastertype.getGroupwrap() != 0) &&
					(types.get(typeGroupID) >= mastertype.getGroupwrap() * userWrapFactor))
			{

				String groupidlist = "";
				if (aship.getOwner().getId() == user.getId())
				{
					groupidlist = em.createQuery("SELECT CONCAT(id, '|') FROM Ship WHERE id>0 AND system=:system AND x=:x AND y=:y AND owner=:owner AND shiptype=:shiptype AND LOCATE('l ',docked) = 0 AND LOCATE('disable_iff',status) = 0", String.class)
							.setParameter("system", ship.getSystem())
							.setParameter("x", ship.getX())
							.setParameter("y", ship.getY())
							.setParameter("owner", user)
							.setParameter("shiptype", aship.getBaseType())
							.getSingleResult();
				}

				t.start_record();
				t.setVar("sshipgroup.name", types.get(typeGroupID) + " x " + mastertype.getNickname(),
						"sshipgroup.idlist", groupidlist,
						"sshipgroup.type.id", aship.getType(),
						"sshipgroup.owner.id", aship.getOwner().getId(),
						"sshipgroup.owner.name", Common._title(bbCodeParser, aship.getOwner().getName()),
						"sshipgroup.type.name", mastertype.getNickname(),
						"sshipgroup.sublist", 0,
						"sshipgroup.type.image", mastertype.getPicture(),
						"sshipgroup.own", aship.getOwner().getId() == user.getId(),
						"sshipgroup.count", types.get(typeGroupID) + (ship.getType() == aship.getType() ? 1 : 0) - ownfleetcount,
						"sshipgroup.fleetlesscount", fleetlesscount);

				if (aship.getOwner().getId() == user.getId())
				{
					t.setVar("sshipgroup.ownship", 1);
				}
				else
				{
					t.setVar("sshipgroup.ownship", 0);
				}

				t.parse("sships.list", "sshipgroup.listitem", true);
				t.stop_record();
				t.clear_record();
				types.put(typeGroupID, -1L);    // einmal anzeigen reicht
			}
			else if ((showOnly != 0) || !types.containsKey(typeGroupID) || (types.get(typeGroupID) != -1))
			{
				if ((showOnly != 0) && firstentry)
				{
					int count = ships.size();

					t.setVar("sshipgroup.name", count + " x " + mastertype.getNickname(),
							"sshipgroup.type.id", aship.getType(),
							"sshipgroup.owner.id", aship.getOwner().getId(),
							"sshipgroup.owner.name", Common._title(bbCodeParser, aship.getOwner().getName()),
							"sshipgroup.type.name", mastertype.getNickname(),
							"sshipgroup.sublist", 1,
							"sshipgroup.type.image", mastertype.getPicture(),
							"sshipgroup.own", aship.getOwner().getId() == user.getId(),
							"sshipgroup.count", count + (ship.getType() == aship.getType() ? 1 : 0) - ownfleetcount,
							"sshipgroup.fleetlesscount", fleetlesscount);

					if (aship.getOwner().getId() == user.getId())
					{
						t.setVar("sshipgroup.ownship", 1);
					}
					else
					{
						t.setVar("sshipgroup.ownship", 0);
					}
					t.parse("sships.list", "sshipgroup.listitem", true);

					firstentry = false;
				}
				t.start_record();
				t.setVar("sships.id", aship.getId(),
						"sships.owner.id", aship.getOwner().getId(),
						"sships.owner.name", Common._title(bbCodeParser, aship.getOwner().getName()),
						"sships.name", Common._plaintitle(aship.getName()),
						"sships.type.id", aship.getType(),
						"sships.hull", Common.ln(aship.getHull()),
						"sships.ablativearmor", Common.ln(aship.getAblativeArmor()),
						"sships.shields", Common.ln(aship.getShields()),
						"sships.fleet.id", aship.getFleet() != null ? aship.getFleet().getId() : 0,
						"sships.type.name", ashiptype.getNickname().replace("'", ""),
						"sships.type.image", ashiptype.getPicture(),
						"sships.docked.id", aship.isDocked() ? shipService.getBaseShip(aship).getId() : null);

				boolean disableIFF = aship.getStatus().contains("disable_iff");
				t.setVar("sships.disableiff", disableIFF);

				if (aship.getOwner().getId() == user.getId())
				{
					t.setVar("sships.ownship", 1);
				}
				else
				{
					t.setVar("sships.ownship", 0);
				}

				if (disableIFF)
				{
					t.setVar("sships.owner.name", "Unbekannt");
				}

				if (aship.getFleet() != null)
				{
					t.setVar("sships.fleet.name", Common._plaintitle(aship.getFleet().getName()));
				}
				// Gedockte Schiffe zuordnen (gelandete brauchen hier nicht beruecksichtigt werden, da sie von der Query bereits aussortiert wurden)
				if (aship.isDocked())
				{
					Ship master = shipService.getBaseShip(aship);
					if (master == null)
					{
						log.warn("Schiff " + aship.getId() + " hat ungueltigen Dockeintrag.");
					}
					else
					{
						t.setVar("sships.docked.name", master.getName());
					}
				}

				// Anzeige Heat (Standard)
				if (shiptype.hasFlag(ShipTypeFlag.SRS_EXT_AWAC))
				{
					t.setVar("sships.heat", aship.getHeat());

					// Anzeige Heat
					if (aship.getHeat() == 0)
					{
						t.setVar("sships.heat.none", 1);
					}
					if ((aship.getHeat() > 0) && (aship.getHeat() <= 100))
					{
						t.setVar("sships.heat.medium", 1);
					}
					else if (aship.getHeat() > 100)
					{
						t.setVar("sships.heat.high", 1);
					}

					// Anzeige Crew
					if ((aship.getCrew() == 0) && (ashiptype.getCrew() != 0))
					{
						t.setVar("sships.nocrew", 1);
					}
					else if (aship.getCrew() > 0)
					{
						t.setVar("sships.crew", aship.getCrew());
					}

					// Anzeige Energie
					if (aship.getEnergy() == 0)
					{
						t.setVar("sships.noe", 1);
					}
					else if (aship.getEnergy() > 0)
					{
						t.setVar("sships.e", aship.getEnergy());
					}
				}
				else if (shiptype.hasFlag(ShipTypeFlag.SRS_AWAC))
				{
					t.setVar("global.standartawac", 1);

					if (aship.getHeat() > 100)
					{
						t.setVar("sships.heat.high", 1);
					}
					else if (aship.getHeat() > 40)
					{
						t.setVar("sships.heat.medium", 1);
					}
					else if (aship.getHeat() > 0)
					{
						t.setVar("sships.heat.low", 1);
					}
					else
					{
						t.setVar("sships.heat.none", 1);
					}
				}

				//Angreifen
				if (!disableIFF && (aship.getOwner().getId() != user.getId()) && (aship.getBattle() == null) && shiptype.isMilitary() && !(ashiptype.getShipClass() == ShipClasses.FELSBROCKEN))
				{
					if (user.getAlly() == null || (aship.getOwner().getAlly() != user.getAlly()))
					{
						t.setVar("sships.action.angriff", 1);
					}
				}

				// Anfunken
				if (handelspostenService.isKommunikationMoeglich(aship, ship))
				{
					t.setVar("sships.action.communicate", aship.getId());
				}

				// Springen (Knossosportal)
				if (!aship.getJumpTarget().isEmpty())
				{
					/*
						Ermittlung der Sprungberechtigten
						moeglich sind: default,all,user,ally,ownally,group
					 */
					String[] target = StringUtils.split(aship.getJumpTarget(), '|');
					String[] targetuser = StringUtils.split(target[2], ':');
					switch (targetuser[0])
					{
						case "all":
							t.setVar("sships.action.jump", 1);
							break;
						case "ally":
							if ((user.getAlly() != null) && (Integer.parseInt(targetuser[1]) == user.getAlly().getId()))
							{
								t.setVar("sships.action.jump", 1);
							}
							break;
						case "user":
							if (Integer.parseInt(targetuser[1]) == user.getId())
							{
								t.setVar("sships.action.jump", 1);
							}
							break;
						case "ownally":
							if ((user.getAlly() != null) && (aship.getOwner().getAlly() == user.getAlly()))
							{
								t.setVar("sships.action.jump", 1);
							}
							break;
						case "group":
							String[] userlist = targetuser[1].split(",");
							if (Common.inArray(Integer.toString(user.getId()), userlist))
							{
								t.setVar("sships.action.jump", 1);
							}
							break;
						default:
							// Default: Selbe Allianz wie der Besitzer oder selbst der Besitzer
							if (((user.getAlly() != null) && (aship.getOwner().getAlly() == user.getAlly())) ||
									((user.getAlly() == null) && (aship.getOwner().getId() == user.getId())))
							{

								t.setVar("sships.action.jump", 1);
							}
							break;
					}
				}

				//Handeln, Pluendernlink, Waren transferieren
				if (aship.isTradepost() && handelspostenService.isTradepostVisible(aship, user) )
				{
					t.setVar("sships.action.trade", 1);
				}
				else if (!disableIFF && (aship.getOwner().getId() == -1) && (ashiptype.getShipClass() == ShipClasses.SCHROTT || aship.getStatus().contains("pluenderbar")))
				{
					t.setVar("sships.action.transferpluender", 1);
				}
				else if ((!disableIFF || (aship.getOwner().getId() == user.getId())) && !(ashiptype.getShipClass() == ShipClasses.FELSBROCKEN))
				{
					t.setVar("sships.action.transfer", 1);
				}

				//Bemannen, Kapern, Einheiten tranferieren
				if (!disableIFF && (aship.getOwner().getId() != user.getId()) && ashiptype.getShipClass().isKaperbar() &&
						((aship.getOwner().getId() != -1) || (ashiptype.getShipClass() == ShipClasses.SCHROTT )))
				{
					if ((user.getAlly() == null) || (aship.getOwner().getAlly() != user.getAlly()))
					{
						if (!ashiptype.hasFlag(ShipTypeFlag.NICHT_KAPERBAR))
						{
							t.setVar("sships.action.kapern", 1);
						}
						else
						{
							t.setVar("sships.action.pluendern", 1);
						}
					}
				}
				else if (!disableIFF && (aship.getOwner().getId() == user.getId()) && (ashiptype.getCrew() > 0))
				{
					t.setVar("sships.action.crewtausch", 1);
				}
				if ((aship.getOwner().getId() == user.getId()) && ashiptype.getUnitSpace() > 0 && shiptype.getUnitSpace() > 0)
				{
					t.setVar("sships.action.unittausch", 1);
				}

				//Offiziere: Captain transferieren
				boolean hasoffizier = aship.getStatus().contains("offizier");
				if (!disableIFF && (caller.offizier != null) && (!hasoffizier || ashiptype.hasFlag(ShipTypeFlag.OFFITRANSPORT)) && !(ashiptype.getShipClass() == ShipClasses.FELSBROCKEN))
				{
					if (ashiptype.getSize() > ShipType.SMALL_SHIP_MAXSIZE)
					{
						boolean ok = true;
						if (ashiptype.hasFlag(ShipTypeFlag.OFFITRANSPORT))
						{
							long officount = em.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest", Long.class)
									.setParameter("dest", aship)
									.getSingleResult();

							if (officount >= ashiptype.getCrew())
							{
								ok = false;
							}
						}

						if (ok)
						{
							t.setVar("sships.action.tcaptain", 1);
						}
					}
				}

				//Schiff in die Werft fliegen
				if ((aship.getOwner().getId() == user.getId()) && (ashiptype.getWerft() != 0))
				{
                    //Werft sammeln und auf EinwegWerft prüfen
                    ShipWerft werft = em.createQuery("from ShipWerft where ship=:ship", ShipWerft.class)
                            .setParameter("ship", aship)
                            .getSingleResult();

                    if(werft != null && !werft.isEinwegWerft()) {
                        t.setVar("sships.action.repair", 1);
                    }
				}

				//Externe Docks: andocken
				if (!aship.isDocked() && (shiptype.getADocks() > dockCount) &&
						((aship.getOwner().getId() == user.getId()) || superdock))
				{
					if (superdock || (ashiptype.getSize() <= ShipType.SMALL_SHIP_MAXSIZE))
					{
						t.setVar("sships.action.aufladen", 1);
					}
				}

				//Jaegerfunktionen: laden, Flotte landen
				if (shiptype.hasFlag(ShipTypeFlag.JAEGER) && (currentDockID != aship.getId()))
				{
					if ((ashiptype.getJDocks() > 0) && (aship.getOwner().getId() == user.getId()))
					{
						long carrierFullCount = shipService.getLandedCount(aship);

						if (carrierFullCount + 1 <= ashiptype.getJDocks())
						{
							t.setVar("sships.action.land", 1);
							if (ship.getFleet() != null)
							{
								boolean ok = true;
								// Falls noch nicht geschehen die Flotte des Jaegers ermitteln
								if (fleetlist == null)
								{
									fleetlist = new ArrayList<>();

									List<Ship> tmpList = em.createQuery("from Ship where id>0 and fleet=:fleet", Ship.class)
											.setParameter("fleet", ship.getFleet())
											.getResultList();
									for (Object aTmpList : tmpList)
									{
										Ship s = (Ship) aTmpList;
										ShipTypeData tmptype = s.getTypeData();
										if (!tmptype.hasFlag(ShipTypeFlag.JAEGER))
										{
											ok = false;
											break;
										}
										fleetlist.add(s.getId());
									}
									if (!ok)
									{
										fleetlist.clear();
									}
								}

								if (!fleetlist.isEmpty() && (fleetlist.size() <= ashiptype.getJDocks()))
								{
									if (carrierFullCount + fleetlist.size() <= ashiptype.getJDocks())
									{
										t.setVar("sships.action.landfleet", 1,
												"global.shiplist", Common.implode("|", fleetlist));
									}
								}
							}
						}
					}
				}

				//Aktuellen Jaeger auf dem (ausgewaehlten) Traeger laden lassen
				if ((aship.getOwner().getId() == user.getId()) && spaceToLand && ashiptype.hasFlag(ShipTypeFlag.JAEGER))
				{
					t.setVar("sships.action.landthis", 1);

					// Flotte des aktuellen Jaegers landen lassen
					if (aship.getFleet() != null)
					{
						if (!jaegerFleetCache.containsKey(aship.getFleet()))
						{
							List<Integer> thisFleetList = new ArrayList<>();

							boolean ok = true;
							List<Ship> tmpList = em.createQuery("from Ship where id>0 and fleet=:fleet", Ship.class)
									.setParameter("fleet", aship.getFleet())
									.getResultList();
							for (Object aTmpList : tmpList)
							{
								Ship s = (Ship) aTmpList;
								ShipTypeData tmptype = s.getTypeData();

								if (!tmptype.hasFlag(ShipTypeFlag.JAEGER))
								{
									ok = false;
									break;
								}
								thisFleetList.add(s.getId());
							}

							if (!ok)
							{
								thisFleetList.clear();
							}

							jaegerFleetCache.put(aship.getFleet(), thisFleetList);
						}
						List<Integer> thisFleetList = jaegerFleetCache.get(aship.getFleet());

						if (!thisFleetList.isEmpty() && (thisFleetList.size() <= shiptype.getJDocks()))
						{
							if (fullcount + thisFleetList.size() <= shiptype.getJDocks())
							{
								t.setVar("sships.action.landthisfleet", 1,
										"sships.shiplist", Common.implode("|", thisFleetList));
							}
						}
					}
				}

				//Flottenfunktionen: anschliessen
				if (aship.getOwner().getId() == user.getId())
				{
					if ((ship.getFleet() == null) && (aship.getFleet() != null))
					{
						t.setVar("sships.action.joinfleet", 1);
					}
					else if ((ship.getFleet() != null) && (aship.getFleet() == null))
					{
						t.setVar("sships.action.add2fleet", 1);
					}
					else if ((aship.getFleet() == null) && (ship.getFleet() == null))
					{
						t.setVar("sships.action.createfleet", 1);
					}
				}
				t.parse("sships.list", "sships.listitem", true);
				t.stop_record();
				t.clear_record();
			}
		}
		t.setVar("global.owncount", ownShipList.size());
		t.setVar("global.friendcount", friendShipList.size()-ownShipList.size());
		t.setVar("global.enemycount", enemyShipList.size());
		if(shiptype.hasFlag(ShipTypeFlag.SRS_AWAC) || shiptype.hasFlag(ShipTypeFlag.SRS_EXT_AWAC) )
		{
			t.setVar("global.own.stable", isSecondRowStable(ownShipList));
			t.setVar("global.enemy.stable", isSecondRowStable(enemyShipList));
			t.setVar("global.friend.stable", isSecondRowStable(friendShipList));
		}

	}

	private void outputBases(Parameters caller, User user,
			TemplateEngine t, String order)
	{
		final long dataOffizierCount = em.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest", Long.class)
				.setParameter("dest", caller.ship)
				.getSingleResult();

		TypedQuery<Base> baseQuery;
		if( !order.equals("type") && !order.equals("shiptype") ) {
			baseQuery = em.createQuery("from Base b where b.system=:sys and floor(sqrt(pow(:x-b.x,2)+pow(:y-b.y,2))) <= b.size order by "+order+",b.id", Base.class);
		}
		else {
			baseQuery = em.createQuery("from Base b where b.system=:sys and floor(sqrt(pow(:x-b.x,2)+pow(:y-b.y,2))) <= b.size order by b.id", Base.class);
		}
		List<Base> bases = baseQuery
			.setParameter("sys", caller.ship.getSystem())
			.setParameter("x", caller.ship.getX())
			.setParameter("y", caller.ship.getY())
			.getResultList();

		for (Base base: bases)
		{
			t.start_record();
			t.setVar("base.id", base.getId(),
					"base.owner.id", base.getOwner().getId(),
					"base.name", base.getName(),
					"base.klasse", base.getKlasse().getId(),
					"base.size", base.getSize(),
					"base.image", base.getKlasse().getLargeImage(),
					"base.transfer", (base.getOwner().getId() != 0),
					"base.unittausch", (base.getOwner().getId() == caller.ship.getOwner().getId() && caller.shiptype.getUnitSpace() > 0),
					"base.colonize", (base.getOwner().getId() == 0) && caller.shiptype.hasFlag(ShipTypeFlag.COLONIZER),
					"base.action.repair", 0);

			if (base.getOwner() == user)
			{
				t.setVar("base.ownbase", 1);
			}

			String ownername = Common._title(bbCodeParser, base.getOwner().getName());
			if (ownername.equals(""))
			{
				ownername = "-";
			}
			if (base.getOwner().getId() == 0)
			{
				ownername = "Verlassen";
			}
			if ((base.getOwner().getId() != 0) && (base.getOwner() != user))
			{
				t.setVar("base.pm", 1);
			}
			t.setVar("base.owner.name", ownername);

			// Offizier transferieren
			if (base.getOwner() == user)
			{
				boolean hasoffizier = caller.offizier != null;
				if (!hasoffizier || caller.shiptype.hasFlag(ShipTypeFlag.OFFITRANSPORT))
				{
					if (caller.shiptype.getSize() > ShipType.SMALL_SHIP_MAXSIZE)
					{
						boolean ok = true;
						if (caller.shiptype.hasFlag(ShipTypeFlag.OFFITRANSPORT))
						{
							if (dataOffizierCount >= caller.shiptype.getCrew())
							{
								ok = false;
							}
						}

						if (ok)
						{
							t.setVar("base.offiziere.set", 1);
						}
					}
				}
				if (caller.offizier != null)
				{
					t.setVar("base.offiziere.transfer", 1);
				}

				BaseWerft werft = base.getWerft();
				if (werft != null)
				{
					if (werft.getBaseField() == -1)
					{
						//Werftfeld suchen
						int i;
						for (i = 0; i < base.getBebauung().length; i++)
						{
							if ((base.getBebauung()[i] != 0) && (Building.getBuilding(base.getBebauung()[i]) instanceof Werft))
							{
								break;
							}
						}
						werft.setBaseField(i);
						t.setVar("base.action.repair", 1,
								"base.werft.field", i);
					}
					else
					{
						t.setVar("base.action.repair", 1,
								"base.werft.field", werft.getBaseField());
					}
				}
			}

			t.parse("bases.list", "bases.listitem", true);
			t.stop_record();
			t.clear_record();
		}
	}

	private void outputNebel(Parameters caller, TemplateEngine t)
	{
		ShipTypeData shiptype = caller.shiptype;
		Ship ship = caller.ship;
		Nebel nebel = em.find(Nebel.class, new MutableLocation(ship));
		if( nebel != null ) {
			t.setVar(	"nebel",	true,
						"nebel.type",	nebel.getType().ordinal(),
						"nebel.description", nebel.getType().getDescription(),
						"nebel.flotteSammeln", ship.getFleet() != null && nebel.getType().isDeuteriumNebel(),
						"global.ship.deutfactor", (nebel.getType().modifiziereDeutFaktor(shiptype.getDeutFactor())>0) );
		}
	}

	private void outputJumpnodes(Parameters caller, User user, TemplateEngine t)
	{
		t.setBlock("_SENSORS","nodes.listitem","nodes.list");

		Ship ship = caller.ship;
		List<JumpNode> nodes = em.createQuery("from JumpNode where x=:x and y=:y and system=:sys", JumpNode.class)
			.setParameter("x", ship.getX())
			.setParameter("y", ship.getY())
			.setParameter("sys", ship.getSystem())
			.getResultList();

		for( JumpNode node : nodes )
		{
			int blocked = 0;
			if( node.isGcpColonistBlock() && races.rasse(user.getRace()).isMemberIn( 0 ) )
			{
				blocked = 1;
			}
			if( user.hasFlag( UserFlag.NO_JUMPNODE_BLOCK ) )
			{
				blocked = 0;
			}

			t.setVar(	"node.id",		node.getId(),
						"node.name",	node.getName(),
						"node.blocked",	blocked );

			t.parse("nodes.list","nodes.listitem",true);
		}
	}

	private void outputBattles(Parameters caller, User user, TemplateEngine t)
	{
		ShipTypeData shiptype = caller.shiptype;
		Ship ship = caller.ship;

		List<Battle> battles = em.createQuery("FROM Battle WHERE x=:x AND y=:y AND system=:system", Battle.class)
									.setParameter("x", ship.getX())
									.setParameter("y", ship.getY())
									.setParameter("system", ship.getSystem())
									.getResultList();

		for( Battle battle : battles ) {
			String party1;
			String party2;

			if( battle.getAlly(0) == 0 ) {
				User commander = battle.getCommander(0);
				party1 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", commander.getId())+"\">"+Common._title(bbCodeParser, commander.getName())+"</a>";
			}
			else {
				Ally ally = em.find(Ally.class, battle.getAlly(0));
				party1 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "allylist", "details", ally.getId())+"\">"+Common._title(bbCodeParser, ally.getName())+"</a>";
			}

			if( battle.getAlly(1) == 0 ) {
				User commander = battle.getCommander(1);
				party2 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", commander.getId() )+"\">"+Common._title(bbCodeParser, commander.getName())+"</a>";
			}
			else {
				Ally ally = em.find(Ally.class, battle.getAlly(1));
				party2 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "allylist", "details", ally.getId())+"\">"+Common._title(bbCodeParser, ally.getName())+"</a>";
			}
			boolean fixedjoin = battleService.getSchlachtMitglied(battle, user) != -1;
			boolean viewable = false;
			if( shiptype.getShipClass().isDarfSchlachtenAnsehen() && !fixedjoin ) {
				viewable = true;
			}

			boolean joinable = true;
			if( shiptype.getShipClass() == ShipClasses.GESCHUETZ ) {
				joinable = false;
			}

			long shipcount = em.createQuery("select count(*) from Ship where id>0 and battle= :battle", Long.class)
				.setParameter("battle", battle.getId())
				.getSingleResult();

			t.setVar(	"battle.id",		battle.getId(),
						"battle.party1",	party1,
						"battle.party2",	party2,
						"battle.side1",		Common._stripHTML(party1).replace("'", ""),
						"battle.side2",		Common._stripHTML(party2).replace("'", ""),
						"battle.fixedjoin",	fixedjoin,
						"battle.joinable",	joinable,
						"battle.viewable",	viewable,
						"battle.shipcount",	shipcount);
			t.parse("battles.list","battles.listitem",true);
		}
	}

	private void outputSubraumspalten(Parameters caller, TemplateEngine t)
	{
		Ship ship = caller.ship;
		final long jumps = em.createQuery("select count(*) from Jump where x=:x and y=:y and system=:sys", Long.class)
			.setParameter("x", ship.getX())
			.setParameter("y", ship.getY())
			.setParameter("sys", ship.getSystem())
			.getSingleResult();
		if( jumps != 0 ) {
			t.setVar(	"global.jumps",			jumps,
						"global.jumps.name",	(jumps>1 ? "Subraumspalten":"Subraumspalte"));
		}
	}

	/**
	 * Prueft, ob die zweite Reihe stabil ist. B
	 * @param shiplist Die Schiffsliste deren zweite Reihe geprueft werden soll
	 * @return <code>true</code>, falls die zweite Reihe unter den Bedingungen stabil ist
	 */
	public boolean isSecondRowStable( List<Ship> shiplist) {

		double owncaps = 0;
		double secondrowcaps = 0;
        for (Ship aship : shiplist) {

            ShipTypeData type = aship.getTypeData();

						double size = type.getSize();

						if (aship.getBaseType().hasFlag(ShipTypeFlag.SECONDROW) && aship.getEinstellungen().gotoSecondrow()) {
							if (!aship.isDocked() && !aship.isLanded())
							{
								secondrowcaps += size;
							}
						}
            else
            {
                if (size > ShipType.SMALL_SHIP_MAXSIZE) {
                    double countedSize = size;
                    if (type.getCrew() > 0) {
                        countedSize *= (aship.getCrew() / ((double) type.getCrew()));
                    }
                    owncaps += countedSize;
                }
            }
        }

        return Double.valueOf(secondrowcaps).intValue() == 0 || Double.valueOf(owncaps).intValue() >= Double.valueOf(secondrowcaps).intValue() * 2;

		}

}
