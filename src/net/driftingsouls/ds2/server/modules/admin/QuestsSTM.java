/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

/**
 * Ermoeglicht das Verwalten von Sectortemplates 
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Quests", name="Sectortemplates")
public class QuestsSTM implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		String stmid = context.getRequest().getParameterString("stmid");
		String stmaction = context.getRequest().getParameterString("stmaction");
		int x = context.getRequest().getParameterInt("x");
		int y = context.getRequest().getParameterInt("y");
		int w = context.getRequest().getParameterInt("w");
		int h = context.getRequest().getParameterInt("h");
		String newstmid = context.getRequest().getParameterString("newstmid");
		int scriptid = context.getRequest().getParameterInt("scriptid");
		int newstm = context.getRequest().getParameterInt("newstm");
		
		Database db = context.getDatabase();
		
		if( stmid.length() != 0 ) {
			if( stmaction.equals("new") ) {			
				echo.append(Common.tableBegin(550,"left"));
				db.update("INSERT INTO global_sectortemplates (id,x,y,w,h,scriptid) " +
							"VALUES ('"+stmid+"',"+x+","+y+","+w+","+h+",0)");
							
				echo.append("Sectortemplate hinzugef&uuml;gt");
				echo.append(Common.tableEnd());	
				echo.append("<br />\n");
			}
			else if( stmaction.equals("edit1") ) {
				SQLResultRow st = db.first("SELECT * FROM global_sectortemplates WHERE id='"+stmid+"'");
				
				echo.append(Common.tableBegin(550,"left"));
				echo.append("<div align=\"center\">STM-ID bearbeiten:</div><br />\n");
				echo.append("<form action=\"./main.php\" method=\"post\">\n");
				echo.append("id: <input type=\"text\" name=\"newstmid\" value=\""+st.getString("id")+"\" /><br />\n");
				echo.append("Pos: <input type=\"text\" name=\"x\" value=\""+st.getInt("x")+"\" size=\"4\" />/\n");
				echo.append("<input type=\"text\" name=\"y\" value=\""+st.getInt("y")+"\" size=\"4\"  /><br />\n");
				echo.append("groesse (optional): <input type=\"text\" name=\"w\" value=\""+st.getInt("w")+"\" size=\"4\"  />/\n");
				echo.append("<input type=\"text\" name=\"h\" value=\""+st.getInt("h")+"\" size=\"4\"  /><br /><br />\n");
				echo.append("<input type=\"hidden\" name=\"stmid\" value=\""+stmid+"\" />\n");
				echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
				echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
				echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
				echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
				echo.append("<input type=\"hidden\" name=\"stmaction\" value=\"edit2\" /></div>\n");
				echo.append("<div align=\"center\"><input type=\"submit\" value=\":: speichern ::\" /></div>\n");
				echo.append("</form>\n");
				echo.append(Common.tableEnd());
				echo.append("<br />\n");	
			}
			else if( stmaction.equals("edit2") ) {
				db.update("UPDATE global_sectortemplates " +
							"SET id='"+newstmid+"',x="+x+",y="+y+"," +
							"w="+w+",h="+h+",scriptid="+scriptid+" " +
							"WHERE id='"+stmid+"'");

				echo.append(Common.tableBegin(550,"left"));
				echo.append("Update durchgef&uuml;hrt<br />");
				echo.append(Common.tableEnd());
				echo.append("<br />\n");
			}
			else if( stmaction.equals("delete1") ) {
				echo.append(Common.tableBegin(550,"left"));
				echo.append("Wollen sie das Sectortemplate '"+stmid+"' wirklich l&ouml;schen?<br />\n");
				echo.append("<a class=\"error\" href=\"./main.php?module=admin&sess="+context.getSession()+"&act="+action+"&page="+page+"&stmid="+stmid+"&stmaction=delete2\">Ja</a>");
				echo.append(" - <a class=\"forschinfo\" href=\"./main.php?module=admin&sess="+context.getSession()+"&act="+action+"&page="+page+"\">Nein</a>");
				echo.append(Common.tableEnd());
				echo.append("<br />\n");	
			}
			else if( stmaction.equals("delete2") ) {
				echo.append(Common.tableBegin(550,"left"));
			
				db.update("DELETE FROM global_sectortemplates WHERE id='"+stmid+"'");
				echo.append("Sectortemplate '"+stmid+"' gel&ouml;scht");
			
				echo.append(Common.tableEnd());
				echo.append("<br />\n");	
			}
		}
		else if( newstm > 0 ) {
			echo.append(Common.tableBegin(550,"left"));
			echo.append("<div align=\"center\">Neue STM-ID erstellen:</div><br />\n");
			echo.append("<form action=\"./main.php\" method=\"post\">\n");
			echo.append("id: <input type=\"text\" name=\"stmid\" value=\"meineid\" /><br />\n");
			echo.append("Pos: <input type=\"text\" name=\"x\" value=\"x\" size=\"4\" />/\n");
			echo.append("<input type=\"text\" name=\"y\" value=\"y\" size=\"4\"  /><br />\n");
			echo.append("groesse (optional): <input type=\"text\" name=\"w\" value=\"0\" size=\"4\"  />/\n");
			echo.append("<input type=\"text\" name=\"h\" value=\"0\" size=\"4\"  /><br /><br />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"stmaction\" value=\"new\" /></div>\n");
			echo.append("<div align=\"center\"><input type=\"submit\" value=\":: speichern ::\" /></div>\n");
			echo.append("</form>\n");
			echo.append(Common.tableEnd());
			echo.append("<br />\n");		
		}
		
		echo.append(Common.tableBegin(550,"left"));
		SQLQuery st = db.query("SELECT * FROM global_sectortemplates");
		while( st.next() ) {
			echo.append("* <a class=\"forschinfo\" href=\"./main.php?module=admin&sess="+context.getSession()+"&act="+action+"&page="+page+"&stmid="+st.getString("id")+"&stmaction=edit1\">");
			echo.append(st.getString("id")+"</a> - "+st.getInt("x")+"/"+st.getInt("y"));
			if( (st.getInt("w") != 0) || (st.getInt("h") != 0) ) {
				echo.append(" (Groesse: "+st.getInt("w")+"x"+st.getInt("h")+")");	
			}	
			if( st.getInt("scriptid") != 0 ) {
				echo.append(" - Scriptid: "+st.getInt("scriptid"));	
			} 
			echo.append(" <a class=\"error\" href=\"./main.php?module=admin&sess="+context.getSession()+"&act="+action+"&page="+page+"&stmid="+st.getString("id")+"&stmaction=delete1\">X</a>");
			echo.append("<br />\n");
		}
		st.free();
		echo.append("<br />\n");
		echo.append("<div align=\"center\"><a class=\"forschinfo\" href=\"./main.php?module=admin&sess="+context.getSession()+"&act="+action+"&page="+page+"&newstm=1\">&gt; neu &lt;</div>\n");
		echo.append(Common.tableEnd());
	}
}
