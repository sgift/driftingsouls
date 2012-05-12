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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

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
 * 
 * Waren koennen erst abgeholt werden, wenn die eigenen Zahlungen geleistet wurden</p>
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des Schiffes, welches auf das GTU-Zwischenlager zugreifen will
 * 
 */
// TODO: Die ID des Handelspostens sollte per URL spezifiziert werden
@Module(name="gtuzwischenlager")
public class GtuZwischenLagerController extends TemplateGenerator {
	private Ship ship;
	private int handel;
		
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public GtuZwischenLagerController(Context context) {
		super(context);
		
		setTemplate("gtuzwischenlager.html");
		
		this.ship = null;
		this.handel = 0;
		
		parameterNumber("ship");
		
		setPageTitle("GTU-Lager");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		int shipId = getInteger("ship");
		
		ship = (Ship)db.get(Ship.class, shipId);
		if( (ship == null) || (ship.getId() < 0) || (ship.getOwner() != user) ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}

		Ship handel = (Ship)db.createQuery("from Ship where id>0 and owner<0 and locate('tradepost',status)!=0 and " +
				"system=? and x=? and y=?")
			.setInteger(0, ship.getSystem())
			.setInteger(1, ship.getX())
			.setInteger(2, ship.getY())
			.setMaxResults(1)
			.uniqueResult();
		if( handel == null ) {
			addError("Es existiert kein Handelsposten in diesem Sektor", Common.buildUrl("default", "module", "schiff", "ship", shipId) );
			
			return false;
		}
		
		this.handel = handel.getId();
		
		t.setVar( "global.shipid", shipId );
		
		return true;
	}
	
	/**
	 * Transferiert nach der Bezahlung (jetzt) eigene Waren aus einem Handelsuebereinkommen
	 * auf das aktuelle Schiff.
	 * @urlparam Integer entry Die ID des Zwischenlager-Eintrags
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void transportOwnAction() {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		parameterNumber("entry");
		int entry = getInteger("entry");
		
		GtuZwischenlager tradeentry = (GtuZwischenlager)db.get(GtuZwischenlager.class, entry);
		
		if( (tradeentry == null) || (tradeentry.getPosten().getId() != this.handel) || ((tradeentry.getUser1() != user) && (tradeentry.getUser2() != user)) ) {
			addError("Es wurde kein passender Handelseintrag gefunden", Common.buildUrl("default", "module", "schiff", "ship", this.ship.getId()) );
			this.setTemplate("");
			
			return;	
		}
		
		//  Der Handelspartner
		User tradepartner = tradeentry.getUser2();
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = tradeentry.getCargo1();
		Cargo tradecargoneed = tradeentry.getCargo1Need();
		// Die Bezahlung
		Cargo owncargo = tradeentry.getCargo2();
		Cargo owncargoneed = tradeentry.getCargo2Need();
	
		if( tradepartner.getId() == user.getId() ) {
			tradepartner = tradeentry.getUser1();
			tradecargo = tradeentry.getCargo2();
			tradecargoneed = tradeentry.getCargo2Need();
			owncargo = tradeentry.getCargo1();
			owncargoneed = tradeentry.getCargo1Need();
		}
	
		Cargo tmpowncargoneed = new Cargo(owncargoneed);
		
		tmpowncargoneed.substractCargo( owncargo );
		if( !tmpowncargoneed.isEmpty() ) {
			addError("Sie m&uuml;ssen die Waren erst komplett bezahlen", Common.buildUrl("default", "module", "schiff", "ship", this.ship.getId()) );
			this.setTemplate("");
			
			return;	
		}
			
		ShipTypeData shiptype = this.ship.getTypeData();
		
		Cargo shipCargo = new Cargo(this.ship.getCargo());
		long freecargo = shiptype.getCargo() - shipCargo.getMass();

		Cargo transportcargo = null;
		if( freecargo <= 0 ) {
			addError("Sie verf&uuml;gen nicht &uuml;ber genug freien Cargo um Waren abholen zu k&ouml;nnen");
			this.redirect("viewEntry");
			
			return;	
		}	
		else if( freecargo < tradecargo.getMass() ) {
			transportcargo = new Cargo(tradecargo).cutCargo( freecargo );	
		}
		else {
			transportcargo = new Cargo(tradecargo);	
		}		

		t.setBlock("_GTUZWISCHENLAGER","transferlist.res.listitem","transferlist.res.list");
				
		ResourceList reslist = transportcargo.getResourceList();
		Resources.echoResList( t, reslist, "transferlist.res.list" );
		
		t.setVar("global.transferlist",1);
		
		shipCargo.addCargo( transportcargo );
		tradecargoneed.substractCargo( transportcargo );
		
		ship.setCargo(shipCargo);

		if( tradecargoneed.isEmpty() && owncargo.isEmpty() ) {
			db.delete(tradeentry);
			
			t.setVar( "transferlist.backlink", 1 );
			
			return;
		}
		
		if( tradeentry.getUser1() == user ) {
			tradeentry.setCargo1(tradecargo);
			tradeentry.setCargo1Need(tradecargoneed);
		}
		else {
			tradeentry.setCargo2(tradecargo);
			tradeentry.setCargo2Need(tradecargoneed);		
		}

		this.redirect("viewEntry");
	}

	/**
	 * Transferiert fuer einen Eintrag noch fehlende Resourcen.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void transportMissingAction() {
		// TODO
	}
	
	/**
	 * Zeigt einen Handelsuebereinkommen an.
	 * @urlparam Integer entry Die ID des Zwischenlager-Eintrags
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void viewEntryAction() {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		parameterNumber("entry");
		int entry = getInteger("entry");
		
		GtuZwischenlager tradeentry = (GtuZwischenlager)db.get(GtuZwischenlager.class, entry);
	
		if( (tradeentry == null) || (tradeentry.getPosten().getId() != this.handel) || ((tradeentry.getUser1() != user) && (tradeentry.getUser2() != user)) ) {
			addError("Es wurde kein passender Handelseintrag gefunden", Common.buildUrl("default", "module", "schiff", "ship", this.ship.getId()) );
			this.setTemplate("");
			
			return;	
		}
		
		t.setVar("global.entry", 1);
		
		t.setBlock("_GTUZWISCHENLAGER","res.listitem","res.list");
	
		// Der Handelspartner
		User tradepartner = tradeentry.getUser2();
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = tradeentry.getCargo1();
		// Die Bezahlung
		Cargo owncargo = tradeentry.getCargo2();
		Cargo owncargoneed = tradeentry.getCargo2Need();;
	
		if( tradepartner.getId() == user.getId() ) {
			tradepartner = tradeentry.getUser1();
			tradecargo = tradeentry.getCargo2();
			owncargo = tradeentry.getCargo1();
			owncargoneed = tradeentry.getCargo1Need();
		}
		
		t.setVar(	"tradeentry.id",			tradeentry.getId(),
					"tradeentry.partner",		Common._title(tradepartner.getName()),
					"tradeentry.missingcargo",	"",
					"tradeentry.waren",			"" );
						
				
		// (zukuenftig) eigene Waren anzeigen		
		ResourceList reslist = tradecargo.getResourceList();
		Resources.echoResList( t, reslist, "tradeentry.waren", "res.listitem" );
	
		// noch ausstehende Bezahlung anzeigen
		owncargoneed.substractCargo( owncargo );
		if( !owncargoneed.isEmpty() ) {
			reslist = owncargoneed.getResourceList();
			Resources.echoResList( t, reslist, "tradeentry.missingcargo", "res.listitem" );
		}
	}
	
	/**
	 * Zeigt die Liste aller Handelsvereinbarungen auf diesem Handelsposten an, an denen der aktuelle Spieler beteiligt ist.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		t.setVar("global.tradelist",1);
		t.setBlock("_GTUZWISCHENLAGER","tradelist.listitem","tradelist.list");
		t.setBlock("tradelist.listitem","res.listitem","res.list");
	
		List<?> tradelist = db.createQuery("from GtuZwischenlager where posten=? and (user1= :user or user2= :user)")
			.setInteger(0, this.handel)
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=tradelist.iterator(); iter.hasNext(); ) {
			GtuZwischenlager tradeentry = (GtuZwischenlager)iter.next();
			
			User tradepartner = tradeentry.getUser2();
			Cargo tradecargo = tradeentry.getCargo1();
			Cargo owncargo = tradeentry.getCargo2();
			Cargo owncargoneed = tradeentry.getCargo2Need();
		
			if( tradepartner == user ) {
				tradepartner = tradeentry.getUser1();
				tradecargo = tradeentry.getCargo2();
				owncargo = tradeentry.getCargo1();
				owncargoneed = tradeentry.getCargo1Need();
			}
		
			t.setVar(	"list.entryid",			tradeentry.getId(),
						"list.user",			Common._title(tradepartner.getName()),
						"res.list",				"",
						"list.cargoreq.list",	"",
						"list.status",			"bereit" );

			// (zukuenftig) eigene Waren anzeigen
			ResourceList reslist = tradecargo.getResourceList();
			Resources.echoResList( t, reslist, "res.list" );
			
			List<ItemCargoEntry> itemlist = tradecargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry item = itemlist.get(i);
				Item itemobject = item.getItemObject();
				if( itemobject.isUnknownItem() ) {
					user.addKnownItem(item.getItemID());
				}
			}
			
			// noch ausstehende Bezahlung anzeigen
			owncargoneed.substractCargo( owncargo );
			
			if( !owncargoneed.isEmpty() ) {
				reslist = owncargoneed.getResourceList();
				Resources.echoResList( t, reslist, "list.cargoreq.list", "res.listitem" );
			}
		
			t.parse("tradelist.list","tradelist.listitem",true);
		}
	}
}
