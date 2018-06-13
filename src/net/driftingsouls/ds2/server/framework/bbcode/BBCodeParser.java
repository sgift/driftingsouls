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
package net.driftingsouls.ds2.server.framework.bbcode;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * <p>Der BBCodeParser formatiert einen Text mittels BBCodes. Bei BBCodes
 * handelt es sich um Tags in der Form [name]...[/name] oder [name] oder 
 * [name=parameter1,parameter2]...[/name] usw.</p>
 * <p>Das Formatieren einzelner Tags kann dabei auch ueber Funktionen geschehen
 * ({@link BBCodeFunction}).</p>
 * <p>Der BBCodeParser erlaubt es neben den Framework-BBCodes auf eigene 
 * anwendungsspezifische BBCodes hinzuzufuegen. Diese muessen dazu in der
 * Datei <code>META-INF/services/net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction</code>
 * eingetragen werden. Zudem koennen zur Laufzeit weitere BBCodes registriert werden.</p>
 * 
 * @author Christopher Jung
 *
 */
public class BBCodeParser {
	private static final Log log = LogFactory.getLog(BBCodeParser.class);
	private static BBCodeParser instance = null;
	
	private Map<String,BBCodeFunction> replaceFunctions = new HashMap<>();
	private Map<String,HashSet<Integer>> tags = new HashMap<>();

	private BBCodeParser() {
		try {
			// Framework-BBCodes registrieren
			registerHandler( "url", 1, new TagURL() );
			registerHandler( "url", 2, new TagURL() );
			registerHandler( "img", 1, "<img style=\"vertical-align:middle; border:0px\" src=\"$1\" alt=\"\" />" );
			registerHandler( "imglf", 1, "<img style=\"float:right; margin-left: 10px; magin-top: 5px; margin-bottom: 5px; border:0px\" src=\"$1\" alt=\"\" />" );
			registerHandler( "imgrf", 1, "<img style=\"float:left; margin-right: 10px; magin-top: 5px; margin-bottom: 5px; border:0px\" src=\"$1\" alt=\"\" />" );
			registerHandler( "b", 1, "<span style=\"font-weight:bold\">$1</span>" );
			registerHandler( "i", 1, "<span style=\"font-style:italic\">$1</span>" );
			registerHandler( "email", 1, "<a href=\"mailto:$1\">$1</a>" );
			registerHandler( "email", 2, "<a href=\"mailto:$2\">$1</a>" );
			registerHandler( "u", 1, "<span style=\"text-decoration:underline\">$1</span>" );
			registerHandler( "size", 2, "<span style=\"font-size:$2pt\">$1</span>" );
			registerHandler( "list", 2, new TagList(TagList.Type.LIST) );
			registerHandler( "list", 1, new TagList(TagList.Type.LIST) );
			registerHandler( "sublist", 2, new TagList(TagList.Type.SUBLIST) );
			registerHandler( "sublist", 1, new TagList(TagList.Type.SUBLIST) );
			registerHandler( "color", 2, "<span style=\"color:$2\">$1</span>" );
			registerHandler( "font", 2, "<span style=\"font-family:$2\">$1</span>" );
			registerHandler( "align", 2, "<div style=\"text-align:$2\">$1</div>" );
			registerHandler( "mark", 2, "<span style=\"background-color:$2\">$1</span>" );
			registerHandler( "hr", 0, "<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />" );
			registerHandler( "hide", 1, "" );
			
			// Weitere BBCodes aus META-INF/services/net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction lesen
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Enumeration<URL> resources = loader.getResources("META-INF/services/"+BBCodeFunction.class.getName());
			while( resources.hasMoreElements() ) {
				URL elem = resources.nextElement();
				
				Document doc = builder.parse(elem.openStream());
				Element root = doc.getDocumentElement();
				NodeList bbcodes = root.getElementsByTagName("bbcode");
				for( int i=0; i < bbcodes.getLength(); i++ ) {
					Element bbcode = (Element)bbcodes.item(i);
					
					final String tag = bbcode.getAttribute("tag");
					final int params = Integer.parseInt(bbcode.getAttribute("params")); 
					
					if( bbcode.getAttribute("handler").length() == 0 ) {
						String text = XMLUtils.firstChildOfType(bbcode, Node.CDATA_SECTION_NODE).getNodeValue();
						text = Common.trimLines(text).trim();
						registerHandler(tag, params, text);
					}
					else {
						String cls = bbcode.getAttribute("handler");
						try {
							Class<? extends BBCodeFunction> bbcodeCls = Class.forName(cls).asSubclass(BBCodeFunction.class);
							registerHandler(tag, params, bbcodeCls.getDeclaredConstructor().newInstance());
						}
						catch( ClassNotFoundException e ) {
							log.warn("Konnte BBCode "+tag+"("+params+") nicht laden. Handler-Klasse '"+cls+"' nicht vorhanden");
						}
					}
				}
			}
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Liefert eine Instanz des BBCodeParsers zurueck.
	 * 
	 * @return Eine BBCodeParser-Instanz
	 */
	public static BBCodeParser getInstance() {
		if( instance == null ) {
			instance = new BBCodeParser();
		}
		return instance;
	}
	
	/**
	 * Gibt explizit eine neue Instanz zurueck. Die
	 * Instanz wird kein zweites Mal zurueckgegeben.
	 * @return eine neue Instanz
	 */
	public static BBCodeParser getNewInstance() {
		return new BBCodeParser();
	}
	
	/**
	 * Registriert einen neuen BBCode unter Angabe eines Ersatzstrings. 
	 * Dabei werden jeweils die $1-$3 des Ersatzstrings durch die jeweiligen
	 * Parameter des BBCodes ersetzt (Muster: [tag $2,$3]$1[/tag]) .
	 * @param tag Name des BBCode-Tags
	 * @param params Anzahl der Parameter des Tags
	 * @param replace Ersatzstring
	 * @throws IllegalArgumentException Falls die Anzahl der Parameter ungueltig ist
	 * @throws IllegalStateException Falls ein gleichlautender Tag mit der selben Anzahl an Parametern bereits registriert wurde
	 */
	public void registerHandler( String tag, int params, String replace ) throws IllegalArgumentException,IllegalStateException {
		registerHandler(tag, params, new SimpleBBCodeFunction(replace));
	}
	
	/**
	 * Registriert einen neuen BBCode unter Verwendung einer Ersatzfunktion.
	 * @param tagname Name des BBCode-Tags
	 * @param params Anzahl der Parameter
	 * @param replaceFunc Ersatzfunktion
	 * @throws IllegalArgumentException Falls die Anzahl der Parameter ungueltig ist
	 * @throws IllegalStateException Falls ein gleichlautender Tag mit der selben Anzahl an Parametern bereits registriert wurde
	 */
	public void registerHandler( String tagname, int params, BBCodeFunction replaceFunc ) throws IllegalArgumentException,IllegalStateException {
		if( params > 3 ) {
			throw new IllegalArgumentException("Illegal parameter count '"+params+"'");
		}
		
		String tag = tagname.toLowerCase();
		
		if( replaceFunctions.containsKey(tag+"("+params+")") ) {
			throw new IllegalStateException("Tag '"+tag+"("+params+")' already known to the BBCodeParser");
		}

		if( !tags.containsKey(tag) ) {
			tags.put(tag, new HashSet<>());
		}
		tags.get(tag).add(params);
		
		replaceFunctions.put(tag+"("+params+")", replaceFunc);
	}
	
	private StringBuilder parse( StringBuilder text, HashSet<String> ign ) {
		int textIndex = 0;
		int index;
		
		StringBuilder buffer = new StringBuilder(text.length());
		while( (index = text.indexOf("[", textIndex)) != -1 ) {
			// Naechsten Tag ermitteln
			if( index != 0 ) {
				buffer.append(text.subSequence(textIndex,index));
				textIndex = index;
			}
			
			int finish = text.indexOf("]", textIndex);
			if( finish == -1 ) {
				break;
			}
			
			String plainTag = text.substring(textIndex+1, finish);
			String tagLC = plainTag.toLowerCase();
			String tag = plainTag;
			textIndex = finish+1;
			
			// Handelt es sich um einen schliessenden Tag?
			if( (tagLC.length() == 0) || (tagLC.charAt(0) == '/') ) {
				buffer.append("[").append(plainTag).append("]");
				continue;
			}
			// Falls vorhanden: Optionen (hinter dem = verarbeiten)
			String[] options = null;
			if( tag.indexOf('=') != -1 ) {
				int optionIndex = tag.indexOf('=');
				String option = tag.substring(optionIndex+1);
				tagLC = tagLC.substring(0,optionIndex);
				tag = tag.substring(0,optionIndex);
				options = option.split(",");
			}

			// existiert der Tag / darf er verwendet werden?
			if( !tags.containsKey(tagLC) ) {
				buffer.append('[');
				buffer.append(plainTag);
				buffer.append(']');
				continue;
			}
			
			// Schliessenden Tag bestimmen
			// Dazu gleiche Tags (oeffnende und schliessende) zaehlen, bis der counter unter 0 faellt
			int closetag = -1;
			int counter = 0;
			int tagIndex = textIndex-1;
			while( (tagIndex = text.indexOf(tag, tagIndex+1)) != -1 ) {
				if( text.charAt(tagIndex-1) == '[' ) {
					char chr = text.charAt(tagIndex+tag.length());
					if( chr == ']' ) {
						counter++;
					}
					else if( chr == '=' && (text.indexOf("]", tagIndex+1) != -1) ) {
						counter++;
					}
				}
				else if( (text.charAt(tagIndex-1) == '/') && (text.charAt(tagIndex-2) == '[') ) {
					// Check, ob ueberhaupt noch genug Platz fuer den Endtag ist
					if( text.length() - tagIndex <= tag.length() ) {
						break;
					}
					
					// Check, ob dies wirklich ein Endtag ist, er also mit einem ] beendet wird
					if( text.charAt(tagIndex+tag.length()) != ']' ) {
						continue;
					}
					
					counter--;
					if( counter < 0 ) {
						closetag = tagIndex-2;
						break;
					}
				}
			}

			// Keinen schliessenden Tag? -> 0 Parameter
			if( (closetag == -1) && tags.get(tagLC).contains(0) ) {
				if( ign.contains(tagLC+"(0)") ) {
					continue;
				}
				buffer.append(replaceFunctions.get(tagLC+"(0)").handleMatch(null));
			}
			else if( closetag != -1 ) {
				// 1-3 Parameter
				StringBuilder paramText = parse(new StringBuilder(text.subSequence(textIndex, closetag)), ign);
				textIndex = closetag+3+tagLC.length();

				int params = 1;
				if( options != null ) {
					params += options.length;
				}
				
				if( tags.get(tagLC).contains(params) ) {
					StringBuilder tagLCNum = new StringBuilder(tagLC.length()+3);
					tagLCNum.append(tagLC);
					tagLCNum.append("(");
					tagLCNum.append(params);
					tagLCNum.append(")");
					if( ign.contains(tagLCNum.toString())  ){
						buffer.append(paramText);
					}
					else {
						BBCodeFunction func = replaceFunctions.get(tagLC+"("+params+")");
	
						buffer.append( func.handleMatch(paramText.toString(), options) );
					}
				}
				else {
					buffer.append('[');
					buffer.append(plainTag);
					buffer.append(']');
				}
			}
			else {
				buffer.append('[');
				buffer.append(plainTag);
				buffer.append(']');
			}
		}
		if( buffer.length() == 0 ) {
			return text;
		}
		
		if( textIndex < text.length()-1 ) {
			buffer.append(text.substring(textIndex));
		}

		return buffer;
	}
	
	/**
	 * Formatiert einen Text mit BBCodes, wobei einige/alle
	 * BBCodes ignoriert werden koennen.
	 * Die zu ignorierenden BBCodes muessen dazu in ignore stehen.
	 * Wenn alle ignoriert werden sollen reicht der String "all" als
	 * Element. Wenn einige bestimmte ignoriert werden sollen, muessen diese
	 * jeweils unter Verwendung des Namens und der Anzahl der Parameter angegeben werden
	 * (Muster: tag(Parameteranzahl) - Ein Tag b mit 1 Parameter haette folglich diesen
	 * Bezeichner: b(1) ) 
	 * Der formatierte Text wird anschliessend zurueckgegeben.
	 * 
	 * @param text Der zu formatierende Text
	 * @param ignore Liste der zu ignorierenden Tags
	 * @return Der formatierte Text
	 */
	public String parse( String text, String[] ignore ) {
		HashSet<String> ign = new HashSet<>();
		if( (ignore != null) && (ignore.length > 0) ) {
			ign.addAll(Arrays.asList(ignore));
			
			if( ign.contains("all") ) {
				ign.remove("all");
				for( String key : replaceFunctions.keySet() ) {
					ign.add(key);
				}
			}
		}
		
		return parse(new StringBuilder(text), ign).toString();
	}
	
	/**
	 * Formatiert einen Text mit BBCodes. Der formatierte Text
	 * wird anschliessend zurueckgegeben.
	 * 
	 * @param text Der zu formatierende Text
	 * @return Der formatierte Text
	 */
	public String parse( String text ) {
		return parse( new StringBuilder(text), new HashSet<>() ).toString();
	}
}
