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
import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 * JDialog stellt ein Dialogfenster da mit den typischen DS2-Tabellenrändern.
 * Dabei stellt JDialog selbst keine neuen Funktionen bereit, sondern überschreibt
 * lediglich von {@link JWindow} bereitgestellte Funktionen.
 *  
 * @author Christopher Jung
 */
public class JDialog extends JWindow {
	private JImageCache imageCache;
	
	private int screenWidth;
	private int screenHeight;
		
	public JDialog( JWindow parent, IWindowManager windowmanager ) {
		super( parent, windowmanager );
		
		getImageCache().getImage("interface/border/border_background.gif",true);
		getImageCache().getImage("interface/border/border_top.gif",true);
		getImageCache().getImage("interface/border/border_left.gif",true);
		getImageCache().getImage("interface/border/border_bottom.gif",true);
		getImageCache().getImage("interface/border/border_right.gif",true);
		
		getImageCache().getImage("interface/border/border_topleft.gif",true);
		getImageCache().getImage("interface/border/border_topright.gif",true);
		getImageCache().getImage("interface/border/border_bottomleft.gif",true);
		getImageCache().getImage("interface/border/border_bottomright.gif",true);
		
		setBorder( 19, 18, 19, 18 );
	}
	
	public boolean mouseClicked(int x, int y, int button) {
		return true;
	}
	
	public boolean mousePressed(int x, int y, int button) {
		return true;
	}
	
	public boolean mouseReleased(int x, int y, int button) {
		return true;
	}
	
	public boolean mouseDragged(int x, int y, int button) {
		return true;
	}
	
	public boolean mouseMoved(int x, int y, int button) {
		return true;
	}
	
	public void mouseExited(int x, int y, int button) {
		return;
	}
	
	public void paint(Graphics2D g) {
		super.paint(g);
		
		// Dialogfeld zeichnen
		
		int cx = 19;		// Warum diese Werte und nicht getClient*()?
		int cy = 18;		// Das ganze hat einen einfachen Grund:
		int cw = getWidth()-38;	// Wir koennen uns nicht drauf verlassen, dass
		int ch = getHeight()-36;// das ClientRect unseren Erwartungen entspricht.
								// JWindow kann naemlich z.B. fuer Scrollbars diese veraendern.
		
		// Top
		g.drawImage(getImageCache().getImage("interface/border/border_top.gif",false),
				cx,0,cw,18,null);
		
		// Bottom
		g.drawImage(getImageCache().getImage("interface/border/border_bottom.gif",false),
				cx,getHeight()-18,cw,18,null);
		
		// Left
		g.drawImage(getImageCache().getImage("interface/border/border_left.gif",false),
				0,cy,19,ch,null);
		
		// Right
		g.drawImage(getImageCache().getImage("interface/border/border_right.gif",false),
				getWidth()-19,cy,19,ch,null);
		
		// Top-Left
		g.drawImage(getImageCache().getImage("interface/border/border_topleft.gif",false),
				0,0,19,18,null);
		
		// Top-Right
		g.drawImage(getImageCache().getImage("interface/border/border_topright.gif",false),
				getWidth()-19,0,19,18,null);
		
		// Bottom-Left
		g.drawImage(getImageCache().getImage("interface/border/border_bottomleft.gif",false),
				0,getHeight()-18,19,18,null);
		
		// Bottom-Right
		g.drawImage(getImageCache().getImage("interface/border/border_bottomright.gif",false),
				getWidth()-19,getHeight()-18,19,18,null);
		
		// Hintergrund
		g.drawImage(getImageCache().getImage("interface/border/border_background.gif",false),
				cx,cy,cw,ch,null);
	}
	
	public Shape getShape() {
		GeneralPath path = new GeneralPath();
		path.moveTo(getX()+16,getY());	
		path.lineTo(getX()+getClientWidth()+22,getY());				// Top
		path.lineTo(getX()+getWidth(),getY()+16);					// TopRight
		path.lineTo(getX()+getWidth(),getY()+getHeight()-16);		// Right
		path.lineTo(getX()+getClientWidth()+22,getY()+getHeight());	// BottomRight
		path.lineTo(getX()+16,getY()+getHeight());					// Bottom
		path.lineTo(getX(),getY()+getHeight()-15);					// BottomLeft
		path.lineTo(getX(),getY()+17);								// Left
		path.closePath();											// TopLeft
		
		return path;
	}
}
