package net.driftingsouls.ds2.server.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.JSONUtils;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.AngularController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Das Interface fuer NPCs.
 * @author Christopher Jung
 *
 */
@Module(name="npc")
public class NpcController extends AngularController
{
	private boolean isHead = false;
	private boolean shop = false;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public NpcController(Context context) {
		super(context);

		setPageTitle("NPC-Menue");
	}

	@Override
	protected boolean validateAndPrepare() {
		User user = (User)this.getUser();
		
		if( !user.hasFlag( User.FLAG_ORDER_MENU ) ) {
			throw new ValidierungException("Nur NPCs können dieses Script nutzen", Common.buildUrl("default", "module", "ueber") );
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
	public JsonElement shopMenuAction() {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();
		
		if( !this.shop )
		{
			return JSONUtils.error("Sie verfügen über keinen Shop und können daher diese Seite nicht aufrufen");
		}
		
		JsonObject result = new JsonObject();
		fillCommonMenuResultData(result);
		
		JsonArray transpListObj = new JsonArray();
	
		List<?> ships = db.createQuery("from Ship s where s.owner=:user and locate('#!/tm gany_transport',s.einstellungen.destcom)!=0")
			.setEntity("user", user)
			.list();
		for (Object ship : ships)
		{
			Ship aship = (Ship) ship;
			ShipTypeData ashiptype = aship.getTypeData();

			JsonObject transObj = new JsonObject();
			transObj.addProperty("status", "lageweile");
			transObj.addProperty("auftrag", "-");

			JsonObject shipObj = new JsonObject();
			shipObj.addProperty("id", aship.getId());
			shipObj.addProperty("name", aship.getName());
			shipObj.addProperty("picture", ashiptype.getPicture());

			transObj.add("ship", shipObj);
			transpListObj.add(transObj);

			Taskmanager taskmanager = Taskmanager.getInstance();
			Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.GANY_TRANSPORT, "*", Integer.toString(aship.getId()), "*");
			if (tasks.length == 0)
			{
				continue;
			}

			Task task = tasks[0];

			String status;
			if (task.getData3().equals("1"))
			{
				status = "verschiebt gany";
			}
			else if (task.getData3().equals("2"))
			{
				status = "rückflug";
			}
			else
			{
				status = "anreise";
			}

			transObj.addProperty("status", status);

			FactionShopOrder order = (FactionShopOrder) db
					.get(FactionShopOrder.class, Integer.parseInt(task.getData1()));

			if (order == null)
			{
				db.delete(task);
				continue;
			}

			User orderuser = order.getUser();

			if (orderuser == null)
			{
				orderuser = new User();
				orderuser.setName("deleted user");
			}

			transObj.addProperty("auftrag", order.getId() + ": " + Common._title(orderuser.getName()) + "\n" + order.getAddData());
		}
		
		result.add("transporter", transpListObj);
		return result;
	}

	/**
	 * Zeichnet einen Spieler mit einem Orden aus.
	 * @param edituserID Die ID des zu bearbeitenden Spielers
	 * @param medal Die ID des Ordens
	 * @param reason Der Grund, warum der Orden verliehen wurde
	 *
	 */
	@Action(ActionType.AJAX)
	public JsonElement awardMedalAction(@UrlParam(name="edituser") String edituserID, int medal, String reason) {
		User user = (User)this.getUser();

		if( !this.isHead ) {
			return JSONUtils.failure("Sie sind nicht berechtigt auf dieses Menü zuzugreifen");
		}

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
				"./data/"+Medals.get().medal(medal).getImage(Medal.IMAGE_SMALL)+"[/img]"+
				Medals.get().medal(medal).getName()+" wurde von [userprofile="+user.getId()+"]"+
				user.getName()+"[/userprofile] verliehen Aufgrund der "+reason);

		PM.send(user, edituser.getId(), "Orden '"+Medals.get().medal(medal).getName()+"' verliehen",
				"Ich habe dir den Orden [img]"+
				"./data/"+Medals.get().medal(medal).getImage(Medal.IMAGE_SMALL)+"[/img]'"+
				Medals.get().medal(medal).getName()+"' verliehen Aufgrund deiner "+reason);

		return JSONUtils.success("Dem Spieler wurde der Orden '"+
				Medals.get().medal(medal).getName()+
				"' verliehen");
	}

	/**
	 * Befoerdert/Degradiert einen Spieler.
	 * @param edituserID Die ID des zu bearbeitenden Spielers
	 * @param rank Der neue Rang
	 *
	 */
	@Action(ActionType.AJAX)
	public JsonElement changeRankAction(@UrlParam(name="edituser") String edituserID, int rank) {
		User user = (User)this.getUser();

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
	public JsonElement deleteLpAction(@UrlParam(name="edituser") String edituserID, @UrlParam(name="lp") int lpId)
	{
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
	 * @param anmerkungen Weitere Anmerkungen zur Vergabe der LP
	 * @param edituserID Die Identifikationsdaten des zu bearbeitenden Spielers
	 * @param grund Der Grund fuer die LP
	 * @param pm <code>true</code> falls eine PM an den Spieler versendet werden soll
	 * @param punkte Die Anzahl der LP
	 * @return Das Antwortobjekt
	 */
	@Action(ActionType.AJAX)
	public JsonElement editLpAction(@UrlParam(name="edituser") String edituserID, String grund, String anmerkungen, int punkte, boolean pm)
	{
		User user = (User)this.getUser();

		User edituser = User.lookupByIdentifier(edituserID);
		if( edituser == null )
		{
			return JSONUtils.failure("Benutzer nicht gefunden");
		}

		if( punkte == 0 || grund.isEmpty() )
		{
			return JSONUtils.failure("Sie muessen den Grund und die Anzahl der Punkte angeben");
		}

		org.hibernate.Session db = getDB();

		Loyalitaetspunkte lp = new Loyalitaetspunkte(edituser, user, grund, punkte);
		lp.setAnmerkungen(anmerkungen);
		edituser.getLoyalitaetspunkte().add(lp);

		db.persist(lp);

		if( pm )
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
	 * @param meldung Die Meldung
	 * @return Die JSON-Antwort
	 */
	@Action(ActionType.AJAX)
	public JsonElement meldungBearbeitetAction(FraktionAktionsMeldung meldung)
	{
		if( meldung == null )
		{
			return JSONUtils.error("Die angegebene Meldung konnte nicht gefunden werden");
		}
		meldung.setBearbeitetAm(new Date());
		return JSONUtils.success("Die Meldung wurde als bearbeitet markiert");
	}

	/**
	 * Zeigt die GUI fuer LP-Verwaltung an.
	 * @param edituserID Die Identifikationsdaten des anzuzeigenden Spielers
	 * @param alleMeldungen <code>true</code>, falls alle Meldungen angezeigt werden sollen
	 *
	 */
	@Action(ActionType.AJAX)
	public JsonElement lpMenuAction(@UrlParam(name="edituser") String edituserID, boolean alleMeldungen)
	{
		User user = (User)this.getUser();

		JsonObject result = new JsonObject();
		fillCommonMenuResultData(result);
		result.addProperty("alleMeldungen", alleMeldungen);

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

		JsonArray meldungenListObj = new JsonArray();
		for (FraktionAktionsMeldung meldung : meldungen)
		{
			JsonObject meldungObj = new JsonObject();
			meldungObj.addProperty("id", meldung.getId());
			meldungObj.add("von", meldung.getGemeldetVon().toJSON());
			meldungObj.addProperty("am", meldung.getGemeldetAm().getTime());
			meldungObj.add("fraktion", meldung.getFraktion().toJSON());
			meldungObj.addProperty("meldungstext", meldung.getMeldungstext());
			meldungObj.addProperty("bearbeitetAm", meldung.getBearbeitetAm() != null ? meldung.getBearbeitetAm().getTime() : null);
			meldungenListObj.add(meldungObj);
		}

		result.add("meldungen", meldungenListObj);

		User edituser = User.lookupByIdentifier(edituserID);

		if( edituser == null ) {
			return result;
		}

		result.add("user", edituser.toJSON());

		UserRank rank = edituser.getRank(user);

		//DateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm");

		JsonArray lpListObj = new JsonArray();
		for( Loyalitaetspunkte lp : new TreeSet<>(edituser.getLoyalitaetspunkte()) )
		{
			JsonElement lpObj = lp.toJSON();
			lpObj.getAsJsonObject().add("verliehenDurch", lp.getVerliehenDurch().toJSON());

			lpListObj.add(lpObj);
		}
		result.add("lpListe", lpListObj);
		result.addProperty("rang", rank.getName());
		result.addProperty("lpBeiNpc", edituser.getLoyalitaetspunkteTotalBeiNpc(user));

		return result;
	}

	/**
	 * Zeigt die GUI fuer die Vergabe von Raengen und Orden an.
	 *
	 */
	@Action(ActionType.AJAX)
	public JsonElement raengeMenuAction(@UrlParam(name="edituser") String edituserID) {
		User user = (User)this.getUser();

		User edituser = User.lookupByIdentifier(edituserID);
		
		JsonObject result = new JsonObject();
		fillCommonMenuResultData(result);

		if( edituser == null ) {
			return result;
		}

		result.add("user", edituser.toJSON());

		UserRank rank = edituser.getRank(user);
		result.addProperty("aktiverRang", rank.getRank());
		
		JsonArray raengeObj = new JsonArray();
		for( Rang rang : user.getOwnGrantableRanks() )
		{
			JsonObject rangObj = new JsonObject();
			rangObj.addProperty("id", rang.getId());
			if( rang.getId() == 0 )
			{
				rangObj.addProperty("name", "-");
			}
			else
			{
				rangObj.addProperty("name", rang.getName());
			}
			raengeObj.add(rangObj);
		}
		
		result.add("raenge", raengeObj);

		JsonArray medalsObj = new JsonArray();
		for( Medal medal : Medals.get().medals().values() ) {
			if( medal.isAdminOnly() ) {
				continue;
			}
			
			medalsObj.add(medal.toJSON());
		}
		result.add("medals", medalsObj);
		
		return result;
	}

	/**
	 * Setzt die Order-Koordinaten, an denen georderte Objekte erscheinen sollen.
	 *
	 * @param lieferposition Die Koordinate des Ortes, an dem die georderten Objekte erscheinen sollen
	 */
	@Action(ActionType.AJAX)
	public JsonElement changeOrderLocationAction(String lieferposition) {
		User user = (User)this.getUser();

		if( lieferposition.isEmpty() )
		{
			user.setNpcOrderLocation(null);
			return JSONUtils.success("Lieferkoordinaten zurückgesetzt");
		}
		
		Location loc = Location.fromString(lieferposition);

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
	public JsonElement orderShipsAction(
			@UrlParam(name="shipflag_disableiff") boolean flagDisableIff,
			@UrlParam(name="shipflag_handelsposten") boolean flagHandelsposten,
			@UrlParam(name="shipflag_nichtkaperbar") boolean flagNichtKaperbar,
			@UrlParam(name="ship#_count") Map<Integer,Integer> shipCounts)
	{
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();

		int costs = 0;

		List<Order> orderList = new ArrayList<>();

		List<?> shipOrders = db
				.createQuery("from OrderableShip s order by s.shipType.shipClass,s.shipType.id")
				.list();
		for (Object shipOrder : shipOrders)
		{
			OrderableShip ship = (OrderableShip) shipOrder;

			Integer count = shipCounts.get(ship.getShipType().getId());
			if (count != null && count > 0)
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
	 * @param order Das zu ordernde Objekt (negativ: offizier)
	 * @param count Die Menge der zu ordernden Objekte
	 *
	 */
	@Action(ActionType.AJAX)
	public JsonElement orderAction(int order, int count) {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();

		int costs;

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

		JsonObject result = new JsonObject();
		result.addProperty("success", false);
		
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
	public JsonElement orderMenuAction() {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();

		Map<Integer,Integer> shiporders = new HashMap<>();
		Map<Integer,Integer> offiorders = new HashMap<>();

		JsonObject result = new JsonObject();
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

		JsonArray shipResultList = new JsonArray();

		List<?> shipOrders = db.createQuery("from OrderableShip order by shipType.shipClass,shipType.id").list();
		for (Object shipOrder : shipOrders)
		{
			OrderableShip ship = (OrderableShip) shipOrder;

			if (!Rassen.get().rasse(user.getRace()).isMemberIn(ship.getRasse()))
			{
				continue;
			}

			JsonObject resShip = new JsonObject();

			resShip.addProperty("klasse", ship.getShipType().getShipClass().getSingular());

			if (!shiporders.containsKey(ship.getId()))
			{
				shiporders.put(ship.getId(), 0);
			}

			resShip.addProperty("id", ship.getShipType().getId());
			resShip.addProperty("name", ship.getShipType().getNickname());
			resShip.addProperty("type", ship.getShipType().getId());
			resShip.addProperty("cost", ship.getCost());
			resShip.addProperty("ordercount", shiporders.get(ship.getId()));

			shipResultList.add(resShip);
		}
		
		result.add("ships", shipResultList);

		/*
			Offiziere
		*/

		JsonArray resOffizierList = new JsonArray();

		List<?> offizierOrders = db.createQuery("from OrderableOffizier where cost > 0 order by id").list();
		for (Object offizierOrder : offizierOrders)
		{
			OrderableOffizier offizier = (OrderableOffizier) offizierOrder;

			if (!offiorders.containsKey(-offizier.getId()))
			{
				offiorders.put(-offizier.getId(), 0);
			}

			JsonObject resOffizier = new JsonObject();
			resOffizier.addProperty("name", offizier.getName());
			resOffizier.addProperty("rang", offizier.getRang());
			resOffizier.addProperty("cost", offizier.getCost());
			resOffizier.addProperty("id", -offizier.getId());
			resOffizier.addProperty("ordercount", offiorders.get(offizier.getId()));

			resOffizierList.add(resOffizier);
		}

		result.add("offiziere", resOffizierList);
		result.addProperty("npcpunkte", user.getNpcPunkte());
		
		outputLieferposition(result, user);
		
		return result;
	}

	private void fillCommonMenuResultData(JsonObject result)
	{
		JsonObject menuObj = new JsonObject();
		menuObj.addProperty("head", this.isHead);
		menuObj.addProperty("shop", this.shop);
		
		result.add("menu", menuObj);
	}

	private void outputLieferposition(JsonObject result, User user)
	{
		Set<Location> uniqueLocations = new HashSet<>();

		Location lieferpos = null;
		if( user.getNpcOrderLocation() != null )
		{
			lieferpos = Location.fromString(user.getNpcOrderLocation());
		}
		
		JsonArray resLieferPos = new JsonArray();
		for( Base base : user.getBases() )
		{
			// Jede Position nur einmal auflisten!
			if( !uniqueLocations.add(base.getLocation()) )
			{
				continue;
			}
			JsonObject resPos = new JsonObject();
			resPos.addProperty("pos", base.getLocation().asString());
			resPos.addProperty("name", Common._plaintitle(base.getName()));
			
			resLieferPos.add(resPos);
		}
		
		result.add("lieferpositionen", resLieferPos);
		result.addProperty("aktuelleLieferposition", lieferpos != null ? lieferpos.asString() : null);
	}
}
