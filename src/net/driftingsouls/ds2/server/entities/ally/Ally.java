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
package net.driftingsouls.ds2.server.entities.ally;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

import org.hibernate.Query;
import org.hibernate.annotations.BatchSize;

/**
 * Repraesentiert eine Allianz in DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ally")
@BatchSize(size=50)
public class Ally {
	@Id	@GeneratedValue
	private int id;
	private String name;
	private String plainname;
	private Date founded;
	private int tick;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="president", nullable=false)
	private User president;
	private String description;
	private String hp;
	private String allytag;
	private boolean showastis;
	private byte showGtuBieter;
	private byte showlrs;
	private String pname;
	private String items;
	private short lostBattles;
	private short wonBattles;
	private int destroyedShips;
	private int lostShips;
	
	@OneToMany(mappedBy="ally", cascade=CascadeType.ALL)
	@OrderBy("rang")
	private Set<AllyRangDescriptor> rangDescriptors;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 */
	public Ally() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Allianz. Als Gruendungszeitpunkt
	 * wird der aktuelle Zeitpunkt eingetragen.
	 * @param name Der Name der Allianz
	 * @param president Der Praesident der Allianz
	 */
	public Ally(String name, User president) {
		this.description = "";
		this.hp = "";
		this.allytag = "[name]";
		this.pname = "Pr&auml;sident";
		this.founded = new Date();
		this.tick = ContextMap.getContext().get(ContextCommon.class).getTick();
		this.showastis = true;
		this.showGtuBieter = 0;
		this.showlrs = 1;
		this.items = "";
		
		this.name = name;
		this.plainname = Common._titleNoFormat(name);
		this.president = president;
	}

	/**
	 * Gibt den Allianz-Tag zurueck.
	 * @return Der Allianz-Tag
	 */
	public String getAllyTag() {
		return allytag;
	}

	/**
	 * Setzt den Allianz-Tag.
	 * @param allytag Der neue Tag
	 */
	public void setAllyTag(String allytag) {
		this.allytag = allytag;
	}

	/**
	 * Gibt die Beschreibung der Allianz zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Setzt die Beschreibung der Allianz.
	 * @param description Die neue Beschreibung
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gibt zurueck, wieviele Schiffe von der Allianz zerstoert wurden.
	 * @return Die Anzahl der zerstoerten Schiffe
	 */
	public int getDestroyedShips() {
		return destroyedShips;
	}

	/**
	 * Setzt die Anzahl der durch die Allianz zerstoerten Schiffe.
	 * @param destroyedShips Die neue Anzahl der zerstoerten Schiffe
	 */
	public void setDestroyedShips(int destroyedShips) {
		this.destroyedShips = destroyedShips;
	}

	/**
	 * Gibt das Datum zurueck, an dem die Allianz gegruendet wurde.
	 * @return Das Gruendungsdatum
	 */
	public Date getFounded() {
		return new Date(founded.getTime());
	}

	/**
	 * Setzt das Gruendungsdatum der Allianz.
	 * @param founded Das neue Gruendungsdatum
	 */
	public void setFounded(Date founded) {
		this.founded = new Date(founded.getTime());
	}

	/**
	 * Gibt die Homepage der Allianz zurueck.
	 * @return Die Homepage
	 */
	public String getHp() {
		return hp;
	}

	/**
	 * Setzt die Homepage der Allianz.
	 * @param hp Die Homepage
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Gibt die Allianzitems zurueck.
	 * @return Der Itemstring mit den Items
	 */
	public String getItems() {
		return items;
	}
	
	/**
	 * Setzt die Allianzitems.
	 * @param items Der neue Itemstring
	 */
	public void setItems(String items) {
		this.items = items;
	}

	/**
	 * Gibt die Anzahl der verlorenen Schlachten zurueck.
	 * @return Die Anzahl der verlorenen Schlachten
	 */
	public short getLostBattles() {
		return lostBattles;
	}

	/**
	 * Setzt die Anzahl der verlorenen Schlachten.
	 * @param lostBattles Die Anzahl der verlorenen Schlachten
	 */
	public void setLostBattles(short lostBattles) {
		this.lostBattles = lostBattles;
	}

	/**
	 * Gibt die Anzahl der verlorenen Schiffe zurueck.
	 * @return Die Anzahl der verlorenen Schiffe
	 */
	public int getLostShips() {
		return lostShips;
	}

	/**
	 * Setzt die Anzahl der verlorenen Schiffe.
	 * @param lostShips Die Anzahl der verlorenen Schiffe
	 */
	public void setLostShips(int lostShips) {
		this.lostShips = lostShips;
	}

	/**
	 * Gibt den Namen der Allianz zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen der Allianz.
	 * @param name Der neue Name
	 */
	public void setName(String name) {
		this.name = name;
		this.plainname = Common._titleNoFormat(name);
	}

	/**
	 * Gibt den Namen der Allianz ohne Tags zurueck.
	 * @return Der Name der Allianz ohne Tags
	 */
	public String getPlainname() {
		return plainname;
	}

	/**
	 * Gibt den Namen des Praesidentenamts zurueck.
	 * @return Der Name des Praesidentneamts
	 */
	public String getPname() {
		return pname;
	}

	/**
	 * Setzt den Namen des Praesidentenamts.
	 * @param pname Der neue Name des Amts
	 */
	public void setPname(String pname) {
		this.pname = pname;
	}

	/**
	 * Gibt den Praesidenten zurueck.
	 * @return Der Praesident
	 */
	public User getPresident() {
		return president;
	}

	/**
	 * Setzt den Allianzpraesidenten.
	 * @param president Der neue Praesident
	 */
	public void setPresident(User president) {
		this.president = president;
	}

	/**
	 * Gibt zurueck, ob in der Sternenkarte Asteroiden von Allianzmitgliedern angezeigt werden sollen .
	 * @return <code>true</code>, falls Asteroiden von Allianzmitgliedern angezeigt werden sollen
	 */
	public boolean getShowAstis() {
		return showastis;
	}

	/**
	 * Setzt, ob Asteroiden von Allianzmitgliedern in der Sternenkarte angezeigt werden sollen.
	 * @param showastis <code>true</code>, falls Asteroiden von Allianzmitgliedern angezeigt werden sollen
	 */
	public void setShowAstis(boolean showastis) {
		this.showastis = showastis;
	}

	/**
	 * Gibt zurueck, ob Versteigerungen von Allianzmitgliedern inkl. Namen angezeigt werden sollen.
	 * @return <code>true</code>, falls bei Versteigerungen von Allianzmitgliedern deren Name angezeigt werden soll
	 */
	public boolean getShowGtuBieter() {
		return showGtuBieter != 0;
	}

	/**
	 * Setzt, ob Versteigerungen von Allianzmitgliedern inkl. Namen angezeigt werden sollen.
	 * @param showGtuBieter <code>true</code>, falls bei Versteigerungen von Allianzmitgliedern deren Name angezeigt werden soll
	 */
	public void setShowGtuBieter(boolean showGtuBieter) {
		this.showGtuBieter = showGtuBieter ? (byte)1 : (byte)0;
	}

	/**
	 * Gibt zurueck, ob die LRS-Sensorendaten von Aufklaerern in der Sternenkarte geteilt werden sollen.
	 * @return <code>true</code>, falls sie geteilt werden sollen
	 */
	public boolean getShowLrs() {
		return showlrs != 0;
	}

	/**
	 * Setzt, ob die LRS-Sensorendaten von Aufklaerern in der Sternenkarte geteilt werden sollen.
	 * @param showlrs <code>true</code>, falls sie geteilt werden sollen
	 */
	public void setShowLrs(boolean showlrs) {
		this.showlrs = showlrs ? (byte)1 : (byte)0;
	}

	/**
	 * Gibt den Tick zurueck, an dem die Allianz gegruendet wurde.
	 * @return Der Tick an den die Allianz gegruendet wurde
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick, an dem die Allianz gegruendet wurde.
	 * @param tick Der Tick an dem die Allianz gegruendet wurde
	 */
	public void setTick(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt die Anzahl der gewonnenen Schlachten zurueck.
	 * @return Die Anzahl der gewonnenen Schlachten
	 */
	public short getWonBattles() {
		return wonBattles;
	}

	/**
	 * Setzt die Anzahl der gewonnenen Schlachten.
	 * @param wonBattles Die Anzahl der gewonnenen Schlachten
	 */
	public void setWonBattles(short wonBattles) {
		this.wonBattles = wonBattles;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Gibt die Liste aller Mitglieder zurueck, welche Minister und/oder Praesident sind.
	 * @return Die Liste aller Spieler mit Minister-/Praesidentenposten
	 */
	@SuppressWarnings("unchecked")
	public List<User> getSuperMembers() {
		return ContextMap.getContext()
			.getDB()
			.createQuery("select distinct u from User u join u.ally a " +
					"where a.id= :allyId and (u.allyposten is not null or a.president=u.id)")
			.setInteger("allyId", this.getId())
			.list();
	}
	
	/**
	 * Gibt die Liste aller Mitglieder zurueck.
	 * @return Die Liste aller Spieler der Allianz
	 */
	@SuppressWarnings("unchecked")
	public List<User> getMembers() {
		return ContextMap.getContext().getDB()
			.createQuery("from User where ally= :ally")
			.setEntity("ally", this)
			.list();
	}
	
	/**
	 * Gibt die Anzahl an Allymitgliedern zurueck.
	 * @return Die Anzahl der Allymitglieder
	 */
	public long getMemberCount() {
		return (Long)ContextMap.getContext().getDB()
			.createQuery("select count(*) from User where ally= :ally")
			.setEntity("ally", this)
			.iterate()
			.next();
	}
	
	/**
	 * Loescht die Allianz.
	 *
	 */
	public void destroy() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		List<?> chnList = db.createQuery("from ComNetChannel where allyOwner=?")
			.setInteger(0, this.id)
			.list();
		for( Iterator<?> iter=chnList.iterator(); iter.hasNext(); ) {
			ComNetChannel channel = (ComNetChannel)iter.next();
			
			db.createQuery("delete from ComNetVisit where channel=?")
				.setEntity(0, channel)
				.executeUpdate();
			
			db.createQuery("delete from ComNetEntry where channel=?")
				.setEntity(0, channel)
				.executeUpdate();
			
			db.delete(channel);
		}
		
		int tick = ContextMap.getContext().get(ContextCommon.class).getTick();
		
		List<?> uids = db.createQuery("from User where ally=?")
			.setEntity(0, this)
			.list();
		for( Iterator<?> iter=uids.iterator(); iter.hasNext(); ) {
			User auser = (User)iter.next();
			
			auser.addHistory(Common.getIngameTime(tick)+": Verlassen der Allianz "+this.name+" im Zuge der Aufl&ouml;sung dieser Allianz");
			auser.setAlly(null);
			if( auser.getAllyPosten() != null ) {
				AllyPosten posten = auser.getAllyPosten();
				auser.setAllyPosten(null);
				db.delete(posten);
			}
			auser.setName(auser.getNickname());
		}
		
		db.createQuery("delete from AllyPosten where ally=?")
			.setEntity(0, this)
			.executeUpdate();
		
		// Delete Ally from running Battles
		Set<Battle> battles = new LinkedHashSet<Battle>();
		
		String query = "from Battle " +
		"where ally1 = :ally or ally2 = :ally";

		Query battleQuery = db.createQuery(query)
			.setInteger("ally", this.getId());

		battles.addAll(Common.cast(battleQuery.list(), Battle.class));
		
		for( Battle battle : battles ) {
			if(battle.getAlly(0) == this.getId())
			{
				battle.setAlly(0, 0);
			}
			if (battle.getAlly(1) == this.getId())
			{
				battle.setAlly(1, 0);	
			}
		}
		
		db.delete(this);
	}
	
	/**
	 * Entfernt einen Spieler aus der Allianz.
	 * @param user Der Spieler
	 */
	public void removeUser(User user) {
		final Context context = ContextMap.getContext();
		final org.hibernate.Session db = context.getDB();
		
		user.setAlly(null);
		user.setAllyPosten(null);
		user.setName(user.getNickname());
		
		db.createQuery("update Battle set ally1=0 where commander1= :user and ally1= :ally")
			.setEntity("user", user)
			.setInteger("ally", this.id)
			.executeUpdate();
	
		db.createQuery("update Battle set ally2=0 where commander2= :user and ally2= :ally")
			.setEntity("user", user)
			.setInteger("ally", this.id)
			.executeUpdate();
		
		int tick = context.get(ContextCommon.class).getTick();
		user.addHistory(Common.getIngameTime(tick)+": Verlassen der Allianz "+this.name);

		checkForLowMemberCount();
	}
	
	/**
	 * Prueft, ob die Allianz noch genug Mitglieder hat um ihr
	 * Fortbestehen zu sichern. Falls dies nicht mehr der Fall ist
	 * wird eine entsprechende Task gesetzt und die Mitglieder davon
	 * in kenntnis gesetzt.
	 *
	 */
	public void checkForLowMemberCount() {
		final org.hibernate.Session db = ContextMap.getContext().getDB();
		
		// Ist der Praesident kein NPC (negative ID) ?
		if( this.president.getId() > 0 ) {
			long count = this.getMemberCount();
			if( count < 3 ) {
				Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21, Integer.toString(this.id), "", "" );
			
				final User nullUser = (User)db.get(User.class, 0);
				
				List<User> supermembers = this.getSuperMembers();
				for( User supermember : supermembers ) {
					PM.send(nullUser, supermember.getId(), "Drohende Allianzaufl&oum;sung", 
							"[Automatische Nachricht]\nAchtung!\nDurch den j&uuml;ngsten Weggang eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder um weiterhin zu bestehen. Du hast nun 21 Ticks Zeit diesen Zustand zu &auml;ndern. Andernfalls wird die Allianz aufgel&ouml;&szlig;t.");
				}
			}
		}
	}
	
	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
	
	/**
	 * Gibt alle fuer die Allianz hinterlegte Rangbezeichnungen fuer NPC-Raenge zurueck.
	 * @return Die Liste
	 */
	public Set<AllyRangDescriptor> getRangDescriptors()
	{
		return this.rangDescriptors;
	}
	
	/**
	 * Gibt die Liste aller bekannten Raenge dieser Allianz zurueck. Dies umfasst sowohl
	 * die spezifischen Raenge dieser Allianz als auch alle allgemeinen Raenge ({@link Medals#raenge()}).
	 * @return Die nach Rangnummer sortierte Liste der Rangbezeichnungen
	 */
	public SortedSet<Rang> getFullRangNameList()
	{
		SortedSet<Rang> result = new TreeSet<Rang>();
		for( AllyRangDescriptor rang : this.rangDescriptors )
		{
			result.add(new Rang(rang.getRang(), rang.getName()));
		}
		
		for( Rang rang : Medals.get().raenge().values() )
		{
			result.add(rang);
		}
		
		return result;
	}
	
	/**
	 * Gibt den Anzeigenamen fuer die angegebene Rangnummer zurueck.
	 * Sofern die Allianz ueber eine eigene Bezeichnung verfuegt wird diese zurueckgegeben.
	 * Andernfalls wird die globale Bezeichnung verwendet.
	 * @param rangNr Die Rangnummer
	 * @return Der Anzeigename
	 */
	public String getRangName(int rangNr)
	{
		for( AllyRangDescriptor rang : this.rangDescriptors )
		{
			if( rang.getRang() == rangNr )
			{
				return rang.getName();
			}
		}
		
		return Medals.get().rang(rangNr).getName();
	}
}
