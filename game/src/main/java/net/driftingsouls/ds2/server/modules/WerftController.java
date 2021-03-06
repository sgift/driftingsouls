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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.services.DismantlingService;
import net.driftingsouls.ds2.server.services.FleetMgmtService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.services.ShipyardService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftGUI;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.Writer;

/**
 * <h1>Anzeige einer Schiffswerft.</h1>
 * Die GUI selbst wird von {@link net.driftingsouls.ds2.server.werften.WerftGUI} gezeichnet
 *
 * @author Christopher Jung
 */
@Module(name = "werft")
public class WerftController extends Controller
{
	@PersistenceContext
	private EntityManager em;

	private final TemplateViewResultFactory templateViewResultFactory;
	private final ShipyardService shipyardService;
	private final BBCodeParser bbCodeParser;
	private final FleetMgmtService fleetMgmtService;
	private final DismantlingService dismantlingService;
	private final ShipActionService shipActionService;

	@Autowired
	public WerftController(TemplateViewResultFactory templateViewResultFactory, ShipyardService shipyardService, BBCodeParser bbCodeParser, FleetMgmtService fleetMgmtService, DismantlingService dismantlingService, ShipActionService shipActionService)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.shipyardService = shipyardService;
		this.bbCodeParser = bbCodeParser;
		this.fleetMgmtService = fleetMgmtService;
		this.dismantlingService = dismantlingService;
		this.shipActionService = shipActionService;

		setPageTitle("Werft");
	}

	private void validiereSchiff(Ship ship)
	{
		User user = (User) getUser();

		if ((ship == null) || (ship.getId() < 0) || (ship.getOwner() != user))
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht oder gehört nicht Ihnen.");
		}
	}

	private void validiereWerft(Ship ship, ShipWerft werft)
	{
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		if (werft == null)
		{
			throw new ValidierungException("Dieses Schiff besitzt keinen Eintrag als Werft!", errorurl);
		}
	}

	/**
	 * Zeigt die GUI an.
	 *
	 * @param ship Die ID des Schiffes, das die Werft ist
	 * @param linkedbase Die ID einer Basis, mit der die Werft gekoppelt werden soll oder -1, falls die Kopplung aufgehoben werden soll
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(Ship ship, int linkedbase) throws IOException
	{
		validiereSchiff(ship);

		ShipWerft werft = em.createQuery("from ShipWerft where ship=:ship", ShipWerft.class)
				.setParameter("ship", ship)
				.getSingleResult();

		validiereWerft(ship, werft);

		// Ueberpruefen, ob die Werft inzwischen verschoben wurde (und ggf. der link aufgeloesst werden muss)
		if (werft.isLinked())
		{
			Base base = werft.getLinkedBase();
			if (!base.getLocation().sameSector(base.getSize(), ship.getLocation(), 0))
			{
				werft.resetLink();
			}

			if (!base.getOwner().equals(ship.getOwner()))
			{
				werft.resetLink();
			}
		}

		Writer echo = getContext().getResponse().getWriter();

		// Soll die Werft an einen Asteroiden gekoppelt werden?
		if (linkedbase != 0)
		{
			echo.append("<span class=\"smallfont\">\n");
			if (linkedbase == -1)
			{
				echo.append("<span style=\"color:green\">Werft abgekoppelt.</span><br />\n");
				werft.resetLink();
			}
			else
			{

                Base base = em.find(Base.class, linkedbase);
                if ((base == null) || (base.getOwner() != ship.getOwner()) ||
                        !base.getLocation().sameSector(base.getSize(), ship.getLocation(), 0))
                {
                    echo.append("<span style=\"color:red\">Sie können die Werft nicht an diese Basis koppeln!</span><br />\n");
                }
                else
                {
                    werft.setLink(base);
                    echo.append("<span style=\"color:green\">Werft an den Asteroiden ").append(Common._plaintitle(base.getName())).append(" gekoppelt.</span><br />\n");
                }
			}
			echo.append("</span><br />\n");
		}

		WerftGUI werftgui = new WerftGUI(getContext(), templateViewResultFactory.createEmpty(), shipyardService, bbCodeParser, fleetMgmtService, dismantlingService, shipActionService);
		echo.append(werftgui.execute(werft));

		echo.append("<br /><a class=\"back\" href=\"").append(Common.buildUrl("default", "module", "schiff", "ship", ship.getId())).append("\">zurück zum Schiff</a><br />\n");
	}
}
