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
import java.awt.event.InputEvent;

/**
 * JButton repräsentiert einen Button.
 * Die Funktionalität ist im Moment jedoch auf das Anzeigen von Bildern beschränkt.
 * Das müsste mal geändert werden....
 * 
 * Wenn der Button gedrückt wird, wird ein "pressed"-Ereignis gesendet
 * 
 * @author Christopher Jung
 */
public class JButton extends JWindow {
	/**
	 * MODE_NORMAL stellt den normalen Zustand eines Buttons da
	 */
	public static final int MODE_NORMAL = 1;
	/**
	 * MODE_MOUSE_OVER stellt den Zustand da, wenn sich die Maus über dem Button befindet
	 */
	public static final int MODE_MOUSE_OVER = 2;
	/**
	 * MODE_PRESSEN stellt den Zustand da, wenn der Button gedrückt ist
	 */
	public static final int MODE_PRESSED = 3;
	
	private String imgNormal;
	private String imgMouseOver;
	private String imgPressed;
	private int status;
	
	/**
	 * Erzeugt einen neuen Button
	 * 
	 * @param parent		Das Elternfenster
	 * @param windowmanager	Der Windowmanager
	 */
	public JButton( JWindow parent, IWindowManager windowmanager ) {
		super( parent, windowmanager );
		
		imgNormal = "";
		imgMouseOver = "";
		imgPressed = "";
		status = MODE_NORMAL;
	}
	
	/**
	 * Setzt ein Bild für einen bestimmten Zustand. Mögliche Zustände sind hierbei
	 * {@link #MODE_NORMAL}, {@link #MODE_MOUSE_OVER} und {@link #MODE_PRESSED}.
	 * 
	 * @param mode	Der Zustand, dessen Bild gesetzt werden soll
	 * @param path	Der Pfad des zu verwendenden Bilds relativ zum DS2-Datenpfad
	 */
	public void setPicture( int mode, String path ) {
		if( mode == MODE_NORMAL ) {
			imgNormal = path;
			getImageCache().getImage(path, true);
		}
		if( mode == MODE_MOUSE_OVER ) {
			imgMouseOver = path;
			getImageCache().getImage(path, true);
		}
		if( mode == MODE_PRESSED ) {
			imgPressed = path;
			getImageCache().getImage(path, true);
		}
	}
	
	@Override
	public boolean mouseMoved( int x, int y, int button ) {
		super.mouseMoved(x, y, button);
		
		if( status == MODE_NORMAL ) {
			status = MODE_MOUSE_OVER;
			
			getWindowManager().requestRedraw(this);
		}
		return true;
	}
	
	@Override
	public void mouseExited( int x, int y, int button ) {
		super.mouseExited( x, y, button );
		
		if( status != MODE_NORMAL ) {
			status = MODE_NORMAL;
			
			getWindowManager().requestRedraw(this);
		}
	}
	
	@Override
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed( x, y, button );
		
		if( button == InputEvent.BUTTON1_MASK ) {		
			status = MODE_PRESSED;
			getWindowManager().requestRedraw(this);
			
			JWindow aparent = getParent();
			
			while( aparent != null ) {
				if( getParent().handleEvent(getHandle(),"pressed") ) {
					break;
				}
				
				aparent = aparent.getParent();
			}
			
			return true;
		}
		
		return result;
	}
	
	@Override
	public boolean mouseReleased( int x, int y, int button ) {
		super.mouseReleased( x, y, button );
		
		if( (x >= 0) && (x <= getClientWidth()) && 
			(y >= 0) && (y <= getClientHeight()) ) {
			status = MODE_MOUSE_OVER;
		}
		else {
			status = MODE_NORMAL;
		}
		
		getWindowManager().requestRedraw(this);
		
		return true;
	}
	
	@Override
	public void paint( Graphics2D g ) {
		if( (status == MODE_NORMAL) && (imgNormal != "") ) {
			g.drawImage( getImageCache().getImage(imgNormal, false),
						0, 0, getClientWidth(), getClientHeight(), null );
		}
		
		if( (status == MODE_MOUSE_OVER) && (imgMouseOver != "") ) {
			g.drawImage( getImageCache().getImage(imgMouseOver, false),
						0, 0, getClientWidth(), getClientHeight(), null );
		}
		
		if( (status == MODE_PRESSED) && (imgPressed != "") ) {
			g.drawImage( getImageCache().getImage(imgPressed, false),
						0, 0, getClientWidth(), getClientHeight(), null );
		}
	}
}
