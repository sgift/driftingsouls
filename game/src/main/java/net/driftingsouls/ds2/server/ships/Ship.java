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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.*;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.IffDeaktivierenItem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.*;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.units.ShipUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repraesentiert ein Schiff in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ships")
@BatchSize(size=50)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@org.hibernate.annotations.Table(
		appliesTo = "ships",
		indexes = {
				@Index(name="ship_coords", columnNames = {"system", "x", "y"}),
				@Index(name="ship_owner", columnNames = {"owner", "id"})
		}
)
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
	@JoinColumn(name="modules", nullable=true)
	@BatchSize(size=50)
	@NotFound(action = NotFoundAction.IGNORE)
	@ForeignKey(name="ships_fk_ships_modules")
	private ShipModules modules;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="owner", nullable=false)
	@BatchSize(size=50)
	@ForeignKey(name="ships_fk_users")
	private User owner;

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="type", nullable=false)
	@BatchSize(size=50)
	@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
	@ForeignKey(name="ships_type_fk")
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
	@Index(name = "ship_status")
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
	@Index(name = "ship_docked")
	private String docked;

	@Enumerated(EnumType.ORDINAL)
	private Alarmstufe alarm;

	@OneToOne(fetch=FetchType.LAZY)
	@BatchSize(size=100)
	@JoinColumn
	@ForeignKey(name="ships_fk_schiff_einstellungen")
	private SchiffEinstellungen einstellungen;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="fleet", nullable=true)
	@BatchSize(size=50)
	@ForeignKey(name="ships_fk_ship_fleets")
	private ShipFleet fleet;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="battle", nullable=true)
	@BatchSize(size=50)
	@ForeignKey(name="ships_fk_battles")
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
	public UnitCargo getUnits() {
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
	public void setUnits(UnitCargo unitcargo) {
		UnitCargo newCargo = this.getUnits();
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

	private static final int MANGEL_TICKS = 9;

	/**
	 * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
	 * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
	 * @return der neue Status-String
	 */
	public String recalculateShipStatus() {
		return this.recalculateShipStatus(true);
	}

	/**
	 * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
	 * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
	 * Die Berechnung des Nahrungsverbrauchs ist dabei optional.
	 * @param nahrungPruefen <code>true</code>, falls der Nahrungsverbrauch geprueft werden soll
	 * @return der neue Status-String
	 */
	public String recalculateShipStatus(boolean nahrungPruefen) {
		if( this.id < 0 ) {
			throw new UnsupportedOperationException("recalculateShipStatus kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		ShipTypeData type = this.getTypeData();

		Cargo cargo = this.cargo;

		List<String> status = new ArrayList<>();

		// Alten Status lesen und ggf Elemente uebernehmen
		String[] oldstatus = StringUtils.split(this.status, ' ');

		if( oldstatus.length > 0 ) {
			for (String astatus : oldstatus)
			{
				// Aktuelle Statusflags rausfiltern
				if ("disable_iff".equals(astatus) ||
						"mangel_reaktor".equals(astatus) ||
						"offizier".equals(astatus) ||
						"nocrew".equals(astatus))
				{
					continue;
				}

				if (nahrungPruefen && "mangel_nahrung".equals(astatus))
				{
					continue;
				}
				status.add(astatus);
			}
		}

		// Treibstoffverbrauch berechnen
		if( type.getRm() > 0 ) {
			long ep = cargo.getResourceCount( Resources.URAN ) * type.getRu() +
			cargo.getResourceCount( Resources.DEUTERIUM ) * type.getRd() +
			cargo.getResourceCount( Resources.ANTIMATERIE ) * type.getRa();
			long er = ep/type.getRm();

			int turns = 2;
			if( (this.alarm != Alarmstufe.GREEN) && (type.getShipClass() != ShipClasses.GESCHUETZ) ) {
				turns = 4;
			}

			if( er <= MANGEL_TICKS/turns ) {
				status.add("mangel_reaktor");
			}
		}

		// Die Items nach IFF und Hydros durchsuchen

		if( cargo.getItemOfType(IffDeaktivierenItem.class) != null ) {
			status.add("disable_iff");
		}

		// Ist ein Offizier an Bord?
		Offizier offi = getOffizier();
		if( offi != null ) {
			status.add("offizier");
		}

		if( nahrungPruefen && lackOfFood() ) {
			status.add("mangel_nahrung");
		}

		this.status = Common.implode(" ", status);

		// Ueberpruefen, ob ein evt vorhandener Werftkomplex nicht exisitert.
        // Oder ein Link zu einem Asteroiden resettet werden muss.
		if( type.getWerft() != 0 ) {
			ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
				.setEntity("ship", this)
				.uniqueResult();

			if(werft != null) {
                if (werft.getKomplex() != null) {
                    werft.getKomplex().checkWerftLocations();
                }
                if(werft.isLinked()) {
                    if(!werft.getLinkedBase().getLocation().sameSector(werft.getLinkedBase().getSize(), getLocation(), 0)) {
                        werft.resetLink();
                    }
                }
            }
			else
			{
				log.error("Das Schiff "+this.id+" besitzt keinen Werfteintrag");
			}
		}

		return this.status;
	}

	/**
	 * Gibt das erstbeste Schiff im Sektor zurueck, dass als Versorger fungiert und noch Nahrung besitzt.
	 * @return Das Schiff
	 */
	private Ship getVersorger()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (Ship)db.createQuery("select s from Ship as s left join s.modules m" +
								" where (s.shiptype.versorger!=false or m.versorger!=false)" +
								" and s.owner=:owner and s.system=:sys and s.x=:x and s.y=:y and s.nahrungcargo > 0 and s.einstellungen.isfeeding != false " +
								"ORDER BY s.nahrungcargo DESC")
								.setEntity("owner", this.owner)
								.setInteger("sys", this.system)
								.setInteger("x", this.x)
								.setInteger("y", this.y)
								.setMaxResults(1)
								.uniqueResult();
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
	protected void setEinstellungen(SchiffEinstellungen einstellungen)
	{
		this.einstellungen = einstellungen;
	}

	private long getSectorTimeUntilLackOfFood()
	{
		Context context = ContextMap.getContext();

		org.hibernate.Session db = context.getDB();

		Object unitsnahrung = db.createQuery("select sum(e.amount*e.unittype.nahrungcost) from ShipUnitCargoEntry as e where e.schiff.system=:system and e.schiff.x=:x and e.schiff.y=:y and e.schiff.owner = :user")
									.setInteger("system", this.system)
									.setInteger("x", this.x)
									.setInteger("y", this.y)
									.setEntity("user", this.owner)
									.iterate()
									.next();

		// Die bloeden Abfragen muessen sein weil die Datenbank meint NULL anstatt 0 zurueckgeben zu muessen.
		double unitstofeed = 0;
		if(unitsnahrung != null)
		{
			unitstofeed = (Double)unitsnahrung;
		}

		Object crewnahrung = db.createQuery("select sum(crew) from Ship where system=:system and x=:x and y=:y and owner=:user")
							.setInteger("system", this.system)
							.setInteger("x", this.x)
							.setInteger("y", this.y)
							.setEntity("user", this.owner)
							.iterate()
							.next();

		long crewtofeed = 0;
		if( crewnahrung != null)
		{
			crewtofeed = (Long)crewnahrung;
		}

		long nahrungtofeed = (long)Math.ceil(unitstofeed + crewtofeed/10.0);

		if(nahrungtofeed == 0)
		{
			return Long.MAX_VALUE;
		}

		Object versorger = db.createQuery("select sum(s.nahrungcargo) from Ship as s left join s.modules m " +
								" where (s.shiptype.versorger!=false or m.versorger!=false)" +
								" and s.owner=:user and s.system=:system and s.x=:x and s.y=:y and s.einstellungen.isfeeding != false")
						.setInteger("system", this.system)
						.setInteger("x", this.x)
						.setInteger("y", this.y)
						.setEntity("user", this.owner)
						.iterate()
						.next();

		long versorgernahrung = 0;
		if( versorger != null)
		{
			versorgernahrung = (Long)versorger;
		}

		List<Base> bases = Common.cast(db.createQuery("from Base where owner=:user and system=:system and x=:x and y=:y and isfeeding=true")
						.setEntity("user", this.owner)
						.setInteger("system", this.system)
						.setInteger("x", this.x)
						.setInteger("y", this.y)
						.list());

		int basenahrung = 0;
		for(Base base : bases)
		{
			basenahrung += base.getCargo().getResourceCount(Resources.NAHRUNG);
		}

		return (versorgernahrung+basenahrung) / nahrungtofeed;
	}

	private boolean lackOfFood() {
		long ticks = timeUntilLackOfFood();
		return ticks <= MANGEL_TICKS && ticks >= 0;
	}

	private boolean isBaseInSector() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		List<?> bases = db.createQuery("from Base where owner=:owner and system=:sys and x=:x and y=:y")
							.setEntity("owner", this.owner)
							.setInteger("sys", this.system)
							.setInteger("x", this.x)
							.setInteger("y", this.y)
							.list();

		return bases.size() > 0;
	}

	private long timeUntilLackOfFood() {
		//Basisschiff beruecksichtigen
		Ship baseShip = getBaseShip();
		if( baseShip != null ) {
			return baseShip.timeUntilLackOfFood();
		}

		int foodConsumption = getFoodConsumption();
		if( foodConsumption <= 0 ) {
			return Long.MAX_VALUE;
		}

		Ship versorger = getVersorger();

		//Den Nahrungsverbrauch berechnen - Ist nen Versorger da ists cool
		if( versorger != null || isBaseInSector()) {
			// Sind wir selbst ein Versorger werden wir ja mit berechnet.
			if( (getTypeData().isVersorger() || getBaseType().isVersorger()) && getEinstellungen().isFeeding())
			{
				return getSectorTimeUntilLackOfFood();
			}
			// Ansonsten schmeissen wir noch das drauf was selbst da ist.
			return getSectorTimeUntilLackOfFood() + this.nahrungcargo / foodConsumption;
		}

		// OK muss alles selbst haben *schnueff*
		return this.nahrungcargo / foodConsumption;
	}

	/**
	 * Calculates the amount of food a ship consumes.
	 * The calculation is done with respect to hydros / shiptype.
	 * The calculation is done with respect to docked ships
	 *
	 * @return Amount of food this ship consumes
	 */
	public int getFoodConsumption() {
		if(this.getOwner().hasFlag(UserFlag.NO_FOOD_CONSUMPTION) || this.isLanded() || this.isDocked())
		{
			return 0;
		}
		ShipTypeData shiptype = this.getTypeData();
		int scaledcrew = this.getScaledCrew();
		int scaledunits = this.getScaledUnits();
		int hydro = shiptype.getHydro();
		int dockedcrew = 0;
		int dockedunits = 0;
		int dockedhydro = 0;

		if( shiptype.getJDocks() > 0 || shiptype.getADocks() > 0 ) {
			//Angehaengte Schiffe beruecksichtigen
			for (Ship dockedShip : getGedockteUndGelandeteSchiffe())
			{
				dockedcrew += dockedShip.getScaledCrew();
				dockedunits += dockedShip.getScaledUnits();
				dockedhydro += dockedShip.getTypeData().getHydro();
			}
		}

		return (int)Math.ceil((scaledcrew+dockedcrew)/10.0)+scaledunits+dockedunits-hydro-dockedhydro;
	}

	/**
	 * Returns the crew scaled by a factor according to alert.
	 * Ships with active alert consume more food.
	 *
	 * @return Crew scaled by a factor according to shiptype.
	 */
	private int getScaledCrew() {
		double scale = alarm.getAlertScaleFactor();
		return (int)Math.ceil(this.crew*scale);
	}

	/**
	 * Returns the units scaled by a factor according to alert.
	 * Ships with active alert consume more food.
	 *
	 * @return Units scaled by a factor according to shiptype.
	 */
	private int getScaledUnits() {
		if(getUnits() != null)
		{
			double scale = alarm.getAlertScaleFactor();
			return (int)Math.ceil(this.getUnits().getNahrung()*scale);
		}
		return 0;
	}

	/**
	 * Gibt die Moduleintraege des Schiffes zurueck.
	 * @return Eine Liste von Moduleintraegen
	 */
	public ModuleEntry[] getModules() {
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

	private int berecheNeuenStatusWertViaDelta(int currentVal, int oldMax, int newMax)
	{
		int delta = newMax - oldMax;
		currentVal += delta;
		if( currentVal > newMax ) {
			currentVal = newMax;
		}
		else if( currentVal < 0 ) {
			currentVal = 0;
		}
		return currentVal;
	}

	/**
	 * Aktualisiert die Schiffswerte nach einer Aenderung an den Typendaten des Schiffs.
	 * @param oldshiptype Die Schiffstypendaten des Schiffstyps vor der Modifikation
	 * @return Der Cargo, der nun nicht mehr auf das Schiff passt
	 */
	public Cargo postUpdateShipType(ShipTypeData oldshiptype) {
		ShipTypeData shiptype = this.getTypeData();

		if( hull != shiptype.getHull() ) {
			this.hull = berecheNeuenStatusWertViaDelta(hull, oldshiptype.getHull(), shiptype.getHull());
		}

		if( shields != shiptype.getShields() ) {
			this.shields = berecheNeuenStatusWertViaDelta(shields, oldshiptype.getShields(), shiptype.getShields());
		}

		if( ablativeArmor != shiptype.getAblativeArmor() ) {
			this.ablativeArmor = berecheNeuenStatusWertViaDelta(ablativeArmor, oldshiptype.getAblativeArmor(), shiptype.getAblativeArmor());
		}

		if( ablativeArmor > shiptype.getAblativeArmor() ) {
			this.ablativeArmor = shiptype.getAblativeArmor();
		}

		if( e != shiptype.getEps() ) {
			this.e = berecheNeuenStatusWertViaDelta(e, oldshiptype.getEps(), shiptype.getEps());
		}

		if( crew != shiptype.getCrew() ) {
			this.crew = berecheNeuenStatusWertViaDelta(crew, oldshiptype.getCrew(), shiptype.getCrew());
		}

		Cargo cargo = new Cargo();
		if( this.cargo.getMass() > shiptype.getCargo() ) {
			Cargo newshipcargo = this.cargo.cutCargo( shiptype.getCargo() );
			cargo.addCargo( this.cargo );
			this.cargo = newshipcargo;
		}

		StringBuilder output = MESSAGE.get();

		org.hibernate.Session db = ContextMap.getContext().getDB();

		int jdockcount = (int)this.getLandedCount();
		if( jdockcount > shiptype.getJDocks() ) {
			int count = 0;

			// toArray(T[]) fuehrt hier leider zu Warnungen...
			Ship[] undockarray = new Ship[jdockcount-shiptype.getJDocks()];
			for( Ship lship : this.getLandedShips() ) {
				undockarray[count++] = lship;
				if( count >= undockarray.length )
				{
					break;
				}
			}

			output.append(jdockcount - shiptype.getJDocks()).append(" gelandete Schiffe wurden gestartet\n");

			this.start(undockarray);
		}

		int adockcount = (int)this.getDockedCount();
		if( adockcount > shiptype.getADocks() ) {
			int count = 0;

			// toArray(T[]) fuehrt hier leider zu Warnungen...
			Ship[] undockarray = new Ship[adockcount-shiptype.getADocks()];
			for( Ship lship : getDockedShips() ) {
				undockarray[count++] = lship;
				if( count >= undockarray.length )
				{
					break;
				}
			}

			output.append(adockcount - shiptype.getADocks()).append(" extern gedockte Schiffe wurden abgedockt\n");

			this.dock(Ship.DockMode.UNDOCK, undockarray);
		}

		if( shiptype.getWerft() == 0 ) {
			db.createQuery("delete from ShipWerft where ship=:ship")
					.setEntity("ship", this)
					.executeUpdate();
		}
		else {
			ShipWerft w = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
					.setEntity("ship", this)
					.uniqueResult();
			if( w == null ) {
				w = new ShipWerft(this);
				db.persist(w);
			}
		}

		return cargo;
	}

	/**
	 * Fuegt ein Modul in das Schiff ein.
	 * @param slot Der Slot, in den das Modul eingebaut werden soll
	 * @param moduleid Die Typen-ID des Modultyps
	 * @param data Weitere Daten, welche das Modul identifizieren
	 */
	public void addModule(int slot, ModuleType moduleid, String data )
	{
		addModule(slot, moduleid, data, true);
	}

	private void addModule(int slot, ModuleType moduleid, String data, boolean validate ) {
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

		List<ModuleEntry> moduletbl = new ArrayList<>(Arrays.asList(getModules()));

		//check modules

		//rebuild
		moduletbl.add(new ModuleEntry(slot, moduleid, data ));

		ShipTypeData type = this.shiptype;

		Map<Integer,String[]>slotlist = new HashMap<>();
		String[] tmpslotlist = StringUtils.splitPreserveAllTokens(type.getTypeModules(), ';');
		for (String aTmpslotlist : tmpslotlist)
		{
			String[] aslot = StringUtils.splitPreserveAllTokens(aTmpslotlist, ':');
			slotlist.put(Integer.parseInt(aslot[0]), aslot);
		}

		List<Module> moduleobjlist = new ArrayList<>();
		List<String> moduleSlotData = new ArrayList<>();

		for (ModuleEntry module : moduletbl)
		{
			if (module.getModuleType() != null)
			{
				Module moduleobj = module.createModule();
				if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()).length > 2))
				{
					moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
				}
				moduleobjlist.add(moduleobj);

				moduleSlotData.add(module.serialize());
			}
		}

		for( int i=0; i < moduleobjlist.size(); i++ ) {
			type = moduleobjlist.get(i).modifyStats( type, moduleobjlist );
		}

		shipModules.setModules(Common.implode(";",moduleSlotData));
		writeTypeToShipModule(shipModules, type);

		if( validate )
		{
			ueberpruefeGedocktGelandetAnzahl();
		}
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
		shipModules.setNahrungCargo(type.getNahrungCargo());
		shipModules.setHeat(type.getHeat());
		shipModules.setCrew(type.getCrew());
		shipModules.setMaxUnitSize(type.getMaxUnitSize());
		shipModules.setUnitSpace(type.getUnitSpace());
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
		shipModules.setFlags(type.getFlags().stream().map(ShipTypeFlag::getFlag).collect(Collectors.joining(" ")));
		shipModules.setWerft(type.getWerft());
		shipModules.setOneWayWerft(type.getOneWayWerft());
		shipModules.setAblativeArmor(type.getAblativeArmor());
		shipModules.setSrs(type.hasSrs());
		shipModules.setMinCrew(type.getMinCrew());
		shipModules.setVersorger(type.isVersorger());
		shipModules.setLostInEmpChance(type.getLostInEmpChance());
	}

	/**
	 * Entfernt ein Modul aus dem Schiff.
	 * @param moduleEntry Der zu entfernende Moduleintrag
	 */
	public void removeModule( ModuleEntry moduleEntry ) {
		removeModule(moduleEntry, true);
	}

	private void removeModule( ModuleEntry moduleEntry, boolean validate ) {
		if( this.id < 0 ) {
			throw new UnsupportedOperationException("addModules kann nur bei Schiffen mit positiver ID ausgefuhert werden!");
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( this.modules == null ) {
			return;
		}

		ShipModules shipModules = this.modules;

		List<ModuleEntry> moduletbl = new ArrayList<>(Arrays.asList(getModules()));

		//check modules

		//rebuild
		ShipTypeData type = this.shiptype;

		Map<Integer,String[]>slotlist = new HashMap<>();
		String[] tmpslotlist = StringUtils.split(type.getTypeModules(), ';');
		for (String aTmpslotlist : tmpslotlist)
		{
			String[] aslot = StringUtils.split(aTmpslotlist, ':');
			slotlist.put(Integer.parseInt(aslot[0]), aslot);
		}

		List<Module> moduleobjlist = new ArrayList<>();
		List<String> moduleSlotData = new ArrayList<>();

		for (ModuleEntry module : moduletbl)
		{
			if (module.getModuleType() != null)
			{
				Module moduleobj = module.createModule();

				if (moduleobj.isSame(moduleEntry))
				{
					continue;
				}

                if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()) != null) && (slotlist.get(module.getSlot()).length > 2)) {
                    moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
                } else {
                    log.error(String.format("ship: %s - slot: %s - not contained in slot list", id, module.getSlot()));
                }
				moduleobjlist.add(moduleobj);

				moduleSlotData.add(module.serialize());
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

		if( validate )
		{
			ueberpruefeGedocktGelandetAnzahl();
		}
	}

	private void ueberpruefeGedocktGelandetAnzahl() {
		ShipTypeData type = this.getTypeData();

		List<Ship> dockshipList = getDockedShips();
		if( dockshipList.size() > type.getADocks() )
		{
			List<Ship> undock = dockshipList.subList(0, dockshipList.size()-type.getADocks());
			this.undock(undock.toArray(new Ship[0]));
		}

		List<Ship> jdockshipList = getLandedShips();
		if( jdockshipList.size() > type.getJDocks() )
		{
			List<Ship> undock = jdockshipList.subList(0, jdockshipList.size()-type.getJDocks());
			this.start(undock.toArray(new Ship[0]));
		}
	}

	/**
	 * Berechnet die durch Module verursachten Effekte des Schiffes neu.
	 */
	public void recalculateModules() {
		if( this.modules == null ) {
			return;
		}

		ShipModules shipModules = this.modules;

		List<ModuleEntry> moduletbl = new ArrayList<>(Arrays.asList(getModules()));

		//check modules

		//rebuild
		ShipTypeData type = this.shiptype;

		Map<Integer,String[]>slotlist = new HashMap<>();
		String[] tmpslotlist = StringUtils.split(type.getTypeModules(), ';');
		for (String aTmpslotlist : tmpslotlist)
		{
			String[] aslot = StringUtils.split(aTmpslotlist, ':');
			slotlist.put(Integer.parseInt(aslot[0]), aslot);
		}

		List<Module> moduleobjlist = new ArrayList<>();
		List<String> moduleSlotData = new ArrayList<>();

		for (ModuleEntry module : moduletbl)
		{
			if (module.getModuleType() != null)
			{
				Module moduleobj = module.createModule();

				if(moduleobj != null) {
					if ((module.getSlot() > 0) && (slotlist.get(module.getSlot()) != null) && (slotlist.get(module.getSlot()).length > 2)) {
						moduleobj.setSlotData(slotlist.get(module.getSlot())[2]);
					} else {
						log.error(String.format("ship: %s - slot: %s - not contained in slot list", id, module.getSlot()));
					}
					moduleobjlist.add(moduleobj);

					moduleSlotData.add(module.serialize());
				} else {
					log.error(String.format("ship: %s - module type: %s - unable to create module - skipping.", id, module.getModuleType()));
				}
			}
		}

		for (Module module : moduleobjlist) {
			type = module.modifyStats( type, moduleobjlist );
		}

		shipModules.setModules(Common.implode(";",moduleSlotData));
		writeTypeToShipModule(shipModules, type);

		ueberpruefeGedocktGelandetAnzahl();
	}

	/**
	 * Gibt eine Liste von booleans zurueck, welche angeben ob ein angegebener Sektor fuer den angegebenen Spieler
	 * unter Alarm steht, d.h. bei einem Einflug eine Schlacht gestartet wird.
	 * Die Reihenfolge der Liste entspricht der der uebergebenen Koordinaten. <code>true</code> kennzeichnet,
	 * dass der Sektor unter Alarm steht.
	 * @param user Der Spieler
	 * @param locs Die Positionen, die ueberprueft werden sollen
	 * @return Liste von Sektoren mit rotem Alarm
	 */
	public static Set<Location> getAlertStatus( User user, Location ... locs ) {
		Set<Location> results = new HashSet<>();

		Map<Location,List<Ship>> result = alertCheck(user, locs);

		for (Map.Entry<Location, List<Ship>> entry : result.entrySet())
		{
			if( !entry.getValue().isEmpty() )
			{
				results.add(entry.getKey());
			}
		}

		return results;
	}

	public static Map<Location,List<Ship>> alertCheck( User user, Location ... locs )
	{
		Set<Integer> xSektoren = new HashSet<>();
		Set<Integer> ySektoren = new HashSet<>();

		Map<Location,List<Ship>> results = new HashMap<>();
		Set<Location> locations = new HashSet<>();
		for(Location location: locs)
		{
			results.put(location, new ArrayList<>());
			locations.add(location);
			xSektoren.add(location.getX());
			ySektoren.add(location.getY());
		}

		if(locations.isEmpty())
		{
			return results;
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();


		List<Ship> ships = Common.cast(db.createQuery("from Ship s inner join fetch s.owner " +
				"where s.e > 0 and s.alarm!=:green and s.docked='' and " +
				"	s.crew!=0 and s.system=:system and s.owner!=:owner and " +
				"   (s.owner.vaccount=0 or s.owner.wait4vac>0) and " +
				"	s.x in (:xSektoren) and s.y in (:ySektoren)")
			.setParameter("green", Alarmstufe.GREEN)
			.setParameter("system", locs[0].getSystem())
			.setParameter("owner", user)
			.setParameterList("xSektoren", xSektoren)
			.setParameterList("ySektoren", ySektoren)
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
			Alarmstufe alert = ship.getAlarm();
			boolean attack = false;
			if(alert == Alarmstufe.RED)
			{
				if(relationlist.beziehungVon(owner) != User.Relation.FRIEND)
				{
					attack = true;
				}
				else if(relationlist.beziehungZu(owner) != User.Relation.FRIEND)
				{
					attack = true;
				}
			}
			else if(alert == Alarmstufe.YELLOW)
			{
				if(relationlist.beziehungVon(owner) == User.Relation.ENEMY)
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

		dockships = Arrays.stream(dockships)
				.filter(s -> s.getTypeData().hasFlag(ShipTypeFlag.JAEGER))
				.toArray(Ship[]::new);

		//No superdock for landing
		Ship[] help = performLandingChecks(false, dockships);
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
				.setParameter("fighter", ShipTypeFlag.JAEGER.getFlag())
				.list());

		if(ships.size() < dockships.length)
		{
			//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
			dockships = ships.toArray(new Ship[ships.size()]);
			errors = true;
		}

		long landedShips = getLandedCount();
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

		db.createQuery("update Ship s set docked=:docked where s in (:dockships) and battle is null")
			.setParameterList("dockships", dockships)
			.setParameter("docked", "l "+this.getId())
			.executeUpdate();

		// Die Query aktualisiert leider nicht die bereits im Speicher befindlichen Objekte...
		for( Ship ship : dockships ) {
			if ( ship.getBattle() == null ){
				ship.setDocked("l "+this.getId());
			}
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
				dockship.setLocation(this);
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
			if(aship.getBaseShipId() != this.getId())
			{
				//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
				continue;
			}

			aship.setDocked("");

			ShipTypeData type = aship.getTypeData();

			if( type.getShipClass() != ShipClasses.CONTAINER ) {
				continue;
		  	}
			gotmodule = true;

			ModuleEntry moduleEntry = new ModuleEntry(0, ModuleType.CONTAINER_SHIP, Integer.toString(aship.getId()));
			aship.removeModule( moduleEntry, false );
			this.removeModule( moduleEntry, false );
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

				if( (ashiptype.getShipClass() == ShipClasses.CONTAINER) && (cargo.getMass() > 0) )
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
	}

	/**
	 * Gibt die Liste aller an diesem Schiff (extern) gedockten Schiffe zurueck.
	 * @return Die Liste der Schiffe
	 */
	public List<Ship> getDockedShips()
	{
		if( this.getTypeData().getADocks() == 0 )
		{
			return new ArrayList<>();
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return Common.cast(db.createQuery("from Ship where id>0 and docked= :docked")
			.setString("docked", Integer.toString(this.id))
			.list());
	}

	/**
	 * Gibt die Liste aller auf diesem Schiff gelandeten Schiffe zurueck.
	 * @return Die Liste der Schiffe
	 */
	public List<Ship> getLandedShips()
	{
		if( this.getTypeData().getJDocks() == 0 )
		{
			return new ArrayList<>();
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return Common.cast(db.createQuery("from Ship where id>0 and docked= :docked")
				.setString("docked", "l " + this.id)
				.list());
	}

	/**
	 * Gibt die Liste aller an diesem Schiff angedockten und auf diesem gelandeten Schiffe zurueck.
	 * @return Die Schiffe
	 */
	public List<Ship> getGedockteUndGelandeteSchiffe() {
		if( this.getTypeData().getJDocks() == 0 && this.getTypeData().getADocks() == 0 ) {
			return new ArrayList<>();
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();
		return Common.cast(db.createQuery("from Ship where id>0 and docked in (:docked,:landed)")
				.setString("docked", Integer.toString(this.id))
				.setString("landed", "l "+this.id)
				.list());
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


		boolean superdock = owner.hasFlag(UserFlag.SUPER_DOCK);
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		StringBuilder outputbuffer = MESSAGE.get();

		Ship[] help = performLandingChecks(superdock, dockships);
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

		long dockedShips = getDockedCount();
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

			if( type.getShipClass() != ShipClasses.CONTAINER )
			{
				continue;
			}

			Cargo dockcargo = aship.getCargo();
			cargo.addCargo( dockcargo );

			if( !dockcargo.isEmpty() )
			{
				aship.setCargo(emptycargo);
			}

			aship.addModule( 0, ModuleType.CONTAINER_SHIP, aship.getId()+"_"+(-type.getCargo()), false );
			this.addModule( 0, ModuleType.CONTAINER_SHIP, aship.getId()+"_"+type.getCargo(), false );
		}

		this.cargo = cargo;

		return errors;
	}

	/**
	 * Checks, die sowohl fuers landen, als auch fuers andocken durchgefuehrt werden muessen.
	 *
	 * @param superdock <code>true</code>, falls im Superdock-Modus
	 * 			(Keine Ueberpruefung von Groesse/Besitzer) gedockt/gelandet werden soll
	 * @param dockships Schiffe auf die geprueft werden soll.
	 * @return Die Liste der zu dockenden/landenden Schiffe
	 */
	private Ship[] performLandingChecks(boolean superdock, Ship ... dockships)
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

		//Check already docked
		ships = Common.cast(
			db.createQuery("from Ship s where docked='' and s in (:dockships)")
				.setParameterList("dockships", dockships)
				.list());

		if(ships.size() < dockships.length)
		{
			//TODO: Hackversuch - schweigend ignorieren, spaeter loggen
			dockships = ships.toArray(new Ship[ships.size()]);

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
	 * Entfernt das Schiff aus der Datenbank.
	 */
	public void destroy() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		// Checken wir mal ob die Flotte danach noch bestehen darf....
		if( this.fleet != null ) {
			long fleetcount = (Long)db.createQuery("select count(*) from Ship where fleet=:fleet")
			.setInteger("fleet", fleet.getId())
			.iterate().next();
			if( fleetcount <= 2 ) {
				final ShipFleet fleet = this.fleet;

				final Iterator<?> shipIter = db.createQuery("from Ship where fleet=:fleet")
					.setEntity("fleet", this.fleet)
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
		for (Task task : tasks)
		{
			taskmanager.removeTask(task.getTaskID());
		}

		// Und nun loeschen wir es...
		this.flags.clear();

		db.createQuery("delete from Offizier where stationiertAufSchiff=:dest")
			.setEntity("dest", this)
			.executeUpdate();

		db.createQuery("delete from Jump where ship=:id")
			.setEntity("id", this)
			.executeUpdate();

		ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
			.setInteger("ship", this.id)
			.uniqueResult();

		if( werft != null ) {
			werft.destroy();
		}

		// Delete Trade Limits if necessary
		if (this.isTradepost())
		{
			db.createQuery("delete from ResourceLimit where ship=:ship").setParameter("ship", this).executeUpdate();
			db.createQuery("delete from SellLimit where ship=:ship").setParameter("ship", this).executeUpdate();
		}

		if(units != null)
		{
			units.forEach(db::delete);
		}

		if( this.einstellungen != null )
		{
			db.delete(this.einstellungen);
			this.einstellungen = null;
		}

		db.flush(); //Damit auch wirklich alle Daten weg sind und Hibernate nicht auf dumme Gedanken kommt *sfz*
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

		if( this.getTypeData().hasFlag(ShipTypeFlag.NICHT_UEBERGEBBAR) ) {
			MESSAGE.get().append("Sie können dieses Schiff nicht &uuml;bergeben.");
			return true;
		}

		if( newowner == null ) {
			MESSAGE.get().append("Der angegebene Spieler existiert nicht");
			return true;
		}

		if( (newowner.getVacationCount() != 0) && (newowner.getWait4VacationCount() == 0) ) {
			MESSAGE.get().append("Sie k&ouml;nnen keine Schiffe an Spieler &uuml;bergeben, welche sich im Vacation-Modus befinden");
			return true;
		}

		if( newowner.hasFlag( UserFlag.NO_SHIP_CONSIGN ) ) {
			MESSAGE.get().append("Sie k&ouml;nnen diesem Spieler keine Schiffe &uuml;bergeben");
			return true;
		}

		if(this.status.contains("noconsign")) {
			MESSAGE.get().append("Die '").append(this.name).append("' (").append(this.id).append(") kann nicht &uuml;bergeben werden");
			return true;
		}

		if( !testonly ) {
			this.history.addHistory("&Uuml;bergeben am [tick="+ContextMap.getContext().get(ContextCommon.class).getTick()+"] an "+newowner.getName()+" ("+newowner.getId()+")");

			this.removeFromFleet();
			this.owner = newowner;

			if(this.isLanded())
			{
				this.getBaseShip().start(this);
			}

			if(this.isDocked())
			{
				this.getBaseShip().undock(this);
			}

			for( Offizier offi : this.getOffiziere() )
			{
				offi.setOwner(newowner);
			}

			if( getTypeData().getWerft() != 0 ) {
				ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=:shipid")
					.setEntity("shipid", this)
					.uniqueResult();

				if(werft != null)
				{
					if(werft.isLinked())
					{
						werft.resetLink();
					}

					if(werft.getKomplex() != null)
					{
						werft.removeFromKomplex();
					}
				}
			}

			if( this.isTradepost() )
			{
				// Um exploits zu verhindern die Sichtbarkeit des Haandelsposten
				// auf nichts stellen
				SchiffEinstellungen einstellungen = this.getEinstellungen();
				einstellungen.setShowtradepost(TradepostVisibility.NONE);
				einstellungen.persistIfNecessary(this);
			}
		}

		StringBuilder message = MESSAGE.get();
		List<Ship> s = getGedockteUndGelandeteSchiffe();
		for (Ship aship : s)
		{
			int oldlength = message.length();
			boolean tmp = aship.consign(newowner, testonly);
			if (tmp && !testonly)
			{
				this.dock(aship.isLanded() ? DockMode.START : DockMode.UNDOCK, aship);
			}

			if ((oldlength > 0) && (oldlength != message.length()))
			{
				message.insert(oldlength - 1, "<br />");
			}
		}

		Cargo cargo = this.cargo;
		List<ItemCargoEntry> itemlist = cargo.getItems();
		for( ItemCargoEntry item : itemlist ) {
			Item itemobject = item.getItem();
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
	 * Gibt die Typen-Daten des angegebenen Schiffs bzw Schifftyps zurueck.
	 * @param shiptype Die ID des Schiffs bzw des Schifftyps
	 * @return die Typen-Daten
	 */
	public static ShipTypeData getShipType( int shiptype ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return (ShipType)db.get(ShipType.class, shiptype);
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
	 * Gibt die Anzahl der an externen Docks gedockten Schiffe zurueck.
	 * @return Die Anzahl
	 */
	public long getDockedCount() {
		if( getTypeData().getADocks() == 0 ) {
			return 0;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (Long)db.createQuery("select count(*) from Ship where id>0 AND docked=:docked")
			.setString("docked", Integer.toString(this.id))
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

		return (Long)db.createQuery("select count(*) from Ship where id>0 AND docked=:landed")
			.setString("landed", "l "+this.id)
			.iterate().next();
	}

	/**
	 * Gibt die Anzahl der auf diesem Schiff gedockten und gelandeten Schiffe zurueck.
	 * @return Die Anzahl
	 */
	public long getAnzahlGedockterUndGelandeterSchiffe() {
		if( getTypeData().getJDocks() == 0 && getTypeData().getADocks() == 0 ) {
			return 0;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (Long)db.createQuery("select count(*) from Ship where id>0 AND docked in (:landed,:docked)")
				.setString("landed", "l " + this.id)
				.setString("docked", Integer.toString(this.id))
				.iterate().next();
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
	 * Transfers crew from the ship to an asteroid.
	 *
	 * @param base Base that sends the crew.
	 * @param amount People that should be transfered.
	 * @return People that where transfered.
	 */
	public int transferCrew(Base base, int amount) {
		//Check ship position
		if(!getLocation().sameSector(0, base, base.getSize()))
		{
			return 0;
		}

		setCrew(getCrew() - amount);
		base.setBewohner(base.getBewohner() + amount);

		recalculateShipStatus();

		return amount;
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
		if(isLanded() || getUnits() == null)
		{
			return 0;
		}

		return getUnits().getRE();
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
	 * @return Die Nahrungsbilanz des Schiffes.
	 */
	public int getNahrungsBalance()
	{
		return getFoodConsumption();
	}

	/**
	 * @return A factor for the energy costs.
	 */
	public int getAlertEnergyCost()
	{
		return (int)Math.ceil(this.getTypeData().getRm() * alarm.getEnergiekostenFaktor());
	}

	/**
	 * returns wether the tradepost is visible or not.
	 * 0 everybody is able to see the tradepost.
	 * 1 everybody except enemys is able to see the tradepost.
	 * 2 every friend is able to see the tradepost.
	 * 3 the own allymembers are able to see the tradepost.
	 * 4 nobody except owner is able to see the tradepost.
	 * @param observer the user who watches the tradepostlist.
	 * @param relationlist the relations of the observer.
	 * @return boolean if tradepost is visible
	 */
	public boolean isTradepostVisible(User observer, User.Relations relationlist)
	{
		TradepostVisibility tradepostvisibility = this.getEinstellungen().getShowtradepost();
		int ownerid = this.getOwner().getId();
		int observerid = observer.getId();
		switch (tradepostvisibility)
		{
	        case ALL:
	        	 return true;
	        case NEUTRAL_AND_FRIENDS:
	        	// check whether we are an enemy of the owner
				return relationlist.beziehungVon(this.owner) != User.Relation.ENEMY;
			case FRIENDS:
	        	// check whether we are a friend of the owner
				return (relationlist.beziehungVon(this.owner) == User.Relation.FRIEND) || (ownerid == observerid);
			case ALLY:
	        	// check if we are members of the same ally and if the owner has an ally
				return ((this.getOwner().getAlly() != null) && observer.getAlly() != null && (this.getOwner().getAlly().getId() == observer.getAlly().getId())) || (ownerid == observerid);
			case NONE:
	        	// check if we are the owner of the tradepost
				return ownerid == observerid;
			default:
				// damn it, broken configuration, don't show the tradepost
	        	 return false;
		}
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

    /**
     * Fuegt dem Schiff ein neues Flag hinzu.
     * Wenn das Schiff das Flag bereits hat und die neue verbleibende Zeit groesser ist als die Alte wird sie aktualisiert.
     *
     * @param flag Flagcode.
     * @param remaining Wieviele Ticks soll das Flag bestand haben? -1 fuer unendlich.
     */
    public void addFlag(int flag, int remaining)
    {
        ShipFlag oldFlag = getFlag(flag);

        if(oldFlag != null)
        {
            if(oldFlag.getRemaining() != -1 && oldFlag.getRemaining() < remaining)
            {
                oldFlag.setRemaining(remaining);
            }
        }
        else
        {
            ShipFlag shipFlag = new ShipFlag(flag, this, remaining);

            org.hibernate.Session db = ContextMap.getContext().getDB();
            db.persist(shipFlag);

            flags.add(shipFlag);
        }
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

    private ShipFlag getFlag(int flag)
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
     * Laesst das Schiff im angegebenen Nebel Deuterium fuer einen bestimmten
     * Energiebetrag sammeln. Falls der Energiebetrag kleiner 0 ist wird
     * die komplette Schiffsenergie fuer das Sammeln verwendet.
     * @param nebel Der Nebel
     * @param energie Der zu verwendende Energiebetrag, falls negativ
     * die gesammte Schiffsenergie
     * @return Die Menge des gesammelten Deuteriums
     */
    public long sammelDeuterium(Nebel nebel, long energie)
    {
    	if( nebel == null || !nebel.getLocation().sameSector(0, this, 0) )
    	{
			return 0;
		}
    	if( !nebel.getType().isDeuteriumNebel() )
    	{
    		return 0;
    	}
    	ShipTypeData type = this.getTypeData();
    	if( this.crew < (type.getCrew()/2) ) {
    		return 0;
    	}
        if( type.getDeutFactor() <= 0 )
    	{
    		return 0;
    	}
    	if( energie > this.e || energie < 0 )
    	{
			energie = this.e;
		}
		Cargo shipCargo = this.cargo;
		long cargo = shipCargo.getMass();

		long deutfactor = type.getDeutFactor();
		deutfactor = nebel.getType().modifiziereDeutFaktor(deutfactor);

		if( (energie * deutfactor)*Cargo.getResourceMass(Resources.DEUTERIUM, 1) > (type.getCargo() - cargo) ) {
			energie = (type.getCargo()-cargo)/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ));
		}

		long saugdeut = energie * deutfactor;

		if( saugdeut > 0 ) {
			shipCargo.addResource( Resources.DEUTERIUM, saugdeut );

			this.e = (int)(this.e-energie);
			this.recalculateShipStatus();
		}
		return saugdeut;
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
