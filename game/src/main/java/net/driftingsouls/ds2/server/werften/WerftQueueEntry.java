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
package net.driftingsouls.ds2.server.werften;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.SchiffHinzufuegenService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Iterator;
import java.util.List;

/**
 * Ein Eintrag in der WerftQueue.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="werft_queues")
public class WerftQueueEntry {
	/**
	 * Lognachrichten der zuletzt aufgerufenen Methoden. Die Nachrichten sind Thread-Lokal.
	 */
	@Transient
	public final ContextLocalMessage MESSAGE = new ContextLocalMessage();

	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.EAGER, optional = false)
	@JoinColumn(name="werft", nullable=false)
	@ForeignKey(name="werft_queues_fk_werften")
	private WerftObject werft;
	private int position;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="building", nullable=false)
	@ForeignKey(name="werft_queues_fk_ship_types")
	private ShipType building;

	@Column(name="item", nullable = false)
	private int buildItem = -1;

	@Column(name="flagschiff", nullable = false)
	private boolean buildFlagschiff = false;
	private int remaining = 0;
	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo costsPerTick;
	private int energyPerTick;
	private int slots = 1;
	private boolean scheduled = false;

	/**
	 * Konstruktor.
	 *
	 */
	public WerftQueueEntry() {
		// EMPTY
	}

	/**
	 * Erstellt einen neuen Bauschlangeneintrag.
	 * @param werft Die Werft
	 * @param building Das zu bauende Schiff
	 * @param remaining Die verbleibende Bauzeit
	 * @param slots Die Anzahl der belegten Slots
	 */
	public WerftQueueEntry(WerftObject werft, ShipType building, int remaining, int slots) {
		this.werft = werft;
		this.position = getNextEmptyPosition(werft);
		this.building = building;
		this.buildItem = -1;
		this.remaining = remaining;
		this.costsPerTick = new Cargo();
		this.slots = slots;
	}

	/**
	 * Erstellt einen neuen Bauschlangeneintrag.
	 * @param werft Die Werft
	 * @param building Das zu bauende Schiff
	 * @param buildItem Das zum Bau benoetigte Item
	 * @param remaining Die verbleibende Bauzeit
	 * @param slots Die Anzahl der belegten Slots
	 */
	public WerftQueueEntry(WerftObject werft, ShipType building, int buildItem, int remaining, int slots) {
		this(werft, building, remaining, slots);
		this.buildItem = buildItem;
	}

	private static int getNextEmptyPosition(WerftObject werft) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Integer position = (Integer)db.createQuery("select max(wq.position) from WerftQueueEntry as wq where wq.werft=:werft")
			.setInteger("werft", werft.getWerftID())
			.iterate().next();

		if( position == null ) {
			return 1;
		}
		return position+1;
	}

	/**
	 * Gibt die Id zurueck.
	 * @return Die Id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Kopiert den Inhalt dieses Bauschlangeneintrags in einen neuen Eintrag einer anderen Werft.
	 * Dieser Bauschlangeneintrag wird dabei weder modifiziert noch geloescht.
	 * @param targetWerft Die Zielwerft
	 * @return Der neue Bauschlangeneintrag
	 */
	public WerftQueueEntry copyToWerft(WerftObject targetWerft) {
		WerftQueueEntry entry = new WerftQueueEntry(targetWerft, this.building, this.buildItem, this.remaining, this.slots);
		entry.costsPerTick = this.costsPerTick;
		entry.energyPerTick = this.energyPerTick;
		entry.buildFlagschiff = this.buildFlagschiff;

		org.hibernate.Session db = ContextMap.getContext().getDB();
		db.persist(entry);

		targetWerft.addQueueEntry(entry);

		return entry;
	}

	/**
	 * Gibt die Werft zurueck, zu der die Bauschlange gehoert.
	 * @return Die Werft
	 */
	public WerftObject getWerft() {
		return this.werft;
	}

	/**
	 * Gibt die Position des Eintrags innerhalb der Bauschlange zurueck.
	 * @return Die Position
	 */
	public int getPosition() {
		return this.position;
	}

	/**
	 * Setzt die Position des Eintrags innerhalb der Bauschlange. Jede Position
	 * darf nur einmal vergeben sein.
	 * @param position Die Position innerhalb der Bauschlange
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * Gibt zurueck, ob es sich bei dem Bau um ein Flagschiff handelt.
	 * @return <code>true</code>, falls es ein Flagschiff ist
	 */
	public boolean isBuildFlagschiff() {
		return buildFlagschiff;
	}

	/**
	 * Setzt, ob es sich beim Bau um ein Flagschiff handelt.
	 * @param buildFlagschiff <code>true</code>, falls es ein Flagschiff ist
	 */
	public void setBuildFlagschiff(boolean buildFlagschiff) {
		this.buildFlagschiff = buildFlagschiff;
	}

	/**
	 * Gibt den zu bauenden Schiffstyp zurueck.
	 * @return Der Schiffstyp
	 */
	public ShipType getBuildShipType() {
		return building;
	}

	/**
	 * Setzt den zu bauenden Schiffstyp.
	 * @param building Der Schiffstyp
	 */
	public void setBuildShipType(ShipType building) {
		this.building = building;
	}

	/**
	 * Gibt das fuer den Bau benoetigte Item zurueck.
	 * Es wird <code>-1</code> zurueckgegeben, falls kein Item benoetigt wird
	 * @return Das Item oder <code>-1</code>
	 */
	public int getRequiredItem() {
		return buildItem;
	}

	/**
	 * Setzt das fuer den Bau benoetigte Item.
	 * @param buildItem Das Item oder <code>-1</code>
	 */
	public void setRequiredItem(int buildItem) {
		this.buildItem = buildItem;
	}

	/**
	 * Gibt die verbleibende Bauzeit zurueck.
	 * @return Die verbleibende Bauzeit
	 */
	public int getRemainingTime() {
		return remaining;
	}

	/**
	 * Setzt die noch verbleibende Bauzeit.
	 * @param remaining Die Bauzeit
	 */
	public void setRemainingTime(int remaining) {
		this.remaining = remaining;
	}

	/**
	 * Gibt die pro Tick faelligen Baukosten zurueck.
	 * @return Die Baukosten pro Tick
	 */
	public Cargo getCostsPerTick() {
		return costsPerTick;
	}

	/**
	 * Setzt die pro Tick faelligen Baukosten.
	 * @param costsPerTick Die Baukosten pro Tick
	 */
	public void setCostsPerTick(Cargo costsPerTick) {
		this.costsPerTick = costsPerTick;
	}

	/**
	 * Gibt die Energiekosten pro Tick zurueck.
	 * @return Die Energiekosten pro Tick
	 */
	public int getEnergyPerTick() {
		return energyPerTick;
	}

	/**
	 * Setzt die Energiekosten pro Tick.
	 * @param energyPerTick Die Energiekosten pro Tick
	 */
	public void setEnergyPerTick(int energyPerTick) {
		this.energyPerTick = energyPerTick;
	}

	/**
	 * Gibt zurueck, ob der Eintrag gerade zum Bau vorgesehen ist.
	 * @return <code>true</code>, falls er gerade gebaut wird
	 */
	public boolean isScheduled() {
		return scheduled;
	}

	/**
	 * Setzt, ob der Eintrag nun gebaut werden soll.
	 * @param sheduled <code>true</code>, falls er gebaut werden soll
	 */
	public void setScheduled(boolean sheduled) {
		this.scheduled = sheduled;
	}

	/**
	 * Gibt zurueck, wieviele Slots der Eintrag belegt.
	 * @return Die Anzahl der belegten Slots
	 */
	public int getSlots() {
		return slots;
	}

	/**
	 * Setzt die Anzahl der belegten Slots.
	 * @param slots Die Anzahl der Slots
	 */
	public void setSlots(int slots) {
		this.slots = slots;
	}

	/**
	 * Beendet den Bauprozess dieses Bauschlangeneintrags erfolgreich.
	 * Sollte dies nicht moeglich sein, wird 0 zurueckgegeben.
	 *
	 * @return die ID des gebauten Schiffes oder 0
	 */
	public int finishBuildProcess() {
		MESSAGE.get().setLength(0);

		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		if( !this.isScheduled() ) {
			return 0;
		}

		ShipType shipd = this.getBuildShipType();

		User auser = this.werft.getOwner();

		SchiffHinzufuegenService schiffHinzufuegenService = new SchiffHinzufuegenService();
		Ship ship = schiffHinzufuegenService.erstelle(auser, shipd, this.werft.getLocation());

		// Item benutzen
		if( this.getRequiredItem() > -1 ) {
			Cargo cargo = this.werft.getCargo(true);
			List<ItemCargoEntry> itemlist = cargo.getItem(this.getRequiredItem());
			boolean ok = false;
			for (ItemCargoEntry anItemlist : itemlist)
			{
				if (anItemlist.getMaxUses() == 0)
				{
					ok = true;
					break;
				}
			}

			if( !ok ) {
				User user = this.werft.getOwner();

				Cargo allyitems = null;
				if( user.getAlly() != null ) {
					allyitems = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );
					itemlist = allyitems.getItem(this.getRequiredItem());
					for (ItemCargoEntry anItemlist : itemlist)
					{
						if (anItemlist.getMaxUses() == 0)
						{
							ok = true;
							break;
						}
					}
				}

				if( !ok ) {
					ItemCargoEntry item;
					String source;
					if( (user.getAlly() != null) && allyitems.hasResource(new ItemID(this.getRequiredItem())) ) {
						item = allyitems.getItem(this.getRequiredItem()).get(0);
						source = "ally";
					}
					else {
						item = cargo.getItem(this.getRequiredItem()).get(0);
						source = "local";
					}

					item.useItem();

					if( source.equals("local") ) {
						this.werft.setCargo(cargo, true);
					}
					else {
						this.werft.getOwner().getAlly().setItems(allyitems.save());
					}
				}
			}
		}

		if( this.isBuildFlagschiff() ) {
			this.werft.setBuildFlagschiff(false);
		}

		db.delete(this);
		this.werft.removeQueueEntry(this);

		final Iterator<?> entryIter = db.createQuery("from WerftQueueEntry where werft=:werft and position>:pos order by position")
			.setEntity("werft", this.werft)
			.setInteger("pos", this.position)
			.iterate();
		while( entryIter.hasNext() ) {
			WerftQueueEntry entry = (WerftQueueEntry)entryIter.next();
			entry.setPosition(entry.getPosition()-1);
		}

		db.flush();

		this.werft.onFinishedBuildProcess(ship.getId());

		return ship.getId();
	}

	/**
	 * Gibt zurueck, ob alle Voraussetzungen fuer eine Weiterfuehrung
	 * des Bauprozesses erfuellt sind. Wenn nichts gebaut wird,
	 * wird ebenfalls true zurueckgegeben.
	 *
	 * @return <code>true</code>, falls alle Voraussetzungen erfuellt sind
	 */
	public boolean isBuildContPossible() {
		if( !this.isScheduled() ) {
			return true;
		}

		// Pruefen, ob ein evt notwendiges Item vorhanden ist
		if( this.getRequiredItem() > -1 ) {
			Cargo cargo = new Cargo(this.werft.getCargo(true));
			User user = this.werft.getOwner();

			if( user.getAlly() != null ) {
				Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );
				cargo.addCargo( allyitems );
			}

			List<ItemCargoEntry> itemlist = cargo.getItem(this.getRequiredItem());
			if( itemlist.size() == 0 ) {
				return false;
			}
		}

		// Pruefen, ob die anfallenden Baukosten bezahlt werden koennen
		if( !this.getCostsPerTick().isEmpty() ) {
			Cargo cargo = new Cargo(this.werft.getCargo(false));
			ResourceList reslist = this.costsPerTick.compare(cargo, false);
			for( ResourceEntry res : reslist ) {
				if( res.getDiff() > 0 ) {
					return false;
				}
			}
		}

		if( this.getEnergyPerTick() != 0 ) {
			return this.werft.getEnergy() >= this.getEnergyPerTick();
		}

		return true;
	}

	private void substractBuildCosts() {
		if( !this.getCostsPerTick().isEmpty() ) {
			Cargo cargo = this.werft.getCargo(false);
			cargo.substractCargo(this.getCostsPerTick());
			this.werft.setCargo(cargo, false);
		}

		if( this.getEnergyPerTick() != 0 ) {
			this.werft.setEnergy(this.werft.getEnergy() - this.getEnergyPerTick());
		}
	}

	/**
	 * Dekrementiert die verbliebene Bauzeit um 1.
	 */
	public final void decRemainingTime() {
		if( this.getRemainingTime() <= 0 ) {
			return;
		}

		this.setRemainingTime(this.getRemainingTime()-1);
	}

	/**
	 * <p>Setzt den Bau fort. Dies umfasst u.a. das Dekrementieren
	 * der verbleibenden Bauzeit um 1 sowie des Abzugs der pro Tick
	 * anfallenden Baukosten.</p>
	 * <p>Es wird nicht geprueft, ob die Bedingungen fuer ein fortsetzen des
	 * Baus erfuellt sind</p>
	 * @see #isBuildContPossible()
	 */
	public void continueBuild() {
		this.decRemainingTime();
		this.substractBuildCosts();
	}
}
