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
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * <h1>Das Standardgebaeude in DS</h1>
 * Alle Gebaeude, die ueber keine eigene Gebaeudeklasse verfuegen, werden von dieser
 * Gebaeudeklasse bearbeitet
 * @author Christopher Jung
 *
 */
class DefaultBuilding extends Building {
	private SQLResultRow data;
	
	/**
	 * Erstellt eine neue Gebaeude-Instanz
	 * @param row Die SQL-Ergebniszeile mit den Gebaeudedaten
	 */
	public DefaultBuilding(SQLResultRow row) {
		data = row;
		data.put("consumes", new UnmodifiableCargo(new Cargo(Cargo.Type.STRING, data.getString("consumes"))) );
		data.put("produces", new UnmodifiableCargo(new Cargo(Cargo.Type.STRING, data.getString("produces"))) );
		data.put("buildcosts", new UnmodifiableCargo(new Cargo(Cargo.Type.STRING, data.getString("buildcosts"))) );
	}

	@Override
	public int getBewohner() {
		return data.getInt("bewohner");
	}

	@Override
	public int getArbeiter() {
		return data.getInt("arbeiter");
	}

	@Override
	public void build(Base base) {
		// EMPTY
	}

	@Override
	public void cleanup(Context context, Base base) {
		// EMPTY
	}

	@Override
	public String modifyStats(Base base, Cargo stats) {
		// EMPTY
		return "";
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		return status == 1;
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		return "";
	}

	@Override
	public boolean printHeader() {
		return true;
	}

	@Override
	public boolean classicDesign() {
		return false;
	}

	@Override
	public String output(TemplateEngine t, Base base, int field, int building) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Verbraucht:<br />\n");
		buffer.append("<div align=\"center\">\n");
		
		boolean entry = false;
		ResourceList reslist = getConsumes().getResourceList();
		for( ResourceEntry res : reslist ) {
			buffer.append("<img src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+" ");
			entry = true;
		}
	
		if( getEVerbrauch() > 0 ) {
			buffer.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/energie.gif\" alt=\"\" />"+getEVerbrauch()+" ");
			entry = true;
		}
		if( !entry ) buffer.append("-");
		
		buffer.append("</div>\n");
		
		buffer.append("Produziert:<br />\n");
		buffer.append("<div align=\"center\">\n");
		
		entry = false;
		reslist = getProduces().getResourceList();
		for( ResourceEntry res : reslist ) {
			buffer.append("<img src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+" ");
			entry = true;
		}
		
		if( getEProduktion() > 0 ) {
			buffer.append("<img src=\""+Configuration.getSetting("URL")+"data/interface/energie.gif\" alt=\"\" />"+getEProduktion());
			entry = true;
		}
	
		if( !entry ) buffer.append("-");
		buffer.append("</div><br />\n");
		return buffer.toString();
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
	public String getPicture() {
		return data.getString("picture");
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
	public int getEVerbrauch() {
		return data.getInt("ever");
	}

	@Override
	public int getEProduktion() {
		return data.getInt("eprodu");
	}

	@Override
	public int getTechRequired() {
		return data.getInt("techreq");
	}

	@Override
	public int getEPS() {
		return data.getInt("eps");
	}

	@Override
	public int getPerPlanetCount() {
		return data.getInt("perplanet");
	}

	@Override
	public int getPerUserCount() {
		return data.getInt("perowner");
	}

	@Override
	public int getCategory() {
		return data.getInt("category");
	}

	@Override
	public boolean isUComplex() {
		return data.getInt("ucomplex") == 1;
	}

	@Override
	public boolean isDeakAble() {
		return data.getBoolean("deakable");
	}

}
