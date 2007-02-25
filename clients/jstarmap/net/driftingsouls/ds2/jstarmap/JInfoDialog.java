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

package net.driftingsouls.ds2.jstarmap;

import java.awt.Graphics2D;
import java.awt.event.InputEvent;

import net.driftingsouls.ds2.framework.IWindowManager;
import net.driftingsouls.ds2.framework.JButton;
import net.driftingsouls.ds2.framework.JDialog;
import net.driftingsouls.ds2.framework.JTextField;
import net.driftingsouls.ds2.framework.JWindow;
import net.driftingsouls.ds2.framework.services.MapConnector;
import net.driftingsouls.ds2.framework.services.XMLConnectorException;

/**
 * JInfoDialog zeigt die Infos zu jedem Sektor an. Den Text holt sich
 * der Dialog dabei selbstständig von {@link MapConnector} nachdem neue
 * x/y-Koordinaten mittels {@link #setSector(int, int)} gesetzt wurden.
 * Wenn in dem Sektor eigene Schiffe sind, wird ein Button angezeigt um
 * Schiffsinfos via {@link JSectorInfoDialog} anzuzeigen
 * 
 * @author Christopher Jung
 */
public class JInfoDialog extends JDialog {	
	private boolean autoHeight;
	private JTextField textfield;
	private MapConnector map;
	private int currentX;
	private int currentY;
	private JSectorInfoDialog dlgSectorInfo;
	private JButton addInfoButton;
	
	/**
	 * Konstruktor
	 * @param parent Das Elternfenster
	 * @param windowmanager Der Fenstermanager
	 * @param myfont Der zu verwendende Font
	 * @param map Der Map-Connector
	 */
	public JInfoDialog( JWindow parent, IWindowManager windowmanager, String myfont, MapConnector map ) {
		super( parent, windowmanager );
		
		autoHeight = false;
		this.map = map;
		currentX = 0;
		currentY = 0;
		
		textfield = new JTextField( this, windowmanager, myfont );
		textfield.setSize(getClientWidth(),getClientHeight());
		textfield.setPosition(0,0);
		addInfoButton = null;
		
		setPosition( IWindowManager.POSITION_ALWAYS_WITHIN_SCREEN );
	}
	
	/**
	 * Setzt den anzuzeigenden Sektor
	 * @param x Die x-Koordinate des DS-Sektors
	 * @param y Die y-Koordinate des DS-Sektors
	 */
	public void setSector( int x, int y ) {
		currentX = x;
		currentY = y;
		textfield.setText( map.getSectorDescription(x,y) );
		
		if( map.sectorHasProperty(currentX,currentY, MapConnector.SECTOR_OWN_SHIPS) ||
				map.sectorHasProperty(currentX,currentY, MapConnector.SECTOR_ALLY_SHIPS) || 
				map.sectorHasProperty(currentX,currentY, MapConnector.SECTOR_ENEMY_SHIPS) ) {
			if( addInfoButton == null ) {
				addInfoButton = new JButton( this, getWindowManager() );
				addInfoButton.setPosition(getClientWidth()-24, 0);
				addInfoButton.setSize( 24, 24 );
				
				addInfoButton.setPicture( JButton.MODE_NORMAL, "interface/jstarmap/lupe.png" );
				addInfoButton.setPicture( JButton.MODE_MOUSE_OVER, "interface/jstarmap/lupe_mo.png" );
				addInfoButton.setPicture( JButton.MODE_PRESSED, "interface/jstarmap/lupe_h.png" );
				
				textfield.setSize(getClientWidth()-24,getClientHeight());
			}
			
			if( autoHeight ) {
				int buttonsize = 24;
				
				if( Math.max(textfield.getClientHeight(),buttonsize) != getClientHeight() ) {
					setClientSize(getClientWidth(),Math.max(textfield.getClientHeight(),buttonsize));
				}
			}
		}
		else if( addInfoButton != null ) {
			addInfoButton.dispose();
			addInfoButton = null;
		}
	}
	
	/**
	 * Setzt die automatische Hoehenanpassung
	 * @param value <code>true</code>, wenn die automatische Hoehenanpassung aktiviert werden soll
	 */
	public void setAutoHeight( boolean value ) {		
		if( value ) {
			disableVScrolling();
		}
		else if( !autoHeight ) {
			enableVScrolling();
		}
			
		autoHeight = value;
	}
	
	@Override
	public void onResize() {
		if( addInfoButton != null ) {
			addInfoButton.setPosition(getClientWidth()-24, 0);
			textfield.setSize(getClientWidth()-24,getClientHeight());
		}
		else {
			textfield.setSize(getClientWidth(),getClientHeight());
		}
		
		if( autoHeight ) {
			int buttonsize = 0;
			if( addInfoButton != null ) {
				buttonsize = 24;
			}
			
			if( Math.max(textfield.getClientHeight(),buttonsize) != getClientHeight() ) {
				setClientSize(getClientWidth(),Math.max(textfield.getClientHeight(),buttonsize));
			}
		}
	}
	
	@Override
	public boolean handleEvent( int handle, String event ) {
		boolean result = super.handleEvent( handle, event );
		
		if( vertifyEventSender( addInfoButton, handle ) && (event == "pressed") ) {
			if( dlgSectorInfo != null ) {
				dlgSectorInfo.dispose();
				dlgSectorInfo = null;
			}
			
			dlgSectorInfo = new JSectorInfoDialog(null, getWindowManager(), getWindowManager().getDefaultFont() );
		
			dlgSectorInfo.setPosition( 100, 100 );
			dlgSectorInfo.setSize(400,400);
			getWindowManager().setVisibility( dlgSectorInfo, true );
			try {
				dlgSectorInfo.setDocumentSource(map.fetchSectorObjectList(currentX,currentY));
			}
			catch( XMLConnectorException e ) {
				System.out.println("Fehler beim Laden des Sektors "+currentX+"/"+currentY+": "+e.getCause());
			}
				
			return true;
		}
		
		return result;
	}
	
	@Override
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed(x, y, button);
		
		if( button == InputEvent.BUTTON3_MASK ) {
			getWindowManager().setVisibility(this, false);
			
			return true;
		}
		
		return result;
	}
	
	@Override
	public void paint(Graphics2D g) {		
		super.paint(g);		
		// ggf. die Hoehe des Dialogfelds automatisch anpassen
		if( autoHeight ) {
			int buttonsize = 0;
			if( addInfoButton != null ) {
				buttonsize = 24;
			}
			
			if( Math.max(textfield.getClientHeight(),buttonsize) != getClientHeight() ) {
				setClientSize(getClientWidth(),textfield.getClientHeight());
			}
		}
	}
}