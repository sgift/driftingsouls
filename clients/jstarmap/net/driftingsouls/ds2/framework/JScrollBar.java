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
 * JScrollBar wird intern dazu verwendet Scrollbars anzuzeigen, falls mal wieder
 * ein Clientbereich zu groß geworden ist.
 * Eine Verwendung ausserhalb dieses Anwendungsgebiets ist nicht möglich.
 * 
 * @author bKtHeG (Christopher Jung)
 */
public class JScrollBar extends JWindow {
	private static final int HIGHLIGHT_NONE = 0;
	private static final int HIGHLIGHT_MO = 1;
	private static final int HIGHLIGHT_H = 2;
	
	private int m_top_highlight;
	private int bottomHighlight;
	private int buttonHighlight;
	
	private int buttonPosition;
	
	private boolean dragButton;
	
	private boolean showButtons;
	
	/**
	 * Konstruktor
	 * @param parent Das Elternfenster
	 * @param windowmanager Der Fenstermanager
	 */
	public JScrollBar( JWindow parent, IWindowManager windowmanager ) {
		super( parent, windowmanager );
		
		m_top_highlight = HIGHLIGHT_NONE;
		bottomHighlight = HIGHLIGHT_NONE;
		buttonHighlight = HIGHLIGHT_NONE;
		
		buttonPosition = 0;
		dragButton = false;
		showButtons = true;

		getImageCache().getImage("interface/scrollbar/bar.png",true);
		getImageCache().getImage("interface/scrollbar/button.png",true);
		getImageCache().getImage("interface/scrollbar/bottom.png",true);
		getImageCache().getImage("interface/scrollbar/top.png",true);
		
		getImageCache().getImage("interface/scrollbar/button_mo.png",false);
		getImageCache().getImage("interface/scrollbar/button_h.png",false);
		getImageCache().getImage("interface/scrollbar/bottom_mo.png",false);
		getImageCache().getImage("interface/scrollbar/bottom_h.png",false);
		getImageCache().getImage("interface/scrollbar/top_mo.png",false);
		getImageCache().getImage("interface/scrollbar/top_h.png",false);
	}
	
	@Override
	public void onResize() {
		super.onResize();
		
		if( getClientHeight() < 110 ) {
			showButtons = false;
		}
		else {
			showButtons = true;
		}
		getWindowManager().requestRedraw(this);
	}
	
	@Override
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed( x, y, button );
		
		if( button != InputEvent.BUTTON1_MASK ) {
			return result;
		}
		
		if( x <= 29 ) {
			// Oberer Button
			if( showButtons && (y <= 42) ) {
				getParent().vScrollClientWindow(10);	
				m_top_highlight = HIGHLIGHT_H;
				
				getWindowManager().requestRedraw(this);
				getWindowManager().requestRedraw(getParent());
			}
			// Unterer Button
			else if( showButtons && (y >= getClientHeight()-42) ) {
				getParent().vScrollClientWindow(-10);
				bottomHighlight = HIGHLIGHT_H;
				
				getWindowManager().requestRedraw(this);
				getWindowManager().requestRedraw(getParent());
			}
			else {
				if( (x >= 5) && (x <= 25) && 
					(y >= buttonPosition) && (y <= buttonPosition+20) ) {
					buttonHighlight = HIGHLIGHT_H;
					
					dragButton = true;
					
					getWindowManager().requestRedraw(this);
				}
			}
		}
		
		return true;
	}
	
	@Override
	public boolean mouseMoved( int x, int y, int button ) {
		/*boolean result = */super.mouseMoved( x, y, button );
	
		if( x <= 29 ) {
			// Oberer Button
			if( showButtons && (y <= 42) ) {
				if( m_top_highlight == HIGHLIGHT_NONE ) {
					m_top_highlight = HIGHLIGHT_MO;
					
					if( (bottomHighlight == HIGHLIGHT_MO) || (buttonHighlight == HIGHLIGHT_MO) ) {
						bottomHighlight = HIGHLIGHT_NONE;
						buttonHighlight = HIGHLIGHT_NONE;
					}
				
					getWindowManager().requestRedraw(this);
				}
			}
			// Unterer Button
			else if( showButtons && (y >= getClientHeight()-42) ) {
				if( bottomHighlight == HIGHLIGHT_NONE ) {
					bottomHighlight = HIGHLIGHT_MO;
				
					if( (m_top_highlight == HIGHLIGHT_MO) || (buttonHighlight == HIGHLIGHT_MO) ) {
						m_top_highlight = HIGHLIGHT_NONE;
						buttonHighlight = HIGHLIGHT_NONE;
					}
					
					getWindowManager().requestRedraw(this);
				}
			}
			else {
				if( (x >= 5) && (x <= 25) && 
						(y >= buttonPosition) && (y <= buttonPosition+20) ) {
					if( buttonHighlight == HIGHLIGHT_NONE) {
						buttonHighlight = HIGHLIGHT_MO;
						
						if( (m_top_highlight == HIGHLIGHT_MO) || (bottomHighlight == HIGHLIGHT_MO) ) {
							m_top_highlight = HIGHLIGHT_NONE;
							bottomHighlight = HIGHLIGHT_NONE;
						}
					
						getWindowManager().requestRedraw(this);
					}
				}
				else if( (m_top_highlight > HIGHLIGHT_NONE) || (bottomHighlight > HIGHLIGHT_NONE) || 
						(buttonHighlight > HIGHLIGHT_NONE) ) {
					
					m_top_highlight = HIGHLIGHT_NONE;
					bottomHighlight = HIGHLIGHT_NONE;
					buttonHighlight = HIGHLIGHT_NONE;
					
					getWindowManager().requestRedraw(this);	
				}
			}
		}
		
		return true;
	}
	
	@Override
	public boolean mouseReleased( int x, int y, int button ) {
		boolean result = super.mouseReleased(x,y,button);
		
		if( (m_top_highlight > HIGHLIGHT_NONE) || (bottomHighlight > HIGHLIGHT_NONE) || 
			(buttonHighlight > HIGHLIGHT_NONE) ) {
			
			m_top_highlight = HIGHLIGHT_NONE;
			bottomHighlight = HIGHLIGHT_NONE;
			buttonHighlight = HIGHLIGHT_NONE;
			
			dragButton = false;
			
			getWindowManager().requestRedraw(this);
			
			return true;
		}
		
		return result;
	}
	
	@Override
	public void mouseExited( int x, int y, int button ) {
		super.mouseExited(x,y,button);
		
		if( (m_top_highlight > HIGHLIGHT_NONE) || (bottomHighlight > HIGHLIGHT_NONE) || 
			(buttonHighlight > HIGHLIGHT_NONE) ) {
			
			m_top_highlight = HIGHLIGHT_NONE;
			bottomHighlight = HIGHLIGHT_NONE;
			buttonHighlight = HIGHLIGHT_NONE;
			
			dragButton = false;
			
			getWindowManager().requestRedraw(this);
		}
		
		return;
	}
	
	@Override
	public boolean mouseDragged( int x, int y, int button ) {
		boolean result = super.mouseReleased(x,y,button);
		
		if( dragButton ) {
			int addOffset = 0;
			if( showButtons ) {
				addOffset = 42;
			}
			
			int cypos = y-addOffset-10;
			
			if( cypos > getClientHeight()-(addOffset*2+20) ) {
				cypos = getClientHeight()-(addOffset*2+20);
			}
			
			if( cypos < 0 ) {
				cypos = 0;
			}
			
			int btnpos = buttonPosition-addOffset;
			
			int diff = btnpos - cypos;
			
			if( diff != 0 ) {
				double vso = getParent().getVScrollOverflow();		// Sonst begreift Java ja nicht, dass es mit Kommawerten rechnen soll....
				double ch = getClientHeight() - (addOffset*2+20);
				
				getParent().vScrollClientWindow((int)(diff*(vso/ch)));
				
				double vsoff = -getParent().getVScrollOffset();
				
				// Immer 10 Pixel nach oben und unten Platz lassen, damit der Button nicht Bottom oder Top verdeckt!
				double buttonpos = (ch/vso)*vsoff-10;
				
				buttonPosition = (int)(addOffset+10+Math.round(buttonpos));
				
				getWindowManager().requestRedraw(this);
				getWindowManager().requestRedraw(getParent());
			}
			
			return true;
		}
		
		return result;
	}
	
	@Override
	public void paint( Graphics2D g ) {
		int addOffset = 0;
		
		if( showButtons ) {
			// TOP

			if( m_top_highlight == HIGHLIGHT_NONE ) {
				g.drawImage(getImageCache().getImage("interface/scrollbar/top.png",true),
						0,0, null);
			}
			else if( m_top_highlight == HIGHLIGHT_MO ) {
				g.drawImage(getImageCache().getImage("interface/scrollbar/top_mo.png",true),
						0,0, null);
			}
			else if( m_top_highlight == HIGHLIGHT_H ) {
				g.drawImage(getImageCache().getImage("interface/scrollbar/top_h.png",true),
						0,0, null);
			}
		
			// BAR
		
			g.drawImage(getImageCache().getImage("interface/scrollbar/bar.png",true),
					13,42,4,getClientHeight()-84, null);
			

			// BOTTOM
		
			if( bottomHighlight == HIGHLIGHT_NONE ) {
				g.drawImage(getImageCache().getImage("interface/scrollbar/bottom.png",true),
						0,getClientHeight()-42, null);
			}
			else if( bottomHighlight == HIGHLIGHT_MO )  {
				g.drawImage(getImageCache().getImage("interface/scrollbar/bottom_mo.png",true),
						0,getClientHeight()-42, null);
			}
			else if( bottomHighlight == HIGHLIGHT_H )  {
				g.drawImage(getImageCache().getImage("interface/scrollbar/bottom_h.png",true),
						0,getClientHeight()-42, null);
			}
			
			addOffset = 42;
		}
		else {
			// BAR
			
			g.drawImage(getImageCache().getImage("interface/scrollbar/bar.png",true),
					13,0,4,getClientHeight(), null);
		}
		
		// BUTTON
		
		double vso = getParent().getVScrollOverflow();		// Sonst begreift Java ja nicht, dass es mit Kommawerten rechnen soll....
		double ch = getClientHeight() - (addOffset*2+20);
		double vsoff = -getParent().getVScrollOffset();
		
		// Immer 10 Pixel nach oben und unten Platz lassen, damit der Button nicht Bottom oder Top verdeckt!
		double buttonpos = (ch/vso)*vsoff-10;
		
		if( showButtons)
		buttonPosition = (int)(addOffset+10+Math.round(buttonpos));
		
		if( buttonHighlight == HIGHLIGHT_NONE ) {
			g.drawImage(getImageCache().getImage("interface/scrollbar/button.png",true),
					5,buttonPosition, null);
		}
		else if( buttonHighlight == HIGHLIGHT_MO )  {
			g.drawImage(getImageCache().getImage("interface/scrollbar/button_mo.png",true),
					5,buttonPosition, null);
		}
		else if( buttonHighlight == HIGHLIGHT_H )  {
			g.drawImage(getImageCache().getImage("interface/scrollbar/button_h.png",true),
					5,buttonPosition, null);
		}
	}
}