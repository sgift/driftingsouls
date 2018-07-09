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
package net.driftingsouls.ds2.server.bbcodes;

import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import net.driftingsouls.ds2.server.tasks.Taskmanager;

/**
 * BBCode zur Bestaetigung/Ablehung von Tasks.
 * @author Christopher Jung
 *
 */
public class TagIntrnlConfTask implements BBCodeFunction
{
	@Override
	public String handleMatch(String content, String... values)
	{
		String taskid = values[0];
		StringBuilder str = new StringBuilder(100);

		if( Taskmanager.getInstance().getTaskByID(taskid) != null )
		{
			str.append("<div align=\"center\">");
			str.append("<table class=\"noBorderX\" width=\"500\"><tr><td class=\"BorderX\" align=\"center\">");
			str.append(content);
			str.append("<br />");
			str.append("<a class=\"ok\" target=\"main\" href=\"./ds?module=comm&action=send&to=task&title=").append(taskid).append("&msg=handletm\">ja</a> - ");
			str.append("<a class=\"error\" target=\"main\" href=\"./ds?module=comm&action=send&to=task&title=").append(taskid).append("&msg=dismiss\">nein</a>");
			str.append("</td></tr></table>");
			str.append("</div>");
		}
		else
		{
			str.append("<div align=\"center\">");
			str.append("<table class=\"noBorderX\" width=\"500\"><tr><td class=\"BorderX\" align=\"center\">");
			str.append(content);
			str.append("<br />");
			str.append("<span style=\"color:red\">Die Anfrage wurde bereits bearbeitet</span>");
			str.append("</td></tr></table>");
			str.append("</div>");
		}
		return str.toString();
	}

}
