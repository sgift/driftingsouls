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
package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.units.UnitType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Index;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Die Benutzerklasse von DS.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("default")
@BatchSize(size=50)
@org.hibernate.annotations.Table(
	appliesTo = "users",
	indexes = {@Index(name="vaccount", columnNames = {"vaccount", "wait4vac"})}
)
public class User extends BasicUser {
	private static final Log log = LogFactory.getLog(User.class);

    /**
	 * Die Arten von Beziehungen zwischen zwei Spielern.
	 * @author Christopher Jung
	 *
	 */
	public enum Relation {
		/**
		 * Neutral.
		 */
		NEUTRAL,	// 0
		/**
		 * Feindlich.
		 */
		ENEMY,		// 1
		/**
		 * Freundlich.
		 */
		FRIEND		// 2
	}

	private int race;
	@Lob
	private String history;
	private String medals;
	private int rang;
	private String ApiKey;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="ally", foreignKey = @ForeignKey(name="users_fk_ally"))
	private Ally ally;
	private BigInteger konto;
	private int npcpunkte;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="allyposten", unique = true, foreignKey = @ForeignKey(name="users_fk_ally_posten"))
	private AllyPosten allyposten;
	private int gtudropzone;
	private String npcorderloc;
	private short lostBattles;
	private short wonBattles;
	private int destroyedShips;
	private int lostShips;
	@Lob
	private String knownItems;
	private int vacpoints;
	private int specializationPoints;
	@Column(nullable = false)
    private BigInteger bounty;
	@Enumerated(EnumType.STRING)
	private PersonenNamenGenerator personenNamenGenerator;
	@Enumerated(EnumType.STRING)
	private SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator;
	@Enumerated(EnumType.STRING)
	private SchiffsNamenGenerator schiffsNamenGenerator;

	@OneToMany(mappedBy="userRankKey.owner")
    private Set<UserRank> userRanks;
    @OneToMany(mappedBy="user", cascade=CascadeType.ALL)
    private Set<Loyalitaetspunkte> loyalitaetspunkte;

	@ManyToMany
	@JoinTable(foreignKey = @ForeignKey(name="users_fk_forschungen"))
	private Set<Forschung> forschungen;

	@OneToMany(cascade = {CascadeType.DETACH,CascadeType.REFRESH})
	@JoinColumn(name="owner")
	// Explizit nur die Bases eines Users laden - sonst kommt Hibernate von Zeit zu Zeit auf die Idee die Bases von User 0 mitzuladen...
	@BatchSize(size=1)
	private Set<Base> bases;

	@OneToMany(cascade = {CascadeType.DETACH,CascadeType.REFRESH})
	@JoinColumn(name="owner")
	@BatchSize(size=1)
	private Set<Ship> ships;

	@Index(name = "vaccount", columnNames = {"vaccount", "wait4vac"})
	private int vaccount;
	private int wait4vac;

	@Lob
	@Column
	private String flags;

	@Transient
	private final Context context;

	/**
	 * Konstruktor.
	 *
	 */
	public User() {
		super();
		context = ContextMap.getContext();
	}

	/**
	 * Legt einen neuen Spieler an.
	 *
	 * @param name Loginname des Spielers.
	 * @param password Passwort - md5-verschluesselt.
	 * @param race Rasse des Spielers.
	 * @param history Bisherige Geschichte des Spielers.
	 * @param email E-Mailadresse des Spielers.
	 */
	public User(int newUserId, String name, String plainname, String password, int race, String history, String email, ConfigService configService) {
		super();
		context = ContextMap.getContext();
		setPassword(password);
		setName("Kolonist");
		setPlainname(plainname);
		this.race = race;
		this.history = history;
		setEmail(email);
		setUn(name);
		setFlag(UserFlag.NOOB);
		setSignup((int) Common.time());
		setInactivity(0);
		this.medals = "";
		this.rang = Byte.parseByte("0");
		this.konto = BigInteger.valueOf(0);
		setLoginFailedCount(0);
		setAccesslevel(0);
		this.npcpunkte = 0;
		setNickname("Kolonist");
		setPlainname("Kolonist");
		this.npcorderloc = "";
		setDisabled(false);
		this.vaccount = 0;
		this.wait4vac = 0;
		this.lostBattles = Short.parseShort("0");
		this.lostShips = 0;
		this.wonBattles = Short.parseShort("0");
		this.destroyedShips = 0;
		setId(newUserId);
		this.knownItems = "";
        bounty = BigInteger.ZERO;
		this.forschungen = new HashSet<>();
		this.specializationPoints = 15;
		this.loyalitaetspunkte = new HashSet<>();
		this.bases = new HashSet<>();
		this.ships = new HashSet<>();
		this.ApiKey = "";

		int defaultDropZone = configService.getValue(WellKnownConfigValue.GTU_DEFAULT_DROPZONE);
		setGtuDropZone(defaultDropZone);
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
	@Override
	public void setTemplateVars(TemplateEngine templateEngine, String prefix) {
		super.setTemplateVars(templateEngine, prefix);

		String pre = prefix+".";
		templateEngine.setVar(
				pre+"race", this.race,
				pre+"history", this.history,
				pre+"medals", this.medals,
				pre+"rang", this.rang,
				pre+"ally", this.ally != null ? this.ally.getId() : 0,
				pre+"konto", this.konto,
				pre+"npcpunkte", this.npcpunkte,
				pre+"allyposten", this.allyposten,
				pre+"gtudropzone", this.gtudropzone,
				pre+"npcorderloc", this.npcorderloc,
				pre+"lostBattles", this.lostBattles,
				pre+"wonBattles", this.wonBattles,
				pre+"destroyedShips", this.destroyedShips,
				pre+"lostShips", this.lostShips,
				pre+"knownItems", this.knownItems,
				pre+"vaccount", this.vaccount,
				pre+"wait4vac", this.wait4vac);
	}

	/**
	 * Gibt den vom Spieler verwendeten Generator fuer Personenanmen zurueck.
	 * @return Der Generator
	 */
	public PersonenNamenGenerator getPersonenNamenGenerator()
	{
		return this.personenNamenGenerator;
	}

	/**
	 * Setzt den vom Spieler verwendeten Generator fuer Personenanmen.
	 * @param personenNamenGenerator Der Generator
	 */
	public void setPersonenNamenGenerator(PersonenNamenGenerator personenNamenGenerator)
	{
		this.personenNamenGenerator = personenNamenGenerator;
	}

	/**
	 * Gibt den vom Spieler verwendeten Generator fuer Schiffsklassen-Prefixe zurueck.
	 * @return Der Generator
	 */
	public SchiffsKlassenNamenGenerator getSchiffsKlassenNamenGenerator()
	{
		return this.schiffsKlassenNamenGenerator;
	}

	/**
	 * Setzt den vom Spieler verwendeten Generator fuer Schiffsklassen-Prefixe.
	 * @param schiffsKlassenNamenGenerator Der Generator
	 */
	public void setSchiffsKlassenNamenGenerator(SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator)
	{
		this.schiffsKlassenNamenGenerator = schiffsKlassenNamenGenerator;
	}

	/**
	 * Gibt den vom Spieler verwendeten Generator fuer Schiffsnamen zurueck.
	 * @return Der Generator
	 */
	public SchiffsNamenGenerator getSchiffsNamenGenerator()
	{
		return this.schiffsNamenGenerator;
	}

	/**
	 * Setzt den vom Spieler verwendeten Generator fuer Schiffsnamen.
	 * @param schiffsNamenGenerator Der Generator
	 */
	public void setSchiffsNamenGenerator(SchiffsNamenGenerator schiffsNamenGenerator)
	{
		this.schiffsNamenGenerator = schiffsNamenGenerator;
	}

	/**
	 * Gibt alle Basen des Benutzers zurueck.
	 * @return Die Basen
	 */
	public Set<Base> getBases()
	{
		return this.bases;
	}

	/**
	 * Setzt die Basen des Benutzers.
	 * @param bases Die Basen
	 */
	public void setBases(Set<Base> bases)
	{
		this.bases = bases;
	}

	/**
	 * Gibt alle Schiffe des Benutzers zurueck.
	 * @return Die Schiffe
	 */
	public Set<Ship> getShips()
	{
		return this.ships;
	}

	/**
	 * Setzt die Schiffe des Benutzers.
	 * @param ships Die Schiffe
	 */
	public void setShips(Set<Ship> ships)
	{
		this.ships = ships;
	}

	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Benutzer zum aktuellen.
	 * Der Transfer kann entweder ein echter Transfer sein (Geld wird abgebucht) oder ein gefakter
	 * Transfer (kein Geld wird abgebucht sondern nur hinzugefuegt).
	 * Zudem faellt jeder Geldtransfer in eine von 3 Kategorien (automatisch, halbautomatisch und manuell).<br>
	 * Die Berechnung erfolgt intern auf Basis von <code>BigInteger</code>.
	 *
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 * @param faketransfer Handelt es sich um einen "gefakten" Geldtransfer (<code>true</code>)?
	 * @param transfertype Der Transfertyp (Kategorie)
	 * @see UserMoneyTransfer.Transfer
	 */
	public void transferMoneyFrom( int fromID, long count, String text, boolean faketransfer, UserMoneyTransfer.Transfer transfertype) {
		transferMoneyFrom(fromID,BigInteger.valueOf(count), text, faketransfer, transfertype);
	}

	/**
	 * Transferiert einen bestimmten RE-Betrag von einem Spieler zum aktuellen.
	 * Es wird KEIN Log geschrieben.
	 *
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll.
	 * @param count Die zu transferierende Geldmenge.
	 */
	public void transferMoneyFrom(int fromID, long count)
	{
		BigInteger biCount = BigInteger.valueOf(count);
		if(!biCount.equals(BigInteger.ZERO))
		{
			User fromUser = (User)context.getDB().get(User.class, fromID);
			if( (fromID != 0))
			{
				fromUser.setKonto(fromUser.getKonto().subtract(biCount));
			}

			konto = konto.add(biCount);
		}
	}

	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Benutzer zum aktuellen.
	 * Der Transfer kann entweder ein echter Transfer sein (Geld wird abgebucht) oder ein gefakter
	 * Transfer (kein Geld wird abgebucht sondern nur hinzugefuegt).
	 * Zudem faellt jeder Geldtransfer in eine von 3 Kategorien (automatisch, halbautomatisch und manuell).
	 *
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 * @param faketransfer Handelt es sich um einen "gefakten" Geldtransfer (<code>true</code>)?
	 * @param transfertype Der Transfertyp (Kategorie)
	 * @see UserMoneyTransfer.Transfer
	 */
	public void transferMoneyFrom( int fromID, BigInteger count, String text, boolean faketransfer, UserMoneyTransfer.Transfer transfertype) {
		org.hibernate.Session db = context.getDB();

		if( !count.equals(BigInteger.ZERO) ) {
			User fromUser = (User)context.getDB().get(User.class, fromID);
			if( (fromID != 0) && !faketransfer ) {
				fromUser.setKonto(fromUser.getKonto().subtract(count));
			}

			konto = konto.add(count);

			UserMoneyTransfer log = new UserMoneyTransfer(fromUser, this, count, text);
			log.setFake(faketransfer);
			log.setType(transfertype);
			db.persist(log);
		}
	}

	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Spieler zum aktuellen. Beim
	 * Transfer handelt es sich um einen manuellen Transfer.
	 *
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 * @param faketransfer Handelt es sich um einen "gefakten" Geldtransfer (<code>true</code>)?
	 * @see #transferMoneyFrom(int, long, String, boolean, UserMoneyTransfer.Transfer)
	 */
	private void transferMoneyFrom(int fromID, long count, String text, boolean faketransfer) {
		transferMoneyFrom( fromID, count, text, faketransfer, UserMoneyTransfer.Transfer.NORMAL );
	}

	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Spieler zum aktuellen. Beim
	 * Transfer handelt es sich um einen manuellen Transfer. Das Geld wird tatsaechlich dem Ausgangsspieler
	 * abgezogen (kein "gefakter" Transfer).
	 *
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 */
	public void transferMoneyFrom( int fromID, long count, String text ) {
		transferMoneyFrom( fromID, count, text, false );
	}

	/**
	 * Erhoeht das auf den Spieler ausgesetzte Kopfgeld um den genannten RE-Betrag.
	 * @param add Der Betrag
	 */
    public void addBounty(BigInteger add)
    {
        this.bounty = this.bounty.add(add);
    }

    /**
	 * Gibt das auf den Spieler ausgesetzte Kopfgeld in RE zurueck.
	 * @return Das Kopfgeld
	 */
    public BigInteger getBounty()
    {
        return this.bounty;
    }

	/**
	 * Gibt alle durch den Spieler erforschten Forschungen zurueck.
	 * @return Die Forschungen
	 */
	public Set<Forschung> getForschungen()
	{
		return new HashSet<>(this.forschungen);
	}

	/**
	 * Setzt alle durch den Spieler erforschten Forschungen.
	 * @param forschungen Die Forschungen
	 */
	public void setForschungen(Set<Forschung> forschungen)
	{
		Set<Forschung> aktuellErforscht = getForschungen();
		Set<Forschung> zuEntfernen = new HashSet<>(aktuellErforscht);
		zuEntfernen.removeAll(forschungen);

		Set<Forschung> hinzuzufuegen = new HashSet<>(forschungen);
		hinzuzufuegen.removeAll(aktuellErforscht);

		this.forschungen.addAll(hinzuzufuegen);

		this.forschungen.removeAll(zuEntfernen);
	}

	/**
	 * Prueft, ob alle angegebenen Forschungen durch den Spieler erforscht wurden.
	 * @param forschungen Die Forschungen
	 * @return <code>true</code>, falls alle Forschungen erforscht wurden
	 */
	public boolean hasResearched(Collection<Forschung> forschungen )
	{
		return forschungen.stream().allMatch(this::hasResearched);
	}

	/**
	 * Prueft, ob die angegebene Forschung durch den Benutzer erforscht wurde.
	 * Falls <code>null</code> uebergeben wird, wird <code>true</code> zurueckgegeben
	 * (Nichts ist immer erforscht).
	 *
	 * @param research Die zu pruefende Forschung
	 * @return <code>true</code>, falls die Forschung erforscht wurde
	 */
	public boolean hasResearched(Forschung research )
	{
		return research == null || this.forschungen.contains(research);
	}

	/**
	 * Fuegt eine Forschung zur Liste der durch den Benutzer erforschten Technologien hinzu,
	 * wenn er sie noch nicht hatte.
	 *
	 * @param research Die erforschte Technologie
	 */
	public void addResearch( Forschung research ) {
		if( !this.hasResearched(research) )
		{
			this.forschungen.add(research);
		}
	}

	/**
	 * Fuegt eine Zeile zur User-Historie hinzu.
	 * @param text Die hinzuzufuegende Zeile
	 */
	public void addHistory( String text ) {
		String history = getHistory();
		if( !"".equals(history) ) {
			this.history = history+"\n"+text;
		}
		else {
			this.history = text;
		}
	}

	/**
	 * Fuegt ein Item zur Liste der dem Spieler bekannten Items hinzu.
	 * Die Funktion prueft nicht, ob das Item allgemein bekannt ist,
	 * sondern geht davon aus, dass das angegebene Item allgemein unbekannt ist.
	 *
	 * @param itemid Die Item-ID
	 */
	public void addKnownItem( int itemid ) {
		if( !isKnownItem(itemid) ) {
			String itemlist = this.knownItems.trim();
			if( !itemlist.equals("") ) {
				itemlist += ","+itemid;
			}
			else {
				itemlist = ""+itemid;
			}

			this.knownItems = itemlist;
		}
	}

	/**
	 * Prueft, ob das Item mit der angegebenen ID dem Benutzer bekannt ist.
	 * Die Funktion prueft nicht, ob das Item allgemein bekannt ist,
	 * sondern geht davon aus, dass das angegebene Item allgemein unbekannt ist.
	 * @param itemid Die ID des Items
	 * @return <code>true</code>, falls das Item den Spieler bekannt ist
	 */
	public boolean isKnownItem( int itemid ) {
		String[] itemlist = StringUtils.split(this.knownItems,',');

		return Common.inArray(""+itemid,itemlist);
	}

	/**
	 * Gibt die ID der Rasse des Spielers zurueck.
	 * @return Die ID der Rasse
	 */
	public int getRace() {
		return this.race;
	}

	/**
	 * Gibt die Spielerhistorie als BBCode-formatierten String zurueck.
	 * @return Die Spielerhistorie
	 */
	public String getHistory() {
		return this.history;
	}

	/**
	 * Gibt die Liste aller Orden und Auszeichnungen des Spielers zurueck.
	 * Die einzelnen Orden-IDs sind mittels ; verbunden
	 * @return Die Liste aller Orden
	 */
	public String getMedals() {
		return this.medals;
	}

	/**
	 * Setzt die Liste der Orden des Spielers.
	 * @param medals Eine mittels ; separierte Liste von Orden
	 */
	public void setMedals( Set<Medal> medals ) {
		this.medals = medals.stream().map((m) -> Integer.toString(m.getId())).collect(Collectors.joining(";"));
	}

	/**
	 * Liefert den Rang des Benutzers zurueck.
	 * @return Der Rang
	 */
	public int getRang() {
		return this.rang;
	}

	/**
	 * Setzt den Rang des Benutzers.
	 * @param rang Die ID des Rangs
	 */
	public void setRang( int rang ) {
		this.rang = rang;
	}

	/**
	 * Liefert die Allianz des Benutzers zurueck.
	 *
	 * @return Die Allianz
	 */
	public Ally getAlly() {
		return this.ally;
	}

	/**
	 * Setzt die Allianz, der der Spieler angehoert.
	 * @param ally die neue Allianz
	 */
	public void setAlly( Ally ally ) {
		this.ally = ally;
	}

	/**
	 * Liefert den Kontostand des Benutzers zurueck.
	 * @return Der Kontostand
	 */
	public BigInteger getKonto() {
		return konto;
	}

	/**
	 * Setzt den Kontostand des Spielers auf den angegebenen Wert.
	 * @param count der neue Kontostand
	 */
	public void setKonto( BigInteger count ) {
		this.konto = count;
	}

	/**
	 * Liefert die Anzahl der NPC-Punkte des Benutzers zurueck.
	 * @return Die Anzahl der NPC-Punkte
	 */
	public int getNpcPunkte() {
		return this.npcpunkte;
	}

	/**
	 * Setzt die Anzahl der NPC-Punkte des Benutzers.
	 * @param punkte Die neue Anzahl der NPC-Punkte
	 */
	public void setNpcPunkte(int punkte) {
		this.npcpunkte = punkte;
	}

	/**
	 * Gibt den durch den Spieler besetzten Allianz-Posten zurueck.
	 * @return Der AllyPosten oder <code>null</code>
	 */
	public AllyPosten getAllyPosten() {
		return this.allyposten;
	}

	/**
	 * Setzt den durch den Spieler besetzten Allianz-Posten.
	 * @param posten Der Allianzposten
	 */
	public void setAllyPosten( AllyPosten posten ) {
		this.allyposten = posten;

		// TODO: Herausfinden warum Hibernate das nicht automatisch macht
		// wenn User.setAllyPosten(posten) aufgerufen wird
		if( (posten != null) && (posten.getUser() != this) ) {
			posten.setUser(this);
		}
	}

	/**
	 * Gibt die ID des Systems zurueck, in den die durch die GTU versteigerten Dinge erscheinen sollen.
	 * Das System muss ueber eine Drop-Zone verfuegen.
	 *
	 * @return Die ID des Systems in den die versteigerten Dinge auftauchen sollen
	 */
	public int getGtuDropZone() {
		return this.gtudropzone;
	}

	/**
	 * Setzt die ID des von der GTU verwendeten Dropzone-Systems des Spielers.
	 * @param system Die ID des neuen Systems mit der bevorzugten GTU-Dropzone
	 */
	public void setGtuDropZone( int system ) {
		this.gtudropzone = system;
	}

	/**
	 * Gibt die Koordinate des Ortes zurueck, an dem von NPCs georderte Dinge erscheinen sollen.
	 *
	 * @return Die Koordinaten des Ortes, an dem georderte Dinge erscheinen sollen
	 */
	public String getNpcOrderLocation() {
		return this.npcorderloc;
	}

	/**
	 * Setzt die Koordinaten des Ortes, an dem von NPCs georderte Dinge erscheinen sollen.
	 * @param loc Die Koordinaten des Ortes, an dem georderte Dinge erscheinen sollen
	 */
	public void setNpcOrderLocation( String loc ) {
		this.npcorderloc = loc;
	}

	/**
	 * Gibt die Anzahl der gewonnenen Schlachten zurueck.
	 * @return die Anzahl der gewonnenen Schlachten
	 */
	public short getWonBattles() {
		return this.wonBattles;
	}

	/**
	 * Setzt die Anzahl der gewonnenen Schlachten.
	 * @param battles Die Anzahl
	 */
	public void setWonBattles(short battles) {
		this.wonBattles = battles;
	}

	/**
	 * Gibt die Anzahl der verlorenen Schlachten zurueck.
	 * @return die Anzahl der verlorenen Schlachten
	 */
	public short getLostBattles() {
		return this.lostBattles;
	}

	/**
	 * Setzt die Anzahl der verlorenen Schlachten.
	 * @param battles Die Anzahl
	 */
	public void setLostBattles(short battles) {
		this.lostBattles = battles;
	}

	/**
	 * Gibt die Anzahl der verlorenen Schiffe zurueck.
	 * @return die Anzahl der verlorenen Schiffe
	 */
	public int getLostShips() {
		return this.lostShips;
	}

	/**
	 * Setzt die Anzahl der verlorenen Schiffe.
	 * @param lost Die Anzahl
	 */
	public void setLostShips(int lost) {
		this.lostShips = lost;
	}

	/**
	 * Gibt die Anzahl der zerstoerten Schiffe zurueck.
	 * @return die Anzahl der zerstoerten Schiffe
	 */
	public int getDestroyedShips() {
		return this.destroyedShips;
	}

	/**
	 * Setzt die Anzahl der zerstoerten Schiffe.
	 * @param ships die neue Anzahl
	 */
	public void setDestroyedShips(int ships) {
		this.destroyedShips = ships;
	}

	/**
	 * Gibt die Liste der bekannten Items zurueck, welche per Default
	 * unbekannt ist.
	 * @return Die Liste der bekannten Items als Item-String
	 */
	public String getKnownItems() {
		return this.knownItems;
	}

	/**
	 * Gibt die Anzahl der Ticks zurueck, die der Account noch im
	 * Vacation-Modus ist. Der Account kann sich auch noch im Vorlauf befinden!
	 * @return Die Anzahl der verbleibenden Vac-Ticks
	 */
	public int getVacationCount() {
		return this.vaccount;
	}

	/**
	 * Setzt die Anzahl der Ticks, die der Account im Vacation-Modus verbringen soll.
	 * @param value Die Anzahl der Ticks im Vacation-Modus
	 */
	public void setVacationCount(int value) {
		this.vaccount = value;
	}

	/**
	 * Gibt zurueck, wieviele Ticks sich der Account noch im Vorlauf fuer den
	 * Vacation-Modus befindet.
	 * @return Die Anzahl der verbleibenden Ticks im Vacation-Vorlauf
	 */
	public int getWait4VacationCount() {
		return this.wait4vac;
	}

	/**
	 * Setzt die Anzahl der Ticks des Vacation-Modus-Vorlaufs auf den angegebenen
	 * Wert.
	 * @param value Die Anzahl der Ticks im Vacation-Modus-Vorlauf
	 */
	public void setWait4VacationCount(int value) {
		this.wait4vac = value;
	}

    /**
     * Gibt an, ob der Spieler ein NPC ist.
     *
     * @return <code>true</code> fuer NPCs.
     */
    public boolean isNPC()
    {
        return getId() < 0;
    }

	/**
	 * Setzt die Rasse eines Users.
	 *
	 * @param race Rassenid
	 */
	public void setRace(int race)
	{
		this.race = race;
	}

	/**
	 * Setzt die Geschichte des Users.
	 *
	 * @param history Die neue Geschichte.
	 */
	public void setHistory(String history)
	{
		this.history = history;
	}

	/**
	 * Gibt die Anzahl der Vacationpunkte zurueck.
	 * @return Die Anzahl
	 */
	public int getVacpoints()
	{
		return vacpoints;
	}

	/**
	 * Setzt die Anzahl der Vacationpunkte.
	 * @param vacpoints Die Anzahl
	 */
	public void setVacpoints(int vacpoints)
	{
		this.vacpoints = vacpoints;
	}

	/**
	 * @return Ticks, die der User maximal im Urlaub sein darf.
	 */
	public int maxVacTicks()
	{
		return getVacpoints() / vacationCostsPerTick();
	}

	/**
	 * Prueft, ob genug Punkte fuer die Urlaubsanfrage vorhanden sind.
	 *
	 * @param ticks Ticks, die der Spieler in den Urlaub gehen will.
	 * @return <code>true</code>, wenn genug Punkte vorhanden sind, sonst <code>false</code>
	 */
	public boolean checkVacationRequest(int ticks)
	{

		int costs = ticks * vacationCostsPerTick();

		return costs <= getVacpoints();

	}

	/**
	 * Aktiviert den Urlaubsmodus.
	 *
	 * @param ticks Ticks, die der Spieler in den Urlaub gehen soll.
	 */
	public void activateVacation(int ticks)
	{
		setVacationCount(ticks);
		setWait4VacationCount(getVacationPrerun(ticks));
	}

	/**
	 * Gibt zurueck, ob sich der User im Vacationmodus befindet.
	 * @return <code>true</code>, falls er sich im Vacationmodus befindet.
	 */
	public boolean isInVacation()
	{
		return (getVacationCount() > 0) && (getWait4VacationCount() == 0);
	}

	/**
	 * @param ticks Anzahl der Urlaubsticks.
	 * @return Prerun Vorlaufzeit in ticks.
	 */
	private int getVacationPrerun(int ticks)
	{
		return Math.max(4,ticks / Common.TICKS_PER_DAY);
	}

	/**
	 * @return Punktekosten fuer einen Urlaubstick.
	 */
	private int vacationCostsPerTick()
	{
		return new ConfigService().getValue(WellKnownConfigValue.VAC_POINTS_PER_VAC_TICK);
	}

	/**
	 * checks if the user is able to see the item.
	 * @param aitem a specific item
	 * @return boolean, true if user  is able to see the item
	 */
	public boolean canSeeItem(Item aitem) {
		return aitem.getAccessLevel() <= this.getAccessLevel() &&
				(!aitem.isUnknownItem() || this.isKnownItem(aitem.getID()) || ContextMap.getContext().hasPermission(WellKnownPermission.ITEM_UNBEKANNTE_SICHTBAR));

	}

	/**
	 * returns a Set of all systems the user has a colony in.
	 * @return the set of all systems the user has a colony in.
	 */
	public Set<Integer> getAstiSystems()
	{
		Set<Integer> systemlist = new HashSet<>();
		for(Base base: this.bases)
		{
			int basesystem = base.getSystem();
			systemlist.add(basesystem);
		}
		return systemlist;
	}

	/**
	 * @return Die Spezialisierungspunkte des Nutzers.
	 */
	public int getSpecializationPoints()
	{
		return this.specializationPoints;
	}

	/**
	 * @param specializationPoints Die Spezialisierungspunkte des Nutzers.
	 */
	public void setSpecializationPoints(int specializationPoints)
	{
		this.specializationPoints = specializationPoints;
	}

	/**
	 * @return Die Spezialisierungspunkte, die noch nicht von Forschungen belegt sind.
	 */
	public long getFreeSpecializationPoints()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		int usedSpecpoints =  this.forschungen.stream().mapToInt(Forschung::getSpecializationCosts).sum();

		//Add researchs, which are currently developed in research centers
		List<Forschungszentrum> researchcenters = Common.cast(db
				.createQuery("from Forschungszentrum where forschung is not null and base.owner=:owner")
				.setEntity("owner", this)
				.list());
		for(Forschungszentrum researchcenter: researchcenters)
		{
			usedSpecpoints += researchcenter.getForschung().getSpecializationCosts();
		}

		return getSpecializationPoints() - usedSpecpoints;
	}

	/**
	 * Verlernt eine Forschung und alle davon abhaengigen Forschungen.
	 *
	 * @param research Die Forschung, die der Spieler fallen lassen will.
	 */
	public void dropResearch(Forschung research)
	{
		if(!this.forschungen.contains(research))
		{
			return;
		}

		//Drop dependent researchs
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<Forschung> dependentResearchs = Common.cast(db.createQuery("from Forschung where req1= :fid or req2= :fid or req3= :fid")
									  			  .setInteger("fid", research.getID())
									  			  .list());

		dependentResearchs.forEach(this::dropResearch);
		this.forschungen.remove(research);
	}

	/**
	 * Gibt zurueck, ob der Einheitentyp dem User bekannt ist.
	 * @param unitType Die ID des Einheitentyps
	 * @return <code>true</code>, falls die Einheit dem User bekannt ist, sonst <code>false</code>
	 */
	public boolean isKnownUnit(UnitType unitType) {
		if( !unitType.isHidden() || ContextMap.getContext().hasPermission(WellKnownPermission.UNIT_VERSTECKTE_SICHTBAR) )
		{
			return true;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		long baseunit;
		long shipunit = 0;

		Object baseunitsuserobject = db.createQuery("select sum(e.amount) " +
				"from BaseUnitCargoEntry as e " +
				"where e.unittype=:unittype and e.basis.owner=:user")
				.setInteger("unittype", unitType.getId())
				.setEntity("user", this)
				.iterate()
				.next();
		if( baseunitsuserobject != null)
		{
			baseunit = (Long)baseunitsuserobject;
			if( baseunit > 0 )
			{
				return true;
			}
		}

		Object shipunitsuserobject = db.createQuery("select sum(e.amount) " +
				"from ShipUnitCargoEntry as e " +
				"where e.unittype=:unittype and e.schiff.owner=:user")
				.setInteger("unittype", unitType.getId())
				.setEntity("user", this)
				.iterate()
				.next();

		if( shipunitsuserobject != null)
		{
			shipunit = (Long)shipunitsuserobject;
		}

		return shipunit > 0;
	}

	/**
	 * Aktualisiert den Rang des Spielers bei einem Ranggeber.
	 *
	 * @param rankGiver
	 *            Jemand der Raenge vergeben kann.
	 * @param rank
	 *            Der neue Rang
	 */
	public void setRank(User rankGiver, int rank)
	{
		for (UserRank rang : this.userRanks)
		{
			if (rang.getRankGiver().getId() == rankGiver.getId())
			{
				if( rank == 0 )
				{
					this.userRanks.remove(rang);
					org.hibernate.Session db = ContextMap.getContext().getDB();
					db.delete(rang);
					return;
				}

				rang.setRank(rank);
				return;
			}
		}

		UserRank.UserRankKey key = new UserRank.UserRankKey(this, rankGiver);

		UserRank userRank = new UserRank(key, rank);
		this.userRanks.add(userRank);

		org.hibernate.Session db = ContextMap.getContext().getDB();
		db.persist(userRank);
	}

	/**
	 * Gibt den Rang eines Benutzers bei einem bestimmten NPC zurueck.
	 * Falls kein Rang vorhanden ist wird der niederigste moegliche Rang
	 * als nicht persistiertes Objekt zurueckgegeben.
	 * @param rankGiver Der NPC
	 * @return Der Rang
	 */
	public UserRank getRank(User rankGiver)
	{
		for (UserRank rang : this.userRanks)
		{
			if (rang.getRankGiver().getId() == rankGiver.getId())
			{
				return rang;
			}
		}
		UserRank.UserRankKey key = new UserRank.UserRankKey(this, rankGiver);

		return new UserRank(key, 0); // Working with null is inconvenient for external classes
	}

	/**
	 * Gibt alle NPC-spezifischen Raenge des Benutzers zurueck.
	 * @return Die Raenge
	 */
	public Set<UserRank> getOwnRanks()
	{
		return this.userRanks;
	}

	/**
	 * Gibt alle an den Nutzer vergebenen Loyalitaetspunkte zurueck.
	 * @return Die Liste
	 */
	public Set<Loyalitaetspunkte> getLoyalitaetspunkte()
	{
		return this.loyalitaetspunkte;
	}

	/**
	 * Gibt die Gesamtanzahl der Loyalitaetspunkte bei einem bestimmten NPC zurueck.
	 * @return Die Gesamtanzahl
	 */
	public int getLoyalitaetspunkteTotalBeiNpc(User npc)
	{
		int total = 0;
		for( Loyalitaetspunkte lp : this.loyalitaetspunkte )
		{
			if( lp.getVerliehenDurch().getId() == npc.getId() )
			{
				total += lp.getAnzahlPunkte();
			}
		}
		return total;
	}

	/**
	 * Ueberprueft, ob ein Flag fuer den Benutzer aktiv ist.
	 * @param flag Das zu ueberpruefende Flag
	 * @return <code>true</code>, falls das Flag aktiv ist
	 */
	public boolean hasFlag( UserFlag flag ) {
		if(this.attachedUser instanceof User)
		{
			if( ((User)this.attachedUser).hasFlag(flag) )
			{
				return true;
			}
		}
		return flags.contains(flag.getFlag());

	}

	/**
	 * Setzt ein Flag fuer den User entweder auf aktiviert (<code>true</code>)
	 * oder auf deaktiviert (<code>false</code>).
	 * @param flag Das zu setzende Flag
	 * @param on true, falls es aktiviert werden soll
	 */
	public void setFlag( UserFlag flag, boolean on ) {
		String flagstring;
		if( on ) {
			if( !"".equals(flags) && flags != null ) { // NULL muss extra abgefangen werden, sonst wird beim erzeugen eines Spielers 'null' hinzugefuegt
				flagstring = flags+" "+flag.getFlag();
			}
			else {
				flagstring = flag.getFlag();
			}
		}
		else {
			StringBuilder newflags = new StringBuilder();

			String[] flags = StringUtils.split(this.flags,' ');
			for( String aflag : flags ) {
				if( !aflag.equals(flag.getFlag()) ) {
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
	public void setFlag( UserFlag flag ) {
		setFlag( flag, true );
	}


	/**
	 * Gibt die Flags des Benutzers zurueck.
	 * @return Die Flags
	 */
	public Set<UserFlag> getFlags()
	{
		return UserFlag.parseFlags(flags);
	}

	/**
	 * Setzt die Flags des Benutzers.
	 * @param flags Die Flags
	 */
	public void setFlags(Set<UserFlag> flags)
	{
		this.flags = flags.stream().map(UserFlag::getFlag).collect(Collectors.joining(" "));
	}
}
