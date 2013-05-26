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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.tick.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fueht spezielle Admin-Kommandos aus.
 * @author Christopher Jung
 *
 */
public class AdminCommands {
	private Map<String,Class<? extends Command>> cmds = new HashMap<String,Class<? extends Command>>();

	/**
	 * Konstruktor.
	 */
	public AdminCommands()
	{
		cmds.put("editship", EditShipCommand.class);
		cmds.put("editfleet", EditFleetCommand.class);
		cmds.put("recalculateshipmodules", RecalcShipModulesCommand.class);
		cmds.put("addresource", AddResourceCommand.class);
		cmds.put("quest", QuestCommand.class);
		cmds.put("battle", BattleCommand.class);
		cmds.put("destroyship", DestroyShipCommand.class);
		cmds.put("buildimgs", BuildImgsCommand.class);
		cmds.put("exectask", ExecTaskCommand.class);
		cmds.put("tick", TickCommand.class);
	}

	/**
	 * Gibt zu einem Admin-Befehl eine Liste moeglicher Vervollstaendigungen
	 * zurueck. Die Vorschlaege koennen dabei beschreibender Natur sein (Dokumentation).
	 * @param cmd Der Befehl
	 * @return Die moeglichen Vervollstaendigungen
	 */
	public List<String> autoComplete(String cmd) {
		List<String> result = new ArrayList<String>();

		String[] command = StringUtils.split(cmd, ' ');
		if( command.length == 0 ) {
			return result;
		}

		if( !cmds.containsKey(command[0]) ) {
			return new ArrayList<String>(cmds.keySet());
		}

		List<String> subAutoCompletes;
		try
		{
			subAutoCompletes = cmds.get(command[0]).newInstance().autoComplete(command);
		}
		catch( InstantiationException e )
		{
			throw new IllegalStateException(e);
		}
		catch( IllegalAccessException e )
		{
			throw new IllegalStateException(e);
		}

		for( String ac : subAutoCompletes ) {
			result.add(command[0]+" "+ac);
		}

		return result;
	}

	/**
	 * Fueht das angegebene Admin-Kommando aus.
	 * @param cmd Das Kommando
	 * @return Die vom Kommando generierte Ausgabe
	 */
	public String executeCommand( String cmd ) {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		if( (user == null) || !context.hasPermission("admin", "commands") ) {
			return "-1";
		}

		String output = "";
		String[] command = StringUtils.split(cmd, ' ');

		if( cmds.containsKey(command[0]) ) {
			try
			{
				Command cmdExecuter = cmds.get(command[0]).newInstance();
				context.autowireBean(cmdExecuter);
				output = cmdExecuter.execute(context, command);
			}
			catch( InstantiationException e )
			{
				output = "Fehler beim Ausfuehren von Befehl "+command[0];
			}
			catch( IllegalAccessException e )
			{
				output = "Fehler beim Ausfuehren von Befehl "+command[0];
			}
		}
		else {
			output = "Unbekannter Befehl "+command[0];
		}

		if( output.length() == 0 ) {
			output = "1";
		}

		return output;
	}

	protected static interface Command {
		public String execute(Context context, String[] command);
		public List<String> autoComplete(String[] command);
	}

	protected static class TickCommand implements Command {
		@Autowired
		private TickAdminCommand tickAdminCommand;

		@Override
		public String execute(Context context, String[] command)
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
							tickAdminCommand.runRegularTick(clazz);
						}
						catch( ClassNotFoundException e )
						{
							return "Unbekannter Teiltick";
						}
					}
					else {
						tickAdminCommand.runRegularTick();
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
							tickAdminCommand.runRareTick(clazz);
						}
						catch( ClassNotFoundException e )
						{
							return "Unbekannter Teiltick";
						}
					}
					else {
						tickAdminCommand.runRareTick();
					}
					return "Raretick wird ausgefuehrt";
				}
			}
			return "Unbekannter befehl";
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList("<regular|rare> run [TickClassName]");
		}
	}

	protected static class EditFleetCommand implements Command {
		@Override
		public String execute( Context context, String[] command ) {
			String output = "";
			org.hibernate.Session db = context.getDB();

			ShipFleet fleet = null;
			if( NumberUtils.isNumber(command[1]) ) {
				int fid = Integer.parseInt(command[1]);
				fleet = (ShipFleet)db.get(ShipFleet.class, fid);
			}
			else if( command[1].length() > 1 && command[1].charAt(0) == 's' )
			{
				String id = command[1].substring(1);
				if( NumberUtils.isNumber(id) )
				{
					Ship ship = (Ship)db.get(Ship.class, Integer.valueOf(id));
					if( ship != null )
					{
						fleet = ship.getFleet();
					}
				}
			}

			if( fleet == null ) {
				return "Flotte '"+command[1]+"' nicht gefunden";
			}

			if( command[2].equals("heat") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Ueberhitzung ungueltig";
				}
				for( Ship ship : fleet.getShips() )
				{
					ship.setHeat(Integer.parseInt(command[3]));
				}
			}
			else if( command[2].equals("engine") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Antrieb ungueltig";
				}
				for( Ship ship : fleet.getShips() )
				{
					ship.setEngine(Integer.parseInt(command[3]));
				}
			}
			else if( command[2].equals("weapons") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Waffen ungueltig";
				}
				for( Ship ship : fleet.getShips() )
				{
					ship.setWeapons(Integer.parseInt(command[3]));
				}
			}
			else if( command[2].equals("jumptarget") ) {
				for( Ship ship : fleet.getShips() )
				{
					ship.setJumpTarget(command[3]);
				}
			}
			else if( command[2].equals("e") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Energie ungueltig";
				}
				for( Ship ship : fleet.getShips() )
				{
					ship.setEnergy(Integer.parseInt(command[3]));
				}
			}
			else if( command[2].equals("pos") ) {
				Location loc = Location.fromString(command[3]);

				for( Ship ship : fleet.getShips() ) {
					ship.setSystem(loc.getSystem());
					ship.setX(loc.getX());
					ship.setY(loc.getY());
					for( Ship lship : ship.getLandedShips() )
					{
						lship.setSystem(loc.getSystem());
						lship.setX(loc.getX());
						lship.setY(loc.getY());
					}
					for( Ship lship : ship.getDockedShips() )
					{
						lship.setSystem(loc.getSystem());
						lship.setX(loc.getX());
						lship.setY(loc.getY());
					}
				}
			}
			else if( command[2].equals("hull") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Huelle ungueltig";
				}
				for( Ship ship : fleet.getShips() )
				{
					ship.setHull(Integer.parseInt(command[3]));
				}
			}
			else if( command[2].equals("shields") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Schilde ungueltig";
				}
				for( Ship ship : fleet.getShips() )
				{
					ship.setShields(Integer.parseInt(command[3]));
				}
			}
			else if( command[2].equals("crew") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Crew ungueltig";
				}
				for( Ship ship : fleet.getShips() )
				{
					ship.setCrew(Integer.parseInt(command[3]));
				}
			}
			else if( command[2].equals("info") ) {
				output += "Flotte: "+fleet.getId()+"\n";
				output += "Name: "+fleet.getName()+"\n";
				output += "Schiffe: "+fleet.getShips().size()+"\n";
			}
			else if( command[2].equals("additemmodule") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Slot ungueltig";
				}
				int slot = Integer.parseInt(command[3]);

				if( !NumberUtils.isNumber(command[4]) ) {
					return "Item-ID ungueltig";
				}
				int itemid = Integer.parseInt(command[4]);
				Item item = (Item)db.get(Item.class, itemid);

				if( (item == null) || (item.getEffect().getType() != ItemEffect.Type.MODULE) ) {
					return "Das Item passt nicht";
				}

				for( Ship ship : fleet.getShips() ) {
					ship.addModule( slot, ModuleType.ITEMMODULE, Integer.toString(itemid) );

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
						ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
							.setInteger("ship", ship.getId())
							.uniqueResult();

						if( werft == null ) {
							werft = new ShipWerft(ship);
							db.persist(werft);
						}
					}
				}

				output = "Modul '"+item.getName()+"'@"+slot+" eingebaut\n";
			}
			else {
				output = "Unknown editship sub-command >"+command[2]+"<";
			}
			for( Ship ship : fleet.getShips() )
			{
				ship.recalculateShipStatus();
			}

			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			if( command.length == 1 ) {
				return Arrays.asList("<fleetId|s<ShipId>> ...");
			}

			List<String> validCommands = Arrays.asList(
					"heat","engine","weapons",
					"jumptarget","e","pos",
					"hull","shields","crew",
					"info","additemmodule");

			if( command.length == 2 ||
					!validCommands.contains(command[2]) ) {
				List<String> autoComplete = new ArrayList<String>();
				for( String cmd : validCommands )
				{
					autoComplete.add(autoCompleteFleet(command)+" "+cmd+" ... ");
				}
				return autoComplete;
			}

			if( "pos".equals(command[2]) ) {
				return Arrays.asList(autoCompleteFleet(command)+" "+command[2]+" <location>");
			}
			if( "info".equals(command[2]) ) {
				return Arrays.asList(autoCompleteFleet(command)+" "+command[2]);
			}
			if( "additemmodule".equals(command[2]) ) {
				return Arrays.asList(autoCompleteFleet(command)+" "+command[2]+" <slot> "+autoCompleteModuleItem(command));
			}

			return Arrays.asList(autoCompleteFleet(command)+" "+command[2]+" <value>");
		}

		private String autoCompleteModuleItem(String[] command)
		{
			if( command.length < 5 || !NumberUtils.isNumber(command[4]) )
			{
				return "<itemId (Integer)>";
			}
			org.hibernate.Session db = ContextMap.getContext().getDB();
			Item item = (Item)db.get(Item.class, Integer.valueOf(command[4]));
			if( item == null )
			{
				return "<itemId (Integer)>";
			}
			return "<"+item.getName()+" ("+item.getID()+")>";
		}

		private String autoCompleteFleet(String[] command)
		{
			String fleetLabel = "<fleetId|s<shipId>>";
			if( command.length > 1 && NumberUtils.isNumber(command[1]) )
			{
				org.hibernate.Session db = ContextMap.getContext().getDB();
				ShipFleet fleet = (ShipFleet)db.get(ShipFleet.class, Integer.valueOf(command[1]));
				if( fleet != null )
				{
					fleetLabel = "<"+fleet.getName()+" ("+fleet.getId()+")>";
				}
			}
			else if( command.length > 1 && command[1].length() > 2 && command[1].charAt(0) == 's')
			{
				String id = command[1].substring(1);
				if( NumberUtils.isNumber(id) )
				{
					org.hibernate.Session db = ContextMap.getContext().getDB();
					Ship ship = (Ship)db.get(Ship.class, Integer.valueOf(id));
					if( ship != null && ship.getFleet() != null )
					{
						fleetLabel = "<"+ship.getFleet().getName()+" ("+ship.getFleet().getId()+")>";
					}
				}
			}
			return fleetLabel;
		}
	}

	protected static class EditShipCommand implements Command {
		@Override
		public String execute( Context context, String[] command ) {
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
			else if( command[2].equals("engine") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Antrieb ungueltig";
				}
				ship.setEngine(Integer.parseInt(command[3]));
			}
			else if( command[2].equals("weapons") ) {
				if( !NumberUtils.isNumber(command[3]) ) {
					return "Waffen ungueltig";
				}
				ship.setWeapons(Integer.parseInt(command[3]));
			}
			else if( command[2].equals("jumptarget") ) {
				ship.setJumpTarget(command[3]);
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

				for( Ship lship : ship.getLandedShips() )
				{
					lship.setSystem(loc.getSystem());
					lship.setX(loc.getX());
					lship.setY(loc.getY());
				}
				for( Ship lship : ship.getDockedShips() )
				{
					lship.setSystem(loc.getSystem());
					lship.setX(loc.getX());
					lship.setY(loc.getY());
				}
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
				int itemid = Integer.parseInt(command[4]);
				Item item = (Item)db.get(Item.class, itemid);

				if( (item == null) || (item.getEffect().getType() != ItemEffect.Type.MODULE) ) {
					return "Das Item passt nicht";
				}

				ship.addModule( slot, ModuleType.ITEMMODULE, Integer.toString(itemid) );

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
					ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=:ship")
						.setInteger("ship", ship.getId())
						.uniqueResult();

					if( werft == null ) {
						werft = new ShipWerft(ship);
						db.persist(werft);
					}
				}

				output = "Modul '"+item.getName()+"'@"+slot+" eingebaut\n";
			}
			else {
				output = "Unknown editship sub-command >"+command[2]+"<";
			}
			ship.recalculateShipStatus();

			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			if( command.length == 1 ) {
				return Arrays.asList("<shipId> ...");
			}

			List<String> validCommands = Arrays.asList(
					"heat","engine","weapons",
					"jumptarget","e","pos",
					"hull","shields","crew",
					"info","additemmodule");

			if( command.length == 2 ||
					!validCommands.contains(command[2]) ) {
				List<String> autoComplete = new ArrayList<String>();
				for( String cmd : validCommands )
				{
					autoComplete.add(autoCompleteShip(command)+" "+cmd+" ... ");
				}
				return autoComplete;
			}

			if( "pos".equals(command[2]) ) {
				return Arrays.asList(autoCompleteShip(command)+" "+command[2]+" <location>");
			}
			if( "info".equals(command[2]) ) {
				return Arrays.asList(autoCompleteShip(command)+" "+command[2]);
			}
			if( "additemmodule".equals(command[2]) ) {
				return Arrays.asList(autoCompleteShip(command)+" "+command[2]+" <slot> "+autoCompleteModuleItem(command));
			}

			return Arrays.asList(autoCompleteShip(command)+" "+command[2]+" <value>");
		}

		private String autoCompleteModuleItem(String[] command)
		{
			if( command.length < 5 || !NumberUtils.isNumber(command[4]) )
			{
				return "<itemId (Integer)>";
			}
			org.hibernate.Session db = ContextMap.getContext().getDB();
			Item item = (Item)db.get(Item.class, Integer.valueOf(command[4]));
			if( item == null )
			{
				return "<itemId (Integer)>";
			}
			return "<"+item.getName()+" ("+item.getID()+")>";
		}

		private String autoCompleteShip(String[] command)
		{
			String shipLabel = "<shipId>";
			if( command.length > 1 && NumberUtils.isNumber(command[1]) )
			{
				org.hibernate.Session db = ContextMap.getContext().getDB();
				Ship ship = (Ship)db.get(Ship.class, Integer.valueOf(command[1]));
				if( ship != null )
				{
					shipLabel = "<"+ship.getName()+" ("+ship.getId()+")>";
				}
			}
			return shipLabel;
		}
	}

	protected static class AddResourceCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
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

		private String getItemAutoComplete(String[] command)
		{
			if( command.length < 3 )
			{
				return "<resId>";
			}

			Item item = null;
			try {
				ResourceID resid = Resources.fromString(command[2]);
				org.hibernate.Session db = ContextMap.getContext().getDB();
				item = (Item)db.get(Item.class, resid.getItemID());
			}
			catch( RuntimeException e ) {
				// EMPTY
			}
			if( item == null )
			{
				return "<resId>";
			}
			return "<"+item.getName()+" ("+item.getID()+")>";
		}

		private String getTargetAutoComplete(String[] command)
		{
			if( command.length < 2 || command[1].length() < 2 )
			{
				return "<(b|s)ObjektID>";
			}
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String id = command[1].substring(1);
			if( !NumberUtils.isNumber(id) )
			{
				return "<(b|s)ObjektID>";
			}
			char c = command[1].charAt(0);

			if( c == 'b' ) {
				Base base = (Base)db.get(Base.class, Integer.parseInt(id));
				if( base == null ) {
					return "<(b|s)ObjektID>";
				}
				return "<Basis "+base.getName()+" ("+base.getId()+")>";
			}
			else if( c == 's' ){
				Ship ship = (Ship)db.get(Ship.class, Integer.parseInt(id));
				if( ship == null ) {
					return "<(b|s)ObjektID>";
				}
				return "<Schiff "+ship.getName()+" ("+ship.getId()+")>";
			}
			else {
				return "<(b|s)ObjektID>";
			}
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList(getTargetAutoComplete(command)+" "+getItemAutoComplete(command)+" <Menge>");
		}
	}

	protected static class QuestCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
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

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList("end <QuestID>", "list");
		}
	}

	protected static class BattleCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
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

				PM.send(sourceUser, battle.getCommander(0).getId(), "Schlacht beendet", "Die Schlacht bei "+battle.getLocation().displayCoordinates(false)+" wurde durch die Administratoren beendet");
				PM.send(sourceUser, battle.getCommander(1).getId(), "Schlacht beendet", "Die Schlacht bei "+battle.getLocation().displayCoordinates(false)+" wurde durch die Administratoren beendet");

				battle.load(battle.getCommander(0), null, null, 0);
				battle.endBattle(0, 0, false);
			}
			else {
				output = "Unknown battle sub-command >"+cmd+"<";
			}
			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList("end <battleID>");
		}
	}

	protected static class DestroyShipCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
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

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList("[sector <location>|owner <userId>|fleet <fleetId>|type <shiptypeId>]+");
		}
	}

	protected static class BuildImgsCommand implements Command {
		private static final Log log = LogFactory.getLog(BuildImgsCommand.class);

		private void checkImage( String baseimg, String fleet ) {
			if( new File(baseimg+fleet+"+.png").isFile() ) {
				return;
			}

			try {
				Font font = null;
				if( !new File(Configuration.getSetting("ABSOLUTE_PATH")+"data/bnkgothm.ttf").isFile() ) {
					log.warn("bnkgothm.ttf nicht auffindbar");
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
				log.error(e, e);
			}
			catch( IOException e ) {
				log.error(e, e);
			}
		}

		private String splitplanetimgs( String baseimg, String targetname ) {
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

		@Override
		public String execute(Context context, String[] command) {
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

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList("starmap <imgName>", "splitplanetimg <imgPath> <targetDir>");
		}
	}

	protected static class ExecTaskCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
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

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList("<taskId> <MessageToSend>");
		}
	}

	protected static class RecalcShipModulesCommand implements Command {
		private static final Logger LOG = LogManager.getLogger(RecalcShipModulesCommand.class);

		@Override
		public String execute(Context context, String[] command) {
			String output;
			org.hibernate.Session db = context.getDB();

			final AtomicInteger count = new AtomicInteger(0);
			long start = System.currentTimeMillis();

			db.getTransaction().commit();

			List<Integer> ships = Common.cast(db
				.createQuery("select s.id from Ship as s join s.modules " +
					"where s.id>0 order by s.owner.id,s.docked,s.shiptype.id asc")
				.list());

			new EvictableUnitOfWork<Integer>("AdminCommand: RecalculateShipModules") {

				@Override
				public void doWork(Integer object) throws Exception
				{
					Ship ship = (Ship)getDB().get(Ship.class, object);
					ship.recalculateModules();
					count.incrementAndGet();

					if( count.get() % 1000 == 0 ) {
						LOG.info("Schiffe berechnet: "+count.get());
					}
				}
			}
			.setFlushSize(20)
			.executeFor(ships);

			db.beginTransaction();

			output = "Es wurden "+count.get()+" Schiffe in "+ (System.currentTimeMillis() - start)/1000d +" Sekunden neu berechnet.";
			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Arrays.asList("");
		}
	}
}
