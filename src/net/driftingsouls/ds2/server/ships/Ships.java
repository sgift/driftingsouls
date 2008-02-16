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
package net.driftingsouls.ds2.server.ships;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.deprecated.CacheMap;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.ships.Ship.DockMode;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

/**
 * Diverse Funktionen rund um Schiffe in DS
 * TODO: Ja, ich weiss, das ist nicht besonders schoen. Besser waeren richtige Schiffsobjekte...
 * @author Christopher Jung
 *
 */
public class Ships implements Loggable {
	private static final int MANGEL_TICKS = 9;
	
	/**
	 * Repraesentiert ein in ein Schiff eingebautes Modul (oder vielmehr die Daten, 
	 * die hinterher verwendet werden um daraus ein Modul zu rekonstruieren)
	 */
	public static class ModuleEntry {
		/**
		 * Der Slot in den das Modul eingebaut ist
		 */
		public final int slot;
		/**
		 * Der Modultyp
		 * @see net.driftingsouls.ds2.server.cargo.modules.Module
		 */
		public final int moduleType;
		/**
		 * Weitere Modultyp-spezifische Daten
		 */
		public final String data;
		
		protected ModuleEntry(int slot, int moduleType, String data) {
			this.slot = slot;
			this.moduleType = moduleType;
			this.data = data;
		}
		
		@Override
		public String toString() {
			return "ModuleEntry: "+slot+":"+moduleType+":"+data;
		}
	}
	
	/**
	 * Objekt mit Funktionsmeldungen
	 */
	public static final ContextLocalMessage MESSAGE = new ContextLocalMessage();
	
	private static Map<Location,Integer> nebel = Collections.synchronizedMap(new CacheMap<Location,Integer>(1000));
	
	/**
	 * Leert den Cache fuer Schiffsdaten
	 *
	 */
	public static void clearShipCache() {
		// TODO - Schiffcache implementieren
	}
	
	/**
	 * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
	 * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
	 * @param shipID die ID des Schiffes
	 * @return der neue Status-String
	 */
	public static String recalculateShipStatus(int shipID) {
		Database db = ContextMap.getContext().getDatabase();

		SQLResultRow ship = db.first("SELECT id,type,crew,status,cargo,owner,alarm,system,x,y FROM ships WHERE id>0 AND id='",shipID,"'");
		
		SQLResultRow type = ShipTypes.getShipType(ship);
		
		Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
		
		List<String> status = new ArrayList<String>();
		
		// Alten Status lesen und ggf Elemente uebernehmen
		String[] oldstatus = StringUtils.split(ship.getString("status"), ' ');
		
		if( oldstatus.length > 0 ) {
			for( int i=0; i < oldstatus.length; i++ ) {
				String astatus = oldstatus[i];
				if( !astatus.equals("disable_iff") && !astatus.equals("mangel_nahrung") && 
					!astatus.equals("mangel_reaktor") && !astatus.equals("offizier") && 
					!astatus.equals("nocrew") && !astatus.equals("nebel") && !astatus.equals("tblmodules") ) {
					status.add(astatus);
				}
			}
		}
		
		// Treibstoffverbrauch bereichnen
		if( type.getInt("rm") > 0 ) {
			long ep = cargo.getResourceCount( Resources.URAN ) * type.getInt("ru") + cargo.getResourceCount( Resources.DEUTERIUM ) * type.getInt("rd") + cargo.getResourceCount( Resources.ANTIMATERIE ) * type.getInt("ra");
			long er = ep/type.getInt("rm");
			
			int turns = 2;
			if( (ship.getInt("alarm") == 1) && (type.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) ) {
				turns = 4;	
			}
			
			if( er <= MANGEL_TICKS/turns ) {
				status.add("mangel_reaktor");
			}
		}
		
		// Ist Crew an Bord?
		if( (type.getInt("crew") != 0) && (ship.getInt("crew") == 0) ) {
			status.add("nocrew");	
		}
	
		// Die Items nach IFF und Hydros durchsuchen
		boolean disableIFF = false;
	
		if( cargo.getItemWithEffect(ItemEffect.Type.DISABLE_IFF) != null ) {
			disableIFF = true;
		}
		
		if( disableIFF ) {
			status.add("disable_iff");
		}
		
		Cargo usercargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM users WHERE id='"+ship.getInt("owner")+"'").getString("cargo"));
		
		// Den Nahrungsverbrauch berechnen
		if( ship.getInt("crew") > 0 ) {
			double scale = 1;
			if( (ship.getInt("alarm") == 1) && (type.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) ) {
				scale = 0.9;	
			}
			
			int nn = (int)Math.ceil(ship.getInt("crew")/scale) - type.getInt("hydro");
			if( (nn > 0) || ((nn == 0) && (type.getInt("hydro") == 0)) ) {
				if( nn == 0 ) nn = 1;
				long nr = usercargo.getResourceCount( Resources.NAHRUNG )/nn;
				
				if( nr <= MANGEL_TICKS ) {
					status.add("mangel_nahrung");
				}
			}
		}
		
		// Ist ein Offizier an Bord?
		Offizier offi = Offizier.getOffizierByDest('s', shipID);
		if( offi != null ) {
			status.add("offizier");
		}
		
		SQLResultRow modules = db.first("SELECT id FROM ships_modules WHERE id="+shipID);
		if( !modules.isEmpty() ) {
			status.add("tblmodules");
		}
		
		boolean savestatus = true;
		
		String statusString = Common.implode(" ", status);
		if( ship.getString("status").equals(statusString) ) {
			savestatus = false;
		}
	
		if( savestatus ) {
			db.tUpdate(1, "UPDATE ships SET status='"+statusString+"' WHERE id>0 AND id='",shipID,"' AND status='",ship.getString("status")+"'");
		}
		
		return statusString;
	}
	
	private static void handleRedAlert( SQLResultRow ship ) {
		Integer[] attackers = redAlertCheck( ship, false );
		
		Database db = ContextMap.getContext().getDatabase();
	
		if( attackers.length > 0 ) {
			// Schauen wir mal ob wir noch ein Schiff mit rotem Alarm ohne Schlacht finden (sortiert nach Besitzer-ID)
			SQLResultRow eship = db.first("SELECT id,owner FROM ships WHERE id>0 AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," ",
									"AND system=",ship.getInt("system")," AND `lock` IS NULL AND docked='' AND e>0 AND owner IN (",Common.implode(",",attackers),") AND alarm=1 AND !LOCATE('nocrew',status) AND battle is null ORDER BY owner");
				
			if( !eship.isEmpty() ) {
				Battle battle = new Battle();
				battle.setStartOwn(true);
				battle.create(eship.getInt("owner"), eship.getInt("id"), ship.getInt("id"));
				
				MESSAGE.get().append("<span style=\"color:red\">Feindliche Schiffe feuern beim Einflug</span><br />\n");
			}
			else {
				// Schlacht suchen und Schiffe hinzufuegen
				eship = db.first("SELECT id,battle FROM ships WHERE id>0 AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," ",
									"AND system=",ship.getInt("system")," AND `lock` IS NULL AND docked='' AND e>0 AND owner IN (",Common.implode(",",attackers),") AND alarm=1 AND !LOCATE('nocrew',status) AND battle!=0 ORDER BY owner");
						
				if( !eship.isEmpty() ) {
					Battle battle = new Battle();
					int eside = db.first("SELECT side FROM battles_ships WHERE shipid='",eship.getInt("id"),"'").getInt("side");
					int oside = (eside + 1) % 2 + 1;
					battle.load(eship.getInt("battle"), ship.getInt("owner"), 0, 0, oside);
					
					if( db.first("SELECT count(*) count FROM ships WHERE docked='",ship.getInt("id"),"'").getInt("count") != 0 ) {
						SQLQuery sid = db.query("SELECT id FROM ships WHERE docked='",ship.getInt("id"),"'");
						while( sid.next() ) {
							battle.addShip( ship.getInt("owner"), sid.getInt("id") );
						}
						sid.free();
					}
					battle.addShip( ship.getInt("owner"), ship.getInt("id") );
					
					if( battle.getEnemyLog(true).length() != 0 ) {
						battle.writeLog();
					}
					
					MESSAGE.get().append("<br /><span style=\"color:red\">Feindliche Schiffe feuern beim Einflug</span><br />\n");
				}
			}
		}
	}
	
	private static Integer[] redAlertCheck( SQLResultRow ship, boolean checkonly ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
	
		User owner = (User)context.getDB().get(User.class, ship.getInt("owner"));
		User.Relations relationlist = owner.getRelations();
	
		List<Integer> attackers = new ArrayList<Integer>();

		SQLQuery aowner = db.query("SELECT DISTINCT owner FROM ships WHERE id>0 AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," ",
							"AND system=",ship.getInt("system")," AND e>0 AND owner!=",ship.getInt("owner")," AND alarm=1 AND `lock` IS NULL AND docked='' AND !LOCATE('nocrew',status) ");
		while( aowner.next() ) {
			User auser = (User)context.getDB().get(User.class, aowner.getInt("owner"));
			if( (auser.getVacationCount() != 0) && (auser.getWait4VacationCount() == 0) ) {
				continue;	
			}
			
			if( relationlist.fromOther.get(auser.getId()) == User.Relation.ENEMY ) {
				attackers.add(aowner.getInt("owner"));
				if( checkonly ) {
					break;
				}
			} 
		}
		aowner.free();
	
		return attackers.toArray(new Integer[attackers.size()]);
	}
	
	/**
	 * Gibt <code>true</code> zurueck, wenn der angegebene Sektor fuer den angegebenen Spieler
	 * unter rotem Alarm steht, d.h. bei einem Einflug eine Schlacht gestartet wird
	 * @param userid Die Spieler-ID
	 * @param system Das System
	 * @param x Die X-Koordinate
	 * @param y Die Y-Koordinate
	 * @return <code>true</code>, falls der Sektor fuer den Spieler unter rotem Alarm steht
	 */
	public static boolean getRedAlertStatus( int userid, int system, int x, int y ) {
		SQLResultRow ship = new SQLResultRow();
		ship.put("owner", userid);
		ship.put("system", system);
		ship.put("x", x);
		ship.put("y", y);
				
		Integer[] attackers = redAlertCheck(ship, true);

		if( attackers.length > 0 ) {
			return true;
		}
		return false;
	}
	
	/**
	 * Die verschiedenen Zustaende, die zum Ende eines Fluges gefuehrt haben koennen
	 */
	public static enum MovementStatus {
		/**
		 * Der Flug war Erfolgreich
		 */
		SUCCESS,
		/**
		 * Der Flug wurde an einem EMP-Nebel abgebrochen
		 */
		BLOCKED_BY_EMP,
		/**
		 * Der Flug wurde vor einem Feld mit rotem Alarm abgebrochen
		 */
		BLOCKED_BY_RED_ALERT,
		/**
		 * Das Schiff konnte nicht mehr weiterfliegen
		 */
		SHIP_FAILURE
	}
	
	private static class MovementResult {
		int distance;
		boolean moved;
		MovementStatus status;

		MovementResult(int distance, boolean moved, MovementStatus status) {
			this.distance = distance;
			this.moved = moved;
			this.status = status;
		}
	}
	
	private static MovementResult moveSingle(SQLResultRow ship, SQLResultRow shiptype, Offizier offizier, int direction, int distance, int adocked, boolean forceLowHeat, boolean verbose) {
		boolean moved = false;
		MovementStatus error = MovementStatus.SUCCESS;
		boolean firstOutput = true;
		
		StringBuilder out = MESSAGE.get();
		
		if( ship.getInt("engine") <= 0 ) {
			if(verbose)
			{
				out.append("<span style=\"color:#ff0000\">Antrieb defekt</span><br />\n");
			}
			distance = 0;
			
			return new MovementResult(distance, moved, MovementStatus.SHIP_FAILURE);
		}
		
		int newe = ship.getInt("e") - shiptype.getInt("cost");
		int news = ship.getInt("s") + shiptype.getInt("heat");
				
		newe -= adocked;
		if( shiptype.getInt("crew")/2 > ship.getInt("crew") ) {
			newe--;
			if(verbose)
			{
				out.append("<span style=\"color:red\">Geringe Besatzung erh&ouml;ht Flugkosten</span><br />\n");
			}
		}
		
		// Antrieb teilweise beschaedigt?
		if( ship.getInt("engine") < 60 ) {
			newe -= 1;
		} 
		else if( ship.getInt("engine") < 40 ) {
			newe -= 2;
		} 
		else if( ship.getInt("engine") < 20 ) { 
			newe -= 4;
		}
		
		if( newe < 0 ) {
			if(!verbose && firstOutput)
			{
				out.append(ship.getString("name")+" ("+ship.getInt("id")+"): ");
				firstOutput = false;
			}
			out.append("<span style=\"color:#ff0000\">Keine Energie. Stoppe bei "+getLocationText(ship, true)+"</span><br />\n");
			distance = 0;
			
			return new MovementResult(distance, moved, MovementStatus.SHIP_FAILURE);
		}

		if( offizier != null ) {			
			// Flugkosten
			int success = offizier.useAbility( Offizier.Ability.NAV, 200 );
			if( success > 0 ) {
				newe += 2;
				if( newe > ship.getInt("e")-1 ) {
					newe = ship.getInt("e") - 1;
				}
				if(verbose)
				{
					out.append(offizier.getName()+" verringert Flugkosten<br />\n");
				}
			}
			// Ueberhitzung
			success = offizier.useAbility( Offizier.Ability.ING, 200 );
			if( success > 0 ) {
				news -= 1;
				if( news < ship.getInt("s") ) {
					news = ship.getInt("s");
				}
				if( verbose ) {
					out.append(offizier.getName()+" verringert &Uuml;berhitzung<br />\n");
				}
			}
			if( verbose ) {
				out.append(StringUtils.replace(offizier.MESSAGE.getMessage(),"\n", "<br />"));
			}
		}
		
		// Grillen wir uns bei dem Flug eventuell den Antrieb?
		if( news > 100 )  {
			if(forceLowHeat && distance > 0) {
				if( !verbose && firstOutput ) {
					out.append(ship.getString("name")+" ("+ship.getInt("id")+"): ");
					firstOutput = false;
				}
				out.append("<span style=\"color:#ff0000\">Triebwerk w&uuml;rde &uuml;berhitzen</span><br />\n");

				out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei "+getLocationText(ship,true)+"</span><br />\n");

				distance = 0;
				return new MovementResult(distance, moved, MovementStatus.SHIP_FAILURE);
			}
		}

		int x = ship.getInt("x");
		int y = ship.getInt("y");
	
		if( direction == 1 ) { x--; y--; }
		else if( direction == 2 ) { y--; }
		else if( direction == 3 ) { x++; y--; }
		else if( direction == 4 ) { x--; }
		else if( direction == 6 ) { x++; }
		else if( direction == 7 ) { x--; y++; }
		else if( direction == 8 ) { y++; }
		else if( direction == 9 ) { x++; y++; }
	
		StarSystem sys = Systems.get().system(ship.getInt("system"));
		
		if( x > sys.getWidth()) { 
			x = sys.getWidth();
			distance = 0;
		}
		if( y > sys.getHeight()) { 
			y = sys.getHeight();
			distance = 0;
		}
		if( x < 1 ) {
			x = 1;
			distance = 0;
		}
		if( y < 1 ) {
			y = 1;
			distance = 0;
		}
		
		if( (ship.getInt("x") != x) || (ship.getInt("y") != y) ) {
			moved = true;
			
			if( ship.getInt("s") >= 100 ) {
				if( !verbose && firstOutput) {
					out.append(ship.getString("name")+" ("+ship.getInt("id")+"): ");
					firstOutput = false;
				}
				out.append("<span style=\"color:#ff0000\">Triebwerke &uuml;berhitzt</span><br />\n");
				
				if( (RandomUtils.nextInt(101)) < 3*(news-100) ) {
					int dmg = (int)( (2*(RandomUtils.nextInt(101)/100d)) + 1 ) * (news-100);
					out.append("<span style=\"color:#ff0000\">Triebwerke nehmen "+dmg+" Schaden</span><br />\n");
					ship.put("engine", ship.getInt("engine")-dmg);
					if( ship.getInt("engine") < 0 ) {
						ship.put("engine", 0);
					}
					if( distance > 0 ) {
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab bei "+getLocationText(ship,true)+"</span><br />\n");
						error = MovementStatus.SHIP_FAILURE;
						distance = 0;
					}
				}
			}
						
			ship.put("x", x);
			ship.put("y", y);
			ship.put("e", newe);
			ship.put("s", news);
			
			if( verbose ) {
				out.append(ship.getString("name")+" fliegt in "+getLocationText(ship,true)+" ein<br />\n");
			}
		}
		
		return new MovementResult(distance, moved, error);
	}
	
	/**
	 * Enthaelt die Daten der Schiffe in einer Flotte, welche sich gerade bewegt
	 *
	 */
	private static class FleetMovementData {
		FleetMovementData() {
			// EMPTY
		}
		
		/**
		 * Die Schiffe in der Flotte
		 */
		Map<Integer,SQLResultRow> ships = new HashMap<Integer,SQLResultRow>();
		/**
		 * Die Offiziere auf den Schiffen der Flotte
		 */
		Map<Integer,Offizier> offiziere = new HashMap<Integer,Offizier>();
	}
	
	private static MovementStatus moveFleet(SQLResultRow ship, int direction, boolean forceLowHeat, boolean verbose)  {
		StringBuilder out = MESSAGE.get();
		MovementStatus error = MovementStatus.SUCCESS;
		
		boolean firstEntry = true;
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");
		
		if( fleetdata == null ) {
			fleetdata = new FleetMovementData();
			
			context.putVariable(Ships.class, "fleetdata", fleetdata);
			
			Database db = context.getDatabase();
			
			SQLQuery fleetshipRow = db.query("SELECT id,name,type,x,y,crew,e,s,engine,system,status,`lock` FROM ships ", 
									"WHERE id>0 AND fleet=",ship.getInt("fleet")," AND x='",ship.getInt("x"),"' AND y='",ship.getInt("y"),"' ", 
									"AND system='",ship.getInt("system"),"' AND owner='",ship.getInt("owner"),"' AND docked='' AND ",
									"id!='",ship.getInt("id"),"' AND e>0 AND battle is null");
			while( fleetshipRow.next() ) {
				if( verbose && firstEntry ) {
					firstEntry = false;
					out.append("<table class=\"noBorder\">\n");
				}
				SQLResultRow fleetship = fleetshipRow.getRow();
				SQLResultRow shiptype = ShipTypes.getShipType(fleetship);
				
				StringBuilder outpb = new StringBuilder();
				
				if( fleetship.getString("lock").length() != 0 ) {
					outpb.append("<span style=\"color:red\">Fehler: Das Schiff ist an ein Quest gebunden</span>\n");
					outpb.append("</span></td></tr>\n");
					error = MovementStatus.SHIP_FAILURE;
				}
				
				if( shiptype.getInt("cost") == 0 ) {
					outpb.append("<span style=\"color:red\">Das Objekt kann nicht fliegen, da es keinen Antieb hat</span><br />");
					error = MovementStatus.SHIP_FAILURE;
				}
				
				if( (fleetship.getInt("crew") == 0) && (shiptype.getInt("crew") > 0) ) {
					outpb.append("<span style=\"color:red\">Fehler: Sie haben keine Crew auf dem Schiff</span><br />");
					error = MovementStatus.SHIP_FAILURE;
				}
				
				if( outpb.length() != 0 ) {
					out.append("<tr>\n");
					out.append("<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange; font-size:12px\"> "+fleetship.getString("name")+" ("+fleetship.getInt("id")+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n");
					out.append(outpb);
					out.append("</span></td></tr>\n");
				}
				else {
					fleetship.put("dockedcount", 0);
					fleetship.put("adockedcount", 0);
					if( (shiptype.getInt("jdocks") > 0) || (shiptype.getInt("adocks") > 0) ) { 
						int docks = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked IN ('l ",fleetship.getInt("id"),"','",fleetship.getInt("id"),"')").getInt("count");
					
						fleetship.put("dockedcount", docks);
						if( shiptype.getInt("adocks") > 0 ) {
							int adocks = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='",fleetship.getInt("id"),"'").getInt("count");
							fleetship.put("adockedcount", adocks);	
						} 
					}
				
					if( fleetship.getString("status").indexOf("offizier") > -1 ) {
						fleetdata.offiziere.put(fleetship.getInt("id"), Offizier.getOffizierByDest('s', fleetship.getInt("id")));
					}
									
					fleetdata.ships.put(fleetship.getInt("id"), fleetship);
				}
			}
			fleetshipRow.free();
		}
		
		if( error != MovementStatus.SUCCESS ) {
			return error;
		}
		
		for( SQLResultRow fleetship : fleetdata.ships.values() ) {
			if( verbose && firstEntry ) {
				firstEntry = false;
				out.append("<table class=\"noBorder\">\n");
			}
			
			if(verbose)
			{
				out.append("<tr>\n");
				out.append("<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange; font-size:12px\"> "+fleetship.getString("name")+" ("+fleetship.getInt("id")+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n");
			}	
			Offizier offizierf = fleetdata.offiziere.get(fleetship.getInt("id"));
	
			SQLResultRow shiptype = ShipTypes.getShipType(fleetship);
			
			MovementResult result = moveSingle(fleetship, shiptype, offizierf, direction, 1, fleetship.getInt("adockedcount"), forceLowHeat, verbose);
			
			//Einen einmal gesetzten Fehlerstatus nicht wieder aufheben
			if(error == MovementStatus.SUCCESS)
			{
				error = result.status;
			}
			
			if(verbose)
			{
				out.append("</span></td></tr>\n");
			}
		}
		
		if( !firstEntry )
			out.append("</table>\n");
			
		return error;
	}
	
	private static void saveFleetShips() {	
		Context context = ContextMap.getContext();
		FleetMovementData fleetdata = (FleetMovementData)context.getVariable(Ships.class, "fleetdata");
		
		if( fleetdata != null ) {
			Database db = context.getDatabase();
			
			PreparedQuery updateShip = db.prepare("UPDATE ships SET x= ?, y= ?, e= ?, s= ?, engine= ? WHERE id= ?"); 
			PreparedQuery updateDocked = db.prepare("UPDATE ships SET x= ?, y= ?, system= ? WHERE id>0 AND docked IN ( ? , ?)");
			
			for( SQLResultRow fleetship : fleetdata.ships.values() ) {
				updateShip.update(fleetship.getInt("x"), fleetship.getInt("y"), fleetship.getInt("e"), fleetship.getInt("s"), fleetship.getInt("engine"), fleetship.getInt("id"));

				if( fleetship.getInt("dockedcount") > 0 ) {
					updateDocked.update(fleetship.getInt("x"), fleetship.getInt("y"), fleetship.getInt("system"), "l "+fleetship.getInt("id"), Integer.toString(fleetship.getInt("id")));
				}
				
				recalculateShipStatus(fleetship.getInt("id"));
			}
		}
		context.putVariable(Ships.class, "fleetships", null);
		context.putVariable(Ships.class, "fleetoffiziere", null);
	}
	
	/**
	 * <p>Fliegt ein Schiff eine Flugroute entlang. Falls das Schiff einer Flotte angehoert, fliegt
	 * diese ebenfalls n Felder in diese Richtung.</p>
	 * <p>Der Flug wird abgebrochen sobald eines der Schiffe nicht mehr weiterfliegen kann</p>
	 * Die Flugrouteninformationen werden waehrend des Fluges modifiziert
	 * 
	 * @param shipID Die ID des Schiffes, welches fliegen soll
	 * @param route Die Flugroute
	 * @param forceLowHeat Soll bei Ueberhitzung sofort abgebrochen werden?
	 * @param disableQuests Sollen Questhandler ignoriert werden?
	 * @return Der Status der zum Abbruch des Fluges gefuehrt hat
	 */
	public static MovementStatus move(int shipID, List<Waypoint> route, boolean forceLowHeat, boolean disableQuests) {
		StringBuilder out = MESSAGE.get();
		
		Database db = ContextMap.getContext().getDatabase();
	
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE id=",shipID);
		
		if( ship.isEmpty() ) {
			out.append("Fehler: Das angegebene Schiff existiert nicht\n");
			return MovementStatus.SHIP_FAILURE; 
		}
		if( ship.getString("lock").length() != 0 ) {
			out.append("Fehler: Das Schiff ist an ein Quest gebunden\n");
			return MovementStatus.SHIP_FAILURE;
		}
	
		User user = (User)ContextMap.getContext().getDB().get(User.class, ship.getInt("owner"));
				
		SQLResultRow shiptype = ShipTypes.getShipType(ship);
		Offizier offizier = Offizier.getOffizierByDest('s',ship.getInt("id"));
		
		//Das Schiff soll sich offenbar bewegen
		if( ship.getString("docked").length() != 0 ) {
			out.append("Fehler: Sie k&ouml;nnen nicht mit dem Schiff fliegen, da es geladet/angedockt ist\n");
			return MovementStatus.SHIP_FAILURE;
		}
	
		if( shiptype.getInt("cost") == 0 ) {
			out.append("Fehler: Das Objekt kann nicht fliegen, da es keinen Antieb hat\n");
			return MovementStatus.SHIP_FAILURE;
		}
	
		if( ship.getInt("battle") > 0 ) {
			out.append("Fehler: Das Schiff ist in einen Kampf verwickelt\n");
			return MovementStatus.SHIP_FAILURE;
		}
		
		if( (ship.getInt("crew") <= 0) && (shiptype.getInt("crew") > 0) ) {
			out.append("<span style=\"color:#ff0000\">Das Schiff verf&uuml;gt &uuml;ber keine Crew</span><br />\n");
			return MovementStatus.SHIP_FAILURE;
		}
			
		int docked = 0;
		int adocked = 0;
		MovementStatus error = MovementStatus.SUCCESS;
		
		if( (shiptype.getInt("jdocks") > 0) || (shiptype.getInt("adocks") > 0) ) {
			docked = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')").getInt("count");
			if( shiptype.getInt("adocks") > 0 ) {
				adocked = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='",ship.getInt("id"),"'").getInt("count");
			}
		}
		
		boolean moved = false;
		
		while( (error == MovementStatus.SUCCESS) && route.size() > 0 ) {
			Waypoint waypoint = route.remove(0);
			
			if( waypoint.type != Waypoint.Type.MOVEMENT ) {
				throw new RuntimeException("Es wird nur "+Waypoint.Type.MOVEMENT+" als Wegpunkt unterstuetzt");
			}
			
			if( waypoint.direction == 5 ) {
				continue;
			}
			
			// Zielkoordinaten/Bewegungsrichtung berechnen
			String xbetween = "x='"+ship.getInt("x")+"'";
			String ybetween = "y='"+ship.getInt("y")+"'";
			int xoffset = 0;
			int yoffset = 0;
			if( waypoint.direction <= 3 ) {
				ybetween = "y BETWEEN '"+(ship.getInt("y")-waypoint.distance)+"' AND '"+ship.getInt("y")+"'";
				yoffset--;
			}
			else if( waypoint.direction >= 7 ) {
				ybetween = "y BETWEEN '"+ship.getInt("y")+"' AND '"+(ship.getInt("y")+waypoint.distance)+"'";
				yoffset++;
			}
			
			if( (waypoint.direction-1) % 3 == 0 ) {
				xbetween = "x BETWEEN '"+(ship.getInt("x")-waypoint.distance)+"' AND '"+ship.getInt("x")+"'";
				xoffset--;
			}
			else if( waypoint.direction % 3 == 0 ) {
				xbetween = "x BETWEEN '"+ship.getInt("x")+"' AND '"+(ship.getInt("x")+waypoint.distance)+"'";
				xoffset++;
			}
			
			// Alle potentiell relevanten Sektoren (ok..und ein wenig ueberfluessiges Zeug bei schraegen Bewegungen) auslesen
			Map<Location,SQLResultRow> sectorlist = new HashMap<Location,SQLResultRow>();
			SQLQuery sectorRow = db.query("SELECT * FROM sectors " ,
					"WHERE system IN (",ship.getInt("system"),",-1) AND (x='-1' OR ",xbetween,") AND (y='-1' OR ",ybetween,") ORDER BY system DESC");
								 	
			while( sectorRow.next() ) {
				SQLResultRow row = sectorRow.getRow();
				sectorlist.put(Location.fromResult(row), row);
			}
			sectorRow.free();
			
			// Alle potentiell relevanten Sektoren mit Schiffen auf rotem Alarm (ok..und ein wenig ueberfluessiges Zeug bei schraegen Bewegungen) auslesen
			Map<Location,Boolean> redalertlist = new HashMap<Location,Boolean>();
			sectorRow = db.query("SELECT x,y FROM ships " ,
					"WHERE owner!='",ship.getInt("owner"),"' AND alarm='1' AND system=",ship.getInt("system")," AND ",xbetween," AND ",ybetween);
								 	
			while( sectorRow.next() ) {
				redalertlist.put(new Location(ship.getInt("system"), sectorRow.getInt("x"), sectorRow.getInt("y")), Boolean.TRUE);
			}
			sectorRow.free();
			
			// Alle potentiell relevanten Sektoren mit EMP-Nebeln (ok..und ein wenig ueberfluessiges Zeug bei schraegen Bewegungen) auslesen
			Map<Location,Boolean> nebulaemplist = new HashMap<Location,Boolean>();
			sectorRow = db.query("SELECT system,x,y,type FROM nebel ",
					"WHERE type>=3 AND type<=5 AND system=",ship.getInt("system")," AND ",xbetween," AND ",ybetween);
								 	
			while( sectorRow.next() ) {
				cacheNebula(sectorRow.getRow());
				nebulaemplist.put(new Location(ship.getInt("system"), sectorRow.getInt("x"), sectorRow.getInt("y")), Boolean.TRUE);
			}
			sectorRow.free();
			
			if( (waypoint.distance > 1) && nebulaemplist.containsKey(Location.fromResult(ship)) ) {
				out.append("<span style=\"color:#ff0000\">Der Autopilot funktioniert in EMP-Nebeln nicht</span><br />\n");
				return MovementStatus.BLOCKED_BY_EMP;
			}
			
			long starttime = System.currentTimeMillis();
			
			int startdistance = waypoint.distance;
			
			// Und nun fliegen wir mal ne Runde....
			while( waypoint.distance > 0 ) {
				// Schauen wir mal ob wir vor rotem Alarm warnen muessen
				if( (startdistance > 1) && redalertlist.containsKey(new Location(ship.getInt("system"),ship.getInt("x")+xoffset, ship.getInt("y")+yoffset)) ) {
					SQLResultRow newship = new SQLResultRow();
					newship.putAll(ship);
					newship.put("x", newship.getInt("x") + xoffset);
					newship.put("y", newship.getInt("y") + yoffset);
					Integer[] attackers = redAlertCheck(newship, false);
					if( attackers.length != 0 ) {
						out.append("<span style=\"color:#ff0000\">Feindliche Schiffe in Alarmbereitschaft im n&auml;chsten Sektor geortet</span><br />\n");
						out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
						error = MovementStatus.BLOCKED_BY_RED_ALERT;
						waypoint.distance = 0;
						break;
					}
				}
				
				if( (startdistance > 1) && nebulaemplist.containsKey(new Location(ship.getInt("system"),ship.getInt("x")+xoffset, ship.getInt("y")+yoffset)) ) {
					out.append("<span style=\"color:#ff0000\">EMP-Nebel im n&auml;chsten Sektor geortet</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					error = MovementStatus.BLOCKED_BY_EMP;
					waypoint.distance = 0;
					break;
				}
				
				int olddirection = waypoint.direction;
				
				// ACHTUNG: Ob das ganze hier noch sinnvoll funktioniert, wenn distance > 1 ist, ist mehr als fraglich...
				if( nebulaemplist.containsKey(new Location(ship.getInt("system"),ship.getInt("x")+xoffset, ship.getInt("y")+yoffset)) && 
					(RandomUtils.nextInt(100+1) > 75) ) {
					int nebel = getNebula(ship);
					if( nebel == 5 ) {
						waypoint.direction = RandomUtils.nextInt(10)+1;
						if( waypoint.direction > 4 ) {
							waypoint.direction++;
							
						}
						// Nun muessen wir noch die Caches fuellen
						if( waypoint.direction != olddirection ) {
							int tmpxoff = 0;
							int tmpyoff = 0;
							
							if( waypoint.direction <= 3 ) {
								tmpyoff--;
							}
							else if( waypoint.direction >= 7 ) {
								tmpyoff++;
							}
							
							if( (waypoint.direction-1) % 3 == 0 ) {
								tmpxoff--;
							}
							else if( waypoint.direction % 3 == 0 ) {
								tmpxoff++;
							}
							
							SQLQuery sector = db.query("SELECT * FROM sectors " ,
				 					"WHERE system IN (",ship.getInt("system"),",-1) AND (x='-1' OR ",(ship.getInt("x")+tmpxoff),") AND (y='-1' OR ",(ship.getInt("y")+tmpyoff),")  ORDER BY system DESC");
							while( sector.next() ) {
								SQLResultRow row = sector.getRow();
								sectorlist.put(Location.fromResult(row), row);
							}
							sector.free();
							
							SQLResultRow rasect = db.first("SELECT x,y FROM ships " ,
								 	"WHERE owner!='",ship.getInt("owner"),"' AND alarm='1' AND system=",ship.getInt("system")," AND x='",(ship.getInt("x")+tmpxoff),"' AND y='",(ship.getInt("y")+tmpyoff),"'");
							 	
							if( !rasect.isEmpty() ) {
								redalertlist.put(new Location(ship.getInt("system"), rasect.getInt("x"), rasect.getInt("y")), Boolean.TRUE);
							}
						}
					}
				}
				
				waypoint.distance--;
				
				SQLResultRow oldship = new SQLResultRow();
				oldship.putAll(ship);
				
				MovementResult result = moveSingle(ship, shiptype, offizier, waypoint.direction, waypoint.distance, adocked, forceLowHeat, false);
				error = result.status;
				waypoint.distance = result.distance;
				
				if( result.moved ) {
					// Jetzt, da sich unser Schiff korrekt bewegt hat, fliegen wir auch die Flotte ein stueck weiter	
					if( ship.getInt("fleet") > 0 ) {
						MovementStatus fleetResult = moveFleet(oldship, waypoint.direction, forceLowHeat, false);
						if( fleetResult != MovementStatus.SUCCESS  ) {
							error = fleetResult;
							waypoint.distance = 0;
						}
					}
					
					moved = true;
					if( !disableQuests && (sectorlist.size() != 0) ) {
						// Schauen wir mal, ob es ein onenter-ereigniss gab
						Location loc = Location.fromResult(ship);
						
						SQLResultRow sector = sectorlist.get(new Location(loc.getSystem(), -1, -1));
						if( sectorlist.containsKey(loc) ) {
							sector = sectorlist.get(loc);
						}
						else if( sectorlist.containsKey(loc.setX(-1)) ) { 
							sector = sectorlist.get(loc.setX(-1));
						}
						else if( sectorlist.containsKey(loc.setY(-1)) ) { 
							sector = sectorlist.get(loc.setY(-1));
						}
						
						if( !sector.isEmpty() && sector.getString("onenter").length() > 0 ) {
							db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",e=",ship.getInt("e"),",s=",ship.getInt("s"),",engine=",ship.getInt("engine"),",docked='' WHERE id=",ship.getInt("id"));
							if( docked != 0 ) {
								db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",system=",ship.getInt("system")," WHERE id>0 AND docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')");
							}
							recalculateShipStatus(ship.getInt("id"));
							saveFleetShips();
							
							ScriptParser scriptparser = ContextMap.getContext().get(ContextCommon.class).getScriptParser(ScriptParser.NameSpace.QUEST);
							scriptparser.setShip(ship);
							if( !user.hasFlag(User.FLAG_SCRIPT_DEBUGGING) ) {
								scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
							}
								
							scriptparser.setRegister("SECTOR", loc.toString() );
										
							Quests.currentEventURL.set("&action=onenter");
									
							db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",e=",ship.getInt("e"),",s=",ship.getInt("s"),",engine=",ship.getInt("engine"),",docked='' WHERE id=",ship.getInt("id"));
							if( docked != 0 ) {
								db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",system=",ship.getInt("system")," WHERE id>0 AND docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')");
							}
							
							if( Quests.executeEvent(scriptparser, sector.getString("onenter"), ship.getInt("owner"), "" ) ) {
								if( scriptparser.getContext().getOutput().length()!= 0 ) {							
									waypoint.distance = 0;
								}
							}
						}
					}
					
					if( redalertlist.containsKey(Location.fromResult(ship)) ) {
						db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",e=",ship.getInt("e"),",s=",ship.getInt("s"),",engine=",ship.getInt("engine"),",docked='' WHERE id=",ship.getInt("id"));
						if( docked != 0 ) {
							db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",system=",ship.getInt("system")," WHERE id>0 AND docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')");
						}
						recalculateShipStatus(ship.getInt("id"));
						saveFleetShips();
						
						handleRedAlert( ship );	
					}
				}
				
				// Wenn wir laenger als 25 Sekunden fuers fliegen gebraucht haben -> abbrechen!
				if( System.currentTimeMillis() - starttime > 25000 ) {
					out.append("<span style=\"color:#ff0000\">Flug dauert zu lange</span><br />\n");
					out.append("<span style=\"color:#ff0000\">Autopilot bricht ab</span><br />\n");
					waypoint.distance = 0;
					error = MovementStatus.SHIP_FAILURE;
				}
			}  // while distance > 0
			
		} // while !error && route.size() > 0
		
		if( moved ) {
			out.append("Ankunft bei "+getLocationText(ship,true)+"<br /><br />\n");
			
			db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",e=",ship.getInt("e"),",s=",ship.getInt("s"),",engine=",ship.getInt("engine"),",docked='' WHERE id=",ship.getInt("id"));
			if( docked != 0 ) {
				db.update("UPDATE ships SET x=",ship.getInt("x"),",y=",ship.getInt("y"),",system=",ship.getInt("system")," WHERE id>0 AND docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')");
			}
		}
		recalculateShipStatus(ship.getInt("id"));
		saveFleetShips();
		
		return error;
	}
	
	private static boolean fleetJump( SQLResultRow ship, int nodeId, boolean knode ) {
		boolean firstentry = true;
		
		String kprotectstr = "";
		StringBuilder outputbuffer = MESSAGE.get();;
		boolean error = false;
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
	
		SQLResultRow node = null;
		String nodetarget = "";
		String nodetypename = "";
		
		if( !knode ) {
			nodetypename = "Der Sprungpunkt";
			
			node = db.first("SELECT name,x,y,system,xout,yout,systemout,wpnblock,gcpcolonistblock FROM jumpnodes WHERE id=",nodeId);
			
			nodetarget = node.getString("name")+" ("+node.getInt("systemout")+")";
		}
		else {
			kprotectstr = "AND id != "+nodeId;
			
			/* Behandlung Knossosportale:
			 *
			 * Ziel wird mit ships.jumptarget festgelegt - Format: art|koords/id|user/ally/gruppe
			 * Beispiele: 
			 * fix|2:35/35|all:
			 * ship|id:10000|ally:1
			 * base|id:255|group:-15,455,1200
			 * fix|8:20/100|default <--- diese Einstellung entspricht der bisherigen Praxis
			 */
			
			node = db.first("SELECT t1.id,t1.name,t1.x,t1.y,t1.system,t1.jumptarget,t1.owner,t2.ally,t1.type,t1.status FROM ships t1 JOIN users t2 ON t1.owner=t2.id WHERE t1.id>0 AND t1.id=",nodeId);
			if( node.isEmpty() ) {
				outputbuffer.append("Fehler: Der angegebene Sprungpunkt existiert nicht<br />\n");
				return true;
			}
			
			nodetypename = ShipTypes.getShipType(node).getString("nickname");
			
			/* 
			 * Ermittlung der Zielkoordinaten
			 * geprueft wird bei Schiffen und Basen das Vorhandensein der Gegenstation
			 * existiert keine, findet kein Sprung statt
			 */
			
			Location targetLoc = null;
			
			String[] target = StringUtils.split(node.getString("jumptarget"), '|');
			if( target[0].equals("fix") ) {
				targetLoc = Location.fromString(target[1]);
								
				nodetarget = target[1];
			} 
			else if( target[0].equals("ship") ) {
				String[] shiptarget = StringUtils.split(target[1], ':');
				SQLResultRow jmptarget = db.first("SELECT system,x,y FROM ships WHERE id=",shiptarget[1]);
				if( jmptarget.isEmpty() ) {
					outputbuffer.append("<span style=\"color:red\">Die Empfangsstation existiert nicht!</span><br />\n");
					return true;
				}
				
				targetLoc = Location.fromResult(jmptarget);
				nodetarget = targetLoc.toString();
			}	
			else if( target[0].equals("base") ) {
				String[] shiptarget = StringUtils.split(target[1], ':');
				SQLResultRow jmptarget = db.first("SELECT system,x,y FROM bases WHERE id=",shiptarget[1]);
				if( jmptarget.isEmpty() ) {
					outputbuffer.append("<span style=\"color:red\">Die Empfangsbasis existiert nicht!</span><br />\n");
					return true;
				}
				
				targetLoc = Location.fromResult(jmptarget);
				nodetarget = targetLoc.toString();
			}
			
			node.put("systemout", targetLoc.getSystem());
			node.put("xout", targetLoc.getX());
			node.put("yout", targetLoc.getY());
		}
	
		SQLQuery aship = db.query("SELECT * FROM ships WHERE id>0 AND fleet=",ship.getInt("fleet")," AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," AND system=",ship.getInt("system")," AND docked='' AND id!=",ship.getInt("id")," ",kprotectstr);
		while( aship.next() ) {
			if( firstentry ) {
				outputbuffer.append("<span style=\"color:lime\">Flotte: </span><br />");
				firstentry = false;
			}
			
			if( aship.getString("lock") != null && aship.getString("lock").length() > 0 ) {
				outputbuffer.append("<span style=\"color:red\">Die "+aship.getString("name")+" ("+aship.getInt("id")+") ist an ein Quest gebunden</span><br />\n");
				error = true;
				break;	
			}
	
			User user = (User)context.getDB().get(User.class, aship.getInt("owner"));
			
			if( !knode ) {				
				// Ist die Jumpnode blockiert?
				if( (aship.getInt("owner") > 1) && node.getBoolean("gcpcolonistblock") && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) && !user.hasFlag(User.FLAG_NO_JUMPNODE_BLOCK) ) {
					outputbuffer.append("<span style=\"color:red\">Die GCP hat diesen Sprungpunkt f&uuml;r Kolonisten gesperrt</span><br />\n");
					error = true;
					break;
				}
		
				// Kann man durch die Jumpnode (mit Waffen) fliegen
				if( node.getBoolean("wpnblock") && !user.hasFlag(User.FLAG_MILITARY_JUMPS) ) {
					SQLResultRow shiptype = ShipTypes.getShipType(aship.getRow());
					
					//Schiff Ueberprfen
					if( shiptype.getInt("military") > 0 ) {
						outputbuffer.append("<span style=\"color:red\">Die GCP verwehrt ihrem Kriegsschiff den Einflug nach "+node.getString("name")+"</span><br />\n");
						error = true;
						break;
					}
		
					//Angedockte Schiffe ueberprfen
					if( shiptype.getInt("adocks")>0 || shiptype.getInt("jdocks")>0 ) {
						boolean wpnfound = false;
						SQLQuery wpncheckhandle = db.query("SELECT t1.id,t1.type,t1.status FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id WHERE id>0 AND t1.docked IN ('l ",aship.getInt("id"),"','",aship.getInt("id"),"') AND (LOCATE('=',t2.weapons) OR LOCATE('tblmodules',t1.status))");
						while( wpncheckhandle.next() ) {
							SQLResultRow checktype = ShipTypes.getShipType(wpncheckhandle.getRow());
							if( checktype.getInt("military") > 0 ) {
								wpnfound = true;
								break;	
							}
						}
						wpncheckhandle.free();
							
						if(	wpnfound ) {
							outputbuffer.append("<span style=\"color:red\">Die GCP verwehrt einem/mehreren ihrer angedockten Kriegsschiffe den Einflug nach "+node.getString("name")+"</span><br />\n");
							error = true;
							break;
						}
					}
				}
			}
			// Gehoert das Knossosportal dem Spieler bzw seiner ally?
			else {
				if( nodeId == aship.getInt("id") ) {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen nicht mit dem "+nodetypename+" durch sich selbst springen</span><br />\n");
					return true;
				}
				
				String[] target = StringUtils.split(node.getString("jumptarget"), '|');
				
				/* 
				 * Ermittlung der Sprungberechtigten
				 */
				String[] jmpnodeuser = StringUtils.split(target[2], ':'); // Format art:ids aufgespalten
				
				if( jmpnodeuser[0].equals("all") ) {
					// Keine Einschraenkungen
				}
				// die alte variante 
				else if( jmpnodeuser[0].equals("default") || jmpnodeuser[0].equals("ownally") ){
					if( ( (user.getAlly() != null) && (node.getInt("ally") != user.getAlly().getId()) ) || 
						( user.getAlly() == null && (node.getInt("owner") != user.getId()) ) ) {
						outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - default</span><br />\n");
						return true;
					}
				}
				// user:$userid
				else if ( jmpnodeuser[0].equals("user") ){
					if( Integer.parseInt(jmpnodeuser[1]) != user.getId() )  {
						outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - owner</span><br />\n");
						return true;
					}
				}
				// ally:$allyid
				else if ( jmpnodeuser[0].equals("ally") ){
					if( (user.getAlly() == null) || (Integer.parseInt(jmpnodeuser[1]) != user.getAlly().getId()) )  {
						outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - ally</span><br />\n");
						return true;
					}
				}
				// group:userid1,userid2, ...,useridn
				else if ( jmpnodeuser[0].equals("group") ){
					Integer[] userlist = Common.explodeToInteger(",", jmpnodeuser[1]);
					if( !Common.inArray(user.getId(), userlist) )  {
						outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - group</span><br />\n");
						return true;
					}
				}
			}
			if( aship.getInt("e") < 5 ) {
				outputbuffer.append("<span style=\"color:red\">Zuwenig Energie zum Springen</span><br />\n");
				error = true;
				break;
			}

			outputbuffer.append(aship.getString("name")+" springt nach "+nodetarget+"<br />\n");

			db.update("UPDATE ships SET x=",node.getInt("xout"),",y=",node.getInt("yout"),",system=",node.getInt("systemout"),",e=",aship.getInt("e")-5," WHERE id>0 AND id=",aship.getInt("id"));
			db.update("UPDATE ships SET x=",node.getInt("xout"),",y=",node.getInt("yout"),",system=",node.getInt("systemout")," WHERE id>0 AND docked IN ('",aship.getInt("id"),"','l ",aship.getInt("id"),"')");
				
			recalculateShipStatus(aship.getInt("id"));
		}
		aship.free();
		
		return error;
	}
	
	/**
	 * <p>Laesst ein Schiff durch einen Sprungpunkt springen.
	 * Der Sprungpunkt kann entweder ein normaler Sprungpunkt
	 * oder ein "Knossos"-Sprungpunkt (als ein mit einem Schiff verbundener
	 * Sprungpunkt) sein.</p>
	 * <p>Bei letzterem kann der Sprung scheitern, wenn keine Sprungberechtigung
	 * vorliegt.</p>
	 * 
	 * @param shipID Die ID des Schiffes, welches fliegen soll
	 * @param nodeID Die ID des Sprungpunkts/Des Schiffes mit dem Sprungpunkt
	 * @param knode <code>true</code>, falls es sich um einen "Knossos"-Sprungpunkt handelt
	 * @return <code>true</code>, falls ein Fehler aufgetreten ist
	 */
	public static boolean jump(int shipID, int nodeID, boolean knode ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		SQLResultRow ship = db.first("SELECT * FROM ships WHERE id>0 AND id=",shipID);
		SQLResultRow shiptype = ShipTypes.getShipType(ship);
		StringBuilder outputbuffer = MESSAGE.get();

		if( ship.getString("lock").length() > 0 ) {
			outputbuffer.append("Fehler: Das Schiff ist an ein Quest gebunden<br />\n");
			return true;
		}
			 
		String nodetypename = "";
		String nodetarget = "";
		
		User user = (User)context.getDB().get(User.class, ship.getInt("owner"));
		SQLResultRow datan = null;
		
		if( !knode ) {
			nodetypename = "Der Sprungpunkt";
			
			datan = db.first("SELECT name,x,y,system,xout,yout,systemout,wpnblock,gcpcolonistblock FROM jumpnodes WHERE id=",nodeID);
			if( datan.isEmpty() ) {
				outputbuffer.append("Fehler: Der angegebene Sprungpunkt existiert nicht<br />\n");
				return true;
			}
			
			nodetarget = datan.getString("name")+" ("+datan.getInt("systemout")+")";
			
			if( (ship.getInt("owner") > 1) && datan.getBoolean("gcpcolonistblock") && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) && !user.hasFlag(User.FLAG_NO_JUMPNODE_BLOCK) ) {
				outputbuffer.append("<span style=\"color:red\">Die GCP hat diesen Sprungpunkt f&uuml;r Kolonisten gesperrt</span><br />\n");
				return true;
			}
	
			if( datan.getBoolean("wpnblock") && !user.hasFlag(User.FLAG_MILITARY_JUMPS) ) {
				//Schiff Ueberprfen
				if( shiptype.getInt("military") > 0 ) {
					outputbuffer.append("<span style=\"color:red\">Die GCP verwehrt ihrem Kriegsschiff den Einflug nach "+datan.getString("name")+"</span><br />\n");
					return true;
				}
	
				//Angedockte Schiffe ueberprfen
				if( shiptype.getInt("adocks")>0 || shiptype.getInt("jdocks")>0 ) {
					boolean wpnfound = false;
					SQLQuery wpncheckhandle = db.query("SELECT t1.id,t1.type,t1.status FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id WHERE id>0 AND t1.docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"') AND (LOCATE('=',t2.weapons) OR LOCATE('tblmodules',t1.status))");
					while( wpncheckhandle.next() ) {
						SQLResultRow checktype = ShipTypes.getShipType(wpncheckhandle.getRow());
						if( checktype.getInt("military") > 0 ) {
							wpnfound = true;
							break;	
						}
					}
					wpncheckhandle.free();
						
					if(	wpnfound ) {
						outputbuffer.append("<span style=\"color:red\">Die GCP verwehrt einem/mehreren ihrer angedockten Kriegsschiffe den Einflug nach "+datan.getString("name")+"</span><br />\n");
						return true;
					}
				}
			}
		} 
		else {	
			/* Behandlung Knossosportale:
			 *
			 * Ziel wird mit ships.jumptarget festgelegt - Format: art|koords/id|user/ally/gruppe
			 * Beispiele: 
			 * fix|2:35/35|all:
			 * ship|id:10000|ally:1
			 * base|id:255|group:-15,455,1200
			 * fix|8:20/100|default <--- diese Einstellung entspricht der bisherigen Praxis
			 */
			nodetypename = "Knossosportal";
			
			datan = db.first("SELECT t1.id,t1.name,t1.x,t1.y,t1.system,t1.jumptarget,t1.owner,t2.ally,t1.type,t1.status FROM ships t1 JOIN users t2 ON t1.owner=t2.id WHERE t1.id>0 AND t1.id=",nodeID);
			if( datan.isEmpty() ) {
				outputbuffer.append("Fehler: Der angegebene Sprungpunkt existiert nicht<br />\n");
				return true;
			}
			
			nodetypename = ShipTypes.getShipType(datan).getString("nickname");
			
			/* 
			 * Ermittlung der Zielkoordinaten
			 * geprueft wird bei Schiffen und Basen das Vorhandensein der Gegenstation
			 * existiert keine, findet kein Sprung statt
			 */
			
			Location targetLoc = null;
			
			String[] target = StringUtils.split(datan.getString("jumptarget"), '|');
			if( target[0].equals("fix") ) {
				targetLoc = Location.fromString(target[1]);
								
				nodetarget = target[1];
			} 
			else if( target[0].equals("ship") ) {
				String[] shiptarget = StringUtils.split(target[1], ':');
				SQLResultRow jmptarget = db.first("SELECT system,x,y FROM ships WHERE id=",shiptarget[1]);
				if( jmptarget.isEmpty() ) {
					outputbuffer.append("<span style=\"color:red\">Die Empfangsstation existiert nicht!</span><br />\n");
					return true;
				}
				
				targetLoc = Location.fromResult(jmptarget);
				nodetarget = targetLoc.toString();
			}	
			else if( target[0].equals("base") ) {
				String[] shiptarget = StringUtils.split(target[1], ':');
				SQLResultRow jmptarget = db.first("SELECT system,x,y FROM bases WHERE id=",shiptarget[1]);
				if( jmptarget.isEmpty() ) {
					outputbuffer.append("<span style=\"color:red\">Die Empfangsbasis existiert nicht!</span><br />\n");
					return true;
				}
				
				targetLoc = Location.fromResult(jmptarget);
				nodetarget = targetLoc.toString();
			}
			
			datan.put("systemout", targetLoc.getSystem());
			datan.put("xout", targetLoc.getX());
			datan.put("yout", targetLoc.getY());
				
			if( nodeID == ship.getInt("id") ) {
				outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen nicht mit dem "+nodetypename+" durch sich selbst springen</span><br />\n");
				return true;
			}
			
			/* 
			 * Ermittlung der Sprungberechtigten
			 */
			String[] jmpnodeuser = StringUtils.split(target[2], ':'); // Format art:ids aufgespalten
			
			if( jmpnodeuser[0].equals("all") ) {
				// Keine Einschraenkungen
			}
			// die alte variante 
			else if( jmpnodeuser[0].equals("default") || jmpnodeuser[0].equals("ownally") ){
				if( ( (user.getAlly() != null) && (datan.getInt("ally") != user.getAlly().getId()) ) || 
					( user.getAlly() == null && (datan.getInt("owner") != user.getId()) ) ) {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - default</span><br />\n");
					return true;
				}
			}
			// user:$userid
			else if ( jmpnodeuser[0].equals("user") ){
				if( Integer.parseInt(jmpnodeuser[1]) != user.getId() )  {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - owner</span><br />\n");
					return true;
				}
			}
			// ally:$allyid
			else if ( jmpnodeuser[0].equals("ally") ){
				if( (user.getAlly() == null) || (Integer.parseInt(jmpnodeuser[1]) != user.getAlly().getId()) )  {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - ally</span><br />\n");
					return true;
				}
			}
			// group:userid1,userid2, ...,useridn
			else if ( jmpnodeuser[0].equals("group") ){
				Integer[] userlist = Common.explodeToInteger(",", jmpnodeuser[1]);
				if( !Common.inArray(user.getId(), userlist) )  {
					outputbuffer.append("<span style=\"color:red\">Sie k&ouml;nnen kein fremdes "+nodetypename+" benutzen - group</span><br />\n");
					return true;
				}
			}
		}
		
		Location shipLoc = Location.fromResult(ship);
		Location nodeLoc = Location.fromResult(datan);
			
		if( !shipLoc.sameSector(0, nodeLoc, 0) ) {
			outputbuffer.append("<span style=\"color:red\">Fehler: "+nodetypename+" befindet sich nicht im selben Sektor wie das Schiff</span><br />\n");
			return true;
		}

		if( ship.getInt("e") < 5 ) {
			outputbuffer.append("<span style=\"color:red\">Zuwenig Energie zum Springen</span><br />\n");
			return true;
		}
		
		if( ship.getInt("fleet") > 0 ) { 
			boolean result = fleetJump(ship, nodeID, knode);
			if( result ) {
				return true;
			}
		}
			
		outputbuffer.append(ship.getString("name")+" springt nach "+nodetarget+"<br />\n");
		db.update("UPDATE ships SET x=",datan.getInt("xout"),",y=",datan.getInt("yout"),",system=",datan.getInt("systemout"),",e=",ship.getInt("e")-5," WHERE id>0 AND id=",ship.getInt("id"));
		db.update("UPDATE ships SET x=",datan.getInt("xout"),",y=",datan.getInt("yout"),",system=",datan.getInt("systemout")," WHERE id>0 AND docked IN ('l ",ship.getInt("id"),"','",ship.getInt("id"),"')");
		recalculateShipStatus(ship.getInt("id"));
	
		return false;
	}
	
	/**
	 * Entfernt das angegebene Schiff aus der Datenbank
	 * @param shipid Die ID des Schiffes
	 */
	public static void destroy(int shipid) {
		Ship ship = (Ship)ContextMap.getContext().getDB().get(Ship.class, shipid);
		ship.destroy();
		MESSAGE.get().append(Ship.MESSAGE.getMessage());
	}
	
	/**
	 * Gibt die SQLResultRow als Schiffsobjekt zurueck
	 * @param row Die SQLResultRow
	 * @return Das Objekt
	 */
	public static Ship getAsObject(SQLResultRow row) {
		return (Ship)ContextMap.getContext().getDB().get(Ship.class, row.getInt("id"));
	}
	
	/**
	 * Generiert ein Truemmerteil mit Loot fuer das angegebene Schiff unter Beruecksichtigung desjenigen,
	 * der es zerstoert hat. Wenn fuer das Schiff kein Loot existiert oder keiner generiert wurde (Zufall spielt eine
	 * Rolle!), dann wird kein Truemmerteil erzeugt.
	 * @param shipid Die ID des Schiffes, fuer das Loot erzeugt werden soll
	 * @param destroyer Die ID des Spielers, der es zerstoert hat
	 */
	public static void generateLoot( int shipid, int destroyer ) {
		Ship ship = (Ship)ContextMap.getContext().getDB().get(Ship.class, shipid);
		ship.generateLoot(destroyer);
		MESSAGE.get().append(Ship.MESSAGE.getMessage());
	}
	
	/**
	 * Uebergibt ein Schiff an einen anderen Spieler. Gedockte/Gelandete Schiffe
	 * werden, falls moeglich, mituebergeben.
	 * @param user Der aktuelle Besitzer des Schiffs
	 * @param ship Das zu uebergebende Schiff
	 * @param newowner Der neue Besitzer des Schiffs
	 * @param testonly Soll nur getestet (<code>true</code>) oder wirklich uebergeben werden (<code>false</code>)
	 * @return <code>true</code>, falls ein Fehler bei der Uebergabe aufgetaucht ist (Uebergabe nicht erfolgreich)
	 */
	public static boolean consign( User user, SQLResultRow ship, User newowner, boolean testonly ) {
		boolean result = getAsObject(ship).consign(newowner, testonly);
		MESSAGE.get().append(Ship.MESSAGE.getMessage());
		
		return result;
	}
	
	/**
	 * Gibt den Positionstext unter Beruecksichtigung von Nebeleffekten zurueck.
	 * Dadurch kann der Positionstext teilweise unleserlich werden (gewuenschter Effekt) 
	 * @param system Die System-ID
	 * @param x Die X-Koordinate
	 * @param y Die Y-Koordinate
	 * @param noSystem Soll das System angezeigt werden?
	 * @return der Positionstext
	 */
	public static String getLocationText(int system, int x, int y, boolean noSystem) {
		int nebel = getNebula(new Location(system, x, y));
		
		StringBuilder text = new StringBuilder(8);
		if( !noSystem ) {
			text.append(system);
			text.append(":");
		}
		
		if( nebel == 3 ) {
			text.append(x / 10);
			text.append("x/");
			text.append(y / 10);
			text.append('x');
			
			return text.toString();
		}
		else if( (nebel == 4) || (nebel == 5) ) {
			text.append(":??/??");
			return text.toString();
		}
		text.append(x);
		text.append('/');
		text.append(y);
		return text.toString();
	}
	
	/**
	 * Gibt den Positionstext fuer die Position zurueck, an der sich das angegebene Schiff gerade befindet.
	 * Beruecksichtigt werden Nebeleffekten.
	 * Dadurch kann der Positionstext teilweise unleserlich werden (gewuenschter Effekt) 
	 * @param ship Das Schiff
	 * @param noSystem Soll die System-ID angezeigt werden?
	 * @return Der Positionstext
	 */
	public static String getLocationText(SQLResultRow ship, boolean noSystem) {
		return getLocationText(ship.getInt("system"), ship.getInt("x"), ship.getInt("y"), noSystem);
	}
	
	/**
	 * Gibt den Positionstext fuer die Position zurueck.
	 * Beruecksichtigt werden Nebeleffekten.
	 * Dadurch kann der Positionstext teilweise unleserlich werden (gewuenschter Effekt) 
	 * @param loc Die Position
	 * @param noSystem Soll die System-ID angezeigt werden?
	 * @return Der Positionstext
	 */
	public static String getLocationText(Location loc, boolean noSystem) {
		return getLocationText(loc.getSystem(), loc.getX(), loc.getY(), noSystem);
	}

	/**
	 * Cachet den angegebenen Nebel
	 * @param nebel Der zu cachende Nebel
	 */
	public static void cacheNebula( SQLResultRow nebel ) {	
		Ships.nebel.put(new Location(nebel.getInt("system"), nebel.getInt("x"), nebel.getInt("y")), nebel.getInt("type"));
	}
	
	/**
	 * Gibt den Nebeltyp an der Position zurueck, an der sich das Schiff gerade befindet
	 * @param ship Das Schiff
	 * @return Der Typ des Nebels. <code>-1</code>, falls an der Stelle kein Nebel ist
	 */
	public static int getNebula(SQLResultRow ship) {
		return getNebula(new Location(ship.getInt("system"), ship.getInt("x"), ship.getInt("y")));
	}
		
	/**
	 * Gibt den Nebeltyp an der angegebenen Position zurueck. Sollte sich an der Position kein
	 * Nebel befinden, wird <code>-1</code> zurueckgegeben.
	 * @param loc Die Position
	 * @return Der Nebeltyp oder <code>-1</code>
	 */
	public static synchronized int getNebula(Location loc) {
		if( !nebel.containsKey(loc) ) {
			Database db = ContextMap.getContext().getDatabase();
			
			SQLResultRow neb = db.prepare("SELECT * FROM nebel WHERE system= ? AND x= ? AND y= ? ").
				first(loc.getSystem(), loc.getX(), loc.getY());
			if( neb.isEmpty() ) {
				nebel.put(loc, -1);	
			}
			else {
				nebel.put(loc, neb.getInt("type"));	
			}
		}
		return nebel.get(loc);
	}
	
	private static Map<Integer,Integer> fleetCountList = Collections.synchronizedMap(new CacheMap<Integer,Integer>(500));
	
	/**
	 * Entfernt das Schiff aus der Flotte. 
	 * @param ship Die SQL-Ergebniszeile des Schiffs
	 */
	public static void removeFromFleet( SQLResultRow ship ) {
		getAsObject(ship).removeFromFleet();
		MESSAGE.get().append(Ship.MESSAGE.getMessage());
	}

	public static boolean dock(DockMode mode, int user, int shipid, int[] shiplist) {
		Ship ship = (Ship)ContextMap.getContext().getDB().get(Ship.class, shipid);
		
		Ship[] dockships = new Ship[shiplist.length];
		for( int i=0; i < shiplist.length; i++ ) {
			dockships[i] = (Ship)ContextMap.getContext().getDB().get(Ship.class, shiplist[i]);
		}
		
		boolean result = ship.dock(mode, dockships);
		MESSAGE.get().append(Ship.MESSAGE.getMessage());
		return result;
	}
}
