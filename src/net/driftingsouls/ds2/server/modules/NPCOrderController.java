package net.driftingsouls.ds2.server.modules;

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

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
	 * Zeigt den aktuellen Status aller fuer Ganymede-Transporte reservierten Transporter an
	 *
	 */
	public void viewTransportsAction() {
		Database db = getDatabase();
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		if( Faction.get(user.getID()) == null || !Faction.get(user.getID()).getPages().hasPage("shop") ) {
			addError("Sie verf&uuml;gen &uuml;ber keinen Shop und k&ouml;nnen daher diese Seite nicht aufrufen");
			redirect();
			return;
		}
		t.set_var("npcorder.transports", 1);
		t.set_block("_NPCORDER", "transports.listitem", "transports.list");
		
		SQLQuery aship = db.query("SELECT * FROM ships WHERE owner="+user.getID()+" AND LOCATE('#!/tm gany_transport',destcom)");
		while( aship.next() ) {
			SQLResultRow ashiptype = ShipTypes.getShipType(aship.getRow());
			
			t.set_var(	"transport.ship",		Common._plaintitle(aship.getString("name")),
						"transport.ship.id",	aship.getInt("id"),
						"transport.ship.picture",	ashiptype.getString("picture"),
						"transport.status",		"langeweile",
						"transport.assignment",	"-" );
								
			Taskmanager taskmanager = Taskmanager.getInstance();
			Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.GANY_TRANSPORT, "*", Integer.toString(aship.getInt("id")), "*");
			if( tasks.length == 0 ) {
				t.parse("transports.list", "transports.listitem", true);
				continue;
			}
			
			Task task = tasks[0];
			
			String status = "";
			if( task.getData3().equals("1") ) {
				status = "verschiebt gany";
			}
			else if( task.getData3().equals("2") ) {
				status = "r&uuml;ckflug";
			}
			else {
				status = "anreise";	
			}
			
			t.set_var("transport.status", status);
			
			SQLResultRow order = db.first("SELECT * FROM factions_shop_orders WHERE id="+Integer.parseInt(task.getData1()));
			
			User orderuser = getContext().createUserObject(order.getInt("user_id"));
			
			t.set_var("transport.assignment", order.getInt("id")+": "+Common._title(orderuser.getName())+"<br />"+order.getString("adddata"));
							
			t.parse("transports.list", "transports.listitem", true);
		}
		aship.free();
	}

	/**
	 * Zeichnet einen Spieler mit einem Orden aus
	 * @urlparam Integer edituser Die ID des zu bearbeitenden Spielers
	 * @urlparam Integer medal Die ID des Ordens
	 * @urlparam String Der Grund, warum der Orden verliehen wurde
	 *
	 */
	public void awardMedalAction() {
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		if( !this.isHead ) {
			addError( "Sie sind nicht berechtigt auf dieses Men&uuml; zuzugreifen" );
			
			this.redirect();
			return;	
		}	
		
		this.parameterNumber("edituser");
		this.parameterNumber("medal");
		this.parameterString("reason");
		int edituserID = this.getInteger("edituser");
		int medal = this.getInteger("medal");
		String reason = this.getString("reason");
		
		User edituser = getContext().createUserObject(edituserID);
			
		if( edituser.getID() == 0 ) {
			addError( "Der angegebene Spieler existiert nicht" );
			this.redirect("medals");
			
			return;	
		}
		
		if( Medals.get().medal(medal) == null ) {
			addError( "Der angegebene Orden ist nicht vorhanden" );
			this.redirect("medals");
			
			return;	
		}
		
		if( Medals.get().medal(medal).isAdminOnly() ) {
			addError( "Diesen Orden k&ouml;nnen sie nicht verleihen" );
			this.redirect("medals");
			
			return;	
		}
		
		if( reason.length() == 0 ) {
			addError( "Sie m&uuml;ssen einen Grund angeben" );
			this.redirect("medals");
			
			return;	
		}
		
		String medallist = edituser.getMedals();
		edituser.setMedals(medallist.trim().length() > 0 ? medallist+";"+medal : Integer.toString(medal));
		
		int ticks = getContext().get(ContextCommon.class).getTick();
		
		edituser.addHistory(Common.getIngameTime(ticks)+": Der Orden [img]"+
				Configuration.getSetting("URL")+"data/"+Medals.get().medal(medal).getImage(Medal.IMAGE_SMALL)+"[/img]"+
				Medals.get().medal(medal).getName()+" wurde von [userprofile="+user.getID()+"]"+
				user.getName()+"[/userprofile] verliehen Aufgrund der "+reason);
		
		PM.send(getContext(), user.getID(), edituser.getID(), 
				"Orden '"+Medals.get().medal(medal).getName()+"' verliehen", 
				"Ich habe dir den Orden [img]"+Configuration.getSetting("URL")+
				"data/"+Medals.get().medal(medal).getImage(Medal.IMAGE_SMALL)+"[/img]'"+
				Medals.get().medal(medal).getName()+"' verliehen Aufgrund deiner "+reason);
		
		t.set_var( "npcorder.message", "Dem Spieler wurde der Orden '"+Medals.get().medal(medal).getName()+"' verliehen" );
		
		this.redirect("medals");
	}
	
	/**
	 * Befoerdert/Degradiert einen Spieler
	 * @urlparam Integer edituser Die ID des zu bearbeitenden Spielers
	 * @urlparam Integer rang Der neue Rang
	 *
	 */
	public void changeRangAction() {
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		if( !this.isHead ) {
			addError( "Sie sind nicht berechtigt auf dieses Men&uuml; zuzugreifen" );
			
			this.redirect();
			return;	
		}	
		
		this.parameterNumber("edituser");
		this.parameterNumber("rang");
		int edituserID = this.getInteger("edituser");
		int rang = this.getInteger("rang");
		
		User edituser = getContext().createUserObject(edituserID);
			
		if( edituser.getID() == 0 ) {
			addError( "Der angegebene Spieler existiert nicht" );
			this.redirect("medals");
			
			return;	
		}
		
		if( user.getRang() <= edituser.getRang() ) {
			addError( "Sie k&ouml;nnen diesen Spieler weder bef&ouml;rdern noch degradieren" );
			this.redirect("medals");
			
			return;
		}
		
		if( rang > user.getRang()-1 ) {
			addError( "Sie k&ouml;nnen diesen Spieler nicht soweit bef&ouml;rdern" );
			this.redirect("medals");
			
			return;
		}
		
		if( rang < 0 ) {
			addError( "Sie k&ouml;nnen diesen Spieler nicht soweit degradieren" );
			this.redirect("medals");
			
			return;
		}
		
		if( rang > edituser.getRang()+1 ) {
			addError( "Sie k&ouml;nnen diesen Spieler nicht so schnell bef&ouml;rdern" );
			this.redirect("medals");
			
			return;
		}
		
		if( rang < edituser.getRang()-1 ) {
			addError( "Sie k&ouml;nnen diesen Spieler nicht so schnell degradieren" );
			this.redirect("medals");
			
			return;
		}
		
		int ticks = getContext().get(ContextCommon.class).getTick();
		
		edituser.addHistory(Common.getIngameTime(ticks)+": Von "+user.getName()+" zum [img]"+
				Configuration.getSetting("URL")+"data/interface/medals/rang"+rang+"+png[/img] ["+
				Medals.get().rang(rang).getName()+"] "+( rang > edituser.getRang() ? "bef&ouml;rdert" : "degradiert"));
		
		t.set_var( "npcorder.message", "Der Spieler wurde zum "+Medals.get().rang(rang).getName()+" "+
				( rang > edituser.getRang() ? "bef&ouml;rdert" : "degradiert") );
		
		edituser.setRang(rang);
		
		this.redirect("medals");
	}

	/**
	 * Zeigt die GUI fuer "Oberhaeupter" von Rassen an, mit der sich Raenge und Orden
	 * setzen lassen
	 *
	 */
	public void medalsAction() {
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		if( !this.isHead ) {
			addError( "Sie sind nicht berechtigt auf dieses Men&uuml; zuzugreifen" );
			
			redirect();
			return;	
		}	
		
		parameterNumber("edituser");
		int edituserID = getInteger("edituser");
		
		t.set_var("npcorder.medalsmenu", 1);
		
		User edituser = getContext().createUserObject(edituserID);
			
		if( edituser.getID() == 0 ) {
			t.set_var("edituser.id", 0);
			
			return;	
		}
		
		edituser.setTemplateVars(t, "edituser");
		
		boolean canEditRang = user.getRang() > edituser.getRang();
		
		t.set_var(	"edituser.name",			Common._title(edituser.getName() ),
					"edituser.rang.name",		Medals.get().rang(edituser.getRang()).getName(),
					"edituser.rang.next",		(edituser.getRang() < user.getRang()-1 ? edituser.getRang()+1 : 0),
					"edituser.rang.next.name",	(edituser.getRang() < user.getRang()-1 ? Medals.get().rang(edituser.getRang()+1) : 0),
					"edituser.rang.prev",		(canEditRang && (edituser.getRang() > 0) ? edituser.getRang()-1 : 0),
					"edituser.rang.prev.name",	(canEditRang && (edituser.getRang() > 0) ? Medals.get().rang(edituser.getRang()-1) : 0) );
			
		int i = 8;
							
		t.set_block("_NPCORDER", "medals.listitem", "medals.list");
		for( Medal medal : Medals.get().medals().values() ) {
			if( medal.isAdminOnly() ) {
				continue;
			}
			
			t.set_var(	"medal.name",	medal.getName(),
						"medal.id",		medal.getID(),
						"medal.image",	medal.getImage(Medal.IMAGE_NORMAL),
						"medal.newrow",	(i % 8) == 0,
						"medal.endrow",	(i + 1 % 8) == 0 );
								
			i++;
			
			t.parse("medals.list", "medals.listitem", true);				
		}
	}
	
	/**
	 * Setzt die Order-Koordinaten, an denen georderte Objekte erscheinen sollen
	 *
	 * @urlparam String orderloc Die Koordinate des Ortes, an dem die georderten Objekte erscheinen sollen
	 */
	// TODO: fertig implementieren (Interface, Ticks)
	public void changeOrderLocationAction() {
		Database db = getDatabase();
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		parameterString("orderloc");
		String orderloc = getString("orderloc");
		
		Location loc = Location.fromString(orderloc);
		
		String ordermessage = "";
	
		SQLResultRow ships = db.first("SELECT s.id FROM ships s JOIN ship_types st ON s.type=st.id " +
				"WHERE st.class="+ShipClasses.STATION.ordinal()+" AND s.id>0 " +
					"AND s.x="+loc.getX()+" AND s.y="+loc.getY()+" AND s.system="+loc.getSystem()+" AND s.owner="+user.getID());
		
		SQLResultRow bases = db.first("SELECT id FROM bases WHERE owner="+user.getID()+" AND " +
				"s.x="+loc.getX()+" AND s.y="+loc.getY()+" AND s.system="+loc.getSystem());
		
		if( !bases.isEmpty() || !ships.isEmpty() ) {
			user.setNpcOrderLocation(loc.toString());
			
			ordermessage = "Neue Lieferkoordinaten gespeichert";
		}
		else {
			ordermessage = "Keine Lieferung nach "+loc+" m&ouml;glich\n";
		}
		
		t.set_var("npcorder.message", ordermessage);
		
		this.redirect();
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
							"ship.newclass.name",	ShipTypes.getShipClass(ship.getInt("class")).getSingular() );
				
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
