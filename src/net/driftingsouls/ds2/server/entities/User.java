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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import net.driftingsouls.ds2.server.werften.ShipWerft;

import org.apache.commons.lang.StringUtils;


/**
 * Die Benutzerklasse von DS
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("default")
public class User extends BasicUser implements Loggable {
	/**
	 * Geldtransfer - Der Transfer ist manuell vom Spieler durchgefuerht worden
	 */
	public static final int TRANSFER_NORMAL = 0;
	/**
	 * Geldtransfer - Der Transfer ist in direkter Folge einer Spieleraktion ausgefuehrt worden
	 */
	public static final int TRANSFER_SEMIAUTO = 1;
	/**
	 * Geldtransfer - Der Transfer ist automatisch erfolgt
	 */
	public static final int TRANSFER_AUTO = 2;
	
	/**
	 * Der Spieler taucht in der Spielerliste nicht auf
	 */
	public static final String FLAG_HIDE = "hide";
	/**
	 * Der Spieler kann auch in entmilitarisierte Systeme mit Militaerschiffen springen
	 */
	public static final String FLAG_MILITARY_JUMPS = "miljumps";
	/**
	 * Der Spieler kann alle Schlachten sehen
	 */
	public static final String FLAG_VIEW_BATTLES = "viewbattles";
	/**
	 * Der Spieler hat Zugriff auf das NPC-Menue
	 */
	public static final String FLAG_ORDER_MENU = "ordermenu";
	/**
	 * Der Spieler kann auch NPC-Systeme sehen
	 */
	public static final String FLAG_VIEW_SYSTEMS = "viewsystems";
	/**
	 * Der Spieler kann sowohl Admin- als auch NPC-Systeme sehen 
	 */
	public static final String FLAG_VIEW_ALL_SYSTEMS = "viewallsystems";
	/**
	 * Der Spieler kann Schiffsscripte benutzen
	 */
	public static final String FLAG_EXEC_NOTES = "execnotes";
	/**
	 * Der Spieler kann Questschlachten leiten (und uebernehmen)
	 */
	public static final String FLAG_QUEST_BATTLES = "questbattles";
	/**
	 * Der Spieler sieht den Debug-Output des Scriptparsers
	 */
	public static final String FLAG_SCRIPT_DEBUGGING = "scriptdebug";
	/**
	 * Dem Spieler koennen keine Schiffe uebergeben werden
	 */
	public static final String FLAG_NO_SHIP_CONSIGN = "noshipconsign";
	/**
	 * Der Spieler kann mit Schiffen jederzeit ins System 99 springen
	 */
	public static final String FLAG_NPC_ISLAND = "npc_island";
	/**
	 * Sprungpunkte sind fuer den Spieler immer passierbar
	 */
	public static final String FLAG_NO_JUMPNODE_BLOCK = "nojnblock";
	/**
	 * Der Spieler kann jedes Schiff, egal welcher Besitzer und wie Gross andocken
	 */
	public static final String FLAG_SUPER_DOCK = "superdock";
	/**
	 * Der Spieler hat Moderatorrechte im Handel
	 */
	public static final String FLAG_MODERATOR_HANDEL = "moderator_handel";
	/**
	 * Der Spieler ist ein Noob
	 */
	public static final String FLAG_NOOB = "noob";
	
	/**
	 * Die Arten von Beziehungen zwischen zwei Spielern
	 * @author Christopher Jung
	 *
	 */
	public static enum Relation {
		/**
		 * Neutral
		 */
		NEUTRAL,	// 0
		/**
		 * Feindlich
		 */
		ENEMY,		// 1
		/**
		 * Freundlich
		 */
		FRIEND;		// 2
	}

	/**
	 * Klasse, welche die Beziehungen eines Spielers zu anderen
	 * Spielern enthaelt
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
		 * Schluessel ist die Spieler-ID
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
	private String cargo;
	private double foodpooldegeneration;
	private String nstat;
	private int npcpunkte;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="allyposten", nullable=true)
	private AllyPosten allyposten;
	private int gtudropzone;
	private String npcorderloc;
	private Integer flagschiff;
	private short lostBattles;
	private short wonBattles;
	private int destroyedShips;
	private int lostShips;
	private String knownItems;
	private boolean blocked = false;
	
	@Transient
	private Context context;
	@Transient
	private UserFlagschiffLocation flagschiffObj = null;
	@Transient
	private Map<Integer,UserResearch> researched;
	
	/**
	 * Konstruktor
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
		setCargo(cargo.save());
		setEmail(email);
		setUn(name);
		setFlag(User.FLAG_NOOB);
		setNahrungsStat("0");
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
		setGtuDropZone(2);
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
		db.persist(this);
		Ordner trash = Ordner.createNewOrdner("Papierkorb", Ordner.getOrdnerByID(0, this), this);
		trash.setFlags(Ordner.FLAG_TRASH);
		addResearch(0);
	}
	
	/**
	 * Macht alle geladenen Benutzereigenschaften dem Templateengine bekannt.
	 * Die daraus resultierenden Template-Variablen haben die Form Prefix+"."+Datenbankname.
	 * Die Eigenschaft Wait4Vacation, welche den Datenbanknamen "wait4vac" hat, wuerde sich, beim 
	 * Prefix "activeuser", somit in der Template-Variablen "activeuser.wait4vac" wiederfinden
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
				pre+"cargo", this.cargo,
				pre+"nstat", this.nstat,
				pre+"npcpunkte", this.npcpunkte,
				pre+"allyposten", this.allyposten,
				pre+"gtudropzone", this.gtudropzone,
				pre+"npcorderloc", this.npcorderloc,
				pre+"flagschiff", this.flagschiff,
				pre+"lostBattles", this.lostBattles,
				pre+"wonBattles", this.wonBattles,
				pre+"destroyedShips", this.destroyedShips,
				pre+"lostShips", this.lostShips,
				pre+"knownItems", this.knownItems,
				pre+"vaccount", this.vaccount,
				pre+"wait4vac", this.wait4vac);
	}
	
	/**
	 * Liefert einen Profile-Link zu den Benutzer zurueck (als HTML).
	 * Als CSS-Klasse fuer den Link wird die angegebene Klasse verwendet
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
	 * Der Linkt enthaelt einen &lt;a&gt;-Tag sowie den Benutzernamen als HTML
	 * @return Der Profile-Link
	 */
	public String getProfileLink() {
		return getProfileLink("");
	}
	
	/**
	 * Setzt den Spieler-Cargo auf den angegebenen Cargo-String in der Datenbank.
	 * Um inkonsistenzen zu vermeiden wird zudem geprueft, ob der urspruengliche
	 * Cargo-String noch aktuell ist.
	 * @param cargo Der neue Cargo-String
	 * @param oldString Der urspruengliche Cargo-String
	 */
	@Deprecated
	public void setCargo(String cargo, String oldString) {
		setCargo(cargo);
	}
	
	/**
	 * Setzt den Spieler-Cargo auf den angegebenen Cargo-String in der Datenbank
	 * @param cargo Der Cargo-String
	 */
	public void setCargo(String cargo) {
		this.cargo = cargo;
	}
	
	/**
	 * Stellt fest, ob noch Platz fuer ein Flagschiff vorhanden ist
	 * 
	 * @return true, falls noch Platz vorhanden ist
	 */
	public boolean hasFlagschiffSpace() {
		return getFlagschiff() == null;
	}
	
	/**
	 * Liefert den Aufenthaltsort des Flagschiffs dieses Spielers.
	 * Der Typ Aufenthaltsort kann entweder ein Schiff (normal), eine Basiswerft
	 * oder eine Schiffswerft sein (in beiden Faellen wird das Schiff noch gebaut)
	 * 
	 * @return Infos zum Aufenthaltsort
	 */
	public UserFlagschiffLocation getFlagschiff() {
		org.hibernate.Session db = context.getDB();

		if( this.flagschiffObj != null ) {
			return (UserFlagschiffLocation)this.flagschiffObj.clone();	
		}

		if( this.flagschiff == null ) {
			ShipWerft swerft = (ShipWerft)db.createQuery("from ShipWerft as sw left join fetch sw.linkedWerft " +
					"where (sw.buildFlagschiff=1 or (sw.linkedWerft is not null and sw.linkedWerft.buildFlagschiff=1)) and sw.ship.owner=?")
				.setEntity(0, this)
				.setMaxResults(1)
				.uniqueResult();
			
			if( swerft == null ) {
				BaseWerft bwerft = (BaseWerft)db.createQuery("from BaseWerft as bw left join fetch bw.linkedWerft " +
						"where (bw.buildFlagschiff=1 or (bw.linkedWerft is not null and bw.linkedWerft.buildFlagschiff=1)) and bw.base.owner=?")
					.setEntity(0, this)
					.setMaxResults(1)
					.uniqueResult();
				if( bwerft != null ) {
					flagschiffObj = new UserFlagschiffLocation(UserFlagschiffLocation.Type.WERFT_BASE, bwerft.getBaseID());
				}
			}
			else {
				flagschiffObj = new UserFlagschiffLocation(UserFlagschiffLocation.Type.WERFT_SHIP, swerft.getShipID());
			}
		}
		else {
			flagschiffObj = new UserFlagschiffLocation(UserFlagschiffLocation.Type.SHIP, this.flagschiff);
		}
	
		if( flagschiffObj != null ) {
			return (UserFlagschiffLocation)flagschiffObj.clone();
		}
		return null;
	}
	
	/**
	 * Setzt die Schiffs-ID des Flagschiffs. Falls diese 0 ist, besitzt der Spieler kein Flagschiff mehr
	 * 
	 * @param shipid Die Schiffs-ID des Flagschiffs
	 */
	public void setFlagschiff(Integer shipid) {
		this.flagschiff = shipid;
	}
	
	@Transient
	private Relations relations = null;
	
	/**
	 * Liefert alle Beziehungen vom Spieler selbst zu anderen Spielern und umgekehrt
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
			
			List<UserRelation> relationlist = context.query("from UserRelation " +
					"where user="+this.getId()+" OR target="+this.getId()+" OR (user!="+this.getId()+" AND target=0) " +
					"order by abs(target) desc", UserRelation.class);
			
			for( UserRelation relation : relationlist ) {
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
	 * Gibt den Status der Beziehung des Spielers zu einem anderen Spieler zurueck
	 * @param userid Die ID des anderen Spielers
	 * @return Der Status der Beziehungen zu dem anderen Spieler
	 */
	public Relation getRelation( int userid ) {
		if( userid == this.getId() ) {
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
	 * Wert
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
					LOG.warn("Versuch die allyinterne Beziehung von User "+this.getId()+" zu "+userid+" auf "+relation+" zu aendern", new Throwable());
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
	 * Die Berechnung erfolgt intern auf Basis von <code>BigInteger</code>
	 * 
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 * @param faketransfer Handelt es sich um einen "gefakten" Geldtransfer (<code>true</code>)?
	 * @param transfertype Der Transfertyp (Kategorie)
	 * @see #TRANSFER_AUTO
	 * @see #TRANSFER_SEMIAUTO
	 * @see #TRANSFER_NORMAL
	 */
	public void transferMoneyFrom( int fromID, long count, String text, boolean faketransfer, int transfertype) {
		transferMoneyFrom(fromID,BigInteger.valueOf(count), text, faketransfer, transfertype);
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
	 * @see #TRANSFER_AUTO
	 * @see #TRANSFER_SEMIAUTO
	 * @see #TRANSFER_NORMAL
	 */
	public void transferMoneyFrom( int fromID, BigInteger count, String text, boolean faketransfer, int transfertype) {
		org.hibernate.Session db = context.getDB();
		
		if( !count.equals(BigInteger.ZERO) ) {
			User fromUser = (User)context.getDB().get(User.class, fromID);
			if( (fromID != 0) && !faketransfer ) {
				fromUser.setKonto(fromUser.getKonto().subtract(count));
			}
		
			konto = konto.add(count);	
		
			UserMoneyTransfer log = new UserMoneyTransfer(fromUser, this, count, text);
			log.setFake(faketransfer);
			log.setType(UserMoneyTransfer.Transfer.values()[transfertype]);
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
	 * @see #transferMoneyFrom(int, long, String, boolean, int)
	 */
	public void transferMoneyFrom( int fromID, long count, String text, boolean faketransfer) {
		transferMoneyFrom( fromID, count, text, faketransfer, TRANSFER_NORMAL );
	}
	
	/**
	 * Transferiert einen bestimmten Geldbetrag (RE) von einem anderen Spieler zum aktuellen. Beim
	 * Transfer handelt es sich um einen manuellen Transfer. Das Geld wird tatsaechlich dem Ausgangsspieler
	 * abgezogen (kein "gefakter" Transfer)
	 * 
	 * @param fromID Die ID des Benutzers, von dem Geld abgebucht werden soll
	 * @param count Die zu transferierende Geldmenge
	 * @param text Der Hinweistext, welcher im "Kontoauszug" angezeigt werden soll
	 */
	public void transferMoneyFrom( int fromID, long count, String text ) {
		transferMoneyFrom( fromID, count, text, false );
	}
	
	private int vaccount;
	private int wait4vac;
	
	/**
	 * Prueft, ob die angegebene Forschung durch den Benutzer erforscht wurde
	 * 
	 * @param researchID Die ID der zu pruefenden Forschung
	 * @return <code>true</code>, falls die Forschung erforscht wurde
	 */
	public boolean hasResearched( int researchID ) {
		return getUserResearch(Forschung.getInstance(researchID)) != null;
	}
	
	/**
	 * Fuegt eine Forschung zur Liste der durch den Benutzer erforschten Technologien hinzu
	 * @param researchID Die ID der erforschten Technologie
	 */
	public void addResearch( int researchID ) {
		org.hibernate.Session db = context.getDB();
		UserResearch userres = new UserResearch(this, Forschung.getInstance(researchID));
		db.persist(userres);
		
		if( this.researched != null ) {
			this.researched.put(researchID, userres);
		}
	}
	
	/**
	 * Fuegt eine Zeile zur User-Historie hinzu
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
	 * Prueft, ob der Spieler noch unter Noob-Schutz steht
	 * @return <code>true</code>, falls der Spieler noch ein Noob ist
	 */
	public boolean isNoob() {
		if( Configuration.getIntSetting("NOOB_PROTECTION") > 0 ) {
			if( this.getId() < 0 ) {
				return false;
			}
			
			return hasFlag( FLAG_NOOB );
		}
		return false;
	}
	
	/**
	 * Gibt die ID der Rasse des Spielers zurueck
	 * @return Die ID der Rasse
	 */
	public int getRace() {
		return this.race;
	}

	/**
	 * Gibt die Spielerhistorie als BBCode-formatierten String zurueck
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
	 * Setzt die Liste der Orden des Spielers
	 * @param medals Eine mittels ; separierte Liste von Orden
	 */
	public void setMedals( String medals ) {
		this.medals = medals;
	}
	
	/**
	 * Liefert den Rang des Benutzers zurueck
	 * @return Der Rang
	 */
	public byte getRang() {
		return this.rang;
	}
	
	/**
	 * Setzt den Rang des Benutzers
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
	 * Setzt die Allianz, der der Spieler angehoert
	 * @param ally die neue Allianz
	 */
	public void setAlly( Ally ally ) {
		this.ally = ally;
	}
	
	/**
	 * Liefert den Kontostand des Benutzers zurueck
	 * @return Der Kontostand
	 */
	public BigInteger getKonto() {
		return konto;
	}
	
	/**
	 * Setzt den Kontostand des Spielers auf den angegebenen Wert
	 * @param count der neue Kontostand
	 */
	public void setKonto( BigInteger count ) {
		this.konto = count;
	}
	
	/**
	 * Gibt den Cargo des Spielers als Cargo-String zurueck
	 * @return der Cargo des Spielers
	 */
	public String getCargo() {
		return this.cargo;
	}
	
	/**
	 * Die Nahrungsbilanz des letzten Ticks
	 * @return Die Nahrungsbilanz des letzten Ticks
	 */
	public String getNahrungsStat() {
		return this.nstat;
	}
	
	/**
	 * Setzt die Nahrungsbilanz des letzten Ticks
	 * @param stat Die Nahrungsbilanz des letzten Ticks
	 */
	public void setNahrungsStat(String stat) {
		this.nstat = stat;
	}
	
	/**
	 * Liefert die Anzahl der NPC-Punkte des Benutzers zurueck.
	 * @return Die Anzahl der NPC-Punkte
	 */
	public int getNpcPunkte() {
		return this.npcpunkte;
	}
	
	/**
	 * Setzt die Anzahl der NPC-Punkte des Benutzers
	 * @param punkte Die neue Anzahl der NPC-Punkte
	 */
	public void setNpcPunkte(int punkte) {
		this.npcpunkte = punkte;
	}
	
	/**
	 * Gibt den durch den Spieler besetzten Allianz-Posten zurueck.
	 * zurueckgegeben. 
	 * @return Der AllyPosten oder <code>null</code>
	 */
	public AllyPosten getAllyPosten() {
		return this.allyposten;
	}
	
	/**
	 * Setzt den durch den Spieler besetzten Allianz-Posten
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
	 * Setzt die ID des von der GTU verwendeten Dropzone-Systems des Spielers
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
	 * Setzt die Koordinaten des Ortes, an dem von NPCs georderte Dinge erscheinen sollen
	 * @param loc Die Koordinaten des Ortes, an dem georderte Dinge erscheinen sollen
	 */
	public void setNpcOrderLocation( String loc ) {
		this.npcorderloc = loc;
	}
	
	/**
	 * Gibt die Anzahl der gewonnenen Schlachten zurueck
	 * @return die Anzahl der gewonnenen Schlachten
	 */
	public short getWonBattles() {
		return this.wonBattles;
	}
	
	/**
	 * Setzt die Anzahl der gewonnenen Schlachten
	 * @param battles Die Anzahl
	 */
	public void setWonBattles(short battles) {
		this.wonBattles = battles;
	}
	
	/**
	 * Gibt die Anzahl der verlorenen Schlachten zurueck
	 * @return die Anzahl der verlorenen Schlachten
	 */
	public short getLostBattles() {
		return this.lostBattles;
	}
	
	/**
	 * Setzt die Anzahl der verlorenen Schlachten
	 * @param battles Die Anzahl
	 */
	public void setLostBattles(short battles) {
		this.lostBattles = battles;
	}
	
	/**
	 * Gibt die Anzahl der verlorenen Schiffe zurueck
	 * @return die Anzahl der verlorenen Schiffe
	 */
	public int getLostShips() {
		return this.lostShips;
	}
	
	/**
	 * Setzt die Anzahl der verlorenen Schiffe
	 * @param lost Die Anzahl
	 */
	public void setLostShips(int lost) {
		this.lostShips = lost;
	}
	
	/**
	 * Gibt die Anzahl der zerstoerten Schiffe zurueck
	 * @return die Anzahl der zerstoerten Schiffe
	 */
	public int getDestroyedShips() {
		return this.destroyedShips;
	}
	
	/**
	 * Setzt die Anzahl der zerstoerten Schiffe
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
	 * Setzt die Anzahl der Ticks, die der Account im Vacation-Modus verbringen soll
	 * @param value Die Anzahl der Ticks im Vacation-Modus
	 */
	public void setVacationCount(int value) {
		this.vaccount = value;
	}

	/**
	 * Gibt zurueck, wieviele Ticks sich der Account noch im Vorlauf fuer den
	 * Vacation-Modus befindet
	 * @return Die Anzahl der verbleibenden Ticks im Vacation-Vorlauf 
	 */
	public int getWait4VacationCount() {
		return this.wait4vac;
	}

	/**
	 * Setzt die Anzahl der Ticks des Vacation-Modus-Vorlaufs auf den angegebenen
	 * Wert
	 * @param value Die Anzahl der Ticks im Vacation-Modus-Vorlauf
	 */
	public void setWait4VacationCount(int value) {
		this.wait4vac = value;
	}
	
	/**
	 * Gibt zurueck, ob der User wegen einer Tickberechnung kurzzeitig blockiert wird
	 * @return <code>true</code>, falls er geblockt wird
	 */
	public boolean isBlocked() {
		return this.blocked;
	}
	
	/**
	 * Setzt, ob der User wegen einer Tickberechnung kurzzeitig geblockt wird
	 * @param blocked <code>true</code>, falls er geblockt wird
	 */
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
	
	/**
	 * Gibt zur angegebenen Forschung die Forschungsdaten des Benutzers zurueck.
	 * Falls der Benutzer die Forschung noch nicht hat wird <code>null</code> zurueckgegeben.
	 * @param research Die Forschung
	 * @return Die Forschungsdaten oder <code>null</code>
	 */
	public UserResearch getUserResearch(Forschung research) {
		if(research == null) {
			return null;
		}
		
		if( this.researched == null ) {
			this.researched = new HashMap<Integer,UserResearch>();
			
			org.hibernate.Session db = context.getDB();
			
			List userresList = db.createQuery("from UserResearch where owner= :user")
				.setEntity("user", this)
				.list();
			
			for( Iterator iter=userresList.iterator(); iter.hasNext(); ) {
				UserResearch userres = (UserResearch)iter.next();
				
				this.researched.put(userres.getResearch().getID(), userres);
			}
		}
		return this.researched.get(research.getID());
	}
	
	/**
	 * @return Prozent des Pools, die jeden Tick mehr/weniger verfaulen als der Grundwert angibt.
	 */
	public double getFoodpooldegeneration()
	{
		return foodpooldegeneration;
	}

	/**
	 * Setzt wieviel/mehr weniger Nahrung bei diesem Nutzer pro Tick verfault.
	 * 
	 * @param foodpooldegeneration Der Offset.
	 */
	public void setFoodpooldegeneration(double foodpooldegeneration)
	{
		this.foodpooldegeneration = foodpooldegeneration;
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
}
