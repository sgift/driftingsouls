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

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Erweiterte Spielerstatistik.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Spieler", name="Erw. Statistik")
public class PlayerStatistics implements AdminPlugin {

	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();

		echo.write("<div class='gfxbox' style='width:700px'>");
		echo.write("<script type='text/javascript'>$(document).ready(function(){");
		echo.write("var data = [");
		echo.write(
			getJsValue(
				"Aktive Spieler (<=49 Ticks)",
				"select count(*) from User where inakt<=49 and id>4 and (vaccount=0 OR wait4vac>0)"));
		echo.write(",");
		echo.write(
			getJsValue(
				"tw. aktive Spieler (50 - 98 Ticks)",
				"select count(*) from User where inakt>49 and inakt<=98 and id>4 and (vaccount=0 OR wait4vac>0)"));
		echo.write(",");
		echo.write(
			getJsValue(
				"tw. inaktive Spieler (99 - 299 Ticks)",
				"select count(*) from User where inakt>98 and inakt<300 and id>4 and (vaccount=0 OR wait4vac>0)"));
		echo.write(",");
		echo.write(
			getJsValue(
				"Inaktive Spieler (>=300 Ticks)",
				"select count(*) from User where inakt>=300 and id>4 and (vaccount=0 OR wait4vac>0)"));
		echo.write(",");
		echo.write(
			getJsValue(
				"Spieler in Vacation",
				"select count(*) from User where id>4 and (vaccount>0 AND wait4vac=0)"));
		echo.write("];\n");
		echo.write("var plot = DS.plot('chart1', [data], { title:'Spieler', seriesDefaults : {");
		echo.write("renderer: $.jqplot.PieRenderer, rendererOptions:{showDataLabels:true, dataLabels:'value', dataLabelThreshold:0}}, ");
		echo.write("legend:{show:true, location:'e'},");
		echo.write("highlighter: {show:true}");
		echo.write("});");
		echo.write("});</script>");
		echo.write("<div id='chart1' style='height:300px'></div>");


		echo.write("<table><thead><tr><th>Gruppe</th><th>Anzahl</th></tr></thead>");
		echo.write("<tbody>");

		addValue(echo,
				"Spieler",
				"select count(*) from User where id>4");

		addValue(echo,
				"NPCs",
				"select count(*) from User where id<=4");

		addValue(echo,
				"Echte NPCs (id<0)",
				"select count(*) from User where id<=0");

		addValue(echo,
				"Aktive NPCs (<=49 Ticks)",
				"select count(*) from User where inakt<=49 and id<5 and (vaccount=0 OR wait4vac>0)");

		addValue(echo,
				"Inaktive NPCs (>=300 Ticks)",
				"select count(*) from User where inakt>=300 and id<5 and (vaccount=0 OR wait4vac>0)");

		echo.write("</tbody></table>");
		echo.write("</div>");
	}

	private String getJsValue(String label, String query) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		long count = (Long)db
			.createQuery(query)
			.uniqueResult();
		return "['"+label+"',"+count+"]";
	}

	private void addValue(Writer echo, String label, String query) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		long count = (Long)db
			.createQuery(query)
			.uniqueResult();
		echo.write("<tr><td>"+label+"</td><td>"+count+"</td></tr>");
	}
}
