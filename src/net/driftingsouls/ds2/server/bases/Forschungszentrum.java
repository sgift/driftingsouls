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
package net.driftingsouls.ds2.server.bases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Forschung;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringEscapeUtils;

class Forschungszentrum extends DefaultBuilding {
	/**
	 * Erstellt eine neue Forschungszentrum-Instanz
	 * @param row Die SQL-Ergebniszeile mit den Gebaeudedaten des Forschungszentrums
	 */
	public Forschungszentrum(SQLResultRow row) {
		super(row);
	}

	@Override
	public void build(int col) {
		super.build(col);
		
		Context context = ContextMap.getContext();
		if( context == null ) {
			throw new RuntimeException("No Context available");
		}
		
		context.getDatabase().update("INSERT INTO fz VALUES(0,",col,",1,0,0)");
	}

	@Override
	public void cleanup(Context context, int col) {
		super.cleanup(context, col);
		
		context.getDatabase().update("DELETE FROM fz WHERE col=",col);
	}
	
	@Override
	public boolean classicDesign() {
		return true;
	}

	@Override
	public boolean printHeader() {
		return false;
	}

	@Override
	public boolean isActive(int col, int status, int field) {
		Database db = ContextMap.getContext().getDatabase();

		SQLResultRow forschungszentrum = db.first( "SELECT dauer FROM fz WHERE col='",col,"'");
		if( !forschungszentrum.isEmpty() && (forschungszentrum.getInt("dauer") > 0) ) {
			return true;
		}
		else if( forschungszentrum.isEmpty() ) {
			LOG.warn("Forschungszentrum ohne fz-Eintrag auf Basis "+col+" gefunden");
		}
		return false;
	}

	@Override
	public String echoShortcut(Context context, int col, int field, int building) {
		Database db = context.getDatabase();
		
		String sess = context.getSession();
		
		StringBuilder result = new StringBuilder(100);
		SQLResultRow fz = db.first("SELECT id,dauer,forschung FROM fz WHERE col=",col);
		if( !fz.isEmpty() ) {
			if( fz.getInt("dauer") == 0 ) {
				result.append("<a class=\"back\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"\">[F]</a>");
			}
			else {
				StringBuilder popup = new StringBuilder(Common.tableBegin( 350, "left" ).replace("\"", "'") );
				SQLResultRow forschung = db.first("SELECT name FROM forschungen WHERE id="+fz.getInt("forschung"));
				popup.append("<img align='left' border='0' src='"+Configuration.getSetting("URL")+"data/tech/"+fz.getInt("forschung")+".gif' alt='' />");
				popup.append(forschung.getString("name")+"<br />");
				popup.append("Dauer: noch <img src='"+Configuration.getSetting("URL")+"data/interface/time.gif' alt='noch ' />"+fz.getInt("dauer")+"<br />");
				popup.append( Common.tableEnd().replace("\"", "'") );

				result.append("<a name=\"p"+col+"_"+field+"\" id=\"p"+col+"_"+field+"\" class=\"error\" onmouseover=\"return overlib('<span style=\\'font-size:13px\\'>"+StringEscapeUtils.escapeJavaScript(popup.toString())+"</span>',REF,'p"+col+"_"+field+"',REFY,22,NOJUSTY,TIMEOUT,0,DELAY,150,WIDTH,280,BGCLASS,'gfxtooltip',FGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip');\" onmouseout=\"return nd();\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"\">[F]<span style=\"font-weight:normal\">"+fz.getInt("dauer")+"</span></a>");
			}
		}
		else {
			result.append("WARNUNG: Forschungszentrum ohne Forschungszentrumeintrag gefunden<br />\n");
		}
		return result.toString();
	}
	
	private void possibleResearch(Context context, StringBuilder echo, int col, int field) {
		Database db = context.getDatabase();
		String sess = context.getSession();
		
		User user = context.getActiveUser();
	
		echo.append("M&ouml;gliche Forschungen: <br />\n");
		echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\">Name</td>\n");
		echo.append("<td class=\"noBorderX\">Kosten</td>\n");
		echo.append("</tr>\n");
	
		Cargo cargo = new Cargo( Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id=",col).getString("cargo"));
	
		List<Integer> researches = new ArrayList<Integer>();
		SQLQuery research = db.query("SELECT t1.forschung FROM fz AS t1, bases AS t2 WHERE t1.forschung>0 AND t1.col=t2.id AND t2.owner=",user.getID());
		while( research.next() ) {
			researches.add(research.getInt("forschung"));
		}
		research.free();
		
		boolean first = true;
	
		Map<Integer,Forschung> map = Forschung.getSpecial("", "name");
		for( Forschung tech : map.values() ) {
			if( !Rassen.get().rasse(user.getRace()).isMemberIn(tech.getRace()) ) {
				continue;	
			}
			if( researches.contains(tech.getID()) ) {
				continue;
			}
			if( user.hasResearched(tech.getID())  ) {
				continue;
			}
			
			boolean ok = true;
			
			for( int k = 1; k <= 3; k++ ) {
				if( (tech.getRequiredResearch(k) != 0) && !user.hasResearched(tech.getRequiredResearch(k)) ) {
					ok = false;
				}
			}
			
			if( ok ) {
				if( !first ) {
					echo.append("<tr><td colspan=\"2\" class=\"noBorderX\"><hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" /></td></tr>\n");
				}
				else {
					first = false;
				}
				
				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\" style=\"width:60%\">\n");
				if( !user.isNoob() || !tech.hasFlag(Forschung.FLAG_DROP_NOOB_PROTECTION) ) {
					echo.append("<a class=\"forschinfo\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;res="+tech.getID()+"\">"+Common._plaintitle(tech.getName())+"</a>\n");
				}
				else {
					echo.append("<a class=\"forschinfo\" href=\"javascript:ask('Achtung!\\nWenn Sie diese Technologie erforschen verlieren sie den GCP-Schutz. Dies bedeutet, dass Sie sowohl angreifen als auch angegriffen werden k&ouml;nnen','./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;res="+tech.getID()+"')\">"+Common._plaintitle(tech.getName())+"</a>\n");
				}
				echo.append("<a class=\"forschinfo\" href=\"./main.php?module=forschinfo&amp;sess="+sess+"&amp;res="+tech.getID()+"\"><img style=\"border:0px;vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/forschung/info.gif\" alt=\"?\" /></a>\n");
				echo.append("&nbsp;&nbsp;");
				echo.append("</td>\n");
				
				echo.append("<td class=\"noBorderX\">");
				echo.append("<img style=\"vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+tech.getTime()+" ");
				
				Cargo costs = new Cargo( Cargo.Type.STRING, tech.getCosts() );
				costs.setOption( Cargo.Option.SHOWMASS, false );
				
				ResourceList reslist = costs.compare( cargo, false );
				for( ResourceEntry res : reslist ) {
					if( res.getDiff() > 0 ) {
						echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" /><span style=\"color:red\">"+res.getCargo1()+"</span> ");
					}
					else {
						echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+" ");
					}
				}
	
				echo.append("</td></tr>\n");
			}
		}
	
		echo.append("</table><br />\n");
	}
	
	private void alreadyResearched( Context context, StringBuilder echo ) {
		Database db = context.getDatabase();
		String sess = context.getSession();
		
		User user = context.getActiveUser();
		
		echo.append("<table class=\"noBorderX\">");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Bereits erforscht:</td></tr>\n");
	
		for( int i = 1; i < 100; i++ ) {
			if( user.hasResearched(i) ) {
				SQLResultRow tech = db.first("SELECT name FROM forschungen WHERE id=",i);
				if( tech.isEmpty() ) {
					continue;
				}
				echo.append("<tr><td class=\"noBorderX\">\n");
				echo.append("<a class=\"forschinfo\" href=\"./main.php?module=forschinfo&amp;sess="+sess+"&amp;res="+i+"\">"+Common._plaintitle(tech.getString("name"))+"</a>");
				echo.append("</td></tr>\n");
			}
		}
		echo.append("</table><br />");
	}
	
	private boolean currentResearch(Context context, StringBuilder echo, int col, int field ) {
		Database db = context.getDatabase();
		String sess = context.getSession();
		
		SQLResultRow fz = db.first("SELECT t1.forschung,t1.dauer,t2.name FROM fz AS t1,forschungen AS t2 WHERE t1.col=",col," AND t1.forschung=t2.id");
	
		if( fz.getInt("forschung")!=0 ) {
			echo.append("<img style=\"float:left;border:0px\" src=\""+Configuration.getSetting("URL")+"data/tech/"+fz.getInt("forschung")+".gif\" alt=\"\" />");
			echo.append("Erforscht: <a class=\"forschinfo\" href=\"./main.php?module=forschinfo&amp;sess="+sess+"&amp;res="+fz.getInt("forschung")+"\">"+Common._plaintitle(fz.getString("name"))+"</a>\n");
			echo.append("[<a class=\"error\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;kill=yes\">x</a>]<br />\n");
			echo.append("Dauer: noch <img style=\"vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"\" />"+fz.getInt("dauer")+" Runden\n");
			echo.append("<br /><br />\n");
			return true;
		} 
		return false;
	}
	
	private void killResearch(Context context, StringBuilder echo, int col, int field, String conf) {
		Database db = context.getDatabase();
		String sess = context.getSession();
		
		if( !conf.equals("ok") ) {
			echo.append("<div style=\"text-align:center\">\n");
			echo.append("Wollen sie die Forschung wirklich abbrechen?<br />\n");
			echo.append("Achtung: Es erfolgt keine R&uuml;ckerstattung der Resourcen!<br /><br />\n");
			echo.append("<a class=\"error\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;kill=yes&amp;conf=ok\">Forschung abbrechen</a><br />\n");
			echo.append("</div>\n");
			return;
		}
		db.update("UPDATE fz SET forschung=0,dauer=0 WHERE col=",col);
		echo.append("<div style=\"text-align:center;color:red;font-weight:bold\">\n");
		echo.append("Forschung abgebrochen<br />\n");
		echo.append("</div>");
	}
	
	private void doResearch(Context context, StringBuilder echo, int researchid, int col, int field, String conf) {
		Database db = context.getDatabase();
		String sess = context.getSession();
		User user = context.getActiveUser();
	
		SQLResultRow fz = db.first("SELECT forschung,dauer FROM fz WHERE col=",col);
	
		Forschung tech = Forschung.getInstance(researchid);
		boolean ok = true;
	
		if( !Rassen.get().rasse(user.getRace()).isMemberIn(tech.getRace()) ) {
			echo.append("<a class=\"error\" href=\"./main.php?module=base&amp;sess="+sess+"&amp;col="+col+"\">Fehler: Diese Forschung kann von ihrer Rasse nicht erforscht werden</a>\n");
			return;
		}
		
		Cargo techCosts = new Cargo( Cargo.Type.STRING, tech.getCosts() );
		techCosts.setOption( Cargo.Option.SHOWMASS, false );
	
		// Muss der User die Forschung noch best?tigen?
		if( !conf.equals("ok") ) {
			echo.append("<div style=\"text-align:center\">\n");
			echo.append(Common._plaintitle(tech.getName())+"<br /><img style=\"vertical-align:middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+tech.getTime()+" ");
			
			ResourceList reslist = techCosts.getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+" ");
			}
			
			echo.append("<br /><br />\n");
			echo.append("<a class=\"ok\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;res="+researchid+"&amp;conf=ok\">Erforschen</a></span><br />\n");
			echo.append("</div>\n");
			
			return;
		}
	
		// Wird bereits im Forschungszentrum geforscht?
		if( fz.getInt("forschung") != 0 ) {
			ok = false;
		}
	
		// Besitzt der Spieler alle fuer die Forschung n?tigen Forschungen?
		for( int i=1; i <= 3; i++ ) {
			if( !user.hasResearched(tech.getRequiredResearch(i)) ) {
				ok = false;
				break;
			}
		}
	
		if( !ok ) {
			echo.append("<a class=\"error\" href=\"./main.php?module=base&amp;sess="+sess+"&amp;col="+col+"\">Fehler: Forschung kann nicht durchgef&uuml;hrt werden</a>\n");
			return;
		}
	
		// Alles bis hierhin ok -> nun zu den Resourcen!
		Cargo cargo = new Cargo( Cargo.Type.STRING, db.first( "SELECT cargo FROM bases WHERE id="+col).getString("cargo"));
		ok = true;
		
		ResourceList reslist = techCosts.compare( cargo, false );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				echo.append("<span style=\"color:red\">Nicht genug <img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getName()+"</span><br />\n");
				ok = false;
			}
		}
	
		// Alles OK -> Forschung starten!!!
		if( ok ) {
			cargo.substractCargo( techCosts );
			echo.append("<div style=\"text-align:center;color:green\">\n");
			echo.append(Common._plaintitle(tech.getName())+" wird erforscht<br />\n");
			echo.append("</div>\n");
			
			db.tBegin();
			db.tUpdate(1,"UPDATE fz SET forschung=",researchid,",dauer=",tech.getTime()," WHERE col=",col," AND forschung=0 AND dauer=0");
			db.tUpdate(1,"UPDATE bases SET cargo='",cargo.save(),"' WHERE id='",col,"' AND cargo='",cargo.save(true),"'");
			if( !db.tCommit() ) {
				context.addError("Beim Starten der Forschung ist ein Fehler aufgetreten. Bitte versuchen sie es sp&auml;ter erneut");
			}
		}
	}

	@Override
	public String output(Context context, TemplateEngine t, int col, int field, int building) {
		Database db = context.getDatabase();

		String sess = context.getSession();
		
		int research 	= context.getRequest().getParameterInt("res");
		String confirm 	= context.getRequest().getParameterString("conf");
		String kill 		= context.getRequest().getParameterString("kill");
		String show 		= context.getRequest().getParameterString("show");
		if( !show.equals("oldres") ) {
			show = "newres";
		}
		
		StringBuilder echo = new StringBuilder(2000);
		
		SQLResultRow base = db.first("SELECT * FROM bases WHERE id='",col,"'");
		
		SQLResultRow fz = db.first("SELECT id FROM fz WHERE col=",col);
		if( fz.isEmpty() ) {
			echo.append("<span style=\"color:red\">Fehler: Dieses Forschungszentrum hat keinen Datenbank-Eintrag</span>\n");
			return echo.toString();
		}
		
		echo.append("<table class=\"show\" cellspacing=\"2\" cellpadding=\"2\">\n");
		echo.append("<tr><td class=\"noBorderS\" style=\"text-align:center;font-size:12px\">Forschungszentrum<br />"+base.getString("name")+"</td><td class=\"noBorder\">&nbsp;</td>\n");
		
		//Neue Forschung & Bereits erforscht
		echo.append("<td class=\"noBorderS\">\n");
	
		echo.append(Common.tableBegin( 440, "center" ));
		
		echo.append("<a class=\"forschinfo\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;show=newres\">Neue Forschung</a>&nbsp;\n");
		echo.append("&nbsp;|&nbsp;&nbsp;<a class=\"forschinfo\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;show=oldres\">Bereits erforscht</a>\n");
		
		echo.append(Common.tableEnd());

		echo.append("</td>\n");
		echo.append("</tr>\n");
		echo.append("<tr>\n");
		echo.append("<td colspan=\"3\" class=\"noBorderS\">\n");
		echo.append("<br />\n");
		
		echo.append(Common.tableBegin( 570, "left" ));
		
		if( (kill.length() != 0) || (research != 0) ) {
			if( kill.length() != 0 ) {
				killResearch( context, echo, col, field, confirm);
			}
			if( research != 0 ) {
				doResearch( context, echo, research, col, field, confirm );
			}
		}
		else if( show.equals("newres") ) {
			if( !currentResearch( context, echo, col, field ) ) {
				possibleResearch( context, echo, col, field );
			}
		} 
		else {
			alreadyResearched( context, echo );
		}
		
		echo.append(Common.tableEnd());

		echo.append("<br />\n");
		echo.append("</td>\n");
		echo.append("</tr>\n");
	
		echo.append("</table>");
		return echo.toString();
	}
}
