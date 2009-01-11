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
package net.driftingsouls.ds2.server.scripting.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;

import org.hibernate.annotations.Type;

/**
 * Repraesentiert ein QuickQuest.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="quests_quick")
public class QuickQuest {
	@Id @GeneratedValue
	private int id;
	private String qid = "";
	private int enabled;
	private String qname = "";
	private String dependsOnQuests = "";
	private int moreThanOnce;
	private String head = "ars.gif";
	@Column(name="`desc`")
	private String description = "";
	private String shortdesc = "";
	private String finishtext = "";
	private String notyettext = "";
	private String source = "0";
	private String sourcetype = "gtuposten";
	private String target = "0";
	private String targettype = "gtuposten";
	@Type(type="cargo")
	private Cargo startitems;
	@Type(type="cargo")
	private Cargo reqitems;
	private long reqre;
	@Type(type="cargo")
	private Cargo awarditems;
	private long awardre;
	private String loottable;
	
	/**
	 * Konstruktor.
	 *
	 */
	public QuickQuest() {
		// EMPTY
	}

	/**
	 * Gibt die Belohnung in Waren zurueck.
	 * @return Die Belohnung
	 */
	public Cargo getAwardItems() {
		return new UnmodifiableCargo(awarditems);
	}

	/**
	 * Setzt die Belohnung in Waren.
	 * @param awarditems Die Belohnung
	 */
	public void setAwardItems(Cargo awarditems) {
		this.awarditems = new Cargo(awarditems);
	}

	/**
	 * Gibt die Belohnung in RE zurueck.
	 * @return Die Belohnung
	 */
	public long getAwardRe() {
		return awardre;
	}

	/**
	 * Setzt die Belohnung in Re.
	 * @param awardre Die Belohnung
	 */
	public void setAwardRe(long awardre) {
		this.awardre = awardre;
	}

	/**
	 * Gibt die Quests zurueck, welche vorher beendet sein muessen.
	 * @return Die benoetigten Quests
	 */
	public String getDependsOnQuests() {
		return dependsOnQuests;
	}

	/**
	 * Setzt die fuer das Quest benoetigten Quests.
	 * @param dependsOnQuests Die benoetigten Quests
	 */
	public void setDependsOnQuests(String dependsOnQuests) {
		this.dependsOnQuests = dependsOnQuests;
	}

	/**
	 * Gibt die Beschreibung zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Setzt die Beschreibung.
	 * @param desc Die Beschreibung
	 */
	public void setDescription(String desc) {
		this.description = desc;
	}

	/**
	 * Gibt zurueck, ob das Quest aktiviert ist.
	 * @return Den Aktivierungszustand
	 */
	public int getEnabled() {
		return enabled;
	}

	/**
	 * Setzt den Aktivierungszustand des Quests.
	 * @param enabled Der Aktivierungszustand
	 */
	public void setEnabled(int enabled) {
		this.enabled = enabled;
	}

	/**
	 * Gibt den Abschlusstext des Quests zurueck.
	 * @return Der Abschlusstext
	 */
	public String getFinishText() {
		return finishtext;
	}

	/**
	 * Setzt den Abschlusstext des Quests.
	 * @param finishtext Der Abschlusstext
	 */
	public void setFinishtext(String finishtext) {
		this.finishtext = finishtext;
	}

	/**
	 * Gibt die Kopfgrafik zurueck, welche in Dialogen angezeigt werden soll.
	 * @return Die Kopfgrafik
	 */
	public String getHead() {
		return head;
	}

	/**
	 * Setzt die Kopfgrafik, welche in Dialogen angezeigt werden soll.
	 * @param head Die Kopfgrafik
	 */
	public void setHead(String head) {
		this.head = head;
	}

	/**
	 * Gibt die ID des QuickQuests zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die Loottabelle des Quests zurueck.
	 * @return Die Loottabelle
	 */
	public String getLoottable() {
		return loottable;
	}

	/**
	 * Setzt die Loottabelle des Quests.
	 * @param loottable Die Loottabelle
	 */
	public void setLoottable(String loottable) {
		this.loottable = loottable;
	}

	/**
	 * Gibt zurueck, ob das Quest mehr als einmal gemacht werden kann.
	 * @return <code>true</code>, falls es mehr als einmal gemacht werden kann
	 */
	public boolean getMoreThanOnce() {
		return moreThanOnce != 0;
	}

	/**
	 * Setzt, ob das Quest mehr als einmal gemacht werden kann.
	 * @param moreThanOnce <code>true</code>, falls es mehr als einmal gemacht werden kann
	 */
	public void setMoreThanOnce(boolean moreThanOnce) {
		this.moreThanOnce = moreThanOnce ? 1 : 0;
	}

	/**
	 * Gibt den Text zurueck, welcher angezeigt wird, wenn das Quest noch nicht abgeschlossen werden kann.
	 * @return Der Text
	 */
	public String getNotYetText() {
		return notyettext;
	}

	/**
	 * Setzt den Text, welcher angezeigt wird, wenn das Quest noch nicht abgeschlossen werden kann.
	 * @param notyettext the notyettext to set
	 */
	public void setNotYetText(String notyettext) {
		this.notyettext = notyettext;
	}

	/**
	 * Gibt die Quest-ID zurueck.
	 * @return Die Quest-ID
	 */
	public String getQid() {
		return qid;
	}

	/**
	 * Setzt die Quest-ID.
	 * @param qid Die Quest-ID
	 */
	public void setQid(String qid) {
		this.qid = qid;
	}

	/**
	 * Gibt den Namen des Quests zurueck.
	 * @return Der Name
	 */
	public String getQName() {
		return qname;
	}

	/**
	 * Setzt den Namen des Quests.
	 * @param qname Der Name
	 */
	public void setQName(String qname) {
		this.qname = qname;
	}

	/**
	 * Gibt die zum Abschluss benoetigten Waren zurueck.
	 * @return Die Waren
	 */
	public Cargo getReqItems() {
		return reqitems;
	}

	/**
	 * Setzt die zum Abschluss benoetigten Waren.
	 * @param reqitems Die Waren
	 */
	public void setReqItems(Cargo reqitems) {
		this.reqitems = reqitems;
	}

	/**
	 * Gibt die zum Abschluss benoetigten RE zurueck.
	 * @return Die RE
	 */
	public long getReqRe() {
		return reqre;
	}

	/**
	 * Setzt die zum Abschluss benoetigten RE.
	 * @param reqre Die RE
	 */
	public void setReqRe(long reqre) {
		this.reqre = reqre;
	}

	/**
	 * Gibt eine kurze Beschreibung des Quests zurueck.
	 * @return Eine kurze Beschreibung
	 */
	public String getShortDesc() {
		return shortdesc;
	}

	/**
	 * Setzt eine kurze Beschreibung des Quests.
	 * @param shortdesc Die kurze Beschreibung
	 */
	public void setShortdesc(String shortdesc) {
		this.shortdesc = shortdesc;
	}

	/**
	 * Gibt den Ausgangspunkt des Quests zurueck.
	 * @return Der Ausgangspunkt
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Setzt den Ausgangspunkt des Quests.
	 * @param source Der Ausgangspunkt
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Gibt den Typ des Ausgangspunkts zurueck.
	 * @return Der Typ
	 */
	public String getSourceType() {
		return sourcetype;
	}

	/**
	 * Setzt den Typ des Ausgangspunkts.
	 * @param sourcetype Der Typ
	 */
	public void setSourceType(String sourcetype) {
		this.sourcetype = sourcetype;
	}

	/**
	 * Gibt die beim Start uebergebenen Waren zurueck.
	 * @return Die Startwaren
	 */
	public Cargo getStartItems() {
		return startitems;
	}

	/**
	 * Setzt die beim Start uebergebenen Waren.
	 * @param startitems Die Waren
	 */
	public void setStartItems(Cargo startitems) {
		this.startitems = startitems;
	}

	/**
	 * Gibt den Zielpunkt zurueck.
	 * @return Der Zielpunkt
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Setzt den Zielpunkt des Quests.
	 * @param target Der Zielpunkt
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * Gibt den Typ des Zielpunkts zurueck.
	 * @return Der Typ
	 */
	public String getTargetType() {
		return targettype;
	}

	/**
	 * Setzt den Typ des Zielpunkts.
	 * @param targettype Der Typ
	 */
	public void setTargetType(String targettype) {
		this.targettype = targettype;
	}
	
	
}
