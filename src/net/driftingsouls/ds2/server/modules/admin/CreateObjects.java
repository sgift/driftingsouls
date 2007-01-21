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
package net.driftingsouls.ds2.server.modules.admin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.Systems;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;

import org.apache.commons.lang.math.RandomUtils;

/**
 * Ermoeglicht das Absetzen von Admin-Kommandos
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Objekte", name="hinzuf&uuml;gen")
public class CreateObjects implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		String objekt = context.getRequest().getParameterString("objekt");
		String pngpath = context.getRequest().getParameterString("pngpath");
		int anzahl = context.getRequest().getParameterInt("anzahl");
		int klasse = context.getRequest().getParameterInt("klasse");
		int width = context.getRequest().getParameterInt("width");
		int height = context.getRequest().getParameterInt("height");
		int type = context.getRequest().getParameterInt("type");
		int system = context.getRequest().getParameterInt("system");
		int minX = context.getRequest().getParameterInt("minX");
		int minY = context.getRequest().getParameterInt("minY");
		int maxX = context.getRequest().getParameterInt("maxX");
		int maxY = context.getRequest().getParameterInt("maxY");
		int systemout = context.getRequest().getParameterInt("systemout");
		String systemname = context.getRequest().getParameterString("systemname");
		
		echo.append("<script type=\"text/javascript\">\n");
		echo.append("<!--\n");
		echo.append("function Go(x) {\n");
	   	echo.append("self.location.href = \"./main.php?module=admin&sess="+context.getSession()+"&page="+page+"&act="+action+"&objekt=\"+x;\n");
		echo.append("}\n");
		echo.append("//-->\n");
		echo.append("</script>\n");
		echo.append(Common.tableBegin( 300, "left" ));
		echo.append("<form action=\"./main.php\" method=\"post\">\n");
		echo.append("<table class=\"noBorderX\" border=\"1\">\n");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\">Objekt</td>\n");
		echo.append("<td class=\"noBorderX\">\n");
		echo.append("<select name=\"objekt\" onChange=\"Go(this.form.objekt.options[this.form.objekt.options.selectedIndex].value)\">\n");
		if( objekt.equals("Base") ) {
			echo.append("<option selected=\"selected\" value=\"Base\">Base</option>\n");
		}
		else {
			echo.append("<option value=\"Base\">Base</option>\n");
		}

		if( objekt.equals("Nebel") ) {
			echo.append("<option selected=\"selected\" value=\"Nebel\">Nebel</option>\n");
		}
		else {
			echo.append("<option value=\"Nebel\">Nebel</option>\n");
		}

		if( objekt.equals("Jumpnode") ) {
			echo.append("<option selected=\"selected\" value=\"Jumpnode\">Jumpnode</option>\n");
		}
		else {
			echo.append("<option value=\"Jumpnode\">Jumpnode</option>\n");
		}
		
		if( objekt.equals("png") ) {
			echo.append("<option selected=\"selected\" value=\"png\">Aus png erstellen</option>\n");
		}
		else {
			echo.append("<option value=\"png\">Aus png erstellen</option>\n");
		}
		
		echo.append("</select></td></tr>");

		if( objekt.equals("png") ) {
			echo.append("<tr><td class=\"noBorderX\">Pfad</td>\n");
			echo.append("<td class=\"noBorderX\"><input name=\"pngpath\" type=\"text\" size=\"50\" value=\""+pngpath+"\" /></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Infos</td>\n");
			echo.append("<td class=\"noBorderX\">#000000 - Freier Raum<br />#FF0000 -  Nebel (Deut)<br />#0000FF - Asteroid (Typ 1; 5x8=40)<br />#0000AF - Asteroid (Typ 3; 6x10=60)<br />#00006F - Asteroid (Typ 4; 5x4=20)</td></tr>\n");
		}

		if( !objekt.equals("Jumpnode") && !objekt.equals("png") ) {
			echo.append("<tr><td class=\"noBorderX\">Anzahl</td>\n");
			echo.append("<td class=\"noBorderX\"><input name=\"anzahl\" type=\"text\" size=\"18\" value=\""+anzahl+"\" /></td></tr>\n");
		}

		// Klasse & Groesse
		if( !objekt.equals("Jumpnode") && !objekt.equals("png") ) {
			if( !objekt.equals("Nebel") ) {
				echo.append("<tr><td class=\"noBorderX\">Klasse</td>\n");
				echo.append("<td class=\"noBorderX\"><input name=\"klasse\" type=\"text\" size=\"18\" value =\""+klasse+"\" /></td></tr>\n");
				echo.append("<tr><td class=\"noBorderX\">Breite</td>\n");
				echo.append("<td class=\"noBorderX\"><input name=\"width\" type=\"text\" size=\"18\" value =\""+width+"\" /></td></tr>\n");
				echo.append("<tr><td class=\"noBorderX\">H&ouml;he</td>\n");
				echo.append("<td class=\"noBorderX\"><input name=\"height\" type=\"text\" size=\"18\" value =\""+height+"\" /></td></tr>\n");
			}
			echo.append("<tr><td class=\"noBorderX\">Typ</td>\n");
			echo.append("<td class=\"noBorderX\"><input name=\"type\" type=\"text\" size=\"18\" value =\""+type+"\" /></td></tr>\n");
		}
		// Klasse & Groesse

		echo.append("<tr><td class=\"noBorderX\">System</td>");
		echo.append("<td class=\"noBorderX\"><input name=\"system\" type=\"text\" size=\"18\" value=\""+system+"\" /></td></tr>\n");

		if( !objekt.equals("png") ) {
			if( !objekt.equals("Jumpnode") ) {
				echo.append("<tr><td class=\"noBorderX\">Min X</td>\n");
			}
			else {
				echo.append("<tr><td class=\"noBorderX\">X</td>\n");
			}
			echo.append("<td class=\"noBorderX\"><input name=\"minX\" type=\"text\" size=\"18\" value =\""+minX+"\" /></td></tr>\n");

			if( !objekt.equals("Jumpnode") ) {
				echo.append("<tr><td class=\"noBorderX\">Min Y</td>\n");
			}
			else {
				echo.append("<tr><td class=\"noBorderX\">Y</td>\n");
			}
			echo.append("<td class=\"noBorderX\"><input name=\"minY\" type=\"text\" size=\"18\" value =\""+minY+"\" /></td></tr>\n");

			if( !objekt.equals("Jumpnode") ) {
				echo.append("<tr><td class=\"noBorderX\">Max X</td>\n");
			}
			else {
				echo.append("<tr><td class=\"noBorderX\">Austritts-X</td>\n");
			}
			echo.append("<td class=\"noBorderX\"><input name=\"maxX\" type=\"text\" size=\"18\" value =\""+maxX+"\" /></td></tr>\n");

			if( !objekt.equals("Jumpnode") ) {
				echo.append("<tr><td class=\"noBorderX\">Max Y</td>\n");
			}
			else {
				echo.append("<tr><td class=\"noBorderX\">Austritts-Y</td>\n");
			}
			echo.append("<td class=\"noBorderX\"><input name=\"maxY\" type=\"text\" size=\"18\" value=\""+maxY+"\" /></td></tr>\n");

			if( objekt.equals("Jumpnode") ) {
				echo.append("<tr><td class=\"noBorderX\">Austritts-System</td>");
				echo.append("<td class=\"noBorderX\"><input name=\"systemout\" type=\"text\" size=\"18\" value=\""+systemout+"\" /></td></tr>\n");
				echo.append("<tr><td class=\"noBorderX\">Austritts-Name</td>");
				echo.append("<td class=\"noBorderX\"><input name=\"systemname\" type=\"text\" size=\"18\" value=\""+systemname+"\" /></td></tr>\n");
			}
		}
		echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\"><input type=\"submit\" value=\".: create\" />&nbsp");
		echo.append("<input type=\"reset\" value=\".: reset\" /></td></tr>");
		echo.append("</table>\n");
		echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("</form>");
		echo.append("</font>");
		
		echo.append(Common.tableEnd());
		
		if( system != 0 ) {
			Database db = context.getDatabase();
			
			echo.append("Bearbeite System: "+system+"<br />\n");
			// Asteroid setzen
			if( objekt.equals("Base") ) {
				int maxid = db.first("SELECT max(id) maxid FROM bases").getInt("maxid");

				int sid = maxid+1;

				for( int i=1; i <= anzahl; i++ ) {
					int x = RandomUtils.nextInt(maxX-minX+1)+minX;
					int y =RandomUtils.nextInt(maxY-minY+1)+minY;

					db.update( "INSERT INTO bases " +
							"(id,x,y,system,klasse,height,width) " +
							"VALUES " +
							"("+sid+","+x+","+y+","+system+","+klasse+","+height+","+width+")" );

					echo.append("Erstelle Kolonie: "+sid+"<br />\n");
					sid++;
				}
			}
			// Nebel setzen
			if( objekt.equals("Nebel") ) {
				for( int i=1; i <= anzahl; i++ ) {
					int x = RandomUtils.nextInt(maxX-minX+1)+minX;
					int y =RandomUtils.nextInt(maxY-minY+1)+minY;

					createNebula(db, system, x, y, type);
				}
			}

			// Jumpnode setzen
			if( objekt.equals("Jumpnode") ) {
				echo.append("Erstelle Jumpnode...<br />\n");

				db.update("INSERT INTO jumpnodes " +
						"(x,y,system,xout,yout,systemout,name) " +
						"VALUES " +
						"("+minX+","+minY+","+system+","+maxX+","+maxY+","+systemout+",'"+systemname+"')");
			}
			
			/*
				Aus png erstellen
			*/
			if( objekt.equals("png") ) {
				File png = new File(pngpath);
				if( !png.isFile() ) {
					echo.append("File not found: "+pngpath+"<br />\n");
					return;
				}
				
				try {
					BufferedImage image = ImageIO.read(new FileInputStream(png));

					for( int x=0; x < Systems.get().system(system).getWidth(); x++ ) {
						for( int y=0; y < Systems.get().system(system).getHeight(); y++ ) {
							int colorhex = image.getRGB(x, y);

							switch(colorhex) {
							case 0x000000:
								continue;
								
							// Deut-Nebel Normal
							case 0xFF0000:
								createNebula(db, system, x+1, y+1, 0);
								break;
								
							// Deut-Nebel Schwach
							case 0xCB0000:
								createNebula(db, system, x+1, y+1, 1);
								break;
								
							// Deut-Nebel Stark
							case 0xFF00AE:
								createNebula(db, system, x+1, y+1, 2);
								break;
								
							// EMP-Nebel Schwach
							case 0x3B9400:
								createNebula(db, system, x+1, y+1, 3);
								break;
								
							// EMP-Nebel Mittel
							case 0x4FC500:
								createNebula(db, system, x+1, y+1, 4);
								break;
								
							// EMP-Nebel Stark
							case 0x66FF00:
								createNebula(db, system, x+1, y+1, 5);
								break;
								
							// Schadensnebel
							case 0xFFBA00:
								createNebula(db, system, x+1, y+1, 6);
								break;
								
							// Normaler Asteroid
							case 0x0000FF:
								createPlanet(db, system, x+1, y+1, 1);
								break;
								
							// Grosser Asteroid
							case 0x0000AF:
								createPlanet(db, system, x+1, y+1, 3);
								break;
								
							// Kleiner Asteroid
							case 0x00006F:
								createPlanet(db, system, x+1, y+1, 4);
								break;
								
							// Sehr kleiner Asteroid
							case 0x40406F:
								createPlanet(db, system, x+1, y+1, 5);
								break;
								
							// Sehr grosser Asteroid
							case 0x4040AF:
								createPlanet(db, system, x+1, y+1, 2);
								break;
								
							default:
								echo.append("Unknown color: #"+Integer.toHexString(colorhex)+"<br />");
							}
						}
					}
				}
				catch( IOException e ) {
					echo.append("Kann PNG "+pngpath+" nicht oeffnen: "+e);
				}
			} // if objekt == "png"
			
		} // if System
	}

	private void createNebula( Database db, int system, int x, int y, int type ) {
		SQLResultRow nebel = db.first("SELECT * FROM nebel WHERE x="+x+" AND y="+y+" AND system="+system);
		if( !nebel.isEmpty() ) {
			db.update("DELETE FROM nebel WHERE id="+nebel.getInt("id"));
		}
		db.update("INSERT INTO nebel (x,y,system,type) " +
				"VALUES " +
				"("+x+","+y+","+system+","+type+")" );
		
		StringBuffer echo = ContextMap.getContext().getResponse().getContent();
		echo.append("Erstelle Nebel bei "+new Location(system,x,y)+"<br />\n");
	}
	
	private void createPlanet( Database db, int system, int x, int y, int klasse ) {
		StringBuffer echo = ContextMap.getContext().getResponse().getContent();
		
		int height = 0;
		int width = 0;
		long cargo = 0;
		switch( klasse ) {
		case 1:
			height = 8;
			width = 5;
			cargo = 100000;
			break;
			
		case 2:
			height = 10;
			width = 10;
			cargo = 180000;
			break;
		
		case 3:
			height = 10;
			width = 6;
			cargo = 150000;
			break;
			
		case 4:
			height = 4;
			width = 5;
			cargo = 70000;
			break;
			
		case 5:
			height = 3;
			width = 5;
			cargo = 50000;
			break;
			
		default:
			echo.append("Ungueltiger Asti-Typ "+klasse+"<br />");
			return;
		}
		
		db.update( "INSERT INTO bases " +
				"(x,y,system,klasse,height,width,maxcargo,maxtiles) " +
				"VALUES " +
				"("+x+","+y+","+system+","+klasse+","+height+","+width+","+cargo+","+(width*height)+")" );
					
		echo.append("Erstelle Basis ($class) bei "+new Location(system,x,y)+"<br />\n");
	}
}
