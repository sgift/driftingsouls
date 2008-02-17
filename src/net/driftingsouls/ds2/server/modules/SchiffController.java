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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.schiffplugins.Parameters;
import net.driftingsouls.ds2.server.modules.schiffplugins.SchiffPlugin;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Die Schiffsansicht
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des anzuzeigenden Schiffes
 *
 */
public class SchiffController extends TemplateGenerator implements Loggable {
	private SQLResultRow ship = null;
	private SQLResultRow shiptype = null;
	private Offizier offizier = null;
	private Map<String,SchiffPlugin> pluginMapper = new LinkedHashMap<String,SchiffPlugin>();
	private boolean noob = false;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffController(Context context) {
		super(context);
		
		setTemplate("schiff.html");
		
		parameterNumber("ship");
	}
	
	private String genSubColor( int value, int defvalue ) {
		if( defvalue == 0 ) {
			return "green";	
		}
		
		if( value < defvalue/2 ) {
			return "red";
		} 
		else if( value < defvalue ) {
			return "yellow";
		} 
		else {
			return "green";
		}
	}
	
	private SchiffPlugin getPluginByName(String name) {
		String clsName = getClass().getPackage().getName()+".schiffplugins."+name;
		try {
			Class<?> cls = Class.forName(clsName);
			Class<? extends SchiffPlugin> spClass = cls.asSubclass(SchiffPlugin.class);
			SchiffPlugin sp = spClass.newInstance();
			return sp;
		}
		catch( Exception e ) {
			LOG.error(e,e);
			return null;
		}
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.setVar( "user.tooltips", user.getUserValue("TBLORDER/schiff/tooltips") );
		
		int shipid = getInteger("ship");
		
		ship = db.first("SELECT * FROM ships WHERE id>0 AND owner='",user.getId(),"' AND id=",shipid);
		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht", Common.buildUrl("default","module", "schiffe") );
			return false;
		}

		if( ship.getInt("battle") > 0 ) {
			addError("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl("default", "module", "angriff", "battle", ship.getInt("battle"), "ship", shipid) );
			return false;
		}


		shiptype = ShipTypes.getShipType(ship);

		offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
		
		if( !action.equals("communicate") && !action.equals("onmove") && !action.equals("onenter") && !ship.getString("lock").equals("") ) {
			ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser( ScriptParser.NameSpace.QUEST );
			scriptparser.setShip(ship);
			if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
				scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
			}
			
			Quests.executeLock(scriptparser, ship.getString("lock"), user);
		}
		
		pluginMapper.put("navigation", getPluginByName("NavigationDefault"));
		pluginMapper.put("cargo", getPluginByName("CargoDefault"));
		
		if( shiptype.getInt("werft") > 0 ) {
			pluginMapper.put("werft", getPluginByName("WerftDefault"));
		}
		
		if( ShipTypes.hasShipTypeFlag(shiptype, ShipTypes.SF_JUMPDRIVE_SHIVAN) ) {
			pluginMapper.put("jumpdrive", getPluginByName("JumpdriveShivan"));
		}
		
		pluginMapper.put("sensors", getPluginByName("SensorsDefault"));		
		
		if( shiptype.getInt("adocks") > 0 ) {
			pluginMapper.put("adocks", getPluginByName("ADocksDefault"));
		}
		
		if( shiptype.getInt("jdocks") > 0 ) {
			pluginMapper.put("jdocks", getPluginByName("JDocksDefault"));
		}
		
		noob = user.isNoob();
		
		// URL fuer Quests setzen
		Quests.currentEventURLBase.set("./main.php?module=schiff&sess="+getString("sess")+"&ship="+getInteger("ship"));
		
		return true;	
	}
	
	/**
	 * Wechselt die Alarmstufe des Schiffes
	 * @urlparam Integer alarm Die neue Alarmstufe
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void alarmAction() {
		if( noob ) {
			redirect();
			return;	
		}
		
		if( (shiptype.getInt("class") == ShipClasses.GESCHUETZ.ordinal()) || (shiptype.getInt("military") == 0) ) {
			redirect();
			return;	
		}
		
		parameterNumber("alarm");
		int alarm = getInteger("alarm");
		
		Database db = getDatabase();
		
		if( (alarm >= 0) && (alarm <= 1) ) { 
			db.update("UPDATE ships SET alarm=",alarm," WHERE id>0 AND id=",ship.getInt("id"));
			
			getTemplateEngine().setVar("ship.message", "Alarmstufe erfolgreich ge&auml;ndert<br />");
		}
		
		Ships.recalculateShipStatus(ship.getInt("id"));
		
		redirect();
	}
	
	/**
	 * Uebergibt das Schiff an einen anderen Spieler
	 * @urlparam Integer newowner Die ID des neuen Besitzers
	 * @urlparam Integer conf 1, falls die Sicherheitsabfrage positiv bestaetigt wurde
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void consignAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("newowner");
		int newownerID = getInteger("newowner");
		
		User newowner = (User)getDB().get(User.class, newownerID);
		
		parameterNumber("conf");
		int conf = getInteger("conf");
		
		if( conf == 0 ) {
			String text = "<span style=\"color:white\">Wollen sie das Schiff "+Common._plaintitle(ship.getString("name"))+" ("+ship.getInt("id")+") wirklich an "+newowner.getProfileLink()+" &uuml;bergeben?</span><br />";
			text += "<a class=\"ok\" href=\""+Common.buildUrl("consign", "ship", ship.getInt("id"), "conf", 1 , "newowner", newowner.getId())+"\">&Uuml;bergeben</a></span><br />";
			t.setVar( "ship.message", text );
			
			redirect();
			return;
		}
		
		int fleet = ship.getInt("fleet");
		
		boolean result = Ships.consign(user, ship, newowner, false );
			
		if( result ) {
			t.setVar("ship.message", Ships.MESSAGE.getMessage());
					
			redirect();
		}
		else {
			String msg = "Ich habe dir die "+ship.getString("name")+" ("+ship.getInt("id")+"), ein Schiff der "+shiptype.getString("nickname")+"-Klasse, &uuml;bergeben\nSie steht bei "+ship.getInt("system")+":"+ship.getInt("x")+"/"+ship.getInt("y");
			PM.send(getContext(), user.getId(), newowner.getId(), "Schiff &uuml;bergeben", msg);
		
			String consMessage = Ships.MESSAGE.getMessage();
			t.setVar("ship.message", (!consMessage.equals("") ? consMessage+"<br />" : "")+"<span style=\"color:green\">Das Schiff wurde erfolgreich an "+newowner.getProfileLink()+" &uuml;bergeben</span><br />");
			
			if( fleet != 0 ) {
				int fleetcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND fleet="+fleet).getInt("count");
			
				if( fleetcount < 3 ) {
					db.update("UPDATE ships SET fleet=null WHERE id>0 AND fleet="+fleet);
					db.update("DELETE FROM ship_fleets WHERE id>0 AND id="+fleet);
				}
			}	
		}
		
		return;
	}
	
	/**
	 * Zerstoert das Schiff
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void destroyAction() {
		TemplateEngine t = getTemplateEngine();

		if( !ship.getString("lock").equals("") ) {
			t.setVar("ship.message", "<span style=\"color:red\">Dieses Schiff kann sich nicht selbstzerst&ouml;ren, da es in ein Quest eingebunden ist</span><br />");
			redirect();
			return;
		}
	
		parameterNumber("conf");
		int conf = getInteger("conf");
	
		if( conf == 0 ) {
			String text = "<span style=\"color:white\">Wollen sie Selbstzerst&ouml;rung des Schiffes "+Common._plaintitle(ship.getString("name"))+" ("+ship.getInt("id")+") wirklich ausf&uuml;hren?</span><br />\n";
			text += "<a class=\"error\" href=\""+Common.buildUrl("destroy", "ship", ship.getInt("id"), "conf", 1)+"\">Selbstzerst&ouml;rung</a></span><br />";
			t.setVar("ship.message", text);
			
			redirect();
			return;
		}
	
		Ships.destroy( ship.getInt("id") );

		t.setVar("ship.message", "<span style=\"color:white\">Das Schiff hat sich selbstzerst&ouml;rt</span><br />");
		return;
	}
	
	/**
	 * Springt durch den angegebenen Sprungpunkt
	 * @urlparam Integer knode Die ID des Sprungpunkts
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void jumpAction() {
		TemplateEngine t = getTemplateEngine();
		
		if( (shiptype.getInt("cost") == 0) || (ship.getInt("engine") == 0) ) {
			redirect();
			return;
		}
		
		parameterNumber("node");
		int node = getInteger("node");
		
		if( node != 0 ) {
			Ships.jump(ship.getInt("id"), node, false);
			t.setVar("ship.message", Ships.MESSAGE.getMessage());
		}

		redirect();
	}
	
	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt
	 * @urlparam Integer knode Die ID des Schiffes mit dem Sprungpunkt
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void kjumpAction() {
		TemplateEngine t = getTemplateEngine();
		
		if( (shiptype.getInt("cost") == 0) || (ship.getInt("engine") == 0) ) {
			redirect();
			return;
		}
		
		parameterNumber("knode");
		int knode = getInteger("knode");
		
		if( knode != 0 ) {
			Ships.jump(ship.getInt("id"), knode, true);
			t.setVar("ship.message", Ships.MESSAGE.getMessage());
		}

		redirect();
	}
	
	/**
	 * Benennt das Schiff um
	 * @urlparam String newname Der neue Name des Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void renameAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("newname");
		String newname = getString("newname");
		
		db.prepare("UPDATE ships SET name= ? WHERE id= ?").update(newname, ship.getInt("id"));
		t.setVar("ship.message", "Name zu "+Common._plaintitle(newname)+" ge&auml;ndert<br />");
		ship.put("name", newname);
	
		redirect();
	}
	
	/**
	 * Fuehrt Aktionen eines Plugins aus. Plugin-spezifische
	 * Parameter haben die Form $PluginName_ops[$ParameterName]
	 * @urlparam String plugin Der Name des Plugins 
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void pluginAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterString("plugin");
		String plugin = getString("plugin");

		Parameters caller = new Parameters();
		caller.controller = this;
		caller.pluginId = plugin;
		caller.ship = ship;
		caller.shiptype = shiptype;
		caller.offizier = offizier;

		if( !pluginMapper.containsKey(plugin) ) {
			redirect();
			return;	
		}
		
		parseSubParameter(plugin+"_ops");
		t.setVar("ship.message", pluginMapper.get(plugin).action(caller));
		
		parseSubParameter("");
		
		redirect();
	}
	
	/**
	 * Landet die angegebenen Schiffe auf dem aktuellen Schiff
	 * @urlparameter String shiplist Eine mit | separierte Liste an Schiffs-IDs
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void landAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterString("shiplist");
		String shipIdList = getString("shiplist");
		
		if( shipIdList.equals("") ) {
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Ships.dock(Ship.DockMode.LAND, user.getId(), ship.getInt("id"), shiplist);
		t.setVar("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Startet die angegebenen Schiffe vom aktuellen Schiff
	 * @urlparam String shiplist Eine mit | separierte Liste von Schiffs-IDs
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void startAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterString("shiplist");
		String shipIdList = getString("shiplist");
		
		if( shipIdList.equals("") ) {
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Ships.dock(Ship.DockMode.START, user.getId(), ship.getInt("id"), shiplist);
		t.setVar("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Dockt die angegebenen Schiffe an das aktuelle Schiff an
	 * @urlparam String shiplist Eine mit | separierte Liste von Schiffs-IDs 
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void aufladenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterString("tar");
		String shipIdList = getString("tar");
		
		if( shipIdList.equals("") ) {
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Database db = getContext().getDatabase();
		
		SQLQuery docked = db.query("SELECT id,docked FROM ships WHERE id > 0 AND id IN ("+Common.implode(",", shiplist)+") AND docked!=''");
		while( docked.next() ) {
			String target = docked.getString("docked");
			int targetID = 0;
			if( target.charAt(0) == 'l' ) {
				targetID = Integer.parseInt(target.substring(2));
			}
			else {
				targetID = Integer.parseInt(target);
			}
			
			Ships.dock(Ship.DockMode.UNDOCK, user.getId(), targetID, new int[] {docked.getInt("id")});
		}
		
		Ships.dock(Ship.DockMode.DOCK, user.getId(), ship.getInt("id"), shiplist);
		t.setVar("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Dockt die angegebenen Schiffe vom aktuellen Schiff ab
	 * @urlparam String shiplist Eine mit | separierte Liste von Schiffs-IDs
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void abladenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterString("tar");
		String shipIdList = getString("tar");
		
		if( shipIdList.equals("") ) {
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Ships.dock(Ship.DockMode.UNDOCK, user.getId(), ship.getInt("id"), shiplist);
		t.setVar("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Laesst ein Schiff einer Flotte beitreten oder aus der aktuellen Flotte austreten
	 * @urlparam Integer join Die ID der Flotte, der das Schiff beitreten soll oder <code>0</code>, falls es aus der aktuellen Flotte austreten soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void joinAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("join");
		int join = getInteger("join");
		
		SQLResultRow fleetship = db.first("SELECT name,x,y,system,owner,fleet,`lock` FROM ships WHERE id>0 AND id=",join);

		// Austreten
		if( (join == 0) && ship.getString("lock").equals("") ) {
			Ships.removeFromFleet(ship);
			ship.put("fleet", 0);
			
			t.setVar("ship.message", "<span style=\"color:green\">"+Ships.MESSAGE.getMessage()+"</span><br />");
		} 
		else if( join == 0 ) {
			t.setVar("ship.message", "<span style=\"color:red\">Dieses Schiff kann nicht aus der Flotte austreten, da diese in ein Quest eingebunden ist</span><br />");		
		}
		// Beitreten
		else {
			SQLResultRow fleet = db.first("SELECT id,name FROM ship_fleets WHERE id='",fleetship.getInt("fleet"),"'");
		
			if( !fleetship.getString("lock").equals("") || !ship.getString("lock").equals("") ) {
				t.setVar("ship.message", "<span style=\"color:red\">Sie k&oumlnnen der Flotte nicht beitreten, solange entweder das Schiff oder die Flotte in ein Quest eingebunden ist</span><br />");
				redirect();
				
				return;
			}
			
			if( !Location.fromResult(ship).sameSector(0, Location.fromResult(fleetship), 0) || ( fleetship.getInt("owner") != user.getId() ) || (fleet.getInt("id") != fleetship.getInt("fleet")) ) {
				t.setVar("ship.message", "<span style=\"color:red\">Beitritt zur Flotte &quot;"+Common._plaintitle(fleet.getString("name"))+"&quot; nicht m&ouml;glich</span><br />");
			}
			else {
				if( fleetship.getInt("fleet") == 0 ) {
					t.setVar("ship.message", "<span style=\"color:red\">Sie m&uuml;ssen erst eine Flotte erstellen</span><br />");
					redirect();
					return;
				}
			
				db.update("UPDATE ships SET fleet=",fleetship.getInt("fleet")," WHERE id>0 AND id=",ship.getInt("id"));
				t.setVar("ship.message", "<span style=\"color:green\">Flotte &quot;"+Common._plaintitle(fleet.getString("name"))+"&quot; beigetreten</span><br />");
			}
		}
		
		redirect();
	}
	
	/**
	 * Laedt die Schilde des aktuellen Schiffes auf
	 * @urlparam Integer shup Die Menge an Energie, die zum Aufladen der Schilde verwendet werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shupAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("shup");
		int shup = getInteger("shup");
		
		int shieldfactor = 100;
		if( shiptype.getInt("shields") < 1000 ) {
			shieldfactor = 10;
		}

		final int maxshup = (int)Math.ceil((shiptype.getInt("shields") - ship.getInt("shields"))/(double)shieldfactor);
		if( shup > maxshup ) {
			shup = maxshup;
		}
		if( shup > ship.getInt("e") ) {
			shup = ship.getInt("e");
		}

		t.setVar("ship.message", "Schilde +"+(shup*shieldfactor)+"<br />");
		
		int oldshields = ship.getInt("shields");
		ship.put("shields", ship.getInt("shields") + shup*shieldfactor);
		if( ship.getInt("shields") > shiptype.getInt("shields") ) {
			ship.put("shields", shiptype.getInt("shields"));
		}
	
		int olde = ship.getInt("e");
		ship.put("e", ship.getInt("e") - shup);

		db.update("UPDATE ships SET e=",ship.getInt("e"),",shields=",ship.getInt("shields")," WHERE id=",ship.getInt("id")," AND e='",olde,"' AND shields='",oldshields,"'");
	
		Ships.recalculateShipStatus(ship.getInt("id"));
	
		redirect();
	}
	
	/**
	 * Speichert ein neues Schiffsaktionsscript und setzt optional
	 * die aktuellen Ausfuehrungsdaten wieder zurueck
	 * @urlparam String script das neue Schfifsaktionsscript
	 * @urlparam Integer reset Wenn der Wert != 0 ist, dann werden die Ausfuehrungsdaten zurueckgesetzt
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void scriptAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("script");
		parameterNumber("reset");
		
		String script = getString("script");
		int reset = getInteger("reset");
		
		if( !script.trim().equals("") ) {
			String resetsql = "";
			if( reset != 0 ) {
				resetsql = ",scriptexedata=NULL";
			}
	
			db.prepare("UPDATE ships SET script= ? ",resetsql," WHERE id>0 AND id= ? ").update(script, ship.getInt("id"));
		}
		else {
			db.update("UPDATE ships SET script=NULL,scriptexedata=NULL WHERE id>0 AND id='",ship.getInt("id"),"'");		
		}
		
		t.setVar("ship.message", "Script gespeichert<br />");
	
		redirect();
	}
	
	/**
	 * Behandelt ein OnCommunicate-Ereigniss
	 * @urlparam Integer communicate Die ID des Schiffes, mit dem kommuniziert werden soll
	 * @urlparam String execparameter Weitere Ausfuehrungsdaten
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void communicateAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("communicate");
		int communicate = getInteger("communicate");
		
		String[] lock = StringUtils.split(ship.getString("lock"), ':');

		if( (lock.length > 2) && !lock[2].equals(Quests.EVENT_ONCOMMUNICATE) ) {
			redirect();
			
			return;
		}
	
		ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser( ScriptParser.NameSpace.QUEST );
		scriptparser.setShip(ship);
		if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
		}
		
		Quests.currentEventURL.set("&action=communicate&communicate="+communicate);
	
		if( (lock.length > 2) ) {		
			scriptparser.setRegister("LOCKEXEC", "1");
		}
		
		scriptparser.setRegister("TARGETSHIP", Integer.toString(communicate));
	
		parameterString("execparameter");
		String execparameter = getString( "execparameter" );
		if( execparameter.equals("") ) {
			execparameter = "0";	
		}
	
		SQLResultRow targetship = db.first("SELECT x,y,system,oncommunicate FROM ships WHERE id>0 AND id='",communicate,"'");
		if( !Location.fromResult(targetship).sameSector(0, Location.fromResult(ship), 0) ) {
			t.setVar("ship.message", "<span style=\"color:red\">Sie k&ouml;nnen nur mit Schiffen im selben Sektor kommunizieren</span><br />");
			redirect();
			return;
		}
		Quests.executeEvent( scriptparser, targetship.getString("oncommunicate"), user.getId(), execparameter );
		
		redirect();
	}
	
	/**
	 * Behandelt ein OnMove-Ereigniss
	 * @urlparam String execparameter Weitere Ausfuehrungsdaten
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void onmoveAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		String[] lock = StringUtils.split(ship.getString("lock"), ':');
		
		if( (lock.length > 2) && !lock[2].equals(Quests.EVENT_ONMOVE) ) {
			redirect();
			
			return;
		}
	
		ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser( ScriptParser.NameSpace.QUEST );
		scriptparser.setShip(ship);
		if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
		}
	
		Quests.currentEventURL.set("&action=onmove");
	
		if( (lock.length > 2) ) {		
			scriptparser.setRegister("LOCKEXEC", "1");
		}
	
		parameterString("execparameter");
		String execparameter = getString( "execparameter" );
		if( execparameter.equals("") ) {
			execparameter = "0";	
		}
	
		if( ship.getString("onmove").equals("") ) {
			t.setVar("ship.message", "<span style=\"color:red\">Das angegebene Schiff verf&uuml;gt nicht &uuml;ber dieses Ereigniss</span><br />");
			redirect();
			return;	
		}

		Quests.executeEvent( scriptparser, ship.getString("onmove"), user.getId(), execparameter );
	
		redirect();
	}
	
	/**
	 * Behandelt ein OnEnter-Ereignis
	 * @urlparam String execparameter Weitere Ausfuehrungsdaten
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void onenterAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		String[] lock = StringUtils.split(ship.getString("lock"), ':');
		
		if( (lock.length > 2) && !lock[2].equals(Quests.EVENT_ONENTER) ) {
			redirect();
			
			return;
		}
	
		ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser( ScriptParser.NameSpace.QUEST );
		scriptparser.setShip(ship);
		if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
		}
		
		// TODO: migrate to libquests
		
		if( lock.length < 3 ) {
			redirect();
			return;
		}
		
		String usescript = lock[0];
		String rquestid = lock[1].substring(1);	
	
		String usequest = lock[1];
	
		if( !usescript.equals("-1") ) {
			parameterString("execparameter");
			String execparameter = getString( "execparameter" );
			if( execparameter.equals("") ) {
				execparameter = "0";	
			}
		
			Quests.currentEventURL.set("&action=onenter");
			
			SQLQuery runningdata = db.query("SELECT id,execdata FROM quests_running WHERE id='",rquestid,"'");
			Blob execdata = null;
		
			if( runningdata.next() ) {
				try {
					execdata = runningdata.getBlob("execdata");
					if( (execdata != null) && (execdata.length() > 0) ) { 
						scriptparser.setContext(
								ScriptParserContext.fromStream(execdata.getBinaryStream())
						);
					}
				}
				catch( Exception e ) {
					LOG.warn("Setting Script-ExecData failed (Ship: "+ship.getInt("id")+": ",e);
					return;
				}
			}
			else {
				runningdata.free();
				t.setVar("ship.message", "FATAL QUEST ERROR: keine running-data gefunden!<br />");
				redirect();
				return;
			}
		
			String script = db.first("SELECT script FROM scripts WHERE id='",usescript,"'").getString("script");
			scriptparser.setRegister("USER", Integer.toString(user.getId()));
			if( !usequest.equals("") ) {
				scriptparser.setRegister("QUEST", "r"+runningdata.getInt("id"));
			}
			scriptparser.setRegister("SCRIPT", usescript);
			scriptparser.setRegister("SECTOR", Location.fromResult(ship).toString());
			if( (lock.length > 2) ) {		
				scriptparser.setRegister("LOCKEXEC", "1");
			}

			scriptparser.executeScript(db, script, execparameter);
		
			usequest = scriptparser.getRegister("QUEST");
			
			if( !usequest.equals("") ) {
				try {
					scriptparser.getContext().toStream(execdata.setBinaryStream(1));
					db.prepare("UPDATE quests_running SET execdata=? WHERE id=? ")
						.update(execdata, runningdata.getInt("id"));
				}
				catch( Exception e ) {
					LOG.warn("Writing back Script-ExecData failed (Ship: "+ship.getInt("id")+": ",e);
					return;
				}
			}
			
			runningdata.free();
		}	
		else {
			t.setVar("ship.message", "Das angegebene Schiff antwortet auf ihre Funksignale nicht<br />");	
		}
		redirect();
	}
	
	private void createRespawnEntry( int shipid, int respawntime ) {
		Database db = getDatabase();
		
		if( respawntime != 0 ) {
			db.update("UPDATE ships SET respawn='",respawntime,"' WHERE id='",shipid,"'");
		}
		else {
			db.update("UPDATE ships SET respawn=NULL WHERE id='",shipid,"'");
		}
		
		List<String> queryfp = new ArrayList<String>();
		List<String> querylp = new ArrayList<String>();
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE id='",shipid,"'");
			
		SQLQuery afield = db.query("SHOW FIELDS FROM ships");
		while( afield.next() ) {
			queryfp.add("`"+afield.getString("Field")+"`");
			if( afield.getString("Field").equals("id") ) {
				querylp.add("'"+(-shipid)+"'");
			}
			else {
				if( (ship.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
					querylp.add("NULL");
				}
				else {
					querylp.add("'"+db.prepareString(ship.getString(afield.getString("Field")))+"'");
				}
			}
		}
		afield.free();
		
		db.update("INSERT INTO ships (",Common.implode(",",queryfp),") VALUES (",Common.implode(",",querylp),")");
		
		// Moduldaten einfuegen, falls vorhanden
		SQLResultRow shipmodules = db.first("SELECT * FROM ships_modules WHERE id='",shipid,"'");
		if( !shipmodules.isEmpty() ) {
			queryfp.clear();
			querylp.clear();
		
			afield = db.query("SHOW FIELDS FROM ships_modules");
			while( afield.next() ) {	
				queryfp.add("`"+afield.getString("Field")+"`");
				if( afield.getString("Field").equals("id") ) {
					querylp.add("'"+(-shipid)+"'");
				}
				else {
					if( (shipmodules.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
						querylp.add("NULL");
					}
					else {
						querylp.add("'"+db.prepareString(shipmodules.getString(afield.getString("Field")))+"'");
					}
				}
			}
			afield.free();
		
			db.update("INSERT INTO ships_modules (",Common.implode(",",queryfp),") VALUES (",Common.implode(",",querylp),")");
		}
		
		// Offiziere einfuegen, falls vorhanden
		SQLQuery offizier = db.query("SELECT * FROM offiziere WHERE dest='s ",shipid,"'");
		while( offizier.next() ) {
			queryfp.clear();
			querylp.clear();
		
			afield = db.query("SHOW FIELDS FROM offiziere");
			while( afield.next() ) {
				queryfp.add("`"+afield.getString("Field")+"`");
				if( afield.getString("Field").equals("dest") ) {
					querylp.add("'s "+(-shipid)+"'");
				}
				else if( afield.getString("Field").equals("id") ) {
					querylp.add("''");
				}
				else {
					if( (offizier.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
						querylp.add("NULL");
					}
					else {
						querylp.add("'"+db.prepareString(offizier.getString(afield.getString("Field")))+"'");
					}
				}
			}
			afield.free();
		
			db.update("INSERT INTO offiziere (",Common.implode(",",queryfp),") VALUES (",Common.implode(",",querylp),")");
		}
		offizier.free();
		
		// Werfteintrag setzen, falls vorhanden
		SQLResultRow werftentry = db.first("SELECT * FROM werften WHERE shipid='",shipid,"'");
		if( !werftentry.isEmpty() ) {
			queryfp.clear();
			querylp.clear();
	
			afield = db.query("SHOW FIELDS FROM werften");
			while( afield.next() ) {
				queryfp.add("`"+afield.getString("Field")+"`");
				if( afield.getString("Field").equals("shipid") ) {
					querylp.add("'"+(-shipid)+"'");
				}
				else if( afield.getString("Field").equals("id") ) {
					querylp.add("''");
				}
				else {
					if( (werftentry.get(afield.getString("Field")) == null) && afield.getString("Null").equals("YES") ) {
						querylp.add("NULL");
					}
					else {
						querylp.add("'"+db.prepareString(werftentry.getString(afield.getString("Field")))+"'");
					}
				}
			}
			afield.free();
		
			db.update("INSERT INTO werften (",Common.implode(",",queryfp),") VALUES (",Common.implode(",",querylp),")");
		}
	}
	
	private void deleteRespawnEntry( int shipid ) {
		Ships.destroy(-shipid);
	}
	
	/**
	 * Setzt die Respawn-Daten des Schiffes. Wenn bereits ein 
	 * Respawn-Eintrag existiert, wird dieser zurueckgesetzt.
	 * Andernfalls wird ein neuer Respawn-Eintrag erzeugt
	 * @urlparam Integer respawntime Die Zeit in Ticks bis zum Respawn
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void respawnAction() {
		Database db = getDatabase();
		User user = (User)getUser(); 
		TemplateEngine t = getTemplateEngine();
		
		if( user.getAccessLevel() < 20 ) {
			redirect();
			return;
		}
		
		int negdata = db.first("SELECT id FROM ships WHERE id='",(-ship.getInt("id")),"'").getInt("id");
		if( negdata < 0 ) {
			deleteRespawnEntry(ship.getInt("id"));
			
			SQLQuery sid = db.query("SELECT id FROM ships WHERE id>0 AND docked IN ('",ship.getInt("id"),"','l ",ship.getInt("id"),"')");
			while( sid.next() ) {
				deleteRespawnEntry(sid.getInt("id"));
			}
			sid.free();
			
			t.setVar("ship.message", "Die Respawn-Daten wurden gel&ouml;scht<br />");
		}
		else {
			parameterNumber("respawntime");
			int respawntime = getInteger("respawntime");
			
			createRespawnEntry(ship.getInt("id"), respawntime);
			
			SQLQuery sid = db.query("SELECT id FROM ships WHERE id>0 AND docked IN ('",ship.getInt("id"),"','l ",ship.getInt("id"),"')");
			while( sid.next() ) {
				createRespawnEntry(sid.getInt("id"),respawntime);
			}
			sid.free();
			
			t.setVar("ship.message", "Die Respawn-Daten wurden angelegt<br />");
		}
		
		redirect();
	}
	
	/**
	 * Transferiert das Schiff ins System 99
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void inselAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser(); 
		
		if( !user.hasFlag( User.FLAG_NPC_ISLAND ) ) {
			redirect();
			return;	
		}
		
		db.update("UPDATE ships SET x='10',y='10',system='99' WHERE id='",ship.getInt("id"),"'");
		t.setVar("ship.message", "<span style=\"color:green\">Willkommen auf der Insel <img align=\"middle\" src=\""+Configuration.getSetting("SMILIE_PATH")+"/icon_smile.gif\" alt=\":)\" /></span><br />");
		
		redirect();
	}
	
private static final Map<String,String> moduleOutputList = new HashMap<String,String>();
	
	static {
		final String url = Configuration.getSetting("URL");
		
		// Nur Number-Spalten!
		moduleOutputList.put("getRu", "<img align='middle' src='"+Cargo.getResourceImage(Resources.URAN)+"' alt='' />Reaktor ");
		moduleOutputList.put("getRd", "<img align='middle' src='"+Cargo.getResourceImage(Resources.DEUTERIUM)+"' alt='' />Reaktor ");
		moduleOutputList.put("getRa", "<img align='middle' src='"+Cargo.getResourceImage(Resources.ANTIMATERIE)+"' alt='' />Reaktor ");
		moduleOutputList.put("getRm", "<img align='middle' src='"+url+"data/interface/energie.gif' alt='' />Reaktor ");
		moduleOutputList.put("getCargo", "<img align='middle' src='"+url+"data/interface/leer.gif' alt='' />Cargo ");
		moduleOutputList.put("getEps", "<img align='middle' src='"+url+"data/interface/energie.gif' alt='' />Energiespeicher ");
		moduleOutputList.put("getHull", "<img align='middle' src='"+url+"data/interface/schiffe/panzerplatte.png' alt='' />H&uuml;lle ");
		moduleOutputList.put("getShields", "Shields ");
		moduleOutputList.put("getCost", "Flugkosten ");
		moduleOutputList.put("getHeat", "&Uuml;berhitzung ");
		moduleOutputList.put("getPanzerung", "<img align='middle' src='"+url+"data/interface/schiffe/panzerplatte.png' alt='' />Panzerung ");
		moduleOutputList.put("getTorpedoDef", "Torpedoabwehr ");
		moduleOutputList.put("getCrew", "<img align='middle' src='"+url+"data/interface/besatzung.gif' alt='' />Crew ");
		moduleOutputList.put("getHydro", "<img align='middle' src='"+Cargo.getResourceImage(Resources.NAHRUNG)+"' alt='' />Produktion ");
		moduleOutputList.put("getSensorRange", "<img align='middle' src='"+url+"data/interface/schiffe/sensorrange.png' alt='' />Sensorreichweite ");
		moduleOutputList.put("getDeutFactor", "Tanker: <img align='middle' src='"+Cargo.getResourceImage(Resources.DEUTERIUM)+"' alt='' />");
		moduleOutputList.put("getReCost", "Wartungskosten ");
		moduleOutputList.put("getADocks", "Externe Docks ");
		moduleOutputList.put("getJDocks", "J&auml;gerdocks ");
		moduleOutputList.put("getAblativeArmor", "Ablative Panzerung ");
	}
	
	/**
	 * Zeigt die Schiffsansicht an
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		ship = db.first("SELECT * FROM ships WHERE id>0 AND owner='",user.getId(),"' AND id=",ship.getInt("id"));
		if( ship.isEmpty() ) {
			addError("Das Schiff existiert nicht mehr oder geh&ouml;rt nicht mehr ihnen");
			return;
		}
		
		shiptype = ShipTypes.getShipType(ship);
		
		ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser( ScriptParser.NameSpace.QUEST );
		if( ship.isEmpty() ) {
			if( (scriptparser != null) && (scriptparser.getContext().getOutput().length() != 0) ) {
				t.setVar("ship.scriptparseroutput",
						scriptparser.getContext().getOutput().replace("{{var.sessid}}", getString("sess")) );
			}
				
			return;
		}

		if( ship.getInt("battle") > 0 ) {
			if( (scriptparser != null) && (scriptparser.getContext().getOutput().length() > 0) ) {
				t.setVar("ship.scriptparseroutput",
						scriptparser.getContext().getOutput().replace("{{var.sessid}}", getString("sess")) );
			}
		
			addError("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl("default", "module", "angriff", "battle", ship.getString("battle"), "ship", ship.getInt("id")) );
			return;
		}
		
		offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
		
		StringBuilder tooltiptext = new StringBuilder(100);
		tooltiptext.append(Common.tableBegin(340, "center").replace('"', '\'') );
		tooltiptext.append("<iframe src='"+Common.buildUrl("default", "module", "impobjects", "system", ship.getInt("system"))+"' name='sector' width='320' height='300' scrolling='auto' marginheight='0' marginwidth='0' frameborder='0'>Ihr Browser unterst&uuml;tzt keine iframes</iframe>");
		tooltiptext.append(Common.tableEnd().replace('"', '\'') );
		String tooltiptextStr = StringEscapeUtils.escapeJavaScript(tooltiptext.toString().replace(">", "&gt;").replace("<", "&lt;"));

		t.setVar(	"ship.showui",			1,
					"ship.id",				ship.getInt("id"),
					"ship.name",			Common._plaintitle(ship.getString("name")),
					"ship.location",		Ships.getLocationText(ship, false),
					"ship.type",			ship.getInt("type"),
					"shiptype.picture",		shiptype.getString("picture"),
					"shiptype.name",		shiptype.getString("nickname"),
					"ship.hull.color",		genSubColor(ship.getInt("hull"), shiptype.getInt("hull")),
					"ship.hull",			Common.ln(ship.getInt("hull")),
					"shiptype.hull",		Common.ln(shiptype.getInt("hull")),
					"ship.shields.color",	genSubColor(ship.getInt("shields"), shiptype.getInt("shields")),
					"ship.shields",			Common.ln(ship.getInt("shields")),
					"shiptype.shields",		Common.ln(shiptype.getInt("shields")),
					"shiptype.cost",		shiptype.getInt("cost"),
					"ship.engine.color",	genSubColor(ship.getInt("engine"), 100),
					"ship.engine",			ship.getInt("engine"),
					"shiptype.weapon",		shiptype.getString("weapons").indexOf('=') > -1,
					"ship.weapons.color",	genSubColor(ship.getInt("weapons"), 100),
					"ship.weapons",			ship.getInt("weapons"),
					"ship.comm.color",		genSubColor(ship.getInt("comm"), 100),
					"ship.comm",			ship.getInt("comm"),
					"ship.sensors.color",	genSubColor(ship.getInt("sensors"), 100),
					"ship.sensors",			ship.getInt("sensors"),
					"shiptype.crew",		Common.ln(shiptype.getInt("crew")),
					"ship.crew",			Common.ln(ship.getInt("crew")),
					"ship.crew.color",		genSubColor(ship.getInt("crew"), shiptype.getInt("crew")),
					"ship.e",				Common.ln(ship.getInt("e")),
					"shiptype.eps",			Common.ln(shiptype.getInt("eps")),
					"ship.e.color",			genSubColor(ship.getInt("e"), shiptype.getInt("eps")),
					"ship.s",				ship.getInt("s"),
					"ship.fleet",			ship.getInt("fleet"),
					"ship.lock",			ship.getString("lock"),
					"shiptype.werft",		shiptype.getInt("werft"),
					"tooltip.systeminfo",	tooltiptextStr,
					"ship.showalarm",		!noob && (shiptype.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) && shiptype.getInt("military") != 0 );
		
		if( ship.getInt("s") >= 100 ) {
			t.setVar("ship.s.color", "red");	
		}
		else if( ship.getInt("s") > 40 ) {
			t.setVar("ship.s.color", "yellow");	
		}
		else {
			t.setVar("ship.s.color", "white");	
		}
		
		if( offizier != null ) {
			t.setBlock("_SCHIFF", "offiziere.listitem", "offiziere.list");
			
			List<Offizier> offiziere = getContext().query("from Offizier where dest='s "+ship.getInt("id")+"'", Offizier.class);
			for( Offizier offi : offiziere ) {
				t.setVar(	"offizier.id",		offi.getID(),
							"offizier.name",	Common._plaintitle(offi.getName()),
							"offizier.picture",	offi.getPicture(),
							"offizier.rang",	offi.getRang() );
									
				t.parse("offiziere.list", "offiziere.listitem", true);
			}
		}
		
		shiptype.put("sensorrange", Math.round(shiptype.getInt("sensorrange")*(ship.getInt("sensors")/100f)));

		if( shiptype.getInt("sensorrange") < 0 ) {
			shiptype.put("sensorrange", 0);
		}
		
		// Flottenlink
		if( ship.getInt("fleet") != 0 ) {
			SQLResultRow fleet = db.first("SELECT name FROM ship_fleets WHERE id=",ship.getInt("fleet"));
			if( !fleet.isEmpty() ) {
				t.setVar("fleet.name", Common._plaintitle(fleet.getString("name")) );
			} 
			else {
				t.setVar("ship.fleet", 0);
				db.update("UPDATE ships SET fleet=null WHERE id>0 AND id=",ship.getInt("id"));
			}
		}
		
		// Aktion: Schnelllink GTU-Handelsdepot
		SQLResultRow handel = db.first("SELECT id,name FROM ships WHERE id>0 AND system=",ship.getInt("system")," AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," AND LOCATE('tradepost',status)");
		if( !handel.isEmpty() ) {
			t.setVar(	"sector.handel",		handel.getInt("id"),
						"sector.handel.name",	Common._plaintitle(handel.getString("name") ));
		}
		
		// Tooltip: Schiffscripte
		if( user.hasFlag( User.FLAG_EXEC_NOTES ) ) {
		
			String script = StringUtils.replace(ship.getString("script"),"\r\n", "\\n");
			script = StringUtils.replace(script,"\n", "\\n");
			script = StringUtils.replace(script,"\"", "\\\"");
			
			t.setVar(	"tooltip.execnotes",		1,
						"tooltip.execnotes.begin",	StringUtils.replaceChars(Common.tableBegin(400, "center"),'"', '\''),
						"tooltip.execnotes.end",	StringUtils.replaceChars(Common.tableEnd(),'"', '\''),
						"tooltip.execnotes.script",	script );
		}
		
		// Tooltip: Schiffsstatusfeld ;; Button: respawn
		if( user.getAccessLevel() > 19 ) {
			tooltiptext = new StringBuilder(100);
			tooltiptext.append(Common.tableBegin(200, "left").replace('"', '\''));
			tooltiptext.append("<span style='text-decoration:underline'>Schiffsstatus:</span><br />"+ship.getString("status").trim().replace(" ", "<br />"));
			if( !ship.getString("lock").equals("") ) {
				tooltiptext.append("<br /><span style='text-decoration:underline'>Lock:</span><br />"+ship.getString("lock")+"<br />");
			}
			tooltiptext.append(Common.tableEnd().replace( '"', '\''));
			String tooltipStr = StringEscapeUtils.escapeJavaScript(tooltiptext.toString().replace(">", "&gt;").replace("<", "&lt;"));

			t.setVar("tooltip.admin", tooltipStr );
				
			t.setVar(	"tooltip.respawn.begin",	Common.tableBegin(200,"center").replace( '"', '\''),
						"tooltip.respawn.end",		Common.tableEnd().replace( '"', '\'' ),
						"ship.respawn",				ship.getInt("respawn") );

			SQLResultRow rentry = db.first("SELECT id FROM ships WHERE id='-",ship.getInt("id"),"'");
			if( !rentry.isEmpty() ) {
				t.setVar(	"ship.show.respawn",	1,
							"ship.hasrespawn",		1);	
			}
			else {
				t.setVar( "ship.show.respawn", 1 );	
			}
		}
		
		if( user.hasFlag( User.FLAG_NPC_ISLAND ) ) {
			t.setVar("ship.npcislandlink", 1);
		}
		
		// Tooltip: Module
		{
		Ship ship = Ships.getAsObject(this.ship);
		ShipTypeData shiptype = ship.getTypeData();
		final Ship.ModuleEntry[] modulelist = ship.getModules();
		if( (modulelist.length > 0) && shiptype.getTypeModules().length() > 0 ) {
			List<String> tooltiplines = new ArrayList<String>();
			tooltiplines.add("<span style='text-decoration:underline'>Module:</span><br />");
			
			ShipTypeData type = Ship.getShipType( ship.getType() );
			ShipTypeData basetype = type;
			
			Map<Integer,String[]> slotlist = new HashMap<Integer,String[]>();
			String[] tmpslotlist = StringUtils.split(type.getTypeModules(),';');
			for( int i=0; i < tmpslotlist.length; i++ ) {
				String[] aslot = StringUtils.split(tmpslotlist[i], ':');
				slotlist.put(new Integer(aslot[0]), aslot);
			}
			
			List<Module> moduleObjList = new ArrayList<Module>();
			boolean itemmodules = false;
			
			for( int i=0; i < modulelist.length; i++ ) {
				Ship.ModuleEntry module = modulelist[i];
				if( module.moduleType != 0 ) {
					Module moduleobj = Modules.getShipModule( module );
					if( (module.slot > 0) && (slotlist.get(module.slot).length > 2) ) {
						moduleobj.setSlotData(slotlist.get(module.slot)[2]);
					}
					
					moduleObjList.add(moduleobj);
					if( moduleobj instanceof ModuleItemModule ) {
						Cargo acargo = new Cargo();
						acargo.addResource( ((ModuleItemModule)moduleobj).getItemID(), 1 );
						ResourceEntry res = acargo.getResourceList().iterator().next();
						tooltiplines.add("<span class='nobr'><img style='vertical-align:middle' src='"+res.getImage()+"' alt='' />"+res.getPlainName()+"</span><br />");
						itemmodules = true;
					}	
				}	
			}
			
			if( itemmodules ) {
				tooltiplines.add("<hr style='height:1px; border:0px; background-color:#606060; color:#606060' />");
			}
			
			for( int i=0; i < moduleObjList.size(); i++ ) {
				type = moduleObjList.get(i).modifyStats( type, moduleObjList );
			}
			
			for( String method : moduleOutputList.keySet() ) {
				try {
					Method m = type.getClass().getMethod(method);
					m.setAccessible(true);
					Number value = (Number)m.invoke(type);
					
					m = basetype.getClass().getMethod(method);
					m.setAccessible(true);
					Number baseValue = (Number)m.invoke(basetype);
					
					// Alles was in moduleOutputList sitzt, muss in der DB durch einen von Number abgeleiteten Typ definiert sein!
					if( !value.equals(baseValue) ) {
						String text = null;
						if( baseValue.doubleValue() < value.doubleValue() ) {
							text = moduleOutputList.get(method)+Common.ln(value)+" (<span class='nobr' style='color:green'>+";	
						}
						else {
							text = moduleOutputList.get(method)+Common.ln(value)+" (<span class='nobr' style='color:red'>";	
						}
						text += Common.ln(value.doubleValue() - baseValue.doubleValue())+"</span>)<br />";
						tooltiplines.add(text);
					}
				}
				catch( InvocationTargetException e ) {
					LOG.error("Fehler beim Aufruf der Property "+method,e);
				}
				catch( NoSuchMethodException e ) {
					LOG.error("Ungueltige Property "+method,e);
				}
				catch( IllegalAccessException e ) {
					LOG.error("Ungueltige Property "+method,e);
				}
			}
			
			// Weapons
			Map<String,String> weaponlist = Weapons.parseWeaponList( type.getWeapons() );
			Map<String,String> defweaponlist = Weapons.parseWeaponList( basetype.getWeapons() );
			
			for( Map.Entry<String, String> entry: weaponlist.entrySet() ) {
				String aweapon = entry.getKey();
				int aweaponcount = Integer.parseInt(entry.getValue());
				if( !defweaponlist.containsKey(aweapon) ) {
					tooltiplines.add("<span class='nobr' style='color:green'>+"+aweaponcount+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
				} else {
					String defweapon = defweaponlist.get(aweapon);
					if( Integer.parseInt(defweapon) < aweaponcount ) {
						tooltiplines.add("<span class='nobr' style='color:green'>+"+(aweaponcount - Integer.parseInt(defweapon))+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
					}	
					else if( Integer.parseInt(defweapon) > aweaponcount ) {
						tooltiplines.add("<span class='nobr' style='color:red'>"+(aweaponcount - Integer.parseInt(defweapon))+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
					}
				}
			}
				
			for( Map.Entry<String, String> entry: defweaponlist.entrySet() ) {
				String aweapon = entry.getKey();
				if( !weaponlist.containsKey(aweapon) ) {
					int weaponint = Integer.parseInt(entry.getValue());
					tooltiplines.add("<span class='nobr' style='color:red'>-"+weaponint+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
				}
			}
				
			// MaxHeat
			Map<String,String> heatlist = Weapons.parseWeaponList( type.getMaxHeat() );
			Map<String,String> defheatlist = Weapons.parseWeaponList( basetype.getMaxHeat() );
			
			for( Map.Entry<String, String> entry: heatlist.entrySet() ) {
				int aweaponheat = Integer.parseInt(entry.getValue());
				String aweapon = entry.getKey();
				
				if( !defheatlist.containsKey(aweapon) ) {
					tooltiplines.add("<span class='nobr' style='color:green'>+"+aweaponheat+" "+Weapons.get().weapon(aweapon).getName()+" Max-Hitze</span><br />");
				}
				else {
					int defweaponheat = Integer.parseInt(defheatlist.get(aweapon));
					if( defweaponheat < aweaponheat ) {
						tooltiplines.add("<span class='nobr' style='color:green'>+"+(aweaponheat - defweaponheat)+" "+Weapons.get().weapon(aweapon).getName()+" Max-Hitze</span><br />");
					}	
					else if( defweaponheat > aweaponheat ) {
						tooltiplines.add("<span class='nobr' style='color:red'>"+(aweaponheat - defweaponheat)+" "+Weapons.get().weapon(aweapon).getName()+" Max-Hitze</span><br />");
					}
				}
			}
		
			// Flags
			String[] newflaglist = StringUtils.split(type.getFlags(),' ');
			for( int i=0; i < newflaglist.length; i++ ) {
				if( newflaglist[i].equals("") ) {
					continue;	
				}	
				
				if( !basetype.hasFlag(newflaglist[i]) ) {
					tooltiplines.add("<span class='nobr' style='color:green'>"+ShipTypes.getShipTypeFlagName(newflaglist[i])+"</span><br />");
				}
			}

			tooltiptext = new StringBuilder(100);
			tooltiptext.append(Common.tableBegin(400,"left").replace('"', '\''));
			if( tooltiplines.size() > 15 ) {
				tooltiptext.append("<div style='height:300px; overflow:auto'>");
			}
			tooltiptext.append(Common.implode("", tooltiplines ));
			if( tooltiplines.size() > 15 ) {
				tooltiptext.append("</div>");
			}
			tooltiptext.append(Common.tableEnd().replace('"', '\'') );
			String tooltipStr = StringEscapeUtils.escapeJavaScript(tooltiptext.toString().replace(">", "&gt;").replace("<", "&lt;"));
					
			if( tooltiplines.size() > 15 ) {
				t.setVar("tooltip.moduleext", tooltipStr);
			}
			else {
				t.setVar("tooltip.module", tooltipStr);
			}
		}
		}
		
		// Schilde aufladen
		if( shiptype.getInt("shields") > 0 && (ship.getInt("shields") < shiptype.getInt("shields")) ) {
			int shieldfactor = 100;
			if( shiptype.getInt("shields") < 1000 ) {
				shieldfactor = 10;
			}
			
			t.setVar("ship.shields.reloade", Common.ln((int)Math.ceil((shiptype.getInt("shields") - ship.getInt("shields"))/(double)shieldfactor)));
		}
		
		String[] alarms = {"yellow","red"};
		String[] alarmn = {"gelb","rot"};
	
		// Alarmstufe aendern
		t.setBlock("_SCHIFF", "ship.alarms.listitem", "ship.alarms.list");
		for( int a = 0; a < alarms.length; a++ ) {
			t.setVar(	"alarm.id",			a,
						"alarm.name", 		alarmn[a],
						"alarm.selected",	(ship.getInt("alarm") == a) );
			t.parse("ship.alarms.list", "ship.alarms.listitem", true);
		}

		if( (ship.getString("status").indexOf("noconsign") == -1) && ship.getString("lock").equals("") ) {
			t.setVar("ship.consignable", 1);
		}
		
		//------------------------------------------------------------
		//
		// Die Plugins
		//
		//------------------------------------------------------------

		Parameters caller = new Parameters();
		caller.controller = this;
		caller.ship = ship;
		caller.shiptype = shiptype;
		caller.offizier = offizier;
		
		if( pluginMapper.containsKey("navigation") ) {
			SchiffPlugin plugin = pluginMapper.get("navigation");
			caller.pluginId = "navigation";
			caller.target = "plugin.navigation";
			plugin.output(caller);
			
			pluginMapper.remove("navigation");
		}
		
		if( pluginMapper.containsKey("cargo") ) {
			SchiffPlugin plugin = pluginMapper.get("cargo");
			caller.pluginId = "cargo";
			caller.target = "plugin.cargo";
			plugin.output(caller);
			
			pluginMapper.remove("cargo");
		}
		
		/* 
			Ok...das ist kein Plugin, es gehoert aber trotzdem zwischen die ganzen Plugins (Questoutput) 
		*/
	
		if( (scriptparser != null) && (scriptparser.getContext().getOutput().length() > 0) ) {
			t.setVar("ship.scriptparseroutput",
					scriptparser.getContext().getOutput().replace("{{var.sessid}}", getString("sess")));
		}
	
		caller.target = "plugin.output";
		t.setBlock("_SCHIFF","plugins.listitem","plugins.list");
		
		// Und nun weiter mit den Plugins
		for( String pluginName : pluginMapper.keySet() ) {		
			SchiffPlugin plugin = pluginMapper.get(pluginName);
		
			//Plugin-ID
			caller.pluginId = pluginName;
	
			//Aufruf der entsprechenden Funktion
			plugin.output(caller);
	
			t.parse("plugins.list", "plugins.listitem", true);
		}		
	}
}
