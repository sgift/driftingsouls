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
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.werften.ShipWerft;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

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
public class SchiffeController extends DSGenerator implements Loggable {
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
	public void changeJDockedAction() {	
		parameterNumber("showLJaeger");
		
		this.getUser().setUserValue("TBLORDER/schiffe/showjaeger", Integer.toString(getInteger("showLJaeger")));
		
		this.redirect();
	}
	
	@Override
	public void defaultAction() {		
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		String only = getString("only");
		int low = getInteger("low");
		int crewless = getInteger("crewless");
		int listoffset = getInteger("listoffset");
		
		t.set_var(	"global.low",		low,
				  	"global.crewless",	crewless,
				  	"global.only",		only,
				  	"user.race",		user.getRace());
		
		String ord = user.getUserValue("TBLORDER/schiffe/order");
		String mode = user.getUserValue("TBLORDER/schiffe/mode");
		String showjaeger = user.getUserValue("TBLORDER/schiffe/showjaeger");
		
		Map<String,String> ordermapper = new HashMap<String,String>();
		ordermapper.put("id", "id");
		ordermapper.put("name", "name,id");
		ordermapper.put("type", "type,id");
		ordermapper.put("sys", "system,x+y,id");
		ordermapper.put("crew", "crew,id");
		ordermapper.put("hull", "hull,id");
		ordermapper.put("e", "e,id");
		
		String ow = ordermapper.get(ord);
		
		String query = "SELECT t1.* "+
			"FROM ships AS t1,ship_types AS t2 "+
			"WHERE t1.id>0 AND t1.owner="+user.getID()+" AND t2.id=t1.type AND ";
		
		if( low != 0 ) {
			query += "(LOCATE('mangel_nahrung',t1.status) OR LOCATE('mangel_reaktor',t1.status)) AND ";
		}
		if( crewless != 0 ) {
			query += "LOCATE('nocrew',t1.status) AND ";
		}
		
		if( only.equals("kampf") && (showjaeger.equals("0")) ) {
			query += "!LOCATE('l ',t1.docked) AND ";
		}
		
		if( only.equals("tank") )	{
			query += "t2.class=3 ORDER BY "+ow;
		}
		else if( only.equals("def") )	{
			query += "t2.class=10 ORDER BY "+ow;
		}
		else if( only.equals("werften") )	{
			query += "t2.werft!='' ORDER BY "+ow;
		}
		else if( only.equals("sensor") ) {
			query += "(t2.class=13 OR t2.class=11) ORDER BY "+ow;
		}
		else if( only.equals("cargo") )	{
			query += "t2.class=8 ORDER BY "+ow;
		}
		else if( only.equals("trans") ) 	{
			query += "t2.class=1 ORDER BY "+ow;
		}
		else if( only.equals("zivil") ) 	{
			query += "(!LOCATE('=',t2.weapons) OR LOCATE('tblmodules',t1.status)) ORDER BY "+ow;
		}
		else if( only.equals("kampf") ) {
			String sql_only = null;
			
			if( getInteger("kampf_only") == 0 ) {
				sql_only = "t2.class IN (2,4,5,6,7,9,15,16,17)";
			}
			else {
				sql_only = "t2.class="+getInteger("kampf_only");
				t.set_var("global.kampf_only",getInteger("kampf_only"));
			}
			query += sql_only+" ORDER BY "+ow;
		}
		else {
			query += "t2.class > -1 ORDER BY "+ow;
		}
		
		query += " LIMIT "+listoffset+",1501";

		if( only.equals("tank") ) {
			t.set_var("only.tank", 1);
		} 
		else if( only.equals("kampf") ) {
			t.set_var(	"only.kampf", 1,
				 		"only.kampf.showljaeger", (showjaeger.equals("1")? "checked=\"checked\"":"") );
		
			if( getInteger("kampf_only") == 0 ) {
				t.set_var("only.kampf.selected-1","selected=\"selected\"");
			}
			else {
				t.set_var("only.kampf.selected"+getInteger("kampf_only"), "selected=\"selected\"");
			}
		} 
		else {
			t.set_var("only.other",1);
		}
		
		String[] alarms = {"yellow","red"};

		Map<Integer,String> fleetcache = new HashMap<Integer,String>();
		
		int shiplistcount = 0;

		t.set_block("_SCHIFFE","schiffe.listitem","schiffe.list");
		t.set_block("schiffe.listitem","schiffe.resitem","schiffe.reslist");
		SQLQuery ship = db.query(query);
		while( ship.next() ) {
			t.start_record();
			
			shiplistcount++;
			
			if( shiplistcount > 1500 ) {
				t.set_var("schiffe.nextoffset", listoffset+1500);
				break;	
			}
			
			ResourceID[] normwarenlist = {Resources.NAHRUNG,Resources.URAN,Resources.DEUTERIUM,Resources.ANTIMATERIE,Resources.BATTERIEN};
			
			SQLResultRow shiptype = ShipTypes.getShipType( ship.getRow() );
			
			Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
	
			if( only.equals("zivil") && shiptype.getInt("military") != 0 ) {
				continue;	
			}
			
			int nr = 0;
			int er = 0;
			
			boolean ok = false;
			if( low != 0 ) {
				if( ship.getString("status").indexOf("mangel_nahrung") > -1 ) {
					nr = 1;
				}
				else {
					nr = low+1;
				}
		
				if( ship.getString("status").indexOf("mangel_reaktor") > -1 ) {
					er = low/2-1;
				}
				else {
					er = low;
				}
			}	
			
			if( ok == false ) {
				String offi = null;
				
				if( ship.getString("status").indexOf("offizier") > -1 ) {
					Offizier offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
					offi = " <a class=\"forschinfo\" href=\""+Common.buildUrl(getContext(), "default", "module", "choff", "off", offizier.getID())+"\"><img style=\"vertical-align:middle\" src=\""+offizier.getPicture()+"\" alt=\"Rang "+offizier.getRang()+"\" /></a>";
				} 

				String crewcolor = "#ffffff";
				if( ship.getInt("crew") < shiptype.getInt("crew")/2 ) {
					crewcolor = "#ff0000";
				}
				else if( ship.getInt("crew") < shiptype.getInt("crew") ) {
					crewcolor = "#ffcc00";
				}

				String hullcolor = "#ffffff";
				if( ship.getInt("hull") < shiptype.getInt("hull")/2 ) {
					hullcolor = "#ff0000";
				}
				else if( ship.getInt("hull") < shiptype.getInt("hull") ) {
					hullcolor = "#ffcc00";
				}

				if( shiptype.getString("werft").length() > 0 ) {
					SQLResultRow werftRow = db.first("SELECT * FROM werften WHERE shipid=",ship.getInt("id"));
					if( werftRow.isEmpty() ) {
						LOG.warn("Schiff "+ship.getInt("id")+" hat keinen Werfteintrag");
					}
					else {
						ShipWerft werft = new ShipWerft(werftRow,shiptype.getString("werft"),ship.getInt("system"),ship.getInt("owner"),ship.getInt("id"));
						werft.setOneWayFlag(shiptype.getInt("ow_werft"));
						SQLResultRow type = werft.getBuildShipType();
						if( type != null ) {
							StringBuilder popup = new StringBuilder(100);
							popup.append(Common.tableBegin(420, "left").replace( '"', '\'') );
							popup.append("<img align='left' border='0' src='"+type.getString("picture")+"' alt='"+type.getString("nickname")+"' />");
							popup.append("&nbsp;Baut: "+type.getString("nickname")+"<br />");
							popup.append("&nbsp;Dauer: <img style='vertical-align:middle' src='"+Configuration.getSetting("URL")+"data/interface/time.gif' alt='noch ' />"+werft.getRemainingTime()+"<br />");
							if( werft.getRequiredItem() != -1 ) {					
								popup.append("&nbsp;Ben&ouml;tigt: ");
								popup.append("<img style='vertical-align:middle' src='../data/items/"+Items.get().item(werft.getRequiredItem()).getPicture()+"' alt='' />");
								if( werft.isBuildContPossible() ) {
									popup.append("<span style='color:green'>");
								}
								else {
									popup.append("<span style='color:red'>");
								}
								popup.append(Items.get().item(werft.getRequiredItem()).getName()+"</span>");
							}
							popup.append(Common.tableEnd().replace( '"', '\'' ));
							String popupStr = StringEscapeUtils.escapeJavaScript(popup.toString().replace(">", "&gt;").replace("<", "&lt;"));
	
							t.set_var(	"ship.werft.popup",		popupStr,
										"ship.werft.dauer",		werft.getRemainingTime(),
										"ship.werft.building",	1 );
						}
					}
				}

				t.set_var(	"ship.id",		ship.getInt("id"),
							"ship.name",	Common._plaintitle(ship.getString("name")),
							"ship.battle",	ship.getInt("battle"),
							"ship.type",	ship.getInt("type"),
							"ship.type.name",	shiptype.getString("nickname"),
							"ship.location",	Ships.getLocationText(ship.getRow(),false),
							"ship.e",		ship.getInt("e"),
							"ship.hull",	Common.ln(ship.getInt("hull")),
							"ship.hullcolor",	hullcolor,
							"ship.image",	shiptype.getString("picture"),
							"ship.crew",	ship.getInt("crew"),
							"ship.alarm",	alarms[ship.getInt("alarm")],
							"ship.offi",	offi,
							"ship.crewcolor",	crewcolor,
							"ship.fleet",	ship.getInt("fleet"),
							"ship.shields",	Common.ln(ship.getInt("shields")),
							"ship.werft",	shiptype.getString("werft"),
							"ship.adocks",	shiptype.getInt("adocks"),
							"ship.jdocks",	shiptype.getInt("jdocks"),
							"ship.docks",	shiptype.getInt("adocks") + shiptype.getInt("jdocks"),
							"schiffe.reslist", "" );
				
				if( ship.getInt("fleet") > 0 ) {
					if( !fleetcache.containsKey(ship.getInt("fleet")) ) {
						fleetcache.put(ship.getInt("fleet"), db.first("SELECT name FROM ship_fleets WHERE id=",ship.getInt("fleet")).getString("name"));
					}
					t.set_var("ship.fleet.name",Common._plaintitle(fleetcache.get(ship.getInt("fleet"))) );
				}
				
				if( !ship.getString("docked").equals("") ) {
					if( ship.getString("docked").charAt(0) != 'l' ) {
						String shipname = db.first("SELECT name FROM ships WHERE id>0 AND id=",ship.getString("docked")).getString("name");
						t.set_var(	"ship.docked.name",	shipname,
									"ship.docked.id",	ship.getString("docked") );
					}
					else {
					 	String[] dockid = StringUtils.split(ship.getString("docked"), ' ');
			 			String shipname = db.first("SELECT name FROM ships WHERE id>0 AND id=",dockid[1]).getString("name");
						t.set_var(	"ship.landed.name",	shipname,
									"ship.landed.id",	dockid[1] );
					}
				}
				
 				if( shiptype.getInt("adocks") > 0 ) {
 					int docked = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='",ship.getInt("id"),"'").getInt("count");
					t.set_var("ship.adocks.docked",docked);
 				}
				
				if( shiptype.getInt("jdocks") > 0 ) {
					int docked = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='l ",ship.getInt("id"),"'").getInt("count");
					t.set_var("ship.jdocks.docked",docked);
 				}
				
				if( (shiptype.getInt("class") == ShipClasses.AWACS.ordinal()) || (shiptype.getInt("class") == ShipClasses.FORSCHUNGSKREUZER.ordinal()) ) {
					t.set_var("ship.awac",1);
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
						
						if( (res.getId() == Resources.URAN) && (shiptype.getInt("ru") <= 0) ) {
							color = "";
						}
						else if( (res.getId() == Resources.ANTIMATERIE) && (shiptype.getInt("ra") <= 0) ) {
							color = "";
						}
						else if( (res.getId() == Resources.DEUTERIUM) && (shiptype.getInt("rd") <= 0) ) {
							color = "";
						}
					}
					
					if( mode.equals("norm") && !Common.inArray( res.getId(), normwarenlist) && !res.getId().isItem() ) {
						continue;
					}
					
					t.set_var(	"res.image",		res.getImage(),
								"res.color",		color,
								"res.count",		res.getCargo1(),
								"res.plainname",	res.getPlainName() );

					t.parse("schiffe.reslist","schiffe.resitem",true);
				}
				
				if( mode.equals("carg") && (shiptype.getInt("cargo") != 0) ) {
					t.set_var(	"ship.restcargo",	Common.ln(shiptype.getInt("cargo") - cargo.getMass()),
								"ship.restcargo.show", 1 );
				}
				if( (wa == 0) && (low != 0) ) {
					t.set_var("ship.e.none",1);
				}
				t.parse("schiffe.list","schiffe.listitem",true);
				
			}
			t.stop_record();
			t.clear_record();
		}
		ship.free();
	}
}