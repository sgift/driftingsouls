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

import net.driftingsouls.ds2.server.*;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.persistence.Table;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Repraesentiert eine Schlacht in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="battles")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@BatchSize(size=50)
@org.hibernate.annotations.Table(appliesTo = "battles", indexes = {@Index(name="battle_coords", columnNames = {"x", "y", "system"})})
@OptimisticLocking(type = OptimisticLockType.DIRTY)
public class Battle implements Locatable
{
	private static final Log log = LogFactory.getLog(Battle.class);

	@Id @GeneratedValue
	private int id;
	private int x;
	private int y;
	private int system;
	private int ally1;
	private int ally2;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="commander1", nullable=false)
	@ForeignKey(name="battles_fk_users1")
	private User commander1;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="commander2", nullable=false)
	@ForeignKey(name="battles_fk_users2")
	private User commander2;
	private boolean ready1;
	private boolean ready2;
	private boolean com1BETAK = true;
	private boolean com2BETAK = true;
	private int takeCommand1;
	private int takeCommand2;
	private int blockcount = 2;
	private long lastaction;
	private long lastturn;
	private int flags;
	@OneToOne(cascade = {})
	@JoinColumn
	@ForeignKey(name="battles_fk_schlachtlog")
	private SchlachtLog schlachtLog;

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
	private List<BattleShip> ownShips = new ArrayList<>();
	@Transient
	private List<BattleShip> enemyShips = new ArrayList<>();

	@Transient
	private List<List<Integer>> addCommanders = new ArrayList<>();

	@Transient
	private boolean guest = false;

	@Transient
	private Map<Integer,Integer> ownShipTypeCount = new HashMap<>();
	@Transient
	private Map<Integer,Integer> enemyShipTypeCount = new HashMap<>();

	@Transient
	private int activeSOwn = 0;
	@Transient
	private int activeSEnemy = 0;

	@Transient
	private StringBuilder logoutputbuffer = new StringBuilder();

	/**
	 * Generiert eine Stringrepraesentation eines Schiffes, welche
	 * in KS-Logs geschrieben werden kann.
	 * @param ship Das Schiff
	 * @return Die Stringrepraesentation
	 */
	public static String log_shiplink( Ship ship ) {
		ShipTypeData shiptype = ship.getTypeData();

		return "[tooltip="+shiptype.getNickname()+"]"+ship.getName()+"[/tooltip] ("+ship.getId()+")";
	}

	/**
	 * Konstruktor.
	 *
	 */
	public Battle() {
		this.addCommanders.add(0, new ArrayList<>());
		this.addCommanders.add(1, new ArrayList<>());
	}

	/**
	 * Konstruktor.
	 * @param location Die Position an der die Schlacht gefuehrt wird
	 */
	public Battle(Location location)
	{
		this();
		this.ownSide = 0;
		this.enemySide = 1;
		this.x = location.getX();
		this.y = location.getY();
		this.system = location.getSystem();
		this.lastaction = Common.time();
		this.lastturn = Common.time();
	}

	/**
	 * Gibt die ID der Schlacht zurueck.
	 * @return die ID
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Gibt den aktuellen Blockierungs-Zaehler fuer den Rundenwechsel zurueck.
	 * @return Der Zaehler
	 */
	public int getBlockCount()
	{
		return this.blockcount;
	}

	/**
	 * Verringert den Blockierungs-Zaehler fuer den Rundenwechsel um eins.
	 */
	public void decrementBlockCount()
	{
		this.blockcount--;
	}

	/**
	 * Gibt den Zeitpunkt des letzten Rundenwechsels in Unix-Zeit (in Sekunden) zurueck.
	 * @return Der Zeitpunkt
	 */
	public long getLetzteRunde()
	{
		return this.lastturn;
	}

	/**
	 * Gibt den Zeitpunkt der letzten Aktion in Unix-Zeit (in Sekunden) zurueck.
	 * @return Der Zeitpunkt
	 */
	public long getLetzteAktion()
	{
		return this.lastaction;
	}

	/**
	 * Gibt die X-Position der Schlacht zurueck.
	 * @return die X-Position
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * Setzt die X-Position der Schlacht.
	 * @param x die X-Position
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * Gibt die Y-Position der Schlacht zurueck.
	 * @return Die Y-Position
	 */
	public int getY() {
		return this.y;
	}

	/**
	 * Setzt die Y-Koordinate der Schlacht.
	 * @param y Die Y-Position
	 */
	public void setY(int y) {
		this.y = y;
	}

	/**
	 * Gibt das System zurueck, in dem die Schlacht stattfindet.
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
		List<BattleShip> shiplist;
		if( side == this.ownSide ) {
			shiplist = getOwnShips();
		}
		else
        {
			shiplist = getEnemyShips();
		}

		double owncaps = 0;
		double secondrowcaps = 0;
        for (BattleShip aship : shiplist) {
            if (aship.hasFlag(BattleShipFlag.JOIN)) {
                continue;
            }
            ShipTypeData type = aship.getTypeData();

            double size = type.getSize();
            if (aship.hasFlag(BattleShipFlag.SECONDROW)) {
                if (!aship.getShip().isDocked() && !aship.getShip().isLanded()) {
                    secondrowcaps += size;
                }
            }
            else
            {
                if (size > ShipType.SMALL_SHIP_MAXSIZE) {
                    double countedSize = size;
                    if (type.getCrew() > 0) {
                        countedSize *= (aship.getCrew() / ((double) type.getCrew()));
                    }
                    owncaps += countedSize;
                }
            }
        }

		if( added != null ) {
            for (BattleShip aship : added) {
                ShipTypeData type = aship.getTypeData();

                if (!type.hasFlag(ShipTypeFlag.SECONDROW)) {
                    continue;
                }

                if (!aship.getShip().isDocked() && !aship.getShip().isLanded()) {
                    secondrowcaps += type.getSize();
                }
            }
		}

        return Double.valueOf(secondrowcaps).intValue() == 0 || Double.valueOf(owncaps).intValue() >= Double.valueOf(secondrowcaps).intValue() * 2;

    }

	/**
	 * Prueft, ob die Schlacht ueber das angegebene Flag verfuegt.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Schlacht das Flag besitzt
	 */
	public boolean hasFlag( BattleFlag flag ) {
		return (this.flags & flag.getBit()) != 0;
	}

	/**
	 * Fuegt der Schlacht das angegebene Flag hinzu.
	 * @param flag Das Flag
	 */
	public void setFlag( BattleFlag flag ) {
		setFlag(flag, true);
	}

	/**
	 * Fuegt der Schlacht ein Flag hinzu oder entfernt eines.
	 * @param flag Das Flag
	 * @param status <code>true</code>, falls das Flag hinzugefuegt werden soll. Andernfalls <code>false</code>
	 */
	public void setFlag( BattleFlag flag, boolean status ) {
		if( status ) {
			this.flags |= flag.getBit();
		}
		else if( (this.flags & flag.getBit()) != 0 ) {
			this.flags ^= flag.getBit();
		}
	}

	/**
	 * Gibt den Betak-Status einer Seite zurueck.
	 * @param side Die Seite
	 * @return <code>true</code>, falls die Seite noch nicht gegen die Betak verstossen hat
	 */
	public boolean getBetakStatus( int side ) {
		return side == 0 ? this.com1BETAK : this.com2BETAK;
	}

	/**
	 * Setzt den Betak-Status einer Seite.
	 * @param side Die Seite
	 * @param status Der neue Betak-Status
	 */
	public void setBetakStatus( int side, boolean status ) {
		if( side == 0 ) {
			this.com1BETAK = status;
		}
		else
        {
			this.com2BETAK = status;
		}
	}

	/**
	 * Prueft, ob der Betrachter ein Gast ist.
	 * @return <code>true</code>, falls der Betrachter ein Gast ist
	 */
	public boolean isGuest() {
		return this.guest;
	}

	/**
	 * Gibt das aktuell ausgewaehlte eigene Schiff zurueck.
	 * @return Das aktuell ausgewaehlte eigene Schiff
	 */
	public BattleShip getOwnShip() {
		return this.ownShips.get(this.activeSOwn);
	}

	/**
	 * Gibt das aktuell ausgewaehlte gegnerische Schiff zurueck.
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
        return this.activeSEnemy < this.enemyShips.size() && this.activeSEnemy >= 0;
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

				if( !aship.getShip().isLanded() &&
						!aship.hasFlag(BattleShipFlag.DESTROYED) && !aship.hasFlag(BattleShipFlag.SECONDROW) ) {
					return i;
				}
			}

			return -1;
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
					!aship.getShip().isLanded() &&
					!aship.hasFlag(BattleShipFlag.DESTROYED) && !aship.hasFlag(BattleShipFlag.SECONDROW) ) {
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
					!aship.getShip().isLanded() &&
					!aship.hasFlag(BattleShipFlag.DESTROYED) && !aship.hasFlag(BattleShipFlag.SECONDROW) ) {
				return i;
			}
		}
	/*
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
		*/
		return -1;
	}

	/**
	 * Gibt den Index des aktuell ausgewaehlten generischen Schiffes zurueck.
	 * @return Der Index des aktuell ausgewaehlten gegnerischen Schiffes
	 */
	public int getEnemyShipIndex() {
		return this.activeSEnemy;
	}

	/**
	 * Setzt den Index des aktuell ausgewaehlten gegnerischen Schiffes.
	 * @param index Der neue Index
	 */
	public void setEnemyShipIndex(int index) {
		if( index >= this.enemyShips.size() ) {
			throw new IndexOutOfBoundsException("Schiffsindex fuer gegnerische Schiffe '"+index+"' > als das das vorhandene Maximum ("+this.enemyShips.size()+")");
		}
		this.activeSEnemy = index;
	}

	/**
	 * Laesst eines oder mehrere Schiffe (in einer Flotte) der Schlacht beitreten.
	 * @param id Die ID des Besitzers der Schiffe
	 * @param shipid Die ID eines der Schiffe, welche beitreten sollen
	 *
	 * @return <code>true</code>, falls der Beitritt erfolgreich war
	 */
	public boolean addShip( int id, int shipid )
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		db.flush();

		Ship shipd = (Ship)db.get(Ship.class, shipid);

		if( (shipd == null) || (shipd.getId() < 0) )
		{
			context.addError("Das angegebene Schiff existiert nicht!");
			return false;
		}
		if( shipd.getOwner().getId() != id )
		{
			context.addError("Das angegebene Schiff geh&ouml;rt nicht ihnen!");
			return false;
		}
		if( !new Location(this.system,this.x,this.y).sameSector(0, shipd.getLocation(), 0) )
		{
			context.addError("Das angegebene Schiff befindet sich nicht im selben Sektor wie die Schlacht!");
			return false;
		}
		if( shipd.getBattle() != null )
		{
			context.addError("Das angegebene Schiff befindet sich bereits in einer Schlacht!");
			return false;
		}

		User userobj = (User)context.getDB().get(User.class, id);
		if( userobj.isNoob() )
		{
			context.addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher keine Schiffe in diese Schlacht schicken!<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden");
			return false;
		}

		ShipTypeData shiptype = shipd.getTypeData();
		if( (shiptype.getShipClass() == ShipClasses.GESCHUETZ ) )
		{
			context.addError("<span style=\"color:red\">Gesch&uuml;tze k&ouml;nnen einer Schlacht nicht beitreten!<br />Diese m&uuml;ssen von Frachtern mitgenommen werden!</span>");
			return false;
		}

		Map<Integer,Integer> shipcounts = new HashMap<>();

		int side = this.ownSide;

		// Beziehungen aktualisieren
		Set<Integer> calcedallys = new HashSet<>();

		List<User> ownUsers = new ArrayList<>();
		ownUsers.add(userobj);
		Set<User> enemyUsers = new HashSet<>();

        if( userobj.getAlly() != null ) {
			List<User> users = userobj.getAlly().getMembers();
			for( User auser : users ) {
				if( auser.getId() == userobj.getId() ) {
					continue;
				}
				ownUsers.add(auser);
			}
		}

		List<User> users = Common.cast(db.createQuery("select distinct bs.ship.owner " +
                "from BattleShip bs " +
                "where bs.battle= :battleId and bs.side= :sideId")
                .setInteger("battleId", this.id)
                .setInteger("sideId", this.enemySide)
                .list());

        for(User euser: users)
        {
            enemyUsers.add(euser);

            if ((euser.getAlly() != null) && !calcedallys.contains(euser.getAlly().getId())) {
                List<User> allyusers = euser.getAlly().getMembers();
                for (User auser : allyusers) {
                    if (auser.getId() == euser.getId()) {
                        continue;
                    }
                    enemyUsers.add(auser);
                }

                calcedallys.add(euser.getAlly().getId());
            }
        }

        for (User auser : ownUsers)
        {
            for (User euser : enemyUsers)
            {
                auser.setRelation(euser.getId(), User.Relation.ENEMY);
                euser.setRelation(auser.getId(), User.Relation.ENEMY);
            }
        }

		List<Integer> shiplist = new ArrayList<>();

		List<Ship> sid;
		// Handelt es sich um eine Flotte?
		if( shipd.getFleet() != null ) {
			sid = Common.cast(db.createQuery("from Ship as s where s.id>0 and s.fleet=:fleet and s.battle is null and s.x=:x and s.y=:y and s.system=:sys")
                    .setEntity("fleet", shipd.getFleet())
                    .setInteger("x", shipd.getX())
                    .setInteger("y", shipd.getY())
                    .setInteger("sys", shipd.getSystem())
                    .list());
		}
		else
        {
			sid = Common.cast(db.createQuery("from Ship as s where s.id>0 and s.id=:id and s.battle is null and s.x=:x and s.y=:y and s.system=:sys")
                    .setInteger("id", shipd.getId())
                    .setInteger("x", shipd.getX())
                    .setInteger("y", shipd.getY())
                    .setInteger("sys", shipd.getSystem())
                    .list());
		}

        for(Ship aship : sid)
        {
            if (db.get(BattleShip.class, aship.getId()) != null) {
                continue;
            }

            shiptype = aship.getTypeData();
            if (shiptype.getShipClass() == ShipClasses.GESCHUETZ) {
                continue;
            }

            shiplist.add(aship.getId());

            // ggf. gedockte Schiffe auch beruecksichtigen
            List<Ship> docked = Common.cast(db.createQuery("from Ship where id>0 and battle is null and docked in (:docked,:landed)")
                    .setString("docked", Integer.toString(aship.getId()))
                    .setString("landed", "l " + aship.getId())
                    .list());

            for(Ship dockedShip : docked)
            {
                if (db.get(BattleShip.class, dockedShip.getId()) != null) {
                    continue;
                }

				BattleShip sid2bs = new BattleShip(this, dockedShip);

                ShipTypeData stype = dockedShip.getTypeData();
                if (stype.getShipClass() == ShipClasses.GESCHUETZ) {
                    sid2bs.addFlag(BattleShipFlag.BLOCK_WEAPONS);
                }

                shiplist.add(dockedShip.getId());


                // Das neue Schiff in die Liste der eigenen Schiffe eintragen
                if (!shiptype.hasFlag(ShipTypeFlag.INSTANT_BATTLE_ENTER) &&
                        !stype.hasFlag(ShipTypeFlag.INSTANT_BATTLE_ENTER)) {
                    sid2bs.addFlag(BattleShipFlag.JOIN);
                }

                sid2bs.setSide(this.ownSide);

                getOwnShips().add(sid2bs);

                Common.safeIntInc(shipcounts, dockedShip.getType());

                db.persist(sid2bs);

                dockedShip.setBattle(this);
            }

			BattleShip aBattleShip = new BattleShip(this, aship);

			// Das neue Schiff in die Liste der eigenen Schiffe eintragen
            if (!shiptype.hasFlag(ShipTypeFlag.INSTANT_BATTLE_ENTER)) {
                aBattleShip.addFlag(BattleShipFlag.JOIN);
            }

            aBattleShip.setSide(side);

            getOwnShips().add(aBattleShip);

            Common.safeIntInc(shipcounts, aship.getType());

            db.persist(aBattleShip);

            aship.setBattle(this);
        }

		if( shiplist.size() > 1 )
		{
			int addedShips = shiplist.size()-1;
			this.log(new SchlachtLogAktion(this.ownSide, "Die " + log_shiplink(shipd) + " ist zusammen mit " + addedShips + " weiteren Schiffen der Schlacht beigetreten"));
			this.logme( "Die "+log_shiplink(shipd)+" ist zusammen mit "+addedShips+" weiteren Schiffen der Schlacht beigetreten\n\n" );
		}
		else
		{
			this.log(new SchlachtLogAktion(this.ownSide, "Die " + log_shiplink(shipd) + " ist der Schlacht beigetreten"));
			this.logme("Die "+log_shiplink(shipd)+" ist der Schlacht beigetreten\n\n");

			shipd.setBattle(this);
		}

		return true;
	}

	/**
	 * Gibt zurueck, auf welcher Seite ein Spieler Teil der Schlacht ist.
	 * Falls ein Spieler nicht Teil der Schlacht ist wird <code>-1</code>
	 * zurueckgegeben.
	 * @param user Der Spieler
	 * @return Die Seite oder <code>-1</code>
	 */
	public int getSchlachtMitglied(User user)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		for( int i=0; i <= 1; i++ )
		{
			if( user.getAlly() != null && user.getAlly().getId() == this.getAlly(i) )
			{
				return i;
			}
			if( this.getCommander(i).getId() == user.getId() )
			{
				return i;
			}
		}

		// Hat der Spieler ein Schiff in der Schlacht
		BattleShip aship = (BattleShip)db.createQuery("from BattleShip where id>0 and ship.owner=:user and battle=:battle")
				.setEntity("user", user)
				.setEntity("battle", this)
				.setMaxResults(1)
				.uniqueResult();

		if( aship != null ) {
			return aship.getSide();
		}
		return -1;
	}

	/**
	 * Laedt weitere Schlachtdaten aus der Datenbank.
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
		// Darf der Spieler (evt als Gast) zusehen?
		//

		int forceSide;

		if( forcejoin == 0 ) {
			forceSide = this.getSchlachtMitglied(user);
			if( forceSide == -1 )
			{
				//Mehr ueber den Spieler herausfinden
				if( context.hasPermission(WellKnownPermission.SCHLACHT_ALLE_AUFRUFBAR) ) {
					this.guest = true;
				}
				else
				{
					long shipcount = ((Number)db.createQuery("select count(*) from Ship " +
							"where owner= :user and x= :x and y= :y and system= :sys and " +
								"battle is null and shiptype.shipClass in (:shipClasses)")
							.setEntity("user", user)
							.setInteger("x", this.x)
							.setInteger("y", this.y)
							.setInteger("sys", this.system)
							.setParameterList("shipClasses", ShipClasses.darfSchlachtenAnsehen())
							.iterate().next()).longValue();
					if( shipcount > 0 ) {
						this.guest = true;
					}
					else
					{
						context.addError("Sie verf&uuml;gen &uuml;ber kein geeignetes Schiff im Sektor um die Schlacht zu verfolgen");
						return false;
					}
				}
			}
		}
		else
        {
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

		List<BattleShip> ships = Common.cast(db.createQuery("from BattleShip bs inner join fetch bs.ship as s " +
                "where s.id>0 and bs.battle=:battle " +
                "order by s.shiptype.id, s.id")
                .setEntity("battle", this)
                .list());

        for (BattleShip ship : ships) {

            if (ship.getSide() == this.ownSide) {
                this.ownShips.add(ship);
                if (!this.guest || !ship.getShip().isLanded()) {
                    if (ownShipTypeCount.containsKey(ship.getShip().getType())) {
                        ownShipTypeCount.put(ship.getShip().getType(), ownShipTypeCount.get(ship.getShip().getType()) + 1);
                    }
                    else
                    {
                        ownShipTypeCount.put(ship.getShip().getType(), 1);
                    }
                }
            }
            else if (ship.getSide() == this.enemySide) {
                this.enemyShips.add(ship);
                if (!ship.getShip().isLanded()) {
                    if (enemyShipTypeCount.containsKey(ship.getShip().getType())) {
                        enemyShipTypeCount.put(ship.getShip().getType(), enemyShipTypeCount.get(ship.getShip().getType()) + 1);
                    }
                    else
                    {
                        enemyShipTypeCount.put(ship.getShip().getType(), 1);
                    }
                }
            }
        }

		if(this.ownShips.isEmpty() || this.enemyShips.isEmpty())
		{
			return false;
		}

		//
		// aktive Schiffe heraussuchen
		//

		this.activeSEnemy = 0;
		this.activeSOwn = 0;

		setFiringShip(ownShip);
        setAttackedShip(enemyShip);

		// Falls die gewaehlten Schiffe gelandet (oder zerstoert) sind -> neue Schiffe suchen
		while( activeSEnemy < enemyShips.size() &&
			  ( this.enemyShips.get(activeSEnemy).hasFlag(BattleShipFlag.DESTROYED) ||
			 	this.enemyShips.get(activeSEnemy).getShip().isLanded() ) ) {
			this.activeSEnemy++;
		}

		if( activeSEnemy >= enemyShips.size() )
		{
			activeSEnemy = 0;
		}

		if( this.guest )
		{
			while( activeSOwn < ownShips.size() && this.ownShips.get(activeSOwn).getShip().isLanded() )
			{
				this.activeSOwn++;
			}

			if( activeSOwn >= ownShips.size() )
			{
				activeSOwn = 0;
			}
		}

		return true;
	}

    public void setFiringShip(Ship ownShip)
    {
        if(ownShip != null)
        {
            for(int i=0; i < ownShips.size(); i++)
            {
                if(ownShips.get(i).getId() == ownShip.getId())
                {
                    this.activeSOwn = i;
                    break;
                }
            }
        }
    }

    public void setAttackedShip(Ship enemyShip)
    {
        if(enemyShip != null)
        {
            for(int i = 0; i < enemyShips.size(); i++)
            {
                if(enemyShips.get(i).getId() == enemyShip.getId())
                {
                    this.activeSEnemy = i;
                    break;
                }
            }
        }
    }

	/**
	 * Loggt eine Nachricht fuer aktuellen Spieler.
	 * @param text Die zu loggende Nachricht
	 */
	public void logme( String text ) {
		this.logoutputbuffer.append(text);
	}

	/**
	 * Gibt die fuer den akuellen Spieler anzuzeigenden Nachrichten zurueck.
	 * @param raw Sollen die Nachrichten im Rohformat (unformatiert) zurueckgegeben werden?
	 * @return die Nachrichten
	 */
	public String getOwnLog( boolean raw ) {
		if( raw ) {
			return this.logoutputbuffer.toString();
		}
		return this.logoutputbuffer.toString().replace("\n", "<br />");
	}

	/**
	 * Fuegt einen Eintrag zum Schlachtlog hinzu.
	 * @param eintrag Der Eintrag
	 */
	public void log(SchlachtLogEintrag eintrag)
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();
		int tick = context.get(ContextCommon.class).getTick();
		eintrag.setTick(tick);

		if( this.schlachtLog == null ) {
			this.schlachtLog = new SchlachtLog(this, tick);
			db.persist(this.schlachtLog);
		}
		this.schlachtLog.add(eintrag);
		db.persist(eintrag);
	}

	/**
	 * Beendet die laufende Runde und berechnet einen Rundenwechsel.
	 * @param calledByUser Wurde das Rundenende (in)direkt durch einen Spieler ausgeloesst? (<code>true</code>)
	 *
	 * @return <code>true</code>, falls die Schlacht weiterhin existiert. <code>false</code>, falls sie beendet wurde.
	 *
	 */
	public boolean endTurn( boolean calledByUser )
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		List<List<BattleShip>> sides = new ArrayList<>();
		if( this.ownSide == 0 ) {
			sides.add(this.ownShips);
			sides.add(this.enemyShips);
		}
		else
        {
			sides.add(this.enemyShips);
			sides.add(this.ownShips);
		}

		//
		// Zuerst die Schiffe berechnen
		//
		for( int i=0; i < 2; i++ )
		{
			List<BattleShip> shipsSecond = new ArrayList<>();

			// Liste kopieren um Probleme beim Entfernen von Schiffen aus der Ursprungsliste zu vermeiden
			List<BattleShip> shiplist = new ArrayList<>(sides.get(i));
            for (BattleShip ship : shiplist)
            {
                if (ship.hasFlag(BattleShipFlag.HIT))
                {
                    ship.getShip().setAblativeArmor(ship.getAblativeArmor());
                    ship.getShip().setHull(ship.getHull());
                    ship.getShip().setShields(ship.getShields());
                    ship.getShip().setEngine(ship.getEngine());
                    ship.getShip().setWeapons(ship.getWeapons());
                    ship.getShip().setComm(ship.getComm());
                    ship.getShip().setSensors(ship.getSensors());
                    ship.removeFlag(BattleShipFlag.HIT);
                }
                else if (ship.hasFlag(BattleShipFlag.DESTROYED))
                {
                    if ( new ConfigService().getValue(WellKnownConfigValue.DESTROYABLE_SHIPS) )
                    {
                        //
                        // Verluste verbuchen (zerstoerte/verlorene Schiffe)
                        //
                        User destroyer = (User) db.get(User.class, ship.getDestroyer());
                        Ally destroyerAlly = destroyer.getAlly();
                        if (destroyerAlly != null) {
                            destroyerAlly.setDestroyedShips(destroyerAlly.getDestroyedShips() + 1);
                        }
                        destroyer.setDestroyedShips(destroyer.getDestroyedShips() + 1);

                        Ally looserAlly = ship.getOwner().getAlly();
                        if (looserAlly != null) {
                            looserAlly.setLostShips(looserAlly.getLostShips() + 1);
                        }
                        User looser = ship.getOwner();
                        looser.setLostShips(looser.getLostShips() + 1);

                        ShipLost lost = new ShipLost(ship.getShip());
                        lost.setDestAlly(destroyerAlly);
                        lost.setDestOwner(destroyer);
                        db.save(lost);

                        destroyShip(ship);
                        continue;
                    }
                    else
                    {
                        ship.removeFlag(BattleShipFlag.DESTROYED);
                        continue; //Das Schiff kann nicht zerstoert werden
                    }
                }


                if ( ship.hasFlag(BattleShipFlag.FLUCHT)) {
                    ShipTypeData ashipType = ship.getTypeData();
                    if (ashipType.getCost() > 0) {
                        removeShip(ship, true);
                    }
                    else
                    {
                        removeShip(ship, false);
                    }
                }

                ship.removeFlag(BattleShipFlag.SHOT);
                ship.removeFlag(BattleShipFlag.SECONDROW_BLOCKED);

                if (ship.hasFlag(BattleShipFlag.BLOCK_WEAPONS)) {
                    if (!((ship.getTypeData().getShipClass() == ShipClasses.GESCHUETZ) && ship.getShip().isDocked())) {
                        ship.removeFlag(BattleShipFlag.BLOCK_WEAPONS);
                    }
                }

                if ((i == 0) && this.hasFlag(BattleFlag.DROP_SECONDROW_0)) {
                    ship.removeFlag(BattleShipFlag.SECONDROW);
                }
                else if ((i == 1) && this.hasFlag(BattleFlag.DROP_SECONDROW_1)) {
					ship.removeFlag(BattleShipFlag.SECONDROW);
                }

                if (ship.hasFlag(BattleShipFlag.JOIN)) {
                    ShipTypeData ashipType = ship.getTypeData();
					if (ashipType.hasFlag(ShipTypeFlag.SECONDROW)) {
						shipsSecond.add(ship);
					}
                    ship.removeFlag(BattleShipFlag.JOIN);
                }

                Map<String, Integer> heat = ship.getWeaponHeat();

                for (String weaponName : heat.keySet()) {
                    heat.put(weaponName, 0);
                }

                if (ship.hasFlag(BattleShipFlag.FLUCHTNEXT)) {
					ship.removeFlag(BattleShipFlag.FLUCHTNEXT);
					ship.addFlag(BattleShipFlag.FLUCHT);
                }

                ship.getShip().setWeaponHeat(heat);
                ship.getShip().setBattleAction(false);
            }

			for(BattleShip second : shipsSecond){
				if(this.isSecondRowStable(i, second)){
					second.addFlag(BattleShipFlag.SECONDROW);
				}
			}
		}

		context.getDB().flush();

		// Ist die Schlacht zuende (weil keine Schiffe mehr vorhanden sind?)
		int owncount = this.ownShips.size();
		int enemycount = this.enemyShips.size();

		if( (owncount == 0) && (enemycount == 0) ) {
			PM.send(this.getCommanders()[this.enemySide], this.getCommanders()[this.ownSide].getId(), "Schlacht unentschieden", "Die Schlacht bei "+this.getLocation().displayCoordinates(false)+" gegen "+this.getCommanders()[this.enemySide].getName()+" wurde mit einem Unentschieden beendet!");
			PM.send(this.getCommanders()[this.ownSide], this.getCommanders()[this.enemySide].getId(), "Schlacht unentschieden", "Die Schlacht bei "+this.getLocation().displayCoordinates(false)+" gegen "+this.getCommanders()[this.ownSide].getName()+" wurde mit einem Unentschieden beendet!");

			// Schlacht beenden - unendschieden
			this.endBattle(0,0);

			this.ownShips.clear();
			this.enemyShips.clear();

			if( calledByUser ) {
				try
				{
                    context.getResponse().getWriter().append("Du hast die Schlacht bei <a class='forschinfo' href='./client#/map/")
						.append(this.getLocation().urlFragment()).append("'>")
						.append(this.getLocation().displayCoordinates(false))
						.append("</a> gegen ");
                    context.getResponse().getWriter().append(Common._title(this.getCommanders()[this.enemySide].getName()));
                    context.getResponse().getWriter().append(" mit einem Unentschieden beendet!");
                }
				catch( IOException e )
				{
					throw new RuntimeException(e);
				}
			}
			return false;
		}
		else if( owncount == 0 ) {
			PM.send(this.getCommanders()[this.enemySide], this.getCommanders()[this.ownSide].getId(), "Schlacht verloren", "Du hast die Schlacht bei "+this.getLocation().displayCoordinates(false)+" gegen "+this.getCommanders()[this.enemySide].getName()+" verloren!");
			PM.send(this.getCommanders()[this.ownSide], this.getCommanders()[this.enemySide].getId(), "Schlacht gewonnen", "Du hast die Schlacht bei "+this.getLocation().displayCoordinates(false)+" gegen "+this.getCommanders()[this.ownSide].getName()+" gewonnen!");

			// Schlacht beenden - eine siegreiche Schlacht fuer den aktive Seite verbuchen sowie eine verlorene fuer den Gegner
			if( this.ownSide == 0 ) {
				this.endBattle(-1,1);
			}
			else
            {
				this.endBattle(1,-1);
			}

			this.ownShips.clear();
			this.enemyShips.clear();


			if( calledByUser ) {
				try
				{
                    context.getResponse().getWriter().append("Du hast die Schlacht bei <a class='forschinfo' href='./client#/map/")
						.append(this.getLocation().urlFragment()).append("'>")
						.append(this.getLocation().displayCoordinates(false))
						.append("</a> gegen ");
                    context.getResponse().getWriter().append(Common._title(this.getCommanders()[this.enemySide].getName()));
                    context.getResponse().getWriter().append(" verloren!");
                }
				catch( IOException e )
				{
					throw new RuntimeException(e);
				}
			}
			return false;
		}
		else if( enemycount == 0 ) {
			PM.send(this.getCommanders()[this.enemySide], this.getCommanders()[this.ownSide].getId(), "Schlacht gewonnen", "Du hast die Schlacht bei "+this.getLocation().displayCoordinates(false)+" gegen "+this.getCommanders()[this.enemySide].getName()+" gewonnen!");
			PM.send(this.getCommanders()[this.ownSide], this.getCommanders()[this.enemySide].getId(), "Schlacht verloren", "Du hast die Schlacht bei "+this.getLocation().displayCoordinates(false)+" gegen "+this.getCommanders()[this.ownSide].getName()+" verloren!");

			// Schlacht beenden - eine siegreiche Schlacht fuer den aktive Seite verbuchen sowie eine verlorene fuer den Gegner
			if( this.ownSide == 0 ) {
				this.endBattle(1,-1);
			}
			else
            {
				this.endBattle(-1,1);
			}

			this.ownShips.clear();
			this.enemyShips.clear();

			if( calledByUser ) {
				try
				{
                    context.getResponse().getWriter().append("Du hast die Schlacht bei <a class='forschinfo' href='./client#/map/")
						.append(this.getLocation().urlFragment()).append("'>")
						.append(this.getLocation().displayCoordinates(false))
						.append("</a> gegen ");
                    context.getResponse().getWriter().append(Common._title(this.getCommanders()[this.enemySide].getName()));
                    context.getResponse().getWriter().append(" gewonnen!");
                }
				catch( IOException e )
				{
					throw new RuntimeException(e);
				}
			}
			return false;
		}

		this.ready1 = false;
		this.ready2 = false;
		this.blockcount = 2;

		this.lastturn = Common.time();

		for( int i=0; i < 2; i++ ) {
			if( !calledByUser && this.getTakeCommands()[i] != 0 ) {
				User com = (User)context.getDB().get(User.class, this.getTakeCommands()[i]);

				PM.send(com, this.getCommanders()[i].getId(), "Schlacht &uuml;bernommen", "Ich habe die Leitung der Schlacht bei "+this.getLocation().displayCoordinates(false)+" &uuml;bernommen.");

				this.log(new SchlachtLogAktion(i, "[Automatisch] "+Common._titleNoFormat(com.getName())+" kommandiert nun die gegnerischen Truppen"));

				this.setCommander(i, com);

				this.log(new SchlachtLogKommandantWechselt(i, this.getCommanders()[i]));

				this.setTakeCommand(i, 0);
			}
		}

		if( this.hasFlag(BattleFlag.FIRSTROUND) ) {
			this.setFlag(BattleFlag.FIRSTROUND, false);
		}

		this.setFlag(BattleFlag.BLOCK_SECONDROW_0, false);
		this.setFlag(BattleFlag.BLOCK_SECONDROW_1, false);

		if(this.hasFlag(BattleFlag.DROP_SECONDROW_0))
		{
			this.setFlag(BattleFlag.DROP_SECONDROW_0, false);
			this.setFlag(BattleFlag.BLOCK_SECONDROW_0, true);
		}
		if(this.hasFlag(BattleFlag.DROP_SECONDROW_1))
		{
			this.setFlag(BattleFlag.DROP_SECONDROW_1, false);
			this.setFlag(BattleFlag.BLOCK_SECONDROW_1, true);
		}

		return true;
	}

	@Transient
	private boolean deleted = false;

	/**
	 * Beendet die Schlacht.
	 * @param side1points Die Punkte, die die erste Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
	 * @param side2points Die Punkte, die die zweite Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
	 */
	public void endBattle(int side1points, int side2points) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if( deleted ) {
			log.warn("Mehrfacher Aufruf von Battle.endBattle festgestellt", new Throwable());
			return;
		}

		deleted = true;

		db.createQuery("delete from BattleShip where battle=:battle")
			.setEntity("battle", this)
			.executeUpdate();
		db.createQuery("update Ship set battle=null,battleAction=0 where id>0 and battle=:battle")
			.setEntity("battle", this)
			.executeUpdate();

		int[] points = new int[] {side1points, side2points};

		for( int i=0; i < points.length; i++ ) {
			if( this.getAllys()[i] != 0 ) {
				Ally ally = (Ally)db.get(Ally.class, this.getAllys()[i]);
				if( points[i] > 0 ) {
					ally.setWonBattles((short)(ally.getWonBattles()+points[i]));
				}
				else
                {
					ally.setLostBattles((short)(ally.getLostBattles()-points[i]));
				}
			}
			if( points[i] > 0 ) {
				 this.getCommanders()[i].setWonBattles((short)( this.getCommanders()[i].getWonBattles()+points[i]));
			}
			else
            {
				 this.getCommanders()[i].setLostBattles((short)( this.getCommanders()[i].getLostBattles()-points[i]));
			}
		}

		this.enemyShips.clear();
		this.ownShips.clear();

		db.delete(this);
	}

	/**
	 * Prueft, ob ein Spieler Kommandant der Schlacht ist.
	 * @param user Der Spieler
	 * @return <code>true</code>, falls er Kommandant ist
	 */
	public boolean isCommander( User user ) {
		return isCommander(user, -1);
	}

	/**
	 * Prueft, ob ein Spieler auf einer Seite Kommandant ist.
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
	 * Gibt die ID der aktuell ausgewaehlten gegnerischen Schiffsgruppe zurueck.
	 * @return Die ID der aktuellen gegnerischen Schiffsgruppe
	 */
	public String getEnemyShipGroup() {
		return enemyShipGroup;
	}

	/**
	 * Setzt die ID der aktuell ausgewaehlten gegnerischen Schiffsgruppe.
	 * @param enemyShipGroup Die neue Schiffsgruppen-ID
	 */
	public void setEnemyShipGroup(String enemyShipGroup) {
		this.enemyShipGroup = enemyShipGroup;
	}

	/**
	 * Gibt die ID der aktuell ausgewaehlten eigenen Schiffsgruppe zurueck.
	 * @return Die ID der aktuellen eigenen Schiffsgruppe
	 */
	public String getOwnShipGroup() {
		return ownShipGroup;
	}

	/**
	 * Setzt die ID der aktuell ausgewaehlten eigenen Schiffsgruppe.
	 * @param ownShipGroup Die neue Schiffsgruppen-ID
	 */
	public void setOwnShipGroup(String ownShipGroup) {
		this.ownShipGroup = ownShipGroup;
	}

	/**
	 * Gibt die ID der gegnerischen Seite zurueck.
	 * @return Die ID der gegnerischen Seite
	 */
	public int getEnemySide() {
		return enemySide;
	}

	/**
	 * Gibt die ID der eigenen Seite zurueck.
	 * @return Die ID der eigenen Seite
	 */
	public int getOwnSide() {
		return ownSide;
	}

	/**
	 * Gibt die Liste der gegnerischen Schiffe zurueck.
	 * @return Die Liste der gegnerischen Schiffe
	 */
	public List<BattleShip> getEnemyShips() {
		return enemyShips;
	}

	/**
	 * Gibt das gegnerische Schiff mit dem angegebenen Index (nicht ID!) zurueck.
	 * @param index Der Index
	 * @return Das Schiff
	 */
	public BattleShip getEnemyShip( int index ) {
		return enemyShips.get(index);
	}

	/**
	 * Gibt die Liste der eigenen Schiffe zurueck.
	 * @return Die Liste der eigenen Schiffe
	 */
	public List<BattleShip> getOwnShips() {
		return ownShips;
	}

	/**
	 * Gibt das eigene Schiff mit dem angegebenen Index (nicht ID!) zurueck.
	 * @param index Der Index
	 * @return Das Schiff
	 */
	public BattleShip getOwnShip( int index ) {
		return ownShips.get(index);
	}

	/**
	 * Gibt die mit einer Seite assoziierte Allianz zurueck.
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
	 * Gibt den Kommandanten einer Seite zurueck.
	 * @param side Die Seite
	 * @return Der Spieler
	 */
	public User getCommander(int side) {
		return side == 0 ? this.commander1 : this.commander2;
	}

	/**
	 * Setzt den Kommandaten einer Seite.
	 * @param side Die Seite
	 * @param user Der neue Kommandant
	 */
	public void setCommander(int side, User user) {
		if( side == 0 ) {
			this.commander1 = user;
		}
		else
        {
			this.commander2 = user;
		}
	}

	private User[] getCommanders() {
		return new User[] {this.commander1, this.commander2};
	}

	/**
	 * Prueft, ob eine Seite mit ihren Aktionen in der Runde fertig ist.
	 * @param side Die Seite
	 * @return <code>true</code>, falls sie mit ihren Aktionen in der Runde fertig ist
	 */
	public boolean isReady(int side) {
		return side == 0 ? this.ready1 : this.ready2;
	}

	/**
	 * Setzt den "fertig"-Status einer Seite fuer die aktuelle Runde.
	 * @param side Die Seite
	 * @param ready Der Status
	 */
	public void setReady(int side, boolean ready) {
		if( side == 0 ) {
			this.ready1 = ready;
		}
		else
        {
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
	 * Setzt die ID eines Spielers, welcher auf einer Seite das Kommando uebernehmen will.
	 * @param side Die Seite
	 * @param id Die ID des Spielers
	 */
	public void setTakeCommand(int side, int id) {
		if( side == 0 ) {
			this.takeCommand1 = id;
		}
		else
        {
			this.takeCommand2 = id;
		}
	}

	private int[] getTakeCommands() {
		return new int[] {this.takeCommand1, this.takeCommand2};
	}

	/**
	 * Gibt eine Map fuer eine Seite zurueck, welche als Schluessel Schiffstypen und als zugeordnete
	 * Werte die Anzahl der vorkommenden Schiffes des Typs auf der Seite hat.
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
	 * Gibt zurueck, wie oft ein Schiffstyp auf einer Seite vorkommt.
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
	 * Gibt zurueck, wie oft ein Schiffstyp auf der eigenen Seite vorkommt.
	 * @param shiptype Die ID des Schifftyps
	 * @return Die Anzahl der vorkommenden Schiffe des Schifftyps
	 */
	public int getOwnShipTypeCount(int shiptype) {
		return getShipTypeCount(this.ownSide, shiptype);
	}

	/**
	 * Gibt zurueck, wie oft ein Schiffstyp auf der gegnerischen Seite vorkommt.
	 * @param shiptype Die ID des Schifftyps
	 * @return Die Anzahl der vorkommenden Schiffe des Schifftyps
	 */
	public int getEnemyShipTypeCount(int shiptype) {
		return getShipTypeCount(this.enemySide, shiptype);
	}

	@Override
	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * Ermittelt den Kampfstärke einer Seite.
	 *
	 * @param side Die Seite, deren Kampfstärke abgefragt werden soll
	 * @return Die Kampfstärke einer Seite
	 */
	public int getBattleValue(Side side)
	{
		int battleValue = 0;
		List<BattleShip> ships = getShips(side);

		for(BattleShip ship: ships)
		{
			battleValue += ship.getBattleValue();
		}

		return battleValue;
	}

	/**
	 * Gets the ships of one battle party.
	 *
	 * @param side Own side or enemy side.
	 * @return Ships, which fight for the given side.
	 */
	public List<BattleShip> getShips(Side side)
	{
		if(side == Side.OWN)
		{
			return Collections.unmodifiableList(getOwnShips());
		}
		return Collections.unmodifiableList(getEnemyShips());
	}

	/**
	 * Zerstoert ein Schiff und alle an ihm gedockten Schiff.
	 * @param ship Das zu zerstoerende Schiff
	 */
	private void destroyShip( BattleShip ship ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if( !ship.getShip().isDestroyed() )
		{
			db.delete(ship);
			ship.getShip().destroy();
		}

		//
		// Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
		//

		List<BattleShip> shiplist = this.ownShips;

		if( ship.getSide() != this.ownSide ) {
			shiplist = this.enemyShips;
		}

		for( int i=0; i < shiplist.size(); i++ ) {
			BattleShip aship = shiplist.get(i);

			if( aship == ship ) {
				shiplist.remove(i);
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
	 * um die Schlacht herum. Evt gedockte Schiffe werden mitentfernt und im selben Sektor platziert.
	 * @param ship Das fliehende Schiff
	 * @param relocate Soll ein zufaelliger Sektor um die Schlacht herum gewaehlt werden? (<code>true</code>)
	 */
	public void removeShip( BattleShip ship, boolean relocate ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Location loc = ship.getShip().getLocation();

		if(relocate && !ship.getShip().isLanded() && !ship.getShip().isDocked()) {
			StarSystem sys = (StarSystem)db.get(StarSystem.class, this.system);
			int maxRetries = 100;

			while( ((loc.getX() == this.x) && (loc.getY() == this.y)) ||
					(loc.getX() < 1) || (loc.getY() < 1) ||
					(loc.getX() > sys.getWidth()) ||
					(loc.getY() > sys.getHeight()) ) {
				loc = loc.setX(this.x + ThreadLocalRandom.current().nextInt(3) - 1);
				loc = loc.setY(this.y + ThreadLocalRandom.current().nextInt(3) - 1);

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
		if( ship.getShip().isDocked() || ship.getShip().isLanded() ) {
			int masterid = ship.getShip().getBaseShip().getId();

			List<BattleShip> shiplist = this.ownShips;
			if( ship.getSide() != this.ownSide ) {
				shiplist = this.enemyShips;
			}

            for (BattleShip aship : shiplist) {
                if (aship.getId() == masterid) {
                    removeShip(aship, relocate);
                    return;
                }
            }
		}

		long dockcount = ship.getShip().getAnzahlGedockterUndGelandeterSchiffe();

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
			else if(dockcount > 0 && aship.getShip().getBaseShip() != null && aship.getShip().getBaseShip().getId() == ship.getId() ) {
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

	/**
	 *
	 * @param user Der Spieler von dem die Nahrungsbalance berechnet werden soll.
	 * @return Gibt die Nahrungsbalance der Schlacht zurueck.
	 */
	public int getNahrungsBalance(User user)
	{
		int balance = 0;

		org.hibernate.Session db = ContextMap.getContext().getDB();
		ScrollableResults ships = db.createQuery("from Ship where owner=:owner and id>0 and battle=:battle")
			.setParameter("owner", user)
			.setParameter("battle", this)
			.setCacheMode(CacheMode.IGNORE)
			.scroll(ScrollMode.FORWARD_ONLY);

		while(ships.next())
		{
			Ship ship = (Ship)ships.get(0);
			balance -= ship.getNahrungsBalance();
            db.evict(ship);
		}

		return balance;
	}

	/**
	 * Setzt die mit einer Seite assoziierte Allianz.
	 * @param side Die Seite
	 * @param ally Die Allianz
	 */
	public void setAlly(int side, int ally) {
		if (side == 0)
		{
			this.ally1 = ally;
		}
		if (side == 1)
		{
			this.ally2 = ally;
		}
	}

	/**
	 * Gibt das Log zu dieser Schlacht zurueck.
	 * @return Das Log
	 */
	public SchlachtLog getSchlachtLog()
	{
		return schlachtLog;
	}

	/**
	 * Setzt das Log zu dieser Schlacht.
	 * @param schlachtLog Das Log
	 */
	public void setSchlachtLog(SchlachtLog schlachtLog)
	{
		this.schlachtLog = schlachtLog;
	}
}
