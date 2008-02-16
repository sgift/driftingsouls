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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.caches.CacheManager;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.werften.ShipWerft;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * Fueht spezielle Admin-Kommandos aus
 * @author Christopher Jung
 *
 */
public class AdminCommands implements Loggable {
	/**
	 * Fueht das angegebene Admin-Kommando aus
	 * @param cmd Das Kommando
	 * @return Die vom Kommando generierte Ausgabe
	 */
	public static String executeCommand( String cmd ) {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		if( (user == null) || (user.getAccessLevel() < 20) ) {
			return "-1";
		}
		
		String output = "";
		String[] command = StringUtils.split(cmd, ' ');

		if( command[0].equals("editship") ) {
			output = cmdEditShip(context, command);
		}
		else if( command[0].equals("addresource") ) {
			output = cmdAddResource(context, command);
		}
		else if( command[0].equals("quest") ) {
			output = cmdQuest(context, command);
		}
		else if( command[0].equals("battle") ) {
			output = cmdBattle(context, command);
		}
		else if( command[0].equals("destroyship") ) {
			output = cmdDestroyShip(context, command);
		}
		else if( command[0].equals("buildimgs") ) {
			output = cmdBuildImgs(context, command);
		}
		else if( command[0].equals("exectask") ) {
			output = cmdExecTask(context, command);
		}
		else if( command[0].equals("clearcaches") ) {
			output = cmdClearCaches(context, command);
		}
		else {
			output = "Unbekannter Befehl "+command[0];
		}
		
		if( output.length() == 0 ) {
			output = "1";
		}
		
		return output;
	}
	
	private static String cmdClearCaches( Context context, String[] command ) {
		String output = "Caches geleert";
		
		CacheManager.getInstance().clearCaches();
		
		return output;
	}
	
	private static String cmdEditShip( Context context, String[] command ) {
		String output = "";
		org.hibernate.Session db = context.getDB();
		
		if( !NumberUtils.isNumber(command[1]) ) {
			return "Ungueltige Schiffs-ID";
		}
		
		int sid = Integer.parseInt(command[1]);
		
		Ship ship = (Ship)db.get(Ship.class, sid);
		if( ship == null ) {
			return "Schiff '"+sid+"' nicht gefunden";
		}
		
		if( command[2].equals("heat") ) {
			if( !NumberUtils.isNumber(command[3]) ) {
				return "Ueberhitzung ungueltig";
			}
			ship.setHeat(Integer.parseInt(command[3]));
		}	
		else if( command[2].equals("e") ) {
			if( !NumberUtils.isNumber(command[3]) ) {
				return "Energie ungueltig";
			}
			ship.setEnergy(Integer.parseInt(command[3]));
		}
		else if( command[2].equals("pos") ) {
			Location loc = Location.fromString(command[3]);
			
			ship.setSystem(loc.getSystem());
			ship.setX(loc.getX());
			ship.setY(loc.getY());
			
			db.createQuery("update Ship set system=?,x=?,y=? where id>0 and docked in (?,?)")
				.setInteger(0, loc.getSystem())
				.setInteger(1, loc.getX())
				.setInteger(2, loc.getY())
				.setString(3, Integer.toString(ship.getId()))
				.setString(4, "l "+ship.getId())
				.executeUpdate();	
		}
		else if( command[2].equals("hull") ) {
			if( !NumberUtils.isNumber(command[3]) ) {
				return "Huelle ungueltig";
			}
			ship.setHull(Integer.parseInt(command[3]));
		}
		else if( command[2].equals("shields") ) {
			if( !NumberUtils.isNumber(command[3]) ) {
				return "Schilde ungueltig";
			}
			ship.setShields(Integer.parseInt(command[3]));
		}
		else if( command[2].equals("crew") ) {
			if( !NumberUtils.isNumber(command[3]) ) {
				return "Crew ungueltig";
			}
			ship.setCrew(Integer.parseInt(command[3]));
		}
		else if( command[2].equals("lock") ) {
			ship.setLock(command[3].equals("null") ? null : command[3]);
		}
		else if( command[2].equals("info") ) {
			ShipTypeData shiptype = ship.getTypeData();
			
			output += "Schiff: "+sid+"\n";
			output += "Typ: "+shiptype.getNickname()+" ("+ship.getType()+")\n";
			output += "Besitzer: "+ship.getOwner().getId()+"\n";
			output += "Position: "+ship.getLocation()+"\n";
			output += "Energie: "+ship.getEnergy()+"\n";
			output += "Heat: "+ship.getHeat()+"\n";
			output += "Huelle: "+ship.getHull()+"\n";
			if( shiptype.getShields() > 0 ) {
				output += "Schilde: "+ship.getShields()+"\n";
			}	
			output += "Crew: "+ship.getCrew()+"\n";
			output += "Lock: "+ship.getLock()+"\n";
			output += "Status: "+ship.getStatus()+"\n";
			output += "Battle: "+ship.getBattle()+"\n";
		}
		else if( command[2].equals("additemmodule") ) {
			if( !NumberUtils.isNumber(command[3]) ) {
				return "Slot ungueltig";
			}
			int slot = Integer.parseInt(command[3]);
			
			if( !NumberUtils.isNumber(command[4]) ) {
				return "Item-ID ungueltig";
			}
			int item = Integer.parseInt(command[4]);
			
			if( (Items.get().item(item) == null) || (Items.get().item(item).getEffect().getType() != ItemEffect.Type.MODULE) ) {
				return "Das Item passt nicht";	
			}
				
			ship.addModule( slot, Modules.MODULE_ITEMMODULE, Integer.toString(item) );
									
			ShipTypeData shiptype = ship.getTypeData();
				
			if( ship.getHull() > shiptype.getHull() ) {
				ship.setHull(shiptype.getHull());	
			}
				
			if( ship.getShields() > shiptype.getShields() ) {
				ship.setShields(shiptype.getShields());	
			}
				
			if( ship.getEnergy() > shiptype.getEps() ) {
				ship.setEnergy(shiptype.getEps());	
			}
				
			if( ship.getCrew() > shiptype.getCrew() ) {
				ship.setCrew(shiptype.getCrew());	
			}
			
			if( shiptype.getWerft() != 0 ) {
				ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where shipid=?")
					.setInteger(0, ship.getId())
					.uniqueResult();
				
				if( werft == null ) {
					werft = new ShipWerft(ship);
					db.persist(werft);	
				}	
			}
					
			output = "Modul '"+Items.get().item(item).getName()+"'@"+slot+" eingebaut\n";
		}
		else {
			output = "Unknown editship sub-command >"+command[2]+"<";	
		}
		ship.recalculateShipStatus();
		
		return output;
	}
	
	private static String cmdAddResource(Context context, String[] command) {
		String output = "";
		
		String oid = command[1];
		ResourceID resid = Resources.fromString(command[2]);
		long count = Long.parseLong(command[3]);
		
		String table = "ships";
		if( oid.charAt(0) == 'b' ) {
			table = "bases";	
		}	
		
		Database db = context.getDatabase();
		SQLResultRow obj = db.first("SELECT id,cargo FROM "+table+" WHERE id>0 AND id="+Integer.parseInt(oid.substring(1)));
		
		if( obj.isEmpty() ) {
			return "Objekt existiert nicht";
		}
		
		Cargo cargo = new Cargo( Cargo.Type.STRING, obj.getString("cargo") );
		cargo.addResource( resid, count );
		
		db.update("UPDATE "+table+" SET cargo='"+cargo.save()+"' WHERE id="+Integer.parseInt(oid.substring(1)));
		
		if( table.equals("ships") ) {
			Ships.recalculateShipStatus( Integer.parseInt(oid.substring(1)) );	
		}
		
		return output;
	}
	
	private static String cmdQuest(Context context, String[] command) {
		String output = "";
		Database db = context.getDatabase();
		
		String cmd = command[1];
		if( cmd.equals("end") ) {
			int rqid = Integer.parseInt(command[2]);
			
			ScriptParser scriptparser = context.get(ContextCommon.class).getScriptParser(ScriptParser.NameSpace.QUEST);
			scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
			
			SQLResultRow runningquest = db.first("SELECT * FROM quests_running WHERE id="+rqid);
			
			if( runningquest.getString("uninstall").length() > 0 ) {
				scriptparser.setRegister("USER", runningquest.getInt("user"));
				scriptparser.setRegister("QUEST", "r"+rqid);
				
				scriptparser.executeScript( db, runningquest.getString("uninstall"), "0" );
			}	
			
			db.update("DELETE FROM quests_running WHERE id="+rqid);
		}
		else if( cmd.equals("list") ) {
			output = "Laufende Quests:\n";
			SQLQuery rquest = db.query("SELECT qr.id,qr.questid,qr.userid,q.name " +
					"FROM quests_running qr JOIN quests q ON qr.questid=q.id");
			while( rquest.next() ) {
				output += "* "+rquest.getInt("id")+" - "+rquest.getString("name")+" ("+rquest.getInt("questid")+") - userid "+rquest.getInt("userid")+"\n";
			}
			rquest.free();
		}
		else {
			output = "Unknown quest sub-command >"+cmd+"<";	
		}
		return output;
	}
	
	private static String cmdBattle(Context context, String[] command) {
		String output = "";
		
		String cmd = command[1];
		if( cmd.equals("end") ) {
			int battleid = Integer.parseInt( command[2] );	
			Database db = context.getDatabase();
			
			SQLResultRow battledata = db.first("SELECT commander1,commander2,x,y,system FROM battles WHERE id="+battleid);
		
			if( battledata.isEmpty() ) {
				return "Die angegebene Schlacht existiert nicht\n";
			}

			PM.send(context, -1, battledata.getInt("commander1"), "Schlacht beendet", "Die Schlacht bei "+Location.fromResult(battledata)+" wurde durch die Administratoren beendet");
			PM.send(context, -1, battledata.getInt("commander2"), "Schlacht beendet", "Die Schlacht bei "+Location.fromResult(battledata)+" wurde durch die Administratoren beendet");
		
			int comid = battledata.getInt("commander1");

			Battle battle = new Battle();
			battle.load(battleid, comid, 0, 0, 0);
			battle.endBattle(0, 0, false);
		}
		else {
			output = "Unknown battle sub-command >"+cmd+"<";	
		}
		return output;
	}
	
	private static String cmdDestroyShip(Context context, String[] command) {
		String output = "";
		
		List<String> sql = new ArrayList<String>();
		for( int i=1; i < command.length; i++ ) {
			if( command[i].equals("sector") ) {
				i++;
				Location sector = Location.fromString(command[i]);
				sql.add("system="+sector.getSystem()+" AND x="+sector.getX()+" AND y="+sector.getY());	
			}
			else if( command[i].equals("owner") ) {
				i++;
				int owner = Integer.parseInt(command[i]);
				sql.add("owner="+owner);
			}
			else if( command[i].equals("fleet") ) {
				i++;
				int fleet = Integer.parseInt(command[i]);
				sql.add("fleet="+fleet);
			}
			else if( command[i].equals("type") ) {
				i++;
				int type = Integer.parseInt(command[i]);
				sql.add("type="+type);
			}
		}
		if( sql.size() > 0 ) {
			Database db = context.getDatabase();
			
			SQLQuery sid = db.query("SELECT id FROM ships WHERE "+Common.implode(" AND ",sql));
			int num = sid.numRows();
			while( sid.next() ) {
				Ships.destroy(sid.getInt("id"));
			}
			sid.free();
			
			output = num+" Schiffe entfernt";
		}
		else {
			output = "Bitte Eingabe konkretisieren (Keine Einschraenkungen vorhanden)";	
		}
		
		return output;
	}
	
	private static void checkImage( String baseimg, String fleet ) {
		if( new File(baseimg+fleet+"+.png").isFile() ) {
			return;
		}
		
		try {
			Font font = null;
			if( !new File(Configuration.getSetting("ABSOLUTE_PATH")+"data/bnkgothm.ttf").isFile() ) {
				LOG.warn("bnkgothm.ttf nicht auffindbar");
				font = Font.getFont("bankgothic md bt");
				if( font == null ) {
					font = Font.getFont("courier");
				}
			}
			else {
				font = Font.createFont(Font.TRUETYPE_FONT, 
						new File(Configuration.getSetting("ABSOLUTE_PATH")+"data/bnkgothm.ttf"));
			}
			
			BufferedImage baseImage = ImageIO.read(new FileInputStream(baseimg+".png"));
			BufferedImage image = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			
			Color red = new Color(255,95,95);
			Color green = new Color(55,255,55);
			Color blue = new Color(127,146,255);
			
			Graphics2D g = image.createGraphics();
			g.drawImage(baseImage, 0, 0, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight(), null);
			
			g.setFont(font.deriveFont(12f));
	
			String[] fleets = StringUtils.splitPreserveAllTokens(fleet, '_');
			if( fleets.length >= 4 ) {
				g.setColor(green);
				g.drawString("F", 0, 15);
				g.setColor(blue);
				g.drawString("F", 8, 15);
				g.setColor(red);
				g.drawString("F", 16, 15);
			}
			else if( fleets.length == 3 ) {
				Color textcolor = blue;
				if( Common.inArray("fo",fleets) ) {
					textcolor = green;
				}
				g.setColor(textcolor);
				g.drawString("F", 4, 15);
	
				textcolor = blue;
				if( Common.inArray("fe",fleets) ) {
					textcolor = red;
				}
	
				g.setColor(textcolor);
				g.drawString("F", 12, 15);
			}
			else if( fleets.length == 2 ) {
				Color textcolor = red;
				
				if( fleets[1].equals("fo") )  {
					textcolor = green;
				}
				else if( fleets[1].equals("fa") ) {
					textcolor = blue;
				}
	
				g.setColor(textcolor);
				g.drawString("F", 8, 15);
			}
			
			g.dispose();
	
			ImageIO.write(image, "png", new File(baseimg+fleet+".png"));
		
		}
		catch( FontFormatException e ) {
			LOG.error(e, e);
		}
		catch( IOException e ) {
			LOG.error(e, e);
		}
	}
	
	private static String splitplanetimgs( String baseimg, String targetname ) {
		String datadir = Configuration.getSetting("ABSOLUTE_PATH")+"data/starmap/";
		
		baseimg = datadir+baseimg;
		targetname = datadir+targetname;
		
		if( !new File(baseimg+".png").isFile() ) {
			return "FATAL ERROR: bild existiert nicht ("+baseimg+".png)<br />\n";
		}
		
		try {
			BufferedImage image = ImageIO.read(
					new BufferedInputStream(
							AdminCommands.class.getClassLoader().getResourceAsStream(baseimg+".png")
					)
			);
			
			int width = image.getWidth() / 25;
			int height = image.getHeight() / 25;
			if( width != height ) {
				return "FATAL ERROR: ungueltige Bildgroesse<br />\n";	
			}
			
			int size = (width - 1) / 2;
			int cx = size + 1;
			int cy = size + 1;
	
			int index = 0;
			for( int y=0; y < height; y++ ) {
				for( int x=0; x < width; x++ ) {
					if( !new Location(0, cx, cy).sameSector( size, new Location(0, x+1, y+1), 0 ) ) {
						continue;	
					}
					BufferedImage img = new BufferedImage(25,25, image.getType());
					Graphics2D g = img.createGraphics();
					g.drawImage(image, 0, 0, 25, 25, x*25, y*25, x*25+25, y*25+25, null);
					
					g.dispose();
					
					ImageIO.write(img, "png", new File(targetname+index+".png"));
					
					index++;
				}
			}
		}
		catch( IOException e ) {
			return e.toString();
		}
		
		return "";
	}
	
	private static String cmdBuildImgs(Context context, String[] command) {
		String output = "";
		
		String cmd = command[1];
		if( cmd.equals("starmap") ) {
			String img = command[2];	
			
			boolean sizedimg = false;
			int imgcount = 0;
			
			String path = Configuration.getSetting("ABSOLUTE_PATH")+"data/starmap/"+img+"/"+img;
			if( !new File(path+"0.png").isFile() ) {
				if( !new File(path+".png").isFile() ) {
					return "Unbekannte Grafik >"+img+"<";
				}
			}
			else {
				sizedimg = true;
			}
			
			while( true ) {
				for( int i=0; i < 8; i++ ) {
					String fleet = "";
				
					if( (i & 4) != 0 ) {
						fleet += "_fo";	
					}
					if( (i & 2) != 0 ) {
						fleet += "_fa";	
					}
					if( (i & 1) != 0 ) {
						fleet += "_fe";	
					}
				
					if( fleet.length() == 0 ) {
						continue;	
					}
					
					if( new File(path+(sizedimg ? imgcount : "")+fleet+".png").isFile() ) {
						new File(path+(sizedimg ? imgcount : "")+fleet+".png").delete();
					}
					
					checkImage(path+(sizedimg ? imgcount : ""),fleet);
				}
				if( sizedimg ) {
					imgcount++;
					if( !new File(path+(sizedimg ? imgcount : "")+".png").isFile() ) {	
						break;
					}	
				}
				else {
					break;
				}
			}
		}
		else if( cmd.equals("splitplanetimg") ) {
			String img = command[2];
			String target = command[3];
			
			output = splitplanetimgs( img, target );
		}
		else {
			output = "Unbekannter Befehl "+cmd; 
		}
		
		return output;
	}
	
	private static String cmdExecTask(Context context, String[] command) {
		String output = "";
		
		String taskid = command[1];
		String message = command[2];
		
		if( Taskmanager.getInstance().getTaskByID(taskid) != null ) {
			Taskmanager.getInstance().handleTask(taskid, message);
			output = "Task ausgef&uuml;hrt";
		}
		else {
			output = "Keine g&uuml;ltige TaskID";
		}
		
		return output;
	}
}
