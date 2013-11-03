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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.ResourceLimit;
import net.driftingsouls.ds2.server.entities.ResourceLimit.ResourceLimitKey;
import net.driftingsouls.ds2.server.entities.SellLimit;
import net.driftingsouls.ds2.server.entities.statistik.StatVerkaeufe;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Verkauft Waren an einem Handelsposten.
 *
 * @author Christopher Jung
 * @urlparam Integer ship die ID des Schiffes, das Waren verkaufen moechte
 * @urlparam Integer tradepost die ID des Handelspostens, an dem die Waren verkauft werden sollen
 */
@Module(name = "trade")
public class TradeController extends TemplateController
{
	private static final Log log = LogFactory.getLog(TradeController.class);

	private Ship ship = null;
	private Cargo kurse = null;
	private Ship posten = null;
	private String place = null;

	/**
	 * Konstruktor.
	 *
	 * @param context Der zu verwendende Kontext
	 */
	public TradeController(Context context)
	{
		super(context);

		setTemplate("trade.html");

		parameterNumber("ship");
		parameterNumber("tradepost");

		setPageTitle("Handelsposten");
	}

	@Override
	protected boolean validateAndPrepare()
	{
		org.hibernate.Session db = getDB();
		int shipId = getInteger("ship");

		this.ship = (Ship) db.get(Ship.class, shipId);
		if ((this.ship == null) || (ship.getId() < 0) || (ship.getOwner() != getUser()))
		{
			addError("Fehler: Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl("default", "module", "schiffe"));

			return false;
		}

		int tradepost = getInteger("tradepost");
		Ship handel = (Ship) db.createQuery("from Ship where id>0 and system=:sys and x=:x and y=:y and id=:id")
				.setInteger("sys", this.ship.getSystem())
				.setInteger("x", this.ship.getX())
				.setInteger("y", this.ship.getY())
				.setInteger("id", tradepost)
				.setMaxResults(1)
				.uniqueResult();
		if (handel == null || !handel.isTradepost())
		{
			addError("Fehler: Der angegebene Handelsposten konnte nicht im Sektor lokalisiert werden", Common.buildUrl("default", "module", "schiff", "ship", shipId));

			return false;
		}

		this.posten = handel;
		this.place = "p" + handel.getId();
		GtuWarenKurse kurse = (GtuWarenKurse) db.get(GtuWarenKurse.class, "p" + handel.getId());

		if (kurse == null && this.posten.getOwner().getRace() == Faction.GTU_RASSE)
		{
			this.place = "tradepost";
			kurse = (GtuWarenKurse) db.get(GtuWarenKurse.class, "tradepost");
		}
		if (kurse == null)
		{
			addError("Fehler: Der An- und Verkauf auf dem Handelsposten wurde nicht freigegeben", Common.buildUrl("default", "module", "schiff", "ship", shipId));

			return false;
		}

		User user = (User) getUser();
		if (!posten.isTradepostVisible(user, user.getRelations()))
		{
			addError("Fehler: Dieser Handelsposten handelt nicht mit Ihnen. Für die Aufnahme von Handelsbeziehungen setzen Sie sich mit dem Eigner in Verbindung.", Common.buildUrl("default", "module", "schiff", "ship", shipId));

			return false;
		}

		this.kurse = new Cargo(kurse.getKurse());
		this.kurse.setOption(Cargo.Option.SHOWMASS, false);

		getTemplateEngine().setVar("global.shipid", shipId);
		getTemplateEngine().setVar("global.tradepost", handel.getId());

		getTemplateEngine().setBlock("_TRADE", "msgs.listitem", "msgs.list");

		return true;
	}

	/**
	 * Kauft die angegebenen Waren vom Handelsposten.
	 */
	@Action(ActionType.DEFAULT)
	public void buyAction(@UrlParam(name = "#from") Map<String, Long> fromMap)
	{
		org.hibernate.Session db = getDB();

		ResourceList resourceList = this.posten.getCargo().getResourceList();
		Cargo tradepostCargo = this.posten.getCargo();
		User user = (User) getUser();
		BigInteger moneyOfBuyer = user.getKonto();
		BigInteger totalRE = BigInteger.ZERO;

		log.info("Warenkauf an HP " + posten.getId() + " durch Schiff " + ship.getId() + " [User: " + user.getId() + "]");

		for (ResourceEntry resource : resourceList)
		{
			Long amountToBuy = fromMap.get(resource.getId().toString());

			if (amountToBuy == null || amountToBuy <= 0)
			{
				continue;
			}

			//Preis und Minimum holen
			ResourceLimitKey resourceLimitKey = new ResourceLimitKey(posten, resource.getId());
			SellLimit limit = (SellLimit) db.get(SellLimit.class, resourceLimitKey);

			//Ware wird nicht verkauft
			if (limit == null || limit.getPrice() <= 0)
			{
				continue;
			}

			//The seller lacks the rank needed to sell this resource
			if (!limit.willSell(this.posten.getOwner(), user))
			{
				continue;
			}

			long amountOnPost = tradepostCargo.getResourceCount(resource.getId()) - limit.getLimit();
			if (amountOnPost <= 0)
			{
				continue;
			}

			if (amountToBuy > amountOnPost)
			{
				amountToBuy = amountOnPost;
			}

			long resourceMass = Cargo.getResourceMass(resource.getId(), 1);
			long neededSpace = amountToBuy * resourceMass;
			long freeSpaceOnShip = ship.getMaxCargo() - ship.getCargo().getMass();

			if (neededSpace > freeSpaceOnShip)
			{
				amountToBuy = freeSpaceOnShip / resourceMass;
			}

			long price = amountToBuy * limit.getPrice();
			//Nicht genug Geld da
			if (moneyOfBuyer.compareTo(BigInteger.valueOf(price)) < 0)
			{
				amountToBuy = moneyOfBuyer.divide(BigInteger.valueOf(price)).longValue();
				price = amountToBuy * limit.getPrice();
			}
			log.info("Verkaufe " + amountToBuy + "x " + resource.getId() + " fuer gesamt " + price);
			totalRE = totalRE.add(BigInteger.valueOf(price));

			if (amountToBuy <= 0)
			{
				continue;
			}

			moneyOfBuyer = moneyOfBuyer.subtract(BigInteger.valueOf(price));

			this.posten.transfer(this.ship, resource.getId(), amountToBuy);
		}

		this.ship.recalculateShipStatus();
		this.posten.recalculateShipStatus();

		if (totalRE.compareTo(BigInteger.ZERO) > 0)
		{
			this.posten.getOwner()
					.transferMoneyFrom(user.getId(), totalRE,
							"Warenkauf Handelsposten bei " + this.posten.getLocation().displayCoordinates(false),
							false, UserMoneyTransfer.Transfer.SEMIAUTO);
		}
		redirect();
	}

	/**
	 * Verkauft die angegebenen Waren.
	 *
	 * @param toMap Verkauft die Resource mit der ID (Key) in der angegebenen Menge
	 */
	@Action(ActionType.DEFAULT)
	public void sellAction(@UrlParam(name = "#to") Map<String, Long> toMap)
	{
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();
		User user = (User) getUser();
		int MIN_TICKS_TO_SURVIVE = 7;
		Cargo shipCargo = ship.getCargo();

		int tick = getContext().get(ContextCommon.class).getTick();

		StatVerkaeufe stats = (StatVerkaeufe) db.createQuery("from StatVerkaeufe where tick=:tick and place=:place and system=:sys")
				.setInteger("tick", tick)
				.setString("place", this.place)
				.setInteger("sys", this.posten.getSystem())
				.uniqueResult();

		Cargo statsCargo;
		if (stats == null)
		{
			stats = new StatVerkaeufe(tick, this.posten.getSystem(), this.place);
			db.persist(stats);
		}

		statsCargo = stats.getStats();

		Cargo tpcargo = this.posten.getCargo();

		BigInteger totalRE = BigInteger.ZERO;
		boolean changed = false;

		ResourceList reslist = this.kurse.getResourceList();
		long freeSpace = posten.getTypeData().getCargo() - posten.getCargo().getMass();
		long reconsumption = -1 * posten.getOwner().getReBalance();
		BigInteger konto = posten.getOwner().getKonto();
		for (ResourceEntry res : reslist)
		{
			Long tmp = toMap.get(res.getId().toString());

			if (tmp != null && tmp > 0)
			{
				if (tmp > shipCargo.getResourceCount(res.getId()))
				{
					tmp = shipCargo.getResourceCount(res.getId());
				}

				long resourceMass = Cargo.getResourceMass(res.getId(), 1);

				//Wir wollen eventuell nur bis zu einem Limit ankaufen
				ResourceLimitKey resourceLimitKey = new ResourceLimitKey(posten, res.getId());
				ResourceLimit resourceLimit = (ResourceLimit) db.get(ResourceLimit.class, resourceLimitKey);

				long limit = Long.MAX_VALUE;
				if (resourceLimit != null)
				{
					//Do we want to buy this resource from this player?
					if (!resourceLimit.willBuy(this.posten.getOwner(), user))
					{
						continue;
					}

					limit = resourceLimit.getLimit();
					//Bereits gelagerte Bestaende abziehen
					limit -= posten.getCargo().getResourceCount(res.getId());
				}

				if (tmp > limit)
				{
					long nichtVerkauft = tmp - limit;
					tmp = limit;

					t.setVar("waren.count", Common.ln(nichtVerkauft),
							"waren.name", res.getName(),
							"waren.img", res.getImage(),
							"waren.fehler", "Es besteht kein Interesse mehr an dieser Ware");

					t.parse("msgs.list", "msgs.listitem", true);
				}

				//Nicht mehr ankaufen als Platz da ist
				if (tmp * resourceMass > freeSpace)
				{
					long nichtVerkauft = tmp - freeSpace / resourceMass;
					tmp = freeSpace / resourceMass;

					t.setVar("waren.count", Common.ln(nichtVerkauft),
							"waren.name", res.getName(),
							"waren.img", res.getImage(),
							"waren.fehler", "Alle Lager sind voll");

					t.parse("msgs.list", "msgs.listitem", true);
				}

				BigDecimal get = BigDecimal.valueOf(tmp).multiply(new BigDecimal(res.getCount1() / 1000d));

				//Aufpassen das ich nicht das Konto leerfresse
				if (reconsumption > 0)
				{
					BigInteger ticks = konto.subtract(get.toBigInteger()).divide(BigInteger.valueOf(reconsumption));
					if (ticks.compareTo(BigInteger.valueOf(MIN_TICKS_TO_SURVIVE)) <= 0)
					{
						//Konto reicht mit Verkauf nur noch fuer weniger als 7 Ticks => begrenzen.
						long maximum = konto
								.subtract(BigInteger.valueOf(MIN_TICKS_TO_SURVIVE * reconsumption))
								.multiply(BigInteger.valueOf(1000))
								.divide(BigInteger.valueOf(res.getCount1())).longValue();

						t.setVar("waren.count", Common.ln(tmp - maximum),
								"waren.name", res.getName(),
								"waren.img", res.getImage(),
								"waren.fehler", "Ihr Handelspartner ist pleite");

						t.parse("msgs.list", "msgs.listitem", true);

						tmp = maximum;
					}
				}

				if (tmp <= 0)
				{
					continue;
				}

				get = BigDecimal.valueOf(tmp).multiply(new BigDecimal(res.getCount1() / 1000d));

				t.setVar("waren.count", Common.ln(tmp),
						"waren.name", res.getName(),
						"waren.img", res.getImage(),
						"waren.re", Common.ln(get),
						"waren.fehler", "");

				t.parse("msgs.list", "msgs.listitem", true);

				totalRE = totalRE.add(get.toBigInteger());
				changed = true;
				shipCargo.substractResource(res.getId(), tmp);

				statsCargo.addResource(res.getId(), tmp);
				tpcargo.addResource(res.getId(), tmp);
				//Freien Platz korrigieren
				freeSpace -= tmp * resourceMass;
				konto = konto.subtract(get.toBigInteger());
			}
		}

		if (changed)
		{
			stats.setStats(statsCargo);

			this.posten.setCargo(tpcargo);
			this.ship.setCargo(shipCargo);

			this.ship.recalculateShipStatus();

			user.transferMoneyFrom(this.posten.getOwner().getId(), totalRE,
					"Warenverkauf Handelsposten bei " + this.posten.getLocation().displayCoordinates(false), false,
					UserMoneyTransfer.Transfer.SEMIAUTO);
		}

		redirect();
	}

	private boolean isFull()
	{
		return posten.getTypeData().getCargo() <= posten.getCargo().getMass();
	}

	/**
	 * Zeigt die eigenen Waren sowie die Warenkurse am Handelsposten an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction()
	{
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		t.setVar("error.none", 1);
		t.setBlock("_TRADE", "res.listitem", "res.list");

		ResourceList reslist = this.kurse.getResourceList();

		// Block to check if user is an enemy
		User user = (User) getUser();

		if (!isFull())
		{
			t.setVar("is.full", 0);
			for (ResourceEntry res : reslist)
			{
				if (!ship.getCargo().hasResource(res.getId()))
				{
					continue;
				}

				ResourceLimitKey resourceLimitKey = new ResourceLimitKey(posten, res.getId());
				ResourceLimit limit = (ResourceLimit) db.get(ResourceLimit.class, resourceLimitKey);

				// Kaufen wir diese Ware vom Spieler?
				if (limit != null && !limit.willBuy(this.posten.getOwner(), user))
				{
					continue;
				}

				String preis;
				if (res.getCount1() < 50)
				{
					preis = "Kein Bedarf";
				}
				else
				{
					preis = Common.ln(res.getCount1() / 1000d) + " RE";
				}

				t.setVar("res.img", res.getImage(),
						"res.id", res.getId(),
						"res.name", res.getName(),
						"res.cargo", ship.getCargo().getResourceCount(res.getId()),
						"res.re", preis);

				t.parse("res.list", "res.listitem", true);
			}
		}
		else
		{

			t.setVar("is.full", true,
					"res.msg", "Dieser Handelsposten ist voll. Bitte beehre uns zu einem späteren Zeitpunkt erneut.");
		}

		t.setBlock("_TRADE", "resbuy.listitem", "resbuy.list");

		ResourceList buyList = this.posten.getCargo().getResourceList();
		for (ResourceEntry resource : buyList)
		{
			ResourceLimitKey resourceLimitKey = new ResourceLimitKey(posten, resource.getId());
			SellLimit limit = (SellLimit) db.get(SellLimit.class, resourceLimitKey);

			//Nicht kaeuflich
			if (limit == null || limit.getPrice() <= 0)
			{
				continue;
			}

			if (!limit.willSell(this.posten.getOwner(), user))
			{
				continue;
			}

			long buyable = this.posten.getCargo().getResourceCount(resource.getId()) - limit.getLimit();
			if (buyable <= 0)
			{
				continue;
			}


			t.setVar("resbuy.img", resource.getImage(),
					"resbuy.id", resource.getId(),
					"resbuy.name", resource.getName(),
					"resbuy.cargo", buyable,
					"resbuy.re", limit.getPrice());
			t.parse("resbuy.list", "resbuy.listitem", true);
		}
	}
}
