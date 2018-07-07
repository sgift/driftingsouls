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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;

/**
 * Erweiterte Spielerstatistik.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Spieler", name="Erw. Statistik", permission = WellKnownAdminPermission.PLAYER_STATISTICS)
public class PlayerStatistics implements AdminPlugin {

	@Override
	public void output(StringBuilder echo) throws IOException
	{
		echo.append("<div class='gfxbox' style='width:700px'>");
		echo.append("<script type='text/javascript'>$(document).ready(function(){");
		echo.append("var data = [");
		echo.append(
				getJsValue(
						"Aktive Spieler (<=49 Ticks)",
						"select count(*) from User where inakt<=49 and id>4 and (vaccount=0 OR wait4vac>0)")
		);
		echo.append(",");
		echo.append(
				getJsValue(
						"tw. aktive Spieler (50 - 98 Ticks)",
						"select count(*) from User where inakt>49 and inakt<=98 and id>4 and (vaccount=0 OR wait4vac>0)")
		);
		echo.append(",");
		echo.append(
				getJsValue(
						"tw. inaktive Spieler (99 - 299 Ticks)",
						"select count(*) from User where inakt>98 and inakt<300 and id>4 and (vaccount=0 OR wait4vac>0)")
		);
		echo.append(",");
		echo.append(
				getJsValue(
						"Inaktive Spieler (>=300 Ticks)",
						"select count(*) from User where inakt>=300 and id>4 and (vaccount=0 OR wait4vac>0)")
		);
		echo.append(",");
		echo.append(
				getJsValue(
						"Spieler in Vacation",
						"select count(*) from User where id>4 and (vaccount>0 AND wait4vac=0)")
		);
		echo.append("];\n");
		echo.append("var plot = DS.plot('chart1', [data], { title:'Spieler', seriesDefaults : {");
		echo.append("renderer: $.jqplot.PieRenderer, rendererOptions:{showDataLabels:true, dataLabels:'value', dataLabelThreshold:0}}, ");
		echo.append("legend:{show:true, location:'e'},");
		echo.append("highlighter: {show:true}");
		echo.append("});");
		echo.append("});</script>");
		echo.append("<div id='chart1' style='height:300px'></div>");


		echo.append("<table><thead><tr><th>Gruppe</th><th>Anzahl</th></tr></thead>");
		echo.append("<tbody>");

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

		echo.append("</tbody></table>");
		echo.append("</div>");
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

	private void addValue(StringBuilder echo, String label, String query) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		long count = (Long)db
			.createQuery(query)
			.uniqueResult();
		echo.append("<tr><td>").append(label).append("</td><td>").append(count).append("</td></tr>");
	}
}
