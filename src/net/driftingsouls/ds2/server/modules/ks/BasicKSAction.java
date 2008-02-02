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
package net.driftingsouls.ds2.server.modules.ks;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AngriffController;

/**
 * Basisklasse fuer KS-Aktionen
 * @author Christopher Jung
 *
 */
public abstract class BasicKSAction {
	/**
	 * Das Ergebnis "Ok"
	 */
	public static final int RESULT_OK = 1;
	/**
	 * Das Ergebnis "nicht kritischer Fehler"
	 */
	public static final int RESULT_ERROR = 0;
	/**
	 * Das Ergebnis "kritischer Fehler"
	 */
	public static final int RESULT_HALT = -1;
	
	private boolean requireCommander;
	private boolean requireActive;
	private int requireAP;
	private boolean requireOwnShipReady;
	private AngriffController controller;
	
	/**
	 * Konstruktor
	 *
	 */
	public BasicKSAction() {
		this.controller = null;
		
		this.requireCommander(true);
		this.requireActive(true);
		this.requireAP(0);
		this.requireOwnShipReady(false);
	}
	
	/**
	 * Setzt den KS-Controller
	 * @param controller Der KS-Controller
	 */
	public void setController(AngriffController controller) {
		this.controller = controller;	
	}
	
	/**
	 * Gibt den assoziierten KS-Controller zurueck
	 * @return Der KS-Controller
	 */
	public AngriffController getController() {
		return this.controller;	
	}
	
	protected void requireCommander( boolean value ) {
		this.requireCommander = value;
	}
	
	protected void requireActive( boolean value ) {
		this.requireActive = value;
	}
	
	protected void requireAP( int value ) {
		this.requireAP = value;
	}
	
	protected void requireOwnShipReady( boolean value ) {
		this.requireOwnShipReady = value;
	}
	
	// TODO: auch bei der validierung sollten requireAP usw gecheckt werden
	/**
	 * Ueberprueft, ob die AKtion ausgefuert werden kann in Kontext der angegebenen Schlacht
	 * @param battle Die Schlacht
	 * @return Das Ergebnis
	 */
	public int validate( Battle battle ) {
		return RESULT_OK;
	} 
	
	/**
	 * Fueht die Aktion aus
	 * @param battle Die Schlacht in deren Kontext die Aktion ausgefuert werden soll
	 * @return Das Ergebnis
	 */
	public int execute( Battle battle ) {
		User user = (User)ContextMap.getContext().getActiveUser();
		
		if( this.requireCommander ) {
			if( !battle.isCommander(user.getId(), battle.getOwnSide()) ) {
				battle.logme( "Sie k&ouml;nnen diese Aktion nicht durchf&uuml;hren, da sie ihre Seite nicht kommandieren\n" );
				return RESULT_ERROR;
			}
		}
		
		if( this.requireActive ) {
			if( battle.isReady(battle.getOwnSide()) ) {
				battle.logme( "Sie haben ihren Zug bereits beendet\n" );
				return RESULT_ERROR;
			}
		}
	
		if( this.requireAP > 0 ) {
			if( battle.getPoints(battle.getOwnSide()) < this.requireAP ) {
				battle.logme( "Nicht genug Aktionspunkte um die Aktion auszuf&uuml;hren" );
				return RESULT_ERROR;
			}
		}
		
		if( this.requireOwnShipReady ) {
			SQLResultRow ownShip = battle.getOwnShip();
	
			if( (ownShip.getInt("action") & Battle.BS_FLUCHT) != 0 ) {
				battle.logme( "Das Schiff flieht gerade\n" );
				return RESULT_ERROR;
			}
	
			if( (ownShip.getInt("action") & Battle.BS_JOIN) != 0 ) {
				battle.logme( "Das Schiff tritt erst gerade der Schlacht bei\n" );
				return RESULT_ERROR;
			}	
		}
		
		return RESULT_OK;
	}
}
