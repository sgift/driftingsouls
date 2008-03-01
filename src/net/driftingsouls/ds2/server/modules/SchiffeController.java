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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Die Schiffsliste
 * @author Christopher Jung
 *
 * @urlparam String only Die anzuzeigende Schiffsart. Falls leer werden alle Schiffe angezeigt
 * @urlparam Integer low Falls != 0 werden alle Schiffe mit Mangel angezeigt
 * @urlparam Integer crewless Falls != 0 werden alle Schiffe ohne Crew angezeigt
 * @urlparam Integer listoffset Der Offset innerhalb der Liste der Schiffe
 * @urlparam Integer kampf_only Falls != 0 werden nur Kriegsschiffe der Schiffsklasse mit der angegebenen ID angezeigt
 * 
 */
public class SchiffeController extends TemplateGenerator implements Loggable {
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffeController(Context context) {
		super(context);
		
		setTemplate("schiffe.html");
		
		parameterString("only");
		parameterNumber("low");
		parameterNumber("crewless");
		parameterNumber("listoffset");
		parameterNumber("kampf_only");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	/**
	 * Aendert den Anzeigemodus fuer den Cargo
	 * @urlparam String mode Der Anzeigemodus fuer den Cargo (<code>carg</code> oder <code>norm</code>)
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void changeModeAction() {
		parameterString("mode");
		
		String mode = getString("mode");
		if( mode.equals("carg") || mode.equals("norm") ) {
			getUser().setUserValue("TBLORDER/schiffe/mode", mode);
		}
		
		redirect();
	}
	
	/**
	 * Aendert den Sortierungsmodus fuer die Schiffe
	 * @urlparam String order Das neue Sortierkriterium
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void changeOrderAction() {
		parameterString("order");
		
		String order = getString("order");
		if( Common.inArray(order, new String[]{"id","name","type","sys","crew","hull","e"}) ) {
			getUser().setUserValue("TBLORDER/schiffe/order", order);
		}
		
		this.redirect();
	}
	
	/**
	 * Aendert den Anzeigemodus fuer gelandete Jaeger
	 * @urlparam Integer showLJaegder Falls != 0 werden gelandete Jaeger angezeigt
	 */
	@Action(ActionType.DEFAULT)
	public void changeJDockedAction() {	
		parameterNumber("showLJaeger");
		
		this.getUser().setUserValue("TBLORDER/schiffe/showjaeger", Integer.toString(getInteger("showLJaeger")));
		
		this.redirect();
	}
	
	private static final int MAX_SHIPS_PER_PAGE = 250;
	
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {		
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		String only = getString("only");
		int low = getInteger("low");
		int crewless = getInteger("crewless");
		int listoffset = getInteger("listoffset");
		
		t.setVar(	"global.low",		low,
				  	"global.crewless",	crewless,
				  	"global.only",		only,
				  	"user.race",		user.getRace());
		
		String ord = user.getUserValue("TBLORDER/schiffe/order");
		String mode = user.getUserValue("TBLORDER/schiffe/mode");
		String showjaeger = user.getUserValue("TBLORDER/schiffe/showjaeger");
		
		Map<String,String> ordermapper = new HashMap<String,String>();
		ordermapper.put("id", "s.id");
		ordermapper.put("name", "s.name,s.id");
		ordermapper.put("type", "s.shiptype,s.id");
		ordermapper.put("sys", "s.system,s.x+s.y,s.id");
		ordermapper.put("crew", "s.crew,s.id");
		ordermapper.put("hull", "s.hull,s.id");
		ordermapper.put("e", "s.e,s.id");
		
		String ow = ordermapper.get(ord);
		
		String query = "from Ship as s left join fetch s.modules "+
			"where s.id>0 and s.owner=? and ";
		
		if( low != 0 ) {
			query += "(locate('mangel_nahrung',s.status)!=0 or locate('mangel_reaktor',s.status)!=0) and locate('nocrew',s.status)=0 and ";
		}
		if( crewless != 0 ) {
			query += "locate('nocrew',s.status)!=0 and ";
		}
		
		if( only.equals("kampf") && (showjaeger.equals("0")) ) {
			query += "locate('l ',s.docked)=0 and ";
		}
		
		if( only.equals("tank") )	{
			query += "s.shiptype.shipClass=3 order by "+ow;
		}
		else if( only.equals("def") )	{
			query += "s.shiptype.shipClass=10 order by "+ow;
		}
		else if( only.equals("werften") )	{
			query += "s.shiptype.werft>0 order by "+ow;
		}
		else if( only.equals("sensor") ) {
			query += "(s.shiptype.shipClass=13 or s.shiptype.shipClass=11) order by "+ow;
		}
		else if( only.equals("cargo") )	{
			query += "s.shiptype.shipClass=8 order by "+ow;
		}
		else if( only.equals("trans") ) 	{
			query += "s.shiptype.shipClass=1 order by "+ow;
		}
		else if( only.equals("zivil") ) 	{
			query += "(locate('=',s.shiptype.weapons)=0 and (s.modules is null or locate('=',s.modules.weapons)=0)) order by "+ow;
		}
		else if( only.equals("kampf") ) {
			String sql_only = null;
			
			if( getInteger("kampf_only") == 0 ) {
				sql_only = "s.shiptype.shipClass in (2,4,5,6,7,9,15,16,17)";
			}
			else {
				sql_only = "s.shiptype.shipClass="+getInteger("kampf_only");
				t.setVar("global.kampf_only",getInteger("kampf_only"));
			}
			query += sql_only+" order by "+ow;
		}
		else {
			query += "s.shiptype.shipClass > -1 order by "+ow;
		}
		
		if( only.equals("tank") ) {
			t.setVar("only.tank", 1);
		} 
		else if( only.equals("kampf") ) {
			t.setVar(	"only.kampf", 1,
				 		"only.kampf.showljaeger", (showjaeger.equals("1")? "checked=\"checked\"":"") );
		
			if( getInteger("kampf_only") == 0 ) {
				t.setVar("only.kampf.selected-1","selected=\"selected\"");
			}
			else {
				t.setVar("only.kampf.selected"+getInteger("kampf_only"), "selected=\"selected\"");
			}
		} 
		else {
			t.setVar("only.other",1);
		}
		
		String[] alarms = {"yellow","red"};

		int shiplistcount = 0;

		t.setBlock("_SCHIFFE","schiffe.listitem","schiffe.list");
		t.setBlock("schiffe.listitem","schiffe.resitem","schiffe.reslist");
		List ships = db.createQuery(query)
			.setEntity(0, user)
			.setMaxResults(MAX_SHIPS_PER_PAGE+1)
			.setFirstResult(listoffset)
			.list();
		for( Iterator iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
			
			t.start_record();
			
			shiplistcount++;
			
			if( shiplistcount > MAX_SHIPS_PER_PAGE ) {
				t.setVar("schiffe.nextoffset", listoffset+MAX_SHIPS_PER_PAGE);
				break;	
			}
			
			ResourceID[] normwarenlist = {Resources.NAHRUNG,Resources.URAN,Resources.DEUTERIUM,Resources.ANTIMATERIE,Resources.BATTERIEN};
			
			ShipTypeData shiptype = ship.getTypeData();
			
			Cargo cargo = ship.getCargo();
	
			if( only.equals("zivil") && shiptype.isMilitary() ) {
				continue;	
			}
			
			int nr = 0;
			int er = 0;
			
			boolean ok = false;
			if( low != 0 ) {
				if( ship.getStatus().indexOf("mangel_nahrung") > -1 ) {
					nr = 1;
				}
				else {
					nr = low+1;
				}
		
				if( ship.getStatus().indexOf("mangel_reaktor") > -1 ) {
					er = low/2-1;
				}
				else {
					er = low;
				}
			}	
			
			if( ok == false ) {
				String offi = null;
				
				if( ship.getStatus().indexOf("offizier") > -1 ) {
					Offizier offizier = Offizier.getOffizierByDest('s', ship.getId());
					if( offizier != null ) {
						offi = " <a class=\"forschinfo\" href=\""+Common.buildUrl("default", "module", "choff", "off", offizier.getID())+"\"><img style=\"vertical-align:middle\" src=\""+offizier.getPicture()+"\" alt=\"Rang "+offizier.getRang()+"\" /></a>";
					}
				} 

				String crewcolor = "#ffffff";
				if( ship.getCrew() < shiptype.getCrew()/2 ) {
					crewcolor = "#ff0000";
				}
				else if( ship.getCrew() < shiptype.getCrew() ) {
					crewcolor = "#ffcc00";
				}

				String hullcolor = "#ffffff";
				if( ship.getHull() < shiptype.getHull()/2 ) {
					hullcolor = "#ff0000";
				}
				else if( ship.getHull() < shiptype.getHull() ) {
					hullcolor = "#ffcc00";
				}

				if( shiptype.getWerft() != 0 ) {
					WerftObject werft = (WerftObject)db.createQuery("from ShipWerft where shipid=?")
						.setEntity(0, ship)
						.uniqueResult();
					if( werft == null ) {
						LOG.warn("Schiff "+ship.getId()+" hat keinen Werfteintrag");
					}
					else {
						if( werft.getKomplex() != null ) {
							werft = werft.getKomplex();
						}
						
						final WerftQueueEntry[] entries = werft.getBuildQueue();
						final int totalSlots = werft.getWerftSlots();
						int usedSlots = 0;
						int buildingCount = 0;
						for( int i=0; i < entries.length; i++ ) {
							if( entries[i].isScheduled() ) {
								usedSlots += entries[i].getSlots();
								buildingCount++;
							}
						}
						
						StringBuilder popup = new StringBuilder(100);
						popup.append(Common.tableBegin(420, "left").replace( '"', '\'') );
						popup.append("Belegte Werftslots: <img style='vertical-align:middle;border:0px' src='"+Configuration.getSetting("URL")+"data/interface/schiffinfo/werftslots.png' alt='' />"+usedSlots+"/"+totalSlots+"<br />");
						popup.append("Im Bau: "+buildingCount+" Schiffe<br />");
						popup.append("In der Warteschlange: "+(entries.length - buildingCount));
						popup.append(Common.tableEnd().replace( '"', '\'' ));
						String popupStr = StringEscapeUtils.escapeJavaScript(popup.toString().replace(">", "&gt;").replace("<", "&lt;"));
	
						t.setVar(	"ship.werft.popup",		popupStr,
									"ship.werft.entries",	entries.length,
									"ship.werft.building",	1 );
					}
				}

				t.setVar(	"ship.id",		ship.getId(),
							"ship.name",	Common._plaintitle(ship.getName()),
							"ship.battle",	ship.getBattle() != null ? ship.getBattle().getId() : 0,
							"ship.type",	ship.getType(),
							"ship.type.name",	shiptype.getNickname(),
							"ship.location",	Ships.getLocationText(ship.getLocation(),false),
							"ship.e",		ship.getEnergy(),
							"ship.hull",	Common.ln(ship.getHull()),
							"ship.hullcolor",	hullcolor,
							"ship.image",	shiptype.getPicture(),
							"ship.crew",	ship.getCrew(),
							"ship.alarm",	alarms[ship.getAlarm()],
							"ship.offi",	offi,
							"ship.crewcolor",	crewcolor,
							"ship.fleet",	ship.getFleet() != null ? ship.getFleet().getId() : 0,
							"ship.shields",	Common.ln(ship.getShields()),
							"ship.werft",	shiptype.getWerft(),
							"ship.adocks",	shiptype.getADocks(),
							"ship.jdocks",	shiptype.getJDocks(),
							"ship.docks",	shiptype.getADocks() + shiptype.getJDocks(),
							"schiffe.reslist", "" );
				
				if( ship.getFleet() != null ) {
					t.setVar("ship.fleet.name",Common._plaintitle(ship.getFleet().getName()) );
				}
				
				if( ship.getDocked().length() > 0 ) {
					if( ship.getDocked().charAt(0) != 'l' ) {
						Ship master = (Ship)db.get(Ship.class, Integer.valueOf(ship.getDocked()));
						t.setVar(	"ship.docked.name",	master.getName(),
									"ship.docked.id",	master.getId() );
					}
					else {
					 	Integer dockid = Integer.valueOf(ship.getDocked().substring(2));
					 	Ship master = (Ship)db.get(Ship.class, dockid);
						t.setVar(	"ship.landed.name",	master.getName(),
									"ship.landed.id",	master.getId() );
					}
				}
				
 				if( shiptype.getADocks() > 0 ) {
 					long docked = (Long)db.createQuery("select count(*) from Ship where id>0 and docked=?")
 						.setString(0, Integer.toString(ship.getId()))
 						.iterate().next();
					t.setVar("ship.adocks.docked",docked);
 				}
				
				if( shiptype.getJDocks() > 0 ) {
					long docked = (Long)db.createQuery("select count(*) from Ship where id>0 and docked=?")
						.setString(0, "l "+ship.getId())
						.iterate().next();
					t.setVar("ship.jdocks.docked",docked);
 				}
				
				if( (shiptype.getShipClass() == ShipClasses.AWACS.ordinal()) || (shiptype.getShipClass() == ShipClasses.FORSCHUNGSKREUZER.ordinal()) ) {
					t.setVar("ship.awac",1);
				}
				
				int wa = 0;
				
				ResourceList reslist = cargo.getResourceList();
				for( ResourceEntry res : reslist ) {
					String color = "";
					if( low != 0 )  {
						if( res.getId().equals(Resources.NAHRUNG) ) {
							if( nr <= low ) {
								color = "red";
							}
						} 
						else if( Common.inArray(res.getId(),new ResourceID[] {Resources.URAN, Resources.DEUTERIUM, Resources.ANTIMATERIE}) ) {
							wa++;
							if( er <= low/2 ) {
								color = "red";
							}
						}
						if( res.getId().equals(Resources.BATTERIEN) ) { 
							color = "";
							wa--; 
						}
						else if( !Common.inArray(res.getId(),new ResourceID[] {Resources.NAHRUNG,Resources.URAN,Resources.DEUTERIUM,Resources.ANTIMATERIE,Resources.BATTERIEN} ) ) {
							color = "";
						}
						
						if( (res.getId() == Resources.URAN) && (shiptype.getRu() <= 0) ) {
							color = "";
						}
						else if( (res.getId() == Resources.ANTIMATERIE) && (shiptype.getRa() <= 0) ) {
							color = "";
						}
						else if( (res.getId() == Resources.DEUTERIUM) && (shiptype.getRd() <= 0) ) {
							color = "";
						}
					}
					
					if( mode.equals("norm") && !Common.inArray( res.getId(), normwarenlist) && !res.getId().isItem() ) {
						continue;
					}
					
					t.setVar(	"res.image",		res.getImage(),
								"res.color",		color,
								"res.count",		res.getCargo1(),
								"res.plainname",	res.getPlainName() );

					t.parse("schiffe.reslist","schiffe.resitem",true);
				}
				
				if( mode.equals("carg") && (shiptype.getCargo() != 0) ) {
					t.setVar(	"ship.restcargo",	Common.ln(shiptype.getCargo() - cargo.getMass()),
								"ship.restcargo.show", 1 );
				}
				if( (wa == 0) && (low != 0) ) {
					t.setVar("ship.e.none",1);
				}
				t.parse("schiffe.list","schiffe.listitem",true);
				
			}
			t.stop_record();
			t.clear_record();
		}
	}
}