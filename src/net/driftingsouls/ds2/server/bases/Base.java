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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.Transfer;
import net.driftingsouls.ds2.server.cargo.Transfering;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Factory;
import net.driftingsouls.ds2.server.entities.Feeding;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.units.BaseUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import net.driftingsouls.ds2.server.werften.BaseWerft;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;
import org.hibernate.classic.Lifecycle;

/**
 * <p>Repraesentiert eine Basis in DS.</p>
 *
 * @author Christopher Jung
 */
@Entity
@Table(name="bases")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@BatchSize(size=50)
public class Base implements Cloneable, Lifecycle, Locatable, Transfering, Feeding
{
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
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="klasse", nullable=false)
	private BaseType klasse;
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
	private String spawnableress;
	private String spawnressavailable;
	private boolean isloading;
	private boolean isfeeding;

	@OneToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH})
	@JoinColumn
	private Academy academy;
	@OneToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH})
	@JoinColumn
	private Forschungszentrum forschungszentrum;
	@OneToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH})
	@JoinColumn
	private BaseWerft werft;
	@OneToMany(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH})
	@JoinColumn(name="col")
	private Set<Factory> factories;

	@OneToMany(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH},
			targetEntity=net.driftingsouls.ds2.server.bases.BaseUnitCargoEntry.class,
			mappedBy="basis")
	@BatchSize(size=50)
	@NotFound(action = NotFoundAction.IGNORE)
	private Set<BaseUnitCargoEntry> units;
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
	 * Konstruktor.
	 *
	 */
	public Base()
	{
		// EMPTY
	}

	/**
	 * Erstellt eine neue Basis.
	 * @param loc Die Position
	 * @param owner Der Besitzer
	 */
	public Base(Location loc, User owner)
	{
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
		this.units = new HashSet<BaseUnitCargoEntry>();
	}

	/**
	 * Gibt die ID der Basis zurueck.
	 * @return die ID der Basis
	 */
	@Override
	public int getId()
	{
		return this.id;
	}

	/**
	 * Gibt die Breite der Bauflaeche auf der Basis in Feldern zurueck.
	 * @return Die Breite
	 */
	public int getWidth()
	{
		return this.width;
	}

	/**
	 * Setzt die Breite der Bauflaeche auf der Basis.
	 * @param width Die Breite
	 */
	public void setWidth(int width)
	{
		this.width = width;
	}

	/**
	 * Gibt die Hoehe der Bauflaeche auf der Basis in Feldern zurueck.
	 * @return Die Hoehe
	 */
	public int getHeight()
	{
		return this.height;
	}

	/**
	 * Setzt die Hoehe der Bauflaeche auf der Basis.
	 * @param height Die Hoehe
	 */
	public void setHeight(int height)
	{
		this.height = height;
	}

	/**
	 * Gibt den Namen der Basis zurueck.
	 * @return Der Name
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Gibt der Basis einen neuen Namen.
	 * @param name Der neue Name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Gibt den Besitzer zurueck.
	 *
	 * @return Der Besitzer
	 */
	@Override
	public User getOwner()
	{
		return this.owner;
	}

	/**
	 * Setzt den neuen Besitzer fuer die Basis.
	 * @param owner Der neue Besitzer
	 */
	public void setOwner(User owner)
	{
		this.owner = owner;
	}

	/**
	 * Gibt die Terrain-Typen der einzelnen Basisfelder zurueck.
	 * @return Die Terraintypen der Felder
	 */
	public Integer[] getTerrain()
	{
		return this.terrainObj.clone();
	}

	/**
	 * Setzt das neue Terrain der Basis.
	 * @param terrain Das neue Terrain
	 */
	public void setTerrain(final Integer[] terrain)
	{
		this.terrainObj = terrain.clone();
		this.terrain = Common.implode("|", terrain);
	}

	/**
	 * Gibt die IDs der auf den einzelnen Feldern stehenden Gebaeude zurueck.
	 * Sollte auf einem Feld kein Gebaeude stehen ist die ID 0.
	 *
	 * @return Die IDs der Gebaeude
	 */
	public Integer[] getBebauung()
	{
		return this.bebauungObj.clone();
	}

	/**
	 * Setzt die neue Bebauung der Basis.
	 * @param bebauung Die neue Bebauung
	 */
	public void setBebauung(final Integer[] bebauung)
	{
		this.bebauungObj = bebauung.clone();
		this.bebauung = Common.implode("|", bebauung);
	}

	/**
	 * Gibt an, auf welchen Feldern die Gebaeude aktiv (1) sind und auf welchen
	 * nicht (0).
	 * @return Aktivierungsgrad der Gebaeude auf den Feldern
	 */
	public Integer[] getActive()
	{
		return this.activeObj.clone();
	}

	/**
	 * Setzt den Aktivierungszustand aller Gebaeude. <code>1</code> bedeutet, dass das
	 * Gebaeude aktiv ist. <code>0</code>, dass das Gebaeude nicht aktiv ist.
	 * @param active Der neue Aktivierungszustand aller Gebaeude
	 */
	public void setActive(final Integer[] active)
	{
		this.activeObj = active.clone();
		this.active = Common.implode("|", active);
	}

	/**
	 * Gibt den Cargo der Basis zurueck.
	 * @return Der Cargo
	 */
	// TODO: UnmodifiableCargos zurueckgeben (zuerst alle Verwendungen checken und umbauen)
	@Override
	public Cargo getCargo()
	{
		return cargo;
	}

	@Override
	public boolean equals(Object object)
	{
		if( object == null )
		{
			return false;
		}

		if(object.getClass() != this.getClass())
		{
			return false;
		}

		Base other = (Base)object;

		return other.getId() == this.getId();
	}

	@Override
	public int hashCode()
	{
		return this.getId();
	}

	/**
	 * Setzt den Cargo des Basisobjekts.
	 * @param cargo Der neue Cargo
	 */
	@Override
	public void setCargo(Cargo cargo)
	{
		this.cargo = cargo;
	}

	/**
	 * Gibt die ID der installierten Core der Basis zurueck.
	 * Falls es keine Core auf der Basis gibt, so wird 0 zurueckgegeben.
	 *
	 * @return Die ID der Core oder 0
	 */
	public int getCore()
	{
		return this.core;
	}

	/**
	 * Setzt den neuen Kern der Basis. <code>0</code> bedeutet,
	 * dass kein Kern vorhanden ist.
	 *
	 * @param core der neue Kern oder <code>0</code>
	 */
	public void setCore(int core)
	{
		this.core = core;
	}

	/**
	 * Gibt die X-Koordinate der Basis zurueck.
	 * @return Die X-Koordinate
	 */
	public int getX()
	{
		return this.x;
	}

	/**
	 * Setzt die X-Koordinate der Basis.
	 * @param x Die X-Koordinate
	 */
	public void setX(int x)
	{
		this.x = x;
	}

	/**
	 * Gibt die Y-Koordinate der Basis zurueck.
	 * @return Die Y-Koordinate
	 */
	public int getY()
	{
		return this.y;
	}

	/**
	 * Setzt die Y-Koordinate der Basis.
	 * @param y Die Y-Koordinate
	 */
	public void setY(int y)
	{
		this.y = y;
	}

	/**
	 * Gibt die ID des Systems zurueck, in dem sich die Basis befindet.
	 * @return Die ID des Systems
	 */
	public int getSystem()
	{
		return this.system;
	}

	/**
	 * Setzt das System in dem sich die Basis befindet.
	 * @param system Das System
	 */
	public void setSystem(int system)
	{
		this.system = system;
	}

	/**
	 * Gibt zurueck, ob die Core aktiv ist.
	 * @return <code>true</code>, falls die Core aktiv ist
	 */
	public boolean isCoreActive()
	{
		return this.coreActive != 0;
	}

	/**
	 * Setzt den Aktivierungszustand der Core.
	 * @param active <code>true</code>, wenn die Core aktiv ist
	 */
	public void setCoreActive(boolean active)
	{
		this.coreActive = active ? 1 : 0;
	}

	/**
	 * Gibt die maximale Masse an Cargo zurueck, die auf der Basis gelagert werden kann.
	 * @return Der Max-Cargo
	 */
	@Override
	public long getMaxCargo()
	{
		return this.maxCargo;
	}

	/**
	 * Setzt den neuen maximalen Cargo der Basis.
	 * @param cargo Der neue maximale Cargo
	 */
	public void setMaxCargo(long cargo)
	{
		this.maxCargo = cargo;
	}

	/**
	 * Gibt die Anzahl der Bewohner auf der Basis zurueck.
	 * @return Die Bewohner
	 */
	public int getBewohner()
	{
		return this.bewohner;
	}

	/**
	 * Setzt die Anzahl der Bewohner auf der Basis.
	 * @param bewohner Die neue Anzahl der Bewohner
	 */
	public void setBewohner(int bewohner)
	{
		this.bewohner = bewohner;
	}

	/**
	 * Gibt dden UnitCargo der Basis zurueck.
	 * @return Der UnitCargo
	 */
	public BaseUnitCargo getUnits()
	{
		List<UnitCargoEntry> entries = new ArrayList<UnitCargoEntry>(units);
		return new BaseUnitCargo(entries, this);
	}

	/**
	 * Setzt den UnitCargo der basis.
	 * @param unitcargo Der neue UnitCargo
	 */
	public void setUnits(UnitCargo unitcargo)
	{
		BaseUnitCargo cargo = this.getUnits();
		if( unitcargo != cargo )
		{
			// Ein anderer Cargo soll gespeichert werden
			// momentane Instanz leeren und neu befuellen
			cargo.clear();
			cargo.addCargo(unitcargo);
		}
		cargo.save();
	}

	/**
	 * Gibt die Anzahl der Arbeiter auf der Basis zurueck.
	 * @return Die Arbeiter
	 */
	public int getArbeiter()
	{
		return this.arbeiter;
	}

	/**
	 * Setzt die neue Menge der Arbeiter auf der Basis.
	 * @param arbeiter Die Anzahl der Arbeiter
	 */
	public void setArbeiter(int arbeiter)
	{
		this.arbeiter = arbeiter;
	}

	/**
	 * Gibt die vorhandene Energiemenge auf der Basis zurueck.
	 * @return Die Energiemenge
	 */
	public int getEnergy()
	{
		return this.energy;
	}

	/**
	 * Setzt die Menge der auf der Basis vorhandenen Energie.
	 * @param e Die auf der Basis vorhandene Energie
	 */
	public void setEnergy(int e)
	{
		this.energy = e;
	}

	/**
	 * Gibt die maximal auf der Basis speicherbare Energiemenge zurueck.
	 * @return die max. Energiemenge
	 */
	public int getMaxEnergy()
	{
		return this.maxEnergy;
	}

	/**
	 * Setzt die maximale Menge an Energie die auf der Basis gespeichert werden kann.
	 * @param maxe Die maximale Menge an Energie
	 */
	public void setMaxEnergy(int maxe)
	{
		this.maxEnergy = maxe;
	}

	/**
	 * Gibt die Basis-Klasse zurueck.
	 * @return die Klasse
	 */
	public BaseType getBaseType()
	{
		return this.klasse;
	}

	/**
	 * Gibt die Klassennummer der Basis zurueck (= Der Astityp).
	 * @return Die Klassennummer
	 */
	public int getKlasse()
	{
		return this.klasse.getId();
	}

	/**
	 * Setzt die Klasse der Basis.
	 * @param klasse Die Klasse
	 */
	public void setKlasse(int klasse)
	{
		org.hibernate.Session db = getDB();
		BaseType type = (BaseType)db.get(BaseType.class, klasse);
		if(type != null)
		{
			this.klasse = type;
		}
	}

	/**
	 * Gibt die Anzahl an Feldern zurueck, die in die Gesamtflaechenanzahl eingerechnet werden
	 * duerfen. Entspricht nicht immer der tatsaechlichen Anzahl an Feldern.
	 * @return Die verrechenbare Anzahl an Feldern
	 */
	public int getMaxTiles()
	{
		return this.maxTiles;
	}

	/**
	 * Setzt die Anzahl an Feldern, die in die Gesamtflaechenanzahl eingerechnet werden sollen.
	 * @param tiles Die Felderanzahl
	 */
	public void setMaxTiles(int tiles)
	{
		this.maxTiles = tiles;
	}

	/**
	 * Gibt den Radius der Basis auf der Sternenkarte zurueck.
	 * 0 bedeutet in diesem Zusammenhang, dass die Basis keine Ausdehung
	 * in benachbarte Felder hat (Normalfall).
	 * @return Der Radius
	 */
	public int getSize()
	{
		return this.size;
	}

	/**
	 * Setzt den Radius der Basis.
	 * @param size Der Radius
	 */
	public void setSize(int size)
	{
		this.size = size;
	}

	/**
	 * Gibt eine Kopie der Liste der automatischen GTU-Verkaufsaktionen zurueck.
	 * @return Eine Kopie der Liste der GTU-Verkaufsaktionen beim Tick
	 */
	public List<AutoGTUAction> getAutoGTUActs()
	{
		List<AutoGTUAction> acts = new ArrayList<AutoGTUAction>();
		acts.addAll(this.autoGtuActsObj);

		return acts;
	}

	/**
	 * Setzt die Liste der automatischen GTU-Verkaufsaktionen.
	 * @param list Die neue Liste
	 */
	public void setAutoGTUActs(List<AutoGTUAction> list)
	{
		this.autoGtuActsObj = list;
		this.autoGtuActs = Common.implode(";", list);
	}

	/**
	 * Gibt die zum spawn freigegebenen Ressourcen zurueck.
	 * @return Die Spawn-Ressourcen als String (itemid,chance,maxmenge)
	 */
	public String getSpawnableRess()
	{
		if (this.spawnableress == null )
		{
			return "";
		}
		return this.spawnableress;
	}

	/**
	 * Setzt die zum spawn freigegebenen Ressourcen zurueck.
	 * @param spawnableRess Die Spawn-Ressourcen als String
	 */
	public void setSpawnableRess(String spawnableRess)
	{
		this.spawnableress = spawnableRess;
	}

	/**
	 * Gibt die zum spawn freigegebenen Ressourcen in einer HashMap zurueck.
	 * Beruecksichtigt ebenfalls die Systemvorraussetzungen
	 * Keys von 1 bis 100 mit der dazugehoerigen Ressource und Maxmenge die gespawnt wird
	 * @return Die zum Spawn freigegebenen Ressourcen
	 */
	private Map<Integer,Integer[]> getSpawnableRessMap()
	{
		org.hibernate.Session db = getDB();
		StarSystem system = (StarSystem)db.get(StarSystem.class, this.system);

		if(getSpawnableRess() == null && system.getSpawnableRess() == null && getBaseType().getSpawnableRess() == null)
		{
			return null;
		}

		Map<Integer,Integer[]> spawnress = new HashMap<Integer,Integer[]>();
		Map<Integer,Integer[]> spawnressmap = new HashMap<Integer,Integer[]>();
		int chances = 0;
		int baseress = 0;

		if( getSpawnableRess() != null )
		{
			String[] spawnableress = StringUtils.split(getSpawnableRess(), ";");
			for(int i = 0;i < spawnableress.length; i++) {
				String[] thisress = StringUtils.split(spawnableress[i], ",");
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if(getSpawnableRessAmount(itemid) <= 0)
				{
					chances += chance;

					spawnress.put(i, new Integer[] {chance, itemid, maxvalue});
				}
			}
			baseress = spawnress.size();
		}
		if( system.getSpawnableRess() != null )
		{
			String[] spawnableresssystem = StringUtils.split(system.getSpawnableRess(), ";");
			for(int i = 0;i < spawnableresssystem.length; i++) {
				String[] thisress = StringUtils.split(spawnableresssystem[i], ",");
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if(getSpawnableRessAmount(itemid) <= 0)
				{
					chances += chance;

					spawnress.put(i+baseress, new Integer[] {chance, itemid, maxvalue});
				}
			}
		}
		if( getBaseType().getSpawnableRess() != null && !getBaseType().getSpawnableRess().isEmpty() )
		{
			String[] spawnableresstype = StringUtils.split(getBaseType().getSpawnableRess(), ";");
			for(int i = 0;i < spawnableresstype.length; i++) {
				String[] thisress = StringUtils.split(spawnableresstype[i], ",");
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if(getSpawnableRessAmount(itemid) <= 0)
				{
					chances += chance;

					spawnress.put(i+baseress, new Integer[] {chance, itemid, maxvalue});
				}
			}
		}
		double chancefactor = 100 / (double)chances;
		chances = 1;
		for(int i = 0; i < spawnress.size(); i++)
		{
			Integer[] thisress = spawnress.get(i);
			int chance = (int)Math.round((double)thisress[0] * chancefactor);
			int itemid = thisress[1];
			int maxvalue = thisress[2];
			for(int a = chances; a <= chance+chances; a++) {
				spawnressmap.put(a, new Integer[] {itemid, maxvalue});
			}
			chances += chance+1;
		}

		return spawnressmap;
	}

	/**
	 * Gibt die aktuell verfuegbare Ressourcenmenge der Spawnable-Ressourcen zurueck.
	 * @return Die Ressourcenmengen als String (itemid,menge;itemid,menge)
	 */
	public String getAvailableSpawnableRess()
	{
		if( this.spawnressavailable == null || this.spawnressavailable.equals("null"))
		{
			return null;
		}
		return this.spawnressavailable;
	}

	/**
	 * Setzt die aktuell verfuegbare Ressourcenmenge der Spawnable-Ressourcen.
	 * @param availableRess Die Ressourcenmengen als String
	 */
	public void setAvailableSpawnableRess(String availableRess)
	{
		this.spawnressavailable = availableRess;
	}

	/**
	 * Gibt zurueck, ob diese Basis den Speicher der Schiffe aufladen soll.
	 * @return <code>true</code>, wenn der Speicher geladen werden soll
	 */
	public boolean isLoading()
	{
		return isloading;
	}

	/**
	 * Setzt, ob diese Basis den Speicher der Schiffe aufladen soll.
	 * @param loading <code>true</code>, wenn der Speicher aufgeladen werden soll
	 */
	public void setLoading(boolean loading)
	{
		this.isloading = loading;
	}

	/**
	 * Gibt zurueck, ob diese Basis die Schiffe im Orbit versorgt.
	 * @return <code>true</code>, wenn die Schiffe versorgt werden
	 */
	public boolean isFeeding()
	{
		return isfeeding;
	}

	/**
	 * Setzt, ob diese Basis die Schiffe im Orbit versorgt.
	 * @param feeding <code>true</code>, wenn die Schiffe versorgt werden sollen
	 */
	public void setFeeding(boolean feeding)
	{
		this.isfeeding = feeding;
	}

	/**
	 * Gibt den Nettoverbrauch der Basis aus.
	 * @return Der Nettoverbrauch
	 */
	private Cargo getNettoConsumption()
	{
		Cargo stat = new Cargo();
		if( (getCore() > 0) && isCoreActive() ) {
			Core core = Core.getCore(getCore());

			stat.addCargo(core.getConsumes());
		}

		Integer[] bebauung = getBebauung();
		Integer[] bebon = getActive();

		for( int o=0; o < getWidth() * getHeight(); o++ )
		{
			if( bebauung[o] == 0 )
			{
				continue;
			}

			Building building = Building.getBuilding(bebauung[o]);

			bebon[o] = building.isActive( this, bebon[o], o ) ? 1 : 0;

			if( bebon[o] == 0 )
			{
				continue;
			}

			building.modifyConsumptionStats( this, stat, bebauung[o] );

			stat.addCargo(building.getConsumes());
		}

		return stat;
	}

	/**
	 * Gibt die Nettoproduktion der Basis aus.
	 * @return Die Nettoproduktion
	 */
	private Cargo getNettoProduction()
	{
		Cargo stat = new Cargo();
		if( (getCore() > 0) && isCoreActive() ) {
			Core core = Core.getCore(getCore());

			stat.addCargo(core.getProduces());
		}

		Integer[] bebauung = getBebauung();
		Integer[] bebon = getActive();

		for( int o=0; o < getWidth() * getHeight(); o++ )
		{
			if( bebauung[o] == 0 )
			{
				continue;
			}

			Building building = Building.getBuilding(bebauung[o]);

			bebon[o] = building.isActive( this, bebon[o], o ) ? 1 : 0;

			if( bebon[o] == 0 )
			{
				continue;
			}

			building.modifyProductionStats( this, stat, bebauung[o] );

			stat.addCargo(building.getProduces());
		}

		return stat;
	}

	/**
	 * Generiert den aktuellen Verbrauch/Produktion-Status einer Basis.
	 * @param base die Basis
	 * @return der aktuelle Verbrauchs/Produktions-Status
	 */
	public static BaseStatus getStatus( Base base )
	{
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

		for( int o=0; o < base.getWidth() * base.getHeight(); o++ )
		{
			if( bebauung[o] == 0 )
			{
				continue;
			}

			Building building = Building.getBuilding(bebauung[o]);

			if( !buildinglocs.containsKey(building.getId()) ) {
				buildinglocs.put(building.getId(), o);
			}

			bebon[o] = building.isActive( base, bebon[o], o ) ? 1 : 0;

			if( bebon[o] == 0 )
			{
				continue;
			}

			building.modifyStats( base, stat, bebauung[o] );

			stat.substractCargo(building.getConsumes());
			stat.addCargo(building.getAllProduces());

			e = e - building.getEVerbrauch() + building.getEProduktion();
			arbeiter += building.getArbeiter();
			bewohner += building.getBewohner();
		}

		stat.substractResource( Resources.NAHRUNG, (long)Math.ceil(base.getBewohner()/10) );

		stat.substractResource( Resources.NAHRUNG, (long)Math.ceil(base.getUnits().getNahrung()/10) );
		stat.substractResource( Resources.RE, base.getUnits().getRE() );

		return new BaseStatus(stat, e, bewohner, arbeiter, Collections.unmodifiableMap(buildinglocs), bebon);
	}

	private BaseStatus getStatus()
	{
		return Base.getStatus(this);
	}

	@Override
	public Object clone()
	{
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
			for( int i=0; i < this.autoGtuActsObj.size(); i++ )
			{
				base.autoGtuActsObj.add((AutoGTUAction)this.autoGtuActsObj.get(i).clone());
			}

			return base;
		}
		catch (CloneNotSupportedException e) {
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
		for( int i=0; i < autogtuacts.length; i++ )
		{
			String[] split = StringUtils.split(autogtuacts[i],":");
			ResourceID rid = Resources.fromString(split[0]);
			if( rid != null ) {
				acts.add(new AutoGTUAction(rid, Integer.parseInt(split[1]), Long.parseLong(split[2])) );
			}
		}
		this.autoGtuActsObj = acts;

		boolean update = false;

		// Ggf die Feldergroessen fixen
		if( getTerrain().length < getWidth()*getHeight() )
		{
			Integer[] terrain = new Integer[getWidth()*getHeight()];
			System.arraycopy(getTerrain(), 0, terrain, 0, getTerrain().length );
			Integer[] allowedterrain = getBaseType().getTerrain();
			for( int i=Math.max(getTerrain().length,0); i < getWidth()*getHeight(); i++ )
			{
				if(allowedterrain == null || allowedterrain.length == 0)
				{
					terrain[i] = 0;
					continue;
				}
				if(allowedterrain.length == 1)
				{
					terrain[i] = allowedterrain[0];
				}
				int rnd = RandomUtils.nextInt(allowedterrain.length);

				terrain[i] = allowedterrain[rnd];
			}

			setTerrain(terrain);
			update = true;
		}

		if( getBebauung().length < getWidth()*getHeight() )
		{
			Integer[] bebauung = new Integer[getWidth()*getHeight()];
			System.arraycopy(getBebauung(), 0, bebauung, 0, getBebauung().length );
			for( int i=Math.max(getBebauung().length,0); i < getWidth()*getHeight(); i++ )
			{
				bebauung[i] = 0;
			}

			setBebauung(bebauung);
			update = true;
		}

		if( getActive().length < getWidth()*getHeight() )
		{
			Integer[] active = new Integer[getWidth()*getHeight()];
			System.arraycopy(getActive(), 0, active, 0, getActive().length );
			for( int i=Math.max(getActive().length,0); i < getWidth()*getHeight(); i++ )
			{
				active[i] = 0;
			}

			setActive(active);
			update = true;
		}

		return update;
	}

	@Override
	public boolean onDelete(Session s) throws CallbackException
	{
		// EMPTY
		return false;
	}

	@Override
	public void onLoad(Session s, Serializable id)
	{
		update();
	}

	@Override
	public boolean onSave(Session s) throws CallbackException
	{
		// EMPTY
		return false;
	}

	@Override
	public boolean onUpdate(Session s) throws CallbackException
	{
		// EMPTY
		return false;
	}

	/**
	 * Gibt die Versionsnummer des Eintrags zurueck.
	 * @return Die Versionsnummer
	 */
	public int getVersion()
	{
		return version;
	}

	@Override
	public Location getLocation()
	{
		return new Location(this.getSystem(), this.getX(), this.getY());
	}

	@Override
	public String transfer(Transfering to, ResourceID resource, long count)
	{
		return new Transfer().transfer(this, to, resource, count);
	}

	/**
	 * Transfers crew from the asteroid to a ship.
	 *
	 * @param ship Ship that gets the crew.
	 * @param amount People that should be transfered.
	 * @return People that where transfered.
	 */
	public int transferCrew(Ship ship, int amount)
	{
		//Check ship position
		if(ship.getSystem() != getSystem() || ship.getX() != getX() || ship.getY() != getY())
		{
			return 0;
		}

		//Only workless people can be transfered, when there is enough space on the ship
		int maxAmount = ship.getTypeData().getCrew() - ship.getCrew();
		int workless = getBewohner() - getArbeiter();
		amount = Math.min(amount, maxAmount);
		amount = Math.min(amount, workless);

		ship.setCrew(ship.getCrew() + amount);
		setBewohner(getBewohner() - amount);

		ship.recalculateShipStatus();

		return amount;
	}

	/**
	 * Gibt die Werft der Basis zurueck.
	 *
	 * @return <code>null</code>, wenn die Basis keine Werft hat, ansonsten das Objekt.
	 */
	public BaseWerft getShipyard()
	{
		return this.getWerft();
	}

	/**
	 * Gibt an, ob die Basis eine Werft hat.
	 *
	 * @return <code>true</code>, wenn die Basis eine Werft hat, <code>false</code> ansonsten.
	 */
	public boolean hasShipyard()
	{
		return getShipyard() != null;
	}

	/**
	 * Gibt das userspezifische Bild der Basis zurueck. Falls es kein spezielles Bild
	 * fuer den angegebenen Benutzer gibt wird <code>null</code> zurueckgegeben.
	 *
	 * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
	 * @param user Aktueller Spieler.
	 * @param scanned <code>true</code>, wenn die Basis derzeit von einem Schiff des Spielers gescannt werden kann.
	 * @return Der Bildstring der Basis oder <code>null</code>
	 */
	public String getOverlayImage(Location location, User user, boolean scanned)
	{
		if(!location.sameSector(0, getLocation(), size))
		{
			return null;
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();
		User nobody = (User)db.get(User.class, -1);
		User zero = (User)db.get(User.class, 0);

		if(size > 0)
		{
			return null;
		}
		else if(getOwner().getId() == user.getId())
		{
			return "asti_own/asti_own";
		}
		else if((getOwner().getId() != 0) && (user.getAlly() != null) && (getOwner().getAlly() == user.getAlly()) && user.getAlly().getShowAstis())
		{
			return "asti_ally/asti_ally";
		}
		else if(scanned && !getOwner().equals(nobody) && !getOwner().equals(zero))
		{
			return "asti_enemy/asti_enemy";
		}
		else
		{
			return null;
		}
	}

	/**
	 * Gibt das Bild der Basis zurueck.
	 * Dabei werden Ausdehnung und Besitzer beruecksichtigt.
	 *
	 * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
	 * @return Der Bildstring der Basis oder einen Leerstring, wenn die Basis die Koordinaten nicht schneidet
	 */
	public String getBaseImage(Location location)
	{
		if(!location.sameSector(0, getLocation(), size))
		{
			return "";
		}

		if(size > 0)
		{
			int imgcount = 0;
			Location centerLoc = getLocation();
			for(int by = getY() - getSize(); by <= getY() + getSize(); by++)
			{
				for(int bx = getX() - getSize(); bx <= getX() + getSize(); bx++)
				{
					Location loc = new Location(getSystem(), bx, by);

					if( !centerLoc.sameSector(0, loc, getSize()))
					{
						continue;
					}

					if(location.equals(loc))
					{
						return "kolonie"+getKlasse()+"_lrs/kolonie"+getKlasse()+"_lrs"+imgcount;
					}

					imgcount++;
				}
			}
		}
		else
		{
			return "kolonie"+getKlasse()+"_lrs/kolonie"+getKlasse()+"_lrs";
		}

		assert false;
		return null;
	}

	/**
	 * @return The current amount of food on the object.
	 */
	@Override
	public long getNahrungCargo()
	{
		Cargo cargo = this.getCargo();
		return cargo.getResourceCount(Resources.NAHRUNG);
	}

	/**
	 * Updates the amount of food on the object.
	 *
	 * @param newFood The new amount of food.
	 */
	@Override
	public void setNahrungCargo(long newFood)
	{
		Cargo cargo = this.getCargo();
		cargo.setResource(Resources.NAHRUNG, newFood);
		this.setCargo(cargo);
	}

	/**
	 * @return Die Bilanz der Basis.
	 */
	public long getBalance()
	{
		BaseStatus status = getStatus(this );

		Cargo produktion = status.getProduction();

		return produktion.getResourceCount( Resources.RE );
	}

	/**
	 * @return Die Nahrungsbilanz der Basis.
	 */
	public long getNahrungsBalance()
	{
		BaseStatus status = getStatus(this );

		Cargo produktion = status.getProduction();

		return produktion.getResourceCount( Resources.NAHRUNG );
	}

	/**
	 * Gibt zurueck, wie viel diese Basis an Nahrung bei sich behalten muss um alle Schiffe im Sektor versorgen zu koennen.
	 * @return Die Nahrung die die Basis behalten muss
	 */
	public long getSaveNahrung()
	{
		if(!isLoading())
		{
			return cargo.getResourceCount(Resources.NAHRUNG);
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		long savenahrung = 0;

		List<?> ships = db.createQuery("from Ship fetch all properties where owner=:owner and system=:sys and x=:x and y=:y")
								.setEntity("owner", getOwner())
								.setInteger("sys", getSystem())
								.setInteger("x", getX())
								.setInteger("y", getY())
								.list();

		for(Iterator<?> iter=ships.iterator();iter.hasNext();)
		{
			Ship ship = (Ship)iter.next();
			savenahrung += ship.getFoodConsumption();
		}

		if(savenahrung > cargo.getResourceCount(Resources.NAHRUNG))
		{
			savenahrung = cargo.getResourceCount(Resources.NAHRUNG);
		}
		return savenahrung;
	}

	/**
	 * Gibt alle auf der Basis vorhandenen Fabriken zurueck.
	 * @return Die Fabriken
	 */
	public Set<Factory> getFactories()
	{
		return this.factories;
	}

	/**
	 * Setzt alle auf der Basis vorhandenen Fabriken.
	 * @param factories Die Fabriken
	 */
	public void setFactories(Set<Factory> factories)
	{
		this.factories = factories;
	}

	/**
	 * Gibt eine evt vorhandene Akademie zurueck.
	 * @return Die Akademie oder <code>null</code>
	 */
	public Academy getAcademy()
	{
		return academy;
	}

	/**
	 * Setzt die Akademie auf der Basis.
	 * @param academy die Akademie
	 */
	public void setAcademy(Academy academy)
	{
		this.academy = academy;
	}

	/**
	 * Gibt ein evt vorhandenes Forschungszentrum zurueck.
	 * @return das Forschungszentrum oder <code>null</code>
	 */
	public Forschungszentrum getForschungszentrum()
	{
		return forschungszentrum;
	}

	/**
	 * Setzt das Forschungszentrum auf der Basis.
	 * @param forschungszentrum Das Forschungszentrum
	 */
	public void setForschungszentrum(Forschungszentrum forschungszentrum)
	{
		this.forschungszentrum = forschungszentrum;
	}

	/**
	 * Gibt eine evt vorhandene Werft zurueck.
	 * @return Die Werft oder <code>null</code>
	 */
	public BaseWerft getWerft()
	{
		return werft;
	}

	/**
	 * Setzt die Werft auf der Basis.
	 * @param werft Die Werft
	 */
	public void setWerft(BaseWerft werft)
	{
		this.werft = werft;
	}

	/**
	 * Laesst die Basis ticken.
	 *
	 * @return Die Ticknachrichten, wenn es welche gab.
	 */
	public String tick()
	{
		String message = "Basis " + getName() + "\n----------------------------------\n";
		boolean usefullMessage = false;

		String proof = proofBuildings();
		if(!proof.equals(""))
		{
			message += proof;
			usefullMessage = true;
		}

		BaseStatus state = getStatus();

		immigrate(state);

		if(!rebalanceEnergy(state))
		{
			message += "Zu wenig Energie. Die Produktion f&auml;llt aus.\n";
			usefullMessage = true;
		}
		else
		{
			String prodmsg = produce(state);
			if(!prodmsg.equals(""))
			{
				message += prodmsg;
				usefullMessage = true;
			}
			else
			{
				long money = automaticSale();
				boolean overfullCargo = clearOverfullCargo(state);
				if(money > 0)
				{
					getOwner().transferMoneyFrom(Faction.GTU, money, "Automatischer Warenverkauf Asteroid " + getName(), false, UserMoneyTransfer.Transfer.AUTO);
				}

				if(money > 0)
				{
					message += "Ihnen wurden " + money + " RE f&uuml;r automatische Verk&auml;ufe gut geschrieben.\n";
					usefullMessage = true;
				}

				if(overfullCargo)
				{
					message += "Wegen uuml;berfuuml;llten Lagerr&auml;umen wurde ein Teil der Produktion vernichtet.\n";
					usefullMessage = true;
				}
			}
		}

		if(getBewohner() > state.getLivingSpace())
		{
			setBewohner(state.getLivingSpace());
		}

		if(usefullMessage)
		{
			message += "\n";
		}
		else
		{
			message = "";
		}

		return message;
	}

	/**
	 * Ueberprueft alle Gebaeude und schaltet bei nicht vorhandenen Voraussetzungen ab.
	 * @return Gibt eine Meldung mit allen abgeschalteten Gebaeuden zurueck
	 */
	private String proofBuildings()
	{
		User owner = getOwner();
		String msg = "";

		if( (getCore() > 0) && isCoreActive() ) {
			Core core = Core.getCore(getCore());

			if( core.isShutDown() && !owner.hasResearched(core.getTechRequired()) )
			{
				setCoreActive(false);
				msg += "Der Core wurde wegen unzureichenden Voraussetzungen abgeschaltet.\n";
			}
		}

		Integer[] bebauung = getBebauung();
		Integer[] bebon = getActive();

		for( int o=0; o < getWidth() * getHeight(); o++ )
		{
			if( bebauung[o] == 0 )
			{
				continue;
			}

			Building building = Building.getBuilding(bebauung[o]);

			if( bebon[o] == 0 )
			{
				continue;
			}

			if( building.isShutDown() &&
					(!owner.hasResearched(building.getTechRequired())
							|| (owner.getRace() != building.getRace() && building.getRace() != 0)))
			{
				bebon[o] = 0;
				msg += "Das Geb&auml;ude "+building.getName()+" wurde wegen unzureichenden Voraussetzungen abgeschaltet.\n";
			}
		}

		setActive(bebon);

		return msg;
	}

	/**
	 * Enforces the automatic sale rules of the base.
	 *
	 * @return The money for resource sales.
	 */
	private long automaticSale()
	{
		long money = 0;
		List<AutoGTUAction> actions = getAutoGTUActs();
		if(!actions.isEmpty() )
		{
			for(AutoGTUAction action: actions)
			{

				ResourceID resource = action.getResID();

				long sell;
				switch(action.getActID())
				{
					case AutoGTUAction.SELL_ALL:
						sell = action.getCount();
						if(sell > cargo.getResourceCount(resource))
						{
							sell = cargo.getResourceCount(resource);
						}
						break;
					case AutoGTUAction.SELL_TO_LIMIT:
						long maximum = action.getCount();
						sell = cargo.getResourceCount(resource) - maximum;
						break;
					default:
						sell = 0;
				}

				if(sell > 0)
				{
					cargo.substractResource(resource, sell);
					money += getSalePrice(resource, sell);
				}
			}
		}

		return money;
	}

	/**
	 * Enforces the maximum cargo rules.
	 *
	 * @param state Der Status der Basis
	 * @return The money for resource sales.
	 */
	private boolean clearOverfullCargo(BaseStatus state)
	{
		long maxCargo = getMaxCargo();
		long surplus = cargo.getMass() - maxCargo;

		if(surplus > 0)
		{
			ResourceList production = state.getProduction().getResourceList();
			production.sortByCargo(true);
			for(ResourceEntry resource: production)
			{
				//Only sell produced resources, not consumed
				long resourceCount = resource.getCount1();
				if(resourceCount < 0)
				{
					continue;
				}

				long productionMass = Cargo.getResourceMass(resource.getId(), resourceCount);
                if(productionMass == 0)
                {
                    continue;
                }

                //Remove only as much as needed, not more
				long toSell;
				if(productionMass <= surplus)
				{
					toSell = resourceCount;
				}
				else
				{
					long resourceMass = Cargo.getResourceMass(resource.getId(), 1);
					toSell = (long)Math.ceil((double)surplus/(double)resourceMass);
				}

				cargo.substractResource(resource.getId(), toSell);
				surplus = cargo.getMass() - maxCargo;

				if(cargo.getMass() <= maxCargo)
				{
					return true;
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Calculates the money using the current gtu price for base sales.
	 * @param resource Die ID der Ressource
	 * @param count Die Anzahl
	 * @return The money for a base sale of the resource.
	 */
	private long getSalePrice(ResourceID resource, long count)
	{
		GtuWarenKurse kurs = (GtuWarenKurse)getDB().get(GtuWarenKurse.class, "asti");
		Cargo prices = kurs.getKurse();
		double price = prices.getResourceCount(resource) / 1000d;

		long pay = Math.round(price * count);
		return pay;
	}

	private boolean rebalanceEnergy(BaseStatus state)
	{
		int energy = getEnergy();
		int eps = getMaxEnergy();
		int production = state.getEnergy();

		energy = energy + production;
		if(energy < 0)
		{
			return false;
		}

		if(energy > eps)
		{
			long overflow = energy - eps;
			long emptyBatteries = cargo.getResourceCount(Resources.LBATTERIEN);
			if(emptyBatteries < overflow)
			{
				overflow = emptyBatteries;
			}

			cargo.substractResource(Resources.LBATTERIEN, overflow);
			cargo.addResource(Resources.BATTERIEN, overflow);
			energy = eps;
		}

		setEnergy(energy);
		return true;
	}

	private String produce(BaseStatus state)
	{
		String msg = "";
		Cargo baseCargo = (Cargo)cargo.clone();
		Cargo nettoproduction = getNettoProduction();
		Cargo nettoconsumption = getNettoConsumption();
		org.hibernate.Session db = getDB();
		boolean ok = true;

		if(state.getArbeiter() > getBewohner())
		{
			return "Sie haben mehr Arbeiter als (maximal)Bev&ouml;lkerung. Die Produktion f&auml;llt aus.";
		}

		baseCargo.addResource(Resources.RE, getOwner().getKonto().longValue());

		for(ResourceEntry entry : nettoproduction.getResourceList())
		{
			// Auf Spawn Resource pruefen und ggf Produktion anpassen
			Item item = (Item)db.get(Item.class,entry.getId().getItemID());
			if(item.isSpawnableRess()) {
				// Genug auf dem Asteroiden vorhanden
				// und abziehen
				if(getSpawnableRessAmount(item.getID()) > nettoproduction.getResourceCount(entry.getId())) {
					setSpawnableRessAmount(item.getID(), getSpawnableRessAmount(item.getID()) - nettoproduction.getResourceCount(entry.getId()));
				}
				// Ueberhaupt nichts auf dem Asteroiden vorhanden
				else if (getSpawnableRessAmount(item.getID()) == 0) {
					// Dann ziehen wir die Production eben ab
					nettoproduction.setResource(entry.getId(), 0);
					msg += "Ihre Arbeiter konnten keine Vorkommen der Ressource "+item.getName()+" finden.\n";
				}
				// Es kann nicht mehr die volle Produktion gefoerdert werden
				else {
					// Produktion drosseln und neue Ressource spawnen
					nettoproduction.setResource(entry.getId(), getSpawnableRessAmount(item.getID()));
					respawnRess(item.getID());
				}
			}
		}
		Cargo fullproduction = (Cargo)nettoproduction.clone();
		fullproduction.substractCargo(nettoconsumption);

		ResourceList resources = baseCargo.compare(fullproduction, true);
		for(ResourceEntry entry: resources)
		{
			long stock = entry.getCount1();
			long production = entry.getCount2();

			long balance = stock + production;

			//Not enough resources for production
			if(balance < 0)
			{
				msg += "Zu wenig "+entry.getPlainName()+" vorhanden. Die Produktion f&auml;llt aus.\n";
				ok = false;
			}

			if(production > 0)
			{
				baseCargo.addResource(entry.getId(), production);
			}
			else
			{
				production = Math.abs(production);
				baseCargo.substractResource(entry.getId(), production);
			}
		}

		//Try to take all of the RE from the pool
		//Only use the asteroid cargo if there's no choice
		long baseRE = cargo.getResourceCount(Resources.RE);

		long newRE = baseCargo.getResourceCount(Resources.RE);

		if(newRE > baseRE)
		{
			getOwner().setKonto(BigInteger.valueOf(newRE - baseRE));
			baseCargo.setResource(Resources.RE, baseRE);
		}
		else
		{
			getOwner().setKonto(BigInteger.ZERO);
		}

		if(!feedInhabitants(baseCargo))
		{
			msg += "Wegen einer Hungersnot fliehen ihre Einwohner. Die Produktion f&auml;llt aus.\n";
			ok = false;
		}

		// Zuerst sollen die Marines verhungern danach die Bevoelkerung.
		if(!feedMarines(baseCargo))
		{
			msg += "Wegen Unterern&auml;hrung desertieren ihre Truppen.\n";
		}

		if(ok)
		{
			this.cargo = baseCargo;
		}
		return msg;
	}

	private void immigrate(BaseStatus state)
	{
		int inhabitants = getBewohner();
		int maxInhabitants = state.getLivingSpace();

		if(maxInhabitants > inhabitants)
		{
			int immigrants = maxInhabitants - inhabitants;

			Session db = getDB();
			ConfigValue immigrationFactorValue = (ConfigValue)db.get(ConfigValue.class, "immigrationfactor");
			ConfigValue randomizeImmigrationValue = (ConfigValue)db.get(ConfigValue.class, "randomizeimmigration");

			double immigrationFactor = Double.valueOf(immigrationFactorValue.getValue());
			boolean randomizeImmigration = Boolean.parseBoolean(randomizeImmigrationValue.getValue());

			immigrants *= immigrationFactor;
			if(randomizeImmigration)
			{
				immigrants = RandomUtils.nextInt(immigrants);
				if(immigrants == 0)
				{
					immigrants = 1;
				}
			}

			setBewohner(getBewohner() + immigrants);
		}
	}

	private boolean feedMarines(Cargo baseCargo)
	{
		int hungryPeople = (int)Math.ceil(getUnits().getNahrung() / 10);
		int fleeingPeople = feedPeople(hungryPeople, baseCargo);

		if(fleeingPeople > 0)
		{
			getUnits().fleeUnits(fleeingPeople);
			return false;
		}

		return true;
	}


	private boolean feedInhabitants(Cargo baseCargo)
	{
		int hungryPeople = (int)Math.ceil(getBewohner() / 10);
		int fleeingPeople = feedPeople(hungryPeople, baseCargo);

		if(fleeingPeople > 0)
		{
			setBewohner(getBewohner() - fleeingPeople);
			return false;
		}

		return true;
	}

	/**
	 * Tries to feed hungry people.
	 *
	 * @param hungryPeople People, which should be feed.
	 * @param baseCargo Der Cargo aus dem die Bewohner versorgt werden sollen
	 * @return People, which got no food.
	 */
	private int feedPeople(int hungryPeople, Cargo baseCargo)
	{
		long food = baseCargo.getResourceCount(Resources.NAHRUNG);
		if(food > hungryPeople)
		{
			food -= hungryPeople;
			hungryPeople = 0;
		}
		else
		{
			hungryPeople -= food;
			food = 0;
		}
		baseCargo.setResource(Resources.NAHRUNG, food);
		return hungryPeople;
	}

	private Session getDB()
	{
		return ContextMap.getContext().getDB();
	}

	private int getSpawnableRessAmount(int itemid)
	{
		if(getAvailableSpawnableRess() == null) {
			return 0;
		}
		String[] spawnress = StringUtils.split(getAvailableSpawnableRess(), ";");
		for(int i = 0; i < spawnress.length; i++) {
			String[] thisress = StringUtils.split(spawnress[i], ",");
			if(Integer.valueOf(thisress[0]) == itemid) {
				return Integer.valueOf(thisress[1]);
			}
		}
		return 0;
	}

	private void setSpawnableRessAmount(int itemid, long value)
	{
		if(getAvailableSpawnableRess() == null)
		{
			return;
		}
		String[] spawnress = StringUtils.split(getAvailableSpawnableRess(), ";");
		String newspawnress = "";
		boolean found = false;
		for(int i = 0; i < spawnress.length; i++)
		{
			String[] thisress = StringUtils.split(spawnress[i], ",");
			if(Integer.valueOf(thisress[0]) == itemid) {
				found = true;
				if( value > 0)
				{
					newspawnress = newspawnress + itemid + "," + value + ";";
				}
			}
			else
			{
				newspawnress = newspawnress + itemid + "," + thisress[1] + ";";
			}
		}
		if(!found)
		{
			newspawnress = newspawnress + itemid + "," + value + ";";
		}
		newspawnress = StringUtils.substring(newspawnress, 0, newspawnress.length() - 1);
		this.spawnressavailable = newspawnress;
	}

	private void respawnRess(int itemid)
	{
		org.hibernate.Session db = getDB();
		User sourceUser = (User)db.get(User.class, -1);

		setSpawnableRessAmount(itemid, 0);

		Map<Integer,Integer[]> spawnableress = getSpawnableRessMap();
		if(spawnableress == null || spawnableress.isEmpty())
		{
			return;
		}
		int chance = RandomUtils.nextInt(99) + 1;
		Integer[] spawnress = spawnableress.get(chance);
		int item = spawnress[0];
		int maxvalue = RandomUtils.nextInt(spawnress[1]-1)+1;

		setSpawnableRessAmount(item, maxvalue);

		Item olditem = (Item)db.get(Item.class, itemid);
		Item newitem = (Item)db.get(Item.class, item);
		String message = "Kolonie: " + this.getName() + " (" + this.getId() + ")\n";
		message = message + "Ihre Arbeiter melden: Die Ressource " + olditem.getName() + " wurde aufgebraucht!\n";
		message = message + "Erfreulich ist: Ihre Geologen haben " + newitem.getName() + " gefunden!";

		PM.send(sourceUser, this.getOwner().getId(), "Ressourcen aufgebraucht!", message);
	}

	/**
	 * Ermittelt alle Basen eines Nutzers an einer gegebenen Position.
	 * @param loc Die Position
	 * @param besitzer Der Besitzer
	 * @return Die Liste der Basen
	 */
	public static List<Base> byLocationAndBesitzer(Location loc, User besitzer)
	{
		List<Base> bases = new ArrayList<Base>();
		for( Base base : besitzer.getBases() )
		{
			if( base.getLocation().equals(loc) )
			{
				bases.add(base);
			}
		}
		return bases;
	}
}
