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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.Vector;

/**
 * aWindowEntry repräsentiert ein Fenster mit all seinen Eigenschaften innerhalb
 * der internen Fensterliste des WindowManagers.
 * aWindowEntry fungiert dabei zum größten Teil nur als Datenspeicher. Die meisten
 * Berechnungen erfolgen im WindowManager selbst
 * 
 * @author Christopher Jung
 */
class aWindowEntry {
	public static final int VISIBILITY_OFF = 1;
	public static final int VISIBILITY_ON = 2;
	public static final int VISIBILITY_FORCE_OFF = 5;
	public static final int VISIBILITY_FORCE_ON = 6;
	public static final int VISIBILITY_FORCE = 4;
	
	private int visibility;
	private JWindow window;
	private Shape shape;
	private JWindow parent;
	private CanvasWindowManager wm;
	
	private Rectangle windowRect;
	private Rectangle clientWindowRect;
	private Rectangle virtualClientWindowRect;
	private Rectangle minVirtualClientWindowRect;
	private Rectangle borderSize;

	private Point windowPosition;
	private Point windowRelativePosition;
	private int windowPositionMode;
	
	private int vScrollOffset;
	private int scrollBarHandle;
	private boolean enableScrolling;
	
	private BufferedImage offscreenBuffer;
	private boolean redraw;
	
	public aWindowEntry( CanvasWindowManager sm, JWindow aWindow, JWindow aParent ) {
		visibility = VISIBILITY_OFF;
		window = aWindow;
		wm = sm;
		parent = aParent;
		windowRect = new Rectangle(1,1);
		clientWindowRect = new Rectangle(1,1);
		virtualClientWindowRect = new Rectangle(1,1);
		minVirtualClientWindowRect = new Rectangle(1,1);
		borderSize = new Rectangle(0,0,0,0);
		scrollBarHandle = 0;
		windowPositionMode = IWindowManager.POSITION_CUSTOM;
		windowPosition = new Point(0,0);
		windowRelativePosition = new Point(0,0);
		vScrollOffset = 0;
		enableScrolling = true;
		offscreenBuffer = null;
		redraw = false;
	}
	
	public int getVisibility() {
		return visibility;
	}
	
	public void setVisibility( int vis ) {
		visibility = vis;
		
		if( vis == VISIBILITY_FORCE_ON ) {
			vis = VISIBILITY_ON;
		}
		else if( vis == VISIBILITY_FORCE_OFF ) {
			vis = VISIBILITY_OFF;
		}
		
		Vector children = wm.getChildren( window );
		for( int i = 0; i < children.size(); i++ ) {
			wm.setVisibility(((JWindow)children.get(i)),vis);
		}
	}
	
	public JWindow getWindow() {
		return window;
	}
	
	public JWindow getParent() {
		return parent;
	}
	
	public Shape getShape() {
		return shape;
	}
	
	public void setShape( Shape ashape ) {
		shape = ashape;
	}
	
	public Rectangle getWindowRect() {
		return windowRect;
	}
	
	public void setWindowRect(Rectangle rect) {
		windowRect = rect;
	}
	
	public Rectangle getClientRect() {
		return clientWindowRect;
	}
	
	public void setClientRect(Rectangle rect) {
		clientWindowRect = rect;
	}
	
	public Rectangle getVirtualClientRect() {
		return virtualClientWindowRect;
	}
	
	public void setVirtualClientRect(Rectangle rect) {
		virtualClientWindowRect = rect;
	}
	
	public Rectangle getMinimalVirtualClientRect() {
		return minVirtualClientWindowRect;
	}
	
	public void setMinimalVirtualClientRect(Rectangle rect) {
		minVirtualClientWindowRect = rect;
	}
	
	public Rectangle getBorderSize() {
		return borderSize;
	}
	
	public void setBorderSize(Rectangle rect) {
		borderSize = rect;
	}
	
	public int getScrollBarHandle() {
		return scrollBarHandle;
	}
	
	public void setScrollBarHandle( int handle ) {
		scrollBarHandle = handle;
	}
	
	public Point getWindowPosition() {
		return windowPosition;
	}
	
	public Point getRelativeWindowPosition() {		
		return windowRelativePosition;
	}
	
	public void setWindowRelativePosition( Point p ) {
		windowRelativePosition = p;
	}
	
	public void setWindowPosition( Point p ) {
		windowPosition = p;
	}
	
	public int getWindowPositionMode() {
		return windowPositionMode;
	}
	
	public void setWindowPositionMode( int mode ) {
		windowPositionMode = mode;
	}
	
	public Rectangle getVisibleClientRect() {
		Rectangle rect = new Rectangle(	windowPosition.x+borderSize.x,
										windowPosition.y+borderSize.y,
										clientWindowRect.width,
										clientWindowRect.height);	
		return rect;
	}
	
	public int mapX( int absX ) {
		absX -= windowPosition.x;
		absX -= borderSize.x;
		
		return absX;
	}
	
	public int mapY( int absY ) {
		absY -= windowPosition.y;
		absY -= borderSize.y;
		
		int parentoffset = 0;
		if( getParent() != null ) {
			aWindowEntry parent = this;
									
			do {
				if( parent.getWindow().getHandle() != wm.getWindowEntry(parent.getParent().getHandle()).getScrollBarHandle() ) {
					parent = wm.getWindowEntry(parent.getParent().getHandle());
					
					parentoffset += parent.getVScrollOffset();
				}
				else {
					parent = wm.getWindowEntry(parent.getParent().getHandle());
				}
			} while( parent.getParent() != null );
		}
		absY -= parentoffset;
		
		return absY;
	}
	
	public int getVScrollOffset() {
		return vScrollOffset;
	}
	
	public void setVScrollOffset( int offset ) {
		vScrollOffset = offset;
	}
	
	public boolean isScrollable() {
		return enableScrolling;
	}
	
	public void setScrollable( boolean scroll ) {
		enableScrolling = scroll;
	}
	
	public void setOffscreenBuffer( BufferedImage img ) {
		offscreenBuffer = img;
	}
	
	public BufferedImage getOffscreenBuffer() {
		return offscreenBuffer;
	}
	
	public boolean getRedrawStatus() {
		return redraw;
	}
	
	public void setRedrawStatus( boolean redraw ) {
		this.redraw = redraw;
	}
}