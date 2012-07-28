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
package net.driftingsouls.ds2.server.werften;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.IEDisableShip;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftShip;
import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Basisklasse fuer alle Werfttypen in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="werften")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="werfttype",discriminatorType=DiscriminatorType.CHAR)
@DiscriminatorValue("O")
public abstract class WerftObject extends DSObject implements Locatable {
	@Id @GeneratedValue
	private int id;
	@Column(name="flagschiff")
	private boolean buildFlagschiff = false;
	private int type = 0;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="linkedWerft", nullable=true)
	private WerftKomplex linkedWerft = null;

	@Version
	private int version;


	@Transient
	private Logger log = Logger.getLogger(WerftObject.class);

	/**
	 * Konstruktor.
	 *
	 */
	public WerftObject() {
		// EMPTY
	}

	/**
	 * Erstellt eine neue Werft.
	 * @param type Der Typ der Werft
	 */
	public WerftObject(int type) {
		this.type = type;
	}

	/**
	 * Gibt die aktuell zum Bau vorgesehenen Bauschlangeneintraege zurueck.
	 * @return Die Liste der zum Bauvorgesehenen Bauschlangeneintraege
	 */
	public WerftQueueEntry[] getScheduledQueueEntries() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<?> list = db.createQuery("from WerftQueueEntry where werft=:werft and scheduled=1 order by position")
			.setInteger("werft", this.getWerftID())
			.list();
		WerftQueueEntry[] entries = new WerftQueueEntry[list.size()];
		int index = 0;
		for( Iterator<?> iter=list.iterator(); iter.hasNext(); ) {
			entries[index++] = (WerftQueueEntry)iter.next();
		}

		return entries;
	}

	/**
	 * Gibt zurueck, ob in der Werft im Moment gebaut wird.
	 * @return <code>true</code>, falls gebaut wird
	 */
	public final boolean isBuilding() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return db.createQuery("from WerftQueueEntry where werft=:werft")
			.setInteger("werft", this.getWerftID())
			.iterate().hasNext();
	}

	/**
	 * Gibt zurueck, ob sich in der Bauschlange ein Flagschiff befindet.
	 * @return <code>true</code>, falls ein Flagschiff gebaut werden soll
	 */
	public boolean isBuildFlagschiff() {
		return this.buildFlagschiff;
	}

	/**
	 * Gibt die Anzahl der aktuell belegten Slots zurueck.
	 * @return Die Anzahl der belegten Slots
	 */
	public final int getUsedSlots() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return ((Number)db.createQuery("select sum(slots) from WerftQueueEntry where werft=:werft and scheduled=1")
			.setInteger("werft", this.getWerftID())
			.iterate().next()).intValue();
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem ein Bauauftrag voraussichtlich fertig sein wird.
	 * @param searched Der Bauauftrag
	 * @return Die Zeit in Ticks bis zur Fertigstellung
	 */
	public final int getTicksTillFinished(WerftQueueEntry searched) {
		List<WerftQueueEntry> entries = new ArrayList<WerftQueueEntry>();
		entries.addAll(Arrays.asList(getBuildQueue()));

		int slots = this.getWerftSlots();
		int time = 0;
		boolean first = true;

		SortedMap<Integer,List<WerftQueueEntry>> scheduled = new TreeMap<Integer,List<WerftQueueEntry>>();

		while( !entries.isEmpty() ) {
			for( int i=0; i < entries.size(); i++ ) {
				WerftQueueEntry entry = entries.get(i);

				if( (first && entry.isScheduled()) || (!first && entry.getSlots() <= slots) ) {
					if( entry == searched ) {
						return time+entry.getRemainingTime();
					}

					slots -= entries.get(i).getSlots();

					if( !scheduled.containsKey(time+entry.getRemainingTime()) ) {
						scheduled.put(time+entry.getRemainingTime(), new ArrayList<WerftQueueEntry>());
					}
					scheduled.get(time+entry.getRemainingTime()).add(entry);
					entries.remove(i--);
				}
			}

			if( first && scheduled.isEmpty() ) {
				first = false;
				continue;
			}
			else if( scheduled.isEmpty() ) {
				break;
			}

			first = false;

			time = scheduled.firstKey();
			List<WerftQueueEntry> remove = scheduled.remove(time);
			for( WerftQueueEntry entry : remove ) {
				slots += entry.getSlots();
			}
		}

		return -1;
	}

	/**
	 * Gibt den Typ der Werft zurueck.
	 * @return Typ der Werft
	 */
	public int getType() {
		return type;
	}

	/**
	 * Wird von Bauschlangeneintraegen aufgerufen, nachdem sie erfolgreich abgeschlossen wurden.
	 * Der Bauschlangeneintrag ist zu diesem Zeitpunkt bereits entfernt.
	 * @param shipid Die ID des neugebauten Schiffes
	 */
	protected void onFinishedBuildProcess(int shipid) {
		this.entries = null;
		rescheduleQueue();
	}

	/**
	 * Berechnet, welche Eintraege der Bauschlange im Moment gebaut werden und welche nicht.
	 *
	 */
	protected void rescheduleQueue() {
		int freeSlots = this.getWerftSlots();

		final Cargo cargo = new Cargo(this.getCargo(true));
		final User user = this.getOwner();

		if( user.getAlly() != null ) {
			Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );
			cargo.addCargo( allyitems );
		}

		final WerftQueueEntry[] entries = getBuildQueue();
		for( int i=0; i < entries.length; i++ ) {
			final WerftQueueEntry entry = entries[i];
			// Falls ein Item benoetigt wird pruefen, ob es vorhanden ist
			if( entry.getRequiredItem() != -1 ) {
				List<ItemCargoEntry> itemlist = cargo.getItem(entry.getRequiredItem());
				if( itemlist.size() == 0 ) {
					entry.setScheduled(false);
					continue;
				}

				// Item entfernen, damit ein Bauplan nicht mehrfach benutzt wird
				cargo.substractResource(itemlist.get(0).getResourceID(), 1);
			}

			if( entry.getSlots() > freeSlots ) {
				entry.setScheduled(false);
				continue;
			}

			entry.setScheduled(true);

			freeSlots -= entry.getSlots();
		}
	}

	/**
	 * Entfernt alle Eintraege aus der Bauschlange.
	 *
	 */
	public void clearQueue() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		db.createQuery("delete from WerftQueueEntry where werft=:werft")
			.setInteger("werft", this.getWerftID())
			.executeUpdate();

		this.buildFlagschiff = false;
		this.entries = null;
	}

	/**
	 * Gibt das Einweg-Flag der Werft zurueck.
	 * @return Das Einweg-Flag
	 */
	public int getOneWayFlag() {
		return 0;
	}

	/**
	 * Gibt die ID des Werfteintrags zurueck.
	 * @return Die ID des Werfteintrags
	 */
	public int getWerftID() {
		return id;
	}

	/**
	 * Gibt den Namen der Werft zurueck.
	 * @return Der Name
	 */
	public abstract String getWerftName();

	/**
	 * Gibt das Bild der Werft zurueck.
	 * @return Das Bild
	 */
	public abstract String getWerftPicture();

	/**
	 * Gibt die Anzahl an Werftslots zurueck, die der Werft zur Verfuegung stehen.
	 * @return Die Werftslots
	 */
	public abstract int getWerftSlots();

	/**
	 * Gibt den Besitzer der Werft zurueck.
	 * @return Der Besitzer
	 */
	public abstract User getOwner();

	/**
	 * Gibt den Cargo der Werft zurueck.
	 * @param localonly Soll nur der eigene (<code>true</code>) oder auch der Cargo von gekoppelten Objekten (<code>false</code>) genommen werden?
	 * @return Der Cargo der Werft
	 */
	public abstract Cargo getCargo(boolean localonly);

	/**
	 * Schreibt den Cargo der Werft wieder in die DB.
	 * @param cargo Der neue Cargo der Werft
	 * @param localonly Handelt es sich nur um den Cargo der Werft (<code>true</code>) oder auch um den Cargo von gekoppelten Objekten (<code>false</code>)?
	 */
	public abstract void setCargo(Cargo cargo, boolean localonly);

	/**
	 * Gibt die maximale Cargogroesse zurueck, den die Werft besitzen kann.
	 * @param localonly Soll nur der eigene (<code>true</code>) oder auch der Lagerplatz von gekoppelten Objekten (<code>false</code>) genommen werden?
	 * @return Die maximale Cargogroesse
	 */
	public abstract long getMaxCargo(boolean localonly);

	/**
	 * Gibt die vorhandene Crew zurueck.
	 * @return Die vorhandene Crew
	 */
	public abstract int getCrew();

	/**
	 * Gibt die maximale Crew der Werft zurueck.
	 * @return Die maximale Crew
	 */
	public abstract int getMaxCrew();

	/**
	 * Setzt die Crew der Werft auf den angegebenen Wert.
	 * @param crew Die neue Crew der Werft
	 */
	public abstract void setCrew(int crew);

	/**
	 * Gibt die vorhanene Energie der Werft zurueck.
	 * @return Die Energie auf der Werft
	 */
	public abstract int getEnergy();

	/**
	 * Setzt die vorhanene Energie auf der Werft auf den neuen Wert.<br>
	 * Annahme: Es kann nur weniger Energie werden - niemals mehr
	 * @param e Die neue Energie der Werft
	 */
	public abstract void setEnergy(int e);

	/**
	 * Gibt zurueck, wieviele Offiziere auf die Werft transferiert werden koennen.
	 * @return Die max. Anzahl an transferierbaren Offizieren
	 */
	public abstract int canTransferOffis();

	/**
	 * Transferiert den Offizier mit der angegebenen ID auf die Werft.
	 * @param offi Die ID des zu transferierenden Offiziers
	 */
	public abstract void transferOffi(int offi);

	/**
	 * Gibt die URL-Basis der Werft zurueck.
	 * @return Die URL-Basis
	 */
	public abstract String getUrlBase();

	/**
	 * Gibt einige versteckte Formfelder zurueck fuer Werftaufrufe via Forms.
	 * @return Einige versteckte Formfelder
	 */
	public abstract String getFormHidden();

	/**
	 * Gibt die X-Koordinate der Werft zurueck.
	 * @return Die X-Koordinate
	 */
	public abstract int getX();

	/**
	 * Gibt die Y-Koordinate der Werft zurueck.
	 * @return Die Y-Koordinate
	 */
	public abstract int getY();

	/**
	 * Gibt das System zurueck, in dem die Werft steht.
	 * @return Die ID des Systems
	 */
	public abstract int getSystem();

	/**
	 * Gibt den Namen der Werft zurueck.
	 * @return Der Name
	 */
	public abstract String getName();

	/**
	 * Gibt die URL zum Objekt zurueck, auf dem sich die Werft befindet.
	 * @return Die Url
	 */
	public abstract String getObjectUrl();

	/**
	 * Gibt den Anteil an verfuegbaren Arbeitern an der Menge
	 * der (eigendlich) notwendigen Arbeiter zurueck. Der Wert
	 * liegt zwischen 0 und 1.
	 * @return Der Anteil
	 */
	public abstract double getWorkerPercentageAvailable();

	/**
	 * Gibt den Radius der Werft zurueck.
	 * @return Der Radius
	 */
	public int getSize() {
		return 0;
	}

	/**
	 * Entfernt ein Item-Modul aus einem Schiff. Das Item-Modul
	 * befindet sich anschiessend auf der Werft.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param ship Das Schiff
	 * @param slot Modulslot, aus dem das Modul ausgebaut werden soll
	 *
	 */
	public void removeModule( Ship ship, int slot ) {
		Map<Integer,Integer> usedslots = new HashMap<Integer,Integer>();
		ModuleEntry[] modules = ship.getModules();
		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].getSlot(), i);
		}

		if( !usedslots.containsKey(slot) ) {
			MESSAGE.get().append("Es befindet sich kein Modul in diesem Slot\n");
			return;
		}

		ShipTypeData shiptype = ship.getTypeData();

		String[] aslot = null;

		String[] moduleslots = StringUtils.split(shiptype.getTypeModules(), ';');
		for( int i=0; i < moduleslots.length; i++ ) {
			String[] data = StringUtils.split(moduleslots[i], ':');

			if( Integer.parseInt(data[0]) == slot ) {
				aslot = data;
				break;
			}
		}

		if( aslot == null ) {
			MESSAGE.get().append("Keinen passenden Slot gefunden\n");
			return;
		}

		ShipTypeData oldshiptype = null;
		try {
			oldshiptype = (ShipTypeData)shiptype.clone();
		}
		catch( CloneNotSupportedException e ) {
			oldshiptype = shiptype;
		}

		ModuleEntry module = modules[usedslots.get(slot)];
		Module moduleobj = module.createModule();
		if( aslot.length > 2 ) {
			moduleobj.setSlotData(aslot[2]);
		}

		Cargo cargo = getCargo(false);

		if( moduleobj instanceof ModuleItemModule ) {
			ResourceID itemid = ((ModuleItemModule)moduleobj).getItemID();
			cargo.addResource( itemid, 1 );
		}
		ship.removeModule( module );

		moduleUpdateShipData(ship, oldshiptype, cargo);

		MESSAGE.get().append("Modul ausgebaut\n");
		return;
	}

	private void moduleUpdateShipData(Ship ship, ShipTypeData oldshiptype, Cargo cargo) {
		ShipTypeData shiptype = ship.getTypeData();

		if( ship.getHull() != shiptype.getHull() ) {
			double factor = ship.getHull() / (double)oldshiptype.getHull();
			ship.setHull((int)(shiptype.getHull() * factor));
		}

		if( ship.getHull() > shiptype.getHull() ) {
			ship.setHull(shiptype.getHull());
		}

		if( ship.getShields() > shiptype.getShields() ) {
			ship.setShields(shiptype.getShields());
		}

		if( ship.getAblativeArmor() != shiptype.getAblativeArmor() ) {
			double factor = 1;
			if(oldshiptype.getAblativeArmor() != 0)
			{
				factor = ship.getAblativeArmor() / (double)oldshiptype.getAblativeArmor();
			}
			ship.setAblativeArmor((int)(shiptype.getAblativeArmor() * factor));
		}

		if( ship.getAblativeArmor() > shiptype.getAblativeArmor() ) {
			ship.setAblativeArmor(shiptype.getAblativeArmor());
		}

		if( ship.getEnergy() > shiptype.getEps() ) {
			ship.setEnergy(shiptype.getEps());
		}

		if( ship.getCrew() > shiptype.getCrew() ) {
			ship.setCrew(shiptype.getCrew());
		}

		Cargo shipcargo = ship.getCargo();
		if( shipcargo.getMass() > shiptype.getCargo() ) {
			Cargo newshipcargo = shipcargo.cutCargo( shiptype.getCargo() );
			if( this.getMaxCargo(false) - cargo.getMass() > 0 ) {
				Cargo addwerftcargo = shipcargo.cutCargo( this.getMaxCargo(false) - cargo.getMass() );
				cargo.addCargo( addwerftcargo );
			}
			shipcargo = newshipcargo;
			ship.setCargo(shipcargo);
		}

		this.setCargo( cargo, false );

		StringBuilder output = MESSAGE.get();

		org.hibernate.Session db = ContextMap.getContext().getDB();

		int jdockcount = (int)ship.getLandedCount();
		if( jdockcount > shiptype.getJDocks() ) {
			int count = 0;

			// toArray(T[]) fuehrt hier leider zu Warnungen...
			Ship[] undockarray = new Ship[jdockcount-shiptype.getJDocks()];
			for( Ship lship : ship.getLandedShips() ) {
				undockarray[count++] = (Ship)lship;
				if( count >= undockarray.length )
				{
					break;
				}
			}

			output.append((jdockcount-shiptype.getJDocks())+" gelandete Schiffe wurden gestartet\n");

			ship.start(undockarray);
		}

		int adockcount = (int)ship.getDockedCount();
		if( adockcount > shiptype.getADocks() ) {
			int count = 0;

			// toArray(T[]) fuehrt hier leider zu Warnungen...
			Ship[] undockarray = new Ship[adockcount-shiptype.getADocks()];
			for( Ship lship : ship.getDockedShips() ) {
				undockarray[count++] = (Ship)lship;
				if( count >= undockarray.length )
				{
					break;
				}
			}

			output.append((adockcount-shiptype.getADocks())+" extern gedockte Schiffe wurden abgedockt\n");

			ship.dock(Ship.DockMode.UNDOCK, undockarray);
		}

		if( shiptype.getWerft() == 0 ) {
			db.createQuery("delete from ShipWerft where shipid=:ship")
				.setEntity("ship", ship)
				.executeUpdate();
		}
		else {
			ShipWerft w = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
				.setEntity("ship", ship)
				.uniqueResult();
			if( w == null ) {
				w = new ShipWerft(ship);
				db.persist(w);
			}
		}
	}

	//--------------------------------------------------------------------------------------------------------------

	/**
	 * Fuegt einem Schiff ein Item-Modul hinzu. Das Item-Modul
	 * muss auf der Werft vorhanden sein.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param ship Das Schiff
	 * @param slot Der Slot, in den das Modul eingebaut werden soll
	 * @param itemid Die ID des einzubauenden Item-Moduls
	 *
	 */
	public void addModule( Ship ship, int slot, int itemid ) {
		Map<Integer,Integer> usedslots = new HashMap<Integer,Integer>();
		ModuleEntry[] modules = ship.getModules();
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].getSlot(), i);
		}

		if( usedslots.containsKey(slot) ) {
			MESSAGE.get().append("Der Slot ist bereits belegt\n");
			return;
		}

		Cargo cargo = this.getCargo(false);
		List<ItemCargoEntry> itemlist = cargo.getItemsWithEffect( ItemEffect.Type.MODULE );

		ShipTypeData shiptype = ship.getTypeData();

		String[] aslot = null;

		String[] moduleslots = StringUtils.split(shiptype.getTypeModules(), ';');
		for( int i=0; i < moduleslots.length; i++ ) {
			String[] data = StringUtils.split(moduleslots[i], ':');

			if( Integer.parseInt(data[0]) == slot ) {
				aslot = data;
				break;
			}
		}

		if( aslot == null ) {
			MESSAGE.get().append("Keinen passenden Slot gefunden\n");
			return;
		}

		Item item = (Item)db.get(Item.class, itemid);
		if( (aslot == null) || !ModuleSlots.get().slot(aslot[1]).isMemberIn( ((IEModule)item.getEffect()).getSlots() ) ) {
			MESSAGE.get().append("Das Item passt nicht in dieses Slot\n");
			return;
		}

		if( item.getAccessLevel() > context.getActiveUser().getAccessLevel() ) {
			MESSAGE.get().append("Ihre Techniker wissen nichts mit dem Modul anzufangen\n");
			return;
		}

		ItemCargoEntry myitem = null;

		for( int i=0; i < itemlist.size(); i++ ) {
			if( itemlist.get(i).getItemID() == itemid ) {
				myitem = itemlist.get(i);
				break;
			}
		}

		if( myitem == null ) {
			MESSAGE.get().append("Kein passendes Item gefunden\n");
			return;
		}

		ShipTypeData oldshiptype = null;
		try {
			oldshiptype = (ShipTypeData)shiptype.clone();
		}
		catch( CloneNotSupportedException e ) {
			oldshiptype = shiptype;
		}

		ship.addModule( slot, ModuleType.ITEMMODULE, Integer.toString(itemid) );
		cargo.substractResource( myitem.getResourceID(), 1 );

		moduleUpdateShipData(ship, oldshiptype, cargo);

		MESSAGE.get().append("Modul eingebaut\n");

		return;
	}

	/**
	 * Berechnet den Cargo, den man beim Demontieren eines Schiffes zurueckbekommt. Er entspricht somit
	 * dem reinen Schrottwert des Schiffes :)
	 * Die aktuell geladenen Waren des Schiffes sind nicht teil des Cargos!
	 * @param ship Das Schiff
	 *
	 * @return Cargo mit den Resourcen
	 */
	public Cargo getDismantleCargo( Ship ship ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		ShipTypeData shiptype = ship.getTypeData();

		ShipBaubar baubar = (ShipBaubar)db.createQuery("from ShipBaubar where type=:type")
			.setInteger("type", ship.getType())
			.setMaxResults(1)
			.uniqueResult();

		//Kosten berechnen
		Cargo cost = new Cargo();

		if( baubar == null ) {
			double htr = ship.getHull()*0.0090;
			cost.addResource( Resources.KUNSTSTOFFE, (long)(htr/15) );
			cost.addResource( Resources.TITAN, (long)(htr/5) );
			cost.addResource( Resources.ADAMATIUM, (long)(htr/10) );
			cost.addResource( Resources.PLATIN,
					(long)(htr*(
							Math.floor((100-ship.getEngine())/2d) +
							Math.floor((100-ship.getSensors())/4d) +
							Math.floor((100-ship.getComm())/4d) +
							Math.floor((100-ship.getWeapons())/2d)
					)/900d) );
			cost.addResource( Resources.SILIZIUM,
					(long)(htr*(
							Math.floor((100-ship.getEngine())/6d) +
							Math.floor((100-ship.getSensors())/2d) +
							Math.floor((100-ship.getComm())/2d) +
							Math.floor((100-ship.getWeapons())/5d)
					)/1200d) );
			cost.addResource( Resources.KUNSTSTOFFE,
					(long)(
							cost.getResourceCount(Resources.SILIZIUM)+
							cost.getResourceCount(Resources.PLATIN)/2d+
							cost.getResourceCount(Resources.TITAN)/3d
					)*3);
		}
		else {
			Cargo buildcosts = baubar.getCosts();

			double factor = (ship.getHull()/(double)shiptype.getHull())*0.90d;
			cost.addResource( Resources.KUNSTSTOFFE, (long)(factor*buildcosts.getResourceCount(Resources.KUNSTSTOFFE)) );
			cost.addResource( Resources.TITAN, (long)(factor*buildcosts.getResourceCount(Resources.TITAN)) );
			cost.addResource( Resources.ADAMATIUM, (long)(factor*buildcosts.getResourceCount(Resources.ADAMATIUM)) );
			cost.addResource( Resources.PLATIN,
					(long)(( factor-
							((100-ship.getEngine())/200d)-
							((100-ship.getSensors())/400d)-
							((100-ship.getComm())/400d)-
							((100-ship.getWeapons())/200d)
						)*buildcosts.getResourceCount(Resources.PLATIN)) );
			cost.addResource( Resources.SILIZIUM,
					(long)(( factor-
							((100-ship.getEngine())/600d)-
							((100-ship.getSensors())/200d)-
							((100-ship.getComm())/200d)-
							((100-ship.getWeapons())/500d)
						)*buildcosts.getResourceCount(Resources.SILIZIUM)) );
			cost.addResource( Resources.XENTRONIUM,
					(long)(( factor-
							((100-ship.getEngine())/200d)-
							((100-ship.getSensors())/400d)-
							((100-ship.getComm())/400d)-
							((100-ship.getWeapons())/200d)
						)*buildcosts.getResourceCount(Resources.XENTRONIUM)) );
			cost.addResource( Resources.ISOCHIPS,
					(long)(( factor-
							((ship.getEngine()-100)/600d)-
							((100-ship.getSensors())/100d)-
							((100-ship.getComm())/100d)-
							((100-ship.getWeapons())/300d)
						)*buildcosts.getResourceCount(Resources.ISOCHIPS)) );
		}

		if( cost.getResourceCount( Resources.PLATIN ) <  0 ) {
			cost.setResource( Resources.PLATIN, 0 );
		}
		if( cost.getResourceCount( Resources.SILIZIUM ) <  0 ) {
			cost.setResource( Resources.SILIZIUM, 0 );
		}
		if( cost.getResourceCount( Resources.XENTRONIUM ) <  0 ) {
			cost.setResource( Resources.XENTRONIUM, 0 );
		}
		if( cost.getResourceCount( Resources.ISOCHIPS ) <  0 ) {
			cost.setResource( Resources.ISOCHIPS, 0 );
		}

		return cost;
	}


	/**
	 * Demontiert mehrere Schiffe auf einmal.
	 *
	 * @param ships Schiffe, die demontiert werden sollten.
	 * @return Anzahl der Schiffe, die wirklich demontiert wurde.
	 */
	public int dismantleShips(Collection<Ship> ships)
	{
		int dismantledShips = 0;
		for(Ship ship: ships)
		{
			if(dismantleShip(ship, false))
			{
				dismantledShips++;
			}
		}

		return dismantledShips;
	}

	/**
	 * Demontiert ein Schiff. Es wird dabei nicht ueberprueft, ob sich Schiff
	 * und Werft im selben Sektor befinden, ob das Schiff in einem Kampf ist usw sondern
	 * nur das demontieren selbst.{@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param ship Das zu demontierende Schiff
	 * @param testonly Soll nur geprueft (true) oder wirklich demontiert werden (false)?
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean dismantleShip(Ship ship, boolean testonly) {
		log.debug("Dismantling ship " + ship.getId());
		StringBuilder output = MESSAGE.get();

		if( ship.getId() < 0 ) {
			ContextMap.getContext().addError("Das angegebene Schiff existiert nicht");
			log.debug("Ship doesn't exist");
			return false;
		}

		if(this instanceof ShipWerft)
		{
			Ship shipyard = ((ShipWerft)this).getShip();
			if(ship.equals(shipyard))
			{
				log.debug("Shipyard tried to dismantle itself.");
				return false;
			}
		}

		Cargo scargo = ship.getCargo();

		Cargo cargo = this.getCargo(false);

	 	long maxcargo = this.getMaxCargo(false);

		Cargo cost = this.getDismantleCargo( ship );

		Cargo newcargo = (Cargo)cargo.clone();
		long totalcargo = cargo.getMass();

		boolean ok = true;

		cost.addCargo( scargo );
		newcargo.addCargo( cost );

		if( cost.getMass() + totalcargo > maxcargo ) {
			log.debug("Not enough space for resources.");
			output.append("Nicht gen&uuml;gend Platz f&uuml;r alle Waren\n");
			ok = false;
		}

		int maxoffis = this.canTransferOffis();

		List<Offizier> offiziere = Offizier.getOffiziereByDest('s', ship.getId());

		if( offiziere.size() > maxoffis ) {
			output.append("Nicht genug Platz f&uuml;r alle Offiziere");
			ok = false;
		}
		if( !ok ) {
			return false;
		}

		if( ok && !testonly ) {
			this.setCargo(newcargo, false);

			this.setCrew(this.getCrew()+ship.getCrew());
			if(this.getCrew() > this.getMaxCrew())
			{
				this.setCrew(this.getMaxCrew());
			}

			for( Offizier offi : offiziere ) {
				this.transferOffi(offi.getID());
			}

			ship.destroy();
		}

		return ok;
	}

	/**
	 * Die Reparaturkosten eines Schiffes.
	 *
	 */
	public static class RepairCosts {
		/**
		 * Die Energiekosten.
		 */
		public int e;
		/**
		 * Die Resourcenkosten.
		 */
		public Cargo cost;

		protected RepairCosts() {
			//EMPTY
		}
	}

	/**
	 * Berechnet die Reparaturkosten fuer ein Schiff.
	 * @param ship Das Schiff
	 *
	 * @return Die Reparaturkosten
	 */
	public RepairCosts getRepairCosts( Ship ship )
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		ShipTypeData shiptype = ship.getTypeData();
		RepairCosts repairCosts = new RepairCosts();

		Cargo materialCosts = new Cargo();
		ShipBaubar buildable = (ShipBaubar)db.createQuery("from ShipBaubar where type=:type")
											 .setInteger("type", ship.getType())
											 .setMaxResults(1)
											 .uniqueResult();

		if(buildable != null)
		{
			double ablativeDamageFactor = 0.0;
			if(shiptype.getAblativeArmor() > 0)
			{
				ablativeDamageFactor = 1 -  ship.getAblativeArmor() / (double)shiptype.getAblativeArmor();
			}

			double hullDamageFactor = 1 -  ship.getHull() / (double)shiptype.getHull();
			double systemDamageFactor = 1 - (ship.getEngine() / 100.0 + ship.getComm() / 100.0 + ship.getWeapons() / 100.0 + ship.getSensors() / 100.0) / 4.0;

			double outerDamageFactor = (ablativeDamageFactor + hullDamageFactor) / 2.0;
			double innerDamageFactor = (hullDamageFactor + systemDamageFactor) / 2.0;

			double damageFactor = outerDamageFactor * 0.7 + innerDamageFactor * 0.3;

			ConfigValue dampening = (ConfigValue)db.get(ConfigValue.class, "repaircostdampeningfactor");
			double dampeningFactor = Double.parseDouble(dampening.getValue());

			Cargo buildCosts = new Cargo(buildable.getCosts());
			ResourceList buildCostList = buildCosts.getResourceList();
			Iterator<ResourceEntry> resources = buildCostList.iterator();
			double costFactor = damageFactor * dampeningFactor;
			while(resources.hasNext())
			{
				ResourceEntry resource = resources.next();
				materialCosts.addResource(resource.getId(), (long)(Math.abs(resource.getCount1()) * costFactor));
			}

			repairCosts.e = (int)(buildable.getEKosten() * costFactor);
		}
		else
		{
			//Kosten berechnen
			int htr = shiptype.getHull()-ship.getHull();
			int htrsub = (int)Math.round(shiptype.getHull()*0.5d);
			int ablativeArmorToRepair = shiptype.getAblativeArmor() - ship.getAblativeArmor();

			if( htr > htrsub ) {
				htrsub = htr;
			}

			materialCosts.addResource( Resources.KUNSTSTOFFE, (long)(htr/55d) );
			materialCosts.addResource( Resources.TITAN, (long)(htr/20d) );
			materialCosts.addResource( Resources.ADAMATIUM, (long)(htr/40d) );
			materialCosts.addResource( Resources.PLATIN,
					(long)(htrsub/100d*(
							Math.floor(100-ship.getEngine()) +
							Math.floor((100-ship.getSensors())/4d) +
							Math.floor((100-ship.getComm())/4d) +
							Math.floor(100-ship.getWeapons())/2d
						)/106d) );
			materialCosts.addResource( Resources.SILIZIUM,
					(long)(htrsub/100d*(
							(100-ship.getEngine())/3 +
							(100-ship.getSensors())/2 +
							(100-ship.getComm())/2 +
							(100-ship.getWeapons())/5
						)/72d) );
			materialCosts.addResource( Resources.KUNSTSTOFFE,
					(long)((
							materialCosts.getResourceCount(Resources.SILIZIUM)+
							materialCosts.getResourceCount(Resources.PLATIN)/2d+
							materialCosts.getResourceCount(Resources.TITAN)/3d
						)*0.5d) );
			int energie = (int)Math.round(
					((long)(
							materialCosts.getResourceCount(Resources.SILIZIUM)+
							materialCosts.getResourceCount(Resources.PLATIN)/2d+
							materialCosts.getResourceCount(Resources.TITAN)/2d
					)*1.5d));

			if( energie > 900 ) {
				energie = 900;
			}

			materialCosts.addResource(Resources.URAN, ablativeArmorToRepair/100);
			materialCosts.addResource(Resources.TITAN, ablativeArmorToRepair/100);
			materialCosts.addResource(Resources.ADAMATIUM, ablativeArmorToRepair/200);

			repairCosts.e = energie;
		}

		repairCosts.cost = materialCosts;

		return repairCosts;

		/*
		//Kosten berechnen
		int htr = shiptype.getHull()-ship.getHull();
		int htrsub = (int)Math.round(shiptype.getHull()*0.5d);
		int ablativeArmorToRepair = shiptype.getAblativeArmor() - ship.getAblativeArmor();

		if( htr > htrsub ) {
			htrsub = htr;
		}

		Cargo cost = new Cargo();
		cost.addResource( Resources.KUNSTSTOFFE, (long)(htr/55d) );
		cost.addResource( Resources.TITAN, (long)(htr/20d) );
		cost.addResource( Resources.ADAMATIUM, (long)(htr/40d) );
		cost.addResource( Resources.PLATIN,
				(long)(htrsub/100d*(
						Math.floor(100-ship.getEngine()) +
						Math.floor((100-ship.getSensors())/4d) +
						Math.floor((100-ship.getComm())/4d) +
						Math.floor(100-ship.getWeapons())/2d
					)/106d) );
		cost.addResource( Resources.SILIZIUM,
				(long)(htrsub/100d*(
						(100-ship.getEngine())/3 +
						(100-ship.getSensors())/2 +
						(100-ship.getComm())/2 +
						(100-ship.getWeapons())/5
					)/72d) );
		cost.addResource( Resources.KUNSTSTOFFE,
				(long)((
						cost.getResourceCount(Resources.SILIZIUM)+
						cost.getResourceCount(Resources.PLATIN)/2d+
						cost.getResourceCount(Resources.TITAN)/3d
					)*0.5d) );
		int energie = (int)Math.round(
				((long)(
						cost.getResourceCount(Resources.SILIZIUM)+
						cost.getResourceCount(Resources.PLATIN)/2d+
						cost.getResourceCount(Resources.TITAN)/2d
				)*1.5d));

		if( energie > 900 ) {
			energie = 900;
		}

		cost.addResource(Resources.URAN, ablativeArmorToRepair/100);
		cost.addResource(Resources.TITAN, ablativeArmorToRepair/100);
		cost.addResource(Resources.ADAMATIUM, ablativeArmorToRepair/200);

		RepairCosts rc = new RepairCosts();
		rc.e = energie;
		rc.cost = cost;

		return rc;
		*/
	}

	/**
	 * Repariert ein Schiff auf einer Werft.
	 * Es werden nur Dinge geprueft, die unmittelbar mit dem Repariervorgang selbst
	 * etwas zu tun haben. Die Positionen von Schiff und Werft usw werden jedoch nicht gecheckt.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param ship Das Schiff
	 * @param testonly Soll nur getestet (true) oder auch wirklich repariert (false) werden?
	 *
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean repairShip(Ship ship, boolean testonly) {
        if(ship.hasFlag(Ship.FLAG_RECENTLY_REPAIRED))
        {
            MESSAGE.get().append("Das Schiff wurde k&uuml;rzlich repariert und kann derzeit nicht repariert werden.");
            return false;
        }

		ShipTypeData shiptype = ship.getTypeData();

		Cargo cargo = this.getCargo(false);
	    boolean ok = true;
		RepairCosts rc = this.getRepairCosts(ship);


		Cargo newcargo = (Cargo) cargo.clone();
		int newe = this.getEnergy();

		//Kosten ausgeben
		ResourceList reslist = rc.cost.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				ok = false;
			}
		}
		newcargo.substractCargo( rc.cost );

		if( rc.e > 0 ) {
			if( rc.e > newe ) {
				ok = false;
			}
			newe -= rc.e;
		}


		if( !ok ) {
			MESSAGE.get().append("Nicht gen&uuml;gend Material zur Reperatur vorhanden");
			return false;
		}
		else if( !testonly ) {
			this.setCargo( newcargo, false );

			this.setEnergy(newe);
			ship.setAblativeArmor(shiptype.getAblativeArmor());
			ship.setHull(shiptype.getHull());
			ship.setEngine(100);
			ship.setSensors(100);
			ship.setComm(100);
			ship.setWeapons(100);
            ship.addFlag(Ship.FLAG_RECENTLY_REPAIRED, 7);
		}
		return true;
	}

	/**
	 * Gibt die Schiffstypen zurueck, die auf dieser Werft gebaut werden koennen.
	 *
	 * @return Schiffstypen, die auf dieser Werft gebaut werden koennen.
	 */
	public Set<ShipType> getBuildableShips() {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User owner = this.getOwner();

		Set<ShipType> shipTypes = new HashSet<ShipType>();

		List<ShipBaubar> buildableShips = Common.cast(db.createQuery("from ShipBaubar where werftslots <= :slot")
			.setInteger("slot", this.getWerftSlots())
			.list());

		//Allowed by draft
		Set<IEDraftShip> drafts = getUsableShipDrafts();
		for(IEDraftShip draft: drafts) {
			shipTypes.add((ShipType)db.get(ShipType.class, draft.getShipType()));
		}


		//Filter parts which are not database checkable
		for(ShipBaubar buildableShip: buildableShips) {
			if(!Rassen.get().rasse(owner.getRace()).isMemberIn(buildableShip.getRace())) {
				continue;
			}

			if( !owner.hasResearched(buildableShip.getRes(1)) || !owner.hasResearched(buildableShip.getRes(2)) || !owner.hasResearched(buildableShip.getRes(3))) {
				continue;
			}

			shipTypes.add(buildableShip.getType());
		}

		//Filter disallowed shiptypes
		Set<ShipType> disabledShips = getDisabledShips();
		for (Iterator<ShipType> it = shipTypes.iterator(); it.hasNext(); ) {
			ShipType shipType = it.next();
			if(disabledShips.contains(shipType)) {
				it.remove();
				continue;
			}
		}

		return shipTypes;
	}

	/**
	 * Gibt alle Schiffstypen zurueck, die diese Werft derzeit nicht bauen kann.
	 *
	 * @return Schiffstypen, die nicht baubar sind.
	 */
	private Set<ShipType> getDisabledShips() {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Cargo availablecargo = this.getCargo(false);
		Cargo allyitems = getAllyItems();
		Set<ShipType> disabledShips = new HashSet<ShipType>();

		Cargo allitems = new Cargo();
		allitems.addCargo(allyitems);
		allitems.addCargo(availablecargo);

		for(ItemCargoEntry item: allitems.getItemsWithEffect( ItemEffect.Type.DISABLE_SHIP ))
		{
			IEDisableShip disableShip = (IEDisableShip)item.getItemEffect();
			disabledShips.add((ShipType)db.load(ShipType.class, disableShip.getShipType()));
		}

		return disabledShips;
	}

	private Set<ItemCargoEntry> getAllItems() {
		Cargo allyitems = getAllyItems();
		Cargo shipyardCargo = this.getCargo(false);

		Set<ItemCargoEntry> items = new HashSet<ItemCargoEntry>();
		items.addAll(allyitems.getItemsWithEffect(ItemEffect.Type.DRAFT_SHIP));
		items.addAll(shipyardCargo.getItemsWithEffect(ItemEffect.Type.DRAFT_SHIP));

		return items;
	}

	/**
	 * Gibt alle Bauplaene auf der Werft zurueck, die derzeit benutzt werden koennen.
	 *
	 * @return Nutzbare Bauplaene.
	 */
	private Set<IEDraftShip> getUsableShipDrafts() {
		User owner = this.getOwner();
		Set<IEDraftShip> shipDrafts = new HashSet<IEDraftShip>();


		//All Drafts - ally and local
		Set<ItemCargoEntry> items = getAllItems();

		for(ItemCargoEntry item: items) {
			if(item.getItemEffect().getType() != ItemEffect.Type.DRAFT_SHIP) {
				continue;
			}

			IEDraftShip draft = (IEDraftShip)item.getItemEffect();

			if( draft.getWerftSlots() > this.getWerftSlots() ) {
				continue;
			}

			if(!owner.hasResearched(draft.getTechReq(1)) || !owner.hasResearched(draft.getTechReq(2)) || !owner.hasResearched(draft.getTechReq(3))) {
				continue;
			}

			shipDrafts.add(draft);
		}

		return shipDrafts;
	}

	private Cargo getAllyItems() {
		User owner = this.getOwner();
		if( owner.getAlly() != null ) {
			return new Cargo(Cargo.Type.ITEMSTRING, owner.getAlly().getItems());
		}

		return new Cargo();
	}

	/**
	 * Liefert die Liste aller theoretisch baubaren Schiffe auf dieser Werft.
	 * Das vorhanden sein von Resourcen wird hierbei nicht beruecksichtigt.
	 * @return array mit Schiffsbaudaten (ships_baubar) sowie
	 * 			'_item' => array( ('local' | 'ally'), $resourceid) oder '_item' => false
	 * 			zur Bestimmung ob und wenn ja welcher Bauplan benoetigt wird zum bauen
	 */
	@Deprecated
	public SQLResultRow[] getBuildShipList() {
		List<SQLResultRow> result = new ArrayList<SQLResultRow>();

		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		User user = this.getOwner();

		String query = "SELECT t1.id,t1.race,t1.type,t1.dauer,t1.costs,t1.ekosten,t1.crew,t1.tr1,t1.tr2,t1.tr3,t1.flagschiff " +
			"FROM ships_baubar t1 JOIN ship_types t2 ON t1.type=t2.id " +
			"WHERE t1.werftslots<="+this.getWerftSlots()+" "+
			"ORDER BY t2.nickname";


		Cargo availablecargo = this.getCargo(false);

		Cargo allyitems = null;
		if( user.getAlly() != null ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING, user.getAlly().getItems());
		}
		else {
			allyitems = new Cargo();
		}

		Map<Integer,Boolean> disableShips = new HashMap<Integer,Boolean>();

		List<ItemCargoEntry> itemlist = availablecargo.getItemsWithEffect( ItemEffect.Type.DISABLE_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			IEDisableShip effect = (IEDisableShip)itemlist.get(i).getItemEffect();
			disableShips.put(effect.getShipType(), true);
		}

		itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DISABLE_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			IEDisableShip effect = (IEDisableShip)itemlist.get(i).getItemEffect();
			disableShips.put(effect.getShipType(), true);
		}

		SQLQuery shipdataRow = db.query(query);
		while( shipdataRow.next() ) {
			if( disableShips.containsKey(shipdataRow.getInt("type")) ) {
				continue;
			}
			if( !Rassen.get().rasse(user.getRace()).isMemberIn(shipdataRow.getInt("race")) ) {
				continue;
			}

			//Forschungen checken
			if( !user.hasResearched(shipdataRow.getInt("tr1")) ||
				!user.hasResearched(shipdataRow.getInt("tr2")) ||
				!user.hasResearched(shipdataRow.getInt("tr3"))) {
				continue;
			}

			SQLResultRow shipdata = shipdataRow.getRow();

			Cargo costs = new Cargo( Cargo.Type.AUTO, shipdata.getString("costs") );

			shipdata.put("costs", costs);
			shipdata.put("_item", false);
			result.add(shipdata);
		}
		shipdataRow.free();

		//Items
		Cargo localcargo = this.getCargo(true);
		itemlist = localcargo.getItemsWithEffect( ItemEffect.Type.DRAFT_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			ItemCargoEntry item = itemlist.get(i);
			IEDraftShip effect = (IEDraftShip)item.getItemEffect();

			if( effect.getWerftSlots() > this.getWerftSlots() ) {
				continue;
			}

			//Forschungen checken
			if(!user.hasResearched(effect.getTechReq(1)) || !user.hasResearched(effect.getTechReq(2)) || !user.hasResearched(effect.getTechReq(3))) {
				continue;
			}

			Cargo cost = effect.getBuildCosts();

			// TODO: Nicht schoen
			SQLResultRow shipdata = new SQLResultRow();
			shipdata.put("id", -1);
			shipdata.put("type", effect.getShipType());
			shipdata.put("costs", cost);
			shipdata.put("crew", effect.getCrew());
			shipdata.put("dauer", effect.getDauer());
			shipdata.put("ekosten", effect.getE());
			shipdata.put("race", effect.getRace());
			shipdata.put("tr1", effect.getTechReq(1));
			shipdata.put("tr2", effect.getTechReq(2));
			shipdata.put("tr3", effect.getTechReq(3));
			shipdata.put("werftslots", effect.getWerftSlots());
			shipdata.put("flagschiff", effect.isFlagschiff());
			shipdata.put("_item", new Object[] {"local", item.getResourceID()});

			result.add(shipdata);
		}

		itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DRAFT_SHIP );
		for( int i=0; i < itemlist.size(); i++ ) {
			ItemCargoEntry item = itemlist.get(i);
			IEDraftShip effect = (IEDraftShip)item.getItemEffect();

			if( effect.getWerftSlots() > this.getWerftSlots() ) {
				continue;
			}

			//Forschungen checken
			if(!user.hasResearched(effect.getTechReq(1)) || !user.hasResearched(effect.getTechReq(2)) || !user.hasResearched(effect.getTechReq(3))) {
				continue;
			}

			Cargo cost = effect.getBuildCosts();

			// TODO: Nicht schoen
			SQLResultRow shipdata = new SQLResultRow();
			shipdata.put("id", -1);
			shipdata.put("type", effect.getShipType());
			shipdata.put("costs", cost);
			shipdata.put("crew", effect.getCrew());
			shipdata.put("dauer", effect.getDauer());
			shipdata.put("ekosten", effect.getE());
			shipdata.put("race", effect.getRace());
			shipdata.put("tr1", effect.getTechReq(1));
			shipdata.put("tr2", effect.getTechReq(2));
			shipdata.put("tr3", effect.getTechReq(3));
			shipdata.put("werftslots", effect.getWerftSlots());
			shipdata.put("flagschiff", effect.isFlagschiff());
			shipdata.put("_item", new Object[] {"ally", item.getResourceID()});

			result.add(shipdata);
		}

		return result.toArray(new SQLResultRow[result.size()]);
	}

	/**
	 * Liefert die Schiffsbaudaten zu einer Kombination aus Schiffsbau-ID und/oder Item.
	 * Die Baukosten werden falls notwendig angepasst (linear ansteigende Kosten).
	 * Wenn keine passenden Schiffsbaudaten generiert werden koennen wird ein leeres
	 * Schiffsbaudatenarray zurueckgegeben. {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param build Die Schiffsbau-ID
	 * @param itemid Die Item-ID
	 *
	 * @return schiffsbaudaten
	 */
	public ShipBaubar getShipBuildData( int build, int itemid ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = this.getOwner();

		Cargo allyitems = null;
	   	if( user.getAlly() != null ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING,user.getAlly().getItems());
			Cargo localcargo = this.getCargo(true);

			allyitems.addCargo( localcargo );
		}
		else {
			Cargo localcargo = this.getCargo(true);
			allyitems = localcargo;
		}

	   	ShipBaubar shipdata = null;
		if( build > 0 ) {
			shipdata = (ShipBaubar)db.createQuery("from ShipBaubar where id=:id")
										.setInteger("id", build)
										.setMaxResults(1)
										.uniqueResult();
			if( shipdata == null ) {
				MESSAGE.get().append("Es wurde kein passender Schiffsbauplan gefunden");
				return null;
			}

			return shipdata;
		}

		int itemcount = allyitems.getItem( itemid ).size();

		if( itemcount == 0 ) {
			MESSAGE.get().append("Kein passendes Item vorhanden");
			return null;
		}
		Item item = (Item)db.get(Item.class, itemid);
		if( item.getEffect().getType() != ItemEffect.Type.DRAFT_SHIP ) {
		 	MESSAGE.get().append("Bei dem Item handelt es sich um keinen Schiffsbauplan");
		 	return null;
		}
		IEDraftShip effect = (IEDraftShip)item.getEffect();
		ShipType shiptype = (ShipType)db.get(ShipType.class, effect.getShipType());
		shipdata = new ShipBaubar(shiptype);
		shipdata.setCosts(effect.getBuildCosts());
		shipdata.setCrew(effect.getCrew());
		shipdata.setDauer(effect.getDauer());
		shipdata.setEKosten(effect.getE());
		shipdata.setRace(effect.getRace());
		shipdata.setRes1(effect.getTechReq(1));
		shipdata.setRes2(effect.getTechReq(2));
		shipdata.setRes3(effect.getTechReq(3));
		shipdata.setWerftSlots(effect.getWerftSlots());
		shipdata.setFlagschiff(effect.isFlagschiff());

		return shipdata;
	}

	/**
	 * Findet heraus welches Item zum Bau benoetigt wird und baut danach das Schiff.
	 * @param typeid Die ID des zu bauenden Schifftyps
	 * @param costsPerTick Sollen die Baukosten pro Tick (<code>true</code>) oder der Gesamtbetrag jetzt (<code>false</code>) abgezogen werden
	 * @param testOnly Soll nur getestet (true) oder wirklich gebaut (false) werden?
	 * @see WerftObject#buildShip
	 *
	 * @return true, wenn kein Fehler aufgetreten ist.
	 */
	public boolean buildShip(int typeid, boolean costsPerTick, boolean testOnly) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		int item = -1;
		ShipType type = (ShipType)db.load(ShipType.class, typeid);
		ShipBaubar ship = (ShipBaubar)db.createQuery("from ShipBaubar where type=:type")
			.setEntity("type", type)
			.uniqueResult();

		if(ship == null) {
			for(ItemCargoEntry entry: getAllItems()) {
				if(entry.getItemEffect().getType() == ItemEffect.Type.DRAFT_SHIP) {
					IEDraftShip draft = (IEDraftShip)entry.getItemEffect();
					if(draft.getShipType() == typeid) {
						item = entry.getItemID();
						break;
					}
				}
			}
		}
		else {
			item = 0;
		}

		//Cannot build without item, correct item not found
		if(item == -1) {
			return false;
		}

		if(item > 0) {
			return buildShip(0, item, costsPerTick, testOnly);
		}
		return buildShip(ship.getId(), item, costsPerTick, testOnly);
	}

	/**
	 * Baut ein Schiff in der Werft auf Basis der angegebenen Schiffbau-ID und der
	 * angegebenen Item-ID (Bauplan). {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param build Schiffbau-ID
	 * @param item Item-ID
	 * @param costsPerTick Sollen die Baukosten pro Tick (<code>true</code>) oder der Gesamtbetrag jetzt (<code>false</code>) abgezogen werden
	 * @param testonly Soll nur getestet (true) oder wirklich gebaut (false) werden?
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean buildShip( int build, int item, boolean costsPerTick, boolean testonly ) {
		StringBuilder output = MESSAGE.get();

		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = this.getOwner();

		Cargo cargo = new Cargo(this.getCargo(false));

	   	Cargo allyitems = null;
	   	if( user.getAlly() != null ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING,user.getAlly().getItems());

			allyitems.addCargo( this.getCargo(true) );
		}
		else {
			allyitems = this.getCargo(true);
		}

		ShipBaubar shipdata = this.getShipBuildData( build, item );
		if( (shipdata == null) ) {
			return false;
		}

		List<ItemCargoEntry> itemlist = allyitems.getItemsWithEffect( ItemEffect.Type.DISABLE_SHIP );
		for(int i=0; i < itemlist.size(); i++ ) {
			IEDisableShip effect = (IEDisableShip)itemlist.get(i).getItemEffect();

			if( effect.getShipType() == shipdata.getType().getId() ) {
				output.append("Ihnen wurde der Bau dieses Schiffs verboten");
				return false;
			}
		}

		//Kann die aktuelle Rasse das Schiff bauen?
		if( !Rassen.get().rasse(user.getRace()).isMemberIn(shipdata.getRace()) ) {
			output.append("Ihre Rasse kann dieses Schiff nicht bauen");
			return false;
		}

		//Verfuegt der Spieler ueber alle noetigen Forschungen?
		if( !user.hasResearched(shipdata.getRes(1)) || !user.hasResearched(shipdata.getRes(2)) || !user.hasResearched(shipdata.getRes(3)) ) {
			output.append("Sie besitzen nicht alle zum Bau n&ouml;tigen Technologien");
			return false;
		}

		//Kann das Schiff in dieser Werft gebaut werden?
		if( shipdata.getWerftSlots() > this.getWerftSlots() ) {
			output.append("Dieses Werft ist nicht gro&szlig; genug f&uuml;r das Schiff");
			return false;
		}

		//Resourcenbedarf angeben
		boolean ok = true;

		int e = this.getEnergy();

		Cargo shipdataCosts = new Cargo(shipdata.getCosts());

		if( !costsPerTick ) {
			// Abzug der sofort anfallenden Baukosten
			ResourceList reslist = shipdataCosts.compare( cargo, false );
			for( ResourceEntry res : reslist ) {
				if( res.getDiff() > 0 ) {
					ok = false;
					break;
				}
			}

			// E-Kosten
			if( shipdata.getEKosten() > this.getEnergy()) {
				ok = false;
			}
		}

		int frei = this.getCrew();

		//Crew
		if (shipdata.getCrew() > frei) {
			ok = false;
		}

		if( !ok ) {
			output.append("Nicht genug Material verf&uuml;gbar");

			return false;
		}
		else if( testonly ) {
			//Auf bestaetigung des "Auftrags" durch den Spieler warten

			return true;
		}
		else {
			frei -= shipdata.getCrew();

			if( !costsPerTick ) {
				cargo.substractCargo( shipdataCosts );
				e -= shipdata.getEKosten();

				this.setCargo(cargo, false);
				this.setEnergy(e);
			}
			this.setCrew(frei);

			// TODO: Ab nach ShipWerft...
			if( this.getOneWayFlag() != 0 && this instanceof ShipWerft ) {
				// Einweg-Werft-Code

				ShipWerft werft = (ShipWerft)this;

				ShipType newtype = (ShipType)db.get(ShipType.class, this.getOneWayFlag());

				String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
				String history = "Baubeginn am "+currentTime+" durch "+user.getName()+" ("+user.getId()+")";

				Ship ship = werft.getShip();
				ship.getHistory().addHistory(history);
				ship.setName("Baustelle");
				ship.setBaseType(newtype);
				ship.setHull(newtype.getHull());
				ship.setEnergy(newtype.getEps());
				ship.setOwner(user);

				this.type = 2;

			}
			else if( this.getOneWayFlag() != 0 ) {
				output.append("WARNING: UNKNOWN OW_WERFT (possible: building) in buildShip@WerftObject");

				return false;
			}

			/*
				Werftauftrag einstellen
			*/
			ShipType type = shipdata.getType();

			WerftQueueEntry entry = new WerftQueueEntry(this, type, (build > 0 ? -1 : item), shipdata.getDauer(), shipdata.getWerftSlots());
			entry.setBuildFlagschiff(shipdata.isFlagschiff());
			if( entry.isBuildFlagschiff() ) {
				this.buildFlagschiff = true;
			}
			if( costsPerTick ) {
				shipdataCosts.multiply(1/(double)shipdata.getDauer(), Cargo.Round.CEIL);
				entry.setCostsPerTick(shipdataCosts);
				entry.setEnergyPerTick((int)Math.ceil(shipdata.getEKosten()/(double)shipdata.getDauer()));
			}
			db.persist(entry);

			this.entries = null;

			rescheduleQueue();

			return true;
		}
	}

	@Transient
	private WerftQueueEntry[] entries = null;

	/**
	 * Gibt die Bauschlange der Werft zurueck (inkl gerade im Bau befindlicher Schiffe).
	 * @return Die Bauschlange
	 */
	public WerftQueueEntry[] getBuildQueue() {
		if( entries != null ) {
			return entries.clone();
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		List<?> queue = db.createQuery("from WerftQueueEntry where werft=:werft order by position asc")
			.setInteger("werft", this.getWerftID())
			.list();

		WerftQueueEntry[] list = new WerftQueueEntry[queue.size()];
		int index = 0;
		for( Iterator<?> iter=queue.iterator(); iter.hasNext(); ) {
			list[index++] = (WerftQueueEntry)iter.next();
		}

		this.entries = list;

		return list;
	}


	/**
	 * Bricht das Bauvorhaben ab.
	 * @param entry Das Bauvorhaben
	 */
	public void cancelBuild(WerftQueueEntry entry) {
		if( entry.getWerft().getWerftID() != this.getWerftID() ) {
			throw new IllegalArgumentException("Das WerftQueue-Objekt gehoert nicht zu dieser Werft");
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( entry.isBuildFlagschiff() ) {
			this.buildFlagschiff = false;
		}
		db.delete(entry);

		final Iterator<?> entryIter = db.createQuery("from WerftQueueEntry where werft=:werft and position>:pos order by position")
			.setEntity("werft", entry.getWerft())
			.setInteger("pos", entry.getPosition())
			.iterate();
		while( entryIter.hasNext() ) {
			WerftQueueEntry aEntry = (WerftQueueEntry)entryIter.next();
			aEntry.setPosition(aEntry.getPosition()-1);
		}

		this.entries = null;
		this.rescheduleQueue();
	}

	/**
	 * Tauscht die Baudaten der beiden Bauschlangeneintraege. Die Eintraege selbst aendern ihre
	 * Position innerhalb der Schlange nicht.
	 * @param entry1 Der erste Eintrag
	 * @param entry2 Der zweite Eintrag
	 */
	public void swapQueueEntries(WerftQueueEntry entry1, WerftQueueEntry entry2) {
		ShipType type = entry1.getBuildShipType();
		entry1.setBuildShipType(entry2.getBuildShipType());
		entry2.setBuildShipType(type);

		int buildItem = entry1.getRequiredItem();
		entry1.setRequiredItem(entry2.getRequiredItem());
		entry2.setRequiredItem(buildItem);

		int remaining = entry1.getRemainingTime();
		entry1.setRemainingTime(entry2.getRemainingTime());
		entry2.setRemainingTime(remaining);

		boolean fs = entry1.isBuildFlagschiff();
		entry1.setBuildFlagschiff(entry2.isBuildFlagschiff());
		entry2.setBuildFlagschiff(fs);

		Cargo cargo = entry1.getCostsPerTick();
		entry1.setCostsPerTick(entry2.getCostsPerTick());
		entry2.setCostsPerTick(cargo);

		int e = entry1.getEnergyPerTick();
		entry1.setEnergyPerTick(entry2.getEnergyPerTick());
		entry2.setEnergyPerTick(e);

		int slots = entry1.getSlots();
		entry1.setSlots(entry2.getSlots());
		entry2.setSlots(slots);

		boolean scheduled = entry1.isScheduled();
		entry1.setScheduled(entry2.isScheduled());
		entry2.setScheduled(scheduled);

		rescheduleQueue();
	}

	/**
	 * Gibt den Bauschlangeneintrag mit der angegebenen Position zurueck.
	 * @param position Die Position
	 * @return Der Bauschlangeneintrag
	 */
	public WerftQueueEntry getBuildQueueEntry(int position) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (WerftQueueEntry)db.createQuery("from WerftQueueEntry where werft=:werft and position=:pos")
			.setInteger("werft", this.getWerftID())
			.setInteger("pos", position)
			.uniqueResult();
	}

	@Override
	public Location getLocation() {
		return new Location(getSystem(), getX(), getY());
	}

	/**
	 * Setzt, ob sich in der Bauschlange ein Flagschiff befindet.
	 * @param buildFlagschiff <code>true</code>, falls in der Bauschlange ein Flagschiff ist
	 */
	public void setBuildFlagschiff(boolean buildFlagschiff) {
		this.buildFlagschiff = buildFlagschiff;
	}

	/**
	 * Gibt die Werft zurueck, an die diese Werft gekoppelt ist.
	 * @return Die Werft
	 */
	public WerftKomplex getKomplex() {
		return linkedWerft;
	}

	/**
	 * Entfernt die Werft aus dem Werftkomplex.
	 * Wenn sich die Werft in keinem Komplex befindet wird
	 * keinerlei Aktion durchgefuehrt.
	 *
	 */
	public void removeFromKomplex() {
		if( this.linkedWerft == null ) {
			return;
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		WerftObject[] werften = this.linkedWerft.getMembers();

		if( werften.length < 3 ) {
			final WerftKomplex komplex = this.linkedWerft;

			for( int i=0; i < werften.length; i++ ) {
				werften[i].linkedWerft = null;
			}

			// Die Werftauftraege der groessten Werft zuordnen oder
			// (falls notwendig) loeschen
			final WerftObject largest = werften[0].getWerftSlots() > werften[1].getWerftSlots() ? werften[0] : werften[1];

			WerftQueueEntry[] entries = komplex.getBuildQueue();
			for( int i=0; i < entries.length; i++ ) {
				if( entries[i].getSlots() <= largest.getWerftSlots() ) {
					entries[i].copyToWerft(largest);
				}
				db.delete(entries[i]);
			}

			this.entries = null;

			db.delete(komplex);
			db.flush();
			largest.rescheduleQueue();
		}
		else {
			WerftKomplex komplex = this.linkedWerft;
			this.linkedWerft = null;

			komplex.refresh();
			komplex.rescheduleQueue();
		}
	}

	/**
	 * Setzt den Werftkomplex, an die diese Werft gekoppelt werden soll.
	 * @param linkedWerft Der Werftkomplex
	 * @throws IllegalStateException Falls sich die Werft bereits in einem Komplex befindet
	 */
	public void addToKomplex(WerftKomplex linkedWerft) throws IllegalStateException {
		if( !this.isLinkableWerft() ) {
			throw new RuntimeException("Diese Werft kann sich mit keiner anderen Werft zusammenschliessen");
		}

		if( this.linkedWerft != null ) {
			throw new IllegalStateException("Diese Werft befindet sich bereits in einem Komplex");
		}

		this.linkedWerft = linkedWerft;

		org.hibernate.Session db = ContextMap.getContext().getDB();

		WerftQueueEntry[] entries = this.getBuildQueue();
		for( int i=0; i < entries.length; i++ ) {
			entries[i].copyToWerft(this.linkedWerft);
			db.delete(entries[i]);
		}

		this.entries = null;

		linkedWerft.refresh();
		this.linkedWerft.rescheduleQueue();
	}

	/**
	 * Erstellt einen neuen Werftkomplex zwischen dieser Werft und der angegebenen Werft.
	 * @param werft Die Werft mit der ein Komplex gebildet werden soll
	 */
	public void createKomplexWithWerft(WerftObject werft) {
		if( !werft.isLinkableWerft() ) {
			throw new IllegalArgumentException("Die Zielwerft kann sich mit keiner anderen Werft zusammenschliessen");
		}

		if( !this.isLinkableWerft() ) {
			throw new RuntimeException("Diese Werft kann sich mit keiner anderen Werft zusammenschliessen");
		}

		if( this.linkedWerft != null ) {
			throw new IllegalStateException("Diese Werft befindet sich bereits in einem Komplex");
		}

		if( werft.linkedWerft != null ) {
			throw new IllegalStateException("Die Zielwerft befindet sich bereits in einem Komplex");
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		WerftKomplex komplex = new WerftKomplex();
		db.save(komplex);

		this.linkedWerft = komplex;
		werft.linkedWerft = komplex;

		WerftQueueEntry[] entries = this.getBuildQueue();
		for( int i=0; i < entries.length; i++ ) {
			entries[i].copyToWerft(this.linkedWerft);
			db.delete(entries[i]);
		}

		this.entries = null;

		entries = werft.getBuildQueue();
		for( int i=0; i < entries.length; i++ ) {
			entries[i].copyToWerft(werft.linkedWerft);
			db.delete(entries[i]);
		}

		werft.entries = null;

		db.flush();

		this.linkedWerft.rescheduleQueue();
	}

	/**
	 * Gibt zurueck, ob sich diese Werft mit einer anderen Werft zusammenschliessen kann.
	 * @return <code>true</code>, falls ein Zusammenschluss moeglich ist
	 */
	public abstract boolean isLinkableWerft();

	/**
	 * Loescht die Werft und alle mit ihr verbundenen Auftraege.
	 *
	 */
	public void destroy() {
		if( getKomplex() != null ) {
			removeFromKomplex();
		}
		clearQueue();

		ContextMap.getContext().getDB().delete(this);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
