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
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Transfer von Crew von Schiffen zu Schiffen/Basen (und umgekehrt)
 * @author Christopher Jung
 * 
 * @urlparam int ship Die ID des Schiffes von dem/zu dem transferiert werden soll
 * @urlparam int tar Die ID der Basis/des Schiffes, welches als Gegenstueck beim transfer fungiert. Die Bedeutung ist abhaengig vom Parameter <code>mode</code>
 * @urlparam String mode Der Transfermodus. Entweder ss (Schiff zu Schiff) oder sb (Schiff zu Basis) 
 *
 */
public class CrewtauschController extends TemplateGenerator {
	private SQLResultRow ship = null;
	private SQLResultRow datat = null;
	private int maxcrewf;
	private int maxcrewt;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public CrewtauschController(Context context) {
		super(context);
		
		setTemplate("crewtausch.html");
		
		parameterNumber("ship");
		parameterNumber("tar");
		parameterString("mode");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = (User)getUser();
		
		int shipID = getInteger("ship");
		String mode = getString("mode");
		int tar = getInteger("tar");
		
		SQLResultRow ship = db.first("SELECT id,name,crew,type,owner,status,x,y,system FROM ships WHERE owner=",user.getId()," AND id>0 AND id=",shipID);
		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}	
		
		SQLResultRow datat = null;
		int maxcrewf = 0;
		int maxcrewt = 0;
		
		if( mode.equals("ss") ) {
			datat = db.first("SELECT id,name,crew,type,owner,x,y,system,status FROM ships WHERE id>0 AND id=",tar);

			if( !Location.fromResult(ship).sameSector(0, Location.fromResult(datat), 0) ) {
				addError("Die beiden Schiffe befinden sich nicht im selben Sektor", Common.buildUrl("default", "module", "schiff", "ship", shipID) );
				
				return false;
			}

			maxcrewf = ShipTypes.getShipType( ship ).getInt("crew");
			
			maxcrewt = ShipTypes.getShipType( datat ).getInt("crew");
		}
		else if( mode.equals("sb") ) {
			datat = db.first("SELECT id,name,bewohner-arbeiter crew,bewohner,arbeiter,owner,x,y,system,size FROM bases WHERE id=",tar);

			if( !Location.fromResult(ship).sameSector(0, Location.fromResult(datat), datat.getInt("size")) ) {
				addError("Schiff und Basis befinden sich nicht im selben Sektor", Common.buildUrl("default", "module", "schiff", "ship", shipID) );
				
				return false;
			}

			maxcrewf = ShipTypes.getShipType( ship ).getInt("crew");
			maxcrewt = -1;
		}
		
		if( ship.getInt("owner") != datat.getInt("owner") ) {
			addError("Eines der Schiffe geh&ouml;rt ihnene nicht", Common.buildUrl("default", "module", "schiff", "ship", shipID) );
				
			return false;
		}

		this.ship = ship;
		this.datat = datat;
		this.maxcrewf = maxcrewf;
		this.maxcrewt = maxcrewt;
		
		return true;	
	}
	
	/**
	 * Transferiert Crew vom Ausgangsschiff zum Zielschiff/Basis
	 * @urlparam int send Die Anzahl der zu transferierenden Crew
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void sendAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		String mode = getString("mode");
		
		parameterNumber("send");
		int send = getInteger("send");
		
		if( send < 0 ) {
			send = 0;
		}
		if( (maxcrewt > -1) && (send > maxcrewt - datat.getInt("crew")) ) {
			send = maxcrewt - datat.getInt("crew");
		}
		if( send > ship.getInt("crew") ) {
			send = ship.getInt("crew");
		}
		
		if( send > 0 ) {
			t.setVar(	"crewtausch.transfer",	1,
						"transfer.way.to",		1,
						"transfer.count",		send );
			
			db.tBegin();

			db.tUpdate(1, "UPDATE ships SET crew=crew-",send," WHERE id>0 AND id=",ship.getInt("id")," AND crew=",ship.getInt("crew"));
			if( mode.equals("ss") ) {
				db.tUpdate(1, "UPDATE ships SET crew=crew+",send," WHERE id>0 AND id=",datat.getInt("id")," AND crew=",datat.getInt("crew"));

				datat.put("crew", datat.getInt("crew")+send);
		
				Ships.recalculateShipStatus(datat.getInt("id"));
			}
			else if( mode.equals("sb") ) {
				db.tUpdate(1, "UPDATE bases SET bewohner=bewohner+",send," WHERE id>0 AND id=",datat.getInt("id")," AND bewohner=",datat.getInt("bewohner")," AND arbeiter=",datat.getInt("arbeiter"));

				datat.put("crew", datat.getInt("crew")+send);
				datat.put("bewohner", datat.getInt("bewohner")+send);
			}
			ship.put("crew", ship.getInt("crew")-send);
			Ships.recalculateShipStatus(ship.getInt("id"));
			
			if( !db.tCommit() ) {
				addError("Beim Transfer der Crew ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter nocheinmal");
			}
		}	
		redirect();
	}
	
	/**
	 * Transfer in umgekehrter Richtung.<br>
	 * Transferiert Crew vom Zielschiff/Basis zum Ausgangsschiff
	 * @urlparam int rec Die Anzahl der zu transferierenden Crew
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void recAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		String mode = getString("mode");
		
		parameterNumber("rec");
		int rec = getInteger("rec");
		
		if( rec < 0 ) {
			rec = 0;
		}
		if( rec > maxcrewf - ship.getInt("crew") ) {
			rec = maxcrewf - ship.getInt("crew");
		}
		if( rec > datat.getInt("crew") ) {
			rec = datat.getInt("crew");
		}
		
		if( rec > 0 ) {
			t.setVar(	"crewtausch.transfer",	1,
						"transfer.way.to",		0,
						"transfer.count",		rec );
		
			db.tBegin();

			db.tUpdate(1, "UPDATE ships SET crew=crew+",rec," WHERE id>0 AND id=",ship.getInt("id")," AND crew=",ship.getInt("crew"));
			if( mode.equals("ss") ) {
				db.tUpdate(1, "UPDATE ships SET crew=crew-",rec," WHERE id>0 AND id=",datat.getInt("id")," AND crew=",datat.getInt("crew"));

				datat.put("crew", datat.getInt("crew")-rec);
		
				Ships.recalculateShipStatus(datat.getInt("id"));
			}
			else if( mode.equals("sb") ) {
				db.tUpdate(1, "UPDATE bases SET bewohner=bewohner-",rec," WHERE id>0 AND id=",datat.getInt("id")," AND bewohner=",datat.getInt("bewohner")," AND arbeiter=",datat.getInt("arbeiter"));

				datat.put("crew", datat.getInt("crew")-rec);
				datat.put("bewohner", datat.getInt("bewohner")-rec);
			}
			ship.put("crew", ship.getInt("crew")+rec);
			Ships.recalculateShipStatus(ship.getInt("id"));
			
			if( !db.tCommit() ) {
				addError("Beim Transfer der Crew ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter nocheinmal");
			}
		}
			
		redirect();
	}
	
	/**
	 * Anzeige von Infos sowie Eingabe der zu transferierenden Crew
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		String mode = getString("mode");
		
		t.setVar(	"ship.id",			ship.getInt("id"),
					"ship.name",		Common._plaintitle(ship.getString("name")),
					"ship.crew",		ship.getInt("crew"),
					"ship.maxcrew",		maxcrewf,
					"target.id",		datat.getInt("id"),
					"target.name",		datat.getString("name"),
					"target.crew",		datat.getString("crew"),
					"target.maxcrew",	(maxcrewt > -1 ? maxcrewt : "&#x221E;"),
					"global.mode",		mode,
					"global.mode.ss",	mode.equals("ss"),
					"global.mode.sb",	mode.equals("sb") );
	}
}
