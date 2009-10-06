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
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.caches.CacheManager;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.ShipWerft;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Required;

/**
 * Fueht spezielle Admin-Kommandos aus.
 * @author Christopher Jung
 *
 */
@Configurable
public class AdminCommands {
	private static final Log log = LogFactory.getLog(AdminCommands.class);
	
	private Configuration config;
	
	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Autowired @Required
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
	/**
	 * Fueht das angegebene Admin-Kommando aus.
	 * @param cmd Das Kommando
	 * @return Die vom Kommando generierte Ausgabe
	 */
	public String executeCommand( String cmd ) {
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
		else if( command[0].equals("recalculateshipmodules"))
		{
			output = cmdRecalcShipModules(context, command);
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
		else if( command[0].equals("tick") ) {
			output = cmdTick(context, command);
		}
		else {
			output = "Unbekannter Befehl "+command[0];
		}
		
		if( output.length() == 0 ) {
			output = "1";
		}
		
		return output;
	}
	
	private String cmdTick(Context context, String[] command)
	{
		if( command.length < 2 ) {
			return "";
		}
		if( "regular".equals(command[1]) ) {
			if( "run".equals(command[2]) ) {
				if( command.length > 3 ) {
					String only = command[3];
					if( !only.startsWith("net.driftingsouls") ) {
						only = "net.driftingsouls.ds2.server.tick.regular."+only;
					}
					try
					{
						Class<? extends TickController> clazz = Class.forName(only)
							.asSubclass(TickController.class);
						new TickAdminCommand().runRegularTick(clazz);
					}
					catch( ClassNotFoundException e )
					{
						return "Unbekannter Teiltick";
					}
				}
				else {
					new TickAdminCommand().runRegularTick();
				}
				return "Tick wird ausgefuehrt";
			}
		}
		else if( "rare".equals(command[1]) ) {
			if( "run".equals(command[2]) ) {
				if( command.length > 3 ) {
					String only = command[3];
					if( !only.startsWith("net.driftingsouls") ) {
						only = "net.driftingsouls.ds2.server.tick.rare."+only;
					}
					try
					{
						Class<? extends TickController> clazz = Class.forName(only)
							.asSubclass(TickController.class);
						new TickAdminCommand().runRareTick(clazz);
					}
					catch( ClassNotFoundException e )
					{
						return "Unbekannter Teiltick";
					}
				}
				else {
					new TickAdminCommand().runRareTick();
				}
				return "Raretick wird ausgefuehrt";
			}
		}
		return "Unbekannter befehl";
	}

	private String cmdClearCaches( Context context, String[] command ) {
		String output = "Caches geleert";
		
		CacheManager.getInstance().clearCaches();
		
		return output;
	}
	
	private String cmdEditShip( Context context, String[] command ) {
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
	
	private String cmdAddResource(Context context, String[] command) {
		String output = "";
		
		String oid = command[1];
		ResourceID resid = null;
		try {
			resid = Resources.fromString(command[2]);
		}
		catch( RuntimeException e ) {
			return "Die angegebene Resource ist ungueltig";
		}
		
		if( !NumberUtils.isNumber(command[3]) ) {
			return "Menge ungueltig";
		}
		long count = Long.parseLong(command[3]);
		
		org.hibernate.Session db = context.getDB();
		
		if( !NumberUtils.isNumber(oid.substring(1)) ) {
			return "ID ungueltig";
		}
		
		Cargo cargo = null;
		if( oid.startsWith("b") ) {
			Base base = (Base)db.get(Base.class, Integer.parseInt(oid.substring(1)));
			if( base == null ) {
				return "Objekt existiert nicht";
			}
			cargo = new Cargo(base.getCargo());
		}
		else {
			Ship ship = (Ship)db.get(Ship.class, Integer.parseInt(oid.substring(1)));
			if( ship == null ) {
				return "Objekt existiert nicht";
			}
			cargo = new Cargo(ship.getCargo());
		}
		
		cargo.addResource( resid, count );

		if( oid.startsWith("s") ) {
			Ship ship = (Ship)db.get(Ship.class, Integer.parseInt(oid.substring(1)));
			ship.setCargo(cargo);
			ship.recalculateShipStatus();
		}
		else {
			Base base = (Base)db.get(Base.class, Integer.parseInt(oid.substring(1)));
			base.setCargo(cargo);
		}
		
		return output;
	}
	
	private String cmdQuest(Context context, String[] command) {
		String output = "";
		org.hibernate.Session db = context.getDB();
		
		String cmd = command[1];
		if( cmd.equals("end") ) {
			int rqid = Integer.parseInt(command[2]);
			
			ScriptEngine scriptparser = context.get(ContextCommon.class).getScriptParser("DSQuestScript");
			final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
			
			scriptparser.getContext().setErrorWriter(new NullLogger());
			
			RunningQuest runningquest = (RunningQuest)db.get(RunningQuest.class, rqid);
			
			if( !runningquest.getUninstall().isEmpty() ) {
				engineBindings.put("USER", runningquest.getUser().getId());
				engineBindings.put("QUEST", "r"+rqid);
				engineBindings.put("_PARAMETERS", "0");
				
				try {
					scriptparser.eval( runningquest.getUninstall() );
				}
				catch( ScriptException e ) {
					throw new RuntimeException(e);
				}
			}
			
			db.delete(runningquest);
		}
		else if( cmd.equals("list") ) {
			output = "Laufende Quests:\n";
			List<?> rquestList = db.createQuery("from RunningQuest rq inner join fetch rq.quest").list();
			for( Iterator<?> iter=rquestList.iterator(); iter.hasNext(); ) {
				RunningQuest rquest = (RunningQuest)iter.next();
				
				output += "* "+rquest.getId()+" - "+rquest.getQuest().getName()+" ("+rquest.getQuest().getId()+") - userid "+rquest.getUser().getId()+"\n";
			}
		}
		else {
			output = "Unknown quest sub-command >"+cmd+"<";	
		}
		return output;
	}
	
	private String cmdBattle(Context context, String[] command) {
		String output = "";
		
		String cmd = command[1];
		if( cmd.equals("end") ) {
			int battleid = Integer.parseInt( command[2] );	
			org.hibernate.Session db = context.getDB();
			
			Battle battle = (Battle)db.get(Battle.class, battleid);
		
			if( battle == null ) {
				return "Die angegebene Schlacht existiert nicht\n";
			}

			User sourceUser = (User)context.getDB().get(User.class, -1);
			
			PM.send(sourceUser, battle.getCommander(0).getId(), "Schlacht beendet", "Die Schlacht bei "+battle.getLocation()+" wurde durch die Administratoren beendet");
			PM.send(sourceUser, battle.getCommander(1).getId(), "Schlacht beendet", "Die Schlacht bei "+battle.getLocation()+" wurde durch die Administratoren beendet");
		
			battle.load(battle.getCommander(0), null, null, 0);
			battle.endBattle(0, 0, false);
		}
		else {
			output = "Unknown battle sub-command >"+cmd+"<";	
		}
		return output;
	}
	
	private String cmdDestroyShip(Context context, String[] command) {
		String output = "";
		
		List<String> sql = new ArrayList<String>();
		for( int i=1; i < command.length; i++ ) {
			if( command[i].equals("sector") ) {
				i++;
				Location sector = Location.fromString(command[i]);
				sql.add("system="+sector.getSystem()+" and x="+sector.getX()+" and y="+sector.getY());	
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
			org.hibernate.Session db = context.getDB();
			
			List<?> ships = db.createQuery("from Ship where "+Common.implode(" and ",sql)).list();
			int num = ships.size();
			for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
				Ship aship = (Ship)iter.next();
				aship.destroy();
			}
			
			output = num+" Schiffe entfernt";
		}
		else {
			output = "Bitte Eingabe konkretisieren (Keine Einschraenkungen vorhanden)";	
		}
		
		return output;
	}
	
	private void checkImage( String baseimg, String fleet ) {
		if( new File(baseimg+fleet+"+.png").isFile() ) {
			return;
		}
		
		try {
			Font font = null;
			if( !new File(config.get("ABSOLUTE_PATH")+"data/bnkgothm.ttf").isFile() ) {
				log.warn("bnkgothm.ttf nicht auffindbar");
				font = Font.getFont("bankgothic md bt");
				if( font == null ) {
					font = Font.getFont("courier");
				}
			}
			else {
				font = Font.createFont(Font.TRUETYPE_FONT, 
						new File(config.get("ABSOLUTE_PATH")+"data/bnkgothm.ttf"));
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
			log.error(e, e);
		}
		catch( IOException e ) {
			log.error(e, e);
		}
	}
	
	private String splitplanetimgs( String baseimg, String targetname ) {
		String datadir = config.get("ABSOLUTE_PATH")+"data/starmap/";
		
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
	
	private String cmdBuildImgs(Context context, String[] command) {
		String output = "";
		
		String cmd = command[1];
		if( cmd.equals("starmap") ) {
			String img = command[2];	
			
			boolean sizedimg = false;
			int imgcount = 0;
			
			String path = config.get("ABSOLUTE_PATH")+"data/starmap/"+img+"/"+img;
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
	
	private String cmdExecTask(Context context, String[] command) {
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
	
	private String cmdRecalcShipModules(Context context, String[] command) {
		String output = "";
		org.hibernate.Session db = context.getDB();
		
		ScrollableResults ships = db.createQuery("from Ship as s left join fetch s.modules " +
												 "where s.id>0 order by s.owner,s.docked,s.shiptype asc")
												 .setCacheMode(CacheMode.IGNORE)
												 .scroll(ScrollMode.FORWARD_ONLY);
		
		int count = 0;
		while(ships.next())
		{
			Ship ship = (Ship) ships.get(0);
			ship.recalculateModules();
			count++;
			
			if(count % 20 == 0)
			{
				db.flush();
				HibernateFacade.evictAll(db, Ship.class, ShipModules.class, Offizier.class);
			}
		}
		
		output = "Es wurden "+count+" Schiffe neu berechnet.";
		return output;
	}
}
