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
package net.driftingsouls.ds2.server.framework;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringUtils;


/**
 * Die Benutzerklasse von DS
 * @author Christopher Jung
 *
 */
// TODO: Falls ein Benutzer in der Datenbank nicht existiert, sollte dies einen Fehler ausloesen
// und nicht zu einem leeren Benutzer mit der ID 0 fuehren
public class User implements Loggable {
	/**
	 * Geldtransfer - Der Transfer ist manuell vom Spieler durchgefuerht worden
	 */
	public static final int TRANSFER_NORMAL = 0;
	/**
	 * Geldtransfer - Der Transfer ist in direkter Folge einer Spieleraktion ausgefuehrt worden
	 */
	public static final int TRANSFER_SEMIAUTO = 1;
	/**
	 * Geldtransfer - Der Transfer ist automatisch erfolgt
	 */
	public static final int TRANSFER_AUTO = 2;
	
	/**
	 * Der Spieler taucht in der Spielerliste nicht auf
	 */
	public static final String FLAG_HIDE = "hide";
	/**
	 * Der Spieler kann auch in entmilitarisierte Systeme mit Militaerschiffen springen
	 */
	public static final String FLAG_MILITARY_JUMPS = "miljumps";
	/**
	 * Der Spieler kann alle Schlachten sehen
	 */
	public static final String FLAG_VIEW_BATTLES = "viewbattles";
	/**
	 * Der Spieler hat Zugriff auf das NPC-Menue
	 */
	public static final String FLAG_ORDER_MENU = "ordermenu";
	/**
	 * Der Spieler kann auch NPC-Systeme sehen
	 */
	public static final String FLAG_VIEW_SYSTEMS = "viewsystems";
	/**
	 * Der Spieler kann sowohl Admin- als auch NPC-Systeme sehen 
	 */
	public static final String FLAG_VIEW_ALL_SYSTEMS = "viewallsystems";
	/**
	 * Der Spieler kann Schiffsscripte benutzen
	 */
	public static final String FLAG_EXEC_NOTES = "execnotes";
	/**
	 * Es findet keine Kopplung von IP und Session-ID statt
	 */
	public static final String FLAG_DISABLE_IP_SESSIONS = "NO_IP_SESS";
	/**
	 * Es findet kein Autologout in Folge von Inaktivitaet statt
	 */
	public static final String FLAG_DISABLE_AUTO_LOGOUT = "NO_AUTOLOGOUT";
	/**
	 * Der Spieler kann Questschlachten leiten (und uebernehmen)
	 */
	public static final String FLAG_QUEST_BATTLES = "questbattles";
	/**
	 * Der Spieler sieht den Debug-Output des Scriptparsers
	 */
	public static final String FLAG_SCRIPT_DEBUGGING = "scriptdebug";
	/**
	 * Dem Spieler koennen keine Schiffe uebergeben werden
	 */
	public static final String FLAG_NO_SHIP_CONSIGN = "noshipconsign";
	/**
	 * Der Spieler ist von der Klicksperre befreit
	 */
	public static final String FLAG_NO_ACTION_BLOCKING = "noactionblocking";
	/**
	 * Der Spieler kann mit Schiffen jederzeit ins System 99 springen
	 */
	public static final String FLAG_NPC_ISLAND = "npc_island";
	/**
	 * Sprungpunkte sind fuer den Spieler immer passierbar
	 */
	public static final String FLAG_NO_JUMPNODE_BLOCK = "nojnblock";
	/**
	 * Der Spieler kann jedes Schiff, egal welcher Besitzer und wie Gross andocken
	 */
	public static final String FLAG_SUPER_DOCK = "superdock";
	/**
	 * Der Spieler hat Moderatorrechte im Handel
	 */
	public static final String FLAG_MODERATOR_HANDEL = "moderator_handel";
	/**
	 * Der Spieler ist ein Noob
	 */
	public static final String FLAG_NOOB = "noob";
	
	/**
	 * Die Arten von Beziehungen zwischen zwei Spielern
	 * @author Christopher Jung
	 *
	 */
	public enum Relation {
		/**
		 * Neutral
		 */
		NEUTRAL,	// 0
		/**
		 * Feindlich
		 */
		ENEMY,		// 1
		/**
		 * Freundlich
		 */
		FRIEND;		// 2
	}
	
	/**
	 * Klasse, welche die Beziehungen eines Spielers zu anderen
	 * Spielern enthaelt
	 * @author Christopher Jung
	 *
	 */
	public class Relations {
		/**
		 * Die Beziehungen des Spielers zu anderen Spielern.
		 * Schluessel ist die Spieler-ID
		 */
		public Map<Integer,Relation> toOther = new HashMap<Integer,Relation>();
		/**
		 * Die Beziehungen von anderen Spielern zum Spieler selbst.
		 * Schluessel ist die Spieler-ID
		 */
		public Map<Integer,Relation> fromOther = new HashMap<Integer,Relation>();
		
		protected Relations() {
			// EMPTY
		}
	}
	
	private static String[] dbfields = { "id", "un", "name", "passwort", "race", "inakt", "signup",
			"history", "medals", "rang", "ally", "konto", "cargo", "nstat", "email", "log_fail",
			"accesslevel", "npcpunkte", "nickname", "allyposten", "gtudropzone", "npcorderloc",
			"imgpath", "flagschiff", "disabled", "flags", "vaccount", "wait4vac", "wonBattles",
			"lostBattles", "lostShips", "destroyedShips", "knownItems" };
	
	private int id;
	private List<String> preloadedValues = new ArrayList<String>();
	private SQLResultRow data;
	private Context context;
	private int attachedID;
	private SQLResultRow attachedData;
	
	private static String defaultImagePath = null;
	
	private UserFlagschiffLocation flagschiff = null;
	private String userImagePath = null;
	
	/**
	 * Konstruktor
	 * @param c Der aktuelle Kontext
	 * @param id Die ID des Spielers
	 * @param sessiondata Sessiondaten
	 */
	public User( Context c, int id, SQLResultRow sessiondata ) {
		context = c;
		attachedID = 0;
		attachedData = null;

		this.id = id;
		
		data = c.getDatabase().first("SELECT * FROM users WHERE id='",id,"'");
		if( data.isEmpty() ) {
			LOG.error("FAILED TO LOAD USER: "+id);
			data.put("id", 0);
		}
		else {
			userImagePath = data.getString("imgpath");
		}
		
		preloadedValues.addAll(Arrays.asList(dbfields));

		if( (sessiondata != null) && (sessiondata.getInt("usegfxpak") == 0) ) {
			data.put("imgpath", getDefaultImagePath(context.getDatabase()));
		}
		
		context.cacheUser( this );
	}
	
	/**
	 * Konstruktor
	 * @param c Der aktuelle Kontext
	 * @param id Die ID des Spielers
	 */
	public User( Context c, int id ) {
		this( c, id, (SQLResultRow)null );
	}

	protected User( Context c, SQLResultRow row ) {
		context = c;
		attachedID = 0;
		attachedData = null;
		data = row;
		
		if( !data.containsKey("id") ) {
			throw new RuntimeException("Feld 'id' nicht vorhanden, obwohl dies zwingend notwendig ist!");
		}
		id = data.getInt("id");
		
		for( int i=0; i < dbfields.length; i++ ) {
			if( data.containsKey(dbfields[i]) ) {
				preloadedValues.add(dbfields[i]);
			}
		}
		
		context.cacheUser( this );
	}
	
	/**
	 * Laedt einige Parameter aus der Datenbank
	 * @param preload Die zu ladenden Datenbankspalten
	 */
	public void preloadValues(String[] preload) {
		if( preload.length == 0 ) {
			return;
		}
		List<String> preloadList = new ArrayList<String>(Arrays.asList(preload));

		for( int i=0; i < preloadList.size(); i++ ) {
			if( preloadedValues.contains(preloadList.get(i)) ) {
				preloadList.remove(i);
				i--;
				continue;
			}
			if( !Common.inArray(preloadList.get(i),dbfields) ) {
				context.addError("Fehler (User:"+id+"): Der Wertetyp '"+preloadList.get(i)+"' ist un&uuml;ltig");
				preloadList.remove(i);
				i--;
			}	
		}
		if( preloadList.size() == 0 ) {
			return;
		}
		String sqlPreload = Common.implode(",",preloadList);
		
		data.putAll(context.getDatabase().first("SELECT ",sqlPreload," FROM users WHERE id='",id,"'"));
			
		preloadedValues.addAll(preloadList);
	}
	
	/**
	 * Liefert die User-ID des User-Objekts zurueck
	 * 
	 * @return Die User-ID
	 */
	public int getID() {
		return data.getInt("id");
	}
	
	/**
	 * Koppelt den Benutzer temporaer an einen anderen. Dadurch werden AccessLevel und Flags
	 * des angegebenen Benutzers verwendet
	 * @param uid Die ID des Benutzers, der temporaer an diesen gekoppelt werden soll
	 */
	public void attachToUser( int uid ) {
		attachedID = uid;
		
		attachedData = context.getDatabase().first("SELECT ",Common.implode(",",preloadedValues)," FROM users WHERE id='",uid,"'");
	}
	
	/**
	 * Macht alle geladenen Benutzereigenschaften dem Templateengine bekannt.
	 * Die daraus resultierenden Template-Variablen haben die Form "user."+Datenbankname.
	 * Die Eigenschaft Wait4Vacation, welche den Datenbanknamen "wait4vac" hat, wuerde sich
	 * somit in der Template-Variablen "user.wait4vac" wiederfinden
	 * 
	 * @param templateEngine Das Template-Engine, in dem die Variablen gesetzt werden sollen
	 */
	public void setTemplateVars(TemplateEngine templateEngine) {
		setTemplateVars(templateEngine, "user");
	}
	
	/**
	 * Macht alle geladenen Benutzereigenschaften dem Templateengine bekannt.
	 * Die daraus resultierenden Template-Variablen haben die Form Prefix+"."+Datenbankname.
	 * Die Eigenschaft Wait4Vacation, welche den Datenbanknamen "wait4vac" hat, wuerde sich, beim 
	 * Prefix "activeuser", somit in der Template-Variablen "activeuser.wait4vac" wiederfinden
	 * 
	 * @param templateEngine Das Template-Engine, in dem die Variablen gesetzt werden sollen
	 * @param prefix Der fuer die Template-Variablen zu verwendende Prefix
	 */
	public void setTemplateVars(TemplateEngine templateEngine, String prefix) {
		String pre = prefix+".";
		for( String val : preloadedValues ) {
			templateEngine.set_var(pre+val, data.get(val));
		}
	}
	
	/**
	 * Liefert einen Profile-Link zu den Benutzer zurueck (als HTML).
	 * Als CSS-Klasse fuer den Link wird die angegebene Klasse verwendet
	 * @param username Der anzuzeigende Spielername
	 * @return Der Profile-Link
	 */
	public String getProfileLink(String username) {
		if( username == null || username.equals("") ) {
			checkAndLoad("name");
			username = Common._title(data.getString("name"));
		}
		
		return "<a class=\"profile\" href=\""+Common.buildUrl(ContextMap.getContext(), "default", "module", "userprofile", "user", this.id)+"\">"+username+"</a>";
	}
	
	/**
	 * Liefert einen vollstaendigen Profile-Link zu den Benutzer zurueck (als HTML).
	 * Der Linkt enthaelt einen &lt;a&gt;-Tag sowie den Benutzernamen als HTML
	 * @return Der Profile-Link
	 */
	public String getProfileLink() {
		return getProfileLink("");
	}

	/**
	 * Liefert den Standard-Image-Path zurueck
	 * @param db Eine Datenbankverbindung
	 * @return Der Standard-Image-Path
	 */
	public static String getDefaultImagePath(Database db) {
		if( defaultImagePath == null ) {
			defaultImagePath = db.first("SHOW FIELDS FROM users LIKE 'imgpath'").getString("Default");
		}
		return defaultImagePath;
	}

	/**
	 * Liefert den Image-Path dieses Benutzers zurueck.
	 * 
	 * @return Der Image-Path des Benutzers
	 */
	public String getUserImagePath() {
		if( userImagePath == null ) {
			userImagePath = context.getDatabase().first("SELECT imgpath FROM users WHERE id='",id,"'").getString("imgpath");
		}
		return userImagePath;
	}
	
	/**
	 * Setzt den Spieler-Cargo auf den angegebenen Cargo-String in der Datenbank.
	 * Um inkonsistenzen zu vermeiden wird zudem geprueft, ob der urspruengliche
	 * Cargo-String noch aktuell ist.
	 * @param cargo Der neue Cargo-String
	 * @param oldString Der urspruengliche Cargo-String
	 */
	public void setCargo(String cargo, String oldString) {
		Database db = context.getDatabase();
		
		if( oldString != null ) {
			db.tUpdate(1, "UPDATE users SET cargo='",cargo,"' WHERE id='",id,"' AND cargo='",oldString,"'");	
		}
		else {
			db.tUpdate(1, "UPDATE users SET cargo='",cargo,"' WHERE id='",id,"'");	
		}
	}
	
	/**
	 * Setzt den Spieler-Cargo auf den angegebenen Cargo-String in der Datenbank
	 * @param cargo Der Cargo-String
	 */
	public void setCargo(String cargo) {
		setCargo(cargo, null);
	}
	
	/**
	 * Ueberprueft, ob ein Flag fuer den Benutzer aktiv ist.
	 * @param flag Das zu ueberpruefende Flag
	 * @return <code>true</code>, falls das Flag aktiv ist
	 */
	public boolean hasFlag( String flag ) {
		checkAndLoad("flags");
		
		if( data.getString("flags").indexOf(flag) > -1 ) {
			return true;
		}

		if( (attachedID != 0) && (attachedData.getString("flags").indexOf(flag) > -1) ) {
			return true;
		}
			
		return false;
	}
	
	/**
	 * Setzt ein Flag fuer den User entweder auf aktiviert (<code>true</code>)
	 * oder auf deaktiviert (<code>false</code>)
	 * @param flag Das zu setzende Flag
	 * @param on true, falls es aktiviert werden soll
	 */
	public void setFlag( String flag, boolean on ) {
		checkAndLoad("flags");
		String flagstring = "";
		if( on ) {
			if( !"".equals(data.getString("flags")) ) {
				flagstring = data.getString("flags")+" "+flag;
			}
			else {
				flagstring = flag;
			}
		}
		else {
			StringBuilder newflags = new StringBuilder();
		
			String[] flags = StringUtils.split(data.getString("flags"),' ');
			for( String aflag : flags ) {
				if( !aflag.equals(flag) ) {
					if( newflags.length() > 0 ) {
						newflags.append(" ");
					}
					newflags.append(aflag);
				}
			}
			flagstring = newflags.toString();
		}
		
		context.getDatabase().tUpdate(1,"UPDATE users SET flags='",flagstring,"' WHERE id='",id,"' AND flags='",data.getString("flags"),"'");
		data.put("flags", flagstring);
	}
	
	/**
	 * Aktiviert ein Flag fuer den User
	 * @param flag Das zu aktivierende Flag
	 */
	public void setFlag( String flag ) {
		setFlag( flag, true );
	}
	
	private int flagschiffSpace = -1;
	
	/**
	 * Stellt fest, ob noch Platz fuer ein Flagschiff vorhanden ist
	 * 
	 * @return true, falls noch Platz vorhanden ist
	 */
	public boolean hasFlagschiffSpace() {
		Database db = context.getDatabase();
		
		checkAndLoad("flagschiff");
		
		if( flagschiffSpace > -1 ) {
			if( flagschiffSpace > 0 ) {
				return false;
			}	
			return true;
		}
		
		if( data.getInt("flagschiff") == 0 ) {
			SQLResultRow fs = db.first("SELECT flagschiff FROM werften t1,ships t2 WHERE t2.id>0 AND t1.flagschiff=1 AND t1.shipid=t2.id AND t2.owner='",this.id,"'");
			if( fs.isEmpty() ) {
				fs = db.first("SELECT flagschiff FROM werften t1,bases t2 WHERE t1.flagschiff=1 AND t1.col=t2.id AND t2.owner='",this.id,"'");
				if( fs.isEmpty() ) {
					flagschiffSpace = 0;
				}
				else {
					flagschiffSpace = fs.getBoolean("flagschiff") ? 1 : 0;
				}
			}
			else {
				flagschiffSpace = fs.getBoolean("flagschiff") ? 1 : 0;
			}
		}
		else {
			flagschiffSpace = 1;
		}
	
		if( flagschiffSpace > 0 ) {
			return false;
		}
		return true;
	}
	
	/**
	 * Liefert den Aufenthaltsort des Flagschiffs dieses Spielers.
	 * Der Typ Aufenthaltsort kann entweder ein Schiff (normal), eine Basiswerft
	 * oder eine Schiffswerft sein (in beiden Faellen wird das Schiff noch gebaut)
	 * 
	 * @return Infos zum Aufenthaltsort
	 */
	public UserFlagschiffLocation getFlagschiff() {
		Database db = context.getDatabase();
		
		checkAndLoad("flagschiff");

		if( flagschiff != null ) {
			return (UserFlagschiffLocation)flagschiff.clone();	
		}

		if( data.getInt("flagschiff") == 0 ) {
			SQLResultRow bFlagschiff = db.first("SELECT t2.id,t1.flagschiff FROM werften t1,ships t2 WHERE t2.id>0 AND t1.flagschiff=1 AND t1.shipid=t2.id AND t2.owner='",id,"'");
			if( bFlagschiff.isEmpty() ) {
				bFlagschiff = db.first("SELECT t2.id,t1.flagschiff FROM werften t1,bases t2 WHERE t1.flagschiff=1 AND t1.col=t2.id AND t2.owner='",id,"'");
				if( !bFlagschiff.isEmpty() ) {
					flagschiff = new UserFlagschiffLocation(UserFlagschiffLocation.Type.WERFT_BASE, bFlagschiff.getInt("id"));
				}
			}
			else {
				flagschiff = new UserFlagschiffLocation(UserFlagschiffLocation.Type.WERFT_SHIP, bFlagschiff.getInt("id"));
			}
		}
		else {
			flagschiff = new UserFlagschiffLocation(UserFlagschiffLocation.Type.SHIP, data.getInt("flagschiff"));
		}
	
		if( flagschiff != null ) {
			return (UserFlagschiffLocation)flagschiff.clone();
		}
		return null;
	}
	
	/**
	 * Setzt die Schiffs-ID des Flagschiffs. Falls diese 0 ist, besitzt der Spieler kein Flagschiff mehr
	 * 
	 * @param shipid Die Schiffs-ID des Flagschiffs
	 */
	public void setFlagschiff(int shipid) {
		checkAndLoad("flagschiff");
		context.getDatabase().tUpdate(1, "UPDATE users SET flagschiff='",shipid,"' WHERE id='",id,"' AND flagschiff='",data.get("flagschiff"),"'");
		data.put("flagschiff", shipid);
	}
	
	/**
	 * Liefert den Wert eines User-Values zurueck.
	 * User-Values sind die Eintraege, welche sich in der Tabelle user_values befinden
	 * 
	 * @param valuename Name des User-Values
	 * @return Wert des User-Values
	 */
	public String getUserValue( String valuename ) {
		Database db = context.getDatabase();
		
		PreparedQuery pq = db.prepare("SELECT `id`,`value` FROM user_values WHERE `user_id`IN ( ? ,0) AND `name`= ? ORDER BY abs(user_id) DESC LIMIT 1");
		SQLResultRow value = pq.pfirst(id, valuename);
		pq.close();

		if( value.isEmpty() ) {
			LOG.warn("Uservalue "+valuename+" hat keinen Defaultwert");
			return "";
		}
		return value.getString("value");
	}
	
	/**
	 * Setzt ein User-Value auf einen bestimmten Wert
	 * @see #getUserValue(String)
	 * 
	 * @param valuename Name des User-Values
	 * @param newvalue neuer Wert des User-Values
	 */
	public void setUserValue( String valuename, String newvalue ) {
		Database db = context.getDatabase();
		
		SQLResultRow valuen = db.prepare("SELECT `id`,`name` FROM user_values WHERE `user_id`= ? AND `name`= ?")
			.first(id, valuename);

		// Existiert noch kein Eintag?
		if( valuen.isEmpty() ) {
			PreparedQuery pq = db.prepare("INSERT INTO user_values (`user_id`,`name`,`value`) VALUES ( ?, ?, ?)");
			pq.update(id, valuename, newvalue);
			pq.close();
		}
		else {
			PreparedQuery pq = db.prepare("UPDATE user_values SET `value`= ? WHERE `user_id`= ? AND `name`= ?");
			pq.update(newvalue, id, valuename);
			pq.close();
		}
	}
	
	private Relations relations = null;
	
	/**
	 * Liefert alle Beziehungen vom Spieler selbst zu anderen Spielern und umgekehrt
	 * 
	 * @return Gibt ein Array zurueck. 
	 * 	Position 0 enthaelt alle Beziehungen von einem selbst ($userid => $beziehung).
	 * 	Position 1 enthaelt alle Beziehungen zu einem selbst ($userid => $beziehung).
	 * 
	 * 	Beziehungen zu Spieler 0 betreffen grundsaetzlich alle Spieler ohne eigene Regelung
	 */
	public Relations getRelations() {
		Database db = context.getDatabase();
		
		if( this.relations == null ) {
			Relations relations = new Relations();
			SQLQuery relation = db.query("SELECT * FROM user_relations WHERE user_id='",this.id,"' OR target_id='",this.id,"' OR (user_id!='",this.id,"' AND target_id='0') ORDER BY ABS(target_id) DESC");
			while( relation.next() ) {
				if( relation.getInt("user_id") == this.id ) {
					relations.toOther.put(relation.getInt("target_id"), Relation.values()[relation.getInt("status")]);	
				}
				else if( !relations.fromOther.containsKey(relation.getInt("user_id")) ) {
					relations.fromOther.put(relation.getInt("user_id"), Relation.values()[relation.getInt("status")]);
				}
			}
			relation.free();
			
			if( !relations.toOther.containsKey(0) ) {
				relations.toOther.put(0, Relation.NEUTRAL);	
			}
			
			relations.toOther.put(this.id, Relation.FRIEND);
			relations.fromOther.put(this.id, Relation.FRIEND);
			
			this.relations = relations;
		}

		Relations rel = new Relations();
		rel.fromOther.putAll(relations.fromOther);
		rel.toOther.putAll(relations.toOther);
		return rel;
	}
	
	/**
	 * Gibt den Status der Beziehung des Spielers zu einem anderen Spieler zurueck
	 * @param userid Die ID des anderen Spielers
	 * @return Der Status der Beziehungen zu dem anderen Spieler
	 */
	public Relation getRelation( int userid ) {
		Database db = context.getDatabase();
		
		if( userid == this.id ) {
			return Relation.FRIEND;
		}
		
		Relation rel = Relation.NEUTRAL;
		
		if( relations == null ) {
			SQLResultRow currelation = db.first("SELECT status FROM user_relations WHERE user_id='",id,"' AND target_id='",userid,"'");
			if( currelation.isEmpty() ) {
				currelation = db.first("SELECT status FROM user_relations WHERE user_id='",this.id,"' AND target_id='0'");
			}
		
			if( !currelation.isEmpty() ) {
				rel = Relation.values()[currelation.getInt("status")];	
			}
		}
		else {
			if( relations.toOther.containsKey(userid) ) {
				rel = relations.toOther.get(userid);	
			}
		}
		return rel;
	}
	
	/**
	 * Setzt die Beziehungen des Spielers zu einem anderen Spieler auf den angegebenen
	 * Wert
	 * @param userid Die ID des anderen Spielers
	 * @param relation Der neue Status der Beziehungen
	 */
	public void setRelation( int userid, Relation relation ) {
		Database db = context.getDatabase();
		
		if( userid == this.id ) {
			return;
		}
		
		SQLResultRow currelation = db.first("SELECT * FROM user_relations WHERE user_id='",this.id,"' AND target_id='",userid,"'");
		if( userid != 0 ) {
			if( (relation != Relation.FRIEND) && (getAlly() != 0) ) {
				User targetuser = context.createUserObject(userid);
				if( targetuser.getAlly() == getAlly() ) {
					LOG.warn("Versuch die allyinterne Beziehung von User "+id+" zu "+userid+" auf "+relation+" zu aendern", new Throwable());
					return;
				}
			}
			SQLResultRow defrelation = db.first("SELECT * FROM user_relations WHERE user_id='",this.id,"' AND target_id='0'");
		
			if( defrelation.isEmpty() ) {
				defrelation.put("user_id", this.id);
				defrelation.put("target_id", 0);
				defrelation.put("status", Relation.NEUTRAL.ordinal());	
			}
		
			if( relation.ordinal() == defrelation.getInt("status") ) {
				if( !currelation.isEmpty() && (currelation.getInt("user_id") != 0) ) {
					if( relations != null ) {
						relations.toOther.remove(userid);
					}
					
					db.update("DELETE FROM user_relations WHERE user_id='",this.id,"' AND target_id='",userid,"'");	
				}
			}
			else {
				if( relations != null ) {
					relations.toOther.put(userid, relation);
				}
				if( !currelation.isEmpty() ) {
					db.update("UPDATE user_relations SET status='",relation.ordinal(),"' WHERE user_id='",this.id,"' AND target_id='",userid,"'");	
				}	
				else {
					db.update("INSERT INTO user_relations (user_id,target_id,status) VALUES ('",this.id,"','",userid,"','",relation.ordinal(),"')");	
				}
			}
		}
		else {
			if( relation == Relation.NEUTRAL ) {
				if( relations != null ) {
					relations.toOther.put(0, Relation.NEUTRAL);
				}
				db.update("DELETE FROM user_relations WHERE user_id='",this.id,"' AND target_id='0'");
			}
			else {
				if( relations != null ) {
					relations.toOther.put(0, relation);
				}
				if( !currelation.isEmpty() ) {
					db.update("UPDATE user_relations SET status='",relation.ordinal(),"' WHERE user_id='",this.id,"' AND target_id='0'");	
				}	
				else {
					db.update("INSERT INTO user_relations (user_id,target_id,status) VALUES ('",this.id,"','0','",relation.ordinal(),"')");	
				}
			}
			db.update("DELETE FROM user_relations WHERE user_id='",this.id,"' AND status='",relation.ordinal(),"' AND target_id!='0'");
		}
	}
	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Benutzer zum aktuellen.
	 * Der Transfer kann entweder ein echter Transfer sein (Geld wird abgebucht) oder ein gefakter
	 * Transfer (kein Geld wird abgebucht sondern nur hinzugefuegt).
	 * Zudem faellt jeder Geldtransfer in eine von 3 Kategorien (automatisch, halbautomatisch und manuell).<br>
	 * Die Berechnung erfolgt intern auf Basis von <code>BigInteger</code>
	 * 
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 * @param faketransfer Handelt es sich um einen "gefakten" Geldtransfer (<code>true</code>)?
	 * @param transfertype Der Transfertyp (Kategorie)
	 * @see #TRANSFER_AUTO
	 * @see #TRANSFER_SEMIAUTO
	 * @see #TRANSFER_NORMAL
	 */
	public void transferMoneyFrom( int fromID, long count, String text, boolean faketransfer, int transfertype) {
		transferMoneyFrom(fromID,BigInteger.valueOf(count), text, faketransfer, transfertype);
	}

	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Benutzer zum aktuellen.
	 * Der Transfer kann entweder ein echter Transfer sein (Geld wird abgebucht) oder ein gefakter
	 * Transfer (kein Geld wird abgebucht sondern nur hinzugefuegt).
	 * Zudem faellt jeder Geldtransfer in eine von 3 Kategorien (automatisch, halbautomatisch und manuell).
	 * 
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 * @param faketransfer Handelt es sich um einen "gefakten" Geldtransfer (<code>true</code>)?
	 * @param transfertype Der Transfertyp (Kategorie)
	 * @see #TRANSFER_AUTO
	 * @see #TRANSFER_SEMIAUTO
	 * @see #TRANSFER_NORMAL
	 */
	public void transferMoneyFrom( int fromID, BigInteger count, String text, boolean faketransfer, int transfertype) {
		Database db = context.getDatabase();
		
		if( !count.equals(BigInteger.ZERO) ) {
			if( (fromID != 0) && !faketransfer ) {			
				if( context.getCachedUser(fromID) != null ) {
					context.getCachedUser(fromID).setKonto( context.getCachedUser(fromID).getKonto().subtract(count) );
				}
				else {
					db.tUpdate(1, "UPDATE users SET konto=konto-",count," WHERE id=",fromID);	
				}
			}
		
			db.tUpdate(1, "UPDATE users SET konto=konto+",count," WHERE id=",this.id);
		
			checkAndLoad("konto");
		
			data.put("konto", data.getBigInteger("konto").add(count));	
		
			db.update("INSERT INTO user_moneytransfer (`from`,`to`,`time`,`count`,`text`,`fake`,`type`) ",
					"VALUES ('",fromID,"','",this.id,"','",Common.time(),"','",count,"','",db.prepareString(text),"','",(faketransfer ? 1 : 0),"','",transfertype,"')");
		}
	}
	
	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Spieler zum aktuellen. Beim
	 * Transfer handelt es sich um einen manuellen Transfer.
	 * 
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 * @param faketransfer Handelt es sich um einen "gefakten" Geldtransfer (<code>true</code>)?
	 * @see #transferMoneyFrom(int, long, String, boolean, int)
	 */
	public void transferMoneyFrom( int fromID, long count, String text, boolean faketransfer) {
		transferMoneyFrom( fromID, count, text, faketransfer, TRANSFER_NORMAL );
	}
	
	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Spieler zum aktuellen. Beim
	 * Transfer handelt es sich um einen manuellen Transfer. Das Geld wird tatsaechlich dem Ausgangsspieler
	 * abgezogen (kein "gefakter" Transfer)
	 * 
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 */
	public void transferMoneyFrom( int fromID, long count, String text ) {
		transferMoneyFrom( fromID, count, text, false );
	}
	
	private SQLResultRow research = null;
	
	/**
	 * Gibt den zum Spieler gehoerenden <code>user_f</code>-Eintrag als SQL-Ergebniszeile
	 * zurueck
	 * @return Der <code>user_f</code>-Eintrag
	 */
	public SQLResultRow getResearchedList() {
		if( research == null ) {
			research = context.getDatabase().first("SELECT * FROM user_f WHERE id='",id,"'");
			if( research.isEmpty() ) {
				throw new RuntimeException("Spieler "+id+" verfuegt ueber keine Forschungstabelle");
			}
		}
		return research;
	}
	
	/**
	 * Prueft, ob die angegebene Forschung durch den Benutzer erforscht wurde
	 * 
	 * @param researchID Die ID der zu pruefenden Forschung
	 * @return <code>true</code>, falls die Forschung erforscht wurde
	 */
	public boolean hasResearched( int researchID ) {
		// Forschungs-ID -1 ist per Definition nicht erforschbar - also immer false zurueckgeben.
		if( researchID == -1 ) {
			return false;
		}
		if( research == null ) {
			research = context.getDatabase().first("SELECT * FROM user_f WHERE id='",id,"'");
			if( research.isEmpty() ) {
				throw new RuntimeException("Spieler "+id+" verfuegt ueber keine Forschungstabelle");
			}
		}

		return research.getBoolean("r"+researchID);
	}
	
	/**
	 * Fuegt eine Forschung zur Liste der durch den Benutzer erforschten Technologien hinzu
	 * @param researchID Die ID der erforschten Technologie
	 */
	public void addResearch( int researchID ) {
		if( !hasResearched( researchID ) ) {
			research.put("r"+researchID, true);
			
			context.getDatabase().update("UPDATE user_f SET r",researchID,"='1' WHERE id='",this.id,"'");
		}
	}
	
	/**
	 * Fuegt eine Zeile zur User-Historie hinzu
	 * @param text Die hinzuzufuegende Zeile
	 */
	public void addHistory( String text ) {
		Database db = context.getDatabase();
		
		checkAndLoad("history");
		
		String history = getHistory();
		if( !"".equals(history) ) {
			db.update("UPDATE users SET history='",db.prepareString(history+"\n"+text),"' WHERE id='",id,"'");
			data.put("history", history+"\n"+text);
		}
		else {
			db.update("UPDATE users SET history='",db.prepareString(text),"' WHERE id='",id,"'");
			data.put("history", text);
		}
	}
	
	/**
	 * Fuegt ein Item zur Liste der dem Spieler bekannten Items hinzu.
	 * Die Funktion prueft nicht, ob das Item allgemein bekannt ist,
	 * sondern geht davon aus, dass das angegebene Item allgemein unbekannt ist.
	 * 
	 * @param itemid Die Item-ID
	 */
	public void addKnownItem( int itemid ) {
		checkAndLoad("knownItems");
		
		if( !isKnownItem(itemid) ) {
			String itemlist = data.getString("knownItems").trim();
			if( !itemlist.equals("") ) {
				itemlist += ","+itemid;
			}
			else {
				itemlist = ""+itemid;
			}
			context.getDatabase().tUpdate(1,"UPDATE users SET knownItems='",itemlist,"' WHERE id='",id,"' AND knownItems='",data.getString("knownItems"),"'");
			data.put("knownItems", itemlist);
		}
	}
	
	/**
	 * Prueft, ob das Item mit der angegebenen ID dem Benutzer bekannt ist.
	 * Die Funktion prueft nicht, ob das Item allgemein bekannt ist,
	 * sondern geht davon aus, dass das angegebene Item allgemein unbekannt ist.
	 * @param itemid Die ID des Items
	 * @return <code>true</code>, falls das Item den Spieler bekannt ist
	 */
	public boolean isKnownItem( int itemid ) {
		checkAndLoad("knownItems");
		String[] itemlist = data.getString("knownItems").split(",");
		
		return Common.inArray(""+itemid,itemlist);	
	}
	
	/**
	 * Gibt das Zugriffslevel des Benutzers zurueck
	 * @return Das Zugriffslevel
	 */
	public int getAccessLevel() {
		checkAndLoad("accesslevel");
		int acl = data.getInt("accesslevel");
		if( (attachedID != 0) && (attachedData.getInt("accesslevel") > acl)) {
			return attachedData.getInt("accesslevel");
		}
		return acl;
	}
	
	/**
	 * Prueft, ob der Spieler noch unter Noob-Schutz steht
	 * @return <code>true</code>, falls der Spieler noch ein Noob ist
	 */
	public boolean isNoob() {
		if( Configuration.getIntSetting("NOOB_PROTECTION") > 0 ) {
			if( id < 0 ) {
				return false;
			}
			
			return hasFlag( FLAG_NOOB );
		}
		return false;
	}
	
	private void checkAndLoad( String pname ) {
		if( !preloadedValues.contains(pname) ) {
			data.putAll(context.getDatabase().first("SELECT `",pname,"` FROM users WHERE id='",id,"'"));
			
			if( attachedID != 0 ) {
				attachedData.putAll(context.getDatabase().first("SELECT `",pname,"` FROM users WHERE id='",attachedID,"'"));
			}
			
			preloadedValues.add(pname);
		}
	}
	
	/**
	 * Gibt den Benutzernamen des Spielers zurueck. Der Benutzername
	 * wird lediglich zum einloggen verwendet und wird nicht angezeigt.
	 * @return Der Benutzername
	 */
	public String getUN() {
		checkAndLoad("un");
		return data.getString("un");
	}
	
	/**
	 * Gibt den vollstaendigen Ingame-Namen des Spielers zurueck.
	 * Der vollstaendige Ingame-Name enthaelt den Ally-Tag sofern vorhanden
	 * und ist ggf auch mittels BBCode formatiert 
	 * @return Der vollstaendige Ingame-Name
	 */
	public String getName() {
		checkAndLoad("name");
		return data.getString("name");
	}
	
	/**
	 * Setzt den vollstaendigen Ingame-Namen des Spielers auf den angegebenen
	 * BBCode-String. Gleichzeitig wird das Feld <code>plainname</code> mit dem neuen
	 * Namen ohne BBCodes aktuallisiert.
	 * 
	 * @param name der neue vollstaendige Ingame-Name
	 */
	public void setName( String name ) {
		checkAndLoad("name");
		if( !name.equals(data.getString("name")) ) {
			context.getDatabase().
				prepare("UPDATE users SET name=?,plainname=? WHERE id=? AND name=?").
				tUpdate(1, name, Common._titleNoFormat(name), id, data.getString("name"));
			data.put("name", name);
			data.put("plainname", Common._titleNoFormat(name));
		}
	}

	/**
	 * Gibt die ID der Rasse des Spielers zurueck
	 * @return Die ID der Rasse
	 */
	public int getRace() {
		checkAndLoad("race");
		return data.getInt("race");
	}

	/**
	 * Gibt das verschluesselte Passwort des Spielers zurueck
	 * @return Das verschluesselte Passwort
	 */
	public String getPassword() {
		checkAndLoad("passwort");
		return data.getString("passwort");
	}
	
	/**
	 * Setzt das Passwort fuer den Spieler
	 * @param pw Das neue (mittels MD5 kodierte) Passwort
	 */
	public void setPassword( String pw ) {
		checkAndLoad("passwort");
		context.getDatabase().tUpdate(1, "UPDATE users SET passwort='",pw,"' WHERE id='",id,"' AND passwort='",data.getString("passwort"),"'");
		data.put("passwort", pw);
	}
	
	/**
	 * Gibt die Inaktivitaet des Spielers in Ticks zurueck
	 * @return Die Inaktivitaet des Spielers in Ticks
	 */
	public int getInactivity() {
		checkAndLoad("inakt");
		return data.getInt("inakt");
	}
	
	/**
	 * Gibt die Timestamp des Zeitpunkts zurueck, an dem Sich der Spieler 
	 * angemeldet hat
	 * @return Die Timestamp des Anmeldezeitpunkts
	 */
	public int getSignup() {
		checkAndLoad("signup");
		return data.getInt("signup");
	}

	/**
	 * Gibt die Spielerhistorie als BBCode-formatierten String zurueck
	 * @return Die Spielerhistorie
	 */
	public String getHistory() {
		checkAndLoad("history");
		return data.getString("history");
	}
	
	/**
	 * Gibt die Liste aller Orden und Auszeichnungen des Spielers zurueck.
	 * Die einzelnen Orden-IDs sind mittels ; verbunden
	 * @return Die Liste aller Orden
	 */
	public String getMedals() {
		checkAndLoad("medals");
		return data.getString("medals");
	}
	
	/**
	 * Setzt die Liste der Orden des Spielers
	 * @param medals Eine mittels ; separierte Liste von Orden
	 */
	public void setMedals( String medals ) {
		checkAndLoad("medals");
		context.getDatabase().tUpdate(1, "UPDATE users SET medals='",medals,"' WHERE id='",id,"' AND medals='",data.getString("medals"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("medals", medals);
		}
	}
	
	/**
	 * Liefert den Rang des Benutzers zurueck
	 * @return Der Rang
	 */
	public int getRang() {
		checkAndLoad("rang");
		return data.getInt("rang");
	}
	
	/**
	 * Setzt den Rang des Benutzers
	 * @param rang Die ID des Rangs
	 */
	public void setRang( int rang ) {
		checkAndLoad("rang");
		context.getDatabase().tUpdate(1, "UPDATE users SET rang='",rang,"' WHERE id='",id,"' AND rang='",data.getInt("rang"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("rang", rang);
		}
	}
	
	/**
	 * Liefert die ID der Allianz des Benutzers zurueck.
	 * Wenn der Benutzer in keiner Allianz ist, ist die ID 0.
	 * 
	 * @return Die ID der Allianz
	 */
	public int getAlly() {
		checkAndLoad("ally");
		return data.getInt("ally");
	}
	
	/**
	 * Setzt die Allianz, der der Spieler angehoert, auf den angegebenen Wert
	 * @param ally die neue Allianz
	 */
	public void setAlly( int ally ) {
		checkAndLoad("ally");
		context.getDatabase().tUpdate(1, "UPDATE users SET ally='",ally,"' WHERE id='",id,"' AND ally='",data.getInt("ally"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("ally", ally);
		}
	}
	
	/**
	 * Liefert den Kontostand des Benutzers zurueck
	 * @return Der Kontostand
	 */
	public BigInteger getKonto() {
		checkAndLoad("konto");
		return data.getBigInteger("konto");
	}
	
	/**
	 * Setzt den Kontostand des Spielers auf den angegebenen Wert
	 * @param count der neue Kontostand
	 */
	public void setKonto( BigInteger count ) {
		checkAndLoad("konto");
		context.getDatabase().tUpdate(1, "UPDATE users SET konto='",count,"' WHERE id='",id,"' AND konto='",data.getBigInteger("konto"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("konto", count);
		}
	}
	
	/**
	 * Gibt den Cargo des Spielers als Cargo-String zurueck
	 * @return der Cargo des Spielers
	 */
	public String getCargo() {
		checkAndLoad("cargo");
		return data.getString("cargo");
	}
	
	/**
	 * Die Nahrungsbilanz des letzten Ticks
	 * @return Die Nahrungsbilanz des letzten Ticks
	 */
	public String getNahrungsStat() {
		checkAndLoad("nstat");
		return data.getString("nstat");
	}
	
	/**
	 * Gibt die Email-Adresse des Spielers zurueck
	 * @return Die Email-Adresse
	 */
	public String getEmail() { 
		checkAndLoad("email");
		return data.getString("email");
	}
	
	/**
	 * Gibt die Anzahl der fehlgeschlagenen Login-Versuche des Spielers zurueck
	 * @return die Anzahl der fehlgeschlagenene Logins
	 */
	public int getLoginFailedCount() {
		checkAndLoad("log_fail");
		return data.getInt("log_fail");
	}
	
	/**
	 * Setzt die Anzahl der fehlgeschlagenen Logins des Spielers auf den angegebenen Wert
	 * @param count Die neue Anzahl der fehlgeschlagenene Logins
	 */
	public void setLoginFailedCount(int count) {
		checkAndLoad("log_fail");
		context.getDatabase().tUpdate(1, "UPDATE users SET log_fail='",count,"' WHERE id='",id,"' AND log_fail='",data.get("log_fail"),"'");
		data.put("log_fail", count);
	}
	
	/**
	 * Liefert die Anzahl der NPC-Punkte des Benutzers zurueck.
	 * @return Die Anzahl der NPC-Punkte
	 */
	public int getNpcPunkte() {
		checkAndLoad("npcpunkte");
		return data.getInt("npcpunkte");
	}
	
	/**
	 * Setzt die Anzahl der NPC-Punkte des Benutzers
	 * @param punkte Die neue Anzahl der NPC-Punkte
	 */
	public void setNpcPunkte(int punkte) {
		checkAndLoad("npcpunkte");
		context.getDatabase().tUpdate(1, "UPDATE users SET npcpunkte='",punkte,"' WHERE id='",id,"' AND npcpunkte='",data.getInt("npcpunkte"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("npcpunkte", punkte);
		}
	}

	/**
	 * Gibt den Ingame-Namen des Spielers ohne Ally-Tag zurueck.
	 * Der Name ist ggf mittels BBCodes formatiert
	 * @return der Ingame-Name ohne Ally-Tag
	 */
	public String getNickname() {
		checkAndLoad("nickname");
		return data.getString("nickname");
	}
	
	/**
	 * Setzt den Ingame-Namen ohne Ally-Tag des Spielers auf den angegebenen BBCode-String 
	 * @param nick der neue Ingame-Name ohne Ally-Tag
	 */
	public void setNickname( String nick ) {
		checkAndLoad("nickname");
		context.getDatabase()
			.prepare( "UPDATE users SET nickname= ? WHERE id= ? AND nickname= ?")
			.tUpdate(1, nick, id, data.getString("nickname"));
		data.put("nickname", nick);
	}
	
	/**
	 * Gibt den unformatierten Ingame-Namen des Spielers zurueck.
	 * Der Name ist inklusive des Ally-Tags sofern vorhanden
	 * @return Der unformatierte Name inkl. Ally-Tag
	 */
	public String getPlainname() {
		checkAndLoad("plainname");
		return data.getString("plainname");
	}
	
	/**
	 * Gibt die ID des durch den Spieler besetzten Allianz-Postens
	 * zurueck. Sollte der Spieler keinen Allianz-Posten besetzen, so wird 0
	 * zurueckgegeben. 
	 * @return Die ID des Allianz-Postens oder 0
	 */
	public int getAllyPosten() {
		checkAndLoad("allyposten");
		return data.getInt("allyposten");
	}
	
	/**
	 * Setzt den durch den Spieler besetzten Allianz-Posten
	 * @param posten Die ID des Allianzpostens
	 */
	public void setAllyPosten( int posten ) {
		checkAndLoad("allyposten");
		context.getDatabase().tUpdate(1, "UPDATE users SET allyposten='",posten,"' WHERE id='",id,"' AND allyposten='",data.getInt("allyposten"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("allyposten", posten);
		}
	}
	
	/**
	 * Gibt die ID des Systems zurueck, in den die durch die GTU versteigerten Dinge erscheinen sollen.
	 * Das System muss ueber eine Drop-Zone verfuegen.
	 * 
	 * @return Die ID des Systems in den die versteigerten Dinge auftauchen sollen
	 */
	public int getGtuDropZone() {
		checkAndLoad("gtudropzone");
		return data.getInt("gtudropzone");
	}
	
	/**
	 * Setzt die ID des von der GTU verwendeten Dropzone-Systems des Spielers
	 * @param system Die ID des neuen Systems mit der bevorzugten GTU-Dropzone
	 */
	public void setGtuDropZone( int system ) {
		checkAndLoad("gtudropzone");
		context.getDatabase().tUpdate(1, "UPDATE users SET gtudropzone='",system,"' WHERE id='",id,"' AND gtudropzone='",data.getInt("gtudropzone"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("gtudropzone", system);
		}
	}
	
	/**
	 * Gibt die Koordinate des Ortes zurueck, an dem von NPCs georderte Dinge erscheinen sollen.
	 * 
	 * @return Die Koordinaten des Ortes, an dem georderte Dinge erscheinen sollen
	 */
	public String getNpcOrderLocation() {
		checkAndLoad("npcorderloc");
		return data.getString("npcorderloc");
	}
	
	/**
	 * Setzt die Koordinaten des Ortes, an dem von NPCs georderte Dinge erscheinen sollen
	 * @param loc Die Koordinaten des Ortes, an dem georderte Dinge erscheinen sollen
	 */
	public void setNpcOrderLocation( String loc ) {
		checkAndLoad("npcorderloc");
		context.getDatabase().tUpdate(1, "UPDATE users SET npcorderloc='",loc,"' WHERE id='",id,"' AND npcorderloc='",data.getInt("npcorderloc"),"'");
		if( !context.getDatabase().isTransaction() || context.getDatabase().tStatus() ) {
			data.put("npcorderloc", loc);
		}
	}
	
	/**
	 * Gibt den Image-Pfad des Spielers zurueck
	 * @return Der Image-Pfad des Spielers
	 */
	public String getImagePath() {
		checkAndLoad("imgpath");
		return data.getString("imgpath");
	}
	
	/**
	 * Setzt den Image-Pfad des Spielers auf den angegebenen Wert
	 * @param value Der neue Image-Pfad des Spielers
	 */
	public void setImagePath(String value) {
		if( this.userImagePath != null ) {
			context.getDatabase().prepare("UPDATE users SET imgpath= ? WHERE id= ? AND imgpath= ?")
				.tUpdate(1, value, id, userImagePath);
		}
		else {
			context.getDatabase().prepare("UPDATE users SET imgpath= ? WHERE id= ?")
				.tUpdate(1, value, id);
		}
		this.userImagePath = value;
	}
	
	/**
	 * Gibt <code>true</code> zurueck, falls der Account deaktiviert ist
	 * @return <code>true</code>, falls der Account deaktiviert ist
	 */
	public boolean getDisabled() {
		checkAndLoad("disabled");
		if( data.getInt("disabled") != 0 ) {
			return true;
		}
		return false;
	}
	
	/**
	 * (De)aktiviert den Account. 
	 * @param value <code>true</code>, wenn der Account deaktiviert sein soll. Andernfalls <code>false</code>
	 */
	public void setDisabled(boolean value) {
		checkAndLoad("disabled");
		context.getDatabase().tUpdate(1, "UPDATE users SET disabled='",(value ? 1 : 0),"' WHERE id='",id,"' AND disabled='",data.getInt("disabled"),"'");
		data.put("disabled", (value ? 1 : 0));
	}
	
	/**
	 * Gibt die Anzahl der Ticks zurueck, die der Account noch im 
	 * Vacation-Modus ist. Der Account kann sich auch noch im Vorlauf befinden!
	 * @return Die Anzahl der verbleibenden Vac-Ticks
	 */
	public int getVacationCount() {
		checkAndLoad("vaccount");
		return data.getInt("vaccount");
	}
	
	/**
	 * Setzt die Anzahl der Ticks, die der Account im Vacation-Modus verbringen soll
	 * @param value Die Anzahl der Ticks im Vacation-Modus
	 */
	public void setVacationCount(int value) {
		checkAndLoad("vaccount");
		context.getDatabase().tUpdate(1, "UPDATE users SET vaccount='",value,"' WHERE id='",id,"' AND vaccount='",data.getInt("vaccount"),"'");
		data.put("vaccount", value);
	}

	/**
	 * Gibt zurueck, wieviele Ticks sich der Account noch im Vorlauf fuer den
	 * Vacation-Modus befindet
	 * @return Die Anzahl der verbleibenden Ticks im Vacation-Vorlauf 
	 */
	public int getWait4VacationCount() {
		checkAndLoad("wait4vac");
		return data.getInt("wait4vac");
	}
	
	/**
	 * Setzt die Anzahl der Ticks des Vacation-Modus-Vorlaufs auf den angegebenen
	 * Wert
	 * @param value Die Anzahl der Ticks im Vacation-Modus-Vorlauf
	 */
	public void setWait4VacationCount(int value) {
		checkAndLoad("wait4vac");
		context.getDatabase().tUpdate(1, "UPDATE users SET wait4vac='",value,"' WHERE id='",id,"' AND wait4vac='",data.getInt("wait4vac"),"'");
		data.put("wait4vac", value);
	}
	
	/**
	 * Gibt die Anzahl der gewonnenen Schlachten zurueck
	 * @return die Anzahl der gewonnenen Schlachten
	 */
	public int getWonBattles() {
		checkAndLoad("wonbattles");
		return data.getInt("wonbattles");
	}
	
	/**
	 * Gibt die Anzahl der verlorenen Schlachten zurueck
	 * @return die Anzahl der verlorenen Schlachten
	 */
	public int getLostBattles() {
		checkAndLoad("lostbattles");
		return data.getInt("lostbattles");
	}
	
	/**
	 * Gibt die Anzahl der verlorenen Schiffe zurueck
	 * @return die Anzahl der verlorenen Schiffe
	 */
	public int getLostShips() {
		checkAndLoad("lostships");
		return data.getInt("lostships");
	}
	
	/**
	 * Gibt die Anzahl der zerstoerten Schiffe zurueck
	 * @return die Anzahl der zerstoerten Schiffe
	 */
	public int getDestroyedShips() {
		checkAndLoad("destroyedships");
		return data.getInt("destroyedships");
	}
	
	/**
	 * Gibt die Liste der bekannten Items zurueck, welche per Default
	 * unbekannt ist.
	 * @return Die Liste der bekannten Items als Item-String
	 */
	public String getKnownItems() {
		checkAndLoad("knownItems");
		return data.getString("knownItems");
	}
}
