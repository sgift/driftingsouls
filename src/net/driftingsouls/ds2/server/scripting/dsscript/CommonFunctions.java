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


import org.apache.commons.lang.StringUtils;

/**
 * Allgemeine Script-Funktionen.
 * @author Christopher Jung
 *
 */
class CommonFunctions {
	void registerFunctions(ScriptParser parser) {
		parser.registerCommand( "COMPARE", new Compare(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "COPY", new Copy(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "ADD", new Add(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "SUBSTRACT", new Substract(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "MULTIPLY", new Multiply(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "DIVIDE", new Divide(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "RANDOM", new Random(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN_REG );
		//parser.registerCommand( "COPYELEMENT", "copyelement", "common", ScriptParser.Args.PLAIN, ScriptParser.Args.REG, ScriptParser.Args.PLAIN_REG);
		//parser.registerCommand( "ARRAYRESET", "arrayreset", "common", ScriptParser.Args.PLAIN);
		//parser.registerCommand( "ARRAYNEXT", "arraynext", "common", ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN);
		parser.registerCommand( "STRAPPEND", new StrAppend(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG);
		parser.registerCommand( "COPYSTRING", new CopyString(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG);
		parser.registerCommand( "STRREPLACE", new StrReplace(), ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN, ScriptParser.Args.PLAIN_REG );
		parser.registerCommand( "GETSCRIPTPARAMETER", new GetScriptParameter(), ScriptParser.Args.PLAIN_REG, ScriptParser.Args.PLAIN);
	}
	
	static class Compare implements SPFunction {
		@Override
		public boolean[] execute(ScriptParser scriptparser, String[] command ) {
			double val1 = Value.Double(command[1]);
			double val2 = Value.Double(command[2]);
		
			scriptparser.log("val1: "+val1+"\n");
			scriptparser.log("val2: "+val2+"\n");
			
			double result = val1 - val2;
			
			scriptparser.log("result(#cmp): "+result+"\n");
			
			scriptparser.setRegister("cmp",Double.toString(result));
			
			return CONTINUE;
		}
	}
	
	static class Copy implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			String reg = command[1];
			String val = command[2];	
		
			scriptparser.log("register: "+reg+"\n");
			scriptparser.log("value: "+val+"\n");
			
			scriptparser.setRegister( reg, val );
			
			return CONTINUE;
		}
	}
	
	static class Add implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			double val1 = Double.parseDouble(command[1]);
			double val2 = Double.parseDouble(command[2]);
		
			scriptparser.log("val1: "+val1+"\n");
			scriptparser.log("val2: "+val2+"\n");
			
			scriptparser.setRegister("#MATH",Double.toString(val1+val2));
			
			return CONTINUE;
		}
	}
	
	static class Substract implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			double val1 = Double.parseDouble(command[1]);
			double val2 = Double.parseDouble(command[2]);
		
			scriptparser.log("val1: "+val1+"\n");
			scriptparser.log("val2: "+val2+"\n");
			
			scriptparser.setRegister("#MATH",Double.toString(val1-val2));
			
			return CONTINUE;
		}
	}
	
	static class Multiply implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			double val1 = Double.parseDouble(command[1]);
			double val2 = Double.parseDouble(command[2]);
		
			scriptparser.log("val1: "+val1+"\n");
			scriptparser.log("val2: "+val2+"\n");
			
			scriptparser.setRegister("#MATH",Double.toString(val1*val2));
			
			return CONTINUE;
		}
	}
	
	static class Divide implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			double val1 = Double.parseDouble(command[1]);
			double val2 = Double.parseDouble(command[2]);
		
			scriptparser.log("val1: "+val1+"\n");
			scriptparser.log("val2: "+val2+"\n");
			
			scriptparser.setRegister("#MATH",Double.toString(val1/val2));
			
			return CONTINUE;
		}
	}
	
	static class Random implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			int min = Integer.parseInt(command[1]);
			int max = Integer.parseInt(command[2]);
		
			int myrand = new java.util.Random().nextInt(max-min+1)-min;
			
			scriptparser.log("min: "+min+" - max: "+max+" - rand: "+myrand+"\n");
			
			scriptparser.setRegister("#MATH",Integer.toString(myrand));
			
			return CONTINUE;
		}
	}
	
	static class StrAppend implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			String mystring = "";
			if( command[1].charAt(0) == '#' ) {
				mystring = scriptparser.getRegister(command[1]);
			} 	 
			scriptparser.log("String1: "+mystring+"\n");
			
			mystring += command[2];
			scriptparser.log("String2: "+command[2]+"\n");
			
			if( command[1].charAt(0) == '#' ) {
				scriptparser.setRegister(command[1],mystring);
			}
			
			return CONTINUE;
		}
	}
	
	static class CopyString implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			String targetstring = "";
			if( command[1].charAt(0) == '#' ) {
				targetstring = scriptparser.getRegister(command[1]);
			} 	 
			scriptparser.log("String (target): "+targetstring);
			
			String sourcestring = command[2];
			scriptparser.log("String (source): "+sourcestring);
			
			if( command[1].charAt(0) == '#' ) {
				scriptparser.setRegister(command[1],command[2]);
			}
			
			return CONTINUE;
		}
	}
	
	static class StrReplace implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			String strdata = "";
			if( command[1].charAt(0) == '#' ) {
				strdata = scriptparser.getRegister(command[1]);
			}
			
			String myvar = command[2];
			String replace = command[3];
			
			scriptparser.log("string: "+strdata+"\n");
			scriptparser.log("var: "+myvar+"\n");
			scriptparser.log("replace: "+replace+"\n");
			
			strdata = StringUtils.replace(strdata, "{"+myvar+"}", replace );
			
			if( command[1].charAt(0) == '#' ) {
				scriptparser.setRegister(command[1], strdata);
			}
			
			return CONTINUE;
		}
	}
	
	static class GetScriptParameter implements SPFunction {
		@Override
		public boolean[] execute( ScriptParser scriptparser, String[] command ) {
			int paramNum = Integer.parseInt(command[1]);
			scriptparser.log("Parameter: "+paramNum+"\n");
		
			String target = command[2];
			scriptparser.log("Target: "+target+"\n");
			
			String param = scriptparser.getParameter(paramNum);
			
			if( target.charAt(0) == '#' ) {
				scriptparser.setRegister(target,param);
			}
			
			return CONTINUE;
		}
	}
}
