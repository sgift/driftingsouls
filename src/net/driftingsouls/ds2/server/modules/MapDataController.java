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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generiert die Sternenkarte, die Sternensystem-Liste sowie die Liste der Schiffe in einem Sektor.
 * @author Christopher Jung
 * 
 * @urlparam Integer sys Das anzuzeigende System
 * @urlparam Integer debugme Falls != 0 wird eine Sternenkarte mit Debugausgaben generiert
 *
 */
public class MapDataController extends DSGenerator {
	private static final Log log = LogFactory.getLog(MapDataController.class);
	
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
	private Ally ally;
	private int system;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public MapDataController(Context context) {
		super(context);
		
		parameterNumber("sys");
		parameterNumber("debugme");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = (User)getUser();
	
		this.usedUser = user;
		
		if( user.getAccessLevel() >= 20 ) {
			this.parameterNumber("forceuser");
			int forceuser = getInteger("forceuser");
	
			if( forceuser != 0 ) {
				this.usedUser = (User)getContext().getDB().get(User.class, forceuser);	
			}	
		}
		
		ally = this.usedUser.getAlly();
		
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
			getContext().getResponse().setContentType("text/xml", "UTF-16");
		}
		else {
			getContext().getResponse().setContentType("text/plain", "ISO-8859-1");
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
	
	private void echoSectorShipData( Ship ship, String relation ) throws IOException {
		ShipTypeData stype = ship.getTypeData();
		
		Writer echo = getContext().getResponse().getWriter();
		
		echo.append("<ship id=\""+ship.getId()+"\" relation=\""+relation+"\">\n");
		echo.append("<owner>"+ship.getOwner().getId()+"</owner>\n");
		
		echo.append("<ownername><![CDATA["+ship.getOwner().getName()+"]]></ownername>\n");
		echo.append("<picture><![CDATA["+stype.getPicture()+"]]></picture>\n");
		
		echo.append("<type id=\""+stype.getTypeId()+"\">\n");
		echo.append("<name><![CDATA["+stype.getNickname()+"]]></name>\n");
		echo.append("<hull>"+stype.getHull()+"</hull>\n");
		echo.append("<shields>"+stype.getShields()+"</shields>\n");
		echo.append("<crew>"+stype.getCrew()+"</crew>\n");
		echo.append("<eps>"+stype.getEps()+"</eps>\n");
		echo.append("<cargo>"+stype.getCargo()+"</cargo>\n");
		echo.append("</type>\n");
			
		echo.append("<name><![CDATA["+ship.getName()+"]]></name>\n");
		echo.append("<hull>"+ship.getHull()+"</hull>\n");
		echo.append("<shields>"+ship.getShields()+"</shields>\n");
		echo.append("<fleet>"+(ship.getFleet() != null ? ship.getFleet().getId() : null)+"</fleet>\n");
		echo.append("<battle>"+(ship.getBattle() != null ? ship.getBattle().getId() : null)+"</battle>\n");
		if( relation.equals("owner") ) {
			echo.append("<crew>"+ship.getCrew()+"</crew>\n");		
			echo.append("<e>"+ship.getEnergy()+"</e>\n");
			echo.append("<s>"+ship.getHeat()+"</s>\n");
			echo.append("<usedcargo>"+ship.getCargo().getMass()+"</usedcargo>\n");
		}
		
		echo.append("</ship>\n");
	}
	
	/**
	 * Gibt den Inhalt eines Sektors als XML-Dokument zurueck.
	 * @throws IOException 
	 * @urlparam Integer x Die X-Koordinate des Sektors
	 * @urlparam Integer y Die Y-Koordinate des Sektors
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void showSectorAction() throws IOException {
		org.hibernate.Session db = getDB();
		
		parameterNumber("x");
		parameterNumber("y");
		
		int x = getInteger("x");
		int y = getInteger("y");
	
		Writer echo = getContext().getResponse().getWriter();
		echo.append("<?xml version='1.0' encoding='UTF-16'?>\n");
		echo.append("<sector x=\""+x+"\" y=\""+y+"\" system=\""+this.system+"\">\n");
		
		Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(this.system, x, y));
		
		// EMP-Nebel?
		if( (nebel != null) && (nebel.getType() >= 3) && (nebel.getType() <= 5) ) {
			echo.append("</sector>");
			return;	
		}
				
		String usersql = "="+this.usedUser.getId();
		if( (this.ally != null) && this.ally.getShowLrs() ) {				

			List<User> members = this.usedUser.getAlly().getMembers();
			
			int[] allyusers = new int[members.size()];
			int index = 0;
			for( User member : members ) {
				allyusers[index++] = member.getId();
			}
			
			usersql = " in ("+Common.implode(",",allyusers)+")";
		}
		
		try {
			List<?> shipList = db.createQuery("from Ship " +
					"where id>0 and owner "+usersql+" and system= :sys and x= :x and y=:y and locate('l ',docked)=0")
				.setInteger("sys", this.system)
				.setInteger("x", x)
				.setInteger("y", y)
				.list();
			
			for( Iterator<?> iter=shipList.iterator(); iter.hasNext(); ) {
				Ship ship = (Ship)iter.next();
				
				if( nebel != null ) {
					nebel = null;
				}
				if( this.usedUser.equals(ship.getOwner()) ) {
					this.echoSectorShipData(ship, "owner");
				}
			}
		
			if( nebel != null ) {
				echo.append("</sector>\n");
				return;
			}
							
			List<?> scannerList = db.createQuery("from Ship as s " +
					"where s.id>0 and s.system= :sys and s.owner "+usersql+" and " +
							"s.shiptype.shipClass in (11,13)")
				.setInteger("sys", this.system)
				.list();
			for( Iterator<?> iter=scannerList.iterator(); iter.hasNext(); ) {
				Ship scanner = (Ship)iter.next();
				
				ShipTypeData scannertype = scanner.getTypeData();
					
				if( scanner.getCrew() < scannertype.getCrew()/3 ) {
					continue;
				}
					
				int range = scannertype.getSensorRange();
					
				// Nebel?
				nebel = (Nebel)db.get(Nebel.class, new MutableLocation(scanner.getLocation()));
				if( nebel != null ) {
					range = (int)Math.round(range/2d);	
				}
					
				range = (int)Math.round(range*(scanner.getSensors()/100d));
				if( Math.round(Math.sqrt(Math.pow(scanner.getY()-y,2)+Math.pow(scanner.getX()-x,2))) > range ) {
					continue;
				}
						
				// Schiffe
				List<?> sList = db.createQuery("from Ship as s inner join fetch s.owner left join fetch s.modules " +
						"where s.id>0 and locate('l ',s.docked)=0 and s.system= :sys and s.owner!= :user and s.x= :x and s.y= :y and " +
							"(s.visibility is null or s.visibility= :userid) and (locate(:smallflag, s.shiptype.flags)=0 and (s.modules is null or locate(:smallflag,s.modules.flags)=0)) " +
							"order by s.x,s.y")
					.setInteger("sys", this.system)
					.setInteger("x", x)
					.setInteger("y", y)
					.setEntity("user", this.usedUser)
					.setInteger("userid", this.usedUser.getId())
					.setString("smallflag", ShipTypes.SF_SEHR_KLEIN)
					.list();
				for( Iterator<?> iter2=sList.iterator(); iter2.hasNext(); ) {
					Ship s = (Ship)iter2.next();
					
					ShipTypeData st = s.getTypeData();
					
					if( st.hasFlag(ShipTypes.SF_SEHR_KLEIN) ) {
						continue;	
					}
								
					if( (this.usedUser.getAlly() != null) && this.usedUser.getAlly().equals(s.getOwner().getAlly()) ) {
						echoSectorShipData(s, "ally");
					}
					else {
						echoSectorShipData(s, "enemy");
					}
				}

				break;
			}
		}
		catch( UnsupportedEncodingException e ) {
			log.error("Kann Sektor "+system+":"+x+"/"+y+" fuer die Sternenkarte nicht aufbereiten", e);
		}
		
		echo.append("</sector>");
	}
	
	/**
	 * Gibt die Liste der Systeme als XML-Dokument zurueck.
	 * @throws IOException 
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void getSystemsAction() throws IOException {
		Writer echo = getContext().getResponse().getWriter();
		
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
	 * Generiert die Sternenkarte.
	 * @throws IOException 
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() throws IOException {
		boolean debug = getInteger("debugme") != 0;
		
		org.hibernate.Session db = getDB();
		StarSystem sys = Systems.get().system(system);
		
		int[][] map = new int[sys.getWidth()+1][sys.getHeight()+1];
		String[][] maptext = new String[sys.getWidth()+1][sys.getHeight()+1];

		//--------------------------------------
		//Jumpgates in die Karte eintragen
		//--------------------------------------
		List<?> nodeList = db.createQuery("from JumpNode " +
				"where system= :sys and hidden=0 and (x between 1 and :width) and (y between 1 and :height) " +
				"order by id")
			.setInteger("sys", this.system)
			.setInteger("width", sys.getWidth())
			.setInteger("height", sys.getHeight())
			.list();
		for( Iterator<?> iter=nodeList.iterator(); iter.hasNext(); ) {
			JumpNode node = (JumpNode)iter.next();
			
			map[node.getX()][node.getY()] = OBJECT_JUMPNODE;
			maptext[node.getX()][node.getY()] = "Ziel: "+node.getName()+" ("+node.getSystemOut()+":"+node.getXOut()+"/"+node.getYOut()+")\n";
		}
		
		//--------------------------------------
		//Asteroiden in die Karte eintragen
		//--------------------------------------
		List<?> baseList = db.createQuery("from Base as b inner join fetch b.owner " +
				"where b.system= :sys and b.size=0 and (b.x between 1 and :width) and (b.y between 1 and :height) " +
				"order by case when b.owner= :user then 0 else 1 end, b.id")
			.setInteger("sys", this.system)
			.setInteger("width", sys.getWidth())
			.setInteger("height", sys.getHeight())
			.setEntity("user", this.usedUser)
			.list();
		for( Iterator<?> iter=baseList.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			
			Location loc = base.getLocation();
			
			if( (this.ally != null) && this.ally.getShowAstis() && !this.usedUser.equals(base.getOwner()) && this.ally.equals(base.getOwner().getAlly()) ) {	
				map[loc.getX()][loc.getY()] = OBJECT_ASTI_ALLY;
				appendStr(maptext, loc.getX(), loc.getY(), base.getName()+" - "+base.getOwner().getName()+"\n");
			} 
			else  if( this.usedUser.equals(base.getOwner()) ) {
				map[loc.getX()][loc.getY()] = OBJECT_ASTI_OWN;
				appendStr(maptext, loc.getX(), loc.getY(), base.getName()+" - "+base.getOwner().getName()+"\n");
			}
			else if( map[loc.getX()][loc.getY()] != OBJECT_ASTI_OWN ) {
				map[loc.getX()][loc.getY()] = OBJECT_ASTI;
			} 
		}
		
		//--------------------------------------
		//Nebel in die Karte eintragen
		//--------------------------------------
		List<?> nebelList = db.createQuery("from Nebel " +
				"where system= :sys and (x between 1 and :width) and (y between 1 and :height) " +
				"order by system,x,y")
			.setInteger("sys", this.system)
			.setInteger("width", sys.getWidth())
			.setInteger("height", sys.getHeight())
			.list();
		for( Iterator<?> iter=nebelList.iterator(); iter.hasNext(); ) {
			Nebel nebel = (Nebel)iter.next();
			
			int neb = 0;
			switch( nebel.getType() ) {
			case 0: neb = OBJECT_NEBEL_DEUT_NORMAL; break;
			case 1: neb = OBJECT_NEBEL_DEUT_LOW; break;
			case 2: neb = OBJECT_NEBEL_DEUT_HIGH; break;
			case 3: neb = OBJECT_NEBEL_EMP_LOW; break;
			case 4: neb = OBJECT_NEBEL_EMP_NORMAL; break;
			case 5: neb = OBJECT_NEBEL_EMP_HIGH; break;
			case 6: neb = OBJECT_NEBEL_DAMAGE; break;	
			}
			map[nebel.getX()][nebel.getY()] = neb;
		}
		
		//--------------------------------------
		//Eigene Schiffe in die Karte eintragen
		//--------------------------------------
		List<?> shipList = db.createQuery("select x,y,count(*) from Ship " +
				"where id>0 and owner= :user and system= :sys group by x,y")
			.setInteger("sys", this.system)
			.setEntity("user", this.usedUser)
			.list();
		for( Iterator<?> iter=shipList.iterator(); iter.hasNext(); ) {
			Object[] data = (Object[])iter.next();
			
			final long count = (Long)data[2];
			
			Location loc = new Location(system, (Integer)data[0], (Integer)data[1]);
			if( checkMapObjectsOR(map[loc.getX()][loc.getY()], OBJECT_NEBEL_EMP_LOW, OBJECT_NEBEL_EMP_NORMAL, OBJECT_NEBEL_EMP_HIGH) ) {
				continue;
			}
			if( (map[loc.getX()][loc.getY()] & OBJECT_FLEET_OWN) == 0 ) {
				map[loc.getX()][loc.getY()] |= OBJECT_FLEET_OWN;
				if( count > 1 ) {
					appendStr(maptext, loc.getX(), loc.getY(), count+" eigene Schiffe\n");
				}
				else {
					appendStr(maptext, loc.getX(), loc.getY(), count+" eigenes Schiffe\n");
				}
			}
		}
	
		//--------------------------------------------------------
		//	Alle Scanner-Schiffe durchlaufen und Kontakte eintragen
		//--------------------------------------------------------
	
		String usersql = "="+this.usedUser.getId();
		if( (this.ally != null) && this.ally.getShowLrs() ) {				

			List<User> members = this.usedUser.getAlly().getMembers();
			
			int[] allyusers = new int[members.size()];
			int index = 0;
			for( User member : members ) {
				allyusers[index++] = member.getId();
			}
			
			usersql = " in ("+Common.implode(",",allyusers)+")";
		}
		
		Set<Location> lrsScanSectors = new HashSet<Location>();
		Set<Location> scannedSectors = new HashSet<Location>();
		
		List<?> scannerList = db.createQuery("from Ship as s " +
				"where s.id>0 and s.system= :sys and s.owner "+usersql+" and s.shiptype.shipClass in (11,13)")
			.setInteger("sys", this.system)
			.list();
		for( Iterator<?> iter=scannerList.iterator(); iter.hasNext(); ) {
			Ship scanner = (Ship)iter.next();
			
			Location loc = scanner.getLocation();
			
			ShipTypeData scannertype = scanner.getTypeData();
			
			if( scanner.getCrew() < scannertype.getCrew()/3 ) {
				continue;
			}
			
			int range = scannertype.getSensorRange();
			
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
			
			range = (int)Math.round(range*(scanner.getSensors()/100d));
			
			scannedSectors.clear();
			
			// Basen
			List<?> bList = db.createQuery("from Base as b inner join fetch b.owner " +
					"where b.system= :sys and b.owner!= :user and b.owner!=0 and " +
							"(b.x between :minx and :maxx) and " +
							"(b.y between :miny and :maxy)")
				.setInteger("sys", this.system)
				.setEntity("user", this.usedUser)
				.setInteger("minx", loc.getX()-range)
				.setInteger("maxx", loc.getX()+range)
				.setInteger("miny", loc.getY()-range)
				.setInteger("maxy", loc.getY()+range)
				.list();
			for( Iterator<?> iter2=bList.iterator(); iter2.hasNext(); ) {
				Base base = (Base)iter2.next();
				
				Location bLoc = base.getLocation();
				
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
				
				if( (this.ally != null) && !this.ally.getShowAstis() && this.ally.equals(base.getOwner().getAlly()) ) {
					int mod = map[bLoc.getX()][bLoc.getY()] & 7;
					map[bLoc.getX()][bLoc.getY()] = OBJECT_ASTI_ALLY + mod;
					
					appendStr(maptext, bLoc.getX(), bLoc.getY(), base.getName()+" - "+base.getOwner().getName()+"\n");
				}
				else if( this.ally == null || (this.ally != null && this.ally.equals(base.getOwner().getAlly())) ) {
					int mod = map[bLoc.getX()][bLoc.getY()] & 7;
					map[bLoc.getX()][bLoc.getY()] = OBJECT_ASTI_ENEMY + mod;
					
					appendStr(maptext, bLoc.getX(), bLoc.getY(), base.getName()+" - "+base.getOwner().getName()+"\n");
				}
			}
			
			int enemycount = 0;
			int allycount = 0;
			Map<User,Integer> usercount = new HashMap<User,Integer>();
			int lastX = 0;
			int lastY = 0;
			
			// Schiffe
			List<?> sList = db.createQuery("from Ship as s inner join fetch s.owner left join fetch s.modules " +
					"where s.id>0 and s.system= :sys and locate('l ',s.docked)=0 and s.owner!= :user and (s.x between :minx and :maxx) and (s.y between :miny and :maxy) and " +
						"(s.visibility is null or s.visibility= :userid) and (locate(:smallflag, s.shiptype.flags)=0 and (s.modules is null or locate(:smallflag, s.modules.flags)=0)) " +
					"order by s.x,s.y")
				.setInteger("sys", this.system)
				.setEntity("user", this.usedUser)
				.setInteger("userid", this.usedUser.getId())
				.setInteger("minx", loc.getX()-range)
				.setInteger("maxx", loc.getX()+range)
				.setInteger("miny", loc.getY()-range)
				.setInteger("maxy", loc.getY()+range)
				.setString("smallflag", ShipTypes.SF_SEHR_KLEIN)
				.list();
			for( Iterator<?> iter2=sList.iterator(); iter2.hasNext(); ) {
				Ship s = (Ship)iter2.next();
				
				Location sLoc = s.getLocation();
				
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
				
				ShipTypeData st = s.getTypeData();
				if( st.hasFlag(ShipTypes.SF_SEHR_KLEIN) ) {
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
						for( Map.Entry<User, Integer> entry: usercount.entrySet() ) {
							User userobj = entry.getKey();
							Integer count = entry.getValue();
							appendStr(maptext, lastX, lastY, count+" Schiff"+(count==1?"":"e")+" "+userobj.getName()+"\n");
						}
					}
					lastX = sLoc.getX();
					lastY = sLoc.getY(); 	
					enemycount = 0;
					allycount = 0;
					usercount.clear();
				}
				
				if( this.usedUser.equals(s.getOwner()) ) {
					continue;	
				}
				
				if( !usercount.containsKey(s.getOwner()) ) {
					usercount.put(s.getOwner(), 1);
				}
				else {
					usercount.put(s.getOwner(), usercount.get(s.getOwner())+1);
				}
				
				if( (this.ally != null) && this.ally.equals(s.getOwner().getAlly()) ) {
					if( (s.getDocked().length() == 0 || (s.getDocked().charAt(0) != 'l')) ) {
						map[sLoc.getX()][sLoc.getY()] |= OBJECT_FLEET_ALLY;
						allycount++;
					}
				}
				else {
					if( (s.getDocked().length() == 0 || (s.getDocked().charAt(0) != 'l')) ) {
						map[sLoc.getX()][sLoc.getY()] |= OBJECT_FLEET_ENEMY;
						enemycount++;
					}
				}
			}
			
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
					for( Map.Entry<User, Integer> entry: usercount.entrySet() ) {
						User userobj = entry.getKey();
						Integer count = entry.getValue();
						appendStr(maptext, lastX, lastY, count+" Schiff"+(count==1?"":"e")+" "+userobj.getName()+"\n");
					}
				}
			}
		}
		
		lrsScanSectors = null;
		scannedSectors = null;
		
		//--------------------------------------
		// Karte ausgeben
		//--------------------------------------

		try {
			Writer echo = getContext().getResponse().getWriter();
			// Senden wir ersteinmal die Protokoll-Version
			echo.append((char)PROTOCOL_VERSION);
			echo.append((char)PROTOCOL_MINOR_VERSION);
	
			if( this.usedUser.getId() < 0 ) {
				echo.append((char)1);
			}
			else {
				echo.append((char)0);
			}
			echo.append(getStarmapValue(Math.abs(this.usedUser.getId())));
			
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
			List<?> lobjectList = db.createQuery("from Base " +
					"where system= :sys and size>0 and (x between 1 and :width) and (y between 1 and :height)")
				.setInteger("sys", this.system)
				.setInteger("width", sys.getWidth())
				.setInteger("height", sys.getHeight())
				.list();
			for( Iterator<?> iter=lobjectList.iterator(); iter.hasNext(); ) {
				Base lobject = (Base)iter.next();
				
				echo.append((char)ADDDATA_LARGE_OBJECT);
						
				echo.append(getStarmapValue(lobject.getX()-1));
			
				echo.append(getStarmapValue(lobject.getY()-1));
				
				echo.append(getStarmapValue(lobject.getSize()));
				
				String image = "kolonie"+lobject.getKlasse()+"_lrs";
				image = new String(image.getBytes("ISO-8859-1"), "ISO-8859-1");
							
				int length = image.length();
						
				echo.append(getStarmapValue( length ));
						
				echo.append(image);
			}
		}
		catch( UnsupportedEncodingException e ) {
			log.error("Kann Sternenkarte nicht kodieren", e);
		}
	}
}
