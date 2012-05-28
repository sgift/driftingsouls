package net.driftingsouls.ds2.server.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.FactionShopOrder;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserRank;
import net.driftingsouls.ds2.server.entities.npcorders.Order;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Das Interface fuer NPCs.
 * @author Christopher Jung
 *
 */
@Configurable
@Module(name="npcorder")
public class NPCOrderController extends TemplateGenerator {
	private boolean isHead = false;
	
	private Configuration config;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public NPCOrderController(Context context) {
		super(context);
		
		setTemplate("npcorder.html");
		
		setPageTitle("NPC-Menue");
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
		User user = (User)this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		if( !user.hasFlag( User.FLAG_ORDER_MENU ) ) {
			addError("Nur NPCs k&ouml;nnen dieses Script nutzen", Common.buildUrl("default", "module", "ueber") );
			
			return false;
		}
		
		if( Rassen.get().rasse(user.getRace()).isHead(user.getId()) ) {
			t.setVar( "npcorder.factionhead", 1 );
								
			this.isHead = true;	
		}
		
		if( Faction.get(user.getId()) != null && Faction.get(user.getId()).getPages().hasPage("shop") ) {
			t.setVar("npcorder.shop", 1);	
		}
		
		return true;
	}
	
	/**
	 * Zeigt den aktuellen Status aller fuer Ganymede-Transporte reservierten Transporter an.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void viewTransportsAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();
		
		if( Faction.get(user.getId()) == null || !Faction.get(user.getId()).getPages().hasPage("shop") ) {
			addError("Sie verf&uuml;gen &uuml;ber keinen Shop und k&ouml;nnen daher diese Seite nicht aufrufen");
			redirect();
			return;
		}
		t.setVar("npcorder.transports", 1);
		t.setBlock("_NPCORDER", "transports.listitem", "transports.list");
		
		List<?> ships = db.createQuery("from Ship where owner=? and locate('#!/tm gany_transport',destcom)!=0")
			.setEntity(0, user)
			.list();
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			ShipTypeData ashiptype = aship.getTypeData();
			
			t.setVar(	"transport.ship",		Common._plaintitle(aship.getName()),
						"transport.ship.id",	aship.getId(),
						"transport.ship.picture",	ashiptype.getPicture(),
						"transport.status",		"langeweile",
						"transport.assignment",	"-" );
								
			Taskmanager taskmanager = Taskmanager.getInstance();
			Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.GANY_TRANSPORT, "*", Integer.toString(aship.getId()), "*");
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
			
			t.setVar("transport.status", status);
			
			FactionShopOrder order = (FactionShopOrder)db.get(FactionShopOrder.class, Integer.parseInt(task.getData1())); 
			
			if( order == null)
			{
				db.delete(task);
				continue;
			}
			
			User orderuser = order.getUser();
			
			if(orderuser == null)
			{
				orderuser =  new User();
				orderuser.setName("deleted user");
			}
			
			t.setVar("transport.assignment", order.getId()+": "+Common._title(orderuser.getName())+"<br />"+order.getAddData());
							
			t.parse("transports.list", "transports.listitem", true);
		}
	}

	/**
	 * Zeichnet einen Spieler mit einem Orden aus.
	 * @urlparam Integer edituser Die ID des zu bearbeitenden Spielers
	 * @urlparam Integer medal Die ID des Ordens
	 * @urlparam String Der Grund, warum der Orden verliehen wurde
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void awardMedalAction() {
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();
		
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
		
		User edituser = (User)getContext().getDB().get(User.class, edituserID);
			
		if( edituser == null ) {
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
				config.get("URL")+"data/"+Medals.get().medal(medal).getImage(Medal.IMAGE_SMALL)+"[/img]"+
				Medals.get().medal(medal).getName()+" wurde von [userprofile="+user.getId()+"]"+
				user.getName()+"[/userprofile] verliehen Aufgrund der "+reason);
		
		PM.send(user, edituser.getId(), "Orden '"+Medals.get().medal(medal).getName()+"' verliehen", 
				"Ich habe dir den Orden [img]"+config.get("URL")+
				"data/"+Medals.get().medal(medal).getImage(Medal.IMAGE_SMALL)+"[/img]'"+
				Medals.get().medal(medal).getName()+"' verliehen Aufgrund deiner "+reason);
		
		t.setVar( "npcorder.message", "Dem Spieler wurde der Orden '"+Medals.get().medal(medal).getName()+"' verliehen" );
		
		this.redirect("medals");
	}

	/**
	 * Befoerdert/Degradiert einen Spieler.
	 * @urlparam Integer edituser Die ID des zu bearbeitenden Spielers
	 * @urlparam Integer rank Der neue Rang
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void changeRankAction() {
		User user = (User)this.getUser();

		this.parameterNumber("edituser");
		this.parameterNumber("rank");
		int edituserID = this.getInteger("edituser");
		int rank = this.getInteger("rank");

		User edituser = (User)getContext().getDB().get(User.class, edituserID);

		if( edituser == null ) {
			addError( "Der angegebene Spieler existiert nicht" );
			this.redirect("medals");
			
			return;	
		}

		if( rank < 0 ) {
			addError( "Sie k&ouml;nnen diesen Spieler nicht soweit degradieren" );
			this.redirect("medals");
			
			return;
		}

        edituser.setRank(user, rank);

		this.redirect("medals");
	}

	/**
	 * Zeigt die GUI fuer "Oberhaeupter" von Rassen an, mit der sich Raenge und Orden.
	 * setzen lassen
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void medalsAction() {
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();

		parameterNumber("edituser");
		int edituserID = getInteger("edituser");
		
		t.setVar("npcorder.medalsmenu", 1);
		
		User edituser = (User)getDB().get(User.class, edituserID);
			
		if( edituser == null ) {
			t.setVar("edituser.id", 0);
			
			return;	
		}
		
		edituser.setTemplateVars(t, "edituser");

        UserRank rank = edituser.getRank(user);
        
        t.setBlock("_NPCORDER", "ranks.listitem", "ranks.list");
        for( Rang rang : Medals.get().raenge().values() )
        {
        	t.setVar("rank.id", rang.getID(),
        			"rank.name", rang.getName(),
        			"rank.active", rang.getID() == rank.getRank());
        	
        	t.parse("ranks.list", "ranks.listitem", true);
        }
        
		t.setVar(	"edituser.name",	Common._title(edituser.getName() ),
					"edituser.rank",	rank.getRank());
					
		t.setBlock("_NPCORDER", "medals.listitem", "medals.list");
		for( Medal medal : Medals.get().medals().values() ) {
			if( medal.isAdminOnly() ) {
				continue;
			}
			
			t.setVar(	"medal.name",	medal.getName(),
						"medal.id",		medal.getID(),
						"medal.image",	medal.getImage(Medal.IMAGE_NORMAL) );
								

			t.parse("medals.list", "medals.listitem", true);				
		}
	}
	
	/**
	 * Setzt die Order-Koordinaten, an denen georderte Objekte erscheinen sollen.
	 *
	 * @urlparam String orderloc Die Koordinate des Ortes, an dem die georderten Objekte erscheinen sollen
	 */
	// TODO: fertig implementieren (Interface, Ticks)
	@Action(ActionType.DEFAULT)
	public void changeOrderLocationAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();
		
		parameterString("orderloc");
		String orderloc = getString("orderloc");
		
		Location loc = Location.fromString(orderloc);
		
		String ordermessage = "";
	
		Ship ship = (Ship)db.createQuery("from Ship as s " +
				"where s.shiptype.shipClass= :cls AND s.id>0 " +
					"and s.x= :x and s.y= :y and s.system= :sys and s.owner= :user")
			.setInteger("cls", ShipClasses.STATION.ordinal())
			.setEntity("user", user)
			.setInteger("x", loc.getX())
			.setInteger("y", loc.getY())
			.setInteger("sys", loc.getSystem())
			.setMaxResults(1)
			.uniqueResult();
		
		Base base = (Base)db.createQuery("from Base where owner= :user and " +
				"x= :x and y= :y and system= :sys")
			.setEntity("user", user)
			.setInteger("x", loc.getX())
			.setInteger("y", loc.getY())
			.setInteger("sys", loc.getSystem())
			.setMaxResults(1)
			.uniqueResult();
		
		if( (base != null) || (ship != null) ) {
			user.setNpcOrderLocation(loc.toString());
			
			ordermessage = "Neue Lieferkoordinaten gespeichert";
		}
		else {
			ordermessage = "Keine Lieferung nach "+loc+" m&ouml;glich\n";
		}
		
		t.setVar("npcorder.message", ordermessage);
		
		this.redirect();
	}
	
	
	/**
	 * Ordert eine Menge von Schiffen.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void orderShipsAction()
	{
		org.hibernate.Session db = getDB();
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();
		
		int costs = 0;
		
		List<Order> orderList = new ArrayList<Order>();
		
		List<?> shipOrders = db.createQuery("from OrderableShip order by shipType.shipClass,shipType").list();
		for( Iterator<?> iter=shipOrders.iterator(); iter.hasNext(); )
		{
			OrderableShip ship = (OrderableShip)iter.next();
			
			parameterNumber("ship"+ship.getShipType().getId()+"_count");
			
			int count = getInteger("ship"+ship.getShipType().getId()+"_count");
			if( count > 0 )
			{
				costs += count*ship.getCost();
				
				for( int i=0; i < count; i++ ) {
					Order orderObj = new Order(user.getId(), ship.getId());
					orderObj.setTick(3);
					orderList.add(orderObj);
				}
			}
		}
		
		String ordermessage = "";

		if( costs > 0 ) {
			if( user.getNpcPunkte() < costs ) {
				ordermessage = "<span style=\"color:red\">Nicht genug Kommandopunkte</span>";
			} 
			else {
				ordermessage = orderList.size()+" Schiff(e) zugeteilt - wird/werden in 3 Ticks eintreffen";
				for( Order order : orderList ) {
					db.persist(order);
				}
				
				user.setNpcPunkte( user.getNpcPunkte() - costs );
			}
		} 
		else {
			ordermessage = "Sorry, aber umsonst bekommst du hier nichts...\n";
		}
		
		t.setVar("npcorder.message", ordermessage);
		
		this.redirect();
	}
	
	/**
	 * Ordert eine Menge von Schiffen/Offizieren.
	 * @urlparam Integer order Das zu ordernde Objekt (negativ: offizier)
	 * @urlparam Integer count Die Menge der zu ordernden Objekte
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void orderAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();
		
		parameterNumber("order");
		parameterNumber("count");
		
		int costs = 0;
		
		int order = getInteger("order");
		int count = getInteger("count");
		
		if( count <= 0 ) {
			count = 1;	
		}
		
		if( order < 0 ) {
			OrderableOffizier orderOffi = (OrderableOffizier)db.get(OrderableOffizier.class, -order);
			costs = count*orderOffi.getCost();
		}
		else
		{
			throw new IllegalArgumentException("Unbekannte ID");
		}
		
		String ordermessage = "";

		if( costs > 0 ) {
			if( user.getNpcPunkte() < costs ) {
				ordermessage = "<span style=\"color:red\">Nicht genug Kommandopunkte</span>";
			} 
			else {
				ordermessage = "Offizier(e) zugeteilt - wird/werden in 3 Ticks eintreffen";
				
				for( int i=0; i < count; i++ ) {
					Order orderObj = new Order(user.getId(), order);
					orderObj.setTick(3);
					db.persist(orderObj);
				}
				
				user.setNpcPunkte( user.getNpcPunkte() - costs );
			}
		} 
		else {
			ordermessage = "Sorry, aber umsonst bekommst du hier nichts...\n";
		}
		
		t.setVar("npcorder.message", ordermessage);
		
		this.redirect();
	}

	/**
	 * Zeigt die GUI zum Ordern von Schiffen/Offizieren.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();
		
		Map<Integer,Integer> orders = new HashMap<Integer,Integer>();
		
		t.setVar( "npcorder.ordermenu", 1 );

		List<?> orderList = db.createQuery("from Order where user= :user")
			.setInteger("user", user.getId())
			.list();
		for( Iterator<?> iter=orderList.iterator(); iter.hasNext(); ) {
			Order order = (Order)iter.next();
			Common.safeIntInc(orders, order.getType());
		}

		/*
			Schiffe
		*/
		
		int oldclass = 0;

		t.setBlock("_NPCORDER", "ships.listitem", "ships.list");

		List<?> shipOrders = db.createQuery("from OrderableShip order by shipType.shipClass,shipType").list();
		for( Iterator<?> iter=shipOrders.iterator(); iter.hasNext(); ) {
			OrderableShip ship = (OrderableShip)iter.next();
			
			t.start_record();
			
			if( ship.getShipType().getShipClass() != oldclass ) {
				t.setVar(	"ship.newclass",		1,
							"ship.newclass.name",	ShipTypes.getShipClass(ship.getShipType().getShipClass()).getSingular() );
				
				oldclass = ship.getShipType().getShipClass();
			}
			
			if( !orders.containsKey(ship.getId()) ) {
				orders.put(ship.getId(), 0);
			}
			
			t.setVar(	"ship.name",		ship.getShipType().getNickname(),
						"ship.type",		ship.getId(),
						"ship.cost",		ship.getCost(),
						"ship.ordercount",	orders.get(ship.getId()) );
								
			t.parse("ships.list", "ships.listitem", true);
			
			t.stop_record();
			t.clear_record();
		}
		
		/*
			Offiziere
		*/
		
		t.setBlock("_NPCORDER", "offiziere.listitem", "offiziere.list");
		
		List<?> offizierOrders = db.createQuery("from OrderableOffizier where cost > 0 order by id").list();
		for( Iterator<?> iter=offizierOrders.iterator(); iter.hasNext(); ) {
			OrderableOffizier offizier = (OrderableOffizier)iter.next();
			
			if( !orders.containsKey(-offizier.getId()) ) {
				orders.put(-offizier.getId(), 0);
			}
			
			t.setVar(	"offizier.name",		offizier.getName(),
						"offizier.rang",		offizier.getRang(),
						"offizier.cost",		offizier.getCost(),
						"offizier.id",			-offizier.getId(),
						"offizier.ordercount",	orders.get(offizier.getId()) );
								
			t.parse("offiziere.list", "offiziere.listitem", true);
		}
	}
}
