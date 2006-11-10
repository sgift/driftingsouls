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
package net.driftingsouls.ds2.server;

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextInstance;
import net.driftingsouls.ds2.server.scripting.ScriptParser;

/**
 * Kontextlokale Operationen
 * @author Christopher Jung
 */
@ContextInstance(ContextInstance.Type.SINGLETON)
public class ContextCommon {
	private Context context = null;
	
	/**
	 * Konstruktur - Wird vom Kontext aufgerufen
	 * @param context Der Kontext, an den die Instanz gebunden werden soll
	 */
	public ContextCommon(Context context) {
		this.context = context;
	}

	private int tick = -1;
	
	/**
	 * Liefert den aktuellen Tick zurueck
	 * 
	 * @return Der aktuelle Tick 
	 */
	public int getTick() {
		if( tick == -1 ) {
			tick = context.getDatabase().first("SELECT ticks FROM config").getInt("ticks");
		}	
		return tick;
	}
	
	private Map<ScriptParser.NameSpace,ScriptParser> scriptParsers = new HashMap<ScriptParser.NameSpace,ScriptParser>();
	
	/**
	 * Gibt eine Instanz des ScriptParsers fuer den angegebenen Namespace zurueck
	 * @param namespace Der Namespace des ScriptParsers
	 * @return der ScriptParser
	 */
	public ScriptParser getScriptParser( ScriptParser.NameSpace namespace ) {
		if( !scriptParsers.containsKey(namespace) ) {
			ScriptParser parser = new ScriptParser(namespace);
			scriptParsers.put(namespace, parser);
			return parser;
		}
		return scriptParsers.get(namespace);
	}
}
