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

import java.io.File;
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Aendern der Einstellungen eines Benutzers durch den Benutzer selbst.
 * @author Christopher Jung
 *
 */
@Configurable
public class OptionsController extends TemplateGenerator {
	private static final Log log = LogFactory.getLog(OptionsController.class);
	
	private Configuration config;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public OptionsController(Context context) {
		super(context);
		
		setTemplate("options.html");	
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
		return true;
	}
	
	/**
	 * Aendert den Namen und das Passwort des Benutzers.
	 * @urlparam String name Der neue Benutzername
	 * @urlparam String pw Das neue Passwort
	 * @urlparam String pw2 Die Wiederholung des neuen Passworts
	 */
	@Action(ActionType.DEFAULT)
	public void changeNamePassAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
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
			if( user.getAlly() != null ) {
				String allytag = user.getAlly().getAllyTag();
				newname = allytag;
				newname = StringUtils.replace(newname, "[name]", name);
			}
			
			changemsg += "<span style=\"color:green\">Der Ingame-Namen <span style=\"color:white\">"+Common._title(user.getNickname())+"</span> wurde in <span style=\"color:white\">"+Common._title(name)+"</span> ge&auml;ndert</span><br />\n";
			
			Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getContext().getRequest().getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> Namensaenderung: Ingame-Namen <"+user.getNickname()+"> in <"+name+"> Browser <"+getContext().getRequest().getUserAgent()+">\n");
		
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
			String message = Common.trimLines(config.get("PWCHANGE_EMAIL"));
			message = StringUtils.replace(message, "{username}", user.getUN());
			message = StringUtils.replace(message, "{date}", Common.date("H:i j.m.Y"));
			
			Common.mail (user.getEmail(), subject, message);

			Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+getContext().getRequest().getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> Passwortaenderung Browser <"+getContext().getRequest().getUserAgent()+"> \n");
		}
		else if( pw.length() != 0 ) {
			changemsg += "<span style=\"color:red\">Die beiden eingegebenen Passw&ouml;rter stimmen nicht &uuml;berein</span><br />\n";
		}

		t.setVar(	"options.changenamepwd",			1,
					"options.changenamepwd.nickname",	Common._plaintitle(user.getNickname()),
					"options.message", 					changemsg );
	}
	
	/**
	 * Sendet die Löschanfrage des Spielers.
	 * @urlparam Integer del Der Interaktionsschritt. Bei 0 wird das Eingabeformular angezeigt. Andernfalls wird versucht die Anfrage zu senden
	 * @urlparam String reason Die schluessige Begruendung. Muss mindestens die Laenge 5 haben
	 */
	@Action(ActionType.DEFAULT)
	public void delAccountAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("del");
		parameterString("reason");
		
		int del = getInteger("del");
		String reason = getString("reason");
		if( del == 0 ) {
			t.setVar( "options.delaccountform", 1 );
		
			return;
		}
		else if( reason.length() < 5 ) {
			t.setVar(	"options.message",			"Bitte geben sie Gr&uuml;nde f&uuml;r die L&ouml;schung an!<br />\n",
						"options.delaccountform",	1 );
		
			return;
		}
		else {
			StringBuilder msg = new StringBuilder(100);
	 		msg.append("PLZ DELETE ME!!!\nMY ID IS: [userprofile=");
	 		msg.append(user.getId());
	 		msg.append("]");
	 		msg.append(user.getId());
	 		msg.append("[/userprofile]\n");
	 		msg.append("MY UN IS: ");
	 		msg.append(user.getUN());
	 		msg.append("\n");
	 		msg.append("MY CURRENT NAME IS: ");
	 		msg.append(user.getName());
	 		msg.append("\n");
	 		msg.append("MY REASONS:\n");
	 		msg.append(reason);
	 		PM.sendToAdmins(user, "Account l&ouml;schen", msg.toString(), 0);
	 		
			t.setVar(	"options.delaccountresp",		1,
						"delaccountresp.admins",		config.get("ADMIN_PMS_ACCOUNT") );
			
			return;
		}
	}
	
	/**
	 * Aendert die erweiterten Einstellungen des Spielers.
	 * @urlparam Integer shipgroupmulti der neue Schiffsgruppierungswert
	 * @urlparam Integer inttutorial Die Seite des Tutorials in der Uebersicht (0 = deaktiviert)
	 * @urlparam Integer scriptdebug Ist fuer den Spieler die Option zum Debugging von (ScriptParser-)Scripten sichtbar gewesen? (es wird hinterher noch auf das AccessLevel gecheckt. Daher kein "Einbruch" moeglich.
	 * @urlparam Integer scriptdebugstatus Bei einem Wert != 0 wird das ScriptDebugging aktiviert (benoetigt AccessLevel >= 20)
	 * @urlparam Integer mapwidth Die Breite der Sternenkarte
	 * @urlparam Integer mapheight Die Hoehe der Sternenkarte
	 * @urlparam Integer defrelation Die Default-Beziehung zu anderen Spielern (1 = feindlich, 2 = freundlich, sonst neutral)  
	 */
	@Action(ActionType.DEFAULT)
	public void changeXtraAction() {
		User user = (User)getUser();
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
			if( user.getAlly() != null ) {
				for( User auser : user.getAlly().getMembers() ) {
					if( auser.getId() == user.getId() ) {
						continue;
					}
					user.setRelation(auser.getId(), User.Relation.FRIEND);
					auser.setRelation(user.getId(), User.Relation.FRIEND);
				}
			}
		}
		
		t.setVar( "options.message", changemsg );
		
		redirect("xtra");
	}
	
	/**
	 * Zeigt die erweiterten Einstellungen des Spielers.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void xtraAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();

		t.setVar(	"options.xtra",			1,
					"user.wrapfactor",		user.getUserValue("TBLORDER/schiff/wrapfactor"),
					"user.inttutorial",		user.getUserValue("TBLORDER/uebersicht/inttutorial"),
					"user.admin",			user.getAccessLevel() >= 20,
					"user.mapwidth",		user.getUserValue("TBLORDER/map/width"),
					"user.mapheight",		user.getUserValue("TBLORDER/map/height"),
					"user.scriptdebug",		user.hasFlag(User.FLAG_SCRIPT_DEBUGGING),
					"user.defrelation",		user.getRelation(0).ordinal() );
	}
	
	private static final int MAX_UPLOAD_SIZE = 307200;
	
	/**
	 * Aendert das Logo des Spielers.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void logoAction() {
		TemplateEngine t = getTemplateEngine();
		
		List<FileItem> list = getContext().getRequest().getUploadedFiles();
		if( list.size() == 0 ) {
			redirect();
			return;
		}
		
		if( list.get(0).getSize() > MAX_UPLOAD_SIZE ) {
			t.setVar("options.message","Das Logo ist leider zu gro&szlig;. Bitte w&auml;hle eine Datei mit maximal 300kB Gr&ouml;&stlig;e<br />");
			redirect();
			return;
		}
		
		String uploaddir = config.get("ABSOLUTE_PATH")+"data/logos/user/";
		try {
			File uploadedFile = new File(uploaddir+getUser().getId()+".gif");
			list.get(0).write(uploadedFile);
			t.setVar("options.message","Das neue Logo wurde auf dem Server gespeichert<br />");
		}
		catch( Exception e ) {
			t.setVar("options.message","Offenbar ging beim Upload etwas schief (Ist die Datei evt. zu gro&szlig;?)<br />");
			log.warn(e);
		}
		redirect();
	}
	
	/**
	 * Setzt die Grafikpak-Einstellungen fuer den Spieler.
	 * @urlparam String gfxpak Der neue GfxPak-Pfad. Wenn dieser leer ist, wird der Default-Pfad verwendet
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void gfxPakAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("gfxpak");
		
		String gfxpak = getString("gfxpak");

		if( gfxpak.length() == 0 ) {
			user.setImagePath(BasicUser.getDefaultImagePath());
			
			t.setVar( "options.message", "Pfad zum Grafikpak zur&uuml;ckgesetzt<br />\n" );
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
			
			t.setVar( "options.message", "Pfad zum Grafikpak ge&auml;ndert<br />\n" );
		}
		
		redirect();
	}
	
	/**
	 * Aktiviert den Vac-Mode fuer den Spieler.
	 * @urlparam Integer vacmode die ID des zu benutzenden Vacmodes
	 */
	@Action(ActionType.DEFAULT)
	public void vacModeAction() 
	{
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		parameterNumber("vacdays");
		int vacDays = getInteger("vacdays");
		int vacTicks = Common.daysToTicks(vacDays);
		
		if(!user.checkVacationRequest(vacTicks))
		{
			t.setVar("options.message", "Dein Urlaubskonto reicht nicht aus f&uuml;r soviele Tage Urlaub.");
			redirect();
			return;
		}
		
		user.activateVacation(vacTicks);
		t.setVar("options.message", "Der Vorlauf f&uuml;r deinen Urlaub wurde gestartet.");
		redirect();
	}
	
	/**
	 * Speichert die neuen Optionen.
	 * @urlparam Integer enableipsess Falls != 0 wird die Koppelung der Session an die IP aktiviert
	 * @urlparam Integer enableautologout Falls != 0 wird der automatische Logout aktiviert
	 * @urlparam Integer showtooltip Falls != 0 werden die Hilfstooltips aktiviert
	 * @urlparam Integer wrapfactor Der neue Schiffsgruppierungsfaktor (0 = keine Gruppierung)
	 */
	@Action(ActionType.DEFAULT)
	public void saveOptionsAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		Session db = getDB();
		
		parameterNumber("enableipsess");
		parameterNumber("enableautologout");
		parameterNumber("showtooltip");
		parameterNumber("wrapfactor");
		
		boolean enableipsess = getInteger("enableipsess") != 0 ? true : false;
		boolean enableautologout = getInteger("enableautologout") != 0 ? true : false;
		boolean showtooltip = getInteger("showtooltip") != 0 ? true : false;
		int wrapfactor = getInteger("wrapfactor");
	
		String changemsg = "";

		if( enableipsess == user.hasFlag( BasicUser.FLAG_DISABLE_IP_SESSIONS ) ) {
			user.setFlag( BasicUser.FLAG_DISABLE_IP_SESSIONS, !enableipsess );
			
			changemsg += "Session-ID von der IP-Adresse "+(enableipsess ? "ge" : "ent" )+"koppelt<br />\n";
		} 
	
		if( enableautologout == user.hasFlag( BasicUser.FLAG_DISABLE_AUTO_LOGOUT ) ) {
			user.setFlag( BasicUser.FLAG_DISABLE_AUTO_LOGOUT, !enableautologout );
			db.createQuery("delete from PermanentSession where userId=:userId")
			  .setParameter("userId", user.getId())
			  .executeUpdate();
			
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
	
		t.setVar( "options.message", changemsg );
	
		redirect();
	}
	
	/**
	 * Deaktiviert den Noob-Schutz des Spielers.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void dropNoobProtectionAction() {
		User user = (User)getUser();
		TemplateEngine t = getTemplateEngine();
		
		if( user.isNoob() ) {
			user.setFlag( User.FLAG_NOOB, false );
			t.setVar("options.message", "GCP-Schutz wurde vorzeitig aufgehoben.<br />");
		}
		
		redirect();
	}

	/**
	 * Uebersicht ueber die Einstellungen.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		
		String imagepath = user.getUserImagePath();
			
		if( imagepath.equals(BasicUser.getDefaultImagePath()) ) {
			imagepath = "";
		}

		t.setVar(	"options.general",	1,
					"user.wrapfactor",	user.getUserValue("TBLORDER/schiff/wrapfactor"),
					"user.tooltip",		user.getUserValue("TBLORDER/schiff/tooltips"),
					"user.ipsess",		!user.hasFlag( BasicUser.FLAG_DISABLE_IP_SESSIONS ),
					"user.autologout",	!user.hasFlag( BasicUser.FLAG_DISABLE_AUTO_LOGOUT ),
					"user.imgpath",		imagepath,
					"user.noob",		user.isNoob(),
					"vacation.maxtime", Common.ticks2DaysInDays(user.maxVacTicks()));
	}
}
