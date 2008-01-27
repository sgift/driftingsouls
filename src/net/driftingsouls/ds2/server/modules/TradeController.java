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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Verkauft Waren an einem Handelsposten
 * @author Christopher Jung
 * @urlparam Integer ship die ID des Schiffes, das Waren verkaufen moechte
 * @urlparam Integer tradepost die ID des Handelspostens, an dem die Waren verkauft werden sollen
 */
public class TradeController extends DSGenerator {
	private SQLResultRow ship = null;
	private Cargo shipCargo = null;
	private Cargo kurse = null;
	private int sellRetryCount = 0;
	private SQLResultRow posten = null;
	private String place = null;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public TradeController(Context context) {
		super(context);

		setTemplate("trade.html");
		
		parameterNumber("ship");
		parameterNumber("tradepost");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		int shipId = getInteger("ship");
		
		this.ship = db.first("SELECT * FROM ships WHERE owner='",getUser().getID(),"' AND id>0 AND id='",shipId,"'");
		if( this.ship.isEmpty() ) {
			addError( "Fehler: Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl(getContext(), "default", "module", "schiffe") );
			
			return false;
		}
		
		int tradepost = getInteger("tradepost");
		SQLResultRow handel = db.first("SELECT * FROM ships WHERE id>0 AND system=",this.ship.getInt("system")," AND x=",this.ship.getInt("x")," AND y=",this.ship.getInt("y")," AND LOCATE('tradepost',status) AND id='",tradepost,"'");
		if( handel.isEmpty() ) {
			addError( "Fehler: Der angegebene Handelsposten konnte nicht im Sektor lokalisiert werden", Common.buildUrl(getContext(), "default", "module", "schiff", "ship", shipId));
			
			return false;
		}
		
		this.posten = handel;
		
		this.shipCargo = new Cargo( Cargo.Type.STRING, this.ship.getString("cargo") );
		
		this.place = "p"+handel.getInt("id");
		SQLResultRow kurse = db.first("SELECT kurse FROM gtu_warenkurse WHERE place='p",handel.getInt("id"),"'");
		
		if( kurse.isEmpty() ) {
			this.place="ps"+handel.getInt("system");
			kurse = db.first("SELECT kurse FROM gtu_warenkurse WHERE place='ps",handel.getInt("system"),"'");
			
			if( kurse.isEmpty() ) {
				this.place = "tradepost";
				kurse = db.first("SELECT kurse FROM gtu_warenkurse WHERE place='tradepost'");
			}
		}
		this.kurse = new Cargo( Cargo.Type.STRING, kurse.getString("kurse") );
		this.kurse.setOption( Cargo.Option.SHOWMASS, false );
		
		getTemplateEngine().setVar( "global.shipid", shipId );
		getTemplateEngine().setVar( "global.tradepost", handel.getInt("id") );
		
		getTemplateEngine().setBlock("_TRADE","msgs.listitem","msgs.list");
		
		return true;
	}

	/**
	 * Verkauft die angegebenen Waren
	 * @urlparam Integer ${resid}to Verkauft die Resource mit der ID ${resid} in der angegebenen Menge
	 *
	 */
	public void sellAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		int tick = getContext().get(ContextCommon.class).getTick();
		int system = this.ship.getInt("system");
		
		SQLResultRow stats = db.first("SELECT stats FROM stats_verkaeufe WHERE tick=",tick," AND place='",this.place,"' AND system=",system);
		Cargo statsCargo = null;
		if( stats.isEmpty() ) {
			statsCargo = new Cargo();
		}
		else {
			statsCargo = new Cargo( Cargo.Type.STRING, stats.getString("stats") );
		}
		
		Cargo tpcargo = new Cargo( Cargo.Type.STRING, this.posten.getString("cargo") );
	
		long totalRE = 0;
		boolean changed = false;
	
		ResourceList reslist = this.kurse.getResourceList();
		for( ResourceEntry res : reslist ) {
			parameterNumber( res.getId()+"to" );
			long tmp = getInteger( res.getId()+"to" );

			if( tmp > 0 ) {
				if( tmp > shipCargo.getResourceCount( res.getId() ) ) {
					tmp = shipCargo.getResourceCount( res.getId() );
				}
			
				if( tmp <= 0 ) {
					continue;
				}
			
				long get = (long)(tmp*res.getCount1()/1000d);
			
				t.setVar(	"waren.count",	tmp,
							"waren.name",	res.getName(),
							"waren.img",	res.getImage(),
							"waren.re",		Common.ln(get) );
								
				t.parse("msgs.list","msgs.listitem",true);
			
				totalRE += get;
				changed = true;
				shipCargo.substractResource( res.getId(), tmp );
	
				statsCargo.addResource( res.getId(), tmp );
				tpcargo.addResource( res.getId(), tmp );
			}
		}
		
		if( changed ) {
			db.tBegin(true);
			
			SQLResultRow statid = db.first("SELECT id FROM stats_verkaeufe WHERE tick=",tick," AND place='",this.place,"' AND system=",system);
			if( statid.isEmpty() ) {
				db.update("INSERT INTO stats_verkaeufe (tick,place,system,stats) VALUES (",tick,",'",this.place,"',",system,",'",statsCargo.save(),"')");
			}
			else {		
				db.tUpdate(1, "UPDATE stats_verkaeufe SET stats='",statsCargo.save(),"' WHERE id=",statid.getInt("id")," AND stats='",statsCargo.save(true),"'");
			}
			
			db.tUpdate(1, "UPDATE ships SET cargo='",tpcargo.save(),"' WHERE id>0 AND id='",this.posten.getInt("id"),"' AND cargo='",this.posten.getString("cargo"),"'");
		
	
			db.tUpdate(1, "UPDATE ships SET cargo='",this.shipCargo.save(),"' WHERE id>0 AND id='",this.ship.getInt("id"),"' AND cargo='",this.shipCargo.save(true),"'");
	
			Ships.recalculateShipStatus(this.ship.getInt("id"));
	
			getUser().transferMoneyFrom(this.posten.getInt("owner"), totalRE, "Warenverkauf Handelsposten bei "+this.ship.getInt("system")+":"+this.ship.getInt("x")+"/"+this.ship.getInt("y"), false, User.TRANSFER_SEMIAUTO );
			
			if( !db.tCommit() ) {
				if( !db.getErrorStatus() && (this.sellRetryCount < 3) ) {
					t.setVar("msgs.list","");
					this.sellRetryCount++;
					
					redirect("sell");
					
					return;
				}
				addError("Fehler: Die Transaktion der Waren war nicht erfolgreich");
			}
		}
		
		redirect();
	}
	
	/**
	 * Zeigt die eigenen Waren sowie die Warenkurse am Handelsposten an
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar("error.none",1);
		t.setBlock("_TRADE","res.listitem","res.list");

		ResourceList reslist = this.kurse.getResourceList();
		for( ResourceEntry res : reslist ) {
			if( !this.shipCargo.hasResource( res.getId() ) ) {
				continue;	
			}
			
			String preis = "";
			if( res.getCount1() < 50) {
				preis = "Kein Bedarf";
			}
			else {
				preis = Common.ln(res.getCount1()/1000d)+" RE";
			}
	
			t.setVar(	"res.img",		res.getImage(),
						"res.id",		res.getId(),
						"res.name",		res.getName(),
						"res.cargo",	this.shipCargo.getResourceCount( res.getId() ),
						"res.re",		preis );
						
			t.parse("res.list","res.listitem",true);
		}
	}
}
