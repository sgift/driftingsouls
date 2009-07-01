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

import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.bases.AcademyQueueEntry;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Eine Akademie.
 * 
 *
 */
@Entity
@Table(name="academy")
public class Academy {
	@Id
	private int col;
	@OneToOne(fetch=FetchType.LAZY)
	@PrimaryKeyJoinColumn
	private Base base;
	private boolean train;
	@Version
	private int version;
	
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
		this.col = base.getId();
		this.base = base;
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
	 * Gibt die ID der Basis zurueck, auf der sich die Akademie befindet.
	 * @return Die ID der Basis
	 */
	public int getBaseId() {
		return col;
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
	public AcademyQueueEntry[] getScheduledQueueEntries() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<?> list = db.createQuery("from AcademyQueueEntry where base=:base and scheduled=:scheduled order by position")
		.setParameter("base", this.getBase())
		.setParameter("scheduled", true)
		.list();
		AcademyQueueEntry[] entries = new AcademyQueueEntry[list.size()];
		int index = 0;
		for( Iterator<?> iter=list.iterator(); iter.hasNext(); ) {
			entries[index++] = (AcademyQueueEntry)iter.next();
		}
	
		return entries;
	}
	
	/**
	 * Gibt die Anzahl der aktuell laufenen Ausbildlungsvorhaben der Bauschlange zurueck.
	 * @return Die Anzahl der aktuellen Ausbildungsvorhaben
	 */
	public int getNumberScheduledQueueEntries() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<?> list = db.createQuery("from AcademyQueueEntry where base=:base and scheduled=:scheduled order by position")
		.setParameter("base", this.getBase())
		.setParameter("scheduled", true)
		.list();
		return list.size();
	}
	
	/**
	 * Gibt die komplette Bauschlange zurueck.
	 * @return Die Bauschlange
	 */
	public AcademyQueueEntry[] getQueueEntries() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<?> list = db.createQuery("from AcademyQueueEntry where base=:base order by position")
		.setParameter("base", this.getBase())
		.list();
		AcademyQueueEntry[] entries = new AcademyQueueEntry[list.size()];
		int index = 0;
		for( Iterator<?> iter=list.iterator(); iter.hasNext(); ) {
			entries[index++] = (AcademyQueueEntry)iter.next();
		}
	
		return entries;
	}
	
	/**
	 * Berechnet die aktuell laufenden Ausbildungen neu.
	 * 
	 */
	public void rescheduleQueue() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		ConfigValue maxoffstotrainConfig = (ConfigValue)db.get(ConfigValue.class, "maxoffstotrain");
		double maxoffstotrain = Double.valueOf(maxoffstotrainConfig.getValue());
		int trainingoffs = 0;
		
		AcademyQueueEntry[] queue = this.getQueueEntries();
		
		for( AcademyQueueEntry entry : queue )  {
			entry.setScheduled(false);
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
		
		AcademyQueueEntry[] entries = this.getScheduledQueueEntries();
		if( offID < 0 ){
			return false;
		}
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
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<AcademyQueueEntry> list = Common.cast(db.createQuery("from AcademyQueueEntry where base=:base and position=:position")
											 .setParameter("base", this.getBase())
											 .setParameter("position", position)
											 .list());
		
		AcademyQueueEntry[] entries = new AcademyQueueEntry[list.size()];
		for(int i = 0; i < entries.length; i++)
		{
			entries[i] = list.get(i);
		}
	
		return entries[0];
	}
	
	/**
	 * Gibt den Bauschlangeneintrag mit der Id zurueck.
	 * @param id Die Id des Bauschlangeneintrags
	 * @return Der Bauschlangeneintrag
	 */
	public AcademyQueueEntry getQueueEntryById(int id) {
		
		AcademyQueueEntry[] entries = this.getQueueEntries();
		
		for( AcademyQueueEntry entry : entries ) {
			if( entry.getId() == id )
			{
				return entry;
			}
		}
		return null;
	}
}
