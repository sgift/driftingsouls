package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.bases.*;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.units.BaseUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import org.hibernate.Session;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;
import java.util.stream.Collectors;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.framework.ConfigService;

/**
 * Die Basis - Alle Funktionalitaeten der Basis befinden sich in
 * dieser Klasse.
 *
 * @author Gregor Fuhs
 */
public class BaseController implements DSController{
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
        if(!validateBase(base, ctx)){
            templateEngine.process("base", ctx, response.getWriter());
            return;
        }

        switch(action){
          case CHANGEFEEDING:
            changeFeedingAction(ctx, request, base);
            defaultAction(ctx, request, base);
            break;
          case CHANGENAME:
            changeNameAction(ctx, request, base);
            defaultAction(ctx, request, base);
            break;
          case CHANGEBUILDINGSTATUS:
            changeBuildingStatusAction(ctx, request, base);
            defaultAction(ctx, request, base);
            break;
          case SCAN:
          Ship ship = null;
            try{
                ship = Ship.getShipById(Integer.parseInt(request.getParameter("ship")));
            }catch(Exception e){}
            scanAction(ctx, request, base, ship);
            break;
          case ACTIVATEALL:
            activateAllAction(ctx, request, base);
            defaultAction(ctx, request, base);
            break;
          default:
            defaultAction(ctx, request, base);
            break;
        }

        templateEngine.process("base", ctx, response.getWriter());
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
            buildingonoff = Integer.parseInt(request.getParameter("act"));
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
     * Aktion zur Anzeige der Basis
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param base die Basis
     * @URL-Parameter action: scan 
     * @URL-Parameter col Die ID der Basis
     * @URL-Parameter ship Die ID des scannenden Schiffs
     */
    public void scanAction(WebContext ctx, HttpServletRequest request, Base base, Ship ship){
        boolean scan = ship != null;
		int shipid = 0;
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
			shipid = ship.getId();
			int e = new ConfigService().getValue(WellKnownConfigValue.ASTI_SCAN_COST);
			ship.setEnergy(ship.getEnergy() - e);
		}

		User user = (User)context.getActiveUser();
		int mapheight = (1 + base.getHeight() * 2) * 22+25;
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
            if((i+1)%base.getWidth() == 0){
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

        List<BuildingView> buildings = new ArrayList<>();

		for( Map.Entry<Integer,Integer> entry : buildingonoffstatus.entrySet() ) {
			int bstatus = entry.getValue();

			Building building = Building.getBuilding(entry.getKey());
            BuildingView bv = new BuildingView(building,entry.getKey(),(bstatus == -1) || (bstatus == 2),(bstatus == -1) || (bstatus == 1)  );
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

		double summeWohnen = Math.max(base.getBewohner(),basedata.getLivingSpace());
		long arbeiterProzent = Math.round(basedata.getArbeiter()/summeWohnen*100);
		long arbeitslosProzent = Math.max(Math.round((base.getBewohner()-basedata.getArbeiter())/summeWohnen*100),0);
		long wohnraumFreiProzent = Math.max(Math.round((basedata.getLivingSpace()-base.getBewohner())/summeWohnen*100),0);
		long wohnraumFehltProzent = Math.max(Math.round((base.getBewohner()-basedata.getLivingSpace())/summeWohnen*100),0);
		long prozent = arbeiterProzent+arbeitslosProzent+wohnraumFehltProzent+wohnraumFreiProzent;
		if( prozent > 100 ) {
			long remaining = prozent-100;
			long diff = Math.min(remaining,arbeiterProzent);
			arbeiterProzent -= diff;
			remaining -= diff;
			if( remaining > 0 ) {
				arbeitslosProzent -= remaining;
			}
		}
        Bevoelkerung bevoelkerung = new Bevoelkerung(arbeiterProzent,arbeitslosProzent,wohnraumFreiProzent,wohnraumFehltProzent);

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

    @RequestMapping("/base/build")
    @ResponseBody
    public String getJSON( @RequestParam("buildingid") int buildingid, @RequestParam("field") int fieldid, @RequestParam("col") int baseid){
        return "test: buildingid="+buildingid+"; field="+fieldid+"; col="+baseid;
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
            deaconly = Boolean.parseBoolean(request.getParameter("deaconly"));
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

        //t.setBlock("_BUILD", "buildings.listitem", "buildings.list");
        //t.setBlock("buildings.listitem", "buildings.res.listitem", "buildings.res.list");

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

        public String name;

        public RessView(String name, List<ResourceEntry> ress) {
            this.name = name;
            this.ress = ress;
        }

        public List<ResourceEntry> ress;

        public int getAmount(){
            return ress.size();
        }

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

    private static class BuildingView{
        public final Building building;
        public boolean allowon = false;
        public boolean allowoff = false;
        public final int id;
        public BuildingView(Building building, int id, boolean allowoff, boolean allowon) {
            this.building = building;
            this.allowon = allowon;
            this.allowoff = allowoff;
            this.id = id;
        }

    }

    private static class Bevoelkerung{
        public final long arbeiterProzent;
		public final long arbeitslosProzent;
		public final long wohnraumFreiProzent;
		public final long wohnraumFehltProzent;
        public Bevoelkerung(long arbeiterProzent, long arbeitslosProzent, long wohnraumFreiProzent,
                long wohnraumFehltProzent) {
            this.arbeiterProzent = arbeiterProzent;
            this.arbeitslosProzent = arbeitslosProzent;
            this.wohnraumFreiProzent = wohnraumFreiProzent;
            this.wohnraumFehltProzent = wohnraumFehltProzent;
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
