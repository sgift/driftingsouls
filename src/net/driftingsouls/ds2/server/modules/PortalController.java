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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.SectorTemplateManager;
import net.driftingsouls.ds2.server.bases.AutoGTUAction;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.authentication.AccountDisabledException;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationException;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.authentication.LoginDisabledException;
import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import net.driftingsouls.ds2.server.framework.authentication.WrongPasswordException;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;

import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Required;

/**
 * Das Portal.
 * @author Christopher Jung
 *
 */
@Configurable
@Module(name="portal", defaultModule=true)
public class PortalController extends TemplateGenerator {
	private AuthenticationManager authManager;
	private Configuration config;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public PortalController(Context context) {
		super(context);

		setTemplate("portal.html");
	}
	
	/**
	 * Injiziert den DS-AuthenticationManager zum einloggen von Benutzern.
	 * @param authManager Der AuthenticationManager
	 */
	@Required
	@Autowired
	public void setAuthenticationManager(AuthenticationManager authManager) {
		this.authManager = authManager;
	}
	
	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Required
	@Autowired
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
	@Override
	protected void printHeader( String action ) {
		// EMPTY
	}
	
	@Override
	protected boolean validateAndPrepare( String action ) {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar(	"TUTORIAL_ID", this.config.get("ARTICLE_TUTORIAL"),
					"FAQ_ID", this.config.get("ARTICLE_FAQ"),
					"URL", this.config.get("URL") );
							
		return true;	
	}

	/**
	 * Ermoeglicht das generieren eines neuen Passworts und anschliessenden
	 * zumailens dessen.
	 * @urlparam String username der Benutzername des Accounts
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void passwordLostAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("username");
		String username = getString("username");

		if( "".equals(username) ) {
			t.setVar("show.passwordlost",1);
		}
		else {
			//username = db.prepareString(username);
			//SQLResultRow row = db.first("SELECT email,id FROM users WHERE un='",username,"'");
			//String email = row.getString("email");
			//int loguserid = row.getInt("id");
			
			User user = (User)db.createQuery("from User where un = :username")
							.setString("username", username)
							.uniqueResult();
			if( user != null)
			{
				if( !"".equals(user.getEmail()) ) {
					String password = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE));
					String enc_pw = Common.md5(password);
					
					user.setPassword(enc_pw);
					//db.update("UPDATE users SET passwort='",enc_pw,"' WHERE un='",username,"'");
	
					String subject = "Neues Passwort fuer Drifting Souls 2";
					
					String message = this.config.get("PWNEW_EMAIL").replace("{username}", getString("username"));
					message = message.replace("{password}", password);
					message = message.replace("{date}", Common.date("H:i j.m.Y"));
					
					Common.mail( user.getEmail(), subject, message );
					
					Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+user.getId()+") <"+username+"> Passwortanforderung von Browser <"+getRequest().getUserAgent()+">\n");
			
					t.setVar(	"show.passwordlost.msg.ok", 1,
								"passwordlost.email", user.getEmail() );
				}
				else {
					Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+user.getId()+") <"+username+"> Passwortanforderung von Browser <"+getRequest().getUserAgent()+">\n");
	
					t.setVar("show.passwordlost.msg.error",1);
				}
			}
			else
			{
				Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> <"+username+"> Passwortanforderung von Browser <"+getRequest().getUserAgent()+">\n");
			}
		}
	}


	/**
	 * Zeigt die Banner Seite an an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void bannerAction() {
		getTemplateEngine().setVar("show.banner",1);
	}

	/**
	 * Zeigt die AGB an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void infosAgbAction() {
		getTemplateEngine().setVar("show.agb",1);
	}

	/**
	 * Zeigt das Impressum an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void impressumAction() {
		getTemplateEngine().setVar("show.impressum",1);
	}
	
	private static class StartLocations {
		final int systemID;
		@SuppressWarnings("unused")
		final int orderLocationID;
		final HashMap<Integer,StartLocation> minSysDistance;

		StartLocations(int systemID, int orderLocationID, HashMap<Integer,StartLocation> minSysDistance) {
			this.systemID = systemID;
			this.orderLocationID = orderLocationID;
			this.minSysDistance = minSysDistance;
		}
	}
	
	private static class StartLocation {
		int orderLocationID;
		int distance;

		StartLocation(int orderLocationID, int distance) {
			this.orderLocationID = orderLocationID;
			this.distance = distance;
		}
	}
	
	private StartLocations getStartLocation() {
		org.hibernate.Session db = getDB();
		
		int systemID = 0;
		int orderLocationID = 0;
		int mindistance = 99999;
		HashMap<Integer,StartLocation> minsysdistance = new HashMap<Integer,StartLocation>();
		
		List<?> systems = db.createQuery("from StarSystem order by id asc").list();
		for( Iterator<?> iter = systems.iterator(); iter.hasNext(); )
		{
			StarSystem system = (StarSystem)iter.next();
			Location[] locations = system.getOrderLocations();
			
			for( int i=0; i < locations.length; i++ ) {
				int dist = 0;
				int count = 0;
				Iterator<?> distiter = db.createQuery("SELECT sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y)) FROM Base WHERE owner = 0 AND system = :system AND klasse = 1 ORDER BY sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y))")
											.setInteger("x", locations[i].getX())
											.setInteger("y", locations[i].getY())
											.setInteger("system", system.getID())
											.setMaxResults(15)
											.iterate();
				
				/*SQLQuery adist = db.query("SELECT sqrt((",locations[i].getX(),"-x)*(",locations[i].getX(),"-x)+(",locations[i].getY(),"-y)*(",locations[i].getY(),"-y)) distance FROM bases WHERE owner=0 AND system='",system.getID(),"' AND klasse=1 ORDER BY distance LIMIT 15");
				while( adist.next() ) {
					dist += adist.getInt("distance");
					count++;
				}
				adist.free();*/
				
				while(distiter.hasNext())
				{
					dist += (Double)distiter.next();
					count++;
				}
				
				if( count < 15 ) {
					continue;
				}
				
				if( !minsysdistance.containsKey(system.getID()) || (minsysdistance.get(system.getID()).distance > dist) ) {
					minsysdistance.put(system.getID(),  new StartLocation(i, dist));
					
					if( mindistance > dist ) {
						mindistance = dist;
						systemID = system.getID();
						orderLocationID = i;
					}
				}
			}
		}
		return new StartLocations(systemID, orderLocationID, minsysdistance);
	}
	
	private boolean register( String username, String email, int race, int system, String key, ConfigValue keys) {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		
		if( "".equals(username) || "".equals(email) ) {
			return false;
		}
		
	/*	String uname = db.prepareString(username);
		SQLResultRow auser = db.first("SELECT * FROM users WHERE un='",uname,"'");
		SQLResultRow auser2 = db.first("SELECT * FROM users WHERE email='",db.prepareString(email),"'");
	 */
		User user1 = (User)db.createQuery("from User where un = :username")
						.setString("username", username)
						.setMaxResults(1)
						.uniqueResult();
		User user2 = (User)db.createQuery("from User where email = :email")
						.setString("email", email)
						.setMaxResults(1)
						.uniqueResult();
		
		if( user1 != null ) {
			t.setVar("show.register.msg.wrongname",1);
			return false;
		}
		if( user2 != null ) {
			t.setVar("show.register.msg.wrongemail",1);
			return false;
		}
		if( !Rassen.get().rasse(race).isPlayable() ) {
			t.setVar("show.register.msg.wrongrace",1);
			return false;
		}
		
		boolean needkey = false;
		if( keys.getValue().indexOf('*') == -1 ) {
			needkey = true;
		}
		
		if( needkey && (keys.getValue().indexOf("<"+key+">") == -1) ) {
			t.setVar("show.register.msg.wrongkey", 1);
			return false;	
		}
		
		StarSystem thissystem = (StarSystem)db.get(StarSystem.class, system);
		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem").list());
		
		if( (system == 0) || (thissystem == null) || (thissystem.getOrderLocations().length == 0) ) {
			t.setBlock("_PORTAL", "register.systems.listitem", "register.systems.list");
			t.setBlock("_PORTAL", "register.systemdesc.listitem", "register.systemdesc.list");
			
			StartLocations locations = getStartLocation();
			t.setVar(	"register.system.id", locations.systemID,
						"register.system.name", thissystem.getName(),
						"show.register.choosesystem", 1 );
		
			for( StarSystem sys : systems ) {
				if( (sys.getOrderLocations().length > 0) && locations.minSysDistance.containsKey(sys.getID()) ) {
					t.setVar(	"system.id", sys.getID(),
								"system.name", sys.getName(),
								"system.selected", (sys.getID() == locations.systemID),
								"system.description", Common._text(sys.getDescription()) );
										
					t.parse("register.systems.list", "register.systems.listitem", true);
					t.parse("register.systemdesc.list", "register.systemdesc.listitem", true);
				}
			}
			
			return true;
		}
		
		if( needkey ) {
	 		String[] keylist = keys.getValue().replace("\r\n", "\n").split("\n");
		 	HashMap<String,String> parameters = new HashMap<String,String>();
		 	int pos = 0;
		 	for( pos=0; pos < keylist.length; pos++ ) {
	 			if( keylist[pos].indexOf("<"+key+">") == 0 ) {
	 				if( keylist[pos].length() > ("<"+key+">").length() ) {
		 				String[] params = keylist[pos].substring(("<"+key+">").length()).split(",");
		 						
		 				for( String param : params ) {
		 					String[] aParam = param.split("="); 
		 					parameters.put(aParam[0], aParam[1]);
		 				}
	 				}
	 						
	 				break;	
	 			}	
	 		}
	 		
	 		if( parameters.containsKey("race") && (Integer.parseInt(parameters.get("race")) != race) ) {
	 			t.setVar("show.register.msg.wrongrace",1);
				return false;
	 		}
	 		String[] newKeyList = new String[keylist.length-1];
	 		if( pos != 0 ) {
	 			java.lang.System.arraycopy(keylist,0, newKeyList, 0, pos);
	 		}
	 		if( pos != keylist.length - 1 ) {
	 			java.lang.System.arraycopy(keylist,pos+1, newKeyList, pos, keylist.length-pos-1);
	 		}
	 		
	 		keys.setValue(Common.implode("\n",newKeyList));
	 	}
		
		String password = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE));
		String enc_pw = Common.md5(password);

		int maxid = (Integer)db.createQuery("SELECT max(id) FROM User").iterate().next();
		int newid = maxid+1;

		int ticks = getContext().get(ContextCommon.class).getTick();

		String history = "Kolonistenlizenz erworben am "+Common.getIngameTime(ticks)+" ["+Common.date("d.m.Y H:i:s")+"]";		
		
		User newuser = new User(username, enc_pw, race, history, new Cargo(), email);
		
		// Startgeld festlegen
		
		newuser.setKonto(BigInteger.valueOf(50000));
		
		// Schiffe erstellen
	 	StartLocations locations = getStartLocation();
	 	Location[] orderlocs = thissystem.getOrderLocations();
	 	Location orderloc = orderlocs[locations.minSysDistance.get(system).orderLocationID];
	 	
	 	String[] baselayoutStr = this.config.get("REGISTER_BASELAYOUT").split(",");
	 	Integer[] activebuildings = new Integer[baselayoutStr.length];
	 	Integer[] baselayout = new Integer[baselayoutStr.length];
	 	int bewohner = 0;
	 	int arbeiter = 0;
	 	
	 	for( int i=0; i < baselayoutStr.length; i++ ) {
	 		baselayout[i] = Integer.parseInt(baselayoutStr[i]);
	 		
	 		if( baselayout[i] != 0 ) {
	 			activebuildings[i] = 1;
	 			Building building = Building.getBuilding(baselayout[i]);
	 			bewohner += building.getBewohner();
	 			arbeiter += building.getArbeiter();
	 		}
	 		else {
	 			activebuildings[i] = 0;
	 		}
	 	}

	 	Base base = (Base)db.createQuery("from Base where klasse=1 and owner=0 and system=? order by sqrt((?-x)*(?-x)+(?-y)*(?-y)) ")
	 		.setInteger(0, system)
	 		.setInteger(1, orderloc.getX())
	 		.setInteger(2, orderloc.getX())
	 		.setInteger(3, orderloc.getY())
	 		.setInteger(4, orderloc.getY())
	 		.setMaxResults(1)
	 		.uniqueResult();
	 	
	 	// Alte Gebaeude entfernen
	 	Integer[] bebauung = base.getBebauung();
		for( int i=0; i < bebauung.length; i++ ) {
			if( bebauung[i] == 0 ) {
				continue;
			}
			
			Building building = Building.getBuilding(bebauung[i]);
			building.cleanup(getContext(), base, bebauung[i]);
		}
	 	
		BaseType basetype = (BaseType)db.get(BaseType.class, 1);
	 	//User newuser = (User)getDB().get(User.class, newid);
	 	
	 	base.setEnergy(base.getMaxEnergy());
	 	base.setOwner(newuser);
	 	base.setBebauung(baselayout);
	 	base.setActive(activebuildings);
	 	base.setArbeiter(arbeiter);
	 	base.setBewohner(bewohner);
	 	base.setWidth(basetype.getWidth());
	 	base.setHeight(basetype.getHeight());
	 	base.setMaxCargo(basetype.getCargo());
	 	base.setCargo(new Cargo(Cargo.Type.AUTO, this.config.get("REGISTER_BASECARGO")));
	 	base.setCore(0);
	 	base.setUnits(new UnitCargo());
	 	base.setCoreActive(false);
	 	base.setAutoGTUActs(new ArrayList<AutoGTUAction>());
	 	
	 	db.createQuery("update Offizier set userid=? where dest in (?, ?)")
	 		.setInteger(0, base.getOwner().getId())
	 		.setString(1, "b "+base.getId())
	 		.setString(2, "t "+base.getId())
	 		.executeUpdate();
	 	 	
	 	for( int i=0; i < baselayout.length; i++ ) {
			if( baselayout[i] > 0 ) {
				Building building = Building.getBuilding(baselayout[i]);
				building.build(base, baselayout[i]);	 			
			}
		}
	 	
	 	Nebel nebel = (Nebel)db.createQuery("from Nebel where loc.system=? and type<3 order by sqrt((?-loc.x)*(?-loc.x)+(?-loc.y)*(?-loc.y))*(mod(type+1,3)+1)*3")
	 		.setInteger(0, system)
	 		.setInteger(1, base.getX())
	 		.setInteger(2, base.getX())
	 		.setInteger(3, base.getY())
	 		.setInteger(4, base.getY())
	 		.setMaxResults(1)
	 		.uniqueResult();

	 	if( race == 1 ) {		
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER", base.getLocation(), newid);
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER_TANKER", nebel.getLocation(), newid);
		} 
		else {			
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER", base.getLocation(), newid);
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER_TANKER", nebel.getLocation(), newid);
		}
	 	
		//Willkommens-PM versenden
	 	User source = (User)db.get(User.class, this.config.getInt("REGISTER_PM_SENDER"));
		PM.send( source, newid, "Willkommen bei Drifting Souls 2", 
				this.config.get("REGISTER_PM"));
		
		t.setVar( "show.register.msg.ok", 1,
					"register.newid", newid );
							
		Common.copyFile(this.config.get("ABSOLUTE_PATH")+"data/logos/user/0.gif",
				this.config.get("ABSOLUTE_PATH")+"data/logos/user/"+newid+".gif");

		String message = this.config.get("REGISTER_EMAIL");
		message = message.replace("{username}", username);
		message = message.replace("{password}", password);
		message = message.replace("{date}", Common.date("H:i j.m.Y"));
			
		Common.mail(email, "Anmeldung bei Drifting Souls 2", message);

		return true;
	}
	
	/**
	 * Registriert einen neuen Spieler. Falls keine Daten eingegeben wurden, 
	 * wird die GUI zum registrieren angezeigt.
	 * @urlparam String username der Benutzername des Accounts
	 * @urlparam Integer race Die Rasse des Accounts
	 * @urlparam String email Die Email-Adresse
	 * @urlparam String key Der Registrierungssschluessel
	 * @urlparam Integer Das Startsystem
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void registerAction() {
		Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		
		boolean showform = true;

		parameterString("username");
		parameterNumber("race");
		parameterString("email");
		parameterString("key");
		parameterNumber("system");
		
		String username = getString("username");
		int race = getInteger("race");
		String email = getString("email");
		String key = getString("key");
		int system = getInteger("system");
	
		ConfigValue disableregister = (ConfigValue)db.get(ConfigValue.class, "disableregister");
		if( !"".equals(disableregister.getValue()) ) {
			username = null;
			race = 0;
			email = null;
			showform = false;
		
			t.setVar(	"show.register.registerdisabled" , 1,
						"register.registerdisabled.msg" , Common._text(disableregister.getValue()) );
								
			return;
		}
		
		ConfigValue keys = (ConfigValue)db.get(ConfigValue.class, "keys");
		boolean needkey = false;
		if( keys.getValue().indexOf('*') == -1  ) {
			needkey = true;
		}		
		
		StarSystem thissystem = (StarSystem)db.get(StarSystem.class, system);
		
		t.setVar(	"register.username"		, username,
					"register.email"		, email,
					"register.needkey"		, needkey,
					"register.key"			, key,
					"register.race"			, race,
					"register.system.id"	, system,
					"register.system.name" 	, (thissystem != null  ? thissystem.getName() : "") );
		
		showform = !register(username, email, race, system, key, keys);
		
		if( showform ) {
			t.setBlock("_PORTAL","register.rassen.listitem","register.rassen.list");
			t.setBlock("_PORTAL","register.rassendesc.listitem","register.rassendesc.list");

			int first = -1;

			for( Rasse rasse : Rassen.get() ) {
				if( rasse.isPlayable() ) {
					t.setVar(	"rasse.id"			, rasse.getID(),
								"rasse.name"		, rasse.getName(),
								"rasse.selected"	, (first == -1 ? 1 : 0),
								"rasse.description"	, Common._text(rasse.getDescription()) );
					
					if( first == -1 ) {
						first = rasse.getID();
					}
					
					t.parse("register.rassen.list","register.rassen.listitem",true);
					t.parse("register.rassendesc.list","register.rassendesc.listitem",true);
				}
			}
			
			t.setVar(	"show.register"				, 1,
						"register.rassen.selected"	, first );
		}
	}
	
	/**
	 * Loggt einen Spieler ein. Falls keine Daten angegeben wurden, 
	 * wird die GUI zum einloggen angezeigt.
	 * @urlparam String username Der Benutzername
	 * @urlparam String password Das Passwort
	 * @urlparam Integer usegfxpak != 0, falls ein vorhandenes Grafikpak benutzt werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void loginAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterString("username");
		parameterString("password");
		parameterNumber("usegfxpak");
		parameterString("rememberme");
		
		String username = getString("username");
		String password = getString("password");
		int usegfxpak = getInteger("usegfxpak") != 0 ? 1 : 0;
		boolean rememberMe = Boolean.parseBoolean(getString("rememberme"));
		
		if( !username.isEmpty() && !password.isEmpty() ) {
			try {
				User user = (User)this.authManager.login(username, password, usegfxpak != 0, rememberMe);
				
				doLogin(user);
				
				return;
			}
			catch( LoginDisabledException e ) {
				t.setVar(	"show.login.logindisabled", 1,
							"login.logindisabled.msg", Common._text(e.getMessage()) );
				
				return;
			}
			catch( AccountInVacationModeException e ) {
				t.setVar(	
						"show.login.vacmode", 1,
						"login.vacmode.dauer", e.getDauer(),
						"login.vacmode.username", username,
						"login.vacmode.password", password);
				
				return;
			}
			catch( WrongPasswordException e ) {
				t.setVar( "show.msg.login.wrongpassword",1 );
			}
			catch( AccountDisabledException e ) {
				t.setVar("show.login.msg.accdisabled",1);
			}
			catch( TickInProgressException e ) {
				t.setVar("show.login.msg.tick",1);
			}
			catch( AuthenticationException e ) {
				// EMPTY
			}
		}

		t.setVar(	"show.login", 1,
					"show.overview", 1,
					"show.news", 1,
					"login.username", username );
	}

	private void doLogin(User user) {
		TemplateEngine t = getTemplateEngine();

		t.setVar( "show.login.msg.ok", 1 );
		
		// Ueberpruefen ob das gfxpak noch aktuell ist
		if( !BasicUser.getDefaultImagePath().equals(user.getImagePath()) ) {
			t.setVar(	"login.checkgfxpak", 1,
						"login.checkgfxpak.path", user.getUserImagePath() );
		}
				
		getResponse().redirectTo(Common.buildUrl("default", "module", "main"));
		return;
	}
	
	/**
	 * Ermoeglicht das Absenden einer Anfrage zur Deaktivierung des Vac-Modus.
	 * @urlparam String asess Die Session-ID
	 * @urlparam String reason Der Grund fuer eine vorzeitige Deaktivierung
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void loginVacmodeDeakAction() {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		
		parameterString("username");
		parameterString("pw");
		String username = getString("username");
		String password = getString("pw");
		
		User user = (User)db.createQuery("from User where un=:username")
			.setString("username", username)
			.uniqueResult();
	
		String encPw = Common.md5(password);
		
		if( user == null || !encPw.equals(user.getPassword()) ) {
			t.setVar("show.login.vacmode.msg.accerror",1);
			return;
		}
		
		parameterString("reason");
		String reason = getString("reason");
		
		PM.sendToAdmins(user, "VACMODE-DEAK", 
				"[VACMODE-DEAK]\nMY ID: "+user.getId()+"\nREASON:\n"+reason, 0);
		
		t.setVar("show.login.vacmode.msg.send",1);
	}
	
	/**
	 * Allows players, which are remembered by ds to login directly.
	 */
	@Action(ActionType.DEFAULT)
	public void reloginAction()
	{
		getResponse().redirectTo(Common.buildUrl("default", "module", "main"));
		return;
	}
	
	/**
	 * Zeigt die News an.
	 * @urlparam Integer archiv != 0, falls alte News angezeigt werden sollen
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("archiv");
		int archiv = getInteger("archiv");
		
		if(this.authManager.isRemembered())
		{
			t.setVar("is.logged.in", 1);
		}
		
		t.setVar(
				"show.news",	1,
				"show.overview",	archiv == 0,
				"show.news.archiv", archiv );
		t.setBlock("_PORTAL","news.listitem","news.list");

		List<NewsEntry> allnews = Common.cast(db.createQuery("FROM NewsEntry ORDER BY date DESC")
												.setMaxResults(archiv != 0 ? 100 : 5)
												.list());
		for(NewsEntry news : allnews ) {
			t.setVar(	"news.date", Common.date("d.m.Y H:i", news.getDate()),
						"news.title", news.getTitle(),
						"news.author", news.getAuthor(),
						"news.text", Common._text(news.getNewsText()) );
			t.parse("news.list","news.listitem",true);
		}
	}
}
