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
package net.driftingsouls.ds2.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Die Sektor-Template-Verwaltung
 * @author Christopher Jung
 *
 */
public class SectorTemplateManager {
	private static SectorTemplateManager instance = null;
	
	private SectorTemplateManager() {
	}

	/*
	 * TODO: Caching, Caching, Caching....
	 */
	public static SectorTemplateManager getInstance() {
		if( instance == null ) {
			instance = new SectorTemplateManager();
		}
		return instance;
	}

	private class DockEntry {
		public String docked;
		public int shipid;

		public DockEntry(int shipid, String docked) {
			this.shipid = shipid;
			this.docked = docked;
		}
	}
	
	private class FleetEntry {
		public int fleetid;
		public int shipid;

		public FleetEntry(int shipid, int fleetid) {
			this.shipid = shipid;
			this.fleetid = fleetid;
		}
	}
	
	public Integer[] useTemplate(Database db, String name, Location location, int owner, boolean smartid ) {
		if( smartid ) {
			System.err.println("FIXME: SectorTemplateManager.useTemplate -> smartid not implemented");
			new Throwable().printStackTrace();
		}
		
		SQLResultRow res = db.first("SELECT * FROM global_sectortemplates WHERE id='",name,"'");
		if( res.isEmpty() ) {
			System.err.println("ERROR: SectorTemplateManager.useTemplate -> unknown resourceid '"+name+"' used");
			new Throwable().printStackTrace();
			return null;
		}
		
		List<Integer> shipids = new ArrayList<Integer>();
		
		String query = "SELECT * FROM ships WHERE id>0 AND system=0 ";
	
		if( res.getInt("w") == 0 ) {
			query += " AND x="+res.getInt("x")+" ";
		}
		else {
			query += " AND (x BETWEEN "+res.getInt("x")+" AND "+(res.getInt("x")+res.getInt("w"))+") ";
		}
		
		if( res.getInt("h") == 0 ) {
			query += " AND y="+res.getInt("y")+" ";
		}
		else {
			query += " AND (y BETWEEN "+res.getInt("y")+" AND "+(res.getInt("y")+res.getInt("h"))+") ";
		}
		
		List<DockEntry> docked = new ArrayList<DockEntry>();
		Map<Integer,Integer> idtable = new HashMap<Integer,Integer>();
		List<FleetEntry> fleet = new ArrayList<FleetEntry>();
		
		SQLQuery ship = db.query(query);
		while( ship.next() ) {
			int newx = location.getX() + ship.getInt("x") - res.getInt("x");
			int newy = location.getY() + ship.getInt("y") - res.getInt("y");
			
			db.update("INSERT INTO ships ", 
					" (owner,x,y,system,name,type,cargo,status,crew,e,s,hull,shields,heat,engine,weapons,comm,sensors,docked,alarm,destx,desty,destsystem,destcom,bookmark,jumptarget,autodeut,history) ",
					" VALUES ",
					" ('",owner,"','",newx,"','",newy,"','",location.getSystem(),"','",ship.get("name"),"','",ship.get("type"),"','",ship.get("cargo"),"','",ship.get("status"),"', ",
					 " '",ship.get("crew"),"','",ship.get("e"),"','",ship.get("s"),"','",ship.get("hull"),"','",ship.get("shields"),"', ",
					 " '",ship.get("heat"),"','",ship.get("engine"),"','",ship.get("weapons"),"','",ship.get("comm"),"', ",
					 " '",ship.get("sensors"),"','','",ship.get("alarm"),"', ",
					 " '",ship.get("destx"),"','",ship.get("desty"),"','",ship.get("destsystem"),"','",ship.get("destcom"),"', ",
					 " '",ship.get("bookmark"),"','",ship.get("jumptarget"),"','",ship.get("autodeut"),"','",ship.get("history"),"')");
			int shipid = db.insertID();
			
			idtable.put(ship.getInt("id"), shipid);
			
			if( !"".equals(ship.getString("docked")) ) {
				docked.add( new DockEntry(shipid, ship.getString("docked")) );
			}
			
			if( ship.getInt("fleet") != 0 ) {
				fleet.add( new FleetEntry(shipid, ship.getInt("fleet")) );
			}
			
			shipids.add(shipid);
			
			if( ship.getString("status").indexOf("tblmodules") != -1 ) {
				/* TODO: das geht auch schoener...ggf via Ships.recalculateShipModules */
				SQLResultRow modules = db.first("SELECT * FROM ships_modules WHERE id='",ship.getInt("id"),"'");
				db.update("INSERT INTO ships_modules " ,
						"(id,modules,nickname,picture,ru,rd,ra,rm," ,
						"eps,cost,hull,panzerung,cargo,heat,crew,weapons,maxheat,torpedodef," ,
						"shields,size,jdocks,adocks,sensorrange,hydro,deutfactor,recost,flags,werft,ow_werft) VALUES " ,
						"('$shipid','",modules.get("modules"),"','",modules.get("nickname"),"','",modules.get("picture"),"','",modules.get("ru"),"','",modules.get("rd"),"','",modules.get("ra"),"','",modules.get("rm"),"'," ,
						"'",modules.get("eps"),"','",modules.get("cost"),"','",modules.get("hull"),"','",modules.get("panzerung"),"','",modules.get("cargo"),"','",modules.get("heat"),"','",modules.get("crew"),"','",modules.get("weapons"),"','",modules.get("maxheat"),"','",modules.get("torpedodef"),"'," ,
						"'",modules.get("shields"),"','",modules.get("size"),"','",modules.get("jdocks"),"','",modules.get("adocks"),"','",modules.get("sensorrange"),"','",modules.get("hydro"),"','",modules.get("deutfactor"),"','",modules.get("recost"),"','",modules.get("flags"),"','",modules.get("werft"),"','",modules.get("ow_werft"),"')");
			}
		}
		ship.free();
		
		// Gedockte Schiffe behandeln
		boolean landed = false;
		for( DockEntry dock : docked ) {
			int masterid = -1;
			if( dock.docked.charAt(0) == 'l' ) {
				String split[] = dock.docked.split(" ");
				masterid = Integer.parseInt(split[1]);
				landed = true;	
			}	
			else {
				masterid = Integer.parseInt(dock.docked);
				landed = false;
			}
			
			masterid = idtable.get(masterid);
			
			String newdock = "";
			if( landed ) {
				newdock = "l "+masterid;	
			}
			else {
				newdock = Integer.toString(masterid);	
			}
			
			db.update("UPDATE ships SET docked='",newdock,"' WHERE id='",dock.shipid,"'");
		}
		
		Map<Integer,Integer> fleetlist = new HashMap<Integer,Integer>();
		
		for( FleetEntry flship : fleet ) {
			if( !fleetlist.containsKey(flship.fleetid) ) {
				String fleetname = db.first("SELECT name FROM ship_fleets WHERE id='",flship.fleetid,"'").getString("name");
				db.update("INSERT INTO ship_fleets (name) VALUES ('",fleetname,"')");
				fleetlist.put( flship.fleetid, db.insertID() );
			}
			db.update("UPDATE ships SET fleet='",fleetlist.get(flship.fleetid),"' WHERE id='",flship.shipid,"'");
		}
		
		return shipids.toArray(new Integer[shipids.size()]);
	}

	public Integer[] useTemplate(Database db, String name, Location location, int owner) {
		return useTemplate(db, name, location, owner, false);
	}
}
