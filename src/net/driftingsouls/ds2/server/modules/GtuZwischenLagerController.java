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

import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Die UI zum GTU-Zwischenlager
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
public class GtuZwischenLagerController extends DSGenerator {
	private SQLResultRow ship;
	private int handel;
	private int retryCount;
		
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public GtuZwischenLagerController(Context context) {
		super(context);
		
		setTemplate("gtuzwischenlager.html");
		
		this.ship = null;
		this.handel = 0;
		
		this.retryCount = 0;
		
		parameterNumber("ship");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		int shipId = getInteger("ship");
		
		ship = db.first("SELECT * FROM ships WHERE id>0 AND owner=",user.getId()," AND id=",shipId);
		if( ship.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}

		SQLResultRow handel = db.first("SELECT id FROM ships WHERE id>0 AND owner=",Faction.GTU," AND LOCATE('tradepost',status) AND system=",ship.getInt("system")," AND x=",ship.getInt("x")," AND y=",ship.getInt("y"));
		if( handel.isEmpty() ) {
			addError("Es existiert kein Handelsposten in diesem Sektor", Common.buildUrl("default", "module", "schiff", "ship", shipId) );
			
			return false;
		}
		
		this.handel = handel.getInt("id");
		
		t.setVar( "global.shipid", shipId );
		
		return true;
	}
	
	/**
	 * Transferiert nach der Bezahlung (jetzt) eigene Waren aus einem Handelsuebereinkommen
	 * auf das aktuelle Schiff
	 * @urlparam Integer entry Die ID des Zwischenlager-Eintrags
	 *
	 */
	public void transportOwnAction() {
		Database db = getDatabase();
		User user = this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		parameterNumber("entry");
		int entry = getInteger("entry");
		
		SQLResultRow tradeentry = db.first("SELECT * FROM gtu_zwischenlager WHERE posten=",this.handel," AND id=",entry);
	
		if( tradeentry.isEmpty() || ((tradeentry.getInt("user1") != user.getId()) && (tradeentry.getInt("user2") != user.getId())) ) {
			addError("Es wurde kein passender Handelseintrag gefunden", Common.buildUrl("default", "module", "schiff", "ship", this.ship.getInt("id")) );
			this.setTemplate("");
			
			return;	
		}
		
		// Der Handelspartner
		User tradepartner = getContext().createUserObject(tradeentry.getInt("user2"));
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo1"));
		Cargo tradecargoneed = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo1need"));
		// Die Bezahlung
		Cargo owncargo = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo2"));
		Cargo owncargoneed = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo2need"));
	
		if( tradepartner.getId() == user.getId() ) {
			tradepartner = getContext().createUserObject(tradeentry.getInt("user1"));
			tradecargo = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo2"));
			tradecargoneed = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo2need"));
			owncargo = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo1"));
			owncargoneed = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo1need"));
		}
	
		Cargo tmpowncargoneed = (Cargo)owncargoneed.clone();
		
		tmpowncargoneed.substractCargo( owncargo );
		if( !tmpowncargoneed.isEmpty() ) {
			addError("Sie m&uuml;ssen die Waren erst komplett bezahlen", Common.buildUrl("default", "module", "schiff", "ship", this.ship.getInt("id")) );
			this.setTemplate("");
			
			return;	
		}
			
		SQLResultRow shiptype = ShipTypes.getShipType(this.ship);
		
		Cargo shipCargo = new Cargo(Cargo.Type.STRING, ship.getString("cargo"));
		long freecargo = shiptype.getLong("cargo") - shipCargo.getMass();

		Cargo transportcargo = null;
		if( freecargo <= 0 ) {
			addError("Sie verf&uuml;gen nicht &uuml;ber genug freien Cargo um Waren abholen zu k&ouml;nnen");
			this.redirect("viewEntry");
			
			return;	
		}	
		else if( freecargo < tradecargo.getMass() ) {
			transportcargo = ((Cargo)tradecargo.clone()).cutCargo( freecargo );	
		}
		else {
			transportcargo = (Cargo)tradecargo.clone();	
		}		

		t.setBlock("_GTUZWISCHENLAGER","transferlist.res.listitem","transferlist.res.list");
				
		ResourceList reslist = transportcargo.getResourceList();
		Resources.echoResList( t, reslist, "transferlist.res.list" );
		
		t.setVar("global.transferlist",1);
		
		shipCargo.addCargo( transportcargo );
		tradecargoneed.substractCargo( transportcargo );
		
		db.tBegin();
		db.tUpdate(1, "UPDATE ships SET cargo='",shipCargo.save(),"' WHERE id>0 AND id=",this.ship.getInt("id")," AND cargo='",shipCargo.save(true),"'");

		if( tradecargoneed.isEmpty() && owncargo.isEmpty() ) {
			db.tUpdate(1, "DELETE FROM gtu_zwischenlager WHERE id=",entry);
			if( !db.tCommit() ) {
				if( this.retryCount < 3 ) {
					this.retryCount++;
					this.redirect("transportOwn");
				
					return;
				}
				addError("Die Waren konnten nicht erfolgreich zum Schiff transferiert werden");	
			}
			
			t.setVar( "transferlist.backlink", 1 );
			
			return;
		}
		
		if( tradeentry.getInt("user1") == user.getId() ) {
			db.tUpdate(1, "UPDATE gtu_zwischenlager SET cargo1='",tradecargo.save(),"',cargo1need='",tradecargoneed.save(),"' " ,
					"WHERE id='",entry,"' AND cargo1='",tradecargo.save(true),"' AND cargo1need='",tradecargoneed.save(true),"'");
		}
		else {
			db.tUpdate(1, "UPDATE gtu_zwischenlager SET cargo2='",tradecargo.save(),"',cargo2need='",tradecargoneed.save(),"' " ,
					"WHERE id='",entry,"' AND cargo2='",tradecargo.save(true),"' AND cargo2need='",tradecargoneed.save(true),"'");		
		}

		if( !db.tCommit() ) {
			if( this.retryCount < 3 ) {
				this.retryCount++;
				this.redirect("transportOwn");
				
				return;
			}
			addError("Die Waren konnten nicht erfolgreich zum Schiff transferiert werden");	
		}
		
		this.redirect("viewEntry");
	}

	/**
	 * Transferiert fuer einen Eintrag noch fehlende Resourcen 
	 *
	 */
	public void transportMissingAction() {
		// TODO
	}
	
	/**
	 * Zeigt einen Handelsuebereinkommen an
	 * @urlparam Integer entry Die ID des Zwischenlager-Eintrags
	 *
	 */
	public void viewEntryAction() {
		Database db = getDatabase();
		User user = this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		parameterNumber("entry");
		int entry = getInteger("entry");
		
		SQLResultRow tradeentry = db.first("SELECT * FROM gtu_zwischenlager WHERE posten=",this.handel," AND id=",entry);
	
		if( tradeentry.isEmpty() || ((tradeentry.getInt("user1") != user.getId()) && (tradeentry.getInt("user2") != user.getId())) ) {
			addError("Es wurde kein passender Handelseintrag gefunden", Common.buildUrl("default", "module", "schiff", "ship", this.ship.getInt("id")) );
			this.setTemplate("");
			
			return;	
		}
		
		t.setVar("global.entry", 1);
		
		t.setBlock("_GTUZWISCHENLAGER","res.listitem","res.list");
	
		// Der Handelspartner
		User tradepartner = getContext().createUserObject(tradeentry.getInt("user2"));
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo1"));
		// Die Bezahlung
		Cargo owncargo = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo2"));
		Cargo owncargoneed = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo2need"));
	
		if( tradepartner.getId() == user.getId() ) {
			tradepartner = getContext().createUserObject(tradeentry.getInt("user1"));
			tradecargo = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo2"));
			owncargo = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo1"));
			owncargoneed = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo1need"));
		}
		
		t.setVar(	"tradeentry.id",			tradeentry.getInt("id"),
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
	 * Zeigt die Liste aller Handelsvereinbarungen auf diesem Handelsposten an, an denen der aktuelle Spieler beteiligt ist
	 */
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = this.getUser();
		TemplateEngine t = this.getTemplateEngine();
		
		t.setVar("global.tradelist",1);
		t.setBlock("_GTUZWISCHENLAGER","tradelist.listitem","tradelist.list");
		t.setBlock("tradelist.listitem","res.listitem","res.list");
	
		SQLQuery tradeentry = db.query("SELECT * FROM gtu_zwischenlager WHERE posten=",this.handel," AND (user1=",user.getId()," OR user2=",user.getId(),")");
		while( tradeentry.next() ) {
			User tradepartner = getContext().createUserObject(tradeentry.getInt("user2"));
			Cargo tradecargo = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo1"));
			Cargo owncargo = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo2"));
			Cargo owncargoneed = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo2need"));
		
			if( tradepartner.getId() == user.getId() ) {
				tradepartner = getContext().createUserObject(tradeentry.getInt("user1"));
				tradecargo = new Cargo(Cargo.Type.STRING,tradeentry.getString("cargo2"));
				owncargo = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo1"));
				owncargoneed = new Cargo(Cargo.Type.STRING, tradeentry.getString("cargo1need"));
			}
		
			t.setVar(	"list.entryid",			tradeentry.getInt("id"),
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
		tradeentry.free();
	}
}
