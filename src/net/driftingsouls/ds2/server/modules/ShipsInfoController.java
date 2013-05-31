package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.*;

/**
 * Outputs basic information on ships.
 */
@Configurable
@Module(name = "shipsinfo")
public class ShipsInfoController extends TemplateGenerator
{
    public ShipsInfoController(Context context)
    {
        super(context);
        setTemplate("shipsinfo.html");
        setPageTitle("Schiffsliste");
    }

    @Override
    protected boolean validateAndPrepare(String action)
    {
        return true;
    }

    @Override
    @Action(ActionType.DEFAULT)
    public void defaultAction()
    {
        TemplateEngine t = getTemplateEngine();
        User user = (User) ContextMap.getContext().getActiveUser();
        org.hibernate.Session db = getDB();

        List<ShipType> ships = Common.cast(db.createQuery("from ShipType where hide=:hide").setParameter("hide", false).list());
        if(ships.size() == 0)
        {
            return;
        }

        List<ShipBaubar> buildableShipList = db.createQuery("from ShipBaubar").list();
        Map<Integer, ShipBaubar> buildableShips = new HashMap<Integer, ShipBaubar>();
        for(ShipBaubar buildable: buildableShipList)
        {
            buildableShips.put(buildable.getType().getId(), buildable);
        }

        Map<BuildKind, Set<ShipType>> sortedShipTypes = sortTypes(db, user, buildableShips, ships);
        writeList(t, buildableShips, sortedShipTypes.get(BuildKind.ASTEROID), "_SHIPSINFO", "asteroidinfo.shiplist.list", "asteroidinfo.shiplist.listitem", "ship.buildcosts.asteroid.list");
        writeList(t, buildableShips, sortedShipTypes.get(BuildKind.GANYMED), "_SHIPSINFO", "ganymedinfo.shiplist.list", "ganymedinfo.shiplist.listitem", "ship.buildcosts.ganymed.list");
        writeList(t, buildableShips, sortedShipTypes.get(BuildKind.NOWHERE), "_SHIPSINFO", "restinfo.shiplist.list", "restinfo.shiplist.listitem", null);
    }
    
    private void writeList(TemplateEngine t, Map<Integer, ShipBaubar> buildableShips, Set<ShipType> shipTypes, String blockName, String listName, String itemName, String reslistName)
    {
        t.setBlock(blockName, itemName, listName);
        if(reslistName != null)
        {
            t.setBlock(blockName, reslistName + "item", reslistName);
        }

        for(ShipType type: shipTypes)
        {
            t.setVar(   "ship.id",      type.getTypeId(),
                        "ship.name",    type.getNickname(),
                        "ship.picture", type.getPicture());

            ShipBaubar buildData = buildableShips.get(type.getId());
            if(buildData != null && reslistName != null)
            {
                ResourceList resources = buildData.getCosts().getResourceList();
                Resources.echoResList(t, resources, reslistName);
                t.parse(reslistName, reslistName+"item", true);
            }

            t.parse(listName, itemName, true);
        }
    }
    
    private Map<BuildKind, Set<ShipType>> sortTypes(org.hibernate.Session db, User user, Map<Integer, ShipBaubar> buildableShips, List<ShipType> shipTypes)
    {
        BaseWerft asteroidYard = new BaseWerft();
        int slotsOnAsteroids = asteroidYard.getWerftSlots();

        Map<BuildKind, Set<ShipType>> sortedShipTypes = new HashMap<BuildKind, Set<ShipType>>();
        sortedShipTypes.put(BuildKind.ASTEROID, new TreeSet<ShipType>(sortTypeByName));
        sortedShipTypes.put(BuildKind.GANYMED, new TreeSet<ShipType>(sortTypeByName));
        sortedShipTypes.put(BuildKind.NOWHERE, new TreeSet<ShipType>(sortTypeByName));

        for(ShipType shipType: shipTypes)
        {
            ShipBaubar buildData = buildableShips.get(shipType.getId());
            if(buildData != null)
            {
                boolean researched = user.hasResearched(buildData.getRes(1)) && user.hasResearched(buildData.getRes(2)) && user.hasResearched(buildData.getRes(3));
                if(researched)
                {
                    int slotsNeeded = buildData.getWerftSlots();
                    if(slotsNeeded > slotsOnAsteroids)
                    {
                        sortedShipTypes.get(BuildKind.GANYMED).add(shipType);
                    }
                    else
                    {
                        sortedShipTypes.get(BuildKind.ASTEROID).add(shipType);
                    }
                }
                else
                {
                    sortedShipTypes.get(BuildKind.NOWHERE).add(shipType);
                }
            }
            else
            {
                sortedShipTypes.get(BuildKind.NOWHERE).add(shipType);
            }
        }

        return sortedShipTypes;
    }
                                          
    private enum BuildKind
    {
        ASTEROID, GANYMED, NOWHERE
    }

    private class NameSorter implements Comparator<ShipType>
    {

        @Override
        public int compare(ShipType shipType, ShipType shipType1) 
        {
            return shipType.getNickname().compareTo(shipType1.getNickname());
        }
    }

    private NameSorter sortTypeByName = new NameSorter();
}


