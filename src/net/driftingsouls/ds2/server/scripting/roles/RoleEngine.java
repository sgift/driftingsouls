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
package net.driftingsouls.ds2.server.scripting.roles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import net.driftingsouls.ds2.server.scripting.roles.interpreter.IllegalRoleDefinitionException;
import net.driftingsouls.ds2.server.scripting.roles.interpreter.Interpreter;
import net.driftingsouls.ds2.server.scripting.roles.interpreter.RoleExecuter;
import net.driftingsouls.ds2.server.scripting.roles.parser.Parser;
import net.driftingsouls.ds2.server.scripting.roles.parser.ParsingException;
import net.driftingsouls.ds2.server.scripting.roles.parser.RoleDefinition;

/**
 * Die javax.script.ScriptEngine-Implementierung des Rollensystems.
 * @author Christopher Jung
 *
 */
class RoleEngine extends AbstractScriptEngine {
	private ScriptEngineFactory factory;
	
	/**
	 * Konstruktor.
	 * @param factory Die Fabrikklasse welche zum Erstellen der Instanz verwendet wurde
	 */
	public RoleEngine(ScriptEngineFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException {
		try {
			RoleDefinition roleDef = Parser.parse(script);
			RoleExecuter executer = Interpreter.executerFromDefinition(roleDef);
			executer.execute(context);
		}
		catch( IllegalRoleDefinitionException | ParsingException e ) {
			throw new ScriptException(e);
		}

		return null;
	}

	@Override
	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
		StringBuilder builder = new StringBuilder();
		BufferedReader bf = new BufferedReader(reader);
		String line;
		try {
			while( (line = bf.readLine()) != null ) {
				builder.append(line).append("\n");
			}
		}
		catch( IOException e ) {
			throw new ScriptException(e);
		}
		
		return eval(builder.toString(), context);
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return this.factory;
	}
}
