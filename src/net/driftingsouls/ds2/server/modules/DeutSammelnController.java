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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Sammelt mit einem Tanker in einem angegebenen Nebel Deuterium.
 * 
 * @author Christopher Jung
 * @urlparam Integer ship Die ID des Tankers
 * @urlparam Integer nebel Die ID des Nebels
 */
public class DeutSammelnController extends TemplateGenerator {
	private SQLResultRow ship = null;
	private SQLResultRow nebel = null;
	private SQLResultRow shiptype = null;
	private int retryCount = 0;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public DeutSammelnController(Context context) {
		super(context);
		
		setTemplate("deutsammeln.html");
		
		parameterNumber("ship");
		parameterNumber("nebel");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = (User)getUser();

		int shipID = getInteger("ship");
		int nebelID = getInteger("nebel");
		
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE owner=",user.getId()," AND id>0 AND id='",shipID,"'");
		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}

		SQLResultRow shiptype = ShipTypes.getShipType( ship );

		SQLResultRow nebel = db.first("SELECT id,x,y,system,type FROM nebel WHERE id=",nebelID);

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", shipID);

		if( !Location.fromResult(nebel).sameSector(0, Location.fromResult(ship), 0) ) {
			addError("Der Nebel befindet sich nicht im selben Sektor wie das Schiff", errorurl );
			
			return false;
		}
		
		if( nebel.getInt("type") > 2 )  {
			addError("In diesem Nebel k&ouml;nnen sie kein Deuterium sammeln", errorurl );
			
			return false;
		}

		if( shiptype.getInt("deutfactor") <= 0 )  {
			addError("Dieser Schiffstyp kann kein Deuterium sammeln", errorurl );
			
			return false;
		}

		if( ship.getInt("crew") < (shiptype.getInt("crew")/2) ) {
			addError("Sie haben nicht genug Crew um Deuterium zu sammeln", errorurl );
			
			return false;
		}
		
		this.ship = ship;
		this.nebel = nebel;
		this.shiptype = shiptype;

		return true;
	}
	
	/**
	 * Sammelnt fuer eine angegebene Menge Energie Deuterium aus einem Nebel
	 * @urlparam Integer e Die Menge Energie, fuer die Deuterium gesammelt werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void sammelnAction() {	
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
	
		parameterNumber("e");
		long e = getInteger("e");
	
		if( e > ship.getInt("e") ) {
			e = ship.getInt("e");
		}
		Cargo shipCargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		long cargo = shipCargo.getMass();
		
		long deutfactor = shiptype.getLong("deutfactor");
		if( nebel.getInt("type") == 1 ) {
			deutfactor--;
		}
		else if( nebel.getInt("type") == 2 ) {
			deutfactor++;
		}
		
		String message = "";

		if( (e * deutfactor)*Cargo.getResourceMass(Resources.DEUTERIUM, 1) > (shiptype.getLong("cargo") - cargo) ) {
			e = (shiptype.getLong("cargo")-cargo)/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ));
			message += "Kein Platz mehr im Frachtraum<br />";
		}
		
		long saugdeut = e * deutfactor;
		
		message += "<img src=\""+Cargo.getResourceImage(Resources.DEUTERIUM)+"\" alt=\"\" />"+saugdeut+" f&uuml;r <img src=\""+Configuration.getSetting("URL")+"data/interface/energie.gif\" alt=\"Energie\" />"+e+" gesammelt<br />";
		
		if( saugdeut > 0 ) {
			shipCargo.addResource( Resources.DEUTERIUM, saugdeut );
		
			db.tBegin();
			db.tUpdate(1, "UPDATE ships SET e=",ship.getInt("e")-e,",cargo='",shipCargo.save(),"' WHERE id>0 AND id=",ship.getInt("id")," AND cargo='",shipCargo.save(true),"' AND e=",ship.getInt("e"));
			if( !db.tCommit() ) {
				if( retryCount < 3 ) {
					retryCount++;
					redirect("sammeln");	
				}	
				else {
					addError("Das Deuterium konnte nicht erfolgreich gesammelt werden");	
					redirect();
				
					return;
				}
			}
			ship.put("e", ship.getInt("e")-e);
			Ships.recalculateShipStatus(ship.getInt("id"));
		}
		
		t.setVar("deutsammeln.message", message);
		
		redirect();
	}
	
	/**
	 * Zeigt eine Eingabemaske an, in der angegeben werden kann,
	 * fuer wieviel Energie Deuterium gesammelt werden soll
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {		
		TemplateEngine t = getTemplateEngine();
		
		int deutfactor = shiptype.getInt("deutfactor");
		if( nebel.getInt("type") == 1 ) {
			deutfactor--;
		}
		else if( nebel.getInt("type") == 2 ) {
			deutfactor++;
		}
		
		t.setVar(	"deuterium.image",		Cargo.getResourceImage(Resources.DEUTERIUM),
					"nebel.id",				nebel.getInt("id"),
					"ship.type.deutfactor",	deutfactor,
					"ship.id",				ship.getInt("id"),
					"ship.e",				ship.getInt("e") );
	}
}
