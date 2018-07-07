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

import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;
import net.driftingsouls.ds2.server.units.UnitType;

/**
 * BBCode fuer Einheiten.
 * @author Christopher Jung
 *
 */
public class TagUnit implements BBCodeFunction {
	@Override
	public String handleMatch(String content, String... values) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		try {
			Long count = null;
			if( content.length() > 0 ) {
				count = Long.parseLong(content);
			}
			int uid = Integer.parseInt(values[0]);

			String format = "in";
			if( values.length > 1 ) {
				format = values[1];
			}

			String unknstr = "Unbekannte Einheit";
			if( count != null ) {
				unknstr = Common.ln(count)+"x "+unknstr;
			}

			UnitType unit = (UnitType)db.get(UnitType.class, uid);

			if( unit == null ) {
				return unknstr;
			}

			User user = (User)context.getActiveUser();
			if( user == null && unit.isHidden() ) {
				return unknstr;
			}
			else if( user != null && unit.isHidden() && !context.hasPermission(WellKnownPermission.UNIT_VERSTECKTE_SICHTBAR) )
			{
				return unknstr;
			}

			StringBuilder tmpString = new StringBuilder(30);

			if( count != null ) {
				tmpString.append(Common.ln(count));
				tmpString.append("x ");
			}

			if( format.indexOf('i') != -1 ) {
				tmpString.append("<img align=\"middle\" border=\"0\" src=\"");
				tmpString.append(unit.getPicture());
				tmpString.append("\" alt=\"\" />");
			}

			if( format.indexOf('n') != -1 ) {
				tmpString.append(unit.getName());
			}

			return tmpString.toString();
		}
		catch( NumberFormatException e ) {
			return "<span style=\"color:red\">Ungueltiger Unit-Tag: [unit="+Common.implode(",", values)+"]"+content+"[/unit]</span>";
		}
	}

}
