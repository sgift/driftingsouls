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

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;

/**
 * Das Hauptframe von DS
 * @author Christopher Jung
 *
 */
public class MainController extends DSGenerator {

	public MainController(Context context) {
		super(context);
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}

	@Override
	protected void printHeader( String action ) {	
		StringBuffer out = getContext().getResponse().getContent();
		out.append("<head>\n");
		out.append("<title>Drifting Souls 2</title>\n");
		out.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
		out.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+Configuration.getSetting("URL")+"format.css\" />\n");
		out.append("</head>\n");
	}


	@Override
	protected void printFooter(String action) {
	}
	
	public void defaultAction() {
		User user = getUser();
		StringBuffer out = getContext().getResponse().getContent();
		
		if( user.getUserImagePath() != User.getDefaultImagePath(getDatabase()) ) {
			parameterNumber("gfxpakversion");
			int gfxpakversion = getInteger("gfxpakversion");
			
			if( (gfxpakversion != 0) && (gfxpakversion != Configuration.getIntSetting("GFXPAK_VERSION")) ) {
				Database db = getDatabase();
				db.update("UPDATE sessions SET usegfxpak='0' WHERE session='",getString("sess"),"' AND id='",user.getID(),"'");
				
				out.append("<script type=\"text/javascript\">\n");
				out.append("<!--\n");
				out.append("alert('Die von ihnen verwendete Version des Grafikpaks ist veraltet. Die Benutzung des Grafikpaks wurde daher deaktiviert.\nBitte laden sie sich die neuste Version des Grafikpaks herunter!')\n");
				out.append("// -->\n");
				out.append("</script>\n");
			}
		}
	
		if( user.getUserValue("TBLORDER/admin/show_cmdline") != null ) {
			out.append("<frameset cols=\"182,*\" framespacing=\"0\" border=\"0\" frameborder=\"0\">\n");
			out.append("<frame src=\"./main.php?sess="+getString("sess")+"&amp;module=links\" id=\"_framenavi\" name=\"navi\" frameborder=\"0\" />\n");
			out.append("<frameset rows=\"*,75\" framespacing=\"0\" border=\"0\" frameborder=\"0\">\n");
			out.append("<frame src=\"./main.php?sess="+getString("sess")+"&amp;module=ueber\" id=\"_framemain\" name=\"main\" scrolling=\"auto\" frameborder=\"0\" />\n");
			out.append("<frame src=\"./main.php?sess="+getString("sess")+"&amp;module=admin&amp;namedplugin=adminConsole&amp;cleanpage=1\" id=\"_frameconsole\" name=\"console\" scrolling=\"auto\" frameborder=\"0\" />\n");
			out.append("</frameset>\n");
			out.append("</frameset>\n");
		}
		else {
			out.append("<frameset cols=\"182,*\" framespacing=\"0\" border=\"0\" frameborder=\"0\">\n");
			out.append("<frame src=\"./main.php?sess="+getString("sess")+"&amp;module=links\" id=\"_framenavi\" name=\"navi\" frameborder=\"0\" />\n");
			out.append("<frame src=\"./main.php?sess="+getString("sess")+"&amp;module=ueber\" id=\"_framemain\" name=\"main\" scrolling=\"auto\" frameborder=\"0\" />\n");
			out.append("</frameset>\n");
		}
	}
}
