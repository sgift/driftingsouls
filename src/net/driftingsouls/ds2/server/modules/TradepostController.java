package net.driftingsouls.ds2.server.modules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.ResourceLimit;
import net.driftingsouls.ds2.server.entities.ResourceLimit.ResourceLimitKey;
import net.driftingsouls.ds2.server.entities.SellLimit;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.SchiffEinstellungen;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.TradepostVisibility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Erlaubt die Einstellungen fuer Handelsposten.
 *
 */
@Configurable
@Module(name="tradepost")
public class TradepostController extends TemplateGenerator {
	@SuppressWarnings("unused")
	private Configuration config;	// never read, but we'll need it for later integreation of pictures i guess
	private Ship ship = null;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public TradepostController(Context context) {
		super(context);

		setTemplate("tradepost.html");

		setPageTitle("Tradepost");
	}

    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config)
    {
    	this.config = config;
    }

	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}

	/**
	 * shows configuration site for a single tradepost.
	 * @urlparam String ship the ship-id
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

		t.setBlock("_TRADEPOST", "tradepost.list", "tradepost.post");
		t.setBlock("_TRADEPOST", "tradepost.items.entry", "tradepost.items.list");

		// get variables
		parameterNumber("ship");
		int shipid = getInteger("ship");
		Cargo buylistgtu = null;
		ship = (Ship)db.get(Ship.class, shipid);	// the tradepost

		if(ship == null)
		{
			addError("Das angegebene Schiff existiert nicht");
			return;
		}

		// security check
		if(!getUser().equals(ship.getOwner()))
		{
			addError("Allgemeine Richtlinienverletzung.");
			return;
		}

		// check is ship is tradepost
		if(ship.isTradepost())
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

		// get all SellLimits of this ship
		List<SellLimit> selllimitlist = Common.cast(db.createQuery("from SellLimit where shipid=:shipid").setParameter("shipid", shipid).list());

		// get all ResourceLimits of this ship
		List<ResourceLimit> buylimitlist = Common.cast(db.createQuery("from ResourceLimit where shipid=:shipid").setParameter("shipid", shipid).list());
		// get GtuWarenKurse cause of fucking database structure
		GtuWarenKurse kurse = (GtuWarenKurse)db.get(GtuWarenKurse.class, "p"+shipid);
		if(kurse == null)
		{
			// there's no cargo, create one bastard
			buylistgtu = new Cargo();
			// there's no GtuWarenKurse Object, create one
			kurse = new GtuWarenKurse("p"+shipid, ship.getName(), buylistgtu);
			db.persist(kurse);
		}
		else
		{
			buylistgtu = kurse.getKurse();
		}

		// generate Maps which contain the SellLimits and den ResourceLimits of the Tradepost
		Map<Integer,SellLimit> selllistmap = new LinkedHashMap<Integer,SellLimit>();
		Map<Integer,ResourceLimit> buylistmap = new LinkedHashMap<Integer,ResourceLimit>();
		for(SellLimit limit: selllimitlist)
		{
			// add a sepcific selllimit to the map at position of his id
			selllistmap.put(limit.getId().getResourceId(), limit);
		}
		for(ResourceLimit limit: buylimitlist)
		{
			// add a specific buylimit to the map at position of his id
			buylistmap.put(limit.getId().getResourceId(), limit);
		}

		t.setVar(	"tradepost.id",	shipid,
					"tradepost.image", ship.getTypeData().getPicture(),
					"tradepost.name", ship.getName(),
					"tradepost.koords", new Location(ship.getSystem(), ship.getX(), ship.getY()).displayCoordinates(false) );

		// build form
		for( Item aitem : itemlist ) {
			int itemid = aitem.getID();
			ItemID itemidobejct = new ItemID(aitem.getID());

			// check if user is allowed to see the item and go to next item if not
			if( !user.canSeeItem(aitem))
			{
				continue;
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
			if(selllistmap.containsKey(itemid * -1))
			{
				salesprice = selllistmap.get(itemid * -1).getPrice();
				saleslimit = selllistmap.get(itemid * -1).getLimit();
                salesrank = selllistmap.get(itemid * -1).getMinRank();
			}
			// check if the List of items to buy contains current item
			if(buylistmap.containsKey(itemid * -1))
			{
				buylimit = buylistmap.get(itemid * -1).getLimit();
				buyprice = buylistgtu.getResourceCount(itemidobejct) / 1000d;
                buyrank = buylistmap.get(itemid * -1).getMinRank();
			}

			// hier wollte ich einen intelligenten kommentar einfuegen
			String name = Common._plaintitle(aitem.getName());
			if( aitem.getQuality().color().length() > 0 ) {
				name = "<span style=\"color:"+aitem.getQuality().color()+"\">"+name+"</span>";
			}

			t.setVar(	"item.picture",	aitem.getPicture(),
					"item.id",		itemid,
					"item.name",	name,
					"item.cargo",	Common.ln(aitem.getCargo()),
					"item.paramid", "i"+aitem.getID());

			if( selllistmap.containsKey(itemid*-1) || buylistmap.containsKey(itemid*-1) )
			{
				t.setVar(	"item.salesprice",	Common.ln(salesprice),
							"item.buyprice",	Common.ln(buyprice),
							"item.saleslimit",	saleslimit,
							"item.buylimit",	buylimit,
	                        "item.sellrank", salesrank,
	                        "item.buyrank", buyrank,
							"item.salebool",	selllistmap.containsKey(itemid * -1),
							"item.buybool",		buylistmap.containsKey(itemid * -1) );

				t.parse("tradepost.post", "tradepost.list", true);
			}
			else
			{
				t.parse("tradepost.items.list", "tradepost.items.entry", true);
			}
		}

		t.setBlock("_TRADEPOST", "tradepostvisibility.list", "tradepostvisibility.post");
		// now we cycle through the possible values and insert them into the template
		for( TradepostVisibility visibility : TradepostVisibility.values() )
		{
			t.setVar("tradepostvisibility.id", visibility.name(),
					"tradepostvisibility.descripton", visibility.getLabel(),
					"tradepostvisibility.selected", (ship.getEinstellungen().getShowtradepost() == visibility));
			t.parse("tradepostvisibility.post", "tradepostvisibility.list", true);
		}

	}

	/**
	 * shows configuration site for a single tradepost.
	 * @urlparam String ship the ship-id
	 */
	@Action(ActionType.DEFAULT)
	public void updateAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

		t.setBlock("_TRADEPOST", "tradepost.list", "tradepost.post");

		// get variables
		parameterNumber("ship");
		int shipid = getInteger("ship");
		Cargo buylistgtu = null;
		ship = (Ship)db.get(Ship.class, shipid);	// the tradepost

		if(ship == null)
		{
			addError("Das angegebene Schiff existiert nicht");
			return;
		}

		// security check
		if(!getUser().equals(ship.getOwner()))
		{
			addError("Allgemeine Richtlinienverletzung.");
			return;
		}

		// check is ship is tradepost
		if(ship.isTradepost())
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

		// get all SellLimits of this ship
		List<SellLimit> selllimitlist = Common.cast(db.createQuery("from SellLimit where shipid=:shipid").setParameter("shipid", shipid).list());

		// get all ResourceLimits of this ship
		List<ResourceLimit> buylimitlist = Common.cast(db.createQuery("from ResourceLimit where shipid=:shipid").setParameter("shipid", shipid).list());
		// get GtuWarenKurse cause of fucking database structure
		GtuWarenKurse kurse = (GtuWarenKurse)db.get(GtuWarenKurse.class, "p"+shipid);
		if(kurse == null)
		{
			// there's no cargo, create one bastard
			buylistgtu = new Cargo();
			// there's no GtuWarenKurse Object, create one
			kurse = new GtuWarenKurse("p"+shipid, ship.getName(), buylistgtu);
			db.persist(kurse);
		}
		else
		{
			buylistgtu = kurse.getKurse();
		}

		// generate Maps which contain the SellLimits and den ResourceLimits of the Tradepost
		Map<Integer,SellLimit> selllistmap = new LinkedHashMap<Integer,SellLimit>();
		Map<Integer,ResourceLimit> buylistmap = new LinkedHashMap<Integer,ResourceLimit>();
		for(SellLimit limit: selllimitlist)
		{
			// add a sepcific selllimit to the map at position of his id
			selllistmap.put(limit.getId().getResourceId(), limit);
		}
		for(ResourceLimit limit: buylimitlist)
		{
			// add a specific buylimit to the map at position of his id
			buylistmap.put(limit.getId().getResourceId(), limit);
		}

		t.setVar(	"tradepost.id",	shipid,
				"tradepost.image", ship.getTypeData().getPicture(),
				"tradepost.name", ship.getName(),
				"tradepost.koords", new Location(ship.getSystem(), ship.getX(), ship.getY()).displayCoordinates(false) );

		// read possible new value of tradepostvisibility and write to ship
		parameterString("tradepostvisibility");
		String tradepostvisibility = getString("tradepostvisibility");

		SchiffEinstellungen einstellungen = ship.getEinstellungen();
		einstellungen.setShowtradepost(TradepostVisibility.valueOf(tradepostvisibility));
		einstellungen.persistIfNecessary(this.ship);

		// build form
		for( Item aitem : itemlist ) {
			processItem(user, kurse, selllistmap, buylistmap, aitem);
		}
	}

	private void processItem(User user, GtuWarenKurse kurse, Map<Integer,SellLimit> selllistmap, Map<Integer,ResourceLimit> buylistmap, Item aitem)
	{
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		int itemid = aitem.getID();
		final ItemID rid = new ItemID(aitem.getID());

		ResourceLimitKey resourcekey = new ResourceLimitKey(ship, rid);

		// check if user is allowed to see the item and go to next item if not
		if( !user.canSeeItem(aitem))
		{
			return;
		}

		// read new values for item
		parameterNumber("i"+aitem.getID()+"salesprice");
		parameterNumber("i"+aitem.getID()+"buyprice");
		parameterNumber("i"+aitem.getID()+"saleslimit");
		parameterNumber("i"+aitem.getID()+"buylimit");
        parameterNumber("i"+aitem.getID()+"sellrank");
        parameterNumber("i"+aitem.getID()+"buyrank");

		long salesprice = getInteger("i"+aitem.getID()+"salesprice");
		double buyprice = getDouble("i"+aitem.getID()+"buyprice");
		long saleslimit = getInteger("i"+aitem.getID()+"saleslimit");
		long buylimit = getInteger("i"+aitem.getID()+"buylimit");
        int sellRank = getInteger("i"+aitem.getID()+"sellrank");
        int buyRank = getInteger("i"+aitem.getID()+"buyrank");

		final Request request = ContextMap.getContext().getRequest();
		boolean salebool = (request.getParameterInt("i"+aitem.getID()+"salebool") == 1 ? true : false);
		boolean buybool = (request.getParameterInt("i"+aitem.getID()+"buybool") == 1 ? true : false);
		boolean fill = (request.getParameterInt("i"+aitem.getID()+"fill") == 1 ? true : false);

		// check if values are positive and set to zero if not
		if(salesprice < 0)
		{
			salesprice = 0;
		}
		if(buyprice < 0)
		{
			buyprice = 0;
		}
		if(saleslimit < 0)
		{
			saleslimit = 0;
		}
		if(buylimit < 0)
		{
			buylimit = 0;
		}

        if(sellRank < 0 || !ship.getOwner().isNPC())
        {
            sellRank = 0;
        }

        if(buyRank < 0 || !ship.getOwner().isNPC() )
        {
            buyRank = 0;
        }

        if( !ship.getOwner().isNPC() )
        {
        	fill = false;
        }

		SellLimit itemsell = null;
		ResourceLimit itembuy = null;
		// check if we dont want to sell the resource any more
		if(!salebool)
		{
			if(selllistmap.containsKey(itemid * -1))
			{
				itemsell = selllistmap.get(itemid * -1);
				db.delete(itemsell);
			}
		}
		else
		{
			// check if the List of items to sell contains current item
			if(selllistmap.containsKey(itemid * -1))
			{
				itemsell = selllistmap.get(itemid * -1);
				itemsell.setPrice(salesprice);
				itemsell.setLimit(saleslimit);
                itemsell.setMinRank(sellRank);
			}
			else
			{
				// create new object
				itemsell = new SellLimit(resourcekey, salesprice, saleslimit, sellRank);
				db.persist(itemsell);
			}

			if( fill )
			{
				Cargo cargo = ship.getCargo();
				long cnt = cargo.getResourceCount(rid);
				final long mincount = Math.max(saleslimit*5,1000);
				if( cnt < mincount )
				{
					cargo.setResource(rid, mincount);
					ship.setCargo(cargo);
				}
			}
		}

		// check if we dont want to buy the resource any more
		if(!buybool)
		{
			if(buylistmap.containsKey(itemid * -1))
			{
				itembuy = buylistmap.get(itemid * -1);
				db.delete(itembuy);
			}
		}
		else
		{
			// check if the List of items to buy contains current item
			if(buylistmap.containsKey(itemid * -1))
			{
				itembuy = buylistmap.get(itemid * -1);

				itembuy.setLimit(buylimit);
                itembuy.setMinRank(buyRank);
			}
			else
			{
				// create new object
				itembuy = new ResourceLimit(resourcekey, buylimit, buyRank);
				db.persist(itembuy);
			}
			Cargo kcargo = kurse.getKurse();
			kcargo.setResource(rid , Math.round(buyprice * 1000));
			kurse.setKurse(kcargo);
		}
		t.setVar(	"tradepost.update", "1",
					"tradepost.update.message", "Die Einstellungen wurden &uuml;bernommen.",
					"tradepost.id",	ship.getId() );

		t.parse("tradepost.post", "tradepost.list", true);
	}
}
