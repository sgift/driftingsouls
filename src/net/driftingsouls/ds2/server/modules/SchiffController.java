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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.ContextCommon;
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
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.schiffplugins.Parameters;
import net.driftingsouls.ds2.server.modules.schiffplugins.SchiffPlugin;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.scripting.entities.Script;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die Schiffsansicht.
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des anzuzeigenden Schiffes
 *
 */
@Configurable
public class SchiffController extends TemplateGenerator {
	private Log log = LogFactory.getLog(SchiffController.class);
	
	private Ship ship = null;
	private ShipTypeData shiptype = null;
	private Offizier offizier = null;
	private Map<String,SchiffPlugin> pluginMapper = new LinkedHashMap<String,SchiffPlugin>();
	private boolean noob = false;
	
	private Configuration config;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public SchiffController(Context context) {
		super(context);
		
		setTemplate("schiff.html");
		
		parameterNumber("ship");
		
		setPageTitle("Schiff");
	}
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
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
			log.error(e,e);
			return null;
		}
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		t.setVar( "user.tooltips", user.getUserValue("TBLORDER/schiff/tooltips") );
		
		int shipid = getInteger("ship");
		
		ship = (Ship)db.get(Ship.class, shipid);
		if( (ship == null) || (ship.getId() < 0) || (ship.getOwner() != user) ) 
		{
			addError("Das angegebene Schiff existiert nicht", Common.buildUrl("default","module", "schiffe") );
			return false;
		}

		if( ship.getBattle() != null ) 
		{
			addError("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl("default", "module", "angriff", "battle", ship.getBattle().getId(), "ship", shipid) );
			return false;
		}


		shiptype = ship.getTypeData();

		offizier = ship.getOffizier();
		
		if( !action.equals("communicate") && !action.equals("onmove") && !action.equals("onenter") && (ship.getLock() != null) && !ship.getLock().equals("") ) {
			ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
			scriptparser.getContext().setAttribute("_SHIP", ship, ScriptContext.ENGINE_SCOPE);
			
			if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
				scriptparser.getContext().setErrorWriter(new NullLogger());
			}
			
			Quests.executeLock(scriptparser, ship.getLock(), user);
		}
		
		pluginMapper.put("navigation", getPluginByName("NavigationDefault"));
		pluginMapper.put("cargo", getPluginByName("CargoDefault"));
		
		if( shiptype.getWerft() != 0 ) {
			pluginMapper.put("werft", getPluginByName("WerftDefault"));
		}
		
		if( shiptype.hasFlag(ShipTypes.SF_JUMPDRIVE_SHIVAN) ) {
			pluginMapper.put("jumpdrive", getPluginByName("JumpdriveShivan"));
		}
		
		pluginMapper.put("sensors", getPluginByName("SensorsDefault"));		
		
		if( shiptype.getADocks() > 0 ) {
			pluginMapper.put("adocks", getPluginByName("ADocksDefault"));
		}
		
		if( shiptype.getJDocks() > 0 ) {
			pluginMapper.put("jdocks", getPluginByName("JDocksDefault"));
		}
		
		if( shiptype.getUnitSpace() > 0 ) {
			pluginMapper.put("units", getPluginByName("UnitsDefault"));
		}
		
		noob = user.isNoob();
		
		// URL fuer Quests setzen
		Quests.currentEventURLBase.set("./ds?module=schiff&ship="+getInteger("ship"));
		
		return true;	
	}
	
	/**
	 * Wechselt die Alarmstufe des Schiffes.
	 * @urlparam Integer alarm Die neue Alarmstufe
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void alarmAction() {
		if( noob ) {
			redirect();
			return;	
		}
		
		if( (shiptype.getShipClass() == ShipClasses.GESCHUETZ.ordinal()) || !shiptype.isMilitary() ) {
			redirect();
			return;	
		}
		
		parameterNumber("alarm");
		int alarm = getInteger("alarm");
		
		if( (alarm >= Ship.Alert.GREEN.getCode()) && (alarm <= Ship.Alert.RED.getCode()) ) { 
			ship.setAlarm(alarm);
			
			getTemplateEngine().setVar("ship.message", "Alarmstufe erfolgreich ge&auml;ndert<br />");
		}
		
		ship.recalculateShipStatus();
		
		redirect();
	}
	
	/**
	 * Uebergibt das Schiff an einen anderen Spieler.
	 * @urlparam Integer newowner Die ID des neuen Besitzers
	 * @urlparam Integer conf 1, falls die Sicherheitsabfrage positiv bestaetigt wurde
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void consignAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("newowner");
		int newownerID = getInteger("newowner");
		
		User newowner = (User)db.get(User.class, newownerID);
		if( newowner == null ) {
			t.setVar("ship.message", "<span style=\"color:red\">Der Spieler existiert nicht</span><br />");
			redirect();
			return;
		}
		
		parameterNumber("conf");
		int conf = getInteger("conf");
		
		if( conf == 0 ) {
			String text = "<span style=\"color:white\">Wollen sie das Schiff "+Common._plaintitle(ship.getName())+" ("+ship.getId()+") wirklich an "+newowner.getProfileLink()+" &uuml;bergeben?</span><br />";
			text += "<a class=\"ok\" href=\""+Common.buildUrl("consign", "ship", ship.getId(), "conf", 1 , "newowner", newowner.getId())+"\">&Uuml;bergeben</a></span><br />";
			t.setVar( "ship.message", text );
			
			redirect();
			return;
		}
		
		ShipFleet fleet = ship.getFleet();
		
		boolean result = ship.consign(newowner, false);
			
		if( result ) {
			t.setVar("ship.message", Ship.MESSAGE.getMessage());
					
			redirect();
		}
		else {
			String msg = "Ich habe dir die "+ship.getName()+" ("+ship.getId()+"), ein Schiff der "+shiptype.getNickname()+"-Klasse, &uuml;bergeben\nSie steht bei " + ship.getLocation().displayCoordinates(false);
			PM.send(user, newowner.getId(), "Schiff &uuml;bergeben", msg);
		
			String consMessage = Ship.MESSAGE.getMessage();
			t.setVar("ship.message", (!consMessage.equals("") ? consMessage+"<br />" : "")+"<span style=\"color:green\">Das Schiff wurde erfolgreich an "+newowner.getProfileLink()+" &uuml;bergeben</span><br />");
			
			if( fleet != null ) {
				long fleetcount = (Long)db.createQuery("select count(*) from Ship where id>0 and fleet=?")
					.setEntity(0, fleet)
					.iterate().next();
			
				if( fleetcount < 3 ) {
					db.createQuery("update Ship set fleet=null where id>0 and fleet=?")
						.setEntity(0, fleet)
						.executeUpdate();
					
					db.delete(fleet);
				}
			}	
		}
		
		return;
	}
	
	/**
	 * Zerstoert das Schiff.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void destroyAction() {
		TemplateEngine t = getTemplateEngine();

		if( (ship.getLock() != null) && (ship.getLock().length() > 0) ) {
			t.setVar("ship.message", "<span style=\"color:red\">Dieses Schiff kann sich nicht selbstzerst&ouml;ren, da es in ein Quest eingebunden ist</span><br />");
			redirect();
			return;
		}
		if( ship.isNoSuicide() )
		{
			t.setVar("ship.message", "<span style=\"color:red\">Dieses Schiff kann sich nicht selbstzerst&ouml;ren.</span><br />");
			redirect();
			return;
		}
	
		parameterNumber("conf");
		int conf = getInteger("conf");
	
		if( conf == 0 ) {
			String text = "<span style=\"color:white\">Wollen sie Selbstzerst&ouml;rung des Schiffes "+Common._plaintitle(ship.getName())+" ("+ship.getId()+") wirklich ausf&uuml;hren?</span><br />\n";
			text += "<a class=\"error\" href=\""+Common.buildUrl("destroy", "ship", ship.getId(), "conf", 1)+"\">Selbstzerst&ouml;rung</a></span><br />";
			t.setVar("ship.message", text);
			
			redirect();
			return;
		}
	
		ship.destroy();

		t.setVar("ship.message", "<span style=\"color:white\">Das Schiff hat sich selbstzerst&ouml;rt</span><br />");
		return;
	}
	
	/**
	 * Springt durch den angegebenen Sprungpunkt.
	 * @urlparam Integer knode Die ID des Sprungpunkts
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void jumpAction() {
		TemplateEngine t = getTemplateEngine();
		
		if( (shiptype.getCost() == 0) || (ship.getEngine() == 0) ) {
			redirect();
			return;
		}
		
		parameterNumber("node");
		int node = getInteger("node");
		
		if( node != 0 ) {
			ship.jump(node, false);
			t.setVar("ship.message", Ship.MESSAGE.getMessage());
		}

		redirect();
	}
	
	/**
	 * Benutzt einen an ein Schiff assoziierten Sprungpunkt.
	 * @urlparam Integer knode Die ID des Schiffes mit dem Sprungpunkt
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void kjumpAction() {
		TemplateEngine t = getTemplateEngine();
		
		if( (shiptype.getCost() == 0) || (ship.getEngine() == 0) ) {
			redirect();
			return;
		}
		
		parameterNumber("knode");
		int knode = getInteger("knode");
		
		if( knode != 0 ) {
			ship.jump(knode, true);
			t.setVar("ship.message", Ship.MESSAGE.getMessage());
		}

		redirect();
	}
	
	/**
	 * Benennt das Schiff um.
	 * @urlparam String newname Der neue Name des Schiffes
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void renameAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterString("newname");
		String newname = getString("newname");
		
		ship.setName(newname);
		t.setVar("ship.message", "Name zu "+Common._plaintitle(newname)+" ge&auml;ndert<br />");
	
		redirect();
	}
	
	/**
	 * Fuehrt Aktionen eines Plugins aus. Plugin-spezifische
	 * Parameter haben die Form $PluginName_ops[$ParameterName].
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
	 * Landet die angegebenen Schiffe auf dem aktuellen Schiff.
	 * @urlparameter String shiplist Eine mit | separierte Liste an Schiffs-IDs
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void landAction() {
		TemplateEngine t = getTemplateEngine();

		parameterString("shiplist");
		String shipIdList = getString("shiplist");
		
		if( shipIdList.equals("") ) {
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
			return;
		}
		
		int[] shipidlist = Common.explodeToInt("|",shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for( int i=0; i < shipidlist.length; i++ ) {
			Ship aship = (Ship)getDB().get(Ship.class, shipidlist[i]);
			if( aship == null ) {
				addError("Eines der angegebenen Schiffe existiert nicht");
				redirect();
				return;
			}
			shiplist[i] = aship;
		}
		
		ship.land(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Startet die angegebenen Schiffe vom aktuellen Schiff.
	 * @urlparam String shiplist Eine mit | separierte Liste von Schiffs-IDs
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void startAction() {
		TemplateEngine t = getTemplateEngine();

		parameterString("shiplist");
		String shipIdList = getString("shiplist");
		
		if( shipIdList.equals("") ) {
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
			return;
		}
		
		int[] shipidlist = Common.explodeToInt("|",shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for( int i=0; i < shipidlist.length; i++ ) {
			Ship aship = (Ship)getDB().get(Ship.class, shipidlist[i]);
			if( aship == null ) {
				addError("Eines der angegebenen Schiffe existiert nicht");
				redirect();
				return;
			}
			shiplist[i] = aship;
		}
		
		ship.start(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Dockt die angegebenen Schiffe an das aktuelle Schiff an.
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
		
		int[] shipidlist = Common.explodeToInt("|",shipIdList);
		
		org.hibernate.Session db = getDB();
		
		List<?> dockedList = db.createQuery("from Ship where id>0 and id in ("+Common.implode(",", shipidlist)+") and docked!=''")
			.list();
		for( Iterator<?> iter=dockedList.iterator(); iter.hasNext(); ) {
			Ship docked = (Ship)iter.next();
			
			if( docked.getOwner() != user ) {
				addError("Eines der Schiffe gehoert nicht ihnen");
				redirect();
				return;
			}
			
			Ship targetShip = docked.getBaseShip();
			
			targetShip.undock(docked);
		}
		
		Ship[] shiplist = new Ship[shipidlist.length];
		for( int i=0; i < shipidlist.length; i++ ) {
			Ship aship = (Ship)getDB().get(Ship.class, shipidlist[i]);
			if( aship == null ) {
				addError("Eines der angegebenen Schiffe existiert nicht");
				redirect();
				return;
			}
			shiplist[i] = aship;
		}
		
		ship.dock(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Dockt die angegebenen Schiffe vom aktuellen Schiff ab.
	 * @urlparam String shiplist Eine mit | separierte Liste von Schiffs-IDs
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void abladenAction() {
		TemplateEngine t = getTemplateEngine();

		parameterString("tar");
		String shipIdList = getString("tar");
		
		if( shipIdList.equals("") ) {
			t.setVar("ship.message", "Es wurden keine Schiffe angegeben");
			redirect();
		}
		
		int[] shipidlist = Common.explodeToInt("|",shipIdList);
		Ship[] shiplist = new Ship[shipidlist.length];
		for( int i=0; i < shipidlist.length; i++ ) {
			Ship aship = (Ship)getDB().get(Ship.class, shipidlist[i]);
			if( aship == null ) {
				addError("Eines der angegebenen Schiffe existiert nicht");
				redirect();
				return;
			}
			shiplist[i] = aship;
		}
		
		ship.undock(shiplist);
		t.setVar("ship.message", Ship.MESSAGE.getMessage());

		redirect();
	}
	
	/**
	 * Laesst ein Schiff einer Flotte beitreten oder aus der aktuellen Flotte austreten.
	 * @urlparam Integer join Die ID der Flotte, der das Schiff beitreten soll oder <code>0</code>, falls es aus der aktuellen Flotte austreten soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void joinAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("join");
		int join = getInteger("join");

		// Austreten
		if( (join == 0) && ((ship.getLock() == null) || (ship.getLock().length() == 0)) ) {
			ship.removeFromFleet();
			
			t.setVar("ship.message", "<span style=\"color:green\">"+Ship.MESSAGE.getMessage()+"</span><br />");
		} 
		else if( join == 0 ) {
			t.setVar("ship.message", "<span style=\"color:red\">Dieses Schiff kann nicht aus der Flotte austreten, da diese in ein Quest eingebunden ist</span><br />");		
		}
		// Beitreten
		else {
			Ship fleetship = (Ship)db.get(Ship.class, join);
			if( (fleetship == null) || (fleetship.getId() < 0) ) {
				redirect();
				return;
			}
			
			ShipFleet fleet = fleetship.getFleet();
			
			if( fleet == null ) {
				t.setVar("ship.message", "<span style=\"color:red\">Sie m&uuml;ssen erst eine Flotte erstellen</span><br />");
				redirect();
				return;
			}
		
			if( ((fleetship.getLock() != null) && (fleetship.getLock().length() > 0)) || ((ship.getLock() != null) && (ship.getLock().length() > 0)) ) {
				t.setVar("ship.message", "<span style=\"color:red\">Sie k&oumlnnen der Flotte nicht beitreten, solange entweder das Schiff oder die Flotte in ein Quest eingebunden ist</span><br />");
				redirect();
				
				return;
			}
			
			if( !ship.getLocation().sameSector(0, fleetship.getLocation(), 0) || ( fleetship.getOwner() != user ) ) {
				t.setVar("ship.message", "<span style=\"color:red\">Beitritt zur Flotte &quot;"+Common._plaintitle(fleet.getName())+"&quot; nicht m&ouml;glich</span><br />");
			}
			else {
				ship.setFleet(fleet);
				t.setVar("ship.message", "<span style=\"color:green\">Flotte &quot;"+Common._plaintitle(fleet.getName())+"&quot; beigetreten</span><br />");
			}
		}
		
		redirect();
	}
	
	/**
	 * Laedt die Schilde des aktuellen Schiffes auf.
	 * @urlparam Integer shup Die Menge an Energie, die zum Aufladen der Schilde verwendet werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shupAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("shup");
		int shup = getInteger("shup");
		
		int shieldfactor = 100;
		if( shiptype.getShields() < 1000 ) {
			shieldfactor = 10;
		}

		final int maxshup = (int)Math.ceil((shiptype.getShields() - ship.getShields())/(double)shieldfactor);
		if( shup > maxshup ) {
			shup = maxshup;
		}
		if( shup > ship.getEnergy() ) {
			shup = ship.getEnergy();
		}

		t.setVar("ship.message", "Schilde +"+(shup*shieldfactor)+"<br />");
		
		ship.setShields(ship.getShields() + shup*shieldfactor);
		if( ship.getShields() > shiptype.getShields() ) {
			ship.setShields(shiptype.getShields());
		}
	
		ship.setEnergy(ship.getEnergy() - shup);

		ship.recalculateShipStatus();
	
		redirect();
	}
	
	/**
	 * Speichert ein neues Schiffsaktionsscript und setzt optional
	 * die aktuellen Ausfuehrungsdaten wieder zurueck.
	 * @urlparam String script das neue Schfifsaktionsscript
	 * @urlparam Integer reset Wenn der Wert != 0 ist, dann werden die Ausfuehrungsdaten zurueckgesetzt
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void scriptAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterString("script");
		parameterNumber("reset");
		
		String script = getString("script");
		int reset = getInteger("reset");
		
		if( !script.trim().equals("") ) {
			if( reset != 0 ) {
				ship.setScriptExeData(null);
			}
			ship.setScript(script);
		}
		else {
			ship.setScriptExeData(null);
			ship.setScript(null);		
		}
		
		t.setVar("ship.message", "Script gespeichert<br />");
	
		redirect();
	}
	
	/**
	 * Behandelt ein OnCommunicate-Ereigniss.
	 * @urlparam Integer communicate Die ID des Schiffes, mit dem kommuniziert werden soll
	 * @urlparam String execparameter Weitere Ausfuehrungsdaten
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void communicateAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("communicate");
		int communicate = getInteger("communicate");
		
		String[] lock = StringUtils.split(ship.getLock(), ':');

		if( (lock != null) && ((lock.length > 2) && !lock[2].equals(Quests.EVENT_ONCOMMUNICATE)) ) {
			redirect();
			
			return;
		}
	
		ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
		final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
		
		engineBindings.put("_SHIP", ship);
		if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.getContext().setErrorWriter(new NullLogger());
		}
		
		Quests.currentEventURL.set("&action=communicate&communicate="+communicate);

		engineBindings.put("TARGETSHIP", Integer.toString(communicate));
	
		parameterString("execparameter");
		String execparameter = getString( "execparameter" );
		if( execparameter.equals("") ) {
			execparameter = "0";	
		}
	
		Ship targetship = (Ship)db.get(Ship.class, communicate);
		if( (targetship == null) || (targetship.getId() < 0) || !targetship.getLocation().sameSector(0, ship.getLocation(), 0) ) {
			t.setVar("ship.message", "<span style=\"color:red\">Sie k&ouml;nnen nur mit Schiffen im selben Sektor kommunizieren</span><br />");
			redirect();
			return;
		}
		Quests.executeEvent( scriptparser, targetship.getOnCommunicate(), user, execparameter, lock != null && lock.length > 2 );
		
		redirect();
	}
	
	/**
	 * Behandelt ein OnMove-Ereigniss.
	 * @urlparam String execparameter Weitere Ausfuehrungsdaten
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void onmoveAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		String[] lock = StringUtils.split(ship.getLock(), ':');
		
		if( (lock == null) || ((lock.length > 2) && !lock[2].equals(Quests.EVENT_ONMOVE)) ) {
			redirect();
			
			return;
		}
	
		ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
		scriptparser.getContext().setAttribute("_SHIP", ship, ScriptContext.ENGINE_SCOPE);
		
		if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.getContext().setErrorWriter(new NullLogger());
		}
	
		Quests.currentEventURL.set("&action=onmove");

		parameterString("execparameter");
		String execparameter = getString( "execparameter" );
		if( execparameter.equals("") ) {
			execparameter = "0";	
		}
	
		if( (ship.getOnMove() == null) || ship.getOnMove().equals("") ) {
			t.setVar("ship.message", "<span style=\"color:red\">Das angegebene Schiff verf&uuml;gt nicht &uuml;ber dieses Ereigniss</span><br />");
			redirect();
			return;	
		}

		Quests.executeEvent( scriptparser, ship.getOnMove(), user, execparameter, lock.length > 2 );
	
		redirect();
	}
	
	/**
	 * Behandelt ein OnEnter-Ereignis.
	 * @urlparam String execparameter Weitere Ausfuehrungsdaten
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void onenterAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		
		String[] lock = StringUtils.split(ship.getLock(), ':');
		
		if( (lock == null) || ((lock.length > 2) && !lock[2].equals(Quests.EVENT_ONENTER)) ) {
			redirect();
			
			return;
		}
	
		ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
		scriptparser.getContext().setAttribute("_SHIP", ship, ScriptContext.ENGINE_SCOPE);
		
		if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.getContext().setErrorWriter(new NullLogger());
		}
		
		// TODO: migrate to Quests.executeEvent
		
		if( lock.length < 3 ) {
			redirect();
			return;
		}
		
		String usescript = lock[0];
		String rquestid = lock[1].substring(1);	
	
		String usequest = lock[1];
	
		if( usescript.equals("-1") ) {
			t.setVar("ship.message", "Das angegebene Schiff antwortet auf ihre Funksignale nicht<br />");
			redirect();
			return;
		}
		parameterString("execparameter");
		String execparameter = getString( "execparameter" );
		if( execparameter.equals("") ) {
			execparameter = "0";	
		}
	
		Quests.currentEventURL.set("&action=onenter");
		
		RunningQuest runningdata = (RunningQuest)db.get(RunningQuest.class, Integer.valueOf(rquestid));
		Blob execdata = null;
	
		if( runningdata == null ) {
			t.setVar("ship.message", "FATAL QUEST ERROR: keine running-data gefunden!<br />");
			redirect();
			return;
		}
		try {
			execdata = runningdata.getExecData();
			if( (execdata != null) && (execdata.length() > 0) ) { 
				scriptparser.setContext(
						ScriptParserContext.fromStream(execdata.getBinaryStream())
				);
			}
		}
		catch( Exception e ) {
			log.warn("Setting Script-ExecData failed (Ship: "+ship.getId()+": ",e);
			return;
		}

		final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
		
		Script script = (Script)db.get(Script.class, Integer.valueOf(usescript));
		engineBindings.put("USER", Integer.toString(user.getId()));
		if( !usequest.equals("") ) {
			engineBindings.put("QUEST", "r"+runningdata.getId());
		}
		engineBindings.put("SCRIPT", usescript);
		engineBindings.put("SECTOR", ship.getLocation().toString());
		if( (lock.length > 2) ) {		
			engineBindings.put("LOCKEXEC", "1");
		}

		engineBindings.put("_PARAMETERS", execparameter);
		try {
			scriptparser.eval(script.getScript());
		}
		catch( ScriptException e ) {
			throw new RuntimeException(e);
		}
	
		usequest = (String)engineBindings.get("QUEST");
		
		if( !usequest.equals("") ) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ScriptParserContext.toStream(scriptparser.getContext(), out);
				runningdata.setExecData(Hibernate.createBlob(out.toByteArray()));
			}
			catch( Exception e ) {
				log.warn("Writing back Script-ExecData failed (Ship: "+ship.getId()+": ",e);
				return;
			}
		}
		redirect();
	}
	
	/**
	 * Transferiert das Schiff ins System 99.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void inselAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser(); 
		
		if( !user.hasFlag( User.FLAG_NPC_ISLAND ) ) {
			redirect();
			return;	
		}
		
		ship.setX(10);
		ship.setY(10);
		ship.setSystem(99);
		
		t.setVar("ship.message", "<span style=\"color:green\">Willkommen auf der Insel <img align=\"middle\" src=\""+config.get("SMILIE_PATH")+"/icon_smile.gif\" alt=\":)\" /></span><br />");
		
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
	 * Zeigt die Schiffsansicht an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		db.flush();
			
		ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
		if( ship.isDestroyed() ) {
			if( (scriptparser != null) && (scriptparser.getContext().getWriter().toString().length() != 0) ) {
				t.setVar("ship.scriptparseroutput", scriptparser.getContext().getWriter().toString() );
			}
			else {
				addError("Das Schiff existiert nicht mehr oder geh&ouml;rt nicht mehr ihnen");
			}
			return;
		}
		
	//	db.refresh(ship);
		shiptype = ship.getTypeData();
		
		if( ship.getBattle() != null ) {
			if( (scriptparser != null) && (scriptparser.getContext().getWriter().toString().length() > 0) ) {
				t.setVar("ship.scriptparseroutput", scriptparser.getContext().getWriter().toString() );
			}
		
			addError("Das Schiff ist in einen Kampf verwickelt (hier klicken um zu diesem zu gelangen)!", Common.buildUrl("default", "module", "angriff", "battle", ship.getBattle().getId(), "ship", ship.getId()) );
			return;
		}
		
		ship.recalculateShipStatus();
		offizier = ship.getOffizier();
		
		StringBuilder tooltiptext = new StringBuilder(100);
		tooltiptext.append(Common.tableBegin(340, "center").replace('"', '\'') );
		tooltiptext.append("<iframe src='"+Common.buildUrl("default", "module", "impobjects", "system", ship.getSystem())+"' name='sector' width='320' height='300' scrolling='auto' marginheight='0' marginwidth='0' frameborder='0'>Ihr Browser unterst&uuml;tzt keine iframes</iframe>");
		tooltiptext.append(Common.tableEnd().replace('"', '\'') );
		String tooltiptextStr = StringEscapeUtils.escapeJavaScript(tooltiptext.toString().replace(">", "&gt;").replace("<", "&lt;"));

		t.setVar(	"ship.showui",			1,
					"ship.islanded",		ship.isLanded(),
					"ship.id",				ship.getId(),
					"ship.name",			Common._plaintitle(ship.getName()),
					"ship.location",		ship.getLocation().displayCoordinates(false),
					"ship.type",			ship.getType(),
					"shiptype.picture",		shiptype.getPicture(),
					"shiptype.name",		shiptype.getNickname(),
					"ship.hull.color",		genSubColor(ship.getHull(), shiptype.getHull()),
					"ship.hull",			Common.ln(ship.getHull()),
					"shiptype.hull",		Common.ln(shiptype.getHull()),
					"ship.ablativearmor.color",		genSubColor(ship.getAblativeArmor(), shiptype.getAblativeArmor()),
					"ship.ablativearmor",			Common.ln(ship.getAblativeArmor()),
					"shiptype.ablativearmor",		Common.ln(shiptype.getAblativeArmor()),
					"ship.shields.color",	genSubColor(ship.getShields(), shiptype.getShields()),
					"ship.shields",			Common.ln(ship.getShields()),
					"shiptype.shields",		Common.ln(shiptype.getShields()),
					"shiptype.cost",		shiptype.getCost(),
					"ship.engine.color",	genSubColor(ship.getEngine(), 100),
					"ship.engine",			ship.getEngine(),
					"shiptype.weapon",		shiptype.isMilitary(),
					"ship.weapons.color",	genSubColor(ship.getWeapons(), 100),
					"ship.weapons",			ship.getWeapons(),
					"ship.comm.color",		genSubColor(ship.getComm(), 100),
					"ship.comm",			ship.getComm(),
					"ship.sensors.color",	genSubColor(ship.getSensors(), 100),
					"ship.sensors",			ship.getSensors(),
					"shiptype.crew",		Common.ln(shiptype.getCrew()),
					"ship.crew",			Common.ln(ship.getCrew()),
					"ship.crew.color",		genSubColor(ship.getCrew(), shiptype.getCrew()),
					"ship.marines",			(shiptype.getUnitSpace() > 0) ? true : false,
					"ship.e",				Common.ln(ship.getEnergy()),
					"shiptype.eps",			Common.ln(shiptype.getEps()),
					"ship.e.color",			genSubColor(ship.getEnergy(), shiptype.getEps()),
					"ship.s",				ship.getHeat(),
					"ship.fleet",			ship.getFleet() != null ? ship.getFleet().getId() : 0,
					"ship.lock",			ship.getLock(),
					"shiptype.werft",		shiptype.getWerft(),
					"tooltip.systeminfo",	tooltiptextStr,
					"ship.showalarm",		!noob && (shiptype.getShipClass() != ShipClasses.GESCHUETZ.ordinal()) && shiptype.isMilitary() );
		
		if( ship.getHeat() >= 100 ) {
			t.setVar("ship.s.color", "red");	
		}
		else if( ship.getHeat() > 40 ) {
			t.setVar("ship.s.color", "yellow");	
		}
		else {
			t.setVar("ship.s.color", "white");	
		}
		
		if( offizier != null ) {
			t.setBlock("_SCHIFF", "offiziere.listitem", "offiziere.list");
			
			List<Offizier> offiziere = Offizier.getOffiziereByDest('s', ship.getId());
			for( Offizier offi : offiziere ) {
				t.setVar(	"offizier.id",		offi.getID(),
							"offizier.name",	Common._plaintitle(offi.getName()),
							"offizier.picture",	offi.getPicture(),
							"offizier.rang",	offi.getRang() );
									
				t.parse("offiziere.list", "offiziere.listitem", true);
			}
		}
		
		// Flottenlink
		if( ship.getFleet() != null ) {
			t.setVar(
					"fleet.name", Common._plaintitle(ship.getFleet().getName()),
					"fleet.id", ship.getFleet().getId() );
		}
		
		// Aktion: Schnelllink GTU-Handelsdepot
		Iterator<?> handel = db.createQuery("from Ship where id>0 and system=? and x=? and y=? and locate('tradepost',status)!=0")
			.setInteger(0, ship.getSystem())
			.setInteger(1, ship.getX())
			.setInteger(2, ship.getY())
			.iterate();
		
		if( handel.hasNext() ) {
			Ship handelShip = (Ship)handel.next();
			t.setVar(	"sector.handel",		handelShip.getId(),
						"sector.handel.name",	Common._plaintitle(handelShip.getName()));
		}
		
		// Tooltip: Tradepost
		if(ship.isTradepost())
		{
			t.setVar("tooltip.tradepost", 1);
		}
		
		// Tooltip: Schiffscripte
		if( user.hasFlag( User.FLAG_EXEC_NOTES ) ) {
		
			String script = StringUtils.replace(ship.getScript(),"\r\n", "\n");
			script = StringUtils.replace(script,"\n", "\\n");
			script = StringUtils.replace(script,"\"", "\\\"");
			
			t.setVar(	"tooltip.execnotes",		1,
						"tooltip.execnotes.begin",	StringUtils.replaceChars(Common.tableBegin(400, "center"),'"', '\''),
						"tooltip.execnotes.end",	StringUtils.replaceChars(Common.tableEnd(),'"', '\''),
						"tooltip.execnotes.script",	script );
		}
		
		// Tooltip: Schiffsstatusfeld
		if( user.isAdmin() ) {
			tooltiptext = new StringBuilder(100);
			tooltiptext.append(Common.tableBegin(200, "left").replace('"', '\''));
			tooltiptext.append("<span style='text-decoration:underline'>Schiffsstatus:</span><br />"+ship.getStatus().trim().replace(" ", "<br />"));
			if( (ship.getLock() != null) && (ship.getLock().length() > 0) ) {
				tooltiptext.append("<br /><span style='text-decoration:underline'>Lock:</span><br />"+ship.getLock()+"<br />");
			}
			tooltiptext.append(Common.tableEnd().replace( '"', '\''));
			String tooltipStr = StringEscapeUtils.escapeJavaScript(tooltiptext.toString().replace(">", "&gt;").replace("<", "&lt;"));

			t.setVar("tooltip.admin", tooltipStr );
		}
		
		if( user.hasFlag( User.FLAG_NPC_ISLAND ) ) {
			t.setVar("ship.npcislandlink", 1);
		}
		
		// Tooltip: Module
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
					log.error("Fehler beim Aufruf der Property "+method,e);
				}
				catch( NoSuchMethodException e ) {
					log.error("Ungueltige Property "+method,e);
				}
				catch( IllegalAccessException e ) {
					log.error("Ungueltige Property "+method,e);
				}
			}
			
			// Weapons
			Map<String,String> weaponlist = Weapons.parseWeaponList( type.getWeapons() );
			Map<String,String> defweaponlist = Weapons.parseWeaponList( basetype.getWeapons() );
			
			for( Map.Entry<String, String> entry: weaponlist.entrySet() )
			{
				String aweapon = entry.getKey();
				int aweaponcount = Integer.parseInt(entry.getValue());
				if( !defweaponlist.containsKey(aweapon) )
				{
					tooltiplines.add("<span class='nobr' style='color:green'>+"+aweaponcount+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
				}
				else
				{
					String defweapon = defweaponlist.get(aweapon);
					if( Integer.parseInt(defweapon) < aweaponcount )
					{
						tooltiplines.add("<span class='nobr' style='color:green'>+"+(aweaponcount - Integer.parseInt(defweapon))+" "+Weapons.get().weapon(aweapon).getName()+"</span><br />");
					}	
					else if( Integer.parseInt(defweapon) > aweaponcount )
					{
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
		
		// Schilde aufladen
		if( shiptype.getShields() > 0 && (ship.getShields() < shiptype.getShields()) ) {
			int shieldfactor = 100;
			if( shiptype.getShields() < 1000 ) {
				shieldfactor = 10;
			}
			
			t.setVar("ship.shields.reloade", Common.ln((int)Math.ceil((shiptype.getShields() - ship.getShields())/(double)shieldfactor)));
		}
		
		String[] alarmn = {"gr&uuml;n","gelb","rot"};
	
		// Alarmstufe aendern
		t.setBlock("_SCHIFF", "ship.alarms.listitem", "ship.alarms.list");
		for( int a = 0; a < alarmn.length; a++ ) {
			t.setVar(	"alarm.id",			a,
						"alarm.name", 		alarmn[a],
						"alarm.selected",	(ship.getAlarm() == a) );
			t.parse("ship.alarms.list", "ship.alarms.listitem", true);
		}

		if( (ship.getStatus().indexOf("noconsign") == -1) && ((ship.getLock() == null) || ship.getLock().equals("")) ) {
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
		
		if( pluginMapper.containsKey("units"))
		{
			SchiffPlugin plugin = pluginMapper.get("units");
			caller.pluginId = "units";
			caller.target = "plugin.units";
			plugin.output(caller);
			
			pluginMapper.remove("units");
		}
		
		/* 
			Ok...das ist kein Plugin, es gehoert aber trotzdem zwischen die ganzen Plugins (Questoutput) 
		*/
	
		if( (scriptparser != null) ) {
			t.setVar("ship.scriptparseroutput", scriptparser.getContext().getWriter().toString());
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
