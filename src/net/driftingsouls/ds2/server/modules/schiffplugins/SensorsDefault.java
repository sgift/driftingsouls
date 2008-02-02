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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Schiffsmodul fuer die SRS-Sensoren
 * @author Christopher Jung
 *
 */
public class SensorsDefault implements SchiffPlugin, Loggable {
	private int showOnly = 0;
	private int showId = 0;
	
	public String action(Parameters caller) {
		SchiffController controller = caller.controller;
		controller.parameterNumber("showonly");
		controller.parameterNumber("showid");
		controller.parameterString("order");
		
	 	showOnly = controller.getInteger("showonly");
		showId = controller.getInteger("showid");
		
		String order = controller.getString("order");
		if( !order.equals("") ) {
			if( !order.equals("id") && !order.equals("name") && !order.equals("owner") && !order.equals("type") ) {
				order = "id";
			}
			controller.getUser().setUserValue("TBLORDER/schiff/sensororder", order );
		}
		
		return "";
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		SQLResultRow data = caller.ship;
		SQLResultRow datatype = caller.shiptype;
		Offizier offizier = caller.offizier;
		SchiffController controller = caller.controller;
		
		Database db = controller.getDatabase();
		User user = (User)controller.getUser();
		TemplateEngine t = controller.getTemplateEngine();
		
		int ship = data.getInt("id");
		
		t.setFile("_PLUGIN_"+pluginid, "schiff.sensors.default.html");
		t.setVar(	"global.ship",				ship,
					"global.pluginid",			pluginid,
					"ship.sensors.location",	Ships.getLocationText(data, true),
					"global.awac",				ShipTypes.hasShipTypeFlag(datatype, ShipTypes.SF_SRS_AWAC) );

		final int dataOffizierCount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s "+data.getInt("id")+"'").getInt("count");
		
		List<Integer> fleetlist = null;

		Map<Integer,SQLResultRow> fleetcache = new HashMap<Integer,SQLResultRow>();
		
		if ( ( datatype.getInt("sensorrange") > 0 ) && ( data.getInt("crew") >= datatype.getInt("crew")/3 ) ) {
			int nebel = Ships.getNebula(data);
			if( (nebel < 3) || (nebel > 5) ) {
				t.setVar("global.longscan",1);
			}
		}

		String order = user.getUserValue("TBLORDER/schiff/sensororder");

		if( ( data.getInt("sensors") > 30 ) && ( data.getInt("crew") >= datatype.getInt("crew") / 4 ) ) {
			t.setVar("global.goodscan",1);
		} else if( data.getInt("sensors") > 0 ) {
			t.setVar("global.badscan",1);
		}

		if( data.getInt("sensors") > 0 ) {
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
		if( data.getInt("sensors") > 0 ) {
			SQLQuery base = null;
			if( !order.equals("type") ) {
				base = db.query("SELECT id,owner,name,klasse,size FROM bases WHERE system=",data.getInt("system")," AND FLOOR(SQRT(POW(",data.getInt("x"),"-x,2)+POW(",data.getInt("y"),"-y,2))) <= size ORDER BY ",order,",id");
			}
			else {
				base = db.query("SELECT id,owner,name,klasse,size FROM bases WHERE system=",data.getInt("system")," AND FLOOR(SQRT(POW(",data.getInt("x"),"-x,2)+POW(",data.getInt("y"),"-y,2))) <= size ORDER BY id");
			}
			
			while( base.next() ) {
				SQLQuery datan = base;
				
				t.start_record();
				t.setVar(	"base.id",			datan.getInt("id"),
							"base.owner.id",	datan.getInt("owner"),
							"base.name",		datan.getString("name"),
							"base.klasse",		datan.getInt("klasse"),
							"base.size",		datan.getInt("size"),
							"base.image",		Configuration.getSetting("URL")+"data/starmap/kolonie"+datan.getInt("klasse")+"_srs.png",
							"base.transfer",	(datan.getInt("owner") != 0),
							"base.colonize",	((datan.getInt("owner") == 0) || (datan.getInt("owner") == -1)) && ShipTypes.hasShipTypeFlag(datatype,ShipTypes.SF_COLONIZER),
							"base.action.repair",	0 );

				if( datan.getInt("owner") == user.getId()) {
					t.setVar("base.ownbase",1);
				}

				User ownerObj = (User)controller.getDB().get(User.class, datan.getInt("owner"));
				String owner = Common._title(ownerObj.getName());
				if( owner.equals("") ) owner = "-";
				if( datan.getInt("owner") == -1) owner = "verlassen";
				if( !owner.equals("-") && (datan.getInt("owner") != -1) && (datan.getInt("owner") != user.getId()) ) {
					t.setVar("base.pm", 1);
				}
				t.setVar("base.owner.name",owner);

				// Offizier transferieren
				if( datan.getInt("owner") == user.getId() ) {
					boolean hasoffizier = offizier != null;
					if( !hasoffizier || ShipTypes.hasShipTypeFlag(datatype, ShipTypes.SF_OFFITRANSPORT) ) {
						if( datatype.getInt("size") > 2 ) {
							boolean ok = true;
							if( ShipTypes.hasShipTypeFlag(datatype, ShipTypes.SF_OFFITRANSPORT) ) {
								if( dataOffizierCount >= datatype.getInt("crew") ) {
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
				}

				if( datan.getInt("owner") == user.getId() ) {
					SQLResultRow werft = db.first("SELECT id FROM werften WHERE col=",datan.getInt("id"));
					if( !werft.isEmpty() ) {
						//Werftfeld suchen
						Base baseData = new Base(db.first("SELECT * FROM bases WHERE id='",datan.getInt("id"),"'"));
						
						// TODO: Das geht sicherlich auch deutlich schoener.....
						int i=0;
						for( i=0; i < baseData.getBebauung().length; i++ ) {
							if( (baseData.getBebauung()[i] != 0) && Building.getBuilding(db, baseData.getBebauung()[i]).getClass().getName().indexOf("bases.Werft") > -1 ) {
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
			base.free();
		}
		
		//
		// Nebel,Jumpnodes und Schiffe nur anzeigen, wenn genuegend crew vorhanden und die Sensoren funktionsfaehig sind (>30)
		//
		if( ( data.getInt("sensors") > 30 ) && ( data.getInt("crew") >= datatype.getInt("crew") / 4 ) ) {
			/*
				Nebel
			*/

			SQLResultRow nebel = db.first("SELECT id,type FROM nebel WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));

			if( !nebel.isEmpty() ) {
				t.setVar(	"nebel.id",		nebel.getInt("id"),
							"nebel.type",	nebel.getInt("type"),
							"global.ship.deutfactor", (datatype.getInt("deutfactor") != 0 && (nebel.getInt("type") < 3) ));
			}
			
			/*
				Jumpnodes
			*/
	
			SQLResultRow node = db.first("SELECT * FROM jumpnodes WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));
			if( !node.isEmpty() ) {
				int blocked = 0;
				if( node.getBoolean("gcpcolonistblock") && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) ) {
					blocked = 1;
				}
				if( user.hasFlag( User.FLAG_NO_JUMPNODE_BLOCK ) ) blocked = 0;
							
				t.setVar(	"node.id",		node.getInt("id"),
							"node.name",	node.getString("name"),
							"node.blocked",	blocked );
			}
			
			/*
				Schlachten
			*/
			SQLQuery battle = db.query("SELECT * FROM battles WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));
			while( battle.next() ) {
				boolean questbattle = false;
				if( (battle.getString("visibility") != null) && (battle.getString("visibility").length() > 0) ) {
					Integer[] visibility = Common.explodeToInteger(",",battle.getString("visibility"));
					if( Common.inArray(user.getId(),visibility) ) {
						questbattle = true;
					}
				}
				int ownAlly = user.getAlly();
				String party1 = null;
				String party2 = null;
				
				if( battle.getInt("ally1") == 0 ) {
					party1 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", battle.getInt("commander1"))+"\">"+Common._title(db.first("SELECT name FROM users WHERE id=",battle.getInt("commander1")).getString("name"))+"</a>";
				} 
				else {
					party1 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "allylist", "details", battle.getInt("ally1"))+"\">"+Common._title(db.first("SELECT name FROM ally WHERE id="+battle.getInt("ally1")).getString("name"))+"</a>";
				}
	
				if( battle.getInt("ally2") == 0 ) {
					party2 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", battle.getInt("commander2") )+"\">"+Common._title(db.first("SELECT name FROM users WHERE id=",battle.getInt("commander2")).getString("name"))+"</a>";
				} 
				else {
					party2 = "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "allylist", "details", battle.getInt("ally2"))+"\">"+Common._title(db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally2")).getString("name"))+"</a>";
				}
				boolean fixedjoin = false;
				if( (battle.getInt("commander1") == user.getId()) || 
					(battle.getInt("commander2") == user.getId()) || 
					( (battle.getInt("ally1") > 0) && (battle.getInt("ally1") == ownAlly) ) || 
					( (battle.getInt("ally2") > 0) && (battle.getInt("ally2") == ownAlly) ) ) {
					fixedjoin = true;
				}
				boolean viewable = false;
				if( ((datatype.getInt("class") == ShipClasses.FORSCHUNGSKREUZER.ordinal()) || (datatype.getInt("class") == ShipClasses.AWACS.ordinal())) && !fixedjoin ) {
					viewable = true;
				}
				
				boolean joinable = true;
				if( datatype.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) {
					joinable = false;
				}
					
				int shipcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND battle='",battle.getInt("id"),"'").getInt("count");
					
				t.setVar(	"battle.id",		battle.getInt("id"),
							"battle.party1",	party1,
							"battle.party2",	party2,
							"battle.side1",		Common._stripHTML(party1),
							"battle.side2",		Common._stripHTML(party2),
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
	
			SQLResultRow jumps = db.first("SELECT count(*) count FROM jumps WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));
			if( !jumps.isEmpty() ) {
				t.setVar(	"global.jumps",			jumps.getInt("count"),
							"global.jumps.name",	(jumps.getInt("count")>1 ? "Subraumspalten":"Subraumspalte"));
			}
			
			/*
				Schiffe
			*/
			
			// Cache fuer Jaegerflotten. Key ist die Flotten-ID. Value die Liste der Schiffs-IDs der Flotte
			Map<Integer,List<Integer>> jaegerFleetCache = new HashMap<Integer,List<Integer>>();
			
			// Die ID des Schiffes, an dem das aktuell ausgewaehlte Schiff angedockt ist
			int currentDockID = 0;
			if( data.getString("docked").length() > 0 ) {
				if( data.getString("docked").charAt(0) == 'l' ) {
					currentDockID = Integer.parseInt(data.getString("docked").substring(2));
				}
				else {
					currentDockID = Integer.parseInt(data.getString("docked"));
				}
			}
			
			int user_wrapfactor = Integer.parseInt(user.getUserValue("TBLORDER/schiff/wrapfactor"));
			
			// dockCount - die Anzahl der aktuell angedockten Schiffe
			final int dockCount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='",data.getInt("id"),"'").getInt("count");
			
			// superdock - Kann der aktuelle Benutzer alles andocken?
			boolean superdock = false;
			if( datatype.getInt("adocks") > dockCount ) {	
				superdock = user.hasFlag( User.FLAG_SUPER_DOCK );
			}
			
			// fullcount - Die Anzahl der freien Landeplaetze auf dem aktuell ausgewaehlten Traeger
			// spaceToLand - Ist ueberhaupt noch Platz auf dem aktuell ausgewaehlten Traeger?
			boolean spaceToLand = false;
			int fullcount = db.first("SELECT count(*) fullcount FROM ships WHERE id>0 AND docked='l ",data.getInt("id"),"'").getInt("fullcount");
			if( fullcount + 1 <= datatype.getInt("jdocks") ) {
				spaceToLand = true;
			}
			
			String thisorder = "t1."+order;
			if( order.equals("id") ) {
				thisorder = "myorder";
			}
			
			SQLQuery datas = null;
			boolean firstentry = false;
			Map<String,Integer> types = new HashMap<String,Integer>();
			
			// Soll nur ein bestimmter Schiffstyp angezeigt werden?
			if( this.showOnly != 0 ) { 
				datas = db.query("SELECT t1.id,t1.owner,t1.name,t1.type,t1.crew,t1.e,t1.s,t1.hull,t1.shields,t1.docked,t1.fleet,t1.jumptarget,t1.status,t1.oncommunicate,t3.name AS username,t3.ally,t1.battle,IF(t1.docked!='',t1.docked+0.1,t1.id) as myorder ",
									"FROM ships AS t1,users AS t3 ",
								   	"WHERE t1.id!=",ship," AND t1.id>0 AND t1.x=",data.getInt("x")," AND t1.y=",data.getInt("y")," AND t1.system=",data.getInt("system")," AND t1.battle=0 AND (t1.visibility IS NULL OR t1.visibility='",user.getId(),"') AND !LOCATE('l ',t1.docked) AND t1.owner=t3.id AND t1.type=",this.showOnly," AND t1.owner=",this.showId," AND !LOCATE('disable_iff',t1.status) ",
									"ORDER BY ",thisorder,",myorder,fleet");	
				firstentry = true;								
			} 
			else { 
				// wenn wir kein Wrap wollen, koennen wir uns das hier auch sparen
				
				if( user_wrapfactor != 0 ) {
					// herausfinden wieviele Schiffe welches Typs im Sektor sind		
					SQLQuery typesQuery = db.query("SELECT count(*) as menge,type,owner ",
										"FROM ships ",
										"WHERE id!=",ship," AND id>0 AND x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system")," AND battle=0 AND (visibility IS NULL OR visibility='",user.getId(),"') AND !LOCATE('disable_iff',status) AND !LOCATE('l ',docked) ",
										"GROUP BY type,owner");
					while( typesQuery.next() ) {
						types.put(typesQuery.getInt("type")+"_"+typesQuery.getInt("owner"), typesQuery.getInt("menge"));
					}
					typesQuery.free();
				}
				datas = db.query("SELECT t1.id,t1.owner,t1.name,t1.type,t1.crew,t1.e,t1.s,t1.hull,t1.shields,t1.docked,t1.fleet,t1.jumptarget,t1.status,t1.oncommunicate,t3.name AS username,t3.ally,t1.battle,IF(t1.docked!='',t1.docked+0.1,t1.id) as myorder ",
									"FROM ships AS t1,users AS t3 ",
									"WHERE t1.id!=",ship," AND t1.id>0 AND t1.x=",data.getInt("x")," AND t1.y=",data.getInt("y")," AND t1.system=",data.getInt("system")," AND t1.battle=0 AND (t1.visibility IS NULL OR t1.visibility='",user.getId(),"') AND !LOCATE('l ',t1.docked) AND t1.owner=t3.id ",
									"ORDER BY ",thisorder,",myorder,fleet");
			}
			
			while( datas.next() ) {
				SQLResultRow ashiptype = ShipTypes.getShipType( datas.getRow() );
				SQLResultRow mastertype = ShipTypes.getShipType( datas.getInt("type"), false );
				
				final String typeGroupID = datas.getInt("type")+"_"+datas.getInt("owner");

				// Schiff nur als/in Gruppe anzeigen
				if( (this.showOnly == 0) && !datas.getString("status").contains("disable_iff") && 
					(user_wrapfactor != 0) && (mastertype.getInt("groupwrap") != 0) && 
					(types.get(typeGroupID) >= mastertype.getInt("groupwrap")*user_wrapfactor) )  {
					
					int fleetlesscount = 0;
					int ownfleetcount = 0;
					String groupidlist = ""; 				
					if( datas.getInt("owner") == user.getId() ) {
						fleetlesscount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getId(),"' AND type='",datas.getInt("type"),"' AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status) AND fleet=0").getInt("count");
						if( data.getInt("fleet") != 0 ) {
							ownfleetcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getId(),"' AND type='",datas.getInt("type"),"'  AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status) AND fleet=",data.getInt("fleet")).getInt("count");
						}
						groupidlist = db.first("SELECT GROUP_CONCAT(id SEPARATOR '|') grp FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getId(),"' AND type='",datas.getInt("type"),"'  AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status)").getString("grp");
					}		
					
					t.start_record();
					t.setVar(	"sshipgroup.name",			types.get(typeGroupID)+" x "+mastertype.getString("nickname"),
								"sshipgroup.idlist",		groupidlist,
								"sshipgroup.type.id",		datas.getInt("type"),
								"sshipgroup.owner.id",		datas.getInt("owner"),
								"sshipgroup.owner.name",	Common._title(datas.getString("username")),
								"sshipgroup.type.name",		mastertype.getString("nickname"),
								"sshipgroup.sublist",		0,																		
								"sshipgroup.type.image",	mastertype.getString("picture"),
								"sshipgroup.own",			datas.getInt("owner") == user.getId(),
								"sshipgroup.count",			types.get(typeGroupID) + (data.getInt("type") == datas.getInt("type") ? 1 : 0) - ownfleetcount,
								"sshipgroup.fleetlesscount",	fleetlesscount );
		
					if( datas.getInt("owner") == user.getId() ) {
						t.setVar("sshipgroup.ownship",1);
					} else {
						t.setVar("sshipgroup.ownship",0);
					}
									
					t.parse("sships.list","sshipgroup.listitem",true);
					t.stop_record();
					t.clear_record();									
					types.put(typeGroupID, -1);	// einmal anzeigen reicht
				} 
				else if( (this.showOnly != 0) || !types.containsKey(typeGroupID) || (types.get(typeGroupID) != -1) ) {
					if( (this.showOnly != 0) && firstentry ) {
						int count = datas.numRows();		
						
						int fleetlesscount = 0;
						int ownfleetcount = 0;					
						if( datas.getInt("owner") == user.getId() ) {
							fleetlesscount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getId(),"' AND type='",datas.getInt("type"),"' AND docked='' AND fleet=0").getInt("count");
							if( data.getInt("fleet") != 0 ) {
								ownfleetcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getId(),"' AND type='",datas.getInt("type"),"' AND docked='' AND fleet=",data.getInt("fleet")).getInt("count");
							}
						}	
						
						t.setVar(	"sshipgroup.name",			count+" x "+mastertype.getString("nickname"),
									"sshipgroup.type.id",		datas.getInt("type"),
									"sshipgroup.owner.id",		datas.getInt("owner"),
									"sshipgroup.owner.name", 	Common._title(datas.getString("username")),
									"sshipgroup.type.name",		mastertype.getString("nickname"),
									"sshipgroup.sublist", 		1,																		
									"sshipgroup.type.image",	mastertype.getString("picture"),
									"sshipgroup.own",			datas.getInt("owner") == user.getId(),
									"sshipgroup.count",			count + (data.getInt("type") == datas.getInt("type") ? 1 : 0) - ownfleetcount,
									"sshipgroup.fleetlesscount",	fleetlesscount );
				
						if( datas.getInt("owner") == user.getId() ) {
							t.setVar("sshipgroup.ownship",1);
						} 
						else {
							t.setVar("sshipgroup.ownship",0);
						}			
						t.parse("sships.list","sshipgroup.listitem",true);	
						
						firstentry = false;
					}
					t.start_record();
					t.setVar(	"sships.id",			datas.getInt("id"),
								"sships.owner.id" ,		datas.getInt("owner"),
								"sships.owner.name",	Common._title(datas.getString("username")),
								"sships.name",			Common._plaintitle(datas.getString("name")),
								"sships.type.id",		datas.getInt("type"),
								"sships.hull",			Common.ln(datas.getInt("hull")),
								"sships.shields",		Common.ln(datas.getInt("shields")),
								"sships.fleet.id",		datas.getInt("fleet"),
								"sships.type.name",		ashiptype.getString("nickname"),
								"sships.type.image",	ashiptype.getString("picture"),
								"sships.docked.id",		datas.getString("docked") );

					boolean disableIFF = datas.getString("status").indexOf("disable_iff") > -1;
					t.setVar("sships.disableiff",disableIFF);
		
					if( datas.getInt("owner") == user.getId() ) {
						t.setVar("sships.ownship",1);
					} else {
						t.setVar("sships.ownship",0);
					}

					if( disableIFF ) t.setVar("sships.owner.name","Unbekannt");
		
					if( datas.getInt("fleet") > 0 ) {
						if( !fleetcache.containsKey(datas.getInt("fleet")) ) {
							fleetcache.put(datas.getInt("fleet"), db.first("SELECT * FROM ship_fleets WHERE id="+datas.getInt("fleet")));
						}
						t.setVar("sships.fleet.name",Common._plaintitle(fleetcache.get(datas.getInt("fleet")).getString("name")));
					}
					// Gedockte Schiffe zuordnen (gelandete brauchen hier nicht beruecksichtigt werden, da sie von der Query bereits aussortiert wurden)
					if( !datas.getString("docked").equals("") ) {
						SQLResultRow namerow = db.first("SELECT name FROM ships WHERE id>0 AND id="+datas.getString("docked"));
						if( namerow.isEmpty() ) {
							LOG.warn("Schiff "+datas.getInt("id")+" hat ungueltigen Dockeintrag '"+datas.getInt("docked")+"'");
						}
						else {
							String shipname = namerow.getString("name");
							t.setVar("sships.docked.name",shipname);
						}
					}
					
					// Anzeige Heat (Standard)
					if( ShipTypes.hasShipTypeFlag(datatype, ShipTypes.SF_SRS_EXT_AWAC) ) {
						t.setVar("sships.heat",datas.get("s"));
						
						// Anzeige Heat
						if( datas.getInt("s") == 0 ) {
							t.setVar("sships.heat.none",1);
						}
						if( (datas.getInt("s") > 0) && (datas.getInt("s") <= 100) ) {
							t.setVar("sships.heat.medium",1);
						} else if( datas.getInt("s") > 100 ) {
							t.setVar("sships.heat.hight",1);
						}
		
						// Anzeige Crew
						if( (datas.getInt("crew") == 0) && (ashiptype.getInt("crew") != 0) ) {
							t.setVar("sships.nocrew",1);
						} else if( datas.getInt("crew") > 0 ) {
							t.setVar("sships.crew",datas.get("crew"));
						}
		
						// Anzeige Energie
						if( datas.getInt("e") == 0 ) {
							t.setVar("sships.noe",1);
						} else if( datas.getInt("e") > 0 ) {
							t.setVar("sships.e",datas.get("e"));
						}
					} 
					else if( ShipTypes.hasShipTypeFlag(datatype, ShipTypes.SF_SRS_AWAC) ) {
						t.setVar("global.standartawac",1);
						
						if( datas.getInt("s") > 100 ) {
							t.setVar("sships.heat.high",1);
						} else if( datas.getInt("s") > 40 ) {
							t.setVar("sships.heat.medium",1);
						} else if( datas.getInt("s") > 0 ) {
							t.setVar("sships.heat.low",1);
						} else {
							t.setVar("sships.heat.none",1);
						}
					}

					//Angreifen
					if( !disableIFF && (datas.getInt("owner") != user.getId()) && (datas.getInt("battle")==0) && (datatype.getInt("military") != 0) ) {
						if( ( (user.getAlly() > 0) && (datas.getInt("ally") != user.getAlly()) ) || (user.getAlly() == 0) ) {
							t.setVar("sships.action.angriff",1);
						}
					}

					// Anfunken
					if( datas.getString("oncommunicate") != null && !datas.getString("oncommunicate").equals("") ) {
						boolean found = true;
						if( datas.getString("oncommunicate").indexOf("*:") == -1 ) {
							found = false;
							String[] comlist = datas.getString("oncommunicate").split(";");
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
					if( datas.getString("jumptarget").length() > 0 ) {
						/*
							Ermittlung der Sprungberechtigten
							moeglich sind: default,all,user,ally,ownally,group
						 */
						String[] target = StringUtils.split(datas.getString("jumptarget"), '|');
						String[] targetuser = StringUtils.split(target[2], ':');
						if( targetuser[0].equals("all") ) {
							t.setVar("sships.action.jump",1);
						}
						else if( targetuser[0].equals("ally") ) {
							if(  (user.getAlly() > 0) && (Integer.parseInt(targetuser[1]) == user.getAlly()) ) {
								t.setVar("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("user") ) {
							if ( Integer.parseInt(targetuser[1]) == user.getId() ){
								t.setVar("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("ownally") ) {
							if ( (user.getAlly() > 0) && (datas.getInt("ally") == user.getAlly()) ){
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
							if( ( (user.getAlly() > 0) && (datas.getInt("ally") == user.getAlly()) ) || ((user.getAlly() == 0) && (datas.getInt("owner") == user.getId()) ) ) {
								t.setVar("sships.action.jump",1);
							}
						}
					}

					//Handeln, Pluendernlink, Waren transferieren
					if( datas.getString("status").indexOf("tradepost") != -1 ) {
						t.setVar("sships.action.trade",1);
					} 
					else if( !disableIFF && (datas.getInt("owner") == -1) && (ashiptype.getInt("class") == ShipClasses.SCHROTT.ordinal() || ashiptype.getInt("class") == ShipClasses.FELSBROCKEN.ordinal()) ) {
						t.setVar("sships.action.transferpluender",1);
					} 
					else if( !disableIFF || (datas.getInt("owner") == user.getId()) ) {
						t.setVar("sships.action.transfer",1);
					}

					//Bemannen, Kapern
					if( !disableIFF && (datas.getInt("owner") != user.getId()) && (ashiptype.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) &&
						((datas.getInt("owner") != -1) || (ashiptype.getInt("class") == ShipClasses.SCHROTT.ordinal() || ashiptype.getInt("class") == ShipClasses.FELSBROCKEN.ordinal())) ) {
						if( ( (user.getAlly() > 0) && (datas.getInt("ally") != user.getAlly()) ) || (user.getAlly() == 0) ) {
							if( !ShipTypes.hasShipTypeFlag(ashiptype, ShipTypes.SF_NICHT_KAPERBAR) ) {
								t.setVar("sships.action.kapern",1);
							}
							else {
								t.setVar("sships.action.pluendern",1);
							}
						}
					} else if( !disableIFF && (datas.getInt("owner") == user.getId()) && (ashiptype.getInt("crew") > 0)  ) {
						t.setVar("sships.action.crewtausch",1);
					}

					//Offiziere: Captain transferieren
					boolean hasoffizier = datas.getString("status").indexOf("offizier") != -1;
					if( !disableIFF && (offizier != null) && (!hasoffizier || ShipTypes.hasShipTypeFlag(ashiptype, ShipTypes.SF_OFFITRANSPORT) ) ) {
						if( ashiptype.getInt("size") > 2 ) {
							boolean ok = true;
							if( ShipTypes.hasShipTypeFlag(ashiptype, ShipTypes.SF_OFFITRANSPORT) ) {
								int officount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s "+datas.getInt("id")+"'").getInt("count");
								
								if( officount >= ashiptype.getInt("crew") ) {
									ok = false;
								}
							}
							
							if( ok ) {
								t.setVar("sships.action.tcaptain",1);
							}
						}
					}

					//Schiff in die Werft fliegen
					if( (datas.getInt("owner") == user.getId()) && !ashiptype.getString("werft").equals("") ) {
						t.setVar("sships.action.repair",1);
					}

					//Externe Docks: andocken
					if( datas.getString("docked").length() == 0 && ( datatype.getInt("adocks") > dockCount ) && 
						( (datas.getInt("owner") == user.getId() ) || superdock) ) {
						if( superdock || ( ashiptype.getInt("size") < 3 ) ) {
							t.setVar("sships.action.aufladen",1);
						}
					}

					//Jaegerfunktionen: laden, Flotte landen
					if( ShipTypes.hasShipTypeFlag(datatype, ShipTypes.SF_JAEGER) && (currentDockID != datas.getInt("id")) ) {
						if( ( ashiptype.getInt("jdocks") > 0 ) && ( datas.getInt("owner") == user.getId() ) ) {
							int carrierFullCount = db.first("SELECT count(*) fullcount FROM ships WHERE id>0 AND docked='l ",datas.getInt("id"),"'").getInt("fullcount");

							if( carrierFullCount + 1 <= ashiptype.getInt("jdocks") ) {
								t.setVar("sships.action.land",1);
								if( data.getInt("fleet") > 0 ) {
									boolean ok = true;
									// Falls noch nicht geschehen die Flotte des Jaegers ermitteln
									if( fleetlist == null ) {
										fleetlist = new ArrayList<Integer>();
										
										SQLQuery tmp = db.query("SELECT id,type,status FROM ships WHERE id>0 AND fleet='"+data.getInt("fleet")+"'");
										while( tmp.next() ) {
											SQLResultRow tmptype = ShipTypes.getShipType( tmp.getRow() );
											if( !ShipTypes.hasShipTypeFlag(tmptype, ShipTypes.SF_JAEGER) ) {
												ok = false;
												break;
											}
											fleetlist.add(tmp.getInt("id"));
										}		
										tmp.free();
										if( !ok ) {
											fleetlist.clear();
										}
									}

									if( !fleetlist.isEmpty() && (fleetlist.size() <= ashiptype.getInt("jdocks")) ) {
										if( carrierFullCount + fleetlist.size() <= ashiptype.getInt("jdocks") )
											t.setVar(	"sships.action.landfleet", 1,
														"global.shiplist", Common.implode("|",fleetlist) );
									}
								}
							}
						}
					}
				
					//Aktuellen Jaeger auf dem (ausgewaehlten) Traeger laden lassen
					if( (datas.getInt("owner") == user.getId()) && spaceToLand && ShipTypes.hasShipTypeFlag(ashiptype, ShipTypes.SF_JAEGER) ) {
						t.setVar("sships.action.landthis",1);
						
						// Flotte des aktuellen Jaegers landen lassen
						if( datas.getInt("fleet") != 0 ) {
							if( !jaegerFleetCache.containsKey(datas.getInt("fleet"))) {
								List<Integer> thisFleetList = new ArrayList<Integer>();
								
								boolean ok = true;
								SQLQuery tmp = db.query("SELECT id,type,status FROM ships WHERE id>0 AND fleet='"+datas.getInt("fleet")+"'");
								while( tmp.next() ) {
									SQLResultRow tmptype = ShipTypes.getShipType( tmp.getRow() );
									if( !ShipTypes.hasShipTypeFlag(tmptype, ShipTypes.SF_JAEGER) ) {
										ok = false;
										break;
									}
									thisFleetList.add(tmp.getInt("id"));
								}		
								tmp.free();
								if( !ok ) {
									thisFleetList.clear();
								}
								
								jaegerFleetCache.put(datas.getInt("fleet"), thisFleetList);
							}
							List<Integer> thisFleetList = jaegerFleetCache.get(datas.getInt("fleet"));
							
							if( !thisFleetList.isEmpty() && (thisFleetList.size() <= datatype.getInt("jdocks")) ) {
								if( fullcount + thisFleetList.size() <= datatype.getInt("jdocks") )
									t.setVar(	"sships.action.landthisfleet", 1,
												"sships.shiplist", Common.implode("|",thisFleetList) );
							}
						}
					}

					//Flottenfunktionen: anschliessen
					if( datas.getInt("owner") == user.getId() ) {
						if( (data.getInt("fleet") <= 0) && (datas.getInt("fleet")>0) ) {
							t.setVar("sships.action.joinfleet",1);
						}
						else if( (data.getInt("fleet") > 0) && (datas.getInt("fleet") <= 0) ) {
							t.setVar("sships.action.add2fleet",1);
						}
						else if( (datas.getInt("fleet")<=0) && (data.getInt("fleet")<=0) ) {
							t.setVar("sships.action.createfleet",1);
						}
					}
					t.parse("sships.list","sships.listitem",true);
					t.stop_record();
					t.clear_record();
				}
			}
			datas.free();
		}
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
