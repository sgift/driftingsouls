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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Pluendert ein Schiff
 * @author Christopher Jung
 *
 * @urlparam Integer from Die ID des Schiffes, mit dem ein anderes Schiff gepluendert werden soll
 * @urlparam Integer to Die ID des zu pluendernden Schiffes
 */
public class PluendernController extends TemplateGenerator {
	private Ship shipFrom;
	private Ship shipTo;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public PluendernController(Context context) {
		super(context);
		
		setTemplate("pluendern.html");
		
		parameterNumber("from");
		parameterNumber("to");
		
		setPageTitle("Pluendern");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		User user = (User)this.getUser();
		
		int from = getInteger("from");
		int to = getInteger("to");
		
		Ship shipFrom = (Ship)db.get(Ship.class, from);
		if( (shipFrom == null) || (shipFrom.getId() < 0) || (shipFrom.getOwner() != user) ) {
			addError("Sie brauchen ein Schiff um zu pl&uuml;ndern", Common.buildUrl("default", "module", "schiff", "ship", from));
					
			return false;	
		}

		final String errorurl = Common.buildUrl("default", "module", "schiff", "ship", from);
		
		Ship shipTo = (Ship)db.get(Ship.class, to);
		if( (shipTo == null) || (shipTo.getId() < 0) ) {
			addError("Das angegebene Zielschiff existiert nicht", errorurl);
					
			return false;
		}
		ShipTypeData shipTypeTo = shipTo.getTypeData();
	
		if( user.isNoob() ) {
			addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher nicht pl&uuml;nndern<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden", errorurl);
					
			return false;
		}
	
		if( to == from ) {
			addError("Sie k&ouml;nnen nicht sich selbst pl&uuml;ndern", errorurl);
					
			return false;
		}
		
		User taruser = shipTo.getOwner();
		
		if( taruser.isNoob() ) {
			addError("Dieser Kolonist steht unter GCP-Schutz", errorurl);
					
			return false;
		}
		
		if( (taruser.getVacationCount() != 0) && (taruser.getWait4VacationCount() == 0) ) {
			addError("Sie k&ouml;nnen Schiffe dieses Spielers nicht kapern oder pl&uuml;ndern solange er sich im Vacation-Modus befindet", errorurl);
					
			return false;
		}
		
		if( (shipTo.getVisibility() != null) && (shipTo.getVisibility() != user.getId()) ) {
			addError("Sie k&ouml;nnen nur pl&uuml;ndern, was sie auch sehen", errorurl);
					
			return false;
		}

		if( !shipFrom.getLocation().sameSector( 0, shipTo.getLocation(), 0) ) {
			addError("Das zu pl&uuml;ndernde Schiff befindet sich nicht im selben Sektor", errorurl);
					
			return false;
		}

		if( (shipFrom.getBattle() != null) || (shipTo.getBattle() != null) ) {
			addError("Eines der Schiffe ist in einen Kampf verwickelt", errorurl);
					
			return false;
		}

		if( shipTypeTo.getShipClass() == ShipClasses.GESCHUETZ.ordinal() ) {
			addError("Sie k&ouml;nnen autonome orbitale Verteidigungsanlagen weder kapern noch pl&uuml;ndern", errorurl);
					
			return false;
		}

		// IFF-Check
		boolean disableIFF = shipTo.getStatus().indexOf("disable_iff") > -1;
		if( disableIFF ) {
			addError("Das Schiff kann nicht gepl&uuml;ndert werden", errorurl);
					
			return false;
		}

		if( shipTo.getDocked().length() > 0 ) {
			if( shipTo.getDocked().charAt(0) == 'l' ) {
				addError("Sie k&ouml;nnen gelandete Schiffe weder kapern noch pl&uuml;ndern", errorurl);
					
				return false;
			} 

			Ship mastership = (Ship)db.get(Ship.class, Integer.valueOf(shipTo.getDocked()));
			if( ( (mastership.getCrew() != 0) ) && (mastership.getEngine() != 0) && 
				(mastership.getWeapons() != 0) ) {
				addError("Das Schiff, an das das feindliche Schiff angedockt hat, ist noch bewegungsf&auml;hig", errorurl);
					
				return false;
			}
		}

		if( (shipTo.getCrew() != 0) && (shipTypeTo.getCost() != 0) && (shipTo.getEngine() != 0) ) {
			addError("Feindliches Schiff nicht bewegungsunf&auml;hig", errorurl);
					
			return false;
		}

		if( shipTypeTo.hasFlag(ShipTypes.SF_KEIN_TRANSFER) ) {
			addError("Sie k&ouml;nnen keine Waren zu oder von diesem Schiff transferieren", errorurl);
					
			return false;
		}
		
		if( shipTypeTo.hasFlag(ShipTypes.SF_NICHT_PLUENDERBAR) ) {
			addError("Sie k&ouml;nnen keine Waren von diesem Schiff pl&uuml;ndern", errorurl);
					
			return false;
		}

		if( (shipTypeTo.getShipClass() == ShipClasses.STATION.ordinal() ) && (shipTo.getCrew() != 0) ) {
			addError("Solange die Crew &uuml;ber die Waren wacht werden sie hier nichts klauen k&ouml;nnen", errorurl);
					
			return false;
		}
		
		ShipTypeData shipTypeFrom = shipFrom.getTypeData();		
		if( shipTypeFrom.hasFlag(ShipTypes.SF_KEIN_TRANSFER) ) {
			addError("Sie k&ouml;nnen keine Waren zu oder von ihrem Schiff transferieren", errorurl);
					
			return false;
		}
		
		this.shipFrom = shipFrom;
		this.shipTo = shipTo;
		
		this.getTemplateEngine().setVar( 
				"fromship.name",	this.shipFrom.getName(),
				"fromship.id",		this.shipFrom.getId(),
				"toship.name",		this.shipTo.getName(),
				"toship.id",		this.shipTo.getId() );
													
		return true;
	}

	/**
	 * Transferiert Waren zwischen den Schiffen
	 * @urlparam Integer $resID+"to" Die Menge der Ware $resID, welche zum Zielschiff transferiert werden soll
	 * @urlparam Integer $resID+"from" Die Menge der Ware $resID, welche vom Zielschiff herunter transferiert werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void transferAction() {
		TemplateEngine t = this.getTemplateEngine();
		User user = (User)this.getUser();
		
		Cargo cargofrom = this.shipFrom.getCargo();
		Cargo cargoto = this.shipTo.getCargo();
		
		ShipTypeData shipTypeTo = this.shipTo.getTypeData();
		ShipTypeData shipTypeFrom = this.shipFrom.getTypeData();
	
		long curcargoto = shipTypeTo.getCargo() - cargoto.getMass();
		long curcargofrom = shipTypeFrom.getCargo() - cargofrom.getMass();

		StringBuilder msg = new StringBuilder();
		Cargo newCargoTo = (Cargo)cargoto.clone();
		Cargo newCargoFrom = (Cargo)cargofrom.clone();
	
		long totaltransferfcount = 0;
		boolean transfer = false;
		
		t.setBlock("_PLUENDERN", "transfer.listitem", "transfer.list" );

		ResourceList reslist = cargofrom.compare( cargoto, true );
		for( ResourceEntry res : reslist ) {
			this.parameterNumber(res.getId()+"to");
			this.parameterNumber(res.getId()+"from");
			
			long transt = this.getInteger(res.getId()+"to");
		 	long transf = this.getInteger(res.getId()+"from");

			t.start_record();

			t.setVar( "res.image", res.getImage() );

			// Transfer vom Ausgangsschiff zum Zielschiff
			if( transt > 0 ) {				
				t.setVar(	"transfer.target",	this.shipTo.getName(),
							"transfer.count",	transt );
				
				if( transt > res.getCount1() ) {
					transt = res.getCount1();
					
					t.setVar(	"transfer.notenoughcargo",	1,
								"transfer.cargo",			res.getCount1() );
				} 

				if( curcargoto - transt < 0 ) {
					transt = (long)( curcargoto/(double)Cargo.getResourceMass( res.getId(), 1 ) );
					
					t.setVar(	"transfer.notenoughspace",	1,
								"transfer.newcount",		transt );
				}
				
				// Falls es sich um ein unbekanntes Item handelt, dann dem Besitzer des Zielschiffes bekannt machen
				if( (transt > 0) && res.getId().isItem() ) {
					int itemid = res.getId().getItemID();
					if( Items.get().item(itemid).isUnknownItem() ) {
						User targetUser = this.shipTo.getOwner();
						targetUser.addKnownItem(itemid);
					}
				}
				
				newCargoTo.addResource( res.getId(), transt );
				newCargoFrom.substractResource( res.getId(), transt );
				curcargoto = shipTypeTo.getCargo() - newCargoTo.getMass();
				curcargofrom = shipTypeFrom.getCargo() - newCargoFrom.getMass();
				
				t.setVar( "transfer.totalcargo", newCargoTo.getResourceCount( res.getId() ) );
				
				if( transt > 0 ) {
					transfer = true;
					msg.append("[resource="+res.getId()+")"+transt+"[/resource] zur&uuml;ckgegeben.\n");
				}
				
				t.parse("transfer.list", "transfer.listitem", true);
			}
			// Transfer vom Zielschiff zum Ausgangsschiff
			else if( transf > 0 ) {				
				t.setVar(	"transfer.target",	this.shipFrom.getName(),
							"transfer.count",	transf );
				
				if( transf > res.getCount2() ) {
					transf = res.getCount2();
					
					t.setVar(	"transfer.notenoughcargo",	1,
								"transfer.cargo",			res.getCount2() );
				} 
				
				if( curcargofrom - transf < 0 ) {
					transf = (long)( curcargofrom/(double)Cargo.getResourceMass( res.getId(), 1 ) );
					
					t.setVar(	"transfer.notenoughspace",	1,
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
				
				curcargoto = shipTypeTo.getCargo() - newCargoTo.getMass();
				curcargofrom = shipTypeFrom.getCargo() - newCargoFrom.getMass();
				
				newCargoFrom.addResource( res.getId(), transf );
				newCargoTo.substractResource( res.getId(), transf );
				
				t.setVar( "transfer.totalcargo", newCargoFrom.getResourceCount( res.getId() ) );
				
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
			msg.insert(0, this.shipTo.getName()+" ("+this.shipTo.getId()+") wird von "+
					this.shipFrom.getName()+" ("+this.shipFrom.getId()+") bei "+
					this.shipFrom.getLocation()+" gepl&uuml;ndert.\n");
			
			PM.send(this.shipFrom.getOwner(), this.shipTo.getOwner().getId(), "Schiff gepl&uuml;ndert", msg.toString());
		}
		
		// Schiffe aktuallisieren
		if( transfer ) {
			this.shipFrom.setCargo(newCargoFrom);
			this.shipTo.setCargo(newCargoTo);
	
			String status = this.shipTo.recalculateShipStatus();
			this.shipFrom.recalculateShipStatus();
			
			// Falls das Schiff instabil ist, dann diesem den "destory"-Status geben,
			// damit der Schiffstick dieses zerstoert
			if( (totaltransferfcount > 0) && shipTypeTo.hasFlag(ShipTypes.SF_INSTABIL)  ) {
				t.setVar("toship.isinstabil", 1);
			
				String statust = status;
				
				if( statust.length() > 0 ) {
					statust += " destroy";
				}	
				else {
					statust += "destroy";
				}	
			
				this.shipTo.setStatus(statust);
			}
		}
		
		this.redirect();
	}
	
	/**
	 * Zeigt die GUI fuer den Warentransfer an
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = this.getTemplateEngine();
		
		Cargo fromcargo = this.shipFrom.getCargo();
		Cargo tocargo = this.shipTo.getCargo();

		ShipTypeData shipTypeFrom = this.shipFrom.getTypeData();
		ShipTypeData shipTypeTo = this.shipTo.getTypeData();

		t.setVar(	"fromship.name",	this.shipFrom.getName(),
					"fromship.id",		this.shipFrom.getId(),
					"fromship.cargo",	shipTypeFrom.getCargo()-fromcargo.getMass(),
					"toship.name",		this.shipTo.getName(),
					"toship.id",		this.shipTo.getId(),
					"toship.cargo",		shipTypeTo.getCargo()-tocargo.getMass() );
		
		t.setBlock("_PLUENDERN", "res.listitem", "res.list");
					
		ResourceList reslist = fromcargo.compare( tocargo, true );
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.id",		res.getId(),
						"res.name",		res.getName(),
						"res.image",	res.getImage(),
						"res.cargo1",	res.getCargo1(),
						"res.cargo2",	res.getCargo2() );
			
			t.parse("res.list", "res.listitem", true);
		}
	}
}
