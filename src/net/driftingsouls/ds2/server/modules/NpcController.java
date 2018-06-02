package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Loyalitaetspunkte;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.UserRank;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FactionShopOrder;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionAktionsMeldung;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.services.FraktionsGuiEintragService;
import net.driftingsouls.ds2.server.entities.npcorders.Order;
import net.driftingsouls.ds2.server.entities.npcorders.OrderOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewMessage;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.modules.viewmodels.FraktionAktionsMeldungViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.LoyalitaetspunkteViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.MedalViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.RangViewModel;
import net.driftingsouls.ds2.server.modules.viewmodels.UserViewModel;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Das Interface fuer NPCs.
 *
 * @author Christopher Jung
 */
@Module(name = "npc")
public class NpcController extends Controller
{
	private FraktionsGuiEintragService fraktionsGuiEintragService;
	private boolean isHead = false;
	private boolean shop = false;

	/**
	 * Konstruktor.
	 */
	@Autowired
	public NpcController(FraktionsGuiEintragService fraktionsGuiEintragService)
	{
		this.fraktionsGuiEintragService = fraktionsGuiEintragService;
	}

	@Override
	protected boolean validateAndPrepare()
	{
		User user = (User) this.getUser();

		if (!user.hasFlag(UserFlag.ORDER_MENU))
		{
			throw new ValidierungException("Nur NPCs können dieses Script nutzen", Common.buildUrl("default", "module", "ueber"));
		}

		if (Rassen.get().rasse(user.getRace()).isHead(user))
		{
			this.isHead = true;
		}

		FraktionsGuiEintrag fraktionsGuiEintrag = fraktionsGuiEintragService.findeNachUser(user);
		if (fraktionsGuiEintrag != null && fraktionsGuiEintrag.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			this.shop = true;
		}

		return true;
	}

	public static class NpcMenuViewModel
	{
		public boolean head;
		public boolean shop;
	}

	public static abstract class NpcViewModel
	{
		public NpcMenuViewModel menu;
	}

	private void fillCommonMenuResultData(NpcViewModel result)
	{
		result.menu = new NpcMenuViewModel();
		result.menu.head = this.isHead;
		result.menu.shop = this.shop;
	}

	@ViewModel
	public static class ShopMenuViewModel extends NpcViewModel
	{
		public static class ShipViewModel
		{
			public int id;
			public String name;
			public String picture;
		}

		public static class TransporterViewModel
		{
			public String auftrag;
			public String status;
			public ShipViewModel ship;
		}

		public List<TransporterViewModel> transporter = new ArrayList<>();
	}

	/**
	 * Zeigt den aktuellen Status aller fuer Ganymede-Transporte reservierten Transporter an.
	 */
	@Action(ActionType.AJAX)
	public ShopMenuViewModel shopMenuAction()
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		if (!this.shop)
		{
			throw new ValidierungException("Sie verfügen über keinen Shop und können daher diese Seite nicht aufrufen");
		}

		ShopMenuViewModel result = new ShopMenuViewModel();
		fillCommonMenuResultData(result);

		List<?> ships = db.createQuery("from Ship s where s.owner=:user and locate('#!/tm gany_transport',s.einstellungen.destcom)!=0")
						.setEntity("user", user)
						.list();
		for (Object ship : ships)
		{
			Ship aship = (Ship) ship;
			ShipTypeData ashiptype = aship.getTypeData();

			ShopMenuViewModel.TransporterViewModel transObj = new ShopMenuViewModel.TransporterViewModel();
			transObj.status = "lageweile";
			transObj.auftrag = "-";

			ShopMenuViewModel.ShipViewModel shipObj = new ShopMenuViewModel.ShipViewModel();
			shipObj.id = aship.getId();
			shipObj.name = aship.getName();
			shipObj.picture = ashiptype.getPicture();

			transObj.ship = shipObj;

			Taskmanager taskmanager = Taskmanager.getInstance();
			Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.GANY_TRANSPORT, "*", Integer.toString(aship.getId()), "*");
			if (tasks.length == 0)
			{
				continue;
			}

			Task task = tasks[0];

			String status;
			switch (task.getData3()) {
				case "1":
					status = "verschiebt gany";
					break;
				case "2":
					status = "rückflug";
					break;
				default:
					status = "anreise";
					break;
			}

			transObj.status = status;

			FactionShopOrder order = (FactionShopOrder) db.get(FactionShopOrder.class, Integer.parseInt(task.getData1()));

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

			transObj.auftrag = order.getId() + ": " + Common._title(orderuser.getName()) + "\n" + order.getAddData();

			result.transporter.add(transObj);
		}

		return result;
	}

	/**
	 * Zeichnet einen Spieler mit einem Orden aus.
	 *
	 * @param edituserID Die ID des zu bearbeitenden Spielers
	 * @param medal Die ID des Ordens
	 * @param reason Der Grund, warum der Orden verliehen wurde
	 */
	@Action(ActionType.AJAX)
	public ViewMessage awardMedalAction(@UrlParam(name = "edituser") String edituserID, Medal medal, String reason)
	{
		User user = (User) this.getUser();

		if (!this.isHead)
		{
			return ViewMessage.failure("Sie sind nicht berechtigt auf dieses Menü zuzugreifen");
		}

		User edituser = User.lookupByIdentifier(edituserID);

		if (edituser == null)
		{
			return ViewMessage.failure("Der angegebene Spieler existiert nicht");
		}

		if (medal == null)
		{
			return ViewMessage.failure("Der angegebene Orden ist nicht vorhanden");
		}

		if (medal.isAdminOnly())
		{
			return ViewMessage.failure("Diesen Orden kännen sie nicht verleihen");
		}

		if (reason.length() == 0)
		{
			return ViewMessage.failure("Sie müssen einen Grund angeben");
		}

		Set<Medal> medallist = edituser.getMedals();
		medallist.add(medal);
		edituser.setMedals(medallist);

		int ticks = getContext().get(ContextCommon.class).getTick();

		edituser.addHistory(Common.getIngameTime(ticks) + ": Der Orden [medal]"+medal.getId()+"[/medal]" +
							" wurde von [userprofile=" + user.getId() + "]" +
							user.getName() + "[/userprofile] verliehen Aufgrund der " + reason);

		PM.send(user, edituser.getId(), "Orden '" + medal.getName() + "' verliehen",
			   "Ich habe dir den Orden [medal]" + medal.getId() + "[/medal]" +
			   " verliehen Aufgrund deiner " + reason);

		return ViewMessage.success("Dem Spieler wurde der Orden '" +
								   medal.getName() +
								   "' verliehen");
	}

	/**
	 * Befoerdert/Degradiert einen Spieler.
	 *
	 * @param edituserID Die ID des zu bearbeitenden Spielers
	 * @param rank Der neue Rang
	 */
	@Action(ActionType.AJAX)
	public ViewMessage changeRankAction(@UrlParam(name = "edituser") String edituserID, int rank)
	{
		User user = (User) this.getUser();

		User edituser = User.lookupByIdentifier(edituserID);

		if (edituser == null)
		{
			return ViewMessage.failure("Der angegebene Spieler existiert nicht");
		}

		if (rank < 0)
		{
			return ViewMessage.failure("Sie können diesen Spieler nicht soweit degradieren");
		}

		edituser.setRank(user, rank);

		return ViewMessage.success("Rang geändert");
	}

	@Action(ActionType.AJAX)
	public ViewMessage deleteLpAction(@UrlParam(name = "edituser") String edituserID, @UrlParam(name = "lp") int lpId)
	{
		User edituser = User.lookupByIdentifier(edituserID);
		if (edituser == null)
		{
			return ViewMessage.failure("Der Spieler existiert nicht");
		}

		Loyalitaetspunkte lp = null;
		for (Loyalitaetspunkte alp : edituser.getLoyalitaetspunkte())
		{
			if (alp.getId() == lpId)
			{
				lp = alp;
				break;
			}
		}

		if (lp == null)
		{
			return ViewMessage.failure("Der LP-Eintrag wurde nicht gefunden");
		}

		org.hibernate.Session db = getDB();

		db.delete(lp);
		edituser.getLoyalitaetspunkte().remove(lp);

		return ViewMessage.success("Eintrag gelöscht");
	}

	/**
	 * Fuegt LP zu einem Spieler hinzu.
	 *
	 * @param anmerkungen Weitere Anmerkungen zur Vergabe der LP
	 * @param edituserID Die Identifikationsdaten des zu bearbeitenden Spielers
	 * @param grund Der Grund fuer die LP
	 * @param pm <code>true</code> falls eine PM an den Spieler versendet werden soll
	 * @param punkte Die Anzahl der LP
	 * @return Das Antwortobjekt
	 */
	@Action(ActionType.AJAX)
	public ViewMessage editLpAction(@UrlParam(name = "edituser") String edituserID, String grund, String anmerkungen, int punkte, boolean pm)
	{
		User user = (User) this.getUser();

		User edituser = User.lookupByIdentifier(edituserID);
		if (edituser == null)
		{
			return ViewMessage.failure("Benutzer nicht gefunden");
		}

		if (punkte == 0 || grund.isEmpty())
		{
			return ViewMessage.failure("Sie muessen den Grund und die Anzahl der Punkte angeben");
		}

		org.hibernate.Session db = getDB();

		Loyalitaetspunkte lp = new Loyalitaetspunkte(edituser, user, grund, punkte);
		lp.setAnmerkungen(anmerkungen);
		edituser.getLoyalitaetspunkte().add(lp);

		db.persist(lp);

		if (pm)
		{
			String pmText = "[Automatische Mitteilung]\n" +
							"Du hast soeben " + punkte + " Loyalitätspunkte erhalten. " +
							"Du verfügst nun insgesamt über " + edituser.getLoyalitaetspunkteTotalBeiNpc(user) + " Loyalitätspunkte bei mir.\n\n";
			pmText += "Grund für die Vergabe: " + grund;
			PM.send(user, edituser.getId(), "Loyalitätspunkte erhalten", pmText, PM.FLAGS_AUTOMATIC);
		}

		return ViewMessage.success(punkte + " LP vergeben");
	}

	/**
	 * Markiert die Meldung einer Aktion als "bearbeitet".
	 *
	 * @param meldung Die Meldung
	 * @return Die JSON-Antwort
	 */
	@Action(ActionType.AJAX)
	public ViewMessage meldungBearbeitetAction(FraktionAktionsMeldung meldung)
	{
		if (meldung == null)
		{
			return ViewMessage.error("Die angegebene Meldung konnte nicht gefunden werden");
		}
		meldung.setBearbeitetAm(new Date());
		return ViewMessage.success("Die Meldung wurde als bearbeitet markiert");
	}

	@ViewModel
	public static class LpMenuViewModel extends NpcViewModel
	{
		public static class LpMenuLoyalitaetspunkteViewModel extends LoyalitaetspunkteViewModel
		{
			public UserViewModel verliehenDurch;
		}

		public boolean alleMeldungen;
		public List<FraktionAktionsMeldungViewModel> meldungen = new ArrayList<>();
		public UserViewModel user;
		public List<LpMenuLoyalitaetspunkteViewModel> lpListe = new ArrayList<>();
		public String rang;
		public int lpBeiNpc;
	}

	/**
	 * Zeigt die GUI fuer LP-Verwaltung an.
	 *
	 * @param edituserID Die Identifikationsdaten des anzuzeigenden Spielers
	 * @param alleMeldungen <code>true</code>, falls alle Meldungen angezeigt werden sollen
	 */
	@Action(ActionType.AJAX)
	public LpMenuViewModel lpMenuAction(@UrlParam(name = "edituser") String edituserID, boolean alleMeldungen)
	{
		User user = (User) this.getUser();

		LpMenuViewModel result = new LpMenuViewModel();
		fillCommonMenuResultData(result);
		result.alleMeldungen = alleMeldungen;

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -14);

		List<FraktionAktionsMeldung> meldungen;
		if (alleMeldungen)
		{
			meldungen = Common.cast(getDB()
									.createQuery("from FraktionAktionsMeldung where bearbeitetAm is null or bearbeitetAm>:maxBearbeitet order by bearbeitetAm,gemeldetAm")
									.setDate("maxBearbeitet", cal.getTime())
									.list());
		}
		else
		{
			meldungen = Common.cast(getDB()
									.createQuery("from FraktionAktionsMeldung where fraktion=:user and (bearbeitetAm is null or bearbeitetAm>:maxBearbeitet) order by bearbeitetAm,gemeldetAm")
									.setEntity("user", user)
									.setDate("maxBearbeitet", cal.getTime())
									.list());
		}

		result.meldungen.addAll(meldungen.stream().map(FraktionAktionsMeldungViewModel::map).collect(Collectors.toList()));

		User edituser = User.lookupByIdentifier(edituserID);

		if (edituser == null)
		{
			return result;
		}

		result.user = UserViewModel.map(edituser);

		UserRank rank = edituser.getRank(user);

		//DateFormat format = new SimpleDateFormat("dd.MM.yy HH:mm");

		for (Loyalitaetspunkte lp : new TreeSet<>(edituser.getLoyalitaetspunkte()))
		{
			LpMenuViewModel.LpMenuLoyalitaetspunkteViewModel lpObj = new LpMenuViewModel.LpMenuLoyalitaetspunkteViewModel();
			LoyalitaetspunkteViewModel.map(lp, lpObj);
			lpObj.verliehenDurch = UserViewModel.map(lp.getVerliehenDurch());

			result.lpListe.add(lpObj);
		}
		result.rang = rank.getName();
		result.lpBeiNpc = edituser.getLoyalitaetspunkteTotalBeiNpc(user);

		return result;
	}

	@ViewModel
	public static class RaengeMenuViewModel extends NpcViewModel
	{
		public UserViewModel user;
		public int aktiverRang;
		public List<RangViewModel> raenge = new ArrayList<>();
		public List<MedalViewModel> medals = new ArrayList<>();
	}

	/**
	 * Zeigt die GUI fuer die Vergabe von Raengen und Orden an.
	 */
	@Action(ActionType.AJAX)
	public RaengeMenuViewModel raengeMenuAction(@UrlParam(name = "edituser") String edituserID)
	{
		User user = (User) this.getUser();

		User edituser = User.lookupByIdentifier(edituserID);

		RaengeMenuViewModel result = new RaengeMenuViewModel();
		fillCommonMenuResultData(result);

		if (edituser == null)
		{
			return result;
		}

		result.user = UserViewModel.map(edituser);

		UserRank rank = edituser.getRank(user);
		result.aktiverRang = rank.getRank();

		result.raenge.addAll(user.getOwnGrantableRanks().stream().map(RangViewModel::map).collect(Collectors.toList()));

		for (Medal medal : Medals.get().medals())
		{
			if (medal.isAdminOnly())
			{
				continue;
			}

			result.medals.add(MedalViewModel.map(medal));
		}

		return result;
	}

	/**
	 * Setzt die Order-Koordinaten, an denen georderte Objekte erscheinen sollen.
	 *
	 * @param lieferposition Die Koordinate des Ortes, an dem die georderten Objekte erscheinen sollen
	 */
	@Action(ActionType.AJAX)
	public ViewMessage changeOrderLocationAction(String lieferposition)
	{
		User user = (User) this.getUser();

		if (lieferposition.isEmpty())
		{
			user.setNpcOrderLocation(null);
			return ViewMessage.success("Lieferkoordinaten zurückgesetzt");
		}

		Location loc = Location.fromString(lieferposition);

		if (!Base.byLocationAndBesitzer(loc, user).isEmpty())
		{
			user.setNpcOrderLocation(loc.asString());

			return ViewMessage.success("Neue Lieferkoordinaten gespeichert");
		}

		return ViewMessage.failure("Keine Lieferung nach " + loc.asString() + " möglich");
	}


	/**
	 * Ordert eine Menge von Schiffen.
	 */
	@Action(ActionType.AJAX)
	public ViewMessage orderShipsAction(
									   @UrlParam(name = "shipflag_disableiff") boolean flagDisableIff,
									   @UrlParam(name = "shipflag_handelsposten") boolean flagHandelsposten,
									   @UrlParam(name = "shipflag_nichtkaperbar") boolean flagNichtKaperbar,
									   @UrlParam(name = "ship#_count") Map<OrderableShip, Integer> shipCounts)
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		int costs = 0;

		List<Order> orderList = new ArrayList<>();

		for (Map.Entry<OrderableShip, Integer> entry : shipCounts.entrySet())
		{
			if( entry.getValue() == null || entry.getValue() <= 0 )
			{
				continue;
			}
			int count = entry.getValue();
			OrderableShip ship = entry.getKey();
			costs += count * ship.getCost();

			for (int i = 0; i < count; i++)
			{
				OrderShip orderObj = new OrderShip(user, ship.getShipType());
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

		if (costs > 0)
		{
			if (user.getNpcPunkte() < costs)
			{
				return ViewMessage.failure("Nicht genug Kommandopunkte");
			}

			orderList.forEach(db::persist);

			user.setNpcPunkte(user.getNpcPunkte() - costs);

			return ViewMessage.success(orderList.size() + " Schiff(e) zugeteilt - wird/werden in 1 Tick(s) eintreffen");
		}

		return ViewMessage.failure("Sorry, aber umsonst bekommst du hier nichts...");
	}

	/**
	 * Ordert eine Menge von Offizieren.
	 *
	 * @param order Der zu ordernde Offizier
	 * @param count Die Menge
	 */
	@Action(ActionType.AJAX)
	public ViewMessage orderAction(OrderableOffizier order, int count)
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		int costs;

		if (count <= 0)
		{
			count = 1;
		}

		if( order == null )
		{
			return ViewMessage.failure("Es gibt keinen solchen Offizier");
		}

		costs = count * order.getCost();
		if (costs > 0)
		{
			if (user.getNpcPunkte() < costs)
			{
				return ViewMessage.failure("Nicht genug Kommandopunkte");
			}
			for (int i = 0; i < count; i++)
			{
				Order orderObj = new OrderOffizier(user, order.getId());
				orderObj.setTick(1);
				db.persist(orderObj);
			}

			user.setNpcPunkte(user.getNpcPunkte() - costs);
			return ViewMessage.success("Offizier(e) zugeteilt - wird/werden in 1 Tick(s) eintreffen");
		}

		return ViewMessage.failure("Sorry, aber umsonst bekommst du hier nichts...");
	}

	@ViewModel
	public static class OrderMenuViewModel extends NpcViewModel
	{
		public static class OrderableShipViewModel
		{
			public String klasse;
			public int id;
			public String name;
			public int type;
			public int cost;
			public Integer ordercount;
		}

		public static class OrderableOffizierViewModel
		{
			public int id;
			public String name;
			public int rang;
			public int cost;
			public Integer ordercount;
		}

		public static class LieferpositionViewModel
		{
			public String name;
			public String pos;
		}

		public int npcpunkte;
		public String aktuelleLieferposition;
		public List<OrderableOffizierViewModel> offiziere = new ArrayList<>();
		public List<OrderableShipViewModel> ships = new ArrayList<>();
		public List<LieferpositionViewModel> lieferpositionen = new ArrayList<>();
	}

	/**
	 * Zeigt die GUI zum Ordern von Schiffen/Offizieren.
	 */
	@Action(ActionType.AJAX)
	public OrderMenuViewModel orderMenuAction()
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		Map<ShipType, Integer> shiporders = new HashMap<>();
		Map<Integer, Integer> offiorders = new HashMap<>();

		OrderMenuViewModel result = new OrderMenuViewModel();
		fillCommonMenuResultData(result);

		List<?> orderList = db.createQuery("from Order where user= :user")
							.setInteger("user", user.getId())
							.list();
		for (Object anOrderList : orderList)
		{
			Order order = (Order) anOrderList;
			if (order instanceof OrderShip)
			{
				Common.safeIntInc(shiporders, ((OrderShip) order).getShipType());
			}
			else if (order instanceof OrderOffizier)
			{
				Common.safeIntInc(offiorders, ((OrderOffizier) order).getType());
			}
		}

		/*
			Schiffe
		*/

		List<?> shipOrders = db.createQuery("from OrderableShip order by shipType.shipClass,shipType.id").list();
		for (Object shipOrder : shipOrders)
		{
			OrderableShip ship = (OrderableShip) shipOrder;

			if (!Rassen.get().rasse(user.getRace()).isMemberIn(ship.getRasse()))
			{
				continue;
			}

			OrderMenuViewModel.OrderableShipViewModel resShip = new OrderMenuViewModel.OrderableShipViewModel();

			resShip.klasse = ship.getShipType().getShipClass().getSingular();

			shiporders.putIfAbsent(ship.getShipType(), 0);
			resShip.id = ship.getId();
			resShip.name = ship.getShipType().getNickname();
			resShip.type = ship.getShipType().getId();
			resShip.cost = ship.getCost();
			resShip.ordercount = shiporders.get(ship.getShipType());

			result.ships.add(resShip);
		}

		/*
			Offiziere
		*/

		List<?> offizierOrders = db.createQuery("from OrderableOffizier where cost > 0 order by id").list();
		for (Object offizierOrder : offizierOrders)
		{
			OrderableOffizier offizier = (OrderableOffizier) offizierOrder;

			offiorders.putIfAbsent(-offizier.getId(), 0);

			OrderMenuViewModel.OrderableOffizierViewModel resOffizier = new OrderMenuViewModel.OrderableOffizierViewModel();
			resOffizier.name = offizier.getName();
			resOffizier.rang = offizier.getRang();
			resOffizier.cost = offizier.getCost();
			resOffizier.id = offizier.getId();
			resOffizier.ordercount = offiorders.get(offizier.getId());

			result.offiziere.add(resOffizier);
		}

		result.npcpunkte = user.getNpcPunkte();

		outputLieferposition(result, user);

		return result;
	}


	private void outputLieferposition(OrderMenuViewModel result, User user)
	{
		Set<Location> uniqueLocations = new HashSet<>();

		Location lieferpos = null;
		if (user.getNpcOrderLocation() != null)
		{
			lieferpos = Location.fromString(user.getNpcOrderLocation());
		}

		for (Base base : user.getBases())
		{
			// Jede Position nur einmal auflisten!
			if (!uniqueLocations.add(base.getLocation()))
			{
				continue;
			}
			OrderMenuViewModel.LieferpositionViewModel resPos = new OrderMenuViewModel.LieferpositionViewModel();
			resPos.pos = base.getLocation().asString();
			resPos.name = Common._plaintitle(base.getName());

			result.lieferpositionen.add(resPos);
		}

		result.aktuelleLieferposition = lieferpos != null ? lieferpos.asString() : null;
	}
}
