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

import net.driftingsouls.ds2.framework.IWindowManager;
import net.driftingsouls.ds2.framework.JDialog;
import net.driftingsouls.ds2.framework.JWindow;
import net.driftingsouls.ds2.framework.JButton;
import net.driftingsouls.ds2.framework.services.MapConnector;

import java.awt.event.InputEvent;

/**
 * JMapContextDialog ermöglicht das Zoomen der Karte sowie das aufrufen der Konfiguration
 * ({@link JConfigureDialog})
 * 
 * @author Christopher Jung
 *
 */
public class JMapContextDialog extends JDialog {
	private JButton zoomOutButton;
	private JButton zoomInButton;
	private JButton settingsButton;
	private MapConnector map;
	
	private JConfigureDialog dlgConfigure;
	
	public JMapContextDialog(JWindow parent, IWindowManager windowmanager, MapConnector map ) {
		super(parent, windowmanager);
		
		setClientSize( 72, 24 );
		
		this.map = map;
		
		zoomInButton = new JButton(this, windowmanager);
		zoomInButton.setPosition( 0, 0 );
		zoomInButton.setSize( 24, 24 );
		
		zoomInButton.setPicture( JButton.MODE_NORMAL, "interface/jstarmap/lupe_add.png" );
		zoomInButton.setPicture( JButton.MODE_MOUSE_OVER, "interface/jstarmap/lupe_add_mo.png" );
		zoomInButton.setPicture( JButton.MODE_PRESSED, "interface/jstarmap/lupe_add_h.png" );
		
		zoomOutButton = new JButton(this, windowmanager);
		zoomOutButton.setPosition( 24, 0 );
		zoomOutButton.setSize( 24, 24 );
		
		zoomOutButton.setPicture( JButton.MODE_NORMAL, "interface/jstarmap/lupe_sub.png" );
		zoomOutButton.setPicture( JButton.MODE_MOUSE_OVER, "interface/jstarmap/lupe_sub_mo.png" );
		zoomOutButton.setPicture( JButton.MODE_PRESSED, "interface/jstarmap/lupe_sub_h.png" );
		
		settingsButton = new JButton(this, windowmanager);
		settingsButton.setPosition( 48, 0 );
		settingsButton.setSize( 24, 24 );
		
		settingsButton.setPicture( JButton.MODE_NORMAL, "interface/jstarmap/einstellungen.png" );
		settingsButton.setPicture( JButton.MODE_MOUSE_OVER, "interface/jstarmap/einstellungen_mo.png" );
		settingsButton.setPicture( JButton.MODE_PRESSED, "interface/jstarmap/einstellungen_h.png" );
		
		dlgConfigure = null;
	}
	
	public boolean handleEvent( int handle, String event ) {
		boolean result = super.handleEvent( handle, event );
		
		if( vertifyEventSender( zoomOutButton, handle ) && (event == "pressed") ) {
			JMapViewer viewer = (JMapViewer)getParent();
				
			if( viewer.getCurrentZoom() > 20 ) {
				viewer.zoomMap( viewer.getCurrentZoom()-20, getX(), getY() );
			}
				
			return true;
		}
		
		if( vertifyEventSender( zoomInButton, handle) && (event == "pressed") ) {
			JMapViewer viewer = (JMapViewer)getParent();
				
			if( viewer.getCurrentZoom() < 181 ) {
				viewer.zoomMap( viewer.getCurrentZoom()+20, getX(), getY() );
			}
				
			return true;
		}
		
		if( vertifyEventSender( settingsButton, handle) && (event == "pressed") ) {
			if( dlgConfigure == null ) {
				dlgConfigure = new JConfigureDialog( null, getWindowManager(), map );
				dlgConfigure.setPosition( IWindowManager.POSITION_SCREEN_CENTER );
				getWindowManager().setVisibility( dlgConfigure, false );
			}
			getWindowManager().setVisibility(dlgConfigure, true);
		}
		
		return result;
	}
	
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed(x, y, button);
		
		if( button == InputEvent.BUTTON3_MASK ) {
			getWindowManager().setVisibility(this, false);
			
			return true;
		}
		
		return result;
	}
}
