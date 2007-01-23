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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.comm.PM;
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
		TextLogger() {
			// EMPTY
		}
		
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
		HtmlLogger() {
			// EMPTY
		}
		
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
		NullLogger() {
			// EMPTY
		}
		
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
	
	/**
	 * Die verschiedenen, dem ScriptParser bekannten, Namespaces
	 *
	 */
	public enum NameSpace {
		/**
		 * Schiffsaktions-Scripte
		 */
		ACTION,
		/**
		 * Quest-Scripte
		 */
		QUEST
	}
	
	/**
	 * Die verschiedenen Argumenttypen
	 *
	 */
	public enum Args {
		/**
		 * Variabele Anzahl an Parametern
		 */
		VARIABLE(1),
		/**
		 * Einfache Daten
		 */
		PLAIN(2),
		/**
		 * Register, welche die Daten enthalten
		 */
		REG(4),
		/**
		 * Einfache Daten oder Register
		 */
		PLAIN_REG(PLAIN.value() | REG.value()),
		/**
		 * Einfache Daten variabler Anzahl
		 */
		PLAIN_VARIABLE(PLAIN.value() | VARIABLE.value()),
		/**
		 * Register variabler Anzahl
		 */
		REG_VARIABLE(REG.value() | VARIABLE.value()),
		/**
		 * Einfache Daten oder Register variabler Anzahl
		 */
		PLAIN_REG_VARIABLE(PLAIN.value() | REG.value() | VARIABLE.value());

		private int value = 0;
		Args(int value) {
			this.value = value;
		}
		
		/**
		 * Gibt die Integer-Repraesentation des Argument-Typs zurueck
		 * @return Die Integer-Repraesentation
		 */
		public int value() {
			return value;
		}
	}
	
	private static Map<String,Integer[]> JUMP_FUNCTIONS = new HashMap<String,Integer[]>();
	
	static {
		JUMP_FUNCTIONS.put("!JL", new Integer[] {-1});
		JUMP_FUNCTIONS.put("!JG", new Integer[] {1});
		JUMP_FUNCTIONS.put("!JE", new Integer[] {0});
		JUMP_FUNCTIONS.put("!JNE", new Integer[] {-1,1});
		JUMP_FUNCTIONS.put("!JLE", new Integer[] {-1,0});
		JUMP_FUNCTIONS.put("!JGE", new Integer[] {0,1});
	}
	
	private Logger logFunction = LOGGER_NULL;
	private SQLResultRow ship = null;
	private StringBuffer out = new StringBuffer();
	private NameSpace namespace = null;
	private Map<String,Object> register = null;
	private Map<String,SPFunction> funcregister = null;
	private Map<String,Args[]> funcargregister = null;
	private int lastcommand = 0;
	private List<String> addparameterlist = null;
	
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
		this.register = new HashMap<String,Object>();
		this.funcregister = new HashMap<String,SPFunction>();
		this.funcargregister = new HashMap<String,Args[]>();
		this.addparameterlist = new ArrayList<String>();
		
		new CommonFunctions().registerFunctions(this);
		
		if( namespace == NameSpace.ACTION ) {
			new ActionFunctions().registerFunctions(this);
		}
		else if( namespace == NameSpace.QUEST ) {
			new QuestFunctions().registerFunctions(this);
		}
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
	 */
	public void setShip( SQLResultRow ship ) {
		this.ship = ship;
	}
	
	/**
	 * Gibt das Schiff zurueck
	 * @return Das Schiff
	 */
	public SQLResultRow getShip() {
		return ship;
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
	 * Laedt Ausfuehrungsdaten aus dem angegebenen InputStream und fuegt sie dem ScriptParser hinzu
	 * @param data Der InputStream
	 * @throws Exception
	 */
	public void addExecutionData( InputStream data ) throws Exception {
		if( data != null ) {
			ObjectInputStream oinput = new ObjectInputStream(data);
			ExecData entry = (ExecData)oinput.readObject();
			if( (entry.register != null) && (entry.register.size() > 0) ) {
				this.register.putAll(entry.register);
			}
			oinput.close();
		}
	}
	
	/**
	 * Setzt die Ausfuehrungsdaten des ScriptParsers auf die im Stream enthaltenen Daten
	 * @param data Der InputStream
	 * @throws Exception
	 */
	public void setExecutionData( InputStream data ) throws Exception {
		if( data == null ) {
			return;
		}
		ObjectInputStream oinput = new ObjectInputStream(data);
		ExecData entry = (ExecData)oinput.readObject();
		
		this.lastcommand = entry.lastcommand;
		if( (entry.register != null) && (entry.register.size() > 0) ) {
			this.register.putAll(entry.register);
		}
	}
	
	/**
	 * Schreibt die Ausfuehrungsdaten des ScriptParsers in den angegebenen Stream
	 * @param out Der OutputStream
	 * @throws Exception
	 */
	public void writeExecutionData(OutputStream out) throws Exception {
		if( out == null ) {
			return;
		}
		ObjectOutputStream oout = new ObjectOutputStream(out);
		ExecData entry = new ExecData();
		entry.register = this.register;
		entry.lastcommand = lastcommand;
		oout.writeObject(entry);
		oout.close();
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
	 * Setzt die internen Daten des Scriptparsers auf den Ausgangsstatus zurueck
	 */
	public void cleanup() {
		this.lastcommand = 0;
		this.register.clear();
		this.out.setLength(0);
		this.ship = null;
		this.addparameterlist.clear();
	}
	
	private String processOperator( String current, char operator, String number ) {
		if( operator != 0 ) {			
			switch( operator ) {
			case '+': 
				current = Double.toString((Double.parseDouble(current) + Double.parseDouble(number)));
				break;
			case '-':
				current = Double.toString((Double.parseDouble(current) - Double.parseDouble(number)));
				break;
			case '*':
				current = Double.toString((Double.parseDouble(current) * Double.parseDouble(number)));
				break;
			case '/':
				current = Double.toString((Double.parseDouble(current) / Double.parseDouble(number)));
				break;
			case '%':
				current = Double.toString((Double.parseDouble(current) % Double.parseDouble(number)));
				break;
			case '^':
				current = Double.toString(Math.pow(Double.parseDouble(current), Double.parseDouble(number)));
				break;
			case '.':
				current += number;
				break;
			}
		}
		else {
			current = number;
		}
		return current;
	}
	
	private class TermElement {
		char operator;
		String current;
		
		TermElement(char operator, String current) {
			this.operator=operator;
			this.current=current;
		}
	}

	private static  Set<Character> validops = new HashSet<Character>();
	static {
		validops.add('+');
		validops.add('-');
		validops.add('/');
		validops.add('*');
		validops.add('%');
		validops.add('^');
		validops.add('.');
	}
	
	private static Set<Character> validbrackets = new HashSet<Character>();
	static {
		validbrackets.add('(');
		validbrackets.add(')');
	}
	
	private static Set<Character> validnumbers = new HashSet<Character>();
	static {
		validnumbers.add('0');
		validnumbers.add('1');
		validnumbers.add('2');
		validnumbers.add('3');
		validnumbers.add('4');
		validnumbers.add('5');
		validnumbers.add('6');
		validnumbers.add('7');
		validnumbers.add('8');
		validnumbers.add('9');
	}
	
	private static Set<Character> regends = new HashSet<Character>();
	static {
		regends.add('+');
		regends.add('-');
		regends.add('/');
		regends.add('*');
		regends.add('%');
		regends.add('^');
		regends.add('.');
		regends.add(' ');
	}
	
	/**
	 * Fuehrt einen Term aus und gibt das Ergebnis zurueck
	 * @param term Der Term
	 * @return Das Ergebnis
	 */
	public String evalTerm( String term ) {
		int index = 0;
		Stack<TermElement> stack = new Stack<TermElement>();
		
		String current = "";
		char operator = 0;
		
		while( index < term.length() ) {
			if( validnumbers.contains(term.charAt(index)) ) {
				String number = "";
				
				while( (index < term.length()) && (validnumbers.contains(term.charAt(0)) || ((term.charAt(index) == '.') && (number.length() > 0) ) ) ) {
					number += term.charAt(index++);
				}

				if( (number.length() > 0) && (number.charAt(number.length()-1) == '.') ) {
					index--;
					number = number.substring(0, number.length()-1);	
				}

				current = processOperator(current, operator, number);
				operator = 0;
			}
			else if( validops.contains(term.charAt(index)) ) {
				operator = term.charAt(index++);
			}
			else if( validbrackets.contains(term.charAt(index)) ) {
				if( term.charAt(index) == '(' ) {
					stack.push(new TermElement(operator,current));
					current = "";
					operator = 0;
				}	
				else if( term.charAt(index) == ')' ) {
					TermElement val = stack.pop();
					
					current = processOperator(val.current, val.operator, current);
					operator = 0;
				}
				index++;
			}
			else if( term.charAt(index) == '#' ) {
				String reg = "";
				
				while( (index < term.length()) && !regends.contains(term.charAt(index)) ) {
					reg += term.charAt(index++);
				}
				
				String val = this.getRegister(reg);
				
				current = processOperator(current, operator, val);
				operator = 0;
			}
			else if( term.charAt(index) == '"' ) {
				index++;
				String str = "";
				
				while( (index < term.length()) && ((term.charAt(index) != '"') || (term.charAt(index-1) == '\\')) ) {
					str += term.charAt(index++);
				}

				index++;
				current = processOperator(current, operator, str);
			}
			else if( term.charAt(index) == ' ' ) {
				index++;	
			}
			else {
				this.log("Ungueltiges Zeichen '"+term.charAt(index)+"' an Position "+index+" im Term '"+term+"' gefunden\n");
				return "0";	
			}
		}
		return current;
	}
	
	/**
	 * Gibt den Parameter mit der angegebenen Nummer zurueck.
	 * Sollte kein Parameter mit der Nummer existiert wird <code>null</code>
	 * zurueckgegeben.
	 * @param number Die Nummer des Parameters
	 * @return Der Inhalt oder <code>null</code>
	 */
	public String getParameter( int number ) {
		if( (number > -1) && (addparameterlist.size() > number) ) { 
			return this.addparameterlist.get(number);
		}
		return null;
	}
	
	/**
	 * Fuehrt das angegebene Script aus
	 * @param db Eine offene DB-Verbindung
	 * @param script das Script
	 */
	public void executeScript( Database db, String script ) {
		executeScript(db, script, "");
	}
	
	/**
	 * Fuehrt das angegebene Script aus
	 * @param db Eine offene DB-Verbindung
	 * @param script Das Script
	 * @param parameter Ausfuehrungsparameter
	 */
	public void executeScript( Database db, String script, String parameter ) {
		this.startLogging();
		
		this.out.setLength(0);
		
		this.addparameterlist.clear();

		SQLResultRow ship = this.getShip();
		
		//Auswertung		
		int restartcount = 0;
		
		int lastcommand = this.lastcommand;
		
		this.log("Gesetzte Register:\n");
		for( String reg : this.register.keySet() ) {
			this.log("#"+reg+" => "+this.register.get(reg)+"\n");	
		}
		this.log("\n");
		
		// Wurde die Ausfuehrung des Scripts beendet?
		if( (this.namespace == NameSpace.ACTION) && 
			(lastcommand == -1) && (parameter.length() == 0) ) {
			this.log("Ausfuehrung des scripts bereits beendet\n");
			this.stopLogging();
			return;	
		}
		
		// Script einlesen und untersuchen
		script = StringUtils.replace(script,"\r\n","\n");
		String[] lines = StringUtils.split(script, '\n');
	
		List<String[]> commands = new ArrayList<String[]>();
		Map<String,Integer> parameterlist = new HashMap<String,Integer>();
		
		// Gueltige Parameter beim Scriptaufruf (gilt nicht fuer jumps! und den 0-Parameter)
		// werden gesetzt via §parameter param1 param2 param3 ...
		Set<String> validparameters = new HashSet<String>();
		
		// Interne immer gueltige Parameter beim Scriptaufruf
		Set<String> validInternalParams = new HashSet<String>();
		validInternalParams.add("0");
		
		// limitexeccount bestimmt wieviele befehle maximal abgearbeitet werden duerfen 
		// int limitexeccount = -1; <--- unbegrenzt
		int limitexeccount = 5000;
		
		int index = 0;
		for(int i=0; i < lines.length; i++ ) {
			String line = lines[i].trim();
			if( line.length() == 0 ) {
				continue;
			}
			
			if( line.charAt(0) == '!' ) { 
				String[] acommand = StringUtils.split(line, ' ');
				commands.add(index, acommand);
				index++;
			}
			else if( (line.charAt(0) == '#') && (line.indexOf('=') > -1) ) {
				int pos = line.indexOf('=');
				String reg = line.substring(0, pos);
				String term = line.substring(pos+1);
				commands.add(index, new String[] {reg.trim(), term.trim()});
				index++;
			}
			else if( line.charAt(0) == ':' ) {
				parameterlist.put(line, index);
			}
			else if( line.charAt(0) == '§' ) {
				String[] acommand = StringUtils.split(line, ' ');
				if( acommand[0].equals("§parameter") ) {
					for( int j=1; j < acommand.length; j++ ) {
						validparameters.add(acommand[j]);
					}
				}
				else if( acommand[0].equals("§limitexec") ) {
					limitexeccount = Integer.parseInt( acommand[1] );	
					this.log("+++ Setzte max. Befehle auf "+limitexeccount+"\n\n");
				}
			}
		}
		
		String[] addparameterlist = StringUtils.split(parameter, ':');
		if( addparameterlist.length > 0 ) {
			parameter = addparameterlist[0];
		}
		else {
			parameter = "";
		}

		for( int i=1; i < addparameterlist.length; i++ ) {
			this.addparameterlist.add(addparameterlist[i]);
		}
		
		// Ggf. Parameter behandeln
		if( parameter.length() > 0 ) {
			
			if( parameter.equals("-1") ) {
				this.out.setLength(0);
				this.out.append(this.getRegister("_OUTPUT"));
				this.stopLogging();
				return;
			}
			else if( !validInternalParams.contains(parameter) && !parameterlist.containsKey(':'+parameter) ) {
				this.log("+++ Ungueltiger Parameter >"+parameter+"< - benutze >0<\n");
				parameter = "0";	
			}	
			else if( !validInternalParams.contains(parameter) && (validparameters.size() > 0) && !validparameters.contains(parameter) ) {
				this.log("+++ Parameter >"+parameter+"< gesperrt - benutze >0<\n");
				parameter = "0";	
			}
			lastcommand = parameterlist.get(':'+parameter);
		}
		
		// Und los gehts!
		while( true ) {
			if( lastcommand >= commands.size() ) {
				if( (this.ship != null) && (this.namespace == NameSpace.ACTION) ) {
					PM.send(ContextMap.getContext(), -1, ship.getInt("owner"), "Script beendet", "[Scriptsystem:"+ship.getInt("id")+"]\nDie "+ship.getString("name")+" hat ihre Befehle abgearbeitet!");
				}
				this.log("+++ Ausfuehrung beendet\n\n");
				lastcommand = -1;
				
				break;
			}
			

			if( commands.get(lastcommand)[0].charAt(0) == '#' ) {
				String[] cmd = commands.get(lastcommand);
				this.log("* Berechne Ausdruck '"+cmd[1]+"' nach "+cmd[0]+"\n");
				
				this.setRegister(cmd[0],this.evalTerm(cmd[1]));
				
				lastcommand++;
				if( limitexeccount > 0 ) {
					limitexeccount--;
					if( limitexeccount == 0 ) {
						this.log("+++ max. Befehle erreicht\n\n");
						break;
					}
				}
				
				continue;
			}
			
			String funcname = commands.get(lastcommand)[0].toUpperCase();
			
			if( this.funcregister.containsKey(funcname) ) {
				String[] cmd = commands.get(lastcommand);
				
				SPFunction func = this.funcregister.get(funcname);
				
				Args[] args = this.funcargregister.get(funcname);
				if( (cmd.length-1 > args.length) && ((args[args.length-1].ordinal() & Args.VARIABLE.value()) != 0) ) {
					Args[] args2 = new Args[cmd.length-1];
					System.arraycopy(args, 0, args2, 0, args.length);
					
					for( int i=args.length; i < cmd.length-1; i++ ) {
						args2[i] = args[cmd.length-1];
					}
					
					args = args2;
				}

				for( int i=0; i < Math.min(args.length,cmd.length-1); i++ ) {
					String cmdParam = cmd[i+1];
						
					if( (cmdParam.charAt(0) == '#') && ((args[i].ordinal() & Args.REG.ordinal()) > 0) ) {
						cmdParam = this.getRegister(cmdParam);	
					}
						
					cmd[i+1] = cmdParam;	
				}
				
				this.log("*COMMAND: "+funcname+"\n");
				
				// ich HASSE eval(...), aber hier gehts leider nicht ohne...
				boolean[] result = func.execute(db, this, cmd);
				
				lastcommand += (result[1] ? 1 : 0);
				if( !result[0] ) {
					break;
				}
				if( limitexeccount > 0 ) {
					limitexeccount--;
					if( limitexeccount == 0 ) {
						this.log("+++ max. Befehle erreicht\n\n");
						break;
					}
				}
			}
			else if( funcname.equals("!RESTART") ) {
				this.log("*COMMAND: !RESTART\n");
				restartcount++;
				lastcommand = 0;
				if( restartcount > 3 ) {
					this.log("Maximale Anzahl an !RESTART-Befehlen in einer Ausfuehrung erreicht\n\n");
					break;
				}
				this.log("\n");
				
				if( limitexeccount > 0 ) {
					limitexeccount--;
					if( limitexeccount == 0 ) {
						this.log("+++ max. Befehle erreicht\n\n");
						break;
					}
				}
			} 
			else if( funcname.equals("!QUIT") ) {
				this.log("*COMMAND: !QUIT\n");
				lastcommand = -1;
				
				this.log("+++ Ausfuehrung beendet\n\n");
				break;
			} 
			else if( funcname.equals("!PAUSE") ) {
				this.log("*COMMAND: !PAUSE\n");				
				this.log("+++ Ausfuehrung vorerst angehalten\n\n");
				break;
			} 
			else if( funcname.startsWith("!J") && JUMP_FUNCTIONS.containsKey(funcname) ) {
				this.log("*COMMAND: !JL\n");
				boolean ok = false;
				
				int compResult = new Double(this.getRegister("cmp")).compareTo(0d);
				
				Integer[] vals = JUMP_FUNCTIONS.get(funcname);
				for( int i=0; i < vals.length; i++ ) {
					if( vals[i] == compResult ) {
						ok = true;
						break;
					}
				}
				if( ok ) {
					this.log("Marke: "+commands.get(lastcommand)[1]+"\n");
					lastcommand = parameterlist.get(":"+commands.get(lastcommand)[1]);
				}	
				else {
					lastcommand++;
				}
				
				if( limitexeccount > 0 ) {
					limitexeccount--;
					if( limitexeccount == 0 ) {
						this.log("+++ max. Befehle erreicht\n\n");
						break;
					}
				}
			} 
			else if( funcname.equals("!JUMP") ) {
				this.log("*COMMAND: !JUMP\n");
				String jumptarget = commands.get(lastcommand)[1];
				if( jumptarget.charAt(0) == '#' ) {
					jumptarget = this.getRegister(jumptarget);
				}
				this.log("Marke: "+jumptarget+"\n");
				
				if( !parameterlist.containsKey(":"+jumptarget) ) {
					this.log("WARNUNG: Unbekannte Sprungmarke $jumptarget\n");
				}
				lastcommand = parameterlist.get(":"+jumptarget);
				
				if( limitexeccount > 0 ) {
					limitexeccount--;
					if( limitexeccount == 0 ) {
						this.log("+++ max. Befehle erreicht\n\n");
						break;
					}
				}
			} 
			else if( funcname.equals("!DUMP") ) {
				this.log("*COMMAND: !DUMP\n");
				String dump = commands.get(lastcommand)[1];
				
				if( dump.equals("register") ) {
					this.log("################# register ###################\n");
					for( String reg : this.register.keySet() ) {
						this.log(reg+" ("+this.register.get(reg)+"), ");
					}
					this.log("\n\n");
				}
				else if( dump.equals("jumpaddrs") ) {
					this.log("################# jumpaddrs ###################\n");
					for( String jumpname : parameterlist.keySet() ) {
						this.log(jumpname+" ("+parameterlist.get(jumpname)+"), ");
					}
					this.log("\n\n");
				}
				else if( dump.equals("code") ) {
					this.log("################# commands ###################\n");
					for( int i=0; i < commands.size(); i++ ) {
						this.log(i+": "+Common.implode(" ",commands.get(i))+"\n");
					}
					this.log("\n\n");
				}
				
				this.log("############### END OF DUMP #################\n");
				
				lastcommand++;
				
				if( limitexeccount > 0 ) {
					limitexeccount--;
					if( limitexeccount == 0 ) {
						this.log("+++ max. Befehle erreicht\n\n");
						break;
					}
				}
			} 
			else {
				this.log("*UNKNOWN COMMAND >"+funcname+"<\n");
				this.log("*CURRENT COMMAND POINTER: "+lastcommand+"\n\n");
				this.log("################# jumpaddrs ###################\n");
				for( String jumpname : parameterlist.keySet() ) {
					this.log(jumpname+" ("+parameterlist.get(jumpname)+"), ");
				}
				this.log("\n\n");
				this.log("################# register ###################\n");
				for( String reg : this.register.keySet() ) {
					this.log(reg+" ("+this.register.get(reg)+"), ");
				}
				this.log("\n\n");
				this.log("################# commands ###################\n");
				for( int i=0; i < commands.size(); i++ ) {
					this.log(i+": "+Common.implode(" ",commands.get(i))+"\n");
				}
				this.log("\n\n");
				break;
			}
		}
		this.lastcommand = lastcommand;
		
		this.stopLogging();
	}
}
