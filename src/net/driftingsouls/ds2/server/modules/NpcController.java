package net.driftingsouls.ds2.server.modules;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.FactionShopOrder;
import net.driftingsouls.ds2.server.entities.FraktionAktionsMeldung;
import net.driftingsouls.ds2.server.entities.Loyalitaetspunkte;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserRank;
import net.driftingsouls.ds2.server.entities.npcorders.Order;
import net.driftingsouls.ds2.server.entities.npcorders.OrderOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.JSONUtils;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParamType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParams;
import net.driftingsouls.ds2.server.namegenerator.NameGenerator;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Das Interface fuer NPCs.
 * @author Christopher Jung
 *
 */
@Configurable
@Module(name="npc")
public class NpcController extends AngularGenerator {
	private boolean isHead = false;
	private boolean shop = false;

	private Configuration config;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public NpcController(Context context) {
		super(context);

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
		
		if( !user.hasFlag( User.FLAG_ORDER_MENU ) ) {
			addError("Nur NPCs können dieses Script nutzen", Common.buildUrl("default", "module", "ueber") );

			return false;
		}

		if( Rassen.get().rasse(user.getRace()).isHead(user.getId()) ) {
			this.isHead = true;
		}

		if( Faction.get(user.getId()) != null && Faction.get(user.getId()).getPages().hasPage("shop") ) {
			this.shop = true;
		}

		return true;
	}

	/**
	 * Zeigt den aktuellen Status aller fuer Ganymede-Transporte reservierten Transporter an.
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject shopMenuAction() {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();
		
		if( !this.shop )
		{
			return JSONUtils.error("Sie verfügen über keinen Shop und können daher diese Seite nicht aufrufen");
		}
		
		JSONObject result = new JSONObject();
		fillCommonMenuResultData(result);
		
		JSONArray transpListObj = new JSONArray();
	
		List<?> ships = db.createQuery("from Ship s where s.owner=:user and locate('#!/tm gany_transport',s.einstellungen.destcom)!=0")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			ShipTypeData ashiptype = aship.getTypeData();

			JSONObject transObj = new JSONObject();
			transObj.accumulate("status", "lageweile")
				.accumulate("auftrag", "-");
			
			JSONObject shipObj = new JSONObject();
			shipObj.accumulate("id", aship.getId())
				.accumulate("name", aship.getName())
				.accumulate("picture", ashiptype.getPicture());
			
			transObj.accumulate("ship", shipObj);
			transpListObj.add(transObj);

			Taskmanager taskmanager = Taskmanager.getInstance();
			Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.GANY_TRANSPORT, "*", Integer.toString(aship.getId()), "*");
			if( tasks.length == 0 ) {
				continue;
			}

			Task task = tasks[0];

			String status = "";
			if( task.getData3().equals("1") ) {
				status = "verschiebt gany";
			}
			else if( task.getData3().equals("2") ) {
				status = "rückflug";
			}
			else {
				status = "anreise";
			}

			transObj.accumulate("status", status);
			
			FactionShopOrder order = (FactionShopOrder)db
					.get(FactionShopOrder.class, Integer.parseInt(task.getData1()));

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

			transObj.accumulate("auftrag", order.getId()+": "+Common._title(orderuser.getName())+"\n"+order.getAddData());
		}
		
		result.accumulate("transporter", transpListObj);
		return result;
	}

	/**
	 * Zeichnet einen Spieler mit einem Orden aus.
	 * @urlparam Integer edituser Die ID des zu bearbeitenden Spielers
	 * @urlparam Integer medal Die ID des Ordens
	 * @urlparam String Der Grund, warum der Orden verliehen wurde
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject awardMedalAction() {
		User user = (User)this.getUser();

		if( !this.isHead ) {
			return JSONUtils.failure("Sie sind nicht berechtigt auf dieses Menü zuzugreifen");
		}

		this.parameterString("edituser");
		this.parameterNumber("medal");
		this.parameterString("reason");
		String edituserID = this.getString("edituser");
		int medal = this.getInteger("medal");
		String reason = this.getString("reason");

		User edituser = User.lookupByIdentifier(edituserID);

		if( edituser == null ) {
			return JSONUtils.failure("Der angegebene Spieler existiert nicht");
		}

		if( Medals.get().medal(medal) == null ) {
			return JSONUtils.failure("Der angegebene Orden ist nicht vorhanden");
		}

		if( Medals.get().medal(medal).isAdminOnly() ) {
			return JSONUtils.failure("Diesen Orden kännen sie nicht verleihen");
		}

		if( reason.length() == 0 ) {
			return JSONUtils.failure("Sie müssen einen Grund angeben");
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

		return JSONUtils.success("Dem Spieler wurde der Orden '"+
				Medals.get().medal(medal).getName()+
				"' verliehen");
	}

	/**
	 * Befoerdert/Degradiert einen Spieler.
	 * @urlparam Integer edituser Die ID des zu bearbeitenden Spielers
	 * @urlparam Integer rank Der neue Rang
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject changeRankAction() {
		User user = (User)this.getUser();

		this.parameterString("edituser");
		this.parameterNumber("rank");
		String edituserID = this.getString("edituser");
		int rank = this.getInteger("rank");

		User edituser = User.lookupByIdentifier(edituserID);
		
		if( edituser == null ) {
			return JSONUtils.failure("Der angegebene Spieler existiert nicht");
		}

		if( rank < 0 ) {
			return JSONUtils.failure("Sie können diesen Spieler nicht soweit degradieren");
		}

		edituser.setRank(user, rank);

		return JSONUtils.success("Rang geändert");
	}

	@Action(ActionType.AJAX)
	public JSONObject deleteLpAction()
	{
		parameterString("edituser");
		parameterNumber("lp");
		String edituserID = getString("edituser");
		int lpId = getInteger("lp");

		User edituser = User.lookupByIdentifier(edituserID);
		if( edituser == null )
		{
			return JSONUtils.failure("Der Spieler existiert nicht");
		}

		Loyalitaetspunkte lp = null;
		for( Loyalitaetspunkte alp : edituser.getLoyalitaetspunkte() )
		{
			if( alp.getId() == lpId )
			{
				lp = alp;
				break;
			}
		}

		if( lp == null )
		{
			return JSONUtils.failure("Der LP-Eintrag wurde nicht gefunden");
		}

		org.hibernate.Session db = getDB();

		db.delete(lp);
		edituser.getLoyalitaetspunkte().remove(lp);

		return JSONUtils.success("Eintrag gelöscht");
	}

	/**
	 * Fuegt LP zu einem Spieler hinzu.
	 * @return Das Antwortobjekt
	 */
	@Action(ActionType.AJAX)
	@UrlParams({
			@UrlParam(name="edituser", description = "Die Identifikationsdaten des zu bearbeitenden Spielers"),
			@UrlParam(name="grund", description = "Der Grund fuer die LP"),
			@UrlParam(name="anmerkungen", description = "Weitere Anmerkungen zur Vergabe der LP"),
			@UrlParam(name="punkte", type= UrlParamType.NUMBER, description = "Die Anzahl der LP"),
			@UrlParam(name="pm", type=UrlParamType.NUMBER, description = "1 falls eine PM an den Spieler versendet werden soll")
	})
	public JSONObject editLpAction()
	{
		User user = (User)this.getUser();

		String edituserID = getString("edituser");

		User edituser = User.lookupByIdentifier(edituserID);
		if( edituser == null )
		{
			return JSONUtils.failure("Benutzer nicht gefunden");
		}

		String grund = getString("grund");
		String anmerkungen = getString("anmerkungen");
		int punkte = getInteger("punkte");
		boolean sendPm = getInteger("pm") == 1;

		if( punkte == 0 || grund.isEmpty() )
		{
			return JSONUtils.failure("Sie muessen den Grund und die Anzahl der Punkte angeben");
		}

		org.hibernate.Session db = getDB();

		Loyalitaetspunkte lp = new Loyalitaetspunkte(edituser, user, grund, punkte);
		lp.setAnmerkungen(anmerkungen);
		edituser.getLoyalitaetspunkte().add(lp);

		db.persist(lp);

		if( sendPm )
		{
			String pmText = "[Automatische Mitteilung]\n" +
					"Du hast soeben "+punkte+" Loyalitätspunkte erhalten. " +
					"Du verfügst nun insgesamt über "+edituser.getLoyalitaetspunkteTotalBeiNpc(user)+" Loyalitätspunkte bei mir.\n\n";
			pmText += "Grund für die Vergabe: "+grund;
			PM.send(user, edituser.getId(), "Loyalitätspunkte erhalten", pmText, PM.FLAGS_AUTOMATIC);
		}

		return JSONUtils.success(punkte+" LP vergeben");
	}

	/**
	 * Markiert die Meldung einer Aktion als "bearbeitet".
	 * @return Die JSON-Antwort
	 */
	@Action(ActionType.AJAX)
	@UrlParam(name="meldung", type=UrlParamType.NUMBER, description = "Die ID der Meldung")
	public JSONObject meldungBearbeitetAction()
	{
		FraktionAktionsMeldung meldung =
				(FraktionAktionsMeldung)getDB().get(FraktionAktionsMeldung.class, (long)getInteger("meldung"));

		if( meldung == null )
		{
			return JSONUtils.error("Die angegebene Meldung konnte nicht gefunden werden");
		}
		meldung.setBearbeitetAm(new Date());
		return JSONUtils.success("Die Meldung wurde als bearbeitet markiert");
	}

	/**
	 * Zeigt die GUI fuer LP-Verwaltung an.
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject lpMenuAction()
	{
		User user = (User)this.getUser();

		parameterString("edituser");
		parameterNumber("alleMeldungen");
		String edituserID = getString("edituser");
		boolean alleMeldungen = getInteger("alleMeldungen") != 0;
		
		JSONObject result = new JSONObject();
		fillCommonMenuResultData(result);
		result.accumulate("alleMeldungen", alleMeldungen);

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -14);

		List<FraktionAktionsMeldung> meldungen;
		if( alleMeldungen )
		{
			meldungen = Common.cast(getDB()
					.createQuery("from FraktionAktionsMeldung where bearbeitetAm is null or bearbeitetAm>:maxBearbeitet order by bearbeitetAm,gemeldetAm")
					.setDate("maxBearbeitet", cal.getTime())
					.list());
		}
		else {
			meldungen = Common.cast(getDB()
					.createQuery("from FraktionAktionsMeldung where fraktion=:user and (bearbeitetAm is null or bearbeitetAm>:maxBearbeitet) order by bearbeitetAm,gemeldetAm")
					.setEntity("user", user)
					.setDate("maxBearbeitet", cal.getTime())
					.list());
		}

		JSONArray meldungenListObj = new JSONArray();
		for (FraktionAktionsMeldung meldung : meldungen)
		{
			JSONObject meldungObj = new JSONObject();
			meldungObj.accumulate("id", meldung.getId());
			meldungObj.accumulate("von", meldung.getGemeldetVon().toJSON());
			meldungObj.accumulate("am", meldung.getGemeldetAm().getTime());
			meldungObj.accumulate("fraktion", meldung.getFraktion().toJSON());
			meldungObj.accumulate("meldungstext", meldung.getMeldungstext());
			meldungObj.accumulate("bearbeitetAm", meldung.getBearbeitetAm() != null ? meldung.getBearbeitetAm().getTime() : null);
			meldungenListObj.add(meldungObj);
		}

		result.accumulate("meldungen", meldungenListObj);

		User edituser = User.lookupByIdentifier(edituserID);

		if( edituser == null ) {
			return result;
		}

		result.accumulate("user", edituser.toJSON());

		UserRank rank = edituser.getRank(user);

		//DateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm");

		JSONArray lpListObj = new JSONArray();
		for( Loyalitaetspunkte lp : new TreeSet<Loyalitaetspunkte>(edituser.getLoyalitaetspunkte()) )
		{
			JSONObject lpObj = lp.toJSON();
			lpObj.accumulate("verliehenDurch", lp.getVerliehenDurch().toJSON());

			lpListObj.add(lpObj);
		}
		result.accumulate("lpListe", lpListObj);
		result.accumulate("rang", rank.getName());
		result.accumulate("lpBeiNpc", edituser.getLoyalitaetspunkteTotalBeiNpc(user));

		return result;
	}

	/**
	 * Zeigt die GUI fuer die Vergabe von Raengen und Orden an.
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject raengeMenuAction() {
		User user = (User)this.getUser();

		parameterString("edituser");
		String edituserID = getString("edituser");

		User edituser = User.lookupByIdentifier(edituserID);
		
		JSONObject result = new JSONObject();
		fillCommonMenuResultData(result);

		if( edituser == null ) {
			return result;
		}

		result.accumulate("user", edituser.toJSON());

		UserRank rank = edituser.getRank(user);
		result.accumulate("aktiverRang", rank.getRank());
		
		JSONArray raengeObj = new JSONArray();
		for( Rang rang : user.getOwnGrantableRanks() )
		{
			JSONObject rangObj = new JSONObject();
			rangObj.accumulate("id", rang.getId());
			if( rang.getId() == 0 )
			{
				rangObj.accumulate("name", "-");
			}
			else
			{
				rangObj.accumulate("name", rang.getName());
			}
			raengeObj.add(rangObj);
		}
		
		result.accumulate("raenge", raengeObj);

		JSONArray medalsObj = new JSONArray();
		for( Medal medal : Medals.get().medals().values() ) {
			if( medal.isAdminOnly() ) {
				continue;
			}
			
			medalsObj.add(medal.toJSON());
		}
		result.accumulate("medals", medalsObj);
		
		return result;
	}

	/**
	 * Setzt die Order-Koordinaten, an denen georderte Objekte erscheinen sollen.
	 *
	 * @urlparam String orderloc Die Koordinate des Ortes, an dem die georderten Objekte erscheinen sollen
	 */
	@Action(ActionType.AJAX)
	public JSONObject changeOrderLocationAction() {
		User user = (User)this.getUser();

		parameterString("lieferposition");
		String orderloc = getString("lieferposition");

		if( orderloc.isEmpty() )
		{
			user.setNpcOrderLocation(null);
			return JSONUtils.success("Lieferkoordinaten zurückgesetzt");
		}
		
		Location loc = Location.fromString(orderloc);

		if( !Base.byLocationAndBesitzer(loc, user).isEmpty() ) {
			user.setNpcOrderLocation(loc.asString());

			return JSONUtils.success("Neue Lieferkoordinaten gespeichert");
		}
		
		return JSONUtils.failure("Keine Lieferung nach "+loc.asString()+" möglich");
	}


	/**
	 * Ordert eine Menge von Schiffen.
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject orderShipsAction()
	{
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();

		parameterNumber("shipflag_disableiff");
		parameterNumber("shipflag_handelsposten");
		parameterNumber("shipflag_nichtkaperbar");
		boolean flagDisableIff = getInteger("shipflag_disableiff") == 1;
		boolean flagHandelsposten = getInteger("shipflag_handelsposten") == 1;
		boolean flagNichtKaperbar = getInteger("shipflag_nichtkaperbar") == 1;

		int costs = 0;

		List<Order> orderList = new ArrayList<Order>();

		List<?> shipOrders = db
				.createQuery("from OrderableShip s order by s.shipType.shipClass,s.shipType.id")
				.list();
		for (Object shipOrder : shipOrders)
		{
			OrderableShip ship = (OrderableShip) shipOrder;

			parameterNumber("ship" + ship.getShipType().getId() + "_count");

			int count = getInteger("ship" + ship.getShipType().getId() + "_count");
			if (count > 0)
			{
				costs += count * ship.getCost();

				for (int i = 0; i < count; i++)
				{
					OrderShip orderObj = new OrderShip(user.getId(), ship.getId());
					orderObj.setTick(1);
					if (flagDisableIff)
					{
						orderObj.addFlag("disable_iff");
						costs += 5;
					}
					if (flagNichtKaperbar || flagHandelsposten)
					{
						orderObj.addFlag("nicht_kaperbar");
						costs += 5;
					}
					if (flagHandelsposten)
					{
						orderObj.addFlag("tradepost");
						costs += 5;
					}

					orderList.add(orderObj);
				}
			}
		}

		if( costs > 0 ) {
			if( user.getNpcPunkte() < costs ) {
				return JSONUtils.failure("Nicht genug Kommandopunkte");
			}

			for( Order order : orderList ) {
				db.persist(order);
			}

			user.setNpcPunkte( user.getNpcPunkte() - costs );
			
			return JSONUtils.success(orderList.size()+" Schiff(e) zugeteilt - wird/werden in 1 Tick(s) eintreffen");
		}
		
		return JSONUtils.failure("Sorry, aber umsonst bekommst du hier nichts...");
	}

	/**
	 * Ordert eine Menge von Schiffen/Offizieren.
	 * @urlparam Integer order Das zu ordernde Objekt (negativ: offizier)
	 * @urlparam Integer count Die Menge der zu ordernden Objekte
	 *
	 */
	@Action(ActionType.AJAX)
	public JSONObject orderAction() {
		org.hibernate.Session db = getDB();
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

		JSONObject result = new JSONObject();
		result.accumulate("success", false);
		
		if( costs > 0 ) {
			if( user.getNpcPunkte() < costs ) {
				return JSONUtils.failure("Nicht genug Kommandopunkte");
			}
			for( int i=0; i < count; i++ ) {
				Order orderObj = new OrderOffizier(user.getId(), -order);
				orderObj.setTick(1);
				db.persist(orderObj);
			}

			user.setNpcPunkte( user.getNpcPunkte() - costs );
			return JSONUtils.success("Offizier(e) zugeteilt - wird/werden in 1 Tick(s) eintreffen");
		}
		
		return JSONUtils.failure("Sorry, aber umsonst bekommst du hier nichts...");
	}

	/**
	 * Zeigt die GUI zum Ordern von Schiffen/Offizieren.
	 */
	@Action(ActionType.AJAX)
	public JSONObject orderMenuAction() {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();

		Map<Integer,Integer> shiporders = new HashMap<Integer,Integer>();
		Map<Integer,Integer> offiorders = new HashMap<Integer,Integer>();

		JSONObject result = new JSONObject();
		fillCommonMenuResultData(result);

		List<?> orderList = db.createQuery("from Order where user= :user")
			.setInteger("user", user.getId())
			.list();
		for (Object anOrderList : orderList)
		{
			Order order = (Order) anOrderList;
			if (order instanceof OrderShip)
			{
				Common.safeIntInc(shiporders, ((OrderShip) order).getType());
			}
			else if (order instanceof OrderOffizier)
			{
				Common.safeIntInc(offiorders, ((OrderOffizier) order).getType());
			}
		}

		/*
			Schiffe
		*/

		JSONArray shipResultList = new JSONArray();

		List<?> shipOrders = db.createQuery("from OrderableShip order by shipType.shipClass,shipType.id").list();
		for (Object shipOrder : shipOrders)
		{
			OrderableShip ship = (OrderableShip) shipOrder;

			if (!Rassen.get().rasse(user.getRace()).isMemberIn(ship.getRasse()))
			{
				continue;
			}

			JSONObject resShip = new JSONObject();

			resShip.accumulate("klasse", ship.getShipType().getShipClass().getSingular());

			if (!shiporders.containsKey(ship.getId()))
			{
				shiporders.put(ship.getId(), 0);
			}

			resShip.accumulate("id", ship.getShipType().getId());
			resShip.accumulate("name", ship.getShipType().getNickname());
			resShip.accumulate("type", ship.getShipType().getId());
			resShip.accumulate("cost", ship.getCost());
			resShip.accumulate("ordercount", shiporders.get(ship.getId()));

			shipResultList.add(resShip);
		}
		
		result.accumulate("ships", shipResultList);

		/*
			Offiziere
		*/

		JSONArray resOffizierList = new JSONArray();

		List<?> offizierOrders = db.createQuery("from OrderableOffizier where cost > 0 order by id").list();
		for( Iterator<?> iter=offizierOrders.iterator(); iter.hasNext(); ) {
			OrderableOffizier offizier = (OrderableOffizier)iter.next();

			if( !offiorders.containsKey(-offizier.getId()) ) {
				offiorders.put(-offizier.getId(), 0);
			}

			JSONObject resOffizier = new JSONObject();
			resOffizier.accumulate("name", offizier.getName());
			resOffizier.accumulate("rang", offizier.getRang());
			resOffizier.accumulate("cost", offizier.getCost());
			resOffizier.accumulate("id", -offizier.getId());
			resOffizier.accumulate("ordercount", offiorders.get(offizier.getId()));
			
			resOffizierList.add(resOffizier);
		}

		result.accumulate("offiziere", resOffizierList);
		result.accumulate("npcpunkte", user.getNpcPunkte());
		
		outputLieferposition(result, user);
		
		return result;
	}

	private void fillCommonMenuResultData(JSONObject result)
	{
		JSONObject menuObj = new JSONObject();
		menuObj.accumulate("head", this.isHead);
		menuObj.accumulate("shop", this.shop);
		
		result.accumulate("menu", menuObj);
	}

	private void outputLieferposition(JSONObject result, User user)
	{
		Set<Location> uniqueLocations = new HashSet<Location>();

		Location lieferpos = null;
		if( user.getNpcOrderLocation() != null )
		{
			lieferpos = Location.fromString(user.getNpcOrderLocation());
		}
		
		JSONArray resLieferPos = new JSONArray();
		for( Base base : user.getBases() )
		{
			// Jede Position nur einmal auflisten!
			if( !uniqueLocations.add(base.getLocation()) )
			{
				continue;
			}
			JSONObject resPos = new JSONObject();
			resPos
				.accumulate("pos", base.getLocation().asString())
				.accumulate("name", Common._plaintitle(base.getName()));
			
			resLieferPos.add(resPos);
		}
		
		result.accumulate("lieferpositionen", resLieferPos);
		result.accumulate("aktuelleLieferposition", lieferpos != null ? lieferpos.asString() : null);
	}
}
