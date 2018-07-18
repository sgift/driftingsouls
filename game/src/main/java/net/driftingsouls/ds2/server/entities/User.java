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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator;
import net.driftingsouls.ds2.server.ships.SchiffsReKosten;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.units.UnitType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Die Benutzerklasse von DS.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("default")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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

	/**
	 * Klasse, welche die Beziehungen eines Spielers zu anderen
	 * Spielern enthaelt.
	 * @author Christopher Jung
	 *
	 */
	public static class Relations {
		/**
		 * Die Beziehungen des Spielers zu anderen Spielern.
		 * Schluessel ist die Spieler-ID
		 */
		protected Map<Integer,Relation> toOther = new HashMap<>();
		/**
		 * Die Beziehungen von anderen Spielern zum Spieler selbst.
		 * Schluessel ist die Spieler-ID.
		 */
		protected Map<Integer,Relation> fromOther = new HashMap<>();

		private User user;

		protected Relations(User user) {
			this.user = user;
		}

		/**
		 * Gibt zurueck, ob die Beziehung zu einem gegebenen anderen Spieler
		 * in beide Richtungen den angegebenen Beziehungtyp hat.
		 * @param otherUser Der andere Spieler
		 * @param relation Der Beziehungstyp
		 * @return <code>true</code>, falls in beide Richtungen der Beziehungstyp gilt
		 */
		public boolean isOnly(User otherUser, Relation relation)
		{
			return beziehungZu(otherUser) == relation && beziehungVon(otherUser) == relation;
		}

		/**
		 * Gibt die Beziehung des Spielers zu einem anderen Spieler zurueck.
		 * @param otherUser Der andere Spieler
		 * @return Der Beziehungstyp
		 */
		public Relation beziehungZu(User otherUser)
		{
			if( user.getAlly() != null && user.getAlly().getMembers().contains(otherUser) )
			{
				// Allianzen sind immer befreundet
				return Relation.FRIEND;
			}

			Relation relation = this.toOther.get(otherUser.getId());
			if( relation != null )
			{
				return relation;
			}
			relation = this.toOther.get(0);
			if( relation != null )
			{
				return relation;
			}
			// Keine Default-Beziehung definiert -> Neutral
			return Relation.NEUTRAL;
		}

		/**
		 * Gibt die Beziehung eines anderen Spielers zu diesem Spieler zurueck.
		 * @param otherUser Der andere Spieler
		 * @return Der Beziehungstyp
		 */
		public Relation beziehungVon(User otherUser)
		{
			if( user.getAlly() != null && user.getAlly().getMembers().contains(otherUser) )
			{
				// Allianzen sind immer befreundet
				return Relation.FRIEND;
			}

			Relation relation = this.fromOther.get(otherUser.getId());
			if( relation != null )
			{
				return relation;
			}
			// Keine Default-Beziehung definiert -> Neutral
			return Relation.NEUTRAL;
		}
	}

	private int race;
	@Lob
	private String history;
	private String medals;
	private int rang;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="ally", nullable=true)
	@ForeignKey(name="users_fk_ally")
	private Ally ally;
	private BigInteger konto;
	private int npcpunkte;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="allyposten", nullable=true, unique = true)
	@ForeignKey(name="users_fk_ally_posten")
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
	@JoinTable
	@ForeignKey(name="users_fk_forschungen", inverseName = "users_forschungen_fk_users")
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

	private int vaccount;
	private int wait4vac;

	@Lob
	@Column
	private String flags;

	@Transient
	private Context context;

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
	 * @param cargo Ressourcen im Spielercargo.
	 * @param email E-Mailadresse des Spielers.
	 */
	public User(String name, String password, int race, String history, Cargo cargo, String email) {
		super();
		context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		setPassword(password);
		setName("Kolonist");
		this.race = race;
		this.history = history;
		setEmail(email);
		setUn(name);
		setFlag(UserFlag.NOOB);
		setSignup((int) Common.time());
		setInactivity(0);
		this.medals = "";
		this.rang = (int) Byte.valueOf("0");
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
		this.lostBattles = Short.valueOf("0");
		this.lostShips = 0;
		this.wonBattles = Short.valueOf("0");
		this.destroyedShips = 0;
		Integer newUserId = (Integer)db.createQuery("SELECT max(id) from User").uniqueResult();
		setId(newUserId != null ? ++newUserId : 1);
		this.knownItems = "";
        bounty = BigInteger.ZERO;
		db.persist(this);
		Ordner trash = Ordner.createNewOrdner("Papierkorb", Ordner.getOrdnerByID(0, this), this);
		trash.setFlags(Ordner.FLAG_TRASH);
		this.forschungen = new HashSet<>();
		this.specializationPoints = 15;
		this.loyalitaetspunkte = new HashSet<>();
		this.bases = new HashSet<>();
		this.ships = new HashSet<>();

		int defaultDropZone = new ConfigService().getValue(WellKnownConfigValue.GTU_DEFAULT_DROPZONE);
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
		return this.personenNamenGenerator == null ? Rassen.get().rasse(this.race).getPersonenNamenGenerator() : this.personenNamenGenerator;
	}

	/**
	 * Setzt den vom Spieler verwendeten Generator fuer Personenanmen.
	 * @param personenNamenGenerator Der Generator
	 */
	public void setPersonenNamenGenerator(PersonenNamenGenerator personenNamenGenerator)
	{
		this.personenNamenGenerator = Rassen.get().rasse(this.race).getPersonenNamenGenerator() == personenNamenGenerator ? null : personenNamenGenerator;
	}

	/**
	 * Gibt den vom Spieler verwendeten Generator fuer Schiffsklassen-Prefixe zurueck.
	 * @return Der Generator
	 */
	public SchiffsKlassenNamenGenerator getSchiffsKlassenNamenGenerator()
	{
		return this.schiffsKlassenNamenGenerator == null ? Rassen.get().rasse(this.race).getSchiffsKlassenNamenGenerator() : this.schiffsKlassenNamenGenerator;
	}

	/**
	 * Setzt den vom Spieler verwendeten Generator fuer Schiffsklassen-Prefixe.
	 * @param schiffsKlassenNamenGenerator Der Generator
	 */
	public void setSchiffsKlassenNamenGenerator(SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator)
	{
		this.schiffsKlassenNamenGenerator = Rassen.get().rasse(this.race).getSchiffsKlassenNamenGenerator() == schiffsKlassenNamenGenerator ? null : schiffsKlassenNamenGenerator;
	}

	/**
	 * Gibt den vom Spieler verwendeten Generator fuer Schiffsnamen zurueck.
	 * @return Der Generator
	 */
	public SchiffsNamenGenerator getSchiffsNamenGenerator()
	{
		return this.schiffsNamenGenerator == null ? Rassen.get().rasse(this.race).getSchiffsNamenGenerator() : this.schiffsNamenGenerator;
	}

	/**
	 * Setzt den vom Spieler verwendeten Generator fuer Schiffsnamen.
	 * @param schiffsNamenGenerator Der Generator
	 */
	public void setSchiffsNamenGenerator(SchiffsNamenGenerator schiffsNamenGenerator)
	{
		this.schiffsNamenGenerator = Rassen.get().rasse(this.race).getSchiffsNamenGenerator() == schiffsNamenGenerator ? null : schiffsNamenGenerator;
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
	 * Liefert einen Profile-Link zu den Benutzer zurueck (als HTML).
	 * Als CSS-Klasse fuer den Link wird die angegebene Klasse verwendet.
	 * @param username Der anzuzeigende Spielername
	 * @return Der Profile-Link
	 */
	public String getProfileLink(String username) {
		if( username == null || username.equals("") ) {
			username = Common._title(this.getName());
		}

		return "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", this.getId())+"\">"+username+"</a>";
	}

	/**
	 * Liefert einen vollstaendigen Profile-Link zu den Benutzer zurueck (als HTML).
	 * Der Linkt enthaelt einen &lt;a&gt;-Tag sowie den Benutzernamen als HTML.
	 * @return Der Profile-Link
	 */
	public String getProfileLink() {
		return getProfileLink("");
	}

	@Transient
	private Relations relations = null;

	/**
	 * Liefert alle Beziehungen vom Spieler selbst zu anderen Spielern und umgekehrt.
	 *
	 * @return Gibt ein Array zurueck.
	 * 	Position 0 enthaelt alle Beziehungen von einem selbst ($userid => $beziehung).
	 * 	Position 1 enthaelt alle Beziehungen zu einem selbst ($userid => $beziehung).
	 *
	 * 	Beziehungen zu Spieler 0 betreffen grundsaetzlich alle Spieler ohne eigene Regelung
	 */
	public Relations getRelations() {
		if( this.relations == null ) {
			Relations relations = new Relations(this);

			org.hibernate.Session db = context.getDB();

			Map<User,Relation> defaults = new HashMap<>();

			List<?> relationlist = db.createQuery("from UserRelation " +
					"where user= :user OR target= :user OR (user!= :user AND target.id=0) " +
					"order by abs(target.id) desc")
				.setEntity("user", this)
				.list();

			for (Object aRelationlist : relationlist)
			{
				UserRelation relation = (UserRelation) aRelationlist;
				if (relation.getUser().getId() == this.getId())
				{
					relations.toOther.put(relation.getTarget().getId(), Relation.values()[relation.getStatus()]);
				}
				else if( relation.getTarget().getId() == 0 )
				{
					defaults.put(relation.getUser(), Relation.values()[relation.getStatus()]);
				}
				else
				{
					relations.fromOther.put(relation.getUser().getId(), Relation.values()[relation.getStatus()]);
				}
			}

			for (Map.Entry<User, Relation> userRelationEntry : defaults.entrySet())
			{
				if( relations.fromOther.containsKey(userRelationEntry.getKey().getId()) )
				{
					continue;
				}
				relations.fromOther.put(userRelationEntry.getKey().getId(), userRelationEntry.getValue());
			}


			if( !relations.toOther.containsKey(0) ) {
				relations.toOther.put(0, Relation.NEUTRAL);
			}

			relations.toOther.put(this.getId(), Relation.FRIEND);
			relations.fromOther.put(this.getId(), Relation.FRIEND);

			this.relations = relations;
		}

		Relations rel = new Relations(this);
		rel.fromOther.putAll(relations.fromOther);
		rel.toOther.putAll(relations.toOther);
		return rel;
	}

	/**
	 * Gibt den Status der Beziehung des Spielers zu einem anderen Spieler zurueck.
	 * @param user Der andere Spieler oder <code>null</code>, falls die Standardbeziehung abgefragt werden soll
	 * @return Der Status der Beziehungen zu dem anderen Spieler
	 */
	public Relation getRelation(User user)
	{
		if( user == this )
		{
			return Relation.FRIEND;
		}

		if( user == null ) {
			user = (User)context.getDB().get(User.class, 0);
		}

		if( this.ally != null && this.ally.getMembers().contains(user) )
		{
			return Relation.FRIEND;
		}

		Relation rel = Relation.NEUTRAL;

		if( relations == null ) {
			UserRelation currelation = (UserRelation)context.getDB()
				.createQuery("from UserRelation WHERE user=:user AND target=:userid")
				.setInteger("user", this.getId())
				.setInteger("userid", user.getId())
				.uniqueResult();

			if( currelation == null ) {
				currelation = (UserRelation)context.getDB()
					.createQuery("from UserRelation WHERE user=:user AND target.id=0")
					.setInteger("user", this.getId())
					.uniqueResult();
			}

			if( currelation != null ) {
				rel = Relation.values()[currelation.getStatus()];
			}
		}
		else {
			if( relations.toOther.containsKey(user.getId()) ) {
				rel = relations.toOther.get(user.getId());
			}
		}
		return rel;
	}

	/**
	 * Setzt die Beziehungen des Spielers zu einem anderen Spieler auf den angegebenen
	 * Wert.
	 * @param userid Die ID des anderen Spielers
	 * @param relation Der neue Status der Beziehungen
	 */
	public void setRelation( int userid, Relation relation ) {
		org.hibernate.Session db = context.getDB();

		if( userid == this.getId() ) {
			return;
		}

		UserRelation currelation = (UserRelation)db
			.createQuery("from UserRelation WHERE user=:user AND target=:targetid")
			.setInteger("user", this.getId())
			.setInteger("targetid", userid)
			.uniqueResult();
		if( userid != 0 ) {
			if( (relation != Relation.FRIEND) && (getAlly() != null) ) {
				User targetuser = (User)context.getDB().get(User.class, userid);
				if( targetuser.getAlly() == getAlly() ) {
					log.warn("Versuch die allyinterne Beziehung von User "+this.getId()+" zu "+userid+" auf "+relation+" zu aendern", new Throwable());
					return;
				}
			}
			UserRelation defrelation = (UserRelation)db
				.createQuery("from UserRelation WHERE user=:user AND target.id=0")
				.setInteger("user", this.getId())
				.uniqueResult();

			if( defrelation == null ) {
				User nullUser = (User)db.get(User.class, 0);

				defrelation = new UserRelation(this, nullUser, Relation.NEUTRAL.ordinal());
			}

			if( relation.ordinal() == defrelation.getStatus() ) {
				if( (currelation != null) && (currelation.getTarget().getId() != 0) ) {
					if( relations != null ) {
						relations.toOther.remove(userid);
					}

					db.delete(currelation);
				}
			}
			else {
				if( relations != null ) {
					relations.toOther.put(userid, relation);
				}
				if( currelation != null ) {
					currelation.setStatus(relation.ordinal());
				}
				else {
					User user = (User)db.get(User.class, userid);
					currelation = new UserRelation(this, user, relation.ordinal());
					db.persist(currelation);
				}
			}
		}
		else {
			if( relation == Relation.NEUTRAL ) {
				if( relations != null ) {
					relations.toOther.put(0, Relation.NEUTRAL);
				}
				db.createQuery("delete from UserRelation where user=:user and target.id=0")
					.setInteger("user", this.getId())
					.executeUpdate();
			}
			else {
				if( relations != null ) {
					relations.toOther.put(0, relation);
				}
				if( currelation != null ) {
					currelation.setStatus(relation.ordinal());
				}
				else {
					User nullUser = (User)db.get(User.class, 0);

					currelation = new UserRelation(this, nullUser, relation.ordinal());
					db.persist(currelation);
				}
			}
			db.createQuery("delete from UserRelation where user=:user and status=:status AND target.id!=0")
				.setInteger("user", this.getId())
				.setInteger("status", relation.ordinal())
				.executeUpdate();
		}
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
	public void transferMoneyFrom( int fromID, long count, String text, boolean faketransfer) {
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
	public boolean hasResearched( @Nonnull Collection<Forschung> forschungen )
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
	public boolean hasResearched( @Nullable Forschung research )
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
	 * Prueft, ob der Spieler noch unter Noob-Schutz steht.
	 * @return <code>true</code>, falls der Spieler noch ein Noob ist
	 */
	public boolean isNoob() {
		return new ConfigService().getValue(WellKnownConfigValue.NOOB_PROTECTION) && hasFlag(UserFlag.NOOB);
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
	public Set<Medal> getMedals() {
		int[] medals = Common.explodeToInt(";", this.medals);

		return Arrays.stream(medals).boxed().map((id) -> Medals.get().medal(id)).filter(Objects::nonNull).collect(Collectors.toSet());
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
	 * Gibt die Nahrungs- und RE-Bilanz zurueck.
	 * @return die Bilanzen
	 */
	public long[] getFullBalance()
	{
		return new long[] {
				!hasFlag(UserFlag.NO_FOOD_CONSUMPTION) ? this.getNahrungBalance() : 0,
				!hasFlag(UserFlag.NO_DESERTEUR) ? getReBalance() : 0,
		};
	}

	/**
	 * Gibt die RE-Bilanz zurueck. Ein negativer Wert bedeutet,
	 * dass der Benutzer jeden Tick RE Zahlen muss. Ein positiver,
	 * dass er jeden Tick RE erwirtschaftet.
	 * @return die Bilanzen in RE
	 */
	public long getReBalance()
	{
		int baseRe = 0;
		for(Base base: this.bases)
		{
			baseRe += base.getBalance();
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		// Kosten der Schiffe ermitteln
		Long schiffsKosten = (Long)db
			.createQuery("select sum(coalesce(sm.reCost,st.reCost)) " +
				"from Ship s join s.shiptype st left join s.modules sm " +
				"where s.owner=:user and s.docked not like 'l %'")
			.setParameter("user", this)
			.iterate().next();

		// Kosten der auf den Schiffen stationierten Einheiten ermitteln
		Long einheitenKosten = (Long)db
			.createQuery("select sum(ceil(u.amount*u.unittype.recost)) " +
				"from Ship s join s.units u "+
				"where s.owner=:user and s.docked not like 'l %'")
			.setParameter("user", this)
			.iterate().next();

		if( schiffsKosten == null )
		{
			schiffsKosten = 0L;
		}
		if( einheitenKosten == null )
		{
			einheitenKosten = 0L;
		}

		return baseRe - SchiffsReKosten.berecheKosten(schiffsKosten, einheitenKosten).longValue();
	}

	private long getNahrungBalance()
	{
		long balance = 0;
		for(Base base: this.bases)
		{
			balance += base.getNahrungsBalance();
		}

		for( Ship ship : this.ships )
		{
			if( ship.getId() <= 0 )
			{
				continue;
			}
			if( ship.getBattle() != null )
			{
				continue;
			}
			balance -= ship.getNahrungsBalance();
		}

		return balance;
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
			if (!systemlist.contains(basesystem))
			{
				systemlist.add(basesystem);
			}
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
	 * Gibt alle durch den NPC vergebbaren Raenge zurueck.
	 * @return Die Raenge
	 */
	public SortedSet<Rang> getOwnGrantableRanks()
	{
		if( this.ally != null )
		{
			return this.ally.getFullRangNameList();
		}
		return new TreeSet<>(Medals.get().raenge().values());
	}

	/**
	 * Gibt einen durch den NPC vergebbaren Rang zurueck. Falls der Rang unbekannt ist
	 * wird <code>null</code> zurueckgegeben.
	 * @param rank Die Nummer des Rangs
	 * @return Der Rang oder <code>null</code>
	 */
	public Rang getOwnGrantableRank(int rank)
	{
		for( Rang r : this.getOwnGrantableRanks() )
		{
			if( r.getId() == rank )
			{
				return r;
			}
		}
		return null;
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
	 * <p>Ermittelt zu einem gegebenen Identifier den Benutzer. Ein Identifier
	 * kann die ID des Benutzers oder sein (unformatierter) Name sein.
	 * Beim Namen werden auch teilweise Matches beruecksichtigt.</p>
	 * <p>Es wird nur dann ein User-Objekt zurueckgegeben, wenn
	 * zu dem gegebenen Identifier genau ein Benutzer ermittelt
	 * werden kann (Eindeutigkeit).</p>
	 * @param identifier Der Identifier
	 * @return Der passende Benutzer oder <code>null</code>
	 */
	public static User lookupByIdentifier(String identifier)
	{
		if( identifier.isEmpty() )
		{
			return null;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		if( NumberUtils.isCreatable(identifier) )
		{
			try
			{
				User user = (User)db.get(User.class, Integer.parseInt(identifier));
				if( user != null && user.getId() != 0 )
				{
					return user;
				}
			}
			catch( NumberFormatException e )
			{
				// Keine gueltige ID - anders weiter versuchen
			}
		}

		List<User> users = Common.cast(
			db.createQuery("select u from User u where plainname like :name and id<>0")
				.setParameter("name", "%"+identifier+"%")
				.setMaxResults(2)
				.list());

		if( users.size() == 1 )
		{
			// Nur bei Eindeutigkeit den User zurueckgeben
			// um "Unfaelle" zu vermeiden
			return users.iterator().next();
		}

		return null;
	}

	/**
	 * Liefert den Wert einer Benutzereinstellung zurueck. Sofern mehrere Eintraege zu diesem
	 * User-Value existieren wird der aelteste zurueckgegeben.
	 *
	 * @param valueDesc Die Beschreibung der Einstellung
	 * @return Wert des User-Values
	 */
	public <T> T getUserValue( WellKnownUserValue<T> valueDesc ) {
		UserValue value = (UserValue)context.getDB()
				.createQuery("from UserValue where user=:user and name=:name order by id")
				.setEntity("user", this)
				.setString("name", valueDesc.getName())
				.setMaxResults(1)
				.uniqueResult();

		return StringToTypeConverter.convert(valueDesc.getType(), value != null ? value.getValue() : valueDesc.getDefaultValue());
	}

	/**
	 * Liefert alle Werte eines User-Values zurueck.
	 * User-Values sind die Eintraege, welche sich in der Tabelle user_values befinden.
	 *
	 * @param valueDesc Die Beschreibung der Einstellung
	 * @return Werte des User-Values
	 */
	public <T> List<T> getUserValues( WellKnownUserValue<T> valueDesc ) {
		List<UserValue> values = Common.cast(context.getDB()
				.createQuery("from UserValue where user=:user and name=:name order by id")
				.setEntity("user", this)
				.setString("name", valueDesc.getName())
				.list());

		if( values.isEmpty() )
		{
			return Arrays.asList(StringToTypeConverter.convert(valueDesc.getType(), valueDesc.getDefaultValue()));
		}

		return values.stream().map(UserValue::getValue).map(v -> StringToTypeConverter.convert(valueDesc.getType(), v)).collect(Collectors.toList());
	}

	/**
	 * Setzt ein User-Value auf einen bestimmten Wert. Sollten mehrere Eintraege
	 * existieren wird nur der aelteste aktualisiert.
	 * @see #getUserValue(WellKnownUserValue)
	 *
	 * @param valueDesc Die Beschreibung der Einstellung
	 * @param newvalue neuer Wert des User-Values
	 */
	public <T> void setUserValue( WellKnownUserValue<T> valueDesc, T newvalue ) {
		UserValue valuen = (UserValue)context.getDB().createQuery("from UserValue where user=:user and name=:name order by id")
				.setEntity("user", this)
				.setString("name", valueDesc.getName())
				.uniqueResult();

		// Existiert noch kein Eintag?
		if( valuen == null && newvalue != null) {
			valuen = new UserValue(this, valueDesc.getName(), newvalue.toString());
			context.getDB().persist(valuen);
		}
		else if( newvalue != null ) {
			valuen.setValue(newvalue.toString());
		}
		else {
			context.getDB().delete(valuen);
		}
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
