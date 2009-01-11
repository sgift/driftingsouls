/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;

import org.apache.commons.fileupload.FileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Ermoeglicht das Einfuegen von Downloads ins Portal.
 * @author Christopher Jung
 *
 */
@Configurable
@AdminMenuEntry(category="Portal", name="Downloads")
public class PortalDownloads implements AdminPlugin {
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

    @Override
	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		
		int dlid = context.getRequest().getParameterInt("dlid");
		String dldescription = context.getRequest().getParameterString("dldescription");
		String dlname = context.getRequest().getParameterString("dlname");
		String dlaction = context.getRequest().getParameterString("dlaction");
		
		Database db = context.getDatabase();
		
		final String downloadPath = config.get("ABSOLUTE_PATH")+"downloads/";
		
		// Download loeschen
		if( dlaction.equals("delete") ) {
			SQLResultRow dl = db.first("SELECT * FROM portal_downloads WHERE id="+dlid);
			if( !dl.isEmpty() ) {
				new File(downloadPath+dl.getString("file"))
					.delete();
				
				db.update("DELETE FROM portal_downloads WHERE id="+dlid);
				echo.append("Download gel&ouml;scht<br /><br />\n");
			}
		}
		// Download hinzufuegen
		else if( dlaction.equals("add") ) {
			long time = Common.time();
			
			try {
				List<FileItem> list = context.getRequest().getUploadedFiles();
				String fileName = downloadPath+Common.date("YmdHis_",time)+list.get(0).getName();
				String fileUrl = Common.date("YmdHis_",time)+list.get(0).getName();
				
				list.get(0).write(new File(fileName));
				
				db.prepare("INSERT INTO portal_downloads (`name`,`date`,`description`,`file`) VALUES " +
						"( ?, ?, ?, ?)")
					.update(dlname, time, dldescription, fileUrl);
					
				echo.append("Download hinzugef&uuml;gt<br /><br />\n");
			}
			catch( Exception e ) {
				echo.append("Konnte Download nicht hinzuf&uuml;gen: "+e);
			}
		}
		
		// Liste aller Downloads anzeigen
		echo.append(Common.tableBegin(600,"center"));
		
		echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
		SQLQuery dl = db.query("SELECT * FROM portal_downloads");
		while( dl.next() ) {
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">\n");		
			echo.append(Common._plaintitle(dl.getString("name")));
			echo.append("</td>\n");
			
			echo.append("<td class=\"noBorderX\">\n");
			echo.append(Common.date("j.n.Y G:i",dl.getLong("date")));	
			echo.append("</td>\n");
			
			echo.append("<td class=\"noBorderX\">\n");
			echo.append(Common._plaintext(dl.getString("description")));	
			echo.append("</td>\n");
			
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<a class=\"error\" href=\"" +
					"./ds?module=admin"+
					"&act="+action+"&page="+page+"&dlid="+dl.getInt("id")+"&dlaction=delete" +
					"\">X</a>");
			echo.append("</td>\n");
			
			echo.append("</tr>\n");
		}
		echo.append("</table>\n");

		echo.append("<hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" />\n");

		echo.append("<form action=\"ds\" method=\"post\" enctype=\"multipart/form-data\">\n");
		echo.append("<table class=\"noBorderX\">\n");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">\n");	
		echo.append("Name:&nbsp;\n");
		echo.append("</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\" colspan=\"4\">\n");	
		echo.append("<input type=\"text\" name=\"dlname\" value=\"\" maxlength=\"60\" style=\"width:300px\" />\n");
		echo.append("</td>\n");
		echo.append("</tr>\n");
		
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">\n");	
		echo.append("Datei:&nbsp;\n");
		echo.append("</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\" colspan=\"4\">\n");	
		echo.append("<input type=\"file\" name=\"dlfile\" style=\"width:300px\" />\n");
		echo.append("</td>\n");
		echo.append("</tr>\n");
			
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">\n");	
		echo.append("Beschreibung:&nbsp;\n");
		echo.append("</td>\n");
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\" colspan=\"4\">\n");	
		echo.append("<textarea name=\"dldescription\" rows=\"3\" cols=\"30\" style=\"width:300px\"></textarea>\n");
		echo.append("</td>\n");
		echo.append("</tr>\n");

		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\" style=\"vertical-align:top;text-align:center\" colspan=\"5\">\n");
		echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"hidden\" name=\"dlaction\" value=\"add\" />\n");
		echo.append("<input type=\"submit\" value=\"hinzuf&uuml;gen\"  style=\"width:100px\"/>");
		echo.append("</td>\n");	
		echo.append("</tr>\n");
		
		echo.append("</table>\n");
		echo.append("</form>\n");
		echo.append(Common.tableEnd());
	}
}
