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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.Blob;

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
import net.driftingsouls.ds2.server.modules.schiffplugins.Parameters;
import net.driftingsouls.ds2.server.modules.schiffplugins.SchiffPlugin;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.ships.ShipClasses;
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
public class SchiffController extends DSGenerator implements Loggable {
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
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.set_var( "user.tooltips", user.getUserValue("TBLORDER/schiff/tooltips") );
		
		int shipid = getInteger("ship");
		
		ship = db.first("SELECT * FROM ships WHERE id>0 AND owner='",user.getID(),"' AND id=",shipid);
		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht", Common.buildUrl(getContext(),"default", "module", "schiffe") );
			return false;
		}

		if( ship.getInt("battle") > 0 ) {
			addError("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl(getContext(), "default", "module", "angriff", "battle", ship.getInt("battle"), "ship", shipid) );
			return false;
		}


		shiptype = Ships.getShipType(ship, true);

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
		
		if( !shiptype.getString("werft").equals("") ) {
			pluginMapper.put("werft", getPluginByName("WerftDefault"));
		}
		
		if( Ships.hasShipTypeFlag(shiptype, Ships.SF_JUMPDRIVE_SHIVAN) ) {
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
		
		return true;	
	}
	
	/**
	 * Wechselt die Alarmstufe des Schiffes
	 * @urlparam Integer alarm Die neue Alarmstufe
	 *
	 */
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
			
			getTemplateEngine().set_var("ship.message", "Alarmstufe erfolgreich ge&auml;ndert<br />");
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
	public void consignAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterNumber("newowner");
		int newownerID = getInteger("newowner");
		
		User newowner = createUserObject(newownerID);
		
		parameterNumber("conf");
		int conf = getInteger("conf");
		
		if( conf == 0 ) {
			String text = "<span style=\"color:white\">Wollen sie das Schiff "+Common._plaintitle(ship.getString("name"))+" ("+ship.getInt("id")+") wirklich an "+newowner.getProfileLink()+" &uuml;bergeben?</span><br />";
			text += "<a class=\"ok\" href=\""+Common.buildUrl(getContext(), "consign", "ship", ship.getInt("id"), "conf" , 1, "newowner" , newowner.getID())+"\">&Uuml;bergeben</a></span><br />";
			t.set_var( "ship.message", text );
			
			redirect();
			return;
		}
		
		int fleet = ship.getInt("fleet");
		
		boolean result = Ships.consign(user, ship, newowner, false );
			
		if( result ) {
			t.set_var("ship.message", Ships.MESSAGE.getMessage());
					
			redirect();
		}
		else {
			String msg = "Ich habe dir die "+ship.getString("name")+" ("+ship.getInt("id")+"), ein Schiff der "+shiptype.getString("nickname")+"-Klasse, &uuml;bergeben\nSie steht bei "+ship.getInt("system")+":"+ship.getInt("x")+"/"+ship.getInt("y");
			PM.send(getContext(), user.getID(), newowner.getID(), "Schiff &uuml;bergeben", msg);
		
			String consMessage = Ships.MESSAGE.getMessage();
			t.set_var("ship.message", (!consMessage.equals("") ? consMessage+"<br />" : "")+"<span style=\"color:green\">Das Schiff wurde erfolgreich an "+newowner.getProfileLink()+" &uuml;bergeben</span><br />");
			
			if( fleet != 0 ) {
				int fleetcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND fleet="+fleet).getInt("count");
			
				if( fleetcount < 3 ) {
					db.update("UPDATE ships SET fleet=0 WHERE id>0 AND fleet="+fleet);
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
	public void destroyAction() {
		TemplateEngine t = getTemplateEngine();

		if( !ship.getString("lock").equals("") ) {
			t.set_var("ship.message", "<span style=\"color:red\">Dieses Schiff kann sich nicht selbstzerst&ouml;ren, da es in ein Quest eingebunden ist</span><br />");
			redirect();
			return;
		}
	
		parameterNumber("conf");
		int conf = getInteger("conf");
	
		if( conf == 0 ) {
			String text = "<span style=\"color:white\">Wollen sie Selbstzerst&ouml;rung des Schiffes "+Common._plaintitle(ship.getString("name"))+" ("+ship.getInt("id")+") wirklich ausf&uuml;hren?</span><br />\n";
			text += "<a class=\"error\" href=\""+Common.buildUrl(getContext(), "destroy", "ship", ship.getInt("id"), "conf", 1)+"\">Selbstzerst&ouml;rung</a></span><br />";
			t.set_var("ship.message", text);
			
			redirect();
			return;
		}
	
		Ships.destroy( ship.getInt("id") );

		t.set_var("ship.message", "<span style=\"color:white\">Das Schiff hat sich selbstzerst&ouml;rt</span><br />");
		return;
	}
	
	/**
	 * Springt durch den angegebenen Sprungpunkt
	 * @urlparam Integer knode Die ID des Sprungpunkts
	 *
	 */
	public void jumpAction() {
		TemplateEngine t = getTemplateEngine();
		
		if( (shiptype.getInt("cost") == 0) || (ship.getInt("engine") == 0) ) {
			redirect();
			return;
		}
		
		parameterNumber("knode");
		int node = getInteger("knode");
		
		if( node != 0 ) {
			Ships.jump(ship.getInt("id"), node, false);
			t.set_var("ship.message", Ships.MESSAGE.getMessage());
		}

		redirect();
	}
	
	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt
	 * @urlparam Integer knode Die ID des Schiffes mit dem Sprungpunkt
	 *
	 */
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
			t.set_var("ship.message", Ships.MESSAGE.getMessage());
		}

		redirect();
	}
	
	/**
	 * Benennt das Schiff um
	 * @urlparam String newname Der neue Name des Schiffes
	 *
	 */
	public void renameAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("newname");
		String newname = getString("newname");
		
		db.prepare("UPDATE ships SET name= ? WHERE id= ?").update(newname, ship.getInt("id"));
		t.set_var("ship.message", "Name zu "+Common._plaintitle(newname)+" ge&auml;ndert<br />");
		ship.put("name", newname);
	
		redirect();
	}
	
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
		t.set_var("ship.message", pluginMapper.get(plugin).action(caller));
		
		parseSubParameter("");
		
		redirect();
	}
	
	public void landAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterString("shiplist");
		String shipIdList = getString("shiplist");
		
		if( shipIdList.equals("") ) {
			t.set_var("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Ships.dock(Ships.DockMode.LAND, user.getID(), ship.getInt("id"), shiplist);
		t.set_var("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	public void startAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterString("shiplist");
		String shipIdList = getString("shiplist");
		
		if( shipIdList.equals("") ) {
			t.set_var("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Ships.dock(Ships.DockMode.START, user.getID(), ship.getInt("id"), shiplist);
		t.set_var("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	public void aufladenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterString("tar");
		String shipIdList = getString("tar");
		
		if( shipIdList.equals("") ) {
			t.set_var("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Ships.dock(Ships.DockMode.DOCK, user.getID(), ship.getInt("id"), shiplist);
		t.set_var("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	public void abladenAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterString("tar");
		String shipIdList = getString("tar");
		
		if( shipIdList.equals("") ) {
			t.set_var("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shiplist = Common.explodeToInt("|",shipIdList);
		
		Ships.dock(Ships.DockMode.UNDOCK, user.getID(), ship.getInt("id"), shiplist);
		t.set_var("ship.message", Ships.MESSAGE.getMessage());

		redirect();
	}
	
	public void joinAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterNumber("join");
		int join = getInteger("join");
		
		SQLResultRow fleetship = db.first("SELECT name,x,y,system,owner,fleet,`lock` FROM ships WHERE id>0 AND id=",join);

		// Austreten
		if( (join == 0) && ship.getString("lock").equals("") ) {
			Ships.removeFromFleet(ship);
			ship.put("fleet", 0);
			
			t.set_var("ship.message", "<span style=\"color:green\">"+Ships.MESSAGE.getMessage()+"</span><br />");
		} 
		else if( join == 0 ) {
			t.set_var("ship.message", "<span style=\"color:red\">Dieses Schiff kann nicht aus der Flotte austreten, da diese in ein Quest eingebunden ist</span><br />");		
		}
		// Beitreten
		else {
			SQLResultRow fleet = db.first("SELECT id,name FROM ship_fleets WHERE id='",fleetship.getInt("fleet"),"'");
		
			if( !fleetship.getString("lock").equals("") || !ship.getString("lock").equals("") ) {
				t.set_var("ship.message", "<span style=\"color:red\">Sie k&oumlnnen der Flotte nicht beitreten, solange entweder das Schiff oder die Flotte in ein Quest eingebunden ist</span><br />");
				redirect();
				
				return;
			}
			
			if( !Location.fromResult(ship).sameSector(0, Location.fromResult(fleetship), 0) || ( fleetship.getInt("owner") != user.getID() ) || (fleet.getInt("id") != fleetship.getInt("fleet")) ) {
				t.set_var("ship.message", "<span style=\"color:red\">Beitritt zur Flotte &quot;"+Common._plaintitle(fleet.getString("name"))+"&quot; nicht m&ouml;glich</span><br />");
			}
			else {
				if( fleetship.getInt("fleet") == 0 ) {
					t.set_var("ship.message", "<span style=\"color:red\">Sie m&uuml;ssen erst eine Flotte erstellen</span><br />");
					redirect();
					return;
				}
			
				db.update("UPDATE ships SET fleet=",fleetship.getInt("fleet")," WHERE id>0 AND id=",ship.getInt("id"));
				t.set_var("ship.message", "<span style=\"color:green\">Flotte &quot;"+Common._plaintitle(fleet.getString("name"))+"&quot; beigetreten</span><br />");
			}
		}
		
		redirect();
	}
	
	public void shupAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("shup");
		int shup = getInteger("shup");
		
		int shieldfactor = 100;
		if( shiptype.getInt("shields") < 1000 ) {
			shieldfactor = 10;
		}

		if( shup > (shiptype.getInt("shields") - ship.getInt("shields"))/shieldfactor ) {
			shup = (shiptype.getInt("shields") - ship.getInt("shields"))/shieldfactor;
		}
		if( shup > ship.getInt("e") ) {
			shup = ship.getInt("e");
		}

		t.set_var("ship.message", "Schilde +"+(shup*shieldfactor)+"<br />");
		
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
		
		t.set_var("ship.message", "Script gespeichert<br />");
	
		redirect();
	}
	
	public void communicateAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
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
	
		parameterString("execparameter");
		String execparameter = getString( "execparameter" );
		if( execparameter.equals("") ) {
			execparameter = "0";	
		}
	
		SQLResultRow targetship = db.first("SELECT x,y,system,oncommunicate FROM ships WHERE id>0 AND id='",communicate,"'");
		if( !Location.fromResult(targetship).sameSector(0, Location.fromResult(ship), 0) ) {
			t.set_var("ship.message", "<span style=\"color:red\">Sie k&ouml;nnen nur mit Schiffen im selben Sektor kommunizieren</span><br />");
			redirect();
			return;
		}
		Quests.executeEvent( scriptparser, targetship.getString("oncommunicate"), user.getID(), execparameter );
		
		redirect();
	}
	
	public void onmoveAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
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
			t.set_var("ship.message", "<span style=\"color:red\">Das angegebene Schiff verf&uuml;gt nicht &uuml;ber dieses Ereigniss</span><br />");
			redirect();
			return;	
		}

		Quests.executeEvent( scriptparser, ship.getString("onmove"), user.getID(), execparameter );
	
		redirect();
	}
	
	public void onenterAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
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
					scriptparser.setExecutionData(execdata.getBinaryStream() );
				}
				catch( Exception e ) {
					LOG.warn("Setting Script-ExecData failed (Ship: "+ship.getInt("id")+": ",e);
					return;
				}
			}
			else {
				runningdata.free();
				t.set_var("ship.message", "FATAL QUEST ERROR: keine running-data gefunden!<br />");
				redirect();
				return;
			}
		
			String script = db.first("SELECT script FROM scripts WHERE id='",usescript,"'").getString("script");
			scriptparser.setRegister("USER", Integer.toString(user.getID()));
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
					execdata.truncate(0);
					scriptparser.writeExecutionData(execdata.setBinaryStream(0));	
				}
				catch( Exception e ) {
					LOG.warn("Writing back Script-ExecData failed (Ship: "+ship.getInt("id")+": ",e);
					return;
				}
			}
			
			runningdata.free();
		}	
		else {
			t.set_var("ship.message", "Das angegebene Schiff antwortet auf ihre Funksignale nicht<br />");	
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
	
	public void respawnAction() {
		Database db = getDatabase();
		User user = getUser(); 
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
			
			t.set_var("ship.message", "Die Respawn-Daten wurden gel&ouml;scht<br />");
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
			
			t.set_var("ship.message", "Die Respawn-Daten wurden angelegt<br />");
		}
		
		redirect();
	}
	
	public void inselAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser(); 
		
		if( !user.hasFlag( User.FLAG_NPC_ISLAND ) ) {
			redirect();
			return;	
		}
		
		db.update("UPDATE ships SET x='10',y='10',system='99' WHERE id='",ship.getInt("id"),"'");
		t.set_var("ship.message", "<span style=\"color:green\">Willkommen auf der Insel <img align=\"middle\" src=\""+Configuration.getSetting("SMILIE_PATH")+"/icon_smile.gif\" alt=\":)\" /></span><br />");
		
		redirect();
	}
	
	private static final Map<String,String> moduleOutputList = new HashMap<String,String>();
	
	static {
		final String url = Configuration.getSetting("URL");
		
		// Nur Number-Spalten!
		moduleOutputList.put("ru", "<img align='middle' src='"+Cargo.getResourceImage(Resources.URAN)+"' alt='' />Reaktor ");
		moduleOutputList.put("rd", "<img align='middle' src='"+Cargo.getResourceImage(Resources.DEUTERIUM)+"' alt='' />Reaktor ");
		moduleOutputList.put("ra", "<img align='middle' src='"+Cargo.getResourceImage(Resources.ANTIMATERIE)+"' alt='' />Reaktor ");
		moduleOutputList.put("rm", "<img align='middle' src='"+url+"data/interface/energie.gif' alt='' />Reaktor ");
		moduleOutputList.put("cargo", "<img align='middle' src='"+url+"data/interface/leer.gif' alt='' />Cargo ");
		moduleOutputList.put("eps", "<img align='middle' src='"+url+"data/interface/energie.gif' alt='' />Energiespeicher ");
		moduleOutputList.put("hull", "<img align='middle' src='"+url+"data/interface/schiffe/panzerplatte.png' alt='' />H&uuml;lle ");
		moduleOutputList.put("shields", "Shields ");
		moduleOutputList.put("cost", "Flugkosten ");
		moduleOutputList.put("heat", "&Uuml;berhitzung ");
		moduleOutputList.put("panzerung", "<img align='middle' src='"+url+"data/interface/schiffe/panzerplatte.png' alt='' />Panzerung ");
		moduleOutputList.put("torpedodef", "Torpedoabwehr ");
		moduleOutputList.put("crew", "<img align='middle' src='"+url+"data/interface/besatzung.gif' alt='' />Crew ");
		moduleOutputList.put("hydro", "<img align='middle' src='"+Cargo.getResourceImage(Resources.NAHRUNG)+"' alt='' />Produktion ");
		moduleOutputList.put("sensorrange", "<img align='middle' src='"+url+"data/interface/schiffe/sensorrange.png' alt='' />Sensorreichweite ");
		moduleOutputList.put("deutfactor", "Tanker: <img align='middle' src='"+Cargo.getResourceImage(Resources.DEUTERIUM)+"' alt='' />");
		moduleOutputList.put("recost", "Wartungskosten ");
		moduleOutputList.put("adocks", "Externe Docks ");
		moduleOutputList.put("jdocks", "J&auml;gerdocks ");
	}
	
	/**
	 * Zeigt die Schiffsansicht an
	 */
	@Override
	public void defaultAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		ship = db.first("SELECT * FROM ships WHERE id>0 AND owner='",user.getID(),"' AND id=",ship.getInt("id"));
		shiptype = Ships.getShipType(ship, true);
		
		ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser( ScriptParser.NameSpace.QUEST );
		if( ship.isEmpty() ) {
			if( (scriptparser != null) && !scriptparser.getOutput().equals("") ) {
				t.set_var("ship.scriptparseroutput",scriptparser.getOutput().replace("{{var.sessid}}", getString("sess")) );
			}
				
			return;
		}

		if( ship.getInt("battle") > 0 ) {
			if( (scriptparser != null) && !scriptparser.getOutput().equals("") ) {
				t.set_var("ship.scriptparseroutput",scriptparser.getOutput().replace("{{var.sessid}}", getString("sess")) );
			}
		
			addError("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl(getContext(), "default", "module", "angriff", "battle", ship.getString("battle"), "ship", ship.getInt("id")) );
			return;
		}
		
		offizier = Offizier.getOffizierByDest('s', ship.getInt("id"));
		
		StringBuilder tooltiptext = new StringBuilder(100);
		tooltiptext.append(Common.tableBegin(340, "center").replace('"', '\'') );
		tooltiptext.append("<iframe src='"+Common.buildUrl(getContext(), "default", "module", "impobjects", "system", ship.getInt("system"))+"' name='sector' width='320' height='300' scrolling='auto' marginheight='0' marginwidth='0' frameborder='0'>Ihr Browser unterst&uuml;tzt keine iframes</iframe>");
		tooltiptext.append(Common.tableEnd().replace('"', '\'') );
		String tooltiptextStr = StringEscapeUtils.escapeJavaScript(tooltiptext.toString().replace(">", "&gt;").replace("<", "&lt;"));

		t.set_var(	"ship.showui",			1,
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
					"shiptype.werft",		shiptype.getString("werft"),
					"tooltip.systeminfo",	tooltiptextStr,
					"ship.showalarm",		!noob && (shiptype.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) && shiptype.getInt("military") != 0 );
		
		if( ship.getInt("s") >= 100 ) {
			t.set_var("ship.s.color", "red");	
		}
		else if( ship.getInt("s") > 40 ) {
			t.set_var("ship.s.color", "yellow");	
		}
		else {
			t.set_var("ship.s.color", "white");	
		}
		
		if( offizier != null ) {
			t.set_block("_SCHIFF", "offiziere.listitem", "offiziere.list");
			
			SQLQuery offi = db.query("SELECT * FROM offiziere WHERE dest='s ",ship.getInt("id"),"'");
		
			while( offi.next() ) {
				Offizier offiObj = new Offizier( offi.getRow() );
				
				t.set_var(	"offizier.id",		offiObj.getID(),
							"offizier.name",	Common._plaintitle(offiObj.getName()),
							"offizier.picture",	offiObj.getPicture(),
							"offizier.rang",	offiObj.getRang() );
									
				t.parse("offiziere.list", "offiziere.listitem", true);
			}
			offi.free();
		}
		
		shiptype.put("sensorrange", Math.round(shiptype.getInt("sensorrange")*(ship.getInt("sensors")/100f)));

		if( shiptype.getInt("sensorrange") < 0 ) {
			shiptype.put("sensorrange", 0);
		}
		
		// Flottenlink
		if( ship.getInt("fleet") != 0 ) {
			SQLResultRow fleet = db.first("SELECT name FROM ship_fleets WHERE id=",ship.getInt("fleet"));
			if( !fleet.isEmpty() ) {
				t.set_var("fleet.name", Common._plaintitle(fleet.getString("name")) );
			} 
			else {
				t.set_var("ship.fleet", 0);
				db.update("UPDATE ships SET fleet=0 WHERE id>0 AND id=",ship.getInt("id"));
			}
		}
		
		// Aktion: Schnelllink GTU-Handelsdepot
		SQLResultRow handel = db.first("SELECT id,name FROM ships WHERE id>0 AND system=",ship.getInt("system")," AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," AND LOCATE('tradepost',status)");
		if( !handel.isEmpty() ) {
			t.set_var(	"sector.handel",		handel.getInt("id"),
						"sector.handel.name",	Common._plaintitle(handel.getString("name") ));
		}
		
		// Tooltip: Schiffscripte
		if( user.hasFlag( User.FLAG_EXEC_NOTES ) ) {
		
			String script = ship.getString("script").replace("\r\n", "\n").replace("\"", "\\\"");
			
			t.set_var(	"tooltip.execnotes",		1,
						"tooltip.execnotes.begin",	Common.tableBegin(400, "center").replace('"', '\''),
						"tooltip.execnotes.end",	Common.tableEnd().replace('"', '\''),
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

			t.set_var("tooltip.admin", tooltipStr );
				
			t.set_var(	"tooltip.respawn.begin",	Common.tableBegin(200,"center").replace( '"', '\''),
						"tooltip.respawn.end",		Common.tableEnd().replace( '"', '\'' ),
						"ship.respawn",				ship.getInt("respawn") );

			SQLResultRow rentry = db.first("SELECT id FROM ships WHERE id='-",ship.getInt("id"),"'");
			if( !rentry.isEmpty() ) {
				t.set_var(	"ship.show.respawn",	1,
							"ship.hasrespawn",		1);	
			}
			else {
				t.set_var( "ship.show.respawn", 1 );	
			}
		}
		
		if( user.hasFlag( User.FLAG_NPC_ISLAND ) ) {
			t.set_var("ship.npcislandlink", 1);
		}
		
		// Tooltip: Module
		if( (ship.getString("status").indexOf("tblmodules") > -1) && !shiptype.getString("modules").equals("") ) {
			List<String> tooltiplines = new ArrayList<String>();
			tooltiplines.add("<span style='text-decoration:underline'>Module:</span><br />");
			
			Ships.ModuleEntry[] modulelist = Ships.getModules(ship);
			
			SQLResultRow type = Ships.getShipType( ship.getInt("type"), false );
			SQLResultRow basetype = new SQLResultRow();
			basetype.putAll(type);
			
			Map<Integer,String[]> slotlist = new HashMap<Integer,String[]>();
			String[] tmpslotlist = StringUtils.split(type.getString("modules"),';');
			for( int i=0; i < tmpslotlist.length; i++ ) {
				String[] aslot = StringUtils.split(tmpslotlist[i], ':');
				slotlist.put(new Integer(aslot[0]), aslot);
			}
			
			List<Module> moduleObjList = new ArrayList<Module>();
			boolean itemmodules = false;
			
			for( int i=0; i < modulelist.length; i++ ) {
				Ships.ModuleEntry module = modulelist[i];
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
				type = moduleObjList.get(i).modifyStats( type, basetype, moduleObjList );
			}
			
			for( String propname : type.keySet() ) {
				// Alles was in moduleOutputList sitzt, muss in der DB durch einen von Number abgeleiteten Typ definiert sein!
				if( moduleOutputList.containsKey(propname) && !basetype.get(propname).equals(type.get(propname)) ) {
					double value = type.getDouble(propname);
					double baseValue = basetype.getDouble(propname);
					String text = null;
					if( baseValue < value ) {
						text = moduleOutputList.get(propname)+Common.ln(value)+" (<span class='nobr' style='color:green'>+";	
					}
					else {
						text = moduleOutputList.get(propname)+Common.ln(value)+" (<span class='nobr' style='color:red'>";	
					}
					text += Common.ln(value - baseValue)+"</span>)<br />";
					tooltiplines.add(text);
				}
				else if( propname.equals("weapon") ) {
					Map<String,String> weaponlist = Weapons.parseWeaponList( type.getString(propname) );
					Map<String,String> defweaponlist = Weapons.parseWeaponList( basetype.getString("weapon") );
					
					for( String aweapon : weaponlist.keySet() ) {
						int aweaponcount = Integer.parseInt(weaponlist.get(aweapon));
						if( !defweaponlist.containsKey(aweapon) ) {
							tooltiplines.add("<span class='nobr' style='color:green'>+"+aweaponcount+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
						}
						else if( Integer.parseInt(defweaponlist.get(aweapon)) < aweaponcount ) {
							tooltiplines.add("<span class='nobr' style='color:green'>+"+(aweaponcount - Integer.parseInt(defweaponlist.get(aweapon)))+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
						}	
						else if( Integer.parseInt(defweaponlist.get(aweapon)) > aweaponcount ) {
							tooltiplines.add("<span class='nobr' style='color:red'>"+(aweaponcount - Integer.parseInt(defweaponlist.get(aweapon)))+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
						}
					}
					
					for( String aweapon : defweaponlist.keySet() ) {
						if( !weaponlist.containsKey(aweapon) ) {
							tooltiplines.add("<span class='nobr' style='color:red'>-"+Integer.parseInt(defweaponlist.get(aweapon))+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
						}
					}
				}
				else if( propname.equals("maxheat") ) {
					Map<String,String> heatlist = Weapons.parseWeaponList( type.getString(propname) );
					Map<String,String> defheatlist = Weapons.parseWeaponList( basetype.getString("maxheat") );
					
					for( String aweapon : heatlist.keySet() ) {
						int aweaponheat = Integer.parseInt(heatlist.get(aweapon));
						if( !defheatlist.containsKey(aweapon) ) {
							tooltiplines.add("<span class='nobr' style='color:green'>+"+aweaponheat+" "+Weapons.get().weapon(aweapon).getName()+" Max-Hitze</span><br />");
						}
						else if( Integer.parseInt(defheatlist.get(aweapon)) < aweaponheat ) {
							tooltiplines.add("<span class='nobr' style='color:green'>+"+(aweaponheat - Integer.parseInt(defheatlist.get(aweapon)))+" "+Weapons.get().weapon(aweapon).getName()+" Max-Hitze</span><br />");
						}	
						else if( Integer.parseInt(defheatlist.get(aweapon)) > aweaponheat ) {
							tooltiplines.add("<span class='nobr' style='color:red'>"+(aweaponheat - Integer.parseInt(defheatlist.get(aweapon)))+" "+Weapons.get().weapon(aweapon).getName()+" Max-Hitze</span><br />");
						}
					}
				}
				else if( propname.equals("flags") ) {
					String[] newflaglist = StringUtils.split(type.getString(propname),' ');
					for( int i=0; i < newflaglist.length; i++ ) {
						if( newflaglist[i].equals("") ) {
							continue;	
						}	
						
						if( !Ships.hasShipTypeFlag(basetype, newflaglist[i]) ) {
							tooltiplines.add("<span class='nobr' style='color:green'>"+Ships.getShipTypeFlagName(newflaglist[i])+"</span><br />");
						}
					}
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
				t.set_var("tooltip.moduleext", tooltipStr);
			}
			else {
				t.set_var("tooltip.module", tooltipStr);
			}
		}
		
		// Schilde aufladen
		if( shiptype.getInt("shields") > 0 && (ship.getInt("shields") < shiptype.getInt("shields")) ) {
			int shieldfactor = 10;
			if( shiptype.getInt("shields") < 1000 ) {
				shieldfactor = 10;
			}
			
			t.set_var("ship.shields.reloade", Math.ceil((shiptype.getInt("shields") - ship.getInt("shields"))/shieldfactor));
		}
		
		String[] alarms = {"yellow","red"};
		String[] alarmn = {"gelb","rot"};
	
		// Alarmstufe aendern
		t.set_block("_SCHIFF", "ship.alarms.listitem", "ship.alarms.list");
		for( int a = 0; a < alarms.length; a++ ) {
			t.set_var(	"alarm.id",			a,
						"alarm.name", 		alarmn[a],
						"alarm.selected",	(ship.getInt("alarm") == a) );
			t.parse("ship.alarms.list", "ship.alarms.listitem", true);
		}

		if( shiptype.getString("werft").equals("") && (ship.getString("status").indexOf("noconsign") == -1) && 
			ship.getString("lock").equals("") ) {
			t.set_var("ship.consignable", 1);
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
	
		if( (scriptparser != null) && !scriptparser.getOutput().equals("") ) {
			t.set_var("ship.scriptparseroutput",scriptparser.getOutput().replace("{{var.sessid}}", getString("sess")));
		}
	
		caller.target = "plugin.output";
		t.set_block("_SCHIFF","plugins.listitem","plugins.list");
		
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
