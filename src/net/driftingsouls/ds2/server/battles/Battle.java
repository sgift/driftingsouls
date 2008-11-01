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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.script.ScriptEngine;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.tick.regular.SchiffsTick;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Repraesentiert eine Schlacht in DS
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="battles")
public class Battle implements Locatable {
	private static final Log log = LogFactory.getLog(Battle.class);
	
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
	
	@Id @GeneratedValue
	private int id;
	private int x;
	private int y;
	private int system;
	private int ally1;
	private int ally2;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="commander1", nullable=false)
	private User commander1;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="commander2", nullable=false)
	private User commander2;
	private boolean ready1;
	private boolean ready2;
	private String com1Msg = "";
	private String com2Msg = "";
	private boolean com1BETAK = true;
	private boolean com2BETAK = true;
	private int takeCommand1;
	private int takeCommand2;
	private int blockcount = 2;
	@SuppressWarnings("unused")
	private long lastaction;
	private long lastturn;
	private int flags;
	private int inakt;
	private String onend;
	private String visibility;
	private Integer quest;
	
	@Version
	private int version;
	
	@Transient
	private String ownShipGroup = "0";
	@Transient
	private String enemyShipGroup = "0";
	
	@Transient
	private int ownSide;
	@Transient
	private int enemySide;
	
	@Transient
	private List<BattleShip> ownShips = new ArrayList<BattleShip>();
	@Transient
	private List<BattleShip> enemyShips = new ArrayList<BattleShip>();
	
	@Transient
	private List<List<Integer>> addCommanders = new ArrayList<List<Integer>>();

	@Transient
	private boolean guest = false;
	
	@Transient
	private Map<Integer,Integer> ownShipTypeCount = new HashMap<Integer,Integer>();
	@Transient
	private Map<Integer,Integer> enemyShipTypeCount = new HashMap<Integer,Integer>();
	
	@Transient
	private int activeSOwn = 0;
	@Transient
	private int activeSEnemy = 0;
	
	@Transient
	private StringBuilder logoutputbuffer = new StringBuilder();
	@Transient
	private StringBuilder logenemybuffer = new StringBuilder();
	
	/**
	 * Generiert eine Stringrepraesentation eines Schiffes, welche
	 * in KS-Logs geschrieben werden kann
	 * @param ship Das Schiff
	 * @return Die Stringrepraesentation
	 */
	public static String log_shiplink( Ship ship ) {
		ShipTypeData shiptype = ship.getTypeData();

		return "[tooltip="+shiptype.getNickname()+"]"+ship.getName()+"[/tooltip] ("+ship.getId()+")";
	}
	
	/**
	 * Konstruktor
	 *
	 */
	public Battle() {
		this.addCommanders.add(0, new ArrayList<Integer>());
		this.addCommanders.add(1, new ArrayList<Integer>());
	}
	
	/**
	 * Gibt die ID der Schlacht zurueck
	 * @return die ID
	 */
	public int getId() {
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
	public boolean isSecondRowStable( int side, BattleShip ... added ) {
		List<BattleShip> shiplist = null;
		if( side == this.ownSide ) {
			shiplist = getOwnShips();
		}
		else {
			shiplist = getEnemyShips();
		}
		
		double owncaps = 0;
		double secondrowcaps = 0;
		for( int i=0; i < shiplist.size(); i++ ) 
		{
			BattleShip aship = shiplist.get(i);
			
			if( (aship.getAction() & BS_JOIN) != 0 || (aship.getAction() & BS_FLUCHT) != 0 ) {
				continue;	
			}
			ShipTypeData type = aship.getTypeData();
			
			double size = type.getSize();
			if( (aship.getAction() & BS_SECONDROW) != 0 ) {
				if( aship.getShip().getDocked().length() == 0 ) {
					secondrowcaps += size;
				}
			}
			else {
				if( size > ShipType.SMALL_SHIP_MAXSIZE ) {
					double countedSize = size;
					if( type.getCrew() > 0 ) {
						countedSize *= (aship.getCrew()/((double)type.getCrew()));
					}
					owncaps += countedSize;
				}
			}
		}
		
		if( added != null ) {
			for( int i=0; i < added.length; i++ ) {
				BattleShip aship = added[i];
				
				ShipTypeData type = aship.getTypeData();
				
				if( !type.hasFlag(ShipTypes.SF_SECONDROW) ) {
					continue;
				}
				
				if( aship.getShip().getDocked().length() == 0 ) {
					secondrowcaps += type.getSize();
				}	
			}
		}

		if( Double.valueOf(secondrowcaps).intValue() == 0 ) {
			return true;	
		}
				
		if( Double.valueOf(owncaps).intValue() <= Double.valueOf(secondrowcaps).intValue()*2 ) {
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
		if( side == 0 ) {
			this.com1Msg += text;
			this.com1Msg = StringUtils.right(this.com1Msg, 10000);
		}
		else {
			this.com2Msg += text;
			this.com2Msg = StringUtils.right(this.com2Msg, 10000);
		}
	}
	
	/**
	 * Leert den Nachrichtenpuffer fuer die angegebene Seite
	 * @param side Die Seite oder <code>-1</code> fuer die eigene Seite
	 */
	public void clearComMessageBuffer( int side ) {
		if( side == -1 ) {
			side = this.ownSide;
		}

		if( side == 0 ) {
			this.com1Msg = "";
		}
		else {
			this.com2Msg = "";
		}
	}
	
	/**
	 * Gibt den Inhalt des Nachrichtenpuffers der angegebenen Seite zurueck
	 * @param side Die Seite
	 * @return Der Nachrichtenpuffer
	 */
	public String getComMessageBuffer( int side ) {
		return side == 0 ? this.com1Msg : this.com2Msg;
	}

	/**
	 * Gibt den Betak-Status einer Seite zurueck
	 * @param side Die Seite
	 * @return <code>true</code>, falls die Seite noch nicht gegen die Betak verstossen hat
	 */
	public boolean getBetakStatus( int side ) {
		return side == 0 ? this.com1BETAK : this.com2BETAK;	
	}
	
	/**
	 * Setzt den Betak-Status einer Seite
	 * @param side Die Seite
	 * @param status Der neue Betak-Status
	 */
	public void setBetakStatus( int side, boolean status ) {
		if( side == 0 ) {
			this.com1BETAK = status;
		}
		else {
			this.com2BETAK = status;
		}
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
	public BattleShip getOwnShip() {
		return this.ownShips.get(this.activeSOwn);
	}

	/**
	 * Gibt das aktuell ausgewaehlte gegnerische Schiff zurueck
	 * @return Das aktuell ausgewaehlte gegnerische Schiff
	 */
	public BattleShip getEnemyShip() {
		return this.enemyShips.get(this.activeSEnemy);
	}
	
	/**
	 * Prueft, ob das aktuell ausgewaehlte gegnerische Schiff ein gueltiges Ziel ist
	 * (das Schiff also existiert). Bei gueltigen Zielen ist ein aufruf von {@link #getEnemyShip()}
	 * gefahrlos moeglich.
	 * @return <code>true</code>, falls das Ziel gueltig ist
	 */
	public boolean isValidTarget() {
		if( this.activeSEnemy >= this.enemyShips.size() ) {
			return false;
		}
		if( this.activeSEnemy < 0 ) {
			return false;
		}
		return true;
	}
	
	/**
	 * Liefert den Index des naechsten feindlichen Schiffes nach dem aktuell ausgewaehlten. 
	 * Bevorzugt werden Schiffe gleichen Typs
	 * @return Der Index des naechsten passenden Schiffes
	 */
	public int getNewTargetIndex() {
		// Falls das aktuelle Schiff ungueltig, dann das erstbeste in der Liste zurueckgeben
		if( !this.isValidTarget() ) {
			for( int i=0; i < enemyShips.size(); i++ ) {
				BattleShip aship = enemyShips.get(i);
				
				if( ((aship.getShip().getDocked().length() == 0) || (aship.getShip().getDocked().charAt(0) != 'l')) && 
						(aship.getAction() & Battle.BS_DESTROYED) == 0 && (aship.getAction() & Battle.BS_SECONDROW) == 0 ) {
					return i;
				}
			}
			
			return 0;
		}
		
		boolean foundOld = false;
			
		BattleShip enemyShip = this.enemyShips.get(this.activeSEnemy);
		
		// Schiff gleichen Typs hinter dem aktuellen Schiff suchen
		List<BattleShip> enemyShips = getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			BattleShip aship = enemyShips.get(i);
			if( !foundOld && (aship.getId() == enemyShip.getId()) ) {
				foundOld = true;	
			}
			else if( foundOld && (aship.getShip().getType() == enemyShip.getShip().getType()) && 
					((aship.getShip().getDocked().length() == 0) || (aship.getShip().getDocked().charAt(0) != 'l')) && 
					(aship.getAction() & Battle.BS_DESTROYED) == 0 && (aship.getAction() & Battle.BS_SECONDROW) == 0 ) {
				return i;
			}
		}
	
		// Schiff gleichen Typs vor dem aktuellen Schiff suchen
		for( int i=0; i < enemyShips.size(); i++ ) {
			BattleShip aship = enemyShips.get(i);
			if( aship.getId() == enemyShip.getId() ) {
				break;
			}
			if( (aship.getShip().getType() == enemyShip.getShip().getType()) && 
					((aship.getShip().getDocked().length() == 0) || (aship.getShip().getDocked().charAt(0) != 'l')) && 
					(aship.getAction() & Battle.BS_DESTROYED) == 0 && (aship.getAction() & Battle.BS_SECONDROW) == 0 ) {
				return i;
			}
		}
	
		// Irgendein nicht gelandetes Schiff suchen
		for( int i=0; i < enemyShips.size(); i++ ) {
			BattleShip aship = enemyShips.get(i);
			if( aship.getId() == enemyShip.getId() ) {
				continue;
			}
			
			if( ((aship.getShip().getDocked().length() == 0) || (aship.getShip().getDocked().charAt(0) != 'l')) && 
					(aship.getAction() & Battle.BS_DESTROYED) == 0 && (aship.getAction() & Battle.BS_SECONDROW) == 0 ) {
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
	 * Setzt den Inaktivitaetszaehler der Schlacht zurueck
	 */
	public void resetInactivity(  ) {	
		this.inakt = 0;
	}
	
	/**
	 * Erstellt eine neue Schlacht
	 * @param id Die ID des Spielers, der die Schlacht beginnt
	 * @param ownShipID Die ID des Schiffes des Spielers, der angreift
	 * @param enemyShipID Die ID des angegriffenen Schiffes
	 * @return Die Schlacht, falls sie erfolgreich erstellt werden konnte. Andernfalls <code>null</code>
	 */
	public static Battle create( int id, int ownShipID, int enemyShipID ) {
		return create(id, ownShipID, enemyShipID, false);
	}
	
	private static void insertShipsIntoDatabase(Battle battle, List<BattleShip> ships, List<Integer> startlist, List<Integer> idlist)
	{
		Set<Integer> addedships = new TreeSet<Integer>();
		StringBuilder buildIn = new StringBuilder();
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		for( int i=0; i < ships.size(); i++ ) {
			BattleShip ship = ships.get(i);
			
			Ship baseShip = ship.getShip().getBaseShip();
			if( (baseShip != null) && 
				(ship.getDocked().charAt(0) == 'l') && (baseShip.startFighters())) 
			{
				ship.getShip().setDocked("");
				startlist.add(ship.getId());
			}
			idlist.add(ship.getId());
			buildIn.append(ship.getShip().getId() + ",");
			addedships.add(ship.getShip().getId());
			
			ship.setBattle(battle);
			db.persist(ship);
		}
		
		String in = buildIn.substring(0, buildIn.length() - 1); //Remove last ,
		
		//Set Parameter doesn't work for in (cast problem string/integer)
		db.createQuery("update Ship set battle=:battle where id in ("+in+")")
		  .setParameter("battle", battle)
		  .executeUpdate();
	}

	/**
	 * Erstellt eine neue Schlacht
	 * @param id Die ID des Spielers, der die Schlacht beginnt
	 * @param ownShipID Die ID des Schiffes des Spielers, der angreift
	 * @param enemyShipID Die ID des angegriffenen Schiffes
	 * @param startOwn <code>true</code>, falls eigene gelandete Schiffe starten sollen
	 * @return Die Schlacht, falls sie erfolgreich erstellt werden konnte. Andernfalls <code>null</code>
	 */
	public static Battle create( int id, int ownShipID, int enemyShipID, final boolean startOwn ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		log.info("battle: "+id+" :: "+ownShipID+" :: "+enemyShipID);
		// Kann der Spieler ueberhaupt angreifen (Noob-Schutz?)
		User user = (User)context.getDB().get(User.class, id);
		if( user.isNoob() ) {
			context.addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher keinen Gegner angreifen!<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden");
			return null;
		}
		
		Ship tmpOwnShip = (Ship)db.get(Ship.class, ownShipID);
		Ship tmpEnemyShip = (Ship)db.get(Ship.class, enemyShipID);

		if( (tmpOwnShip == null) || (tmpOwnShip.getId() < 0) || (tmpOwnShip.getOwner() != user) ) {
			context.addError("Das angreifende Schiff existiert nicht oder untersteht nicht ihrem Kommando!");
			return null;
		}
		
		if( (tmpEnemyShip == null) || (tmpEnemyShip.getId() < 0) ) {
			context.addError("Das angegebene Zielschiff existiert nicht!");
			return null;
		}
		
		if( !tmpOwnShip.getLocation().sameSector(0,tmpEnemyShip.getLocation(),0) ) {
			context.addError("Die beiden Schiffe befinden sich nicht im selben Sektor");
			return null;
		}

		//
		// Kann der Spieler angegriffen werden (NOOB-Schutz?/Vac-Mode?)
		//

		User enemyUser = tmpEnemyShip.getOwner();
		if( enemyUser.isNoob() ) {
			context.addError("Der Gegner steht unter GCP-Schutz und kann daher nicht angegriffen werden!");
			return null;
		}
		
		if( enemyUser.getVacationCount() != 0 && enemyUser.getWait4VacationCount() == 0 ) {
			context.addError("Der Gegner befindet sich im Vacation-Modus und kann daher nicht angegriffen werden!");
			return null;
		}
		
		//
		// IFF-Stoersender?
		// 
		boolean disable_iff = tmpEnemyShip.getStatus().indexOf("disable_iff") > -1;
		if( disable_iff ) {
			context.addError("Dieses Schiff kann nicht angegriffen werden (egal wieviel du mit der URL rumspielt!)");
			return null;	
		}
		
		//
		// Questlock?
		// 
		if( tmpOwnShip.getLock() != null && tmpOwnShip.getLock().length() > 0 ) {
			context.addError("Ihr Schiff ist an ein Quest gebunden");
			return null;
		}
		
		if( tmpEnemyShip.getLock() != null && tmpEnemyShip.getLock().length() > 0 ) {
			context.addError("Das gegnerische Schiff ist an ein Quest gebunden");
			return null;
		}

		//
		// Schiffsliste zusammenstellen
		//
		Battle battle = new Battle();
		
		battle.ownSide = 0;
		battle.enemySide = 1;
		
		BattleShip enemyBattleShip = null;
		BattleShip ownBattleShip = null;
		
		Set<User> ownUsers = new HashSet<User>();
		Set<User> enemyUsers = new HashSet<User>();
		List<?> shiplist = db.createQuery("from Ship as s inner join fetch s.owner as u "+
				"where s.id>:minid and s.x=:x and s.y=:y and " +
				"s.system=:system and s.battle is null and " +
				"(ncp(u.ally,:ally1)=1 or " + 
				"ncp(u.ally,:ally2)=1) and " +
				"locate('disable_iff',s.status)=0 and s.lock is null and (u.vaccount=0 or u.wait4vac > 0)")
			.setInteger("minid", 0)
			.setInteger("x", tmpOwnShip.getX())
			.setInteger("y", tmpOwnShip.getY())
			.setInteger("system", tmpOwnShip.getSystem())
			.setParameter("ally1", tmpOwnShip.getOwner().getAlly())
			.setParameter("ally2", tmpEnemyShip.getOwner().getAlly())
			.list();
							   		
		for( Iterator<?> iter=shiplist.iterator(); iter.hasNext(); ) {
			Ship aShip = (Ship)iter.next();
			// Loot-Truemmer sollten in keine Schlacht wandern... (nicht schoen, gar nicht schoen geloest)
			if( (aShip.getOwner().getId() == -1) && (aShip.getType() == Configuration.getIntSetting("CONFIG_TRUEMMER")) ) {
				continue;
			}
			User tmpUser = aShip.getOwner();
					
			if( tmpUser.isNoob() ) {
				continue;
			}
			
			BattleShip battleShip = new BattleShip(null, aShip);
			
			ShipTypeData shiptype = aShip.getBaseType();
			if( (aShip.getDocked().length() == 0) && shiptype.hasFlag(ShipTypes.SF_SECONDROW) ) {
				battleShip.setAction(BS_SECONDROW);
			}
			
			if( (shiptype.getShipClass() == ShipClasses.GESCHUETZ.ordinal()) && aShip.getDocked().length() > 0 ) {
				battleShip.setAction(battleShip.getAction() | BS_DISABLE_WEAPONS);
			}
			
			if( ( (tmpOwnShip.getOwner().getAlly() != null) && (tmpUser.getAlly() == tmpOwnShip.getOwner().getAlly()) ) || (id == tmpUser.getId()) ) {
				ownUsers.add(tmpUser);
				battleShip.setSide(0);
				
				if( (ownShipID != 0) && (aShip.getId() == ownShipID) ) {
					ownBattleShip = battleShip;
				} 
				else {
					battle.ownShips.add(battleShip);
				}
			} 
			else if( ( (tmpEnemyShip.getOwner().getAlly() != null) && (tmpUser.getAlly() == tmpEnemyShip.getOwner().getAlly()) ) || (tmpEnemyShip.getOwner().getId() == tmpUser.getId()) ) {
				enemyUsers.add(tmpUser);
				battleShip.setSide(1);
				
				if( (enemyShipID != 0) && (aShip.getId() == enemyShipID) ) {
					enemyBattleShip = battleShip;
				} 
				else {
					battle.enemyShips.add(battleShip);
				}
			}

		}
		
		tmpOwnShip = null;
		tmpEnemyShip = null;

		//
		// Schauen wir mal ob wir was sinnvolles aus der DB gelesen haben
		// - Wenn nicht: Abbrechen
		//
		
		if( ownBattleShip == null ) {
			context.addError("Offenbar liegt ein Problem mit dem von ihnen angegebenen Schiff oder ihrem eigenen Schiff vor (wird es evt. bereits angegriffen?)");
			return null;
		}
		
		if( enemyBattleShip == null && (battle.enemyShips.size() == 0) ) {
			context.addError("Offenbar liegt ein Problem mit den feindlichen Schiffen vor (es gibt n&aumlmlich keine die sie angreifen k&ouml;nnten)");
			return null;
		}
		else if( enemyBattleShip == null ) {
			context.addError("Offenbar liegt ein Problem mit den feindlichen Schiffen vor (es gibt zwar welche, jedoch fehlt das Zielschiff)");
			return null;
		}
		
		//
		// Schlacht in die DB einfuegen
		//

		Ship ownShip = ownBattleShip.getShip();
		battle.x = ownShip.getX();
		battle.y = ownShip.getY();
		battle.system = ownShip.getSystem();
		battle.ally1 = ownBattleShip.getOwner().getAlly() != null ? ownBattleShip.getOwner().getAlly().getId() : 0;
		battle.ally2 = enemyBattleShip.getOwner().getAlly() != null ? enemyBattleShip.getOwner().getAlly().getId() : 0;
		battle.commander1 = ownBattleShip.getOwner();
		battle.commander2 = enemyBattleShip.getOwner();
		battle.lastaction = Common.time();
		battle.lastturn = Common.time();
		battle.flags = FLAG_FIRSTROUND;
		battle.com1Msg = "";
		battle.com2Msg = "";
		db.save(battle);

		//
		// Schiffe in die Schlacht einfuegen
		//

		int tick = context.get(ContextCommon.class).getTick();

		// * Gegnerische Schiffe in die Schlacht einfuegen
		List<Integer> idlist = new ArrayList<Integer>();
		List<Integer> startlist = new ArrayList<Integer>();

		Ship enemyShip = enemyBattleShip.getShip();
		if( (enemyBattleShip.getDocked().length() > 0) && (enemyBattleShip.getDocked().charAt(0) == 'l') ) 
		{
			enemyShip.getBaseShip().start(enemyShip);
			startlist.add(enemyBattleShip.getId());
		}
		idlist.add(enemyBattleShip.getId());
		
		enemyBattleShip.setBattle(battle);
		db.persist(enemyBattleShip);
		
		enemyShip.setBattle(battle);

		insertShipsIntoDatabase(battle, battle.enemyShips, startlist, idlist);
		if( startlist.size() > 0 ) {
			battle.logme(startlist.size()+" J&auml;ger sind automatisch gestartet\n");
			battle.logenemy("<action side=\"1\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\n"+startlist.size()+" J&auml;ger sind automatisch gestartet\n]]></action>\n");

			startlist.clear();
		}

		startlist = new ArrayList<Integer>();
		
		// * Eigene Schiffe in die Schlacht einfuegen
		idlist.add(ownBattleShip.getId());
		
		ownBattleShip.setBattle(battle);
		db.persist(ownBattleShip);
		
		ownShip.setBattle(battle);
		if( (ownBattleShip.getDocked().length() > 0) && (ownBattleShip.getDocked().charAt(0) == 'l') ) 
		{
			//TODO: Maybe we could optimize this a little bit further with mass start?
			ownShip.getBaseShip().start(enemyShip);
			startlist.add(enemyBattleShip.getId());
		}
		ownShip.setDocked("");

		insertShipsIntoDatabase(battle, battle.ownShips, startlist, idlist);
		if( startOwn && startlist.size() > 0 ) {
			battle.logme(startlist.size()+" J&auml;ger sind automatisch gestartet\n");
			battle.logenemy("<action side=\"0\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\n"+startlist.size()+" J&auml;ger sind automatisch gestartet\n]]></action>\n");
		}
		startlist = null;

		idlist = null;

		//
		// Log erstellen
		//

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(Configuration.getSetting("LOXPATH")+"battles/battle_id"+battle.id+".log"));
			writer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
			writer.append("<battle>\n");
			writer.append("<fileinfo format=\""+LOGFORMAT+"\" />\n");
			writer.append("<coords x=\""+ownShip.getX()+"\" y=\""+ownShip.getY()+"\" system=\""+ownShip.getSystem()+"\" />\n");
	
			if( ownBattleShip.getOwner().getAlly() != null ) {
				writer.append("<side1 commander=\""+ownBattleShip.getOwner().getId()+"\" ally=\""+ownBattleShip.getOwner().getAlly().getId()+"\" />\n");
			}
			else {
				writer.append("<side1 commander=\""+ownBattleShip.getOwner().getId()+"\" />\n");
			}
	
			if( enemyBattleShip.getOwner().getAlly() != null ) {
				writer.append("<side2 commander=\""+enemyBattleShip.getOwner().getId()+"\" ally=\""+enemyBattleShip.getOwner().getAlly().getId()+"\" />\n");
			}
			else {
				writer.append("<side2 commander=\""+enemyBattleShip.getOwner().getId()+"\" />\n");
			}
	
			writer.append("<startdate tick=\""+tick+"\" time=\""+Common.time()+"\" />\n");
			writer.close();
			
			if( SystemUtils.IS_OS_UNIX ) {
				Runtime.getRuntime().exec("chmod 0666 "+Configuration.getSetting("LOXPATH")+"battles/battle_id"+battle.id+".log");
			}
		}
		catch( IOException e ) {
			log.error("Konnte KS-Log fuer Schlacht "+battle.id+" nicht erstellen", e);
		}
		
		
		//
		// Beziehungen aktualisieren
		//
		
		// Zuerst schauen wir mal ob wir es mit Allys zu tun haben und 
		// berechnen ggf die Userlisten neu 
		Set<Integer> calcedallys = new HashSet<Integer>();
		
		for( User auser : new ArrayList<User>(ownUsers) ) {
			if( (auser.getAlly() != null) && !calcedallys.contains(auser.getAlly()) ) {
				List<Integer> userIdList = new ArrayList<Integer>();
				for( User oUser : ownUsers ) {
					userIdList.add(oUser.getId());
				}
				List<?> allyusers = db.createQuery("from User where ally=? and (id not in ("+Common.implode(",",userIdList)+"))")
					.setEntity(0, auser.getAlly())
					.list();
				for( Iterator<?> iter=allyusers.iterator(); iter.hasNext(); ) {
					User allyuser = (User)iter.next();
					
					ownUsers.add(allyuser);	
				}
				calcedallys.add(auser.getAlly().getId());
			}
		}

		for( User auser : new ArrayList<User>(enemyUsers) ) {
			if( (auser.getAlly() != null) && !calcedallys.contains(auser.getAlly()) ) {
				List<Integer> userIdList = new ArrayList<Integer>();
				for( User oUser : enemyUsers ) {
					userIdList.add(oUser.getId());
				}
				List<?> allyusers = db.createQuery("from User where ally=? and (id not in ("+Common.implode(",",userIdList)+"))")
					.setEntity(0, auser.getAlly())
					.list();
				for( Iterator<?> iter=allyusers.iterator(); iter.hasNext(); ) {
					User allyuser = (User)iter.next();
					
					enemyUsers.add(allyuser);	
				}
				calcedallys.add(auser.getAlly().getId());
			}
		}
		calcedallys = null;
		
		for( User auser : ownUsers ) {
			for( User euser : enemyUsers ) {
				auser.setRelation(euser.getId(), User.Relation.ENEMY);
				euser.setRelation(auser.getId(), User.Relation.ENEMY);
			}
		}

		return battle;
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
		org.hibernate.Session db = context.getDB();
		
		Ship shipd = (Ship)db.get(Ship.class, shipid);
	
		if( (shipd == null) || (shipd.getId() < 0) ) {
			context.addError("Das angegebene Schiff existiert nicht!");
			return false;
		}
		if( shipd.getOwner().getId() != id ) {
			context.addError("Das angegebene Schiff geh&ouml;rt nicht ihnen!");
			return false;
		}
		if( shipd.getLock() != null && shipd.getLock().length() > 0 ) {
			context.addError("Das Schiff ist an ein Quest gebunden");
			return false;
		}
		if( !new Location(this.system,this.x,this.y).sameSector(0, shipd.getLocation(), 0) ) {
			context.addError("Das angegebene Schiff befindet sich nicht im selben Sektor wie die Schlacht!");
			return false;
		}
		if( shipd.getBattle() != null ) {
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
		
		User userobj = (User)context.getDB().get(User.class, id);
		if( userobj.isNoob() ) {
			context.addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher keine Schiffe in diese Schlacht schicken!<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden");
			return false;
		}
		
		ShipTypeData shiptype = shipd.getTypeData();
		if( shiptype.getShipClass() == ShipClasses.GESCHUETZ.ordinal() ) {
			context.addError("<span style=\"color:red\">Gesch&uuml;tze k&ouml;nnen einer Schlacht nicht beitreten!</span>");
			return false;
		}
		
		Map<Integer,Integer> shipcounts = new HashMap<Integer,Integer>();
		
		int side = this.ownSide;
		
		// Beziehungen aktualisieren
		Set<Integer> calcedallys = new HashSet<Integer>();
		
		List<User> ownUsers = new ArrayList<User>();
		ownUsers.add(userobj);
		Set<User> enemyUsers = new HashSet<User>();
		
		User curuser = userobj;
		if( curuser.getAlly() != null ) {
			List<User> users = curuser.getAlly().getMembers();
			for( User auser : users ) {
				if( auser.getId() == curuser.getId() ) {
					continue;
				}
				ownUsers.add(auser);	
			}
		}
		
		List<?> users = db.createQuery("select distinct bs.ship.owner " +
				"from BattleShip bs " +
				"where bs.battle= :battleId and bs.side= :sideId")
			.setInteger("battleId", this.id)
			.setInteger("sideId", this.enemySide)
			.list();

		for( Iterator<?> iter=users.iterator(); iter.hasNext(); ) {
			User euser = (User)iter.next();
			
			enemyUsers.add(euser);

			if( (euser.getAlly() != null) && !calcedallys.contains(euser.getAlly()) ) {
				List<User> allyusers = euser.getAlly().getMembers();
				for( User auser : allyusers ) {
					if( auser.getId() == euser.getId() ) {
						continue;
					}
					enemyUsers.add(auser);	
				}
				
				calcedallys.add(euser.getAlly().getId());
			}
		}
		
		for( int i=0; i < ownUsers.size(); i++ ) {
			User auser = ownUsers.get(i);
			
			for( User euser : enemyUsers ) {
				auser.setRelation(euser.getId(), User.Relation.ENEMY);
				euser.setRelation(auser.getId(), User.Relation.ENEMY);
			}
		}
		
		calcedallys = null;

		List<Integer> shiplist = new ArrayList<Integer>();
		
		List<?> sid = null;
		
		// Handelt es sich um eine Flotte?
		if( shipd.getFleet() != null ) {
			sid = db.createQuery("from Ship as s where s.id>0 and s.fleet=? and s.battle is null and s.x=? and s.y=? and s.system=? and s.lock is null")
					.setEntity(0, shipd.getFleet())
					.setInteger(1, shipd.getX())
					.setInteger(2, shipd.getY())
					.setInteger(3, shipd.getSystem())
					.list();
		}
		else {
			sid = db.createQuery("from Ship as s where s.id>0 and s.id=? and s.battle is null and s.x=? and s.y=? and s.system=? and s.lock is null")
					.setInteger(0, shipd.getId())
					.setInteger(1, shipd.getX())
					.setInteger(2, shipd.getY())
					.setInteger(3, shipd.getSystem())
					.list();
		}
		
		for( Iterator<?> iter=sid.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			
			shiptype = aship.getTypeData();
			if( shiptype.getShipClass() == ShipClasses.GESCHUETZ.ordinal() ) {
				continue;
			}
			
			shiplist.add(aship.getId());
				
			// ggf. gedockte Schiffe auch beruecksichtigen
			List<?> docked = db.createQuery("from Ship where id>0 and battle is null and docked in (?,?)")
				.setString(0, Integer.toString(aship.getId()))
				.setString(1, "l "+aship.getId())
				.list();
			
			for( Iterator<?> iter2=docked.iterator(); iter2.hasNext(); ) {
				Ship sid2 = (Ship)iter2.next();
				
				ShipTypeData stype = sid2.getTypeData();
				if( stype.getShipClass() == ShipClasses.GESCHUETZ.ordinal() ) {
					sid2.setDocked("");
					continue;
				}
				
				shiplist.add(sid2.getId());
					
				int sid2Action = 0;
				
				// Das neue Schiff in die Liste der eigenen Schiffe eintragen
				if( !shiptype.hasFlag(ShipTypes.SF_INSTANT_BATTLE_ENTER) && 
					!stype.hasFlag(ShipTypes.SF_INSTANT_BATTLE_ENTER) ) {
					sid2Action = BS_JOIN;
				}
				
				BattleShip sid2bs = new BattleShip(this, sid2);
				sid2bs.setAction(sid2Action);
				sid2bs.setSide(this.ownSide);
				
				getOwnShips().add(sid2bs);
	
				Common.safeIntInc(shipcounts, sid2.getType());
				
				db.persist(sid2bs);
				
				sid2.setBattle(this);
			}
		
			int sidAction = 0; 
			
			// Das neue Schiff in die Liste der eigenen Schiffe eintragen
			if( !shiptype.hasFlag(ShipTypes.SF_INSTANT_BATTLE_ENTER) ) {
				sidAction = BS_JOIN;
			}

			BattleShip aBattleShip = new BattleShip(this, aship);
			aBattleShip.setAction(sidAction);
			aBattleShip.setSide(side);
			
			getOwnShips().add(aBattleShip);
		
			Common.safeIntInc(shipcounts, aship.getType());
			
			db.persist(aBattleShip);
			
			aship.setBattle(this);
		}
		
		int tick = context.get(ContextCommon.class).getTick();
		
		if( shiplist.size() > 1 ) {
			int addedShips = shiplist.size();
			this.logenemy("<action side=\""+this.ownSide+"\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\nDie "+log_shiplink(shipd)+" ist zusammen mit "+addedShips+" weiteren Schiffen der Schlacht beigetreten\n]]></action>\n");
			this.logme( "Die "+log_shiplink(shipd)+" ist zusammen mit "+addedShips+" weiteren Schiffen der Schlacht beigetreten\n\n" );
		}
		else {
			this.logenemy("<action side=\""+this.ownSide+"\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\nDie "+log_shiplink(shipd)+" ist der Schlacht beigetreten\n]]></action>\n");
			this.logme("Die "+log_shiplink(shipd)+" ist der Schlacht beigetreten\n\n");
		
			shipd.setBattle(this);
		}
		
		return true;
	}
	
	/**
	 * Laedt eine Schlacht aus der Datenbank
	 * @param battleId die ID der Schlacht
	 * @param user Der aktive Spieler
	 * @param ownShip Das auszuwaehlende eigene Schiff (oder <code>null</code>)
	 * @param enemyShip Das auszuwaehlende gegnerische Schiff (oder <code>null</code>)
	 * @param forcejoin Die ID einer Seite (1 oder 2), welche als die eigene zu waehlen ist. Falls 0 wird automatisch eine gewaehlt
	 * 
	 * @return Die Schlacht oder <code>null</code>
	 */
	public static Battle loadBattle(int battleId, User user, Ship ownShip, Ship enemyShip, int forcejoin) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		Battle battle = (Battle)db.get(Battle.class, battleId);
		if( battle == null ) {
			db.createQuery("update Ship set battle=null where id>0 and battle=?")
				.setInteger(0, battleId)
				.executeUpdate();
		
			context.addError("Die Schlacht ist bereits zuende!");
			return null;
		}
		
		battle.load(user, ownShip, enemyShip, forcejoin);
		
		return battle;
	}
	
	/**
	 * Laedt weitere Schlachtdaten aus der Datenbank
	 * @param user Der aktive Spieler
	 * @param ownShip Das auszuwaehlende eigene Schiff (oder <code>null</code>)
	 * @param enemyShip Das auszuwaehlende gegnerische Schiff (oder <code>null</code>)
	 * @param forcejoin Die ID einer Seite (1 oder 2), welche als die eigene zu waehlen ist. Falls 0 wird automatisch eine gewaehlt
	 * 
	 * @return <code>true</code>, falls die Schlacht erfolgreich geladen wurde
	 */
	public boolean load(User user, Ship ownShip, Ship enemyShip, int forcejoin ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		/*
			TODO:
				- Update der Allys falls der Commander einer Seite einer Ally beitritt (und vorher in keiner war)
				- Update der Schiffe, falls ein Spieler nicht mehr einer der beiden Seiten angehoert (und ggf auch update der Kommandanten)
		*/

		//
		// Weitere Commander in Folge von Questschlachten feststellen
		//
		if( (this.quest != null) && !Common.inArray(user.getId(),this.getCommanders()) && ((this.commander1.getId() < 0) ^ (this.commander2.getId() < 0) ) ) {
			if( user.hasFlag(User.FLAG_QUEST_BATTLES) || user.getAccessLevel() > 20 ) {
				if( this.commander1.getId() < 0 )  {
					this.addCommanders.get(0).add(user.getId());
				}
				else {
					this.addCommanders.get(1).add(user.getId());
				}	
			}
		}
	
		//
		// Darf der Spieler (evt als Gast) zusehen?
		//
	
		int forceSide = -1;
	
		if( forcejoin == 0 ) {
			if( ( (user.getAlly() != null) && !Common.inArray(user.getAlly().getId(),this.getAllys()) && !this.isCommander(user) ) ||
				( (user.getAlly() == null) && !this.isCommander(user) ) ) {
	
				// Hat der Spieler ein Schiff in der Schlacht
				BattleShip aship = (BattleShip)db.createQuery("from BattleShip where id>0 and ship.owner=? and battle=?")
					.setEntity(0, user)
					.setEntity(1, this)
					.setMaxResults(1)
					.uniqueResult();
				
				if( aship != null ) {
					forceSide = aship.getSide();
				}
				else {
					//Mehr ueber den Spieler herausfinden
					if( user.getAccessLevel() > 20 ) {
						this.guest = true;
					}
					else if( user.hasFlag(User.FLAG_VIEW_BATTLES) ) {
						this.guest = true;
					}
					else {
						long shipcount = ((Number)db.createQuery("select count(*) from Ship " +
								"where owner= :user and x= :x and y= :y and system= :sys and " +
									"battle is null and shiptype.shipClass in (11,13)")
								.setEntity("user", user)
								.setInteger("x", this.x)
								.setInteger("y", this.y)
								.setInteger("sys", this.system)
								.iterate().next()).longValue();
						if( shipcount > 0 ) {
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
	
		if( (user.getAlly() != null && (user.getAlly().getId() == this.ally1)) || this.isCommander(user,0) || this.guest || forceSide == 0 ) {
			this.ownSide = 0;
			this.enemySide = 1;
		}

		else if( (user.getAlly() != null && (user.getAlly().getId() == this.ally2)) || this.isCommander(user,1) || forceSide == 1 ) {
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

		List<?> ships = db.createQuery("from BattleShip bs inner join fetch bs.ship as s " +
				    			"where s.id>0 and bs.battle=? " +
								"order by s.shiptype,s.id")
						.setEntity(0, this)
						.list();
	
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			BattleShip aship = (BattleShip)iter.next();
			
			if( aship.getSide() == this.ownSide ) {
				this.ownShips.add(aship);
				if( !this.guest || (aship.getDocked().length() == 0) || (aship.getDocked().charAt(0) != 'l') ) {
					if( ownShipTypeCount.containsKey(aship.getShip().getType()) ) {
						ownShipTypeCount.put(aship.getShip().getType(), ownShipTypeCount.get(aship.getShip().getType())+1);
					}
					else {
						ownShipTypeCount.put(aship.getShip().getType(), 1);
					}
				}
			}
			else if( aship.getSide() == this.enemySide ) {
				this.enemyShips.add(aship);
				if( (aship.getDocked().length() == 0) || (aship.getDocked().charAt(0) != 'l') ) {
					if( enemyShipTypeCount.containsKey(aship.getShip().getType()) ) {
						enemyShipTypeCount.put(aship.getShip().getType(), enemyShipTypeCount.get(aship.getShip().getType())+1);
					}
					else {
						enemyShipTypeCount.put(aship.getShip().getType(), 1);
					}
				}
			}
		}
	
		//
		// aktive Schiffe heraussuchen
		//
	
		this.activeSEnemy = 0;
		this.activeSOwn = 0;
	
		if( enemyShip != null ) {
			for( int i=0; i < enemyShips.size(); i++ ) {
				if( enemyShips.get(i).getId() == enemyShip.getId() ) {
					this.activeSEnemy = i;
					break;
				}
			}
		}
	
		if( ownShip != null ) {
			for( int i=0; i < ownShips.size(); i++ ) {
				if( ownShips.get(i).getId() == ownShip.getId() ) {
					this.activeSOwn = i;
					break;
				}
			}
		}
	
		// Falls die gewaehlten Schiffe gelandet (oder zerstoert) sind -> neue Schiffe suchen
		while( activeSEnemy < enemyShips.size() &&
			  ( (this.enemyShips.get(activeSEnemy).getAction() & BS_DESTROYED) != 0 ||
			 	((this.enemyShips.get(activeSEnemy).getDocked().length() > 0) &&
			 	(this.enemyShips.get(activeSEnemy).getDocked().charAt(0) == 'l')) ) ) {
			this.activeSEnemy++;
		}
	
		if( activeSEnemy >= enemyShips.size() ) activeSEnemy = 0;

		if( this.guest ) {
			while( activeSOwn < ownShips.size() &&
				  ( (this.ownShips.get(activeSOwn).getDocked().length() > 0) &&
				 	(this.ownShips.get(activeSOwn).getDocked().charAt(0) == 'l') ) ) {
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
	 * @return <code>true</code>, falls die Schlacht weiterhin existiert. <code>false</code>, falls sie beendet wurde.
	 * 
	 */
	public boolean endTurn( boolean calledByUser ) {
		Context context = ContextMap.getContext();
			
		List<List<BattleShip>> sides = new ArrayList<List<BattleShip>>();
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
			List<BattleShip> destroyList = new ArrayList<BattleShip>();
			List<BattleShip> fluchtList = new ArrayList<BattleShip>();
			List<BattleShip> fluchtReposList = new ArrayList<BattleShip>();
			
			List<BattleShip> shiplist = sides.get(i);
			for( int key=0; key < shiplist.size(); key++ ) {
				BattleShip aship = shiplist.get(key);

				if( (aship.getNewCount() > 0) && (aship.getNewCount() != aship.getCount()) ) {
					aship.setCount(aship.getNewCount());
					aship.setNewCount((byte)0);
				}
				
				if( (aship.getAction() & BS_HIT) != 0 ) {
					aship.getShip().setAblativeArmor(aship.getAblativeArmor());
					aship.getShip().setHull(aship.getHull());
					aship.getShip().setShields(aship.getShields());
					aship.getShip().setEngine(aship.getEngine());
					aship.getShip().setWeapons(aship.getWeapons());
					aship.getShip().setComm(aship.getComm());
					aship.getShip().setSensors(aship.getSensors());
					aship.setAction(aship.getAction() ^ BS_HIT);
				}
				else if( (aship.getAction() & BS_DESTROYED) != 0 ) {	
					if( Configuration.getIntSetting("DESTROYABLE_SHIPS") != 0 ) {
						//destroyList.add(aship);
						destroyShip(aship);
					}
					else {
						continue; //Das Schiff kann nicht zerstoert werden
					}
					
					aship.setAction(aship.getAction() ^ BS_DESTROYED);
					//continue;
				}
				
				
				
				if( (aship.getAction() & BS_FLUCHT) != 0 ) 
				{
					ShipTypeData ashipType = aship.getTypeData();
					if( ashipType.getCost() > 0 ) {
						//fluchtReposList.add(aship);
						removeShip(aship, false);
					}
					else {
						//fluchtList.add(aship);
						removeShip(aship, true);
					}
	
					//continue;
				}
				
				if( (aship.getAction() & BS_SHOT) != 0 ) {
					aship.setAction(aship.getAction() ^ BS_SHOT);
				}
				
				if( (aship.getAction() & BS_SECONDROW_BLOCKED) != 0 ) {
					aship.setAction(aship.getAction() ^ BS_SECONDROW_BLOCKED);
				}
				
				if( (aship.getAction() & BS_BLOCK_WEAPONS) != 0 ) {
					aship.setAction(aship.getAction() ^ BS_BLOCK_WEAPONS);
				}
				
				if( (i == 0) && this.hasFlag(FLAG_DROP_SECONDROW_0) && 
						(aship.getAction() & BS_SECONDROW) != 0 ) {
					aship.setAction(aship.getAction() ^ BS_SECONDROW);	
				}
				else if( (i == 1) && this.hasFlag(FLAG_DROP_SECONDROW_1) && 
						(aship.getAction() & BS_SECONDROW) != 0 ) {
					aship.setAction(aship.getAction() ^ BS_SECONDROW);	
				}
				
				if( (aship.getAction() & BS_JOIN) != 0 ) 
				{
					ShipTypeData ashipType = aship.getTypeData();
					if( ashipType.hasFlag(ShipTypes.SF_SECONDROW) && 
						this.isSecondRowStable(i, aship) ) {
						aship.setAction(aship.getAction() ^ BS_SECONDROW);
					}
					aship.setAction(aship.getAction() ^ BS_JOIN);
				}
	
				Map<String,String> heat = Weapons.parseWeaponList(aship.getWeaponHeat());
	
				for( String weaponName : heat.keySet() ) {
					heat.put(weaponName, "0");
				}
				
				if( (aship.getAction() & BS_FLUCHTNEXT) != 0 ) {
					aship.setAction((aship.getAction() ^ BS_FLUCHTNEXT) | BS_FLUCHT);
				}
				
				aship.getShip().setWeaponHeat(Weapons.packWeaponList(heat));
				aship.getShip().setBattleAction(false);
			}
			
			//Remove destroyed and fleeing ships
			//this.destroyShips(destroyList);
			//this.removeShips(fluchtList, false);
			//this.removeShips(fluchtReposList, true);
		}
		
		// Ist die Schlacht zuende (weil keine Schiffe mehr vorhanden sind?)
		int owncount = this.ownShips.size();
		int enemycount = this.enemyShips.size();
		
		if( (owncount == 0) && (enemycount == 0) ) {
			PM.send(this.getCommanders()[this.enemySide], this.getCommanders()[this.ownSide].getId(), "Schlacht unentschieden", "Die Schlacht bei "+this.getLocation()+" gegen "+this.getCommanders()[this.enemySide].getName()+" wurde mit einem Unentschieden beendet!");
			PM.send(this.getCommanders()[this.ownSide], this.getCommanders()[this.enemySide].getId(), "Schlacht unentschieden", "Die Schlacht bei "+this.getLocation()+" gegen "+this.getCommanders()[this.ownSide].getName()+" wurde mit einem Unentschieden beendet!");

			// Schlacht beenden - unendschieden
			this.endBattle(0,0, true);

			this.ownShips.clear();
			this.enemyShips.clear();

			if( calledByUser ) {
				context.getResponse().getContent().append("Du hast die Schlacht gegen "+Common._title( this.getCommanders()[this.enemySide].getName())+" mit einem Unendschieden beendet!");
			}
			return false;
		}
		else if( owncount == 0 ) {
			PM.send(this.getCommanders()[this.enemySide], this.getCommanders()[this.ownSide].getId(), "Schlacht verloren", "Du hast die Schlacht bei "+this.getLocation()+" gegen "+this.getCommanders()[this.enemySide].getName()+" verloren!");
			PM.send(this.getCommanders()[this.ownSide], this.getCommanders()[this.enemySide].getId(), "Schlacht gewonnen", "Du hast die Schlacht bei "+this.getLocation()+" gegen "+this.getCommanders()[this.ownSide].getName()+" gewonnen!");
					
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
				context.getResponse().getContent().append("Du hast die Schlacht gegen "+Common._title(this.getCommanders()[this.enemySide].getName())+" verloren!");
			}
			return false;
		}
		else if( enemycount == 0 ) {
			PM.send(this.getCommanders()[this.enemySide], this.getCommanders()[this.ownSide].getId(), "Schlacht gewonnen", "Du hast die Schlacht bei "+this.getLocation()+" gegen "+this.getCommanders()[this.enemySide].getName()+" gewonnen!");
			PM.send(this.getCommanders()[this.ownSide], this.getCommanders()[this.enemySide].getId(), "Schlacht verloren", "Du hast die Schlacht bei "+this.getLocation()+" gegen "+this.getCommanders()[this.ownSide].getName()+" verloren!");

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
				context.getResponse().getContent().append("Du hast die Schlacht gegen "+Common._title(this.getCommanders()[this.enemySide].getName())+" gewonnen!");
			} 
			return false;
		}
		
		this.ready1 = false;
		this.ready2 = false;
		this.blockcount = 2;

		this.lastturn = Common.time();

		int tick = context.get(ContextCommon.class).getTick();

		for( int i=0; i < 2; i++ ) {
			if( !calledByUser && this.getTakeCommands()[i] != 0 ) {
				this.logenemy("<action side=\""+i+"\" time=\""+Common.time()+"\" tick=\""+tick+"\"><![CDATA[\n");
	
				User com = (User)context.getDB().get(User.class, this.getTakeCommands()[i]);
	
				PM.send(com, this.getCommanders()[i].getId(), "Schlacht &uuml;bernommen", "Ich habe die Leitung der Schlacht bei "+this.getLocation()+" &uuml;bernommen.");
	
				this.logenemy("[Automatisch] "+Common._titleNoFormat(com.getName())+" kommandiert nun die gegnerischen Truppen\n\n");
	
				this.setCommander(i, com);
	
				this.logenemy("]]></action>\n");
	
				this.logenemy("<side"+(i+1)+" commander=\""+this.getCommanders()[i].getId()+"\" ally=\""+this.getAllys()[i]+"\" />\n");
	
				this.setTakeCommand(i, 0);
			}
		}
		
		if( this.hasFlag(FLAG_FIRSTROUND) ) {
			this.setFlag(FLAG_FIRSTROUND, false);
		}
		
		this.setFlag(FLAG_DROP_SECONDROW_0, false);
		this.setFlag(FLAG_DROP_SECONDROW_1, false);
		this.setFlag(FLAG_BLOCK_SECONDROW_0, false);
		this.setFlag(FLAG_BLOCK_SECONDROW_1, false);
		
		// Schiffstick ausfuehren
		context.getRequest().setParameter("battle", Integer.toString(this.id));
		
		TickController schiffstick = new SchiffsTick();
		schiffstick.execute();
		schiffstick.dispose();

		if( this.inakt > 6 ) {
			final User sourceUser = (User)context.getDB().get(User.class, -1);
			
			PM.send(sourceUser, this.commander1.getId(), "Schlacht beendet", "Die Schlacht bei "+this.getLocation()+" wurde wegen Inaktivit&auml;t automatisch beendet");
			PM.send(sourceUser, this.commander2.getId(), "Schlacht beendet", "Die Schlacht bei "+this.getLocation()+" wurde wegen Inaktivit&auml;t automatisch beendet");
			this.endBattle(0, 0, true);
			 
			if( !calledByUser ) {
				context.getResponse().getContent().append("-> Die Schlacht wurde wegen Inaktivit&auml;t beendet"); 	
			}
			
			return false;
		} 

		this.inakt++;
		
		return true;
	}
	
	private static final Object LOG_WRITE_LOCK = new Object();
	
	/**
	 * Schreibt das aktuelle Kampflog in die Logdatei
	 */
	public void writeLog() {
		if( (this.ownShips.size() > 0) && (this.enemyShips.size() > 0) ) {
			synchronized(LOG_WRITE_LOCK) {
				Common.writeLog("battles/battle_id"+this.id+".log", this.getEnemyLog(true));
			}
		}

		this.addComMessage(this.ownSide, this.getEnemyLog(false));
	}
	
	@Transient
	private boolean deleted = false;
	
	/**
	 * Beendet die Schlacht
	 * @param side1points Die Punkte, die die erste Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
	 * @param side2points Die Punkte, die die zweite Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
	 * @param executeScripts Sollen ggf vorhandene Scripte, welche auf das Ende der Schlacht "lauschen" ausgefuehrt werden (<code>true</code>)?
	 */
	public void endBattle( int side1points, int side2points, boolean executeScripts ) {
		this.writeLog();
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if( deleted ) {
			log.warn("Mehrfacher Aufruf von Battle.endBattle festgestellt", new Throwable());
			return;
		}
		
		deleted = true;
		
		if( executeScripts ) {
			String onendhandler = this.onend;
			if( (onendhandler != null) && (onendhandler.length() > 0) ) {			
				ScriptEngine scriptparser = context.get(ContextCommon.class).getScriptParser("DSQuestScript");
				if( context.getActiveUser() != null ) {
					BasicUser activeuser = context.getActiveUser();
					if( !activeuser.hasFlag(User.FLAG_SCRIPT_DEBUGGING) ) {
						scriptparser.getContext().setErrorWriter(new NullLogger());
					}
				}
				else {
					scriptparser.getContext().setErrorWriter(new NullLogger());
				}
				
				User questUser = (User)context.getActiveUser();
				if( questUser == null ) {
					questUser = (User)db.get(User.class, 0);
				}
				Quests.executeEvent( scriptparser, onendhandler, questUser, "", false );
			}
		}

		db.createQuery("delete from BattleShip where battle=?")
			.setEntity(0, this)
			.executeUpdate();
		db.createQuery("update Ship set battle=null,battleAction=0 where id>0 and battle=?")
			.setEntity(0, this)
			.executeUpdate();

		Common.writeLog("battles/battle_id"+this.id+".log", "</battle>");

		final String newlog = Configuration.getSetting("LOXPATH")+"battles/ended/"+Common.time()+"_id"+this.id+".log";
		new File(Configuration.getSetting("LOXPATH")+"battles/battle_id"+this.id+".log")
			.renameTo(new File(newlog));
		
		db.createQuery("update ShipLost set battle=0,battleLog=? where battle=?")
			.setString(0, newlog)
			.setEntity(1, this)
			.executeUpdate();

		int[] points = new int[] {side1points, side2points};
		
		for( int i=0; i < points.length; i++ ) {
			if( this.getAllys()[i] != 0 ) {
				Ally ally = (Ally)db.get(Ally.class, this.getAllys()[i]);
				if( points[i] > 0 ) {
					ally.setWonBattles((short)(ally.getWonBattles()+points[i]));
				} 
				else {
					ally.setLostBattles((short)(ally.getLostBattles()-points[i]));
				}
			}
			if( points[i] > 0 ) {
				 this.getCommanders()[i].setWonBattles((short)( this.getCommanders()[i].getWonBattles()+points[i]));
			} 
			else {
				 this.getCommanders()[i].setLostBattles((short)( this.getCommanders()[i].getLostBattles()-points[i]));
			}
		}

		this.enemyShips.clear();
		this.ownShips.clear();
		
		db.delete(this);
	}
	
	/**
	 * Zerstoert eine Liste von Schiffen und alle daran angedockten Schiffe.
	 * @param ships Schiffe, die zerstoert werden sollen
	 */
	//TODO: Optimize ship.getShip().destroy
	private void destroyShips(List<BattleShip> ships)
	{
		if(ships == null || ships.size() == 0)
		{
			return;
		}
		
		StringBuilder toDestroy = new StringBuilder(); //List of ids - will be used in mass delete
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		for(BattleShip ship: ships)
		{
			long dockcount = (Long)db.createQuery("select count(*) from Ship where docked in (?,?)")
			.setString(0, Integer.toString(ship.getId()))
			.setString(1, "l "+ship.getId())
			.iterate().next();
			
			toDestroy.append(ship.getId() + ",");
			ship.getShip().destroy();
			
			//
			// Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
			//

			boolean found = false;
			List<BattleShip> shiplist = null;
			
			if( ship.getSide() != this.ownSide ) 
			{
				shiplist = getEnemyShips();
			}
			else
			{
				shiplist = getOwnShips();
			}
			
		
			for( int i=0; i < shiplist.size(); i++ ) {
				BattleShip aship = shiplist.get(i);
				
				if( aship == ship ) {
					shiplist.remove(i);
					i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
					
					found = true;
				}
				// Evt ist das Schiff an das gerade zerstoerte gedockt
				// In diesem Fall muss es ebenfalls entfernt werden
				else if( (dockcount > 0) && (aship.getDocked().length() > 0) &&
						(aship.getDocked().equals("l "+ship.getId()) || aship.getDocked().equals(Integer.toString(ship.getId()))) ) {
					shiplist.remove(i);
					i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
					
					dockcount--;
					
					toDestroy.append(aship.getId() + ",");
					aship.getShip().destroy();
				}
				
				if( found && (dockcount == 0) ) {
					break;
				}
			}
			
			if( (ship.getSide() == this.enemySide) && (this.activeSEnemy >= shiplist.size()) ) {
				this.activeSEnemy = 0;
			}
			else if( (ship.getSide() == this.ownSide) && (this.activeSOwn >= shiplist.size()) ) {
				this.activeSOwn = 0;
			}
		}
		
		//Remove all destroyed ships
		String destroy = toDestroy.substring(0, toDestroy.length() - 1);
		db.createQuery("delete from BattleShip where id in ("+ destroy +")").executeUpdate();
	}
	
	
	private void removeShips(List<BattleShip> ships, boolean relocate)
	{
		if(ships == null || ships.size() == 0)
		{
			return;
		}
		
		StringBuilder toDestroy = new StringBuilder(); //List of ids - will be used in mass delete
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		for(BattleShip ship: ships)
		{
			Location loc = ship.getShip().getLocation();
			
			if( relocate && (ship.getDocked().length() == 0) ) {
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
			
			long dockcount = (Long)db.createQuery("select count(*) from Ship where docked IN (?,?)")
			.setString(0, Integer.toString(ship.getId()))
			.setString(1, "l "+ship.getId())
			.iterate().next();
		
			ship.getShip().setBattle(null);
			ship.getShip().setX(loc.getX());
			ship.getShip().setY(loc.getY());
			
			toDestroy.append(ship.getId() + ",");
			ship.getShip().recalculateShipStatus();
			
			//
			// Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
			//

			boolean found = false;
			List<BattleShip> shiplist = this.ownShips;
			
			if( ship.getSide() != this.ownSide ) {
				shiplist = this.enemyShips;
			}
			
			for( int i=0; i < shiplist.size(); i++ ) {
				BattleShip aship = shiplist.get(i);
				
				if( aship == ship ) {
					shiplist.remove(i);
					i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
					
					found = true;
				}
				// Evt ist das Schiff an das gerade fliehende gedockt
				// In diesem Fall muss es ebenfalls entfernt werden
				else if( (dockcount > 0) && (aship.getDocked().length() > 0) &&
						(aship.getDocked().equals("l "+ship.getId()) || aship.getDocked().equals(Integer.toString(ship.getId()))) ) {
					shiplist.remove(i);
					i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
					
					dockcount--;
					
					aship.getShip().setBattle(null);
					aship.getShip().setBattleAction(false);
					aship.getShip().setX(loc.getX());
					aship.getShip().setY(loc.getY());
					
					toDestroy.append(aship.getId() + ",");
					aship.getShip().recalculateShipStatus();
				}
				
				if( found && (dockcount == 0) ) {
					break;
				}
			}
			
			if( (ship.getSide() == this.enemySide) && (this.activeSEnemy >= shiplist.size()) ) {
				this.activeSEnemy = 0;
			}
			else if( (ship.getSide() == this.ownSide) && (this.activeSOwn >= shiplist.size()) ) {
				this.activeSOwn = 0;
			}
		}
		
		//Remove all ships
		String destroy = toDestroy.substring(0, toDestroy.length() - 1);
		db.createQuery("delete from BattleShip where id in ("+ destroy +")").executeUpdate();
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
	 * Prueft, ob die Schlacht fuer einen Benutzer sichtbar ist
	 * @param user Der Benutzer
	 * @return <code>true</code>, falls sie sichtbar ist
	 */
	public boolean isVisibleToUser(User user) {
		if( isCommander(user) ) {
			return true;
		}
		
		if( (this.visibility != null) && (this.visibility.length() > 0) ) {
			Integer[] visibility = Common.explodeToInteger(",", this.visibility);
			if( !Common.inArray(user.getId(),visibility) ) {
				return false;	
			}
		}
		return true;
	}
	
	/**
	 * Gibt die ID des mit der Schlacht verknuepften laufenden Quests zurueck.
	 * Wenn kein Quest mit der Schlacht verknuepft ist, so wird <code>null</code>
	 * zurueckgegeben.
	 * @return Die ID des laufenden Quests oder <code>null</code>
	 */
	public Integer getQuest() {
		return this.quest;
	}
	
	/**
	 * Setzt die ID des mit der Schlacht verknuepften laufenden Quests.
	 * <code>null</code> bedeutet, dass kein Quest mit der Schlacht verknuepft ist.
	 * @param quest Die ID des mit der Schlacht verknuepften laufenden Quests oder <code>null</code>
	 */
	public void setQuest( Integer quest ) {
		this.quest = quest;
	}
	
	/**
	 * Gibt den Blockcount der Schlacht zurueck. Der Blockcount
	 * bestimmt, wieviele Ticks eine Schlacht ueberspringen darf bevor
	 * ein Rundenwechsel erzwungen wird.
	 * @return Der Blockcount
	 */
	public int getBlockCount() {
		return this.blockcount;
	}
	
	/**
	 * Gibt den Zeitpunkt des letzten Rundenwechsels zurueck
	 * @return Der Zeitpunkt
	 */
	public long getLastTurn() {
		return this.lastturn;
	}
	
	/**
	 * Prueft, ob ein Spieler Kommandant der Schlacht ist
	 * @param user Der Spieler
	 * @return <code>true</code>, falls er Kommandant ist
	 */
	public boolean isCommander( User user ) {
		return isCommander(user, -1);
	}
	
	/**
	 * Prueft, ob ein Spieler auf einer Seite Kommandant ist
	 * @param user Der Spieler
	 * @param side Die Seite oder <code>-1</code>, falls die Seite egal ist
	 * @return <code>true</code>, falls er Kommandant ist
	 */
	public boolean isCommander( User user, int side ) {
		int myside = -1;
		
		if( (this.commander1.getId() == user.getId()) || this.addCommanders.get(0).contains(user.getId()) ) {
			myside = 0;
		}
		else if( (this.commander2.getId() == user.getId()) || this.addCommanders.get(1).contains(user.getId()) ) {
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
	public List<BattleShip> getEnemyShips() {
		return enemyShips;
	}
	
	/**
	 * Gibt das gegnerische Schiff mit dem angegebenen Index (nicht ID!) zurueck
	 * @param index Der Index
	 * @return Das Schiff
	 */
	public BattleShip getEnemyShip( int index ) {
		return enemyShips.get(index);
	}

	/**
	 * Gibt die Liste der eigenen Schiffe zurueck
	 * @return Die Liste der eigenen Schiffe
	 */
	public List<BattleShip> getOwnShips() {
		return ownShips;
	}
	
	/**
	 * Gibt das eigene Schiff mit dem angegebenen Index (nicht ID!) zurueck
	 * @param index Der Index
	 * @return Das Schiff
	 */
	public BattleShip getOwnShip( int index ) {
		return ownShips.get(index);
	}
	
	/**
	 * Gibt die mit einer Seite assoziierte Allianz zurueck
	 * @param side Die Seite
	 * @return Die ID der Allianz oder 0
	 */
	public int getAlly(int side) {
		return side == 0 ? ally1 : ally2;
	}
	
	private int[] getAllys() {
		return new int[] {this.ally1,this.ally2};
	}
	
	/**
	 * Gibt den Kommandanten einer Seite zurueck
	 * @param side Die Seite
	 * @return Der Spieler
	 */
	public User getCommander(int side) {
		return side == 0 ? this.commander1 : this.commander2;
	}
	
	/**
	 * Setzt den Kommandaten einer Seite
	 * @param side Die Seite
	 * @param user Der neue Kommandant
	 */
	public void setCommander(int side, User user) {
		if( side == 0 ) {
			this.commander1 = user;
		}
		else {
			this.commander2 = user;
		}
	}
	
	private User[] getCommanders() {
		return new User[] {this.commander1, this.commander2};
	}
	
	/**
	 * Prueft, ob eine Seite mit ihren Aktionen in der Runde fertig ist
	 * @param side Die Seite
	 * @return <code>true</code>, falls sie mit ihren Aktionen in der Runde fertig ist
	 */
	public boolean isReady(int side) {
		return side == 0 ? this.ready1 : this.ready2;
	}
	
	/**
	 * Setzt den "fertig"-Status einer Seite fuer die aktuelle Runde
	 * @param side Die Seite
	 * @param ready Der Status
	 */
	public void setReady(int side, boolean ready) {
		if( side == 0 ) {
			this.ready1 = ready;
		}
		else {
			this.ready2 = ready;
		}
	}
	
	/**
	 * Gibt die ID des Spielers zurueck, der auf einer Seite das Kommando uebernehmen will. Sollte
	 * kein solcher Spieler existieren, so wird 0 zurueckgegeben.
	 * @param side Die Seite
	 * @return Die ID des Spielers oder 0
	 */
	public int getTakeCommand(int side) {
		return side == 0 ? this.takeCommand1 : this.takeCommand2;
	}
	
	/**
	 * Setzt die ID eines Spielers, welcher auf einer Seite das Kommando uebernehmen will
	 * @param side Die Seite
	 * @param id Die ID des Spielers
	 */
	public void setTakeCommand(int side, int id) {
		if( side == 0 ) {
			this.takeCommand1 = id;
		}
		else {
			this.takeCommand2 = id;
		}
	}
	
	private int[] getTakeCommands() {
		return new int[] {this.takeCommand1, this.takeCommand2};
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

	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}

	/**
	 * Gibt die Versionsnummer zurueck
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
	
	/**
	 * Zerstoert ein Schiff und alle an ihm gedockten Schiff
	 * @param ship Das zu zerstoerende Schiff
	 */
	private void destroyShip( BattleShip ship ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		long dockcount = (Long)db.createQuery("select count(*) from Ship where docked in (?,?)")
			.setString(0, Integer.toString(ship.getId()))
			.setString(1, "l "+ship.getId())
			.iterate().next();
		
		db.delete(ship);
		ship.getShip().destroy();

		//
		// Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
		//

		boolean found = false;
		List<BattleShip> shiplist = this.ownShips;
		
		if( ship.getSide() != this.ownSide ) {
			shiplist = this.enemyShips;
		}
	
		for( int i=0; i < shiplist.size(); i++ ) {
			BattleShip aship = shiplist.get(i);
			
			if( aship == ship ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				found = true;
			}
			// Evt ist das Schiff an das gerade zerstoerte gedockt
			// In diesem Fall muss es ebenfalls entfernt werden
			else if( (dockcount > 0) && (aship.getDocked().length() > 0) &&
					(aship.getDocked().equals("l "+ship.getId()) || aship.getDocked().equals(Integer.toString(ship.getId()))) ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				dockcount--;
				
				db.delete(aship);
				aship.getShip().destroy();
			}
			
			if( found && (dockcount == 0) ) {
				break;
			}
		}
		
		if( (ship.getSide() == this.enemySide) && (this.activeSEnemy >= shiplist.size()) ) {
			this.activeSEnemy = 0;
		}
		else if( (ship.getSide() == this.ownSide) && (this.activeSOwn >= shiplist.size()) ) {
			this.activeSOwn = 0;
		}
	}
	
	/**
	 * Entfernt ein Schiff aus einer Schlacht und platziert es falls gewuenscht in einem zufaelligen Sektor
	 * um die Schlacht herum. Evt gedockte Schiffe werden mitentfernt und im selben Sektor platziert
	 * @param ship Das fliehende Schiff
	 * @param relocate Soll ein zufaelliger Sektor um die Schlacht herum gewaehlt werden? (<code>true</code>)
	 */
	private void removeShip( BattleShip ship, boolean relocate ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		Location loc = ship.getShip().getLocation();
		
		if( relocate && (ship.getDocked().length() == 0) ) {
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

		if( ship.getShip().getBattle() == null ) {
			// Es kann vorkommen, dass das Schiff bereits entfernt wurde (wegen einer dock-Beziehung)
			return;
		}
		
		// Falls das Schiff an einem anderen Schiff gedockt ist, dann das 
		// Elternschiff fliehen lassen. Dieses kuemmert sich dann um die
		// gedockten Schiffe
		if( ship.getDocked().length() > 0 ) {
			int masterid = 0;
			if( ship.getDocked().charAt(0) == 'l' ) {
				masterid = Integer.parseInt(ship.getDocked().substring(2));
			}
			else {
				masterid = Integer.parseInt(ship.getDocked());
			}
			
			List<BattleShip> shiplist = this.ownShips;
			if( ship.getSide() != this.ownSide ) {
				shiplist = this.enemyShips;
			}
			
			for( int i=0; i < shiplist.size(); i++ ) {
				BattleShip aship = shiplist.get(i);
				
				if( aship.getId() == masterid ) {
					removeShip(aship, relocate);
					return;
				}
			}
		}
		
		long dockcount = (Long)db.createQuery("select count(*) from Ship where docked IN (?,?)")
			.setString(0, Integer.toString(ship.getId()))
			.setString(1, "l "+ship.getId())
			.iterate().next();
		
		ship.getShip().setBattle(null);
		ship.getShip().setX(loc.getX());
		ship.getShip().setY(loc.getY());
		
		db.delete(ship);
		ship.getShip().recalculateShipStatus();

		//
		// Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
		//

		boolean found = false;
		List<BattleShip> shiplist = this.ownShips;
		
		if( ship.getSide() != this.ownSide ) {
			shiplist = this.enemyShips;
		}
		
		for( int i=0; i < shiplist.size(); i++ ) {
			BattleShip aship = shiplist.get(i);
			
			if( aship == ship ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				found = true;
			}
			// Evt ist das Schiff an das gerade fliehende gedockt
			// In diesem Fall muss es ebenfalls entfernt werden
			else if( (dockcount > 0) && (aship.getDocked().length() > 0) &&
					(aship.getDocked().equals("l "+ship.getId()) || aship.getDocked().equals(Integer.toString(ship.getId()))) ) {
				shiplist.remove(i);
				i--; // Arraypositionen haben sich nach dem Entfernen veraendert. Daher Index aktuallisieren
				
				dockcount--;
				
				aship.getShip().setBattle(null);
				aship.getShip().setBattleAction(false);
				aship.getShip().setX(loc.getX());
				aship.getShip().setY(loc.getY());
				
				db.delete(aship);
				aship.getShip().recalculateShipStatus();
			}
			
			if( found && (dockcount == 0) ) {
				break;
			}
		}
		
		if( (ship.getSide() == this.enemySide) && (this.activeSEnemy >= shiplist.size()) ) {
			this.activeSEnemy = 0;
		}
		else if( (ship.getSide() == this.ownSide) && (this.activeSOwn >= shiplist.size()) ) {
			this.activeSOwn = 0;
		}
	}
}
