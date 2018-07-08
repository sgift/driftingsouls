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

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.*;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Schiffsbauplan;
import net.driftingsouls.ds2.server.config.items.Schiffsmodul;
import net.driftingsouls.ds2.server.config.items.Schiffsverbot;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftShip;
import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.ships.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

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
	@Column(name="flagschiff", nullable = false)
	private boolean buildFlagschiff = false;
	@Column(nullable = false)
	private WerftTyp type = WerftTyp.SCHIFF;
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="linkedWerft", nullable=true)
	@ForeignKey(name="werften_fk_werften")
	private WerftKomplex linkedWerft = null;

	@SuppressWarnings("UnusedDeclaration")
	@Version
	private int version;

	@OneToMany(mappedBy = "werft", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WerftQueueEntry> queue;


	@Transient
	private Logger log = Logger.getLogger(WerftObject.class);

	/**
	 * Konstruktor.
	 *
	 */
	protected WerftObject() {
		this.queue = new HashSet<>();
	}

	/**
	 * Erstellt eine neue Werft.
	 * @param type Der Typ der Werft
	 */
	public WerftObject(WerftTyp type) {
		this.type = type;
		this.queue = new HashSet<>();
	}

	/**
	 * Gibt die aktuell zum Bau vorgesehenen Bauschlangeneintraege zurueck.
	 * @return Die Liste der zum Bauvorgesehenen Bauschlangeneintraege
	 */
	public @Nonnull WerftQueueEntry[] getScheduledQueueEntries() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<?> list = db.createQuery("from WerftQueueEntry where werft=:werft and scheduled=true order by position")
			.setInteger("werft", this.getWerftID())
			.list();
		WerftQueueEntry[] entries = new WerftQueueEntry[list.size()];
		int index = 0;
		for (Object aList : list)
		{
			entries[index++] = (WerftQueueEntry) aList;
		}

		return entries;
	}

	/**
	 * Gibt zurueck, ob in der Werft im Moment gebaut wird.
	 * @return <code>true</code>, falls gebaut wird
	 */
	public boolean isBuilding() {
		return !this.queue.isEmpty();
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
	public int getUsedSlots() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return ((Number)db.createQuery("select sum(slots) from WerftQueueEntry where werft=:werft and scheduled=true")
			.setInteger("werft", this.getWerftID())
			.iterate().next()).intValue();
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem ein Bauauftrag voraussichtlich fertig sein wird.
	 * @param searched Der Bauauftrag
	 * @return Die Zeit in Ticks bis zur Fertigstellung
	 */
	public int getTicksTillFinished(@Nonnull WerftQueueEntry searched) {
		List<WerftQueueEntry> entries = new ArrayList<>();
		entries.addAll(getBuildQueue());

		int slots = this.getWerftSlots();
		int time = 0;
		boolean first = true;

		SortedMap<Integer,List<WerftQueueEntry>> scheduled = new TreeMap<>();

		while( !entries.isEmpty() ) {
			for( int i=0; i < entries.size(); i++ ) {
				WerftQueueEntry entry = entries.get(i);

				if( (first && entry.isScheduled()) || (!first && entry.getSlots() <= slots) ) {
					if( entry == searched ) {
						return time+entry.getRemainingTime();
					}

					slots -= entries.get(i).getSlots();

					if( !scheduled.containsKey(time+entry.getRemainingTime()) ) {
						scheduled.put(time+entry.getRemainingTime(), new ArrayList<>());
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
	public @NotNull WerftTyp getType() {
		return type;
	}

    /**
     * Setzt den Typ der Werft.
     * @param type Der neue Typ der Werft
     */
    public void setType(@NotNull WerftTyp type) {
        this.type = type;
    }

	/**
	 * Wird von Bauschlangeneintraegen aufgerufen, nachdem sie erfolgreich abgeschlossen wurden.
	 * Der Bauschlangeneintrag ist zu diesem Zeitpunkt bereits entfernt.
	 * @param shipid Die ID des neugebauten Schiffes
	 */
	protected void onFinishedBuildProcess(int shipid) {
		rescheduleQueue();
	}

	/**
	 * Berechnet, welche Eintraege der Bauschlange im Moment gebaut werden und welche nicht.
	 *
	 */
	public void rescheduleQueue() {
		int freeSlots = this.getWerftSlots();

		final Cargo cargo = new Cargo(this.getCargo(true));
		final User user = this.getOwner();

		if( user.getAlly() != null ) {
			Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );
			cargo.addCargo( allyitems );
		}

		final List<WerftQueueEntry> entries = getBuildQueue();
		for (final WerftQueueEntry entry : entries)
		{
			// Falls ein Item benoetigt wird pruefen, ob es vorhanden ist
			if (entry.getRequiredItem() != -1)
			{
				List<ItemCargoEntry> itemlist = cargo.getItem(entry.getRequiredItem());
				if (itemlist.size() == 0)
				{
					entry.setScheduled(false);
					continue;
				}

				// Item entfernen, damit ein Bauplan nicht mehrfach benutzt wird
				cargo.substractResource(itemlist.get(0).getResourceID(), 1);
			}

			if (entry.getSlots() > freeSlots)
			{
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
		this.queue.clear();

		this.buildFlagschiff = false;
	}

	/**
	 * Gibt das Einweg-Flag der Werft zurueck.
	 * @return Das Einweg-Flag
	 */
	public @Nullable ShipType getOneWayFlag() {
		return null;
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
	public void removeModule( @Nonnull Ship ship, int slot ) {
		if(this.type == WerftTyp.EINWEG)
		{
			MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
			return;
		}
		Map<Integer,Integer> usedslots = new HashMap<>();
		ModuleEntry[] modules = ship.getModules();
		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].getSlot(), i);
		}

		if( !usedslots.containsKey(slot) ) {
			MESSAGE.get().append(ship.getName()).append(" - Es befindet sich kein Modul in diesem Slot\n");
			return;
		}

		ShipTypeData shiptype = ship.getTypeData();

		String[] aslot = null;

		String[] moduleslots = StringUtils.split(shiptype.getTypeModules(), ';');
		for (String moduleslot : moduleslots)
		{
			String[] data = StringUtils.split(moduleslot, ':');

			if (Integer.parseInt(data[0]) == slot)
			{
				aslot = data;
				break;
			}
		}

		if( aslot == null ) {
			MESSAGE.get().append(ship.getName()).append(" - Keinen passenden Slot gefunden\n");
			return;
		}

		ShipTypeData oldshiptype;
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

		Cargo transferCargoZurWerft = ship.postUpdateShipType(oldshiptype);

		if( this.getMaxCargo(false) - cargo.getMass() > 0 ) {
			Cargo addwerftcargo = transferCargoZurWerft.cutCargo( this.getMaxCargo(false) - cargo.getMass() );
			cargo.addCargo( addwerftcargo );

			if( !transferCargoZurWerft.isEmpty() )
			{
				Cargo shipCargo = ship.getCargo();
				shipCargo.addCargo(transferCargoZurWerft);
				ship.setCargo(shipCargo);
			}
		}
        setCargo(cargo, false);

		MESSAGE.get().append(Ship.MESSAGE.get());
		MESSAGE.get().append(ship.getName()).append(" - Modul ausgebaut\n");
	}

    /**
     * Gibt zurueck, ob es sich bei dieser Werft um eine Einwegwerft oder dessen Vorstufe handelt.
     * Einwegwerften können nur ein Schiff bauen und werden dann zerstört.
     * @return <code>true</code>, falls es sich um eine Einwegwerft handelt, sonst <code>false</code>
     */
    public boolean isEinwegWerft()
    {
        return false;
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
	public void addModule( @Nonnull Ship ship, int slot, int itemid ) {
		if(this.isEinwegWerft())
		{
			MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
			return;
		}
		Map<Integer,Integer> usedslots = new HashMap<>();
		ModuleEntry[] modules = ship.getModules();
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		for( int i=0; i < modules.length; i++ ) {
			usedslots.put(modules[i].getSlot(), i);
		}

		if( usedslots.containsKey(slot) ) {
			MESSAGE.get().append(ship.getName()).append(" - Der Slot ist bereits belegt\n");
			return;
		}

		Cargo cargo = this.getCargo(false);
		List<ItemCargoEntry<Schiffsmodul>> itemlist = cargo.getItemsOfType(Schiffsmodul.class);

		ShipTypeData shiptype = ship.getTypeData();

		String[] aslot = null;

		String[] moduleslots = StringUtils.split(shiptype.getTypeModules(), ';');
		for (String moduleslot : moduleslots)
		{
			String[] data = StringUtils.split(moduleslot, ':');

			if (Integer.parseInt(data[0]) == slot)
			{
				aslot = data;
				break;
			}
		}

		if( aslot == null ) {
			MESSAGE.get().append(ship.getName()).append(" - Keinen passenden Slot gefunden\n");
			return;
		}

		Item item = (Item)db.get(Item.class, itemid);
		if( !ModuleSlots.get().slot(aslot[1]).isMemberIn( ((IEModule)item.getEffect()).getSlots() ) ) {
			MESSAGE.get().append(ship.getName()).append(" - Das Item passt nicht in dieses Slot\n");
			return;
		}

		if( item.getAccessLevel() > context.getActiveUser().getAccessLevel() ) {
			MESSAGE.get().append(ship.getName()).append(" - Ihre Techniker wissen nichts mit dem Modul anzufangen\n");
			return;
		}

		ItemCargoEntry myitem = null;

		for (ItemCargoEntry<Schiffsmodul> anItemlist : itemlist)
		{
			if (anItemlist.getItemID() == itemid)
			{
				myitem = anItemlist;
				break;
			}
		}

		if( myitem == null || myitem.getCount() <= 0 ) {
			MESSAGE.get().append(ship.getName()).append(" - Kein passendes Item gefunden\n");
			return;
		}

		ShipTypeData oldshiptype;
		try {
			oldshiptype = (ShipTypeData)shiptype.clone();
		}
		catch( CloneNotSupportedException e ) {
			oldshiptype = shiptype;
		}

		ship.addModule( slot, ModuleType.ITEMMODULE, Integer.toString(itemid) );
		cargo.substractResource( myitem.getResourceID(), 1 );

		Cargo transferCargoZurWerft = ship.postUpdateShipType(oldshiptype);

		if( this.getMaxCargo(false) - cargo.getMass() > 0 ) {
			Cargo addwerftcargo = transferCargoZurWerft.cutCargo( this.getMaxCargo(false) - cargo.getMass() );
			cargo.addCargo( addwerftcargo );

			if( !transferCargoZurWerft.isEmpty() )
			{
				Cargo shipCargo = ship.getCargo();
				shipCargo.addCargo(transferCargoZurWerft);
				ship.setCargo(shipCargo);
			}
		}
        setCargo(cargo,false);

		MESSAGE.get().append(Ship.MESSAGE.get());

		MESSAGE.get().append(ship.getName()).append(" - Modul eingebaut\n");

	}

	/**
	 * Berechnet den Cargo, den man beim Demontieren eines Schiffes zurueckbekommt. Er entspricht somit
	 * dem reinen Schrottwert des Schiffes :)
	 * Die aktuell geladenen Waren des Schiffes sind nicht teil des Cargos!
	 * @param ship Das Schiff
	 *
	 * @return Cargo mit den Resourcen
	 */
	public @Nonnull Cargo getDismantleCargo( @Nonnull Ship ship ) {
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
			cost.addResource( Resources.URAN, (long)(htr/10) );
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
			cost.addResource( Resources.URAN, (long)(factor*buildcosts.getResourceCount(Resources.URAN)) );
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
	public int dismantleShips(@Nonnull Collection<Ship> ships)
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
	public boolean dismantleShip(@Nonnull Ship ship, boolean testonly) {
		if(this.isEinwegWerft())
		{
			MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
			return false;
		}
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

		Set<Offizier> offiziere = ship.getOffiziere();

		if( offiziere.size() > maxoffis ) {
			output.append("Nicht genug Platz f&uuml;r alle Offiziere");
			ok = false;
		}
		if( !ok ) {
			return false;
		}

		if(!testonly) {
			this.setCargo(newcargo, false);

			this.setCrew(this.getCrew()+ship.getCrew());
			if(this.getCrew() > this.getMaxCrew())
			{
				this.setCrew(this.getMaxCrew());
			}

			//Iterate over copy or we get ConcurrentModificationException when officer is moved out of original set
			Set<Offizier> dockyardOfficers = new HashSet<>(offiziere);
			for( Offizier offi : dockyardOfficers ) {
				this.transferOffi(offi.getID());
			}

			ship.destroy();
		}

		return true;
	}

	/**
	 * Bewegt den Eintrag in der Bauschlange ganz an das Ende.
	 * @param position Die Position des zu verschiebenden Eintrags
	 */
	public void moveBuildQueueEntryToBottom(int position)
	{
		List<WerftQueueEntry> queue = this.getBuildQueue();
		final int queueSize = queue.size();
		int index = 0;
		for( ; index < queueSize; index++ )
		{
			if( queue.get(index).getPosition() == position )
			{
				break;
			}
		}
		for( ; index < queueSize-1; index++ )
		{
			WerftQueueEntry entry = queue.get(index);
			WerftQueueEntry entry2 = queue.get(index+1);
			if( (entry != null) && (entry2 != null) ) {
				this.swapQueueEntries(entry, entry2);
			}
		}
	}

	/**
	 * Verschiebt den angegenen Eintrag in der Bauschlange ganz
	 * an den Anfang.
	 * @param position Die Position des zu verschiebenden Eintrags
	 */
	public void moveBuildQueueEntryToTop(int position)
	{
		List<WerftQueueEntry> queue = this.getBuildQueue();
		int index = queue.size()-1;
		for( ; index > 0; index-- )
		{
			if( queue.get(index).getPosition() == position )
			{
				break;
			}
		}
		for( ; index > 0; index-- )
		{
			WerftQueueEntry entry = queue.get(index);
			WerftQueueEntry entry2 = queue.get(index-1);
			if( (entry != null) && (entry2 != null) ) {
				this.swapQueueEntries(entry, entry2);
			}
		}
	}

	/**
	 * Berechnet die Reparaturkosten fuer ein Schiff.
	 * @param ship Das Schiff
	 *
	 * @return Die Reparaturkosten
	 */
	public @Nonnull RepairCosts getRepairCosts( @Nonnull Ship ship )
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

			double dampeningFactor = new ConfigService().getValue(WellKnownConfigValue.REPAIR_COST_DAMPENING_FACTOR);

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
	public boolean repairShip(@Nonnull Ship ship, boolean testonly) {
		if(!ship.getLocation().sameSector(0, this.getLocation(), this.getSize()))
		{
			MESSAGE.get().append("Diese Werft befindet sich nicht an der gleichen Position wie das Schiff.");
			return false;
		}
		if(this.isEinwegWerft())
		{
			MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
			return false;
		}
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
            ship.addFlag(Ship.FLAG_RECENTLY_REPAIRED, 5);
		}
		return true;
	}

	/**
	 * Liefert die Liste aller theoretisch baubaren Schiffe auf dieser Werft.
	 * Das vorhanden sein von Resourcen wird hierbei nicht beruecksichtigt.
	 * @return array mit Schiffsbaudaten (ships_baubar) sowie
	 * 			'_item' => array( ('local' | 'ally'), $resourceid) oder '_item' => false
	 * 			zur Bestimmung ob und wenn ja welcher Bauplan benoetigt wird zum bauen
	 */
	public @Nonnull List<SchiffBauinformationen> getBuildShipList() {
		List<SchiffBauinformationen> result = new ArrayList<>();

		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		User user = this.getOwner();

		Cargo availablecargo = this.getCargo(false);

		Cargo allyitems;
		if( user.getAlly() != null ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING, user.getAlly().getItems());
		}
		else {
			allyitems = new Cargo();
		}

		Map<ShipType,Boolean> disableShips = new HashMap<>();

		for (ItemCargoEntry<Schiffsverbot> anItemlist : availablecargo.getItemsOfType(Schiffsverbot.class))
		{
			disableShips.put(anItemlist.getItem().getSchiffstyp(), true);
		}

		for (ItemCargoEntry<Schiffsverbot> anItemlist : allyitems.getItemsOfType(Schiffsverbot.class))
		{
			disableShips.put(anItemlist.getItem().getSchiffstyp(), true);
		}

		List<?> baubarList = db
				.createQuery("select sb from ShipBaubar sb WHERE sb.werftSlots <= :werftslots order by sb.type.nickname")
				.setInteger("werftslots", this.getWerftSlots())
				.list();
		for( Object obj : baubarList )
		{
			ShipBaubar sb = (ShipBaubar)obj;
			if( disableShips.containsKey(sb.getType()) ) {
				continue;
			}
			if( !Rassen.get().rasse(user.getRace()).isMemberIn(sb.getRace()) ) {
				continue;
			}

			if( sb.getType().getShipClass() == ShipClasses.SCHUTZSCHILD && !(this.getType() == WerftTyp.BASIS)) {
				continue;
			}

			//Forschungen checken
			if( !user.hasResearched(sb.getBenoetigteForschungen()) )
			{
				continue;
			}

			result.add(new SchiffBauinformationen(sb, BauinformationenQuelle.FORSCHUNG, null));
		}

		//Items
		Cargo localcargo = this.getCargo(true);
		for (ItemCargoEntry<Schiffsbauplan> item : localcargo.getItemsOfType(Schiffsbauplan.class))
		{
			IEDraftShip effect = item.getItem().getEffect();

			if (effect.getWerftSlots() > this.getWerftSlots())
			{
				continue;
			}

			//Forschungen checken
			if (!user.hasResearched(effect.getBenoetigteForschungen()) )
			{
				continue;
			}

			result.add(new SchiffBauinformationen(effect.toShipBaubar(), BauinformationenQuelle.LOKALES_ITEM, item.getResourceID()));
		}

		for (ItemCargoEntry<Schiffsbauplan> item : allyitems.getItemsOfType(Schiffsbauplan.class))
		{
			IEDraftShip effect = item.getItem().getEffect();

			if (effect.getWerftSlots() > this.getWerftSlots())
			{
				continue;
			}

			//Forschungen checken
			if (!user.hasResearched(effect.getBenoetigteForschungen()) )
			{
				continue;
			}

			result.add(new SchiffBauinformationen(effect.toShipBaubar(), BauinformationenQuelle.ALLIANZ_ITEM, item.getResourceID()));
		}

		return result;
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
	public @Nullable SchiffBauinformationen getShipBuildData( int build, int itemid ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = this.getOwner();

		Cargo allyitems;
	   	if( user.getAlly() != null ) {
			allyitems = new Cargo(Cargo.Type.ITEMSTRING,user.getAlly().getItems());
			Cargo localcargo = this.getCargo(true);

			allyitems.addCargo( localcargo );
		}
		else {
			allyitems = new Cargo();
		}

	   	ShipBaubar shipdata;
		if( build > 0 ) {
			shipdata = (ShipBaubar)db.createQuery("from ShipBaubar where id=:id")
										.setInteger("id", build)
										.setMaxResults(1)
										.uniqueResult();
			if( shipdata == null ) {
				MESSAGE.get().append("Es wurde kein passender Schiffsbauplan gefunden");
				return null;
			}

			return new SchiffBauinformationen(shipdata, BauinformationenQuelle.FORSCHUNG, null);
		}

		BauinformationenQuelle quelle = BauinformationenQuelle.LOKALES_ITEM;
		int itemcount = this.getCargo(true).getItem(itemid).size();
		if( itemcount == 0 )
		{
			quelle = BauinformationenQuelle.ALLIANZ_ITEM;
			itemcount = allyitems.getItem( itemid ).size();
		}

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
		shipdata = effect.toShipBaubar();

		return new SchiffBauinformationen(shipdata, quelle, new ItemID(itemid));
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
	public boolean buildShip( int build, int item, boolean costsPerTick, boolean testonly )
	{
		SchiffBauinformationen shipdata = this.getShipBuildData( build, item );
		if( shipdata == null )
		{
			MESSAGE.get().append("Diese Werft dieses Bauprojekt nicht durchführen");
			return false;
		}
		return buildShip(shipdata, costsPerTick, testonly);
	}

	/**
	 * Baut ein Schiff in der Werft auf Basis der angegebenen Schiffbau-ID und der
	 * angegebenen Item-ID (Bauplan). {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param build Die Schiffsbaudaten
	 * @param costsPerTick Sollen die Baukosten pro Tick (<code>true</code>) oder der Gesamtbetrag jetzt (<code>false</code>) abgezogen werden
	 * @param testonly Soll nur getestet (true) oder wirklich gebaut (false) werden?
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean buildShip( @Nonnull SchiffBauinformationen build, boolean costsPerTick, boolean testonly ) {
		StringBuilder output = MESSAGE.get();

		if( this.type == WerftTyp.EINWEG )
		{
			output.append("Diese Werft kann nur ein einziges Bauprojekt durchführen");
			return false;
		}

		if( !this.getBuildShipList().contains(build) )
		{
			output.append("Diese Werft dieses Bauprojekt nicht durchführen");
			return false;
		}
		if(build.getBaudaten().getType().getShipClass() == ShipClasses.SCHUTZSCHILD){
			for( Ship ship : this.getOwner().getShips() ){
				if( ship.getTypeData().getShipClass() == ShipClasses.SCHUTZSCHILD){
					output.append("Diese Station kann nur einmal existieren");
					return false;
				}
			}

			for( WerftQueueEntry entry : this.getBuildQueue() ){
				if( entry.getBuildShipType().getShipClass() == ShipClasses.SCHUTZSCHILD){
					output.append("Diese Station kann nur einmal existieren");
					return false;
				}
			}
		}

		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Cargo cargo = new Cargo(this.getCargo(false));

		ShipBaubar shipdata = build.getBaudaten();
		if( (shipdata == null) ) {
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

			/*
				Werftauftrag einstellen
			*/
			ShipType type = shipdata.getType();

			WerftQueueEntry entry = new WerftQueueEntry(this, type, build.getItem() != null ? build.getItem().getItemID() : -1, shipdata.getDauer(), shipdata.getWerftSlots());
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
			this.queue.add(entry);

			rescheduleQueue();

			return true;
		}
	}

	/**
	 * Gibt die Bauschlange der Werft zurueck (inkl gerade im Bau befindlicher Schiffe)
	 * in sortierter Reihenfolge (Position in der Bauschlange) zurueck.
	 * @return Die Bauschlange
	 */
	public @Nonnull List<WerftQueueEntry> getBuildQueue() {
		return this.queue.stream().sorted(Comparator.comparingInt(WerftQueueEntry::getPosition)).collect(Collectors.toList());
	}

	/**
	 * Bricht das Bauvorhaben ab.
	 * @param entry Das Bauvorhaben
	 */
	public void cancelBuild(@Nonnull WerftQueueEntry entry) {
		if(this.type == WerftTyp.EINWEG)
		{
			MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert. Es kann nicht abgebrochen werden.");
			return;
		}
		if( entry.getWerft().getWerftID() != this.getWerftID() ) {
			throw new IllegalArgumentException("Das WerftQueue-Objekt gehoert nicht zu dieser Werft");
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( entry.isBuildFlagschiff() ) {
			this.buildFlagschiff = false;
		}
		db.delete(entry);
		this.removeQueueEntry(entry);

		final Iterator<?> entryIter = db.createQuery("from WerftQueueEntry where werft=:werft and position>:pos order by position")
			.setEntity("werft", entry.getWerft())
			.setInteger("pos", entry.getPosition())
			.iterate();
		while( entryIter.hasNext() ) {
			WerftQueueEntry aEntry = (WerftQueueEntry)entryIter.next();
			aEntry.setPosition(aEntry.getPosition()-1);
		}

		this.rescheduleQueue();
	}

	/**
	 * Tauscht die Baudaten der beiden Bauschlangeneintraege. Die Eintraege selbst aendern ihre
	 * Position innerhalb der Schlange nicht.
	 * @param entry1 Der erste Eintrag
	 * @param entry2 Der zweite Eintrag
	 */
	public void swapQueueEntries(@Nonnull WerftQueueEntry entry1, @Nonnull WerftQueueEntry entry2) {
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
	public @Nullable WerftQueueEntry getBuildQueueEntry(int position) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return (WerftQueueEntry)db.createQuery("from WerftQueueEntry where werft=:werft and position=:pos")
			.setInteger("werft", this.getWerftID())
			.setInteger("pos", position)
			.uniqueResult();
	}

	@Override
	public @Nonnull Location getLocation() {
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

			for (WerftObject aWerften : werften)
			{
				aWerften.linkedWerft = null;
			}

			// Die Werftauftraege der groessten Werft zuordnen oder
			// (falls notwendig) loeschen
			final WerftObject largest = werften[0].getWerftSlots() > werften[1].getWerftSlots() ? werften[0] : werften[1];

			List<WerftQueueEntry> entries = komplex.getBuildQueue();
			for (WerftQueueEntry entry : entries)
			{
				if (entry.getSlots() <= largest.getWerftSlots())
				{
					entry.copyToWerft(largest);
				}
				komplex.removeQueueEntry(entry);
			}

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

	protected void addQueueEntry(WerftQueueEntry entry)
	{
		this.queue.add(entry);
	}

	protected void removeQueueEntry(WerftQueueEntry entry)
	{
		this.queue.remove(entry);
	}

	/**
	 * Setzt den Werftkomplex, an die diese Werft gekoppelt werden soll.
	 * @param linkedWerft Der Werftkomplex
	 * @throws IllegalStateException Falls sich die Werft bereits in einem Komplex befindet
	 */
	public void addToKomplex(@Nonnull WerftKomplex linkedWerft) throws IllegalStateException {
		if( !this.isLinkableWerft() ) {
			throw new RuntimeException("Diese Werft kann sich mit keiner anderen Werft zusammenschliessen");
		}

		if( this.linkedWerft != null ) {
			throw new IllegalStateException("Diese Werft befindet sich bereits in einem Komplex");
		}

		this.linkedWerft = linkedWerft;

		List<WerftQueueEntry> entries = this.getBuildQueue();
		for (WerftQueueEntry entry : entries)
		{
			entry.copyToWerft(this.linkedWerft);
			this.removeQueueEntry(entry);
			removeQueueEntry(entry);
		}

		linkedWerft.refresh();
		this.linkedWerft.rescheduleQueue();
	}

	/**
	 * Erstellt einen neuen Werftkomplex zwischen dieser Werft und der angegebenen Werft.
	 * @param werft Die Werft mit der ein Komplex gebildet werden soll
	 */
	public void createKomplexWithWerft(@Nonnull WerftObject werft) {
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

		List<WerftQueueEntry> entries = this.getBuildQueue();
		for (WerftQueueEntry entry : entries)
		{
			entry.copyToWerft(this.linkedWerft);
			removeQueueEntry(entry);
		}

		entries = werft.getBuildQueue();
		for (WerftQueueEntry entry : entries)
		{
			entry.copyToWerft(werft.linkedWerft);
			werft.removeQueueEntry(entry);
		}

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

		Session db = ContextMap.getContext().getDB();
		clearQueue();
		db.delete(this);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
