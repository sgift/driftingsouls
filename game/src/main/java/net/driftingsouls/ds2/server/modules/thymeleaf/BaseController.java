package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.bases.*;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import org.hibernate.Session;
import org.springframework.http.HttpHeaders;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.framework.ConfigService;
import org.json.*;
/**
 * Die Basis - Alle Funktionalitaeten der Basis befinden sich in
 * dieser Klasse.
 *
 * @author Gregor Fuhs
 */
public class BaseController implements DSController {
    private Context context;

    private static class BuildingComparator implements Comparator<Integer> {

		@Override
		public int compare(Integer o1, Integer o2) {
			int diff = Building.getBuilding(o1).getName().compareTo(Building.getBuilding(o2).getName());
			if( diff != 0 )
			{
				return diff;
			}
			return o1.compareTo(o2);
		}
	}

    private enum Action{
        CHANGEFEEDING,
        CHANGENAME,
        CHANGEBUILDINGSTATUS,
        SCAN,
        ACTIVATEALL,
        BUILD,
        UPDATE,
        DEFAULT
    }


    private boolean validateBase(Base base, WebContext ctx) {
		User user = (User)context.getActiveUser();

		if( base == null ) {
			Error error = new Error("Die angegebene Kolonie existiert nicht");
            ctx.setVariable("error",error);
            return false;
		}
		if (base.getOwner() != user)
		{
            Error error = new Error("Die angegebene Kolonie gehört nicht Ihnen");
            ctx.setVariable("error",error);
            return false;
		}

		base.getCargo().setOption( Cargo.Option.LINKCLASS, "schiffwaren" );
        return true;
	}

    private boolean validateScan(Base base, Ship ship, WebContext ctx) {

		if ((ship == null) || (ship.getId() < 0) || (ship.getOwner().getId() != ((User)context.getActiveUser()).getId()))
		{
            Error error = new Error ("Das angegebene Schiff existiert nicht oder gehört nicht Ihnen");
            ctx.setVariable("error", error);
            return false;
		}

		if( (base == null)) {
            Error error = new Error ("Die angegebene Kolonie existiert nicht");
            ctx.setVariable("error", error);
            return false;
		}

		if( ship.getCrew() < ship.getTypeData().getMinCrew())
		{
            Error error = new Error ("Das Schiff verfügt nicht über ausreichend Crew um den Asteroiden zu scannen");
            ctx.setVariable("error", error);
            return false;
		}

		if (ship.getSensors() == 0)
		{
            Error error = new Error ("Die Sensoren des Schiffes sind defekt. Ein Asteroidenscan ist daher nicht möglich");
            ctx.setVariable("error", error);
            return false;
		}

		if(!ship.getTypeData().hasFlag(ShipTypeFlag.ASTISCAN))
		{
            Error error = new Error ("Dieses Schiff besitzt keinen Asteroidenscanner und kann diese Kolonie daher nicht scannen");
            ctx.setVariable("error", error);
            return false;
		}

		if (!ship.getLocation().sameSector(0, base.getLocation(), base.getSize()))
		{
            Error error = new Error ("Der Asteroid befindet sich nicht im selben Sektor wie das Schiff");
            ctx.setVariable("error", error);
            return false;
		}

		if (ship.getBattle() != null)
		{
            Error error = new Error ("Das angegebene Schiff befindet sich in einer Schlacht");
            ctx.setVariable("error", error);
            return false;
		}

		if(ship.getEnergy() < new ConfigService().getValue(WellKnownConfigValue.ASTI_SCAN_COST) )
		{
            Error error = new Error ("Nicht ausreichend Energie für den Asteroidenscan vorhanden");
            ctx.setVariable("error", error);
            return false;
		}
        return true;
	}
  
    /**
     * Erzeugt die BaseSeite (/base). 
     * URL-Parameter:
     * action: 
     *  <code>null</code>, default 
     *  changefeeding
     *  changename
     *  changebuildingstatus
     *  scan
     *  activateall
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
        ctx.setVariable("action",action);
        Base base = null;
        try{
            base = Base.getBaseById(Integer.parseInt(request.getParameter("col")));
        }catch(Exception e){
            Error error = new Error("Die angegebene Kolonie existiert nicht");
            ctx.setVariable("error",error);
            templateEngine.process("base", ctx, response.getWriter());
            return;
        }
        if(action != Action.SCAN && !validateBase(base, ctx)){
            templateEngine.process("base", ctx, response.getWriter());
            return;
        }

        switch(action){
          case CHANGEFEEDING:
            changeFeedingAction(ctx, request, base);
            break;
          case CHANGENAME:
            changeNameAction(ctx, request, base);
            break;
          case CHANGEBUILDINGSTATUS:
            changeBuildingStatusAction(ctx, request, base);
            break;
          case SCAN:
          Ship ship = null;
            try{
                ship = Ship.getShipById(Integer.parseInt(request.getParameter("ship")));
            }catch(Exception e){}
            scanAction(ctx, request, base, ship);
            templateEngine.process("base", ctx, response.getWriter());
            //wir wollen nur scannen, nicht die Basis komplett anzeigen, daher hier return
            return;
          case ACTIVATEALL:
            activateAllAction(ctx, request, base);
            break;
          case BUILD:
            buildAction(request,base, response);
            //Seitenupdate wird in Javascript gemacht, daher hier ein return
            return;
          case UPDATE:
            updateAction(request,base,response);
            return;
            //Seitenupdate wird in Javascript gemacht, daher hier ein return
          default:
            break;
        }
        defaultAction(ctx, request, base);
        templateEngine.process("base", ctx, response.getWriter());
    }

    public void prepareResponseForJSON(HttpServletResponse response){
        response.resetBuffer();
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.setCharacterEncoding("UTF-8");
    }

    public void JSONPostERROR(String message, JSONObject json, HttpServletResponse response) throws IOException {
        prepareResponseForJSON(response);
        json.put("success",false);
        json.put("message",message);
        response.getWriter().write(json.toString());
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!
    }

    public void updateAction(HttpServletRequest request, Base base, HttpServletResponse response) throws IOException {

        prepareResponseForJSON(response);
        JSONObject json = new JSONObject();
        if(request.getParameter("field")!= null){
            Integer field = -1;
            try{
                field = Integer.parseInt(request.getParameter("field"));
            }catch(Exception e){
            }
            JSONObject g = new JSONObject();
            g.put("geb_id",-1);
            g.put("field",field);
            g.put("ground","data/buildings/ground"+base.getTerrain()[field]+".png");
            json.put("gebaut",g);
            json.put("success",true);
        }
        json.put("id",base.getId());
        addFullCargoToJSON(json, base);
        addBaumenuDiffToJSON(json, base);
        addEnergyToJSON(json, base);
        addBevoelkerungToJSON(json, base);
        addBuildingsActionsToJSON(json, base);
        response.getWriter().write(json.toString());
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!
    }

    public void buildAction(HttpServletRequest request, Base base, HttpServletResponse response) throws IOException {

        int buildingid = -1;
        int field = -1;
        boolean success = false;
        prepareResponseForJSON(response);
        JSONObject json  = new JSONObject();
        try {
            buildingid = Integer.parseInt(request.getParameter("building"));
            field = Integer.parseInt(request.getParameter("field"));

        }catch(NumberFormatException e){
            JSONPostERROR("Ung&uuml;tige Parameter: Parameter konnten nicht geparst werden.",json,response);
            return;
        }
        json.put("field",field);

        User user = (User) context.getActiveUser();

        //Darf das Gebaeude ueberhaupt gebaut werden?
        if (field >= base.getWidth() * base.getHeight())
        {
            JSONPostERROR("Ung&uuml;tige Parameter: Feld existiert nicht.",json,response);
            return;
        }

        Building building = Building.getBuilding(buildingid);
        if(building == null)
        {
            JSONPostERROR("Ung&uuml;tige Parameter: Geb&auml;de ung&uuml;ltig.",json,response);
            return;
        }

        //Anzahl der Gebaeude berechnen
        Map<Integer, Integer> buildingcount = berechneGebaeudeanzahlDieserBasis(base);

        //Anzahl der Gebaeude pro Spieler berechnen
        Map<Integer, Integer> ownerbuildingcount = berechneGebaeudeanzahlAllerBasen(user);

        //Anzahl der Gebaeude berechnen
        if (building.getPerPlanetCount() != 0)
        {
            if (buildingcount.containsKey(building.getId()) && building.getPerPlanetCount() <= buildingcount.get(building.getId()))
            {
                JSONPostERROR("Maximale Anzahl pro Asteroid (" + building.getPerPlanetCount() + ") erreicht.",json,response);
                return;
            }
        }

        //Anzahl der Gebaeude pro Spieler berechnen
        if (building.getPerUserCount() != 0)
        {
            if (ownerbuildingcount.containsKey(building.getId()) && building.getPerUserCount() <= ownerbuildingcount.get(building.getId()))
            {
                JSONPostERROR("Sie k&ouml;nnen dieses Geb&auml;ude maximal " + building.getPerUserCount() + " Mal insgesamt bauen",json,response);
                return;
            }
        }

        // Pruefe auf richtiges Terrain
        if (!building.hasTerrain(base.getTerrain()[field]))
        {
            JSONPostERROR("Dieses Geb&auml;ude ist nicht auf diesem Terrainfeld baubar.",json,response);
            return;
        }

        if (base.getBebauung()[field] != 0)
        {
            JSONPostERROR("Es existiert bereits ein Geb&auml;ude an dieser Stelle",json,response);
            return;
        }

        if (building.isUComplex())
        {
            int c = berechneAnzahlUnterirdischerKomplexe(context.getDB(), buildingcount);
            int grenze = berechneMaximaleAnzahlUnterirdischerKomplexe(base);

            if (c > grenze - 1)
            {
                JSONPostERROR("Maximale Anzahl (" + grenze + ") unterirdischer Komplexe erreicht",json,response);
                return ;
            }
        }
        if (!Rassen.get().rasse(user.getRace()).isMemberIn(building.getRace()))
        {
            JSONPostERROR("Sie geh&ouml;ren der falschen Spezies an und k&ouml;nnen dieses Geb&auml;ude nicht selbst errichten.",json,response);
            return ;
        }
        if (!user.hasResearched(building.getTechRequired()))
        {
            JSONPostERROR("Sie verf&uuml;gen nicht über alle n&ouml;tigen Forschungen um dieses Geb&auml;ude zu bauen",json,response);
            return ;
        }
        //Alle technischen Fehler des Baus geprueft, sieht bislang gut aus. Also setzen wir den Erfolg erstmal
        success = true;
        //noetige Resourcen berechnen/anzeigen
        Cargo basecargo = base.getCargo();

        //jetzt pruefen wir noch, ob ausreichend Baumaterial verfuegbar
        ResourceList compreslist = building.getBuildCosts().compare(basecargo, false);
        for (ResourceEntry compres : compreslist)
        {
            if (compres.getDiff() > 0)
            {
                //und entziehen hier ggfs den Erfolg wieder
                success = false;
                break;
            }
        }

        // Alles OK -> bauen
        if (success)
        {
            Integer[] bebauung = base.getBebauung();
            bebauung[field] = building.getId();
            base.setBebauung(bebauung);

            Integer[] active = base.getActive();
            // Muss das Gebaeude aufgrund von Arbeitermangel deaktiviert werden?
            boolean aktiviert = false;
            if ((building.getArbeiter() > 0) && (building.getArbeiter() + base.getArbeiter() > base.getBewohner()))
            {
                active[field] = 0;
            }
            else
            {
                active[field] = 1;
                aktiviert = true;
            }
            // Resourcen abziehen
            basecargo.substractCargo(building.getBuildCosts());
            base.setCargo(basecargo);
            base.setArbeiter(base.getArbeiter() + building.getArbeiter());
            base.setActive(active);

            // Evt. muss das Gebaeude selbst noch ein paar Dinge erledigen
            building.build(base, building.getId());

            JSONObject g = new JSONObject();
            g.put("geb_id",buildingid);
            g.put("field",field);
            g.put("offline",aktiviert? "" : "offline");
            g.put("bildpfad",building.getPictureForRace(user.getRace()));
            g.put("col",base.getId());
            g.put("geb_name",building.getPlainName());
            json.put("gebaut",g);
            json.put("message", "Geb&auml;de "+building.getPlainName()+" erfolgreich gebaut.");
        }
        else
        {
            //scheinbar waren doch nicht ausreichend Ress da, weil vielleicht in der Zwischenzeit Ress transferiert wurden
            json.put("message", "Geb&auml;de "+building.getPlainName()+" aus Ressourcenmangel nicht gebaut.");
        }
        //und deshalb laden wir hier auch noch einmal den Cargo und das Baumenu neu, dass die sich updaten
        json.put("success",success);
        json.put("id",base.getId());
        //Jetzt das Baumenue, nur Aenderungen
        addBaumenuDiffToJSON(json, base);
        //Jetzt den neuen Cargo:
        addFullCargoToJSON(json, base);
        addEnergyToJSON(json,base);
        addBevoelkerungToJSON(json,base);
        addBuildingsActionsToJSON(json, base);
        response.getWriter().write(json.toString());
        response.flushBuffer(); // marks response as committed -- if we don't do this the request will go through normally!

    }

    /**
     * fuegt den gesamten Basiscargo dem uebergebenen JSONObject hinzu
     * @param json das zu erweiternde JSONObject
     * @param base die Basis, deren Cargo hinzugefuegt werden soll
     */
    public void addFullCargoToJSON(JSONObject json, Base base){
        JSONArray ja = new JSONArray();
        for(ResourceEntry r : base.getCargoResourceList()){
            JSONObject jo = new JSONObject();
            jo.put("ress_name",r.getPlainName());
            jo.put("menge",r.getCount1());
            jo.put("produktion", r.getCount2());
            jo.put("kategorie",convertItemEffectType2ItemTyp(Item.getItemById(r.getId().getItemID()).getEffect().getType()).getName());
            jo.put("bildpfad",r.getImage());
            ja.put(jo);
        }
        JSONObject empty = new JSONObject();
        empty.put("empty", base.getCargoEmpty());
        empty.put("max",base.getMaxCargo());
        empty.put("change", base.getCstat());
        json.put("empty_cargo",empty);
        json.put("cargo",ja);
    }

    public void addEnergyToJSON(JSONObject json, Base base){
        JSONObject energy = new JSONObject();
        energy.put("gespeicherte_energie", base.getEnergy_formated());
        energy.put("energiebilanz", base.getEstat_formated());
        json.put("energy",energy);
    }
    public void addBevoelkerungToJSON(JSONObject json, Base base){
        JSONObject bev = new JSONObject();
        BaseStatus basedata = Base.getStatus(base);
        bev.put("arbeiter", basedata.getArbeiter());
        bev.put("einwohner",base.getBewohner());
        bev.put("wohnraum",base.getWohnraum());
        bev.put("arbeitslos",Math.max(base.getBewohner()-basedata.getArbeiter(),0));
        bev.put("wohnraumfrei",Math.max(basedata.getLivingSpace()-base.getBewohner(),0));
        bev.put("wohnraumfehlt",Math.max(base.getBewohner()-basedata.getLivingSpace(),0));
        json.put("stats",bev);
    }

    public void addBuildingsActionsToJSON(JSONObject json, Base base){
        JSONArray act = new JSONArray();
        TreeMap<Integer,Integer> buildingonoffstatus = new TreeMap<>(new BuildingComparator());

        for( int i = 0; i < base.getWidth() * base.getHeight(); i++ ) {
            if( base.getBebauung()[i] != 0 ) {
                Building building = Building.getBuilding(base.getBebauung()[i]);
                if( !buildingonoffstatus.containsKey(base.getBebauung()[i]) ) {
                    buildingonoffstatus.put(base.getBebauung()[i], 0);
                }
                if( building.isDeakAble() ) {
                    if( buildingonoffstatus.get(base.getBebauung()[i]) == 0 ) {
                        buildingonoffstatus.put( base.getBebauung()[i], base.getActive()[i] + 1 );
                    }
                    else if( buildingonoffstatus.get(base.getBebauung()[i]) != base.getActive()[i] + 1 ) {
                        buildingonoffstatus.put(base.getBebauung()[i],-1);
                    }
                }
            }
        }

        for( Map.Entry<Integer,Integer> entry : buildingonoffstatus.descendingMap().entrySet() ) {
            int bstatus = entry.getValue();
            JSONObject b = new JSONObject();
            Building building = Building.getBuilding(entry.getKey());
            b.put("name",building.getPlainName());
            b.put("buildingTypeId",building.getId());
            b.put("deaktivierbar",(bstatus == -1) || (bstatus == 2));
            b.put("aktivierbar",(bstatus == -1) || (bstatus == 1));
            act.put(b);
        }
        json.put("buildingActions",act);
    }



    /**
     * fuegt alle Aenderungen des Baumenues in die uebergebene JSON
     * @param json das JSONObject, das erweitert werden soll
     * @param base die Basis, auf der das Baumenu angezeigt werden soll
     */
    public void addBaumenuDiffToJSON(JSONObject json, Base base){
        if(base == null){
            return;
        }
        Cargo basecargo = base.getCargo();
        JSONArray jb = new JSONArray();
        Iterator<?> buildingIter = context.getDB().createQuery("from Building  order by name")
                .iterate();
        for (; buildingIter.hasNext(); ) {
            Building b = (Building) buildingIter.next();
            JSONArray ja = new JSONArray();
            for(ResourceEntry r : b.getBuildCosts().compare(basecargo, false)){
                if(r.getDiff() > 0) {
                    JSONObject jo = new JSONObject();
                    jo.put("ress_id",r.getId().getItemID());
                    ja.put(jo);
                }
            }
            if(ja != null && !ja.isEmpty()) {
                JSONObject job = new JSONObject();
                job.put("geb_id", b.getId());
                job.put("mangel", ja);
                jb.put(job);
            }
        }
        json.put("buildings",jb);
    }

    /**
     * Aktion zur Aenderung des Versorgunsstatus einer Basis.
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param base die Basis
     * @URL-Parameter action: changefeeding 
     * @URL-Parameter feeding Der neue Versorgungsstatus der Basis
     * @URL-Parameter col Die ID der Basis
     */
    public void changeFeedingAction(WebContext ctx, HttpServletRequest request, Base base){

        var feeding = 0;
        try{
            feeding = Integer.parseInt(request.getParameter("feeding"));
        } catch(Exception e){
            Error error = new Error("Versorgungsoption unbekannt");
            ctx.setVariable("error", error);
            return;
        }
		String message = null;
		switch (feeding)
		{
			case 0:
				base.setFeeding(false);
				message = "Versorgung abgeschaltet!";
				break;
			case 1:
				base.setFeeding(true);
				message = "Versorgung angeschaltet.";
				break;
			case 2:
				base.setLoading(false);
				message = "Automatisches Auffüllen abgeschaltet!";
				break;
			case 3:
				base.setLoading(true);
				message = "Automatisches Auffüllen angeschaltet!";
				break;
		}
        ctx.setVariable("message",message);
    }

    /**
     * Aktion zur Aenderung des Namen einer Basis.
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param base die Basis
     * @URL-Parameter action: changename 
     * @URL-Parameter newname Der neue Name der Basis
     * @URL-Parameter col Die ID der Basis
     */
    public void changeNameAction(WebContext ctx, HttpServletRequest request, Base base){
        var newname = request.getParameter("newname");
        if( newname.length() > 50 ) {
			newname = newname.substring(0,50);
		}
		if( newname.trim().isEmpty() )
		{
			newname = "Kolonie "+base.getId();
		}

		base.setName(newname);
        String message = "Name zu " + Common._plaintitle(newname) + " geändert";
        ctx.setVariable("message", message);
    }
    
    /** 
     * Aktion zur (De)Aktivierung von Gebaeudegruppen.
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param base die Basis
     * @URL-Parameter action: changebuildingstatus 
     * @URL-Parameter act <code>false</code>, wenn die Gebaeude deaktiviert werden sollen. Andernfalls <code>true</code>
     * @URL-Parameter buildingonoff Die ID des Gebaeudetyps, dessen Gebaeude (de)aktiviert werden sollen
     * @URL-Parameter col Die ID der Basis
     */
    public void changeBuildingStatusAction(WebContext ctx, HttpServletRequest request, Base base){
        
        int act = 0;
        String message = null;
        try{
            act = Integer.parseInt(request.getParameter("act"));
        }catch(Exception e){}
        int buildingonoff = -1;
        try{
            buildingonoff = Integer.parseInt(request.getParameter("buildingonoff"));
        }catch(Exception e){
            message = "Ung&uuml;ltige Geb&auml;ude-ID angegeben";
            ctx.setVariable("message", message);
            return;
        }

		Building building = Building.getBuilding(buildingonoff);

		// Wenn das Gebaude automatisch abschalten soll und der Besitzer
		// die entsprechenden Forschungen oder die Rasse nicht hat
		// bleibt das Gebaeude aus (Rasse != GCP)
		if( act == 1 && building.isShutDown() &&
				(!base.getOwner().hasResearched(building.getTechRequired())
						|| ((base.getOwner().getRace() != building.getRace()) && building.getRace() != 0)))
		{
			message = "<span style=\"color:red\">Sie haben nicht die notwendigen Voraussetzungen, um diese Gebäude aktivieren zu können</span>";
		}
		else if( building.isDeakAble() ) {
			int count = 0;
			Integer[] active = base.getActive();

			for( int i=0; i <= base.getWidth()*base.getHeight()-1 ; i++ ) {

				if( (base.getBebauung()[i] == buildingonoff) && (active[i] != act) ) {
					if(act == 0 || (base.getBewohner() >= base.getArbeiter() + building.getArbeiter())) {
						active[i] = act;

						count++;

						if( act != 0 ) {
							base.setArbeiter(base.getArbeiter()+building.getArbeiter());
						}
					}
				}
			}

			base.setActive(active);

			if( count != 0 ) {
				String result;

				if( act != 0 ) {
					result = "<span style=\"color:green\">";
				}
				else {
					result = "<span style=\"color:red\">";
				}
				result += count+" Gebäude wurde"+(count > 1 ? "n" : "")+' '+(act != 0 ? "" : "de")+"aktiviert</span>";

				message = result;
			}
		}
		else {
			message = "<span style=\"color:red\">Sie können diese Gebäude nicht deaktivieren</span>";
		}
        ctx.setVariable("message", message);
    }

    /** 
     * Aktion zur Anzeige der Basis. Wird ein Schiff uebergeben, wird der Asti gescannt, wird <code>null</code> uebergeben, wird der Asti normal aufgerufen.
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param base die Basis
     * @URL-Parameter action: scan 
     * @URL-Parameter col Die ID der Basis
     * @URL-Parameter ship Die ID des scannenden Schiffs
     */
    public void scanAction(WebContext ctx, HttpServletRequest request, Base base, Ship ship){
        boolean scan = ship != null;
        if (!scan)
        {
            if(!validateBase(base, ctx)){
                return;
            }
        }
        else{
            if(!validateScan(base,ship, ctx)){
                return;
            }
            int e = new ConfigService().getValue(WellKnownConfigValue.ASTI_SCAN_COST);
            ship.setEnergy(ship.getEnergy() - e);
        }
		User user = (User)context.getActiveUser();
        ctx.setVariable("base", base);
        ctx.setVariable("scan",scan);

		BaseStatus basedata = Base.getStatus(base);

		//------------------
		// Core
		//------------------
		if( base.getCore() != null ) {
			Core core = base.getCore();
            ctx.setVariable("core", core);
		}

		//----------------
		// Karte
		//----------------
		Map<Integer,Integer> buildingonoffstatus = new TreeMap<>(new BuildingComparator());

        List<List<Tile>> tiles = new ArrayList<>();
        List<Tile> tilerow = new ArrayList<>();
		for( int i = 0; i < base.getWidth() * base.getHeight(); i++ ) {
			String image;
            Tile tile = new Tile();
			//Leeres Feld
			if( base.getBebauung()[i] == 0 ) {
				image = "data/buildings/ground"+base.getTerrain()[i]+".png";
				base.getActive()[i] = 2;
			}
			else {
				Building building = Building.getBuilding(base.getBebauung()[i]);
				base.getActive()[i] = basedata.getActiveBuildings()[i];

				if( !buildingonoffstatus.containsKey(base.getBebauung()[i]) ) {
					buildingonoffstatus.put(base.getBebauung()[i], 0);
				}
				if( building.isDeakAble() ) {

					if( buildingonoffstatus.get(base.getBebauung()[i]) == 0 ) {
						buildingonoffstatus.put( base.getBebauung()[i], base.getActive()[i] + 1 );
					}
					else if( buildingonoffstatus.get(base.getBebauung()[i]) != base.getActive()[i] + 1 ) {
						buildingonoffstatus.put(base.getBebauung()[i],-1);
					}
				}

				image = building.getPictureForRace(user.getRace());
                
				if( building.isDeakAble() && (base.getActive()[i] == 0) ) {
                   tile.setOverlay(true);
                   tile.setOverlayImage("overlay_offline.png");
				}
                tile.setBuilding(true);
                tile.setBuildingName(Common._plaintitle(building.getName()));
                tile.setBuildingId(building.getId());
                tile.setBuildingJson(building.isSupportsJson());
			}
            tile.setField(i);
            tile.setBuildingImage(image);
            tile.setId(i);
            //endrow
            tilerow.add(tile);
            //maximal 10 pro Tilerow, da sonst die Map zu breit wird
            if((i+1)%Math.min(base.getWidth(),10) == 0){
            // if((i+1)%base.getWidth() == 0){
                tiles.add(tilerow);
                tilerow = new ArrayList<>();
            }
		}
        ctx.setVariable("tiles",tiles);

		//----------------
		// Waren
		//----------------

		base.setArbeiter(basedata.getArbeiter());

		ResourceList reslist = base.getCargo().compare(basedata.getProduction(), true,true);
        reslist.sortByID(false);
        List<RessView> ress = new ArrayList<>();
        for(ItemTyp type : ItemTyp.values()) {
            List<ResourceEntry> list = reslist.stream().filter(res -> convertItemEffectType2ItemTyp(Item.getItemById(res.getId().getItemID()).getEffect().getType()).equals(type)).collect(Collectors.toList());
            RessView view = new RessView(type.getName(),list);
            ress.add(view);
        }
        ctx.setVariable("cargo",ress);


		//----------------
		// Aktionen
		//----------------

        List<BuildingGroup> buildings = new ArrayList<>();

		for( Map.Entry<Integer,Integer> entry : buildingonoffstatus.entrySet() ) {
			int bstatus = entry.getValue();

			Building building = Building.getBuilding(entry.getKey());
            BuildingGroup bv = new BuildingGroup(building,entry.getKey(),(bstatus == -1) || (bstatus == 2),(bstatus == -1) || (bstatus == 1)  );
            buildings.add(bv);
		}
        ctx.setVariable("buildings",buildings);

		//-----------------------------------------
		// Energieverbrauch, Bevoelkerung, Einheiten usw.
		//------------------------------------------
        if(!base.getUnits().isEmpty())
        {
            List<UnitCargoEntry> units = base.getUnits().getUnitList();
            ctx.setVariable("units",units);
        }


        Bevoelkerung bevoelkerung = new Bevoelkerung(base,basedata);

		ctx.setVariable("bevoelkerung", bevoelkerung);
        if(!scan)
        {
            erzeugeBaumenue(base, ctx);
        }
    }
    
    /** 
     * Aktion zur Anzeige der Basis
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param base die Basis
     * @URL-Parameter action: default 
     * @URL-Parameter col Die ID der Basis
     */
    public void defaultAction(WebContext ctx, HttpServletRequest request, Base base){
        scanAction(ctx, request, base,  null);
    }

    /**
     * Aktion zur Anzeige der Basis
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param base die Basis
     * @URL-Parameter action: activateAll
     * @URL-Parameter col Die ID der Basis
     * @URL-Parameter deaconly <code>true</code>, falls die Gebaeude/Cores nur deaktiviert, nicht aber aktiviert werden sollen
     */
    public void activateAllAction(WebContext ctx, HttpServletRequest request, Base base)
    {
        ctx.setVariable("base", base);
        boolean deaconly = false;
        try{
            deaconly = request.getParameter("deaconly").equals("1") || Boolean.parseBoolean(request.getParameter("deaconly")) ;
        }catch(Exception e){}
        /*
			Alle Gebaeude deaktivieren
		*/
        Core core = base.getCore();
        String name = null;
        List<String> deak = new ArrayList<>();
        if ((core != null) && base.isCoreActive())
        {

            base.setArbeiter(base.getArbeiter() - core.getArbeiter());
            base.setCoreActive(false);

            if (deaconly)
            {
                name = Common._plaintitle(core.getName());
                deak.add(name);
                name=null;
            }
        }

        Integer[] ondb = base.getActive();
        for (int i = 0; i < base.getWidth() * base.getHeight(); i++)
        {
            if ((base.getBebauung()[i] != 0) && (ondb[i] == 1))
            {
                Building building = Building.getBuilding(base.getBebauung()[i]);

                if (building.isDeakAble())
                {
                    ondb[i] = 0;
                    base.setArbeiter(base.getArbeiter() - building.getArbeiter());

                    if (deaconly)
                    {
                        name = Common._plaintitle(building.getName());
                        deak.add(name);
                    }
                }
            }
        }
        ctx.setVariable("deak",deak);

        base.setActive(ondb);

		/*
			Falls gewuenscht, nun alle Gebaeude nacheinander aktivieren
		*/
        if (!deaconly)
        {
            List<Activate> activate = new ArrayList<>();
            if (core != null)
            {
                if (base.getBewohner() >= base.getArbeiter() + core.getArbeiter())
                {
                    base.setArbeiter(base.getArbeiter() + core.getArbeiter());
                    base.setCoreActive(true);
                    Activate act = new Activate( Common._plaintitle(core.getName()),true);
                    activate.add(act);
                }
                else
                {
                    Activate act = new Activate( Common._plaintitle(core.getName()),false);
                    activate.add(act);
                }
            }

            for (int i = 0; i < base.getWidth() * base.getHeight(); i++)
            {
                if (base.getBebauung()[i] != 0)
                {
                    Building building = Building.getBuilding(base.getBebauung()[i]);

                    if (building.isDeakAble() && (base.getBewohner() >= base.getArbeiter() + building.getArbeiter()))
                    {
                        ondb[i] = 1;
                        base.setArbeiter(base.getArbeiter() + building.getArbeiter());
                        Activate act = new Activate( Common._plaintitle(building.getName()),true);
                        activate.add(act);
                    }
                    else if (building.isDeakAble())
                    {
                        Activate act = new Activate( Common._plaintitle(building.getName()),false);
                        activate.add(act);
                    }
                }
            }

            base.setActive(ondb);
            ctx.setVariable("activate",activate);
        }

    }

    /**
     * Zeigt die Liste der baubaren Gebaeude, sortiert nach Kategorien, an.
     *  @param base Die Basis, auf der das Gebaeude gebaut werden soll
     *  @param ctx der Webkontext
     */
    public void erzeugeBaumenue(Base base, WebContext ctx)
    {
        User user = (User) context.getActiveUser();
        org.hibernate.Session db = context.getDB();

        //Anzahl der Gebaeude berechnen
        Map<Integer, Integer> buildingcount = berechneGebaeudeanzahlDieserBasis(base);

        //Anzahl der Gebaeude pro Spieler berechnen
        Map<Integer, Integer> ownerbuildingcount = berechneGebaeudeanzahlAllerBasen(user);

        Cargo basecargo = base.getCargo();

        //Max UComplex-Gebaeude-Check
        int grenze = berechneMaximaleAnzahlUnterirdischerKomplexe(base);
        int c = berechneAnzahlUnterirdischerKomplexe(db, buildingcount);

        boolean ucomplex = c <= grenze - 1;

        List<BuildingKategorie> kategorien = new ArrayList<>();

        BuildingKategorie energie    = new BuildingKategorie("Energie",    new ArrayList<>() );
        BuildingKategorie nahrung    = new BuildingKategorie("Nahrung",    new ArrayList<>() );
        BuildingKategorie metalle    = new BuildingKategorie("Metalle",    new ArrayList<>() );
        BuildingKategorie produktion = new BuildingKategorie("Produktion", new ArrayList<>() );
        BuildingKategorie sonstiges  = new BuildingKategorie("Sonstiges",  new ArrayList<>() );

        //Alle Gebaeude ausgeben
        Iterator<?> buildingIter = db.createQuery("from Building  order by name")
                .iterate();
        for (; buildingIter.hasNext(); )
        {
            Building building = (Building) buildingIter.next();

            if (!user.hasResearched(building.getTechRequired()))
            {
                continue;
            }
            if (!Rassen.get().rasse(user.getRace()).isMemberIn(building.getRace()))
            {
                continue;
            }

            Bauprojekt bp = new Bauprojekt(building, basecargo);

            //Existiert bereits die max. Anzahl dieses Geb. Typs auf dem Asti?
            if ((building.getPerPlanetCount() != 0) && buildingcount.containsKey(building.getId()) &&
                    (building.getPerPlanetCount() <= buildingcount.get(building.getId())))
            {
                bp.setBaubar(false);
                switch(building.getCategory()){
                    case 0:
                        energie.gebaeude.add(bp);
                        continue;
                    case 1:
                        nahrung.gebaeude.add(bp);
                        continue;
                    case 2:
                        metalle.gebaeude.add(bp);
                        continue;
                    case 3:
                        produktion.gebaeude.add(bp);
                        continue;
                    default:
                        sonstiges.gebaeude.add(bp);
                        continue;
                }
            }

            if ((building.getPerUserCount() != 0) && ownerbuildingcount.containsKey(building.getId()) &&
                    (building.getPerUserCount() <= ownerbuildingcount.get(building.getId())))
            {
                bp.setBaubar(false);
                switch(building.getCategory()){
                    case 0:
                        energie.gebaeude.add(bp);
                        continue;
                    case 1:
                        nahrung.gebaeude.add(bp);
                        continue;
                    case 2:
                        metalle.gebaeude.add(bp);
                        continue;
                    case 3:
                        produktion.gebaeude.add(bp);
                        continue;
                    default:
                        sonstiges.gebaeude.add(bp);
                        continue;
                }
            }

            if (!ucomplex && building.isUComplex())
            {
                bp.setBaubar(false);
                switch(building.getCategory()){
                    case 0:
                        energie.gebaeude.add(bp);
                        continue;
                    case 1:
                        nahrung.gebaeude.add(bp);
                        continue;
                    case 2:
                        metalle.gebaeude.add(bp);
                        continue;
                    case 3:
                        produktion.gebaeude.add(bp);
                        continue;
                    default:
                        sonstiges.gebaeude.add(bp);
                        continue;
                }
            }
            bp.setBaubar(true);
            switch(building.getCategory()){
                case 0:
                    energie.gebaeude.add(bp);
                    continue;
                case 1:
                    nahrung.gebaeude.add(bp);
                    continue;
                case 2:
                    metalle.gebaeude.add(bp);
                    continue;
                case 3:
                    produktion.gebaeude.add(bp);
                    continue;
                default:
                    sonstiges.gebaeude.add(bp);
                    continue;
            }
        }
        kategorien.add(energie);
        kategorien.add(nahrung);
        kategorien.add(metalle);
        kategorien.add(produktion);
        kategorien.add(sonstiges);

        ctx.setVariable("kategorien", kategorien);

    }

    //Hilfsfunktionen

    private static int berechneMaximaleAnzahlUnterirdischerKomplexe(Base base)
    {
        return (base.getWidth() * base.getHeight()) / 8;
    }

    private static int berechneAnzahlUnterirdischerKomplexe(Session db, Map<Integer, Integer> buildingcount)
    {
        int c = 0;

        Iterator<?> ucBuildingIter = db.createQuery("from Building where ucomplex=true").iterate();
        for (; ucBuildingIter.hasNext(); )
        {
            Building building = (Building) ucBuildingIter.next();
            if (buildingcount.containsKey(building.getId()))
            {
                c += buildingcount.get(building.getId());
            }
        }
        return c;
    }

    private static Map<Integer, Integer> berechneGebaeudeanzahlDieserBasis(Base base)
    {
        Map<Integer, Integer> buildingcount = new HashMap<>();
        for (int building : base.getBebauung())
        {
            Common.safeIntInc(buildingcount, building);
        }
        return buildingcount;
    }

    private static Map<Integer, Integer> berechneGebaeudeanzahlAllerBasen(User user)
    {
        Map<Integer, Integer> ownerbuildingcount = new HashMap<>();

        for (Base abase : user.getBases())
        {
            for (int bid : abase.getBebauung())
            {
                Common.safeIntInc(ownerbuildingcount, bid);
            }
        }
        return ownerbuildingcount;
    }


    //ViewKlassen fuer Thymeleaf

    private static class RessView{
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public List<ResourceEntry> getRess() {
            return ress;
        }
        public void setRess(List<ResourceEntry> ress) {
            this.ress = ress;
        }
        public int getAmount(){
            return ress.size();
        }
        public RessView(String name, List<ResourceEntry> ress) {
            this.name = name;
            this.ress = ress;
        }

        public List<ResourceEntry> ress;
        public String name;
    }

    private static class BuildingKategorie{
        public String name;
        public List<Bauprojekt> gebaeude;

        public BuildingKategorie(String name, List<Bauprojekt> gebauede) {
            this.name = name;
            this.gebaeude = gebauede;
        }
    }

    private static class Bauprojekt{
        public Bauprojekt(Building building, Cargo basecargo) {
            this.building = building;
            baubar = false;
            baukosten =  building.getBuildCosts().compare(basecargo, false, true);
        }
        public Building getBuilding() {
            return building;
        }
        public void setBuilding(Building building) {
            this.building = building;
        }
        public boolean isBaubar() {
            return baubar;
        }
        public void setBaubar(boolean baubar) {
            this.baubar = baubar;
        }
        public ResourceList getBaukosten() {
            return baukosten;
        }
        public void setBaukosten(ResourceList baukosten) {
            this.baukosten = baukosten;
        }

        public Building building;
        public boolean baubar;
        public ResourceList baukosten;
    }
    
    private static class Error{
      public String text;

      public Error(String text){
        this.text = text;
      }
    }

    private static class Tile{
        public boolean overlay;
        public String overlayImage;
        public boolean building;
        public String buildingName;
        public int buildingId;
        public boolean buildingJson;
        public int field;
        public String buildingImage;
        public int id;

        public boolean isOverlay() {
            return overlay;
        }
        public void setOverlay(boolean overlay) {
            this.overlay = overlay;
        }
        public String getOverlayImage() {
            return overlayImage;
        }
        public void setOverlayImage(String overlayImage) {
            this.overlayImage = overlayImage;
        }
        public boolean isBuilding() {
            return building;
        }
        public void setBuilding(boolean building) {
            this.building = building;
        }
        public String getBuildingName() {
            return buildingName;
        }
        public void setBuildingName(String buildingName) {
            this.buildingName = buildingName;
        }
        public int getBuildingId() {
            return buildingId;
        }
        public void setBuildingId(int buildingId) {
            this.buildingId = buildingId;
        }
        public boolean isBuildingJson() {
            return buildingJson;
        }
        public void setBuildingJson(boolean buildingJson) {
            this.buildingJson = buildingJson;
        }
        public int getField() {
            return field;
        }
        public void setField(int field) {
            this.field = field;
        }
        public String getBuildingImage() {
            return buildingImage;
        }
        public void setBuildingImage(String buildingImage) {
            this.buildingImage = buildingImage;
        }
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }

    }

    private static class BuildingGroup{
        public final Building building;
        public boolean allowon = false;
        public boolean allowoff = false;
        public final int id;

        public BuildingGroup(Building building, int id, boolean allowoff, boolean allowon) {
            this.building = building;
            this.allowon = allowon;
            this.allowoff = allowoff;
            this.id = id;
        }
        public boolean isAllowon() {
            return allowon;
        }
        public boolean isAllowoff() {
            return allowoff;
        }

    }

    private static class Bevoelkerung{
        public final long arbeiter;
        public final long arbeitslos;
        public final long wohnraumFrei;
        public final long wohnraumFehlt;
        public Bevoelkerung(Base base, BaseStatus basedata) {
            this.arbeiter = basedata.getArbeiter();
            this.arbeitslos = Math.max(base.getBewohner()-basedata.getArbeiter(),0);
            this.wohnraumFrei = Math.max(basedata.getLivingSpace()-base.getBewohner(),0);
            this.wohnraumFehlt = Math.max(base.getBewohner()-basedata.getLivingSpace(),0);
        }
    }

    private static class Activate{
        public final String name;
        public final boolean success;

        private Activate(String name, boolean success) {
            this.name = name;
            this.success = success;
        }
    }

    public enum ItemTyp{
        ROHSTOFF("Waren"),
        MODUL("Module"),
        MUNITION("Munition"),
        SONSTIGES("Sonstiges");
        private final String name;
        ItemTyp(String name) {
            this.name = name;
        }
        /**
         * Gibt den umgangssprachlichen Namen der des ItemTyps zurueck.
         * @return der Name
         */
        public String getName() {
            return name;
        }
    }

    /**
     * konvertiert einen ItemEffect in einen ItemTyp fuer die Kategorisierung in der GUI
     * gibt also quasi die Oberkategorie zurueck
     * @param type der ItemEffect.Type
     * @return der GUI-ItemTyp
     */
    public ItemTyp convertItemEffectType2ItemTyp(ItemEffect.Type type){
        switch (type){
            case NONE:
                return ItemTyp.ROHSTOFF;
            case MODULE:
            case MODULE_SET_META:
                return ItemTyp.MODUL;
            case AMMO:
                return ItemTyp.MUNITION;
            default:
                return ItemTyp.SONSTIGES;
        }
    }
}
