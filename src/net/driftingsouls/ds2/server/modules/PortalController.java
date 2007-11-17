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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.RandomUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.SectorTemplateManager;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.uilibs.PlayerList;

/**
 * Das Portal
 * @author Christopher Jung
 *
 */
class PortalController extends DSGenerator {
	private int retries = 5;

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public PortalController(Context context) {
		super(context);

		requireValidSession(false);
		
		setTemplate("portal.html");
	}
	
	@Override
	protected void printHeader( String action ) {
		// EMPTY
	}
	
	@Override
	protected boolean validateAndPrepare( String action ) {
		TemplateEngine t = getTemplateEngine();
		
		t.set_var(	"TUTORIAL_ID", Configuration.getSetting("ARTICLE_TUTORIAL"),
					"FAQ_ID", Configuration.getSetting("ARTICLE_FAQ"),
					"URL", Configuration.getSetting("URL") );
							
		return true;	
	}
	
	/**
	 * Zeigt die Liste der Downloads an
	 *
	 */
	public void downloadAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		t.set_var("show.downloads", 1);
		t.set_block("_PORTAL", "downloads.listitem", "downloads.list");
		SQLQuery dl = db.query("SELECT * FROM portal_downloads ORDER BY `date` DESC");
		while( dl.next() ) {
			t.set_var(	"download.name", Common._plaintitle(dl.getString("name")),
						"download.file", dl.getString("file"),
						"download.date", Common.date("j.n.Y G:i",dl.getInt("date")),
						"download.description", Common._plaintext(dl.getString("description")) );
								
			t.parse("downloads.list", "downloads.listitem", true);
		}
		dl.free();
	}
	
	/**
	 * Ermoeglicht das generieren eines neuen Passworts und anschliessenden
	 * zumailens dessen
	 * @urlparam String username der Benutzername des Accounts
	 *
	 */
	public void passwordLostAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("username");
		String username = getString("username");

		if( "".equals(username) ) {
			t.set_var("show.passwordlost",1);
		}
		else {
			username = db.prepareString(username);
			SQLResultRow row = db.first("SELECT email,id FROM users WHERE un='",username,"'");
			String email = row.getString("email");
			int loguserid = row.getInt("id");

			if( !"".equals(email) ) {
				String password = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE));
				String enc_pw = Common.md5(password);
				db.update("UPDATE users SET passwort='",enc_pw,"' WHERE un='",username,"'");

				String subject = "Neues Passwort fuer Drifting Souls 2";
				
				String message = Configuration.getSetting("PWNEW_EMAIL").replace("{username}", getString("username"));
				message = message.replace("{password}", password);
				message = message.replace("{date}", Common.date("H:i j.m.Y"));
				
				Common.mail( email, subject, message );
				
				Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+loguserid+") <"+username+"> Passwortanforderung von Browser <"+getRequest().getUserAgent()+">\n");
		
				t.set_var(	"show.passwordlost.msg.ok", 1,
							"passwordlost.email", email );
			}
			else {
				Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+loguserid+") <"+username+"> Passwortanforderung von Browser <"+getRequest().getUserAgent()+">\n");

				t.set_var("show.passwordlost.msg.error",1);
			}
		}
	}
	
	/**
	 * Zeigt allgemeine Infos an
	 *
	 */
	public void infosAction() {
		getTemplateEngine().set_var("show.infos",1);
	}
	
	/**
	 * Zeigt einen Artikel an oder, falls keiner angegeben ist, die Liste
	 * der Artikel
	 * @urlparam Integer artikel Der anzuzeigende Artikel. Falls 0, dann die Liste der Artikel
	 * @urlparam Integer page Die anzuzeigende Seite des Artikels
	 *
	 */
	public void infosArtikelAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("artikel");
		int artikel = getInteger("artikel");

		if( artikel == 0 ) {
			t.set_var("show.articlelist",1);
			t.set_block("_PORTAL","articles.listitem","articles.list");

			SQLQuery article = db.query("SELECT id,title,author FROM portal_articles ORDER BY id");
			while( article.next() ) {
				t.set_var(	"article.id", article.getInt("id"),
							"article.title", article.getString("title"),
							"article.author", article.getString("author") );
				t.parse("articles.list","articles.listitem",true);
			}
			article.free();
		} 
		else {
			parameterNumber("page");
			int page = getInteger("page");

			if( page == 0 ) {
				page = 1;
			}

			SQLResultRow article = db.first("SELECT title,author,article FROM portal_articles WHERE id='",artikel,"'");

			String[] pages = article.getString("article").split("\\[nextpage\\]");
			String text = pages[page-1];

			t.set_var(	"show.article", 1,
						"article.id", artikel,
						"article.title", article.getString("title"),
						"article.author", article.getString("author"),
						"article.text", Common._text(text),
						"article.nextprev", pages.length > 1,
						"article.prevpage", ((page > 1) ? (page-1) : 0),
						"article.nextprevpage", (page-1 > 0) && (page < pages.length),
						"article.nextpage", (page < pages.length ? (page+1) : 0 ) );
	
		}
	}
	
	/**
	 * Zeigt die Liste der registrierten Spieler an
	 *
	 */
	public void infosPlayerlistAction() {
		StringBuffer context = getContext().getResponse().getContent();
		getContext().getResponse().resetContent();
		
		PlayerList.draw(getContext());
		
		StringBuffer plist = getContext().getResponse().getContent();
		getContext().getResponse().setContent(context.toString());

		getTemplateEngine().set_var(	"show.plist", 1,
										"plist.text", plist.toString() );
	}
	
	private static Map<String,String> articleClasses = new HashMap<String,String>();
	static {
		articleClasses.put("ship", "Schiffe");
		articleClasses.put("race", "Rassen");
		articleClasses.put("groups", "Gruppierungen");
		articleClasses.put("history", "Geschichte");
		articleClasses.put("other", "Andere Fakten");
	}
	
	/**
	 * Zeigt die Daten und Fakten an
	 *
	 */
	public void infosDfAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		t.set_var("show.df",1);

		t.set_block("_PORTAL","facts.articlegroup.listitem","facts.articlegroup.list");
		t.set_block("facts.articlegroup.listitem","facts.articles.listitem","facts.articles.list");

		String oldclass = null;

		SQLQuery article = db.query("SELECT id,class,title FROM portal_facts ORDER BY class,title");
		while( article.next() ) {
			if( (oldclass != null) && !oldclass.equals(article.getString("class")) ) {
				t.set_var("articlegroup.name",articleClasses.get(oldclass));
				t.parse("facts.articlegroup.list","facts.articlegroup.listitem",true);
				
				t.set_var("facts.articles.list","");
			}
			oldclass = article.getString("class");

			t.set_var(	"article.id"	, article.getInt("id"),
						"article.title"	, Common._title(article.getString("title")) );

			t.parse("facts.articles.list","facts.articles.listitem",true);
		}
		article.free();

		t.set_var("articlegroup.name",articleClasses.get(oldclass));
		t.parse("facts.articlegroup.list","facts.articlegroup.listitem",true);

		parameterNumber("article");
		int articleID = getInteger("article");
		
		if( articleID != 0 ) {
			SQLResultRow thisarticle = db.first("SELECT title,text,class FROM portal_facts WHERE id='",articleID,"'");

			t.set_var(	"article.class"	, articleClasses.get(thisarticle.getString("class")),
						"article.title"	, Common._title(thisarticle.getString("title")),
						"article.text"	, Common._text(thisarticle.getString("text")) );
		}
	}
	
	/**
	 * Zeigt die AGB an
	 *
	 */
	public void infosAgbAction() {
		getTemplateEngine().set_var("show.agb",1);
	}

	/**
	 * Zeigt das Impressum an
	 *
	 */
	public void impressumAction() {
		getTemplateEngine().set_var("show.impressum",1);
	}

	/**
	 * Zeigt die Links an
	 *
	 */
	public void linksAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.set_var("show.links",1);
		t.set_block("_PORTAL","links.listitem","links.list");

		SQLQuery link = db.query("SELECT url,name,descrip FROM portal_links ORDER BY id");
		while( link.next() ) {
			t.set_var(	"link.url", link.getString("url"),
						"link.name", link.getString("name"),
						"link.description", link.getString("descrip") );
			t.parse("links.list","links.listitem",true);
		}
		link.free();
	}

	/**
	 * Zeigt den JavaChat an
	 *
	 */
	public void javachatAction() {
		getTemplateEngine().set_var("show.javachat",1);
	}
	
	private static class StartLocations {
		int systemID;
		int orderLocationID;
		HashMap<Integer,StartLocation> minSysDistance;

		StartLocations(int systemID, int orderLocationID, HashMap<Integer,StartLocation> minSysDistance) {
			this.systemID = systemID;
			this.orderLocationID = orderLocationID;
			this.minSysDistance = minSysDistance;
		}
	}
	
	private static class StartLocation {
		int orderLocationID;
		int distance;

		StartLocation(int orderLocationID, int distance) {
			this.orderLocationID = orderLocationID;
			this.distance = distance;
		}
	}
	
	private StartLocations getStartLocation() {
		Database db = getDatabase();
		
		int systemID = 0;
		int orderLocationID = 0;
		int mindistance = 99999;
		HashMap<Integer,StartLocation> minsysdistance = new HashMap<Integer,StartLocation>();
		
		for( StarSystem system : Systems.get() ) {
			Location[] locations = system.getOrderLocations();
			
			for( int i=0; i < locations.length; i++ ) {
				int dist = 0;
				int count = 0;
				SQLQuery adist = db.query("SELECT sqrt((",locations[i].getX(),"-x)*(",locations[i].getX(),"-x)+(",locations[i].getY(),"-y)*(",locations[i].getY(),"-y)) distance FROM bases WHERE owner=0 AND system='",system.getID(),"' AND klasse=1 ORDER BY distance LIMIT 15");
				while( adist.next() ) {
					dist += adist.getInt("distance");
					count++;
				}
				adist.free();
				
				if( count < 15 ) {
					continue;
				}
				
				if( !minsysdistance.containsKey(system.getID()) || (minsysdistance.get(system.getID()).distance > dist) ) {
					minsysdistance.put(system.getID(),  new StartLocation(i, dist));
					
					if( mindistance > dist ) {
						mindistance = dist;
						systemID = system.getID();
						orderLocationID = i;
					}
				}
			}
		}
		return new StartLocations(systemID, orderLocationID, minsysdistance);
	}
	
	private boolean register( String username, String email, int race, int system, String key, SQLResultRow settings ) {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		if( "".equals(username) || "".equals(email) ) {
			return false;
		}
		
		String uname = db.prepareString(username);
		SQLResultRow auser = db.first("SELECT * FROM users WHERE un='",uname,"'");
		SQLResultRow auser2 = db.first("SELECT * FROM users WHERE email='",db.prepareString(email),"'");

		if( !auser.isEmpty() ) {
			t.set_var("show.register.msg.wrongname",1);
			return false;
		}
		if( !auser2.isEmpty() ) {
			t.set_var("show.register.msg.wrongemail",1);
			return false;
		}
		if( !Rassen.get().rasse(race).isPlayable() ) {
			t.set_var("show.register.msg.wrongrace",1);
			return false;
		}
		
		boolean needkey = false;
		if( settings.getString("keys").indexOf('*') == -1 ) {
			needkey = true;
		}
		
		if( needkey && (settings.getString("keys").indexOf("<"+key+">") == -1) ) {
			t.set_var("show.register.msg.wrongkey", 1);
			return false;	
		}
		
		if( (system == 0) || (Systems.get().system(system) == null) || (Systems.get().system(system).getOrderLocations().length == 0) ) {
			t.set_block("_PORTAL", "register.systems.listitem", "register.systems.list");
			t.set_block("_PORTAL", "register.systemdesc.listitem", "register.systemdesc.list");
			
			StartLocations locations = getStartLocation();
			t.set_var(	"register.system.id", locations.systemID,
						"register.system.name", Systems.get().system(locations.systemID).getName(),
						"show.register.choosesystem", 1 );
		
			for( StarSystem sys : Systems.get() ) {
				if( sys.getOrderLocations().length > 0 ) {
					t.set_var(	"system.id", sys.getID(),
								"system.name", sys.getName(),
								"system.selected", (sys.getID() == locations.systemID),
								"system.description", Common._text(sys.getDescription()) );
										
					t.parse("register.systems.list", "register.systems.listitem", true);
					t.parse("register.systemdesc.list", "register.systemdesc.listitem", true);
				}
			}
			
			return true;
		}
		
		if( needkey ) {
	 		String[] keylist = settings.getString("keys").replace("\r\n", "\n").split("\n");
		 	HashMap<String,String> parameters = new HashMap<String,String>();
		 	int pos = 0;
		 	for( pos=0; pos < keylist.length; pos++ ) {
	 			if( keylist[pos].indexOf("<"+key+">") == 0 ) {
	 				if( keylist[pos].length() > ("<"+key+">").length() ) {
		 				String[] params = keylist[pos].substring(("<"+key+">").length()).split(",");
		 						
		 				for( String param : params ) {
		 					String[] aParam = param.split("="); 
		 					parameters.put(aParam[0], aParam[1]);
		 				}
	 				}
	 						
	 				break;	
	 			}	
	 		}
	 		
	 		if( parameters.containsKey("race") && (Integer.parseInt(parameters.get("race")) != race) ) {
	 			t.set_var("show.register.msg.wrongrace",1);
				return false;
	 		}
	 		String[] newKeyList = new String[keylist.length-1];
	 		if( pos != 0 ) {
	 			java.lang.System.arraycopy(keylist,0, newKeyList, 0, pos);
	 		}
	 		if( pos != keylist.length - 1 ) {
	 			java.lang.System.arraycopy(keylist,pos+1, newKeyList, pos, keylist.length-pos-1);
	 		}
	 		
	 		db.tBegin();
	 		db.tUpdate(1,"UPDATE config SET `keys`='",Common.implode("\n",newKeyList),"' WHERE `keys`='",settings.getString("keys"),"'");
	 	}
	 	else {
	 		db.tBegin();
	 	}
		
		String password = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE));
		String enc_pw = Common.md5(password);

		int maxid = db.first("SELECT max(id) maxid FROM users").getInt("maxid");
		int newid = maxid+1;

		int ticks = getContext().get(ContextCommon.class).getTick();

		String history = "Kolonistenlizenz erworben am "+Common.getIngameTime(ticks)+" ["+Common.date("d.m.Y H:i:s")+"]";		
		
		Cargo cargo = new Cargo();
		cargo.addResource( Resources.NAHRUNG, 100000 );
		
		db.tUpdate(1,"INSERT INTO users (id,un,passwort,race,signup,history,email,cargo,flags,nstat) " ,
				"VALUES " ,
				"('",newid,"','",username,"','",enc_pw,"','",race,"','",Common.time(),"','",history,"','",email,"','",cargo.save(),"','",User.FLAG_NOOB,"','0')");
		
		// Standard-Ordner erstellen
		db.tUpdate(1, "INSERT INTO ordner SET name='Papierkorb',playerid='",newid,"',flags='",Ordner.FLAG_TRASH,"',parent=0");

		db.tUpdate(1, "INSERT INTO user_f (id) VALUES ('",newid,"')");
		
		// Schiffe erstellen
	 	StartLocations locations = getStartLocation();
	 	Location[] orderlocs = Systems.get().system(system).getOrderLocations();
	 	Location orderloc = orderlocs[locations.minSysDistance.get(system).orderLocationID];
	 	
	 	String[] baselayout = Configuration.getSetting("REGISTER_BASELAYOUT").split(",");
	 	Integer[] activebuildings = new Integer[baselayout.length];
	 	
	 	int bewohner = 0;
	 	int arbeiter = 0;
	 	
	 	for( int i=0; i < baselayout.length; i++ ) {
	 		if( !baselayout[i].equals("0") ) {
	 			activebuildings[i] = 1;
	 			Building building = Building.getBuilding(db, Integer.parseInt(baselayout[i]));
	 			bewohner += building.getBewohner();
	 			arbeiter += building.getArbeiter();
	 		}
	 		else {
	 			activebuildings[i] = 0;
	 		}
	 	}

	 	SQLResultRow base = db.first("SELECT *,sqrt((",orderloc.getX(),"-x)*(",orderloc.getX(),"-x)+(",orderloc.getY(),"-y)*(",orderloc.getY(),"-y)) distance " ,
	 			"FROM bases " ,
	 			"WHERE klasse=1 AND owner=0 AND system='",system,"' ORDER BY distance LIMIT 1");
	 	
	 	try {
		 	Base baseobj = new Base(base);
		 	Integer[] bebauung = baseobj.getBebauung();
		 	for( int i=0; i < bebauung.length; i++ ) {
		 		if( bebauung[i] != 0 ) {
		 			Building.getBuilding(db, bebauung[i]).cleanup(getContext(), baseobj);
		 		}
		 	}
	 	}
	 	catch( RuntimeException e ) {
	 		e.printStackTrace();
	 		Common.mailThrowable(e, "Register Cleanup failed", "Base "+base.getInt("id"));
	 	}
	 	
	 	base = db.first("SELECT *,sqrt((",orderloc.getX(),"-x)*(",orderloc.getX(),"-x)+(",orderloc.getY(),"-y)*(",orderloc.getY(),"-y)) distance " ,
	 			"FROM bases " ,
	 			"WHERE id='",base.getInt("id"),"'");
	 	
	 	SQLResultRow newbase = (SQLResultRow)base.clone();
	 	newbase.put("e", base.get("maxe"));;
	 	newbase.put("owner", newid);
	 	newbase.put("bebauung", Common.implode("|",baselayout));
	 	newbase.put("active", Common.implode("|", activebuildings));
	 	newbase.put("arbeiter", arbeiter);
	 	newbase.put("bewohner", bewohner);
	 	newbase.put("cargo", new Cargo(Cargo.Type.STRING, Configuration.getSetting("REGISTER_BASECARGO")).save() );
	 	
	 	db.tUpdate(1, "UPDATE bases SET " ,
	 			"e='",newbase.get("e"),"', " ,
	 			"owner='",newbase.get("owner"),"'," ,
	 			"bebauung='",newbase.get("bebauung"),"'," ,
	 			"active='",newbase.get("active"),"'," ,
	 			"arbeiter='",newbase.get("arbeiter"),"'," ,
	 			"bewohner='",newbase.get("bewohner"),"'," ,
	 			"cargo='",newbase.get("cargo"),"'," ,
	 			"core=0," ,
	 			"coreactive=0," ,
	 			"autogtuacts='' " ,
	 			"WHERE " ,
	 			"id='",base.get("id"),"' AND " ,
	 			"owner='0' AND " ,
	 			"system='",system,"' AND " ,
	 			"e='",base.get("e"),"' AND " ,
	 			"bebauung='",base.get("bebauung"),"' AND " ,
	 			"active='",base.get("active"),"' AND " ,
	 			"arbeiter='",base.get("arbeiter"),"' AND " ,
	 			"bewohner='",base.get("bewohner"),"' AND " ,
	 			"cargo='",base.get("cargo"),"'");
	 	
	 	db.update("UPDATE offiziere SET userid='",newbase.get("owner"),"' WHERE dest IN ('b ",base.get("id"),"','t ",base.get("id"),"')");
	 	
	 	Base newBaseObj = new Base(newbase);
	 	for( int i=0; i < baselayout.length; i++ ) {
			if( Integer.parseInt(baselayout[i]) > 0 ) {
				Building building = Building.getBuilding(db, Integer.parseInt(baselayout[i]));
				building.build(newBaseObj);	 			
			}
		}
	 	
	 	SQLResultRow nebel = db.first("SELECT *,sqrt((",base.get("x"),"-x)*(",base.get("x"),"-x)+(",base.get("y"),"-y)*(",base.get("y"),"-y)) distance,sqrt((",base.get("x"),"-x)*(",base.get("x"),"-x)+(",base.get("y"),"-y)*(",base.get("y"),"-y))*(((type+1)%3)+1)*3 moddist FROM nebel WHERE system='",system,"' AND type<3 ORDER BY moddist LIMIT 1");
	 	
	 	if( race == 1 ) {		
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER", new Location(system, base.getInt("x"), base.getInt("y")), newid);
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER_TANKER", new Location(system, nebel.getInt("x"), nebel.getInt("y")), newid);
		} 
		else {			
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER", new Location(system, base.getInt("x"), base.getInt("y")), newid);
			SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER_TANKER", new Location(system, nebel.getInt("x"), nebel.getInt("y")), newid);
		}
	 	

		if( db.tCommit() ) {
			//Willkommens-PM versenden
			PM.send( getContext(),Configuration.getIntSetting("REGISTER_PM_SENDER"), newid, 
					"Willkommen bei Drifting Souls 2", Configuration.getSetting("REGISTER_PM"));
		
			t.set_var( "show.register.msg.ok", 1,
						"register.newid", newid );
							
			Common.copyFile(Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/user/0.gif",
					Configuration.getSetting("ABSOLUTE_PATH")+"data/logos/user/"+newid+".gif");

			String message = Configuration.getSetting("REGISTER_EMAIL");
			message = message.replace("{username}", username);
			message = message.replace("{password}", password);
			message = message.replace("{date}", Common.date("H:i j.m.Y"));
			
			Common.mail(email, "Anmeldung bei Drifting Souls 2", message);
		}
		else {
			if( retries  > 0 ) {
				retries--;
				return register( username, email, race, system, key, settings );
			}
			t.set_var("show.message", "Leider konnte die Registrierung nicht erfolgreich durchgef&uuml;hrt werden. Bitte versuchen sie es ein wenig sp&auml;ter erneut.<br />Die Administratoren wurden vorsorglich informiert und werden, falls ein Fehler vorliegt, diesen schnellstm&ouml;glich beheben");
			PM.sendToAdmins(getContext(), -1, "Registrierungsfehler", 
							"[color=orange]WARNUNG[/color]\nEs konnte auch nach 5 Versuchen keine erfolgreiche Registrierung eines Spielers durchgef&uuml;hrt werden.\n\nUsername: "+username+"\nEmail: "+email+"\nRace: "+race+"\nSystem: "+system+"\nKey: "+key+"\nSettings: "+settings.getString("keys"),PM.FLAGS_IMPORTANT);
			return false;
		}

		return true;
	}
	
	/**
	 * Registriert einen neuen Spieler. Falls keine Daten eingegeben wurden, 
	 * wird die GUI zum registrieren angezeigt
	 * @urlparam String username der Benutzername des Accounts
	 * @urlparam Integer race Die Rasse des Accounts
	 * @urlparam String email Die Email-Adresse
	 * @urlparam String key Der Registrierungssschluessel
	 * @urlparam Integer Das Startsystem
	 *
	 */
	public void registerAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		boolean showform = true;

		parameterString("username");
		parameterNumber("race");
		parameterString("email");
		parameterString("key");
		parameterNumber("system");
		
		String username = getString("username");
		int race = getInteger("race");
		String email = getString("email");
		String key = getString("key");
		int system = getInteger("system");
	
		SQLResultRow settings = db.first("SELECT disableregister,`keys` FROM config");
		if( !"".equals(settings.getString("disableregister")) ) {
			username = null;
			race = 0;
			email = null;
			showform = false;
		
			t.set_var(	"show.register.registerdisabled" , 1,
						"register.registerdisabled.msg" , Common._text(settings.getString("disableregister")) );
								
			return;
		}
		
		boolean needkey = false;
		if( settings.getString("keys").indexOf('*') == -1  ) {
			needkey = true;
		}		
		
		t.set_var(	"register.username"		, username,
					"register.email"		, email,
					"register.needkey"		, needkey,
					"register.key"			, key,
					"register.race"			, race,
					"register.system.id"	, system,
					"register.system.name" 	, (Systems.get().system(system) != null  ? Systems.get().system(system).getName() : "") );
		
		showform = !register(username, email, race, system, key, settings);
		
		if( showform ) {
			t.set_block("_PORTAL","register.rassen.listitem","register.rassen.list");
			t.set_block("_PORTAL","register.rassendesc.listitem","register.rassendesc.list");

			int first = -1;

			for( Rasse rasse : Rassen.get() ) {
				if( rasse.isPlayable() ) {
					t.set_var(	"rasse.id"			, rasse.getID(),
								"rasse.name"		, rasse.getName(),
								"rasse.selected"	, (first == -1 ? 1 : 0),
								"rasse.description"	, Common._text(rasse.getDescription()) );
					
					if( first == -1 ) {
						first = rasse.getID();
					}
					
					t.parse("register.rassen.list","register.rassen.listitem",true);
					t.parse("register.rassendesc.list","register.rassendesc.listitem",true);
				}
			}
			
			t.set_var(	"show.register"				, 1,
						"register.rassen.selected"	, first );
		}
	}
	
	/**
	 * Loggt einen Spieler ein. Falls keine Daten angegeben wurden, 
	 * wird die GUI zum einloggen angezeigt
	 * @urlparam String username Der Benutzername
	 * @urlparam String password Das Passwort
	 * @urlparam Integer usegfxpak != 0, falls ein vorhandenes Grafikpak benutzt werden soll
	 *
	 */
	public void loginAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("username");
		parameterString("password");
		parameterNumber("usegfxpak");
		
		String username = getString("username");
		String password = getString("password");
		int usegfxpak = getInteger("usegfxpak") != 0 ? 1 : 0;
		boolean clear = false;
		
		String disablelogin = db.first("SELECT disablelogin FROM config").getString("disablelogin");
		if( !"".equals(disablelogin) ) {
			username = "";
			password = "";
			clear = true;
		
			t.set_var(	"show.login.logindisabled", 1,
						"login.logindisabled.msg", Common._text(disablelogin) );
		}

		if( !"".equals(username) && !"".equals(password) ) {
			String enc_pw = Common.md5(password);
			username = db.prepareString(username);

			SQLResultRow uid = db.first("SELECT id FROM users WHERE un='",username,"'");
			if( uid.isEmpty() ) {
				t.set_var( "show.msg.login.wrongpassword",1 );
				Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+username+") <"+username+"> Password <"+password+"> ***UNGUELTIGER ACCOUNT*** von Browser <"+getRequest().getUserAgent()+">\n");
				clear = false;
			}
			else {
				User user = getContext().createUserObject(uid.getInt("id"));
				
	    		if( !user.getPassword().equals(enc_pw) ) {
					t.set_var( "show.msg.login.wrongpassword",1 );
					user.setLoginFailedCount(user.getLoginFailedCount()+1);
	  				Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+user.getID()+") <"+username+"> Password <"+password+"> ***LOGIN GESCHEITERT*** von Browser <"+getRequest().getUserAgent()+">\n");
					clear = false;
				} 
				else if( user.getDisabled() ) {
					t.set_var("show.login.msg.accdisabled",1);
					Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+user.getID()+") <"+username+"> Password <"+password+"> ***ACCOUNT GESPERRT*** von Browser <"+getRequest().getUserAgent()+">\n");
	
					db.update("DELETE FROM sessions WHERE id='",user.getID(),"'");
					clear = false;
				} 
				else {
					SQLResultRow session = db.first("SELECT * FROM sessions WHERE id='",user.getID(),"'");
					if( !session.isEmpty() && (session.getInt("tick") != 0) ) {
						t.set_var("show.login.msg.tick",1);
						clear = false;
					}
					else{
						Common.writeLog("login.log",Common.date( "j.m.Y H:i:s")+": <"+getRequest().getRemoteAddress()+"> ("+user.getID()+") <"+username+"> Login von Browser <"+getRequest().getUserAgent()+">\n");
	
	  					int id = user.getID();
	
	  					String sess = Common.md5(""+RandomUtils.nextInt(Integer.MAX_VALUE));
	
	  					db.update("DELETE FROM sessions WHERE id='",id,"' AND attach IS NULL");
	  					db.update("INSERT INTO sessions (id,session,ip,lastaction,usegfxpak) ",
									" VALUES('",id,"','",sess,"','<",getRequest().getRemoteAddress(),">','",Common.time(),"','",usegfxpak,"')");
	
						if( (user.getVacationCount() == 0) || (user.getWait4VacationCount() != 0) ) {
							t.set_var(	"show.login.msg.ok", 1,
										"login.sess", sess );
						}	
						else {
							t.set_var(	"show.login.vacmode", 1,
										"login.vacmode.dauer", Common.ticks2Days(user.getVacationCount()),
										"login.vacmode.sess", sess );
						}
						
						// Ueberpruefen ob das gfxpak noch aktuell ist
						if( (usegfxpak != 0) && !user.getUserImagePath().equals(User.getDefaultImagePath(db)) ) {
							t.set_var(	"login.checkgfxpak", 1,
										"login.checkgfxpak.path", user.getUserImagePath() );
						}
						
						/*
						 * HACK (? - das ganze sollte vieleicht besser ins Framework)
						 * 
						 * Browser erkennen und ggf eine Warnung ausgeben
						 * 
						 */
						
						String browser = getRequest().getUserAgent().toLowerCase();
						String browsername = null;
						if( browser.indexOf("opera") != -1 ) {
							browsername = "opera";
						}
						else if( browser.indexOf("msie") != -1 ) {
							browsername = "msie";
						}
						else if( (browser.indexOf("firefox") != -1) || (browser.indexOf("gecko") != -1) ) {
							browsername = "mozilla";
						}
						else {
							browsername = "unknown";	
						}
						
						try {
							if( browsername.equals("opera") ) {
								Matcher browserpattern = Pattern.compile("opera ([0-9\\.,]+)").matcher(browser);
								if( browserpattern.find() ) {
									String tmp = browserpattern.group(0);
									
									double version = Double.parseDouble(tmp);
									if( (version > 0) && (version < 9.0) ) {
										t.set_var(	"show.login.browserwarning", 1,
													"browser.name", "Opera",
													"browser.version", version );
									}
								}
							}
							else if( browsername.equals("msie") ) {
								Matcher browserpattern = Pattern.compile("msie ([0-9\\.,]+)").matcher(browser);
								browserpattern.find();
								String tmp = browserpattern.group(1);
	
								double version = Double.parseDouble(tmp);
								
								t.set_var(	"show.login.browserwarning", 1,
											"browser.name", "Microsoft Internet Explorer",
											"browser.version", version );
							}
						}
						catch( Exception e ) {
							java.lang.System.err.println(e);
							e.printStackTrace();
						}
	
	  					clear = true;
					}
				}
			}
		}

		if( !clear ) {
			t.set_var(	"show.login", 1,
						"login.username", username );
		}
	}
	
	/**
	 * Ermoeglicht das Absenden einer Anfrage zur Deaktivierung des Vac-Modus
	 * @urlparam String asess Die Session-ID
	 * @urlparam String reason Der Grund fuer eine vorzeitige Deaktivierung
	 *
	 */
	public void loginVacmodeDeakAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("asess");
		String sess = getString("asess");
	
		SQLResultRow sessdata = db.first("SELECT id,ip,lastaction,usegfxpak FROM sessions WHERE session='",sess,"'");

		if( sessdata.isEmpty() ) {
			t.set_var("show.login.vacmode.msg.accerror",1);
			return;
		}
		
		User auser = getContext().createUserObject(sessdata.getInt("id"));
		if( !auser.hasFlag(User.FLAG_DISABLE_IP_SESSIONS) && (sessdata.getString("ip").indexOf("<"+getRequest().getRemoteAddress()+">") > -1) ) {
			t.set_var("show.login.vacmode.msg.accerror",1);
			return;
		}
		
		if( !auser.hasFlag(User.FLAG_DISABLE_AUTO_LOGOUT) && (Common.time() - sessdata.getInt("lastaction") > Configuration.getIntSetting("AUTOLOGOUT_TIME")) ) {
			db.update("DELETE FROM sessions WHERE id='",sessdata.getInt("id"),"'");
			t.set_var("show.login.vacmode.msg.accerror",1);
			return;
		}
		
		parameterString("reason");
		String reason = getString("reason");
		
		PM.sendToAdmins(getContext(), sessdata.getInt("id"), "VACMODE-DEAK", "[VACMODE-DEAK]\nMY ID: "+sessdata.getInt("id")+"\nREASON:\n"+reason, 0);
		
		t.set_var("show.login.vacmode.msg.send",1);
	}
	
	/**
	 * Zeigt die News an
	 * @urlparam Integer archiv != 0, falls alte News angezeigt werden sollen
	 */
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("archiv");
		int archiv = getInteger("archiv");
		
		t.set_var( new Object[] {	"show.news",	1,
									"show.news.archiv", archiv } );
		t.set_block("_PORTAL","news.listitem","news.list");

		SQLQuery qhandle = db.query("SELECT * FROM portal_news ORDER BY date DESC LIMIT ",( archiv != 0 ? "5,100" : "5"));
		while( qhandle.next() ) {
			t.set_var(	"news.date", Common.date("d.m.Y H:i", qhandle.getInt("date")),
						"news.title", qhandle.getString("title"),
						"news.author", qhandle.getString("author"),
						"news.text", Common._text(qhandle.getString("txt")) );
			t.parse("news.list","news.listitem",true);
		}
		qhandle.free();
	}
}