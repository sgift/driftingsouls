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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.ComNetEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo.Crew;
import net.driftingsouls.ds2.server.units.UnitType;
import net.driftingsouls.ds2.server.werften.ShipWerft;

import org.apache.commons.lang.math.RandomUtils;

/**
 * Ermoeglicht das Kapern eines Schiffes sowie verlinkt auf das Pluendern des Schiffes.
 * @author Christopher Jung
 * 
 * @urlparam Integer ship Die ID des Schiffes, mit dem der Spieler kapern moechte
 * @urlparam Integer tar Die ID des zu kapernden/pluendernden Schiffes
 */
public class KapernController extends TemplateGenerator {
	private Ship ownShip;
	private Ship targetShip;
	
	/**
	 * Konstruktor.
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
		org.hibernate.Session db = ContextMap.getContext().getDB();
		User user = (User)this.getUser();
		
		int ship = getInteger("ship");
		if( ship < 0 ) {
			ship = 0;
		}
		int tar = getInteger("tar");
		if( tar < 0 ) {
			tar = 0;	
		}

		Ship aship = (Ship)db.get(Ship.class, ship);
		Ship dship = (Ship)db.get(Ship.class, tar);
		
		if( aship == null || aship.getOwner().getId() != user.getId() ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl("default", "module", "schiffe") );
			
			return false;
		}
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship);
		
		if( dship == null ) {
			addError("Das angegebene Zielschiff existiert nicht", errorurl );
			
			return false;
		}

		if( user.isNoob() ) {
			addError("Sie k&ouml;nnen weder kapern noch pl&uuml;ndern solange sie unter GCP-Schutz stehen<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden", errorurl );
			
			return false;
		}
		
		User taruser = dship.getOwner();
		if( taruser.isNoob() ) {
			addError("Der Kolonist steht unter GCP-Schutz", errorurl );
			
			return false;
		}
		
		if( (taruser.getVacationCount() != 0) && (taruser.getWait4VacationCount() == 0) ) {
			addError("Sie k&ouml;nnen Schiffe dieses Spielers nicht kapern oder pl&uuml;ndern solange er sich im Vacation-Modus befindet", errorurl);
					
			return false;
		}
		
		if( dship.getOwner().getId() == aship.getOwner().getId() ) {
			addError("Sie k&ouml;nnen ihre eigenen Schiffe nicht kapern", errorurl);
					
			return false;
		}
		
		if( (dship.getVisibility() != null) && (dship.getVisibility() != user.getId()) ) {
			addError("Sie k&ouml;nnen nur kapern, was sie auch sehen", errorurl);
					
			return false;
		}

		if( !aship.getLocation().sameSector(0, dship.getLocation(), 0) ) {
			addError("Das Zielschiff befindet sich nicht im selben Sektor", errorurl);
					
			return false;
		}

		if( (aship.getEngine() == 0) || (aship.getWeapons() == 0) ) {
			addError("Diese Schrottm&uuml;hle wird nichts kapern k&ouml;nnen", errorurl);
					
			return false;
		}

		if( aship.getUnits().isEmpty() ) {
			addError("Sie ben&ouml;tigen Einheiten um zu kapern", errorurl);
					
			return false;
		}
		
		if( (dship.getTypeData().getCost() != 0) && (dship.getEngine() != 0) && (dship.getCrew() != 0 || !dship.getUnits().isEmpty()) ) {
			addError("Das feindliche Schiff ist noch bewegungsf&auml;hig", errorurl);
					
			return false;
		}

		// Wenn das Ziel ein Geschtz (10) ist....
		if( dship.getTypeData().getShipClass() == ShipClasses.GESCHUETZ.ordinal() ) {
			addError("Sie k&ouml;nnen orbitale Verteidigungsanlagen weder kapern noch pl&uuml;ndern", errorurl);
					
			return false;
		}
		
		if( dship.isDocked() || dship.isLanded() ) {
			if( dship.isLanded() ) {
				addError("Sie k&ouml;nnen gelandete Schiffe weder kapern noch pl&uuml;ndern", errorurl);
					
				return false;
			} 

			Ship mship = dship.getBaseShip();
			if( (mship.getEngine() != 0) && (mship.getCrew() != 0 || !mship.getUnits().isEmpty()) ) {
				addError("Das Schiff, an das das feindliche Schiff angedockt hat, ist noch bewegungsf&auml;hig", errorurl);
				
				return false;
			}
		}
		
		//In einem Kampf?
		if( (aship.getBattle() != null) || (dship.getBattle() != null) ) {
			addError("Eines der Schiffe ist zur Zeit in einen Kampf verwickelt", errorurl);
					
			return false;
		}
		
		if( dship.getStatus().indexOf("disable_iff") > -1) {
			addError("Das Schiff besitzt keine IFF-Kennung und kann daher nicht gekapert/gepl&uuml;ndert werden", errorurl);
					
			return false;
		}
		
		this.ownShip = aship;
		this.targetShip = dship;
		
		this.getTemplateEngine().setVar(
				"ownship.id",		aship.getId(),
				"ownship.name",		aship.getName(),
				"targetship.id",	dship.getId(),
				"targetship.name",	dship.getName() );

		return true;
	}

	/**
	 * Kapert das Schiff.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void erobernAction() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)this.getUser();
		
		t.setVar("kapern.showkaperreport", 1);
		
		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", this.ownShip.getId());
	
		if( targetShip.getTypeData().hasFlag(ShipTypes.SF_NICHT_KAPERBAR ) ) {
			addError("Sie k&ouml;nnen dieses Schiff nicht kapern", errorurl);
			this.setTemplate("");
					
			return;
		}
		
		User targetUser = (User)getDB().get(User.class, this.targetShip.getOwner().getId());
				
		String kapermessage = "<div align=\"center\">Die Einheiten st&uuml;rmen die "+this.targetShip.getName()+"</div><br />";
		StringBuilder msg = new StringBuilder();
		
		boolean ok = false;

        //Trying to steal a ship already costs bounty or one could help another player to get a ship without any penalty
        if(!targetUser.hasFlag(User.FLAG_NO_AUTO_BOUNTY))
        {
            BigDecimal account = new BigDecimal(targetUser.getKonto());
            account = account.movePointLeft(1).setScale(0, RoundingMode.HALF_EVEN);
            
            BigInteger shipBounty = this.targetShip.getTypeData().getBounty();
            shipBounty = account.toBigIntegerExact().min(shipBounty);
 
            if(shipBounty.compareTo(BigInteger.ZERO) != 0)
            {
                user.addBounty(shipBounty);
                //Make it public that there's a new bounty on a player, so others can go to hunt
                ConfigValue value = (ConfigValue)db.get(ConfigValue.class, "bountychannel");
                int bountyChannel = Integer.valueOf(value.getValue());
                ComNetChannel channel = (ComNetChannel)db.get(ComNetChannel.class, bountyChannel);
                ComNetEntry entry = new ComNetEntry(user, channel);
                entry.setHead("Kopfgeld");
                entry.setText("Gesuchter: " + user.getNickname() + "[BR]Wegen: Diebstahl von Schiffen[BR]Betrag: " + shipBounty);
                db.persist(entry);
            }
        }
		
		// Falls Crew auf dem Zielschiff vorhanden ist
		if( this.targetShip.getCrew() != 0 || !this.targetShip.getUnits().isEmpty() ) {
			if( this.targetShip.getTypeData().getCrew() == 0 ) {
				addError("Dieses Schiff ist nicht kaperbar", errorurl);
				this.setTemplate("");
						
				return;
			}	
			
			if( this.targetShip.getTypeData().getShipClass() == ShipClasses.STATION.ordinal() ) {
				List<Integer> ownerlist = new ArrayList<Integer>();
				if( targetUser.getAlly() != null ) {
					for( User uid : targetUser.getAlly().getMembers() ) {
						ownerlist.add(uid.getId());
					}
				}		
				else {
					ownerlist.add(targetUser.getId());
				}
				
				int shipcount = 0;
				List<Ship> shiplist = Common.cast(db.createQuery("SELECT t1.id,t1.status,t1.shiptype " + 
								"FROM Ship t1 left join fetch ShipType t2 " + 
							 	"WHERE t1.x="+this.targetShip.getX()+" AND t1.y="+this.targetShip.getY()+" AND t1.system="+this.targetShip.getSystem()+" AND " + 
							 		"t1.owner IN ("+Common.implode(",",ownerlist)+") AND t1.id>0 AND t1.battle is null AND  " +
									" t1.shiptype=t2.id").list());
				Iterator<Ship> iter = shiplist.iterator();			
				while( iter.hasNext() ) {
					Ship ship = iter.next();
					if( ship.getTypeData().isMilitary() && ship.getCrew() > 0 ) {
						shipcount++;	
					}
				}
				
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
						User source = (User)getDB().get(User.class, -1);
						PM.send( source, this.targetShip.getOwner().getId(), "Kaperversuch entdeckt", "Ihre Schiffe haben einen Kaperversuch bei "+this.targetShip.getLocation().displayCoordinates(false)+" vereitelt und den Gegner angegriffen" );
						
						Battle battle = Battle.create( this.targetShip.getOwner().getId(), this.targetShip.getId() , this.ownShip.getId(), true);
												
						t.setVar(
							"kapern.message",	"Ihr Kaperversuch wurde entdeckt und einige gegnerischen Schiffe haben das Feuer er&ouml;ffnet",
							"kapern.battle",	battle.getId() );
			
						return;
					}
				}
			}
			
			msg.append("Die Einheiten der "+this.ownShip.getName()+" ("+this.ownShip.getId()+"), eine "+this.ownShip.getTypeData().getNickname()+", st&uuml;rmt die "+this.targetShip.getName()+" ("+this.targetShip.getId()+"), eine "+this.targetShip.getTypeData().getNickname()+", bei "+this.targetShip.getLocation().displayCoordinates(false)+"\n\n");
	

			Crew dcrew = new UnitCargo.Crew(this.targetShip.getCrew());
			UnitCargo ownUnits = this.ownShip.getUnits();
			UnitCargo enemyUnits = this.targetShip.getUnits();
			
			UnitCargo saveunits = ownUnits.trimToMaxSize(targetShip.getTypeData().getMaxUnitSize());


			int attmulti = 1;
			int defmulti = 1;
			
			Offizier defoffizier = Offizier.getOffizierByDest('s', this.targetShip.getId());
			if( defoffizier != null ) {
				defmulti = defoffizier.getKaperMulti(true);
			}
			Offizier attoffizier = Offizier.getOffizierByDest('s', this.ownShip.getId());
			if( attoffizier != null)
			{
				attmulti = attoffizier.getKaperMulti(false);
			}

			if( !ownUnits.isEmpty() && !(enemyUnits.isEmpty() && this.targetShip.getCrew() == 0 ) ) {
				
				UnitCargo toteeigeneUnits = new UnitCargo();
				UnitCargo totefeindlicheUnits = new UnitCargo();
				
				if(ownUnits.kapern(enemyUnits, toteeigeneUnits, totefeindlicheUnits, dcrew, attmulti, defmulti ))
				{
					ok = true;
					if(toteeigeneUnits.isEmpty() && totefeindlicheUnits.isEmpty())
					{
						if( attoffizier != null)
						{
							attoffizier.gainExperience(Offizier.Ability.COM, 5);
						}
						msg.append("Das Schiff ist kampflos verloren.\n");
					}
					else
					{
						msg.append("Das Schiff ist verloren.\n");
						HashMap<Integer, Long> ownunitlist = toteeigeneUnits.getUnitList();
						HashMap<Integer, Long> enemyunitlist = totefeindlicheUnits.getUnitList();
						
						if(!ownunitlist.isEmpty())
						{
							for(Entry<Integer, Long> unit : ownunitlist.entrySet())
							{
								UnitType unittype = (UnitType)db.get(UnitType.class, unit.getKey());
								msg.append("Angreifer:\n"+unit.getValue()+" "+unittype.getName()+" erschossen\n");
							}
						}
						
						if(!enemyunitlist.isEmpty())
						{
							for(Entry<Integer, Long> unit : enemyunitlist.entrySet())
							{
								UnitType unittype = (UnitType)db.get(UnitType.class, unit.getKey());
								msg.append("Verteidiger:\n"+unit.getValue()+" "+unittype.getName()+" gefallen\n");
							}
						}
						
						if( attoffizier != null)
						{
							attoffizier.gainExperience(Offizier.Ability.COM, 3);
						}
					}
				}
				else
				{
					msg.append("Angreifer flieht.\n");
					HashMap<Integer, Long> ownunitlist = toteeigeneUnits.getUnitList();
					HashMap<Integer, Long> enemyunitlist = totefeindlicheUnits.getUnitList();
					
					if(!ownunitlist.isEmpty())
					{
						for(Entry<Integer, Long> unit : ownunitlist.entrySet())
						{
							UnitType unittype = (UnitType)db.get(UnitType.class, unit.getKey());
							msg.append("Angreifer:\n"+unit.getValue()+" "+unittype.getName()+" erschossen\n");
						}
					}
					
					if(!enemyunitlist.isEmpty())
					{
						for(Entry<Integer, Long> unit : enemyunitlist.entrySet())
						{
							UnitType unittype = (UnitType)db.get(UnitType.class, unit.getKey());
							msg.append("Verteidiger:\n"+unit.getValue()+" "+unittype.getName()+" gefallen\n");
						}
					}
					
					if( defoffizier != null)
					{
						defoffizier.gainExperience(Offizier.Ability.SEC, 5);
					}
				}
			} 
			else if( !ownUnits.isEmpty() ) {
				ok = true;
				if(attoffizier != null)
				{
					attoffizier.gainExperience(Offizier.Ability.COM, 5);
				}
				msg.append("Schiff wird widerstandslos &uuml;bernommen\n");
			}

			ownUnits.addCargo(saveunits);
			
			this.ownShip.setUnits(ownUnits);
				
			this.targetShip.setUnits(enemyUnits);
			this.targetShip.setCrew(dcrew.getValue());

			// Wurde das Schiff gekapert?
			if( ok ) {
				// Unbekannte Items bekannt machen
				Cargo cargo = this.targetShip.getCargo();

				List<ItemCargoEntry> itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry item = itemlist.get(i);

					Item itemobject = item.getItemObject();
					if( itemobject.isUnknownItem() ) {
						user.addKnownItem(item.getItemID());
					}
				}

				String currentTime = Common.getIngameTime(ContextMap.getContext().get(ContextCommon.class).getTick());

				this.targetShip.setHistory(this.targetShip.getHistory()+"Gekapert am "+currentTime+" durch "+user.getName()+" ("+user.getId()+")\n");

				this.targetShip.removeFromFleet();
				this.targetShip.setOwner(user);

				List<Integer> kaperlist = new ArrayList<Integer>();
				kaperlist.add(this.targetShip.getId());

				List<Ship> docked = Common.cast(db.createQuery("from Ship where id>0 and docked in (?,?)")
						.setString(0, Integer.toString(this.targetShip.getId()))
						.setString(1, "l "+this.targetShip.getId())
						.list());
				for( Ship dockShip : docked )
				{
					dockShip.removeFromFleet();
					dockShip.setOwner(user);
					
					db.createQuery("update Offizier set userid=? where dest=?")
					.setEntity(0, user)
					.setString(1, "s "+dockShip.getId())
					.executeUpdate();
					if( dockShip.getTypeData().getWerft() != 0 ) {
						ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=?")
						.setEntity(0, dockShip)
						.uniqueResult();

						if( werft.getKomplex() != null ) {
							werft.removeFromKomplex();
						}
						werft.setLink(null);
					}

				}

				db.createQuery("update Offizier set userid=? where dest=?")
				.setEntity(0, user)
				.setString(1, "s "+this.targetShip.getId())
				.executeUpdate();
				if( this.targetShip.getTypeData().getWerft() != 0 ) {
					ShipWerft werft = (ShipWerft)db.createQuery("from ShipWerft where ship=?")
					.setEntity(0, this.targetShip)
					.uniqueResult();

					if( werft.getKomplex() != null ) {
						werft.removeFromKomplex();
					}
					werft.setLink(null);
				}
			} 
		}
		// Falls keine Crew auf dem Zielschiff vorhanden ist
		else {
			ok = true;
			
			t.setVar("kapern.message", kapermessage+"Das Schiff wird widerstandslos &uuml;bernommen");
			
			msg.append("Das Schiff "+this.targetShip.getName()+"("+this.targetShip.getId()+"), eine "+this.targetShip.getTypeData().getNickname()+", wird bei "+this.targetShip.getLocation().displayCoordinates(false)+" an "+this.ownShip.getName()+" ("+this.ownShip.getId()+") &uuml;bergeben\n");
		}
		
		// Transmisson
		PM.send( user, targetUser.getId(), "Kaperversuch", msg.toString() );
		
		// Wurde das Schiff gekapert?
		if( ok ) {
			// Evt unbekannte Items bekannt machen
			Cargo cargo = new Cargo( this.targetShip.getCargo() );

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
			this.targetShip.setHistory(this.targetShip.getHistory()+"Gekapert am "+currentTime+" durch "+user.getName()+" ("+user.getId()+")\n");
			this.targetShip.setOwner(this.ownShip.getOwner());
			
			if( this.targetShip.getTypeData().getWerft() > 0 ) {
				db.createQuery("UPDATE WerftObject SET linked=null,linkedWerft=null WHERE shipid="+this.targetShip.getId());
			}
		}	

		this.ownShip.recalculateShipStatus();
		this.targetShip.recalculateShipStatus();
	}

	/**
	 * Zeigt die Auswahl ab, ob das Schiff gekapert oder gepluendert werden soll.
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar("kapern.showmenu", 1);
		
		if( (this.targetShip.getTypeData().getCost() != 0) && (this.targetShip.getEngine() != 0) ) {
			if( this.targetShip.getCrew() == 0 && this.targetShip.getUnits().isEmpty()) {
				t.setVar(	"targetship.status",	"verlassen",
							"menu.showpluendern",	1,
							"menu.showkapern",		!this.targetShip.getTypeData().hasFlag(ShipTypes.SF_NICHT_KAPERBAR ) );
			} 
			else {
				t.setVar("targetship.status", "noch bewegungsf&auml;hig");
			}
		} 
		else {
			t.setVar(	"targetship.status",	"bewegungsunf&auml;hig",
						"menu.showpluendern",	(this.targetShip.getCrew() == 0),
						"menu.showkapern",		!this.targetShip.getTypeData().hasFlag( ShipTypes.SF_NICHT_KAPERBAR) );
		}
	}
}
