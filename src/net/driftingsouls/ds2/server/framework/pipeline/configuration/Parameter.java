/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.pipeline.configuration;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Node;

class Parameter {
	private static final int PLAIN = 0;
	private static final int URL_PARAMETER = 1;
	private static final int URL_DIRECTORY = 2;
	
	private int type = PLAIN;
	private String data = "";
	
	/**
	 * Erstellt aus einem Elternknoten, welcher Parameterinformationen enthaelt, ein
	 * Parameterobjekt.
	 * 
	 * @param masternode Der Elternknoten mit Parameterinformationen
	 * @throws Exception
	 */
	public Parameter(Node masternode) throws Exception {
		Node node = XMLUtils.getNodeByXPath(masternode, "plain | urlparameter | urldirectory");
		if( node == null ) {
			throw new Exception("Keine Parameter vorhanden");
		}
		
		if( "plain".equals(node.getNodeName()) ) {
			type = PLAIN;
			data = XMLUtils.getStringByXPath(node, "@name").trim();
		}
		else if( "urlparameter".equals(node.getNodeName()) ) {
			type = URL_PARAMETER;
			data = XMLUtils.getStringByXPath(node, "@name").trim();
		}
		else if( "urldirectory".equals(node.getNodeName()) ) {
			type = URL_DIRECTORY;
			data = XMLUtils.getStringByXPath(node, "@number").trim();
			Integer.parseInt(data); // Check, ob das Konvertieren ohne Probleme geht
		}
	}
	
	/**
	 * Liefert den sich aus dem Parameter und den aktuellen Kontext ergebenden Wert.
	 * @param context Der aktuelle Kontext
	 * @return Der Wert
	 * @throws Exception
	 */
	public String getValue(Context context) throws Exception {
		switch( type ) {
		case PLAIN:
			return data;

		case URL_PARAMETER:
			return context.getRequest().getParameter(data);

		case URL_DIRECTORY:
			String[] dirs = context.getRequest().getPath().substring(1).split("\\/");
			
			int number = Integer.parseInt(data);
			if( (Math.abs(number) > dirs.length) || (number == 0) ) {
				throw new Exception("Match-Rule: Directory index out of bounds");
			}
			if( number > 0 ) {
				return dirs[number-1];
			}

			return dirs[dirs.length+number];					
		}
		return null;
	}
}
