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
package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Index;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;


/**
 * Die Benutzerklasse von DS.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="users")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("'default'")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public abstract class BasicUser {
/**
	 * Sortiert die Benutzer entsprechend ihres Anzeigenamens.
	 */
	public static final Comparator<BasicUser> PLAINNAME_ORDER = (o1, o2) -> {
		int diff = o1.getPlainname().compareToIgnoreCase(o2.getPlainname());
		if( diff != 0 )
		{
			return diff;
		}
		return o1.getId()-o2.getId();
	};

	@Id
	private int id;

	@Index(name="basicuser_un")
	@Column(nullable = false)
	private String un;
	@Column(nullable = false)
	private String name;
	@Column(nullable = false)
	private String passwort;
	private int inakt;
	private int signup;
	@Column(nullable = false)
	private String email;
	@Column(name="log_fail", nullable = false)
	private int logFail;
	private int accesslevel;
	@Column(nullable = false)
	private String nickname;
	@Column(nullable = false)
	private String plainname;
	private byte disabled;
	@OneToMany(mappedBy="user", cascade=CascadeType.ALL, orphanRemoval = true)
	private Set<Permission> permissions;

	@Version
	private int version;

	@Transient
	protected BasicUser attachedUser;

	/**
	 * Konstruktor.
	 *
	 */
	public BasicUser() {
		attachedUser = null;
		this.permissions = new HashSet<>();
	}

	/**
	 * Liefert die User-ID des User-Objekts zurueck.
	 *
	 * @return Die User-ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt alle expliziten Permissions dieses Nutzers zurueck.
	 * @return Die Permissions
	 */
	public Set<Permission> getPermissions()
	{
		return this.permissions;
	}

	/**
	 * Koppelt den Benutzer temporaer an einen anderen. Dadurch werden AccessLevel und Flags
	 * des angegebenen Benutzers verwendet.
	 * @param user Der Benutzer, der temporaer an diesen gekoppelt werden soll
	 */
	public void attachToUser( BasicUser user ) {
		this.attachedUser = user;
	}

	/**
	 * Macht alle geladenen Benutzereigenschaften dem Templateengine bekannt.
	 * Die daraus resultierenden Template-Variablen haben die Form "user."+Datenbankname.
	 * Die Eigenschaft Wait4Vacation, welche den Datenbanknamen "wait4vac" hat, wuerde sich
	 * somit in der Template-Variablen "user.wait4vac" wiederfinden.
	 *
	 * @param templateEngine Das Template-Engine, in dem die Variablen gesetzt werden sollen
	 */
	public void setTemplateVars(TemplateEngine templateEngine) {
		setTemplateVars(templateEngine, "user");
	}

	/**
	 * Macht alle geladenen Benutzereigenschaften dem Templateengine bekannt.
	 * Die daraus resultierenden Template-Variablen haben die Form Prefix+"."+Datenbankname.
	 * Die Eigenschaft Wait4Vacation, welche den Datenbanknamen "wait4vac" hat, wuerde sich, beim
	 * Prefix "activeuser", somit in der Template-Variablen "activeuser.wait4vac" wiederfinden.
	 *
	 * @param templateEngine Das Template-Engine, in dem die Variablen gesetzt werden sollen
	 * @param prefix Der fuer die Template-Variablen zu verwendende Prefix
	 */
	public void setTemplateVars(TemplateEngine templateEngine, String prefix) {
		String pre = prefix+".";
		templateEngine.setVar(
				pre+"id", this.id,
				pre+"un", this.un,
				pre+"name", this.name,
				pre+"passwort", this.passwort,
				pre+"email", this.email,
				pre+"log_fail", this.logFail,
				pre+"accesslevel", this.accesslevel,
				pre+"nickname", this.nickname,
				pre+"plainname", this.plainname,
				pre+"disabled", this.disabled);
	}

	/**
	 * Gibt das Zugriffslevel des Benutzers zurueck.
	 * @return Das Zugriffslevel
	 */
	public int getAccessLevel() {
		int acl = this.accesslevel;
		if( (attachedUser != null) && (attachedUser.getAccessLevel() > acl)) {
			return attachedUser.getAccessLevel();
		}
		return acl;
	}

	/**
	 * Gibt zurueck, ob der User ein Admin ist.
	 * @return <code>true</code>, wenn es ein Admin ist
	 */
	public boolean isAdmin() {
		if( getAccessLevel() >= 30 )
		{
			return true;
		}
		return attachedUser != null && attachedUser.isAdmin();
	}

	/**
	 * Gibt den Benutzernamen des Spielers zurueck. Der Benutzername
	 * wird lediglich zum einloggen verwendet und wird nicht angezeigt.
	 * @return Der Benutzername
	 */
	public String getUN() {
		return this.un;
	}

	/**
	 * Gibt den vollstaendigen Ingame-Namen des Spielers zurueck.
	 * Der vollstaendige Ingame-Name enthaelt den Ally-Tag sofern vorhanden
	 * und ist ggf auch mittels BBCode formatiert.
	 * @return Der vollstaendige Ingame-Name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Setzt den vollstaendigen Ingame-Namen des Spielers auf den angegebenen
	 * BBCode-String. Gleichzeitig wird das Feld <code>plainname</code> mit dem neuen
	 * Namen ohne BBCodes aktuallisiert.
	 *
	 * @param name der neue vollstaendige Ingame-Name
	 */
	public void setName( String name ) {
		if( !name.equals(this.name) ) {
			this.name = name;
			this.plainname = BBCodeParser.getInstance().parse(name,new String[] {"all"});
		}
	}

	/**
	 * Gibt das verschluesselte Passwort des Spielers zurueck.
	 * @return Das verschluesselte Passwort
	 */
	public String getPassword() {
		return this.passwort;
	}

	/**
	 * Setzt das Passwort fuer den Spieler.
	 * @param pw Das neue (mittels MD5 kodierte) Passwort
	 */
	public void setPassword( String pw ) {
		this.passwort = pw;
	}

	/**
	 * Gibt die Inaktivitaet des Spielers in Ticks zurueck.
	 * @return Die Inaktivitaet des Spielers in Ticks
	 */
	public int getInactivity() {
		return this.inakt;
	}

	/**
	 * Setzt die Inaktivitaet des Spielers in Ticks.
	 * @param inakt Die neue Inaktivitaet des Spielers
	 */
	public void setInactivity(int inakt) {
		this.inakt = inakt;
	}

	/**
	 * Gibt die Timestamp des Zeitpunkts zurueck, an dem Sich der Spieler
	 * angemeldet hat.
	 * @return Die Timestamp des Anmeldezeitpunkts
	 */
	public int getSignup() {
		return this.signup;
	}

	/**
	 * Gibt die Email-Adresse des Spielers zurueck.
	 * @return Die Email-Adresse
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * Gibt die Anzahl der fehlgeschlagenen Login-Versuche des Spielers zurueck.
	 * @return die Anzahl der fehlgeschlagenene Logins
	 */
	public int getLoginFailedCount() {
		return this.logFail;
	}

	/**
	 * Setzt die Anzahl der fehlgeschlagenen Logins des Spielers auf den angegebenen Wert.
	 * @param count Die neue Anzahl der fehlgeschlagenene Logins
	 */
	public void setLoginFailedCount(int count) {
		this.logFail = count;
	}

	/**
	 * Gibt den Ingame-Namen des Spielers ohne Ally-Tag zurueck.
	 * Der Name ist ggf mittels BBCodes formatiert.
	 * @return der Ingame-Name ohne Ally-Tag
	 */
	public String getNickname() {
		return this.nickname;
	}

	/**
	 * Setzt den Ingame-Namen ohne Ally-Tag des Spielers auf den angegebenen BBCode-String .
	 * @param nick der neue Ingame-Name ohne Ally-Tag
	 */
	public void setNickname( String nick ) {
		this.nickname = nick;
	}

	/**
	 * Gibt den unformatierten Ingame-Namen des Spielers zurueck.
	 * Der Name ist inklusive des Ally-Tags sofern vorhanden.
	 * @return Der unformatierte Name inkl. Ally-Tag
	 */
	public String getPlainname() {
		return this.plainname;
	}

	/**
	 * Gibt <code>true</code> zurueck, falls der Account deaktiviert ist.
	 * @return <code>true</code>, falls der Account deaktiviert ist
	 */
	public boolean getDisabled() {
		return this.disabled != 0;
	}

	/**
	 * Setzt die ID des Benutzers. Diese Methode funktioniert
	 * nur so lange, wie das Objekt nicht durch Hibernate persistiert wurde.
	 * Danach koennen beliebige seiteneffekte auftreten.
	 * @param id Die neue ID
	 */
	protected void setId(int id)
	{
		this.id = id;
	}

	/**
	 * (De)aktiviert den Account.
	 * @param value <code>true</code>, wenn der Account deaktiviert sein soll. Andernfalls <code>false</code>
	 */
	public void setDisabled(boolean value) {
		this.disabled = value ? (byte)1 : (byte)0;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * Setzt die Email-Adresse des Spielers.
	 * @param email Die Email
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Setzt den Plaintext-Namen des Spielers.
	 * @param plainname Der Name
	 */
	protected void setPlainname(String plainname) {
		this.plainname = plainname;
	}

	/**
	 * Setzt den Zeitpunkt, zu dem sich der User registriert hat
	 * in Sekunden seit dem 1.1.1970.
	 * @param signup Die Timestamp
	 */
	protected void setSignup(int signup) {
		this.signup = signup;
	}

	/**
	 * Setzt den Loginnamen des Users.
	 * @param un Der Loginname
	 */
	protected void setUn(String un) {
		this.un = un;
	}

	/**
	 * Setzt den Zugriffslevel den Users auf Adminfunktionen.
	 * @param accesslevel Der Level
	 */
	public void setAccesslevel(int accesslevel) {
		this.accesslevel = accesslevel;
	}

	@Override
	public String toString()
	{
		return "BasicUser [id: "+this.id+" un: "+this.un+"]";
	}
}
