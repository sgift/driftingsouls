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

	public BaseWerft(SQLResultRow werftdata, String werfttag, int system, int owner, int baseid, int fieldid) {
		super( werftdata, werfttag, system, owner );
		this.baseid = baseid;
		this.fieldid = fieldid;
	}
	
	public int getBaseID() {
		return this.baseid;
	}
	
	public int getBaseField() {
		return this.fieldid;
	}

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
		// TODO
		throw new RuntimeException("STUB");
	}
	
	@Override
	public long getMaxCargo( boolean localonly ) {
		// TODO
		Common.stub();
		return 0;
	}
	
	@Override
	public int getCrew() {
		// TODO
		Common.stub();
		return 0;
	}
	
	@Override
	public int getMaxCrew() {
		return 9999;
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
		return 9999;
	}
	
	@Override
	public void transferOffi(int offi) {
		// TODO
		Common.stub();
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
		return "<input type=\"hidden\" name=\"ship\" value=\""+baseid+"\" />\n"+
			"<input type=\"hidden\" name=\"field\" value=\""+fieldid+"\" />\n"+
			"<input type=\"hidden\" name=\"module\" value=\"werft\" />\n";
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
