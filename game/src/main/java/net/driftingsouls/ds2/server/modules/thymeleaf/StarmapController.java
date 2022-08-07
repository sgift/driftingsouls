package net.driftingsouls.ds2.server.modules.thymeleaf;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
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
        DEFAULT
    }

    /**
     * Erzeugt die ComNetSeite (/comnet).
     * URL-Parameter:
     * action:
     *  <code>null</code>, default
     *  vorschau
     *  search
     *  read
     *  senden
     *  write
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

        if (scannable && content.isHasSectorContent(position))
        {
            posObj.scanner = content.getScanningShip(position);
        }
        else
        {
            return null;
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

    private static class Channel{
        public String action = "default";
        public ComNetChannel channel;
        public boolean showvorschau = false;
        public boolean showinputform = false;
        public boolean showsearchform = false;
        public boolean showsearcherror = false;
        public boolean showsubmit = false;
        public boolean showread = false;
        public boolean showchannellist = false;
        public boolean isreadable = false;
        public boolean iswriteable = false;
        public String name;
        public boolean showprivateinfo = false;
        public int showvor = 0;
        public int showback = 0;
        public boolean showreadnextpossible = false;
        public boolean isactive = false;
        public boolean newposts = false;

        public Channel(ComNetChannel channel){
            this.channel = channel;
            this.name = Common._title(channel.getName());
        }


    }
    public static class Error{
        public String text;

        public Error(String text){
            this.text = text;
        }
    }
}
