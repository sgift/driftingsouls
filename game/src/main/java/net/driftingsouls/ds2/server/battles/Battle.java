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

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Repraesentiert eine Schlacht in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="battles", indexes = {
	@Index(name="battle_coords", columnList = "x, y, system")
})
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@BatchSize(size=50)
@OptimisticLocking(type = OptimisticLockType.DIRTY)
public class Battle implements Locatable
{
	@Id @GeneratedValue
	private int id;
	private int x;
	private int y;
	private int system;
	private int ally1;
	private int ally2;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="commander1", nullable=false, foreignKey = @ForeignKey(name="battles_fk_users1"))
	private User commander1;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="commander2", nullable=false, foreignKey = @ForeignKey(name="battles_fk_users2"))
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
	@OneToOne()
	@JoinColumn(foreignKey = @ForeignKey(name="battles_fk_schlachtlog"))
	private SchlachtLog schlachtLog;

	@Version
	private int version;

	@Transient
	private String ownShipGroup = "0";
	@Transient
	private String enemyShipGroup = "0";
	@Transient
	private boolean deleted = false;

	@Transient
	private int ownSide;
	@Transient
	private int enemySide;

	@Transient
	private final List<BattleShip> ownShips = new ArrayList<>();
	@Transient
	private final List<BattleShip> enemyShips = new ArrayList<>();

	@Transient
	private final List<List<Integer>> addCommanders = new ArrayList<>();

	@Transient
	private boolean guest = false;

	@Transient
	private final Map<Integer,Integer> ownShipTypeCount = new HashMap<>();
	@Transient
	private final Map<Integer,Integer> enemyShipTypeCount = new HashMap<>();

	@Transient
	private int activeSOwn = 0;
	@Transient
	private int activeSEnemy = 0;

	@Transient
	private final StringBuilder logoutputbuffer = new StringBuilder();

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
	 * Gibt die Anzahl der eigenen Schiffe zurueck.
	 * @return die Anzahl
	 */
	public int getOwnShipCount() {
		return ownShips.size();
	}


	/**
	 * Gibt die Anzahl der feindlichen Schiffe zurueck.
	 * @return die Anzahl
	 */
	public int getEnemyShipCount() {
		return enemyShips.size();
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

        return ((int)secondrowcaps) == 0 || ((int)(owncaps)) >= ((int)secondrowcaps) * 2;

		}


	/**
* Prueft, ob eine zweite Reihe existiert
* @param side Die Seite deren zweite Reihe geprueft werden soll
* @return <code>true</code>, falls eine zweite Reihe exitiert
*/
public boolean hasSecondRow( int side) {
	List<BattleShip> shiplist;
	if( side == this.ownSide ) {
		shiplist = getOwnShips();
	}
	else{
		shiplist = getEnemyShips();
	}

	int counter = 0;
	for (BattleShip aship : shiplist) {
			if (aship.hasFlag(BattleShipFlag.JOIN)) {
					continue;
			}

			if (aship.hasFlag(BattleShipFlag.SECONDROW)) {
					if (!aship.getShip().isDocked() && !aship.getShip().isLanded()) {
							counter ++;
					}
			}
	}
	return counter > 0;
}

	/**
* Prueft, ob fiehende / beitretende Schiffe existiert
* @param side Die Seite deren zweite Reihe geprueft werden soll
* @return <code>true</code>, falls eine zweite Reihe exitiert
*/
public boolean hasJoinFluchtRow( int side) {
	List<BattleShip> shiplist;
	if( side == this.ownSide ) {
		shiplist = getOwnShips();
	}
	else{
		shiplist = getEnemyShips();
	}

	int counter = 0;
	for (BattleShip aship : shiplist) {
			if (aship.hasFlag(BattleShipFlag.JOIN) || aship.hasFlag(BattleShipFlag.FLUCHT)) {
					counter ++;
			}
	}
	return counter > 0;
}

	/**
* Prueft, ob eine erste Reihe existiert
* @param side Die Seite deren zweite Reihe geprueft werden soll
* @return <code>true</code>, falls eine zweite Reihe exitiert
*/
public boolean hasFrontRow( int side) {

	List<BattleShip> shiplist;
	if( side == this.ownSide ) {
		shiplist = getOwnShips();
	}
	else{
		shiplist = getEnemyShips();
	}


	int counter = 0;
	for (BattleShip aship : shiplist) {
			if (!(aship.hasFlag(BattleShipFlag.JOIN) || aship.hasFlag(BattleShipFlag.FLUCHT) || aship.hasFlag(BattleShipFlag.SECONDROW))) {
					counter ++;
			}
	}
	return counter > 0;
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
	 * Liefert das naechste eigene Schiff nach dem aktuell ausgewaehlten.
	 * @return Das naechste passende BattleShip
	 */
	public BattleShip getNextOwnBattleShip() {

		// alle Schiffe ab dem aktiven durchlaufen
		for( int i= activeSOwn+1; i < ownShips.size(); i++){

			BattleShip aship = ownShips.get(i);
			if( !aship.getShip().isLanded() &&
						!aship.hasFlag(BattleShipFlag.DESTROYED) &&
						!aship.hasFlag(BattleShipFlag.FLUCHT) &&
						!aship.hasFlag(BattleShipFlag.JOIN) ) {
					return aship;
				}
		}
		// nichts gefunden, dann von vorne bis zum aktiven
		for( int i= 0; i < activeSOwn; i++){

			BattleShip aship = ownShips.get(i);
			if( !aship.getShip().isLanded() &&
						!aship.hasFlag(BattleShipFlag.DESTROYED) &&
						!aship.hasFlag(BattleShipFlag.FLUCHT) &&
						!aship.hasFlag(BattleShipFlag.JOIN) ) {
					return aship;
				}
		}
		// immer noch nichts gefunden, dann bleibt es beim aktuellen
		return ownShips.get(activeSOwn);

	}

	/**
	 * Liefert das naechste feindlioche Schiff nach dem aktuell ausgewaehlten.
	 * @return Das naechste passende BattleShip
	 */
	public BattleShip getNextEnemyBattleShip() {

		// alle Schiffe ab dem aktiven durchlaufen
		for (int i = activeSEnemy + 1; i < enemyShips.size(); i++) {

			BattleShip ship = enemyShips.get(i);
			if (isNextEnemyShipCandidate(ship)) {
				return ship;
			}
		}
		// nichts gefunden, dann von vorne bis zum aktiven
		for (int i = 0; i < activeSEnemy; i++) {

			BattleShip ship = enemyShips.get(i);
			if (isNextEnemyShipCandidate(ship)) {
				return ship;
			}
		}
		// immer noch nichts gefunden, dann bleibt es beim aktuellen
		return enemyShips.get(activeSEnemy);

	}

	private boolean isNextEnemyShipCandidate(BattleShip ship) {
		return 	!ship.getShip().isLanded() &&
				!ship.hasFlag(BattleShipFlag.DESTROYED) &&
				!ship.hasFlag(BattleShipFlag.FLUCHT) &&
				!ship.hasFlag(BattleShipFlag.JOIN) &&
				!ship.hasFlag(BattleShipFlag.SECONDROW);
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
	 * Gibt das eigene Schiff mit der angegebenen ID oder null zurueck, falls nicht gefunden.
	 * @param shipID Die Schiffs-ID
	 * @return Das Schiff
	 */
	public BattleShip getShipByID( int shipID ) {
		if(shipID <= 0) {
			return null;
		}

		return ownShips.stream()
				.filter(ownShip -> ownShip.getId() == shipID)
				.findAny()
				.orElse(null);
	}

	/**
	 * Gibt die mit einer Seite assoziierte Allianz zurueck.
	 * @param side Die Seite
	 * @return Die ID der Allianz oder 0
	 */
	public int getAlly(int side) {
		return side == 0 ? ally1 : ally2;
	}

	public int[] getAllys() {
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

	public User[] getCommanders() {
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

	public int[] getTakeCommands() {
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

	public void setGuest(boolean guest) {
		this.guest = guest;
	}

	public void setOwnSide(int ownSide) {
		this.ownSide = ownSide;
	}

	public void setEnemySide(int enemySide) {
		this.enemySide = enemySide;
	}

	public Map<Integer, Integer> getOwnShipTypeCount() {
		return ownShipTypeCount;
	}

	public Map<Integer, Integer> getEnemyShipTypeCount() {
		return enemyShipTypeCount;
	}

	public void setActiveSEnemy(int activeSEnemy) {
		this.activeSEnemy = activeSEnemy;
	}

	public void setActiveSOwn(int activeSOwn) {
		this.activeSOwn = activeSOwn;
	}

	public int getActiveSEnemy() {
		return activeSEnemy;
	}

	public int getActiveSOwn() {
		return activeSOwn;
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

	public void setBlockcount(int blockcount) {
		this.blockcount = blockcount;
	}

	public void setLastturn(long lastturn) {
		this.lastturn = lastturn;
	}

	public boolean getDeleted() {
		return this.deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public StringBuilder getLogoutputbuffer() {
		return logoutputbuffer;
	}
}
