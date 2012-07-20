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

import org.springframework.beans.factory.annotation.Configurable;

/**
 * Erweiterte Spielerstatistik.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Spieler", name="Erw. Statistik")
@Configurable
public class PlayerStatistics implements AdminPlugin {

	@Override
	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		Writer echo = context.getResponse().getWriter();

		echo.write("<div class='gfxbox' style='width:700px'>");
		echo.write("<table><thead><tr><th>Gruppe</th><th>Anzahl</th></tr></thead>");
		echo.write("<tbody>");

		long count = (Long)db
			.createQuery("select count(*) from User where inakt<=49 and id>4 and (vaccount=0 OR wait4vac>0)")
			.uniqueResult();
		echo.write("<tr><td>Aktive Spieler (<=49 Ticks)</td><td>"+count+"</td></tr>");

		count = (Long)db
			.createQuery("select count(*) from User where inakt>=300 and id>4 and (vaccount=0 OR wait4vac>0)")
			.uniqueResult();
		echo.write("<tr><td>Inaktive Spieler (>=300 Ticks)</td><td>"+count+"</td></tr>");

		count = (Long)db
			.createQuery("select count(*) from User where id>4 and (vaccount>0 AND wait4vac=0)")
			.uniqueResult();
		echo.write("<tr><td>Spieler in Vacation</td><td>"+count+"</td></tr>");

		count = (Long)db
			.createQuery("select count(*) from User where inakt<=49 and id<5 and (vaccount=0 OR wait4vac>0)")
			.uniqueResult();
		echo.write("<tr><td>Aktive NPCs (<=49 Ticks)</td><td>"+count+"</td></tr>");

		echo.write("</tbody></table>");
		echo.write("</div>");
	}

}
