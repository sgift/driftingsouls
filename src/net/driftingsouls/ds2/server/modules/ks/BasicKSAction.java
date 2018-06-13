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
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.AngriffController;

import java.io.IOException;

/**
 * Basisklasse fuer KS-Aktionen.
 * @author Christopher Jung
 *
 */
public abstract class BasicKSAction {
	/**
	 * Das Ergebnis einer KS-Aktion. Dies entscheidet darueber wie das KS nach der Aktion weiter
	 * verfaehrt.
	 *
	 */
	public enum Result
	{
		/**
		 * Das Ergebnis "Ok".
		 */
		OK,
		/**
		 * Das Ergebnis "nicht kritischer Fehler".
		 */
		ERROR,
		/**
		 * Das Ergebnis "kritischer Fehler".
		 */
		HALT
	}
	
	private boolean requireCommander;
	private boolean requireActive;
	private boolean requireOwnShipReady;
	private AngriffController controller;
    private User commander;
	
	/**
	 * Konstruktor.
	 *
	 */
	public BasicKSAction()
	{
		this.controller = null;
		
		this.requireCommander(true);
		this.requireActive(true);
		this.requireOwnShipReady(false);
        this.commander = null;
	}
    
    public BasicKSAction(User user)
    {
        this.commander = user;
    }
	
	/**
	 * Setzt den KS-Controller.
	 * @param controller Der KS-Controller
	 */
	public void setController(AngriffController controller)
	{
		this.controller = controller;	
	}
	
	/**
	 * Gibt den assoziierten KS-Controller zurueck.
	 * @return Der KS-Controller
	 */
	public AngriffController getController()
	{
		return this.controller;	
	}
	
	protected void requireCommander( boolean value )
	{
		this.requireCommander = value;
	}
	
	protected void requireActive( boolean value )
	{
		this.requireActive = value;
	}
	
	protected void requireOwnShipReady( boolean value )
	{
		this.requireOwnShipReady = value;
	}
	
	// TODO: auch bei der validierung sollten requireAP usw gecheckt werden
	/**
	 * Ueberprueft, ob die AKtion ausgefuert werden kann in Kontext der angegebenen Schlacht.
	 * @param battle Die Schlacht
	 * @return Das Ergebnis
	 */
	public Result validate( Battle battle )
	{
		return Result.OK;
	} 
	
	/**
	 * Fuehrt die Aktion aus.
	 *
	 * @param t Die verwendete Template-Engine
	 * @param battle Die Schlacht in deren Kontext die Aktion ausgefuert werden soll
	 * @return Das Ergebnis
	 * @throws IOException 
	 */
	public Result execute(TemplateEngine t, Battle battle) throws IOException
	{
        User user = this.commander;
        if(user == null)
        {
	        user = (User)ContextMap.getContext().getActiveUser();
        }
		
		if( this.requireCommander ) {
			if( !battle.isCommander(user, battle.getOwnSide()) ) {
				battle.logme( "Sie k&ouml;nnen diese Aktion nicht durchf&uuml;hren, da sie ihre Seite nicht kommandieren\n" );
				return Result.ERROR;
			}
		}
		
		if( this.requireActive ) {
			if( battle.isReady(battle.getOwnSide()) ) {
				battle.logme( "Sie haben ihren Zug bereits beendet\n" );
				return Result.ERROR;
			}
		}
		
		if( this.requireOwnShipReady ) {
			BattleShip ownShip = battle.getOwnShip();
	
			if( ownShip.hasFlag(BattleShipFlag.FLUCHT) ) {
				battle.logme( "Das Schiff flieht gerade\n" );
				return Result.ERROR;
			}
	
			if( ownShip.hasFlag(BattleShipFlag.JOIN) ) {
				battle.logme( "Das Schiff tritt erst gerade der Schlacht bei\n" );
				return Result.ERROR;
			}	
		}
		
		return Result.OK;
	}
}
