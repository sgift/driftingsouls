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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.Session;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ein Eintrag in der Produktionsschlange einer Akademie.
 *
 */
@Entity
@Table(name="academy_queue_entry")
public class AcademyQueueEntry {

	private static Map<Integer,Offizier.Ability> dTrain = new HashMap<>();

	static {
		dTrain.put(1, Offizier.Ability.ING);
		dTrain.put(2, Offizier.Ability.WAF);
		dTrain.put(3, Offizier.Ability.NAV);
		dTrain.put(4, Offizier.Ability.SEC);
		dTrain.put(5, Offizier.Ability.COM);
	}

	/**
	 * Lognachrichten der zuletzt aufgerufenen Methoden. Die Nachrichten sind Thread-Lokal.
	 */
	@Transient
	public final ContextLocalMessage MESSAGE = new ContextLocalMessage();

	@Id @GeneratedValue
	private int id;
	@ManyToOne(optional = false)
	@JoinColumn(nullable = false)
	@ForeignKey(name = "academy_queues_fk_academy")
	private Academy academy;
	private int position;
	private int training;
	private int trainingtype = 0;
	private int remaining = 0;
	private boolean scheduled = false;

	/**
	 * Konstruktor.
	 *
	 */
	public AcademyQueueEntry()
	{
		// EMPTY
	}

	/**
	 * Erstellt einen neuen Bauschlangeneintrag.
	 * @param academy Die Akademie zu der die Bauschlange gehoert
	 * @param training Der zu trainierende offz (id, bei neuen offizieren -1 bis -4)
	 * @param remaining Die verbleibende Bauzeit
	 */
	public AcademyQueueEntry(Academy academy, int training, int remaining)
	{
		this.academy = academy;
		this.position = getNextEmptyPosition();
		this.training = training;
		this.remaining = remaining;
	}

	/**
	 * Erstellt einen neuen Bauschlangeneintrag.
	 * @param academy Die Akademie zu der die Bauschlange gehoert
	 * @param training Der zu trainierende offz (id, bei neuen offizieren -1 bis -4)
	 * @param remaining Die verbleibende Bauzeit
	 * @param trainingtype Die Faehigkeit die ausgebildet wird
	 */
	public AcademyQueueEntry(Academy academy, int training, int remaining, int trainingtype) {
		this(academy,training,remaining);
		this.trainingtype = trainingtype;
	}

	private int getNextEmptyPosition()
	{
		int maxpos = 0;
		for( AcademyQueueEntry entry : this.academy.getQueueEntries() )
		{
			maxpos = Math.max(entry.getPosition(), maxpos);
		}
		return maxpos+1;
	}

	/**
	 * Gibt die Id zurueck.
	 * @return Die Id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Gibt die Basis zurueck, zu der die Bauschlange gehoert.
	 * @return Die Basis
	 */
	public Academy getAcademy() {
		return this.academy;
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
	 * Gibt die ID der ausgebildeten Offiziers zurueck.
	 * @return Die ID (-1 bis -4 bei neu gebauten Offizieren)
	 */
	public int getTraining() {
		return this.training;
	}

	/**
	 * Gibt den Typ der Auusbildung zurueck.
	 * @return Der Typ der Ausbildung (0 = neuer Offizier, 1-5 Aufgewertete Eigenschaft)
	 */
	public int getTrainingType() {
		return this.trainingtype;
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
	 * Beendet den Trainingsprozess dieses Bauschlangeneintrags erfolgreich.
	 *
	 * @return <code>true</code> wenn erfolgreich, ansonsten <code>false</code>
	 */
	public boolean finishBuildProcess() {
		MESSAGE.get().setLength(0);

		Context context = ContextMap.getContext();
		Session db = context.getDB();

		if( !this.isScheduled() ) {
			return false;
		}

		// Speichere alle wichtigen Daten
		int offizier = this.training;
		int training = this.trainingtype;
		User owner = this.academy.getBase().getOwner();
		int position = this.position;

		// Loesche Eintrag und berechne Queue neu
		db.delete(this);
		this.academy.getQueueEntries().remove(this);

		for(AcademyQueueEntry entry: this.academy.getQueueEntries())
		{
			if( entry.getPosition() > position )
			{
				entry.setPosition(entry.getPosition() - 1);
			}
		}
		db.flush();
		academy.rescheduleQueue();

		if(training == 0)
		{
			/*
			 * Neuer Offizier wurde ausgebildet
			 */
			String offiname = owner.getPersonenNamenGenerator().generiere();

			Offizier offz = new Offizier(owner, offiname);

			if( !Offiziere.LIST.containsKey(-offizier) ) {
				offizier = -Offiziere.LIST.keySet().iterator().next();
			}

			Offiziere.Offiziersausbildung offi = Offiziere.LIST.get(-offizier);

			for (Offizier.Ability ability : Offizier.Ability.values())
			{
				offz.setAbility(ability, offi.getAbility(ability));
			}

			int spec = ThreadLocalRandom.current().nextInt(offi.getSpecials().length);
			spec = offi.getSpecials()[spec];

			offz.setSpecial(Offizier.Special.values()[spec-1]);

			offz.setTraining(false);
			offz.stationierenAuf(academy.getBase());
			id = (Integer)db.save(offz);
		}
		else
		{
			/*
			 * Offizier wurde weitergebildet
			 */

			final Offizier.Ability ability = dTrain.get(training);

			final Offizier offz = Offizier.getOffizierByID(offizier);
			if( offz == null )
			{
				return true;
			}
			offz.setAbility(ability, offz.getAbility(ability)+10);
			if( !academy.isOffizierScheduled(offz.getID()) )
			{
				offz.setTraining(false);
			}
			id = (Integer)db.save(offz);
		}

		return true;
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
	 * Gibt zurueck ob es sich bei dem Eintrag um den letzten Eintrag der Bauschlange handelt.
	 * @return <code>true</code> wenn es sich um den letzten Eintrag handelt, ansonsten <code>false</code>
	 */
	public boolean isLastPosition() {

		int lastposition = this.getNextEmptyPosition()-1;

		return this.getPosition() == lastposition;
	}

	/**
	 * Loescht den Eintrag aus der Bauschlange.
	 */
	public void deleteQueueEntry() {
		this.getAcademy().getQueueEntries().remove(this);
	}
}
