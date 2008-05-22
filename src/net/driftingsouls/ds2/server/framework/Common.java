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
package net.driftingsouls.ds2.server.framework;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import org.apache.commons.lang.StringUtils;

/**
 * Allgemeine Hilfsfunktionen des Frameworks
 * @author Christopher Jung
 *
 */
public class Common implements Loggable {
	private static NumberFormat numberFormat;
	private static Locale locale;
	
	static {
		locale = Locale.GERMAN;
		numberFormat = NumberFormat.getInstance(Locale.GERMAN);
	}
	
	private Common() {
		// EMPTY
	}
	
	private static Set<StackTraceElement> stubWarnList = new HashSet<StackTraceElement>();
	
	/**
	 * Markiert eine Methode als "STUB". Beim ersten Aufruf der Methode wird eine Warnung
	 * ausgegeben.
	 *
	 */
	public static void stub() {
		StackTraceElement[] elements = new Throwable().getStackTrace();
		if( !stubWarnList.contains(elements[1]) ) {
			stubWarnList.add(elements[1]);
			LOG.warn("STUB: "+elements[1].toString());
		}
	}
	
	/**
	 * Entfernt am Anfang und Ende jeder Zeile eines Texts ueberschuessige Zeichen (Leerzeichen usw)
	 * @param text Der Text
	 * @return Das Ergebnis
	 */
	public static String trimLines(String text) {
		StringBuilder output = new StringBuilder(text.length());
		String[] lines = StringUtils.split(text.trim(),'\n');
		for( int i=0; i < lines.length; i++ ) {
			output.append(lines[i].trim());
			if( i < lines.length - 1 ) { 
				output.append("\n");
			}
		}
		return output.toString();
	}
	
	/**
	 * Baut eine URL unter Verwendung des aktuellen Kontexts, einer Ziel-Action und weiterer Parameter
	 * zusammen. Die weiteren Parameter sind inner in zweier Paerchen anzuordnen und bestehen immer aus
	 * Parameternamen und -wert. Sollte in diesen weiteren Parametern nicht der Parameter "module" auftauchen
	 * wird automatisch das aktuelle Modul angegeben.
	 * @param action Die Ziel-Action
	 * @param paramlist Weitere Parameter
	 *  
	 * @return die zusammengebaute URL
	 */
	public static String buildUrl( String action, Object ... paramlist ) {
		final Context context = ContextMap.getContext();
		
		final StringBuilder buffer = new StringBuilder("?module=");	
		final Request request = context.getRequest();
		
		final HashMap<String,String> params = new HashMap<String,String>();
		for( int i=0; i < paramlist.length; i+=2 ) {
			params.put(paramlist[i].toString(), paramlist[i+1].toString());
		}
		
		if( params.containsKey("module") ) {
			buffer.append(params.get("module"));
			params.remove("module");
		}
		else if( request.getParameter("module") != null ) {
			buffer.append(request.getParameter("module"));
		}

		if( request.getParameter("_style") != null ) {
			buffer.append("&amp;_style="+request.getParameter("_style"));	
		}
	
		buffer.append("&amp;action="+action);
	
		for( Entry<String, String> entry: params.entrySet() ) 
		{
			buffer.append("&amp;"+entry.getKey()+"="+entry.getValue());
		}
		
		return buffer.toString();
	}
	
	/**
	 * Liefert das aktuell verwendete <code>NumberFormat</code>
	 * @return das aktuell verwendete <code>NumberFormat</code>
	 */
	public static NumberFormat getNumberFormat() {
		return numberFormat;
	}
	
	/**
	 * Liefert die aktive <code>Locale</code> zurueck
	 * @return Die aktive <code>Locale</code>
	 */
	public static Locale getLocale() {
		return locale;
	}
	
	/**
	 * Setzt die aktive Locale
	 * @param locale die neue Locale
	 */
	static void setLocale( Locale locale ) {
		Common.locale = locale;
		numberFormat = NumberFormat.getInstance(locale);
	}
	
	/**
	 * Formatiert eine Zahl unter Verwendung der gerade gesetzten <code>Locale</code>
	 * @param number Die zu formatierende Zahl
	 * @return Die formatierte Zahl
	 */
	public static String ln( Number number ) {
		return numberFormat.format(number);
	}
	
	/**
	 * Gibt den Anfangsteil einer DS-Tabelle als String zurueck
	 * @param width Die gewuenschte Breite
	 * @param align Die gewuenschte Ausrichtung des Inhalts (<code>left</code>,<code>center</code>,<code>right</code>,<code>justify</code>)
	 * @return Der Tabellenkopf als String
	 * @see #tableEnd()
	 */
	public static String tableBegin( int width, String align ) {
		return tableBegin(Integer.toString(width), align, Configuration.getSetting("URL"));
	}
	
	/**
	 * Gibt den Anfangsteil einer DS-Tabelle als String zurueck
	 * @param width Die gewuenschte Breite
	 * @param align Die gewuenschte Ausrichtung des Inhalts (<code>left</code>,<code>center</code>,<code>right</code>,<code>justify</code>)
	 * @param imagePath der Pfad zum Data-Verzeichnis
	 * @return Der Tabellenkopf als String
	 * @see #tableEnd()
	 */
	public static String tableBegin( String width, String align, String imagePath ) {
		StringBuilder sb = new StringBuilder(200);
		
		sb.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"noBorderX\" width=\""+width+"\">");
		sb.append("<tr>");
		sb.append("<td class=\"noBorderXnBG\" style=\"width:19px\"><img src=\""+imagePath+"data/interface/border/border_topleft.gif\" alt=\"\" /></td>");
		sb.append("<td class=\"noBorderXnBG\" style=\"background-image:url("+imagePath+"data/interface/border/border_top.gif); background-repeat:repeat-x\"></td>");
		sb.append("<td class=\"noBorderXnBG\" style=\"width:19px\"><img src=\""+imagePath+"data/interface/border/border_topright.gif\" alt=\"\" /></td>");
		sb.append("</tr>");
		sb.append("<tr>");
		sb.append("<td class=\"noBorderXnBG\" rowspan=\"1\" style=\"width:19px; background-image:url("+imagePath+"data/interface/border/border_left.gif); background-repeat:repeat-y\"></td>");
		sb.append("<td class=\"noBorderX\" colspan=\"1\" style=\"background-image: url("+imagePath+"data/interface/border/border_background.gif);\" align=\""+align+"\">");
		
		return sb.toString();
	}
	
	/**
	 * Gibt den Endteil einer DS-Tabelle als String zurueck
	 * @return Der Tabellenfuss als String
	 * @see #tableBegin(int, String)
	 */
	public static String tableEnd() {
		return tableEnd(Configuration.getSetting("URL"));
	}
	
	/**
	 * Gibt den Endteil einer DS-Tabelle als String zurueck
	 * @param imagePath der Pfad zum Data-Verzeichnis
	 * @return Der Tabellenfuss als String
	 * @see #tableBegin(int, String)
	 */
	public static String tableEnd(String imagePath) {
		StringBuilder sb = new StringBuilder(200);
		
		sb.append("</td>");
		sb.append("<td class=\"noBorderXnBG\" rowspan=\"1\" style=\"width:19px; background-image:url("+imagePath+"data/interface/border/border_right.gif); background-repeat:repeat-y\"></td>");
		sb.append("</tr>");
		sb.append("<tr>");
		sb.append("<td class=\"noBorderXnBG\" style=\"width:19px\"><img src=\""+imagePath+"data/interface/border/border_bottomleft.gif\" alt=\"\" /></td>");
		sb.append("<td class=\"noBorderXnBG\" colspan=\"1\" style=\"background-image:url("+imagePath+"data/interface/border/border_bottom.gif); background-repeat:repeat-x\"></td>");
		sb.append("<td class=\"noBorderXnBG\" style=\"width:19px\"><img src=\""+imagePath+"data/interface/border/border_bottomright.gif\" alt=\"\" /></td>");
		sb.append("</tr>");
		sb.append("</table>");
		
		return sb.toString();
	}
	
	/**
	 * Verknuepft ein Array mittels Trennzeichen zu einem String
	 * 
	 * @param <T> Der Typ des Sets
	 * @param separator Das Trennzeichen
	 * @param list Das zu verknuepfende Set
	 * @return Das verknuepfte Set
	 */
	public static <T> String implode( String separator, Set<T> list ) {
		if( list.size() > 0 ) {
			StringBuilder sb = new StringBuilder(5*list.size());
			boolean first = true;
			
			for( T entry : list ) {
				if( !first) {
					sb.append(separator);
				}

				sb.append(entry);
				first = false;
			}
			
			return sb.toString();
		}
		return "";
	}
	
	/**
	 * Verknuepft ein Array mittels Trennzeichen zu einem String
	 * 
	 * @param separator Das Trennzeichen
	 * @param list Das zu verknuepfende Array
	 * @return Das verknuepfte Array
	 */
	public static String implode( String separator, int[] list ) {
		if( list.length > 0 ) {
			StringBuilder sb = new StringBuilder(5*list.length);
			
			for( int i=0; i < list.length; i++ ) {
				if( i > 0 ) {
					sb.append(separator);
				}

				sb.append(list[i]);
			}
			
			return sb.toString();
		}
		return "";
	}
	
	/**
	 * Verknuepft ein Array mittels Trennzeichen zu einem String
	 * 
	 * @param <T> Der Typ des Arrays
	 * @param separator Das Trennzeichen
	 * @param list Das zu verknuepfende Array
	 * @return Das verknuepfte Array
	 */
	public static <T> String implode( String separator, T[] list ) {
		if( list.length > 0 ) {
			StringBuilder sb = new StringBuilder(5*list.length);
			
			for( int i=0; i < list.length; i++ ) {
				if( i > 0 ) {
					sb.append(separator);
				}

				sb.append(list[i]);
			}
			
			return sb.toString();
		}
		return "";
	}
	
	/**
	 * Verknuepft eine Liste mittels Trennzeichen zu einem String
	 * 
	 * @param separator Das Trennzeichen
	 * @param list Die zu verknuepfende Liste
	 * @return Die verknuepfte Liste
	 */
	public static String implode( String separator, List<? extends Object> list ) {
		if( list.size() > 0 ) {
			StringBuilder sb = new StringBuilder(5*list.size());
			boolean first = true;
			
			for( Object elem : list ) {
				if( !first ) {
					sb.append(separator);
				}
				else {
					first = false;
				}
				sb.append(elem);
			}
			
			return sb.toString();
		}
		return "";
	}
	
	/**
	 * Splittet einen String anhand eines Trenn-Strings in einzelne Teile
	 * und konvertiert diese in Integer-Zahlen 
	 * @param separator Der Trenn-String
	 * @param array Der zu trennende String
	 * @return der getrennte String als Integer-Array
	 */
	public static Integer[] explodeToInteger(String separator, String array) {
		int[] result = explodeToInt(separator, array, -1);
		Integer[] result2 = new Integer[result.length];
		for( int i=0; i < result.length; i++ ) {
			result2[i] = Integer.valueOf(result[i]);
		}
		return result2;
	}
	
	/**
	 * Splittet einen String anhand eines Trenn-Strings in einzelne Teile
	 * und konvertiert diese in Integer-Zahlen 
	 * @param separator Der Trenn-String
	 * @param array Der zu trennende String
	 * @return der getrennte String als Integer-Array
	 */
	public static int[] explodeToInt(String separator, String array) {
		return explodeToInt(separator, array, -1);
	}
	
	/**
	 * Splittet einen String anhand eines Trenn-Strings in einzelne Teile
	 * und konvertiert diese in Integer-Zahlen. Das resultierende Integer-Array
	 * hat dabei exakt die angegebende Groesse
	 *  
	 * @param separator Der Trenn-String
	 * @param array Der zu trennende String
	 * @param size Die Groesse des Ergebnis-Arrays oder <code>-1</code>, wenn diese variabel sein soll  
	 * @return der getrennte String als Integer-Array
	 * 
	 * @see #explodeToInt(String, String)
	 */
	public static int[] explodeToInt(String separator, String array, int size) {
		String[] list = StringUtils.split(array, separator);
		int[] result = new int[size == -1 ? list.length : size];
		for( int i=0; i < list.length; i++ ) {
			try {
				result[i] = Integer.parseInt(list[i]);
			}
			catch( Exception e ) {
				// EMPTY
			} 
		}
		
		return result;
	}
	
	/**
	 * Liefert den aktuellen Zeitstempel in Sekunden ab dem 1.1.1970
	 * 
	 * @return Der Zeitstempel
	 */
	public static long time() {
		return new Date().getTime() / 1000;
	}
	
	private static String[] months = {
		"Januar",
		"Februar",
		"März",
		"April",
		"Mai",
		"Juni",
		"Juli",
		"August",
		"September",
		"Oktober",
		"November",
		"Dezember"
	};
	
	private static String[] days = {
		"Sonntag",
		"Montag",
		"Dienstag",
		"Mittwoch",
		"Donnerstag",
		"Freitag",
		"Samstag"
	};
	
	/**
	 * Erzeugt anhand des Formatierungsmusters einen Datumsstring mit dem aktuellen Datum
	 * 
	 * @param pattern Der Formatierungsstring
	 * @return Das formatierte Datum
	 * @see #date(String, int)
	 */
	public static String date(String pattern) {
		return date(pattern, time());
	}

	/**
	 * Erzeugt aus einem Zeitstempel und einem Formatierungsmuster einen Datumsstring.
	 * Vgl. date(...)-Befehl in php
	 * 
	 * @param pattern Das Formatierungsmuster
	 * @param time Der Zeitstempel
	 * @return Das formatierte Datum
	 */
	public static String date(String pattern, long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(time*1000));
		
		StringBuilder buffer = new StringBuilder();
		for( int i=0; i < pattern.length(); i++ ) {
			char chr = pattern.charAt(i);
			switch( chr ) {
			case 'a':
				if( cal.get(Calendar.HOUR) < 12 ) {
					buffer.append("am");
				}
				else {
					buffer.append("pm");
				}
				break;
			case 'A':
				if( cal.get(Calendar.HOUR) < 12 ) {
					buffer.append("AM");
				}
				else {
					buffer.append("PM");
				}
				break;
			case 'B':
				// TODO
				break;
			case 'c':
				// TODO
				break;
			case 'd': {
					int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
					if( dayOfMonth < 10 ) {
						buffer.append('0');
					}
					buffer.append(dayOfMonth);
				}
				break;
			case 'D':
				buffer.append(months[cal.get(Calendar.MONTH)].substring(0,3));
				break;
			case 'F':
				buffer.append(months[cal.get(Calendar.MONTH)]);
				break;
			case 'g': {
					int hour = cal.get(Calendar.HOUR_OF_DAY);
					if( hour > 12 ) {
						hour -= 12;
					}
					if( hour == 0 ) {
						hour = 12;
					}
					buffer.append(hour);
				}
				break;
			case 'G':
				buffer.append(cal.get(Calendar.HOUR_OF_DAY));
				break;
			case 'h': {
					int hour = cal.get(Calendar.HOUR_OF_DAY);
					if( hour > 12 ) {
						hour -= 12;
					}
					if( hour == 0 ) {
						hour = 12;
					}
					if( hour < 10 ) {
						buffer.append('0');
					}
					buffer.append(hour);
				}
				break;
			case 'H': {
					int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
					if( hourOfDay < 10 ) {
						buffer.append('0');
					}
					buffer.append(hourOfDay);
				}
				break;
			case 'i': {
					int minute = cal.get(Calendar.MINUTE);
					if( minute < 10 ) {
						buffer.append('0');
					}
					buffer.append(minute);
				}
				break;
			case 'I':
				buffer.append(cal.getTimeZone().inDaylightTime(cal.getTime()) ? "1" : "0");
				break;
			case 'j':
				buffer.append(cal.get(Calendar.DAY_OF_MONTH));
				break;
			case 'l':
				buffer.append(days[cal.get(Calendar.DAY_OF_MONTH)]);
				break;
			case 'L':
				// TODO
				break;
			case 'm': {
					int month = cal.get(Calendar.MONTH)+1;
					if( month < 10 ) {
						buffer.append('0');
					}
					buffer.append(month);
				}
				break;
			case 'M':
				buffer.append(months[cal.get(Calendar.MONTH)].substring(0,3));
				break;
			case 'n':
				buffer.append(cal.get(Calendar.MONTH)+1);
				break;
			case 'O':
				buffer.append(cal.getTimeZone().getRawOffset());
				break;
			case 'r':
				// TODO
				break;
			case 's':
				int sec = cal.get(Calendar.SECOND);
				if( sec < 10 ) {
					buffer.append('0');
				}
				buffer.append(sec);
				break;
			case 'S': {
					int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
					switch( dayOfMonth ) {
					case 1: 
						buffer.append("st");
						break;
					case 2:
						buffer.append("nd");
						break;
					case 3:
						buffer.append("rd");
						break;
					default:
						buffer.append("th");
					}
				}
				break;
			case 't':
				buffer.append(cal.getActualMaximum(Calendar.DAY_OF_MONTH));
				break;
			case 'T':
				buffer.append(cal.getTimeZone().getDisplayName(false, TimeZone.SHORT));
				break;
			case 'U':
				buffer.append(cal.getTime().getTime());
				break;
			case 'w':
				buffer.append(cal.get(Calendar.DAY_OF_WEEK));
				break;
			case 'W':
				// TODO
				break;
			case 'Y': {
					int year = cal.get(Calendar.YEAR);
					if( year < 1000 ) buffer.append('0');
					if( year < 100 ) buffer.append('0');
					if( year < 10 ) buffer.append('0');
					buffer.append(year);
				}
				break;
			case 'y':
				buffer.append(cal.get(Calendar.YEAR) % 100 );
				break;
			case 'z':
				buffer.append(cal.get(Calendar.DAY_OF_YEAR));
				break;
			case 'Z':
				buffer.append(cal.get(Calendar.ZONE_OFFSET) / 1000);
				break;
			case '\\':
				i++;
				if( i < pattern.length() ) {
					chr = buffer.charAt(i);
					buffer.append(chr);
				}
				break;
			default:
				buffer.append(chr);
			}
		}
		return buffer.toString();
	}
	
	/**
	 * Prueft, ob ein Wert in einem Array vorhanden ist
	 * 
	 * @param <T> Der Typ von Array und Wert
	 * @param key Der zu suchende Wert
	 * @param list Das Array
	 * @return true, falls der Wert im Array vorhanden ist
	 */
	public static <T> boolean inArray(T key, T[] list) {
		for( int i=0; i < list.length; i++ ) {
			if( (list[i] != null) && list[i].equals(key) ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Prueft, ob ein Wert in einem Array vorhanden ist
	 *
	 * @param key Der zu suchende Wert
	 * @param list Das Array
	 * @return true, falls der Wert im Array vorhanden ist
	 */
	public static boolean inArray(int key, int[] list) {
		for( int i=0; i < list.length; i++ ) {
			if( list[i] == key ) {
				return true;
			}
		}
		return false;
	}
	
	private static Pattern specialChar = Pattern.compile("&([A-Za-z]{0,4}\\w{2,3};|#[0-9]{2,3};)");
	
	/**
	 * Wandelt einen Text in gueltigen HTML-Code um.<br>
	 * Fuer HTML ungueltige Zeichen werden in entsprechende Aequivalente
	 * HTML-Zeichen uebersetzt.<br>
	 * Bereits im Text vorhandene HTML-Zeichen (&amp;...;)  werden nach Moeglichkeit
	 * erhalten.<br>
	 * Zeilenumbrueche werden nicht in &lt;br&gt; umgewandelt
	 * 
	 * @param string der zu formatierende Text
	 * @return der in HTML formatierte Text
	 */
	// http://www.rgagnon.com/javadetails/java-0306.html
	public static String escapeHTML(String string) {
		if( string == null ) {
			return null;
		}
		
		StringBuilder sb = new StringBuilder(string.length());
		// true if last char was blank
		boolean lastWasBlankChar = false;
		int len = string.length();
		char c;

		for( int i = 0; i < len; i++ ) {
			c = string.charAt(i);
			if( c == ' ' ) {
				// blank gets extra work,
				// this solves the problem you get if you replace all
				// blanks with &nbsp;, if you do that you loss
				// word breaking
				if( lastWasBlankChar ) {
					lastWasBlankChar = false;
					sb.append("&nbsp;");
				}
				else {
					lastWasBlankChar = true;
					sb.append(' ');
				}
			}
			else {
				lastWasBlankChar = false;
				//
				// HTML Special Chars
				if( c == '"' ) sb.append("&quot;");
				else if( c == '&' ) {
					int index = string.indexOf(';', i);
					if( (index != -1) && ((index - i < 9) || (string.charAt(i+1) == '#')) ) {
						String str = string.substring(i,index+1);
						if( specialChar.matcher(str).matches() ) {
							sb.append(c);
						}
						else {
							sb.append("&amp;");
						}
					}
					else {
						sb.append("&amp;");
					}
				}
				else if( c == '<' ) sb.append("&lt;");
				else if( c == '>' ) sb.append("&gt;");
				else {
					int ci = 0xffff & c;
					if( ci < 160 )
						// nothing special only 7 Bit
						sb.append(c);
					else {
						// Not 7 Bit use the unicode system
						sb.append("&#");
						sb.append(ci);
						sb.append(';');
					}
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * Formatiert einen Text in HTML-Code. Alle dem <code>BBCodeParser</code>
	 * bekannten Tags werden dafuer genutzt. Zudem werden Zeilenumbrueche behandelt
	 * 
	 * @param text Der zu formatierende Text
	 * @return Der formatierte Text als HTML-Code
	 */
	public static String _text( String text ) {
		return _text(text, null );
	}
	
	/**
	 * Formatiert einen Text in HTML-Code. Alle dem <code>BBCodeParser</code>
	 * bekannten Tags, ausgenommen jene angegebene, werden dafuer genutzt. 
	 * Zudem werden Zeilenumbrueche behandelt
	 * 
	 * @param text Der zu formatierende Text
	 * @param ignore zu ignorierende BBCode-Tags (Namensformat: {@link net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser#parse(String, String[])})
	 * @return Der formatierte Text als HTML-Code
	 */
	public static String _text( String text, String[] ignore ) {
		if( text == null ) {
			return null;
		}
		String result = escapeHTML(text);
		
		result = BBCodeParser.getInstance().parse(result,ignore);

		result = StringUtils.replace(result, "\r\n", "<br />");
		result = StringUtils.replace(result, "\n", "<br />");

		return result;
	}
	
	private static final Pattern stripHTML = Pattern.compile("(<\\/?)(\\w+)([^>]*>)");
	
	/**
	 * Entfernt alle HTML-Tags aus dem String und gibt ihn zurueck
	 * @param text Der STring
	 * @return Der String ohne HTML-Tags
	 */
	public static String _stripHTML( String text ) {
		Matcher m = stripHTML.matcher(text);
		return m.replaceAll("");
	}
	
	/**
	 * Formatiert einen Text in HTML-Code. Zeilenumbrueche werden behandelt,
	 * BBCodes jedoch ignoriert.
	 * @param text Der zu formatierende Text
	 * @return Der formatierte Text als HTML-OCde
	 */
	public static String _plaintext( String text ) {
		String result = escapeHTML(text);

		result = StringUtils.replace(result,"\r\n", "<br />");
		result = StringUtils.replace(result,"\n", "<br />");

		return result;
	}
	
	/**
	 * Liste von BBCodes, welche bei einzeligem Text problematisch sind.
	 */
	private static final String[] _TITLE_NAME = new String[] {"img(1)","url(1)","url(2)","imglf(1)","imgrf(1)","email(1)","email(2)","list(1)","list(2)","size(2)"};
	
	/**
	 * Formatiert einen Text in HTML-Code. Zeilenumbrueche werden ignoriert. Ebenso werden einige
	 * fuer einzeilige Texte problematische BBCodes nicht behandelt.
	 * @param text Der zu formatierende Text
	 * @return Der formatierte Text
	 * @see #_TITLE_NAME
	 */
	public static String _title( String text ) {
		return _title(text, _TITLE_NAME);
	}
	
	/**
	 * Formatiert einen Text in HTML-Code. Zeilenumbrueche werden ignoriert. Ebenso
	 * die zu ignorierenden BBCodes
	 * @param text Der zu formatierende Text
	 * @param ignore Liste der zu ignorierenden BBCodes (Namensformat: {@link net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser#parse(String, String[])})
	 * @return Der formatierte Text
	 * @see #_TITLE_NAME
	 */
	public static String _title( String text, String[] ignore ) {
		if( text == null ) {
			return null;
		}
		String result = escapeHTML(text);
				
		result = BBCodeParser.getInstance().parse(result,ignore);

		return result;
	}
	
	/**
	 * Formatiert einen Text in HTML-Code. Zeilenumbrueche werden ignoriert. Alle BBCodes bleiben erhalten und
	 * werden nicht in HTML umgewandelt oder entfernt.
	 * 
	 * @param text der zu formatierende Text
	 * @return Der formatierte Text
	 * @see #_plaintitle(String)
	 */
	public static String _titleNoFormat( String text ) {
		String result = escapeHTML(text);
		
		result = BBCodeParser.getInstance().parse(result,new String[] {"all"});
		
		return result;
	}
	
	/**
	 * Formatiert einen Text in HTML-Code. Zeilenumbrueche werden ignoriert. Alle BBCodes bleiben erhalten und
	 * werden nicht in HTML umgewandelt oder entfernt.
	 * 
	 * @param text der zu formatierende Text
	 * @return Der formatierte Text
	 * @see #_titleNoFormat(String)
	 */
	public static String _plaintitle( String text ) {
		return escapeHTML(text);
	}
	
	/**
	 * Kodiert einen String mittels md5
	 * 
	 * @param text der zu kodierende String
	 * @return der kodierte String
	 */
	public static String md5( String text ) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(text.getBytes("UTF-8"));
			byte[] md5 = digest.digest();
			StringBuilder hexString = new StringBuilder();
			for( int i=0; i<md5.length; i++ ) {
				hexString.append(Integer.toHexString(0xFF & md5[i]));
			}
			return hexString.toString();
		}
		catch( Exception e ) {
			LOG.error(e,e);
		}
		
		return null;
	}
	
	/**
	 * Schreibt einen Eintrag in eine Logdatei. Wenn die Logdatei
	 * nicht existiert wird nichts gemacht.
	 * 
	 * @param log Name der Logdatei (relativ zum Logpfad
	 * @param text Der zu schreibende Text.
	 */
	public static void writeLog( String log, String text ) {
		File file = new File(Configuration.getSetting("LOXPATH")+log);
		if( file.isFile() ) {
			try {
				BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
				bf.write(text);
				bf.close();
			}
			catch( Exception e ) {
				LOG.error(e,e);
			}
		}
	}

	/**
	 * Gibt einen Formatierten Zeitstring zurueck, welcher angibt,
	 * wie lange es bis zu dem Tick dauert (die Berechnung erfolgt absolut
	 * von 0 aus und nicht vom aktuellen Tick)
	 * 
	 * @param ticks Der zu konvertierende Tick
	 * @return Der Zeitstring
	 */
	public static String ticks2Days( int ticks ) {
		StringBuilder text = new StringBuilder();
		
		int dauer = ticks / 7;
		int wochen = dauer / 7;
		int tage = dauer % 7;
		
		if( wochen > 1 ) {
			text.append(wochen+" Wochen");
		}
		else if( wochen == 1 ) {
			text.append(wochen+" Woche");
		}
					
		if( (tage > 0) && (wochen > 0) ) {
			text.append(", ");
		}
					
		if( tage > 1 ) {
			text.append(tage+" Tage");
		}
		else if( tage == 1 ) {
			text.append(tage+" Tag");
		}
		else if( (wochen == 0) && (tage == 0) ) {
			text.append("wenige Stunden"); 	
		}
	
		return text.toString();
	}
	
	private static int[] monthLengths = {31,28,31,30,31,30,31,31,30,31,30,31};
	
	/**
	 * Konvertiert einen Tick in die Ingamezeitrechnung
	 * 
	 * @param tick Der zu konvertierende Tick
	 * @return Ein String, welcher das Ingame-Datum enthaelt
	 */
	public static String getIngameTime(int tick) {
		int curtime = tick;
		if( curtime < 0 ) {
			curtime = 0;
		}
		
		if( curtime > 0 ) {
			curtime = curtime/3;
		}

		int years = curtime / 365;
		curtime %= 365;
		
		int curpos = 0;

		while( curtime > monthLengths[curpos] ) {
			curtime -= monthLengths[curpos];
			curpos++;
		}

		if( curtime == 0 ) curtime = 1;

		return curtime+". "+months[curpos]+" "+(2372+years);
	}
	
	private static Session mailSession = null;
	private static final String MAIL_SEND_ADDRESS = "ds-admin@drifting-souls.net";
	private static final String MAIL_GUZMAN = "r.hajek@web.de";
	
	/**
	 * Versendet eine Email. Als Senderadresse wird automatisch MAIL_SEND_ADDRESS
	 * und als BCC MAIL_GUZMAN eingetragen.
	 * 
	 * @param address Die Zieladresse
	 * @param subject Der Titel der Mail
	 * @param text Der Inhalt der Email
	 */
	public static synchronized void mail( String address, String subject, String text ) {
		if( mailSession == null ) {
			Properties props = System.getProperties();
			props.put("mail.smtp.host", Configuration.getSetting("SMTP-SERVER"));
			mailSession = Session.getInstance(props, null);		
		}
		Message message = new MimeMessage(mailSession);
		try {
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(address));
			message.setSubject(subject);
			message.setContent(text, "text/plain");
			message.setFrom(new InternetAddress(MAIL_SEND_ADDRESS));
			message.setRecipient(Message.RecipientType.BCC, new InternetAddress(MAIL_GUZMAN));
			message.setReplyTo(new InternetAddress[] {new InternetAddress(MAIL_SEND_ADDRESS)});
			
			Transport.send(message);
		}
		catch( Exception e ) {
			LOG.error("Konnte Email '"+subject+"' an Addresse "+address+" nicht senden: "+e);
		}
	}
	
	/**
	 * Sendet, falls eine Mailadresse angegeben ist, eine Exception als Text an die in der Konfiguration unter
	 * EXCEPTION_MAIL angegebene Email-Adresse
	 * @param t Das Throwable mit den Infos zur Exception
	 * @param addInfo weitere Informationen, welche in der Email angezeigt werden sollen
	 * @param title der Titel der Mail
	 */
	public static void mailThrowable(Throwable t, String title, String addInfo) {
		if( Configuration.getSetting("EXCEPTION_MAIL") == null ) {
			return;
		}
		
		StringBuilder msg = new StringBuilder(100);
		if( (addInfo != null) && (addInfo.length() > 0) ) {
			msg.append(addInfo);
			msg.append("\n\n------------------------------\n");
		}
		
		// Zuerst das innerste nested Throwable ausgeben.
		// Das aeusserste hingegen zuletzt
		Stack<Throwable> nestedThrowables = new Stack<Throwable>();
		do {
			nestedThrowables.push(t);
		}
		while( (t = t.getCause()) != null );
		
		while( !nestedThrowables.isEmpty() ) {
			t = nestedThrowables.pop();
			
			msg.append("\nThrowable: \n");
			msg.append(t+"\n");
			StackTraceElement[] st = t.getStackTrace();
			for( int i=0; i < st.length; i++ ) {
				msg.append(st[i].toString()+"\n");
			}
			msg.append("\n");
		}
		
		String prefix = Configuration.getSetting("EXCEPTION_MAIL_PREFIX");
		
		String[] mailAddrs = StringUtils.split(Configuration.getSetting("EXCEPTION_MAIL"), ';');
		for( int i=0; i < mailAddrs.length; i++ ) {
			Common.mail(mailAddrs[i], prefix+" "+(title != null && title.length() > 0 ? title : "Exception"), msg.toString());
		}
	}

	/**
	 * Kopiert eine Datei
	 * @param source Name der zu kopierenden Datei
	 * @param destination Ziel der Datei (inkl. Dateiname!)
	 */
	public static void copyFile(String source, String destination) {
		/* TODO: Exceptions? */
		try {
			FileChannel sourceChannel = new FileInputStream(new File(source)).getChannel();
			FileChannel destinationChannel = new FileOutputStream(new File(destination)).getChannel();
			sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
			sourceChannel.close();
			destinationChannel.close();
		}
		catch(IOException e) {
			LOG.error(e,e);
		}
	}
	
	/**
	 * Fuegt einen neuen Eintrag in die Log-DB ein. Als ausfuehrender Benutzer wird der in dem Kontext
	 * aktive Benutzer verwendet
	 * 
	 * @param type Der Typ des Eintrags
	 * @param source Die Quelle des Ereignisses
	 * @param target Das Ziel des Ereignisses
	 * @param data Weitere Daten in Form Schluessel, Wert, Schluessel, Wert (die Anzahl muss folglich gerade sein)
	 */
	public static void dblog( String type, String source, String target, String ... data) {
		dblog(type, source, target, 0, data);
	}
	
	/**
	 * Fuegt einen neuen Eintrag in die Log-DB ein
	 * 
	 * @param type Der Typ des Eintrags
	 * @param source Die Quelle des Ereignisses
	 * @param target Das Ziel des Ereignisses
	 * @param userid Der Benutzer, der das Ereigniss ausgeloest hat (falls dieser vom aktiven Benutzer abweicht)
	 * @param data Weitere Daten in Form Schluessel, Wert, Schluessel, Wert (die Anzahl muss folglich gerade sein)
	 */
	public static void dblog( String type, String source, String target, int userid, String ... data ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		if( (userid == 0) && (context.getActiveUser() != null) ) {
			userid = context.getActiveUser().getId();
		}
		
		StringBuilder dataText = new StringBuilder();
		for( int i=0; i < data.length; i+=2 ) {
			dataText.append(data[i]);
			dataText.append(':');
			dataText.append(data[i+1]);
			if( i+1 < data.length ) {
				dataText.append("\n");
			}
		}
		
		db.update("INSERT INTO logging (type,user_id,time,source,target,data)" ,
				"VALUES " ,
				"('",type,"','",userid,"','",time(),"','",source,"','",target,"',",(dataText.length() != 0 ? "'"+dataText+"'" : "NULL"),")");
	}

	/**
	 * Fuegt Zeilenumbrueche ein. Beruecksichtigt werden dabei 
	 * die Woerter und es wird versucht kein Wort auf zwei Zeilen aufzuspalten.
	 * 
	 * @param string Der Text
	 * @param col Die maximale Anzahl an Zeichen in einer Zeile
	 * @param string2 Der Zeilentrenner (z.B. \n)
	 * @return Der Text mit Zeilenumbruechen
	 */
	public static String wordwrap(String string, int col, String string2) {
		if( string == null ) {
			return null;
		}
		StringBuilder result = new StringBuilder(string);
		
		int lastSpace = -1;
		int length = 0;
		for( int i=0; i < result.length(); i++ ) {
			if( length > col ) {
				length = i - lastSpace-1;
				if( lastSpace > -1 ) {
					result.replace(lastSpace, lastSpace+1, string2);
					i+=string2.length()-1;
				}
				else {
					result.insert(i, string2);
					i+=string2.length();
				}
				lastSpace = -1;
				continue;
			}
			if( result.charAt(i) == ' ' ) {
				lastSpace = i;
			}
			length++;
		}
		
		return result.toString();
	}
	
	/**
	 * Inkrementiert einen Integerwert in einer Map um den Wert 1
	 * @param <T> Der Schluesseltyp der Map
	 * @param map Die Map
	 * @param property Der Schluessel, dessen Wert inkrementiert werden soll
	 */
	public static <T> void safeIntInc(Map<T,Integer> map, T property) {
		Integer val = map.get(property);
		if( val == null ) {
			map.put(property, 1);
			return;
		}
		
		map.put(property, val.intValue()+1);		
	}
	
	/**
	 * Inkrementiert einen Integerwert in einer Map um den Wert 1
	 * @param <T> Der Schluesseltyp der Map
	 * @param map Die Map
	 * @param property Der Schluessel, dessen Wert inkrementiert werden soll
	 */
	public static <T> void safeLongInc(Map<T,Long> map, T property) {
		Long val = map.get(property);
		if( val == null ) {
			map.put(property, Long.valueOf(1));
			return;
		}
		
		map.put(property, val.longValue()+1);		
	}
}
