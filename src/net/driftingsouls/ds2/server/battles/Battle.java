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
package net.driftingsouls.ds2.server.battles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.tick.regular.SchiffsTick;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.RandomUtils;

/**
 * Repraesentiert eine Schlacht in DS
 * @author Christopher Jung
 *
 */
public class Battle implements Loggable {
	private static final int LOGFORMAT = 2;
	
	//
	// Aktionskonstanten von Schiffen in der Schlacht (battles_ships->action)
	//
	/**
	 * Schiff wird am Rundenende geloescht
	 */
	public static final int BS_DESTROYED = 1;
	/**
	 * Schiff verlaesst am Rundenende die Schlacht
	 */
	public static final int BS_FLUCHT = 2;
	/**
	 * Schiff ist getroffen (Wertabgleich ships und battles_ships!)
	 */
	public static final int BS_HIT = 4;
	/**
	 * Das Schiff hat gefeuernt
	 */
	public static final int BS_SHOT = 8;
	/**
	 * Schiff tritt der Schlacht bei
	 */
	public static final int BS_JOIN = 16;
	/**
	 * Schiff befindet sich in der zweiten Reihe
	 */
	public static final int BS_SECONDROW = 32;
	/**
	 * Schiff flieht naechste Runde
	 */
	public static final int BS_FLUCHTNEXT = 64;
	/**
	 *  Schiff hat bereits eine zweite Reihe aktion in der Runde ausgefuehrt
	 */
	public static final int BS_SECONDROW_BLOCKED = 128;
	/**
	 * Waffen sind bis zum Rundenende blockiert
	 */
	public static final int BS_BLOCK_WEAPONS = 256;
	/**
	 * Waffen sind bis zum Kampfende blockiert
	 */
	public static final int BS_DISABLE_WEAPONS = 512;
	
	// Flags fuer Schlachten
	/**
	 * Erste Runde
	 */
	public static final int FLAG_FIRSTROUND = 1;
	/**
	 * Entfernt die zweite Reihe auf Seite 0
	 */
	public static final int FLAG_DROP_SECONDROW_0 = 2;
	/**
	 * Entfernt die zweite Reihe auf Seite 1
	 */
	public static final int FLAG_DROP_SECONDROW_1 = 4;
	/**
	 * Blockiert die zweite Reihe auf Seite 0
	 */
	public static final int FLAG_BLOCK_SECONDROW_0 = 8;
	/**
	 * Blockiert die zweite Reihe auf Seite 1
	 */
	public static final int FLAG_BLOCK_SECONDROW_1 = 16;
	
	private int id = 0;
	private int x = 0;
	private int y = 0;
	private int system = 0;
	private int flags = 0;
	private String ownShipGroup = "0";
	private String enemyShipGroup = "0";
	
	private int ownSide = 0;
	private int enemySide = 0;
	
	private List<SQLResultRow> ownShips = new ArrayList<SQLResultRow>();
	private List<SQLResultRow> enemyShips = new ArrayList<SQLResultRow>();
	
	private boolean startOwn = false;

	private int[] points = new int[2];
	private int[] ally = new int[2];
	private int[] commander = new int[2];
	private boolean[] ready = new boolean[2];
	private String[] comMsg = new String[2];
	private boolean[] betak = new boolean[2];
	private int[] takeCommand = new int[2];
	private List<List<Integer>> addCommanders = new ArrayList<List<Integer>>();
	
	private int blockcount = 0;
	private long lastturn = 0;
	private String visibility = "";
	private int quest = 0;
	private boolean guest = false;
	
	private SQLResultRow tableBuffer = null;
	
	private Map<Integer,Integer> ownShipTypeCount = new HashMap<Integer,Integer>();
	private Map<Integer,Integer> enemyShipTypeCount = new HashMap<Integer,Integer>();
	
	private int activeSOwn = 0;
	private int activeSEnemy = 0;
	
	private StringBuilder logoutputbuffer = new StringBuilder();
	private StringBuilder logenemybuffer = new StringBuilder();
	
	/**
	 * Generiert eine Stringrepraesentation eines Schiffes, welche
	 * in KS-Logs geschrieben werden kann
	 * @param ship Das Schiff
	 * @return Die Stringrepraesentation
	 */
	public static String log_shiplink( SQLResultRow ship ) {
		SQLResultRow shiptype = Ships.getShipType(ship);

		return "[tooltip="+shiptype.getString("nickname")+"]"+ship.getString("name")+"[/tooltip] ("+ship.getInt("id")+")";
	}
	
	/**
	 * Gibt die ID der Schlacht zurueck
	 * @return die ID
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Gibt die X-Position der Schlacht zurueck
	 * @return die X-Position
	 */
	public int getX() {
		return this.x;
	}
	
	/**
	 * Gibt die Y-Position der Schlacht zurueck
	 * @return Die Y-Position
	 */
	public int getY() {
		return this.y;
	}
	
	/**
	 * Gibt das System zurueck, in dem die Schlacht stattfindet
	 * @return das System
	 */
	public int getSystem() {
		return this.system;
	}
	
	/**
	 * Prueft, ob die zweite Reihe stabil ist. Beruecksichtigt wird auf Wunsch 
	 * auch eine Liste von Schiffen, welche der Schlacht noch nicht begetreten sind
	 * (unter der Annahme, dass gemaess Flags die Schiffe in der ersten bzw zweiten
	 * Reihe landen wuerden).
	 * @param side Die Seite deren zweite Reihe geprueft werden soll
	 * @param added Eine Liste zusaetzlicher Schiffe, welche sich noch nicht in der Schlacht befinden oder <code>null</code>
	 * @return <code>true</code>, falls die zweite Reihe unter den Bedingungen stabil ist
	 */
	public boolean isSecondRowStable( int side, SQLResultRow[] added ) {
		List<SQLResultRow> shiplist = null;
		if( side == this.ownSide ) {
			shiplist = this.ownShips;
		}
		else {
			shiplist = this.enemyShips;
		}
		
		int owncaps = 0;
		int secondrowcaps = 0;
		for( int i=0; i < shiplist.size(); i++ ) {
			SQLResultRow aship = shiplist.get(i);
			
			if( (aship.getInt("action") & BS_JOIN) != 0 || (aship.getInt("action") & BS_FLUCHT) != 0 ) {
				continue;	
			}
			SQLResultRow type = Ships.getShipType(aship);
			
			if( (aship.getInt("action") & BS_SECONDROW) != 0 ) {
				if( aship.getString("docked").length() == 0 ) {
					secondrowcaps += type.getInt("size");
				}
			}
			else if( type.getInt("size") > 3 ) {
				owncaps += type.getInt("size");	
			}	
		}
		
		if( added != null ) {
			for( int i=0; i < added.length; i++ ) {
				SQLResultRow aship = added[i];
				
				SQLResultRow type = Ships.getShipType(aship);
				
				if( !Ships.hasShipTypeFlag(type, Ships.SF_SECONDROW) ) {
					continue;
				}
				
				if( aship.getString("docked").length() == 0 ) {
					secondrowcaps += type.getInt("size");
				}	
			}
		}

		if( secondrowcaps == 0 ) {
			return true;	
		}
				
		if( owncaps <= secondrowcaps*2 ) {
			return false;
		}
		return true;
	}
	
	/**
	 * Prueft, ob die Schlacht ueber das angegebene Flag verfuegt
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Schlacht das Flag besitzt
	 */
	public boolean hasFlag( int flag ) {
		return (this.flags & flag) != 0;	
	}
	
	/**
	 * Fuegt der Schlacht das angegebene Flag hinzu
	 * @param flag Das Flag
	 */
	public void setFlag( int flag ) { 
		setFlag(flag, true);
	}
	
	/**
	 * Fuegt der Schlacht ein Flag hinzu oder entfernt eines
	 * @param flag Das Flag
	 * @param status <code>true</code>, falls das Flag hinzugefuegt werden soll. Andernfalls <code>false</code>
	 */
	public void setFlag( int flag, boolean status ) {
		if( status ) {
			this.flags |= flag;	
		}	
		else if( (this.flags & flag) != 0 ) {
			this.flags ^= flag;	
		}
	}
	
	/**
	 * Fuegt dem Nachrichtenpuffer einer Seite eine Nachricht hinzu
	 * @param side Die Seite
	 * @param text Der hinzuzufuegende Text
	 */
	public void addComMessage( int side, String text ) {
		Database db = ContextMap.getContext().getDatabase();
		
		db.prepare("UPDATE battles SET com"+(side+1)+"Msg=CONCAT(com"+(side+1)+"Msg, ?) WHERE id= ?")
			.update(text, this.id);
	}
	
	/**
	 * Leert den Nachrichtenpuffer fuer die angegebene Seite
	 * @param side Die Seite oder <code>-1</code> fuer die eigene Seite
	 */
	public void clearComMessageBuffer( int side ) {
		if( side == -1 ) {
			side = this.ownSide;
		}
		Database db = ContextMap.getContext().getDatabase();
		
		db.update("UPDATE battles SET com",(side+1),"Msg='' WHERE id=",this.id);
		comMsg[side] = "";
	}
	
	/**
	 * Gibt den Inhalt des Nachrichtenpuffers der angegebenen Seite zurueck
	 * @param side Die Seite
	 * @return Der Nachrichtenpuffer
	 */
	public String getComMessageBuffer( int side ) {
		return this.comMsg[side];
	}
	
	private int getActionPoints( int side ) {
		List<SQLResultRow> olist = this.ownShips;
		List<SQLResultRow> elist = this.enemyShips;
		
		if( side != this.ownSide ) {
			elist = this.ownShips;
			olist = this.enemyShips;
		}
		
		double points = 600;
		
		double ownsize = 0;
		int owncount = 0;
		double enemysize = 0;
		int enemycount = 0;
		
		for( int i=0; i < olist.size(); i++ ) {
			SQLResultRow aShip = olist.get(i);
			
			SQLResultRow aShipType = Ships.getShipType(aShip);
			if( (aShipType.getInt("size") > 3) && (aShipType.getInt("military") != 0) && (aShipType.getInt("crew") > 0) ) {
				double factor = aShip.getInt("crew") / (double)aShipType.getInt("crew");
				ownsize += factor*aShipType.getInt("size");
				owncount++;
			}
		}	
		
		for( int i=0; i < elist.size(); i++ ) {
			SQLResultRow aShip = elist.get(i);
			
			SQLResultRow aShipType = Ships.getShipType(aShip);
			if( (aShipType.getInt("size") > 3) && (aShipType.getInt("military") != 0) && (aShipType.getInt("crew") > 0) ) {
				double factor = aShip.getInt("crew") / (double)aShipType.getInt("crew");
				enemysize += factor*aShipType.getInt("size");
				enemycount++;
			}
		}	
		
		if( enemycount > 0 ) {
			if( enemysize < 1 ) {
				enemysize = 1;
			}
			if( ownsize > enemysize ) {
				points += (ownsize/enemysize-1 > 1 ? 1 : ownsize/enemysize-1)*points; 	
			}
		}
		else {
			int add = owncount*2;
			if( add > 100 ) {
				add = 100;
			}
			points += points*add/100;
		}		
		
		return (int)Math.round(points);
	}
	
	/**
	 * Bestimmt, ob eigene gelandete Schiffe beim Start der Schlacht starten sollen
	 * @param value <code>true</code>, falls eigene gelandete Schiffe starten sollen
	 */
	public void setStartOwn( boolean value ) {
		this.startOwn = value;	
	}
	
	/**
	 * Gibt den Betak-Status einer Seite zurueck
	 * @param side Die Seite
	 * @return <code>true</code>, falls die Seite noch nicht gegen die Betak verstossen hat
	 */
	public boolean getBetakStatus( int side ) {
		return this.betak[side];	
	}
	
	/**
	 * Setzt den Betak-Status einer Seite
	 * @param side Die Seite
	 * @param status Der neue Betak-Status
	 */
	public void setBetakStatus( int side, boolean status ) {
		this.betak[side] = status;
	}
	
	/**
	 * Prueft, ob der Betrachter ein Gast ist
	 * @return <code>true</code>, falls der Betrachter ein Gast ist
	 */
	public boolean isGuest() {
		return this.guest;	
	}

	/**
	 * Gibt das aktuell ausgewaehlte eigene Schiff zurueck
	 * @return Das aktuell ausgewaehlte eigene Schiff
	 */
	public SQLResultRow getOwnShip() {
		return this.ownShips.get(this.activeSOwn);
	}

	/**
	 * Gibt das aktuell ausgewaehlte gegnerische Schiff zurueck
	 * @return Das aktuell ausgewaehlte gegnerische Schiff
	 */
	public SQLResultRow getEnemyShip() {
		return this.enemyShips.get(this.activeSEnemy);
	}
	
	/**
	 * Liefert den Index des naechsten feindlichen Schiffes nach dem aktuell ausgewaehlten. 
	 * Bevorzugt werden Schiffe gleichen Typs
	 * @return Der Index des naechsten passenden Schiffes
	 */
	public int getNewTargetIndex() {
		boolean foundOld = false;
	
		SQLResultRow enemyShip = getEnemyShip();
		
		// Schiff gleichen Typs hinter dem aktuellen Schiff suchen
		List<SQLResultRow> enemyShips = getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow aship = enemyShips.get(i);
			if( !foundOld && (aship.getInt("id") == enemyShip.getInt("id")) ) {
				foundOld = true;	
			}
			else if( foundOld && (aship.getInt("type") == enemyShip.getInt("type")) && ((aship.getString("docked").length() == 0) || (aship.getString("docked").charAt(0) != 'l')) && (aship.getInt("action") & Battle.BS_DESTROYED) == 0 && (aship.getInt("action") & Battle.BS_SECONDROW) == 0 ) {
				return i;
			}
		}

		// Schiff gleichen Typs vor dem aktuellen Schiff suchen
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow aship = enemyShips.get(i);
			if( aship.getInt("id") == enemyShip.getInt("id") ) {
				break;
			}
			if( (aship.getInt("type") == enemyShip.getInt("type")) && ((aship.getString("docked").length() == 0) || (aship.getString("docked").charAt(0) != 'l')) && (aship.getInt("action") & Battle.BS_DESTROYED) == 0 && (aship.getInt("action") & Battle.BS_SECONDROW) == 0 ) {
				return i;
			}
		}
	
		// Irgendein nicht gelandetes Schiff suchen
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow aship = enemyShips.get(i);
			if( ((aship.getString("docked").length() == 0) || (aship.getString("docked").charAt(0) != 'l')) && (aship.getInt("action") & Battle.BS_DESTROYED) == 0 && (aship.getInt("action") & Battle.BS_SECONDROW) == 0 ) {
				return i;
			}
		}
		
		return 0;
	}
	
	/**
	 * Gibt den Index des aktuell ausgewaehlten generischen Schiffes zurueck
	 * @return Der Index des aktuell ausgewaehlten gegnerischen Schiffes
	 */
	public int getEnemyShipIndex() {
		return this.activeSEnemy;
	}
	
	/**
	 * Setzt den Index des aktuell ausgewaehlten gegnerischen Schiffes
	 * @param index Der neue Index
	 */
	public void setEnemyShipIndex(int index) {
		if( index >= this.enemyShips.size() ) {
			throw new IndexOutOfBoundsException("Schiffsindex fuer gegnerische Schiffe '"+index+"' > als das das vorhandene Maximum ("+this.enemyShips.size()+")");
		}
		this.activeSEnemy = index;
	}
	
	/**
	 * Gibt den Index des aktuell ausgewaehlten eigenen Schiffes zurueck
	 * @return Der Index des aktuell ausgewaehlten eigenen Schiffes
	 */
	public int getOwnShipIndex() {
		return this.activeSOwn;
	}
	
	/**
	 * Setzt den Index des aktuell ausgewaehlten eigenen Schiffes
	 * @param index Der neue Index
	 */
	public void setOwnShipIndex(int index) {
		if( index >= this.ownShips.size() ) {
			throw new IndexOutOfBoundsException("Schiffsindex fuer eigene Schiffe '"+index+"' > als das das vorhandene Maximum ("+this.ownShips.size()+")");
		}
		this.activeSOwn = index;
	}

	/**
	 * Speichert die aktuellen Aenderungen in der Schlacht in der Datenbank
	 * @param ignoreinakt Soll das Inaktivitaetsfeld nicht zurueckgesetzt werden (<code>true</code>)?
	 */
	public void save( boolean ignoreinakt  ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		List<String> update = new ArrayList<String>();
		List<Object> updateData = new ArrayList<Object>();
		List<String> where = new ArrayList<String>();
		List<Object> whereData = new ArrayList<Object>();
		SQLResultRow data = new SQLResultRow();

		data.put("ally1", ally[0]);
		data.put("ally2", this.ally[1]);
		data.put("commander1", this.commander[0]);
		data.put("commander2", this.commander[1]);
		data.put("com1Points", this.points[0]);
		data.put("com2Points", this.points[1]);
		data.put("ready1", this.ready[0]);
		data.put("ready2", this.ready[1]);
		data.put("com1BETAK", this.betak[0]);
		data.put("com2BETAK", this.betak[1]);
		data.put("takeCommand1", this.takeCommand[0]);
		data.put("takeCommand2", this.takeCommand[1]);
		data.put("blockcount", this.blockcount);
		data.put("lastturn", this.lastturn);
		data.put("flags", this.flags);
		
		for( String key : data.keySet() ) {
			Object element = data.get(key);
			if( ((element != null) && !element.equals(this.tableBuffer.get(key))) || (this.tableBuffer.get(key) != null) ) {
				update.add(key+"= ? ");
				updateData.add(element);
				where.add(key+"= ? ");
				whereData.add(this.tableBuffer.get(key));
			}
		}
				
		if( this.tableBuffer.getInt("quest") != this.quest ) {
			if( this.quest != 0 ) {
				update.add("quest= ? ");
				updateData.add(this.quest);
			}
			else {
				update.add("quest=NULL");
			}
			if( this.tableBuffer.getInt("quest") != 0 ) {
				where.add("quest= ? ");
				whereData.add(this.tableBuffer.getInt("quest"));
			}
			else {
				where.add("quest=NULL");
			}
		}
		
		if( (this.visibility != null && !this.visibility.equals(this.tableBuffer.get("visibility"))) ||
			(this.tableBuffer.get("visibility") != null) ) {
			if( this.visibility != null ) {
				update.add("visibility= ? ");
				updateData.add(this.visibility);
			}
			else {
				update.add("visibility=NULL");
			}
			if( this.tableBuffer.get("vsibility") != null ) {
				where.add("visibility= ? ");
				whereData.add(this.tableBuffer.get("visibility"));
			}
			else {
				where.add("visibility=NULL");
			}
		}
		
		if( !ignoreinakt ) {
			update.add("inakt= ? ");
			updateData.add(0);
		}
		update.add("lastaction= ? ");
		updateData.add(Common.time());
		
		where.add("id= ? ");
		whereData.add(this.id);
	
		PreparedQuery pq = db.prepare("UPDATE battles ",
					"SET ",Common.implode(",",update)+" ",
					"WHERE "+Common.implode(" AND ",where));
		
		int index=1;
		for( int i=0; i < updateData.size(); i++ ) {
			pq.setObject(index++, updateData.get(i));
		}
		for( int i=0; i < whereData.size(); i++ ) {
			pq.setObject(index++, whereData.get(i));
		}
		pq.tUpdate(1);
		pq.close();
	}

	/**
	 * Erstellt eine neue Schlacht
	 * @param id Die ID des Spielers, der die Schlacht beginnt
	 * @param ownShipID Die ID des Schiffes des Spielers, der angreift
	 * @param enemyShipID Die ID des angegriffenen Schiffes
	 * @return <code>true</code>, falls die Schlacht erfolgreich erstellt wurde
	 */
	public boolean create( int id, int ownShipID, int enemyShipID ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		// Kann der Spieler ueberhaupt angreifen (Noob-Schutz?)
		User user = context.createUserObject( id );
		if( user.isNoob() ) {
			context.addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher keinen Gegner angreifen!<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden");
			return false;
		}
		
		SQLResultRow tmpOwnShip = db.first("SELECT t1.id,t1.x,t1.y,t1.system,t2.ally,t1.lock FROM ships t1 JOIN users t2 ON t1.owner=t2.id WHERE t1.id>0 AND t1.owner=",id," AND t1.id=",ownShipID);
		SQLResultRow tmpEnemyShip = db.first("SELECT t1.id,t1.x,t1.y,t1.system,t1.owner,t1.status,t2.ally,t1.lock FROM ships t1 JOIN users t2 ON t1.owner=t2.id WHERE t1.id>0 AND t1.id=",enemyShipID);

		if( tmpOwnShip.isEmpty() ) {
			context.addError("Das angreifende Schiff existiert nicht oder untersteht nicht ihrem Kommando!");
			return false;
		}
		
		if( tmpEnemyShip.isEmpty() ) {
			context.addError("Das angegebene Zielschiff existiert nicht!");
			return false;
		}
		
		if( !Location.fromResult(tmpOwnShip).sameSector(0,Location.fromResult(tmpEnemyShip),0) ) {
			context.addError("Die beiden Schiffe befinden sich nicht im selben Sektor");
			return false;
		}

		//
		// Kann der Spieler angegriffen werden (NOOB-Schutz?/Vac-Mode?)
		//

		User enemyUser = context.createUserObject( tmpEnemyShip.getInt("owner") );
		if( enemyUser.isNoob() ) {
			context.addError("Der Gegner steht unter GCP-Schutz und kann daher nicht angegriffen werden!");
			return false;
		}
		
		if( enemyUser.getVacationCount() != 0 && enemyUser.getWait4VacationCount() == 0 ) {
			context.addError("Der Gegner befindet sich im Vacation-Modus und kann daher nicht angegriffen werden!");
			return false;
		}
		
		//
		// IFF-Stoersender?
		// 
		boolean disable_iff = tmpEnemyShip.getString("status").indexOf("disable_iff") > -1;
		if( disable_iff ) {
			context.addError("Dieses Schiff kann nicht angegriffen werden (egal wieviel du mit der URL rumspielt!)");
			return false;	
		}
		
		//
		// Questlock?
		// 
		if( tmpOwnShip.getString("lock").length() > 0 ) {
			context.addError("Ihr Schiff ist an ein Quest gebunden");
			return false;
		}
		
		if( tmpEnemyShip.getString("lock").length() > 0 ) {
			context.addError("Das gegnerische Schiff ist an ein Quest gebunden");
			return false;
		}

		//
		// Schiffsliste zusammenstellen
		//
		
		this.ownSide = 0;
		this.enemySide = 1;
		
		SQLResultRow enemyShip = new SQLResultRow();
		SQLResultRow ownShip = new SQLResultRow();
		
		Set<Integer> ownUsers = new HashSet<Integer>();
		Set<Integer> enemyUsers = new HashSet<Integer>();		

		SQLQuery aShipRow = db.query("SELECT s.*,u.name AS username,u.ally ",
									"FROM ships s JOIN users u ON s.owner=u.id ",
									"WHERE s.id>0 AND s.x=",tmpOwnShip.getInt("x")," AND s.y=",tmpOwnShip.getInt("y")," AND " ,
							   			"s.system=",tmpOwnShip.getInt("system")," AND s.battle=0 AND " ,
							   			"u.ally IN (",tmpOwnShip.getInt("ally"),",",tmpEnemyShip.getInt("ally"),") AND " ,
							   			"!LOCATE('disable_iff',s.status) AND `lock` IS NULL AND (u.vaccount=0 OR u.wait4vac > 0)");
							   		
		while( aShipRow.next() ) {
			// Loot-Truemmer sollten in keine Schlacht wandern... (nicht schoen, gar nicht schoen geloest)
			if( (aShipRow.getInt("owner") == -1) && (aShipRow.getInt("type") == Configuration.getIntSetting("CONFIG_TRUEMMER")) ) {
				continue;
			}
			
			User tmpUser = context.createUserObject( aShipRow.getInt("owner") );
						
			if( tmpUser.isNoob() ) {
				continue;
			}
			
			SQLResultRow aShip = aShipRow.getRow();
			
			SQLResultRow shiptype = Ships.getShipType(aShip);
			if( (aShip.getString("docked").length() == 0) && Ships.hasShipTypeFlag(shiptype, Ships.SF_SECONDROW) ) {
				aShip.put("action", BS_SECONDROW);
			}
			else {
				aShip.put("action", 0);
			}
			
			if( (shiptype.getInt("class") == ShipClasses.GESCHUETZ.ordinal()) && aShip.getString("docked").length() > 0 ) {
				aShip.put("action", aShip.getInt("action") | BS_DISABLE_WEAPONS);
			}
			
			if( ( (tmpOwnShip.getInt("ally") > 0) && (aShip.getInt("ally") == tmpOwnShip.getInt("ally")) ) || (id == aShip.getInt("owner")) ) {
				ownUsers.add(aShip.getInt("owner"));
				if( (ownShipID != 0) && (aShip.getInt("id") == ownShipID) ) {
					ownShip = aShip;
				} 
				else {
					this.ownShips.add(aShip);
				}
			} 
			else if( ( (tmpEnemyShip.getInt("ally") > 0) && (aShip.getInt("ally") == tmpEnemyShip.getInt("ally")) ) || (tmpEnemyShip.getInt("owner") == aShip.getInt("owner")) ) {
				enemyUsers.add(aShip.getInt("owner"));
				if( (enemyShipID != 0) && (aShip.getInt("id") == enemyShipID) ) {
					enemyShip = aShip;
				} 
				else {
					this.enemyShips.add(aShip);
				}
			}

		}
		aShipRow.free();
		
		tmpOwnShip.clear();
		tmpEnemyShip.clear();

		//
		// Schauen wir mal ob wir was sinnvolles aus der DB gelesen haben
		// - Wenn nicht: Abbrechen
		//
		
		if( ownShip.isEmpty() ) {
			context.addError("Offenbar liegt ein Problem mit dem von ihnen angegebenen Schiff oder ihrem eigenen Schiff vor (wird es evt. bereits angegriffen?)");
			return false;
		}
		
		if( enemyShip.isEmpty() && (enemyShips.size() == 0) ) {
			context.addError("Offenbar liegt ein Problem mit den feindlichen Schiffen vor (es gibt n&aumlmlich keine die sie angreifen k&ouml;nnten)");
			return false;
		}
		else if( enemyShip.isEmpty() ) {
			context.addError("Offenbar liegt ein Problem mit den feindlichen Schiffen vor (es gibt zwar welche, jedoch fehlt das Zielschiff)");
			return false;
		}
		
		//
		// Schlacht in die DB einfuegen
		//

		db.update("INSERT INTO battles (x,y,system,ally1,ally2,commander1,com1Points,commander2,com2Points,lastaction,lastturn,flags) ",
						 	"VALUES (",ownShip.getInt("x"),",",ownShip.getInt("y"),",",ownShip.getInt("system"),",",ownShip.getInt("ally"),",",enemyShip.getInt("ally"),", ",
							ownShip.getInt("owner"),",0,",enemyShip.getInt("owner"),",0,",Common.time(),",",Common.time(),",'",FLAG_FIRSTROUND,"')");
		this.id = db.insertID();

		if( db.affectedRows() == 0 ) {
			context.addError("<span style=\"color:red\">Die Schlacht konnte nicht erfolgreich erstellt werden</span>");
			return false;
		}

		//
		// Schiffe in die Schlacht einfuegen
		//

		int tick = context.get(ContextCommon.class).getTick();

		// * Gegnerische Schiffe in die Schlacht einfuegen
		List<Integer> idlist = new ArrayList<Integer>();
		List<Integer> startlist = new ArrayList<Integer>();

		if( (enemyShip.getString("docked").length() > 0) && (enemyShip.getString("docked").charAt(0) == 'l') ) {
			startlist.add(enemyShip.getInt("id"));
		}
		idlist.add(enemyShip.getInt("id"));
		
		SQLResultRow enemyShipType = Ships.getShipType(enemyShip);

		db.update("INSERT INTO battles_ships ",
					"(shipid,battleid,side,hull,shields,engine,weapons,comm,sensors,action,count) ",
					"VALUES (",enemyShip.getInt("id"),",",this.id,",1,",enemyShip.getInt("hull"),",",enemyShip.getInt("shields"),",",enemyShip.getInt("engine"),",",enemyShip.getInt("weapons"),",",enemyShip.getInt("comm"),",",enemyShip.getInt("sensors"),",",enemyShip.getInt("action"),",",enemyShipType.getInt("shipcount"),")");


		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow ship = enemyShips.get(i);
			
			if( (ship.getString("docked").length() > 0) && 
				(ship.getString("docked").charAt(0) == 'l') ) {
				startlist.add(ship.getInt("id"));
			}
			idlist.add(ship.getInt("id"));
			
			SQLResultRow shiptype = Ships.getShipType(ship);

			db.update("INSERT INTO battles_ships ",
					"(shipid,battleid,side,hull,shields,engine,weapons,comm,sensors,action,count) ",
					"VALUES (",ship.getInt("id"),",",this.id,",1,",ship.getInt("hull"),",",ship.getInt("shields"),",",ship.getInt("engine"),",",ship.getInt("weapons"),",",ship.getInt("comm"),",",ship.getInt("sensors"),",",ship.getInt("action"),",",shiptype.getInt("shipcount"),")");
		}
		if( startlist.size() > 0 ) {
			this.logme(startlist.size()+" J&auml;ger sind automatisch gestartet\n");
			this.logenemy("<action side=\"1\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\n"+startlist.size()+" J&auml;ger sind automatisch gestartet\n]]></action>\n");

			db.update("UPDATE ships SET docked='' WHERE id>0 AND id IN ("+Common.implode(",",startlist),")");
			startlist.clear();
		}

		// * Eigene Schiffe in die Schlacht einfuegen
		idlist.add(ownShip.getInt("id"));
		
		SQLResultRow ownShipType = Ships.getShipType(ownShip);

		db.update("INSERT INTO battles_ships ",
				"(shipid,battleid,side,hull,shields,engine,weapons,comm,sensors,action,count) ",
				"VALUES (",ownShip.getInt("id"),",",this.id,",0,",ownShip.getInt("hull"),",",ownShip.getInt("shields"),",",ownShip.getInt("engine"),",",ownShip.getInt("weapons"),",",ownShip.getInt("comm"),",",ownShip.getInt("sensors"),",",ownShip.getInt("action"),",",ownShipType.getInt("shipcount"),")");

		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow ship = ownShips.get(i);
			
			if( this.startOwn && (ship.getString("docked").length() > 0) && 
				(ship.getString("docked").charAt(0) == 'l') ) {
				startlist.add(ship.getInt("id"));
			}
			idlist.add(ship.getInt("id"));
			
			SQLResultRow shiptype = Ships.getShipType(ship);

			db.update("INSERT INTO battles_ships ",
					"(shipid,battleid,side,hull,shields,engine,weapons,comm,sensors,action,count) ",
					"VALUES (",ship.getInt("id"),",",this.id,",0,",ship.getInt("hull"),",",ship.getInt("shields"),",",ship.getInt("engine"),",",ship.getInt("weapons"),",",ship.getInt("comm"),",",ship.getInt("sensors"),",",ship.getInt("action"),",",shiptype.getInt("shipcount"),")");
		}
		if( this.startOwn && startlist.size() > 0 ) {
			this.logme(startlist.size()+" J&auml;ger sind automatisch gestartet\n");
			this.logenemy("<action side=\"0\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\n"+startlist.size()+" J&auml;ger sind automatisch gestartet\n]]></action>\n");

			db.update("UPDATE ships SET docked='' WHERE id>0 AND id IN ("+Common.implode(",",startlist),")");
		}
		startlist = null;

		db.update("UPDATE ships SET battle=",this.id," WHERE id>0 AND id IN (",Common.implode(",",idlist),")");
		idlist = null;

		//
		// Log erstellen
		//

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(Configuration.getSetting("LOXPATH")+"battles/battle_id"+this.id+".log"));
			writer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
			writer.append("<battle>\n");
			writer.append("<fileinfo format=\""+LOGFORMAT+"\" />\n");
			writer.append("<coords x=\""+ownShip.getInt("x")+"\" y=\""+ownShip.getInt("y")+"\" system=\""+ownShip.getInt("system")+"\" />\n");
	
			if( ownShip.getInt("ally") > 0 ) {
				writer.append("<side1 commander=\""+ownShip.getInt("owner")+"\" ally=\""+ownShip.getInt("ally")+"\" />\n");
			}
			else {
				writer.append("<side1 commander=\""+ownShip.getInt("owner")+"\" />\n");
			}
	
			if( enemyShip.getInt("ally") > 0 ) {
				writer.append("<side2 commander=\""+enemyShip.getInt("owner")+"\" ally=\""+enemyShip.getInt("ally")+"\" />\n");
			}
			else {
				writer.append("<side2 commander=\""+enemyShip.getInt("owner")+"\" />\n");
			}
	
			writer.append("<startdate tick=\""+tick+"\" time=\""+Common.time()+"\" />\n");
			writer.close();
			
			if( SystemUtils.IS_OS_UNIX ) {
				Runtime.getRuntime().exec("chmod 0666 "+Configuration.getSetting("LOXPATH")+"battles/battle_id"+this.id+".log");
			}
		}
		catch( IOException e ) {
			LOG.error("Konnte KS-Log fuer Schlacht "+this.id+" nicht erstellen", e);
		}
		
		
		//
		// Beziehungen aktuallisieren
		//
		
		// Zuerst schauen wir mal ob wir es mit Allys zu tun haben und 
		// berechnen ggf die Userlisten neu 
		Set<Integer> calcedallys = new HashSet<Integer>();
		
		for( Integer auserID : ownUsers ) {
			User auser = context.createUserObject(auserID);
			
			if( (auser.getAlly() != 0) && calcedallys.contains(auser.getAlly()) ) {		
				SQLQuery allyuser = db.query("SELECT id FROM users WHERE ally=",auser.getAlly()," AND !(id IN (",Common.implode(",",ownUsers),"))");
				while( allyuser.next() ) {
					ownUsers.add(allyuser.getInt("id"));	
				}
				calcedallys.add(auser.getAlly());
			}
		}

		for( Integer auserID : enemyUsers ) {
			User auser = context.createUserObject(auserID);
			
			if( (auser.getAlly() != 0) && calcedallys.contains(auser.getAlly()) ) {		
				SQLQuery allyuser = db.query("SELECT id FROM users WHERE ally=",auser.getAlly()," AND !(id IN (",Common.implode(",",enemyUsers),"))");
				while( allyuser.next() ) {
					enemyUsers.add(allyuser.getInt("id"));	
				}
				calcedallys.add(auser.getAlly());
			}
		}
		calcedallys = null;
		
		for( Integer auserID : ownUsers ) {
			User auser = context.createUserObject(auserID);
			
			for( Integer euserID : enemyUsers ) {
				User euser = context.createUserObject(euserID);
								
				auser.setRelation(euser.getID(), User.Relation.ENEMY);
				euser.setRelation(auser.getID(), User.Relation.ENEMY);
			}
		}
		
		//
		// APs berechnen
		//

		
		// Zuerst berechnen wir die eigenen AP
		int ownPoints = this.getActionPoints(this.ownSide);

		// Nun berechnen wir die gegnerischen AP 
		int enemyPoints = this.getActionPoints(this.enemySide);
		
		db.update("UPDATE battles SET com1Points=",ownPoints,",com2Points=",enemyPoints," WHERE id=",this.id);
		
		return true;
	}

	/**
	 * Laesst eines oder mehrere Schiffe (in einer Flotte) der Schlacht beitreten
	 * @param id Die ID des Besitzers der Schiffe
	 * @param shipid Die ID eines der Schiffe, welche beitreten sollen
	 * 
	 * @return <code>true</code>, falls der Beitritt erfolgreich war
	 */
	public boolean addShip( int id, int shipid ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		SQLResultRow shipd = db.first("SELECT * ",
				 "FROM ships ",
				 "WHERE id>0 AND id=",shipid);
	
		if( shipd.isEmpty() ) {
			context.addError("Das angegebene Schiff existiert nicht!");
			return false;
		}
		if( shipd.getInt("owner") != id ) {
			context.addError("Das angegebene Schiff geh&ouml;rt nicht ihnen!");
			return false;
		}
		if( shipd.getString("lock").length() > 0 ) {
			context.addError("Das Schiff ist an ein Quest gebunden");
			return false;
		}
		if( !new Location(this.system,this.x,this.y).sameSector(0, Location.fromResult(shipd), 0) ) {
			context.addError("Das angegebene Schiff befindet sich nicht im selben Sektor wie die Schlacht!");
			return false;
		}
		if( shipd.getInt("battle") != 0 ) {
			context.addError("Das angegebene Schiff befindet sich bereits in einer Schlacht!");
			return false;
		}
		
		if( this.visibility != null ) {
			Integer[] visibility = Common.explodeToInteger(",",this.visibility);
			if( !Common.inArray(id,visibility) ) {
				context.addError("Sie k&ouml;nnen dieser Schlacht nicht beitreten!");
				return false;
			}
		}
		
		User userobj = context.createUserObject(id);
		if( userobj.isNoob() ) {
			context.addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher keine Schiffe in diese Schlacht schicken!<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden");
			return false;
		}
		
		SQLResultRow shiptype = Ships.getShipType(shipd);
		if( shiptype.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) {
			context.addError("<span style=\"color:red\">Gesch&uuml;tze k&ouml;nnen einer Schlacht nicht beitreten!</span>");
			return false;
		}
		
		Map<Integer,Integer> shipcounts = new HashMap<Integer,Integer>();
		
		int side = this.ownSide;
		
		// Beziehungen aktuallisieren
		Set<Integer> calcedallys = new HashSet<Integer>();
		
		List<User> ownUsers = new ArrayList<User>();
		ownUsers.add(userobj);
		Set<User> enemyUsers = new HashSet<User>();
		
		User curuser = userobj;
		if( curuser.getAlly() != 0 ) {
			UserIterator iter = context.createUserIterator("SELECT * FROM users WHERE ally=",curuser.getAlly()," AND id!=",curuser.getID());
			for( User allyuser : iter ) {
				ownUsers.add(allyuser);	
			}
			iter.free();
		}
		
		UserIterator iter = context.createUserIterator("SELECT DISTINCT u.* FROM (users u JOIN ships s ON u.id=s.owner) JOIN battles_ships bs ON s.id=bs.shipid WHERE bs.side=",this.enemySide);
		for( User euser : iter ) {
			enemyUsers.add(euser);

			if( (euser.getAlly() != 0) && calcedallys.contains(euser.getAlly()) ) {
				UserIterator allyIter = context.createUserIterator("SELECT * FROM users WHERE ally=",euser.getAlly()," AND id!=",euser.getID());
				for( User allyuser : allyIter ) {
					enemyUsers.add(allyuser);	
				}
				allyIter.free();
				
				calcedallys.add(euser.getAlly());
			}
		}
		iter.free();
		
		for( int i=0; i < ownUsers.size(); i++ ) {
			User auser = ownUsers.get(i);
			
			for( User euser : enemyUsers ) {
				auser.setRelation(euser.getID(), User.Relation.ENEMY);
				euser.setRelation(auser.getID(), User.Relation.ENEMY);
			}
		}
		
		calcedallys = null;
		
		shipd.put("action", BS_JOIN);
		
		List<Integer> shiplist = new ArrayList<Integer>();
		
		SQLQuery sid = null;
		
		// Handelt es sich um eine Flotte?
		if( shipd.getInt("fleet") > 0 ) {
			sid = db.query("SELECT * FROM ships WHERE id>0 AND fleet=",shipd.getInt("fleet")," AND battle=0 AND x=",shipd.getInt("x")," AND y=",shipd.getInt("y")," AND system=",shipd.getInt("system")," AND `lock` IS NULL");
		}
		else {
			sid = db.query("SELECT * FROM ships WHERE id>0 AND id=",shipd.getInt("id")," AND battle=0 AND x=",shipd.getInt("x")," AND y=",shipd.getInt("y")," AND system=",shipd.getInt("system")," AND `lock` IS NULL");
		}
		
		while( sid.next() ) {
			SQLResultRow sidRow = sid.getRow();
			
			shiptype = Ships.getShipType(sidRow);
			if( shiptype.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) {
				continue;
			}
			
			shiplist.add(sid.getInt("id"));
				
			// ggf. gedockte Schiffe auch beruecksichtigen
			db.update("UPDATE ships SET battle=",this.id," WHERE id>0 AND battle=0 AND docked IN ('",sid.getInt("id"),"','l ",sid.getInt("id"),"')");
			if( db.affectedRows() > 0 ) {
				SQLQuery sid2 = db.query("SELECT * FROM ships WHERE id>0 AND docked IN ('",sid.getInt("id"),"','l ",sid.getInt("id"),"')");
				while( sid2.next() ) {
					SQLResultRow sid2Row = sid2.getRow();
					
					SQLResultRow stype = Ships.getShipType(sid2Row);
					if( stype.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) {
						db.update("UPDATE ships SET docked='',battle=0 WHERE id=",sid2.getInt("id"));
						continue;
					}
					
					shiplist.add(sid2.getInt("id"));
						
					int sid2Action = 0;
					
					// Das neue Schiff in die Liste der eigenen Schiffe eintragen
					if( !Ships.hasShipTypeFlag(shiptype, Ships.SF_INSTANT_BATTLE_ENTER) && 
						!Ships.hasShipTypeFlag(stype, Ships.SF_INSTANT_BATTLE_ENTER) ) {
						sid2Action = BS_JOIN;
					}
					
					sid2Row.put("action", sid2Action);
					sid2Row.put("side", this.ownSide);
					sid2Row.put("count", stype.getInt("shipcount"));
					this.ownShips.add(sid2Row);
		
					Common.safeIntInc(shipcounts, sid2.getInt("type"));
		
					db.update("INSERT INTO battles_ships ",
								"(shipid,battleid,side,hull,shields,engine,weapons,comm,sensors,action,count) ",
								"VALUES (",sid2.getInt("id"),",",this.id,",",side,",",sid2.getInt("hull"),",",sid2.getInt("shields"),",",sid2.getInt("engine"),",",sid2.getInt("weapons"),",",sid2.getInt("comm"),",",sid2.getInt("sensors"),",",sid2Action,",'",stype.getInt("shipcount"),"')");
				}
				sid2.free();
			}
		
			int sidAction = 0; 
			
			// Das neue Schiff in die Liste der eigenen Schiffe eintragen
			if( !Ships.hasShipTypeFlag(shiptype, Ships.SF_INSTANT_BATTLE_ENTER) ) {
				sidAction = BS_JOIN;
			}

			sidRow.put("action", sidAction);
			sidRow.put("side", this.ownSide);
			sidRow.put("count", shiptype.getInt("shipcount"));
			this.ownShips.add(sid.getRow());
		
			Common.safeIntInc(shipcounts, sid.getInt("type"));
		
			//Battles_ships Eintrag einfuegen
			db.update("INSERT INTO battles_ships ",
						"(shipid,battleid,side,hull,shields,engine,weapons,comm,sensors,action,count) ",
						"VALUES (",sid.getInt("id"),",",this.id,",",side,",",sid.getInt("hull"),",",sid.getInt("shields"),",",sid.getInt("engine"),",",sid.getInt("weapons"),",",sid.getInt("comm"),",",sid.getInt("sensors"),",",sidAction,",'",shiptype.getInt("shipcount"),"')");
		}
		sid.free();
		
		int tick = context.get(ContextCommon.class).getTick();
		
		if( shiplist.size() > 1 ) {
			db.update("UPDATE ships SET battle=",this.id," WHERE id>0 AND id IN (",Common.implode(",",shiplist),") AND battle=0 AND x=",shipd.getInt("x")," AND y=",shipd.getInt("y")," AND system=",shipd.getInt("system"));
		
			int addedShips = shiplist.size();
			this.logenemy("<action side=\""+this.ownSide+"\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\nDie "+log_shiplink(shipd)+" ist zusammen mit "+addedShips+" weiteren Schiffen der Schlacht beigetreten\n]]></action>\n");
			this.logme( "Die "+log_shiplink(shipd)+" ist zusammen mit "+addedShips+" weiteren Schiffen der Schlacht beigetreten\n\n" );
		}
		else {
			this.logenemy("<action side=\""+this.ownSide+"\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\nDie "+log_shiplink(shipd)+" ist der Schlacht beigetreten\n]]></action>\n");
			this.logme("Die "+log_shiplink(shipd)+" ist der Schlacht beigetreten\n\n");
		
			db.update("UPDATE ships SET battle=",this.id," WHERE id>0 AND battle=0 AND id=",shipid);
		}
		
		return true;
	}
	
	/**
	 * Laedt eine Schlacht aus der Datenbank
	 * @param battleID die ID der Schlacht
	 * @param id Die ID des aktiven Spielers
	 * @param ownShipID die ID eines auszuwaehlenden eigenen Schiffes (oder 0)
	 * @param enemyShipID die ID eines auszuwaehlenden gegnerischen Schiffes (oder 0)
	 * @param forcejoin Die ID einer Seite (1 oder 2), welche als die eigene zu waehlen ist. Falls 0 wird automatisch eine gewaehlt
	 * 
	 * @return <code>true</code>, falls die Schlacht erfolgreich geladen wurde
	 */
	public boolean load(int battleID, int id, int ownShipID, int enemyShipID, int forcejoin ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		/*
			TODO:
				- Update der Allys falls der Commander einer Seite einer Ally beitritt (und vorher in keiner war)
				- Update der Schiffe, falls ein Spieler nicht mehr einer der beiden Seiten angehoert (und ggf auch update der Kommandanten)
		*/
		SQLResultRow battledata = db.first( "SELECT * FROM battles WHERE id=",battleID);
	
		if( battledata.isEmpty() ) {
			db.update("UPDATE ships SET battle=0 WHERE id>0 AND battle=",battleID);
			context.addError("Die Schlacht ist bereits zuende!");
			return false;
		}
	
		this.id = battleID;
		this.points[0] = battledata.getInt("com1Points");
		this.points[1] = battledata.getInt("com2Points");
		this.ally[0] = battledata.getInt("ally1");
		this.ally[1] = battledata.getInt("ally2");
		this.commander[0] = battledata.getInt("commander1");
		this.commander[1] = battledata.getInt("commander2");
		this.x = battledata.getInt("x");
		this.y = battledata.getInt("y");
		this.system = battledata.getInt("system");
		this.ready[0] = battledata.getBoolean("ready1");
		this.ready[1] = battledata.getBoolean("ready2");
		this.comMsg[0] = battledata.getString("com1Msg");
		this.comMsg[1] = battledata.getString("com2Msg");
		this.betak[0] = battledata.getBoolean("com1BETAK");
		this.betak[1] = battledata.getBoolean("com2BETAK");
		this.takeCommand[0] = battledata.getInt("takeCommand1");
		this.takeCommand[1] = battledata.getInt("takeCommand2");
		this.blockcount = battledata.getInt("blockcount");
		this.lastturn = battledata.getInt("lastturn");
		this.visibility = battledata.getString("visibility").length() == 0 ? null : battledata.getString("visibility");
		this.quest = battledata.getInt("quest");
		this.guest = false;
		this.flags = battledata.getInt("flags");
		
		this.addCommanders.add(0, new ArrayList<Integer>());
		this.addCommanders.add(1, new ArrayList<Integer>());
	
		this.tableBuffer = battledata;
	
		User auser = context.createUserObject(id);
		
		//
		// Weitere Commander in Folge von Questschlachten feststellen
		//
		if( (this.quest != 0) && !Common.inArray(id,this.commander) && ((this.commander[0] < 0) ^ (this.commander[1] < 0) ) ) {
			if( auser.hasFlag(User.FLAG_QUEST_BATTLES) || auser.getAccessLevel() > 20 ) {
				if( this.commander[0] < 0 )  {
					this.addCommanders.get(0).add(id);
				}
				else {
					this.addCommanders.get(1).add(id);
				}	
			}
		}
	
		//
		// Darf der Spieler (evt als Gast) zusehen?
		//
	
		int forceSide = -1;
	
		if( forcejoin == 0 ) {
			if( ( (auser.getAlly() > 0) && !Common.inArray(auser.getAlly(),this.ally) && !this.isCommander(id) ) ||
				( (auser.getAlly() == 0) && !this.isCommander(id) ) ) {
	
				// Hat der Spieler ein Schiff in der Schlacht
				SQLResultRow aship = db.first("SELECT id,side ",
									 "FROM ships t1 JOIN battles_ships t2 ON t1.id=t2.shipid ",
									 "WHERE t1.id>0 AND t1.owner=",id," AND t2.battleid=",this.id);
				if( !aship.isEmpty() ) {
					forceSide = aship.getInt("side");
				}
				else {
					//Mehr ueber den Spieler herausfinden
					if( auser.getAccessLevel() > 20 ) {
						this.guest = true;
					}
					else if( auser.hasFlag(User.FLAG_VIEW_BATTLES) ) {
						this.guest = true;
					}
					else {
						SQLResultRow shipcount = db.first("SELECT count(*) count FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id ",
												 "WHERE t1.id>0 AND t1.owner=",id," AND t1.x=",this.x," AND t1.y=",this.y," AND ",
												 "t1.system=",this.system," AND t1.battle=0 AND t2.class IN (11,13)");
						if( shipcount.getInt("count") > 0 ) {
							this.guest = true;
						}
						else {
							context.addError("Sie verf&uuml;gen &uuml;ber kein geeignetes Schiff im Sektor um die Schlacht zu verfolgen");
							return false;
						}
					}
				}
			}
		}
		else {
			forceSide = forcejoin - 1;
		}
	
		//
		// Eigene Seite feststellen
		//
	
		if( (auser.getAlly() != 0 && (auser.getAlly() == this.ally[0])) || this.isCommander(id,0) || this.guest || forceSide == 0 ) {
			this.ownSide = 0;
			this.enemySide = 1;
		}
		else if( (auser.getAlly() != 0 && (auser.getAlly() == this.ally[1])) || this.isCommander(id,1) || forceSide == 1 ) {
			this.ownSide = 1;
			this.enemySide = 0;
		}
	
		//
		// Liste aller Schiffe in der Schlacht erstellen
		//
	
		this.ownShips.clear();
		this.enemyShips.clear();
		
		this.ownShipTypeCount.clear();
		this.enemyShipTypeCount.clear();

		SQLQuery aship = db.query( "SELECT t1.*,t2.action,t2.side,t2.count ",
				    			"FROM ships t1 JOIN battles_ships t2 ON t1.id=t2.shipid ",
				    			"WHERE t1.id>0 AND t1.battle=",this.id," AND t2.battleid=",this.id," AND t1.x=",this.x," AND t1.y=",this.y," AND t1.system=",this.system," ",
								"ORDER BY type,id");
	
		while( aship.next() ) {
			if( aship.getInt("side") == this.ownSide ) {
				this.ownShips.add(aship.getRow());
				if( !this.guest || (aship.getString("docked").length() == 0) || (aship.getString("docked").charAt(0) != 'l') ) {
					if( ownShipTypeCount.containsKey(aship.getInt("type")) ) {
						ownShipTypeCount.put(aship.getInt("type"), ownShipTypeCount.get(aship.getInt("type"))+1);
					}
					else {
						ownShipTypeCount.put(aship.getInt("type"), 1);
					}
				}
			}
			else if( aship.getInt("side") == this.enemySide ) {
				this.enemyShips.add(aship.getRow());
				if( (aship.getString("docked").length() == 0) || (aship.getString("docked").charAt(0) != 'l') ) {
					if( enemyShipTypeCount.containsKey(aship.getInt("type")) ) {
						enemyShipTypeCount.put(aship.getInt("type"), enemyShipTypeCount.get(aship.getInt("type"))+1);
					}
					else {
						enemyShipTypeCount.put(aship.getInt("type"), 1);
					}
				}
			}
		}
		aship.free();

	
		//
		// aktive Schiffe heraussuchen
		//
	
		this.activeSEnemy = 0;
		this.activeSOwn = 0;
	
		if( enemyShipID != 0 ) {
			for( int i=0; i < enemyShips.size(); i++ ) {
				if( enemyShips.get(i).getInt("id") == enemyShipID ) {
					this.activeSEnemy = i;
					break;
				}
			}
		}
	
		if( ownShipID != 0 ) {
			for( int i=0; i < ownShips.size(); i++ ) {
				if( ownShips.get(i).getInt("id") == ownShipID ) {
					this.activeSOwn = i;
					break;
				}
			}
		}
	
		// Falls die gewaehlten Schiffe gelandet (oder zerstoert) sind -> neue Schiffe suchen
		while( activeSEnemy < enemyShips.size() &&
				enemyShips.get(activeSEnemy).isEmpty() &&
			  ( (this.enemyShips.get(activeSEnemy).getInt("action") & BS_DESTROYED) != 0 ||
			 	((this.enemyShips.get(activeSEnemy).getString("docked").length() > 0) &&
			 	(this.enemyShips.get(activeSEnemy).getString("docked").charAt(0) == 'l')) ) ) {
			this.activeSEnemy++;
		}
	
		if( activeSEnemy >= enemyShips.size() ) activeSEnemy = 0;

		if( this.guest ) {
			while( activeSOwn < ownShips.size() &&
					ownShips.get(activeSOwn).isEmpty() &&
				  ( (this.ownShips.get(activeSOwn).getString("docked").length() > 0) &&
				 	(this.ownShips.get(activeSOwn).getString("docked").charAt(0) == 'l') ) ) {
				this.activeSOwn++;
			}
		
			if( activeSOwn >= ownShips.size() ) activeSOwn = 0;
		}
		
		return true;
	}

	/**
	 * Loggt eine Nachricht fuer aktuellen Spieler
	 * @param text Die zu loggende Nachricht
	 */
	public void logme( String text ) {
		this.logoutputbuffer.append(text);
	}

	/**
	 * Gibt die fuer den akuellen Spieler anzuzeigenden Nachrichten zurueck
	 * @param raw Sollen die Nachrichten im Rohformat (unformatiert) zurueckgegeben werden?
	 * @return die Nachrichten
	 */
	public String getOwnLog( boolean raw ) {
		if( raw ) {
			return this.logoutputbuffer.toString();
		}
		return StringUtils.replace(this.logoutputbuffer.toString(), "\n", "<br />");
	}

	/**
	 * Loggt eine Nachricht fuer den Gegner/fuer das Kampflog
	 * @param text Die zu loggende Nachricht
	 */
	public void logenemy( String text ) {
		this.logenemybuffer.append(text);
	}

	/**
	 * Gibt die fuer den Gegner/fuer das Kampflog zu speichernden Nachrichten zurueck
	 * @param raw Sollen die Nachrichten im Rohformat (unformatiert) zurueckgegeben werden?
	 * @return die Nachrichten
	 */
	public String getEnemyLog( boolean raw ) {
		if( raw ) {
			return this.logenemybuffer.toString();
		}
		return StringUtils.replace(this.logenemybuffer.toString(), "\n", "<br />");
	}
	
	/**
	 * Leert den Nachrichtenpuffer fuer den Gegner/fuer das Kampflog
	 *
	 */
	public void clearEnemyLog() {
		this.logenemybuffer.setLength(0);
	}
	
	/**
	 * Beendet die laufende Runde und berechnet einen Rundenwechsel
	 * @param calledByUser Wurde das Rundenende (in)direkt durch einen Spieler ausgeloesst? (<code>true</code>)
	 * 
	 * @return <code>true</code>, falls die Schlacht weiterhin existiert. <code>false</code>, falls sie zu beendet wurde.
	 * 
	 */
	public boolean endTurn( boolean calledByUser ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
			
		List<List<SQLResultRow>> sides = new ArrayList<List<SQLResultRow>>();
		if( this.ownSide == 0 ) {
			sides.add(this.ownShips);
			sides.add(this.enemyShips);
		}
		else {
			sides.add(this.enemyShips);
			sides.add(this.ownShips);
		}
		
		//
		// Zuerst die Schiffe berechnen
		//

		for( int i=0; i < 2; i++ ) {
			List<SQLResultRow> destroyList = new ArrayList<SQLResultRow>();
			
			List<SQLResultRow> shiplist = sides.get(i);
			for( int key=0; key < shiplist.size(); key++ ) {
				SQLResultRow aship = shiplist.get(key);
				
				int oldAction = aship.getInt("action");
				SQLResultRow battle_ship = db.first("SELECT hull,shields,engine,weapons,comm,sensors,newcount FROM battles_ships WHERE shipid=",aship.getInt("id"));
				
				if( (battle_ship.getInt("newcount") > 0) && (battle_ship.getInt("newcount") != aship.getInt("count")) ) {
					aship.put("count", battle_ship.getInt("newcount"));
					db.update("UPDATE battles_ships SET count=",battle_ship.getInt("newcount"),",newcount=0 WHERE shipid=",aship.getInt("id"));
				}
				
				if( (aship.getInt("action") & BS_HIT) != 0 ) {	
					db.update("UPDATE ships ",
								"SET hull=",battle_ship.getInt("hull"),", ",
									"shields=",battle_ship.getInt("shields"),", ",
									"engine=",battle_ship.getInt("engine"),", ",
									"weapons=",battle_ship.getInt("weapons"),", ",
									"comm=",battle_ship.getInt("comm"),", ",
									"sensors=",battle_ship.getInt("sensors")," ",
								"WHERE id>0 AND id=",aship.getInt("id") );
	
					aship.put("action", aship.getInt("action") ^ BS_HIT);
				}
				else if( (aship.getInt("action") & BS_DESTROYED) != 0 ) {	
					if( Configuration.getIntSetting("DESTROYABLE_SHIPS") != 0 ) {
						destroyList.add(aship);
	
						continue;
					}
					
					aship.put("action", aship.getInt("action") ^ BS_DESTROYED);
				}
				
				SQLResultRow ashipType = Ships.getShipType( aship );
				
				if( (aship.getInt("action") & BS_FLUCHT) != 0 ) {
					if( ashipType.getInt("cost") > 0 ) {
						this.removeShip(aship, true);
					} 
					else {
						this.removeShip(aship, false);
					}
	
					continue;
				}
				
				if( (aship.getInt("action") & BS_SHOT) != 0 ) {
					aship.put("action", aship.getInt("action") ^ BS_SHOT);
				}
				
				if( (aship.getInt("action") & BS_SECONDROW_BLOCKED) != 0 ) {
					aship.put("action", aship.getInt("action") ^ BS_SECONDROW_BLOCKED);
				}
				
				if( (aship.getInt("action") & BS_BLOCK_WEAPONS) != 0 ) {
					aship.put("action", aship.getInt("action") ^ BS_BLOCK_WEAPONS);
				}
				
				if( (i == 0) && this.hasFlag(FLAG_DROP_SECONDROW_0) && 
						(aship.getInt("action") & BS_SECONDROW) != 0 ) {
					aship.put("action", aship.getInt("action") ^ BS_SECONDROW);	
				}
				else if( (i == 1) && this.hasFlag(FLAG_DROP_SECONDROW_1) && 
						(aship.getInt("action") & BS_SECONDROW) != 0 ) {
					aship.put("action", aship.getInt("action") ^ BS_SECONDROW);	
				}
				
				if( (aship.getInt("action") & BS_JOIN) != 0 ) {
					if( Ships.hasShipTypeFlag(ashipType, Ships.SF_SECONDROW) && 
						this.isSecondRowStable(i, new SQLResultRow[] {aship}) ) {
						aship.put("action", aship.getInt("action") ^ BS_SECONDROW);
					}
					aship.put("action", aship.getInt("action") ^ BS_JOIN);
				}
	
				Map<String,String> heat = Weapons.parseWeaponList(aship.getString("heat"));
	
				for( String weaponName : heat.keySet() ) {
					heat.put(weaponName, "0");
				}
				
				if( (aship.getInt("action") & BS_FLUCHTNEXT) != 0 ) {
					aship.put("action", (aship.getInt("action") ^ BS_FLUCHTNEXT) | BS_FLUCHT);
				}
				
				if( aship.getInt("action") != oldAction ) {
					db.update("UPDATE battles_ships SET action=",aship.getInt("action")," WHERE shipid=",aship.getInt("id"));
				}
	
				db.update("UPDATE ships SET heat='",Weapons.packWeaponList(heat),"',battleAction=0 WHERE id>0 AND id=",aship.getInt("id"));
			}
			
			// Zerstoerte Schiffe aus der Schlacht entfernen
			for( int key=0; key < destroyList.size(); key++ ) {
				this.destroyShip(destroyList.get(key));
			}
			
			int points = this.getActionPoints(i);
			this.points[i] = points;
		}
		
		// Ist die Schlacht zuende (weil keine Schiffe mehr vorhanden sind?)
		int owncount = db.first("SELECT count(*) count FROM battles_ships WHERE battleid=",this.id," AND side=",this.ownSide).getInt("count");
		int enemycount = db.first("SELECT count(*) count FROM battles_ships WHERE battleid=",this.id," AND side=",this.enemySide).getInt("count");

		if( (owncount == 0) && (enemycount == 0) ) {
			User user1 = context.createUserObject(this.commander[this.enemySide]);
			User user2 = context.createUserObject(this.commander[this.ownSide]);

			PM.send(context, this.commander[this.enemySide], this.commander[this.ownSide], "Schlacht unentschieden", "Die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" gegen "+user1.getName()+" wurde mit einem Unendschieden beendet!");
			PM.send(context, this.commander[this.ownSide], this.commander[this.enemySide], "Schlacht unentschieden", "Die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" gegen "+user2.getName()+" wurde mit einem Unendschieden beendet!");

			// Schlacht beenden - unendschieden
			this.endBattle(0,0, true);

			this.ownShips.clear();
			this.enemyShips.clear();

			if( calledByUser ) {
				context.getResponse().getContent().append("Du hast die Schlacht gegen "+Common._title(user1.getName())+" mit einem Unendschieden beendet!");
			}
			return false;
		}
		else if( owncount == 0 ) {
			User user1 = context.createUserObject(this.commander[this.enemySide]);
			User user2 = context.createUserObject(this.commander[this.ownSide]);

			PM.send(context, this.commander[this.enemySide], this.commander[this.ownSide], "Schlacht verloren", "Du hast die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" gegen "+user1.getName()+" verloren!");
			PM.send(context, this.commander[this.ownSide], this.commander[this.enemySide], "Schlacht gewonnen", "Du hast die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" gegen "+user2.getName()+" gewonnen!");
					
			// Schlacht beenden - eine siegreiche Schlacht fuer den aktive Seite verbuchen sowie eine verlorene fuer den Gegner
			if( this.ownSide == 0 ) {
				this.endBattle(-1,1, true);
			}
			else {
				this.endBattle(1,-1, true);
			}

			this.ownShips.clear();
			this.enemyShips.clear();


			if( calledByUser ) {
				context.getResponse().getContent().append("Du hast die Schlacht gegen "+Common._title(user1.getName())+" verloren!");
			}
			return false;
		}
		else if( enemycount == 0 ) {
			User user1 = context.createUserObject(this.commander[this.enemySide]);
			User user2 = context.createUserObject(this.commander[this.ownSide]);

			PM.send(context, this.commander[this.enemySide], this.commander[this.ownSide], "Schlacht gewonnen", "Du hast die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" gegen "+user1.getName()+" gewonnen!");
			PM.send(context, this.commander[this.ownSide], this.commander[this.enemySide], "Schlacht verloren", "Du hast die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" gegen "+user2.getName()+" verloren!");

			// Schlacht beenden - eine siegreiche Schlacht fuer den aktive Seite verbuchen sowie eine verlorene fuer den Gegner
			if( this.ownSide == 0 ) {
				this.endBattle(1,-1, true);
			}
			else {
				this.endBattle(-1,1, true);
			}

			this.ownShips.clear();
			this.enemyShips.clear();

			if( calledByUser ) {
				context.getResponse().getContent().append("Du hast die Schlacht gegen "+Common._title(user1.getName())+" gewonnen!");
			} 
			return false;
		}
		
		this.ready[0] = false;
		this.ready[1] = false;
		this.blockcount = 2;

		this.lastturn = Common.time();

		int tick = context.get(ContextCommon.class).getTick();

		for( int i=0; i < 2; i++ ) {
			if( !calledByUser && this.takeCommand[i] != 0 ) {
				this.logenemy("<action side=\""+i+"\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\n");
	
				User com = context.createUserObject(this.takeCommand[i]);
	
				PM.send(context, this.takeCommand[i], this.commander[i], "Schlacht &uuml;bernommen", "Ich habe die Leitung der Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" &uuml;bernommen.");
	
				this.logenemy("[Automatisch] "+Common._titleNoFormat(com.getName())+" kommandiert nun die gegnerischen Truppen\n\n");
	
				this.commander[i] = this.takeCommand[i];
	
				this.logenemy("]]></action>\n");
	
				this.logenemy("<side"+(i+1)+" commander=\""+this.commander[i]+"\" ally=\""+this.ally[i]+"\" />\n");
	
				this.takeCommand[i] = 0;
			}
		}
		
		int inakt = db.first("SELECT inakt FROM battles WHERE id=",this.id).getInt("inakt");

		if( this.hasFlag(FLAG_FIRSTROUND) ) {
			this.setFlag(FLAG_FIRSTROUND, false);
		}
		
		this.setFlag(FLAG_DROP_SECONDROW_0, false);
		this.setFlag(FLAG_DROP_SECONDROW_1, false);
		this.setFlag(FLAG_BLOCK_SECONDROW_0, false);
		this.setFlag(FLAG_BLOCK_SECONDROW_1, false);

		this.save(true);
		
		// Schiffstick ausfuehren
		context.getRequest().setParameter("battle", Integer.toString(this.id));
		
		TickController schiffstick = new SchiffsTick();
		schiffstick.execute();
		schiffstick.dispose();

		if( inakt > 6 ) {
			PM.send(context, -1, this.commander[0], "Schlacht beendet", "Die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" wurde wegen Inaktivit&auml;t automatisch beendet");
			PM.send(context, -1, this.commander[1], "Schlacht beendet", "Die Schlacht bei "+this.system+" : "+this.x+"/"+this.y+" wurde wegen Inaktivit&auml;t automatisch beendet");
			this.endBattle(0, 0, true);
			 
			if( !calledByUser ) {
				context.getResponse().getContent().append("-> Die Schlacht wurde wegen Inaktivit&auml;t beendet"); 	
			}
			
			return false;
		} 

		db.update("UPDATE battles SET inakt="+(inakt+1)+" WHERE id=",this.id);
		
		return true;
	}
	
	/**
	 * Schreibt das aktuelle Kampflog in die Logdatei
	 */
	public void writeLog() {
		if( (this.ownShips.size() > 0) && (this.enemyShips.size() > 0) ) {
			Common.writeLog("battles/battle_id"+this.id+".log", this.getEnemyLog(true));
		}
		
		Database db = ContextMap.getContext().getDatabase();
		String log = this.getEnemyLog(false);
		
		if( this.ownSide == 0 ) {
			PreparedQuery pq = db.prepare("UPDATE battles SET com2Msg=CONCAT(com2Msg, ?) WHERE id= ?");
			pq.update(log, this.id);
			pq.close();
			this.comMsg[1] += log;
		}
		else {
			PreparedQuery pq = db.prepare("UPDATE battles SET com1Msg=CONCAT(com1Msg, ?) WHERE id= ?");
			pq.update(log, this.id);
			pq.close();
			this.comMsg[1] += log;
		}
	}
	
	/**
	 * Beendet die Schlacht
	 * @param side1points Die Punkte, die die erste Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
	 * @param side2points Die Punkte, die die zweite Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
	 * @param executeScripts Sollen ggf vorhandene Scripte, welche auf das Ende der Schlacht "lauschen" ausgefuehrt werden (<code>true</code>)?
	 */
	public void endBattle( int side1points, int side2points, boolean executeScripts ) {
		this.writeLog();
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		if( executeScripts ) {
			String onendhandler = db.first("SELECT onend FROM battles WHERE id=",this.id).getString("onend");
			if( onendhandler.length() > 0 ) {			
				ScriptParser scriptparser = context.get(ContextCommon.class).getScriptParser(ScriptParser.NameSpace.QUEST);
				if( context.getActiveUser() != null ) {
					User activeuser = context.getActiveUser();
					if( !activeuser.hasFlag(User.FLAG_SCRIPT_DEBUGGING) ) {
						scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
					}
				}
				else {
					scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
				}
				
				Quests.executeEvent( scriptparser, onendhandler, (context.getActiveUser() != null ? context.getActiveUser().getID() : 0), "" );
			}
		}

		db.update("DELETE FROM battles_ships WHERE battleid=",this.id);
		db.update("UPDATE ships SET battle=0,battleAction=0 WHERE id>0 AND battle=",this.id);
		db.update("DELETE FROM battles WHERE id=",this.id);

		Common.writeLog("battles/battle_id"+this.id+".log", "</battle>");

		final String newlog = Configuration.getSetting("LOXPATH")+"battles/ended/"+Common.time()+"_id"+this.id+".log";
		new File(Configuration.getSetting("LOXPATH")+"battles/battle_id"+this.id+".log").renameTo(new File(newlog));
		
		db.update("UPDATE ships_lost SET battle=0,battlelog='",newlog,"' WHERE battle=",this.id);

		if( this.ally[0] != 0 ) {
			if( side1points > 0 ) {
				db.update("UPDATE ally SET wonBattles=wonBattles + "+side1points+" WHERE id=",this.ally[0]);
			} 
			else {
				db.update("UPDATE ally SET lostBattles=lostBattles + ",-side1points," WHERE id=",this.ally[0]);
			}
		}
		if( side1points > 0 ) {
			db.update("UPDATE users SET wonBattles=wonBattles + "+side1points+" WHERE id=",this.commander[0]);
		} 
		else {
			db.update("UPDATE users SET lostBattles=lostBattles + ",-side1points," WHERE id=",this.commander[0]);
		}

		if( this.ally[1] != 0 ) {
			if( side2points > 0 ) {
				db.update("UPDATE ally SET wonBattles=wonBattles + "+side2points+" WHERE id=",this.ally[1]);
			} 
			else {
				db.update("UPDATE ally SET lostBattles=lostBattles + ",-side2points," WHERE id=",this.ally[1]);
			}
		}
		if( side2points > 0 ) {
			db.update("UPDATE ally SET wonBattles=wonBattles + "+side2points+" WHERE id=",this.commander[1]);
		} 
		else {
			db.update("UPDATE ally SET lostBattles=lostBattles + ",-side2points," WHERE id=",this.commander[1]);
		}

		this.enemyShips.clear();
		this.ownShips.clear();
	}

	/**
	 * Zerstoert ein Schiff und alle an ihm gedockten Schiff
	 * @param ship Das zu zerstoerende Schiff
	 */
	private void destroyShip( SQLResultRow ship ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		int side = db.first("SELECT side FROM battles_ships WHERE shipid=",ship.getInt("id")).getInt("side");
		int dockcount = db.first("SELECT count(*) count FROM ships WHERE docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')").getInt("count");
		
		db.update("DELETE FROM battles_ships WHERE shipid=",ship.getInt("id"));
		
		Ships.destroy( ship.getInt("id") );

		//
		// Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
		//

		boolean found = false;
		List<SQLResultRow> shiplist = this.ownShips;
		
		if( side != this.ownSide ) {
			shiplist = this.enemyShips;
		}
	
		for( int i=0; i < shiplist.size(); i++ ) {
			SQLResultRow aship = shiplist.get(i);
			
			if( aship.getInt("id") == ship.getInt("id") ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				found = true;
			}
			// Evt ist das Schiff an das gerade zerstoerte gedockt
			// In diesem Fall muss es ebenfalls entfernt werden
			else if( (dockcount > 0) && (aship.getString("docked").length() > 0) &&
					(aship.getString("docked").equals("l "+ship.getInt("id")) || aship.getString("docked").equals(Integer.toString(ship.getInt("id")))) ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				dockcount--;
				
				Ships.destroy( aship.getInt("id") );
				db.update("DELETE FROM battles_ships WHERE shipid=",aship.getInt("id"));
			}
			
			if( found && (dockcount == 0) ) {
				break;
			}
		}
		
		if( (side == this.enemySide) && ((this.activeSEnemy >= shiplist.size()) || shiplist.get(this.activeSEnemy).isEmpty()) ) {
			this.activeSEnemy = 0;
		}
		else if( (side == this.ownSide) && ((this.activeSOwn >= shiplist.size()) || shiplist.get(this.activeSOwn).isEmpty()) ) {
			this.activeSOwn = 0;
		}
	}
	
	/**
	 * Entfernt ein Schiff aus einer Schlacht und platziert es falls gewuenscht in einem zufaelligen Sektor
	 * um die Schlacht herum. Evt gedockte Schiffe werden mitentfernt und im selben Sektor platziert
	 * @param ship Das fliehende Schiff
	 * @param relocate Soll ein zufaelliger Sektor um die Schlacht herum gewaehlt werden? (<code>true</code>)
	 */
	private void removeShip( SQLResultRow ship, boolean relocate ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		Location loc = Location.fromResult(ship);
		
		if( relocate && (ship.getString("docked").length() == 0) ) {
			StarSystem sys = Systems.get().system(this.system);
			int maxRetries = 100;

			while( ((loc.getX() == this.x) && (loc.getY() == this.y)) ||
					(loc.getX() < 1) || (loc.getY() < 1) || 
					(loc.getX() > sys.getWidth()) ||
					(loc.getY() > sys.getHeight()) ) {
				loc = loc.setX(this.x + RandomUtils.nextInt(3) - 1);
				loc = loc.setY(this.y + RandomUtils.nextInt(3) - 1);
				
				maxRetries--;
				if( maxRetries == 0 ) {
					break;
				}
			}
		}

		int side = db.first("SELECT side FROM battles_ships WHERE shipid=",ship.getInt("id")).getInt("side");
		int dockcount = db.first("SELECT count(*) count FROM ships WHERE docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')").getInt("count");
		
		db.update("UPDATE ships SET battle=0,battleAction=0,x=",loc.getX(),",y=",loc.getY()," WHERE id>0 AND id=",ship.getInt("id"));
		db.update("DELETE FROM battles_ships WHERE shipid=",ship.getInt("id"));

		Ships.recalculateShipStatus(ship.getInt("id"));

		//
		// Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
		//

		boolean found = false;
		List<SQLResultRow> shiplist = this.ownShips;
		
		if( side != this.ownSide ) {
			shiplist = this.enemyShips;
		}
		
		for( int i=0; i < shiplist.size(); i++ ) {
			SQLResultRow aship = shiplist.get(i);
			
			if( aship.getInt("id") == ship.getInt("id") ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				found = true;
			}
			// Evt ist das Schiff an das gerade fliehende gedockt
			// In diesem Fall muss es ebenfalls entfernt werden
			else if( (dockcount > 0) && (aship.getString("docked").length() > 0) &&
					(aship.getString("docked").equals("l "+ship.getInt("id")) || aship.getString("docked").equals(Integer.toString(ship.getInt("id")))) ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				dockcount--;
				
				db.update("UPDATE ships SET battle=0,battleAction=0,x=",loc.getX(),",y=",loc.getY()," WHERE id>0 AND id=",aship.getInt("id"));
				db.update("DELETE FROM battles_ships WHERE shipid=",aship.getInt("id"));
				
				Ships.recalculateShipStatus(aship.getInt("id"));
			}
			
			if( found && (dockcount == 0) ) {
				break;
			}
		}
		
		if( (side == this.enemySide) && ((this.activeSEnemy >= shiplist.size()) || shiplist.get(this.activeSEnemy).isEmpty()) ) {
			this.activeSEnemy = 0;
		}
		else if( (side == this.ownSide) && ((this.activeSOwn >= shiplist.size()) || shiplist.get(this.activeSOwn).isEmpty()) ) {
			this.activeSOwn = 0;
		}
	}
	
	/**
	 * Fuegt einen Benutzer der Sichtbarkeit der Schlacht hinzu.
	 * Der Benutzer kann somit die Schlacht fortan sehen
	 * @param userid Die ID des hinzuzufuegenden Benutzers
	 */
	public void addToVisibility( int userid ) {
		if( visibility.length() > 0 ) {
			visibility += ","+userid;
		}
		else {
			visibility = Integer.toString(userid);
		}
	}
	
	/**
	 * Setzt die ID des mit der Schlacht verknuepften Quests
	 * @param quest Die ID des mit der Schlacht verknuepften Quests
	 */
	public void setQuest( int quest ) {
		this.quest = quest;
	}
	
	/**
	 * Prueft, ob ein Spieler Kommandant der Schlacht ist
	 * @param id Die ID des Spielers
	 * @return <code>true</code>, falls er Kommandant ist
	 */
	public boolean isCommander( int id ) {
		return isCommander(id, -1);
	}
	
	/**
	 * Prueft, ob ein Spieler auf einer Seite Kommandant ist
	 * @param id Die ID des Spielers
	 * @param side Die Seite oder <code>-1</code>, falls die Seite egal ist
	 * @return <code>true</code>, falls er Kommandant ist
	 */
	public boolean isCommander( int id, int side ) {
		int myside = -1;
		
		if( (this.commander[0] == id) || this.addCommanders.get(0).contains(id) ) {
			myside = 0;
		}
		else if( (this.commander[1] == id) || this.addCommanders.get(1).contains(id) ) {
			myside = 1;	
		}
		
		if( side == -1 ) {
			return myside != -1;	
		}
		
		return (myside == side);
	}

	/**
	 * Gibt die ID der aktuell ausgewaehlten gegnerischen Schiffsgruppe zurueck
	 * @return Die ID der aktuellen gegnerischen Schiffsgruppe
	 */
	public String getEnemyShipGroup() {
		return enemyShipGroup;
	}

	/**
	 * Setzt die ID der aktuell ausgewaehlten gegnerischen Schiffsgruppe
	 * @param enemyShipGroup Die neue Schiffsgruppen-ID
	 */
	public void setEnemyShipGroup(String enemyShipGroup) {
		this.enemyShipGroup = enemyShipGroup;
	}

	/**
	 * Gibt die ID der aktuell ausgewaehlten eigenen Schiffsgruppe zurueck
	 * @return Die ID der aktuellen eigenen Schiffsgruppe
	 */
	public String getOwnShipGroup() {
		return ownShipGroup;
	}

	/**
	 * Setzt die ID der aktuell ausgewaehlten eigenen Schiffsgruppe
	 * @param ownShipGroup Die neue Schiffsgruppen-ID
	 */
	public void setOwnShipGroup(String ownShipGroup) {
		this.ownShipGroup = ownShipGroup;
	}

	/**
	 * Gibt die ID der gegnerischen Seite zurueck
	 * @return Die ID der gegnerischen Seite
	 */
	public int getEnemySide() {
		return enemySide;
	}

	/**
	 * Gibt die ID der eigenen Seite zurueck
	 * @return Die ID der eigenen Seite
	 */
	public int getOwnSide() {
		return ownSide;
	}
	
	/**
	 * Gibt die Liste der gegnerischen Schiffe zurueck
	 * @return Die Liste der gegnerischen Schiffe
	 */
	public List<SQLResultRow> getEnemyShips() {
		return enemyShips;
	}
	
	/**
	 * Gibt das gegnerische Schiff mit dem angegebenen Index (nicht ID!) zurueck
	 * @param index Der Index
	 * @return Das Schiff
	 */
	public SQLResultRow getEnemyShip( int index ) {
		return enemyShips.get(index);
	}

	/**
	 * Gibt die Liste der eigenen Schiffe zurueck
	 * @return Die Liste der eigenen Schiffe
	 */
	public List<SQLResultRow> getOwnShips() {
		return ownShips;
	}
	
	/**
	 * Gibt das eigene Schiff mit dem angegebenen Index (nicht ID!) zurueck
	 * @param index Der Index
	 * @return Das Schiff
	 */
	public SQLResultRow getOwnShip( int index ) {
		return ownShips.get(index);
	}
	
	/**
	 * Gibt die Aktionspunkte einer Seite zurueck
	 * @param side Die Seite
	 * @return Die Anzahl der Aktionspunkte der Seite
	 */
	public int getPoints(int side) {
		return this.points[side];
	}
	
	/**
	 * Setzt die Aktionspunkte einer Seite auf einen neuen Wert
	 * @param side Die Seite
	 * @param points Die neue Anzahl an Aktionspunkten
	 */
	public void setPoints(int side, int points) {
		this.points[side] = points;
	}
	
	/**
	 * Gibt die mit einer Seite assoziierte Allianz zurueck
	 * @param side Die Seite
	 * @return Die ID der Allianz oder 0
	 */
	public int getAlly(int side) {
		return this.ally[side];
	}
	
	/**
	 * Gibt die Spieler-ID des Kommandanten einer Seite zurueck
	 * @param side Die Seite
	 * @return Die Spieler-ID
	 */
	public int getCommander(int side) {
		return this.commander[side];
	}
	
	/**
	 * Setzt den Kommandaten einer Seite
	 * @param side Die Seite
	 * @param id Der neue Kommandant
	 */
	public void setCommander(int side, int id) {
		this.commander[side] = id;
	}
	
	/**
	 * Prueft, ob eine Seite mit ihren Aktionen in der Runde fertig ist
	 * @param side Die Seite
	 * @return <code>true</code>, falls sie mit ihren Aktionen in der Runde fertig ist
	 */
	public boolean isReady(int side) {
		return this.ready[side];
	}
	
	/**
	 * Setzt den "fertig"-Status einer Seite fuer die aktuelle Runde
	 * @param side Die Seite
	 * @param ready Der Status
	 */
	public void setReady(int side, boolean ready) {
		this.ready[side] = ready;
	}
	
	/**
	 * Gibt die ID des Spielers zurueck, der auf einer Seite das Kommando uebernehmen will. Sollte
	 * kein solcher Spieler existieren, so wird 0 zurueckgegeben.
	 * @param side Die Seite
	 * @return Die ID des Spielers oder 0
	 */
	public int getTakeCommand(int side) {
		return this.takeCommand[side];
	}
	
	/**
	 * Setzt die ID eines Spielers, welcher auf einer Seite das Kommando uebernehmen will
	 * @param side Die Seite
	 * @param id Die ID des Spielers
	 */
	public void setTakeCommand(int side, int id) {
		this.takeCommand[side] = id;
	}
	
	/**
	 * Gibt eine Map fuer eine Seite zurueck, welche als Schluessel Schiffstypen und als zugeordnete
	 * Werte die Anzahl der vorkommenden Schiffes des Typs auf der Seite hat
	 * @param side Die Seite
	 * @return Eine Map, welche angibt, wie haeufig ein Schiffstyp auf einer Seite vorkommt
	 */
	public Map<Integer,Integer> getShipTypeCount( int side ) {
		if( side == this.ownSide ) {
			return ownShipTypeCount;
		}
		return enemyShipTypeCount;
	}
	
	/**
	 * Gibt zurueck, wie oft ein Schiffstyp auf einer Seite vorkommt
	 * @param side Die ID der Seite
	 * @param shiptype Die ID des Schifftyps
	 * @return Die Anzahl der vorkommenden Schiffe des Schifftyps
	 */
	public int getShipTypeCount( int side, int shiptype ) {
		Integer count = (side == this.ownSide ? ownShipTypeCount.get(shiptype) : enemyShipTypeCount.get(shiptype));
		if( count == null ) {
			return 0;
		}
		return count;
	}
	
	/**
	 * Gibt zurueck, wie oft ein Schiffstyp auf der eigenen Seite vorkommt
	 * @param shiptype Die ID des Schifftyps
	 * @return Die Anzahl der vorkommenden Schiffe des Schifftyps 
	 */
	public int getOwnShipTypeCount(int shiptype) {
		return getShipTypeCount(this.ownSide, shiptype);
	}
	
	/**
	 * Gibt zurueck, wie oft ein Schiffstyp auf der gegnerischen Seite vorkommt
	 * @param shiptype Die ID des Schifftyps
	 * @return Die Anzahl der vorkommenden Schiffe des Schifftyps 
	 */
	public int getEnemyShipTypeCount(int shiptype) {
		return getShipTypeCount(this.enemySide, shiptype);
	}
}
