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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Werft;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Schiffsmodul fuer die SRS-Sensoren
 * @author Christopher Jung
 *
 */
@Configurable
public class SensorsDefault implements SchiffPlugin {
	private static final Log log = LogFactory.getLog(SensorsDefault.class);
	
	private int showOnly = 0;
	private int showId = 0;
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
	public String action(Parameters caller) {
		SchiffController controller = caller.controller;
		controller.parameterNumber("showonly");
		controller.parameterNumber("showid");
		controller.parameterString("order");
		
	 	showOnly = controller.getInteger("showonly");
		showId = controller.getInteger("showid");
		
		String order = controller.getString("order");
		if( !order.equals("") ) {
			if( !order.equals("id") && !order.equals("name") && !order.equals("owner") && !order.equals("shiptype") ) {
				order = "id";
			}
			controller.getUser().setUserValue("TBLORDER/schiff/sensororder", order );
		}
		
		return "";
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;
		Offizier offizier = caller.offizier;
		SchiffController controller = caller.controller;
		
		Database database = controller.getDatabase();
		org.hibernate.Session db = controller.getDB();
		User user = (User)controller.getUser();
		TemplateEngine t = controller.getTemplateEngine();
		
		t.setFile("_PLUGIN_"+pluginid, "schiff.sensors.default.html");
		
		//Kein SRS oder nicht nutzbar? Ende Gelaende.
		if(!ship.canUseSrs()) {
			t.setVar("has.srs", false);
			t.parse(caller.target,"_PLUGIN_"+pluginid);
			return;
		}
		t.setVar("has.srs", true);
		
		t.setVar(	"global.ship",				ship.getId(),
					"global.pluginid",			pluginid,
					"ship.sensors.location",	Ships.getLocationText(ship.getLocation(), true),
					"global.awac",				shiptype.hasFlag(ShipTypes.SF_SRS_AWAC) );

		final long dataOffizierCount = (Long)db.createQuery("select count(*) from Offizier where dest=?")
			.setString(0, "s "+ship.getId())
			.iterate().next();
		
		List<Integer> fleetlist = null;
		
		int sensorrange = Math.round(shiptype.getSensorRange()*(ship.getSensors()/100f));
		
		if ( ( sensorrange > 0 ) && ( ship.getCrew() >= shiptype.getCrew()/3 ) ) {
			int nebel = Ships.getNebula(ship.getLocation());
			if( (nebel < 3) || (nebel > 5) ) {
				t.setVar("global.longscan",1);
			}
		}

		String order = user.getUserValue("TBLORDER/schiff/sensororder");

		if( ( ship.getSensors() > 30 ) && ( ship.getCrew() >= shiptype.getCrew() / 4 ) ) {
			t.setVar("global.goodscan",1);
		} else if( ship.getSensors() > 0 ) {
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
			Query baseQuery = null;
			if( !order.equals("type") && !order.equals("shiptype") ) {
				baseQuery = db.createQuery("from Base where system=? and floor(sqrt(pow(?-x,2)+pow(?-y,2))) <= size order by "+order+",id");
			}
			else {
				baseQuery = db.createQuery("from Base where system=? and floor(sqrt(pow(?-x,2)+pow(?-y,2))) <= size order by id");
			}
			List<?> bases = baseQuery
				.setInteger(0, ship.getSystem())
				.setInteger(1, ship.getX())
				.setInteger(2, ship.getY())
				.list();
			
			for( Iterator<?> iter=bases.iterator(); iter.hasNext(); ) {
				Base base = (Base)iter.next();
				
				t.start_record();
				t.setVar(	"base.id",			base.getId(),
							"base.owner.id",	base.getOwner().getId(),
							"base.name",		base.getName(),
							"base.klasse",		base.getKlasse(),
							"base.size",		base.getSize(),
							"base.image",		config.get("URL")+"data/starmap/kolonie"+base.getKlasse()+"_srs.png",
							"base.transfer",	(base.getOwner().getId() != 0),
							"base.colonize",	((base.getOwner().getId() == 0) || (base.getOwner().getId() == -1)) && shiptype.hasFlag(ShipTypes.SF_COLONIZER),
							"base.action.repair",	0 );

				if( base.getOwner() == user) {
					t.setVar("base.ownbase",1);
				}

				String ownername = Common._title(base.getOwner().getName());
				if( ownername.equals("") ) ownername = "-";
				if( base.getOwner().getId() == -1) ownername = "verlassen";
				if( !ownername.equals("-") && (base.getOwner().getId() != -1) && (base.getOwner() != user) ) {
					t.setVar("base.pm", 1);
				}
				t.setVar("base.owner.name",ownername);

				// Offizier transferieren
				if( base.getOwner() == user ) {
					boolean hasoffizier = offizier != null;
					if( !hasoffizier || shiptype.hasFlag(ShipTypes.SF_OFFITRANSPORT) ) {
						if( shiptype.getSize() > ShipType.SMALL_SHIP_MAXSIZE ) {
							boolean ok = true;
							if( shiptype.hasFlag(ShipTypes.SF_OFFITRANSPORT) ) {
								if( dataOffizierCount >= shiptype.getCrew() ) {
									ok = false;
								}
							}
							
							if( ok ) {
								t.setVar("base.offiziere.set",1);
							}
						}
					}
					if( offizier != null ) {
						t.setVar("base.offiziere.transfer",1);
					}

					boolean entry = db.createQuery("from BaseWerft where col=?")
						.setEntity(0, base)
						.iterate().hasNext();
					if( entry ) {
						//Werftfeld suchen
						int i=0;
						for( i=0; i < base.getBebauung().length; i++ ) {
							if( (base.getBebauung()[i] != 0) && (Building.getBuilding(base.getBebauung()[i]) instanceof Werft) ) {
								break;	
							}
						}
						t.setVar(	"base.action.repair",	1,
									"base.werft.field",		i );
					}
				}

				t.parse("bases.list","bases.listitem",true);
				t.stop_record();
				t.clear_record();
			}
		}
		
		//
		// Nebel,Jumpnodes und Schiffe nur anzeigen, wenn genuegend crew vorhanden und die Sensoren funktionsfaehig sind (>30)
		//
		if( ( ship.getSensors() > 30 ) && ( ship.getCrew() >= shiptype.getCrew() / 4 ) ) {
			/*
				Nebel
			*/

			Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(ship));
			if( nebel != null ) {
				t.setVar(	"nebel",	true,
							"nebel.type",	nebel.getType(),
							"global.ship.deutfactor", (shiptype.getDeutFactor() != 0 && (nebel.getType() < 3) ));
			}
			
			/*
				Jumpnodes
			*/
	
			JumpNode node = (JumpNode)db.createQuery("from JumpNode where x=? and y=? and system=?")
				.setInteger(0, ship.getX())
				.setInteger(1, ship.getY())
				.setInteger(2, ship.getSystem())
				.uniqueResult();
			
			if( node != null ) {
				int blocked = 0;
				if( node.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) ) {
					blocked = 1;
				}
				if( user.hasFlag( User.FLAG_NO_JUMPNODE_BLOCK ) ) blocked = 0;
							
				t.setVar(	"node.id",		node.getId(),
							"node.name",	node.getName(),
							"node.blocked",	blocked );
			}
			
			/*
				Schlachten
			*/
			SQLQuery battle = database.query("SELECT * FROM battles WHERE x=",ship.getX()," AND y=",ship.getY()," AND system=",ship.getSystem());
			while( battle.next() ) {
				boolean questbattle = false;
				if( (battle.getString("visibility") != null) && (battle.getString("visibility").length() > 0) ) {
					Integer[] visibility = Common.explodeToInteger(",",battle.getString("visibility"));
					if( Common.inArray(user.getId(),visibility) ) {
						questbattle = true;
					}
				}
				Ally ownAlly = user.getAlly();
				String party1 = null;
				String party2 = null;
				
				if( battle.getInt("ally1") == 0 ) {
					User commander = (User)db.get(User.class, battle.getInt("commander1"));
					party1 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", commander.getId())+"\">"+Common._title(commander.getName())+"</a>";
				} 
				else {
					Ally ally = (Ally)db.get(Ally.class, battle.getInt("ally1"));
					party1 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "allylist", "details", ally.getId())+"\">"+Common._title(ally.getName())+"</a>";
				}
	
				if( battle.getInt("ally2") == 0 ) {
					User commander = (User)db.get(User.class, battle.getInt("commander2"));
					party2 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", commander.getId() )+"\">"+Common._title(commander.getName())+"</a>";
				} 
				else {
					Ally ally = (Ally)db.get(Ally.class, battle.getInt("ally2"));
					party2 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "allylist", "details", ally.getId())+"\">"+Common._title(ally.getName())+"</a>";
				}
				boolean fixedjoin = false;
				if( (battle.getInt("commander1") == user.getId()) || 
					(battle.getInt("commander2") == user.getId()) || 
					( (ownAlly != null) && (battle.getInt("ally1") == ownAlly.getId()) ) || 
					( (ownAlly != null) && (battle.getInt("ally2") == ownAlly.getId()) ) ) {
					fixedjoin = true;
				}
				boolean viewable = false;
				if( ((shiptype.getShipClass() == ShipClasses.FORSCHUNGSKREUZER.ordinal()) || (shiptype.getShipClass() == ShipClasses.AWACS.ordinal())) && !fixedjoin ) {
					viewable = true;
				}
				
				boolean joinable = true;
				if( shiptype.getShipClass() == ShipClasses.GESCHUETZ.ordinal() ) {
					joinable = false;
				}
					
				long shipcount = (Long)db.createQuery("select count(*) from Ship where id>0 and battle= :battle")
					.setInteger("battle", battle.getInt("id"))
					.iterate().next();
					
				t.setVar(	"battle.id",		battle.getInt("id"),
							"battle.party1",	party1,
							"battle.party2",	party2,
							"battle.side1",		Common._stripHTML(party1).replace("'", ""),
							"battle.side2",		Common._stripHTML(party2).replace("'", ""),
							"battle.fixedjoin",	fixedjoin,
							"battle.joinable",	joinable,
							"battle.viewable",	viewable,
							"battle.shipcount",	shipcount,
							"battle.quest",		questbattle );
				t.parse("battles.list","battles.listitem",true);
			}
			battle.free();
		
			/*
				Subraumspalten (durch Sprungantriebe)
			*/
	
			final long jumps = (Long)db.createQuery("select count(*) from Jump where x=? and y=? and system=?")
				.setInteger(0, ship.getX())
				.setInteger(1, ship.getY())
				.setInteger(2, ship.getSystem())
				.iterate().next();
			if( jumps != 0 ) {
				t.setVar(	"global.jumps",			jumps,
							"global.jumps.name",	(jumps>1 ? "Subraumspalten":"Subraumspalte"));
			}
			
			/*
				Schiffe
			*/
			
			// Cache fuer Jaegerflotten. Key ist die Flotten-ID. Value die Liste der Schiffs-IDs der Flotte
			Map<ShipFleet,List<Integer>> jaegerFleetCache = new HashMap<ShipFleet,List<Integer>>();
			
			// Die ID des Schiffes, an dem das aktuell ausgewaehlte Schiff angedockt ist
			int currentDockID = 0;
			if( ship.getDocked().length() > 0 ) {
				if( ship.getDocked().charAt(0) == 'l' ) {
					currentDockID = Integer.parseInt(ship.getDocked().substring(2));
				}
				else {
					currentDockID = Integer.parseInt(ship.getDocked());
				}
			}
			
			int user_wrapfactor = Integer.parseInt(user.getUserValue("TBLORDER/schiff/wrapfactor"));
			
			// dockCount - die Anzahl der aktuell angedockten Schiffe
			final long dockCount = ship.getDockedCount();
			
			// superdock - Kann der aktuelle Benutzer alles andocken?
			boolean superdock = false;
			if( shiptype.getADocks() > dockCount ) {	
				superdock = user.hasFlag( User.FLAG_SUPER_DOCK );
			}
			
			// fullcount - Die Anzahl der freien Landeplaetze auf dem aktuell ausgewaehlten Traeger
			final long fullcount = ship.getLandedCount();
			
			// spaceToLand - Ist ueberhaupt noch Platz auf dem aktuell ausgewaehlten Traeger?
			final boolean spaceToLand = fullcount < shiptype.getJDocks();
			
			String thisorder = "s."+order;
			if( order.equals("id") ) {
				thisorder = "case when s.docked!='' then s.docked else s.id end";
			}
			if( order.equals("type") ) {
				thisorder = "s.shiptype";
			}
			
			List<?> ships = null;
			boolean firstentry = false;
			Map<String,Long> types = new HashMap<String,Long>();
			
			// Soll nur ein bestimmter Schiffstyp angezeigt werden?
			if( this.showOnly != 0 ) { 
				// IF(t1.docked!='',t1.docked+0.1,t1.id) as myorder
				ships = db.createQuery("from Ship s inner join fetch s.owner " +
						"where s.id!= :id and s.id>0 and s.x= :x and s.y= :y and s.system= :sys and " +
							"s.battle is null and (s.visibility is null or s.visibility= :user) and " +
							"locate('l ',s.docked)=0 and s.shiptype= :showonly and s.owner= :showid and " +
							"locate('disable_iff',s.status)=0 "+
						"order by "+thisorder+",case when s.docked!='' then s.docked else s.id end,fleet")
					.setInteger("id", ship.getId())
					.setInteger("x", ship.getX())
					.setInteger("y", ship.getY())
					.setInteger("sys", ship.getSystem())
					.setInteger("user", user.getId())
					.setInteger("showonly", this.showOnly)
					.setInteger("showid", this.showId)
					.list();
				
				firstentry = true;								
			} 
			else { 
				// wenn wir kein Wrap wollen, koennen wir uns das hier auch sparen
				
				if( user_wrapfactor != 0 ) {
					// herausfinden wieviele Schiffe welches Typs im Sektor sind		
					List<?> typeList = db.createQuery("select count(*),s.shiptype,s.owner.id " +
							"from Ship s " +
							"where s.id!= :id and s.id>0 and s.x= :x and s.y= :y and s.system= :sys and s.battle is null and " +
								"(s.visibility is null or s.visibility= :user ) and locate('disable_iff',s.status)=0 and " +
								"locate('l ',s.docked)=0 " +
							"group by s.shiptype,s.owner")
						.setInteger("id", ship.getId())
						.setInteger("x", ship.getX())
						.setInteger("y", ship.getY())
						.setInteger("sys", ship.getSystem())
						.setInteger("user", user.getId())
						.list();
					
					for( Iterator<?> iter=typeList.iterator(); iter.hasNext(); ) {
						Object[] data = (Object[])iter.next();
						
						Long count = (Long)data[0];
						ShipType type = (ShipType)data[1];
						int owner = (Integer)data[2];
						types.put(type.getId()+"_"+owner, count);
					}
				}
				ships = db.createQuery("from Ship s inner join fetch s.owner " +
						"where s.id!= :id and s.id>0 and s.x= :x and s.y=:y and s.system= :sys and " +
							"s.battle is null and (s.visibility is null or s.visibility= :user ) and locate('l ',s.docked)=0 " +
						"order by "+thisorder+",case when s.docked!='' then s.docked else s.id end, fleet")
					.setInteger("id", ship.getId())
					.setInteger("x", ship.getX())
					.setInteger("y", ship.getY())
					.setInteger("sys", ship.getSystem())
					.setInteger("user", user.getId())
					.list();
			}
			
			for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
				Ship aship = (Ship)iter.next();
				ShipTypeData ashiptype = aship.getTypeData();
				ShipTypeData mastertype = aship.getBaseType();
				
				final String typeGroupID = aship.getType()+"_"+aship.getOwner().getId();

				// Schiff nur als/in Gruppe anzeigen
				if( (this.showOnly == 0) && !aship.getStatus().contains("disable_iff") && 
					(user_wrapfactor != 0) && (mastertype.getGroupwrap() != 0) && 
					(types.get(typeGroupID) >= mastertype.getGroupwrap()*user_wrapfactor) )  {
					
					int fleetlesscount = 0;
					int ownfleetcount = 0;
					String groupidlist = ""; 				
					if( aship.getOwner().getId() == user.getId() ) {
						fleetlesscount = database.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",ship.getSystem(),"' AND x='",ship.getX(),"' AND y='",ship.getY(),"' AND owner='",user.getId(),"' AND type='",aship.getType(),"' AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status) AND fleet is null").getInt("count");
						if( ship.getFleet() != null ) {
							ownfleetcount = database.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",ship.getSystem(),"' AND x='",ship.getX(),"' AND y='",ship.getY(),"' AND owner='",user.getId(),"' AND type='",aship.getType(),"'  AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status) AND fleet=",ship.getFleet().getId()).getInt("count");
						}
						groupidlist = database.first("SELECT GROUP_CONCAT(id SEPARATOR '|') grp FROM ships WHERE id>0 AND system='",ship.getSystem(),"' AND x='",ship.getX(),"' AND y='",ship.getY(),"' AND owner='",user.getId(),"' AND type='",aship.getType(),"'  AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status)").getString("grp");
					}		
					
					t.start_record();
					t.setVar(	"sshipgroup.name",			types.get(typeGroupID)+" x "+mastertype.getNickname(),
								"sshipgroup.idlist",		groupidlist,
								"sshipgroup.type.id",		aship.getType(),
								"sshipgroup.owner.id",		aship.getOwner().getId(),
								"sshipgroup.owner.name",	Common._title(aship.getOwner().getName()),
								"sshipgroup.type.name",		mastertype.getNickname(),
								"sshipgroup.sublist",		0,																		
								"sshipgroup.type.image",	mastertype.getPicture(),
								"sshipgroup.own",			aship.getOwner().getId() == user.getId(),
								"sshipgroup.count",			types.get(typeGroupID) + (ship.getType() == aship.getType() ? 1 : 0) - ownfleetcount,
								"sshipgroup.fleetlesscount",	fleetlesscount );
		
					if( aship.getOwner().getId() == user.getId() ) {
						t.setVar("sshipgroup.ownship",1);
					} else {
						t.setVar("sshipgroup.ownship",0);
					}
									
					t.parse("sships.list","sshipgroup.listitem",true);
					t.stop_record();
					t.clear_record();									
					types.put(typeGroupID, -1L);	// einmal anzeigen reicht
				} 
				else if( (this.showOnly != 0) || !types.containsKey(typeGroupID) || (types.get(typeGroupID) != -1) ) {
					if( (this.showOnly != 0) && firstentry ) {
						int count = ships.size();		
						
						int fleetlesscount = 0;
						int ownfleetcount = 0;					
						if( aship.getOwner().getId() == user.getId() ) {
							fleetlesscount = database.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",ship.getSystem(),"' AND x='",ship.getX(),"' AND y='",ship.getY(),"' AND owner='",user.getId(),"' AND type='",aship.getType(),"' AND docked='' AND fleet is null").getInt("count");
							if( ship.getFleet() != null ) {
								ownfleetcount = database.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",ship.getSystem(),"' AND x='",ship.getX(),"' AND y='",ship.getY(),"' AND owner='",user.getId(),"' AND type='",aship.getType(),"' AND docked='' AND fleet=",ship.getFleet().getId()).getInt("count");
							}
						}	
						
						t.setVar(	"sshipgroup.name",			count+" x "+mastertype.getNickname(),
									"sshipgroup.type.id",		aship.getType(),
									"sshipgroup.owner.id",		aship.getOwner().getId(),
									"sshipgroup.owner.name", 	Common._title(aship.getOwner().getName()),
									"sshipgroup.type.name",		mastertype.getNickname(),
									"sshipgroup.sublist", 		1,																		
									"sshipgroup.type.image",	mastertype.getPicture(),
									"sshipgroup.own",			aship.getOwner().getId() == user.getId(),
									"sshipgroup.count",			count + (ship.getType() == aship.getType() ? 1 : 0) - ownfleetcount,
									"sshipgroup.fleetlesscount",	fleetlesscount );
				
						if( aship.getOwner().getId() == user.getId() ) {
							t.setVar("sshipgroup.ownship",1);
						} 
						else {
							t.setVar("sshipgroup.ownship",0);
						}			
						t.parse("sships.list","sshipgroup.listitem",true);	
						
						firstentry = false;
					}
					t.start_record();
					t.setVar(	"sships.id",			aship.getId(),
								"sships.owner.id" ,		aship.getOwner().getId(),
								"sships.owner.name",	Common._title(aship.getOwner().getName()),
								"sships.name",			Common._plaintitle(aship.getName()),
								"sships.type.id",		aship.getType(),
								"sships.hull",			Common.ln(aship.getHull()),
								"sships.ablativearmor",	Common.ln(aship.getAblativeArmor()),
								"sships.shields",		Common.ln(aship.getShields()),
								"sships.fleet.id",		aship.getFleet() != null ? aship.getFleet().getId() : 0,
								"sships.type.name",		ashiptype.getNickname().replace("'", ""),
								"sships.type.image",	ashiptype.getPicture(),
								"sships.docked.id",		aship.getDocked() );

					boolean disableIFF = aship.getStatus().contains("disable_iff");
					t.setVar("sships.disableiff",disableIFF);
		
					if( aship.getOwner().getId() == user.getId() ) {
						t.setVar("sships.ownship",1);
					} else {
						t.setVar("sships.ownship",0);
					}

					if( disableIFF ) t.setVar("sships.owner.name","Unbekannt");
		
					if( aship.getFleet() != null ) {
						t.setVar("sships.fleet.name",Common._plaintitle(aship.getFleet().getName()));
					}
					// Gedockte Schiffe zuordnen (gelandete brauchen hier nicht beruecksichtigt werden, da sie von der Query bereits aussortiert wurden)
					if( !aship.getDocked().isEmpty() ) {
						Ship master = (Ship)db.get(Ship.class, Integer.valueOf(aship.getDocked()));
						if( master == null ) {
							log.warn("Schiff "+aship.getId()+" hat ungueltigen Dockeintrag '"+aship.getDocked()+"'");
						}
						else {
							t.setVar("sships.docked.name",master.getName());
						}
					}
					
					// Anzeige Heat (Standard)
					if( shiptype.hasFlag(ShipTypes.SF_SRS_EXT_AWAC) ) {
						t.setVar("sships.heat",aship.getHeat());
						
						// Anzeige Heat
						if( aship.getHeat() == 0 ) {
							t.setVar("sships.heat.none",1);
						}
						if( (aship.getHeat() > 0) && (aship.getHeat() <= 100) ) {
							t.setVar("sships.heat.medium",1);
						} else if( aship.getHeat() > 100 ) {
							t.setVar("sships.heat.hight",1);
						}
		
						// Anzeige Crew
						if( (aship.getCrew() == 0) && (ashiptype.getCrew() != 0) ) {
							t.setVar("sships.nocrew",1);
						} else if( aship.getCrew() > 0 ) {
							t.setVar("sships.crew",aship.getCrew());
						}
		
						// Anzeige Energie
						if( aship.getEnergy() == 0 ) {
							t.setVar("sships.noe",1);
						} else if( aship.getEnergy() > 0 ) {
							t.setVar("sships.e",aship.getEnergy());
						}
					} 
					else if( shiptype.hasFlag(ShipTypes.SF_SRS_AWAC) ) {
						t.setVar("global.standartawac",1);
						
						if( aship.getHeat() > 100 ) {
							t.setVar("sships.heat.high",1);
						} else if( aship.getHeat() > 40 ) {
							t.setVar("sships.heat.medium",1);
						} else if( aship.getHeat() > 0 ) {
							t.setVar("sships.heat.low",1);
						} else {
							t.setVar("sships.heat.none",1);
						}
					}

					//Angreifen
					if( !disableIFF && (aship.getOwner().getId() != user.getId()) && (aship.getBattle()==null) && shiptype.isMilitary() ) {
						if( ( (user.getAlly() != null) && (aship.getOwner().getAlly() != user.getAlly()) ) || (user.getAlly() == null) ) {
							t.setVar("sships.action.angriff",1);
						}
					}

					// Anfunken
					if( aship.getOnCommunicate() != null && !aship.getOnCommunicate().isEmpty() ) {
						boolean found = true;
						if( !aship.getOnCommunicate().contains("*:") ) {
							found = false;
							String[] comlist = StringUtils.split(aship.getOnCommunicate(), ';');
							for( int i=0; i < comlist.length; i++ ) {
								String[] comentry = comlist[i].split(":");
								if( Integer.parseInt(comentry[0]) == user.getId() ) {
									found = true;
									break;	
								}	
							}
						}
						else {
							found = true;	
						}
						
						if( found ) {
							t.setVar("sships.action.communicate",1);
						}
					}

					// Springen (Knossosportal)
					if( !aship.getJumpTarget().isEmpty() ) {
						/*
							Ermittlung der Sprungberechtigten
							moeglich sind: default,all,user,ally,ownally,group
						 */
						String[] target = StringUtils.split(aship.getJumpTarget(), '|');
						String[] targetuser = StringUtils.split(target[2], ':');
						if( targetuser[0].equals("all") ) {
							t.setVar("sships.action.jump",1);
						}
						else if( targetuser[0].equals("ally") ) {
							if(  (user.getAlly() != null) && (Integer.parseInt(targetuser[1]) == user.getAlly().getId()) ) {
								t.setVar("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("user") ) {
							if ( Integer.parseInt(targetuser[1]) == user.getId() ){
								t.setVar("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("ownally") ) {
							if ( (user.getAlly() != null) && (aship.getOwner().getAlly() == user.getAlly()) ){
								t.setVar("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("group") ) {
							String[] userlist = targetuser[1].split(",");
							if( Common.inArray(Integer.toString(user.getId()),userlist) )  {
								t.setVar("sships.action.jump",1);
							}
						}
						else {
							// Default: Selbe Allianz wie der Besitzer oder selbst der Besitzer
							if( ( (user.getAlly() != null) && (aship.getOwner().getAlly() == user.getAlly()) ) || 
								((user.getAlly() == null) && (aship.getOwner().getId() == user.getId())) ) {
								
								t.setVar("sships.action.jump",1);
							}
						}
					}

					//Handeln, Pluendernlink, Waren transferieren
					if( aship.getStatus().indexOf("tradepost") != -1 ) {
						t.setVar("sships.action.trade",1);
					} 
					else if( !disableIFF && (aship.getOwner().getId() == -1) && (ashiptype.getShipClass() == ShipClasses.SCHROTT.ordinal() || ashiptype.getShipClass() == ShipClasses.FELSBROCKEN.ordinal()) ) {
						t.setVar("sships.action.transferpluender",1);
					} 
					else if( !disableIFF || (aship.getOwner().getId() == user.getId()) ) {
						t.setVar("sships.action.transfer",1);
					}

					//Bemannen, Kapern
					if( !disableIFF && (aship.getOwner().getId() != user.getId()) && (ashiptype.getShipClass() != ShipClasses.GESCHUETZ.ordinal()) &&
						((aship.getOwner().getId() != -1) || (ashiptype.getShipClass() == ShipClasses.SCHROTT.ordinal() || ashiptype.getShipClass() == ShipClasses.FELSBROCKEN.ordinal())) ) {
						if( (user.getAlly() == null) || (aship.getOwner().getAlly() != user.getAlly()) ) {
							if( !ashiptype.hasFlag(ShipTypes.SF_NICHT_KAPERBAR) ) {
								t.setVar("sships.action.kapern",1);
							}
							else {
								t.setVar("sships.action.pluendern",1);
							}
						}
					} else if( !disableIFF && (aship.getOwner().getId() == user.getId()) && (ashiptype.getCrew() > 0)  ) {
						t.setVar("sships.action.crewtausch",1);
					}

					//Offiziere: Captain transferieren
					boolean hasoffizier = aship.getStatus().contains("offizier");
					if( !disableIFF && (offizier != null) && (!hasoffizier || ashiptype.hasFlag(ShipTypes.SF_OFFITRANSPORT) ) ) {
						if( ashiptype.getSize() > ShipType.SMALL_SHIP_MAXSIZE ) {
							boolean ok = true;
							if( ashiptype.hasFlag(ShipTypes.SF_OFFITRANSPORT) ) {
								long officount = (Long)db.createQuery("select count(*) from Offizier where dest=?")
									.setString(0, "s "+aship.getId())
									.iterate().next();
								
								if( officount >= ashiptype.getCrew() ) {
									ok = false;
								}
							}
							
							if( ok ) {
								t.setVar("sships.action.tcaptain",1);
							}
						}
					}

					//Schiff in die Werft fliegen
					if( (aship.getOwner().getId() == user.getId()) && (ashiptype.getWerft() != 0) ) {
						t.setVar("sships.action.repair",1);
					}

					//Externe Docks: andocken
					if( aship.getDocked().isEmpty() && ( shiptype.getADocks() > dockCount ) && 
						( (aship.getOwner().getId() == user.getId() ) || superdock) ) {
						if( superdock || ( ashiptype.getSize() <= ShipType.SMALL_SHIP_MAXSIZE ) ) {
							t.setVar("sships.action.aufladen",1);
						}
					}

					//Jaegerfunktionen: laden, Flotte landen
					if( shiptype.hasFlag(ShipTypes.SF_JAEGER) && (currentDockID != aship.getId()) ) {
						if( ( ashiptype.getJDocks() > 0 ) && ( aship.getOwner().getId() == user.getId() ) ) {
							long carrierFullCount = aship.getLandedCount();

							if( carrierFullCount + 1 <= ashiptype.getJDocks() ) {
								t.setVar("sships.action.land",1);
								if( ship.getFleet() != null ) {
									boolean ok = true;
									// Falls noch nicht geschehen die Flotte des Jaegers ermitteln
									if( fleetlist == null ) {
										fleetlist = new ArrayList<Integer>();
										
										List<?> tmpList = db.createQuery("from Ship where id>0 and fleet=?")
											.setEntity(0, ship.getFleet())
											.list();
										for( Iterator<?> iter2=tmpList.iterator(); iter2.hasNext(); ) {
											Ship s = (Ship)iter2.next();
											ShipTypeData tmptype = s.getTypeData();
											if( !tmptype.hasFlag(ShipTypes.SF_JAEGER) ) {
												ok = false;
												break;
											}
											fleetlist.add(s.getId());
										}		
										if( !ok ) {
											fleetlist.clear();
										}
									}

									if( !fleetlist.isEmpty() && (fleetlist.size() <= ashiptype.getJDocks()) ) {
										if( carrierFullCount + fleetlist.size() <= ashiptype.getJDocks() )
											t.setVar(	"sships.action.landfleet", 1,
														"global.shiplist", Common.implode("|",fleetlist) );
									}
								}
							}
						}
					}
				
					//Aktuellen Jaeger auf dem (ausgewaehlten) Traeger laden lassen
					if( (aship.getOwner().getId() == user.getId()) && spaceToLand && ashiptype.hasFlag(ShipTypes.SF_JAEGER) ) {
						t.setVar("sships.action.landthis",1);
						
						// Flotte des aktuellen Jaegers landen lassen
						if( aship.getFleet() != null ) {
							if( !jaegerFleetCache.containsKey(aship.getFleet())) {
								List<Integer> thisFleetList = new ArrayList<Integer>();
								
								boolean ok = true;
								List<?> tmpList = db.createQuery("from Ship where id>0 and fleet=?")
									.setEntity(0, aship.getFleet())
									.list();
								for( Iterator<?> iter2=tmpList.iterator(); iter2.hasNext(); ) {
									Ship s = (Ship)iter2.next();
									ShipTypeData tmptype = s.getTypeData();
									
									if( !tmptype.hasFlag(ShipTypes.SF_JAEGER) ) {
										ok = false;
										break;
									}
									thisFleetList.add(s.getId());
								}		

								if( !ok ) {
									thisFleetList.clear();
								}
								
								jaegerFleetCache.put(aship.getFleet(), thisFleetList);
							}
							List<Integer> thisFleetList = jaegerFleetCache.get(aship.getFleet());
							
							if( !thisFleetList.isEmpty() && (thisFleetList.size() <= shiptype.getJDocks()) ) {
								if( fullcount + thisFleetList.size() <= shiptype.getJDocks() )
									t.setVar(	"sships.action.landthisfleet", 1,
												"sships.shiplist", Common.implode("|",thisFleetList) );
							}
						}
					}

					//Flottenfunktionen: anschliessen
					if( aship.getOwner().getId() == user.getId() ) {
						if( (ship.getFleet() == null) && (aship.getFleet() != null) ) {
							t.setVar("sships.action.joinfleet",1);
						}
						else if( (ship.getFleet() != null) && (aship.getFleet() == null) ) {
							t.setVar("sships.action.add2fleet",1);
						}
						else if( (aship.getFleet() == null) && (ship.getFleet() == null) ) {
							t.setVar("sships.action.createfleet",1);
						}
					}
					t.parse("sships.list","sships.listitem",true);
					t.stop_record();
					t.clear_record();
				}
			}
		}
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
