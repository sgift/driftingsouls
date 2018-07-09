package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewMessage;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.map.AdminFieldView;
import net.driftingsouls.ds2.server.map.AdminStarmap;
import net.driftingsouls.ds2.server.map.FieldView;
import net.driftingsouls.ds2.server.map.PlayerFieldView;
import net.driftingsouls.ds2.server.map.PlayerStarmap;
import net.driftingsouls.ds2.server.map.PublicStarmap;
import net.driftingsouls.ds2.server.map.SectorImage;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.modules.viewmodels.AllyViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.JumpNodeViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.ShipFleetViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.UserViewModel;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zeigt die Sternenkarte eines Systems an.
 *
 * @author Drifting-Souls Team
 */
@Module(name = "map")
public class MapController extends Controller
{
	public void validiereSystem(StarSystem system)
	{
		User user = (User) getUser();

		if (system == null || !system.isVisibleFor(user))
		{
			throw new ValidierungException("Sie haben keine entsprechenden Karten");
		}
	}

	@Action(value = ActionType.AJAX)
	public ViewMessage speichereSystemkarteAction(
			@UrlParam(name="sys#x") Map<StarSystem,Integer> xWerte,
			@UrlParam(name="sys#y") Map<StarSystem,Integer> yWerte)
	{
		if (!hasPermission(WellKnownAdminPermission.STARMAP_SYSTEMAUSWAHL))
		{
			return ViewMessage.error("Du bist nicht berechtigt diese Aktion auszuführen");
		}

		org.hibernate.Session db = getDB();

		List<StarSystem> systems = Common.cast(db.createCriteria(StarSystem.class).list());
		for (StarSystem system : systems)
		{
			Integer x = xWerte.get(system);
			Integer y = yWerte.get(system);

			if( x == null || y == null )
			{
				continue;
			}

			system.setMapX(x);
			system.setMapY(y);
		}

		return ViewMessage.success("Systeme gespeichert");
	}

	private SystemauswahlViewModel createResultObj()
	{
		SystemauswahlViewModel result = new SystemauswahlViewModel();
		result.adminSichtVerfuegbar = hasPermission(WellKnownAdminPermission.STARMAP_VIEW);
		result.systemkarteEditierbar = hasPermission(WellKnownAdminPermission.STARMAP_SYSTEMAUSWAHL);

		return result;
	}

	@ViewModel
	public static class SystemauswahlViewModel
	{
		public static class AllianzViewModel
		{
			public int id;
			public String name;
			public String plainname;
		}

		public static class SystemViewModel
		{
			public String name;
			public int id;
			public String addinfo;
			public boolean npcOnly;
			public boolean adminOnly;
			public int mapX;
			public int mapY;
			public boolean basis;
			public boolean schiff;
			public AllianzViewModel allianz;

			public List<JumpNodeViewModel> sprungpunkte = new ArrayList<>();
		}

		public int system;
		public boolean adminSichtVerfuegbar;
		public boolean systemkarteEditierbar;
		public List<SystemViewModel> systeme = new ArrayList<>();
	}

	/**
	 * Zeigt die Sternenkarte an.
	 *
	 * @param sys Das momentan ausgewaehlte Sternensystem
	 */
	@Action(value = ActionType.AJAX, readOnly = true)
	public SystemauswahlViewModel systemauswahlAction(StarSystem sys)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		SystemauswahlViewModel result = createResultObj();

		List<JumpNode> jumpNodes = Common.cast(db
				.createQuery("from JumpNode jn where " + (!hasPermission(WellKnownAdminPermission.STARMAP_VIEW) ? "jn.hidden=false and " : "") + "jn.system!=jn.systemOut")
				.list());

		Map<Integer, Ally> systemFraktionen = ermittleDominierendeAllianzen(db);
		Set<Integer> basen = ermittleSystemeMitEigenerBasis(db);
		Set<Integer> schiffe = ermittleSystemeMitEigenenSchiffen(db);

		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem order by id asc").list());
		for (StarSystem system : systems)
		{
			if (!system.isVisibleFor(user))
			{
				continue;
			}

			if( sys == null )
			{
				sys = system;
			}

			String systemAddInfo = " ";

			if (system.getAccess() == StarSystem.Access.ADMIN)
			{
				systemAddInfo += "[admin]";
			}
			else if (system.getAccess() == StarSystem.Access.NPC)
			{
				systemAddInfo += "[hidden]";
			}

			SystemauswahlViewModel.SystemViewModel sysObj = new SystemauswahlViewModel.SystemViewModel();
			sysObj.name = system.getName();
			sysObj.id = system.getID();
			sysObj.addinfo = systemAddInfo;
			sysObj.npcOnly = system.getAccess() == StarSystem.Access.NPC;
			sysObj.adminOnly = system.getAccess() == StarSystem.Access.ADMIN;
			sysObj.mapX = system.getMapX();
			sysObj.mapY = system.getMapY();

			// Sprungpunkte
			for (JumpNode jn : jumpNodes)
			{
				if (jn.getSystem() == system.getID())
				{
					sysObj.sprungpunkte.add(JumpNodeViewModel.map(jn));
				}
			}

			// Dominierende NPC-Allianzen
			Ally maxAlly = systemFraktionen.get(system.getID());
			if (maxAlly != null)
			{
				sysObj.allianz = new SystemauswahlViewModel.AllianzViewModel();
				sysObj.allianz.name = Common._title(maxAlly.getName());
				sysObj.allianz.plainname = BBCodeParser.getInstance().parse(maxAlly.getName(), new String[]{"all"});
				sysObj.allianz.id = maxAlly.getId();
			}

			// Basen
			sysObj.basis = basen.contains(system.getID());

			// Schiffe
			sysObj.schiff = schiffe.contains(system.getID());

			result.systeme.add(sysObj);
		}

		result.system = sys != null ? sys.getID() : 0;

		return result;
	}

	private Set<Integer> ermittleSystemeMitEigenenSchiffen(Session db)
	{
		List<Integer> result = Common.cast(db
										   .createQuery("select s.system from Ship s where s.owner=:user group by s.system")
										   .setEntity("user", getUser())
										   .list());

		return new HashSet<>(result);
	}

	private Set<Integer> ermittleSystemeMitEigenerBasis(Session db)
	{
		List<Integer> result = Common.cast(db
										   .createQuery("select b.system from Base b where b.owner=:user group by b.system")
										   .setEntity("user", getUser())
										   .list());

		return new HashSet<>(result);
	}

	private Map<Integer, Ally> ermittleDominierendeAllianzen(Session db)
	{
		List<Object[]> data = Common.cast(db
										  .createQuery("select s.system,s.owner.ally,sum(s.shiptype.size) " +
													   "from Ship s join s.shiptype st left join s.modules sm " +
													   "where (s.status like :flags or sm.flags like :flags or st.flags like :flags) and s.owner.id<0 and s.owner.ally is not null " +
													   "group by s.system,s.owner.ally " +
													   "order by s.system,count(*)")
										  .setParameter("flags", "%" + ShipTypeFlag.TRADEPOST.getFlag() + "%")
										  .list());

		Map<Integer, Ally> systeme = new HashMap<>();
		int currentSys = -1;
		Ally currentAlly = null;
		long maxCount = 0;
		for (Object[] entry : data)
		{
			if ((Integer) entry[0] != currentSys)
			{
				systeme.put(currentSys, currentAlly);
				maxCount = 0;
				currentAlly = null;
				currentSys = (Integer) entry[0];
			}
			if ((Long) entry[2] > maxCount)
			{
				maxCount = (Long) entry[2];
				currentAlly = (Ally) entry[1];
			}
			else if ((Long) entry[2] == maxCount)
			{
				currentAlly = null;
			}
		}
		systeme.put(currentSys, currentAlly);

		return systeme;
	}

	/**
	 * Gibt eine einzelne Tile zurueck, entweder aus dem Cache oder, falls nicht vorhanden, neu generiert.
	 *
	 * @param sys Das anzuzeigende Sternensystem
	 * @param tileX Die X-Kachel
	 * @param tileY Die Y-Kachel
	 * @throws IOException Speicherfehler
	 */
	@Action(value = ActionType.BINARY, readOnly = true)
	public void tileAction(StarSystem sys, int tileX, int tileY) throws IOException
	{
		validiereSystem(sys);

		if (tileX < 0)
		{
			tileX = 0;
		}
		if (tileY < 0)
		{
			tileY = 0;
		}

		TileCache cache = TileCache.forSystem(sys);
		File tileCacheFile = cache.getTile(tileX, tileY);

		try (InputStream in = new FileInputStream(tileCacheFile))
		{
			getResponse().setContentType("image/png");
			try (OutputStream outputStream = getResponse().getOutputStream())
			{
				IOUtils.copy(in, outputStream);
			}
		}
	}

	@ViewModel
	public static class MapViewModel
	{
		public static class SystemViewModel
		{
			public int id;
			public int width;
			public int height;
		}

		public static class SizeViewModel
		{
			public int minx;
			public int miny;
			public int maxx;
			public int maxy;
		}

		public static class SectorImageViewModel
		{
			public String image;
			public int x;
			public int y;

			public static SectorImageViewModel map(SectorImage image)
			{
				SectorImageViewModel viewmodel = new SectorImageViewModel();
				viewmodel.image = image.getImage();
				viewmodel.x = image.getX();
				viewmodel.y = image.getY();
				return viewmodel;
			}
		}

		public static class LocationViewModel
		{
			public int x;
			public int y;
			public boolean scan;
			public SectorImageViewModel bg;
			public int scanner;
			public String fg;
			public boolean battle;
			public boolean roterAlarm;
		}

		public SystemViewModel system;
		public SizeViewModel size;
		public List<LocationViewModel> locations = new ArrayList<>();
	}

	/**
	 * Gibt die Kartendaten des gewaehlten Ausschnitts als JSON-Response zurueck.
	 *
	 * @param sys Das anzuzeigende Sternensystem
	 * @param xstart Die untere Grenze des Ausschnitts auf der X-Achse
	 * @param xend Die obere Grenze des Ausschnitts  auf der X-Achse
	 * @param ystart Die untere Grenze des Ausschnitts auf der Y-Achse
	 * @param yend Die obere Grenze des Ausschnitts  auf der Y-Achse
	 * @param admin {@code true} falls die Adminsicht auf die Sternenkarte verwendet werden soll
	 */
	@Action(value = ActionType.AJAX, readOnly = true)
	public MapViewModel mapAction(StarSystem sys, int xstart, int xend, int ystart, int yend, boolean admin)
	{
		validiereSystem(sys);

		MapViewModel json = new MapViewModel();

		org.hibernate.Session db = getDB();

		User user = (User) getUser();

		json.system = new MapViewModel.SystemViewModel();
		json.system.id = sys.getID();
		json.system.width = sys.getWidth();
		json.system.height = sys.getHeight();

		int width = sys.getWidth();
		int height = sys.getHeight();

		//Limit width and height to map size
		if (xstart < 1)
		{
			xstart = 1;
		}

		if (xend > width)
		{
			xend = width;
		}

		if (ystart < 1)
		{
			ystart = 1;
		}

		if (yend > height)
		{
			yend = height;
		}

		//Use sensible defaults in case of useless input
		if (yend <= ystart)
		{
			yend = height;
		}

		if (xend <= xstart)
		{
			xend = width;
		}

		PublicStarmap content;
		if (admin && hasPermission(WellKnownAdminPermission.STARMAP_VIEW))
		{
			content = new AdminStarmap(sys, user, new int[]{xstart, ystart, xend - xstart, yend - ystart});
		}
		else
		{
			content = new PlayerStarmap(user, sys, new int[]{xstart, ystart, xend - xstart, yend - ystart});
		}

		json.size = new MapViewModel.SizeViewModel();
		json.size.minx = xstart;
		json.size.miny = ystart;
		json.size.maxx = xend;
		json.size.maxy = yend;

		for (int y = ystart; y <= yend; y++)
		{
			for (int x = xstart; x <= xend; x++)
			{
				Location position = new Location(sys.getID(), x, y);
				MapViewModel.LocationViewModel locationViewModel = createMapLocationViewModel(content, position);
				if (locationViewModel != null)
				{
					json.locations.add(locationViewModel);
				}
			}
		}

		// Das Anzeigen sollte keine DB-Aenderungen verursacht haben
		db.clear();

		return json;
	}

	private MapViewModel.LocationViewModel createMapLocationViewModel(PublicStarmap content, Location position)
	{
		boolean scannable = content.isScannbar(position);
		SectorImage sectorImage = content.getUserSectorBaseImage(position);
		SectorImage sectorOverlayImage = content.getSectorOverlayImage(position);

		boolean endTag = false;

		MapViewModel.LocationViewModel posObj = new MapViewModel.LocationViewModel();
		posObj.x = position.getX();
		posObj.y = position.getY();
		posObj.scan = scannable;

		if (sectorImage != null)
		{
			endTag = true;
			posObj.bg = MapViewModel.SectorImageViewModel.map(sectorImage);
			sectorImage = sectorOverlayImage;
		}
		else if (scannable)
		{
			endTag = true;
			posObj.bg = new MapViewModel.SectorImageViewModel();//MapViewModel.SectorImageViewModel.map(content.getSectorBaseImage(position));
			sectorImage = sectorOverlayImage;
		}
		else if (sectorOverlayImage != null)
		{
			endTag = true;
			sectorImage = sectorOverlayImage;
		}

		if (scannable && content.isHasSectorContent(position))
		{
			Ship scanner = content.getScanSchiffFuerSektor(position);

			posObj.scanner = scanner != null ? scanner.getId() : -1;
		}

		if (sectorImage != null)
		{
			posObj.fg = sectorImage.getImage();
		}

		posObj.battle = content.isSchlachtImSektor(position);
		posObj.roterAlarm = content.isRoterAlarmImSektor(position);

		if (endTag)
		{
			return posObj;
		}
		return null;
	}

	@ViewModel
	public static class SectorViewModel
	{
		public static class UserWithShips
		{
			public String name;
			public int id;
			public int race;
			public boolean eigener;
			public List<ShipTypeViewModel> shiptypes = new ArrayList<>();
		}

		public static class ShipTypeViewModel
		{
			public int id;
			public String name;
			public String picture;
			public int size;
			public List<ShipViewModel> ships = new ArrayList<>();
		}

		public static class ShipViewModel
		{
			public int id;
			public String name;
			public long gedockt;
			public int maxGedockt;
			public ShipFleetViewModel fleet;
		}

		public static class OwnShipViewModel extends ShipViewModel
		{
			public long gelandet;
			public int maxGelandet;
			public int energie;
			public int maxEnergie;
			public int ueberhitzung;
			public boolean kannFliegen;
			public int sensorRange;
		}

		public static class BaseViewModel
		{
			public int id;
			public String name;
			public String username;
			public String image;
			public int klasse;
			public String typ;
			public boolean eigene;
		}

		public static class JumpNodeViewModel
		{
			public int id;
			public String name;
			public boolean blocked;
		}

		public static class NebelViewModel
		{
			public int type;
			public String image;
		}

		public static class BattleViewModel
		{
			public int id;
			public boolean einsehbar;
			public List<BattleSideViewModel> sides = new ArrayList<>();
		}

		public static class BattleSideViewModel
		{
			public UserViewModel commander;
			public AllyViewModel ally;
		}

		public int subraumspaltenCount;
		public boolean roterAlarm;
		public List<UserWithShips> users = new ArrayList<>();
		public List<BaseViewModel> bases = new ArrayList<>();
		public List<JumpNodeViewModel> jumpnodes = new ArrayList<>();
		public List<BattleViewModel> battles = new ArrayList<>();
		public NebelViewModel nebel;
	}

	/**
	 * Zeigt einen einzelnen Sektor mit allen Details an.
	 *
	 * @param sys Das anzuzeigende Sternensystem
	 * @param x Die X-Koordinate des zu scannenden Sektors
	 * @param y Die Y-Koordinate des zu scannenden Sektors
	 * @param scanship Die ID des fuer den Scanvorgang zu verwendenden Schiffs
	 * @param admin {@code true} falls die Adminsicht auf die Sternenkarte verwendet werden soll
	 */
	@Action(value = ActionType.AJAX, readOnly = true)
	public SectorViewModel sectorAction(StarSystem sys, int x, int y, Ship scanship, boolean admin)
	{
		validiereSystem(sys);

		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		SectorViewModel json = new SectorViewModel();

		final Location loc = new Location(sys.getID(), x, y);

		FieldView field;
		if (admin && hasPermission(WellKnownAdminPermission.STARMAP_VIEW))
		{
			field = new AdminFieldView(db, user, loc);
		}
		else
		{
			field = new PlayerFieldView(db, user, loc, scanship);
		}

		json.users.addAll(exportSectorShips(field));

		for (Base base : field.getBases())
		{
			SectorViewModel.BaseViewModel baseObj = new SectorViewModel.BaseViewModel();
			baseObj.id = base.getId();
			baseObj.name = base.getName();
			baseObj.username = Common._title(base.getOwner().getName());
			baseObj.image = base.getKlasse().getLargeImage();
			baseObj.klasse = base.getKlasse().getId();
			baseObj.typ = base.getKlasse().getName();
			baseObj.eigene = base.getOwner().getId() == user.getId();

			json.bases.add(baseObj);
		}

		for (JumpNode jumpNode : field.getJumpNodes())
		{
			SectorViewModel.JumpNodeViewModel jnObj = new SectorViewModel.JumpNodeViewModel();
			jnObj.id = jumpNode.getId();
			jnObj.name = jumpNode.getName();
			jnObj.blocked = jumpNode.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn(0);
			json.jumpnodes.add(jnObj);
		}

		Nebel nebel = field.getNebel();
		if (nebel != null)
		{
			json.nebel = new SectorViewModel.NebelViewModel();
			json.nebel.type = nebel.getType().getCode();
			json.nebel.image = nebel.getImage();
		}

		json.battles.addAll(exportSectorBattles(db, field));

		json.subraumspaltenCount = field.getSubraumspalten().size();
		json.roterAlarm = field.isRoterAlarm();


		return json;
	}

	private List<SectorViewModel.BattleViewModel> exportSectorBattles(Session db, FieldView field)
	{
		List<SectorViewModel.BattleViewModel> battleListObj = new ArrayList<>();
		List<Battle> battles = field.getBattles();
		if (battles.isEmpty())
		{
			return battleListObj;
		}

		User user = (User) getUser();
		boolean viewable = getContext().hasPermission(WellKnownPermission.SCHLACHT_ALLE_AUFRUFBAR);

		if (!viewable)
		{
			Map<ShipType, List<Ship>> ships = field.getShips().get(user);
			if (ships != null && !ships.isEmpty())
			{
				for (ShipType shipType : ships.keySet())
				{
					if (shipType.getShipClass().isDarfSchlachtenAnsehen())
					{
						viewable = true;
					}
				}
			}
		}

		for (Battle battle : battles)
		{
			SectorViewModel.BattleViewModel battleObj = new SectorViewModel.BattleViewModel();
			battleObj.id = battle.getId();
			battleObj.einsehbar = viewable || battle.getSchlachtMitglied(user) != -1;

			for (int i = 0; i < 2; i++)
			{
				SectorViewModel.BattleSideViewModel sideObj = new SectorViewModel.BattleSideViewModel();
				sideObj.commander = UserViewModel.map(battle.getCommander(i));
				if (battle.getAlly(i) != 0)
				{
					Ally ally = (Ally) db.get(Ally.class, battle.getAlly(i));
					sideObj.ally = AllyViewModel.map(ally);
				}
				battleObj.sides.add(sideObj);
			}

			battleListObj.add(battleObj);
		}
		return battleListObj;
	}

	private List<SectorViewModel.UserWithShips> exportSectorShips(FieldView field)
	{
		List<SectorViewModel.UserWithShips> users = new ArrayList<>();
		for (Map.Entry<User, Map<ShipType, List<Ship>>> owner : field.getShips().entrySet())
		{
			SectorViewModel.UserWithShips jsonUser = new SectorViewModel.UserWithShips();
			jsonUser.name = Common._text(owner.getKey().getName());
			jsonUser.id = owner.getKey().getId();
			jsonUser.race = owner.getKey().getRace();

			boolean ownFleet = owner.getKey().getId() == getUser().getId();
			jsonUser.eigener = ownFleet;

			for (Map.Entry<ShipType, List<Ship>> shiptype : owner.getValue().entrySet())
			{
				SectorViewModel.ShipTypeViewModel jsonShiptype = new SectorViewModel.ShipTypeViewModel();
				jsonShiptype.id = shiptype.getKey().getId();
				jsonShiptype.name = shiptype.getKey().getNickname();
				jsonShiptype.picture = shiptype.getKey().getPicture();
				jsonShiptype.size = shiptype.getKey().getSize();

				for (Ship ship : shiptype.getValue())
				{
					ShipTypeData typeData = ship.getTypeData();
					SectorViewModel.ShipViewModel shipObj;
					if (ownFleet)
					{
						SectorViewModel.OwnShipViewModel ownShip = new SectorViewModel.OwnShipViewModel();
						ownShip.gelandet = ship.getLandedCount();
						ownShip.maxGelandet = typeData.getJDocks();

						ownShip.energie = ship.getEnergy();
						ownShip.maxEnergie = typeData.getEps();

						ownShip.ueberhitzung = ship.getHeat();

						ownShip.kannFliegen = typeData.getCost() > 0 && !ship.isDocked() && !ship.isLanded();

						int sensorRange = ship.getEffectiveScanRange();
						if (field.getNebel() != null)
						{
							sensorRange /= 2;
						}
						ownShip.sensorRange = sensorRange;

						shipObj = ownShip;
					}
					else
					{
						shipObj = new SectorViewModel.ShipViewModel();
					}
					shipObj.id = ship.getId();
					shipObj.name = ship.getName();
					shipObj.gedockt = ship.getDockedCount();
					shipObj.maxGedockt = typeData.getADocks();


					if (ship.getFleet() != null)
					{
						shipObj.fleet = ShipFleetViewModel.map(ship.getFleet());
					}

					jsonShiptype.ships.add(shipObj);
				}
				jsonUser.shiptypes.add(jsonShiptype);
			}
			users.add(jsonUser);
		}
		return users;
	}
}
