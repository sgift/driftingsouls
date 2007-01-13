package net.driftingsouls.ds2.server.modules;

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Das Interface fuer NPCs
 * @author Christopher Jung
 *
 */
public class NPCOrderController extends DSGenerator {
	private boolean isHead = false;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public NPCOrderController(Context context) {
		super(context);
		
		setTemplate("npcorder.html");	
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		User user = this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		if( !user.hasFlag( User.FLAG_ORDER_MENU ) ) {
			addError("Nur NPCs k&ouml;nnen dieses Script nutzen", Common.buildUrl(getContext(), "default", "module", "ueber") );
			
			return false;
		}
		
		if( Rassen.get().rasse(user.getRace()).isHead(user.getID()) ) {
			t.set_var( "npcorder.factionhead", 1 );
								
			this.isHead = true;	
		}
		
		if( Faction.get(user.getID()) != null && Faction.get(user.getID()).getPages().hasPage("shop") ) {
			t.set_var("npcorder.shop", 1);	
		}
		
		return true;
	}
	
	/**
	 * Ordert eine Menge von Schiffen/Offizieren
	 * @urlparam Integer order Das zu ordernde Objekt (positiv, dann Schiff; negativ, dann offizier)
	 * @urlparam Integer count Die Menge der zu ordernden Objekte
	 *
	 */
	public void orderAction() {
		Database db = getDatabase();
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		parameterNumber("order");
		parameterNumber("count");
		
		int costs = 0;
		
		int order = getInteger("order");
		int count = getInteger("count");
		
		if( count <= 0 ) {
			count = 1;	
		}
		
		if( order > 0 ) {
			costs = count*db.first("SELECT cost FROM orders_ships WHERE type=",order).getInt("cost");
		}
		else if( order < 0 ) {
			costs = count*db.first("SELECT cost FROM orders_offiziere WHERE id=",(-order)).getInt("cost");
		}
		
		String ordermessage = "";

		if( costs > 0 ) {
			if( user.getNpcPunkte() < costs ) {
				ordermessage = "<span style=\"color:red\">Nicht genug Kommandopunkte</span>";
			} 
			else {
				if( order > 0 ) {
					ordermessage = "Schiff(e) zugeteilt - wird/werden in 3 Ticks eintreffen";
				} 
				else {
					ordermessage = "Offizier(e) zugeteilt - wird/werden in 3 Ticks eintreffen";
				}
				for( int i=0; i < count; i++ ) {
					db.update("INSERT INTO orders (type,tick,user) VALUES (",order,",3,",user.getID(),")");
				}
				
				user.setNpcPunkte( user.getNpcPunkte() - costs );
			}
		} 
		else {
			ordermessage = "Sorry, aber umsonst bekommst du hier nichts...\n";
		}
		
		t.set_var("npcorder.message", ordermessage);
		
		this.redirect();
	}

	/**
	 * Zeigt die GUI zum Ordern von Schiffen/Offizieren
	 */
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		Map<Integer,Integer> orders = new HashMap<Integer,Integer>();
		
		t.set_var( "npcorder.ordermenu", 1 );

		SQLQuery order = db.query("SELECT type FROM orders WHERE user=",user.getID());
		while( order.next() ) {
			Common.safeIntInc(orders, order.getInt("type"));
		}
		order.free();

		/*
			Schiffe
		*/
		
		int oldclass = 0;

		t.set_block("_NPCORDER", "ships.listitem", "ships.list");

		SQLQuery ship = db.query("SELECT t1.*,t2.nickname name,t2.class FROM orders_ships t1 JOIN ship_types t2 ON t1.type=t2.id ORDER BY t2.class,t1.type");
		while( ship.next() ) {
			t.start_record();
			
			if( ship.getInt("class") != oldclass ) {
				t.set_var(	"ship.newclass",		1,
							"ship.newclass.name",	Ships.getShipClass(ship.getInt("class")).getSingular() );
				
				oldclass = ship.getInt("class");
			}
			
			if( orders.containsKey(ship.getInt("type")) ) {
				orders.put(ship.getInt("type"), 0);
			}
			
			t.set_var(	"ship.name",		ship.getString("name"),
						"ship.type",		ship.getInt("type"),
						"ship.cost",		ship.getInt("cost"),
						"ship.ordercount",	orders.get(ship.getInt("type")) );
								
			t.parse("ships.list", "ships.listitem", true);
			
			t.stop_record();
			t.clear_record();
		}
		ship.free();
		
		/*
			Offiziere
		*/
		
		t.set_block("_NPCORDER", "offiziere.listitem", "offiziere.list");
		
		SQLQuery offizier = db.query("SELECT * FROM orders_offiziere WHERE cost > 0 ORDER BY id");
		while( offizier.next() ) {
			if( orders.containsKey(-offizier.getInt("id")) ) {
				orders.put(-offizier.getInt("id"), 0);
			}
			
			t.set_var(	"offizier.name",		offizier.getString("name"),
						"offizier.rang",		offizier.getInt("rang"),
						"offizier.cost",		offizier.getInt("cost"),
						"offizier.id",			-offizier.getInt("id"),
						"offizier.ordercount",	orders.get(offizier.getInt("id")) );
								
			t.parse("offiziere.list", "offiziere.listitem", true);
		}
		offizier.free();
	}
}
