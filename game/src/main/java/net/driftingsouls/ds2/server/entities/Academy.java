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
package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.AcademyQueueEntry;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.ConfigService;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Eine Akademie.
 * 
 *
 */
@Entity
@Table(name="academy")
public class Academy {
	@Id @GeneratedValue
	private int id;
	@OneToOne(fetch=FetchType.LAZY, mappedBy="academy")
	private Base base;
	private boolean train;
	@Version
	private int version;
	
	@OneToMany(mappedBy="academy",cascade=CascadeType.ALL, orphanRemoval=true)
	private List<AcademyQueueEntry> queue;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Academy() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Akademie.
	 * @param base Die Basis auf der die Akademie steht
	 */
	public Academy(Base base) {
		this.base = base;
		this.queue = new ArrayList<>();
	}

	/**
	 * Gibt die Basis zurueck.
	 * @return Die Basis
	 */
	public Base getBase() {
		return base;
	}

	/**
	 * Setzt die Basis auf der sich die Akademie befindet.
	 * @param col Die Basis
	 */
	public void setBase(Base col) {
		this.base = col;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Gibt zurueck, ob die Akademie ausbildet.
	 * @return <code>true</code> wenn die Akademie ausbildet, ansonsten <code>false</code>
	 */
	public boolean getTrain() {
		return this.train;
	}
	
	/**
	 * Setzt den Ausbildungsstand der Akademie.
	 * @param train <code>true</code> oder <code>false</code> ob die Akademie gerade ausbildet
	 */
	public void setTrain(boolean train) {
		this.train = train;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
	
	/**
	 * Gibt die aktuell ausgebildeten Offiziere zurueck.
	 * @return Die Liste der zur Ausbildung vorgesehenen Bauschlangeneintraege
	 */
	public List<AcademyQueueEntry> getScheduledQueueEntries()
	{
		return this.queue.stream().filter(AcademyQueueEntry::isScheduled).collect(Collectors.toList());
	}
	
	/**
	 * Gibt die Anzahl der aktuell laufenen Ausbildlungsvorhaben der Bauschlange zurueck.
	 * @return Die Anzahl der aktuellen Ausbildungsvorhaben
	 */
	public int getNumberScheduledQueueEntries() {
		return getScheduledQueueEntries().size();
	}
	
	/**
	 * Gibt die komplette Bauschlange in unsortierter Form zurueck.
	 * @return Die Bauschlange
	 */
	public List<AcademyQueueEntry> getQueueEntries() {
		return this.queue;
	}
	
	/**
	 * Berechnet die aktuell laufenden Ausbildungen neu.
	 * 
	 */
	public void rescheduleQueue() {
		int maxoffstotrain = new ConfigService().getValue(WellKnownConfigValue.MAX_OFFS_TO_TRAIN);
		int trainingoffs = 0;
		
		List<AcademyQueueEntry> queue = this.queue;
		
		for( AcademyQueueEntry entry : queue ) 
		{
			entry.setScheduled(false);
		}
		for( AcademyQueueEntry entry : queue ) 
		{
			if( trainingoffs < maxoffstotrain && !this.isOffizierScheduled(entry.getTraining()) ) {
				entry.setScheduled(true);
				trainingoffs = trainingoffs+1;
			}
		}
	}
	
	/**
	 * Gibt an ob der Offizier bereits aktuell ausgebildet wird.
	 * @param offID Die ID des Offiziers
	 * @return <code>true</code> wenn der Offizier aktuell ausgebildet wird, ansonsten <code>false</code>
	 */
	public boolean isOffizierScheduled(int offID) {
		if( offID < 0 ){
			return false;
		}
		List<AcademyQueueEntry> entries = this.getScheduledQueueEntries();
		for ( AcademyQueueEntry entry : entries ) {
			if( entry.getTraining() == offID ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gibt den Bauschlangeneintrag mit der Position zurueck.
	 * @param position Die Position in der Bauschlange
	 * @return Der Bauschlangeneintrag
	 */
	public AcademyQueueEntry getQueueEntryByPosition(int position) {
		for( AcademyQueueEntry entry : this.queue )
		{
			if( entry.getPosition() == position )
			{
				return entry;
			}
		}
		return null;
	}
	
	/**
	 * Gibt den Bauschlangeneintrag mit der Id zurueck.
	 * @param id Die Id des Bauschlangeneintrags
	 * @return Der Bauschlangeneintrag
	 */
	public AcademyQueueEntry getQueueEntryById(int id) {
		
		List<AcademyQueueEntry> entries = this.getQueueEntries();
		
		for( AcademyQueueEntry entry : entries ) {
			if( entry.getId() == id )
			{
				return entry;
			}
		}
		return null;
	}
	
	/**
	 * Fuegt einen neuen Eintrag zur Bauschlange hinzu.
	 * @param entry Der Eintrag
	 */
	public void addQueueEntry(AcademyQueueEntry entry)
	{
		this.queue.add(entry);
	}
}
