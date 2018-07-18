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
package net.driftingsouls.ds2.server.framework.bbcode;

import net.driftingsouls.ds2.server.framework.Common;

import java.util.regex.Pattern;

class TagURL implements BBCodeFunction {
	/**
	 * Maximale Laenge des Titels.
	 */
	private static final int MAXWIDTH = 100;
	/**
	 * Falls der Titel zu Lang ist: Anzahl Zeichen vom Anfang.
	 */
	private static final int WIDTH_BEGIN = 60;
	/**
	 * Falls der Titel zu Lang ist: Anzahl Zeichen vom Ende.
	 */
	private static final int WIDTH_END = 25;
	
	private static final Pattern protocol = Pattern.compile("[a-z]+://");
	
	@Override
	public String handleMatch(String title, String ... options) {
		String url = title;
		
		if( (options != null) && (options.length != 0) ) {
			url = options[0];
			if( "".equals(title.trim()) ) {
				title = url;
			}
		}

		if( !protocol.matcher(url).lookingAt() ) {
			url = "http://"+url;
		}
		
		if( title.length() > MAXWIDTH ) {
			if( Common._stripHTML(title).length() > MAXWIDTH ) {
				title = Common._stripHTML(title);
				title = title.substring(0,WIDTH_BEGIN)+"..."+title.substring(title.length()-WIDTH_END);
			}
		}
		
		return "<a class=\"forschinfo\" href=\""+url+"\" target=\"_blank\">"+title+"</a>";
	}

}
