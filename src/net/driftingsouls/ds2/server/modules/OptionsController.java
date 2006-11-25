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

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Aendern der Einstellungen eines Benutzers durch den Benutzer selbst
 * @author Christopher Jung
 *
 */
public class OptionsController extends DSGenerator {
	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public OptionsController(Context context) {
		super(context);
		
		setTemplate("options.html");	
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	/**
	 * Aendert den Namen und das Passwort des Benutzers
	 *
	 */
	public void changeNamePassAction() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		parameterString("name");
		parameterString("pw");
		parameterString("pw2");

		String changemsg = "";

		String name = getString("name");
		if( (name.length() != 0) && !name.equals(user.getNickname()) ) {
			boolean addhistory = false;
			
			BBCodeParser bbcodeparser = BBCodeParser.getInstance();
			if( !bbcodeparser.parse(user.getNickname(),new String[]{"all"}).trim().equals(bbcodeparser.parse(name,new String[]{"all"}).trim()) ) {
				addhistory = true;
			}
			
			String newname = name;
			if( user.getAlly() != 0 ) {
				String allytag = db.first("SELECT allytag FROM ally WHERE id=",user.getAlly()).getString("allytag");
				newname = allytag;
				newname = StringUtils.replace(newname, "[name]", name);
			}
			
			changemsg += "<span style=\"color:green\">Der Ingame-Namen <span style=\"color:white\">"+Common._title(user.getNickname())+"</span> wurde in <span style=\"color:white\">"+Common._title(name)+"</span> ge&auml;ndert</span><br />\n";
			
			Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getContext().getRequest().getRemoteAddress()+"> ("+user.getID()+") <"+user.getUN()+"> Namensaenderung: Ingame-Namen <"+user.getNickname()+"> in <"+name+"> Browser <"+getContext().getRequest().getUserAgent()+">\n");
		
			if( addhistory ) {
				user.addHistory(Common.getIngameTime(getContext().get(ContextCommon.class).getTick())+": Umbenennung in "+newname);
			}
			
			user.setName(newname);
			user.setNickname(name);
		}

		String pw = getString("pw");
		String pw2 = getString("pw2");
		if( (pw.length() != 0) && pw.equals(pw2) ) {
			String enc_pw = Common.md5(pw);
			
			user.setPassword(enc_pw);
			changemsg += "<span style=\"color:red\">Das Password wurde ge&auml;ndert</span><br />\n";

			String subject = "Drifting Souls - Passwortaenderung";
			String message = Common.trimLines(Configuration.getSetting("PWCHANGE_EMAIL"));
			message = StringUtils.replace(message, "{username}", user.getUN());
			message = StringUtils.replace(message, "{date}", Common.date("H:i j.m.Y"));
			
			Common.mail (user.getEmail(), subject, message);

			Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getContext().getRequest().getRemoteAddress()+"> ("+user.getID()+") <"+user.getUN()+"> Passwortaenderung Browser <"+getContext().getRequest().getUserAgent()+"> \n");
		}
		else if( pw.length() != 0 ) {
			changemsg += "<span style=\"color:red\">Die beiden eingegebenen Passw&ouml;rter stimmen nicht &uuml;berein</span><br />\n";
		}

		t.set_var(	"options.changenamepwd",			1,
					"options.changenamepwd.nickname",	Common._plaintitle(user.getNickname()),
					"options.message", 					changemsg );
	}
	
	/**
	 * Sendet die Löschanfrage des Spielers
	 *
	 */
	public void delAccountAction() {
		// TODO
		Common.stub();
	}
	
	/**
	 * Aendert die erweiterten Einstellungen des Spielers
	 * 
	 */
	public void changeXtraAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("shipgroupmulti");
		parameterNumber("inttutorial");
		parameterNumber("scriptdebug");
		parameterNumber("mapwidth");
		parameterNumber("mapheight");
		parameterNumber("defrelation");
		
		int shipgroupmulti = getInteger("shipgroupmulti");
		int inttutorial = getInteger("inttutorial");
		int scriptdebug = getInteger("scriptdebug");
		int mapwidth = getInteger("mapwidth");
		int mapheight = getInteger("mapheight");
		int defrelation = getInteger("defrelation");
		
		User.Relation rel = User.Relation.NEUTRAL;
		switch( defrelation ) {
		case 1: 
			rel = User.Relation.ENEMY;
			break;
		case 2:
			rel = User.Relation.FRIEND;
			break;
		}
		
		String changemsg = "";

		if( shipgroupmulti != Integer.parseInt(user.getUserValue("TBLORDER/schiff/wrapfactor")) ) {
			changemsg += "Neuer Schiffsgruppenmultiplikator gespeichert...<br />\n";
			
			user.setUserValue( "TBLORDER/schiff/wrapfactor", Integer.toString(shipgroupmulti) );
		}
		
		if( (scriptdebug != 0) && (user.getAccessLevel() >= 20) ) {
			parameterNumber("scriptdebugstatus");
			
			boolean scriptdebugstatus = getInteger("scriptdebugstatus") != 0 ? true : false;
			
			if( scriptdebugstatus != user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {	
				user.setFlag( User.FLAG_SCRIPT_DEBUGGING, scriptdebugstatus  );		
				
				changemsg += "Scriptdebugging "+(scriptdebugstatus ? "" : "de")+"aktiviert<br />\n";
			}
		}
		
		if( inttutorial != Integer.parseInt(user.getUserValue("TBLORDER/uebersicht/inttutorial")) ) {
			if( inttutorial != 0 ) {
				changemsg += "Tutorial aktiviert...<br />\n";
			}
			else {
				changemsg += "Tutorial deaktiviert...<br />\n";
			}
			user.setUserValue("TBLORDER/uebersicht/inttutorial", Integer.toString(inttutorial) );
		}
		
		if( mapwidth != Integer.parseInt(user.getUserValue("TBLORDER/map/width")) ) {
			changemsg += "Kartenbreite ge&auml;ndert...<br />\n";

			user.setUserValue("TBLORDER/map/width", Integer.toString(mapwidth) );
		}
		
		if( mapheight != Integer.parseInt(user.getUserValue("TBLORDER/map/height")) ) {
			changemsg += "Kartenh&ouml;he ge&auml;ndert...<br />\n";

			user.setUserValue("TBLORDER/map/height", Integer.toString(mapheight));
		}
		
		if( rel != user.getRelation(0) ) {
			changemsg += "Diplomatiehaltung ge&auml;ndert...<br />\n";

			user.setRelation(0,rel);
		}
		
		t.set_var( "options.message", changemsg );
		
		redirect("xtra");
	}
	
	/**
	 * Zeigt die erweiterten Einstellungen des Spielers
	 *
	 */
	public void xtraAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();

		t.set_var(	"options.xtra",			1,
					"user.wrapfactor",		user.getUserValue("TBLORDER/schiff/wrapfactor"),
					"user.inttutorial",		user.getUserValue("TBLORDER/uebersicht/inttutorial"),
					"user.admin",			user.getAccessLevel() >= 20,
					"user.mapwidth",		user.getUserValue("TBLORDER/map/width"),
					"user.mapheight",		user.getUserValue("TBLORDER/map/height"),
					"user.scriptdebug",		user.hasFlag( User.FLAG_SCRIPT_DEBUGGING),
					"user.defrelation",		user.getRelation(0) );
	}
	
	/**
	 * Aendert das Logo des Spielers
	 *
	 */
	public void logoAction() {
		// TODO
		Common.stub();
	}
	
	/**
	 * Setzt die Grafikpak-Einstellungen fuer den Spieler
	 *
	 */
	public void gfxPakAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("gfxpak");
		
		String gfxpak = getString("gfxpak");

		if( gfxpak.length() == 0 ) {
			user.setImagePath(User.getDefaultImagePath(getDatabase()));
			
			t.set_var( "options.message", "Pfad zum Grafikpak zur&uuml;ckgesetzt<br />\n" );
		} 
		else {
			if( (gfxpak.indexOf('/') == -1) && (gfxpak.indexOf('\\') != -1) ) {
				gfxpak = StringUtils.replace(gfxpak, "\\", "/");
			} 
			if( !gfxpak.endsWith("/") ){
				gfxpak += '/';
			}
			if( gfxpak.indexOf("://") == -1 ) {
				if( !gfxpak.startsWith("/") ) {
					gfxpak = '/'+gfxpak;
				}
				gfxpak = "file://"+gfxpak;
			}
			user.setImagePath( gfxpak );
			
			t.set_var( "options.message", "Pfad zum Grafikpak ge&auml;ndert<br />\n" );
		}
		
		redirect();
	}
	
	/**
	 * Aktiviert den Vac-Mode fuer den Spieler
	 *
	 */
	public void vacModeAction() {
		// TODO
		Common.stub();
	}
	
	/**
	 * Speichert die neuen Optionen
	 *
	 */
	public void saveOptionsAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("enableipsess");
		parameterNumber("enableautologout");
		parameterNumber("showtooltip");
		parameterNumber("wrapfactor");
		
		boolean enableipsess = getInteger("enableipsess") != 0 ? true : false;
		boolean enableautologout = getInteger("enableautologout") != 0 ? true : false;
		boolean showtooltip = getInteger("showtooltip") != 0 ? true : false;
		int wrapfactor = getInteger("wrapfactor");
	
		String changemsg = "";

		if( enableipsess == user.hasFlag( User.FLAG_DISABLE_IP_SESSIONS ) ) {
			user.setFlag( User.FLAG_DISABLE_IP_SESSIONS, !enableipsess );
			
			changemsg += "Session-ID von der IP-Adresse "+(enableipsess ? "ge" : "ent" )+"koppelt<br />\n";
		} 
	
		if( enableautologout == user.hasFlag( User.FLAG_DISABLE_AUTO_LOGOUT ) ) {
			user.setFlag( User.FLAG_DISABLE_AUTO_LOGOUT, !enableautologout );
		 
			changemsg += "Das automatische Ausloggen wurde "+(enableautologout ? "" : "de" )+"aktiviert<br />\n";
		} 

		if( showtooltip != (Integer.parseInt(user.getUserValue("TBLORDER/schiff/tooltips")) != 0) ) {
			user.setUserValue( "TBLORDER/schiff/tooltips", showtooltip ? "1" : "0" );
			
			changemsg += "Anzeige der Tooltips "+(showtooltip ? "" : "de")+"aktiviert<br />\n";		
		} 
	
		if( wrapfactor != Integer.parseInt(user.getUserValue("TBLORDER/schiff/wrapfactor")) ) {
			user.setUserValue("TBLORDER/schiff/wrapfactor", Integer.toString(wrapfactor) );
			
			changemsg += "Schiffsgruppierungen "+(wrapfactor != 0 ? "aktiviert" : "deaktiviert")+"<br />\n";
		}
	
		t.set_var( "options.message", changemsg );
	
		redirect();
	}
	
	/**
	 * Deaktiviert den Noob-Schutz des Spielers
	 *
	 */
	public void dropNoobProtectionAction() {
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		if( user.isNoob() ) {
			user.setFlag( User.FLAG_NOOB, false );
			t.set_var("options.message", "GCP-Schutz wurde vorzeitig aufgehoben.<br />");
		}
		
		redirect();
	}

	/**
	 * Uebersicht ueber die Einstellungen
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		
		String imagepath = user.getUserImagePath();
			
		if( imagepath.equals(User.getDefaultImagePath(getDatabase())) ) {
			imagepath = "";
		}

		t.set_var(	"options.general",	1,
					"user.wrapfactor",	user.getUserValue("TBLORDER/schiff/wrapfactor"),
					"user.tooltip",		user.getUserValue("TBLORDER/schiff/tooltips"),
					"user.ipsess",		!user.hasFlag( User.FLAG_DISABLE_IP_SESSIONS ),
					"user.autologout",	!user.hasFlag( User.FLAG_DISABLE_AUTO_LOGOUT ),
					"user.imgpath",		imagepath,
					"user.noob",		user.isNoob() );


		t.set_block("_OPTIONS","options.general.vac.listitem","options.general.vac.list");
		// TODO
		Common.stub();
		/*for( $__vacmodes as $k=>$vacmode ) {
			$vacdauer = ticks2Days( $vacmode['dauer'])."; Vorlauf: ".$vacmode['vacwait']." Ticks";
	
			$t->set_var( array( 'options.general.vac.dauer' => $vacdauer,
								'options.general.vac.index'	=> $k ) );
	
			$t->parse('options.general.vac.list','options.general.vac.listitem',true);
		}*/
	}
}
