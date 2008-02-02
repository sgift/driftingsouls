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

import java.sql.Blob;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.SectorTemplateManager;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.Waypoint;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * Scriptbefehle fuer Questscripte
 * @author Christopher Jung
 *
 */
public class QuestFunctions {
	void registerFunctions(ScriptParser parser) {
		// Questfunktionen
		parser.registerCommand( "LOADDIALOG", new LoadDialog(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "INITDIALOG", new InitDialog(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "ADDANSWER", new AddAnswer(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG_VARIABLE );
		parser.registerCommand( "SETANSWERURL", new SetAnswerURL(), ScriptParser.Args.PLAIN, ScriptParser.Args.REG );
		parser.registerCommand( "COPYVAR", new CopyVar(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "SAVEVAR", new SaveVar(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "SETDIALOGTEXTVAR", new SetDialogTextVar(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "INITQUEST", new InitQuest(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ENDQUEST", new EndQuest(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "GETQUESTID", new GetQuestID(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "INSTALLHANDLER", new InstallHandler(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "REMOVEHANDLER", new RemoveHandler(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "ADDUNINSTALLCMD", new AddUninstallCmd(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "COMPLETEQUEST", new CompleteQuest(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "HASQUESTCOMPLETED", new HasQuestCompleted(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "SETQUESTUISTATUS", new SetQuestUIStatus(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "SAVEOUTPUT", new SaveOutput(), ScriptParser.Args.PLAIN_VARIABLE );
		parser.registerCommand( "LOADQUESTCONTEXT", new LoadQuestContext(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "SAVEQUESTCONTEXT", new SaveQuestContext(), ScriptParser.Args.PLAIN_REG );
		
		// Cargofunktionen
		parser.registerCommand( "ADDQUESTITEM", new AddQuestItem(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "HASQUESTITEM", new HasQuestItem(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ADDITEM", new AddItem(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "HASRESOURCE", new HasResource(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETRESOURCE", new GetResource(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ADDRESOURCE", new AddResource(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "TRANSFERWHOLECARGO", new TransferWholeCargo(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN );
		
		// Schiffsfunktionen
		parser.registerCommand( "LOCKSHIP", new LockShip(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN );
		parser.registerCommand( "UNLOCKSHIP", new UnlockShip(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN );
		parser.registerCommand( "ADDQUESTSHIPS", new AddQuestShips(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ADDSHIPS", new AddShips(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "REMOVESHIP", new RemoveShip(), ScriptParser.Args.PLAIN_REG);
		parser.registerCommand( "MOVESHIP", new MoveShip(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ISSHIPDESTROYED", new IsShipDestroyed(), ScriptParser.Args.PLAIN_REG);
		parser.registerCommand( "ADDLOOTTABLE", new AddLootTable(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "DELETELOOTTABLE", new DeleteLootTable(), ScriptParser.Args.PLAIN_REG);
		
		// Diverses
		parser.registerCommand( "MSG", new Msg(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.REG, ScriptParser.Args.REG );
		parser.registerCommand( "ADDMONEY", new AddMoney(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETMONEY", new GetMoney(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "CLONEOFFIZIER", new CloneOffizier(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "REMOVEOFFIZIER", new RemoveOffizier(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "STARTBATTLE", new StartBattle(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ADDBATTLEVISIBILITY", new AddBattleVisibility(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ENDBATTLE", new EndBattle(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETNOOBSTATUS", new GetNoobStatus(), ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETUSERVALUE", new GetUserValue(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "SETUSERVALUE", new SetUserValue(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETSECTORPROPERTY", new GetSectorProperty(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETSYSTEMPROPERTY", new GetSystemProperty(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GTUAUCTIONSHIP", new GtuAuctionShip(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GTUAUCTIONCARGO", new GtuAuctionCargo(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		
		// QuickQuests
		parser.registerCommand( "GENERATEQUICKQUESTSOURCEMENU", new GenerateQuickQuestSourceMenu(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GENERATEQUICKQUESTTARGETMENU", new GenerateQuickQuestTargetMenu(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "HANDLEQUICKQUESTEVENT", new HandleQuickQuestEvent(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
	}
	
	/*—---------------------------------------------
	 * 
	 * 	Questfunktionen
	 *
	 ----------------------------------------------*/
	
	class Answer {
		String url;
		String text;
		
		Answer(String text, String url) {
			this.text = text;
			this.url = url;
		}
	}
	
	// Diese Daten sind bei jeder ScriptParser-Instanz individuell!
	String dialogText = "";
	String dialogImage = "";
	Map<String,Answer> dialogAnswers = new LinkedHashMap<String,Answer>();
	
	class LoadDialog implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SQLResultRow dialogdata = null;
			
			if( NumberUtils.isNumber(command[1]) ) {
				int textid = Integer.parseInt(command[1]);
				scriptparser.log("dialog: "+textid+"\n");
			
				dialogdata = db.first( "SELECT text,picture FROM quests_text WHERE id=",textid);
			}
			else {
				dialogdata = new SQLResultRow();
				dialogdata.put("text", command[1]);
				dialogdata.put("picture", command[2]);

				scriptparser.log("dialog: Parameter1/2\n");
			}
			dialogText = dialogdata.getString("text");
			dialogImage = dialogdata.getString("picture");
			
			return CONTINUE;
		}
	}
	
	class InitDialog implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			BBCodeParser bbcodeparser = BBCodeParser.getInstance();
			
			String text = bbcodeparser.parse(dialogText);
			
			ScriptParserContext context = scriptparser.getContext();
			context.out( "<table class=\"noBorderX\"><tr><td class=\"noBorderX\" valign=\"top\">" );
			context.out( "<img src=\""+Configuration.getSetting("URL")+"data/quests/"+dialogImage+"\" alt=\"\" />" );
			context.out( "</td><td class=\"noBorderX\" valign=\"top\">" );
			context.out( StringUtils.replace(text, "\n", "<br />")+"<br /><br />" );
			
			for( Answer answer : dialogAnswers.values() ) {	
				context.out( "<a class=\"forschinfo\" href=\""+answer.url+"\">"+answer.text+"</a><br />" );
			}
			context.out( "</td></tr></table>" );
			
			return CONTINUE;
		}
	}
	
	class CopyVar implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("register: "+command[1]+"\n");
			scriptparser.log("value: "+command[2]+"\n");
			if( command.length > 3 ) {
				scriptparser.log("reg4value: "+command[3]+"\n");	
			}
			
			Object val = null;
			
			String[] value = StringUtils.split(command[2], '.');
			if( value[0].equals("shipsource") ) {
				SQLResultRow ship = null;
				if( command.length == 3 ) {
					ship = scriptparser.getShip();
				}
				else if( scriptparser.getRegisterObject(command[3]) instanceof SQLResultRow ) {
					ship = (SQLResultRow)scriptparser.getRegisterObject(command[3]);
				}
				else {
					int shipID = Integer.parseInt(scriptparser.getRegister(command[3]));
					ship = db.first("SELECT * FROM ships WHERE id>0 AND id=",shipID);
				}
				
				if( (value.length <= 1) || (value[1].length() == 0) ) {
					val = ship;
				}
				if( value[1].equals("cargo") ) {
					val = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
				}
				else if( value[1].equals("offizier") ) {
					val = Offizier.getOffizierByDest('s', ship.getInt("id"));
				}
				else if( value[1].equals("flagschiff") ) {
					User owner = (User)ContextMap.getContext().getDB().get(User.class, ship.getInt("owner"));
					UserFlagschiffLocation flagschiff = owner.getFlagschiff();
					val = (flagschiff != null) && (flagschiff.getID() == ship.getInt("id"));
				}
				else {
					val = ship.get(value[1].trim());	
				}
			}	
			else if( value[0].equals("sessionid") ) {
				val = ContextMap.getContext().getSession();
			}
			else if( value[0].equals("tick") ) {
				val = ContextMap.getContext().get(ContextCommon.class).getTick();
			}
			else if( value[0].equals("offizier") ) {
				Offizier offi = (Offizier)scriptparser.getRegisterObject(command[3]);
				
				if( value[1].equals("name") ) {
					val = offi.getName();
				}
				else if( value[1].equals("id") ) {
					val = offi.getID();
				}
				else if( value[1].equals("rang") ) {
					val = offi.getRang();
				}
				else if( value[1].equals("dest") ) {
					val = offi.getDest();
				}
				else if( value[1].equals("owner") ) {
					val = offi.getOwner();
				}
				else if( value[1].equals("ability") ) {
					val = offi.getAbility(Offizier.Ability.valueOf(value[2]));
				}
				else if( value[1].equals("special") ) {
					val = offi.getSpecial().toString();
				}
			}
			else if( value[0].equals("base") ) {
				SQLResultRow base = null;
				
				Object baseObj = scriptparser.getRegisterObject(command[3]);
				if( baseObj instanceof SQLResultRow ) {
					base = (SQLResultRow)baseObj;
				}
				else {
					int baseID = ((Number)baseObj).intValue();
					base = db.first("SELECT id,name,owner,x,y,system,cargo,maxcargo,klasse FROM bases WHERE id=",baseID);
				}
				
				if( (value.length <= 1) || (value[1].length() == 0) ) {
					val = base;
				}
				else if( value[1].equals("cargo") ) {
					val = new Cargo( Cargo.Type.STRING, base.getString("cargo") );
				}
				else {
					val = base.get(value[1].trim());	
				}
			}	
			else if( value[0].equals("jumpnode") ) {
				SQLResultRow jn = null;
				
				Object jnObj = scriptparser.getRegisterObject(command[3]);
				if( jnObj instanceof SQLResultRow ) {
					jn = (SQLResultRow)jnObj;
				}
				else {
					int jnID = ((Number)jnObj).intValue();
					jn = db.first("SELECT id,name,owner,x,y,system,cargo,maxcargo,klasse FROM bases WHERE id=",jnID);
				}
								
				if( (value.length <= 1) || (value[1].length() == 0) ) {
					val = jn;
				}
				else {
					val = jn.get(value[1].trim());	
				}
			}
			
			if( val instanceof Boolean ) {
				val = (val == Boolean.TRUE ? 1 : 0);
			}
			scriptparser.setRegister(command[1],val);
			
			return CONTINUE;
		}
	}
	
	class AddAnswer implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String answerid = command[1];
			String internalid = command[2];
			
			scriptparser.log( "answerid: "+answerid+"\n" );
			scriptparser.log( "internalid: "+internalid+"\n" );
			
			List<String> addParamList = new ArrayList<String>();
			for( int i=3; i < command.length; i++ ) {
				if( command[i].trim().length() > 0 ) {
					addParamList.add(command[i]);
					scriptparser.log( "addparam: "+command[i]+"\n" );
				}
			}
		
			String addparams = Common.implode(":",addParamList);	
		
			if( answerid.charAt(0) == '#' ) {
				answerid = scriptparser.getRegister(answerid);
			}
			
			String answer = answerid;
			
			if( NumberUtils.isNumber(answerid) ) {
				int answerIdNumber = Integer.parseInt( answerid );
				SQLResultRow answerRow = db.first("SELECT text FROM quests_answers WHERE id=",answerIdNumber);
				if( answerRow.isEmpty() ) {
					answer = "Unbekannte Antwort &gt;"+answerid+"&lt;";
					scriptparser.log("ERROR: Unbekannte Antwort &gt;"+answerid+"&lt;\n");
				}
				else {
					answer = answerRow.getString("text");
				}
			}
		
			if( addparams.length() > 0 ) {
				internalid += ":"+addparams;
			}
			
			String url = Quests.buildQuestURL( internalid );
			
			dialogAnswers.put(answerid, new Answer(answer, url));
			
			return CONTINUE;
		}
	}
	
	class SetAnswerURL implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String internalid = command[1];
			String answerurl = command[2];
			
			scriptparser.log( "internalid: "+internalid+"\n" );
			scriptparser.log( "answerurl: "+answerurl+"\n" );
			
			if( dialogAnswers.containsKey(internalid) ) {
				dialogAnswers.get(internalid).url = answerurl;
			}
			
			return CONTINUE;
		}
	}
	
	class SaveVar implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("value: "+command[1]+"\n");
			scriptparser.log("register: "+command[2]+"\n");
			Object val = scriptparser.getRegisterObject(command[2]);
			if( (command.length > 3) && (command[3].length() > 0) ) {
				scriptparser.log("reg4value: "+command[3]+"\n");	
			}
			
			String[] value = StringUtils.split(command[1], '.');
			if( value[0].equals("shipsource") ) {
				SQLResultRow ship = scriptparser.getShip();
				if( (command.length > 3) && (command[3].length() > 0) ) {
					Object shipObj = scriptparser.getRegister(command[3]);
					if( !(shipObj instanceof SQLResultRow) ) {
						ship = db.first("SELECT * FROM ships WHERE id>0 AND id="+Value.Int(shipObj.toString()));
					}
					else {
						ship = (SQLResultRow)shipObj;
					}
				}
				
				if( !value[1].equals("cargo") ) {
					ship.put(value[1].trim(), val);
					db.prepare("UPDATE ships SET `?`= ? WHERE id>0 AND id= ?")
						.update(value[1].trim(), val, ship.getInt("id"));
				}
				else {
					ship.put("cargo", ((Cargo)val).save());
					db.update("UPDATE ships SET `cargo`='"+ship.getString("cargo")+"' WHERE id>0 AND id="+ship.getInt("id"));
				}
				
				if( (command.length == 3) || (command[3].length() == 0) ) {
					scriptparser.setShip( ship );
				}
			}	
			else if( value[0].equals("sessionid") ) {
				// Kein speichern von sessionids moeglich
				scriptparser.log("Speichern der Sessionid nicht moeglich\n");
			}
			else if( value[0].equals("base") ) {
				Object baseObj = scriptparser.getRegisterObject(command[3]);
				if( !(baseObj instanceof SQLResultRow) ) {
					baseObj = db.first("SELECT * FROM bases WHERE id="+Value.Int(baseObj.toString()));
				}
				SQLResultRow base = (SQLResultRow)baseObj;
				
				if( !value[1].equals("cargo") ) {
					base.put(value[1].trim(), val);
					db.prepare("UPDATE bases SET `?`= ? WHERE id>0 AND id= ?")
						.update(value[1].trim(), val, base.getInt("id"));
				}
				else {
					base.put("cargo", ((Cargo)val).save());
					db.update("UPDATE base SET `cargo`='"+base.getString("cargo")+"' WHERE id>0 AND id="+base.getInt("id"));
				}
			}
			
			return CONTINUE;
		}
	}
	
	class SetDialogTextVar implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String myvar = command[1];
			String replace = command[2];
			
			scriptparser.log("var: "+myvar+"\n");
			scriptparser.log("replace: "+replace+"\n");
			
			dialogText = StringUtils.replace(dialogText, "{"+myvar+"}", replace );
			
			return CONTINUE;
		}
	}
	
	class InitQuest implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questid = command[1];
			int noreuse = 0;
			if( command.length > 2 ) {
				noreuse = Value.Int(command[2]);
			}
			
			scriptparser.log("questid: "+questid+"\n");
			
			
			int userid = Integer.parseInt(scriptparser.getRegister("USER"));
			
			Integer id = null;
			if( (noreuse == 0) && (questid.charAt(0) != 'r') ) {
				SQLResultRow idRow = db.first("SELECT id FROM quests_running WHERE questid=",questid," AND userid=",userid);
				if( !idRow.isEmpty() ) {
					id = idRow.getInt("id");
				}
			}
			else if( noreuse == 0 ) {
				String rquestid = questid.substring(1);
				SQLResultRow idRow = db.first("SELECT id FROM quests_running WHERE id=",rquestid);
				if( !idRow.isEmpty() ) {
					id = idRow.getInt("id");
				}
			}
			
			if( id == null ) {
				db.update("INSERT INTO quests_running (questid,userid) VALUES (",questid,",",userid,")");
				id = db.insertID();
			}
			
			scriptparser.setRegister("QUEST","r"+id);
			
			return CONTINUE;
		}
	}
	
	class EndQuest implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questid = scriptparser.getRegister("QUEST");
			int userid = Integer.parseInt(scriptparser.getRegister("USER"));
			
			SQLResultRow runningdata = null;
			if( questid.charAt(0) != 'r' ) {
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE questid=",questid," AND userid=",userid);
			}
			else {
				String rquestid = questid.substring(1);	
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE id=",rquestid);
			}
			
			// ggf. das Quest "deinstallieren" (handler entfernen)
			if( runningdata.getString("uninstall").length() > 0 ) {
				ScriptParserContext context = scriptparser.getContext();
				
				scriptparser.setContext(new ScriptParserContext());
				scriptparser.executeScript( db, runningdata.getString("uninstall"), "0" );
				
				scriptparser.setContext(context);
			}
			
			db.update("DELETE FROM quests_running WHERE id="+runningdata.getInt("id"));
			
			scriptparser.setRegister("QUEST","");
			
			return CONTINUE;
		}
	}
	
	class GetQuestID implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String rquestid = command[1];
			
			scriptparser.log("rquestid: "+rquestid+"\n");
			
			rquestid = rquestid.substring(1);
			SQLResultRow qid = db.first("SELECT questid FROM quests_running WHERE id="+Value.Int(rquestid));
			
			if( qid.isEmpty() ) {
				scriptparser.log("rquestid ist ungueltig!\n");
				scriptparser.setRegister("A", "0");
			}
			else {
				scriptparser.setRegister("A",Integer.toString(qid.getInt("questid")));
			}
			
			return CONTINUE;
		}
	}
	
	class InstallHandler implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String event = command[1];
			scriptparser.log("event: "+event+"\n");
			
			StringBuilder removescript = new StringBuilder();
			String questid = "";
			int userid = 0;
			
			if( event.equals("oncommunicate") || event.equals("onendbattle") || event.equals("onenter") ) {
				// Object-ID
				String objid = command[2];
				if( objid.charAt(0) == '#' ) {
					objid = scriptparser.getRegister(objid);	
				}
				scriptparser.log("objid: "+objid+"\n");
				
				// User-ID
				if( command[3].charAt(0) == '#' ) {
					command[3] = scriptparser.getRegister(command[3]);	
				}
				userid = Value.Int(command[3]);
				
				scriptparser.log("userid: "+userid+"\n");
				
				// Script-ID
				if( command[4].charAt(0) == '#' ) {
					command[4] = scriptparser.getRegister(command[4]);	
				}
				int scriptid = Value.Int( command[4] );
				scriptparser.log("scriptid: "+scriptid+"\n");
				
				// Quest-ID
				if( command[5].charAt(0) == '#' ) {
					command[5] = scriptparser.getRegister(command[5]);	
				}
				questid = command[5];
				scriptparser.log("questid: "+questid+"\n");
			
				// On-Communicate
				if( event.equals("oncommunicate") ) {
					SQLResultRow ship = db.first("SELECT id,oncommunicate FROM ships " +
							"WHERE id>0 AND id="+Value.Int(objid));
					if( !ship.isEmpty() ) {
						String comm = ship.getString("oncommunicate");
						if( comm.length() > 0 ) {
							comm += ";";
						}
						comm += userid+":"+scriptid+":"+questid;
						db.prepare("UPDATE ships SET oncommunicate= ? WHERE id>0 AND id= ?")
							.update(comm, Value.Int(objid));
					
						removescript.append("!REMOVEHANDLER "+event+" "+objid+" "+userid+" "+scriptid+" "+questid+"\n");
					}
				}
				// On-Enter
				else if( event.equals("onenter") ) {
					Location loc = Location.fromString(objid);
					
					SQLResultRow sector = db.first("SELECT system,x,y,onenter FROM sectors " +
							"WHERE system="+loc.getSystem()+" AND x="+loc.getX()+" AND y="+loc.getY());
					if( sector.isEmpty() ) {
						db.update("INSERT INTO sectors (system,x,y) " +
								"VALUES ("+loc.getSystem()+","+loc.getX()+","+loc.getY()+")");
						sector.put("onenter", "");
					}
					
					String onenter = sector.getString("onenter");
					if( onenter.length() > 0) {
						onenter += ";";	
					}
					onenter += userid+":"+scriptid+":"+questid;
					
					db.prepare("UPDATE sectors SET onenter= ? WHERE system= ? AND x= ? AND y= ?")
						.update(onenter, loc.getSystem(), loc.getX(), loc.getY());
					
					removescript.append("!REMOVEHANDLER "+event+" "+objid+" "+userid+" "+scriptid+" "+questid+"\n");
				}
				// Battle-OnEnd
				else {
					db.prepare("UPDATE battles SET onend= ? WHERE id= ?")
						.update(userid+":"+scriptid+":"+questid, Value.Int("objid"));	
				}
			}
			// OnTick
			else if( event.equals("ontick") ) {
				// User-ID
				if( command[2].charAt(0) == '#' ) {
					command[2]= scriptparser.getRegister(command[2]);	
				}
				userid = Value.Int(command[2]);
				scriptparser.log("userid: "+userid+"\n");
				
				// Script-ID
				if( command[3].charAt(0) == '#' ) {
					command[3] = scriptparser.getRegister(command[3]);	
				}
				int scriptid = Value.Int( command[3] );
				scriptparser.log("scriptid: "+scriptid+"\n");
				
				// Quest-ID
				if( command[4].charAt(0) == '#' ) {
					command[4] = scriptparser.getRegister(command[4]);	
				}
				questid = command[4];
				scriptparser.log("questid: "+questid+"\n");
				
				db.prepare("UPDATE quests_running SET ontick= ? WHERE questid= ? AND userid= ?")
					.update(scriptid, questid, userid);
				
				removescript.append("!REMOVEHANDLER "+event+" "+userid+" "+questid+"\n");
			}
			
			if( removescript.length() > 0 ) {
				SQLResultRow runningdata = null;
				if( questid.charAt(0) != 'r' ) {
					runningdata = db.first("SELECT id,uninstall FROM quests_running " +
							"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
				}
				else {
					int rquestid = Value.Int(questid.substring(1));
					runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE id="+rquestid);
				}
				
				if( !runningdata.isEmpty() ) {
					String uninstall = runningdata.getString("uninstall");
					if( uninstall.length() == 0 ) {
						uninstall = ":0\n";	
					}
					uninstall += removescript;
					
					db.prepare("UPDATE quests_running SET uninstall= ? WHERE id= ?")
						.update(uninstall, runningdata.getInt("id"));
				}
			}
			
			return CONTINUE;
		}
	}
	
	class RemoveHandler implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String event = command[1];
			scriptparser.log("event: "+event+"\n");
			
			String questid = "";
			int userid = 0;
		
			if( event.equals("oncommunicate") || event.equals("onenter") ) {
				// Object-ID
				String objid = command[2];
				if( objid.charAt(0) == '#' ) {
					objid = scriptparser.getRegister(objid);	
				}
				scriptparser.log("objid: "+objid+"\n");
				
				// User-ID
				if( command[3].charAt(0) == '#' ) {
					command[3] = scriptparser.getRegister(command[3]);	
				}
				userid = Value.Int(command[3]);
				
				scriptparser.log("userid: "+userid+"\n");
				
				// Script-ID
				if( command[4].charAt(0) == '#' ) {
					command[4] = scriptparser.getRegister(command[4]);	
				}
				int scriptid = Value.Int( command[4] );
				scriptparser.log("scriptid: "+scriptid+"\n");
				
				// Quest-ID
				if( command[5].charAt(0) == '#' ) {
					command[5] = scriptparser.getRegister(command[5]);	
				}
				questid = command[5];
				scriptparser.log("questid: "+questid+"\n");
		
				if( event.equals("oncommunicate") ) {
					SQLResultRow ship = db.first("SELECT id,oncommunicate FROM ships " +
							"WHERE id>0 AND id="+Value.Int(objid));
					if( !ship.isEmpty()  ) {
						List<String> newcom = new ArrayList<String>();
					
						String[] com = StringUtils.split(ship.getString("oncommunicate"), ';');
						for( int i=0; i < com.length; i++ ) {
							String[] tmp = StringUtils.split(com[i], ':');
							int usr = Value.Int(tmp[0]);
							int script = Value.Int(tmp[1]);
							String quest = tmp[2];
							if( (usr != userid) || (script != scriptid) || !quest.equals(questid) ) {
								newcom.add(com[i]);	
							}	
						}
						
						if( newcom.size() > 0 ) {
							db.prepare("UPDATE ships SET oncommunicate=? WHERE id>0 AND id= ?")
								.update(Common.implode(";", newcom), Value.Int(objid));
						}
						else {
							db.update("UPDATE ships SET oncommunicate=NULL WHERE id>0 AND id="+Value.Int(objid));
						}
					}
				}
				else {
					Location loc = Location.fromString(objid);
					
					SQLResultRow sector = db.first("SELECT system,x,y,onenter FROM sectors " +
							"WHERE system="+loc.getSystem()+" AND x="+loc.getX()+" AND y="+loc.getY());
					if( !sector.isEmpty() ) {
						List<String> newenter = new ArrayList<String>();
						
						String[] enter = StringUtils.split(sector.getString("onenter"), ';');
						for( int i=0; i < enter.length; i++ ) {
							String[] tmp = StringUtils.split(enter[i], ':');
							int usr = Value.Int(tmp[0]);
							int script = Value.Int(tmp[1]);
							String quest = tmp[2];
							if( (usr != userid) || (script != scriptid) || !quest.equals(questid) ) {
								newenter.add(enter[i]);	
							}	
						}
						
						if( newenter.size() > 0 ) {
							db.prepare("UPDATE sectors SET onenter= ? WHERE system= ? AND x= ? AND y= ?")
								.update(Common.implode(";", newenter), loc.getSystem(), loc.getX(), loc.getY());
						}
						else if( sector.getInt("objects") != 0 ) {
							db.update("UPDATE ships SET onenter=NULL " +
									"WHERE system="+loc.getSystem()+" " +
											"AND x="+loc.getX()+" AND y="+loc.getY());
						}
						else {
							db.update("DELETE FROM sectors " +
									"WHERE system="+loc.getSystem()+" " +
									"AND x="+loc.getX()+" AND y="+loc.getY());
						}
					}
				}	
			}
			else if( event.equals("ontick") ) {
				// User-ID
				if( command[2].charAt(0) == '#' ) {
					command[2]= scriptparser.getRegister(command[2]);	
				}
				userid = Value.Int(command[2]);
				scriptparser.log("userid: "+userid+"\n");
							
				// Quest-ID
				if( command[3].charAt(0) == '#' ) {
					command[3] = scriptparser.getRegister(command[3]);	
				}
				questid = command[3];
				scriptparser.log("questid: "+questid+"\n");
				
				if( questid.charAt(0) != 'r' ) {
					db.update("UPDATE quests_running SET ontick=NULL " +
							"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
				}
				else {
					int rquestid = Value.Int(questid.substring(1));
					db.update("UPDATE quests_running SET ontick=NULL WHERE id="+rquestid);	
				}
			}
			
			return CONTINUE;
		}
	}
	
	class AddUninstallCmd implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));
			
			StringBuilder removescript = new StringBuilder();
			
			for( int i=1; i < command.length; i++ ) {
				String cmd = command[1];
				if( cmd.charAt(0) == '#' ) {
					removescript.append(scriptparser.getRegister(cmd)+' ');	
				}	
				else if( cmd.charAt(0) == '\\' ) {
					removescript.append(cmd.substring(1)+' ');
				}
				else {
					removescript.append(cmd+' ');	
				}
			}
			
			removescript.append("\n");
			
			SQLResultRow runningdata = null;
			if( questid.charAt(0) != 'r' ) {
				runningdata = db.first("SELECT id,uninstall FROM quests_running " +
						"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
			}
			else {
				int rquestid = Value.Int(questid.substring(1));
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE id="+rquestid);	
			}
			
			if( !runningdata.isEmpty() ) {
				String uninstall = runningdata.getString("uninstall");
				
				if( uninstall.length() == 0 ) {
					uninstall = ":0\n";	
				}
				uninstall += removescript.toString();
					
				db.prepare("UPDATE quests_running SET uninstall= ? WHERE id= ?")
					.update(uninstall, runningdata.getInt("id"));
			}
			else {
				scriptparser.log("WARNUNG: keine quest_running-data gefunden\n");	
			}
			
			return CONTINUE;
		}
	}
	
	class CompleteQuest implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questidStr = command[1];
			scriptparser.log("questid: "+questidStr+"\n");
			
			int questid = 0;
			if( questidStr.charAt(0) == 'r' ) {
				String rquestid = questidStr.substring(1);
				questid = db.first("SELECT questid FROM quests_running WHERE id="+Value.Int(rquestid)).getInt("questid");
			}
			else {
				questid = Value.Int(questidStr);
			}
			
			int userid = Value.Int(scriptparser.getRegister("USER"));
			
			SQLResultRow cqid = db.first("SELECT id FROM quests_completed WHERE questid="+questid+" AND userid="+userid);
			if( cqid.isEmpty() ) {	
				db.update("INSERT INTO quests_completed (questid,userid) VALUES ("+questid+","+userid+")");
			}
			
			return CONTINUE;
		}
	}
	
	class HasQuestCompleted implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questid = command[1];
			scriptparser.log("questid: "+questid+"\n");
			
			if( questid.charAt(0) == 'r' ) {
				int rquestid = Integer.parseInt(questid.substring(1));
				questid = Integer.toString(
						db.first("SELECT questid FROM quests_running " +
								"WHERE id="+rquestid).getInt("questid")
					);
			}
			
			int userid = Value.Int(scriptparser.getRegister("USER"));
		
			SQLResultRow id = db.first("SELECT id FROM quests_completed " +
					"WHERE userid="+userid+" AND questid="+questid);
		
			if( !id.isEmpty() ) {
				scriptparser.setRegister("cmp","1");
			}
			else {
				scriptparser.setRegister("cmp","0");	
			}
			
			return CONTINUE;
		}
	}
	
	class SetQuestUIStatus implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String statustext = "";
			if( command[1].charAt(0) == '#' ) {
				statustext = scriptparser.getRegister(command[1]);
			}

			scriptparser.log("statustext: "+statustext+"\n");
			
			int publish = Value.Int(command[2]);
			scriptparser.log("publish: "+publish+"\n");
			
			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));
		
			if( questid.charAt(0) != 'r' ) {
				db.prepare("UPDATE quests_running SET statustext= ? ,publish= ? WHERE userid= ? AND questid= ?")
					.update(statustext, publish, userid, Value.Int(questid));
			}
			else {
				String rquestid = questid.substring(1);
				db.prepare("UPDATE quests_running SET statustext= ? ,publish= ? WHERE id= ?")
					.update(statustext, publish, Value.Int(rquestid));	
			}
			
			return CONTINUE;
		}
	}
	
	class SaveOutput implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.setRegister("_OUTPUT",scriptparser.getContext().getOutput());
			
			return CONTINUE;
		}
	}
	
	class LoadQuestContext implements SPFunction, Loggable {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questid = command[1];
			scriptparser.log("QuestID: "+questid+"\n");
			
			SQLQuery questdata = null;
			if( questid.charAt(0) != 'r' ) {
				int user = Value.Int(scriptparser.getRegister("USER"));
				
				questdata = db.query("SELECT id,execdata " +
						"FROM quests_running " +
						"WHERE questid="+Integer.parseInt(questid)+" AND userid="+user);	
			}
			else {
				String rquestid = questid.substring(1);
				questdata = db.query("SELECT id,execdata " +
						"FROM quests_running " +
						"WHERE id="+Integer.parseInt(rquestid));
			}
			
			if( !questdata.next() ) {
				scriptparser.log("Warnung: Kein passendes laufendes Quest gefunden\n");
				scriptparser.setRegister("QUEST","0");
			}
			else {
				try {
					Blob blob = questdata.getBlob("execdata");
					scriptparser.getContext().addContextData(
							ScriptParserContext.fromStream(blob.getBinaryStream())
					);
					scriptparser.setRegister("QUEST","r"+questdata.getInt("id"));
				}
				catch( Exception e ) {
					scriptparser.log("Fehler: Konnte Questdaten nicht laden: "+e+"\n");
					scriptparser.setRegister("QUEST","r"+questdata.getInt("id"));
					LOG.warn("Fehler beim Laden der Questdaten (Quest: "+questid+"): "+e,e);
				}
			}
			questdata.free();
			
			return CONTINUE;
		}
	}
	
	class SaveQuestContext implements SPFunction, Loggable {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questid = command[1];
			scriptparser.log("QuestID: "+questid+"\n");
			
			SQLQuery questdata = null;
			if( questid.charAt(0) != 'r' ) {
				int user = Value.Int(scriptparser.getRegister("USER"));
				
				questdata = db.query("SELECT id,execdata " +
						"FROM quests_running " +
						"HERE questid="+Integer.parseInt(questid)+" AND userid="+user);	
			}
			else {
				String rquestid = questid.substring(1);
				questdata = db.query("SELECT id,execdata " +
						"FROM quests_running " +
						"WHERE id="+Integer.parseInt(rquestid));
			}
			
			if( !questdata.next() ) {
				scriptparser.log("Warnung: Kein passendes laufendes Quest gefunden\n");
			}
			else {
				try {
					Blob blob = questdata.getBlob("execdata");
					scriptparser.getContext().toStream(blob.setBinaryStream(1));
					db.prepare("UPDATE quests_running SET execdata=? WHERE id=? ")
						.update(blob, questdata.getInt("id"));
				}
				catch( Exception e ) {
					scriptparser.log("Fehler: Konnte Questdaten nicht schreiben: "+e+"\n");
					scriptparser.setRegister("QUEST","r"+questdata.getInt("id"));
					LOG.warn("Fehler beim Schreiben der Questdaten (Quest: "+questid+"): "+e,e);
				}
			}
			questdata.free();
			
			return CONTINUE;
		}
	}
	
	/*—---------------------------------------------
	 * 
	 * 	Cargofunktionen
	 *
	 ----------------------------------------------*/
	
	class AddQuestItem implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("cargo: "+command[1]+"\n");
			Object cargoObj = scriptparser.getRegisterObject(command[1]);
			if( (cargoObj == null) || !(cargoObj instanceof Cargo) ) {
				scriptparser.log("FATAL ERROR: Kein gueltiges Cargo-Objekt gefunden!\n");
				return CONTINUE;
			}
			
			Cargo cargo = (Cargo)cargoObj;
			int newitem = Value.Int(command[2]);
			scriptparser.log("item: "+newitem+"\n");
			
			long count = Value.Long(command[3]);
			scriptparser.log("count: "+count+"\n");
			
			String questid = scriptparser.getRegister("QUEST");
			if( questid.charAt(0) != 'r' ) {
				int userid = Value.Int(scriptparser.getRegister("USER"));
				SQLResultRow questRow = db.first("SELECT id FROM quests_running " +
						"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
				
				if( questRow.isEmpty() ) {
					scriptparser.log("FATAL ERROR: Kein passendes Quest gefunden!\n");
					return CONTINUE;
				}
				questid = Integer.toString(questRow.getInt("id"));
			}
			else {
				questid = questid.substring(1);
			}
		
			ResourceID item = new ItemID(newitem, 0, Value.Int(questid));
			if( count > 0 ) {
				cargo.addResource( item, count );
			}
			else {
				cargo.substractResource( item, -count );	
			}
			
			scriptparser.setRegister(command[1], cargo);
			
			return CONTINUE;
		}
	}
	
	class HasQuestItem implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("cargo: "+command[1]+"\n");
			Object cargoObj = scriptparser.getRegisterObject(command[1]);
			if( (cargoObj == null) || !(cargoObj instanceof Cargo) ) {
				scriptparser.log("FATAL ERROR: Kein gueltiges Cargo-Objekt gefunden!\n");
				return CONTINUE;
			}
			
			Cargo cargo = (Cargo)cargoObj;
			
			int newitem = Value.Int(command[2]);
			scriptparser.log("item: "+newitem+"\n");
			
			int count = Value.Int(command[3]);
			scriptparser.log("count: "+count+"\n");
		
			String questid = scriptparser.getRegister("QUEST");
			if( questid.charAt(0) != 'r' ) {
				int userid = Value.Int(scriptparser.getRegister("USER"));
				SQLResultRow questRow = db.first("SELECT id FROM quests_running " +
						"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
				
				if( questRow.isEmpty() ) {
					scriptparser.log("FATAL ERROR: Kein passendes Quest gefunden!\n");
					return CONTINUE;
				}
				questid = Integer.toString(questRow.getInt("id"));
			}
			else {
				questid = questid.substring(1);
			}
		
			ResourceID item = new ItemID(newitem, 0, Value.Int(questid));
			boolean result = cargo.hasResource( item );
			
			scriptparser.setRegister("cmp", result ? "1" : "0");
			
			return CONTINUE;
		}
	}
	
	class AddItem implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {		
			scriptparser.log("cargo: "+command[1]+"\n");
			Object cargoObj = scriptparser.getRegisterObject(command[1]);
			if( (cargoObj == null) || !(cargoObj instanceof Cargo) ) {
				scriptparser.log("FATAL ERROR: Kein gueltiges Cargo-Objekt gefunden!\n");
				return CONTINUE;
			}
			
			Cargo cargo = (Cargo)cargoObj;
			
			int newitem = Value.Int(command[2]);
			scriptparser.log("item: "+newitem+"\n");
			
			long count = Value.Long(command[3]);
			scriptparser.log("count: "+count+"\n");
		
			ResourceID resid = new ItemID(newitem);
			if( count > 0 ) {
				cargo.addResource( resid, count );
			}
			else {
				cargo.substractResource( resid, -count );	
			}
			scriptparser.setRegister(command[1], cargo);
			
			return CONTINUE;
		}
	}
	
	class HasResource implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("cargo: "+command[1]+"\n");
			Object cargoObj = scriptparser.getRegisterObject(command[1]);
			if( (cargoObj == null) || !(cargoObj instanceof Cargo) ) {
				scriptparser.log("FATAL ERROR: Kein gueltiges Cargo-Objekt gefunden!\n");
				return CONTINUE;
			}
			
			Cargo cargo = (Cargo)cargoObj;
			
			ResourceID resid = Resources.fromString(command[2]);
			scriptparser.log("resource: "+resid+"\n");
			
			long count = Value.Long(command[3]);
			scriptparser.log("count: "+count+"\n");
		
			boolean result = cargo.hasResource( resid, count );
			
			scriptparser.setRegister("cmp", result ? "1" : "0");
			
			return CONTINUE;
		}
	}
	
	class GetResource implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("cargo: "+command[1]+"\n");
			Object cargoObj = scriptparser.getRegisterObject(command[1]);
			if( (cargoObj == null) || !(cargoObj instanceof Cargo) ) {
				scriptparser.log("FATAL ERROR: Kein gueltiges Cargo-Objekt gefunden!\n");
				return CONTINUE;
			}
			
			Cargo cargo = (Cargo)cargoObj;
			
			ResourceID resid = Resources.fromString(command[2]);
			scriptparser.log("resource: "+resid+"\n");
			
			long result = cargo.getResourceCount( resid );
			
			scriptparser.setRegister("A", Long.toString(result));
			
			return CONTINUE;
		}
	}
	
	class AddResource implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("cargo: "+command[1]+"\n");
			Object cargoObj = scriptparser.getRegisterObject(command[1]);
			if( (cargoObj == null) || !(cargoObj instanceof Cargo) ) {
				scriptparser.log("FATAL ERROR: Kein gueltiges Cargo-Objekt gefunden!\n");
				return CONTINUE;
			}
			
			Cargo cargo = (Cargo)cargoObj;
			
			ResourceID resid = Resources.fromString(command[2]);
			scriptparser.log("resource: "+resid+"\n");
			
			long count = Value.Long(command[3]);
			scriptparser.log("count: "+count+"\n");
		
			if( count > 0 ) {
				cargo.addResource( resid, count );
			}
			else {
				cargo.substractResource( resid, -count );	
			}
			scriptparser.setRegister(command[1], cargo);
			
			return CONTINUE;
		}
	}
	
	class TransferWholeCargo implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			scriptparser.log("cargo(target): "+command[1]+"\n");
			Object cargotarObj = scriptparser.getRegisterObject(command[1]);
			
			scriptparser.log("cargo(source): "+command[2]+"\n");
			Object cargosourceObj = scriptparser.getRegister(command[2]);
			
			if( (cargotarObj instanceof Cargo) && (cargosourceObj instanceof Cargo) ) {
				((Cargo)cargotarObj).addCargo((Cargo)cargosourceObj);
				cargosourceObj = new Cargo();
			}
			
			scriptparser.setRegister(command[1], cargotarObj);
			scriptparser.setRegister(command[2], cargosourceObj);
			
			return CONTINUE;
		}
	}

	/*—---------------------------------------------
	 * 
	 * 	Schiffsfunktionen
	 *
	 ----------------------------------------------*/
	
	class LockShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int ship = Value.Int(command[1]);
			scriptparser.log("ship: "+ship+"\n" );
			
			String event = command[2];
			scriptparser.log("event: "+event+"\n" );
			
			String nevent = null;
			
			if( event.equals("oncommunicate") ) {
				nevent = Quests.EVENT_ONCOMMUNICATE;
			}
			else if( event.equals("ontick[running]") ) {
				nevent = Quests.EVENT_RUNNING_ONTICK;
			}
			else if( event.equals("onenter") ) {
				nevent = Quests.EVENT_ONENTER;
			}
			
			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));
			int scriptid = Value.Int(scriptparser.getRegister("SCRIPT"));
			
			final String eventString = scriptid+":"+questid+":"+nevent;
			
			SQLResultRow shipdata = db.first("SELECT fleet,`lock` FROM ships WHERE id>0 AND id="+ship);
			if( eventString.equals(shipdata.getString("lock")) ) {
				return CONTINUE;
			}
			
			db.prepare("UPDATE ships SET `lock`= ?,docked= ? " +
					"WHERE id>0 AND id= ?")
					.update(eventString, "", ship);
					   
			db.update("UPDATE ships SET `lock`='-1' " +
					"WHERE id>0 AND docked IN ('"+ship+"','l "+ship+"')");
					   
			if( shipdata.getInt("fleet") != 0 ) {
				SQLQuery sid = db.query("SELECT id FROM ships " +
						"WHERE id>0 AND AND `lock`!='-1' AND " +
							"fleet="+shipdata.getInt("fleet")+" AND id!="+ship);
				
				while( sid.next() ) {
					db.prepare("UPDATE ships SET `lock`= ?, docked=? " +
							"WHERE id>0 AND id= ?")
							.update(eventString, "", sid.getInt("id"));
					   
					db.update("UPDATE ships SET `lock`='-1' " +
					   			"WHERE id>0 AND docked IN ('"+sid.getInt("id")+"','l "+sid.getInt("id")+"')");	
				}
				sid.free();
			}
			
			final String removescript = "!UNLOCKSHIP "+ship+" "+event+"\n";
			
			SQLResultRow runningdata = null;
			if( questid.charAt(0) != 'r' ) {
				runningdata = db.first("SELECT id,uninstall FROM quests_running " +
						"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
			}
			else {
				int rquestid = Value.Int(questid.substring(1));
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE id="+rquestid);	
			}
			
			String uninstall = runningdata.getString("uninstall");
			if( uninstall.length() == 0 ) {
				uninstall = ":0\n";
			}
			uninstall += removescript;
					
			db.prepare("UPDATE quests_running SET uninstall=? WHERE id= ?")
				.update(uninstall, runningdata.getInt("id"));
		
			return CONTINUE;
		}
	}
	
	class UnlockShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int ship = Value.Int(command[1]);
			scriptparser.log("ship: "+ship+"\n" );
			
			String event = command[2];
			scriptparser.log("event: "+event+"\n" );
			
			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));
			
			db.update("UPDATE ships SET `lock`=NULL WHERE id>0 AND id="+ship);
			db.update("UPDATE ships SET `lock`=NULL WHERE id>0 AND docked IN ('"+ship+"','l "+ship+"')");
			
			SQLResultRow shipdata = db.first("SELECT fleet FROM ships WHERE id>0 AND id="+ship);
			if( shipdata.getInt("fleet") != 0 ) {
				SQLQuery sid = db.query("SELECT id FROM ships " +
						"WHERE id>0 AND AND fleet="+shipdata.getInt("fleet")+" AND id!="+ship);
				
				while( sid.next() ) {
					db.update("UPDATE ships SET `lock`=NULL " +
							"WHERE id>0 AND id="+sid.getInt("id"));
					db.update("UPDATE ships SET `lock`=NULL " +
							"WHERE id>0 AND docked IN ('"+sid.getInt("id")+"','l "+sid.getInt("id")+"')");
				}
				sid.free();
			}
			
			final String removescript = "!UNLOCKSHIP "+ship+" "+event+"\n";
			
			SQLResultRow runningdata = null;
			if( questid.charAt(0) != 'r' ) {
				runningdata = db.first("SELECT id,uninstall FROM quests_running " +
						"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
			}
			else {
				int rquestid = Value.Int(questid.substring(1));
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE id="+rquestid);	
			}
			
			String uninst = runningdata.getString("uninstall");
			uninst = StringUtils.replace(uninst, "\r\n","\n");
			String[] lines = StringUtils.split(uninst, '\n');
			
			StringBuilder newuninst = new StringBuilder();
			for( int i=0; i < lines.length; i++ ) {
				if( !lines[i].trim().equals(removescript) ) {
					newuninst.append(lines[i]);	
				}	
			}
					
			db.prepare("UPDATE quests_running SET uninstall=? WHERE id= ?")
				.update(uninst, runningdata.getInt("id"));
		
			scriptparser.setRegister("_OUTPUT", "");
			
			return CONTINUE;
		}
	}
	
	class AddQuestShips implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SectorTemplateManager stm = SectorTemplateManager.getInstance();
			
			String stmid = command[1];
			scriptparser.log("stmid: "+stmid+"\n");
			
			int system = Value.Int(command[2]);
			int x = Value.Int(command[3]);
			int y = Value.Int(command[4]);
			Location loc = new Location(system, x, y);
			scriptparser.log("coords: "+loc+"\n");
			
			int owner = Value.Int(command[5]);
			scriptparser.log("owner: "+owner+"\n");
			
			boolean visibility = false;
			if( command.length > 6 ) {
				visibility = Value.Int(command[6]) != 0;
				if( visibility ) {
					scriptparser.log("visibile: true\n");
				}
			}
				
			Integer[] shipids = stm.useTemplate(db, stmid, loc, owner );
		
			// TODO: Wie Arrays behandeln?
			scriptparser.setRegister("A",shipids);
		
			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));
			
			StringBuilder removescript = new StringBuilder();
			for( int i=0; i < shipids.length; i++ ) {
				removescript.append("!REMOVESHIP "+shipids[i]+"\n");
			}	
			
			SQLResultRow runningdata = null;
			if( questid.charAt(0) != 'r' ) {
				runningdata = db.first("SELECT id,uninstall FROM quests_running " +
						"WHERE questid="+Value.Int(questid)+" AND userid="+userid);
			}
			else {
				int rquestid = Value.Int(questid.substring(1));
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE id="+rquestid);	
			}
			
			String uninstall = runningdata.getString("uninstall");
			if( uninstall.length() == 0 ) {
				uninstall = ":0\n";
			}
			uninstall += removescript.toString();
					
			db.prepare("UPDATE quests_running SET uninstall=? WHERE id= ?")
				.update(uninstall, runningdata.getInt("id"));
			
			if( !visibility ) {	
				db.update("UPDATE ships SET visibility='"+userid+"' " +
						"WHERE id>0 AND id IN ("+Common.implode(",",shipids)+")");
			}
			
			return CONTINUE;
		}
	}
	
	class AddShips implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			SectorTemplateManager stm = SectorTemplateManager.getInstance();
			
			String stmid = command[1];
			scriptparser.log("stmid: "+stmid+"\n");
			
			int system = Value.Int(command[2]);
			int x = Value.Int(command[3]);
			int y = Value.Int(command[4]);
			Location loc = new Location(system, x, y);
			scriptparser.log("coords: "+loc+"\n");
			
			int owner = Value.Int(command[5]);
			scriptparser.log("owner: "+owner+"\n");
			
			Integer[] shipids = stm.useTemplate(db, stmid, loc, owner );
		
			// TODO: Wie Arrays behandeln?
			scriptparser.setRegister("A",shipids);
			
			return CONTINUE;
		}
	}
	
	class RemoveShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int shipid = Integer.parseInt(command[1]);
			
			Ships.destroy( shipid );
			
			return CONTINUE;
		}
	}
	
	class MoveShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int shipid = Value.Int(command[1]);
			scriptparser.log("shipid: "+shipid+"\n");
			
			Location target = Location.fromString(command[2]);
			scriptparser.log("target: "+target+"\n");
				
			SQLResultRow curpos = db.first("SELECT x,y,system,s FROM ships WHERE id=",shipid);
			
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
			
			RouteFactory router = new RouteFactory();
			List<Waypoint> route = router.findRoute(Location.fromResult(curpos), target);
					
			Ships.MovementStatus result = Ships.move(shipid, route, true, false); 
			scriptparser.log(Common._stripHTML(Ships.MESSAGE.getMessage()));
			
			if( result != Ships.MovementStatus.SUCCESS ) {
				scriptparser.setRegister("A","1");
			}
			else {
				scriptparser.setRegister("A","0");
			}
			
			SQLResultRow ship = db.first("SELECT id,x,y,system FROM ships WHERE id=",shipid);
			if( !Location.fromResult(ship).equals(target) ) {
				scriptparser.log("Position nicht korrekt - Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return STOP;
			}
			
			scriptparser.log("\n");
			return CONTINUE;
		}
	}
	
	class IsShipDestroyed implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int shipid = Value.Int(command[1]);
			scriptparser.log("shipid: "+shipid+"\n");
			
			SQLResultRow ship = db.first("SELECT id,battle FROM ships WHERE id>0 AND id="+shipid);
			if( ship.isEmpty() ) {
				scriptparser.setRegister("cmp","0");	
			}
			else if( ship.getInt("battle") != 0 ) {
				SQLResultRow battleship = db.first("SELECT action " +
						"FROM battles_ships " +
						"WHERE shipid="+shipid+" AND battleid="+ship.getInt("battle"));

				if( (battleship.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
					scriptparser.setRegister("cmp","0");
				}
				else {
					scriptparser.setRegister("cmp","1");
				}
			}
			else {
				scriptparser.setRegister("cmp","1");	
			}
			
			return CONTINUE;
		}
	}
	
	class AddLootTable implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));
			
			int shiptype = Value.Int(command[1]);
			scriptparser.log("shiptype: "+shiptype+"\n");
			
			int shipowner = Value.Int(command[2]);
			scriptparser.log("shipowner: "+shipowner+"\n");
			
			int chance = Value.Int(command[3]);
			scriptparser.log("chance: "+chance);
			
			ResourceID resourceid = Resources.fromString(command[4]);
			scriptparser.log("resourceid: "+resourceid+"\n");
			if( resourceid.isItem() ) {
				if( resourceid.getQuest() == 1 ) {
					int qid = 0;
					if( questid.charAt(0) != 'r' ) {
						qid = db.first("SELECT id FROM quests_running WHERE questid="+Value.Int(questid)+" AND userid="+userid).getInt("id");
					}
					else {
						qid = Value.Int(questid.substring(1));
					}
					
					resourceid = new ItemID(resourceid.getItemID(), resourceid.getUses(), qid);
				}
			}
			
			long count = Value.Long(command[5]);
			scriptparser.log("resource-count: "+count+"\n");
			
			long totalmax = 0;
			if( command.length > 6 ) {
				totalmax = Value.Long(command[6]);
				scriptparser.log("totalmax: "+totalmax+"\n");
			}
			
			if( totalmax == 0 ) {
				totalmax = -1;
			}
			
			db.update("INSERT INTO ship_loot " +
					"(shiptype,owner,targetuser,chance,resource,count,totalmax) " +
					"VALUES " +
					"("+shiptype+","+shipowner+","+userid+","+chance+",'"+resourceid.toString()+"',"+count+","+totalmax+")");
			
			int loottable = db.insertID();
			
			SQLResultRow runningdata = null;
			if( questid.charAt(0) != 'r' ) {
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE questid="+Value.Int(questid)+" AND userid="+userid);
			}
			else {
				int rquestid = Value.Int(questid.substring(1));
				runningdata = db.first("SELECT id,uninstall FROM quests_running WHERE id="+rquestid);
			}
				
			if( !runningdata.isEmpty() ) {
				if( runningdata.getString("uninstall").length() == 0 ) {
					runningdata.put("uninstall", ":0\n");	
				}
				runningdata.put("uninstall", runningdata.getString("uninstall") + "!DELETELOOTTABLE "+loottable+"\n");
					
				db.update("UPDATE quests_running SET uninstall='"+runningdata.getString("uninstall")+"' WHERE id="+runningdata.getInt("id"));
			}
			
			return CONTINUE;
		}
	}
	
	class DeleteLootTable implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int lootid = Value.Int(command[1]);
			scriptparser.log("loottable-id: "+lootid+"\n");
			
			if( lootid > 0 ) {
				db.update("DELETE FROM ship_loot WHERE id="+lootid);
			}
			
			return CONTINUE;
		}
	}
	
	/*—---------------------------------------------
	 * 
	 * 	Diverses
	 *
	 ----------------------------------------------*/
	
	class Msg implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int from = Integer.parseInt(command[1]);
			int to = Integer.parseInt(command[2]);
			
			scriptparser.log("sender: "+from+"\n");
			scriptparser.log("receiver: "+to+"\n");
			
			String title = command[3];
			scriptparser.log("title: "+title+"\n");  
			
			String msg = command[4];
			scriptparser.log("msg: "+msg+"\n\n");
					
			PM.send( ContextMap.getContext(), from, to, title, msg );
			
			return CONTINUE;
		}
	}
	
	class AddMoney implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int userid = Value.Int(command[1]);
			scriptparser.log("userid: "+userid+"\n");
			
			int fromid = Value.Int(command[2]);
			scriptparser.log("fromid: "+fromid+"\n");
			
			long money = Value.Long(command[3]);
			scriptparser.log("money: "+money+"\n");
			
			String reason = command[4];
			scriptparser.log("reason: "+reason+"\n");
			
			int fake = Value.Int(command[5]);
			scriptparser.log("fake: "+fake+"\n");
			
			if( money > 0 ) {
				User user = (User)ContextMap.getContext().getDB().get(User.class, userid);
				user.transferMoneyFrom(fromid, money, reason, fake != 0);
			}
			else {
				User user = (User)ContextMap.getContext().getDB().get(User.class, fromid);
				user.transferMoneyFrom(userid, -money, reason, fake != 0);
			}
			
			return CONTINUE;
		}
	}
	
	class GetMoney implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int userid = Value.Int(command[1]);
			scriptparser.log("userid: "+userid+"\n");
			
			User user = (User)ContextMap.getContext().getDB().get(User.class, userid);
			scriptparser.setRegister("A",user.getKonto().toString());
			
			return CONTINUE;
		}
	}
	
	class CloneOffizier implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int offiid = Value.Int(command[1]);
			scriptparser.log("offiid: "+offiid+"\n");
			
			String desttype = command[2];
			scriptparser.log("desttype: "+desttype+"\n");
			
			int destid = Value.Int(command[3]);
			scriptparser.log("destid: "+destid+"\n");
			
			SQLResultRow offizier = db.first("SELECT * FROM offiziere WHERE id="+offiid);
			if( offizier.isEmpty() ) {
				scriptparser.log("Warnung: Offizier konnte nicht gefunden werden");
				return CONTINUE;
			}
			
			int destowner = 0;
			if( desttype.equals("s") ) {
				destowner = db.first("SELECT owner FROM ships WHERE id>0 AND id="+destid).getInt("id");
			}
			else if( desttype.equals("b") ) {
				destowner = db.first("SELECT owner FROM bases WHERE id="+destid).getInt("id");
			}
			
			// Neuen Offizier in die DB schreiben
			offizier.put("userid", destowner);
			offizier.put("dest", desttype+" "+destid);
			
			StringBuilder columns = new StringBuilder();
			StringBuilder values = new StringBuilder();
			Object[] data = new Object[offizier.size()-1]; // ID wird nicht angegeben
			
			int i=0;
			for( String key : offizier.keySet() ) {
				if( key.equals("id") ) {
					continue;
				}
				if( columns.length() > 0 ) {
					columns.append(',');
					values.append(',');
				}
				columns.append(key);
				values.append('?');
				data[i++] = offizier.get(key);
			}
			
			PreparedQuery pq = db.prepare("INSERT INTO offiziere ("+columns+") VALUES ("+values+")");
			pq.update(data);
			int newoffiid = pq.insertID();
			pq.close();
			
			scriptparser.setRegister("A",Integer.toString(newoffiid));
				
			if( desttype.equals("s") ) {
				Ships.recalculateShipStatus(destid);	
			}
			
			return CONTINUE;
		}
	}
	
	class RemoveOffizier implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int offiid = Value.Int(command[1]);
			scriptparser.log("offiid: "+offiid+"\n");
			
			SQLResultRow dest = db.first("SELECT dest FROM offiziere WHERE id="+offiid);
			if( dest.isEmpty() ) {
				return CONTINUE;	
			}
			String[] destArray = StringUtils.split(dest.getString("dest"), ' ');
						
			db.update("DELETE FROM offiziere WHERE id="+offiid);
			
			if( destArray[0].equals("s") ) {
				Ships.recalculateShipStatus(Value.Int(destArray[1]));	
			}
			
			return CONTINUE;
		}
	}
	
	class StartBattle implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int attacker = Value.Int(command[1]);
			scriptparser.log("attacker(ship): "+attacker+"\n");
			
			int defender = Value.Int(command[2]);
			scriptparser.log("defender(ship): "+defender+"\n");
			
			int questbattle = Value.Int(command[3]);
			scriptparser.log("questbattle: "+questbattle+"\n");
				
			SQLResultRow owner = db.first("SELECT owner FROM ships WHERE id>0 AND id="+attacker);
			
			Battle battle = new Battle();
			battle.create( owner.getInt("owner"), attacker, defender );
			
			scriptparser.setRegister("A", Integer.toString(battle.getID()) );
			
			if( questbattle != 0 ) {
				String questid = scriptparser.getRegister("QUEST");
				if( questid.charAt(0) != 'r' ) {
					int userid = Value.Int(scriptparser.getRegister("USER"));
					questid = Integer.toString(
							db.first("SELECT id FROM quests_running " +
									"WHERE questid="+Value.Int(questid)+" AND userid="+userid).getInt("id")
					);
				}
				else {
					questid = questid.substring(1);	
				}
				
				SQLResultRow defowner = db.first("SELECT owner FROM ships WHERE id>0 AND id="+defender);
				battle.setQuest(Value.Int(questid));
				battle.addToVisibility(owner.getInt("id"));
				battle.addToVisibility(defowner.getInt("id"));	
				battle.save(true);
			}
			
			return CONTINUE;
		}
	}
	
	
	class AddBattleVisibility implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int battleid = Value.Int(command[1]);
			scriptparser.log("battleid: "+battleid+"\n");
			
			int userid = Value.Int(command[2]);
			scriptparser.log("userid: "+userid+"\n");
			
			Battle battle = new Battle();
			battle.load( 0, battleid, 0, 0, 1 );
			battle.addToVisibility(userid);
			
			battle.save(true);
			
			return CONTINUE;
		}
	}
	
	
	
	class EndBattle implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int battleid = Value.Int(command[1]);
			scriptparser.log("battleid: "+battleid+"\n");
			
			int side1points = Value.Int(command[2]);
			scriptparser.log("side1points: "+side1points+"\n");
			
			int side2points = Value.Int(command[3]);
			scriptparser.log("side2points: "+side2points+"\n");
			
			int executescripts = Value.Int(command[4]);
			scriptparser.log("executescripts: "+executescripts+"\n");
				
			Battle battle = new Battle();
			battle.load( 0, battleid, 0, 0, 1 );
			battle.endBattle( side1points, side2points, executescripts != 0 );
			
			return CONTINUE;
		}
	}
	
	
	class GetNoobStatus implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int userid = Integer.parseInt(command[1]);
			scriptparser.log("userid: "+userid+"\n");
			
			User user = (User)ContextMap.getContext().getDB().get(User.class, userid);
			if( user.isNoob() ) {
				scriptparser.setRegister("A","1");
			}	
			else {
				scriptparser.setRegister("A","0");
			}	

			return CONTINUE;
		}
	}
	
	
	
	class GetUserValue implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int userid = Integer.parseInt(command[1]);
			scriptparser.log("userid: "+userid+"\n");
			
			String valuename = command[2];
			scriptparser.log("value(key): "+valuename+"\n");
			
			User user = (User)ContextMap.getContext().getDB().get(User.class, userid);
			
			String value = user.getUserValue(valuename);
			
			if( value.length() == 0 ) {
				scriptparser.log("Uservalue ist nicht gesetzt - #cmp = -1\n");
				scriptparser.setRegister("cmp",-1);	
			}
			else {
				scriptparser.setRegister("cmp",0);	
			}
			scriptparser.setRegister("A",value);
			
			return CONTINUE;
		}
	}
	
	
	class SetUserValue implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int userid = Integer.parseInt(command[1]);
			scriptparser.log("userid: "+userid+"\n");
			
			String valuename = command[2];
			scriptparser.log("value(key): "+valuename+"\n");
			
			String value = command[3];
			scriptparser.log("value: "+value+"\n");
			
			User user = (User)ContextMap.getContext().getDB().get(User.class, userid);
			user.setUserValue(valuename, value);
			
			return CONTINUE;
		}
	}
	
	
	class GetSectorProperty implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			Location sector = Location.fromString(command[1]);
			scriptparser.log("sector: "+sector+"\n");
			
			String property = command[2];
			scriptparser.log("property: "+property+"\n");
			
			List<String> result = new ArrayList<String>();
			
			String locSQL = "system="+sector.getSystem()+" AND x="+sector.getX()+" AND y="+sector.getY();
			if( property.equals("nebel") ) {
				SQLResultRow nebel = db.first("SELECT id FROM nebel WHERE "+locSQL);
				if( !nebel.isEmpty() ) {
					result.add(Integer.toString(nebel.getInt("id")));
				}
			} 
			else if( property.equals("bases") ) {
				SQLQuery bid = db.query("SELECT id FROM bases WHERE "+locSQL);
				while( bid.next() ) {
					result.add(Integer.toString(bid.getInt("id")));	
				}
				bid.free();
			}
			else if( property.equals("jumpnodes") ) {
				SQLQuery jn = db.query("SELECT id FROM jumpnodes WHERE "+locSQL);
				while( jn.next() ) {
					result.add(Integer.toString(jn.getInt("id")));	
				}
				jn.free();
			}
			else if( property.equals("ships") ) {
				SQLQuery ship = db.query("SELECT id FROM ships WHERE id>0 AND "+locSQL);
				while( ship.next() ) {
					result.add(Integer.toString(ship.getInt("id")));	
				}
				ship.free();
			}
			else if( property.equals("shipsByOwner") ) {
				int owner = Value.Int(command[3]);
				scriptparser.log("owner: "+owner+"\n");
				
				SQLQuery ship = db.query("SELECT id FROM ships WHERE id>0 AND "+locSQL+" AND owner="+owner);
				while( ship.next() ) {
					result.add(Integer.toString(ship.getInt("id")));	
				}
				ship.free();
			}
			else if( property.equals("shipsByTag") ) {
				String tag = command[3];
				scriptparser.log("tag: "+tag+"\n");
				
				SQLQuery ship = db.prepare("SELECT id FROM ships WHERE id>0 AND "+locSQL+" AND LOCATE( ? ,status)")
					.query("<"+tag+">");
				while( ship.next() ) {
					result.add(Integer.toString(ship.getInt("id")));	
				}
				ship.free();	
			}
		
			scriptparser.setRegister("A",result.toArray(new String[result.size()]));
			
			return CONTINUE;
		}
	}
	
	
	class GetSystemProperty implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int system = Value.Int(command[1]);
			scriptparser.log("system: "+system+"\n");
			
			String property = command[2];
			scriptparser.log("property: "+property+"\n");
			
			List<String> result = new ArrayList<String>();
			
			if( property.equals("shipsByTag") ) {
				String tag = command[3];
				scriptparser.log("tag: "+tag+"\n");
				
				SQLQuery ship = db.query("SELECT id FROM ships WHERE id>0 AND system="+system+" AND LOCATE('<"+tag+">',status)");
				while( ship.next() ) {
					result.add(Integer.toString(ship.getInt("id")));
				}
				ship.free();
			}
		
			scriptparser.setRegister("A",result.toArray(new String[result.size()]));
			
			return CONTINUE;
		}
	}
	
	class GtuAuctionShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int shipid = Integer.parseInt(command[1]);
			scriptparser.log("shiptypeid: "+shipid+"\n");
			
			int ticks = Integer.parseInt(command[2]);
			scriptparser.log("ticks: "+ticks+"\n");
			
			int initbid = Integer.parseInt(command[3]);
			scriptparser.log("initbid: "+initbid+"\n");
			
			int owner = Integer.parseInt(command[4]);
			if( owner == 0 ) {
				owner = Faction.GTU;
			}
			scriptparser.log("owner: "+owner+"\n");
			
			int curtick = ContextMap.getContext().get(ContextCommon.class).getTick();
			ticks += curtick;
			
			db.update("INSERT INTO versteigerungen (mtype,type,tick,preis,owner)" ,
					" VALUES ('1','",shipid,"',",ticks,",",initbid,",",owner,")");
			
			return CONTINUE;
		}
	}
	
	class GtuAuctionCargo implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			ResourceID resid = Resources.fromString(command[1]);
			scriptparser.log("resid: "+resid+"\n");
			
			int count = Value.Int(command[2]);
			scriptparser.log("count: "+count+"\n");
			
			int ticks = Value.Int(command[3]);
			scriptparser.log("ticks: "+ticks+"\n");
			
			int initbid = Value.Int(command[4]);
			scriptparser.log("initbid: "+initbid+"\n");
			
			int owner = Value.Int(command[5]);
			if( owner == 0 ) {
				owner = Faction.GTU;
			}
			scriptparser.log("owner: "+owner+"\n");
			
			int curtick = ContextMap.getContext().get(ContextCommon.class).getTick();
			ticks += curtick;
			
			Cargo cargo = new Cargo();
			cargo.addResource(resid, count);
			
			db.update("INSERT INTO versteigerungen (mtype,type,tick,preis,owner)" +
					" VALUES ('2','"+cargo.save()+"',"+ticks+","+initbid+","+owner+")");
			
			return CONTINUE;
		}
	}
	
	/*—---------------------------------------------
	 * 
	 * 	Quick-Quests
	 *
	 ----------------------------------------------*/
	
	class GenerateQuickQuestSourceMenu implements SPFunction,Loggable {
		private void call( SPFunction f, Database db, ScriptParser scriptparser, Object ... cmd) {
			scriptparser.log("-> "+f.getClass().getSimpleName()+"\n");
			String[] command = new String[cmd.length+1];
			command[0] = "!"+f.getClass().getSimpleName();
			for( int i=0; i < cmd.length; i++ ) {
				command[i+1] = cmd[i].toString();
			}
			
			f.execute(db, scriptparser, command);
		}
		
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String typeid = command[1];
			scriptparser.log("TypeID: "+typeid+"\n");
			
			int shipid = Integer.parseInt(command[2]);
			scriptparser.log("ShipID: "+shipid+"\n");
		
			SQLQuery qquest = db.query("SELECT * FROM quests_quick " +
					"WHERE enabled>0 AND " +
					"(source='"+shipid+"' OR source LIKE '"+shipid+",%' OR source LIKE '%,"+shipid+"' OR source LIKE '%,"+shipid+",%') AND " +
					"sourcetype='"+typeid+"'");
			while( qquest.next() ) {
				if( qquest.getInt("moreThanOnce") == 0 ) {
					call(new HasQuestCompleted(), db, scriptparser, qquest.getInt("enabled"));
					if( Integer.parseInt(scriptparser.getRegister("#cmp")) > 0 ) {
						continue;
					}
				}
				if( qquest.getString("dependsOnQuests").length() > 0 ) {
					boolean ok = true;
					
					String[] qquests = StringUtils.split(qquest.getString("dependsOnQuests"), ';');
					for( int i=0; i < qquests.length; i++ ) {
						String[] tmp = StringUtils.split(qquests[i], ':');
						SQLResultRow qid = db.first("SELECT id FROM quests WHERE qid='"+tmp[1]+"'");
						if( qid.isEmpty() ) {
							LOG.warn("QQuest "+qquest.getInt("id")+" benoetigt Quest "+tmp[1]+", welches jedoch nicht existiert");
							continue;
						}
		
						call(new HasQuestCompleted(), db, scriptparser, qid.getInt("id"));
						if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
							ok = false;
							break;
						}
					}	
					if( !ok ) {
						continue;
					}
				}
				call(new LoadQuestContext(), db, scriptparser, qquest.getInt("enabled"));
							
				if( Value.Int(scriptparser.getRegister("#QSTATUS")) > 0 ) {
					continue;
				}
				
				call(new AddAnswer(), db, scriptparser, 
						"Auftrag &gt;"+qquest.getString("qname")+"&lt",
						"_quick_quests",
						"desc",
						qquest.getInt("id"));
			}
			qquest.free();
			
			return CONTINUE;
		}
	}
	
	
	class GenerateQuickQuestTargetMenu implements SPFunction, Loggable {
		private void call( SPFunction f, Database db, ScriptParser scriptparser, Object ... cmd) {
			scriptparser.log("-> "+f.getClass().getSimpleName()+"\n");
			String[] command = new String[cmd.length+1];
			command[0] = "!"+f.getClass().getSimpleName();
			for( int i=0; i < cmd.length; i++ ) {
				command[i+1] = cmd[i].toString();
			}
			
			f.execute(db, scriptparser, command);
		}
		
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String typeid = command[1];
			scriptparser.log("TypeID: "+typeid+"\n");
			
			int shipid = Integer.parseInt(command[2]);
			scriptparser.log("ShipID: "+shipid+"\n");
			
			SQLQuery qquest = db.query("SELECT * FROM quests_quick " +
					"WHERE enabled>0 AND " +
					"(target='"+shipid+"' OR target LIKE '"+shipid+",%' OR target LIKE '%,"+shipid+"' OR target LIKE '%,"+shipid+",%') AND " + 
					"targettype='"+typeid+"'");
			while( qquest.next() ) {
				call(new LoadQuestContext(), db, scriptparser, qquest.getInt("enabled"));
				
				if( scriptparser.getRegister("#QUEST").length() == 0 || 
					scriptparser.getRegister("#QUEST").equals("0")  ) {
					continue;
				}

				int rquestid = Integer.parseInt(scriptparser.getRegister("#QUEST").substring(1));
				int qid = db.first("SELECT questid FROM quests_running WHERE id="+rquestid).getInt("questid");
				if( qid != qquest.getInt("enabled") ) {
					continue;
				}
				
				if( Value.Int(scriptparser.getRegister("#QSTATUS")) != 1 ) {
					continue;
				}
				
				call(new AddAnswer(), db, scriptparser, 
						"Auftrag &gt;"+qquest.getString("qname")+"&lt beenden",
						"_quick_quests",
						"end",
						qquest.getInt("id"));
			}
			qquest.free();
			
			return CONTINUE;
		}
	}
	
	
	class HandleQuickQuestEvent implements SPFunction, Loggable {
		private void call( SPFunction f, Database db, ScriptParser scriptparser, Object ... cmd) {
			scriptparser.log("-> "+f.getClass().getSimpleName()+"\n");
			String[] command = new String[cmd.length+1];
			command[0] = "!"+f.getClass().getSimpleName();
			for( int i=0; i < cmd.length; i++ ) {
				command[i+1] = cmd[i].toString();
			}
			
			f.execute(db, scriptparser, command);
		}
		
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			String typeid = command[1];
			scriptparser.log("TypeID: "+typeid+"\n");
			
			int shipid = Integer.parseInt(command[2]);
			scriptparser.log("ShipID: "+shipid+"\n");
			
			scriptparser.log("Action: "+scriptparser.getParameter(0)+"\n");
			
			String action = scriptparser.getParameter(0);
			if( "desc".equals(action) ) {
				scriptparser.log("QQuest: "+scriptparser.getParameter(1)+"\n");
				SQLResultRow qquest = db.first("SELECT * FROM quests_quick WHERE id="+Value.Int(scriptparser.getParameter(1)));
				if( qquest.isEmpty() || !qquest.getString("sourcetype").equals(typeid) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				int[] sourcelist = Common.explodeToInt(",", qquest.getString("source"));
				if( !Common.inArray(shipid, sourcelist) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				call(new LoadQuestContext(), db, scriptparser, qquest.getInt("enabled"));
				call(new GetQuestID(), db, scriptparser, scriptparser.getRegister("#QUEST"));
				
				if( Value.Int(scriptparser.getRegister("#A")) == qquest.getInt("enabled") ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;	
				}
					
				if( qquest.getInt("moreThanOnce") == 0 ) {
					call(new HasQuestCompleted(), db, scriptparser, qquest.getInt("enabled"));
					if( Value.Int(scriptparser.getRegister("#cmp")) > 0 ) {
						scriptparser.setRegister("#A","0");
						return CONTINUE;
					}
				}
				
				if( qquest.getString("dependsOnQuests").length() > 0 ) {
					String[] qquests = StringUtils.split(qquest.getString("dependsOnQuests"), ';');
					for( int i=0; i < qquests.length; i++ ) {
						String[] tmp = StringUtils.split(qquests[i], ':');
						SQLResultRow qid = db.first("SELECT id FROM quests WHERE qid='"+tmp[1]+"'");
						if( qid.isEmpty() ) {
							LOG.warn("QQuest "+qquest.getInt("id")+" benoetigt Quest "+tmp[1]+", welches jedoch nicht existiert");
							continue;
						}
		
						call(new HasQuestCompleted(), db, scriptparser, qid.getInt("id"));
						if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
							scriptparser.setRegister("#A","0");
							return CONTINUE;
						}
					}
				}
				
				String dialogtext = qquest.getString("shortdesc")+"[hr]\n"+qquest.getString("desc")+"\n\n";
				if( (qquest.getString("reqitems").length() > 0) || (qquest.getInt("reqre") > 0) ) {
					dialogtext += "Benötigt:[color=red]\n";
					if( qquest.getString("reqitems").length() > 0 ) {
						Cargo cargo = new Cargo(Cargo.Type.STRING, qquest.getString("reqitems"));
					
						ResourceList reslist = cargo.getResourceList();
						for( ResourceEntry res : reslist ) {
							dialogtext += "[resource="+res.getId()+"]"+res.getCount1()+"[/resource]\n";	
						}
					}
					if( qquest.getInt("reqre") != 0 ) {
						dialogtext += Common.ln(qquest.getInt("reqre"))+" RE\n";
					}
					dialogtext += "[/color]\n\n";
				}
				if( qquest.getString("awarditems").length() > 0 ) {
					dialogtext += "Belohnung in Waren:\n";
					Cargo cargo = new Cargo(Cargo.Type.STRING, qquest.getString("awarditems"));
					
					ResourceList reslist = cargo.getResourceList();
					for( ResourceEntry res : reslist ) {
						dialogtext += "[resource="+res.getId()+"]"+res.getCount1()+"[/resource]\n";	
					}
					dialogtext += "\n";
				}
				if( qquest.getString("awardre").length() > 0 ) {
					dialogtext += "Belohnung in RE: "+Common.ln(qquest.getInt("awardre"))+"\n";	
				}
				
				call(new LoadDialog(), db, scriptparser, 
						dialogtext, 
						qquest.getString("head"));
				
				call(new AddAnswer(), db, scriptparser,
						"Annehmen",
						"_quick_quests",
						"yes",
						qquest.getInt("id"));
											
				call(new AddAnswer(), db, scriptparser,
						"Ablehnen",
						"0");
							
				call(new InitDialog(), db, scriptparser);
			} 
			// ende action.equals("desc")
			else if( "yes".equals(action) ) {
				scriptparser.log("QQuest: "+scriptparser.getParameter(1)+"\n");
				SQLResultRow qquest = db.first("SELECT * FROM quests_quick WHERE id="+Value.Int(scriptparser.getParameter(1)));
				if( qquest.isEmpty() || !qquest.getString("sourcetype").equals(typeid) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;	
				}
				
				int[] sourcelist = Common.explodeToInt(",", qquest.getString("source"));
				if( !Common.inArray(shipid, sourcelist) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				call(new LoadQuestContext(), db, scriptparser, qquest.getInt("enabled") );
				call(new GetQuestID(), db, scriptparser, scriptparser.getRegister("#QUEST") );
				if( Value.Int(scriptparser.getRegister("#A")) == qquest.getInt("enabled") ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
					
				if( qquest.getInt("moreThanOnce") == 0 ) {
					call(new HasQuestCompleted(), db, scriptparser, qquest.getInt("enabled") );
					if( Value.Int(scriptparser.getRegister("#cmp")) > 0 ) {
						scriptparser.setRegister("#A","0");
						return CONTINUE;
					}
				}
				if( qquest.getString("dependsOnQuests").length() > 0 ) {
					String[] qquests = StringUtils.split(qquest.getString("dependsOnQuests"), ';');
					for( int i=0; i < qquests.length; i++ ) {
						String[] tmp = StringUtils.split(qquests[i], ':');
						SQLResultRow qid = db.first("SELECT id FROM quests WHERE qid='"+tmp[1]+"'");
						if( qid.isEmpty() ) {
							LOG.warn("QQuest "+qquest.getInt("id")+" benoetigt Quest "+tmp[1]+", welches jedoch nicht existiert");
							continue;
						}
		
						call(new HasQuestCompleted(), db, scriptparser, qid.getInt("id"));
						if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
							scriptparser.setRegister("#A","0");
							return CONTINUE;
						}
					}
				}
				
				call(new InitQuest(), db, scriptparser, qquest.getInt("enabled") );
				scriptparser.setRegister("#QSTATUS","1");
				
				// Evt fuer das Quest benoetigte Items auf das Schiff transferieren
				if( qquest.getString("startitems").length() > 0 ) {
					call(new CopyVar(), db, scriptparser, 
							"#ship",
							"shipsource.cargo" );
					
					Cargo cargo = new Cargo( Cargo.Type.STRING, qquest.getString("startitems") );
					
					ResourceList reslist = cargo.getResourceList();
					for( ResourceEntry res : reslist ) {
						if( res.getId().isItem() ) {
							if( res.getId().getQuest() != 0 ) {
								call( new AddQuestItem(), db, scriptparser, 
										"#ship",
										res.getId().getItemID(),
										res.getCount1() );
							}
							else {
								call( new AddResource(), db, scriptparser, 
										"#ship",
										res.getId().toString(),
										res.getCount1() );
							}
						}
						else {
							call( new AddResource(), db, scriptparser, 
									"#ship",
									res.getId().toString(),
									res.getCount1() );
						}
					}
					call( new SaveVar(), db, scriptparser, 
							"shipsource.cargo",
							"#ship" );
					
					scriptparser.setRegister("#ship","0");
				}
				// Loottable ergaenzen
				if( qquest.getString("loottable").length() != 0 ) {
					String[] loottable = StringUtils.split(qquest.getString("loottable"), ';');
					for( int i=0; i < loottable.length; i++ ) {
						String[] atable = StringUtils.split(loottable[i], ',');
						if( atable.length > 4 ) {
							call( new AddLootTable(), db, scriptparser, 
									atable[0],
									atable[1],
									atable[2],
									atable[3],
									atable[4],
									atable.length > 5 ? atable[5] : "" );
						}	
					}	
				}
				scriptparser.setRegister("#quest"+qquest.getInt("id")+"_status", qquest.getString("shortdesc"));
				call( new SetQuestUIStatus(), db, scriptparser, 
						"#quest"+qquest.getInt("id")+"_status",
						"1" );	
			} 
			// ende if( action.equals("yes")
			else if( "end".equals(action) ) {
				scriptparser.log("QQuest: "+scriptparser.getParameter(1)+"\n");
				SQLResultRow qquest = db.first("SELECT * FROM quests_quick WHERE id="+Value.Int(scriptparser.getParameter(1)));
				if( qquest.isEmpty() || !qquest.getString("sourcetype").equals(typeid) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;	
				}
				
				int[] sourcelist = Common.explodeToInt(",", qquest.getString("source"));
				if( !Common.inArray(shipid, sourcelist) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				call(new LoadQuestContext(), db, scriptparser, qquest.getInt("enabled") );
				
				if( scriptparser.getRegister("#QUEST").length() == 0 ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				int rquestid = Value.Int(scriptparser.getRegister("#QUEST").substring(1));
				SQLResultRow qid = db.first("SELECT questid FROM quests_running WHERE id="+rquestid);
				if( qid.isEmpty() || (qid.getInt("questid") != qquest.getInt("enabled")) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
					
				if( Value.Int(scriptparser.getRegister("#QSTATUS")) != 1 ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				boolean quitable = true;
				
				// Die zum beenden benoetigten Items checken
				if( qquest.getString("reqitems").length() != 0 ) {
					Cargo cargo = new Cargo( Cargo.Type.STRING, qquest.getString("reqitems") );
					
					call( new CopyVar(), db, scriptparser, 
							"#ship",
							"shipsource.cargo" );
					
					ResourceList reslist = cargo.getResourceList();
					for( ResourceEntry res : reslist ) {
						if( res.getId().isItem() && (res.getId().getQuest() != 0) ) {
							call( new HasQuestItem(), db, scriptparser, 
									"#ship",
									res.getId().getItemID(),
									res.getCount1() );
							
							if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
								quitable = false;
								break;
							}					  
						}
						else {
							call( new HasResource(), db, scriptparser, 
									"#ship",
									res.getId(),
									res.getCount1() );
							
							if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
								quitable = false;
								break;
							}
						}
					}
					scriptparser.setRegister("#ship","0");
				} // end 'reqitems'
				
				if( quitable && (qquest.getLong("reqre") > 0) ) {
					call( new GetMoney(), db, scriptparser, scriptparser.getRegister("#USER") );
					if( Value.Int(scriptparser.getRegister("A")) < qquest.getLong("reqre") ) {
						quitable = false;
					}
				}
				
				// Koennen wir das Quest nun beenden oder nicht?
				if( quitable ) {
					// Die ganzen gegenstaende abbuchen
					if( qquest.getString("reqitems").length() > 0 ) {
						Cargo cargo = new Cargo( Cargo.Type.STRING, qquest.getString("reqitems") );
						
						call( new CopyVar(), db, scriptparser, 
								"#ship",
								"shipsource.cargo" );
						
						ResourceList reslist = cargo.getResourceList();
						for( ResourceEntry res : reslist ) {						
							if( res.getId().isItem() && (res.getId().getQuest() != 0) ) {
								call( new AddQuestItem(), db, scriptparser, 
										"#ship",
										res.getId().getItemID(),
										-res.getCount1() );
							}
							else {				  
								call( new AddResource(), db, scriptparser, 
										"#ship",
										res.getId(),
										-res.getCount1() );
							}
						}
						call(new SaveVar(), db, scriptparser, 
								"shipsource.cargo",
								"#ship" );
						
						scriptparser.setRegister("#ship","0");
					} // end 'reqitems'
						
					if( qquest.getLong("reqre") != 0 ) {
						call( new AddMoney(), db, scriptparser, 
								0,
								scriptparser.getRegister("#USER"),
								qquest.getLong("reqre"),
								"Kosten Quest '"+qquest.getString("qname")+"'",
								0 );
					}
						
					// Belohnungen (Waren/RE)
					if( qquest.getString("awarditems").length() > 0 ) {
						Cargo cargo = new Cargo( Cargo.Type.STRING, qquest.getString("awarditems") );
						call( new CopyVar(), db, scriptparser, 
								"#ship",
								"shipsource.cargo" );
						
						ResourceList reslist = cargo.getResourceList();
						for( ResourceEntry res : reslist ) {
							call( new AddResource(), db, scriptparser, 
									"#ship",
									res.getId(),
									res.getCount1() );
						}
						call( new SaveVar(), db, scriptparser, 
								"shipsource.cargo",
								"#ship" );
						
						scriptparser.setRegister("#ship","0");	
					}
					if( Value.Long(qquest.getString("awardre")) != 0 ) {
						call( new AddMoney(), db, scriptparser, 
								scriptparser.getRegister("#USER"),
								0,
								qquest.getString("awardre"),
								"Belohnung Quest '"+qquest.getString("qname")+"'",
								1 );
					}
					call( new CompleteQuest(), db, scriptparser, scriptparser.getRegister("#QUEST") );
					
					String dialogtext = null;
					if( qquest.getString("finishtext").length() == 0 ) {
						dialogtext = "Sehr gut! Du hast deine Aufgabe beendet.\nHier hast du ein paar Dinge die du sicher gut gebrauchen kannst:\n\n";
					}
					else {
						dialogtext = qquest.getString("finishtext")+"\n\n";	
					}
						
					if( qquest.getString("awarditems").length() > 0 ) {
						dialogtext += "Belohnung in Waren:\n";
						Cargo cargo = new Cargo(Cargo.Type.STRING, qquest.getString("awarditems"));
					
						ResourceList reslist = cargo.getResourceList();
						for( ResourceEntry res : reslist ) {
							dialogtext += "[resource="+res.getId()+"]"+res.getCount1()+"[/resource]\n";	
						}
						dialogtext += "\n";
					}
					if( Value.Long(qquest.getString("awardre")) != 0 ) {
						dialogtext += "Belohnung in RE: "+Common.ln(Value.Long(qquest.getString("awardre")))+"\n";	
					}
						
					call(new LoadDialog(), db, scriptparser, 
							dialogtext,
							qquest.getString("head") );
																		
					call(new AddAnswer(), db, scriptparser, 
							"Auf Wiedersehen!",
							"0" );
					
					call(new InitDialog(), db, scriptparser );
						
					call(new EndQuest(), db, scriptparser );
				}
				else {
					String dialogtext = null;
					if( qquest.getString("notyettext").length() > 0 ) {
						dialogtext = qquest.getString("notyettext");	
					}
					else {
						dialogtext = "Tut mir leid. Du hast die Aufgabe noch nicht komplett erledigt.";	
					}
					call( new LoadDialog(), db, scriptparser, 
							dialogtext,
							qquest.getString("head") );
																		
					call( new AddAnswer(), db, scriptparser, 
							"Auf Wiedersehen!",
							"0" );
					
					call( new InitDialog(), db, scriptparser );
				}
			} 
			// ende if( action.equals("end") ) 
			
			scriptparser.setRegister("#A","1");
			
			return CONTINUE;
		}
	}
}
