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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.IntTutorial;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Die Uebersicht.
 * @author Christopher Jung
 *
 */
@Configurable
public class UeberController extends TemplateGenerator {
	private static final Log log = LogFactory.getLog(UeberController.class);
	
	private String box = "";
	
	private Configuration config;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public UeberController(Context context) {
		super(context);
		
		setTemplate("ueber.html");
	}
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }

	@Override
	protected boolean validateAndPrepare(String action) {
		box = getUser().getUserValue("TBLORDER/uebersicht/box");

		return true;
	}
	
	/**
	 * Beendet den Vacation-Modus-Vorlauf.
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
	 * Wechselt die Tutorial-Seite bzw beendet das Tutorial.
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
	 * Wechselt den Anzeigemodus der Flotten/Bookmark-Box.
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
	 * Beendet das angegebene Quest.
	 * @urlparam Integer questid Die ID des zu beendenden Quests
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void stopQuestAction() {
		org.hibernate.Session db = getDB();
		
		parameterNumber("questid");
		int questid = getInteger("questid");
		
		RunningQuest questdata = (RunningQuest)db.get(RunningQuest.class, questid);
		if( (questdata == null) || (questdata.getUser().getId() != getUser().getId()) ) {
			addError("Sie k&ouml;nnen dieses Quest nicht abbrechen");
			redirect();
			return;
		}
		
		ScriptEngine scriptparser = new ScriptEngineManager().getEngineByName("DSQuestScript");
		if( !getUser().hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
			scriptparser.getContext().setErrorWriter(new NullLogger());
		}

		try {
			Blob execdata = questdata.getExecData();
			if( (execdata != null) && (execdata.length() > 0) ) {
				scriptparser.setContext(ScriptParserContext.fromStream(execdata.getBinaryStream()));
			}
		}
		catch( Exception e ) {
			log.warn("Loading Script-ExecData failed (Quest: "+questid+": ",e);
			redirect();
			return;
		}
		final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
		
		engineBindings.put("USER", Integer.toString(getUser().getId()));
		engineBindings.put("QUEST", "r"+questid);
		engineBindings.put("_PARAMETERS", "0");
		try {
			scriptparser.eval(":0\n!ENDQUEST\n!QUIT");
		}
		catch( ScriptException e ) {
			throw new RuntimeException(e);
		}
		
		redirect();	
	}
	
	/**
	 * Zeigt die Uebersicht an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		String ticktime = getTickTime();

		String race = "???";
		if( Rassen.get().rasse(user.getRace()) != null ) {
			race = Rassen.get().rasse(user.getRace()).getName();
		}
		
		int ticks = getContext().get(ContextCommon.class).getTick();
		ConfigValue value = (ConfigValue)getDB().get(ConfigValue.class, "corruption");
		double corruption = Double.valueOf(value.getValue()) + user.getCorruption();

		int[] fullbalance = user.getFullBalance();
		if(fullbalance[1] > 0 && corruption > 0)
		{
			fullbalance[1] *= 1-corruption;
		}
				
		t.setVar(	"user.name",				Common._title(user.getName()),
				  	"user.race",				race,
				  	"res.nahrung.image",		Cargo.getResourceImage(Resources.NAHRUNG),
				  	"res.re.image",				Cargo.getResourceImage(Resources.RE),
				  	"user.nahrung.new", 		Common.ln(fullbalance[0]),
					"user.nahrung.new.plain", 	fullbalance[0],
				  	"user.konto",				Common.ln(user.getKonto()),
				  	"user.balance",				Common.ln(fullbalance[1]),
				  	"user.balance.plain",		fullbalance[1],
				  	"user.specpoints",			user.getFreeSpecializationPoints(),
				  	"user.maxspecpoints",		user.getSpecializationPoints(),
				  	"global.ticks",				ticks,
				  	"global.ticktime",			ticktime );
				  
		UserFlagschiffLocation flagschiff = user.getFlagschiff();
		if( flagschiff != null ) {
			switch( flagschiff.getType() ) {
			case SHIP:
				Ship ship = (Ship)db.get(Ship.class, flagschiff.getID());
				if( ship == null ) {
					user.setFlagschiff(null);
				} 
				else {
					t.setVar(	"flagschiff.id",	flagschiff.getID(),
								"flagschiff.name",	ship.getName() ) ;
				}
				break;
			
			case WERFT_SHIP: {
				WerftObject werft = (WerftObject)db.createQuery("from ShipWerft where ship=?")
					.setInteger(0, flagschiff.getID())
					.uniqueResult();
				
				if( werft.getKomplex() != null ) {
					werft = werft.getKomplex();
				}
				
				WerftQueueEntry[] entries = werft.getBuildQueue();
				for( int i=0; i < entries.length; i++ ) {
					if( entries[i].isBuildFlagschiff() ) {
						t.setVar("flagschiff.dauer", werft.getTicksTillFinished(entries[i]));
						break;
					}
				}
				
				break;
			}
			case WERFT_BASE: {
				WerftObject werft = (WerftObject)db.createQuery("from BaseWerft where base=?")
					.setInteger(0, flagschiff.getID())
					.uniqueResult();
				
				if( werft.getKomplex() != null ) {
					werft = werft.getKomplex();
				}
				
				WerftQueueEntry[] entries = werft.getBuildQueue();
				for( int i=0; i < entries.length; i++ ) {
					if( entries[i].isBuildFlagschiff() ) {
						t.setVar("flagschiff.dauer", werft.getTicksTillFinished(entries[i]));
						break;
					}
				}
				break;
			}
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

		long newCount = (Long)db.createQuery("select count(*) from PM where empfaenger= :user and gelesen=0")
			.setEntity("user", user)
			.iterate().next();
		t.setVar("user.newmsgs", Common.ln(newCount));

		//------------------------------
		// Mangel auf Asteroiden checken
		//------------------------------

		int bw = 0;
		int bases = 0;

		List<?> basen = db.createQuery("from Base where owner= :user order by id")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=basen.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			bases++;
			
			BaseStatus basedata = Base.getStatus(getContext(), base);
			
			Cargo cargo = new Cargo(base.getCargo());
			cargo.addResource(Resources.RE, user.getKonto().longValue());
			
			boolean mangel = false;

			ResourceList reslist = basedata.getProduction().getResourceList();
			for( ResourceEntry res : reslist ) {
				if( (res.getCount1() < 0) && (-(cargo.getResourceCount(res.getId())/res.getCount1()) <= 9) ) {
					mangel = true;
					break;
				}
			}
			
			if(basedata.getEnergy() < 0 && base.getEnergy() / -basedata.getEnergy() <=9)
			{
				mangel = true;
			}
			
			if( mangel ) {
				bw++;
			}
		}

		t.setVar("astis.mangel", bw);

		//------------------------------
		// Mangel auf Schiffen checken
		//------------------------------

		long sw = 0;
		long shipNoCrew = 0;

		sw = (Long)db.createQuery("select count(*) " +
					"from Ship " +
					"where id>0 and owner= :user and (locate('mangel_nahrung',status)!=0 or locate('mangel_reaktor',status)!=0) and locate('nocrew',status)=0")
				.setEntity("user", user)
				.iterate().next();

		shipNoCrew = (Long)db.createQuery("select count(*) from Ship as s where s.id>0 and s.owner= :user" +
				" and ((s.modules is not null and s.crew < (select crew from ShipModules where id=s.modules)) or s.crew < (select crew from ShipType where id = s.shiptype))" )
				.setEntity("user", user)
				.iterate().next();
						  
		t.setVar(	"schiffe.mangel", Common.ln(sw),
					"schiffe.nocrew", Common.ln(shipNoCrew) );

		//------------------------------
		// Schlachten anzeigen
		//------------------------------
		
		StringBuilder battlelist = new StringBuilder();

		// Ab hier beginnt das erste Bier
		Set<Battle> battles = new LinkedHashSet<Battle>();

		if(user.getAccessLevel() < 20 && !user.hasFlag(User.FLAG_VIEW_BATTLES)) {
			// Zwei separate Queries fuer alle Schlachten um einen sehr unvorteilhaften Join zu vermeiden
			String query = "from Battle " +
					"where commander1= :user or commander2= :user ";
			
			//hat der Benutzer eine ally, dann haeng das hier an
			if(user.getAlly() != null) {
				query += " or ally1 = :ally or ally2 = :ally";
			}
			// ach haengen wir mal den quest kram dran
			if(user.hasFlag(User.FLAG_QUEST_BATTLES)){
				query += " or quest is not null";	
			}
			
			Query battleQuery = db.createQuery(query)
				.setEntity("user", user);
			
			if( user.getAlly() != null ) {
				battleQuery = battleQuery.setInteger("ally", user.getAlly().getId());
			}
			
			battles.addAll(Common.cast(battleQuery.list(), Battle.class));
			
			battles.addAll(Common.cast(
				db.createQuery("select distinct battle from Ship where battle is not null and owner=:user")
					.setEntity("user", user)
					.list(),		
				Battle.class));
		}
		// Bei entsprechendem AccessLevel/Flag alle Schlachten anzeigen
		else {
			battles.addAll(Common.cast(db.createQuery("from Battle").list(), Battle.class));
		}
		
		// Ab hier beginnt das zweite Bier
		for( Battle battle : battles ) {
			String eparty = "";
			String eparty2 = "";
			if( battle.getAlly(0) == 0 ) {
				final User commander1 = battle.getCommander(0);
				eparty = Common._title(commander1.getName());
			} 
			else {
				final Ally ally = (Ally)db.get(Ally.class, battle.getAlly(0));
				eparty = Common._title(ally.getName());
			}
			
			if( battle.getAlly(1) == 0 ) {
				final User commander2 = battle.getCommander(1);
				eparty2 = Common._title(commander2.getName());
			} 
			else {
				final Ally ally = (Ally)db.get(Ally.class, battle.getAlly(1));
				eparty2 = Common._title(ally.getName());
			}
					
			
			battlelist.append("<a class=\"error\" href=\"ds?module=angriff&amp;battle="+battle.getId()+"\">Schlacht "+eparty+" vs "+eparty2+" bei "+battle.getLocation().displayCoordinates(false)+"</a>&nbsp;");
			
			
			if( ( (user.getAccessLevel() >= 20) || user.hasFlag(User.FLAG_QUEST_BATTLES) ) 
				&& (battle.getQuest() != null) ) {
				RunningQuest quest = (RunningQuest)db.get(RunningQuest.class, battle.getQuest());
				battlelist.append("*&nbsp;[Quest: "+quest.getQuest().getName()+"]");
			}
			
			// Nahrunganzeige der Schlacht
			int nahrung = battle.getNahrungsBalance(user);
			
			if ( nahrung < 0 )
			{
				battlelist.append("<span style=\"color:red\">("+Common.ln(nahrung));
			}
			else
			{
				battlelist.append("<span style=\"color:green\">(+"+Common.ln(nahrung));
			}
			
			battlelist.append(" <img src=\""+Cargo.getResourceImage(Resources.NAHRUNG)+"\" alt=\"Nahrung\" title=\"Nahrung\" />)</span>");
			
			// RE Anzeige der Schlacht
			int re = battle.getBalance(user);

			if ( re < 0 )
			{
				battlelist.append("&nbsp;<span style=\"color:red\">("+Common.ln(re));
			}
			else
			{
				battlelist.append("&nbsp;<span style=\"color:green\">(+"+Common.ln(re));
			}
			
			battlelist.append(" RE)</span>");
			
			battlelist.append("<br />\n");
		}

		t.setVar("global.battlelist", battlelist);
		//------------------------------
		// Logo anzeigen
		//------------------------------

		if( user.getAlly() != null ) {
			t.setVar("ally.logo", user.getAlly().getId());
		}

		//------------------------------
		// Interaktives Tutorial
		//------------------------------

		int inttutorial = Integer.parseInt(user.getUserValue("TBLORDER/uebersicht/inttutorial"));

		if( inttutorial != 0 ) {
			long shipcount = (Long)db.createQuery("select count(*) from Ship " +
			"where id>0 and owner= :user")
			.setEntity("user", user)
			.iterate().next();
			showTutorialPages(bases, shipcount, inttutorial);
		}

		//------------------------------------
		// Die Box (Bookmarks/Flotten)
		//------------------------------------

		t.setBlock("_UEBER","bookmarks.listitem","bookmarks.list");
		t.setBlock("_UEBER","fleets.listitem","fleets.list");
		t.setVar(	"bookmarks.list",	"",
					"fleets.list",		"" );


		// Bookmarks zusammenbauen
		t.setVar("show.bookmarks",1);

		List<?> bookmarks = db.createQuery("from Ship where id>0 and bookmark=1 and owner=? order by id desc")
			.setEntity(0, user)
			.list();
		for( Iterator<?> iter=bookmarks.iterator(); iter.hasNext(); ) {
			Ship bookmark = (Ship)iter.next();
			ShipTypeData shiptype = bookmark.getTypeData();
			t.setVar(	"bookmark.shipid",		bookmark.getId(),
						"bookmark.shipname",	bookmark.getName(),
						"bookmark.location",	bookmark.getLocation().displayCoordinates(false),
						"bookmark.shiptype",	shiptype.getNickname(),
						"bookmark.description",	bookmark.getDestSystem()+":"+bookmark.getDestX()+"/"+bookmark.getDestY()+"<br />"+bookmark.getDestCom().replace("\r\n","<br />") );
			t.parse("bookmarks.list","bookmarks.listitem",true);
		}
		// Flotten zusammenbauen
		t.setVar("show.fleets",1);
		boolean jdocked = false;
		
		List<?> fleets = db.createQuery("select count(*),s.fleet from Ship s " +
				"where s.id>0 and s.owner= :user and s.fleet!=0 " +
				"group by s.fleet " +
				"order by s.docked,s.system,s.x,s.y")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=fleets.iterator(); iter.hasNext(); ) {
			Object[] data = (Object[])iter.next();
			long count = (Long)data[0];
			ShipFleet fleet = (ShipFleet)data[1];
			
			Ship aship = (Ship)db.createQuery("from Ship where fleet=?")
				.setEntity(0, fleet)
				.iterate().next();
			
			if( !jdocked && aship.isLanded() ) {
				jdocked = true;
				t.setVar( "fleet.jaegerfleet", 1 );
			}
			else {
				t.setVar( "fleet.jaegerfleet", 0 );
			}
			
			Ship baseShip = aship.getBaseShip();
			
			String locationText;
			if(baseShip == null)
			{
				locationText = aship.getLocation().displayCoordinates(false); 
			}
			else
			{
				locationText = baseShip.getLocation().displayCoordinates(false);
			}

			t.setVar(	"fleet.shipid",		aship.getId(),
						"fleet.name",		fleet.getName(),
						"fleet.location",	locationText,
						"fleet.shipcount",	count );
			t.parse("fleets.list","fleets.listitem",true);
		}

		//------------------------------------
		// Die Quests
		//------------------------------------
		t.setBlock("_UEBER","quests.listitem","quests.list");
		t.setVar("quests.list","");
		
		List<?> quests = db.createQuery("from RunningQuest rq inner join fetch rq.quest " +
				"where rq.user= :user and rq.publish=1")
			.setEntity("user", user)
			.list();
		for( Iterator<?> iter=quests.iterator(); iter.hasNext(); ) {
			RunningQuest quest = (RunningQuest)iter.next();
			
			t.setVar(	"quest.name",		quest.getQuest().getName(),
						"quest.statustext",	quest.getStatusText(),
						"quest.id",			quest.getId());
								
			t.parse("quests.list", "quests.listitem", true);
		}
	}

	private String getTickTime() {
		String ticktime = "";
		
		// Letzten Tick ermitteln (Zeitpunkt)
		try
		{
			BufferedReader bf = new BufferedReader(new FileReader(config.get("LOXPATH")+"ticktime.log"));
			try
			{
				ticktime = bf.readLine();
			}
			finally
			{
				bf.close();
			}
		}
		catch(IOException e )
		{
			System.err.println(e);
			e.printStackTrace();
		}
		
		if( ticktime == null )
		{
			ticktime = "";
		}
		return ticktime;
	}

	private void showTutorialPages(int bases, long shipcount, int inttutorial) {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
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

		IntTutorial sheet = (IntTutorial)db.createQuery("from IntTutorial where id= :id and reqName= :reqname and reqBase= :reqbase and reqShip= :reqship")
			.setInteger("id", inttutorial)
			.setInteger("reqname", reqname ? 1 : 0)
			.setInteger("reqbase", reqbase ? 1 : 0)
			.setInteger("reqship", reqship ? 1 : 0)
			.uniqueResult();

		// Ist die aktuelle Tutorialseite veraltet?
		if( (sheet == null) || (sheet.getId() != inttutorial) ) {
			sheet = (IntTutorial)db.createQuery("from IntTutorial where reqName= :reqname and reqBase= :reqbase and reqShip= :reqship order by id")
				.setInteger("reqname", reqname ? 1 : 0)
				.setInteger("reqbase", reqbase ? 1 : 0)
				.setInteger("reqship", reqship ? 1 : 0)
				.setMaxResults(1)
				.uniqueResult();
			
			if( sheet == null ) {
				user.setUserValue("TBLORDER/uebersicht/inttutorial", "0");
				return;
			}
			
			// Neue Tutorialseite speichern
			inttutorial = sheet.getId();
			user.setUserValue("TBLORDER/uebersicht/inttutorial", Integer.toString(inttutorial));
		}

		// Existiert eine Nachfolgerseite?
		IntTutorial nextsheet = (IntTutorial)db.createQuery("from IntTutorial where reqSheet= :reqsheet and reqName= :reqname and reqBase= :reqbase and reqShip= :reqship")
			.setInteger("reqsheet", sheet.getId())
			.setInteger("reqname", reqname ? 1 : 0)
			.setInteger("reqbase", reqbase ? 1 : 0)
			.setInteger("reqship", reqship ? 1 : 0)
			.setMaxResults(1)
			.uniqueResult();

		// Kann das Tutorial jetzt beendet werden?
		if( (nextsheet == null) && reqname && reqship && reqbase ) {
			t.setVar("sheet.endtutorial",1);
		}
		else if( nextsheet != null ) {
			t.setVar("sheet.nextsheet",1);
		}

		t.setVar(	"interactivetutorial.show",	1,
					"sheet.headpic",			sheet.getHeadImg(),
					"sheet.text",				Common._text(sheet.getText()) );
	}

}
