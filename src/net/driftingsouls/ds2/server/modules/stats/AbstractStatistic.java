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
package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.Map;

abstract class AbstractStatistic implements Statistic {
	private Context context = null;

	protected AbstractStatistic() {
		context = ContextMap.getContext();
	}

	protected final Context getContext() {
		return context;
	}

	protected final String getUserURL() {
		return "./ds?module=userprofile&amp;user=";
	}

	protected final String getAllyURL() {
		return "./ds?module=allylist&amp;action=details&amp;details=";
	}

	/**
	 * Sortierer fuer Maps, der nicht nach dem Key sondern nach dem Value absteigend
	 * sortiert.
	 * @author Christopher Jung
	 *
	 * @param <T> Der Map-Key
	 */
	protected static class MapValueDescComparator<T> implements Comparator<T>
	{
		private Map<T,Long> map;

		/**
		 * Konstruktor.
		 * @param map Die zu sortierende Map
		 */
		public MapValueDescComparator(Map<T, Long> map)
		{
			this.map = map;
		}

		@Override
		public int compare(T arg0, T arg1)
		{
			return this.map.get(arg1).compareTo(this.map.get(arg0));
		}
	}

	protected interface LinkGenerator<T>
	{
		String generate(T object);
	}

	/**
	 * Link-Generator fuer User-Entities.
	 */
	protected static final LinkGenerator<User> USER_LINK_GENERATOR = object -> "<a class=\"profile\" href=\"./ds?module=userprofile&amp;user="+object.getId()+"\">"+
		Common._title(object.getName())+" ("+object.getId()+")</a>";

	/**
	 * Link-Generator fuer Ally-Entities.
	 */
	protected static final LinkGenerator<Ally> ALLY_LINK_GENERATOR = object -> "<a class=\"profile\" href=\"./ds?module=allylist&amp;action=details&amp;details="+object.getId()+"\">"+
		Common._title(object.getName())+" ("+object.getId()+")</a>";

	/**
	 * Generiert eine Statistik mit Platz, Namen und (optional) Anzahl.
	 * @param name Der Name der Statistik
	 * @param counts Eine sortierte Map deren Key eine Entity und deren Value die anzuzeigene Anzahl enthaelt.
	 * @param generator Der Generator fuer den anzuzeigenden Namen (Link)
	 * @param showCount <code>true</code> falls die Anzahl angezeigt werden soll
	 * @param size Die maximale Anzahl an anzuzeigenden Zeilen bzw 0 fuer beliebig viele
	 * @throws IOException
	 */
	protected <T> void generateStatistic(String name, Map<T,Long> counts, LinkGenerator<T> generator, boolean showCount, long size) throws IOException {
		Writer echo = getContext().getResponse().getWriter();

		echo.append("<h1>").append(name).append("</h1>");
		echo.append("<table class='stats'>\n");

		int count = 0;
		for( Map.Entry<T,Long> entry : counts.entrySet() )
		{
	   		echo.append("<tr><td>").append(Integer.toString(count + 1)).append(".</td>\n");
			echo.append("<td>").append(generator.generate(entry.getKey())).append("</td>\n");
			if( showCount ) {
				echo.append("<td>").append(Common.ln(entry.getValue())).append("</td></tr>\n");
			}

	   		count++;
	   		if( size > 0 && count >= size )
	   		{
	   			break;
	   		}
		}

		echo.append("</table>\n");
	}
}
