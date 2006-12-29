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

package net.driftingsouls.ds2.framework;

import java.util.LinkedList;
import java.util.Vector;
import java.util.HashMap;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.util.Stack;


/**
 * aTextPart repräsentiert ein Teilstück eines Textes. Dieses Teilstück kann
 * sowohl ein Text selbst oder ein Formatierungstag sein
 * aTextPart wird ausschließlich von JTextField zum speichern von geparsten Texten
 * benutzt.
 * 
 * @author Christopher Jung
 *
 */
class aTextPart {
	public static final int TYPE_TEXT 	= 1;
	public static final int TYPE_COLOR 	= 2;
	public static final int TYPE_BOLD 	= 3;
	public static final int TYPE_FONT 	= 4;
	public static final int TYPE_ITALIC = 5;
	public static final int TYPE_SIZE = 5;
	
	private int type;
	private Object data;
	
	public aTextPart( int type, Object data ) {
		this.type = type;
		this.data = data;
	}
	
	public int getType() {
		return type;
	}
	
	public Object getData() {
		return data;
	}
}

/**
 * JTextField ist ein mehrzeiliges Textfeld (kein Texteingabefeld!), welches
 * mit BBCode formatierten Text anzeigen kann und sich bei bedarf automatisch vergrößert. 
 * 
 * @author bKtHeG (Christopher Jung)
 */
public class JTextField extends JWindow {
	private static HashMap colorMap;
	
	static {
		colorMap = new HashMap();
		// VGA-16 Palette
		colorMap.put( "black", 	new Integer(0x000000) );
		colorMap.put( "maroon",	new Integer(0x800000) );
		colorMap.put( "green", 	new Integer(0x008000) );
		colorMap.put( "olive", 	new Integer(0x808000) );
		colorMap.put( "navy", 	new Integer(0x000080) );
		colorMap.put( "purple", 	new Integer(0x800080) );
		colorMap.put( "teal", 	new Integer(0x008080) );
		colorMap.put( "silver", 	new Integer(0xc0c0c0) );
		colorMap.put( "gray", 	new Integer(0x808080) );
		colorMap.put( "red", 		new Integer(0xFF0000) );
		colorMap.put( "lime", 	new Integer(0x00FF00) );
		colorMap.put( "yellow", 	new Integer(0xFFFF00) );
		colorMap.put( "blue", 	new Integer(0x0000FF) );
		colorMap.put( "fuchsia", 	new Integer(0xFF00FF) );
		colorMap.put( "aqua",		new Integer(0x00FFFF) );
		colorMap.put( "white",	new Integer(0xFFFFFF) );
		
		// Netscape 120
		colorMap.put( "aliceblue",	new Integer(0xF0F8FF) );
		colorMap.put( "antiquewhite",	new Integer(0xFAEBD7) );
		colorMap.put( "aquamarine",	new Integer(0x7FFFD4) );
		colorMap.put( "azure",		new Integer(0xF0FFFF) );
		colorMap.put( "beige",		new Integer(0xF5F5DC) );
		colorMap.put( "blueviolet",	new Integer(0x8A2BE2) );
		colorMap.put( "brown",		new Integer(0xA52A2A) );
		colorMap.put( "burlywood",	new Integer(0xDEB887) );
		colorMap.put( "cadetblue",	new Integer(0x5F9EA0) );
		colorMap.put( "chartreuse",	new Integer(0x7FFF00) );
		colorMap.put( "chocolate",	new Integer(0xD2691E) );
		colorMap.put( "coral",		new Integer(0xFF7F50) );
		colorMap.put( "cornflowerblue",	new Integer(0x6495ED) );
		colorMap.put( "cornsilk",		new Integer(0xFFF8DC) );
		colorMap.put( "crimson",		new Integer(0xDC143C) );
		colorMap.put( "darkblue",		new Integer(0x00008B) );
		colorMap.put( "darkcyan",		new Integer(0x008B8B) );
		colorMap.put( "darkgoldenrod",new Integer(0xB8860B) );
		colorMap.put( "darkgray",		new Integer(0xA9A9A9) );
		colorMap.put( "darkgreen",	new Integer(0x006400) );
		colorMap.put( "darkkhaki",	new Integer(0xBDB76B) );
		colorMap.put( "darkmagenta",	new Integer(0x8B008B) );
		colorMap.put( "darkolivegreen",	new Integer(0x556B2F) );
		colorMap.put( "darkorange",	new Integer(0xFF8C00) );
		colorMap.put( "darkorchid",	new Integer(0x9932CC) );
		colorMap.put( "darkred",		new Integer(0x8B0000) );
		colorMap.put( "darksalmon",	new Integer(0xE9967A) );
		colorMap.put( "darkseagreen",	new Integer(0x8FBC8F) );
		colorMap.put( "darkslateblue",new Integer(0x483D8B) );
		colorMap.put( "darkslategray",new Integer(0x2F4F4F) );
		colorMap.put( "darkturquoise",new Integer(0x00CED1) );
		colorMap.put( "darkviolet",	new Integer(0x9400D3) );
		colorMap.put( "deeppink",		new Integer(0xFF1493) );
		colorMap.put( "deepskyblue",	new Integer(0x00BFFF) );
		colorMap.put( "dimgray",		new Integer(0x696969) );
		colorMap.put( "dodgerblue",	new Integer(0x1E90FF) );
		colorMap.put( "firebrick",	new Integer(0xB22222) );
		colorMap.put( "floralwhite",	new Integer(0xFFFAF0) );
		colorMap.put( "forestgreen",	new Integer(0x228B22) );
		colorMap.put( "gainsboro",	new Integer(0xDCDCDC) );
		colorMap.put( "ghostwhite",	new Integer(0xF8F8FF) );
		colorMap.put( "gold",			new Integer(0xFFD700) );
		colorMap.put( "goldenrod",	new Integer(0xDAA520) );
		colorMap.put( "greenyellow",	new Integer(0xADFF2F) );
		colorMap.put( "honeydew",		new Integer(0xF0FFF0) );
		colorMap.put( "hotpink",		new Integer(0xFF69B4) );
		colorMap.put( "indianred",	new Integer(0xCD5C5C) );
		colorMap.put( "indigo",		new Integer(0x4B0082) );
		colorMap.put( "ivory",		new Integer(0xFFFFF0) );
		colorMap.put( "khaki",		new Integer(0xF0E68C) );
		colorMap.put( "lavender",		new Integer(0xE6E6FA) );
		colorMap.put( "lavenderblush",new Integer(0xFFF0F5) );
		colorMap.put( "lawngreen",	new Integer(0x7CFC00) );
		colorMap.put( "lemonchiffon",	new Integer(0xFFFACD) );
		colorMap.put( "lightblue",	new Integer(0xADD8E6) );
		colorMap.put( "lightcoral",	new Integer(0xF08080) );
		colorMap.put( "lightcyan",	new Integer(0xE0FFFF) );
		colorMap.put( "lightgoldenrodyellow",	new Integer(0xFAFAD2) );
		colorMap.put( "lightgreen",	new Integer(0x90EE90) );
		colorMap.put( "lightgrey",	new Integer(0xD3D3D3) );
		colorMap.put( "lightpink",	new Integer(0xFFB6C1) );
		colorMap.put( "lightsalmon",	new Integer(0xFFA07A) );
		colorMap.put( "lightseagreen",new Integer(0x20B2AA) );
		colorMap.put( "lightskyblue",	new Integer(0x87CEFA) );
		colorMap.put( "lightslategray",	new Integer(0x778899) );
		colorMap.put( "lightsteelblue",	new Integer(0xB0C4DE) );
		colorMap.put( "lightyellow",	new Integer(0xFFFFE0) );
		colorMap.put( "limegreen",	new Integer(0x32CD32) );
		colorMap.put( "linen",		new Integer(0xFAF0E6) );
		colorMap.put( "mediumaquamarine",	new Integer(0x66CDAA) );
		colorMap.put( "mediumblue",	new Integer(0x0000CD) );
		colorMap.put( "mediumorchid",	new Integer(0xBA55D3) );
		colorMap.put( "mediumpurple",	new Integer(0x9370DB) );
		colorMap.put( "mediumseagreen",	new Integer(0x3CB371) );
		colorMap.put( "mediumslateblue",	new Integer(0x7B68EE) );
		colorMap.put( "mediumspringgreen",new Integer(0x00FA9A) );
		colorMap.put( "mediumturquoise",	new Integer(0x48D1CC) );
		colorMap.put( "mediumvioletred",	new Integer(0xC71585) );
		colorMap.put( "midnightblue",	new Integer(0x191970) );
		colorMap.put( "mintcream",	new Integer(0xF5FFFA) );
		colorMap.put( "mistyrose",	new Integer(0xFFE4E1) );
		colorMap.put( "moccasin",		new Integer(0xFFE4B5) );
		colorMap.put( "navajowhite",	new Integer(0xFFDEAD) );
		colorMap.put( "oldlace",		new Integer(0xFDF5E6) );
		colorMap.put( "olivedrab",	new Integer(0x6B8E23) );
		colorMap.put( "orange",		new Integer(0xFFA500) );
		colorMap.put( "orangered",	new Integer(0xFF4500) );
		colorMap.put( "orchid",		new Integer(0xDA70D6) );
		colorMap.put( "palegoldenrod",new Integer(0xEEE8AA) );
		colorMap.put( "palegreen",	new Integer(0x98FB98) );
		colorMap.put( "paleturquoise",new Integer(0xAFEEEE) );
		colorMap.put( "palevioletred",new Integer(0xDB7093) );
		colorMap.put( "papayawhip",	new Integer(0xFFEFD5) );
		colorMap.put( "peachpuff",	new Integer(0xFFDAB9) );
		colorMap.put( "peru",			new Integer(0xCD853F) );
		colorMap.put( "pink",			new Integer(0xFFC0CB) );
		colorMap.put( "plum",			new Integer(0xDDA0DD) );
		colorMap.put( "powderblue",	new Integer(0xB0E0E6) );
		colorMap.put( "rosybrown",	new Integer(0xBC8F8F) );
		colorMap.put( "royalblue",	new Integer(0x4169E1) );
		colorMap.put( "saddlebrown",	new Integer(0x8B4513) );
		colorMap.put( "salmon",		new Integer(0xFA8072) );
		colorMap.put( "sandybrown",	new Integer(0xF4A460) );
		colorMap.put( "seagreen",		new Integer(0x2E8B57) );
		colorMap.put( "seashell",		new Integer(0xFFF5EE) );
		colorMap.put( "sienna",		new Integer(0xA0522D) );
		colorMap.put( "skyblue",		new Integer(0x87CEEB) );
		colorMap.put( "slateblue",	new Integer(0x6A5ACD) );
		colorMap.put( "slategray",	new Integer(0x708090) );
		colorMap.put( "snow",			new Integer(0xFFFAFA) );
		colorMap.put( "springgreen",	new Integer(0x00FF7F) );
		colorMap.put( "steelblue",	new Integer(0x4682B4) );
		colorMap.put( "tan",			new Integer(0xD2B48C) );
		colorMap.put( "thistle",		new Integer(0xD8BFD8) );
		colorMap.put( "tomato",		new Integer(0xFF6347) );
		colorMap.put( "turquoise",	new Integer(0x40E0D0) );
		colorMap.put( "violet",		new Integer(0xEE82EE) );
		colorMap.put( "wheat",		new Integer(0xF5DEB3) );
		colorMap.put( "whitesmoke",	new Integer(0xF5F5F5) );
		colorMap.put( "yellowgreen",	new Integer(0x9ACD32) );
		
		// Weitere (weil einige sich immer vertippen)
		colorMap.put( "grey", 	new Integer(0x808080) );
	}
	
	private LinkedList lineList;
	private Vector textHeights;
	private BufferedImage textImage;
	
	private String fontName;
	
	private String dialogText;
	private int wantedHeight;
	private int oldWidth;
	
	/**
	 * Erzeugt ein neues Textfeld
	 * 
	 * @param parent		Das Elternfenster
	 * @param windowmanager	Der Fenstermanager
	 * @param myfont		Die zu verwendende Schriftart
	 */
	public JTextField( JWindow parent, IWindowManager windowmanager, String myfont ) {
		super( parent, windowmanager );
		
		lineList = null;
		textImage = null;
		dialogText = " ";
		textHeights = new Vector();
		wantedHeight = getClientHeight();
		
		fontName = myfont;
		
		oldWidth = getClientWidth();
	}
	
	/**
	 * Verarbeitet den Text und wandelt ihn in das interne Darstellungsformat um
	 * 
	 * @param g	Der zu verwendende Grafikkontext
	 */
	private void parseText( Graphics2D g ) {
		// Text berechnen
	
		lineList = new LinkedList();
		
		Stack colors = new Stack();
		colors.push(new Integer(0xc7c7c7));
		
		Stack fonts = new Stack();
		fonts.push( g.getFont() );
		
		textHeights = new Vector();
		
		dialogText = dialogText.replaceAll("&auml;","ä");
		dialogText = dialogText.replaceAll("&ouml;","ö");
		dialogText = dialogText.replaceAll("&uuml;","ü");
		dialogText = dialogText.replaceAll("&szlig;","ß");
		dialogText = dialogText.replaceAll("&#039;","'");
		
		StringBuffer dialogText = new StringBuffer(this.dialogText);
		
		while( dialogText.length() > 0 ) {
			int pos = dialogText.indexOf("\n");
			if( pos == -1 ) {
				pos = dialogText.length();
			}
			String currentline = dialogText.substring(0,pos);
			dialogText.delete(0,pos+1);
			
			if( currentline.equals("") && (dialogText.length() == 0) ) {
				break;
			}
	
		/*String lineList[] = dialogText.split("\n");
		for( int i=0; i < lineList.length; i++ ) {
			String currentline = lineList[i];*/
			Vector textvector = new Vector();
			StringBuffer tmpText = new StringBuffer();
			int lineLength = 0;
			StringBuffer word = new StringBuffer();
			boolean needWhiteSpace = false;
			boolean wasWhiteSpace = false;
			int currentHeight = g.getFontMetrics().getHeight()+2;
			
			for( int j = 0; j < currentline.length(); j++ ) {					
				if( currentline.charAt(j) != '[' ) {
					if( currentline.charAt(j) != ' ' ) {
						word.append(currentline.charAt(j));
						wasWhiteSpace = false;
					}
					else {
						if( g.getFontMetrics().stringWidth(tmpText.toString()+' '+word.toString())+lineLength >= getClientWidth() ) {
							textvector.add(new aTextPart(aTextPart.TYPE_TEXT,tmpText));
							this.lineList.add(textvector);

							textHeights.add( new Integer(currentHeight) );
							currentHeight = g.getFontMetrics().getHeight()+2;

							textvector = new Vector();
							lineLength = 0;
							tmpText = new StringBuffer(word);
						}
						else if( !word.toString().trim().equals("") ) {
							if( !tmpText.equals("") || (needWhiteSpace && (lineLength > 0)) ) {
								tmpText.append(" ");
								needWhiteSpace = false;
							}
							tmpText.append(word);
						}
						else if( (lineLength > 0) && !needWhiteSpace ) {
							needWhiteSpace = true;
						}
						
						word = new StringBuffer();
						
						
						wasWhiteSpace = true;
					}
				}
				else {
					if( !tmpText.equals("") || !word.equals("") ) {
						if( g.getFontMetrics().stringWidth(tmpText.toString()+' '+word.toString())+lineLength >= getClientWidth() ) {
							textvector.add(new aTextPart(aTextPart.TYPE_TEXT,tmpText));
							this.lineList.add(textvector);

							textHeights.add( new Integer(currentHeight) );
							currentHeight = g.getFontMetrics().getHeight()+2;
							
							textvector = new Vector();
							lineLength = 0;
							tmpText = new StringBuffer(word);
						}
						else if( !word.toString().trim().equals("") ) {
							if( !tmpText.equals("") || (needWhiteSpace && (lineLength > 0)) ) {
								tmpText.append(' ');
								needWhiteSpace = false;
							}
							tmpText.append(word);
						}
						textvector.add(new aTextPart(aTextPart.TYPE_TEXT,tmpText));

						lineLength += g.getFontMetrics().stringWidth(tmpText.toString());
						tmpText = new StringBuffer();
						word = new StringBuffer();
					}
					
					if( wasWhiteSpace ) {
						needWhiteSpace = true;
					}
					
					StringBuffer command = new StringBuffer();
					
					while( (j < currentline.length()) && (currentline.charAt(j) != ']') ) {
						command.append(currentline.charAt(j));
						
						j++;
					}
					
					command.append(currentline.charAt(j));
					
					// Command ueberpruefen
					int cmd = 0;
					boolean endtag = false;
					
					if( currentline.charAt(j) == ']' ) {
						String lowerc = command.toString().toLowerCase();
						
						if( lowerc.indexOf("[color=") != -1 ) {
							cmd = aTextPart.TYPE_COLOR;
						}
						else if( lowerc.indexOf("[/color]") != -1 ) {
							cmd = aTextPart.TYPE_COLOR;
							endtag = true;
						}
						else if( lowerc.indexOf("[b]") != -1 ) {
							cmd = aTextPart.TYPE_BOLD;
						}
						else if( lowerc.indexOf("[/b]") != -1 ) {
							cmd = aTextPart.TYPE_BOLD;
							endtag = true;
						}
						else if( lowerc.indexOf("[font=") != -1 ) {
							cmd = aTextPart.TYPE_FONT;
						}
						else if( lowerc.indexOf("[/font]") != -1 ) {
							cmd = aTextPart.TYPE_FONT;
							endtag = true;
						}
						else if( lowerc.indexOf("[i]") != -1 ) {
							cmd = aTextPart.TYPE_ITALIC;
						}
						else if( lowerc.indexOf("[/i]") != -1 ) {
							cmd = aTextPart.TYPE_ITALIC;
							endtag = true;
						}
						else if( lowerc.indexOf("[size=") != -1 ) {
							cmd = aTextPart.TYPE_SIZE;
						}
						else if( lowerc.indexOf("[/size]") != -1 ) {
							cmd = aTextPart.TYPE_SIZE;
							endtag = true;
						}
					}
					
					/*
					 * 
					 * 	[color=.....] verarbeiten
					 * 
					 */
					
					if( (cmd == aTextPart.TYPE_COLOR) && (endtag == false) ) {
						Integer colorvalue = new Integer(0);
						
						int index = command.indexOf("=");
						String colorcode = command.substring( index+1, command.length()-1 );
						
						if( colorcode.charAt(0) == '"' ) {
							colorcode = colorcode.substring( 1, colorcode.length()-1 );
						}
						
						if( colorcode.charAt(0) == '#' ) {
							colorcode = command.substring( index+2, command.length()-1 );
							
							try {
								colorvalue = Integer.decode("0x"+colorcode);
							}
							catch( NumberFormatException e ) {
								System.out.println("Ungueltiger Farbwert in [color="+colorcode+"]");
								System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
								
								colorvalue = new Integer(0);
							}
						}
						else if( colorMap.get(colorcode.toLowerCase()) != null ) {
							colorvalue = (Integer)colorMap.get(colorcode.toLowerCase());
						}
						else {
							try {
								colorvalue = Integer.decode("0x"+colorcode);
							}
							catch( NumberFormatException e ) {
								System.out.println("Ungueltiger Farbwert in [color="+colorcode+"]");
								System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
								
								colorvalue = new Integer(0);
							}
						}

						colors.push(colorvalue);

						textvector.add(new aTextPart(aTextPart.TYPE_COLOR,colorvalue));
					}
					else if( cmd == aTextPart.TYPE_COLOR ) {	
						if( colors.size() > 1 ) {
							colors.pop(); //Tag "schliessen"
							
							Integer colorvalue = (Integer)colors.peek();
							
							textvector.add(new aTextPart(aTextPart.TYPE_COLOR,colorvalue));
						}
						else {
							System.out.println("ERROR: unerwarteter [/color]-Tag");
						}
					}
					/*
					 * 
					 * 	[b]...[/b] verarbeiten
					 * 
					 */
					else if( (cmd == aTextPart.TYPE_BOLD) && (endtag == false) ) {
						Font newFont = new Font(g.getFont().getFamily(), g.getFont().getStyle() | Font.BOLD, g.getFont().getSize() );
						
						fonts.push(newFont);
						
						g.setFont(newFont);
						
						if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
							currentHeight = g.getFontMetrics().getHeight()+2;
						}
						
						textvector.add(new aTextPart(aTextPart.TYPE_BOLD,newFont));
					}
					else if( cmd == aTextPart.TYPE_BOLD ) {
						if( fonts.size() > 1 ) {
							fonts.pop(); //Tag "schliessen"
							
							Font oldFont = (Font)fonts.peek();
							
							g.setFont(oldFont);
							
							if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
								currentHeight = g.getFontMetrics().getHeight()+2;
							}
							
							textvector.add(new aTextPart(aTextPart.TYPE_BOLD,oldFont));
						}
						else {
							System.out.println("ERROR: unerwarteter [/b]-Tag");
						}
					}
					/*
					 * 
					 * 	[font=.....] verarbeiten
					 * 
					 */
					else if( (cmd == aTextPart.TYPE_FONT) && (endtag == false) ) {
						int index = command.indexOf("=");
						String fontname = command.substring( index+1, command.length()-1 );
						
						if( fontname.charAt(0) == '"' ) {
							fontname = fontname.substring( 1, fontname.length()-1 );
						}

						Font newFont = new Font(fontname, g.getFont().getStyle(), g.getFont().getSize() );

						fonts.push(newFont);
						
						g.setFont(newFont);
						
						if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
							currentHeight = g.getFontMetrics().getHeight()+2;
						}
						
						textvector.add(new aTextPart(aTextPart.TYPE_FONT,newFont));
					}
					else if( cmd == aTextPart.TYPE_FONT ) {	
						if( fonts.size() > 1 ) {
							fonts.pop(); //Tag "schliessen"
							
							Font oldFont = (Font)fonts.peek();
							
							g.setFont(oldFont);
							
							if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
								currentHeight = g.getFontMetrics().getHeight()+2;
							}
							
							textvector.add(new aTextPart(aTextPart.TYPE_FONT,oldFont));
						}
						else {
							System.out.println("ERROR: unerwarteter [/font]-Tag");
						}
					}
					/*
					 * 
					 * 	[i]...[/i] verarbeiten
					 * 
					 */
					else if( (cmd == aTextPart.TYPE_ITALIC) && (endtag == false) ) {
						Font newFont = new Font(g.getFont().getFamily(), g.getFont().getStyle() | Font.ITALIC, g.getFont().getSize() );
						
						fonts.push(newFont);
						
						g.setFont(newFont);
						
						if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
							currentHeight = g.getFontMetrics().getHeight()+2;
						}
						
						textvector.add(new aTextPart(aTextPart.TYPE_ITALIC,newFont));
					}
					else if( cmd == aTextPart.TYPE_ITALIC ) {
						if( fonts.size() > 1 ) {
							fonts.pop(); //Tag "schliessen"
							
							Font oldFont = (Font)fonts.peek();
							
							g.setFont(oldFont);
							
							if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
								currentHeight = g.getFontMetrics().getHeight()+2;
							}
							
							textvector.add(new aTextPart(aTextPart.TYPE_ITALIC,oldFont));
						}
						else {
							System.out.println("ERROR: unerwarteter [/i]-Tag");
						}
					}
					/*
					 * 
					 * 	[size=.....] verarbeiten
					 * 
					 */
					else if( (cmd == aTextPart.TYPE_SIZE) && (endtag == false) ) {
						int index = command.indexOf("=");
						String fontsize = command.substring( index+1, command.length()-1 );
						
						if( fontsize.charAt(0) == '"' ) {
							fontsize = fontsize.substring( 1, fontsize.length()-1 );
						}
						
						Integer size = null;

						try {
							size = Integer.decode(fontsize);
						}
						catch( NumberFormatException e ) {
							System.out.println("Ungueltiger Schriftgroessenwert in [size="+fontsize+"]");
							System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
							
							size = new Integer(10);
						}
						
						Font newFont = new Font(g.getFont().getFamily(), g.getFont().getStyle(), size.intValue() );

						fonts.push(newFont);
						
						g.setFont(newFont);
						
						if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
							currentHeight = g.getFontMetrics().getHeight()+2;
						}
						
						textvector.add(new aTextPart(aTextPart.TYPE_SIZE,newFont));
					}
					else if( cmd == aTextPart.TYPE_SIZE ) {	
						if( fonts.size() > 1 ) {
							fonts.pop(); //Tag "schliessen"
							
							Font oldFont = (Font)fonts.peek();
							
							g.setFont(oldFont);
							
							if( g.getFontMetrics().getHeight()+2 > currentHeight ) {
								currentHeight = g.getFontMetrics().getHeight()+2;
							}
							
							textvector.add(new aTextPart(aTextPart.TYPE_SIZE,oldFont));
						}
						else {
							System.out.println("ERROR: unerwarteter [/size]-Tag");
						}
					}
					/*
					 * 
					 *  Kein command -> Text ganz normal ausgeben
					 * 
					 */
					else {
						for( int k = 0; k < command.length(); k++ ) {
							if( command.charAt(k) != ' ' ) {
								word.append(command.charAt(k));
								wasWhiteSpace = false;
							}
							else {
								if( g.getFontMetrics().stringWidth(tmpText.toString()+' '+word)+lineLength >= getClientWidth() ) {
									textvector.add(new aTextPart(aTextPart.TYPE_TEXT,tmpText));
									this.lineList.add(textvector);

									textHeights.add( new Integer(currentHeight) );
									currentHeight = g.getFontMetrics().getHeight()+2;

									textvector = new Vector();
									lineLength = 0;
									tmpText = new StringBuffer(word);
								}
								else if( !word.toString().trim().equals("") ) {
									if( tmpText.equals("") || (needWhiteSpace && (lineLength > 0)) ) {
										tmpText.append(" ");
										needWhiteSpace = false;
									}
									tmpText.append(word);
								}
								else if( (lineLength > 0) && !needWhiteSpace ) {
									word = new StringBuffer(" ");
								}
								word = new StringBuffer();
								
								wasWhiteSpace = true;
							}
						}
						command = null;
					}
				}
			}
			
			if( !tmpText.equals("") || !word.equals("") ) {
				if( g.getFontMetrics().stringWidth(tmpText.toString()+word.toString())+lineLength >= getClientWidth() ) {
					textvector.add(new aTextPart(aTextPart.TYPE_TEXT,tmpText));
					this.lineList.add(textvector);

					textHeights.add( new Integer(currentHeight) );
					currentHeight = g.getFontMetrics().getHeight()+2;

					textvector = new Vector();
					lineLength = 0;
					tmpText = new StringBuffer(word);
				}
				else if( !word.toString().trim().equals("") ) {
					if( !tmpText.equals("") || (needWhiteSpace && (lineLength > 0)) ) {
						tmpText.append(' ');
						needWhiteSpace = false;
					}
					tmpText.append(word);
				}
				
				textvector.add(new aTextPart(aTextPart.TYPE_TEXT,tmpText));
			}
			
			this.lineList.add(textvector);
			textvector = new Vector();
				
			textHeights.add( new Integer(currentHeight) );
			currentHeight = g.getFontMetrics().getHeight()+2;
		}
	}
	
	/**
	 * Setzt den anzuzeigenden Text des Textfelds.
	 * Nach dem setzen wird die Größe des Textfeldes bei bedarf angepasst und
	 * das Textfeld selbst neugezeichnet
	 * 
	 * @param text	Der anzuzeigende Text
	 */
	public synchronized void setText( String text ) {
		boolean needresize = false;
			
		dialogText = text.trim();
		lineList = null;
		textImage = null;

		textImage = getImageCache().createImg(getClientWidth(),getClientHeight(), Transparency.TRANSLUCENT );
			
		Graphics2D localg = textImage.createGraphics();
			
		// Schrift einstellen
	
		localg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
		localg.setFont(new Font(fontName,0,12));
						
		localg.setColor(new Color(0xc7c7c7) );
		
		parseText( localg );
		
		// Hoehe anpassen
		int height = -2;
		for( int i = 0; i < textHeights.size(); i++ ) {
			height += ((Integer)textHeights.get(i)).intValue();
		}
		
		if( height <= 0 ) {
			height = 1;
		}
		
		if( height != getClientHeight() ) {
			needresize = true;
			wantedHeight = height;
		}
		
		localg.dispose();
		
		//
		// Ein neues Bild mit den korrekten Abmessungen erstellen und zeichen
		//
		
		textImage = getImageCache().createImg(getClientWidth(),height, Transparency.TRANSLUCENT );
			
		localg = textImage.createGraphics();
	
		// Schrift einstellen
	
		localg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
		localg.setFont(new Font(fontName,0,12));
						
		localg.setColor(new Color(0xc7c7c7) );
		
		
		// Text ausgeben
		
		int yPosition = -4;
		int xPosition = 0;
		aTextPart txtpart = null;
		Vector linevector = null;
		
		for( int i=0; i < lineList.size(); i++ ) {
			xPosition = 0;
			yPosition += ((Integer)textHeights.get(i)).intValue();
			
			linevector = (Vector)lineList.get(i);
			
			for( int j = 0; j < linevector.size(); j++ ) {
				txtpart = (aTextPart)linevector.get(j);
				
				if( txtpart.getType() == aTextPart.TYPE_TEXT ) {
					localg.drawString(txtpart.getData().toString(),xPosition,yPosition);
					xPosition += localg.getFontMetrics().stringWidth(txtpart.getData().toString());
				}
				else if( txtpart.getType() == aTextPart.TYPE_COLOR ) {
					localg.setColor( new Color( ((Integer)txtpart.getData()).intValue() ) );
				}
				else if( txtpart.getType() == aTextPart.TYPE_BOLD ) {
					localg.setFont( (Font)txtpart.getData() );
				}
				else if( txtpart.getType() == aTextPart.TYPE_FONT ) {
					localg.setFont( (Font)txtpart.getData() );
				}
				else if( txtpart.getType() == aTextPart.TYPE_ITALIC ) {
					localg.setFont( (Font)txtpart.getData() );
				}
				else if( txtpart.getType() == aTextPart.TYPE_SIZE ) {
					localg.setFont( (Font)txtpart.getData() );
				}
			}
		}
			
		localg.dispose();
		lineList = null;
		textHeights.clear();
		
		if( needresize ) {
			setSize(getClientWidth(),height);
		}
		getWindowManager().requestRedraw(this);
	}
	
	public void onResize() {
		if( (dialogText != "") && (getClientWidth() != oldWidth) ) {
			setText(dialogText);
		}
		
		oldWidth = getClientWidth();
		
		super.onResize();
	}
	
	public void paint(Graphics2D g) {				
		super.paint(g);
		
		if( textImage == null ) {
			setText(dialogText);
		}
		
		g.drawImage(textImage,0,0,null);		
	}
}