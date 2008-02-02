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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftGUI;

/**
 * <h1>Anzeige einer Schiffswerft</h1>
 * Die GUI selbst wird von {@link net.driftingsouls.ds2.server.werften.WerftGUI} gezeichnet
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des Schiffes, das die Werft ist
 * @urlparam Integer linkedbase Die ID einer Basis, mit der die Werft gekoppelt werden soll oder -1, falls die Kopplung aufgehoben werden soll
 *
 */
public class WerftController extends TemplateGenerator {
	private SQLResultRow ship;
	private SQLResultRow werftdata;
	private SQLResultRow type;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public WerftController(Context context) {
		super(context);
		
		parameterNumber("ship");
		parameterNumber("linkedbase");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = (User)getUser();
		
		int shipID = getInteger("ship");
		
		ship = db.first("SELECT id,owner,name,type,x,y,system,e,status FROM ships WHERE id>0 AND id=",shipID," AND owner='",user.getId(),"'");
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getInt("id"));
	
		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
			
			return false;
		}

		type = ShipTypes.getShipType( ship );

		werftdata = db.first("SELECT * FROM werften WHERE shipid=",ship.getInt("id"));
		if( werftdata.isEmpty() ) {
			addError("Dieses Schiff besitzt keinen Eintrag als Werft!", errorurl);
			
			return false;
		}
		
		return true;	
	}

	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		Database db = getDatabase();

		int linkedbase = getInteger("linkedbase");
	
		ShipWerft werft = new ShipWerft(werftdata,type.getString("werft"),ship.getInt("system"),ship.getInt("owner"),ship.getInt("id"));
		werft.setOneWayFlag(type.getInt("ow_werft"));
	
		// Ueberpruefen, ob die Werft inzwischen verschoben wurde (und ggf. der link aufgeloesst werden muss)
		if( werft.isLinked() ) {
			SQLResultRow base = db.first("SELECT x,y,system,size FROM bases WHERE id='",werft.getLinkedBase(),"'");
			if( !Location.fromResult(base).sameSector(base.getInt("size"), Location.fromResult(ship), 0) ) {
				werft.resetLink();
			}
		}

		StringBuffer echo = getContext().getResponse().getContent();
		
		// Soll die Werft an einen Asteroiden gekoppelt werden?
		if( linkedbase != 0 ) {
			echo.append("<span class=\"smallfont\">\n");
			if( linkedbase == -1 ) {
				echo.append("<span style=\"color:green\">Werft abgekoppelt</span><br />\n");
				werft.resetLink();
			}
			else {
   				if( type.getInt("cost") == 0 ) {
					SQLResultRow base = db.first("SELECT id,name FROM bases "+
										"WHERE id='"+linkedbase+"' AND x="+ship.getInt("x")+" AND y="+ship.getInt("y")+" "+
												"AND system="+ship.getInt("system")+" AND owner="+ship.getInt("owner") );
					if( base.isEmpty() ) {
						echo.append("<span style=\"color:red\">Sie k&ouml;nnen die Werft nicht an diese Basis koppeln!</span><br />\n");
					} 
					else {
						werft.setLink(base.getInt("id"));
						echo.append("<span style=\"color:green\">Werft an den Asteroiden "+Common._plaintitle(base.getString("name"))+" gekoppelt</span><br />\n");
					}
				}
			}
			echo.append("</span><br />\n");
		}

		echo.append("Werft "+Common._plaintitle(ship.getString("name"))+"<br /><br />\n");

		WerftGUI werftgui = new WerftGUI( getContext(), getTemplateEngine() );
		echo.append(werftgui.execute( werft ));
		
		echo.append("<br /><a class=\"back\" href=\""+Common.buildUrl("default", "module", "schiff", "ship", ship.getInt("id"))+"\">Zur&uuml;ck zum Schiff</a><br />\n");
	}
}
