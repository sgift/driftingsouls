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
package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.FactionPages;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.FactionOffer;
import net.driftingsouls.ds2.server.entities.FactionShopEntry;
import net.driftingsouls.ds2.server.entities.FactionShopOrder;
import net.driftingsouls.ds2.server.entities.FraktionAktionsMeldung;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Loyalitaetspunkte;
import net.driftingsouls.ds2.server.entities.ResourceLimit;
import net.driftingsouls.ds2.server.entities.ResourceLimit.ResourceLimitKey;
import net.driftingsouls.ds2.server.entities.UpgradeInfo;
import net.driftingsouls.ds2.server.entities.UpgradeJob;
import net.driftingsouls.ds2.server.entities.UpgradeMaxValues;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.entities.Versteigerung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextInstance;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.JumpNodeRouter;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Zeigt die Fraktionsseiten an.
 *
 * @author Christopher Jung
 */
@Module(name = "ersteigern")
public class ErsteigernController extends TemplateController
{
	/**
	 * Ein Eintrag im Shop.
	 *
	 * @author Christopher Jung
	 */
	private abstract static class ShopEntry
	{
		private int id;
		private int factionID;
		private FactionShopEntry.Type type;
		private String resource;
		private long price;
		private long lpKosten;
		private int availability;

		/**
		 * Konstruktor.
		 *
		 * @param data Die SQL-Ergebniszeile zum Eintrag
		 */
		public ShopEntry(FactionShopEntry data)
		{
			this.id = data.getId();
			this.factionID = data.getFaction();
			this.type = data.getType();
			this.resource = data.getResource();
			this.price = data.getPrice();
			this.lpKosten = data.getLpKosten();
			this.availability = data.getAvailability();
		}

		/**
		 * Gibt die ID des Eintrags zurueck.
		 *
		 * @return Die ID
		 */
		public int getID()
		{
			return this.id;
		}

		/**
		 * Gibt die ID der Fraktion zurueck, der der Eintrag gehoert.
		 *
		 * @return Die ID der Fraktion
		 */
		@SuppressWarnings("unused")
		public int getFactionID()
		{
			return this.factionID;
		}

		/**
		 * Gibt den Typ des Eintrags zurueck.
		 *
		 * @return Der Typ
		 */
		public FactionShopEntry.Type getType()
		{
			return this.type;
		}

		/**
		 * Gibt den Namen des Eintrags zurueck.
		 *
		 * @return Der Name
		 */
		public abstract String getName();

		/**
		 * Gibt das zum Eintrag gehoerende Bild zurueck.
		 *
		 * @return Das Bild
		 */
		public abstract String getImage();

		/**
		 * Gibt einen zum Eintrag gehoerenden Link zurueck.
		 *
		 * @return Der Link
		 */
		public abstract String getLink();

		/**
		 * Gibt die LP-Kosten fuer den Eintrag zurueck.
		 *
		 * @return Die LP-Kosten
		 */
		public long getLpKosten()
		{
			return this.lpKosten;
		}

		/**
		 * Gibt die Verfuegbarkeit des Eintrags zurueck.
		 *
		 * @return Die Verfuegbarkeit
		 */
		public int getAvailability()
		{
			return this.availability;
		}

		/**
		 * Gibt die Verfuegbarkeit des Eintrags als Text zurueck.
		 *
		 * @return Die Verfuegbarkeit als Text
		 */
		public String getAvailabilityName()
		{
			switch (this.getAvailability())
			{
				case 0:
					return "Genug vorhanden";
				case 1:
					return "Nur noch 1-3 vorhanden";
				case 2:
					return "Nicht verf&uuml;gbar";
			}
			return "";
		}

		/**
		 * Gibt die mit der Verfuegbarkeit assoziierte Farbe zurueck.
		 *
		 * @return Die Farbe der Verfuegbarkeit
		 */
		public String getAvailabilityColor()
		{
			switch (this.getAvailability())
			{
				case 0:
					return "#55DD55";
				case 1:
					return "#FFFF44";
				case 2:
					return "#CC2222";
			}
			return "";
		}

		/**
		 * Soll die Verkaufsmenge angezeigt werden?
		 *
		 * @return <code>true</code>, falls die Verkaufsmenge angezeigt werden soll
		 */
		public boolean showAmountInput()
		{
			return true;
		}

		/**
		 * Gibt den Kaufpreis zurueck.
		 *
		 * @return Der Kaufpreis
		 */
		public long getPrice()
		{
			return price;
		}

		/**
		 * Gibt den Kaufpreis als Text zurueck.
		 *
		 * @return Der Kaufpreis als Text
		 */
		public String getPriceAsText()
		{
			return Common.ln(this.getPrice());
		}

		/**
		 * Gibt den Verkaufsinhalt, den der Eintrag enthaelt, zurueck. Der Typ ist Abhaengig vom
		 * Typen des Eintrags.
		 *
		 * @return Der Verkaufsinhalt
		 */
		public String getResource()
		{
			return this.resource;
		}
	}

	/**
	 * Repraesentiert ein Shopeintrag, welcher ein Schiff enthaelt.
	 *
	 * @author Christopher Jung
	 */
	private static class ShopShipEntry extends ShopEntry
	{
		private ShipTypeData shiptype;

		/**
		 * Konstruktor.
		 *
		 * @param data Die SQL-Ergebniszeile des Shopeintrags
		 */
		public ShopShipEntry(FactionShopEntry data)
		{
			super(data);

			this.shiptype = Ship.getShipType(Integer.parseInt(this.getResource()));
		}

		@Override
		public String getName()
		{
			return this.shiptype.getNickname();
		}

		@Override
		public String getImage()
		{
			return this.shiptype.getPicture();
		}

		@Override
		public String getLink()
		{
			return Common.buildUrl("default", "module", "schiffinfo", "ship", shiptype.getTypeId());
		}
	}

	/**
	 * Repraesentiert ein Shopeintrag, welcher eine Resource enthaelt.
	 *
	 * @author Christopher Jung
	 */
	private static class ShopResourceEntry extends ShopEntry
	{
		private ResourceEntry resourceEntry;

		/**
		 * Konstruktor.
		 *
		 * @param data Die SQL-Ergebniszeile des Shopeintrags
		 */
		public ShopResourceEntry(FactionShopEntry data)
		{
			super(data);

			Cargo cargo = new Cargo();
			cargo.addResource(Resources.fromString(this.getResource()), 1);
			cargo.setOption(Cargo.Option.SHOWMASS, false);
			cargo.setOption(Cargo.Option.LARGEIMAGES, true);
			this.resourceEntry = cargo.getResourceList().iterator().next();
		}

		@Override
		public String getName()
		{
			return Cargo.getResourceName(resourceEntry.getId());
		}

		@Override
		public String getImage()
		{
			return resourceEntry.getImage();
		}

		@Override
		public String getLink()
		{
			return Common.buildUrl("details", "module", "iteminfo", "item", resourceEntry
					.getId().getItemID());
		}

		@Override
		public String getAvailabilityName()
		{
			return super.getAvailabilityName();
		}
	}

	/**
	 * Repraesentiert ein Shopeintrag, welcher einen Ganymede-Transport enthaelt.
	 *
	 * @author Christopher Jung
	 */
	private static class ShopGanyTransportEntry extends ShopEntry
	{
		/**
		 * Die Schiffstypen-ID einer Ganymede.
		 */
		public static final int SHIPTYPE_GANYMEDE = 33;

		private long minprice = Long.MAX_VALUE;
		private long maxprice = Long.MIN_VALUE;
		private int ganytransid;

		/**
		 * Konstruktor.
		 *
		 * @param data Die SQL-Ergebniszeile des Shopeintrags
		 */
		public ShopGanyTransportEntry(FactionShopEntry[] data)
		{
			super(data[0]);

			for (FactionShopEntry aData : data)
			{
				if (aData.getPrice() < this.minprice)
				{
					this.minprice = aData.getPrice();
				}
				if (aData.getPrice() > this.maxprice)
				{
					this.maxprice = aData.getPrice();
				}
				this.ganytransid = data[0].getId();
			}
		}

		@Override
		public long getLpKosten()
		{
			return 0;
		}

		@Override
		public int getID()
		{
			return ganytransid;
		}

		@Override
		public long getPrice()
		{
			return (this.minprice != this.maxprice) ? (this.minprice + this.maxprice) / 2
					: this.minprice;
		}

		@Override
		public String getPriceAsText()
		{
			return (this.minprice != this.maxprice) ? (Common.ln(this.minprice) + " - " + Common
					.ln(this.maxprice)) : (Common.ln(this.minprice)) + "<br />pro System";
		}

		@Override
		public String getName()
		{
			return "Ganymede-Transport";
		}

		@Override
		public String getImage()
		{
			return "./data/interface/ganymede_transport.png";
		}

		@Override
		public String getLink()
		{
			return "#";
		}

		@Override
		public boolean showAmountInput()
		{
			return false;
		}

		@Override
		public int getAvailability()
		{
			return 0;
		}
	}

	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public ErsteigernController(Context context)
	{
		super(context);

		setPageTitle("Fraktionen");
	}

	private int ermittleFraktionUndErstelleMenue(int faction)
	{
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		if (faction == 0)
		{
			faction = ermittleStandardFraktionFuerSpieler(db, user);
		}

		validiereFraktion(faction);

		TemplateEngine t = getTemplateEngine();

		// Die Templatevariablen duerfen nur einmal gesetzt werden (redirects!)
		if (t.getVar("global.faction").isEmpty())
		{
			FactionPages pages = Faction.get(faction).getPages();
			for (String aPage : pages.getPages())
			{
				t.setVar("faction." + aPage, 1);
			}

			// Fraktionsmenue

			t.setBlock("_ERSTEIGERN", "global.factionmenu.listitem", "global.factionmenu.list");

			Map<Integer, Faction> factions = Faction.getFactions();
			for (Faction factionObj : factions.values())
			{
				if (!factionObj.getPages().isEnabled())
				{
					continue;
				}

				User aFactionUser = (User) db.get(User.class, factionObj.getID());
				if (aFactionUser == null)
				{
					continue;
				}
				t.setVar(
						"item.faction.name", Common._title(aFactionUser.getName()),
						"item.faction.id", factionObj.getID());

				t.parse("global.factionmenu.list", "global.factionmenu.listitem", true);
			}

			User factionuser = (User) db.get(User.class, faction);

			t.setVar(
					"user.konto", Common.ln(user.getKonto()),
					"global.faction", faction,
					"global.faction.name", Common._title(factionuser.getName()));
		}

		return faction;
	}

	private void validiereFraktion(int faction)
	{
		if (faction == 0)
		{
			throw new ValidierungException("Keine Fraktion will mit ihnen zu handeln solange die Beziehungen feindlich sind");
		}

		if (Faction.get(faction) == null)
		{
			throw new ValidierungException("Die angegebene Fraktion verf&uuml;gt &uuml;ber keine eigene Seite");
		}
	}

	private boolean istHandelErlaubt(User user, int faction)
	{
		return user.getRelation(faction) != User.Relation.ENEMY
				&& user.getRelations().fromOther.get(faction) != User.Relation.ENEMY;
	}

	private int ermittleStandardFraktionFuerSpieler(Session db, User user)
	{
		int faction = 0;
		if (Faction.get(user.getId()) != null)
		{
			faction = user.getId();
		}
		else
		{
			Map<Integer, Faction> factions = Faction.getFactions();
			for (Map.Entry<Integer, Faction> integerFactionEntry : factions.entrySet())
			{
				int aFactionID = integerFactionEntry.getKey();
				Faction factionObj = integerFactionEntry.getValue();
				if (!factionObj.getPages().isEnabled())
				{
					continue;
				}

				User aFactionUser = (User) db.get(User.class, factionObj.getID());
				if (aFactionUser == null)
				{
					continue;
				}

				if ((user.getRelation(aFactionID) != User.Relation.ENEMY)
						&& (user.getRelations().fromOther.get(aFactionID) != User.Relation.ENEMY))
				{
					faction = aFactionID;
					break;
				}
			}
		}
		return faction;
	}

	/**
	 * Aendert das System, in dem ersteigerte Dinge gespawnt werden sollen.
	 *
	 * @param favsys Die ID des neuen Systems, in dem ersteigerte Dinge gespawnt werden sollen
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void changeDropZoneAction(int faction, StarSystem favsys)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);

		if (!Faction.get(faction).getPages().hasPage("versteigerung"))
		{
			redirect();
			return;
		}

		User user = (User) getUser();
		TemplateEngine t = getTemplateEngine();

		if (ermittleMoeglicheDropZones().contains(favsys))
		{
			user.setGtuDropZone(favsys.getID());
			t.setVar("show.newcoords", 1);
		}

		redirect();
	}

	/**
	 * Gibt ein Gebot auf eine Versteigerung ab bzw zeigt, falls kein Gebot angegeben wurde, die
	 * angegebene Versteigerung an.
	 *
	 * @param bid Der gebotene Betrag oder 0
	 * @param entry Die Auktion auf die geboten werden soll
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void bidEntryAction(int faction, int bid, @UrlParam(name = "auk") Versteigerung entry)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);

		if (!Faction.get(faction).getPages().hasPage("versteigerung"))
		{
			redirect();
			return;
		}

		User user = (User) getUser();

		if (!istHandelErlaubt(user, faction))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			redirect();
			return;
		}

		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		if (entry == null || (entry.getOwner().getId() == user.getId()))
		{
			addError("Sie k&ouml;nnen nicht bei eigenen Versteigerungen mitbieten");
			redirect();
			return;
		}

		// Wenn noch kein Gebot abgegeben wurde -> Versteigerung anzeigen
		if (bid == 0)
		{
			int entrywidth = entry.isObjectFixedImageSize() ? 50 : 0;
			long entrycount = entry.getObjectCount();
			String entryname = entry.getObjectName();
			String entryimage = entry.getObjectPicture();
			String entrylink = entry.getObjectUrl();

			String bietername = "";

			User bieter = entry.getBieter();

			if (bieter.getId() == faction)
			{
				bietername = bieter.getName();
			}
			else if (bieter.getId() == user.getId())
			{
				bietername = bieter.getName();
			}
			else if (hasPermission("fraktionen", "bietername"))
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
					"entry.type.name", StringEscapeUtils.escapeJavaScript(StringUtils.replaceChars(entryname, '"', '\'')),
					"entry.type.image", entryimage,
					"entry.link", entrylink,
					"entry.width", entrywidth,
					"entry.height", entrywidth,
					"entry.count", entrycount,
					"bid.player", Common._title(bietername),
					"bid.player.id", bieter.getId(),
					"bid.price", cost,
					"bid.id", entry.getId());
			return;
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
				if (entry.getBieter().getId() != faction)
				{
					User bieter = entry.getBieter();
					User factionUser = (User) db.get(User.class, faction);

					PM.send(factionUser, entry.getBieter().getId(),
							"Bei Versteigerung &uuml;berboten",
							"Sie wurden bei der Versteigerung um '"
									+ entryname
									+ "' &uuml;berboten. Die von ihnen gebotenen RE in H&ouml;he von "
									+ Common.ln(entry.getPreis())
									+ " wurden auf ihr Konto zur&uuml;ck&uuml;berwiesen.\n\nGaltracorp Unlimited");

					bieter.transferMoneyFrom(faction, entry.getPreis(),
							"R&uuml;ck&uuml;berweisung Gebot #2" + entry.getId() + " '" + entryname
									+ "'", false, UserMoneyTransfer.Transfer.SEMIAUTO);
				}

				if (entry.getTick() < ticks + 3)
				{
					entry.setTick(ticks + 3);
				}
				entry.setBieter(user);
				entry.setPreis(bid);

				User gtu = (User) db.get(User.class, faction);
				gtu.transferMoneyFrom(user.getId(), bid, "&Uuml;berweisung Gebot #2"
						+ entry.getId() + " '" + entryname + "'", false, UserMoneyTransfer.Transfer.SEMIAUTO);

				user.setTemplateVars(t);
				t.setVar("user.konto", Common.ln(user.getKonto()), "show.highestbid", 1);
			}
			else
			{
				t.setVar("show.lowres", 1);
			}
		}

		redirect();
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
	 * @param ack yes falls die Uberweisung bestaetigt wurde (Sicherheitsabfrage)
	 * @param count Die zu ueberweisenden RE
	 * @param to Der Spieler an den ueberwiesen werden soll
	 * @param requestToken Ein Sicherheitstoken zur Bestaetigung der Ueberweisung
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void ueberweisenAction(int faction, String to, String ack, int count, @UrlParam(name = "token") String requestToken)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		if (!Faction.get(faction).getPages().hasPage("bank"))
		{
			redirect();
			return;
		}

		User user = (User) getUser();
		if (!istHandelErlaubt(user, faction))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			redirect();
			return;
		}

		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		if (user.getKonto().compareTo(new BigDecimal(count).toBigInteger()) < 0)
		{
			count = user.getKonto().intValue();
		}

		if (count <= 0)
		{
			redirect();
			return;
		}

		User tmp = User.lookupByIdentifier(to);
		if (tmp == null)
		{
			addError("Der angegebene Spieler konnte nicht gefunden werden");
			redirect("bank");
			return;
		}

		UeberweisungsTokenContainer token = getContext().get(UeberweisungsTokenContainer.class);

		// Falls noch keine Bestaetigung vorliegt: Bestaetigung der Ueberweisung erfragen
		if (!ack.equals("yes") || !token.getToken().equals(requestToken))
		{
			token.generateNewToken();

			t.setVar(
					"show.ueberweisen", 1,
					"ueberweisen.betrag", Common.ln(count),
					"ueberweisen.betrag.plain", count,
					"ueberweisen.to.name", Common._title(tmp.getName()),
					"ueberweisen.to", tmp.getId(),
					"ueberweisen.token", token.getToken());

			return;
		}

		int ticks = getContext().get(ContextCommon.class).getTick();

		tmp.transferMoneyFrom(user.getId(), count, "&Uuml;berweisung vom "
				+ Common.getIngameTime(ticks));
		User factionUser = (User) db.get(User.class, Faction.BANK);

		PM.send(factionUser, tmp.getId(), "RE &uuml;berwiesen", user.getNickname()
				+ " hat dir soeben " + Common.ln(count) + " RE &uuml;berwiesen");
		PM.send(factionUser, user.getId(), "RE &uuml;berwiesen  an " + tmp.getNickname(),
				"Du hast " + tmp.getNickname() + " soeben " + Common.ln(count)
						+ " RE &uuml;berwiesen");

		user.setTemplateVars(t);
		t.setVar("user.konto", Common.ln(user.getKonto()));

		redirect("bank");
	}

	/**
	 * Aendert den Anzeigetyp fuer Kontotransaktionen.
	 *
	 * @param type Der neue Anzeigetyp (0-2)
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void showKontoTransactionTypeAction(int faction, int type)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		if (!Faction.get(faction).getPages().hasPage("bank"))
		{
			redirect();
			return;
		}

		User user = (User) getUser();

		if ((type >= 0) && (type < 3))
		{
			user.setUserValue("TBLORDER/factions/konto_maxtype", Integer.toString(type));
		}
		redirect("bank");
	}

	/**
	 * Shows the Bank Page.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void bankAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		if (!Faction.get(faction).getPages().hasPage("bank"))
		{
			redirect();
			return;
		}

		TemplateEngine t = this.getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		t.setVar("show.bank", 1);

		// Auwahl max. Transaktionstyp in der Kontoanzeige generieren
		int transtype = Integer.parseInt(user.getUserValue("TBLORDER/factions/konto_maxtype"));

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

	}

	/**
	 * Zeigt die Seite mit diversen weiteren Infos an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void otherAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		if (!Faction.get(faction).getPages().hasPage("other"))
		{
			redirect();
			return;
		}

		TemplateEngine t = this.getTemplateEngine();
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
			ResourceLimitKey resourceLimitKey = new ResourceLimitKey(tradepost, res.getId());
			ResourceLimit limit = (ResourceLimit) db.get(ResourceLimit.class, resourceLimitKey);

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
			ResourceLimitKey resourceLimitKey = new ResourceLimitKey(tradepost, resource.getId());
			SellLimit limit = (SellLimit)db.get(SellLimit.class, resourceLimitKey);
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
	public void angeboteAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		if (!Faction.get(faction).getPages().hasPage("angebote"))
		{
			redirect();
			return;
		}

		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		t.setVar("show.angebote", 1);

		t.setBlock("_ERSTEIGERN", "angebote.item", "angebote.list");
		t.setBlock("_ERSTEIGERN", "angebote.emptyitem", "none");

		t.setVar("none", "");

		int count = 0;
		List<?> angebote = db.createQuery("from FactionOffer where faction=:faction")
				.setInteger("faction", faction)
				.list();
		for (Object anAngebote : angebote)
		{
			FactionOffer offer = (FactionOffer) anAngebote;

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
	}

	/**
	 * Zeigt die laufenden Versteigerungen an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void versteigerungAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = this.getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("versteigerung"))
		{
			redirect();
			return;
		}

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
			String entryname = StringUtils.replaceChars(objectname, '"', '\'');
			//String entryname = StringEscapeUtils.escapeJavaScript(replaceobjectname);

			int entrywidth = entry.isObjectFixedImageSize() ? 50 : 0;

			String bietername = "";

			if (bieter.getId() == faction)
			{
				bietername = bieter.getName();
			}
			else if (bieter == user)
			{
				bietername = bieter.getName();
			}
			else if (hasPermission("fraktionen", "bietername"))
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

			if (hasPermission("fraktionen", "anbietername") && (entry.getOwner().getId() != faction)
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
					"entry.user", (entry.getOwner().getId() != faction),
					"entry.bidAllowed", istHandelErlaubt(user, faction),
					"entry.ownauction", (entry.getOwner() == user));

			t.parse("entry.list", "entry.listitem", true);
		}

		t.setBlock("_ERSTEIGERN", "gtu.dropzones.listitem", "gtu.dropzones.list");


		dropZoneAuswahlAnzeigen(t, db, user);
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
					"gtuzwischenlager.x", aposten.getX(), "gtuzwischenlager.y", aposten.getY(),
					"gtuzwischenlager.system", aposten.getSystem());

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

		int defaultDropZone = new ConfigService().getValue(Integer.class, "gtudefaultdropzone");

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
	public void generalAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();

		if (!Faction.get(faction).getPages().hasPage("general"))
		{
			redirect();
			return;
		}

		t.setVar("show.general", 1, "global.faction.text", Common._text(Faction.get(faction)
				.getPages().getFactionText()));

	}

	/**
	 * Berechnet die Kosten eines Transportauftrags und speichert ihn in der Datenbank.
	 *
	 * @param sourcesystem Das Ausgangssystem
	 * @param ganymedeid Die ID der zu transportierenden Ganymede
	 * @param targetsystem Die ID des Zielsystems
	 * @param targetx Die Ziel-X-Koordinate
	 * @param targety Die Ziel-Y-Koordinate
	 * @param transport Sofert der Wert <code>1</code>, wird der Transportauftrag
	 * bestaetigt und abgespeichert
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void shopOrderGanymedeSummaryAction(int faction, int sourcesystem, int ganymedeid, int targetsystem, int targetx, int targety, int transport)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		if (!Faction.get(faction).getPages().hasPage("shop"))
		{
			redirect();
			return;
		}

		if (!istHandelErlaubt(user, faction))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			redirect();
			return;
		}

		FactionShopEntry shopentry = (FactionShopEntry) db
				.createQuery("from FactionShopEntry where faction=:faction and type=2")
				.setInteger("faction", faction)
				.setMaxResults(1)
				.uniqueResult();

		if (shopentry == null)
		{
			this.redirect();
			return;
		}

		Ship gany = (Ship) db.get(Ship.class, ganymedeid);

		if (gany == null ||
				gany.getOwner().getId() != user.getId() ||
				gany.getSystem() != sourcesystem ||
				gany.getType() != ShopGanyTransportEntry.SHIPTYPE_GANYMEDE)
		{
			addError("Die angegebene Ganymede konnte im Ausgangssystem nicht lokalisiert werden");
			unsetParameter("sourcesystem");
			unsetParameter("ganymedeid");

			redirect("shopOrderGanymede");
			return;
		}

		FactionShopOrder sameorder = (FactionShopOrder) db
				.createQuery("from FactionShopOrder where user=:user and addData like :pattern and status < 4")
				.setParameter("user", user)
				.setParameter("pattern", gany.getId() + "@%")
				.uniqueResult();
		if (sameorder != null)
		{
			addError("Es existiert bereits ein Transport-Auftrag f&uuml;r diese Ganymede");
			unsetParameter("sourcesystem");
			unsetParameter("ganymedeid");

			redirect("shopOrderGanymede");
			return;
		}

		StarSystem system = (StarSystem) db.get(StarSystem.class, targetsystem);
		if ((system.getAccess() == StarSystem.AC_ADMIN)
				&& !user.hasFlag(User.FLAG_VIEW_ALL_SYSTEMS))
		{
			addError("Die angegebene Zielsystem konnte nicht lokalisiert werden");
			unsetParameter("targetsystem");

			redirect("shopOrderGanymede");
			return;
		}
		else if ((system.getAccess() == StarSystem.AC_NPC)
				&& !user.hasFlag(User.FLAG_VIEW_ALL_SYSTEMS)
				&& !user.hasFlag(User.FLAG_VIEW_SYSTEMS))
		{
			addError("Die angegebene Zielsystem konnte nicht lokalisiert werden");
			unsetParameter("targetsystem");

			redirect("shopOrderGanymede");
			return;
		}

		if ((targetx < 1) || (targetx > system.getWidth()) || (targety < 1)
				|| (targety > system.getHeight()))
		{
			addError("Die angegebene Zielkoordinaten konnten im Zielsystem nicht lokalisiert werden");
			unsetParameter("targetx");
			unsetParameter("targety");

			redirect("shopOrderGanymede");
			return;
		}

		// Weg finden und Preis ausrechnen
		Map<Integer, List<JumpNode>> jumpnodes = new HashMap<>();
		List<?> jnList = db.createQuery("from JumpNode where hidden=0 and (systemOut!=:source or system=:source)")
				.setInteger("source", sourcesystem)
				.list();

		for (Object obj : jnList)
		{
			JumpNode jn = (JumpNode) obj;
			if (!jumpnodes.containsKey(jn.getSystem()))
			{
				jumpnodes.put(jn.getSystem(), new ArrayList<JumpNode>());
			}
			jumpnodes.get(jn.getSystem()).add(jn);
		}

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
					.setParameter("faction", faction)
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
			User factionUser = (User) getDB().get(User.class, faction);
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

			PM.send(user, faction, "[auto] Shop-Bestellung [Ganymede]",
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

			t.setVar("show.message", "Bestellung &uuml;ber 1 Ganymede-Transport des Objekts "
					+ gany.getId() + " von " + sourcesystem + ":" + gany.getX() + "/"
					+ gany.getY() + " nach " + targetsystem + ":" + targetx + "/" + targety
					+ " f&uuml;r " + Common.ln(totalcost)
					+ " erhalten und vom System best&auml;tigt.<br />Einen angenehmen Tag noch!");

			redirect("shop");
		}
	}

	/**
	 * Zeigt die GUI zur Erstellung eines Ganymede-Transportauftrags.
	 *
	 * @param sourcesystem Das Ausgangssystem
	 * @param ganymedeid Die ID der zu transportierenden Ganymede
	 * @param targetsystem Die ID des Zielsystems
	 * @param targetx Die Ziel-X-Koordinate
	 * @param targety Die Ziel-Y-Koordinate
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void shopOrderGanymedeAction(int faction, int sourcesystem, int ganymedeid, int targetsystem, int targetx, int targety)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();
		org.hibernate.Session db = getDB();

		if (!Faction.get(faction).getPages().hasPage("shop"))
		{
			redirect();
			return;
		}

		if (!istHandelErlaubt(user, faction))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			redirect();
			return;
		}

		FactionShopEntry entry = (FactionShopEntry) db
				.createQuery("from FactionShopEntry where faction=:faction and type=2")
				.setInteger("faction", faction)
				.setMaxResults(1)
				.uniqueResult();

		if (entry == null)
		{
			this.redirect();
			return;
		}

		// Wenn alle Parameter eingegebene wurden -> shopOrderGanymedeSummary
		if (targetsystem != 0 && ganymedeid != 0
				&& targetx != 0 && targety != 0 && sourcesystem != 0)
		{

			redirect("shopOrderGanymedeSummary");
			return;
		}

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
			return;
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
			if ((system.getAccess() == StarSystem.AC_ADMIN)
					&& !user.hasFlag(User.FLAG_VIEW_ALL_SYSTEMS))
			{
				continue;
			}
			else if ((system.getAccess() == StarSystem.AC_NPC)
					&& !user.hasFlag(User.FLAG_VIEW_ALL_SYSTEMS)
					&& !user.hasFlag(User.FLAG_VIEW_SYSTEMS))
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
	}

	/**
	 * Fuehrt eine Bestellung im Shop aus. Der User muss dazu eine gewuenschte Lieferposition
	 * angeben. Wenn diese noch nicht angegeben wurde, wird sie erfragt.
	 *
	 * @param shopentry Die ID des Shopeintrags, der bestellt werden soll
	 * @param ordercount Die Liefermenge
	 * @param ordersys Das Liefersystem
	 * @param orderx Die X-Komponente der Lieferkoordinate
	 * @param ordery Die Y-Komponente der Lieferkoordinate
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void shopOrderAction(int faction, FactionShopEntry shopentry, int ordercount, int ordersys, int orderx, int ordery)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("shop"))
		{
			redirect();
			return;
		}

		if (!istHandelErlaubt(user, faction))
		{
			addError("Die angegebene Fraktion weigert sich mit ihnen zu handeln solange die Beziehungen feindlich sind");
			redirect();
			return;
		}

		if (shopentry == null)
		{
			t.setVar("show.message",
					"<span style=\"color:red\">Es existiert kein passendes Angebot</span>");
			redirect("shop");
			return;
		}

		if (!shopentry.canBuy(user))
		{
			t.setVar("show.message",
					"<span style=\"color:red\">Es existiert kein passendes Angebot</span>");
			redirect("shop");
			return;
		}

		if (shopentry.getAvailability() == 2)
		{
			t.setVar("show.message",
					"<span style=\"color:red\">Das Angebot ist nicht verf&uuml;gbar</span>");
			redirect("shop");
			return;
		}

		// Ganymed-Transporte verarbeiten
		if (shopentry.getType() == FactionShopEntry.Type.TRANSPORT)
		{
			redirect("shopOrderGanymede");

			return;
		}

		if (ordercount < 1)
		{
			redirect("shop");
			return;
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
			t.setVar("show.message",
					"<span style=\"color:red\">Sie verf&uuml;gen nicht &uuml;ber genug Geld</span>");
			redirect("shop");
			return;
		}

		User factionUser = (User) db.get(User.class, faction);
		if (user.getLoyalitaetspunkteTotalBeiNpc(factionUser) < entry.getLpKosten() * ordercount)
		{
			t.setVar("show.message",
					"<span style=\"color:red\">Sie verfügen nicht über genug Loyalitätspunkte</span>");
			redirect("shop");
			return;
		}

		if (ordersys == 0 || orderx == 0 || ordery == 0)
		{
			t.setVar("show.shopOrderLocation", 1,
					"order.count", ordercount,
					"order.name", entry.getName(),
					"order.entry", entry.getID());
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

			PM.send(user, faction, "[auto] Shop-Bestellung", "Besteller: [userprofile="
					+ user.getId() + "]" + user.getName() + " (" + user.getId()
					+ ")[/userprofile]\nObjekt: " + entry.getName() + "\nMenge: " + ordercount
					+ "\nLieferkoordinaten: " + ordersys + ":" + orderx + "/" + ordery
					+ "\nZeitpunkt: " + Common.date("d.m.Y H:i:s"));

			t.setVar(
					"show.message",
					"Bestellung &uuml;ber "
							+ ordercount
							+ "x "
							+ entry.getName()
							+ " f&uuml;r "
							+ Common.ln(entry.getPrice() * ordercount) + " RE"
							+ (entry.getLpKosten() > 0 ? " und " + Common.ln(entry.getLpKosten()) + " LP " : "")
							+ " erhalten und vom System best&auml;tigt.<br />Sollten noch R&uuml;ckfragen bestehend so wird sich ein Sachbearbeiter bei ihnen melden.<br />Einen angenehmen Tag noch!");

			redirect("shop");
		}
	}

	/**
	 * Erstellt einen neuen Shop-Eintrag.
	 *
	 * @param entryCost Die Kosten des Eintrags in LP
	 * @param entryLpKosten Die Kosten des Eintrags in RE
	 * @param entryType Der Typ des Eintrags (ship,item,transport)
	 * @param entryTypeId Die ID der anzubietenden Ware des angegebenen Eintragtyps
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void shopEntryCreate(int faction, String entryType, String entryTypeId, int entryCost, int entryLpKosten)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("shop"))
		{
			redirect();
			return;
		}

		if (faction == user.getId())
		{
			FactionShopEntry.Type type = null;
			switch (entryType)
			{
				case "ship":
					type = FactionShopEntry.Type.SHIP;
					if (!NumberUtils.isNumber(entryTypeId))
					{
						t.setVar("show.message", "<span style=\"color:red\">Format ungueltig</span>");
						redirect("shop");
						return;
					}
					ShipType st = (ShipType) db.get(ShipType.class, Integer.parseInt(entryTypeId));
					if (st == null)
					{
						t.setVar("show.message", "<span style=\"color:red\">Kein bekannter Schiffstyp</span>");
						redirect("shop");
						return;
					}
					break;
				case "item":
					type = FactionShopEntry.Type.ITEM;
					if (ItemID.fromString(entryTypeId) == null)
					{
						t.setVar("show.message", "<span style=\"color:red\">Format ungueltig</span>");

						redirect("shop");
						return;
					}
					break;
				case "transport":
					type = FactionShopEntry.Type.TRANSPORT;
					if (!NumberUtils.isNumber(entryTypeId) && !"*".equals(entryTypeId))
					{
						t.setVar("show.message", "<span style=\"color:red\">Format ungueltig</span>");

						redirect("shop");
						return;
					}
					break;
			}

			FactionShopEntry entry = new FactionShopEntry(faction, type, entryTypeId);
			entry.setAvailability(0);
			entry.setPrice(entryCost);
			entry.setLpKosten(entryLpKosten);

			db.persist(entry);
		}

		redirect("shop");
	}

	/**
	 * Aendert einen Shopeintrag.
	 *
	 * @param availability Die neue Verfuegbarkeit
	 * @param entryLpKosten Die LP-Kosten
	 * @param entryPrice Der Preis
	 * @param entryRang Der Rang
	 * @param operation Die auszufuehrende Aktion (ändern, löschen)
	 * @param shopentry Die ID des Shopeintrags
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void shopChangeEntryAction(int faction, String operation, FactionShopEntry shopentry, int availability, int entryRang, long entryPrice, long entryLpKosten)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("shop"))
		{
			redirect();
			return;
		}

		if (faction == user.getId())
		{
			if ((shopentry == null) || (shopentry.getFaction() != faction))
			{
				addError("Es konnte kein passender Shopeintrag gefunden werden");
				redirect("shop");
				return;
			}

			if ("löschen".equalsIgnoreCase(operation))
			{
				if (shopentry.getAnzahlOffeneBestellungen() > 0)
				{
					addError("Es gibt noch offene Bestellungen zu diesem Shopeintrag");
					redirect("shop");
					return;
				}

				db.delete(shopentry);

				t.setVar("show.message", "Eintrag gelöscht");
				redirect("shop");
				return;
			}

			if (availability < 0 || availability > 2)
			{
				addError("Ung&uuml;ltiger Status");
				redirect("shop");
				return;
			}

			shopentry.setAvailability(availability);
			shopentry.setMinRank(entryRang);
			shopentry.setPrice(entryPrice);
			shopentry.setLpKosten(entryLpKosten);

			t.setVar("show.message", "Eintrag geaendert");
		}
		redirect("shop");
	}

	/**
	 * Aendert den Auftragsstatus einer Bestellung.
	 *
	 * @param orderentry Die ID des Auftrags
	 * @param orderstatus Der neue Auftragsstatus
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void changeShopOrderStatusAction(int faction, FactionShopOrder orderentry, int orderstatus)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("shop"))
		{
			redirect();
			return;
		}

		if (faction == user.getId())
		{
			if ((orderentry == null) || (orderentry.getStatus() > 3)
					|| (orderentry.getShopEntry().getFaction() != faction))
			{
				addError("Es konnte kein passender Ordereintrag gefunden werden");
				redirect("shop");
				return;
			}

			if (orderstatus < 0 || orderstatus > 4)
			{
				addError("Ung&uuml;ltiger Status");
				redirect("shop");
				return;
			}

			orderentry.setStatus(orderstatus);

			t.setVar("show.message", "Neuer Status erfolgreich zugewiesen");
		}
		redirect("shop");
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
	 * @param base Die ID des auszubauenden Asteroiden
	 * @param bar Die Zahlungsmethode, {@code true} bedeutet Barzahlung, sonst Abbuchung
	 * @param cargo Die ID der Cargoerweiterung
	 * @param colonizer Die ID des zu verwendenden Colonizers
	 * @param felder Die ID der Felderweiterung
	 * @param order Soll wirklich bestellt werden (bestellen)?
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@SuppressWarnings("unchecked")
	@Action(ActionType.DEFAULT)
	public void ausbauAction(int faction,
							 @UrlParam(name = "astiid") Base base,
							 @UrlParam(name = "colonizerid") Ship colonizer,
							 UpgradeInfo felder,
							 UpgradeInfo cargo,
							 boolean bar,
							 String order)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("ausbau"))
		{
			redirect();
			return;
		}

		if ("bestellen".equals(order) && felder != null && cargo != null)
		{
			if (base == null)
			{
				addError("Der angew&auml;hlte Asteroid existiert nicht");
				redirect();
				return;
			}

			if (!base.getOwner().equals(getUser()))
			{
				addError("Dieser Asteroid geh&ouml;rt Ihnen nicht");
				redirect();
				return;
			}

			if (colonizer == null || !colonizer.getOwner().equals(user)
					|| !colonizer.getLocation().equals(base.getLocation())
					|| !colonizer.getTypeData().hasFlag(ShipTypes.SF_COLONIZER))
			{
				addError("Der ausgew&auml;hlte Colonizer ist ung&uuml;ltig");
				redirect();
				return;
			}

			// Alle Werte wurden übergeben, nur noch testen ob sie akzeptabel sind
			// Für jeden Asti darf maximal ein Auftrag in der DB sein
			UpgradeJob auftrag = (UpgradeJob) db.createQuery("from UpgradeJob where base=:base")
					.setParameter("base", base)
					.uniqueResult();

			if (auftrag != null)
			{
				addError("F&uuml;r diesen Asteroid besteht bereits ein Auftrag");
				redirect();
				return;
			}

			final UpgradeMaxValues maxvalues = (UpgradeMaxValues) db.get(UpgradeMaxValues.class,
					base.getKlasse());
			if (maxvalues == null)
			{
				addError("Dieser Asteroid kann leider nicht ausgebaut werden");
				redirect();
				return;
			}

			// Teste ob die übergebenen felder und cargo Parameter korrekt sind
			if (!cargo.getCargo() || felder.getCargo() || cargo.getType() != base.getKlasse() || felder.getType() != base.getKlasse())
			{ // Da es selbst für den leeren Ausbau Einträge gibt, funktioniert das hier
				addError("Es wurden illegale Ausbauten ausgew&auml;hlt");
				redirect();
				return;
			}

			if (felder.getMod() > 0 && (base.getMaxTiles() + felder.getMod() > maxvalues.getMaxTiles()))
			{
				addError("Der Asteroid hat zuviele Felder nach diesem Ausbau");
				redirect();
				return;
			}

			if (cargo.getMod() > 0 && (base.getMaxCargo() + cargo.getMod() > maxvalues.getMaxCargo()))
			{
				addError("Der Asteroid hat zuviel Lagerraum nach diesem Ausbau.");
				redirect();
				return;
			}

			if (felder.getMod() <= 0 && cargo.getMod() <= 0)
			{
				redirect();
				return;
			}

			// Erstelle einen neuen Auftrag
			auftrag = new UpgradeJob(base, user, felder, cargo, bar, colonizer);

			User factionUser = (User) db.get(User.class, faction);
			if (!bar)
			{
				// Testen ob genuegend Geld vorhanden ist um es uns untern Nagel zu reiszen
				if (user.getKonto()
						.compareTo(
								new BigDecimal(felder.getPrice() + cargo.getPrice())
										.toBigInteger()) < 0)
				{
					addError("Sie verf&uuml;gen nicht &uuml;ber genug Geld</span>");
					redirect();
					return;
				}
				factionUser.transferMoneyFrom(user.getId(), felder.getPrice()
						+ cargo.getPrice(), "Ausbau von " + base.getName());
			}

			// Den Besitzer des Colonizers ändern
			if (colonizer.consign(factionUser, false))
			{
				addError("Der Colonizer konnte nicht übergeben werden.");
				redirect();
				return;
			}

			// Auftrag speichern
			db.persist(auftrag);

			// Erstelle einen neuen Task für den Auftrag
			Taskmanager taskmanager = Taskmanager.getInstance();
			taskmanager.addTask(Taskmanager.Types.UPGRADE_JOB, 1,
					Integer.toString(auftrag.getId()), "0", Integer.toString(faction));

			t.setVar(
					"show.message",
					"Ihr Auftrag wurde an den zust&auml;ndigen Sachbearbeiter weitergeleitet. Die Bauma&szlig;nahmen werden in k&uuml;rze beginnen.");

			redirect();
			return;
		}

		t.setVar("show.ausbau", 1);

		t.setBlock("_ERSTEIGERN", "ausbau.asti.listitem", "ausbau.asti.list");
		t.setBlock("_ERSTEIGERN", "ausbau.colonizer.listitem", "ausbau.colonizer.list");
		t.setBlock("_ERSTEIGERN", "ausbau.cargo.listitem", "ausbau.cargo.list");
		t.setBlock("_ERSTEIGERN", "ausbau.felder.listitem", "ausbau.felder.list");

		// Hole alle Astis des Spielers und markiere gewaehlten Asti
		List<Base> astis = db.createQuery("from Base where owner=:user order by id")
				.setParameter("user", user).list();
		Base selectedBase = null;
		for (Base asti : astis)
		{
			final UpgradeMaxValues maxvalues = (UpgradeMaxValues) db.get(UpgradeMaxValues.class,
					asti.getKlasse());
			if (maxvalues == null)
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
			selectedBase = astis.get(0);
		}

		if (selectedBase == null)
		{
			return;
		}

		t.setVar("erz.name", Cargo.getResourceName(Resources.ERZ), "erz.image", Cargo
				.getResourceImage(Resources.ERZ), "bbs.name", Cargo.getResourceName(new ItemID(
				ITEM_BBS)), "bbs.image", Cargo.getResourceImage(new ItemID(ITEM_BBS)));

		// Hole die Colos des ausgewaehlten Astis
		List<Ship> colonizers = db
				.createQuery(
						"from Ship where shiptype.flags like :colonizer and "
								+ "owner=:user and system=:baseSystem and x=:baseX AND y=:baseY order by id")
				.setString("colonizer", ShipTypes.SF_COLONIZER).setParameter("user", user)
				.setInteger("baseSystem", selectedBase.getSystem()).setInteger("baseX",
						selectedBase.getX()).setInteger("baseY", selectedBase.getY()).list();

		User factionUser = (User) db.get(User.class, faction);
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

		final UpgradeMaxValues maxvalues = (UpgradeMaxValues) db.get(UpgradeMaxValues.class,
				selectedBase.getKlasse());

		// Setze die ausbau-mods, finde heraus welche bereits angewendet wurden und Typ des Astis
		List<UpgradeInfo> possibleMods = db.createQuery(
				"from UpgradeInfo where type=:asteroidClass order by id").setParameter(
				"asteroidClass", selectedBase.getKlasse()).list();
		for (UpgradeInfo info : possibleMods)
		{
			if (info.getCargo())
			{
				// Testen ob info den Cargo modifiziert
				if (info.getMod() == 0 || selectedBase.getMaxCargo() + info.getMod() <= maxvalues.getMaxCargo())
				{
					t.setVar("cargo.mod", info.getMod(),
							"cargo.id", info.getId(),
							"cargo.preis", info.getPrice(),
							"cargo.bbs", info.getMiningExplosive(),
							"cargo.erz", info.getOre());
					t.parse("ausbau.cargo.list", "ausbau.cargo.listitem", true);
				}
			}
			else
			{ // Es handelt sich um ein Felder Ausbau
				if (info.getMod() == 0 || selectedBase.getMaxTiles() + info.getMod() <= maxvalues.getMaxTiles())
				{
					t.setVar("felder.mod", info.getMod(),
							"felder.id", info.getId(),
							"felder.preis", info.getPrice(),
							"felder.bbs", info.getMiningExplosive(),
							"felder.erz", info.getOre());
					t.parse("ausbau.felder.list", "ausbau.felder.listitem", true);
				}
			}
		}
	}

	/**
	 * Zeigt den Shop der Fraktion an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void shopAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("shop"))
		{
			redirect();
			return;
		}

		User factionUser = (User) db.get(User.class, faction);

		t.setVar("show.shop", 1);

		t.setBlock("_ERSTEIGERN", "shop.listitem", "shop.list");
		t.setBlock("_ERSTEIGERN", "shop.orderlist.listitem", "shop.orderlist.list");
		t.setBlock("_ERSTEIGERN", "shop.shopownerlist.listitem", "shop.shopownerlist.list");

		if (faction != user.getId())
		{
			List<?> orderentryList = db
					.createQuery(
							"from FactionShopOrder as fso "
									+ "where fso.shopEntry.faction= :faction and fso.user= :user and fso.status<4")
					.setInteger("faction", faction).setEntity("user", user).list();
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
						"orderentry.lpkosten", order.getLpKosten() > 0 ? Common.ln(shopentry.getLpKosten()) : "",
						"orderentry.count", Common.ln(order.getCount()),
						"orderentry.status", getStatusName(order.getStatus()),
						"orderentry.bgcolor", getStatusColor(order.getStatus()));

				t.parse("shop.orderlist.list", "shop.orderlist.listitem", true);
			}
		}
		else
		{
			t.setVar("shop.owner", 1);

			List<?> orderentryList = db
					.createQuery(
							"from FactionShopOrder as fso "
									+ "where fso.shopEntry.faction = :faction and fso.status < 4 "
									+ "order by case when fso.status=0 then fso.status else fso.date end asc")
					.setInteger("faction", faction).list();
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

		// Zuerst alle Ganymed-Transportdaten auslesen

		List<?> ganyEntryList = db.createQuery(
				"from FactionShopEntry where faction= :faction and type=2")
				.setInteger("faction", faction)
				.list();

		FactionShopEntry[] ganytransport = new FactionShopEntry[ganyEntryList.size()];
		int i = 0;

		for (Object aGanyEntryList : ganyEntryList)
		{
			ganytransport[i++] = (FactionShopEntry) aGanyEntryList;
		}

		final boolean handelErlaubt = istHandelErlaubt(user, faction);
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
				"from FactionShopEntry where faction = :faction and type!=2").setInteger("faction",
				faction).list();
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
	}

	/**
	 * Zeigt die Meldeseite fuer LP der Fraktion an.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void aktionMeldenAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();

		if (!Faction.get(faction).getPages().hasPage("aktionmelden"))
		{
			redirect();
			return;
		}

		t.setVar("show.aktionmelden", 1);
	}

	/**
	 * Erstellt eine LP-Meldung bei der momentanen Fraktion.
	 *
	 * @param meldungstext Der Beschreibungstext der Aktion
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void aktionsMeldungErstellenAction(int faction, String meldungstext)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();

		if (!Faction.get(faction).getPages().hasPage("aktionmelden"))
		{
			redirect();
			return;
		}

		User factionUser = (User) db.get(User.class, faction);

		if (meldungstext == null || meldungstext.trim().length() < 10)
		{
			addError("Bitte gib eine genaue Beschreibung deiner Aktion ein");
			redirect("aktionMelden");
			return;
		}

		FraktionAktionsMeldung meldung = new FraktionAktionsMeldung(user, factionUser);
		meldung.setMeldungstext(meldungstext);
		db.persist(meldung);

		t.setVar("show.message", "Die Aktionsmeldung wurde der Fraktion erfolgreich übermittelt");

		redirect("aktionMelden");
	}

	/**
	 * Leitet zur Default-Seite einer Fraktion weiter.
	 *
	 * @param faction Die ID der anzuzeigenden Fraktion
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(int faction)
	{
		faction = ermittleFraktionUndErstelleMenue(faction);
		this.redirect(Faction.get(faction).getPages().getFirstPage());

	}
}
