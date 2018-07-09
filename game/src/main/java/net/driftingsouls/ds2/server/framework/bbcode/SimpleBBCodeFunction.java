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

import java.util.ArrayList;
import java.util.List;

/**
 * Spezial-BBCode-Klasse.
 * Wird intern von BBCodeParser zur Repraesentation von BBCodes ohne eigene BBCodeFunction genutzt.
 * @author bktheg
 *
 */
class SimpleBBCodeFunction implements BBCodeFunction {
	private String replace = null;
	private String[] parts = null;
	private Integer[] indexParts = null;
	private int max = 0;
	
	SimpleBBCodeFunction(String replace) {
		List<String> parts = new ArrayList<>();
		List<Integer> indexParts = new ArrayList<>();
		this.replace = replace;
		int index;
		while( (index = replace.indexOf('$')) != -1 ) {
			if( (index+1 < replace.length()) ) {
				char chr = replace.charAt(index+1);
				if( chr == '1' || chr == '2' || chr == '3' ) {
					if( !"".equals(replace.substring(0,index)) ) {
						parts.add(replace.substring(0, index));
					}
					indexParts.add(Integer.parseInt(""+chr));
					if( index+2 < replace.length() ) {
						replace = replace.substring(index+2);
					}
					else {
						replace ="";
						break;
					}
				}
			}
		}
		if( !replace.equals("") ) {
			parts.add(replace);
		}

		this.parts = parts.toArray(new String[parts.size()]);
		this.indexParts = indexParts.toArray(new Integer[indexParts.size()]);
		max = Math.max(this.parts.length, this.indexParts.length);
	}

	@Override
	public String handleMatch(String paramText, String ... values) {
		if( paramText == null ) {
			return replace;
		}
		StringBuilder result = new StringBuilder(replace.length());
		for( int i=0; i < max; i++ ) {
			if( i < parts.length ) {
				result.append(parts[i]);
			}
			if( i < indexParts.length ) {
				if( indexParts[i] == 1 ) {
					result.append(paramText);
				}
				else if( (values != null) && (indexParts[i]-1 <= values.length) ) { 
					result.append(values[indexParts[i]-2]);
				}
			}
		}

		return result.toString();
	}
}
