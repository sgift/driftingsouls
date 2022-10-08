package net.driftingsouls.ds2.server.modules.thymeleaf;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.PermissionDescriptor;
import net.driftingsouls.ds2.server.framework.PermissionResolver;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.map.*;
import net.driftingsouls.ds2.server.modules.MapController;
import net.driftingsouls.ds2.server.modules.viewmodels.AllyViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.ShipFleetViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.UserViewModel;
import net.driftingsouls.ds2.server.repositories.ItemRepository;
import net.driftingsouls.ds2.server.repositories.ShipsRepository;
import net.driftingsouls.ds2.server.repositories.StarsystemRepository;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.driftingsouls.ds2.server.entities.jooq.tables.BattlesShips.BATTLES_SHIPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
import static org.jooq.impl.DSL.count;

/**
 * Die Sternenkarte.
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
        GET_STARSYSTEM_MAP_DATA,
        DEFAULT
    }

    private User user;

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
            //TODO: Fall back to system where user has most of their asteroids
            system = 605;
        }

        switch(action){
            case GET_SYSTEM_DATA:
                systemData(system, response);
                break;
            case GET_SCANFIELDS:
                scanFields(system, response);
                break;
            case GET_SCANNED_FIELDS:
                scannedFields(system, response);
                break;
            case GET_SECTOR_INFORMATION:
                sectorInformation(system, request, response);
                break;
            case GET_STARSYSTEM_MAP_DATA:
                getStarSystemMapData(request, response);
                break;
            case DEFAULT:
                defaultAction(system, ctx, request);
                templateEngine.process("starmap", ctx, response.getWriter());
                break;
        }
    }

    /**
     * Aktion zur Anzeige der Starmap
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     */
    private void defaultAction(int system, WebContext ctx, HttpServletRequest request){

        user = (User) context.getActiveUser();


        int x = 1;
        int y = 1;
        try {
            x = Integer.parseInt(request.getParameter("x"));
            y = Integer.parseInt(request.getParameter("y"));
        }catch(Exception e){}

        ctx.setVariable("system", system);
        ctx.setVariable("x", x);
        ctx.setVariable("y", y);

        List<StarsystemsSelectList> systemsViewModel = new ArrayList<>();

        var starsystems = StarsystemRepository.getInstance().getStarsystemsData();

        for (var starsystem: starsystems) {
            if(!starsystem.isVisibleFor(user)) continue;
            systemsViewModel.add(new StarsystemsSelectList(starsystem.id, starsystem.name + " ("+ starsystem.id +")"));
        }

        ctx.setVariable("starsystems", systemsViewModel);
        //ctx.setVariable("importantLocations", systemsViewModel);
    }

    private void systemData(int systemId, HttpServletResponse response) throws IOException
    {
        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        boolean admin = false;

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

    private void scanFields(int systemId, HttpServletResponse response) throws IOException
    {
        //System.out.println(new SimpleDateFormat("HH.mm.ss.SSS").format(new java.util.Date())); // Code to time method executions to find roots of slow performance

        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        User user = (User) context.getActiveUser();
        boolean admin = false;

        StarSystem sys = (StarSystem) db.get(StarSystem.class, systemId);
        validiereSystem(sys);

        MapController.MapViewModel jsonData = new MapController.MapViewModel();

        jsonData.system = new MapController.MapViewModel.SystemViewModel();
        jsonData.system.id = sys.getID();
        jsonData.system.width = sys.getWidth();
        jsonData.system.height = sys.getHeight();

        PublicStarmap content;
        if (admin && hasPermission(WellKnownAdminPermission.STARMAP_VIEW))
        {
            content = new AdminStarmap(sys.getID(), user);
        }
        else
        {
            content = new PlayerStarmap(user, sys.getID());
        }

        var json = new Gson().toJson(content.getScanSectorData());
        response.getWriter().write(json);
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!
    }

    private void scannedFields(int systemId, HttpServletResponse response) throws IOException
    {
        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        User user = (User) context.getActiveUser();
        boolean admin = false;

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

    public void sectorInformation(int systemId, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
        prepareResponseForJSON(response);
        org.hibernate.Session db = context.getDB();
        User user = (User) context.getActiveUser();
        boolean admin = false;

        StarSystem sys = (StarSystem) db.get(StarSystem.class, systemId);
        validiereSystem(sys);
        int x;
        int y;
        try {
            x = Integer.parseInt(request.getParameter("x"));
            y = Integer.parseInt(request.getParameter("y"));
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
			field = new AdminFieldView(db, loc, new AdminStarmap(sys.getID(), user));
		}
		else
		{
			field = new PlayerFieldView(user, loc, new PlayerStarmap(user, sys.getID()), context.getEM());
		}

        var ships = field.getShips();
		jsonData.users.addAll(exportSectorShips(field, user, ships));

        var stationaryObjects = new ArrayList<>(field.getBases());
        stationaryObjects.addAll(field.getBrocken());
		for (StationaryObjectData base : stationaryObjects)
		{
			MapController.SectorViewModel.BaseViewModel baseObj = new MapController.SectorViewModel.BaseViewModel();
			baseObj.id = base.id;
			baseObj.name = base.name;
			baseObj.username = Common._title(base.ownerName);
			baseObj.image = base.image;
			baseObj.klasse = base.typeId;
			baseObj.typ = base.typeName;
			baseObj.eigene = base.ownerId == user.getId();

			jsonData.bases.add(baseObj);
		}

		for (NodeData jumpNode : field.getJumpNodes())
		{
			MapController.SectorViewModel.JumpNodeViewModel jnObj = new MapController.SectorViewModel.JumpNodeViewModel();
			jnObj.id = jumpNode.id;
			jnObj.name = jumpNode.name;
			jnObj.blocked = jumpNode.blocked && Rassen.get().rasse(user.getRace()).isMemberIn(0);
			jsonData.jumpnodes.add(jnObj);
		}

		Nebel.Typ nebel = field.getNebel();
		if (nebel != null)
		{
			jsonData.nebel = new MapController.SectorViewModel.NebelViewModel();
			jsonData.nebel.type = nebel.getCode();
			jsonData.nebel.image = nebel.getImage();
		}

        //TODO: Handle battle scanner
		jsonData.battles.addAll(exportSectorBattles(field, false));

		jsonData.subraumspaltenCount = field.getJumpCount();
		jsonData.roterAlarm = field.isRoterAlarm();

        var json = new Gson().toJson(jsonData);
        response.getWriter().write(json);
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!
	}

    public void prepareResponseForJSON(HttpServletResponse response){
        response.resetBuffer();
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    private MapController.MapViewModel.LocationViewModel createMapLocationViewModel(PublicStarmap content, Location position)
    {
        boolean scannable = content.isScanned(position);
        if(!scannable) return null;

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
            if(posObj.scanner == -1) return null;
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

    public void getStarSystemMapData(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        prepareResponseForJSON(response);

        var data = StarsystemRepository.getStarSystemMapData(user.getId());

        var json = new Gson().toJson(data);
        response.getWriter().write(json);
        response.flushBuffer();
    }

    private List<MapController.SectorViewModel.UserWithShips> exportSectorShips(FieldView field, User user, Map<UserData, Map<net.driftingsouls.ds2.server.map.ShipTypeData, List<ShipData>>> ships)
	{
		List<MapController.SectorViewModel.UserWithShips> users = new ArrayList<>();
        var allShipTypes = ShipsRepository.getShipTypesData();
        //ShipsRepository.getShipTypes();
		for (Map.Entry<UserData, Map<net.driftingsouls.ds2.server.map.ShipTypeData, List<ShipData>>> owner : ships.entrySet())
		{
            var ownerInformation = owner.getKey();
			MapController.SectorViewModel.UserWithShips jsonUser = new MapController.SectorViewModel.UserWithShips();
			jsonUser.name = Common._text(ownerInformation.name);
			jsonUser.id = ownerInformation.id;
			jsonUser.race = ownerInformation.raceId;

			boolean ownFleet = ownerInformation.id == context.getActiveUser().getId();
			jsonUser.eigener = ownFleet;

			for (Map.Entry<net.driftingsouls.ds2.server.map.ShipTypeData, List<ShipData>> shiptype : owner.getValue().entrySet())
			{
                var typeData = shiptype.getKey();
				MapController.SectorViewModel.ShipTypeViewModel jsonShiptype = new MapController.SectorViewModel.ShipTypeViewModel();
				jsonShiptype.id = typeData.id;
				jsonShiptype.name = typeData.name;
				jsonShiptype.picture = typeData.picture;
				jsonShiptype.size = typeData.size;

				for (ShipData ship : shiptype.getValue())
				{
					MapController.SectorViewModel.ShipViewModel shipObj;
					if (ownFleet)
					{
						var ownShip = ownShipToViewModel(ship, field, typeData);
                        if(ship.landedShips.size() > 0)
                        {
                            var minResultLandedShip = ship.landedShips.get(0);
                            var landedShipTypes = new HashMap<Integer, List<ShipData>>();

                            for (var landedShip: ship.landedShips) {
                                if(!landedShipTypes.containsKey(landedShip.type)) landedShipTypes.put(landedShip.type, new ArrayList<>());
                                landedShipTypes.get(landedShip.type).add(landedShip);
                            }

                            for (var landedShiptype: landedShipTypes.keySet()) {
                                ownShip.landedShips.add(getLandedShipMinValues(landedShipTypes.get(landedShiptype), allShipTypes.get(landedShiptype)));
                            }
                        }
                        shipObj = ownShip;
					}
					else
					{
						shipObj = new MapController.SectorViewModel.ShipViewModel();
					}

                    addCommonDataToViewModel(shipObj, ship, typeData);

					if (ship.fleetId != null)
					{
						shipObj.fleet = new ShipFleetViewModel(ship.fleetId, ship.fleetName);
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

    private MapController.SectorViewModel.OwnShipViewModel ownShipToViewModel(ShipData ship, FieldView field, net.driftingsouls.ds2.server.map.ShipTypeData typeData)
    {
        MapController.SectorViewModel.OwnShipViewModel ownShip = new MapController.SectorViewModel.OwnShipViewModel();
        ownShip.gelandet = ship.landedShipCount;
        ownShip.maxGelandet = typeData.fighterDocks;

        ownShip.energie = ship.energy;
        ownShip.maxEnergie = typeData.maxEnergy;
        ownShip.x = field.getLocation().getX();
        ownShip.y = field.getLocation().getY();

        ownShip.ueberhitzung = ship.heat;

        ownShip.kannFliegen = typeData.movementCost > 0 && !ship.isDocked && !ship.isLanded;

        int sensorRange = (int)Math.floor(typeData.scanRange * ship.sensors/100d);
                        /* TODO: Adjust for nebel scan .. we probably need a view again
						if (field.getNebel() != null)
						{
							if(!ship.getTypeData().hasFlag(ShipTypeFlag.NEBELSCAN))
							{
								sensorRange /= 2;
							}
						}
                         */
        ownShip.sensorRange = sensorRange;

        return ownShip;
    }

    private MapController.SectorViewModel.LandedShipViewModel landedShipToViewModel(ShipData ship, net.driftingsouls.ds2.server.map.ShipTypeData typeData)
    {
        MapController.SectorViewModel.LandedShipViewModel landedShip = new MapController.SectorViewModel.LandedShipViewModel();
        landedShip.id = ship.id;
        landedShip.carrierId = ship.carrierId;
        landedShip.energie = ship.energy;
        landedShip.maxEnergie = typeData.maxEnergy;
        landedShip.name = ship.name;
        landedShip.type = ship.type;
        landedShip.typeName = typeData.name;
        landedShip.picture = typeData.picture;

        return landedShip;
    }

    private MapController.SectorViewModel.LandedShipViewModel getLandedShipMinValues(List<ShipData> landedShips, ShipTypeData typeData)
    {
        //result.energie = Math.min(result.energie, second.energie);
        int energy = Integer.MAX_VALUE;
        Map<Integer, ShipData.ShipDataAmmo> minAmmos = new HashMap<>();
        HashSet<Integer> ammoIds = new HashSet<>();

        for (var landedShip: landedShips) {
            ammoIds.addAll(landedShip.getAmmo().keySet());
        }
        for (var ammoId: ammoIds) {
            minAmmos.put(ammoId, new ShipData.ShipDataAmmo(ammoId, Long.MAX_VALUE, ""));
        }

        for(var landedShip: landedShips)
        {
            var cargoedAmmos = landedShip.getAmmo();
            for (var ammoId: ammoIds) {
                if(cargoedAmmos.containsKey(ammoId))
                {
                    minAmmos.put(ammoId, new ShipData.ShipDataAmmo(ammoId, Math.min(cargoedAmmos.get(ammoId).amount, minAmmos.get(ammoId).amount), ItemRepository.getInstance().getItemData(ammoId).getPicture()));
                }
                else minAmmos.put(ammoId, new ShipData.ShipDataAmmo(ammoId, 0, ItemRepository.getInstance().getItemData(ammoId).getPicture()));
            }
            energy = Math.min(energy, landedShip.energy);
        }

        var firstShip = landedShips.get(0);
        var result = new MapController.SectorViewModel.LandedShipViewModel();
        result.carrierId = firstShip.carrierId;
        result.ammoCargo = new ArrayList<>();
        result.energie = energy;
        result.picture = typeData.picture;
        result.maxEnergie = typeData.maxEnergy;
        result.type = typeData.id;
        result.typeName = typeData.name;
        result.count = landedShips.size();


        for (var minAmmo: minAmmos.values()) {
            result.ammoCargo.add(new MapController.SectorViewModel.LandedShipViewModel.AmmoCargo(minAmmo.id, minAmmo.amount, minAmmo.picture));
        }

        return result;
    }


    private void addCommonDataToViewModel(MapController.SectorViewModel.ShipViewModel shipObj, ShipData ship, net.driftingsouls.ds2.server.map.ShipTypeData typeData)
    {
        shipObj.id = ship.id;
        shipObj.name = ship.name;
        shipObj.gedockt = ship.dockedShips;
        shipObj.maxGedockt = typeData.externalDocks;
        shipObj.isOwner = ship.ownerId == user.getId();
        shipObj.race = ship.ownerRaceId;
    }

    private List<MapController.SectorViewModel.BattleViewModel> exportSectorBattles(FieldView field, boolean hasBattleScanner)
	{
		List<MapController.SectorViewModel.BattleViewModel> battleListObj = new ArrayList<>();
		List<BattleData> battles = field.getBattles();
		if (battles.isEmpty())
		{
			return battleListObj;
		}

		User user = (User) context.getActiveUser();
		boolean viewable = context.hasPermission(WellKnownPermission.SCHLACHT_ALLE_AUFRUFBAR) || hasBattleScanner;

		for (BattleData battle : battles)
		{
			MapController.SectorViewModel.BattleViewModel battleObj = new MapController.SectorViewModel.BattleViewModel();
			battleObj.id = battle.id;

            if(viewable) {
                battleObj.einsehbar = true;
            } else {
                if(battle.attackerId == user.getId() || battle.attackerAllyId == user.getAlly().getId() ||
                   battle.defenderId == user.getId() || battle.defenderAllyId == user.getAlly().getId()) {
                    battleObj.einsehbar = true;
                } else {
                    try(var conn = DBUtil.getConnection(context.getEM())) {
                        var db = DBUtil.getDSLContext(conn);
                        var select = db.select(count()).from(BATTLES_SHIPS)
                            .join(SHIPS)
                            .on(BATTLES_SHIPS.SHIPID.eq(SHIPS.ID))
                            .where(BATTLES_SHIPS.BATTLEID.eq(battle.id)
                                .and(SHIPS.OWNER.eq(user.getId())));

                        battleObj.einsehbar = select.fetchOne(count()) > 0;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            var attacker = new MapController.SectorViewModel.BattleSideViewModel();
            attacker.commander = new UserViewModel(battle.attackerRace, battle.attackerId, battle.attackerName, battle.plainAttackerName);
            if(battle.attackerAllyId != null) {
                attacker.ally = new AllyViewModel(battle.attackerAllyId, battle.attackerAllyName, battle.plainAttackerAllyName);
            }
            battleObj.sides.add(attacker);

            var defender = new MapController.SectorViewModel.BattleSideViewModel();
            defender.commander = new UserViewModel(battle.defenderRace, battle.defenderId, battle.defenderName, battle.plainDefenderName);
            if(battle.defenderAllyId != null) {
                defender.ally = new AllyViewModel(battle.defenderAllyId, battle.defenderAllyName, battle.plainDefenderAllyName);
            }
            battleObj.sides.add(defender);

			battleListObj.add(battleObj);
		}
		return battleListObj;
	}

    public static class Error{
        public String text;

        public Error(String text){
            this.text = text;
        }
    }

    public static class StarsystemsSelectList
    {
        public final int id;
        public final String name;

        public StarsystemsSelectList(int id, String name)
        {

            this.id = id;
            this.name = name;
        }
    }
}
