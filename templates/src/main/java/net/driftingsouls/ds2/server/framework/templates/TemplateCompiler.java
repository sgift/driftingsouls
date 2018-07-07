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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h1>Der Template-Compiler.</h1>
 * Compiliert ein Template zu Java-Code, welcher anschliessend von javac weiterverarbeitet
 * werden kann.
 * @author Christopher Jung
 *
 */
public class TemplateCompiler {
	private final List<String> templatePackages;

	private interface TemplateCompileFunction {
		/**
		 * Fuehrt die Compilezeit-Funktion aus.
		 * @param parameter Die Parameter der Funktion
		 * @return Der in das Template einzufuegende String
		 */
        String process(List<String> parameter);
	}

	private static class TCFLinkTo implements TemplateCompileFunction {
		TCFLinkTo() {
			// EMPTY
		}
		@Override
		public String process(List<String> parameter) {
			Map<String,String> paramlist = new LinkedHashMap<>();
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
				text.append("target=\\\"").append(paramlist.get("link_target")).append("\\\" ");
				paramlist.remove("link_target");
			}

			if( paramlist.containsKey("css_style") ) {
				text.append("style=\\\"").append(paramlist.get("css_style")).append("\\\" ");
				paramlist.remove("css_style");
			}

			if( paramlist.containsKey("css_class") ) {
				text.append("class=\\\"").append(paramlist.get("css_class")).append("\\\" href=\\\"ds?module=");
				paramlist.remove("css_class");
			}
			else {
				text.append("class=\\\"forschinfo\\\" href=\\\"./ds?module=");
			}

			if( paramlist.containsKey("module") ) {
				text.append(paramlist.get("module")).append("&amp;");
				paramlist.remove("module");
			}
			else {
				text.append("\"); str.append(templateEngine.getVar(\"global.module\")); str.append(\"&amp;");
			}

			text.append("action=").append(action);

			if( paramlist.size() > 0 ) {
				for( Map.Entry<String, String> entry : paramlist.entrySet() ) {
					text.append("&amp;").append(entry.getKey()).append("=").append(entry.getValue());
				}
			}
			text.append("\\\">").append(name).append("</a>");

			return text.toString();
		}
	}

	private static class TCFImageLinkTo implements TemplateCompileFunction {
		TCFImageLinkTo() {
			// EMPTY
		}
		@Override
		public String process(List<String> parameter) {
			Map<String,String> paramlist = new LinkedHashMap<>();
			List<String> params = new ArrayList<>();

			for (String arg : parameter)
			{
				int pos = arg.indexOf(':');
				if (pos != -1)
				{
					String pname = arg.substring(0, pos).trim();
					String param = arg.substring(pos + 1).trim();
					if (param.charAt(0) == '$')
					{
						param = param.substring(1);
						param = "\"); str.append(templateEngine.getVar(\"" + param + "\")); str.append(\"";
					}
					paramlist.put(pname, param);

					if (!pname.equals("image_css_style"))
					{
						params.add(arg);
					}
				}
				else
				{
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

	private static class TCFFormCreateHidden implements TemplateCompileFunction {
		TCFFormCreateHidden() {
			// EMPTY
		}
		@Override
		public String process(List<String> parameter) {
			Map<String,String> paramlist = new LinkedHashMap<>();
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
				text.append("<input type=\\\"hidden\\\" name=\\\"action\\\" value=\\\"").append(action).append("\\\" />\n");
			}

			if( paramlist.containsKey("module") ) {
				text.append("<input type=\\\"hidden\\\" name=\\\"module\\\" value=\\\"").append(paramlist.get("module")).append("\\\" />\n");
				paramlist.remove("module");
			}
			else {
				text.append("<input type=\\\"hidden\\\" name=\\\"module\\\" value=\\\"\"); str.append(templateEngine.getVar(\"global.module\")); str.append(\"\\\" />\n");
			}

			for( Map.Entry<String, String> entry : paramlist.entrySet() ) {
				text.append("<input type=\\\"hidden\\\" name=\\\"").append(entry.getKey()).append("\\\" value=\\\"").append(entry.getValue()).append("\\\" />\n");
			}

			return text.toString();
		}
	}

	private static class TCFCheckbox implements TemplateCompileFunction {
		TCFCheckbox() {
			// EMPTY
		}
		@Override
		public String process(List<String> parameter) {
			String text = parameter.get(0);
			String name = parameter.get(1);
			String var = parameter.get(2);

			String ret = "<input type=\"checkbox\" name=\""+name+"\" id=\""+name+"\" {if "+var+"}checked=\"checked\"{/endif} value=\"1\" /><label for=\""+name+"\">"+text+"</label>";

			return ret.replace("\"", "\\\"");
		}
	}

	private static final Map<String,TemplateCompileFunction> COMPILE_FUNCTIONS = new HashMap<>();

	static {
		COMPILE_FUNCTIONS.put("image_link_to", new TCFImageLinkTo());
		COMPILE_FUNCTIONS.put("link_to", new TCFLinkTo());
		COMPILE_FUNCTIONS.put("form_create_hidden", new TCFFormCreateHidden());
		COMPILE_FUNCTIONS.put("checkbox", new TCFCheckbox());
	}

	private String sourceFilename;
	private String baseOutputPath;
	private String javaFilename;

	/**
	 * Konstruktor.
	 * @param sourceFilename Die zu kompilierende Datei
	 * @param baseOutputPath Das Ausgabeverzeichnis, in dem die nach Java uebersetze Datei abgelegt werden soll
	 */
	private TemplateCompiler(String sourceFilename, String baseOutputPath, String javaFilename) {
		this.sourceFilename = sourceFilename;
		this.baseOutputPath = baseOutputPath;
		this.javaFilename = javaFilename;

		templatePackages = List.of("net", "driftingsouls", "ds2", "server", "framework", "templates");
	}

	private String parse_if( String bedingung ) {
		bedingung = bedingung.trim();
		String[] bed = bedingung.split(" ");
		List<String> bedingungen = new ArrayList<>();
		for (String aBed : bed)
		{
			if (aBed.trim().length() != 0)
			{
				bedingungen.add(aBed.trim());
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
		StringBuilder blockBuilder = new StringBuilder(block.length());
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
		List<String> parameters = new ArrayList<>(Arrays.asList(parameter.split(",")));
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
		StringBuilder blockBuilder = new StringBuilder(block.length());
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
		 * Der Name des Blocks.
		 */
		String name;

		/**
		 * Der Inhalt des Blocks.
		 */
		String block;

		/**
		 * Die im Block auftauchenden Variablen.
		 */
		List<String> varlist;

		/**
		 * Der Elternblock.
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

		List<CompiledBlock> blocklist = new ArrayList<>();

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

			if( !blockMatch.find() )
			{
				throw new IllegalStateException("Block "+name+" nicht geschlossen");
			}
			StringBuilder extractedblock = new StringBuilder(blockMatch.group(1));

			List<CompiledBlock> subblocks = parse_blocks( extractedblock, name );

			List<String> newvarlist = new ArrayList<>();
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
		List<String> result = new ArrayList<>();
		for( int i=0; i < blocks.size(); i++ ) {
			if( blocks.get(i).parent.equals(name) ) {
				result.addAll(blocks.get(i).varlist);
				result.addAll(parse_getChildVars( blocks, blocks.get(i).name));
			}
		}

		return result;
	}

	/**
	 * Startet den Kompiliervorgang.
	 * @throws IOException
	 */
	public void compile() throws IOException {
		String baseFileName = new File(sourceFilename).getName();
		baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf(".html"));

		String str = readTemplate();

		StringBuilder strBuilder;
		str = str.replace("\\", "\\\\");
		str = str.replace("\"", "\\\"");

		// Funktionen ersetzen
		str = parse_functions(str);

		// if's ersetzen
		str = parse_control_structures(str);

		// Variablen ersetzen
		str = parse_vars(str);

		Matcher match = Pattern.compile("templateEngine.getVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(str);

		List<String> completevarlist = new ArrayList<>();
		while( match.find() ) {
			completevarlist.add(match.group(1));
		}

		match = Pattern.compile("templateEngine.getBlockReplacementVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(str);
		while( match.find() ) {
			completevarlist.add(match.group(1));
		}

		// Bloecke ersetzen
		strBuilder = new StringBuilder(str);
		List<CompiledBlock> result = parse_blocks(strBuilder, "MAIN");
		str = strBuilder.toString();

		// Compilierte Datei schreiben
		// Zuerst der Header

		String bfname = baseFileName.replace(".", "");
		StringBuilder newfile = new StringBuilder(1000);

		newfile.append("package ");
		newfile.append(String.join(".", templatePackages));
		newfile.append(";\n");
		newfile.append("import net.driftingsouls.ds2.server.framework.templates.Template;\n");
		newfile.append("import net.driftingsouls.ds2.server.framework.templates.TemplateBlock;\n");
		newfile.append("import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;\n\n");

		newfile.append("/* Dieses Script wurde automatisch generiert\n");
		newfile.append("   Alle Aenderungen gehen daher bei der naeschten Generierung\n");
		newfile.append("   verloren. Wenn sie Aenderungen machen wollen, tun sie dies\n");
		newfile.append("   bitte im entsprechenden Ursprungstemplate */\n\n");

		newfile.append("public class ").append(bfname).append(" implements Template {\n");
		newfile.append("\tpublic void prepare( TemplateEngine templateEngine, String filehandle ) {\n");

		for (CompiledBlock block : result)
		{
			String parent = "filehandle";
			if (!block.parent.equals("MAIN"))
			{
				parent = "\"" + block.parent + "\"";
			}

			newfile.append("\t\ttemplateEngine.registerBlockItrnl(\"").append(block.name).append("\",filehandle,").append(parent).append(");\n");
		}

		newfile.append("\t}\n\n");

		// Jetzt die Klassen fuer die einzelnen Bloecke

		for( int i=0; i < result.size(); i++ ) {
			CompiledBlock block = result.get(i);

			newfile.append("\tstatic class ").append(block.name.replace(".", "")).append(" implements TemplateBlock {\n");
			newfile.append("\t\tpublic String[] getBlockVars(boolean all) {\n");
			newfile.append("\t\t\tif( !all ) {\n");

			if( block.varlist.size() != 0 ) {
				newfile.append("\t\t\t\treturn new String[] {\"").append(String.join("\",\"", block.varlist)).append("\"};\n");
			}
			else {
				newfile.append("\t\t\t\treturn new String[] {};\n");
			}

			newfile.append("\t\t\t} else {\n");

			List<String> varlist = block.varlist;
			varlist.addAll(parse_getChildVars(result, block.name));

			if( varlist.size() != 0 ) {
				newfile.append("\t\t\t\treturn new String[] {\"").append(String.join("\",\"", varlist)).append("\"};\n");
			}
			else {
				newfile.append("\t\t\t\treturn new String[] {};\n");
			}
			newfile.append("\t\t\t}\n\t\t}\n");
			newfile.append("\t\tpublic String output(TemplateEngine templateEngine) {\n");
			newfile.append("\t\tStringBuilder str = new StringBuilder(").append(block.block.length()).append(");\n");

			String[] blockstr =  block.block.replace("\r\n", "\n").split("\n");
			for( int j=0; j < blockstr.length; j++ ) {
				if( j < blockstr.length - 1 ) {
					newfile.append("\t\tstr.append(\"").append(blockstr[j]).append("\\n\");\n");
				}
				else {
					newfile.append("\t\tstr.append(\"").append(blockstr[j]).append("\");\n");
				}
			}
			newfile.append("\t\treturn str.toString();");

			newfile.append("\t\t}\n");
			newfile.append("\t}\n");
		}

		// Und jetzt den Rest

		newfile.append("\tpublic TemplateBlock getBlock(String block) {\n");
		for (CompiledBlock block : result)
		{
			newfile.append("\t\tif( block.equals(\"").append(block.name).append("\") ) {\n");
			newfile.append("\t\t\treturn new ").append(block.name.replace(".", "")).append("();\n");
			newfile.append("\t\t}\n");
		}
		newfile.append("\t\treturn null;\n");
		newfile.append("\t}\n");

		match = Pattern.compile("templateEngine.getVar\\(\"([^\"]*)\"([^\\)]*)\\)").matcher(str);

		List<String> newvarlist = new ArrayList<>();
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
			newfile.append("\t\t\treturn new String[] {\"").append(String.join("\",\"", newvarlist)).append("\"};\n");
		}
		else {
			newfile.append("\t\t\treturn new String[] {};\n");
		}

		newfile.append("\t\t} else {\n");

		if( completevarlist.size() > 0 ) {
			newfile.append("\t\t\treturn new String[] {\"").append(String.join("\",\"", completevarlist)).append("\"};\n");
		}
		else {
			newfile.append("\t\t\treturn new String[] {};\n");
		}
		newfile.append("\t\t}\n\t}\n");

		// Nicht zu vergessen: Der Inhalt der Templatedatei, der keinem Block zugeordnet ist...

		newfile.append("\tpublic String main( TemplateEngine templateEngine ) {\n");
		newfile.append("\t\tStringBuilder str = new StringBuilder(").append(str.length()).append(");\n");
		str = str.replace("\r\n", "\n");
		String[] strLines = str.split("\n");
		for( int i=0; i < strLines.length; i++ ) {
			if( i < strLines.length - 1 ) {
				newfile.append("\t\tstr.append(\"").append(strLines[i]).append("\\n\");\n");
			}
			else {
				newfile.append("\t\tstr.append(\"").append(strLines[i]).append("\");\n");
			}
		}
		newfile.append("\t\treturn str.toString();");

		newfile.append("\t}\n");
		newfile.append("}");


		Path outputFolder = Paths.get(baseOutputPath, templatePackages.toArray(new String[0]));
		outputFolder = Files.createDirectories(outputFolder);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFolder.resolve(javaFilename + ".java").toFile()))) {
			writer.write(newfile.toString());
		}
	}

	private String readTemplate() throws IOException
	{
		StringBuilder strBuilder = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(new File(sourceFilename))))
		{
			String curLine;
			while ((curLine = reader.readLine()) != null)
			{
				if (strBuilder.length() != 0)
				{
					strBuilder.append("\n");
				}
				strBuilder.append(curLine);
			}
		}

		return strBuilder.toString();
	}

	private static void compileDirectory(File dir, String basePath) throws IOException {
		File[] files = dir.listFiles();
		assert files != null;
		for (File file : files)
		{
			if (file.getName().endsWith(".html"))
			{
				String baseFileName = file.getName().substring(0, file.getName().length() - 5);
				String javaFileName = baseFileName.replace(".", "");
				TemplateCompiler compiler = new TemplateCompiler(file.toString(), basePath, javaFileName);
				compiler.compile();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if( args.length != 2 ) {
			System.out.println("java net.driftingsouls.ds2.server.framework.templates.TemplateCompiler [TemplateFolder] [baseOutputPath]");
			return;
		}

		String file = args[0];
		String outputPath = args[1];

		Path inputFolder = Paths.get(file);
		if(!Files.isDirectory(inputFolder)) {
			System.out.println("TemplateFolder is not a directory");
			return;
		}

		compileDirectory(inputFolder.toFile(), outputPath);
	}
}