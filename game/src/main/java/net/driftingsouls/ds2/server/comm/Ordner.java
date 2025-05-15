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
package net.driftingsouls.ds2.server.comm;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repraesentiert einen Ordner im Postfach.
 * Hinweis: Die Ordner-ID 0 hat eine spezielle Bedeutung.
 * Sie kennzeichnet den Hauptordner, in dem sich alle Unterordner
 * befinden. Der Hauptordner existiert jedoch nicht als eigenständiger
 * Ordner in der Datenbank.
 * @author Christoph Peltz
 * @author Christopher Jung
 */
@Entity
@Table(name="ordner")
public class Ordner {
	/**
	 * Ein normaler Ordner.
	 */
	public static final int FLAG_NORMAL = 0;
	/**
	 * Der Muelleimer.
	 */
	public static final int FLAG_TRASH 	= 1;

	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="playerid", nullable=false)
	@ForeignKey(name="ordner_fk_users")
	private User owner;
	private int flags;
	private int parent;
	@Version
	private int version;

	/**
	 * Konstruktor.
	 */
	public Ordner() {
		// EMPTY
	}

	private Ordner(String name, User owner, Ordner parent) {
		this.name = name;
		this.owner = owner;
		this.parent = parent.getId();
	}

	/**
	 * Gibt den Ordner mit der angegebenen ID zurueck.
	 * Sollte kein solcher Ordner existieren, so wird <code>null</code> zurueckgegeben.
	 *
	 * @param id Die ID des Ordners
	 * @return Der Ordner
	 */
	public static Ordner getOrdnerByID( int id, EntityManager db ) {
		return getOrdnerByID(id, null, db);
	}

	/**
	 * Gibt den Ordner mit der angegebenen ID des angegebenen Benutzers zurueck.
	 * Sollte kein solcher Ordner existieren, so wird <code>null</code> zurueckgegeben.
	 *
	 * @param id Die ID des Ordners
	 * @param user Der Benutzer
	 * @return Der Ordner
	 */
	public static Ordner getOrdnerByID(int id, User user, EntityManager db) {

		if( id != 0 ) {
			Ordner ordner = db.find(Ordner.class, id);

			if( ordner == null ) {
				return null;
			}

			if( (user != null) && (ordner.getOwner() != user) ) {
				return null;
			}

			return ordner;
		}

        return createMainFolder(user);
	}

	public static Ordner createMainFolder(User user) {
		Ordner ordner = new Ordner();
		ordner.id = 0;
		ordner.name = "Hauptverzeichnis";
		ordner.flags = 0;
		ordner.owner = user;

		return ordner;
	}

	/**
	 * Gibt den Papierkorb eines Benutzers zurueck. Jeder Benutzer hat einen Papierkorb...
	 * @param user Der Benutzer
	 * @return Der Papierkorb
	 */
	public static Ordner getTrash ( User user ) {
		Context context = ContextMap.getContext();
		Object trash = context.getVariable(Ordner.class, "trash"+user.getId());
		if( trash == null ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			trash = db.createQuery("from Ordner where owner=:owner and bit_and(flags,:flag)!=0")
				.setEntity("owner", user)
				.setInteger("flag", Ordner.FLAG_TRASH)
				.uniqueResult();

			context.putVariable(Ordner.class, "trash"+user.getId(), trash);
		}
		return (Ordner)trash;
	}

	/**
	 * Loescht den Ordner. Alle im Ordner enthaltenen.
	 * Unterordner und Pms werden ebenfalls geloescht.
	 * @return <code>0</code>, falls das Loeschen erfolgreich war, <code>1</code>, falls erst noch eine PM gelesen werden muss
	 * und <code>2</code>, bei sonstigen Fehlern
	 */
	public int deleteOrdner(EntityManager db) {
		int result;
		if( (this.flags & Ordner.FLAG_TRASH) != 0 ) {
			return 2;
		}
		if( (result = PM.deleteAllInOrdner(this, this.owner, db)) != 0 ){
			return result;
		}
		List<Ordner> childFolders = db.createQuery("from Ordner where parent=:parent and owner=:owner", Ordner.class)
			.setParameter("parent", this)
			.setParameter("owner", this.owner)
			.getResultList();

		for (var subordner: childFolders)
		{
			if ((result = subordner.deleteOrdner(db)) != 0)
			{
				return result;
			}
		}

		db.remove(this);

		return 0;
	}

	/**
	 * Erstellt einen neuen Ordner fuer einen bestimmten Spieler.
	 * @param name Der Name des neuen Ordners
	 * @param parent Der Elternordner
	 * @param user Der Besitzer
	 * @return Der neue Ordner
	 * @throws IllegalArgumentException Falls der Elternordner der Papierkorb ist
	 */
	public static Ordner createNewOrdner( String name, Ordner parent, User user, EntityManager db ) throws IllegalArgumentException {
		if( (parent.getFlags() & Ordner.FLAG_TRASH) != 0 ) {
			throw new IllegalArgumentException("Ordnererstellung im Papierkorb nicht moeglich");
		}
		Ordner ordner = new Ordner(name, user, parent);
		db.persist(ordner);

		return ordner;
	}

	/**
	 * Gibt alle Kindordner, ob direkt oder indirekt, des Ordners zurueck.
	 * @return Liste mit Ordnern
	 */
	public List<Ordner> getAllChildren(EntityManager db) {
		List<Ordner> children = getChildren(db);

		for( int i=0; i < children.size(); i++ ){
			children.addAll( children.get(i).getAllChildren(db) );
		}

		return children;
	}

	/**
	 * Gibt alle direkten Kindordner des Ordners zurueck.
	 * @return Liste mit Ordnern
	 */
	public List<Ordner> getChildren(EntityManager db) {
		return db.createQuery("from Ordner where parent=:parent and owner=:owner", Ordner.class)
				.setParameter("parent", this.id)
				.setParameter("owner", this.owner)
				.getResultList();
	}

	/**
	 * Gibt alle PMs im Ordner selbst zurueck. PMs in unterordnern werden ignoriert.
	 * @return Die Liste aller PMs im Ordner
	 */
	public List<PM> getPms(EntityManager db) {
		return db.createQuery("from PM where empfaenger=:user and gelesen < :gelesen and ordner= :ordner order by time desc", PM.class)
			.setParameter("user", this.owner)
			.setParameter("ordner", this.id)
			.setParameter("gelesen", this.hasFlag(FLAG_TRASH) ? 10 : 2)
			.getResultList();
	}

	/**
	 * Gibt die Anzahl der im Ordner vorhandenen PMs zurueck.
	 * Unterordner werden nicht beruecksichtigt.
	 * @return Die Anzahl der PMs
	 */
	public int getPmCount(EntityManager db) {
		int gelesen = 2;
		if( (this.flags & Ordner.FLAG_TRASH) != 0 ){
			gelesen = 10;
		}

		return db.createQuery("select count(*) from PM where empfaenger=:owner and ordner=:ordner and gelesen<:read", Integer.class)
                .setParameter("owner", this.owner)
                .setParameter("ordner", this.id)
                .setParameter("read", gelesen)
                .getSingleResult();
	}

	/**
	 * Gibt die Anzahl der PMs in allen Ordnern unterhalb des Ordners zurueck.
	 * PMs in Unterordnern erhoehen die Anzahl der PMs im uebergeordneten Ordner.
	 * Zurueckgegeben wird eine Map, in der die Ordner-ID der Schluessel ist. Der Wert
	 * ist die Anzahl der PMs.
	 * @return Map mit der Anzahl der PMs in den jeweiligen Unterordnern
	 */
	public Map<Ordner,Integer> getPmCountPerSubOrdner(EntityManager db) {
		Map<Ordner,Integer> result = new HashMap<>();

		List<Ordner> ordners = this.getAllChildren(db);

		// Wenn der Ordner keine Unterordner hat, dann eine leere Map zurueckgeben
		if(ordners.isEmpty()) {
			return result;
		}

		// Array mit den Ordner-IDs erstellen sowie vermerken, wieviele Kindordner
		// ein Ordner besitzt
		Map<Ordner,Integer> childCount = new HashMap<>();
		Integer[] ordnerIDs = new Integer[ordners.size()];
		for( int i=0; i < ordners.size(); i++ ) {
			ordnerIDs[i] = ordners.get(i).getId();
			Common.safeIntInc(childCount, ordners.get(i).getParent(db));
		}

		// Map mit der Anzahl der PMs fuellen, die sich direkt im Ordner befinden
		int trash = getTrash( this.owner ).getId();

		List<Object[]> pmcounts = db.createQuery("select ordner, count(*) from PM " +
						"where empfaenger=:owner and ordner in (:ordnerIds) " +
						"and gelesen < case when ordner=:trash then 10 else 2 end " +
						"group by ordner", Object[].class)
				.setParameter("owner", this.owner)
				.setParameter("ordnerIds", ordnerIDs)
				.setParameter("trash", trash)
				.getResultList();
		for (Object[] pmcount : pmcounts) {
			result.put(Ordner.getOrdnerByID(((Number) pmcount[0]).intValue(), db), ((Number) pmcount[1]).intValue());
		}

		// PMs in den einzelnen Ordnern unter Beruecksichtigung der
		// Unterordner berechnen - maximal 100 Zyklen lang
		int maxloops = 100;
		while( (!childCount.isEmpty()) && (maxloops-- > 0) ) {
			for( int i=0; i < ordners.size(); i++ ) {
				Ordner aOrdner = ordners.get(i);
				if( childCount.get(aOrdner) != null ) {
					continue;
				}

				Ordner parent = aOrdner.getParent(db);
				// Die Anzahl der PMs des Elternordners um die
				// des aktuellen Ordners erhoehen. Anschliessend
				// den aktuellen Ordner aus der Liste entfernen
				if( !result.containsKey(parent) ) {
					result.put(parent, 0);
				}
				Integer child = result.get(aOrdner);
				result.put(parent, result.get(parent)+ (child != null ? child : 0) );

				childCount.put(parent, childCount.get(parent)-1);
				childCount.remove(aOrdner);

				ordners.remove(i);
				i--;
			}
		}

		return result;
	}

	/**
	 * Markiert alle Pms im Ordner als gelesen (ausser solche, welche als wichtig markiert sind).
	 *
	 */
	public void markAllAsRead(EntityManager db) {
		db.createQuery("update PM set gelesen=1 " +
				"where empfaenger= :user and ordner= :ordner and (gelesen=0 and bit_and(flags,:important)=0)")
			.setParameter("user", this.owner)
			.setParameter("ordner", this.id)
			.setParameter("important", PM.FLAGS_IMPORTANT)
			.executeUpdate();
	}

	/**
	 * Loescht alle Pms im Ordner (ausser solche, welche als wichtig markiert sind, aber noch
	 * nicht gelesen wurden).
	 *
	 */
	public void deleteAllPms(EntityManager db) {
		int trash = Ordner.getTrash( this.owner ).getId();

		db.createQuery("update PM set gelesen=2, ordner= :trash " +
				"where empfaenger= :user and ordner= :ordner and (gelesen=1 or bit_and(flags,:important)=0)")
			.setParameter("trash", trash)
			.setParameter("user", this.owner)
			.setParameter("ordner", this)
			.setParameter("important", PM.FLAGS_IMPORTANT)
			.executeUpdate();
	}

	/**
	 * Loescht alle PMs im Ordner, die von dem angegebenen Benutzer stammen
	 * (ausser solche, welche als wichtig markiert sind, aber noch
	 * nicht gelesen wurden).
	 * @param user Der Benutzer
	 */
	public void deletePmsByUser(User user, EntityManager db) {
		int trash = Ordner.getTrash( this.owner ).getId();

		db.createQuery("update PM set gelesen=2, ordner= :trash " +
				"where empfaenger= :user and sender=:sender and ordner= :ordner and (gelesen=1 or bit_and(flags,:important)=0)")
			.setParameter("trash", trash)
			.setParameter("user", this.owner)
			.setParameter("sender", user)
			.setParameter("ordner", this)
			.setParameter("important", PM.FLAGS_IMPORTANT)
			.executeUpdate();
	}

	/**
	 * Gibt die ID es Ordners zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Gibt den Namen des Ordners zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Setzt den Namen des Ordners.
	 * @param name der neue Name
	 */
	public void setName( String name ) {
		this.name = name;
	}


	/**
	 * Gibt den Eltern-Ordner zurueck.
	 * @return Der Elternordner
	 */
	public Ordner getParent(EntityManager db) {
		return Ordner.getOrdnerByID(this.parent, this.owner, db);
	}

	/**
	 * Setzt den Elternordner.
	 * @param parent Der Elternordner
	 */
	public void setParent(Ordner parent) {
		this.parent = parent.getId();
	}

	/**
	 * Gibt die Flags des Ordners zurueck.
	 * @return Die Flags
	 */
	public int getFlags() {
		return this.flags;
	}

	/**
	 * Setzt die Flags des Ordners.
	 * @param flags Die Flags
	 */
	public void setFlags(int flags) {
		this.flags = flags;
	}

	/**
	 * Prueft, ob der Ordner das angegebene Flag besitzt.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls der Ordner das Flag besitzt
	 */
	public boolean hasFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	/**
	 * Gibt den Besitzer des Ordners zurueck.
	 * @return Der Besitzer
	 */
	public User getOwner() {
		return this.owner;
	}

	/**
	 * Setzt den Besitzer des Ordners.
	 * @param owner Der Besitzer
	 */
	public void setOwner(User owner) {
		this.owner = owner;
	}

	@Override
	public int hashCode() {
		int result = 31 + id;
		return 31 * result + owner.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if( obj == null ) {
			return false;
		}

		if( getClass() != obj.getClass() ) {
			return false;
		}

		if( this.id != ((Ordner)obj).id ) {
			return false;
		}

		return this.owner.equals(((Ordner) obj).owner);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
