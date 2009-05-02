package net.driftingsouls.ds2.server.modules;

import java.util.List;

import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.entities.ResourceLimit;
import net.driftingsouls.ds2.server.entities.SellLimit;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Erlaubt die Einstellungen fuer Handelsposten.
 *
 */
@Configurable
public class TradepostController extends TemplateGenerator {
	private Configuration config;
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
	 * shows configuration site for a single tradepost
	 * @urlparam String ship the ship-id
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();

		t.setBlock("_TRADEPOST", "tradepost.list", "tradepost.post");
		
		// get ship-id
		parameterString("ship");
		int shipid = getInteger("ship");
		ship = (Ship)db.get(Ship.class, shipid);
		
		// check is ship is tradepost
		if(ship.isTradepost())
		{
			t.setVar("ship.tradepost", 1);
		}
		else
		{
			t.setVar("tradepost.message", "Dieses Schiff ist kein Handelsposten. Bitte bestellen Sie die entsprechende Software beim Handelsunternehmen Ihres vertrauens");
			return;
		}
				
		// get all SellLimits of this ship
		List<SellLimit> selllimitlist = Common.cast(db.createQuery("from SellLimit where shipid=:shipid").setParameter("shipid", shipid).list());
		
		// get all ResourceLimits of this ship
		List<ResourceLimit> buylimitlist = Common.cast(db.createQuery("from ResourceLimit where shipid=:shipid").setParameter("shipid", shipid).list());
		
		// build form
		for( Item aitem : Items.get() ) {
			int itemid = aitem.getID();
			
			// check if user is allowed to see the item and go to next item if not
			if( !user.canSeeItem(aitem))
			{
				continue;
			}
			
			// read actual values of limits
			SellLimit itemsell = selllimitlist.get(itemid);
			ResourceLimit itembuy = buylimitlist.get(itemid);
			
			long salesprice = 0;
			long buyprice = 0;
			long saleslimit = 0;
			long buylimit = 0;
			
			// read new values for item
			parameterString("i"+aitem.getID()+"salesprice");
			parameterString("i"+aitem.getID()+"buyprice");
			parameterString("i"+aitem.getID()+"saleslimit");
			parameterString("i"+aitem.getID()+"buylimit");
			salesprice = getInteger("i"+aitem.getID()+"salesprice");
			buyprice = getInteger("i"+aitem.getID()+"buyprice");
			saleslimit = getInteger("i"+aitem.getID()+"saleslimit");
			buylimit = getInteger("i"+aitem.getID()+"buylimit");
			
			// set new values ti submitted values or to 0 if not set
			if(salesprice != 0)
			{
				itemsell.setPrice(salesprice);
			}
			else
			{
				itemsell.setPrice(0);
			}
			if(buyprice != 0)
			{
				itembuy.setPrice(buyprice);
			}
			else
			{
				itembuy.setPrice(0);
			}
			if(saleslimit != 0)
			{
				itemsell.setLimit(saleslimit);
			}
			else
			{
				itemsell.setLimit(0);
			}
			if(buylimit != 0)
			{
				itembuy.setLimit(buylimit);
			}
			else
			{
				itembuy.setLimit(0);
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
						"item.salesprice",	itemsell.getPrice(),
						"item.buyprice",	itembuy.getPrice(),
						"item.saleslimit",	itemsell.getLimit(),
						"item.buylimit",	itembuy.getLimit(),
						"item.salesprice.parameter",	"i"+aitem.getID()+"salesprice",
						"item.buyprice.parameter",	"i"+aitem.getID()+"buyprice",
						"item.saleslimit.parameter",	"i"+aitem.getID()+"saleslimit",
						"item.buylimit.parameter",	"i"+aitem.getID()+"buylimit",
						"tradepost.id",	shipid );
			
			t.parse("tradepost.post", "tradepost.list", true);
		}
	}
}
