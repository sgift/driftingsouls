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
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Pluendert ein Schiff.
 *
 * @author Christopher Jung
 */
@Module(name = "pluendern")
public class PluendernController extends Controller
{
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public PluendernController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Pluendern");
	}

	private void validiereEigenesUndZielschiff(Ship eigenesSchiff, Ship zielSchiff) {
		User user = (User) this.getUser();

		if ((eigenesSchiff == null) || (eigenesSchiff.getId() < 0) || (eigenesSchiff.getOwner() != user))
		{
			throw new ValidierungException("Sie brauchen ein Schiff um zu pl&uuml;ndern", Common.buildUrl("default", "module", "ships"));
		}

		final String errorurl = Common.buildUrl("default", "module", "schiff", "ship", eigenesSchiff.getId());

		if ((zielSchiff == null) || (zielSchiff.getId() < 0))
		{
			throw new ValidierungException("Das angegebene Zielschiff existiert nicht", errorurl);
		}
		ShipTypeData shipTypeTo = zielSchiff.getTypeData();

		if (user.isNoob())
		{
			throw new ValidierungException("Sie stehen unter GCP-Schutz und k&ouml;nnen daher nicht pl&uuml;nndern<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden", errorurl);
		}

		if (eigenesSchiff == zielSchiff)
		{
			throw new ValidierungException("Sie k&ouml;nnen nicht sich selbst pl&uuml;ndern", errorurl);
		}

		User taruser = zielSchiff.getOwner();

		if (taruser.isNoob())
		{
			throw new ValidierungException("Dieser Kolonist steht unter GCP-Schutz", errorurl);
		}

		if ((taruser.getVacationCount() != 0) && (taruser.getWait4VacationCount() == 0))
		{
			throw new ValidierungException("Sie k&ouml;nnen Schiffe dieses Spielers nicht kapern oder pl&uuml;ndern solange er sich im Vacation-Modus befindet", errorurl);
		}

		if (!eigenesSchiff.getLocation().sameSector(0, zielSchiff.getLocation(), 0))
		{
			throw new ValidierungException("Das zu pl&uuml;ndernde Schiff befindet sich nicht im selben Sektor", errorurl);
		}

		if ((eigenesSchiff.getBattle() != null) || (zielSchiff.getBattle() != null))
		{
			throw new ValidierungException("Eines der Schiffe ist in einen Kampf verwickelt", errorurl);
		}

		if (!shipTypeTo.getShipClass().isKaperbar())
		{
			throw new ValidierungException("Sie k&ouml;nnen " + shipTypeTo.getShipClass().getPlural() + " weder kapern noch pl&uuml;ndern", errorurl);
		}

		if (!zielSchiff.getStatus().contains("pluenderbar") && shipTypeTo.getShipClass() == ShipClasses.FELSBROCKEN)
		{
			throw new ValidierungException("Sie k&ouml;nnen " + shipTypeTo.getShipClass().getPlural() + " nicht pl&uuml;ndern, solange der H&uuml;llenwert nicht weit genug gesenkt wurde", errorurl);
		}

		// IFF-Check
		boolean disableIFF = zielSchiff.getStatus().contains("disable_iff");
		if (disableIFF)
		{
			throw new ValidierungException("Das Schiff kann nicht gepl&uuml;ndert werden", errorurl);
		}

		if (zielSchiff.isDocked() || zielSchiff.isLanded())
		{
			if (zielSchiff.isLanded())
			{
				throw new ValidierungException("Sie k&ouml;nnen gelandete Schiffe weder kapern noch pl&uuml;ndern", errorurl);
			}

			Ship mastership = zielSchiff.getBaseShip();
			if (((mastership.getCrew() != 0)) && (mastership.getEngine() != 0) &&
					(mastership.getWeapons() != 0))
			{
				throw new ValidierungException("Das Schiff, an das das feindliche Schiff angedockt hat, ist noch bewegungsf&auml;hig", errorurl);
			}
		}

		if ((zielSchiff.getCrew() != 0) && (shipTypeTo.getCost() != 0) && (zielSchiff.getEngine() != 0))
		{
			throw new ValidierungException("Feindliches Schiff nicht bewegungsunf&auml;hig", errorurl);
		}

		if (shipTypeTo.hasFlag(ShipTypeFlag.KEIN_TRANSFER))
		{
			throw new ValidierungException("Sie k&ouml;nnen keine Waren zu oder von diesem Schiff transferieren", errorurl);
		}

		if (shipTypeTo.hasFlag(ShipTypeFlag.NICHT_PLUENDERBAR))
		{
			throw new ValidierungException("Sie k&ouml;nnen keine Waren von diesem Schiff pl&uuml;ndern", errorurl);
		}

		if ((shipTypeTo.getShipClass() == ShipClasses.STATION) && (zielSchiff.getCrew() != 0))
		{
			throw new ValidierungException("Solange die Crew &uuml;ber die Waren wacht werden sie hier nichts klauen k&ouml;nnen", errorurl);
		}

		ShipTypeData shipTypeFrom = eigenesSchiff.getTypeData();
		if (shipTypeFrom.hasFlag(ShipTypeFlag.KEIN_TRANSFER))
		{
			throw new ValidierungException("Sie k&ouml;nnen keine Waren zu oder von ihrem Schiff transferieren", errorurl);
		}
	}

	/**
	 * Transferiert Waren zwischen den Schiffen.
	 *  @param toMap Die Menge der Ware (Key), welche zum Zielschiff transferiert werden soll
	 * @param fromMap Die Menge der Ware (Key), welche vom Zielschiff herunter transferiert werden soll
	 * @param shipFrom Die ID des Schiffes, mit dem ein anderes Schiff gepluendert werden soll
	 * @param shipTo Die ID des zu pluendernden Schiffes
	 * @param fromkapern <code>true</code>, falls das Modul vom Kapern-Modul aus aufgerufen wurde
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult transferAction(@UrlParam(name = "#to") Map<String, Long> toMap,
											 @UrlParam(name = "#from") Map<String, Long> fromMap,
											 @UrlParam(name = "from") Ship shipFrom,
											 @UrlParam(name = "to") Ship shipTo,
											 boolean fromkapern)
	{
		User user = (User) this.getUser();
		org.hibernate.Session db = getDB();

		validiereEigenesUndZielschiff(shipFrom, shipTo);

		Cargo cargofrom = shipFrom.getCargo();
		Cargo cargoto = shipTo.getCargo();

		ShipTypeData shipTypeTo = shipTo.getTypeData();
		ShipTypeData shipTypeFrom = shipFrom.getTypeData();

		long curcargoto = shipTypeTo.getCargo() - cargoto.getMass();
		long curcargofrom = shipTypeFrom.getCargo() - cargofrom.getMass();

		StringBuilder msg = new StringBuilder();
		Cargo newCargoTo = (Cargo) cargoto.clone();
		Cargo newCargoFrom = (Cargo) cargofrom.clone();

		long totaltransferfcount = 0;
		boolean transfer = false;

		String message = "";
		ResourceList reslist = cargofrom.compare(cargoto, true);
		for (ResourceEntry res : reslist)
		{
			Long transt = toMap.get(res.getId().toString());
			Long transf = fromMap.get(res.getId().toString());

			// Transfer vom Ausgangsschiff zum Zielschiff
			if (transt != null && transt > 0)
			{
				message += "Transportiere [resource="+res.getId()+"]"+transt+"[/resource] zu "+shipTo.getName();

				if (transt > res.getCount1())
				{
					transt = res.getCount1();

					message += " - Nur [resource="+res.getId()+"]"+res.getCount1()+"[/resource] vorhanden";
				}

				long massToTransfer = Cargo.getResourceMass(res.getId(), transt);
				if (curcargoto - massToTransfer < 0)
				{
					transt = (long) (curcargoto / (double) Cargo.getResourceMass(res.getId(), 1));

					message += " - Nur noch Platz für [resource="+res.getId()+"]"+transt+"[/resource] vorhanden";
				}

				// Falls es sich um ein unbekanntes Item handelt, dann dem Besitzer des Zielschiffes bekannt machen
				if (transt > 0)
				{
					int itemid = res.getId().getItemID();
					Item item = (Item) db.get(Item.class, itemid);
					if (item.isUnknownItem())
					{
						User targetUser = shipTo.getOwner();
						targetUser.addKnownItem(itemid);
					}
				}

				newCargoTo.addResource(res.getId(), transt);
				newCargoFrom.substractResource(res.getId(), transt);
				curcargoto = shipTypeTo.getCargo() - newCargoTo.getMass();
				curcargofrom = shipTypeFrom.getCargo() - newCargoFrom.getMass();

				message += " - jetzt [resource="+res.getId()+"]"+newCargoTo.getResourceCount(res.getId())+"[/resource] auf "+shipTo.getName()+" vorhanden";

				if (transt > 0)
				{
					transfer = true;
					msg.append("[resource=").append(res.getId()).append(")").append(Common.ln(transt)).append("[/resource] zurückgegeben.\n");
				}

				message += "\n";
			}
			// Transfer vom Zielschiff zum Ausgangsschiff
			else if (transf != null && transf > 0)
			{
				message += "Transportiere [resource="+res.getId()+"]"+transf+"[/resource] zu "+shipFrom.getName();

				if (transf > res.getCount2())
				{
					transf = res.getCount2();

					message += " - Nur [resource="+res.getId()+"]"+res.getCount2()+"[/resource] vorhanden";
				}

				long massToTransfer = Cargo.getResourceMass(res.getId(), transf);
				if (curcargofrom - massToTransfer < 0)
				{
					transf = (long) (curcargofrom / (double) Cargo.getResourceMass(res.getId(), 1));

					message += " - Nur noch Platz für [resource="+res.getId()+"]"+transf+"[/resource] vorhanden";
				}

				// Falls es sich um ein unbekanntes Item handelt, dann dieses dem Spieler bekannt machen
				if (transf > 0)
				{
					int itemid = res.getId().getItemID();
					Item item = (Item) db.get(Item.class, itemid);
					if (item.isUnknownItem())
					{
						user.addKnownItem(itemid);
					}
				}

				totaltransferfcount += transf;

				newCargoFrom.addResource(res.getId(), transf);
				newCargoTo.substractResource(res.getId(), transf);

				curcargoto = shipTypeTo.getCargo() - newCargoTo.getMass();
				curcargofrom = shipTypeFrom.getCargo() - newCargoFrom.getMass();

				message += " - jetzt [resource="+res.getId()+"]"+newCargoFrom.getResourceCount(res.getId())+"[/resource] auf "+shipFrom.getName()+" vorhanden";

				if (transf > 0)
				{
					transfer = true;
					msg.append("[resource=").append(res.getId()).append(")").append(Common.ln(transf)).append("[/resource] gestohlen.\n");
				}

				message += "\n";
			}
		}

		// Schiffe aktuallisieren
		if (transfer)
		{
			// Transmission versenden
			versendeNachrichtNachTransfer(msg, shipFrom, shipTo);

			if( aktualisiereSchiffNachWarentransfer(shipTypeTo, newCargoTo, newCargoFrom, totaltransferfcount, shipFrom, shipTo) ) {
				message += "[color=red]Das geplünderte Schiff beginnt zu zerfallen[/color]";
			}
		}

		return new RedirectViewResult("default").withMessage(message);
	}

	private void versendeNachrichtNachTransfer(StringBuilder msg, Ship shipFrom, Ship shipTo)
	{
		if ((msg.length() > 0) && shipTo.getOwner().getId() != -1)
		{
			msg.insert(0, shipTo.getName() + " (" + shipTo.getId() + ") wird von " +
					shipFrom.getName() + " (" + shipFrom.getId() + ") bei " +
					shipFrom.getLocation().displayCoordinates(false) + " gepl&uuml;ndert.\n");

			PM.send(shipFrom.getOwner(), shipTo.getOwner().getId(), "Schiff gepl&uuml;ndert", msg.toString());
		}
	}

	private boolean aktualisiereSchiffNachWarentransfer(ShipTypeData shipTypeTo, Cargo newCargoTo, Cargo newCargoFrom, long totaltransferfcount, Ship shipFrom, Ship shipTo)
	{
		shipFrom.setCargo(newCargoFrom);
		shipTo.setCargo(newCargoTo);

		String status = shipTo.recalculateShipStatus();
		shipFrom.recalculateShipStatus();

		// Falls das Schiff instabil ist, dann diesem den "destory"-Status geben,
		// damit der Schiffstick dieses zerstoert
		if ((totaltransferfcount > 0) && shipTypeTo.hasFlag(ShipTypeFlag.INSTABIL))
		{
			String statust = status;

			if (statust.length() > 0)
			{
				statust += " destroy";
			}
			else
			{
				statust += "destroy";
			}

			shipTo.setStatus(statust);

			return true;
		}
		return false;
	}

	/**
	 * Zeigt die GUI fuer den Warentransfer an.
	 * @param fromkapern <code>true</code>, falls das Modul vom Kapern-Modul aus aufgerufen wurde
	 * @param shipFrom Die ID des Schiffes, mit dem ein anderes Schiff gepluendert werden soll
	 * @param shipTo Die ID des zu pluendernden Schiffes
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name="from") Ship shipFrom, @UrlParam(name="to") Ship shipTo, boolean fromkapern, RedirectViewResult redirect)
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);

		validiereEigenesUndZielschiff(shipFrom, shipTo);

		if( redirect != null )
		{
			t.setVar("pluendern.message", Common._text(redirect.getMessage()));
		}

		t.setVar(
				"fromship.name", shipFrom.getName(),
				"fromship.id", shipFrom.getId(),
				"toship.name", shipTo.getName(),
				"toship.id", shipTo.getId(),
				"frompage.kapern", fromkapern);

		Cargo fromcargo = shipFrom.getCargo();
		Cargo tocargo = shipTo.getCargo();

		ShipTypeData shipTypeFrom = shipFrom.getTypeData();
		ShipTypeData shipTypeTo = shipTo.getTypeData();

		t.setVar("fromship.name", shipFrom.getName(),
				"fromship.id", shipFrom.getId(),
				"fromship.cargo", Common.ln(shipTypeFrom.getCargo() - fromcargo.getMass()),
				"toship.name", shipTo.getName(),
				"toship.id", shipTo.getId(),
				"toship.cargo", Common.ln(shipTypeTo.getCargo() - tocargo.getMass()));

		t.setBlock("_PLUENDERN", "res.listitem", "res.list");

		ResourceList reslist = fromcargo.compare(tocargo, true);
		for (ResourceEntry res : reslist)
		{
			t.setVar("res.id", res.getId(),
					"res.name", res.getName(),
					"res.image", res.getImage(),
					"res.cargo1", res.getCargo1(),
					"res.cargo2", res.getCargo2());

			t.parse("res.list", "res.listitem", true);
		}
		return t;
	}
}
