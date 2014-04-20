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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.GlobalSectorTemplate;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;
import java.util.List;

/**
 * Ermoeglicht das Verwalten von Sectortemplates.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Quests", name="Sectortemplates", permission = WellKnownAdminPermission.QUESTS_STM)
public class QuestsSTM implements AdminPlugin {
	@Override
	public void output(StringBuilder echo) throws IOException {
		Context context = ContextMap.getContext();

		String stmid = context.getRequest().getParameterString("stmid");
		String stmaction = context.getRequest().getParameterString("stmaction");
		int x = context.getRequest().getParameterInt("x");
		int y = context.getRequest().getParameterInt("y");
		int w = context.getRequest().getParameterInt("w");
		int h = context.getRequest().getParameterInt("h");
		String newstmid = context.getRequest().getParameterString("newstmid");
		int scriptid = context.getRequest().getParameterInt("scriptid");
		int newstm = context.getRequest().getParameterInt("newstm");
		
		org.hibernate.Session db = context.getDB();
		
		if( stmid.length() != 0 ) {
			switch (stmaction)
			{
				case "new":
				{
					echo.append("<div class='gfxbox' style='width:590px'>");
					GlobalSectorTemplate newtemplate = new GlobalSectorTemplate(stmid, x, y, w, h, 0);
					db.persist(newtemplate);

					echo.append("Sectortemplate hinzugef&uuml;gt");
					echo.append("</div>");
					echo.append("<br />\n");
					break;
				}
				case "edit1":
				{
					GlobalSectorTemplate template = (GlobalSectorTemplate) db.get(GlobalSectorTemplate.class, stmid);

					echo.append("<div class='gfxbox' style='width:590px'>");
					echo.append("<div align=\"center\">STM-ID bearbeiten:</div><br />\n");
					echo.append("<form action=\"./ds\" method=\"post\">\n");
					echo.append("id: <input type=\"text\" name=\"newstmid\" value=\"").append(template.getId()).append("\" /><br />\n");
					echo.append("Pos: <input type=\"text\" name=\"x\" value=\"").append(template.getX()).append("\" size=\"4\" />/\n");
					echo.append("<input type=\"text\" name=\"y\" value=\"").append(template.getY()).append("\" size=\"4\"  /><br />\n");
					echo.append("groesse (optional): <input type=\"text\" name=\"w\" value=\"").append(template.getWidth()).append("\" size=\"4\"  />/\n");
					echo.append("<input type=\"text\" name=\"h\" value=\"").append(template.getHeigth()).append("\" size=\"4\"  /><br /><br />\n");
					echo.append("<input type=\"hidden\" name=\"stmid\" value=\"").append(stmid).append("\" />\n");
					echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
					echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
					echo.append("<input type=\"hidden\" name=\"stmaction\" value=\"edit2\" /></div>\n");
					echo.append("<div align=\"center\"><input type=\"submit\" value=\":: speichern ::\" /></div>\n");
					echo.append("</form>\n");
					echo.append("</div>");
					echo.append("<br />\n");
					break;
				}
				case "edit2":
				{
					GlobalSectorTemplate template = (GlobalSectorTemplate) db.get(GlobalSectorTemplate.class, stmid);

					GlobalSectorTemplate newtemplate = new GlobalSectorTemplate(newstmid, x, y, w, h, scriptid);

					db.delete(template);
					db.persist(newtemplate);

					echo.append("<div class='gfxbox' style='width:590px'>");
					echo.append("Update durchgef&uuml;hrt<br />");
					echo.append("</div>");
					echo.append("<br />\n");
					break;
				}
				case "delete1":
					echo.append("<div class='gfxbox' style='width:590px'>");
					echo.append("Wollen sie das Sectortemplate '").append(stmid).append("' wirklich l&ouml;schen?<br />\n");
					echo.append("<a class=\"error\" href=\"./ds?module=admin&namedplugin=").append(getClass().getName()).append("&stmid=").append(stmid).append("&stmaction=delete2\">Ja</a>");
					echo.append(" - <a class=\"forschinfo\" href=\"./ds?module=admin&namedplugin=").append(getClass().getName()).append("\">Nein</a>");
					echo.append("</div>");
					echo.append("<br />\n");
					break;
				case "delete2":
				{
					echo.append("<div class='gfxbox' style='width:590px'>");

					GlobalSectorTemplate template = (GlobalSectorTemplate) db.get(GlobalSectorTemplate.class, stmid);
					db.delete(template);
					echo.append("Sectortemplate '").append(stmid).append("' gel&ouml;scht");

					echo.append("</div>");
					echo.append("<br />\n");
					break;
				}
			}
		}
		else if( newstm > 0 ) {
			echo.append("<div class='gfxbox' style='width:590px'>");
			echo.append("<div align=\"center\">Neue STM-ID erstellen:</div><br />\n");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("id: <input type=\"text\" name=\"stmid\" value=\"meineid\" /><br />\n");
			echo.append("Pos: <input type=\"text\" name=\"x\" value=\"x\" size=\"4\" />/\n");
			echo.append("<input type=\"text\" name=\"y\" value=\"y\" size=\"4\"  /><br />\n");
			echo.append("groesse (optional): <input type=\"text\" name=\"w\" value=\"0\" size=\"4\"  />/\n");
			echo.append("<input type=\"text\" name=\"h\" value=\"0\" size=\"4\"  /><br /><br />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"stmaction\" value=\"new\" /></div>\n");
			echo.append("<div align=\"center\"><input type=\"submit\" value=\":: speichern ::\" /></div>\n");
			echo.append("</form>\n");
			echo.append("</div>");
			echo.append("<br />\n");		
		}
		
		echo.append("<div class='gfxbox' style='width:590px'>");
		List<GlobalSectorTemplate> templates = Common.cast(db.createQuery("from GlobalSectorTemplate").list());
		
		for(GlobalSectorTemplate template : templates ) {
			echo.append("* <a class=\"forschinfo\" href=\"./ds?module=admin&namedplugin=").append(getClass().getName()).append("&stmid=").append(template.getId()).append("&stmaction=edit1\">");
			echo.append(template.getId()).append("</a> - ").append(template.getX()).append("/").append(template.getY());
			if( (template.getWidth() != 0) || (template.getHeigth() != 0) ) {
				echo.append(" (Groesse: ").append(template.getWidth()).append("x").append(template.getHeigth()).append(")");
			}	
			if( template.getScriptId() != 0 ) {
				echo.append(" - Scriptid: ").append(template.getScriptId());
			} 
			echo.append(" <a class=\"error\" href=\"./ds?module=admin&namedplugin=").append(getClass().getName()).append("&stmid=").append(template.getId()).append("&stmaction=delete1\">X</a>");
			echo.append("<br />\n");
		}
		echo.append("<br />\n");
		echo.append("<div align=\"center\"><a class=\"forschinfo\" href=\"./ds?module=admin&namedplugin=").append(getClass().getName()).append("&newstm=1\">&gt; neu &lt;</div>\n");
		echo.append("</div>");
	}
}
