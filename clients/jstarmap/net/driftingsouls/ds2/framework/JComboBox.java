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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.InputEvent;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Iterator;

import java.awt.Rectangle;

/**
 * aComboBoxWindow ist eine Hilfsklasse für JComboBox.
 * Die Klasse dient dazu das Dropdown-Fenster mit Inhalt anzuzeigen
 * und Ereignisse dort zu verarbeiten
 * 
 * @author Christopher Jung
 */
class aComboBoxWindow extends JDialog {
	private Vector items;
	private Vector data;
	private JComboBox comboBox;
	private int lineHeight;
	private Object selectedItem;
	
	public aComboBoxWindow( JWindow parent, IWindowManager windowmanager, JComboBox masterbox ) {
		super( parent, windowmanager );
		
		comboBox = masterbox;
		selectedItem = null;
		
		items = new Vector();
		data = new Vector();
		
		Graphics2D localg = getImageCache().createImg(1, 1, Transparency.OPAQUE ).createGraphics();
		
		// Schrift einstellen
		localg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		localg.setFont( new Font( getWindowManager().getDefaultFont(), 0, 12 ) );			
		localg.setColor( new Color(0xc7c7c7) );
		
		lineHeight = localg.getFontMetrics().getHeight()+2;
	}
	
	public void onResize() {
		super.onResize();
		
		if( items.size()*lineHeight > getClientHeight() ) {
			getWindowManager().setWindowVClientMinSize( this, items.size()*lineHeight-getClientHeight() );
		}
		else {
			getWindowManager().setWindowVClientMinSize( this, 0 );
		}
	}
	
	public void addItem( String itemname, Object itemdata ) {
		items.add( itemname );
		data.add( itemdata );
		
		if( items.size()*lineHeight > getClientHeight() ) {
			getWindowManager().setWindowVClientMinSize( this, items.size()*lineHeight-getClientHeight() );
		}
		else {
			getWindowManager().setWindowVClientMinSize( this, 0 );
		}
	}
	
	public void clear() {
		items = new Vector();
		data = new Vector();
		
		getWindowManager().setWindowVClientMinSize( this, 0 );
	}
	
	public Object getSelectedData() {
		return selectedItem;
	}
	
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed( x, y, button );
		
		if( button == InputEvent.BUTTON1_MASK ) {
			if( lineHeight == 0 ) {
				return result;
			}
			
			if( (x < 0) || (x > getClientX()) || (y < 0) || (y > getClientY()) ) {
				return result;
			}
			
			int line = (y-getVScrollOffset()) / lineHeight;
			if( line < data.size() ) {
				selectedItem = data.get(line);
			}
			else {
				selectedItem = null;
			}
			
			if( selectedItem != null ) {
				comboBox.handleEvent( getHandle(), "selected" );
			}
		}
		
		return result;
	}
	
	public void paint( Graphics2D g ) {
		super.paint(g);
		
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font(getWindowManager().getDefaultFont(),0,12));
		g.setColor(new Color(0xc7c7c7) );
		
		lineHeight = g.getFontMetrics().getHeight()+2;
		
		int x = getClientX() - getX();
		int y = getClientY() - getY();
		
		g.setClip( new Rectangle(x, y, getClientWidth(), getClientHeight() ) );
		
		int yPosition = y+g.getFontMetrics().getHeight()-2+getVScrollOffset();
		
		for( int i=0; i < items.size(); i++ ) {
			g.drawString( (String)items.get(i), x, yPosition );
			
			yPosition += g.getFontMetrics().getHeight() + 2;
		}
	}
}

/**
 * JComboBox repräsentiert eine ComboBox. 
 * Den ausklappbaren Teil der Combobox übernimmt dabei {@link aComboBoxWindow}.
 * Wenn die Auswahl der Combobox geändert wird, wird ein "selected" Ereignis ausgelöst
 * 
 * @author bKtHeG (Christopher Jung)
 *
 */

public class JComboBox extends JWindow {
	private LinkedHashMap names;
	private LinkedHashMap data;
	private int index;
	private int selected;
	
	private aComboBoxWindow cBoxWindow;
	
	// Offset des Textes usw in der Hintergrundgrafik
	private int xOffset;
	private int yOffset;

	/**
	 * Erstellt eine neue ComboBox
	 * @param parent		Das Elternfenster
	 * @param windowmanager	Der Fenstermanager
	 */
	public JComboBox(JWindow parent, IWindowManager windowmanager) {
		super(parent, windowmanager);
		
		index = 0;
		selected = -1;
		xOffset = -1;
		yOffset = -1;
		
		cBoxWindow = null;
		
		names = new LinkedHashMap();
		data = new LinkedHashMap();
		
		getImageCache().getImage("interface/jstarmap/icon_dropdown.png", true);
		getImageCache().getImage("interface/jstarmap/vertiefung.png", true);
	}
	
	public void onResize() {
		super.onResize();
		
		if( getImageCache().isLoaded("interface/jstarmap/vertiefung.png") ) {
			xOffset = getImageCache().getImage("interface/jstarmap/vertiefung.png", true).getWidth();
			xOffset = Math.round((float)getClientWidth()/(float)xOffset * 8);
		
			yOffset = getImageCache().getImage("interface/jstarmap/vertiefung.png", true).getHeight();
			yOffset = Math.round((float)getClientHeight()/(float)yOffset * 7);
		}
	}
	
	public void onChangeVisibility( boolean vis ) {
		super.onChangeVisibility(vis);
		
		if( !vis && (cBoxWindow != null) ) {
			cBoxWindow.dispose();
			cBoxWindow = null;
		}
	}
	
	/**
	 * Fügt ein neues Element zur Combobox hinzu.
	 * Sollte noch kein Element ausgewählt sein, wird automatisch das
	 * neue Element ausgewählt.
	 * 
	 * @param name	Der anzuzeigende Name des Elements
	 * @param data	Die mit dem Element verbundenen Daten
	 * 
	 * @return Der Index des Elements
	 */
	public int addElement( String name, Object data ) {
		this.names.put( new Integer(index), name );
		this.data.put( new Integer(index), data );
		
		if( selected < 0 ) {
			selected = index;
		}
		
		return index++;
	}
	
	/**
	 * Entfernt ein Element aus der Combobox
	 * 
	 * @param index	Der Index des zu entfernenden Elements
	 */
	public void removeElement( int index ) {
		names.remove( new Integer(index) );
		data.remove( new Integer(index) );
		
		if( index == selected ) {
			selected = -1;
			getWindowManager().requestRedraw();
		}
	}
	
	/**
	 * Setzt das ausgewählte Element auf ein spezifisches Element
	 * 
	 * @param index	Der Index des auszuwählenden Elements
	 */
	public void setSelectedElement( int index ) {
		if( names.get(new Integer(index)) != null ) {
			selected = index;
		}
	}
	
	/**
	 * Liefert das im Moment ausgewählte Element. Das Ergebnis ist -1 falls kein
	 * Element ausgewählt wurde.
	 * 
	 * @return	Der Index des ausgewählten Elements
	 */
	public int getSelectedElement() {
		return selected;
	}
	
	/**
	 * Liefert die Objektdaten eines bestimmten Elements zurück
	 * 
	 * @param index	Der Index des Elements
	 * 
	 * @return Die zum Element gehörenden Objektdaten
	 * @see #getElementString
	 */
	public Object getElementData( int index ) {
		return data.get(new Integer(index));
	}
	
	/**
	 * Liefert den Elementtext eines bestimmten Elements zurück
	 * 
	 * @param index	Der Index des Elements
	 * 
	 * @return Der Elementtext
	 * 
	 * @see #getElementData
	 */
	public String getElementString( int index ) {
		return (String)names.get(new Integer(index));
	}
	
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed( x, y, button );
		
		if( button == InputEvent.BUTTON1_MASK ) {
			if( (x >= getClientWidth()-14-xOffset) && (x <= getClientWidth()-xOffset) &&
				(y >= yOffset-1) && (y <= yOffset+13) ) {
				
				if( cBoxWindow == null ) {
					cBoxWindow = new aComboBoxWindow( null, getWindowManager(), this );
					cBoxWindow.setSize( getWidth(), getWidth()*2/3 );
					cBoxWindow.setPosition( getX(), getY()+getHeight()+2 );
									
					Iterator iter = names.keySet().iterator();
					while( iter.hasNext() ) {
						Integer key = (Integer)iter.next();
						
						cBoxWindow.addItem((String)names.get(key), key);
					}
					getWindowManager().setVisibility(cBoxWindow,true);
					
					getWindowManager().requestRedraw();
				}
				else {
					cBoxWindow.dispose();
					cBoxWindow = null;
					
					getWindowManager().requestRedraw();
				}
				
				return true;
			}
		}
		
		return result;
	}
	
	public boolean handleEvent( int handle, String event ) {
		boolean result = super.handleEvent( handle, event );
		
		if( vertifyEventSender(cBoxWindow, handle) && (event == "selected") ) {
			Integer value = (Integer)cBoxWindow.getSelectedData();
			
			if( (names.get(value) != null) && (selected != value.intValue()) ) {
				selected = value.intValue();
				
				if( getParent() != null ) {
					getParent().handleEvent( getHandle(), "selected" );
				}
			}
			
			cBoxWindow.dispose();
			cBoxWindow = null;
			
			getWindowManager().requestRedraw();
			
			return true;
		}
		
		return result;
	}
	
	public void paint(Graphics2D g) {
		super.paint(g);
		
		g.drawImage(getImageCache().getImage("interface/jstarmap/vertiefung.png", true),
				0, 0, getClientWidth(), getClientHeight(), null );
		
		if( (xOffset == -1) && getImageCache().isLoaded("interface/jstarmap/vertiefung.png") ) {
			onResize();
		}
		
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font(getWindowManager().getDefaultFont(),0,12));
		g.setColor(new Color(0xc7c7c7) );
		
		String text = "[leer]";
		if( selected > -1 ) {
			text = (String)names.get(new Integer(selected));
			if( text == null ) {
				text = "[leer]";
			}
		}
		
		g.drawString( text, xOffset, g.getFontMetrics().getHeight()+yOffset-1 );
		
		g.drawImage(getImageCache().getImage("interface/jstarmap/icon_dropdown.png", true),
				getClientWidth()-14-xOffset, yOffset+1, null );
	}
}
