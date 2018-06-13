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

class TagList implements BBCodeFunction {
	enum Type {
		/**
		 * Eine Liste.
		 */
		LIST,
		/**
		 * Eine Unterliste (Liste innerhalb einer Liste.
		 */
		SUBLIST
	}

	private Type type;
	
	protected TagList( Type type ) {
		this.type = type;
	}
	
	@Override
	public String handleMatch(String list, String ... options) {
		String type = "";
		
		if( (options != null) && (options.length > 0) ) {
			type = options[0].trim();
		}

		String[] listfacts;
		if( this.type == Type.LIST ) {
			listfacts = list.split("\\[\\*]");
		}
		else {
			listfacts = list.split("\\[-]");
		}
		
		StringBuilder listContent = new StringBuilder();
		if( !"".equals(type) ) {
			listContent.append("<ol type=\"");
			listContent.append(type);
			listContent.append("\">");
		}
		else {
			listContent.append("<ul>");
		}
		
		// Alle Elemente durchlaufen (Text vor dem ersten [*] ignorieren..)
		for( int i=1; i < listfacts.length; i++ ) {
			listContent.append("<li>");
			listContent.append(listfacts[i]);
			listContent.append("</li>");
		}
		
		if( !"".equals(type) ) {
			listContent.append("</ol>");
		}
		else {
			listContent.append("</ul>");
		}
		
	 	return listContent.toString();
	}

}
