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
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Repraesentiert eine Werft auf einer Basis in DS
 * @author Christopher Jung
 *
 */
public class BaseWerft extends WerftObject {
	private int baseid;
	private int fieldid;
	private int x = -1;
	private int y = -1;
	private String name = null;
	private Integer size = null;

	/**
	 * Konstruktor
	 * @param werftdata Die Werftdaten
	 * @param werfttag Der Werfttag
	 * @param system Das System in dem sich die Werft befindet
	 * @param owner Der Besitzer der Werft
	 * @param baseid Die ID der Basis, auf dem die Werft steht
	 * @param fieldid Die Feld-ID auf der Basis oder -1, falls das Feld unbekannt ist
	 */
	public BaseWerft(SQLResultRow werftdata, String werfttag, int system, int owner, int baseid, int fieldid) {
		super( werftdata, werfttag, system, owner );
		this.baseid = baseid;
		this.fieldid = fieldid;
	}
	
	/**
	 * Gibt die ID der Basis zurueck, auf dem die Werft steht
	 * @return Die ID der Basis
	 */
	public int getBaseID() {
		return this.baseid;
	}
	
	/**
	 * Gibt die Feld-ID zurueck, auf dem die Werft steht. Sollte
	 * die Feld-ID unbekannt sein, so wird <code>-1</code> zurueckgegeben
	 * @return Die Feld-ID oder -1
	 */
	public int getBaseField() {
		return this.fieldid;
	}

	@Override
	public int getWerftType() {
		return WerftObject.BUILDING;
	}
	
	@Override
	public Cargo getCargo(boolean localonly) {
		Database db = ContextMap.getContext().getDatabase();
		
		Cargo cargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id>0 AND id=",baseid).getString("cargo"));
	
		return cargo;
	}
	
	@Override
	public void setCargo(Cargo cargo, boolean localonly) {
		Database db = ContextMap.getContext().getDatabase();
		db.update("UPDATE bases SET cargo='",cargo.save(),"' WHERE id=",this.baseid);
	}
	
	@Override
	public long getMaxCargo( boolean localonly ) {
		Database db = ContextMap.getContext().getDatabase();
		return db.first("SELECT maxcargo FROM bases WHERE id=",this.baseid).getLong("maxcargo");
	}
	
	@Override
	public int getCrew() {
		Database db = ContextMap.getContext().getDatabase();
		SQLResultRow base = db.first("SELECT bewohner,arbeiter FROM bases WHERE id=",this.baseid);
		return (base.getInt("bewohner")-base.getInt("arbeiter"));
	}
	
	@Override
	public int getMaxCrew() {
		return 9999;
	}
	
	@Override
	public void setCrew(int crew) {
		Database db = ContextMap.getContext().getDatabase();
		
		int arbeiter = db.first("SELECT arbeiter FROM bases WHERE id=",this.baseid).getInt("arbeiter");

		int bewohner = crew + arbeiter;
		db.update("UPDATE bases SET bewohner='",bewohner,"' WHERE id=",this.baseid);
	}
	
	@Override
	public int getEnergy() {
		Database db = ContextMap.getContext().getDatabase();
		return db.first("SELECT e FROM bases WHERE id=",this.baseid).getInt("e");
	}
	
	@Override
	public void setEnergy(int e) {
		if( e < 0 ) {
			throw new RuntimeException("ERROR: BaseWerft.setEnergy(): e ("+e+") kleiner 0");
		}

		Database db = ContextMap.getContext().getDatabase();
		db.update("UPDATE bases SET e='",e,"' WHERE id=",this.baseid);
	}
	
	@Override
	public int canTransferOffis() {
		return 9999;
	}
	
	@Override
	public void transferOffi(int offi) {
		Database db = ContextMap.getContext().getDatabase();
		db.update("UPDATE offiziere SET dest='b ",this.baseid,"' WHERE id=",offi);
	}
	
	@Override
	public String getUrlBase() {
		if( fieldid == -1 ) {
			throw new RuntimeException("BaseWerft: Werftfeld nicht gesetzt");
		}
		return "./main.php?module=building&amp;col="+baseid+"&amp;field="+fieldid;
	}

	@Override
	public String getFormHidden() {
		if( fieldid == -1 ) {
			throw new RuntimeException("BaseWerft: Werftfeld nicht gesetzt");
		}
		return "<input type=\"hidden\" name=\"col\" value=\""+baseid+"\" />\n"+
			"<input type=\"hidden\" name=\"field\" value=\""+fieldid+"\" />\n"+
			"<input type=\"hidden\" name=\"module\" value=\"building\" />\n";
	}

	@Override
	public int getX() {
		if( x == -1 ) {
			x = ContextMap.getContext().getDatabase().first("SELECT x FROM bases WHERE id>0 AND id=",baseid).getInt("x");
		}
		return x;
	}

	@Override
	public int getY() {
		if( y == -1 ) {
			y = ContextMap.getContext().getDatabase().first("SELECT y FROM bases WHERE id>0 AND id=",baseid).getInt("y");
		}
		return y;
	}
	
	@Override
	public String getName() {
		if( name == null ) {
			name = ContextMap.getContext().getDatabase().first("SELECT name FROM bases WHERE id>0 AND id=",baseid).getString("name");
		}
		return name;
	}
	
	@Override
	public int getSize() {
		if( size == null ) {
			size = ContextMap.getContext().getDatabase().first("SELECT size FROM bases WHERE id>0 AND id=",baseid).getInt("size");
		}
		return size;
	}
}
