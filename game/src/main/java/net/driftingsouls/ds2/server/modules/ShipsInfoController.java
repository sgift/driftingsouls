package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Outputs basic information on ships.
 */
@Configurable
@Module(name = "shipsinfo")
public class ShipsInfoController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public ShipsInfoController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		setPageTitle("Schiffsliste");
	}

	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		User user = (User) ContextMap.getContext().getActiveUser();
		org.hibernate.Session db = getDB();

		List<ShipType> ships = Common.cast(db.createQuery("from ShipType where hide=:hide").setParameter("hide", false).list());
		if (ships.size() == 0)
		{
			return t;
		}

		List<ShipBaubar> buildableShipList = Common.cast(db.createCriteria(ShipBaubar.class).list());
		Map<Integer, ShipBaubar> buildableShips = new HashMap<>();
		for (ShipBaubar buildable : buildableShipList)
		{
			buildableShips.put(buildable.getType().getId(), buildable);
		}

		Map<BuildKind, Set<ShipType>> sortedShipTypes = sortTypes(user, buildableShips, ships);
		writeList(t, buildableShips, sortedShipTypes.get(BuildKind.ASTEROID), "_SHIPSINFO", "asteroidinfo.shiplist.list", "asteroidinfo.shiplist.listitem", "ship.buildcosts.asteroid.list");
		writeList(t, buildableShips, sortedShipTypes.get(BuildKind.GANYMED), "_SHIPSINFO", "ganymedinfo.shiplist.list", "ganymedinfo.shiplist.listitem", "ship.buildcosts.ganymed.list");
		writeList(t, buildableShips, sortedShipTypes.get(BuildKind.LACKING_RESEARCH), "_SHIPSINFO", "researchableinfo.shiplist.list", "researchableinfo.shiplist.listitem", "ship.buildcosts.researchable.list");
		writeList(t, buildableShips, sortedShipTypes.get(BuildKind.OTHER_SPECIES), "_SHIPSINFO", "otherinfo.shiplist.list", "otherinfo.shiplist.listitem", "ship.buildcosts.others.list");
		writeList(t, buildableShips, sortedShipTypes.get(BuildKind.NOWHERE), "_SHIPSINFO", "restinfo.shiplist.list", "restinfo.shiplist.listitem", null);

		return t;
	}

	private void writeList(TemplateEngine t, Map<Integer, ShipBaubar> buildableShips, Set<ShipType> shipTypes, String blockName, String listName, String itemName, String reslistName)
	{
		t.setBlock(blockName, itemName, listName);
		if (reslistName != null)
		{
			t.setBlock(blockName, reslistName + "item", reslistName);
		}

		for (ShipType type : shipTypes)
		{
			t.setVar("ship.id", type.getTypeId(),
					"ship.name", type.getNickname(),
					"ship.picture", type.getPicture());

			ShipBaubar buildData = buildableShips.get(type.getId());
			if (buildData != null && reslistName != null)
			{
				ResourceList resources = buildData.getCosts().getResourceList();
				Resources.echoResList(t, resources, reslistName);
			}

			t.parse(listName, itemName, true);
		}
	}

	private Map<BuildKind, Set<ShipType>> sortTypes(User user, Map<Integer, ShipBaubar> buildableShips, List<ShipType> shipTypes)
	{
		int slotsOnAsteroids = berechneMaxWerftSlotsAufBasen(user);

		Map<BuildKind, Set<ShipType>> sortedShipTypes = new HashMap<>();
		sortedShipTypes.put(BuildKind.ASTEROID, new TreeSet<>(sortTypeByName));
		sortedShipTypes.put(BuildKind.GANYMED, new TreeSet<>(sortTypeByName));
		sortedShipTypes.put(BuildKind.LACKING_RESEARCH, new TreeSet<>(sortTypeByName));
		sortedShipTypes.put(BuildKind.OTHER_SPECIES, new TreeSet<>(sortTypeByName));
		sortedShipTypes.put(BuildKind.NOWHERE, new TreeSet<>(sortTypeByName));

		for (ShipType shipType : shipTypes)
		{
			ShipBaubar buildData = buildableShips.get(shipType.getId());
			if (buildData != null)
			{
				if (Rassen.get().rasse(user.getRace()).isMemberIn(buildData.getRace()))
				{
					boolean researched = user.hasResearched(buildData.getBenoetigteForschungen());
					if (researched)
					{
						int slotsNeeded = buildData.getWerftSlots();
						if (slotsNeeded > slotsOnAsteroids)
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
						boolean visible = buildData.getBenoetigteForschungen().stream().allMatch((f) -> f.isVisibile(user));
						if (visible)
						{
							sortedShipTypes.get(BuildKind.LACKING_RESEARCH).add(shipType);
						}
						else
						{
							sortedShipTypes.get(BuildKind.NOWHERE).add(shipType);
						}
					}
				}
				else
				{
					sortedShipTypes.get(BuildKind.OTHER_SPECIES).add(shipType);
				}
			}
			else
			{
				sortedShipTypes.get(BuildKind.NOWHERE).add(shipType);
			}
		}

		return sortedShipTypes;
	}

	private int berechneMaxWerftSlotsAufBasen(User user)
	{
		int slotsOnAsteroids = 0;
		for (Base base : user.getBases())
		{
			if( base.getWerft() != null && base.getWerft().getWerftSlots() > slotsOnAsteroids )
			{
				slotsOnAsteroids = base.getWerft().getWerftSlots();
			}
		}
		return slotsOnAsteroids;
	}

	private enum BuildKind
	{
		ASTEROID, GANYMED, LACKING_RESEARCH, OTHER_SPECIES, NOWHERE
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


