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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.notification.Notifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.List;

/**
 * <p>Repraesentiert eine PM in der Datenbank.</p>
 * <p>Eine PM ist immer mit einem Sender sowie Empfaenger verbunden.
 * Zudem befindet sie sich in einem Ordner, wobei 0 der Hauptordner ist.
 * Eine PM besitzt zudem einen Gelesen-Status. Ist dieser 0 so wurde die Nachricht noch
 * nicht gelesen. 1 kennzeichnet sie als gelesen. Wenn der Wert 2 oder hoeher ist
 * wurde die PM geloescht. Ihr gelesen-Status steigt dann jeden Tick um 1
 * bis ein Schwellenwert ueberschritten und die PM endgueltig geloescht wird.</p>
 * <p>Zudem steht ein Kommentarfeld fuer Anmerkungen sowie eine Reihe von Flags zur
 * Verfuegung.</p>
 * @author Christopher Jung
 * @author Christian Peltz
 *
 */
@Entity
@Table(name="transmissionen")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@org.hibernate.annotations.Table(
	appliesTo = "transmissionen",
	indexes = {@Index(name="empfaenger", columnNames = {"empfaenger", "gelesen"})}
)
public class PM {
	/**
	 * Die PM hat einen Admin-Hintergrund.
	 */
	public static final int FLAGS_ADMIN = 1;
	/**
	 * Es handelt sich um eine automatisch versendete PM.
	 */
	public static final int FLAGS_AUTOMATIC = 2;
	/**
	 * Die PM wurde durch den Tick versendet.
	 */
	public static final int FLAGS_TICK = 4;
	/**
	 * Die PM hat einen rassenspezifischen Hintergrund.
	 */
	public static final int FLAGS_OFFICIAL = 8;	// Spezieller (fraktions/rassenspezifischer) Hintergrund
	/**
	 * Die PM muss gelesen werden bevor sie geloescht werden kann.
	 */
	public static final int FLAGS_IMPORTANT = 16;	// Muss "absichtlich" gelesen werden

	/**
	 * Der PM-Empfaenger des Taskmanagers.
	 */
	public static final int TASK = Integer.MIN_VALUE;

	@Version
	private int version;

	@Transient
	private static Log log = LogFactory.getLog(PM.class);

	/**
	 * Sendet eine PM von einem Spieler zu einem anderen.
	 * @param from Der versendende Spieler
	 * @param to Der Spieler, der die PM erhalten soll
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 */
	public static void send( User from, int to, String title, String txt ) {
		send( from, to, title, txt, 0);
	}

	/**
	 * Sendet eine PM von einem Spieler zu einer Allianz.
	 * @param from Der versendende Spieler
	 * @param to Die Allianz, welche die PM erhalten soll
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 */
	public static void sendToAlly( User from, Ally to, String title, String txt ) {
		sendToAlly( from, to, title, txt, 0);
	}

	/**
	 * Sendet eine PM von einem Spieler zu einer Allianz.
	 * @param from Der versendende Spieler
	 * @param to Die Allianz, welche die PM erhalten soll
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 * @param flags Flags, welche die PM erhalten soll
	 */
	public static void sendToAlly( User from, Ally to, String title, String txt, int flags ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		String msg = "an Allianz "+to.getName()+"\n"+txt;

		if( title.length() > 100 ) {
			title = title.substring(0,100);
		}

		List<?> members = db.createQuery("from User where ally=:ally")
			.setEntity("ally", to)
			.list();
		for (Object member1 : members)
		{
			User member = (User) member1;

			PM pm = new PM(from, member, title, msg);
			pm.setFlags(flags);
			db.persist(pm);
		}
	}

	/**
	 * Sendet eine PM von einem Spieler zu einem anderen Spieler.
	 * @param from Der versendende Spieler
	 * @param to Die ID des Spielers, welche die PM erhalten soll
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 * @param flags Flags, welche die PM erhalten soll
	 */
	public static void send( User from, int to, String title, String txt, int flags ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		/*
		 *  Normale PM
		 */

		if( to != TASK ) {
			if( title.length() > 100 ) {
				title = title.substring(0,100);
			}
			if( txt.length() > 5000 ) {
				txt = txt.substring(0,5000);
			}

			User user = (User)db.get(User.class, to);
			if( user != null ) {
				PM pm = new PM(from, user, title, txt);
				pm.setFlags(flags);
				db.persist(pm);

				for( int forward : user.getUserValues(WellKnownUserValue.TBLORDER_PMS_FORWARD) )
				{
					if( forward != 0 ) {
						send(user, forward, "Fwd: "+title,
								"[align=center][color=green]- Folgende Nachricht ist soeben eingegangen -[/color][/align]\n" +
								"[b]Absender:[/b] [userprofile="+from.getId()+"]"+from.getName()+"[/userprofile] ("+from.getId()+")\n\n"+
								txt, flags);
					}
				}
			}
			else {
				context.addError("Transmission an Spieler "+to+" fehlgeschlagen");
			}
		}
		/*
		 * Taskverarbeitung (Spezial-PM)
		 */
		else {

			Taskmanager taskmanager = Taskmanager.getInstance();

			if( txt.equals("handletm") ) {
				taskmanager.handleTask(title, "pm_yes" );
			}
			else {
				taskmanager.handleTask(title, "pm_no" );
			}
		}
	}

	/**
	 * Sendet eine PM an alle Admins (spezifiziert durch den Konfigurationseintrag <code>ADMIN_PMS_ACCOUT</code>).
	 * @param from Der versendende Spieler
	 * @param title Der Titel der PM
	 * @param txt Der Text
	 * @param flags Flags, welche die PM erhalten soll
	 */
	public static void sendToAdmins(User from, String title, String txt, int flags  ) {
		String[] adminlist = new ConfigService().getValue(WellKnownConfigValue.ADMIN_PMS_ACCOUNT).split(",");
		for( String admin : adminlist ) {
			send(from, Integer.parseInt(admin), title, txt, flags);
		}
	}

	/**
	 * Loescht alle PMs aus einem Ordner eines bestimmten Spielers.
	 * Der Vorgang schlaegt fehl, wenn noch nicht alle wichtigen PMs gelesen wurden.
	 * @param ordner Der Ordner, dessen Inhalt geloescht werden soll
	 * @param user Der Besitzer des Ordners
	 * @return 0, falls der Vorgang erfolgreich war. 1, wenn ein Fehler aufgetreten ist und 2, falls nicht alle PMs gelesen wurden
	 */
	public static int deleteAllInOrdner( Ordner ordner, User user ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		List<?> pms = db.createQuery("from PM where ordner=:ordner")
			.setInteger("ordner", ordner.getId())
			.list();
		for (Object pm1 : pms)
		{
			PM pm = (PM) pm1;

			if (pm.getEmpfaenger().getId() != user.getId())
			{
				return 2;
			}
			int result = pm.delete();
			if (result != 0)
			{
				return result;
			}
		}

		return 0;	//geloescht
	}

	/**
	 * Verschiebt alle PMs von einem Ordner in einen anderen.
	 * @param source Der Ausgangsordner
	 * @param dest Der Zielordner
	 * @param user Der Besitzer der PM
	 */
	public static void moveAllToOrdner( Ordner source, Ordner dest , User user) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Ordner trash = Ordner.getTrash( user );

		List<?> pms = db.createQuery("from PM where ordner=:ordner and empfaenger=:user")
			.setInteger("ordner", source.getId())
			.setEntity("user", user)
			.list();
		for (Object pm1 : pms)
		{
			PM pm = (PM) pm1;
			int gelesen = (trash == source) ? 1 : pm.getGelesen();

			pm.setGelesen((trash == dest) ? 2 : gelesen);
			pm.setOrdner(dest.getId());
		}
	}

	/**
	 * Stelllt alle geloeschten PMs eines Spielers wieder her.
	 * @param user Der Spieler
	 */
	public static void recoverAll( User user ) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		int trash = Ordner.getTrash( user ).getId();

		db.createQuery("update PM set ordner=0,gelesen=1 where ordner=:ordner")
			.setInteger("ordner", trash)
			.executeUpdate();
	}

	@Id @GeneratedValue
	private int id;
	private int gelesen;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="sender", nullable=false)
	@ForeignKey(name="transmissionen_fk_users1")
	private User sender;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="empfaenger", nullable=false)
	@ForeignKey(name="transmissionen_fk_users2")
	private User empfaenger;
	@Column(nullable = false)
	private String title;
	private long time;
	// Kein Join auf Ordner, da der Hauptordner 0 nicht in der DB existiert
	private int ordner;
	private int flags;
	@Lob
	@Column(nullable = false)
	private String inhalt;
	@Lob
	@Column(nullable = false)
	private String kommentar;

	/**
	 * Konstruktor.
	 *
	 */
	public PM() {
		// EMPTY
	}

	/**
	 * Erstellt eine neue PM.
	 * @param sender Der Sender der PM
	 * @param empfaenger Der Empfaenger
	 * @param title Der Titel
	 * @param inhalt Der Inhalt
	 */
	public PM(User sender, User empfaenger, String title, String inhalt) {
		this.gelesen = 0;
		this.sender = sender;
		this.empfaenger = empfaenger;
		this.title = title;
		this.time = Common.time();
		this.ordner = 0;
		this.flags = 0;
		this.inhalt = inhalt;
		this.kommentar = "";
		if(empfaenger.getApiKey()!="") {
			new Notifier (empfaenger.getApiKey()).sendMessage("DS2: "+title+" von "+sender.getPlainname(), inhalt);		
		}
	}

	/**
	 * Gibt den Empfaenger zurueck.
	 * @return Der Empfaenger
	 */
	public User getEmpfaenger() {
		return empfaenger;
	}

	/**
	 * Setzt den Empfaenger.
	 * @param empfaenger Der Empfaenger
	 */
	public void setEmpfaenger(User empfaenger) {
		this.empfaenger = empfaenger;
	}

	/**
	 * Gibt die Flags zurueck.
	 * @return Die Flags
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * Prueft, ob die Nachricht das angegebene Flag hat.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Nachricht das Flag hat
	 */
	public boolean hasFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	/**
	 * Setzt die Flags der Nachricht.
	 * @param flags Die Flags
	 */
	public void setFlags(int flags) {
		this.flags = flags;
	}

	/**
	 * Gibt den Gelesen-Status der Nachricht zurueck.
	 * @return Der Gelesen-Status
	 */
	public int getGelesen() {
		return gelesen;
	}

	/**
	 * Setzt den Gelesen-Status der Nachricht.
	 * @param gelesen Der Gelesen-Status
	 */
	public void setGelesen(int gelesen) {
		this.gelesen = gelesen;
	}

	/**
	 * Gibt den Inhalt der Nachricht zurueck.
	 * @return Der Inhalt
	 */
	public String getInhalt() {
		return inhalt;
	}

	/**
	 * Setzt den Inahlt der Nachricht.
	 * @param inhalt Der Inhalt
	 */
	public void setInhalt(String inhalt) {
		this.inhalt = inhalt;
	}

	/**
	 * Gibt den Kommentar/die Anmerkung zur Nachricht zurueck.
	 * @return Der Kommentar
	 */
	public String getKommentar() {
		return kommentar;
	}

	/**
	 * Setzt den Kommentar/die Anmerkung zur Nachricht.
	 * @param kommentar Der Kommentar
	 */
	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}

	/**
	 * Gibt den Ordner zurueck, in dem sich die Nachricht befindet.
	 * @return Der Ordner
	 */
	public int getOrdner() {
		return ordner;
	}

	/**
	 * Setzt den Ordner, in dem sich die Nachricht befindet.
	 * @param ordner Der Ordner
	 */
	public void setOrdner(int ordner) {
		this.ordner = ordner;
	}

	/**
	 * Gibt den Sender der Nachricht zurueck.
	 * @return Der Sender
	 */
	public User getSender() {
		return sender;
	}

	/**
	 * Setzt den Sender der Nachricht.
	 * @param sender Der Sender
	 */
	public void setSender(User sender) {
		this.sender = sender;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem die Nachricht erstellt wurde.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Setzt den Zeitpunkt, an dem die Nachricht erstellt wurde.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Gibt den Titel der Nachricht zurueck.
	 * @return Der Titel
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Setzt den Titel der Nachricht.
	 * @param title Der Titel
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gibt die ID der Nachricht zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Stellt eine geloeschte PM wieder her.
	 */
	public void recover() {
		if( this.gelesen <= 1 ) {
			return;
		}

		int trash = Ordner.getTrash( this.empfaenger ).getId();
		if( this.ordner != trash ) {
			return;
		}
		this.gelesen = 1;
		this.ordner = 0;
	}

	/**
	 * Loescht die PM.
	 * @return 0, falls der Vorgang erfolgreich war. 1, wenn ein Fehler aufgetreten ist und 2, falls nicht alle PMs gelesen wurden
	 */
	public int delete() {
		if( this.gelesen > 1 ) {
			return 2;
		}

		Ordner trashCan = Ordner.getTrash(this.empfaenger);
		int trash;
		if(trashCan != null)
		{
			trash = trashCan.getId();
		}
		else
		{
			if(this.empfaenger != null)
			{
				log.error("User " + this.empfaenger.getId() + " has no trash can.");
			}
			else
			{
				log.error("An anonymous user (object was null) has no trash can.");
			}
			return 1;
		}
		if( this.hasFlag(PM.FLAGS_IMPORTANT) && (this.gelesen < 1) ) {
			return 1;
		}
		this.gelesen = 2;
		this.ordner = trash;

		return 0;	//geloescht
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
