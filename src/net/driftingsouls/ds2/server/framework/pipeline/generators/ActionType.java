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
package net.driftingsouls.ds2.server.framework.pipeline.generators;

/**
 * Die verschiedenen Aufrufarten
 *
 */
public enum ActionType {
	/**
	 * Eine normale HTTP-Request mit HTML-Anwort
	 */
	DEFAULT("Action"),
	/**
	 * Eine Ajax-Request
	 */
	AJAX("AjaxAct");
	
	private String type;
	
	private ActionType(String type) {
		this.type = type;
	}
	
	/**
	 * Gibt den Postfix der Aktionsmethoden zurueck
	 * @return Der Postfix der Aktionsmethoden
	 */
	public String getActionExt() {
		return type;
	}
	
	/**
	 * Gibt den Aktionstyp zurueck, der zum Ausfuehrungsmodus gehoert
	 * @param execType Der Ausfuehrungsmodus
	 * @return Der Aktionstyp
	 */
	public static ActionType getByExecType(String execType) {
		for( int i=0; i < values().length; i++ ) {
			if( values()[i].name().equalsIgnoreCase(execType) ) {
				return values()[i];
			}
		}
		return null;
	}
}
