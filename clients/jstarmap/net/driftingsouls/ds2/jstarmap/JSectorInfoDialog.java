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
import java.util.Vector;
import java.awt.event.InputEvent;

import net.driftingsouls.ds2.framework.JDialog;
import net.driftingsouls.ds2.framework.IImageStatusNotifier;
import net.driftingsouls.ds2.framework.JShipInfo;
import net.driftingsouls.ds2.framework.JWindow;
import net.driftingsouls.ds2.framework.IWindowManager;

import org.w3c.dom.*;

/**
 * JSectorInfoDialog stellt die Liste aller (eigenen) Schiffe in einem
 * Sektor da. Dazu wird für jedes Schiff ein {@link JShipInfo}-Element verwendet.
 * Die notwendigen Daten werden in XML-DOM-Format von {@link JInfoDialog} via
 * {@link #setDocumentSource(Document)} übergeben.
 * Das Parsen übernimmt dann diese Klasse.
 * 
 * @author Christopher Jung
 */
public class JSectorInfoDialog extends JDialog implements IImageStatusNotifier {
	private Vector shipPanels;
	private String fontName;
	
	public JSectorInfoDialog( JWindow parent, IWindowManager windowmanager, String fontname ) {
		super( parent, windowmanager );
		
		fontName = fontname; 
		
		shipPanels = new Vector();
	}
	
	private void parseTypeTag( Node typenode, JShipInfo shipinfo ) {
		NodeList elements = typenode.getChildNodes();
		
		for( int i=0; i < elements.getLength(); i++ ) {
			Node currentElement = elements.item(i);
			
			if( currentElement.getNodeName() == "name" ) {
				CDATASection nameText = (CDATASection)currentElement.getFirstChild();
				shipinfo.setProperty("type/name",nameText.getData());
			}
		
			if( currentElement.getNodeName() == "hull" ) {
				Text hullText = (Text)currentElement.getFirstChild();
				shipinfo.setProperty("type/hull",hullText.getData());
			}
		
			if( currentElement.getNodeName() == "shields" ) {
				Text shieldsText = (Text)currentElement.getFirstChild();
				shipinfo.setProperty("type/shields",shieldsText.getData());
			}
			
			if( currentElement.getNodeName() == "crew" ) {
				Text shieldsText = (Text)currentElement.getFirstChild();
				shipinfo.setProperty("type/crew",shieldsText.getData());
			}
			
			if( currentElement.getNodeName() == "eps" ) {
				Text shieldsText = (Text)currentElement.getFirstChild();
				shipinfo.setProperty("type/eps",shieldsText.getData());
			}
			
			if( currentElement.getNodeName() == "cargo" ) {
				Text shieldsText = (Text)currentElement.getFirstChild();
				shipinfo.setProperty("type/cargo",shieldsText.getData());
			}
		}
	}
	
	public void setDocumentSource( Document doc ) {
		for( int i=0; i < shipPanels.size(); i++ ) {
			((JShipInfo)shipPanels.get(i)).dispose();
		}
		shipPanels = new Vector();
		
		int y = 0;
		
		NodeList shiplist = doc.getElementsByTagName("ship");
		
		// Die Schiffsliste durchgehen
		for( int i=0; i < shiplist.getLength(); i++ ) {
			Element ship = (Element)shiplist.item(i);
			
			JShipInfo shipinfo = new JShipInfo(this,getWindowManager(),fontName);
			shipinfo.setSize(getClientWidth(),50);
			shipinfo.setPosition(0,y);
			
			Attr relation = ship.getAttributeNode("relation");
			String rel = relation.getValue();
			shipinfo.setProperty("relation", rel);
			
			Attr id = ship.getAttributeNode("id");
			
			NodeList elements = ship.getChildNodes();
			for( int j=0; j < elements.getLength(); j++ ) {
				Node currentElement = elements.item(j);
				
				if( currentElement.getNodeName() == "picture" ) {
					Text pictureText = (Text)currentElement.getFirstChild();
				
					// Leider fangen alle Pfade mit data/ an, und das mag 
					// JImageCache gar nicht
					String picture = pictureText.getData().substring(5);
					if( !getImageCache().isLoaded(picture) ) {
						getImageCache().getImage(picture, false, this, null );
					}
					shipinfo.setProperty("picture",picture);
				}
			
				if( currentElement.getNodeName() == "name" ) {
					CDATASection nameText = (CDATASection)currentElement.getFirstChild();
					shipinfo.setProperty("name",nameText.getData());
				}
			
				if( currentElement.getNodeName() == "hull" ) {
					Text hullText = (Text)currentElement.getFirstChild();
					shipinfo.setProperty("hull",hullText.getData());
				}
			
				if( currentElement.getNodeName() == "shields" ) {
					Text shieldsText = (Text)currentElement.getFirstChild();
					shipinfo.setProperty("shields",shieldsText.getData());
				}
				
				if( currentElement.getNodeName() == "crew" ) {
					Text crewText = (Text)currentElement.getFirstChild();
					shipinfo.setProperty("crew",crewText.getData());
				}
				
				if( currentElement.getNodeName() == "e" ) {
					Text energyText = (Text)currentElement.getFirstChild();
					shipinfo.setProperty("e",energyText.getData());
				}
				
				if( currentElement.getNodeName() == "s" ) {
					Text heatText = (Text)currentElement.getFirstChild();
					shipinfo.setProperty("s",heatText.getData());
				}
				
				if( currentElement.getNodeName() == "usedcargo" ) {
					Text usedcargoText = (Text)currentElement.getFirstChild();
					shipinfo.setProperty("usedcargo",usedcargoText.getData());
				}
				
				if( currentElement.getNodeName() == "type" ) {
					parseTypeTag( currentElement, shipinfo );
				}
				
				if( currentElement.getNodeName() == "ownername" ) {
					Text ownernameText = (Text)currentElement.getFirstChild();
					shipinfo.setProperty("ownername",ownernameText.getData());
				}
			}
			shipinfo.onResize(); // Gibt es da nicht ne bessere Loesung?
			y += shipinfo.getHeight()+10;
			
			shipPanels.add(shipinfo);
			getWindowManager().requestRedraw(shipinfo);
		}
	}
	
	public void onImageLoaded( String image, Object data ) {
		int y = 0;
		
		for( int i=0; i < shipPanels.size(); i++ ) {
			JShipInfo shipinfo = (JShipInfo)shipPanels.get(i);
			
			shipinfo.setPosition(0,y);
			
			if( shipinfo.getProperty("picture") == image ) {
				// Auf dass sich diese Komponente aktuallisiert
				shipinfo.setProperty("picture",image);
			}
			
			shipinfo.setSize(getClientWidth(),50);
			shipinfo.onResize(); // Igitt...Gibt es da nicht ne bessere loesung?
			getWindowManager().requestRedraw(shipinfo);
			y += shipinfo.getHeight()+5;
		}
		getWindowManager().requestRedraw(this);
	}
	
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed( x, y, button );
		
		if( button == InputEvent.BUTTON3_MASK ) {
			getWindowManager().setVisibility( this, false );
			
			return true;
		}
		
		return result;		
	}
	
	public void paint( Graphics2D g ) {
		super.paint(g);
	}
}