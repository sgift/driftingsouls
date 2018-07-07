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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.HandelspostenService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Die UI zum GTU-Zwischenlager.
 * <p>Hinweise zur Datenbankstruktur:<br>
 * <ul>
 * <li><b>user1</b> - Die ID des einen Handelspartners</li>
 * <li><b>user2</b> - Die ID des anderen Handelspartner</li>
 * <li><b>cargo1</b> - Die bisher vom zweiten Handelspartner geleistete Zahlung. Dies sind die Waren, die dem ersten Handelspartner zustehen</li>
 * <li><b>cargo1need</b> - Die insgesamt vom zweiten Handelspartner zu leistenden Zahlungen. Diese Warenmenge steht dem ersten Handelspartner insgesamt zu</li>
 * <li><b>cargo2</b> - Die bisher vom ersten Handelspartner geleistete Zahlung. Dies sind die Waren, die dem zweiten Handelspartner zustehen</li>
 * <li><b>cargo2need</b> - Die insgesamt vom ersten Handelspartner zu leistenden Zahlungen. Diese Warenmenge steht dem zweiten Handelspartner insgesamt zu</li>
 * </ul>
 * <p/>
 * Waren koennen erst abgeholt werden, wenn die eigenen Zahlungen geleistet wurden</p>
 *
 * @author Christopher Jung
 */
// TODO: Die ID des Handelspostens sollte per URL spezifiziert werden
@Module(name = "gtuzwischenlager")
public class GtuZwischenLagerController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;
	private HandelspostenService handelspostenService;

	@Autowired
	public GtuZwischenLagerController(TemplateViewResultFactory templateViewResultFactory,
									  HandelspostenService handelspostenService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.handelspostenService = handelspostenService;

		setPageTitle("GTU-Lager");
	}

	private void validiereSchiff(Ship ship)
	{
		User user = (User) this.getUser();
		if ((ship == null) || (ship.getId() < 0) || (ship.getOwner() != user))
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl("default", "module", "schiffe"));
		}
	}

	/**
	 * Transferiert nach der Bezahlung (jetzt) eigene Waren aus einem Handelsuebereinkommen
	 * auf das aktuelle Schiff.
	 *  @param ship Die ID des Schiffes, welches auf das GTU-Zwischenlager zugreifen will
	 * @param tradeentry Die ID des Zwischenlager-Eintrags
	 */
	@Action(ActionType.DEFAULT)
	public Object transportOwnAction(Ship ship, @UrlParam(name = "entry") GtuZwischenlager tradeentry, Ship handelsposten)
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();

		validiereSchiff(ship);

		validiereGtuZwischenlager(tradeentry, ship, handelsposten);

		//  Der Handelspartner
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = tradeentry.getCargo1();
		Cargo tradecargoneed = tradeentry.getCargo1Need();
		// Die Bezahlung
		Cargo owncargo = tradeentry.getCargo2();
		Cargo owncargoneed = tradeentry.getCargo2Need();

		if (tradeentry.getUser2().getId() == user.getId())
		{
			tradecargo = tradeentry.getCargo2();
			tradecargoneed = tradeentry.getCargo2Need();
			owncargo = tradeentry.getCargo1();
			owncargoneed = tradeentry.getCargo1Need();
		}

		Cargo tmpowncargoneed = new Cargo(owncargoneed);

		tmpowncargoneed.substractCargo(owncargo);
		if (!tmpowncargoneed.isEmpty())
		{
			throw new ValidierungException("Sie müssen die Waren erst komplett bezahlen", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}

		ShipTypeData shiptype = ship.getTypeData();

		Cargo shipCargo = new Cargo(ship.getCargo());
		long freecargo = shiptype.getCargo() - shipCargo.getMass();

		Cargo transportcargo;
		if (freecargo <= 0)
		{
			addError("Sie verf&uuml;gen nicht über genug freien Cargo um Waren abholen zu k&ouml;nnen");
			return new RedirectViewResult("viewEntry");
		}
		else if (freecargo < tradecargo.getMass())
		{
			transportcargo = new Cargo(tradecargo).cutCargo(freecargo);
		}
		else
		{
			transportcargo = new Cargo(tradecargo);
		}

		ResourceList reslist = transportcargo.getResourceList();

		shipCargo.addCargo(transportcargo);
		tradecargoneed.substractCargo(transportcargo);
		tradecargo.substractCargo(transportcargo);

		ship.setCargo(shipCargo);

		if (tradecargoneed.isEmpty() && owncargo.isEmpty())
		{
			db.delete(tradeentry);

			TemplateEngine t = templateViewResultFactory.createFor(this);
			t.setVar("global.shipid", ship.getId());
			t.setVar("global.handelsposten", handelsposten.getId());
			t.setBlock("_GTUZWISCHENLAGER", "transferlist.res.listitem", "transferlist.res.list");
			Resources.echoResList(t, reslist, "transferlist.res.list");
			t.setVar("global.transferlist", 1);
			t.setVar("transferlist.backlink", 1);

			return t;
		}

		if (tradeentry.getUser1() == user)
		{
			tradeentry.setCargo1(tradecargo);
			tradeentry.setCargo1Need(tradecargoneed);
		}
		else
		{
			tradeentry.setCargo2(tradecargo);
			tradeentry.setCargo2Need(tradecargoneed);
		}

		return new RedirectViewResult("viewEntry").withMessage("Transferiere Waren\n\n"+Resources.resourceListToBBCode(reslist));
	}

	private void validiereGtuZwischenlager(GtuZwischenlager tradeentry, Ship ship, Ship handelsposten)
	{
		User user = (User) this.getUser();

		if ((tradeentry == null) || (tradeentry.getPosten() != handelsposten) || ((tradeentry.getUser1() != user) && (tradeentry.getUser2() != user)))
		{
			throw new ValidierungException("Es wurde kein passender Handelseintrag gefunden", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}
	}

	/**
	 * Transferiert fuer einen Eintrag noch fehlende Resourcen.
	 */
	@Action(ActionType.DEFAULT)
	public void transportMissingAction()
	{
		// TODO
	}

	/**
	 * Zeigt einen Handelsuebereinkommen an.
	 *  @param ship Die ID des Schiffes, welches auf das GTU-Zwischenlager zugreifen will
	 * @param tradeentry Die ID des Zwischenlager-Eintrags
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine viewEntryAction(Ship ship, @UrlParam(name = "entry") GtuZwischenlager tradeentry, Ship handelsposten, RedirectViewResult redirect)
	{
		User user = (User) this.getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		validiereSchiff(ship);

		if( redirect != null )
		{
			t.setVar("global.message", Common._text(redirect.getMessage()));
		}
		t.setVar("global.shipid", ship.getId());
		t.setVar("global.handelsposten", handelsposten.getId());

		validiereGtuZwischenlager(tradeentry, ship, handelsposten);

		t.setVar("global.entry", 1);

		t.setBlock("_GTUZWISCHENLAGER", "res.listitem", "res.list");

		// Der Handelspartner
		User tradepartner = tradeentry.getUser2();
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = tradeentry.getCargo1();
		// Die Bezahlung
		Cargo owncargo = tradeentry.getCargo2();
		Cargo owncargoneed = tradeentry.getCargo2Need();

		if (tradepartner.getId() == user.getId())
		{
			tradepartner = tradeentry.getUser1();
			tradecargo = tradeentry.getCargo2();
			owncargo = tradeentry.getCargo1();
			owncargoneed = tradeentry.getCargo1Need();
		}

		t.setVar("tradeentry.id", tradeentry.getId(),
				"tradeentry.partner", Common._title(tradepartner.getName()),
				"tradeentry.missingcargo", "",
				"tradeentry.waren", "");


		// (zukuenftig) eigene Waren anzeigen
		ResourceList reslist = tradecargo.getResourceList();
		Resources.echoResList(t, reslist, "tradeentry.waren", "res.listitem");

		// noch ausstehende Bezahlung anzeigen
		owncargoneed.substractCargo(owncargo);
		if (!owncargoneed.isEmpty())
		{
			reslist = owncargoneed.getResourceList();
			Resources.echoResList(t, reslist, "tradeentry.missingcargo", "res.listitem");
		}

		return t;
	}

	/**
	 * Zeigt die Liste aller Handelsvereinbarungen auf diesem Handelsposten an, an denen der aktuelle Spieler beteiligt ist.
	 *
	 * @param ship Die ID des Schiffes, welches auf das GTU-Zwischenlager zugreifen will
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(Ship ship, Ship handelsposten)
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();
		TemplateEngine t = templateViewResultFactory.createFor(this);

		validiereSchiff(ship);

		t.setVar("global.shipid", ship.getId());
		t.setVar("global.handelsposten", handelsposten.getId());

		t.setVar("global.tradelist", 1);
		t.setBlock("_GTUZWISCHENLAGER", "tradelist.listitem", "tradelist.list");
		t.setBlock("tradelist.listitem", "res.listitem", "res.list");

		validiereHandelspostenKontaktierbar(handelsposten, ship);

		List<?> tradelist = db.createQuery("from GtuZwischenlager where posten=:posten and (user1= :user or user2= :user)")
				.setEntity("posten", handelsposten)
				.setEntity("user", user)
				.list();
		for (Object aTradelist : tradelist)
		{
			GtuZwischenlager tradeentry = (GtuZwischenlager) aTradelist;

			User tradepartner = tradeentry.getUser2();
			Cargo tradecargo = tradeentry.getCargo1();
			Cargo owncargo = tradeentry.getCargo2();
			Cargo owncargoneed = tradeentry.getCargo2Need();

			if (tradepartner == user)
			{
				tradepartner = tradeentry.getUser1();
				tradecargo = tradeentry.getCargo2();
				owncargo = tradeentry.getCargo1();
				owncargoneed = tradeentry.getCargo1Need();
			}

			t.setVar("list.entryid", tradeentry.getId(),
					"list.user", Common._title(tradepartner.getName()),
					"res.list", "",
					"list.cargoreq.list", "",
					"list.status", "bereit");

			// (zukuenftig) eigene Waren anzeigen
			ResourceList reslist = tradecargo.getResourceList();
			Resources.echoResList(t, reslist, "res.list");

			List<ItemCargoEntry> itemlist = tradecargo.getItems();
			for (ItemCargoEntry item : itemlist)
			{
				Item itemobject = item.getItem();
				if (itemobject.isUnknownItem())
				{
					user.addKnownItem(item.getItemID());
				}
			}

			// noch ausstehende Bezahlung anzeigen
			owncargoneed.substractCargo(owncargo);

			if (!owncargoneed.isEmpty())
			{
				reslist = owncargoneed.getResourceList();
				Resources.echoResList(t, reslist, "list.cargoreq.list", "res.listitem");
			}

			t.parse("tradelist.list", "tradelist.listitem", true);
		}
		return t;
	}

	private void validiereHandelspostenKontaktierbar(Ship handel, Ship ship)
	{
		if( !handelspostenService.isKommunikationMoeglich(handel, ship) )
		{
			throw new ValidierungException("Das Schiff kann keinen Kontakt zum Handelsposten aufnehmen");
		}
	}
}
