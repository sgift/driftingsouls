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
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Transfer von Crew von Schiffen zu Schiffen/Basen (und umgekehrt).
 * @author Christopher Jung
 * 
 * @urlparam int ship Die ID des Schiffes von dem/zu dem transferiert werden soll
 * @urlparam int tar Die ID der Basis/des Schiffes, welches als Gegenstueck beim transfer fungiert. Die Bedeutung ist abhaengig vom Parameter <code>mode</code>
 * @urlparam String mode Der Transfermodus. Entweder ss (Schiff zu Schiff) oder sb (Schiff zu Basis) 
 *
 */
@Module(name="crewtausch")
public class CrewtauschController extends TemplateGenerator {
	/**
	 * Das Ziel fuer einen Crewtransfer.
	 *
	 */
	private static interface Target {
		/**
		 * Gibt die verfuegbare Crew zurueck.
		 * @return Die Crew
		 */
		public int getCrew();
		/**
		 * Setzt die Crew auf dem Objekt.
		 * @param crew Die Crew
		 */
		public void setCrew(int crew);
		/**
		 * Wird am Ende des Transfervorgangs aufgerufen.
		 *
		 */
		public void finishTransfer();
		/**
		 * Gibt den Namen des Objekts zurueck.
		 * @return Der Name
		 */
		public String getName();
		/**
		 * Gibt die ID des Objekts zurueck.
		 * @return Die ID
		 */
		public int getId();
		/**
		 * Gibt den Besitzer des Objekts zurueck.
		 * @return Der Besitzer
		 */
		public User getOwner();
		/**
		 * Gibt die maximale Anzahl an Crew auf dem Objekt zurueck.
		 * @return Die maximale Crewmenge (<code>-1</code> = unbegrenzt)
		 */
		public int getMaxCrew();
	}
	
	/**
	 * Crewtransfer-Ziel "Schiff".
	 *
	 */
	private static class ShipTarget implements Target {
		private Ship ship;
		
		ShipTarget(Ship ship) {
			this.ship = ship;
		}

		public void finishTransfer() {
			ship.recalculateShipStatus();
		}

		public int getCrew() {
			return ship.getCrew();
		}
		
		public int getId() {
			return ship.getId();
		}

		public String getName() {
			return ship.getName();
		}

		public void setCrew(int crew) {
			ship.setCrew(crew);
		}
		
		public int getMaxCrew() {
			return ship.getTypeData().getCrew();
		}
		
		public User getOwner() {
			return ship.getOwner();
		}
	}
	
	/**
	 * Crewtransfer-Ziel "Basis".
	 *
	 */
	private static class BaseTarget implements Target {
		private Base base;
		
		BaseTarget(Base base) {
			this.base = base;
		}

		public void finishTransfer() {
			// EMPTY
		}

		public int getCrew() {
			return base.getBewohner()-base.getArbeiter();
		}

		public int getId() {
			return base.getId();
		}

		public String getName() {
			return base.getName();
		}

		public void setCrew(int crew) {
			base.setBewohner(base.getArbeiter()+crew);
		}

		public int getMaxCrew() {
			return -1;
		}

		public User getOwner() {
			return base.getOwner();
		}
	}
	
	private Ship ship = null;
	private Target datat = null;
	private int maxcrewf;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public CrewtauschController(Context context) {
		super(context);
		
		setTemplate("crewtausch.html");
		
		parameterNumber("ship");
		parameterNumber("tar");
		parameterString("mode");
		
		setPageTitle("Crewtransfer");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		
		int shipID = getInteger("ship");
		String mode = getString("mode");
		int tar = getInteger("tar");
		
		Ship ship = (Ship)db.get(Ship.class, shipID);
		if( (ship == null) || (ship.getOwner() != user) || (ship.getId() < 0)) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}	
		
		Target datat = null;
		int maxcrewf = 0;
		
		if( mode.equals("ss") ) {
			Ship aship = (Ship)db.get(Ship.class, tar);

			if( (aship == null) || (aship.getId() < 0) || !ship.getLocation().sameSector(0, aship, 0) ) {
				addError("Die beiden Schiffe befinden sich nicht im selben Sektor", Common.buildUrl("default", "module", "schiff", "ship", shipID) );
				
				return false;
			}

			maxcrewf = ship.getTypeData().getCrew();
			datat = new ShipTarget(aship);
		}
		else if( mode.equals("sb") ) {
			Base abase = (Base)db.get(Base.class, tar);

			if( (abase == null) || !ship.getLocation().sameSector(0, abase, abase.getSize()) ) {
				addError("Schiff und Basis befinden sich nicht im selben Sektor", Common.buildUrl("default", "module", "schiff", "ship", shipID) );
				
				return false;
			}

			maxcrewf = ship.getTypeData().getCrew();
			datat = new BaseTarget(abase);
		}
		else
		{
			addError("Dieser Transportweg ist unbekannt (hoer mit dem scheiss URL-Hacking auf) - Versuch geloggt", Common.buildUrl("default", "module", "schiff", "ship", shipID) );
		}
		
		if( ship.getOwner() != datat.getOwner() ) {
			addError("Eines der Schiffe geh&ouml;rt ihnene nicht", Common.buildUrl("default", "module", "schiff", "ship", shipID) );
				
			return false;
		}

		this.ship = ship;
		this.datat = datat;
		this.maxcrewf = maxcrewf;
		
		return true;	
	}
	
	/**
	 * Transferiert Crew vom Ausgangsschiff zum Zielschiff/Basis.
	 * @urlparam int send Die Anzahl der zu transferierenden Crew
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void sendAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("send");
		int send = getInteger("send");
		
		if( send < 0 ) {
			send = 0;
		}
		if( (datat.getMaxCrew() > -1) && (send > datat.getMaxCrew() - datat.getCrew()) ) {
			send = datat.getMaxCrew() - datat.getCrew();
		}
		if( send > ship.getCrew() ) {
			send = ship.getCrew();
		}
		
		if( send > 0 ) {
			t.setVar(	"crewtausch.transfer",	1,
						"transfer.way.to",		1,
						"transfer.count",		send );
			
			ship.setCrew(ship.getCrew()-send);
			datat.setCrew(datat.getCrew()+send);
			datat.finishTransfer();
			ship.recalculateShipStatus();
		}	
		
		redirect();
	}
	
	/**
	 * Transfer in umgekehrter Richtung.<br>
	 * @urlparam int rec Die Anzahl der zu transferierenden Crew
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void recAction() {
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("rec");
		int rec = getInteger("rec");
		
		if( rec < 0 ) {
			rec = 0;
		}
		if( rec > maxcrewf - ship.getCrew() ) {
			rec = maxcrewf - ship.getCrew();
		}
		if( rec > datat.getCrew() ) {
			rec = datat.getCrew();
		}
		
		if( rec > 0 ) {
			t.setVar(	"crewtausch.transfer",	1,
						"transfer.way.to",		0,
						"transfer.count",		rec );
		
			ship.setCrew(ship.getCrew()+rec);
			datat.setCrew(datat.getCrew()-rec);
			datat.finishTransfer();
			ship.recalculateShipStatus();
		}
			
		redirect();
	}
	
	/**
	 * Anzeige von Infos sowie Eingabe der zu transferierenden Crew.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		String mode = getString("mode");
		
		t.setVar(	"ship.id",			ship.getId(),
					"ship.name",		Common._plaintitle(ship.getName()),
					"ship.crew",		ship.getCrew(),
					"ship.maxcrew",		maxcrewf,
					"target.id",		datat.getId(),
					"target.name",		datat.getName(),
					"target.crew",		datat.getCrew(),
					"target.maxcrew",	(datat.getMaxCrew() > -1 ? datat.getMaxCrew() : "&#x221E;"),
					"global.mode",		mode,
					"global.mode.ss",	mode.equals("ss"),
					"global.mode.sb",	mode.equals("sb"),
					"target.send",		datat.getMaxCrew() - datat.getCrew(),
					"ship.receive",		ship.getTypeData().getCrew() - ship.getCrew());
	}
}
