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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Repraesentiert eine Werft auf einem Schiff in DS
 * @author Christopher Jung
 *
 */
public class ShipWerft extends WerftObject {
	private int shipid;
	private int maxcrew;
	private int linkedbase;
	private int x = -1;
	private int y = -1;
	private String name = null;

	public ShipWerft(SQLResultRow werftdata, String werfttag, int system, int owner, int shipid) {
		super( werftdata, werfttag, system, owner );
		this.shipid = shipid;
		maxcrew = -1;
		linkedbase = werftdata.getInt("linked");
	}
	
	/**
	 * Gibt zurueck, ob die Werft mit einer Basis verbunden ist
	 * @return true, falls eine Verbindung existiert
	 */
	public boolean isLinked() {
		return linkedbase != 0;
	}
	
	/**
	 * Gibt die ID der verbundenen Basis oder 0 zurueck
	 * @return ID oder 0 
	 */
	public int getLinkedBase() {
		return linkedbase;
	}

	/**
	 * Setzt die Verbindung mit einer Basis zurueck. Die Verbindung
	 * existiert folglich danach nicht mehr
	 */
	public void resetLink() {
		setLink(0);
	}
	
	/**
	 * Erstellt eine Verbindung mit einer angegebenen Basis
	 * @param baseid ID der Basis
	 */
	public void setLink( int baseid ) {
		linkedbase  = baseid;
		ContextMap.getContext().getDatabase().update("UPDATE werften SET linked='",baseid,"' WHERE id='",getWerftID(),"'");
						
	}

	public int getShipID() {
		return shipid;
	}
	
	@Override
	public int getWerftType() {
		return WerftObject.SHIP;
	}
	
	@Override
	public Cargo getCargo(boolean localonly) {
		Database db = ContextMap.getContext().getDatabase();
		
		Cargo cargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM ships WHERE id>0 AND id=",shipid).getString("cargo"));
		
		if( linkedbase != 0 && !localonly ) {
			Cargo basecargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id=",linkedbase).getString("cargo"));

			cargo.addCargo( basecargo );

		}
		return cargo;
	}
	
	@Override
	public void setCargo(Cargo cargo, boolean localonly) {
		// TODO
		throw new RuntimeException("STUB");
	}
	
	@Override
	public long getMaxCargo( boolean localonly ) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow shiptype = Ships.getShipType( shipid, true );
		long cargo = shiptype.getLong("cargo");
		if( linkedbase != 0 && !localonly ) {
			cargo += db.first("SELECT maxcargo FROM bases WHERE id=",linkedbase).getLong("maxcargo");
		}
		return cargo;
	}
	
	@Override
	public int getCrew() {
		// TODO
		Common.stub();
		return 0;
	}
	
	@Override
	public int getMaxCrew() {
		// TODO
		Common.stub();
		return 0;
	}
	
	@Override
	public void setCrew(int crew) {
		// TODO
		Common.stub();
	}
	
	@Override
	public int getEnergy() {
		// TODO
		Common.stub();
		return 0;
	}
	
	@Override
	public void setEnergy(int e) {
		// TODO
		Common.stub();
	}
	
	@Override
	public int canTransferOffis() {
		// TODO
		Common.stub();
		return 0;
	}
	
	@Override
	public void transferOffi(int offi) {
		// TODO
		Common.stub();
	}
	
	@Override
	public String getUrlBase() {
		return "./main.php?module=werft&amp;ship="+shipid;
	}

	@Override
	public String getFormHidden() {
		return "<input type=\"hidden\" name=\"ship\" value=\""+shipid+"\" />\n"+
			"<input type=\"hidden\" name=\"module\" value=\"werft\" />\n";
	}

	@Override
	public int getX() {
		if( x == -1 ) {
			x = ContextMap.getContext().getDatabase().first("SELECT x FROM ships WHERE id>0 AND id=",shipid).getInt("x");
		}
		return x;
	}

	@Override
	public int getY() {
		if( y == -1 ) {
			y = ContextMap.getContext().getDatabase().first("SELECT y FROM ships WHERE id>0 AND id=",shipid).getInt("y");
		}
		return y;
	}
	
	@Override
	public String getName() {
		if( name == null ) {
			name = ContextMap.getContext().getDatabase().first("SELECT name FROM ships WHERE id>0 AND id=",shipid).getString("name");
		}
		return name;
	}
	
	@Override
	public int finishBuildProcess() {
		int id = super.finishBuildProcess();
		if( (id != 0) && (getType() == 2) ) {
			Ships.destroy(shipid);
		}
		
		return id;
	}
	
	@Override
	public boolean repairShip(SQLResultRow ship, boolean testonly) {
		boolean result = super.repairShip(ship, testonly);
		
		Ships.recalculateShipStatus(getShipID());
		return result;
	}
	
	@Override
	public void removeModule( SQLResultRow ship, int slot ) {
		super.removeModule( ship, slot );
		
		Ships.recalculateShipStatus(getShipID());
	}
	
	@Override
	public void addModule( SQLResultRow ship, int slot, int item ) {
		super.addModule( ship, slot, item );
		
		Ships.recalculateShipStatus(getShipID());
	}
	
	@Override
	public boolean dismantleShip(int dismantle, boolean testonly) {	
		boolean result = super.dismantleShip(dismantle, testonly);
		
		Ships.recalculateShipStatus(getShipID());
		return result;
	}
	
	@Override
	public boolean buildShip( int build, int item, boolean testonly ) {
		boolean result = super.buildShip(build, item, testonly);
		
		Ships.recalculateShipStatus(getShipID());
		return result;
	}
}
