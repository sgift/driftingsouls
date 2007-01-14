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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

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
	Map<String,Answer> dialogAnswers = new HashMap<String,Answer>();
	
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
					
			scriptparser.out( "<table class=\"noBorderX\"><tr><td class=\"noBorderX\" valign=\"top\">" );
			scriptparser.out( "<img src=\""+Configuration.getSetting("URL")+"data/quests/"+dialogImage+"\" alt=\"\" />" );
			scriptparser.out( "</td><td class=\"noBorderX\" valign=\"top\">" );
			scriptparser.out( StringUtils.replace(text, "\n", "<br />")+"<br /><br />" );
			
			for( Answer answer : dialogAnswers.values() ) {	
				scriptparser.out( "<a class=\"forschinfo\" href=\""+answer.url+"\">"+answer.text+"</a><br />" );
			}
			scriptparser.out( "</td></tr></table>" );
			
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
					User owner = ContextMap.getContext().createUserObject(ship.getInt("owner"));
					UserFlagschiffLocation flagschiff = owner.getFlagschiff();
					val = (flagschiff.getID() == ship.getInt("id"));
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
		
			if( addparams.length() == 0 ) {
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
			// TODO
			Common.stub();
			
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
			int noreuse = Integer.parseInt(command[2]);
			
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
				//$execdata = $scriptparser->getExecutionData();
				
				scriptparser.executeScript( db, runningdata.getString("uninstall"), "0" );
				
				//$scriptparser->setExecutionData($execdata);
			}
			
			db.update("DELETE FROM ".SQL_TBL_QUESTS_RUNNING." WHERE id='".$runningdata['id']."'");
			
			scriptparser.setRegister("QUEST","");
			
			return CONTINUE;
		}
	}
	
	class GetQuestID implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class InstallHandler implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class RemoveHandler implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	
	class AddUninstallCmd implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class CompleteQuest implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class HasQuestCompleted implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class SetQuestUIStatus implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class SaveOutput implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class LoadQuestContext implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class SaveQuestContext implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
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
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class HasQuestItem implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class AddItem implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class HasResource implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class GetResource implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class AddResource implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class TransferWholeCargo implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
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
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class UnlockShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class AddQuestShips implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class AddShips implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class RemoveShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class MoveShip implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class IsShipDestroyed implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class AddLootTable implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class DeleteLootTable implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
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
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class GetMoney implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class CloneOffizier implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class RemoveOffizier implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class StartBattle implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class AddBattleVisibility implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class EndBattle implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class GetNoobStatus implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class GetUserValue implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			int userid = Integer.parseInt(command[1]);
			scriptparser.log("userid: "+userid+"\n");
			
			String valuename = command[2];
			scriptparser.log("value(key): "+valuename+"\n");
			
			User user = ContextMap.getContext().createUserObject(userid);
			
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
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class GetSectorProperty implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class GetSystemProperty implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
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
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	/*—---------------------------------------------
	 * 
	 * 	Quick-Quests
	 *
	 ----------------------------------------------*/
	
	class GenerateQuickQuestSourceMenu implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class GenerateQuickQuestTargetMenu implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
	
	class HandleQuickQuestEvent implements SPFunction {
		public boolean[] execute( Database db, ScriptParser scriptparser, String[] command ) {
			// TODO
			Common.stub();
			
			return CONTINUE;
		}
	}
}
