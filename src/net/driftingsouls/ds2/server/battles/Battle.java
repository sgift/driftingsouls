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
package net.driftingsouls.ds2.server.battles;

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Repraesentiert eine Schlacht in DS
 * @author Christopher Jung
 *
 */
public class Battle {
	//
	// Aktionskonstanten von Schiffen in der Schlacht (battles_ships->action)
	//
	/**
	 * Schiff wird am Rundenende geloescht
	 */
	public static final int BS_DESTROYED = 1;
	/**
	 * Schiff verlaesst am Rundenende die Schlacht
	 */
	public static final int BS_FLUCHT = 2;
	/**
	 * Schiff ist getroffen (Wertabgleich ships und battles_ships!)
	 */
	public static final int BS_HIT = 4;
	/**
	 * Das Schiff hat gefeuernt
	 */
	public static final int BS_SHOT = 8;
	/**
	 * Schiff tritt der Schlacht bei
	 */
	public static final int BS_JOIN = 16;
	/**
	 * Schiff befindet sich in der zweiten Reihe
	 */
	public static final int BS_SECONDROW = 32;
	/**
	 * Schiff flieht naechste Runde
	 */
	public static final int BS_FLUCHTNEXT = 64;
	/**
	 *  Schiff hat bereits eine zweite Reihe aktion in der Runde ausgefuehrt
	 */
	public static final int BS_SECONDROW_BLOCKED = 128;
	/**
	 * Waffen sind bis zum Rundenende blockiert
	 */
	public static final int BS_BLOCK_WEAPONS = 256;
	/**
	 * Waffen sind bis zum Kampfende blockiert
	 */
	public static final int BS_DISABLE_WEAPONS = 512;
	
	// Flags fuer Schlachten
	/**
	 * Erste Runde
	 */
	public static final int FLAG_FIRSTROUND = 1;
	/**
	 * Entfernt die zweite Reihe auf Seite 0
	 */
	public static final int FLAG_DROP_SECONDROW_0 = 2;
	/**
	 * Entfernt die zweite Reihe auf Seite 1
	 */
	public static final int FLAG_DROP_SECONDROW_1 = 4;
	/**
	 * Blockiert die zweite Reihe auf Seite 0
	 */
	public static final int FLAG_BLOCK_SECONDROW_0 = 8;
	/**
	 * Blockiert die zweite Reihe auf Seite 1
	 */
	public static final int FLAG_BLOCK_SECONDROW_1 = 16;
	
	private int id = 0;
	private int x = 0;
	private int y = 0;
	private int system = 0;
	private int flags = 0;
	
	/**
	 * Gibt die ID der Schlacht zurueck
	 * @return die ID
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Gibt die X-Position der Schlacht zurueck
	 * @return die X-Position
	 */
	public int getX() {
		return this.x;
	}
	
	/**
	 * Gibt die Y-Position der Schlacht zurueck
	 * @return Die Y-Position
	 */
	public int getY() {
		return this.y;
	}
	
	/**
	 * Gibt das System zurueck, in dem die Schlacht stattfindet
	 * @return das System
	 */
	public int getSystem() {
		return this.system;
	}
	
	public boolean isSecondRowStable( int side, SQLResultRow[] added ) {
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Prueft, ob die Schlacht ueber das angegebene Flag verfuegt
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Schlacht das Flag besitzt
	 */
	public boolean hasFlag( int flag ) {
		return (this.flags & flag) != 0;	
	}
	
	/**
	 * Fuegt der Schlacht das angegebene Flag hinzu
	 * @param flag Das Flag
	 */
	public void setFlag( int flag ) { 
		setFlag(flag, true);
	}
	
	/**
	 * Fuegt der Schlacht ein Flag hinzu oder entfernt eines
	 * @param flag Das Flag
	 * @param status <code>true</code>, falls das Flag hinzugefuegt werden soll. Andernfalls <code>false</code>
	 */
	public void setFlag( int flag, boolean status ) {
		if( status ) {
			this.flags |= flag;	
		}	
		else if( (this.flags & flag) != 0 ) {
			this.flags ^= flag;	
		}
	}
	
	public void addComMessage( int side, String text ) {
		throw new RuntimeException("STUB");
	}
	
	public void clearComMessageBuffer( int side ) {
		throw new RuntimeException("STUB");
	}
	
	public void getComMessageBuffer( int side ) {
		throw new RuntimeException("STUB");
	}
	
	public void setStartOwn( boolean value ) {
		throw new RuntimeException("STUB");
	}
	
	public boolean getBetakStatus( int side ) {
		throw new RuntimeException("STUB");
	}
	
	public void setBetakStatus( int side, boolean status ) {
		throw new RuntimeException("STUB");
	}
	
	public boolean isGuest() {
		throw new RuntimeException("STUB");	
	}

	public SQLResultRow getOwnShip() {
		throw new RuntimeException("STUB");
	}

	public SQLResultRow getEnemyShip() {
		throw new RuntimeException("STUB");
	}
	
	public void syncOwnShip(SQLResultRow ownShip) {
		throw new RuntimeException("STUB");
	}
	
	public void syncEnemyShip(SQLResultRow enemyShip) {
		throw new RuntimeException("STUB");
	}
	
	//---------------------------------------
	//
	// save - Eine Schlacht speichern
	//
	//----------------------------------------

	public void save( boolean ignoreinakt  ) {
		throw new RuntimeException("STUB");
	}
	
	//----------------------------------------
	//
	// create - Eine neue Schlacht erstellen
	//
	//----------------------------------------

	public void create( int id, int ownShipID, int enemyShipID ) {
		throw new RuntimeException("STUB");
	}
	
	//---------------------------------------------------------------------
	//
	// addShip - Ein Schiff oder eine Flotte zu der Schlacht hinzufuegen
	//
	//---------------------------------------------------------------------

	public void addShip( int id, int shipid ) {
		addShip(id, shipid, -1);
	}
	
	public void addShip( int id, int shipid, int forceside ) {
		throw new RuntimeException("STUB");
	}
	
	//---------------------------------------------------------------------
	//
	// load - Die Schlacht laden
	//
	//---------------------------------------------------------------------

	public void load(int battleID, int id, int ownShipID, int enemyShipID, boolean forcejoin ) {
		throw new RuntimeException("STUB");
	}
	
	//-------------------------------------------------------------------------------
	//
	// logme - Nachricht an den aktuellen Spieler ausgeben (bzw ausgabe vorbereiten)
	//
	//-------------------------------------------------------------------------------

	public void logme( String text ) {
		throw new RuntimeException("STUB");
	}

	//-------------------------------------------------------------------------------
	//
	// getOwnLog - Nachrichten an den aktuellen Spieler zurueckgeben
	//		raw -> Daten im Rohformat zurueckgeben
	//
	//-------------------------------------------------------------------------------

	public String getOwnLog( boolean raw ) {
		throw new RuntimeException("STUB");
	}
	
	//-------------------------------------------------------------------------------
	//
	// logenemy - Nachricht an den Gegner "senden" (bzw senden vorbereiten)
	//
	//-------------------------------------------------------------------------------

	public void logenemy( String text ) {
		throw new RuntimeException("STUB");
	}

	//-------------------------------------------------------------------------------
	//
	// getEnemyLog - Nachrichten an den Gegner zurueckgeben
	//		raw -> Daten im Rohformat zurueckgeben
	//
	//-------------------------------------------------------------------------------

	public String getEnemyLog( boolean raw ) {
		throw new RuntimeException("STUB");
	}
	
	//-------------------------------------------------------------------------------
	//
	// endTurn - Eine Runde beenden
	//		Gibt false zurueck, wenn die Schlacht zuende ist
	//
	//-------------------------------------------------------------------------------

	public void endTurn( boolean calledByUser ) {
		throw new RuntimeException("STUB");
	}
	
	//-------------------------------------------------------------------------------
	//
	// writeLog - Das Kampflog aktuallisieren
	//
	//-------------------------------------------------------------------------------

	public void writeLog() {
		throw new RuntimeException("STUB");
	}
	
	//-------------------------------------------------------------------------------
	//
	// endBattle - Eine Schlacht beenden
	//
	//-------------------------------------------------------------------------------

	public void endBattle( int side1points, int side2points, boolean executeScripts ) {
		throw new RuntimeException("STUB");
	}
	
	//-------------------------------------------------------------------------------
	//
	// destroyShip - Ein Schiff (und daran gedockte Schiffe) zerstoeren
	//
	//-------------------------------------------------------------------------------

	private void destroyShip( SQLResultRow ship ) {
		throw new RuntimeException("STUB");
	}
	
	//----------------------------------------------------------------------------------
	//
	// removeShip - Ein Schiff (und daran gedockte Schiffe) aus der Schlacht entfernen
	//
	//----------------------------------------------------------------------------------

	private void removeShip( SQLResultRow ship, boolean relocate ) {
		throw new RuntimeException("STUB");
	}
	
	public void addToVisibility( int userid ) {
		throw new RuntimeException("STUB");
	}
	
	public void setQuest( int quest ) {
		throw new RuntimeException("STUB");
	}
	
	public boolean isCommander( int id ) {
		return isCommander(id, -1);
	}
	
	public boolean isCommander( int id, int side ) {
		throw new RuntimeException("STUB");
	}
}
