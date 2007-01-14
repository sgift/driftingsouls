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
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Pluendert ein Schiff
 * @author Christopher Jung
 *
 * @urlparam Integer from Die ID des Schiffes, mit dem ein anderes Schiff gepluendert werden soll
 * @urlparam Integer to Die ID des zu pluendernden Schiffes
 */
public class PluendernController extends DSGenerator {
	private SQLResultRow shipFrom;
	private SQLResultRow shipTo;
	private int retryCount;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public PluendernController(Context context) {
		super(context);
		
		setTemplate("pluendern.html");
		
		parameterNumber("from");
		parameterNumber("to");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = this.getUser();
		
		int from = getInteger("from");
		int to = getInteger("to");
		
		SQLResultRow shipFrom = db.first("SELECT * FROM ships WHERE id>0 AND id=",from," AND owner=",user.getID());
		if( shipFrom.isEmpty() ) {
			addError("Sie brauchen ein Schiff um zu pl&uuml;ndern", Common.buildUrl(getContext(), "default", "module", "schiff", "ship", from));
					
			return false;	
		}

		final String errorurl = Common.buildUrl(getContext(), "default", "module", "schiff", "ship", from);
		
		SQLResultRow shipTo = db.first("SELECT * FROM ships WHERE id>0 AND id=",to);
		if( shipTo.isEmpty() ) {
			addError("Das angegebene Zielschiff existiert nicht", errorurl);
					
			return false;
		}
		SQLResultRow shipTypeTo = Ships.getShipType(shipTo);
	
		if( user.isNoob() ) {
			addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher nicht pl&uuml;nndern<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden", errorurl);
					
			return false;
		}
	
		if( to == from ) {
			addError("Sie k&ouml;nnen nicht sich selbst pl&uuml;ndern", errorurl);
					
			return false;
		}
		
		User taruser = getContext().createUserObject( shipTo.getInt("owner"));
		
		if( taruser.isNoob() ) {
			addError("Dieser Kolonist steht unter GCP-Schutz", errorurl);
					
			return false;
		}
		
		if( (taruser.getVacationCount() != 0) && (taruser.getWait4VacationCount() == 0) ) {
			addError("Sie k&ouml;nnen Schiffe dieses Spielers nicht kapern oder pl&uuml;ndern solange er sich im Vacation-Modus befindet", errorurl);
					
			return false;
		}
		
		if( (shipTo.getInt("visibility") != 0) && (shipTo.getInt("visibility") != user.getID()) ) {
			addError("Sie k&ouml;nnen nur pl&uuml;ndern, was sie auch sehen", errorurl);
					
			return false;
		}

		if( !Location.fromResult(shipFrom).sameSector( 0, Location.fromResult(shipTo), 0) ) {
			addError("Das zu pl&uuml;ndernde Schiff befindet sich nicht im selben Sektor", errorurl);
					
			return false;
		}

		if( (shipFrom.getInt("battle") != 0) || (shipTo.getInt("battle") != 0) ) {
			addError("Eines der Schiffe ist in einen Kampf verwickelt", errorurl);
					
			return false;
		}

		if( shipTypeTo.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) {
			addError("Sie k&ouml;nnen autonome orbitale Verteidigungsanlagen weder kapern noch pl&uuml;ndern", errorurl);
					
			return false;
		}

		// IFF-Check
		boolean disableIFF = shipTo.getString("status").indexOf("disable_iff") > -1;
		if( disableIFF ) {
			addError("Das Schiff kann nicht gepl&uuml;ndert werden", errorurl);
					
			return false;
		}

		if( shipTo.getString("docked").length() > 0 ) {
			if( shipTo.getString("docked").charAt(0) == 'l' ) {
				addError("Sie k&ouml;nnen gelandete Schiffe weder kapern noch pl&uuml;ndern", errorurl);
					
				return false;
			} 

			SQLResultRow mastership = db.first("SELECT * FROM ships WHERE id>0 AND id=",shipTo.getString("docked"));
			if( ( (mastership.getInt("crew") != 0) ) && (mastership.getInt("engine") != 0) && 
				(mastership.getInt("weapons") != 0) ) {
				addError("Das Schiff, an das das feindliche Schiff angedockt hat, ist noch bewegungsf&auml;hig", errorurl);
					
				return false;
			}
		}

		if( (shipTo.getInt("crew") != 0) && (shipTypeTo.getInt("cost") != 0) && (shipTo.getInt("engine") != 0) ) {
			addError("Feindliches Schiff nicht bewegungsunf&auml;hig", errorurl);
					
			return false;
		}

		if( Ships.hasShipTypeFlag(shipTypeTo, Ships.SF_KEIN_TRANSFER) ) {
			addError("Sie k&ouml;nnen keine Waren zu oder von diesem Schiff transferieren", errorurl);
					
			return false;
		}
		
		if( Ships.hasShipTypeFlag(shipTypeTo, Ships.SF_NICHT_PLUENDERBAR) ) {
			addError("Sie k&ouml;nnen keine Waren von diesem Schiff pl&uuml;ndern", errorurl);
					
			return false;
		}

		if( (shipTypeTo.getInt("class") == ShipClasses.STATION.ordinal() ) && (shipTo.getInt("crew") != 0) ) {
			addError("Solange die Crew &uuml;ber die Waren wacht werden sie hier nichts klauen k&ouml;nnen", errorurl);
					
			return false;
		}
		
		SQLResultRow shipTypeFrom = Ships.getShipType(shipFrom);		
		if( Ships.hasShipTypeFlag(shipTypeFrom, Ships.SF_KEIN_TRANSFER) ) {
			addError("Sie k&ouml;nnen keine Waren zu oder von ihrem Schiff transferieren", errorurl);
					
			return false;
		}
		
		this.shipFrom = shipFrom;
		this.shipTo = shipTo;
		
		this.getTemplateEngine().set_var( 
				"fromship.name",	this.shipFrom.getString("name"),
				"fromship.id",		this.shipFrom.getInt("id"),
				"toship.name",		this.shipTo.getString("name"),
				"toship.id",		this.shipTo.getInt("id") );
													
		return true;
	}

	/**
	 * Transferiert Waren zwischen den Schiffen
	 * @urlparam Integer $resID+"to" Die Menge der Ware $resID, welche zum Zielschiff transferiert werden soll
	 * @urlparam Integer $resID+"from" Die Menge der Ware $resID, welche vom Zielschiff herunter transferiert werden soll
	 *
	 */
	public void transferAction() {
		Database db = getDatabase();
		TemplateEngine t = this.getTemplateEngine();
		User user = this.getUser();
		
		Cargo cargofrom = new Cargo( Cargo.Type.STRING, this.shipFrom.getString("cargo") );
		Cargo cargoto = new Cargo( Cargo.Type.STRING, this.shipTo.getString("cargo") );
		
		SQLResultRow shipTypeTo = Ships.getShipType( this.shipTo );
		SQLResultRow shipTypeFrom = Ships.getShipType( this.shipFrom );
	
		long curcargoto = shipTypeTo.getLong("cargo") - cargoto.getMass();
		long curcargofrom = shipTypeFrom.getLong("cargo") - cargofrom.getMass();

		StringBuilder msg = new StringBuilder();
		Cargo newCargoTo = (Cargo)cargoto.clone();
		Cargo newCargoFrom = (Cargo)cargofrom.clone();
	
		long totaltransferfcount = 0;
		boolean transfer = false;
		
		t.set_block("_PLUENDERN", "transfer.listitem", "transfer.list" );

		ResourceList reslist = cargofrom.compare( cargoto, true );
		for( ResourceEntry res : reslist ) {
			this.parameterNumber(res.getId()+"to");
			this.parameterNumber(res.getId()+"from");
			
			long transt = this.getInteger(res.getId()+"to");
		 	long transf = this.getInteger(res.getId()+"from");

			t.start_record();

			t.set_var( "res.image", res.getImage() );

			// Transfer vom Ausgangsschiff zum Zielschiff
			if( transt > 0 ) {				
				t.set_var(	"transfer.target",	this.shipTo.getString("name"),
							"transfer.count",	transt );
				
				if( transt > res.getCount1() ) {
					transt = res.getCount1();
					
					t.set_var(	"transfer.notenoughcargo",	1,
								"transfer.cargo",			res.getCount1() );
				} 

				if( curcargoto - transt < 0 ) {
					transt = (long)( curcargoto/(double)Cargo.getResourceMass( res.getId(), 1 ) );
					
					t.set_var(	"transfer.notenoughspace",	1,
								"transfer.newcount",		transt );
				}
				
				// Falls es sich um ein unbekanntes Item handelt, dann dem Besitzer des Zielschiffes bekannt machen
				if( (transt > 0) && res.getId().isItem() ) {
					int itemid = res.getId().getItemID();
					if( Items.get().item(itemid).isUnknownItem() ) {
						User targetUser = getContext().createUserObject(this.shipTo.getInt("owner"));
						targetUser.addKnownItem(itemid);
					}
				}
				
				newCargoTo.addResource( res.getId(), transt );
				newCargoFrom.substractResource( res.getId(), transt );
				curcargoto = shipTypeTo.getLong("cargo") - newCargoTo.getMass();
				curcargofrom = shipTypeFrom.getLong("cargo") - newCargoFrom.getMass();
				
				t.set_var( "transfer.totalcargo", newCargoTo.getResourceCount( res.getId() ) );
				
				if( transt > 0 ) {
					transfer = true;
					msg.append("[resource="+res.getId()+")"+transt+"[/resource] zur&uuml;ckgegeben.\n");
				}
				
				t.parse("transfer.list", "transfer.listitem", true);
			}
			// Transfer vom Zielschiff zum Ausgangsschiff
			else if( transf > 0 ) {				
				t.set_var(	"transfer.target",	this.shipFrom.getString("name"),
							"transfer.count",	transf );
				
				if( transf > res.getCount2() ) {
					transf = res.getCount2();
					
					t.set_var(	"transfer.notenoughcargo",	1,
								"transfer.cargo",			res.getCount2() );
				} 
				
				if( curcargofrom - transf < 0 ) {
					transf = (long)( curcargofrom/(double)Cargo.getResourceMass( res.getId(), 1 ) );
					
					t.set_var(	"transfer.notenoughspace",	1,
								"transfer.newcount",		transf );
				}
				
				// Falls es sich um ein unbekanntes Item handelt, dann dieses dem Spieler bekannt machen
				if( (transf > 0) && res.getId().isItem() ) {
					int itemid = res.getId().getItemID();
					if( Items.get().item(itemid).isUnknownItem() ) {
						user.addKnownItem(itemid);
					}
				}
				
				totaltransferfcount += transf;
				
				curcargoto = shipTypeTo.getLong("cargo") - newCargoTo.getMass();
				curcargofrom = shipTypeFrom.getLong("cargo") - newCargoFrom.getMass();
				
				newCargoFrom.addResource( res.getId(), transf );
				newCargoTo.substractResource( res.getId(), transf );
				
				t.set_var( "transfer.totalcargo", newCargoFrom.getResourceCount( res.getId() ) );
				
				if( transf > 0 ) {
					transfer = true;
					msg.append("[resource="+res.getId()+")"+transf+"[/resource] gestohlen.\n");
				}
				
				t.parse("transfer.list", "transfer.listitem", true);
			}	
			
			t.stop_record();
			t.clear_record();
		}

		// Transmission versenden
		if( transfer && (msg.length() > 0) ) {
			msg.insert(0, this.shipTo.getString("name")+" ("+this.shipTo.getInt("id")+") wird von "+
					this.shipFrom.getString("name")+" ("+this.shipFrom.getInt("id")+") bei "+
					Location.fromResult(this.shipFrom)+" gepl&uuml;ndert.\n");
			
			PM.send(getContext(), this.shipFrom.getInt("owner"), this.shipTo.getInt("owner"), "Schiff gepl&uuml;ndert", msg.toString());
		}
		
		// Schiffe aktuallisieren
		if( transfer ) {
			db.tBegin();
			db.tUpdate(1, "UPDATE ships SET cargo='"+newCargoFrom.save()+"' WHERE id>0 AND id="+this.shipFrom.getInt("id")+" AND cargo='"+newCargoFrom.save(true)+"'");
			db.tUpdate(1, "UPDATE ships SET cargo='"+newCargoTo.save()+"' WHERE id>0 AND id="+this.shipTo.getInt("id")+" AND cargo='"+newCargoTo.save(true)+"'");
	
			String status = Ships.recalculateShipStatus(this.shipTo.getInt("id"));
			Ships.recalculateShipStatus(this.shipFrom.getInt("id"));
			
			// Falls das Schiff instabil ist, dann diesem den "destory"-Status geben,
			// damit der Schiffstick dieses zerstoert
			if( (totaltransferfcount > 0) && Ships.hasShipTypeFlag(shipTypeTo, Ships.SF_INSTABIL)  ) {
				t.set_var("toship.isinstabil", 1);
			
				String statust = status;
				
				if( statust.length() > 0 ) {
					statust += " destroy";
				}	
				else {
					statust += "destroy";
				}	
			
				db.tUpdate(1, "UPDATE ships SET status='"+statust+"' WHERE id>0 AND id="+this.shipTo.getInt("id")+" AND status='"+status+"'");
			}
		
			// Transaktion beenden
			if( !db.tCommit() ) {
				t.set_var( "transfer.list", "");
			
				if( this.retryCount < 3 ) {
					this.retryCount++;
					this.redirect("transfer");
				}
				else {
					addError("Der Transfer der Waren ist fehlgeschlagen");
					this.redirect();
				}
				
				return;
			}
		
			this.shipFrom.put("cargo", newCargoFrom.save());
			this.shipTo.put("cargo", newCargoTo.save());
		}
		
		this.redirect();
	}
	
	/**
	 * Zeigt die GUI fuer den Warentransfer an
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = this.getTemplateEngine();
		
		Cargo fromcargo = new Cargo( Cargo.Type.STRING, this.shipFrom.getString("cargo") );
		Cargo tocargo = new Cargo( Cargo.Type.STRING, this.shipTo.getString("cargo") );

		SQLResultRow shipTypeFrom = Ships.getShipType( this.shipFrom );
		SQLResultRow shipTypeTo = Ships.getShipType( this.shipTo );

		t.set_var(	"fromship.name",	this.shipFrom.getString("name"),
					"fromship.id",		this.shipFrom.getInt("id"),
					"fromship.cargo",	shipTypeFrom.getLong("cargo")-fromcargo.getMass(),
					"toship.name",		this.shipTo.getString("name"),
					"toship.id",		this.shipTo.getInt("id"),
					"toship.cargo",		shipTypeTo.getInt("cargo")-tocargo.getMass() );
		
		t.set_block("_PLUENDERN", "res.listitem", "res.list");
					
		ResourceList reslist = fromcargo.compare( tocargo, true );
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.id",		res.getId(),
						"res.name",		res.getName(),
						"res.image",	res.getImage(),
						"res.cargo1",	res.getCargo1(),
						"res.cargo2",	res.getCargo2() );
			
			t.parse("res.list", "res.listitem", true);
		}
	}
}
