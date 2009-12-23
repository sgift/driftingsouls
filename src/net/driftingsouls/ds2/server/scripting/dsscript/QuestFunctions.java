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
package net.driftingsouls.ds2.server.scripting.dsscript;

import java.io.IOException;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.SectorTemplateManager;
import net.driftingsouls.ds2.server.Offizier.Ability;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.entities.VersteigerungResource;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.ShipUtils;
import net.driftingsouls.ds2.server.scripting.entities.Answer;
import net.driftingsouls.ds2.server.scripting.entities.Quest;
import net.driftingsouls.ds2.server.scripting.entities.QuickQuest;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.scripting.entities.Text;
import net.driftingsouls.ds2.server.ships.Ship;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Scriptbefehle fuer Questscripte.
 * @author Christopher Jung
 *
 */
@Configurable
public class QuestFunctions {
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
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
	
	static class QuestAnswer {
		String url;
		String text;
		
		QuestAnswer(String text, String url) {
			this.text = text;
			this.url = url;
		}
	}
	
	// Diese Daten sind bei jeder ScriptParser-Instanz individuell!
	String dialogText = "";
	String dialogImage = "";
	Map<String,QuestAnswer> dialogAnswers = new LinkedHashMap<String,QuestAnswer>();
	
	class LoadDialog implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			Text dialogdata = null;
			
			if( NumberUtils.isNumber(command[1]) ) {
				int textid = Integer.parseInt(command[1]);
				scriptparser.log("dialog: "+textid+"\n");
			
				dialogdata = (Text)db.get(Text.class, textid);
			}
			else {
				dialogdata = new Text(command[1]);
				dialogdata.setPicture(command[2]);
				
				scriptparser.log("dialog: Parameter1/2\n");
			}
			dialogText = dialogdata.getText();
			dialogImage = dialogdata.getPicture();
			
			return CONTINUE;
		}
	}
	
	class InitDialog implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			BBCodeParser bbcodeparser = BBCodeParser.getInstance();
			
			String text = bbcodeparser.parse(dialogText);
			
			ScriptContext context = scriptparser.getContext();
			try {
				context.getWriter().append( "<table class=\"noBorderX\"><tr><td class=\"noBorderX\" valign=\"top\">" );
				context.getWriter().append( "<img src=\""+config.get("URL")+"data/quests/"+dialogImage+"\" alt=\"\" />" );
				context.getWriter().append( "</td><td class=\"noBorderX\" valign=\"top\">" );
				context.getWriter().append( StringUtils.replace(text, "\n", "<br />")+"<br /><br />" );
				
				for( QuestAnswer answer : dialogAnswers.values() ) {	
					context.getWriter().append( "<a class=\"forschinfo\" href=\""+answer.url+"\">"+answer.text+"</a><br />" );
				}
				context.getWriter().append( "</td></tr></table>" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
			
			return CONTINUE;
		}
	}
	
	static class CopyVar implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			Database database = ContextMap.getContext().getDatabase();
			
			scriptparser.log("register: "+command[1]+"\n");
			scriptparser.log("value: "+command[2]+"\n");
			if( command.length > 3 ) {
				scriptparser.log("reg4value: "+command[3]+"\n");	
			}
			
			Object val = null;
			
			String[] value = StringUtils.split(command[2], '.');
			if( value[0].equals("shipsource") ) {
				Ship ship = null;
				if( command.length == 3 ) {
					ship = scriptparser.getShip();
				}
				else if( scriptparser.getRegisterObject(command[3]) instanceof Ship ) {
					ship = (Ship)scriptparser.getRegisterObject(command[3]);
				}
				else {
					int shipID = Integer.parseInt(scriptparser.getRegister(command[3]));
					ship = (Ship)db.get(Ship.class, shipID);
				}
				
				if( (value.length <= 1) || (value[1].length() == 0) ) {
					val = ship;
				}
				if( value[1].equals("cargo") ) {
					val = ship.getCargo();
				}
				else if( value[1].equals("offizier") ) {
					val = Offizier.getOffizierByDest('s', ship.getId());
				}
				else if( value[1].equals("flagschiff") ) {
					User owner = ship.getOwner();
					UserFlagschiffLocation flagschiff = owner.getFlagschiff();
					val = (flagschiff != null) && (flagschiff.getID() == ship.getId());
				}
				else if( value[1].equals("id") ) {
					val = ship.getId();	
				}
				else if( value[1].equals("type") ) {
					val = ship.getType();	
				}
				else if( value[1].equals("name") ) {
					val = ship.getName();	
				}
				else if( value[1].equals("x") ) {
					val = ship.getX();	
				}
				else if( value[1].equals("y") ) {
					val = ship.getY();	
				}
				else if( value[1].equals("system") ) {
					val = ship.getSystem();	
				}
				else if( value[1].equals("status") ) {
					val = ship.getStatus();	
				}
				else if( value[1].equals("crew") ) {
					val = ship.getCrew();	
				}
				else if( value[1].equals("e") ) {
					val = ship.getEnergy();	
				}
				else if( value[1].equals("s") ) {
					val = ship.getHeat();	
				}
				else if( value[1].equals("hull") ) {
					val = ship.getHull();	
				}
				else if( value[1].equals("shields") ) {
					val = ship.getShields();	
				}
				else if( value[1].equals("heat") ) {
					val = ship.getWeaponHeat();	
				}
				else if( value[1].equals("engine") ) {
					val = ship.getEngine();	
				}
				else if( value[1].equals("weapons") ) {
					val = ship.getWeapons();	
				}
				else if( value[1].equals("comm") ) {
					val = ship.getComm();	
				}
				else if( value[1].equals("sensors") ) {
					val = ship.getSensors();	
				}
				else if( value[1].equals("docked") ) {
					val = ship.getDocked();	
				}
				else if( value[1].equals("alarm") ) {
					val = ship.getAlarm();	
				}
				else if( value[1].equals("fleet") ) {
					val = (ship.getFleet() != null ? ship.getFleet().getId() : 0);	
				}
				else if( value[1].equals("destsystem") ) {
					val = ship.getDestSystem();	
				}
				else if( value[1].equals("destx") ) {
					val = ship.getDestX();	
				}
				else if( value[1].equals("desty") ) {
					val = ship.getDestY();	
				}
				else if( value[1].equals("destcom") ) {
					val = ship.getDestCom();	
				}
				else if( value[1].equals("bookmark") ) {
					val = ship.isBookmark();	
				}
				else if( value[1].equals("battle") ) {
					val = ship.getBattle();	
				}
				else if( value[1].equals("battleaction") ) {
					val = ship.isBattleAction();	
				}
				else if( value[1].equals("jumptarget") ) {
					val = ship.getJumpTarget();	
				}
				else if( value[1].equals("autodeut") ) {
					val = ship.getAutoDeut();	
				}
				else if( value[1].equals("history") ) {
					val = ship.getHistory();	
				}
				else if( value[1].equals("script") ) {
					val = ship.getScript();	
				}
				else if( value[1].equals("scriptexedata") ) {
					val = ship.getScriptExeData();	
				}
				else if( value[1].equals("oncommunicate") ) {
					val = ship.getOnCommunicate();	
				}
				else if( value[1].equals("lock") ) {
					val = ship.getLock();
				}
				else if( value[1].equals("visibility") ) {
					val = ship.getVisibility();
				}
				else if( value[1].equals("onmove") ) {
					val = ship.getOnMove();
				}
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
					val = offi.getOwner().getId();
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
					base = database.first("SELECT id,name,owner,x,y,system,cargo,maxcargo,klasse FROM bases WHERE id=",baseID);
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
					jn = database.first("SELECT id,name,owner,x,y,system,cargo,maxcargo,klasse FROM bases WHERE id=",jnID);
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
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
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
				Answer answerRow = (Answer)db.get(Answer.class, answerIdNumber);
				if( answerRow == null ) {
					answer = "Unbekannte Antwort &gt;"+answerid+"&lt;";
					scriptparser.log("ERROR: Unbekannte Antwort &gt;"+answerid+"&lt;\n");
				}
				else {
					answer = answerRow.getText();
				}
			}
		
			if( addparams.length() > 0 ) {
				internalid += ":"+addparams;
			}
			
			String url = Quests.buildQuestURL( internalid );
			
			dialogAnswers.put(answerid, new QuestAnswer(answer, url));
			
			return CONTINUE;
		}
	}
	
	class SetAnswerURL implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	static class SaveVar implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			Database database = ContextMap.getContext().getDatabase();
			
			scriptparser.log("value: "+command[1]+"\n");
			scriptparser.log("register: "+command[2]+"\n");
			Object val = scriptparser.getRegisterObject(command[2]);
			if( (command.length > 3) && (command[3].length() > 0) ) {
				scriptparser.log("reg4value: "+command[3]+"\n");	
			}
			
			String[] value = StringUtils.split(command[1], '.');
			if( value[0].equals("shipsource") ) {
				Ship ship = scriptparser.getShip();
				if( (command.length > 3) && (command[3].length() > 0) ) {
					Object shipObj = scriptparser.getRegister(command[3]);
					ship = (Ship)db.get(Ship.class, Value.Int(shipObj.toString()));

				}
				
				if( value[1].equals("cargo") ) {
					ship.setCargo((Cargo)val);
				}
				else if( value[1].equals("name") ) {
					ship.setName(val.toString());	
				}
				else if( value[1].equals("x") ) {
					ship.setX(Value.Int(val.toString()));	
				}
				else if( value[1].equals("y") ) {
					ship.setY(Value.Int(val.toString()));	
				}
				else if( value[1].equals("system") ) {
					ship.setSystem(Value.Int(val.toString()));	
				}
				else if( value[1].equals("status") ) {
					ship.setStatus(val.toString());	
				}
				else if( value[1].equals("crew") ) {
					ship.setCrew(Value.Int(val.toString()));	
				}
				else if( value[1].equals("e") ) {
					ship.setEnergy(Value.Int(val.toString()));	
				}
				else if( value[1].equals("s") ) {
					ship.setHeat(Value.Int(val.toString()));	
				}
				else if( value[1].equals("hull") ) {
					ship.setHull(Value.Int(val.toString()));		
				}
				else if( value[1].equals("shields") ) {
					ship.setShields(Value.Int(val.toString()));		
				}
				else if( value[1].equals("heat") ) {
					ship.setWeaponHeat(val.toString());	
				}
				else if( value[1].equals("engine") ) {
					ship.setEngine(Value.Int(val.toString()));	
				}
				else if( value[1].equals("weapons") ) {
					ship.setWeapons(Value.Int(val.toString()));	
				}
				else if( value[1].equals("comm") ) {
					ship.setComm(Value.Int(val.toString()));	
				}
				else if( value[1].equals("sensors") ) {
					ship.setSensors(Value.Int(val.toString()));	
				}
				else if( value[1].equals("docked") ) {
					ship.setDocked(val.toString());	
				}
				else if( value[1].equals("alarm") ) {
					ship.setAlarm(Value.Int(val.toString()));	
				}
				else {
					throw new UnsupportedOperationException("Setzen der Eigenschaft '"+value[1]+"' nicht unterstuetzt");
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
					baseObj = database.first("SELECT * FROM bases WHERE id="+Value.Int(baseObj.toString()));
				}
				SQLResultRow base = (SQLResultRow)baseObj;
				
				if( !value[1].equals("cargo") ) {
					base.put(value[1].trim(), val);
					database.prepare("UPDATE bases SET `?`= ? WHERE id>0 AND id= ?")
						.update(value[1].trim(), val, base.getInt("id"));
				}
				else {
					base.put("cargo", ((Cargo)val).save());
					database.update("UPDATE base SET `cargo`='"+base.getString("cargo")+"' WHERE id>0 AND id="+base.getInt("id"));
				}
			}
			
			return CONTINUE;
		}
	}
	
	class SetDialogTextVar implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			String myvar = command[1];
			String replace = command[2];
			
			scriptparser.log("var: "+myvar+"\n");
			scriptparser.log("replace: "+replace+"\n");
			
			dialogText = StringUtils.replace(dialogText, "{"+myvar+"}", replace );
			
			return CONTINUE;
		}
	}
	
	static class InitQuest implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			String questid = command[1];
			int noreuse = 0;
			if( command.length > 2 ) {
				noreuse = Value.Int(command[2]);
			}
			
			scriptparser.log("questid: "+questid+"\n");
			
			int userid = Integer.parseInt(scriptparser.getRegister("USER"));
			
			RunningQuest runningQuest = null;
			if( (noreuse == 0) && (questid.charAt(0) != 'r') ) {
				runningQuest = (RunningQuest)db.createQuery("from RunningQuest where quest=:quest and user=:user")
					.setInteger("quest", Integer.parseInt(questid))
					.setInteger("user", userid)
					.setMaxResults(1)
					.uniqueResult();
			}
			else if( noreuse == 0 ) {
				String rquestid = questid.substring(1);
				runningQuest = (RunningQuest)db.get(RunningQuest.class, Integer.parseInt(rquestid));
			}
			
			if( runningQuest == null ) {
				Quest quest = (Quest)db.get(Quest.class, Integer.parseInt(questid));
				User user = (User)db.get(User.class, userid);
				runningQuest = new RunningQuest(quest,user);
				db.save(runningQuest);
			}
			
			scriptparser.setRegister("QUEST","r"+runningQuest.getId());
			
			return CONTINUE;
		}
	}
	
	static class EndQuest implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			String questid = scriptparser.getRegister("QUEST");
			int userid = Integer.parseInt(scriptparser.getRegister("USER"));
			
			RunningQuest runningQuest = null;
			if( questid.charAt(0) != 'r' ) {
				runningQuest = (RunningQuest)db.createQuery("from RunningQuest where quest=:quest and user=:user")
					.setInteger("quest", Integer.parseInt(questid))
					.setInteger("user", userid)
					.setMaxResults(1)
					.uniqueResult();
			}
			else {
				String rquestid = questid.substring(1);
				runningQuest = (RunningQuest)db.get(RunningQuest.class, Integer.parseInt(rquestid));
			}
						
			// ggf. das Quest "deinstallieren" (handler entfernen)
			if( runningQuest.getUninstall() != null ) {
				ScriptContext context = scriptparser.getContext();
				
				scriptparser.setContext(new ScriptParserContext());
				scriptparser.getContext().setAttribute("_PARAMETERS", "0", ScriptContext.ENGINE_SCOPE);
				try {
					scriptparser.eval( runningQuest.getUninstall() );
				}
				catch( ScriptException e ) {
					throw new RuntimeException(e);
				}
				
				scriptparser.setContext(context);
			}
			
			db.delete(runningQuest);
			
			scriptparser.setRegister("QUEST","");
			
			return CONTINUE;
		}
	}
	
	static class GetQuestID implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			String rquestid = command[1];
			
			scriptparser.log("rquestid: "+rquestid+"\n");
			
			rquestid = rquestid.substring(1);
			RunningQuest runningQuest = (RunningQuest)db.get(RunningQuest.class, Value.Int(rquestid));
			
			if( runningQuest == null ) {
				scriptparser.log("rquestid ist ungueltig!\n");
				scriptparser.setRegister("A", "0");
			}
			else {
				scriptparser.setRegister("A",Integer.toString(runningQuest.getQuest().getId()));
			}
			
			return CONTINUE;
		}
	}
	
	static class InstallHandler implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class RemoveHandler implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class AddUninstallCmd implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
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
			
			RunningQuest runningQuest = null;
			if( questid.charAt(0) != 'r' ) {
				runningQuest = (RunningQuest)db.createQuery("from RunningQuest where quest=:quest and user=:user")
					.setInteger("quest", Value.Int(questid))
					.setInteger("user", userid)
					.setMaxResults(1)
					.uniqueResult();
			}
			else {
				String rquestid = questid.substring(1);
				runningQuest = (RunningQuest)db.get(RunningQuest.class, Integer.parseInt(rquestid));
			}
			
			if( runningQuest != null ) {
				String uninstall = runningQuest.getUninstall();
				
				if( uninstall == null ) {
					uninstall = ":0\n";	
				}
				uninstall += removescript.toString();
					
				runningQuest.setUninstall(uninstall);
			}
			else {
				scriptparser.log("WARNUNG: keine quest_running-data gefunden\n");	
			}
			
			return CONTINUE;
		}
	}
	
	static class CompleteQuest implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class HasQuestCompleted implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class SetQuestUIStatus implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class SaveOutput implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			// Funktioniert nur mit StringWriter und co.
			scriptparser.setRegister("_OUTPUT",scriptparser.getContext().getWriter().toString());
			
			return CONTINUE;
		}
	}
	
	class LoadQuestContext implements SPFunction {
		private final Log log = LogFactory.getLog(LoadQuestContext.class);
		
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
					Object ip = scriptparser.getContext().getAttribute("__INSTRUCTIONPOINTER");
					
					Blob blob = questdata.getBlob("execdata"); 
					scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE).putAll(
							ScriptParserContext.fromStream(blob.getBinaryStream()).getBindings(ScriptContext.ENGINE_SCOPE)
					);
					
					scriptparser.getContext().setAttribute("__INSTRUCTIONPOINTER", ip, ScriptContext.ENGINE_SCOPE);
					scriptparser.setRegister("QUEST","r"+questdata.getInt("id"));
				}
				catch( Exception e ) {
					scriptparser.log("Fehler: Konnte Questdaten nicht laden: "+e+"\n");
					scriptparser.setRegister("QUEST","r"+questdata.getInt("id"));
					log.warn("Fehler beim Laden der Questdaten (Quest: "+questid+"): "+e,e);
				}
			}
			questdata.free();
			
			return CONTINUE;
		}
	}
	
	class SaveQuestContext implements SPFunction {
		private final Log log = LogFactory.getLog(SaveQuestContext.class);
		
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
					ScriptParserContext.toStream(scriptparser.getContext(), blob.setBinaryStream(1));
					db.prepare("UPDATE quests_running SET execdata=? WHERE id=? ")
						.update(blob, questdata.getInt("id"));
				}
				catch( Exception e ) {
					scriptparser.log("Fehler: Konnte Questdaten nicht schreiben: "+e+"\n");
					scriptparser.setRegister("QUEST","r"+questdata.getInt("id"));
					log.warn("Fehler beim Schreiben der Questdaten (Quest: "+questid+"): "+e,e);
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
	
	static class AddQuestItem implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class HasQuestItem implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class AddItem implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	static class HasResource implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	static class GetResource implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	static class AddResource implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	static class TransferWholeCargo implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	static class LockShip implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class UnlockShip implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class AddQuestShips implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class AddShips implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class RemoveShip implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			Ship ship = (Ship)db.get(Ship.class, Integer.parseInt(command[1]));
			ship.destroy();
			
			return CONTINUE;
		}
	}
	
	static class MoveShip implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			int shipid = Value.Int(command[1]);
			scriptparser.log("shipid: "+shipid+"\n");
			
			Location target = Location.fromString(command[2]);
			scriptparser.log("target: "+target+"\n");
				
			Ship ship = (Ship)db.get(Ship.class, shipid);
			
			if( !ShipUtils.move(ship, target, Integer.MAX_VALUE) ) {
				scriptparser.log(ShipUtils.MESSAGE.getMessage());
				scriptparser.log("\n");
				
				return STOP;
			}
			scriptparser.setRegister("A", "1");
			
			scriptparser.log(ShipUtils.MESSAGE.getMessage());			
			scriptparser.log("\n");
			
			return CONTINUE;
		}
	}
	
	static class IsShipDestroyed implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class AddLootTable implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class DeleteLootTable implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class Msg implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			int from = Integer.parseInt(command[1]);
			int to = Integer.parseInt(command[2]);
			
			scriptparser.log("sender: "+from+"\n");
			scriptparser.log("receiver: "+to+"\n");
			
			String title = command[3];
			scriptparser.log("title: "+title+"\n");  
			
			String msg = command[4];
			scriptparser.log("msg: "+msg+"\n\n");
			
			User source = (User)ContextMap.getContext().getDB().get(User.class, -1);
			PM.send( source, to, title, msg );
			
			return CONTINUE;
		}
	}
	
	static class AddMoney implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	static class GetMoney implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			int userid = Value.Int(command[1]);
			scriptparser.log("userid: "+userid+"\n");
			
			User user = (User)ContextMap.getContext().getDB().get(User.class, userid);
			scriptparser.setRegister("A",user.getKonto().toString());
			
			return CONTINUE;
		}
	}
	
	static class CloneOffizier implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			int offiid = Value.Int(command[1]);
			scriptparser.log("offiid: "+offiid+"\n");
			
			String desttype = command[2];
			scriptparser.log("desttype: "+desttype+"\n");
			
			int destid = Value.Int(command[3]);
			scriptparser.log("destid: "+destid+"\n");
			
			Offizier baseOffi = (Offizier)db.get(Offizier.class, offiid);
			if( baseOffi == null ) {
				scriptparser.log("Warnung: Offizier konnte nicht gefunden werden");
				return CONTINUE;
			}
			
			User destowner = null;
			if( desttype.equals("s") ) {
				Ship destobj = (Ship)db.get(Ship.class, destid);
				destowner = destobj.getOwner();;
			}
			else if( desttype.equals("b") ) {
				Base destobj = (Base)db.get(Base.class, destid);
				destowner = destobj.getOwner();;
			}
			
			Offizier offizier = new Offizier(destowner, baseOffi.getName());
			offizier.setDest(desttype, destid);
			for( Ability ability : Offizier.Ability.values() ) {
				offizier.setAbility(ability, baseOffi.getAbility(ability));
			}
			offizier.setSpecial(baseOffi.getSpecial());
			
			int newoffiid = (Integer)db.save(offizier);
			
			scriptparser.setRegister("A",Integer.toString(newoffiid));
				
			if( desttype.equals("s") ) {
				Ship destobj = (Ship)db.get(Ship.class, destid);
				destobj.recalculateShipStatus();	
			}
			
			return CONTINUE;
		}
	}
	
	static class RemoveOffizier implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			int offiid = Value.Int(command[1]);
			scriptparser.log("offiid: "+offiid+"\n");
			
			Offizier offi = (Offizier)db.get(Offizier.class, offiid);
			if( offi == null ) {
				return CONTINUE;	
			}
			String[] destArray = offi.getDest();
			
			db.delete(offi);
			
			if( destArray[0].equals("s") ) {
				Ship destobj = (Ship)db.get(Ship.class, Value.Int(destArray[1]));
				destobj.recalculateShipStatus();	
			}
			
			return CONTINUE;
		}
	}
	
	static class StartBattle implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			int attacker = Value.Int(command[1]);
			scriptparser.log("attacker(ship): "+attacker+"\n");
			
			int defender = Value.Int(command[2]);
			scriptparser.log("defender(ship): "+defender+"\n");
			
			int questbattle = Value.Int(command[3]);
			scriptparser.log("questbattle: "+questbattle+"\n");
				
			Ship attackerShip = (Ship)db.get(Ship.class, attacker);
			if( (attackerShip == null) || (attackerShip.getId() < 0) ) {
				return CONTINUE;
			}
			
			Ship defenderShip = (Ship)db.get(Ship.class, defender);
			if( (defenderShip == null) || (defenderShip.getId() < 0) ) {
				return CONTINUE;
			}
			
			Battle battle = Battle.create( attackerShip.getOwner().getId(), attacker, defender );
			
			scriptparser.setRegister("A", Integer.toString(battle.getId()) );
			
			if( questbattle != 0 ) {
				String questid = scriptparser.getRegister("QUEST");
				if( questid.charAt(0) != 'r' ) {
					int userid = Value.Int(scriptparser.getRegister("USER"));
					RunningQuest runningQuest = (RunningQuest)db.createQuery("from RunningQuest where quest=:quest and user=:user")
						.setInteger("quest", Integer.parseInt(questid))
						.setInteger("user", userid)
						.setMaxResults(1)
						.uniqueResult();
					
					questid = Integer.toString(runningQuest.getId());
				}
				else {
					questid = questid.substring(1);
				}
				
				battle.setQuest(Value.Int(questid));
				battle.addToVisibility(attackerShip.getOwner().getId());
				battle.addToVisibility(defenderShip.getOwner().getId());	
			}
			
			return CONTINUE;
		}
	}
	
	
	static class AddBattleVisibility implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			int battleid = Value.Int(command[1]);
			scriptparser.log("battleid: "+battleid+"\n");
			
			int userid = Value.Int(command[2]);
			scriptparser.log("userid: "+userid+"\n");
			
			Battle battle = (Battle)db.get(Battle.class, battleid);
			battle.addToVisibility(userid);
			
			return CONTINUE;
		}
	}
	
	
	static class EndBattle implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			int battleid = Value.Int(command[1]);
			scriptparser.log("battleid: "+battleid+"\n");
			
			int side1points = Value.Int(command[2]);
			scriptparser.log("side1points: "+side1points+"\n");
			
			int side2points = Value.Int(command[3]);
			scriptparser.log("side2points: "+side2points+"\n");
			
			int executescripts = Value.Int(command[4]);
			scriptparser.log("executescripts: "+executescripts+"\n");
				
			Battle battle = (Battle)db.get(Battle.class, battleid);
			battle.load( battle.getCommander(0), null, null, 1 );
			battle.endBattle( side1points, side2points, executescripts != 0 );
			
			return CONTINUE;
		}
	}
	
	
	static class GetNoobStatus implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	
	
	static class GetUserValue implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	
	static class SetUserValue implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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
	
	
	static class GetSectorProperty implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			Database database = ContextMap.getContext().getDatabase();
			
			Location sector = Location.fromString(command[1]);
			scriptparser.log("sector: "+sector+"\n");
			
			String property = command[2];
			scriptparser.log("property: "+property+"\n");
			
			List<String> result = new ArrayList<String>();
			
			String locSQL = "system="+sector.getSystem()+" AND x="+sector.getX()+" AND y="+sector.getY();
			if( property.equals("nebel") ) {
				Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(sector));
				if( nebel != null ) {
					result.add(nebel.getLocation().toString());
				}
			} 
			else if( property.equals("bases") ) {
				List<?> baseIds = db.createQuery("select id from Base where system= :sys and x= :x and y= :y")
					.setInteger("sys", sector.getSystem())
					.setInteger("x", sector.getX())
					.setInteger("y", sector.getY())
					.list();
				for( Iterator<?> iter=baseIds.iterator(); iter.hasNext(); ) {
					Integer id = (Integer)iter.next();
					result.add(id.toString());	
				}
			}
			else if( property.equals("jumpnodes") ) {
				SQLQuery jn = database.query("SELECT id FROM jumpnodes WHERE "+locSQL);
				while( jn.next() ) {
					result.add(Integer.toString(jn.getInt("id")));	
				}
				jn.free();
			}
			else if( property.equals("ships") ) {
				SQLQuery ship = database.query("SELECT id FROM ships WHERE id>0 AND "+locSQL);
				while( ship.next() ) {
					result.add(Integer.toString(ship.getInt("id")));	
				}
				ship.free();
			}
			else if( property.equals("shipsByOwner") ) {
				int owner = Value.Int(command[3]);
				scriptparser.log("owner: "+owner+"\n");
				
				SQLQuery ship = database.query("SELECT id FROM ships WHERE id>0 AND "+locSQL+" AND owner="+owner);
				while( ship.next() ) {
					result.add(Integer.toString(ship.getInt("id")));	
				}
				ship.free();
			}
			else if( property.equals("shipsByTag") ) {
				String tag = command[3];
				scriptparser.log("tag: "+tag+"\n");
				
				SQLQuery ship = database.prepare("SELECT id FROM ships WHERE id>0 AND "+locSQL+" AND LOCATE( ? ,status)")
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
	
	
	static class GetSystemProperty implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class GtuAuctionShip implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			Database db = ContextMap.getContext().getDatabase();
			
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
	
	static class GtuAuctionCargo implements SPFunction {
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
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
			
			VersteigerungResource versteigerung = new VersteigerungResource((User)db.get(User.class, owner), cargo, initbid);
			db.persist(versteigerung);
			
			return CONTINUE;
		}
	}
	
	/*—---------------------------------------------
	 * 
	 * 	Quick-Quests
	 *
	 ----------------------------------------------*/
	
	class GenerateQuickQuestSourceMenu implements SPFunction {
		private final Log log = LogFactory.getLog(GenerateQuickQuestSourceMenu.class);
		
		private void call( SPFunction f, ScriptParser scriptparser, Object ... cmd) {
			scriptparser.log("-> "+f.getClass().getSimpleName()+"\n");
			String[] command = new String[cmd.length+1];
			command[0] = "!"+f.getClass().getSimpleName();
			for( int i=0; i < cmd.length; i++ ) {
				command[i+1] = cmd[i].toString();
			}
			
			f.execute(scriptparser, command);
		}
		
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			String typeid = command[1];
			scriptparser.log("TypeID: "+typeid+"\n");
			
			int shipid = Integer.parseInt(command[2]);
			scriptparser.log("ShipID: "+shipid+"\n");
		
			List<?> qquestList = db.createQuery("from QuickQuest " +
					"where enabled>0 and " +
					"(source= :shipid or source like :expshipid1 or source like :expshipid2 or source like :expshipid3) and " + 
					"sourcetype= :type")
				.setString("shipid", Integer.toString(shipid))
				.setString("expshipid1", "%"+shipid)
				.setString("expshipid2", "%"+shipid+"%")
				.setString("expshipid3", shipid+"%")
				.setString("type", typeid)
				.list();
			for( Iterator<?> iter=qquestList.iterator(); iter.hasNext(); ) {
				QuickQuest qquest = (QuickQuest)iter.next();
				
				if( !qquest.getMoreThanOnce() ) {
					call(new HasQuestCompleted(), scriptparser, qquest.getEnabled());
					if( Integer.parseInt(scriptparser.getRegister("#cmp")) > 0 ) {
						continue;
					}
				}
				if( qquest.getDependsOnQuests().length() > 0 ) {
					boolean ok = true;
					
					String[] qquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
					for( int i=0; i < qquests.length; i++ ) {
						String[] tmp = StringUtils.split(qquests[i], ':');
						Quest quest = (Quest)db.createQuery("from Quest where qid= :qid")
							.setString("qid", tmp[1])
							.uniqueResult();
						if( quest == null ) {
							log.warn("QQuest "+qquest.getId()+" benoetigt Quest "+tmp[1]+", welches jedoch nicht existiert");
							continue;
						}
		
						call(new HasQuestCompleted(), scriptparser, quest.getId());
						if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
							ok = false;
							break;
						}
					}	
					if( !ok ) {
						continue;
					}
				}
				call(new LoadQuestContext(), scriptparser, qquest.getEnabled());
							
				if( Value.Int(scriptparser.getRegister("#QSTATUS")) > 0 ) {
					continue;
				}
				
				call(new AddAnswer(), scriptparser, 
						"Auftrag &gt;"+qquest.getQName()+"&lt",
						"_quick_quests",
						"desc",
						qquest.getId());
			}
			
			return CONTINUE;
		}
	}
	
	
	class GenerateQuickQuestTargetMenu implements SPFunction {
		private void call( SPFunction f, ScriptParser scriptparser, Object ... cmd) {
			scriptparser.log("-> "+f.getClass().getSimpleName()+"\n");
			String[] command = new String[cmd.length+1];
			command[0] = "!"+f.getClass().getSimpleName();
			for( int i=0; i < cmd.length; i++ ) {
				command[i+1] = cmd[i].toString();
			}
			
			f.execute(scriptparser, command);
		}
		
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			String typeid = command[1];
			scriptparser.log("TypeID: "+typeid+"\n");
			
			int shipid = Integer.parseInt(command[2]);
			scriptparser.log("ShipID: "+shipid+"\n");
			
			List<?> qquestList = db.createQuery("from QuickQuest " +
					"where enabled>0 and " +
					"(target= :shipid or target like :expshipid1 or target like :expshipid2 or target like :expshipid3) and " + 
					"targettype= :type")
				.setString("shipid", Integer.toString(shipid))
				.setString("expshipid1", "%"+shipid)
				.setString("expshipid2", "%"+shipid+"%")
				.setString("expshipid3", shipid+"%")
				.setString("type", typeid)
				.list();
			for( Iterator<?> iter=qquestList.iterator(); iter.hasNext(); ) {
				QuickQuest qquest = (QuickQuest)iter.next();
				
				call(new LoadQuestContext(), scriptparser, qquest.getEnabled());
				
				if( scriptparser.getRegister("#QUEST").length() == 0 || 
					scriptparser.getRegister("#QUEST").equals("0")  ) {
					continue;
				}

				int rquestid = Integer.parseInt(scriptparser.getRegister("#QUEST").substring(1));
				RunningQuest rquest = (RunningQuest)db.get(RunningQuest.class, rquestid);
				if( rquest.getQuest().getId() != qquest.getEnabled() ) {
					continue;
				}
				
				if( Value.Int(scriptparser.getRegister("#QSTATUS")) != 1 ) {
					continue;
				}
				
				call(new AddAnswer(), scriptparser, 
						"Auftrag &gt;"+qquest.getQName()+"&lt beenden",
						"_quick_quests",
						"end",
						qquest.getId());
			}
			
			return CONTINUE;
		}
	}
	
	
	class HandleQuickQuestEvent implements SPFunction {
		private final Log log = LogFactory.getLog(HandleQuickQuestEvent.class);
		
		private void call( SPFunction f, ScriptParser scriptparser, Object ... cmd) {
			scriptparser.log("-> "+f.getClass().getSimpleName()+"\n");
			String[] command = new String[cmd.length+1];
			command[0] = "!"+f.getClass().getSimpleName();
			for( int i=0; i < cmd.length; i++ ) {
				command[i+1] = cmd[i].toString();
			}
			
			f.execute(scriptparser, command);
		}
		
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			String typeid = command[1];
			scriptparser.log("TypeID: "+typeid+"\n");
			
			int shipid = Integer.parseInt(command[2]);
			scriptparser.log("ShipID: "+shipid+"\n");
			
			scriptparser.log("Action: "+scriptparser.getParameter(0)+"\n");
			
			String action = scriptparser.getParameter(0);
			if( "desc".equals(action) ) {
				scriptparser.log("QQuest: "+scriptparser.getParameter(1)+"\n");
				
				QuickQuest qquest = (QuickQuest)db.get(QuickQuest.class, Value.Int(scriptparser.getParameter(1)));
				
				if( (qquest == null) || !qquest.getSourceType().equals(typeid) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				int[] sourcelist = Common.explodeToInt(",", qquest.getSource());
				if( !Common.inArray(shipid, sourcelist) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				call(new LoadQuestContext(), scriptparser, qquest.getEnabled());
				call(new GetQuestID(), scriptparser, scriptparser.getRegister("#QUEST"));
				
				if( Value.Int(scriptparser.getRegister("#A")) == qquest.getEnabled() ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;	
				}
					
				if( !qquest.getMoreThanOnce() ) {
					call(new HasQuestCompleted(), scriptparser, qquest.getEnabled());
					if( Value.Int(scriptparser.getRegister("#cmp")) > 0 ) {
						scriptparser.setRegister("#A","0");
						return CONTINUE;
					}
				}
				
				if( qquest.getDependsOnQuests().length() > 0 ) {
					String[] qquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
					for( int i=0; i < qquests.length; i++ ) {
						String[] tmp = StringUtils.split(qquests[i], ':');
						Quest quest = (Quest)db.createQuery("from Quest where qid= :qid")
							.setString("qid", tmp[1])
							.uniqueResult();
						if( quest == null ) {
							log.warn("QQuest "+qquest.getId()+" benoetigt Quest "+tmp[1]+", welches jedoch nicht existiert");
							continue;
						}
		
						call(new HasQuestCompleted(), scriptparser, quest.getId());
						if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
							scriptparser.setRegister("#A","0");
							return CONTINUE;
						}
					}
				}
				
				String dialogtext = qquest.getShortDesc()+"[hr]\n"+qquest.getDescription()+"\n\n";
				if( !qquest.getReqItems().isEmpty() || qquest.getReqRe() != 0 ) {
					dialogtext += "Benötigt:[color=red]\n";
					if( !qquest.getReqItems().isEmpty() ) {
						ResourceList reslist = qquest.getReqItems().getResourceList();
						for( ResourceEntry res : reslist ) {
							dialogtext += "[resource="+res.getId()+"]"+res.getCount1()+"[/resource]\n";	
						}
					}
					if( qquest.getReqRe() != 0 ) {
						dialogtext += Common.ln(qquest.getReqRe())+" RE\n";
					}
					dialogtext += "[/color]\n\n";
				}
				if( !qquest.getAwardItems().isEmpty() ) {
					dialogtext += "Belohnung in Waren:\n";

					ResourceList reslist = qquest.getAwardItems().getResourceList();
					for( ResourceEntry res : reslist ) {
						dialogtext += "[resource="+res.getId()+"]"+res.getCount1()+"[/resource]\n";	
					}
					dialogtext += "\n";
				}
				if( qquest.getAwardRe() != 0 ) {
					dialogtext += "Belohnung in RE: "+Common.ln(qquest.getAwardRe())+"\n";	
				}
				
				call(new LoadDialog(), scriptparser, 
						dialogtext, 
						qquest.getHead());
				
				call(new AddAnswer(), scriptparser,
						"Annehmen",
						"_quick_quests",
						"yes",
						qquest.getId());
											
				call(new AddAnswer(), scriptparser,
						"Ablehnen",
						"0");
							
				call(new InitDialog(), scriptparser);
			} 
			// ende action.equals("desc")
			else if( "yes".equals(action) ) {
				scriptparser.log("QQuest: "+scriptparser.getParameter(1)+"\n");
				
				QuickQuest qquest = (QuickQuest)db.get(QuickQuest.class, Value.Int(scriptparser.getParameter(1)));
				
				if( (qquest == null) || !qquest.getSourceType().equals(typeid) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;	
				}
				
				int[] sourcelist = Common.explodeToInt(",", qquest.getSource());
				if( !Common.inArray(shipid, sourcelist) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				call(new LoadQuestContext(), scriptparser, qquest.getEnabled() );
				call(new GetQuestID(), scriptparser, scriptparser.getRegister("#QUEST") );
				if( Value.Int(scriptparser.getRegister("#A")) == qquest.getEnabled() ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
					
				if( !qquest.getMoreThanOnce() ) {
					call(new HasQuestCompleted(), scriptparser, qquest.getEnabled() );
					if( Value.Int(scriptparser.getRegister("#cmp")) > 0 ) {
						scriptparser.setRegister("#A","0");
						return CONTINUE;
					}
				}
				if( qquest.getDependsOnQuests().length() > 0 ) {
					String[] qquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
					for( int i=0; i < qquests.length; i++ ) {
						String[] tmp = StringUtils.split(qquests[i], ':');
						Quest quest = (Quest)db.createQuery("from Quest where qid= :qid")
							.setString("qid", tmp[1])
							.uniqueResult();
						if( quest == null ) {
							log.warn("QQuest "+qquest.getId()+" benoetigt Quest "+tmp[1]+", welches jedoch nicht existiert");
							continue;
						}
		
						call(new HasQuestCompleted(), scriptparser, quest.getId());
						if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
							scriptparser.setRegister("#A","0");
							return CONTINUE;
						}
					}
				}
				
				call(new InitQuest(), scriptparser, qquest.getEnabled() );
				scriptparser.setRegister("#QSTATUS","1");
				
				// Evt fuer das Quest benoetigte Items auf das Schiff transferieren
				if( !qquest.getStartItems().isEmpty() ) {
					call(new CopyVar(), scriptparser, 
							"#ship",
							"shipsource.cargo" );
					
					ResourceList reslist = qquest.getStartItems().getResourceList();
					for( ResourceEntry res : reslist ) {
						if( res.getId().isItem() ) {
							if( res.getId().getQuest() != 0 ) {
								call( new AddQuestItem(), scriptparser, 
										"#ship",
										res.getId().getItemID(),
										res.getCount1() );
							}
							else {
								call( new AddResource(), scriptparser, 
										"#ship",
										res.getId().toString(),
										res.getCount1() );
							}
						}
						else {
							call( new AddResource(), scriptparser, 
									"#ship",
									res.getId().toString(),
									res.getCount1() );
						}
					}
					call( new SaveVar(), scriptparser, 
							"shipsource.cargo",
							"#ship" );
					
					scriptparser.setRegister("#ship","0");
				}
				// Loottable ergaenzen
				if( qquest.getLoottable() != null ) {
					String[] loottable = StringUtils.split(qquest.getLoottable(), ';');
					for( int i=0; i < loottable.length; i++ ) {
						String[] atable = StringUtils.split(loottable[i], ',');
						if( atable.length > 4 ) {
							call( new AddLootTable(), scriptparser, 
									atable[0],
									atable[1],
									atable[2],
									atable[3],
									atable[4],
									atable.length > 5 ? atable[5] : "" );
						}	
					}	
				}
				scriptparser.setRegister("#quest"+qquest.getId()+"_status", qquest.getShortDesc());
				call( new SetQuestUIStatus(), scriptparser, 
						"#quest"+qquest.getId()+"_status",
						"1" );	
			} 
			// ende if( action.equals("yes")
			else if( "end".equals(action) ) {
				scriptparser.log("QQuest: "+scriptparser.getParameter(1)+"\n");
				
				QuickQuest qquest = (QuickQuest)db.get(QuickQuest.class, Value.Int(scriptparser.getParameter(1)));
				
				if( (qquest == null) || !qquest.getSourceType().equals(typeid) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;	
				}
				
				int[] sourcelist = Common.explodeToInt(",", qquest.getSource());
				if( !Common.inArray(shipid, sourcelist) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				call(new LoadQuestContext(), scriptparser, qquest.getEnabled() );
				
				if( scriptparser.getRegister("#QUEST").length() == 0 ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				int rquestid = Value.Int(scriptparser.getRegister("#QUEST").substring(1));
				RunningQuest rquest = (RunningQuest)db.get(RunningQuest.class, rquestid);
				if( (rquest == null) || (rquest.getQuest().getId() != qquest.getEnabled()) ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
					
				if( Value.Int(scriptparser.getRegister("#QSTATUS")) != 1 ) {
					scriptparser.setRegister("#A","0");
					return CONTINUE;
				}
				
				boolean quitable = true;
				
				// Die zum beenden benoetigten Items checken
				if( !qquest.getReqItems().isEmpty() ) {
					call( new CopyVar(), scriptparser, 
							"#ship",
							"shipsource.cargo" );
					
					ResourceList reslist = qquest.getReqItems().getResourceList();
					for( ResourceEntry res : reslist ) {
						if( res.getId().isItem() && (res.getId().getQuest() != 0) ) {
							call( new HasQuestItem(), scriptparser, 
									"#ship",
									res.getId().getItemID(),
									res.getCount1() );
							
							if( Value.Int(scriptparser.getRegister("#cmp")) <= 0 ) {
								quitable = false;
								break;
							}					  
						}
						else {
							call( new HasResource(), scriptparser, 
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
				
				if( quitable && (qquest.getReqRe() != 0) ) {
					call( new GetMoney(), scriptparser, scriptparser.getRegister("#USER") );
					if( Value.Int(scriptparser.getRegister("A")) < qquest.getReqRe() ) {
						quitable = false;
					}
				}
				
				// Koennen wir das Quest nun beenden oder nicht?
				if( quitable ) {
					// Die ganzen gegenstaende abbuchen
					if( !qquest.getReqItems().isEmpty() ) {
						call( new CopyVar(), scriptparser, 
								"#ship",
								"shipsource.cargo" );
						
						ResourceList reslist = qquest.getReqItems().getResourceList();
						for( ResourceEntry res : reslist ) {						
							if( res.getId().isItem() && (res.getId().getQuest() != 0) ) {
								call( new AddQuestItem(), scriptparser, 
										"#ship",
										res.getId().getItemID(),
										-res.getCount1() );
							}
							else {				  
								call( new AddResource(), scriptparser, 
										"#ship",
										res.getId(),
										-res.getCount1() );
							}
						}
						call(new SaveVar(), scriptparser, 
								"shipsource.cargo",
								"#ship" );
						
						scriptparser.setRegister("#ship","0");
					} // end 'reqitems'
						
					if( qquest.getReqRe() != 0 ) {
						call( new AddMoney(), scriptparser, 
								0,
								scriptparser.getRegister("#USER"),
								qquest.getReqRe(),
								"Kosten Quest '"+qquest.getQName()+"'",
								0 );
					}
						
					// Belohnungen (Waren/RE)
					if( !qquest.getAwardItems().isEmpty() ) {
						call( new CopyVar(), scriptparser, 
								"#ship",
								"shipsource.cargo" );
						
						ResourceList reslist = qquest.getAwardItems().getResourceList();
						for( ResourceEntry res : reslist ) {
							call( new AddResource(), scriptparser, 
									"#ship",
									res.getId(),
									res.getCount1() );
						}
						call( new SaveVar(), scriptparser, 
								"shipsource.cargo",
								"#ship" );
						
						scriptparser.setRegister("#ship","0");	
					}
					if( qquest.getAwardRe() != 0 ) {
						call( new AddMoney(), scriptparser, 
								scriptparser.getRegister("#USER"),
								0,
								qquest.getAwardRe(),
								"Belohnung Quest '"+qquest.getQName()+"'",
								1 );
					}
					call( new CompleteQuest(), scriptparser, scriptparser.getRegister("#QUEST") );
					
					String dialogtext = null;
					if( qquest.getFinishText().isEmpty() ) {
						dialogtext = "Sehr gut! Du hast deine Aufgabe beendet.\nHier hast du ein paar Dinge die du sicher gut gebrauchen kannst:\n\n";
					}
					else {
						dialogtext = qquest.getFinishText()+"\n\n";	
					}
						
					if( !qquest.getAwardItems().isEmpty() ) {
						dialogtext += "Belohnung in Waren:\n";
					
						ResourceList reslist = qquest.getAwardItems().getResourceList();
						for( ResourceEntry res : reslist ) {
							dialogtext += "[resource="+res.getId()+"]"+res.getCount1()+"[/resource]\n";	
						}
						dialogtext += "\n";
					}
					if( qquest.getAwardRe() != 0 ) {
						dialogtext += "Belohnung in RE: "+Common.ln(qquest.getAwardRe())+"\n";	
					}
						
					call(new LoadDialog(), scriptparser, 
							dialogtext,
							qquest.getHead() );
																		
					call(new AddAnswer(), scriptparser, 
							"Auf Wiedersehen!",
							"0" );
					
					call(new InitDialog(), scriptparser );
						
					call(new EndQuest(), scriptparser );
				}
				else {
					String dialogtext = null;
					if( !qquest.getNotYetText().isEmpty() ) {
						dialogtext = qquest.getNotYetText();	
					}
					else {
						dialogtext = "Tut mir leid. Du hast die Aufgabe noch nicht komplett erledigt.";	
					}
					call( new LoadDialog(), scriptparser, 
							dialogtext,
							qquest.getHead() );
																		
					call( new AddAnswer(), scriptparser, 
							"Auf Wiedersehen!",
							"0" );
					
					call( new InitDialog(), scriptparser );
				}
			} 
			// ende if( action.equals("end") ) 
			
			scriptparser.setRegister("#A","1");
			
			return CONTINUE;
		}
	}
}
