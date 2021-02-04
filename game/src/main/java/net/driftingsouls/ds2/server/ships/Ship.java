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

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Transfer;
import net.driftingsouls.ds2.server.cargo.Transfering;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.Feeding;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.units.ShipUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repraesentiert ein Schiff in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ships", indexes = {
	@Index(name = "ship_status", columnList = "status"),
	@Index(name = "ship_docked", columnList = "docked")
})
@BatchSize(size=50)
@Cache(usage= CacheConcurrencyStrategy.READ_WRITE)
public class Ship implements Locatable,Transfering,Feeding {
	private static final Log log = LogFactory.getLog(Ship.class);

	/**
	 * Objekt mit Funktionsmeldungen.
	 */
	public static final ContextLocalMessage MESSAGE = new ContextLocalMessage();

	@Id @GeneratedValue(generator="ds-shipid")
	@GenericGenerator(name="ds-shipid", strategy = "net.driftingsouls.ds2.server.ships.ShipIdGenerator")
	private int id;

	@OneToOne(cascade={CascadeType.REFRESH,CascadeType.DETACH,CascadeType.REMOVE})
	@JoinColumn(name="modules", foreignKey = @ForeignKey(name="ships_fk_ships_modules"))
	@BatchSize(size=50)
	@NotFound(action = NotFoundAction.IGNORE)

	private ShipModules modules;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="owner", nullable=false, foreignKey = @ForeignKey(name="ships_fk_users"))
	@BatchSize(size=50)
	private User owner;

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="type", nullable=false, foreignKey = @ForeignKey(name="ships_type_fk"))
	@BatchSize(size=50)
	@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
	private ShipType shiptype;

	@Type(type="largeCargo")
	@BatchSize(size=50)
	@Column(nullable = false)
	private Cargo cargo;

	private long nahrungcargo;
	private int x;
	private int y;
	private int system;

	@Column(nullable = false)
	private String status;

	private int crew;
	private int e;

	@Column(name="s", nullable = false)
	private int heat;
	private int hull;
	private int shields;

	@Lob
	@Column(name="heat", nullable = false)
	private String weaponHeat;
	private int engine;
	private int weapons;
	private int comm;
	private int sensors;

	@Column(nullable = false)
	private String docked;

	@Enumerated(EnumType.ORDINAL)
	private Alarmstufe alarm;

	@OneToOne(fetch=FetchType.LAZY)
	@BatchSize(size=100)
	@JoinColumn(foreignKey = @ForeignKey(name="ships_fk_schiff_einstellungen"))
	private SchiffEinstellungen einstellungen;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="fleet", foreignKey = @ForeignKey(name="ships_fk_ship_fleets"))
	@BatchSize(size=50)
	private ShipFleet fleet;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="battle", foreignKey = @ForeignKey(name="ships_fk_battles"))
	@BatchSize(size=50)
	private Battle battle;

	private boolean battleAction;

	@Column(nullable = false)
	private String jumptarget;

	@OneToOne(
			fetch=FetchType.LAZY,
			cascade=CascadeType.ALL,
			optional = false)
	@PrimaryKeyJoinColumn(name="id", referencedColumnName="id")
	private ShipHistory history;

	private int ablativeArmor;

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@OneToMany(
			fetch=FetchType.LAZY,
			targetEntity=net.driftingsouls.ds2.server.ships.ShipUnitCargoEntry.class,
			cascade = {CascadeType.DETACH,CascadeType.REFRESH,CascadeType.MERGE},
			mappedBy="schiff")
	@BatchSize(size=500)
	@NotFound(action = NotFoundAction.IGNORE)
	private Set<ShipUnitCargoEntry> units;

	@OneToMany(
			fetch=FetchType.LAZY,
			cascade = {CascadeType.DETACH,CascadeType.REFRESH,CascadeType.MERGE},
			mappedBy="stationiertAufSchiff")
	@BatchSize(size=500)
	@NotFound(action = NotFoundAction.IGNORE)
	private Set<Offizier> offiziere;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ship", orphanRemoval = true)
    private Set<ShipFlag> flags;

	@Version
	private int version;

	@Transient
	private boolean destroyed = false;
	@Transient
	private UnitCargo unitcargo = null;

    //Ship flags, no enum as this doesn't work very well with hibernate

    /** Das Schiff wurde vor kurzem repariert und kann aktuell nicht erneut repariert werden. */
    public static final int FLAG_RECENTLY_REPAIRED = 1;
		/** Der Felsbrocken wurde vor kurzem abgebaut und kann aktuell nicht weiter abgebaut werden. */
		public static final int FLAG_RECENTLY_MINED = 1;

	/**
	 * Konstruktor.
	 */
	protected Ship() {
		// EMPTY
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
		this.name = "";
		this.status = "";
		this.jumptarget = "";
		this.history = new ShipHistory(this);
		this.docked = "";
		this.weaponHeat = "";
		this.setBaseType(shiptype);
		this.x = x;
		this.y = y;
		this.system = system;
		this.hull = shiptype.getHull();
		this.ablativeArmor = shiptype.getAblativeArmor();
		this.engine = 100;
		this.weapons = 100;
		this.comm = 100;
		this.sensors = 100;
		this.offiziere = new HashSet<>();
		this.alarm = Alarmstufe.GREEN;
	}

    /**
     * @param flags Die Flags des Schiffes
     */
    public void setFlags(Set<ShipFlag> flags)
    {
        this.flags = flags;
    }

	/**
	 * Gibt die ID des Schiffes zurueck.
	 * @return Die ID des Schiffes
	 */
	@Override
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
	@Override
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
	@Override
	public Cargo getCargo() {
		return new Cargo(this.cargo);
	}

	/**
	 * Setzt den Cargo des Schiffes.
	 * @param cargo Der neue Cargo
	 */
	@Override
	public void setCargo(Cargo cargo) {
		this.cargo = new Cargo(cargo);
	}

	/**
	 * @return The current amount of food on the object.
	 */
	@Override
	public long getNahrungCargo()
	{
		return this.nahrungcargo;
	}

	/**
	 * Updates the amount of food on the object.
	 *
	 * @param nahrungcargo The new amount of food.
	 */
	@Override
	public void setNahrungCargo(long nahrungcargo)
	{
		this.nahrungcargo = nahrungcargo;
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
	 * Setzt die Position des Schiffes.
	 * @param locatable Die Position
	 */
	public void setLocation(Locatable locatable)
	{
		Location loc = locatable.getLocation();
		this.system = loc.getSystem();
		this.x = loc.getX();
		this.y = loc.getY();
	}

	/**
	 * Gibt die Alarmstufe des Schiffes zurueck.
	 * @return Die Alarmstufe
	 */
	public Alarmstufe getAlarm() {
		return alarm;
	}

	/**
	 * Setzt die Alarmstufe des Schiffes.
	 * @param alarm Die neue Alarmstufe
	 */
	public void setAlarm(Alarmstufe alarm) {
		this.alarm = alarm;
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
	 * Gibt den UnitCargo des Schiffes zurueck.
	 * @return Der UnitCargo
	 */
	public UnitCargo getUnitCargo() {
		if(unitcargo == null)
		{
			List<UnitCargoEntry> entries;
			if(getTypeData().getUnitSpace() > 0 && units != null)
			{
				entries = new ArrayList<>(units);
			}
			else
			{
				entries = new ArrayList<>();
			}
			unitcargo = new ShipUnitCargo(entries, this);
		}
		return unitcargo;
	}

	/**
	 * Setzt den UnitCargo des Schiffes.
	 * @param unitcargo Der UnitCargo
	 */
	public void setUnitCargo(UnitCargo unitcargo) {
		UnitCargo newCargo = this.getUnitCargo();
		if( unitcargo != newCargo )
		{
			// Ein anderer Cargo soll gespeichert werden
			// momentane Instanz leeren und neu befuellen
			newCargo.clear();
			newCargo.addCargo(unitcargo);
		}
		newCargo.save();
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
	public Map<String,Integer> getWeaponHeat() {
		return Weapons.parseWeaponList(this.weaponHeat);
	}

	/**
	 * Setzt die Waffenhitze.
	 * @param heat Die neue Waffenhitze
	 */
	public void setWeaponHeat(Map<String,Integer> heat) {
		this.weaponHeat = Weapons.packWeaponList(heat);
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
	 * Gibt die Dockdaten des Schiffes zurueck.
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
	 * Returns true if Ship is a tradepost.
	 * @return tradepoststatus
	 */
	public boolean isTradepost()
	{
		return this.getStatus().contains("tradepost") || this.getTypeData().hasFlag(ShipTypeFlag.TRADEPOST);
	}

	/**
	 * Setzt das Statusfeld des Schiffes.
	 * @param status das neue Statusfeld
	 */
	public void setStatus(String status) {
		this.status = status;
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
	 * Gibt die Typen-Daten des Schiffs zurueck.
	 * @return die Typen-Daten
	 */
	public ShipTypeData getTypeData() {
		if( this.modules != null ) {
			return this.modules;
		}

		return this.shiptype;
	}

	public void setDestroyed(boolean destroyed) {
		this.destroyed = destroyed;
	}

	/**
	 * Gibt die momentanen Einstellungen des Schiffs zurueck.
	 * Sofern keine persistierten Einstellungen vorliegen wird
	 * ein Default-Einstellungsobjekt zurueckgegeben. Bei
	 * Aenderungen ist dieses durch den Aufrufer zu persistieren.
	 * @return Die Einstellungen
	 */
	public SchiffEinstellungen getEinstellungen()
	{
		if( this.einstellungen != null )
		{
			return this.einstellungen;
		}
		return new SchiffEinstellungen();
	}

	/**
	 * Setzt das Einstellungsobjekt des Schiffs.
	 * Es sollte stattdessen besser {@link SchiffEinstellungen#persistIfNecessary(Ship)}
	 * verwendet werden.
	 * @param einstellungen Das Einstellungsobjekt
	 */
	public void setEinstellungen(SchiffEinstellungen einstellungen)
	{
		this.einstellungen = einstellungen;
	}

	/**
	 * Returns the crew scaled by a factor according to alert.
	 * Ships with active alert consume more food.
	 *
	 * @return Crew scaled by a factor according to shiptype.
	 */
	public int getScaledCrew() {
		double scale = alarm.getAlertScaleFactor();
		return (int)Math.ceil(this.crew*scale);
	}

	/**
	 * Returns the units scaled by a factor according to alert.
	 * Ships with active alert consume more food.
	 *
	 * @return Units scaled by a factor according to shiptype.
	 */
	public int getScaledUnits() {
		if(getUnitCargo() != null)
		{
			double scale = alarm.getAlertScaleFactor();
			return (int)Math.ceil(this.getUnitCargo().getNahrung()*scale);
		}
		return 0;
	}

	public ShipModules getModules() {
		return modules;
	}

	public void setModules(ShipModules modules) {
		this.modules = modules;
	}

	/**
	 * Gibt die Moduleintraege des Schiffes zurueck.
	 * @return Eine Liste von Moduleintraegen
	 */
	public ModuleEntry[] getModuleEntries() {
		List<ModuleEntry> result = new ArrayList<>();

		if( this.modules == null ) {
			return new ModuleEntry[0];
		}

		ShipModules moduletbl = this.modules;
		if( moduletbl.getModules().length() != 0 ) {
			String[] modulelist = StringUtils.split(moduletbl.getModules(), ';');
			for (String aModulelist : modulelist)
			{
				result.add(ModuleEntry.unserialize(aModulelist));
			}
		}

		return result.toArray(new ModuleEntry[0]);
	}

	/**
	 * Die verschiedenen Dock-Aktionen.
	 */
	public enum DockMode {
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
		START
	}

	public void clearFlags() {
		flags.clear();
	}

	/**
	 * Gibt zurueck, ob das Schiff zerstoert
	 * wurde und somit nicht mehr in der Datenbank existiert. Diese
	 * Methode laesst hingegen keine Rueckschluesse zu, ob ein Schiff innerhalb
	 * einer Schlacht als zerstoert <b>markiert</b> wurde.
	 * @return <code>true</code> falls es bereits zerstoert wurde
	 */
	public boolean isDestroyed() {
		return this.destroyed;
	}

	@Override
	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}

	@Override
	public String transfer(Transfering to, ResourceID resource, long count) {
		return new Transfer().transfer(this, to, resource, count);
	}

	/**
	 * Gibt den maximalen Cargo, den das Schiff aufnehmen kann, zurueck.
	 * @return Der maximale Cargo
	 */
	@Override
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
	 * Bestimmt, ob ein Schiff sein SRS nutzen kann.
	 *
	 * @return <code>False</code>, wenn das Schiff kein SRS hat oder gelandet ist. <code>True</code> ansonsten.
	 */
	public boolean canUseSrs() {
		return getTypeData().hasSrs() && !isLanded();
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

		return this.getWeapons() < 100;

	}

	/**
	 * Gibt an, ob das Schiff auf einem anderen Schiff gelandet ist.
	 *
	 * @return <code>true</code>, wenn das Schiff gelandet ist, sonst <code>false</code>
	 */
	public boolean isLanded()
	{
		return getDocked() != null && getDocked().startsWith("l");
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

	/**
	 * @return Die Bilanz des Schiffes.
	 */
	public int getBalance()
	{
		if(isLanded())
		{
			return 0;
		}

		return getTypeData().getReCost();
	}

	public int getUnitBalance() {
		if(isLanded() || getUnitCargo() == null)
		{
			return 0;
		}

		return getUnitCargo().getRE();
	}

    /**
     * Gibt zurueck, ob dieses Schiff eine Reparatur benötigt und diese durchführen kann.
     * @return <code>true</code>, falls das Schiff eine Reparatur benötigt
     */
    public boolean needRepair()
    {
        if(hasFlag(Ship.FLAG_RECENTLY_REPAIRED))
        {
            return false;
        }
        if(getBattle() != null)
        {
            return false;
        }
        ShipTypeData type = getTypeData();
		return getHull() < type.getHull() ||
				getAblativeArmor() < type.getAblativeArmor() ||
				getEngine() < 100 ||
				getSensors() < 100 ||
				getWeapons() < 100 ||
				getComm() < 100;
	}

	/**
	 *
	 * @return The effective scan range caused by sensors subsystem
	 */
	public int getEffectiveScanRange() {
		double scanrange = this.getTypeData().getSensorRange() * ( this.getSensors()/100d);
		return (int) Math.floor(scanrange);
	}

	/**
	 * Gibt an, ob das Schiff selbstzerstoert werden kann.
	 *
	 * @return <code>true</code>, wenn das Schiff nicht selbstzerstoert werden kann, sonst <code>false</code>
	 */
	public boolean isNoSuicide()
	{
		return this.getStatus().contains("nosuicide");
	}

	/**
	 * @return A factor for the energy costs.
	 */
	public int getAlertEnergyCost()
	{
		return (int)Math.ceil(this.getTypeData().getRm() * alarm.getEnergiekostenFaktor());
	}

    /**
     * Prüft, ob das Schiff ein Statusflag hat.
     *
     * @param flag Das Flag auf das geprüft wird.
     * @return <code>true</code>, wenn es das Flag gibt, <code>false</code> ansonsten.
     */
    public boolean hasFlag(int flag)
    {
        ShipFlag shipFlag = new ShipFlag(flag, this, -1);
        return flags.contains(shipFlag);
    }

	public Set<ShipFlag> getFlags() {
		return flags;
	}

	/**
     * Entfernt das Flag vom Schiff.
     * Es ist irrelevant, ob das Schiff das Flag hatte.
     *
     * @param flag Das zu entfernende Flag.
     */
    public void deleteFlag(int flag)
    {
        ShipFlag shipFlag = getFlag(flag);

        if(shipFlag != null)
        {
            flags.remove(shipFlag);

            org.hibernate.Session db = ContextMap.getContext().getDB();
            db.delete(shipFlag);
        }
    }

    public ShipFlag getFlag(int flag)
    {
        ShipFlag shipFlag = null;
        //Not really efficient, but we don't plan to have too many flags
        for(ShipFlag candidate: flags)
        {
            if(candidate.getFlagType() == flag)
            {
                shipFlag = candidate;
                break;
            }
        }

        return shipFlag;
    }

    /**
     * Gibt die Historieninformationen zum Schiff zurueck.
     * @return Die Historieninformationen
     */
    public ShipHistory getHistory()
    {
    	return this.history;
    }

	/**
	 * Stationiert einen Offizier auf dem Schiff.
	 * @param offizier Der Offizier
	 */
	public void onOffizierStationiert(Offizier offizier)
	{
		this.offiziere.add(offizier);
	}

	/**
	 * Callback wenn ein Offizier von Schiff entfernt wird.
	 * @param offizier Der Offizier
	 */
	public void onOffizierEntfernt(Offizier offizier)
	{
		this.offiziere.remove(offizier);
	}

	/**
	 * Gibt alle auf dem Schiff stationierten Offiziere zurueck.
	 * @return Die Offiziere
	 */
	public Set<Offizier> getOffiziere()
	{
		return this.offiziere;
	}

	/**
	 * Gibt den kommandierenden Offizier des Schiffes zurueck.
	 * @return Der Offizier
	 */
	public Offizier getOffizier() {
		if( this.getTypeData().getSize() <= ShipType.SMALL_SHIP_MAXSIZE &&
				this.getTypeData().getShipClass() != ShipClasses.RETTUNGSKAPSEL ) {
			return null;
		}

		Offizier offi = null;
		for( Offizier aoffi : this.offiziere )
		{
			if( offi == null || offi.getRang() < aoffi.getRang() )
			{
				offi = aoffi;
			}
		}
		return offi;
	}
}
