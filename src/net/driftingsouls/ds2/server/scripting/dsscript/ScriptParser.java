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
package net.driftingsouls.ds2.server.scripting.dsscript;

import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.ships.Ship;
import org.apache.commons.lang.StringUtils;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Der ScriptParser.
 * @author Christopher Jung
 *
 */
public class ScriptParser extends AbstractScriptEngine {
	/**
	 * Die verschiedenen Argumenttypen.
	 *
	 */
	public enum Args {
		/**
		 * Variabele Anzahl an Parametern.
		 */
		VARIABLE(1),
		/**
		 * Einfache Daten.
		 */
		PLAIN(2),
		/**
		 * Register, welche die Daten enthalten.
		 */
		REG(4),
		/**
		 * Einfache Daten oder Register.
		 */
		PLAIN_REG(PLAIN.value() | REG.value()),
		/**
		 * Einfache Daten variabler Anzahl.
		 */
		PLAIN_VARIABLE(PLAIN.value() | VARIABLE.value()),
		/**
		 * Register variabler Anzahl.
		 */
		REG_VARIABLE(REG.value() | VARIABLE.value()),
		/**
		 * Einfache Daten oder Register variabler Anzahl.
		 */
		PLAIN_REG_VARIABLE(PLAIN.value() | REG.value() | VARIABLE.value());

		private int value = 0;
		Args(int value) {
			this.value = value;
		}
		
		/**
		 * Gibt die Integer-Repraesentation des Argument-Typs zurueck.
		 * @return Die Integer-Repraesentation
		 */
		public int value() {
			return value;
		}
	}
	
	private static Map<String,Integer[]> JUMP_FUNCTIONS = new HashMap<>();
	
	static {
		JUMP_FUNCTIONS.put("!JL", new Integer[] {-1});
		JUMP_FUNCTIONS.put("!JG", new Integer[] {1});
		JUMP_FUNCTIONS.put("!JE", new Integer[] {0});
		JUMP_FUNCTIONS.put("!JNE", new Integer[] {-1,1});
		JUMP_FUNCTIONS.put("!JLE", new Integer[] {-1,0});
		JUMP_FUNCTIONS.put("!JGE", new Integer[] {0,1});
	}

	private Map<String,SPFunction> funcregister = null;
	private Map<String,Args[]> funcargregister = null;
	private ScriptEngineFactory factory;
	
	/**
	 * Konstruktor.
	 * @param factory Die Factory, welche die Instanz erzeugt hat
	 */
	public ScriptParser(ScriptEngineFactory factory) {
		this.factory = factory;
		
		this.funcregister = new HashMap<>();
		this.funcargregister = new HashMap<>();
		
		new CommonFunctions().registerFunctions(this);
		new ActionFunctions().registerFunctions(this);
	}

	/**
	 * Registriert eine ScriptParser-Funktion.
	 * @param command Der Name der Script-Parser-Funktion
	 * @param function Die Funktionsimplementierung
	 * @param args Die Parameterstruktur
	 * @return <code>true</code>, falls das Kommando neu registriert wurde und noch nicht registriert war
	 */
	protected boolean registerCommand( String command, SPFunction function, Args ... args ) {
		command = "!"+command;
		if( !funcregister.containsKey(command) ) {
			funcregister.put(command, function);
			funcargregister.put(command, args);
			
			return true;
		}
		return false;
	}
	
	/**
	 * Loggt den angegebenen String.
	 * @param txt Der zu loggende String
	 */
	protected void log( String txt ) {
		try {
			getContext().getErrorWriter().append(txt);
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Setzt das Schiff.
	 * @param ship Das Schiff
	 */
	protected void setShip( Ship ship ) {
		context.setAttribute("_SHIP", ship, ScriptContext.ENGINE_SCOPE);
	}
	
	/**
	 * Gibt das Schiff zurueck.
	 * @return Das Schiff
	 */
	protected Ship getShip() {
		return (Ship)context.getAttribute("_SHIP");
	}
		
	/**
	 * Gibt das angegebene Register zurueck.
	 * @param reg Das Register
	 * @return der Inhalt des Registers
	 */
	protected String getRegister( String reg ) {
		if( reg.charAt(0) == '#' ) {
			reg = reg.substring(1);	
		}
		
		Object val = context.getBindings(ScriptContext.ENGINE_SCOPE).get(reg);
		
		if( val == null ) {
			return "";
		}
		return val.toString();
	}
	
	/**
	 * Gibt das angegebene Register zurueck.
	 * @param reg Das Register
	 * @return der Inhalt des Registers
	 */
	protected Object getRegisterObject( String reg ) {
		if( reg.charAt(0) == '#' ) {
			reg = reg.substring(1);	
		}
		
		return context.getBindings(ScriptContext.ENGINE_SCOPE).get(reg);
	}
	
	/**
	 * Setzt das angegebene Register auf den angegebenen Wert.
	 * @param reg Der Name des Registers
	 * @param data Der Wert
	 */
	protected void setRegister( String reg, Object data ) {
		if( reg.charAt(0) == '#' ) {
			reg = reg.substring(1);	
		}
		
		context.getBindings(ScriptContext.ENGINE_SCOPE).put(reg, data);
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
	
	private static class TermElement {
		char operator;
		String current;
		
		TermElement(char operator, String current) {
			this.operator=operator;
			this.current=current;
		}
	}

	private static  Set<Character> validops = new HashSet<>();
	static {
		validops.add('+');
		validops.add('-');
		validops.add('/');
		validops.add('*');
		validops.add('%');
		validops.add('^');
		validops.add('.');
	}
	
	private static Set<Character> validbrackets = new HashSet<>();
	static {
		validbrackets.add('(');
		validbrackets.add(')');
	}
	
	private static Set<Character> validnumbers = new HashSet<>();
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
	
	private static Set<Character> regends = new HashSet<>();
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
	 * Fuehrt einen Term aus und gibt das Ergebnis zurueck.
	 * @param term Der Term
	 * @return Das Ergebnis
	 */
	private String evalTerm( String term ) {
		int index = 0;
		Stack<TermElement> stack = new Stack<>();
		
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
	protected String getParameter( int number ) {
		List<?> addparameterlist = (List<?>)getContext().getAttribute("_ADDPARAMETERLIST");
		if( (number > -1) && (addparameterlist.size() > number) ) { 
			return (String)addparameterlist.get(number);
		}
		return null;
	}
	
	/**
	 * Setzt den zur Ausfuehrung von Scripten zu verwendenden Kontext.
	 * @param context Der Kontext
	 */
	public void setContext(ScriptParserContext context) {
		this.context = context;
	}

	private int getLastCommand() {
		Integer attr = (Integer)this.context.getAttribute("__INSTRUCTIONPOINTER", ScriptContext.ENGINE_SCOPE);
		
		if( attr == null ) {
			return 0;
		}
	
		return attr;
	}
	
	private void setLastCommand(int ip) {
		this.context.setAttribute("__INSTRUCTIONPOINTER", ip, ScriptContext.ENGINE_SCOPE);
	}
	
	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException {
		ScriptContext oldContext = this.context;
		this.context = context;
		
		// Auswertung
		int restartcount = 0;
		
		String parameter = (String)context.getAttribute("_PARAMETERS");
		if( parameter == null ) {
			parameter = "";
		}
		
		// Wurde die Ausfuehrung des Scripts beendet?
		if( (getLastCommand() == -1) && (parameter.length() == 0) ) {
			this.log("Ausfuehrung des scripts bereits beendet\n");
			
			// TODO: Sollte nicht hier sein...
			try {
				this.context.getErrorWriter().close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
			
			this.context = oldContext;
			
			return null;	
		}
		
		// Script einlesen und untersuchen
		script = StringUtils.replace(script,"\r\n","\n");
		String[] lines = StringUtils.split(script, '\n');
	
		List<String[]> commands = new ArrayList<>();
		Map<String,Integer> parameterlist = new HashMap<>();
		
		// Gueltige Parameter beim Scriptaufruf (gilt nicht fuer jumps! und den 0-Parameter)
		// werden gesetzt via §parameter param1 param2 param3 ...
		Set<String> validparameters = new HashSet<>();
		
		// Interne immer gueltige Parameter beim Scriptaufruf
		Set<String> validInternalParams = new HashSet<>();
		validInternalParams.add("0");
		
		// limitexeccount bestimmt wieviele befehle maximal abgearbeitet werden duerfen 
		// int limitexeccount = -1; <--- unbegrenzt
		int limitexeccount = 5000;
		
		int index = 0;
		for (String line1 : lines)
		{
			String line = line1.trim();
			if (line.length() == 0)
			{
				continue;
			}

			if (line.charAt(0) == '!')
			{
				String[] acommand = StringUtils.split(line, ' ');
				commands.add(index, acommand);
				index++;
			}
			else if ((line.charAt(0) == '#') && (line.indexOf('=') > -1))
			{
				int pos = line.indexOf('=');
				String reg = line.substring(0, pos);
				String term = line.substring(pos + 1);
				commands.add(index, new String[]{reg.trim(), term.trim()});
				index++;
			}
			else if (line.charAt(0) == ':')
			{
				parameterlist.put(line, index);
			}
			else if (line.charAt(0) == '§')
			{
				String[] acommand = StringUtils.split(line, ' ');
				if (acommand[0].equals("§parameter") && acommand.length > 1)
				{
					validparameters.addAll(Arrays.asList(acommand).subList(1, acommand.length));
				}
				else if (acommand[0].equals("§limitexec"))
				{
					limitexeccount = Integer.parseInt(acommand[1]);
					this.log("+++ Setzte max. Befehle auf " + limitexeccount + "\n\n");
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

		List<String> addparams = new ArrayList<>();
		getContext().setAttribute("_ADDPARAMETERLIST", addparams, ScriptContext.ENGINE_SCOPE);
		if( addparameterlist.length > 1 )
		{
			addparams.addAll(Arrays.asList(addparameterlist).subList(1, addparameterlist.length));
		}
		
		try {
			// Ggf. Parameter behandeln
			if( parameter.length() > 0 ) {
				if( parameter.equals("-1") ) {
					context.getWriter().append(context.getBindings(ScriptContext.ENGINE_SCOPE).get("_OUTPUT").toString());
					// TODO: Sollte nicht hier sein...
					try {
						this.context.getErrorWriter().close();
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
					
					this.context = oldContext;
					
					return null;
				}
				else if( !validInternalParams.contains(parameter) && !parameterlist.containsKey(':'+parameter) ) {
					this.log("+++ Ungueltiger Parameter >"+parameter+"< - benutze >0<\n");
					parameter = "0";	
				}	
				else if( !validInternalParams.contains(parameter) && (validparameters.size() > 0) && !validparameters.contains(parameter) ) {
					this.log("+++ Parameter >"+parameter+"< gesperrt - benutze >0<\n");
					parameter = "0";	
				}
				setLastCommand(parameterlist.get(':'+parameter));
			}
		
			// Und los gehts!
			while( true ) {
				// Falls der Befehlszeiger am Ende angekommen ist die Ausfuehrung beenden
				if( getLastCommand() >= commands.size() ) {
					// Den Besitzer des zugehoerigen Schiffes informieren
					Ship ship = this.getShip();
					User source = (User)ContextMap.getContext().getDB().get(User.class, -1);

					PM.send(source, ship.getOwner().getId(), "Script beendet", "[Scriptsystem:"+ship.getId()+"]\nDie "+ship.getName()+
					" hat ihre Befehle abgearbeitet!");

					this.log("+++ Ausfuehrung beendet\n\n");
					setLastCommand(-1);
					
					break;
				}
				
				String[] command = commands.get(getLastCommand());
	
				if( command[0].charAt(0) == '#' ) {
					String[] cmd = commands.get(getLastCommand());
					this.log("* Berechne Ausdruck '"+cmd[1]+"' nach "+cmd[0]+"\n");
					
					this.setRegister(cmd[0],this.evalTerm(cmd[1]));
					
					setLastCommand(getLastCommand()+1);
					if( limitexeccount > 0 ) {
						limitexeccount--;
						if( limitexeccount == 0 ) {
							this.log("+++ max. Befehle erreicht\n\n");
							break;
						}
					}
					
					continue;
				}
				
				String funcname = command[0].toUpperCase();
				
				if( this.funcregister.containsKey(funcname) ) {
					SPFunction func = this.funcregister.get(funcname);
					
					Args[] args = this.funcargregister.get(funcname);
					if( (command.length-1 > args.length) && ((args[args.length-1].ordinal() & Args.VARIABLE.value()) != 0) ) {
						Args[] args2 = new Args[command.length-1];
						System.arraycopy(args, 0, args2, 0, args.length);
						
						for( int i=args.length; i < command.length-1; i++ ) {
							args2[i] = args[args.length-1];
						}
						
						args = args2;
					}
	
					for( int i=0; i < Math.min(args.length,command.length-1); i++ ) {
						String cmdParam = command[i+1];
							
						if( (cmdParam.charAt(0) == '#') && ((args[i].ordinal() & Args.REG.ordinal()) > 0) ) {
							cmdParam = this.getRegister(cmdParam);	
						}
							
						command[i+1] = cmdParam;	
					}
					
					this.log("*COMMAND: "+funcname+"\n");
					
					boolean[] result = func.execute(this, command);
					
					setLastCommand(getLastCommand()+(result[1] ? 1 : 0));
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
					setLastCommand(0);
					
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
					setLastCommand(-1);
					
					this.log("+++ Ausfuehrung beendet\n\n");
					break;
				} 
				else if( funcname.equals("!PAUSE") ) {
					this.log("*COMMAND: !PAUSE\n");				
					this.log("+++ Ausfuehrung vorerst angehalten\n\n");
					break;
				} 
				else if( funcname.startsWith("!J") && JUMP_FUNCTIONS.containsKey(funcname) ) {
					this.log("*COMMAND: "+funcname+"\n");
					boolean ok = false;
					
					int compResult = new Double(this.getRegister("cmp")).compareTo(0d);
					
					Integer[] vals = JUMP_FUNCTIONS.get(funcname);
					for (Integer val : vals)
					{
						if (val == compResult)
						{
							ok = true;
							break;
						}
					}
					if( ok ) {
						this.log("Marke: "+command[1]+"\n");
						if( !parameterlist.containsKey(":"+command[1]) ) {
							this.log("Unbekannte Sprungmarke: :"+command[1]);
							break;
						}
						setLastCommand(parameterlist.get(":"+command[1]));
					}	
					else {
						setLastCommand(getLastCommand()+1);
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
					String jumptarget = command[1];
					if( jumptarget.charAt(0) == '#' ) {
						jumptarget = this.getRegister(jumptarget);
					}
					this.log("Marke: "+jumptarget+"\n");
					
					if( !parameterlist.containsKey(":"+jumptarget) ) {
						this.log("WARNUNG: Unbekannte Sprungmarke "+jumptarget+"\n");
					}
					setLastCommand(parameterlist.get(":"+jumptarget));
					
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
					String dump = command[1];

					switch (dump)
					{
						case "register":
							this.log("################# register ###################\n");
							for (String reg : context.getBindings(ScriptContext.ENGINE_SCOPE).keySet())
							{
								if (reg.equals("_SHIP"))
								{
									continue;
								}
								this.log(reg + " (" + context.getBindings(ScriptContext.ENGINE_SCOPE).get(reg) + "), ");
							}
							this.log("\n\n");
							break;
						case "jumpaddrs":
							this.log("################# jumpaddrs ###################\n");
							for (Map.Entry<String, Integer> entry : parameterlist.entrySet())
							{
								this.log(entry.getKey() + " (" + entry.getValue() + "), ");
							}
							this.log("\n\n");
							break;
						case "code":
							this.log("################# commands ###################\n");
							for (int i = 0; i < commands.size(); i++)
							{
								this.log(i + ": " + Common.implode(" ", commands.get(i)) + "\n");
							}
							this.log("\n\n");
							break;
					}
					
					this.log("############### END OF DUMP #################\n");
					
					setLastCommand(getLastCommand()+1);
					
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
					this.log("*CURRENT COMMAND POINTER: "+getLastCommand()+"\n\n");
					this.log("################# jumpaddrs ###################\n");
					for( Map.Entry<String, Integer> entry: parameterlist.entrySet() ) {
						this.log(entry.getKey()+" ("+entry.getValue()+"), ");
					}
					this.log("\n\n");
					this.log("################# register ###################\n");
					for( String reg : context.getBindings(ScriptContext.ENGINE_SCOPE).keySet() ) {
						if( reg.equals("_SHIP") ) {
							continue;
						}
						this.log(reg+" ("+context.getBindings(ScriptContext.ENGINE_SCOPE).get(reg)+"), ");
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
		}
		catch( Exception e ) {
			StringBuilder text = new StringBuilder();

			StackTraceElement[] trace = e.getStackTrace();
			for( int i=0; i < Math.min(trace.length,5); i++ ) {
				text.append(trace[i].toString()).append("\n");
			}

			Ship ship = this.getShip();
			User source = (User)ContextMap.getContext().getDB().get(User.class, -1);

			PM.send(source, ship.getOwner().getId(), "Scriptfehler", "[Scriptsystem:"+ship.getId()+"]\nDas Script der "+ship.getName()+
			" konnte nicht korrekt ausgefuehrt werden!\n\n" +
			"Befehl: "+Common.implode(" ", commands.get(getLastCommand()))+"\n\n"+
			e+"\n"+text);
		}
		
		// TODO: Sollte nicht hier sein...
		try {
			this.context.getErrorWriter().close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
		
		this.context = oldContext;
		
		return null;
	}

	@Override
	public Bindings createBindings() {
		SimpleBindings bindings = new SimpleBindings();
		bindings.put("__INSTRUCTIONPOINTER", 0);
		
		return bindings;
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
		return factory;
	}
}
