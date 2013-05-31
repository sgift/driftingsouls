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
package net.driftingsouls.ds2.server.scripting.dsscript;

import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Basisklasse fuer DSScript-Factories.
 * @author Christopher Jung
 *
 */
public abstract class DSScriptFactory implements ScriptEngineFactory {
	@Override
	public String getEngineName() {
		return "Drifting Souls ScriptParser Engine";
	}

	@Override
	public String getEngineVersion() {
		return "1.0";
	}

	@Override
	public List<String> getExtensions() {
		return Arrays.asList("dsscript");
	}

	@Override
	public String getLanguageName() {
		return "DSScript";
	}

	@Override
	public String getLanguageVersion() {
		return "1.0";
	}

	@Override
	public String getMethodCallSyntax(String obj, String m, String... args) {
		throw new UnsupportedOperationException("Methodenaufruf wird nicht unterstuetzt");
	}

	@Override
	public List<String> getMimeTypes() {
		return Arrays.asList("text/plain", "text/dsscript");
	}

	@Override
	public List<String> getNames() {
		return Arrays.asList("dsscript", "DSScript", "scriptparser", "ScriptParser");
	}

	@Override
	public String getOutputStatement(String toDisplay) {
		throw new UnsupportedOperationException("Methodenaufruf wird nicht unterstuetzt");
	}

	@Override
	public Object getParameter(String key) {
		if( ScriptEngine.ENGINE.equals(key) ) {
			return getEngineName();
		}
		if( ScriptEngine.ENGINE_VERSION.equals(key) ) {
			return getEngineVersion();
		}
		if( ScriptEngine.NAME.equals(key) ) {
			return "DSScript";
		}
		if( ScriptEngine.LANGUAGE.equals(key) ) {
			return getLanguageName();
		}
		if( ScriptEngine.LANGUAGE_VERSION.equals(key) ) {
			return getLanguageVersion();
		}
		
		return null;
	}

	@Override
	public String getProgram(String... statements) {
		StringBuilder builder = new StringBuilder();
		for (String statement : statements)
		{
			builder.append(statement).append("\n");
		}
		return builder.toString();
	}
}
