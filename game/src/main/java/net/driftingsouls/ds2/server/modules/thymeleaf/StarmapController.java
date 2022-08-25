package net.driftingsouls.ds2.server.modules.thymeleaf;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.ComNetEntry;
import net.driftingsouls.ds2.server.entities.ComNetVisit;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.bbcode.Smilie;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.map.*;
import net.driftingsouls.ds2.server.modules.MapController;
import net.driftingsouls.ds2.server.services.ComNetService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.modules.viewmodels.ShipFleetViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.UserViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.AllyViewModel;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.battles.Battle;
import org.hibernate.Session;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Das ComNet - Alle Funktionalitaeten des ComNets befinden sich in
 * dieser Klasse.
 *
 * @author Gregor Fuhs
 */
public class StarmapController implements DSController, PermissionResolver {
    private Context context;

    @Override
    public final boolean hasPermission(PermissionDescriptor permission)
    {
        return this.context.hasPermission(permission);
    }

    private enum Action{
        GET_SYSTEM_DATA,
        GET_SCANFIELDS,
        GET_SCANNED_FIELDS,
        GET_SECTOR_INFORMATION,
        DEFAULT
    }

    /**
     * Erzeugt die StarMapSeite (/starmap).
     * URL-Parameter:
     * action:
     *  <code>null</code>, default
     *  GET_SYSTEM_DATA
     *  GET_SCANFIELDS
     *  GET_SCANNED_FIELDS
     */
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        context = ContextMap.getContext();
        Action action;
        try {
            action = Action.valueOf(request.getParameter("action").toUpperCase());
        }catch(Exception e){
            action = Action.DEFAULT;
        }

        int system;
        try {
            system = Integer.parseInt(request.getParameter("system"));
        }catch(Exception e){
            system = 605;
        }

        switch(action){
            case GET_SYSTEM_DATA:
                systemData(system, ctx, request, response);
                break;
            case GET_SCANFIELDS:
                scanfields(system, ctx, request, response);
                break;
            case GET_SCANNED_FIELDS:
                scannedFields(system, ctx, request, response);
                break;
            case GET_SECTOR_INFORMATION:
                sectorInformation(system, ctx, request, response);
                break;
            default:
                defaultAction(ctx, request);
                templateEngine.process("starmap", ctx, response.getWriter());
                break;
        }
    }

    /**
     * Aktion zur Anzeige der Starmap
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     */
    private void defaultAction(WebContext ctx, HttpServletRequest request){

        User user = (User) context.getActiveUser();


    }

    private void systemData(int systemId, WebContext ctx, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        User user = (User) context.getActiveUser();
        boolean admin = false;

        //var map = new PlayerStarmap(user, systemId, null);

        StarSystem sys = (StarSystem) db.get(StarSystem.class, systemId);
        validiereSystem(sys);

        JSONObject json = new JSONObject();
        json.put("system", systemId);
        json.put("width", sys.getWidth());
        json.put("height", sys.getHeight());
        json.put("admin", admin);

        response.getWriter().write(json.toString());
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!
    }

    private void scanfields(int systemId, WebContext ctx, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        User user = (User) context.getActiveUser();
        boolean admin = false;

        //var map = new PlayerStarmap(user, systemId, null);

        StarSystem sys = (StarSystem) db.get(StarSystem.class, systemId);
        validiereSystem(sys);

        MapController.MapViewModel jsonData = new MapController.MapViewModel();



        jsonData.system = new MapController.MapViewModel.SystemViewModel();
        jsonData.system.id = sys.getID();
        jsonData.system.width = sys.getWidth();
        jsonData.system.height = sys.getHeight();

        int xStart = 1;
        int yStart = 1;
        int xEnd = sys.getWidth();
        int yEnd = sys.getHeight();

        PublicStarmap content;
        var mapArea = new MapArea(xStart, xEnd - xStart, yStart, yEnd - yStart);
        if (admin && hasPermission(WellKnownAdminPermission.STARMAP_VIEW))
        {
            content = new AdminStarmap(sys.getID(), user);
        }
        else
        {
            content = new PlayerStarmap(user, sys.getID());
        }

        // Das Anzeigen sollte keine DB-Aenderungen verursacht haben
        db.clear();

        var json = new Gson().toJson(content.getScanSectorData());
        response.getWriter().write(json.toString());
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!

        // return json;
    }

    private void scannedFields(int systemId, WebContext ctx, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        User user = (User) context.getActiveUser();
        boolean admin = false;

        //var map = new PlayerStarmap(user, systemId, null);

        StarSystem sys = (StarSystem) db.get(StarSystem.class, systemId);
        validiereSystem(sys);

        MapController.MapViewModel jsonData = new MapController.MapViewModel();



        jsonData.system = new MapController.MapViewModel.SystemViewModel();
        jsonData.system.id = sys.getID();
        jsonData.system.width = sys.getWidth();
        jsonData.system.height = sys.getHeight();

        int xStart = 1;
        int yStart = 1;
        int xEnd = sys.getWidth();
        int yEnd = sys.getHeight();

        PublicStarmap content;
        var mapArea = new MapArea(xStart, xEnd - xStart, yStart, yEnd - yStart);
        if (admin && hasPermission(WellKnownAdminPermission.STARMAP_VIEW))
        {
            content = new AdminStarmap(sys.getID(), user);
        }
        else
        {
            content = new PlayerStarmap(user, sys.getID());
        }

      jsonData.size = new MapController.MapViewModel.SizeViewModel();
      jsonData.size.minx = xStart;
      jsonData.size.miny = yStart;
      jsonData.size.maxx = xEnd;
      jsonData.size.maxy = yEnd;

      for (int y = yStart; y <= yEnd; y++)
      {
        for (int x = xStart; x <= xEnd; x++)
        {
          Location position = new Location(sys.getID(), x, y);
          MapController.MapViewModel.LocationViewModel locationViewModel = createMapLocationViewModel(content, position);
          if (locationViewModel != null)
          {
            jsonData.locations.add(locationViewModel);
          }
        }
      }

        // Das Anzeigen sollte keine DB-Aenderungen verursacht haben
        db.clear();

        var json = new Gson().toJson(jsonData);
        response.getWriter().write(json);
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!

        // return json;
    }

    public void sectorInformation(int systemId, WebContext ctx, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        User user = (User) context.getActiveUser();
        boolean admin = false;

        //var map = new PlayerStarmap(user, systemId, null);

        StarSystem sys = (StarSystem) db.get(StarSystem.class, systemId);
        validiereSystem(sys);
        int x = -1;
        int y = -1;
        int scanshipid = -1;
        Ship scanship = null;
        try {
            x = Integer.parseInt(request.getParameter("x"));
            y = Integer.parseInt(request.getParameter("y"));
            scanshipid = Integer.parseInt(request.getParameter("scanship"));
            scanship = Ship.getShipById(scanshipid);
        }catch(Exception e){
            return;
        }

		MapController.SectorViewModel jsonData = new MapController.SectorViewModel();
        jsonData.system = sys.getID();
        jsonData.x = x;
        jsonData.y = y;

		final Location loc = new Location(sys.getID(), x, y);

		FieldView field;
		if (admin && hasPermission(WellKnownAdminPermission.STARMAP_VIEW))
		{
			field = new AdminFieldView(db, loc);
		}
		else
		{
			field = new PlayerFieldView(db, user, loc, scanship);
		}

		jsonData.users.addAll(exportSectorShips(field, user));

		for (Base base : field.getBases())
		{
			MapController.SectorViewModel.BaseViewModel baseObj = new MapController.SectorViewModel.BaseViewModel();
			baseObj.id = base.getId();
			baseObj.name = base.getName();
			baseObj.username = Common._title(base.getOwner().getName());
			baseObj.image = base.getKlasse().getLargeImage();
			baseObj.klasse = base.getKlasse().getId();
			baseObj.typ = base.getKlasse().getName();
			baseObj.eigene = base.getOwner().getId() == user.getId();

			jsonData.bases.add(baseObj);
		}
		for (Ship brocken : field.getBrocken())
		{
			MapController.SectorViewModel.BaseViewModel brockenObj = new MapController.SectorViewModel.BaseViewModel();
			brockenObj.id = brocken.getId();
			brockenObj.name = brocken.getName();
			brockenObj.username = Common._title(brocken.getOwner().getName());
			brockenObj.image = brocken.getTypeData().getPicture(); //getKlasse().getLargeImage();
			brockenObj.klasse = brocken.getTypeData().getTypeId();	//getKlasse().getId();
			brockenObj.typ = brocken.getTypeData().getNickname();  //getKlasse().getName();
			brockenObj.eigene = brocken.getOwner().getId() == user.getId();

			jsonData.bases.add(brockenObj);
		}

		for (JumpNode jumpNode : field.getJumpNodes())
		{
			MapController.SectorViewModel.JumpNodeViewModel jnObj = new MapController.SectorViewModel.JumpNodeViewModel();
			jnObj.id = jumpNode.getId();
			jnObj.name = jumpNode.getName();
			jnObj.blocked = jumpNode.isGcpColonistBlock() && Rassen.get().rasse(user.getRace()).isMemberIn(0);
			jsonData.jumpnodes.add(jnObj);
		}

		Nebel nebel = field.getNebel();
		if (nebel != null)
		{
			jsonData.nebel = new MapController.SectorViewModel.NebelViewModel();
			jsonData.nebel.type = nebel.getType().getCode();
			jsonData.nebel.image = nebel.getImage();
		}

		jsonData.battles.addAll(exportSectorBattles(db, field));

		jsonData.subraumspaltenCount = field.getSubraumspalten().size();
		jsonData.roterAlarm = field.isRoterAlarm();

        // Das Anzeigen sollte keine DB-Aenderungen verursacht haben
        db.clear();

        var json = new Gson().toJson(jsonData);
        response.getWriter().write(json);
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!

        // return json;

	}

    public void prepareResponseForJSON(HttpServletResponse response){
        response.resetBuffer();
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private MapController.MapViewModel.LocationViewModel createMapLocationViewModel(PublicStarmap content, Location position)
    {
        boolean scannable = content.isScanned(position);
        SectorImage sectorImage = content.getUserSectorBaseImage(position);
        SectorImage sectorOverlayImage = content.getSectorOverlayImage(position);

        boolean endTag = false;

        MapController.MapViewModel.LocationViewModel posObj = new MapController.MapViewModel.LocationViewModel();
        posObj.x = position.getX();
        posObj.y = position.getY();
        posObj.scan = scannable;

        if(!content.isHasSectorContent(position)) return null;

        if (scannable && content.isHasSectorContent(position))
        {
            posObj.scanner = content.getScanningShip(position);
        }

        if (sectorImage != null)
        {
            endTag = true;
            posObj.bg = MapController.MapViewModel.SectorImageViewModel.map(sectorImage);
            sectorImage = sectorOverlayImage;
        }
        else if (scannable)
        {
            endTag = true;
            posObj.bg = new MapController.MapViewModel.SectorImageViewModel();//MapViewModel.SectorImageViewModel.map(content.getSectorBaseImage(position));
            sectorImage = sectorOverlayImage;
        }
        else if (sectorOverlayImage != null)
        {
            endTag = true;
            sectorImage = sectorOverlayImage;
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

    public void validiereSystem(StarSystem system)
    {
        User user = (User) context.getActiveUser();

        if (system == null || !system.isVisibleFor(user))
        {
            throw new ValidierungException("Sie haben keine entsprechenden Karten");
        }
    }

    private List<MapController.SectorViewModel.UserWithShips> exportSectorShips(FieldView field, User user)
	{
		List<MapController.SectorViewModel.UserWithShips> users = new ArrayList<>();
		for (Map.Entry<User, Map<ShipType, List<Ship>>> owner : field.getShips().entrySet())
		{
			MapController.SectorViewModel.UserWithShips jsonUser = new MapController.SectorViewModel.UserWithShips();
			jsonUser.name = Common._text(owner.getKey().getName());
			jsonUser.id = owner.getKey().getId();
			jsonUser.race = owner.getKey().getRace();

			boolean ownFleet = owner.getKey().getId() == context.getActiveUser().getId();
			jsonUser.eigener = ownFleet;

			for (Map.Entry<ShipType, List<Ship>> shiptype : owner.getValue().entrySet())
			{
				MapController.SectorViewModel.ShipTypeViewModel jsonShiptype = new MapController.SectorViewModel.ShipTypeViewModel();
				jsonShiptype.id = shiptype.getKey().getId();
				jsonShiptype.name = shiptype.getKey().getNickname();
				jsonShiptype.picture = shiptype.getKey().getPicture();
				jsonShiptype.size = shiptype.getKey().getSize();

				for (Ship ship : shiptype.getValue())
				{
					ShipTypeData typeData = ship.getTypeData();
					MapController.SectorViewModel.ShipViewModel shipObj;
					if (ownFleet)
					{
						MapController.SectorViewModel.OwnShipViewModel ownShip = new MapController.SectorViewModel.OwnShipViewModel();
						ownShip.gelandet = ship.getLandedCount();
						ownShip.maxGelandet = typeData.getJDocks();

						ownShip.energie = ship.getEnergy();
						ownShip.maxEnergie = typeData.getEps();
                        ownShip.x = field.getLocation().getX();
                        ownShip.y = field.getLocation().getY();

						ownShip.ueberhitzung = ship.getHeat();

						ownShip.kannFliegen = typeData.getCost() > 0 && !ship.isDocked() && !ship.isLanded();

						int sensorRange = ship.getEffectiveScanRange();
						if (field.getNebel() != null)
						{
							if(!ship.getTypeData().hasFlag(ShipTypeFlag.NEBELSCAN))
							{
								sensorRange /= 2;
							}
						}
						ownShip.sensorRange = sensorRange;

						shipObj = ownShip;
					}
					else
					{
						shipObj = new MapController.SectorViewModel.ShipViewModel();
					}
					shipObj.id = ship.getId();
					shipObj.name = ship.getName();
					shipObj.gedockt = ship.getDockedCount();
					shipObj.maxGedockt = typeData.getADocks();
                    shipObj.isOwner = ship.getOwner().getId() == user.getId();
                    shipObj.race = ship.getOwner().getRace();


					if (ship.getFleet() != null)
					{
						shipObj.fleet = ShipFleetViewModel.map(ship.getFleet());
					}

					jsonShiptype.ships.add(shipObj);
				}
                jsonShiptype.count = jsonShiptype.ships.size();
				jsonUser.shiptypes.add(jsonShiptype);
			}
			users.add(jsonUser);
		}
		return users;
	}

    private List<MapController.SectorViewModel.BattleViewModel> exportSectorBattles(Session db, FieldView field)
	{
		List<MapController.SectorViewModel.BattleViewModel> battleListObj = new ArrayList<>();
		List<Battle> battles = field.getBattles();
		if (battles.isEmpty())
		{
			return battleListObj;
		}

		User user = (User) context.getActiveUser();
		boolean viewable = context.hasPermission(WellKnownPermission.SCHLACHT_ALLE_AUFRUFBAR);

		if (!viewable)
		{
			Map<ShipType, List<Ship>> ships = field.getShips().get(user);
			if (ships != null && !ships.isEmpty())
			{
				for (ShipType shipType : ships.keySet())
				{
					if (shipType.getShipClass().isDarfSchlachtenAnsehen()) {
						viewable = true;
						break;
					}
				}
			}
		}

		for (Battle battle : battles)
		{
			MapController.SectorViewModel.BattleViewModel battleObj = new MapController.SectorViewModel.BattleViewModel();
			battleObj.id = battle.getId();
			battleObj.einsehbar = viewable || battle.getSchlachtMitglied(user) != -1;

			for (int i = 0; i < 2; i++)
			{
				MapController.SectorViewModel.BattleSideViewModel sideObj = new MapController.SectorViewModel.BattleSideViewModel();
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

            public static MapController.MapViewModel.SectorImageViewModel map(SectorImage image)
            {
                MapController.MapViewModel.SectorImageViewModel viewmodel = new MapController.MapViewModel.SectorImageViewModel();
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
            public MapController.MapViewModel.SectorImageViewModel bg;
            public int scanner;
            public String fg;
            public boolean battle;
            public boolean roterAlarm;
        }

        public MapController.MapViewModel.SystemViewModel system;
        public MapController.MapViewModel.SizeViewModel size;
        public final List<MapController.MapViewModel.LocationViewModel> locations = new ArrayList<>();
    }

    public static class Error{
        public String text;

        public Error(String text){
            this.text = text;
        }
    }
}
