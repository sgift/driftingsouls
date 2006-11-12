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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

class Academy extends DefaultBuilding {
	private static final Map<Integer,String> offis = new HashMap<Integer,String>();
	private static final Map<Integer,String> attributes = new HashMap<Integer,String>();
	
	static {
		offis.put(1, "Ingenieur");
		offis.put(2, "Navigator");
		offis.put(3, "Sicherheitsexperte");
		offis.put(4, "Captain");
		
		attributes.put(1, "Technik");
		attributes.put(2, "Waffen");
		attributes.put(3, "Navigation");
		attributes.put(4, "Sicherheit");
		attributes.put(5, "Kommandoeffizienz");
	}
	
	/**
	 * Erstellt eine neue Academy-Instanz
	 * @param row Die SQL-Ergebniszeile mit den Gebaeudedaten der Akademie
	 */
	public Academy(SQLResultRow row) {
		super(row);
	}

	@Override
	public void build(int col) {
		super.build(col);
		
		Context context = ContextMap.getContext();
		
		context.getDatabase().update("INSERT INTO academy VALUES(0,",col,",0,0,'')");
	}

	@Override
	public boolean classicDesign() {
		return true;
	}
	
	@Override
	public boolean printHeader() {
		return true;
	}

	@Override
	public void cleanup(Context context, int col) {
		super.cleanup(context, col);
		
		context.getDatabase().update("DELETE FROM academy WHERE col=",col);
		context.getDatabase().update("UPDATE offiziere SET dest='b ",col,"' WHERE dest='t ",col,"'");	
	}

	@Override
	public boolean isActive(int col, int status, int field) {
		Database db = ContextMap.getContext().getDatabase();

		SQLResultRow academy = db.first( "SELECT remain FROM academy WHERE col='",col,"'");
		if( academy.getInt("remain") > 0 ) {
			return true;
		}
		return false;
	}
	
	@Override
	public String echoShortcut(Context context, int col, int field, int building) {
		Database db = context.getDatabase();
		
		String sess = context.getSession();
		
		StringBuilder result = new StringBuilder(200);
		
		SQLResultRow acc = db.first("SELECT id,remain,train,`upgrade` FROM academy WHERE col=",col);
		if( !acc.isEmpty() ) {
			if( acc.getInt("remain") == 0 ) {
				result.append("<a class=\"back\" href=\"./main.php?module=building&amp;sess=");
				result.append(sess);
				result.append("&amp;col=");
				result.append(col);
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[A]</a>");
			} 
			else {	
				StringBuilder popup = new StringBuilder(200);
				popup.append(Common.tableBegin(300, "left").replace('"', '\''));
				if( acc.getInt("train") != 0 ) {
					popup.append("Bildet aus: ");
					popup.append(offis.get(acc.getInt("train")));
					popup.append("<br />");
				}
				else if( acc.getString("upgrade").length() != 0 ) {											 
					String[] upgrade = StringUtils.split(acc.getString("upgrade"), ' ');
					SQLResultRow offi = db.first("SELECT name FROM offiziere WHERE id='",upgrade[0],"'");
					
					popup.append("Bildet aus: ");
					popup.append(offi.getString("name"));
					popup.append(" (");
					popup.append(attributes.get(Integer.parseInt(upgrade[1])));
					popup.append(")<br />");
				}
				popup.append("Dauer: <img style='vertical-align:middle' src='");
				popup.append(Configuration.getSetting("URL"));
				popup.append("data/interface/time.gif' alt='noch ' />");
				popup.append(acc.getInt("remain"));
				popup.append("<br />");
				popup.append(Common.tableEnd().replace('"', '\''));
				
				String popupStr = StringEscapeUtils.escapeJavaScript(popup.toString());
				
				result.append("<a name=\"p");
				result.append(col);
				result.append("_");
				result.append(field);
				result.append("\" id=\"p");
				result.append(col);
				result.append("_");
				result.append(field);
				result.append("\" class=\"error\" onmouseover=\"return overlib('<span style=\\'font-size:13px\\'>");
				result.append(popupStr);
				result.append("</span>',REF,'p");
				result.append(col);
				result.append("_");
				result.append(field);
				result.append("',REFY,22,NOJUSTY,TIMEOUT,0,DELAY,150,WIDTH,300,BGCLASS,'gfxtooltip',FGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip');\" onmouseout=\"return nd();\" href=\"./main.php?module=building&amp;sess=");
				result.append(sess);
				result.append("&amp;col=");
				result.append(col);
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[A]<span style=\"font-weight:normal\">");
				result.append(acc.getInt("remain"));
				result.append("</span></a>");
			}
		}
		else {
			result.append("WARNUNG: Akademie ohne Akademieeintrag gefunden<br />\n");
		}
	
		return result.toString();
	}

	@Override
	public String output(Context context, TemplateEngine t, int col, int field, int building) {
		Database db = context.getDatabase();
		User user = context.getActiveUser();
		
		String sess = context.getSession();
		
		int newo = context.getRequest().getParameterInt("newo");
		int train = context.getRequest().getParameterInt("train");
		int off = context.getRequest().getParameterInt("off");
		String conf = context.getRequest().getParameterString("conf");
		StringBuilder echo = new StringBuilder(5000);
		
		echo.append("<div class=\"smallfont\">\n");
		
		SQLResultRow academy = db.first("SELECT id,train,remain,`upgrade` FROM academy WHERE col='",col,"'");
		if( academy.isEmpty() ) {
			echo.append("<span style=\"color:#ff0000; font-weight:bold\">Fehler: Diese Akademie verf&uuml;gt &uuml;ber keinen Akademie-Eintrag in der Datenbank</span><br />\n");
			return echo.toString();
		}
		
		//---------------------------------
		// Einen neue Offiziere ausbilden
		//---------------------------------
		
		if( newo != 0 ) {
			if( (academy.getInt("train") == 0) && (academy.getString("upgrade").length() == 0)) {
				Cargo cargo = new Cargo(Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id='",col,"'").getString("cargo"));
			
				boolean ok = true;
				if( cargo.getResourceCount( Resources.SILIZIUM ) < 25 ) {
					echo.append("<span style=\"color:#ff0000\">Nicht genug Silizium</span><br />\n");
					ok = false;
				}
				Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo() );
				if( cargo.getResourceCount( Resources.NAHRUNG )+usercargo.getResourceCount( Resources.NAHRUNG ) < 35 ) {
					echo.append("<span style=\"color:#ff0000\">Nicht genug Nahrung</span><br />\n");
					ok = false;
				}
		
				if( ok ) {
					echo.append("Training beginnt<br /><br /><br />\n");
		
					cargo.substractResource( Resources.SILIZIUM, 25 );
					usercargo.substractResource( Resources.NAHRUNG, 35 );
					if( usercargo.getResourceCount( Resources.NAHRUNG ) < 0 ) {
						cargo.substractResource( Resources.NAHRUNG, -usercargo.getResourceCount( Resources.NAHRUNG ) );
						usercargo.setResource( Resources.NAHRUNG, 0 );	
					}
		
					db.tBegin();
					user.setCargo(usercargo.save(), usercargo.save(true));
					db.tUpdate(1,"UPDATE academy SET train=",newo,",remain=8 WHERE col='",col,"' AND train='0' AND remain='0'");
					db.tUpdate(1,"UPDATE bases SET cargo='",cargo.save(),"' WHERE id='",col,"' AND cargo='",cargo.save(true),"'");
					if( !db.tCommit() ) {
						context.addError("Beginn der Ausbildung fehlgeschlagen. Bitte versuchen sie es erneut");
					}
					else {
						academy.put("train", newo);
						academy.put("remain", 8);
					}
		
				} 
				else {
					echo.append("<br /><br />");
				}
			}
		}
	
		//--------------------------------------
		// "Upgrade" eines Offiziers durchfuehren
		//--------------------------------------
		
		if( (train != 0) && (off != 0) ) {
			if( (academy.getInt("train") == 0) && (academy.getString("update").length() == 0) ) {
				SQLResultRow offizier = db.first("SELECT * FROM offiziere WHERE id='",off,"'");
				if( offizier.getString("dest").equals("b "+col) ) {
					echo.append(Common.tableBegin( 500, "left" ));
					
					echo.append("Trainiere "+offizier.getString("name")+":<br />\n");
					Map<Integer,String> dTrain = new HashMap<Integer,String>();
					dTrain.put(1, "ing");
					dTrain.put(2, "waf");
					dTrain.put(3, "nav");
					dTrain.put(4, "sec");
					dTrain.put(5, "com");
									 
					int sk = offizier.getInt(dTrain.get(train))+1;
					int nk = (int)(offizier.getInt(dTrain.get(train))*1.5d)+1;
					int dauer = (int)(offizier.getInt(dTrain.get(train))/4d)+1;
					
					if( train == 1 ) {
						echo.append("verbessere Technik");
					} 
					else if( train == 2 ) {
						echo.append("verbessere Waffen");
					} 
					else if( train == 3 ) {
						echo.append("verbessere Navigation");
					}
					else if( train == 4 ) {
						echo.append("verbessere Sicherheit");
					} 
					else if( train == 5 ) {
						echo.append("verbessere Kommandoeffizienz");
					}
					echo.append(" - Kosten: <img src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+dauer+" <img src=\""+Cargo.getResourceImage(Resources.SILIZIUM)+"\" alt=\"\" />"+sk+" <img src=\""+Cargo.getResourceImage(Resources.NAHRUNG)+"\" alt=\"\" />"+nk+"<br />\n");
					
					Cargo cargo = new Cargo(Cargo.Type.STRING, db.first("SELECT cargo FROM bases WHERE id='",col,"'").getString("cargo"));

					boolean ok = true;
					if( cargo.getResourceCount( Resources.SILIZIUM ) < sk) {
						echo.append("<span style=\"color:#ff0000\">Nicht genug Silizium</span><br />\n"); 
						ok = false;
					}
					Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo() );
					if( cargo.getResourceCount( Resources.NAHRUNG )+usercargo.getResourceCount( Resources.NAHRUNG ) < nk ) {
						echo.append("<span style=\"color:#ff0000\">Nicht genug Nahrung</span><br />\n"); 
						ok = false;
					}
		
					if( !conf.equals("ok") ) {
						echo.append("<br /><div style=\"text-align:center\"><a class=\"back\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;train="+train+"&amp;off="+off+"&amp;conf=ok\">Training durchf&uuml;hren</a></div>\n");
						echo.append(Common.tableEnd());
						echo.append("<br />\n");
						
						return echo.toString();
					}
		
					if( ok ) {
						echo.append("<br />Training beginnt<br />\n");
		
						cargo.substractResource( Resources.SILIZIUM, sk );
						usercargo.substractResource( Resources.NAHRUNG, nk );
						if( usercargo.getResourceCount( Resources.NAHRUNG ) < 0 ) {
							cargo.substractResource( Resources.NAHRUNG, -usercargo.getResourceCount( Resources.NAHRUNG ) );
							usercargo.setResource( Resources.NAHRUNG, 0 );	
						}
		
						db.tBegin();
						user.setCargo( usercargo.save(), usercargo.save(true) );
						db.tUpdate(1,"UPDATE academy SET `upgrade`='",off," ",train,"',remain='",dauer,"' WHERE col='",col,"' AND remain='0' AND `upgrade`=''");
						db.tUpdate(1,"UPDATE offiziere SET dest='t ",col,"' WHERE id='",off,"' AND dest='b ",col,"'");
						db.tUpdate(1,"UPDATE bases SET cargo='",cargo.save(),"' WHERE id='",col,"' AND cargo='",cargo.save(true),"'");
						if( !db.tCommit() ) {
							context.addError("Beginn der Ausbildung fehlgeschlagen. Bitte versuchen sie es erneut");
						}
						else {					
							academy.put("upgrade", off+" "+train);
							academy.put("remain", dauer);
						}					
						echo.append(Common.tableEnd());
						echo.append("<br />\n");
						
						return echo.toString();
					}
				}
			}
		}
		boolean allowActions = true;
		
		//-----------------------------------------------
		// werden gerade Offiziere ausgebildet? Welche?
		//-----------------------------------------------
		
		if( (academy.getInt("train") != 0) || (academy.getString("upgrade").length() != 0) ) {
			echo.append(Common.tableBegin(400, "left"));
			echo.append("<span style=\"font-weight:bold\">Bildet aus:</span>\n");
		
			if( academy.getInt("train") != 0 ) {
				echo.append(Common._plaintitle(Offiziere.LIST.get(academy.getInt("train")).getString("name"))+"<br />");
			}
			else {
				String[] upgradeData = StringUtils.split(academy.getString("upgrade"), ' ' );
				Offizier offizier = Offizier.getOffizierByID(Integer.parseInt(upgradeData[0]));
				echo.append("<img style=\"vertical-align:middle\" src=\""+offizier.getPicture()+"\" alt=\"\" />\n");
				echo.append("<a class=\"academy\" href=\"./main.php?module=choff&amp;sess="+sess+"&amp;off="+offizier.getID()+"\">"+Common._plaintitle(offizier.getName())+"</a><br />");
			}
		
			echo.append("<span style=\"font-weight:bold\">Dauer:</span> noch <img align=\"middle\" src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"\" />"+academy.getInt("remain")+" Runden<br />\n");
		
			echo.append("</span>");
			echo.append(Common.tableEnd());
			echo.append("<br />\n");
			
			allowActions = false;
		}
		
		//---------------------------------
		// Liste: Neue Offiziere ausbilden
		//---------------------------------
		if( allowActions ) {
			echo.append(Common.tableBegin(550, "left"));
		
			echo.append("<span style=\"font-weight:bold\">Neuen Offizier ausbilden:</span><br />\n");
			echo.append("Kosten: <img src=\""+Configuration.getSetting("URL")+"data/interface/time.gif\" alt=\"\" />8 <img src=\""+Cargo.getResourceImage(Resources.SILIZIUM)+"\" alt=\"\" />25 <img src=\""+Cargo.getResourceImage(Resources.NAHRUNG)+"\" alt=\"\" />35<br /><br />");
			echo.append("<table class=\"noBorderX\" border=\"1\">\n");
			echo.append("<tr>");
			echo.append("<td class=\"noBorderX\">Name</td><td class=\"noBorderX\">Technik</td><td class=\"noBorderX\">Waffen</td><td class=\"noBorderX\">Navigation</td><td class=\"noBorderX\">Sicherheit</td><td class=\"noBorderX\">Kommandoeffizienz</td></tr>\n");
			for( SQLResultRow offi : Offiziere.LIST.values() ) {
				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\"><a class=\"academy\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;newo="+offi.getInt("id")+"\">"+Common._title(offi.getString("name"))+"</a></td>\n");
				echo.append("<td class=\"noBorderX\" align=\"center\">"+offi.getInt("ing")+"</td>\n");
				echo.append("<td class=\"noBorderX\" align=\"center\">"+offi.getInt("waf")+"</td>\n");
				echo.append("<td class=\"noBorderX\" align=\"center\">"+offi.getInt("nav")+"</td>\n");
				echo.append("<td class=\"noBorderX\" align=\"center\">"+offi.getInt("sec")+"</td>\n");
				echo.append("<td class=\"noBorderX\" align=\"center\">"+offi.getInt("com")+"</td>\n");
				echo.append("</tr>\n");
			}
			echo.append("</table><br />\n");
			echo.append(Common.tableEnd());
			echo.append("<br />\n");
		}
		
		//---------------------------------
		// Liste: "Upgrade" von Offizieren
		//---------------------------------
		
		boolean firstEntry = true;
		
		SQLQuery offizier = db.query("SELECT * FROM offiziere WHERE dest='b ",col,"'");
		while( offizier.next() ) {
			if( firstEntry ) {
				echo.append(Common.tableBegin(550, "left"));
				echo.append("<span style=\"font-weight:bold\">Anwesende Offiziere:</span><br /><br />\n");
				echo.append("<table class=\"noBorderX\" border=\"1\">\n");
				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\">Name</td>\n");
				echo.append("<td class=\"noBorderX\">Technik</td>\n");
				echo.append("<td class=\"noBorderX\">Waffen</td>\n");
				echo.append("<td class=\"noBorderX\">Navigation</td>\n");
				echo.append("<td class=\"noBorderX\">Sicherheit</td>\n");
				echo.append("<td class=\"noBorderX\">Kommandoeffizienz</td>\n");
				echo.append("<td class=\"noBorderX\">Spezial</td>\n");
				echo.append("</tr>\n");

				firstEntry = false;
			}
			Offizier offi = new Offizier( offizier.getRow() );
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" style=\"white-space:nowrap\">\n");
			echo.append("<img style=\"vertical-align:middle\" src=\""+offi.getPicture()+"\" alt=\"\" />\n");
			echo.append("<a class=\"academy\" href=\"./main.php?module=choff&amp;sess="+sess+"&amp;off="+offi.getID()+"\">"+Common._plaintitle(offi.getName())+"</a>\n");
			echo.append("</td>");
			
			echo.append("<td class=\"noBorderX\" align=\"center\">\n");
			if( allowActions ) {
				echo.append("<a class=\"academy\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;train=1&amp;off="+offi.getID()+"\">"+offi.getAbility(Offizier.Ability.ING)+"</a>\n");
			}
			else {
				echo.append(offi.getAbility(Offizier.Ability.ING));
			}
			echo.append("</td>");
			
			echo.append("<td class=\"noBorderX\" align=\"center\">\n");
			if( allowActions ) {
				echo.append("<a class=\"academy\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;train=2&amp;off="+offi.getID()+"\">"+offi.getAbility(Offizier.Ability.WAF)+"</a>\n");
			}
			else {
				echo.append(offi.getAbility(Offizier.Ability.WAF));
			}
			echo.append("</td>");
			
			echo.append("<td class=\"noBorderX\" align=\"center\">\n");
			if( allowActions ) {
				echo.append("<a class=\"academy\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;train=3&amp;off="+offi.getID()+"\">"+offi.getAbility(Offizier.Ability.NAV)+"</a>\n");
			}
			else {
				echo.append(offi.getAbility(Offizier.Ability.NAV));
			}
			echo.append("</td>");
			
			echo.append("<td class=\"noBorderX\" align=\"center\">\n");
			if( allowActions ) {
				echo.append("<a class=\"academy\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;train=4&amp;off="+offi.getID()+"\">"+offi.getAbility(Offizier.Ability.SEC)+"</a>\n");
			}
			else {
				echo.append(offi.getAbility(Offizier.Ability.SEC));
			}
			echo.append("</td>");
			
			echo.append("<td class=\"noBorderX\" align=\"center\">\n");
			if( allowActions ) {
				echo.append("<a class=\"academy\" href=\"./main.php?module=building&amp;sess="+sess+"&amp;col="+col+"&amp;field="+field+"&amp;train=5&amp;off="+offi.getID()+"\">"+offi.getAbility(Offizier.Ability.COM)+"</a>\n");
			}
			else {
				echo.append(offi.getAbility(Offizier.Ability.COM));
			}
			echo.append("</td>");
			
			echo.append("<td class=\"noBorderX\">"+offi.getSpecial()+"</td>\n");
			echo.append("</tr>\n");
		}
		if( !firstEntry ) {
			echo.append("</table><br />\n");
			echo.append(Common.tableEnd());
			echo.append("<br />\n");
		}
		offizier.free();
		
		echo.append("<br /></div>");
		
		return echo.toString();
	}
}
