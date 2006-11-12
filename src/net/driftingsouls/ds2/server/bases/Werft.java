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

import org.apache.commons.lang.StringEscapeUtils;

import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import net.driftingsouls.ds2.server.werften.WerftGUI;
import net.driftingsouls.ds2.server.werften.WerftObject;

class Werft extends DefaultBuilding {
	/**
	 * Erstellt eine neue Instanz der Werft
	 * @param row Die SQL-Ergebniszeile mit den Gebaeudedaten der Werft
	 */
	public Werft(SQLResultRow row) {
		super(row);
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
	public void build(int col) {
		super.build(col);
		
		ContextMap.getContext().getDatabase().update("INSERT INTO werften (type,col) VALUES(1,",col,")");
	}


	@Override
	public void cleanup(Context context, int col) {
		super.cleanup(context, col);
		
		context.getDatabase().update("DELETE FROM werften WHERE col=",col);
	}


	@Override
	public String echoShortcut(Context context, int col, int field, int building) {
		Database db = context.getDatabase();
		
		String sess = context.getSession();
		
		SQLResultRow colony = db.first("SELECT id,owner,system FROM bases WHERE id=",col);
		
		StringBuilder result = new StringBuilder(200);
		
		SQLResultRow werftRow = db.first("SELECT * FROM werften WHERE col=",col);
		if( !werftRow.isEmpty() ) {
			BaseWerft werft = new BaseWerft(werftRow,"pwerft",colony.getInt("system"),colony.getInt("owner"),colony.getInt("id"), field);
			
			if( !werft.isBuilding() ) {
				result.append("<a class=\"back\" href=\"./main.php?module=building&amp;sess=");
				result.append(sess);
				result.append("&amp;col=");
				result.append(col);
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[W]</a>");
			} 
			else {
				SQLResultRow type = werft.getBuildShipType();
	
				StringBuilder popup = new StringBuilder(200);
				popup.append(Common.tableBegin(420, "left").replace('"', '\''));
				popup.append("<img align='left' border='0' src='");
				popup.append(type.getString("picture"));
				popup.append("' alt='");
				popup.append(type.getString("nickname"));
				popup.append("' />");
				popup.append("&nbsp;Baut: ");
				popup.append(type.getString("nickname"));
				popup.append("<br />");
				popup.append("&nbsp;Dauer: <img style='vertical-align:middle' src='");
				popup.append(Configuration.getSetting("URL"));
				popup.append("data/interface/time.gif' alt='noch ' />");
				popup.append(werft.getRemainingTime());
				popup.append("<br />");
				if( werft.getRequiredItem() != -1 ) {
					
					popup.append("&nbsp;Ben&ouml;tigt: ");
					popup.append("<img style='vertical-align:middle' src='");
					popup.append("../data/items/");
					popup.append(Items.get().item(werft.getRequiredItem()).getPicture());
					popup.append("' alt='' />");
					if( werft.isBuildContPossible() ) {
						popup.append("<span style='color:green'>");
					}
					else {
						popup.append("<span style='color:red'>");
					}
					popup.append(Items.get().item(werft.getRequiredItem()).getName());
					popup.append("</span>");
				}
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
				result.append("',REFY,22,NOJUSTY,FGCLASS,'gfxtooltip',BGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip',TIMEOUT,0,DELAY,150,WIDTH,430);\" onmouseout=\"return nd();\" href=\"./main.php?module=building&amp;sess=");
				result.append(sess);
				result.append("&amp;col=");
				result.append(col);
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[W]<span style=\"font-weight:normal\">");
				result.append(werft.getRemainingTime());
				result.append("</span></a>");
			}
		}
		
		return result.toString();
	}

	@Override
	public boolean isActive(int col, int status, int field) {
		Database db = ContextMap.getContext().getDatabase();
	
		SQLResultRow werftRow = db.first("SELECT * FROM werften WHERE col=",col);
		if( !werftRow.isEmpty() ) {
			SQLResultRow colony = db.first("SELECT id,owner,system FROM bases WHERE id=",col);
			
			WerftObject werft = new BaseWerft(werftRow,"pwerft",colony.getInt("system"),colony.getInt("owner"),colony.getInt("id"), field);
			return (werft.isBuilding() ? true : false);
		}
		
		return false;	
	}

	@Override
	public String output(Context context, TemplateEngine t, int col, int field, int building) {
		Database db = context.getDatabase();
		User user = context.getActiveUser();
		
		String sess = context.getSession();
		StringBuilder response = new StringBuilder(500);
		
		SQLResultRow colony = db.first("SELECT id,owner,name,x,y,system,e FROM bases WHERE id=",col);
	
		if( colony.isEmpty() ) {
	   		response.append("<a href=\"./main.php?module=basen&amp;sess="+sess+"\"><span style=\"color:#ff0000; font-weight:bold\">Fehler: Die angegebene Kolonie existiert nicht</span></a>\n");
			return response.toString();
		}
		
		if( colony.getInt("owner") != user.getID()) {
	   		response.append("<a href=\"./main.php?module=basen&amp;sess="+sess+"\"><span style=\"color:#ff0000; font-weight:bold\">Fehler: Diese Werft gehoert ihnen NICHT!</span></a>\n");
			return response.toString();
		}
		
		SQLResultRow werftdata = db.first("SELECT * FROM werften WHERE col=",colony.getInt("id"));
		if( werftdata.isEmpty() ) {
	   		response.append("<a href=\"./main.php?module=basen&amp;sess="+sess+"\"><span style=\"color:#ff0000; font-weight:bold\">Fehler: Die angegebene Kolonie hat keine Werft</span></a>\n");
			return response.toString();
		}
		
		response.append("<div>Werft auf "+colony.getString("name")+"<br /><br /></div>\n");
		
		BaseWerft werft = new BaseWerft(werftdata,"pwerft",colony.getInt("system"),colony.getInt("owner"),colony.getInt("id"), field);
		WerftGUI werftgui = new WerftGUI( context );
		response.append(werftgui.execute( werft ));
		
		response.append("<div><br /></div>\n");
		return response.toString();
	}
}
