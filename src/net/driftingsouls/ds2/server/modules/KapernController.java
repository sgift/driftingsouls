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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.ShipClasses;

/**
 * Ermoeglicht das Kapern eines Schiffes sowie verlinkt auf das Pluendern des Schiffes
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des Schiffes, mit dem der Spieler kapern moechte
 * @urlparam Integer tar Die ID des zu kapernden/pluendernden Schiffes
 */
public class KapernController extends DSGenerator {
	private SQLResultRow ownShip;
	private SQLResultRow targetShip;
	private SQLResultRow targetShipType;
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public KapernController(Context context) {
		super(context);
		
		setTemplate("kapern.html");
		
		parameterNumber("ship");
		parameterNumber("tar");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		Database db = getDatabase();
		User user = this.getUser();
		
		int ship = getInteger("ship");
		if( ship < 0 ) {
			ship = 0;
		}
		int tar = getInteger("tar");
		if( tar < 0 ) {
			tar = 0;	
		}

		SQLResultRow datas = db.first("SELECT id,name,x,y,system,e,type,engine,weapons,hull,battle,crew FROM ships WHERE id=",ship," AND owner=",user.getID());
		SQLResultRow datan = db.first("SELECT * FROM ships WHERE id=",tar);

		if( datas.isEmpty() ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl(getContext(), "default", "module", "schiffe") );
			
			return false;
		}
		
		String errorurl = Common.buildUrl(getContext(), "default", "module", "schiff", "ship", ship);
		
		if( datan.isEmpty() ) {
			addError("Das angegebene Zielschiff existiert nicht", errorurl );
			
			return false;
		}

		if( user.isNoob() ) {
			addError("Sie k&ouml;nnen weder kapern noch pl&uuml;ndern solange sie unter GCP-Schutz stehen<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden", errorurl );
			
			return false;
		}
		
		User taruser = getContext().createUserObject( datan.getInt("owner") );
		if( taruser.isNoob() ) {
			addError("Der Kolonist steht unter GCP-Schutz", errorurl );
			
			return false;
		}
		
		if( (taruser.getVacationCount() != 0) && (taruser.getWait4VacationCount() == 0) ) {
			addError("Sie k&ouml;nnen Schiffe dieses Spielers nicht kapern oder pl&uuml;ndern solange er sich im Vacation-Modus befindet", errorurl);
					
			return false;
		}
		
		if( datan.getInt("owner") == user.getID() ) {
			addError("Sie k&ouml;nnen ihre eigenen Schiffe nicht kapern", errorurl);
					
			return false;
		}
		
		if( (datan.getInt("visibility") != 0) && (datan.getInt("visibility") != user.getID()) ) {
			addError("Sie k&ouml;nnen nur kapern, was sie auch sehen", errorurl);
					
			return false;
		}

		if( !Location.fromResult(datas).sameSector( 0, Location.fromResult(datan), 0) ) {
			addError("Das Zielschiff befindet sich nicht im selben Sektor", errorurl);
					
			return false;
		}

		if( (datas.getInt("engine") == 0) || (datas.getInt("weapons") == 0) ) {
			addError("Diese Schrottm&uuml;hle wird nichts kapern k&ouml;nnen", errorurl);
					
			return false;
		}

		if( datas.getInt("crew") <= 0 ) {
			addError("Sie ben&ouml;tigen Crew um zu kapern", errorurl);
					
			return false;
		}
		
		SQLResultRow tard = ShipTypes.getShipType( datan );

		if( (tard.getInt("cost") != 0) && (datan.getInt("engine") != 0) && (datan.getInt("crew") != 0) ) {
			addError("Das feindliche Schiff ist noch bewegungsf&auml;hig", errorurl);
					
			return false;
		}

		// Wenn das Ziel ein Geschtz (10) ist....
		if( tard.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) {
			addError("Sie k&ouml;nnen orbitale Verteidigungsanlagen weder kapern noch pl&uuml;ndern", errorurl);
					
			return false;
		}
		
		if( datan.getString("docked").length() > 0 ) {
			if( datan.getString("docked").charAt(0) == 'l' ) {
				addError("Sie k&ouml;nnen gelandete Schiffe weder kapern noch pl&uuml;ndern", errorurl);
					
				return false;
			} 

			SQLResultRow mastership = db.first("SELECT * FROM ships WHERE id=",datan.getString("docked"));
			if( (mastership.getInt("engine") != 0) && (mastership.getInt("crew") != 0) ) {
				addError("Das Schiff, an das das feindliche Schiff angedockt hat, ist noch bewegungsf&auml;hig", errorurl);
				
				return false;
			}
		}
		
		//In einem Kampf?
		if( (datan.getInt("battle") != 0) || (datas.getInt("battle") != 0) ) {
			addError("Eines der Schiffe ist zur Zeit in einen Kampf verwickelt", errorurl);
					
			return false;
		}
		
		boolean disableIFF = datan.getString("status").indexOf("disable_iff") > -1;

		if( disableIFF ) {
			addError("Das Schiff besitzt keine IFF-Kennung und kann daher nicht gekapert/gepl&uuml;ndert werden", errorurl);
					
			return false;
		}
		
		this.ownShip = datas;
		this.targetShip = datan;
		this.targetShipType = tard;
		
		this.getTemplateEngine().set_var(
				"ownship.id",		datas.getInt("id"),
				"ownship.name",		datas.getString("name"),
				"targetship.id",	datan.getInt("id"),
				"targetship.name",	datan.getString("name") );

		return true;
	}

	/**
	 * Kapert das Schiff
	 *
	 */
	public void erobernAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = this.getUser();
		
		t.set_var("kapern.showkaperreport", 1);
		
		String errorurl = Common.buildUrl(getContext(), "default", "module", "schiff", "ship", this.ownShip.getInt("id"));
		
		if( (this.targetShip.getInt("system") == 1) && (this.targetShip.getInt("crew") > 0) ) {
			addError("Sie k&ouml;nnen keine Schiffe im System "+Systems.get().system(1).getName()+" (1) st&uuml;rmen", errorurl);
			this.setTemplate("");
					
			return;
		}
	
		if( ShipTypes.hasShipTypeFlag(this.targetShipType, ShipTypes.SF_NICHT_KAPERBAR ) ) {
			addError("Sie k&ouml;nnen dieses Schiff nicht kapern", errorurl);
			this.setTemplate("");
					
			return;
		}
		
		boolean flagschiffspace = user.hasFlagschiffSpace();
		
		User targetUser = getContext().createUserObject(this.targetShip.getInt("owner"));
		UserFlagschiffLocation flagschiffstatus = targetUser.getFlagschiff();

		if( !flagschiffspace && (flagschiffstatus != null) && 
			(flagschiffstatus.getID() == this.targetShip.getInt("id")) ) {
			addError("Sie d&uuml;rfen lediglich ein Flagschiff besitzen");
			t.set_var("kapern.showkaperreport", 0);
			
			this.redirect();
			return;
		} 
		
		int acrew = this.ownShip.getInt("crew");
		int dcrew = this.targetShip.getInt("crew");

		SQLResultRow ownShipType = ShipTypes.getShipType( this.ownShip );

		String kapermessage = "<div align=\"center\">Die Crew st&uuml;rmt die "+this.targetShip.getString("name")+"</div><br />";
		StringBuilder msg = new StringBuilder();
		
		boolean ok = false;
		
		// Falls Crew auf dem Zielschiff vorhanden ist
		if( this.targetShip.getInt("crew") != 0 ) {
			if( this.targetShipType.getInt("crew") == 0 ) {
				addError("Dieses Schiff ist nicht kaperbar", errorurl);
				this.setTemplate("");
						
				return;
			}	
			
			if( this.targetShipType.getInt("class") == ShipClasses.STATION.ordinal() ) {
				List<Integer> ownerlist = new ArrayList<Integer>();
				if( targetUser.getAlly() > 0 ) {
					UserIterator iter = getContext().createUserIterator("SELECT * FROM users WHERE ally=",targetUser.getAlly());
					for( User uid : iter ) {
						ownerlist.add(uid.getID());
					}
					iter.free();
				}		
				else {
					ownerlist.add(targetUser.getID());
				}
				
				int shipcount = 0;
				SQLQuery aship = db.query("SELECT t1.id,t1.status,t1.type ", 
								"FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id ", 
							 	"WHERE t1.x="+this.targetShip.getInt("x")+" AND t1.y="+this.targetShip.getInt("y")+" AND t1.system="+this.targetShip.getInt("system")+" AND ", 
							 		"t1.owner IN ("+Common.implode(",",ownerlist)+") AND t1.id>0 AND t1.battle=0 AND (LOCATE('=',t2.weapons) OR LOCATE('tblmodules',t1.status)) AND  ",
									"!LOCATE('nocrew',t1.status) AND t1.type=t2.id");
								
				while( aship.next() ) {
					SQLResultRow ashiptype = ShipTypes.getShipType(aship.getRow());
					if( ashiptype.getInt("military") > 0 ) {
						shipcount++;	
					}
				}
				aship.free();
				
				if( shipcount > 0 ) {
					double ws = -Math.pow(0.7,shipcount/3d)+1;
					ws *= 100;
					
					boolean found = false;
					for( int i=1; i <= shipcount; i++ ) {
						if( RandomUtils.nextInt(101) > ws ) {
							continue;
						}
						found = true;
						break;
					}	
					if( found ) {
						PM.send( getContext(), -1, this.targetShip.getInt("owner"), "Kaperversuch entdeckt", "Ihre Schiffe haben einen Kaperversuch bei "+this.targetShip.getInt("system")+":"+this.targetShip.getInt("x")+"/"+this.targetShip.getInt("y")+" vereitelt und den Gegner angegriffen" );
						
						Battle battle = new Battle();
						battle.create(user.getID(), this.ownShip.getInt("id"), this.targetShip.getInt("id"));
												
						t.set_var(
							"kapern.message",	"Ihr Kaperversuch wurde entdeckt und einige gegnerischen Schiffe haben das Feuer er&ouml;ffnet",
							"kapern.battle",	battle.getID() );
			
						return;
					}
				}
			}
			
			msg.append("Die Crew der "+this.ownShip.getString("name")+" ("+this.ownShip.getInt("id")+"), eine "+ownShipType.getString("nickname")+", st&uuml;rmt die "+this.targetShip.getString("name")+" ("+this.targetShip.getInt("id")+"), eine "+this.targetShipType.getString("nickname")+", bei "+this.targetShip.getInt("system")+":"+this.targetShip.getInt("x")+"/"+this.targetShip.getInt("y")+"+\n\n");
	
			int defmulti = 1;
			
			Offizier offizier = Offizier.getOffizierByDest('s', this.targetShip.getInt("id"));
			if( offizier != null ) {
				defmulti = (int)Math.round(offizier.getAbility(Offizier.Ability.SEC)/25d)+1;
			}

			if( acrew >= dcrew*3*defmulti ) {
				ok = true;
				
				t.set_var("kapern.message", kapermessage+"Die Crew gibt das Schiff kampflos auf und l&auml;uft &uuml;ber" );
				msg.append("Ihre Crew gibt das Schiff kampflos auf l&auml;uft &uuml;ber.\n");
			}
			else {
				//$dcrew = round(($dcrew*$defmulti - $acrew)/$defmulti);
				
				if( Math.round((dcrew*defmulti - acrew)/defmulti) > 0 ) {
					int oldacrew = acrew;
					int olddcrew = dcrew;
					acrew  = (int)Math.round(acrew * 0.1);
					if( acrew < 1 ) {
						acrew = 1;
					}

					dcrew = Math.round((dcrew*defmulti - oldacrew+acrew)/defmulti);
					
					t.set_var("kapern.message", kapermessage+"Der Feind verteidigt das Schiff:<br />"+(oldacrew-acrew)+" Crewmitglieder fallen.<br />"+(olddcrew-dcrew)+" Feinde get&ouml;tet.<br /><br /><span style=\"color:red\">Angriff abgebrochen</span>" );
					msg.append("Ihre Crew verteidigt das Schiff:\n"+(oldacrew-acrew)+" Feinde werden erschossen.\n"+(olddcrew-dcrew)+" Besatzungsmitglieder haben ihr leben gelassen.\n\n[color=green]Der Feind flieht[/color]\n");
					
					ok = false;
				} 
				else {
					t.set_var("kapern.message", kapermessage+"Der Feind verteidigt das Schiff:<br />"+Math.round(dcrew*defmulti)+" Crewmitglieder fallen.<br />"+dcrew+" Feinde get&ouml;tet" );
					msg.append("Ihre Crew verteidigt das Schiff:\n"+Math.round(dcrew*defmulti)+" Feinde werden erschossen.\n"+dcrew+" Besatzungsmitglieder haben ihr leben gelassen.\n\n[color=red]Die "+this.targetShip.getString("name")+" ist verloren[/color]\n");
					
					acrew -= Math.round(dcrew*defmulti);
					dcrew = 0;
					ok = true;
				}
			}
		}
		// Falls keine Crew auf dem Zielschiff vorhanden ist
		else {
			ok = true;
			
			t.set_var("kapern.message", kapermessage+"Das Schiff wird widerstandslos &uuml;bernommen");
			
			msg.append("Das Schiff "+this.targetShip.getString("name")+"("+this.targetShip.getInt("id")+"), eine "+this.targetShipType.getString("nickname")+", wird bei "+this.targetShip.getInt("system")+":"+this.targetShip.getInt("x")+"/"+this.targetShip.getInt("y")+" an "+this.ownShip.getString("name")+" ("+this.ownShip.getInt("id")+") &uuml;bergeben\n");
		}
		
		db.tBegin();
		
		// Transmisson
		PM.send( getContext(), user.getID(), this.targetShip.getInt("owner"), "Kaperversuch", msg.toString() );
		
		db.update("UPDATE ships SET crew=",acrew," WHERE id=",this.ownShip.getInt("id"));
		db.update("UPDATE ships SET crew=",dcrew," WHERE id=",this.targetShip.getInt("id"));
		
		// Wurde das Schiff gekapert?
		if( ok ) {
			// Evt unbekannte Items bekannt machen
			Cargo cargo = new Cargo( Cargo.Type.STRING, this.targetShip.getString("cargo") );

			List<ItemCargoEntry> itemlist = cargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry item = itemlist.get(i);
				Item itemobject = item.getItemObject();
				if( itemobject.isUnknownItem() ) {
					user.addKnownItem(item.getItemID());
				}
			}
			
			String currentTime = Common.getIngameTime(getContext().get(ContextCommon.class).getTick());
			
			// Schiff uebereignen
			this.targetShip.put("history", this.targetShip.getString("history")+"Gekapert am "+currentTime+" durch "+user.getName()+" ("+user.getID()+")\n");
			
			db.prepare("UPDATE ships SET owner= ?,fleet= ?,history= ? WHERE id= ? OR docked IN ( ?, ?)")
				.update(user.getID(), 0, this.targetShip.getString("history"), this.targetShip.getInt("id"), "l "+this.targetShip.getInt("id"), Integer.toString(this.targetShip.getInt("id")));
			db.update("UPDATE offiziere SET userid="+user.getID()+" WHERE dest='s "+this.targetShip.getInt("id")+"'");
			
			if( this.targetShipType.getString("werft").length() > 0 ) {
				db.update("UPDATE werften SET linked=0 WHERE shipid="+this.targetShip.getInt("id"));
			}
			
			// Flagschiffeintraege aktuallisieren
			if( (flagschiffstatus != null) && 
				(flagschiffstatus.getType() == UserFlagschiffLocation.Type.SHIP) && 
				(this.targetShip.getInt("id") == flagschiffstatus.getID()) ) {
				targetUser.setFlagschiff(0);
				user.setFlagschiff(this.targetShip.getInt("id"));
			}
		}	
		Ships.recalculateShipStatus(this.ownShip.getInt("id"));
		Ships.recalculateShipStatus(this.targetShip.getInt("id"));
	
		Common.dblog("kapern", Integer.toString(this.ownShip.getInt("id")), Integer.toString(this.targetShip.getInt("id")), user.getID(),
				"owner",	Integer.toString(this.targetShip.getInt("owner")),
				"pos",		Location.fromResult(this.targetShip).toString(),
				"shiptype",	Integer.toString(this.targetShip.getInt("type")) );
		
		db.tCommit();
	}

	/**
	 * Zeigt die Auswahl ab, ob das Schiff gekapert oder gepluendert werden soll
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.set_var("kapern.showmenu", 1);
		
		if( (this.targetShipType.getInt("cost") != 0) && (this.targetShip.getInt("engine") != 0) ) {
			if( this.targetShip.getInt("crew") == 0 ) {
				t.set_var(	"targetship.status",	"verlassen",
							"menu.showpluendern",	1,
							"menu.showkapern",		!ShipTypes.hasShipTypeFlag(this.targetShipType, ShipTypes.SF_NICHT_KAPERBAR ) );
			} 
			else {
				t.set_var("targetship.status", "noch bewegungsf&auml;hig");
			}
		} 
		else {
			t.set_var(	"targetship.status",	"bewegungsunf&auml;hig",
						"menu.showpluendern",	(this.targetShip.getInt("crew") == 0),
						"menu.showkapern",		!ShipTypes.hasShipTypeFlag(this.targetShipType, ShipTypes.SF_NICHT_KAPERBAR) );
		}
	}
}
