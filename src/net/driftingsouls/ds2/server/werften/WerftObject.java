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
package net.driftingsouls.ds2.server.werften;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Basisklasse fuer alle Werfttypen in DS
 * @author Christopher Jung
 *
 */
public abstract class WerftObject extends DSObject {
	protected static final int BUILDING = 0;
	protected static final int SHIP = 1;
	
	protected int werftid = 0;
	protected String werfttag = "";
	protected int system = 0;
	protected int owner = 0;
	protected int oneway = 0;
	private int building = 0;
	private int buildItem = -1;
	private int remaining = 0;
	private boolean buildFlagschiff = false;
	private int type = 0;
	
	protected WerftObject( SQLResultRow werftdata, String werfttag, int system, int owner) {
		this.werftid = werftdata.getInt("id");
		this.werfttag = werfttag;
		this.system = system;
		this.owner = owner;
		this.type = werftdata.getInt("type");
		this.building = werftdata.getInt("building");
		this.buildItem = werftdata.getInt("item");
		this.remaining = werftdata.getInt("remaining");
		this.buildFlagschiff = werftdata.getBoolean("flagschiff");
	}
	
	/**
	 * Gibt zurueck, ob in der Werft im Moment gebaut wird
	 * @return <code>true</code>, falls gebaut wird
	 */
	public boolean isBuilding() {
		return building != 0;
	}
	
	/**
	 * Gibt den im Moment gebauten Schiffstyp zurueck
	 * @return Array mit Schiffstypdaten oder <code>null</code>
	 */
	public SQLResultRow getBuildShipType() {
		if( building > 0 ) {
			return Ships.getShipType(building, false);
		}
		
		return null;
	}
	
	/**
	 * Dekrementiert die verbliebene Bauzeit um 1
	 */
	public void decRemainingTime() {
		remaining--;
		ContextMap.getContext().getDatabase().update("UPDATE werften SET remaining=remaining-1 WHERE id=",getWerftID());
	}
	
	/**
	 * Inkrementiert die verbliebene Bauzeit um 1
	 */
	public void incRemainingTime() {
		remaining++;
		ContextMap.getContext().getDatabase().update("UPDATE werften SET remaining=remaining+1 WHERE id=",getWerftID());
	}
	
	/**
	 * Liefert die noch verbleibende Bauzeit
	 * @return verbleibende Bauzeit
	 */
	public int getRemainingTime() {
		return remaining;
	}
	
	/**
	 * Gibt das fuer den Bau benoetigte Item zurueck.
	 * Falls kein Item benoetigt wird, wird -1 zurueckgegeben.
	 * @return Item-ID oder -1
	 */
	public int getRequiredItem() {
		return buildItem;
	}
	
	/**
	 * Gibt den Typ der Werft zurueck
	 * @return Typ der Werft
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Beendet den Bauprozess des aktuell gebauten Schiffes erfolgreich.
	 * Sollte dies nicht moeglich sein, wird 0 zurueckgegeben.
	 * 
	 * @return die ID des gebauten Schiffes oder 0
	 */
	public int finishBuildProcess() {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Gibt zurueck, ob alle Voraussetzungen fuer eine Weiterfuehrung
	 * des Bauprozesses erfuellt sind. Wenn nichts gebaut wird,
	 * wird ebenfalls true zurueckgegeben.
	 * 
	 * @return <code>true</code>, falls alle Voraussetzungen erfuellt sind
	 */
	public boolean isBuildContPossible() {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Bricht das aktuelle Bauvorhaben ab
	 */
	public void cancelBuild() {
		Database db = ContextMap.getContext().getDatabase();
		
		db.update("UPDATE werften SET building='0',remaining='0',item='-1',flagschiff='0' WHERE id='",getWerftID(),"'");
		building = 0;
		remaining = 0;
		buildItem = 0;
		buildFlagschiff = false;
	}
	
	public void setOneWayFlag(int flag) {
		oneway = flag;
	}

	public int getOneWayFlag() {
		return oneway;
	}

	public int getWerftID() {
		return werftid;
	}

	public String getWerftTag() {
		return werfttag;
	}

	public int getOwner() {
		return owner;
	}

	public int getSystem() {
		return system;
	}
	
	public abstract int getWerftType();

	public abstract Cargo getCargo(boolean localonly);

	/*
		Den Cargo wieder in die DB schreiben
	*/
	public abstract void setCargo(Cargo cargo, boolean localonly);

	public abstract long getMaxCargo(boolean localonly);

	public abstract int getCrew();

	public abstract int getMaxCrew();

	public abstract void setCrew(int crew);

	public abstract int getEnergy();

	/**
		Neuen Energiestatus in die DB schreiben
		 -> Annahme: Es kann nur weniger Energie werden - niemals mehr
	*/
	public abstract void setEnergy(int e);

	public abstract int canTransferOffis();

	public abstract void transferOffi(int offi);

	public abstract String getUrlBase();
	
	public abstract String getFormHidden();

	public abstract int getX();

	public abstract int getY();
	
	public abstract String getName();
	
	public int getSize() {
		return 0;	
	}
	
	/**
	 * Entfernt ein Item-Modul aus einem Schiff. Das Item-Modul
	 * befindet sich anschiessend auf der Werft.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param ship Array mit Schiffsdaten
	 * @param slot Modulslot, aus dem das Modul ausgebaut werden soll
	 * 
	 */
	public void removeModule( SQLResultRow ship, int slot ) {
		// TODO
		throw new RuntimeException("STUB");
	} 
	
	//--------------------------------------------------------------------------------------------------------------
	
	/**
	 * Fuegt einem Schiff ein Item-Modul hinzu. Das Item-Modul
	 * muss auf der Werft vorhanden sein.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param ship Array mit den Daten des Schiffes
	 * @param slot Der Slot, in den das Modul eingebaut werden soll
	 * @param item Die ID des einzubauenden Item-Moduls
	 * 
	 */
	public void addModule( SQLResultRow ship, int slot, int item ) {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Berechnet den Cargo, den man beim Demontieren eines Schiffes zurueckbekommt. Er entspricht somit
	 * dem reinen Schrottwert des Schiffes :)
	 * Die aktuell geladenen Waren des Schiffes sind nicht teil des Cargos!
	 * @param ship Array mit Daten des Schiffes
	 * 
	 * @return Cargo mit den Resourcen
	 */
	public Cargo getDismantleCargo( SQLResultRow ship ) {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Demontiert ein Schiff. Es wird dabei nicht ueberprueft, ob sich Schiff
	 * und Werft im selben Sektor befinden, ob das Schiff in einem Kampf ist usw sondern
	 * nur das demontieren selbst.{@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param dismantle Die ID des zu demontierenden Schiffes
	 * @param testonly Soll nur geprueft (true) oder wirklich demontiert werden (false)?
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean dismantleShip(int dismantle, boolean testonly) {	
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Berechnet die Reparaturkosten fuer ein Schiff
	 * @param ship Array mit Schiffsdaten
	 * 
	 * @return array( Energiekosten, CCargo mit Warenkosten ) 
	 */
	public Object[] getRepairCosts( SQLResultRow ship ) {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Repariert ein Schiff auf einer Werft.
	 * Es werden nur Dinge geprueft, die unmittelbar mit dem Repariervorgang selbst
	 * etwas zu tun haben. Die Positionen von Schiff und Werft usw werden jedoch nicht gecheckt.
	 * {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 *
	 * @param ship Array mit Schiffsdaten
	 * @param testonly Soll nur getestet (true) oder auch wirklich repariert (false) werden?
	 * 
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean repairShip(SQLResultRow ship, boolean testonly) {
		// TODO
		throw new RuntimeException();
	}
	
	/**
	 * Liefert die Liste aller theoretisch baubaren Schiffe auf dieser Werft.
	 * Das vorhanden sein von Resourcen wird hierbei nicht beruecksichtigt.
	 * @return array mit Schiffsbaudaten (ships_baubar) sowie 
	 * 			'_item' => array( ('local' | 'ally'), $resourceid) oder '_item' => false
	 * 			zur Bestimmung ob und wenn ja welcher Bauplan benoetigt wird zum bauen
	 */
	public SQLResultRow[] getBuildShipList() {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Liefert die Schiffsbaudaten zu einer Kombination aus Schiffsbau-ID und/oder Item.
	 * Die Baukosten werden falls notwendig angepasst (linear ansteigende Kosten).
	 * Wenn keine passenden Schiffsbaudaten generiert werden koennen wird ein leeres
	 * Schiffsbaudatenarray zurueckgegeben. {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param build Die Schiffsbau-ID
	 * @param item Die Item-ID
	 * 
	 * @return array( schiffsbaudaten, Ausgabestring )
	 */
	public SQLResultRow getShipBuildData( int build, int item ) {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	/**
	 * Baut ein Schiff in der Werft auf Basis der angegebenen Schiffbau-ID und der
	 * angegebenen Item-ID (Bauplan). {@link DSObject#MESSAGE} enthaelt die Hinweistexte
	 * 
	 * @param build Schiffbau-ID
	 * @param item Item-ID
	 * @param testonly Soll nur getestet (true) oder wirklich gebaut (false) werden?
	 * @return true, wenn kein Fehler aufgetreten ist
	 */
	public boolean buildShip( int build, int item, boolean testonly ) {
		// TODO
		throw new RuntimeException("STUB");
	}
}
