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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.text.NumberFormat;

/**
 * JShipInfo stellt ein Schiffsinfofeld da. Das Format ist hierbei im Moment immer
 * folgendes: Links das Bild (falls vorhanden) und rechts der Text.
 * Für die Anzeige des Texts wird ein JTextField verwendet.
 * 
 * Gültige Schiffswerte sind im Moment:
 * picture
 * name
 * hull			type/hull
 * shields		type/shields
 * crew			type/crew
 * s			
 * usedcargo	type/cargo
 *  
 * @author bKtHeG (Christopher Jung)
 *
 */
public class JShipInfo extends JWindow {
	private HashMap properties;
	private JTextField textfield;
	private int imageWidth;
	private boolean changed;
	
	/**
	 * Erzeugt ein neues Schiffsinfofeld
	 * 
	 * @param parent		Das Elternfenster
	 * @param windowmanager	Der Windowmanager
	 * @param fontname		Der Name der zu verwendenden Schriftart
	 */
	public JShipInfo( JWindow parent, IWindowManager windowmanager, String fontname ) {
		super( parent, windowmanager );
		
		properties = new HashMap();
		
		textfield = new JTextField(this, getWindowManager(), fontname );
		
		imageWidth = 130;
		changed = false;
	}
	
	public void onResize() {
		int w = 0;
		
		BufferedImage img = null;
		float newSize = 0;
		
		if( changed ) {
			changed = false;
			recalculateText();
		}
		
		if( getProperty("picture") != null ) {
			img = getImageCache().getImage(getProperty("picture"),false);
			
			if( img.getWidth() > imageWidth ) {
				newSize = (float)img.getHeight()*((float)imageWidth/(float)img.getWidth());
			}
			else {
				newSize = img.getHeight();
			}
			
			w += imageWidth;
			w += 5;
		}
		
		textfield.setPosition(w,0);
		textfield.setSize(getClientWidth()-w,getClientHeight());
		
		if( img == null ) {
			setClientSize(getClientWidth(),textfield.getHeight());
		}
		else {
			setClientSize(getClientWidth(),Math.max(textfield.getHeight(),(int)newSize));
		}
		getWindowManager().requestRedraw(this);
	}
	
	/**
	 * Setzt eine Schiffseigenschaft auf einen Wert
	 * 
	 * @param name	Der Name der Schiffseigenschaft
	 * @param value	Der neue Wert
	 */
	public void setProperty( String name, String value ) {
		properties.put(name,value);
		changed = true;
		getWindowManager().requestRedraw(this);
	}
	
	/**
	 * Liefert den Wert einer Schiffseigenschaft
	 * @param name	Der Name der Schiffseigenschaft
	 * @return Der Wert der Schiffseigenschaft
	 */
	public String getProperty( String name ) {
		return (String)properties.get(name);
	}
	
	/**
	 * Liefert den Wert einer Schiffseigenschaft als Integer zurück
	 * 
	 * @param name	Der Name der Schiffseigenschaft
	 * @return Der Wert der Schiffseigenschaft in Integer-Form
	 */
	public int getIntegerProperty( String name ) {
		try {
			String text = (String)properties.get(name);
			if( text == null ) {
				text = "0";
			}
			
			Integer value = Integer.decode(text);
			
			return value.intValue();
		}
		catch( NumberFormatException e ) {
			System.out.println("Ungueltiger Zahlenwert in JShipInfo::getIntegerProperty("+name+")");
			System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
			
			return 0;
		}
	}
	
	/**
	 * Berechnet den anzuzeigenden Text
	 */
	private void recalculateText() {
		BufferedImage img = null;
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(true);
		
		if( getProperty("picture") != null ) {
			img = getImageCache().getImage(getProperty("picture"),false);
		}
		
		String text = "";
		String color = "";
		if( (getProperty("relation") != null) && getProperty("relation").equals("enemy") ) {
			color = "#FF3F3F";
		}
		else if( (getProperty("relation") != null) && getProperty("relation").equals("ally") ) {
			color = "#5F72FF";
		}
		
		if( getProperty("name") != null ) {
			if( color.length() > 0 ) {
				text += "[color="+color+"]";
			}
			text += "[b][size=14]"+getProperty("name")+"[/size][/b]\n";
			if( color.length() > 0 ) {
				text += "[/color]";
			}
		}
		
		if( (getProperty("relation") != null) && !getProperty("relation").equals("owner") && 
				(getProperty("ownername") != null) ) {
			text += getProperty("ownername")+"\n";
		}
		
		if( getProperty("hull") != null ) {
			text += "Hülle: "+nf.format(getIntegerProperty("hull"));
			if( getProperty("type/hull") != null ) {
				text += "/"+nf.format(getIntegerProperty("type/hull"));
			}
			text += "\n";
		}
		
		if( (getProperty("shields") != null) && (getIntegerProperty("type/shields") > 0) ) {
			text += "Schilde: "+nf.format(getIntegerProperty("shields"));
			text += "/"+nf.format(getIntegerProperty("type/shields"));
			text += "\n";
		}
		
		if( (getProperty("crew") != null) && (getIntegerProperty("type/crew") > 0) ) {
			text += "Crew: ";
			if( getIntegerProperty("crew") == 0 ) {
				text += "[color=red]";
			}
			
			text += nf.format(getIntegerProperty("crew"));
			text += "/"+nf.format(getIntegerProperty("type/crew"));
			
			if( getIntegerProperty("crew") == 0 ) {
				text += "[/color]";
			}
			
			text += "\n";
		}
		
		if( getIntegerProperty("s") > 0 ) {
			text += "Hitze: "+nf.format(getIntegerProperty("s"));
			text += "\n";
		}
		
		if( (getProperty("usedcargo") != null) && (getIntegerProperty("type/cargo") > 0) ) {
			text += "Cargo: "+nf.format(getIntegerProperty("usedcargo"));
			text += "/"+nf.format(getIntegerProperty("type/cargo"));
			text += "\n";
		}

		textfield.setText(text);
		
		onResize();
	}
	
	/**
	 * Setzt die Breite der Schiffsbilder fest
	 * 
	 * @param width	Die Breite in Pixeln
	 */
	public void setImageWidth( int width ) {
		imageWidth = width;
	}
	
	public void paint(Graphics2D g) {	
		if( changed ) {
			changed = false;
			recalculateText();
		}

		if( getProperty("picture") != null ) {
			BufferedImage img = getImageCache().getImage(getProperty("picture"),false);
			
			if( img.getWidth() > imageWidth ) {
				float newSize = (float)img.getHeight()*((float)imageWidth/(float)img.getWidth());
				
				g.drawImage( img, 0, 0, imageWidth, (int)newSize, null );
			}
			else {
				g.drawImage( img, 0, 0, null );
			}
			
			if( textfield.getX()-getClientX() != imageWidth+5 ) {
				recalculateText();
			}
		}
	}
}