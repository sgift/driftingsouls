package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.ResourceLimit;
import net.driftingsouls.ds2.server.entities.ResourceLimit.ResourceLimitKey;
import net.driftingsouls.ds2.server.entities.SellLimit;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.SchiffEinstellungen;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.TradepostVisibility;
import org.hibernate.Session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Erlaubt die Einstellungen fuer Handelsposten.
 */
@Module(name = "tradepost")
public class TradepostController extends TemplateController
{
	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public TradepostController(Context context)
	{
		super(context);

		setPageTitle("Tradepost");
	}

	private void validiereSchiff(Ship ship)
	{
		// get variables
		if (ship == null)
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht");
		}

		// security check
		if (!getUser().equals(ship.getOwner()))
		{
			throw new ValidierungException("Allgemeine Richtlinienverletzung.");
		}
	}

	/**
	 * shows configuration site for a single tradepost.
	 *
	 * @param ship the ship-id
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(Ship ship)
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		validiereSchiff(ship);

		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

		t.setBlock("_TRADEPOST", "tradepost.list", "tradepost.post");
		t.setBlock("_TRADEPOST", "tradepost.items.entry", "tradepost.items.list");

		// check is ship is tradepost
		if (ship.isTradepost())
		{
			// hey fine, we have a tradepost here
			t.setVar("ship.tradepost", 1);
		}
		else
		{
			// not a tradepost, sent message to user and stop script
			t.setVar("tradepost.message", "Dieses Schiff ist kein Handelsposten. Bitte bestellen Sie die entsprechende Software beim Handelsunternehmen Ihres vertrauens");
			return;
		}

		t.setVar("ship.owner.isnpc", user.isNPC());

		// generate Maps which contain the SellLimits and den ResourceLimits of the Tradepost
		Map<Integer, SellLimit> selllistmap = new LinkedHashMap<>();
		Map<Integer, ResourceLimit> buylistmap = new LinkedHashMap<>();
		GtuWarenKurse kurse = ladeAnUndVerkaufsdaten(ship, selllistmap, buylistmap);
		Cargo buylistgtu = kurse.getKurse();


		t.setVar("tradepost.id", ship.getId(),
				"tradepost.image", ship.getTypeData().getPicture(),
				"tradepost.name", ship.getName(),
				"tradepost.koords", new Location(ship.getSystem(), ship.getX(), ship.getY()).displayCoordinates(false));

		// build form
		for (Item aitem : itemlist)
		{
			itemAnzeigen(t, user, selllistmap, buylistmap, buylistgtu, aitem);
		}

		t.setBlock("_TRADEPOST", "tradepostvisibility.list", "tradepostvisibility.post");
		// now we cycle through the possible values and insert them into the template
		for (TradepostVisibility visibility : TradepostVisibility.values())
		{
			t.setVar("tradepostvisibility.id", visibility.name(),
					"tradepostvisibility.descripton", visibility.getLabel(),
					"tradepostvisibility.selected", (ship.getEinstellungen().getShowtradepost() == visibility));
			t.parse("tradepostvisibility.post", "tradepostvisibility.list", true);
		}

	}

	private void itemAnzeigen(TemplateEngine t, User user, Map<Integer, SellLimit> selllistmap, Map<Integer, ResourceLimit> buylistmap, Cargo buylistgtu, Item aitem)
	{
		int itemid = aitem.getID();
		ItemID itemidobejct = new ItemID(aitem.getID());

		// check if user is allowed to see the item and go to next item if not
		if (!user.canSeeItem(aitem))
		{
			return;
		}

		// initiate starting values
		long salesprice = 0;
		double buyprice = 0;
		long saleslimit = 0;
		long buylimit = 0;
		int salesrank = 0;
		int buyrank = 0;

		// read actual values of limits
		// check if the List of items to sell contains current item
		if (selllistmap.containsKey(itemid * -1))
		{
			salesprice = selllistmap.get(itemid * -1).getPrice();
			saleslimit = selllistmap.get(itemid * -1).getLimit();
			salesrank = selllistmap.get(itemid * -1).getMinRank();
		}
		// check if the List of items to buy contains current item
		if (buylistmap.containsKey(itemid * -1))
		{
			buylimit = buylistmap.get(itemid * -1).getLimit();
			buyprice = buylistgtu.getResourceCount(itemidobejct) / 1000d;
			buyrank = buylistmap.get(itemid * -1).getMinRank();
		}

		// hier wollte ich einen intelligenten kommentar einfuegen
		String name = Common._plaintitle(aitem.getName());
		if (aitem.getQuality().color().length() > 0)
		{
			name = "<span style=\"color:" + aitem.getQuality().color() + "\">" + name + "</span>";
		}

		t.setVar("item.picture", aitem.getPicture(),
				"item.id", itemid,
				"item.name", name,
				"item.cargo", Common.ln(aitem.getCargo()),
				"item.paramid", "i" + aitem.getID());

		if (selllistmap.containsKey(itemid * -1) || buylistmap.containsKey(itemid * -1))
		{
			t.setVar("item.salesprice", Common.ln(salesprice),
					"item.buyprice", Common.ln(buyprice),
					"item.saleslimit", saleslimit,
					"item.buylimit", buylimit,
					"item.sellrank", salesrank,
					"item.buyrank", buyrank,
					"item.salebool", selllistmap.containsKey(itemid * -1),
					"item.buybool", buylistmap.containsKey(itemid * -1));

			t.parse("tradepost.post", "tradepost.list", true);
		}
		else
		{
			t.parse("tradepost.items.list", "tradepost.items.entry", true);
		}
	}

	private GtuWarenKurse ladeAnUndVerkaufsdaten(Ship ship, Map<Integer, SellLimit> selllistmap, Map<Integer, ResourceLimit> buylistmap)
	{
		Session db = getDB();

		// get all SellLimits of this ship
		List<SellLimit> selllimitlist = Common.cast(db.createQuery("from SellLimit where shipid=:shipid").setParameter("shipid", ship.getId()).list());

		// get all ResourceLimits of this ship
		List<ResourceLimit> buylimitlist = Common.cast(db.createQuery("from ResourceLimit where shipid=:shipid").setParameter("shipid", ship.getId()).list());
		// get GtuWarenKurse cause of fucking database structure
		GtuWarenKurse kurse = ermittleKurseFuerSchiff(ship);

		for (SellLimit limit : selllimitlist)
		{
			// add a sepcific selllimit to the map at position of his id
			selllistmap.put(limit.getId().getResourceId(), limit);
		}
		for (ResourceLimit limit : buylimitlist)
		{
			// add a specific buylimit to the map at position of his id
			buylistmap.put(limit.getId().getResourceId(), limit);
		}
		return kurse;
	}

	private GtuWarenKurse ermittleKurseFuerSchiff(Ship ship)
	{
		User user = (User) getUser();
		Session db = getDB();

		Cargo buylistgtu;
		GtuWarenKurse kurse = (GtuWarenKurse) db.get(GtuWarenKurse.class, "p" + ship.getId());
		if (kurse == null)
		{
			if (user.getRace() == Faction.GTU_RASSE)
			{
				GtuWarenKurse tmpKurse = (GtuWarenKurse) db.get(GtuWarenKurse.class, "tradepost");
				buylistgtu = new Cargo(tmpKurse.getKurse());
			}
			else
			{
				// there's no cargo, create one bastard
				buylistgtu = new Cargo();
			}

			// there's no GtuWarenKurse Object, create one
			kurse = new GtuWarenKurse("p" + ship.getId(), ship.getName(), buylistgtu);
			db.persist(kurse);

			return kurse;
		}
		else
		{
			return kurse;
		}
	}

	/**
	 * shows configuration site for a single tradepost.
	 *
	 * @param ship the ship-id
	 * @param tradepostvisibility Die Sichtbarkeit des Handelspostens
	 */
	@Action(ActionType.DEFAULT)
	public void updateAction(Ship ship,
			TradepostVisibility tradepostvisibility,
			@UrlParam(name="i#salesprice") Map<Integer,Long> salesprice,
			@UrlParam(name="i#buyprice") Map<Integer,Double> buyprice,
			@UrlParam(name="i#saleslimit") Map<Integer,Long> saleslimit,
			@UrlParam(name="i#buylimit") Map<Integer,Long> buylimit,
			@UrlParam(name="i#sellrank") Map<Integer,Integer> sellrank,
			@UrlParam(name="i#buyrank") Map<Integer,Integer> buyrank,
			@UrlParam(name="i#salebool") Map<Integer,Boolean> salebool,
			@UrlParam(name="i#buybool") Map<Integer,Boolean> buybool,
			@UrlParam(name="i#fill") Map<Integer,Boolean> fill)
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		validiereSchiff(ship);

		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

		t.setBlock("_TRADEPOST", "tradepost.list", "tradepost.post");


		// check is ship is tradepost
		if (ship.isTradepost())
		{
			// hey fine, we have a tradepost here
			t.setVar("ship.tradepost", 1);
		}
		else
		{
			// not a tradepost, sent message to user and stop script
			t.setVar("tradepost.message", "Dieses Schiff ist kein Handelsposten. Bitte bestellen Sie die entsprechende Software beim Handelsunternehmen Ihres vertrauens");
			return;
		}
		// generate Maps which contain the SellLimits and den ResourceLimits of the Tradepost
		Map<Integer, SellLimit> selllistmap = new LinkedHashMap<>();
		Map<Integer, ResourceLimit> buylistmap = new LinkedHashMap<>();

		GtuWarenKurse kurse = ladeAnUndVerkaufsdaten(ship, selllistmap, buylistmap);

		t.setVar("tradepost.id", ship.getId(),
				"tradepost.image", ship.getTypeData().getPicture(),
				"tradepost.name", ship.getName(),
				"tradepost.koords", new Location(ship.getSystem(), ship.getX(), ship.getY()).displayCoordinates(false));

		// read possible new value of tradepostvisibility and write to ship
		SchiffEinstellungen einstellungen = ship.getEinstellungen();
		einstellungen.setShowtradepost(tradepostvisibility);
		einstellungen.persistIfNecessary(ship);

		// build form
		for (Item aitem : itemlist)
		{
			if (!user.canSeeItem(aitem))
			{
				continue;
			}
			processItem(ship, kurse, selllistmap, buylistmap, aitem, salesprice, buyprice, saleslimit, buylimit, sellrank, buyrank, salebool, buybool, fill);
		}
	}

	private void processItem(Ship ship, GtuWarenKurse kurse, Map<Integer, SellLimit> selllistmap, Map<Integer, ResourceLimit> buylistmap, Item aitem,
							 Map<Integer,Long> salesprices,
							 Map<Integer,Double> buyprices,
							 Map<Integer,Long> saleslimits,
							 Map<Integer,Long> buylimits,
							 Map<Integer,Integer> sellranks,
							 Map<Integer,Integer> buyranks,
							 Map<Integer,Boolean> salebools,
							 Map<Integer,Boolean> buybools,
							 Map<Integer,Boolean> fills)
	{
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		int itemid = aitem.getID();
		final ItemID rid = new ItemID(aitem.getID());

		ResourceLimitKey resourcekey = new ResourceLimitKey(ship, rid);

		// check if user is allowed to see the item and go to next item if not
		if (!user.canSeeItem(aitem))
		{
			return;
		}

		if( !salesprices.containsKey(aitem.getID()) )
		{
			return;
		}

		// read new values for item
		long salesprice = salesprices.get(aitem.getID());
		double buyprice = buyprices.get(aitem.getID());
		long saleslimit = saleslimits.get(aitem.getID());
		long buylimit = buylimits.get(aitem.getID());
		Integer sellrank = sellranks.get(aitem.getID());
		Integer buyrank = buyranks.get(aitem.getID());
		boolean salebool = salebools.containsKey(aitem.getID()) && salebools.get(aitem.getID());
		boolean buybool = buybools.containsKey(aitem.getID()) && buybools.get(aitem.getID());
		boolean fill = fills.containsKey(aitem.getID()) && fills.get(aitem.getID());

		// check if values are positive and set to zero if not
		if (salesprice < 0)
		{
			salesprice = 0;
		}
		if (buyprice < 0)
		{
			buyprice = 0;
		}
		if (saleslimit < 0)
		{
			saleslimit = 0;
		}
		if (buylimit < 0)
		{
			buylimit = 0;
		}

		if (sellrank == null || sellrank < 0 || !ship.getOwner().isNPC())
		{
			sellrank = 0;
		}

		if (buyrank == null || buyrank < 0 || !ship.getOwner().isNPC())
		{
			buyrank = 0;
		}

		if (!ship.getOwner().isNPC())
		{
			fill = false;
		}

		SellLimit itemsell;
		ResourceLimit itembuy;
		// check if we dont want to sell the resource any more
		if (!salebool || salesprice <= 0)
		{
			if (selllistmap.containsKey(itemid * -1))
			{
				itemsell = selllistmap.get(itemid * -1);
				db.delete(itemsell);
			}
		}
		else
		{
			// check if the List of items to sell contains current item
			if (selllistmap.containsKey(itemid * -1))
			{
				itemsell = selllistmap.get(itemid * -1);
				itemsell.setPrice(salesprice);
				itemsell.setLimit(saleslimit);
				itemsell.setMinRank(sellrank);
			}
			else
			{
				// create new object
				itemsell = new SellLimit(resourcekey, salesprice, saleslimit, sellrank);
				db.persist(itemsell);
			}

			if (fill)
			{
				Cargo cargo = ship.getCargo();
				long cnt = cargo.getResourceCount(rid);
				if (cnt < saleslimit)
				{
					cargo.setResource(rid, saleslimit);
					ship.setCargo(cargo);
				}
			}
		}

		// check if we dont want to buy the resource any more
		if (!buybool || buyprice <= 0)
		{
			if (buylistmap.containsKey(itemid * -1))
			{
				itembuy = buylistmap.get(itemid * -1);
				db.delete(itembuy);
			}
		}
		else
		{
			// check if the List of items to buy contains current item
			if (buylistmap.containsKey(itemid * -1))
			{
				itembuy = buylistmap.get(itemid * -1);

				itembuy.setLimit(buylimit);
				itembuy.setMinRank(buyrank);
			}
			else
			{
				// create new object
				itembuy = new ResourceLimit(resourcekey, buylimit, buyrank);
				db.persist(itembuy);
			}
			Cargo kcargo = kurse.getKurse();
			kcargo.setResource(rid, Math.round(buyprice * 1000));
			kurse.setKurse(kcargo);
		}
		t.setVar("tradepost.update", "1",
				"tradepost.update.message", "Die Einstellungen wurden &uuml;bernommen.",
				"tradepost.id", ship.getId());

		t.parse("tradepost.post", "tradepost.list", true);
	}
}
