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
package net.driftingsouls.ds2.server.scripting;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Der ScriptParser
 * @author Christopher Jung
 *
 */
public class ScriptParser {
	
	/**
	 * Interface fuer ScriptParser-Logger
	 * @author Christopher Jung
	 *
	 */
	public static interface Logger {
		/**
		 * Startet das Logging (Log-Header)
		 */
		public void start();
		/**
		 * Loggt einen (ein- oder mehrzeiligen) Text
		 * @param txt der zu loggende Text
		 */
		public void log(String txt);
		/**
		 * Beendet das Logging (Log-Footer
		 */
		public void stop();
	}
	
	private static class TextLogger implements Logger {
		public void start() {
			System.out.println("###################Scriptparser [Debug]###################\n");
		}
		
		public void log(String txt) {
			System.out.println(txt);
		}
		
		public void stop() {
			System.out.println("#########################ENDE#############################\n");
		}
	}
	
	private static class HtmlLogger implements Logger {
		public void start() {
			Context context = ContextMap.getContext();
			if( context == null ) {
				return;
			}
			StringBuffer out = context.getResponse().getContent();
			out.append(Common.tableBegin(500,"left"));
			out.append("<div align=\"center\">Scriptparser [Debug]</div><br />");
			out.append("<span style=\"font-size:11px\">\n");	
		}
		
		public void log(String txt) {
			Context context = ContextMap.getContext();
			if( context == null ) {
				return;
			}
			StringBuffer out = context.getResponse().getContent();
			out.append(StringUtils.replace(txt, "\n", "<br />"));
		}
		
		public void stop() {
			Context context = ContextMap.getContext();
			if( context == null ) {
				return;
			}
			StringBuffer out = context.getResponse().getContent();
			out.append("</span>\n");
			out.append(Common.tableEnd());
			out.append("<br />\n");
		}
	}
	
	private static class NullLogger implements Logger {
		public void log(String txt) {
			// EMPTY
		}

		public void start() {
			// EMPTY
		}

		public void stop() {
			// EMPTY
		}
	}
	
	/**
	 * Loggt den ScriptParser-Output als Text in der Konsole
	 */
	public static Logger LOGGER_TEXT = new TextLogger();
	/**
	 * Loggt den ScriptParser-Output als HTML in den Ausgabe-Puffer
	 */
	public static Logger LOGGER_HTML = new HtmlLogger();
	
	/**
	 * Loggt den ScriptParser-Output nicht
	 */
	public static Logger LOGGER_NULL = new NullLogger();
	
	public enum NameSpace {
		ACTION,
		QUEST
	}
	
	public enum Args {
		VARIABLE(1),
		PLAIN(2),
		REG(4),
		PLAIN_REG(PLAIN.value() | REG.value()),
		PLAIN_VARIABLE(PLAIN.value() | VARIABLE.value()),
		REG_VARIABLE(REG.value() | VARIABLE.value()),
		PLAIN_REG_VARIABLE(PLAIN.value() | REG.value() | VARIABLE.value());

		private int value = 0;
		Args(int value) {
			this.value = value;
		}
		
		public int value() {
			return value;
		}
	}
	
	private Logger logFunction = LOGGER_NULL;
	private SQLResultRow ship = null;
	private StringBuffer out = new StringBuffer();
	private NameSpace namespace = null;
	private Map<String,String> register = null;
	private Map<String,SPFunction> funcregister = null;
	private Map<String,Args[]> funcargregister = null;
	
	/**
	 * Konstruktor
	 * @param namespace Der Namespace, in dem der ScriptParser arbeiten soll
	 */
	public ScriptParser(NameSpace namespace) {
		// TODO: Nicht schoen. Bitte besser machen
		if( (ContextMap.getContext() != null) && 
			(ContextMap.getContext().getRequest().getClass().getName().toUpperCase().contains("HTTP")) ) {
			setLogFunction(LOGGER_HTML);
		}
		else {
			setLogFunction(LOGGER_TEXT);
		}		
		
		this.namespace = namespace;
		this.register = new HashMap<String,String>();
		this.funcregister = new HashMap<String,SPFunction>();
		this.funcargregister = new HashMap<String,Args[]>();
	}

	/**
	 * Registriert eine ScriptParser-Funktion
	 * @param command Der Name der Script-Parser-Funktion
	 * @param function Die Funktionsimplementierung
	 * @param args Die Parameterstruktur
	 * @return <code>true</code>, falls das Kommando neu registriert wurde und noch nicht registriert war
	 */
	public boolean registerCommand( String command, SPFunction function, Args ... args ) {
		command = "!"+command;
		if( !funcregister.containsKey(command) ) {
			funcregister.put(command, function);
			funcargregister.put(command, args);
			
			return true;
		}
		return false;
	}
	
	/**
	 * Setzt die Logger-Klasse
	 * @param func Die neue Logger-Klasse
	 */
	public void setLogFunction( Logger func ) {
		logFunction = func;
	}
	
	/**
	 * Loggt den angegebenen String
	 * @param txt Der zu loggende String
	 */
	public void log( String txt ) {
		logFunction.log(txt);
	}
	
	/**
	 * Startet das Logging
	 *
	 */
	public void startLogging() {
		logFunction.start();
	}
	
	/**
	 * Stoppt das Logging
	 *
	 */
	public void stopLogging() {
		logFunction.stop();
	}
	
	/**
	 * Setzt das Schiff
	 * @param ship Das Schiff
	 * @deprecated Bitte das register <code>#SOURCESHIP</code>
	 */
	@Deprecated
	public void setShip( SQLResultRow ship ) {
		this.ship = ship;
	}
	
	/**
	 * Gibt das Schiff zurueck
	 * @return Das Schiff
	 * @deprecated Bitte das register <code>#SOURCESHIP</code>
	 */
	@Deprecated
	public SQLResultRow getShip() {
		return ship;
	}
	
	public void addExecutionData( String data ) {
		// TODO
		Common.stub();
	}
	
	public void setExecutionData( String data ) {
		// TODO
		Common.stub();
	}
	
	public String getExecutionData() {
		throw new RuntimeException("STUB");
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
	 * Gibt das angegebene Register zurueck
	 * @param reg Das Register
	 * @return der Inhalt des Registers
	 */
	public String getRegister( String reg ) {
		if( reg.charAt(0) == '#' ) {
			reg = reg.substring(1);	
		}
		String val = this.register.get(reg);
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
	public void setRegister( String reg, String data ) {
		if( reg.charAt(0) == '#' ) {
			reg = reg.substring(1);	
		}
		this.register.put(reg, data);
	}
	
	public void cleanup() {
		throw new RuntimeException("STUB");
	}
	
	public String evalTerm( String term ) {
		throw new RuntimeException("STUB");
	}
	
	public String getParameter( int number ) {
		throw new RuntimeException("STUB");
	}
	
	public void executeScript( Database db, String script ) {
		executeScript(db, script, "");
	}
	
	public void executeScript( Database db, String script, String parameter ) {
		// TODO
		Common.stub();
	}
}
