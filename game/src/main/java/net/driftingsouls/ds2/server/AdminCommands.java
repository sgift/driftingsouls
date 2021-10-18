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
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.DismantlingService;
import net.driftingsouls.ds2.server.services.FleetMgmtService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.TaskManager;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitType;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Cache;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Fueht spezielle Admin-Kommandos aus.
 * @author Christopher Jung
 *
 */
@Component
@Lazy
public class AdminCommands implements ApplicationContextAware {
	private final Map<String,Class<? extends Command>> cmds = new HashMap<>();

	@PersistenceContext
	private EntityManager em;

	private ApplicationContext applicationContext;

	private final BattleService battleService;
	private final ShipService shipService;
	private final PmService pmService;
	private final LocationService locationService;
	private final FleetMgmtService fleetMgmtService;
	private final DismantlingService dismantlingService;
	private final ShipActionService shipActionService;

	public AdminCommands(BattleService battleService, ShipService shipService, PmService pmService, LocationService locationService, FleetMgmtService fleetMgmtService, DismantlingService dismantlingService, ShipActionService shipActionService)
	{
		this.shipService = shipService;
		this.pmService = pmService;
		this.locationService = locationService;
		this.fleetMgmtService = fleetMgmtService;
		this.dismantlingService = dismantlingService;
		this.shipActionService = shipActionService;
		Context context = ContextMap.getContext();
		if( context.hasPermission(WellKnownAdminPermission.EDIT_SHIP) )
		{
			cmds.put("editship", EditShipCommand.class);
			cmds.put("editfleet", EditFleetCommand.class);
			cmds.put("recalculateshipmodules", RecalcShipModulesCommand.class);
			cmds.put("destroyship", DestroyShipCommand.class);
		}
		if( context.hasPermission(WellKnownAdminPermission.EDIT_SHIP) || context.hasPermission(WellKnownAdminPermission.EDIT_BASES) )
		{
			cmds.put("addresource", AddResourceCommand.class);
			cmds.put("addunits", AddUnitsCommand.class);
		}

		if( context.hasPermission(WellKnownAdminPermission.BATTLE_END) )
		{
			cmds.put("battle", BattleCommand.class);
		}
		if( context.getActiveUser().isAdmin() )
		{
			cmds.put("buildimgs", BuildImgsCommand.class);
			cmds.put("exectask", ExecTaskCommand.class);
			cmds.put("tick", TickCommand.class);
			cmds.put("clearcache", ClearCache.class);
			cmds.put("cleartilecache", ClearSystemTileCache.class);
		}
		this.battleService = battleService;
	}

	/**
	 * Gibt zu einem Admin-Befehl eine Liste moeglicher Vervollstaendigungen
	 * zurueck. Die Vorschlaege koennen dabei beschreibender Natur sein (Dokumentation).
	 * @param cmd Der Befehl
	 * @return Die moeglichen Vervollstaendigungen
	 */
	public List<String> autoComplete(String cmd) {
		List<String> result = new ArrayList<>();

		String[] command = StringUtils.split(cmd, ' ');
		if( command.length == 0 ) {
			return result;
		}

		if( !cmds.containsKey(command[0]) ) {
			return new ArrayList<>(cmds.keySet());
		}

		List<String> subAutoCompletes;
		try
		{
			subAutoCompletes = applicationContext.getBean(cmds.get(command[0])).autoComplete(command);
		}
		catch( BeansException e )
		{
			throw new IllegalStateException(e);
		}

		result.addAll(subAutoCompletes.stream().map(ac -> command[0] + " " + ac).collect(Collectors.toList()));

		return result;
	}

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public static class CommandFailedException extends RuntimeException
	{
		public CommandFailedException(String message)
		{
			super(message);
		}
	}

	@ViewModel
	public static class AdminCommandResultViewModel
	{
		public final String message;
		public final boolean success;

		public AdminCommandResultViewModel(String message, boolean success)
		{
			this.message = message;
			this.success = success;
		}
	}

	/**
	 * Fueht das angegebene Admin-Kommando aus.
	 * @param cmd Das Kommando
	 * @return Die vom Kommando generierte Ausgabe
	 */
	public AdminCommandResultViewModel executeCommand( String cmd ) {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		if( (user == null) || !context.hasPermission(WellKnownAdminPermission.CONSOLE) ) {
			return new AdminCommandResultViewModel("Keine Berechtigung", false);
		}

		String output;
		String[] command = StringUtils.split(cmd, ' ');

		if( cmds.containsKey(command[0]) ) {
			try
			{
				Command commandExecutor = cmds.get(command[0]).getDeclaredConstructor().newInstance();
				context.autowireBean(commandExecutor);
				output = commandExecutor.execute(context, command);
			}
			catch( CommandFailedException e )
			{
				return new AdminCommandResultViewModel(e.getMessage(), false);
			}
			catch( InstantiationException | IllegalAccessException | RuntimeException | NoSuchMethodException | InvocationTargetException e )
			{
				return new AdminCommandResultViewModel("Fehler: "+e.getClass().getName()+": "+e.getMessage(), false);
			}
		}
		else {
			return new AdminCommandResultViewModel("Unbekannter Befehl", false);
		}

		if( output.length() == 0 ) {
			return new AdminCommandResultViewModel("ok", true);
		}

		return new AdminCommandResultViewModel(output, true);
	}

	protected interface Command {
		String execute(Context context, String[] command);
		List<String> autoComplete(String[] command);
	}

	@Component
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
							throw new CommandFailedException("Unbekannter Teiltick");
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
							throw new CommandFailedException("Unbekannter Teiltick");
						}
					}
					else {
						tickAdminCommand.runRareTick();
					}
					return "Raretick wird ausgefuehrt";
				}
			}
			throw new CommandFailedException("Unbekannter befehl");
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Collections.singletonList("<regular|rare> run [TickClassName]");
		}
	}

	protected class EditFleetCommand implements Command {
		@Override
		public String execute( Context context, String[] command ) {
			String output = "";

			ShipFleet fleet = null;
			if( NumberUtils.isCreatable(command[1]) ) {
				int fid = Integer.parseInt(command[1]);
				fleet = em.find(ShipFleet.class, fid);
			}
			else if( command[1].length() > 1 && command[1].charAt(0) == 's' )
			{
				String id = command[1].substring(1);
				if( NumberUtils.isCreatable(id) )
				{
					Ship ship = em.find(Ship.class, Integer.valueOf(id));
					if( ship != null )
					{
						fleet = ship.getFleet();
					}
				}
			}

			if( fleet == null ) {
				throw new CommandFailedException("Flotte '"+command[1]+"' nicht gefunden");
			}

			switch (command[2])
			{
				case "heat":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Ueberhitzung ungueltig");
					}
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setHeat(Integer.parseInt(command[3]));
					}
					break;
				case "engine":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Antrieb ungueltig");
					}
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setEngine(Integer.parseInt(command[3]));
					}
					break;
				case "weapons":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Waffen ungueltig");
					}
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setWeapons(Integer.parseInt(command[3]));
					}
					break;
				case "jumptarget":
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setJumpTarget(command[3]);
					}
					break;
				case "e":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Energie ungueltig");
					}
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setEnergy(Integer.parseInt(command[3]));
					}
					break;
				case "pos":
					Location loc = Location.fromString(command[3]);

					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setLocation(loc);
						for (Ship lship : shipService.getLandedShips(ship))
						{
							lship.setLocation(loc);
						}
						for (Ship lship : shipService.getDockedShips(ship))
						{
							lship.setLocation(loc);
						}
					}
					break;
				case "hull":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Huelle ungueltig");
					}
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setHull(Integer.parseInt(command[3]));
					}
					break;
				case "shields":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Schilde ungueltig");
					}
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setShields(Integer.parseInt(command[3]));
					}
					break;
				case "crew":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Crew ungueltig");
					}
					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						ship.setCrew(Integer.parseInt(command[3]));
					}
					break;
				case "info":
					output += "Flotte: " + fleet.getId() + "\n";
					output += "Name: " + fleet.getName() + "\n";
					output += "Schiffe: " + fleetMgmtService.getShips(fleet).size() + "\n";
					break;
				case "additemmodule":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Slot ungueltig");
					}
					int slot = Integer.parseInt(command[3]);

					if (!NumberUtils.isCreatable(command[4]))
					{
						throw new CommandFailedException("Item-ID ungueltig");
					}
					int itemid = Integer.parseInt(command[4]);
					Item item = Item.getItem(itemid);

					if ((item == null) || (item.getEffect().getType() != ItemEffect.Type.MODULE))
					{
						throw new CommandFailedException("Das Item passt nicht");
					}

					for (Ship ship : fleetMgmtService.getShips(fleet))
					{
						shipService.addModule(ship, slot, ModuleType.ITEMMODULE, Integer.toString(itemid));

						ShipTypeData shiptype = ship.getTypeData();

						if (ship.getHull() > shiptype.getHull())
						{
							ship.setHull(shiptype.getHull());
						}

						if (ship.getShields() > shiptype.getShields())
						{
							ship.setShields(shiptype.getShields());
						}

						if (ship.getEnergy() > shiptype.getEps())
						{
							ship.setEnergy(shiptype.getEps());
						}

						if (ship.getCrew() > shiptype.getCrew())
						{
							ship.setCrew(shiptype.getCrew());
						}

						if (shiptype.getWerft() != 0)
						{
							ShipWerft werft = em.createQuery("from ShipWerft where ship=:ship", ShipWerft.class)
									.setParameter("ship", ship)
									.getSingleResult();

							if (werft == null)
							{
								werft = new ShipWerft(ship);
								em.persist(werft);
							}
						}
					}

					output = "Modul '" + item.getName() + "'@" + slot + " eingebaut\n";
					break;
				default:
					throw new CommandFailedException("Unknown editship sub-command >" + command[2] + "<");
			}
			for( Ship ship : fleetMgmtService.getShips(fleet) )
			{
				shipActionService.recalculateShipStatus(ship);
			}

			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			if( command.length == 1 ) {
				return Collections.singletonList("<fleetId|s<ShipId>> ...");
			}

			List<String> validCommands = Arrays.asList(
					"heat","engine","weapons",
					"jumptarget","e","pos",
					"hull","shields","crew",
					"info","additemmodule");

			if( command.length == 2 ||
					!validCommands.contains(command[2]) ) {
				return validCommands.stream().map(cmd -> autoCompleteFleet(command) + " " + cmd + " ... ").collect(Collectors.toList());
			}

			if( "pos".equals(command[2]) ) {
				return Collections.singletonList(autoCompleteFleet(command) + " " + command[2] + " <location>");
			}
			if( "info".equals(command[2]) ) {
				return Collections.singletonList(autoCompleteFleet(command) + " " + command[2]);
			}
			if( "additemmodule".equals(command[2]) ) {
				return Collections.singletonList(autoCompleteFleet(command) + " " + command[2] + " <slot> " + autoCompleteModuleItem(command));
			}

			return Collections.singletonList(autoCompleteFleet(command) + " " + command[2] + " <value>");
		}

		private String autoCompleteModuleItem(String[] command)
		{
			if( command.length < 5 || !NumberUtils.isCreatable(command[4]) )
			{
				return "<itemId (Integer)>";
			}
			Item item = Item.getItem(Integer.valueOf(command[4]));
			if( item == null )
			{
				return "<itemId (Integer)>";
			}
			return "<"+item.getName()+" ("+item.getID()+")>";
		}

		private String autoCompleteFleet(String[] command)
		{
			String fleetLabel = "<fleetId|s<shipId>>";
			if( command.length > 1 && NumberUtils.isCreatable(command[1]) )
			{
				ShipFleet fleet = em.find(ShipFleet.class, Integer.valueOf(command[1]));
				if( fleet != null )
				{
					fleetLabel = "<"+fleet.getName()+" ("+fleet.getId()+")>";
				}
			}
			else if( command.length > 1 && command[1].length() > 2 && command[1].charAt(0) == 's')
			{
				String id = command[1].substring(1);
				if( NumberUtils.isCreatable(id) )
				{
					Ship ship = em.find(Ship.class, Integer.valueOf(id));
					if( ship != null && ship.getFleet() != null )
					{
						fleetLabel = "<"+ship.getFleet().getName()+" ("+ship.getFleet().getId()+")>";
					}
				}
			}
			return fleetLabel;
		}
	}

	protected class EditShipCommand implements Command {
		@Override
		public String execute( Context context, String[] command ) {
			String output = "";

			if( !NumberUtils.isCreatable(command[1]) ) {
				throw new CommandFailedException("Ungueltige Schiffs-ID");
			}

			int sid = Integer.parseInt(command[1]);

			Ship ship = em.find(Ship.class, sid);
			if( ship == null ) {
				throw new CommandFailedException("Schiff '"+sid+"' nicht gefunden");
			}

			switch (command[2])
			{
				case "heat":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Ueberhitzung ungueltig");
					}
					ship.setHeat(Integer.parseInt(command[3]));
					break;
				case "engine":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Antrieb ungueltig");
					}
					ship.setEngine(Integer.parseInt(command[3]));
					break;
				case "weapons":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Waffen ungueltig");
					}
					ship.setWeapons(Integer.parseInt(command[3]));
					break;
				case "jumptarget":
					ship.setJumpTarget(command[3]);
					break;
				case "e":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Energie ungueltig");
					}
					ship.setEnergy(Integer.parseInt(command[3]));
					break;
				case "pos":
					Location loc = Location.fromString(command[3]);

					ship.setLocation(loc);

					for (Ship lship : shipService.getLandedShips(ship))
					{
						lship.setLocation(loc);
					}
					for (Ship lship : shipService.getDockedShips(ship))
					{
						lship.setLocation(loc);
					}
					break;
				case "hull":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Huelle ungueltig");
					}
					ship.setHull(Integer.parseInt(command[3]));
					break;
				case "shields":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Schilde ungueltig");
					}
					ship.setShields(Integer.parseInt(command[3]));
					break;
				case "crew":
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Crew ungueltig");
					}
					ship.setCrew(Integer.parseInt(command[3]));
					break;
				case "info":
				{
					ShipTypeData shiptype = ship.getTypeData();

					output += "Schiff: " + sid + "\n";
					output += "Typ: " + shiptype.getNickname() + " (" + ship.getType() + ")\n";
					output += "Besitzer: " + ship.getOwner().getId() + "\n";
					output += "Position: " + ship.getLocation().asString() + "\n";
					output += "Energie: " + ship.getEnergy() + "\n";
					output += "Heat: " + ship.getHeat() + "\n";
					output += "Huelle: " + ship.getHull() + "\n";
					if (shiptype.getShields() > 0)
					{
						output += "Schilde: " + ship.getShields() + "\n";
					}
					output += "Crew: " + ship.getCrew() + "\n";
					output += "Status: " + ship.getStatus() + "\n";
					output += "Battle: " + ship.getBattle() + "\n";
					break;
				}
				case "additemmodule":
				{
					if (!NumberUtils.isCreatable(command[3]))
					{
						throw new CommandFailedException("Slot ungueltig");
					}
					int slot = Integer.parseInt(command[3]);

					if (!NumberUtils.isCreatable(command[4]))
					{
						throw new CommandFailedException("Item-ID ungueltig");
					}
					int itemid = Integer.parseInt(command[4]);
					Item item = Item.getItem(itemid);

					if ((item == null) || (item.getEffect().getType() != ItemEffect.Type.MODULE))
					{
						throw new CommandFailedException("Das Item passt nicht");
					}

					shipService.addModule(ship, slot, ModuleType.ITEMMODULE, Integer.toString(itemid));

					ShipTypeData shiptype = ship.getTypeData();

					if (ship.getHull() > shiptype.getHull())
					{
						ship.setHull(shiptype.getHull());
					}

					if (ship.getShields() > shiptype.getShields())
					{
						ship.setShields(shiptype.getShields());
					}

					if (ship.getEnergy() > shiptype.getEps())
					{
						ship.setEnergy(shiptype.getEps());
					}

					if (ship.getCrew() > shiptype.getCrew())
					{
						ship.setCrew(shiptype.getCrew());
					}

					if (shiptype.getWerft() != 0)
					{
						ShipWerft werft = em.createQuery("from ShipWerft where ship=:ship", ShipWerft.class)
								.setParameter("ship", ship.getId())
								.getSingleResult();

						if (werft == null)
						{
							werft = new ShipWerft(ship);
							em.persist(werft);
						}
					}

					output = "Modul '" + item.getName() + "'@" + slot + " eingebaut\n";
					break;
				}
				default:
					throw new CommandFailedException("Unknown editship sub-command >" + command[2] + "<");
			}
			shipActionService.recalculateShipStatus(ship);

			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			if( command.length == 1 ) {
				return Collections.singletonList("<shipId> ...");
			}

			List<String> validCommands = Arrays.asList(
					"heat","engine","weapons",
					"jumptarget","e","pos",
					"hull","shields","crew",
					"info","additemmodule");

			if( command.length == 2 ||
					!validCommands.contains(command[2]) ) {
				return validCommands.stream().map(cmd -> autoCompleteShip(command) + " " + cmd + " ... ").collect(Collectors.toList());
			}

			if( "pos".equals(command[2]) ) {
				return Collections.singletonList(autoCompleteShip(command) + " " + command[2] + " <location>");
			}
			if( "info".equals(command[2]) ) {
				return Collections.singletonList(autoCompleteShip(command) + " " + command[2]);
			}
			if( "additemmodule".equals(command[2]) ) {
				return Collections.singletonList(autoCompleteShip(command) + " " + command[2] + " <slot> " + autoCompleteModuleItem(command));
			}

			return Collections.singletonList(autoCompleteShip(command) + " " + command[2] + " <value>");
		}

		private String autoCompleteModuleItem(String[] command)
		{
			if( command.length < 5 || !NumberUtils.isCreatable(command[4]) )
			{
				return "<itemId (Integer)>";
			}
			Item item = Item.getItem(Integer.valueOf(command[4]));
			if( item == null )
			{
				return "<itemId (Integer)>";
			}
			return "<"+item.getName()+" ("+item.getID()+")>";
		}

		private String autoCompleteShip(String[] command)
		{
			String shipLabel = "<shipId>";
			if( command.length > 1 && NumberUtils.isCreatable(command[1]) )
			{
				Ship ship = em.find(Ship.class, Integer.valueOf(command[1]));
				if( ship != null )
				{
					shipLabel = "<"+ship.getName()+" ("+ship.getId()+")>";
				}
			}
			return shipLabel;
		}
	}

	protected class AddResourceCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
			String output = "";

			String oid = command[1];
			ResourceID resid;
			try {
				resid = Resources.fromString(command[2]);
			}
			catch( RuntimeException e ) {
				throw new CommandFailedException("Die angegebene Resource ist ungueltig");
			}

			if( !NumberUtils.isCreatable(command[3]) ) {
				throw new CommandFailedException("Menge ungueltig");
			}
			long count = Long.parseLong(command[3]);

			if( !NumberUtils.isCreatable(oid.substring(1)) ) {
				throw new CommandFailedException("ID ungueltig");
			}

			Cargo cargo;
			if( oid.startsWith("b") ) {
				Base base = em.find(Base.class, Integer.parseInt(oid.substring(1)));
				if( base == null ) {
					throw new CommandFailedException("Objekt existiert nicht");
				}
				cargo = new Cargo(base.getCargo());
			}
			else {
				Ship ship = em.find(Ship.class, Integer.parseInt(oid.substring(1)));
				if( ship == null ) {
					throw new CommandFailedException("Objekt existiert nicht");
				}
				cargo = new Cargo(ship.getCargo());
			}

			cargo.addResource( resid, count );

			if( oid.startsWith("s") ) {
				Ship ship = em.find(Ship.class, Integer.parseInt(oid.substring(1)));
				ship.setCargo(cargo);
				shipActionService.recalculateShipStatus(ship);
			}
			else {
				Base base = em.find(Base.class, Integer.parseInt(oid.substring(1)));
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
				item = Optional.ofNullable(resid)
						.map(resourceID -> Item.getItem(resourceID.getItemID()))
						.orElse(null);
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

			String id = command[1].substring(1);
			if( !NumberUtils.isCreatable(id) )
			{
				return "<(b|s)ObjektID>";
			}
			char c = command[1].charAt(0);

			if( c == 'b' ) {
				Base base = em.find(Base.class, Integer.parseInt(id));
				if( base == null ) {
					return "<(b|s)ObjektID>";
				}
				return "<Basis "+base.getName()+" ("+base.getId()+")>";
			}
			else if( c == 's' ){
				Ship ship = em.find(Ship.class, Integer.parseInt(id));
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
			return Collections.singletonList(getTargetAutoComplete(command) + " " + getItemAutoComplete(command) + " <Menge>");
		}
	}

	protected class AddUnitsCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
			String output = "";

			String oid = command[1];
			UnitType unitType;
			try {
				unitType = em.find(UnitType.class, Integer.parseInt(command[2]));
			}
			catch( RuntimeException e ) {
				throw new CommandFailedException("Der angegebene Einheitentyp ist ungueltig");
			}

			if( !NumberUtils.isCreatable(command[3]) ) {
				throw new CommandFailedException("Menge ungueltig");
			}
			long count = Long.parseLong(command[3]);

			if( !NumberUtils.isCreatable(oid.substring(1)) ) {
				throw new CommandFailedException("ID ungueltig");
			}

			UnitCargo cargo;
			if( oid.startsWith("b") ) {
				Base base = em.find(Base.class, Integer.parseInt(oid.substring(1)));
				if( base == null ) {
					throw new CommandFailedException("Objekt existiert nicht");
				}
				cargo = base.getUnits();
			}
			else {
				Ship ship = em.find(Ship.class, Integer.parseInt(oid.substring(1)));
				if( ship == null ) {
					throw new CommandFailedException("Objekt existiert nicht");
				}
				cargo = ship.getUnitCargo();
			}

			cargo.addUnit(unitType, count);

			if( oid.startsWith("s") ) {
				Ship ship = em.find(Ship.class, Integer.parseInt(oid.substring(1)));
				ship.setUnitCargo(cargo);
				shipActionService.recalculateShipStatus(ship);
			}
			else {
				Base base = em.find(Base.class, Integer.parseInt(oid.substring(1)));
				base.setUnits(cargo);
			}

			return output;
		}

		private String getItemAutoComplete(String[] command)
		{
			if( command.length < 3 )
			{
				return "<unitId>";
			}

			UnitType unitType = null;
			try {
				unitType = em.find(UnitType.class, Integer.parseInt(command[2]));
			}
			catch( RuntimeException e ) {
				// EMPTY
			}
			if( unitType == null )
			{
				return "<unitId>";
			}
			return "<"+unitType.getName()+" ("+unitType.getId()+")>";
		}

		private String getTargetAutoComplete(String[] command)
		{
			if( command.length < 2 || command[1].length() < 2 )
			{
				return "<(b|s)ObjektID>";
			}
			String id = command[1].substring(1);
			if( !NumberUtils.isCreatable(id) )
			{
				return "<(b|s)ObjektID>";
			}
			char c = command[1].charAt(0);

			if( c == 'b' ) {
				Base base = em.find(Base.class, Integer.parseInt(id));
				if( base == null ) {
					return "<(b|s)ObjektID>";
				}
				return "<Basis "+base.getName()+" ("+base.getId()+")>";
			}
			else if( c == 's' ){
				Ship ship = em.find(Ship.class, Integer.parseInt(id));
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
			return Collections.singletonList(getTargetAutoComplete(command) + " " + getItemAutoComplete(command) + " <Menge>");
		}
	}

	protected class BattleCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
			String output = "";

			String cmd = command[1];
			if( cmd.equals("end") ) {
				int battleid = Integer.parseInt( command[2] );

				Battle battle = em.find(Battle.class, battleid);

				if( battle == null ) {
					throw new CommandFailedException("Die angegebene Schlacht existiert nicht");
				}

				User sourceUser = em.find(User.class, -1);

				pmService.send(sourceUser, battle.getCommander(0).getId(), "Schlacht beendet", "Die Schlacht bei "+locationService.displayCoordinates(battle.getLocation(), false)+" wurde durch die Administratoren beendet.");
				pmService.send(sourceUser, battle.getCommander(1).getId(), "Schlacht beendet", "Die Schlacht bei "+locationService.displayCoordinates(battle.getLocation(), false)+" wurde durch die Administratoren beendet.");

				battleService.load(battle, battle.getCommander(0), null, null, 0);
				battleService.endBattle(battle,0, 0);
			}
			else {
				throw new CommandFailedException("Unknown battle sub-command >"+cmd+"<");
			}
			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Collections.singletonList("end <battleID>");
		}
	}

	protected class DestroyShipCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
			String output;

			List<String> sql = new ArrayList<>();
			for( int i=1; i < command.length; i++ ) {
				switch (command[i])
				{
					case "sector":
						i++;
						Location sector = Location.fromString(command[i]);
						sql.add("system=" + sector.getSystem() + " and x=" + sector.getX() + " and y=" + sector.getY());
						break;
					case "owner":
						i++;
						int owner = Integer.parseInt(command[i]);
						sql.add("owner=" + owner);
						break;
					case "fleet":
						i++;
						int fleet = Integer.parseInt(command[i]);
						sql.add("fleet=" + fleet);
						break;
					case "type":
						i++;
						int type = Integer.parseInt(command[i]);
						sql.add("type=" + type);
						break;
				}
			}
			if(!sql.isEmpty()) {
				List<Ship> ships = em.createQuery("from Ship where "+Common.implode(" and ", sql), Ship.class).getResultList();
				int num = ships.size();
				for(Ship ship: ships)
				{
					dismantlingService.destroy(ship);
				}

				output = num+" Schiffe entfernt";
			}
			else {
				throw new CommandFailedException("Bitte Eingabe konkretisieren (Keine Einschraenkungen vorhanden)");
			}

			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Collections.singletonList("[sector <location>|owner <userId>|fleet <fleetId>|type <shiptypeId>]+");
		}
	}

	protected static class BuildImgsCommand implements Command {
		private static final Log log = LogFactory.getLog(BuildImgsCommand.class);

		private void checkImage( String baseimg, String fleet ) {
			if( new File(baseimg+fleet+"+.png").isFile() ) {
				return;
			}

			try {
				Font font;
				if( !new File(Configuration.getAbsolutePath()+"data/bnkgothm.ttf").isFile() ) {
					log.warn("bnkgothm.ttf nicht auffindbar");
					font = Font.getFont("bankgothic md bt");
					if( font == null ) {
						font = Font.getFont("courier");
					}
				}
				else {
					font = Font.createFont(Font.TRUETYPE_FONT,
							new File(Configuration.getAbsolutePath()+"data/bnkgothm.ttf"));
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
			catch( FontFormatException | IOException e ) {
				log.error(e, e);
			}
		}

		private String splitplanetimgs( String baseimg, String targetname ) {
			String datadir = Configuration.getAbsolutePath()+"data/starmap/";

			baseimg = datadir+baseimg;
			targetname = datadir+targetname;

			if( !new File(baseimg+".png").isFile() ) {
				throw new CommandFailedException("FATAL ERROR: bild existiert nicht ("+baseimg+".png)");
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
			switch (cmd)
			{
				case "starmap":
				{
					String img = command[2];

					boolean sizedimg = false;
					int imgcount = 0;

					String path = Configuration.getAbsolutePath() + "data/starmap/" + img + "/" + img;
					if (!new File(path + "0.png").isFile())
					{
						if (!new File(path + ".png").isFile())
						{
							throw new CommandFailedException("Unbekannte Grafik >" + img + "<");
						}
					}
					else
					{
						sizedimg = true;
					}

					while (true)
					{
						for (int i = 0; i < 8; i++)
						{
							String fleet = "";

							if ((i & 4) != 0)
							{
								fleet += "_fo";
							}
							if ((i & 2) != 0)
							{
								fleet += "_fa";
							}
							if ((i & 1) != 0)
							{
								fleet += "_fe";
							}

							if (fleet.length() == 0)
							{
								continue;
							}

							final File file = new File(path + (sizedimg ? imgcount : "") + fleet + ".png");
							if (file.isFile())
							{
								file.delete();
							}

							checkImage(path + (sizedimg ? imgcount : ""), fleet);
						}
						if (sizedimg)
						{
							imgcount++;
							if (!new File(path + imgcount + ".png").isFile())
							{
								break;
							}
						}
						else
						{
							break;
						}
					}
					break;
				}
				case "splitplanetimg":
				{
					String img = command[2];
					String target = command[3];

					output = splitplanetimgs(img, target);
					break;
				}
				default:
					throw new CommandFailedException("Unbekannter Befehl " + cmd);
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
		private final TaskManager taskManager;

		public ExecTaskCommand(TaskManager taskManager) {
			this.taskManager = taskManager;
		}

		@Override
		public String execute(Context context, String[] command) {
			String output;

			String taskid = command[1];
			String message = command[2];

			if( taskManager.getTaskByID(taskid) != null ) {
				taskManager.handleTask(taskid, message);
				output = "Task ausgeführt";
			}
			else {
				throw new CommandFailedException("Keine gültige TaskID");
			}

			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Collections.singletonList("<taskId> <MessageToSend>");
		}
	}

	protected class RecalcShipModulesCommand implements Command {
		@Override
		public String execute(Context context, String[] command) {
			String output;

			final AtomicInteger count = new AtomicInteger(0);
			long start = System.currentTimeMillis();

			List<Integer> ships;
			if( command.length < 2 )
			{
				ships = em.createQuery("select s.id from Ship as s join s.modules " +
								"where s.id>0 order by s.owner.id,s.docked,s.shiptype.id asc", Integer.class)
						.getResultList();
			}
			else {
				ships = new ArrayList<>();
				ships.add(Integer.parseInt(command[1]));
			}

			new EvictableUnitOfWork<Integer>("AdminCommand: RecalculateShipModules") {

				@Override
				public void doWork(Integer object) {
					Ship ship = em.find(Ship.class, object);
					shipService.recalculateModules(ship);
					count.incrementAndGet();
				}
			}
			.setFlushSize(20)
			.executeFor(ships);

			output = "Es wurden "+count.get()+" Schiffe in "+ (System.currentTimeMillis() - start)/1000d +" Sekunden neu berechnet.";
			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Collections.singletonList("[shipId]");
		}
	}

	protected static class ClearCache implements Command {
		@Override
		public String execute(Context context, String[] command) {
			String output = "Cache cleared";

			SessionFactory sf = context.getBean(SessionFactory.class, null);
			Cache c = sf.getCache();
			c.evictEntityData();
			c.evictCollectionData();
			c.evictQueryRegions();

			return output;
		}

		@Override
		public List<String> autoComplete(String[] command)
		{
			return Collections.singletonList("");
		}
	}

	protected class ClearSystemTileCache implements Command {

		@Override
		public String execute(Context context, String[] command) {
			if(command.length != 1) {
				return "Usage: cleartilecache";
			}

			List<StarSystem> systems = em.createQuery("from StarSystem order by id asc", StarSystem.class).getResultList();
			for (StarSystem system: systems) {
				TileCache tileCache = TileCache.forSystem(system);
				tileCache.resetCache();
			}

			return "Tile Cache cleared";
		}

		@Override
		public List<String> autoComplete(String[] command) {
			return Collections.singletonList("");
		}
	}
}
