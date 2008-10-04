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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextInstance;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;

/**
 * Kontextlokale Operationen
 * 
 * @author Christopher Jung
 */
@ContextInstance(ContextInstance.Scope.REQUEST)
public final class ContextCommon
{
	private Context context = null;

	/**
	 * Konstruktur - Wird vom Kontext aufgerufen
	 */
	public ContextCommon()
	{
		this.context = ContextMap.getContext();
	}

	/**
	 * Liefert den aktuellen Tick zurueck
	 * 
	 * @return Der aktuelle Tick
	 */
	public int getTick()
	{
		ConfigValue value = (ConfigValue)context.getDB().get(ConfigValue.class, "ticks");
		return Integer.valueOf(value.getValue());
	}

	private Map<String, ScriptEngine> scriptParsers = new HashMap<String, ScriptEngine>();

	/**
	 * Gibt eine Instanz einer ScriptEngine zurueck
	 * 
	 * @param name Der Name der ScriptEngine
	 * @return die ScriptEngine
	 */
	public ScriptEngine getScriptParser(String name)
	{
		if( !scriptParsers.containsKey(name) )
		{
			ScriptEngine parser = new ScriptEngineManager().getEngineByName(name);
			parser.setContext(new ScriptParserContext());
			scriptParsers.put(name, parser);
			return parser;
		}
		return scriptParsers.get(name);
	}
}
