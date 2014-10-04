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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
	@Lob
	@Column(nullable = false)
	private String name;
	@Column(nullable = false)
	private String plainname;
	@Column(nullable = false)
	private Date founded;
	private int tick;
	@OneToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="president", nullable=false)
	@ForeignKey(name = "ally_fk_users")
	private User president;
	@Lob
	@Column(nullable = false)
	private String description;
	@Lob
	@Column(nullable = false)
	private String hp;
	@Column(nullable = false)
	private String allytag;
	private boolean showastis;
	private byte showGtuBieter;
	private byte showlrs;
	@Column(nullable = false)
	private String pname;
	@Lob
	@Column(nullable = false)
	private String items;
	private short lostBattles;
	private short wonBattles;
	private int destroyedShips;
	private int lostShips;

	@OneToMany(mappedBy="ally", cascade=CascadeType.ALL)
	@OrderBy("rang")
	private Set<AllyRangDescriptor> rangDescriptors;

	@OneToMany(mappedBy = "ally")
	private Set<User> members;

	@Version
	private int version;

	@OneToMany(mappedBy = "ally")
	private Set<AllyPosten> posten;

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
		this.rangDescriptors = new TreeSet<>();
		this.members = new HashSet<>();

		this.name = name;
		this.plainname = Common._titleNoFormat(name);
		this.president = president;
		this.posten = new HashSet<>();
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
	 * Gibt die Liste aller Mitglieder zurueck.
	 * @return Die Liste aller Spieler der Allianz
	 */
	@SuppressWarnings("unchecked")
	public List<User> getMembers() {
		return new ArrayList<>(members);
	}

	/**
	 * Entfernt alle Mitglieder aus der Allianz.
	 */
	public void removeAllMembers()
	{
		this.members.clear();
	}

	/**
	 * Entfernt ein Mitglied aus der Allianz.
	 * @param member Das Mitglied
	 */
	public void removeMember(User member)
	{
		this.members.remove(member);
	}

	/**
	 * Gibt die Anzahl an Allymitgliedern zurueck.
	 * @return Die Anzahl der Allymitglieder
	 */
	public long getMemberCount() {
		return members.size();
	}

	/**
	 * Gibt die Posten der Allianz zurueck.
	 * @return Die Posten
	 */
	public Set<AllyPosten> getPosten()
	{
		return this.posten;
	}

	/**
	 * Fuegt einen User zu dieser Allianz hinzu.
	 * Achtung: Der User wird ausschliesslich hinzugefuegt, weitere Verwaltungsmassnahmen
	 * (z.B. Namensaenderungen) sind gesondert durchzufuehren.
	 * @param user Der User
	 */
	public void addUser(User user)
	{
		user.setAlly(this);
		members.add(user);
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
		SortedSet<Rang> result = new TreeSet<>();
		for( AllyRangDescriptor rang : this.rangDescriptors )
		{
			result.add(new Rang(rang.getRang(), rang.getName(), rang.getImage()));
		}

		result.addAll(Medals.get().raenge().values());

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
