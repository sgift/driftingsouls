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

import java.util.HashSet;
import java.util.Set;

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

import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DiscriminatorFormula;


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
	private static final Log log = LogFactory.getLog(BasicUser.class);

	private static String defaultImagePath = null;

	@Id
	private int id;

	private String un;
	private String name;
	private String passwort;
	private int inakt;
	private int signup;
	private String email;
	@Column(name="log_fail")
	private int logFail;
	private int accesslevel;
	private String nickname;
	private String plainname;
	private String imgpath;
	private byte disabled;
	private String flags;
	@OneToMany(mappedBy="user", cascade=CascadeType.ALL)
	private Set<Permission> permissions;

	@Version
	private int version;

	@Transient
	private boolean forceDefaultImgPath = false;
	@Transient
	private Context context;
	@Transient
	private BasicUser attachedUser;

	/**
	 * Konstruktor.
	 *
	 */
	public BasicUser() {
		context = ContextMap.getContext();
		attachedUser = null;
		this.permissions = new HashSet<Permission>();
	}

	/**
	 * Fuegt dem Benutzer weitere Sessiondaten hinzu.
	 * @param useGfxPak <code>true</code>, falls ein Grafikpak genutzt werden soll
	 */
	public void setSessionData(boolean useGfxPak) {
		if( !useGfxPak ) {
			forceDefaultImgPath = true;
		}
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
				pre+"imgpath", this.imgpath,
				pre+"disabled", this.disabled,
				pre+"flags", this.flags);
	}

	/**
	 * Liefert den Standard-Image-Path zurueck.
	 * @return Der Standard-Image-Path
	 */
	public static String getDefaultImagePath() {
		if( defaultImagePath == null ) {
			defaultImagePath = Configuration.getSetting("IMAGE_URL");
			if( defaultImagePath == null ) {
				defaultImagePath = Configuration.getSetting("URL");
			}
		}
		return defaultImagePath;
	}

	/**
	 * Liefert den Image-Path dieses Benutzers zurueck.
	 *
	 * @return Der Image-Path des Benutzers
	 */
	public String getUserImagePath() {
		return imgpath;
	}

	/**
	 * Ueberprueft, ob ein Flag fuer den Benutzer aktiv ist.
	 * @param flag Das zu ueberpruefende Flag
	 * @return <code>true</code>, falls das Flag aktiv ist
	 */
	public boolean hasFlag( String flag ) {
		if( flags.indexOf(flag) > -1 ) {
			return true;
		}

		if( (attachedUser != null) && attachedUser.hasFlag(flag) ) {
			return true;
		}

		return false;
	}

	/**
	 * Setzt ein Flag fuer den User entweder auf aktiviert (<code>true</code>)
	 * oder auf deaktiviert (<code>false</code>).
	 * @param flag Das zu setzende Flag
	 * @param on true, falls es aktiviert werden soll
	 */
	public void setFlag( String flag, boolean on ) {
		String flagstring = "";
		if( on ) {
			if( !"".equals(flags) ) {
				flagstring = flags+" "+flag;
			}
			else {
				flagstring = flag;
			}
		}
		else {
			StringBuilder newflags = new StringBuilder();

			String[] flags = StringUtils.split(this.flags,' ');
			for( String aflag : flags ) {
				if( !aflag.equals(flag) ) {
					if( newflags.length() > 0 ) {
						newflags.append(" ");
					}
					newflags.append(aflag);
				}
			}
			flagstring = newflags.toString();
		}

		this.flags = flagstring;
	}

	/**
	 * Aktiviert ein Flag fuer den User.
	 * @param flag Das zu aktivierende Flag
	 */
	public void setFlag( String flag ) {
		setFlag( flag, true );
	}

	/**
	 * Liefert den Wert eines User-Values zurueck.
	 * User-Values sind die Eintraege, welche sich in der Tabelle user_values befinden.
	 *
	 * @param valuename Name des User-Values
	 * @return Wert des User-Values
	 */
	public String getUserValue( String valuename ) {
		UserValue value = (UserValue)context.getDB()
			.createQuery("from UserValue where user in (?,0) and name=? order by abs(user) desc")
			.setInteger(0, this.id)
			.setString(1, valuename)
			.setMaxResults(1)
			.uniqueResult();

		if( value == null ) {
			log.warn("Uservalue "+valuename+" hat keinen Defaultwert");
			return "";
		}
		return value.getValue();
	}

	/**
	 * Setzt ein User-Value auf einen bestimmten Wert.
	 * @see #getUserValue(String)
	 *
	 * @param valuename Name des User-Values
	 * @param newvalue neuer Wert des User-Values
	 */
	public void setUserValue( String valuename, String newvalue ) {
		UserValue valuen = (UserValue)context.getDB().createQuery("from UserValue where user=? and name=?")
			.setInteger(0, this.id)
			.setString(1, valuename)
			.uniqueResult();

		// Existiert noch kein Eintag?
		if( valuen == null ) {
			valuen = new UserValue(this, valuename, newvalue);
			context.getDB().persist(valuen);
		}
		else {
			valuen.setValue(newvalue);
		}
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
		if( attachedUser != null)
		{
			return attachedUser.isAdmin();
		}
		return false;
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
			this.plainname = Common._titleNoFormat(name);
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
	 * Gibt den Image-Pfad des Spielers zurueck.
	 * @return Der Image-Pfad des Spielers
	 */
	public String getImagePath() {
		if( !this.forceDefaultImgPath && this.imgpath != null ) {
			return this.imgpath;
		}

		return getDefaultImagePath();
	}

	/**
	 * Setzt den Image-Pfad des Spielers auf den angegebenen Wert.
	 * @param value Der neue Image-Pfad des Spielers
	 */
	public void setImagePath(String value) {
		this.imgpath = value;
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

	/**
	 * Gibt die Flags des Benutzers zurueck.
	 * @return Die Flags
	 */
	public String getFlags()
	{
		return flags;
	}

	/**
	 * Setzt die Flags des Benutzers.
	 * @param flags Die Flags
	 */
	public void setFlags(String flags)
	{
		this.flags = flags;
	}

	@Override
	public String toString()
	{
		return "BasicUser [id: "+this.id+" un: "+this.un+"]";
	}
}
