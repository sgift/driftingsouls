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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.units.UnitCargo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;


/**
 * Die Benutzerklasse von DS.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("default")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Configurable
@BatchSize(size=50)
public class User extends BasicUser {
	private static final Log log = LogFactory.getLog(User.class);

	/**
	 * Der Spieler taucht in der Spielerliste nicht auf.
	 */
	public static final String FLAG_HIDE = "hide";
	/**
	 * Der Spieler kann auch in entmilitarisierte Systeme mit Militaerschiffen springen.
	 */
	public static final String FLAG_MILITARY_JUMPS = "miljumps";
	/**
	 * Der Spieler kann alle Schlachten sehen.
	 */
	public static final String FLAG_VIEW_BATTLES = "viewbattles";
	/**
	 * Der Spieler hat Zugriff auf das NPC-Menue.
	 */
	public static final String FLAG_ORDER_MENU = "ordermenu";
	/**
	 * Der Spieler kann auch NPC-Systeme sehen.
	 */
	public static final String FLAG_VIEW_SYSTEMS = "viewsystems";
	/**
	 * Der Spieler kann sowohl Admin- als auch NPC-Systeme sehen.
	 */
	public static final String FLAG_VIEW_ALL_SYSTEMS = "viewallsystems";
	/**
	 * Der Spieler kann Schiffsscripte benutzen.
	 */
	public static final String FLAG_EXEC_NOTES = "execnotes";
	/**
	 * Der Spieler kann Questschlachten leiten (und uebernehmen).
	 */
	public static final String FLAG_QUEST_BATTLES = "questbattles";
	/**
	 * Der Spieler sieht den Debug-Output des Scriptparsers.
	 */
	public static final String FLAG_SCRIPT_DEBUGGING = "scriptdebug";
	/**
	 * Der Spieler sieht zusaetzliche Anzeigen der TWs im Kampf.
	 */
	public static final String FLAG_KS_DEBUG = "ks_debug";
	/**
	 * Dem Spieler koennen keine Schiffe uebergeben werden.
	 */
	public static final String FLAG_NO_SHIP_CONSIGN = "noshipconsign";
	/**
	 * Der Spieler kann mit Schiffen jederzeit ins System 99 springen.
	 */
	public static final String FLAG_NPC_ISLAND = "npc_island";
	/**
	 * Sprungpunkte sind fuer den Spieler immer passierbar.
	 */
	public static final String FLAG_NO_JUMPNODE_BLOCK = "nojnblock";
	/**
	 * Der Spieler kann jedes Schiff, egal welcher Besitzer und wie Gross andocken.
	 */
	public static final String FLAG_SUPER_DOCK = "superdock";
	/**
	 * Der Spieler hat Moderatorrechte im Handel.
	 */
	public static final String FLAG_MODERATOR_HANDEL = "moderator_handel";
	/**
	 * Der Spieler ist ein Noob.
	 */
	public static final String FLAG_NOOB = "noob";
	/**
	 * Die Schiffe des Spielers werden nicht beschaedigt, wenn sie zu wenig Crew haben.
	 */
	public static final String FLAG_NO_HULL_DECAY = "nohulldecay";
	/**
	 * Die Schiffe des Spielers laufen nicht zur Ratte ueber, wenn zu wenig Geld auf dem Konto ist.
	 */
	public static final String FLAG_NO_DESERTEUR = "nodeserteur";
    /**
     * Kann alle Kaempfe uebernehmen, egal wer sie gerade kommandiert.
     */
    public static final String FLAG_KS_TAKE_BATTLES = "cantakeallbattles";
    /**
     * Dieser Spieler setzt nie automatisch Kopfgeld aus.
     */
    public static final String FLAG_NO_AUTO_BOUNTY = "noautobounty";
    /**
     * Dieser Spieler braucht keine Nahrung.
     */
    public static String FLAG_NO_FOOD_CONSUMPTION = "nofoodconsumption";

    /**
	 * Die Arten von Beziehungen zwischen zwei Spielern.
	 * @author Christopher Jung
	 *
	 */
	public static enum Relation {
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
		public Map<Integer,Relation> toOther = new HashMap<Integer,Relation>();
		/**
		 * Die Beziehungen von anderen Spielern zum Spieler selbst.
		 * Schluessel ist die Spieler-ID.
		 */
		public Map<Integer,Relation> fromOther = new HashMap<Integer,Relation>();
		
		protected Relations() {
			// EMPTY
		}
	}

	private int race;
	private String history;
	private String medals;
	private byte rang;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="ally", nullable=true)
	private Ally ally;
	private BigInteger konto;
	private int npcpunkte;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="allyposten", nullable=true)
	private AllyPosten allyposten;
	private int gtudropzone;
	private String npcorderloc;
	private short lostBattles;
	private short wonBattles;
	private int destroyedShips;
	private int lostShips;
	private String knownItems;
	private int vacpoints;
	private int specializationPoints;
    private BigInteger bounty;
    @OneToMany(mappedBy="userRankKey.owner")
    private Set<UserRank> userRanks;
	
	@OneToMany
	@Cascade({org.hibernate.annotations.CascadeType.EVICT,org.hibernate.annotations.CascadeType.REFRESH})
	@JoinColumn(name="owner")
	private Set<UserResearch> researches;
	
	@OneToMany
	@Cascade({org.hibernate.annotations.CascadeType.EVICT,org.hibernate.annotations.CascadeType.REFRESH})
	@JoinColumn(name="owner")
	// Explizit nur die Bases eines Users laden - sonst kommt Hibernate von Zeit zu Zeit auf die Idee die Bases von User 0 mitzuladen...
	@BatchSize(size=1)
	private Set<Base> bases;
	
	@OneToMany
	@Cascade({org.hibernate.annotations.CascadeType.EVICT,org.hibernate.annotations.CascadeType.REFRESH})
	@JoinColumn(name="owner")
	@BatchSize(size=1)
	private Set<Ship> ships;
	
	@Transient
	private Context context;
	
	@Transient
	private Configuration config;
	
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
		setFlag(User.FLAG_NOOB);
		setSignup((int)Common.time());
		setImagePath(BasicUser.getDefaultImagePath());
		setInactivity(0);
		setMedals("");
		setRang(Byte.valueOf("0"));
		setKonto(BigInteger.valueOf(0));
		setLoginFailedCount(0);
		setAccesslevel(0);
		setNpcPunkte(0);
		setNickname("Kolonist");
		setPlainname("Kolonist");
		setNpcOrderLocation("");
		setDisabled(false);
		setVacationCount(0);
		setWait4VacationCount(0);
		setLostBattles(Short.valueOf("0"));
		setLostShips(0);
		setWonBattles(Short.valueOf("0"));
		setDestroyedShips(0);
		int newUserId = (Integer)db.createQuery("SELECT max(id) from User").uniqueResult();
		newUserId++;
		setId(newUserId);
		this.knownItems = "";
        bounty = BigInteger.ZERO;
		db.persist(this);
		Ordner trash = Ordner.createNewOrdner("Papierkorb", Ordner.getOrdnerByID(0, this), this);
		trash.setFlags(Ordner.FLAG_TRASH);
		this.researches = new HashSet<UserResearch>();
		addResearch(0);
		
		ConfigValue value = (ConfigValue)db.get(ConfigValue.class, "gtudefaultdropzone");
		int defaultDropZone = Integer.valueOf(value.getValue());
		setGtuDropZone(defaultDropZone);
	}
	
    /**
    * Injiziert die DS-Konfiguration.
    * @param config Die DS-Konfiguration
    */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
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
			Relations relations = new Relations();
			
			org.hibernate.Session db = context.getDB();
			
			List<?> relationlist = db.createQuery("from UserRelation " +
					"where user= :user OR target= :user OR (user!= :user AND target=0) " +
					"order by abs(target) desc")
				.setEntity("user", this)
				.list();
			
			for( Iterator<?> iter=relationlist.iterator(); iter.hasNext(); ) {
				UserRelation relation = (UserRelation)iter.next();
				if( relation.getUser().getId() == this.getId() ) {
					relations.toOther.put(relation.getTarget().getId(), Relation.values()[relation.getStatus()]);	
				}
				else if( !relations.fromOther.containsKey(relation.getUser().getId()) ) {
					relations.fromOther.put(relation.getUser().getId(), Relation.values()[relation.getStatus()]);
				}
			}

			if( !relations.toOther.containsKey(0) ) {
				relations.toOther.put(0, Relation.NEUTRAL);	
			}
			
			relations.toOther.put(this.getId(), Relation.FRIEND);
			relations.fromOther.put(this.getId(), Relation.FRIEND);
			
			this.relations = relations;
		}

		Relations rel = new Relations();
		rel.fromOther.putAll(relations.fromOther);
		rel.toOther.putAll(relations.toOther);
		return rel;
	}
	
	/**
	 * Gibt den Status der Beziehung des Spielers zu einem anderen Spieler zurueck.
	 * @param userid Die ID des anderen Spielers
	 * @return Der Status der Beziehungen zu dem anderen Spieler
	 */
	public Relation getRelation(int userid) 
	{
		if( userid == this.getId() ) 
		{
			return Relation.FRIEND;
		}
		
		Relation rel = Relation.NEUTRAL;
		
		if( relations == null ) {
			UserRelation currelation = (UserRelation)context.getDB()
				.createQuery("from UserRelation WHERE user=? AND target_id=?")
				.setInteger(0, this.getId())
				.setInteger(1, userid)
				.uniqueResult();
			
			if( currelation == null ) {
				currelation = (UserRelation)context.getDB()
					.createQuery("from UserRelation WHERE user=? AND target_id=0")
					.setInteger(0, this.getId())
					.uniqueResult();
			}
		
			if( currelation != null ) {
				rel = Relation.values()[currelation.getStatus()];	
			}
		}
		else {
			if( relations.toOther.containsKey(userid) ) {
				rel = relations.toOther.get(userid);	
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
			.createQuery("from UserRelation WHERE user=? AND target_id=?")
			.setInteger(0, this.getId())
			.setInteger(1, userid)
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
				.createQuery("from UserRelation WHERE user=? AND target_id=0")
				.setInteger(0, this.getId())
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
				db.createQuery("delete from UserRelation where user=? and target=0")
					.setInteger(0, this.getId())
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
			db.createQuery("delete from UserRelation where user=? and status=? AND target!=0")
				.setInteger(0, this.getId())
				.setInteger(1, relation.ordinal())
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
        this.bounty.add(add);
    }

    /**
	 * Gibt das auf den Spieler ausgesetzte Kopfgeld in RE zurueck.
	 * @return Das Kopfgeld
	 */
    public BigInteger getBounty()
    {
        return this.bounty;
    }
	
	private int vaccount;
	private int wait4vac;
	
	/**
	 * Prueft, ob die angegebene Forschung durch den Benutzer erforscht wurde.
	 * 
	 * @param researchID Die ID der zu pruefenden Forschung
	 * @return <code>true</code>, falls die Forschung erforscht wurde
	 */
	public boolean hasResearched( int researchID ) {
		return getUserResearch(Forschung.getInstance(researchID)) != null;
	}
	
	/**
	 * Fuegt eine Forschung zur Liste der durch den Benutzer erforschten Technologien hinzu,
	 * wenn er sie noch nicht hatte.
	 * 
	 * @param researchID Die ID der erforschten Technologie
	 */
	public void addResearch( int researchID ) {
		org.hibernate.Session db = context.getDB();
		Forschung research = Forschung.getInstance(researchID);
		
		if( this.getUserResearch(research) == null )
		{
			UserResearch userres = new UserResearch(this, research);
			db.persist(userres);
			this.researches.add(userres);
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
		if( config.getInt("NOOB_PROTECTION") > 0 ) {
			if( this.getId() < 0 ) {
				return false;
			}
			
			return hasFlag( FLAG_NOOB );
		}
		return false;
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
	public void setMedals( String medals ) {
		this.medals = medals;
	}
	
	/**
	 * Liefert den Rang des Benutzers zurueck.
	 * @return Der Rang
	 */
	public byte getRang() {
		return this.rang;
	}
	
	/**
	 * Setzt den Rang des Benutzers.
	 * @param rang Die ID des Rangs
	 */
	public void setRang( byte rang ) {
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
	 * Gibt an, ob der Spieler ein Admin ist.
	 * 
	 * @return <code>true</code>, wenn der Spieler Admin ist, sonst <code>false</code>.
	 */
	@Override
	public boolean isAdmin()
	{
		return getAccessLevel() >= 30;
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
	 * Gibt zur angegebenen Forschung die Forschungsdaten des Benutzers zurueck.
	 * Falls der Benutzer die Forschung noch nicht hat wird <code>null</code> zurueckgegeben.
	 * @param research Die Forschung
	 * @return Die Forschungsdaten oder <code>null</code>
	 */
	public UserResearch getUserResearch(Forschung research) {
		if(research == null) 
		{
			return null;
		}
		
		for( UserResearch aresearch : this.researches )
		{
			if( aresearch.getResearch().equals(research) )
			{
				return aresearch;
			}
		}
		return null;
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
		
		if(costs > getVacpoints())
		{
			return false;
		}
		
		return true;
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
		return ticks / Common.TICKS_PER_DAY;
	}
	
	/**
	 * @return Punktekosten fuer einen Urlaubstick.
	 */
	private int vacationCostsPerTick()
	{
		Session db = ContextMap.getContext().getDB();
		ConfigValue value = (ConfigValue)db.get(ConfigValue.class, "vacpointspervactick");
		return Integer.valueOf(value.getValue());
	}

	/**
	 * checks if the user is able to see the item.
	 * @param aitem a specific item
	 * @return boolean, true if user  is able to see the item
	 */
	public boolean canSeeItem(Item aitem) {
		boolean check = false;
		if( aitem.getAccessLevel() <= this.getAccessLevel() ) {
			check = true;	
		}
		return check;
	}
	
	/**
	 * Gibt die Nahrungs- und RE-Bilanz zurueck.
	 * @return die Bilanzen
	 */
	public int[] getFullBalance()
	{
		return this.getFullBalance(true);
	}
	
	/**
	 * Gibt die RE-Bilanz zurueck.
	 * @return die Bilanzen
	 */
	public int getReBalance()
	{
		return this.getFullBalance(false)[1];
	}
	
	private int[] getFullBalance(boolean includeFood)
	{
		int[] balance = new int[2];
		balance[0] = 0;
		balance[1] = 0;

		for(Base base: this.bases)
		{
			if( includeFood ) {
				balance[0] += base.getNahrungsBalance();
			}
			balance[1] += base.getBalance();
		}
		
		// Eigentliche Abfrage nur ausfuehren, wenn auch Schiffe vorhanden
		// Grund: Hibernate mag kein scroll+join fetch auf collections wenn es keine Ergebnisse gibt (#HHH-2293)
		/*ScrollableResults ships = db.createQuery("select distinct s from Ship s left join fetch s.units where s.owner=:owner and s.id>0 and s.battle is null")
		 							.setParameter("owner", this)
		 							.setCacheMode(CacheMode.IGNORE)
		 							.scroll(ScrollMode.FORWARD_ONLY);
		while(ships.next())
		{
			Ship ship = (Ship)ships.get(0);
			if( includeFood ) {
				balance[0] -= ship.getNahrungsBalance();
			}
			balance[1] -= ship.getBalance();
		}*/
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
			if( includeFood )
			{
				balance[0] -= ship.getNahrungsBalance();
			}
			balance[1] -= ship.getBalance();
		}
		
		return balance;
	}
	
	/**
	 * returns a Set of all systems the user has a colony in.
	 * @return the set of all systems the user has a colony in.
	 */
	public Set<Integer> getAstiSystems()
	{
		Set<Integer> systemlist = new HashSet<Integer>();
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
		long usedSpecpoints =  (Long)db.createQuery("select sum(res.research.specializationCosts) from UserResearch res where res.owner=:owner")
		  		   	   				   .setParameter("owner", this)
		  		   	   				   .uniqueResult();
		
		//Add researchs, which are currently developed in research centers
		List<Forschungszentrum> researchcenters = Common.cast(db.createQuery("from Forschungszentrum where forschung is not null and base.owner=?")
												  		  		.setEntity(0, this)
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
		UserResearch userResearch = getUserResearch(research);
		if(userResearch == null)
		{
			return;
		}
		
		//Drop dependent researchs
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<Forschung> dependentResearchs = Common.cast(db.createQuery("from Forschung where req1= :fid or req2= :fid or req3= :fid")
									  			  .setInteger("fid", research.getID())
									  			  .list());
		
		for(Forschung dependentResearch: dependentResearchs)
		{
			dropResearch(dependentResearch);
		}
		
		db.delete(userResearch);
	}

	/**
	 * Gibt zurueck, ob der Einheitentyp dem User bekannt ist.
	 * @param id Die ID des Einheitentyps
	 * @return <code>true</code>, falls die Einheit dem User bekannt ist, sonst <code>false</code>
	 */
	public boolean isKnownUnit(int id) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		User user = (User)ContextMap.getContext().getActiveUser();
		long baseunit = 0;
		long shipunit = 0;
		
		Object baseunitsuserobject = db.createQuery("select sum(e.amount) from UnitCargoEntry as e, Base as b where e.key.type=:type and e.key.unittype=:unittype and e.key.destid = b.id and b.owner=:user")
				.setInteger("type", UnitCargo.CARGO_ENTRY_BASE)
				.setInteger("unittype", id)
				.setEntity("user", user)
				.iterate()
				.next();
		if( baseunitsuserobject != null)
		{
			baseunit = (Long)baseunitsuserobject;
		}
		
		Object shipunitsuserobject = db.createQuery("select sum(e.amount) from UnitCargoEntry as e, Ship as s where e.key.type=:type and e.key.unittype=:unittype and e.key.destid = s.id and s.owner=:user")
				.setInteger("type", UnitCargo.CARGO_ENTRY_SHIP)
				.setInteger("unittype", id)
				.setEntity("user", user)
				.iterate()
				.next();
		
		if( shipunitsuserobject != null)
		{
			shipunit = (Long)shipunitsuserobject;
		}
		
		return baseunit+shipunit > 0 || user.isAdmin();
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
				rang.setRank(rank);
				return;
			}
		}
		
		UserRank.UserRankKey key = new UserRank.UserRankKey(this, rankGiver);

		org.hibernate.Session db = ContextMap.getContext().getDB();
		UserRank userRank = new UserRank(key, rank);
		db.persist(userRank);
	}

	/**
	 * Gibt den Rang eines Benutzers bei einem bestimmten NPC zurueck.
	 * Falls kein Rang vorhanden ist wird der niederigste moegliche Rang
	 * zurueckgegeben.
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
}
