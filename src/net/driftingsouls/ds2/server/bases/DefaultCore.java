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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * <h1>Die Standardcore in DS</h1>
 * Alle Cores, welche keine eigene Core-Klasse besitzen, werden von
 * dieser Klasse bearbeitet.
 * 
 * @author Christopher Jung
 *
 */
public class DefaultCore extends Core {
	private SQLResultRow data;
	
	/**
	 * Erstellt eine neue Core-Instanz
	 * @param row Eine SQL-Ergebniszeile mit den Core-Daten
	 */
	public DefaultCore(SQLResultRow row) {
		data = row;
		data.put("consumes", new UnmodifiableCargo(new Cargo(Cargo.Type.STRING, data.getString("consumes"))) );
		data.put("produces", new UnmodifiableCargo(new Cargo(Cargo.Type.STRING, data.getString("produces"))) );
		data.put("buildcosts", new UnmodifiableCargo(new Cargo(Cargo.Type.STRING, data.getString("buildcosts"))) );
	}

	@Override
	public int getId() {
		return data.getInt("id");
	}

	@Override
	public String getName() {
		return data.getString("name");
	}

	@Override
	public int getAstiType() {
		return data.getInt("astitype");
	}

	@Override
	public Cargo getBuildCosts() {
		return (Cargo)data.get("buildcosts");
	}

	@Override
	public Cargo getProduces() {
		return (Cargo)data.get("produces");
	}

	@Override
	public Cargo getConsumes() {
		return (Cargo)data.get("consumes");
	}

	@Override
	public int getArbeiter() {
		return data.getInt("arbeiter");
	}

	@Override
	public int getEVerbrauch() {
		return data.getInt("ever");
	}

	@Override
	public int getEProduktion() {
		return data.getInt("eprodu");
	}

	@Override
	public int getBewohner() {
		return data.getInt("bewohner");
	}

	@Override
	public int getTechRequired() {
		return data.getInt("techreq");
	}

	@Override
	public int getEPS() {
		return data.getInt("eps");
	}

}
