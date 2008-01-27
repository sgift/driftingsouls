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
package net.driftingsouls.ds2.server.modules;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Generiert die Sternenkarte, die Sternensystem-Liste sowie die Liste der Schiffe in einem Sektor
 * @author Christopher Jung
 * 
 * @urlparam Integer sys Das anzuzeigende System
 * @urlparam Integer debugme Falls != 0 wird eine Sternenkarte mit Debugausgaben generiert
 *
 */
public class MapDataController extends DSGenerator implements Loggable {
	private static final int PROTOCOL_VERSION = 8;
	private static final int PROTOCOL_MINOR_VERSION = 0;
	
	private static final int ADDDATA_TEXT = 1;
	private static final int ADDDATA_LARGE_OBJECT = 2;
	
	private static final int OBJECT_FLEET_ENEMY = 1;
	private static final int OBJECT_FLEET_ALLY = 2;
	private static final int OBJECT_FLEET_OWN = 4;
	private static final int OBJECT_ASTI = 8;
	private static final int OBJECT_ASTI_OWN = 16;
	private static final int OBJECT_ASTI_ALLY = 24;
	private static final int OBJECT_ASTI_ENEMY = 32;
	private static final int OBJECT_NEBEL_DEUT_LOW = 40;
	private static final int OBJECT_NEBEL_DEUT_NORMAL = 48;
	private static final int OBJECT_NEBEL_DEUT_HIGH = 56;
	private static final int OBJECT_NEBEL_EMP_LOW = 64;
	private static final int OBJECT_NEBEL_EMP_NORMAL = 72;
	private static final int OBJECT_NEBEL_EMP_HIGH = 80;
	private static final int OBJECT_NEBEL_DAMAGE = 88;
	private static final int OBJECT_JUMPNODE = 96;
	
	private User usedUser;
	private SQLResultRow ally;
	private int system;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public MapDataController(Context context) {
		super(context);
		
		parameterNumber("sys");
		parameterNumber("debugme");
		
		requireValidSession(false);
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = getUser();
		Database db = getDatabase();
		
		this.usedUser = user;
		
		if( user.getAccessLevel() >= 20 ) {
			this.parameterNumber("forceuser");
			int forceuser = getInteger("forceuser");
	
			if( forceuser != 0 ) {
				this.usedUser = getContext().createUserObject(forceuser);	
			}	
		}
		
		if( this.usedUser.getAlly() != 0 ) {
			ally = db.first("SELECT showastis,showlrs FROM ally WHERE id=",usedUser.getAlly());
		} 
		else {
			ally = new SQLResultRow();
			ally.put("showastis", false);
			ally.put("showlrs", 0);
		}
		
		int sys = getInteger("sys");
		
		if( Systems.get().system(sys) == null ) {
			sys = 1;
		}

		if( (Systems.get().system(sys).getAccess() == StarSystem.AC_ADMIN) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
			sys = 1;
		}  
		else if( (Systems.get().system(sys).getAccess() == StarSystem.AC_NPC) && !user.hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !user.hasFlag( User.FLAG_VIEW_SYSTEMS ) ) {
			sys = 1;
		}
		
		this.system = sys;
		
		return true;
	}

	@Override
	protected void printHeader( String action ) {
		if( action.equals("showSector") || action.equals("getSystems") ) {
			getContext().getResponse().setContentType("text/xml");
			getContext().getResponse().setCharSet("UTF-16");
		}
		else {
			getContext().getResponse().setContentType("text/plain");
			getContext().getResponse().setCharSet("ISO-8859-1");
		}
	}
	
	@Override
	protected void printFooter( String action ) {
		// EMPTY
	}
	
	private String getStarmapValue( int value ) throws UnsupportedEncodingException {	
		if( value == 0 ) {
			return new String(new byte[] {0}, "ISO-8859-1");
		}
		
		boolean was255 = false;
		byte[] returnstr = new byte[value / 255 + 1];
		int index = 0;
		
		while( value > 0 ) {
			if( value >= 255 ) {
				was255 = true;
				returnstr[index++] = (byte)255;
				value -= 255;	
			}	
			else {
				returnstr[index++] = (byte)value;
				was255 = false;
				value = 0;	
			}
		}
		if( was255 ) {
			returnstr[index++] = (byte)0;	
		}
	
		return new String(returnstr, "ISO-8859-1");
	}
	
	private void echoSectorShipData( SQLResultRow aship, String relation ) throws UnsupportedEncodingException {
		SQLResultRow stype = ShipTypes.getShipType(aship);
		
		StringBuffer echo = getContext().getResponse().getContent();
		
		echo.append("<ship id=\""+aship.getInt("id")+"\" relation=\""+relation+"\">\n");
		echo.append("<owner>"+aship.getInt("owner")+"</owner>\n");
		
		User auser = getContext().createUserObject(aship.getInt("owner"));
		echo.append("<ownername><![CDATA["+auser.getName()+"]]></ownername>\n");
		echo.append("<picture><![CDATA["+stype.getString("picture")+"]]></picture>\n");
		
		echo.append("<type id=\""+stype.getInt("id")+"\">\n");
		echo.append("<name><![CDATA["+stype.getString("nickname")+"]]></name>\n");
		echo.append("<hull>"+stype.getInt("hull")+"</hull>\n");
		echo.append("<shields>"+stype.getInt("shields")+"</shields>\n");
		echo.append("<crew>"+stype.getInt("crew")+"</crew>\n");
		echo.append("<eps>"+stype.getInt("eps")+"</eps>\n");
		echo.append("<cargo>"+stype.getInt("cargo")+"</cargo>\n");
		echo.append("</type>\n");
			
		echo.append("<name><![CDATA["+aship.getString("name")+"]]></name>\n");
		echo.append("<hull>"+aship.getInt("hull")+"</hull>\n");
		echo.append("<shields>"+aship.getInt("shields")+"</shields>\n");
		echo.append("<fleet>"+aship.getInt("fleet")+"</fleet>\n");
		echo.append("<battle>"+aship.getInt("battle")+"</battle>\n");
		if( relation.equals("owner") ) {
			echo.append("<crew>"+aship.getInt("crew")+"</crew>\n");		
			echo.append("<e>"+aship.getInt("e")+"</e>\n");
			echo.append("<s>"+aship.getInt("s")+"</s>\n");
			Cargo cargo = new Cargo( Cargo.Type.STRING, aship.getString("cargo") );
			echo.append("<usedcargo>"+cargo.getMass()+"</usedcargo>\n");
		}
		
		echo.append("</ship>\n");
	}
	
	/**
	 * Gibt den Inhalt eines Sektors als XML-Dokument zurueck
	 * @urlparam Integer x Die X-Koordinate des Sektors
	 * @urlparam Integer y Die Y-Koordinate des Sektors
	 *
	 */
	public void showSectorAction() {
		Database db = getDatabase();
		
		parameterNumber("x");
		parameterNumber("y");
		
		int x = getInteger("x");
		int y = getInteger("y");
	
		StringBuffer echo = getContext().getResponse().getContent();
		echo.append("<?xml version='1.0' encoding='UTF-16'?>\n");
		echo.append("<sector x=\""+x+"\" y=\""+y+"\" system=\""+this.system+"\">\n");
	
		if( usedUser == null ) {
			echo.append("</sector>\n");
	
			return;		
		}
		
		SQLResultRow nebel = db.first("SELECT id,type FROM nebel WHERE system=",this.system," AND x=",x," AND y=",y);
		
		// EMP-Nebel?
		if( !nebel.isEmpty() && (nebel.getInt("type") >= 3) && (nebel.getInt("type") <= 5) ) {
			echo.append("</sector>");
			return;	
		}
				
		String usersql = "="+this.usedUser.getId();
		if( this.ally.getInt("showlrs") != 0 ) {				
			SQLQuery uid = db.query("SELECT id FROM users WHERE ally=",this.usedUser.getAlly());
			
			Integer[] allyusers = new Integer[uid.numRows()];
			int index = 0;
			
			while( uid.next() ) {
				allyusers[index++] = uid.getInt("id");
			}
			uid.free();
			
			usersql = " IN ("+Common.implode(",",allyusers)+")";
		}
		
		try {
			SQLQuery aship = db.query("SELECT * FROM ships WHERE id>0 AND owner ",usersql," AND system=",this.system," AND x=",x," AND y=",y," AND !LOCATE('l ',docked)");
			while( aship.next() ) {
				if( !nebel.isEmpty() ) {
					nebel.clear();
				}
				if( aship.getInt("owner") == this.usedUser.getId() ) {
					this.echoSectorShipData(aship.getRow(), "owner");
				}
			}
			aship.free();
		
			if( !nebel.isEmpty() ) {
				echo.append("</sector>\n");
				return;
			}
				
			List<Integer> verysmallshiptypes = new ArrayList<Integer>();
			verysmallshiptypes.add(0); // Ein dummy-Wert, damit es keine SQL-Fehler gibt
			
			SQLQuery stid = db.query("SELECT id FROM ship_types WHERE LOCATE('",ShipTypes.SF_SEHR_KLEIN,"',flags)");
			while( stid.next() ) {
				verysmallshiptypes.add(stid.getInt("id"));
			}
			stid.free();
					
			SQLQuery scanner = db.query("SELECT t1.x,t1.y,t1.crew,t1.sensors,t1.type,t1.id,t1.status " ,
					"FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id ",
					"WHERE t1.id>0 AND t1.system=",this.system," AND t1.owner",usersql," AND " ,
							"t2.class IN (11,13)");
			while( scanner.next() ) {
				SQLResultRow scannertype = ShipTypes.getShipType( scanner.getRow() );
					
				if( scanner.getInt("crew") < scannertype.getInt("crew")/3 ) {
					continue;
				}
					
				int range = scannertype.getInt("sensorrange");
					
				// Nebel?
				nebel = db.first("SELECT id FROM nebel WHERE system=",this.system," AND x=",scanner.getInt("x")," AND y=",scanner.getInt("y"));
				if( !nebel.isEmpty() ) {
					range = (int)Math.round(range/2d);	
				}
					
				range = (int)Math.round(range*(scanner.getInt("sensors")/100d));
				if( Math.round(Math.sqrt(Math.pow(scanner.getInt("y")-y,2)+Math.pow(scanner.getInt("x")-x,2))) > range ) {
					continue;
				}
						
				// Schiffe
				SQLQuery s = db.query("SELECT t1.*,t2.ally " ,
						"FROM ships t1 JOIN users t2 ON t1.owner=t2.id " ,
						"WHERE t1.id>0 AND !LOCATE('l ',docked) AND t1.system=",this.system," AND t1.owner!=",this.usedUser.getId()," AND t1.x=",x," AND t1.y=",y," AND " ,
							"(t1.visibility IS NULL OR t1.visibility='",this.usedUser.getId(),"') AND (!(t1.type IN (",Common.implode(",",verysmallshiptypes),")) OR LOCATE('tblmodules',t1.status)) " ,
							"ORDER BY t1.x,t1.y");
				while( s.next() ) {			
					SQLResultRow st = ShipTypes.getShipType( s.getRow() );
					if( ShipTypes.hasShipTypeFlag(st, ShipTypes.SF_SEHR_KLEIN) ) {
						continue;	
					}
								
					if( (this.usedUser.getAlly() != 0) && (s.getInt("ally") == this.usedUser.getAlly()) ) {
						echoSectorShipData(s.getRow(), "ally");
					}
					else if( this.usedUser.getAlly() == 0 || (this.usedUser.getAlly() != 0 && (s.getInt("ally") != this.usedUser.getAlly())) ) {
						echoSectorShipData(s.getRow(), "enemy");
					}
				}
				s.free();
				
				break;
			}
			scanner.free();
		}
		catch( UnsupportedEncodingException e ) {
			LOG.error("Kann Sektor "+system+":"+x+"/"+y+" fuer die Sternenkarte nicht aufbereiten", e);
		}
		
		echo.append("</sector>");
	}
	
	/**
	 * Gibt die Liste der Systeme als XML-Dokument zurueck
	 *
	 */
	public void getSystemsAction() {
		StringBuffer echo = getContext().getResponse().getContent();
		
		echo.append("<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n");
		echo.append("<systems>\n");
		for( StarSystem asystem : Systems.get() ) {
			if( (asystem.getAccess() == StarSystem.AC_ADMIN) && !this.getUser().hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) ) {
				continue;
			}  
			else if( (asystem.getAccess() == StarSystem.AC_NPC) && !this.getUser().hasFlag( User.FLAG_VIEW_ALL_SYSTEMS ) && !this.getUser().hasFlag( User.FLAG_VIEW_SYSTEMS ) ) {
				continue;
			}
		
			echo.append("<system id=\""+asystem.getID()+"\"><![CDATA["+asystem.getName()+"]]></system>\n");
		}
		echo.append("</systems>");
	}
	
	private boolean checkMapObjectsOR(int value, int ... values) {
		value -= (value & 7);
		
		for( int i = 0; i < values.length; i++ ) {
			if( value == values[i] ) {
				return true;	
			}
		}
		
		return false;
	}
	
	private void appendStr(String[][] map, int x, int y, String str ) {
		if( map[x][y] == null ) {
			map[x][y] = str;
		}
		else {
			map[x][y] += str;
		}
	}
	
	/**
	 * Generiert die Sternenkarte
	 */
	@Override
	public void defaultAction() {
		boolean debug = getInteger("debugme") != 0;
		
		Database db = getDatabase();
		StarSystem sys = Systems.get().system(system);
		
		int[][] map = new int[sys.getWidth()+1][sys.getHeight()+1];
		String[][] maptext = new String[sys.getWidth()+1][sys.getHeight()+1];

		//--------------------------------------
		//Jumpgates in die Karte eintragen
		//--------------------------------------
		SQLQuery node = db.query("SELECT x,y,name,systemout,xout,yout " +
				"FROM jumpnodes " +
				"WHERE system=",this.system," AND hidden=0 AND (x BETWEEN 1 AND ",sys.getWidth(),") AND (y BETWEEN 1 AND ",sys.getHeight(),") " +
				"ORDER BY id");
		while( node.next() ) {
			map[node.getInt("x")][node.getInt("y")] = OBJECT_JUMPNODE;
			maptext[node.getInt("x")][node.getInt("y")] = "Ziel: "+node.getString("name")+" ("+node.getInt("systemout")+":"+node.getInt("xout")+"/"+node.getInt("yout")+")\n";
		}
		node.free();
		
		//--------------------------------------
		//Asteroiden in die Karte eintragen
		//--------------------------------------
		SQLQuery base = db.query("SELECT b.x,b.y,b.owner,b.klasse,u.ally,b.name,u.name username " +
				"FROM bases b JOIN users u ON b.owner=u.id " +
				"WHERE b.system=",this.system," AND b.size=0 AND (b.x BETWEEN 1 AND ",sys.getWidth(),") AND (b.y BETWEEN 1 AND ",sys.getHeight(),") " +
				"ORDER BY "+(this.usedUser != null ? "IF(b.owner="+this.usedUser.getId()+",0,1)," : "")+"b.id");
		while( base.next() ) {
			Location loc = new Location(system, base.getInt("x"), base.getInt("y"));
			
			if( this.ally.getBoolean("showastis") && (base.getInt("owner") != this.usedUser.getId()) && (base.getInt("ally") == this.usedUser.getAlly()) ) {	
				map[loc.getX()][loc.getY()] = OBJECT_ASTI_ALLY;
				appendStr(maptext, loc.getX(), loc.getY(), base.getString("name")+" - "+base.getString("username")+"\n");
			} 
			else  if( (this.usedUser != null) && (base.getInt("owner") == this.usedUser.getId()) ) {
				map[loc.getX()][loc.getY()] = OBJECT_ASTI_OWN;
				appendStr(maptext, loc.getX(), loc.getY(), base.getString("name")+" - "+base.getString("username")+"\n");
			}
			else if( map[loc.getX()][loc.getY()] != OBJECT_ASTI_OWN ) {
				map[loc.getX()][loc.getY()] = OBJECT_ASTI;
			} 
		}
		base.free();
		
		//--------------------------------------
		//Nebel in die Karte eintragen
		//--------------------------------------
		SQLQuery nebel = db.query("SELECT x,y,type " +
				"FROM nebel " +
				"WHERE system=",this.system," AND (x BETWEEN 1 AND ",sys.getWidth(),") AND (y BETWEEN 1 AND ",sys.getHeight(),") " +
				"ORDER BY id");
		while( nebel.next() ) {
			int neb = 0;
			switch( nebel.getInt("type") ) {
			case 0: neb = OBJECT_NEBEL_DEUT_NORMAL; break;
			case 1: neb = OBJECT_NEBEL_DEUT_LOW; break;
			case 2: neb = OBJECT_NEBEL_DEUT_HIGH; break;
			case 3: neb = OBJECT_NEBEL_EMP_LOW; break;
			case 4: neb = OBJECT_NEBEL_EMP_NORMAL; break;
			case 5: neb = OBJECT_NEBEL_EMP_HIGH; break;
			case 6: neb = OBJECT_NEBEL_DAMAGE; break;	
			}
			map[nebel.getInt("x")][nebel.getInt("y")] = neb;
		}
		nebel.free();
		
		//--------------------------------------
		//Eigene Schiffe in die Karte eintragen
		//--------------------------------------
		if( usedUser != null ) {
			SQLQuery schiff = db.query("SELECT x,y,count(*) shipcount FROM ships WHERE id>0 AND owner=",this.usedUser.getId()," AND system=",this.system," GROUP BY x,y");
		
			while( schiff.next() ) {
				Location loc = new Location(system, schiff.getInt("x"), schiff.getInt("y"));
				if( checkMapObjectsOR(map[loc.getX()][loc.getY()], OBJECT_NEBEL_EMP_LOW, OBJECT_NEBEL_EMP_NORMAL, OBJECT_NEBEL_EMP_HIGH) ) {
					continue;
				}
				if( (map[loc.getX()][loc.getY()] & OBJECT_FLEET_OWN) == 0 ) {
					map[loc.getX()][loc.getY()] |= OBJECT_FLEET_OWN;
					if( schiff.getInt("shipcount") > 1 ) {
						appendStr(maptext, loc.getX(), loc.getY(), schiff.getInt("shipcount")+" eigene Schiffe\n");
					}
					else {
						appendStr(maptext, loc.getX(), loc.getY(), schiff.getInt("shipcount")+" eigenes Schiffe\n");
					}
				}
			}
			schiff.free();
		
		
			//--------------------------------------------------------
			//	Alle Scanner-Schiffe durchlaufen und Kontakte eintragen
			//--------------------------------------------------------
		
			List<Integer> verysmallshiptypes = new ArrayList<Integer>();
			verysmallshiptypes.add(0); // Ein dummy-Wert, damit es keine SQL-Fehler gibt
			
			SQLQuery stid = db.query("SELECT id FROM ship_types WHERE LOCATE('",ShipTypes.SF_SEHR_KLEIN,"',flags)");
			while( stid.next() ) {
				verysmallshiptypes.add(stid.getInt("id"));
			}
			stid.free();
			
			String usersql = "="+this.usedUser.getId();
			if( this.ally.getInt("showlrs") != 0 ) {				
				SQLQuery uid = db.query("SELECT id FROM users WHERE ally=",this.usedUser.getAlly());
				
				Integer[] allyusers = new Integer[uid.numRows()];
				int index = 0;
				
				while( uid.next() ) {
					allyusers[index++] = uid.getInt("id");
				}
				uid.free();
				
				usersql = " IN ("+Common.implode(",",allyusers)+")";
			}
			
			Set<Location> lrsScanSectors = new HashSet<Location>();
			Set<Location> scannedSectors = new HashSet<Location>();
			
			SQLQuery scanner = db.query("SELECT t1.x,t1.y,t1.crew,t1.sensors,t1.type,t1.id,t1.status " +
					"FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id " +
					"WHERE t1.id>0 AND t1.system=",this.system," AND t1.owner",usersql," AND t2.class IN (11,13)");
			while( scanner.next() ) {
				Location loc = new Location(system, scanner.getInt("x"), scanner.getInt("y"));
				
				SQLResultRow scannertype = ShipTypes.getShipType( scanner.getRow() );
				
				if( scanner.getInt("crew") < scannertype.getInt("crew")/3 ) {
					continue;
				}
				
				int range = scannertype.getInt("sensorrange");
				
				// Nebel?
				if( checkMapObjectsOR(map[loc.getX()][loc.getY()], OBJECT_NEBEL_DEUT_NORMAL, OBJECT_NEBEL_DAMAGE ) ) {
					range = (int)Math.round(range/2d);
				}
				else if( checkMapObjectsOR(map[loc.getX()][loc.getY()], OBJECT_NEBEL_DEUT_LOW)) {
					range = (int)Math.round(range/0.75d);
				}
				else if( checkMapObjectsOR(map[loc.getX()][loc.getY()], OBJECT_NEBEL_DEUT_HIGH) ) {
					range = (int)Math.round(range/3d);
				}
				else if( checkMapObjectsOR(map[loc.getX()][loc.getY()], OBJECT_NEBEL_EMP_LOW, OBJECT_NEBEL_EMP_NORMAL, OBJECT_NEBEL_EMP_HIGH) ) {
					continue;
				}
				
				range = (int)Math.round(range*(scanner.getInt("sensors")/100d));
				
				scannedSectors.clear();
				
				// Basen
				SQLQuery b = db.query("SELECT t1.x,t1.y,t2.ally,t1.owner,t1.name,t2.name username " +
						"FROM bases t1 JOIN users t2 ON t1.owner=t2.id " +
						"WHERE t1.system=",this.system," AND t1.owner!=",this.usedUser.getId()," AND t1.owner!=0 AND " +
								"(t1.x BETWEEN ",(loc.getX()-range)," AND ",(loc.getX()+range),") AND " +
								"(t1.y BETWEEN ",(loc.getY()-range)," AND ",(loc.getY()+range)+")");
				while( b.next() ) {
					Location bLoc = new Location(system, b.getInt("x"), b.getInt("y"));
					
					// Nebel?
					if( checkMapObjectsOR( map[bLoc.getX()][bLoc.getY()],
						OBJECT_NEBEL_DEUT_LOW,
						OBJECT_NEBEL_DEUT_NORMAL,
						OBJECT_NEBEL_DEUT_HIGH,
						OBJECT_NEBEL_EMP_LOW,
						OBJECT_NEBEL_EMP_NORMAL,
						OBJECT_NEBEL_EMP_HIGH,
						OBJECT_NEBEL_DAMAGE) ) {
						continue;
					}
					
					if( Math.round(Math.sqrt(Math.pow(loc.getY()-bLoc.getY(),2)+Math.pow(loc.getX()-bLoc.getX(),2))) > range )  {
						continue;
					}
					
					if( !scannedSectors.contains(bLoc) && (
						checkMapObjectsOR(map[bLoc.getX()][bLoc.getY()], OBJECT_ASTI_ALLY, OBJECT_ASTI_ENEMY) ) ) {
						continue;
					}
					
					scannedSectors.add(bLoc);
					
					if( this.usedUser.getAlly() != 0 && !this.ally.getBoolean("showastis") && (b.getInt("ally") == this.usedUser.getAlly()) ) {
						int mod = map[bLoc.getX()][bLoc.getY()] & 7;
						map[bLoc.getX()][bLoc.getY()] = OBJECT_ASTI_ALLY + mod;
						
						appendStr(maptext, bLoc.getX(), bLoc.getY(), b.getString("name")+" - "+b.getString("username")+"\n");
					}
					else if( this.usedUser.getAlly() == 0 || (this.usedUser.getAlly() != 0 && (b.getInt("ally") != this.usedUser.getAlly())) ) {
						int mod = map[bLoc.getX()][bLoc.getY()] & 7;
						map[bLoc.getX()][bLoc.getY()] = OBJECT_ASTI_ENEMY + mod;
						
						appendStr(maptext, bLoc.getX(), bLoc.getY(), b.getString("name")+" - "+b.getString("username")+"\n");
					}
				}
				b.free();
				
				int enemycount = 0;
				int allycount = 0;
				Map<Integer,Integer> usercount = new HashMap<Integer,Integer>();
				int lastX = 0;
				int lastY = 0;
				
				// Schiffe
				SQLQuery s = db.query("SELECT t1.x,t1.y,t2.ally,t1.owner,t1.docked,t1.status,t1.type,t1.id " ,
						"FROM ships t1 JOIN users t2 ON t1.owner=t2.id " ,
						"WHERE t1.id>0 AND t1.system=",system," AND !LOCATE('l ',docked) AND t1.owner!=",this.usedUser.getId()," AND (t1.x BETWEEN ",(loc.getX()-range)," AND ",(loc.getX()+range),") AND (t1.y BETWEEN ",(loc.getY()-range)," AND ",(loc.getY()+range),") AND " ,
							"(t1.visibility IS NULL OR t1.visibility=",this.usedUser.getId(),") AND (!(t1.type IN (",Common.implode(",",verysmallshiptypes),")) OR LOCATE('tblmodules',t1.status)) " ,
						"ORDER BY t1.x,t1.y");
				while( s.next() ) {
					Location sLoc = new Location(system, s.getInt("x"), s.getInt("y"));
					
					if( lrsScanSectors.contains(sLoc) ) {
						continue;
					}
					
					// EMP-Nebel?
					if( checkMapObjectsOR( map[sLoc.getX()][sLoc.getY()],
						OBJECT_NEBEL_EMP_LOW,
						OBJECT_NEBEL_EMP_NORMAL,
						OBJECT_NEBEL_EMP_HIGH) ) {
						continue;
					}

					// Nebel (und kein eigenes Schiff anwesend)?
					if( checkMapObjectsOR( map[sLoc.getX()][sLoc.getY()],
						OBJECT_NEBEL_DEUT_LOW,
						OBJECT_NEBEL_DEUT_NORMAL,
						OBJECT_NEBEL_DEUT_HIGH,
						OBJECT_NEBEL_DAMAGE) && 
						(map[sLoc.getX()][sLoc.getY()] & OBJECT_FLEET_OWN) == 0 ) {
						continue;
					}
					
					if( Math.round(Math.sqrt(Math.pow(loc.getY()-sLoc.getY(),2)+Math.pow(loc.getX()-sLoc.getX(),2))) > range )  {
						continue;
					}
					
					SQLResultRow st = ShipTypes.getShipType( s.getRow() );
					if( ShipTypes.hasShipTypeFlag(st, ShipTypes.SF_SEHR_KLEIN) ) {
						continue;	
					}
					
					if( (lastX != sLoc.getX()) || (lastY != sLoc.getY()) ) {
						lrsScanSectors.add(new Location(system, lastX, lastY));
						if( usercount.size() > 9 ) {
							if( allycount != 0 && ((maptext[lastX][lastY] == null) || (maptext[lastX][lastY].indexOf("verb&uuml;ndete") == -1)) ) {
								appendStr(maptext, lastX, lastY, allycount+" verb&uuml;ndete"+(allycount==1?"s":"")+" Schiff"+(allycount==1?"":"e")+"\n");
							}
							if( enemycount != 0 && ((maptext[lastX][lastY] == null) || (maptext[lastX][lastY].indexOf("feindliche") == -1)) ) {
								appendStr(maptext, lastX, lastY, enemycount+" feindliche"+(enemycount==1?"s":"")+" Schiff"+(enemycount==1?"":"e")+"\n"); 
							}
						}
						else if( !usercount.isEmpty() ) {
							for( Integer userid : usercount.keySet() ) {
								User userobj = getContext().createUserObject(userid);
								appendStr(maptext, lastX, lastY, usercount.get(userid)+" Schiff"+(usercount.get(userid)==1?"":"e")+" "+userobj.getName()+"\n");
							}
						}
						lastX = sLoc.getX();
						lastY = sLoc.getY(); 	
						enemycount = 0;
						allycount = 0;
						usercount.clear();
					}
					
					if( s.getInt("owner") == this.usedUser.getId() ) {
						continue;	
					}
					
					if( !usercount.containsKey(s.getInt("owner")) ) {
						usercount.put(s.getInt("owner"), 1);
					}
					else {
						usercount.put(s.getInt("owner"), usercount.get(s.getInt("owner"))+1);
					}
					
					if( (this.usedUser.getAlly() != 0) && (s.getInt("ally") == this.usedUser.getAlly()) ) {
						if( (s.getString("docked").length() == 0 || (s.getString("docked").charAt(0) != 'l')) ) {
							map[sLoc.getX()][sLoc.getY()] |= OBJECT_FLEET_ALLY;
							allycount++;
						}
					}
					else if( (this.usedUser.getAlly() == 0) || (this.usedUser.getAlly() != 0 && (s.getInt("ally") != this.usedUser.getAlly())) ) {
						if( (s.getString("docked").length() == 0 || (s.getString("docked").charAt(0) != 'l')) ) {
							map[sLoc.getX()][sLoc.getY()] |= OBJECT_FLEET_ENEMY;
							enemycount++;
						}
					}
				}
				s.free();
				
				if( lastX != 0 && lastY != 0 ) {
					lrsScanSectors.add(new Location(system, lastX, lastY));
					if( usercount.size() > 9 ) {
						if( allycount != 0 && ((maptext[lastX][lastY] == null) || (maptext[lastX][lastY].indexOf("verb&uuml;ndete") == -1)) ) {
							appendStr(maptext, lastX, lastY, allycount+" verb&uuml;ndete"+(allycount==1?"s":"")+" Schiff"+(allycount==1?"":"e")+"\n");
						}
						if( enemycount != 0 && ((maptext[lastX][lastY] == null) || (maptext[lastX][lastY].indexOf("feindliche") == -1)) ) {
							appendStr(maptext, lastX, lastY, enemycount+" feindliche"+(enemycount==1?"s":"")+" Schiff"+(enemycount==1?"":"e")+"\n"); 
						}
					}
					else if( !usercount.isEmpty() ) {
						for( Integer userid : usercount.keySet() ) {
							User userobj = getContext().createUserObject(userid);
							appendStr(maptext, lastX, lastY, usercount.get(userid)+" Schiff"+(usercount.get(userid)==1?"":"e")+" "+userobj.getName()+"\n");
						}
					}
				}
			}
			scanner.free();
			
			lrsScanSectors = null;
			scannedSectors = null;
		}
		
		//--------------------------------------
		// Karte ausgeben
		//--------------------------------------

		try {
			StringBuffer echo = getContext().getResponse().getContent();
			// Senden wir ersteinmal die Protokoll-Version
			echo.append((char)PROTOCOL_VERSION);
			echo.append((char)PROTOCOL_MINOR_VERSION);
	
			if( (this.usedUser != null) && this.usedUser.getId() < 0 ) {
				echo.append((char)1);
			}
			else {
				echo.append((char)0);
			}
			echo.append(getStarmapValue(Math.abs(usedUser != null ? this.usedUser.getId() : 0)));
			
			// Nun die Kartengroesse senden (erst x dann y - jeweils zwei stellen pro char)
			int width = sys.getWidth();
			int height = sys.getHeight();
			
			echo.append(getStarmapValue(width));
			echo.append(getStarmapValue(height));
			
			//Nun das System senden
			if( !debug ) {
				echo.append(getStarmapValue(this.system));
			}
			else {
				echo.append("<br />System: "+this.system+"<br />\n");
			}
			
			//Die Felder einzeln ausgeben
			int zerocount = 0;
			for( int y = 1; y <= sys.getHeight(); y++ ) {
				for( int x = 1; x <= sys.getWidth(); x++ ) {
					if( (zerocount != 0) && ( map[x][y] != 0 ) ) {
						if( !debug ) {
							echo.append((char)0);
							echo.append(this.getStarmapValue(zerocount));
						}
	 					else {
	 						echo.append("zerocount: "+zerocount+"<br />\n");
	 					}
						
						zerocount = 0;
					}
						
					if( map[x][y] == 0 ) {
						zerocount++;
					} 
					else {
						if( !debug ) {
							echo.append(getStarmapValue( map[x][y] ));
						}	
						else {
							echo.append("value: "+map[x][y]+" at "+x+" / "+y+"<br />\n");
						}
					}
				}
			}
			if( zerocount != 0 ) {
				if( !debug ) {
					echo.append((char)0);
					echo.append(getStarmapValue(zerocount));
				}
	 			else {
	 				echo.append("zerocount: "+zerocount+"<br />\n");
	 			}
						
				zerocount = 0;
			}
			
			// Texte ausgeben
			for( int y = 1; y <= sys.getHeight(); y++ ) {
				for( int x = 1; x <= sys.getWidth(); x++ ) {
					if( maptext[x][y] != null && maptext[x][y].trim().length() > 0 ) {
						echo.append((char)ADDDATA_TEXT);
						
						echo.append(getStarmapValue(x-1));
			
						echo.append(getStarmapValue(y-1));	
						
						String text = new String(maptext[x][y].trim().getBytes("ISO-8859-1"), "ISO-8859-1");
						
						int length = text.length();
						
						echo.append(getStarmapValue( length ));
						
						echo.append(text);
					}
				}
			}
			
			// Planeten (grosse Objekte) ausgeben
			SQLQuery lobject = db.query("SELECT x,y,size,klasse FROM bases WHERE system=",this.system," AND size>0  AND (x BETWEEN 1 AND ",sys.getWidth(),") AND (y BETWEEN 1 AND ",sys.getHeight(),")");
			while( lobject.next() ) {
				echo.append((char)ADDDATA_LARGE_OBJECT);
						
				echo.append(getStarmapValue(lobject.getInt("x")-1));
			
				echo.append(getStarmapValue(lobject.getInt("y")-1));
				
				echo.append(getStarmapValue(lobject.getInt("size")));
				
				String image = "kolonie"+lobject.getInt("klasse")+"_lrs";
				image = new String(image.getBytes("ISO-8859-1"), "ISO-8859-1");
							
				int length = image.length();
						
				echo.append(getStarmapValue( length ));
						
				echo.append(image);
			}
			lobject.free();
		}
		catch( UnsupportedEncodingException e ) {
			LOG.error("Kann Sternenkarte nicht kodieren", e);
		}
	}
}
