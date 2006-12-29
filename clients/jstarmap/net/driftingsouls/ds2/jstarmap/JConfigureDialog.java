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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;

import net.driftingsouls.ds2.framework.IWindowManager;
import net.driftingsouls.ds2.framework.JButton;
import net.driftingsouls.ds2.framework.JCheckBox;
import net.driftingsouls.ds2.framework.JComboBox;
import net.driftingsouls.ds2.framework.JDialog;
import net.driftingsouls.ds2.framework.JEditBox;
import net.driftingsouls.ds2.framework.JWindow;
import net.driftingsouls.ds2.framework.services.MapConnector;
import net.driftingsouls.ds2.framework.services.ServerConnector;
import net.driftingsouls.ds2.framework.services.SoapConnector;
import net.driftingsouls.ds2.framework.services.XMLConnector;
import net.driftingsouls.ds2.framework.services.XMLConnectorException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * JConfigureDialog ermöglicht es Sternenkarten-bezogene Einstellungen zu tätigen.
 * Im Moment gibt es da allerdings noch nicht soooooo viele.
 * Lediglich das Auswählen des Systems sowie für Admins das auswählen des Spielers
 * ist möglich.
 * Auch können diese Einstellungen im Moment noch nicht auf dem Server gespeichert werden.
 * 
 * @author Christopher Jung
 */
public class JConfigureDialog extends JDialog {
	private JComboBox systemSelectionBox;
	
	private JEditBox userSelectionBox;
	private JButton userSelectionButton;
	private JCheckBox bufferedModeCheckBox;
	
	private MapConnector map;
	private boolean isAdmin;

	public JConfigureDialog(JWindow parent, IWindowManager windowmanager, MapConnector aMap ) {
		super(parent, windowmanager);
		
		map = aMap;
		
		SoapConnector jsi = (SoapConnector)ServerConnector.getInstance().getService(SoapConnector.SERVICE);
		
		if( jsi.admin_isAdmin() ) {
			setClientSize(300, 400);
			
			isAdmin = true;
			
			userSelectionBox = new JEditBox( this, getWindowManager() );
			userSelectionBox.setSize(170, 25 );
			userSelectionBox.setPosition( 80, 90 );
			userSelectionBox.setText(String.valueOf(map.getCurrentUser()));
			
			userSelectionButton = new JButton( this, getWindowManager() );
			userSelectionButton.setPosition( 255, 90 );
			userSelectionButton.setSize( 24, 24 );
			
			userSelectionButton.setPicture( JButton.MODE_NORMAL, "interface/jstarmap/ok.png" );
			userSelectionButton.setPicture( JButton.MODE_MOUSE_OVER, "interface/jstarmap/ok_mo.png" );
			userSelectionButton.setPicture( JButton.MODE_PRESSED, "interface/jstarmap/ok_h.png" );
		}
		else {
			setClientSize(300, 90);
			
			isAdmin = false;
		}
		
		/*
		 * 
		 *  System-Auswahlbox erstellen
		 * 
		 */
		
		systemSelectionBox = new JComboBox( this, windowmanager );
		systemSelectionBox.setSize( 200, 21 );
		systemSelectionBox.setPosition( 80, 20 );
		
		XMLConnector xml = (XMLConnector)ServerConnector.getInstance().getService(XMLConnector.SERVICE);
		Document doc;
		try {
			doc = xml.fetchURL("mapdata","getSystems", "");
		}
		catch( XMLConnectorException e ) {
			System.out.println("Fehler beim Aufruf von mapdata->getSystems: "+e.getCause());
			return;
		}
		
		if( doc != null ) {
			NodeList systemlist = doc.getElementsByTagName("system");
			for( int i=0; i < systemlist.getLength(); i++ ) {
				Element system = (Element)systemlist.item(i);
				String text = system.getAttribute("id");
				Integer systemId = new Integer(0);
			
				try {
					if( text == null ) {
						text = "0";
					}
				
					systemId = Integer.decode(text);
				}
				catch( NumberFormatException e ) {
					System.out.println("Ungueltiger Zahlenwert in JConfigureDialog::constructor");
					System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
				
					systemId = new Integer(0);
				}
			
				Text systemName = (Text)system.getFirstChild();
		
				int handle = systemSelectionBox.addElement(systemName.getData(), systemId );
			
				if( systemId.intValue() == map.getSystem() ) {
					systemSelectionBox.setSelectedElement(handle);
				}
			}
		}
		
		/*
		 * BufferedMode-Checkbox erstellen
		 */
		bufferedModeCheckBox = new JCheckBox(this,getWindowManager());
		bufferedModeCheckBox.setPosition(10,55);
		bufferedModeCheckBox.setSize(300,20);
		bufferedModeCheckBox.setText("Gepufferte Bildschirmausgabe?");
		bufferedModeCheckBox.setChecked(((Boolean)getWindowManager().getProperty(IWindowManager.PROPERTY_BUFFERED_OUTPUT)).booleanValue());
	}
	
	public boolean handleEvent( int handle, String event ) {
		boolean result = super.handleEvent( handle, event );
		
		if( vertifyEventSender(systemSelectionBox, handle) && (event == "selected") ) {
			Integer system = (Integer)systemSelectionBox.getElementData(systemSelectionBox.getSelectedElement());
			try {
				map.loadMap( system.intValue() );
			}
			catch( Exception e) {}
			
			return true;
		}
		else if( vertifyEventSender(userSelectionButton, handle) && (event == "pressed") ) {
			String user = userSelectionBox.getText();
			try {
				map.loadMap( map.getSystem(), Integer.decode(user).intValue() );
				
				userSelectionBox.setText(String.valueOf(map.getCurrentUser()));
			}
			catch( Exception e) {}
		}
		
		return result;
	}
	
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed(x, y, button);
		
		if( button == InputEvent.BUTTON3_MASK ) {
			getWindowManager().setVisibility(this, false);
			getWindowManager().setProperty(IWindowManager.PROPERTY_BUFFERED_OUTPUT,new Boolean(bufferedModeCheckBox.isChecked()));
			
			SoapConnector jsi = (SoapConnector)ServerConnector.getInstance().getService(SoapConnector.SERVICE);
			if( bufferedModeCheckBox.isChecked() ) {
				jsi.setUserValue(SoapConnector.USERVALUE_STARMAP_BUFFEREDOUTPUT,"1");
			}
			else {
				jsi.setUserValue(SoapConnector.USERVALUE_STARMAP_BUFFEREDOUTPUT,"0");
			}
			return true;
		}
		
		return result;
	}
	
	public void paint( Graphics2D g ) {
		super.paint(g);
		
		int x = getClientX()-getX();
		int y = getClientY()-getY();
		
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font(getWindowManager().getDefaultFont(),0,12));
		g.setColor(new Color(0xc7c7c7) );
		
		g.drawString( "System:", x+10, y+20+g.getFontMetrics().getHeight()+1 );
		if( isAdmin ) {
			g.drawString( "Spieler:", x+10, y+90+g.getFontMetrics().getHeight()+1 );
		}
	}
}
