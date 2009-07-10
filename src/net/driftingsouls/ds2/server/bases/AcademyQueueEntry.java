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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.namegenerator.NameGenerator;

import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.Session;

/**
 * 
 *
 */
@Entity
@Table(name="academy_queues")
public class AcademyQueueEntry {
	
	private static Map<Integer,Offizier.Ability> dTrain = new HashMap<Integer,Offizier.Ability>();
	
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
	private Base base;
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
	 * @param base Die Basis
	 * @param training Der zu trainierende offz (id, bei neuen offizieren -1 bis -4)
	 * @param remaining Die verbleibende Bauzeit
	 */
	public AcademyQueueEntry(Base base, int training, int remaining) 
	{
		this.base = base;
		this.position = getNextEmptyPosition();
		this.training = training;
		this.remaining = remaining;
	}
	
	/**
	 * Erstellt einen neuen Bauschlangeneintrag.
	 * @param basis Die Basis
	 * @param training Der zu trainierende offz (id, bei neuen offizieren -1 bis -4)
	 * @param remaining Die verbleibende Bauzeit
	 * @param trainingtype Die Faehigkeit die ausgebildet wird
	 */
	public AcademyQueueEntry(Base basis, int training, int remaining, int trainingtype) {
		this(basis,training,remaining);
		this.trainingtype = trainingtype;
	}
		
	private int getNextEmptyPosition() 
	{
		Session db = ContextMap.getContext().getDB();
		
		AcademyQueueEntry last = (AcademyQueueEntry)db.createQuery("from AcademyQueueEntry where base=:base order by position desc").setParameter("base", this.base).setMaxResults(1).uniqueResult();
		
		if(last == null)
		{
			return 1;
		}
		
		return last.getPosition() + 1;
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
	public Base getBasis() {
		return this.base;
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
		User owner = this.base.getOwner();
		Base base = this.base;
		Academy academy = (Academy)db.get(Academy.class, base.getId());
		int race = owner.getRace();
		int position = this.position;
		
		// Loesche Eintrag und berechne Queue neu
		db.delete(this);
		List<AcademyQueueEntry> entries = Common.cast(db.createQuery("from AcademyQueueEntry where base=:base and position > :position order by position")
				.setParameter("base", base)
				.setParameter("position", position)
				.list());

		for(AcademyQueueEntry entry: entries)
		{
			entry.setPosition(entry.getPosition() - 1);
		}
		
		academy.rescheduleQueue();
		
		if(training == 0)
		{
			/*
			 * Neuer Offizier wurde ausgebildet
			 */
			String offiname = getNewOffiName(race);

			Offizier offz = new Offizier(owner, offiname);
			
			if( !Offiziere.LIST.containsKey(-offizier) ) {
				offizier = -Offiziere.LIST.keySet().iterator().next();
			}

			SQLResultRow offi = Offiziere.LIST.get(-offizier);

			offz.setAbility(Offizier.Ability.ING, offi.getInt("ing"));
			offz.setAbility(Offizier.Ability.WAF, offi.getInt("waf"));
			offz.setAbility(Offizier.Ability.NAV, offi.getInt("nav"));
			offz.setAbility(Offizier.Ability.SEC, offi.getInt("sec"));
			offz.setAbility(Offizier.Ability.COM, offi.getInt("com"));

			int spec = RandomUtils.nextInt(((int[])offi.get("specials")).length);
			spec = ((int[])offi.get("specials"))[spec];

			offz.setSpecial(Offizier.Special.values()[spec-1]);

			offz.setDest("b", base.getId());
			id = (Integer)db.save(offz);
		}
		else
		{
			/*
			 * Offizier wurde weitergebildet
			 */
			
			final Offizier.Ability ability = dTrain.get(training);
			
			final Offizier offz = Offizier.getOffizierByID(offizier);
			offz.setAbility(ability, offz.getAbility(ability)+2);
			if( !academy.isOffizierScheduled(offz.getID()) )
			{
				offz.setDest("b", base.getId());
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
	 * Gibt einen neuen Offiziersnamen aus.
	 * @param race Die Rasse aus der der Offiziersname generiert werden soll
	 * @return Der Offiziersname
	 */
	private String getNewOffiName(int race) 
	{		
		String offiname = "Offizier";

		NameGenerator generator = Rassen.get().rasse(race).getNameGenerator(Rasse.GeneratorType.PERSON);
		if( generator != null ) {
			offiname = generator.generate(1)[0];
		}

		return offiname;
	}
	
	/**
	 * Gibt zurueck ob es sich bei dem Eintrag um den letzten Eintrag der Bauschlange handelt.
	 * @return <code>true</code> wenn es sich um den letzten Eintrag handelt, ansonsten <code>false</code>
	 */
	public boolean isLastPosition() {
		
		int lastposition = this.getNextEmptyPosition()-1;
		
		if( this.getPosition() == lastposition )
		{
			return true;
		}
		return false;
	}

	/**
	 * Loescht den Eintrag aus der Bauschlange.
	 */	
	public void deleteQueueEntry() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		db.delete(this);
	}
}
