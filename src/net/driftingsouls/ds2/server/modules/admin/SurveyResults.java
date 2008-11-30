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

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Zeigt die Ergebnisse von Umfragen an
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Spieler", name="Umfrageergebnisse")
public class SurveyResults implements AdminPlugin {
	// TODO: wir brauchen eine libSurvey....
	// Das da sind naemlich identische Konstanten wie im Survey-Modul
	private static final String ETYPE_TEXTBOX = "textbox";
	private static final String ETYPE_EDITBOX	= "editbox";
	private static final String ETYPE_RATEBOX = "ratebox";
	private static final String ETYPE_COMBOBOX = "combobox";
	private static final String ETYPE_DESCRIPTION = "description";
	
	
	private Map<Integer,String> unserializeResult(String result) {
		int[] tmp = Common.explodeToInt(",", result);
		byte[] resultarray = new byte[tmp.length];
		for( int i=0; i < tmp.length; i++ ) {
			resultarray[i] = (byte)tmp[i];
		}
		
		Object resultObj = SerializationUtils.deserialize(resultarray);
		
		return (Map<Integer,String>)resultObj;
	}
	
	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		
		int resultid = context.getRequest().getParameterInt("resultid");
		int surveyid = context.getRequest().getParameterInt("surveyid");
		
		Database db = context.getDatabase();
		
		if( surveyid == 0 ) {
			echo.append(Common.tableBegin(600,"center"));
			SQLQuery survey = db.query("SELECT * FROM surveys");
			while( survey.next() ) {
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=admin&page="+page+"&act="+action+"&resultid=0&surveyid="+survey.getInt("id")+"\">"+survey.getString("name")+"</a>");
				if( (survey.getInt("enabled") != 0) && (survey.getInt("timeout") > 0) ) {
					echo.append(" - <span style=\"color:green\">aktiv</span>");	
				}
				echo.append("<br />\n"); 
				int count = db.first("SELECT count(*) count FROM survey_results WHERE survey_id="+survey.getInt("id")).getInt("count");
				
				int maxcount = db.first("SELECT count(*) count " +
						"FROM users " +
						"WHERE (id BETWEEN "+survey.getInt("minid")+" AND "+survey.getInt("maxid")+") AND " +
								"(signup BETWEEN "+survey.getLong("mintime")+" AND "+survey.getLong("maxtime")+")")
					.getInt("count");
				
				echo.append("<span style=\"font-size:12px\">"+count+"/"+maxcount+" Ergebnisse</span><br />\n");
			}
			survey.free();
			echo.append(Common.tableEnd());
		}
		else {
			if( resultid == 0 ) {
				resultid = db.first("SELECT id FROM survey_results " +
						"WHERE survey_id="+surveyid+" " +
						"ORDER BY id ASC").getInt("id");	
			}
			
			echo.append(Common.tableBegin(700,"center"));
			echo.append("<div style=\"text-align:center\">\n");
			echo.append("Datensatz: "+resultid+" - Survey: "+surveyid);
			echo.append("</div>");
			
			echo.append("<table class=\"noBorderX\" width=\"95%\">\n");
			echo.append("<tr>\n");
			
			SQLResultRow previd = db.first("SELECT id FROM survey_results " +
					"WHERE survey_id="+surveyid+" AND id<"+resultid+" " +
					"ORDER BY id DESC");
			
			echo.append("<td class=\"noBorderX\" style=\"text-align:left\">");
			if( !previd.isEmpty() ) {
				echo.append("<a class=\"ok\" href=\"./ds?module=admin&page="+page+"&act="+action+"&resultid="+previd.getInt("id")+"&surveyid="+surveyid+"\">&lt;&lt;&lt;</a>");
			}
			echo.append("</td>\n");
			
			echo.append("<td class=\"noBorderX\" style=\"vertical-align:top;width:35px\">&nbsp;</td>\n");
			
			SQLResultRow nextid = db.first("SELECT id FROM survey_results " +
					"WHERE survey_id="+surveyid+" AND id>"+resultid+" " +
					"ORDER BY id ASC");
			echo.append("<td class=\"noBorderX\" style=\"text-align:right\">");
			if( !nextid.isEmpty() ) {
				echo.append("<a class=\"ok\" href=\"./ds?module=admin&page="+page+"&act="+action+"&resultid="+nextid.getInt("id")+"&surveyid="+surveyid+"\">&gt;&gt;&gt;</a>");
			}
			echo.append("</td>\n");
			echo.append("</tr>\n");
			
			String result = db.first("SELECT result FROM survey_results WHERE id="+resultid).getString("result");
			Map<Integer,String> resultdata = unserializeResult(result);			
			
			SQLQuery entry = db.query("SELECT * FROM survey_entries WHERE survey_id="+surveyid);
			while( entry.next() ) {
				echo.append("<tr>\n");
				final String type = entry.getString("type");
				if( type.equals(ETYPE_TEXTBOX) ) {
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">"+Common._text(entry.getString("name"))+"</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top;width:35px\">&nbsp;</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">\n");
					echo.append(Common._plaintext(resultdata.get(entry.getInt("id"))));
				}	
				else if( type.equals(ETYPE_EDITBOX) ) {
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">"+Common._text(entry.getString("name"))+"</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top;width:35px\">&nbsp;</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">\n");
					echo.append(resultdata.get(entry.getInt("id")));
				}	
				else if( type.equals(ETYPE_RATEBOX) ) {
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">"+Common._text(entry.getString("name"))+"</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top;width:35px\">&nbsp;</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">\n");
					echo.append(resultdata.get(entry.getInt("id")));
				}	
				else if( type.equals(ETYPE_COMBOBOX) ) {
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">"+Common._text(entry.getString("name"))+"</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top;width:35px\">&nbsp;</td>\n");
					echo.append("<td class=\"noBorderX\" style=\"vertical-align:top\">\n");
					String[] itemlist = StringUtils.split(
							StringUtils.replace(entry.getString("params"), "\r\n", "\n"), 
							'\n');
					echo.append(itemlist[Integer.parseInt(resultdata.get(entry.getInt("id")))]+" ("+resultdata.get(entry.getInt("id"))+")");
				}	
				else if( type.equals(ETYPE_DESCRIPTION) ) {
					echo.append("<td class=\"noBorderX\" colspan=\"3\" style=\"vertical-align:top\">"+Common._text(entry.getString("name"))+"\n");		
				}
				
				echo.append("</td>");
				echo.append("</tr>");
				
				echo.append("<tr>\n");
				echo.append("<td colspan=\"3\" class=\"noBorderX\" style=\"vertical-align:top\">\n");
				echo.append("<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />\n");
				echo.append("</td>");
				echo.append("</tr>");
			}
			entry.free();
			
			echo.append("</table>");
			echo.append(Common.tableEnd());
		}
	}
}
