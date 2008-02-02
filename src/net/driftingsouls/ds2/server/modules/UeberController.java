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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Blob;
import java.util.ArrayList;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Die Uebersicht
 * @author Christopher Jung
 *
 */
public class UeberController extends TemplateGenerator implements Loggable {
	private String box = "";
	
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public UeberController(Context context) {
		super(context);
		
		setTemplate("ueber.html");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		box = getUser().getUserValue("TBLORDER/uebersicht/box");
		
		return true;
	}
	
	/**
	 * Beendet den Vacation-Modus-Vorlauf
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void stopWait4VacAction() {
		User user = (User)getUser();
		
		user.setVacationCount(0);
		user.setWait4VacationCount(0);
		
		user.setTemplateVars(this.getTemplateEngine());
		Common.writeLog("login.log", Common.date("j+m+Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> Abbruch Vac-Vorlauf Browser <"+getRequest().getUserAgent()+"> \n");
				
		redirect();
	}
	
	/**
	 * Wechselt die Tutorial-Seite bzw beendet das Tutorial 
	 * @urlparam Integer tutorial 1, falls die naechste Tutorialseite angezeigt werden soll. Zum Beenden -1
	 */
	@Action(ActionType.DEFAULT)
	public void tutorialAction() {
		User user = (User)getUser();
		
		parameterNumber("tutorial");
		int tutorial = getInteger("tutorial");
		
		if( tutorial == 1 ) {
			int inttutorial = Integer.parseInt(user.getUserValue("TBLORDER/uebersicht/inttutorial"));

			user.setUserValue("TBLORDER/uebersicht/inttutorial", Integer.toString(inttutorial+1));
		}
		else if( tutorial == -1 ) {
			user.setUserValue("TBLORDER/uebersicht/inttutorial","0");
		}
		
		this.redirect();
	}
	
	/**
	 * Wechselt den Anzeigemodus der Flotten/Bookmark-Box
	 * @urlparam String box Der Name des neuen Anzeigemodus
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void boxAction() {
		parameterString("box");
		
		String box = getString("box");

		if( !box.equals(this.box) ) {
			getUser().setUserValue("TBLORDER/uebersicht/box", box);
			this.box = box;
		}
		
		redirect();
	}
	
	/**
	 * Beendet das angegebene Quest
	 * @urlparam Integer questid Die ID des zu beendenden Quests
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void stopQuestAction() {
		Database db = getDatabase();
		
		parameterNumber("questid");
		int questid = getInteger("questid");
		
		SQLQuery questdata = db.query("SELECT * FROM quests_running WHERE id='",questid,"'");
		if( !questdata.next() || (questdata.getInt("userid") != getUser().getId()) ) {
			questdata.free();
			addError("Sie k&ouml;nnen dieses Quest nicht abbrechen");
			redirect();
			return;
		}
		
		ScriptParser scriptparser = new ScriptParser( ScriptParser.NameSpace.QUEST );
		if( !getUser().hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
		}

		try {
			Blob execdata = questdata.getBlob("execdata");
			if( (execdata != null) && (execdata.length() > 0) ) {
				scriptparser.setContext(ScriptParserContext.fromStream(execdata.getBinaryStream()));
			}
		}
		catch( Exception e ) {
			LOG.warn("Loading Script-ExecData failed (Quest: "+questid+": ",e);
			redirect();
			return;
		}
		scriptparser.setRegister("USER", Integer.toString(getUser().getId()));
		scriptparser.setRegister("QUEST", "r"+questid);
		scriptparser.executeScript(db, ":0\n!ENDQUEST\n!QUIT","0");
		
		questdata.free();
		
		redirect();	
	}
	
	/**
	 * Zeigt die Uebersicht an
	 */
	@Action(ActionType.DEFAULT)
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		String ticktime = "";
				
		// Letzten Tick ermitteln (Zeitpunkt)
		try {
			BufferedReader bf = new BufferedReader(new FileReader(Configuration.getSetting("LOXPATH")+"ticktime.log"));
			ticktime = bf.readLine();
			bf.close();
		}
		catch(IOException e ) {
			System.err.println(e);
			e.printStackTrace();
		}

		String race = "???";
		if( Rassen.get().rasse(user.getRace()) != null ) {
			race = Rassen.get().rasse(user.getRace()).getName();
		}

		int ticks = getContext().get(ContextCommon.class).getTick();
		
		Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo() );
		
		t.setVar(	"user.name",		Common._title(user.getName()),
				  	"user.race",		race,
				  	"res.nahrung.image",	Cargo.getResourceImage(Resources.NAHRUNG),
				  	"user.nahrung",		Common.ln(usercargo.getResourceCount(Resources.NAHRUNG)),
				  	"global.ticks",		ticks,
				  	"global.ticktime",	ticktime );
				  		  
		// Gibt es eine Umfrage an der wir teilnehmen koennen
		SQLResultRow survey = db.prepare("SELECT * FROM surveys WHERE enabled='1' AND minid<=? AND maxid>=? AND ",
				" mintime<=? AND maxtime>=? AND timeout>0")
				.first(user.getId(), user.getId(), user.getSignup(), user.getSignup());
				
		if( !survey.isEmpty() ) {
			SQLResultRow voted = db.prepare("SELECT * FROM survey_voted WHERE survey_id=? AND user_id=?")
				.first(survey.getInt("id"), user.getId());
			
			if( !voted.isEmpty() ) {
				t.setVar("global.survey", 1);
			}
		}
				  
		UserFlagschiffLocation flagschiff = user.getFlagschiff();
		if( flagschiff != null ) {
			switch( flagschiff.getType() ) {
			case SHIP:
				SQLResultRow ship = db.prepare("SELECT id,name FROM ships WHERE id>0 AND id=?").first(flagschiff.getID());
				if( ship.isEmpty() ) {
					user.setFlagschiff(0);
				} 
				else {
					t.setVar(	"flagschiff.id",	flagschiff.getID(),
								"flagschiff.name",	ship.getString("name") ) ;
				}
				break;
			
			case WERFT_SHIP:
				int dauer = db.first("SELECT remaining FROM werften WHERE shipid=",flagschiff.getID()).getInt("remaining");
				t.setVar("flagschiff.dauer",dauer);
				break;
			
			case WERFT_BASE:
				dauer = db.first("SELECT remaining FROM werften WHERE col=",flagschiff.getID()).getInt("remaining");
				t.setVar("flagschiff.dauer",dauer);
				break;
			}
		}

		//
		// Ingame-Zeit setzen
		//

		String curtime = Common.getIngameTime(ticks);

		t.setVar("time.current", curtime);

		//------------------------------
		// auf neue Nachrichten checken
		//------------------------------

		int newCount = db.first("SELECT count(*) newmsgs FROM transmissionen WHERE empfaenger=",user.getId()," AND gelesen=0").getInt("newmsgs");
		t.setVar("user.newmsgs", Common.ln(newCount));

		//------------------------------
		// Mangel auf Asterodien checken
		//------------------------------

		usercargo = new Cargo( Cargo.Type.STRING, user.getCargo() );

		int bw = 0;
		int bases = 0;

		SQLQuery base = db.prepare("SELECT * FROM bases WHERE owner=? ORDER BY id").query(user.getId());
		while( base.next() ) {
			bases++;
			
			BaseStatus basedata = Base.getStatus(getContext(), new Base(base.getRow()));
			
			Cargo cargo = new Cargo( Cargo.Type.STRING, base.getString("cargo") );
			cargo.addResource( Resources.NAHRUNG, usercargo.getResourceCount(Resources.NAHRUNG) );

			boolean mangel = false;

			ResourceList reslist = basedata.getStatus().getResourceList();
			for( ResourceEntry res : reslist ) {
				if( (res.getCount1() < 0) && (-(cargo.getResourceCount(res.getId())/res.getCount1()) < 9) ) {
					mangel = true;
					break;
				}
			}
			
			if( mangel ) {
				bw++;
			}
		}
		base.free();

		t.setVar("astis.mangel", bw);

		//------------------------------
		// Mangel auf Schiffen checken
		//------------------------------

		int sw = 0;
		int shipNoCrew = 0;

		sw = db.prepare("SELECT count(*) `count` ",
					"FROM ships ",
					"WHERE id>0 AND owner=? AND (LOCATE('mangel_nahrung',status) OR LOCATE('mangel_reaktor',status)) ORDER BY id")
					.first(user.getId())
					.getInt("count");

		shipNoCrew = db.prepare("SELECT count(id) `count` FROM ships ",
							"WHERE id>0 AND owner=? AND LOCATE('nocrew',status)" )
					.first(user.getId())
					.getInt("count");
						  
		String nstat = "0";			  
		if( ticktime.indexOf("Bitte warten") > -1 ) {
			t.setVar("user.nahrung.stat.tick", 1);
		}
		else {
			nstat = user.getNahrungsStat();
		}
						  
		t.setVar(	"schiffe.mangel", Common.ln(sw),
					"schiffe.nocrew", Common.ln(shipNoCrew),
					"user.nahrung.stat", Common.ln(Long.parseLong(nstat)),
					"user.nahrung.stat.plain", Long.parseLong(nstat) );

		//------------------------------
		// Schlachten anzeigen
		//------------------------------
		
		StringBuilder battlelist = new StringBuilder();

		ArrayList<Integer> battleidlist = new ArrayList<Integer>();

		// User darf nur die Eigenen oder Ally-Schlachten sehen
		if( (user.getAccessLevel() < 20) && !user.hasFlag(User.FLAG_VIEW_BATTLES) && !user.hasFlag(User.FLAG_QUEST_BATTLES) ) {
			SQLQuery battle = null;
			if( user.getAlly() != 0 ) {
				battle = db.query("SELECT * FROM battles WHERE commander1=",user.getId()," OR commander2=",user.getId()," OR ally1=",user.getAlly()," OR ally2=",user.getAlly());
			}
			else {
				battle = db.query("SELECT * FROM battles WHERE commander1=",user.getId()," OR commander2=",user.getId());
			}
			
			while( battle.next() ) {
				if( (user.getAlly() != 0) && (battle.getString("visibility") != null) && 
						(battle.getString("visibility").length() > 0) ) {
					Integer[] visibility = Common.explodeToInteger(",", battle.getString("visibility"));
					if( !Common.inArray(user.getId(),visibility) ) {
						continue;	
					}
				}
				battleidlist.add(battle.getInt("id"));

				String eparty = "";
				String comm = "";
				if( ((user.getAlly() == 0) && (battle.getInt("commander1") != user.getId())) || 
					((user.getAlly() != 0) && (battle.getInt("ally1") != user.getAlly()) ) ) {
					if( battle.getInt("ally1") == 0 ) {
						User com1 = (User)getDB().get(User.class, battle.getInt("commander1"));
						eparty = Common._title(com1.getName());
					} 
					else {
						eparty = Common._title(
								db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally1"))
									.getString("name"));
					}
					
					User com2 = (User)getDB().get(User.class, battle.getInt("commander2"));
					comm = Common._title(com2.getName());
				} 
				else {
					if( battle.getInt("ally2") == 0 ) {
						User com2 = (User)getDB().get(User.class, battle.getInt("commander2"));
						eparty = Common._title(com2.getName());
					} 
					else {
						eparty = Common._title(
								db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally2"))
									.getString("name"));
					}
					
					User com1 = (User)getDB().get(User.class, battle.getInt("commander1"));
					comm = Common._title(com1.getName());
				}
				
				if( user.getAlly() != 0 ) {
					battlelist.append("<a class=\"error\" href=\"main.php?module=angriff&amp;sess="+getString("sess")+"&amp;battle="+battle.getInt("id")+"\">Schlacht mit "+eparty+" bei "+battle.getInt("system")+" : "+battle.getInt("x")+"/"+battle.getInt("y")+"</a> ["+comm+"]<br />\n");
				}
				else {
					battlelist.append("<a class=\"error\" href=\"main.php?module=angriff&amp;sess="+getString("sess")+"&amp;battle="+battle.getInt("id")+"\">Es ist eine Schlacht mit "+eparty+" bei "+battle.getInt("system")+" : "+battle.getInt("x")+"/"+battle.getInt("y")+" im Gange</a><br />\n");
				}
			}
			battle.free();
		}

		SQLQuery battle = null;
		
		//Nun alle Schlachten auflisten, wo der Spieler Schiffe drin hat (die aber noch nicht aufgelistet wurden) - oder zeige alle Schlachten an, wenn es jemand mit entsprechenden Rechten ist
		if( (user.getAccessLevel() < 20) && user.hasFlag(User.FLAG_QUEST_BATTLES) ) {
			battle = db.query("SELECT * FROM battles WHERE (quest IS NOT NULL AND (commander1<0 XOR commander2<0)) OR (commander1=",user.getId(),") OR (commander2=",user.getId(),")");
		}
		else if( (user.getAccessLevel() >= 20) || user.hasFlag(User.FLAG_VIEW_BATTLES) ) {
			battle = db.query("SELECT * FROM battles");
		}
		else {			
			battlelist.append("<br />\n");
			battle = db.query("SELECT t1.* ",
							"FROM battles t1,ships t2 ",
							"WHERE t2.id>0 AND t2.owner=",user.getId()," AND t2.battle=t1.id ",(!battleidlist.isEmpty() ? "AND !(t2.battle IN ("+Common.implode(",",battleidlist)+"))":"")," ",
							"GROUP BY t1.id" );
		}
		
		while( battle.next() ) {
			if( !user.hasFlag(User.FLAG_QUEST_BATTLES) && (battle.getInt("quest") != 0) ) {
				continue;
			}
			String eparty = "";
			String eparty2 = "";
			if( battle.getInt("ally1") == 0 ) {
				User com1 = (User)getDB().get(User.class, battle.getInt("commander1"));
				eparty = Common._title(com1.getName());
			} 
			else {
				eparty = Common._title(
							db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally1"))
								.getString("name"));
			}
			User com1 = (User)getDB().get(User.class, battle.getInt("commander1"));
			String comm1 = Common._title(com1.getName());

			if( battle.getInt("ally2") == 0 ) {
				User com2 = (User)getDB().get(User.class, battle.getInt("commander2"));
				eparty2 = Common._title(com2.getName());
			} 
			else {
				eparty2 = Common._title(
						db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally2"))
							.getString("name"));
			}
			User com2 = (User)getDB().get(User.class, battle.getInt("commander2"));
			String comm2 = Common._title(com2.getName());
		
			battlelist.append("<a class=\"error\" href=\"main.php?module=angriff&amp;sess="+getString("sess")+"&amp;battle="+battle.getInt("id")+"\">Schlacht "+eparty+" vs "+eparty2+" bei "+battle.getInt("system")+":"+battle.getInt("x")+"/"+battle.getInt("y")+"</a><br />*&nbsp;["+comm1+" vs "+comm2+"]<br />\n");
			
			if( ( (user.getAccessLevel() >= 20) || user.hasFlag(User.FLAG_QUEST_BATTLES) ) 
				&& (battle.getInt("quest") != 0) ) {
				String questname = db.first("SELECT t2.name FROM quests_running t1, quests t2 WHERE t1.id='",battle.getInt("quest"),"' AND t1.questid=t2.id").getString("name");
				battlelist.append("*&nbsp;[Quest: "+questname+"]<br />\n");
			}
			
			battlelist.append("<br />\n");
		}
		battle.free();

		t.setVar("global.battlelist", battlelist);
		//------------------------------
		// Logo anzeigen
		//------------------------------

		if( user.getAlly() != 0 ) {
			t.setVar("ally.logo", user.getAlly());
		}

		//------------------------------
		// Interaktives Tutorial
		//------------------------------

		int shipcount = db.first("SELECT count(*) `count` FROM ships WHERE id>0 AND owner='",user.getId(),"'").getInt("count");
		int inttutorial = Integer.parseInt(user.getUserValue("TBLORDER/uebersicht/inttutorial"));

		if( inttutorial != 0 ) {
			boolean reqname = false;
			boolean reqship = false;
			boolean reqbase = false;
	
			if( !user.getName().equals("Kolonist") ) {
				reqname = true;
			}
		
			if( shipcount > 0 ) {
				reqship = true;
			}
	
			if( bases > 0 ) {
				reqbase = true;
			}
			
			if( !reqship ) {
				reqbase = true;
			}
			
			if( !reqname ) {
				reqship = true;
				reqbase = true;
			}

			SQLResultRow sheet = db.first("SELECT id,headimg,text FROM inttutorial WHERE id='",inttutorial,"' AND reqname='",(reqname ? 1 : 0),"' AND reqbase='",(reqbase ? 1 : 0),"' AND reqship='",(reqship ? 1 : 0),"'");
	
			// Ist die aktuelle Tutorialseite veraltet?
			if( sheet.isEmpty() || (sheet.getInt("id") != inttutorial) ) {		
				sheet = db.first("SELECT id,headimg,text FROM inttutorial WHERE reqname='",(reqname ? 1 : 0),"' AND reqbase='",(reqbase ? 1 : 0),"' AND reqship='",(reqship ? 1 : 0),"' ORDER BY id");
		
				// Neue Tutorialseite speichern
				inttutorial = sheet.getInt("id");
				user.setUserValue("TBLORDER/uebersicht/inttutorial", Integer.toString(inttutorial));
			}
	
			// Existiert eine Nachfolgerseite?
			SQLResultRow nextsheet = db.first("SELECT id,headimg,text FROM inttutorial WHERE reqsheet='",inttutorial,"' AND reqname='",(reqname ? 1 : 0),"' AND reqbase='",(reqbase ? 1 : 0),"' AND reqship='",(reqship ? 1 : 0),"'");
	
			// Kann das Tutorial jetzt beendet werden?
			if( nextsheet.isEmpty() && reqname && reqship && reqbase ) {
				t.setVar("sheet.endtutorial",1);
			}
			else if( !nextsheet.isEmpty() ) {
				t.setVar("sheet.nextsheet",1);
			}
	
			t.setVar(	"interactivetutorial.show",	1,
						"sheet.headpic",			sheet.getString("headimg"),
						"sheet.text",				Common._text(sheet.getString("text")) );
		}

		//------------------------------------
		// Die Box (Bookmarks/Flotten)
		//------------------------------------

		t.setBlock("_UEBER","bookmarks.listitem","bookmarks.list");
		t.setBlock("_UEBER","fleets.listitem","fleets.list");
		t.setVar(	"bookmarks.list",	"",
					"fleets.list",		"" );

		if( box.equals("bookmarks") ) {
			t.setVar("show.bookmarks",1);
	
			SQLQuery bookmark = db.query("SELECT id,name,x,y,system,destx,desty,destsystem,destcom,status,type FROM ships WHERE id>0 AND bookmark=1 AND owner=",user.getId()," ORDER BY id DESC");
			while( bookmark.next() ) {
				SQLResultRow shiptype = ShipTypes.getShipType( bookmark.getRow() );
				t.setVar(	"bookmark.shipid",		bookmark.getInt("id"),
							"bookmark.shipname",	bookmark.getString("name"),
							"bookmark.location",	Ships.getLocationText(bookmark.getRow(), false),
							"bookmark.shiptype",	shiptype.getString("nickname"),
							"bookmark.description",	bookmark.getInt("destsystem")+":"+bookmark.getInt("destx")+"/"+bookmark.getInt("desty")+"<br />"+bookmark.getString("destcom").replace("\r\n","<br />") );
				t.parse("bookmarks.list","bookmarks.listitem",true);
			}
			bookmark.free();
		}
		else if( box.equals("fleets") ) {
			t.setVar("show.fleets",1);
			boolean jdocked = false;
			
			SQLQuery fleet = db.query("SELECT count(*) as shipcount,t1.x,t1.y,t1.system,t1.id,t1.docked,t2.name FROM ships t1,ship_fleets t2 WHERE t1.id>0 AND t1.owner=",user.getId()," AND t1.fleet=t2.id GROUP BY t2.id ORDER BY t1.docked,t1.system,t1.x,t1.y");
			while( fleet.next() ) {
				if( !jdocked && (fleet.getString("docked").indexOf('l') == 0) ) {
					jdocked = true;
					t.setVar( "fleet.jaegerfleet", 1 );
				}
				else {
					t.setVar( "fleet.jaegerfleet", 0 );
				}

				t.setVar(	"fleet.shipid",		fleet.getInt("id"),
							"fleet.name",		fleet.getString("name"),
							"fleet.location",	Ships.getLocationText(fleet.getRow(), false),
							"fleet.shipcount",	fleet.getInt("shipcount") );
				t.parse("fleets.list","fleets.listitem",true);
			}
			fleet.free();
		}

		//------------------------------------
		// Die Quests
		//------------------------------------
		t.setBlock("_UEBER","quests.listitem","quests.list");
		t.setVar("quests.list","");
		
		SQLQuery quest = db.query("SELECT t1.statustext,t1.id,t2.name FROM quests_running t1,quests t2 WHERE t1.userid='",user.getId(),"' AND t1.publish='1' AND t1.questid=t2.id");
		while( quest.next() ) {
			t.setVar(	"quest.name",		quest.getString("name"),
						"quest.statustext",	quest.getString("statustext"),
						"quest.id",			quest.getInt("id"));
								
			t.parse("quests.list", "quests.listitem", true);
		}
		quest.free();
	}

}
