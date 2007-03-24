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
package net.driftingsouls.ds2.server.scripting;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

/**
 * Scriptfunktionen fuer Schiffsaktionsscripte
 * @author Christopher Jung
 *
 */
public class ActionFunctions {
	void registerFunctions(ScriptParser parser) {
		parser.registerCommand( "SHIPMOVE", new ShipMove(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "WAIT", new Wait(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "NJUMP", new NJump(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "KJUMP", new KJump(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "MSG", new Msg(), ScriptParser.Args.PLAIN_REG,ScriptParser.Args.REG,ScriptParser.Args.REG );
		parser.registerCommand( "START", new Dock(Ships.DockMode.START, true), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "UNDOCK", new Dock(Ships.DockMode.UNDOCK, true), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "LAND", new Dock(Ships.DockMode.LAND, false), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "DOCK", new Dock(Ships.DockMode.DOCK, false), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "GETRESOURCE", new GetResource(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "TRANSFERCARGO", new TransferCargo(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ATTACK", new Attack(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "RESETSCRIPT", new ResetScript(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "EXECUTETASK", new ExecuteTask(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETSHIPOWNER", new GetShipOwner(), ScriptParser.Args.PLAIN_REG);
	}
	
	class ShipMove implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SQLResultRow ship = scriptparser.getShip();
			
			Location target = Location.fromString(command[1]);
			scriptparser.log(target+"\n");
			
			int maxcount = Integer.MAX_VALUE;
			if( (command.length > 2) && (command[2] != null) && command[2].length() > 0 ) {
				maxcount = Integer.parseInt(command[2]);
			}
			scriptparser.log("maxcount: "+maxcount+"\n");
						
			SQLResultRow curpos = db.first("SELECT x,y,system,s FROM ships WHERE id=",ship.getInt("id"));
						
			int deltax = target.getX()-curpos.getInt("x");
			int deltay = target.getY()-curpos.getInt("y");
						
			if( (deltax == 0) && (deltay == 0) ) {
				scriptparser.log("Zielposition bereits erreicht!\n\n");
				return CONTINUE;
			}
						
			if( curpos.getInt("s") > 100 ) {
				scriptparser.log("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return STOP;
			}
					
			int direction = -1;
			int count = 0;
			boolean wait = false;
			while( true ) {
				int newdirection = 5;
				if( deltax > 0 ) {
					newdirection += 1;
				}
				else if( deltax < 0 ) {
					newdirection -= 1;
				}
							
				if( deltay > 0 ) {
					newdirection += 3;
				}
				else if( deltay < 0 ) {
					newdirection -= 3;
				}
							
				if( ((direction != -1) && (direction != newdirection)) || (maxcount == 0) ) {
					boolean result = Ships.move(ship.getInt("id"), direction, count, true, false); 
					scriptparser.log(Common._stripHTML(Ships.MESSAGE.getMessage()));
								
					if( result ) {
						wait = true;
						break;
					}
		
					if( newdirection == 5 ) {
						break;
					}
					count = 1;
					if( maxcount > 0 ) {
						maxcount--;	
					} 
					else {
						wait = true;
						break;	
					}
					direction = newdirection;
				} 
				else {
					count++;
					if( maxcount > 0 ) {
						maxcount--;	
					}
					else {
						count--;
						if( count == 0 ) {
							wait = true;
							break;
						}	
					}
					direction = newdirection;
				}
				int xOffset = 0;
				int yOffset = 0;
				
				if( direction == 1 ) { xOffset--; yOffset--;}
				else if( direction == 2 ) { yOffset--;}
				else if( direction == 3 ) { xOffset++; yOffset--;}
				else if( direction == 4 ) { xOffset--;}
				else if( direction == 6 ) { xOffset++;}
				else if( direction == 7 ) { xOffset--; yOffset++;}
				else if( direction == 8 ) { yOffset++;}
				else if( direction == 9 ) { xOffset++; yOffset++;}
				
				curpos.put("x", curpos.getInt("x")+xOffset);
				curpos.put("y", curpos.getInt("y")+yOffset);
					
				deltax = target.getX()-curpos.getInt("x");
				deltay = target.getY()-curpos.getInt("y");
			}
			ship = db.first("SELECT * FROM ships WHERE id=",ship.getInt("id"));
			scriptparser.setShip(ship);
			
			if( wait ) {
				scriptparser.log("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return STOP;
			}
			if( Math.abs(deltax)+Math.abs(deltay) == 0 ) {
				scriptparser.log("\n");
				return CONTINUE;
			}
			
			return CONTINUE;
		}
	}
	
	class Wait implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SQLResultRow ship = scriptparser.getShip();
			
			String cmd = (command.length > 1 ? command[1] : "");
			
			if( cmd.equals("shipInRange") ) {	
				int shipid = Integer.parseInt(command[2]);
				int range = 0;
				if( (command.length > 3) && (command[3] != null) && (command[3].length() > 0) ) {
					range = Integer.parseInt(command[3]);
				}

				scriptparser.log("Warte auf Schiff "+shipid+" im Umkreis von "+range+" Feldern\n\n");
				
				SQLResultRow shipdata = db.first("SELECT x,y,system FROM ships WHERE id>0 AND id=",ship.getInt("id"));
				Location shipLoc = Location.fromResult(shipdata);
				
				SQLResultRow result = db.first("SELECT id,x,y,system FROM ships WHERE id>0 AND id=",shipid," AND system=",shipdata.getInt("system"));
				
				if( !result.isEmpty() && shipLoc.sameSector(range, Location.fromResult(result), 0) ) {
					return CONTINUE;
				}
				return STOP;
			}
			else if( cmd.equals("tick") ) {
				int tick = ContextMap.getContext().get(ContextCommon.class).getTick();
				int waittick = Integer.parseInt(command[2]);
				
				scriptparser.log("Warte auf Tick "+waittick+" - aktuell: "+tick+"\n\n");
				
				if( tick < waittick ) {
					return STOP;
				}
				return CONTINUE;
			}
			else {
				scriptparser.log("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
			}
			
			return STOP_AND_INC;
		}
	}
	
	class KJump implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SQLResultRow ship = scriptparser.getShip();
			
			int nodeid = Integer.parseInt(command[1]);
			scriptparser.log("knodeid: "+nodeid+"\n"); 
			boolean result = Ships.jump(ship.getInt("id"), nodeid, true); 
			scriptparser.log(Common._stripHTML(Ships.MESSAGE.getMessage()));
			if( result ) {
				scriptparser.log("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return STOP;
			}					
			
			ship = db.first("SELECT * FROM ships WHERE id=",ship.getInt("id"));
			scriptparser.setShip(ship);
			
			scriptparser.log("\n");
			
			return CONTINUE;
		}
	}
	
	class NJump implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SQLResultRow ship = scriptparser.getShip();
			
			int nodeid = Integer.parseInt(command[1]);
			scriptparser.log("nodeid: "+nodeid+"\n"); 
			boolean result = Ships.jump(ship.getInt("id"), nodeid, false); 
			scriptparser.log(Common._stripHTML(Ships.MESSAGE.getMessage()));
			if( result ) {
				scriptparser.log("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return STOP;
			}					
			
			ship = db.first("SELECT * FROM ships WHERE id=",ship.getInt("id"));
			scriptparser.setShip(ship);
			
			scriptparser.log("\n");
			
			return CONTINUE;
		}
	}
	
	class Msg implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int to = Integer.parseInt(command[1]);
			
			scriptparser.log("receiver: "+to+"\n");
			
			String title = command[2];
			scriptparser.log("title: "+title+"\n");  
			
			String msg = command[3];
			scriptparser.log("msg: "+msg+"\n\n");
			
			SQLResultRow ship = scriptparser.getShip();
			
			PM.send( ContextMap.getContext(), ship.getInt("owner"), to, title, msg );
			
			return CONTINUE;
		}
	}
	
	class Dock implements SPFunction {
		private boolean allowAll = false;
		private Ships.DockMode mode = Ships.DockMode.DOCK;
		
		/**
		 * Erstellt eine Scriptfunktion als Wrapper um {@link net.driftingsouls.ds2.server.ships.Ships#dock(net.driftingsouls.ds2.server.ships.Ships.DockMode, int, int, int[])}
		 * @param mode Der Dock-Modus
		 * @param allowAll Soll <code>all</code> (alle gedockten Schiffe) zugelassen werden?
		 */
		public Dock(Ships.DockMode mode, boolean allowAll) {
			this.allowAll = allowAll;
			this.mode = mode;
		}
		
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SQLResultRow ship = scriptparser.getShip();
			
			int[] dockids = null;
			if( !allowAll || !command[1].equals("all") ) {		
				dockids = new int[command.length-1];
				for( int i=1; i < command.length; i++ ) { 
					dockids[i-1] = Integer.parseInt(command[i]);
				}
			}
					
			boolean result = Ships.dock(mode, ship.getInt("owner"), ship.getInt("id"), dockids); 
			
			scriptparser.log(Common._stripHTML(Ships.MESSAGE.getMessage()));
			if( result ) {
				scriptparser.log("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return STOP;
			}					
			
			return CONTINUE;
		}
	}
	
	class GetResource implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int shipid = Integer.parseInt(command[1]);
			scriptparser.log("shipid: "+shipid+"\n");
			
			String resid = command[2];
			scriptparser.log("resid: "+resid+"\n");
			
			SQLResultRow ship = scriptparser.getShip();
			if( shipid > 0 ) {
				int owner = ship.getInt("owner");
				ship = db.first("SELECT id,cargo FROM ships WHERE id=",shipid," AND owner=",owner);
				
				if( ship.isEmpty() ) {
					scriptparser.log("FEHLER: Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht diesem Spieler\n");	
				}
			}
			else if( !ship.containsKey("cargo") ) {
				ship.put("cargo", db.first("SELECT cargo FROM ships WHERE id=",ship.getInt("id")).getString("cargo"));	
			}
			Cargo cargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
			long rescount = cargo.getResourceCount(Resources.fromString(resid));
			
			scriptparser.setRegister("A",Long.toString(rescount));
			
			return CONTINUE;
		}
	}
	
	class TransferCargo implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int shipid1 = Integer.parseInt(command[1]);
			scriptparser.log("shipid1: "+shipid1+"\n");
			
			String way = command[2].toLowerCase();
			scriptparser.log("way: "+way+"\n");
			
			int shipid2 = Integer.parseInt(command[3]);
			scriptparser.log("shipid2: "+shipid2+"\n");
			
			ResourceID resid = Resources.fromString(command[4]);
			scriptparser.log("resid: "+resid+"\n");
			
			long count = Long.parseLong(command[5]);
			scriptparser.log("count: "+count+"\n");
			
			SQLResultRow ship1 = scriptparser.getShip();
			
			if( shipid1 > 0 ) {
				SQLResultRow ship = scriptparser.getShip();
				int owner = ship.getInt("owner");
				ship1 = db.first("SELECT id,cargo,owner,type,status FROM ships WHERE id=",shipid1," AND owner=",owner);
				if( ship1.isEmpty() ) {
					scriptparser.log("FEHLER: Das angegebene Schiff (Schiff1) existiert nicht oder geh&ouml;rt nicht diesem Spieler\n");
					return CONTINUE;	
				}
			}
			
			SQLResultRow ship2 = db.first("SELECT id,cargo,type,status FROM ships WHERE id=",shipid2," AND owner=",ship1.getInt("owner"));
			if( ship2.isEmpty() ) {
				scriptparser.log("FEHLER: Das angegebene Schiff (Schiff2) existiert nicht oder geh&ouml;rt nicht diesem Spieler\n");
				return CONTINUE;	
			}
			
			if( !way.equals("from") && !way.equals("to") ) {
				scriptparser.log("FEHLER: Der angegebene Transfermodus &gt;"+way+"&lt; existiert nicht\n");
				return CONTINUE;
			}
			
			if( way.equals("from") ) {
				SQLResultRow tmps = ship1;	
				ship1 = ship2;
				ship2 = tmps;
			}
			
			Cargo cargo1 = new Cargo( Cargo.Type.STRING, ship1.getString("cargo") );
			Cargo cargo2 = new Cargo( Cargo.Type.STRING, ship2.getString("cargo") );
			
			if( count < 0 ) {
				count = cargo1.getResourceCount( resid ) - count;	
			}
			
			if( count > cargo1.getResourceCount( resid ) ) {
				count = cargo1.getResourceCount( resid );
			}
			
			SQLResultRow shiptype2 = ShipTypes.getShipType(ship2);
			if( count > shiptype2.getLong("cargo") - cargo2.getMass() ) {
				count = shiptype2.getLong("cargo") - cargo2.getMass();
			}
			
			scriptparser.log("Transferiere "+count+"x"+resid+"\n");
			
			if( count > 0 ) {
				cargo2.addResource(resid, count);
				cargo1.substractResource(resid, count);
			
				db.tBegin();
				db.tUpdate(1, "UPDATE ships SET cargo='",cargo1.save(),"' WHERE id=",ship1.getInt("id")," AND cargo='",cargo1.save(true),"'");
				db.tUpdate(1, "UPDATE ships SET cargo='",cargo2.save(),"' WHERE id=",ship2.getInt("id")," AND cargo='",cargo2.save(true),"'");
				if( !db.tCommit() ) {
					scriptparser.log("FEHLER: Transaktion fehlgeschlagen\n");	
				}
				
				SQLResultRow ship = db.first("SELECT * FROM ships WHERE id=",ship1.getInt("id"));
				scriptparser.setShip(ship);
			}	
			
			return CONTINUE;
		}
	}
	
	class Attack implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int playerid = Integer.parseInt(command[1]);
			scriptparser.log("playerid: "+playerid+"\n");
			
			SQLResultRow ship = scriptparser.getShip();
			SQLResultRow aship = db.first("SELECT id FROM ships WHERE owner=",playerid," AND system=",ship.getInt("system")," AND x=",ship.getInt("x")," AND y=",ship.getInt("y")," AND battle=0");
			
			if( aship.isEmpty() ) {
				scriptparser.log("Kein passendes Schiff gefunden. Ausfuehrung bis zum naechsten Tick angehalten\n");
				return STOP;	
			}
			
			Battle battle = new Battle();
			battle.create(ship.getInt("owner"), ship.getInt("id"), aship.getInt("id"));
			
			ship.put("battle", battle.getID());
			scriptparser.setShip(ship);
			
			return CONTINUE;
		}
	}
	
	class ResetScript implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SQLResultRow ship = scriptparser.getShip();
			
			db.query("UPDATE ships SET script=NULL,scriptexedata=NULL WHERE id=",ship.getInt("id")," LIMIT 1");
			ship.put("script", "");
			ship.put("scriptexedata", "");
			
			scriptparser.setShip(ship);
			
			return CONTINUE;
		}
	}
	
	class ExecuteTask implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String taskid = command[1];
			scriptparser.log("taskid: "+taskid+"\n");
			
			String cmd = command[2];
			scriptparser.log("cmd: spa_"+cmd+"\n");
			
			Taskmanager taskmanager = Taskmanager.getInstance();
			taskmanager.handleTask(taskid, "spa_"+cmd);
			
			return CONTINUE;
		}
	}
	
	class GetShipOwner implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int shipid = Integer.parseInt(command[1]);
			scriptparser.log("shipid: "+shipid+"\n");
			
			SQLResultRow ship = scriptparser.getShip();
			SQLResultRow ashipowner = db.first("SELECT owner FROM ships WHERE id="+shipid+" AND system="+ship.getInt("system")+" AND x="+ship.getInt("x")+" AND y="+ship.getInt("y"));
			
			scriptparser.setRegister("A",ashipowner.isEmpty() ? "0" : Integer.toString(ashipowner.getInt("owner")));
			
			return CONTINUE;
		}
	}
}
