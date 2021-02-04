package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.services.AllianzService;
import net.driftingsouls.ds2.server.services.MedalService;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Loyalitaetspunkte;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.UserRank;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FactionShopOrder;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionAktionsMeldung;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.framework.ConfigService;
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
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.services.UserService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
	private boolean isHead = false;
	private boolean shop = false;

	@PersistenceContext
	private EntityManager em;

	private final FraktionsGuiEintragService fraktionsGuiEintragService;
	private final Rassen races;
	private final UserService userService;
	private final ConfigService configService;
	private final PmService pmService;
	private final BBCodeParser bbCodeParser;
	private final MedalService medalService;
	private final AllianzService allyService;
	private final TaskManager taskManager;

	/**
	 * Konstruktor.
	 */
	@Autowired
	public NpcController(FraktionsGuiEintragService fraktionsGuiEintragService, Rassen races, UserService userService, ConfigService configService, PmService pmService, MedalService medalService, BBCodeParser bbCodeParser, AllianzService allyService, TaskManager taskManager)
	{
		this.fraktionsGuiEintragService = fraktionsGuiEintragService;
		this.races = races;
		this.userService = userService;
		this.configService = configService;
		this.pmService = pmService;
		this.medalService = medalService;
		this.bbCodeParser = bbCodeParser;
		this.allyService = allyService;
		this.taskManager = taskManager;
	}

	@Override
	protected boolean validateAndPrepare()
	{
		User user = (User) this.getUser();

		if (!user.hasFlag(UserFlag.ORDER_MENU))
		{
			throw new ValidierungException("Nur NPCs können dieses Script nutzen.", Common.buildUrl("default", "module", "ueber"));
		}

		if (races.rasse(user.getRace()).isHead(user))
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

		public final List<TransporterViewModel> transporter = new ArrayList<>();
	}

	/**
	 * Zeigt den aktuellen Status aller fuer Ganymede-Transporte reservierten Transporter an.
	 */
	@Action(ActionType.AJAX)
	public ShopMenuViewModel shopMenuAction()
	{
		User user = (User) this.getUser();

		if (!this.shop)
		{
			throw new ValidierungException("Sie verfügen über keinen Shop und können daher diese Seite nicht aufrufen.");
		}

		ShopMenuViewModel result = new ShopMenuViewModel();
		fillCommonMenuResultData(result);

		List<Ship> ships = em.createQuery("from Ship s where s.owner=:user and locate('#!/tm gany_transport',s.einstellungen.destcom)!=0", Ship.class)
						.setParameter("user", user)
						.getResultList();
		for (Ship ship: ships)
		{
			ShipTypeData ashiptype = ship.getTypeData();

			ShopMenuViewModel.TransporterViewModel transObj = new ShopMenuViewModel.TransporterViewModel();
			transObj.status = "lageweile";
			transObj.auftrag = "-";

			ShopMenuViewModel.ShipViewModel shipObj = new ShopMenuViewModel.ShipViewModel();
			shipObj.id = ship.getId();
			shipObj.name = ship.getName();
			shipObj.picture = ashiptype.getPicture();

			transObj.ship = shipObj;

			Task[] tasks = taskManager.getTasksByData(TaskManager.Types.GANY_TRANSPORT, "*", Integer.toString(ship.getId()), "*");
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

			FactionShopOrder order = em.find(FactionShopOrder.class, Integer.parseInt(task.getData1()));

			if (order == null)
			{
				em.remove(task);
				continue;
			}

			User orderuser = order.getUser();

			if (orderuser == null)
			{
				orderuser = new User();
				orderuser.setName("deleted user");
				orderuser.setPlainname("deleted user");
			}

			transObj.auftrag = order.getId() + ": " + Common._title(bbCodeParser, orderuser.getName()) + "\n" + order.getAddData();

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

		User edituser = userService.lookupByIdentifier(edituserID);

		if (edituser == null)
		{
			return ViewMessage.failure("Der angegebene Spieler existiert nicht.");
		}

		if (medal == null)
		{
			return ViewMessage.failure("Der angegebene Orden ist nicht vorhanden.");
		}

		if (medal.isAdminOnly())
		{
			return ViewMessage.failure("Diesen Orden kännen sie nicht verleihen.");
		}

		if (reason.length() == 0)
		{
			return ViewMessage.failure("Sie müssen einen Grund angeben.");
		}

		Set<Medal> medallist = userService.getMedals(user);
		medallist.add(medal);
		edituser.setMedals(medallist);

		int ticks = configService.getValue(WellKnownConfigValue.TICKS);

		edituser.addHistory(Common.getIngameTime(ticks) + ": Der Orden [medal]"+medal.getId()+"[/medal]" +
							" wurde von [userprofile=" + user.getId() + "]" +
							user.getName() + "[/userprofile] verliehen Aufgrund der " + reason);

		pmService.send(user, edituser.getId(), "Orden '" + medal.getName() + "' verliehen",
			   "Ich habe Dir den Orden [medal]" + medal.getId() + "[/medal]" +
			   " verliehen Aufgrund deiner " + reason);

		return ViewMessage.success("Dem Spieler wurde der Orden '" +
								   medal.getName() +
								   "' verliehen.");
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

		User edituser = userService.lookupByIdentifier(edituserID);

		if (edituser == null)
		{
			return ViewMessage.failure("Der angegebene Spieler existiert nicht.");
		}

		if (rank < 0)
		{
			return ViewMessage.failure("Sie können diesen Spieler nicht so weit degradieren.");
		}

		edituser.setRank(user, rank);

		return ViewMessage.success("Rang geändert");
	}

	@Action(ActionType.AJAX)
	public ViewMessage deleteLpAction(@UrlParam(name = "edituser") String edituserID, @UrlParam(name = "lp") int lpId)
	{
		User edituser = userService.lookupByIdentifier(edituserID);
		if (edituser == null)
		{
			return ViewMessage.failure("Der Spieler existiert nicht.");
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
			return ViewMessage.failure("Der LP-Eintrag wurde nicht gefunden.");
		}

		em.remove(lp);
		edituser.getLoyalitaetspunkte().remove(lp);

		return ViewMessage.success("Eintrag gelöscht!");
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

		User edituser = userService.lookupByIdentifier(edituserID);
		if (edituser == null)
		{
			return ViewMessage.failure("Benutzer nicht gefunden.");
		}

		if (punkte == 0 || grund.isEmpty())
		{
			return ViewMessage.failure("Sie müssen den Grund und die Anzahl der Punkte angeben.");
		}

		Loyalitaetspunkte lp = new Loyalitaetspunkte(edituser, user, grund, punkte);
		lp.setAnmerkungen(anmerkungen);
		edituser.getLoyalitaetspunkte().add(lp);

		em.persist(lp);

		if (pm)
		{
			String pmText = "[Automatische Mitteilung]\n" +
							"Du hast soeben " + punkte + " Loyalitätspunkte erhalten. " +
							"Du verfügst nun insgesamt über " + edituser.getLoyalitaetspunkteTotalBeiNpc(user) + " Loyalitätspunkte bei mir.\n\n";
			pmText += "Grund für die Vergabe: " + grund;
			pmService.send(user, edituser.getId(), "Loyalitätspunkte erhalten", pmText, PM.FLAGS_AUTOMATIC);
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
			return ViewMessage.error("Die angegebene Meldung konnte nicht gefunden werden.");
		}
		meldung.setBearbeitetAm(new Date());
		return ViewMessage.success("Die Meldung wurde als bearbeitet markiert.");
	}

	@ViewModel
	public static class LpMenuViewModel extends NpcViewModel
	{
		public static class LpMenuLoyalitaetspunkteViewModel extends LoyalitaetspunkteViewModel
		{
			public UserViewModel verliehenDurch;
		}

		public boolean alleMeldungen;
		public final List<FraktionAktionsMeldungViewModel> meldungen = new ArrayList<>();
		public UserViewModel user;
		public final List<LpMenuLoyalitaetspunkteViewModel> lpListe = new ArrayList<>();
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
			meldungen = em.createQuery("from FraktionAktionsMeldung where bearbeitetAm is null or bearbeitetAm>:maxBearbeitet order by bearbeitetAm,gemeldetAm", FraktionAktionsMeldung.class)
							.setParameter("maxBearbeitet", cal.getTime())
							.getResultList();
		}
		else
		{
			meldungen = em.createQuery("from FraktionAktionsMeldung where fraktion=:user and (bearbeitetAm is null or bearbeitetAm>:maxBearbeitet) order by bearbeitetAm,gemeldetAm", FraktionAktionsMeldung.class)
							.setParameter("user", user)
							.setParameter("maxBearbeitet", cal.getTime())
							.getResultList();
		}

		result.meldungen.addAll(meldungen.stream()
			.map(meldung -> FraktionAktionsMeldungViewModel.map(bbCodeParser, meldung))
			.collect(Collectors.toList()));

		User edituser = userService.lookupByIdentifier(edituserID);

		if (edituser == null)
		{
			return result;
		}

		result.user = UserViewModel.map(bbCodeParser, edituser);

		UserRank rank = edituser.getRank(user);

		for (Loyalitaetspunkte lp : new TreeSet<>(edituser.getLoyalitaetspunkte()))
		{
			LpMenuViewModel.LpMenuLoyalitaetspunkteViewModel lpObj = new LpMenuViewModel.LpMenuLoyalitaetspunkteViewModel();
			LoyalitaetspunkteViewModel.map(lp, lpObj);
			lpObj.verliehenDurch = UserViewModel.map(bbCodeParser, lp.getVerliehenDurch());

			result.lpListe.add(lpObj);
		}
		result.rang = allyService.getRankName(rank);
		result.lpBeiNpc = edituser.getLoyalitaetspunkteTotalBeiNpc(user);

		return result;
	}

	@ViewModel
	public static class RaengeMenuViewModel extends NpcViewModel
	{
		public UserViewModel user;
		public int aktiverRang;
		public final List<RangViewModel> raenge = new ArrayList<>();
		public final List<MedalViewModel> medals = new ArrayList<>();
	}

	/**
	 * Zeigt die GUI fuer die Vergabe von Raengen und Orden an.
	 */
	@Action(ActionType.AJAX)
	public RaengeMenuViewModel raengeMenuAction(@UrlParam(name = "edituser") String edituserID)
	{
		User user = (User) this.getUser();

		User edituser = userService.lookupByIdentifier(edituserID);

		RaengeMenuViewModel result = new RaengeMenuViewModel();
		fillCommonMenuResultData(result);

		if (edituser == null)
		{
			return result;
		}

		result.user = UserViewModel.map(bbCodeParser, edituser);

		UserRank rank = edituser.getRank(user);
		result.aktiverRang = rank.getRank();

		result.raenge.addAll(userService.getOwnGrantableRanks(user).stream().map(RangViewModel::map).collect(Collectors.toList()));

		for (Medal medal : medalService.medals())
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
			return ViewMessage.success("Lieferkoordinaten zurückgesetzt.");
		}

		Location loc = Location.fromString(lieferposition);

		if (!Base.byLocationAndBesitzer(loc, user).isEmpty())
		{
			user.setNpcOrderLocation(loc.asString());

			return ViewMessage.success("Neue Lieferkoordinaten gespeichert.");
		}

		return ViewMessage.failure("Keine Lieferung nach " + loc.asString() + " möglich.");
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
				return ViewMessage.failure("Nicht genug Kommandopunkte!");
			}

			orderList.forEach(em::persist);

			user.setNpcPunkte(user.getNpcPunkte() - costs);

			return ViewMessage.success(orderList.size() + " Schiff(e) zugeteilt - wird/werden in 1 Tick(s) eintreffen.");
		}

		return ViewMessage.failure("Sorry, aber umsonst bekommst Du hier nichts...");
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
		User user = (User) this.getUser();

		int costs;

		if (count <= 0)
		{
			count = 1;
		}

		if( order == null )
		{
			return ViewMessage.failure("Es gibt keinen solchen Offizier.");
		}

		costs = count * order.getCost();
		if (costs > 0)
		{
			if (user.getNpcPunkte() < costs)
			{
				return ViewMessage.failure("Nicht genug Kommandopunkte!");
			}
			for (int i = 0; i < count; i++)
			{
				Order orderObj = new OrderOffizier(user, order.getId());
				orderObj.setTick(1);
				em.persist(orderObj);
			}

			user.setNpcPunkte(user.getNpcPunkte() - costs);
			return ViewMessage.success("Offizier(e) zugeteilt - wird/werden in 1 Tick(s) eintreffen.");
		}

		return ViewMessage.failure("Sorry, aber umsonst bekommst Du hier nichts...");
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
		public final List<OrderableOffizierViewModel> offiziere = new ArrayList<>();
		public final List<OrderableShipViewModel> ships = new ArrayList<>();
		public final List<LieferpositionViewModel> lieferpositionen = new ArrayList<>();
	}

	/**
	 * Zeigt die GUI zum Ordern von Schiffen/Offizieren.
	 */
	@Action(ActionType.AJAX)
	public OrderMenuViewModel orderMenuAction()
	{
		User user = (User) this.getUser();

		Map<ShipType, Integer> shiporders = new HashMap<>();
		Map<Integer, Integer> offiorders = new HashMap<>();

		OrderMenuViewModel result = new OrderMenuViewModel();
		fillCommonMenuResultData(result);

		List<Order> orders = em.createQuery("from Order where user= :user", Order.class)
							.setParameter("user", user.getId())
							.getResultList();
		for (Order order: orders)
		{
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

		List<OrderableShip> shipOrders = em.createQuery("from OrderableShip order by shipType.shipClass,shipType.id", OrderableShip.class).getResultList();
		for (OrderableShip ship: shipOrders)
		{
			if (!races.rasse(user.getRace()).isMemberIn(ship.getRasse()))
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

		List<OrderableOffizier> offizierOrders = em.createQuery("from OrderableOffizier where cost > 0 order by id", OrderableOffizier.class).getResultList();
		for (OrderableOffizier offizier : offizierOrders)
		{
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
