/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.modules.fraktionen;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.*;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.*;
import net.driftingsouls.ds2.server.entities.fraktionsgui.*;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeJob;
import net.driftingsouls.ds2.server.entities.fraktionsgui.baseupgrade.UpgradeType;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.ContextInstance;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.FraktionsGuiEintragService;
import net.driftingsouls.ds2.server.ships.JumpNodeRouter;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Zeigt die Fraktionsseiten an.
 *
 * @author Christopher Jung
 */
@Module(name = "ersteigern")
public class ErsteigernController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;
	private ConfigService configService;
	private FraktionsGuiEintragService fraktionsGuiEintragService;

	@Autowired
	public ErsteigernController(TemplateViewResultFactory templateViewResultFactory,
			ConfigService configService,
			FraktionsGuiEintragService fraktionsGuiEintragService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.configService = configService;
		this.fraktionsGuiEintragService = fraktionsGuiEintragService;

		setPageTitle("Fraktionen");
	}

	private FraktionsGuiEintrag ermittleFraktion(User factionUser)
	{
		User user = (User) getUser();

		if (factionUser == null)
		{
			FraktionsGuiEintrag eintrag = ermittleStandardFraktionFuerSpieler(user);
			if( eintrag != null )
			{
				factionUser = eintrag.getUser();
			}
			else
			{
				throw new ValidierungException("Keine Fraktion will mit ihnen zu handeln solange die Beziehungen feindlich sind");
			}
		}

		FraktionsGuiEintrag faction = fraktionsGuiEintragService.findeNachUser(factionUser);

		validiereFraktion(faction);

		return faction;
	}

	private void erstelleMenue(TemplateEngine t, FraktionsGuiEintrag faction)
	{
		User user = (User) getUser();
		// Die Templatevariablen duerfen nur einmal gesetzt werden (redirects!)
		if (t.getVar("global.faction").isEmpty())
		{
			for (FraktionsGuiEintrag.Seite aPage : faction.getSeiten())
			{
				t.setVar("faction." + aPage.getId(), 1);
			}

			// Fraktionsmenue

			t.setBlock("_ERSTEIGERN", "global.factionmenu.listitem", "global.factionmenu.list");

			List<FraktionsGuiEintrag> fraktionen = fraktionsGuiEintragService.findeAlle();
			for (FraktionsGuiEintrag eintrag : fraktionen)
			{
				t.setVar(
						"item.faction.name", Common._title(eintrag.getUser().getName()),
						"item.faction.id", eintrag.getUser().getId());

				t.parse("global.factionmenu.list", "global.factionmenu.listitem", true);
			}

			User factionuser = faction.getUser();

			t.setVar(
					"user.konto", Common.ln(user.getKonto()),
					"global.faction", faction.getUser().getId(),
					"global.faction.name", Common._title(factionuser.getName()));
		}
	}

	private void validiereFraktion(FraktionsGuiEintrag faction)
	{
		if (faction == null)
		{
			throw new ValidierungException("Die angegebene Fraktion verfügt über keine eigene Seite");
		}
	}

	private boolean istHandelErlaubt(User user, FraktionsGuiEintrag faction)
	{
		User factionUser = faction.getUser();
		return user.getRelation(factionUser) != User.Relation.ENEMY
				&& factionUser.getRelation(user) != User.Relation.ENEMY;
	}

	private FraktionsGuiEintrag ermittleStandardFraktionFuerSpieler(User user)
	{
		List<FraktionsGuiEintrag> fraktionen = fraktionsGuiEintragService.findeAlle();
		for (FraktionsGuiEintrag eintrag : fraktionen)
		{
			if (eintrag.getUser() == user)
			{
				return eintrag;
			}
		}
		for (FraktionsGuiEintrag eintrag : fraktionen)
		{
			if ((user.getRelation(eintrag.getUser()) != User.Relation.ENEMY)
					&& (eintrag.getUser().getRelation(user) != User.Relation.ENEMY))
			{
				return eintrag;
			}
		}

		return null;
	}

	/**
	 * Aendert das System, in dem ersteigerte Dinge gespawnt werden sollen.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param favsys Die ID des neuen Systems, in dem ersteigerte Dinge gespawnt werden sollen
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeDropZoneAction(User faction, StarSystem favsys)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.VERSTEIGERUNG))
		{
			return new RedirectViewResult("default");
		}

		User user = (User) getUser();

		String message = null;
		if (ermittleMoeglicheDropZones().contains(favsys))
		{
			user.setGtuDropZone(favsys.getID());
			message = "Neue Lieferkoordinate gespeichert";
		}

		return new RedirectViewResult("versteigerung").withMessage(message);
	}

	/**
	 * Gibt ein Gebot auf eine Versteigerung ab bzw zeigt, falls kein Gebot angegeben wurde, die
	 * angegebene Versteigerung an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param bid Der gebotene Betrag oder 0
	 * @param entry Die Auktion auf die geboten werden soll
	 */
	@Action(ActionType.DEFAULT)
	public Object bidEntryAction(User faction, int bid, @UrlParam(name = "auk") Versteigerung entry)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.VERSTEIGERUNG))
		{
			return new RedirectViewResult("default");
		}

		User user = (User) getUser();

		if (!istHandelErlaubt(user, factionObj))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);

		if (entry == null || (entry.getOwner().getId() == user.getId()))
		{
			addError("Sie können nicht bei eigenen Versteigerungen mitbieten");
			return new RedirectViewResult("default");
		}

		// Wenn noch kein Gebot abgegeben wurde -> Versteigerung anzeigen
		if (bid == 0)
		{
			erstelleMenue(t, factionObj);

			int entrywidth = entry.isObjectFixedImageSize() ? 50 : 0;
			long entrycount = entry.getObjectCount();
			String entryname = entry.getObjectName();
			String entryimage = entry.getObjectPicture();
			String entrylink = entry.getObjectUrl();

			String bietername = "";

			User bieter = entry.getBieter();

			if (bieter == factionObj.getUser())
			{
				bietername = bieter.getName();
			}
			else if (bieter.getId() == user.getId())
			{
				bietername = bieter.getName();
			}
			else if (hasPermission(WellKnownPermission.FRAKTIONEN_BIETERNAME))
			{
				bietername = bieter.getName();
			}
			else if ((bieter.getAlly() != null) && (bieter.getAlly() == user.getAlly()))
			{
				if (bieter.getAlly().getShowGtuBieter())
				{
					bietername = bieter.getName();
				}
			}

			long cost = entry.getPreis() + (long) (entry.getPreis() / 20d);
			if (cost == entry.getPreis())
			{
				cost++;
			}

			t.setVar("show.bid.entry", 1,
					"entry.type.name", StringEscapeUtils.escapeEcmaScript(entryname.replace('"', '\'')),
					"entry.type.image", entryimage,
					"entry.link", entrylink,
					"entry.width", entrywidth,
					"entry.height", entrywidth,
					"entry.count", entrycount,
					"bid.player", Common._title(bietername),
					"bid.player.id", bieter.getId(),
					"bid.price", cost,
					"bid.id", entry.getId());
			return t;
		}
		// Gebot bestaetigt -> Versteigerung aktuallisieren
		else if (bid > 0)
		{
			int ticks = getContext().get(ContextCommon.class).getTick();
			long cost = entry.getPreis() + (long) (entry.getPreis() / 20d);
			if (cost == entry.getPreis())
			{
				cost++;
			}

			final String entryname = entry.getObjectName();

			if ((bid >= cost)
					&& (user.getKonto().compareTo(new BigDecimal(bid).toBigInteger()) >= 0))
			{
				if (entry.getBieter() != factionObj.getUser())
				{
					User bieter = entry.getBieter();
					User factionUser = factionObj.getUser();

					PM.send(factionUser, entry.getBieter().getId(),
							"Bei Versteigerung &uuml;berboten",
							"Sie wurden bei der Versteigerung um '"
									+ entryname
									+ "' &uuml;berboten. Die von ihnen gebotenen RE in H&ouml;he von "
									+ Common.ln(entry.getPreis())
									+ " wurden auf ihr Konto zur&uuml;ck&uuml;berwiesen.\n\nGaltracorp Unlimited");

					bieter.transferMoneyFrom(factionObj.getUser().getId(), entry.getPreis(),
							"R&uuml;ck&uuml;berweisung Gebot #2" + entry.getId() + " '" + entryname
									+ "'", false, UserMoneyTransfer.Transfer.SEMIAUTO);
				}

				if (entry.getTick() < ticks + 3)
				{
					entry.setTick(ticks + 3);
				}
				entry.setBieter(user);
				entry.setPreis(bid);

				User gtu = factionObj.getUser();
				gtu.transferMoneyFrom(user.getId(), bid, "&Uuml;berweisung Gebot #2"
						+ entry.getId() + " '" + entryname + "'", false, UserMoneyTransfer.Transfer.SEMIAUTO);

				return new RedirectViewResult("versteigerung").withMessage("Sie sind der höchstbietende");
			}
			else
			{
				return new RedirectViewResult("versteigerung").withMessage("<span style=\"color:red\">Zuwenig RE auf ihrem Konto</span>");
			}
		}

		return new RedirectViewResult("default");
	}

	/**
	 * Der Container fuer Sicherheitstokens bei Ueberweisungsvorgaengen.
	 */
	@ContextInstance(ContextInstance.Scope.SESSION)
	protected static class UeberweisungsTokenContainer implements Serializable
	{
		private static final long serialVersionUID = 935839793552232133L;

		private String token;

		/**
		 * Konstruktor.
		 */
		public UeberweisungsTokenContainer()
		{
			this.token = UUID.randomUUID().toString();
		}

		/**
		 * Erzeugt ein neues Token im Container.
		 */
		public void generateNewToken()
		{
			this.token = UUID.randomUUID().toString();
		}

		/**
		 * Gibt das Token als String zurueck.
		 *
		 * @return Das Token
		 */
		public String getToken()
		{
			return this.token;
		}
	}

	/**
	 * Ueberweist einen bestimmten Geldbetrag an einen anderen Spieler. Wenn die Ueberweisung noch
	 * nicht explizit bestaetigt wurde, wird die Bestaetigung erfragt.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param to Der Spieler an den ueberwiesen werden soll
	 * @param ack yes falls die Uberweisung bestaetigt wurde (Sicherheitsabfrage)
	 * @param count Die zu ueberweisenden RE
	 * @param requestToken Ein Sicherheitstoken zur Bestaetigung der Ueberweisung
	 */
	@Action(ActionType.DEFAULT)
	public Object ueberweisenAction(User faction, String to, String ack, int count, @UrlParam(name = "token") String requestToken)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.BANK))
		{
			return new RedirectViewResult("default");
		}

		User user = (User) getUser();
		if (!istHandelErlaubt(user, factionObj))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			return new RedirectViewResult("default");
		}

		if (user.getKonto().compareTo(new BigDecimal(count).toBigInteger()) < 0)
		{
			count = user.getKonto().intValue();
		}

		if (count <= 0)
		{
			return new RedirectViewResult("default");
		}

		User tmp = User.lookupByIdentifier(to);
		if (tmp == null)
		{
			addError("Der angegebene Spieler konnte nicht gefunden werden");
			return new RedirectViewResult("bank");
		}

		UeberweisungsTokenContainer token = getContext().get(UeberweisungsTokenContainer.class);

		// Falls noch keine Bestaetigung vorliegt: Bestaetigung der Ueberweisung erfragen
		if (!ack.equals("yes") || !token.getToken().equals(requestToken))
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);
			erstelleMenue(t, factionObj);

			token.generateNewToken();

			t.setVar(
					"show.ueberweisen", 1,
					"ueberweisen.betrag", Common.ln(count),
					"ueberweisen.betrag.plain", count,
					"ueberweisen.to.name", Common._title(tmp.getName()),
					"ueberweisen.to", tmp.getId(),
					"ueberweisen.token", token.getToken());

			return t;
		}

		int ticks = getContext().get(ContextCommon.class).getTick();

		tmp.transferMoneyFrom(user.getId(), count, "&Uuml;berweisung vom "
				+ Common.getIngameTime(ticks));
		User factionUser = factionObj.getUser();

		PM.send(factionUser, tmp.getId(), "RE überwiesen", user.getNickname()
				+ " hat dir soeben " + Common.ln(count) + " RE überwiesen");
		PM.send(factionUser, user.getId(), "RE überwiesen an " + tmp.getNickname(),
				"Du hast " + tmp.getNickname() + " soeben " + Common.ln(count)
						+ " RE überwiesen");

		return new RedirectViewResult("bank");
	}

	/**
	 * Aendert den Anzeigetyp fuer Kontotransaktionen.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param type Der neue Anzeigetyp (0-2)
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult showKontoTransactionTypeAction(User faction, int type)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.VERSTEIGERUNG))
		{
			return new RedirectViewResult("default");
		}

		User user = (User) getUser();

		if ((type >= 0) && (type < 3))
		{
			user.setUserValue(WellKnownUserValue.TBLORDER_FACTIONS_KONTO_MAXTYPE, type);
		}
		return new RedirectViewResult("bank");
	}

	/**
	 * Shows the Bank Page.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public Object bankAction(User faction, RedirectViewResult redirect)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.BANK))
		{
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		erstelleMenue(t, factionObj);

		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("show.bank", 1);

		// Auwahl max. Transaktionstyp in der Kontoanzeige generieren
		int transtype = user.getUserValue(WellKnownUserValue.TBLORDER_FACTIONS_KONTO_MAXTYPE);

		String newtypetext;
		switch (transtype - 1 % 3)
		{
			case 0:
				newtypetext = "Nur manuelle anzeigen";
				break;
			case 1:
				newtypetext = "Alle nicht-automatischen anzeigen";
				break;
			case 2:
			default:
				newtypetext = "Alle anzeigen";
				transtype = 2;
		}

		t.setVar("konto.newtranstype.name", newtypetext, "konto.newtranstype", transtype - 1 % 3);

		// Kontobewegungen anzeigen
		t.setBlock("_UEBER", "moneytransfer.listitem", "moneytransfer.list");

		List<?> transferList = db
				.createQuery(
						"from UserMoneyTransfer umt "
								+ "where umt.type<= :transtype and (umt.from= :user or umt.to= :user) order by umt.time desc")
				.setInteger("transtype", transtype).setEntity("user", user).setMaxResults(40)
				.list();
		for (Object aTransferList : transferList)
		{
			UserMoneyTransfer entry = (UserMoneyTransfer) aTransferList;

			User player;

			if (user.equals(entry.getFrom()))
			{
				player = entry.getTo();
			}
			else
			{
				player = entry.getFrom();
			}

			// Negative Ueberweiszungen (die GTU wollte z.B. Geld von uns) beruecksichtigen
			int from = 0;
			BigInteger count = entry.getCount();
			if (user.equals(entry.getFrom())
					|| (count.compareTo(BigInteger.ZERO) < 0 && !user.equals(entry.getFrom())))
			{
				from = 1;
			}

			// Ueberweiszungen an andere durch - kennzeichnen
			if (from == 1)
			{
				if (count.compareTo(BigInteger.ZERO) > 0)
				{
					count = count.negate();
				}
			}
			else
			{
				count = count.abs();
			}

			t.setVar("moneytransfer.time", Common.date("j.n.Y H:i", entry.getTime()),
					"moneytransfer.from", from, "moneytransfer.player", Common._title(player
					.getName()), "moneytransfer.player.id", player.getId(),
					"moneytransfer.count", Common.ln(count), "moneytransfer.reason", entry
					.getText());

			t.parse("moneytransfer.list", "moneytransfer.listitem", true);
		}

		return t;
	}

	/**
	 * Zeigt die Seite mit diversen weiteren Infos an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public Object otherAction(User faction, RedirectViewResult redirect)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SONSTIGES))
		{
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		erstelleMenue(t, factionObj);
		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);

		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		User.Relations relationlist;
		relationlist = user.getRelations();

		t.setVar("show.other", 1);

		// GTU-Preise
		t.setBlock("_ERSTEIGERN", "kurse.listitem", "kurse.list");
		t.setBlock("kurse.listitem", "kurse.waren.listitem", "kurse.waren.list");
		t.setBlock("kurse.listitem", "kurse.verkaufswaren.listitem", "kurse.verkaufswaren.list");

		outputAstiKurse(t, db);

		List<Ship> postenList = Common.cast(db
				.createQuery("select s from Ship s left join s.modules sm " +
						"where s.id>0 and locate('tradepost',s.status)!=0 or " +
						"s.shiptype.flags like '%tradepost%' or " +
						"sm.flags like '%tradepost%' " +
						"order by s.system,s.x+s.y")
				.list());

		for (Ship tradepost : postenList)
		{
			if (!tradepost.isTradepostVisible(user, relationlist))
			{
				continue;
			}
			StarSystem sys = (StarSystem) db.get(StarSystem.class, tradepost.getSystem());
			if (!sys.isVisibleFor(user))
			{
				continue;
			}

			outputHandelspostenKurse(t, db, user, tradepost);
		}
		return t;
	}

	private void outputHandelspostenKurse(TemplateEngine t, org.hibernate.Session db, User user, Ship tradepost)
	{
		GtuWarenKurse kurse = (GtuWarenKurse) db.get(GtuWarenKurse.class, "p" + tradepost.getId());
		if (kurse == null && tradepost.getOwner().getRace() == Faction.GTU_RASSE)
		{
			kurse = (GtuWarenKurse) db.get(GtuWarenKurse.class, "tradepost");
		}
		if (kurse == null || kurse.getKurse().isEmpty())
		{
			return;
		}

		t.setVar(
				"posten.name", tradepost.getName(),
				"kurse.waren.list", "",
				"posten.owner.name", Common._title(tradepost.getOwner().getName()),
				"posten.owner.id", tradepost.getOwner().getId(),
				"posten.location", tradepost.getLocation().displayCoordinates(false));

		boolean full = tradepost.getTypeData().getCargo() <= tradepost.getCargo().getMass();

		Cargo kurseCargo = new Cargo(kurse.getKurse());
		kurseCargo.setOption(Cargo.Option.SHOWMASS, false);
		ResourceList reslist = kurseCargo.getResourceList();
		for (ResourceEntry res : reslist)
		{
			ResourceLimit limit = ResourceLimit.fuerSchiffUndItem(tradepost, res.getId());

			// Kaufen wir diese Ware vom Spieler?
			if (limit != null && !limit.willBuy(tradepost.getOwner(), user))
			{
				continue;
			}

			boolean sellable = limit == null || tradepost.getCargo().getResourceCount(res.getId()) < limit.getLimit();

			t.setVar("ware.image", res.getImage(),
					"ware.preis", (res.getCount1() / 1000d > 0.05 ? Common.ln(res.getCount1() / 1000d) : ""),
					"ware.name", res.getName(),
					"ware.plainname", res.getPlainName(),
					"ware.id", res.getId(),
					"ware.inaktiv", full || !sellable);

			t.parse("kurse.waren.list", "kurse.waren.listitem", true);
		}

		/*
		Vorerst keine Verkaufspreise, da die Liste zu lang und unuebersichtlich wird


		ResourceList buyList = tradepost.getCargo().getResourceList();
		for(ResourceEntry resource: buyList) {
			SellLimit limit = SellLimit.fuerSchiffUndItem(tradepost, resource.getId());
			if( limit == null )
			{
				continue;
			}
			if( limit.getPrice() <= 0 )
			{
				continue;
			}
			if(!limit.willSell(tradepost.getOwner(), user))
            {
                continue;
            }

			boolean buyable = tradepost.getCargo().getResourceCount(resource.getId()) - limit.getLimit() > 0;

			t.setVar(
					"ware.image", resource.getImage(),
					"ware.preis", Common.ln(limit.getPrice()),
					"ware.name", resource.getName(),
					"ware.plainname", resource.getPlainName(),
					"ware.id", resource.getId(),
					"ware.inaktiv", !buyable);

			t.parse("kurse.verkaufswaren.list", "kurse.verkaufswaren.listitem", true);
		}
		*/
		t.parse("kurse.list", "kurse.listitem", true);
	}

	private void outputAstiKurse(TemplateEngine t, org.hibernate.Session db)
	{
		GtuWarenKurse asti = (GtuWarenKurse) db.get(GtuWarenKurse.class, "asti");
		Cargo kurseCargo = new Cargo(asti.getKurse());
		kurseCargo.setOption(Cargo.Option.SHOWMASS, false);
		t.setVar(
				"posten.name", asti.getName(),
				"kurse.waren.list", "",
				"posten.owner.name", "",
				"posten.owner.id", "",
				"posten.location", "");

		ResourceList reslist = kurseCargo.getResourceList();
		for (ResourceEntry res : reslist)
		{
			t.setVar("ware.image", res.getImage(),
					"ware.preis", (res.getCount1() / 1000d > 0.05 ? Common.ln(res.getCount1() / 1000d) : ""),
					"ware.name", res.getName(),
					"ware.plainname", res.getPlainName(),
					"ware.id", res.getId());

			t.parse("kurse.waren.list", "kurse.waren.listitem", true);
		}
		t.parse("kurse.list", "kurse.listitem", true);
	}

	/**
	 * Zeigt die Angebote der Fraktion an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public Object angeboteAction(User faction, RedirectViewResult redirect)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.ANGEBOTE))
		{
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();
		erstelleMenue(t, factionObj);

		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("show.angebote", 1);

		t.setBlock("_ERSTEIGERN", "angebote.item", "angebote.list");
		t.setBlock("_ERSTEIGERN", "angebote.emptyitem", "none");

		t.setVar("none", "");

		int count = 0;
		List<?> angebote = db.createQuery("from FraktionsAngebot where faction=:faction")
				.setEntity("faction", factionObj.getUser())
				.list();
		for (Object anAngebote : angebote)
		{
			FraktionsAngebot offer = (FraktionsAngebot) anAngebote;

			count++;
			t.setVar("angebot.title", Common._title(offer.getTitle()), "angebot.image", offer
					.getImage(), "angebot.description", Common._text(offer.getDescription()),
					"angebot.linebreak", (count % 3 == 0 ? "1" : ""));

			t.parse("angebote.list", "angebote.item", true);
		}
		while (count % 3 > 0)
		{
			count++;
			t.parse("angebote.list", "angebote.emptyitem", true);
		}
		return t;
	}

	/**
	 * Zeigt die laufenden Versteigerungen an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public Object versteigerungAction(User faction, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);
		erstelleMenue(t, factionObj);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.VERSTEIGERUNG))
		{
			return new RedirectViewResult("default");
		}

		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("show.versteigerungen", 1);
		t.setBlock("_ERSTEIGERN", "entry.listitem", "entry.list");

		/*
		 * Laufende Handelsvereinbarungen anzeigen (nur solche, die man schon selbst erfuellt hat im
		 * Moment)
		 */
		handelsvereinbarungenAnzeigen(t, db, user);

		/*
		 * Einzelversteigerungen
		 */

		List<?> versteigerungen = db.createQuery("from Versteigerung order by id desc").list();
		for (Object aVersteigerungen : versteigerungen)
		{
			Versteigerung entry = (Versteigerung) aVersteigerungen;
			User bieter = entry.getBieter();

			String objectname = entry.getObjectName();
			String entryname = objectname.replace('"', '\'');

			int entrywidth = entry.isObjectFixedImageSize() ? 50 : 0;

			String bietername = "";

			if (bieter == factionObj.getUser())
			{
				bietername = bieter.getName();
			}
			else if (bieter == user)
			{
				bietername = bieter.getName();
			}
			else if (hasPermission(WellKnownPermission.FRAKTIONEN_BIETERNAME))
			{
				bietername = bieter.getName();
			}
			else if ((bieter.getAlly() != null) && (bieter.getAlly() == user.getAlly()))
			{
				if (bieter.getAlly().getShowGtuBieter())
				{
					bietername = bieter.getName();
				}
			}

			String ownername = "";

			if (hasPermission(WellKnownPermission.FRAKTIONEN_ANBIETERNAME) && (entry.getOwner() != factionObj.getUser())
					&& (entry.getOwner() != user))
			{
				ownername = Common._title(entry.getOwner().getName());
			}

			int ticks = getContext().get(ContextCommon.class).getTick();

			t.setVar("entry.link", entry.getObjectUrl(),
					"entry.type.name", entryname,
					"entry.type.image", entry.getObjectPicture(),
					"entry.preis", Common.ln(entry.getPreis()),
					"entry.bieter", Common._title(bietername),
					"entry.bieter.id", entry.getBieter().getId(),
					"entry.dauer", entry.getTick() - ticks,
					"entry.aukid", entry.getId(),
					"entry.width", entrywidth,
					"entry.height", entrywidth,
					"entry.count", entry.getObjectCount(),
					"entry.user.name", ownername,
					"entry.user.id", entry.getOwner().getId(),
					"entry.user", (entry.getOwner() != factionObj.getUser()),
					"entry.bidAllowed", istHandelErlaubt(user, factionObj),
					"entry.ownauction", (entry.getOwner() == user));

			t.parse("entry.list", "entry.listitem", true);
		}

		t.setBlock("_ERSTEIGERN", "gtu.dropzones.listitem", "gtu.dropzones.list");


		dropZoneAuswahlAnzeigen(t, db, user);
		return t;
	}

	private void handelsvereinbarungenAnzeigen(TemplateEngine t, Session db, User user)
	{
		t.setBlock("_ERSTEIGERN", "gtuzwischenlager.listitem", "gtuzwischenlager.list");

		Set<Ship> gzlliste = new HashSet<>();

		List<?> entries = db.createQuery("from GtuZwischenlager where user1= :user or user2= :user")
				.setEntity("user", user).list();

		for (Object entry1 : entries)
		{
			GtuZwischenlager aentry = (GtuZwischenlager) entry1;

			Cargo owncargoneed = aentry.getCargo1Need();
			if (aentry.getUser2() == user)
			{
				owncargoneed = aentry.getCargo2Need();
			}

			if (owncargoneed.isEmpty())
			{
				gzlliste.add(aentry.getPosten());
			}
		}

		for (Ship aposten : gzlliste)
		{
			t.setVar("gtuzwischenlager.name", Common._plaintitle(aposten.getName()),
					"gtuzwischenlager.location", aposten.getLocation().displayCoordinates(true),
					"gtuzwischenlager.location.url", aposten.getLocation().urlFragment());

			t.parse("gtuzwischenlager.list", "gtuzwischenlager.listitem", true);
		}
	}

	private void dropZoneAuswahlAnzeigen(TemplateEngine t, Session db, User user)
	{
		List<StarSystem> dropZones = ermittleMoeglicheDropZones();
		StarSystem aktuelleDropZone = (StarSystem) db.get(StarSystem.class, user.getGtuDropZone());
		if (!dropZones.contains(aktuelleDropZone))
		{
			dropZones.add(aktuelleDropZone);
		}
		for (StarSystem system : dropZones)
		{
			t.setVar("dropzone.system.id", system.getID(),
					"dropzone.system.name", system.getName(),
					"dropzone.selected", (user.getGtuDropZone() == system.getID()));

			t.parse("gtu.dropzones.list", "gtu.dropzones.listitem", true);
		}
	}


	public List<StarSystem> ermittleMoeglicheDropZones()
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		List<StarSystem> result = new ArrayList<>();

		int defaultDropZone = configService.getValue(WellKnownConfigValue.GTU_DEFAULT_DROPZONE);

		List<?> systems = db.createCriteria(StarSystem.class).addOrder(Order.asc("id")).list();
		for (Object system1 : systems)
		{
			StarSystem system = (StarSystem) system1;
			if (system.getDropZone() != null && (user.getAstiSystems().contains(system.getID()) || system.getID() == defaultDropZone))
			{
				result.add(system);
			}
		}
		return result;
	}

	/**
	 * Zeigt den Fraktionstext an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public Object generalAction(User faction, RedirectViewResult redirect)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.ALLGEMEIN))
		{
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		erstelleMenue(t, factionObj);

		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("show.general", 1, "global.faction.text", Common._text(factionObj.getText()));

		return t;
	}

	/**
	 * Berechnet die Kosten eines Transportauftrags und speichert ihn in der Datenbank.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param sourcesystem Das Ausgangssystem
	 * @param ganymedeid Die ID der zu transportierenden Ganymede
	 * @param targetsystem Die ID des Zielsystems
	 * @param targetx Die Ziel-X-Koordinate
	 * @param targety Die Ziel-Y-Koordinate
	 * @param transport Sofert der Wert <code>1</code>, wird der Transportauftrag
	 * bestaetigt und abgespeichert
	 */
	@Action(ActionType.DEFAULT)
	public Object shopOrderGanymedeSummaryAction(User faction, int sourcesystem, int ganymedeid, int targetsystem, int targetx, int targety, int transport)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			return new RedirectViewResult("default");
		}

		if (!istHandelErlaubt(user, factionObj))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			return new RedirectViewResult("default");
		}

		FactionShopEntry shopentry = (FactionShopEntry) db
				.createQuery("from FactionShopEntry where faction=:faction and type=2")
				.setEntity("faction", factionObj.getUser())
				.setMaxResults(1)
				.uniqueResult();

		if (shopentry == null)
		{
			return new RedirectViewResult("default");
		}

		Ship gany = (Ship) db.get(Ship.class, ganymedeid);

		if (gany == null ||
				gany.getOwner().getId() != user.getId() ||
				gany.getSystem() != sourcesystem ||
				gany.getType() != ShopGanyTransportEntry.SHIPTYPE_GANYMEDE)
		{
			addError("Die angegebene Ganymede konnte im Ausgangssystem nicht lokalisiert werden");

			return new RedirectViewResult("shopOrderGanymede")
					.setParameter("sourcesystem", 0)
					.setParameter("ganymedeid", 0);
		}

		FactionShopOrder sameorder = (FactionShopOrder) db
				.createQuery("from FactionShopOrder where user=:user and addData like :pattern and status < 4")
				.setParameter("user", user)
				.setParameter("pattern", gany.getId() + "@%")
				.uniqueResult();
		if (sameorder != null)
		{
			addError("Es existiert bereits ein Transport-Auftrag f&uuml;r diese Ganymede");
			return new RedirectViewResult("shopOrderGanymede")
					.setParameter("sourcesystem", 0)
					.setParameter("ganymedeid", 0);
		}

		StarSystem system = (StarSystem) db.get(StarSystem.class, targetsystem);
		if (system.isVisibleFor(user))
		{
			addError("Die angegebene Zielsystem konnte nicht lokalisiert werden");

			return new RedirectViewResult("shopOrderGanymede")
					.setParameter("targetsystem", 0);
		}

		if ((targetx < 1) || (targetx > system.getWidth()) || (targety < 1)
				|| (targety > system.getHeight()))
		{
			addError("Die angegebene Zielkoordinaten konnten im Zielsystem nicht lokalisiert werden");

			return new RedirectViewResult("shopOrderGanymede")
					.setParameter("targetx", 0)
					.setParameter("targety", 0);
		}

		// Weg finden und Preis ausrechnen
		Map<Integer, List<JumpNode>> jumpnodes = new HashMap<>();
		List<?> jnList = db.createQuery("from JumpNode where hidden=false and (systemOut!=:source or system=:source)")
				.setInteger("source", sourcesystem)
				.list();

		for (Object obj : jnList)
		{
			JumpNode jn = (JumpNode) obj;
			if (!jumpnodes.containsKey(jn.getSystem()))
			{
				jumpnodes.put(jn.getSystem(), new ArrayList<>());
			}
			jumpnodes.get(jn.getSystem()).add(jn);
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		erstelleMenue(t, factionObj);

		long totalcost = 0;
		JumpNodeRouter.Result shortestpath = new JumpNodeRouter(jumpnodes)
				.locateShortestJNPath(sourcesystem, gany.getX(), gany.getY(), targetsystem, targetx, targety);
		if (shortestpath == null)
		{
			transport = 0;
			t.setVar("transport.price", "<span style=\"color:red\">Kein Weg gefunden</span>");
		}
		else
		{
			Map<String, Long> costindex = new HashMap<>();
			List<FactionShopEntry> entries = Common.cast(db
					.createQuery("from FactionShopEntry where faction=:faction and type=:type")
					.setParameter("faction", factionObj.getUser())
					.setParameter("type", FactionShopEntry.Type.TRANSPORT)
					.list());
			for (FactionShopEntry entry : entries)
			{
				costindex.put(entry.getResource(), entry.getPrice());
			}

			Set<Integer> systemlist = new HashSet<>();
			systemlist.add(sourcesystem);
			for (int i = 0; i < shortestpath.path.size(); i++)
			{
				JumpNode jn = shortestpath.path.get(i);
				systemlist.add(jn.getSystemOut());
			}

			for (Integer sys : systemlist)
			{
				if (costindex.containsKey(Integer.toString(sys)))
				{
					totalcost += costindex.get(Integer.toString(sys));
				}
				else
				{
					totalcost += costindex.get("*");
				}
			}

			if (totalcost < 0)
			{
				totalcost = 0;
			}

			if (user.getKonto().compareTo(new BigDecimal(totalcost).toBigInteger()) >= 0)
			{
				t.setVar("transport.price", Common.ln(totalcost) + " RE", "transport.enableOrder",
						1);
			}
			else
			{
				transport = 0;
				t.setVar("transport.price", "<span style=\"color:red\">" + Common.ln(totalcost)
						+ " RE</span>");
			}
		}

		if (transport == 0)
		{
			t.setVar("show.shopOrderGanymedeSummary", 1,
					"ganymede.id", gany.getId(),
					"ganymede.name", gany.getName(),
					"source.system", sourcesystem,
					"source.x", gany.getX(),
					"source.y", gany.getY(),
					"target.system", targetsystem,
					"target.x", targetx,
					"target.y", targety);
		}
		else
		{
			User factionUser = factionObj.getUser();
			factionUser.transferMoneyFrom(user.getId(), totalcost,
					"&Uuml;berweisung Bestellung #ganytransXX" + gany.getId());

			StringBuilder waypoints = new StringBuilder(300);
			waypoints.append("Start: ").append(sourcesystem).append(":").append(gany.getX()).append("/").append(gany.getY()).append("\n");
			for (int i = 0; i < shortestpath.path.size(); i++)
			{
				JumpNode jn = shortestpath.path.get(i);

				waypoints.append(jn.getSystem()).append(":").append(jn.getX()).append("/").append(jn.getY()).append(" -> ");
				waypoints.append(jn.getSystemOut()).append(":").append(jn.getXOut()).append("/").append(jn.getYOut()).append(" ");
				waypoints.append("[ID: ").append(jn.getId()).append("]\n");
			}
			waypoints.append("Ziel: ").append(targetsystem).append(":").append(targetx).append("/").append(targety).append("\n");

			PM.send(user, factionObj.getUser().getId(), "[auto] Shop-Bestellung [Ganymede]",
					"Besteller: [userprofile=" + user.getId() + "]" + user.getName() + " ("
							+ user.getId() + ")[/userprofile]\nObjekt: " + gany.getName()
							+ " (" + gany.getId() + ")\nPreis: " + Common.ln(totalcost)
							+ "\nZeitpunkt: " + Common.date("d.m.Y H:i:s")
							+ "\n\n[b][u]Pfad[/u][/b]:\n" + waypoints);

			String adddataStr = gany.getId() + "@" + sourcesystem + ":" + gany.getX()
					+ "/" + gany.getY() + "->" + targetsystem + ":" + targetx + "/" + targety;


			FactionShopOrder newOrder = new FactionShopOrder(shopentry, user);
			newOrder.setPrice(totalcost);
			newOrder.setAddData(adddataStr);
			db.persist(newOrder);

			Taskmanager taskmanager = Taskmanager.getInstance();
			taskmanager.addTask(Taskmanager.Types.GANY_TRANSPORT, 1, Integer.toString(newOrder.getId()), "", "");

			String message = "Bestellung &uuml;ber 1 Ganymede-Transport des Objekts "
					+ gany.getId() + " von " + sourcesystem + ":" + gany.getX() + "/"
					+ gany.getY() + " nach " + targetsystem + ":" + targetx + "/" + targety
					+ " f&uuml;r " + Common.ln(totalcost)
					+ " erhalten und vom System best&auml;tigt.<br />Einen angenehmen Tag noch!";

			return new RedirectViewResult("shop").withMessage(message);
		}
		return t;
	}

	/**
	 * Zeigt die GUI zur Erstellung eines Ganymede-Transportauftrags.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param sourcesystem Das Ausgangssystem
	 * @param ganymedeid Die ID der zu transportierenden Ganymede
	 * @param targetsystem Die ID des Zielsystems
	 * @param targetx Die Ziel-X-Koordinate
	 * @param targety Die Ziel-Y-Koordinate
	 */
	@Action(ActionType.DEFAULT)
	public Object shopOrderGanymedeAction(User faction, int sourcesystem, int ganymedeid, int targetsystem, int targetx, int targety)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			return new RedirectViewResult("default");
		}

		if (!istHandelErlaubt(user, factionObj))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			return new RedirectViewResult("default");
		}

		FactionShopEntry entry = (FactionShopEntry) db
				.createQuery("from FactionShopEntry where faction=:faction and type=2")
				.setEntity("faction", factionObj.getUser())
				.setMaxResults(1)
				.uniqueResult();

		if (entry == null)
		{
			return new RedirectViewResult("default");
		}

		// Wenn alle Parameter eingegebene wurden -> shopOrderGanymedeSummary
		if (targetsystem != 0 && ganymedeid != 0
				&& targetx != 0 && targety != 0 && sourcesystem != 0)
		{

			return new RedirectViewResult("shopOrderGanymedeSummary");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		erstelleMenue(t, factionObj);

		t.setBlock("_ERSTEIGERN", "ganytrans.sourcesystem.listitem", "ganytrans.sourcesystem.list");
		t.setBlock("_ERSTEIGERN", "ganytrans.ganymedes.listitem", "ganytrans.ganymedes.list");
		t.setBlock("_ERSTEIGERN", "ganytrans.targetsystem.listitem", "ganytrans.targetsystem.list");
		t.setVar("show.shopOrderGanymede", 1);

		// Dummyeintrag (Ausgangssysteme)
		t.setVar("sourcesystem.id", 0, "sourcesystem.name", "-");
		t.parse("ganytrans.sourcesystem.list", "ganytrans.sourcesystem.listitem", true);

		// Liste aller bereits mit einem Transport-Auftrag ausgestatteten Ganys generieren
		Set<Integer> blockedganylist = new HashSet<>();

		List<FactionShopOrder> orderList = Common.cast(db
				.createQuery("from FactionShopOrder fso " +
						"where fso.user=:user and fso.status<4 and fso.shopEntry.type=:type")
				.setEntity("user", user)
				.setParameter("type", FactionShopEntry.Type.TRANSPORT)
				.list());
		for (FactionShopOrder order : orderList)
		{
			String[] tmp = StringUtils.split(order.getAddData(), "@");
			int ganyid = Integer.parseInt(tmp[0]);

			blockedganylist.add(ganyid);
		}

		String blockedganysql = "";
		if (blockedganylist.size() > 0)
		{
			blockedganysql = "AND id not in (:ganylist)";
		}

		ShipType ganyType = (ShipType) db.get(ShipType.class, ShopGanyTransportEntry.SHIPTYPE_GANYMEDE);

		boolean first = true;
		Query query = db
				.createQuery("select distinct system from Ship where shiptype=:ganyType and owner=:user " + blockedganysql)
				.setEntity("ganyType", ganyType)
				.setEntity("user", user);
		if (blockedganylist.size() > 0)
		{
			query.setParameterList("ganylist", blockedganylist);
		}
		List<Integer> ganySystems = Common.cast(query
				.list());
		for (Integer asystem : ganySystems)
		{
			if (sourcesystem == asystem)
			{
				t.setVar("sourcesystem.selected", 1);
				first = false;
			}
			else
			{
				t.setVar("sourcesystem.selected", 0);
			}
			StarSystem system = (StarSystem) db.get(StarSystem.class, asystem);
			t.setVar("sourcesystem.id", asystem, "sourcesystem.name", system.getName());

			t.parse("ganytrans.sourcesystem.list", "ganytrans.sourcesystem.listitem", true);
		}

		// Check, ob ein System ausgewaehlt wurde.
		// Wenn nicht -> Ende
		if (first || sourcesystem == 0)
		{
			return t;
		}

		t.setVar("sourcesystem.known", 1);

		// Moegliche Ganymedes ausgeben
		first = true;
		List<Ship> ships = Common.cast(db
				.createQuery("from Ship where shiptype=:ganyType and owner=:user and system=:sys order by x+y")
				.setEntity("ganyType", ganyType)
				.setEntity("user", user)
				.setInteger("sys", sourcesystem)
				.list());
		for (Ship agany : ships)
		{
			if (blockedganylist.contains(agany.getId()))
			{
				continue;
			}

			if (first)
			{
				t.setVar("ganymede.selected", 1);
				first = false;
			}
			else
			{
				t.setVar("ganymede.selected", 0);
			}
			t.setVar("ganymede.id", agany.getId(),
					"ganymede.name", Common._plaintitle(agany.getName()));

			t.parse("ganytrans.ganymedes.list", "ganytrans.ganymedes.listitem", true);
		}

		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem").list());

		// Zielsysteme ausgeben
		first = true;
		for (StarSystem system : systems)
		{
			if ( !system.isVisibleFor(user) )
			{
				continue;
			}

			if (first)
			{
				t.setVar("targetsystem.selected", 1);
				first = false;
			}
			else
			{
				t.setVar("targetsystem.selected", 0);
			}

			t.setVar("targetsystem.id", system.getID(), "targetsystem.name", system.getName());

			t.parse("ganytrans.targetsystem.list", "ganytrans.targetsystem.listitem", true);
		}
		return t;
	}

	/**
	 * Fuehrt eine Bestellung im Shop aus. Der User muss dazu eine gewuenschte Lieferposition
	 * angeben. Wenn diese noch nicht angegeben wurde, wird sie erfragt.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param shopentry Die ID des Shopeintrags, der bestellt werden soll
	 * @param ordercount Die Liefermenge
	 * @param ordersys Das Liefersystem
	 * @param orderx Die X-Komponente der Lieferkoordinate
	 * @param ordery Die Y-Komponente der Lieferkoordinate
	 */
	@Action(ActionType.DEFAULT)
	public Object shopOrderAction(User faction, FactionShopEntry shopentry, int ordercount, int ordersys, int orderx, int ordery)
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			return new RedirectViewResult("default");
		}

		if (!istHandelErlaubt(user, factionObj))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			return new RedirectViewResult("default");
		}

		if (shopentry == null)
		{
			return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Es existiert kein passendes Angebot</span>");
		}

		if (!shopentry.canBuy(user))
		{
			return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Es existiert kein passendes Angebot</span>");
		}

		if (shopentry.getAvailability() == 2)
		{
			return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Das Angebot ist nicht verf&uuml;gbar</span>");
		}

		// Ganymed-Transporte verarbeiten
		if (shopentry.getType() == FactionShopEntry.Type.TRANSPORT)
		{
			return new RedirectViewResult("shopOrderGanymede");
		}

		if (ordercount < 1)
		{
			return new RedirectViewResult("shop");
		}

		ShopEntry entry;

		if (shopentry.getType() == FactionShopEntry.Type.SHIP)
		{ // Schiff
			entry = new ShopShipEntry(shopentry);
		}
		else if (shopentry.getType() == FactionShopEntry.Type.ITEM)
		{ // Cargo
			entry = new ShopResourceEntry(shopentry);
		}
		else
		{
			throw new RuntimeException("Unbekannter Versteigerungstyp '" + shopentry.getType()
					+ "'");
		}

		if (user.getKonto().compareTo(new BigDecimal(entry.getPrice() * ordercount).toBigInteger()) < 0)
		{
			return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Sie verf&uuml;gen nicht &uuml;ber genug Geld</span>");
		}

		User factionUser = factionObj.getUser();
		if (user.getLoyalitaetspunkteTotalBeiNpc(factionUser) < entry.getLpKosten() * ordercount)
		{
			return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Sie verfügen nicht über genug Loyalitätspunkte</span>");
		}

		if (ordersys == 0 || orderx == 0 || ordery == 0)
		{
			TemplateEngine t = templateViewResultFactory.createFor(this);
			erstelleMenue(t, factionObj);

			t.setVar("show.shopOrderLocation", 1,
					"order.count", ordercount,
					"order.name", entry.getName(),
					"order.entry", entry.getID());

			return t;
		}
		else
		{
			FactionShopOrder order = new FactionShopOrder(shopentry, user);
			order.setCount(ordercount);
			order.setPrice(ordercount * entry.getPrice());
			order.setLpKosten(ordercount * entry.getLpKosten());
			order.setAddData(ordersys + ":" + orderx + "/" + ordery);

			db.persist(order);

			String bestellId = "#" + entry.getType() + entry.getResource() + "XX";

			if (entry.getLpKosten() > 0)
			{
				Loyalitaetspunkte lp = new Loyalitaetspunkte(user, factionUser, "Bestellung " + bestellId, (int) (-ordercount * entry.getLpKosten()));
				user.getLoyalitaetspunkte().add(lp);
				db.persist(lp);
			}

			if (entry.getPrice() > 0)
			{
				factionUser.transferMoneyFrom(user.getId(), entry.getPrice() * ordercount,
						"&Uuml;berweisung Bestellung " + bestellId
								+ ordercount);
			}

			PM.send(user, factionObj.getUser().getId(), "[auto] Shop-Bestellung", "Besteller: [userprofile="
					+ user.getId() + "]" + user.getName() + " (" + user.getId()
					+ ")[/userprofile]\nObjekt: " + entry.getName() + "\nMenge: " + ordercount
					+ "\nLieferkoordinaten: " + ordersys + ":" + orderx + "/" + ordery
					+ "\nZeitpunkt: " + Common.date("d.m.Y H:i:s"));

			String message = "Bestellung über "
							+ ordercount
							+ "x "
							+ entry.getName()
							+ " für "
							+ Common.ln(entry.getPrice() * ordercount) + " RE"
							+ (entry.getLpKosten() > 0 ? " und " + Common.ln(entry.getLpKosten()) + " LP " : "")
							+ " erhalten und vom System bestätigt.<br />Sollten noch Rückfragen bestehend so wird sich ein Sachbearbeiter bei ihnen melden.<br />Einen angenehmen Tag noch!";

			return new RedirectViewResult("shop").withMessage(message);
		}
	}

	/**
	 * Erstellt einen neuen Shop-Eintrag.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param entryType Der Typ des Eintrags (ship,item,transport)
	 * @param entryTypeId Die ID der anzubietenden Ware des angegebenen Eintragtyps
	 * @param entryCost Die Kosten des Eintrags in LP
	 * @param entryLpKosten Die Kosten des Eintrags in RE
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult shopEntryCreate(User faction, String entryType, String entryTypeId, int entryCost, int entryLpKosten)
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			return new RedirectViewResult("default");
		}

		if (factionObj.getUser() == user)
		{
			FactionShopEntry.Type type = null;
			switch (entryType)
			{
				case "ship":
					type = FactionShopEntry.Type.SHIP;
					if (!NumberUtils.isCreatable(entryTypeId))
					{
						return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Format ungueltig</span>");
					}
					ShipType st = (ShipType) db.get(ShipType.class, Integer.parseInt(entryTypeId));
					if (st == null)
					{
						return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Kein bekannter Schiffstyp</span>");
					}
					break;
				case "item":
					type = FactionShopEntry.Type.ITEM;
					if (ItemID.fromString(entryTypeId) == null)
					{
						return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Format ungueltig</span>");
					}
					break;
				case "transport":
					type = FactionShopEntry.Type.TRANSPORT;
					if (!NumberUtils.isNumber(entryTypeId) && !"*".equals(entryTypeId))
					{
						return new RedirectViewResult("shop").withMessage("<span style=\"color:red\">Format ungueltig</span>");
					}
					break;
			}

			FactionShopEntry entry = new FactionShopEntry(factionObj.getUser(), type, entryTypeId);
			entry.setAvailability(0);
			entry.setPrice(entryCost);
			entry.setLpKosten(entryLpKosten);

			db.persist(entry);
		}

		return new RedirectViewResult("shop");
	}

	/**
	 * Aendert einen Shopeintrag.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param operation Die auszufuehrende Aktion (ändern, löschen)
	 * @param shopentry Die ID des Shopeintrags
	 * @param availability Die neue Verfuegbarkeit
	 * @param entryRang Der Rang
	 * @param entryPrice Der Preis
	 * @param entryLpKosten Die LP-Kosten
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult shopChangeEntryAction(User faction, String operation, FactionShopEntry shopentry, int availability, int entryRang, long entryPrice, long entryLpKosten)
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			return new RedirectViewResult("default");
		}

		if (factionObj.getUser() == user)
		{
			if ((shopentry == null) || (shopentry.getFaction() != factionObj.getUser()))
			{
				addError("Es konnte kein passender Shopeintrag gefunden werden");
				return new RedirectViewResult("shop");
			}

			if ("löschen".equalsIgnoreCase(operation))
			{
				if (shopentry.getAnzahlOffeneBestellungen() > 0)
				{
					addError("Es gibt noch offene Bestellungen zu diesem Shopeintrag");
					return new RedirectViewResult("shop");
				}

				db.delete(shopentry);

				return new RedirectViewResult("shop").withMessage("Eintrag gelöscht");
			}

			if (availability < 0 || availability > 2)
			{
				addError("Ungültiger Status");
				return new RedirectViewResult("shop");
			}

			shopentry.setAvailability(availability);
			shopentry.setMinRank(entryRang);
			shopentry.setPrice(entryPrice);
			shopentry.setLpKosten(entryLpKosten);

			return new RedirectViewResult("shop").withMessage("Eintrag geaendert");
		}
		return new RedirectViewResult("shop");
	}

	/**
	 * Aendert den Auftragsstatus einer Bestellung.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param orderentry Die ID des Auftrags
	 * @param orderstatus Der neue Auftragsstatus
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult changeShopOrderStatusAction(User faction, FactionShopOrder orderentry, int orderstatus)
	{
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			return new RedirectViewResult("default");
		}

		if (factionObj.getUser() == user)
		{
			if ((orderentry == null) || (orderentry.getStatus() > 3)
					|| (orderentry.getShopEntry().getFaction() != factionObj.getUser()))
			{
				addError("Es konnte kein passender Ordereintrag gefunden werden");
				return new RedirectViewResult("shop");
			}

			if (orderstatus < 0 || orderstatus > 4)
			{
				addError("Ung&uuml;ltiger Status");
				return new RedirectViewResult("shop");
			}

			orderentry.setStatus(orderstatus);

			return new RedirectViewResult("shop").withMessage("Neuer Status erfolgreich zugewiesen");
		}
		return new RedirectViewResult("shop");
	}

	private String getStatusColor(int status)
	{
		switch (status)
		{
			case 0:
				return "#DDDD00";
			case 1:
				return "#4477DD";
			case 2:
				return "#55DD55";
			case 3:
				return "#CC2222";
		}
		return "";
	}

	private String getStatusName(int status)
	{
		switch (status)
		{
			case 0:
				return "&nbsp;&nbsp;Neu&nbsp;&nbsp;";
			case 1:
				return "In Bearbeitung";
			case 2:
				return "Auslieferung";
			case 3:
				return "Nicht verf&uuml;gbar";
		}
		return "";
	}

	private static final int ITEM_BBS = 182;

	/**
	 * Zeigt die GUI für den Asti-Asubau an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param base Die ID des auszubauenden Asteroiden
	 * @param colonizer Die ID des zu verwendenden Colonizers
	 * @param upgradeInfoMap Die Upgrades, die eingebaut werden sollen
	 * @param bar Die Zahlungsmethode, {@code true} bedeutet Barzahlung, sonst Abbuchung
	 * @param order Soll wirklich bestellt werden (bestellen)?
	 */
	@Action(ActionType.DEFAULT)
	public Object ausbauAction(User faction,
										   @UrlParam(name = "astiid") Base base,
										   @UrlParam(name = "colonizerid") Ship colonizer,
										   @UrlParam(name = "upgrade#") Map<String, UpgradeInfo> upgradeInfoMap,
										   boolean bar,
										   String order,
										   RedirectViewResult redirect)
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.AUSBAU))
		{
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		erstelleMenue(t, factionObj);

		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);

		if ("bestellen".equals(order) && !upgradeInfoMap.isEmpty())
		{
			if (base == null)
			{
				addError("Der angew&auml;hlte Asteroid existiert nicht");
				return new RedirectViewResult("default");
			}

			if (!base.getOwner().equals(getUser()))
			{
				addError("Dieser Asteroid gehört Ihnen nicht");
				return new RedirectViewResult("default");
			}

			if (colonizer == null || !colonizer.getOwner().equals(user)
					|| !colonizer.getLocation().equals(base.getLocation())
					|| !colonizer.getTypeData().hasFlag(ShipTypeFlag.COLONIZER))
			{
				addError("Der ausgewählte Colonizer ist ungültig");
				return new RedirectViewResult("default");
			}

            List<UpgradeInfo> upgrades = new ArrayList<>(upgradeInfoMap.values());

			// Alle Werte wurden übergeben, nur noch testen ob sie akzeptabel sind
			// Für jeden Asti darf maximal ein Auftrag in der DB sein
			UpgradeJob auftrag = (UpgradeJob) db.createQuery("from UpgradeJob where base=:base")
					.setParameter("base", base)
					.uniqueResult();

			if (auftrag != null)
			{
				addError("F&uuml;r diesen Asteroid besteht bereits ein Auftrag");
				return new RedirectViewResult("default");
			}

            // Teste, ob die upgrades zulaessig sind
			for(UpgradeInfo upgrade : upgrades)
			{
				// Die Standardwerte 0 sind nicht als UpgradeInfo vorhanden. Sind diese ausgewählt wird null hinzugefügt
				if (upgrade != null && !upgrade.getUpgradeType().checkUpgrade(upgrade, base))
				{
					addError(upgrade.getUpgradeType().errorMsg());
					return new RedirectViewResult("default");
				}
			}

			// Erstelle einen neuen Auftrag
			auftrag = new UpgradeJob(base, user, bar, colonizer);
			upgrades.stream().filter(Objects::nonNull).forEach(auftrag::addUpgrade);

			User factionUser = factionObj.getUser();
			if (!bar)
			{
				// Testen ob genuegend Geld vorhanden ist um es uns untern Nagel zu reiszen
				if (user.getKonto().compareTo(new BigDecimal(auftrag.getPrice()).toBigInteger()) < 0)
				{
					addError("Sie verfügen nicht über genug Geld");
					return new RedirectViewResult("default");
				}
				factionUser.transferMoneyFrom(user.getId(), auftrag.getPrice(), "Ausbau von " + base.getName());
			}

			// Den Besitzer des Colonizers ändern
			if (colonizer.consign(factionUser, false))
			{
				addError("Der Colonizer konnte nicht übergeben werden.");
				return new RedirectViewResult("default");
			}

			// Auftrag speichern
			db.persist(auftrag);

			// Erstelle einen neuen Task für den Auftrag
			Taskmanager taskmanager = Taskmanager.getInstance();
			taskmanager.addTask(Taskmanager.Types.UPGRADE_JOB, 1,
					Integer.toString(auftrag.getId()), "0", Integer.toString(factionObj.getUser().getId()));

			return new RedirectViewResult("default").withMessage("Ihr Auftrag wurde an den zuständigen Sachbearbeiter weitergeleitet. Die Baumaßnahmen werden in kürze beginnen.");
		}

		t.setVar("show.ausbau", 1);

		t.setBlock("_ERSTEIGERN", "ausbau.asti.listitem", "ausbau.asti.list");
		t.setBlock("_ERSTEIGERN", "ausbau.colonizer.listitem", "ausbau.colonizer.list");
		t.setBlock("_ERSTEIGERN", "ausbau.upgradetypes.listitem", "ausbau.upgradetypes.list");
        t.setBlock("_ERSTEIGERN", "ausbau.upgradetypes.javascript.1.listitem", "ausbau.upgradetypes.javascript.1.list");
        t.setBlock("_ERSTEIGERN", "ausbau.upgradetypes.javascript.2.listitem", "ausbau.upgradetypes.javascript.2.list");
        t.setBlock("ausbau.upgradetypes.listitem", "ausbau.upgradetypes.mods.listitem", "ausbau.upgradetypes.mods.list");

        ConfigValue configrabattfaktor = configService.get(WellKnownConfigValue.DI_FAKTOR_RABATT);
        ConfigValue configzeitfaktor = configService.get(WellKnownConfigValue.DI_FAKTOR_ZEIT);
        double rabattfaktor = Double.valueOf(configrabattfaktor.getValue());
        double zeitfaktor = Double.valueOf(configzeitfaktor.getValue());

        t.setVar("ausbau.rabattfaktordi", rabattfaktor,
                 "ausbau.zeitfaktordi", zeitfaktor );

        // Hole alle Astis des Spielers und markiere gewaehlten Asti
		Set<Base> astis = user.getBases();
		Base selectedBase = null;
		for (Base asti : astis)
		{
			if (asti.getKlasse().getUpgradeMaxValues().isEmpty())
			{
				continue;
			}

			t.setVar("asti.id", asti.getId(),
					"asti.name", asti.getName(),
					"asti.selected", base == asti);
			t.parse("ausbau.asti.list", "ausbau.asti.listitem", true);
			if (base == asti)
			{
				selectedBase = asti;
			}
		}

		if (selectedBase == null && !astis.isEmpty())
		{
			selectedBase = astis.iterator().next();
		}

		if (selectedBase == null)
		{
			return t;
		}

		t.setVar("erz.name", Cargo.getResourceName(Resources.ERZ), "erz.image", Cargo
				.getResourceImage(Resources.ERZ), "bbs.name", Cargo.getResourceName(new ItemID(
				ITEM_BBS)), "bbs.image", Cargo.getResourceImage(new ItemID(ITEM_BBS)));

		// Hole die Colos des ausgewaehlten Astis
		List<Ship> colonizers = Common.cast(db
				.createQuery(
						"from Ship where shiptype.flags like :colonizer and "
								+ "owner=:user and system=:baseSystem and x=:baseX AND y=:baseY order by id")
				.setString("colonizer", ShipTypeFlag.COLONIZER.getFlag())
				.setParameter("user", user)
				.setInteger("baseSystem", selectedBase.getSystem())
				.setInteger("baseX", selectedBase.getX())
				.setInteger("baseY", selectedBase.getY())
				.list());

		User factionUser = factionObj.getUser();
		for (Ship acolonizer : colonizers)
		{
			if (acolonizer.consign(factionUser, true))
			{
				continue;
			}
			t.setVar("colonizer.id", acolonizer.getId(),
					"colonizer.name", acolonizer.getName());
			t.parse("ausbau.colonizer.list", "ausbau.colonizer.listitem", true);
		}

		// Setze die ausbau-mods, finde heraus welche bereits angewendet wurden und Typ des Astis
        for(UpgradeType upgradeType : UpgradeType.values()) {
			List<UpgradeInfo> possibleMods = selectedBase.getKlasse().getUpgradeInfos().stream().filter(u -> u.getUpgradeType() == upgradeType).sorted().collect(Collectors.toList());
			if (possibleMods.isEmpty())
			{
				continue;
			}

			t.setVar(
					"upgradetypes.selectionname", upgradeType.getDescription(),
					"upgradetypes.name", upgradeType.getName(),
					"upgradetypes.nullmod", upgradeType.getUpgradeText(0),
					"ausbau.upgradetypes.mods.list", ""
			);

			for (UpgradeInfo info : possibleMods) {
				if (upgradeType.checkUpgrade(info, selectedBase)) {
					t.setVar("upgradetypes.mods.mod", upgradeType.getUpgradeText(info.getModWert()),
							"upgradetypes.mods.id", info.getId(),
							"upgradetypes.mods.preis", info.getPrice(),
							"upgradetypes.mods.bbs", info.getMiningExplosive(),
							"upgradetypes.mods.erz", info.getOre(),
							"upgradetypes.mods.minticks", info.getMinTicks(),
							"upgradetypes.mods.maxticks", info.getMaxTicks());
					t.parse("ausbau.upgradetypes.mods.list", "ausbau.upgradetypes.mods.listitem", true);
				}
			}
			t.parse("ausbau.upgradetypes.list", "ausbau.upgradetypes.listitem", true);
			t.parse("ausbau.upgradetypes.javascript.1.list", "ausbau.upgradetypes.javascript.1.listitem", true);
			t.parse("ausbau.upgradetypes.javascript.2.list", "ausbau.upgradetypes.javascript.2.listitem", true);
        }
		return t;
	}

	/**
	 * Zeigt den Shop der Fraktion an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public Object shopAction(User faction, RedirectViewResult redirect)
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.SHOP))
		{
			return new RedirectViewResult("default");
		}

		User factionUser = factionObj.getUser();

		TemplateEngine t = templateViewResultFactory.createFor(this);
		erstelleMenue(t, factionObj);
		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("show.shop", 1);

		t.setBlock("_ERSTEIGERN", "shop.listitem", "shop.list");
		t.setBlock("_ERSTEIGERN", "shop.orderlist.listitem", "shop.orderlist.list");
		t.setBlock("_ERSTEIGERN", "shop.shopownerlist.listitem", "shop.shopownerlist.list");

		if (factionObj.getUser() != user)
		{
			eigeneBestellungenImShopAnzeigen(db, user, factionObj, t);
		}
		else
		{
			alleBestellungenImShopAnzeigen(db, factionObj, t);
		}

		// Zuerst alle Ganymed-Transportdaten auslesen

		List<?> ganyEntryList = db.createQuery(
				"from FactionShopEntry where faction= :faction and type=2")
				.setEntity("faction", factionObj.getUser())
				.list();

		FactionShopEntry[] ganytransport = new FactionShopEntry[ganyEntryList.size()];
		int i = 0;

		for (Object aGanyEntryList : ganyEntryList)
		{
			ganytransport[i++] = (FactionShopEntry) aGanyEntryList;
		}

		final boolean handelErlaubt = istHandelErlaubt(user, factionObj);
		// Falls vorhanden jetzt eine Ganymed-Infozeile ausgeben
		if (ganytransport.length > 0)
		{
			ShopEntry shopEntryObj = new ShopGanyTransportEntry(ganytransport);

			t.setVar("entry.type.image", shopEntryObj.getImage(),
					"entry.name", shopEntryObj.getName(),
					"entry.link", shopEntryObj.getLink(),
					"entry.id", shopEntryObj.getID(),
					"entry.availability.name", shopEntryObj.getAvailabilityName(),
					"entry.availability.color", shopEntryObj.getAvailabilityColor(),
					"entry.availability", shopEntryObj.getAvailability(),
					"entry.price", shopEntryObj.getPriceAsText(),
					"entry.lpkosten", shopEntryObj.getLpKosten() > 0 ? Common.ln(shopEntryObj.getLpKosten()) : "",
					"entry.showamountinput", shopEntryObj.showAmountInput(),
					"entry.orderable", handelErlaubt);

			t.parse("shop.list", "shop.listitem", true);
		}

		// Nun den normalen Shop ausgeben
		List<?> shopentryList = db.createQuery(
				"from FactionShopEntry where faction = :faction and type!=2 order by minRank asc, price asc")
				.setEntity("faction", factionObj.getUser()).list();
		for (Object aShopentryList : shopentryList)
		{
			FactionShopEntry shopentry = (FactionShopEntry) aShopentryList;

			ShopEntry shopEntryObj = null;
			if (shopentry.getType() == FactionShopEntry.Type.SHIP)
			{
				shopEntryObj = new ShopShipEntry(shopentry);
			}
			else if (shopentry.getType() == FactionShopEntry.Type.ITEM)
			{
				shopEntryObj = new ShopResourceEntry(shopentry);
			}

			t.setVar("entry.type.image", shopEntryObj.getImage(),
					"entry.name", shopEntryObj.getName(),
					"entry.link", shopEntryObj.getLink(),
					"entry.id", shopEntryObj.getID(),
					"entry.rang", shopentry.getMinRank(),
					"entry.availability.name", shopEntryObj.getAvailabilityName(),
					"entry.availability.color", shopEntryObj.getAvailabilityColor(),
					"entry.availability", shopEntryObj.getAvailability(),
					"entry.price", shopEntryObj.getPriceAsText(),
					"entry.lpkosten", shopEntryObj.getLpKosten() > 0 ? Common.ln(shopEntryObj.getLpKosten()) : "",
					"entry.showamountinput", shopEntryObj.showAmountInput(),
					"entry.npcrang", factionUser.getOwnGrantableRank(shopentry.getMinRank()),
					"entry.orderable", handelErlaubt && shopentry.canBuy(user));

			t.parse("shop.list", "shop.listitem", true);
		}
		return t;
	}

	private void alleBestellungenImShopAnzeigen(Session db, FraktionsGuiEintrag factionObj, TemplateEngine t)
	{
		t.setVar("shop.owner", 1);

		List<?> orderentryList = db
				.createQuery(
						"from FactionShopOrder as fso "
								+ "where fso.shopEntry.faction = :faction and fso.status < 4 "
								+ "order by case when fso.status=0 then fso.status else fso.date end asc")
				.setInteger("faction", factionObj.getUser().getId()).list();
		for (Object anOrderentryList : orderentryList)
		{
			FactionShopOrder order = (FactionShopOrder) anOrderentryList;

			FactionShopEntry shopentry = order.getShopEntry();
			ShopEntry shopEntryObj = null;

			String entryadddata = "";
			if (shopentry.getType() == FactionShopEntry.Type.SHIP)
			{ // Schiff
				shopEntryObj = new ShopShipEntry(shopentry);

				entryadddata = "LK: " + order.getAddData();
			}
			else if (shopentry.getType() == FactionShopEntry.Type.ITEM)
			{ // Cargo
				shopEntryObj = new ShopResourceEntry(shopentry);
				entryadddata = "LK: " + order.getAddData();
			}
			else if (shopentry.getType() == FactionShopEntry.Type.TRANSPORT)
			{ // Ganytransport
				String[] tmp = StringUtils.split(order.getAddData(), "@");
				int ganyid = Integer.parseInt(tmp[0]);

				String[] coords = StringUtils.split(tmp[1], "->");

				entryadddata = ganyid + "<br />" + coords[0] + " - " + coords[1];
				shopEntryObj = new ShopGanyTransportEntry(new FactionShopEntry[]{shopentry});
			}

			User ownerobj = order.getUser();

			t.setVar("orderentry.name", shopEntryObj.getName(),
					"orderentry.adddata", entryadddata,
					"orderentry.owner", order.getUser().getId(),
					"orderentry.owner.name", Common._title(ownerobj.getName()),
					"orderentry.link", shopEntryObj.getLink(),
					"orderentry.id", order.getId(),
					"orderentry.price", Common.ln(order.getPrice()),
					"orderentry.lpkosten", order.getLpKosten() > 0 ? Common.ln(order.getLpKosten()) : "",
					"orderentry.count", Common.ln(order.getCount()),
					"orderentry.status", order.getStatus(),
					"orderentry.status.name", getStatusName(order.getStatus()),
					"orderentry.bgcolor", getStatusColor(order.getStatus()));

			t.parse("shop.shopownerlist.list", "shop.shopownerlist.listitem", true);
		}
	}

	private void eigeneBestellungenImShopAnzeigen(Session db, User user, FraktionsGuiEintrag factionObj, TemplateEngine t)
	{
		List<?> orderentryList = db
				.createQuery(
						"from FactionShopOrder as fso "
								+ "where fso.shopEntry.faction= :faction and fso.user= :user and fso.status<4")
				.setEntity("faction", factionObj.getUser())
				.setEntity("user", user)
				.list();
		for (Object anOrderentryList : orderentryList)
		{
			FactionShopOrder order = (FactionShopOrder) anOrderentryList;

			FactionShopEntry shopentry = order.getShopEntry();
			ShopEntry shopEntryObj;

			String entryadddata = "";
			if (shopentry.getType() == FactionShopEntry.Type.SHIP)
			{ // Schiff
				shopEntryObj = new ShopShipEntry(shopentry);
			}
			else if (shopentry.getType() == FactionShopEntry.Type.ITEM)
			{ // Cargo
				shopEntryObj = new ShopResourceEntry(shopentry);
			}
			else if (shopentry.getType() == FactionShopEntry.Type.TRANSPORT)
			{ // Ganytransport
				shopEntryObj = new ShopGanyTransportEntry(new FactionShopEntry[]{shopentry});

				String[] tmp = StringUtils.split(order.getAddData(), "@");

				Ship gany = (Ship) db.get(Ship.class, Integer.parseInt(tmp[0]));
				if (gany != null)
				{
					String ganyname = Common._plaintitle(gany.getName());

					String[] coords = StringUtils.split(tmp[1], "->");
					entryadddata = ganyname + " (" + gany.getId() + ")<br />nach " + coords[1];
				}
			}
			else
			{
				throw new RuntimeException("Unbekannter Shopeintrag-Typ '"
						+ shopentry.getType() + "'");
			}

			t.setVar(
					"orderentry.name", shopEntryObj.getName(),
					"orderentry.adddata", entryadddata,
					"orderentry.type.image", shopEntryObj.getImage(),
					"orderentry.link", shopEntryObj.getLink(),
					"orderentry.id", order.getId(),
					"orderentry.price", Common.ln(order.getPrice()),
					"orderentry.lpkosten", order.getLpKosten() > 0 ? Common.ln(shopentry.getLpKosten()*order.getCount()) : "",
					"orderentry.count", Common.ln(order.getCount()),
					"orderentry.status", getStatusName(order.getStatus()),
					"orderentry.bgcolor", getStatusColor(order.getStatus()));

			t.parse("shop.orderlist.list", "shop.orderlist.listitem", true);
		}
	}

	/**
	 * Zeigt die Meldeseite fuer LP der Fraktion an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public Object aktionMeldenAction(User faction, RedirectViewResult redirect)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.AKTION_MELDEN))
		{
			return new RedirectViewResult("default");
		}

		TemplateEngine t = templateViewResultFactory.createFor(this);
		erstelleMenue(t, factionObj);
		t.setVar("show.message", redirect != null ? redirect.getMessage() : null);
		t.setVar("show.aktionmelden", 1);
		return t;
	}

	/**
	 * Erstellt eine LP-Meldung bei der momentanen Fraktion.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 * @param meldungstext Der Beschreibungstext der Aktion
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult aktionsMeldungErstellenAction(User faction, String meldungstext)
	{
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);

		if (!factionObj.getSeiten().contains(FraktionsGuiEintrag.Seite.AKTION_MELDEN))
		{
			return new RedirectViewResult("default");
		}

		User factionUser = factionObj.getUser();

		if (meldungstext == null || meldungstext.trim().length() < 10)
		{
			addError("Bitte gib eine genaue Beschreibung deiner Aktion ein");
			return new RedirectViewResult("aktionMelden");
		}

		FraktionAktionsMeldung meldung = new FraktionAktionsMeldung(user, factionUser);
		meldung.setMeldungstext(meldungstext);
		db.persist(meldung);

        PM.send(user, factionUser.getId(), "LP Meldung", "Es ist eine neue Meldung zum Erhalt von Loyalitätspunkten eingegangen.");

		return new RedirectViewResult("aktionMelden").withMessage("Die Aktionsmeldung wurde der Fraktion erfolgreich übermittelt");
	}

	/**
	 * Leitet zur Default-Seite einer Fraktion weiter.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult defaultAction(User faction)
	{
		FraktionsGuiEintrag factionObj = ermittleFraktion(faction);
		return new RedirectViewResult(factionObj.getErsteSeite().getId());

	}
}
