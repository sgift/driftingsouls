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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.Offizier.Ability;
import net.driftingsouls.ds2.server.SectorTemplateManager;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Sector;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungResource;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungSchiff;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.ShipUtils;
import net.driftingsouls.ds2.server.scripting.entities.Answer;
import net.driftingsouls.ds2.server.scripting.entities.CompletedQuest;
import net.driftingsouls.ds2.server.scripting.entities.Quest;
import net.driftingsouls.ds2.server.scripting.entities.QuickQuest;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.scripting.entities.Text;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipLoot;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scriptbefehle fuer Questscripte.
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
		parser.registerCommand( "ADDBATTLEVISIBILITY", new AddBattleVisibility(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ENDBATTLE", new EndBattle(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETNOOBSTATUS", new GetNoobStatus(), ScriptParser.Args.PLAIN_REG );
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
	Map<String,QuestAnswer> dialogAnswers = new LinkedHashMap<>();

	class LoadDialog implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			Text dialogdata;

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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			BBCodeParser bbcodeparser = BBCodeParser.getInstance();

			String text = bbcodeparser.parse(dialogText);

			ScriptContext context = scriptparser.getContext();
			try {
				context.getWriter().append( "<table class=\"noBorderX\"><tr><td class=\"noBorderX\" valign=\"top\">" );
				context.getWriter().append("<img src=\"./data/quests/").append(dialogImage).append("\" alt=\"\" />");
				context.getWriter().append( "</td><td class=\"noBorderX\" valign=\"top\">" );
				context.getWriter().append(StringUtils.replace(text, "\n", "<br />")).append("<br /><br />");

				for( QuestAnswer answer : dialogAnswers.values() ) {
					context.getWriter().append("<a class=\"forschinfo\" href=\"").append(answer.url).append("\">").append(answer.text).append("</a><br />");
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			scriptparser.log("register: "+command[1]+"\n");
			scriptparser.log("value: "+command[2]+"\n");
			if( command.length > 3 ) {
				scriptparser.log("reg4value: "+command[3]+"\n");
			}

			Object val = null;

			String[] value = StringUtils.split(command[2], '.');
			switch (value[0])
			{
				case "shipsource":
					Ship ship;
					if (command.length == 3)
					{
						ship = scriptparser.getShip();
					}
					else if (scriptparser.getRegisterObject(command[3]) instanceof Ship)
					{
						ship = (Ship) scriptparser.getRegisterObject(command[3]);
					}
					else
					{
						int shipID = Integer.parseInt(scriptparser.getRegister(command[3]));
						ship = (Ship) db.get(Ship.class, shipID);
					}

					if ((value.length <= 1) || (value[1].length() == 0))
					{
						val = ship;
					}
					switch (value[1])
					{
						case "cargo":
							val = ship.getCargo();
							break;
						case "offizier":
							val = ship.getOffizier();
							break;
						case "id":
							val = ship.getId();
							break;
						case "type":
							val = ship.getType();
							break;
						case "name":
							val = ship.getName();
							break;
						case "x":
							val = ship.getX();
							break;
						case "y":
							val = ship.getY();
							break;
						case "system":
							val = ship.getSystem();
							break;
						case "status":
							val = ship.getStatus();
							break;
						case "crew":
							val = ship.getCrew();
							break;
						case "e":
							val = ship.getEnergy();
							break;
						case "s":
							val = ship.getHeat();
							break;
						case "hull":
							val = ship.getHull();
							break;
						case "shields":
							val = ship.getShields();
							break;
						case "heat":
							val = ship.getWeaponHeat();
							break;
						case "engine":
							val = ship.getEngine();
							break;
						case "weapons":
							val = ship.getWeapons();
							break;
						case "comm":
							val = ship.getComm();
							break;
						case "sensors":
							val = ship.getSensors();
							break;
						case "docked":
							val = ship.getDocked();
							break;
						case "alarm":
							val = ship.getAlarm();
							break;
						case "fleet":
							val = (ship.getFleet() != null ? ship.getFleet().getId() : 0);
							break;
						case "destsystem":
							val = ship.getEinstellungen().getDestSystem();
							break;
						case "destx":
							val = ship.getEinstellungen().getDestX();
							break;
						case "desty":
							val = ship.getEinstellungen().getDestY();
							break;
						case "destcom":
							val = ship.getEinstellungen().getDestCom();
							break;
						case "bookmark":
							val = ship.getEinstellungen().isBookmark();
							break;
						case "battle":
							val = ship.getBattle();
							break;
						case "battleaction":
							val = ship.isBattleAction();
							break;
						case "jumptarget":
							val = ship.getJumpTarget();
							break;
						case "autodeut":
							val = ship.getEinstellungen().getAutoDeut();
							break;
						case "history":
							val = ship.getHistory();
							break;
						case "script":
							val = ship.getScript();
							break;
						case "scriptexedata":
							val = ship.getScriptExeData();
							break;
					}
					break;
				case "tick":
					val = ContextMap.getContext().get(ContextCommon.class).getTick();
					break;
				case "offizier":
					Offizier offi = (Offizier) scriptparser.getRegisterObject(command[3]);

					switch (value[1])
					{
						case "name":
							val = offi.getName();
							break;
						case "id":
							val = offi.getID();
							break;
						case "rang":
							val = offi.getRang();
							break;
						case "dest":
							if (offi.getStationiertAufBasis() != null)
							{
								val = "b " + offi.getStationiertAufBasis().getId();
							}
							else if (offi.getStationiertAufSchiff() != null)
							{
								val = "s " + offi.getStationiertAufSchiff().getId();
							}
							break;
						case "owner":
							val = offi.getOwner().getId();
							break;
						case "ability":
							val = offi.getAbility(Ability.valueOf(value[2]));
							break;
						case "special":
							val = offi.getSpecial().toString();
							break;
					}
					break;
				case "base":
					Base base;

					Object baseObj = scriptparser.getRegisterObject(command[3]);
					if (baseObj instanceof Base)
					{
						base = (Base) baseObj;
					}
					else
					{
						int baseID = ((Number) baseObj).intValue();
						base = (Base) db.get(Base.class, baseID);
					}
					if ((value.length <= 1) || (value[1].length() == 0))
					{
						val = base;
					}
					else if (value[1].equals("cargo"))
					{
						val = base.getCargo();
					}
					else if ("id".equals(value[1]))
					{
						val = base.getId();
					}
					else if ("name".equals(value[1]))
					{
						val = base.getName();
					}
					else if ("owner".equals(value[1]))
					{
						val = base.getOwner().getId();
					}
					else if ("x".equals(value[1]))
					{
						val = base.getLocation().getX();
					}
					else if ("y".equals(value[1]))
					{
						val = base.getLocation().getY();
					}
					else if ("system".equals(value[1]))
					{
						val = base.getLocation().getSystem();
					}
					else if ("maxcargo".equals(value[1]))
					{
						val = base.getMaxCargo();
					}
					else if ("klasse".equals(value[1]))
					{
						val = base.getKlasse().getId();
					}
					break;
				case "jumpnode":
					JumpNode jn;

					Object jnObj = scriptparser.getRegisterObject(command[3]);
					if (jnObj instanceof JumpNode)
					{
						jn = (JumpNode) jnObj;
					}
					else
					{
						int jnID = ((Number) jnObj).intValue();
						jn = (JumpNode) db.get(JumpNode.class, jnID);
					}

					if ((value.length <= 1) || (value[1].length() == 0))
					{
						val = jn;
					}
					else if ("id".equals(value[1]))
					{
						val = jn.getId();
					}
					else if ("x".equals(value[1]))
					{
						val = jn.getLocation().getX();
					}
					else if ("y".equals(value[1]))
					{
						val = jn.getLocation().getY();
					}
					else if ("system".equals(value[1]))
					{
						val = jn.getLocation().getSystem();
					}
					else if ("xout".equals(value[1]))
					{
						val = jn.getXOut();
					}
					else if ("yout".equals(value[1]))
					{
						val = jn.getYOut();
					}
					else if ("systemout".equals(value[1]))
					{
						val = jn.getSystemOut();
					}
					else if ("name".equals(value[1]))
					{
						val = jn.getName();
					}
					break;
			}

			if( val instanceof Boolean ) {
				val = (val == Boolean.TRUE ? 1 : 0);
			}
			scriptparser.setRegister(command[1],val);

			return CONTINUE;
		}
	}

	class AddAnswer implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String answerid = command[1];
			String internalid = command[2];

			scriptparser.log( "answerid: "+answerid+"\n" );
			scriptparser.log( "internalid: "+internalid+"\n" );

			List<String> addParamList = new ArrayList<>();
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
		@Override
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			scriptparser.log("value: "+command[1]+"\n");
			scriptparser.log("register: "+command[2]+"\n");
			Object val = scriptparser.getRegisterObject(command[2]);
			if( (command.length > 3) && (command[3].length() > 0) ) {
				scriptparser.log("reg4value: "+command[3]+"\n");
			}

			String[] value = StringUtils.split(command[1], '.');
			switch (value[0])
			{
				case "shipsource":
					Ship ship = scriptparser.getShip();
					if ((command.length > 3) && (command[3].length() > 0))
					{
						Object shipObj = scriptparser.getRegister(command[3]);
						ship = (Ship) db.get(Ship.class, Value.Int(shipObj.toString()));

					}

					switch (value[1])
					{
						case "cargo":
							ship.setCargo((Cargo) val);
							break;
						case "name":
							ship.setName(val.toString());
							break;
						case "x":
							ship.setX(Value.Int(val.toString()));
							break;
						case "y":
							ship.setY(Value.Int(val.toString()));
							break;
						case "system":
							ship.setSystem(Value.Int(val.toString()));
							break;
						case "status":
							ship.setStatus(val.toString());
							break;
						case "crew":
							ship.setCrew(Value.Int(val.toString()));
							break;
						case "e":
							ship.setEnergy(Value.Int(val.toString()));
							break;
						case "s":
							ship.setHeat(Value.Int(val.toString()));
							break;
						case "hull":
							ship.setHull(Value.Int(val.toString()));
							break;
						case "shields":
							ship.setShields(Value.Int(val.toString()));
							break;
						case "heat":
							ship.setWeaponHeat(Weapons.parseWeaponList(val.toString()));
							break;
						case "engine":
							ship.setEngine(Value.Int(val.toString()));
							break;
						case "weapons":
							ship.setWeapons(Value.Int(val.toString()));
							break;
						case "comm":
							ship.setComm(Value.Int(val.toString()));
							break;
						case "sensors":
							ship.setSensors(Value.Int(val.toString()));
							break;
						case "alarm":
							ship.setAlarm(Value.Int(val.toString()));
							break;
						default:
							throw new UnsupportedOperationException("Setzen der Eigenschaft '" + value[1] + "' nicht unterstuetzt");
					}

					if ((command.length == 3) || (command[3].length() == 0))
					{
						scriptparser.setShip(ship);
					}
					break;
				case "sessionid":
					// Kein speichern von sessionids moeglich
					scriptparser.log("Speichern der Sessionid nicht moeglich\n");
					break;
				case "base":
					Object baseObj = scriptparser.getRegisterObject(command[3]);
					if (!(baseObj instanceof Base))
					{
						baseObj = db.get(Base.class, Value.Int(baseObj.toString()));
					}
					Base base = (Base) baseObj;

					switch (value[1])
					{
						case "cargo":
							base.setCargo((Cargo) val);
							break;
						case "name":
							base.setName(val.toString());
							break;
						case "maxcargo":
							base.setMaxCargo(Value.Int(val.toString()));
							break;
					}
					break;
			}

			return CONTINUE;
		}
	}

	class SetDialogTextVar implements SPFunction {
		@Override
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
		@Override
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String questid = scriptparser.getRegister("QUEST");
			int userid = Integer.parseInt(scriptparser.getRegister("USER"));

			RunningQuest runningQuest;
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
		@Override
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

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
				switch (event)
				{
					// On-Enter
					case "onenter":
						Location loc = Location.fromString(objid);

						Sector sector = (Sector) db.get(Sector.class, new MutableLocation(loc));
						if (sector == null)
						{
							sector = new Sector(new MutableLocation(loc));
							sector.setOnEnter("");
							db.persist(sector);
						}

						String onenter = sector.getOnEnter();
						if (onenter.length() > 0)
						{
							onenter += ";";
						}
						onenter += userid + ":" + scriptid + ":" + questid;

						sector.setOnEnter(onenter);

						removescript.append("!REMOVEHANDLER ").append(event).append(" ").append(objid).append(" ").append(userid).append(" ").append(scriptid).append(" ").append(questid).append("\n");
						break;
					// Battle-OnEnd
					default:
						Battle battle = (Battle) db.get(Battle.class, Value.Int("objid"));
						battle.setOnEnd("userid+\":\"+scriptid+\":\"+questid");
						break;
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

				RunningQuest runningdata = (RunningQuest)db
						.createQuery("from RunningQuest  where quest=:questid and user=:userid")
						.setInteger("questid", Value.Int(questid))
						.setInteger("userid", userid)
						.uniqueResult();
				runningdata.setOnTick(scriptid);

				removescript.append("!REMOVEHANDLER ").append(event).append(" ").append(userid).append(" ").append(questid).append("\n");
			}

			if( removescript.length() > 0 ) {
				RunningQuest runningdata;
				if( questid.charAt(0) != 'r' ) {
					runningdata = (RunningQuest)db
							.createQuery("from RunningQuest  where quest=:questid and user=:userid")
							.setInteger("questid", Value.Int(questid))
							.setInteger("userid", userid)
							.uniqueResult();
				}
				else {
					int rquestid = Value.Int(questid.substring(1));
					runningdata = (RunningQuest)db.get(RunningQuest.class, rquestid);
				}

				if( runningdata != null ) {
					String uninstall = runningdata.getUninstall();
					if( uninstall.length() == 0 ) {
						uninstall = ":0\n";
					}
					uninstall += removescript;

					runningdata.setUninstall(uninstall);
				}
			}

			return CONTINUE;
		}
	}

	static class RemoveHandler implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String event = command[1];
			scriptparser.log("event: "+event+"\n");

			String questid;
			int userid;

			if( event.equals("onenter") ) {
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


				Location loc = Location.fromString(objid);

				Sector sector = (Sector)db.get(Sector.class, new MutableLocation(loc));
				if( sector != null ) {
					List<String> newenter = new ArrayList<>();

					String[] enter = StringUtils.split(sector.getOnEnter(), ';');
					for (String anEnter : enter)
					{
						String[] tmp = StringUtils.split(anEnter, ':');
						int usr = Value.Int(tmp[0]);
						int script = Value.Int(tmp[1]);
						String quest = tmp[2];
						if ((usr != userid) || (script != scriptid) || !quest.equals(questid))
						{
							newenter.add(anEnter);
						}
					}

					if( newenter.size() > 0 ) {
						sector.setOnEnter(Common.implode(";", newenter));
					}
					else if( sector.getObjects() != 0 ) {
						sector.setOnEnter(null);
					}
					else {
						db.delete(sector);
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
					RunningQuest rquest = (RunningQuest)db
							.createQuery("from RunningQuest  where quest=:questid and user=:userid")
							.setInteger("questid", Value.Int(questid))
							.setInteger("userid", userid)
							.uniqueResult();

					rquest.setOnTick(null);
				}
				else {
					int rquestid = Value.Int(questid.substring(1));
					RunningQuest rquest = (RunningQuest)db.get(RunningQuest.class, rquestid);
					rquest.setOnTick(null);
				}
			}

			return CONTINUE;
		}
	}

	static class AddUninstallCmd implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));

			StringBuilder removescript = new StringBuilder();

			for( int i=1; i < command.length; i++ ) {
				String cmd = command[1];
				if( cmd.charAt(0) == '#' ) {
					removescript.append(scriptparser.getRegister(cmd)).append(' ');
				}
				else if( cmd.charAt(0) == '\\' ) {
					removescript.append(cmd.substring(1)).append(' ');
				}
				else {
					removescript.append(cmd).append(' ');
				}
			}

			removescript.append("\n");

			RunningQuest runningQuest;
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String questidStr = command[1];
			scriptparser.log("questid: "+questidStr+"\n");

			int questid;
			if( questidStr.charAt(0) == 'r' ) {
				String rquestid = questidStr.substring(1);
				RunningQuest rquest = (RunningQuest)db.get(RunningQuest.class, Value.Int(rquestid));
				questid = rquest.getQuest().getId();
			}
			else {
				questid = Value.Int(questidStr);
			}

			int userid = Value.Int(scriptparser.getRegister("USER"));

			CompletedQuest cquest = (CompletedQuest)db
					.createQuery("from CompletedQuest where quest=:questid and user=:userid")
					.setInteger("questid", questid)
					.setInteger("userid", userid)
					.uniqueResult();
			if( cquest == null ) {
				User user = (User)db.get(User.class, userid);
				Quest quest = (Quest)db.get(Quest.class, questid);
				cquest = new CompletedQuest(quest, user);
				db.persist(cquest);
			}

			return CONTINUE;
		}
	}

	static class HasQuestCompleted implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String questid = command[1];
			scriptparser.log("questid: "+questid+"\n");

			if( questid.charAt(0) == 'r' ) {
				int rquestid = Integer.parseInt(questid.substring(1));
				RunningQuest rquest = (RunningQuest)db.get(RunningQuest.class, rquestid);
				questid = Integer.toString(rquest.getId());
			}

			int userid = Value.Int(scriptparser.getRegister("USER"));

			CompletedQuest cquest = (CompletedQuest)db
					.createQuery("from CompletedQuest where user=:userid and quest=:questid")
					.setInteger("userid", userid)
					.setInteger("questid", Integer.parseInt(questid))
					.uniqueResult();

			if( cquest != null ) {
				scriptparser.setRegister("cmp","1");
			}
			else {
				scriptparser.setRegister("cmp","0");
			}

			return CONTINUE;
		}
	}

	static class SetQuestUIStatus implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

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
				RunningQuest rquest = (RunningQuest)db
						.createQuery("from RunningQuest  where quest=:questid and user=:userid")
						.setInteger("questid", Value.Int(questid))
						.setInteger("userid", userid)
						.uniqueResult();

				rquest.setStatusText(statustext);
				rquest.setPublish(publish);
			}
			else {
				String rquestid = questid.substring(1);
				RunningQuest rquest = (RunningQuest)db.get(RunningQuest.class, Value.Int(rquestid));

				rquest.setStatusText(statustext);
				rquest.setPublish(publish);
			}

			return CONTINUE;
		}
	}

	static class SaveOutput implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			// Funktioniert nur mit StringWriter und co.
			scriptparser.setRegister("_OUTPUT",scriptparser.getContext().getWriter().toString());

			return CONTINUE;
		}
	}

	class LoadQuestContext implements SPFunction {
		private final Log log = LogFactory.getLog(LoadQuestContext.class);

		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String questid = command[1];
			scriptparser.log("QuestID: "+questid+"\n");

			RunningQuest rquest;
			if( questid.charAt(0) != 'r' ) {
				int user = Value.Int(scriptparser.getRegister("USER"));

				rquest = (RunningQuest)db
						.createQuery("from RunningQuest where quest=:quest and user=:user")
						.setInteger("quest", Integer.parseInt(questid))
						.setInteger("user", user)
						.uniqueResult();
			}
			else {
				String rquestid = questid.substring(1);

				rquest = (RunningQuest)db.load(RunningQuest.class, Integer.parseInt(rquestid));
			}

			if( rquest == null ) {
				scriptparser.log("Warnung: Kein passendes laufendes Quest gefunden\n");
				scriptparser.setRegister("QUEST","0");
			}
			else {
				try {
					Object ip = scriptparser.getContext().getAttribute("__INSTRUCTIONPOINTER");

					byte[] blob = rquest.getExecData();
					scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE).putAll(
							ScriptParserContext.fromStream(new ByteArrayInputStream(blob)).getBindings(ScriptContext.ENGINE_SCOPE)
					);

					scriptparser.getContext().setAttribute("__INSTRUCTIONPOINTER", ip, ScriptContext.ENGINE_SCOPE);
					scriptparser.setRegister("QUEST","r"+rquest.getId());
				}
				catch( Exception e ) {
					scriptparser.log("Fehler: Konnte Questdaten nicht laden: "+e+"\n");
					scriptparser.setRegister("QUEST","r"+rquest.getId());
					log.warn("Fehler beim Laden der Questdaten (Quest: "+questid+"): "+e,e);
				}
			}

			return CONTINUE;
		}
	}

	class SaveQuestContext implements SPFunction {
		private final Log log = LogFactory.getLog(SaveQuestContext.class);

		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String questid = command[1];
			scriptparser.log("QuestID: "+questid+"\n");

			RunningQuest questdata;
			if( questid.charAt(0) != 'r' ) {
				int user = Value.Int(scriptparser.getRegister("USER"));

				questdata = (RunningQuest)db.createQuery("from RunningQuest where quest=:quest and user=:user")
					.setParameter("quest", Integer.parseInt(questid))
					.setParameter("user", user)
					.uniqueResult();
			}
			else {
				String rquestid = questid.substring(1);
				questdata = (RunningQuest)db.get(RunningQuest.class, Integer.parseInt(rquestid));
			}

			if( questdata == null ) {
				scriptparser.log("Warnung: Kein passendes laufendes Quest gefunden\n");
			}
			else {
				try {
					ByteArrayOutputStream blob = new ByteArrayOutputStream();
					ScriptParserContext.toStream(scriptparser.getContext(), blob);
					questdata.setExecData(blob.toByteArray());
				}
				catch( Exception e ) {
					scriptparser.log("Fehler: Konnte Questdaten nicht schreiben: "+e+"\n");
					scriptparser.setRegister("QUEST","r"+questdata.getId());
					log.warn("Fehler beim Schreiben der Questdaten (Quest: "+questid+"): "+e,e);
				}
			}

			return CONTINUE;
		}
	}

	/*—---------------------------------------------
	 *
	 * 	Cargofunktionen
	 *
	 ----------------------------------------------*/

	static class AddQuestItem implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

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
				RunningQuest rquest = (RunningQuest)db
						.createQuery("from RunningQuest  where quest=:questid and user=:userid")
						.setInteger("questid", Value.Int(questid))
						.setInteger("userid", userid)
						.uniqueResult();

				if( rquest == null ) {
					scriptparser.log("FATAL ERROR: Kein passendes Quest gefunden!\n");
					return CONTINUE;
				}
				questid = Integer.toString(rquest.getId());
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

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
				RunningQuest rquest = (RunningQuest)db
						.createQuery("from RunningQuest  where quest=:questid and user=:userid")
						.setInteger("questid", Value.Int(questid))
						.setInteger("userid", userid)
						.uniqueResult();

				if( rquest == null ) {
					scriptparser.log("FATAL ERROR: Kein passendes Quest gefunden!\n");
					return CONTINUE;
				}
				questid = Integer.toString(rquest.getId());
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
		@Override
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
		@Override
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
		@Override
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
		@Override
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			scriptparser.log("cargo(target): "+command[1]+"\n");
			Object cargotarObj = scriptparser.getRegisterObject(command[1]);

			scriptparser.log("cargo(source): "+command[2]+"\n");
			Object cargosourceObj = scriptparser.getRegisterObject(command[2]);

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

	static class AddQuestShips implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

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

			Integer[] shipids = stm.useTemplate(ContextMap.getContext().getDB(), stmid, loc, owner );

			// TODO: Wie Arrays behandeln?
			scriptparser.setRegister("A",shipids);

			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));

			StringBuilder removescript = new StringBuilder();
			for (Integer shipid : shipids)
			{
				removescript.append("!REMOVESHIP ").append(shipid).append("\n");
			}

			RunningQuest runningdata;
			if( questid.charAt(0) != 'r' ) {
				runningdata = (RunningQuest)db.createQuery("from RunningQuest where quest=:quest and user=:user")
					.setParameter("quest", Value.Int(questid))
					.setParameter("user", userid)
					.uniqueResult();
			}
			else {
				int rquestid = Value.Int(questid.substring(1));
				runningdata = (RunningQuest)db.get(RunningQuest.class, rquestid);
			}

			String uninstall = runningdata.getUninstall();
			if( uninstall.length() == 0 ) {
				uninstall = ":0\n";
			}
			uninstall += removescript.toString();

			runningdata.setUninstall(uninstall);

			return CONTINUE;
		}
	}

	static class AddShips implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
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

			Integer[] shipids = stm.useTemplate(ContextMap.getContext().getDB(), stmid, loc, owner );

			// TODO: Wie Arrays behandeln?
			scriptparser.setRegister("A",shipids);

			return CONTINUE;
		}
	}

	static class RemoveShip implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			Ship ship = (Ship)db.get(Ship.class, Integer.parseInt(command[1]));
			ship.destroy();

			return CONTINUE;
		}
	}

	static class MoveShip implements SPFunction {
		@Override
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			int shipid = Value.Int(command[1]);
			scriptparser.log("shipid: "+shipid+"\n");

			Ship ship = (Ship)db.get(Ship.class, shipid);
			if( ship == null || ship.getId() < 0 ) {
				scriptparser.setRegister("cmp","0");
			}
			else if( ship.getBattle() != null ) {
				BattleShip bs = (BattleShip)db
						.createQuery("from BattleShip where ship=:ship")
						.setEntity("ship", ship)
						.uniqueResult();

				if( (bs.getAction() & Battle.BS_DESTROYED) != 0 ) {
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String questid = scriptparser.getRegister("QUEST");
			int userid = Value.Int(scriptparser.getRegister("USER"));

			int shiptype = Value.Int(command[1]);
			scriptparser.log("shiptype: "+shiptype+"\n");

			int shipownerId = Value.Int(command[2]);
			scriptparser.log("shipowner: "+shipownerId+"\n");

			int chance = Value.Int(command[3]);
			scriptparser.log("chance: "+chance);

			ResourceID resourceid = Resources.fromString(command[4]);
			scriptparser.log("resourceid: "+resourceid+"\n");
			if( resourceid.getQuest() == 1 ) {
				int qid;
				if( questid.charAt(0) != 'r' ) {
					RunningQuest rquest = (RunningQuest)db
							.createQuery("from RunningQuest  where quest=:questid and user=:userid")
							.setInteger("questid", Value.Int(questid))
							.setInteger("userid", userid)
							.uniqueResult();
					qid = rquest.getId();
				}
				else {
					qid = Value.Int(questid.substring(1));
				}

				resourceid = new ItemID(resourceid.getItemID(), resourceid.getUses(), qid);
			}

			int count = Value.Int(command[5]);
			scriptparser.log("resource-count: "+count+"\n");

			int totalmax = 0;
			if( command.length > 6 ) {
				totalmax = Value.Int(command[6]);
				scriptparser.log("totalmax: "+totalmax+"\n");
			}

			if( totalmax == 0 ) {
				totalmax = -1;
			}

			User user = (User)db.get(User.class, userid);
			User shipowner = (User)db.get(User.class, shipownerId);

			ShipLoot loot = new ShipLoot();
			loot.setShipType(shiptype);
			loot.setOwner(shipowner);
			loot.setTargetUser(user);
			loot.setChance(chance);
			loot.setResource(resourceid.toString());
			loot.setCount(count);
			loot.setTotalMax(totalmax);

			db.persist(loot);

			int loottable = loot.getId();

			RunningQuest runningdata;
			if( questid.charAt(0) != 'r' ) {
				runningdata = (RunningQuest)db
						.createQuery("from RunningQuest  where quest=:questid and user=:userid")
						.setInteger("questid", Value.Int(questid))
						.setInteger("userid", userid)
						.uniqueResult();
			}
			else {
				int rquestid = Value.Int(questid.substring(1));
				runningdata = (RunningQuest)db.get(RunningQuest.class, rquestid);
			}

			if( runningdata != null ) {
				if( runningdata.getUninstall() == null || runningdata.getUninstall().length() == 0 ) {
					runningdata.setUninstall(":0\n");
				}
				runningdata.setUninstall(runningdata.getUninstall() + "!DELETELOOTTABLE "+loottable+"\n");
			}

			return CONTINUE;
		}
	}

	static class DeleteLootTable implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			int lootid = Value.Int(command[1]);
			scriptparser.log("loottable-id: "+lootid+"\n");

			if( lootid > 0 ) {
				ShipLoot loot = (ShipLoot)db.get(ShipLoot.class, lootid);
				db.delete(loot);
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
		@Override
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
		@Override
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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			int userid = Value.Int(command[1]);
			scriptparser.log("userid: "+userid+"\n");

			User user = (User)ContextMap.getContext().getDB().get(User.class, userid);
			scriptparser.setRegister("A",user.getKonto().toString());

			return CONTINUE;
		}
	}

	static class CloneOffizier implements SPFunction {
		@Override
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

			Offizier offizier;
			switch (desttype)
			{
				case "s":
				{
					Ship destobj = (Ship) db.get(Ship.class, destid);

					offizier = new Offizier(destobj.getOwner(), baseOffi.getName());
					offizier.stationierenAuf(destobj);
					break;
				}
				case "b":
				{
					Base destobj = (Base) db.get(Base.class, destid);
					offizier = new Offizier(destobj.getOwner(), baseOffi.getName());
					offizier.stationierenAuf(destobj);
					break;
				}
				default:
					scriptparser.log("Warnung: Kein gueltiges Ziel '" + desttype + "' fuer Offizier");
					return CONTINUE;
			}

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
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			int offiid = Value.Int(command[1]);
			scriptparser.log("offiid: "+offiid+"\n");

			Offizier offi = (Offizier)db.get(Offizier.class, offiid);
			if( offi == null ) {
				return CONTINUE;
			}
			Ship targetShip = offi.getStationiertAufSchiff();

			db.delete(offi);

			if( targetShip != null ) {
				targetShip.recalculateShipStatus();
			}

			return CONTINUE;
		}
	}

	static class AddBattleVisibility implements SPFunction {
		@Override
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
		@Override
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
		@Override
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

	static class GetSectorProperty implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			Location sector = Location.fromString(command[1]);
			scriptparser.log("sector: "+sector+"\n");

			String property = command[2];
			scriptparser.log("property: "+property+"\n");

			List<String> result = new ArrayList<>();

			switch (property)
			{
				case "nebel":
					Nebel nebel = (Nebel) db.get(Nebel.class, new MutableLocation(sector));
					if (nebel != null)
					{
						result.add(nebel.getLocation().toString());
					}
					break;
				case "bases":
					List<?> baseIds = db.createQuery("select id from Base where system= :sys and x= :x and y= :y")
							.setInteger("sys", sector.getSystem())
							.setInteger("x", sector.getX())
							.setInteger("y", sector.getY())
							.list();
					for (Object baseId : baseIds)
					{
						Integer id = (Integer) baseId;
						result.add(id.toString());
					}
					break;
				case "jumpnodes":
					List<?> jnIds = db.createQuery("select id from JumpNode WHERE system= :sys and x= :x and y= :y")
							.setInteger("sys", sector.getSystem())
							.setInteger("x", sector.getX())
							.setInteger("y", sector.getY())
							.list();
					for (Object jnId : jnIds)
					{
						Integer id = (Integer) jnId;
						result.add(id.toString());
					}
					break;
				case "ships":
				{
					List<?> shipIds = db.createQuery("select id from Ship WHERE id > 0 and system= :sys and x= :x and y= :y")
							.setInteger("sys", sector.getSystem())
							.setInteger("x", sector.getX())
							.setInteger("y", sector.getY())
							.list();
					for (Object shipId : shipIds)
					{
						Integer id = (Integer) shipId;
						result.add(id.toString());
					}
					break;
				}
				case "shipsByOwner":
				{
					int owner = Value.Int(command[3]);
					scriptparser.log("owner: " + owner + "\n");

					List<?> shipIds = db.createQuery("select id from Ship WHERE id>0 and system= :sys and x= :x and y= :y and owner=:owner")
							.setInteger("sys", sector.getSystem())
							.setInteger("x", sector.getX())
							.setInteger("y", sector.getY())
							.setInteger("owner", owner)
							.list();
					for (Object shipId : shipIds)
					{
						Integer id = (Integer) shipId;
						result.add(id.toString());
					}
					break;
				}
				case "shipsByTag":
				{
					String tag = command[3];
					scriptparser.log("tag: " + tag + "\n");

					List<?> shipIds = db.createQuery("select id from Ship WHERE id>0 and system= :sys and x= :x and y= :y and status like :status")
							.setInteger("sys", sector.getSystem())
							.setInteger("x", sector.getX())
							.setInteger("y", sector.getY())
							.setString("status", "%<" + tag + ">%")
							.list();
					for (Object shipId : shipIds)
					{
						Integer id = (Integer) shipId;
						result.add(id.toString());
					}
					break;
				}
			}

			scriptparser.setRegister("A",result.toArray(new String[result.size()]));

			return CONTINUE;
		}
	}


	static class GetSystemProperty implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			int system = Value.Int(command[1]);
			scriptparser.log("system: "+system+"\n");

			String property = command[2];
			scriptparser.log("property: "+property+"\n");

			List<String> result = new ArrayList<>();

			if( property.equals("shipsByTag") ) {
				String tag = command[3];
				scriptparser.log("tag: "+tag+"\n");

				List<?> shipIds = db.createQuery("select id from Ship WHERE id>0 and system= :sys and status like :status")
						.setInteger("sys", system)
						.setString("status", "%<"+tag+">%")
						.list();
				for (Object shipId : shipIds)
				{
					Integer id = (Integer) shipId;
					result.add(id.toString());
				}
			}

			scriptparser.setRegister("A",result.toArray(new String[result.size()]));

			return CONTINUE;
		}
	}

	static class GtuAuctionShip implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

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

			User user = (User)db.get(User.class, owner);
			ShipType st = (ShipType)db.get(ShipType.class, shipid);

			VersteigerungSchiff v = new VersteigerungSchiff(user, st, initbid);
			v.setBieter((User)db.get(User.class, Faction.GTU));
			v.setTick(ticks);
			db.persist(v);

			return CONTINUE;
		}
	}

	static class GtuAuctionCargo implements SPFunction {
		@Override
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
			versteigerung.setBieter((User)db.get(User.class, Faction.GTU));
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

		@Override
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
			for (Object aQquestList : qquestList)
			{
				QuickQuest qquest = (QuickQuest) aQquestList;

				if (!qquest.getMoreThanOnce())
				{
					call(new HasQuestCompleted(), scriptparser, qquest.getEnabled());
					if (Integer.parseInt(scriptparser.getRegister("#cmp")) > 0)
					{
						continue;
					}
				}
				if (qquest.getDependsOnQuests().length() > 0)
				{
					boolean ok = true;

					String[] qquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
					for (String qquest1 : qquests)
					{
						String[] tmp = StringUtils.split(qquest1, ':');
						Quest quest = (Quest) db.createQuery("from Quest where qid= :qid")
								.setString("qid", tmp[1])
								.uniqueResult();
						if (quest == null)
						{
							log.warn("QQuest " + qquest.getId() + " benoetigt Quest " + tmp[1] + ", welches jedoch nicht existiert");
							continue;
						}

						call(new HasQuestCompleted(), scriptparser, quest.getId());
						if (Value.Int(scriptparser.getRegister("#cmp")) <= 0)
						{
							ok = false;
							break;
						}
					}
					if (!ok)
					{
						continue;
					}
				}
				call(new LoadQuestContext(), scriptparser, qquest.getEnabled());

				if (Value.Int(scriptparser.getRegister("#QSTATUS")) > 0)
				{
					continue;
				}

				call(new AddAnswer(), scriptparser,
						"Auftrag &gt;" + qquest.getQName() + "&lt",
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

		@Override
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
			for (Object aQquestList : qquestList)
			{
				QuickQuest qquest = (QuickQuest) aQquestList;

				call(new LoadQuestContext(), scriptparser, qquest.getEnabled());

				if (scriptparser.getRegister("#QUEST").length() == 0 ||
						scriptparser.getRegister("#QUEST").equals("0"))
				{
					continue;
				}

				int rquestid = Integer.parseInt(scriptparser.getRegister("#QUEST").substring(1));
				RunningQuest rquest = (RunningQuest) db.get(RunningQuest.class, rquestid);
				if (rquest.getQuest().getId() != qquest.getEnabled())
				{
					continue;
				}

				if (Value.Int(scriptparser.getRegister("#QSTATUS")) != 1)
				{
					continue;
				}

				call(new AddAnswer(), scriptparser,
						"Auftrag &gt;" + qquest.getQName() + "&lt beenden",
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

		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			String typeid = command[1];
			scriptparser.log("TypeID: "+typeid+"\n");

			int shipid = Integer.parseInt(command[2]);
			scriptparser.log("ShipID: "+shipid+"\n");

			scriptparser.log("Action: "+scriptparser.getParameter(0)+"\n");

			String action = scriptparser.getParameter(0);
			switch (action)
			{
				case "desc":
				{
					scriptparser.log("QQuest: " + scriptparser.getParameter(1) + "\n");

					QuickQuest qquest = (QuickQuest) db.get(QuickQuest.class, Value.Int(scriptparser.getParameter(1)));

					if ((qquest == null) || !qquest.getSourceType().equals(typeid))
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					int[] sourcelist = Common.explodeToInt(",", qquest.getSource());
					if (!Common.inArray(shipid, sourcelist))
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					call(new LoadQuestContext(), scriptparser, qquest.getEnabled());
					call(new GetQuestID(), scriptparser, scriptparser.getRegister("#QUEST"));

					if (Value.Int(scriptparser.getRegister("#A")) == qquest.getEnabled())
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					if (!qquest.getMoreThanOnce())
					{
						call(new HasQuestCompleted(), scriptparser, qquest.getEnabled());
						if (Value.Int(scriptparser.getRegister("#cmp")) > 0)
						{
							scriptparser.setRegister("#A", "0");
							return CONTINUE;
						}
					}

					if (qquest.getDependsOnQuests().length() > 0)
					{
						String[] qquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
						for (String qquest1 : qquests)
						{
							String[] tmp = StringUtils.split(qquest1, ':');
							Quest quest = (Quest) db.createQuery("from Quest where qid= :qid")
									.setString("qid", tmp[1])
									.uniqueResult();
							if (quest == null)
							{
								log.warn("QQuest " + qquest.getId() + " benoetigt Quest " + tmp[1] + ", welches jedoch nicht existiert");
								continue;
							}

							call(new HasQuestCompleted(), scriptparser, quest.getId());
							if (Value.Int(scriptparser.getRegister("#cmp")) <= 0)
							{
								scriptparser.setRegister("#A", "0");
								return CONTINUE;
							}
						}
					}

					String dialogtext = qquest.getShortDesc() + "[hr]\n" + qquest.getDescription() + "\n\n";
					if (!qquest.getReqItems().isEmpty() || qquest.getReqRe() != 0)
					{
						dialogtext += "Benötigt:[color=red]\n";
						if (!qquest.getReqItems().isEmpty())
						{
							ResourceList reslist = qquest.getReqItems().getResourceList();
							for (ResourceEntry res : reslist)
							{
								dialogtext += "[resource=" + res.getId() + "]" + res.getCount1() + "[/resource]\n";
							}
						}
						if (qquest.getReqRe() != 0)
						{
							dialogtext += Common.ln(qquest.getReqRe()) + " RE\n";
						}
						dialogtext += "[/color]\n\n";
					}
					if (!qquest.getAwardItems().isEmpty())
					{
						dialogtext += "Belohnung in Waren:\n";

						ResourceList reslist = qquest.getAwardItems().getResourceList();
						for (ResourceEntry res : reslist)
						{
							dialogtext += "[resource=" + res.getId() + "]" + res.getCount1() + "[/resource]\n";
						}
						dialogtext += "\n";
					}
					if (qquest.getAwardRe() != 0)
					{
						dialogtext += "Belohnung in RE: " + Common.ln(qquest.getAwardRe()) + "\n";
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
					break;
				}
				// ende action.equals("desc")
				case "yes":
				{
					scriptparser.log("QQuest: " + scriptparser.getParameter(1) + "\n");

					QuickQuest qquest = (QuickQuest) db.get(QuickQuest.class, Value.Int(scriptparser.getParameter(1)));

					if ((qquest == null) || !qquest.getSourceType().equals(typeid))
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					int[] sourcelist = Common.explodeToInt(",", qquest.getSource());
					if (!Common.inArray(shipid, sourcelist))
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					call(new LoadQuestContext(), scriptparser, qquest.getEnabled());
					call(new GetQuestID(), scriptparser, scriptparser.getRegister("#QUEST"));
					if (Value.Int(scriptparser.getRegister("#A")) == qquest.getEnabled())
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					if (!qquest.getMoreThanOnce())
					{
						call(new HasQuestCompleted(), scriptparser, qquest.getEnabled());
						if (Value.Int(scriptparser.getRegister("#cmp")) > 0)
						{
							scriptparser.setRegister("#A", "0");
							return CONTINUE;
						}
					}
					if (qquest.getDependsOnQuests().length() > 0)
					{
						String[] qquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
						for (String qquest1 : qquests)
						{
							String[] tmp = StringUtils.split(qquest1, ':');
							Quest quest = (Quest) db.createQuery("from Quest where qid= :qid")
									.setString("qid", tmp[1])
									.uniqueResult();
							if (quest == null)
							{
								log.warn("QQuest " + qquest.getId() + " benoetigt Quest " + tmp[1] + ", welches jedoch nicht existiert");
								continue;
							}

							call(new HasQuestCompleted(), scriptparser, quest.getId());
							if (Value.Int(scriptparser.getRegister("#cmp")) <= 0)
							{
								scriptparser.setRegister("#A", "0");
								return CONTINUE;
							}
						}
					}

					call(new InitQuest(), scriptparser, qquest.getEnabled());
					scriptparser.setRegister("#QSTATUS", "1");

					// Evt fuer das Quest benoetigte Items auf das Schiff transferieren
					if (!qquest.getStartItems().isEmpty())
					{
						call(new CopyVar(), scriptparser,
								"#ship",
								"shipsource.cargo");

						ResourceList reslist = qquest.getStartItems().getResourceList();
						for (ResourceEntry res : reslist)
						{
							if (res.getId().getQuest() != 0)
							{
								call(new AddQuestItem(), scriptparser,
										"#ship",
										res.getId().getItemID(),
										res.getCount1());
							}
							else
							{
								call(new AddResource(), scriptparser,
										"#ship",
										res.getId().toString(),
										res.getCount1());
							}
						}
						call(new SaveVar(), scriptparser,
								"shipsource.cargo",
								"#ship");

						scriptparser.setRegister("#ship", "0");
					}
					// Loottable ergaenzen
					if (qquest.getLoottable() != null)
					{
						String[] loottable = StringUtils.split(qquest.getLoottable(), ';');
						for (String aLoottable : loottable)
						{
							String[] atable = StringUtils.split(aLoottable, ',');
							if (atable.length > 4)
							{
								call(new AddLootTable(), scriptparser,
										atable[0],
										atable[1],
										atable[2],
										atable[3],
										atable[4],
										atable.length > 5 ? atable[5] : "");
							}
						}
					}
					scriptparser.setRegister("#quest" + qquest.getId() + "_status", qquest.getShortDesc());
					call(new SetQuestUIStatus(), scriptparser,
							"#quest" + qquest.getId() + "_status",
							"1");
					break;
				}
				// ende if( action.equals("yes")
				case "end":
				{
					scriptparser.log("QQuest: " + scriptparser.getParameter(1) + "\n");

					QuickQuest qquest = (QuickQuest) db.get(QuickQuest.class, Value.Int(scriptparser.getParameter(1)));

					if ((qquest == null) || !qquest.getSourceType().equals(typeid))
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					int[] sourcelist = Common.explodeToInt(",", qquest.getSource());
					if (!Common.inArray(shipid, sourcelist))
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					call(new LoadQuestContext(), scriptparser, qquest.getEnabled());

					if (scriptparser.getRegister("#QUEST").length() == 0)
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					int rquestid = Value.Int(scriptparser.getRegister("#QUEST").substring(1));
					RunningQuest rquest = (RunningQuest) db.get(RunningQuest.class, rquestid);
					if ((rquest == null) || (rquest.getQuest().getId() != qquest.getEnabled()))
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					if (Value.Int(scriptparser.getRegister("#QSTATUS")) != 1)
					{
						scriptparser.setRegister("#A", "0");
						return CONTINUE;
					}

					boolean quitable = true;

					// Die zum beenden benoetigten Items checken
					if (!qquest.getReqItems().isEmpty())
					{
						call(new CopyVar(), scriptparser,
								"#ship",
								"shipsource.cargo");

						ResourceList reslist = qquest.getReqItems().getResourceList();
						for (ResourceEntry res : reslist)
						{
							if (res.getId().getQuest() != 0)
							{
								call(new HasQuestItem(), scriptparser,
										"#ship",
										res.getId().getItemID(),
										res.getCount1());

								if (Value.Int(scriptparser.getRegister("#cmp")) <= 0)
								{
									quitable = false;
									break;
								}
							}
							else
							{
								call(new HasResource(), scriptparser,
										"#ship",
										res.getId(),
										res.getCount1());

								if (Value.Int(scriptparser.getRegister("#cmp")) <= 0)
								{
									quitable = false;
									break;
								}
							}
						}
						scriptparser.setRegister("#ship", "0");
					} // end 'reqitems'

					if (quitable && (qquest.getReqRe() != 0))
					{
						call(new GetMoney(), scriptparser, scriptparser.getRegister("#USER"));
						if (Value.Int(scriptparser.getRegister("A")) < qquest.getReqRe())
						{
							quitable = false;
						}
					}

					// Koennen wir das Quest nun beenden oder nicht?
					if (quitable)
					{
						// Die ganzen gegenstaende abbuchen
						if (!qquest.getReqItems().isEmpty())
						{
							call(new CopyVar(), scriptparser,
									"#ship",
									"shipsource.cargo");

							ResourceList reslist = qquest.getReqItems().getResourceList();
							for (ResourceEntry res : reslist)
							{
								if (res.getId().getQuest() != 0)
								{
									call(new AddQuestItem(), scriptparser,
											"#ship",
											res.getId().getItemID(),
											-res.getCount1());
								}
								else
								{
									call(new AddResource(), scriptparser,
											"#ship",
											res.getId(),
											-res.getCount1());
								}
							}
							call(new SaveVar(), scriptparser,
									"shipsource.cargo",
									"#ship");

							scriptparser.setRegister("#ship", "0");
						} // end 'reqitems'

						if (qquest.getReqRe() != 0)
						{
							call(new AddMoney(), scriptparser,
									0,
									scriptparser.getRegister("#USER"),
									qquest.getReqRe(),
									"Kosten Quest '" + qquest.getQName() + "'",
									0);
						}

						// Belohnungen (Waren/RE)
						if (!qquest.getAwardItems().isEmpty())
						{
							call(new CopyVar(), scriptparser,
									"#ship",
									"shipsource.cargo");

							ResourceList reslist = qquest.getAwardItems().getResourceList();
							for (ResourceEntry res : reslist)
							{
								call(new AddResource(), scriptparser,
										"#ship",
										res.getId(),
										res.getCount1());
							}
							call(new SaveVar(), scriptparser,
									"shipsource.cargo",
									"#ship");

							scriptparser.setRegister("#ship", "0");
						}
						if (qquest.getAwardRe() != 0)
						{
							call(new AddMoney(), scriptparser,
									scriptparser.getRegister("#USER"),
									0,
									qquest.getAwardRe(),
									"Belohnung Quest '" + qquest.getQName() + "'",
									1);
						}
						call(new CompleteQuest(), scriptparser, scriptparser.getRegister("#QUEST"));

						String dialogtext;
						if (qquest.getFinishText().isEmpty())
						{
							dialogtext = "Sehr gut! Du hast deine Aufgabe beendet.\nHier hast du ein paar Dinge die du sicher gut gebrauchen kannst:\n\n";
						}
						else
						{
							dialogtext = qquest.getFinishText() + "\n\n";
						}

						if (!qquest.getAwardItems().isEmpty())
						{
							dialogtext += "Belohnung in Waren:\n";

							ResourceList reslist = qquest.getAwardItems().getResourceList();
							for (ResourceEntry res : reslist)
							{
								dialogtext += "[resource=" + res.getId() + "]" + res.getCount1() + "[/resource]\n";
							}
							dialogtext += "\n";
						}
						if (qquest.getAwardRe() != 0)
						{
							dialogtext += "Belohnung in RE: " + Common.ln(qquest.getAwardRe()) + "\n";
						}

						call(new LoadDialog(), scriptparser,
								dialogtext,
								qquest.getHead());

						call(new AddAnswer(), scriptparser,
								"Auf Wiedersehen!",
								"0");

						call(new InitDialog(), scriptparser);

						call(new EndQuest(), scriptparser);
					}
					else
					{
						String dialogtext;
						if (!qquest.getNotYetText().isEmpty())
						{
							dialogtext = qquest.getNotYetText();
						}
						else
						{
							dialogtext = "Tut mir leid. Du hast die Aufgabe noch nicht komplett erledigt.";
						}
						call(new LoadDialog(), scriptparser,
								dialogtext,
								qquest.getHead());

						call(new AddAnswer(), scriptparser,
								"Auf Wiedersehen!",
								"0");

						call(new InitDialog(), scriptparser);
					}
					break;
				}
			}
			// ende if( action.equals("end") )

			scriptparser.setRegister("#A","1");

			return CONTINUE;
		}
	}
}
