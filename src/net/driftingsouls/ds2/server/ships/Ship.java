/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.ships;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.Transfer;
import net.driftingsouls.ds2.server.cargo.Transfering;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Sector;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.werften.ShipWerft;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Repraesentiert ein Schiff in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ships")
@Configurable
public class Ship implements Locatable,Transfering {
	private static final Log log = LogFactory.getLog(Ship.class);
	
	/**
	 * Objekt mit Funktionsmeldungen.
	 */
	public static final ContextLocalMessage MESSAGE = new ContextLocalMessage();
	
	@Id @GeneratedValue(generator="ds-shipid")
	@GenericGenerator(name="ds-shipid", strategy = "net.driftingsouls.ds2.server.ships.ShipIdGenerator")
	private int id;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="modules", nullable=true)
	private ShipModules modules;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="owner", nullable=false)
	private User owner;
	private String name;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="type", nullable=false)
	private ShipType shiptype; 
	@Type(type="cargo")
	private Cargo cargo;
	private int x;
	private int y;
	private int system;
	private String status;
	private int crew;
	private int marines;
	private int e;
	@Column(name="s")
	private int heat;
	private int hull;
	private int shields;
	@Column(name="heat")
	private String weaponHeat;
	private int engine;
	private int weapons;
	private int comm;
	private int sensors;
	private String docked;
	private int alarm;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="fleet", nullable=true)
	private ShipFleet fleet;
	private int destsystem;
	private int destx;
	private int desty;
	private String destcom;
	private boolean bookmark;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="battle", nullable=true)
	private Battle battle;
	private boolean battleAction;
	private String jumptarget;
	private byte autodeut;
	private String history;
	private String script;
	private Blob scriptexedata;
	private String oncommunicate;
	@Column(name="`lock`")
	private String lock;
	private Integer visibility;
	private String onmove;
	private Byte respawn;
	private int ablativeArmor;
	private boolean startFighters;
	
	@Transient
	private Offizier offizier;
	
	@Version
	private int version;
	
	@Transient
	private boolean destroyed = false;
	
	@Transient
	private Configuration config;
	
	/**
	 * Konstruktor.
	 */
	public Ship() {
		// EMPTY
	}
	
	/**
	 * Erstellt ein neues Schiff.
	 * @param owner Der Besitzer
	 */
	@Deprecated
	public Ship(User owner) {
		this.owner = owner;
		this.cargo = new Cargo();
		this.destcom = "";
		this.name = "";
		this.status = "";
		this.jumptarget = "";
		this.history = "";
		this.docked = "";
		this.weaponHeat = "";
		this.autodeut = 1;
	}
	
	/**
	 * Erstellt ein neues Schiff.
	 * @param owner Der Besitzer
	 * @param shiptype Der Schiffstyp
	 * @param system Das System
	 * @param x Die X-Koordinate
	 * @param y Die Y-Koordinate
	 */
	public Ship(User owner, ShipType shiptype, int system, int x, int y)
	{
		this.owner = owner;
		this.cargo = new Cargo();
		this.destcom = "";
		this.name = "";
		this.status = "";
		this.jumptarget = "";
		this.history = "";
		this.docked = "";
		this.weaponHeat = "";
		this.autodeut = 1;
		this.setName(name);
		this.setBaseType(shiptype);
		this.setX(x);
		this.setY(y);
		this.setSystem(system);
		this.setHull(shiptype.getHull());
		this.setAblativeArmor(shiptype.getAblativeArmor());
		this.setEngine(100);
		this.setWeapons(100);
		this.setComm(100);
		this.setSensors(100);
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

	/**
	 * Gibt die ID des Schiffes zurueck.
	 * @return Die ID des Schiffes
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Setzt die ID des Schiffes.
	 * @param id Die ID
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Gibt den Besitzer des Schiffes zurueck.
	 * @return Der Besitzer
	 */
	public User getOwner() {
		return this.owner;
	}
	
	/**
	 * Setzt den Besitzer des Schiffes.
	 * @param owner Der Besitzer des Schiffes
	 */
	public void setOwner(User owner) {
		this.owner = owner;
	}
	
	/**
	 * Gibt den Namen des Schiffes zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Setzt den Namen des Schiffes.
	 * @param name Der neue Name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gibt den Typ des Schiffes zurueck.
	 * @return Der Typ
	 */
	public int getType() {
		return this.shiptype.getId();
	}
	
	/**
	 * Gibt das zugrunde liegende Schiffstypen-Objekt zurueck.
	 * @return Das Schiffstypen-Objekt
	 */
	public ShipType getBaseType() {
		return this.shiptype;
	}
	
	/**
	 * Setzt den Typ des Schiffes.
	 * @param type Der neue Typ
	 */
	public void setBaseType(ShipType type) {
		this.shiptype = type;
	}
	
	/**
	 * Gibt den Cargo des Schiffes zurueck.
	 * @return Der Cargo
	 */
	public Cargo getCargo() {
		return new Cargo(this.cargo);
	}
	
	/**
	 * Setzt den Cargo des Schiffes.
	 * @param cargo Der neue Cargo
	 */
	public void setCargo(Cargo cargo) {
		this.cargo = new Cargo(cargo);
	}
	
	/**
	 * Gibt die X-Koordinate des Schiffes zurueck.
	 * @return Die X-Koordinate
	 */
	public int getX() {
		return this.x;
	}
	
	/**
	 * Setzt die X-Koordinate des Schiffes.
	 * @param x Die X-Koordinate
	 */
	public void setX(int x) {
		this.x = x;
	}
	
	/**
	 * Gibt die Y-Koordinate des Schiffes zurueck.
	 * @return Die Y-Koordinate
	 */
	public int getY() {
		return this.y;
	}
	
	/**
	 * Setzt die Y-Koordinate des Schiffes.
	 * @param y Die Y-Koordinate
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * Gibt das Sternensystem des Schiffes zurueck.
	 * @return Das Sternensystem
	 */
	public int getSystem() {
		return this.system;
	}
	
	/**
	 * Setzt das Sternensystem des Schiffes.
	 * @param system Das Sternensystem
	 */
	public void setSystem(int system) {
		this.system = system;
	}

	/**
	 * Gibt die Alarmstufe des Schiffes zurueck.
	 * @return Die Alarmstufe
	 */
	public int getAlarm() {
		return alarm;
	}

	/**
	 * Setzt die Alarmstufe des Schiffes.
	 * @param alarm Die neue Alarmstufe
	 */
	public void setAlarm(int alarm) {
		this.alarm = alarm;
	}

	/**
	 * Gibt zurueck, ob das Schiff automatisch Deuterium sammeln soll oder nicht.
	 * @return <code>true</code>, falls das Schiff automatisch Deuterium sammeln soll
	 */
	public boolean getAutoDeut() {
		return autodeut != 0;
	}

	/**
	 * Setzt das automatische Deuteriumsammeln.
	 * @param autodeut <code>true</code>, falls das Schiff automatisch Deuterium sammeln soll
	 */
	public void setAutoDeut(boolean autodeut) {
		this.autodeut = autodeut ? (byte)1 : 0;
	}

	/**
	 * Gibt die Schlacht zurueck, in der dich das Schiff befindet.
	 * @return Die Schlacht oder <code>null</code>
	 */
	public Battle getBattle() {
		return battle;
	}

	/**
	 * Setzt die Schlacht in der sich das Schiff befindet.
	 * @param battle Die Schlacht
	 */
	public void setBattle(Battle battle) {
		this.battle = battle;
	}

	/**
	 * Gibt zurueck, ob das Schiff in der Schlacht eine Aktion durchgefuehrt hat.
	 * @return <code>true</code>, falls das Schiff in der Schlacht bereits eine Aktion durchgefuehrt hat
	 */
	public boolean isBattleAction() {
		return battleAction;
	}

	/**
	 * Setzt den Aktionsstatus des Schiffes in einer Schlacht.
	 * @param battleAction <code>true</code>, falls das Schiff in der Schlacht bereits eine Aktion durchgefuehrt hat
	 */
	public void setBattleAction(boolean battleAction) {
		this.battleAction = battleAction;
	}

	/**
	 * Gibt zurueck, ob das Schiff gebookmarkt ist.
	 * @return <code>true</code>, falls es ein Lesezeichen hat
	 */
	public boolean isBookmark() {
		return bookmark;
	}

	/**
	 * Setzt den Lesezeichenstatus fuer ein Schiff.
	 * @param bookmark <code>true</code>, falls es ein Lesezeichen haben soll
	 */
	public void setBookmark(boolean bookmark) {
		this.bookmark = bookmark;
	}

	/**
	 * Gibt den Status des Kommunikationssubsystems zurueck.
	 * @return Der Status der Kommunikation
	 */
	public int getComm() {
		return comm;
	}

	/**
	 * Setzt den Status des Kommunikationssubsystems.
	 * @param comm Das neue Status
	 */
	public void setComm(int comm) {
		this.comm = comm;
	}

	/**
	 * Gibt die Crewanzahl auf dem Schiff zurueck.
	 * @return Die Crewanzahl
	 */
	public int getCrew() {
		return crew;
	}

	/**
	 * Setzt die Crewanzahl auf dem Schiff.
	 * @param crew Die neue Crewanzahl
	 */
	public void setCrew(int crew) {
		this.crew = crew;
	}

	/**
	 * Gibt die Anzahl Marines auf dem Schiff zurueck.
	 * @return Die Anzahl Marines
	 */
	public int getMarines() {
		return marines;
	}

	/**
	 * Setzt die Anzahl Marines auf dem Schiff.
	 * @param marines Die neue Anzahl Marines
	 */
	public void setMarines(int marines) {
		this.marines = marines;
	}
	
	/**
	 * Gibt die Energiemenge auf dem Schiff zurueck.
	 * @return Die Energiemenge
	 */
	public int getEnergy() {
		return e;
	}

	/**
	 * Setzt die Energiemenge auf dem Schiff.
	 * @param e Die neue Energiemenge
	 */
	public void setEnergy(int e) {
		this.e = e;
	}

	/**
	 * Gibt die Antriebshitze zurueck.
	 * @return Die Antriebshitze
	 */
	public int getHeat() {
		return heat;
	}

	/**
	 * Setzt die Antriebshitze.
	 * @param s Die neue Antriebshitze
	 */
	public void setHeat(int s) {
		this.heat = s;
	}

	/**
	 * Gibt den Huellenstatus zurueck.
	 * @return Der Huellenstatus
	 */
	public int getHull() {
		return hull;
	}

	/**
	 * Setzt den Huellenstatus.
	 * @param hull Der neue Huellenstatus
	 */
	public void setHull(int hull) {
		this.hull = hull;
	}

	/**
	 * Gibt den Schildstatus zurueck.
	 * @return Der Schildstatus
	 */
	public int getShields() {
		return shields;
	}

	/**
	 * Setzt den Schildstatus.
	 * @param shields Der neue Schildstatus
	 */
	public void setShields(int shields) {
		this.shields = shields;
	}

	/**
	 * Gibt die Waffenhitze zurueck.
	 * @return heat Die Waffenhitze
	 */
	public String getWeaponHeat() {
		return this.weaponHeat;
	}
	
	/**
	 * Setzt die Waffenhitze.
	 * @param heat Die neue Waffenhitze
	 */
	public void setWeaponHeat(String heat) {
		this.weaponHeat = heat;
	}

	/**
	 * Gibt die Flotte zurueck in der sich das Schiff befindet.
	 * @return Die Flotte
	 */
	public ShipFleet getFleet() {
		return fleet;
	}

	/**
	 * Setzt die Flotte in der sich das Schiff befindet.
	 * @param fleet Die neue Flotte
	 */
	public void setFleet(ShipFleet fleet) {
		this.fleet = fleet;
	}

	/**
	 * Gibt den Status des Antriebssubsystems zurueck.
	 * @return Der Status des Antriebs
	 */
	public int getEngine() {
		return engine;
	}

	/**
	 * Setzt den Status des Antriebssubsystems.
	 * @param engine Der neue Status
	 */
	public void setEngine(int engine) {
		this.engine = engine;
	}

	/**
	 * Gibt den Status der Sensoren zurueck.
	 * @return Der Status
	 */
	public int getSensors() {
		return sensors;
	}

	/**
	 * Setzt den Status des Sensorsubsystems.
	 * @param sensors Der neue Status
	 */
	public void setSensors(int sensors) {
		this.sensors = sensors;
	}

	/**
	 * Gibt den Status des Waffensubsystems zurueck.
	 * @return Der Status des Waffensubsystems
	 */
	public int getWeapons() {
		return weapons;
	}

	/**
	 * Setzt den Status des Waffensubsystems.
	 * @param weapons Der neue Status
	 */
	public void setWeapons(int weapons) {
		this.weapons = weapons;
	}

	/**
	 * Gibt die Dochdaten des Schiffes zurueck.
	 * @return Die Dockdaten
	 */
	public String getDocked() {
		return docked;
	}

	/**
	 * Setzt die Dockdaten des Schiffes.
	 * @param docked Die neuen Dockdaten
	 */
	public void setDocked(String docked) {
		this.docked = docked;
	}

	/**
	 * Gibt das Statusfeld des Schiffes zurueck.
	 * @return Das Statusfeld
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Setzt das Statusfeld des Schiffes.
	 * @param status das neue Statusfeld
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Gibt den mit dem Schiff assoziierten Kommentar zurueck.
	 * @return Der Kommentar
	 */
	public String getDestCom() {
		return destcom;
	}

	/**
	 * Setzt den Kommentar des Schiffes.
	 * @param destcom Der Kommentar
	 */
	public void setDestCom(String destcom) {
		this.destcom = destcom;
	}

	/**
	 * Gibt das Zielsystem zurueck.
	 * @return Das Zielsystem
	 */
	public int getDestSystem() {
		return destsystem;
	}

	/**
	 * Setzt das Zielsystem.
	 * @param destsystem Das neue Zielsystem
	 */
	public void setDestSystem(int destsystem) {
		this.destsystem = destsystem;
	}

	/**
	 * Gibt die Ziel-X-Koordinate zurueck.
	 * @return Die Ziel-X-Koordinate
	 */
	public int getDestX() {
		return destx;
	}

	/**
	 * Setzt die Ziel-X-Koordinate.
	 * @param destx Die neue X-Koordinate
	 */
	public void setDestX(int destx) {
		this.destx = destx;
	}

	/**
	 * Gibt die Ziel-Y-Koordinate zurueck.
	 * @return Die Ziel-Y-Koordinate
	 */
	public int getDestY() {
		return desty;
	}

	/**
	 * Setzt die Ziel-Y-Koordinate.
	 * @param desty Die neue Y-Koordinate
	 */
	public void setDestY(int desty) {
		this.desty = desty;
	}

	/**
	 * Gibt das Sprungziel zurueck, welches von diesem Schiff aus angesprungen werden kann.
	 * @return Das Sprungziel
	 */
	public String getJumpTarget() {
		return jumptarget;
	}

	/**
	 * Setzt das Sprungziel, welches von diesem Schiff aus angesprungen werden kann.
	 * @param jumptarget Das Sprungziel (Koordinatenangabe)
	 */
	public void setJumpTarget(String jumptarget) {
		this.jumptarget = jumptarget;
	}

	/**
	 * Gibt die Schiffshistorie zurueck.
	 * @return Die Schiffshistorie
	 */
	public String getHistory() {
		return history;
	}

	/**
	 * Setzt die Schiffshistorie.
	 * @param history Die neue Schiffshistorie
	 */
	public void setHistory(String history) {
		this.history = history;
	}

	/**
	 * Gibt den Status des Schiffslocks zurueck.
	 * @return Das Schiffslock
	 */
	public String getLock() {
		return lock;
	}

	/**
	 * Setzt den Schiffslock.
	 * @param lock Der neue Schiffslock
	 */
	public void setLock(String lock) {
		this.lock = lock;
	}

	/**
	 * Gibt die Daten des OnCommunicate-Ereignisses zurueck.
	 * @return Die Ausfuehrungsdaten
	 */
	public String getOnCommunicate() {
		return oncommunicate;
	}

	/**
	 * Setzt die Ausfuehrungsdaten des OnCommunicate-Ereignisses.
	 * @param oncommunicate Die neuen Ausfuehrungsdaten
	 */
	public void setOnCommunicate(String oncommunicate) {
		this.oncommunicate = oncommunicate;
	}

	/**
	 * Gibt die Daten des OnMove-Ereignisses zurueck.
	 * @return Die Ausfuehrungsdaten
	 */
	public String getOnMove() {
		return onmove;
	}

	/**
	 * Setzt die Ausfuehrungsdaten des OnMove-Ereignisses.
	 * @param onmove Die neuen Ausfuehrungsdaten
	 */
	public void setOnMove(String onmove) {
		this.onmove = onmove;
	}

	/**
	 * Gibt die Anzahl an Runden bis zu einem Respawn zurueck.
	 * @return Die Anzahl der Runden bis zu einem Respawn
	 */
	public Byte getRespawn() {
		return respawn;
	}

	/**
	 * Setzt die Anzahl an Runden bis zu einem Respawn.
	 * @param respawn Die neue Rundenanzahl
	 */
	public void setRespawn(Byte respawn) {
		this.respawn = respawn;
	}

	/**
	 * Gibt das Script des Schiffes zurueck.
	 * @return Das Script
	 */
	public String getScript() {
		return script;
	}

	/**
	 * Setzt das Script des Schiffes.
	 * @param script Das neue Script
	 */
	public void setScript(String script) {
		this.script = script;
	}

	/**
	 * Gibt die Scriptausfuehrungsdaten zurueck.
	 * @return Die Scriptausfuehrungsdaten
	 */
	public Blob getScriptExeData() {
		return scriptexedata;
	}

	/**
	 * Setzt die Scriptausfuehrungsdaten.
	 * @param scriptexedata Die neuen Ausfuehrungsdaten
	 */
	public void setScriptExeData(Blob scriptexedata) {
		this.scriptexedata = scriptexedata;
	}

	/**
	 * Gibt die Sichtbarkeitsdaten zurueck.
	 * @return Die Sichtbarkeitsdaten des Schiffes
	 */
	public Integer getVisibility() {
		return visibility;
	}

	/**
	 * Setzt die Sichtbarkeitsdaten des Schiffes.
	 * @param visibility Die neuen Sichtbarkeitsdaten
	 */
	public void setVisibility(Integer visibility) {
		this.visibility = visibility;
	}
	
	/**
	 * Gibt die Typen-Daten des Schiffs zurueck.
	 * @return die Typen-Daten
	 */
	public ShipTypeData getTypeData() {	
		ShipModules modules = this.modules;
			
		return getShipType(this.shiptype.getId(), modules, false);
	}
	
	private static final int MANGEL_TICKS = 9;
	
	/**
	 * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
	 * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
	 * @return der neue Status-String
	 */
	public String recalculateShipStatus() {
		if( this.id < 0 ) {
			throw new UnsupportedOperationException("recalculateShipStatus kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
		}
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		ShipTypeData type = this.getTypeData();
		
		Cargo cargo = this.cargo;
		
		List<String> status = new ArrayList<String>();
		
		// Alten Status lesen und ggf Elemente uebernehmen
		String[] oldstatus = StringUtils.split(this.status, ' ');
		
		if( oldstatus.length > 0 ) {
			for( int i=0; i < oldstatus.length; i++ ) {
				String astatus = oldstatus[i];
				
				// deprecated -- vorlaeufig automatisch rausfiltern
				if( astatus.equals("tblmodules") || astatus.equals("nebel") ) {
					continue;
				}
				
				// Aktuelle Statusflags rausfiltern
				if( !astatus.equals("disable_iff") && !astatus.equals("mangel_nahrung") && 
						!astatus.equals("mangel_reaktor") && !astatus.equals("offizier") && 
						!astatus.equals("nocrew") ) {
					status.add(astatus);
				}
			}
		}
		
		// Treibstoffverbrauch bereichnen
		if( type.getRm() > 0 ) {
			long ep = cargo.getResourceCount( Resources.URAN ) * type.getRu() + 
			cargo.getResourceCount( Resources.DEUTERIUM ) * type.getRd() + 
			cargo.getResourceCount( Resources.ANTIMATERIE ) * type.getRa();
			long er = ep/type.getRm();
			
			int turns = 2;
			if( (this.alarm != Alert.GREEN.getCode()) && (type.getShipClass() != ShipClasses.GESCHUETZ.ordinal()) ) {
				turns = 4;	
			}
			
			if( er <= MANGEL_TICKS/turns ) {
				status.add("mangel_reaktor");
			}
		}
		
		// Ist Crew an Bord?
		if( (type.getCrew() != 0) && (this.crew == 0) ) {
			status.add("nocrew");	
		}

		// Die Items nach IFF und Hydros durchsuchen
		boolean disableIFF = false;

		if( cargo.getItemWithEffect(ItemEffect.Type.DISABLE_IFF) != null ) {
			disableIFF = true;
		}
		
		if( disableIFF ) {
			status.add("disable_iff");
		}

		// Ist ein Offizier an Bord?
		Offizier offi = getOffizier();
		if( offi != null ) {
			status.add("offizier");
		}

		try {
			ShipModules modules = (ShipModules)db.get(ShipModules.class, this.id);
			if( modules != null ) {
				this.modules = modules;
			}
		}
		catch( ObjectNotFoundException e ) {
			this.modules = null;
		}

		if( lackOfFood() ) {
			status.add("mangel_nahrung");
		}

		this.status = Common.implode(" ", status);

		// Ueberpruefen, ob ein evt vorhandener Werftkomplex nicht exisitert
		if( type.getWerft() != 0 ) {
			ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=?")
				.setEntity(0, this)
				.uniqueResult();
			
			if( (werft != null) && (werft.getKomplex() != null) ) {
				werft.getKomplex().checkWerftLocations();
			}
			else if( werft == null ) {
				log.error("Das Schiff "+this.id+" besitzt keinen Werfteintrag");
			}
		}
		
		return this.status;
	}

	private boolean lackOfFood() {
		if( timeUntilLackOfFood() <= MANGEL_TICKS ) {
			return true;
		}

		return false;
	}

	private long timeUntilLackOfFood() {
		Cargo usercargo = new Cargo(Cargo.Type.STRING, this.owner.getCargo());
		long timeUntilLackOfFood = 0;
		int foodConsumption = getNettoFoodConsumption();

		if( foodConsumption <= 0 ) {
			return Long.MAX_VALUE;
		}

		//Den Nahrungsverbrauch berechnen
		if( isUserCargoUsable() ) {
			timeUntilLackOfFood = usercargo.getResourceCount(Resources.NAHRUNG) / foodConsumption;
		}
		else {
			//Basisschiff beruecksichtigen
			Ship baseShip = getBaseShip();
			if( baseShip != null ) {
				timeUntilLackOfFood = baseShip.timeUntilLackOfFood();
			}

			timeUntilLackOfFood += this.cargo.getResourceCount(Resources.NAHRUNG) / foodConsumption;
		}
		return timeUntilLackOfFood;
	}

	/**
	 * Calculates the amount of food a ship consumes.
	 * The calculation is done with respect to hydros.
	 * 
	 * @return Amount of food this ship consumes
	 */
	private int getFoodConsumption() {
		ShipTypeData shiptype = this.getTypeData();
		int scaledCrew = getScaledCrew();
		int production = shiptype.getHydro();
		return scaledCrew-production;
	}

	/**
	 * Calculates the amound of food the ship consumes with respect to docked ships.
	 * 
	 * @return The amount this ship consumes with respect to docked ships.
	 */
	private int getNettoFoodConsumption() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		int foodConsumption = getFoodConsumption();
		
		if( getTypeData().getADocks() > 0 || getTypeData().getJDocks() > 0 ) {
			//Angehaengte Schiffe beruecksichtigen
			List<?> dockedShips = db.createQuery("from Ship as ship where ship.docked in (?,?)")
				.setString(0, Integer.toString(this.id))
				.setString(1, "l "+this.id)
				.list();
			for( Iterator<?> iter=dockedShips.iterator(); iter.hasNext(); ) {
				Ship dockedShip = (Ship)iter.next();
					
				foodConsumption += dockedShip.getNettoFoodConsumption();
			}
		}
		return foodConsumption;
	}
	
	/**
	 * Gibt den Skalierungsfaktor fuer aktiven Alarm zurueck.
	 * 
	 * @return Skalierungsfaktor, je nach Alarmstufe.
	 */
	public double getAlertScaleFactor()
	{
		double[] factors = new double[] { 1, 0.9, 0.9 };
		
		return factors[getAlarm()];
	}

	/**
	 * Returns the crew scaled by a factor according to alert.
	 * Ships with active alert consume more food.
	 * 
	 * @return Crew scaled by a factor according to shiptype.
	 */
	private int getScaledCrew() {
		ShipTypeData type = this.getTypeData();
		double scale = getAlertScaleFactor();
		int scaledCrew = (int)Math.ceil(this.crew/scale) - type.getHydro();
		return scaledCrew;
	}

	/**
	 * Repraesentiert ein in ein Schiff eingebautes Modul (oder vielmehr die Daten, 
	 * die hinterher verwendet werden um daraus ein Modul zu rekonstruieren).
	 */
	public static class ModuleEntry {
		/**
		 * Der Slot in den das Modul eingebaut ist.
		 */
		public final int slot;
		/**
		 * Der Modultyp.
		 * @see net.driftingsouls.ds2.server.cargo.modules.Module
		 */
		public final int moduleType;
		/**
		 * Weitere Modultyp-spezifische Daten.
		 */
		public final String data;

		protected ModuleEntry(int slot, int moduleType, String data) {
			this.slot = slot;
			this.moduleType = moduleType;
			this.data = data;
		}

		@Override
		public String toString() {
			return "ModuleEntry: "+slot+":"+moduleType+":"+data;
		}
	}

	/**
	 * Gibt die Moduleintraege des Schiffes zurueck.
	 * @return Eine Liste von Moduleintraegen
	 */
	public ModuleEntry[] getModules() {
		List<Ship.ModuleEntry> result = new ArrayList<ModuleEntry>();

		if( this.modules == null ) {
			return new ModuleEntry[0];
		}

		ShipModules moduletbl = this.modules;
		if( moduletbl.getModules().length() != 0 ) {
			String[] modulelist = StringUtils.split(moduletbl.getModules(), ';');
			if( modulelist.length != 0 ) {
				for( int i=0; i < modulelist.length; i++ ) {
					String[] module = StringUtils.split(modulelist[i], ':');
					result.add(new ModuleEntry(Integer.parseInt(module[0]), Integer.parseInt(module[1]), module[2]));	
				}
			}
		}

		return result.toArray(new ModuleEntry[result.size()]);
	}

	/**
	 * Fuegt ein Modul in das Schiff ein.
	 * @param slot Der Slot, in den das Modul eingebaut werden soll
	 * @param moduleid Die Typen-ID des Modultyps
	 * @param data Weitere Daten, welche das Modul identifizieren
	 */
	public void addModule(int slot, int moduleid, String data ) {
		if( this.id < 0 ) {
			throw new UnsupportedOperationException("addModules kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		ShipModules shipModules = this.modules;
		if( shipModules == null ) {
			shipModules = new ShipModules(this);
			db.persist(shipModules);

			this.modules = shipModules;
		}

		List<ModuleEntry> moduletbl = new ArrayList<ModuleEntry>();
		moduletbl.addAll(Arrays.asList(getModules()));

		//check modules

		//rebuild
		moduletbl.add(new ModuleEntry(slot, moduleid, data ));

		ShipTypeData type = Ship.getShipType( this.shiptype.getId(), null, true );

		Map<Integer,String[]>slotlist = new HashMap<Integer,String[]>();
		String[] tmpslotlist = StringUtils.splitPreserveAllTokens(type.getTypeModules(), ';');
		for( int i=0; i < tmpslotlist.length; i++ ) {
			String[] aslot = StringUtils.splitPreserveAllTokens(tmpslotlist[i], ':');
			slotlist.put(Integer.parseInt(aslot[0]), aslot);
		}

		List<Module> moduleobjlist = new ArrayList<Module>();
		List<String> moduleSlotData = new ArrayList<String>(); 

		for( int i=0; i < moduletbl.size(); i++ ) {
			ModuleEntry module = moduletbl.get(i);
			if( module.moduleType != 0 ) {
				Module moduleobj = Modules.getShipModule( module );
				if( (module.slot > 0) && (slotlist.get(module.slot).length > 2) ) {
					moduleobj.setSlotData(slotlist.get(module.slot)[2]);
				}
				moduleobjlist.add(moduleobj);

				moduleSlotData.add(module.slot+":"+module.moduleType+":"+module.data);
			}
		}

		for( int i=0; i < moduleobjlist.size(); i++ ) {
			type = moduleobjlist.get(i).modifyStats( type, moduleobjlist );		
		}

		shipModules.setModules(Common.implode(";",moduleSlotData));
		writeTypeToShipModule(shipModules, type);
	}

	private void writeTypeToShipModule(ShipModules shipModules, ShipTypeData type) {
		shipModules.setNickname(type.getNickname());
		shipModules.setPicture(type.getPicture());
		shipModules.setRu(type.getRu());
		shipModules.setRd(type.getRd());
		shipModules.setRa(type.getRa());
		shipModules.setRm(type.getRm());
		shipModules.setEps(type.getEps());
		shipModules.setCost(type.getCost());
		shipModules.setHull(type.getHull());
		shipModules.setPanzerung(type.getPanzerung());
		shipModules.setCargo(type.getCargo());
		shipModules.setHeat(type.getHeat());
		shipModules.setCrew(type.getCrew());
		shipModules.setWeapons(type.getWeapons());
		shipModules.setMaxHeat(type.getMaxHeat());
		shipModules.setTorpedoDef(type.getTorpedoDef());
		shipModules.setShields(type.getShields());
		shipModules.setSize(type.getSize());
		shipModules.setJDocks(type.getJDocks());
		shipModules.setADocks(type.getADocks());
		shipModules.setSensorRange(type.getSensorRange());
		shipModules.setHydro(type.getHydro());
		shipModules.setDeutFactor(type.getDeutFactor());
		shipModules.setReCost(type.getReCost());
		shipModules.setFlags(type.getFlags());
		shipModules.setWerft(type.getWerft());
		shipModules.setOneWayWerft(type.getOneWayWerft());
		shipModules.setAblativeArmor(type.getAblativeArmor());
		shipModules.setSrs(type.hasSrs());
		shipModules.setPickingCost(type.getPickingCost());
		shipModules.setScanCost(type.getScanCost());
	}

	/**
	 * Entfernt ein Modul aus dem Schiff.
	 * @param slot Der Slot, aus dem das Modul entfernt werden soll
	 * @param moduleid Die Typen-ID des Modultyps
	 * @param data Weitere Daten, welche das Modul identifizieren
	 */
	public void removeModule( int slot, int moduleid, String data ) {	
		if( this.id < 0 ) {
			throw new UnsupportedOperationException("addModules kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( this.modules == null ) {
			return;
		}

		ShipModules shipModules = this.modules;

		List<ModuleEntry> moduletbl = new ArrayList<ModuleEntry>();
		moduletbl.addAll(Arrays.asList(getModules()));

		//check modules

		//rebuild	
		ShipTypeData type = Ship.getShipType( this.shiptype.getId(), null, true );

		Map<Integer,String[]>slotlist = new HashMap<Integer,String[]>();
		String[] tmpslotlist = StringUtils.split(type.getTypeModules(), ';');
		for( int i=0; i < tmpslotlist.length; i++ ) {
			String[] aslot = StringUtils.split(tmpslotlist[i], ':');
			slotlist.put(Integer.parseInt(aslot[0]), aslot);
		}

		List<Module> moduleobjlist = new ArrayList<Module>();
		List<String> moduleSlotData = new ArrayList<String>(); 

		for( int i=0; i < moduletbl.size(); i++ ) {
			ModuleEntry module = moduletbl.get(i);
			if( module.moduleType != 0 ) {
				Module moduleobj = Modules.getShipModule( module );

				if( moduleobj.isSame(slot, moduleid, data) ) {
					continue;
				}

				if( (module.slot > 0) && (slotlist.get(module.slot).length > 2) ) {
					moduleobj.setSlotData(slotlist.get(module.slot)[2]);
				}
				moduleobjlist.add(moduleobj);

				moduleSlotData.add(module.slot+":"+module.moduleType+":"+module.data);
			}
		}

		for( int i=0; i < moduleobjlist.size(); i++ ) {
			type = moduleobjlist.get(i).modifyStats( type, moduleobjlist );		
		}

		if( moduleSlotData.size() > 0 ) {
			shipModules.setModules(Common.implode(";",moduleSlotData));
			writeTypeToShipModule(shipModules, type);
		}
		else {
			db.delete(shipModules);
			
			this.modules = null;
		}
	}

	/**
	 * Berechnet die durch Module verursachten Effekte des Schiffes neu.
	 */
	public void recalculateModules() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( this.modules == null ) {
			return;
		}

		ShipModules shipModules = (ShipModules)db.get(ShipModules.class, this.id);

		List<ModuleEntry> moduletbl = new ArrayList<ModuleEntry>();
		moduletbl.addAll(Arrays.asList(getModules()));

		//check modules

		//rebuild	
		ShipTypeData type = Ship.getShipType( this.shiptype.getId(), null, true );

		Map<Integer,String[]>slotlist = new HashMap<Integer,String[]>();
		String[] tmpslotlist = StringUtils.split(type.getTypeModules(), ';');
		for( int i=0; i < tmpslotlist.length; i++ ) {
			String[] aslot = StringUtils.split(tmpslotlist[i], ':');
			slotlist.put(Integer.parseInt(aslot[0]), aslot);
		}

		List<Module> moduleobjlist = new ArrayList<Module>();
		List<String> moduleSlotData = new ArrayList<String>(); 

		for( int i=0; i < moduletbl.size(); i++ ) {
			ModuleEntry module = moduletbl.get(i);
			if( module.moduleType != 0 ) {
				Module moduleobj = Modules.getShipModule( module );

				if( (module.slot > 0) && (slotlist.get(module.slot).length > 2) ) {
					moduleobj.setSlotData(slotlist.get(module.slot)[2]);
				}
				moduleobjlist.add(moduleobj);

				moduleSlotData.add(module.slot+":"+module.moduleType+":"+module.data);
			}
		}

		for( int i=0; i < moduleobjlist.size(); i++ ) {
			type = moduleobjlist.get(i).modifyStats( type, moduleobjlist );		
		}

		shipModules.setModules(Common.implode(";",moduleSlotData));
		writeTypeToShipModule(shipModules, type);
	}

	private void handleAlert() 
	{
		User owner = this.owner;
		List<Ship> attackShips = alertCheck(owner, this.getLocation()).values().iterator().next();

		if(attackShips.isEmpty())
		{
			return;
		}
		
		for(Ship ship: attackShips)
		{
			if(ship.getBattle() != null)
			{
				org.hibernate.Session db = ContextMap.getContext().getDB();
				BattleShip bship = (BattleShip)db.get(BattleShip.class, ship.getId());
				int oside = (bship.getSide() + 1) % 2 + 1;
				Battle battle = ship.getBattle();
				battle.load(this.owner, null, null, oside);

				int docked = ((Number)db.createQuery("select count(*) from Ship where docked=?")
						.setString(0, Integer.toString(this.id))
						.iterate().next()).intValue();

				if(docked != 0) 
				{
					List<Ship> dlist = Common.cast(db.createQuery("from Ship where docked=?")
													 .setString(0, Integer.toString(this.id))
													 .list());
					
					for(Ship aship: dlist) 
					{
						battle.addShip(this.owner.getId(), aship.getId());
					}
				}
				battle.addShip(this.owner.getId(), this.id);

				if( battle.getEnemyLog(true).length() != 0 ) 
				{
					battle.writeLog();
				}
				
				return;
			}
		}
		
		Ship ship = attackShips.get(0); //Take some ship .. no special mechanism here.
		Battle.create(ship.getOwner().getId(), ship.getId(), this.id, true);
	}

	/**
	 * Gibt eine Liste von booleans zurueck, welche angeben ob ein angegebener Sektor fuer den angegebenen Spieler
	 * unter Alarm steht, d.h. bei einem Einflug eine Schlacht gestartet wird.
	 * Die Reihenfolge der Liste entspricht der der uebergebenen Koordinaten. <code>true</code> kennzeichnet,
	 * dass der Sektor unter Alarm steht.
	 * @param userid Die Spieler-ID
	 * @param locs Die Positionen, die ueberprueft werden sollen
	 * @return Liste von Booleans in der Reihenfolge der Koordinaten.
	 */
	public static boolean[] getAlertStatus( int userid, Location ... locs ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		boolean[] results = new boolean[locs.length];
		
		User user = (User)db.get(User.class, userid);

		Map<Location,List<Ship>> result = alertCheck(user, locs);
		
		for( int i=0; i < locs.length; i++ ) {
			results[i] = !result.get(locs[i]).isEmpty();
		}
		return results;
	}

	private static Map<Location,List<Ship>> alertCheck( User user, Location ... locs ) 
	{
		Map<Location,List<Ship>> results = new HashMap<Location,List<Ship>>();
		Set<Location> locations = new HashSet<Location>();
		for(Location location: locs)
		{
			results.put(location, new ArrayList<Ship>());
			locations.add(location);
		}
		
		if(locations.isEmpty())
		{
			return results;
		}
		
		org.hibernate.Session db = ContextMap.getContext().getDB();

		
		List<Ship> ships = Common.cast(db.createQuery("from Ship s inner join fetch s.owner where s.e > 0 and s.alarm!=:green and s.lock is null and s.docked='' and s.crew!=0 and s.system=:system and s.owner!=:owner")
							 			 .setParameter("green", Alert.GREEN.getCode())
							 			 .setParameter("system", locs[0].getSystem())
							 			 .setParameter("owner", user)
							 			 .list());
		
		User.Relations relationlist = user.getRelations();	
		for(Ship ship: ships)
		{
			Location location = ship.getLocation();
			if(!locations.contains(location))
			{
				continue;
			}
			
			User owner = ship.getOwner();
			int alert = ship.getAlarm();
			boolean attack = false;
			if(alert == Alert.RED.getCode())
			{
				if(relationlist.fromOther.get(owner.getId()) != User.Relation.FRIEND)
				{
					attack = true;
				}
				else if(relationlist.toOther.get(owner.getId()) != User.Relation.FRIEND)
				{
					attack = true;
				}
			}
			else if(alert == Alert.YELLOW.getCode())
			{
				if(relationlist.fromOther.get(owner.getId()) == User.Relation.ENEMY)
				{
					attack = true;
				}
			}
			
			if(attack)
			{
				results.get(location).add(ship);
			}
		}

		return results;
	}
	
	/**
	 * Die verschiedenen Zustaende, die zum Ende eines Fluges gefuehrt haben koennen.
	 */
	public static enum MovementStatus {
		/**
		 * Der Flug war Erfolgreich.
		 */
		SUCCESS,
		/**
		 * Der Flug wurde an einem EMP-Nebel abgebrochen.
		 */
		BLOCKED_BY_EMP,
		/**
		 * Der Flug wurde vor einem Feld mit rotem Alarm abgebrochen.
		 */
		BLOCKED_BY_ALERT,
		/**
		 * Das Schiff konnte nicht mehr weiterfliegen.
		 */
		SHIP_FAILURE
	}
	
	public static enum Alert
	{
		GREEN(0), YELLOW(1), RED(2);
		
		private Alert(int code)
		{
			this.code = code;
		}
		
		public int getCode()
		{
			return this.code;
		}
		
		private final int code;
	}

	private static class MovementResult {
		int distance;
		boolean moved;
		MovementStatus status;

		MovementResult(int distance, boolean moved, MovementStatus status) {
			this.distance = distance;
			this.moved = moved;
			this.status = status;
		}
	}

	private static MovementResult moveSingle(Ship ship, ShipTypeData shiptype, Offizier offizier, int direction, int distance, int adocked, boolean forceLowHeat, boolean verbose) {
		boolean moved = false;
		MovementStatus status = MovementStatus.SUCCESS;
		boolean firstOutput = true;

		StringBuilder out = MESSAGE.get();

		if( ship.getEngine() <= 0 ) {
			if(verbose) {
				out.append("<span style=\"color:#ff0000\">Antrieb defekt</span><br />\n");
			}
			distance = 0;

			return new MovementResult(distance, moved, MovementStatus.SHIP_FAILURE);
		}

		int newe = ship.getEnergy() - shiptype.getCost();
		int news = ship.getHeat() + shiptype.getHeat();

		newe -= adocked;
		if( shiptype.getMinCrew() > ship.getCrew() ) {
			newe--;
			if(verbose) {
				out.append("<span style=\"color:red\">Geringe Besatzung erh&ouml;ht Flugkosten</span><br />\n");
			}
		}

		// Antrieb teilweise beschaedigt?
		if( ship.getEngine() < 20 ) {
			newe -= 4;
		} 
		else if( ship.getEngine() < 40 ) {
			newe -= 2;
		} 
		else if( ship.getEngine() < 60 ) { 
			newe -= 1;
		}

		if( newe < 0 ) {
			if(!verbose && firstOutput)
			{
				out.append(ship.getName()+" ("+ship.getId()+"): ");
				firstOutput = false;
			}
			out.append("<span style=\"color:#ff0000\">Keine Energie. Stoppe bei "+Ships.getLocationText(ship.getLocation(), true)+"</span><br />\n");
			distance = 0;

			return new MovementResult(distance, moved, MovementStatus.SHIP_FAILURE);
		}

		if( offizier != null ) {			
			// Flugkosten
			int success = offizier.useAbility( Offizier.Ability.NAV, 200 );
			if( success > 0 ) {
				newe += 2;
				if( newe > ship.getEnergy()-1 ) {
					newe = ship.getEnergy() - 1;
				}
				if(verbose) {
					out.append(offizier.getName()+" verringert Flugkosten<br />\n");
				}
			}
			// Ueberhitzung
			success = offizier.useAbility( Offizier.Ability.ING, 200 );
			if( success > 0 ) {
				news -= 1;
				if( news < ship.getHeat() ) {
					news = ship.getHeat();
				}
				if( verbose ) {
					out.append(offizier.getName()+" verringert &Uuml;berhitzung<br />\n");
				}
			}
			if( verbose ) {
				out.append(StringUtils.replace(offizier.MESSAGE.getMessage(),"\n", "<br />"));
			}
		}

		// Grillen wir uns bei dem Flug eventuell den Antrieb?
		if( news > 100 )  {
			if(forceLowHeat && distance > 0) {
				if( !verbose && firstOutput ) {
					out.append(ship.getName()+" ("+ship.getId()+"): ");
					firstOutput = false;
				}
				out.append("<span style=\"color:#ff0000\">Triebwerk w&uuml;rde &uuml;berhitzen</span><br />\n");

				out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei "+Ships.getLocationText(ship.getLocation(),true)+"</span><br />\n");
				out.append("</span></td></tr>\n");
				distance = 0;
				return new MovementResult(distance, moved, MovementStatus.SHIP_FAILURE);
			}
		}

		int x = ship.getX();
		int y = ship.getY();

		if( direction == 1 ) { x--; y--; }
		else if( direction == 2 ) { y--; }
		else if( direction == 3 ) { x++; y--; }
		else if( direction == 4 ) { x--; }
		else if( direction == 6 ) { x++; }
		else if( direction == 7 ) { x--; y++; }
		else if( direction == 8 ) { y++; }
		else if( direction == 9 ) { x++; y++; }

		StarSystem sys = Systems.get().system(ship.getSystem());

		if( x > sys.getWidth()) { 
			x = sys.getWidth();
			distance = 0;
		}
		if( y > sys.getHeight()) { 
			y = sys.getHeight();
			distance = 0;
		}
		if( x < 1 ) {
			x = 1;
			distance = 0;
		}
		if( y < 1 ) {
			y = 1;
			distance = 0;
		}

		if( (ship.getX() != x) || (ship.getY() != y) ) {
			moved = true;

			if( ship.getHeat() >= 100 ) {
				if( !verbose && firstOutput) {
					out.append(ship.getName()+" ("+ship.getId()+"): ");
					firstOutput = false;
				}
				out.append("<span style=\"color:#ff0000\">Triebwerke &uuml;berhitzt</span><br />\n");

				if( (RandomUtils.nextInt(101)) < 3*(news-100) ) {
					int dmg = (int)( (2*(RandomUtils.nextInt(101)/100d)) + 1 ) * (news-100);
					out.append("<span style=\"color:#ff0000\">Triebwerke nehmen "+dmg+" Schaden</span><br />\n");
					ship.setEngine(ship.getEngine()-dmg);
					if( ship.getEngine() < 0 ) {
						ship.setEngine(0);
					}
					if( distance > 0 ) {
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei "+Ships.getLocationText(ship.getLocation(),true)+"</span><br />\n");
						status = MovementStatus.SHIP_FAILURE;
						distance = 0;
					}
				}
			}

			ship.setX(x);
			ship.setY(y);
			ship.setEnergy(newe);
			ship.setHeat(news);
			if( verbose ) {
				out.append(ship.getName()+" fliegt in "+Ships.getLocationText(ship.getLocation(),true)+" ein<br />\n");
			}
		}

		return new MovementResult(distance, moved, status);
	}

	/**
	 * Enthaelt die Daten der Schiffe in einer Flotte, welche sich gerade bewegt.
	 *
	 */
	private static class FleetMovementData {
		FleetMovementData() {
			// EMPTY
		}

		/**
		 * Die Schiffe in der Flotte.
		 */
		Map<Integer,Ship> ships = new HashMap<Integer,Ship>();
		/**
		 * Die Offiziere auf den Schiffen der Flotte.
		 */
		Map<Integer,Offizier> offiziere = new HashMap<Integer,Offizier>();
		/**
		 * Die Anzahl der gedockten/gelandeten Schiffe.
		 */
		Map<Integer,Integer> dockedCount = new HashMap<Integer,Integer>();
		/**
		 * Die Anzahl der extern gedocketen Schiffe.
		 */
		Map<Integer,Integer> aDockedCount = new HashMap<Integer,Integer>();
	}


	private boolean initFleetData(boolean verbose) {
		Context context = ContextMap.getContext();
		boolean error = false;
		boolean firstEntry = true;
		StringBuilder out = MESSAGE.get();

		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");
		if( fleetdata != null ) {
			return false;
		}

		fleetdata = new FleetMovementData();

		context.putVariable(Ships.class, "fleetdata", fleetdata);

		org.hibernate.Session db = context.getDB();

		List<?> fleetships = db.createQuery("from Ship " +
				"where id>0 and fleet=? and x=? and y=? and system=? and owner=? and " +
				"docked='' and id!=? and e>0 and battle is null")
			.setEntity(0, this.fleet)
			.setInteger(1, this.x)
			.setInteger(2, this.y)
			.setInteger(3, this.system)
			.setEntity(4, this.owner)
			.setInteger(5, this.id)
			.list();

		for( Iterator<?> iter=fleetships.iterator(); iter.hasNext(); ) {
			if( verbose && firstEntry ) {
				firstEntry = false;
				out.append("<table class=\"noBorder\">\n");
			}
			Ship fleetship = (Ship)iter.next();
			ShipTypeData shiptype = fleetship.getTypeData();

			StringBuilder outpb = new StringBuilder();

			if( (fleetship.getLock() != null) && (fleetship.getLock().length() != 0) ) {
				outpb.append("<span style=\"color:red\">Fehler: Das Schiff ist an ein Quest gebunden</span>\n");
				outpb.append("</span></td></tr>\n");
				error = true;
			}

			if( shiptype.getCost() == 0 ) {
				outpb.append("<span style=\"color:red\">Das Objekt kann nicht fliegen, da es keinen Antieb hat</span><br />");
				error = true;
			}

			if( (fleetship.getCrew() == 0) && (shiptype.getCrew() > 0) ) {
				outpb.append("<span style=\"color:red\">Fehler: Sie haben keine Crew auf dem Schiff</span><br />");
				error = true;
			}

			if( outpb.length() != 0 ) {
				out.append("<tr>\n");
				out.append("<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange; font-size:12px\"> "+fleetship.getName()+" ("+fleetship.getId()+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n");
				out.append(outpb);
				out.append("</span></td></tr>\n");
			}
			else {
				int dockedcount = 0;
				int adockedcount = 0;
				if( (shiptype.getJDocks() > 0) || (shiptype.getADocks() > 0) ) { 
					int docks = ((Number)db.createQuery("select count(*) from Ship where id>0 and docked in (?,?)")
							.setString(0, "l "+fleetship.getId())
							.setString(1, Integer.toString(fleetship.getId()))
							.iterate().next()).intValue();

					dockedcount = docks;
					if( shiptype.getADocks() > 0 ) {
						adockedcount = (int)fleetship.getDockedCount();	
					} 
				}

				if( fleetship.getStatus().indexOf("offizier") > -1 ) {
					fleetdata.offiziere.put(fleetship.getId(), fleetship.getOffizier());
				}

				fleetdata.dockedCount.put(fleetship.getId(), dockedcount);
				fleetdata.aDockedCount.put(fleetship.getId(), adockedcount);

				fleetdata.ships.put(fleetship.getId(), fleetship);
			}
		}

		return error;
	}

	private MovementStatus moveFleet(int direction, boolean forceLowHeat, boolean verbose)  {
		StringBuilder out = MESSAGE.get();
		MovementStatus status = MovementStatus.SUCCESS;

		boolean firstEntry = true;
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");

		for( Ship fleetship : fleetdata.ships.values() ) {
			if( verbose && firstEntry ) {
				firstEntry = false;
				out.append("<table class=\"noBorder\">\n");
			}

			if(verbose) {
				out.append("<tr>\n");
				out.append("<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange; font-size:12px\"> "+fleetship.getName()+" ("+fleetship.getId()+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n");
			}	
			Offizier offizierf = fleetdata.offiziere.get(fleetship.getId());

			ShipTypeData shiptype = fleetship.getTypeData();

			MovementResult result = moveSingle(fleetship, shiptype, offizierf, direction, 1, fleetdata.aDockedCount.get(fleetship.getId()), forceLowHeat, verbose);

			//Einen einmal gesetzten Fehlerstatus nicht wieder aufheben
			if( status == MovementStatus.SUCCESS ) {
				status = result.status;
			}

			if(verbose) {
				out.append("</span></td></tr>\n");
			}
		}

		if( !firstEntry )
		{
			out.append("</table>\n");
		}

		return status;
	}

	private static void saveFleetShips() {	
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");

		if( fleetdata != null ) {
			org.hibernate.Session db = context.getDB();

			for( Ship fleetship : fleetdata.ships.values() ) {
				if( fleetdata.dockedCount.get(fleetship.getId()) > 0 ) {
					List<?> dockedList = db.createQuery("from Ship where id>0 and docked in (?,?)")
						.setString(0, "l "+fleetship.getId())
						.setString(1, Integer.toString(fleetship.getId()))
						.list();
					for( Iterator<?> iter=dockedList.iterator(); iter.hasNext(); ) {
						Ship dockedShip = (Ship)iter.next();
						dockedShip.setSystem(fleetship.getSystem());
						dockedShip.setX(fleetship.x);
						dockedShip.setY(fleetship.y);
						dockedShip.recalculateShipStatus();
					}
				}

				fleetship.recalculateShipStatus();
			}
		}
		context.putVariable(Ships.class, "fleetships", null);
		context.putVariable(Ships.class, "fleetoffiziere", null);
	}
	
	/**
	 * <p>Fliegt eine Flugroute entlang. Falls das Schiff einer Flotte angehoert, fliegt
	 * diese ebenfalls n Felder in diese Richtung.</p>
	 * <p>Der Flug wird abgebrochen sobald eines der Schiffe nicht mehr weiterfliegen kann</p>
	 * Die Flugrouteninformationen werden waehrend des Fluges modifiziert.
	 * 
	 * @param route Die Flugroute
	 * @param forceLowHeat Soll bei Ueberhitzung sofort abgebrochen werden?
	 * @param disableQuests Sollen Questhandler ignoriert werden?
	 * @return Der Status, der zum Ende des Fluges gefuehrt hat
	 */
	public MovementStatus move(List<Waypoint> route, boolean forceLowHeat, boolean disableQuests) {
		StringBuilder out = MESSAGE.get();

		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( (this.lock != null) && (this.lock.length() != 0) ) {
			out.append("Fehler: Das Schiff ist an ein Quest gebunden\n");
			return MovementStatus.SHIP_FAILURE;
		}

		User user = this.owner;

		ShipTypeData shiptype = this.getTypeData();
		Offizier offizier = getOffizier();

		//Das Schiff soll sich offenbar bewegen
		if( this.docked.length() != 0 ) {
			out.append("Fehler: Sie k&ouml;nnen nicht mit dem Schiff fliegen, da es geladet/angedockt ist\n");
			return MovementStatus.SHIP_FAILURE;
		}

		if( shiptype.getCost() == 0 ) {
			out.append("Fehler: Das Objekt kann nicht fliegen, da es keinen Antieb hat\n");
			return MovementStatus.SHIP_FAILURE;
		}

		if( this.battle != null ) {
			out.append("Fehler: Das Schiff ist in einen Kampf verwickelt\n");
			return MovementStatus.SHIP_FAILURE;
		}

		if( (this.crew <= 0) && (shiptype.getCrew() > 0) ) {
			out.append("<span style=\"color:#ff0000\">Das Schiff verf&uuml;gt &uuml;ber keine Crew</span><br />\n");
			return MovementStatus.SHIP_FAILURE;
		}

		int docked = 0;
		int adocked = 0;
		MovementStatus status = MovementStatus.SUCCESS;

		if( (shiptype.getJDocks() > 0) || (shiptype.getADocks() > 0) ) {
			docked = ((Number)db.createQuery("select count(*) from Ship where id>0 and docked in (?,?)")
					.setString(0, "l "+this.id)
					.setString(1, Integer.toString(this.id))
					.iterate().next()).intValue();

			if( shiptype.getADocks() > 0 ) {
				adocked = (int)getDockedCount();
			}
		}

		boolean moved = false;

		while( (status == MovementStatus.SUCCESS) && route.size() > 0 ) {
			Waypoint waypoint = route.remove(0);

			if( waypoint.type != Waypoint.Type.MOVEMENT ) {
				throw new RuntimeException("Es wird nur "+Waypoint.Type.MOVEMENT+" als Wegpunkt unterstuetzt");
			}

			if( waypoint.direction == 5 ) {
				continue;
			}

			// Zielkoordinaten/Bewegungsrichtung berechnen
			int xoffset = 0;
			int yoffset = 0;
			if( waypoint.direction <= 3 ) {
				yoffset--;
			}
			else if( waypoint.direction >= 7 ) {
				yoffset++;
			}

			if( (waypoint.direction-1) % 3 == 0 ) {
				xoffset--;
			}
			else if( waypoint.direction % 3 == 0 ) {
				xoffset++;
			}

			// Alle potentiell relevanten Sektoren (ok..und ein wenig ueberfluessiges Zeug bei schraegen Bewegungen) auslesen
			Map<Location,Sector> sectorlist = new HashMap<Location,Sector>();
			List<?> sectors = db.createQuery("from Sector " +
					"where system in (:system,-1) and " +
						"(x=-1 or x between :lowerx and :upperx) and " +
						"(y=-1 or y between :lowery and :uppery) order by system desc")
					.setInteger("system", this.system)
					.setInteger("lowerx", (waypoint.direction-1) % 3 == 0 ? this.x-waypoint.distance : this.x )
					.setInteger("upperx", (waypoint.direction) % 3 == 0 ? this.x+waypoint.distance : this.x )
					.setInteger("lowery", waypoint.direction <= 3 ? this.y-waypoint.distance : this.y )
					.setInteger("uppery", waypoint.direction >= 7 ? this.y+waypoint.distance : this.y )
					.list();

			for( Iterator<?> iter=sectors.iterator(); iter.hasNext(); ) {
				Sector sector = (Sector)iter.next();
				sectorlist.put(sector.getLocation(), sector);
			}
			
			List<Ship> sectorList = Common.cast(db.createQuery("from Ship " +
			"where owner!=:owner and system=:system and x between :lowerx and :upperx and y between :lowery and :uppery")
			.setEntity("owner", this.owner)
			.setInteger("system", this.system)
			.setInteger("lowerx", (waypoint.direction-1) % 3 == 0 ? this.x-waypoint.distance : this.x )
			.setInteger("upperx", (waypoint.direction) % 3 == 0 ? this.x+waypoint.distance : this.x )
			.setInteger("lowery", waypoint.direction <= 3 ? this.y-waypoint.distance : this.y )
			.setInteger("uppery", waypoint.direction >= 7 ? this.y+waypoint.distance : this.y )
			.list());
			
			List<Location> locations = new ArrayList<Location>();
			for(Ship ship: sectorList)
			{
				locations.add(ship.getLocation());
			}
			
			Map<Location, List<Ship>> alertList = alertCheck(owner, locations.toArray(new Location[0]));

			// Alle potentiell relevanten Sektoren mit EMP-Nebeln (ok..und ein wenig ueberfluessiges Zeug bei schraegen Bewegungen) auslesen
			Map<Location,Boolean> nebulaemplist = new HashMap<Location,Boolean>();
			sectorList = db.createQuery("from Nebel " +
					"where type>=3 and type<=5 and system=:system and x between :lowerx and :upperx and y between :lowery and :uppery")
					.setInteger("system", this.system)
					.setInteger("lowerx", (waypoint.direction-1) % 3 == 0 ? this.x-waypoint.distance : this.x )
					.setInteger("upperx", (waypoint.direction) % 3 == 0 ? this.x+waypoint.distance : this.x )
					.setInteger("lowery", waypoint.direction <= 3 ? this.y-waypoint.distance : this.y )
					.setInteger("uppery", waypoint.direction >= 7 ? this.y+waypoint.distance : this.y )
					.list();

			for( Iterator<?> iter=sectorList.iterator(); iter.hasNext(); ) {
				Nebel nebel = (Nebel)iter.next();
				nebulaemplist.put(nebel.getLocation(), Boolean.TRUE);
			}

			if( (waypoint.distance > 1) && nebulaemplist.containsKey(this.getLocation()) ) {
				out.append("<span style=\"color:#ff0000\">Der Autopilot funktioniert in EMP-Nebeln nicht</span><br />\n");
				return MovementStatus.BLOCKED_BY_EMP;
			}

			if( this.fleet != null ) {
				initFleetData(false);
			}

			long starttime = System.currentTimeMillis();

			int startdistance = waypoint.distance;

			// Und nun fliegen wir mal ne Runde....
			while( waypoint.distance > 0 ) {
				final Location nextLocation = new Location(this.system,this.x+xoffset, this.y+yoffset);
												
				if(alertList.containsKey(nextLocation) && user.isNoob()){
					List<Ship> attackers = alertCheck(user, nextLocation)
						.values().iterator().next();
					if( !attackers.isEmpty() ) {
						out.append("<span style=\"color:#ff0000\">Sie stehen unter dem Schutz des Commonwealth.</span><br />Ihnen ist der Einflug in gesicherte Gebiete untersagt<br />\n");
						status = MovementStatus.BLOCKED_BY_ALERT;
						waypoint.distance = 0;
						break;
					}
				}
				
				// Schauen wir mal ob wir vor Alarm warnen muessen
				if( (startdistance > 1) && alertList.containsKey(nextLocation) ) {
					List<Ship> attackers = alertCheck(user, nextLocation)
						.values().iterator().next();
					if( !attackers.isEmpty() ) {
						out.append("<span style=\"color:#ff0000\">Feindliche Schiffe in Alarmbereitschaft im n&auml;chsten Sektor geortet</span><br />\n");
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
						status = MovementStatus.BLOCKED_BY_ALERT;
						waypoint.distance = 0;
						break;
					}
				}
				
				if( (startdistance > 1) && nebulaemplist.containsKey(nextLocation) ) {
					out.append("<span style=\"color:#ff0000\">EMP-Nebel im n&auml;chsten Sektor geortet</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					status = MovementStatus.BLOCKED_BY_EMP;
					waypoint.distance = 0;
					break;
				}

				int olddirection = waypoint.direction;

				// ACHTUNG: Ob das ganze hier noch sinnvoll funktioniert, wenn distance > 1 ist, ist mehr als fraglich...
				if( nebulaemplist.containsKey(nextLocation) && 
						(RandomUtils.nextDouble() < getTypeData().getLostInEmpChance()) ) {
					int nebel = Ships.getNebula(getLocation());
					if( nebel == 5 ) {
						waypoint.direction = RandomUtils.nextInt(10)+1;
						if( waypoint.direction == 5 ) {
							waypoint.direction++;
						}
						// Nun muessen wir noch die Caches fuellen
						if( waypoint.direction != olddirection ) {
							int tmpxoff = 0;
							int tmpyoff = 0;

							if( waypoint.direction <= 3 ) {
								tmpyoff--;
							}
							else if( waypoint.direction >= 7 ) {
								tmpyoff++;
							}

							if( (waypoint.direction-1) % 3 == 0 ) {
								tmpxoff--;
							}
							else if( waypoint.direction % 3 == 0 ) {
								tmpxoff++;
							}

							sectors = db.createQuery("from Sector "+
									"where system in (?,-1) AND x in (-1,?) and y in (-1,?) order by system desc")
									.setInteger(0, this.system)
									.setInteger(1, this.x+tmpxoff)
									.setInteger(2, this.y+tmpyoff)
									.list();
							for( Iterator<?> iter=sectors.iterator(); iter.hasNext(); ) {
								Sector sector = (Sector)iter.next();
								sectorlist.put(sector.getLocation(), sector);
							}
							
							alertList.putAll(alertCheck(owner, new Location[] { new Location(this.system, this.x+tmpxoff, this.y+tmpyoff) }));
						}
					}
				}

				waypoint.distance--;

				MovementResult result = moveSingle(this, shiptype, offizier, waypoint.direction, waypoint.distance, adocked, forceLowHeat, false);
				status = result.status;
				waypoint.distance = result.distance;

				if( result.moved ) {
					// Jetzt, da sich unser Schiff korrekt bewegt hat, fliegen wir auch die Flotte ein stueck weiter	
					if( this.fleet != null ) {
						MovementStatus fleetResult = moveFleet(waypoint.direction, forceLowHeat, false);
						if( fleetResult != MovementStatus.SUCCESS  ) {
							status = fleetResult;
							waypoint.distance = 0;
						}
					}

					moved = true;
					if( !disableQuests && (sectorlist.size() != 0) ) {
						// Schauen wir mal, ob es ein onenter-ereigniss gab
						Location loc = this.getLocation();

						Sector sector = sectorlist.get(new Location(loc.getSystem(), -1, -1));
						if( sectorlist.containsKey(loc) ) {
							sector = sectorlist.get(loc);
						}
						else if( sectorlist.containsKey(loc.setX(-1)) ) { 
							sector = sectorlist.get(loc.setX(-1));
						}
						else if( sectorlist.containsKey(loc.setY(-1)) ) { 
							sector = sectorlist.get(loc.setY(-1));
						}

						if( (sector != null) && sector.getOnEnter().length() > 0 ) {
							this.docked = "";
							if( docked != 0 ) {
								db.createQuery("update Ship set x=? ,y=?, system=? where id>0 and docked in (?,?)")
									.setInteger(0, this.x)
									.setInteger(1, this.y)
									.setInteger(2, this.system)
									.setString(3, "l "+this.id)
									.setString(4, Integer.toString(this.id))
									.executeUpdate();
							}
							this.recalculateShipStatus();
							saveFleetShips();

							ScriptEngine scriptparser = ContextMap.getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
							final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
							
							engineBindings.put("_SHIP", this);
							if( !user.hasFlag(User.FLAG_SCRIPT_DEBUGGING) ) {
								scriptparser.getContext().setErrorWriter(new NullLogger());
							}

							engineBindings.put("SECTOR", loc.toString() );

							Quests.currentEventURL.set("&action=onenter");

							db.refresh(this);

							if( docked != 0 ) {
								db.createQuery("update Ship set x=? ,y=?, system=? where id>0 and docked in (?,?)")
									.setInteger(0, this.x)
									.setInteger(1, this.y)
									.setInteger(2, this.system)
									.setString(3, "l "+this.id)
									.setString(4, Integer.toString(this.id))
									.executeUpdate();
							}

							if( Quests.executeEvent(scriptparser, sector.getOnEnter(), this.owner, "", false ) ) {
								if( scriptparser.getContext().getWriter().toString().length()!= 0 ) {							
									waypoint.distance = 0;
								}
							}
						}
					}

					if( alertList.containsKey(this.getLocation()) ) {
						this.docked = "";
						if( docked != 0 ) {
							db.createQuery("update Ship set x=? ,y=?, system=? where id>0 and docked in (?,?)")
								.setInteger(0, this.x)
								.setInteger(1, this.y)
								.setInteger(2, this.system)
								.setString(3, "l "+this.id)
								.setString(4, Integer.toString(this.id))
								.executeUpdate();
						}
						this.recalculateShipStatus();
						saveFleetShips();


						this.handleAlert();	
					}
				}

				// Wenn wir laenger als 25 Sekunden fuers fliegen gebraucht haben -> abbrechen!
				if( System.currentTimeMillis() - starttime > 25000 ) {
					out.append("<span style=\"color:#ff0000\">Flug dauert zu lange</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					waypoint.distance = 0;
					status = MovementStatus.SHIP_FAILURE;
				}
			}  // while distance > 0

		} // while !error && route.size() > 0

		if( moved ) {
			out.append("Ankunft bei "+Ships.getLocationText(this.getLocation(),true)+"<br /><br />\n");

			this.docked = "";
			if( docked != 0 ) {
				List<?> dockedList = db.createQuery("from Ship where id>0 and docked in (?,?)")
					.setString(0, "l "+this.id)
					.setString(1, Integer.toString(this.id))
					.list();
				for( Iterator<?> iter=dockedList.iterator(); iter.hasNext(); ) {
					Ship dockedShip = (Ship)iter.next();
					dockedShip.setSystem(this.system);
					dockedShip.setX(this.x);
					dockedShip.setY(this.y);
					dockedShip.recalculateShipStatus();
				}
			}
		}
		this.recalculateShipStatus();
		saveFleetShips();

		return status;
	}

	/**
	 * <p>Laesst das Schiff durch einen Sprungpunkt springen.
	 * Der Sprungpunkt kann entweder ein normaler Sprungpunkt
	 * oder ein "Knossos"-Sprungpunkt (als ein mit einem Schiff verbundener
	 * Sprungpunkt) sein.</p>
	 * <p>Bei letzterem kann der Sprung scheitern, wenn keine Sprungberechtigung
	 * vorliegt.</p>
	 * 
	 * @param nodeID Die ID des Sprungpunkts/Des Schiffes mit dem Sprungpunkt
	 * @param knode <code>true</code>, falls es sich um einen "Knossos"-Sprungpunkt handelt
	 * @return <code>true</code>, falls ein Fehler aufgetreten ist
	 */
	public boolean jump(int nodeID, boolean knode) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		StringBuilder outputbuffer = MESSAGE.get();

		String nodetypename = "";
		String nodetarget = "";

		User user = this.owner;

		//
		// Daten der Sprungpunkte laden
		//

		Location nodeLoc = null;
		Location outLoc = null;
		Object nodeObj = null;

		if( !knode ) {
			JumpNode node = (JumpNode)db.get(JumpNode.class, nodeID);
			nodetypename = "Der Sprungpunkt";

			if( node == null ) {
				outputbuffer.append("Fehler: Der angegebene Sprungpunkt existiert nicht<br />\n");
				return true;
			}

			nodetarget = node.getName()+" ("+node.getSystemOut()+")";

			if( (user.getId() > 0) && node.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) && !user.hasFlag(User.FLAG_NO_JUMPNODE_BLOCK) ) {
				outputbuffer.append("<span style=\"color:red\">Die GCP hat diesen Sprungpunkt f&uuml;r Kolonisten gesperrt</span><br />\n");
				return true;
			}

			nodeLoc = node.getLocation();
			outLoc = new Location(node.getSystemOut(), node.getXOut(), node.getYOut());

			nodeObj = node;
		}
		else {
			/* Behandlung Knossosportale:
			 *
			 * Ziel wird mit ships.jumptarget festgelegt - Format: art|koords/id|user/ally/gruppe
			 * Beispiele: 
			 * fix|2:35/35|all:
			 * ship|id:10000|ally:1
			 * base|id:255|group:-15,455,1200
			 * fix|8:20/100|default <--- diese Einstellung entspricht der bisherigen Praxis
			 */
			nodetypename = "Knossosportal";

			Ship shipNode = (Ship)db.get(Ship.class, nodeID);
			if( shipNode == null ) {
				outputbuffer.append("Fehler: Der angegebene Sprungpunkt existiert nicht<br />\n");
				return true;
			}

			nodetypename = shipNode.getTypeData().getNickname();

			/* 
			 * Ermittlung der Zielkoordinaten
			 * geprueft wird bei Schiffen und Basen das Vorhandensein der Gegenstation
			 * existiert keine, findet kein Sprung statt
			 */

			String[] target = StringUtils.split(shipNode.getJumpTarget(), '|');
			if( target[0].equals("fix") ) {
				outLoc = Location.fromString(target[1]);

				nodetarget = target[1];
			} 
			else if( target[0].equals("ship") ) {
				String[] shiptarget = StringUtils.split(target[1], ':');
				Ship jmptarget = (Ship)db.get(Ship.class, Integer.valueOf(shiptarget[1]));
				if( jmptarget == null ) {
					outputbuffer.append("<span style=\"color:red\">Die Empfangsstation existiert nicht!</span><br />\n");
					return true;
				}

				outLoc = new Location(jmptarget.getSystem(), jmptarget.getX(), jmptarget.getY());
				nodetarget = outLoc.toString();
			}	
			else if( target[0].equals("base") ) {
				String[] shiptarget = StringUtils.split(target[1], ':');
				Base jmptarget = (Base)db.get(Base.class, Integer.valueOf(shiptarget[1]));
				if( jmptarget == null ) {
					outputbuffer.append("<span style=\"color:red\">Die Empfangsbasis existiert nicht!</span><br />\n");
					return true;
				}

				outLoc = jmptarget.getLocation();
				nodetarget = outLoc.toString();
			}

			// Einmalig das aktuelle Schiff ueberpruefen. 
			// Evt vorhandene Schiffe in einer Flotte werden spaeter separat gecheckt
			if( shipNode.getId() == this.id ) {
				outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen nicht mit dem "+nodetypename+" durch sich selbst springen</span><br />\n");
				return true;
			}

			/* 
			 * Ermittlung der Sprungberechtigten
			 */
			String[] jmpnodeuser = StringUtils.split(target[2], ':'); // Format art:ids aufgespalten

			if( jmpnodeuser[0].equals("all") ) {
				// Keine Einschraenkungen
			}
			// die alte variante 
			else if( jmpnodeuser[0].equals("default") || jmpnodeuser[0].equals("ownally") ){
				if( ( (user.getAlly() != null) && (shipNode.getOwner().getAlly() != user.getAlly()) ) || 
						( user.getAlly() == null && (shipNode.getOwner() != user) ) ) {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - default</span><br />\n");
					return true;
				}
			}
			// user:$userid
			else if ( jmpnodeuser[0].equals("user") ){
				if( Integer.parseInt(jmpnodeuser[1]) != user.getId() )  {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - owner</span><br />\n");
					return true;
				}
			}
			// ally:$allyid
			else if ( jmpnodeuser[0].equals("ally") ){
				if( (user.getAlly() == null) || (Integer.parseInt(jmpnodeuser[1]) != user.getAlly().getId()) )  {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - ally</span><br />\n");
					return true;
				}
			}
			// group:userid1,userid2, ...,useridn
			else if ( jmpnodeuser[0].equals("group") ){
				Integer[] userlist = Common.explodeToInteger(",", jmpnodeuser[1]);
				if( !Common.inArray(user.getId(), userlist) )  {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - group</span><br />\n");
					return true;
				}
			}

			nodeLoc = new Location(shipNode.getSystem(), shipNode.getX(), shipNode.getY());
		}

		Location shipLoc = new Location(this.system, this.x, this.y);

		if( !shipLoc.sameSector(0, nodeLoc, 0) ) {
			outputbuffer.append("<span style=\"color:red\">Fehler: "+nodetypename+" befindet sich nicht im selben Sektor wie das Schiff</span><br />\n");
			return true;
		}

		//
		// Liste der Schiffe ermitteln, welche springen sollen
		//

		List<Ship> shiplist = new ArrayList<Ship>();
		// Falls vorhanden die Schiffe der Flotte einfuegen
		if( this.fleet != null ) {
			List<?> fleetships = db.createQuery("from Ship where id>0 and fleet=? AND x=? AND y=? AND system=? and docked=''")
			.setEntity(0, this.fleet)
			.setInteger(1, this.x)
			.setInteger(2, this.y)
			.setInteger(3, this.system)
			.list();

			for( Iterator<?> iter=fleetships.iterator(); iter.hasNext(); ) {
				Ship fleetship = (Ship)iter.next();

				// Bei Knossossprungpunkten darauf achten, dass das Portal nicht selbst mitspringt
				if( knode && (fleetship.getId() == nodeID) ) {
					continue;
				}

				shiplist.add(fleetship);
			}
		}
		// Keine Flotte -> nur das aktuelle Schiff einfuegen
		else {
			shiplist.add(this);
		}

		//
		// Jedes Schiff in der Liste springen lassen
		//
		for( Ship ship : shiplist ) {
			if( (this.lock != null) && (this.lock.length() > 0) ) {
				outputbuffer.append("<span style=\"color:red\">"+ship.getName()+" ("+ship.getId()+"): Das Schiff ist an ein Quest gebunden</span><br />\n");
				return true;
			}

			ShipTypeData shiptype = ship.getTypeData();

			// Liste der gedockten Schiffe laden
			List<Ship> docked = new ArrayList<Ship>();
			if( (shiptype.getADocks() > 0) || (shiptype.getJDocks() > 0) ) { 
				List<?> line = db.createQuery("from Ship where id>0 and docked in (?,?)")
					.setString(0, Integer.toString(ship.getId()))
					.setString(1, "l "+ship.getId())
					.list();
				for( Iterator<?> iter=line.iterator(); iter.hasNext(); ) {
					Ship aship = (Ship)iter.next();
					docked.add(aship);
				}
			}

			if( !knode ) {
				JumpNode node = (JumpNode)nodeObj;

				if( node.isWeaponBlock() && !user.hasFlag(User.FLAG_MILITARY_JUMPS) ) {
					//Schiff ueberprfen
					if( shiptype.isMilitary() ) {
						outputbuffer.append("<span style=\"color:red\">"+ship.getName()+" ("+ship.getId()+"): Die GCP verwehrt ihrem Kriegsschiff den Einflug nach "+node.getName()+"</span><br />\n");
						return true;
					}

					//Angedockte Schiffe ueberprfen
					if( shiptype.getADocks()>0 || shiptype.getJDocks()>0 ) {
						boolean wpnfound = false;
						for( Ship aship : docked ) {
							ShipTypeData checktype = aship.getTypeData();
							if( checktype.isMilitary() ) {
								wpnfound = true;
								break;	
							}
						}

						if(	wpnfound ) {
							outputbuffer.append("<span style=\"color:red\">"+ship.getName()+" ("+ship.getId()+"): Die GCP verwehrt einem/mehreren ihrer angedockten Kriegsschiffe den Einflug nach "+node.getName()+"</span><br />\n");
							return true;
						}
					}
				}
			}

			if( ship.getEnergy() < 5 ) {
				outputbuffer.append("<span style=\"color:red\">"+ship.getName()+" ("+ship.getId()+"): Zuwenig Energie zum Springen</span><br />\n");
				return true;
			}

			outputbuffer.append(ship.getName()+" ("+ship.getId()+") springt nach "+nodetarget+"<br />\n");
			ship.setSystem(outLoc.getSystem());
			ship.setX(outLoc.getX());
			ship.setY(outLoc.getY());
			ship.setEnergy(ship.getEnergy()-5);

			for( Ship aship : docked ) {
				aship.setX(outLoc.getX());
				aship.setY(outLoc.getY());
				aship.setSystem(outLoc.getSystem());
				aship.recalculateShipStatus();
			}

			ship.recalculateShipStatus();
		}

		return false;
	}

	/**
	 * Die verschiedenen Dock-Aktionen.
	 */
	public static enum DockMode {
		/**
		 * Schiff extern docken.
		 */
		DOCK,
		/**
		 * Schiff abdocken.
		 */
		UNDOCK,
		/**
		 * Schiff landen.
		 */
		LAND,
		/**
		 * Schiff starten.
		 */
		START;
	}
	
	/**
	 * Jaeger landen.
	 * Der Vorgang wird mit den Berechtigungen des Besitzers des Traegers ausgefuehrt.
	 * 
	 * @param dockships Eine Liste mit Schiffen, die landen sollen.
	 * @return <code>true</code>, falls ein Fehler aufgetreten ist
	 */
	public boolean land(Ship... dockships)
	{
		if(dockships == null || dockships.length == 0)
		{
			throw new IllegalArgumentException("Keine Schiffe zum landen gewaehlt.");
		}
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		StringBuilder outputbuffer = MESSAGE.get();

		//No superdock for landing
		Ship[] help = performLandingChecks(outputbuffer, false, dockships);
		boolean errors = false;
		if(help.length < dockships.length)
		{
			errors = true;
		}
		dockships = help;
		
		if(dockships.length == 0)
		{
			return errors;
		}
		
		//Check for type fighter
		List<Ship> ships = Common.cast(
			db.createQuery("from Ship s where locate(:fighter, shiptype.flags) > -1 and s in (:dockships)")
				.setParameterList("dockships", dockships)
				.setParameter("fighter", ShipTypes.SF_JAEGER)
				.list());
		
		if(ships.size() < dockships.length)
		{
			//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
			dockships = ships.toArray(new Ship[0]);
			errors = true;
		}
		
		long landedShips = (Long)db.createQuery("select count(*) from Ship where docked=?")
			.setParameter(0, "l "+getId())
			.uniqueResult();
		if(landedShips + dockships.length > this.getTypeData().getJDocks())
		{
			outputbuffer.append("<span style=\"color:red\">Fehler: Nicht gen&uuml;gend freier Landepl&auml;tze vorhanden</span><br />\n");
			
			//Shorten list to max allowed size
			int maxDockShips = this.getTypeData().getJDocks() - (int)landedShips;
			if( maxDockShips < 0 ) 
			{
				maxDockShips = 0;
			}
			help = new Ship[maxDockShips];
			System.arraycopy(dockships, 0, help, 0, maxDockShips);
			dockships = help;
		}
		
		if(dockships.length == 0)
		{
			return errors;
		}
		
		db.createQuery("update Ship s set docked=:docked where s in (:dockships)")
			.setParameterList("dockships", dockships)
			.setParameter("docked", "l "+this.getId())
			.executeUpdate();
		
		// Die Query aktualisiert leider nicht die bereits im Speicher befindlichen Objekte...
		for( Ship ship : dockships ) {
			ship.setDocked("l "+this.getId());
		}
		
		return errors;
	}
	
	/**
	 * Jaeger starten. Der Vorgang wird mit den Berechtigungen des Besitzers des Traegers ausgefuehrt.
	 * <b>Warnung:</b> Beim starten aller Schiffe werden die Objekte der Session - sofern sie vorhanden sind - 
	 * momentan nicht aktualisiert.
	 * 
	 * @param dockships Eine Liste mit Schiffen, die starten sollen. Keine Angabe bewirkt das alle Schiffe gestartet werden.
	 */
	public void start(Ship... dockships)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		if(dockships == null || dockships.length == 0)
		{
			db.createQuery("update Ship set docked='', system=:system, x=:x, y=:y where docked=:docked")
			  .setParameter("system", system)
			  .setParameter("x", x)
			  .setParameter("y", y)
			  .setParameter("docked", "l "+this.getId())
			  .executeUpdate();
		}
		else
		{	
			db.createQuery("update Ship s set docked='', system=:system, x=:x, y=:y where docked=:docked and s in (:dockships)")
				.setParameterList("dockships", dockships)
				.setParameter("system", system)
				.setParameter("x", x)
				.setParameter("y", y)
				.setParameter("docked", "l "+this.getId())
				.executeUpdate();
			
			for( Ship dockship : dockships ) {
				dockship.setDocked("");
				dockship.setX(x);
				dockship.setY(y);
				dockship.setSystem(system);
			}
		}
	}
	
	/**
	 * Schiffe abdocken.
	 * Der Vorgang wird mit den Berechtigungen des Besitzers des Traegers ausgefuehrt.
	 * 
	 * @param dockships Eine Liste mit Schiffen, die abgedockt werden sollen. Keine Angabe bewirkt das alle Schiffe abgedockt werden.
	 */
	public void undock(Ship... dockships)
	{
		if( dockships == null || dockships.length == 0 ) {
			List<Ship> dockshipList = getDockedShips();
			dockships = dockshipList.toArray(new Ship[dockshipList.size()]);
		}
		
		boolean gotmodule = false;
		for( Ship aship : dockships ) 
		{
			if(!aship.getDocked().equals("" + getId()))
			{
				//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
				continue;
			}
			
			aship.setDocked("");
		  
			ShipTypeData type = aship.getTypeData();
		  
			if( type.getShipClass() != ShipClasses.CONTAINER.ordinal() ) {
				continue;
		  	}
			gotmodule = true;
		  
			aship.removeModule( 0, Modules.MODULE_CONTAINER_SHIP, Integer.toString(aship.getId()) );		
			this.removeModule( 0, Modules.MODULE_CONTAINER_SHIP, Integer.toString(aship.getId()) );
		}
		  
		if( gotmodule ) 
		{
			Cargo cargo = this.cargo;
		  
			// Schiffstyp neu abholen, da sich der Maxcargo geaendert hat
			ShipTypeData shiptype = this.getTypeData();
		  
			Cargo newcargo = cargo;
			if( cargo.getMass() > shiptype.getCargo() ) 
			{
				newcargo = cargo.cutCargo( shiptype.getCargo() );	
			}
			else 
			{
				cargo = new Cargo();	
			}
		  
			for( int i=0; i < dockships.length && cargo.getMass() > 0; i++ ) 
			{
				Ship aship = dockships[i];
				ShipTypeData ashiptype = aship.getTypeData();

				if( (ashiptype.getShipClass() == ShipClasses.CONTAINER.ordinal()) && (cargo.getMass() > 0) ) 
				{
					Cargo acargo = cargo.cutCargo( ashiptype.getCargo() );
					if( !acargo.isEmpty() ) 
					{
						aship.setCargo(acargo);
					}	
				}
			}
			this.cargo = newcargo;
		}
		this.recalculateShipStatus();
	}

	/**
	 * Gibt die Liste aller an diesem Schiff (extern) gedockten Schiffe zurueck.
	 * @return Die Liste der Schiffe
	 */
	public List<Ship> getDockedShips()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<Ship> dockshipList = Common.cast(db.createQuery("from Ship where id>0 and docked= :docked")
			.setString("docked", Integer.toString(this.id))
			.list());
		return dockshipList;
	}
	
	/**
	 * Dockt eine Menge von Schiffen an dieses Schiff an.
	 * @param dockships Die anzudockenden Schiffe
	 * @return <code>true</code>, falls Fehler aufgetreten sind
	 */
	public boolean dock(Ship... dockships)
	{
		if(dockships == null || dockships.length == 0)
		{
			throw new IllegalArgumentException("Keine Schiffe zum landen gewaehlt.");
		}
		
		
		boolean superdock = owner.hasFlag(User.FLAG_SUPER_DOCK);
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		StringBuilder outputbuffer = MESSAGE.get();
		
		Ship[] help = performLandingChecks(outputbuffer, superdock, dockships);
		boolean errors = false;
		if(help.length < dockships.length)
		{
			errors = true;
		}
		dockships = help;
		
		if(dockships.length == 0)
		{
			return errors;
		}
		
		long dockedShips = (Long)db.createQuery("select count(*) from Ship where docked=?")
			.setParameter(0, ""+getId())
			.uniqueResult();
		if(!superdock)
		{
			//Check for size
			List<Ship> ships = Common.cast(
				db.createQuery("from Ship s where shiptype.size <= :maxsize and s in (:dockships)")
					.setParameterList("dockships", dockships)
					.setParameter("maxsize", ShipType.SMALL_SHIP_MAXSIZE)
					.list());
			
			if(ships.size() < dockships.length)
			{
				//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
				dockships = ships.toArray(new Ship[0]);
				errors = true;
			}
		}
		
		if(dockedShips + dockships.length > this.getTypeData().getADocks())
		{
			outputbuffer.append("<span style=\"color:red\">Fehler: Nicht gen&uuml;gend freier Andockplatz vorhanden</span><br />\n");
			
			//Shorten list to max allowed size
			int maxDockShips = this.getTypeData().getADocks() - (int)dockedShips;
			if( maxDockShips < 0 ) {
				maxDockShips = 0;
			}
			help = new Ship[maxDockShips];
			System.arraycopy(dockships, 0, help, 0, maxDockShips);
			dockships = help;
		}
		
		if(dockships.length == 0)
		{
			return errors;
		}
		
		Cargo cargo = this.cargo;
		final Cargo emptycargo = new Cargo();
		  
		for(Ship aship: dockships) 
		{
			aship.setDocked(Integer.toString(this.id));
			ShipTypeData type = aship.getTypeData();
		  
			if( type.getShipClass() != ShipClasses.CONTAINER.ordinal() ) 
			{
				continue;
			}
		  
			Cargo dockcargo = aship.getCargo();
			cargo.addCargo( dockcargo );
		  
			if( !dockcargo.isEmpty() ) 
			{
				aship.setCargo(emptycargo);
			}
		  
			aship.addModule( 0, Modules.MODULE_CONTAINER_SHIP, aship.getId()+"_"+(-type.getCargo()) );
			this.addModule( 0, Modules.MODULE_CONTAINER_SHIP, aship.getId()+"_"+type.getCargo() );
		}
		  
		this.cargo = cargo;
		this.recalculateShipStatus();
		
		return errors;
	}
	
	/**
	 * Checks, die sowohl fuers landen, als auch fuers andocken durchgefuehrt werden muessen.
	 * 
	 * @param outputbuffer Puffer fuer Fehlermeldungen.
	 * @param superdock <code>true</code>, falls im Superdock-Modus 
	 * 			(Keine Ueberpruefung von Groesse/Besitzer) gedockt/gelandet werden soll
	 * @param dockships Schiffe auf die geprueft werden soll.
	 * @return Die Liste der zu dockenden/landenden Schiffe
	 */
	private Ship[] performLandingChecks(StringBuilder outputbuffer, boolean superdock, Ship ... dockships)
	{
		if(dockships.length == 0)
		{
			return dockships;
		}
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		//Enforce position
		List<Ship> ships = Common.cast( 
			db.createQuery("from Ship s where system=:system and x=:x and y=:y and s in (:dockships)")
				.setParameterList("dockships", dockships)
				.setParameter("system", system)
				.setParameter("x", x)
				.setParameter("y", y)
				.list());
		
		if(ships.size() < dockships.length)
		{
			//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
			dockships = ships.toArray(new Ship[0]);

			if(dockships.length == 0)
			{
				return dockships;
			}
		}
		
		
		/*
		//Check quests
		ships = (List<Ship>)db.createQuery("from Ship where (lock is :lock or lock=:lock2) and id in ("+ ids +")")
							  .setParameter("lock", null)
							  .setParameter("lock2", "")
							  .list();
		*/
		
		ships = Common.cast(
			db.createQuery("from Ship s where (lock is null or lock = '') and s in (:dockships)")
				.setParameterList("dockships", dockships)
				.list());
		
		if(ships.size() < dockships.length)
		{
			outputbuffer.append("<span style=\"color:red\">Fehler: Mindestens ein Schiff ist an ein Quest gebunden</span><br />\n");
			dockships = ships.toArray(new Ship[0]);
			
			if(dockships.length == 0)
			{
				return dockships;
			}
		}
		
		
		//Check already docked
		ships = Common.cast(
			db.createQuery("from Ship s where docked='' and s in (:dockships)")
				.setParameterList("dockships", dockships)
				.list());
		
		if(ships.size() < dockships.length)
		{
			//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
			dockships = ships.toArray(new Ship[0]);
			
			if(dockships.length == 0)
			{
				return dockships;
			}
		}
		
		//Enforce owner
		if(!superdock)
		{
			ships = Common.cast(
				db.createQuery("from Ship s where owner=:owner and s in (:dockships)")
					.setParameterList("dockships", dockships)
					.setParameter("owner", owner)
					.list());
			
			if(ships.size() < dockships.length)
			{
				//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
				dockships = ships.toArray(new Ship[0]);
				
				if(dockships.length == 0)
				{
					return dockships;
				}
			}
		}
		
		return dockships;
	}
		  
	/**
	 * Schiffe an/abdocken sowie Jaeger landen/starten. Der Dockvorgang wird mit den Berechtigungen
	 * des Schiffsbesitzers ausgefuehrt.
	 * Diese Methode sollte der Uebersichtlichkeit halber nur verwendet werden, 
	 * wenn die Aktion (docken, abdocken, etc.) nicht im Voraus bestimmt werden kann.
	 * 
	 * @param mode Der Dock-Modus (Andocken, Abdocken usw)
	 * @param dockships eine Liste mit Schiffen, welche (ab)docken oder landen/starten sollen. Keine Angabe bewirkt das alle Schiffe abgedockt/gestartet werden
	 * @return <code>true</code>, falls ein Fehler aufgetreten ist
	 */
	public boolean dock(DockMode mode, Ship ... dockships) 
	{		
		if(mode == DockMode.DOCK)
		{
			return this.dock(dockships);
		}
		
		if(mode == DockMode.LAND)
		{
			return this.land(dockships);
		}
		
		if(mode == DockMode.START)
		{
			this.start(dockships);
		}
		
		if(mode == DockMode.UNDOCK)
		{
			this.undock(dockships);
		}
	
  		return false;
  	}
	  


	/**
	 * Generiert ein Truemmerteil mit Loot fuer das Schiff unter Beruecksichtigung desjenigen,
	 * der es zerstoert hat. Wenn fuer das Schiff kein Loot existiert oder keiner generiert wurde (Zufall spielt eine
	 * Rolle!), dann wird kein Truemmerteil erzeugt.
	 * @param destroyer Die ID des Spielers, der es zerstoert hat
	 */
	public void generateLoot( int destroyer ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		ShipTypeData shiptype = this.getTypeData();

		int rnd = RandomUtils.nextInt(101);

		// Gibts was zu looten?
		if( rnd > shiptype.getChance4Loot() ) {
			return;
		}

		// History analysieren (Alle Schiffe die erst kuerzlich uebergeben wurden, haben kein Loot)
		// TODO: Das funktioniert im Moment nur, weil im Log nur Uebergaben stehen...
		String[] history = StringUtils.split(this.history.trim(), '\n');
		if( history.length > 0 ) {
			String lastHistory = history[history.length-1].trim();

			final int length = "&Uuml;bergeben am [tick=".length();

			if( lastHistory.startsWith("&Uuml;bergeben am [tick=") ) {
				int endIndex = lastHistory.indexOf("] an ",length);
				if( endIndex > -1 ) {				
					try {
						int date = Integer.parseInt(
								lastHistory.substring(
										length,
										endIndex
								)
						);

						if( ContextMap.getContext().get(ContextCommon.class).getTick() - date < 49 ) {
							return;
						}
					}
					catch( StringIndexOutOfBoundsException e ) {
						log.warn("[Ships.generateLoot] Fehler beim Parsen des Schiffshistoryeintrags '"+lastHistory+"' - "+length+", "+endIndex);
					}
				}
				else {
					log.warn("[Ships.generateLoot] Fehler beim Parsen des Schiffshistoryeintrags '"+lastHistory+"'");
				}
			}
		}	

		// Moeglichen Loot zusammensuchen
		List<ShipLoot> loot = new ArrayList<ShipLoot>();
		int maxchance = 0;

		List<?> lootList = db.createQuery("from ShipLoot where owner=? and shiptype in (?,?) and targetuser in (0,?) and totalmax!=0")
		.setInteger(0, this.owner.getId())
		.setInteger(1, this.shiptype.getId())
		.setInteger(2, -this.id)
		.setInteger(3, destroyer)
		.list();

		for( Iterator<?> iter=lootList.iterator(); iter.hasNext(); ) {
			ShipLoot lootEntry = (ShipLoot)iter.next();

			maxchance += lootEntry.getChance();
			loot.add(lootEntry);
		}

		if( loot.size() == 0 ) {
			return;	
		}

		ConfigValue truemmerMaxItems = (ConfigValue)db.get(ConfigValue.class, "truemmer_maxitems");
		
		// Und nun den Loot generieren
		Cargo cargo = new Cargo();

		for( int i=0; i <= Integer.parseInt(truemmerMaxItems.getValue()); i++ ) {
			rnd = RandomUtils.nextInt(maxchance+1);
			int currentchance = 0;
			for( int j=0; j < loot.size(); j++ ) {
				ShipLoot aloot = loot.get(j);
				if( aloot.getChance() + currentchance > rnd ) {
					if( aloot.getTotalMax() > 0 ) {
						aloot.setTotalMax(aloot.getTotalMax()-1);	
					}
					cargo.addResource( Resources.fromString(aloot.getResource()), aloot.getCount() );
					break;	
				}	

				currentchance += aloot.getChance();
			}

			rnd = RandomUtils.nextInt(101);

			// Gibts nichts mehr zu looten?
			if( rnd > shiptype.getChance4Loot() ) {
				break;
			}
		}

		// Truemmer-Schiff hinzufuegen und entfernen-Task setzen
		Ship truemmer = new Ship((User)db.get(User.class, -1));
		truemmer.setName("Tr&uuml;mmerteile");
		truemmer.setBaseType((ShipType)db.get(ShipType.class, config.getInt("CONFIG_TRUEMMER")));
		truemmer.setCargo(cargo);
		truemmer.setX(this.x);
		truemmer.setY(this.y);
		truemmer.setSystem(this.system);
		truemmer.setHull(config.getInt("CONFIG_TRUEMMER_HUELLE"));
		truemmer.setVisibility(destroyer);
		int id = (Integer)db.save(truemmer);

		Taskmanager.getInstance().addTask(Taskmanager.Types.SHIP_DESTROY_COUNTDOWN, 21, Integer.toString(id), "", "" );

		return;
	}

	/**
	 * Entfernt das Schiff aus der Datenbank.
	 */
	public void destroy() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		// Checken wir mal ob die Flotte danach noch bestehen darf....
		if( this.fleet != null ) {
			long fleetcount = (Long)db.createQuery("select count(*) from Ship where fleet=?")
			.setInteger(0, fleet.getId())
			.iterate().next();
			if( fleetcount <= 2 ) {
				final ShipFleet fleet = this.fleet;
				
				final Iterator<?> shipIter = db.createQuery("from Ship where fleet=?")
					.setEntity(0, this.fleet)
					.iterate();
				while( shipIter.hasNext() ) {
					Ship aship = (Ship)shipIter.next();
					aship.setFleet(null);
				}
				
				db.delete(fleet);
			}
		}

		// Ist das Schiff selbst gedockt? -> Abdocken
		if(this.docked != null && !this.docked.equals("") && (this.docked.charAt(0) != 'l') ) {
			Ship docked = (Ship)db.get(Ship.class, Integer.parseInt(this.docked));
			if(docked != null)
			{
				docked.undock(this);
			}
			else
			{
				log.debug("Docked entry of ship was illegal: " + this.docked);
			}
		}

		// Wenn es das Flagschiff ist -> Flagschiff auf null setzen
		if( (this.owner.getFlagschiff() != null) && (this.id == this.owner.getFlagschiff().getID()) ) {
			this.owner.setFlagschiff(null);
		}

		// Evt. gedockte Schiffe abdocken
		ShipTypeData type = this.getTypeData();
		if( type.getADocks() != 0 ) {
			undock();	
		}
		if( type.getJDocks() != 0 ) {
			start();	
		}

		// Gibts bereits eine Loesch-Task? Wenn ja, dann diese entfernen
		Taskmanager taskmanager = Taskmanager.getInstance();
		Task[] tasks = taskmanager.getTasksByData( Taskmanager.Types.SHIP_DESTROY_COUNTDOWN, Integer.toString(this.id), "*", "*");
		for( int i=0; i < tasks.length; i++ ) {
			taskmanager.removeTask(tasks[i].getTaskID());	
		}

		// Falls eine respawn-Zeit gesetzt ist und ein Respawn-Image existiert -> respawn-Task setzen
		if( this.respawn != null ) {
			Ship negship = (Ship)db.get(Ship.class, -this.id);
			if( negship != null ) {
				taskmanager.addTask(Taskmanager.Types.SHIP_RESPAWN_COUNTDOWN, this.respawn, Integer.toString(-this.id), "", "");	
			}
		}

		// Und nun loeschen wir es...
		db.createQuery("delete from Offizier where dest=?")
			.setString(0, "s "+this.id)
			.executeUpdate();

		db.createQuery("delete from Jump where shipid=?")
			.setInteger(0, this.id)
			.executeUpdate();

		ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where shipid=?")
			.setInteger(0, this.id)
			.uniqueResult();

		if( werft != null ) {
			werft.destroy();
		}

		db.createQuery("delete from ShipModules where id=?")
			.setInteger(0, this.id)
			.executeUpdate();

		db.delete(this);
		
		this.destroyed = true;
	}

	/**
	 * Gibt zurueck, ob das Schiff mittels {@link #destroy()} zerstoert
	 * wurde und somit nicht mehr in der Datenbank existiert. Diese
	 * Methode laesst hingegen keine Rueckschluesse zu, ob ein Schiff innerhalb
	 * einer Schlacht als zerstoert <b>markiert</b> wurde.
	 * @return <code>true</code> falls es bereits zerstoert wurde
	 */
	public boolean isDestroyed() {
		return this.destroyed;
	}
	
	/**
	 * Uebergibt ein Schiff an einen anderen Spieler. Gedockte/Gelandete Schiffe
	 * werden, falls moeglich, mituebergeben.
	 * @param newowner Der neue Besitzer des Schiffs
	 * @param testonly Soll nur getestet (<code>true</code>) oder wirklich uebergeben werden (<code>false</code>)
	 * @return <code>true</code>, falls ein Fehler bei der Uebergabe aufgetaucht ist (Uebergabe nicht erfolgreich)
	 */
	public boolean consign( User newowner, boolean testonly ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( this.id < 0 ) {
			throw new UnsupportedOperationException("consign kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
		}

		if( newowner == null ) {
			MESSAGE.get().append("Der angegebene Spieler existiert nicht");
			return true;
		}

		if( (newowner.getVacationCount() != 0) && (newowner.getWait4VacationCount() == 0) ) {
			MESSAGE.get().append("Sie k&ouml;nnen keine Schiffe an Spieler &uuml;bergeben, welche sich im Vacation-Modus befinden");
			return true;
		}

		if( newowner.hasFlag( User.FLAG_NO_SHIP_CONSIGN ) ) {
			MESSAGE.get().append("Sie k&ouml;nnen diesem Spieler keine Schiffe &uuml;bergeben");
		}

		if( this.lock != null ) {
			MESSAGE.get().append("Die '"+this.name+"'("+this.id+") kann nicht &uuml;bergeben werden, da diese in ein Quest eingebunden ist");
			return true;
		}

		if( this.status.indexOf("noconsign") != -1 ) {
			MESSAGE.get().append("Die '"+this.name+"' ("+this.id+") kann nicht &uuml;bergeben werden");
			return true;
		}

		UserFlagschiffLocation flagschiff = this.owner.getFlagschiff();

		boolean result = true;		
		if( (flagschiff != null) && (flagschiff.getID() == this.id) ) {
			result = newowner.hasFlagschiffSpace();
		}

		if( !result  ) {
			MESSAGE.get().append("Die "+this.name+" ("+this.id+") kann nicht &uuml;bergeben werden, da der Spieler bereits &uuml;ber ein Flagschiff verf&uuml;gt");
			return true;
		}

		User oldOwner = this.owner;

		if( !testonly ) {	
			this.history += "&Uuml;bergeben am [tick="+ContextMap.getContext().get(ContextCommon.class).getTick()+"] an "+newowner.getName()+" ("+newowner.getId()+")\n";
			
			this.removeFromFleet();
			this.owner = newowner;
			
			db.createQuery("update Offizier set userid=? where dest=?")
				.setEntity(0, newowner)
				.setString(1, "s "+this.id)
				.executeUpdate();

			Common.dblog( "consign", Integer.toString(this.id), Integer.toString(newowner.getId()),	
					"pos", new Location(this.system, this.x, this.y).toString(),
					"shiptype", Integer.toString(this.shiptype.getId()) );

			if( (flagschiff != null) && (flagschiff.getType() == UserFlagschiffLocation.Type.SHIP) && 
					(flagschiff.getID() == this.id) ) {
				oldOwner.setFlagschiff(null);
				newowner.setFlagschiff(this.id);
			}
			
			if( getTypeData().getWerft() != 0 ) {
				ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=?")
					.setEntity(0, this)
					.uniqueResult();
				
				if( (werft != null) && (werft.getKomplex() != null) ) {
					werft.removeFromKomplex();
				}
			}
		}

		StringBuilder message = MESSAGE.get();
		List<?> s = db.createQuery("from Ship where id>0 and docked in (?,?)")
			.setString(0, Integer.toString(this.id))
			.setString(1, "l "+this.id)
			.list();
		for( Iterator<?> iter=s.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			int oldlength = message.length();
			boolean tmp = aship.consign(newowner, testonly );
			if( tmp && !testonly ) {
				this.dock( aship.getDocked().charAt(0) == 'l' ? DockMode.START : DockMode.UNDOCK, aship);			
			}

			if( (oldlength > 0) && (oldlength != message.length()) ) {
				message.insert(oldlength-1, "<br />");
			}
		}

		Cargo cargo = this.cargo;
		List<ItemCargoEntry> itemlist = cargo.getItems();
		for( ItemCargoEntry item : itemlist ) {
			Item itemobject = item.getItemObject();
			if( itemobject.isUnknownItem() ) {
				newowner.addKnownItem(item.getItemID());
			}
		}

		return false;
	}

	/**
	 * Entfernt das Schiff aus der Flotte. 
	 */
	public void removeFromFleet() {
		if( this.id < 0 ) {
			throw new UnsupportedOperationException("removeFromFleet kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
		}

		if( this.fleet == null ) {
			return;
		}

		this.fleet.removeShip(this);
		
		MESSAGE.get().append(ShipFleet.MESSAGE.getMessage());
	}

	@Override
	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}

	/**
	 * Gibt die Liste aller Flags zurueck, ueber die der angegebene
	 * Schiffstyp verfuegt.
	 * @param shiptype Die Daten des Schiffstyps
	 * @return Die Liste der Flags
	 */
	public static String[] getShipTypeFlagList(ShipTypeData shiptype) {
		return StringUtils.split( shiptype.getFlags(), ' ');
	}

	/**
	 * Gibt die Typen-Daten des angegebenen Schiffs bzw Schifftyps zurueck.
	 * @param shiptype Die ID des Schiffs bzw des Schifftyps
	 * @return die Typen-Daten
	 */
	public static ShipTypeData getShipType( int shiptype ) {
		return getShipType(shiptype, null, false);
	}

	private static ShipTypeData getShipType( int shiptype, ShipModules shipdata, boolean plaindata ) {	
		org.hibernate.Session db = ContextMap.getContext().getDB();
		if( !plaindata ) {
			ShipType type = (ShipType)db.get(ShipType.class, shiptype);
			if( type == null ) {
				throw new NoSuchShipTypeException("Der Schiffstyp "+shiptype+" existiert nicht");
			}
			
			String picture = "";
			if( shipdata == null ) {
				picture = type.getPicture();
			}
			else {
				picture = shipdata.getPicture();
			}

			return new ShipTypeDataPictureWrapper(shipdata != null ? shipdata : type, 
					!type.getPicture().equals(picture) ? true : false);
		}

		if( shipdata != null ) {
			return shipdata;
		}
		
		ShipType type = (ShipType)db.get(ShipType.class, shiptype);
		if( type == null ) {
			throw new NoSuchShipTypeException("Der Schiffstyp "+shiptype+" existiert nicht");
		}
		
		return type;
	}

	/**
	 * Gibt das Schiff zurueck, an dem dieses Schiff gedockt/gelandet ist.
	 * @return Das Schiff oder <code>null</code>
	 */
	public Ship getBaseShip() {
		int baseShipId = getBaseShipId();
		if( baseShipId == -1 ) {
			return null;
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (Ship)db.get(Ship.class, baseShipId);
	}

	private int getBaseShipId() {
		if( this.docked.isEmpty() ) {
			return -1;
		}

		if( this.docked.charAt(0) == 'l' ) {
			return Integer.parseInt(this.docked.substring(2));
		}
		
		return Integer.parseInt(this.docked);
	}

	/**
	 * Checks if there is an asteroid of the user in the same starsystem the ship
	 * is in.
	 * 
	 * @return <code>true</code> if the ship can be feed from the pool, <code>false</code> if not
	 */
	public boolean isUserCargoUsable() {
		if( config.getInt("USE_NEW_FOOD_SYSTEM") == 1 ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			return ((Number)db.createQuery("select count(*) from Base where owner=? and system=?")
				.setEntity(0, this.owner)
				.setInteger(1, this.system)
				.iterate().next()).intValue() != 0;
		}
		
		return true;
	}

	@Override
	public String transfer(Transfering to, ResourceID resource, long count) {
		return new Transfer().transfer(this, to, resource, count);
	}

	/**
	 * Gibt den maximalen Cargo, den das Schiff aufnehmen kann, zurueck.
	 * @return Der maximale Cargo
	 */
	public long getMaxCargo() {
		return getTypeData().getCargo();
	}
	
	/**
	 * Gibt den Wert der ablativen Panzerung des Schiffes zurueck.
	 * @return Der Panzerungswert
	 */
	public int getAblativeArmor() {
		return ablativeArmor;
	}

	/**
	 * Setzt die ablative Panzerung des Schiffes.
	 * @param ablativeArmor Der neue Panzerungswert
	 */
	public void setAblativeArmor(int ablativeArmor) {
		this.ablativeArmor = ablativeArmor;
	}

	/**
	 * Gibt zurueck, ob Jaeger beim Kampfbeginn gestartet werden sollen.
	 * @return <code>true</code>, falls sie gestartet werden sollen
	 */
	public boolean startFighters() {
		return startFighters;
	}

	/**
	 * Setzt, ob Jaeger beim Kampfbeginn gestartet werden sollen.
	 * @param startFighters <code>true</code>, falls sie gestartet werden sollen
	 */
	public void setStartFighters(boolean startFighters) {
		this.startFighters = startFighters;
	}
	
	/**
	 * Bestimmt, ob ein Schiff sein SRS nutzen kann.
	 * 
	 * @return <code>False</code>, wenn das Schiff kein SRS hat oder gelandet ist. <code>True</code> ansonsten.
	 */
	public boolean canUseSrs() {
		if(!getTypeData().hasSrs()) {
			return false;
		}
		
		String motherShip = getDocked();
		if(motherShip != null && motherShip.startsWith("l")) {
			return false;
		}
		
		return true;
	}

	/**
	 * Gibt die Anzahl der an externen Docks gedockten Schiffe zurueck.
	 * @return Die Anzahl
	 */
	public long getDockedCount() {
		if( getTypeData().getADocks() == 0 ) {
			return 0;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		return (Long)db.createQuery("select count(*) from Ship where id>0 AND docked=?")
			.setString(0, Integer.toString(this.id))
			.iterate().next();
	}
	
	/**
	 * Gibt die Anzahl der gelandeten Schiffe zurueck.
	 * @return Die Anzahl
	 */
	public long getLandedCount() {
		if( getTypeData().getJDocks() == 0 ) {
			return 0;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		return (Long)db.createQuery("select count(*) from Ship where id>0 AND docked=?")
			.setString(0, "l "+this.id)
			.iterate().next();
	}
	
	/**
	 * Gibt den Offizier des Schiffes zurueck.
	 * @return Der Offizier
	 */
	public Offizier getOffizier() {
		if( this.offizier == null ) {
			this.offizier = Offizier.getOffizierByDest('s', this.id);
		}
		if( this.offizier != null ) {
			String[] dest =  this.offizier.getDest();
			if( !dest[0].equals("s") || Integer.parseInt(dest[1]) != this.id ) {
				this.offizier = null;
			}
		}
		return this.offizier;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
	
	/**
	 * Gibt zurueck, ob das Schiff beschaedigt ist.
	 * 
	 * @return <code>true</code>, wenn das Schiff beschaedigt ist, ansonsten <code>false</code>
	 */
	public boolean isDamaged() {
		if(this.getAblativeArmor() < this.getTypeData().getAblativeArmor()) {
			return true;
		}
		
		if(this.getHull() < this.getTypeData().getHull()) {
			return true;
		}
		
		if(this.getEngine() < 100) {
			return true;
		}
		
		if(this.getSensors() < 100) {
			return true;
		}
		
		if(this.getComm() < 100) {
			return true;
		}
		
		if(this.getWeapons() < 100) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Transfers crew from the ship to an asteroid.
	 * 
	 * @param base Base that sends the crew.
	 * @param amount People that should be transfered.
	 * @return People that where transfered.
	 */
	public int transferCrew(Base base, int amount) {
		//Check ship position
		if(base.getSystem() != getSystem() || base.getX() != getX() || base.getY() != getY()) {
			return 0;
		}
		
		setCrew(getCrew() - amount);
		base.setBewohner(base.getBewohner() + amount);
		
		recalculateShipStatus();
		
		return amount;
	}
	
	/**
	 * Greift ein gegnerisches Schiff an.
	 * 
	 * @param enemy Das Schiff, dass angegriffen werden soll.
	 * @return Den Kampf oder null, falls kein Kampf erstellt werden konnte.
	 */
	public Battle attack(Ship enemy)
	{
		return Battle.create(this.getOwner().getId(), this.getId(), enemy.getId());
	}
	
	/**
	 * Greift ein gegnerisches Schiff an.
	 * 
	 * @param enemy Das Schiff, dass angegriffen werden soll.
	 * @param startOwn <code>true</code>, wenn die eigenen Jaeger starten sollen
	 * @return Den Kampf oder null, falls kein Kampf erstellt werden konnte.
	 */
	public Battle attack(Ship enemy, boolean startOwn)
	{
		return Battle.create(this.getOwner().getId(), this.getId(), enemy.getId());
	}

	/**
	 * Gibt an, ob das Schiff auf einem anderen Schiff gelandet ist.
	 * 
	 * @return <code>true</code>, wenn das Schiff gelandet ist, sonst <code>false</code>
	 */
	public boolean isLanded()
	{
		return getDocked().startsWith("l");
	}
	
	/**
	 * Gibt an, ob das Schiff an einem anderen Schiff angedockt ist.
	 * 
	 * @return <code>true</code>, wenn das Schiff gedockt ist, sonst <code>false</code>
	 */
	public boolean isDocked()
	{
		return !getDocked().trim().equals("") && !isLanded();
	}
}
