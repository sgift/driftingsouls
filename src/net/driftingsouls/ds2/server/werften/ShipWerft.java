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
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;
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

	/**
	 * Konstruktor
	 * @param werftdata Die Werftdaten
	 * @param werfttag Der Werfttag
	 * @param system Das System, in dem sich die Werft befindet
	 * @param owner Der Besitzer der Werft
	 * @param shipid Die ID des Schiffes, auf dem sich die Werft befindet
	 */
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

	/**
	 * Gibt die Schiffs-ID zurueck, auf dem sich die Werft befindet
	 * @return Die ID des Schiffs, auf dem sich die Werft befindet
	 */
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
		Database db = ContextMap.getContext().getDatabase();
		
		if( (this.linkedbase != 0) && !localonly ) {
			SQLResultRow shiptype = ShipTypes.getShipType( this.shipid, true );

			Cargo basecargo = new Cargo(Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id=",this.linkedbase).getString("cargo"));

			cargo.substractCargo( basecargo );
			
			ResourceList reslist = cargo.getResourceList();
			for( ResourceEntry res : reslist ) {
				if( res.getCount1() < 0 ) {
					basecargo.addResource( res.getId(), cargo.getResourceCount( res.getId() ) );
					cargo.setResource( res.getId(), 0 );
				}
			}

			// Ueberpruefen, ob wir nun zu viel Cargo auf dem Schiff haben
			long cargocount = cargo.getMass();
			
			if( cargocount > shiptype.getLong("cargo") ) {			
				Cargo shipcargo = cargo.cutCargo(shiptype.getLong("cargo"));
				basecargo.addCargo(cargo);
				cargo = shipcargo;
			}
			db.update("UPDATE bases SET cargo='",basecargo.save(),"' WHERE id=",this.linkedbase);
		}

		db.update("UPDATE ships SET cargo='",cargo.save(),"' WHERE id>0 AND id=",this.shipid);
	}
	
	@Override
	public long getMaxCargo( boolean localonly ) {
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow shiptype = ShipTypes.getShipType( shipid, true );
		long cargo = shiptype.getLong("cargo");
		if( linkedbase != 0 && !localonly ) {
			cargo += db.first("SELECT maxcargo FROM bases WHERE id=",linkedbase).getLong("maxcargo");
		}
		return cargo;
	}
	
	@Override
	public int getCrew() {
		Database db = ContextMap.getContext().getDatabase();
		
		int crew = db.first("SELECT crew FROM ships WHERE id>0 AND id=",this.shipid).getInt("crew");
		if( this.linkedbase != 0 ) {
			SQLResultRow base = db.first("SELECT bewohner,arbeiter FROM bases WHERE id=",this.linkedbase);
			crew += (base.getInt("bewohner")-base.getInt("arbeiter"));
		}
		return crew;
	}
	
	@Override
	public int getMaxCrew() {
		if( this.maxcrew == -1 ) {
			if( this.linkedbase == 0 ) {
				SQLResultRow shiptype = ShipTypes.getShipType( this.shipid, true );
				this.maxcrew = shiptype.getInt("crew");
			} else {
				this.maxcrew = 99999;
			}
		}

		return this.maxcrew;
	}
	
	@Override
	public void setCrew(int crew) {
		if( crew > this.getMaxCrew() ) {
			throw new RuntimeException("ERROR: ShipWerft.setCrew(): crew ("+crew+") gr&ouml;&szlig;er als maxcrew ("+this.getMaxCrew()+")");
		}

		Database db = ContextMap.getContext().getDatabase();		

		int shipcrew = 0;
		if( this.linkedbase != 0 ) {
			SQLResultRow shiptype = ShipTypes.getShipType( this.shipid, true );
			SQLResultRow base = db.first("SELECT bewohner,arbeiter FROM bases WHERE id=",this.linkedbase);
			int basecrew = base.getInt("bewohner")-base.getInt("arbeiter");
			shipcrew = db.first("SELECT crew FROM ships WHERE id>0 AND id=",this.shipid).getInt("crew");

			if( crew < shipcrew+basecrew ) {
				crew -= shipcrew;
				if( crew < 0 ) {
					shipcrew += crew;
					crew = 0;
				}
				basecrew = crew;
			}
			else {
				crew -= basecrew;
				if( crew < 1 ) {
					basecrew += crew-1;
					crew = 0;
				}
				
				if( crew > shiptype.getInt("crew") ) {
					basecrew += crew-shiptype.getInt("crew");
					crew = shiptype.getInt("crew");
				}
				shipcrew = crew;
			}

			int bewohner = basecrew + base.getInt("arbeiter");
			db.update("UPDATE bases SET bewohner='",bewohner,"' WHERE id=",this.linkedbase);
		} else {
			shipcrew = crew;
		}
		db.update("UPDATE ships SET crew='",shipcrew,"' WHERE id>0 AND id=",this.shipid);
	}
	
	@Override
	public int getEnergy() {
		Database db = ContextMap.getContext().getDatabase();
		
		int e = db.first("SELECT e FROM ships WHERE id>0 AND id=",this.shipid).getInt("e");

		if( this.linkedbase != 0 ) {
			e += db.first("SELECT e FROM bases WHERE id=",this.linkedbase).getInt("e");
		}

		return e;
	}
	
	@Override
	public void setEnergy(int e) {
		if( e < 0 ) {
			throw new RuntimeException("ERROR: ShipWerft.setEnergy(): e ("+e+") kleiner 0");
		}

		Database db = ContextMap.getContext().getDatabase();

		if( this.linkedbase != 0 ) {
			int basee = db.first("SELECT e FROM bases WHERE id=",this.linkedbase).getInt("e");

			e -= basee;

			if( e < 0 ) {
				basee += e;
				e = 0;
			}
			db.update("UPDATE bases SET e='",basee,"' WHERE id=",this.linkedbase);
		}

		db.update("UPDATE ships SET e='",e,"' WHERE id>0 AND id=",this.shipid);
	}
	
	@Override
	public int canTransferOffis() {
		if( this.linkedbase != 0 ) {
			Database db = ContextMap.getContext().getDatabase();
			
			int officount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s ",this.shipid,"'").getInt("count");
			int maxoffis = 1;
			SQLResultRow shiptype = ShipTypes.getShipType(this.getShipID(), true);
			if( ShipTypes.hasShipTypeFlag(shiptype, ShipTypes.SF_OFFITRANSPORT) ) {
				maxoffis = shiptype.getInt("crew");
			}
			if( officount >= maxoffis ) {
				return 0;
			} 
			return maxoffis-officount;
		}
		return 99999;
	}
	
	@Override
	public void transferOffi(int offi) {
		if( this.canTransferOffis() == 0 ) {
			throw new RuntimeException("ERROR: ShipWerft.transferOffi(): Kein Platz f&uuml;r weitere Offiziere");
		}
		Database db = ContextMap.getContext().getDatabase();
		
		if( this.linkedbase == 0 ) {
			db.update("UPDATE offiziere SET dest='s ",this.shipid,"' WHERE id=",offi);
		}
		else {
			SQLResultRow myoffi = db.first("SELECT id FROM offiziere WHERE dest='s ",this.shipid,"'");
			if( !myoffi.isEmpty() ) {
				db.update("UPDATE offiziere SET dest='b ",this.linkedbase,"' WHERE id=",offi);
			} else {
				db.update("UPDATE offiziere SET dest='s ",this.shipid,"' WHERE id=",offi);
			}
		}
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
