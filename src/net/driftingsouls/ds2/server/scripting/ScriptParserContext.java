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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Der Kontext eines ausgefuehrten Scripts. Enthaelt Register,
 * Befehlszeiger usw.
 * 
 * @author Christopher Jung
 *
 */
public class ScriptParserContext {
	private Map<String,Object> register;
	private int lastcommand;
	private StringBuffer out = new StringBuffer();
	
	/**
	 * Konstruktor
	 *
	 */
	public ScriptParserContext() {
		this.register = new HashMap<String,Object>();
		this.lastcommand = 0;
	}
	
	/**
	 * Gibt das angegebene Register zurueck
	 * @param reg Das Register
	 * @return der Inhalt des Registers
	 */
	public String getRegister( String reg ) {
		return getRegisterObject(reg).toString();
	}
	
	/**
	 * Gibt das angegebene Register zurueck
	 * @param reg Das Register
	 * @return der Inhalt des Registers
	 */
	public Object getRegisterObject( String reg ) {
		if( reg.charAt(0) == '#' ) {
			reg = reg.substring(1);	
		}
		Object val = this.register.get(reg);
		if( val == null ) {
			return "";
		}
		return val;
	}
	
	/**
	 * Setzt das angegebene Register auf den angegebenen Wert
	 * @param reg Der Name des Registers
	 * @param data Der Wert
	 */
	public void setRegister( String reg, Object data ) {
		if( reg.charAt(0) == '#' ) {
			reg = reg.substring(1);	
		}
		this.register.put(reg, data);
	}
	
	/**
	 * Gibt die Liste aller Registernamen zurueck
	 * @return Die Liste aller Registernamen
	 */
	public Set<String> getRegisterList() {
		return this.register.keySet();
	}
	
	/**
	 * Gibt den Index des zuletzt ausgefuehrten Komamndos zurueck
	 * @return Der Indes des zuletzt ausgefuehrten Kommandos
	 */
	public int getLastCommand() {
		return this.lastcommand;
	}
	
	/**
	 * Setzt den Index des zuletzt ausgefuehrten Kommandos
	 * @param cmd Der Index des zuletzt ausgefuehrten Kommandos
	 */
	public void setLastCommand(int cmd) {
		this.lastcommand = cmd;
	}
	
	/**
	 * Gibt die Ausgabe der Scripte zurueck
	 * @return Die Scriptausgabe
	 */
	public String getOutput() {
		return out.toString();
	}
	
	/**
	 * Fuegt den angegebenen Text zur Scriptausgabe hinzu
	 * @param text der auszugebende Text
	 */
	public void out( String text ) {
		out.append(text);
	}
	
	/**
	 * Leert die Scriptausgabe
	 *
	 */
	public void clearOutput() {
		out.setLength(0);
	}
	
	private static class ExecData implements Serializable {
		private static final long serialVersionUID = 1L;
		
		ExecData() {
			// EMPTY
		}
		Map<String,Object> register;
		int lastcommand;
	}
	
	/**
	 * Fuegt Ausfuehrungsdaten (Register) aus dem angegebenen Kontext in den aktuellen Kontext ein
	 * @param data Der Kontext
	 * @throws Exception
	 */
	public void addContextData( ScriptParserContext data ) throws Exception {
		if( (data != null) && (data.register != null) && (data.register.size() > 0) ) {
			this.register.putAll(data.register);
		}
	}
	
	/**
	 * Liesst den Kontext mit den Ausfuehrungsdaten aus dem Stream
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
		context.lastcommand = entry.lastcommand;
		
		if( (entry.register != null) && (entry.register.size() > 0) ) {
			context.register.putAll(entry.register);
		}
		
		return context;
	}
	
	/**
	 * Schreibt die Ausfuehrungsdaten des ScriptParsers in den angegebenen Stream
	 * @param out Der OutputStream
	 * @throws Exception
	 */
	public void toStream(OutputStream out) throws Exception {
		if( out == null ) {
			return;
		}
		// Schiffsdaten nicht speichern
		Object ship = this.register.remove("_SHIP");
		
		ObjectOutputStream oout = new ObjectOutputStream(out);
		ExecData entry = new ExecData();
		entry.register = this.register;
		entry.lastcommand = lastcommand;
		oout.writeObject(entry);
		oout.close();
		
		this.register.put("_SHIP", ship);
	}
}
