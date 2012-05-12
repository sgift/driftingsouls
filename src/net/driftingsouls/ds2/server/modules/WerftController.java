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

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftGUI;

/**
 * <h1>Anzeige einer Schiffswerft.</h1>
 * Die GUI selbst wird von {@link net.driftingsouls.ds2.server.werften.WerftGUI} gezeichnet
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des Schiffes, das die Werft ist
 * @urlparam Integer linkedbase Die ID einer Basis, mit der die Werft gekoppelt werden soll oder -1, falls die Kopplung aufgehoben werden soll
 *
 */
@Module(name="werft")
public class WerftController extends TemplateGenerator {
	private Ship ship;
	private ShipWerft werft;
	private ShipTypeData type;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public WerftController(Context context) {
		super(context);
		
		parameterNumber("ship");
		parameterNumber("linkedbase");
		
		setPageTitle("Werft");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		int shipID = getInteger("ship");
		
		ship = (Ship)db.get(Ship.class, shipID);
	
		if( (ship == null) || (ship.getId() < 0) || (ship.getOwner() != user) ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen");
			
			return false;
		}
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		type = ship.getTypeData();

		werft = (ShipWerft)db.createQuery("from ShipWerft where shipid=?")
			.setInteger(0, ship.getId())
			.uniqueResult();
		
		if( werft == null ) {
			addError("Dieses Schiff besitzt keinen Eintrag als Werft!", errorurl);
			
			return false;
		}
		
		return true;	
	}

	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() throws IOException {
		org.hibernate.Session db = getDB();

		int linkedbase = getInteger("linkedbase");
	
		// Ueberpruefen, ob die Werft inzwischen verschoben wurde (und ggf. der link aufgeloesst werden muss)
		if( werft.isLinked() ) {
			Base base = werft.getLinkedBase();
			if( !base.getLocation().sameSector(base.getSize(), ship.getLocation(), 0) ) 
			{
				werft.resetLink();
			}
			
			if(!base.getOwner().equals(ship.getOwner()))
			{
				werft.resetLink();
			}
		}

		Writer echo = getContext().getResponse().getWriter();
		
		// Soll die Werft an einen Asteroiden gekoppelt werden?
		if( linkedbase != 0 ) {
			echo.append("<span class=\"smallfont\">\n");
			if( linkedbase == -1 ) {
				echo.append("<span style=\"color:green\">Werft abgekoppelt</span><br />\n");
				werft.resetLink();
			}
			else {
   				if( type.getCost() == 0 ) {
   					Base base = (Base)db.get(Base.class, linkedbase);
					if( (base == null) || (base.getOwner() != ship.getOwner()) || 
							!base.getLocation().sameSector(base.getSize(), ship.getLocation(), 0) ) {
						echo.append("<span style=\"color:red\">Sie k&ouml;nnen die Werft nicht an diese Basis koppeln!</span><br />\n");
					} 
					else {
						werft.setLink(base);
						echo.append("<span style=\"color:green\">Werft an den Asteroiden "+Common._plaintitle(base.getName())+" gekoppelt</span><br />\n");
					}
				}
			}
			echo.append("</span><br />\n");
		}

		WerftGUI werftgui = new WerftGUI( getContext(), getTemplateEngine() );
		echo.append(werftgui.execute( werft ));
		
		echo.append("<br /><a class=\"back\" href=\""+Common.buildUrl("default", "module", "schiff", "ship", ship.getId())+"\">Zur&uuml;ck zum Schiff</a><br />\n");
	}
}
