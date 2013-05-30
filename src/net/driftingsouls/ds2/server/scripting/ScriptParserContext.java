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
package net.driftingsouls.ds2.server.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Der Kontext eines ausgefuehrten Scripts. Enthaelt Register,
 * Befehlszeiger usw.
 * 
 * @author Christopher Jung
 *
 */
public class ScriptParserContext extends SimpleScriptContext {
	/**
	 * Konstruktor.
	 *
	 */
	public ScriptParserContext() {
		super();
		
		// TODO: Nicht schoen. Bitte besser machen
		if( (ContextMap.getContext() != null) && 
			(ContextMap.getContext().getRequest().getClass().getName().toUpperCase().contains("HTTP")) ) {
			setErrorWriter(new HtmlLogger());
		}
		else {
			setErrorWriter(new TextLogger());
		}
		
		setWriter(new StringWriter());
		engineScope.put("__INSTRUCTIONPOINTER", 0);
	}
	
	/**
	 * Gibt die Ausgabe der Scripte zurueck.
	 * @return Die Scriptausgabe
	 */
	public String getOutput() {
		return ((StringWriter)getWriter()).getBuffer().toString();
	}
	
	/**
	 * Fuegt den angegebenen Text zur Scriptausgabe hinzu.
	 * @param text der auszugebende Text
	 */
	public void out( String text ) {
		try {
			this.writer.append(text);
		}
		catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	private static class ExecData implements Serializable {
		// Achtung! Jede Veraenderung kann zu inkompatibilitaeten der serialisierten Daten fuehren!
		private static final long serialVersionUID = 1L;
		
		ExecData() {
			// EMPTY
		}
		Map<String,Object> register;
		int lastcommand;
	}
	
	/**
	 * Liesst den Kontext mit den Ausfuehrungsdaten aus dem Stream.
	 * @param data Der InputStream
	 * @return Der Kontext
	 * @throws Exception
	 */
	public static ScriptParserContext fromStream( InputStream data ) throws Exception {
		if( data == null ) {
			return null;
		}
		ObjectInputStream oinput = new ObjectInputStream(data);
		ExecData entry = (ExecData)oinput.readObject();
		
		ScriptParserContext context = new ScriptParserContext();
		context.engineScope.put("__INSTRUCTIONPOINTER", entry.lastcommand);
		
		if( (entry.register != null) && (entry.register.size() > 0) ) {
			context.engineScope.putAll(entry.register);
		}
		
		return context;
	}
	
	/**
	 * Schreibt die Ausfuehrungsdaten des ScriptParsers in den angegebenen Stream.
	 * @param context Die zu schreibenden Ausfuehrungsdaten
	 * @param out Der OutputStream
	 * @throws Exception
	 */
	public static void toStream(ScriptContext context, OutputStream out) throws Exception {
		if( out == null ) {
			return;
		}
		// Schiffsdaten nicht speichern
		Object ship = context.getBindings(ScriptContext.ENGINE_SCOPE).remove("_SHIP");
		
		ObjectOutputStream oout = new ObjectOutputStream(out);
		ExecData entry = new ExecData();
		entry.register = new HashMap<>(context.getBindings(ScriptContext.ENGINE_SCOPE));
		Integer ip = (Integer)context.getBindings(ScriptContext.ENGINE_SCOPE).get("__INSTRUCTIONPOINTER");
		if( ip != null ) {
			entry.lastcommand = ip;
		}
		oout.writeObject(entry);
		oout.close();
		
		context.getBindings(ScriptContext.ENGINE_SCOPE).put("_SHIP", ship);
	}
}
