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
package net.driftingsouls.ds2.server.config.items.effects;

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.apache.commons.lang.StringUtils;

/**
 * Item-Effekt "Schiffsbauplan".
 * @author Christopher Jung
 *
 */
public class IEDraftShip extends ItemEffect {
	private int shiptype = 0;
	private int race = 0;
	private boolean flagschiff = false;
	private Cargo buildcosts = null;
	private int crew = 0;
	private int e = 0;
	private int dauer = 0;
	private int[] techs = null;
	private int werftslots = 1;
	
	protected IEDraftShip(boolean allyEffect) {
		super(ItemEffect.Type.DRAFT_SHIP, allyEffect);
	}
	
	/**
	 * Gibt den Typ des baubaren Schiffs zurueck.
	 * @return Der Typ
	 */
	public int getShipType() {
		return shiptype;
	}
	
	/**
	 * Gibt die Rasse zurueck, fuer die das Schiff baubar ist.
	 * @return Die Rasse
	 */
	public int getRace() {
		return race;
	}
	
	/**
	 * Gibt die Baukosten zurueck.
	 * @return Die Baukosten
	 */
	public Cargo getBuildCosts() {
		return buildcosts;
	}
	
	/**
	 * Gibt die zum Bau benoetigte Crew zurueck.
	 * @return Die benoetigte Crew
	 */
	public int getCrew() {
		return crew;
	}
	
	/**
	 * Gibt die zum Bau benoetigte Energie zurueck.
	 * @return die benoetigte Energie
	 */
	public int getE() {
		return e;
	}
	
	/**
	 * Gibt zurueck, ob ein Flagschiff gebaut wird.
	 * @return <code>true</code>, wenn es ein Flagschiff ist
	 */
	public boolean isFlagschiff() {
		return flagschiff;
	}
	
	/**
	 * Gibt die Baudauer zurueck.
	 * @return Die Baudauer
	 */
	public int getDauer() {
		return dauer;
	}
	
	/**
	 * Gibt die benoetigte Tech mit dem Index zurueck.
	 * @param index Der Index (1-3)
	 * @return Die Tech
	 */
	public int getTechReq(int index) {
		index--;
		if( (index < 0) || (index >= techs.length) ) {
			return 0;
		}
		return techs[index];
	}
	
	/**
	 * Gibt die Werftslots zurueck, die zum Bau des Schiffes erforderlich sind.
	 * @return Die Werftslots
	 */
	public int getWerftSlots() {
		return werftslots;
	}
	
	/**
	 * Laedt einen Effect aus einem String.
	 * @param effectString Der Effect als String
	 * @return Der Effect
	 * @throws IllegalArgumentException falls der Effect nicht richtig geladen werden konnte
	 */
	public static ItemEffect fromString(String effectString) throws IllegalArgumentException {
		IEDraftShip draft = new IEDraftShip(false);
		
		String[] effects = StringUtils.split(effectString, "&");
		draft.shiptype = Integer.parseInt(effects[0]);
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		ShipType shipType = (ShipType)db.get(ShipType.class, draft.shiptype);
		if( shipType == null ) {
			throw new IllegalArgumentException("Illegaler Schiffstyp '"+draft.shiptype+"' im Item-Effekt 'Schiffsbauplan'");
		}
		
		draft.race = Integer.parseInt(effects[1]);
		draft.flagschiff = effects[2].equals("true");
		draft.crew = Integer.parseInt(effects[3]);
		draft.e = Integer.parseInt(effects[4]);
		draft.dauer = Integer.parseInt(effects[5]);
		draft.werftslots = Integer.parseInt(effects[6]);
		draft.buildcosts = new UnmodifiableCargo(new Cargo(Cargo.Type.AUTO, effects[7]));
		String[] techs = StringUtils.split(effects[8], ",");
		draft.techs = new int[techs.length];
		for(int i = 0; i < techs.length; i++) {
			draft.techs[i] = Integer.parseInt(techs[i]);
		}
		
		return draft;
	}
	
	/**
	 * Laedt ein ItemEffect aus dem angegebenen Context.
	 * @param context Der Context
	 * @return Der Effect
	 */
	public static ItemEffect fromContext(Context context) {
		IEDraftShip draft = new IEDraftShip(false);
		
		draft.shiptype = context.getRequest().getParameterInt("shiptype");
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		ShipType shipType = (ShipType)db.get(ShipType.class, draft.shiptype);
		if( shipType == null) {
			return null;
		}
		
		draft.race = context.getRequest().getParameterInt("race");
		draft.flagschiff = context.getRequest().getParameterString("flagschiff").equals("true");
		draft.crew = context.getRequest().getParameterInt("crew");
		draft.e = context.getRequest().getParameterInt("energie");
		draft.dauer = context.getRequest().getParameterInt("dauer");
		draft.werftslots = context.getRequest().getParameterInt("werftslots");
		draft.buildcosts = new UnmodifiableCargo(new Cargo(Cargo.Type.AUTO, context.getRequest().getParameterString("buildcosts")));
		String[] techs = StringUtils.split(context.getRequest().getParameterString("techs"), ",");
		draft.techs = new int[techs.length];
		for(int i = 0; i < techs.length; i++) {
			draft.techs[i] = Integer.parseInt(techs[i]);
		}
		
		return draft;
	}
	
	/**
	 * Gibt das passende Fenster fuer das Adminmenue aus.
	 * @param echo Der Writer des Adminmenues
	 * @throws IOException Exception falls ein fehler auftritt
	 */
	@Override
	public void getAdminTool(Writer echo) throws IOException {
		
		echo.append("<input type=\"hidden\" name=\"type\" value=\"draft-ship\" >");
		echo.append("<tr><td class=\"noBorderS\">SchiffsId: </td><td><input type=\"text\" name=\"shiptype\" value=\"" + getShipType() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Rasse: </td><td><input type=\"text\" name=\"race\" value=\"" + getRace() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Flagschiff (true/false): </td><td><input type=\"text\" name=\"flagschiff\" value=\"" + isFlagschiff() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Benoetigte Crew: </td><td><input type=\"text\" name=\"crew\" value=\"" + getCrew() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Benoetigte Energie: </td><td><input type=\"text\" name=\"energie\" value=\"" + getE() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Benoetigte Dauer: </td><td><input type=\"text\" name=\"dauer\" value=\"" + getDauer() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Benoetigte Werftslots: </td><td><input type=\"text\" name=\"werftslots\" value=\"" + getWerftSlots() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Baukosten: </td><td><input type=\"text\" name=\"buildcosts\" value=\"" + getBuildCosts().save() + "\"></td></tr>\n");
		String techs = Common.implode(",", this.techs);
		echo.append("<tr><td class=\"noBorderS\">Ben�tigte Technologien: </td><td><input type=\"text\" name=\"techs\" value=\"" + techs + "\"></td></tr>\n");
	}

	/**
	 * Konvertiert die Baudaten des Itemeffekts in ein {@link ShipBaubar}-Objekt.
	 * @return Das neu erstellte Objekt
	 */
	public ShipBaubar toShipBaubar()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		ShipType type = (ShipType)db.get(ShipType.class, this.shiptype);

		ShipBaubar baudaten = new ShipBaubar(type);
		baudaten.setCosts(this.buildcosts);
		baudaten.setCrew(this.crew);
		baudaten.setDauer(this.dauer);
		baudaten.setEKosten(this.e);
		baudaten.setRace(this.race);
		baudaten.setRes1(this.getTechReq(1));
		baudaten.setRes2(this.getTechReq(2));
		baudaten.setRes3(this.getTechReq(3));
		baudaten.setWerftSlots(this.werftslots);
		baudaten.setFlagschiff(this.flagschiff);

		return baudaten;
	}
	
	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		String itemstring = "draft-ship:" + getShipType() + "&" + getRace() + "&" + isFlagschiff() + "&" + getCrew() + "&" + getE() + "&" + getDauer() + "&" + getWerftSlots() + "&" + getBuildCosts().save();
		String techs = Common.implode(",", this.techs);
		if( techs.equals("")) {
			techs = ",";
		}
		itemstring = itemstring + "&" + techs;
		
		return itemstring;
	}
}
