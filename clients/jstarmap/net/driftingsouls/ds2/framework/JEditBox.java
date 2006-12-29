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
import java.awt.event.KeyEvent;
import java.awt.Shape;
import java.awt.Rectangle;

/**
 * JEditBox stellt eine einzeilige Eingabebox da. Im Moment fehlt allerdings noch
 * einiges an Funktionalität (Beispiel: Ein funktionsfähiger Textcursor, Copy&Paste,...)
 * 
 * @author Christopher Jung
 *
 */
public class JEditBox extends JWindow {

	// Offset des Textes usw in der Hintergrundgrafik
	private int xOffset;
	private int yOffset;
	
	private String m_text;
	
	private int maxLength;
	private int carret;
	
	private int lastTextOffset;
	
	/**
	 * Erstellt eine neue Instanz einer EditBox
	 * 
	 * @param parent		Das Elternfenster
	 * @param windowmanager	Der Fenstermanager
	 */
	public JEditBox(JWindow parent, IWindowManager windowmanager) {
		super(parent, windowmanager);
		
		xOffset = -1;
		yOffset = -1;
		
		m_text = "";
		
		maxLength = -1;
		carret = 0;
		lastTextOffset = 0;
	
		getImageCache().getImage("interface/jstarmap/vertiefung.png", true);
	}
	
	/**
	 * Setzt den Inhalt der Editbox auf den angebenen Wert
	 * 
	 * @param text	Der neue Inhalt der Editbox
	 */
	public void setText( String text ) {
		m_text = text;
		if( (maxLength > -1) && (maxLength > m_text.length()) ) {
			m_text = m_text.substring(0, maxLength);
		}
		carret = m_text.length();
	}
	
	/**
	 * Liefert den Inhalt der Editbox zurück
	 * 
	 * @return Der Inhalt der Editbox
	 */
	public String getText() {
		return m_text;
	}
	
	public void onResize() {
		super.onResize();
		
		if( getImageCache().isLoaded("interface/jstarmap/vertiefung.png") ) {
			if( lastTextOffset > 0 ) {
				lastTextOffset -= xOffset;
			}
			xOffset = getImageCache().getImage("interface/jstarmap/vertiefung.png", true).getWidth();
			xOffset = Math.round((float)getClientWidth()/(float)xOffset * 10);
			lastTextOffset += xOffset;
			
			yOffset = getImageCache().getImage("interface/jstarmap/vertiefung.png", true).getHeight();
			yOffset = Math.round((float)getClientHeight()/(float)yOffset * 7);
		}
	}
	
	public boolean keyPressed( int keycode, char key ) {
		if( keycode == KeyEvent.VK_BACK_SPACE ) {
			if( (m_text.length() > 0) && (carret > 0) ) {
				String newtext = "";
				if( carret > 1 ) {
					newtext += m_text.substring(0,carret-1);
				}
				if( carret < m_text.length() ) {
					newtext += m_text.substring(carret,m_text.length());
				}
				m_text = newtext;
				carret--;
			}
		}
		else if( keycode == KeyEvent.VK_DELETE ) {
			if( (m_text.length() > 0) && (carret < m_text.length()) ) {
				String newtext = "";
				if( carret > 0 ) {
					newtext += m_text.substring(0,carret);
				}
				if( carret+1 < m_text.length() ) {
					newtext += m_text.substring(carret+1,m_text.length());
				}
				m_text = newtext;
			}
		}
		else if( keycode == KeyEvent.VK_LEFT ) {
			if( carret > 0 ) {
				carret--;
			}
		}
		else if( keycode == KeyEvent.VK_RIGHT ) {
			if( carret < m_text.length() ) {
				carret++;
			}
		}
		else if( keycode == KeyEvent.VK_HOME ) {
			carret = 0;
		}
		else if( keycode == KeyEvent.VK_END ) {
			carret = m_text.length();
		}
		else if( (maxLength == -1) || (maxLength > m_text.length()) ) {
			String newtext = "";
			if( carret > 0 ) {
				newtext = m_text.substring(0,carret);
			}
			newtext += key;
			if( carret < m_text.length() ) {
				newtext += m_text.substring(carret,m_text.length());
			}
			m_text = newtext;
			carret++;
		}
		
		getWindowManager().requestRedraw(this);
		
		return true;
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
		
		Shape s = g.getClip();
		g.setClip( new Rectangle(xOffset,yOffset, getClientWidth()-xOffset*2, getClientHeight()-yOffset*2) );
	
		int x = lastTextOffset;
		int carretwidth = g.getFontMetrics().stringWidth(m_text.substring(0,carret));

		
		if( x+carretwidth <= xOffset ) {
			x = xOffset - carretwidth;
		}
		
		if( x+carretwidth >= getClientWidth()-xOffset ) {
			x = getClientWidth()-xOffset-carretwidth;
		}
		
		lastTextOffset = x;
		
		g.drawString( m_text, x, g.getFontMetrics().getHeight()+yOffset-1 );
		
		g.setClip(s);
		
		if( getWindowManager().hasFocus(this) ) {
			Font newFont = new Font(g.getFont().getFamily(), g.getFont().getStyle(), g.getFont().getSize()+4 );
			g.setFont(newFont);
			g.setColor(new Color(0xe7e7e7));
			g.drawString( "I", x+carretwidth-2, g.getFontMetrics().getHeight()+yOffset-4 );

		}
	}
}
