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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Scriptfunktionen fuer Schiffsaktionsscripte
 * @author Christopher Jung
 *
 */
public class ActionFunctions {
	void registerFunctions(ScriptParser parser) {
		parser.registerCommand( "SHIPMOVE", new ShipMove(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "WAIT", new Wait(), ScriptParser.Args.PLAIN_VARIABLE );
		//parser.registerCommand( "NJUMP", new NJump(), ScriptParser.Args.PLAIN_REG );
		//parser.registerCommand( "KJUMP", new KJump(), ScriptParser.Args.PLAIN_REG );
		//parser.registerCommand( "MSG", new Msg(), ScriptParser.Args.PLAIN_REG,ScriptParser.Args.REG,ScriptParser.Args.REG );
		//parser.registerCommand( "START", new Start(), ScriptParser.Args.PLAIN_VARIABLE );
		//parser.registerCommand( "UNDOCK", new Undock(), ScriptParser.Args.PLAIN_VARIABLE );
		//parser.registerCommand( "LAND", new Land(), ScriptParser.Args.PLAIN_VARIABLE );
		//parser.registerCommand( "DOCK", new Dock(), ScriptParser.Args.PLAIN_VARIABLE );
		//parser.registerCommand( "GETRESOURCE", new GetResource(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		//parser.registerCommand( "TRANSFERCARGO", new TransferCargo(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		//parser.registerCommand( "ATTACK", new Attack(), ScriptParser.Args.PLAIN_REG );
		//parser.registerCommand( "RESETSCRIPT", new ResetScript(), ScriptParser.Args.PLAIN_VARIABLE );
		//parser.registerCommand( "EXECUTETASK", new ExecuteTask(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		//parser.registerCommand( "GETSHIPOWNER", new GetShipOwner(), ScriptParser.Args.PLAIN_REG);
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
			
			String cmd = command[1];
			
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
				
				scriptparser.log("Warte auf Tick $waittick - aktuell: "+tick+"\n\n");
				
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
}
