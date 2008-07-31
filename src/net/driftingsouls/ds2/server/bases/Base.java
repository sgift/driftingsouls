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
package net.driftingsouls.ds2.server.bases;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.Transfer;
import net.driftingsouls.ds2.server.cargo.Transfering;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.ships.Ship;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.classic.Lifecycle;

/**
 * <p>Repraesentiert eine Basis in DS</p>
 * Hinweis: Das setzen von Werten aktuallisiert nicht die Datenbank!
 * 
 * @author Christopher Jung
 */
@Entity
@Table(name="bases")
public class Base implements Cloneable, Lifecycle, Locatable, Transfering {
	@Id @GeneratedValue
	private int id;
	private String name;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="owner", nullable=false)
	private User owner;
	private int x;
	private int y;
	private int system;
	private int bewohner;
	private int marines;
	private int arbeiter;
	@Column(name="e")
	private int energy;
	@Column(name="maxe")
	private int maxEnergy;
	@Type(type="cargo")
	private Cargo cargo;
	@Column(name="maxcargo")
	private long maxCargo;
	private int core;
	private int klasse;
	private int width;
	private int height;
	@Column(name="maxtiles")
	private int maxTiles;
	private int size;
	private String terrain;
	private String bebauung;
	private String active;
	@Column(name="coreactive")
	private int coreActive;
	@Column(name="autogtuacts")
	private String autoGtuActs;
	@Version
	private int version;
	
	@Transient
	private List<AutoGTUAction> autoGtuActsObj;
	@Transient
	private Integer[] terrainObj;
	@Transient
	private Integer[] bebauungObj;
	@Transient
	private Integer[] activeObj;
	
	/**
	 * Konstruktor
	 *
	 */
	public Base() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Basis
	 * @param loc Die Position
	 * @param owner Der Besitzer
	 */
	public Base(Location loc, User owner) {
		this.x = loc.getX();
		this.y = loc.getY();
		this.system = loc.getSystem();
		this.terrain = "";
		this.bebauung = "";
		this.active = "";
		this.cargo = new Cargo();
		this.autoGtuActs = "";
		this.owner = owner;
		this.name = "Leerer Asteroid";
	}
	
	/**
	 * Gibt die ID der Basis zurueck
	 * @return die ID der Basis
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * Gibt die Breite der Bauflaeche auf der Basis in Feldern zurueck
	 * @return Die Breite
	 */
	public int getWidth() {
		return this.width;
	}
	
	/**
	 * Setzt die Breite der Bauflaeche auf der Basis
	 * @param width Die Breite
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * Gibt die Hoehe der Bauflaeche auf der Basis in Feldern zurueck
	 * @return Die Hoehe
	 */
	public int getHeight() {
		return this.height;
	}
	
	/**
	 * Setzt die Hoehe der Bauflaeche auf der Basis
	 * @param height Die Hoehe
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	
	/**
	 * Gibt den Namen der Basis zurueck
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Gibt der Basis einen neuen Namen
	 * @param name Der neue Name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gibt den Besitzer zurueck.
	 * 
	 * @return Der Besitzer
	 */
	public User getOwner() {
		return this.owner;
	}
	
	/**
	 * Setzt den neuen Besitzer fuer die Basis
	 * @param owner Der neue Besitzer
	 */
	public void setOwner(User owner) {
		this.owner = owner;
	}
	
	/**
	 * Gibt die Terrain-Typen der einzelnen Basisfelder zurueck.
	 * @return Die Terraintypen der Felder
	 */
	public Integer[] getTerrain() {
		return this.terrainObj.clone();
	}
	
	/**
	 * Setzt das neue Terrain der Basis
	 * @param terrain Das neue Terrain
	 */
	public void setTerrain(final Integer[] terrain) {
		this.terrainObj = terrain;
		this.terrain = Common.implode("|", terrain);
	}
	
	/**
	 * Gibt die IDs der auf den einzelnen Feldern stehenden Gebaeude zurueck.
	 * Sollte auf einem Feld kein Gebaeude stehen ist die ID 0.
	 * 
	 * @return Die IDs der Gebaeude
	 */
	public Integer[] getBebauung() {
		return this.bebauungObj.clone();
	}
	
	/**
	 * Setzt die neue Bebauung der Basis
	 * @param bebauung Die neue Bebauung
	 */
	public void setBebauung(final Integer[] bebauung) {
		this.bebauungObj = bebauung;
		this.bebauung = Common.implode("|", bebauung);
	}
	
	/**
	 * Gibt an, auf welchen Feldern die Gebaeude aktiv (1) sind und auf welchen
	 * nicht (0)
	 * @return Aktivierungsgrad der Gebaeude auf den Feldern
	 */
	public Integer[] getActive() {
		return this.activeObj.clone();
	}
	
	/**
	 * Setzt den Aktivierungszustand aller Gebaeude. <code>1</code> bedeutet, dass das
	 * Gebaeude aktiv ist. <code>0</code>, dass das Gebaeude nicht aktiv ist.
	 * @param active Der neue Aktivierungszustand aller Gebaeude
	 */
	public void setActive(final Integer[] active) {
		this.activeObj = active;
		this.active = Common.implode("|", active);
	}
	
	/**
	 * Gibt den Cargo der Basis zurueck
	 * @return Der Cargo
	 */
	// TODO: UnmodifiableCargos zurueckgeben (zuerst alle Verwendungen checken und umbauen) 
	public Cargo getCargo() {
		return cargo;
	}
	
	/**
	 * Setzt den Cargo des Basisobjekts
	 * @param cargo Der neue Cargo
	 */
	public void setCargo(Cargo cargo) {
		this.cargo = cargo;
	}

	/**
	 * Gibt die ID der installierten Core der Basis zurueck.
	 * Falls es keine Core auf der Basis gibt, so wird 0 zurueckgegeben.
	 * 
	 * @return Die ID der Core oder 0
	 */
	public int getCore() {
		return this.core;
	}
	
	/**
	 * Setzt den neuen Kern der Basis. <code>0</code> bedeutet,
	 * dass kein Kern vorhanden ist.
	 * 
	 * @param core der neue Kern oder <code>0</code>
	 */
	public void setCore(int core) {
		this.core = core;
	}

	/**
	 * Gibt die X-Koordinate der Basis zurueck
	 * @return Die X-Koordinate
	 */
	public int getX() {
		return this.x;
	}
	
	/**
	 * Setzt die X-Koordinate der Basis
	 * @param x Die X-Koordinate
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * Gibt die Y-Koordinate der Basis zurueck
	 * @return Die Y-Koordinate
	 */
	public int getY() {
		return this.y;
	}
	
	/**
	 * Setzt die Y-Koordinate der Basis
	 * @param y Die Y-Koordinate
	 */
	public void setY(int y) {
		this.y = y;
	}

	/**
	 * Gibt die ID des Systems zurueck, in dem sich die Basis befindet
	 * @return Die ID des Systems
	 */
	public int getSystem() {
		return this.system;
	}
	
	/**
	 * Setzt das System in dem sich die Basis befindet
	 * @param system Das System
	 */
	public void setSystem(int system){
		this.system = system;
	}

	/**
	 * Gibt zurueck, ob die Core aktiv ist.
	 * @return <code>true</code>, falls die Core aktiv ist
	 */
	public boolean isCoreActive() {
		return this.coreActive != 0;
	}
	
	/**
	 * Setzt den Aktivierungszustand der Core
	 * @param active <code>true</code>, wenn die Core aktiv ist
	 */
	public void setCoreActive(boolean active) {
		this.coreActive = active ? 1 : 0;
	}

	/**
	 * Gibt die maximale Masse an Cargo zurueck, die auf der Basis gelagert werden kann
	 * @return Der Max-Cargo
	 */
	public long getMaxCargo() {
		return this.maxCargo;
	}
	
	/**
	 * Setzt den neuen maximalen Cargo der Basis
	 * @param cargo Der neue maximale Cargo
	 */
	public void setMaxCargo(long cargo) {
		this.maxCargo = cargo;
	}

	/**
	 * Gibt die Anzahl der Bewohner auf der Basis zurueck
	 * @return Die Bewohner
	 */
	public int getBewohner() {
		return this.bewohner;
	}
	
	/**
	 * Setzt die Anzahl der Bewohner auf der Basis
	 * @param bewohner Die neue Anzahl der Bewohner
	 */
	public void setBewohner(int bewohner) {
		this.bewohner = bewohner;
	}
	
	/**
	 * Gibt die Anzahl der Marines auf der Basis zurueck
	 * @return Die Anzahl der Marines
	 */
	public int getMarines() {
		return this.marines;
	}
	
	/**
	 * Setzt die Anzahl der Marines auf der basis
	 * @param marines Die neue Anzahl der Marines
	 */
	public void setMarines(int marines) {
		this.marines = marines;
	}
	
	/**
	 * Gibt die Anzahl der Arbeiter auf der Basis zurueck
	 * @return Die Arbeiter
	 */
	public int getArbeiter() {
		return this.arbeiter;
	}
	
	/**
	 * Setzt die neue Menge der Arbeiter auf der Basis. 
	 * @param arbeiter Die Anzahl der Arbeiter
	 */
	public void setArbeiter(int arbeiter) {
		this.arbeiter = arbeiter;
	}

	/**
	 * Gibt die vorhandene Energiemenge auf der Basis zurueck
	 * @return Die Energiemenge
	 */
	public int getEnergy() {
		return this.energy;
	}
	
	/**
	 * Setzt die Menge der auf der Basis vorhandenen Energie
	 * @param e Die auf der Basis vorhandene Energie
	 */
	public void setEnergy(int e) {
		this.energy = e;
	}
	
	/**
	 * Gibt die maximal auf der Basis speicherbare Energiemenge zurueck
	 * @return die max. Energiemenge
	 */
	public int getMaxEnergy() {
		return this.maxEnergy;
	}
	
	/**
	 * Setzt die maximale Menge an Energie die auf der Basis gespeichert werden kann
	 * @param maxe Die maximale Menge an Energie
	 */
	public void setMaxEnergy(int maxe) {
		this.maxEnergy = maxe;
	}
	
	/**
	 * Gibt die Klassennummer der Basis zurueck (= Der Astityp)
	 * @return Die Klassennummer
	 */
	public int getKlasse() {
		return this.klasse;
	}
	
	/**
	 * Setzt die Klasse der Basis
	 * @param klasse Die Klasse
	 */
	public void setKlasse(int klasse) {
		this.klasse = klasse;
	}
	
	/**
	 * Gibt die Anzahl an Feldern zurueck, die in die Gesamtflaechenanzahl eingerechnet werden
	 * duerfen. Entspricht nicht immer der tatsaechlichen Anzahl an Feldern
	 * @return Die verrechenbare Anzahl an Feldern
	 */
	public int getMaxTiles() {
		return this.maxTiles;
	}
	
	/**
	 * Setzt die Anzahl an Feldern, die in die Gesamtflaechenanzahl eingerechnet werden sollen
	 * @param tiles Die Felderanzahl
	 */
	public void setMaxTiles(int tiles) {
		this.maxTiles = tiles;
	}
	
	/**
	 * Gibt den Radius der Basis auf der Sternenkarte zurueck.
	 * 0 bedeutet in diesem Zusammenhang, dass die Basis keine Ausdehung
	 * in benachbarte Felder hat (Normalfall)
	 * @return Der Radius
	 */
	public int getSize() {
		return this.size;
	}
	
	/**
	 * Setzt den Radius der Basis
	 * @param size Der Radius
	 */
	public void setSize(int size) {
		this.size = size;
	}
	
	/**
	 * Gibt eine Kopie der Liste der automatischen GTU-Verkaufsaktionen zurueck
	 * @return Eine Kopie der Liste der GTU-Verkaufsaktionen beim Tick
	 */
	public List<AutoGTUAction> getAutoGTUActs() {
		List<AutoGTUAction> acts = new ArrayList<AutoGTUAction>();
		acts.addAll(this.autoGtuActsObj);
		
		return acts;
	}
	
	/**
	 * Setzt die Liste der automatischen GTU-Verkaufsaktionen
	 * @param list Die neue Liste
	 */
	public void setAutoGTUActs(List<AutoGTUAction> list) {
		this.autoGtuActsObj = list;
		this.autoGtuActs = Common.implode(";", list);
	}
	
	/**
	 * Generiert den aktuellen Verbrauch/Produktion-Status einer Basis
	 * @param context Der aktuelle Kontext
	 * @param base die ID der Basis
	 * @return der aktuelle Verbrauchs/Produktions-Status
	 */
	public static BaseStatus getStatus( Context context, int base ) {
		org.hibernate.Session db = context.getDB();
		
		return getStatus(context, (Base)db.get(Base.class, base));
	}
		
	/**
	 * Generiert den aktuellen Verbrauch/Produktion-Status einer Basis
	 * @param context Der aktuelle Kontext
	 * @param base die Basis
	 * @return der aktuelle Verbrauchs/Produktions-Status
	 */
	public static BaseStatus getStatus( Context context, Base base ) {
		Cargo stat = new Cargo();
		int e = 0;
		int arbeiter = 0;
		int bewohner = 0;
		Map<Integer,Integer> buildinglocs = new TreeMap<Integer,Integer>(); 
	
		if( (base.getCore() > 0) && base.isCoreActive() ) {
			Core core = Core.getCore(base.getCore());

			stat.substractCargo(core.getConsumes());
			stat.addCargo(core.getProduces());
			
			e = e - core.getEVerbrauch() + core.getEProduktion();
			arbeiter += core.getArbeiter();
			bewohner += core.getBewohner();
		}
		
		Integer[] bebauung = base.getBebauung();
		Integer[] bebon = base.getActive();
			
		for( int o=0; o < base.getWidth() * base.getHeight(); o++ ) {
			if( bebauung[o] == 0 ) {
				continue;
			} 
			
			Building building = Building.getBuilding(bebauung[o]);
	
			if( !buildinglocs.containsKey(building.getId()) ) {
				buildinglocs.put(building.getId(), o);
			}
				
			bebon[o] = building.isActive( base, bebon[o], o ) ? 1 : 0;
		
			if( bebon[o] == 0 ) {
				continue;
			}
			
			building.modifyStats( base, stat );
		
			stat.substractCargo(building.getConsumes());
			stat.addCargo(building.getProduces());
			
			e = e - building.getEVerbrauch() + building.getEProduktion();
			arbeiter += building.getArbeiter();
			bewohner += building.getBewohner();
		}
	
		stat.substractResource( Resources.NAHRUNG, base.getBewohner() );

		return new BaseStatus(stat, e, bewohner, arbeiter, Collections.unmodifiableMap(buildinglocs), bebon);
	}

	@Override
	public Object clone() {
		Base base;
		try {
			base = (Base)super.clone();
			base.id = this.id;
			base.name = this.name;
			base.owner = this.owner;
			base.x = this.x;
			base.y = this.y;
			base.system = this.system;
			base.bewohner = this.bewohner;
			base.arbeiter = this.arbeiter;
			base.energy = this.energy;
			base.maxEnergy = this.maxEnergy;
			base.maxCargo = this.maxCargo;
			base.core = this.core;
			base.klasse = this.klasse;
			base.width = this.width;
			base.height = this.height;
			base.maxTiles = this.maxTiles;
			base.size = this.size;
			base.terrain = this.terrain;
			base.bebauung = this.bebauung;
			base.active = this.active;
			base.coreActive = this.coreActive;
			base.autoGtuActs = this.autoGtuActs;
			base.terrainObj = this.getTerrain().clone();
			base.activeObj = this.getActive().clone();
			base.bebauungObj = this.getBebauung().clone();
			base.cargo = (Cargo)this.getCargo().clone();
			
			base.autoGtuActsObj = new ArrayList<AutoGTUAction>();
			for( int i=0; i < this.autoGtuActsObj.size(); i++ ) {
				base.autoGtuActsObj.add((AutoGTUAction)this.autoGtuActsObj.get(i).clone());
			}
		
			return base;
		} catch (CloneNotSupportedException e) {
			// EMPTY
		}
		return null;
	}

	private boolean update() {
		this.terrainObj = Common.explodeToInteger("|",this.terrain);
		this.bebauungObj = Common.explodeToInteger("|",this.bebauung);
		this.activeObj = Common.explodeToInteger("|",this.active);
		
		String[] autogtuacts = StringUtils.split(this.autoGtuActs,";");
		List<AutoGTUAction> acts = new ArrayList<AutoGTUAction>();
		for( int i=0; i < autogtuacts.length; i++ ) {
			String[] split = StringUtils.split(autogtuacts[i],":");
			
			acts.add(new AutoGTUAction(Resources.fromString(split[0]), Integer.parseInt(split[1]), Long.parseLong(split[2])) );
		}
		this.autoGtuActsObj = acts;
		
		boolean update = false;
		
		// Ggf die Feldergroessen fixen
		if( getTerrain().length < getWidth()*getHeight() ) {
			Integer[] terrain = new Integer[getWidth()*getHeight()];
			System.arraycopy(getTerrain(), 0, terrain, 0, getTerrain().length );
			for( int i=Math.max(getTerrain().length-1,0); i < getWidth()*getHeight(); i++ ) {
				int rnd = RandomUtils.nextInt(7);
				if( rnd > 4 ) {
					terrain[i] = rnd - 4;	
				}
				else {
					terrain[i] = 0;	
				}
			}
			
			setTerrain(terrain);
			update = true;
		}
			
		if( getBebauung().length < getWidth()*getHeight() ) {
			Integer[] bebauung = new Integer[getWidth()*getHeight()];
			System.arraycopy(getBebauung(), 0, bebauung, 0, getBebauung().length );
			for( int i=Math.max(getBebauung().length-1,0); i < getWidth()*getHeight(); i++ ) {
				bebauung[i] = 0;	
			}
			
			setBebauung(bebauung);
			update = true;
		}
		
		if( getActive().length < getWidth()*getHeight() ) {
			Integer[] active = new Integer[getWidth()*getHeight()];
			System.arraycopy(getActive(), 0, active, 0, getActive().length );
			for( int i=Math.max(getActive().length-1,0); i < getWidth()*getHeight(); i++ ) {
				active[i] = 0;	
			}
			
			setActive(active);
			update = true;
		}
		
		return update;
	}
	
	public boolean onDelete(Session s) throws CallbackException {
		// EMPTY
		return false;
	}

	public void onLoad(Session s, Serializable id) {
		update();
	}

	public boolean onSave(Session s) throws CallbackException {
		// EMPTY
		return false;
	}

	public boolean onUpdate(Session s) throws CallbackException {
		// EMPTY
		return false;
	}

	/**
	 * Gibt die Versionsnummer des Eintrags zurueck
	 * @return Die Versionsnummer
	 */
	public int getVersion() {
		return version;
	}

	public Location getLocation() {
		return new Location(this.getSystem(), this.getX(), this.getY());
	}
	
	public String transfer(Transfering to, ResourceID resource, long count) {
		return new Transfer().transfer(this, to, resource, count);
	}
	
	/**
	 * Transfers crew from the asteroid to a ship.
	 * 
	 * @param ship Ship that gets the crew.
	 * @param amount People that should be transfered.
	 * @return People that where transfered.
	 */
	public int transferCrew(Ship ship, int amount) {
		//Check ship position
		if(ship.getSystem() != getSystem() || ship.getX() != getX() || ship.getY() != getY()) {
			return 0;
		}
		
		//Only workless people can be transfered, when there is enough space on the ship
		int maxAmount = ship.getTypeData().getCrew() - ship.getCrew();
		int workless = getBewohner() - getArbeiter();
		amount = Math.min(amount, maxAmount);
		amount = Math.min(amount, workless);
		
		ship.setCrew(ship.getCrew() + amount);
		setBewohner(getBewohner() - amount);
		
		return amount;
	}
}
