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

import java.awt.event.KeyEvent;

import javax.swing.JApplet;

import net.driftingsouls.ds2.framework.CanvasWindowManager;
import net.driftingsouls.ds2.framework.IWindowManager;
import net.driftingsouls.ds2.framework.services.MapConnector;
import net.driftingsouls.ds2.framework.services.ServerConnector;
import net.driftingsouls.ds2.framework.services.SoapConnector;

/**
 * JStarmap ist das Herzstück der gesamten Sternenkarte. Hier beginnt die
 * gesamte Ausführung. 
 * JStarmap erzeugt lediglich {@link MapConnector}, {@link JMapViewer} sowie
 * {@link JAboutDialog} (Diesen zeigt JStarmap auch bei Tastendruck an).
 *  
 * @author Christopher Jung
 */
public class JStarmap extends JApplet {
	private static final long serialVersionUID = 1L;
	
	protected MapConnector map;
	private JAboutDialog aboutDlg;
	protected JMapViewer mapViewer;
	protected CanvasWindowManager wm;
	
	@Override
	public void init() {
		setLayout(null);
	
		String _system = getParameter("starsystem");
		String session = getParameter("session");
		
		// ServerConnector erstellen sofern erforderlich....
		if( ServerConnector.getInstance() == null ) {
			try {
				ServerConnector.createInstance(getParameter("datapath"), session);
			}
			catch( Exception e ) {
				System.out.println("FATAL: "+e);
				return;
			}
		}
		
		// Windowmanager erstellen und initalisieren
		wm = new CanvasWindowManager(getParameter("datapath")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void keyPressed(KeyEvent evt) {
				canvasKeyPressed(evt);
				super.keyPressed(evt);
			}
		};
		
		wm.setBounds(0,0,getWidth(),getHeight());
		wm.setVisible(true);
		add( wm );
		
		wm.init();
		
		map = (MapConnector)ServerConnector.getInstance().getService(MapConnector.SERVICE);
		
		// MapViewer erstellen
		mapViewer = new JMapViewer( null, wm, map );
		mapViewer.setPosition(0,0);
		mapViewer.setSize(getWidth(),getHeight());
		wm.setVisibility( mapViewer, true );
		
		// Karte laden...
		final String starsystem = _system;
		
		Thread athread = new Thread( 
				new Runnable() {
					public void run() {
						// Karte laden 
						try {
							int system = 0;
							
							system = Integer.decode(starsystem).intValue();
							
							map.loadMap(system);
							wm.requestRedraw(mapViewer);
							
							map.precacheImages(wm.getImageCache());
						}
						catch( NumberFormatException e ) {
							System.out.println("Ungueltiger Zahlenwert");
							System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
						}
						catch( Exception e) {
							System.out.println("Fehler beim Laden der Karte: "+e);
							e.printStackTrace();
						}
					}
				}
		);
		athread.setPriority(Thread.MIN_PRIORITY);
		athread.start();
		
		// UserValues laden
		SoapConnector mysoap = (SoapConnector)ServerConnector.getInstance().getService(SoapConnector.SERVICE);
		int bufferedoutput = Integer.parseInt(mysoap.getUserValue(SoapConnector.USERVALUE_STARMAP_BUFFEREDOUTPUT));
		if( bufferedoutput != 0 ) {
			wm.setProperty(IWindowManager.PROPERTY_BUFFERED_OUTPUT, Boolean.TRUE );
		}
		else {
			wm.setProperty(IWindowManager.PROPERTY_BUFFERED_OUTPUT, Boolean.FALSE );
		}
		
		// Aboutdialog erstellen und (noch) mit Testdaten füllen
		aboutDlg = new JAboutDialog( null, wm, wm.getDefaultFont());
		String dlgtext = "--- Bedienhinweise ---\n";
		dlgtext += "<Rechte Maustaste> - Fenster schließen\n";
		dlgtext += "\n";
		dlgtext += "Karte:\n";
		dlgtext += "<Rechte Maustaste ziehen> - Navigieren\n";
		dlgtext += "<Linke Maustaste> - Sektorinformationen anzeigen\n";
		dlgtext += "<Mittlere Maustaste> - Weitere Aktionen (Zoomen, Optionen) anzeigen\n"; 
		
		aboutDlg.setText(dlgtext);
	}
	
	/**
	 * Behandelt einen Tastendruck im Canvas
	 * @param evt Die Event-Daten
	 */
	public void canvasKeyPressed(KeyEvent evt) {
		int key = evt.getKeyCode();
		
		if( key == KeyEvent.VK_F1 ) {
			if( wm.getVisibility( aboutDlg ) ) {
				wm.setVisibility( aboutDlg, false );
			}
			else {
				aboutDlg.setPosition( 30, 30 );
				aboutDlg.setSize(getWidth()-60,getHeight()-60);
				wm.setVisibility( aboutDlg, true );
			}
		}
	}
}