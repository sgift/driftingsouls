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
package net.driftingsouls.ds2.server.framework.templates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.DriftingSouls;

/**
 * <h1>Der Template-Compiler</h1>
 * Compiliert ein Template zu Java-Code, welcher anschliessend von javac weiterverarbeitet 
 * werden kann
 * @author Christopher Jung
 *
 */
public class TemplateCompiler {
	private interface TemplateCompileFunction {
		/**
		 * Fuehrt die Compilezeit-Funktion aus
		 * @param parameter Die Parameter der Funktion
		 * @return Der in das Template einzufuegende String
		 */
		public String process(List<String> parameter);
	}
	
	private static class TCFTableBegin implements TemplateCompileFunction {
		TCFTableBegin() {
			// EMPTY
		}
		public String process(List<String> parameter) {
			String width = (parameter.size() > 0 ? parameter.get(0) : "420");
			String align = (parameter.size() > 1 ? parameter.get(1) : "center");
			
			if( width.charAt(0) == '$' ) {
				width = width.substring(1);
				width = "\"); str.append(templateEngine.getVar(\""+width+"\")); str.append(\"";
			}
			if( align.charAt(0) == '$' ) {
				align = width.substring(1);
				align = "\"); str.append(templateEngine.getVar(\""+align+"\")); str.append(\"";
			}

			// TODO: check & ggf fixme (slash-problem ?)
			String txt = StringUtils.replace(Common.tableBegin("{$$WIDTH$$}","{$$ALIGN$$}","{$$IMAGEPATH$$}"),"\"", "\\\"");
			txt = StringUtils.replace(txt, "{$$IMAGEPATH$$}", "\"); str.append(templateEngine.getVar(\"global.datadir\")); str.append(\"");
			txt = StringUtils.replace(txt, "{$$ALIGN$$}", align);
			txt = StringUtils.replace(txt, "{$$WIDTH$$}", width);
			return txt;
		}	
	}
	
	private static class TCFTableEnd implements TemplateCompileFunction {
		TCFTableEnd() {
			// EMPTY
		}
		public String process(List<String> parameter) {
			String txt = StringUtils.replace(Common.tableEnd("{$$IMAGEPATH$$}"), "\"", "\\\"");
			return StringUtils.replace(txt, "{$$IMAGEPATH$}}", "\"); str.append(templateEngine.getVar(\"URL\")); str.append(\"");
		}	
	}
	
	private static class TCFLinkTo implements TemplateCompileFunction {
		TCFLinkTo() {
			// EMPTY
		}
		public String process(List<String> parameter) {
			Map<String,String> paramlist = new LinkedHashMap<String,String>();
			String name = parameter.get(0);
			String action = parameter.get(1);
			
			if( name.charAt(0) == '$' ) {
				name = name.substring(1);
				name = "\"); str.append(templateEngine.getVar(\""+name+"\")); str.append(\"";
			}
			
			if( action.charAt(0) == '$' ) {
				action = action.substring(1);
				action = "\"); str.append(templateEngine.getVar(\""+action+"\")); str.append(\"";
			}
			
			for( int i=2; i < parameter.size(); i++ ) {
				String arg = parameter.get(i);
				int pos = arg.indexOf(':');
				if( pos != -1 ) {
					String pname = arg.substring(0, pos).trim();
					String param = arg.substring(pos+1).trim();
					if( param.charAt(0) == '$' ) {
						param = param.substring(1);
						param = "\"); str.append(templateEngine.getVar(\""+param+"\")); str.append(\"";
					}
					paramlist.put(pname, param);	
				}
			}	
			
			StringBuilder text = new StringBuilder(50);
			text.append("<a ");
			
			if( paramlist.containsKey("link_target") ) {
				text.append("target=\\\""+paramlist.get("link_target")+"\\\" ");
				paramlist.remove("link_target");	
			}
			
			if( paramlist.containsKey("css_style") ) {
				text.append("style=\\\""+paramlist.get("css_style")+"\\\" ");
				paramlist.remove("css_style");	
			}	
			
			if( paramlist.containsKey("css_class") ) {
				text.append("class=\\\""+paramlist.get("css_class")+"\\\" href=\\\"ds?module=");
				paramlist.remove("css_class");	
			}	
			else {
				text.append("class=\\\"forschinfo\\\" href=\\\"./ds?module=");	
			}
			
			if( paramlist.containsKey("module") ) {
				text.append(paramlist.get("module")+"&amp;sess=");
				paramlist.remove("module");	
			}
			else {
				text.append("\"); str.append(templateEngine.getVar(\"global.module\")); str.append(\"&amp;sess=");
			}
			
			text.append("\"); str.append(templateEngine.getVar(\"global.sess\")); str.append(\"");
			
			text.append("&amp;action="+action);
			
			if( paramlist.size() > 0 ) {
				for( String key : paramlist.keySet() ) {
					text.append("&amp;"+key+"="+paramlist.get(key));	
				}
			}
			text.append("\\\">"+name+"</a>");
			
			return text.toString();
		}	
	}
	
	private static class TCFImageLinkTo implements TemplateCompileFunction {
		TCFImageLinkTo() {
			// EMPTY
		}
		public String process(List<String> parameter) {
			Map<String,String> paramlist = new LinkedHashMap<String,String>();
			List<String> params = new ArrayList<String>();
			
			for( int i=0; i < parameter.size(); i++ ) {
				String arg = parameter.get(i);
				int pos = arg.indexOf(':');
				if( pos != -1 ) {
					String pname = arg.substring(0, pos).trim();
					String param = arg.substring(pos+1).trim();
					if( param.charAt(0) == '$' ) {
						param = param.substring(1);
						param = "\"); str.append(templateEngine.getVar(\""+param+"\")); str.append(\"";
					}
					paramlist.put(pname, param);	
					
					if( !pname.equals("image_css_style") ) {
						params.add(arg);	
					}
				}
				else {
					params.add(arg);	
				}
			}
			
			String img_css = "";
			if( paramlist.containsKey("image_css_style") ) {
				img_css = ";"+paramlist.get("image_css_style");
				paramlist.remove("image_css_style");
			}
			
			params.set(0, "<img style=\\\"border:0px"+img_css+"\\\" src=\\\"\"); str.append(templateEngine.getVar(\"URL\")); str.append(\"data/"+params.get(0)+"\\\" alt=\\\"\\\" />");
			
			return new TCFLinkTo().process(params); 
		}	
	}
	
	private static class TCFOverlib implements TemplateCompileFunction {
		TCFOverlib() {
			// EMPTY
		}
		public String process(List<String> parameter) {
			String text = parameter.get(0);
			
			if( text.charAt(0) == '$' ) {
				text = text.substring(1);
				text = "\"); str.append(templateEngine.getVar(\""+text+"\")); str.append(\"";
			}
			else {
				text = StringUtils.replace(StringEscapeUtils.escapeJavaScript(text.replaceAll("<", "&lt;").replaceAll(">", "&gt;")), "\\", "\\\\");	
			}
			
			String event = parameter.get(1);
			
			Map<String,String> paramlist = new LinkedHashMap<String,String>();
			paramlist.put("TIMEOUT", "0");
			paramlist.put("DELAY", "400");
			paramlist.put("WIDTH", "300");
						
			for( int i=2; i < parameter.size(); i++ ) {
				String arg = parameter.get(i);
				int pos = arg.indexOf(':');
				if( pos != -1 ) {
					String pname = arg.substring(0, pos).trim();
					String param = arg.substring(pos+1).trim();
					if( param.charAt(0) == '$' ) {
						param = param.substring(1);
						param = "\"); str.append(templateEngine.getVar(\""+param+"\")); str.append(\"";
					}
					if( paramlist.containsKey(pname.toUpperCase()) ) {
						paramlist.put(pname.toUpperCase(), param);
					}
					else {
						paramlist.put(pname, param);
					}
				}
			}
			
			List<String> paramtext = new ArrayList<String>();
			for( String key : paramlist.keySet() ) {
				paramtext.add(key+','+paramlist.get(key));
			}
				
			if( event.equals("mo") ) {
				text = "onmouseover=\\\"return overlib(\\'"+text+"\\',"+Common.implode(",",paramtext)+");\\\" onmouseout=\\\"return nd();\\\"";		
			}
			else if( event.equals("md") ) {
				text = "onclick=\\\"return overlib(\\'"+text+"\\',"+Common.implode(",",paramtext)+");\\\" onmouseout=\\\"return nd();\\\"";
			}
			
			return text;
		}	
	}
	
	private static class TCFFormCreateHidden implements TemplateCompileFunction {
		TCFFormCreateHidden() {
			// EMPTY
		}
		public String process(List<String> parameter) {
			Map<String,String> paramlist = new LinkedHashMap<String,String>();
			String action = parameter.get(0);
			
			if( action.charAt(0) == '$' ) {
				action = action.substring(1);
				action = "\"); str.append(templateEngine.getVar(\""+action+"\")); str.append(\"";
			}
			
			for( int i=1; i < parameter.size(); i++ ) {
				String arg = parameter.get(i);
				int pos = arg.indexOf(':');
				if( pos != -1 ) {
					String pname = arg.substring(0, pos).trim();
					String param = arg.substring(pos+1).trim();
					if( param.charAt(0) == '$' ) {
						param = param.substring(1);
						param = "\"); str.append(templateEngine.getVar(\""+param+"\")); str.append(\"";
					}
					paramlist.put(pname, param);	
				}
			}
			
			StringBuilder text = new StringBuilder(50);
			if( !action.equals("-") ) {
				text.append("<input type=\\\"hidden\\\" name=\\\"action\\\" value=\\\""+action+"\\\" />\n");
			}
			
			if( paramlist.containsKey("module") ) {
				text.append("<input type=\\\"hidden\\\" name=\\\"module\\\" value=\\\""+paramlist.get("module")+"\\\" />\n");
				paramlist.remove("module");	
			}
			else {
				text.append("<input type=\\\"hidden\\\" name=\\\"module\\\" value=\\\"\"); str.append(templateEngine.getVar(\"global.module\")); str.append(\"\\\" />\n");
			}
			
			text.append("<input type=\\\"hidden\\\" name=\\\"sess\\\" value=\\\"\"); str.append(templateEngine.getVar(\"global.sess\")); str.append(\"\\\" />\n");
			
			for( String key : paramlist.keySet() ) {
				text.append("<input type=\\\"hidden\\\" name=\\\""+key+"\\\" value=\\\""+paramlist.get(key)+"\\\" />\n");
			}
			
			return text.toString();
		}	
	}
	
	private static class TCFCheckbox implements TemplateCompileFunction {
		TCFCheckbox() {
			// EMPTY
		}
		public String process(List<String> parameter) {
			String text = parameter.get(0);
			String name = parameter.get(1);
			String var = parameter.get(2);
			
			String ret = "<input type=\"checkbox\" name=\""+name+"\" id=\""+name+"\" {if "+var+"}checked=\"checked\"{/endif} value=\"1\" /><label for=\""+name+"\">"+text+"</label>";
			
			return StringUtils.replace(ret, "\"", "\\\"");
		}	
	}
	
	private static final Map<String,TemplateCompileFunction> COMPILE_FUNCTIONS = new HashMap<String,TemplateCompileFunction>();
	
	static {
		COMPILE_FUNCTIONS.put("table_begin", new TCFTableBegin());
		COMPILE_FUNCTIONS.put("table_end", new TCFTableEnd());
		COMPILE_FUNCTIONS.put("image_link_to", new TCFImageLinkTo());
		COMPILE_FUNCTIONS.put("link_to", new TCFLinkTo());
		COMPILE_FUNCTIONS.put("overlib", new TCFOverlib());
		COMPILE_FUNCTIONS.put("form_create_hidden", new TCFFormCreateHidden());
		COMPILE_FUNCTIONS.put("checkbox", new TCFCheckbox());
	}
	
	private String file;
	private String outputPath;
	private String subPackage;
	
	/**
	 * Konstruktor
	 * @param file Die zu kompilierende Datei
	 * @param outputPath Das Ausgabeverzeichnis, in dem die kompilierte Datei abgelegt werden soll
	 */
	public TemplateCompiler(String file, String outputPath) {
		this(file, outputPath, null);
	}
	
	/**
	 * Konstruktor
	 * @param file Die zu kompilierende Datei
	 * @param outputPath Das Ausgabeverzeichnis, in dem die kompilierte Datei abgelegt werden soll
	 * @param subPackage Das zu verwendende Overlay-Paket. <code>null</code>, falls das Template in kein Overlay-Paket gehoert
	 */
	public TemplateCompiler(String file, String outputPath, String subPackage) {
		this.file = file;
		this.outputPath = outputPath;	
		this.subPackage = subPackage;
	}
	
	private String parse_if( String bedingung ) {
		bedingung = bedingung.trim();
		String[] bed = bedingung.split(" ");
		List<String> bedingungen = new ArrayList<String>();
		for( int i=0; i < bed.length; i++ ) {
			if( bed[i].trim().length() != 0 ) {
				bedingungen.add(bed[i].trim());
			} 
		}

		// Test auf Wahrheit
		if( bedingungen.size() < 2 ) {
			bedingung = Pattern.compile("([a-zA-Z0-9_\\.]{3,})").matcher(bedingung).replaceAll("templateEngine.isVarTrue(\"$1\")");
		}
		// Negierter Test auf Wahrheit
		else if( bedingungen.size() == 2 ) {
			bedingung = Pattern.compile("([a-zA-Z0-9_\\.]{3,})").matcher(bedingungen.get(1)).replaceAll("!templateEngine.isVarTrue(\"$1\")");
		}
		// Vergleichsoperation
		else if( bedingungen.size() == 3 ) {
			String op = bedingungen.get(1);
			String val1 = bedingungen.get(0);
			String val2 = bedingungen.get(2);
			bedingung = "templateEngine.getNumberVar(\""+val1+"\").doubleValue() "+op+" "+val2;
		}
		
		return "\"); if( "+bedingung+" ) { str.append(\"";
	}
	
	private String parse_control_structures( String block ) {
		StringBuffer blockBuilder = new StringBuffer(block.length());
		Matcher match = Pattern.compile("\\{if (['\"]?)([^\"'\\}]*)\\1}").matcher(block);
		int index = 0;
		while( match.find() ) {
			blockBuilder.append(block.substring(index, match.start()));
			blockBuilder.append(parse_if(match.group(2)));
			index = match.end();
		}
		blockBuilder.append(block.substring(index));
		
		block = blockBuilder.toString();
		
		block = Pattern.compile("\\{else\\}").matcher(block).replaceAll("\"); } else { str.append(\"");
		
		block = Pattern.compile("\\{\\/endif\\}").matcher(block).replaceAll("\"); } str.append(\"");
		return block;
	}
	
	private String parse_function( String name, String parameter, boolean callnow ) {
		parameter = parameter.trim();
		List<String> parameters = new ArrayList<String>(Arrays.asList(parameter.split(",")));
		// Laufzeit-Funktionen
		if( !callnow ) {
			for( int i=0; i < parameters.size(); i++ ) {
				if( parameters.get(i).trim().length() != 0 ) {
					parameters.set(i, "\""+parameters.get(i).trim()+"\"");
				}
				else {
					parameters.remove(i);
					i--;
				}
			}
		
			// TODO
			throw new RuntimeException("STUB");
			//return ".\$templateEngine->$name( ".implode( ',', $parameter )." ).";
		}
		
		// Compilezeit-Funktionen
		for( int i=0; i < parameters.size(); i++ ) {
			if( parameters.get(i).trim().length() != 0 ) {
				parameters.set(i, parameters.get(i).trim());
			}
			else {
				parameters.remove(i);
				i--;
			}
		}
			
		if( COMPILE_FUNCTIONS.containsKey(name) ) {
			return COMPILE_FUNCTIONS.get(name).process(parameters);
		}
		throw new RuntimeException("Die compile-time Funktion '"+name+"' konnte nicht lokalisiert werden");
	}
	
	private String parse_functions( String block ) {
		StringBuffer blockBuilder = new StringBuffer(block.length());
		Matcher match = Pattern.compile("\\{#([^\\s^\\}^\\?]+)([^\\}]*)\\}").matcher(block);
		int index = 0;
		while( match.find() ) {
			blockBuilder.append(block.substring(index, match.start()));
			blockBuilder.append(parse_function(match.group(1), match.group(2), false));
			index = match.end();
		}
		blockBuilder.append(block.substring(index));
		
		match = Pattern.compile("\\{!([^\\s^\\}^\\?]+)([^\\}]*)\\}").matcher(blockBuilder.toString());
		blockBuilder.setLength(0);
		index = 0;
		while( match.find() ) {
			blockBuilder.append(block.substring(index, match.start()));
			blockBuilder.append(parse_function(match.group(1), match.group(2), true));
			index = match.end();
		}
		blockBuilder.append(block.substring(index));

		return blockBuilder.toString();
	}
	
	private String parse_vars( String block ) {
		return Pattern.compile("\\{([^\\}^\\s^\\?]+)\\}").matcher(block).replaceAll("\\\"); str.append(templateEngine.getVar(\"$1\")); str.append(\\\"");
	}
	
	private static class CompiledBlock {
		/**
		 * Der Name des Blocks
		 */
		String name;
		
		/**
		 * Der Inhalt des Blocks
		 */
		String block;
		
		/**
		 * Die im Block auftauchenden Variablen
		 */
		List<String> varlist;
		
		/**
		 * Der Elternblock
		 */
		String parent;
		
		CompiledBlock(String name, String block, List<String> varlist, String parent) {
			super();
			this.name = name;
			this.block = block;
			this.varlist = varlist;
			this.parent = parent;
		}
	}
	
	private List<CompiledBlock> parse_blocks( StringBuilder blockBuilder, String parent ) {
		String block = blockBuilder.toString();
		
		String reg = "<!--\\s+BEGIN ([^\\s]*)\\s+-->";
		Matcher match = Pattern.compile(reg, Pattern.MULTILINE).matcher(block);
		
		List<CompiledBlock> blocklist = new ArrayList<CompiledBlock>();
		
		while( match.find() ) {
			// ok...wir haben einen block
			// nun wollen wir mal das Ende suchen und ihn rausoperieren
			
			String name = match.group(1);

			Matcher blockMatch = 
				Pattern.compile( "<!--\\s+BEGIN "+Pattern.quote(name)+"\\s+-->(.*)\\s*<!--\\s+END "+Pattern.quote(name)+"\\s+-->", Pattern.MULTILINE | Pattern.DOTALL)
				.matcher(block);

			block = 
				Pattern.compile( "<!--\\s+BEGIN "+Pattern.quote(name)+"\\s+-->(.*)\\s*<!--\\s+END "+Pattern.quote(name)+"\\s+-->", Pattern.MULTILINE | Pattern.DOTALL)
				.matcher(block)
				.replaceAll("\"); str.append(templateEngine.getBlockReplacementVar(\""+name+"\")); str.append(\"");
			
			blockMatch.find();
			StringBuilder extractedblock = new StringBuilder(blockMatch.group(1));
								
			List<CompiledBlock> subblocks = parse_blocks( extractedblock, name );

			List<String> newvarlist = new ArrayList<String>();
			/*Matcher varlist = Pattern.compile( "templateEngine.getVar\\(\"([^\"]*)\"([^\\)]*)\\)/").matcher(extractedblock.toString());

			while( varlist.find() ) {
				newvarlist.add(varlist.group(1));
			}*/
			
			Matcher varlist = Pattern.compile( "templateEngine.getBlockReplacementVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(extractedblock.toString());

			while( varlist.find() ) {
				newvarlist.add(varlist.group(1));
			}
								
			blocklist.add(new CompiledBlock(name, extractedblock.toString(), newvarlist, parent ));
			blocklist.addAll(subblocks);
			
			match = Pattern.compile(reg, Pattern.MULTILINE).matcher(block);
		}
		
		blockBuilder.setLength(0);
		blockBuilder.insert(0, block);
		
		return blocklist;
	}
	
	private List<String> parse_getChildVars( List<CompiledBlock> blocks, String name ) {
		List<String> result = new ArrayList<String>();
		for( int i=0; i < blocks.size(); i++ ) {
			if( blocks.get(i).parent.equals(name) ) {
				result.addAll(blocks.get(i).varlist);
				result.addAll(parse_getChildVars( blocks, blocks.get(i).name));
			}	
		}
		
		return result;	
	}
	
	/**
	 * Startet den Kompiliervorgang
	 * @throws IOException
	 */
	public void compile() throws IOException {
		String baseFileName = file.substring(file.lastIndexOf("/")+1, file.lastIndexOf(".html"));
		BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
		
		String str = "";
		String curLine = "";
		while( (curLine = reader.readLine()) != null ) {
			if( str.length() != 0 ) {
				str += "\n";
			}
			str += curLine;
		}
		
		reader.close();
		
		str = StringUtils.replace(str, "\\", "\\\\");
		str = StringUtils.replace(str, "\"", "\\\"");
		//str = StringUtils.replace(str, "\\'","\\\\'");
		
		// Funktionen ersetzen
		str = parse_functions(str);

		// if's ersetzen
		str = parse_control_structures(str);
		
		// Variablen ersetzen
		str = parse_vars(str);

		Matcher match = Pattern.compile("templateEngine.getVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(str);

		List<String> completevarlist = new ArrayList<String>();
		while( match.find() ) {
			completevarlist.add(match.group(1));
		}
		
		match = Pattern.compile("templateEngine.getBlockReplacementVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(str);
		while( match.find() ) {
			completevarlist.add(match.group(1));
		}
		
		// Bloecke ersetzen
		StringBuilder strBuilder = new StringBuilder(str);
		List<CompiledBlock> result = parse_blocks(strBuilder, "MAIN");
		str = strBuilder.toString();
		
		// Compilierte Datei schreiben
		// Zuerst der Header
		
		String bfname = StringUtils.replace(baseFileName, ".", "");
		StringBuilder newfile = new StringBuilder(1000);
		if( subPackage == null ) {
			newfile.append("package net.driftingsouls.ds2.server.templates;\n");
		}
		else {
			newfile.append("package net.driftingsouls.ds2.server.templates."+subPackage+";\n");
		}
		newfile.append("import net.driftingsouls.ds2.server.framework.templates.Template;\n");
		newfile.append("import net.driftingsouls.ds2.server.framework.templates.TemplateBlock;\n");
		newfile.append("import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;\n\n");
		
		newfile.append("/* Dieses Script wurde automatisch generiert\n");
		newfile.append("   Alle Aenderungen gehen daher bei der naechten Generierung\n");
		newfile.append("   verloren. Wenn sie Aenderungen machen wollen, tun sie dies\n");
		newfile.append("   bitte im entsprechenden Ursprungstemplate */\n\n");
		
		newfile.append("public class "+bfname+" implements Template {\n");
		newfile.append("\tpublic void prepare( TemplateEngine templateEngine, String filehandle ) {\n");
		
		for( int i=0; i < result.size(); i++ ) {
			CompiledBlock block = result.get(i);
			String parent = "filehandle";
			if( !block.parent.equals("MAIN") ) {
				parent = "\""+block.parent+"\"";
			}

			newfile.append("\t\ttemplateEngine.registerBlockItrnl(\""+block.name+"\",filehandle,"+parent+");\n");
		}
	
		newfile.append("\t}\n\n");
		
		// Jetzt die Klassen fuer die einzelnen Bloecke
		
		for( int i=0; i < result.size(); i++ ) {
			CompiledBlock block = result.get(i);
			
			newfile.append("\tstatic class "+StringUtils.replace(block.name, ".", "")+" implements TemplateBlock {\n");
			newfile.append("\t\tpublic String[] getBlockVars(boolean all) {\n");
			newfile.append("\t\t\tif( !all ) {\n");
			
			if( block.varlist.size() != 0 ) {
				newfile.append("\t\t\t\treturn new String[] {\""+Common.implode("\",\"",block.varlist)+"\"};\n");
			}
			else {
				newfile.append("\t\t\t\treturn new String[] {};\n");
			}
			
			newfile.append("\t\t\t} else {\n");
			
			List<String> varlist = block.varlist;
			varlist.addAll(parse_getChildVars(result, block.name));
			
			if( varlist.size() != 0 ) {
				newfile.append("\t\t\t\treturn new String[] {\""+Common.implode("\",\"",varlist)+"\"};\n");
			}
			else {
				newfile.append("\t\t\t\treturn new String[] {};\n");
			}
			newfile.append("\t\t\t}\n\t\t}\n");
			newfile.append("\t\tpublic String output(TemplateEngine templateEngine) {\n");
			newfile.append("\t\tStringBuilder str = new StringBuilder("+block.block.length()+");\n");

			String[] blockstr =  StringUtils.replace(block.block, "\r\n", "\n").split("\n");
			for( int j=0; j < blockstr.length; j++ ) {
				if( j < blockstr.length - 1 ) {
					newfile.append("\t\tstr.append(\""+blockstr[j]+"\\n\");\n");
				}
				else {
					newfile.append("\t\tstr.append(\""+blockstr[j]+"\");\n");
				}
			}
			newfile.append("\t\treturn str.toString();");

			newfile.append("\t\t}\n");
			newfile.append("\t}\n");
		}
		
		// Und jetzt den Rest
		
		newfile.append("\tpublic TemplateBlock getBlock(String block) {\n");
		for( int i=0; i < result.size(); i++ ) {
			CompiledBlock block = result.get(i);
			newfile.append("\t\tif( block.equals(\""+block.name+"\") ) {\n");
			newfile.append("\t\t\treturn new "+StringUtils.replace(block.name, ".", "")+"();\n");
			newfile.append("\t\t}\n");
		}
		newfile.append("\t\treturn null;\n");
		newfile.append("\t}\n");
		
		match = Pattern.compile("templateEngine.getVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(str);

		List<String> newvarlist = new ArrayList<String>();
		while( match.find() ) {
			newvarlist.add(match.group(1));
		}
		
		match = Pattern.compile( "templateEngine.getBlockReplacementVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(str);

		while( match.find() ) {
			newvarlist.add(match.group(1));
		}
		
		newfile.append("\tpublic String[] getVarList(boolean all) {\n");
		newfile.append("\t\tif( !all ) {\n");
		
		if( newvarlist.size() > 0 ) {
			newfile.append("\t\t\treturn new String[] {\""+Common.implode("\",\"",newvarlist)+"\"};\n");
		}
		else {
			newfile.append("\t\t\treturn new String[] {};\n");
		}
		
		newfile.append("\t\t} else {\n");

		if( completevarlist.size() > 0 ) {
			newfile.append("\t\t\treturn new String[] {\""+Common.implode("\",\"",completevarlist)+"\"};\n");
		}
		else {
			newfile.append("\t\t\treturn new String[] {};\n");
		}
		newfile.append("\t\t}\n\t}\n");
		
		// Nicht zu vergessen: Der Inhalt der Templatedatei, der keinem Block zugeordnet ist...
		
		newfile.append("\tpublic String main( TemplateEngine templateEngine ) {\n");
		newfile.append("\t\tStringBuilder str = new StringBuilder("+str.length()+");\n");
		str = StringUtils.replace(str, "\r\n", "\n");
		String[] strLines = str.split("\n");
		for( int i=0; i < strLines.length; i++ ) {
			if( i < strLines.length - 1 ) {
				newfile.append("\t\tstr.append(\""+strLines[i]+"\\n\");\n");
			}
			else {
				newfile.append("\t\tstr.append(\""+strLines[i]+"\");\n");
			}
		}
		newfile.append("\t\treturn str.toString();");

		newfile.append("\t}\n");
		newfile.append("}");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputPath+"/"+bfname+".java")));
		writer.write(newfile.toString());
		writer.close();
	}
	
	private static void compileDirectory( File dir, String outputPath, String subPackage ) throws IOException {
		File[] files = dir.listFiles();
		for( int i=0; i < files.length; i++ ) {
			if( files[i].getName().indexOf(".html") != -1 ) {
				String file = files[i].getAbsolutePath();
				String baseFileName = file.substring(file.lastIndexOf("/")+1, file.lastIndexOf(".html"));
				String bfname = StringUtils.replace(baseFileName, ".", "");
				File compiledFile = new File(outputPath+"/"+bfname+".java");
				if( !compiledFile.exists() || (compiledFile.lastModified() < files[i].lastModified()) ) {
					System.out.println("compiling "+file);
					TemplateCompiler compiler = new TemplateCompiler(file, outputPath, subPackage);
					compiler.compile();
				}
			}
			else if( files[i].isDirectory() && !files[i].isHidden() ) {
				String subOutputPath = outputPath+"/"+files[i].getName();
				if( !new File(subOutputPath).exists() ) {
					new File(subOutputPath).mkdir();
				}
				compileDirectory(files[i], subOutputPath, subPackage != null ? subPackage+"."+files[i].getName() : files[i].getName());
			}
		}
	}
	
	/**
	 * Main
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if( args.length < 3 ) {
			System.out.println("java net.driftingsouls.ds2.server.framework.templates.TemplateCompiler [Configdir] [TemplateFile] [outputpath]");
			return;
		}
		System.getProperties().setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
		
		Log LOG = new LogFactoryImpl().getInstance("DS2");
		LOG.info("Booting DS...");
		
		try {
			new DriftingSouls(LOG, args[0], false);
		}
		catch( Exception e ) {
			LOG.fatal(e, e);
			throw new Exception(e);
		}
		String file = args[1];
		String outputPath = args[2];
		
		// Wenn es sich um ein Verzeichnis handelt, dann alle HTML-Dateien kompilieren, 
		// sofern sie neuer sind als die kompilierten Fassungen
		if( new File(file).isDirectory() ) {
			compileDirectory(new File(file), outputPath, null);
		}
		// Wenn direkt eine Datei angegeben wurde, dann diese auf jeden Fall kompilieren
		else {
			System.out.println("compiling "+file);
			TemplateCompiler compiler = new TemplateCompiler(file, outputPath);
			compiler.compile();
		}
	}

}
