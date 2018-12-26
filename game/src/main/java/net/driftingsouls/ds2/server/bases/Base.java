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

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.*;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.*;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.units.BaseUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.classic.Lifecycle;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * <p>Repraesentiert eine Basis in DS.</p>
 *
 * @author Christopher Jung
 */
@Entity
@Table(name="bases")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@BatchSize(size=50)
@org.hibernate.annotations.Table(
	appliesTo = "bases",
	indexes = {@Index(name="owner", columnNames = {"owner", "id"}), @Index(name="coords", columnNames = {"x", "y", "system"})}
)
public class Base implements Cloneable, Lifecycle, Locatable, Transfering, Feeding
{
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="owner", nullable=false)
	@ForeignKey(name="bases_fk_users")
	private User owner;
	private int x;
	private int y;
	private int system;
	private int bewohner;
	private int arbeiter;
	@Column(name="e", nullable = false)
	private int energy;
	@Column(name="maxe", nullable = false)
	private int maxEnergy;
	@Column(nullable = false)
	@Type(type="largeCargo")
	private Cargo cargo;
	@Column(name="maxcargo", nullable = false)
	private long maxCargo;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="bases_fk_core")
	private Core core;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="klasse", nullable=false)
	@ForeignKey(name="bases_fk_basetypes")
	private BaseType klasse;
	private int width;
	private int height;
	@Column(name="maxtiles", nullable = false)
	private int maxTiles;
	private int size;
	@Lob
	@Column(nullable = false)
	private String terrain;
	@Lob
	@Column(nullable = false)
	private String bebauung;
	@Lob
	@Column(nullable = false)
	private String active;
	@Column(name="coreactive", nullable = false)
	private int coreActive;
	@Lob
	@Column(name="autogtuacts", nullable = false)
	private String autoGtuActs;
	@Lob
	private String spawnableress;
	@Lob
	private String spawnressavailable;
	private boolean isloading;
	@Index(name="idx_feeding")
	private boolean isfeeding;

	@OneToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH, CascadeType.REMOVE})
	@JoinColumn
	@ForeignKey(name="bases_fk_academy")
	private Academy academy;
	@OneToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH, CascadeType.REMOVE})
	@JoinColumn
	@ForeignKey(name="bases_fk_fz")
	private Forschungszentrum forschungszentrum;
	@OneToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH, CascadeType.REMOVE})
	@JoinColumn
	@ForeignKey(name="bases_fk_werften")
	private BaseWerft werft;
	@OneToMany(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH, CascadeType.REMOVE})
	@JoinColumn(name="col")
	private Set<Factory> factories;

	@OneToMany(fetch=FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "stationiertAufBasis")
	private Set<Offizier> offiziere = new HashSet<>();

	@OneToMany(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.DETACH, CascadeType.REMOVE},
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
	public Base(Location loc, User owner, BaseType klasse)
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
		this.units = new HashSet<>();
		this.klasse = klasse;
		this.size = klasse.getSize();
		this.width = klasse.getWidth();
		this.height = klasse.getHeight();
		this.maxCargo = klasse.getCargo();
		this.maxEnergy = klasse.getEnergy();
		this.spawnressavailable = klasse.getSpawnableRess();
		this.maxTiles = klasse.getMaxTiles();
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
	 * Gibt die installierte Core der Basis zurueck.
	 * Falls es keine Core auf der Basis gibt, so wird <code>null</code> zurueckgegeben.
	 *
	 * @return Die Core
	 */
	public Core getCore()
	{
		return this.core;
	}

	/**
	 * Setzt den neuen Kern der Basis. <code>null</code> bedeutet,
	 * dass kein Kern vorhanden ist.
	 *
	 * @param core der neue Kern oder <code>null</code>
	 */
	public void setCore(Core core)
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
		List<UnitCargoEntry> entries = new ArrayList<>(units);
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
	 * Gibt die Klassennummer der Basis zurueck (= Der Astityp).
	 * @return Die Klassennummer
	 */
	public BaseType getKlasse()
	{
		return this.klasse;
	}

	/**
	 * Setzt die Klasse der Basis.
	 * @param klasse Die Klasse
	 */
	public void setKlasse(BaseType klasse)
	{
		this.klasse = klasse;
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
	// TODO: Sollte aus dem BaseType ermittelt werden
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
		List<AutoGTUAction> acts = new ArrayList<>();
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

	public static class SpawnableRess
	{
		public final int itemId;
		public final int chance;
		public final int maxValue;

		SpawnableRess(int itemId, int chance, int maxValue)
		{
			this.itemId = itemId;
			this.chance = chance;
			this.maxValue = maxValue;
		}
	}

	public static class SpawnableRessMap
	{
		private Map<Integer,SpawnableRess> chanceMap;
		private Map<Integer,SpawnableRess> itemMap;
		private int totalChance;

		SpawnableRessMap()
		{
			this.chanceMap = new LinkedHashMap<>();
			this.itemMap = new LinkedHashMap<>();
			this.totalChance = 0;
		}

		void addSpawnRess(SpawnableRess spawnRess)
		{
			this.itemMap.put(spawnRess.itemId, spawnRess);
			this.totalChance += spawnRess.chance;
		}

		void buildChanceMap()
		{
			final double chancefactor = 100 / (double)this.totalChance;
			int chances = 1;
			for (SpawnableRess thisress : this.itemMap.values())
			{
				int chance = (int) Math.round((double) thisress.chance * chancefactor);
				int itemid = thisress.itemId;
				int maxvalue = thisress.maxValue;
				for (int a = chances; a <= chance + chances; a++)
				{
					chanceMap.put(a, new SpawnableRess(itemid, 1, maxvalue));
				}
				chances += chance + 1;
			}
		}

		public SpawnableRess newRandomRess()
		{
			int chance = 1;
			if(chanceMap.size() > 1) {
				chance = ThreadLocalRandom.current().nextInt(1, chanceMap.size());
				return this.chanceMap.get(chance);
			}

			return chanceMap.get(chance);
		}

		public boolean isEmpty()
		{
			return this.itemMap.isEmpty();
		}

		public boolean containsRess(Item item)
		{
			return this.itemMap.containsKey(item.getID());
		}
	}

	/**
	 * Gibt die zum spawn freigegebenen Ressourcen zurueck.
	 * Beruecksichtigt ebenfalls die Systemvorraussetzungen.
	 * @return Die zum Spawn freigegebenen Ressourcen
	 */
	public SpawnableRessMap getSpawnableRessMap()
	{
		org.hibernate.Session db = getDB();
		StarSystem system = (StarSystem)db.get(StarSystem.class, this.system);

		if(system == null) {
			return null;
		}

		if(getSpawnableRess() == null && system.getSpawnableRess() == null && getKlasse().getSpawnableRess() == null)
		{
			return null;
		}

		SpawnableRessMap spawnMap = new SpawnableRessMap();

		if( getSpawnableRess() != null )
		{
			String[] spawnableress = StringUtils.split(getSpawnableRess(), ";");
			for (String spawnableres : spawnableress)
			{
				String[] thisress = StringUtils.split(spawnableres, ",");
				if( thisress.length != 3 )
				{
					continue;
				}
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if (getSpawnableRessAmount(itemid) <= 0)
				{
					spawnMap.addSpawnRess(new SpawnableRess(itemid, chance, maxvalue));
				}
			}
		}
		if( system.getSpawnableRess() != null )
		{
			String[] spawnableresssystem = StringUtils.split(system.getSpawnableRess(), ";");
			for (String aSpawnableresssystem : spawnableresssystem)
			{
				String[] thisress = StringUtils.split(aSpawnableresssystem, ",");
				if( thisress.length != 3 )
				{
					continue;
				}
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if (getSpawnableRessAmount(itemid) <= 0)
				{
					spawnMap.addSpawnRess(new SpawnableRess(itemid, chance, maxvalue));
				}
			}
		}
		if( getKlasse().getSpawnableRess() != null && !getKlasse().getSpawnableRess().isEmpty() )
		{
			String[] spawnableresstype = StringUtils.split(getKlasse().getSpawnableRess(), ";");
			for (String aSpawnableresstype : spawnableresstype)
			{
				String[] thisress = StringUtils.split(aSpawnableresstype, ",");
				if( thisress.length != 3 )
				{
					continue;
				}
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if (getSpawnableRessAmount(itemid) <= 0)
				{
					spawnMap.addSpawnRess(new SpawnableRess(itemid, chance, maxvalue));
				}
			}
		}

		spawnMap.buildChanceMap();

		return spawnMap;
	}

	/**
	 * Gibt die aktuell verfuegbare Ressourcenmenge der Spawnable-Ressourcen zurueck.
	 * @return Die Ressourcenmengen als String (itemid,menge;itemid,menge)
	 */
	public String getAvailableSpawnableRess()
	{
		if( "null".equals(this.spawnressavailable))
		{
			return null;
		}
		else if( this.spawnressavailable == null )
		{
			return "";
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
	 * Generiert den aktuellen Verbrauch/Produktion-Status einer Basis.
	 * @param base die Basis
	 * @return der aktuelle Verbrauchs/Produktions-Status
	 */
	public static BaseStatus getStatus( Base base )
	{

        Fabrik.ContextVars vars = ContextMap.getContext().get(Fabrik.ContextVars.class);
        vars.clear();

		Cargo stat = new Cargo();
        Cargo prodstat = new Cargo();
        Cargo constat = new Cargo();

		int e = 0;
		int arbeiter = 0;
		int bewohner = 0;
		Map<Integer,Integer> buildinglocs = new TreeMap<>();

		if( (base.getCore() != null) && base.isCoreActive() ) {
			Core core = base.getCore();

			stat.substractCargo(core.getConsumes());
            constat.addCargo(core.getConsumes());
			stat.addCargo(core.getProduces());
            prodstat.addCargo(core.getProduces());

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
            building.modifyProductionStats(base, prodstat, bebauung[o]);
            building.modifyConsumptionStats(base, constat, bebauung[o]);

			stat.substractCargo(building.getConsumes());
            constat.addCargo(building.getConsumes());
			stat.addCargo(building.getAllProduces());
            prodstat.addCargo(building.getProduces());

			e = e - building.getEVerbrauch() + building.getEProduktion();
			arbeiter += building.getArbeiter();
			bewohner += building.getBewohner();
		}

        // Nahrung nicht mit in constat rein. Dies wird im Tick benutzt, der betrachtet Nahrungsverbrauch aber separat.
		stat.substractResource( Resources.NAHRUNG, (long)Math.ceil(base.getBewohner()/10.0) );
		stat.substractResource( Resources.NAHRUNG, base.getUnits().getNahrung() );
        // RE nicht mit in constat rein. Dies wird im Tick benutzt, der betrachtet RE-Verbrauch aber separat.
		stat.substractResource( Resources.RE, base.getUnits().getRE() );

		return new BaseStatus(stat, prodstat, constat, e, bewohner, arbeiter, Collections.unmodifiableMap(buildinglocs), bebon);
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

			base.autoGtuActsObj = new ArrayList<>();
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
		List<AutoGTUAction> acts = new ArrayList<>();
		for (String autogtuact : autogtuacts)
		{
			String[] split = StringUtils.split(autogtuact, ":");
			ResourceID rid = Resources.fromString(split[0]);
			if (rid != null)
			{
				acts.add(new AutoGTUAction(rid, Integer.parseInt(split[1]), Long.parseLong(split[2])));
			}
		}
		this.autoGtuActsObj = acts;

		boolean update = false;

		// Ggf die Feldergroessen fixen
		if( getTerrain().length < getWidth()*getHeight() )
		{
			Integer[] terrain = new Integer[getWidth()*getHeight()];
			System.arraycopy(getTerrain(), 0, terrain, 0, getTerrain().length );
			Integer[] allowedterrain = getKlasse().getTerrain();
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
				int rnd = ThreadLocalRandom.current().nextInt(allowedterrain.length);

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
		if( !getLocation().sameSector(this.size, ship, 0))
		{
			return 0;
		}

		//Only workless people can be transfered, when there is enough space on the ship
		int maxAmount = ship.getTypeData().getCrew() - ship.getCrew();
		int workless = Math.max(getBewohner() - getArbeiter(), 0);
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
			return "data/starmap/asti_own/asti_own.png";
		}
		else if(((getOwner().getId() != 0) && (user.getAlly() != null) && (getOwner().getAlly() == user.getAlly()) && user.getAlly().getShowAstis()) ||
				user.getRelations().isOnly(owner, User.Relation.FRIEND))
		{
			return "data/starmap/asti_ally/asti_ally.png";
		}
		else if(scanned && !getOwner().equals(nobody) && !getOwner().equals(zero))
		{
			return "data/starmap/asti_enemy/asti_enemy.png";
		}
		else
		{
			return null;
		}
	}

	/**
	 * Ermittelt den Offset in Sektoren fuer die Darstellung des von
	 * {@link #getBaseImage(net.driftingsouls.ds2.server.Location)} ermittelten Bildes.
	 * Der ermittelte Offset ist immer Negativ oder <code>0</code> und stellt
	 * die Verschiebung der Grafik selbst dar (vgl. CSS-Sprites).
	 *
	 * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
	 * @return Der Offset als Array (<code>[x,y]</code>)
	 */
	public int[] getBaseImageOffset(Location location)
	{
		return this.klasse.getSectorImageOffset(location, this.getLocation());
	}

	/**
	 * Gibt das Bild der Basis zurueck.
	 * Dabei werden Ausdehnung und Besitzer beruecksichtigt. Zudem
	 * kann das zurueckgelieferte Bild mehrere Sektoren umfassen. Der korrekte
	 * Offset zur Darstellung des angefragten Sektors kann mittels
	 * {@link #getBaseImageOffset(net.driftingsouls.ds2.server.Location)}
	 * ermittelt werden.
	 *
	 * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
	 * @return Der Bildstring der Basis oder einen Leerstring, wenn die Basis die Koordinaten nicht schneidet
	 */
	public String getBaseImage(Location location)
	{
		return this.klasse.getSectorImage(location, this.getLocation());
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

		for (Object ship1 : ships)
		{
			Ship ship = (Ship) ship1;
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
	 * Enforces the automatic sale rules of the base.
	 *
	 * @return The money for resource sales.
	 */
	public long automaticSale()
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
	public boolean clearOverfullCargo(BaseStatus state)
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

		return Math.round(price * count);
	}

	public int rebalanceEnergy(BaseStatus state)
	{
		int energy = getEnergy();
		int eps = getMaxEnergy();
        int production = state.getEnergy();

		energy = energy + production;
		if(energy < 0)
		{
			return -1;
		}

		if(energy > eps)
		{
        /*
			long overflow = energy - eps;
			long emptyBatteries = cargo.getResourceCount(Resources.LBATTERIEN);
			if(emptyBatteries < overflow)
			{
				overflow = emptyBatteries;
			}

			state.getNettoConsumption().addResource(Resources.LBATTERIEN, overflow);
			state.getNettoProduction().addResource(Resources.BATTERIEN, overflow);
            */
			energy = eps;
		}

		return energy;
	}

	public void immigrate(BaseStatus state)
	{
		int inhabitants = getBewohner();
		int maxInhabitants = state.getLivingSpace();

		if(maxInhabitants > inhabitants)
		{
			int immigrants = maxInhabitants - inhabitants;

			double immigrationFactor = new ConfigService().getValue(WellKnownConfigValue.IMMIGRATION_FACTOR);
			boolean randomizeImmigration = new ConfigService().getValue(WellKnownConfigValue.RANDOMIZE_IMMIGRATION);

			immigrants *= immigrationFactor;
			if(randomizeImmigration)
			{
				immigrants = ThreadLocalRandom.current().nextInt(immigrants);
				if(immigrants == 0)
				{
					immigrants = 1;
				}
			}

			setBewohner(getBewohner() + immigrants);
		}
	}

	public boolean feedMarines(Cargo baseCargo)
	{
		int hungryPeople = (int)Math.ceil(getUnits().getNahrung());
		int fleeingPeople = feedPeople(hungryPeople, baseCargo);

		if(fleeingPeople > 0)
		{
			getUnits().fleeUnits(fleeingPeople);
			return false;
		}

		return true;
	}

    public boolean payMarines(Cargo baseCargo)
    {
        long marinesold = getUnits().getRE();
        long re = baseCargo.getResourceCount(Resources.RE);

        if(marinesold > re)
        {
            getUnits().getMeuterer(re);
            baseCargo.setResource(Resources.RE, 0);
            return false;
        }
        else
        {
            baseCargo.substractResource(Resources.RE, marinesold);
        }
        return true;
    }


	public boolean feedInhabitants(Cargo baseCargo)
	{
		int hungryPeople = (int)Math.ceil(getBewohner() / 10);
		int fleeingPeople = feedPeople(hungryPeople, baseCargo);

		if(fleeingPeople > 0)
		{
			setBewohner(getBewohner() - fleeingPeople*10);
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

	public int getSpawnableRessAmount(int itemid)
	{
		if(getAvailableSpawnableRess() == null) {
			return 0;
		}
		String[] spawnress = StringUtils.split(getAvailableSpawnableRess(), ";");
		for (String spawnres : spawnress)
		{
			String[] thisress = StringUtils.split(spawnres, ",");
			if (Integer.valueOf(thisress[0]) == itemid)
			{
				return Integer.valueOf(thisress[1]);
			}
		}
		return 0;
	}

	public void setSpawnableRessAmount(int itemid, long value)
	{
		if(getAvailableSpawnableRess() == null)
		{
			return;
		}
		String[] spawnress = StringUtils.split(getAvailableSpawnableRess(), ";");
		String newspawnress = "";
		boolean found = false;
		for (String spawnres : spawnress)
		{
			String[] thisress = StringUtils.split(spawnres, ",");
			if (Integer.parseInt(thisress[0]) == itemid)
			{
				found = true;
				if (value > 0)
				{
					newspawnress = newspawnress + itemid + "," + value + ";";
				}
			}
			else if( thisress.length > 1 )
			{
				newspawnress = newspawnress + thisress[0] + "," + thisress[1] + ";";
			}
		}
		if(!found)
		{
			newspawnress = newspawnress + itemid + "," + value + ";";
		}
		newspawnress = StringUtils.substring(newspawnress, 0, newspawnress.length() - 1);
		this.spawnressavailable = newspawnress;
	}

	/**
	 * Ermittelt alle Basen eines Nutzers an einer gegebenen Position.
	 * @param loc Die Position
	 * @param besitzer Der Besitzer
	 * @return Die Liste der Basen
	 */
	public static List<Base> byLocationAndBesitzer(Location loc, User besitzer)
	{
		return besitzer.getBases().stream().filter(base -> base.getLocation().equals(loc)).collect(Collectors.toList());
	}

	/**
	 * Gibt alle auf der Basis stationierten Offiziere zurueck.
	 * @return Die Offiziere
	 */
	public Set<Offizier> getOffiziere()
	{
		return offiziere;
	}
}
